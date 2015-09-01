/*----------------------------------------------------------------------
  File    : Recoder.java
  Contents: class for a recoder for node (and edge) types
  Author  : Christian Borgelt
  History : 2006.08.10 file created from file Elements.java
            2006.10.25 comments in javadoc style added
            2007.06.22 trimming made more flexible
            2009.08.06 adapted to java generics (Comparable)
            2010.01.21 functions getFreq() and getSupp() added
----------------------------------------------------------------------*/
package moss;

import java.util.Arrays;

/*--------------------------------------------------------------------*/
/** Class for recoder types for nodes.
 *  <p>This class is needed for sorting the types w.r.t. their
 *  frequency in the graph database to process and for recoding the
 *  graphs. It is a helper class for class <code>Recoder</code>.</p>
 *  <p>For each type its frequency (number of nodes in the graph
 *  database with this type) and its support (number of graphs in
 *  the database that contain a node with this type) are recorded.</p>
 *  @author Christian Borgelt
 *  @since  2006.08.10 */
/*--------------------------------------------------------------------*/
class RcType implements Comparable<RcType> {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the value of the type (before encoding, original type) */
  protected int    type;
  /** the code  of the type (after encoding) */
  protected int    code;
  /** the frequency of this type in the graph database */
  protected int    frq;
  /** the support of this type in the graph database */
  protected int    supp;
  /** the index of the last processed graph */
  protected int    idx;
  /** the successor in a hash bin list */
  protected RcType succ;

  /*------------------------------------------------------------------*/
  /** Create a recoder type object.
   *  @param  type the value of the type (old code)
   *  @param  code the code  of the type (new code)
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public RcType (int type, int code)
  {                             /* --- create a recoder type object */
    this.type = type;           /* note the type value */
    this.code = code;           /* and  the type code */
    this.supp = this.frq = 0;   /* initialize the frequencies */
    this.idx  = -1;             /* clear the graph index */
  }  /* RcType() */

  /*------------------------------------------------------------------*/
  /** Compare two types w.r.t. their frequency.
   *  @param  obj the type object to compare to
   *  @return the sign of the frequency difference, that is,
   *  @return <code>-1</code>, <code>0</code>, or <code>+1</code>
   *          as the frequency of this type is less than, equal to,
   *          or greater than the frequency of the given type
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int compareTo (RcType obj)
  {                             /* --- compare two type frequencies */
    if (this.frq < obj.frq) return -1;
    if (this.frq > obj.frq) return +1;
    return 0;                   /* return sign of freq. difference */
  }  /* compareTo() */

}  /* class RcType */


/*--------------------------------------------------------------------*/
/** Class for recoders for node types.
 *  <p>Since it can speed up the mining process for frequent
 *  substructures considerably if the node types are processed in
 *  increasing order of their frequency, it is advisable to recode
 *  the node types to reflect the frequency order.</p>
 *  <p>A recoder is implemented as a hash table (for encoding types)
 *  and an accompanying array (for decoding type codes).</p>
 *  @author Christian Borgelt
 *  @since  2006.08.10 */
/*--------------------------------------------------------------------*/
public class Recoder {

  /*------------------------------------------------------------------*/
  /*  instance variables                                              */
  /*------------------------------------------------------------------*/
  /** the types and their frequencies, sorted by their code */
  private RcType[] types;
  /** the hash table for mapping types to codes (old codes to new) */
  private RcType[] bins;
  /** the current number of types */
  private int      size;
  /** the index of the current graph (for frequency counting) */
  private int      idx;

