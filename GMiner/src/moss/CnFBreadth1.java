/*----------------------------------------------------------------------
  File    : CnFBreadth1.java
  Contents: breadth-first search spanning tree canonical form
            and corresponding maximum edge source extension
  Author  : Christian Borgelt
  History : 2002.03.11 file created as file submol.java
            2002.04.02 function compareToFrag() added
            2003.08.07 complete rewrite of extension functions
            2005.06.08 elimination of forward edges added
            2005.07.19 forward edges allowed, backward edges eliminated
            2005.07.21 element manager added, excluded check removed
            2005.07.23 order of the extensions inverted
            2005.08.11 class Extension made abstract
            2006.04.10 function compareToFrag() for rings simplified
            2006.04.11 function compareEdge() added (to compare edges)
            2006.05.02 functions makeCanonic() and makeWord() added
            2006.05.03 function makeCanonic() completed and debugged
            2006.05.10 function describe added (code word printing)
            2006.05.12 extension-specific ring filtering moved here
            2006.05.16 bug in function ring fixed (ring direction)
            2006.05.18 adapted to new field Extension.dst
            2006.05.31 function isCanonic() extended, result changed
            2006.06.06 adapted to changed type of edge flags
            2006.06.07 function compareWord() added
            2006.07.01 adaptation of ring extensions moved here
            2006.07.03 edge splicing for ring extension adaptation
            2006.07.04 second functions makeWord()/compareWord() added
            2006.07.06 function adaptRing() redesigned completely
            2006.07.10 marking of non-ring edges added to adaptRing()
            2007.03.02 default constructor without arguments added
            2007.03.23 bug in adaptRing() fixed (last fixed edge)
            2007.03.24 adapted to new functions of super-class
            2007.06.21 adapted to new class TypeMgr (node/edge types)
            2007.10.19 bug in ring extension handling fixed
            2008.03.06 adapted to modified ring key test (isRingKey)
            2009.04.29 adapted to modified class Extension
            2009.05.05 harmless bug in compareEdge() removed
            2009.07.06 extra check of new edge from same source added
            2010.01.22 renamed to CnFBreadth1.java (canonical form)
            2010.01.28 check of maximum degree of source node added
            2010.02.03 workaround for Java bug in makeCanonic() added
            2011.02.16 orbits used to suppress some equivalent siblings
            2011.02.22 functions init(), next() and nextFrag() reworked
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for breadth-first search spanning tree canonical form
 *  and the corresponding maximum source extensions.
 *  <p>Maximum source extensions are the restricted extensions of
 *  a breadth-first search spanning tree canonical form. Only nodes
 *  having an index no less than the maximum source of an edge (where
 *  the source of an edge is the incident node with the smaller index)
 *  may be extended by adding an edge. Edges closing rings must lead
 *  "forward", that is, must lead from a node with a smaller index
 *  to a node with a larger index. In addition, at the node with the
 *  maximum source index added edges must succeed all already incident
 *  edges that have this node as a source node.</p>
 *  <p>For comparing edges and constructing code words the following
 *  precedence order of the edge properties is used:
 *  <ul><li>source node index (ascending)</li>
 *      <li>edge attribute (ascending)</li>
 *      <li>node attribute (ascending)</li>
 *      <li>destination node index (ascending)</li></ul>
 *  Hence the general form of a code word is<br>
 *  a (i<sub>s</sub> b a i<sub>d</sub>)<sup>m</sup><br>
 *  where a is a node attribute, b an edge attribute, i<sub>s</sub>
 *  the index of the source node of an edge, i<sub>d</sub> the index
 *  of the destination node of an edge and m the number of edges.</p>
 *  <p>The difference to <code>CnFBreadth2</code> is the position of
 *  the destination node index in the precedence order of the edge
 *  properties. While in <code>CnFBreadth2</code> it is the second
 *  property to be compared, it is the last in this class.</p>
 *  @author Christian Borgelt
 *  @since  2003.08.06/2005.08.11 */
