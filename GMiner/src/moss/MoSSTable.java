/*----------------------------------------------------------------------
  File    : MoSSTable.java
  Contents: Class for data tables for molecular substructure mining
  Author  : Christian Borgelt
  History : 2007.02.15 file created from Table.java
            2007.03.04 adapted to new Format classes
            2007.06.29 adapted to new GraphReader class
            2007.07.01 error reporting improved (file not found)
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import javax.swing.table.AbstractTableModel;

/*--------------------------------------------------------------------*/
/** Class for data tables for molecular substructure mining.
 *  <p>This data table class is implemented as a subclass of
 *  <code>AbstractTableModel</code> so that it can be displayed
 *  directly in a <code>JTable</code>.</p>
 *  @author Christian Borgelt
 *  @since  2007.02.15 */
/*--------------------------------------------------------------------*/
public class MoSSTable extends AbstractTableModel {

  private static final long serialVersionUID = 0x00020003;

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** mode: graphs */
  public static final int GRAPHS = GraphReader.GRAPHS;
  /** mode: substructures */
  public static final int SUBS   = GraphReader.SUBS;
  /** mode: identifiers */
  public static final int IDS    = GraphReader.SUBS +1;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the mode of the table */
  private int          mode   = GRAPHS;
  /** the format of the table */
  private String       format = null;
  /** the names of the columns of the table */
  private String[]     names  = null;
  /** the columns the table (arrays of different types) */
  private Object[]     data   = null;
  /** the number of rows of the table */
  private int          rowcnt = 0;
  /** the buffer for reading input lines */
  private StringBuffer buf    = null;

