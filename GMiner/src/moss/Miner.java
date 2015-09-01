/*----------------------------------------------------------------------
  File    : Miner.java
  Contents: Find common substructures of graphs
  Author  : Christian Borgelt
  History : 2002.03.11 file created
            2002.03.15 main function added
            2002.03.18 search improved, output to files added
            2002.03.21 function output added, main program restructured
            2002.03.28 optional use of SYBYL line notation added
            2002.03.29 empty seed structures made possible
            2002.03.31 inversion of split at threshold made possible
            2002.04.03 immediate output of substructures added
            2002.04.04 option to ignore the edge type added
            2002.04.22 interpretation of options +/-: modified
            2002.05.01 search horizon problem fixed (maximum size)
            2002.07.15 pruning of equivalent branches added
            2003.02.21 option -C added (find variable length chains)
            2003.03.31 output of Prolog description added
            2003.08.01 perfect extension pruning added (option -P)
            2003.08.03 ring extensions added (options -r and -R)
            2003.08.04 ignore edge type only in rings added (option -B)
            2003.08.07 adapted to new classes, considerable rewrite
            2003.08.10 empty seeds, node type exclusion added (-x)
            2004.03.28 bug concerning output tokenizer fixed
            2004.04.05 bug in node sorting w.r.t. frequency fixed
            2005.07.21 graph encoding added, seedless search modified
            2005.07.22 graph trimming (removal and marking) added
            2005.07.24 setup restructured, canonical form pruning added
            2005.08.01 trimming of input lines and fields added
            2005.08.02 masking node and edge types made an extra phase
            2005.08.03 special search code for seed embedding removed
            2005.08.04 recursive search simplified, defaults changed
            2005.08.05 application of pruning criteria restructured
            2005.08.06 chain pruning simplified (minimum length)
            2005.08.08 combined perfect extension/canonical form pruning
            2005.08.10 initial fragments created through wrappers
            2005.08.11 adapted to second extension (rightmost path)
            2005.08.15 seed handling included in preparation functions
            2005.08.17 fragment creation modified, option -M added
            2005.08.23 bug in graph grouping fixed (NamedGraph.group)
            2005.09.02 adapted to modified packed embedding lists
            2006.03.01 run method and possibility to abort search added
            2006.04.04 main function restructured, new constructor
            2006.04.10 bug in function init fixed (seed parsing)
            2006.05.08 fragment adaptation, function record simplified
            2006.05.09 option -N added (normalized fragment output)
            2006.05.16 bug in graph identifier output fixed
            2006.05.17 option -O added (do not record open rings)
            2006.05.31 definition of constant MERGERINGS added
            2006.06.02 ring exts. combined with canonical form pruning
            2006.06.04 ring exts. combined with rightmost extensions
            2006.06.06 bug in combined rightmost/perfect exts. fixed
            2006.06.07 equivalent sibling pruning for ring extensions
            2006.06.08 bug in combined rightmost/perfect exts. fixed
            2006.06.19 closed fragment pruning for CLOSERINGS corrected
            2006.06.22 function duplicate added, record redesigned
            2006.06.26 miner initialization considerably restructured
            2006.06.28 some code cleanup, some functions reordered
            2006.06.29 input graph grouping moved to graph loading
            2006.07.03 comments allowed in input file (# at line start)
            2006.07.04 error reporting for graph loading improved
            2006.07.06 adapted to changes of class CanonicalForm
            2006.07.10 perfect extension pruning allowed for ring exts.
            2006.07.12 counters for repository comparisons added
            2006.07.13 bug in edge and node masking fixed
            2006.07.18 parameter setting restructured for class MoSS
            2006.08.04 revert extension information only if necessary
            2006.08.09 ignore the node type only in rings added
            2006.08.10 adapted to new class Recoder (node type coding)
            2006.08.12 adapted to new Notation classes
            2006.08.13 node types to exclude as seeds added (option -y)
            2006.10.26 function to set the basic parameters split
            2006.11.03 substructure repository turned into a hash table
            2006.11.10 repository of found fragments redesigned
            2007.02.15 output format for identifier lists changed
            2007.02.23 bug in function addGraph fixed (list of actives)
            2007.02.24 adapted to extended class Notation
            2007.03.01 counter for repository accesses added
            2007.03.02 SDfile format added for the input
            2007.03.05 graph loading moved to run function
            2007.03.23 bug in seed preparation fixed (node recoding)
            2007.03.27 perfect ext. pruning for rightmost exts. fixed
            2007.05.30 messages are printed to a log stream
            2007.06.21 new support types added, adapted to new classes
            2007.06.29 adapted to GraphReader and GraphWriter classes
            2007.07.06 command line argument evaluation modified
            2007.11.07 warning about failed ring marking added
            2010.01.21 plain search and recusion methods added
            2010.01.22 adapted to renaming of canonical form classes
            2010.01.27 embedding usage from a certain level downward
            2010.01.28 check whether minimum support can reached added
            2011.02.16 adapted to return value of CanonicalForm.init()
            2011.02.22 optional extension filtering with orbits added
            2011.03.01 optional generation of all extensions added
            2011.03.03 pruning with node equivalence classes added
            2011.06.20 reversed interpretation of option -K fixed
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.FileReader;
import java.io.StringReader;
import java.io.Writer;
import java.io.FileWriter;
import java.io.PrintStream;

/*--------------------------------------------------------------------*/
/**
 * Class for managing repository elements.
 * <p>
 * A repository element is a hash bin list element that records (an embedding
 * of) a substructure.
 * </p>
 * 
 * @author Christian Borgelt
 * @since 2006.11.10
 */
/*--------------------------------------------------------------------*/
class RepElem {

	/*------------------------------------------------------------------*/
	/* instance variables */
	/*------------------------------------------------------------------*/
	/** the graph referred to */
	protected Graph graph;
	/** the size of the substructure (number of nodes) */
	protected int size;
	/** the edges of (one embedding of) the substructure */
	protected Edge[] edges;
	/** the support of the substructure (in the focus) */
	protected int supp0;
	/** the support of the substructure (in the complement) */
	protected int supp1;
	/** the number of embeddings (in the focus) */
	protected int embs0;
	/** the number of embeddings (in the complement) */
	protected int embs1;
	/** the hash code of the substructure */
	protected int hash;
	/** the next element in the hash bin list */
	protected RepElem succ;

	/*------------------------------------------------------------------*/
	/**
	 * Create an element of a substructure repository.
	 * 
	 * @param frag
	 *            the fragment to store
	 * @param hash
	 *            the hash code of the substructure
	 * @since 2006.11.10 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected RepElem(Fragment frag, int hash) { /*
												 * --- create a repository
												 * element
												 */
		Embedding emb = frag.list;
		this.graph = emb.graph;
		this.size = emb.nodes.length;
		this.edges = emb.edges;
		this.supp0 = frag.supp[0];
		this.supp1 = frag.supp[1];
		this.embs0 = frag.supp[2];
		this.embs1 = frag.supp[3];
		this.hash = hash;
	} /* RepElem() */

} /* class RepElem */

/*--------------------------------------------------------------------*/
/**
 * Class for the molecular substructure miner.
 * 
 * @author Christian Borgelt
 * @since 2002.03.11
 */
/*--------------------------------------------------------------------*/
public class Miner implements Runnable {

	private static final long serialVersionUID = 0x00060006;

	/*------------------------------------------------------------------*/
	/* constants: version information */
	/*------------------------------------------------------------------*/
	/** the program description */
	public static final String DESCRIPTION = "molecular substructure miner (MoSS)";
	/** the version of this program */
	public static final String VERSION = "version 6.6 (2011.07.07)";
	/** the copyright information for this program */
	public static final String COPYRIGHT = "(c) 2002-2011 Christian Borgelt";

	/*------------------------------------------------------------------*/
	/* constants: message texts */
	/*------------------------------------------------------------------*/
	private static final String RING_WARN = "  warning: could not mark all rings in ";

	/*------------------------------------------------------------------*/
	/* constants: sizes and flags */
	/*------------------------------------------------------------------*/
	/** flag for extensions by single edges */
	public static final int EDGEEXT = CanonicalForm.EDGE;
	/** flag for extensions by rings */
	public static final int RINGEXT = CanonicalForm.RING;
	/** flag for extensions by chains */
	public static final int CHAINEXT = CanonicalForm.CHAIN;
	/** flag for extensions by equivalent variants of rings */
	public static final int EQVARS = CanonicalForm.EQVARS;
	/** flag for extension filtering with node orbits */
	public static final int ORBITS = CanonicalForm.ORBITS;
	/** flag for using node equivalence classes */
	public static final int CLASSES = CanonicalForm.CLASSES;
	/** flag for generating all extensions */
	public static final int ALLEXTS = CanonicalForm.ALLEXTS;
	/** flag for restriction to closed fragments */
	public static final int CLOSED = 0x000100;
	/** flag for filtering open rings */
	public static final int CLOSERINGS = 0x000200;
	/** flag for merging ring extensions with the same first edge */
	public static final int MERGERINGS = 0x000400;
	/** flag for full (unmerged) ring extensions */
	private static final int FULLRINGS = 0x000800;
	/** flag for pruning fragments with unclosable rings */
	public static final int PR_UNCLOSE = 0x001000;
	/** flag for partial perfect extension pruning */
	public static final int PR_PARTIAL = 0x002000;
	/** flag for full perfect extension pruning */
	public static final int PR_PERFECT = 0x004000;
	/** flag for equivalent sibling extension pruning */
	public static final int PR_EQUIV = 0x008000;
	/** flag for canonical form pruning */
	public static final int PR_CANONIC = 0x010000;
	/** flag for unembedding siblings of the current search tree nodes */
	public static final int UNEMBED = 0x020000;
	/** flag for normalized substructure output */
	public static final int NORMFORM = 0x040000;
	/** flag for verbose reporting */
	public static final int VERBOSE = 0x080000;
	/** flag for converting Kekul&eacute; representations */
	public static final int AROMATIZE = 0x100000;
	/** flag for conversion to another description format */
	public static final int TRANSFORM = 0x200000;
	/** flag for conversion to logic representation */
	public static final int LOGIC = 0x400000;
	/** flag for no search statistics output */
	public static final int NOSTATS = 0x800000;
	/**
	 * default search mode flags: edge extensions, embeddings, canonical form
	 * and full perfect extension pruning
	 */
	public static final int DEFAULT = EDGEEXT | PR_CANONIC | ORBITS | CLOSED
			| PR_PERFECT | AROMATIZE;

	/*------------------------------------------------------------------*/
	/* instance variables */
	/*------------------------------------------------------------------*/
	/** the search mode flags */
	protected int mode = DEFAULT;
	/** the type of support to use */
	protected int type = Fragment.GRAPHS;
	/** the minimum support in the focus as a fraction */
	protected double fsupp = 0.1F;
	/** the minimum support in the focus as an absolute value */
	protected int supp = 1;
	/** the maximum support in the complement as a fraction */
	protected double fcomp = 0.02F;
	/** the maximum support in the complement as an absolute value */
	protected int comp = 0;
	/** the minimum size of substructures to report (number of nodes) */
	protected int min = 0;
	/** the maximum size of substructures to report (number of nodes) */
	protected int max = Integer.MAX_VALUE;
	/** the minimum size of rings (number of nodes/edges) */
	protected int rgmin = 0;
	/** the maximum size of rings (number of nodes/edges) */
	protected int rgmax = 0;
	/** the masks for nodes and edges */
	protected int[] masks = null;
	/** the recoder for the node types */
	protected Recoder coder = null;
	/** the seed structure to start the search from */
	protected Graph seed = null;
	/** the excluded node types */
	protected Graph extype = null;
	/** the node types that are excluded as seeds */
	protected Graph exseed = null;
	/** the list of graphs to mine (database) */
	protected NamedGraph graphs = null;
	/** the current insertion point for the focus */
	protected NamedGraph curr = null;
	/** the tail of the list of graphs (insertion point for complement) */
	protected NamedGraph tail = null;
	/** the numbers of graphs in focus and complement */
	protected int[] cnts = null;
	/** the level at which to switch to embeddings */
	protected int emblvl = 0;
	/** the initial fragment (embedded seed structure) */
	protected Fragment frag = null;
	/** the maximum number of embeddings per graph */
	protected int maxepg = 0;
	/** the repository of processed substructures (hash table) */
	protected RepElem[] bins = null;
	/** the size of the repository (number of substructures) */
	protected int rsize = 0;
	/** the canonical form and restricted extension generator */
	protected CanonicalForm cnf = null;
	/** the canonical form for normalizing the output */
	protected CanonicalForm norm = null;
	/** the number of reported substructures */
	protected int subcnt = -1;
	/** the graph data set file reader */
	protected GraphReader reader = null;
	/** the threshold for the split into focus and complement */
	protected double thresh = 0.5;
	/** the group for graphs with a value below the threshold */
	protected int group = 0;
	/** the substructure file writer */
	protected GraphWriter writer = null;
	/** the identifier file writer */
	protected Writer wrids = null;
	/** stream to write progress messages to */
	protected PrintStream log = System.err;
	/** the error status for the search process */
	private Exception error = null;
	/** whether to abort the search thread */
	private volatile boolean stop = false;

