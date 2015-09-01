/*----------------------------------------------------------------------
  File    : LiNoG.java
  Contents: class for a simple line notation for (attributed) graphs
  Authors : Christian Borgelt
  History : 2007.07.02 file created from file SLN.java
            2007.07.05 first version completed
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/*--------------------------------------------------------------------*/
/** Class for a simple line notation for (attributed) graphs
 *  @author Christian Borgelt
 *  @since  2007.07.02 */
/*--------------------------------------------------------------------*/
public class LiNoG extends FreeNtn {
  
  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the parsed graph */
  private Graph        graph;
  /** the label to node index map */
  private int[]        labels;
  /** the node type recoder for a graph to describe */
  private Recoder      coder;
  /** the buffer for creating graph descriptions */
  private StringBuffer desc;
  /** the buffer for reading and creating node descriptions */
  private StringBuffer buf;

  /*------------------------------------------------------------------*/
  /** Create an attributed graph line notation object.
   *  <p>By default this notation uses <code>FreeTypeMgr</code> objects
   *  for the type managers, which can be extended dynamically.</p>
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public LiNoG ()
  { this(new FreeTypeMgr(), new FreeTypeMgr()); }

  /*------------------------------------------------------------------*/
  /** Create an attributed graph line notation object.
   *  @param  nodemgr the manager for the node types
   *  @param  edgemgr the manager for the edge types
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public LiNoG (TypeMgr nodemgr, TypeMgr edgemgr)
  {                             /* --- create an LiNoG object */ 
    this.nodemgr = nodemgr;     /* store the given */
    this.edgemgr = edgemgr;     /* type managers */
    this.labels  = new int[64]; /* create a label to node index map */
    this.desc = this.buf = null;/* clear the description buffer */
  }  /* LiNoG() */

  /*------------------------------------------------------------------*/
  /** Whether this is a line notation (single line description).
   *  @return <code>true</code>, since LiNoG is a line notation
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isLine ()
  { return true; }

  /*------------------------------------------------------------------*/
  /** Read a node.
   *  @param  bracket whether a bracket '[' is required
   *  @return the index of the added node
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readNode (boolean bracket) throws IOException
  {                             /* --- read a node description */
    int    c, i;                /* current character, loop variable */
    int    t, n;                /* node type, label buffer */
    String name;                /* node type name */
    char   e1, e2;              /* end characters */

    /* --- get the node type --- */
    if (this.buf == null)       /* create a read buffer */
      this.buf = new StringBuffer();
    this.buf.setLength(0);      /* clear the read buffer */
    c = this.read();            /* read the first character */
    if (bracket && (c != '['))  /* check for a bracket if needed */
      throw new IOException("missing '['");
    if      (c == '[') { e1 = ']'; e2 = ';'; }
    else if (c <=  0 ) { e1 = e2 = '.'; }
    else if (c != '.') { e1 = e2 = '.'; this.buf.append((char)c); }
    else throw new IOException("empty node type");
    while (true) {              /* read loop for the node type name */
      c = this.read();          /* read the next character */
      if ((c < 0) || (c == e1) || (c == e2)) break;
      this.buf.append((char)c); /* append characters until */
    }                           /* the type name ends */
    t = this.nodemgr.add(name = this.buf.toString());
    if (t < 0) throw new IOException("unknown node type '" +name +"'");
    t = this.graph.addNode(t);  /* add a node to the graph */
    if (e1 != ']') {            /* if no bracket, return node index */
      this.unread(c); return t; }
    if (c != ';') {             /* if no label follows */
      if (c != ']') throw new IOException("missing ']'");
      return t;                 /* check for a closing bracket */
    }                           /* and return the node type */

    /* --- get the node label --- */
    for (i = n = 0; true; i++){ /* digit read loop */
      c = this.read();          /* read the next character */
      if ((c < '0') || (c > '9')) break;
      n = n *10 +(c -'0');      /* compute the label value */
    }
    if ((i <= 0) || (n <= 0))   /* check for an empty label */
      throw new IOException("empty or invalid label");
    if (n >= this.labels.length) {
      int[] v = new int[n +16]; /* if the label array is full */
      System.arraycopy(this.labels, 0, v, 0, this.labels.length);
      this.labels = v;          /* enlarge the label array */
      for (i = this.labels.length; i < v.length; i++)
        v[i] = -1;              /* clear the new part */
    }                           /* of the label array */
    if (this.labels[n] >= 0)    /* check for duplicate labels */
      throw new IOException("duplicate label " +n);
    if (c != ']') throw new IOException("missing ']'");
    return this.labels[n] = t;  /* store and return the node index */
  }  /* readNode() */

  /*------------------------------------------------------------------*/
  /** Read an edge type (and a possibly following reference).
   *  @return the type of the edge
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2007.07.05 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readEdge () throws IOException
  {                             /* --- read an edge description */
    int    c, i;                /* current character, loop variable */
    int    t, n;                /* edge type, label buffer */
    String name;                /* edge type name */

    /* --- get the edge type --- */
    this.labels[0] = -1;        /* clear the label buffer */
    c = this.read();            /* read the first character */
    if (c == '.') return -1;    /* check for a null edge */
    this.buf.setLength(0);      /* clear the read buffer */
    do {                        /* append characters until */
      this.buf.append((char)c); /* the type name ends */
      c = this.read();          /* read the next character */
    } while ((c >= 0) && (c != '@') && (c != '['));
    t = this.edgemgr.add(name = this.buf.toString());
    if (t < 0) throw new IOException("unknown edge type '" +name +"'");
    if (c != '@') {             /* if no ref., return edge type */
      this.unread(c); return t; }

    /* --- get the node label --- */
    for (i = n = 0; true; i++){ /* digit read loop */
      c = this.read();          /* read the next character */
      if ((c < '0') || (c > '9')) break;
      n = n *10 +(c -'0');      /* compute the label value */
    }
    if ((i <= 0) || (n <= 0))   /* check for an empty label */
      throw new IOException("empty or invalid label");
    if ((n >= this.labels.length)
    ||  (this.labels[n] < 0))   /* check for a known label */
      throw new IOException("unknown label (forward reference?)");
    if (c != ';')               /* if end is not a separator, */
      this.unread(c);           /* push back character after label */
    this.labels[0] = n;         /* store the label value */
    return t;                   /* return the edge type */
  }  /* readEdge() */

  /*------------------------------------------------------------------*/
  /** Recursive function to parse (a branch of) an attributed graph.
   *  @param  src the source node for the next edge
   *  @return whether a started branch was closed
   *          (i.e. whether the last character was a ')')
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private boolean parse (int src) throws IOException
  {                             /* --- parse a graph description */
    int c, t;                   /* next character, edge type */
    int dst;                    /* index of destination node */

    if (src < 0)                /* if there is no source node, */
      src = this.readNode(false);       /* read the first node */
    while (true) {              /* parse loop for a branch */
      c = this.read();          /* read the next character */
      if (c < 0) return false;  /* if at end, abort indicating no ')' */
      if (c == ')')             /* if at the end of a branch */
        return true;            /* abort indicating a ')' */
      if (c == '(') {           /* if at the start of a branch */
        if (!this.parse(src))   /* recursively parse the branch */
          throw new IOException("missing ')'");
        continue;               /* check for a closing ')' */
      }
      this.unread(c);           /* push back the character */
      t = this.readEdge();      /* read an edge (and maybe a label) */
      c = this.labels[0];       /* get the possibly read label */
      if (c < 0) dst = this.readNode(t >= 0);
      else       dst = this.labels[c];
      if (t >= 0) {             /* if the edge is not null */
        if (src == dst)         /* and no loop (self connection) */
          throw new IOException("loop edge (source = destination)");
        this.graph.addEdge(src, dst, t);
      }                         /* add an edge to the graph */
      if (c < 0) src = dst;     /* if edge was not a backward ref., */
    }                           /* go to the destination node */
  }  /* parse() */
  
  /*------------------------------------------------------------------*/
  /** Parse a description of an attributed graph.
   *  @param  reader the reader to read from
   *  @return the parsed attributed graph
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph parse (Reader reader) throws IOException
  {                             /* --- parse a molecule description */
    this.setReader(reader);     /* note the reader */
    for (int i = this.labels.length; --i >= 0; )
      this.labels[i] = -1;      /* clear the label node index map */
    this.graph = new Graph(this);
    if (this.parse(-1))         /* create a new graph and parse it */
      throw new IOException("superfluous ')'");
    this.graph.opt();           /* optimize memory usage and */
    return this.graph;          /* return the created graph */
  }  /* parse() */

  /*------------------------------------------------------------------*/
  /** Create a description of a node.
   *  @param  type  the type  of the node
   *  @param  label the label of the node
   *  @return the description of the node
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private String describe (int type, int label)
  {                             /* --- describe a node */
    if ((type & Node.CHAIN) != 0) return "[*]";
    if (this.buf == null)       /* create a description buffer */
      this.buf = new StringBuffer();
    this.buf.setLength(0);      /* clear the description buffer */
    this.buf.append('[');       /* store the node type name */
    if (type == Node.WILDCARD) this.buf.append('*');
    else this.buf.append(this.nodemgr.getName(type));
    if (label > 0) {            /* add a label if necessary */
      this.buf.append(';'); this.buf.append(label); }
    this.buf.append(']');       /* terminate the node description */
    return this.buf.toString(); /* return the created description */
  }  /* describe() */

  /*------------------------------------------------------------------*/
  /** Trim an unnecessary ';' from the description
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void trim ()
  {                             /* --- trim an unnecessary ';' */
    int n = this.desc.length()-1;
    if ((n > 0) && (this.desc.charAt(n) == ';'))
      this.desc.setLength(n);   /* remove a trailing ';' */
  }  /* trim() */

  /*------------------------------------------------------------------*/
  /** Recursive function to create a description.
   *  @param  node the current node
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void out (Node node)
  {                             /* --- recursive part of output */
    int    i, k, n;             /* loop variables, number of branches */
    Edge   e;                   /* to traverse the edges */
    Node   d;                   /* destination of an edge */
    String name;                /* edge type name */

    k = node.type;              /* get and decode the node type */
    if (this.coder != null) k = this.coder.decode(k);
    n = node.mark;              /* if the node needs a label, */
    if (node.mark < 0)          /* get the next available number */
      node.mark = ++this.labels[0];
    this.desc.append(this.describe(k, node.mark));
                                /* append the node description */
    for (i = node.deg; --i >= 0; ) {
      e = node.edges[i];        /* traverse the unprocessed edges */
      if (e.mark == 0) continue;   /* that lead back to some node */
      d = (e.src != node) ? e.src : e.dst;
      if (d.mark <= 0) { n++; continue; }
      e.mark = 0;               /* mark the edge as processed */
      this.trim();              /* trim a possible ';' */
      this.desc.append(this.edgemgr.getName(e.type & Edge.TYPEMASK));
      this.desc.append('@'); this.desc.append(d.mark);
      this.desc.append(';');    /* append backward connection */
    }                           /* (edge name and node label) */
    /* Here n contains the number of branches leading away from the  */
    /* node, which is the number of edges leading to unvisited nodes */
    /* minus the number of edges that lead back to the node through  */
    /* labels (obtained from the original value of node.mark).       */
    for (i = 0; i < node.deg; i++) {
      e = node.edges[i];        /* traverse the unprocessed edges */
      if (e.mark == 0) continue;
      e.mark = 0;               /* mark the edge as processed and */
      name = this.edgemgr.getName(e.type & Edge.TYPEMASK);
      if (--n > 0) {            /* start branch if it is not the last */
        this.trim(); this.desc.append("("); }
      else {                    /* if this is the last branch */
        k = (name.length() > 0) ? name.charAt(0) : -1;
        if ((k < '0') || (k > '9')) this.trim();
      }                         /* check first character of edge name */
      this.desc.append(name);   /* append the edge description */
      this.out((e.src != node) ? e.src : e.dst);
      if (n > 0) {              /* process the branch recursively, */
        this.trim();            /* and if it is not the last branch, */
        this.desc.append(")");  /* trim a possible ';' and */
      }                         /* terminate the branch */
    }                           /* (last branch needs no parantheses) */
  }  /* out() */

  /*------------------------------------------------------------------*/
  /** Create a description of a given attributed graph.
   *  @param  graph the graph to describe
   *  @return a description of the given graph
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String describe (Graph graph)
  {                             /* --- create a string description */
    int  i, n;                  /* loop variable, component counter */
    Node a;                     /* to traverse the nodes */

    if (this.desc == null)      /* create a description buffer */
      this.desc = new StringBuffer();
    this.desc.setLength(0);     /* clear the description buffer */
    this.labels[0] = 0;         /* clear the label buffer */
    this.coder = graph.coder;   /* note the node type recoder */
    for (i = graph.nodecnt; --i >= 0; )
      graph.nodes[i].mark = 1;  /* mark the nodes of the graph */
    for (i = graph.edgecnt; --i >= 0; )
      graph.edges[i].mark = 0;  /* mark the edges of the graph */
    for (i = n = 0; i < graph.nodecnt; i++) {
      a = graph.nodes[i];       /* traverse the unprocessed nodes */
      if (a.mark < 0) continue; /* (unprocessed connected components) */
      if (n++    > 0) {         /* separate components by dots */
        this.trim(); this.desc.append('.'); }
      Notation.mark(a);         /* mark the visits of each node, */
      this.out(a);              /* output a connected component, */
      Notation.unmark(a);       /* and clear the node markers */
    }
    this.trim();                /* trim a possible ';' */
    return this.desc.toString();/* return the created description */
  }  /* describe() */

  /*------------------------------------------------------------------*/
  /** Write a description of a graph.
   *  @param  graph  the graph to write
   *  @param  writer the writer to write to
   *  @since  2007.07.05 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void write (Graph graph, Writer writer) throws IOException
  { writer.write(this.describe(graph)); }

  /*------------------------------------------------------------------*/
  /** Main function for testing basic functionality.
   *  <p>It is tried to parse the first argument as an LiNoG
   *  description of a graph. If this is successful, the parsed graph
   *  is printed using the function <code>describe()</code>.</p>
   *  @param  args the command line arguments
   *  @since  2007.07.02 (Christian Borgelt) */
  /*------------------------------------------------------------------*/
  
  public static void main (String args[])
  {                             /* --- main function for testing */
    if (args.length != 1) {     /* if wrong number of arguments */
      System.err.println("usage: java moss.LiNoG <LiNoG string>");
      return;                   /* print a usage message */
    }                           /* and abort the program */
    try {                       /* try to parse the description */
      Notation ntn   = new LiNoG();
      Graph    graph = ntn.parse(new StringReader(args[0]));
      System.out.println(ntn.describe(graph)); }
    catch (IOException e) {     /* catch and report parse errors */
      System.err.println(e.getMessage()); }
  }  /* main() */
  
}  /* class LiNoG */
