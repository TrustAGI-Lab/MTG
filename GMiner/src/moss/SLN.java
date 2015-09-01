/*----------------------------------------------------------------------
  File    : SLN.java
  Contents: class for the SYBYL Line Notation
  Authors : Christian Borgelt
  History : 2006.08.16 file created from file SLNTokenizer.java
            2006.10.23 parameter graph added to function parse
            2006.10.25 comments in javadoc style added
            2006.10.27 output of special atom types modified
            2006.10.31 adapted to refactored classes
            2006.11.05 map from element names to element codes added
            2006.11.12 shorthand hydrogen reading and writing added
            2007.03.04 function isLine() added
            2007.03.19 bug in label output fixed (missing '@')
            2007.06.21 adapted to type managers (atoms and bonds)
            2007.06.22 rewritten to use readers and writers
            2007.06.26 charge parsing improved
            2007.07.02 buffers for creating descriptions added
            2007.07.05 loop check added (it must be src != dst)
            2008.04.30 bug in readAtom() fixed (separator reading)
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/*--------------------------------------------------------------------*/
/** Class for the SYBYL line notation (SLN, Tripos, Inc.).
 *  @author Christian Borgelt
 *  @since  2006.08.12 */
/*--------------------------------------------------------------------*/
public class SLN extends MoleculeNtn {
  
  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the parsed molecule */
  private Graph        mol;
  /** the label to node index map */
  private int[]        labels;
  /** the atom type recoder for a molecule to describe */
  private Recoder      coder;
  /** the buffer for creating atom descriptions */
  private StringBuffer buf;