	/* --- benchmark variables --- */
	/** for benchmarking: the maximum depth of the search tree */
	protected int maxdep;
	/** for benchmarking: the number of search tree nodes */
	protected long nodecnt;
	/** for benchmarking: the number of created fragments */
	protected long fragcnt;
	/** for benchmarking: the number of created embeddings */
	protected long embcnt;
	/** for benchmarking: insufficient support pruning counter */
	protected long lowsupp;
	/** for benchmarking: perfect extension pruning counter */
	protected long perfect;
	/** for benchmarking: equivalent frag. pruning counter */
	protected long equiv;
	/** for benchmarking: ring order pruning counter */
	protected long ringord;
	/** for benchmarking: canonical form pruning counter */
	protected long canonic;
	/** for benchmarking: duplicate fragments counter */
	protected long duplic;
	/** for benchmarking: non-closed fragments counter */
	protected long nonclsd;
	/** for benchmarking: open ring fragments counter */
	protected long openrgs;
	/** for benchmarking: invalid chains counter */
	protected long chains;
	/** for benchmarking: invalid fragments counter */
	protected long invalid;
	/** for benchmarking: the number of repository accesses */
	protected long repcnt;
	/** for benchmarking: the number of fragment comparisons */
	protected long cmpcnt;
	/** for benchmarking: the number of isomorphism tests */
	protected long isocnt;
	/** for benchmarking: the number of comparisons with embeddings */
	protected long embcmps;

