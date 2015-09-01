/*----------------------------------------------------------------------
  File    : ExtList.java
  Contents: Management of extension lists and their elements
  Author  : Christian Borgelt
  History : 2005.07.23 file created as part of file Fragment.java
            2005.08.11 function merge modified
            2006.06.19 adapted to ring extensions
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for managing extension lists and their elements.
 *  <p>Extension lists are used to determine whether a fragment is
 *  closed, that is, whether no superstructure has the same support.
 *  The basic idea is to try to find an extension that is possible in
 *  all graphs by maintaining a list of extensions that are possible
 *  in all already processed graphs. In each processing step all
 *  extensions that are impossible in the next graph are removed
 *  from the list. A fragment is closed if the list gets empty before
 *  all graphs have been processed, otherwise it is not closed.</p>
 *  @author Christian Borgelt
 *  @since  2005.07.23 */
/*--------------------------------------------------------------------*/
public class ExtList {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the index of the source      node */
  protected int     src;
  /** the index of the destination node */
  protected int     dst;
  /** the type  of the extension   edge */
  protected int     edge;
  /** the type  of the destination node */
  protected int     node;
  /** the ring information (if needed) */
  protected int[]   ring;
  /** the successor in a list */
  protected ExtList succ;

  /*------------------------------------------------------------------*/
  /** Create an extension list element for a single edge extension.
   *  @param  src  the index of the source node
   *  @param  dst  the index of the destination node (or -1)
   *  @param  edge the type of the extension edge
   *  @param  node the type of the destination node
   *  @since  2005.07.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected ExtList (int src, int dst, int edge, int node)
  {                             /* --- create extension list element */
    this.src  = src;  this.dst  = dst;
    this.edge = edge; this.node = node;
    this.ring = null;           /* store extension information */
  }  /* ExtList() */    

  /*------------------------------------------------------------------*/
  /** Create an extension list element for a ring extension.
   *  <p>The information about the ring edges, contained in the array
   *  <code>ring</code>, is copied before it is stored in the extension
   *  list element. Hence the array passed as an argument may be reused
   *  for collecting information about another ring.</p>
   *  @param  src  the index of the source node
   *  @param  dst  the index of the destination node (or -1)
   *  @param  edge the type of the (first) extension edge
   *  @param  node the type of the destination node
   *  @param  ring an array containing information about the ring edges
   *  @param  n    the number of relevant fields in <code>ring</code>
   *  @since  2005.07.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected ExtList (int src, int dst, int edge, int node,
                     int[] ring, int n)
  {                             /* --- create extension list element */
    this.src  = src;  this.dst  = dst;
    this.edge = edge; this.node = node;
    System.arraycopy(ring, 0, this.ring = new int[n], 0, n);
  }  /* ExtList() */            /* store extension information */

  /*------------------------------------------------------------------*/
  /** Compare to another extension list element.
   *  <p>This function is needed for merging two extension lists.</p>
   *  @param  e the extension list element to compare to
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as this list element is less than, equal to, or greater
   *          than the given list element
   *  @since  2005.07.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareTo (ExtList e)
  {                             /* --- compare extension list elements*/
    int i, k;                   /* loop variable, buffer */

    if (this.src  < e.src)  return -1;  /* compare the properties */
    if (this.src  > e.src)  return +1;  /* of the first/only edge */
    if (this.dst  < e.dst)  return -1;
    if (this.dst  > e.dst)  return +1;
    if (this.edge < e.edge) return -1;
    if (this.edge > e.edge) return +1;
    if (this.node < e.node) return -1;
    if (this.node > e.node) return +1;
    i = (this.ring == null) ? 0 : this.ring.length;
    k = (   e.ring == null) ? 0 :    e.ring.length;
    if (i < k) return -1;       /* compare the extension type */
    if (i > k) return +1;       /* (edge or ring) and the ring size */
    for (i = 0; i < k; i++) {   /* traverse the ring edges */
      if (this.ring[i] < e.ring[i]) return -1;
      if (this.ring[i] > e.ring[i]) return +1;
    }                           /* compare the ring edge properties */
    return 0;                   /* if no diff. found, exts. are equal */
  }  /* compareTo() */

  /*------------------------------------------------------------------*/
  /** Merge two sorted extension lists (and remove duplicates).
   *  <p>This function is used to implement a simple merge sort
   *  for extension lists. In addition, all elements that occur in
   *  both extension lists are joined by transferring only one of two
   *  equal elements to the output list, thus removing duplicates.</p>
   *  @param  l1 the first  extension list to merge
   *  @param  l2 the second extension list to merge
   *  @return the merged extension lists
   *  @since  2005.07.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected static ExtList merge (ExtList l1, ExtList l2)
  {                             /* --- merge two extension lists */
    int     r;                  /* result of comparison */
    ExtList out, tail, e;       /* output list, list element */

    if (l1 == null) return l2;  /* if one of the lists is empty, */
    if (l2 == null) return l1;  /* return the other */
    tail = null;                /* init. the tail of the output list */
    r = l1.compareTo(l2);       /* compare the first list elements */
    if (r == 0) { l2 = l2.succ; }
    if (r <= 0) { out = tail = l1; l1 = out.succ; }
    else        { out = tail = l2; l2 = out.succ; }
    while ((l1 != null) && (l2 != null)) {
      r = l1.compareTo(l2);     /* compare the first list elements */
      if (r == 0) { l2 = l2.succ; }
      if (r <= 0) { e = l1; l1 = e.succ; }
      else        { e = l2; l2 = e.succ; }
      tail.succ = e; tail = e;  /* move smaller element */
    }                           /* to the output list */
    if      (l1 != null) tail.succ = l1;
    else if (l2 != null) tail.succ = l2;
    else                 tail.succ = null;
    return out;                 /* append remaining elements */
  }  /* merge() */              /* and return the merged list */

  /*------------------------------------------------------------------*/
  /** Sort an extension list (and remove duplicates).
   *  <p>The algorithm is a simple merge sort. The input list is split
   *  into two lists of roughly equal size by traversing it and storing
   *  its elements alternatingly into two output lists. These two lists
   *  are sorted recursively and then merged.</p>
   *  @param  list the extension list to sort
   *  @return the sorted extension list
   *  @since  2005.07.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected static ExtList sort (ExtList list)
  {                             /* --- sort an extension list */
    ExtList l1, l2, e;          /* sublists for mergesort */

    if ((list == null) || (list.succ == null))
      return list;              /* abort for zero and one element */
    l1 = l2 = null;             /* initialize the two output lists */
    while (true) {              /* traverse the input list */
      e = list; list = list.succ; e.succ = l1; l1 = e;
      if (list == null) break;  /* transfer element to first list */
      e = list; list = list.succ; e.succ = l2; l2 = e;
      if (list == null) break;  /* transfer element to second list */
    }                           /* (split into two equal lists) */
    return merge(sort(l1), sort(l2));
  }  /* sort() */               /* sort and then merge the two lists */

}  /* class ExtList */
