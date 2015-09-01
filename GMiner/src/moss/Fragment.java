/*----------------------------------------------------------------------
  File    : Fragment.java
  Contents: Management of graph fragments
  Author  : Christian Borgelt
  History : 2002.03.11 file created as file submol.java
            2002.03.14 output method added (toString, for debugging)
            2002.07.15 function isEquivTo() added (later rewritten)
            2003.08.01 function isPerfect() added (later rewritten)
            2003.08.06 file split, this part renamed to Fragment.java
            2003.08.07 complete rewrite of most functions
            2003.08.08 function revert() added (perfect extensions)
            2003.08.09 position of new edges changed, field "idx" added
            2005.06.08 perfect extension pruning restricted to bridges
            2005.07.22 adapted to changed embedding marking functions
            2005.07.23 function isClosed() debugged and optimized
            2005.07.24 function isCanonic() completed and debugged
            2005.08.03 special functions for seed embedding removed
            2005.08.05 chain size check for pruning simplified
            2005.08.11 function isCanonic() moved to CanonicalForm class
            2005.08.17 optional packed embedding lists added
            2005.08.21 reembedding adapted to packed embedding lists
            2005.09.02 creation of packed embedding lists modified
            2006.04.07 unnecessary code removed from function revert()
            2006.04.10 fields nodes and edges removed (unnecessary)
            2006.04.11 function adapt() added (for perfect extensions)
            2006.04.29 function adapt() debugged, Fragment.flags added
            2006.05.02 adapted to changed CanonicalForm.isCanonic()
            2006.05.03 function makeCanonic() added and debugged
            2006.05.11 function isCanonic() replaced by new version
            2006.05.14 bug in isPerfect() fixed (packed embeddings)
            2006.05.16 function mergeExts() added and debugged
            2006.05.17 single extension section treatment in mergeExts()
            2006.05.18 adapted to new field CanonicalForm.dst
            2006.05.31 function adapt() extended, result added
            2006.06.01 function isCanonic() extended, result changed
            2006.06.04 bugs in functions adapt() and mergeExts() fixed
            2006.06.06 ring extension handling in adapt() modified
            2006.06.07 bug in function isPerfect() fixed (single graph)
            2006.06.08 ring adaptation corrected in function adapt()
            2006.06.19 function isClosed() adapted to ring extensions
            2006.06.20 changed reorg. point for max. source extensions
            2006.06.22 flag CLOSED added (for closed fragment check)
            2006.06.23 flag VALID  added (for marking invalid fragments)
            2006.06.30 filtering of identical ring embeddings added
            2006.07.01 adaptation of rings moved to CanonicalForm
            2006.08.05 ring closing edges allowed for perfect extensions
            2006.08.12 adapted to new Notation classes
            2006.10.27 reembedding a fragment made more robust
            2006.10.29 isPerfect adapted, bug in reembedding fixed
            2006.10.31 adapted to refactored classes
            2006.11.03 function chainsValid() added (for output check)
            2006.11.11 function hashCode() added (for repository)
            2007.03.24 function adapt() extended (parameter 'check')
            2007.08.03 embedding counters split into focus/complement
            2007.08.09 constants FOCUS and COMPL added
            2007.08.10 support computation restructured
            2007.08.14 functions pack() and unpack() added
            2007.10.24 adapted to removed base embedding reference
            2007.10.25 main function added for testing purposes
            2007.11.08 bug in function mergeExts() fixed
            2008.03.05 another bug in function mergeExts() fixed
            2009.05.02 perfect extension pruning improved
            2009.05.04 perfect extension pruning further improved
            2009.05.07 function map() improved (embedding adaptation)
            2010.01.22 adapted to renaming of canonical form classes
            2010.01.23 functions add/first/next() renamed to *Emb()
            2010.01.24 isClosed() adapted to work with graph cover
            2011.02.18 function equalsCanonic() added
            2011.02.16 function equivSiblings() added
            2011.03.03 function map() made to work without embeddings
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.StringReader;

/*--------------------------------------------------------------------*/
/** Class for graph fragments (subgraphs and their embeddings).
 *  <p>A graph fragment is a part of a graph. It consists of a list
 *  of embeddings that indicate where the fragment can be found in
 *  different graphs. In addition, a fragment contains information
 *  about the extension edge (and maybe extension node) by which it
 *  was constructed from a smaller fragment.</p>
 *  @author Christian Borgelt
 *  @since  2002.03.11 */
/*--------------------------------------------------------------------*/
public class Fragment {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** group identifier: focus */
  public    static final int FOCUS     = NamedGraph.FOCUS;
  /** group identifier: complement */
  public    static final int COMPL     = NamedGraph.COMPL;
  /** support type: number of graphs that contain an embedding */
  public    static final int GRAPHS    = 0;
  /** support type: maximum independent set of normal  overlap graph */
  public    static final int MIS_OLAP  = 1;
  /** support type: maximum independent set of harmful overlap graph */
  public    static final int MIS_HARM  = 2;
  /** support type: minimum number of different nodes mapped to */
  public    static final int MIN_IMAGE = 3;
  /** the support type mask */
  public    static final int SUPPMASK  = 0x03;
  /** support type flag: whether to use the greedy MIS algorithm */
  public    static final int GREEDY    = 0x10;
  /** flag for a valid fragment; invalid fragments can occur in ring
   *  mining with canonical form pruning: they are non-canonical
   *  fragments that have to processed nevertheless */
  protected static final int VALID     = 0x0001;
  /** flag for a closed fragment
   *  (no super-fragment has the same support) */
  protected static final int CLOSED    = 0x0002;
  /** flag for the result of an extension that is a chain start */
  protected static final int CHAIN     = 0x0004;
  /** flag for possible equivalent siblings */
  protected static final int SIBLINGS  = 0x0008;
  /** flag for a perfect extension; set if the fragment was created
   *  by a perfect extension of its base fragment */
  protected static final int PERFECT   = 0x0010;
  /** flag for reverted extension information */
  protected static final int REVERTED  = 0x0020;
  /** flag for an adapted fragment; this flag is set when a fragment
   *  gets adapted, so that the adaptation is not repeated */
  protected static final int ADAPTED   = 0x0040;
  /** flag for valid orbit identifiers in the fragment graph */
  protected static final int ORBITS    = 0x0080;
  /** flag for a packed list of embeddings */
  protected static final int PACKED    = 0x0100;
  /** default flags that are set when a fragment is created */
  protected static final int DEFAULT   = VALID | CLOSED | SIBLINGS;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the fragment as a graph */
  protected Graph      graph;
  /** the base fragment (which was extended) */
  protected Fragment   base;
  /** the current base embedding (which was extended) */
  protected Embedding  bemb;
  /** the graph cover of the fragment */
  protected NamedGraph cover[];
  /** the list of embeddings of the fragment */
  protected Embedding  list;
  /** the tail of the embedding list */
  protected Embedding  tail;
  /** the current embedding (cursor) */
  protected Embedding  curr;
  /** the maximal number of embeddings per graph */
  protected int        max;
  /** the number of embeddings/graphs for the current graph */
  protected int        cnt;
  /** the number of variable length chains */
  protected int        chcnt;
  /** the index of the (first) new edge */
  protected int        idx;
  /** the index of the source node of the (first) new edge */
  protected int        src;
  /** the index of the destination node of the (first) new edge */
  protected int        dst;
  /** the number of nodes in a ring or chain
    *  (positive: ring, negative: chain) */
  protected int        size;
  /** the property flags of the embedding (e.g. <code>PERFECT</code>) */
  protected int        flags;
  /** the support and embedding counters for focus and complement */
  protected int[]      supp;
  /** the indices of the nodes of new ring edges */
  protected int[]      ris;
  
  /**gSemi value of current fragment, added by Shirui Pan, 25/5/2012*/
  protected double gSemiValue;

