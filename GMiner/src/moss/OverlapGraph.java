/*----------------------------------------------------------------------
  File    : OverlapGraph.java
  Contents: class for an overlap graph for embeddings
  Author  : Christian Borgelt
  History : 2007.06.14 file created
            2007.06.15 greedy maximum independent set computation added
            2007.06.18 exact  maximum independent set computation added
            2007.06.19 both algorithms debugged and optimized
            2007.06.21 function clear() added
            2009.08.06 adapted to java generics (Comparable)
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.FileReader;
import java.util.Arrays;

/*--------------------------------------------------------------------*/
/** Class for the nodes of an overlap graph for embeddings.
 *  @author Christian Borgelt
 *  @since  2007.06.14 */
/*--------------------------------------------------------------------*/
class OGNode implements Comparable<OGNode> {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the embedding represented by the node */
  protected Embedding emb;
  /** the node degree (number of incident edges) */
  protected int       deg;
  /** the reduced node degree (remaining graph without fixed nodes) */
  protected int       red;
  /** the node marker (for connected components/recursion depth) */
  protected int       mark;
  /** the array of adjacent nodes */
  protected OGNode[]  adjs;

  /*------------------------------------------------------------------*/
  /** Create an unconnected node.
   *  @param  emb  the represented embedding
   *  @param  size the size of the array of adjacent nodes
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected OGNode (Embedding emb, int size)
  {                             /* --- create an overlap graph node */
    this.emb  = emb;            /* store the embedding and */
    this.deg  = 0;              /* create an array of adjacent nodes */
    this.adjs = new OGNode[(size < 4) ? 4 : size];
  }  /* OGNode() */             

  /*------------------------------------------------------------------*/
  /** Enlarge the array of adjacent nodes.
   *  @param  max maximum size of the adjacent nodes array
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void enlarge (int max)
  {                             /* --- enlarge adjacent nodes array */
    OGNode[] tmp = this.adjs;   /* buffer for the old node array */
    int      len = tmp.length;  /* size of the (new) node array */

    len += (len > 4) ? len >> 1 : 4;
    if (len > max) len = max;   /* compute the new array size */
    System.arraycopy(tmp, 0, this.adjs = new OGNode[len], 0, this.deg);
  }  /* enlarge() */            /* enlarge array and copy the nodes */

  /*------------------------------------------------------------------*/
  /** Recursively mark the nodes of a connected component.
   *  @param  mark the value with which to mark the nodes
   *  @since  2007.06.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void mark (int mark)
  {                             /* --- mark connected component */
    this.mark = mark;           /* mark the node as visited */
    for (int i = this.deg; --i >= 0; ) {
      OGNode d = this.adjs[i];  /* traverse the adjacent nodes */
      if (d.mark < 0) d.mark(mark);
    }                           /* recursively mark unvisited nodes */
  }  /* mark() */

  /*------------------------------------------------------------------*/
  /** Compare to another node.
   *  @param  obj the node to compare to
   *  @return the sign of the difference of the node markers, that is,
   *          <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the marker of this node is less than, equal to, or
   *          greater than the marker of the node given as an argument
   *  @since  2007.06.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int compareTo (OGNode obj)
  {                             /* --- compare to another node */
    if (this.mark < obj.mark) return -1;
    if (this.mark > obj.mark) return +1;
    return 0;                   /* return sign of the difference */
  }  /* compareTo() */          /* of the node marker values */

}  /* class OGNode */


/*--------------------------------------------------------------------*/
/** Class to represent overlap graphs for embeddings.
 *  @author Christian Borgelt
 *  @since  2007.06.14 */
/*--------------------------------------------------------------------*/
public class OverlapGraph {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** whether this is a harmful overlap graph */
  private boolean  harmful;
  /** the (current) number of nodes */
  private int      cnt;
  /** the array of nodes */
  private OGNode[] nodes;
  /** the number of selected nodes */
  private int      sel;
  /** the number of remaining nodes (neither selected nor excluded) */
  private int      rem;
  /** the number of nodes in the best independent set found yet */
  private int      best;
  /** the buffer for the nodes (for reordering) */
  private OGNode[] buf;
  /** the stack of fixed nodes (selected or excluded) */
  private OGNode[] stack;
  /** the next position on the stack */
  private int      pos;

