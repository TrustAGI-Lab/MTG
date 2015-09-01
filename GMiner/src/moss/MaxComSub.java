/*----------------------------------------------------------------------
  File    : MaxComSub.java
  Contents: finding maximum common subgraphs
  Author  : Christian Borgelt
  History : 2006.11.19 file created
            2006.11.22 bug in findByEdge fixed (unmap isolated nodes)
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.StringReader;

/*--------------------------------------------------------------------*/
/** Class for finding maximum common subgraphs.
 *  @author Christian Borgelt
 *  @since  2006.11.19 */
/*--------------------------------------------------------------------*/
public class MaxComSub {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the first graph of the pair
   *  for which to find the maximum common subgraph */
  protected Graph g1;
  /** the second graph of the pair
   *  for which to find the maximum common subgraph */
  protected Graph g2;
  /** the costs of the best sequence of edit operations transforming
   *  graph 1 into graph 2 */
  protected int   costs;
  /** the mapping of the nodes of graph 1 to graph 2 */
  protected int[] nodemap;
  /** the mapping of the edges of graph 1 to graph 2 */
  protected int[] edgemap;
  /** the found maximum common subgraph (created on demand) */
  protected Graph mcs;

  /*------------------------------------------------------------------*/
  /** Find the maximum common subgraph of two given graphs.
   *  @param  g1 the first  graph
   *  @param  g2 the second graph
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MaxComSub (Graph g1, Graph g2)
  { this(g1, g2, false); }

  /*------------------------------------------------------------------*/
  /** Find the maximum common subgraph of two given graphs.
   *  @param  g1     the first  graph
   *  @param  g2     the second graph
   *  @param  byNode whether to do the search by node mappings
   *                 (default: edge mappings)
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MaxComSub (Graph g1, Graph g2, boolean byNode)
  {                             /* --- find maximum common subgraph */
    int i;                      /* loop variable */

