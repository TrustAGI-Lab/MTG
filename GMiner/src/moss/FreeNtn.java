/*----------------------------------------------------------------------
  File    : FreeNtn.java
  Contents: abstract class for free graph notations
  Author  : Christian Borgelt
  History : 2007.06.22 file created
            2008.08.13 type managers moved to Notation
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for free graph notations (with dynamic type managers).
 *  @author Christian Borgelt
 *  @since  2007.06.22 */
/*--------------------------------------------------------------------*/
public abstract class FreeNtn extends Notation {

  /*------------------------------------------------------------------*/
  /** Whether this notation has a fixed set of (node and edge) types.
   *  @return <code>false</code>, because any type managers can be used
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean hasFixedTypes ()
  { return false; }

  /*------------------------------------------------------------------*/
  /** Get the node type manager.
   *  @return the node type manager
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public TypeMgr getNodeMgr ()
  { return this.nodemgr; }

  /*------------------------------------------------------------------*/
  /** Set the node type manager.
   *  @param  nodemgr the new node type manager
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setNodeMgr (TypeMgr nodemgr)
  { this.nodemgr = nodemgr; }

  /*------------------------------------------------------------------*/
  /** Get the edge type manager.
   *  @return the edge type manager
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public TypeMgr getEdgeMgr ()
  { return this.edgemgr; }

  /*------------------------------------------------------------------*/
  /** Set the edge type manager.
   *  @param  edgemgr the new edge type manager
   *  @since  2007.06.29 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void setEdgeMgr (TypeMgr edgemgr)
  { this.edgemgr = edgemgr; }

}  /* class FreeNtn */
