/*----------------------------------------------------------------------
  File    : FreeTypeMgr.java
  Contents: class for a free type manager (no predefined types)
  Author  : Christian Borgelt
  History : 2007.06.20 file created
            2009.08.13 adapted to changes of class TypeMgr
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for node or edge types.
 *  @author Christian Borgelt
 *  @since  2007.06.20 */
/*--------------------------------------------------------------------*/
class Type {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the name/string description of the type */
  protected String name;
  /** the code of the type */
  protected int    code;
  /** the hash value of the name */
  protected int    hash;
  /** the successor in a hash bin list */
  protected Type   succ;

  /*------------------------------------------------------------------*/
  /** Create a type object.
   *  @param  name the name/string description of the type
   *  @param  code the code of the type
   *  @param  hash the hash value of the name
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Type (String name, int code, int hash)
  {                             /* --- create a type object */
    this.name = name;           /* note the type name */
    this.code = code;           /* and  the type code */
    this.hash = hash;           /* and  the hash code of the name */
  }  /* Type() */

}  /* class Type */


/*--------------------------------------------------------------------*/
/** Class for a node or edge type map.
 *  @author Christian Borgelt
 *  @since  2007.06.20 */
/*--------------------------------------------------------------------*/
public class FreeTypeMgr extends TypeMgr {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the array of hash bins (name to code map) */
  private Type[] bins;
  /** the inverse map        (code to name map) */
  private Type[] imap;
  /** the current number of types */
  private int    size;

  /*------------------------------------------------------------------*/
  /** Create a type map.
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public FreeTypeMgr ()
  {                             /* --- create a type map */
    this.bins = new Type[255];  /* create a hash table */
    this.imap = new Type[256];  /* and the inverse map */
    this.size = 0;              /* init. the type counter */
  }  /* FreeTypeMgr() */

  /*------------------------------------------------------------------*/
  /** Check whether a type manager is fixed (is not extendable).
   *  @return <code>false</code>, because new types can be added
   *  @since  2009.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isFixed ()
  { return false; }

  /*------------------------------------------------------------------*/
  /** Clear the type map (remove all types).
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void clear ()
  {                             /* --- clear an identifier map */
    for (int i = this.bins.length; --i >= 0; )
      this.bins[i] = null;      /* clear all hash buckets */
    for (int i = this.size; --i >= 0; )
      this.imap[i] = null;      /* clear the inverse map */
    this.size = 0;              /* reinit. the number of types */
  }  /* clear() */

  /*------------------------------------------------------------------*/
  /** Get the current number of types.
   *  @return the current number of types
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int size ()
  { return this.size; }

  /*------------------------------------------------------------------*/
  /** Internal function to reorganize the hash tables.
   *  <p>This function gets called if the load factor of the hash tables
   *  exceeds a threshold, thus indicating that the performance of the
   *  hash tables is about to deteriorate. To counteract this the hash
   *  tables are enlarged, roughly doubling the number of hash bins.</p>
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void rehash ()
  {                             /* --- reorganize the hash table */
    int  i, k;                  /* loop variable, hash bin index */
    Type t;                     /* to traverse the types */

    k = (this.bins.length << 1) +1;
    this.bins = new Type[k];    /* create a new hash table */
    for (i = this.size; --i >= 0; ) {
      t = this.imap[i];         /* traverse the inverse map */
      t.succ = this.bins[k = t.hash % this.bins.length];
      this.bins[k] = t;         /* add the element at the head */
    }                           /* of the appropriate hash bin list */
  }  /* rehash() */

  /*------------------------------------------------------------------*/
  /** Add a type to the type map.
   *  <p>If the name is already present, no new mapping is added,
   *  but the code already associated with the name is returned,
   *  thus automatically avoiding duplicate entries.</p>
   *  @param  name the name of the type
   *  @return the code of the type
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int add (String name)
  {                             /* --- add a type */
    int    hash;                /* hash code of the name */
    int    i;                   /* hash bin index */
    Type[] v;                   /* buffer for reallocation */
    Type   t;                   /* to traverse the hash bin list */

    hash = name.hashCode() & Integer.MAX_VALUE;
    i    = hash % this.bins.length;  /* traverse the hash bin list */
    for (t = this.bins[i]; t != null; t = t.succ)
      if ((hash == t.hash) && name.equals(t.name))
        return t.code;          /* if name exists, return its code */
    t = new Type(name, this.size, hash);
    t.succ = this.bins[i];      /* create a new hash table element */
    this.bins[i] = t;           /* and add it to the hash bin list */
    if (this.size >= this.imap.length) {
      i = this.size +((this.size > 256) ? this.size >> 1 : 256);
      System.arraycopy(this.imap, 0, v = new Type[i], 0, this.size);
      this.imap = v;            /* enlarge the inverse map array */
    }                           /* and copy the existing map types */
    this.imap[t.code] = t;      /* store the new type */
    if (++this.size >= this.bins.length)
      this.rehash();            /* reorganize the hash table */
    return t.code;              /* return assigned identifier */
  }  /* add() */

  /*------------------------------------------------------------------*/
  /** Map a type name to the corresponding type code.
   *  @param  name the name of the type
   *  @return the code of the type or
   *          -1 if the name does not exist in the type map
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getCode (String name)
  {                             /* --- get the code of a type */
    int  hash;                  /* hash value of the name */
    int  i;                     /* hash bin index */
    Type t;                     /* to traverse the hash bin list */

    hash = name.hashCode() & Integer.MAX_VALUE;
    i    = hash % this.bins.length;  /* traverse the hash bin list */
    for (t = this.bins[i]; t != null; t = t.succ)
      if ((hash == t.hash) && name.equals(t.name))
        return t.code;          /* if name exists, return its code */
    return -1;                  /* if name was not found, return -1 */
  }  /* getCode() */

  /*------------------------------------------------------------------*/
  /** Map a type code to the corresponding type name.
   *  @param  code the code of the type
   *  @return the name of the type
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String getName (int code)
  { return this.imap[code].name; }

}  /* class FreeTypeMgr */
