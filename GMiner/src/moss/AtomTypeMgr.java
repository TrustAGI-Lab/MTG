/*----------------------------------------------------------------------
  File    : AtomTypeMgr.java
  Contents: class for an atom type manager
  Author  : Christian Borgelt
  History : 2007.06.20 file created (parts transferred from Node.java)
            2007.06.21 functions required by class TypeMgr added
            2007.06.22 special name and hydrogen functions added
            2007.06.25 function getCode() improved
            2009.08.13 renamed, some convenience functions added
----------------------------------------------------------------------*/
package moss;

/*--------------------------------------------------------------------*/
/** Class for an atom type manager.
 *  @author Christian Borgelt
 *  @since  2007.06.20 */
/*--------------------------------------------------------------------*/
public class AtomTypeMgr extends TypeMgr {

  /*------------------------------------------------------------------*/
  /*  constants: element types                                        */
  /*------------------------------------------------------------------*/
  /** atom type: unknown */
  public static final int UNKNOWN       =  -1;
  public static final int NULL          =   0;
  public static final int HYDROGEN      =   1;  /* H  */
  public static final int HELIUM        =   2;  /* He */
  public static final int LITHIUM       =   3;  /* Li */
  public static final int BERYLLIUM     =   4;  /* Be */
  public static final int BORON         =   5;  /* B  */
  public static final int CARBON        =   6;  /* C  */
  public static final int NITROGEN      =   7;  /* N  */
  public static final int OXYGEN        =   8;  /* O  */
  public static final int FLOURINE      =   9;  /* F  */
  public static final int NEON          =  10;  /* Ne */
  public static final int SODIUM        =  11;  /* Na */
  public static final int MAGNESIUM     =  12;  /* Mg */
  public static final int ALUMINUM      =  13;  /* Al */
  public static final int SILICON       =  14;  /* Si */
  public static final int PHOSPHORUS    =  15;  /* P  */
  public static final int SULFUR        =  16;  /* S  */
  public static final int CHLORINE      =  17;  /* Cl */
  public static final int ARGON         =  18;  /* Ar */
  public static final int POTASSIUM     =  19;  /* K  */
  public static final int CALCIUM       =  20;  /* Ca */
  public static final int SCANDIUM      =  21;  /* Sc */
  public static final int TITANIUM      =  22;  /* Ti */
  public static final int VANADIUM      =  23;  /* V  */
  public static final int CHROMIUM      =  24;  /* Cr */
  public static final int MANGANESE     =  25;  /* Mn */
  public static final int IRON          =  26;  /* Fe */
  public static final int COBALT        =  27;  /* Co */
  public static final int NICKEL        =  28;  /* Ni */
  public static final int COPPER        =  29;  /* Cu */
  public static final int ZINC          =  30;  /* Zn */
  public static final int GALLIUM       =  31;  /* Ga */
  public static final int GERMANIUM     =  32;  /* Ge */
  public static final int ARSENIC       =  33;  /* As */
  public static final int SELENIUM      =  34;  /* Se */
  public static final int BROMINE       =  35;  /* Br */
  public static final int KRYPTON       =  36;  /* Kr */
  public static final int RUBIDIUM      =  37;  /* Rb */
  public static final int STRONTIUM     =  38;  /* Sr */
  public static final int YTTRIUM       =  39;  /* Y  */
  public static final int ZIRCONIUM     =  40;  /* Zr */
  public static final int NIOBIUM       =  41;  /* Nb */
  public static final int MOLYBDENUM    =  42;  /* Mo */
  public static final int TECHNETIUM    =  43;  /* Tc */
  public static final int RUTHENIUM     =  44;  /* Ru */
  public static final int RHODIUM       =  45;  /* Rh */
  public static final int PALLADIUM     =  46;  /* Pd */
  public static final int SILVER        =  47;  /* Ag */
  public static final int CADMIUM       =  48;  /* Cd */
  public static final int INDIUM        =  49;  /* In */
  public static final int TIN           =  50;  /* Sn */
  public static final int ANTIMONY      =  51;  /* Sb */
  public static final int TELLURIUM     =  52;  /* Te */
  public static final int IODINE        =  53;  /* I  */
  public static final int XENON         =  54;  /* Xe */
  public static final int CESIUM        =  55;  /* Cs */
  public static final int BARIUM        =  56;  /* Ba */
  public static final int LANTHANUM     =  57;  /* La */
  public static final int CERIUM        =  58;  /* Ce */
  public static final int PRASEODYMIUM  =  59;  /* Pr */
  public static final int NEODYMIUM     =  60;  /* Nd */
  public static final int PROMETHIUM    =  61;  /* Pm */
  public static final int SAMARIUM      =  62;  /* Sm */
  public static final int EUROPIUM      =  63;  /* Eu */
  public static final int GADOLINIUM    =  64;  /* Gd */
  public static final int TERBIUM       =  65;  /* Tb */
  public static final int DYSPROSIUM    =  66;  /* Dy */
  public static final int HOLMIUM       =  67;  /* Ho */
  public static final int ERBIUM        =  68;  /* Er */
  public static final int THULIUM       =  69;  /* Tm */
  public static final int YTTERBIUM     =  70;  /* Yb */
  public static final int LUTETIUM      =  71;  /* Lu */
  public static final int HAFNIUM       =  72;  /* Hf */
  public static final int TANTALUM      =  73;  /* Ta */
  public static final int TUNGSTEN      =  74;  /* W  */
  public static final int RHENIUM       =  75;  /* Re */
  public static final int OSMIUM        =  76;  /* Os */
  public static final int IRIDIUM       =  77;  /* Ir */
  public static final int PLATINUM      =  78;  /* Pt */
  public static final int GOLD          =  79;  /* Au */
  public static final int MERCURY       =  80;  /* Hg */
  public static final int THALLIUM      =  81;  /* Tl */
  public static final int LEAD          =  82;  /* Pb */
  public static final int BISMUTH       =  83;  /* Bi */
  public static final int POLONIUM      =  84;  /* Po */
  public static final int ASTATINE      =  85;  /* At */
  public static final int RADON         =  86;  /* Rn */
  public static final int FRANCIUM      =  87;  /* Fr */
  public static final int RADIUM        =  88;  /* Ra */
  public static final int ACTINIUM      =  89;  /* Ac */
  public static final int THORIUM       =  90;  /* Th */
  public static final int PROTACTINIUM  =  91;  /* Pa */
  public static final int URANIUM       =  92;  /* U  */
  public static final int NEPTUNIUM     =  93;  /* Np */
  public static final int PLUTONIUM     =  94;  /* Pu */
  public static final int AMERICUM      =  95;  /* Am */
  public static final int CURIUM        =  96;  /* Cm */
  public static final int BERKELIUM     =  97;  /* Bk */
  public static final int CALIFORNIUM   =  98;  /* Cf */
  public static final int EINSTEINIUM   =  99;  /* Es */
  public static final int FERMIUM       = 100;  /* Fm */
  public static final int MENDELEVIUM   = 101;  /* Md */
  public static final int NOBELIUM      = 102;  /* No */
  public static final int LAWRENCIUM    = 103;  /* Lr */
  public static final int RUTHERFORDIUM = 104;  /* Rf */
  public static final int DUBNIUM       = 105;  /* Db */
  public static final int SEABORGIUM    = 106;  /* Sg */
  public static final int BOHRIUM       = 107;  /* Bh */
  public static final int HASSIUM       = 108;  /* Hs */
  public static final int MEITNERIUM    = 109;  /* Mt */
  public static final int DARMSTADTIUM  = 110;  /* Ds */
  public static final int ROENTGENIUM   = 111;  /* Rg */
  public static final int COPERNICIUM   = 112;  /* Cn */