  /*------------------------------------------------------------------*/
  /** Create a recoder of default size.
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public Recoder ()
  {                             /* --- create a recoder */
    this.types = new RcType[256];  /* create an array for the types */
    this.bins  = new RcType[255];  /* and a hash table */
    this.size  = this.idx = 0;  /* init. counters and graph index */
  }  /* Recoder() */

  /*------------------------------------------------------------------*/
  /** Get the size of the recoder.
   *  <p>The size is the number of stored type/code pairs.</p>
   *  @return the size of the recoder (number of types)
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int size ()
  { return this.size; }

  /*------------------------------------------------------------------*/
  /** Internal function to reorganize the hash table of the recoder.
   *  <p>This function gets called if the load factor of the hash table
   *  exceeds a threshold, thus indicating that the performance of the
   *  hash table is about to deteriorate. To counteract this the hash
   *  table is enlarged, roughly doubling the number of hash bins.</p>
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void rehash ()
  {                             /* --- reorganize the hash table */
    int    i, k;                /* loop variable, hash bin index */
    RcType t;                   /* to traverse the types */

    k = (this.bins.length << 1) +1;
    this.bins = new RcType[k];  /* create a new hash table */
    for (i = this.size; --i >= 0; ) {
      t = this.types[i];        /* traverse the types */
      t.succ = this.bins[k = t.type % this.bins.length];
      this.bins[k] = t;         /* add the type at the head */
    }                           /* of the approriate hash bin list */
  }  /* rehash() */

  /*------------------------------------------------------------------*/
  /** Add a type to the recoder.
   *  <p>The added type is assigned the next code, which is the size
   *  of the recoder before the new type was added. This ensures that
   *  type codes are consecutive integers starting at 0.</p>
   *  @param  type the type to add to the recoder
   *  @return the code that is assigned to the type
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int add (int type)
  {                             /* --- add a type */
    int    i, n;                /* hash bin index, new array size */
    RcType t, v[];              /* new type object, realloc. buffer */

    i = type % this.bins.length;/* try to find the type code */
    for (t = this.bins[i]; t != null; t = t.succ)
      if (t.type == type) return t.code;
    if (this.size >= this.types.length) {
      n = this.size +((this.size > 256) ? this.size >> 1 : 256);
      System.arraycopy(this.types, 0, v = new RcType[n], 0, this.size);
      this.types = v;           /* if the type array is full, */
    }                           /* enlarge it and copy all types */
    this.types[this.size] = t = new RcType(type, this.size);
    t.succ = this.bins[i];      /* create a new type and */
    this.bins[i] = t;           /* add it to the hash table */
    if (++this.size >= this.bins.length)
      this.rehash();            /* reorganize the hash table */
    return t.code;              /* return the assigned code */
  }  /* add() */

  /*------------------------------------------------------------------*/
  /** Encode a type, that is, retrieve its code.
   *  @param  type the type to encode
   *  @return the code of the given type or -1
   *          if the type is not contained in the recoder
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int encode (int type)
  {                             /* --- encode a type value */
    int i = type % this.bins.length;
    for (RcType t = this.bins[i]; t != null; t = t.succ)
      if (t.type == type) return t.code;
    return -1;                  /* find and return the type code */
  }  /* encode() */

  /*------------------------------------------------------------------*/
  /** Decode a type code, that is, retrieve the original type value.
   *  @param  code the type code to decode
   *  @return the original type value associated with the code or
   *          the value of the code itself if the recoder does not
   *          contain a corresponding type.
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int decode (int code)
  {                             /* --- decode a type code */
    if ((code < 0) || (code >= this.size))
      return code;              /* if the code is unknown, return it */
    return this.types[code].type;
  }  /* decode() */             /* otherwise return corresp. value */

  /*------------------------------------------------------------------*/
  /** Count a type code.
   *  <p>Increment the internal counters for the frequency
   *  (and maybe also for the support) of this type code.</p>
   *  @param  code the type code to count
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void count (int code)
  {                             /* --- count occurrence of a type */
    RcType t = this.types[code];/* get the corresponding type object */
    t.frq++;                    /* and increment the counters */
    if (t.idx < this.idx) { t.idx = this.idx; t.supp++; }
  }  /* count() */

  /*------------------------------------------------------------------*/
  /** Commit a type code counting.
   *  <p>This function must be called after each graph for which the
   *  types of its node have been counted, so that the support of a
   *  type (number of graphs that contain a node of a given type) can
   *  be determined.</p>
   *  @see    #count(int)
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void commit ()
  {                             /* --- commit support counting */
    if (++this.idx < Integer.MAX_VALUE)
      return;                   /* if there is no overflow, abort */
    for (int i = this.size; --i >= 0; )
      this.types[i].idx = -1;   /* reinitialize graph indices */
    this.idx = 0;               /* (per element and globally) */
  }  /* commit() */

  /*------------------------------------------------------------------*/
  /** Get the frequency of a type (number of occurrences).
   *  @param  type the type of which to get the frequency
   *  @since  2010.01.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getFreq (int type)
  { return this.types[type].frq; }

  /*------------------------------------------------------------------*/
  /** Get the support of a type (number of containing graphs).
   *  @param  type the type of which to get the support
   *  @since  2010.01.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getSupp (int type)
  { return this.types[type].supp; }

  /*------------------------------------------------------------------*/
  /** Trim the recoder with a minimum support or frequency.
   *  <p>All types having a support or a frequency less than the given
   *  minimum support or frequency are marked as excluded. Note that
   *  the types with a lower support are only marked, not actually
   *  removed from the recoder. Hence it is possible to reactivate
   *  them, for example by calling the function<code>clear()</code>
   *  for such a type.</p>
   *  @param  min the minimum support of a type
   *  @see    #trimFreq(int)
   *  @see    #trimSupp(int)
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void trim (boolean freq, int min)
  { if (freq) this.trimFreq(min); else this.trimSupp(min); }

  /*------------------------------------------------------------------*/
  /** Trim the recoder with a minimum support.
   *  @param  min the minimum support of a type
   *  @see    #trimSupp(int)
   *  @since  2007.06.25 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void trim (int min)
  { this.trimSupp(min); }

  /*------------------------------------------------------------------*/
  /** Trim the recoder with a minimum support.
   *  @param  min the minimum support of a type
   *  @see    #trim(boolean,int)
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void trimSupp (int min)
  {                             /* --- trim types w.r.t. support */ 
    for (int i = this.size; --i >= 0; ) {
      RcType t = this.types[i]; /* traverse all types */
      if (t.supp < min) t.supp = t.frq = -1;
    }                           /* exclude infrequent types */
  }  /* trimSupp() */

  /*------------------------------------------------------------------*/
  /** Trim the recoder with a minimum frequency.
   *  @param  min the minimum frequency of a type
   *  @see    #trim(boolean,int)
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void trimFreq (int min)
  {                             /* --- trim types w.r.t. frequency */ 
    for (int i = this.size; --i >= 0; ) {
      RcType t = this.types[i]; /* traverse all types */
      if (t.frq < min) t.supp = t.frq = -1;
    }                           /* exclude infrequent types */
  }  /* trimFreq() */

  /*------------------------------------------------------------------*/
  /** Clear the frequency and support of a type.
   *  <p>Calling this function also removes a possible marking of the
   *  type as excluded.</p>
   *  @param  code the code of the type for which to clear the counters
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void clear (int code)
  { RcType e = this.types[code]; e.supp = e.frq = 0; }

  /*------------------------------------------------------------------*/
  /** Mark a type as excluded.
   *  <p>Note that marking a type as excluded loses its frequency and
   *  support information. Excluded types will be sorted to the front
   *  with the function <code>sort()</code>, that is, by sorting
   *  excluded types will be assigned the lowest codes.</p>
   *  @param  code the code of the type to mark as excluded
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void exclude (int code)
  { RcType e = this.types[code]; e.supp = e.frq = -1; }

  /*------------------------------------------------------------------*/
  /** Check whether a type is excluded.
   *  <p>Types can be excluded by explicitely calling the function
   *  <code>exclude()</code> or by trimming the recoder with a minimum
   *  frequency (by calling the function <code>trim()</code>).</p>
   *  @param  code the code of the type to check
   *  @see    #exclude(int)
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isExcluded (int code)
  { return this.types[code].supp < 0; }

  /*------------------------------------------------------------------*/
  /** Set frequency and support to a maximal value.
   *  <p>This function indirectly offers the possibility to move a type
   *  to the end of the recoder. Since types are sorted w.r.t. to their
   *  frequency, a type with maximal frequency will end up at the end
   *  of the recoder. This is needed if certain types are to be treated
   *  in a special way, independent of their frequency in the graph
   *  database.</p>
   *  @param  code the code of the type
   *               for which to maximize the frequency
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void maximize (int code)
  { RcType e = this.types[code]; e.supp = e.frq = Integer.MAX_VALUE; }

  /*------------------------------------------------------------------*/
  /** Check whether a code has maximal frequency.
   *  @param  code the code of the type to check
   *  @see    #maximize(int)
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isMaximal (int code)
  { return this.types[code].supp >= Integer.MAX_VALUE; }

  /*------------------------------------------------------------------*/
  /** Sort types w.r.t.&nbsp;their frequency.
   *  <p>The types are sorted ascendingly w.r.t. their frequency,
   *  so that the least frequent type receives the code 0, the next
   *  frequent the code 1 etc. Excluded types precede all non-excluded
   *  types, maximized type succeed all other types.</p>
   *  @since  2006.08.10 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void sort ()
  {                             /* --- sort types w.r.t. frequency */ 
    Arrays.sort(this.types, 0, this.size);
    for (int i = this.size; --i >= 0; )
      this.types[i].code = i;   /* sort and recode the types */
  }  /* sort() */

}  /* class Recoder */
