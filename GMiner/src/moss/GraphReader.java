/*----------------------------------------------------------------------
  File    : GraphReader.java
  Contents: class for readers for graph data sets
  Author  : Christian Borgelt
  History : 2007.02.24 file created as part of Notation.java
            2007.03.04 generalized and made a separated class
            2007.03.21 functions for numbers of nodes and edges added
            2007.06.26 split into reader and writer
            2011.06.20 added more abbreviations to identify formats
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.PushbackReader;

/*--------------------------------------------------------------------*/
/** Class for a reader for graph data sets.
 *  @author Christian Borgelt
 *  @since  2007.02.24 */
/*--------------------------------------------------------------------*/
public abstract class GraphReader extends PushbackReader {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** read mode: graphs */
  public static final int GRAPHS = 0;
 
  
  /** read mode: substructures */
  public static final int SUBS   = 1;
  
  /** read mode: SEMI-GRAPHS, ADD BY SHIRUI */
  public static final int SEMIGRAPHS = 2;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the read mode */
  protected int      mode  = GRAPHS;
  /** the notation for parsing graph descriptions */
  protected Notation ntn   = null;
  /** the name/identifier of the current graph */
  protected String   name  = null;
  /** the description of the current graph */
  protected String   desc  = null;
  /** the current graph */
  protected Graph    graph = null;
  /** the value associated with the current graph */
  protected float    value = 0.0F;
  /** the number of nodes of the current graph */
  protected int      nodes = -1;
  /** the number of edges of the current graph */
  protected int      edges = -1;
  /** the absolute support in the focus */
  protected int      sabs  = 0;
  /** the relative support in the focus */
  protected float    srel  = 0.0F;
  /** the absolute support in the complement */
  protected int      cabs  = 0;
  /** the relative support in the complement */
  protected float    crel  = 0.0F;

  /** is the graph being labeled, added by shirui*/
  protected boolean  isLabel  = true;
  
  /*------------------------------------------------------------------*/
  /** Create a reader for a graph data set.
   *  @param  reader the reader to read from
   *  @param  mode   the read mode
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected GraphReader (Reader reader, int mode)
  {                             /* --- create a graph reader */
    super(reader, 4);           /* init. reader and store read mode */
    this.mode = mode; /*modified by Shirui 12-5-2012*/
  }  /* GraphReader() */

  /*------------------------------------------------------------------*/
  /** Get the mode of the graph reader.
   *  @return the mode of the graph reader
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getMode ()
  { return this.mode; }

  /*------------------------------------------------------------------*/
  /** Get the notation of the graph reader.
   *  @return the notation of the graph reader
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Notation getNotation ()
  { return this.ntn; }
  
  public void settNotation (Notation ntn)
  { this.ntn = ntn; }

  /*------------------------------------------------------------------*/
  /** Read an (optional) header.
   *  @return whether a header was present
   *          (otherwise the end of the input has been reached)
   *  @throws IOException if an i/o error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract boolean readHeader () throws IOException;

  /*------------------------------------------------------------------*/
  /** Read a graph.
   *  <p>The next graph description is read and split into the graph
   *  name/identifier, the graph description, the associated value
   *  (only in mode <code>GRAPHS</code>), and the support information
   *  (only in mode <code>SUBS</code>).</p>
   *  These properties may then be retrieved with the functions
   *  <code>getName()<code>, <code>getDesc()</code>,
   *  <code>getValue()</code> etc.</p>
   *  @return whether another graph description could be read
   *          (otherwise the end of the input has been reached)
   *  @throws IOException if an i/o error or a parse error occurs
   *  @since  2007.02.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract boolean readGraph () throws IOException;

  /*------------------------------------------------------------------*/
  /** Get the name of the current graph.
   *  @return the name of the current graph
   *  @since  2007.02.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String getName ()
  { return this.name; }

  /*------------------------------------------------------------------*/
  /** Get a line description of the current graph.
   *  @return a line description of the current graph
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String getDesc ()
  {                             /* --- get a (line) description */
    if (this.desc == null) {    /* if there is no description */
      if (this.graph == null) return null;
      this.desc = this.ntn.describe(this.graph);
    }                           /* create a description if possible */
    return this.desc;           /* return the description */
  }  /* getDesc() */

  /*------------------------------------------------------------------*/
  /** Get the current graph or substructure.
   *  @return the current graph
   *  @throws IOException if a parse error occurs
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph getGraph () throws IOException
  { return this.graph; }

  /*------------------------------------------------------------------*/
  /** Get the number of nodes of the current graph.
   *  @return the number of nodes of the current graph
   *          or -1 if this number is not known
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getNodeCount ()
  { return this.nodes; }

  /*------------------------------------------------------------------*/
  /** Get the number of edges of the current graph.
   *  @return the number of edges of the current graph
   *          or -1 if this number is not known
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getEdgeCount ()
  { return this.edges; }

  /*------------------------------------------------------------------*/
  /** Get the value associated with the current graph.
   *  @return the value associated with the current graph
   *  @since  2007.02.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public float getValue ()
  { return this.value; }

  /*------------------------------------------------------------------*/
  /** Get the absolute focus support of the current substructure.
   *  @return the absolute focus support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getAbsSupp ()
  { return this.sabs; }

  /*------------------------------------------------------------------*/
  /** Get the relative focus support of the current substructure.
   *  @return the relative focus support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public float getRelSupp ()
  { return this.srel; }

  /*------------------------------------------------------------------*/
  /** Get the absolute complement support of the current substructure.
   *  @return the absolute complement support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getAbsCompl ()
  { return this.cabs; }

  /*------------------------------------------------------------------*/
  /** Get the relative support of the current substructure.
   *  @return the relative complement support
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public float getRelCompl ()
  { return this.crel; }

  /*------------------------------------------------------------------*/
  /** Create a graph reader for a given format and mode.
   *  @param  reader the reader to read from
   *  @param  mode   the read mode
   *  @param  format the name of the format/notation
   *  @return the created graph reader
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static GraphReader createReader (Reader reader, int mode,
                                          String format)
  {                             /* --- create a graph reader */
    if (format.equalsIgnoreCase("smiles"))
      return new TableReader(reader, mode, new SMILES());
    if (format.equalsIgnoreCase("smi"))
      return new TableReader(reader, mode, new SMILES());
    if (format.equalsIgnoreCase("sln"))
      return new TableReader(reader, mode, new SLN());
    if (format.equalsIgnoreCase("sdfile"))
      return new SDfileReader(reader, mode);
    if (format.equalsIgnoreCase("sdf"))
      return new SDfileReader(reader, mode);
    if (format.equalsIgnoreCase("sd"))
        return new SDfileReader(reader, mode);
    if (format.equalsIgnoreCase("mdl"))
      return new SDfileReader(reader, mode);
    if (format.equalsIgnoreCase("linog"))
      return new TableReader(reader, mode, new LiNoG());
    if (format.equalsIgnoreCase("list"))
      return new NEListReader(reader, mode);
    if (format.equalsIgnoreCase("nelist"))
      return new NEListReader(reader, mode);
    if (format.equalsIgnoreCase("nel"))
      return new NEListReader(reader, mode);
    return null;                /* evaluate the format/notation name */
  }  /* createReader() */

public boolean getIsLabel() {
	return isLabel;
}

public void setIsLabel(boolean isLabel) {
	this.isLabel = isLabel;
}

}  /* class GraphReader */
