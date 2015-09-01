/*----------------------------------------------------------------------
  File    : Graph.java
  Contents: Attributed graph management for substructure mining
  Author  : Christian Borgelt
  History : 2002.03.11 file created (as Molecule.java)
            2002.03.13 output method added
            2002.03.14 memory optimization added
            2002.03.21 second function embed() added (from Miner.java)
            2002.04.14 functions to set graph markers added
            2003.07.31 functions prune(), rings(), and markRings() added
            2003.08.04 graph duplication function added (for debug)
            2003.08.07 adapted to new classes, some rewriting
            2005.06.07 bridge finding functions added
            2005.07.20 pre-masking of nodes and edges added
            2005.07.21 encoding and decoding of nodes added
            2005.07.22 triming of excluded nodes added
            2005.08.03 embedding replaced by special recursive function
            2005.08.04 matching of ring edges in embedding added
            2005.08.06 bug in function embed() fixed (single node)
            2005.08.16 embedding optimized (now based on edges)
            2006.05.03 function makeCanonic() added
            2006.05.11 function isCanonic() added (from Fragment.java)
            2006.05.12 edge type flag RING set in function rings()
            2006.06.01 function isCanonic() extended, result changed
            2006.06.04 function markPseudo() added
            2006.06.06 adapted to changed type of edge flags
            2006.06.26 return value added to function trim()
            2006.06.28 embedding functions redesigned
            2006.08.09 ignoring the node type only in rings added
            2006.08.12 parsing and output moved to Notation classes
            2006.10.24 reorganization of isCanonic() and makeCanonic()
            2006.10.26 ring flag counting improved
            2006.10.27 embedding of substructures with chains added
            2006.10.29 bugs in chain embedding fixed
            2006.10.31 renamed to Graph.java (for generalization)
            2006.11.02 functions hashCode() and equals() added
            2006.11.06 function hashCode() tuned (better distinction)
            2006.11.07 function hashCode() tuned (incident edges)
            2006.11.08 final tuning of hashCode() (combination ops)
            2006.11.10 node degree used to speed up embedding
            2006.11.17 graph cloning improved (using node markers)
            2007.03.04 adapted to modified notation class
            2007.03.19 function normalize(CanonicalForm) added
            2007.03.23 bug in function normalize() fixed (nodes/edges)
            2007.06.12 bug in function isConnected() fixed (nodecnt)
            2007.06.20 functions getNodeType() and getEdgeType() added
            2007.06.21 adapted to type managers (atoms and bonds)
            2007.06.29 added a notation (access to type managers)
            2007.06.30 functions getNodeMgr() and getEdgeMgr() added
            2007.08.09 check for trimmed nodes added to mark(), index()
            2007.10.25 general line notation parsing added
            2007.11.07 indication of ring marking failure added
            2007.11.08 prepareEmbed(), embed() and maskTypes() improved
            2009.04.30 function hashCode() improved
            2009.05.04 function Graph(Fragment) improved
            2010.01.19 bug in containment test fixed (always true)
            2011.02.18 function equalsCanonic() added
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.StringReader;

/*--------------------------------------------------------------------*/
/**
 * Class to represent attributed graphs for substructure mining.
 * <p>
 * An attributed graph is stored as an array of nodes and an array of edges.
 * There is an optional recoder by which the node types may be mapped to other
 * codes to speed up processing.
 * </p>
 * 
 * @author Christian Borgelt
 * @since 2002.03.11
 */
/*--------------------------------------------------------------------*/
public class Graph implements Cloneable {

	/*------------------------------------------------------------------*/
	/* constants */
	/*------------------------------------------------------------------*/
	/**
	 * a dummy parameter/return value for the containment check (saves a
	 * recursion parameter)
	 */
	private static final Embedding CONTAINED = new Embedding();

	/*------------------------------------------------------------------*/
	/* instance variables */
	/*------------------------------------------------------------------*/
	/** the array of nodes (may be only partially used) */
	protected Node[] nodes;
	/**
	 * the current number of nodes (may be smaller than the array length)
	 */
	protected int nodecnt;
	/** the array of edges (may be only partially used) */
	protected Edge[] edges;
	/**
	 * the current number of edges (may be smaller than the array length)
	 */
	protected int edgecnt;
	/** an optional node type recoder */
	public Recoder coder;
	/** the notation for describing the graph */
	protected Notation ntn;
	
	protected double weight=1.0; //add by Shirui, universum graph, p
	  /**weight of a graph, added by Shirui*/
	  
	protected double weight2=1.0; //for universum graphs, b