/*--------------------------------------------------------------------*/
public class CnFBreadth1 extends CanonicalForm {

  /*------------------------------------------------------------------*/
  /** Create a breadth-first search spanning tree canonical form.
   *  @since  2007.03.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public CnFBreadth1 ()
  { super(); this.mode |= ALLORBS; }

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
   *  @since  2003.08.06/2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean init (Fragment frag, Embedding emb)
  {                             /* --- initialize an extension */
    if ((this.mode & ALLEXTS) != 0)  /* if to generate all exts., */
      return super.init(frag, emb);  /* call superclass function */
    this.frag = frag;           /* note the (possibly new) fragment */
    this.src  = frag.src;       /* start with the previous source */
    if (this.useOrbits()) {     /* if node orbits can be used */
      while (frag.graph.nodes[this.src].orbit != this.src)
        if (++this.src >= frag.graph.nodecnt)
          return false;         /* find an orbit representative */
    }                           /* as the (new) source node */
    /* Source nodes that are not orbit representatives are skipped, */
    /* because extending them only creates equivalent siblings that */
    /* are not canonical and thus will be discarded later anyway.   */
    /* If no source node can be found that represents its orbit,    */
    /* no extensions can be created (regardless of the embedding).  */
    this.idx  = -1;             /* init. index of the current edge */
    this.size = -1;             /* clear the ring size and flags */
    this.all  =  0;             /* (also extension type indicator) */
    this.pos1 = this.pos2 = -1; /* clear ring variant variables */
    this.pmin = this.pmax = -1; /* (make sure state is defined) */
    this.emb  = emb;            /* note the embedding to extend */
    if (emb != null)            /* if an embedding is given, */
      emb.index();              /* mark the embedding in its graph */
    else {                      /* if no embedding is given, */
      frag.graph.index();       /* index nodes of the fragment graph */
      this.dst = frag.graph.nodecnt;
      this.src--;               /* init. to last extension of node */
    }                           /* before the current source node */
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
   *  @since  2003.08.06/2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean next ()
  {                             /* --- create the next extension */
    Node s, d, o[], n[];        /* to traverse/access the nodes */
    Edge e;                     /* to traverse/access the edges */
    int  t;                     /* node or edge type */

    if ((this.mode & ALLEXTS) != 0)
      return super.next();      /* generate all exts with superclass */

    this.chcnt = this.frag.chcnt;     /* copy the chain counter */

    /* --- continue an old extension --- */
    if (((this.mode & EQVARS) != 0)
    &&   (this.pos1 >= 0)       /* if there is another equivalent */
    &&    this.variant())       /* variant of the previous ring, */
      return true;              /* return "extension successful" */
    if (((this.mode & RING)   != 0)
    &&   (this.all  != 0)       /* if there is another ring flag */
    &&    this.ring())          /* and some ring is admissible, */
      return true;              /* return "extension successful" */
    if (((this.mode & CHAIN)  != 0)
    &&   (this.size == 0)       /* if last extension was an edge and */
    &&    this.chain())         /* it can be extended into a chain, */
      return true;              /* return "extension successful" */

    /* --- find a new extension --- */
    o = (this.useOrbits()) ? this.frag.graph.nodes : null;
    n = this.emb.nodes;         /* get the nodes of the embedding */
    s = n[this.src];            /* and the current source node */
    while (true) {              /* find the next unprocessed edge */
      while (++this.idx >= s.deg) {
        do {                    /* if node's last edge is processed */
          if (++this.src >= n.length) {
            this.emb.mark(-1);  /* go to the next extendable node */
            return false;       /* and if there is none, abort */
          }                     /* (no more extensions to create) */
        } while ((o != null)    /* check for an orbit representative */
        &&       (o[this.src].orbit != this.src));
        /* The orbit field of the node structure contains the index  */
        /* of the node that represents the orbit of the node. It is  */
        /* filled in the canonical form test with the smallest index */
        /* of a node in this orbit. Only orbit representatives need  */
        /* to be extended, because extending other nodes in an orbit */
        /* leads to equivalent siblings (that are not canonical).    */
        s = n[this.src];        /* get the new source node and */
        this.idx = -1;          /* start with the node's first edge */
      }                         /* (idx is incremented before used) */
      e = s.edges[this.idx];    /* get the next edge of the source */
      if (e.mark != -1)         /* if the edge is in the embedding */
        continue;               /* or excluded, it cannot be added */
      /* Edges that are already in the embedding have e.mark >= 0, */
      /* excluded edges are marked with a value of e.mark < -1.    */
      d = (s != e.src) ? e.src : e.dst;    /* get destination node */
      if (d.mark < 0) {         /* if the new edge adds a node */
        if (n.length +this.chcnt >= this.max)
          continue;             /* check whether a new node is ok */
        this.dst = n.length; }  /* assign the next free node index */
      else {                    /* if the new edge closes a ring */
        if (d.mark <= this.src) /* skip edges that lead "backward" */
          continue;             /* in the fragment (not canonical) */
        /* At first sight it is plausible that one can reduce the  */
        /* number of equivalent siblings by checking the orbit of  */
        /* the destination node, allowing a connection only if the */
        /* destination node is the representative of its orbit.    */
        /* Unfortunately this does not work as can be demonstrated */
        /* by the following graphs (in SMILES): C1CCCCC1C2CCCCC2   */
        /* (an edge closing either of the two rings is ruled out)  */
        /* and O12OO3O1N4N3C5C4CC52 (the edge corresponding to the */
        /* label 2 is ruled out, even though the orbits differ).   */
        this.dst = d.mark;      /* note the destination node index */
      }                         /* (node is already in the fragment) */
      if ((this.frag.idx >= 0)  /* if same source as preceding step */
      &&  (this.frag.src == this.src)) {
        t = this.emb.edges[this.frag.idx].type;
        if (e.type <  t) continue;
        if (e.type == t) {      /* skip smaller edge types */
          t = n[this.frag.dst].type;
          if ( (d.type <  t)    /* skip smaller node types */
          ||  ((d.type == t) && (this.dst < this.frag.dst)))
            continue;           /* and for equal node types */
        }                       /* skip smaller destination indices */
      }                         /* (compare edges lexicographically) */
      /* An extension edge from the node that was already extended  */
      /* in the previous step is admissible only if its description */
      /* is lexicographically no smaller than the description of    */
      /* any previous extension edge from the same node.            */
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
    int     t, i;               /* node/edge type and dest. index */
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
          if (++this.src >= g.nodecnt) /* get the next source index */
            return null;        /* and if there is none left, abort */
          s = g.nodes[this.src];/* check for an orbit representative */
          if (o && (s.orbit != this.src)) continue;           
          /* Source nodes that are not orbit representatives can be  */
          /* skipped, because extending them only creates equivalent */
          /* siblings that are not canonical and will be discarded.  */
          t = this.xemgr.first(s.type);
        }                       /* get the first extension edge */
        s = g.nodes[this.src];  /* get the current source node */
        if (s.deg >= this.xemgr.getDegree())
          continue;             /* skip if source has too many edges */
        i = this.src;           /* get default destination index */
        if (s.deg > 0) {        /* if there are incident edges, */
          e = s.edges[s.deg-1]; /* get the source node's last edge */
          d = (e.src != s) ? e.src : e.dst;
          if (d.mark > this.src) {  /* if last edge leads downward */
            if (t <  e.type) continue;
            if (t == e.type) {  /* skip smaller edge types */
              t = this.xemgr.getDest();
              if (t <  d.type) continue;
              if (t == d.type) i = d.mark;
            }                   /* skip smaller node types and */
          }                     /* for equal node types ensure */
        }                       /* a greater destination index */
        /* An extension edge from the node that was already extended  */
        /* in the previous step is admissible only if its description */
        /* is lexicographically no smaller than the description of    */
        /* any previous extension edge from the same node.            */
        this.dst = i+1;         /* set the new destination index */
      }                         /* (is checked in while condition) */
      if ((this.dst >= g.nodecnt)
      ||  (g.nodes[this.dst].type == this.xemgr.getDest()))
        break;                  /* if the edge leads to a new node or */
    }                           /* the destination type fits, abort */
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
    int  i;                     /* loop variable */
    Node s, d;                  /* to traverse the ring nodes */
    Edge frst, last;            /* to access first and last edge */

    s = this.nodes[0];          /* get the current source node */
    for (i = this.size; --i > 0; ) {
      d = this.nodes[i];        /* traverse the ring nodes and */
      if ( (d.mark >= 0)        /* check whether the ring is */
      &&   (d.mark <  s.mark)   /* admissible for this source */
      &&  ((this.edges[i  ].mark < 0)
      ||   (this.edges[i-1].mark < 0)))
        break;                  /* (no ring node that is incident to */
    }                           /* a new edge (mark < 0) must have */
    if (i > 0) return false;    /* a smaller index than the source) */
    this.sym = false;           /* default: locally asymmetric */
    frst = this.edges[0];       /* check first and last ring edge */
    last = this.edges[this.size-1];  /* if only the first is new, */
    if (last.mark >= 0) return true; /* the ring direction is ok */
    if (last.type > frst.type) return true;   /* compare the */
    if (last.type < frst.type) return false;  /* edge types */
    d = (last.src != s) ? last.src : last.dst;
    s = this.nodes[1];          /* get the destination nodes */
    if (d.type    > s.type)    return true;   /* compare the */
    if (d.type    < s.type)    return false;  /* destination types */
    if ((d.mark >= 0) && (d.mark < this.dst))
      return false;             /* compare the destination indices */
    return this.sym = true;     /* note the local symmetry */
  }  /* validRing() */

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
    int  i;                     /* loop variable */
    Edge e, r;                  /* to access/traverse the edges */
    Node s, d, x;               /* to access/traverse the nodes */

    this.pmin = this.emb.edges.length;
    this.pmax = -1;             /* init. the insertion position range */
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
      if (e.mark < this.pmin) this.pmin = e.mark;
      if (e.mark > this.pmax) this.pmax = e.mark;
    }                           /* find range of equivalent edges */
    if (this.pmax < 0) {        /* if there are no equivalent edges, */
      this.pos1 = this.pos2 = -1; return;}/* abort with unknown pos. */
    this.pos1 = ++this.pmax;    /* compute the initial position(s) */
    this.pos2 = (this.sym) ? ++this.pmax : -1;
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
    if  (--this.pos1 >= this.pmin) /* shift first edge to the left */
      return true;                 /* if shift is possible, abort */
    if ((  this.pos2 < 0)       /* check if second edge can be moved */
    ||  (--this.pos2 <= this.pmin)) {  /* if not (or there is none), */
      this.pos1 = -1; return false; }  /* all variants are created */
    this.pos1 = this.pos2-1;    /* place first edge before second */
    return true;                /* return 'next variant created' */
  }  /* variant() */

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
    int     i, k, r, n;         /* loop variable, indices */
    Graph   graph;              /* the fragment as a graph */
    Edge    e, x;               /* to traverse the edges */
    boolean changed;            /* whether fragment was changed */

    graph = this.prepare(frag); /* mark rings in the fragment */
    this.nodes[0] = graph.nodes[0];
    this.nodes[0].mark = 0;     /* store and mark the root node */
    x = graph.edges[frag.idx];  /* get the first edge of the ring */
    k = frag.ris[frag.ris.length-3];
    if (k >= 0) {               /* if insertion position is known */
      changed = (k != frag.idx);
      for (i = 0; i < k; i++)   /* copy up to first insertion point */
        this.edges[i] = graph.edges[i];
      this.edges[x.mark = k++] = x;   /* store the first ring edge */
      r = frag.ris[frag.ris.length-2];
      if (r > 0) {              /* if last ring edge is equivalent */
        while (k < r)           /* copy up to second insertion point */
          this.edges[k++] = graph.edges[i++];
        this.edges[k++] = graph.edges[n = graph.edgecnt-1];
        if (r != n) changed = true;   /* store the last ring edge and */
      }                         /* update whether fragment is changed */
      r = frag.ris[frag.ris.length-1];
      while (k <= r)            /* copy remaining equivalent edges */
        this.edges[k++] = graph.edges[i++];
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
   *  <ul><li>source node index (ascending)</li>
   *      <li>edge attribute (ascending)</li>
   *      <li>node attribute (ascending)</li>
   *      <li>destination node index (ascending)</li></ul></p>
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
    if (s1.mark < s2.mark) return -1;  /* compare the indices */
    if (s1.mark > s2.mark) return +1;  /* of the source nodes */
    if (e1.type < e2.type) return -1;  /* compare the types */
    if (e1.type > e2.type) return +1;  /* of the two edges */
    if (d1.type < d2.type) return -1;  /* compare the types */ 
    if (d1.type > d2.type) return +1;  /* of the destination nodes */
    i1 = (d1.mark >= 0) ? d1.mark : next;
    i2 = (d2.mark >= 0) ? d2.mark : next;
    if (i1      < i2)      return -1;  /* compare the indices */
    if (i1      > i2)      return +1;  /* of the destination nodes */
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
   *  @since  2002.04.02/2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int compareToFrag (Fragment frag)
  {                             /* --- compare extension to fragment */
    int t1, t2;                 /* buffers for comparison */

    if (this.src < frag.src) return -1;  /* compare the indices */
    if (this.src > frag.src) return +1;  /* of the source nodes */
    t1 =      this.edges[    0   ].type;
    t2 = frag.list.edges[frag.idx].type;
    if (t1 < t2) return -1;     /* compare the types */
    if (t1 > t2) return +1;     /* of the (first) added edge */
    t1 =      this.nodes[    1   ].type;
    t2 = frag.list.nodes[frag.dst].type;
    if (t1 < t2) return -1;     /* compare the types */
    if (t1 > t2) return +1;     /* of the destination nodes */
    if (this.dst < frag.dst) return -1;  /* compare the indices */
    if (this.dst > frag.dst) return +1;  /* of the destination nodes */
    t1 = (this.size > 0) ? 1 : ((this.size < 0) ? -1 : 0);
    t2 = (frag.size > 0) ? 1 : ((frag.size < 0) ? -1 : 0);
    if (t1 > t2) return -1;     /* get the extension types */
    if (t1 < t2) return +1;     /* from the sizes and compare them */
    return (this.size <= 0) ? 0 : this.compareRing(frag);
  }  /* compareToFrag() */      /* compare ring ext. if necessary */

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
        this.word[++k] = e.src.mark; /* if "forward" edge */
        this.word[++k] = e.type;     /* start with source node */
        this.word[++k] = e.dst.type;
        this.word[++k] = e.dst.mark; }
      else {                         /* if "backward" edge */
        this.word[++k] = e.dst.mark; /* start with dest.  node */
        this.word[++k] = e.type;
        this.word[++k] = e.src.type;
        this.word[++k] = e.src.mark;
      }                         /* describe an edge of the graph */
    }                           /* (four characters per edge) */
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
      if (s.mark < this.word[++k]) return -1;
      if (s.mark > this.word[  k]) return +1;
      if (e.type < this.word[++k]) return -1;
      if (e.type > this.word[  k]) return +1;
      if (d.type < this.word[++k]) return -1;
      if (d.type > this.word[  k]) return +1;
      if (d.mark < this.word[++k]) return -1;
      if (d.mark > this.word[  k]) return +1;
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

    if (ei >= this.size) {      /* if all edges have been matched */
      for (i = cnt; --i >= 0; ) {
        s = this.nodes[i];      /* traverse all nodes of the graph */
        if (s.mark < s.orbit) s.orbit = s.mark;
      }                         /* update the orbit representatives */
      return this.size;         /* return the total number of edges */
    }                           /* to indicate a successful match */
    c = (ei << 2) +1;           /* compute index in code word */
    for ( ; ni < this.word[c]; ni++) {
      s = this.nodes[ni];       /* check nodes before current source */
      for (i = s.deg; --i >= 0; )
        if (s.edges[i].mark < 0)/* if there is an unmarked edge */
          return ei;            /* from a node with a smaller index, */
    }                           /* the fragment is not canonical */
    r = this.size;              /* set the default result and */
    s = this.nodes[ni];         /* get the current source node */
    for (i = 0; i < s.deg; i++) {
      e = s.edges[i];           /* traverse the unmarked edges */
      if (e.mark >= 0)             continue;
      if (e.type < this.word[c+1]) return ei;  /* check the type */
      if (e.type > this.word[c+1]) return r;   /* of the edge */
      d = (e.src != s) ? e.src : e.dst;
      if (d.type < this.word[c+2]) return ei;  /* check the type */
      if (d.type > this.word[c+2]) return r;   /* of the dest. node */
      m = d.mark;               /* note the node marker and */
      k = (m < 0) ? cnt : m;    /* get the destination node index */
      if (k      < this.word[c+3]) return ei;  /* check the index */
      if (k      > this.word[c+3]) continue;   /* of the dest. node */
      e.mark = 0;               /* mark the matching edge (and node) */
      if (m < 0) { this.nodes[d.mark = cnt++] = d; }
      k = this.isCanonic(ei+1, ni, cnt);
      if (m < 0) { d.mark = -1;        cnt--;      }
      e.mark = -1;              /* unmark edge (and node) again */
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
    int     i, k, c, m;         /* loop variable, node index, buffer */
    Edge    e;                  /* to traverse/access the edges */
    Node    s, d;               /* to traverse/access the nodes */
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
          s = this.nodes[k]; if (s.mark < s.orbit) s.orbit = s.mark; }
        return false;           /* return that the code word */
      }                         /* was not changed */
    }
    while (true) {              /* source node search loop */
      if ((ni >= cnt)           /* if beyond already numbered nodes */
      ||  (ni >  this.word[c])) /* or beyond a small enough source, */
        return false;           /* abort the recursion */
      // In Java 1.6.0 the above test may not be written as:
      //   if ((ni >  this.word[c])
      //   ||  (ni >= cnt))
      //     return false;
      // In this form, the test randomly fails and the return
      // statement is executed even though the condition is false.
      // This seems to be a bug in the Java compiler/interpreter.
      s = this.nodes[ni];       /* traverse possible source nodes */
      for (i = s.deg; --i >= 0; )
        if (s.edges[i].mark < 0) break;
      if (i >= 0) break;        /* check for an unmarked edge */
      ni++;                     /* if all edges are marked, */
    }                           /* go to the next node */
    if (ni < this.word[c]) {    /* if source node has smaller index, */
      this.word[c]   = ni;      /* replace the code word letter */
      this.word[c+1] = Integer.MAX_VALUE;
    }                           /* (so that the rest is replaced) */
    changed = false;            /* default: code word is unchanged */
    for (i = 0; i < s.deg; i++) {
      e = s.edges[i];           /* traverse the unmarked edges */
      if (e.mark >= 0) continue;
      if (e.type > this.word[c+1])
        return changed;         /* compare the edge type */
      if (e.type < this.word[c+1]) {
        this.word[c+1] = e.type;/* set new edge type */
        this.word[c+2] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      d = (e.src != s) ? e.src : e.dst;
      if (d.type > this.word[c+2])
        return changed;         /* compare destination node type */
      if (d.type < this.word[c+2]) {
        this.word[c+2] = d.type;/* set new destination node type */
        this.word[c+3] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      m = d.mark;               /* note the node marker and */
      k = (m < 0) ? cnt : m;    /* get the destination node index */
      if (k      > this.word[c+3])
        continue;               /* compare destination node index */
      if (k      < this.word[c+3]) {
        this.word[c+3] = k;     /* set new destination node index */
        this.word[c+4] = Integer.MAX_VALUE;
      }                         /* on change invalidate next entry */
      e.mark = 0;               /* mark the edge (and node) */
      if (m < 0) { this.nodes[d.mark = cnt++] = d; }
      if (this.makeCanonic(ei+1, ni, cnt)) {
        this.edges[ei] = e; changed = true; }
      if (m < 0) { d.mark = -1;        cnt--;      }
      e.mark = -1;              /* recursively construct code word, */
    }                           /* then unmark edge (and node) again */
    return changed;             /* return whether edges were replaced */
  }  /* makeCanonic() */

  /*------------------------------------------------------------------*/
  /** Check whether a fragment contains unclosable rings.
   *  <p>If the output is restricted to fragments containing only
   *  closed rings, the restricted extensions of a breadth-first search
   *  spanning tree canonical form render all nodes not on the rightmost
   *  path unextendable. If such a node has only one incident ring
   *  edge, the ring of which this edge is part cannot be closed by
   *  future extensions. Hence neither this fragment nor any of its
   *  extensions can produce output and thus it can be pruned.</p>
   *  @param  frag the fragment to check for unclosable rings
   *  @return whether the given fragment contains unclosable rings
   *  @since  2006.05.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean hasUnclosableRings (Fragment frag)
  {                             /* --- check for uncloseable rings */
    int   i, k, n;              /* loop variable, edge counter */
    Graph graph;                /* the fragment as a graph */
    Node  node;                 /* to traverse the nodes */

    graph = frag.getGraph();    /* get the fragment as a graph */
    for (i = frag.src; --i >= 0; ) {
      node = graph.nodes[i];    /* traverse the unextendable nodes */
      for (n = 0, k = node.deg; --k >= 0; )
        if (node.edges[k].isInRing()) n++;
      if (n == 1) return true;  /* if there is a single ring edge, */
    }                           /* a ring cannot be closed anymore, */
    return false;               /* else all rings may be closable */
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

    if (create)                 /* construct the graph's code word */
      this.makeWord(graph, graph.edgecnt);
    nmgr = graph.getNodeMgr();  /* get the node type manager */
    emgr = graph.getEdgeMgr();  /* and the edge type manager */
    k = this.word[0];           /* get and decode type of root node */
    if (graph.coder != null) k = graph.coder.decode(k);
    s = new StringBuffer(nmgr.getName(k));
    n = graph.edgecnt << 2;     /* get the number of characters */
    for (i = 0; i < n; ) {      /* traverse the characters */
      s.append('|');            /* separator for edges */
      s.append(this.word[++i]); /* source node index */
      s.append(' ');            /* separator to edge type */
      s.append(emgr.getName(this.word[++i]));
      s.append(' ');            /* separator to node type */
      k = this.word[++i];       /* get and decode the node type */
      if (graph.coder != null) k = graph.coder.decode(k);
      s.append(nmgr.getName(k));
      s.append(' ');            /* separator to node index */
      s.append(this.word[++i]); /* destination node index */
    }                           /* store the edge descriptions */
    return s.toString();        /* return created string description */
  }  /* describe() */

}  /* class CnFBreadth1 */
