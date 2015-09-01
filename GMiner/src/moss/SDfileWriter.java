/*----------------------------------------------------------------------
  File    : SDfileWriter.java
  Contents: class for writers for SDfiles (Structure-Data files)
  Author  : Christian Borgelt
  History : 2007.02.24 file created as SDfileFmt.java
            2007.06.26 split into reader and writer
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Writer;

/*--------------------------------------------------------------------*/
/** Class for writers for structure-data files (SDfile, Elsevier MDL).
 *  @author Christian Borgelt
 *  @since  2007.02.24 */
/*--------------------------------------------------------------------*/
public class SDfileWriter extends GraphWriter {

  /*------------------------------------------------------------------*/
  /** Create a writer for SDfiles.
   *  @param  writer the writer to write to
   *  @param  mode   the write mode
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public SDfileWriter (Writer writer, int mode)
  {                             /* --- create an SDfile writer */
    super(writer, mode);        /* store the arguments */
    this.ntn = new Ctab();      /* create a notation */
  }  /* SDfileWriter() */

  /*------------------------------------------------------------------*/
  /** Write a header.
   *  <p>This function does nothing,
   *  since headers are not supported with SDfiles.</p>
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void writeHeader () throws IOException
  { }

  /*------------------------------------------------------------------*/
  /** Write a description of the current graph.
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void writeGraph () throws IOException
  {                             /* --- write the current graph */
    this.write(this.name);      /* write the graph name and */
    this.write("\n\n\n");       /* dummy info and comments line */
    this.ntn.write(this.graph, this);
    this.write('\n');           /* write the connection table */
    if (this.mode == GRAPHS) {    /* if graphs */
      this.write("> <value>\n");/* write associated value */
      this.write(this.value +"\n"); }
    else if(this.mode==SUBS){                      /* if substructures */
      this.write("> <support>\n");
      this.write(this.sabs +" ");
      this.write(this.srel +"\n");
      this.write(this.cabs +" ");
      this.write(this.crel +"\n");
    }                           /* write the support values */
    else{ /*write semi graphs, added by Shirui Pan*/
        this.write("> <value>\n");/* write associated value */
        this.write(this.value +"\n\n");
        
        this.write("> <isLabel>\n");/* write isLabeled value*/
        this.write(this.isLabel+"\n");
    }
    this.write("\n$$$$\n");     /* terminate the graph description */
  }  /* writeGraph() */

}  /* class SDfileWriter */
