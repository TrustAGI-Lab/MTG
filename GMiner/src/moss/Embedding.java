/*----------------------------------------------------------------------
  File    : Embedding.java
  Contents: Management of embeddings of graph fragments
  Author  : Christian Borgelt
  History : 2002.03.11 file created as file submol.java
            2003.08.04 embedding duplication function added (for debug)
            2003.08.07 file split, this part renamed to Embedding.java
            2003.08.09 position of new edges changed to end of array
            2003.08.10 extension of an embedding by an node added
            2005.07.22 marking functions/marking strategy modified
            2005.08.01 output functions removed (did not work anyway)
            2005.08.02 constructor from graph markers added
            2005.08.03 special functions for seed embedding removed
            2005.08.17 unnecessary functions removed, reembedding added
            2006.05.03 bug in constructor Embedding(CanonicalForm) fixed
            2006.10.23 marking functions redesigned
            2006.10.31 adapted to refactored classes
            2006.11.11 function hashCode() added
            2007.02.23 bug in function hashCode() fixed
            2007.06.12 functions overlaps and overlapsHarmfully added
            2007.08.10 function getGroup() added
            2007.08.16 check for identical images added to overlaps()
            2007.10.23 base reference removed, function common() added
            2009.04.30 function hashCode() improved
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.StringReader;

/*--------------------------------------------------------------------*/
/**
 * Class for embeddings of substructures into graphs.
 * <p>
 * An embedding of a substructure is represented by referring to the nodes and
 * edges of a graph, namely by simply listing the nodes and edges (from the
 * graph) that form the substructure. An embedding must contain at least one
 * node, i.e., it cannot be empty. It need not contain any edges, though.
 * </p>
 * <p>
 * If the edges array has a positive length, then usually
 * <code>edges[edges.length-1]</code> contains the edge added last (perfect
 * extensions and ring extensions may lead to exceptions from this rule,
 * especially after adaptation).
 * </p>
 * <p>
 * The nodes in the nodes array are usually sorted in the order in which they
 * have been added in the extension process (perfect extensions and ring
 * extensions may lead to exceptions from this rule, especially after
 * adaptation).
 * </p>
 * 
 * @author Christian Borgelt
 * @since 2002.03.11
 */
/*--------------------------------------------------------------------*/
public class Embedding {

	/*------------------------------------------------------------------*/
	/* constants */
	/*------------------------------------------------------------------*/
	/** dummy for an empty edge vector (avoid multiple instances) */
	private static final Edge[] EMPTY = new Edge[0];

	/*------------------------------------------------------------------*/
	/* instance variables */
	/*------------------------------------------------------------------*/
	/** the next embedding in a list of embeddings */
	protected Embedding succ = null;
	/** the graph referred to */
	protected Graph graph;
	/** the array of nodes (only references to the underlying graph) */
	protected Node[] nodes;
	/** the array of edges (only references to the underlying graph) */
	protected Edge[] edges;

