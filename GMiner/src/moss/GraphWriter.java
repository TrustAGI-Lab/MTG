/*----------------------------------------------------------------------
  File    : GraphWriter.java
  Contents: class for writers for graph data sets
  Author  : Christian Borgelt
  History : 2007.02.24 file created as part of Notation.java
            2007.03.04 generalized and made a separated class
            2007.03.21 functions for numbers of nodes and edges added
            2007.06.26 split into reader and writer
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Writer;
import java.io.BufferedWriter;

/*--------------------------------------------------------------------*/
/** Class for a writer for graph data sets.
 *  @author Christian Borgelt
 *  @since  2007.02.24 */
/*--------------------------------------------------------------------*/
public abstract class GraphWriter extends BufferedWriter {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** write mode: graphs */
  public static final int GRAPHS = 0;
  /** write mode: substructures */
  public static final int SUBS   = 1;
  
  /** write mode: SEMI-GRAPHS, ADD BY SHIRUI */
  public static final int SEMIGRAPHS = 2;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the write mode */
  protected int      mode  = GRAPHS;
  /** the notation for the graphs */
  protected Notation ntn   = null;
  /** the name of the current graph */
  protected String   name  = null;
  /** the description of the current graph */
  protected String   desc  = null;
  /** the current graph */
  protected Graph    graph = null;
  /** the value associated with the current graph */
  protected float    value = 0.0F;
  /** the number of nodes of the current graph */
  protected int      nodes = 0;
  /** the number of edges of the current graph */
  protected int      edges = 0;
  /** the absolute support in the focus */
  protected int      sabs  = 0;
  /** the relative support in the focus */
  protected float    srel  = 0.0F;
  /** the absolute support in the complement */
  protected int      cabs  = 0;
  /** the relative support in the complement */
  protected float    crel  = 0.0F;

  /** is the graph being labeled, added by shirui*/
  protected boolean    isLabel  = true;
  
  /*------------------------------------------------------------------*/
  /** Create a writer for a graph data set.
   *  @param  writer the writer to write to
   *  @param  mode   the write mode
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected GraphWriter (Writer writer, int mode)
  {                             /* --- create a graph reader */
    super(writer);              /* init. writer and store write mode */
    this.mode = mode; /*modified by Shirui*/
  }  /* GraphWriter() */

  /*------------------------------------------------------------------*/
  /** Get the mode of the graph writer.
   *  @return the mode of the graph writer
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getMode ()
  { return this.mode; }

  /*------------------------------------------------------------------*/
  /** Get the notation of the graph writer.
   *  @return the notation of the graph writer
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Notation getNotation ()
  { return this.ntn; }
  
  public void setNotation (Notation ntn)
  { this.ntn = ntn; }

  /*------------------------------------------------------------------*/
  /** Set the name of the current graph.
   *  @param  name the name of the current graph
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setName (String name)
  { this.name = name; }

  /*------------------------------------------------------------------*/
  /** Set the current graph or substructure.
   *  @param  graph the graph or substructure to set
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setGraph (Graph graph)
  {                             /* --- set the current graph */
    this.graph = graph;         /* store the graph and */
    if (graph == null)          /* either clear the counters or */
      this.nodes = this.edges = 0;
    else {                      /* set the node and edge counters */
      this.nodes = graph.getNodeCount();
      this.edges = graph.getEdgeCount();
    }                           /* (cannot be set directly) */
  }  /* setGraph() */

  /*------------------------------------------------------------------*/
  /** Set the value associated with the current graph.
   *  @param  value the value associated with the current graph
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setValue (float value)
  { this.value = value; }

  /*------------------------------------------------------------------*/
  /** Set the absolute focus support of the current substructure.
   *  @param  supp the absolute focus support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setAbsSupp (int supp)
  { this.sabs = supp; }

  /*------------------------------------------------------------------*/
  /** Set the relative focus support of the current substructure.
   *  @param  supp the relative focus support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setRelSupp (float supp)
  { this.srel = supp; }

  /*------------------------------------------------------------------*/
  /** Set the absolute complement support of the current substructure.
   *  @param  supp the absolute complement support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setAbsCompl (int supp)
  { this.cabs = supp; }

  /*------------------------------------------------------------------*/
  /** Set the relative complement support of the current substructure.
   *  @param  supp the relative complement support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setRelCompl (float supp)
  { this.crel = supp; }

  public boolean getIsLabel() {
	return isLabel;
}

public void setIsLabel(boolean isLabel) {
	this.isLabel = isLabel;
}

/*------------------------------------------------------------------*/
  /** Write a header.
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract void writeHeader () throws IOException;

  /*------------------------------------------------------------------*/
  /** Write the current graph description.
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract void writeGraph () throws IOException;

  /*------------------------------------------------------------------*/
  /** Create a graph writer for a given format and mode.
   *  @param  writer the writer to write to
   *  @param  mode   the write mode
   *  @param  format the name of the format
   *  @return the created input/output format
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static GraphWriter createWriter (Writer writer, int mode,
                                          String format)
  {                             /* --- create a graph writer */
    if (format.equalsIgnoreCase("smiles") ||format.equalsIgnoreCase("smi"))
      return new TableWriter(writer, mode, new SMILES());
    if (format.equalsIgnoreCase("sln"))
      return new TableWriter(writer, mode, new SLN());
    if (format.equalsIgnoreCase("sdfile"))
      return new SDfileWriter(writer, mode);
    if (format.equalsIgnoreCase("sdf"))
        return new SDfileWriter(writer, mode);
    if (format.equalsIgnoreCase("mdl"))
      return new SDfileWriter(writer, mode);
    if (format.equalsIgnoreCase("linog"))
      return new TableWriter(writer, mode, new LiNoG());
    if (format.equalsIgnoreCase("list"))
      return new NEListWriter(writer, mode);
    if (format.equalsIgnoreCase("nelist"))
      return new NEListWriter(writer, mode);
    if (format.equalsIgnoreCase("nel"))
        return new NEListWriter(writer, mode);
    return null;                /* evaluate the notation name */
  }  /* createWriter() */

}  /* class GraphWriter */
