/*----------------------------------------------------------------------
  File    : TypeMgr.java
  Contents: class for a node or edge type manager
  Author  : Christian Borgelt
  History : 2007.06.20 file created
            2009.08.13 general constants for type handling added
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for a node or edge type manager.
 *  <p>A node or edge type manager manages integer numbers that are
 *  used to encode the type/attribute/label of a node or an edge.
 *  Such a type consists of a base type (30 bits) and flags (2 bits)
 *  that indicate a wildcard type and a special type. For wildcards
 *  and specials the base type may still be used to distinguish the
 *  actual types (e.g. distinguish different types of wildcards).</p>
 *  @author Christian Borgelt
 *  @since  2007.06.20 */
/*--------------------------------------------------------------------*/
public abstract class TypeMgr {

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** the mask for the base type */
  public static final int BASEMASK = Integer.MAX_VALUE >> 1;
  /** the mask for the type flags */
  public static final int FLAGMASK = ~BASEMASK;
  /** the flag for a wildcard type */
  public static final int WILDCARD = Integer.MIN_VALUE;
  /** the flag for a special type */
  public static final int SPECIAL  = Integer.MAX_VALUE & FLAGMASK;

  /*------------------------------------------------------------------*/
  /** Check whether a type manager is fixed (is not extendable).
   *  @return whether the type manager is fixed
   *  @since  2009.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract boolean isFixed ();

  /*------------------------------------------------------------------*/
  /** Add a type to the type manager.
   *  <p>If the name is already present, no new mapping is added,
   *  but the code already associated with the name is returned,
   *  thus automatically avoiding duplicate entries.</p>
   *  <p>If the type manager does not allow for adding types and
   *  the name is not present, this function should return
   *  <code>-1</code>.</p>
   *  @param  name the name of the type
   *  @return the code of the type or <code>-1</code> if the name does
   *          not exist in this type manager and adding is not possible
   *  @see    #isFixed()
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract int add (String name);

  /*------------------------------------------------------------------*/
  /** Map a type name to the corresponding type code.
   *  @param  name the name of the type
   *  @return the code of the type
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract int getCode (String name);

  /*------------------------------------------------------------------*/
  /** Map a type code to the corresponding type name.
   *  @param  code the code of the type
   *  @return the name of the type
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public abstract String getName (int code);

  /*------------------------------------------------------------------*/
  /** Get the base type (remove flags).
   *  @param  code the code from which to get the base type
   *  @return the base type specified by the code
   *  @since  2008.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int getBase (int code)
  { return code & BASEMASK; }

  /*------------------------------------------------------------------*/
  /** Check whether a type code specifies a wildcard.
   *  @param  code the code of the type to check
   *  @return whether the code specifies a wildcard
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static boolean isWildcard (int code)
  { return (code & WILDCARD) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether a type code specifies a special type.
   *  @param  code the code of the type to check
   *  @return whether the code specifies a special type
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static boolean isSpecial (int code)
  { return (code & SPECIAL) != 0; }

}  /* class TypeMgr */