  /*------------------------------------------------------------------*/
  /** Create a (empty) overlap graph with a default size.
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public OverlapGraph ()
  { this(false, 16); }

  /*------------------------------------------------------------------*/
  /** Create a (empty) overlap graph with a default size.
   *  @param  harmful whether the graph is to be a harmful overlap graph
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public OverlapGraph (boolean harmful)
  { this(harmful, 16); }

  /*------------------------------------------------------------------*/
  /** Create a (empty) overlap graph with a given size.
   *  @param  harmful whether the graph is to be a harmful overlap graph
   *  @param  size    the expected number of nodes
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public OverlapGraph (boolean harmful, int size)
  {                             /* --- create an empty overlap graph */
    this.harmful = harmful;     /* note the graph type */
    this.nodes   = new OGNode[size];
    this.cnt     = 0;           /* create a node array */
    this.stack   = this.buf = null;
  }  /* OverlapGraph() */

  /*------------------------------------------------------------------*/
  /** Check whether this is a harmful overlap graph.
   *  @return whether this is a harmful overlap graph
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isHarmful ()
  { return this.harmful; }

  /*------------------------------------------------------------------*/
  /** Get the size of the overlap graph (number of nodes).
   *  @return the number of nodes of the overlap graph
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int size ()
  { return this.cnt; }

  /*------------------------------------------------------------------*/
  /** Clear an overlap graph (remove all embeddings).
   *  @since  2007.06.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void clear ()
  {                             /* --- clear the overlap graph */
    for (int i = this.cnt; --i >= 0; )
      this.nodes[i] = null;     /* "delete" all nodes and */
    this.cnt = 0;               /* reinit. the node counter */
  }  /* clear() */

  /*------------------------------------------------------------------*/
  /** Add an embedding to the overlap graph.
   *  <p>The embedding is compared to all previously added embeddings
   *  and it is checked whether there is a (harmful) overlap with them.
   *  If there is an overlap, the nodes representing the embeddings are
   *  connected with an edge.</p>
   *  @param  emb the embedding to add
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void add (Embedding emb)
  {                             /* --- add an embedding */
    int      i, n;              /* loop variable */
    OGNode[] tmp = this.nodes;  /* buffer for the old node array */
    int      max = tmp.length;  /* (new) array size */
    OGNode   s, d;              /* buffers for nodes to connect */

    if (this.cnt >= max) {      /* if the node array is full */
      this.nodes = new OGNode[max += max >> 1];
      System.arraycopy(tmp, 0, this.nodes, 0, this.cnt);
    }                           /* enlarge the array and copy nodes */
    for (n = 0, i = this.cnt; --i >= 0; ) {
      d = this.nodes[i];        /* traverse the existing nodes */
      n += d.mark = (emb.overlaps(d.emb, this.harmful)) ? 1 : 0;
    }                           /* count overlaps and mark embeddings */
    this.nodes[this.cnt] =      /* create a new node and */
    s = new OGNode(emb, n);     /* add it to the node array */
    for (--max, i = this.cnt++; --i >= 0; ) {
      d = this.nodes[i];        /* traverse the existing embeddings, */
      if (d.mark <= 0) continue;/* but skip non-overlapping ones */
      if (d.deg  >= d.adjs.length) d.enlarge(max);
      d.adjs[d.deg++] = s;      /* add the nodes to each other's */
      s.adjs[s.deg++] = d;      /* adjacent node arrays */
    }                           /* (add an edge between the nodes) */
  }  /* add() */

  /*------------------------------------------------------------------*/
  /** Select a safe node for an independent node set.
   *  <p>Isolated nodes and leaves are safe to select. For isolated
   *  nodes this is trivial. For leaf nodes there must be a maximum
   *  independent set containing them, because selecting them is less
   *  restrictive that selecting their only adjacent node. On the other
   *  hand, if the only adjacent node is not selected, the leaf can be
   *  selected for an independent set.</p>
   *  <p>Note that, in general, any node the neighbors of which form
   *  a complete graph is safe to select. However, this test is more
   *  complicated and thus has not been implemented up to now.</p>
   *  <p>The given node, which must be an isolated node or a leaf,
   *  is selected and its neighbors are excluded. In addition, any
   *  neighbors of these excluded neighbors that become safe to select
   *  due to the exclusion (that is, which are isolated nodes or leaves
   *  in the reduced overlap graph) are selected (by calling this
   *  function recursively).</p>
   *  @param  node the node to select
   *  @see    #selectSafeRev(OGNode)
   *  @since  2007.06.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void selectSafe (OGNode node)
  {                             /* --- select a safe node */
    int    i, n;                /* loop variables */
    OGNode s, d;                /* to traverse the nodes */

    node.mark = 0;              /* mark and count node as selected */
    this.sel++; this.rem--;     /* and remove it from the rem. nodes */
    if (node.red <= 0) return;  /* if the node is isolated, abort */
    for (i = node.deg; node.adjs[--i].mark >= 0; );
    s = node.adjs[i];           /* find the only neighbor of a leaf, */
    s.mark = 1;                 /* mark it as excluded, and remove */
    this.rem--; n = 0;          /* it from the remaining nodes */
    for (i = s.deg; --i >= 0; ) {
      d = s.adjs[i];            /* reduce its neighbors degrees */
      if ((d.mark < 0) && (--d.red <= 1)) n++;
    }                           /* count leaves and isolated nodes */
    if (n <= 0) return;         /* if no recursion is needed, abort */
    for (i = s.deg; --i >= 0; ) {
      d = s.adjs[i];            /* traverse the neighbor's neighbors */
      if ((d.mark < 0) && (d.red <= 1)) this.selectSafe(d);
    }                           /* select safe nodes recursively */
  }  /* selectSafe() */

  /*------------------------------------------------------------------*/
  /** Process a connected component greedily.
   *  @param  beg the start of the node range (included)
   *  @param  end the end   of the node range (excluded)
   *  @since  2007.06.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void greedy (int beg, int end)
  {                             /* --- process a component greedily */
    int    i, k, m = 1;         /* loop variables */
    OGNode node, s, d;          /* to traverse the nodes */

    do {                        /* greedy node selection loop */
      while (this.buf[--end].mark >= 0)
        ;                       /* find the next unprocessed node */
      for (node = this.buf[i = k = end]; --i >= beg; ) {
        d = this.buf[i];        /* traverse the remaining nodes */
        if ((d.mark < 0) && (d.red < node.red)) { node = d; k = i; }
      }                         /* find a node with minimum degree */
      this.buf[k]   = this.buf[end];
      this.buf[end] = node;     /* swap the found node to the end */
      node.mark = 0;            /* mark and count it as selected and */
      this.sel++; m++;          /* remove the node and its neighbors */
      this.rem -= node.red +1;  /* from the remaining nodes */
      for (i = node.deg; --i >= 0; ) {
        s = node.adjs[i]; if (s.mark < 0) s.mark = m; }
      for (i = node.deg; --i >= 0; ) {
        s = node.adjs[i];       /* traverse the excluded neighbors */
        if (s.mark != m) continue;
        s.red = 0;              /* initialize the safe node counter */
        for (k = s.deg; --k >= 0; ) {
          d = s.adjs[k];        /* traverse each neighbor's neighbors */
          if ((d.mark < 0) && (--d.red <= 1)) s.red++;
	}                       /* reduce their node degrees and */
      }                         /* count leaves and isolated nodes */
      for (i = node.deg; --i >= 0; ) {
        s = node.adjs[i];       /* traverse the node's neighbors */
        if ((s.mark != m) || (s.red <= 0)) continue;
        for (k = s.deg; --k >= 0; ) {
          d = s.adjs[k];        /* traverse each neighbor's neighbors */
          if ((d.mark < 0) && (d.red <= 1)) this.selectSafe(d);
        }                       /* select unprocessed leaves and */
      }                         /* isolated nodes recursively */
    } while (this.rem > 3);     /* while the rest is not trivial */
    this.sel += this.rem & 1;   /* process the trivial rest */
  }  /* greedy() */

  /*------------------------------------------------------------------*/
  /** Select a safe node for an independent node set.
   *  <p>Version for reversible selection, exact algorithm.</p>
   *  @param  node the leaf node to select
   *  @see    #selectSafe(OGNode)
   *  @since  2007.06.18 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void selectSafeRev (OGNode node)
  {                             /* --- select a safe node */
    int    i, n;                /* loop variables */
    OGNode s, d;                /* to traverse the nodes */

    this.stack[this.pos++] = node;  /* mark the node as selected */
    node.mark = this.pos +this.pos;
    this.sel++; this.rem--;     /* update the node counters */
    if (node.red <= 0) return;  /* if the node is isolated, abort */
    for (i = node.deg; node.adjs[--i].mark >= 0; );
    s = node.adjs[i];           /* find the unprocessed neighbor */
    s.mark = node.mark +1;      /* and exclude this neighbor */
    this.rem--; n = 0;          /* update the rem. node counter */
    for (i = s.deg; --i >= 0; ) {
      d = s.adjs[i];            /* reduce degrees of the neighbors */
      if ((d.mark < 0) && (--d.red <= 1)) n++;
    }                           /* count isolated nodes and leaves */
    if (n <= 0) return;         /* if no recursion needed, abort */
    for (i = s.deg; --i >= 0; ) {
      d = s.adjs[i];            /* traverse the neighbor's neighbors */
      if ((d.mark < 0) && (d.red <= 1)) this.selectSafeRev(d);
    }                           /* select safe nodes recursively */
  }  /* selectSafeRev() */

  /*------------------------------------------------------------------*/
  /** Select a node for an independent node set.
   *  <p>The given node is selected and its neighbors are excluded.
   *  In addition, all neighbors of these excluded neighbors that
   *  become safe to select due to the exclusion (that is, which are
   *  either isolated nodes or leaves in the reduced overlap graph)
   *  are selected (by calling this function recursively).</p>
   *  <p>The selection can be undone by calling the function
   *  {@link #restore(OGNode)} with the same node.</p>
   *  @param  node the node to select
   *  @see    #restore(OGNode)
   *  @since  2007.06.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void select (OGNode node)
  {                             /* --- select a node */
    int    i, k, m, n = 0;      /* loop variables, buffers */
    OGNode s, d;                /* to traverse the nodes */

    this.stack[this.pos++] = node;
    node.mark = this.pos << 1;  /* mark the node as selected */
    this.sel++; this.rem--;     /* update the node counters */
    m = node.mark +1;           /* get the exclusion marker */
    for (i = node.deg; --i >= 0; ) {
      s = node.adjs[i]; if (s.mark < 0) s.mark = m; }
    this.rem -= node.red;       /* mark the neighbors as excluded */
    for (i = node.deg; --i >= 0; ) {
      s = node.adjs[i];         /* traverse the excluded neighbors */
      if (s.mark != m) continue;
      for (k = s.deg; --k >= 0; ) {
        d = s.adjs[k];          /* traverse each neighbor's neighbors */
        if ((d.mark < 0) && (--d.red <= 1)) n++;
      }                         /* reduce degrees of the neighbors, */
    }                           /* count isolated nodes and leaves */
    if (n <= 0) return;         /* if no recursion needed, abort */
    for (i = node.deg; --i >= 0; ) {
      s = node.adjs[i];         /* traverse the excluded neighbors */
      if (s.mark != m) continue;
      for (k = s.deg; --k >= 0; ) {
        d = s.adjs[k];          /* traverse each neighbor's neighbors */
        if ((d.mark < 0) && (d.red <= 1)) this.selectSafeRev(d);
      }                         /* select safe nodes recursively */
    }                           /* (but in a restorable way) */
  }  /* select() */

  /*------------------------------------------------------------------*/
  /** Exclude a node from an independent set.
   *  <p>The node is excluded from an independent set and the node
   *  degrees of its neighbors are reduced. If this creates isolated
   *  nodes or leaves in the reduced overlap graph, these nodes are
   *  selected recursively (by calling the function
   *  {@link #selectSafeRev(OGNode)}).</p>
   *  <p>The exclusion can be undone by calling the function
   *  {@link #restore(OGNode)} with the same node.</p>
   *  @param  node the node to exclude
   *  @see    #restore(OGNode)
   *  @since  2007.06.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void exclude (OGNode node)
  {                             /* --- exclude a node */
    int    i, n = 0;            /* loop variable */
    OGNode d;                   /* to traverse the adjacent nodes */

    this.stack[this.pos++] = node;
    node.mark = 2 *this.pos +1; /* mark the node as excluded */
    this.rem--;                 /* update the rem. node counter */
    for (i = node.deg; --i >= 0; ) {
      d = node.adjs[i];         /* reduce degrees of neighbors */
      if ((d.mark < 0) && (--d.red <= 1)) n++;
    }                           /* count isolated nodes and leaves */
    if (n <= 0) return;         /* if no recursion needed, abort */
    for (i = node.deg; --i >= 0; ) {
      d = node.adjs[i];         /* traverse the neighbors again */
      if ((d.mark < 0) && (d.red <= 1)) this.selectSafeRev(d);
    }                           /* select safe nodes recursively */
  }  /* exclude() */

  /*------------------------------------------------------------------*/
  /** Restore the state of the overlap graph.
   *  <p>The node and all nodes, which were selected as a consequence
   *  of it being selected or excluded, are marked as unprocessed.
   *  Their reduced node degrees are reset to the state before the
   *  selection or exclusion.</p>
   *  @param  node the node to deselect
   *  @see    #select(OGNode)
   *  @see    #exclude(OGNode)
   *  @since  2007.06.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void restore (OGNode node)
  {                             /* --- restore the state of the graph */
    int    i, k;                /* loop variables */
    OGNode r, s, d;             /* to traverse the nodes */

    do {                        /* traverse the node stack */
      r = this.stack[--this.pos];
      if ((r.mark & 1) == 0) {  /* if the node was selected */
        for (i = r.deg; --i >= 0; ) {
          s = r.adjs[i];        /* traverse the node's neighbors */
          if (s.mark <= r.mark) continue;
          for (k = s.deg; --k >= 0; ) {
            d = s.adjs[k]; if (d.mark < 0) d.red++; }
	}                       /* increase degree of their neighbors */
        for (i = r.deg; --i >= 0; ) {
          s = r.adjs[i]; if (s.mark > r.mark) s.mark = -1; }
        this.rem += r.red;      /* unexclude the node's neighbors */
        this.sel--; }           /* the node is no longer selected */
      else {                    /* if the node was excluded */
        for (i = r.deg; --i >= 0; ) {
          s = r.adjs[i]; if (s.mark < 0) s.red++; }
      }                         /* increase node degrees of neighbors */
      this.rem++;               /* add the node to the rem. nodes */
      r.mark = -1;              /* mark the node as unprocessed */
    } while (r != node);        /* while not back at reference node */
  }  /* restore() */

  /*------------------------------------------------------------------*/
  /** Find a maximum independent set recursively (branch and bound).
   *  @param  beg the start of the node range (included)
   *  @param  end the end   of the node range (excluded)
   *  @since  2007.06.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void recurse (int beg, int end)
  {                             /* --- find an MIS recursively */
    int    i, k;                /* loop variables */
    OGNode s, d;                /* to traverse the nodes */

    if (this.rem <= 3) {        /* if there at most three nodes left */
      k = this.sel +(this.rem & 1);
      if (k > this.best) this.best = k;
      return;                   /* solve the rest graph directly */
    }                           /* and update the best solution */
    if (this.sel +this.rem <= this.best)
      return;                   /* check if a larger IS is possible */
    while (this.buf[--end].mark >= 0)
      ;                         /* find the next unprocessed node */
    for (s = this.buf[i = k = end]; --i >= beg; ) {
      d = this.buf[i];          /* traverse the remaining nodes */
      if ((d.mark < 0) && (d.red > s.red)) { s = d; k = i; }
    }                           /* find a node with maximum degree */
    this.buf[k]   = this.buf[end];
    this.buf[end] = s;          /* swap the found node to the end */
    this.select (s);            /* select the node, */
    this.recurse(beg, end);     /* find MIS for rest recursively, */
    this.restore(s);            /* and restore the previous state */
    this.exclude(s);            /* exclude the node, */
    this.recurse(beg, end);     /* find MIS for rest recursively, */
    this.restore(s);            /* and restore the previous state */
  }  /* recurse() */

  /*------------------------------------------------------------------*/
  /** Find the size of a maximum independent node set (MIS).
   *  <p>This function uses a heuristic greedy algorithm that may yield
   *  a wrong result. However, as a tradeoff, it is considerably faster
   *  than the exact algorithm.</p>
   *  <p>The function selects in each step (one of) the node(s) with
   *  the smallest node degree, excludes its neighbors, and then selects
   *  nodes that became safe to select in the reduced overlap graph.</p>
   *  @return the size of a maximum independent node set
   *  @see    #getMISSize(boolean)
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getMISGreedy ()
  { return this.getMISSize(true); }

  /*------------------------------------------------------------------*/
  /** Find the size of a maximum independent node set (MIS).
   *  <p>This function uses an exact algorithm based on a recursive
   *  node selection/exclusion scheme. It guarantees the optimal
   *  solution, but has exponential time complexity. In addition,
   *  it does not exploit all possible tricks, but is rather a fairly
   *  straightforward implementation of the basic search scheme.</p>
   *  @return the size of a maximum independent node set
   *  @see    #getMISSize(boolean)
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getMISExact ()
  { return this.getMISSize(false); }

  /*------------------------------------------------------------------*/
  /** Find the size of a maximum independent node set (MIS).
   *  @return the size of a maximum independent node set
   *  @see    #getMISSize(boolean)
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getMISSize ()
  { return this.getMISSize(false); }

  /*------------------------------------------------------------------*/
  /** Find the size of a maximum independent node set (MIS).
   *  <p>The search is terminated as soon as an independent set of
   *  at least the minimum required size is found. In this case the
   *  returned value may be smaller than the actual MIS size, even
   *  if the exact algorithm is used.</p>
   *  @param  greedy whether to use the greedy algorithm
   *  @return the size of a maximum independent node set
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getMISSize (boolean greedy)
  {                             /* --- get the size of an MIS */
    int    i, k, n;             /* loop variables, number of nodes */
    int    c;                   /* number of connected components */
    OGNode s;                   /* to traverse the nodes */

    /* --- handle trivial cases --- */
    if (this.cnt <= 1) return this.cnt;
    if (this.cnt <= 2) return 2 -this.nodes[0].deg;

    /* --- select all safe nodes --- */
    this.sel = n = 0;           /* initialize the node counters */
    for (i = this.cnt; --i >= 0; ) {
      s = this.nodes[i];        /* traverse the nodes and */
      s.red = s.deg;            /* copy the node degree */
      if      (s.deg <= 0) { s.mark =  0; this.sel++; }
      else if (s.deg <= 1) { s.mark = -1; n++; }
      else s.mark = -1;         /* select the isolated nodes and */
    }                           /* mark other nodes as unprocessed */
    this.rem = this.cnt -this.sel;
    if (n > 0) {                /* if there are leaf nodes */
      for (i = this.cnt; --i >= 0; ) {
        s = this.nodes[i];      /* traverse the nodes again */
        if ((s.mark < 0) && (s.red == 1)) this.selectSafe(s);
      }                         /* select all leaf nodes */
    }                           /* (and other safe nodes recursively) */
    if (this.rem <= 3) return this.sel += this.rem & 1;
    /* If there are at most three nodes left, there are either zero */
    /* or three nodes left, because after selecting all safe nodes, */
    /* all remaining nodes must have at least two incident edges.   */
    /* With three nodes, the remaining graph must be a triangle.    */

    /* --- split into connected components --- */
    this.buf = new OGNode[this.rem];
    for (i = c = 0; i < this.cnt; i++) {
      s = this.nodes[i];        /* traverse the nodes */
      if (s.mark < 0) this.buf[c++] = s;
    }                           /* copy remaining nodes to the buffer */
    for (i = c = 0; i < this.rem; i++) {
      s = this.buf[i];          /* traverse the nodes again */
      if (s.mark < 0) s.mark(c++);
    }                           /* mark the connected components */
    Arrays.sort(this.buf);      /* sort nodes according to markers */

    /* --- process the connected components --- */
    i = this.rem;               /* create a stack for exact algorithm */
    if (!greedy) this.stack = new OGNode[i];
    while (--c >= 0) {          /* traverse the connected components */
      for (k = i; --i >= 0; )   /* find the start of the component */
        if (this.buf[i].mark != c) break;                
      this.rem = n = k -(++i);  /* compute the number of nodes */
      /* The number of nodes in each connected component must be at */
      /* least three, and each node must have at least two incident */
      /* edges, because otherwise the nodes would have been fixed   */
      /* when safe nodes where selected (see above).                */
      if (n <= 3) { this.sel++;        continue; }
      /* If the connected component contains exactly three nodes,   */
      /* it must be triangle, so exactly one node can be selected.  */
      for (n = k; --n >= i; )   /* traverse the nodes of the comp. */
        this.buf[n].mark = -1;  /* and clear the node markers */
      if (greedy) { this.greedy(i, k); continue; }
      /* If not to use the exact algorithm, use the greedy variant. */
      this.pos  = 0;            /* init. the node stack position */
      this.best = this.sel;     /* and the node selection counter */
      this.recurse(i, k);       /* find an MIS for the component */
      this.sel  = this.best;    /* and store the best size found */
    }                           /* (this.sel has been reset) */

    this.stack = null;          /* "delete" the node stack */
    this.buf   = null;          /* and the node buffer */
    return this.sel;            /* return the size of an MIS */
  }  /* getMISSize() */

  /*------------------------------------------------------------------*/
  /** Parse a graph description.
   *  <p>The graph is assumed to be given in ASCII DIMACS format.</p>
   *  @param  reader the reader to read from
   *  @return the parsed graph
   *  @since  2007.06.18 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static OverlapGraph parseGraph (Reader reader)
    throws IOException
  {                             /* --- parse ASCII DIMACS format */
    int          i, k;          /* loop variable */
    int          c;             /* character read */
    StringBuffer buf;           /* buffer for a field */
    int          nodecnt;       /* number of nodes of the graph */
    int          edgecnt;       /* number of edges of the graph */
    int          s, d;          /* source and destination node index */
    OGNode       src, dst;      /* source and destination node */
    OverlapGraph graph;         /* created graph */

    /* --- skip leading comments --- */
    while ((c = reader.read()) == 'c')
      while (((c = reader.read()) != '\n') && (c != -1));

    /* --- read number of nodes and edges --- */
    if (c != 'p') throw new IOException("'p' expected");
    if ((c = reader.read()) != ' ')
      throw new IOException("' ' expected");
    while (c == ' ')            /* check for 'p' <separator> */
      c = reader.read();        /* and then skip blanks */
    if (c == -1) throw new IOException("'edge' expected");
    buf = new StringBuffer();   /* read the next field */
    do { buf.append((char)c); c = reader.read(); }
    while ((c != ' ') && (c != -1));
    if (!buf.toString().trim().equals("edge"))
      throw new IOException("'edge' expected");

    while (c == ' ') c = reader.read();
    if (c == -1) throw new IOException("number expected");
    buf.setLength(0);           /* read the number of nodes */
    do { buf.append((char)c); c = reader.read(); }
    while ((c != ' ') && (c != -1));
    try { nodecnt = Integer.parseInt(buf.toString().trim()); }
    catch (NumberFormatException e) {
      throw new IOException(e.getMessage()); }

    while (c == ' ') c = reader.read();
    if (c == -1) throw new IOException("number expected");
    buf.setLength(0);           /* read the number of edges */
    do { buf.append((char)c); c = reader.read(); }
    while ((c != '\n') && (c != -1));
    try { edgecnt = Integer.parseInt(buf.toString().trim()); }
    catch (NumberFormatException e) {
      throw new IOException(e.getMessage()); }

    /* --- create the graph --- */
    graph = new OverlapGraph(false, nodecnt);
    for (i = nodecnt; --i >= 0;)/* create a graph without edges */
      graph.nodes[i] = new OGNode(null, 4);
    graph.cnt = nodecnt;        /* set the number of nodes */

    /* --- read edge descriptions --- */
    for (i = edgecnt; --i >= 0; ) {  /* while more edges to read */
      if ((c = reader.read()) != 'e')
        throw new IOException("'e' expected");
      if ((c = reader.read()) != ' ')
        throw new IOException("' ' expected");

      while (c == ' ') c = reader.read();
      if (c == -1) throw new IOException("number expected");
      buf.setLength(0);         /* read the source node index */
      do { buf.append((char)c); c = reader.read(); }
      while ((c != ' ') && (c != -1));
      try { s = Integer.parseInt(buf.toString().trim()); }
      catch (NumberFormatException e) {
        throw new IOException(e.getMessage()); }
      if ((s <= 0) || (s > nodecnt))
        throw new IOException("invalid node index " +s);

      while (c == ' ') c = reader.read();
      if (c == -1) throw new IOException("number expected");
      buf.setLength(0);         /* read the destination node index */
      do { buf.append((char)c); c = reader.read(); }
      while ((c != '\n') && (c != -1));
      try { d = Integer.parseInt(buf.toString().trim()); }
      catch (NumberFormatException e) {
        throw new IOException(e.getMessage()); }
      if ((d <= 0) || (d > nodecnt))
        throw new IOException("invalid node index " +d);

      src = graph.nodes[s-1];   /* get the nodes to connect */
      dst = graph.nodes[d-1];   /* and avoid duplicate edges */
      for (k = src.deg; --k >= 0; )
        if (src.adjs[k] == dst) break;
      if (k >= 0) continue;     /* enlarge the adjacent node arrays */
      if (src.deg >= src.adjs.length) src.enlarge(nodecnt-1);
      if (dst.deg >= dst.adjs.length) dst.enlarge(nodecnt-1);
      src.adjs[src.deg++] = dst;/* store the nodes in each other's */
      dst.adjs[dst.deg++] = src;/* adjacent node arrays */
    }
    if (reader.read() != -1)    /* check for end of description */
      throw new IOException("garbage at end of file");
    return graph;               /* return the created graph */
  }  /* parseGraph() */

  /*------------------------------------------------------------------*/
  /** Main function for testing some basic functionality.
   *  @param  args the command line arguments
   *  @since  2007.06.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static void main (String args[])
  {                             /* --- main function for testing */
    int          n = 0;         /* number of embeddings */
    Notation     smiles;        /* graph/molecule notation */
    Graph        graph, sub;    /* graph and subgraph */
    Embedding    embs, e;       /* list of embeddings */
    OverlapGraph olap;          /* constructed overlap graph */
    long         t;             /* for time measurements */

    if ((args.length < 1)       /* if no arguments are given */
    ||  (args.length > 2)) {    /* or wrong number of arguments */
      System.out.println("java moss.OverlapGraph <graph> <subgraph>");
      return;                   /* print a usage message */
    }                           /* and abort the program */

    if (args.length <= 1) {     /* if only one argument is given */
      t = System.currentTimeMillis();
      System.out.print("reading " +args[0] +" ... ");
      try { olap = OverlapGraph.parseGraph(new FileReader(args[0])); }
      catch (IOException x) {   /* read the graph from a file */
        System.err.println("\n" +x.getMessage()); return; }
      t = System.currentTimeMillis() -t;
      System.out.println("done [" +(t/1000.0) +"s].");
      t = System.currentTimeMillis();
      System.out.print("MIS (greedy): " +olap.getMISGreedy());
      t = System.currentTimeMillis() -t;
      System.out.println(" [" +(t/1000.0) +"s]");
      t = System.currentTimeMillis();
      System.out.print("MIS (exact) : " +olap.getMISSize());
      t = System.currentTimeMillis() -t;
      System.out.println(" [" +(t/1000.0) +"s]");
      return;                   /* find a maximum independent set */
    }                           /* with both methods */

    smiles = new SMILES();      /* create a SMILES parser */
    try {                       /* parse the given graphs */
      graph = smiles.parse(new StringReader(args[0]));
      sub   = smiles.parse(new StringReader(args[1]));
      graph.prepare(); sub.prepareEmbed(); }
    catch (IOException x) {     /* catch and report a parse error */
      System.err.println(x.getMessage()); return; }
    embs = graph.embed(sub);    /* embed one graph into the other */
    for (e = embs; e != null; e = e.succ)
      n++;                      /* count the embeddings */
    System.out.println(n +" embedding(s)");
    olap = new OverlapGraph(false, n);
    for (e = embs; e != null; e = e.succ)
      olap.add(e);              /* create a normal overlap graph */
    System.out.println("MIS (greedy): " +olap.getMISGreedy());
    System.out.println("MIS (exact) : " +olap.getMISSize());
    olap = new OverlapGraph(true, n);
    for (e = embs; e != null; e = e.succ)
      olap.add(e);              /* create a harmful overlap graph */
    System.out.println("HO  (greedy): " +olap.getMISGreedy());
    System.out.println("HO  (exact) : " +olap.getMISSize());
  }  /* main() */

}  /* class OverlapGraph */
