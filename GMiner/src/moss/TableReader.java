/*----------------------------------------------------------------------
  File    : TableReader.java
  Contents: class for readers for table formats for graph data sets
  Author  : Christian Borgelt
  History : 2007.03.04 file created as TableFmt.java
            2007.06.26 split into reader and writer
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.FileWriter;

/*--------------------------------------------------------------------*/
/** Class for readers for simple table formats for graph data sets.
 *  @author Christian Borgelt
 *  @since  2007.03.04 */
/*--------------------------------------------------------------------*/
public class TableReader extends GraphReader {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** class/type flag: record separator */
  public static final int RECSEP  = 0x01;
  /** class/type flag: field separator */
  public static final int FLDSEP  = 0x02;
  /** class/type flag: blank character */
  public static final int BLANK   = 0x04;
  /** class/type flag: comment character */
  public static final int COMMENT = 0x08;
  /** the field names for the different file types */

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the character flags */
  private char[]       cflags;
  /** the buffer for the next table field */
  private StringBuffer buf;
  /** the next table field */
  private String       field;
  /** the last delimiter read: -1 if end of file/input,
   *  0 if field separator, 1 if record separator */
  private int          delim;
  /** the current record number */
  private int          recno;
  /** the fields of a record */
  private String[]     record;
  /** whether there is a pushed back record */
  private boolean      pbrec;