	/*------------------------------------------------------------------*/
	/**
	 * Create/Initialize an empty graph.
	 * <p>
	 * This constructor is needed internally for turning graphs into named
	 * graphs.
	 * </p>
	 * 
	 * @since 2007.06.29 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Graph() {
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a (empty) graph with default array sizes.
	 * 
	 * @param ntn
	 *            the notation of the graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Graph(Notation ntn) {
		this(ntn, null, 16, 16);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a (empty) graph with default array sizes.
	 * 
	 * @param ntn
	 *            the notation of the graph
	 * @since 2010.01.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Graph(Notation ntn, Recoder coder) {
		this(ntn, coder, 16, 16);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a (empty) graph with the given array sizes.
	 * <p>
	 * The graph is created in such a way that it can contain, without resizing
	 * any arrays, <code>nodecnt</code> nodes and <code>edgecnt</code> edges.
	 * Nevertheless more nodes and edges may be added later. The parameters only
	 * serve the purpose to set the proper sizes if they are already known in
	 * advance.
	 * </p>
	 * 
	 * @param ntn
	 *            the notation of the graph
	 * @param nodecnt
	 *            the expected number of nodes
	 * @param edgecnt
	 *            the expected number of edges
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Graph(Notation ntn, int nodecnt, int edgecnt) {
		this(ntn, null, nodecnt, edgecnt);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a (empty) graph with the given array sizes.
	 * <p>
	 * The graph is created in such a way that it can contain, without resizing
	 * any arrays, <code>nodecnt</code> nodes and <code>edgecnt</code> edges.
	 * Nevertheless more nodes and edges may be added later. The parameters only
	 * serve the purpose to set the proper sizes if they are already known in
	 * advance.
	 * </p>
	 * 
	 * @param ntn
	 *            the notation of the graph
	 * @param coder
	 *            the node type recoder of the graph
	 * @param nodecnt
	 *            the expected number of nodes
	 * @param edgecnt
	 *            the expected number of edges
	 * @since 2010.01.21/2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Graph(Notation ntn, Recoder coder, int nodecnt, int edgecnt) { /*
																		 * ---
																		 * create
																		 * an
																		 * empty
																		 * graph
																		 */
		this.nodes = new Node[nodecnt]; /* allocate arrays for nodes */
		this.edges = new Edge[edgecnt]; /* and for edges and */
		this.nodecnt = this.edgecnt = 0; /* clear the counters */
		this.ntn = ntn; /* store the graph notation */
		this.coder = coder; /* and the node type recoder */
	} /* Graph() */

	/*------------------------------------------------------------------*/
	/**
	 * Create a clone of a graph.
	 * <p>
	 * This function creates a clone without changing anything in the original
	 * graph (not even the node markers, which are only changed temporarily and
	 * restored on completion).
	 * </p>
	 * 
	 * @param graph
	 *            the graph to clone
	 * @since 2003.08.04 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Graph(Graph graph) {
		this(graph, -1, -1, 0, 0);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create an extended graph (extended by one edge).
	 * <p>
	 * This function creates a clone without changing anything in the original
	 * graph (not even the node markers, which are only changed temporarily and
	 * restored on completion).
	 * </p>
	 * 
	 * @param graph
	 *            the graph to clone
	 * @param src
	 *            the source node index of the edge to add
	 * @param dst
	 *            the destination node index of the edge to add
	 * @param edge
	 *            the edge type of the edge to add
	 * @param node
	 *            the destination node type of the edge to add
	 * @since 2010.01.23/2003.08.04 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Graph(Graph graph, int src, int dst, int edge, int node) { /*
																		 * ---
																		 * create
																		 * an
																		 * extended
																		 * graph
																		 */
		int i, d; /* loop variable, node degree */
		Node ns, nd; /* to traverse the nodes */
		Edge es, ed; /* to traverse the edges */

		this.ntn = graph.ntn; /* copy the notation and the recoder */
		this.coder = graph.coder; /* and the graph's nodes */
		this.nodecnt = graph.nodecnt + ((dst >= graph.nodecnt) ? 1 : 0);
		this.nodes = new Node[this.nodecnt];
		for (i = graph.nodecnt; --i >= 0;) {
			ns = graph.nodes[i]; /* traverse the nodes of the graph */
			d = ns.deg;
			if ((i == src) || (i == dst))
				d++;
			this.nodes[i] = nd = new Node(ns.type, d);
			nd.mark = ns.mark; /* create new node of the same type */
			ns.mark = i; /* and mark old node by its index */
		} /* (needed for fast edge cloning) */
		this.edgecnt = graph.edgecnt + ((src >= 0) ? 1 : 0);
		this.edges = new Edge[this.edgecnt];
		for (i = graph.edgecnt; --i >= 0;) {
			es = graph.edges[i]; /* traverse the edges of the graph */
			this.edges[i] = ed = new Edge(this.nodes[es.src.mark],
					this.nodes[es.dst.mark], es.type, es.flags);
			ed.mark = es.mark; /* create new edge of the same type */
		} /* and copy ring and bridge markers */
		if (src >= 0) { /* if to add an edge */
			if (dst >= graph.nodecnt) /* if to add a node, create one */
				this.nodes[graph.nodecnt] = new Node(node);
			ns = this.nodes[src]; /* get the source and */
			nd = this.nodes[dst]; /* destination node */
			this.edges[graph.edgecnt] = new Edge(ns, nd, edge);
		} /* create and add a new edge */
		for (i = graph.nodecnt; --i >= 0;)
			graph.nodes[i].mark = this.nodes[i].mark;
	} /* Graph() *//* restore the node markers */

	/*------------------------------------------------------------------*/
	/**
	 * Turn a fragment into a graph.
	 * <p>
	 * This function is needed for unembedding/reembedding and the functions
	 * <code>isCanonic()</code> and <code>makeCanonic()</code> in the class
	 * <code>Fragment</code>. Turning a fragment into a graph does not change
	 * anything in any underlying graph (not even the node markers, which are
	 * only changed temporarily in one underlying graph and restored on
	 * completion).
	 * </p>
	 * 
	 * @param frag
	 *            the fragment to turn into a graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Graph(Fragment frag) { /* --- turn fragment into a graph */
		int i, k; /* loop variables, node counter */
		Embedding emb; /* to access an embbeding */
		Edge edge; /* to traverse/access the edges */
		Node src, dst; /* to traverse/access the nodes */
		int[] m; /* buffer for node markers */

		emb = frag.list; /* copy the embedding information */
		this.ntn = emb.graph.ntn;
		this.coder = emb.graph.coder;
		this.nodecnt = emb.nodes.length + frag.chcnt;
		this.nodes = new Node[this.nodecnt];
		this.edgecnt = emb.edges.length;
		this.edges = new Edge[this.edgecnt];
		if (frag.chcnt <= 0) { /* if there are no chains */
			for (i = emb.nodes.length; --i >= 0;) {
				src = emb.nodes[i]; /* traverse nodes of the embedding */
				this.nodes[i] = dst = new Node(src.type);
				dst.mark = src.mark; /* create new node of the same type */
				src.mark = i; /* and mark old node by its index */
			} /* (needed for fast edge cloning) */
			for (i = emb.edges.length; --i >= 0;) {
				edge = emb.edges[i]; /* traverse and copy the edges */
				this.edges[i] = new Edge(this.nodes[edge.src.mark],
						this.nodes[edge.dst.mark], edge.type, edge.flags);
			} /* add the edges to the graph */
			for (i = emb.nodes.length; --i >= 0;) {
				emb.nodes[i].mark = this.nodes[i].mark;
			}
		} /* restore the emb's. node markers */
		else { /* if there are chains */
			for (m = new int[i = emb.graph.nodecnt]; --i >= 0;) {
				src = emb.graph.nodes[i];
				m[i] = src.mark; /* buffer all node markers */
				src.mark = -1; /* of the underlying graph */
			} /* and then clear them */
			for (i = emb.nodes.length; --i >= 0;)
				emb.nodes[i].mark = -2; /* unmark all nodes of the embedding */
			src = emb.nodes[0]; /* mark and then copy */
			src.mark = 0; /* the root node of the embedding */
			this.nodes[0] = new Node(src.type);
			for (k = i = 0; i < emb.edges.length; i++) {
				edge = emb.edges[i]; /* traverse the embedding edges */
				src = null; /* and check for "unsaturated" ones */
				if (edge.src.mark < -1)
					this.nodes[edge.src.mark = ++k] = new Node(edge.src.type);
				else if (edge.src.mark < 0)
					src = edge.src; /* note a possible chain node */
				if (edge.dst.mark < -1)
					this.nodes[edge.dst.mark = ++k] = new Node(edge.dst.type);
				else if (edge.dst.mark < 0)
					src = edge.dst; /* note a possible chain node */
				if (src == null)
					continue; /* skip "saturated" edges */
				src.mark = ++k; /* mark the node at the one end, */
				edge = emb.edges[++i]; /* then get the other chain edge */
				if (edge.src.mark == -1)
					src = edge.src;
				else if (edge.dst.mark == -1)
					src = edge.dst;
				src.mark = k; /* mark the node at the other end */
				this.nodes[k] = new Node(Node.CHAIN | src.type);
			} /* create a special pseudo-node */
			for (i = 0; i < this.edgecnt; i++) {
				edge = emb.edges[i]; /* traverse and copy the edges */
				this.edges[i] = new Edge(this.nodes[edge.src.mark],
						this.nodes[edge.dst.mark], edge.type, edge.flags);
			} /* add the edges to the graph */
			for (i = emb.graph.nodecnt; --i >= 0;)
				emb.graph.nodes[i].mark = m[i];
		} /* restore the graph's node markers */
		this.mark(-1); /* unmark all nodes and edges */
		this.opt(); /* of the created graph and */
	} /* Graph() *//* optimize the memory usage */

	/*------------------------------------------------------------------*/
	/**
	 * Create a clone of the graph.
	 * <p>
	 * This function simply returns <code>new Graph(this)</code>. It is intended
	 * mainly for debugging purposes.
	 * </p>
	 * 
	 * @return a clone of this graph
	 * @see #Graph(Graph)
	 * @since 2006.10.23 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Object clone() {
		return new Graph(this);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Clear a graph (remove all nodes and edges).
	 * 
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void clear() {
		this.nodecnt = this.edgecnt = 0;
		this.coder = null;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Check whether another graph is equal to this graph.
	 * <p>
	 * For this function to work properly, the argument graph must either have
	 * been processed with the function <code>prepareEmbed()</code>, or it must
	 * have been created from a fragment that itself was generated in the search
	 * process, so that the edges are sorted in a specific way (so that source
	 * nodes are always already numbered). In addition, the graph to which the
	 * argument graph is compared (object on which this function is called) must
	 * be prepared by calling the function <code>prepare()</code> on it.
	 * </p>
	 * 
	 * @param graph
	 *            the graph to compare to
	 * @return whether the given graph is equal to this graph
	 * @see #contains(Graph)
	 * @since 2006.11.02 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean equals(Object graph) { /* --- compare two graphs */
		if (graph == this)
			return true;
		if (graph == null)
			return false;
		/* In principle, there should be a check here whether */
		/* the variable 'graph' actually points to a Graph. */
		if ((this.nodecnt != ((Graph) graph).nodecnt)
				|| (this.edgecnt != ((Graph) graph).edgecnt)
				|| (this.hashCode() != ((Graph) graph).hashCode()))
			return false; /* compare counters and hash code */
		return this.contains((Graph) graph);
	} /* equals() */

	/*------------------------------------------------------------------*/
	/**
	 * Compute the hash code of the attributed graph.
	 * <p>
	 * For subgraphs this function should yield the same value as the
	 * corresponding function <code>Embedding.hashCode()</code>.
	 * </p>
	 * <p>
	 * Note that the hash value of a graph changes when the node types are
	 * encoded with a <code>Recoder</code>.
	 * </p>
	 * 
	 * @return the hash code of the graph
	 * @since 2006.11.02 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int hashCode() { /* --- compute a hash code */
		int i, k, t, u; /* loop variables, buffers */
		int h, s; /* the computed hash code */
		Node src, dst; /* to traverse the nodes */
		Edge edge; /* to traverse the edges */

		h = s = 0; /* initialize the hash values */
		for (i = this.nodecnt; --i >= 0;) {
			src = this.nodes[i]; /* traverse the nodes */
			if (src.isChain())
				continue;
			t = src.type + src.deg;
			for (k = src.deg; --k >= 0;) {
				edge = src.edges[k]; /* traverse the incident edges */
				dst = (edge.src != src) ? edge.src : edge.dst;
				u = (dst.type & ~Node.CHAIN) ^ dst.deg;
				t += src.type ^ (u + edge.type);
			} /* combine node types and degrees */
			h ^= t ^ (t << 9) ^ (t << 15);
			s += t; /* combine the computed values */
		} /* in two different ways */
		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse the edges */
			t = (edge.src.type & ~Node.CHAIN) ^ edge.src.deg;
			u = (edge.dst.type & ~Node.CHAIN) ^ edge.dst.deg;
			t = t + u + (t & u) + (t | u) + (t ^ u);
			h ^= t ^ (t << 11) ^ (t << 19);
			s += t += edge.type; /* combine node types and degrees */
			h ^= t ^ (t << 7) ^ (t << 17);
		} /* incorporate the edge type */
		h ^= this.nodecnt ^ this.edgecnt;
		s += this.nodecnt + this.edgecnt;
		h ^= s ^ (s << 15); /* combine the two hash codes */
		if (h < 0)
			h ^= -1; /* ensure a positive hash value */
		return h; /* return the computed hash code */
	} /* hashCode() */

	/*------------------------------------------------------------------*/
	/**
	 * Get the notation of the graph.
	 * 
	 * @return the notation of the graph
	 * @since 2007.06.29 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Notation getNotation() {
		return this.ntn;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get the node type manager of the graph.
	 * 
	 * @return the node type manager of the graph
	 * @since 2007.06.30 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public TypeMgr getNodeMgr() {
		return this.ntn.getNodeMgr();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get the edge type manager of the graph.
	 * 
	 * @return the edge type manager of the graph
	 * @since 2007.06.30 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public TypeMgr getEdgeMgr() {
		return this.ntn.getEdgeMgr();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get the recoder for the node types.
	 * 
	 * @return the recoder currently used for the node types, or
	 *         <code>null</code> if no recoder is used
	 * @see #encode(Recoder)
	 * @since 2006.06.28 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Recoder getRecoder() {
		return this.coder;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Add a node to the graph, do not encode the type.
	 * <p>
	 * Even if the graph contains a node type recoder, the node type is stored
	 * exactly as it is given.
	 * </p>
	 * 
	 * @param type
	 *            the type of the node to add
	 * @return the index of the node in the graph
	 * @see #addNodeRaw(int)
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int addNodeRaw(int type) { /* --- add a node to a graph */
		Node[] old = this.nodes; /* buffer for the old node array */
		int max = old.length; /* (new) array size */

		if (this.nodecnt >= max) { /* if the node array is full */
			this.nodes = new Node[max + ((max > 8) ? max >> 1 : 8)];
			System.arraycopy(old, 0, this.nodes, 0, this.nodecnt);
		} /* enlarge the array and copy nodes */
		this.nodes[this.nodecnt] = new Node(type);
		return this.nodecnt++; /* add a new node to the array and */
	} /* addNodeRaw() *//* return the index of the new node */

	/*------------------------------------------------------------------*/
	/**
	 * Add a node to the graph, encode type if needed.
	 * <p>
	 * If the graph contains a node type recoder, the given node type is
	 * automatically encoded with it.
	 * </p>
	 * 
	 * @param type
	 *            the type of the node to add
	 * @return the index of the node in the graph
	 * @see #addNode(int)
	 * @since 2010.01.19 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int addNode(int type) { /* --- add a node to a graph */
		if (this.coder != null)
			type = this.coder.encode(type);
		return this.addNodeRaw(type);
	} /* addNode() *//* encode the node type if needed */

	/*------------------------------------------------------------------*/
	/**
	 * Get the number of nodes of the graph.
	 * 
	 * @return the number of nodes of the graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int getNodeCount() {
		return this.nodecnt;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get a node by its index.
	 * 
	 * @param index
	 *            the index of the node to retrieve
	 * @return the <code>index</code>-th node of the graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Node getNode(int index) {
		return this.nodes[index];
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get the type of a node.
	 * <p>
	 * If the graph contains a recoder for the node types, the node type is
	 * automatically decoded.
	 * </p>
	 * 
	 * @param index
	 *            the index of the node
	 * @return the type of the <code>index</code>-th node of the graph
	 * @see #getNodeTypeRaw(int)
	 * @since 2007.06.20 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int getNodeType(int index) { /* --- get the type of a node */
		Node node = this.nodes[index];
		if (node.isSpecial())
			return node.type;
		return (this.coder == null) /* decode the type if necessary */
		? node.type : this.coder.decode(node.type);
	} /* getNodeType() */

	/*------------------------------------------------------------------*/
	/**
	 * Get the type of a node.
	 * <p>
	 * Even if the graph contains a recoder for the node types, the node type is
	 * not decoded, but returned as it is stored.
	 * </p>
	 * 
	 * @param index
	 *            the index of the node
	 * @return the type of the <code>index</code>-th node of the graph
	 * @see #getNodeType(int)
	 * @since 2010.01.19 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int getNodeTypeRaw(int index) {
		return this.nodes[index].type;
	} /* --- get the type of a node */

	/*------------------------------------------------------------------*/
	/**
	 * Add an edge to the graph.
	 * 
	 * @param src
	 *            the index of the source node of the edge
	 * @param dst
	 *            the index of the destination node of the edge
	 * @param type
	 *            the type of the edge to add
	 * @return the index of the edge in the graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int addEdge(int src, int dst, int type) { /*
													 * --- add an edge to a
													 * graph
													 */
		Edge[] old = this.edges; /* buffer for the old edge array */
		int max = old.length; /* (new) array size */

		if (this.edgecnt >= max) { /* if the edge array is full */
			this.edges = new Edge[max + ((max > 8) ? max >> 1 : 8)];
			System.arraycopy(old, 0, this.edges, 0, this.edgecnt);
		} /* enlarge the array and copy edges */
		this.edges[this.edgecnt] = /* add a new edge to the array */
		new Edge(this.nodes[src], this.nodes[dst], type);
		return this.edgecnt++; /* return the index of the new edge */
	} /* addEdge() */

	/*------------------------------------------------------------------*/
	/**
	 * Get the number of edges of the graph.
	 * 
	 * @return the number of edges of the graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int getEdgeCount() {
		return this.edgecnt;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get an edge by its index.
	 * 
	 * @param index
	 *            the index of the edge to retrieve
	 * @return the <code>index</code>-th edge of the graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Edge getEdge(int index) {
		return this.edges[index];
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get the type of an edge.
	 * <p>
	 * Since edge types are currently not encoded, there is no need to
	 * distinguish <code>getEdgeType</code> and <code>getEdgeTypeRaw</code>.
	 * 
	 * @param index
	 *            the index of the edge
	 * @return the type of the <code>index</code>-th edge of the graph
	 * @since 2007.06.20 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int getEdgeType(int index) {
		return this.edges[index].type;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Optimize memory usage.
	 * <p>
	 * This function shrinks the node array and the edge array to the minimal
	 * size that is necessary to hold the current number of nodes and edges and
	 * thus tries to reduce the memory consumption.
	 * </p>
	 * 
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected void opt() { /* --- optimize memory usage */
		for (int i = this.nodecnt; --i >= 0;)
			this.nodes[i].opt(); /* optimize the nodes */
		if (this.nodes.length > this.nodecnt) {
			Node[] old = this.nodes; /* create an array of the right size */
			this.nodes = new Node[this.nodecnt];
			System.arraycopy(old, 0, this.nodes, 0, this.nodecnt);
		} /* copy the existing nodes */
		if (this.edges.length > this.edgecnt) {
			Edge[] old = this.edges; /* create an array of the right size */
			this.edges = new Edge[this.edgecnt];
			System.arraycopy(old, 0, this.edges, 0, this.edgecnt);
		} /* copy the existing edges */
	} /* opt() */

	/*------------------------------------------------------------------*/
	/**
	 * Mark all nodes and edges with a given value.
	 * 
	 * @param mark
	 *            the value with which to mark nodes and edges.
	 * @since 2002.04.14 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void mark(int mark) { /* --- set all markers */
		for (int i = this.nodecnt; --i >= 0;)
			if (this.nodes[i].mark >= -1) /* mark the nodes of the graph */
				this.nodes[i].mark = mark; /* that have not been trimmed */
		for (int i = this.edgecnt; --i >= 0;)
			if (this.edges[i].mark >= -1) /* mark the edges of the graph */
				this.edges[i].mark = mark; /* that have not been trimmed */
	} /* mark() */

	/*------------------------------------------------------------------*/
	/**
	 * Mark all nodes and edges with their index.
	 * 
	 * @since 2002.04.14 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected void index() { /* --- index nodes and edges */
		for (int i = this.nodecnt; --i >= 0;)
			if (this.nodes[i].mark >= -1) /* number the nodes of the graph */
				this.nodes[i].mark = i; /* that have not been trimmed */
		for (int i = this.edgecnt; --i >= 0;)
			if (this.edges[i].mark >= -1) /* number the edges of the graph */
				this.edges[i].mark = i; /* that have not been trimmed */
	} /* index() */

	/*------------------------------------------------------------------*/
	/**
	 * Encode the node types with the given type recoder.
	 * <p>
	 * The given type recoder is stored in the graph and may be retrieved with
	 * the function <code>getRecoder()</code>.
	 * </p>
	 * 
	 * @param coder
	 *            the type recoder to use
	 * @see #decode()
	 * @see #getRecoder()
	 * @since 2005.06.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void encode(Recoder coder) { /* --- encode node types */
		for (int i = this.nodecnt; --i >= 0;)
			this.nodes[i].encode(coder);
		this.coder = coder; /* store the type recoder */
	} /* encode() */

	/*------------------------------------------------------------------*/
	/**
	 * Decode the node types.
	 * <p>
	 * Calling this function has no effect if the nodes have not been encoded
	 * with <code>Graph.encode()</code> before. Otherwise the original types of
	 * the nodes are restored and the stored recoder is removed.
	 * </p>
	 * 
	 * @see #encode(Recoder)
	 * @see #getRecoder()
	 * @since 2005.06.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void decode() { /* --- decode node types */
		if (this.coder == null) /* if the graph is not encoded, */
			return; /* abort the function */
		for (int i = this.nodecnt; --i >= 0;)
			this.nodes[i].decode(this.coder);
		this.coder = null; /* delete the type recoder */
	} /* decode() */

	/*------------------------------------------------------------------*/
	/**
	 * Mask the node and edge types.
	 * <p>
	 * The types of all nodes and edges of the graph are logically anded with
	 * the masks in a given array of masks.
	 * </p>
	 * 
	 * @param masks
	 *            an array with 4 elements, which refer to<br>
	 *            0: nodes outside rings<br>
	 *            1: edges outside rings<br>
	 *            2: nodes in rings<br>
	 *            3: edges in rings
	 * @since 2005.07.20 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void maskTypes(int masks[]) { /* --- mask node and edge types */
		int i, k; /* loop variables */
		Node node; /* to traverse the nodes */
		Edge edge; /* to traverse the edges */

		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse the edges */
			edge.maskType(masks[edge.isInRing() ? 3 : 1]);
		} /* mask the edge type */
		for (i = this.nodecnt; --i >= 0;) {
			node = this.nodes[i]; /* traverse the nodes */
			for (k = node.deg; --k >= 0;)
				if (node.edges[k].isInRing())
					break;
			node.maskType(masks[(k >= 0) ? 2 : 0]);
		} /* mask the node type */
	} /* maskTypes() */

	/*------------------------------------------------------------------*/
	/**
	 * Trim a graph based on its encoding recoder.
	 * <p>
	 * All nodes with types that are marked as excluded (in the recoder used to
	 * encode the node types) as well as all incident edges are removed. Removal
	 * consists in specifically marking these nodes and edges if the parameter
	 * <code>remove</code> is <code>false</code>. Only if this parameter is
	 * <code>true</code>, the nodes and edges are actually removed from the
	 * graph.
	 * </p>
	 * 
	 * @param remove
	 *            whether the nodes and edges are actually to be removed (
	 *            <code>true</code>) or only to be marked (<code>false</code>)
	 * @since 2005.07.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected boolean trim(boolean remove) { /* --- trim graphs */
		int i, k, n = 0; /* loop variables */
		Node node; /* to traverse the nodes */
		Edge edge; /* to traverse the edges */

		if (this.coder == null) /* if there is no type recoder, */
			return false; /* there cannot be excluded nodes */
		for (i = this.nodecnt; --i >= 0;) {
			node = this.nodes[i]; /* traverse the nodes */
			if (node.isSpecial() || !this.coder.isExcluded(node.type))
				continue; /* skip not excluded nodes */
			node.mark = Integer.MIN_VALUE;
			n++; /* mark excluded nodes for deletion */
			for (k = node.deg; --k >= 0;)
				node.edges[k].mark = Integer.MIN_VALUE;
		} /* also mark all incident edges */
		if (n <= 0)
			return false; /* if no excluded nodes found, abort */
		if (!remove)
			return true; /* if not to remove nodes, abort */
		for (i = this.nodecnt; --i >= 0;) {
			node = this.nodes[i]; /* traverse the unmarked nodes */
			if (node.mark <= Integer.MIN_VALUE)
				continue;
			for (k = n = 0; k < node.deg; k++) {
				edge = node.edges[k]; /* traverse the edges of the node */
				if (edge.mark > Integer.MIN_VALUE)
					node.edges[n++] = edge;
			} /* delete all deleted edges */
			node.deg = n; /* and set the new edge counter */
		} /* (the new node degree) */
		for (i = n = 0; i < this.nodecnt; i++) {
			node = this.nodes[i]; /* traverse the nodes again */
			if (node.mark > Integer.MIN_VALUE)
				this.nodes[n++] = node;
		} /* remove the deleted nodes */
		this.nodecnt = n; /* set the new node counter */
		for (i = n = 0; i < this.edgecnt; i++) {
			edge = this.edges[i]; /* traverse the edges */
			if (edge.mark > Integer.MIN_VALUE)
				this.edges[n++] = edge;
		} /* remove the deleted edges */
		this.edgecnt = n; /* set the new edge counter */
		return true; /* return 'nodes removed' */
	} /* trim() */

	/*------------------------------------------------------------------*/
	/**
	 * Check whether the graph is connected.
	 * 
	 * @return whether the graph is connected.
	 * @since 2007.03.23 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean isConnected() { /* --- check whether connected */
		int i; /* loop variable */
		Node node; /* to traverse the nodes */
		boolean c; /* flag for a connected graph */

		if (this.nodecnt <= 1) /* if there is at most one node, */
			return true; /* the graph is connected */
		for (i = this.nodecnt; --i >= 0;)
			this.nodes[i].mark = -1; /* unmark all nodes and then mark */
		this.nodes[0].mark(0); /* connected comp. of first node */
		c = true; /* default: graph is connected */
		for (i = this.nodecnt; --i >= 0;) {
			node = this.nodes[i]; /* traverse the nodes */
			c &= (node.mark >= 0); /* check the node markers and */
			node.mark = -1; /* clear them at the same time */
		} /* (ensure an unmarked graph) */
		return c; /* return whether graph is connected */
	} /* isConnected() */

	/*------------------------------------------------------------------*/
	/**
	 * Internal recursive function for bridge finding and marking.
	 * 
	 * @param cur
	 *            the current node in the depth-first search
	 * @param in
	 *            the edge through which the current node was reached
	 * @param lvl
	 *            the current depth-first search level
	 * @return the depth-first search level with the lowest number that is
	 *         reachable
	 * @since 2005.06.07 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int bridges(Node cur, Edge in, int lvl) { /*
													 * --- recursively find
													 * bridges
													 */
		int i; /* loop variable */
		int low, l; /* lowest level of reachable node */
		Edge out; /* to traverse the outgoing edges */
		Node dst; /* destination of outgoing edge */

		low = cur.mark = lvl++; /* mark node with current level */
		for (i = cur.deg; --i >= 0;) {
			out = cur.edges[i]; /* traverse all outgoing edges */
			if (out == in)
				continue; /* (skip incoming edge) */
			dst = (out.src != cur) ? out.src : out.dst;
			l = dst.mark; /* get the destination node marker */
			if (l < 0) { /* if the dest. node is not marked, */
				l = this.bridges(dst, out, lvl); /* find bridges recursively */
				out.markBridge(l >= lvl); /* and mark edge as bridge */
			} /* if no lower level can be reached */
			if (l < low)
				low = l; /* update the lowest reachable level */
		} /* if necessary */
		return low; /* return lowest reachable level */
	} /* bridges() */

	/*------------------------------------------------------------------*/
	/**
	 * Mark all edges that are bridges.
	 * <p>
	 * This function marks all edges, the removal of which increases the number
	 * of connected components of the graph (bridges).<br>
	 * The bridge finding algorithm employed here is adapted from:<br>
	 * R.E. Tarjan.<br>
	 * Depth-First Search and Linear Graph Algorithms.<br>
	 * SIAM Journal of Computing 1(2):146--160.<br>
	 * Society for Industrial and Applied Mathematics, Philadelphia, PA, USA
	 * 1972
	 * 
	 * @return the number of edges that have been marked
	 * @since 2005.06.07 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int markBridges() { /* --- mark edges that are bridges */
		int i, n = 0; /* loop variable, number of bridges */

		for (i = this.nodecnt; --i >= 0;)
			this.nodes[i].mark = -1; /* unmark all nodes */
		for (i = this.nodecnt; --i >= 0;) {
			if (this.nodes[i].mark < 0)
				this.bridges(this.nodes[i], null, 0);
		} /* find all bridges in all components */
		for (i = this.edgecnt; --i >= 0;)
			if (this.edges[i].isBridge())
				n++;
		return n; /* count the marked bridges */
	} /* markBridges() *//* and return their number */

	/*------------------------------------------------------------------*/
	/**
	 * Internal function to remove a branch from a graph.
	 * <p>
	 * This function removes a branch from a graph by unmarking the nodes and
	 * edges contained in it. It starts from a node a that has to be at the end
	 * of a branch (although it does no harm if it is not). The idea is to speed
	 * up the search for rings by removing everything that is definitely not
	 * part of a ring.
	 * 
	 * @param node
	 *            a node at the end of a branch
	 * @since 2003.07.31 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private void prune(Node node) { /* --- prune a possible branch */
		int i; /* loop variable */
		Edge edge; /* to traverse the edges */

		while (node.mark == 1) { /* while at the end of a branch */
			node.mark = 0; /* unmark (remove) the node */
			for (i = node.deg; --i >= 0;)
				if (node.edges[i].mark > 0)
					break;
			edge = node.edges[i]; /* find the corresponding edge and */
			edge.mark = -1; /* unmark (remove) it from the graph */
			node = (edge.src != node) ? edge.src : edge.dst;
			node.mark--; /* get the node on the other side */
		} /* and decrement the edge counter */
	} /* prune() */

	/*------------------------------------------------------------------*/
	/**
	 * Internal function to recursively find and mark rings.
	 * <p>
	 * This function does a depth first search to find rings in a graph and
	 * marks the edges of each ring it finds with a unique bit that does not
	 * conflict with a bit already used for another ring it is connected to
	 * (similar to a coloring problem).
	 * </p>
	 * 
	 * @param node
	 *            the current node
	 * @param edge
	 *            the current edge
	 * @param term
	 *            the node at which the search was started
	 * @param rings
	 *            the already used (and thus excluded) ring flags
	 * @param min
	 *            the minimal number of further nodes that need to be found
	 *            before marking may be started
	 * @param max
	 *            the maximal number of further nodes
	 * @return the new ring flags set in the recursion
	 * @since 2003.07.31 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private long rings(Node node, Edge edge, Node term, long rings, int min,
			int max) { /* --- recursive ring search */
		int i; /* loop variable */
		long r; /* new ring flags */
		Edge out; /* to traverse the edges */
		Node dst; /* to traverse the nodes */

		for (i = node.deg; --i >= 0;)
			/* collect used ring flags */
			rings |= node.edges[i].flags & Edge.RINGMASK;
		--min;
		--max; /* decrement the ring edge counters */
		if (node == term) { /* if a ring has been found */
			if (min > 0)
				return 0; /* if min. size not reached, abort */
			for (r = 1; (r & Edge.RINGMASK) != 0; r <<= 1)
				if ((rings & r) == 0)
					break;
			if ((r & Edge.RINGMASK) == 0)
				return Edge.BRIDGE;
			edge.flags |= r; /* find an unused ring flag, set it */
			return r; /* in the ring flags of the edge, */
		} /* and return the ring flag */
		if ((node.mark < 0) /* check for an already visited node */
				|| (max <= 0))
			return 0; /* and for the maximum search depth */
		node.mark = -node.mark; /* mark the node as visited */
		for (r = 0, i = node.deg; --i >= 0;) {
			out = node.edges[i]; /* traverse the edges of the node */
			if ((out.mark <= 0) || (out == edge))
				continue; /* skip unmarked edges and back edge */
			dst = (out.src != node) ? out.src : out.dst;
			r |= this.rings(dst, out, term, rings | r, min, max);
		} /* recursively mark rings */
		node.mark = -node.mark; /* remove visited marker */
		edge.flags |= r; /* add the rings flags to the edge */
		return r; /* return the new ring flags */
	} /* rings() */

	/*------------------------------------------------------------------*/
	/**
	 * Mark rings in a given size range.
	 * <p>
	 * This function marks all edges and nodes that are part of a ring in the
	 * given size range with a default type flag.
	 * </p>
	 * <p>
	 * If not all rings could be marked (because there is an edge that is part
	 * of more rings than there are possible rings flags), the return value is
	 * negative (but its absolute value nevertheless is the number of rings that
	 * have been marked).
	 * </p>
	 * 
	 * @param min
	 *            the smallest ring size (number of edges/nodes)
	 * @param max
	 *            the largest ring size (number of edges/nodes)
	 * @return the number of rings that have been marked (negative if not all
	 *         rings could be marked)
	 * @since 2003.07.31 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int markRings(int min, int max) {
		return this.markRings(min, max, Edge.RING);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Mark rings in a given size range with a given type flag.
	 * <p>
	 * This function marks all edges and nodes that are part of a ring in the
	 * given size range with the given type flag. If the ring should not be
	 * marked in the type (or an existing type flag should be kept) and only the
	 * rings flags of the edges should be set, this function may be called with
	 * <code>typeflag == 0</code>.
	 * </p>
	 * 
	 * @param min
	 *            the smallest ring size (number of edges/nodes)
	 * @param max
	 *            the largest ring size (number of edges/nodes)
	 * @param typeflag
	 *            the type flag to use for marking
	 * @return the number of rings that have been marked
	 * @since 2003.07.31 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected int markRings(int min, int max, int typeflag) { /*
															 * --- mark rings of
															 * size [min,max]
															 */
		int i, cnt = 0; /* loop variable, counter */
		long r, x = 0; /* buffers for ring edge flags */
		Edge edge; /* to traverse the edges */

		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse the edges and */
			edge.mark = 1; /* mark them for pruning */
			edge.type &= ~typeflag; /* clear the edge's ring flags */
			edge.clearRings(); /* (individual as well as general) */
		}
		if (max <= 0)
			return 0; /* if nothing to mark, abort */
		for (i = this.nodecnt; --i >= 0;)
			this.nodes[i].mark = this.nodes[i].deg;
		for (i = this.nodecnt; --i >= 0;)
			this.prune(this.nodes[i]);/* remove all initial branches */
		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse the remaining edges */
			if (edge.mark <= 0)
				continue;
			x |= r = this.rings(edge.dst, edge, edge.src, 0, min, max);
			r &= Edge.RINGMASK; /* find rings by depth first search */
			for (; r != 0; r >>= 1)
				/* and count the newly marked rings */
				if ((r & 1) != 0)
					cnt++;/* (number of set ring flags) */
			edge.mark = 0; /* unmark the processed edge */
			edge.src.mark--; /* reduce the edge counters */
			edge.dst.mark--; /* in source and destination */
			this.prune(edge.src); /* prune possible new branches */
			this.prune(edge.dst); /* that have been created */
		} /* by removing the edge */
		if ((x & Edge.BRIDGE) != 0)
			cnt = -cnt;
		if (typeflag == 0)
			return cnt;
		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse all edges */
			if (edge.getRings() != 0)
				edge.type |= typeflag;
			else
				edge.type &= ~typeflag;
		} /* set a ring edge flag in the type */
		return cnt; /* return the number of rings */
	} /* markRings() */

	/*------------------------------------------------------------------*/
	/**
	 * Mark pseudo-rings up to a given size.
	 * <p>
	 * Pseudo-rings are rings that are smaller than the rings marked with the
	 * function <code>markRings()</code> (which must have been called before)
	 * and consist only of already marked ring edges. They are needed for a
	 * proper treatment of rings in connection with canonical form pruning.
	 * </p>
	 * 
	 * @param max
	 *            the maximal size of a pseudo-ring
	 * @return the number of marked pseudo-rings
	 * @since 2006.06.04 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected int markPseudo(int max) { /* --- mark pseudo-rings up to max */
		int i, k, cnt = 0; /* loop variable, buffer, counter */
		long r; /* buffer for ring edge flags */
		Edge edge; /* to traverse the edges */
		Node node; /* to traverse the nodes */

		if (max <= 0)
			return 0; /* if nothing to mark, abort */
		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse the edges */
			edge.mark = edge.isInRing() ? 1 : 0;
		} /* mark all ring edges */
		for (i = this.nodecnt; --i >= 0;) {
			node = this.nodes[i];
			node.mark = 0;
			for (k = node.deg; --k >= 0;)
				if (node.edges[k].mark > 0)
					node.mark++;
		} /* count the marked incident edges */
		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse the remaining edges */
			if (edge.mark <= 0)
				continue;
			r = edge.getRings(); /* get the edge's ring flags */
			r = this.rings(edge.dst, edge, edge.src, r, 0, max);
			for (; r != 0; r >>= 1)
				/* find and mark pseudo-rings and */
				if ((r & 1) != 0)
					cnt++;/* count the newly marked rings */
			edge.mark = 0; /* unmark the processed edge */
			edge.src.mark--; /* reduce the edge counters */
			edge.dst.mark--; /* in source and destination */
			this.prune(edge.src); /* prune possible new branches */
			this.prune(edge.dst); /* that have been created */
		} /* by removing the edge */
		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse all edges */
			if (edge.getRings() != 0)
				edge.type |= Edge.RING;
			else
				edge.type &= ~Edge.RING;
		} /* set a ring edge flag in the type */
		return cnt; /* return the number of rings */
	} /* markPseudo() */

	/*------------------------------------------------------------------*/
	/**
	 * Check whether this subgraph has open rings.
	 * <p>
	 * This function checks whether there are edges in this subgraph that are
	 * part of a ring in the underlying graphs, but is not part of a ring in
	 * this subgraph. It is used for filtering if only substructures with
	 * complete rings are desired.
	 * </p>
	 * 
	 * @param min
	 *            the smallest ring size (number of edges/nodes)
	 * @param max
	 *            the largest ring size (number of edges/nodes)
	 * @return whether the subgraph contains an open ring
	 * @since 2006.05.16 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean hasOpenRings(int min, int max) { /* --- check for open rings */
		int i; /* loop variable */
		Edge edge; /* to traverse the edges */

		this.markRings(min, max, 0);/* mark rings, but keep type flag */
		for (i = this.edgecnt; --i >= 0;) {
			edge = this.edges[i]; /* traverse all edges */
			if (edge.isInRing() && (edge.getRings() == 0))
				return true; /* check for a edge in a ring, */
		} /* but without individual rings */
		return false; /* if there is, there is an open ring */
	} /* hasOpenRings() *//* otherwise all rings are closed */

	/*------------------------------------------------------------------*/
	/**
	 * Prepare a graph for frequent substructure mining.
	 * <p>
	 * This function sorts the edges for each node w.r.t. their type and the
	 * type of the other incident node. This is necessary for seed embedding and
	 * frequent substructure mining, because the order of the edges is exploited
	 * to simplify and restrict the search (search tree pruning, leaving loops
	 * early).
	 * </p>
	 * 
	 * @since 2002.03.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void prepare() { /* --- sort the edges of all nodes */
		this.mark(-1); /* clear all node and edge markers */
		for (int i = this.nodecnt; --i >= 0;)
			this.nodes[i].sortEdges();/* sort incident edges of all nodes */
	} /* prepare() */

	/*------------------------------------------------------------------*/
	/**
	 * Prepare a graph for embedding it into another.
	 * <p>
	 * For this function to work properly, the graph must be connected (a single
	 * connected component). It is prepared by sorting the edges of all nodes
	 * and by reordering the nodes and edges into a breadth-first search order
	 * (to simplify the embedding).
	 * </p>
	 * <p>
	 * This function is not called in <code>embed()</code>, so that a graph that
	 * has to be embedded into several other graphs is not prepared in this way
	 * over and over again.
	 * </p>
	 * 
	 * @return whether the graph is connected (and thus preparation succeeded)
	 * @since 2002.03.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean prepareEmbed() { /* --- sort edges and nodes */
		int i, k, n, e; /* loop variables, counters */
		Node src, dst, x; /* to traverse the nodes, buffer */
		Edge edge, y; /* to traverse the edges, buffer */

		this.prepare(); /* sort incident edges of all nodes */
		this.index(); /* mark nodes and edges with indices */
		for (x = this.nodes[i = 0]; ++i < this.nodecnt;) {
			src = this.nodes[i]; /* traverse the nodes */
			if (src.type < x.type)
				x = src;
		} /* find a "minimal" node */
		src = this.nodes[0]; /* and swap it to front */
		this.nodes[src.mark = x.mark] = src;
		this.nodes[0] = x;
		x.mark = -1;
		for (n = 1, i = e = 0; i < this.nodecnt; i++) {
			if (i >= n) { /* check for a connected graph */
				this.mark(-1);
				return false;
			}
			src = this.nodes[i]; /* traverse the nodes breadth-first */
			for (k = 0; k < src.deg; k++) {
				edge = src.edges[k]; /* traverse the edges of each node */
				if (edge.mark < 0)
					continue;
				y = this.edges[e]; /* swap edge to next position */
				this.edges[y.mark = edge.mark] = y;
				this.edges[e++] = edge;
				edge.mark = -1;
				dst = (edge.src != src) ? edge.src : edge.dst;
				if (dst.mark < 0)
					continue;
				x = this.nodes[n]; /* swap dest. node to next position */
				this.nodes[x.mark = dst.mark] = x;
				this.nodes[n++] = dst;
				dst.mark = -1;
			} /* bring nodes into breadth-first */
		} /* search order for embedding */
		return true; /* return "connected graph" */
	} /* prepareEmbed() */

	/*------------------------------------------------------------------*/
	/**
	 * Embed a single node into the graph.
	 * 
	 * @param type
	 *            the type of the node to embed
	 * @return a list of embeddings of the node
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Embedding embed(int type) { /* --- find embeddings of a node */
		int i; /* loop variable */
		Embedding emb, list = null; /* created (list of) embedding(s) */

		for (i = this.nodecnt; --i >= 0;) {
			if ((type != Node.WILDCARD) && (this.nodes[i].type != type))
				continue; /* traverse nodes of the given type */
			emb = new Embedding(this, i);
			emb.succ = list; /* create a new embedding */
			list = emb; /* and add it at the head */
		} /* of the embedding list */
		return list; /* return the created embeddings */
	} /* embed() */

	/*------------------------------------------------------------------*/
	/**
	 * Internal recursive function for embedding a graph.
	 * 
	 * @param graph
	 *            the graph to embed
	 * @param ens
	 *            the array of nodes of the embedding
	 * @param nid
	 *            the index of the next free entry in the node array
	 * @param ees
	 *            the array of edges of the embedding
	 * @param eid
	 *            the current edge index in the graph
	 * @param list
	 *            the list of already found embeddings or the constant
	 *            <code>CONTAINED</code> for a containment check
	 * @return a list of found embeddings or the special constant
	 *         <code>CONTAINED</code> (if <code>list</code> was this constant
	 *         and the graph was found)
	 * @since 2005.08.16 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private Embedding embed(Graph graph, Node[] ens, int nid, Edge[] ees,
			int eid, Embedding list) { /* --- embed a graph recursively */
		int i, m, t; /* loop variable, buffers */
		Edge re, edge, x; /* to access/traverse the edges */
		Node rd, src, dst; /* to access/traverse the nodes */
		Node end; /* end of a chain (if embedded) */
		boolean chain; /* flag for a chain to embed/match */
		Embedding emb; /* created embedding */

		if (eid >= graph.edgecnt) { /* if all edges have been matched */
			if (list == CONTAINED) /* if only to check containment, */
				return CONTAINED; /* return a special dummy embedding */
			emb = new Embedding(this, ens, ees);
			emb.succ = list; /* create a new embedding */
			return emb; /* and add it at the head */
		} /* of the embedding list */
		re = graph.edges[eid]; /* get the next edge to embed */
		rd = re.dst;
		src = re.src; /* and its incident nodes */
		if ((src.mark < 0) || ((rd.mark >= 0) && (src.mark > rd.mark))) {
			rd = src;
			src = re.dst;
		} /* identify source and dest. node */
		src = ens[src.mark]; /* get corresponding source node */
		m = rd.mark; /* note the dest. node marker and */
		t = rd.type & ~Node.CHAIN; /* its type without the chain flag */
		chain = rd.isChain(); /* and check for a chain node */
		emb = (list == CONTAINED) ? null : list;
		for (i = src.deg; --i >= 0;) {
			edge = src.edges[i]; /* traverse the unmarked edges */
			if (edge.mark >= 0)
				continue; /* compare the */
			if (edge.type > re.type)
				continue; /* edge type */
			if (edge.type < re.type)
				return emb;
			dst = (edge.src != src) ? edge.src : edge.dst;
			if (rd.type != Node.WILDCARD) { /* if it is not a wildcard */
				if (dst.type > t)
					continue; /* compare the */
				if (dst.type < t)
					return emb; /* dest. node type */
			}
			if (dst.mark != rd.mark)
				continue; /* compare dest. index */
			if (!chain) { /* if not to match/embed a chain */
				if (dst.deg < rd.deg)
					continue;
				end = null;
			} /* there is no end of chain node */
			else if (dst.isChain()) { /* if to match a chain node, */
				end = null;
			} /* there is no end of chain node */
			else { /* if to embed a chain node */
				if (dst.deg != 2)
					continue;
				x = (dst.edges[0] != edge) ? dst.edges[0] : dst.edges[1];
				if (x.type != re.type)
					continue;
				ees[edge.mark = eid++] = edge;
				do { /* loop to follow the chain */
					edge = x;
					dst = (x.src != dst) ? x.src : x.dst;
					if ((dst.type != t) || (dst.deg != 2))
						break;
					x = dst.edges[(dst.edges[0] != x) ? 0 : 1];
				} while (x.type == re.type);
				x = graph.edges[eid]; /* get edge after the chain node */
				end = (x.src != rd) ? x.src : x.dst;
				end.mark = nid; /* mark the node after the chain */
			} /* in the graph to embed */
			ees[edge.mark = eid] = edge; /* mark and store edge (and node) */
			if (m < 0) {
				ens[dst.mark = rd.mark = nid++] = dst;
			}
			emb = this.embed(graph, ens, nid, ees, eid + 1, list);
			if (m < 0) {
				dst.mark = rd.mark = -1;
				nid--;
			}
			if (end != null) {
				ees[--eid].mark = end.mark = -1;
			}
			edge.mark = -1; /* find embeddings recursively */
			if (emb == CONTAINED) /* if only to check containment, */
				break; /* check the recursion result */
			if (emb != null)
				list = emb;
		} /* transfer the current embeddings */
		return emb; /* return the list of embeddings */
	} /* embed() */

	/*------------------------------------------------------------------*/
	/**
	 * Internal function for embedding a graph.
	 * 
	 * @param graph
	 *            the graph to embed
	 * @param list
	 *            the initial list of embeddings (either <code>null</code> or
	 *            the constant <code>CONTAINED</code> for a containment check)
	 * @return a list of found embeddings or the special constant
	 *         <code>CONTAINED</code> (if <code>list</code> was this constant
	 *         and the graph was found)
	 * @since 2005.08.16 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private Embedding embed(Graph graph, Embedding list) { /*
															 * --- find
															 * embeddings of a
															 * graph
															 */
		int i, n, t, d; /* loop variables, buffers */
		Node node; /* to traverse the nodes */
		Node ens[]; /* nodes of the embedding */
		Edge ees[]; /* edges of the embedding */
		Embedding emb; /* created list of embeddings */

		node = graph.nodes[0]; /* get the first node of the graph */
		t = node.type; /* and note its type for comparisons */
		if (graph.nodecnt <= 1) { /* if there is only one node */
			if (list == null) /* if to embed only a single node, */
				return this.embed(t); /* call specialized function */
			if (t == Node.WILDCARD)
				return CONTAINED;
			for (i = this.nodecnt; --i >= 0;)
				if (this.nodes[i].type == t)
					return CONTAINED; /* search for the single node */
			return null; /* among the graph's nodes */
		} /* and report the result */
		if ((graph.nodecnt > this.nodecnt) || (graph.edgecnt > this.edgecnt))
			return null; /* the embedding must not be larger */
		for (n = 0, i = graph.nodecnt; --i >= 0;)
			if (graph.nodes[i].isChain())
				n++; /* count the chain nodes */
		ens = new Node[graph.nodecnt - n];
		ees = new Edge[graph.edgecnt];
		node.mark = 0; /* mark the first node */
		d = node.deg; /* and note its degree */
		emb = null; /* init. the list of embeddings */
		for (i = this.nodecnt; --i >= 0;) {
			node = this.nodes[i]; /* traverse the graph's nodes */
			if (((t != Node.WILDCARD) && (node.type != t)) || (node.deg < d)) /*
																			 * check
																			 * for
																			 * the
																			 * right
																			 * type
																			 */
				continue; /* and a compatible degree */
			ens[0] = node; /* note the root node and */
			node.mark = 0; /* mark it with its index */
			emb = this.embed(graph, ens, 1, ees, 0, list);
			node.mark = -1; /* match the edges recursively */
			if (emb == CONTAINED) /* if only to check containment, */
				break; /* check the recursion result */
			if (emb != null)
				list = emb;
		} /* transfer the current embeddings */
		graph.nodes[0].mark = -1; /* unmark the first node */
		return emb; /* return the recursion result */
	} /* embed() */

	/*------------------------------------------------------------------*/
	/**
	 * Embed a graph structure (find all its embeddings).
	 * <p>
	 * This function embeds a given graph in all possible ways and returns a
	 * list of all found embeddings. For this function to work properly, the
	 * graph to embed must either have been processed with the function
	 * <code>prepareEmbed()</code>, or it must have been created from a fragment
	 * that itself was generated in the search process (class <code>Miner</code>
	 * ), so that the edges are sorted in a specific way (so that source nodes
	 * are always already numbered). In addition, the graph into which to embed
	 * must be prepared by calling the function <code>prepare()</code> on it.
	 * </p>
	 * 
	 * @param graph
	 *            the graph to embed
	 * @return a list of all embeddings of the graph
	 * @since 2002.03.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Embedding embed(Graph graph) {
		return (graph.nodecnt <= 0) ? null : this.embed(graph, null);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Check whether a node type is contained in this graph.
	 * 
	 * @param type
	 *            the type of the node to check for
	 * @return a list of embeddings of the node
	 * @since 2010.01.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected boolean contains(int type) { /* --- check occurrence of node type */
		for (int i = this.nodecnt; --i >= 0;)
			if ((type == Node.WILDCARD) || (this.nodes[i].type == type))
				return true; /* check for a node of given type */
		return false; /* if none exists, return false */
	} /* embed() */

	/*------------------------------------------------------------------*/
	/**
	 * Check whether a graph is contained in this graph.
	 * <p>
	 * This function tries to embed a given graph and returns as soon as one
	 * embedding is found (in contrast to the function <code>embed(Graph)</code>
	 * , which tries to find all embeddings). For this function to work
	 * properly, the graph to embed must either have been processed with the
	 * function <code>prepareEmbed()</code>, or it must have been created from a
	 * fragment that itself was generated in the search process, so that the
	 * edges are sorted in a specific way (so that source nodes are always
	 * already numbered). In addition, the graph into which to embed the
	 * argument graph must be prepared by calling the function
	 * <code>prepare()</code> on it.
	 * </p>
	 * 
	 * @param graph
	 *            the graph to embed
	 * @return whether the given graph is contained in this graph
	 * @see #embed(Graph)
	 * @since 2002.03.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean contains(Graph graph) { /*
											 * --- check whether a graph is
											 * cont.
											 */
		if (graph.nodecnt <= 0)
			return true;
		if (graph.nodecnt <= 1)
			return this.contains(graph.nodes[0].type);
		return (this.embed(graph, CONTAINED) != null);
	} /* contains() */

	/*------------------------------------------------------------------*/
	/**
	 * Check whether a graph is canonic w.r.t. a given canonical form.
	 * <p>
	 * This function checks whether the current order of the nodes and edges of
	 * the graph yields the smallest code word w.r.t. the canonical form that is
	 * specified by a given canonical form.
	 * </p>
	 * 
	 * @param cnf
	 *            the canonical form
	 * @return whether the graph is canonical
	 * @since 2006.05.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean isCanonic(CanonicalForm cnf) {
		return cnf.isCanonic(this);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Check whether a graph is canonic w.r.t. a given canonical form.
	 * 
	 * @param cnf
	 *            the canonical form
	 * @param fixed
	 *            the number of fixed edges
	 * @return whether the graph is canonic
	 *         <p>
	 *         <table cellpadding=0 cellspacing=0>
	 *         <tr>
	 *         <td>-1,&nbsp;</td>
	 *         <td>if the graph differs from the canonical form in the first
	 *         <code>fixed</code> edges,</td>
	 *         </tr>
	 *         <tr>
	 *         <td align="right">0,&nbsp;</td>
	 *         <td>if it is not canonical, but does not differ in the first
	 *         <code>fixed</code> edges,</td>
	 *         </tr>
	 *         <tr>
	 *         <td>+1,&nbsp;</td>
	 *         <td>if the graph is canonical.</td>
	 *         </tr>
	 *         </table>
	 *         </p>
	 * @since 2006.05.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected int isCanonic(CanonicalForm cnf, int fixed) {
		return cnf.isCanonic(this, fixed);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Make a graph canonic w.r.t. a given canonical form.
	 * <p>
	 * This function brings the nodes and edges of the graph into the order that
	 * yields the smallest code word w.r.t. a given canonical form.
	 * </p>
	 * 
	 * @param cnf
	 *            the extension object defining the canonical form
	 * @return whether the graph was modified
	 * @since 2006.05.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean makeCanonic(CanonicalForm cnf) {
		return this.makeCanonic(cnf, -1);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Make a graph canonic w.r.t. a given canonical form.
	 * <p>
	 * This function brings the nodes and edges of the graph into the order that
	 * yields the smallest code word w.r.t. the canonical form that is specified
	 * by a given extension object. However, the first <code>keep</code> edges
	 * as well as the nodes incident to these edges are not moved. Hence the
	 * result may not be in canonical form, but may differ in these first
	 * <code>keep</code> edges from the canonical form.
	 * </p>
	 * 
	 * @param cnf
	 *            the canonical form
	 * @param keep
	 *            the number of edges to keep at the beginning of the graph/code
	 *            word
	 * @return whether the graph was modified
	 * @since 2006.05.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected boolean makeCanonic(CanonicalForm cnf, int keep) { /*
																 * --- make a
																 * graph canonic
																 */
		if (!cnf.makeCanonic(this, keep))
			return false;
		this.map(cnf); /* copy the reordered arrays */
		return true; /* return 'graph was modified' */
	} /* makeCanonic() */

	/*------------------------------------------------------------------*/
	/**
	 * Normalize a graph w.r.t. a given canonical form.
	 * <p>
	 * This function brings the nodes and edges of the graph into the order that
	 * yields the smallest code word w.r.t. a given canonical form. It also
	 * sorts all edges per node accordingly, so that the output (in basically
	 * any notation) is unique.
	 * </p>
	 * 
	 * @param cnf
	 *            the canonical form
	 * @since 2007.03.19 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void normalize(CanonicalForm cnf) { /* --- normalize a graph */
		this.makeCanonic(cnf, -1); /* make the graph canonic */
		for (int i = this.edgecnt; --i >= 0;)
			this.edges[i].mark = i; /* number the edges */
		for (int i = this.nodecnt; --i >= 0;)
			this.nodes[i].sortEdges();/* sort the incident edges */
		this.mark(-1); /* clear all node and edge markers */
	} /* normalize() */

	/*------------------------------------------------------------------*/
	/**
	 * Reorganize a graph with a map from a canonical form.
	 * <p>
	 * This function carries out the reordering that was determined with one of
	 * the functions <code>CanonicalForm.makeCanonic()</code> or
	 * <code>CanonicalForm.adaptRing()</code> and was stored in internal arrays
	 * of the canonical form.
	 * </p>
	 * 
	 * @param cnf
	 *            the canonical form providing the map
	 * @since 2006.10.24 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected void map(CanonicalForm cnf) { /* --- reorder edges and nodes */
		if (this.edgecnt <= 0)
			return;
		System.arraycopy(cnf.nodes, 0, this.nodes, 0, this.nodecnt);
		System.arraycopy(cnf.edges, 0, this.edges, 0, this.edgecnt);
	} /* map() */

	/*------------------------------------------------------------------*/
	/**
	 * Compare the (canonical) code words of two graphs.
	 * <p>
	 * This function determines whether the code words of two graph (the graph
	 * the function is called on and the argument graph), as they are implicitly
	 * represented by the order of their nodes and edges, are equal.
	 * </p>
	 * <p>
	 * This function is equivalent to <code>equals(Object)</code> provided the
	 * two graphs compared have both been made canonical with the function
	 * <code>makeCanonic(CanonicalForm)</code>. The function is independent of
	 * the used canonical form, because it only checks for equality and not for
	 * an order relationship of the code words.
	 * </p>
	 * 
	 * @param graph
	 *            the graph to compare to
	 * @return whether the (canonical) code words are equal
	 * @since 2011.02.18 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean equalsCanonic(Graph graph) { /*
												 * --- compare (canonical) code
												 * words
												 */
		int i; /* loop variable */
		Node s1, s2, d1, d2; /* to traverse the nodes */
		Edge e1, e2; /* to traverse the edges */

		if (graph == this)
			return true; /* check some trivial cases */
		if (graph == null)
			return false; /* to ensure some properties */
		if ((this.nodecnt != graph.nodecnt) || (this.edgecnt != graph.edgecnt))
			return false; /* compare node and edge counters */
		if (this.nodecnt <= 0)
			return true;
		if (this.nodes[0].type != graph.nodes[0].type)
			return false; /* compare the first letter (root) */
		for (i = 0; i < this.nodecnt; i++)
			/* mark all nodes */
			this.nodes[i].mark = graph.nodes[i].mark = i;
		for (i = 0; i < this.edgecnt; i++) {
			e1 = this.edges[i]; /* traverse the edges of both graphs */
			e2 = graph.edges[i]; /* and compare their types */
			if (e1.type != e2.type)
				return false;
			if (e1.src.mark < e1.dst.mark) {
				s1 = e1.src;
				d1 = e1.dst;
			} else {
				s1 = e1.dst;
				d1 = e1.src;
			}
			if (e2.src.mark < e2.dst.mark) {
				s2 = e2.src;
				d2 = e2.dst;
			} else {
				s2 = e2.dst;
				d2 = e2.src;
			}
			if ((s1.type != s2.type) || (s1.mark != s2.mark)
					|| (d1.type != d2.type) || (d1.mark != d2.mark))
				return false; /* compare the node types */
		} /* and the node indices */
		for (i = 0; i < this.nodecnt; i++)
			/* unmark all nodes */
			this.nodes[i].mark = graph.nodes[i].mark = -1;
		return true; /* return that code words are equal */
	} /* equalsCanonic() */

	/*------------------------------------------------------------------*/
	/**
	 * Create a string description of the graph.
	 * 
	 * @return a string description of the graph
	 * @since 2006.10.24 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public String toString() {
		return this.ntn.describe(this);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a string description in the given notation.
	 * 
	 * @return a string description of the graph
	 * @since 2007.06.29 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public String toString(Notation ntn) {
		return ntn.describe(this);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a code word for a given canonical form.
	 * <p>
	 * The code word is created in the form prescribed by the given canonical
	 * form. However, it need not be the canonical code word for this graph,
	 * since it uses the current order of the nodes and edges of the graph. In
	 * order to obtain the canonical code word, the graph first has to be made
	 * canonic by calling the function <code>makeCanonic(CanonicalForm)</code>
	 * on it.
	 * </p>
	 * 
	 * @param cnf
	 *            the canonical form
	 * @return a code word for the given canonical form
	 * @since 2006.05.08 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public String toString(CanonicalForm cnf) {
		return cnf.describe(this);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Auxiliary function for testing basic functionality.
	 * 
	 * @since 2011.02.15 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private String orbits() { /* --- show the node orbit reps. */
		int i, k; /* loop variable, buffer */
		StringBuffer s; /* created description */
		TypeMgr t; /* node type manager */
		Node node; /* to traverse the nodes */

		t = this.getNodeMgr(); /* get the node type manager and */
		s = new StringBuffer(); /* create an empty string buffer */
		for (i = 0; i < this.nodecnt; i++) {
			node = this.nodes[i]; /* traverse the nodes of the graph */
			if (i > 0)
				s.append(' '); /* print a separator if necessary */
			k = node.type; /* get and decode the node type */
			if (this.coder != null)
				k = this.coder.decode(k);
			s.append(t.getName(k)); /* print the node type */
			s.append(':'); /* and a colon */
			s.append(node.orbit); /* print the orbit representative */
		}
		return s.toString(); /* return the node orbits string */
	} /* orbits() */

	/*------------------------------------------------------------------*/
	/**
	 * Auxiliary function for testing basic functionality.
	 * 
	 * @since 2007.10.25 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private void show() { /* --- show graph and its code words */
		CanonicalForm cnf; /* canonical form for code words */

		this.normalize(cnf = new CnFBreadth1());
		System.out.println(this.ntn.describe(this));
		System.out.println("breadth-first code word:");
		System.out.println(cnf.describe(this, false));
		System.out.println("orbits:");
		System.out.println(this.orbits());
		System.out.println("");

		this.normalize(cnf = new CnFDepth());
		System.out.println(this.ntn.describe(this));
		System.out.println("depth-first   code word:");
		System.out.println(cnf.describe(this, false));
		System.out.println("orbits:");
		System.out.println(this.orbits());
		System.out.println("");
	} /* show() */

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public void setWeight2(double w2){
		this.weight2 = w2;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Main function for testing some basic functionality.
	 * <p>
	 * It is tried to parse the first command line argument as a SMILES, SLN, or
	 * LiNoG description of a graph (in this order). If parsing suceeds, the
	 * graph is normalized and printed together with its breadth-first and
	 * depth-first spanning tree canonical code words.
	 * </p>
	 * 
	 * @param args
	 *            the command line arguments
	 * @since 2006.01.02 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public static void main(String args[]) { /* --- main function for testing */
		Graph graph; /* created graph */
		int[] masks; /* masks for node and edge types */

		if (args.length != 1) { /* if wrong number of arguments */
			System.err.println("usage: java moss.Graph <string>");
			return; /* print a usage message */
		} /* and abort the program */

		masks = new int[4]; /* create node and edge masks */
		masks[0] = masks[2] = AtomTypeMgr.ELEMMASK;
		masks[1] = masks[3] = BondTypeMgr.BONDMASK;

		try { /* try SMILES */
			System.out.println("SMILES:");
			graph = new SMILES().parse(new StringReader(args[0]));
			graph.maskTypes(masks); /* parse the argument and */
			graph.show();
			return;
		} /* show the parsed graph */
		catch (IOException e) { /* catch parse errors */
			System.err.println(e.getMessage() + "\n");
		}

		try { /* try SYBYL line notation (SLN) */
			System.out.println("SLN   :");
			graph = new SLN().parse(new StringReader(args[0]));
			graph.maskTypes(masks); /* parse the argument and */
			graph.show();
			return;
		} /* show the parsed graph */
		catch (IOException e) { /* catch parse errors */
			System.err.println(e.getMessage() + "\n");
		}

		try { /* try general line notation */
			System.out.println("LiNoG :");
			graph = new LiNoG().parse(new StringReader(args[0]));
			/* no type masking *//* parse the argument and */
			graph.show();
			return;
		} /* show the parsed graph */
		catch (IOException e) { /* catch parse errors */
			System.err.println(e.getMessage());
		}
	} /* main() */

} /* class Graph */