	/*------------------------------------------------------------------*/
	/**
	 * Dummy constructor.
	 * <p>
	 * This constructor is only needed to create the dummy object
	 * <code>CONTAINED</code> in the class <code>Graph</code>, which is used as
	 * a special parameter and as a special return value for the function
	 * <code>Graph.embed()</code> in order to save a recursion parameter.
	 * </p>
	 * 
	 * @since 2006.08.28 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Embedding() {
		this.graph = null;
		this.nodes = null;
		this.edges = null;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a single node embedding.
	 * <p>
	 * The node referred to is given by its index in the graph. The edges array
	 * is initialized to an array of zero size (not <code>null</code>).
	 * </p>
	 * 
	 * @param graph
	 *            the graph into which to embed
	 * @param index
	 *            the index of the node in the graph
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Embedding(Graph graph, int index) { /*
												 * --- create a single node
												 * embedding
												 */
		this.graph = graph; /* note the graph referred to */
		this.nodes = new Node[] { graph.nodes[index] };
		this.edges = EMPTY; /* store a reference to the node */
	} /* Embedding() */

	/*------------------------------------------------------------------*/
	/**
	 * Create an embedding from node and edge references.
	 * <p>
	 * This function is used when embedding a seed structure. The given node and
	 * edge arrays are copied.
	 * </p>
	 * 
	 * @param graph
	 *            the graph referred to
	 * @param nodes
	 *            the nodes of the substructure
	 * @param edges
	 *            the edges of the substructure
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Embedding(Graph graph, Node[] nodes, Edge[] edges) { /*
																	 * ---
																	 * create
																	 * embedding
																	 * from
																	 * arrays
																	 */
		this.graph = graph; /* copy the node and edge arrays */
		this.nodes = new Node[nodes.length];
		System.arraycopy(nodes, 0, this.nodes, 0, nodes.length);
		this.edges = new Edge[edges.length];
		System.arraycopy(edges, 0, this.edges, 0, edges.length);
	} /* Embedding() */

	/*------------------------------------------------------------------*/
	/**
	 * Create an embedding from a canonical form.
	 * <p>
	 * This function is called if a created extension is only needed as an
	 * embedding (in contrast to a fragment).
	 * </p>
	 * 
	 * @param cnf
	 *            the canonical form holding the extension
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Embedding(CanonicalForm cnf) { /* --- create an embedding */
		int i, n, e; /* loop variables, buffers */
		Node node; /* to traverse the nodes */
		Edge edge; /* to traverse the edges */

		this.graph = cnf.emb.graph; /* note the graph referred to */
		n = cnf.emb.nodes.length; /* get the old number of nodes */
		if (cnf.nodecnt <= 0) /* if there are no new nodes, */
			this.nodes = cnf.emb.nodes; /* keep base node array */
		else { /* if there are new nodes */
			this.nodes = new Node[n + cnf.nodecnt];
			System.arraycopy(cnf.emb.nodes, 0, this.nodes, 0, n);
		} /* copy the nodes of the base */
		e = cnf.emb.edges.length; /* get the old number of edges */
		if (cnf.edgecnt <= 0) /* if there are no new edges, */
			this.edges = cnf.emb.edges; /* keep base edge array */
		else { /* if there are new edges */
			this.edges = new Edge[e + cnf.edgecnt];
			System.arraycopy(cnf.emb.edges, 0, this.edges, 0, e);
		} /* copy the edges of the base */
		if (cnf.size > 0) { /* if ring extension */
			for (i = 0; i < cnf.size; i++) {
				node = cnf.nodes[i]; /* traverse the extension nodes */
				if (node.mark < 0)
					this.nodes[n++] = node;
				edge = cnf.edges[i]; /* traverse the extension edges */
				if (edge.mark < 0)
					this.edges[e++] = edge;
			}
		} /* copy the new nodes and edges */
		else { /* if normal or chain extension */
			for (i = cnf.nodecnt; --i >= 0;)
				this.nodes[n + i] = cnf.nodes[i + 1];
			for (i = cnf.edgecnt; --i >= 0;)
				this.edges[e + i] = cnf.edges[i];
		} /* copy the new nodes and edges */
	} /* Embedding() */

	/*------------------------------------------------------------------*/
	/**
	 * Extend a given embedding with a given edge.
	 * <p>
	 * This function assumes that the nodes of the embedding to extend are
	 * marked (with non-negative values) in the underlying graph. It is only
	 * needed for <code>Embedding.extend()</code>.
	 * </p>
	 * 
	 * @param emb
	 *            the embedding to extend
	 * @param edge
	 *            the edge by which to extend the embedding
	 * @see #extend(int,int,int,int)
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Embedding(Embedding emb, Edge edge) { /*
													 * --- create an extended
													 * embedding
													 */
		this.graph = emb.graph; /* note the graph referred to */
		int n = emb.edges.length; /* get the (old) number of edges */
		this.edges = new Edge[n + 1]; /* and create the edge array */
		System.arraycopy(emb.edges, 0, this.edges, 0, n);
		this.edges[n] = edge; /* store the new edge */
		if ((edge.src.mark >= 0) /* if neither the source node */
				&& (edge.dst.mark >= 0)) /* nor the destination node are new, */
			this.nodes = emb.nodes; /* set the array of the base emb. */
		else { /* if a new node is to be added */
			n = emb.nodes.length; /* get the (old) number of nodes */
			this.nodes = new Node[n + 1]; /* create a new node array */
			System.arraycopy(emb.nodes, 0, this.nodes, 0, n);
			this.nodes[n] = (edge.src.mark < 0) ? edge.src : edge.dst;
		} /* store the new node */
	} /* Embedding() */

	/*------------------------------------------------------------------*/
	/**
	 * Check whether two embeddings are equal.
	 * <p>
	 * This method is overridden only to avoid certain warnings.
	 * </p>
	 * 
	 * @param emb
	 *            the embedding to compare to
	 * @since 2007.11.06 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean equals(Object emb) {
		return this == emb;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Compute the hash code of the subgraph described by the embedding.
	 * <p>
	 * This function should yield the same value as the corresponding function
	 * <code>Graph.hashCode()</code>.
	 * </p>
	 * 
	 * @return the hash code of the subgraph described by the embedding
	 * @since 2006.11.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int hashCode() { /* --- compute a hash code */
		int i, k, n, t, u; /* loop variables, buffers */
		int h, s; /* the computed hash code */
		Node src, dst; /* to traverse the nodes */
		Edge edge; /* to traverse the edges */

		for (i = this.graph.edgecnt; --i >= 0;) {
			edge = this.graph.edges[i];
			if (edge.mark >= 0)
				edge.mark = -1;
		} /* unmark all allowed edges */
		/* It is not possible to simply unmark all edges (that is, mark */
		/* them with -1), because some of them may be marked with -2, */
		/* signaling that they may not be used for extensions anymore. */
		/* This marking has to be maintained for correct extensions. */
		for (i = this.edges.length; --i >= 0;) {
			edge = this.edges[i]; /* traverse the edges */
			edge.mark = 0; /* and mark them */
			edge.src.mark = edge.src.deg;
			edge.dst.mark = edge.dst.deg;
		} /* mark all incident nodes */
		/* The edge counters are copied into the node markers in order */
		/* to achieve a proper treatment of possibly existing chains. */
		for (n = 0, i = this.nodes.length; --i >= 0;) {
			src = this.nodes[i]; /* traverse the nodes */
			for (src.mark = 0, k = src.deg; --k >= 0;)
				if (src.edges[k].mark >= 0)
					src.mark++;
			n += src.mark; /* count the incident edges */
		} /* and sum these numbers */
		n = this.nodes.length + (this.edges.length - (n >> 1));
		h = s = 0; /* initialize the hash values */
		for (i = this.nodes.length; --i >= 0;) {
			src = this.nodes[i]; /* traverse the nodes */
			t = src.type + src.mark;
			for (k = src.deg; --k >= 0;) {
				edge = src.edges[k]; /* traverse the incident edges */
				if (edge.mark != 0)
					continue;
				dst = (edge.src != src) ? edge.src : edge.dst;
				u = (dst.type & ~Node.CHAIN) ^ dst.mark;
				t += src.type ^ (u + edge.type);
			} /* combine node types and degrees */
			h ^= t ^ (t << 9) ^ (t << 15);
			s += t; /* combine the computed values */
		} /* in two different ways */
		for (i = this.edges.length; --i >= 0;) {
			edge = this.edges[i]; /* traverse the edges */
			t = (edge.src.type & ~Node.CHAIN) ^ edge.src.mark;
			u = (edge.dst.type & ~Node.CHAIN) ^ edge.dst.mark;
			t = t + u + (t & u) + (t | u) + (t ^ u);
			h ^= t ^ (t << 11) ^ (t << 19);
			s += t += edge.type; /* combine node types and degrees */
			h ^= t ^ (t << 7) ^ (t << 17);
		} /* incorporate the edge type */
		h ^= n ^ this.edges.length; /* incorporate the total numbers */
		s += n + this.edges.length; /* of nodes and edges */
		h ^= s ^ (s << 15); /* combine the two hash codes */
		if (h < 0)
			h ^= -1; /* ensure a positive hash value */
		for (i = this.edges.length; --i >= 0;) {
			edge = this.edges[i]; /* traverse the edges */
			edge.src.mark = edge.dst.mark = edge.mark = -1;
		} /* unmark all incident nodes */
		return h; /* return the computed hash code */
	} /* hashCode() */

	public int size(){
		int n = 0;
		Embedding e2=null;
		for (e2=this; e2 != null; e2 = e2.succ)
			n++; /* count the embeddings */
		
		return n;
	}
	
	/*------------------------------------------------------------------*/
	/**
	 * Get the group of the underlying graph.
	 * 
	 * @return the group of the underlying graph
	 * @since 2007.08.10 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected int getGroup() {
		return (this.graph instanceof NamedGraph) ? ((NamedGraph) this.graph).group
				: NamedGraph.FOCUS;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Mark all nodes and edges with a given value.
	 * 
	 * @param mark
	 *            the value with which to mark nodes and edges
	 * @since 2002.04.14 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected void mark(int mark) { /* --- mark embedding in graph */
		for (int i = this.nodes.length; --i >= 0;)
			this.nodes[i].mark = mark;/* mark the nodes of the embedding */
		for (int i = this.edges.length; --i >= 0;)
			this.edges[i].mark = mark;/* mark the edges of the embedding */
	} /* mark() */

	/*------------------------------------------------------------------*/
	/**
	 * Mark all nodes and edges with their index.
	 * 
	 * @since 2002.04.14 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected void index() { /* --- index embedding in graph */
		for (int i = this.nodes.length; --i >= 0;)
			this.nodes[i].mark = i; /* number the nodes of the embedding */
		for (int i = this.edges.length; --i >= 0;)
			this.edges[i].mark = i; /* number the edges of the embedding */
	} /* index() */

	/*------------------------------------------------------------------*/
	/**
	 * Find the (maximum) length of a common edge array prefix.
	 * 
	 * @param emb
	 *            the embedding to compare to
	 * @return the number of edges that are common to the embeddings, or
	 *         <code>-1</code> if not even the root node coincides
	 * @since 2007.10.23 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected int common(Embedding emb) { /* --- find length of common prefix */
		if (this.nodes[0] != emb.nodes[0])
			return -1; /* compare the root node */
		int n = this.edges.length; /* get length of shorter edge array */
		if (n > emb.edges.length)
			n = emb.edges.length;
		for (int i = 0; i < n; i++)
			/* find first edge that differs */
			if (this.edges[i] != emb.edges[i])
				return i;
		return n; /* return length of shorter embedding */
	} /* common() */

	/*------------------------------------------------------------------*/
	/**
	 * Create all extensions of a given type of this embedding.
	 * <p>
	 * The type of the extension is described by the index of the source node
	 * (index in the embedding), the index of the destination node (which may be
	 * negative in order to indicate that the node is not yet part of the
	 * embedding), the type of the edge and the type of the destination node of
	 * the edge to add.
	 * </p>
	 * 
	 * @param src
	 *            the index of the source node
	 * @param dst
	 *            the index of the destination node (or -1 if it is not yet part
	 *            of the embedding)
	 * @param edge
	 *            the type of the extension edge
	 * @param node
	 *            the type of the destination node
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected Embedding extend(int src, int dst, int edge, int node) { /*
																		 * ---
																		 * create
																		 * extensions
																		 * of an
																		 * embed
																		 * .
																		 */
		int i; /* loop variable */
		Node s, d; /* to traverse the nodes */
		Edge e; /* to traverse the edges */
		Embedding emb, list = null; /* list of extended embeddings */

		this.index(); /* mark the base embedding */
		s = this.nodes[src]; /* get the source node of the ext. */
		for (i = s.deg; --i >= 0;) {
			e = s.edges[i]; /* traverse the unmarked edges */
			if (e.mark >= 0)
				continue;
			if (e.type < edge)
				break; /* compare the */
			if (e.type > edge)
				continue; /* edge type */
			d = (e.src != s) ? e.src : e.dst;
			if (d.type < node)
				break; /* compare destination */
			if (d.type > node)
				continue; /* node type */
			if (d.mark != dst)
				continue; /* and index */
			emb = new Embedding(this, e);
			emb.succ = list;
			list = emb;
		} /* add re-extension to the list */
		this.mark(-1); /* unmark the base embedding */
		return list; /* return the list of embeddings */
	} /* extend() */

	/*------------------------------------------------------------------*/
	/**
	 * Check whether this embedding overlaps another.
	 * 
	 * @param emb
	 *            the embedding to check for an overlap
	 * @param harmful
	 *            whether to check for a harmful overlap
	 * @return whether the embeddings overlap each other
	 * @since 2007.06.14 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected boolean overlaps(Embedding emb, boolean harmful) {
		return (harmful) ? this.overlapsHarmfully(emb) : this.overlaps(emb);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Check whether this embedding overlaps another.
	 * 
	 * @param emb
	 *            the embedding to check for an overlap
	 * @return whether the embeddings overlap each other
	 * @since 2007.06.12 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected boolean overlaps(Embedding emb) { /* --- check for overlap */
		int i, k; /* loop variables */

		if (emb.graph != this.graph)
			return false; /* check for the same graph */
		for (i = this.nodes.length; --i >= 0;)
			if (this.nodes[i] == emb.nodes[i])
				return true; /* check for identical node images */
		for (i = this.nodes.length; --i >= 0;)
			this.nodes[i].mark = 0; /* mark nodes of this embedding */
		for (k = emb.nodes.length; --k >= 0;)
			if (emb.nodes[k].mark == 0)
				break; /* check for a marked node */
		for (i = this.nodes.length; --i >= 0;)
			this.nodes[i].mark = -1; /* unmark nodes of this embedding */
		return (k >= 0); /* return whether embeddings overlap */
	} /* overlaps() */

	/*------------------------------------------------------------------*/
	/**
	 * Check whether this embedding overlaps another harmfully.
	 * <p>
	 * It is assumed that the two embeddings refer to the same fragment and thus
	 * have the same number of nodes and edges.
	 * </p>
	 * 
	 * @param emb
	 *            the embedding to check for an overlap
	 * @return whether the embeddings overlap each other harmfully
	 * @since 2007.06.13 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected boolean overlapsHarmfully(Embedding emb) { /*
														 * --- check for harmful
														 * overlap
														 */
		int i, k; /* loop variables */
		int n, m; /* number of nodes and edges */
		int s, d; /* buffers for node markers */
		Edge e; /* to traverse the edges */

		if (emb.graph != this.graph)
			return false; /* check for the same graph */
		for (i = n = this.nodes.length; --i >= 0;)
			if (this.nodes[i] == emb.nodes[i])
				return true; /* check for identical node images */
		for (i = m = this.edges.length; --i >= 0;)
			this.edges[i].mark = 0; /* mark the edges of this embedding */
		k = 0; /* default: there is no overlap */
		for (i = m; --i >= 0;) { /* remark the edges in the overlap */
			e = emb.edges[i];
			if (e.mark == 0)
				e.mark = k = 1;
		}
		for (i = m; --i >= 0;) { /* unmark the edges not in overlap */
			e = this.edges[i]; /* (keep markers only for overlap) */
			if (e.mark == 0)
				e.mark = -1;
			else if (emb.edges[i].mark >= 0)
				e.mark = 0;
		} /* remark edges in bijective mapping */
		if (k == 0)
			return false; /* if there is no overlap, abort */
		for (i = m; --i >= 0;) { /* unmark the edges that are not */
			e = this.edges[i]; /* in the constructed automorphism */
			if (e.mark > 0)
				e.mark = -1;
			else if (e.mark == 0)
				e.src.mark = e.dst.mark = 0;
		} /* mark nodes of edges with an image */
		/* After this loop all nodes and edges are marked that are part */
		/* of the subgraph for which the combination of one embedding */
		/* with the inverse of the other defines an automorphism. This */
		/* subgraph is the largest such subgraph, with the exception of */
		/* isolated nodes (since the construction is based on edges). */
		for (i = n; --i >= 0;)
			/* number the nodes of the subgraph */
			if (this.nodes[i].mark >= 0)
				this.nodes[i].mark = i;
		do { /* connected component marking loop */
			for (k = 0, i = m; --i >= 0;) {
				e = this.edges[i]; /* traverse the marked edges */
				if (e.mark != 0)
					continue;
				s = e.src.mark; /* set the markers of the */
				d = e.dst.mark; /* incident nodes to their minimum */
				if (s < d) {
					e.dst.mark = s;
					k++;
				} else if (s > d) {
					e.src.mark = d;
					k++;
				}
			} /* count the remarked nodes */
		} while (k > 0); /* while a marker was changed */
		/* With the above loop the nodes are marked in such a way that */
		/* two nodes have the same marker if and only if they are in */
		/* the same connected component of the constructed subgraph. */
		for (i = n; --i >= 0;) { /* traverse the nodes that are */
			k = emb.nodes[i].mark; /* incident to edges in the subgraph */
			if ((k >= 0) && (this.nodes[i].mark == k))
				break; /* if nodes are in same component, */
		} /* there is an equivalent subgraph */
		/* Without the do-while loop, k = emb.nodes[i].mark would be the */
		/* index of the node A to which the node with index i is mapped */
		/* by the embedding emb and the inverse of this embedding, that */
		/* is, k = this^-1(emb(i)). With the do-while loop, k is rather */
		/* the index of the connected component containing the node A. */
		/* this.nodes[i].mark is the index of the connected component of */
		/* the node with the index i. Therefore k == this.nodes[i].mark */
		/* means that the i-th node and its image under this^-1 and emb */
		/* are in the same connected component, hence a harmful overlap. */
		this.mark(-1); /* unmark nodes and edges */
		return (i >= 0); /* return whether embeddings overlap */
	} /* overlapsHarmfully() */

	/*------------------------------------------------------------------*/
	/**
	 * Main function for testing the overlap functions.
	 * 
	 * @param args
	 *            the command line arguments
	 * @since 2007.06.12 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public static void main(String args[]) { /* --- main function for testing */
		int n = 0; /* number of embeddings */
		Notation ntn; /* molecule notation */
		Graph graph, sub; /* graph and subgraph */
		Embedding embs, e1, e2; /* list of embeddings */

		if (args.length != 2) { /* if wrong number of arguments */
			System.out.println("java moss.Embedding <graph> <subgraph>");
			return; /* print a usage message */
		} /* and abort the program */
		ntn = new SMILES(); /* create a SMILES parser */
		try { /* parse the given graphs */
			graph = ntn.parse(new StringReader(args[0]));
			sub = ntn.parse(new StringReader(args[1]));
			graph.prepare();
			sub.prepareEmbed();
		} catch (IOException e) { /* catch and report a parse error */
			System.err.println(e.getMessage());
			return;
		}
		embs = graph.embed(sub); /* embed one graph into the other */
		for (e2 = embs; e2 != null; e2 = e2.succ)
			n++; /* count the embeddings */
		System.out.println(n + " embedding(s)");
		while (embs != null) { /* while there are embeddings */
			e1 = embs;
			embs = embs.succ;
			for (e2 = embs; e2 != null; e2 = e2.succ) {
				System.out.print(e1.overlaps(e2) + " ");
				System.out.println(e1.overlapsHarmfully(e2));
			} /* check all pairs of embeddings */
		} /* for (harmful) overlap */
	} /* main() */

} /* class Embedding */