  /*------------------------------------------------------------------*/
  /** Create a table reader with default character flags.
   *  <p>By default the following character settings are used:<br>
   *  record separators: "\n", field separators: " \t", blanks: " \r\t",
   *  comment characters: "#".</p>
   *  @param  reader the reader to work on
   *  @param  mode   the read mode
   *  @param  ntn    the notation of the graphs
   *  @since  2006.10.05 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public TableReader (Reader reader, int mode, Notation ntn)
  {                             /* --- create a table reader */
    super(reader, mode);       /* store the arguments */
    this.ntn    = (ntn != null) ? ntn : new SMILES();
    this.buf    = new StringBuffer();
    this.cflags = new char[256];
    this.cflags['\n'] = RECSEP; /* initialize the character flags */
    this.cflags['\t'] = this.cflags[','] = FLDSEP;
    this.cflags['\r'] = this.cflags[' '] = BLANK;
    this.cflags['#' ] = COMMENT;
    this.field  = null;         /* no field has been read yet */
    this.delim  = -1;           /* set the delimiter to a default */
    this.recno  =  1;           /* clear the record counter */
    this.record = new String[TableWriter.HEADER[this.mode].length];
    this.pbrec  = false;        /* create a record buffer */
  }  /* TableReader() */

  /*------------------------------------------------------------------*/
  /** Set the characters for a specific type/class.
   *  @param  type  the type/class of the characters to set;
   *                must be one of the constants <code>RECSEP</code>,
   *                <code>FLDSEP</code>, <code>BLANK</code>, or
   *                <code>COMMENT</code> (or a combination of these,
   *                by binary or)
   *  @param  chars the characters to set
   *  @since  2007.06.26 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setChars (int type, String chars)
  {                             /* --- set the characters of a class */
    for (int i = this.cflags.length; --i >= 0; )
      this.cflags[i] &= ~type;  /* clear flags for all characters */
    for (int i = chars.length(); --i >= 0; )
      this.cflags[chars.charAt(i)] |= type;
  }  /* setChars() */           /* set flags for given characters */

  /*------------------------------------------------------------------*/
  /** Set the characters for all types.
   *  <p>If a parameter is <code>null</code>, the corresponding
   *  character flags are maintained.</p>
   *  @param  recseps the record  separators
   *  @param  fldseps the field   separators
   *  @param  blanks  the blank   characters
   *  @param  comment the comment characters
   *  @since  2007.05.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setChars (String recseps, String fldseps,
                        String blanks,  String comment)
  {                             /* --- set characters of all classes */
    if (recseps != null) this.setChars(RECSEP,  recseps);
    if (fldseps != null) this.setChars(FLDSEP,  fldseps);
    if (blanks  != null) this.setChars(BLANK,   blanks);
    if (comment != null) this.setChars(COMMENT, comment);
  }  /* setChars() */

  /*------------------------------------------------------------------*/
  /** Check whether a given character is in a given class
   *  or of a given type.
   *  @param  type the type/class for which to query;
   *               must be one of the constants <code>RECSEP</code>,
   *               <code>FLDSEP</code>, <code>BLANK</code>, or
   *               <code>COMMENT</code>
   *  @param  c    the character to query
   *  @return whether the character is in the given class
   *  @since  2006.10.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isType (int type, char c)
  { return (this.cflags[c] & type) != 0; }

  /*------------------------------------------------------------------*/
  /** Get the classes/types of a given character.
   *  @param  c the character to query
   *  @return the classes character is in, as a combination of the
   *          flags <code>RECSEP</code>, <code>FLDSEP</code>,
   *          <code>BLANK</code>, or <code>COMMENT</code>
   *  @since  2006.10.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getTypes (char c)
  { return this.cflags[c]; }

  /*------------------------------------------------------------------*/
  /** Get a string stating the current record number.
   *  Useful for error reporting.
   *  @return a string stating the current record number
   *          in the format "(record xxx)"
   *  @since  2007.01.31 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String rno ()
  { return this.rno(0); }

  /*------------------------------------------------------------------*/
  /** Get a string stating the current record number.
   *  <p>Useful for error reporting.</p>
   *  @param  offset the offset to add to the record number
   *  @return a string stating the current record number
   *          in the format "(record xxx)"
   *  @since  2007.03.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String rno (int offset)
  { return " (record " +(this.recno +offset) +")"; }

  /*------------------------------------------------------------------*/
  /** Read the next field/cell of the table.
   *  <p>Note that a record separator is (virtually) inserted at the
   *  end of the file/input if the file/input does not end with a
   *  record separator.</p>
   *  @return the type of the delimiter of the field read:
   *          <p><table cellpadding=0 cellspacing=0>
   *          <tr><td>-1,&nbsp;</td>
   *              <td>if end of file/input,</td></tr>
   *          <tr><td align="right">0,&nbsp;</td>
   *              <td>if field separator,</td>
   *          <tr><td>+1,&nbsp;</td>
   *              <td>if record separator.</td></tr>
   *          </table></p>
   *  @throws IOException if an i/o error occurs
   *  @since  2006.10.05/2007.06.26 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readField () throws IOException
  {                             /* --- read the next field */
    int c, f, i;                /* character, flags, index */

    this.field = null;          /* initialize the field */
    c = this.read();            /* get and check the next character */
    if (c < 0) return this.delim = -1;

    /* --- skip comment records --- */
    if (this.delim != 0) {      /* if at the start of a record */
      while ((c < this.cflags.length)     /* comment read loop */
      &&     ((this.cflags[c] & COMMENT) != 0)) {
        this.recno++;           /* count the record to be read */
        while ((c >= this.cflags.length)
        ||     ((this.cflags[c] & RECSEP) == 0)) {
          c = this.read();      /* get and check the next character */
          if (c < 0) return this.delim = -1;
        }                       /* read up to a record separator */
        c = this.read();        /* get the next character */
        if (c < 0) return this.delim = -1;
      }                         /* check for end of file/input */
    }                           /* (comment records are skipped) */

    /* --- skip leading blanks --- */
    while ((c < this.cflags.length)
    &&     ((this.cflags[c] & BLANK) != 0)) {
      c = this.read();          /* get and check the next character */
      if (c < 0) return this.delim = 1;
    }
    /* Note that after at least one valid character was read, even  */
    /* if it is a blank, the end of file/input is translated into a */
    /* record separator. -1 is returned only if no character could  */
    /* be read before the end of file/input is encountered.         */

    /* --- read the field --- */
    if (c < this.cflags.length) {
      f = this.cflags[c];       /* get the character class and */
      if ((f & RECSEP) != 0) {  /* check for record separator */
        this.recno++; return this.delim = 1; }
      if ((f & FLDSEP) != 0) {  /* check for field separator */
                      return this.delim = 0; }
    }                           /* return an empty field */
    this.buf.setLength(0);      /* clear the read buffer */
    while (true) {              /* read the field value */
      this.buf.append((char)c); /* store the character in the buffer */
      c = this.read();          /* get the next character */
      if (c <  0)            { this.delim = 1;               break; }
      if (c >= this.cflags.length) continue;
      f = this.cflags[c];       /* check for record/field separator */
      if ((f & RECSEP) != 0) { this.delim = 1; this.recno++; break; }
      if ((f & FLDSEP) != 0) { this.delim = 0;               break; }
    }                           /* read up to a separator */

    /* --- remove trailing blanks --- */
    i = this.buf.length();      /* find index of last non-blank char. */
    do { f = this.buf.charAt(--i); }
    while ((f < this.cflags.length)
    &&     ((this.cflags[f] & BLANK) != 0));
    this.field = this.buf.substring(0, ++i);

    /* --- skip trailing blanks --- */
    if (this.delim != 0)        /* if not at a field separator, */
      return this.delim;        /* abort the function directly */
    while ((c < this.cflags.length)
    &&     ((this.cflags[c] & BLANK) != 0)) {
      c = this.read();          /* get the next character */
      if (c < 0) return this.delim = 1;
    }                           /* skip trailing blanks */
    f = (c < this.cflags.length) ? this.cflags[c] : 0;
    if ((f & RECSEP) != 0) {    /* check for a record separator */
      this.recno++; return this.delim = 1; }
    if ((f & FLDSEP) == 0) this.unread(c);
    return this.delim = 0;      /* set and return the delimiter type */
  }  /* readField() */

  /*------------------------------------------------------------------*/
  /** Read the next record of the table.
   *  @return whether a record could be read
   *          (otherwise the end of the input has been reached)
   *  @throws IOException if an i/o error occurs
   *  @since  2007.06.26 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private boolean readRecord () throws IOException
  {                             /* --- read a graph name/identifier */
    int i, n;                   /* loop variable, number of fields */

    if (this.readField() < 0)   /* read the first field and */
      return false;             /* check for end of input */
    n = this.record.length;     /* get the number of fields */
    for (i = 0; true; ) {       /* read the fields of the record */
      this.record[i] = this.field;
      if (++i >= n) break;      /* if all fields read, abort loop */
      if (this.delim != 0)      /* check for a field separator */
        throw new IOException("too few fields" +this.rno());
      this.readField();         /* read the next field */
    }
    if (this.delim != 1)        /* check for a record separator */
      throw new IOException("too many fields" +this.rno());
    return true;                /* return 'record successfully read' */
  }  /* readRecord() */

  /*------------------------------------------------------------------*/
  /** Read an (optional) table header.
   *  @return whether a header was present
   *          (otherwise the end of the input has been reached)
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean readHeader () throws IOException
  {                             /* --- read an (optional) header */
    int      i;                 /* loop variable */
    String[] hdr;               /* buffer for the header fields */

    if (!this.readRecord())     /* try to read a record and */
      return false;             /* check for end of input */
    hdr = TableWriter.HEADER[this.mode];
    for (i = this.record.length; --i >= 0; )
      if (!this.record[i].equals(hdr[i])) break;
    this.pbrec = (i >= 0);      /* check the field names */
    return i < 0;               /* return whether a header was found */
  }  /* readHeader() */

  /*------------------------------------------------------------------*/
  /** Read the next graph description.
   *  <p>The next graph description is read and split into the graph
   *  name/identifier, the graph description, the associated value
   *  (only in mode <code>GRAPHS</code>), and the support information
   *  (only in mode <code>SUBS</code>).</p>
   *  These properties may then be retrieved with the functions
   *  <code>getName()<code>, <code>getDesc()</code>,
   *  <code>getValue()</code> etc.</p>
   *  @return whether a graph description could be read
   *          (otherwise the end of the input has been reached)
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean readGraph () throws IOException
  {                             /* --- get the next graph description */
    String s = null;            /* buffer for a field */

    if (!this.pbrec             /* if there is no pushed back record */
    &&  !this.readRecord())     /* and no new record can be read, */
      return false;             /* the end of the input is reached */
    this.pbrec = false;         /* buffered record is now processed */
    this.graph = null;          /* clear the graph */
    try {                       /* try to get/parse the fields */
      this.name = this.record[0];  /* get the graph identifier */
      if (this.mode == GRAPHS) {  /* if graphs, modified by Shirui */
        this.value = Float.parseFloat(s = this.record[1]);
        this.desc  = this.record[2];
        this.nodes = this.edges = -1;
        this.srel  = this.crel  = 0.0F;
        this.sabs  = this.cabs  = 0; }
      else if(this.mode == SUBS){                    /* if substructures */
        this.desc  = this.record[1];
        this.nodes = Integer.parseInt(s = this.record[2]);
        this.edges = Integer.parseInt(s = this.record[3]);
        this.sabs  = Integer.parseInt(s = this.record[4]);
        this.srel  = Float.parseFloat(s = this.record[5]);
        this.cabs  = Integer.parseInt(s = this.record[6]);
        this.crel  = Float.parseFloat(s = this.record[7]);
      }
      else{ /*if semi-graphs, added by shirui*/
          this.value = Float.parseFloat(s = this.record[1]);
          this.desc  = this.record[2];
          this.isLabel = Boolean.parseBoolean(this.record[3]);
          this.nodes = this.edges = -1;
          this.srel  = this.crel  = 0.0F;
          this.sabs  = this.cabs  = 0;
      }
      }                       /* get description and values */
    catch (NumberFormatException e) {
      throw new IOException("malformed number '"+s+"'"+this.rno(-1)); }
    return true;                /* return that a graph was read */
  }  /* readGraph() */

  /*------------------------------------------------------------------*/
  /** Get the current graph or substructure.
   *  @return the current graph
   *  @throws IOException if a parse error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph getGraph () throws IOException
  {                             /* --- get the current graph */
    if (this.graph != null)     /* if there is a graph, */
      return this.graph;        /* simply return it */
    if (this.desc == null)      /* if there is no description, */
      return null;              /* there is no graph to be had */
    StringReader srdr = new StringReader(this.desc);
    this.graph = this.ntn.parse(srdr);
    if ((ntn.getDelim() >= 0) || (srdr.read() >= 0))
      throw new IOException("garbage at end of graph description"
                            +this.rno(-1));
    srdr.close();               /* parse the graph description */
    return this.graph;          /* and return the parsed graph */
  }  /* getGraph() */

  /*------------------------------------------------------------------*/
  /** Main function for testing basic functionality.
   *  @param  args the command line arguments
   *  @since  2007.06.26 (Christian Borgelt) */
  /*------------------------------------------------------------------*/
  
  public static void main (String args[])
  {                             /* --- main function for testing */
    Notation    ntn;            /* notation for the graphs */
    TableReader reader;         /* reader for the input  file */
    TableWriter writer;         /* writer for the output file */

    if (args.length != 2) {     /* if wrong number of arguments */
      System.err.println("usage: java moss.TableReader <in> <out>");
      return;                   /* print a usage message */
    }                           /* and abort the program */

    try {                       /* try to read the file */
      ntn    = new SMILES();    /* with a SMILES notation */
      reader = new TableReader(new FileReader(args[0]), GRAPHS, ntn);
      writer = new TableWriter(new FileWriter(args[1]), GRAPHS, ntn);
      if (reader.readHeader())  /* create a reader and a writer */
        writer.writeHeader();   /* and copy a possible header */
      while (reader.readGraph()) {
        writer.setName(reader.getName());
        writer.setGraph(reader.getGraph());
        writer.setValue(reader.getValue());
        writer.writeGraph();    /* while there are more graphs, */
      }                         /* read and write graphs */
      reader.close(); writer.close(); }
    catch (IOException e) {     /* catch and report parse errors */
      System.err.println(e.getMessage()); }
  }  /* main() */
  
}  /* class TableReader */
