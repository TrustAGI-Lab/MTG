/*----------------------------------------------------------------------
  File    : NEListWriter.java
  Contents: Class for a simple node/edge list format writer
  Author  : Christian Borgelt
  History : 2007.06.22 file created as ListFmt.java
            2007.06.29 split into reader and writer
            2007.08.16 bug in function writeGraph() fixed (graph name)
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Writer;

/*--------------------------------------------------------------------*/
/** Class for writers for a simple node/edge list format.
 *  @author Christian Borgelt
 *  @since  2007.06.22 */
/*--------------------------------------------------------------------*/
public class NEListWriter extends GraphWriter {

  /*------------------------------------------------------------------*/
  /** Create a writer for a simple node/edge list format.
   *  @param  writer the writer to write to
   *  @param  mode   the write mode
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NEListWriter (Writer writer, int mode)
  {                             /* create a node/edge list writer */
    super(writer, mode);        /* store the arguments */
    this.ntn = new NEList();    /* create a notation */
  }  /* NEListWriter() */

  /*------------------------------------------------------------------*/
  /** Write a header.
   *  <p>This function does nothing,
   *  since headers are not supported with a node/edge list format.</p>
   *  @throws IOException if an i/o error occurs
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void writeHeader () throws IOException
  { }

  /*------------------------------------------------------------------*/
  /** Write the current graph description.
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void writeGraph () throws IOException
  {                             /* --- write the current graph */
    this.ntn.write(this.graph, this);
    this.write("g ");           /* write the graph and its name */
    this.write((this.name != null) ? this.name : "g");
    this.write('\n');           /* terminate the output line */
    if (this.mode != SUBS){      /* if graphs */
      this.write("x " +this.value);
      if(mode == SEMIGRAPHS){
    	  this.write('\n'); 
    	  this.write("L "+this.getIsLabel());
      }
    }
    else {                      /* if substructures */
      this.write("s " +this.nodes);
      this.write(" "  +this.edges);
      this.write(" "  +this.sabs);
      this.write(" "  +this.srel);
      this.write(" "  +this.cabs);
      this.write(" "  +this.crel);
    }                           /* write additional information */
    this.write("\n\n");         /* terminate the output line */
  }  /* writeGraph() */         /* and add an empty line */

}  /* class NEListWriter */