  /*------------------------------------------------------------------*/
  /** Create an empty fragment.
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Fragment ()
  { this(null, 0); }

  /*------------------------------------------------------------------*/
  /** Create a fragment from a graph.
   *  @param  graph the subgraph representing the fragment
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Fragment (Graph graph)
  { this(graph, 0); }

  /*------------------------------------------------------------------*/
  /** Create an empty fragment with an embedding limit.
   *  @param  max the maximum number of embeddings per graph
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Fragment (int max)
  { this(null, max); }

  /*------------------------------------------------------------------*/
  /** Create a fragment with an embedding limit from a graph.
   *  @param  graph the subgraph representing the fragment
   *  @param  max   the maximum number of embeddings per graph
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Fragment (Graph graph, int max)
  {                             /* --- create an initial fragment */
    this.graph = graph;         /* store the graph */
    this.base  = null;          /* clear the base, the graph cover, */
    this.cover = null;          /* and the embedding list variables */
    this.bemb  = this.list = this.tail = this.curr = null;
    this.max   = (max > 0) ? max : Integer.MAX_VALUE;
    this.cnt   = 0;             /* set memory usage parameters */
    this.src   = this.dst  = 0; /* clear the extension information */
    this.chcnt = this.size = 0; /* and init. the other parameters */
    this.idx   = -1;            /* there is no previous edge */
    this.flags = DEFAULT;       /* set the default properties */
    this.supp  = new int[5];    /* create the support counters */
  }  /* Fragment() */

  /*------------------------------------------------------------------*/
  /** Create a fragment from a graph and a subgraph.
   *  @param  graph the graph into which to embed the subgraph
   *  @param  sub   the subgraph to embed into the graph
   *  @since  2007.10.25 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Fragment (Graph graph, Graph sub)
  { this(graph, sub, 0); }

  /*------------------------------------------------------------------*/
  /** Create a fragment from a graph and a subgraph.
   *  @param  graph the graph into which to embed the subgraph
   *  @param  sub   the subgraph to embed into the graph
   *  @param  max   the maximum number of embeddings
   *  @since  2007.10.25 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Fragment (Graph graph, Graph sub, int max)
  {                             /* --- create a fragment */
    this(graph, max);           /* do basic initialization */
    graph.prepare();            /* prepare the graphs for embedding */
    sub.prepareEmbed();         /* and then embed the subgraph */
    //System.out.println(graph.contains(sub));
    this.addEmb(graph.embed(sub));     /* into the given graph */
  }  /* Fragment() */

  /*------------------------------------------------------------------*/
  /** Special constructor for use in <code>mergeExts()</code>.
   *  <p>A fragment is created from a base fragment and information
   *  about a single edge extension, which is the common first edge
   *  of the fragments to be merged into it.</p>
   *  @param  frag the base fragment which was extended by rings
   *  @param  idx  the index of the first ring edge (only edge to keep)
   *  @param  src  the index of the source      node of the edge
   *  @param  dst  the index of the destination node of the edge
   *  @see    #mergeExts(Fragment[],int)
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private Fragment (Fragment frag, int idx, int src, int dst)
  {                             /* --- create an extended fragment */
    this.graph = null;          /* not yet available as a graph */
    this.base  = frag;          /* store the base, clear graph cover */
    this.cover = null;          /* and embedding list variables */
    this.bemb  = this.list = this.tail = this.curr = null;
    this.max   = frag.max;      /* set memory usage parameters */
    this.cnt   = 0;             /* (number of embeddings per graph) */
    this.chcnt = frag.chcnt;    /* copy the number of chains */
    this.idx   = idx;           /* store the parameters */
    this.src   = src;           /* of the extension edge */
    this.dst   = dst;           /* (edge and node indices) */
    this.size  = 0;             /* set the type to single edge ext. */
    this.flags = DEFAULT;       /* set the default properties */
    this.supp  = new int[5];    /* create the support counters */
  }  /* Fragment() */

  /*------------------------------------------------------------------*/
  /** Create an extended fragment.
   *  <p>A fragment is created from a base fragment and information
   *  about a single edge extension, which is the common first edge
   *  of the fragments to be merged into it.</p>
   *  @param  frag the base fragment which was extended by rings
   *  @param  src  the source      node index of the edge to add
   *  @param  dst  the destination node index of the edge to add
   *  @param  edge the edge type              of the edge to add
   *  @param  node the destination node type  of the edge to add
   *  @since  2010.01.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Fragment (Fragment frag, int src,int dst, int edge,int node)
  {                             /* --- create an extended fragment */
    this(frag, frag.graph.edgecnt, src, dst);
    this.graph = new Graph(frag.graph, src, dst, edge, node);
  }  /* Fragment() */

  /*------------------------------------------------------------------*/
  /** Create a fragment from an extension.
   *  <p>This function is called if there is no fragment that is
   *  equivalent to a created extension and thus a new fragment
   *  has to be created.</p>
   *  @param  cnf the canonical form from which to create the fragment
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Fragment (CanonicalForm cnf)
  {                             /* --- create an extended fragment */
    int  i, k, n;               /* loop variable, counters, buffer */
    Node node;                  /* to traverse the nodes */

    this.graph = null;          /* not yet available as a graph */
    this.base  = cnf.frag;      /* note the base fragment and embed. */
    this.bemb  = cnf.emb;       /* create an extended embedding */
    this.list  = this.tail = this.curr = cnf.makeEmbedding();
    this.max   = cnf.frag.max;  /* copy the maximum number */
    this.cnt   = 1;             /* of embeddings per graph */
    this.size  = cnf.size;      /* note the extension type, */
    this.chcnt = cnf.chcnt;     /* the number of chains, */
    this.src   = cnf.src;       /* the source node index, and */
    this.dst   = cnf.dst;       /* the destination node index */
    this.idx   = cnf.emb.edges.length;
    this.flags = DEFAULT;       /* set the default properties */
    if (cnf.edges[0].isBridge() /* check for a chain start */
    && (cnf.edges[0].type == cnf.cedge)
    && (cnf.nodes[1].type == cnf.cnode))
      this.flags |= CHAIN;      /* set chain start flag if needed */
    if (cnf.useOrbits() && (cnf.size == 0) && (cnf.nodecnt > 0))
      this.flags &= ~SIBLINGS;  /* clear sibling flag for new node */
    i = this.list.getGroup();   /* get group of underlying graph */
    this.supp = new int[5];     /* create the support counters */
    this.supp[  i] = this.supp[2+i] = 1;
    this.supp[1-i] = this.supp[3-i] = 0;
    this.supp[ 4 ] = ((cnf.dst < cnf.emb.nodes.length)
                   ||  cnf.edges[0].isBridge()) ? 1 : 0;
    if (cnf.size <= 0) return;  /* count graph, embedding and base */
    this.ris = new int[((cnf.edgecnt -1) << 1) +3];
    n = cnf.emb.nodes.length;   /* get the next node index */
    for (k = 0, i = 1; i < cnf.size; i++) {
      if (cnf.edges[i].mark >= 0) continue;
      node = cnf.nodes[i];      /* note node indices of new edges */
      this.ris[k++] = (node.mark >= 0) ? node.mark : n++;
      node = cnf.nodes[(i+1) % this.size];
      this.ris[k++] = (node.mark >= 0) ? node.mark : n;
    }                           /* (for CanonicalForm.compareRing) */
    this.ris[k++] = cnf.pos1;   /* note the insertion positions */
    this.ris[k++] = cnf.pos2;   /* (distinguish equivalent rings) */
    this.ris[k++] = cnf.pmax;   /* and the maximal position */
  }  /* Fragment() */

  /*------------------------------------------------------------------*/
  /** Check whether two fragments are equal.
   *  <p>This method is overridden only to avoid certain warnings.</p>
   *  @param  frag the fragment to compare to
   *  @return whether the two fragments are equal
   *  @since  2007.11.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean equals (Object frag)
  { return this == frag; }

  /*------------------------------------------------------------------*/
  /** Compute the hash code of the fragment.
   *  @return the hash code of the fragment
   *  @since  2006.11.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int hashCode ()
  {                             /* --- compute a hash code */
    return (this.graph != null) /* use graph if it exists */
          ? this.graph.hashCode() : this.list.hashCode();
    /* If it exists, the graph is used, because the hash code */
    /* computation is faster for it than for an embedding.    */
  }  /* hashCode() */

  /*------------------------------------------------------------------*/
  /** Get the size of the fragment.
   *  <p>The size of the fragment is the number of nodes in an output
   *  (or a created subgraph), which is why the number of chains, each
   *  of which will be represented as a pseudo-node, has to be added
   *  to the number of nodes of an embedding.</p>
   *  @since  2003.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int size ()
  {                             /* --- get the size of the fragment */
    return (this.graph != null) /* use graph if it exists */
          ? this.graph.nodecnt : this.list.nodes.length +this.chcnt;
  }  /* size() */

  /*------------------------------------------------------------------*/
  /** Get the fragment as a (sub-)graph.
   *  <p>For this function to work the fragment must be created from
   *  a subgraph or an extension or at least one embedding must
   *  have been added to it. Otherwise the (sub-)graph representing
   *  the fragment is undefined.</p>
   *  @return the fragment as a (sub-)graph
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph getGraph ()
  {                             /* --- get fragment as a graph */
    if (this.graph == null)     /* create a graph if necessary */
      this.graph = new Graph(this);
    return this.graph;          /* return created graph */
  }  /* getGraph() */

  /*------------------------------------------------------------------*/
  /** Add an embedding (list) to the fragment.
   *  <p>This function is meant for adding the embeddings of the seed
   *  structure into some graph to the seed fragment. It is assumed
   *  that, if multiple embeddings are added with one call (that is,
   *  if a list of embeddings is added), they all refer to the same
   *  graph. On the other hand, the function may be called several
   *  times with different lists of embbedings all of which refer to
   *  the same graph.</p>
   *  <p>If the fragment is limited w.r.t. the number of embeddings
   *  that may be stored per graph, the embedding(s) may not actually
   *  be stored, but will be regenerated on demand.</p>
   *  @param  emb the embedding to add to the fragment
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void addEmb (Embedding emb)
  { this.addEmb(emb, 1); }

  /*------------------------------------------------------------------*/
  /** Add an embedding (list) to the fragment.
   *  <p>The parameter <code>inc</code> is needed for reembedding,
   *  which should not change the support counters. For these cases
   *  this function is called with <code>inc == 0</code>.</p>
   *  @param  emb the (list of) embedding(s) to add to the fragment
   *  @param  inc the amount by which to increment the support counters
   *  @since  2006.10.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void addEmb (Embedding emb, int inc)
  {                             /* --- add (a list of) embedding(s) */
    if (emb == null) return;    /* check whether the embedding exists */
    int group = emb.getGroup(); /* get the group of the graph */
    if (this.list == null) {    /* if the list is empty, */
      this.list         = emb;  /* set embedding(s) as the new list */
      this.supp[group] += inc;} /* count the graph for its group */
    else {                      /* if there is already a list, */
      this.tail.succ = emb;     /* append the new embedding */
      if (emb.graph != this.tail.graph) {
        this.supp[group] += inc;/* count the graph if necessary, */
        this.cnt  = 0;          /* (re)init. the embedding counter, */
        this.curr = emb;        /* and note the new embedding(s) */
      }                         /* as the first for this graph */
    }
    group += 2;                 /* compute index for embeddings */
    do {                        /* traverse the embedding list */
      this.cnt++;               /* count the embedding */
      this.supp[group] += inc;  /* (per graph and globally) */
      this.tail = emb;          /* note new tail of embedding list */
      emb       = emb.succ;     /* and go to the next embedding */
    } while (emb != null);      /* while not at the end of the list */
    if ((this.cnt        <= this.max)
    ||  (this.tail.graph == this.list.graph))
      return;                   /* check condition for packing */
    this.flags |= PACKED;       /* set flag for packed embeddings */
    if (this.graph == null)     /* create graph if necessary */
      this.graph = new Graph(this);
    /* Note that creating this graph works only, because it is   */
    /* certain that the current embedding refers to a different  */
    /* graph than the one that is used to create this graph.     */
    /* This is also the reason why embeddings into the first     */
    /* graph cannot be packed, at least not at this point.       */
    /* 2009.05.04: These obstacles have been removed by a change */
    /* in the procedure that creates the graph. However, this    */
    /* function as well as the equivalent sibling pruning still  */
    /* have to be adapted to allow for packing the first embeds. */
    this.tail = emb = this.curr;
    emb.succ  = null;           /* if there are too many embeddings, */
    emb.nodes = null;           /* delete them and only keep */
    emb.edges = null;           /* a reference to the graph */
  }  /* add() */

  /*------------------------------------------------------------------*/
  /** Add an extension (as an embedding) to the fragment.
   *  <p>This function is called if a created extension is equivalent
   *  to the fragment and thus only an embedding has to be added.
   *  It assumes that the extension is equal to the fragment,
   *  that is, that <code>cnf.compareTo(this)</code> yields 0.</p>
   *  @param  cnf the canonical form describing the embedding to add
   *  @return whether the embedding was added (duplicate check)
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean addEmb (CanonicalForm cnf)
  {                             /* --- add an extended embedding */
    int       i;                /* buffer for group */
    Embedding emb;              /* created/packed embedding */

    if ((cnf.size < 0)          /* if this is a chain fragment, */
    &&  (cnf.size > this.size)) /* check the chain sizes for pruning */
      this.size = cnf.size;     /* (determine the min. chain length) */
    i = cnf.emb.getGroup();     /* get group of underlying graph */
    this.supp[i+2]++;           /* count the new embedding */
    this.cnt++;                 /* (globally and for last graph) */
    if (cnf.emb.graph != this.tail.graph) {
      this.supp[i]++;           /* count a new graph for its group */
      this.cnt = 0;             /* (re)init. the embedding counter */
    }                           /* for the last graph */
    if (cnf.emb != this.bemb)   /* if created from a new base, */
      this.supp[4]++;           /* count the base embedding */
    if (!cnf.edges[0].isBridge()/* if edge to new node and no bridge */
    &&  (cnf.dst >= cnf.emb.nodes.length))
      this.supp[4] = 0;         /* the extension is not perfect */
    this.bemb = cnf.emb;        /* note the current base embedding */
    if ((this.cnt <= this.max)  /* check conditions for normal append */
    ||  (cnf.emb.graph == this.list.graph)) {
      emb = cnf.makeEmbedding();/* create a new embedding */
      if (emb.graph != this.tail.graph)
        this.curr = emb;        /* note embedding as the first */
      this.tail = this.tail.succ = emb;
      return true;              /* append new embedding to the list */
    }                           /* and abort the function */
    if (this.curr.nodes == null)/* if embedding is already packed, */
      return true;              /* abort the function without action */
    this.flags |= PACKED;       /* set flag for packed embeddings */
    if (this.graph == null)     /* create graph if necessary */
      this.graph = new Graph(this);
    /* Note that creating this graph works only, because it is   */
    /* certain that the current embedding refers to a different  */
    /* graph than the one that is used to create this graph.     */
    /* This is also the reason why the embeddings for the first  */
    /* graphs cannot be packed, at least not at this point.      */
    /* 2009.05.04: These obstacles have been removed by a change */
    /* in the procedure that creates the graph. However, this    */
    /* function as well as the equivalent sibling pruning still  */
    /* have to be adapted to allow for packing the first embeds. */
    this.tail = emb = this.curr;
    emb.succ  = null;           /* if there are too many embeddings, */
    emb.nodes = null;           /* delete them and only keep */
    emb.edges = null;           /* a reference to the graph */
    return true;                /* return 'embedding was added' */
  }  /* add() */

  /*------------------------------------------------------------------*/
  /** Pack the list of embeddings.
   *  <p>Pack the list of embeddings with the maximum number of
   *  embeddings per graph that was specified when this fragment
   *  was created.</p>
   *  @since  2007.08.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void pack ()
  { this.pack(this.max); }

  /*------------------------------------------------------------------*/
  /** Pack the list of embeddings.
   *  @param  max the maximum number of embeddings per graph
   *  @since  2007.08.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void pack (int max)
  {                             /* --- pack the list of embeddings */
    Embedding emb;              /* to traverse the embeddings */
    Graph     g;                /* graph underlying current embedding */
    int       old = this.max;   /* old maximal number of embeddings */

    if (max < 0) max = old;     /* check and adapt and then update */
    this.max = max;             /* the maximal number of embeddings */
    if ((((this.flags & PACKED) != 0) && (max >= old))
    ||  (((this.flags & PACKED) == 0) && (max >= Integer.MAX_VALUE))
    ||  (this.list == null))    /* check whether packing is necessary */
      return;                   /* and abort if it is not */
    this.tail = this.curr = this.list;
    g = this.list.graph;        /* always keep first embedding */
    for (emb = this.list.succ; emb != null; emb = emb.succ) {
      if (emb.graph != g) {     /* traverse the embeddings */
        if ((g != this.list.graph)  /* if at a new graph */
        &&  (this.cnt > this.max)) {
          this.tail = this.curr;    /* if too many embeddings */
          this.curr.nodes = null;   /* into the previous graph, */
          this.curr.edges = null;   /* delete them and only keep */
          this.flags |= PACKED;     /* a reference to the graph */
        }                           /* set flag for packed embeddings */
        g = emb.graph;          /* note the new underlying graph */
        this.curr = emb;        /* and the first embedding into it */
        this.cnt = 0;           /* reinit. the embedding counter */
      }
      this.tail = this.tail.succ = emb;
      this.cnt++;               /* append the embedding to the list */
    }                           /* and count it for the current graph */
  }  /* pack() */

  /*------------------------------------------------------------------*/
  /** Unpack the list of embeddings.
   *  @since  2007.08.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void unpack ()
  {                             /* --- unpack the list of embeddings */
    Embedding emb, tmp;         /* to traverse the embeddings */
    if ((this.flags & PACKED) == 0)
      return;                   /* check for packed embeddings */
    tmp = this.firstEmb();      /* get the first two embeddings */
    for (emb = this.nextEmb(); emb != null; emb = this.nextEmb())
      tmp = tmp.succ = emb;     /* always append the next embedding */
    this.flags &= ~PACKED;      /* clear flag for packed embeddings */
  }  /* unpack() */

  /*------------------------------------------------------------------*/
  /** Get the first embedding of the fragment.
   *  <p>Together with the function <code>next()</code> this function
   *  allows to traverse the list of embeddings without having to pay
   *  attention to the fact that due to a limit for the number of
   *  embeddings that may be stored per graph, some embeddings
   *  have been packed and thus are not available directly. Rather
   *  these two functions regenerate the embeddings from a packed
   *  embedding by reembedding the subgraph representing the
   *  fragment into the corresponding graph.</p>
   *  <p>Note that this function, just as the accompanying function
   *  <code>next()</code>, modifies the <code>tail</code> pointer,
   *  which is reused as a cursor for packed embeddings, and thus it
   *  is impossible to add embeddings after this function has been
   *  called.</p>
   *  @return the first embedding of the fragment or <code>null</code>
   *          if there is no embedding
   *  @see    #nextEmb()
   *  @since  2005.08.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Embedding firstEmb ()
  {                             /* --- get the first embedding */
    this.ris  = null;           /* "delete" the node index array */
    this.tail = null;           /* clear the packed embeddings cursor */
    if (this.cover != null) {   /* if there is a graph cover, */
      this.graph.mark(-1);      /* unmark all nodes and edges */
      return this.curr = this.cover[this.cnt = 0].embed(this.graph);
    }                           /* create embeddings of first graph */
    return this.curr = this.list;
  }  /* first() */              /* return the first list element */

  /*------------------------------------------------------------------*/
  /** Get the next embedding of the fragment.
   *  @return the next embedding of the fragment or <code>null</code>
   *          if there is no next embedding
   *  @see    #firstEmb()
   *  @since  2005.08.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Embedding nextEmb ()
  {                             /* --- get the next embedding */
    if (this.curr == null)      /* if there is no current embedding, */
      return null;              /* there is no next embedding */
    this.curr = this.curr.succ; /* advance the embedding cursor */
    if (this.cover != null) {   /* if there is a graph cover */
      if (this.curr != null) return this.curr;
      if (++this.cnt >= this.supp[0] +this.supp[1])
        return null;            /* check whether at last graph */
      return this.curr = this.cover[this.cnt].embed(this.graph);
    }                           /* create next list of embeddings */
    if (this.curr == null) {    /* if at end of (current) list */
      this.curr = this.tail;    /* check for another packed element */
      this.tail = null;         /* clear the packed element cursor */
      if (this.curr == null)    /* if there is none, this is */
        return null;            /* really the end of the list */
    }
    if (this.curr.nodes != null)/* if this is not a packed element, */
      return this.curr;         /* it can be returned directly */
    this.tail = this.curr.succ; /* advance packed element pointer */
    return this.curr = this.curr.graph.embed(this.graph);
  }  /* next() */               /* reembed fragment into graph */

  /*------------------------------------------------------------------*/
  /** Add a graph to the cover of the fragment.
   *  @param  graph the graph to add to the cover
   *  @since  2010.01.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void addGraph (NamedGraph graph)
  {                             /* --- add a graph to the cover */
    int   k;                    /* size of the cover (support) */
    Graph c[];                  /* buffer for old cover array */

    k = this.supp[0] +this.supp[1]; /* get current size of cover */
    c = this.cover;             /* if the cover array is full */
    if ((c == null) || (k >= c.length)) {
      this.cover = new NamedGraph[k +((k >= 256) ? k >> 1 : 256)];
      if (c != null) System.arraycopy(c, 0, this.cover, 0, k);
    }                           /* enlarge the cover array */
    this.supp[graph.group]++;   /* count graph for the support */
    this.cover[k] = graph;      /* add the graph to the cover */
  }  /* addGraph() */

  /*------------------------------------------------------------------*/
  /** Get the first graph containing the fragment.
   *  <p>Together with the function <code>nextGraph()</code> this
   *  function allows to traverse the list of graphs containing this
   *  fragment without having to pay attention to the fact that the
   *  fragment may have several embeddings into the same graph.</p>
   *  @return the first graph containing the fragment or
   *          <code>null</code> if there is no such graph
   *  @see    #nextGraph()
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph firstGraph ()
  {                             /* --- get the first graph */
    if (this.cover != null)     /* if there is a cover, use it */
      return this.cover[this.cnt = 0];
    this.curr = this.list;      /* get the first embedding */
    return (this.curr != null) ? this.curr.graph : null;
  }  /* firstGraph() */         /* return the embedding's graph */

  /*------------------------------------------------------------------*/
  /** Get the next graph containing the fragment.
   *  @return the next graph containing the fragment or
   *          <code>null</code> if there is no next graph
   *  @see    #firstGraph()
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph nextGraph ()
  {                             /* --- get the next graph */
    if (this.cover != null)     /* if there is a cover, use it */
      return (++this.cnt < this.supp[0] +this.supp[1])
           ? this.cover[this.cnt] : null;
    Graph g = this.curr.graph;  /* note the current graph */
    do {                        /* find embedding with new graph */
      this.curr = this.curr.succ;
      if (this.curr == null) return null;
    } while (this.curr.graph == g);
    return this.curr.graph;     /* return the embedding's graph */
  }  /* nextGraph() */

  /*------------------------------------------------------------------*/
  /** Find the minimum number of different nodes a node is mapped to.
   *  <p>The minimum number of different nodes a node is mapped to is
   *  determined separately for the two graph groups (<code>FOCUS</code>
   *  and <code>COMPL</code>) and stored in <code>this.supp</code>.</p>
   *  @since  2007.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void getMinImage ()
  {                             /* --- compute min. node images */
    int       i;                /* loop variable */
    Graph     g;                /* to traverse the graphs */
    Embedding emb;              /* to traverse the embeddings */
    int[]     ics = new int[2]; /* node image counters */

    this.supp[FOCUS] =          /* init. the node image minima */
    this.supp[COMPL] = Integer.MAX_VALUE;
    this.unpack();              /* unpack the list of embeddings */
    /* A packed embedding list must be unpacked in order to avoid   */
    /* re-embedding, because re-embedding interferes with the node  */
    /* marking used in this function to count the number of images. */
    /* Unpacking is almost without cost if the list is not packed.  */
    for (i = this.size(); --i >= 0; ) {
      ics[FOCUS] = 0;           /* traverse the nodes of the fragment */
      ics[COMPL] = 0;           /* init. the node image counters */
      for (emb = this.list; emb != null; emb = emb.succ) {
        if (emb.nodes[i].mark == i) continue;
        emb.nodes[i].mark = i;  /* traverse the embeddings, */
        ics[emb.getGroup()]++;  /* mark any new node image and */
      }                         /* count the different node images */
      if (ics[FOCUS] < this.supp[FOCUS]) this.supp[FOCUS] = ics[FOCUS];
      if (ics[COMPL] < this.supp[COMPL]) this.supp[COMPL] = ics[COMPL];
    }                           /* update the node image minima */
    this.pack();                /* repack list of embeddings */
    /* Repacking the list of embeddings is basically without cost  */
    /* if the list was not packed before this function was called. */
    for (g = this.firstGraph(); g != null; g = this.nextGraph()) {
      for (i = g.nodecnt; --i >= 0; )
        if (g.nodes[i].mark >= 0) g.nodes[i].mark = -1;
    }                           /* clear all node markers */
  }  /* getMinImage() */

  /*------------------------------------------------------------------*/
  /** Find the size of a maximum independent set of the overlap graph.
   *  <p>The size of a maximum independent set of the overlap graph is
   *  determined separately for the two graph groups (<code>FOCUS</code>
   *  and <code>COMPL</code>) and stored in <code>this.supp</code>.</p>
   *  @param  type the type of MIS support to compute
   *  @since  2007.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void getMISSize (int type)
  {                             /* --- compute max. indep. set size */
    OverlapGraph olap;          /* overlap graph of embeddings */
    Embedding    emb;           /* to traverse the embeddings */
    Graph        g;             /* graph underlying current embedding */
    int          group;         /* group of a graph (focus/compl.) */

    this.supp[FOCUS] = this.supp[COMPL] = 0;
    olap  = new OverlapGraph((type & SUPPMASK) == MIS_HARM);
    group = FOCUS; g = null;    /* create an overlap graph */
    for (emb = this.firstEmb(); emb != null; emb = this.nextEmb()) {
      if (emb.graph != g) {     /* traverse the embeddings and */
        if (g != null)          /* if the next graph is reached */
          this.supp[group] += olap.getMISSize((type & GREEDY) != 0);
        olap.clear();           /* compute the MIS size and */
        g     = emb.graph;      /* reinit. the overlap graph */
        group = emb.getGroup(); /* note the next graph */
      }                         /* and its group */
      olap.add(emb);            /* add the current embedding */
    }                           /* to the overlap graph */
    if (g != null)              /* process the last graph */
      this.supp[group] += olap.getMISSize((type & GREEDY) != 0);
  }  /* getMISSize() */

  /*------------------------------------------------------------------*/
  /** Compute the support of the fragment.
   *  <p>The support of the fragment in both graph groups, that is,
   *  <code>FOCUS</code> and <code>COMPL</code>, is computed.
   *  The computed values can then be accessed with the functions
   *  {@link #getSupport(int)}, {@link #getFocusSupport()}, and
   *  {@link #getComplSupport()}.</p>
   *  @param  type the type of support to compute
   *  @since  2007.06.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void computeSupport (int type)
  {                             /* --- compute the support */
    int t = type & SUPPMASK;    /* get the pure support type */
    if (t == GRAPHS)            /* if the support of a fragment */
      return;                   /* is the number of graphs, abort */
    if (this.list.nodes.length <= 1) {
      this.supp[FOCUS] = this.supp[2+FOCUS];
      this.supp[COMPL] = this.supp[2+COMPL];
      return;                   /* if the fragment has only one node, */
    }                           /* the support is the number of embs. */
    if (t == MIN_IMAGE) this.getMinImage();
    else                this.getMISSize(type);
  }  /* computeSupport() */     /* otherwise call special function */

  /*------------------------------------------------------------------*/
  /** Get the support of a fragment (in both groups together).
   *  @return the (total) support of a fragment
   *  @see    #computeSupport(int)
   *  @since  2007.10.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getSupport ()
  { return this.supp[FOCUS] +this.supp[COMPL]; }

  /*------------------------------------------------------------------*/
  /** Get the support of a fragment in the given group.
   *  @param  group the group for which to get the support
   *  @return the support of a fragment in the given group
   *  @see    #computeSupport(int)
   *  @since  2007.08.09 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getSupport (int group)
  { return this.supp[group]; }

  public double getgSemiValue() {
	return gSemiValue;
}

public void setgSemiValue(double gSemiValue) {
	this.gSemiValue = gSemiValue;
}

/*------------------------------------------------------------------*/
  /** Get the focus support of a fragment.
   *  @return the focus support of a fragment
   *  @see    #computeSupport(int)
   *  @since  2007.06.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getFocusSupport ()
  { return this.supp[FOCUS]; }

  /*------------------------------------------------------------------*/
  /** Get the complement support of a fragment.
   *  @return the complement support of a fragment
   *  @see    #computeSupport(int)
   *  @since  2007.06.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getComplSupport ()
  { return this.supp[COMPL]; }

  /*------------------------------------------------------------------*/
  /** Get the number of embeddings (in both groups together).
   *  @return the (total) number of embeddings
   *  @since  2007.10.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getEmbCount ()
  { return this.supp[2+FOCUS] +this.supp[2+COMPL]; }

  /*------------------------------------------------------------------*/
  /** Get the number of embeddings in the given group.
   *  @param  group the group for which to get the number of embeddings
   *  @return the number of embeddings in the given group
   *  @since  2007.08.09 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getEmbCount (int group)
  { return this.supp[2+group]; }

  /*------------------------------------------------------------------*/
  /** Get the number of embeddings in the focus.
   *  @return the number of embeddings in the focus
   *  @since  2007.08.09 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getFocusEmbCount ()
  { return this.supp[2+FOCUS]; }

  /*------------------------------------------------------------------*/
  /** Get the number of embeddings in the complement.
   *  @return the number of embeddings in the complement
   *  @since  2007.08.09 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getComplEmbCount ()
  { return this.supp[2+COMPL]; }

  /*------------------------------------------------------------------*/
  /** Unembed a fragment, that is, remove all embeddings.
   *  <p>The embeddings of fragments that correspond to nodes in the
   *  search tree that are siblings of nodes on the current path in
   *  the search tree can temporarily be removed. They can recreated
   *  from the embeddings of their base fragments when recursion
   *  returns and the fragment is actually processed.</p>
   *  @since  2005.08.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void unembed ()
  {                             /* --- remove embeddings */
    if (this.graph == null)     /* turn fragment into a graph */
      this.graph = new Graph(this);      /* (for reembedding) */
    this.list = this.tail = this.curr = null;
    this.cnt  = 0;              /* clear the embedding list */
  }  /* unembed() */

  /*------------------------------------------------------------------*/
  /** Reembed a fragment, that is, recreate its embedding list.
   *  <p>As long as sibling fragments are processed, the embeddings
   *  of a fragment need not be avaiblable and thus can temporarily
   *  be removed. They are recreated when the fragment is actually
   *  processed. This is done by extending their base embeddings or,
   *  if this is impossible, by reembedding the fragment.</p>
   *  @since  2005.08.17 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void reembed ()
  {                             /* --- reembed a fragment */
    int       d;                /* destination node index */
    int       edge, node;       /* types of edge and dest. node */
    Embedding emb;              /* to traverse the embeddings */
    Graph     g;                /* to traverse the graphs */

    if (this.list != null)      /* if embeddings exist, */
      return;                   /* reembedding is not necessary */
    if ((this.size != 0)        /* if extension of base is impossible */
    ||  (this.idx < this.graph.edgecnt-1)
    ||  ((this.base.flags & REVERTED) != 0)) {
      g = this.base.firstGraph();  /* reembed the substructure */
      for ( ; g != null; g = this.base.nextGraph())
        this.addEmb(g.embed(this.graph), 0); }
    else {                      /* if extension of base is possible */
      emb  = this.base.firstEmb();    /* get base and its extension */
      d    = (this.dst < emb.nodes.length) ? this.dst : -1;
      edge = this.graph.edges[this.idx].type;
      node = this.graph.nodes[this.dst].type;
      for ( ; emb != null; emb = this.base.nextEmb())
        this.addEmb(emb.extend(this.src, d, edge, node), 0);
    }                           /* create and add extended embeddings */
    /* Note that some of the base embeddings may not be extendable  */
    /* and that some of the graphs containing the base fragment may */
    /* not contain the extended fragment. In such cases g.embed and */
    /* emb.extend, respectively, yield null, thus requiring a check.*/
  }  /* reembed() */

  /*------------------------------------------------------------------*/
  /** Set or clear the valid flag.
   *  <p>By default a fragment is valid. The valid flag is cleared for
   *  a fragment that is not canonical (and thus must not be reported),
   *  but nevertheless has to be kept and processed. Such fragments
   *  can occur when ring extensions are combined with canonical form
   *  pruning.</p>
   *  @param  valid whether to set or clear the valid flag
   *  @since  2006.06.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void setValid (boolean valid)
  { if (valid) this.flags |= VALID; else this.flags &= ~VALID; }

  /*------------------------------------------------------------------*/
  /** Check whether the valid flag is set.
   *  <p>If the valid flag is cleared, the fragment need not be
   *  reported, because it is non-canonical and thus a duplicate.</p>
   *  @return whether the valid flag is set
   *  @since  2006.06.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isValid ()
  { return (this.flags & VALID) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether the fragment is the result of a ring extension.
   *  @return whether the fragment is the result of a ring extension
   *  @since  2003.08.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isRingExt ()
  { return this.size > 0; }

  /*------------------------------------------------------------------*/
  /** Check whether the fragment is the result of an extension
   *  by a ring edge.
   *  @return whether the fragment is the result of an extension
   *          by a ring edge
   *  @since  2006.05.16 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isRingEdgeExt ()
  { return this.list.edges[this.idx].isInRing(); }

  /*------------------------------------------------------------------*/
  /** Check whether an extension is perfect.
   *  <p>An extension is perfect if it can be applied to all base
   *  embeddings. If this is the case, all search tree branches
   *  to the right of the perfect extension can be pruned (partial
   *  perfect extension pruning), since no closed frequent fragment
   *  can be contained in these branches, or even all search tree
   *  branches other than the perfect extension branch can be pruned
   *  (full perfect extension pruning).</p>
   *  <p>In order to avoid certain problems with rings, such pruning
   *  must be restricted to extension edges that are bridges in all
   *  graphs or that close a ring (in all graphs). Ring edges that
   *  do not close a ring can lead to problems if they are part of
   *  rings of different sizes in the underlying graphs and thus are
   *  not considered as candidates for perfect extensions. Furthermore,
   *  if chain extensions are considered, an edge that could be the
   *  start of a chain is also not taken into account as a candidate
   *  for a perfect extension.</p>
   *  <p>If a fragment is found to have resulted from a perfect
   *  extension, it is marked as perfect, so that a second call
   *  to this function does not repeat the checks.</p>
   *  @param  chain whether there are sibling chain extensions
   *  @return whether the extension that created the fragment is perfect
   *  @since  2003.08.01 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isPerfect (boolean chain)
  {                             /* --- check for a perfect extension */
    if ((this.flags & PERFECT) != 0)
      return true;              /* check the perfect extension flag */
    if ((this.base == null)     /* if there is no base structure or */
    ||  (this.size != 0))       /* this is a ring or chain extension, */
      return false;             /* the extension is not perfect */
    if (this.supp[4] != this.base.supp[2] +this.base.supp[3])
      return false;             /* check the embedding counters */
    /* For an extension to be perfect, every embedding of the parent */
    /* fragment must allow for this extension. This is checked by    */
    /* comparing the number of parent embeddings that were extended  */
    /* and the number of embeddings of the parent fragment.          */
    /* Note that this check will fail if the extension edge is not a */
    /* bridge in all underlying database graphs and does not close a */
    /* ring due to the way supp[4] is initialized and updated.       */
    if (chain && ((this.flags & CHAIN) != 0))
      return false;             /* chain starts are not perfect */
    this.flags |= PERFECT;      /* otherwise the extension is perfect */
    return true;                /* return 'extension is perfect' */
  }  /* isPerfect() */

  /*------------------------------------------------------------------*/
  /** Revert the extension information of the fragment.
   *  <p>If a perfect extension is followed as the only branch and
   *  there are search tree branches to the left of this branch (where
   *  "to the left" means "referring to fragments with lexicographically
   *  smaller code words"), the extension information must be reset to
   *  that of the base fragment, so that no fragments are lost.</p>
   *  @since  2003.08.08 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void revert ()
  {                             /* --- revert extension information */
    if ((this.flags & PERFECT) == 0)
      return;                   /* do nothing for non-perfect ext. */
    this.idx = this.base.idx;   /* reset the edge index and the */
    this.src = this.base.src;   /* indices of its source and dest. */
    this.dst = this.base.dst;   /* to those of the base fragment */
    this.flags |= REVERTED;     /* set the reverted flag */
  }  /* revert() */

  
  protected void revert_F (){
    this.idx = this.base.idx;   /* reset the edge index and the */
    this.src = this.base.src;   /* indices of its source and dest. */
    this.dst = this.base.dst;   /* to those of the base fragment */
    this.flags |= REVERTED;     /* set the reverted flag */
  }  /* revert() */
  /*------------------------------------------------------------------*/
  /** Check whether a fragments can have equivalent siblings.
   *  <p>If extensions have been filtered with node orbits, a fragment
   *  can have equivalent siblings only if it is the result of a ring
   *  or chain extension or the result of an edge extension that closes
   *  a ring. If no orbit filtering was carried out, any fragment may
   *  possess equivalent siblings.</p>
   *  @return whether the fragment can have equivalent siblings
   *  @since  2011.02.18 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean equivSiblings ()
  { return (this.flags & SIBLINGS) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether two fragments are equivalent.
   *  <p>Two fragments are equivalent if they refer to the same nodes
   *  and edges, although maybe in a different way. To check this, it
   *  is tried to find an equivalent embedding of the two fragments for
   *  one graph. Technically this is done by marking one embedding
   *  of one fragment in the graph referred to and then checking
   *  whether any embedding of the other fragment into the same
   *  graph is fully marked.</p>
   *  @param  frag the fragment to compare to
   *  @return whether the given fragment is equivalent
   *  @since  2002.07.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isEquivTo (Fragment frag)
  {                             /* --- check for equivalence */
    int       i, k;             /* loop variables */
    Embedding emb, ref;         /* to traverse the embeddings */

    ref = frag.list;            /* get the embeddings of */
    emb = this.list;            /* the fragments to compare */
    if ((ref.graph != emb.graph)/* if basic properties differ */
    ||  (ref.nodes.length != emb.nodes.length)
    ||  (ref.edges.length != emb.edges.length))
      return false;             /* fragments cannot be equivalent */
    for (i = ref.edges.length; --i >= 0; )
      ref.edges[i].mark = 0;    /* mark the embedding in the graph */
    do {                        /* embedding comparison loop */
      for (k = emb.edges.length; --k >= 0; )
        if (emb.edges[k].mark < 0) /* if a edge marker is not set, */
          break;                   /* the embeddings differ */
      if (k < 0) break;         /* if an equiv. embed. found, abort, */
      emb = emb.succ;           /* otherwise go to the next fragment */
    } while ((emb != null)      /* until all embeddings are processed */
    &&       (emb.graph == ref.graph));
    /* Note that it is not necessary to traverse the embeddings with  */
    /* this.firstEmb() and this.nextEmb(), since only the embeddings  */
    /* into the first graph are accessed, which will never be packed. */
    for (i = ref.edges.length; --i >= 0; )
      ref.edges[i].mark = -1;   /* unmark the embedding in graph */
    return (k < 0);             /* return comparison result */
  }  /* isEquivTo() */

  /*------------------------------------------------------------------*/
  /** Create all ring extensions for a given extension edge.
   *  <p>In its current form this function distinguishes extensions by
   *  rings of different size, even if the added edges are the same.
   *  It is the question whether it is worth the effort to remove this
   *  redundancy, as it requires to store more information per ring
   *  edge.</p>
   *  @param  src the index of the source      node of the first edge
   *  @param  re  the first edge of a possible ring extension
   *  @param  dst the index of the destination node of the first edge
   *  @param  buf a buffer for the ring edge information
   *  @param  mna the maximum number of new nodes
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private static ExtList rings (Node src, Edge re, Node dst,
                                int[] buf, int mna)
  {                             /* --- create ring extensions */
    int     i, k, n;            /* loop variables */
    long    all, cur;           /* all ring flags and current one */
    Node    node;               /* to traverse the nodes of the ring */
    Edge    edge, x = null;     /* to traverse the edges of the ring */
    ExtList e,    l = null;     /* list of ring extensions */

    all = re.getRings(); cur = 1;  /* note the ring flags */
    while (all != 0) {          /* while there is another ring flag */
      while ((all & cur) == 0) cur <<= 1;
      all &= ~cur;              /* find and remove the next ring flag */
      edge = re; node = dst;    /* get initial edge and dest. node */
      n = k = 0;                /* initialize the counters */
      do {                      /* traverse the ring */
        if (node.mark < 0) k++; /* count new nodes */
        for (i = node.deg; --i >= 0; ) {
          x = node.edges[i];    /* traverse the edges of the node */
          if (((x.flags & cur) != 0) && (x != edge))
            break;              /* find the next edge */
        }                       /* of the ring to be added */
        if (i < 0) break;       /* if the ring is incomplete, abort */
        edge = x;               /* go to the next edge and node */
        node = (edge.src != node) ? edge.src : edge.dst;
        buf[n++] = edge.type;   /* store the edge type, */
        buf[n++] = node.type;   /* the destination node type, */
        buf[n++] = node.mark;   /* and the dest. node index */
      } while (node != src);    /* while the ring is not closed */
      if ((node != src)         /* check whether the ring was closed */
      ||  (k    > mna))         /* and whether it is small enough */
        continue;               /* if it is not, skip the ring */
      e = new ExtList(src.mark, dst.mark, re.type, dst.type, buf, n);
      e.succ = l; l = e;        /* create a new ring extension and */
    }                           /* add it at the head of the list */
    return l;                   /* return the created extensions */
  }  /* rings() */

  /*------------------------------------------------------------------*/
  /** Check whether there is a matching ring extension.
   *  <p>All ring flags of the given edge are checked whether they
   *  lead to a ring extension matching the one described by the
   *  given extension list element.</p>
   *  @param  e   the extension list element to match
   *  @param  src the index of the source node of the first edge
   *  @param  re  the first edge of a ring extension
   *  @param  dst the index of the destination node of the first edge
   *  @return whether there is a matching ring extension
   *  @since  2005.07.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private static boolean match (ExtList e, Node src, Edge re, Node dst)
  {                             /* --- match ring extensions */
    int   i, n;                 /* loop variables */
    long  all, cur;             /* all ring flags and current one */
    Node  node;                 /* to traverse the nodes of the ring */
    Edge  edge, x = null;       /* to traverse the edges of the ring */

    all = re.getRings(); cur = 1;  /* note the ring flags */
    while (all != 0) {          /* while there is another ring flag */
      while ((all & cur) == 0) cur <<= 1;
      all &= ~cur;              /* find and remove the next ring flag */
      edge = re; node = dst;    /* get initial edge and dest. node */
      n = 0;                    /* initialize the node counter */
      do {                      /* traverse the ring */
        if (n >= e.ring.length) {
          n = -1; break; }      /* check the size of the ring */
        for (i = node.deg; --i >= 0; ) {
          x = node.edges[i];    /* traverse the edges of the node */
          if (((x.flags & cur) != 0) && (x != edge))
            break;              /* find the next edge */
        }                       /* of the ring to be added */
        if (i < 0) break;       /* if the ring is incomplete, abort */
        edge = x;               /* go to the next edge and node */
        node = (edge.src != node) ? edge.src : edge.dst;
        if ((e.ring[n++] != edge.type)
        ||  (e.ring[n++] != node.type)
        ||  (e.ring[n++] != node.mark)) {
          n = -1; break; }      /* check the edge properties */
      } while (node != src);    /* while the ring is not closed */
      if (n >= e.ring.length)   /* check whether all edges */
        return true;            /* of the ring were matched */
    }
    return false;               /* no matching ring found */
  }  /* match() */

  /*------------------------------------------------------------------*/
  /** Set the flag for a closed fragment.
   *  <p>In the search it may be possible to determine, without an
   *  explicit test, that a fragment is not closed, namely if one of
   *  the extensions of the fragment that are considered in the search
   *  has the same support. In this case the fragments closed flag
   *  is cleared, so that the test function <code>isClosed()</code>
   *  returns immediately with a negative result. By default the
   *  closed flag is set for a fragment.</p>
   *  @since  2006.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void setClosed (boolean closed)
  { if (closed) this.flags |= CLOSED; else this.flags &= ~CLOSED; }

  /*------------------------------------------------------------------*/
  /** Check whether the fragment is closed.
   *  <p>A fragment is closed if no superstructure has the same support,
   *  that is, occurs in the same number of graphs in the database.
   *  Closed fragments are not reported.</p>
   *  <p>The basic idea of the test is to form a list of all possible
   *  extensions of the embeddings into the first graph and then
   *  trying to do the same extensions on embeddings into the remaining
   *  graphs. Whenever an extension cannot be done on some embedding,
   *  the extension is removed from the list. That is, the list always
   *  contains the extensions that are possible in all already processed
   *  graphs. If this list gets empty during the process, there is
   *  no extension that is possible in all graphs and consequently
   *  the fragment is closed. If, on the other hand, the list contains
   *  at least one element after all graphs have been processed,
   *  the fragment is not closed, since each of the extensions that
   *  remain in the list can be done in all graphs and thus lead to
   *  a superstructure with the same support.</p>
   *  @param  cnf the canonical form defining
   *              which extensions must be considered
   *  @return whether the fragment is closed
   *  @since  2005.07.23 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isClosed (CanonicalForm cnf)
  {                             /* --- check for a closed fragment */
    int       i, k;             /* loop variables */
    int       mnn;              /* maximal number of new nodes */
    Embedding emb;              /* to traverse the embeddings */
    Graph     cur;              /* to traverse the graphs */
    Node      s, d;             /* to traverse the nodes */
    Edge      edge;             /* edge of previous extension */
    ExtList   l1, l2, l3, e;    /* list of extensions */

    if ((this.flags & CLOSED) == 0)
      return false;             /* check the non-closed flag */

    /* --- collect extensions in first graph --- */
    mnn = cnf.max -this.size(); /* get max. number of new nodes */
    emb = this.firstEmb();      /* and the first embedding */
    cur = emb.graph; l1 = null; /* note graph and init. list */
    do {                        /* extension generation loop */
      emb.index();              /* mark embedding in the graph */
      l2 = null;                /* and init. the embedding list */
      for (i = emb.nodes.length; --i >= 0; ) {
        s = emb.nodes[i];       /* traverse the embedding's nodes */
        for (k = s.deg; --k >= 0; ) {
          edge = s.edges[k];    /* traverse the unmarked edges */
          if (edge.mark >= 0) continue;
          d = (edge.src != s) ? edge.src : edge.dst;
          if ((mnn <= 0) && (d.mark < 0))
            continue;           /* if a new node is not allowed */
          if (!edge.isInRing()  /* if this is not a ring extension */
          ||  ((cnf.mode & (CanonicalForm.RING|Miner.CLOSERINGS)) == 0))
            l3 = e = new ExtList(s.mark, d.mark, edge.type, d.type);
          else {                /* create single edge extension */
            l3 = e = rings(s, edge, d, cnf.word, mnn);
            if (e == null) continue;
            while (e.succ != null) e = e.succ;
          }                     /* create ring extension(s) */
          e.succ = l2; l2 = l3; /* add the new list element(s) */
        }                       /* at the head of the extension list */
      }
      emb.mark(-1);             /* unmark the embedding again */
      if ((cnf.mode & (CanonicalForm.RING|Miner.CLOSERINGS)) != 0)
        l2 = ExtList.sort(l2);  /* sort new extensions if necessary */
      l1 = ExtList.merge(l1,l2);/* merge the extension lists */
      emb = this.nextEmb();     /* (and remove duplicates) */
    } while ((emb != null) && (emb.graph == cur));

    /* --- check extensions in remaining graphs --- */
    while ((emb != null)        /* traverse remaining embeddings */
    &&     (l1  != null)) {     /* while there are extensions left */
      cur = emb.graph;          /* note the next graph */
      l2  = null;               /* clear the waiting list */
      do {                      /* extension match loop */
        emb.index();            /* mark embedding in the graph */
        for (l3 = null; l1 != null; ) {
          e = l1; l1 = e.succ;  /* traverse unmatched extensions */
          s = emb.nodes[e.src]; /* get the extension's source node */
          for (i = s.deg; --i >= 0; ) {
            edge = s.edges[i];  /* traverse the unmarked edges */
            if ((edge.mark >= 0) || (edge.type != e.edge))
              continue;         /* check the edge type */
            d = (edge.src != s) ? edge.src : edge.dst;
            if ((d.type != e.node)
            ||  (d.mark != e.dst))
              continue;         /* check dest. node type and index */
            if ((e.ring == null)
            ||  match(e, s, edge, d))
              break;            /* if there is a matching extension, */
          }                     /* abort the search loop */
          if (i >= 0) { e.succ = l2; l2 = e; }
          else        { e.succ = l3; l3 = e; }
        }                       /* distribute according to match */
        l1 = l3;                /* get unmatched extensions */
        emb.mark(-1);           /* unmark the embedding again */
        emb = this.nextEmb();   /* and go to the next embedding */
      } while ((emb != null) && (emb.graph == cur));
      l1 = l2;                  /* get list of potential extensions */
    }                           /* (may still match all graphs) */
    if (l1 != null)             /* if there are extensions left, */
      this.flags &= ~CLOSED;    /* the fragment is not closed */
    return (l1 == null);        /* return whether fragment is closed */
  }  /* isClosed() */

  /*------------------------------------------------------------------*/
  /** Check whether the fragment is in canonical form.
   *  @param  cnf the canonical form
   *  @return whether the fragment is in canonical form
   *  @since  2005.07.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean isCanonic (CanonicalForm cnf)
  { return this.isCanonic(cnf, false) > 0; }

  /*------------------------------------------------------------------*/
  /** Check whether the fragment is in canonical form.
   *  <p>The test may be executed in a partial form, in which only
   *  the edges up to the last added edge are seen as fixed, while
   *  the rest can be reordered freely to achieve canonical form.</p>
   *  @param  cnf     the canonical form
   *  @param  partial whether to carry out a partial test
   *  @return whether the fragment is in canonical form
   *          <p><table cellpadding=0 cellspacing=0>
   *          <tr><td>-1,&nbsp;</td>
   *              <td>if the fragment differs from the canonical form
   *                  in the fixed edges,</td></tr>
   *          <tr><td align="right">0,&nbsp;</td>
   *              <td>if it is not canonical, but does not differ
   *                  in the fixed edges,</td></tr>
   *          <tr><td>+1,&nbsp;</td>
   *              <td>if the fragment is canonical.</td></tr>
   *          </table></p>
   *  @since  2005.07.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int isCanonic (CanonicalForm cnf, boolean partial)
  {                             /* --- check for canonical form */
    int n = (this.graph != null)
          ?  this.graph.edgecnt : this.list.edges.length;
    if (n <= 0) return 1;       /* check whether there are edges */
    if (this.graph == null)     /* create a graph if necessary */
      this.graph = new Graph(this);
    n = this.graph.isCanonic(cnf, (partial) ? this.idx+1 : n);
    if (n > 0)                  /* if the fragment is canonic, note */
      this.flags |= ORBITS;     /* that orbits have been determined */
    return n;                   /* return the test result */
  }  /* isCanonic() */

  /*------------------------------------------------------------------*/
  /** Reorganize the fragment with a map from a canonical form.
   *  <p>The reorganization consists in reordering the nodes and
   *  edges of the subgraph representing the fragment as well
   *  as reordering the node and edge references of all embeddings.
   *  This function is called in <code>makeCanonic()</code> and in
   *  <code>adapt()</code> to carry out the changes determined by
   *  the corresponding canonical form.</p>
   *  @param  cnf the canonical form containing the map
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void map (CanonicalForm cnf)
  {                             /* --- map fragment after makeCanonic */
    int       in, ie, i;        /* loop variables */
    int       nn, ne;           /* numbers of nodes and edges */
    boolean   nar, ear;         /* flags for array reallocation */
    Embedding emb;              /* to traverse the embeddings */
    Node[]    nsrc;             /* source for node array */
    Edge[]    esrc;             /* source for edge array */

    nn = (this.list != null)    /* get the number of nodes */
       ? this.list.nodes.length : this.graph.nodecnt;
    for (in = 0; in < nn; in++) /* traverse the node map and */
      if (cnf.nmap[in] != in)   /* find the first node to map */
        break;                  /* (first non-identity map) */
    ne = (this.list != null)    /* get the number of edges */
       ? this.list.edges.length : this.graph.edgecnt;
    for (ie = 0; ie < ne; ie++) /* traverse the egde map and */
      if (cnf.emap[ie] != ie)   /* find the first edge to map */
        break;                  /* (first non-identity map) */
    if ((in >= nn) && (ie >= ne)) /* if the mapping is the identity, */
      return;                   /* no adaptation is needed, so abort */
    this.graph.map(cnf);        /* reorganize the representing graph */
    if (this.idx >= 0) {        /* if fragment resulted from an ext. */
      this.idx = cnf.emap[this.idx];
      this.src = cnf.nmap[this.src];
      this.dst = cnf.nmap[this.dst];
    }                           /* map the extension information */
    if (this.list == null)      /* if there are no embeddings, */
      return;                   /* the mapping is completed */
    nar = (in < nn)             /* check whether it is necessary */
       && (this.base != null)   /* to reallocate the node array */
       && (this.base.list.nodes == this.list.nodes);
    ear = (ie < ne)             /* check whether it is necessary */
       && (this.base != null)   /* to reallocate the edge array */
       && (this.base.list.edges == this.list.edges);
    nsrc = cnf.nodes;           /* get default source arrays */
    esrc = cnf.edges;           /* for the reorganization */
    for (emb = this.list; emb != null; emb = emb.succ) {
      if (emb.edges == null)    /* if this is a packed embedding, */
        continue;               /* it can be skipped (no adaptation) */
      if (nn > in) {            /* if not all nodes at right places */
        if (nar) { nsrc = emb.nodes; emb.nodes = new Node[nn]; }
        else System.arraycopy(emb.nodes, in, nsrc, in, nn-in);
        for (i = nn; --i >= in; ) emb.nodes[cnf.nmap[i]] = nsrc[i];
      }                         /* reorder the nodes */
      if (ne > ie) {            /* if not all edges at right places */
        if (ear) { esrc = emb.edges; emb.edges = new Edge[ne]; }
        else System.arraycopy(emb.edges, ie, esrc, ie, ne-ie);
        for (i = ne; --i >= ie; ) emb.edges[cnf.emap[i]] = esrc[i];
      }                         /* reorder the edges */
    }                           /* (reorganize the embeddings) */
  }  /* map() */

  /*------------------------------------------------------------------*/
  /** Make the fragment canonic (minimize the code word).
   *  @param  cnf the canonical form
   *  @return whether the fragment was changed
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean makeCanonic (CanonicalForm cnf)
  { return this.makeCanonic(cnf, -1); }

  /*------------------------------------------------------------------*/
  /** Make the fragment canonic (minimize the code word).
   *  <p>In the process of trying to minimize the code word, the first
   *  <code>keep</code> edges of the fragment are left untouched. Hence
   *  the result may or may not be the overall minimal code word.</p>
   *  @param  cnf  the canonical form
   *  @param  keep the number of edges to keep
   *  @return whether the fragment was changed
   *  @since  2006.05.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean makeCanonic (CanonicalForm cnf, int keep)
  {                             /* --- construct canonical form */
    if (this.list.edges.length <= ((keep < 0) ? 0 : keep))
      return false;             /* check the number of movable edges */
    if (this.graph == null)     /* create a graph if necessary */
      this.graph = new Graph(this);
    if (!this.graph.makeCanonic(cnf, keep))
      return false;             /* make the graph canonic */
    if (keep < 0)               /* if free reordering was allowed, */
      this.flags |= ORBITS;     /* node orbits have been determined */
    this.map(cnf);              /* map the embeddings accordingly */
    return true;                /* return 'fragment is changed' */
  }  /* makeCanonic() */

  /*------------------------------------------------------------------*/
  /** Check whether the fragment has node orbit identifiers.
   *  @return whether the fragment has node orbit identifiers
   *  @since  2011.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean hasOrbits ()
  { return (this.flags & ORBITS) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether the fragment has open rings.
   *  @param  min minimum ring size (number of nodes/edges)
   *  @param  max maximum ring size (number of nodes/edges)
   *  @return whether the fragment has open rings
   *  @since  2006.05.16 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean hasOpenRings (int min, int max)
  { return this.getGraph().hasOpenRings(min, max); }

  /*------------------------------------------------------------------*/
  /** Check whether the fragment has unclosable rings.
   *  <p>If the output is restricted to fragments containing only closed
   *  rings, the restricted extensions (as they can be derived from a
   *  canonical form) render certain nodes unextendable. If such an node
   *  has only one incident ring edge, the ring of which this edge is
   *  part cannot be closed by future extensions. Hence neither this
   *  fragment nor any of its extensions can produce output and thus
   *  it can be pruned.</p>
   *  @param  cnf the canonical form defining the unextendable nodes
   *  @return whether the fragment has unclosable rings
   *  @since  2006.05.16 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean hasUnclosableRings (CanonicalForm cnf)
  { return cnf.hasUnclosableRings(this); }

  /*------------------------------------------------------------------*/
  /** Merge ring extensions that have the same initial edge.
   *  <p>This function does not work with packed embeddings lists due
   *  to ordering problems. However, the fact that the set of embeddings
   *  is reduced would also lead to a considerable loss of information
   *  if embeddings were discarded and recreated by reembedding.</p>
   *  @param  exts the array of ring extension fragments to merge
   *  @param  cnt  the number of fragments to consider
   *  @return the number of fragments in <code>exts</code> after merging
   *  @since  2006.05.16 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int mergeExts (Fragment[] exts, int cnt)
  {                             /* --- merge ring extensions */
    int       i, j, k, n, frst; /* loop variables, buffers */
    int       b, s, d, bt, dt;  /* edge and node indices and types */
    Fragment  res, x;           /* to traverse the extensions */
    Embedding emb, e, c;        /* to traverse the embeddings */
    boolean   found;            /* whether compat. embwdding found */
    Edge[]    bvec;             /* buffer for reallocation */
    Node[]    avec;             /* ditto */

    i = n = 0;                  /* init. the extension indices */
    while (i < cnt) {           /* section finding and merge loop */
      while ((i < cnt) && (exts[i].size <= 0))
        exts[n++] = exts[i++];  /* skip non-ring extensions */
      if (i >= cnt) break;      /* if all extensions processed, abort */
      x  = exts[frst = i++];    /* note the first ring extension */
      s  = x.src;               /* and its defining parameters */
      bt = x.list.edges[b = x.idx].type;
      dt = x.list.nodes[d = x.dst].type;
      while ((i < cnt) && (exts[i].size > 0)
      &&     (exts[i].src                == s)
      &&     (exts[i].dst                == d)
      &&     (exts[i].list.edges[b].type == bt)
      &&     (exts[i].list.nodes[d].type == dt))
        i++;                    /* find corresp. ring extensions */

      /* -- single element section -- */
      if (i -frst <= 1) {       /* if only one extension in section, */
        exts[n++] = x;          /* it can simply be kept and adapted */
        x.size    = 0;          /* set extension type to single edge */
        j         = b+1;        /* compute the new number of edges */
        if (j >= x.list.edges.length) /* if only one edge is added, */
          continue;             /* no fragment adaptation is needed */
        x.graph = null;         /* delete the graph (invalid now) */
        x.ris   = null;         /* and the ring nodes index array */
        k = this.list.nodes.length;
        if (d >= k) k++;        /* compute the new number of nodes */
        if (k >= x.list.nodes.length) k = -1;               
        for (emb = x.list; emb != null; emb = emb.succ) {
          System.arraycopy(emb.edges, 0, bvec = new Edge[j], 0, j);
          emb.edges = bvec;     /* shrink the edge array */
          if (k >= 0) {         /* if there are additional nodes */
            System.arraycopy(emb.nodes, 0, avec = new Node[k], 0, k);
            emb.nodes = avec;   /* shrink the node array */
          }                     /* (length must fit contents) */
          while ((emb.succ != null)
          &&     (emb.succ.common(emb) > this.list.edges.length))
            emb.succ = emb.succ.succ;
        }                       /* remove equivalent embeddings */
        continue;               /* the extension has been adapted, */
      }                         /* so continue with the next */

      /* -- multiple element section -- */
      res = new Fragment(this, b, s, d);   /* create result fragment */
      if (d >= this.list.nodes.length)
        d = -1;                 /* adapt the destination node marker */
      for (emb = this.list; emb != null; emb = emb.succ) {
        for (k = frst; k < i; k++) {
          e = exts[k].list;     /* traverse the extensions */
          if ((e != null) && (e.common(emb) >= this.list.edges.length))
            break;              /* if a compatible embedding */
        }                       /* could be found, abort the loop, */
        if (k >= i) continue;   /* otherwise skip the embedding */
        c = emb.extend(s, d, bt, dt);
        while (c != null) {     /* traverse candidate embeddings */
          found = false;        /* default: no corresp. embedding */
          for (k = frst; k < i; k++) {
            e = exts[k].list;   /* traverse the extensions */
            if ((e == null) || (e.common(emb) < this.list.edges.length)
            ||  (e.edges[b] != c.edges[b]))
              continue;         /* skip incompatible embeddings */
            exts[k--].list = e.succ;
            found = true;       /* remove compatible embeddings */
          }                     /* from all extensions */
          e = c; c = c.succ; e.succ = null;
          if (found) res.addEmb(e);
        }                       /* add compatible single edge */
      }                         /* extension to the result list */
      exts[n++] = res;          /* store the created fragment */
    }                           /* which is the merging result */
    for (i = n; i < cnt; i++)   /* "delete" all other extensions */
      exts[i] = null;           /* from the extension array */
    return n;                   /* return new number of fragments */
  }  /* mergeExts() */

  /*------------------------------------------------------------------*/
  /** Adapt the fragment (limited reordering of the edges and nodes).
   *  <p>For full perfect extension pruning and for combining ring
   *  extensions with canonical form pruning it is necessary to allow
   *  for a (strictly limited) reorganization of a fragment (a certain
   *  reordering of the edges and nodes), so that certain edges, which
   *  may have be added in a wrong order (w.r.t. the canonical form),
   *  are brought into the proper order. The core idea is to split the
   *  list of edges into to parts: a fixed prefix part and a volatile
   *  suffix part. Only the volatile suffix part is adapted in this
   *  function.</p>
   *  @param  cnf   the canonical form
   *  @param  check whether to check for a valid ring order
   *  @return whether the adaptation was successful
   *  @since  2006.04.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean adapt (CanonicalForm cnf, boolean check)
  {                             /* --- adapt a fragment */
    int       i, k, n, e;       /* loop variables, buffers */
    Node[]    nodes;            /* node array of the fragment */
    Node      node;             /* buffer for node to shift */
    Edge[]    edges;            /* edge array of the fragment */
    Edge      edge, x;          /* to traverse the edges */
    Embedding emb;              /* to traverse the embeddings */

    if ((this.flags & ADAPTED) != 0)
      return true;              /* if already adapted, simply abort */
    this.flags |= ADAPTED;      /* set flag for an adapted fragment */
    if (this.size > 0) {        /* if this is a ring extension */
      k = cnf.adaptRing(this, check);    /* adapt the ring extension */
      if (k < 0) return false;  /* if impossible, abort with failure */
      if (k > 0) return true;   /* if no change, abort with success */
      this.map(cnf);            /* reorder the nodes and edges of */
      return true;              /* the graph and the embeddings */
    }                           /* return 'adaptation successful' */
    if (this.idx -this.base.idx <= 1)
      return true;              /* if no movable edges, abort */
    if (this.graph == null)     /* create a graph if necessary */
      this.graph = new Graph(this);
    nodes = this.graph.nodes;     /* traverse the fragment's nodes */
    for (i = this.graph.nodecnt; --i > 0; )
      nodes[i].mark = -1;       /* unmark all nodes and */
    nodes[0].mark = 0;          /* then mark the root node */
    edges = this.graph.edges;   /* traverse the fragment's edges */
    for (k = 1, i = 0; i <= this.base.idx; i++) {
      edge = edges[i];          /* traverse the fixed edges */
      if      (edge.src.mark < 0) edge.src.mark = k++;
      else if (edge.dst.mark < 0) edge.dst.mark = k++;
    }                           /* mark nodes in fixed part */
    x = edges[this.idx];        /* get the newly added edge */
    for (; i < this.idx; i++) { /* and find its proper position */
      edge = edges[i];          /* traverse the movable edges */
      if (((x.src.mark >= 0)    /* if incident to the marked part */
      ||   (x.dst.mark >= 0))   /* and smaller than the next edge */
      &&  (cnf.compareEdge(x, edge, k) < 0))
        break;                  /* abort the position search loop */
      if      (edge.src.mark < 0) edge.src.mark = k++;
      else if (edge.dst.mark < 0) edge.dst.mark = k++;
    }                           /* mark nodes in (now) fixed part */
    if (i >= this.idx)          /* if no shift is necessary, */
      return true;              /* abort the function */
    System.arraycopy(edges, i, edges, i+1, e = this.idx -i);
    edges[i] = x;               /* shift the movable edges */
    if      (x.src.mark < 0) this.src = x.dst.mark;
    else if (x.dst.mark < 0) this.src = x.src.mark;
    else if (x.src.mark < x.dst.mark)
         { this.src = x.src.mark; this.dst = x.dst.mark; k = -1; }
    else { this.src = x.dst.mark; this.dst = x.src.mark; k = -1; }
    n = this.dst -k;            /* adapt the node indices */
    if ((k < 0) || (n <= 0)) {  /* if only to adapt the edges */
      for (emb = this.list; emb != null; emb = emb.succ) {
        if (emb.edges == null)  /* if this is a packed embedding, */
          continue;             /* it can be skipped */
        edge = emb.edges[this.idx]; /* get the edge added last */
        System.arraycopy(emb.edges, i, emb.edges, i+1, e);
        emb.edges[i] = edge;    /* shift the added edge */
      } }                       /* to its proper position */
    else {                      /* if to adapt edges and nodes */
      node = nodes[this.dst];   /* get the node added last */
      System.arraycopy(nodes, k, nodes, k+1, n);
      nodes[k] = node;          /* shift the added node */
      for (emb = this.list; emb != null; emb = emb.succ) {
        if (emb.edges == null)  /* if this is a packed embedding, */
          continue;             /* it can be skipped */
        edge = emb.edges[this.idx]; /* get the edge added last */
        System.arraycopy(emb.edges, i, emb.edges, i+1, e);
        emb.edges[i] = edge;    /* shift the added edge */
        node = emb.nodes[this.dst]; /* get the node added last */
        System.arraycopy(emb.nodes, k, emb.nodes, k+1, n);
        emb.nodes[k] = node;    /* shift the added node */
      }                         /* to its proper position */
      this.dst = k;             /* and set the new index */
    }                           /* of the destination node */
    k = this.idx; this.idx = i; /* note old edge index and set new */
    return true;                /* return 'adaptation successful' */
  }  /* adapt() */

  /*------------------------------------------------------------------*/
  /** Check whether all variable length chains are valid.
   *  <p>A variable length chain is valid if it actually represents
   *  chains of different length in the underlying graphs. If, however,
   *  a chain has the same length in all underlying graphs, it is not
   *  valid, as it could be represented explicitly.</p>
   *  @return whether all variable length chains are valid
   *  @since  2006.11.03 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected boolean chainsValid ()
  {                             /* --- check variable length chains */
    int       i, k, c;          /* loop variables */
    int       n;                /* number of chains / buffer size */
    int[]     buf;              /* buffer for the chain information */
    Embedding emb;              /* to traverse the embeddings */
    Edge      e;                /* to traverse the edges */
    Node      s, d;             /* to traverse the chain atoms */

    n = this.chcnt;             /* get the number of chains */
    if (n <= 0) return true;    /* if there are no chains, abort */
    k = this.supp[2] +this.supp[3];
    if (k <= 1)                 /* if there is only one embedding, */
      return false;             /* the chains are all unique */
    buf = new int[n *= 4];      /* create buffer for chain info. */
    /* The chain information buffer will contain in the field    */
    /* buf[4k  ]: the number of nodes in the k-th chain,         */
    /* buf[4k+1]: the index of the node before the k-th chain,   */
    /* buf[4k+2]: the index of the first edge of the k-th chain, */
    /* buf[4k+3]: the index of the node after the k-th chain.    */
    /* A chain is valid if its minimum length is 1 and there are */
    /* at least two different chain lengths. Hence the number in */
    /* buf[4k] and the current counting result allow a decision. */

    /* --- process the first embedding --- */
    emb = this.firstEmb();      /* get the first embedding */
    for (i = emb.nodes.length; --i >= 0; )
      emb.nodes[i].mark = i;    /* mark the nodes of the embedding */
    for (k = n, i = emb.edges.length; --i >= 0; ) {
      e = emb.edges[i];         /* traverse the embedding's edges */
      if ((e.src.mark >= 0) && (e.dst.mark >= 0))
        continue;               /* skip non-chain edges */
      d = (e.src.mark < 0) ? e.dst : e.src;
      buf[--k] = d.mark;        /* note index of node before chain */
      e = emb.edges[--i];       /* get the first chain edge */
      buf[--k] = i;             /* and note its index */
      s = (e.src.mark < 0) ? e.dst : e.src;
      buf[--k] = s.mark;        /* note index of node after chain */
      for (c = 0; true; ) {     /* traverse the chain */
        s = (e.src != s) ? e.src : e.dst;
        if (s == d) break;      /* if at end of chain, abort loop */
        c++;                    /* count the chain node */
        e = s.edges[(s.edges[0] != e) ? 0 : 1];
      }                         /* get the next chain edge */
      buf[--k] = c;             /* note the number of chain nodes */
    }
    for (i = emb.nodes.length; --i >= 0; )
      emb.nodes[i].mark = -1;   /* unmark the nodes of the embedding */

    /* --- process the remaining embeddings --- */
    for (emb = this.nextEmb(); emb != null; emb = this.nextEmb()) {
      for (i = n, k = n = 0; k < i; k += 4) {
        s = emb.nodes[buf[k+1]];/* traverse the remaining chains */
        e = emb.edges[buf[k+2]];/* for each chain get the nodes */
        d = emb.nodes[buf[k+3]];/* and the first chain edge */
        for (c = 0; true; ) {   /* traverse the chain */
          s = (e.src != s) ? e.src : e.dst;
          if (s == d) break;    /* if at end of chain, abort loop */
          c++;                  /* count the chain node */
          e = s.edges[(s.edges[0] != e) ? 0 : 1];
        }                       /* get the next chain edge */
        if (((buf[k] >  1) && (c == 1))
        ||  ((buf[k] == 1) && (c >  1)))
          continue;             /* skip valid chains */
        if (n != k) System.arraycopy(buf, k, buf, n, 4);
        n += 4;                 /* collect all chains that have */
      }                         /* not yet been found to be valid */
      if (n <= 0) return true;  /* if no chains remain, */
    }                           /* all chains are valid */
    return false;               /* there is at least 1 invalid chain */
  }  /* chainsValid() */

  /*------------------------------------------------------------------*/
  /** Compare the (canonical) code words of two fragments.
   *  <p>This function determines whether the code words of two
   *  fragments (the fragment the function is called on and the
   *  argument fragment), as they are implicitly represented by
   *  the order of their nodes and edges, are equal.</p>
   *  @param  frag the fragment to compare to
   *  @return whether the (canonical) code words are equal
   *  @since  2011.02.18 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean equalsCanonic (Fragment frag)
  { return this.getGraph().equalsCanonic(frag.getGraph()); }

  /*------------------------------------------------------------------*/
  /** Create a string description of the fragment.
   *  @return a string description of the fragment
   *  @since  2002.03.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String toString ()
  { return this.getGraph().toString(); }

  /*------------------------------------------------------------------*/
  /** Create a string description of the fragment.
   *  @param  ntn the notation to use for the description
   *  @return a string description of the fragment
   *  @since  2002.03.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String toString (Notation ntn)
  { return this.getGraph().toString(ntn); }

  /*------------------------------------------------------------------*/
  /** Create the code word of the fragment.
   *  @param  cnf the canonical form defining the code word form
   *  @return a string description of the code word
   *  @since  2006.05.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String toString (CanonicalForm cnf)
  { return this.getGraph().toString(cnf); }

  /*------------------------------------------------------------------*/
  /** Main function for testing some basic functionality.
   *  <p>It is tried to parse the first two command line arguments as
   *  SMILES, SLN, or LiNoG descriptions of graph (in this order).
   *  If parsing suceeds, the second graph is embedded into the first
   *  and the number of embeddings is printed..</p>
   *  @param  args the command line arguments
   *  @since  2007.10.25 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static void main (String args[])
  {                             /* --- main function for testing */
    Notation ntn;               /* graph notation */
    Graph    graph, sub;        /* created graph and subgraph */
    Fragment frag;              /* created fragment */
    int[]    masks;             /* masks for node and edge types */

    if (args.length != 2) {     /* if wrong number of arguments */
      System.err.println("usage: java moss.Fragment <graph> <frag>");
      return;                   /* print a usage message */
    }                           /* and abort the program */

    masks    = new int[4];      /* create node and edge masks */
    masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
    masks[1] = masks[3] = BondTypeMgr.BONDMASK;

    try {                       /* try SMILES */
      System.out.print("SMILES: ");
      ntn   = new SMILES();     /* create a SMILES object */
      graph = ntn.parse(new StringReader(args[0]));
      sub   = ntn.parse(new StringReader(args[1]));
      graph.maskTypes(masks);   /* parse the arguments and */
      sub.maskTypes(masks);     /* mask node and edge types */
      frag  = new Fragment(graph, sub);
      System.out.println(frag.getEmbCount() +" embedding(s)");
      return; }                 /* show the number of embeddings */
    catch (IOException e) {     /* catch parse errors */
      System.err.println(e.getMessage()); }

    try {                       /* try SYBYL line notation (SLN) */
      System.out.print("SLN   : ");
      ntn   = new SLN();        /* create an SLN object */
      graph = ntn.parse(new StringReader(args[0]));
      sub   = ntn.parse(new StringReader(args[1]));
      graph.maskTypes(masks);   /* parse the arguments and */
      sub.maskTypes(masks);     /* mask node and edge types */
      frag  = new Fragment(graph, sub);
      System.out.println(frag.getEmbCount() +" embedding(s)");
      return; }                 /* show the number of embeddings */
    catch (IOException e) {     /* catch parse errors */
      System.err.println(e.getMessage()); }

    try {                       /* try general line notation */
      System.out.print("LiNoG : ");
      ntn   = new LiNoG();      /* create a LiNoG object */
      graph = ntn.parse(new StringReader(args[0]));
      sub   = ntn.parse(new StringReader(args[1]));
      /* no type masking */     /* parse the arguments */
      frag  = new Fragment(graph, sub);
      System.out.println(frag.getEmbCount() +" embedding(s)");
      return; }                 /* show the number of embeddings */
    catch (IOException e) {     /* catch parse errors */
      System.err.println(e.getMessage()); }
  }  /* main() */

}  /* class Fragment */
