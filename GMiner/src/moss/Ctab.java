/*----------------------------------------------------------------------
  File    : Ctab.java
  Contents: class for connection tables (Ctab, Elsevier MDL)
  Authors : Christian Borgelt
  History : 2007.02.21 file created
            2007.03.02 function readGraph() added
            2007.03.04 function isLine() added
            2007.06.21 adapted to type managers (atoms and bonds)
            2007.06.22 rewritten to use readers and writers
            2007.07.02 buffer for creating descriptions added
            2010.07.07 missing marking of aromatic atoms added
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;

/*--------------------------------------------------------------------*/
/** Class for the connection table notation (Ctab, Elsevier MDL).
 *  @author Christian Borgelt
 *  @since  2007.02.21 */
/*--------------------------------------------------------------------*/
public class Ctab extends MoleculeNtn {
  
  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** the decoding map for bond types */
  private static final int[] DECODE_MAP = {
    BondTypeMgr.NULL,   BondTypeMgr.SINGLE,
    BondTypeMgr.DOUBLE, BondTypeMgr.TRIPLE,
    BondTypeMgr.AROMATIC,
    BondTypeMgr.SINGLE, BondTypeMgr.SINGLE,
    BondTypeMgr.DOUBLE, BondTypeMgr.SINGLE };

  /** the encoding map for bond types */
  public static final int[] ENCODE_MAP = {
  /*  0   1   2   3   4   5   6   7  */
      0,  1,  0,  7,  0,  0,  0,  4,
  /*  8   9  10  11  12  13  14  15  */
      0,  0,  0,  0,  0,  0,  0,  2,
  /* 16  17  18  19  20  21  22  23  */
      3,  3,  0,  0,  0,  0,  0,  0,
  /* 24  25  26  27  28  29  30  31  */
      0,  0,  0,  0,  0,  0,  0,  0,
  };

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the buffer for number reading */
  private int[] buf;

