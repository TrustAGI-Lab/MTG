/*----------------------------------------------------------------------
  File    : TableWriter.java
  Contents: class for writers for table formats for graph data sets
  Author  : Christian Borgelt
  History : 2007.03.04 file created as TableFmt.java
            2007.06.26 split into reader and writer
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Writer;

/*--------------------------------------------------------------------*/
/** Class for writers for simple table formats for graph data sets.
 *  @author Christian Borgelt
 *  @since  2007.03.04 */
/*--------------------------------------------------------------------*/
public class TableWriter extends GraphWriter {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** the field names for the different file types. Add a field by Shirui for semi-supervised learning */
  protected static final String[][] HEADER = {
    { "id", "value", "description" },          /* GRAPHS */
    { "id", "description", "nodes", "edges",   /* SUBS */
      "s_abs", "s_rel", "c_abs", "c_rel" }, 
    {"id", "value", "description","islable"} /*Semi-Graph, modified by Shirui 25-5-2012*/  
  };

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the record separator */
  private char recsep = '\n';
  /** the field separator */
  private char fldsep = ',';

  /*------------------------------------------------------------------*/
  /** Create a simple table format writer.
   *  @param  writer the writer to write to
   *  @param  mode   the write mode
   *  @param  ntn    the notation for the graph descriptions
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public TableWriter (Writer writer, int mode, Notation ntn)
  {                             /* --- create a table writer */
    super(writer, mode);        /* store the arguments */
    this.ntn = (ntn != null) ? ntn : new SMILES();
  }  /* TableWriter() */

  /*------------------------------------------------------------------*/
  /** Set the record and field separators.
   *  @param  recsep the record separator
   *  @param  fldsep the field  separator
   *  @since  2007.06.26 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setChars (char recsep, char fldsep)
  { this.recsep = recsep; this.fldsep = fldsep; }

  /*------------------------------------------------------------------*/
  /** Write a header.
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void writeHeader () throws IOException
  {                             /* --- write a table header */
    String[] hdr = HEADER[this.mode];
    for (int i = 0; i < hdr.length; i++) {
      if (i > 0) this.write(this.fldsep);
      this.write(hdr[i]);       /* write the field names */
    }                           /* separated by field separators */
    this.write(this.recsep);    /* terminate the record */
  }  /* writeHeader() */

  /*------------------------------------------------------------------*/
  /** Write a description of the current graph.
   *  @throws IOException if an i/o error occurs
   *  @since  2007.02.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void writeGraph () throws IOException
  {                             /* --- write a graph description */
    this.write(this.name);      /* write the graph name */
    if (this.mode == GRAPHS){      /* write the associated value, MODIFIED BY SHIRUI */
      this.write("" +this.fldsep +this.value);
    this.write(this.fldsep);    /* write the graph description */
    this.ntn.write(this.graph, this);
    }
    if (this.mode == SUBS) {    /* if substructures */
       this.write(this.fldsep);    /* write the graph description */
       this.ntn.write(this.graph, this);
      this.write("" +this.fldsep +this.nodes);
      this.write("" +this.fldsep +this.edges);
      this.write("" +this.fldsep +this.sabs);
      this.write("" +this.fldsep +this.srel);
      this.write("" +this.fldsep +this.cabs);
      this.write("" +this.fldsep +this.crel);
    }                           /* write the support values */
    if (this.mode == SEMIGRAPHS) {  
    	
        this.write("" +this.fldsep +this.value);
        this.write(this.fldsep);    /* write the graph description */
        this.ntn.write(this.graph, this);
        this.write(this.fldsep);
        this.write(""+this.isLabel);
    }
    this.write(this.recsep);    /* terminate the record */
  }  /* writeGraph() */

}  /* class TableWriter */
