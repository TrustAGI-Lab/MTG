/*----------------------------------------------------------------------
  File    : CanonicalForm.java
  Contents: Canonical forms for graphs and restricted extensions
  Author  : Christian Borgelt
  History : 2002.03.11 file created as file submol.java
            2002.04.02 function compareTo() added
            2003.02.19 extension by variable length chains added
            2003.08.01 first version of ring extensions added
            2003.08.05 ring flag processing optimized (field "all")
            2003.08.06 file split, this part renamed to Extension.java
            2003.08.07 complete rewrite of extension functions
            2005.06.09 chain edges required to be bridges
            2005.07.22 handling of incomplete rings and trimming added
            2005.07.24 orientation check added to ring extension
            2005.08.10 wrapper functions makeFragment added
            2005.08.11 made abstract, subclasses MaxSrcExt and RightExt
            2006.04.11 function compareEdge() added
            2006.05.02 parameters of function isCanonic() changed
            2006.05.03 functions makeCanonic() and makeWord() added
            2006.05.08 function makeCanonic() generalized and debugged
            2006.05.10 function describe added (code word printing)
            2006.05.11 second function isCanonic() added
            2006.05.12 function compareRing() added (part of compareTo)
            2006.05.18 field CanonicalForm.dst added, chain simplified
            2006.05.31 function isCanonic() extended, result changed
            2006.06.06 adapted to changed type of edge flags
            2006.06.07 function compareWord() added
            2006.07.01 adaptation of ring extensions moved here
            2006.07.06 generation of equivalent ring variants added
            2006.08.08 bug in definition of CanonicalForm.curr fixed
            2006.08.10 bug in function chain fixed (trimmed edges)
            2006.10.29 function setChainTypes() added, chain adapted
            2007.03.24 functions prepare() and removeRings() added
            2007.03.26 flag sym added (indicating local ring symmetry)
            2007.06.21 adapted to new class TypeMgr (type manager)
            2008.03.06 ring key test improved (simpler interface)
            2009.04.29 functions for setting mode and size added
            2009.05.05 ring flags of equivalent extensions are removed
            2009.05.07 function makeCanonic() and makeMap() improved
            2010.01.22 renamed to CanonicalForm.java
            2011.03.01 functions for node equivalence classes added
            2011.03.03 node orbits exploited with equivalence classes
            2011.03.04 flag ALLORBS added to avoid "instanceof" tests
----------------------------------------------------------------------*/
package moss;

import java.util.Comparator;
import java.util.Arrays;

/*--------------------------------------------------------------------*/
/** Class for canonical forms of graphs and their restricted extensions.
 *  <p>A canonical form object serves the purpose to define a canonical
 *  form of graphs and to create the corresponding restricted extensions
 *  of a fragment.</p>
 *  <p>The same extension object is reused to create extensions of
 *  several fragments instead of creating a new extension object for
 *  each fragment or even embedding. As a consequence the fragment and
 *  embedding to extend are not passed directly to a constructor, but
 *  to an initialization function. In addition, if embeddings are used,
 *  extended fragments are created in a delayed manner, recording only
 *  the extension edge at the beginning and turning it into a full
 *  fragment only on request (to avoid creating duplicates).</p>
 *  <p>The field <code>size</code>is used to indicate the type of the
 *  current extension. A negative size indicates a chain extension,
 *  with the absolute value of the size being the chain length.
 *  A zero size indicates a single edge extension (the standard case).
 *  Finally, a positive size indicates a ring extension, with the size
 *  being the number of nodes/edges in the ring.</p>
 *  @author Christian Borgelt
 *  @since  2002.03.11 */
/*--------------------------------------------------------------------*/
public abstract class CanonicalForm implements Cloneable{

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** extension mode flag: single edge */
  public    static final int EDGE    = 0x01;
  /** extension mode flag: ring (must be marked) */
  public    static final int RING    = 0x02;
  /** extension mode flag: variable length chain */
  public    static final int CHAIN   = 0x04;
  /** extension mode flag: equivalent ring variants */
  public    static final int EQVARS  = 0x08;
  /** extension mode flag: use node orbits to filter (if known) */
  public    static final int ORBITS  = 0x10;
  /** extension mode flag: generate all extensions */
  public    static final int ALLEXTS = 0x20;
  /** extension mode flag: use node equivalence classes */
  public    static final int CLASSES = 0x40;
  /** extension mode flag: use node orbits for all extensions;
   *  not only those leading to a new node */
  public    static final int ALLORBS = 0x80;
  /** flag for a fixed edge in the ring order test */
  protected static final int FIXED   = Integer.MIN_VALUE;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the extension mode (e.g. <code>EDGE</code>, <code>RING</code>)*/
  protected int       mode;
  /** the maximum fragment size (number of nodes) */
  protected int       max;
  /** the minimum ring size (number of nodes/edges) */
  protected int       rgmin;
  /** the maximum ring size (number of nodes/edges) */
  protected int       rgmax;
  /** the node type for chain extensions */
  protected int       cnode;
  /** the edge type for chain extensions */
  protected int       cedge;
  /** the extension edge manager (for extensions without embeddings) */
  protected ExtMgr    xemgr;
  /** the fragment  that is extended */
  protected Fragment  frag;
  /** the embedding that is extended (may be <code>null</code>) */
  protected Embedding emb;
  /** (relevant) nodes of the extension */
  protected Node[]    nodes;
  /** (relevant) edges of the extension */
  protected Edge[]    edges;
  /** the number of nodes in a ring (positive) or chain (negative) */
  protected int       size;
  /** the number of new nodes */
  protected int       nodecnt;
  /** the number of new edges */
  protected int       edgecnt;
  /** the number of variable length chains */
  protected int       chcnt;
  /** the index of the current source node */
  protected int       src;
  /** the current edge index in the source node */
  protected int       idx;
  /** the index of the current destination node */
  protected int       dst;
  /** the type of the extension edge (from <code>EDGE.type</code>) */
  protected int       type;
  /** all (remaining) ring flags of the current edge */
  protected long      all;
  /** the current ring flag */
  protected long      curr;
  /** whether the current ring is locally symmetric */
  protected boolean   sym;
  /** the minimal position/current position index of a ring edge */
  protected int       pmin;
  /** the maximal position/position index of a ring edge */
  protected int       pmax;
  /** the current position 1 of equivalent edges for ring extensions */
  protected int       pos1;
  /** the current position 2 of equivalent edges for ring extensions */
  protected int       pos2;
  /** the number of fixed edges in a canonical form test */
  protected int       fixed;
  /** the code word for isCanonic/makeCanonic */
  protected int[]     word;
  /** the node map for making a graph canonic */
  protected int[]     nmap;
  /** the edge map for making a graph canonic */
  protected int[]     emap;