  /*------------------------------------------------------------------*/
  /** Create a SYBYL line notation object.
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public SLN ()
  {                             /* --- create an SLN object */ 
    this.nodemgr = new AtomTypeMgr();  /* create the node and */
    this.edgemgr = new BondTypeMgr();  /* edge type managers */
    this.labels  = new int[64]; /* create a label to node index map */
    this.desc    = this.buf = null;
  }  /* SLN() */                /* clear the description buffers */

  /*------------------------------------------------------------------*/
  /** Whether this is a line notation (single line description).
   *  @return <code>true</code>, since SLN is a line notation
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isLine ()
  { return true; }

  /*------------------------------------------------------------------*/
  /** Read an atom type, including shorthand hydrogens.
   *  @return the type of the next atom
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readAtom () throws IOException
  {                             /* --- read an atom description */
    int i, k;                   /* loop variable, buffer */
    int c, n;                   /* current and next character */
    int atom = 0;               /* number/code of an atom */

    this.labels[0] = 0;         /* clear the label buffer */

    /* --- find the atom number --- */
    c = this.read();            /* get first element character */
    if ((c < 'A') || (c > 'Z')) /* check for a letter */
      throw new IOException("invalid element '" +(char)c +"'");
    n = this.read();            /* get second element character */
    if ((n < 'a') || (n > 'z')){/* if single letter element */
      k = AtomTypeMgr.map[c-'A'][26];
      if (k <= 0) throw new IOException("invalid element '"
                                        +(char)c +"'");
      this.unread(n); }         /* push back the unused character */
    else {                      /* if double letter element */
      k = AtomTypeMgr.map[c-'A'][n-'a'];
      if (k <= 0) throw new IOException("invalid element '"
                                        +(char)c +(char)n +"'");
    }                           /* (both characters were used) */
    atom |= k;                  /* add the element code */
    c = this.read();            /* read the next character */
    if (c != '[') {             /* if at end of atom description */
      if (c >= 0) this.unread(c);
      return atom | AtomTypeMgr.codeHydros(this.getHydros());
    }                           /* add shorthand hydrogens */

    /* --- get a possible label --- */
    for (i = n = 0; true; i++){ /* read a possible label */
      c = this.read();          /* read the next character */
      if ((c < '0') || (c > '9')) break;
      n = n *10 +(c -'0');      /* compute the label value */
    }
    if (c < 0) throw new IOException("missing ']'");
    this.labels[0] = n;         /* note the label of the atom */
    if (c == ']') {             /* if at end of atom description */
      if (i <= 0) throw new IOException("empty label");
      return atom | AtomTypeMgr.codeHydros(this.getHydros());
    }                           /* add shorthand hydrogens */
    if (i > 0) {                /* check for a separator */
      if ((c != ';') && (c != ':'))
        throw new IOException("missing ';' after label");
      c = this.read();          /* read the character */
    }                           /* after the separator */

    /* --- get a possible charge --- */
    while ((c != ']')           /* attributes read loop */
    &&     (c >= 0)) {          /* (but evaluate only charge) */
      if ((c == '+') || (c == '-')) {
        n = this.read();        /* if a sign follows, check */
        if (n == c) n = '2';    /* for another sign or a digit */
        else if ((n <= '0') || (n > '9')) { this.unread(n); n = '1'; }
        atom |= AtomTypeMgr.codeCharge((c == '+') ? n -'0' : '0' -n);
        break;                  /* add the charge to the code, */
      }                         /* and abort the loop */
      while ((c != ']') && (c != ';') && (c != ':') && (c >= 0))
        c = this.read();        /* skip until separator or delimiter */
      if ((c == ';') || (c == ':')) c = this.read();
    }                           /* consume a separator */

    /* --- skip additional information --- */
    while ((c != ']') && (c >= 0))
      c = this.read();          /* skip characters until ']' */
    if (c < 0) throw new IOException("missing ']'");
    atom |= AtomTypeMgr.codeHydros(this.getHydros());
    return atom;                /* return the atom code */
  }  /* readAtom() */

  /*------------------------------------------------------------------*/
  /** Read an atom label.
   *  @return the value of the label
   *  @throws IOException if the label is incomplete or invalid
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readLabel () throws IOException
  {                             /* --- get a ref. to a labeled atom */
    int c = this.read();        /* get the next character */
    if ((c < '1') || (c > '9')) /* check for a digit */
      throw new IOException("incomplete or invalid label");
    int k = 0;                  /* init. the value of the label */
    do {                        /* process the digits */
      k = k *10 +c -'0';        /* recompute the label value */
      c = this.read();          /* read the next character */
    } while ((c >= '0') && (c <= '9'));
    if (c >= 0) this.unread(c); /* push back character after label */
    return k;                   /* return the label value */
  }  /* readLabel() */

  /*------------------------------------------------------------------*/
  /** Recursive function to parse (a branch of) a molecule.
   *  @param  src the source atom for the next bond
   *  @return whether a started branch was closed
   *          (i.e. whether the last character was a ')')
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private boolean parse (int src) throws IOException
  {                             /* --- parse a molecule description */
    int c;                      /* next character */
    int a, b, i;                /* atom and bond types, buffer */
    int dst;                    /* index of destination atom */

    a = dst = -1;               /* clear the atom type and index */
    b = BondTypeMgr.UNKNOWN;    /* and the bond type */
    while (true) {              /* parse loop for a branch */
      c = this.read();          /* read the next character */
      if (c < 0) return false;  /* if at end, abort indicating no ')' */
      switch (c) {              /* get and evaluate next character */

        /* -- branches -- */
        case ')':               /* if at the end of a branch */
          if (b != BondTypeMgr.UNKNOWN)
            throw new IOException("unexpected ')'");
          return true;          /* check for a preceding bond */
        case '(':               /* if at the start of a branch */
          if ((src < 0) || (b != BondTypeMgr.UNKNOWN))
            throw new IOException("unexpected '('");
          if (!this.parse(src)) /* recursively parse the branch */
            throw new IOException("missing ')'");
          break;                /* check for a closing ')' */

        /* -- labels -- */
        case '@':               /* reference to a labeled atom */
          i = this.readLabel(); /* read and check the label value */
          if (i >= this.labels.length)
            throw new IOException("invalid label " +i);
          dst = this.labels[i]; /* get and check the atom index */
          if (dst < 0) throw new IOException("invalid label " +i);
          a = 0; break;         /* clear the atom type */

        /* -- bonds -- */
        case '.': b = BondTypeMgr.NULL;     break;
        case '-': b = BondTypeMgr.SINGLE;   break;
        case ':': b = BondTypeMgr.AROMATIC; break;
        case '=': b = BondTypeMgr.DOUBLE;   break;
        case '#': b = BondTypeMgr.TRIPLE;   break;

        /* -- atoms -- */
        case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
        case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
        case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
        case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
        case 'Y': case 'Z':     /* if uppercase letter */
          this.unread(c);       /* push back the character */
          a   = this.readAtom();/* and read an atom */
          dst = this.mol.addNode(AtomTypeMgr.removeExts(a));
          i   = this.labels[0]; /* get atom type and add a new atom */
          if (i <= 0) break;    /* if there is no label, abort */
          if (i >= this.labels.length) {
            int[] v = new int[i +16];  /* if the label array is full */
            System.arraycopy(this.labels, 0, v, 0, this.labels.length);
            this.labels = v;    /* enlarge the label array */
            for (int k = this.labels.length; k < v.length; k++)
              v[k] = -1;        /* clear the new part */
          }                     /* of the label array */
          this.labels[i] = dst; /* store the new atom index */
          break;                /* with the label read */

        default : throw new IOException("invalid character '"
                                        +(char)c +"' (" +c +")");
      }                         /* catch all other characters */
      if ((src < 0) && (b != BondTypeMgr.UNKNOWN))
        throw new IOException("unexpected bond");
      if (dst <  0) continue;   /* if no bond to add, continue */
      if (src >= 0) {           /* if there is no source, skip bond */
        if (b == BondTypeMgr.UNKNOWN) /* if the bond type is unknown, */
          b = BondTypeMgr.SINGLE;     /* the bond must be single */
        if (b != BondTypeMgr.NULL) {  /* if the bond is not null */
          if (src == dst)             /* and no loop (edge to self) */
            throw new IOException("loop bond (source = destination)");
          this.mol.addEdge(src, dst, b);
        }                       /* add the bond to the molecule */
        if (b == BondTypeMgr.AROMATIC) { /* if the bond is aromatic */
          this.mol.nodes[src].type |= AtomTypeMgr.AROMATIC;
          this.mol.nodes[dst].type |= AtomTypeMgr.AROMATIC;
        }                       /* set aromatic flag in atoms */
        b = BondTypeMgr.UNKNOWN;/* clear the bond type */
      }                         /* for the next step */
      for (i = AtomTypeMgr.getHydros(a); --i >= 0; ) {
        a = this.mol.addNode(AtomTypeMgr.HYDROGEN);
        this.mol.addEdge(dst, a, BondTypeMgr.SINGLE);
      }                         /* add shorthand hydrogens */
      if (dst > src) src = dst; /* replace the source atom and */
      dst = -1;                 /* clear the atom index */
    }                           /* (for the next loop) */
  }  /* parse() */
  
  /*------------------------------------------------------------------*/
  /** Parse a description of a molecule.
   *  @param  reader the reader to read from
   *  @return the parsed molecule
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph parse (Reader reader) throws IOException
  {                             /* --- parse a molecule description */
    this.setReader(reader);     /* note the reader */
    for (int i = this.labels.length; --i >= 0; )
      this.labels[i] = -1;      /* clear the label to node index map */
    this.mol = new Graph(this); /* create a new molecule */
    if (this.parse(-1))         /* recursively parse the molecule */
      throw new IOException("superfluous ')'");
    this.mol.opt();             /* optimize memory usage and */
    return this.mol;            /* return the created molecule */
  }  /* parse() */

  /*------------------------------------------------------------------*/
  /** Create a description of an atom.
   *  @param  type  the type  of the atom
   *  @param  label the label of the atom
   *  @return the description of the atom
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private String describe (int type, int label)
  {                             /* --- describe an atom */
    int     c, h;               /* charge, number of hydrogens */
    String  e;                  /* element name of the atom */
    boolean b;                  /* whether brackets are needed */

    if (AtomTypeMgr.isChain(type))       /* if a chain atom, */
      return AtomTypeMgr.getChainName(); /* get the special name */
    if (TypeMgr.isWildcard(type)) {      /* if a wildcard atom, */
      e = AtomTypeMgr.getWildcard();     /* get the special name and */
      c = h = 0; }                       /* clear charge, hydrogens */
    else {                      /* if this is a normal atom */
      e = AtomTypeMgr.getElemName(type); /* get the element name, */
      c = AtomTypeMgr.getCharge(type);   /* the charge of the atom, */
      h = AtomTypeMgr.getHydros(type);   /* and the hydrogens */
    }                           /* (get relevant atom properties) */
    b = (label > 0) || (c != 0);
    if (!b && (h == 0))         /* if uncharged and unlabeled */
      return e;                 /* and no hydrogens, abort */
    if (this.buf == null)       /* create a description buffer */
      this.buf = new StringBuffer();
    this.buf.setLength(0);      /* clear the description buffer */
    this.buf.append(e);         /* store the element name */
    if (b) this.buf.append("[");/* and add the label */
    if (label > 0)  { this.buf.append(label);
                      if (c != 0) this.buf.append(";"); }
    if      (c > 0) { this.buf.append("+");
                      if (c > +1) this.buf.append(+c);  }
    else if (c < 0) { this.buf.append("-");
                      if (c < -1) this.buf.append(-c);  }
    if (b) this.buf.append("]");/* add charge and closing bracket */
    if      (h > 0) { this.buf.append("H");
                      if (h >  1) this.buf.append(h); }
    return this.buf.toString(); /* add the shorthand hydrogens and */
  }  /* describe() */           /* return the created description */

  /*------------------------------------------------------------------*/
  /** Recursive function to create a description.
   *  @param  atom the current atom
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void out (Node atom)
  {                             /* --- recursive part of output */
    int  i, k, n, t;            /* loop variables, number of branches */
    Edge b;                     /* to traverse the bonds */
    Node d;                     /* destination of a bond */

    k = atom.type;              /* get and decode chemical element */
    if (this.coder != null) k = this.coder.decode(k);
    n  = MoleculeNtn.getHydros(atom, this.coder);
    k |= AtomTypeMgr.codeHydros(n);
    n = atom.mark;              /* add shorthand hydrogens */
    if (atom.mark < 0)          /* if the atom needs a label, */
      atom.mark = ++this.labels[0];    /* get the next number */
    this.desc.append(this.describe(k, atom.mark));
                                /* append the atom description */
    for (i = 0; i < atom.deg; i++) {
      b = atom.edges[i];        /* traverse the unprocessed bonds */
      if (b.mark == 0) continue;   /* that lead back to some atom */
      d = (b.src != atom) ? b.src : b.dst;
      if (d.mark <= 0) { n++; continue; }
      b.mark = 0;               /* mark the bond as processed */
      t = BondTypeMgr.getBond(b.type);
      this.desc.append(BondTypeMgr.getBondName(t));
      this.desc.append('@');    /* append backward connection */
      this.desc.append(d.mark); /* (bond name and atom label) */
    }
    /* Here n contains the number of branches leading away from the  */
    /* atom, which is the number of bonds leading to unvisited atoms */
    /* minus the number of bonds that lead back to the atom through  */
    /* labels (obtained from the original value of atom.mark).       */
    for (i = 0; i < atom.deg; i++) {
      b = atom.edges[i];        /* traverse the unprocessed bonds */
      if (b.mark == 0) continue;
      b.mark = 0;               /* mark the bond as processed */
      if (--n > 0)              /* start branch if it is not the last */
        this.desc.append("(");  /* and append the bond description */
      t = BondTypeMgr.getBond(b.type);
      this.desc.append(BondTypeMgr.getBondName(t));
      this.out((b.src != atom) ? b.src : b.dst);
      if (n > 0)                /* process the branch recursively and */
        this.desc.append(")");  /* terminate it if it is not the last */
    }                           /* (last branch needs no parantheses) */
  }  /* out() */

  /*------------------------------------------------------------------*/
  /** Create a description of a given molecule.
   *  @param  mol the molecule to describe
   *  @return a description of the given molecule
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String describe (Graph mol)
  {                             /* --- create a string description */
    int  i, n, t;               /* loop variable, counter, buffer */
    Node a, d;                  /* to traverse the atoms */
    Edge e;                     /* to access edges from hydrogens */

    if (this.desc == null)      /* create a description buffer */
      this.desc = new StringBuffer();
    this.desc.setLength(0);     /* clear the description buffer */
    this.labels[0] = 0;         /* clear the label buffer */
    this.coder = mol.coder;     /* note the atom type recoder */
    for (i = mol.nodecnt; --i >= 0; )
      mol.nodes[i].mark = 1;    /* mark the atoms of the molecule */
    for (i = mol.edgecnt; --i >= 0; )
      mol.edges[i].mark = 0;    /* mark the bonds of the molecule */
    for (i = n = 0; i < mol.nodecnt; i++) {
      a = mol.nodes[i];         /* traverse the unprocessed atoms */
      if (a.mark < 0) continue; /* (unprocessed connected components) */
      t = a.type;               /* check the atom type */
      if (mol.coder != null) t = mol.coder.decode(t);
      if ((a.deg == 1)          /* check possible shorthand hydrogen */
      &&  (AtomTypeMgr.getElem(t) == AtomTypeMgr.HYDROGEN)) {
        e = a.edges[0];         /* get the connecting edge */
        d = (e.src != a) ? e.src : e.dst;
        t = d.type;             /* get the destination atom type */
        if (mol.coder != null) t = mol.coder.decode(t);
        if ((BondTypeMgr.getBond(e.type) == BondTypeMgr.SINGLE)
        &&  (AtomTypeMgr.getElem(t)      != AtomTypeMgr.HYDROGEN))
          continue;             /* skip shorthand hydrogens */
      }
      if (n++ > 0)              /* connect components by a null bond */
        this.desc.append(BondTypeMgr.getBondName(BondTypeMgr.NULL));
      Notation.mark(a);         /* mark the visits of each atom, */
      this.out(a);              /* output a connected component, */
      Notation.unmark(a);       /* and clear the atom markers */
    }
    return this.desc.toString();/* return the created description */
  }  /* describe() */

  /*------------------------------------------------------------------*/
  /** Main function for testing basic functionality.
   *  <p>It is tried to parse the first argument as an SLN description
   *  of a molecule. If this is successful, the parsed molecule is
   *  printed using the function <code>describe()</code>.</p>
   *  @param  args the command line arguments
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/
  
  public static void main (String args[])
  {                             /* --- main function for testing */
    if (args.length != 1) {     /* if wrong number of arguments */
      System.err.println("usage: java moss.SLN <SLN string>");
      return;                   /* print a usage message */
    }                           /* and abort the program */
    try {                       /* try to parse the description */
      Notation ntn = new SLN(); /* with an SLN notation */
      Graph    mol = ntn.parse(new StringReader(args[0]));
      System.out.println(ntn.describe(mol)); }
    catch (IOException e) {     /* catch and report parse errors */
      System.err.println(e.getMessage()); }
  }  /* main() */
  
}  /* class SLN */
