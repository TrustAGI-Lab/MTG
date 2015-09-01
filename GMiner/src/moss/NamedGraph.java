/*----------------------------------------------------------------------
  File    : NamedGraph.java
  Contents: Named attributed graph management for substructure mining
  Author  : Christian Borgelt
  History : 2002.03.11 file created created as Graph.java
            2002.04.02 successor pointer added (graph list)
            2002.04.14 graph group added for faster counting
            2003.03.31 output of Prolog description added
            2003.08.10 Prolog output extended (edge types)
            2006.06.29 only group information retained in graph
            2006.10.23 functions for named graphs separated
            2006.10.31 adapted to refactored classes
            2007.02.15 value information readded to the graph
            2007.03.04 adapted to modified notation class
            2007.06.14 function split() added (connected components)
            2007.06.21 adapted to type managers (atoms and bonds)
            2007.10.25 general line notation parsing added
            2007.11.07 function toLogic() adapted to type managers
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

/*--------------------------------------------------------------------*/
/** Class for attributed graphs with a name and a group.
 *  <p>Named graphs also have a name and a group by which they can
 *  be classified as being in the focus or in the complement for the
 *  search. In addition, they possess a successor pointer, so that
 *  they can be connected into a singly linked list. Named graphs
 *  are used for storing the graph database to mine.</p>
 *  @author Christian Borgelt
 *  @since  2002.03.11/2006.10.23 */
/*--------------------------------------------------------------------*/
public class NamedGraph extends Graph implements Cloneable {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** group identifier: focus */
  public static final int FOCUS = 0;
  /** group identifier: complement */
  public static final int COMPL = 1;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the next graph in a list */
  public NamedGraph succ;
  /** the graph name/identifier */
  protected String     name;
  /** the value associated with the graph */
  protected float      value; 
  /** the marker for grouping (either 0 or 1, used as an array index) */
  protected int        group;
  
  /** if a graph is label (for 0:yes, 1:no for semi-supervised), Add by Shirui 25-5-2012.*/
  protected boolean isLabel;
  

  
  /**Cluster ID, added BY SHIRUI*/
  protected int clusterID;
  

  /*------------------------------------------------------------------*/
  /** Create a graph with an empty name and value and group 0.
   *  @param  ntn the notation of the graph
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph (Notation ntn)
  { this(ntn, null, 0.0F, 0); }

  /*------------------------------------------------------------------*/
  /** Create a graph with value and group 0.
   *  @param  ntn  the notation of the graph
   *  @param  name the name of the graph
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph (Notation ntn, String name)
  { this(ntn, name, 0.0F, 0); }

  /*------------------------------------------------------------------*/
  /** Create a graph of default size with a given name and group.
   *  @param  ntn   the notation of the graph
   *  @param  name  the name/identifier of the graph
   *  @param  value the associated value
   *  @param  group the group of the graph
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph (Notation ntn, String name, float value, int group)
  { this(ntn, 16, 16, name, value, group); }

  /*------------------------------------------------------------------*/
  /** Create a graph with a given name, group and size.
   *  @param  ntn     the notation of the graph
   *  @param  nodecnt the expected number of nodes
   *  @param  edgecnt the expected number of edges
   *  @param  name    the name/identifier of the graph
   *  @param  value   the associated value
   *  @param  group   the group of the graph
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph (Notation ntn, int nodecnt, int edgecnt,
                     String name, float value, int group)
  {                             /* --- create an empty graph */
	  this(ntn, 16, 16, name, value, group, true);
  }  /* NamedGraph() */

  /**
   * add by Shirui
   * @param ntn
   * @param nodecnt
   * @param edgecnt
   * @param name
   * @param value
   * @param group
   * @param label
   */
  public NamedGraph (Notation ntn, int nodecnt, int edgecnt,
          String name, float value, int group, boolean label){
	  
    super(ntn,nodecnt,edgecnt); /* initialize the graph */
    this.name  = name;          /* note the graph name, */
    this.value = value;         /* the associated value, */
    this.group = group;         /* and the group flag */
    this.succ  = null;          /* there is no list */
    this.isLabel = label;
  }
  


