/*----------------------------------------------------------------------
  File    : Node.java
  Contents: class for nodes of attributed (labeled/typed) graphs
  Author  : Christian Borgelt
  History : 2002.03.11 file created (as Atom.java)
            2002.03.14 memory optimization added
            2002.03.19 bug in function sortEdges() fixed
            2002.03.22 second constructor added
            2002.03.28 node type changed to protected
            2005.07.20 parameter 'masks' removed from sortEdges()
            2005.07.23 sorting order of a node's edges inverted
            2005.08.01 label and corresponding mask removed
            2006.05.12 edge comparison in sortEdges() simplified
            2006.08.10 charge coding changed, chain type added
            2006.08.13 insertion sort only for small edge arrays
            2006.10.27 definition of special types modified
            2006.10.31 renamed to Node.java, atom-related stuff removed
            2006.11.16 functions encode() and decode() added
            2007.03.23 function to mark a connected component added
            2007.06.24 bug in function sortEdges() fixed (deg > 12)
            2007.06.25 function isWildcard(), constant WILDCARD added
            2007.06.14 function compareTo() added (node markers)
            2007.11.05 functions getDegree() and getEdge() added
            2008.03.07 Arrays.sort invocation adapted to Java 1.5
            2009.08.06 adapted to java generics (Comparable)
            2009.08.13 constants based on type manager constants
            2011.02.15 node orbits added (found with canonical form)
----------------------------------------------------------------------*/
package moss;

import java.util.Comparator;
import java.util.Arrays;

/*--------------------------------------------------------------------*/
/** Class for nodes of an attributed (labeled/typed) graph.
 *  <p>A node records its type (attribute/label) and all edges
 *  that are incident to it in the graph it is part of.</p>
 *  <p>Note that only 30 bits are actually available for the type
 *  of the node. The two most significant bits are reserved as flags
 *  for special purposes, for example, for marking chain nodes.</p>
 *  @author Christian Borgelt
 *  @since  2002.03.11 */
/*--------------------------------------------------------------------*/
public class Node implements Comparable<Node> {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** the mask for the base type */
  public static final int TYPEMASK = TypeMgr.BASEMASK;
  /** the mask for the node flags (wildcard and chain) */
  protected static final int FLAGMASK = TypeMgr.FLAGMASK;
  /** the special code for a wildcard type */
  protected static final int WILDCARD = TypeMgr.WILDCARD;
  /** the flag for a chain node (representing several equal nodes) */
  protected static final int CHAIN    = TypeMgr.SPECIAL;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the type (attribute/label) of a node (including flags) */
  public int    type;
  /** a marker for internal use (e.g. for a substructure) */
  public int    mark;
  /** the representative of the orbit of the node */
  protected int    orbit;
  /** the current number of incident edges (node degree) */
  public int    deg;
  /** the array of incident edges (may not be fully used) */
  public Edge[] edges;

