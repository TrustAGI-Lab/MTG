/*----------------------------------------------------------------------
  File    : Edge.java
  Contents: class for edges of attributed (labeled/typed) graphs
  Author  : Christian Borgelt
  History : 2002.03.11 file created (as Bond.java)
            2002.03.28 type, src, and dst changed to protected
            2003.08.03 final modifier removed from type field
            2003.08.04 initialization of Edge.mark and Edge.flags added
            2005.06.07 bridge indicator and test function added
            2005.07.21 bridge flag moved from type to flags field
            2006.05.12 definition of constant RINGEDGE added
            2006.05.17 RINGEDGE and BRIDGE changed to Integer.MIN_VALUE
            2006.06.06 type of flags field changed to long
            2006.10.31 renamed to Edge.java, bond-related stuff removed
            2007.03.24 TYPEMASK reduced to 30 bits (alternative RING)
            2007.06.14 function compareTo() added (edge markers)
            2009.08.06 adapted to java generics (Comparable)
            2009.08.13 constants based on type manager constants
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for edges of an attributed (labeled/typed) graph.
 *  <p>An edge carries information about the nodes it connects, its
 *  type, whether it is part of a ring (for example, an aromatic ring
 *  in a molecule), and whether it is a bridge in graph it is part of.
 *  In addition, for ring edges a bit flag array makes it possible to
 *  easily follow a ring in a molecule.</p>
 *  <p>Note that only 30 bits are actually available for the type of
 *  the edge. The two most significant bits are reserved as flags,
 *  for example, for marking ring bonds.</p>
 *  @author Christian Borgelt
 *  @since  2002.03.11 */
/*--------------------------------------------------------------------*/
public class Edge implements Comparable<Edge> {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** the mask for the base type */
  public static final int  TYPEMASK = TypeMgr.BASEMASK;
  /** the mask for the edge flags (wildcard and ring) */
  public static final int  FLAGMASK = TypeMgr.FLAGMASK;
  /** the flag for a wildcard type */
  public static final int  WILDCARD = TypeMgr.WILDCARD;
  /** the flag for the edge type to distinguish ring edges */
  public static final int  RING     = TypeMgr.SPECIAL;
  /** the mask to extract the bridge flag */
  public static final long BRIDGE   = Long.MIN_VALUE;
  /** the mask to extract the ring flags */
  public static final long RINGMASK = Long.MAX_VALUE;

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the type of the edge, e.g. <code>SINGLE</code> */
  protected int  type;
  /** a marker for internal use (e.g. for a substructure) */
  public int  mark;
  /** the source node of the edge */
  public Node src;
  /** the destination node of the edge */
  public Node dst;
  /** the flags for rings and bridges */
  protected long flags;