/*------------------------------------------------------------------*/
  /** Turn a graph into a named graph.
   *  <p>Note that the constituents of the graph are not copied, so
   *  changes to the given graph affect the created named graph.</p>
   *  @param  graph the graph to turn into a named graph
   *  @since  2007.06.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph (Graph graph)
  {  
	  this(graph, null, 0.0F, 0); 
	  if(graph instanceof NamedGraph){
		  	NamedGraph ng = (NamedGraph) graph;
		    this.name  = ng.name;    /* copy the name, */
		    this.value = ng.value;   /* the associated value, */
		    this.group = ng.group;   /* and the group flag */
		    this.weight = ng.weight; //add by shirui
		    this.weight2 = ng.weight2;
	  }
  }

  /*------------------------------------------------------------------*/
  /** Turn a graph into a named graph.
   *  <p>Note that the constituents of the graph are not copied, so
   *  changes to the given graph affect the created named graph.</p>
   *  @param  graph the graph to turn into a named graph
   *  @param  name  the identifier/name of the graph
   *  @since  2007.06.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph (Graph graph, String name)
  { this(graph, name, 0.0F, 0); }

  /*------------------------------------------------------------------*/
  /** Turn a graph into a named graph.
   *  <p>Note that the constituents of the graph are not copied, so
   *  changes to the given graph affect the created named graph.</p>
   *  @param  graph the graph to turn into a named graph
   *  @param  name  the name/identifier of the graph
   *  @param  value the associated value
   *  @param  group the group of the graph
   *  @since  2007.06.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph (Graph graph, String name, float value, int group)
  {                             /* --- create a named graph */
    this.nodecnt = graph.nodecnt;
    this.nodes   = graph.nodes; /* get the node array */
    this.edgecnt = graph.edgecnt;
    this.edges   = graph.edges; /* get the edge array */
    this.ntn     = graph.ntn;   /* get the notation */
    this.coder   = graph.coder; /* and the node type recoder */
    this.name    = name;        /* store the graph name, */
    this.value   = value;       /* the associated value, */
    this.group   = group;       /* and the group flag */
    this.succ    = null;        /* there is no list */
  }  /* NamedGraph() */
  
  /**
   * add by shirui
   * @param graph
   * @param name
   * @param value
   * @param group
   * @param label
   */
  public NamedGraph (Graph graph, String name, float value, int group, boolean label)
  {                             /* --- create a named graph */
	 this(graph,name,value,group);
    this.isLabel = label;
  }  /* NamedGraph() */

  /*------------------------------------------------------------------*/
  /** Clone a named graph.
   *  <p>This function returns a deep copy of a named graph
   *  (all constituents of the named graph are copied).
   *  It is intended mainly for debugging purposes.</p>
   *  @param  graph the named graph to duplicate
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected NamedGraph (NamedGraph graph)
  {                             /* --- create a named graph */
    super(graph);               /* copy the underlying graph */
    this.name  = graph.name;    /* copy the name, */
    this.value = graph.value;   /* the associated value, */
    this.group = graph.group;   /* and the group flag */
    this.weight = graph.weight; //add by shirui
    this.weight2 = graph.weight2;
  }  /* NamedGraph() */

  /*------------------------------------------------------------------*/
  /** Create a clone of this named graph.
   *  <p>This function simply returns <code>new NamedGraph(this)</code>.
   *  It is intended mainly for debugging purposes.</p>
   *  @return a clone of this named graph
   *  @see    #NamedGraph(NamedGraph)
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Object clone ()
  { return new NamedGraph(this); }

  /*------------------------------------------------------------------*/
  /** Set the name of this graph.
   *  @param  name the name to set
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setName (String name)
  { this.name = name; }

  /*------------------------------------------------------------------*/
  /** Get the name of this graph.
   *  @return the name of this graph
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String getName ()
  { return this.name; }

  /*------------------------------------------------------------------*/
  /** Set the value of this graph.
   *  @param  value the value to set
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setValue (float value)
  { this.value = value; }

  /*------------------------------------------------------------------*/
  /** Get the value of this graph.
   *  @return the value of this graph
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public float getValue ()
  { return this.value; }
  
  public boolean getLabel() {
	return isLabel;
}

public void setLabel(boolean label) {
	this.isLabel = label;
}

  /*------------------------------------------------------------------*/
  /** Set the group of this graph.
   *  @param  group the group to set
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setGroup (int group)
  { this.group = group; }

  /*------------------------------------------------------------------*/
  /** Get the group of this graph.
   *  @return the group of this graph
   *  @since  2006.10.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getGroup ()
  { return this.group; }

  /*------------------------------------------------------------------*/
  /** Split a graph into its connected components.
   *  @return a list of graphs representing the connected components
   *          or the graph itself if there is only one component
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public NamedGraph split ()
  {                             /* --- split into connected comps. */
    int        i, k, n, m;      /* loop variables */
    int        ncc;             /* number of connected components */
    Node       r;               /* to traverse the nodes */
    Edge       e;               /* to traverse the edges */
    NamedGraph list, g;         /* (list of) created graph(s) */

    if (this.nodecnt <= 1)      /* if there is at most one node, */
      return this;              /* there is only one component */
    for (i = n = this.nodecnt; --i >= 0; )
      this.nodes[i].mark = -1;  /* unmark all nodes */
    for (i = ncc = 0; i < n; i++) {
      r = this.nodes[i];        /* traverse the unmarked nodes */
      if (r.mark < 0) r.mark(ncc++);
    }                           /* mark their connected component */
    if (ncc <= 1) {             /* check for only one component */
      this.mark(-1); return this; }
    for (i = this.edgecnt; --i >= 0; ) {
      e = this.edges[i]; e.mark = e.src.mark; }
    Arrays.sort(this.nodes, 0, i = n);
    Arrays.sort(this.edges, 0, k = this.edgecnt);
    list = null;                /* sort the nodes and edges */
    while (--ncc >= 0) {        /* traverse the connected components */
      for (n = i; --i >= 0; )   /* traverse the nodes */
        if (this.nodes[i].mark != ncc) break;
      i++;                      /* find first node of component */
      for (m = k; --k >= 0; )   /* traverse the edges */
        if (this.edges[k].mark != ncc) break;
      k++;                      /* find first edge of component */
      g = new NamedGraph(this.ntn, n-i, m-k,
                         this.name, this.value, this.group);
      System.arraycopy(this.nodes, i, g.nodes, 0, g.nodecnt = n-i);
      System.arraycopy(this.edges, k, g.edges, 0, g.edgecnt = m-k);
      g.coder = this.coder;     /* create a connected component graph */
      g.succ  = list; list = g; /* copy the relevant information and */
    }                           /* add it to the list of components */
    this.mark(-1);              /* unmark all nodes and edges */
    return list;                /* return the connected components */
  }  /* split() */

  /*------------------------------------------------------------------*/
  /** Create a Prolog description of this graph.
   *  <p>The graph is described by a set of predicates, one per node
   *  and one per edge, which list the graph they belong to together
   *  with their indices and types.</p>
   *  @return a Prolog description of this graph
   *  @since  2003.03.31 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String toLogic ()
  {                             /* --- produce a Prolog description */
    int          i, type;       /* loop variable, type buffer */
    Node         node;          /* to traverse the nodes */
    Edge         edge;          /* to traverse the edges */
    TypeMgr      ntmgr;         /* type manager for nodes */
    TypeMgr      etmgr;         /* type manager for edges */
    StringBuffer s;             /* buffer for Prolog string */

    ntmgr = this.ntn.getNodeMgr();
    etmgr = this.ntn.getEdgeMgr();
    s = new StringBuffer();     /* create buffer for description */
    for (i = 0; i < this.nodecnt; i++) {
      node = this.nodes[i];     /* traverse the nodes */
      node.mark = i;            /* and mark them */
      type = node.getType();    /* get the node type and */
      if (this.coder != null)   /* decode it if necessary */
        type = this.coder.decode(type);
      s.append("node(\"");      /* print the node predicate */
      s.append(this.name);           s.append("\", ");
      s.append(i);                   s.append(", \"");
      s.append(ntmgr.getName(type)); s.append("\").\n");
    }
    for (i = 0; i < this.edgecnt; i++) {
      edge = this.edges[i];     /* traverse the edges */
      type = edge.getType();    /* get the edge type */
      s.append("edge(\"");      /* print the edge predicate */
      s.append(this.name);           s.append("\", ");
      s.append(edge.src.mark);       s.append(", ");
      s.append(edge.dst.mark);       s.append(", \"");
      s.append(etmgr.getName(type)); s.append("\").\n");
    }
    if (this.group > 0) s.append("in");
    s.append("active(\"");      /* append an activity indicator */
    s.append(this.name);        /* (predicate with the graph name */
    s.append("\").\n");         /* as the argument) */
    return s.toString();        /* return the created description */
  }  /* toLogic() */

  public double getWeight() {
	return weight;
}