  /** the number of chemical elements (not all actually exist) */
  public static final int ELEMCNT       = 126;  /* = 0x7e */

  /*------------------------------------------------------------------*/
  /*  constants: flags and masks                                      */
  /*------------------------------------------------------------------*/
  /** the mask for the chemical element of an atom */
  public static final int ELEMMASK    = 0x0000007f;
  /** the flag indicating whether an atom is part of an aromatic ring */
  public static final int AROMATIC    = 0x00000080;
  /** the shift value for extracting the charge from an atom type */
  public static final int CHARGESHIFT = 8;
  /** the mask for extracting the charge from an atom type */
  public static final int CHARGEMASK  = 0x00001f00;
  /** the shift value for extracting the hydrogens from an atom type */
  public static final int HYDROSHIFT  = 16;
  /** the mask for extracting the hydrogens from an atom type */
  public static final int HYDROMASK   = 0x00ff0000;

  /*------------------------------------------------------------------*/
  /*  constants: names                                                */
  /*------------------------------------------------------------------*/
  /** the periodic table of elements; using the chemical element as
   *  an index for this table yields a printable element name */
  protected static final String[] ELEMENTS = {"*",
    "H",                                      "He",
    "Li", "Be", "B",  "C",  "N",  "O",  "F",  "Ne",
    "Na", "Mg", "Al", "Si", "P",  "S",  "Cl", "Ar",
    "K",  "Ca",
      "Sc", "Ti", "V",  "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn",
                "Ga", "Ge", "As", "Se", "Br", "Kr",
    "Rb", "Sr",
      "Y",  "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd",
                "In", "Sn", "Sb", "Te", "I",  "Xe",
    "Cs", "Ba",
      "La", "Ce", "Pr", "Nd", "Pm", "Sm", "Eu", "Gd", "Tb", "Dy",
      "Ho", "Er", "Tm", "Yb",
      "Lu", "Hf", "Ta", "W",  "Re", "Os", "Ir", "Pt", "Au", "Hg",
                "Tl", "Pb", "Bi", "Po", "At", "Rn",
    "Fr", "Ra",
      "Ac", "Th", "Pa", "U",  "Np", "Pu", "Am", "Cm", "Bk", "Cf",
      "Es", "Fm", "Md", "No",
      "Lr", "Rf", "Db", "Sg", "Bh", "Hs", "Mt",

     "?", "?", "?", "?", "?", "?", "?", "?", "?", "?",
     "?", "?", "?", "?", "?", "?", "*", "*"
  };
  /* Question marks are used for atom type codes that cannot occur. */
  /* Stars are used for special atom types, like chain or wildcard. */