  /*------------------------------------------------------------------*/
  /** Create a data table.
   *  @param  mode   the table mode
   *  @param  format the format of the input
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MoSSTable (int mode, String format)
  {                             /* --- create a table */
    this.rowcnt = 0;            /* there are no table rows yet */
    this.mode   = mode;         /* note the table mode */
    this.format = format;       /* and the format */
    if      (mode == IDS) {     /* if identifier table */
      this.names = new String[2];
      this.data  = new Object[2];
      this.names[0] = "id";          this.data[0] = new String[0];
      this.names[1] = "list";        this.data[1] = new String[0]; }
    else if (mode == SUBS) {    /* if substructure table */
      this.names = new String[8];
      this.data  = new Object[8];
      this.names[0] = "id";          this.data[0] = new String[0];
      this.names[1] = "description"; this.data[1] = new String[0];
      this.names[2] = "nodes";       this.data[2] = new int[0];
      this.names[3] = "edges";       this.data[3] = new int[0];
      this.names[4] = "s_abs";       this.data[4] = new int[0];
      this.names[5] = "s_rel";       this.data[5] = new float[0];
      this.names[6] = "c_abs";       this.data[6] = new int[0];
      this.names[7] = "c_rel";       this.data[7] = new float[0]; }
    else {                      /* if graph data table */
      this.names = new String[3];
      this.data  = new Object[3];
      this.names[0] = "id";          this.data[0] = new String[0];
      this.names[1] = "value";       this.data[1] = new float[0];
      this.names[2] = "description"; this.data[2] = new String[0];
    }                           /* create the table schema */
  }  /* MoSSTable() */

  /*------------------------------------------------------------------*/
  /** Get the number of rows of the table.
   *  @return the number of rows of the table
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getRowCount ()
  { return this.rowcnt; }

  /*------------------------------------------------------------------*/
  /** Get the number of columns of the data table.
   *  @return the number of columns of the data table
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getColumnCount ()
  { return (this.names != null) ? this.names.length : 0; }

  /*------------------------------------------------------------------*/
  /** Get the name of a column given its index.
   *  @param  col the index of the column
   *  @return the name of the column with the given index
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String getColumnName (int col)
  { return this.names[col]; }

  /*------------------------------------------------------------------*/
  /** Returns whether a table cell is editable.
   *  <p>Editing is currently not supported.</p>
   *  @param  row the row of the cell to access; must be
   *              in the range 0 to <code>getRowCount()-1</code>
   *  @param  col the column of the cell to access; must be
   *              in the range 0 to <code>getColumnCount()-1</code>
   *  @return whether the specified cell is editable
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isCellEditable (int row, int col)
  { return false; }

  /*------------------------------------------------------------------*/
  /** Get the value of a table cell as an object.
   *  @param  row the row of the cell to access; must be
   *              in the range 0 to <code>getRowCount()-1</code>
   *  @param  col the column of the cell to access; must be
   *              in the range 0 to <code>getColumnCount()-1</code>
   *  @return an object representing the value in the specified cell
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Object getValueAt (int row, int col)
  {                             /* --- get the value of a table cell */
    Object c = this.data[col];  /* get the column data array */
    if (c instanceof int[])     /* if integer-valued column */
      return "" +((int[])c)[row];
    if (c instanceof float[])   /* if real-valued column */
      return "" +((float[])c)[row];
    return ((String[])c)[row];  /* if string-valued column */
  }  /* getValueAt() */

  /*------------------------------------------------------------------*/
  /** Set the value of a table cell from an object.
   *  @param  value the value to set in the specified cell
   *  @param  row   the row of the cell to set; must be
   *                in the range 0 to <code>getRowCount()-1</code>
   *  @param  col   the column of the cell to set; must be
   *                in the range 0 to <code>getColumnCount()-1</code>
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setValueAt (Object value, int row, int col)
  {                             /* --- set the value of a table cell */
    Object c = this.data[col];  /* get the column data array */
    if      (c instanceof int[])    /* if integer-valued column */
      try { ((int[])c)[row] = Integer.parseInt(value.toString()); }
      catch (NumberFormatException e) { ((int[])c)[row] = 0; }
    else if (c instanceof float[])  /* if real-valued column */
      try { ((float[])c)[row] = Float.parseFloat(value.toString()); }
      catch (NumberFormatException e) { ((float[])c)[row] = 0.0F; }
    else                             /* if string-valued column */
      ((String[])c)[row] = value.toString();
  }  /* setValueAt() */

  /*------------------------------------------------------------------*/
  /** Resize the value arrays of the table.
   *  @param  newcnt the new number of rows
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void resize (int newcnt)
  {                             /* --- resize the table */
    Object c;                   /* to traverse the data arrays */
    Object tmp;                 /* buffer for reallocation */

    if (newcnt == this.rowcnt)  /* if the size already fits, */
      return;                   /* abort the function */
    if (newcnt <  this.rowcnt)  /* if the new size is smaller, */
      this.rowcnt = newcnt;     /* cut the excess rows */
    for (int i = this.data.length; --i >= 0; ) {
      c = this.data[i];         /* traverse the table columns */ 
      if      (c instanceof int[])   tmp = new int   [newcnt];
      else if (c instanceof float[]) tmp = new float [newcnt];
      else                           tmp = new String[newcnt];
      System.arraycopy(c, 0, this.data[i] = tmp, 0, this.rowcnt);
    }                           /* enlarge columns and copy values */
    this.rowcnt = newcnt;       /* note the new number of rows */
  }  /* resize() */

  /*------------------------------------------------------------------*/
  /** Print the row/record counter.
   *  @param  n the row/record counter
   *  @since  2007.03.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private static void print (int n)
  {                             /* --- print the row/record counter */
    String s = "        " +n;   /* format the number and print it */
    System.err.print(s.substring(s.length()-9));
    System.err.print("\b\b\b\b\b\b\b\b\b");
  }  /* print() */

  /*------------------------------------------------------------------*/
  /** Read the next input line.
   *  @param  reader the reader to read from
   *  @return the next input line or <code>null</code>
   *          if the end of the input stream has been reached
   *  @since  2007.02.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private String readLine (Reader reader) throws IOException
  {                             /* --- read the next input line */
    int c = reader.read();      /* read the next character */
    if (c < 0) return null;     /* and check for end of input */
    if (this.buf == null)       /* create a read buffer */
      this.buf = new StringBuffer();
    this.buf.setLength(0);      /* clear the read buffer */
    while ((c >= 0) && (c != '\n')) {
      this.buf.append((char)c); /* append current character */
      c = reader.read();        /* to the read buffer and */
    }                           /* read the next character */
    return this.buf.toString(); /* return the input line read */
  }  /* readLine() */

  /*------------------------------------------------------------------*/
  /** Read the next input line.
   *  <p>Empty input lines and lines starting with the given comment
   *  character are skipped. If no character should be seen as a comment
   *  indicator (but empty lines should still be skipped), a value of
   *  <code>-1</code> can be passed for <code>comment</code>.</p>
   *  @param  reader  the reader to read from
   *  @param  comment the character that indicates a comment line
   *  @return the next input line or <code>null</code>
   *          if the end of the input stream has been reached
   *  @since  2007.02.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private String readLine (Reader reader, int comment)
    throws IOException
  {                             /* --- read the next input line */
    String line;                /* next input line */
    do {                        /* read the next input line */
      line = this.readLine(reader);
      if (line == null) return null;
      line = line.trim();       /* remove whitespace */
    } while ((line.length()  <= 0)  /* read while the line is empty */
    ||       (line.charAt(0) == comment));  /* or contains comments */
    return line;                /* return the input line read */
  }  /* readLine() */

  /*------------------------------------------------------------------*/
  /** Read table from an input stream.
   *  @param  file the file to read from
   *  @since  2007.02.08 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void read (File file) throws IOException
  {                             /* --- read table */
    FileReader  reader;         /* reader for the input file */
    GraphReader grdr;           /* reader for graphs/substructures */
    boolean     hdr = false;    /* whether a header was read */
    int         row = 0;        /* current row/number of rows */
    String      line, fld;      /* buffer for an input line/field */
    int         i;              /* index in input line */

    System.err.print("reading " +file +" ... ");
    try {                       /* create a reader for the file */
      reader = new FileReader(file);
      if (this.mode == IDS) {   /* if identifier file */
        while (true) {          /* read the table rows */
          line = this.readLine(reader, '#');
          if (line == null) break; /* read the next input line */
          if (row >= this.rowcnt)  /* resize columns if necessary */
            this.resize(row +((row > 32) ? row >> 1 : 32));
          i = line.indexOf(':');
          if (i < 0) i = line.indexOf(',');
          if (i < 0) i = line.indexOf('\t');
          if (i < 0) throw new IOException("missing separator");
          fld  = line.substring(0,i).trim();
          line = line.substring(i+1).trim();
          if ((row == 0) && !hdr   /* skip a possible header */
          &&  fld.equals("id") && line.equals("list")) {
            hdr = true; continue; } 
          ((String[])this.data[0])[row] = fld;
          ((String[])this.data[1])[row] = line;
          if ((++row & 0xff) == 0) MoSSTable.print(row);
        }                       /* print the number of substructures */
        reader.close(); }       /* in the end close the file */
      else if (this.mode == SUBS) {  /* if substructure data */
        grdr = GraphReader.createReader(reader, this.mode, this.format);
        grdr.readHeader();      /* read an optional header */
        while (grdr.readGraph()) {
          if (row >= this.rowcnt)  /* resize columns if necessary */
            this.resize(row +((row > 32) ? row >> 1 : 32));
          ((String[])this.data[0])[row] = grdr.getName();
          ((String[])this.data[1])[row] = grdr.getDesc();
          ((int[])   this.data[2])[row] = grdr.getNodeCount();
          ((int[])   this.data[3])[row] = grdr.getEdgeCount();
          ((int[])   this.data[4])[row] = grdr.getAbsSupp();
          ((float[]) this.data[5])[row] = grdr.getRelSupp();
          ((int[])   this.data[6])[row] = grdr.getAbsCompl();
          ((float[]) this.data[7])[row] = grdr.getRelCompl();
          if ((++row & 0xff) == 0) MoSSTable.print(row);
        }                       /* print the number of substructures */
        grdr.close(); }         /* in the end close the file */
      else {                    /* if graph data */
        grdr = GraphReader.createReader(reader, this.mode, this.format);
        grdr.readHeader();      /* read an optional header */
        while (grdr.readGraph()) {
          if (row >= this.rowcnt)  /* resize columns if necessary */
            this.resize(row +((row > 32) ? row >> 1 : 32));
          ((String[])this.data[0])[row] = grdr.getName();
          ((float[]) this.data[1])[row] = grdr.getValue();
          ((String[])this.data[2])[row] = grdr.getDesc();
          if ((++row & 0xff) == 0) MoSSTable.print(row);
        }                       /* print the number of graphs */
        grdr.close();           /* in the end close the file */
      } }
    catch (IOException e) {     /* report row number with error */
      System.err.println();     /* except for file not found */
      if (e instanceof FileNotFoundException) throw e;
      throw new IOException(file.getPath() +", " +(row +1) +": "
                            +e.getMessage());
    }
    if (row < this.rowcnt)      /* if there are more rows than */
      this.resize(row);         /* have been read, remove them */
    System.err.println("[" +row +" record(s)] done.");
  }  /* read() */

}  /* class MoSSTable */