public void setWeight(double weight) {
	this.weight = weight;
}

public int getClusterID() {
	return clusterID;
}

public void setClusterID(int clusterID) {
	this.clusterID = clusterID;
}

/*------------------------------------------------------------------*/
  /** Main function for testing some basic functionality.
   *  @param  args the command line arguments
   *  @since  2006.01.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static void main (String args[])
  {                             /* --- main function for testing */
    Notation   ntn;             /* graph notation */
    NamedGraph graph;           /* created graph */
    int[]      masks;           /* masks for node and edge types */

    if ((args.length < 1)       /* if no arguments given */
    ||  (args.length > 2)) {    /* or wrong number of arguments */
      System.err.println("usage: java moss.NamedGraph"
                        +" [<name> <string>|<string>]");
      return;                   /* print a usage message */
    }                           /* and abort the program */

    if (args.length < 2) {      /* if only one argument is given, */
      try {                     /* try to parse it as SMILES */
        ntn   = new SMILES();   /* create a SMILES object */
        graph = new NamedGraph(ntn.parse(new StringReader(args[0]))); }
      catch (IOException e) {   /* parse the argument */
        System.out.println(e.getMessage()); return; }
      for (graph = graph.split(); graph != null; graph = graph.succ)
        System.out.println(graph);
      return;                   /* split the graph into components */
    }                           /* and print these components */

    masks = new int[4];         /* create node and edge masks */
    masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
    masks[1] = masks[3] = BondTypeMgr.BONDMASK;

    try {                       /* try SMILES */
      System.out.print("SMILES: ");
      ntn   = new SMILES();     /* create a SMILES object */
      graph = new NamedGraph(ntn.parse(new StringReader(args[1])),"G");
      graph.maskTypes(masks);   /* parse the argument */
      System.out.print(graph.toLogic()); return; }
    catch (IOException e) {     /* describe graph in logic */
      System.out.println(e.getMessage()); }

    try {                       /* try SYBYL line notation (SLN) */
      System.out.print("SLN   : ");
      ntn   = new SLN();        /* create an SLN object */
      graph = new NamedGraph(ntn.parse(new StringReader(args[1])),"G");
      graph.maskTypes(masks);   /* parse the argument */
      System.out.print(graph.toLogic()); return; }
    catch (IOException e) {     /* describe graph in logic */
      System.err.println(e.getMessage()); }

    try {                       /* try general line notation */
      System.out.print("SLN   : ");
      ntn   = new LiNoG();      /* create a LiNoG object */
      graph = new NamedGraph(ntn.parse(new StringReader(args[1])),"G");
      /* no type masking */     /* parse the argument */
      System.out.print(graph.toLogic()); return; }
    catch (IOException e) {     /* describe graph in logic */
      System.err.println(e.getMessage()); }
  }  /* main() */

}  /* class NamedGraph */