  /*------------------------------------------------------------------*/
  /** Create a node with a given type (attribute/label).
   *  @param  type the type of the node to create
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Node (int type)
  { this(type, 4); }

  /*------------------------------------------------------------------*/
  /** Create a node of a given type and given edge array size.
   *  <p>The node is created in such a way that it can accomodate
   *  references to <code>deg</code> edges. Nevertheless more edges
   *  may be added later. The parameter only serves the purpose to
   *  set the proper size if it is already known in advance.</p>
   *  @param  type the type of the node to create
   *  @param  size the expected number of edges
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Node (int type, int size)
  {                             /* --- create an node */
    this.type  = type;          /* note the type of the node */
    this.orbit = this.deg = 0;  /* create an empty edge array */
    this.edges = new Edge[size];/* of the given size */
  }  /* Node() */

  /*------------------------------------------------------------------*/
  /** Get the full type (attribute/label) of a node.
   *  @return the type of the node
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getType ()
  { return this.type; }

  /*------------------------------------------------------------------*/
  /** Get the base type (attribute/label) of a node.
   *  @return the type of the node
   *  @since  2009.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getBaseType ()
  { return this.type & TYPEMASK; }

  /*------------------------------------------------------------------*/
  /** Mask the edge type with the given mask.
   *  @param  mask the mask for the node type
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void maskType (int mask)
  { this.type &= mask | FLAGMASK; }

  /*------------------------------------------------------------------*/
  /** Check whether this is a special node (wildcard or chain).
   *  @return whether this is a special node
   *  @since  2006.10.31 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isSpecial ()
  { return (this.type & FLAGMASK) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether this is a wildcard node.
   *  @return whether this is a wildcard node
   *  @since  2007.06.25 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isWildcard ()
  { return (this.type & WILDCARD) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether this is a chain node.
   *  @return whether this is a chain node
   *  @since  2006.10.31 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isChain ()
  { return (this.type & CHAIN) != 0; }

  /*------------------------------------------------------------------*/
  /** Encode the node type.
   *  @param  coder the recoder to encode the node type with
   *  @since  2006.11.16 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void encode (Recoder coder)
  { this.type = coder.encode(this.type & TypeMgr.BASEMASK)
              | (this.type & FLAGMASK); }

  /*------------------------------------------------------------------*/
  /** Decode the node type.
   *  @param  coder the recoder to decode the node type with
   *  @since  2006.11.16 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void decode (Recoder coder)
  { this.type = coder.decode(this.type & TypeMgr.BASEMASK)
              | (this.type & FLAGMASK); }

  /*------------------------------------------------------------------*/
  /** Get the degree of the node.
   *  @return the degree of the node
   *  @since  2007.11.05 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getDegree ()
  { return this.deg; }

  /*------------------------------------------------------------------*/
  /** Add an edge to a node.
   *  <p>It is not checked whether the source or the destination of
   *  the edge coincide with this node (as it should be).</p>
   *  @param  edge the edge to be added to the node
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void addEdge (Edge edge)
  {                             /* --- add an edge to an node */
    Edge[] old = this.edges;    /* buffer for the old edge array */
    int    max = old.length;    /* (new) array size */

    if (this.deg >= max) {      /* if the edge array is full */
      this.edges = new Edge[max +((max > 4) ? max >> 1 : 4)];
      System.arraycopy(old, 0, this.edges, 0, this.deg);
    }                           /* enlarge array and copy edges */
    this.edges[this.deg++] = edge;
  }  /* addEdge() */            /* add the new edge to the array */

  /*------------------------------------------------------------------*/
  /** Get an edge of the node.
   *  @param  index the index of the edge (w.r.t. the node)
   *  @return the edge with the given index
   *  @since  2007.11.05 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Edge getEdge (int index)
  { return this.edges[index]; }

  /*------------------------------------------------------------------*/
  /** Optimize memory usage.
   *  <p>This function shrinks the edge array to the minimal size that
   *  is necessary to hold the current number of edges and thus tries
   *  to reduce the memory consumption.</p>
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void opt ()
  {                             /* --- optimize memory usage */
    Edge[] old = this.edges;    /* buffer for the old edge array */
    if (old.length > this.deg){ /* if the array has not minimum size */
      this.edges = new Edge[this.deg];
      System.arraycopy(old, 0, this.edges, 0, this.deg);
    }                           /* shrink the array to needed size */
  }  /* opt() */

  /*------------------------------------------------------------------*/
  /** Recursive function to mark a connected component.
   *  @param  mark the value with which to mark the nodes
   *  @since  2007.03.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void mark (int mark)
  {                             /* --- recursively mark nodes */
    this.mark = mark;           /* mark the node as visited */
    for (int i = this.deg; --i >= 0; ) {
      Edge e = this.edges[i];   /* traverse the incident edges */
      Node n = (e.src != this) ? e.src : e.dst;
      if (n.mark < 0) n.mark(mark);
    }                           /* recursively mark unvisited nodes */
  }  /* mark() */

  /*------------------------------------------------------------------*/
  /** Compare two nodes (w.r.t. their marker values).
   *  <p>This function is needed in <code>NamedGraph.split()</code>
   *  (indirectly through </code>Arrays.sort()</code>).</p>
   *  @param  obj the node to compare to
   *  @return the sign of the difference of the node markers, that is,
   *          <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the marker of this node is less than, equal to, or
   *          greater than the marker of the node given as an argument
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int compareTo (Node obj)
  {                             /* --- compare two nodes */
    if (this.mark < obj.mark) return -1;
    if (this.mark > obj.mark) return +1;
    return 0;                   /* return sign of the difference */
  }  /* compareTo() */          /* of the node markers */

  /*------------------------------------------------------------------*/
  /** Sort the edges that are incident to the node.
   *  <p>The edges are sorted w.r.t. their type, the type of the node
   *  they lead to, and the marker of the edge. This order is exploited
   *  in several functions to terminate loops early (by removing the
   *  need to check all edges that are incident to a node).</p>
   *  <p>Since in normal molecules there should be no more than four
   *  edges, insertion sort is the fastest method. However, for more
   *  than 12 edges the standard function <code>Arrays.sort()</code>
   *  is used, so that it is reasonably fast for general graphs.</p>
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void sortEdges ()
  {                             /* --- sort the edges of a node */
    int  i, k;                  /* loop variables, indices */
    Edge e, x;                  /* to traverse/exchange the edges */
    int  d, t;                  /* type of the destination node */

    if (this.deg > 12) {        /* if not very few edges to sort */
      Arrays.sort(this.edges, 0, this.deg, new Comparator<Edge> () {
        public int compare (Edge e1, Edge e2) {
          if (e1.type > e2.type) return +1;
          if (e1.type < e2.type) return -1;
          Node d1 = (e1.src != Node.this) ? e1.src : e1.dst;
          Node d2 = (e2.src != Node.this) ? e2.src : e2.dst;
          if (d1.type > d2.type) return +1;
          if (d1.type < d2.type) return -1;
          if (e1.mark > e2.mark) return +1;
          if (e1.mark < e2.mark) return -1;
          return 0;             /* compare the type of the edges, */
        } } );                  /* then the types of their dest. */
      return;                   /* nodes and finally the edge markers */
    }
    for (i = 0; ++i < this.deg; ) {
      e = this.edges[k = i];    /* traverse the edges to insert */
      d = (e.src != this) ? e.src.type : e.dst.type;
      do {                      /* traverse the preceding edges */
        x = this.edges[k-1];    /* (find the place for insertion) */
        if (e.type >  x.type)   /* if the edge type is greater, */
          break;                /* the insertion point is found */
        if (e.type == x.type) { /* if the edge type is identical */
          t = (x.src != this) ? x.src.type : x.dst.type;
          if ((d > t) || ((d == t) && (e.mark >= x.mark)))
            break;              /* compare the node type */
        }                       /* and maybe the edge marker */
        this.edges[k] = x;      /* shift the edge upwards */
      } while (--k > 0);        /* while not at start of array */
      this.edges[k] = e;        /* store the edge to insert */
    }                           /* at the position found */
    /* If there are only very few edges to sort, */
    /* insertion sort is by far the fastest sorting method. */
  }  /* sortEdges() */

}  /* class Node */
