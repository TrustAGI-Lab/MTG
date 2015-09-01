/*----------------------------------------------------------------------
  File    : CnFDepth.java
  Contents: depth-first search spanning tree canonical form
            and corresponding rightmost path extension
  Author  : Christian Borgelt
  History : 2002.03.11 file created as file submol.java
            2005.08.11 class Extension made abstract, second strategy
            2005.08.13 precomputation of rightmost path added
            2006.04.11 function compareEdge() added (to compare edges)
            2006.04.30 treatment of additional nodes added
            2006.05.02 functions makeCanonic() and makeWord() added
            2006.05.10 function describe added, makeCanonic() written
            2006.05.17 extension-specific ring function added
            2006.05.18 adapted to new field Extension.dst
            2006.05.31 function isCanonic() extended, result changed
            2006.06.04 creation and check of ring extensions added
            2006.06.06 adapted to changed type of edge flags
            2006.06.07 function compareWord() added
            2006.07.01 adaptation of ring extensions moved here
            2007.03.02 default constructor without arguments added
            2007.03.23 bug in function validRing() fixed (ring markers)
            2007.03.24 adapted to new functions of super-class
            2007.06.21 adapted to new class TypeMgr
            2007.10.19 bug in ring extension handling fixed
            2008.03.06 adapted to modified ring key test (isRingKey)
            2009.04.29 adapted to modified class Extension
            2009.05.05 harmless bug in compareEdge() removed
            2010.01.22 renamed to CnFDepth.java (canonical form)
            2010.01.28 check of maximum degree of source node added
            2011.02.21 orbits used to suppress some equivalent siblings
            2011.02.23 functions init(), next() and nextFrag() reworked
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for a depth-first search spanning tree canonical form
 *  and the corresponding rightmost path extensions.
 *  <p>Rightmost path extensions are the restricted extensions of
 *  a depth-first spanning tree canonical form. Only nodes on the
 *  rightmost path of the spanning tree may be extended by adding a
 *  edge. Edges closing rings must lead from a node on the rightmost
 *  path to the rightmost leaf (the deepest node on the rightmost path).
 *  In addition, the extension edge must succeed the downward edge
 *  from the node on the rightmost path to which it is added.</p>
 *  <p>For comparing edges and constructing code words the following
 *  precedence order of the edge properties is used:
 *  <ul><li>destination node index (ascending)
 *      <li>source node index (descending)</li>
 *      <li>edge attribute (ascending)</li>
 *      <li>node attribute (ascending)</li></li></ul>
 *  Hence the general form of a code word is<br>
 *  a (i<sub>d</sub> i<sub>s</sub> b a)<sup>m</sup><br>
 *  where a is a node attribute, b an edge attribute, i<sub>s</sub>
 *  the index of the source node of an edge, i<sub>d</sub> the index
 *  of the destination node of an edge and m the number of edges.
 *  Keep in mind that i<sub>s</sub> is sorted descendingly.</p>
 *  @author Christian Borgelt
 *  @since  2005.08.11 */
/*--------------------------------------------------------------------*/
public class CnFDepth extends CanonicalForm implements Cloneable{

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the indices of the edges on the rightmost path */
  private int[] path;
  /** the index of the next path edge */
  private int   pbi;
  /** the equivalent positions for a new ring edge */
  private int[] eqpos;