	/*------------------------------------------------------------------*/
	/**
	 * Create an empty miner with default parameter settings.
	 * 
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Miner() { /* --- create a substructure finder */
		this.masks = new int[4]; /* init. the edge and node masks */
		this.masks[0] = this.masks[2] = Node.TYPEMASK;
		this.masks[1] = this.masks[3] = Edge.TYPEMASK;
		this.cnts = new int[2]; /* create the support counters */
	} /* Miner() */

	/*------------------------------------------------------------------*/
	/**
	 * Set the search mode.
	 * <p>
	 * The search mode is a combination of the search mode flags, e.g.
	 * <code>RINGEXT</code> or <code>PR_CANONIC</code>.
	 * </p>
	 * 
	 * @param mode
	 *            the search mode
	 * @since 2006.10.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setMode(int mode) {
		this.mode = mode;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the support type.
	 * <p>
	 * Constants for support types are defined in the class
	 * <code>Fragment</code>.
	 * </p>
	 * 
	 * @param type
	 *            the support type to use
	 * @see Fragment
	 * @since 2006.06.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setType(int type) {
		this.type = type;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the support limits.
	 * <p>
	 * Positive values are fractions of the focus or complement set, negative
	 * values are absolute numbers.
	 * </p>
	 * 
	 * @param supp
	 *            the minimum support in the focus
	 * @param comp
	 *            the maximum support in the complement
	 * @since 2006.10.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setLimits(double supp, double comp) {
		this.fsupp = supp;
		this.fcomp = comp;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the minimum and maximum fragment size.
	 * 
	 * @param min
	 *            the minimum fragment size (number of nodes)
	 * @param max
	 *            the maximum fragment size (number of nodes)
	 * @since 2006.10.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setSizes(int min, int max) {
		this.min = min;
		this.max = (max <= 0) ? Integer.MAX_VALUE : max;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the minimum and maximum ring size.
	 * 
	 * @param min
	 *            the minimum ring size (number of nodes/edges)
	 * @param max
	 *            the maximum ring size (number of nodes/edges)
	 * @since 2006.10.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setRingSizes(int min, int max) {
		this.rgmin = (min < 0) ? 0 : min;
		this.rgmax = (max >= this.min) ? max : this.min;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the node and edge masks.
	 * 
	 * @param node
	 *            the mask for nodes outside (marked) rings
	 * @param edge
	 *            the mask for edges outside (marked) rings
	 * @param ringnode
	 *            the mask for nodes in (marked) rings
	 * @param ringedge
	 *            the mask for edges in (marked) rings
	 * @since 2006.06.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setMasks(int node, int edge, int ringnode, int ringedge) { /*
																			 * ---
																			 * set
																			 * node
																			 * and
																			 * edge
																			 * masks
																			 */
		this.masks[0] = node; /* mask for node types outside rings */
		this.masks[1] = edge; /* mask for edge types outside rings */
		this.masks[2] = ringnode; /* mask for node types inside rings */
		this.masks[3] = ringedge; /* mask for edge types inside rings */
	} /* setMasks() */

	/*------------------------------------------------------------------*/
	/**
	 * Set the embeddings parameters.
	 * <p>
	 * Restricting the maximum number of embeddings per graph can reduce the
	 * amount of memory needed in the search, but slows down the operation
	 * (sometimes considerably).
	 * </p>
	 * 
	 * @param level
	 *            the level at which to switch to embeddings
	 * @param maxepg
	 *            the maximum number of embeddings per graph
	 * @since 2010.01.27 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setEmbed(int level, int maxepg) { /*
												 * --- set the embedding
												 * parameters
												 */
		this.emblvl = (level < 0) ? Integer.MAX_VALUE : level;
		this.maxepg = maxepg; /* note the embedding level and */
	} /* setEmbed() *//* the maximum number per graph */

	/*------------------------------------------------------------------*/
	/**
	 * Set the excluded nodes and excluded seeds.
	 * <p>
	 * Excluded nodes are completely removed from the search, that is, no
	 * substructure containing such an node will be reported. Nodes that are
	 * only excluded as seeds may appear in reported fragments, but are not used
	 * as seeds. This can be useful, for example, in the case where carbon is
	 * the most frequent element and one is not interested in fragments
	 * containing only carbon nodes.
	 * </p>
	 * 
	 * @param extype
	 *            the node types to exclude from the search
	 * @param exseed
	 *            the node types to exclude as seeds
	 * @since 2006.06.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setExcluded(Graph extype, Graph exseed) {
		this.extype = extype;
		this.exseed = exseed;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the excluded nodes and excluded seeds.
	 * <p>
	 * The arguments <code>exat</code> and <code>exsd</code> are parsed as graph
	 * descriptions in the notation given by the argument <code>format</code>.
	 * </p>
	 * 
	 * @param extype
	 *            the description of the excluded nodes
	 * @param exseed
	 *            the description of the nodes to exclude as seeds
	 * @param format
	 *            the format of the descriptions
	 * @since 2006.06.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setExcluded(String extype, String exseed, String format)
			throws IOException { /* --- set the excluded node types */
		Notation ntn = Notation.createNotation(format);
		String s = (ntn instanceof MoleculeNtn) ? "atom" : "node";
		if (!ntn.hasFixedTypes()) /* create and configure notation */
			ntn.setTypeMgrs(this.configNtns());
		if ((extype == null) || extype.equals(""))
			this.extype = null; /* if no excluded node types given, */
		else { /* clear the graph, otherwise */
			this.log.print("parsing excluded " + s + " types ... ");
			this.extype = ntn.parse(new StringReader(extype));
			this.log.println("[" + this.extype.getNodeCount() + " " + s
					+ "(s)] done.");
		} /* parse the excluded nodes */
		if ((exseed == null) || exseed.equals(""))
			this.exseed = null; /* if no excluded seed types given, */
		else { /* clear the graph, otherwise */
			this.log.print("parsing excluded seed types ... ");
			this.exseed = ntn.parse(new StringReader(exseed));
			this.log.println("[" + this.exseed.getNodeCount() + " " + s
					+ "(s)] done.");
		} /* parse the excluded seeds */
	} /* setExcluded() */

	/*------------------------------------------------------------------*/
	/**
	 * Set the seed structure to start the search from.
	 * 
	 * @param seed
	 *            the seed structure for the search
	 * @since 2006.06.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setSeed(Graph seed) throws IOException { /*
														 * --- set the seed
														 * structure
														 */
		if (!seed.isConnected()) /* check for a connected seed */
			throw new IOException("error: seed structure is not connected");
		this.seed = seed; /* note the seed structure */
	} /* setSeed() */

	/*------------------------------------------------------------------*/
	/**
	 * Set the seed structure to start the search from.
	 * <p>
	 * The argument <code>desc</code> is parsed as graph description in the
	 * notation given by the argument <code>format</code>.
	 * </p>
	 * 
	 * @param desc
	 *            the description of the seed structure
	 * @param format
	 *            the format of the seed description
	 * @since 2006.06.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setSeed(String desc, String format) throws IOException { /*
																		 * ---
																		 * set
																		 * the
																		 * seed
																		 * structure
																		 */
		if ((desc == null) || desc.equals("") || desc.equals("*")
				|| desc.equals(".")) {
			this.seed = null;
			return;
		}
		this.log.print("parsing seed description ... ");
		Notation ntn = Notation.createNotation(format);
		String n = (ntn instanceof MoleculeNtn) ? "atom" : "node";
		String e = (ntn instanceof MoleculeNtn) ? "bond" : "edge";
		if (!ntn.hasFixedTypes()) /* create and configure notation */
			ntn.setTypeMgrs(this.configNtns());
		this.setSeed(ntn.parse(new StringReader(desc)));
		this.log.println("[" + this.seed.getNodeCount() + " " + n + "(s)"
				+ ", " + this.seed.getEdgeCount() + " " + e + "(s)] done.");
	} /* setSeed() */

	/*------------------------------------------------------------------*/
	/**
	 * Set the grouping parameters.
	 * <p>
	 * If <code>invert == false</code>, all graphs having an associated value
	 * smaller than the threshold <code>thresh</code> are placed into the focus
	 * and all other graphs are the complement. If <code>invert == true</code>,
	 * this split is inverted, that is, all graphs having an associated value no
	 * less than the threshold <code>thresh</code> are placed into the focus and
	 * all other graphs are the complement.
	 * </p>
	 * 
	 * @param thresh
	 *            the threshold for the grouping
	 * @param invert
	 *            whether to invert the grouping
	 * @since 2007.03.05 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setGrouping(double thresh, boolean invert) { /*
															 * --- set the
															 * grouping
															 * parameters
															 */
		this.thresh = thresh; /* note threshold and group index */
		this.group = (invert) ? 1 : 0;
	} /* setGrouping() */

	/*------------------------------------------------------------------*/
	/**
	 * Sets the stream to which progress messages are written.
	 * <p>
	 * By default all messages are written to {@link System#err}.
	 * </p>
	 * 
	 * @param stream
	 *            the stream to write to
	 * @since 2007.05.30 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setLog(PrintStream stream) {
		this.log = stream;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the input reader.
	 * 
	 * @param reader
	 *            the reader from which to read the graphs
	 * @since 2007.03.05 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setInput(GraphReader reader) {
		this.reader = reader;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the input reader.
	 * 
	 * @param fname
	 *            the name of the input data file
	 * @param format
	 *            the format of the input data
	 * @since 2007.03.05 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setInput(String fname, String format) throws IOException {
		this.reader = GraphReader.createReader(new FileReader(fname),
				GraphReader.GRAPHS, format);
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the output writer.
	 * 
	 * @param writer
	 *            the writer to write the found substructures
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setOutput(GraphWriter writer) {
		this.writer = writer;
		this.wrids = null;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the output writers.
	 * 
	 * @param writer
	 *            the writer to write the found substructures
	 * @param wrids
	 *            the writer to write the graph identifiers
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setOutput(GraphWriter writer, Writer wrids) {
		this.writer = writer;
		this.wrids = wrids;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Set the output writer.
	 * 
	 * @param fname
	 *            the name of the file for the found substructures
	 * @param format
	 *            the format for the output
	 * @since 2007.07.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setOutput(String fname, String format) throws IOException { /*
																			 * ---
																			 * set
																			 * the
																			 * output
																			 */
		this.writer = GraphWriter.createWriter(new FileWriter(fname),
				GraphWriter.SUBS, format);
		this.wrids = null;
	} /* setOutput() */

	/*------------------------------------------------------------------*/
	/**
	 * Set the output writers.
	 * 
	 * @param fn_sub
	 *            the name of the file for the found fragments
	 * @param fn_ids
	 *            the name of the file for the graph identifiers
	 * @param format
	 *            the format for the output
	 * @since 2006.06.26 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setOutput(String fn_sub, String format, String fn_ids)
			throws IOException { /* --- set the output */
		this.writer = GraphWriter.createWriter(new FileWriter(fn_sub),
				GraphWriter.SUBS, format);
		this.wrids = ((fn_ids != null) && !fn_ids.equals("")) ? new FileWriter(
				fn_ids) : null;
	} /* setOutput() */

	/*------------------------------------------------------------------*/
	/**
	 * Set the canonical form.
	 * 
	 * @param cnf
	 *            the canonical form to set
	 * @since 2009.08.04 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void setCnF(CanonicalForm cnf) {
		this.cnf = cnf;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Add a graph to the database.
	 * <p>
	 * When the graph is added, its group is evaluated and it is added to the
	 * list in such a way that all focus graphs are at the beginning of the list
	 * and all complement graphs at the end. Hence the group of a graph must not
	 * be changed after it has been added to a miner. Note that the order in
	 * which the graphs are added is preserved in the focus and the complement
	 * lists.
	 * </p>
	 * 
	 * @param graph
	 *            the graph to add
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void addGraph(NamedGraph graph) { /* --- add a graph to the database */
		this.cnts[graph.group]++; /* count the new graph */
		if (this.graphs == null) { /* if this is the first graph, */
			graph.succ = null; /* initialize the graph list */
			this.graphs = this.curr = this.tail = graph;
		} else if (graph.group > 0) { /* if the graph is in the complement */
			graph.succ = null; /* append the graph at the end */
			this.tail.succ = graph; /* of the graph list (ensure that */
			this.tail = graph;
		} /* focus always precedes complement) */
		else if (this.graphs.group > 0) {
			graph.succ = this.curr;
			this.graphs = graph; /* if first graph in the focus, */
			this.curr = graph;
		} /* store it at the head of the list */
		else { /* if not first graph in the focus */
			if (this.tail == this.curr)
				this.tail = graph; /* replace tail of list if necessary */
			graph.succ = this.curr.succ;
			this.curr.succ = graph; /* append the graph at the end */
			this.curr = graph; /* of the focus graph list */
		}
	} /* addGraph() */

	/*------------------------------------------------------------------*/
	/**
	 * Configure the notations of the miner.
	 * 
	 * @return a configured notation (type managers)
	 * @since 2007.07.06 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private Notation configNtns() { /* --- configure notations */
		Notation out = this.writer.getNotation();
		Notation in = (this.graphs != null) ? this.graphs.getNotation()
				: this.reader.getNotation();
		if (!out.hasFixedTypes()) /* if output types are not fixed, */
			out.setTypeMgrs(in); /* transfer the input types */
		else if (!in.hasFixedTypes()) /* if input types are not fixed, */
			in.setTypeMgrs(out); /* set the fixed types for the input */
		return in; /* return the configured notation */
	} /* configNtns() */

	/*------------------------------------------------------------------*/
	/**
	 * Convert Kekul&eacute; representations to true aromatic rings.
	 * <p>
	 * In a Kekul&eacute; representation an aromatic ring with six bonds is
	 * coded with alternating single and double bonds. In this function such
	 * Kekul&eacute; representations are found and turned into true aromatic
	 * rings (actual aromatic bonds) for all molecules of the database.
	 * </p>
	 * 
	 * @return the number of modified graphs
	 * @since 2003.08.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int aromatize() { /* --- convert Kekul\'e to aromatic */
		int n = 0; /* number of modified graphs */
		if (this.seed != null) /* if there is a seed, */
			BondTypeMgr.aromatize(this.seed); /* process it */
		for (NamedGraph g = this.graphs; g != null; g = g.succ)
			if (BondTypeMgr.aromatize(g) > 0)
				n++;
		return n; /* return number of mod. graphs */
	} /* aromatize() */

	/*------------------------------------------------------------------*/
	/**
	 * Mask the node and edge types of all graphs.
	 * 
	 * @return the number of processed graphs
	 * @since 2002.03.28 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int maskTypes() { /* --- mask node and edge types */
		if (this.seed != null) /* if there is a seed, mask types */
			this.seed.maskTypes(this.masks);
		for (NamedGraph g = this.graphs; g != null; g = g.succ)
			g.maskTypes(this.masks); /* mask types in all graphs */
		return this.cnts[0] + this.cnts[1];
	} /* maskTypes() *//* return the number of graphs */

	/*------------------------------------------------------------------*/
	/**
	 * Mark rings in a given size range in all graphs.
	 * <p>
	 * For ring extensions the rings have to be marked in the graphs, so that
	 * they can easily be found during the search.
	 * </p>
	 * 
	 * @param min
	 *            the minimum ring size (number of edges/nodes)
	 * @param max
	 *            the maximum ring size (number of edges/nodes)
	 * @return the number graphs with of marked rings
	 * @since 2003.08.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int markRings(int min, int max) { /* --- mark rings in all graphs */
		int n = 0, k = 0, r; /* number of graphs, rings */
		if (max > 256)
			max = 256; /* check and adapt */
		if (min > max)
			min = max; /* the ring size range */
		if ((this.seed != null) /* if there is a seed, mark rings */
				&& (this.seed.markRings(min, max) < 0))
			this.log.println(RING_WARN + "seed");
		for (NamedGraph g = this.graphs; g != null; g = g.succ) {
			r = g.markRings(min, max); /* traverse the graphs and mark rings */
			if (r != 0)
				n++; /* count the graphs with rings */
			if (r >= 0)
				continue; /* check for successful marking */
			if (k++ <= 0)
				this.log.println();
			this.log.println(RING_WARN + "\"" + g.getName() + "\"");
		} /* warn about failures */
		return n; /* return the number of mod. graphs */
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
	 *            the maximum ring size (number of edges/nodes)
	 * @return the number of graphs with marked pseudo-rings
	 * @since 2006.06.04 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int markPseudo(int max) { /* --- mark pseudo-rings in all mols. */
		int n = 0; /* number of graphs */
		if (max > 256)
			max = 256; /* check and adapt the ring size */
		for (NamedGraph g = this.graphs; g != null; g = g.succ)
			if (g.markPseudo(max) > 0)
				n++;
		return n; /* return the number of mod. graphs */
	} /* markPseudo() */

	/*------------------------------------------------------------------*/
	/**
	 * Mark bridges in all graphs of the database.
	 * <p>
	 * Bridges have to be marked for correct perfect extension pruning, because
	 * only bridges and edges closing rings are candidates for perfect
	 * extensions.
	 * </p>
	 * 
	 * @return the number of graphs with marked bridges
	 * @since 2005.06.07 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int markBridges() { /* --- mark bridges in all graphs */
		int n = 0; /* number of graphs */
		if (this.seed != null) /* if there is a seed, */
			this.seed.markBridges(); /* mark the bridges in it */
		for (NamedGraph g = this.graphs; g != null; g = g.succ)
			if (g.markBridges() > 0)
				n++;
		return n; /* return the number of mod. graphs */
	} /* markBridges() */

	/*------------------------------------------------------------------*/
	/**
	 * Split all graphs into their connected components.
	 * <p>
	 * Note that the numbers of graphs in the focus and in the complement are
	 * <b>not<b> updated.
	 * </p>
	 * 
	 * @return the total number of connected components
	 * @since 2007.06.14 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int split() { /* --- split into connected comps. */
		NamedGraph list; /* to traverse the graphs */
		NamedGraph ccs; /* connected components */
		int n = 0; /* number of connected components */

		list = this.graphs; /* get the graphs and reinitialize */
		this.graphs = this.tail = this.curr = null;
		while (list != null) { /* while there are more graphs */
			ccs = list.split(); /* split next graph into components */
			if ((ccs.group > 0) && (this.curr == null))
				this.curr = ccs; /* note first graph in complement */
			if (this.tail == null)
				this.graphs = ccs;
			else
				this.tail.succ = ccs;
			n++; /* count the connected components */
			if (ccs != list) /* append components to the output */
				while (ccs.succ != null) {
					ccs = ccs.succ;
					n++;
				}
			this.tail = ccs; /* skip the appended components, */
			list = list.succ; /* note the new output list tail, */
		} /* and skip the processed graph */
		return n; /* return the number of components */
	} /* split() */

	/*------------------------------------------------------------------*/
	/**
	 * Print a counter.
	 * 
	 * @param n
	 *            the counter to print
	 * @since 2007.07.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private void print(int n) { /* --- print a counter */
		String s = "        " + n; /* format the number and print it */
		this.log.print(s.substring(s.length() - 9));
		this.log.print("\b\b\b\b\b\b\b\b\b");
	} /* print() */

	/*------------------------------------------------------------------*/
	/**
	 * Set up the miner (prepare the graph database for the search).
	 * 
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private void setup() { /* --- set up the miner */
		int i, t; /* loop variable, type buffer */
		NamedGraph graph; /* to traverse the graphs */
		Node node; /* to traverse the nodes */
		boolean f; /* flag for frequency-based support */

		t = this.type & Fragment.SUPPMASK;
		f = (t != Fragment.GRAPHS); /* check the support type */
		if (f) { /* if not frequency-based support */
			this.cnts[0] = this.cnts[1] = 0;
			for (graph = this.graphs; graph != null; graph = graph.succ)
				this.cnts[graph.group] += graph.nodecnt;
			this.split(); /* sum the number of nodes and */
		} /* split graphs into connected comps. */
		if (this.fsupp < 0)
			this.supp = (int) Math.ceil(-this.fsupp);
		else
			this.supp = (int) Math.ceil(this.fsupp * this.cnts[0]);
		if (this.fcomp < 0)
			this.comp = (int) Math.floor(-this.fcomp);
		else
			this.comp = (int) Math.floor(this.fcomp * this.cnts[1]);
		if (this.supp <= 0) /* compute and adapt the */
			this.supp = 1; /* absolute support values */

		this.coder = new Recoder(); /* create a node type recoder */
		for (graph = this.graphs; graph != null; graph = graph.succ) {
			if (graph.group != 0)
				break; /* traverse the graphs */
			for (i = graph.nodecnt; --i >= 0;) {
				node = graph.nodes[i]; /* traverse the nodes and */
				if (!node.isSpecial()) /* count the elements in the focus */
					this.coder.count(this.coder.add(node.type));
			} /* commit after each graph */
			this.coder.commit(); /* (to determine the type support) */
		}
		if (this.extype != null) { /* if specific nodes are excluded */
			for (i = this.extype.nodecnt; --i >= 0;) {
				node = this.extype.nodes[i];
				if (node.isSpecial())
					continue;
				t = this.coder.encode(node.type);
				if (t >= 0)
					this.coder.exclude(t);
			} /* mark the excluded nodes */
		}
		if (this.exseed != null) { /* if specific seeds are excluded */
			for (i = this.exseed.nodecnt; --i >= 0;) {
				node = this.exseed.nodes[i];
				if (node.isSpecial())
					continue;
				t = this.coder.encode(node.type);
				if (t >= 0)
					this.coder.maximize(t);
			} /* mark the excluded seeds */
		}
		this.coder.trim(f, this.supp); /* trim the set of elements */
		if ((this.seed != null) /* if the seed has only one node */
				&& (this.seed.nodecnt == 1)) {
			node = this.seed.nodes[0];
			if (!node.isSpecial()) {
				t = this.coder.encode(node.type);
				if (t >= 0)
					this.coder.clear(t);
			} /* clear frequency of the seed */
		} /* (must precede other node types) */
		this.coder.sort(); /* sort the elements by frequency */
		for (graph = this.graphs; graph != null; graph = graph.succ) {
			graph.encode(this.coder); /* encode the graphs, */
			graph.trim(true); /* trim excluded node types, */
			graph.prepare(); /* (re)prepare the graph, */
			graph.mark(-1); /* and clear all markers */
		}
		if (this.seed != null) /* if there is a seed structure, */
			this.seed.encode(this.coder); /* encode the seed's nodes */
	} /* setup() */

	/*------------------------------------------------------------------*/
	/**
	 * Embed the seed structure into all graphs.
	 * 
	 * @return the number of graphs that contain the seed
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int embed() { /* --- embed seed into all graphs */
		int n = 0; /* graph counter */
		NamedGraph graph; /* to traverse the graphs */

		if (this.seed == null) /* check for an empty seed */
			return 0; /* and abort if there is none */
		this.seed.prepareEmbed(); /* prepare seed for embedding */
		this.frag = new Fragment(this.seed, this.maxepg);
		for (graph = this.graphs; graph != null; graph = graph.succ) {
			if (this.emblvl <= 0) /* if to use embeddings, collect them */
				this.frag.addEmb(graph.embed(this.seed));
			else if (graph.contains(this.frag.graph))
				this.frag.addGraph(graph); /* collect the containing graphs */
			if ((++n & 0xff) == 0) /* count the processed graph and */
				this.print(n); /* print the number of graphs */
		}
		return n; /* return the number of graphs */
	} /* embed() */

	/*------------------------------------------------------------------*/
	/**
	 * Reorganize the substructure repository.
	 * <p>
	 * The hash table of the substructure repository is enlarged and the
	 * substructures are rehashed to achieve faster access.
	 * </p>
	 * 
	 * @since 2006.11.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private void rehash() { /* --- reorganize the repository */
		int i, k; /* loop variable, hash bin index */
		RepElem b[]; /* buffer for old hash bin array */
		RepElem e; /* to traverse the hash bin lists */

		b = this.bins; /* note the old hash bin array and */
		k = (b.length << 1) + 1; /* compute the new hash table size */
		this.bins = new RepElem[k]; /* allocate a new hash bin array */
		for (i = b.length; --i >= 0;) {
			while (b[i] != null) { /* traverse the nonempty bins */
				e = b[i];
				b[i] = e.succ;/* of the old hash bin array */
				e.succ = this.bins[k = e.hash % this.bins.length];
				this.bins[k] = e; /* add the element at the head */
			} /* of the approriate hash bin list */
		} /* in the new hash bin array */
	} /* rehash() */

	/*------------------------------------------------------------------*/
	/**
	 * Check for whether a given fragment has already been processed.
	 * <p>
	 * If no canonical form pruning is used, a repository of already processed
	 * fragments is maintained in order to avoid redundant search. Every new
	 * fragment is checked against this repository and is discarded if it is
	 * found, because then it has already been processed. If the given fragment
	 * could not be found in the repository, it is added to the repository.
	 * </p>
	 * 
	 * @param frag
	 *            the fragment to check against the repository
	 * @return whether the fragment is contained in the repository
	 * @since 2006.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private boolean duplicate(Fragment frag) { /*
												 * --- check for a duplicate
												 * fragment
												 */
		int k; /* loop variable */
		int hash, i; /* hash code and hash bin index */
		RepElem re; /* to traverse the bin list */
		Embedding emb; /* to traverse the embeddings */
		boolean found = false; /* whether the fragment was found */

		this.repcnt++; /* count the repository access */
		hash = frag.hashCode(); /* compute the hash code and */
		i = hash % this.bins.length;/* from it the hash bin index */
		for (re = this.bins[i]; re != null; re = re.succ) {
			this.cmpcnt++; /* traverse the hash bin list */
			emb = frag.list; /* get the list of embeddings */
			if ((re.hash != hash) || (re.graph != emb.graph)
					|| (re.supp0 != frag.supp[0]) || (re.supp1 != frag.supp[1])
					|| (re.embs0 != frag.supp[2]) || (re.embs1 != frag.supp[3])
					|| (re.size != emb.nodes.length)
					|| (re.edges.length != emb.edges.length))
				continue; /* do simple comparisons first */
			this.isocnt++; /* count the isomorphism test */
			for (k = re.edges.length; --k >= 0;)
				re.edges[k].mark = 0; /* mark the substructure */
			while ((emb != null) && (emb.graph == re.graph)) {
				this.embcmps++; /* count the embedding */
				for (k = emb.edges.length; --k >= 0;)
					if (emb.edges[k].mark != 0)
						break;
				if (k < 0) {
					found = true;
					break;
				}
				emb = emb.succ; /* if any embedding is fully marked, */
			} /* the fragment is a duplicate */
			for (k = re.edges.length; --k >= 0;)
				re.edges[k].mark = -1; /* unmark the substructure */
			if (found)
				return true; /* in the underlying graph */
		} /* and evaluate the test result */
		re = new RepElem(frag, hash);
		re.succ = this.bins[i]; /* add the new substructure */
		this.bins[i] = re; /* at the head of the hash bin list */
		if (++this.rsize > this.bins.length)
			this.rehash(); /* reorganize hash table if necessary */
		return false; /* return 'no duplicate' */
	} /* duplicate() */

	/*------------------------------------------------------------------*/
	/**
	 * Check and report a found fragment/substructure.
	 * <p>
	 * In order to be actually reported (written to the output file), the
	 * fragment must be valid (<code>Fragment.isValid()</code>), meet the
	 * maximum support requirement for the complement part of the database, be
	 * closed (<code>Fragment.isClosed()</code>) and must not have open rings if
	 * only fragments with closed rings are to be reported.
	 * </p>
	 * 
	 * @param frag
	 *            the fragment to report
	 * @return whether the fragment has been reported
	 * @since 2002.03.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected boolean output(Fragment frag) throws IOException { /*
																 * --- output a
																 * substructure
																 */
		int id; /* substructure identifier */
		Graph sub; /* fragment as a graph */
		Graph g; /* to traverse the graphs */
		double s; /* support of a substructure */

		/* --- check the fragment --- */
		if (!frag.isValid()) { /* skip invalid fragments */
			this.invalid++;
			return false;
		} /* (non-canonic, but needed) */
		if ((frag.size() < this.min) || (frag.supp[1] > this.comp)) /*
																	 * check
																	 * fragment
																	 * size
																	 */
			return false; /* and support in complement */
		if (((this.mode & CLOSED) != 0) && !frag.isClosed(this.cnf)) { /*
																		 * skip
																		 * non
																		 * -closed
																		 * fragments
																		 */
			this.nonclsd++;
			return false;
		}
		if (((this.mode & CLOSERINGS) != 0)
				&& frag.hasOpenRings(this.rgmin, this.rgmax)) {
			this.openrgs++;
			return false;
		} /* skip frags. with open rings */
		if (((this.mode & CHAINEXT) != 0) && !frag.chainsValid()) { /*
																	 * skip
																	 * fragments
																	 * with
																	 * invalid
																	 * chains
																	 */
			this.chains++;
			return false;
		} /* (one length or minimum != 1) */
		id = ++this.subcnt; /* count the new substructure and */
		this.print(this.subcnt); /* print the number of substructures */

		/* --- write substructure file --- */
		sub = frag.getGraph(); /* get fragment as a graph */
		if ((this.mode & NORMFORM) != 0) {
			sub = new Graph(sub); /* if to normalize the output, */
			sub.decode(); /* decode a copy of the fragment */
			if (this.norm == null)
				this.norm = new CnFBreadth1();
			sub.normalize(this.norm); /* normalize the fragment w.r.t */
		} /* maximum source extensions */
		this.writer.setName("" + id); /* set the substructure identifier */
		this.writer.setGraph(sub); /* and the description of the graph */
		s = this.cnts[0]; /* set support in the focus */
		s = (s != 0) ? frag.supp[0] / s : 1.0;
		this.writer.setAbsSupp(frag.supp[0]);
		this.writer.setRelSupp((float) (s * 100.0));
		s = this.cnts[1]; /* set support in the complement */
		s = (s != 0) ? frag.supp[1] / s : 0.0;
		this.writer.setAbsCompl(frag.supp[1]);
		this.writer.setRelCompl((float) (s * 100.0));
		this.writer.writeGraph(); /* write the substructure and */
		this.writer.flush(); /* flush the substructure writer */

		/* --- write graph identifier file --- */
		if (this.wrids == null) /* if there is no identifier file, */
			return true; /* return 'fragment was reported' */
		this.wrids.write(id + ":"); /* write the substructure identifier */
		for (g = frag.firstGraph(); g != null; g = frag.nextGraph()) {
			if (id < 0)
				this.wrids.write(',');
			id = -1;
			this.wrids.write(((NamedGraph) g).name);
		} /* list the graph identifiers */
		this.wrids.write('\n'); /* terminate the output line */
		this.wrids.flush(); /* flush the identifier writer */
		return true; /* return 'fragment was reported' */
	} /* output() */

	/*------------------------------------------------------------------*/
	/**
	 * Main recursive function of the mining process.
	 * <p>
	 * In this function the extensions of the given fragment are created and the
	 * resulting set of extended fragments is pruned with different methods.
	 * Finally the remaining fragments are processed recursively.
	 * </p>
	 * 
	 * @param frag
	 *            the current fragment (to be extended)
	 * @param depth
	 *            the current recursion depth
	 * @return whether to continue the search (search not aborted)
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private boolean recEmbed(Fragment frag, int depth) throws IOException { /*
																			 * ---
																			 * recursive
																			 * search
																			 * with
																			 * embeds
																			 * .
																			 */
		int i, k, n, r; /* loop variables, buffers */
		Embedding emb; /* to traverse the embeddings */
		Fragment[] xfs, vec; /* sorted list of created fragments */
		int cnt, size; /* number of fragments and array size */
		boolean chain; /* flag for a chain extension */
		boolean revert; /* whether to revert extension info. */
		boolean adapt; /* whether to adapt code words */
		boolean part, check; /* flags for the canonical form test */

		if (Thread.currentThread().isInterrupted())
			this.stop = true; /* check for thread interruption */
		if (this.stop) /* check for an external abort */
			return false; /* (if running as a separate thread) */
		this.nodecnt++; /* count the search tree node */
		if (depth > this.maxdep) /* update the maximal depth */
			this.maxdep = depth; /* of the recursion/search tree */

		/* --- verbose information output --- */
		if ((this.mode & VERBOSE) != 0) {
			for (i = depth; --i >= 0;)
				/* if verbose output about */
				System.out.print("   ");/* the search is requested */
			System.out.print(frag); /* print the fragment */
			System.out.print("  "); /* and a separator */
			Graph g = null; /* buffer for the current graph */
			k = 1; /* init. the embedding counter */
			for (emb = frag.firstEmb(); emb != null; emb = frag.nextEmb()) {
				if (emb.graph == g) { /* if embedding in same graph */
					k++;
					continue;
				} /* count embeddings per graph */
				if (k > 1) /* if more than one embedding */
					System.out.print(k); /* print the number of embeddings */
				k = 1; /* reinit. the embedding counter */
				g = emb.graph; /* and note the new graph */
				System.out.print(((NamedGraph) g).name);
			} /* print the graph identifier */
			if (k > 1) /* if more than one embedding */
				System.out.print(k); /* print the number of embeddings */
			System.out.println(" (" + frag.supp[0] + ")");
		} /* print the fragment's support */

		/* --- create extensions --- */
		xfs = new Fragment[size = 16];
		cnt = 0; /* initialize the fragment array */
		for (emb = frag.firstEmb(); emb != null; emb = frag.nextEmb()) {
			if (!this.cnf.init(frag, emb))
				break; /* traverse the embeddings */
			while (this.cnf.next()) { /* while there is another extension */
				i = n = 0;
				k = cnt; /* do a binary search in fragments */
				while (i < k) { /* if the range is not empty */
					n = (i + k) >> 1; /* get index of middle element */
					r = this.cnf.compareToFrag(xfs[n]);
					if (r < 0)
						k = n;
					else if (r > 0)
						i = n + 1;
					else
						break; /* adapt the range boundaries */
				} /* or terminate the search */
				if (i < k) { /* if the fragment was found, */
					if (xfs[n].addEmb(cnf)) /* add the embedding to it and */
						this.embcnt++;
				} /* count it (for benchmarking) */
				else if (emb.getGroup() == NamedGraph.FOCUS) {
					vec = xfs; /* if the fragment was not found */
					if (cnt >= size) { /* if the fragment array is full */
						xfs = new Fragment[size += size >> 1];
						System.arraycopy(vec, 0, xfs, 0, i);
					} /* enlarge the fragment array */
					System.arraycopy(vec, i, xfs, i + 1, cnt - i);
					xfs[i] = this.cnf.makeFragment();
					cnt++;
					this.embcnt++; /* create and store a new fragment */
				} /* and count it (for benchmark) */
			} /* (after this loop cnt is the */
		} /* number of created fragments) */
		this.fragcnt += cnt; /* count all fragments (benchmark) */

		/* --- support-based pruning --- */
		/* It may be better to do this here only if the support can */
		/* be computed efficiently. Otherwise it could be done after */
		/* canonical form or repository pruning. */
		n = 0; /* init. the fragment counter */
		for (i = 0; i < cnt; i++) { /* traverse the fragments */
			xfs[i].computeSupport(this.type);
			if (xfs[i].supp[0] < this.supp) {
				this.lowsupp++;
				continue;
			}
			xfs[n++] = xfs[i]; /* collect the frequent fragments */
			if (((this.mode & CLOSED) != 0)
					&& (xfs[i].supp[0] == frag.supp[0])
					&& (xfs[i].supp[1] == frag.supp[1])
					&& (((this.mode & RINGEXT) != 0) || ((this.mode & CLOSERINGS) == 0)))
				frag.setClosed(false); /* if an extension has same support, */
		} /* mark the fragment as non-closed */
		while (cnt > n)
			/* "delete" the unused fragments */
			xfs[--cnt] = null; /* at the end of the fragment array */
		/* If fragments with open rings are to be suppressed, but ring */
		/* extensions are not used, explicit tests for closed fragments */
		/* are necessary. Otherwise certain fragments may get lost. */
		/* Thus not all qualifying fragments must be marked as closed. */

		/* --- chain pruning --- */
		chain = false; /* clear the chain extension flag */
		if ((this.mode & CHAINEXT) != 0) {
			for (i = n = 0; i < cnt; i++) { /* traverse the fragments */
				if (xfs[i].size < -1)
					continue;
				if (xfs[i].size < 0)
					chain = true;
				xfs[n++] = xfs[i]; /* remove chains with a minimum */
			} /* length greater than one node */
			while (cnt > n)
				/* collect the other fragments */
				xfs[--cnt] = null; /* at the front of the array and */
		} /* decrement the fragment counter */

		/* --- ring extension merging --- */
		if ((this.mode & MERGERINGS) != 0)
			cnt = frag.mergeExts(xfs, cnt);
		/* Note that ring extension merging must be done before any */
		/* adaptation of the fragment (for canonical form pruning), */
		/* because the function mergeExts needs to identify fragments */
		/* that were generated from the same base embedding. */

		/* --- unclosable ring pruning --- */
		if ((this.mode & PR_UNCLOSE) != 0) {
			for (i = n = 0; i < cnt; i++) { /* traverse the fragments */
				if (xfs[i].hasUnclosableRings(this.cnf)) {
					this.openrgs++;
					continue;
				}
				xfs[n++] = xfs[i]; /* identify, count, and remove */
			} /* fragments with unclosable rings */
			while (cnt > n)
				/* collect the remaining fragments */
				xfs[--cnt] = null; /* at the front of the array and */
		} /* decrement the fragment counter */

		/* --- perfect extension pruning --- */
		revert = false; /* default: do not revert ext. info. */
		if ((cnt > 1) /* if to prune w.r.t. perfect exts. */
				&& ((this.mode & (PR_PERFECT | PR_PARTIAL)) != 0)) {
			for (i = 0; i < cnt; i++)
				/* search for a perfect extension */
				if (xfs[i].isPerfect(chain))
					break;
			if (i < cnt) { /* if there is a perfect extension, */
				this.perfect++; /* count it (benchmarking) */
				if ((this.mode & PR_PERFECT) == 0) { /* if partial pruning */
					while (--cnt > i)
						/* delete all extensions to the right */
						xfs[cnt] = null; /* of the perfect extension, because */
					cnt++;
				} /* they yield non-closed fragments */
				else { /* if full perfect extension pruning, */
					revert = (i > 0) /* whether to revert extension info. */
							|| (this.cnf instanceof CnFDepth);
					xfs[0] = xfs[i]; /* note this specific extension */
					while (cnt > 1)
						/* and delete all other extensions */
						xfs[--cnt] = null; /* (process only perfect extension) */
				} /* later the extension information */
			} /* is reverted to the base fragment */
		} /* (otherwise fragments are lost) */
		/* Note that perfect extension pruning must be done before any */
		/* adaptation of the fragment (for canonical form pruning), */
		/* because the function isPerfect needs to identify fragments */
		/* that were generated from the same base embedding. */

		/* --- equivalent sibling pruning --- */
		if ((cnt > 1) /* if to prune equivalent siblings */
				&& ((this.mode & PR_EQUIV) != 0)) {
			adapt = ((this.mode & CLASSES) == 0)
					&& ((this.mode & PR_CANONIC) != 0)
					&& (this.cnf instanceof CnFDepth);
			for (i = n = 1; i < cnt; i++) {
				if (!xfs[i].equivSiblings()) { /* collect frags. without */
					xfs[n++] = xfs[i];
					continue;
				} /* equivalent siblings */
				for (k = n; --k >= 0;)
					/* traverse the fragment pairs */
					if (xfs[k].equivSiblings() && xfs[i].isEquivTo(xfs[k]))
						break; /* search for an equivalent sibling */
				if (k < 0) { /* collect the unique fragments */
					xfs[n++] = xfs[i];
					continue;
				}
				this.equiv++; /* count the equivalent fragment */
				if (!adapt || !xfs[i].isRingEdgeExt())
					continue; /* only ring edge exts. need work */
				xfs[i].adapt(this.cnf, false); /* adapt the fragments before */
				xfs[k].adapt(this.cnf, false); /* comparing their code words */
				/* Note that multiple adaptation calls for the same fragment */
				/* are harmless, because the fragment records whether it has */
				/* already been adapted and thus the work is done only once. */
				/* Note also that these adaptation calls cannot fail, since */
				/* ring extensions can not be combined with canonical form */
				/* pruning and equivalent sibling pruning at the same time. */
				/* However, only with both pruning methods "adapt" may fail. */
				this.cnf.makeWord(xfs[i].getGraph());
				r = this.cnf.compareWord(xfs[k].getGraph());
				if ((r > 0) || ((r == 0) && (xfs[i].idx < xfs[k].idx)))
					xfs[k] = xfs[i]; /* compare code words of fragments */
			} /* and keep only smallest code word */
			while (cnt > n)
				/* "delete" all other fragments */
				xfs[--cnt] = null; /* from the fragment array and */
		} /* decrement the fragment counter */

		/* --- fragment adaptation and ring order pruning --- */
		if (((this.mode & (PR_PERFECT | FULLRINGS)) != 0)
				&& ((this.mode & CLASSES) == 0)
				&& (((this.mode & PR_CANONIC) != 0) || (this.cnf instanceof CnFDepth))) {
			check = ((this.mode & PR_CANONIC) != 0);
			for (i = n = 0; i < cnt; i++) {
				if (!xfs[i].adapt(this.cnf, check)) {
					this.ringord++;
					continue;
				}
				xfs[n++] = xfs[i]; /* adapt the fragments and keep */
			} /* only successfully adapted ones */
			while (cnt > n)
				/* "delete" all other fragments */
				xfs[--cnt] = null; /* from the fragment array and */
		} /* decrement the fragment counter */
		/* If full perfect extension pruning is used, the added edge */
		/* may have to be shifted past existing perfect extensions. */
		/* This is necessary with rightmost extensions even without */
		/* canonical form pruning to ensure proper later extensions. */
		/* Similarly, if ring extensions are used, new edges must be */
		/* shifted past already added, but not yet fixed ring edges. */

		/* --- canonical form pruning --- */
		n = 0; /* init. the fragment counter */
		if ((this.mode & PR_CANONIC) != 0) {
			part = ((this.mode & FULLRINGS) != 0);
			for (i = 0; i < cnt; i++) {/* traverse the fragments */
				r = xfs[i].isCanonic(this.cnf, part);
				if (r < 0) {
					this.canonic++;
					continue;
				}
				if (r <= 0)
					xfs[i].setValid(false);
				xfs[n++] = xfs[i]; /* identify, count, and remove */
			} /* the non-canonical fragments */
		} /* and collect the rest */

		/* --- remove duplicates with repository --- */
		else { /* if ((this.mode & PR_CANONIC) == 0) */
			for (i = 0; i < cnt; i++) {/* traverse the fragments */
				if (this.duplicate(xfs[i])) {
					this.duplic++;
					continue;
				}
				xfs[n++] = xfs[i]; /* identify, count, and remove */
			} /* already processed fragments */
		} /* and collect the rest */
		/* If canonical form pruning is not used, duplicate fragments */
		/* have to be identified and removed by checking each generated */
		/* fragment against a repository of already processed fragments. */
		while (cnt > n)
			/* "delete" the unused fragments */
			xfs[--cnt] = null; /* at the end of the fragment array */

		/* --- revert extension information --- */
		if ((cnt > 0) && revert) /* revert the extension information */
			xfs[0].revert(); /* to that of the base fragment */
		/* If (full) perfect extension pruning is used, the extension */
		/* information has to be reverted to that of the base fragment, */
		/* because otherwise fragments are lost from the search tree */
		/* branches to the left of the perfect extension branch. */

		/* --- unembed sibling nodes --- */
		if ((this.mode & UNEMBED) != 0) {
			for (i = cnt; --i > 0;)
				/* unembed all fragments except */
				xfs[i].unembed(); /* the one to be processed next */
		} /* (saves some memory) */

		/* --- recursively process extensions --- */
		depth++; /* increment the recursion depth */
		for (i = 0; i < cnt; i++) { /* search fragments recursively */
			xfs[i].reembed(); /* reembed the fragment */
			if (!this.recEmbed(xfs[i], depth))
				return false; /* after return from recursion */
			xfs[i] = null; /* "delete" the processed fragment */
		} /* (allow for garbage collection) */
		this.output(frag); /* output the current fragment */
		if (Thread.currentThread().isInterrupted())
			this.stop = true; /* check for thread interruption */
		return !this.stop; /* return whether search was stopped */
	} /* recEmbed() */

	/*------------------------------------------------------------------*/
	/**
	 * Main function for the mining process.
	 * <p>
	 * The seed is embedded into all graphs to create the initial fragment or
	 * all relevant and allowed single node fragments are generated and
	 * processed.
	 * </p>
	 * 
	 * @return the number of found substructures
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int searchEmbed() throws IOException { /*
													 * --- search for
													 * substructures
													 */
		int i, k; /* loop variables */
		NamedGraph graph; /* to traverse the graphs */
		TypeMgr ndmgr; /* manager for node types and names */
		String s; /* buffer for output formatting */

		if (this.cnf == null) /* create a canonical form, */
			this.cnf = new CnFBreadth1(); /* if it does not exist */
		this.cnf.setExtMode(this.mode); /* initialize the */
		this.cnf.setMaxSize(this.max); /* extension object */
		if ((this.mode & RINGEXT) != 0) /* set the ring sizes */
			this.cnf.setRingSizes(this.rgmin, this.rgmax);
		if ((this.mode & CHAINEXT) != 0) /* set the chain types */
			this.cnf.setChainTypes(this.coder.encode(AtomTypeMgr.CARBON),
					BondTypeMgr.SINGLE);
		if ((this.mode & PR_CANONIC) == 0) {
			this.bins = new RepElem[1023];
			this.rsize = 0; /* if no canonical form pruning, */
		} /* create a substructure repository */
		this.subcnt = 0; /* init. the substructure counter */
		this.nodecnt = 0; /* and the benchmark variables */
		this.lowsupp = this.perfect = this.equiv = this.ringord = 0;
		this.canonic = this.duplic = this.nonclsd = this.openrgs = 0;
		this.chains = this.invalid = this.repcnt = this.cmpcnt = 0;
		this.isocnt = this.embcmps = 0;
		this.writer.writeHeader(); /* print header for substructures */
		if (this.wrids != null) /* and graph identifier lists */
			this.wrids.write("id:list\n");
		if ((this.mode & VERBOSE) != 0)
			System.out.println(); /* if verbose output, start new line */
		if (this.seed != null) { /* if there is a seed structure */
			this.frag.computeSupport(this.type);
			this.embcnt = this.frag.supp[2] + this.frag.supp[3];
			this.fragcnt = 1; /* search recursively from the seed */
			if (this.frag.supp[0] >= this.supp)
				this.recEmbed(this.frag, 0);
		} else { /* if there are no initial embeddings */
			ndmgr = this.graphs.getNotation().getNodeMgr();
			this.fragcnt = this.embcnt = 0;
			for (i = 0; i < this.coder.size(); i++) {
				if (this.coder.isExcluded(i) || this.coder.isMaximal(i))
					continue; /* traverse the different nodes */
				s = ndmgr.getName(this.coder.decode(i)) + "         ";
				this.log.print("\nprocessing " + s.substring(0, 8));
				if ((this.mode & VERBOSE) != 0)
					System.out.println(); /* if verbose output, start new line */
				if (this.bins != null) /* clear the repository */
					for (k = this.bins.length; --k >= 0;)
						this.bins[k] = null;/* (empty all hash bins) */
				this.frag = new Fragment(this.maxepg);
				this.fragcnt++; /* create a fragment and count it */
				for (graph = this.graphs; graph != null; graph = graph.succ)
					this.frag.addEmb(graph.embed(i)); /* collect embeddings */
				this.frag.computeSupport(this.type); /* and compute support */
				this.embcnt += this.frag.supp[2] + this.frag.supp[3];
				if ((this.frag.supp[0] >= this.supp)
						&& !this.recEmbed(this.frag, 0))
					return this.subcnt; /* check support, search recursively */
				this.coder.exclude(i); /* exclude the processed node */
				for (graph = this.graphs; graph != null; graph = graph.succ)
					graph.trim(false); /* trim the excluded node type */
			} /* from the graphs of the database */
			this.log.println(); /* (fragments with this node type */
		} /* need not be considered again) */
		this.bins = null; /* "delete" the repository */
		return this.subcnt; /* return number of substructures */
	} /* searchEmbed() */

	/*------------------------------------------------------------------*/
	/**
	 * Main recursive function of the mining process.
	 * <p>
	 * In this function the extensions of the given fragment are created and the
	 * resulting set of extended fragments is pruned with different methods.
	 * Finally the remaining fragments are processed recursively.
	 * </p>
	 * 
	 * @param frag
	 *            the current fragment (to be extended)
	 * @param depth
	 *            the current recursion depth
	 * @return whether to continue the search (search not aborted)
	 * @since 2010.01.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private boolean recPlain(Fragment frag, int depth) throws IOException { /*
																			 * ---
																			 * recursive
																			 * search
																			 * w
																			 * /
																			 * o
																			 * embeds
																			 * .
																			 */
		int i, k, n; /* loop variables */
		int f, s; /* support in focus and overall */
		Fragment x; /* next extended fragment */
		Fragment[] xfs, vec; /* list of created fragments */
		int cnt, size; /* number of fragments and array size */
		NamedGraph g; /* to determine the support */

		if (Thread.currentThread().isInterrupted())
			this.stop = true; /* check for thread interruption */
		if (this.stop) /* check for an external abort */
			return false; /* (if running as a separate thread) */
		this.nodecnt++; /* count the search tree node */
		if (depth > this.maxdep) /* update the maximal depth */
			this.maxdep = depth; /* of the recursion/search tree */

		/* --- verbose information output --- */
		if ((this.mode & VERBOSE) != 0) {
			for (i = depth; --i >= 0;)
				/* if verbose output about */
				System.out.print("   ");/* the search is requested */
			System.out.print(frag); /* print the fragment */
			System.out.println("  (" + frag.supp[0] + ")");
		} /* print the fragment's support */

		/* --- create extensions --- */
		depth++; /* increment the recursion depth */
		xfs = new Fragment[size = 16];
		cnt = 0; /* initialize the fragment array */
		this.cnf.init(frag); /* and the extension generation */
		while (true) { /* extension generation loop */
			x = this.cnf.nextFrag(); /* get the next extended fragment */
			if (x == null)
				break; /* if there is none, abort the loop */
			if (cnt >= size) { /* if the fragment array is full */
				vec = new Fragment[size += size >> 1];
				System.arraycopy(xfs, 0, vec, 0, cnt);
				xfs = vec; /* enlarge the fragment array */
			} /* and set the new array */
			xfs[cnt++] = x; /* store the extended fragment */
		} /* and count it */
		this.fragcnt += cnt; /* count all fragments (benchmark) */

		/* --- canonical form pruning --- */
		n = 0; /* init. the fragment counter */
		if ((this.mode & PR_CANONIC) != 0) {
			for (i = 0; i < cnt; i++) {/* traverse the fragments */
				if (!xfs[i].isCanonic(this.cnf)) {
					this.canonic++;
					continue;
				}
				if ((this.mode & CLASSES) != 0)
					xfs[i].map(this.cnf); /* identify, count, and remove */
				xfs[n++] = xfs[i]; /* the non-canonical fragments and */
			} /* collect the rest and adapt it */
		} /* (if equiv. classes are used) */

		/* --- remove duplicates with repository --- */
		else { /* if ((this.mode & PR_CANONIC) == 0) */
			for (i = 0; i < cnt; i++) {/* traverse the fragments */
				if (this.duplicate(xfs[i])) {
					this.duplic++;
					continue;
				}
				xfs[n++] = xfs[i]; /* identify, count, and remove */
			} /* already processed fragments */
		} /* and collect the rest */
		/* If canonical form pruning is not used, duplicate fragments */
		/* have to be identified and removed by checking each generated */
		/* fragment against a repository of already processed fragments. */
		while (cnt > n)
			/* "delete" the unused fragments */
			xfs[--cnt] = null; /* at the end of the fragment array */

		/* --- support-based pruning --- */
		n = 0; /* init. the fragment counter */
		for (i = 0; i < cnt; i++) { /* traverse the fragments */
			f = frag.supp[0]; /* get the support in the focus */
			s = frag.supp[1] + f; /* and the total support */
			for (k = 0; k < s; k++) { /* traverse the fragment's cover */
				if (f - k + xfs[i].supp[0] < this.supp)
					break; /* check against minimum support */
				g = frag.cover[k]; /* get the next containing graph */
				if (depth >= this.emblvl) /* if to switch to embeddings, */
					xfs[i].addEmb(g.embed(xfs[i].graph)); /* collect embs. */
				else if (g.contains(xfs[i].graph)) /* otherwise get */
					xfs[i].addGraph(g); /* new cover and compute support, */
			} /* then prune with threshold */
			if (xfs[i].supp[0] < this.supp) { /* check fragment support */
				this.lowsupp++;
				continue;
			} /* against minimum support */
			xfs[n++] = xfs[i]; /* collect the frequent fragments */
			if ((xfs[i].supp[0] + xfs[i].supp[1] == s)
					&& ((this.mode & CLOSED) != 0))
				frag.setClosed(false); /* check whether parent is non-closed */
		} /* and set the corresponding flag */
		while (cnt > n)
			/* "delete" the unused fragments */
			xfs[--cnt] = null; /* at the end of the fragment array */

		/* --- equivalent sibling pruning --- */
		if ((cnt > 1) /* if to prune equivalent siblings */
				&& ((this.mode & PR_EQUIV) != 0)
				&& ((this.mode & CLASSES) != 0)) {
			for (i = n = 1; i < cnt; i++) { /* traverse the fragments */
				for (k = n; --k >= 0;)
					/* traverse the fragment pairs */
					if (xfs[i].equalsCanonic(xfs[k]))
						break; /* search for an equivalent sibling */
				if (k >= 0) {
					this.equiv++;
					continue;
				}
				xfs[n++] = xfs[i]; /* identify, count, and remove */
			} /* the equivalent sibling fragments */
			while (cnt > n)
				/* collect the remaining fragments */
				xfs[--cnt] = null; /* at the front of the array and */
		} /* decrement the fragment counter */

		/* --- recursively process extensions --- */
		for (i = 0; i < cnt; i++) { /* traverse the remaining fragments */
			if ((depth < this.emblvl ? !this.recPlain(xfs[i], depth) : !this
					.recEmbed(xfs[i], depth)))
				return false; /* recursively search for fragments */
			xfs[i] = null; /* "delete" the processed fragment */
		} /* to free the now unused memory */

		/* --- finalize current fragment --- */
		this.output(frag); /* output the current fragment */
		if (Thread.currentThread().isInterrupted())
			this.stop = true; /* check for thread interruption */
		return !this.stop; /* return whether search was stopped */
	} /* recPlain() */

	/*------------------------------------------------------------------*/
	/**
	 * Main function for the mining process.
	 * <p>
	 * The seed is embedded into all graphs to create the initial fragment or
	 * all relevant and allowed single node fragments are generated and
	 * processed.
	 * </p>
	 * 
	 * @return the number of found substructures
	 * @since 2010.01.21 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int searchPlain() throws IOException { /*
													 * --- search for
													 * substructures
													 */
		int i, k; /* loop variables */
		Graph g; /* graph of the initial fragment */
		NamedGraph graph; /* to traverse the graphs */
		Edge e; /* to traverse the edges */
		ExtMgr xemgr; /* maneger for extension edges */
		TypeMgr ndmgr; /* manager for node types and names */
		String s; /* buffer for output formatting */

		if (this.cnf == null) /* create an extension object, */
			this.cnf = new CnFBreadth1(); /* if it does not exist */
		this.cnf.setExtMode(this.mode); /* initialize the */
		this.cnf.setMaxSize(this.max); /* canonical form */
		xemgr = new ExtMgr(this.coder.size());
		for (graph = this.graphs; graph != null; graph = graph.succ) {
			if (graph.group != 0)
				break; /* traverse the focus graphs */
			for (i = graph.edgecnt; --i >= 0;) {
				e = graph.edges[i]; /* traverse all edges of each graph */
				if (e.mark >= -1)
					xemgr.add(e);
			} /* add the edges to the */
		} /* extension edge manager */
		xemgr.trim(this.coder); /* trim the extension edges and */
		xemgr.sort(); /* sort them lexicographically */
		this.cnf.setExtMgr(xemgr); /* set the extension edge manager */
		if ((this.mode & PR_CANONIC) == 0) {
			this.bins = new RepElem[1023];
			this.rsize = 0; /* if no canonical form pruning, */
		} /* create a substructure repository */
		this.subcnt = 0; /* init. the substructure counter */
		this.nodecnt = 0; /* and the benchmark variables */
		this.lowsupp = this.perfect = this.equiv = this.ringord = 0;
		this.canonic = this.duplic = this.nonclsd = this.openrgs = 0;
		this.chains = this.invalid = this.repcnt = this.cmpcnt = 0;
		this.isocnt = this.embcmps = 0;
		this.writer.writeHeader(); /* print header for substructures */
		if (this.wrids != null) /* and graph identifier lists */
			this.wrids.write("id:list\n");
		if ((this.mode & VERBOSE) != 0)
			System.out.println(); /* if verbose output, start new line */
		if (this.seed != null) { /* if there is a seed structure, */
			this.fragcnt = 1; /* search recursively from the seed */
			if (this.frag.supp[0] >= this.supp)
				this.recPlain(this.frag, 0);
		} else { /* if there are no initial embeddings */
			ndmgr = this.graphs.getNotation().getNodeMgr();
			this.fragcnt = this.embcnt = 0;
			for (i = 0; i < this.coder.size(); i++) {
				if (this.coder.isExcluded(i) || this.coder.isMaximal(i))
					continue; /* traverse the different nodes */
				s = ndmgr.getName(this.coder.decode(i)) + "         ";
				this.log.print("\nprocessing " + s.substring(0, 8));
				if ((this.mode & VERBOSE) != 0)
					System.out.println(); /* if verbose output, start new line */
				if (this.bins != null) /* clear the repository */
					for (k = this.bins.length; --k >= 0;)
						this.bins[k] = null;
				g = new Graph(this.graphs.ntn, this.coder);
				this.frag = new Fragment(g, this.maxepg);
				g.addNodeRaw(i); /* create a fragment, add a node, */
				this.fragcnt++; /* and count the fragment */
				for (graph = this.graphs; graph != null; graph = graph.succ)
					if (graph.contains(g))/* compute the support */
						this.frag.addGraph(graph);
				if ((this.frag.supp[0] >= this.supp)
						&& !this.recPlain(this.frag, 0))
					return this.subcnt; /* if frequent, search recursively */
				this.coder.exclude(i); /* exclude the processed node */
				xemgr.trim(this.coder); /* trim the extension edges */
				for (graph = this.graphs; graph != null; graph = graph.succ)
					graph.trim(false); /* trim the excluded node type */
			} /* from the graphs of the database */
			this.log.println(); /* (fragments with this node type */
		} /* need not be considered again) */
		this.bins = null; /* "delete" the repository */
		return this.subcnt; /* return number of substructures */
	} /* searchPlain() */

	/*------------------------------------------------------------------*/
	/**
	 * Write all graphs of the database.
	 * 
	 * @since 2002.03.11 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void writeGraphs() throws IOException { /* --- write all graphs */
		int n = 0; /* graph counter */
		NamedGraph graph; /* to traverse the graphs */

		for (graph = this.graphs; graph != null; graph = graph.succ) {
			if ((this.mode & NORMFORM) != 0)
				graph.makeCanonic(new CnFBreadth1());
			if ((this.mode & LOGIC) != 0) { /* if description in logic */
				this.writer.write(graph.toLogic());
				this.writer.write('\n');
			} else { /* if normal description */
				this.writer.setName(graph.name);
				this.writer.setValue(graph.value);
				this.writer.setGraph(graph);
				this.writer.writeGraph();
			} /* set values and write the graph */
			if ((++n & 0xff) == 0)
				this.print(n);
		} /* print the number of graphs */
	} /* writeGraphs() */

	/*------------------------------------------------------------------*/
	/**
	 * Evaluate the command line parameter stating the ring size range.
	 * 
	 * @param s
	 *            the command line parameter stating the ring size range
	 * @return an array with the minimum and maximum ring size
	 * @since 2003.08.03 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private static int[] getRingSizes(String s) { /*
												 * --- get min. and max. ring
												 * size
												 */
		int i, n; /* loop variable */
		int[] sizes; /* minimum and maximum ring size */

		for (n = s.length(), i = 0; i < n; i++)
			if (s.charAt(i) == ':') /* try to find the separator for */
				break; /* the minimum and maximum size */
		sizes = new int[2]; /* create ring sizes array */
		if (i >= n) /* if there is only one number */
			sizes[0] = sizes[1] = Integer.parseInt(s);
		else { /* if there are two numbers */
			sizes[0] = Integer.parseInt(s.substring(0, i));
			sizes[1] = Integer.parseInt(s.substring(i + 1));
		} /* parse minimum and maximum size */
		if ((sizes[1] > 256) || (sizes[0] > sizes[1]))
			sizes[0] = sizes[1] = -1; /* check the given ring sizes */
		return sizes; /* return the ring size range */
	} /* getRingSizes() */

	/*------------------------------------------------------------------*/
	/**
	 * Initialize the miner from command line arguments.
	 * 
	 * @param args
	 *            the command line arguments
	 * @since 2006.03.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void init(String args[]) throws IOException { /*
														 * --- init. a
														 * substructure miner
														 */
		int i, k = 0; /* indices, loop variables */
		String s; /* to traverse the arguments */
		String datfn = null; /* name of graph input file */
		String subfn = "moss.sub";/* name of substructure file */
		String idsfn = null; /* name of identifier file */
		double split = 0.5; /* threshold for split */
		boolean invert = false; /* whether to invert the split */
		double psupp = 10.0; /* minimum support of embedding */
		double pcomp = 2.0; /* maximum support in complement */
		int smode = DEFAULT; /* search mode and support type */
		int stype = Fragment.GRAPHS | Fragment.GREEDY;
		int smin = 1; /* min. and max. size of substructure */
		int smax = Integer.MAX_VALUE;
		int[] sizes = null; /* minimum and maximum size of rings */
		int level = 0; /* level at which to switch to embeds */
		int maxepg = 0; /* max. number of embeddings per mol. */
		String format = "smiles"; /* format for seed description */
		String input = "smiles"; /* input format for graphs */
		String output = "smiles"; /* output format for substructures */
		String desc = ""; /* description of (seed) graph */
		String excl = "H"; /* excluded node types */
		String exsd = ""; /* excluded seed types */
		String cfname = "breadth"; /* name of the canonical form */
		int matom, mbond; /* type masks for atoms and bonds */
		int mrgat, mrgbd; /* type masks for atoms and bonds */
		CanonicalForm cf; /* buffer for canonical form */
		GraphReader rdr; /* buffer for graph reader */
		GraphWriter wrt; /* buffer for graph writer */

		/* --- print startup/usage message --- */
		if (args.length > 0) { /* if no arguments are given */
			this.log.print(this.getClass().getName());
			this.log.println(" - " + DESCRIPTION);
			this.log.println(VERSION + "    " + COPYRIGHT);
		} else { /* if no arguments are given */
			System.out.print("usage: java " + this.getClass().getName());
			System.out.println(" [options] <in> [<out>] [<ids>]");
			System.out.println(DESCRIPTION);
			System.out.println(VERSION + "    " + COPYRIGHT);
			System.out.print("in      name of graph input file         ");
			System.out.println(" (mandatory)");
			System.out.print("out     name of substructure output file ");
			System.out.println(" (default: \"" + subfn + "\")");
			System.out.print("ids     name of graph identifier file    ");
			System.out.println(" (default: none)");
			System.out.print("-i#     input  format for graphs         ");
			System.out.println(" (default: " + input + ")");
			System.out.print("-o#     output format for substructures  ");
			System.out.println(" (default: " + output + ")");
			System.out.print("-f#     seed format (line notation)      ");
			System.out.println(" (default: " + format + ")");
			System.out.print("-j#     seed structure to start");
			System.out.println(" the search from (default: none)");
			System.out.print("-x#     node types to exclude (as a");
			System.out.println(" graph in seed format, default: H)");
			System.out.print("-y#     seed types to exclude (as a");
			System.out.println(" graph in seed format, default: none)");
			System.out.print("-t#     threshold value for split        ");
			System.out.println(" (default: " + split + ")");
			System.out.print("-z      invert split");
			System.out.println(" (> versus <= instead of <= versus >)");
			System.out.print("-m#     minimum size of a substructure   ");
			System.out.println(" (default: " + smin + ")");
			System.out.print("-n#     maximum size of a substructure   ");
			System.out.println(" (default: no limit)");
			System.out.print("-k#     support type (1:MIS, 2:HO, 3:MNI)");
			System.out.println(" (default: 0:graphs)");
			System.out.print("-s#     minimum support in focus         ");
			System.out.println(" (default: " + psupp + "%)");
			System.out.print("-S#     maximum support in complement    ");
			System.out.println(" (default: " + pcomp + "%)");
			System.out.print("        (positive: relative support, ");
			System.out.println("negative: absolute support)");
			System.out.print("-C      do not restrict the output");
			System.out.println(" to closed substructures");
			System.out.print("-G      do not use greedy algorithm for");
			System.out.println(" MIS computation (slower)");
			System.out.print("+/-a    match/ignore aromaticity of atoms");
			System.out.println(" (default: ignore/-)");
			System.out.print("+/-c    match/ignore charge of atoms     ");
			System.out.println(" (default: ignore/-)");
			System.out.print("+/-d    match/ignore atom type           ");
			System.out.println(" (default: match/+)");
			System.out.print("+/-D    match/ignore atom type in rings  ");
			System.out.println(" (default: match/+)");
			System.out.print("+/-:    upgrade/downgrade aromatic bonds ");
			System.out.println(" (default: extra type)");
			System.out.print("+/-b    match/ignore bond type           ");
			System.out.println(" (default: match/+)");
			System.out.print("+/-B    match/ignore bond type in rings  ");
			System.out.println(" (default: match/+)");
			System.out.print("-K      do not convert Kekule");
			System.out.println(" representations to aromatic rings");
			System.out.print("-r#:#   mark rings of size # to # edges  ");
			System.out.println(" (default: no marking)");
			System.out.print("-R      extend with rings of marked sizes");
			System.out.println(" (default: single edges)");
			System.out.print("-E      edge-by-edge support-filtered");
			System.out.println(" ring extensions (includes -O)");
			System.out.print("-O      do not record substructures with");
			System.out.println(" open rings of marked sizes");
			System.out.print("-H      find and match variable length");
			System.out.println(" chains of carbon atoms");
			System.out.print("-g      use rightmost path extensions    ");
			System.out.println(" (default: maximum source)");
			System.out.print("-A      generate all possible extensions ");
			System.out.println(" (default: restricted)");
			System.out.print("-Q      use node equivalence classes     ");
			System.out.println(" (default: only types)");
			System.out.print("+/-P    partial perfect extension pruning");
			System.out.println(" (default: no /-)");
			System.out.print("+/-p    full    perfect extension pruning");
			System.out.println(" (default: yes/+)");
			System.out.print("+/-e    equivalent sibling pruning       ");
			System.out.println(" (default: no /-)");
			System.out.print("+/-q    canonical form pruning           ");
			System.out.println(" (default: yes/+)");
			System.out.print("+/-h    filter extensions with orbits    ");
			System.out.println(" (default: yes/+)");
			System.out.print("-u#     use embeddings only from level # ");
			System.out.println(" (default: 0)");
			System.out.print("        (< 0: do not use embeddings at all,");
			System.out.println(" 0: use always)");
			System.out.print("-M#     maximal number of embeddings");
			System.out.println(" per graph       (to save memory)");
			System.out.print("-U      unembed siblings of current");
			System.out.println(" search tree node (to save memory)");
			System.out.print("-N      normalize substructure output form");
			System.out.println(" (for result comparisons)");
			System.out.print("-v      verbose output during search");
			System.out.println(" (print the search tree)");
			System.out.print("-T      do not print search statistic");
			System.out.println(" (number of embeddings etc.)");
			System.out.print("-l      do not search,");
			System.out.println(" only convert input to the output format");
			System.out.print("-L      do not search,");
			System.out.println(" only convert input to a logic format");
			throw new IOException("no arguments given");
		} /* print a usage message */

		/* remaining option characters: w F I J V W X Y Z */

		/* --- evaluate arguments --- */
		matom = mrgat = AtomTypeMgr.ELEMMASK; /* set default masks */
		mbond = mrgbd = BondTypeMgr.BONDMASK;
		for (i = 0; i < args.length; i++) {
			s = args[i]; /* traverse the arguments */
			if ((s.length() > 0) /* if the argument is an option */
					&& (s.charAt(0) == '-')) {
				if (s.length() < 2) /* check for an option letter */
					throw new IOException("error: missing option");
				switch (s.charAt(1)) { /* evaluate option */
				case 'i':
					input = s.substring(2);
					break;
				case 'o':
					output = s.substring(2);
					break;
				case 'f':
					format = s.substring(2);
					break;
				case 'j':
					desc = s.substring(2);
					break;
				case 'x':
					excl = s.substring(2);
					break;
				case 'y':
					exsd = s.substring(2);
					break;
				case 't':
					split = Double.parseDouble(s.substring(2));
					break;
				case 'z':
					invert = true;
					break;
				case 'm':
					smin = Integer.parseInt(s.substring(2));
					break;
				case 'n':
					smax = Integer.parseInt(s.substring(2));
					break;
				case 's':
					psupp = Double.parseDouble(s.substring(2));
					break;
				case 'S':
					pcomp = Double.parseDouble(s.substring(2));
					break;
				case 'k':
					stype = Integer.parseInt(s.substring(2))
							| (stype & Fragment.GREEDY);
					break;
				case 'C':
					smode &= ~CLOSED;
					break;
				case 'G':
					stype &= ~Fragment.GREEDY;
					break;
				case 'a':
					matom &= ~AtomTypeMgr.AROMATIC;
					break;
				case 'c':
					matom &= ~AtomTypeMgr.CHARGEMASK;
					break;
				case 'd':
					matom &= ~AtomTypeMgr.ELEMMASK;
					mrgat &= ~AtomTypeMgr.ELEMMASK;
					break;
				case 'D':
					mrgat &= ~AtomTypeMgr.ELEMMASK;
					break;
				case ':':
					mbond &= BondTypeMgr.DOWNGRADE;
					mrgbd &= BondTypeMgr.DOWNGRADE;
					break;
				case 'b':
					mbond &= BondTypeMgr.SAMETYPE;
					mrgbd &= BondTypeMgr.SAMETYPE;
					break;
				case 'B':
					mrgbd &= BondTypeMgr.SAMETYPE;
					break;
				case 'K':
					smode &= ~AROMATIZE;
					break;
				case 'r':
					sizes = getRingSizes(s.substring(2));
					break;
				case 'R':
					smode |= RINGEXT;
					break;
				case 'E':
					smode |= RINGEXT | MERGERINGS | CLOSERINGS | PR_UNCLOSE;
					break;
				case 'O':
					smode |= CLOSERINGS | PR_UNCLOSE;
					break;
				case 'H':
					smode |= CHAINEXT;
					break;
				case 'A':
					smode |= ALLEXTS;
					break;
				case 'Q':
					smode |= CLASSES;
					break;
				case 'g':
					cfname = s.substring(2);
					break;
				case 'P':
					smode &= ~PR_PARTIAL;
					break;
				case 'p':
					smode &= ~PR_PERFECT;
					break;
				case 'e':
					smode &= ~PR_EQUIV;
					break;
				case 'q':
					smode &= ~PR_CANONIC;
					break;
				case 'h':
					smode &= ~ORBITS;
					break;
				case 'u':
					level = Integer.parseInt(s.substring(2));
					break;
				case 'M':
					maxepg = Integer.parseInt(s.substring(2));
					break;
				case 'U':
					smode |= UNEMBED;
					break;
				case 'N':
					smode |= NORMFORM;
					break;
				case 'v':
					smode |= VERBOSE;
					break;
				case 'T':
					smode |= NOSTATS;
					break;
				case 'l':
					smode |= TRANSFORM;
					break;
				case 'L':
					smode |= LOGIC;
					break;
				default:
					throw new IOException("error: unknown option -"
							+ s.charAt(1));
				}
			} /* set option variables */
			else if ((s.length() > 0) /* if the argument is an option */
					&& (s.charAt(0) == '+')) {
				if (s.length() < 2) /* check for an option letter */
					throw new IOException("error: missing option");
				switch (s.charAt(1)) { /* evaluate option */
				case 'a':
					matom |= AtomTypeMgr.AROMATIC;
					break;
				case 'c':
					matom |= AtomTypeMgr.CHARGEMASK;
					break;
				case 'd':
					matom |= AtomTypeMgr.ELEMMASK;
					mrgat |= AtomTypeMgr.ELEMMASK;
					break;
				case 'D':
					mrgat |= AtomTypeMgr.ELEMMASK;
					break;
				case ':':
					mbond &= BondTypeMgr.UPGRADE;
					mrgbd &= BondTypeMgr.UPGRADE;
					break;
				case 'b':
					mbond |= BondTypeMgr.BONDMASK;
					mrgbd |= BondTypeMgr.BONDMASK;
					break;
				case 'B':
					mrgbd |= BondTypeMgr.BONDMASK;
					break;
				case 'P':
					smode |= PR_PARTIAL;
					smode &= ~PR_PERFECT;
					break;
				case 'p':
					smode |= PR_PERFECT;
					smode &= ~PR_PARTIAL;
					break;
				case 'e':
					smode |= PR_EQUIV;
					break;
				case 'q':
					smode |= PR_CANONIC;
					break;
				case 'h':
					smode |= ORBITS;
					break;
				default:
					throw new IOException("error: unknown option +"
							+ s.charAt(1));
				}
			} /* set option variables */
			else { /* if the argument is no option */
				switch (k++) { /* evaluate non-option */
				case 0:
					datfn = s;
					break;
				case 1:
					subfn = s;
					break;
				case 2:
					idsfn = s;
					break;
				default:
					throw new IOException("error: too many arguments");
				} /* there should be two fixed args: */
			} /* a seed description and a */
		} /* name of an input file */

		/* --- initialize input/output --- */
		this.setGrouping(split, invert);
		this.setMode(smode); /* set the search mode */
		rdr = GraphReader.createReader(new FileReader(datfn),
				GraphReader.GRAPHS, input);
		if (rdr == null) /* create the graph reader */
			throw new IOException("error: invalid format " + input);
		this.setInput(rdr); /* set the graph reader */
		if (rdr instanceof NEListReader) {
			output = "list";
			excl = null;
		}
		wrt = GraphWriter.createWriter(new FileWriter(subfn), GraphWriter.SUBS,
				output);
		if (wrt == null) /* create the graph writer */
			throw new IOException("error: invalid format " + output);
		this.setOutput(wrt, /* set the graph writer */
				((idsfn == null) || idsfn.equals("")) ? null : new FileWriter(
						idsfn));
		if ((smode & (TRANSFORM | LOGIC)) != 0)
			return; /* check for a mere transformation */

		/* --- initialize other search variables --- */
		if (!Notation.createNotation(format).isLine())
			throw new IOException("error: invalid format " + format);
		this.setType(stype); /* set the support type */
		if (psupp >= 0)
			psupp *= 0.01F; /* change support values from */
		if (pcomp >= 0)
			pcomp *= 0.01F; /* percentages to fractions */
		this.setLimits(psupp, pcomp); /* set the support limits */
		this.setMasks(matom, mbond, mrgat, mrgbd);
		this.setSizes(smin, smax); /* set masks, sizes, etc. */
		if ((sizes != null) && (sizes[0] < 0))
			throw new IOException("error: invalid ring size range");
		if (sizes == null)
			sizes = new int[2];
		this.setRingSizes(sizes[0], sizes[1]);
		this.setEmbed(level, maxepg);/* set embedding parameters */
		this.setSeed(desc, format); /* set seed and excluded types */
		this.setExcluded(excl, exsd, format);
		if (cfname.equals("")) /* map an empty string to ensure */
			cfname = "depth"; /* backward compatibility */
		cf = CanonicalForm.createCnF(cfname);
		if (cf == null) /* create a canonical form */
			throw new IOException("error: invalid canonical form " + cfname);
		this.setCnF(cf); /* set the canonical form */
	} /* init() */

	/*------------------------------------------------------------------*/
	/**
	 * Preprocess the graphs, embed the seed, and start the search.
	 * 
	 * @since 2006.03.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected void mine() throws IOException { /* --- run substructure search */
		int k, n = 0; /* number of graphs/substructures */
		float value; /* value associated with the graph */
		int grp; /* group of the graph */
		NamedGraph graph; /* created graph */
		String m = "graph"; /* buffers for log messages */
		long t; /* for time measurements */

		/* --- load graph data set --- */
		this.subcnt = -1; /* invalidate substructure counter */
		this.configNtns(); /* configure the graph notations */
		if (this.reader != null) { /* if to read a graph data set */
			t = System.currentTimeMillis();
			if (this.reader.getNotation() instanceof MoleculeNtn)
				m = "molecule"; /* get the graph type name */
			this.log.print("reading " + m + "s ... ");
			this.cnts[0] = this.cnts[1] = 0;
			try { /* read the graph descriptions */
				while (this.reader.readGraph()) {
					value = this.reader.getValue();
					grp = (value > this.thresh) ? 1 - this.group : this.group;
					graph = new NamedGraph(this.reader.getGraph(),
							this.reader.getName(), value, grp);
					this.addGraph(graph); /* parse the next graph and add it */
					if ((++n & 0xff) == 0)
						this.print(n);
				}
			} /* print the number of graphs */
			catch (IOException e) { /* report graph number with error */
				throw new IOException((n + 1) + ": " + e.getMessage());
			}
			t = System.currentTimeMillis() - t;
			this.log.println("[" + n + " (" + this.cnts[0] + "+" + this.cnts[1]
					+ ") " + m + "(s)] done [" + (t / 1000.0) + "s].");
		} /* report the number of graphs read */
		if ((this.graphs != null) /* get the graph type name */
				&& (this.graphs.getNotation() instanceof MoleculeNtn))
			m = "molecule"; /* (graph or molecule) */
		if (((this.mode & (TRANSFORM | LOGIC)) == 0) && (this.cnts[0] <= 0)) /*
																			 * check
																			 * for
																			 * graphs
																			 * in
																			 * focus
																			 */
			throw new IOException("error: no " + m + " in the focus");

		/* --- convert Kekule representations --- */
		if ((this.graphs != null) /* if the graphs are molecules */
				&& (this.graphs.getNotation() instanceof MoleculeNtn)
				&& ((this.mode & AROMATIZE) != 0)) {
			this.log.print("converting Kekule representations ... ");
			t = System.currentTimeMillis();
			k = this.aromatize(); /* turn into true aromatic rings */
			t = System.currentTimeMillis() - t;
			this.log.println("[" + k + " " + m + "(s)] done [" + (t / 1000.0)
					+ "s].");
		} /* report the number of mod. graphs */

		/* --- convert to another description language --- */
		if ((this.mode & (TRANSFORM | LOGIC)) != 0) {
			this.log.print("writing " + m + "s ... ");
			t = System.currentTimeMillis();
			this.writeGraphs(); /* write the graph list */
			t = System.currentTimeMillis() - t;
			this.log.println("[" + (this.cnts[0] + this.cnts[1]) + " " + m
					+ "(s)] done [" + (t / 1000.0) + "s].");
			return; /* print a log message */
		} /* and abort the program */

		/* --- make search mode consistent --- */
		if ((this.mode & (RINGEXT | CHAINEXT)) != 0) {
			this.mode &= ~CLASSES; /* for ring or chain extensions */
			this.emblvl = 0; /* node equiv. classes cannot be used */
		} /* and embeddings are mandatory */
		/* Without embeddings ring and chain extensions cannot be */
		/* generated, because they cannot be followed in a graph. */
		if ((this.seed != null) /* if the seed contains several nodes */
				&& (this.seed.nodecnt > 1)) {
			if (this.cnf instanceof CnFDepth)
				this.cnf = new CnFBreadth1();
			this.mode &= ~PR_CANONIC; /* adapt the canonical form and */
		} /* remove canonical form pruning */
		/* With the current state of the implementation, seeds with more */
		/* than one node are possible only with maximum source extensions */
		/* and they cannot be combined with canonical form pruning. Hence */
		/* the corresponding flags must be removed from the search mode. */
		/* A simple example showing the problem with rightmost extensions */
		/* is to search for the fragment "N-C-C-O" with the seed "C-C". */
		/* Note that the problem doesn't involve the canonical form test, */
		/* but only the restricted extensions that are derived from it. */
		if ((this.graphs == null) /* get the graph type name */
				|| !(this.graphs.getNotation().getNodeMgr() instanceof AtomTypeMgr)
				|| !(this.graphs.getNotation().getEdgeMgr() instanceof BondTypeMgr))
			this.mode &= ~CHAINEXT; /* chain exts. only for molecules */
		k = this.type & Fragment.SUPPMASK;
		if (k != Fragment.GRAPHS) /* if support is not number of graphs */
			this.mode &= ~(CLOSED | CHAINEXT);
		/* If the support of a fragment is not computed as the number of */
		/* graphs containing it, restricting the output to only closed */
		/* fragments is not implemented yet. In addition, carbon chains */
		/* are not supported in this case. */
		if ((this.mode & CLOSED) == 0)
			this.mode &= ~(PR_PERFECT | PR_PARTIAL);
		/* Perfect extension pruning (in whatever form) presupposes that */
		/* the output is restricted to closed fragments. */
		if (((this.mode & CHAINEXT) != 0) && !(this.cnf instanceof CnFBreadth1))
			this.cnf = new CnFBreadth1();
		if ((this.mode & RINGEXT) == 0)
			this.mode &= ~MERGERINGS; /* make ring ext. flags consistent */
		else if ((this.mode & MERGERINGS) == 0)
			this.mode |= FULLRINGS; /* set full ring extension flag */
		if ((this.mode & MERGERINGS) != 0) {
			this.mode &= ~UNEMBED;
			this.maxepg = 0;
		}
		/* Merging ring extensions cannot be combined with any memory */
		/* saving option, due to ordering problems when reembedding. */
		/* However, the fact that the set of embeddings is reduced would */
		/* also lead to a considerable loss of information if embeddings */
		/* were discarded and recreated by reembedding. */
		if (((this.mode & FULLRINGS) != 0) && ((this.mode & PR_CANONIC) != 0)) {
			this.mode &= ~(PR_EQUIV | PR_UNCLOSE);
			this.mode |= EQVARS; /* skip certain pruning methods and */
		} /* create equivalent ring variants */
		/* Since equivalent variants of the same ring extension have */
		/* to be generated to combine ring extensions with canonical */
		/* form pruning, equivalent sibling pruning cannot be applied. */
		/* Unclosable ring pruning does no harm, but is useless, since */
		/* with full ring extensions all rings are always closed. */
		if ((this.mode & PR_CANONIC) == 0)
			this.mode &= ~CLASSES; /* equi. classes need canonical forms */
		if ((this.mode & CLASSES) != 0) { /* node equivalence classes */
			this.mode &= ~(PR_PERFECT | PR_PARTIAL);
			this.mode |= PR_EQUIV; /* do not permit perfect ext. pruning */
		} /* but require equiv. sibling pruning */

		/* --- mark bridges --- */
		if ((this.mode & (PR_PERFECT | PR_PARTIAL | CHAINEXT)) != 0) {
			this.log.print("marking bridges ... ");
			t = System.currentTimeMillis();
			k = this.markBridges();
			t = System.currentTimeMillis() - t;
			this.log.println("[" + k + " " + m + "(s)] done [" + (t / 1000.0)
					+ "s].");
		} /* mark bridges in all graphs */

		/* --- mark rings --- */
		if (this.rgmax > 1) { /* if to mark the edges of rings */
			this.log.print("marking rings (sizes " + this.rgmin + " to "
					+ this.rgmax + ") ... ");
			t = System.currentTimeMillis();
			k = this.markRings(this.rgmin, this.rgmax);
			t = System.currentTimeMillis() - t;
			this.log.println("[" + k + " " + m + "(s)] done [" + (t / 1000.0)
					+ "s].");
		} /* mark rings in all graphs */

		/* --- mark pseudo-rings --- */
		if ((this.rgmin > 1) /* if to mark the edges of rings */
				&& ((this.mode & FULLRINGS) != 0)
				&& ((this.mode & PR_CANONIC) != 0)) {
			this.log.print("marking pseudo-rings (sizes up to "
					+ (this.rgmin - 1) + ") ... ");
			t = System.currentTimeMillis();
			k = this.markPseudo(this.rgmin - 1);
			t = System.currentTimeMillis() - t;
			this.log.println("[" + k + " " + m + "(s)] done [" + (t / 1000.0)
					+ "s].");
			if (k > 0)
				this.mode |= CLOSERINGS;
		} /* set ring filter flag if necessary */

		/* --- mask node and edge types --- */
		if ((this.graphs != null) /* if the graphs are molecules */
				&& (this.graphs.getNotation() instanceof MoleculeNtn)) {
			this.log.print("masking atom and bond types ... ");
			t = System.currentTimeMillis();
			k = this.maskTypes(); /* mask types in all graphs */
			t = System.currentTimeMillis() - t;
			this.log.println("[" + k + " " + m + "(s)] done [" + (t / 1000.0)
					+ "s].");
		} /* (do this only for molecules) */

		/* --- prepare the graphs --- */
		this.log.print("preparing/recoding " + m + "s ... ");
		k = this.cnts[0] + this.cnts[1];
		t = System.currentTimeMillis();
		this.setup(); /* set up the substructure search */
		t = System.currentTimeMillis() - t;
		this.log.println("[" + k + " " + m + "(s)] done [" + (t / 1000.0)
				+ "s].");

		/* --- embed the seed --- */
		if (this.seed != null) { /* if a seed is given */
			this.log.print("embedding the seed ... ");
			t = System.currentTimeMillis();
			k = this.embed(); /* embed the seed into the graphs */
			t = System.currentTimeMillis() - t;
			this.log.println("[" + k + " (" + this.frag.supp[0] + "+"
					+ this.frag.supp[1] + ") " + m + "(s)] done ["
					+ (t / 1000.0) + "s].");
			if (k <= 0)
				return; /* print a log message and */
		} /* check the number of graphs */

		/* --- search for substructures --- */
		this.log.print("searching for substructures ... ");
		t = System.currentTimeMillis();
		k = (this.emblvl > 0) ? this.searchPlain() : this.searchEmbed();
		t = System.currentTimeMillis() - t;
		this.log.println("[" + k + " substructure(s)] done [" + (t / 1000.0)
				+ "s].");
	} /* mine() */

	/*------------------------------------------------------------------*/
	/**
	 * Clean up after the search finished or was aborted.
	 * 
	 * @since 2006.03.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	protected void term() throws IOException { /* --- clean up after search */
		if (this.writer != null) {
			this.writer.close();
			this.writer = null;
		}
		if (this.wrids != null) {
			this.wrids.close();
			this.wrids = null;
		}
	} /* term() */

	/*------------------------------------------------------------------*/
	/**
	 * Run the miner and clean up after the search finished.
	 * 
	 * @since 2006.03.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void run() { /* --- run the miner */
		this.error = null; /* clear the error status */
		this.stop = false; /* and the stop flag */
		try {
			this.mine();
		} /* run the substructure search */
		catch (IOException e) {
			this.error = e;
		}
		try {
			this.term();
		} /* clean up after the search */
		catch (IOException e) {
			if (this.error == null)
				this.error = e;
		}
		if (this.error != null) { /* report an error */
			this.log.println("\n" + this.error.getMessage());
		}
	} /* run() */

	/*------------------------------------------------------------------*/
	/**
	 * Abort the miner (if running as a thread).
	 * 
	 * @since 2006.03.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void abort() {
		this.stop = true;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get the substructures that have been found up to now.
	 * <p>
	 * This function enables progress reporting by another thread. It is used in
	 * the graphical user interface (class <code>MoSS</code>).
	 * </p>
	 * <p>
	 * If the return value is negative, it indicates the number of graphs that
	 * have been loaded, otherwise the number of substructures that have been
	 * found.
	 * </p>
	 * 
	 * @return the number of loaded graphs (if non-negative) or the number of
	 *         found substructures (if negative)
	 * @since 2006.03.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public int getCurrent() {
		return (this.subcnt >= 0) ? this.subcnt : -this.cnts[0] - this.cnts[1];
	}

	/*------------------------------------------------------------------*/
	/**
	 * Get the error status of the search process.
	 * <p>
	 * With this function it can be checked, after the search with the
	 * <code>run()</code> method has terminated, whether an error occurred in
	 * the search. Note that an external abort with the function
	 * <code>abort()</code> does <i>not</i> trigger an exception to be thrown.
	 * </p>
	 * 
	 * @return the exception that occurred in the search or <code>null</code> if
	 *         the search was successful
	 * @since 2007.03.05 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Exception getError() {
		return this.error;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Print statistics about the search.
	 * 
	 * @since 2006.03.01 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void stats() { /* --- show search statistics */
		if ((this.mode & (TRANSFORM | LOGIC | NOSTATS)) != 0)
			return;
		this.log.println("search statistics:");
		this.log.println("maximum search tree height   : " + this.maxdep);
		this.log.println("number of search tree nodes  : " + this.nodecnt);
		this.log.println("number of created fragments  : " + this.fragcnt);
		this.log.println("number of created embeddings : " + this.embcnt);
		this.log.println("insufficient support pruning : " + this.lowsupp);
		this.log.println("perfect extension pruning    : " + this.perfect);
		this.log.println("equivalent sibling pruning   : " + this.equiv);
		this.log.println("canonical form pruning       : " + this.canonic);
		this.log.println("ring order pruning           : " + this.ringord);
		this.log.println("duplicate fragment pruning   : " + this.duplic);
		this.log.println("non-closed fragments         : " + this.nonclsd);
		this.log.println("fragments with open rings    : " + this.openrgs);
		this.log.println("fragments with invalid chains: " + this.chains);
		this.log.println("auxiliary invalid fragments  : " + this.invalid);
		this.log.println("accesses to repository       : " + this.repcnt);
		this.log.println("comparisons with fragments   : " + this.cmpcnt);
		this.log.println("actual isomorphism tests     : " + this.isocnt);
		this.log.println("comparisons with embeddings  : " + this.embcmps);
	} /* stats() */

	/*------------------------------------------------------------------*/
	/**
	 * Command line invocation of the molecular substructure miner.
	 * 
	 * @param args
	 *            the command line arguments
	 * @since 2002.03.15 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public static void main(String args[]) { /* --- main function */
		Miner miner = new Miner(); /* substructure miner */
		try { /* run substructure search */
			miner.init(args); /* initialize the miner, */
			miner.run(); /* find substructures, and */
			miner.stats();
		} /* show search statistics */
		catch (IOException e) { /* report i/o error message */
			System.err.println("\n" + e.getMessage());
		}
	} /* main() */

} /* class Miner */
