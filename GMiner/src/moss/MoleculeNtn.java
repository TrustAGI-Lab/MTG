/*----------------------------------------------------------------------
  File    : MoleculeNtn.java
  Contents: abstract class for molecule notations
  Author  : Christian Borgelt
  History : 2006.10.21 file created from file Notation, Atom, and Bond
            2006.10.23 treatment of connected aromatic rings corrected
            2006.11.05 map from element names to element codes added
            2006.11.12 shorthand hydrogen reading and writing added
            2006.11.17 function aromatize moved here (from Graph.java)
            2007.06.21 adapted to type managers (atoms and bonds)
            2007.06.22 rewritten to use readers and writers
            2007.06.23 read function with push back option added
            2007.06.26 function getDelim() added (delimiter character)
            2007.07.02 buffer for creating descriptions added
            2007.11.13 charged hydrogens excluded in getHydros()
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.Writer;

/*--------------------------------------------------------------------*/
/** Class for (linear) notations for molecules.
 *  <p>A molecule is represented as a graph with atoms as nodes
 *  and bonds as edges.</p>
 *  <p>General grammar for (linear) molecule descriptions:</p>
 *  <p><pre>
 *  Molecule ::= Atom Branch
 *  Branch   ::= \epsilon
 *             | Bond Atom  Branch
 *             | Bond Label Branch
 *             | "(" Branch ")" Branch
 *  Atom     ::= Element LabelDef
 *  LabelDef ::= \epsilon
 *             | Label LabelDef</pre>
 *  <p>The definitions of the terms "Element", "Bond", and "Label"
 *  depend on the chosen description language. For the SMILES
 *  language it is:</p>
 *  <p><pre>
 *  Element  ::= "[H]" | "[He]" | "[Li]" | "[Be]" | ...
 *             | "B" | "C" | "N" | "O" | "F" | ...
 *  Bond     ::= \epsilon | "-" | "=" | "#" | ":"
 *  Label    ::= [0-9] | "%" [0-9] [0-9]</pre>
 *  @author Christian Borgelt
 *  @since  2006.08.12 */
/*--------------------------------------------------------------------*/
public abstract class MoleculeNtn extends Notation {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the buffer for creating molecule descriptions */
  protected StringBuffer desc;

  /*------------------------------------------------------------------*/
  /** Whether this notation has a fixed set of (node and edge) types.
   *  @return <code>true</code>, because types are atoms and bonds
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean hasFixedTypes ()
  { return true; }

  /*------------------------------------------------------------------*/
  /** Set the node type manager.
   *  <p>This function has no effect, because node types are fixed.</p>
   *  @param  nodemgr the new node type manager
   *  @see    #hasFixedTypes()
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setNodeMgr (TypeMgr nodemgr)
  { }

  /*------------------------------------------------------------------*/
  /** Set the edge type manager.
   *  <p>This function has no effect, because edge types are fixed.</p>
   *  @param  edgemgr the new edge type manager
   *  @see    #hasFixedTypes()
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setEdgeMgr (TypeMgr edgemgr)
  { }

  /*------------------------------------------------------------------*/
  /** Read shorthand hydrogen atoms.
   *  @return the number of shorthand hydrogen atoms
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected int getHydros () throws IOException
  {                             /* --- read shorthand hydrogens */
    int c = this.read();        /* read the next character */
    if (c < 0) return 0;        /* if there is none, abort */
    if (c != 'H') {             /* check for shorthand hydrogens */
      this.unread(c); return 0; }
    c = this.read();            /* read the next character */
    if (c < 0) return 1;        /* if there is none, there is one H */
    if ((c >= 'a')              /* check for non-hydrogen, e.g. Hg, */
    &&  (c <= 'z')) {           /* and unread the chars. if needed */
      this.unread(c); this.unread('H'); return 0; }
    if ((c <= '0')              /* check for a hydrogen counter */
    ||  (c >  '9')) {           /* if there is none, there is one H */
      this.unread(c); return 1; }
    return c -'0';              /* return the number of hydrogens */
  }  /* getHydros() */

  /*------------------------------------------------------------------*/
  /** Get the number of adjacent hydrogen atoms (and mark them).
   *  @param  atom  the atom for which to get the adjacent hydrogens
   *  @param  coder the recoder for the atom types (if any)
   *  @return the number of shorthand hydrogen atoms
   *  @since  2006.08.12 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int getHydros (Node atom, Recoder coder)
  {                             /* --- get (and mark) hydrogens */
    int  i, t, n = 0;           /* loop variable, buffer, counter */
    Edge b;                     /* to traverse the incident bonds */
    Node d;                     /* to traverse the destination atoms */

    for (i = atom.deg; --i >= 0; ) {
      b = atom.edges[i];        /* traverse the incident edges */
      if ((b.mark == 0) || !BondTypeMgr.isSingle(b.type))
        continue;               /* skip processed and multiple bonds */
      d = (b.src != atom) ? b.src : b.dst;
      if (d.deg != 1) continue; /* check for a leaf node */
      t = (coder != null) ? coder.decode(d.type) : d.type;
      if ((AtomTypeMgr.getElem(t)   != AtomTypeMgr.HYDROGEN)
      ||  (AtomTypeMgr.getCharge(t) != 0))
        continue;               /* skip non-hydrogens/charged hydros. */
      b.mark = 0; d.mark = -1;  /* mark bond and atom as processed */
      n++;                      /* count the hydrogen */
    }
    return (n > 9) ? 9 : n;     /* return the number of hydrogens */
  }  /* getHydros() */

  /*------------------------------------------------------------------*/
  /** Write a description of a molecule.
   *  @param  graph  the molecule to write
   *  @param  writer the writer to write to
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void write (Graph graph, Writer writer) throws IOException
  { writer.write(this.describe(graph)); }

}  /* class MoleculeNtn */