    this.g1 = g1;               /* note the given graphs and */
    this.g2 = g2;               /* create node and edge maps */
    this.nodemap = new int[g1.nodecnt];
    this.edgemap = new int[g1.edgecnt];
    this.costs   = Integer.MAX_VALUE;
    this.mcs     = null;        /* clear costs and result graph */
    if (byNode) {               /* if to find mapping by edges */
      for (i = g1.nodecnt; --i >= 0; ) g1.nodes[i].mark = -1;
      for (i = g1.edgecnt; --i >= 0; ) g1.edges[i].mark = -1-i;
      for (i = g2.nodecnt; --i >= 0; ) g2.nodes[i].mark = -1;
      for (i = g2.edgecnt; --i >= 0; ) g2.edges[i].mark = -1-i;
      this.findByNode(g1.nodecnt, 0, g1.nodecnt -g2.nodecnt); }
    else {                      /* if to find mapping by nodes */
      for (i = g1.nodecnt; --i >= 0; ) g1.nodes[i].mark = -1-i;
      for (i = g1.edgecnt; --i >= 0; ) g1.edges[i].mark = -1;
      for (i = g2.nodecnt; --i >= 0; ) g2.nodes[i].mark = -1-i;
      for (i = g2.edgecnt; --i >= 0; ) g2.edges[i].mark = -1;
      this.findByEdge(g1.edgecnt, 0, g1.edgecnt -g2.edgecnt);
    }                           /* call corresponding function */
  }  /* MaxComSub() */

  /*------------------------------------------------------------------*/
  /** Record a found mapping.
   *  @param  costs the accumulated costs of edit operations
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void record (int costs)
  {                             /* --- record a found mapping */
    int i;                      /* loop variable */

    if (costs >= this.costs)    /* if new map is not better, abort, */
      return;                   /* otherwise sum unmapped nodes/edges */
    for (i = this.g2.nodecnt; --i >= 0; )
      if (this.g2.nodes[i].mark < 0) costs++;
    for (i = this.g2.edgecnt; --i >= 0; )
      if (this.g2.edges[i].mark < 0) costs++;
    if (costs >= this.costs)    /* if new map is not better, abort, */
      return;                   /* otherwise store the new costs */
    this.costs = costs;         /* and copy the new map */
    for (i = this.g1.nodecnt; --i >= 0; )
      this.nodemap[i] = this.g1.nodes[i].mark;
    for (i = this.g1.edgecnt; --i >= 0; )
      this.edgemap[i] = this.g1.edges[i].mark;
  }  /* record() */

  /*------------------------------------------------------------------*/
  /** Find the maximum common subgraph by node mappings.
   *  @param  n     the number of unprocessed nodes
   *  @param  costs the accumulated costs of edit operations
   *  @param  min   the minimum additional costs of edit operations
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void findByNode (int n, int costs, int min)
  {                             /* --- find MCS by node mappings */
    int  i, j, k, x, c;         /* loop variables, buffers */
    Node n1, n2;                /* original and image node */
    Edge e1, e2;                /* to traverse the edges */
    Node a1, a2;                /* to traverse the adjacent nodes */

    if (costs +Math.abs(min) >= this.costs)
      return;                   /* if cannot improve current solution */
    if (n <= 0) {               /* check for a complete mapping */
      this.record(costs); return; }
    n1 = this.g1.nodes[--n];    /* get the next node to map */
    for (i = this.g2.nodecnt; --i >= 0; ) {
      n2 = this.g2.nodes[i];    /* traverse the image nodes */
      if ((n2.mark >= 0) || (n2.type != n1.type))
        continue;               /* skip unsuited images */
      n2.mark = n;              /* map the two nodes onto each other, */
      n1.mark = i;              /* then process the incident edges */
      for (c = 0, j = n1.deg; --j >= 0; ) {
        e1 = n1.edges[j];       /* traverse the incident edges */
        a1 = (e1.src != n1) ? e1.src : e1.dst;
        if (a1.mark < 0) continue;    /* check for a mapped node */
        a2 = this.g2.nodes[a1.mark];  /* and get the image node */
        for (k = n2.deg; --k >= 0; ) {
          e2 = n2.edges[k];     /* traverse the possible images */
          if ((e2.type == e1.type)
          &&  ((e2.src == a2) || (e2.dst == a2))) {
            x = e1.mark; e1.mark = -e2.mark-1; e2.mark = -x-1; break; }
        }                       /* if them edges match, map them */
        if (k < 0) c++;         /* count the edges that */
      }                         /* could not be mapped */
      this.findByNode(n, costs+c, min);  /* recurse to find map */
      for (j = n1.deg; --j >= 0; ) {
        e1 = n1.edges[j];       /* traverse the incident edges */
        if (e1.mark < 0) continue;
        e2 = this.g2.edges[x = e1.mark];
        e1.mark = -e2.mark-1; e2.mark = -x-1;
      }                         /* remove the edge mapping */
      n1.mark = n2.mark = -1;   /* and the node mapping */
    }                           /* in the end also try the empty map */
    this.findByNode(n, costs+1 +n1.deg, min-1);
  }  /* findByNode() */

  /*------------------------------------------------------------------*/
  /** Check whether two nodes match.
   *  @param  n1 the first  node
   *  @param  n2 the second node
   *  @return whether the two nodes match
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private static boolean match (Node n1, Node n2, Graph g)
  { if (n1.mark >= 0) return (n2 == g.nodes[n1.mark]);
    return (n2.mark < 0) && (n1.type == n2.type); }

  /*------------------------------------------------------------------*/
  /** Map a node onto another (or unmap them).
   *  @param  n1 the first  node
   *  @param  n2 the second node
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private static void map (Node n1, Node n2)
  { int x = n1.mark; n1.mark = -n2.mark-1; n2.mark = -x-1; }

  /*------------------------------------------------------------------*/
  /** Find the maximum common subgraph by edge mappings.
   *  @param  n     the number of unprocessed edges
   *  @param  costs the accumulated costs of edit operations
   *  @param  min   the minimum additional costs of edit operations
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void findByEdge (int n, int costs, int min)
  {                             /* --- find MCS by edge mappings */
    int  i, k, m;               /* loop variables, buffer */
    Edge e1, e2;                /* source and destination edge */
    Node n1, n2;                /* to access the nodes */

    if (costs +Math.abs(min) >= this.costs)
      return;                   /* if cannot improve current solution */
    if (n <= 0) {               /* check for a complete mapping */
      for (i = this.g1.nodecnt; --i >= 0; ) {
        n1 = this.g1.nodes[i];  /* traverse the unmapped nodes */
        if (n1.mark >= 0) continue;
        for (k = this.g2.nodecnt; --k >= 0; ) {
          n2 = this.g2.nodes[k];/* traverse possible images */
          if ((n2.mark < 0) && (n2.type == n1.type)) {
            n1.mark = k; n2.mark = i+k+1; break; }
        }                       /* try to find an image node */
        if (k < 0) costs++;     /* if there is none, increment costs */
      }                         /* (this node has to be deleted) */
      this.record(costs);       /* record the new mapping */
      for (i = this.g1.nodecnt; --i >= 0; ) {
        n1 = this.g1.nodes[i];  /* traverse the isolated nodes */
        if (n1.mark <  0) continue;
        n2 = this.g2.nodes[n1.mark];
        if (n2.mark == i) continue;
        n1.mark = -1-i; n2.mark = -(n2.mark-i);
      }                         /* umap the mapped isolated nodes */
      return;                   /* and return from the recursion */
    }
    e1 = this.g1.edges[--n];    /* get the next edge to map */
    for (k = this.g2.edgecnt; --k >= 0; ) {
      e2 = this.g2.edges[k];    /* traverse the destination edges */
      if ((e2.mark >= 0) || (e2.type != e1.type))
        continue;               /* skip unsuited destinations */
      if (match(e1.src, e2.src, g2) && match(e1.dst, e2.dst, g2)) {
        e1.mark = k;            /* map the two edges onto each other, */
        e2.mark = n; m = 0;     /* then map source and dest. node */
        if (e1.src.mark < 0) { map(e1.src, e2.src); m |= 1; }
        if (e1.dst.mark < 0) { map(e1.dst, e2.dst); m |= 2; }
        this.findByEdge(n, costs, min);  /* recurse to find map */
        if ((m & 1) != 0) map(e1.src, e2.src);
        if ((m & 2) != 0) map(e1.dst, e2.dst);
        e1.mark = e2.mark = -1; /* unmap edges and nodes */
      }                         /* (restore original markers) */
      if (match(e1.src, e2.dst, g2) && match(e1.dst, e2.src, g2)) {
        e1.mark = k;            /* map the two edges onto each other */
        e2.mark = n; m = 0;     /* then map source and dest. node */
        if (e1.src.mark < 0) { map(e1.src, e2.dst); m |= 1; }
        if (e1.dst.mark < 0) { map(e1.dst, e2.src); m |= 2; }
        this.findByEdge(n, costs, min);  /* recurse to find map */
        if ((m & 1) != 0) map(e1.src, e2.dst);
        if ((m & 2) != 0) map(e1.dst, e2.src);
        e1.mark = e2.mark = -1; /* unmap edges and nodes */
      }                         /* (restore original markers) */
    }                           /* in the end also try the empty map */
    this.findByEdge(n, costs+1, min-1);
  }  /* findByEdge() */

  /*------------------------------------------------------------------*/
  /** Get the costs of the best sequence of edit operations.
   *  @return the costs of the best sequence of edit operations
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getCosts ()
  { return this.costs; }

  /*------------------------------------------------------------------*/
  /** Get the mapping of the nodes of graph 1 to the nodes of graph 2.
   *  @return the node map (graph index to subgraph index)
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int[] getNodeMap ()
  { return this.nodemap; }

  /*------------------------------------------------------------------*/
  /** Get the mapping of the edges of graph 1 to the edges of graph 2.
   *  @return the edge map (graph index to subgraph index)
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int[] getEdgeMap ()
  { return this.edgemap; }

  /*------------------------------------------------------------------*/
  /** Get the mapping of the edges of graph 1 to the edges of graph 2.
   *  @return the maximum common subgraph
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph getGraph ()
  {                             /* --- get MCS as a graph */
    int  i, k, n;               /* loop variable, counters */
    Node node;                  /* to traverse the nodes */
    Edge edge;                  /* to traverse the edges */

    if (this.mcs != null)       /* if a graph already exists, */
      return this.mcs;          /* simply return it */
    for (n = 0, i = this.g1.nodecnt; --i >= 0; )
      if (this.nodemap[i] >= 0) n++;  /* count the common nodes */
    for (k = 0, i = this.g1.edgecnt; --i >= 0; )
      if (this.edgemap[i] >= 0) k++;  /* count the common edges */
    this.mcs = new Graph(this.g1.ntn, n, k);
                                /* create the max. common subgraph */
    for (i = this.g1.nodecnt; --i >= 0; ) {
      if (this.nodemap[i] < 0) continue;
      node = this.g1.nodes[i];  /* traverse the common nodes */
      node.mark = this.mcs.addNode(node.type);
    }                           /* create nodes and note their index */
    for (i = this.g1.edgecnt; --i >= 0; ) {
      if (this.edgemap[i] < 0) continue;
      edge = this.g1.edges[i];  /* traverse the common edges */
      this.mcs.addEdge(edge.src.mark, edge.dst.mark, edge.type);
    }                           /* create edges */
    return this.mcs;            /* return the created graph */
  }  /* getGraph() */

  /*------------------------------------------------------------------*/
  /** Main function for basic testing basic functionality.
   *  @param  args the command line arguments
   *  @since  2006.11.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static void main (String args[])
  {                             /* --- main function for testing */
    Notation  ntn;              /* graph notation */
    Graph     g1, g2;           /* created graphs */
    int[]     masks;            /* masks for node and edge types */
    MaxComSub mcs;              /* maximum common subgraph */
    Long time1 = System.currentTimeMillis();
    if ((args.length != 2)      /* if wrong number of arguments */
    &&  (args.length != 3)) {   /* (need 2 or 3 arguments) */
      System.err.println("usage: java moss.MaxComSub <graph> <graph>");
      return;                   /* print a usage message */
    }                           /* and abort the program */
    masks = new int[4];         /* create node and edge masks */
    masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
    masks[1] = masks[3] = BondTypeMgr.BONDMASK;
    try {                       /* parse SMILES descriptions */
      ntn = new SMILES();       /* create a SMILES object */
      g1 = ntn.parse(new StringReader(args[0]));
      g1.maskTypes(masks);      /* parse the first argument */
      g2 = ntn.parse(new StringReader(args[1]));
      g2.maskTypes(masks);      /* parse the second argument */
      System.out.println("Graph reading over!");
      System.out.println("Graph1:"+g1+"\tnode:"+g1.getNodeCount()+"\tEdge:"+g1.getEdgeCount());
      System.out.println("Graph2:"+g2+"\tnode:"+g2.getNodeCount()+"\tEdge:"+g2.getEdgeCount());
      
      mcs = new MaxComSub(g1, g2); 
      System.out.println("Construction over!");  
    }
    
    catch (IOException e) {     /* compute maximum common subgraph */
      System.err.println(e.getMessage()); return; }

    System.out.println("MCS   :");
    Graph mcsgraph = mcs.getGraph();
    System.out.println(mcsgraph +" : " +mcs.getCosts()+"\tnode:"+mcsgraph.getNodeCount()+"\tEdge:"+mcsgraph.getEdgeCount());
    System.out.println("Distance:"+(1-1.0*mcsgraph.getNodeCount()/Math.max(g1.getNodeCount(), g2.getNodeCount())));
    Long time2 = System.currentTimeMillis();
    System.out.println("Time:"+(time2-time1)/1000.0);
    
    g1.prepareEmbed();
    g2.prepareEmbed();
    
    System.out.println("G2 contains G1:"+g2.contains(g1));
    Long time3 = System.currentTimeMillis();
    
    System.out.println("Time2:"+(time3-time2)/1000.0);
    
  }  /* main() */

}  /* class MaxComSub */