  /*------------------------------------------------------------------*/
  /** Create a connection table notation.
   *  @since  2007.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Ctab ()
  {                             /* --- create a Ctab object */
    this.nodemgr = new AtomTypeMgr();  /* create the node and */
    this.edgemgr = new BondTypeMgr();  /* edge type managers */
    this.buf     = new int[16]; /* create a buffer for number reading */
    this.desc    = null;        /* clear the description buffer */
  }  /* Ctab() */

  /*------------------------------------------------------------------*/
  /** Whether this is a line notation (single line description).
   *  @return <code>false</code>, since Ctab is a multi-line notation
   *  @since  2007.03.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isLine ()
  { return false; }

  /*------------------------------------------------------------------*/
  /** Read an integer number.
   *  @param  reader the reader to read from
   *  @return the integer number read
   *  @throws IOException if no integer number could be read
   *  @since  2007.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readInt (Reader reader) throws IOException
  {                             /* --- read an integer number */
    int i, k, c;                /* loop variables, character */
    int n, s = 0;               /* integer value read, sign */

    for (i = 0; i < 3; i++) {   /* an integer number consists */
      this.buf[i] = reader.read();     /* of three characters */
      if (this.buf[i] < 0) throw new IOException("incomplete field");
    }
    while (--i >= 0)            /* trim trailing blanks */
      if (this.buf[i] != ' ') { ++i; break; }
    this.buf[i] = 0;            /* place sentinel and */
    while (this.buf[0] == ' '){ /* trim leading blanks */
      if (--i <= 0) throw new IOException("missing number");
      for (k = 0; k < i; k++) this.buf[k] = this.buf[k+1];
    }
    if (this.buf[0] == '-') {   /* process a possible sign */
      if (--i <= 0) throw new IOException("isolated minus sign");
      for (k = 0; k < i; k++) this.buf[k] = this.buf[k+1];
      s = -1;                   /* note the sign and */
    }                           /* remove the character */
    for (k = n = 0; k < i; k++) {
      c = this.buf[k];          /* traverse remaining characters */
      if ((c < '0') || (c > '9'))
       throw new IOException("invalid character '"+(char)c+"' ("+c+")");
      n = n *10 +(c -'0');      /* catch non-digits and combine */
    }                           /* digits into a number */
    return (s < 0) ? -n : n;    /* return the parsed number */
  }  /* readInt() */

  /*------------------------------------------------------------------*/
  /** Get a real number from a string.
   *  @param  reader the reader to read from
   *  @return the real number read
   *  @throws IOException if no real number could be read
   *  @since  2007.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  @SuppressWarnings("unused")
  private float readReal (Reader reader) throws IOException
  {                             /* --- read a real number */
    int   i, k, c;              /* loop variables, character */
    int   n, s = 0;             /* integer part and sign */
    float f;                    /* fractional part */

    for (i = 0; i < 5; i++) {   /* there are five digits before */
      this.buf[i] = reader.read();         /* the decimal point */
      if (this.buf[i] < 0) throw new IOException("incomplete field");
    }
    this.buf[i] = 0;            /* place sentinel and */
    while (this.buf[0] == ' '){ /* trim leading blanks */
      if (--i <= 0) throw new IOException("missing/malformed number");
      for (k = 0; k < i; k++) this.buf[k] = this.buf[k+1];
    }
    if (this.buf[0] == '-') {   /* process a possible sign */
      if (--i <= 0) throw new IOException("isolated minus sign");
      for (k = 0; k < i; k++) this.buf[k] = this.buf[k+1];
      s = -1;                   /* note the sign and */
    }                           /* remove the character */
    for (k = n = 0; k < i; k++) {
      c = this.buf[k];          /* traverse remaining characters */
      if ((c < '0') || (k > '9'))
       throw new IOException("invalid character '"+(char)c+"' ("+c+")");
      n = n *10 +(c -'0');      /* catch non-digits and combine */
    }                           /* digits into a number */
    if (reader.read() != '.')   /* check for a decimal point */
      throw new IOException("missing decimal point '.'");
    for (i = 0; i < 4; i++) {   /* there are four digits after */
      this.buf[i] = reader.read();        /* the decimal point */
      if (this.buf[i] < 0) throw new IOException("incomplete field");
    }
    while (--i >= 0)            /* trim trailing blanks */
      if (this.buf[i] != ' ') { ++i; break; }
    if (i <= 0) return n;       /* if there is no fraction, abort */
    for (f = 0, k = i; --k >= 0; ) {
      c = this.buf[k];          /* traverse remaining characters */
      if ((c < '0') || (c > '9'))
       throw new IOException("invalid character '"+(char)c+"' ("+c+")");
      f = f *0.1F +(c -'0');    /* catch non-digits and combine */
    }                           /* digits into a number */
    f += n;                     /* add integer part to fraction */
    return (s < 0) ? -f : f;    /* return the parsed number */
  }  /* readReal() */

  /*------------------------------------------------------------------*/
  /** Read a chemical element.
   *  @param  reader the reader to read from
   *  @return the chemical element read
   *  @throws IOException if no integer number could be read
   *  @since  2007.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readElem (Reader reader) throws IOException
  {                             /* --- read a chemical element */
    int i, k;                   /* loop variables */
    int c, n;                   /* current and next character */

    for (i = 0; i < 3; i++) {   /* an element name consists */
      this.buf[i] = reader.read();   /* of three characters */
      if (this.buf[i] < 0) throw new IOException("incomplete element");
    }
    while (--i >= 0)            /* trim trailing blanks */
      if (this.buf[i] != ' ') { ++i; break; }
    this.buf[i] = 0;            /* place sentinel and */
    while (this.buf[0] == ' ')  /* trim leading blanks */
      for (--i, k = 0; k < i; k++) this.buf[k] = this.buf[k+1];
    if (i <= 0) throw new IOException("missing element");
    c = this.buf[0];            /* get the first character */
    n = this.buf[1];            /* and a possible second one */
    if (i >  2) throw new IOException("invalid element1 '" +(char)c
                                      +(char)n +(char)this.buf[2] +"'");
    if ((c < 'A') || (c > 'Z')) /* (must be an uppercase letter) */
      throw new IOException("invalid element2 '" +(char)c
                            +((i > 1) ? "" +(char)n : "") +"'");
    
    if (i <= 1) {               /* if there is only one character, */
      k = AtomTypeMgr.map[c-'A'][26];
      if (k <= 0) throw new IOException("invalid element3 '"
                                        +(char)c +"'"); }
    else {                      /* otherwise check second character */
      k = ((n >= 'a') && (n <= 'z'))
        ? AtomTypeMgr.map[c-'A'][n-'a'] : -1;
      if (k <= 0) throw new IOException("invalid element4 '"
                                        +(char)c +(char)n +"'");
    }                           /* get the element code */
    return k;                   /* return the element code */
  }  /* readElem() */

  /*------------------------------------------------------------------*/
  /** Parse the description of a molecule.
   *  @param  reader the reader to read from
   *  @return the parsed molecule
   *  @throws IOException if a parse error or an i/o error occurs
   *  @since  2007.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Graph parse (Reader reader) throws IOException
  {                             /* --- parse a molecule description */
    int   na, nb;               /* numbers of atoms and bonds */
    int   i, k, s, d;           /* loop variables, indices */
    int   t;                    /* atom and bond type */
    Graph mol;                  /* created molecule */

    mol = new Graph(this);      /* create a molecule */
    na  = this.readInt(reader); /* read the number of atoms */
    nb  = this.readInt(reader); /* and  the number of bonds */
    for (i = 0; i < na; i++) {  /* loop to read the atoms */
      do { k = reader.read(); } /* skip the rest of the line */
      while ((k >= 0) && (k != '\n'));
      if ((k <= 0) || (reader.skip(30) != 30))
        throw new IOException("atom line too short");
      t = this.readElem(reader);
      if (reader.skip(3) != 3)  /* skip to charge specification */
        throw new IOException("atom line too short");
      k = this.readInt(reader); /* read and check a possible charge */
      if (k > 7) throw new IOException("invalid charge "  +k);
      if (k > 0) k = 4 -k;      /* compute the actual charge */
      t = mol.addNode(t | AtomTypeMgr.codeCharge(k));
      if (reader.skip(4) != 4)  /* skip to hydrogen counter */
        throw new IOException("atom line too short");
      mol.nodes[t].mark = this.readInt(reader);
    }                           /* note the number of hydrogens */
    for (i = 0; i < nb; i++) {  /* read the bonds */
      do { k = reader.read(); } /* skip the rest of the line */
      while ((k >= 0) && (k != '\n'));
      s = this.readInt(reader); /* read the source      atom index */
      if ((s <= 0) || (s > na)) /* and check it */
        throw new IOException("invalid atom index " +s);
      d = this.readInt(reader); /* read the destination atom index */
      if ((d <= 0) || (d > na)) /* and check it */
        throw new IOException("invalid atom index " +d);
      t = this.readInt(reader); /* read the bond type */
      if ((t <= 0) || (t >= Ctab.DECODE_MAP.length))
        throw new IOException("invalid bond type "  +t);
      t = Ctab.DECODE_MAP[t];   /* get the type of the bond */
      mol.addEdge(s-1, d-1, t); /* add the bond to the molecule */
      if (t == BondTypeMgr.AROMATIC) {
        mol.nodes[s-1].type |= AtomTypeMgr.AROMATIC;
        mol.nodes[d-1].type |= AtomTypeMgr.AROMATIC;
      }                         /* if the bond is aromatic, */
    }                           /* set aromatic flag in atoms */
    t = 0;                      /* clear flag for charge property */
    while (true) {              /* read the properties */
      do { k = reader.read(); } /* skip the rest of the line */
      while ((k >= 0) && (k != '\n'));
      if (k < 0) throw new IOException("missing property block");
      if ((reader.read() != 'M')
      ||  (reader.read() != ' ')
      ||  (reader.read() != ' '))
        throw new IOException("expected line start 'M  '");
      for (i = 0; i < 3; i++) { /* read the property type */
        this.buf[i] = k = reader.read();
        if (k <= 0) throw new IOException("property line too short");
      }                         /* check for three characters */
      if (((this.buf[0] == 'E') || (this.buf[0] == 'e'))
      &&  ((this.buf[1] == 'N') || (this.buf[1] == 'n'))
      &&  ((this.buf[2] == 'D') || (this.buf[2] == 'd')))
        break;                  /* check for end of property block */
      if (((this.buf[0] != 'C') && (this.buf[0] != 'c'))
      ||  ((this.buf[1] != 'H') && (this.buf[1] != 'h'))
      ||  ((this.buf[2] != 'G') && (this.buf[2] != 'g')))
        continue;               /* check for charge property */
      if (t == 0) {             /* if first charge property */
        for (t = 1, i = na; --i >= 0; )
          mol.nodes[i].type &= ~AtomTypeMgr.CHARGEMASK;
      }                         /* remove all existing charges */
      k = this.readInt(reader); /* get the number of entries */
      for (i = 0; i < k; i++) { /* traverse the charges */
        if (reader.read() != ' ')
          throw new IOException("missing separator");
        s = this.readInt(reader);  /* get the atom index */
        if ((s <= 0) || (s > na))  /* and check it */
          throw new IOException("invalid atom index " +s);
        if (reader.read() != ' ')
          throw new IOException("missing separator");
        d = this.readInt(reader);  /* get the charge */
        if ((d < -15) || (d > 15)) /* and check it */
          throw new IOException("invalid charge " +d);
        mol.nodes[s-1].type |= AtomTypeMgr.codeCharge(d);
      }                         /* set the new charges */
    }
    do { k = reader.read(); }   /* skip the rest of the line */
    while ((k >= 0) && (k != '\n'));
    for (i = na; --i >= 0; ) {  /* traverse all nodes */
      k = mol.nodes[i].mark;    /* get the number of hydrogens */
      mol.nodes[i].mark = 0;    /* and clear the node marker */
      while (--k >= 0)          /* add all implicit hydrogens */
        mol.addEdge(i, mol.addNode(AtomTypeMgr.HYDROGEN),
                    BondTypeMgr.SINGLE);
    }
    mol.opt();                  /* optimize memory usage */
    return mol;                 /* return the created molecule */
  }  /* parse() */

  /*------------------------------------------------------------------*/
  /** Create a description of a given molecule.
   *  @param  mol the molecule to describe
   *  @return a description of the given molecule
   *  @since  2007.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String describe (Graph mol)
  {                             /* --- create a string description */
    int    i, k, n, t;          /* loop variables, type buffer */
    Node   s, d;                /* source and destination atom */
    Edge   b;                   /* incident bond */
    String e;                   /* element name, buffer */

		if (this.desc == null) /* create a description buffer */
			this.desc = new StringBuffer();
		this.desc.setLength(0); /* clear the description buffer */
		for (i = mol.edgecnt; --i >= 0;)
			mol.edges[i].mark = 1; /* mark all edges */
		for (i = n = 0; i < mol.nodecnt; i++) {
			s = mol.nodes[i]; /* traverse the nodes */
			if ((s.deg != 1) /* check for non-terminal node */
					|| !BondTypeMgr.isSingle(s.edges[0].type)) {
				s.mark = ++n;
				continue;
			}
			t = (mol.coder != null) ? mol.coder.decode(s.type) : s.type;
			if (AtomTypeMgr.getElem(t) != AtomTypeMgr.HYDROGEN) {
				s.mark = ++n;
				continue;
			} /* check for non-hydrogen */
			b = s.edges[0]; /* get the destination node */
			d = (b.src != s) ? b.src : b.dst;
			t = (mol.coder != null) ? mol.coder.decode(d.type) : d.type;
			if (AtomTypeMgr.getElem(t) == AtomTypeMgr.HYDROGEN) {
				s.mark = ++n;
				continue;
			} /* check for hydrogen pair */
			s.mark = b.mark = 0; /* mark hydrogen and bond as */
		} /* implicit (shorthand hydrogens) */
		for (i = k = 0; i < mol.edgecnt; i++)
			if (mol.edges[i].mark != 0)
				k++;
		e = "  " + n; /* store number of atoms */
		this.desc.append(e.substring(e.length() - 3));
		e = "  " + k; /* store number of bonds */
		this.desc.append(e.substring(e.length() - 3));
		this.desc.append("  0  0  0  0              1 V2000\n");
		for (i = 0; i < mol.nodecnt; i++) {
			s = mol.nodes[i]; /* traverse the nodes again */
			if (s.mark == 0)
				continue;/* skip implicit hydrogens */
			this.desc.append("    0.0000"); /* x-coordinate */
			this.desc.append("    0.0000"); /* y-coordinate */
			this.desc.append("    0.0000 "); /* z-coordinate */
			t = (mol.coder != null) ? mol.coder.decode(s.type) : s.type;
			e = AtomTypeMgr.getElemName(t); /* get the element name */
			this.desc.append(e); /* and store it */
			this.desc.append("   ".substring(e.length()));
			this.desc.append(" 0  "); /* mass difference (dummy) */
			k = AtomTypeMgr.getCharge(t); /* get the charge */
			k = ((k < -3) || (k == 0) || (k > 3)) ? 0 : 4 - k;
			this.desc.append(k); /* store the charge and */
			this.desc.append("  0  ");/* stereo parity (dummy) */
			k = MoleculeNtn.getHydros(s, mol.coder);
			k = (k <= 0) ? 0 : ((k <= 4) ? k + 1 : 5);
			this.desc.append(k); /* store the number of hydrogens */
			this.desc.append("  0  0\n");
		} /* store additional dummy fields */
		for (i = 0; i < mol.edgecnt; i++) {
			b = mol.edges[i]; /* traverse the edges */
			if (b.mark != 1)
				continue;/* skip unmarked edges */
			e = "  " + b.src.mark; /* store source node index */
			this.desc.append(e.substring(e.length() - 3));
			e = "  " + b.dst.mark; /* store destination node index */
			this.desc.append(e.substring(e.length() - 3));
			e = "  " + Ctab.ENCODE_MAP[BondTypeMgr.getBond(b.type)];
			this.desc.append(e.substring(e.length() - 3));
			this.desc.append("  0  0  0\n");
		}       /* store bond type and dummy fields */
    this.desc.append("M  END"); /* store end of connection table */
    return this.desc.toString();/* return the created description */
  }  /* describe() */

  /*------------------------------------------------------------------*/
  /** Main function for testing basic functionality.
   *  <p>It is tried to parse the contents of the file given by the
   *  first argument as a connection table (Ctab) description of a
   *  molecule. If this is successful, the parsed molecule is printed
   *  using the function <code>describe()</code>.</p>
   *  @param  args the command line arguments
   *  @since  2007.02.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/
  
  public static void main (String args[])
  {                             /* --- main function for testing */
    if (args.length != 1) {     /* if wrong number of arguments */
      System.err.println("usage: java moss.Ctab <Ctab file>");
      return;                   /* print a usage message */
    }                           /* and abort the program */
    try {                       /* try to parse the description */
      Notation ntn    = new Ctab();    /* as a connection table */
      Reader   reader = new FileReader(args[0]);
      Graph    mol    = ntn.parse(reader); reader.close();
      System.out.println(ntn.describe(mol)); }
    catch (IOException e) {     /* catch and report parse errors */
      System.err.println(e.getMessage()); }
  }  /* main() */
  
}  /* class Ctab */
