/*----------------------------------------------------------------------
  File    : ExtMgr.java
  Contents: class for an extension edge manager
  Author  : Christian Borgelt
  History : 2010.01.22 file created
            2010.01.23 function trim() added (trim excluded node types)
            2010.01.28 maximum degree of the source node added
----------------------------------------------------------------------*/
package moss;

import java.util.Arrays;

/*--------------------------------------------------------------------*/
/** Class for potential extension edges.
 *  @author Christian Borgelt
 *  @since  2010.01.22 */
/*--------------------------------------------------------------------*/
class ExtEdge implements Comparable<ExtEdge> {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the source node type */
  protected int     src;
  /** the edge type */
  protected int     type;
  /** the destination node type */
  protected int     dst;
  /** the (maximum) source node degree */
  protected int     deg;
  /** the successor in a hash bin list */
  protected ExtEdge succ;
  /** the successor in a list for a source node type */
  protected ExtEdge next;

  /*------------------------------------------------------------------*/
  /** Create an extension edge.
   *  @param  src  the source node type
   *  @param  type the edge type
   *  @param  dst  the destination node type
   *  @param  deg  the (maximum) source node degree
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public ExtEdge (int src, int type, int dst, int deg)
  {                             /* --- create an extension edge */
    this.src  = src;            /* note the source node type */
    this.type = type;           /* and  the edge type */
    this.dst  = dst;            /* and  the destination node type */
    this.deg  = deg;            /* and  the source node degree */
    this.succ = this.next = null;
  }  /* ExtEdge() */

  /*------------------------------------------------------------------*/
  /** Check whether two extension edges are equal.
   *  <p>This function exists only in order to avoid certain warnings
   *  due to the existance of a <code>hashCode</code> function.</p>
   *  @param  obj the extension edge to compare to
   *  @return whether the two extension edges are equal
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean equals (Object obj)
  {                             /* --- check for equality */
    return (this.src  == ((ExtEdge)obj).src)
        && (this.type == ((ExtEdge)obj).type)
        && (this.dst  == ((ExtEdge)obj).dst);
  }  /* equals() */

  /*------------------------------------------------------------------*/
  /** Compute the hash code of the extension edge.
   *  @return the hash code of the extension edge
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int hashCode ()
  {                             /* --- compute a hash code */
    return ((this.src *this.dst) +this.type) & Integer.MAX_VALUE;
  }  /* hashCode() */

  /*------------------------------------------------------------------*/
  /** Compare two extension edges.
   *  @param  obj the extension edge to compare to
   *  @return the comparison result, that is, <code>-1</code>,
   *          <code>0</code>, or <code>+1</code> as this extension
   *          edge is less than, equal to, or greater than the
   *          extension edge given as an argument
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int compareTo (ExtEdge obj)
  {                             /* --- compare two extension edges */
    if (this.src  < obj.src)  return -1;
    if (this.src  > obj.src)  return +1;
    if (this.type < obj.type) return -1;
    if (this.type > obj.type) return +1;
    if (this.dst  < obj.dst)  return -1;
    if (this.dst  > obj.dst)  return +1;
    return 0;                   /* return sign of the difference */
  }  /* compareTo() */          /* of the edge markers */

  /*------------------------------------------------------------------*/
  /** Split an edge list into two parts of (almost) equal length.
   *  @return the part of the list that was split off
   *  @since  2010.01.28 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected ExtEdge split ()
  {                             /* --- split an extension edge list */
    ExtEdge e, d = this;        /* to traverse the list */

    for (e = d.next; e != null; ) {
      e = e.next;               /* traverse the extension edge list */
      if (e != null) { e = e.next; d = d.next; }
    }                           /* one step on d, two steps on e */
    e = d.next; d.next = null;  /* get second list, terminate first */
    return e;                   /* return the split off list */
  }  /* split() */

  /*------------------------------------------------------------------*/
  /** Merge two (sorted) edge lists into one (merge sort phase).
   *  @param  src1 the first  source list
   *  @param  src2 the second source list
   *  @return the merged lists
   *  @since  2010.01.28 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected ExtEdge merge (ExtEdge list)
  {                             /* --- merge extension edge lists */
    ExtEdge src = this;         /* to traverse this edge list */
    ExtEdge out, end;           /* start and end of output list */

    if (list == null) return this; /* handle empty input list */
    if (src.compareTo(list) < 0)/* start output with smaller element */
         { end = out = src;  src  =  src.next; }
    else { end = out = list; list = list.next; }
    while ((src  != null)       /* while both source lists */
    &&     (list != null)) {    /* are not empty */
      if (src.compareTo(list) < 0)
           { end = end.next = src;  src  =  src.next; }
      else { end = end.next = list; list = list.next; }
    }                           /* transfer the smaller element */
    end.next = (src != null) ? src : list;
    return out;                 /* append remaining source elements */
  }  /* merge() */              /* and return the merge result */

}  /* class ExtEdge */