  /*------------------------------------------------------------------*/
  /** Create a depth-first search spanning tree canonical form.
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public CnFDepth ()
  {                             /* --- create a DFS canonical form */
    super();                    /* call the base initialization */
    this.path  = new int[256];  /* and create an edge index array */
    this.eqpos = new int [16];  /* and an equiv. position array */
  }  /* CnFDepth() */

  /*------------------------------------------------------------------*/
  /** Initialize the extension generation process.
   *  <p>Instead of creating a new extension object each time a fragment
   *  or an embedding has to be extended, the same extension object is
   *  reused, thus greatly reducing the overhead for memory allocation.
   *  As a consequence, the extension object has to be reinitialized
   *  for each fragment and each embedding that is to be extended.</p>
   *  @param  frag the fragment  to extend
   *  @param  emb  the embedding to extend
   *               (must be contained in the fragment or
   *               <code>null</code> for pure fragment extensions)
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean init (Fragment frag, Embedding emb)
  {                             /* --- init. a rightmost extension */
    int  i, j, k, n;            /* loop variables, edge counter */
    Node s, d;                  /* to traverse/access the nodes */
    Edge e;                     /* to traverse/access the edges */
    int  v[];                   /* buffer for reallocation */

    if ((this.mode & ALLEXTS) != 0)  /* if to generate all exts., */
      return super.init(frag, emb);  /* call superclass function */
    this.pbi  = -1;             /* start with the first edge */
    this.idx  = -1;             /* of the rightmost leaf node */
    this.size = -1;             /* clear the ring size and flags */
    this.all  =  0;             /* (also extension type indicator) */
    this.pos1 = this.pos2 = -1; /* clear ring variant variables */
    this.pmin = this.pmax = -1; /* (make sure state is defined) */
    this.emb  = emb;            /* note the embedding to extend */
    if (emb == null) {          /* if no embedding is given */
      this.frag = frag;         /* store the fragment to extend */
      frag.graph.index();       /* index nodes of the fragment graph */
      this.src = this.dst = frag.graph.nodecnt;
      return true;              /* init. to last extension of node */
    }                           /* after the current source node */
    emb.index();                /* mark the embedding in its graph */
    this.src = emb.nodes.length -1;  /* get index of the last node */
    /* The last node need not be the rightmost leaf, because there */
    /* can be additional nodes added by perfect or ring extensions.*/
    /* The rightmost leaf rather has the index frag.dst.           */
    if (frag == this.frag)      /* if to work on the same fragment, */
      return true;              /* return that there may be exts. */

    /* --- find the rightmost path --- */
    this.frag = frag;           /* otherwise note the new fragment */
    n = j = 0;                  /* init. the number of path edges */
    s = emb.nodes[frag.dst];    /* get rightmost leaf (end of path) */
    while (s.mark > 0) {        /* construct rightmost path upwards */
      for (k = -1, i = s.deg; --i >= 0; ) {
        e = s.edges[i];         /* traverse the marked edges */
        if (e.mark < 0) continue;
        d = (e.src != s) ? e.src : e.dst;
        if ((d.mark > k) && (d.mark < s.mark)) {
          k = d.mark;           /* find the adjacent node with the */
          j = e.mark;           /* largest index smaller than own, */
        }                       /* that is, find the parent node */
      }                         /* on the rightmost path */
      /* All children of a node have indices greater than the index  */
      /* of the node itself. Edges to nodes with smaller indices are */
      /* either the result of an extension that added the node or of */
      /* an extension that closed a ring. Due to the canonical form, */
      /* the latter (edges closing rings) must come from nodes with  */
      /* smaller indices than the former (parent in spanning tree).  */
      if (n >= this.path.length) {
        System.arraycopy(this.path, 0, v = new int[n+(n >> 1)], 0, n);
        this.path = v;          /* if the path edge index array */
      }                         /* is too small, enlarge it */
      this.path[n++] = j;       /* note the index of the next edge */
      s = emb.nodes[k];         /* on the rightmost path and */
    }                           /* get the next path node */
    /* The indices of the edges on the rightmost path are determined */
    /* only once for a fragment, because they are obviously the same */
    /* for all embeddings of this fragment (avoid redundant search). */
    return true;                /* return that there may be exts. */
  }  /* init() */

  /*------------------------------------------------------------------*/
  /** Create the next extension.
   *  <p>Each time this function is called and returns
   *  <code>true</code>, a new extension has been created, which
   *  may then be compared to already existing fragments (function
   *  <code>compareToFrag()</code>) or turned into a new fragment
   *  (function <code>makeFragment()</code>) or a new embedding
   *  (function <code>makeEmbedding()</code>). When all extensions of
   *  the embedding passed to <code>init()</code> have been created,
   *  the function returns <code>false</code>.</p>
   *  @return whether another extension was created
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean next ()
  {                             /* --- create the next extension */
    Node s, d, z, o[], n[];     /* to traverse/access the nodes */
    Edge e, x;                  /* to traverse/access the edges */

    if ((this.mode & ALLEXTS) != 0)
      return super.next();      /* generate all exts with superclass */

    this.chcnt = this.frag.chcnt;     /* copy the chain counter */

    /* --- continue an old extension --- */
    if (((this.mode & EQVARS) != 0)
    &&   (this.pos1 >= 0)       /* if there is another equivalent */
    &&    this.variant())       /* variant of the previous ring, */
      return true;              /* return "extension successful" */
    if (((this.mode & RING) != 0)
    &&   (this.all  != 0)       /* if there is another ring flag */
    &&    this.ring())          /* and some ring is admissible, */
      return true;              /* return "extension successful" */

    /* --- find a new extension --- */
    o = (this.useOrbits()) ? this.frag.graph.nodes : null;
    n = this.emb.nodes;         /* get the nodes of the embedding */
    s = n[this.src];            /* and the current source node */
    while (true) {              /* find the next unprocessed edge */
      while (++this.idx >= s.deg) {
        if (this.src <= 0) {    /* if the current node is the root, */
          this.emb.mark(-1);    /* there is no other node to go to, */
          return false;         /* so unmark the embedding and abort */
        }                       /* (no more extensions to create) */
        if (this.src > this.frag.dst) /* if beyond rightmost leaf, */
          s = n[--this.src];    /* simply go to the preceding node */
        else {                  /* if on the rightmost path */
          e = this.emb.edges[this.path[++this.pbi]];
          s = (e.src != s) ? e.src : e.dst;
          this.src = s.mark;    /* get the source node of the next */
        }                       /* edge on the rightmost path */
        /* Nodes with indices greater than the index of the rightmost */
        /* leaf (that is, greater than this.frag.dst) are additional  */
        /* nodes (that is, they are not (yet) in the spanning tree).  */
        /* Such nodes can been added by a perfect extension (if there */
        /* were search tree branches to the left of its branch) or a  */
        /* ring extension (if more than one node has been added).     */
        this.idx = -1;          /* start with the node's first edge */
      }                         /* (idx is incremented before used) */
      e = s.edges[this.idx];    /* get the next edge of the source */
      if (e.mark != -1)         /* if the edge is in the embedding */
        continue;               /* or excluded, it cannot be added */
      /* Edges that are already in the embedding have e.mark >= 0, */
      /* excluded edges are marked with a value of e.mark < -1.    */
      d = (s != e.src) ? e.src : e.dst;
      if (d.mark < 0) {         /* if the new edge adds a node */
        if (n.length +this.chcnt >= this.max)
          continue;             /* check whether a new node is ok */
        if ((o != null) && (o[this.src].orbit != this.src))
          continue;             /* check for an orbit representative */
        /* For edges that add a node source nodes that are not orbit */
        /* representatives are skipped, because extending them only  */
        /* creates equivalent siblings that are not canonical.       */
        this.dst = n.length; }  /* assign the next free node index */
      else {                    /* if the new edge closes a ring */
        if ((d.mark <  this.frag.dst)   /* skip edges that do not */
        &&  (s.mark >= this.frag.src))  /* lead to the leaf or start */
          continue;                     /* below the previous source */
        /* This check allows for arbitrary ring-closing edges that   */
        /* lead from any node to an additional node (that is, a node */
        /* having an index greater than that of the rightmost leaf). */
        /* It only rejects certain edges between nodes on the path.  */
        /* For edges closing a ring a test whether the source node   */
        /* an orbit representative rules out edges closing simple    */
        /* like (in SMILES): C1CCCCC1 (the source node is the root   */
        /* and the destination is the rightmost leaf, which are in   */
        /* same orbit (the rightmost leaf is the representative).    */
        this.dst = d.mark;      /* note the destination node index */
      }                         /* (node is already in the fragment) */
      if (this.src < this.frag.dst) { /* if source on rightmost path */
        x = this.emb.edges[this.path[this.pbi]];
        if (e.type <  x.type) continue;
        if (e.type == x.type) { /* skip smaller edge types */
          z = (x.src != s) ? x.src : x.dst;
          if (d.type < z.type) continue;
        }                       /* skip smaller dest. node types */
      }                         /* (edge must not precede path edge) */
      /* An extension edge from a node on the rightmost path (except */
      /* the rightmost leaf) must not precede the downward path edge */
      /* lexicographically (otherwise the fragment is not canonical).*/
      this.nodes[0] = s;        /* note the source node */
      this.nodes[1] = d;        /* and  the destination node */
      this.edges[0] = e;        /* and  the (first) extension edge */
      if     (((this.mode & RING) != 0)
      &&      e.isInRing()) {   /* if a ring extension is possible, */
        this.all  = e.getRings();   /* init. the ring variables and */
        this.curr = 1;          /* check for a valid ring extension */
        if (this.ring()) return true; }
      else if ((this.mode & EDGE) != 0)
        break;                  /* if edge extensions are allowed, */
    }                           /* abort the extension search loop */
    this.nodecnt = (d.mark < 0) ? 1 : 0;
    this.edgecnt = 1;           /* zero/one new node, one new edge */
    this.size    = 0;           /* clear the extension size (edge) */
    return true;                /* return "extension successful" */
  }  /* next() */

  /*------------------------------------------------------------------*/
  /** Create the next extended fragment.
   *  <p>Each call creates a new fragment or returns <code>null</code>.
   *  This function works without embeddings, but draws an a stored list
   *  of extension edges.</p>
   *  @return the next extended fragment or <code>null</code>.
   *  @since  2010.01.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Fragment nextFrag ()
  {                             /* --- create next extended fragment */
    Graph   g;                  /* graph of the fragment */
    Node    s, d;               /* to traverse/access the nodes */
    Edge    e;                  /* to traverse/access the edges */
    int     t, i;               /* ext. edge type, loop variable */
    boolean o;                  /* whether node orbits are used */

    if ((this.mode & ALLEXTS) != 0)
      return super.nextFrag();  /* generate all exts with superclass */

    this.chcnt = this.frag.chcnt;      /* copy the chain counter */
    g = this.frag.graph;        /* get the graph of the fragment */
    o = this.useOrbits();       /* and whether to use node orbits */
    while (true) {              /* try to find another extension */
      ++this.dst;               /* get the next destination index */
      while (this.dst > g.nodecnt) {     /* if there is none left */
        t = this.xemgr.next();  /* get the next extension edge type */
        while (t < 0) {         /* if there is none left */
          if (this.src > this.frag.dst) {
            --this.src;         /* if beyond the rightmost leaf, */
            this.idx = -1; }    /* simply go to the preceding node */
          else {                /* if on the rightmost path */
            s = g.nodes[this.src];
            this.src = -1;      /* get the current source node */
            for (i = s.deg; --i >= 0; ) {
              e = s.edges[i];   /* traverse the source node's edges */
              d = (e.src != s) ? e.src : e.dst;
              if ((d.mark > this.src) && (d.mark < s.mark)) {
                this.src = d.mark; /* find the adjacent node with the */
                this.idx = e.mark; /* largest index smaller than own, */
              }                    /* that is, find the parent node */
            }                      /* on the rightmost path */
            if (this.src < 0) return null;
          }                     /* if there is no parent node, abort */
          t = this.xemgr.first(g.nodes[this.src].type);
        }                       /* get the first extension edge */
        s = g.nodes[this.src];  /* get the current source node */
        if (s.deg >= this.xemgr.getDegree())
          continue;             /* skip if source has too many edges */
        if (this.idx < 0)       /* if add. node or rightmost leaf, */
          this.dst = this.src+1;/* start at node after the source */
        else {                  /* if extension from rightmost path */
          e = g.edges[this.idx];/* get the rightmost path edge */
          if (t <  e.type) continue;
          if (t == e.type) {    /* skip smaller edge types */
            d = (e.src != s) ? e.src : e.dst;
            if (this.xemgr.getDest() < d.type) continue;
          }                     /* skip smaller node types */
          this.dst = this.frag.dst;
        }                       /* edges closing rings must */
      }                         /* lead to the rightmost leaf */
      if (this.dst < g.nodecnt){/* if the new edge closes a ring */
        d = g.nodes[this.dst];  /* get the destination node */
        if (d.type != this.xemgr.getDest())
          continue;             /* check whether dest. type fits */
        s = g.nodes[this.src];  /* get the current source node */
        for (i = s.deg; --i >= 0; ) {
          e = s.edges[i];       /* traverse the incident edges */
          if ((e.src == d) || (e.dst == d)) break;
        }                       /* if an edge to destination does not */
        if (i < 0) break; }     /* exist, the extension can be used */
      else if (!o || (g.nodes[this.src].orbit == this.src))
        break;                  /* if the new edge adds a node, */
    }                           /* check for an orbit representative */
    /* W.r.t. checking the orbit of the destination node (if it is */
    /* not a new node), see the corresponding comment in next().   */
    return new Fragment(this.frag, this.src, this.dst,
                        this.xemgr.getType(),
                        this.xemgr.getDest());
  }  /* nextFrag() */           /* create an extended fragment */

  /*------------------------------------------------------------------*/
  /** Check whether the current ring extension is valid.
   *  <p>In order to reduce the number of generated fragments, rings
   *  are usually generated in only one form. It is checked whether
   *  the source of the first new ring edge is minimal over all edges
   *  of the ring (so that a ring is always attached to the node with
   *  minimal index) and whether the first and last edges of the ring
   *  allow to fix an orientation of the ring, only one of which is
   *  considered valid. Invalid rings are discarded in the function
   *  <code>ring()</code> that creates ring extensions.</p>
   *  @return whether the ring is valid (has the correct form)
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean validRing ()
  {                             /* --- check a ring extension */
    int  i, n;                  /* loop variable, default index */
    int  sm, dm;                /* indices of source and destination */
    Node node, s, d;            /* to traverse the ring nodes */
    Edge ring, e;               /* to traverse the ring edges */

    s = this.nodes[0];          /* get the source node, */
    e = this.edges[0];          /* the first ring edge, */
    d = this.nodes[1];          /* and its destination node */
    if ((d.mark >= 0) && (d.mark < s.mark))
      return false;             /* check for a forward first edge */
    n = this.emb.nodes.length;  /* get default destination index */
    for (i = this.size; --i > 0; ) {
      ring = this.edges[i];     /* traverse the other ring edges */
      if (ring.mark >= 0) continue;  /* that are not in the base */
      sm = (ring.src.mark >= 0) ? ring.src.mark : n;
      dm = (ring.dst.mark >= 0) ? ring.dst.mark : n;
      if (dm < sm) dm = sm;     /* get index of destination node */
      if (dm < this.dst) break; /* check for a better first edge */
    }                           /* (with smaller dest. node index) */
    if (i > 0) return false;    /* if a better way found, skip ring */
    this.sym = false;           /* default: locally asymmetric */
    ring = this.edges[this.size-1];
    if (ring.mark >= 0)       return true;
    node = (ring.src  != s) ? ring.src  : ring.dst;
    dm   = (node.mark >= 0) ? node.mark : n;
    if (dm        > this.dst) return true;
    if (dm        < this.dst) return false;
    if (ring.type > e.type)   return true;
    if (ring.type < e.type)   return false;
    if (node.type > d.type)   return true;
    if (node.type < d.type)   return false;
    return this.sym = true;     /* check for correct direction */
  }  /* validRing() */          /* and note the local symmetry */

  /*------------------------------------------------------------------*/
  /** Initialize the generation of equivalent ring extension variants.
   *  <p>If a ring start (and possibly also ends) with an edge that is
   *  equivalent to one or more edges already in the fragment (that is,
   *  edges that start at the same node, have the same type, and lead
   *  to nodes of the same type), these edges must be spliced with the
   *  already existing equivalent edges in the fragment. All possible
   *  ways of splicing the equivalent edges, which keep their individual
   *  order (that is, the order of the already existing edges and the
   *  order of the added edges), have to be tried. This function
   *  initializes this variant generation.</p>
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void initVars ()
  {                             /* --- init. ring extension variants */
    int   i;                    /* loop variable */
    Edge  e, r;                 /* to access/traverse the edges */
    Node  s, d, x;              /* to access/traverse the nodes */
    int[] p;                    /* buffer for reallocation */

    this.pmax =  0;             /* init. the equiv. position counter */
    this.pos2 = -1;             /* second position is not needed */
    r = this.edges[0];          /* get the first edge of the ring */
    s = this.nodes[0];          /* and its source and */
    d = this.nodes[1];          /* destination node */
    for (i = s.deg; --i >= 0; ) {
      e = s.edges[i];           /* traverse the edges of the source */
      if (e.mark <= this.frag.idx) continue;
      if (e.type != r.type) continue;   /* skip uneligible edges */
      x = (e.src != s) ? e.src : e.dst; /* (fixed or wrong type) */
      if (x.mark <  s.mark) continue;   /* skip backward edges */
      if (x.type != d.type) continue;   /* (to preceding node) */
      if (this.pmax >= this.eqpos.length) {
        p = new int[this.pmax +(this.pmax >> 1)];
        System.arraycopy(this.eqpos, 0, p, 0, this.pmax);
        this.eqpos = p;         /* enlarge the position array */
      }                         /* and copy existing elements */
      this.eqpos[this.pmax++] = e.mark;
    }                           /* collect equivalent positions */
    this.pmin = this.pmax;      /* get the first equivalent position */
    this.pos1 = (--this.pmin < 0) ? -1 : this.eqpos[this.pmin];
  }  /* initVars() */

  /*------------------------------------------------------------------*/
  /** Create the next ring extension variant.
   *  <p>If a ring start (and possibly also ends) with an edge that is
   *  equivalent to one or more edges already in the fragment (that is,
   *  edges that start at the same node, have the same type, and lead
   *  to nodes of the same type), these edges must be spliced with the
   *  already existing equivalent edges in the fragment. All possible
   *  ways of splicing the equivalent edges, which keep their individual
   *  order (that is, the order of the already existing edges and the
   *  order of the added edges), have to be tried. This function
   *  generates the next ring extension variant. Before it can be
   *  called, the function <code>initVars()</code> must have been
   *  invoked.</p>
   *  @return whether another ring variant was created
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean variant ()
  {                             /* --- create ring extension variant */
    if (this.pos1 < 0)          /* if all variants have been created, */
      return false;             /* abort with failure */
    this.pos1 = (--this.pmin < 0) ? -1 : this.eqpos[this.pmin];
    return true;                /* otherwise get the next position */
  }  /* variant() */            /* for the equivalent bond */

  /*------------------------------------------------------------------*/
  /** Reorder the edges of a fragment with a ring extension.
   *  <p>After a ring extension it may be necessary to reorder the
   *  edges of the resulting fragment, so that the edges get into the
   *  proper order w.r.t. the canonical form. In addition, it must be
   *  checked whether rings were added in the right order (if several
   *  rings were added). If not, the ring extension cannot be adapted
   *  and thus the function returns -1.</p>
   *  <p>This function does not actually reorganize the fragment if the
   *  ring extension can be adapted, but only stores the edges and nodes
   *  in their new order in internal arrays. In addition, it creates a
   *  map for reorganizing the nodes and edges, also in an internal
   *  buffer. Either of these may later be used to actually reorganize
   *  the fragment (as a sub-graph) as well as the embeddings. Note
   *  that these arrays and maps are not filled/created if the fragment
   *  need not be changed in any way. In this case the function returns
   *  +1, otherwise the result is 0.</p>
   *  @param  frag  the fragment to adapt
   *  @param  check whether to check the ring order
   *  @return whether the ring adaptation succeeded, that is:
   *          <p><table cellpadding=0 cellspacing=0>
   *          <tr><td>-1,&nbsp;</td>
   *              <td>if the ring adaptation failed,</td></tr>
   *          <tr><td align="right">0,&nbsp;</td>
   *              <td>if the ring adaptation succeeded,
   *              but the fragment needs to be modified,</td></tr>
   *          <tr><td>+1,&nbsp;</td>
   *              <td>if the ring extension
   *                  need not be adapted.</td></tr>
   *          </table></p>
   *  @since  2006.07.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int adaptRing (Fragment frag, boolean check)
  {                             /* --- adapt and check ring extension */
    int     i, k, r, n;         /* loop variables, indices */
    Graph   graph;              /* the fragment as a graph */
    Edge    e, x;               /* to traverse the edges */
    boolean changed;            /* whether fragment was changed */

    graph = this.prepare(frag); /* mark rings in the fragment */
    this.nodes[0] = graph.nodes[0];
    this.nodes[0].mark = 0;     /* store and mark the root node */
    x = graph.edges[frag.idx];  /* get the first edge of the ring */
    r = frag.ris[frag.ris.length-3];
    if (r >= 0) {               /* if insertion position is known */
      changed = (r != frag.idx);
      for (i = 0; i < r; i++)   /* copy up to first insertion point */
        this.edges[i] = graph.edges[i];
      this.edges[x.mark = r] = x;  /* store the first ring edge */
      for (k = n = 0; k <= r; k++) {
        e = this.edges[k];      /* traverse edges in the copied part */
        e.mark = k;             /* mark each edge as processed */
        if      (e.src.mark < 0) this.nodes[e.src.mark = ++n] = e.src;
        else if (e.dst.mark < 0) this.nodes[e.dst.mark = ++n] = e.dst;
      } }                       /* number and collect the nodes */
    else {                      /* if insertion position is unknown */
      for (n = i = 0; i < frag.idx; i++) {
        e = graph.edges[i];     /* traverse the edges ascendingly */
        if (((x.src.mark >= 0)  /* if incident to the marked part */
        ||   (x.dst.mark >= 0)) /* and no greater than the next edge */
        &&  (this.compareEdge(x, e, n+1) < 0))
          break;                /* the insertion position is found */
        this.edges[e.mark = i] = e; /* copy the edge to the buffer */
        if      (e.src.mark < 0) this.nodes[e.src.mark = ++n] = e.src;
        else if (e.dst.mark < 0) this.nodes[e.dst.mark = ++n] = e.dst;
      }                         /* mark and collect fixed nodes */
      this.edges[x.mark = r = i] = x;
      changed = (i != frag.idx);/* store the first ring edge */
      if      (x.src.mark < 0) this.nodes[x.src.mark = ++n] = x.src;
      else if (x.dst.mark < 0) this.nodes[x.dst.mark = ++n] = x.dst;
    }                           /* number and collect fixed nodes */
    for (k = r; ++k < graph.edgecnt; ) {
      if (i == frag.idx) i++;   /* copy the remaining edges */
      this.edges[k] = graph.edges[i++];
    }                           /* (complete the edge array) */
    if (check) {                /* if to check the ring order */
      this.makeWord(this.edges, ++r);   /* build code word prefix */
      this.word[r*4+1] = Integer.MAX_VALUE;
      if (this.makeCanonic(r, 0, n+1))  /* complete the code word */
        changed = true;         /* by making the fragment canonic */
      if (!this.isRingKey(graph, x))
        return -1;              /* check whether prefix is a ring key */
    }                           /* (no ring can be added later) */
    if (!changed) return 1;     /* if fragment is unchanged, abort */
    this.makeMap(graph, n);     /* create a map for nodes and edges */
    return 0;                   /* return that fragment needs change */
  }  /* adaptRing() */

  /*------------------------------------------------------------------*/
  /** Compare two edges with the precedence order of the canonical form.
   *  <p>The precedence order of the edge properties is:
   *  <ul><li>destination node index (ascending)
   *      <li>source node index (descending)</li>
   *      <li>edge attribute (ascending)</li>
   *      <li>node attribute (ascending)</li></li></ul>
   *  <p>This function is meant to compare edges from the same graph
   *  at each point where the next edge needs to be selected, when the
   *  graph (or rather its edge array) is rebuilt. At such a point
   *  all nodes incident to already processed edges are numbered.
   *  However, one of the nodes incident to the compared edges may
   *  not have been numbered yet. As this would make it impossible to
   *  compare the edges, the next number to be given to a node is
   *  also passed to the function.</p>
   *  @param  e1   the first  edge to compare
   *  @param  e2   the second edge to compare
   *  @param  next the index with which to number the next node
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the first edge is less than, equal to, or greater
   *          than the second edge
   *  @since  2006.04.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareEdge (Edge e1, Edge e2, int next)
  {                             /* --- compare two edges */
    Node s1, d1, s2, d2;        /* source and destination nodes */
    int  i1, i2;                /* indices of the destination nodes */

    s1 = e1.src; d1 = e1.dst;   /* get nodes of first edge */
    if ((s1.mark < 0) || ((d1.mark >= 0) && (d1.mark < s1.mark))) {
      s1 = d1; d1 = e1.src; }   /* exchange nodes if necessary */
    s2 = e2.src; d2 = e2.dst;   /* get nodes of second edge */
    if ((s2.mark < 0) || ((d2.mark >= 0) && (d2.mark < s2.mark))) {
      s2 = d2; d2 = e2.src; }   /* exchange nodes if necessary */
    i1 = (d1.mark >= 0) ? d1.mark : next;
    i2 = (d2.mark >= 0) ? d2.mark : next;
    if (i1      < i2)      return -1;  /* compare the indices */
    if (i1      > i2)      return +1;  /* of the destination nodes */
    if (s1.mark > s2.mark) return -1;  /* compare the indices */
    if (s1.mark < s2.mark) return +1;  /* of the source nodes */
    if (e1.type < e2.type) return -1;  /* compare the types */
    if (e1.type > e2.type) return +1;  /* of the two edges */
    if (d1.type < d2.type) return -1;  /* compare the types */ 
    if (d1.type > d2.type) return +1;  /* of the destination nodes */
    return 0;                   /* otherwise the edges are equal */
  }  /* compareEdge() */

  /*------------------------------------------------------------------*/
  /** Compare the current extension to a given fragment.
   *  <p>This function is used to determine whether the current
   *  extension is equivalent to a previously created one (and thus
   *  only an embedding has to be created from it and to be added to
   *  the corresponding fragment) or not (and thus a new fragment has
   *  to be created). It is designed as a comparison function, because
   *  the created fragments are kept as an ordered array, so that a
   *  binary search becomes possible.</p>
   *  @param  frag the fragment to compare to
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the fragment described by this extension is less
   *          than, equal to, or greater than the fragment given
   *          as an argument
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int compareToFrag (Fragment frag)
  {                             /* --- compare extension to fragment */
    int t1, t2;                 /* buffers for comparison */

    if (this.dst < frag.dst) return -1;  /* compare the indices */
    if (this.dst > frag.dst) return +1;  /* of the dest.  nodes */
    if (this.src > frag.src) return -1;  /* compare the indices */
    if (this.src < frag.src) return +1;  /* of the source nodes */
    t1 =      this.edges[    0   ].type;
    t2 = frag.list.edges[frag.idx].type;
    if (t1 < t2) return -1;     /* compare the types */
    if (t1 > t2) return +1;     /* of the (first) added edge */
    t1 =      this.nodes[    1   ].type;
    t2 = frag.list.nodes[frag.dst].type;
    if (t1 < t2) return -1;     /* compare the types */
    if (t1 > t2) return +1;     /* of the destination nodes */
    t1 = (this.size < 0) ? 1 : ((this.size > 0) ? -1 : 0);
    t2 = (frag.size < 0) ? 1 : ((frag.size > 0) ? -1 : 0);
    if (t1 < t2) return -1;     /* get the extension types */
    if (t1 > t2) return +1;     /* from the sizes and compare them */
    return (this.size <= 0)     /* compare ring ext. if necessary */
         ? 0 : this.compareRing(frag);
  }  /* compareToFrag() */

  /*------------------------------------------------------------------*/
  /** Create the (prefix of a) code word for a given edge array.
   *  <p>This function assumes that the node markers contain the node
   *  indices, which is the case if this function is called from one
   *  of the other <code>makeWord</code> functions.</p>
   *  @param  edges the array of edges for which to create the code word
   *  @param  n     the number of edges to consider
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void makeWord (Edge[] edges, int n)
  {                             /* --- construct (part of) code word */
    int  i, k;                  /* loop variables, buffers */
    Edge e;                     /* to traverse the edges */

    e = edges[0];               /* get first edge and root node */
    this.word[0] = (e.src.mark < e.dst.mark) ? e.src.type : e.dst.type;
    for (i = k = 0; i < n; i++) {
      e = edges[i];             /* traverse the graph's edges */
      if (e.src.mark < e.dst.mark) {
        this.word[++k] = e.dst.mark; /* if "forward" edge */
        this.word[++k] = e.src.mark; /* start with source node */
        this.word[++k] = e.type;
        this.word[++k] = e.dst.type; }
      else {                         /* if "backward" edge */
        this.word[++k] = e.src.mark; /* start with destination node */
        this.word[++k] = e.dst.mark;
        this.word[++k] = e.type;
        this.word[++k] = e.src.type;
      }                         /* describe an edge of the graph */
    }
  }  /* makeWord() */

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given edge array.
   *  <p>This function assumes that the node markers contain the node
   *  indices, which is the case if this function is called from one
   *  of the other <code>compareWord</code> functions.</p>
   *  @param  edges the array of edges to compare to
   *  @param  n     the number of edges to consider
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the internal code word is less than, equal to, or
   *          greater than the code word of the given edges array
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareWord (Edge[] edges, int n)
  {                             /* --- compare edges to code word */
    int  i, k;                  /* loop variables */
    Edge e;                     /* to traverse the edges */
    Node s, d;                  /* to traverse the nodes */

    e = edges[0];               /* compare the type of the root node */
    s = (e.src.mark < e.dst.mark) ? e.src : e.dst;
    if (s.type < this.word[0]) return -1;
    if (s.type > this.word[0]) return +1;
    for (i = k = 0; i < n; i++) {
      e = edges[i];             /* traverse the graph's edges */
      s = e.src; d = e.dst;     /* get the source and dest. nodes */
      if (s.mark > d.mark) { s = d; d = e.src; }
      if (d.mark < this.word[++k]) return -1;
      if (d.mark > this.word[  k]) return +1;
      if (s.mark > this.word[++k]) return -1;
      if (s.mark < this.word[  k]) return +1;
      if (e.type < this.word[++k]) return -1;
      if (e.type > this.word[  k]) return +1;
      if (d.type < this.word[++k]) return -1;
      if (d.type > this.word[  k]) return +1;
    }                           /* return sign of difference */
    return 0;                   /* otherwise return 'equal' */
  }  /* compareWord() */

  /*------------------------------------------------------------------*/
  /** Internal recursive function for the canonical form test.
   *  <p>In each recursive call to this function one edge is checked.
   *  If a possibility to construct a lexicographically smaller (prefix
   *  of a) code word is found or if all (prefixes of) code words that
   *  could be constructed are lexicographically greater, the function
   *  returns directly. Only if there is a possibility to construct an
   *  equal prefix, the function calls itself recursively.</p>
   *  @param  ei  the current edge index
   *  @param  ni  the current node index
   *  @param  cnt the number of already numbered nodes
   *  @return the lowest edge index at which the considered graph
   *          differs from the canonical form (in this recursion)
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int isCanonic (int ei, int ni, int cnt)
  {                             /* --- check prefix words recursively */
    int  i, k, c, m, r;         /* loop variable, node index, buffer */
    Edge e;                     /* to traverse/access the edges */
    Node s, d;                  /* to traverse/access the nodes */

    /* --- check for edges closing rings --- */
    if (ei >= this.size) {      /* if all edges have been matched */
      for (i = cnt; --i >= 0; ) {
        s = this.nodes[i];      /* traverse all nodes of the graph */
        if (s.mark > s.orbit) s.orbit = s.mark;
      }                         /* update the orbit representatives */
      return this.size;         /* return the total number of edges */
    }                           /* to indicate a successful match */
    d = this.nodes[cnt-1];      /* get the previous destination node */
    k = -1; c = (ei << 2) +1;   /* init. edge and code word index */
    if ((this.word[c]   <  cnt) /* if a new node is not added (ring) */
    ||  (this.word[c+1] == d.mark)) {  /* or added to rightmost leaf */
      m = (this.word[c] < cnt) ? this.word[c+1] : -1;
      for (i = d.deg; --i >= 0; ) {
        e = d.edges[i];         /* traverse the unmarked edges */
        if (e.mark >= 0) continue;    /* of the rightmost leaf */
        s = (e.src != d) ? e.src : e.dst;
        if (s.mark >  m)        /* check for an edge closing a ring */
          return ei;            /* that should precede the next edge */
        if (s.mark == m) k = i; /* note the index of the edge that */
      }                         /* matches the next reference edge */
    }                           /* (if this edge closes a ring) */

    /* --- process edge closing a ring --- */
    r = this.size;              /* set the default result */
    if (this.word[c] < cnt) {   /* if no new node is added */
      if (k < 0) return r;      /* check for a corresponding edge */
      e = d.edges[k];           /* get the corresponding edge */
      if (e.type < this.word[c+2]) return ei;  /* check the type */
      if (e.type > this.word[c+2]) return r;   /* of the edge */
      e.mark =  0;              /* mark the matching edge */
      k = this.isCanonic(ei+1, 0, cnt);
      e.mark = -1;              /* check remaining edges recursively, */
      return k;                 /* unmark the matching edge, and */
    }                           /* return the recursion result */

    /* --- process edge adding a node --- */
    m = this.word[c+1];         /* note index of new source node */
    while (d.mark > m) {        /* check old path up to new source */
      for (k = -1, i = d.deg; --i >= 0; ) {
        e = d.edges[i];         /* traverse the marked edges */
        if (e.mark < 0) return ei;
        s = (e.src != d) ? e.src : e.dst;
        if ((s.mark > k) && (s.mark < d.mark))
          k = s.mark;           /* find the adjacent node with the */
      }                         /* largest index smaller than own */
      d = this.nodes[k];        /* get next node on rightmost path */
    }                           /* (traverse from leaf up to source) */
    s = this.nodes[m];          /* get the current source node */
    for (i = 0; i < s.deg; i++) {
      e = s.edges[i];           /* traverse the unmarked edges */
      if (e.mark >= 0)             continue;
      if (e.type < this.word[c+2]) return ei;  /* check the type */
      if (e.type > this.word[c+2]) return r;   /* of the edge */
      d = (e.src != s) ? e.src : e.dst;
      if (d.type < this.word[c+3]) return ei;  /* check the type */
      if (d.type > this.word[c+3]) return r;   /* of the dest. node */
      e.mark = 0;               /* mark the edge and the node */
      this.nodes[d.mark = cnt] = d;
      k = this.isCanonic(ei+1, 0, cnt+1);
      e.mark = d.mark = -1;     /* check remaining edges recursively, */
      if (k < r) { if (k < this.fixed) return k; r = k; }
    }                           /* evaluate the recursion result */
    return r;                   /* return the overall result */
  }  /* isCanonic() */

  /*------------------------------------------------------------------*/
  /** Internal recursive function for making a given graph canonic.
   *  <p>This function works in basically the same way as the analogous
   *  function <code>isCanonic()</code>, with the only difference that
   *  whenever a smaller (prefix of a) code word is found, the function
   *  is not terminated, but continues with the new (prefix of a) code
   *  word, thus constructing the lexicographically smallest code word.
   *  </p>
   *  @param  ei  the current edge index
   *  @param  ni  the current node index
   *  @param  cnt the number of already numbered nodes
   *  @return whether the considered graphs needs to be changed
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean makeCanonic (int ei, int ni, int cnt)
  {                             /* --- construct canonic code word */
    int     i, k, c;            /* loop variables, indices */
    Edge    e, x;               /* to traverse/access the edges */
    Node    s, d, a;            /* to traverse/access the nodes */
    boolean changed;            /* whether code word was changed */

    c = (ei << 2) +1;           /* compute index in code word */
    if (ei >= this.size) {      /* if full code word is constructed */
      i = this.word[c]; this.word[c] = 0;
      k = cnt;                  /* reinstall the code word sentinel */
      if (i != 0) {             /* if the code word was changed */
        while (--k >= 0) {      /* copy the new orbit representatives */
          s = this.nodes[k];                       s.orbit = s.mark; }
        return true; }          /* return that the code word changed */
      else {                    /* if the code word was not changed */
        while (--k >= 0) {      /* update the orbit representatives */
          s = this.nodes[k]; if (s.mark > s.orbit) s.orbit = s.mark; }
        return false;           /* return that the code word */
      }                         /* was not changed */
    }

    /* --- edge closing a ring --- */
    e = null; s = null;         /* dummy initialization */
    for ( ; ni < cnt; ni++) {   /* search for edges closing rings */
      d = this.nodes[ni];       /* traverse possible dest. nodes */
      for (i = d.deg; --i >= 0; ) {
        e = d.edges[i];         /* traverse the unmarked edges */
        if (e.mark >= 0) continue;
        s = (e.src != d) ? e.src : e.dst;
        if ((s.mark < ni) && (s.mark >= 0))
          break;                /* if there is a backward edge, */
      }                         /* abort the search loop */
      if (i < 0) continue;      /* if no edge found, go to next node */
      while (--i >= 0) {        /* traverse the remaining unmarked */
        x = d.edges[i];         /* edges of the same destination node */
        if (x.mark >= 0) continue;
        a = (x.src != d) ? x.src : x.dst;
        if ((a.mark < ni) && (a.mark > s.mark)) {
          e = x; s = a; }       /* try to find a source node */
      }                         /* with a larger index */
      if (d.mark > this.word[c])
        return false;           /* compare destination node index */
      if (d.mark < this.word[c]) {
        this.word[c]   = d.mark;/* set new destination node index */
        this.word[c+1] = Integer.MIN_VALUE;
      }                         /* on change invalidate next entry */
      if (s.mark < this.word[c+1])
        return false;           /* compare source node index */
      if (s.mark > this.word[c+1]) {
        this.word[c+1] = s.mark;/* set new source node index */
        this.word[c+2] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      if (e.type > this.word[c+2])
        return false;           /* compare the edge type */
      if (e.type < this.word[c+2]) {
        this.word[c+2] = e.type;/* set new edge type */
        this.word[c+3] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      if (d.type > this.word[c+3])
        return false;           /* compare destination node type */
      if (d.type < this.word[c+3]) {
        this.word[c+3] = d.type;/* set new destination node type */
        this.word[c+4] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      e.mark =  0;              /* mark the edge and recurse */
      changed = this.makeCanonic(ei+1, ni, cnt);
      if (changed) this.edges[ei] = e;
      e.mark = -1;              /* unmark the edge again */
      return changed;           /* return whether the code word */
    }                           /* was changed in the recursion */

    /* --- edge to a new node --- */
    if (cnt > this.word[c])     /* if beyond small enough dest., */
      return false;             /* abort the function */
    changed = false;            /* default: code word is unchanged */
    for (k = cnt; --k >= 0; ) { /* traverse possible source nodes */
      s = this.nodes[k];        /* (in reverse order) */
      for (i = s.deg; --i >= 0; )
        if (s.edges[i].mark < 0) break;
      if (i >= 0) break;        /* if an unmarked edge found, */
    }                           /* abort the search loop */
    if (k < 0) return false;    /* safety check (should not happen) */
    if (cnt < this.word[c]) {   /* if dest. node has smaller index, */
      this.word[c]   = cnt;     /* replace the code word letter */
      this.word[c+1] = Integer.MIN_VALUE;
    }                           /* invalidate the next entry */
    if (k < this.word[c+1])     /* if source node has smaller index, */
      return false;             /* the existing code word is better */
    if (k > this.word[c+1]) {   /* if source node has larger index, */
      this.word[c+1] = k;       /* replace the code word letter */
      this.word[c+2] = Integer.MAX_VALUE;
    }                           /* invalidate the next entry */
    for (i = 0; i < s.deg; i++) {
      e = s.edges[i];           /* traverse the unmarked edges */
      if (e.mark >= 0) continue;
      if (e.type > this.word[c+2])
        return changed;         /* compare the edge type */
      if (e.type < this.word[c+2]) {
        this.word[c+2] = e.type;/* set new edge type */
        this.word[c+3] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      d = (e.src != s) ? e.src : e.dst;
      if (d.type > this.word[c+3])
        return changed;         /* compare destination node type */
      if (d.type < this.word[c+3]) {
        this.word[c+3] = d.type;/* set new destination node type */
        this.word[c+4] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      e.mark = 0;               /* mark the edge and the node */
      this.nodes[d.mark = cnt] = d;
      if (this.makeCanonic(ei+1, ni, cnt+1)) {
        this.edges[ei] = e; changed = true; }
      e.mark = d.mark = -1;     /* recursively construct code word, */
    }                           /* then unmark edge and node again */
    return changed;             /* return whether edges were replaced */
  }  /* makeCanonic() */

  /*------------------------------------------------------------------*/
  /** Check whether a fragment contains unclosable rings.
   *  <p>If the output is restricted to fragments containing only closed
   *  rings, the restricted extensions of a depth-first search spanning
   *  tree canonical form render all nodes with an index smaller than
   *  the maximum source index unextendable. If such a node has only
   *  one incident ring edge, the ring of which this edge is part cannot
   *  be closed by future extensions. Hence neither this fragment nor
   *  any of its extensions can produce output and thus it can be
   *  pruned.</p>
   *  @param  frag the fragment to check for unclosable rings
   *  @return whether the given fragment contains unclosable rings
   *  @since  2006.05.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean hasUnclosableRings (Fragment frag)
  {                             /* --- check for uncloseable rings */
    int   i, k, n;              /* loop variable, edge counter */
    Graph graph;                /* the fragment as a graph */
    Node  s, d;                 /* to traverse the nodes */
    Edge  e;                    /* to traverse the edges on the path */

    graph = frag.getGraph();    /* get the fragment as a graph */
    for (i = graph.nodecnt; --i >= 0; )
      graph.nodes[i].mark = i;  /* mark all nodes with their index */
    s = graph.nodes[frag.dst];  /* get rightmost leaf (end of path) */
    while (s.mark > 0) {        /* construct rightmost path upwards */
      for (k = -1, i = s.deg; --i >= 0; ) {
        e = s.edges[i];         /* traverse the marked edges */
        d = (e.src != s) ? e.src : e.dst;
        if ((d.mark > k) && (d.mark < s.mark))
          k = d.mark;           /* find the adjacent node with the */
      }                         /* largest index smaller than own */
      s.mark = -1;              /* mark current node as extendable */
      s = graph.nodes[k];       /* and get the next path node */
    }
    s.mark = -1;                /* mark root node as extendable */
    for (i = graph.nodecnt; --i > 0; ) {
      s = graph.nodes[i];       /* traverse the unextendable nodes */
      if (s.mark < 0) continue; /* (have their indices as markers) */
      s.mark = -1;              /* unmark the node */
      for (n = 0, k = s.deg; --k >= 0; )
        if (s.edges[k].isInRing()) n++;
      if (n == 1) return true;  /* if there is a single ring edge, */
    }                           /* a ring cannot be closed anymore */
    return false;               /* all rings may be closable */
  }  /* hasUnclosableRings() */

  /*------------------------------------------------------------------*/
  /** Create the code word for a given graph as a string.
   *  <p>This function allows for the code word of the graph already
   *  being available in the internal code word buffer. In this case
   *  the function should be called with <code>create == false</code>
   *  (the graph is only used to retrieve the number of edges).</p>
   *  @param  graph  the graph for which to create a code word string
   *  @param  create whether the code word needs to be created
   *  @return a code word (as a string) for the given graph
   *  @since  2006.05.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected String describe (Graph graph, boolean create)
  {                             /* --- create a graph's code word */
    int          i, k, n;       /* loop variable, buffers */
    StringBuffer s;             /* created description */
    TypeMgr      nmgr, emgr;    /* node and edge type manager */

    if (create)                 /* construct graph's code word */
      this.makeWord(graph, graph.edgecnt);
    nmgr = graph.getNodeMgr();  /* get the node type manager */
    emgr = graph.getEdgeMgr();  /* and the edge type manager */
    k = this.word[0];           /* get and decode type of root node */
    if (graph.coder != null) k = graph.coder.decode(k);
    s = new StringBuffer(nmgr.getName(k));
    n = graph.edgecnt << 2;     /* get the number of characters */
    for (i = 0; i < n; ) {      /* traverse the characters */
      s.append('|');            /* separator for edges */
      s.append(this.word[++i]); /* destination node index */
      s.append(' ');            /* separator for indices */
      s.append(this.word[++i]); /* source      node index */
      s.append(' ');            /* separator to edge type */
      s.append(emgr.getName(this.word[++i]));
      s.append(' ');            /* separator to node type */
      k = this.word[++i];       /* get and decode the node type */
      if (graph.coder != null) k = graph.coder.decode(k);
      s.append(nmgr.getName(k));
    }                           /* store the edge descriptions */
    return s.toString();        /* return created string description */
  }  /* describe() */
  
  //shirui
  public Object clone(){
	  CanonicalForm obj = null;
	  obj = (CanonicalForm) super.clone();
	  return obj;
  }
  

}  /* class CnFDepth */