  /*------------------------------------------------------------------*/
  /** Create a canonical form object.
   *  <p>Since <code>CanonicalForm</code> is an abstract class, this
   *  constructor cannot be called directly to create an instance.
   *  Rather it is meant as a common initialization routine for
   *  subclasses of this class.</p>
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public CanonicalForm ()
  {                             /* -- create a canonical form object */
    this.mode  = EDGE;          /* initialize the extension variables */
    this.max   = Integer.MAX_VALUE;
    this.rgmin = this.rgmax =  0;
    this.cnode = this.cedge = -1;
    this.xemgr = null;          /* there is no ext. edge manager */
    this.frag  = null;          /* there is no fragment yet */
    this.emb   = null;          /* and no embedding either */
    this.nodes = new Node[256]; /* init. the node/edge arrays with */
    this.edges = new Edge[256]; /* a large number of edges/nodes */
    this.word  = new int[1024]; /* init. the code word buffer */
    this.nmap  = new int [256]; /* as well as the node and */
    this.emap  = new int [256]; /* edge maps accordingly */
    this.pmin  = this.pos1 = -1;/* init. the variables for */
    this.pmax  = this.pos2 = -1;/* equivalent variants of rings */
  }  /* CanonicalForm() */

   
  /*------------------------------------------------------------------*/
  /** Set the extension mode.
   *  <p>The extension mode controls what extensions are created.
   *  By default only single edge extensions are created. Other modes
   *  include ring extensions and chain extensions.</p>
   *  @param  mode the extension mode
   *               (e.g. <code>EDGE</code> or <code>EDGE|RING</code>)
   *  @since  2009.04.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setExtMode (int mode)
  {                             /* --- set the extension mode */
    if ((mode & CLASSES) != 0)  /* if to use node equiv. classes, */
      mode |= ALLEXTS;          /* all extensions must be generated */
    if ((mode & RING)    == 0)  /* equivalent variants are needed */
      mode &= ~EQVARS;          /* only for ring extensions */
    this.mode = (this.mode & ALLORBS) | (mode & ~ALLORBS);
  }  /* setExtMode() */         /* initialize the extension mode */

  /*------------------------------------------------------------------*/
  /** Set the maximum fragment size (to limit extensions).
   *  <p>The fragment size is the number of nodes of a fragment.
   *  The maximum fragment size is the maximum number of nodes a
   *  fragment may have. No extended fragments will be created that
   *  contain more than this number of nodes.</p>
   *  @param  max the maximum fragment size (number of nodes)
   *  @since  2009.04.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setMaxSize (int max)
  { this.max = max; }

  /*------------------------------------------------------------------*/
  /** Set the ring sizes for ring extensions.
   *  <p>These ring sizes are actually not needed for creating ring
   *  extensions, but only for adapting them, which is needed only
   *  if canonical form pruning is used.</p>
   *  @param  rgmin the minimal ring size (number of nodes/edges)
   *  @param  rgmax the maximal ring size (number of nodes/edges)
   *  @since  2006.07.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setRingSizes (int rgmin, int rgmax)
  { this.rgmin = rgmin; this.rgmax = rgmax; }

  /*------------------------------------------------------------------*/
  /** Set the node and edge type for chain extensions.
   *  @param  node the type of the chain nodes
   *  @param  edge the type of the chain edges
   *  @since  2006.10.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setChainTypes (int node, int edge)
  { this.cnode = node; this.cedge = edge; }

  /*------------------------------------------------------------------*/
  /** Set the extension edge manager.
   *  @param  xemgr the extension edge manager
   *  @since  2010.01.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setExtMgr (ExtMgr xemgr)
  { this.xemgr = xemgr; }

  /*------------------------------------------------------------------*/
  /** Whether node orbits are to be used to filter extensions.
   *  <p>With the help of node orbits some equivalent siblings
   *  can be suppressed.</p>
   *  @return whether node orbits are to be used
   *  @since  2011.02.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean useOrbits ()
  { return ((this.mode & ORBITS) != 0)
    &&     ((this.frag == null) || this.frag.hasOrbits()); }

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
   *  @since  2003.08.06/2011.02.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean init (Fragment frag, Embedding emb)
  {                             /* --- initialize an extension */
    int  i, k;                  /* loop variables */
    Node n[];                   /* to access the nodes */

    if (((this.mode & CLASSES) != 0)
    &&  ((this.mode & ORBITS)  != 0)
    &&  (emb  != null)          /* if to use node equivalence classes */
    &&  (frag != this.frag)     /* and to filter with node orbits */
    &&  (frag.hasOrbits())      /* the extensions of embeddings */
    &&  (emb.edges.length > 0)){/* and a new fragment is given */
      k = frag.graph.nodecnt;   /* traverse the nodes of the graph */
      n = frag.graph.nodes;     /* and find orbit representatives */
      for (i = k; --i >= 0; ) this.nmap[n[i].orbit] = i;
      for (i = k; --i >= 0; ) n[i].orbit = this.nmap[n[i].orbit];
    }                           /* re-map orbits to representatives  */
    /* If node equivalence classes are used, the node orbits in the  */
    /* fragment's graph refer to the node positions in the canonical */
    /* code word for the equivalence classes. However, the graph of  */
    /* the fragment is not adapted to that node order. Therefore the */
    /* orbit identifiers need not refer to an orbit representative.  */
    /* The above loops remap the orbit identifiers to achieve this.  */
    this.frag = frag;           /* note the (possibly new) fragment */
    this.src  =  0;             /* start with the first node */
    this.idx  = -1;             /* init. index of the current edge */
    this.size = -1;             /* clear the ring size and flags */
    this.all  =  0;             /* (also extension type indicator) */
    this.pos1 = this.pos2 = -1; /* clear ring variant variables */
    this.pmin = this.pmax = -1; /* (make sure state is defined) */
    this.emb  = emb;            /* note the embedding to extend */
    if (emb != null) {          /* if an embedding is given */
      emb.index(); }            /* mark the embedding in its graph */
    else {                      /* if no embedding is given */
      frag.graph.index();       /* index nodes of the fragment graph */
      this.dst = frag.graph.nodecnt;
      this.src--;               /* init. to last extension of node */
    }                           /* before the current source node */
    return true;                /* return that there may be exts. */
  }  /* init() */

  /*------------------------------------------------------------------*/
  /** Initialize the extension generation process.
   *  <p>This function initializes the extension process without
   *  embeddings, that is, based only on internally stored possible
   *  extension edges. This function is equivalent to
   *  <code>initFrag(Fragment)</code>.</p>
   *  @see    #initFrag(Fragment)
   *  @param  frag the fragment to extend
   *  @since  2010.01.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean init (Fragment frag)
  { return this.init(frag, null); }

  /*------------------------------------------------------------------*/
  /** Initialize the extension generation process.
   *  <p>This function initializes the extension process without
   *  embeddings, that is, based only on internally stored possible
   *  extension edges. This function is equivalent to
   *  <code>init(Fragment)</code>.</p>
   *  @see    #init(Fragment)
   *  @param  frag the fragment to extend
   *  @since  2011.02.18 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean initFrag (Fragment frag)
  { return this.init(frag, null); }

  /*------------------------------------------------------------------*/
  /** Create the next (restricted) extension of an embedding.
   *  <p>Each time this function is called and returns
   *  <code>true</code>, a new (restricted) extension has been created.
   *  This extension may then be compared to already existing fragments
   *  (function <code>compareTo()</code>) or turned into a new fragment
   *  (function <code>makeFragment()</code>) or a new embedding
   *  (function <code>makeEmbedding()</code>).
   *  When all (restricted) extensions of the embedding passed to
   *  <code>init(Fragment,Embedding)</code> have been created,
   *  the function returns <code>false</code>.</p>
   *  @return whether another extension was created
   *  @since  2003.08.06/2011.02.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean next ()
  {                             /* --- create the next extension */
    Node s, d, o[], b[], n[];   /* to traverse/access the nodes */
    Edge e;                     /* to traverse/access the edges */

    if ((this.mode & EDGE) == 0)/* if no edge extension are allowed, */
      return false;             /* no extensions can be generated */
    this.chcnt = this.frag.chcnt;       /* copy the chain counter */
    o = (this.useOrbits()) ? this.frag.graph.nodes : null;
    b = ((this.mode & ALLORBS) != 0) ? o : null;
    n = this.emb.nodes;         /* get the nodes of the embedding */
    s = n[this.src];            /* and the current source node */
    while (true) {              /* find the next unprocessed edge */
      while (++this.idx >= s.deg) {
        do {                    /* if node's last edge is processed */
          if (++this.src >= n.length) {
            this.emb.mark(-1);  /* go to the next extendable node */
            return false;       /* and if there is none, abort */
          }                     /* (no more extensions to create) */
        } while ((b != null)    /* check for an orbit representative */
        &&       (b[this.src].orbit != this.src));
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
        if ((o != null) && (o[this.src].orbit != this.src))
          continue;             /* check for an orbit representative */
        /* For edges that add a node source nodes that are not orbit */
        /* representatives are skipped, because extending them only  */
        /* creates equivalent siblings that are not canonical.       */
        this.dst = n.length; }  /* assign the next free node index */
      else {                    /* if the new edge closes a ring */
        if (d.mark <= this.src) /* skip edges that lead "backward" */
          continue;             /* in the fragment (not canonical) */
        this.dst = d.mark;      /* note the destination node index */
      }                         /* (node is already in the fragment) */
      break;                    /* an extension has been found, so */
    }                           /* abort the extension search loop */
    this.edges[0] = e;          /* note the (first) extension edge */
    this.nodes[0] = s;          /* and  the source node */
    this.nodes[1] = d;          /* and  the destination node */
    this.nodecnt  = (d.mark < 0) ? 1 : 0;
    this.edgecnt  = 1;          /* zero/one new node, one new edge */
    this.size     = 0;          /* clear the extension size (edge) */
    return true;                /* return "extension successful" */
  }  /* next() */

  /*------------------------------------------------------------------*/
  /** Create the next (restricted) extension of a fragment.
   *  <p>Each call creates a new extended fragment or returns
   *  <code>null</code>. This function works without embeddings,
   *  (initialization: functions <code>init(Fragment)</code> or
   *  <code>initFrag(Fragment)</code>), but rather draws an a
   *  stored list of extension edges.</p>
   *  @return the next extended fragment or <code>null</code>.
   *  @since  2010.01.21/2011.02.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Fragment nextFrag ()
  {                             /* --- create next extended fragment */
    Graph   g;                  /* graph of the fragment */
    Node    s, d;               /* to traverse/access the nodes */
    Edge    e;                  /* to traverse/access the edges */
    int     t, i;               /* ext. edge type, loop variable */
    boolean o, b;               /* whether node orbits are used */

    this.chcnt = this.frag.chcnt;      /* copy the chain counter */
    g = this.frag.graph;        /* get the graph of the fragment */
    o = this.useOrbits();       /* and whether to use node orbits */
    b = ((this.mode & ALLORBS) != 0) ? o : false;
    while (true) {              /* try to find another extension */
      ++this.dst;               /* get the next destination index */
      while (this.dst > g.nodecnt) {     /* if there is none left */
        t = this.xemgr.next();  /* get the next extension edge type */
        while (t < 0) {         /* if there is none left */
          if (++this.src >= g.nodecnt) /* get the next source index */
            return null;        /* and if there is none left, abort */
          s = g.nodes[this.src];/* check for an orbit representative */
          if (b && (s.orbit != this.src)) continue;           
          /* Source nodes that are not orbit representatives can be  */
          /* skipped, because extending them only creates equivalent */
          /* siblings that are not canonical and will be discarded.  */
          t = this.xemgr.first(s.type);
        }                       /* get the first extension edge */
        if (g.nodes[this.src].deg >= this.xemgr.getDegree())
          continue;             /* skip if source has too many edges */
        this.dst = this.src+1;  /* get the new destination index */
      }                         /* (is checked in while condition) */
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
    return new Fragment(this.frag, this.src, this.dst,
                        this.xemgr.getType(),
                        this.xemgr.getDest());
  }  /* nextFrag() */           /* create an extended fragment */

  /*------------------------------------------------------------------*/
  /** Create a ring extension.
   *  <p>Follow a ring flag through the edges of the graph the
   *  embedding to extend refers to and collect the new edges for
   *  the extension. All created rings are checked with the function
   *  <code>validRing()</code>, restricting certain rings to a specific
   *  form (thus avoiding some unnecessary canonical form tests).
   *  If no (further) ring can be created, the function returns
   *  <code>false</code>, otherwise <code>true</code>.</p>
   *  @return whether another ring extension was created
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean ring ()
  {                             /* --- create a ring extension */
    int  i;                     /* loop variable */
    Node s, d;                  /* to traverse the nodes of the ring */
    Edge r, e = null;           /* to traverse the edges of the ring */
    long x;                     /* ring flags common to new edges */

    s = this.nodes[0];          /* get the current source node  */
    while (this.all != 0) {     /* while there is another ring flag */
      while ((this.all & this.curr) == 0)
        this.curr <<= 1;        /* find the next ring flag */
      x = this.all &= ~this.curr;         /* and remove it */
      this.size    = 1;         /* initialize the ring size */
      this.edgecnt = 1;         /* and the counters for */
      this.nodecnt = 0;         /* the new edges and nodes */
      r = this.edges[0];        /* get the first  ring edge */
      d = this.nodes[1];        /* and the second ring node */
      do {                      /* traverse the ring */
        for (i = d.deg; --i >= 0; ) {
          e = d.edges[i];       /* traverse the edges of the node */
          if ((e != r) && ((e.flags & this.curr) != 0))
            break;              /* find the next edge */
        }                       /* of the ring to be added */
        if ((i < 0) || (e.mark < -1))
          break;                /* if the ring is incomplete, abort */
        /* If e.mark < -1, the edge has been removed from the */
        /* graph by trimming and thus cannot be followed.     */
        this.nodes[this.size  ] = d;        /* collect the nodes */
        if (d.mark < 0) { this.nodecnt++; } /* and count new ones */
        this.edges[this.size++] = e;        /* collect the edges */
        if (e.mark < 0) { this.edgecnt++;   /* and count new ones */
          x &= e.flags; }       /* collect the common ring flags */
        r = e;                  /* go to the next edge and node */
        d = (e.src != d) ? e.src : e.dst;
      } while (d != s);         /* while the ring is not closed */
      if (d != s) continue;     /* check whether the ring was closed */
      if (x != 0)               /* if there are other ring flags, */
        this.removeEquiv(x);    /* remove the equivalent ones */
      if (this.emb.nodes.length +this.chcnt +this.nodecnt > this.max)
        continue;               /* check the size of the fragment */
      if (!this.validRing())    /* check the structure of the ring */
        continue;               /* and abort if it is not valid */
      if ((this.mode & EQVARS) != 0)
        this.initVars();        /* init. equivalent variants */
      return true;              /* of the current ring and */
    }                           /* return "ring extension succeeded" */
    return false;               /* return "ring extension failed" */
  }  /* ring() */

  /*------------------------------------------------------------------*/
  /** Remove ring flags that yield an equivalent extension.
   *  <p>If rings are nested, situations can occur in which the same
   *  set of extension edges results from two different rings, namely
   *  if the two rings differ only in edges that are already in the
   *  fragment that is extended. In this case an extension is created
   *  only from the lowest ring flag that yields this extension.
   *  All other ring flags are removed from <code>this.all</code>.</p>
   *  @param  flags the ring flags common to the extension edges
   *  @return the lowest ring flag yielding the current extension
   *  @since  2009.05.05 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void removeEquiv (long flags)
  {                             /* --- remove equivalent ring flags */
    int  i, n;                  /* loop variable, edge counter */
    Node s, d;                  /* to traverse the nodes of the ring */
    Edge r, e = null;           /* to traverse the edges of the ring */
    long f = this.curr << 1;    /* current ring flag */

    s = this.nodes[0];          /* get the source node */
    while (flags != 0) {        /* ring flag test loop */
      while ((flags & f) == 0)  /* find the next ring flag that is */
        f <<= 1;                /* common to the extension edges and */
      flags &= ~f;              /* remove it (it is processed now) */
      n = 1;                    /* init. the number of new edges */
      r = this.edges[0];        /* get the first  ring edge */
      d = this.nodes[1];        /* and the second ring node */
      do {                      /* traverse the ring */
        for (i = d.deg; --i >= 0; ) {
          e = d.edges[i];       /* traverse the edges of the node */
          if ((e != r) && ((e.flags & f) != 0))
            break;              /* find the next edge */
        }                       /* of the ring to be added */
        if ((i < 0) || (e.mark < -1))
          break;                /* if the ring is incomplete, abort */
        /* If e.mark < -1, the edge has been removed from the */
        /* graph by trimming and thus cannot be followed.     */
        if (e.mark < 0) n++;    /* count the new edges */
        r = e;                  /* go to the next edge and node */
        d = (e.src != d) ? e.src : e.dst;
      } while (d != s);         /* while the ring is not closed */
      if ((d == s) && (n == this.edgecnt))
        this.all &= ~f;         /* if same extension edges result, */
    }                           /* remove the ring flag */
  }  /* removeEquiv() */

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

  protected abstract boolean validRing ();

  /*------------------------------------------------------------------*/
  /** Initialize the generation of equivalent ring extension variants.
   *  <p>If a ring start (and possibly also ends) with an edge that is
   *  equivalent to one or more edges already in the fragment (that is,
   *  edges that start at the same node, have the same type, and lead
   *  to nodes of the same type), these edges must be spliced with the
   *  already existing equivalent edges in the fragment. All possible
   *  ways of splicing the equivalent edges have to be tried. This
   *  function initializes this variant generation.</p>
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract void initVars ();

  /*------------------------------------------------------------------*/
  /** Create the next ring extension variant.
   *  @return whether another ring variant was created
   *  @see    #initVars()
   *  @since  2006.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract boolean variant ();

  /*------------------------------------------------------------------*/
  /** Reorder the edges of a fragment with a ring extension.
   *  <p>After a ring extension it may be necessary to reorder the edges
   *  of the resulting fragment, so that the edges get into the proper
   *  order w.r.t. the canonical form. In addition, it must be checked
   *  whether rings were added in the right order (if several rings
   *  were added). If not, the ring extension cannot be adapted and
   *  thus the function returns -1.</p>
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

  protected abstract int adaptRing (Fragment frag, boolean check);

  /*------------------------------------------------------------------*/
  /** Compare two edges with the precedence order of the canonical form.
   *  <p>A canonical form usually allows to compare two edges in the
   *  necessary way by fixing a specific precedence order of the
   *  defining properties of the edges.</p>
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
   *  @return whether the first edge is smaller (-1) or greater than
   *          (+1) or equal to (0) the second edge
   *  @since  2006.04.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract int compareEdge (Edge e1, Edge e2, int next);

  /*------------------------------------------------------------------*/
  /** Remove the flags of all rings an edge is contained in.
   *  @param  edge the edge to process
   *  @since  2007.03.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected static void removeRings (Edge edge)
  {                             /* --- remove ring flags */
    int  i;                     /* loop variable */
    long rgs, cur;              /* ring flags */
    Node s, a;                  /* to traverse the ring nodes */
    Edge r, x = null;           /* to traverse the ring edges */

    rgs = edge.getRings();      /* traverse the ring flags */
    for (cur = 1; rgs != 0; cur <<= 1) {
      if ((rgs & cur) == 0) continue;
      rgs &= ~cur;              /* remove the processed ring flag */
      r = edge; s = r.src; a = r.dst;
      do {                      /* loop to collect the ring edges */
        for (i = a.deg; --i >= 0; ) {
          x = a.edges[i];       /* traverse the incident edges */
          if ((x != r) && ((x.flags & cur) != 0)) break;
        }                       /* find the next ring edge and */
        r = x; r.flags &= ~cur; /* remove the current ring flag */
        a = (r.src != a) ? r.src : r.dst;
      } while (a != s);         /* get the next ring node */
    }                           /* until the ring is closed */
  }  /* removeRings() */

  /*------------------------------------------------------------------*/
  /** Prepare the rings of a fragment for adaptation and order test.
   *  @param  frag the fragment to prepare
   *  @return the fragment as a graph
   *  @since  2007.03.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Graph prepare (Fragment frag)
  {                             /* --- prepare for ring order test */
    int   i;                    /* loop variable */
    Graph graph;                /* the fragment as a graph */
    Edge  e;                    /* to traverse the edges */

    graph = frag.getGraph();    /* mark rings in the fragment */
    graph.markRings(0, this.rgmax, 0);
    for (i = graph.edgecnt; --i >= 0; ) {
      e = graph.edges[i];       /* traverse the fragment's edges */
      if (!e.isInRing() && (e.getRings() != 0))
        CanonicalForm.removeRings(e);
    }                           /* remove superfluous ring flags */
    graph.prepare();            /* prepare graph for processing */
    this.initCanonic(graph, 0); /* init. the canonical form */
    return graph;               /* return the graph that */
  }  /* prepare() */            /* represents the fragment */

  /*------------------------------------------------------------------*/
  /** Internal recursive function to check whether an edge is removable.
   *  <p>If the given edge (which must be a ring edge) is removable,
   *  then the rings it is contained in can be added later than the
   *  rings of the last edges that have been added. As a consequence
   *  the rings have not been added in the correct order and the last
   *  ring extension is invalid.</p>
   *  @param  edge the edge to check
   *  @return whether the edge is removable
   *  @since  2006.07.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private static boolean isRemovable (Edge edge)
  {                             /* --- check for a removable edge */
    int  i;                     /* loop variable */
    long rgs, cur;              /* ring flags */
    Node s, a;                  /* to traverse the ring nodes */
    Edge r, x = null;           /* to traverse the ring edges */

    rgs = edge.getRings();      /* traverse the ring flags */
    for (cur = 1; rgs != 0; cur <<= 1) {
      if ((rgs & cur) == 0) continue;
      rgs &= ~cur;              /* remove the processed ring flag */
      r = edge; s = r.src; a = r.dst;
      do {                      /* loop to collect the ring edges */
        for (i = a.deg; --i >= 0; ) {
          x = a.edges[i];       /* traverse the incident edges */
          if ((x != r) && ((x.flags & cur) != 0)) break;
        }                       /* find the next ring edge and */
        r = x;                  /* remove the current ring flag */
        if (--r.mark == FIXED)  /* if all ring flags are removed */
          return false;         /* from a fixed edge, abort */
        a = (r.src != a) ? r.src : r.dst;
      } while (a != s);         /* get the next ring node */
    }                           /* until the ring is closed */
    return true;                /* return 'edge is removable' */
  }  /* isRemovable() */

  /*------------------------------------------------------------------*/
  /** Check whether a prefix is a ring key.
   *  <p>This function presupposes that the internal edge buffer
   *  contains the graph's edges in adapted order.</p>
   *  @param  graph the graph to check
   *  @param  edge  the edge at the end of the prefix to check
   *  @since  2007.03.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isRingKey (Graph graph, Edge edge)
  {                             /* --- check prefix for a ring key */
    int  i, k, n;               /* loop variables, number of edges */
    long rgs, cur;              /* ring flags */

    n = graph.edgecnt;          /* mark the fixed edges */
    for (k = n; this.edges[--k] != edge; ) this.edges[k].mark = 0;
    for (i = k+1; --i >= 0; )              this.edges[i].mark = FIXED;
    for (i = n;   --i >= 0; ) { /* traverse all edges, */
      edge = this.edges[i];     /* get their ring flags, and */
      rgs  = edge.getRings();   /* mark non-ring edges as fixed */
      if (rgs == 0) { this.emap[i] = edge.mark = FIXED; continue; }
      for (cur = 1; rgs != 0; cur <<= 1)
        if ((rgs & cur) != 0) { rgs &= ~cur; edge.mark++; }
      this.emap[i] = edge.mark; /* count the number of ring flags */
    }                           /* and buffer the edge markers */
    for (i = k; i < n; i++) {   /* traverse the non-fixed edges */
      edge = this.edges[i];     /* (ring edges in volatile part) */
      if ((edge.mark & FIXED) != 0) continue;
      for (k = n; --k >= 0; )   /* restore the edge markers */
        this.edges[k].mark = this.emap[k];
      if (CanonicalForm.isRemovable(edge))
        return false;           /* if an edge can be removed, then */
    }                           /* rings were added in a wrong order */
    return true;                /* otherwise the order is valid */
  }  /* isRingKey() */

  /*------------------------------------------------------------------*/
  /** Create a variable length chain extension.
   *  <p>A variable length chain consists of nodes of the same type
   *  that are connected by edges of the same type. There must not
   *  be any branches. This function is called when the function
   *  <code>next()</code> detects a possible start of a chain.
   *  However, the check in <code>next()</code> is limited and thus
   *  it may be that no variable length chain can be created. In this
   *  case this function returns <code>false</code>.</p>
   *  @return whether a chain extension was created
   *  @since  2003.02.19 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean chain ()
  {                             /* --- create a chain extension */
    Node node;                  /* to traverse the nodes of the chain */
    Edge edge;                  /* to traverse the edges of the chain */

    edge = this.edges[0];       /* get the starting edge */
    if ((edge.type != this.cedge) || !edge.isBridge())
      return false;             /* first edge must be single, bridge */
    node = this.nodes[1];       /* get dest. node (first in chain) */
    while ((node.deg  == 2)     /* and traverse the chain */
    &&     (node.type == this.cnode)) {
      edge = node.edges[(node.edges[0] != edge) ? 0 : 1];
      if (edge.mark < -1) return false;
      if ((edge.type != this.cedge) || !edge.isBridge())
        break;                  /* edge must be single and a bridge */
      node = (node != edge.src) ? edge.src : edge.dst;
      this.size--;              /* go to the next edge and node and */
    }                           /* increase the chain length */
    if (this.size >= 0) {       /* if no extension chain was found, */
      this.size = -1; return false; }         /* abort the function */
    this.edges[1] = edge;       /* note the last edge of the chain */
    this.edgecnt  = 2;          /* there are always two new edges */
    this.nodes[1] = node;       /* note the node and its index */
    this.nodecnt  = 1;          /* and set the new node counter */
    this.dst      = this.emb.nodes.length;
    this.chcnt++;               /* increment the chain counter */
    return (this.emb.nodes.length +this.chcnt +1 <= this.max);
  }  /* chain() */              /* return extension success */

  /*------------------------------------------------------------------*/
  /** Compare the current extension to a given fragment.
   *  <p>This function is used to determine whether the current
   *  extension is equivalent to a previously created one (and thus
   *  only an embedding has to be created from it, which is then added
   *  to the corresponding fragment) or not (and thus a new fragment
   *  has to be created). It is designed as a comparison function,
   *  because the created fragments are kept as an ordered array,
   *  so that a binary search becomes possible.</p>
   *  @param  frag the fragment to compare to
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the fragment described by this extension is less
   *          than, equal to, or greater than the fragment given
   *          as an argument
   *  @since  2002.04.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract int compareToFrag (Fragment frag);

  /*------------------------------------------------------------------*/
  /** Compare the current ring extension to a fragment.
   *  <p>This is a sub-function of the function <code>compareTo</code>,
   *  which compares the current extension to a given fragment whatever
   *  the type of the extension may be. If both the current extension
   *  and the given fragment describe a ring extension, this function
   *  is called to compare them.</p>
   *  <p>This function assumes that the first edge of the ring together
   *  with its destination node have already been compared (namely in
   *  the function <code>compareTo</code>) and thus only compares the
   *  rest of the new ring edges.</p>
   *  @param  frag the fragment to compare to
   *  @return whether the current extension is smaller (-1) or greater
   *          than (+1) or equal to (0) the given fragment
   *  @since  2006.05.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareRing (Fragment frag)
  {                             /* --- compare a ring extension */
    int  i, k, n, e;            /* loop variable, buffers */
    int  t1, t2;                /* buffers for comparison */
    Edge edge, x;               /* to traverse the edges */
    Node node, y;               /* to traverse the nodes */

    e = frag.base.list.edges.length;
    i =      frag.list.edges.length -e;
    if (this.edgecnt < i) return -1;  /* compare the number */
    if (this.edgecnt > i) return +1;  /* of added edges */
    n = frag.base.list.nodes.length;
    i =      frag.list.nodes.length -n;
    if (this.nodecnt < i) return -1;  /* compare the number */
    if (this.nodecnt > i) return +1;  /* of added nodes */
    /* Note that the ring sizes must not be compared, because it is  */
    /* possible that exactly the same extension (in terms of the set */
    /* of new edges and nodes) results from rings of different size, */
    /* simply because the differing part is already in the fragment. */
    for (i = k = 0; ++i < this.size; ) {
      x = this.edges[i];        /* traverse the remaining edges */
      if (x.mark >= 0) continue;/* skip already contained edges */
      node = this.nodes[i];     /* get the source node */
      t1 = (node.mark >= 0) ? node.mark : n++;
      t2 = frag.ris[k++];       /* get/compute source node indices */
      if (t1 < t2) return -1;   /* compare the indices */
      if (t1 > t2) return +1;   /* of the source nodes */
      y = frag.list.nodes[t2];  /* and then their types */
      if (node.type < y.type) return -1;
      if (node.type > y.type) return +1;
      edge = frag.list.edges[++e]; /* get the corresponding edge */
      if (edge.type > x.type) return -1;  /* compare the types */
      if (edge.type < x.type) return +1;  /* of the added edges */
      node = this.nodes[(i+1) % this.size];
      t1 = (node.mark >= 0) ? node.mark : n;
      t2 = frag.ris[k++];       /* get/compute dest. node indices */
      if (t1 < t2) return -1;   /* compare the indices */
      if (t1 > t2) return +1;   /* of the destination nodes */
      y = frag.list.nodes[t2];  /* and then their types */
      if (node.type < y.type) return -1;
      if (node.type > y.type) return +1;
    }                           /* (compare all added edges) */
    i = frag.ris[k++];          /* compare first insertion position */
    if (this.pos1 < i) return -1;
    if (this.pos1 > i) return +1;
    i = frag.ris[k++];          /* compare second insertion position */
    if (this.pos2 < i) return -1;
    if (this.pos2 > i) return +1;
    return 0;                   /* otherwise the fragments are equal */
  }  /* compareRing() */

  /*------------------------------------------------------------------*/
  /** Check whether a fragment contains unclosable rings.
   *  <p>If the output is restricted to fragments containing only closed
   *  rings, the restricted extensions (as they can be derived from a
   *  canonical form) render certain nodes unextendable. If such a node
   *  has only one incident ring edge, the ring of which this edge is
   *  part cannot be closed by future extensions. Hence neither this
   *  fragment nor any of its extensions can produce output and thus
   *  it can be pruned.</p>
   *  @param  frag the fragment to check for unclosable rings
   *  @return whether the given fragment contains unclosable rings
   *  @since  2006.05.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract boolean hasUnclosableRings (Fragment frag);

  /*------------------------------------------------------------------*/
  /** Create a fragment from the current extension.
   *  <p>This function is called when the current extension is not
   *  equal to an already existing fragment and thus a new fragment
   *  has to be created.</p>
   *  @return the current extension as a fragment
   *  @since  2005.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Fragment makeFragment ()
  { return new Fragment(this); }

  /*------------------------------------------------------------------*/
  /** Create an embedding from the current extension.
   *  <p>This function is called when the current extension is equal
   *  to an already existing fragment and thus only a new embedding
   *  has to be added to that fragment.</p>
   *  @return the current extension as an embedding
   *  @since  2006.10.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Embedding makeEmbedding ()
  { return new Embedding(this); }

  /*------------------------------------------------------------------*/
  /** Initialize a canonical form test or generation.
   *  <p>For a canonical form test or for the procedure that makes a
   *  graph canonical, the internal arrays have to have a certain
   *  size (depending on the size of the graph, that is, the number
   *  of its nodes and edges), so that they can hold the necessary data.
   *  This function ensures proper array sizes and also initializes some
   *  variables.</p>
   *  @param  graph the graph to make canonic or to check
   *  @param  fixed the number of fixed (immovable) edges
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void initCanonic (Graph graph, int fixed)
  {                             /* --- initialize for canonical form */
    int k;                      /* number of nodes/edges/characters */

    k = graph.nodecnt;          /* enlarge node arrays if necessary */
    if (k > this.nodes.length) {
      this.nodes = new Node[k]; this.nmap = new int[k]; }
    while (--k >= 0)            /* init. the orbit representatives */
      graph.nodes[k].orbit = k; /* (every node represents itself) */
    k = graph.edgecnt;          /* enlarge edge arrays if necessary */
    if (k > this.edges.length) {
      this.edges = new Edge[k]; this.emap = new int[k]; }
    this.size  = k;             /* note the number of edges */
    this.fixed = fixed;         /* and  the number of fixed edges */
    k = (k << 2) +2;            /* enlarge code word if necessary */
    if (k > this.word.length) { this.word = new int[k]; }
    this.word[k-1] = 0;         /* place a sentinel in the code word */
  }  /* initCanonic() */

  /*------------------------------------------------------------------*/
  /** Create the code word for a given graph.
   *  <p>The code word is created for the current order of the edges
   *  as it is found in the graph. As a consequence the resulting
   *  code word may or may not be the canonical code word. If the
   *  canonical code word is desired, the graph has to be made
   *  canonic by calling the function <code>makeCanonic()</code>.</p>
   *  @param  graph the graph for which to create the code word
   *  @return the number of generated "characters" (array entries)
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int makeWord (Graph graph)
  { return this.makeWord(graph, graph.edgecnt); }

  /*------------------------------------------------------------------*/
  /** Create the code word for the first edges of a given graph.
   *  <p>In other words, this function creates the prefix of the
   *  code word for the given graph, using only the first edges.
   *  If, however, <code>edgecnt == graph.edgecnt</code>, all edges
   *  are used and thus a full code word is created.</p>
   *  <p>The code word is created for the current order of the edges
   *  as it is found in the graph. As a consequence the resulting
   *  code word may or may not be the canonical code word. If the
   *  canonical code word is desired, the graph has to be made
   *  canonic by calling the function <code>makeCanonic()</code>.</p>
   *  @param  graph   the graph for which to create the code word
   *  @param  edgecnt the number of edges to consider
   *  @return the number of generated "characters" (array entries)
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int makeWord (Graph graph, int edgecnt)
  {                             /* --- construct (part of) code word */
    int n = (edgecnt << 2) +1;  /* compute number of characters */ 
    if (n > this.word.length)   /* if the word gets too long, */
      this.word = new int[n];   /* create a new buffer */
    for (int i = graph.nodecnt; --i >= 0; )
      graph.nodes[i].mark = i;  /* number the nodes of the graph */
    if (edgecnt <= 0) {         /* if there are no edges, abort */
      this.word[0] = graph.nodes[0].type; return 1; }
    this.makeWord(graph.edges, edgecnt);
    return n;                   /* return the number of characters */
  }  /* makeWord() */

  /*------------------------------------------------------------------*/
  /** Create the (prefix of a) code word for a given edge array.
   *  @param  edges the array of edges for which to create the code word
   *  @param  n     the number of edges to consider
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract void makeWord (Edge[] edges, int n);

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given graph.
   *  <p>This function assumes that <code>makeWord()</code> has been
   *  called before (for some other graph or a different form of the
   *  same graph) and has placed a code word into the internal code
   *  word buffer. This code word is then compared to the code word
   *  that would be created for the given graph (without explicitely
   *  generating the code word for the graph).</p>
   *  @param  graph the graph to compare to
   *  @return whether the internal code word is smaller (-1) or greater
   *          than (+1) or equal to (0) the code word of the graph
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareWord (Graph graph)
  { return this.compareWord(graph, graph.edgecnt); }

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given graph.
   *  <p>The comparison takes only the first <code>edgecnt</code>
   *  edges into account. Any remaining edges are not compared.
   *  If, however, <code>edgecnt == graph.edgecnt</code>, the full
   *  code words are compared.</p>
   *  <p>This function assumes that <code>makeWord()</code> has been
   *  called before and has placed a code word into the internal code
   *  word buffer. This code word is then compared to the code word
   *  that would be created for the given graph (without explicitely
   *  generating the code word for the graph).</p>
   *  @param  graph   the graph to compare to
   *  @param  edgecnt the number of edges to consider
   *  @return whether the internal code word is smaller (-1) or greater
   *          than (+1) or equal to (0) the code word of the graph
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int compareWord (Graph graph, int edgecnt)
  {                             /* --- compare graph to code word */
    for (int i = graph.nodecnt; --i >= 0; )
      graph.nodes[i].mark = i;  /* number the nodes of the graph */
    return this.compareWord(graph.edges, edgecnt);
  }  /* compareWord() */        /* compare the edges of the graph */

  /*------------------------------------------------------------------*/
  /** Compare the current code word to the one of the given edge array.
   *  @param  edges the array of edges to compare to
   *  @param  n     the number of edges to consider
   *  @return whether the internal code word is smaller (-1) or greater
   *          than (+1) or equal to (0) the code word of the edges array
   *  @since  2006.06.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected abstract int compareWord (Edge[] edges, int n);

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

  protected abstract int isCanonic (int ei, int ni, int cnt);

  /*------------------------------------------------------------------*/
  /** Check whether a given graph is canonic.
   *  <p>In addition, if the graph is not canonic, it is determined
   *  whether the canonical form differs from the form of the graph
   *  within the first <code>fixed</code> edges. Hence there are three
   *  possible outcomes: (1) the graph is in canonical form (return
   *  value 1), (2) the graph differs from the canonical form in the
   *  first <code>fixed</code> edges (return value -1), (3) the graph
   *  is not in canonical form, but does not differ in the first
   *  <code>fixed</code> edges (return value 0).</p>
   *  @param  graph the graph to check for canonical form
   *  @param  fixed the number of fixed edges
   *  @return -1, if the graph differs from the canonical form
   *              in the first <code>fixed</code> edges,<br>
   *           0, if the graph is not canonical, but does not
   *              differ from the canonical form in the first
   *              <code>fixed</code> edges (but only in some
   *              later edge description),<br>
   *           1, if the graph is canonical.
   *  @since  2005.08.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int isCanonic (Graph graph, int fixed)
  {                             /* --- check for canonical form */
    int  i, k, r, t;            /* loop variable, buffers */
    Node node;                  /* to traverse the nodes */

    if (graph.edgecnt <= 0)     /* if there are no edges, */
      return 1;                 /* the graph's word is minimal */
    if ((this.mode & CLASSES) != 0)
      return (this.isCanExt(graph)) ? 1 : -1;
    this.initCanonic(graph, fixed); /* build initial code word */
    this.makeWord(graph, graph.edgecnt);
    graph.prepare();            /* prepare graph for the test */
    r = graph.edgecnt;          /* set the default result */
    k = graph.nodes[0].type;    /* get the root node's type */
    for (i = graph.nodecnt; --i >= 0; ) {
      node = graph.nodes[i];    /* traverse the nodes of the fragment */
      if (node.type > k) continue;   /* check one letter prefix words */
      if (node.type < k)        /* (words are e.g. A (I_s B A I_d)* ) */ 
        return (fixed >= 0) ? -1 : 0;
      this.nodes[node.mark = 0] = node; /* mark and note the root, */
      t = this.isCanonic(0,0,1);/* check prefix words recursively, */
      node.mark = -1;           /* then unmark the root node again */
      if (t < r) { if (t < fixed) return -1; r = t; }
    }                           /* evaluate the recursion result */
    return (r < graph.edgecnt) ? 0 : 1;
  }  /* isCanonic() */          /* return the overall result */

  /*------------------------------------------------------------------*/
  /** Check whether a given graph is canonic.
   *  @param  graph the graph to check for canonical form
   *  @return whether the given graph is canonic
   *  @since  2009.05.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isCanonic (Graph graph)
  { return this.isCanonic(graph, graph.edgecnt) >= 0; }

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

  protected abstract boolean makeCanonic (int ei, int ni, int cnt);

  /*------------------------------------------------------------------*/
  /** Make a given graph canonic.
   *  <p>The form of the graph (that is, the order of its nodes
   *  and edges) is changed in such a way that it produces the
   *  lexicographically smallest code word. The first <code>keep</code>
   *  edges are left unchanged. If <code>keep = 0</code>, then all
   *  edges may change their positions, but the first node is kept.
   *  Only if <code>keep = -1</code> the graph may be completely
   *  reorganized.</p>
   *  <p>This function does not actually reorganize the graph, but
   *  only stores the found canonical order of the edges and nodes in
   *  internal arrays. In addition, it creates maps for reorganizing
   *  the nodes and edges, also in internal buffers. Either of these
   *  may later be used to actually reorganize the graph as well
   *  as any embeddings (if the graph represents a fragment). Note
   *  that these arrays and maps are not filled/created if the graph
   *  is already in canonical form. In this case the function returns
   *  <code>false</code>, thus indicating that no reorganization is
   *  necessary.</p>
   *  @param  graph the graph to make canonic
   *  @param  keep  the number of edges to keep
   *  @return whether the graphs needs to be changed
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean makeCanonic (Graph graph, int keep)
  {                             /* --- turn graph into canonical form */
    int  i, n;                  /* loop variables, buffers */
    Node node, root;            /* to traverse the nodes, new root */
    Edge edge;                  /* to traverse the edges */

    n = (keep >= 0) ? keep : 0; /* get number of edges to keep */
    if (graph.edgecnt <= n)     /* if no edges are moveable, */
      return false;             /* the graph's word is minimal */
    this.initCanonic(graph, 0); /* init. the canonical form fields */
    if (keep < 0) {             /* if full freedom of reordering */
      graph.prepare();          /* prepare graph for processing */
      root = graph.nodes[0];    /* get current root node as default */
      for (i = 0; i < graph.nodecnt; i++) {
        node = graph.nodes[i];  /* traverse the nodes of the graph */
        if (node.type < root.type) root = node;
      }                         /* find node with smallest type */
      this.word[0] = root.type; /* set the initial character */
      this.word[1] = Integer.MAX_VALUE;
      for (i = graph.nodecnt; --i >= 0; ) {
        node = graph.nodes[i];  /* traverse the graph's nodes */
        if (node.type != root.type)
          continue;             /* check one letter prefix words */
        this.nodes[0] = node;   /* store the potential root node */ 
        node.mark =  0;         /* and mark it with the root index */
        if (this.makeCanonic(0, 0, 1)) root = node;
        node.mark = -1;         /* construct code word recursively, */
      }                         /* then unmark the pot. root again */
      this.nodes[0] = root; }   /* set the (new) root node */
    else {                      /* if to keep some edges */
      if (keep > graph.edgecnt) keep = graph.edgecnt;
      System.arraycopy(graph.edges, 0, this.edges, 0, keep);
      this.makeWord(graph, keep);
      this.word[(keep << 2) +1] = Integer.MAX_VALUE;
      graph.prepare();          /* construct an initial code word */
      n = 0;                    /* and prepare graph for processing */
      for (i = keep; --i >= 0; ) {  /* traverse the edges to keep */
        edge = graph.edges[i]; edge.mark = 0;
        if (edge.src.mark > n) n = edge.src.mark;
        if (edge.dst.mark > n) n = edge.dst.mark;
      }                         /* find node with highest index */
      for (i = n+1; --i >= 0; ) /* copy the marked/visited nodes */
        this.nodes[i] = graph.nodes[i];
      for (i = graph.nodecnt; --i > n; )
        graph.nodes[i].mark = -1;  /* unmark the unvisited nodes */
      for (i = graph.edgecnt; --i >= keep; )
        graph.edges[i].mark = -1;  /* unmark the edges to reorder */
      this.makeCanonic(keep, 0, n+1);
    }                           /* construct code word recursively */
    return !this.makeMap(graph, n);
  }  /* makeCanonic() */        /* build node and edge maps */

  /*------------------------------------------------------------------*/
  /** Make a given graph canonic.
   *  <p>The form of the graph (that is, the order of its nodes
   *  and edges) is changed in such a way that it produces the
   *  lexicographically smallest code word.</p>
   *  @param  graph the graph to make canonic
   *  @return whether the graphs needs to be changed
   *  @see    #makeCanonic(Graph,int)
   *  @since  2009.05.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean makeCanonic (Graph graph)
  { return this.makeCanonic(graph, -1); }

  /*------------------------------------------------------------------*/
  /** Find the node equivalence classes for a given graph.
   *  <p>This function modifies the types of the nodes of the graph.
   *  Since the original types may be needed again later, they have
   *  to be saved, and restored when the equivalence classes are not
   *  needed anymore. It also destroys the node markers and sets them
   *  all to <code>-1</code>.</p>
   *  @param  graph the graph for which to find node equivalence classes
   *  @since  2011.03.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int equivClasses (Graph graph)
  {                             /* --- find node equivalence classes */
    int   i, k, c;              /* loop variable, equiv. class ids */
    Node  node, pre;            /* current and previous node */

    System.arraycopy(graph.nodes, 0, this.nodes, 0, graph.nodecnt);
                                /* copy nodes to an internal array */
    for (i = graph.nodecnt; --i >= 0; )
      this.nodes[i].sortEdges();/* sort the incident edges */
    Arrays.sort(this.nodes, 0, graph.nodecnt, cmpDegEdges);
    pre = this.nodes[0];        /* sort the node array and */
    pre.mark = k = c = 0;       /* mark first node with first class */
    for (i = 0; ++i < graph.nodecnt; ) {
      if (cmpDegEdges.compare(pre, node = this.nodes[i]) != 0) k++;
      node.mark = k; pre = node;/* traverse the nodes and mark them */
    }                           /* with their new class identifiers */
    while (k > c) {             /* while an equiv. class was split */
      c = k;                    /* note the new maximum class id. */
      for (i = graph.nodecnt; --i >= 0; ) {
        node = this.nodes[i];   /* traverse the nodes and set */
        node.type = node.mark;  /* their new equivalence class */
      }                         /* as the node type (update) */
      for (i = graph.nodecnt; --i >= 0; )
        this.nodes[i].sortEdges(); /* resort the incident edges */
      Arrays.sort(this.nodes, 0, graph.nodecnt, cmpAdjNodes);
      pre = this.nodes[0];      /* resort the node array and */
      pre.mark = k = 0;         /* mark first node with first class */
      for (i = 0; ++i < graph.nodecnt; ) {
        if (cmpAdjNodes.compare(pre, node = this.nodes[i]) != 0) k++;
        node.mark = k; pre = node;
      }                         /* traverse the nodes and mark them */
    }                           /* with their new class identifiers */
    graph.mark(-1);             /* clear all node and edge markers */
    return this.nodes[0].type;  /* return the class of the root node */
  }  /* equivClasses() */

  /*------------------------------------------------------------------*/
  /* comparators for equivClasses()                                   */
  /*------------------------------------------------------------------*/
  /* The first of these comparators compares the node types, the node */
  /* degrees and the types of the incident edges. The second compares */
  /* only the types of the nodes themselves and the types of the      */
  /* adjacent nodes, assuming that nodes with the same type have the  */
  /* same degree. This will be the case after the first step in the   */
  /* process of assigning equivalence classes, since nodes with       */
  /* different degrees will be assigned to different classes.         */
  /*------------------------------------------------------------------*/

  private static final Comparator<Node> cmpDegEdges =
  new Comparator<Node> () {
    public int compare (Node a, Node b)
    {                           /* --- compare two nodes */
      int  i;                   /* loop variable */
      Edge x, y;                /* to traverse the incident edges */

      if (a.type < b.type) return -1; /* compare the node types */
      if (a.type > b.type) return +1; /* (only initial type here) */
      if (a.deg  < b.deg)  return -1; /* compare the node degrees */
      if (a.deg  > b.deg)  return +1; /* (number of incident edges) */
      for (i = 0; i < a.deg; i++) {   /* traverse the incident edges */
        x = a.edges[i]; y = b.edges[i];
        if (x.type < y.type) return -1;
        if (x.type > y.type) return +1;
      }                         /* compare types of corresp. edges */
      return 0;                 /* return 'nodes are equivalent' */
    }  /* compare() */
  }; /* Comparator cmpDegEdges */

  /*------------------------------------------------------------------*/

  private static final Comparator<Node> cmpAdjNodes =
  new Comparator<Node> () {
    public int compare (Node a, Node b)
    {                           /* --- compare two nodes */
      int  i;                   /* loop variable */
      Edge e;                   /* to traverse the incident edges */
      Node x, y;                /* to traverse the adjacent nodes */

      if (a.type < b.type) return -1; /* compare the node types */
      if (a.type > b.type) return +1; /* (contains degree and edges) */
      for (i = 0; i < a.deg; i++) {   /* traverse the adjacent nodes */
        e = a.edges[i]; x = (e.src != a) ? e.src : e.dst;
        e = b.edges[i]; y = (e.src != b) ? e.src : e.dst;
        if (x.type < y.type) return -1;
        if (x.type > y.type) return +1;
      }                         /* compare types of corresp. nodes */
      return 0;                 /* return 'nodes are equivalent' */
    }  /* compare() */
  }; /* Comparator cmpAdjNodes */

  /*------------------------------------------------------------------*/
  /** Check whether a given graph is a canonic extension of its base.
   *  <p>The base of the graph is the graph without the last edge in
   *  in its edge array (and without the last node if this node is
   *  incident to no other edge). The difference to the function
   *  <code>isCanonic()</code> is that this function computes node
   *  equivalence classes to simplify the construction of the canonical
   *  code word.</p>
   *  @param  graph the graph to check
   *  @return whether the given graph is a canonic extension
   *  @since  2011.03.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isCanExt (Graph graph)
  {                             /* --- check for a canonic extension */
    int  i, t;                  /* loop variable, root node type */
    Node node, root;            /* to traverse the nodes, new root */
    Edge edge, ecan;            /* last edges in graph and code word */

    if (graph.edgecnt <= 0)     /* if there are no edges, the graph */
      return true;              /* is necessarily a canonic extension */
    this.initCanonic(graph, 0); /* init. canonical form variables */
    for (i = 0; i < graph.nodecnt; i++)    /* save the node types */
      this.nmap[i] = graph.nodes[i].type;  /* (used for eq. classes) */
    t = this.equivClasses(graph);    /* find the equivalence classes */
    this.word[0] = t;           /* set smallest class as first char. */
    this.word[1] = Integer.MAX_VALUE;  /* of the canonical code word */
    root = graph.nodes[0];      /* get current root node as default */
    for (i = 0; i < graph.nodecnt; i++) {
      node = graph.nodes[i];    /* traverse the nodes of the graph */
      if (node.type != t)       /* check one letter prefix words */
        continue;               /* (must start with first eq. class) */
      this.nodes[0] = node;     /* store the potential root node */ 
      node.mark =  0;           /* and mark it with the root index */
      if (this.makeCanonic(0, 0, 1)) root = node;
      node.mark = -1;           /* construct code word recursively, */
    }                           /* then unmark the pot. root again */
    this.nodes[0] = root;       /* set the (new) root node */
    for (i = 0; i < graph.nodecnt; i++)    /* restore the node types */
      graph.nodes[i].type = this.nmap[i];  /* (discard eq. classes) */
    edge = graph.edges[i = graph.edgecnt-1];
    ecan =  this.edges[i];      /* get the last edges of the graph */
    if (  (edge.type      != ecan.type)
    ||  (((edge.src.orbit != ecan.src.orbit)
    ||    (edge.dst.orbit != ecan.dst.orbit))
    &&   ((edge.src.orbit != ecan.dst.orbit)
    ||    (edge.dst.orbit != ecan.src.orbit))))
      return false;             /* check for equivalent last edges */
    this.makeMap(graph, 0);     /* create map for adapting the graph */
    return true;                /* return that extension is canonic */
  }  /* isCanExt() */

  /*------------------------------------------------------------------*/
  /** Build a map for reordering the nodes and edges.
   *  <p>This map describes the transition from the original form to
   *  the canonical form and is built in the <code>word</code> array
   *  of this extension structure. The first <code>graph.edgecnt</code>
   *  elements of this array contain the new indices of the edges, the
   *  next <code>graph.nodecnt</code> elements contain the new indices
   *  of the nodes. The map is used to reorganize the embeddings of a
   *  fragment.</p>
   *  @param  graph the graph for which to build the map
   *  @param  n     the highest already fixed node index
   *  @return whether the map is the identity (no change needed)
   *  @since  2006.05.08 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean makeMap (Graph graph, int n)
  {                             /* --- build map for canonical form */
    int     i, k, m;            /* loop variables, buffers */
    Node    node;               /* to traverse the nodes */
    Edge    edge;               /* to traverse the edges */
    boolean id = true;          /* flag for identity mapping */

    k = graph.edgecnt;          /* get the number of edges */
    for (i = k; --i >= 0; ) this.edges[i].mark = i;
    for (i = k; --i >= 0; ) {   /* number the edges in the array, */
      edge = graph.edges[i];    /* then traverse them in the graph */
      this.emap[i] = m = edge.mark;
      id = id && (m == i);      /* build the edge map and */
      edge.mark = -1;           /* check for identity mapping, */
    }                           /* then clear the edge marker */
    this.nodes[0].mark = 0;     /* number the root node */
    for (i = 0; i < k; i++) {   /* traverse the edges and */
      edge = this.edges[i];     /* number the nodes accordingly */
      if      (edge.src.mark < 0)
        this.nodes[edge.src.mark = ++n] = edge.src;
      else if (edge.dst.mark < 0)
        this.nodes[edge.dst.mark = ++n] = edge.dst;
    }                           /* also sort nodes into new order */
    for (i = graph.nodecnt; --i >= 0; ) {
      node = graph.nodes[i];    /* traverse the nodes of the graph */
      this.nmap[i] = m = node.mark;
      id = id && (m == i);      /* build the node map and */
      node.mark = -1;           /* check for identity mapping, */
    }                           /* then clear the node marker */
    return id;                  /* return whether mapping is identity */
  }  /* makeMap() */

  /*------------------------------------------------------------------*/
  /** Create the code word for a given graph as a string.
   *  @param  graph the graph for which to create a code word
   *  @return a code word (as a string) for the given graph
   *  @since  2006.05.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected String describe (Graph graph)
  { return this.describe(graph, true); }

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
  
  protected abstract String describe (Graph graph, boolean create);
  
  
  //shirui
  public Object clone(){
	  CanonicalForm obj = null;
	  try {
		obj = (CanonicalForm) super.clone();
	} catch (CloneNotSupportedException e) {
		e.printStackTrace();
	}
	return obj;
  }

  /*------------------------------------------------------------------*/
  /** Create an extension object corresponding to a given name.
   *  @param  name the name of the extension type
   *  @return the created extension
   *  @since  2009.08.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static CanonicalForm createCnF (String name)
  {                             /* --- create a canonical form */
    if (name.equalsIgnoreCase("DFS"))      return new CnFDepth();
    if (name.equalsIgnoreCase("Depth"))    return new CnFDepth();
    if (name.equalsIgnoreCase("Rgt"))      return new CnFDepth();
    if (name.equalsIgnoreCase("RgtPath"))  return new CnFDepth();

    if (name.equalsIgnoreCase("BFS"))      return new CnFBreadth1();
    if (name.equalsIgnoreCase("Breadth"))  return new CnFBreadth1();
    if (name.equalsIgnoreCase("Max"))      return new CnFBreadth1();
    if (name.equalsIgnoreCase("MaxSrc"))   return new CnFBreadth1();
    if (name.equalsIgnoreCase("BFS1"))     return new CnFBreadth1();
    if (name.equalsIgnoreCase("Breadth1")) return new CnFBreadth1();
    if (name.equalsIgnoreCase("BFSA"))     return new CnFBreadth1();
    if (name.equalsIgnoreCase("BreadthA")) return new CnFBreadth1();
    if (name.equalsIgnoreCase("Max1"))     return new CnFBreadth1();
    if (name.equalsIgnoreCase("MaxSrc1"))  return new CnFBreadth1();
    if (name.equalsIgnoreCase("MaxA"))     return new CnFBreadth1();
    if (name.equalsIgnoreCase("MaxSrcA"))  return new CnFBreadth1();

    if (name.equalsIgnoreCase("BFS2"))     return new CnFBreadth2();
    if (name.equalsIgnoreCase("Breadth2")) return new CnFBreadth2();
    if (name.equalsIgnoreCase("BFSB"))     return new CnFBreadth2();
    if (name.equalsIgnoreCase("BreadthB")) return new CnFBreadth2();
    if (name.equalsIgnoreCase("Max2"))     return new CnFBreadth2();
    if (name.equalsIgnoreCase("MaxSrc2"))  return new CnFBreadth2();
    if (name.equalsIgnoreCase("MaxB"))     return new CnFBreadth2();
    if (name.equalsIgnoreCase("MaxSrcB"))  return new CnFBreadth2();
    return null;                /* evaluate the canonical form name */
  }  /* createCnF() */

}  /* class CanonicalForm */
