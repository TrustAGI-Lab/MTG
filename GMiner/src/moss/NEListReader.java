/*----------------------------------------------------------------------
  File    : NEListReader.java
  Contents: Class for a simple node/edge list format reader
  Author  : Christian Borgelt
  History : 2007.06.22 file created as ListFmt.java
            2007.06.29 split into reader and writer
            2007.07.06 function getDesc added
            2007.08.16 bug in function main() fixed (type managers)
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;

/*--------------------------------------------------------------------*/
/**
 * Class for readers for a simple node/edge list format.
 * 
 * @author Christian Borgelt
 * @since 2007.06.22
 */
/*--------------------------------------------------------------------*/
public class NEListReader extends GraphReader {

	/*------------------------------------------------------------------*/
	/* instance variables */
	/*------------------------------------------------------------------*/
	/** the notation for line descriptions */
	private Notation linog;
	/** the buffer for an input field */
	private StringBuffer buf;

	/*------------------------------------------------------------------*/
	/**
	 * Create a reader for a simple node/edge list format.
	 * 
	 * @param reader
	 *            the reader to read from
	 * @param mode
	 *            the read mode
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public NEListReader(Reader reader, int mode) { /*
													 * --- create a node/edge
													 * list reader
													 */
		super(reader, mode); /* store the arguments */
		this.ntn = new NEList(); /* create a notation and a buffer */
		this.buf = new StringBuffer();
	} /* NEListReader() */

	/*------------------------------------------------------------------*/
	/**
	 * Read an (optional) header.
	 * 
	 * @return <code>false</code>, because headers are not supported
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean readHeader() throws IOException {
		return false;
	}

	/*------------------------------------------------------------------*/
	/**
	 * Read a graph name.
	 * 
	 * @return the graph name read
	 * @throws IOException
	 *             if no name could be read
	 * @since 2007.06.22 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private String readName() throws IOException { /* --- read a graph name */
		int c = this.read(); /* check for end of line or separator */
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
			return ""; /* (empty graph name) */
		this.buf.setLength(0); /* read the graph name */
		while ((c >= 0) && (c != '\n')) {
			this.buf.append((char) c);
			c = this.read();
		}
		for (int i = this.buf.length(); --i >= 0;) {
			c = this.buf.charAt(i); /* traverse last characters */
			if ((c != ' ') && (c != '\t') && (c != '\r')) {
				this.buf.setLength(i + 1);
				break;
			}
		} /* remove trailing blanks */
		return this.buf.toString(); /* return the graph name */
	} /* readName() */
	
	

	/*------------------------------------------------------------------*/
	/**
	 * Read a field.
	 * 
	 * @return the field read
	 * @throws IOException
	 *             if no field could be read
	 * @since 2007.06.29 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private String readField() throws IOException { /* --- read a field */
		int c = this.read(); /* check for a separator */
		if ((c != ' ') && (c != '\t'))
			throw new IOException("separator expected instead of " + (char) c
					+ "' (" + c + ")");
		do {
			c = this.read();
		} /* skip leading blanks */
		while ((c == ' ') || (c == '\t'));
		this.buf.setLength(0); /* clear the buffer for the field */
		while ((c >= 0) && (c != ' ') && (c != '\t') && (c != '\n')) {
			this.buf.append((char) c); /* append the character read */
			c = this.read(); /* and read the next character */
		}
		this.unread(c); /* push back the last character */
		return this.buf.toString().trim();
	} /* readField() *//* return the field read */
	
	
	  public String readLine () throws IOException
	  {                             /* --- read the next input line */
	    int c = this.read();        /* read the next character */
	    if (c < 0) return null;     /* and check for end of input */
	    this.buf.setLength(0);      /* clear the read buffer for a line */
	    while ((c >= 0) && (c != '\n')) {
	      this.buf.append((char)c); /* append current character */
	      c = this.read();          /* to the read buffer and */
	    }                           /* read the next character */
	    return this.buf.toString(); /* return the input line read */
	  }  /* readLine() */

	/*------------------------------------------------------------------*/
	/**
	 * Check for an empty line.
	 * 
	 * @param c
	 *            the next character (already read)
	 * @return <code>true</code>, because if the line is not empty, an exception
	 *         is thrown
	 * @throws IOException
	 *             if the current line is not empty
	 * @since 2007.06.29 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	private boolean empty(int c) throws IOException { /*
													 * --- check for an empty
													 * line
													 */
		while ((c == ' ') && (c == '\t'))
			c = this.read(); /* skip blank characters */
		if ((c >= 0) && (c != '\n') && (c != '\r'))/* check for end of line */{
			System.out.println((char) c);
			throw new IOException("(rest of) line is not empty");
		}
		return true; /* always return 'true' */
	} /* empty() */

	/*------------------------------------------------------------------*/
	/**
	 * Read a graph.
	 * <p>
	 * The next graph description is read and split into the graph name, the
	 * associated value, the actual graph description, and (only in mode
	 * <code>SUBS</code>) the support information.
	 * </p>
	 * These properties may then be retrieved with the functions
	 * <code>getName()<code>, <code>getValue()</code>, <code>getGraph()</code>
	 * etc.</p>
	 * 
	 * @return whether a graph description could be read (otherwise the end of
	 *         the input has been reached)
	 * @since 2007.02.24 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public boolean readGraph() throws IOException { /* --- read the next graph */
		int c; /* buffer for a character */
		String s; /* buffer for a field */
		this.graph = this.ntn.parse(this);
		if (this.graph == null){
			//System.out.println("Not graphs!");
			return false;
		}
		this.nodes = this.graph.getNodeCount();
		this.edges = this.graph.getEdgeCount();
		this.name = null; /* parse a graph description */
		this.srel = this.crel = this.value = 0.0F;
		this.sabs = this.cabs = 0; /* clear the additional information */
		c = this.ntn.getDelim(); /* check the delimiter character */
		if (c != 'g')
			return this.empty(c);
		this.name = this.readName();/* read a graph name if present */
		s = null; /* clear the field buffer */
		try { /* check for additional information */
			c = this.read(); /* read the next character */
			if (this.mode != SUBS) { /* if graphs */
				if (c != 'x')
					return this.empty(c);
				this.value = Float.parseFloat(s = this.readField());
				
				if(mode == SEMIGRAPHS){ //read semi graphs
					c = this.read();
					c=this.read();
					if (c != 'L') { // read if it is label;
						return this.empty(c);
					}
					s = this.readField();
					this.isLabel = Boolean.parseBoolean(s );
				}
				
			} else if (this.mode == SUBS) { /* if substructures */
				if (c != 's')
					return this.empty(c);
				
				this.nodes=Integer.parseInt(s = this.readField());

				this.edges=Integer.parseInt(s = this.readField());
				this.sabs = Integer.parseInt(s = this.readField());
				this.srel = Float.parseFloat(s = this.readField());
				this.cabs = Integer.parseInt(s = this.readField());
				this.crel = Float.parseFloat(s = this.readField());
	
			} /* read support information */


		} catch (NumberFormatException e) {
			throw new IOException("malformed number '" + s + "'");
		}
		this.empty(this.read()); /* check if rest of line is empty */
		return this.empty(this.read()); 
	} /* readGraph() *//* check for an empty line */

	/*------------------------------------------------------------------*/
	/**
	 * Get a (line) description of the current graph.
	 * <p>
	 * Since a connection table is not a line description, it is reformatted
	 * into the LiNoG format.
	 * </p>
	 * 
	 * @return a line description (LiNoG) of the current graph
	 * @since 2007.07.06 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public String getDesc() { /* --- get a (line) description */
		if (this.desc == null) { /* if there is no description */
			if (this.graph == null)
				return null;
			if (this.linog == null) /* create a line notation */
				this.linog = new LiNoG(); /* if it does not yet exist */
			this.linog.setTypeMgrs(this.ntn);
			this.desc = this.linog.describe(this.graph);
		} /* create a description if possible */
		return this.desc; /* return the description */
	} /* getDesc() */

	/*------------------------------------------------------------------*/
	/**
	 * Main function for testing basic functionality.
	 * 
	 * @param args
	 *            the command line arguments
	 * @since 2007.06.29 (Christian Borgelt)
	 */
	/*------------------------------------------------------------------*/

	public static void main(String args[]) { /* --- main function for testing */
		NEListReader reader; /* reader for the input file */
		NEListWriter writer; /* writer for the output file */

		if (args.length != 2) { /* if wrong number of arguments */
			System.err.println("usage: java moss.NEListReader <in> <out>");
			return; /* print a usage message */
		} /* and abort the program */

		try { /* try to read an NEList format file */
			reader = new NEListReader(new FileReader(args[0]), GRAPHS);
			writer = new NEListWriter(new FileWriter(args[1]), GRAPHS);
			writer.getNotation().setTypeMgrs(reader.getNotation());
			while (reader.readGraph()) {
				writer.setName(reader.getName());
				writer.setGraph(reader.getGraph());
				writer.setValue(reader.getValue());
				writer.writeGraph(); /* while there are more graphs, */
			} /* read and write graphs */
			reader.close();
			writer.close();
		} catch (IOException e) { /* catch and report parse errors */
			System.err.println(e.getMessage());
		}
	} /* main() */

} /* class NEListReader */
