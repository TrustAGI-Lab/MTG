/*----------------------------------------------------------------------
  File    : NEList.java
  Contents: class for simple node/edge list notation
  Author  : Christian Borgelt
  History : 2007.06.22 file created
            2007.08.16 bug in function parse() fixed (node index)
            2007.10.19 bug in function write() fixed (type decoding)
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/*--------------------------------------------------------------------*/
/**
 * Class for a simple node/edge list notation.
 * 
 * @author Christian Borgelt
 * @since 2006.08.12
 */
/*--------------------------------------------------------------------*/
public class NEList extends FreeNtn {

	/*------------------------------------------------------------------*/
	/**
	 * Create a list notation with empty type managers.
	 * <p>
	 * By default this notation uses <code>FreeTypeMgr</code> objects for the
	 * type managers, which can be extended dynamically.
	 * </p>
	 * 
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public NEList() {
		this(new FreeTypeMgr(), new FreeTypeMgr());
	}

	/*------------------------------------------------------------------*/
	/**
	 * Create a list notation with given type managers.
	 * 
	 * @param nodemgr
	 *            the manager for the node types
	 * @param edgemgr
	 *            the manager for the edge types
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public NEList(TypeMgr nodemgr, TypeMgr edgemgr) { /*
													 * --- create a list
													 * notation
													 */
		this.nodemgr = nodemgr; /* store the given */
		this.edgemgr = edgemgr; /* type managers */
	} /* NEList() */

	/*------------------------------------------------------------------*/
	/**
	 * Whether this is a line notation (single line description).
	 * 
	 * @return <code>false</code>, since this is a multi-line notation
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean isLine() {
		return false;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Read an integer number.
	 * 
	 * @return the integer number read
	 * @throws IOException
	 *             if no integer number could be read
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private int readInt() throws IOException { /* --- read an integer number */
		int c; /* loop variable, character */
		int n = 0; /* integer value read */

		c = this.read(); /* check for a separator */
		if ((c != ' ') && (c != '\t'))
			throw new IOException("separator expected instead of " + (char) c
					+ "' (" + c + ")");
		do {
			c = this.read();
		} /* skip leading blanks */
		while ((c == ' ') || (c == '\t'));
		if (c == '-') { /* check for a minus sign */
			n = -1;
			c = this.read();
		}
		if ((c < '0') || (c > '9')) /* check for at least one digit */
			throw new IOException("digit expected instead of '" + (char) c
					+ "' (" + c + ")");
		n = (n < 0) ? '0' - c : c - '0';/* process first digit and */
		c = this.read(); /* read next character */
		while ((c >= '0') && (c <= '9')) {
			n = n * 10 + (c - '0'); /* process next digit and */
			c = this.read(); /* read the next character */
		}
		this.unread(c); /* push back the last character */
		return n; /* return the parsed number */
	} /* readInt() */

	/*------------------------------------------------------------------*/
	/**
	 * Read a node or edge type name.
	 * 
	 * @return the node or edge type name read
	 * @throws IOException
	 *             if no type could be read
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private String readType() throws IOException { /*
													 * --- read a node or edge
													 * label
													 */
		int i; /* loop variable */
		int c; /* to read the characters */
		StringBuffer s; /* buffer for the type name */

		c = this.read(); /* check for end of line or separator */
		if ((c < 0) || (c == '\n'))
			return "";
		if ((c != ' ') && (c != '\t'))
			throw new IOException("separator expected instead of " + (char) c
					+ "' (" + c + ")");
		do {
			c = this.read();
		} /* skip leading blanks */
		while ((c == ' ') || (c == '\t'));
		if ((c < 0) || (c == '\n')) /* check for end of line */
			return ""; /* (empty type name) */
		s = new StringBuffer(); /* create a buffer for the type name */
		while ((c >= 0) && (c != '\n')) {
			s.append((char) c); /* append the character read */
			c = this.read(); /* and read the next character */
		} /* (read the type name) */
		for (i = s.length(); --i >= 0;) {
			c = s.charAt(i); /* traverse last characters */
			if ((c != ' ') && (c != '\t') && (c != '\r')) {
				s.setLength(i + 1);
				break;
			}
		} /* remove trailing blanks */
		return s.toString(); /* return the type name */
	} /* readType() */

	/*------------------------------------------------------------------*/
	/**
	 * Parse a description of an attributed graph.
	 * 
	 * @param reader
	 *            the reader from which to read the description
	 * @return the parsed graph
	 * @throws IOException
	 *             if a parse error or an i/o error occurs
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public Graph parse(Reader reader) throws IOException { /*
															 * --- parse a graph
															 * description
															 */
		int c; /* character or type code */
		int s, d, e = 0; /* node indices */
		String t; /* type of a node or an edge */
		Graph graph = null; /* created graph */

		this.setReader(reader); /* note the reader */
		while (true) { /* read loop for nodes and edges */
			
			c = this.read(); /* read the type indicator */
			
			if ((c == 'n') || (c == 'v')) {
				s = this.readInt(); /* if a node/vertex follows, */
				if (s != ++e) /* read and check the node index */
					throw new IOException("expected " + e + " instead of " + s);
				t = this.readType(); /* read the node label and code it */
				c = this.nodemgr.add(t);
				if (c < 0)
					throw new IOException("invalid node type " + t);
				if (graph == null) /* if it does not yet exist, */
					graph = new Graph(this); /* create a graph */
				graph.addNode(c);
			} /* add a node to the graph */
			else if ((c == 'a') || (c == 'd') || (c == 'e')) {
				if (graph == null)
					graph = new Graph();
				s = this.readInt(); /* if an edge/arc follows, */
				d = this.readInt(); /* read source and dest. node index */
				if ((s <= 0) || (s > graph.nodecnt))
					throw new IOException("invalid node index " + s);
				if ((d <= 0) || (d > graph.nodecnt))
					throw new IOException("invalid node index " + d);
				t = this.readType(); /* read the edge label and code it */
				c = this.edgemgr.add(t);
				if (c < 0)
					throw new IOException("invalid edge type " + t);
				if (graph == null) /* create a graph if necessary and */
					graph = new Graph(this); /* add an edge to the graph */
				graph.addEdge(s - 1, d - 1, c);
			} else if (c == '%' || c=='t') { /* if comment line, */
				do {
					c = this.read();
				} /* skip the rest of the line */
				while ((c >= 0) && (c != '\n'));

			
			}else { /* if anything else follows */
				this.unread(c);
				return graph;
			}
		} /* return the created graph */
	} /* parse() */

	/*------------------------------------------------------------------*/
	/**
	 * Create a string description of a graph.
	 * 
	 * @param graph
	 *            the graph to describe
	 * @return <code>null</code>, since this is not supported
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public String describe(Graph graph) {
		StringBuilder builder = new StringBuilder();
		int i, t; /* loop variable, buffer */
		Node n; /* to traverse the nodes */
		Edge e; /* to traverse the edges */

		for (i = 0; i < graph.nodecnt; i++) {
			n = graph.nodes[i]; /* traverse the nodes */
			builder.append("n " + (n.mark = i + 1));
			builder.append(' '); /* write node index and separator */
			t = n.type; /* get the destination node type */
			if (graph.coder != null)
				t = graph.coder.decode(t);
			builder.append(this.nodemgr.getName(t));
			builder.append('\n'); /* write the node label */
		} /* and terminate the line */
		for (i = 0; i < graph.edgecnt; i++) {
			e = graph.edges[i]; /* traverse the edges */
			builder.append("e " + e.src.mark);
			builder.append(" " + e.dst.mark);
			builder.append(' '); /* write node indices and separator */
			builder.append(this.edgemgr.getName(e.type));
			builder.append('\n'); /* write the edge label */
		} /* and terminate the line */
		for (i = graph.nodecnt; --i >= 0;)
			graph.nodes[i].mark = -1; /* unmark all nodes and edges */
		
		return builder.toString();
	}

	/*------------------------------------------------------------------*/
	/**
	 * Write a description of an attributed graph.
	 * 
	 * @param graph
	 *            the graph to write
	 * @param writer
	 *            the writer to write to
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public void write(Graph graph, Writer writer) throws IOException { /*
																		 * ---
																		 * write
																		 * a
																		 * graph
																		 * description
																		 */
		int i, t; /* loop variable, buffer */
		Node n; /* to traverse the nodes */
		Edge e; /* to traverse the edges */

		for (i = 0; i < graph.nodecnt; i++) {
			n = graph.nodes[i]; /* traverse the nodes */
			writer.write("n " + (n.mark = i + 1));
			writer.write(' '); /* write node index and separator */
			t = n.type; /* get the destination node type */
			if (graph.coder != null)
				t = graph.coder.decode(t);
			writer.write(this.nodemgr.getName(t));
			writer.write('\n'); /* write the node label */
		} /* and terminate the line */
		for (i = 0; i < graph.edgecnt; i++) {
			e = graph.edges[i]; /* traverse the edges */
			writer.write("e " + e.src.mark);
			writer.write(" " + e.dst.mark);
			writer.write(' '); /* write node indices and separator */
			writer.write(this.edgemgr.getName(e.type));
			writer.write('\n'); /* write the edge label */
		} /* and terminate the line */
		for (i = graph.nodecnt; --i >= 0;)
			graph.nodes[i].mark = -1; /* unmark all nodes and edges */
	} /* write() */

	/*------------------------------------------------------------------*/
	/**
	 * Main function for testing basic functionality.
	 * <p>
	 * It is tried to parse the first argument as a list description of a graph.
	 * If this is successful, the parsed graph is printed using the function
	 * <code>write()</code>.
	 * </p>
	 * 
	 * @param args
	 *            the command line arguments
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public static void main(String args[]) { /* --- main function for testing */
		if (args.length != 1) { /* if wrong number of arguments */
			System.err.println("usage: java moss.NEList <file>");
			return; /* print a usage message */
		} /* and abort the program */
		try { /* try to parse the description */
			Notation ntn = new NEList(); /* as a list notation */
			Reader reader = new FileReader(args[0]);
			Graph graph = ntn.parse(reader);
			reader.close();

			Writer writer = new OutputStreamWriter(System.out);
			ntn.write(graph, writer); /* write the parsed graph and */
			writer.flush();
		} /* flush the writer afterwards */
		catch (IOException e) { /* catch and report parse errors */
			System.err.println(e.getMessage());
		}
	} /* main() */

} /* class NEList */