  /*------------------------------------------------------------------*/
  /** Create an edge of a given type between two nodes.
   *  <p>The created edge is automatically added to the two nodes
   *  it connects.</p>
   *  @param  src  the source node of the edge
   *  @param  dst  the destination node of the edge
   *  @param  type the type of the edge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Edge (Node src, Node dst, int type)
  {                             /* --- create an edge */
    this.src   = src;           /* note the source node */
    this.dst   = dst;           /* and the destination node */
    this.type  = type;          /* set the edge type and */
    this.flags = 0;             /* clear ring and bridge flags */
    src.addEdge(this);          /* store the new edge */
    dst.addEdge(this);          /* in the connected nodes */
  }  /* Edge() */

  /*------------------------------------------------------------------*/
  /** Create an edge of a given type between two nodes.
   *  <p>The created edge is automatically added to the two nodes
   *  it connects.</p>
   *  @param  src   the source node of the edge
   *  @param  dst   the destination node of the edge
   *  @param  type  the type of the edge
   *  @param  flags the ring and bridge flags of the edge
   *  @since  2009.05.04 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected Edge (Node src, Node dst, int type, long flags)
  {                             /* --- create an edge */
    this.src   = src;           /* note the source node */
    this.dst   = dst;           /* and the destination node */
    this.type  = type;          /* set the edge type and */
    this.flags = flags;         /* the ring and bridge flags */
    src.addEdge(this);          /* store the new edge */
    dst.addEdge(this);          /* in the connected nodes */
  }  /* Edge() */

  /*------------------------------------------------------------------*/
  /** Get the full type (attribute/label) of an edge.
   *  @return the type of the edge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getType ()
  { return this.type; }

  /*------------------------------------------------------------------*/
  /** Get the base type (attribute/label) of an edge.
   *  @return the type of the edge
   *  @since  2009.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getBaseType ()
  { return this.type & TYPEMASK; }

  /*------------------------------------------------------------------*/
  /** Mask the edge type with the given mask.
   *  @param  mask the mask for the edge typ
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void maskType (int mask)
  { this.type &= mask | FLAGMASK; }

  /*------------------------------------------------------------------*/
  /** Check whether this is a special edge (wildcard or ring).
   *  @return whether this is a special edge
   *  @since  2008.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isSpecial ()
  { return (this.type & FLAGMASK) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether this is a wildcard edge.
   *  @return whether this is a wildcard edge
   *  @since  2008.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isWildcard ()
  { return (this.type & WILDCARD) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether the edge is part of a ring.
   *  <p>This function only extracts the ring flag from the edge type.
   *  It does not analyze the graph containing the edge.</p>
   *  @return whether the edge is part of a ring
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isInRing ()
  { return (this.type & RING) != 0; }

  /*------------------------------------------------------------------*/
  /** Set or clear the ring type flag of the edge.
   *  @param  ring whether the edge is in a ring
   *  @since  2007.06.30 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void markRing (boolean ring)
  { if (ring) this.type |=  RING;
    else      this.type &= ~RING; }

  /*------------------------------------------------------------------*/
  /** Get the source node of the edge.
   *  @return the source node of the edge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Node getSource ()
  { return this.src; }

  /*------------------------------------------------------------------*/
  /** Get the destination node of the edge.
   *  @return the destination node of the edge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Node getDest ()
  { return this.dst; }

  /*------------------------------------------------------------------*/
  /** Get the ring flags of the edge.
   *  <p>This function only extracts the ring flags from the flags
   *  field. It does not analyze the graph containing the edge.</p>
   *  @return the ring flags of the edge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public long getRings ()
  { return this.flags & RINGMASK; }

  /*------------------------------------------------------------------*/
  /** Clear the ring flags of the edge.
   *  @since  2007.06.30 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void clearRings ()
  { this.flags &= ~RINGMASK; }

  /*------------------------------------------------------------------*/
  /** Check whether the edge is a bridge.
   *  <p>This function only extracts the bridge flag from the flags
   *  field. It does not analyze the graph containing the edge.</p>
   *  @return whether the edge is a bridge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isBridge ()
  { return (this.flags & BRIDGE) != 0; }

  /*------------------------------------------------------------------*/
  /** Set or clear the bridge flag of the edge.
   *  @param  bridge whether the edge is a bridge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void markBridge (boolean bridge)
  { if (bridge) this.flags |=  BRIDGE;
    else        this.flags &= ~BRIDGE; }

  /*------------------------------------------------------------------*/
  /** Compare two edges (w.r.t. their marker values).
   *  <p>This function is needed in <code>NamedGraph.split()</code>
   *  (indirectly through </code>Arrays.sort()</code>).</p>
   *  @param  obj the edge to compare to
   *  @return the sign of the difference of the edge markers, that is,
   *          <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the marker of this edge is less than, equal to, or
   *          greater than the marker of the edge given as an argument
   *  @since  2007.06.14 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int compareTo (Edge obj)
  {                             /* --- compare two edges */
    if (this.mark < obj.mark) return -1;
    if (this.mark > obj.mark) return +1;
    return 0;                   /* return sign of the difference */
  }  /* compareTo() */          /* of the edge markers */

  /*------------------------------------------------------------------*/
  /** Check whether two edges are equal.
   *  <p>This method checks whether the two edges connect nodes of the
   *  same type with an edge of the same type. If this is the case,
   *  the function returns <code>true</code>, otherwise it returns
   *  <code>false</code>.</p>
   *  @param  edge the edge to compare to
   *  @return whether the two edges are equal
   *  @since  2009.04.30 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean equals (Object edge)
  {                             /* --- check if two edges are equal */
    if (this == edge) return true;
    if (edge == null) return false;
    /* In principle, there should be a check here whether */
    /* the variable 'edge' actually points to an edge.    */
    if  (this.type     != ((Edge)edge).type)      return false;
    
    if ((this.src.getBaseType() == ((Edge)edge).src.getBaseType())
    	    &&  (this.dst.getBaseType()  == ((Edge)edge).dst.getBaseType() )) return true;
    	    if ((this.src.getBaseType()  == ((Edge)edge).dst.getBaseType() )
    	    &&  (this.dst.getBaseType()  == ((Edge)edge).src.getBaseType() )) return true;
    
/*    if ((this.src.type == ((Edge)edge).src.type)
    &&  (this.dst.type == ((Edge)edge).dst.type)) return true;
    if ((this.src.type == ((Edge)edge).dst.type)
    &&  (this.dst.type == ((Edge)edge).src.type)) return true;*/
    return false;               /* compare edge and node types */
  }  /* equals() */
  
  public String toString(){
	return src.type +"\t"+type+"\t"+dst.type;
	  
  }
  
  public int hashCode(){
	  return type*src.type*dst.type;
  }

public int getMark() {
	return mark;
}

public void setMark(int mark) {
	this.mark = mark;
}

}  /* class Edge */