/*--------------------------------------------------------------------*/
/** Class for an extension edge manager.
 *  <p>An extension edge manager manages triples consisting of two node
 *  types (source and destination) and an edge type, which describe
 *  potential extension edges.</p>
 *  @author Christian Borgelt
 *  @since  2010.01.22 */
/*--------------------------------------------------------------------*/
public class ExtMgr {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the table for access via the source node type */
  protected ExtEdge[] exts = null;
  /** the hash table of the extension edges */
  protected ExtEdge[] bins = null;
  /** the number of extension edges */
  protected int       cnt  = 0;
  /** the current extension edge */
  protected ExtEdge   curr = null;

  /*------------------------------------------------------------------*/
  /** Create an extension edge manager.
   *  @param  types the number of different node types
   *  @param  hsize the initial size of the hash table
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public ExtMgr (int types, int hsize)
  {                             /* --- create an ext. edge manager */
    this.exts = new ExtEdge[types];
    this.bins = new ExtEdge[hsize];
    this.cnt  = 0;              /* create hash table and edge table */
    this.curr = null;           /* and initialize the edge counter */
  }  /* ExtMgr() */

  /*------------------------------------------------------------------*/
  /** Create an extension edge manager.
   *  @param  types the number of different node types
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public ExtMgr (int types)
  { this(types, 1023); }        /* --- create an ext. edge manager */

  /*------------------------------------------------------------------*/
  /** Reorganize the extension edge hash table.
   *  <p>The hash table of the extension manager is enlarged and
   *  the extension edges are rehashed to achieve faster access.</p>
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void rehash ()
  {                             /* --- reorganize the repository */
    int     i, k;               /* loop variable, hash bin index */
    ExtEdge b[];                /* buffer for old hash bin array */
    ExtEdge e, r;               /* to traverse the hash bin lists */

    b = this.bins;              /* note the old hash bin array and */
    k = (b.length << 1) +1;     /* compute the new hash table size */
    this.bins = new ExtEdge[k]; /* allocate a new hash bin array */
    for (i = b.length; --i >= 0; ) {
      while (b[i] != null) {    /* traverse the nonempty bins */
        e = b[i]; r = (e.src == e.dst) ? e : e.succ; b[i] = r.succ;
        r.succ = this.bins[k = e.hashCode() % this.bins.length];
        this.bins[k] = e;       /* add the element at the head */
      }                         /* of the approriate hash bin list */
    }                           /* in the new hash bin array */
  }  /* rehash() */

  /*------------------------------------------------------------------*/
  /** Add an extension edge.
   *  @param  edge the edge to add
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void add (Edge edge)
  {                             /* --- add an extension edge */
    int     i;                  /* hash bin index */
    int     src, dst;           /* source and destination node type */
    int     sdg, ddg;           /* source and destination node degree */
    ExtEdge e, r;               /* to traverse a hash bin list */

    src = edge.src.type;        /* get the types and degrees of */
    dst = edge.dst.type;        /* the source and destination node */
    if (src < dst) { sdg = edge.src.deg;  ddg = edge.dst.deg; }
    else           { sdg = edge.dst.deg;  ddg = edge.src.deg;
                     src = edge.dst.type; dst = edge.src.type; }
    i = (((src *dst) +edge.type) & Integer.MAX_VALUE)
      % this.bins.length;       /* compute the hash bin index */
    for (e = this.bins[i]; e != null; e = r.succ) {
      r = (e.src == e.dst) ? e : e.succ;
      if ((e.type == edge.type) /* traverse the corresp. hash bin and */
      &&  (e.src  == src)       /* try to find the extension edge */
      &&  (e.dst  == dst)) {    /* (same source/dest./edge type) */
        if (sdg > e.deg) e.deg = sdg;
        if (ddg > r.deg) r.deg = ddg; return;
      }                         /* if the ext. edge already exists, */
    }                           /* update the maximum node degrees */
    if (src == dst) sdg = ddg = (sdg > ddg) ? sdg : ddg;
    e = new ExtEdge(src, edge.type, dst, sdg);
    e.succ = this.bins[ i ];    /* create a new extension edge */
    this.bins[ i ] = e;         /* and add it to the hash table */
    if (++this.cnt > this.bins.length)
      this.rehash();            /* reorganize the hash table */
    e.next = this.exts[src];    /* add edge to the table that */
    this.exts[src] = e;         /* is indexed by the source type */
    if (src == dst) return;     /* check if reverse edge differs */
    r = new ExtEdge(dst, edge.type, src, ddg);
    r.succ = e.succ;            /* create reverse extension edge */
    e.succ = r;                 /* and add it to the hash table */
    if (++this.cnt > this.bins.length)
      this.rehash();            /* reorganize the hash table */
    r.next = this.exts[dst];    /* add reverse to the table that */
    this.exts[dst] = r;         /* is indexed by the source type */
  }  /* add() */

  /*------------------------------------------------------------------*/
  /** Sort the extension edges lexicographically.
   *  <p>This sort function uses an array buffer for the edges
   *  and the sort function of the <code>Arrays</code> class.
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void sortvec ()
  {                             /* --- sort the edges per node type */
    int     i, k;               /* loop variables */
    ExtEdge e;                  /* to traverse the extension edges */
    ExtEdge b[];                /* buffer for sorting */

    b = new ExtEdge[this.cnt];  /* create a buffer for sorting */
    for (i = this.exts.length; --i >= 0; ) {
      e = this.exts[i];         /* traverse the source node types */
      if (e == null) continue;  /* skip empty edge lists */
      for (k = 0; e != null; e = e.next)
        b[k++] = e;             /* collect the edges for a node type */
      Arrays.sort(b, 0, k);     /* sort the collected edges */
      b[--k].next = null;       /* terminate the sorted list */
      while (--k >= 0) b[k].next = b[k+1];
      this.exts[i] = b[0];      /* reconnect the sorted edges */
    }                           /* and replace the original list */
  }  /* sortvec() */

  /*------------------------------------------------------------------*/
  /** Sort the extension edges lexicographically.
   *  <p>This sort function uses a straightforward merge sort
   *  on the singly linked extension edge lists.</p>
   *  @since  2010.01.28 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void sort ()
  {                             /* --- sort the edges per node type */
    for (int i = this.exts.length; --i >= 0; ) {
      if (exts[i] != null)      /* traverse the source node types */
        exts[i] = exts[i].merge(exts[i].split());
    }                           /* sort the extension edge list */
  }  /* sort() */

  /*------------------------------------------------------------------*/
  /** Trim the extension edges.
   *  @param  coder the node type recoder with which to trim
   *  @since  2010.01.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void trim (Recoder coder)
  {                             /* --- trim the extension edges */
    int     i;                  /* loop variable */
    ExtEdge e;                  /* to traverse the extension edges */

    for (i = this.exts.length; --i >= 0; ) {
      e = this.exts[i];         /* traverse the list per source type */
      if (e == null) continue;  /* skip empty source node lists */
      if (coder.isExcluded(e.src)) {  /* remove exluded sources */
        this.exts[i] = null; continue; }
      while (e.next != null) {  /* while not at last element */
        if (coder.isExcluded(e.next.dst)) e.next = e.next.next;
        else                              e      = e.next;
      }                         /* remove excluded destinations */
      e = this.exts[i];         /* check the first list element */
      if (coder.isExcluded(e.dst)) this.exts[i] = e.next;
    }                           /* remove it if it is excluded */
    for (i = this.bins.length; --i >= 0; ) {
      e = this.bins[i];         /* traverse the hash bins */
      if (e == null) continue;  /* skip empty hash bins */
      while (e.succ != null) {  /* while not at last element */
        if (coder.isExcluded(e.succ.src)
        ||  coder.isExcluded(e.succ.dst)) e.succ = e.succ.succ;
        else                              e      = e.succ;
      }                         /* remove excluded node types */
      e = this.bins[i];         /* check the first list element */
      if (coder.isExcluded(e.src) || coder.isExcluded(e.dst))
        this.bins[i] = e.succ;  /* remove the first list element */
    }                           /* if it has an excluded node type */
  }  /* trim() */

  /*------------------------------------------------------------------*/
  /** Initialize the traversal of extension edges.
   *  @param  src  the type of the source node
   *  @return the edge type of the first extension edge
   *          or <code>-1</code> if there is no edge
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int first (int src)
  {                             /* --- get the first extension edge */
    this.curr = this.exts[src]; /* get edge list of node type */
    return (this.curr == null) ? -1 : this.curr.type;
  }  /* first() */              /* return the extension edge type */

  /*------------------------------------------------------------------*/
  /** Get the next extension edge.
   *  @return the edge type of the next extension edge
   *          or <code>-1</code> if there is no such edge
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int next ()
  {                             /* --- get the next extension edge */
    if (this.curr == null) return -1;
    this.curr = this.curr.next; /* go to the next list element */
    return (this.curr == null) ? -1 : this.curr.type;
  }  /* next() */               /* return the extension edge type */

  /*------------------------------------------------------------------*/
  /** Get the edge type of the current extension edge.
   *  @return the edge type of the current extension edge
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getType ()
  { return this.curr.type; }

  /*------------------------------------------------------------------*/
  /** Get the destination node type of the current extension edge.
   *  @return the destination node type of the current extension edge
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getDest ()
  { return this.curr.dst; }

  /*------------------------------------------------------------------*/
  /** Get the maximum source node degree of the current extension edge.
   *  @return the maximum source node degree
   *          of the current extension edge
   *  @since  2010.01.28 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getDegree ()
  { return this.curr.deg; }

}  /* class ExtMgr */