  /*------------------------------------------------------------------*/
  /*  class variables                                                 */
  /*------------------------------------------------------------------*/
  /** the map from element names (character pairs) to element codes */
  protected static int[][] map;

  /*------------------------------------------------------------------*/
  /** Class initialization.
   *  @since  2006.11.05 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  static {                      /* --- class initialization */
    int    i, k, m;             /* loop variable, indices */
    String name;                /* to traverse the element names */

    AtomTypeMgr.map = new int[26][27];
                                /* map from element names to codes */
    for (i = ELEMCNT; --i > 0; ) {
      name = ELEMENTS[i];       /* traverse the element names */
      k = name.charAt(0);       /* skip special names (no letter) */
      if ((k < 'A') || (k > 'Z')) continue;
      m = (name.length() > 1) ? name.charAt(1) -'a' : 26;
      AtomTypeMgr.map[k-'A'][m] = i;
    }                           /* set up the element name map */
  }  /* <clinit> */

  /*------------------------------------------------------------------*/
  /** Create an atom type manager.
   *  @since  2007.06.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public AtomTypeMgr ()
  { }

  /*------------------------------------------------------------------*/
  /** Check whether a type manager is fixed (is not extendable).
   *  @return <code>true</code>, because the atom types are fixed
   *  @since  2009.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public boolean isFixed ()
  { return true; }

  /*------------------------------------------------------------------*/
  /** Add an atom type.
   *  <p>The set of atom types is fixed and cannot be extended.
   *  Therefore this function behaves exactly like the function
   *  <code>getCode()</code> and returns -1 for an unknown name.</p>
   *  @param  name the name of the atom
   *  @return the code of the atom or
   *          -1 if the name is not an atom description
   *  @see    #getCode(String)
   *  @since  2007.06.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int add (String name)
  { return this.getCode(name); }

  /*------------------------------------------------------------------*/
  /** Map an atom name to the corresponding code.
   *  @param  name the name of the atom
   *  @return the code of the atom or
   *          -1 if the name is not an atom description
   *  @since  2007.06.20 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public int getCode (String name)
  {                             /* --- get an atom code */
    int  len, k = 0;            /* number of characters, index */
    char c, e;                  /* buffers for characters */
    int  code;                  /* code of the atom */

    len = name.length();        /* get the number of characters */
    if (len <= 0) return -1;    /* check for at least one character */
    e = name.charAt(k);         /* check for an uppercase letter */
    if ((e < 'A') || (e > 'Z')) return -1;
    c = (++k < len) ? name.charAt(k) : 0;
    if ((c < 'a') || (c > 'z')) /* if single letter element, */
      code = map[e-'A'][26];    /* get the element code */
    else {                      /* if double letter atom */
      code = map[e-'A'][c-'a']; /* get the element code */
      c = (++k < len) ? name.charAt(k) : 0;
    }                           /* skip the second letter */
    if (code <= 0) return -1;   /* check the element code */
    if (k >= len)  return code; /* if at the end of the name, abort */
    if ((c == '+')              /* if a sign follows, */
    ||  (c == '-')) {           /* check for a digit */
      e = (++k < len) ? name.charAt(k++) : '1';
      if (e == c) e = '2';      /* also allow a double sign */
      else if ((e <= '0') || (e > '9')) { e = '1'; --k; }
      code |= AtomTypeMgr.codeCharge((c == '+') ? e -'0' : '0' -e);
      c = (k < len) ? name.charAt(k) : 0;
    }                           /* add a charge to the atom type */
    return (k >= len) ? code : -1;
  }  /* getCode() */            /* check for end of name */

  /*------------------------------------------------------------------*/
  /** Map a code to the corresponding atom name.
   *  @param  code the code of the atom
   *  @return the name of the atom
   *  @since  2007.03.05/2007.06.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public String getName (int code)
  {                             /* --- get an atom name */
    int          c;             /* charge of the atom */
    String       e;             /* element type of the atom */
    StringBuffer s;             /* created string description */

    e = ELEMENTS[AtomTypeMgr.getElem(code)];
    c = AtomTypeMgr.getCharge(code); /* get element and charge */
    if (c == 0) return e;       /* if pure element type, abort */
    s = new StringBuffer(e);    /* otherwise add the charge */
    if      (c > 0) { s.append("+"); if (c > +1) s.append(+c); }
    else if (c < 0) { s.append("-"); if (c < -1) s.append(-c); }
    return s.toString();        /* return the created description */
  }  /* getName() */

  /*------------------------------------------------------------------*/
  /** Get the name of a chain atom.
   *  @return the name of a chain atom
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static String getChainName ()
  { return ELEMENTS[ELEMCNT]; }

  /*------------------------------------------------------------------*/
  /** Get the name of a wildcard atom.
   *  @return the name of a wildcard atom
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static String getWildcard ()
  { return ELEMENTS[ELEMCNT+1]; }

  /*------------------------------------------------------------------*/
  /** Extract the chemical element from an atom type.
   *  <p>The chemical element is only part of the type of an atom.
   *  The type of an atom also includes an aromatic flag and a
   *  possible charge.</p>
   *  @param  type the type from which to extract the chemical element
   *  @return the chemical element encoded in the given atom type
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int getElem (int type)
  { return type & ELEMMASK; }

  /*------------------------------------------------------------------*/
  /** Extract the name of a chemical element from an atom type.
   *  @param  type the type from which to extract the chemical element
   *  @return the name of the chemical element encoded int the type
   *  @since  2006.10.31 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static String getElemName (int type)
  { return ELEMENTS[type & ELEMMASK]; }

  /*------------------------------------------------------------------*/
  /** Check whether an atom type specifies a chain atom.
   *  @return whether the atom type specifies a chain atom
   *  @since  2009.08.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static boolean isChain (int type)
  { return (type & SPECIAL) != 0; }

  /*------------------------------------------------------------------*/
  /** Check whether an atom type specifies an aromatic atom.
   *  @return whether the atom type is aromatic
   *  @since  2006.10.31 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static boolean isAromatic (int type)
  { return (type & AROMATIC) != 0; }

  /*------------------------------------------------------------------*/
  /** Encode a charge for addition to an atom type.
   *  <p>The result of this function has to be combined with an element
   *  type using a bitwise and.</p>
   *  @param  chg the charge to encode
   *  @return the encoded charge
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int codeCharge (int chg)
  { chg = (chg < 0) ? (-chg & 0x0f) | 0x10 : (chg & 0x0f);
    return (chg << CHARGESHIFT) & CHARGEMASK; }

  /*------------------------------------------------------------------*/
  /** Extract the charge from an atom type.
   *  @param  type the type from which to extract the charge
   *  @return the charge encoded in the given atom type
   *  @since  2002.03.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int getCharge (int type)
  { type = (type & CHARGEMASK) >> CHARGESHIFT;
    return ((type & 0x10) != 0) ? -(type & 0x0f) : (type & 0x0f); }

  /*------------------------------------------------------------------*/
  /** Remove the charge from an atom type.
   *  @param  type the type from which to remove the charge
   *  @return the atom type without any charge
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int removeCharge (int type)
  { return type & ~CHARGEMASK; }

  /*------------------------------------------------------------------*/
  /** Encode hydrogens for addition to an atom type.
   *  <p>The result of this function has to be combined with an element
   *  type using a bitwise and.</p>
   *  @param  n the number of hydrogens to encode
   *  @return the encoded number of hydrogens
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int codeHydros (int n)
  { return (n << HYDROSHIFT) & HYDROMASK; }

  /*------------------------------------------------------------------*/
  /** Extract the hydrogens from an atom type.
   *  @param  type the type from which to extract the hydrogens
   *  @return the number of hydrogens encoded in the given atom type
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int getHydros (int type)
  { return (type & HYDROMASK) >> HYDROSHIFT; }

  /*------------------------------------------------------------------*/
  /** Remove the hydrogens from an atom type.
   *  @param  type the type from which to remove the hydrogens
   *  @return the atom type without any hydrogens
   *  @since  2007.06.22 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int removeHydros (int type)
  { return type & ~HYDROMASK; }

  /*------------------------------------------------------------------*/
  /** Remove the extended information from an atom type.
   *  @param  type the type from which to remove the information
   *  @return the atom type without extended information
   *  @since  2007.11.09 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static int removeExts (int type)
  { return type & ~HYDROMASK; }

  /*------------------------------------------------------------------*/
  /** Main function for testing some basic functionality.
   *  <p>It is tried to parse the first command line argument
   *  as an atom description and the resulting code is reported.</p>
   *  @param  args the command line arguments
   *  @since  2007.06.21 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static void main (String args[])
  {                             /* --- main function for testing */
    if (args.length != 1) {     /* if wrong number of arguments */
      System.err.println("usage: java moss.AtomTypeMgr <string>");
      return;                   /* print a usage message */
    }                           /* and abort the program */
    AtomTypeMgr atm  = new AtomTypeMgr();
    int         code = atm.getCode(args[0]);
    System.out.print("code(\"" +args[0] +"\") = ");
    System.out.println(code);   /* convert from name to code */
    if (code < 0) return;       /* check for a valid code */
    String      name = atm.getName(code);
    System.out.print("name(" +code +") = ");
    System.out.println(name);   /* convert from code to name */
  }  /* main() */

}  /* class AtomTypeMgr */
