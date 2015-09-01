/*----------------------------------------------------------------------
  File    : MoSS.java
  Contents: graphical user interface for the MoSS program
  Author  : Christian Borgelt
  History : 2006.07.13 file created from file PGView.java
            2006.07.18 first version completed
            2006.08.09 ignoring the node type only in rings added
            2006.08.12 adapted to new Notation classes
            2006.08.13 node types to exclude as seeds added
            2007.02.15 user interface creation improved, views added
            2007.02.24 configuration file reading simplified
            2007.03.02 SDfile format added for the input
            2007.06.21 adapted to type managers (atoms and bonds)
            2007.07.01 configuration file reading improved
            2007.07.06 adapted to extended support types etc.
            2007.07.07 adapted to new class MoSSPanel
            2007.07.11 auto resize switched off for JTable
            2007.11.05 stack trace added to exception in constructor
            2011.02.24 check box for node orbit filtering added
----------------------------------------------------------------------*/
package moss;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*--------------------------------------------------------------------*/
/** Graphical user interface for the molecular substructure miner.
 *  @author Christian Borgelt
 *  @since  2006.07.13 */
/*--------------------------------------------------------------------*/
public class MoSS extends JFrame
  implements Runnable, ChangeListener, ItemListener {

  private static final long serialVersionUID = 0x00020005;

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** the names of the input/output description formats */
  private static final String[] FORMAT_NAMES = {
    "SMILES", "SLN", "SDfile", "LiNoG", "NEList" };
  /** the names of the seed description formats */
  private static final String[] SEED_NAMES = {
    "SMILES", "SLN", "LiNoG" };
  /** the names of support types */
  private static final String[] SUPP_NAMES = {
    "number of containing graphs/molecules",
    "MIS of overlap graph of embeddings",
    "MIS of harmful overlap graph of embeddings",
    "smallest number of different node images" };
  /** the names of the aromatic bond processing modes */
  private static final String[] AROM_NAMES = {
    "extra type", "downgrade", "upgrade" };
  /** the names of the processing modes for ignoring types */
  private static final String[] TYPE_NAMES = {
    "never", "in rings", "always" };
  /** the names of the perfect extension pruning variants */
  private static final String[] PEP_NAMES = {
    "full", "partial", "none" };
  /** the names of the ring extension/mining variants */
  private static final String[] RING_NAMES = {
    "none", "full", "filter", "merge" };
  /** the names of the extension types */
  private static final String[] EXT_NAMES = {
    "maximum source", "rightmost path" };
  /** the codes of the extension types */
  private static final String[] EXT_CODES = {
    "maxsrc", "rgtpath" };

  /*------------------------------------------------------------------*/
  /*  instance variables: files & format tab                        */
  /*------------------------------------------------------------------*/
  /** the input format for the graphs */
  private JComboBox   infmt;
  /** the output format for the substructures */
  private JComboBox   subfmt;
  /** the seed format */
  private JComboBox   seedfmt;
  /** the name of the input file */
  private JTextField  datname;
  /** the name of the substructure output file */
  private JTextField  subname;
  /** the name of the graph identifier file */
  private JTextField  idsname;
  /** the seed structure to start the search from */
  private JTextField  seed;

  /*------------------------------------------------------------------*/
  /*  instance variables: basic parameters tab                        */
  /*------------------------------------------------------------------*/
  /** the threshold for the split */
  private JTextField  thresh;
  /** whether to invert the split */
  private JCheckBox   invert;
  /** the minimal size of a substructure */
  private JSpinner    minsize;
  /** the maximal size of a substructure */
  private JSpinner    maxsize;
  /** the excluded node types */
  private JTextField  extype;
  /** the node types that are excluded for seeds */
  private JTextField  exseed;

  /*------------------------------------------------------------------*/
  /*  instance variables: support tab                                 */
  /*------------------------------------------------------------------*/
  /** the minimal support in the focus */
  private JTextField  minsupp;
  /** the maximal support in the complement */
  private JTextField  maxsupp;
  /** whether to interpret the support as absolute numbers */
  private JCheckBox   abssupp;
  /** the support type */
  private JComboBox   supptype;
  /** whether to use greedy algorithm for MIS computation */
  private JCheckBox   greedy;
  /** whether to restrict the output to closed substructures */
  private JCheckBox   closed;
  /** the labels that can be disabled in the tab */
  private JLabel[]    splbls;

  /*------------------------------------------------------------------*/
  /*  instance variables: matching tab                                */
  /*------------------------------------------------------------------*/
  /** how to treat aromatic bonds */
  private JComboBox   bdarom;
  /** whether to ignore the bond type */
  private JComboBox   bdtype;
  /** whether to ignore the atom type */
  private JComboBox   attype;
  /** whether to match the atom charge */
  private JCheckBox   charge;
  /** whether to match the atom aromaticity */
  private JCheckBox   atarom;

  /*------------------------------------------------------------------*/
  /*  instance variables: rings & chains tab                          */
  /*------------------------------------------------------------------*/
  /** whether to convert Kekule representations */
  private JCheckBox   kekule;
  /** whether to mark rings */
  private JCheckBox   rings;
  /** the minimal size of a ring */
  private JSpinner    minring;
  /** the maximal size of a ring */
  private JSpinner    maxring;
  /** the type of ring extensions */
  private JComboBox   ringext;
  /** whether to find variable length carbon chains */
  private JCheckBox   chains;
  /** the labels that can be disabled in the tab */
  private JLabel[]    rclbls;

  /*------------------------------------------------------------------*/
  /*  instance variables: search & pruning tab                        */
  /*------------------------------------------------------------------*/
  /** the extension type */
  private JComboBox    exttype;
  /** whether to do perfect extension pruning */
  private JComboBox    perfect;
  /** whether to do equivalent sibling pruning */
  private JCheckBox    equiv;
  /** whether to do canonical form pruning */
  private JCheckBox    canonic;
  /** whether to do filter with node orbits */
  private JCheckBox    orbits;

  /*------------------------------------------------------------------*/
  /*  instance variables: embeddings tab                              */
  /*------------------------------------------------------------------*/
  /** the level from which to use embeddings */
  private JSpinner     level;
  /** the maximal number of embeddings per graph */
  private JSpinner     maxepg;
  /** whether to unembed siblings of the current search tree node */
  private JCheckBox    unembed;

  /*------------------------------------------------------------------*/
  /*  instance variables: debugging tab                               */
  /*------------------------------------------------------------------*/
  /** whether to normalize the output */
  private JCheckBox    normal;
  /** whether to do verbose message output */
  private JCheckBox    verbose;

  /*------------------------------------------------------------------*/
  /*  instance variables: window and status line                      */
  /*------------------------------------------------------------------*/
  /** the pane for the parameter tabs */
  private JTabbedPane  pane;
  /** the execute/abort button */
  private JButton      exec;
  /** the status line */
  private JTextField   stat;

  /*------------------------------------------------------------------*/
  /*  other instance variables                                        */
  /*------------------------------------------------------------------*/
  /** the owner of this dialog box */
  private Component    owner   = null;
  /** whether started as a stand-alone program */
  private boolean      isprog  = false;
  /** the file chooser for selecting the input and output files  */
  private JFileChooser chooser = null;
  /** the substructure miner thread */
  private Thread       thread  = null;
  /** the timer for updating the progress information */
  private Timer        timer   = null;
  /** whether the miner is currently running */
  private boolean      running = false;
  /** whether the miner has been aborted */
  private boolean      aborted = true;
  /** the current substructure miner */
  private Miner        miner   = null;
  /** the start time for the miner thread */
  private long         start   = 0;
  /** the buffer for reading the configuration file */
  private StringBuffer buf     = null;

  /*------------------------------------------------------------------*/
  /** Create a user interface dialog box.
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MoSS ()
  { this(null, true); }

  /*------------------------------------------------------------------*/
  /** Create a user interface for the molecular substructure miner.
   *  @param  owner the owner of the window to create
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MoSS (Component owner)
  { this(owner, false); }

  /*------------------------------------------------------------------*/
  /** Create a user interface for the molecular substructure miner.
   *  @param  owner  the owner of the window to create
   *  @param  isProg whether started as a stand-alone program
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public MoSS (Component owner, boolean isProg)
  {                             /* --- create user interface */
    this.owner  = owner;        /* note the owner and */
    this.isprog = isProg;       /* the program flag */
    if (EventQueue.isDispatchThread()) { this.run(); return; }
    try { EventQueue.invokeAndWait(this); }
    catch (Exception e) { e.printStackTrace(System.err); }
  }  /* MoSS() */               /* create the user interface */

  /*------------------------------------------------------------------*/
  /** Create the user interface dialog box.
   *  <p>Following the recommendations in the Java tutorial, the
   *  user interface is created in the "run" method, which is invoked
   *  from the event queue, in order to avoid problems with threads.</p>
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void run ()
  {                             /* --- create toolbox dialog */
    Container content;          /* content pane of the frame */
    MoSSPanel tab;              /* current tab of the tabbed pane */
    JTextArea about;            /* title of the about tab */
    JButton   button;           /* button for the button bar */
    JPanel    bbar;             /* panel for the button bar */
    JPanel    bottom;           /* panel for the bottom */

    this.pane = new JTabbedPane(SwingConstants.LEFT,
                                JTabbedPane.SCROLL_TAB_LAYOUT);

    /* --- files & formats --- */
    this.pane.addTab("Files & Formats", tab = new MoSSPanel());

    tab.addLabel("Input data file:");
    tab.addButton("Select", MoSSPanel.MIDDLE).addActionListener(
      new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.getFileName("Input Data", MoSS.this.datname); } } );
    tab.addButton("View", MoSSPanel.RIGHT).addActionListener(
      new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.showTable(MoSS.this.datname); } } );
    this.datname = tab.addFileInput("moss.dat");

    tab.addLabel("Substructure output file:");
    tab.addButton("Select", MoSSPanel.MIDDLE).addActionListener(
      new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.getFileName("Substructure Output",
                              MoSS.this.subname); } } );
    tab.addButton("View", MoSSPanel.RIGHT).addActionListener(
      new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.showTable(MoSS.this.subname); } } );
    this.subname = tab.addFileInput("moss.sub");

    tab.addLabel("Identifier output file:");
    tab.addButton("Select", MoSSPanel.MIDDLE).addActionListener(
      new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.getFileName("Identifier", MoSS.this.idsname); } } );
    tab.addButton("View", MoSSPanel.RIGHT).addActionListener(
      new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.showTable(MoSS.this.idsname); } } );
    this.idsname = tab.addFileInput("");

    tab.addLabel("Input format:");
    this.infmt   = tab.addComboBox(MoSS.FORMAT_NAMES);
    tab.addLabel("Output format:");
    this.subfmt  = tab.addComboBox(MoSS.FORMAT_NAMES);
    tab.addLabel("Seed format:");
    this.seedfmt = tab.addComboBox(MoSS.SEED_NAMES);
 
    tab.addLabel("Seed description:");
    this.seed = tab.addTextInput("*");
    tab.addHelp("The seed is a substructure from which to start "
               +"the search.\nA star (*) represents an empty seed "
               +"(no restriction).");
    tab.addFiller(0);

    /* --- basic parameters --- */
    this.pane.addTab("Basic Parameters", tab = new MoSSPanel());

    tab.addLabel("Threshold for split:");
    this.thresh = tab.addNumberInput("0.5");
    tab.addLabel("Invert split:");
    this.invert = tab.addCheckBox(false);
    tab.addHelp("The graphs in the input data file are split "
               +"into a focus set and\nits complement. "
               +"Graphs with an associated value less than or\n"
               +"equal to the threshold are in the focus, "
               +"all other graphs are\nin the complement. "
               +"Checking this box exchanges the two sets.");

    tab.addLabel("Minimal substructure size:");
    this.minsize = tab.addSpinner(1, 1, 999999, 1);
    tab.addLabel("Maximal substructure size:");
    this.maxsize = tab.addSpinner(0, 0, 999999, 1);
    tab.addHelp("The substructure size is measured as the number "
               +"of nodes.\nA maximal size of zero means that "
               +"there is no size limit.");

    tab.addLabel("Node types to exclude:");
    this.extype = tab.addTextInput("H");
    tab.addLabel("Seed types to exclude:");
    this.exseed = tab.addTextInput("");
    tab.addHelp("The former are generally excluded from the search, "
               +"the latter\nare not used as seeds, but may appear "
               +"with other seeds.\nBoth sets of node types have to "
               +"be specified in seed format.");
    tab.addFiller(0);

    /* --- support parameters --- */
    this.pane.addTab("Support", tab = new MoSSPanel());

    tab.addLabel("Minimal support in focus:");
    this.minsupp = tab.addNumberInput("10.0");
    tab.addLabel("Maximal support in complement:");
    this.maxsupp = tab.addNumberInput("2.0");
    tab.addLabel("Absolute support:");
    this.abssupp = tab.addCheckBox(false);
    tab.addHelp("If this box is checked, "
               +"the support thresholds are interpreted\n"
               +"as absolute numbers. Otherwise the values are "
               +"interpreted as\npercentages of the size (number "
               +"of graphs or number of nodes)\nof the respective "
               +"subset (focus or complement).");
    tab.addFiller(4);

    tab.addLabel("Substructure support type:", MoSSPanel.RIGHT);
    this.supptype = tab.addComboBox(MoSS.SUPP_NAMES);
    this.supptype.addItemListener(this);
 
    this.splbls = new JLabel[2];
    this.splbls[0] = tab.addLabel("Use greedy MIS algorithm:");
    this.greedy = tab.addCheckBox(true);
    tab.addHelp("Exact maximum independent set algorithms "
               +"can be very slow.");

    this.splbls[1] = tab.addLabel("Report only closed substructures:");
    this.closed = tab.addCheckBox(true);
    tab.addHelp("A substructure is closed if no supergraph "
                +"has the same support.\nThis provides a lossless "
                +"way of reducing the size of the output.");

    this.itemStateChanged(null);
    tab.addFiller(0);

    /* --- matching --- */
    this.pane.addTab("Matching", tab = new MoSSPanel());

    tab.addLabel("Aromatic bonds:");
    this.bdarom = tab.addComboBox(MoSS.AROM_NAMES);
    tab.addHelp("Downgrading aromatic bonds means treating them "
               +"as single,\nupgrading means treating them "
               +"as double bonds.");

    tab.addLabel("Ignore type of bonds:");
    this.bdtype = tab.addComboBox(MoSS.TYPE_NAMES);
    tab.addLabel("Ignore type of atoms:");
    this.attype = tab.addComboBox(MoSS.TYPE_NAMES);
    tab.addHelp("In order to be able to ignore the atom type "
               +"or the bond type\nin rings, ring bonds must be "
               +"distinguished from other bonds\n"
               +"(see tab \"Rings and Chains\").");

    tab.addLabel("Match charge of atoms:");
    this.charge = tab.addCheckBox(false);
    tab.addHelp("If the charge is matched, atoms with the same "
               +"element type\nbut different charge are seen as "
               +"different atoms.");
    tab.addLabel("Match aromaticity of atoms:");
    this.atarom = tab.addCheckBox(false);
    tab.addHelp("An atom is aromatic if it is part "
               +"of an aromatic ring.");

    tab.addFiller(6);
    tab.addHelp("All of the above options have an effect "
               +"only if the input data\ndescribes molecules "
               +"(determined from the input/output format).");
    tab.addFiller(0);

    /* --- rings & chains --- */
    this.pane.addTab("Rings & Chains", tab = new MoSSPanel());

    tab.addLabel("Convert Kekule representations:");
    this.kekule = tab.addCheckBox(true);
    tab.addHelp("Aromatic rings may be coded as alternating single "
               +"and double\nbonds (so-called Kekule representation "
               +"of an aromatic ring).\nIt is recommended to convert "
               +"these to actual aromatic bonds.");

    tab.addLabel("Distinguish ring bonds:");
    this.rings = tab.addCheckBox(false);
    this.rings.addChangeListener(this);

    this.rclbls = new JLabel[3];
    this.rclbls[0] = tab.addLabel("Minimal ring size:");
    this.minring   = tab.addSpinner(5, 3, 256, 1);
    this.rclbls[1] = tab.addLabel("Maximal ring size:");
    this.maxring   = tab.addSpinner(6, 3, 256, 1);
    this.rclbls[2] = tab.addLabel("Ring extensions:");
    this.ringext   = tab.addComboBox(MoSS.RING_NAMES);
    tab.addHelp("Ring extensions require that ring bonds are "
               +"distinguished from\nother bonds and that a range "
               +"of relevant ring sizes is specified.\nIt is "
               +"recommended to use full ring extensions if any.");

    tab.addFiller(4);
    tab.addLabel("Variable length carbon chains:");
    this.chains = tab.addCheckBox(false);
    tab.addHelp("A carbon chain consists only of carbon atoms "
               +"connected\nby single bonds, which are bridges "
               +"in the molecule.");

    this.stateChanged(null);
    tab.addFiller(0);

    /* --- search & pruning --- */
    this.pane.addTab("Search & Pruning", tab = new MoSSPanel());

    tab.addLabel("Extension type:");
    this.exttype = tab.addComboBox(MoSS.EXT_NAMES);
    tab.addHelp("Maximum source extension defines "
               +"the MoSS/MoFa algorithm,\nrightmost path "
               +"extension the gSpan/CloseGraph algorithm.");

    tab.addLabel("Perfect extension pruning:");
    this.perfect = tab.addComboBox(MoSS.PEP_NAMES);
    tab.addHelp("Partial perfect extension pruning discards "
               +"search tree branches\nto the right of a "
               +"perfect extension, full perfect extension pruning\n"
               +"also discards search tree branches "
               +"to the left of it.");
    
    tab.addLabel("Equivalent sibling pruning:");
    this.equiv = tab.addCheckBox(false);
    tab.addHelp("Equivalent sibling pruning can discard "
               +"certain duplicate fragments,\n"
               +"which otherwise have to be removed "
               +"with canonical form pruning.");
    tab.addLabel("Canonical form pruning:");
    this.canonic = tab.addCheckBox(true);
    tab.addHelp("Otherwise a repository of already processed "
               +"subgraphs is used.");
    tab.addLabel("Node orbit filtering:");
    this.orbits = tab.addCheckBox(true);
    tab.addHelp("Node orbit filtering suppresses "
               +"most equivalent siblings early\n"
               +"and thus helps to avoid redundant search. "
               +"It requires\ncanonical form pruning "
               +"to determine the node orbits.");
    tab.addFiller(0);

    /* --- embeddings --- */
    this.pane.addTab("Embeddings", tab = new MoSSPanel());

    tab.addLabel("Embeddings from level:");
    this.level = tab.addSpinner(0, -1, 999999, 1);
    tab.addHelp("In order to speed up extensions each fragment "
               +"maintains\na list of its embeddings into the graphs "
               +"of the database.\nHowever, it can be advantageous "
               +"to use embeddings only\ndeeper in the search tree. "
               +"In addition, for graphs with few\nlabels/types it "
               +"can be faster not to use embeddings at all (-1).");
    tab.addFiller(4);
    tab.addLabel("Maximal embeddings:");
    this.maxepg = tab.addSpinner(0, 0, 999999, 1);
    tab.addHelp("Restricting the maximal number of embeddings "
               +"that are\nkept per graph can reduce the amount "
               +"of memory needed.\nDiscarded embeddings are "
               +"recreated when they are needed.\nA value of zero "
               +"means that there is no restriction.");
    tab.addLabel("Unembed sibling nodes:");
    this.unembed = tab.addCheckBox(false);
    tab.addHelp("Since the mining process is basically a depth-first "
               +"search,\nthe embeddings of siblings of the current "
               +"search tree node\ncan be removed. They are recreated "
               +"when they are needed.");
    tab.addFiller(8);

    tab.addHelp("These options help to save memory, sometimes "
               +"considerably,\nbut they may increase the "
               +"processing time.");
    tab.addFiller(0);

    /* --- debugging --- */
    this.pane.addTab("Debugging", tab = new MoSSPanel());

    tab.addLabel("Normalize output:");
    this.normal = tab.addCheckBox(false);
    tab.addHelp("Normalize the description of the found "
               +"substructures,\nso that they can be compared "
               +"by simple string matching.\n"
               +"This is mainly a debugging option, "
               +"since it helps to detect\ndifferences "
               +"in the output of different algorithm variants.");

    tab.addLabel("Verbose message output:");
    this.verbose = tab.addCheckBox(false);
    tab.addHelp("Print the nodes of the search tree "
               +"during the search.\n"
               +"Since the search tree can become very large, "
               +"this option\nshould be activated only "
               +"for very small input data sets.");
    tab.addFiller(0);

    /* --- about --- */
    this.pane.addTab("About", tab = new MoSSPanel());

    about = new JTextArea("MoSS - Molecular Substructure Miner");
    about.setBackground(tab.getBackground());
    about.setFont(MoSSPanel.BOLD);
    about.setEditable(false);
    tab.add(about, MoSSPanel.RIGHT);

    tab.addHelp(
       "A simple graphical user interface for the MoSS program.\n\n"
      +"GUI version 2.6 (2011.08.01), Miner " +Miner.VERSION +"\n"
      +"written by Christian Borgelt\n"
      +"European Centre for Soft Computing\n"
      +"c/ Gonzalo Gutierrez Quiros s/n\n"
      +"E-33600 Mieres, Asturias, Spain\n"
      +"christian.borgelt@softcomputing.es\n\n"
      +"This program is free software;\n"
      +"you can redistribute it and/or modify it under\n"
      +"the terms of the GNU Lesser General Public License\n"
      +"as published by the Free Software Foundation.\n\n"
      +"This program is distributed in the hope that\n"
      +"it will be useful, but WITHOUT ANY WARRANTY;\n"
      +"without even the implied warranty of MERCHANTABILITY\n"
      +"or FITNESS FOR A PARTICULAR PURPOSE. See the\n"
      +"GNU Lesser (Library) General Public License "
      +"for more details.");

    /* --- buttons --- */
    bbar = new JPanel(new GridLayout(1, 4, 4, 4));
    bbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    bbar.add(this.exec = new JButton("Run"));
    this.exec.addActionListener(new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.execute(); } } );
    bbar.add(button = new JButton("Load"));
    button.addActionListener(new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.loadConfig(null); } } );
    bbar.add(button = new JButton("Save"));
    button.addActionListener(new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        MoSS.this.saveConfig(null); } } );
    if (this.isprog) {          /* terminate the program */
      bbar.add(button = new JButton("Quit"));
      button.addActionListener(new ActionListener () {
        public void actionPerformed (ActionEvent e) {
          System.exit(0); } } );}
    else {                      /* only close the dialog box */
      bbar.add(button = new JButton("Close"));
      button.addActionListener(new ActionListener () {
        public void actionPerformed (ActionEvent e) {
          MoSS.this.setVisible(false); } } );
    }                           /* configure the button bar */

    /* --- status line --- */
    this.stat = new JTextField("MoSS - Molecular Substructure Miner");
    this.stat.setEditable(false);
    bottom = new JPanel(new BorderLayout());
    bottom.add(bbar,      BorderLayout.NORTH);
    bottom.add(this.stat, BorderLayout.SOUTH);

    /* --- add contents --- */
    content = this.getContentPane();
    content.add(this.pane, BorderLayout.CENTER);
    content.add(bottom,    BorderLayout.SOUTH);
    this.setTitle("MoSS - Molecular Substructure Miner");
    this.setDefaultCloseOperation(this.isprog
      ? JFrame.EXIT_ON_CLOSE : WindowConstants.HIDE_ON_CLOSE);
    if (this.owner == null) this.setLocation(48, 48);
    else                    this.setLocationRelativeTo(this.owner);
    this.pack();                /* configure the frame */
  }  /* run() */

  /*------------------------------------------------------------------*/
  /** Handle the state change of the ring marking check box.
   *  @param  e the change event to react to
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void stateChanged (ChangeEvent e)
  {                             /* --- handle state changes */
    boolean state = this.rings.isSelected();
    this.minring.setEnabled(state);
    this.maxring.setEnabled(state);
    this.ringext.setEnabled(state);
    for (int i = this.rclbls.length; --i >= 0; )
      this.rclbls[i].setEnabled(state);
  }  /* stateChanged() */

  /*------------------------------------------------------------------*/
  /** Handle the state change of the support type combo box.
   *  @param  e the change event to react to
   *  @since  2007.07.06 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void itemStateChanged (ItemEvent e)
  {                             /* --- handle selection changes */
    int     i = this.supptype.getSelectedIndex();
    boolean state;
    this.greedy.setEnabled(state = (i >= 1) && (i <= 2));
    this.splbls[0].setEnabled(state);
    this.closed.setEnabled(state = (i <= 0));
    this.splbls[1].setEnabled(state);
  }  /* itemStateChanged() */

  /*------------------------------------------------------------------*/
  /** Get the file chooser (create if necessary).
   *  @return the file chooser
   *  @since  2006.07.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JFileChooser getChooser ()
  {                             /* --- get the file chooser */
    if (this.chooser != null)   /* if the chooser already exists, */
      return this.chooser;      /* simply return it */
    this.chooser = new JFileChooser();
    this.chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    this.chooser.setCurrentDirectory(new File("."));
    this.chooser.setFileHidingEnabled(true);
    this.chooser.setAcceptAllFileFilterUsed(true);
    this.chooser.setMultiSelectionEnabled(false);
    this.chooser.setFileView(null);
    return this.chooser;        /* create and configure the chooser */
  }  /* getChooser() */

  /*------------------------------------------------------------------*/
  /** Get a file name and store it in a text field.
   *  @param  title the title of the file chooser
   *  @param  text  the text field in which to store the file name
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void getFileName (String title, JTextField text)
  {                             /* -- get and store a file name */
    this.getChooser().setDialogTitle("Choose " +title +" File...");
    int r = this.chooser.showOpenDialog(this);
    if (r != JFileChooser.APPROVE_OPTION) return;
    text.setText(this.chooser.getSelectedFile().getPath());
  }  /* getFileName() */

  /*------------------------------------------------------------------*/
  /** Read a line (of the configuration file).
   *  @param  reader the reader to read from
   *  @return the line read
   *  @throws IOException if an i/o error occurs
   *                      or no line could be read
   *  @since  2007.02.24 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private String readLine (FileReader reader) throws IOException
  {                             /* --- read the next input line */
    int c = reader.read();      /* read the next character */
    if (c < 0) throw new IOException("premature end of file");
    if (this.buf == null)       /* create a buffer for a line */
      this.buf = new StringBuffer();
    this.buf.setLength(0);      /* clear the read buffer */
    while ((c >= 0) && (c != '\n')) {
      this.buf.append((char)c); /* append current character */
      c = reader.read();        /* to the read buffer and */
    }                           /* read the next character */
    return this.buf.toString(); /* return the input line read */
  }  /* readLine() */

  /*------------------------------------------------------------------*/
  /** Read an integer value (from the configuration file).
   *  @param  reader the reader to read from
   *  @return the integer value read
   *  @throws IOException if an i/o error occurs
   *                      or no field could be read
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private int readInt (FileReader reader) throws IOException
  {                             /* --- get next integer value */
    int c = reader.read();      /* read the next character */
    if (c < 0) throw new IOException("premature end of file");
    if (this.buf == null)       /* create a buffer for a field */
      this.buf = new StringBuffer();
    this.buf.setLength(0);      /* clear the read buffer */
    while ((c >= 0) && (c != ',') && (c != ';') && (c != '\n')) {
      this.buf.append((char)c); /* append current character */
      c = reader.read();        /* to the read buffer and */
    }                           /* read the next character */
    try { return Integer.parseInt(this.buf.toString().trim()); }
    catch (NumberFormatException e) { return 0; }
  }  /* readInt() */             /* decode and return the next field */

  /*------------------------------------------------------------------*/
  /** Load a configuration file and set the input fields.
   *  @param  file the file to load the configuration from
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void loadConfig (File file)
  {                             /* --- load configuration */
    if (file == null) {         /* if no file name is given */
      this.getChooser().setDialogTitle("Load Configuration...");
      int r = this.chooser.showOpenDialog(this);
      if (r != JFileChooser.APPROVE_OPTION) return;
      file = this.chooser.getSelectedFile();
    }                           /* let the user choose a file */
    try {                       /* open the configuration file */
      FileReader reader = new FileReader(file);
      this.datname.setText(this.readLine(reader));
      this.subname.setText(this.readLine(reader));
      this.idsname.setText(this.readLine(reader));
      this.seed.setText   (this.readLine(reader));
      this.extype.setText (this.readLine(reader));
      this.exseed.setText (this.readLine(reader));
      this.thresh.setText (this.readLine(reader));
      this.minsupp.setText(this.readLine(reader));
      this.maxsupp.setText(this.readLine(reader));
      this.minsize.setValue(new Integer(this.readInt(reader)));
      this.maxsize.setValue(new Integer(this.readInt(reader)));
      this.minring.setValue(new Integer(this.readInt(reader)));
      this.maxring.setValue(new Integer(this.readInt(reader)));
      this.level.setValue  (new Integer(this.readInt(reader)));
      this.maxepg.setValue (new Integer(this.readInt(reader)));
      this.infmt.setSelectedIndex   (this.readInt(reader));
      this.subfmt.setSelectedIndex  (this.readInt(reader));
      this.seedfmt.setSelectedIndex (this.readInt(reader));
      this.supptype.setSelectedIndex(this.readInt(reader));
      this.bdarom.setSelectedIndex  (this.readInt(reader));
      this.bdtype.setSelectedIndex  (this.readInt(reader));
      this.attype.setSelectedIndex  (this.readInt(reader));
      this.ringext.setSelectedIndex (this.readInt(reader));
      this.exttype.setSelectedIndex (this.readInt(reader));
      this.perfect.setSelectedIndex (this.readInt(reader));
      this.invert.setSelected       (this.readInt(reader) != 0);
      this.abssupp.setSelected      (this.readInt(reader) != 0);
      this.greedy.setSelected       (this.readInt(reader) != 0);
      this.closed.setSelected       (this.readInt(reader) != 0);
      this.charge.setSelected       (this.readInt(reader) != 0);
      this.atarom.setSelected       (this.readInt(reader) != 0);
      this.kekule.setSelected       (this.readInt(reader) != 0);
      this.rings.setSelected        (this.readInt(reader) != 0);
      this.chains.setSelected       (this.readInt(reader) != 0);
      this.equiv.setSelected        (this.readInt(reader) != 0);
      this.canonic.setSelected      (this.readInt(reader) != 0);
      this.orbits.setSelected       (this.readInt(reader) != 0);
      this.unembed.setSelected      (this.readInt(reader) != 0);
      this.normal.setSelected       (this.readInt(reader) != 0);
      this.verbose.setSelected      (this.readInt(reader) != 0);
      reader.close(); }         /* read the configuration values */
    catch (IOException e) {     /* and close the input file */
      JOptionPane.showMessageDialog(this,
        "Error reading configuration file:\n" +e.getMessage(),
        "Error", JOptionPane.ERROR_MESSAGE);
    }                           /* check for successful reading */
    this.stat.setText("configuration loaded: " +file.getName());
  }  /* loadConfig() */

  /*------------------------------------------------------------------*/
  /** Save a configuration file
   *  @param  file the file to save the current configuration to
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void saveConfig (File file)
  {                             /* --- save configuration */
    if (file == null) {         /* if no file name is given, */
      this.getChooser().setDialogTitle("Save Configuration...");
      int r = this.chooser.showOpenDialog(this);
      if (r != JFileChooser.APPROVE_OPTION) return;
      file = this.chooser.getSelectedFile();
    }                           /* let the user choose a file */
    try {                       /* open the configuration file */
      FileWriter writer = new FileWriter(file);
      writer.write(this.datname.getText()); writer.write('\n');
      writer.write(this.subname.getText()); writer.write('\n');
      writer.write(this.idsname.getText()); writer.write('\n');
      writer.write(this.seed.getText());    writer.write('\n');
      writer.write(this.extype.getText());  writer.write('\n');
      writer.write(this.exseed.getText());  writer.write('\n');
      writer.write(this.thresh.getText());  writer.write('\n');
      writer.write(this.minsupp.getText()); writer.write('\n');
      writer.write(this.maxsupp.getText()); writer.write('\n');
      writer.write(((Integer)this.minsize.getValue()).intValue() +",");
      writer.write(((Integer)this.maxsize.getValue()).intValue() +",");
      writer.write(((Integer)this.minring.getValue()).intValue() +",");
      writer.write(((Integer)this.maxring.getValue()).intValue() +",");
      writer.write(((Integer)this.level.getValue()).intValue()   +",");
      writer.write(((Integer)this.maxepg.getValue()).intValue()  +",");
      writer.write(this.infmt.getSelectedIndex()    +",");
      writer.write(this.subfmt.getSelectedIndex()   +",");
      writer.write(this.seedfmt.getSelectedIndex()  +",");
      writer.write(this.supptype.getSelectedIndex() +",");
      writer.write(this.bdarom.getSelectedIndex()   +",");
      writer.write(this.bdtype.getSelectedIndex()   +",");
      writer.write(this.attype.getSelectedIndex()   +",");
      writer.write(this.ringext.getSelectedIndex()  +",");
      writer.write(this.exttype.getSelectedIndex()  +",");
      writer.write(this.perfect.getSelectedIndex()  +",");
      writer.write(this.invert.isSelected()  ? "1," : "0,");
      writer.write(this.abssupp.isSelected() ? "1," : "0,");
      writer.write(this.greedy.isSelected()  ? "1," : "0,");
      writer.write(this.closed.isSelected()  ? "1," : "0,");
      writer.write(this.charge.isSelected()  ? "1," : "0,");
      writer.write(this.atarom.isSelected()  ? "1," : "0,");
      writer.write(this.kekule.isSelected()  ? "1," : "0,");
      writer.write(this.rings.isSelected()   ? "1," : "0,");
      writer.write(this.chains.isSelected()  ? "1," : "0,");
      writer.write(this.equiv.isSelected()   ? "1," : "0,");
      writer.write(this.canonic.isSelected() ? "1," : "0,");
      writer.write(this.orbits.isSelected()  ? "1," : "0,");
      writer.write(this.unembed.isSelected() ? "1," : "0,");
      writer.write(this.normal.isSelected()  ? "1," : "0,");
      writer.write(this.verbose.isSelected() ? "1"  : "0");
      writer.write('\n');       /* write the configuration values */
      writer.close(); }         /* and close the output file */
    catch (IOException e) {     /* check for successful writing */
      JOptionPane.showMessageDialog(this,
        "Error writing configuration file:\n" +e.getMessage(),
        "Error", JOptionPane.ERROR_MESSAGE);
    }                           /* show a status message */
    this.stat.setText("configuration saved: " +file.getName());
  }  /* saveConfig() */

  /*------------------------------------------------------------------*/
  /** Show a molecule table.
   *  @param  text the text field containing the file name
   *  @since  2007.02.15 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void showTable (JTextField text)
  {                             /* --- show a molecule table */
    String      fname;          /* name of the input file */
    int         mode;           /* read mode for the input */
    JFrame      frame;          /* frame of the table viewer */
    JScrollPane scroll;         /* scoll pane for the view */
    JTable      view;           /* table view */
    MoSSTable   tab;            /* table model */ 
    String      msg;            /* buffer for a message */
    Dimension   size;           /* preferred size of the view */

    fname = text.getText();     /* get and check the file name */
    if (fname.length() <= 0) return;
    if      (text == this.idsname) { /* if identifier file, */
      mode = MoSSTable.IDS;     /* get the identifier mode */
      msg  = null; }            /* clear the format string */
    else if (text == this.subname) { /* if substructure file */
      mode = MoSSTable.SUBS;    /* get mode and format */
      msg  = (String)this.subfmt.getSelectedItem(); }
    else {                      /* if graph data file */
      mode = MoSSTable.GRAPHS;  /* get mode and format */
      msg  = (String)this.infmt.getSelectedItem();
    }                           /* create a table for display */
    tab = new MoSSTable(mode, msg);
    try { tab.read(new File(fname)); }
    catch (IOException e) {     /* load table from a file */
      System.err.println(msg = e.getMessage());
      JOptionPane.showMessageDialog(this, msg,
        "Error", JOptionPane.ERROR_MESSAGE);
      return;                   /* report a possible error and */
    }                           /* abort the function in this case */
    view   = new JTable(tab);   /* create a table view */
    view.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    scroll = new JScrollPane(view);
    frame  = new JFrame();      /* add the table view to the frame */
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(scroll, BorderLayout.CENTER);
    frame.setTitle(fname);
    frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    frame.setLocationRelativeTo(this);
    size = view.getPreferredSize();
    if (size.width  > 400) size.width  = 400;
    if (size.height > 400) size.height = 400;
    view.setPreferredScrollableViewportSize(size);
    frame.pack();               /* prepare the frame again */
    frame.setVisible(true);     /* and finally show it */
  }  /* showTable() */

  /*------------------------------------------------------------------*/
  /** Execute the substructure search, that is, start a miner.
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public void execute ()
  {                             /* --- execute substructure search */
    int    mode;                /* search mode */
    int    type;                /* support type */
    String fname;               /* buffer for a file name */
    String fmt;                 /* input/output format */
    float  split;               /* threshold for split */
    float  supp,  comp;         /* support in focus and complement */
    int    matom, mbond;        /* atom and bond masks outside rings */
    int    mrgat, mrgbd;        /* atom and bond masks inside  rings */
    int    min,   max;          /* minimum and maximum fragment size */
    int    rgmin, rgmax;        /* minimum and maximum ring size */

    if (this.running) {         /* if running, abort computations */
      if (this.aborted) return; /* if already aborted, abort */
      this.stat.setText("aborting search ...");
      this.aborted = true; this.miner.abort(); return;
    }                           /* abort the miner and return */
    this.start = System.currentTimeMillis();
    this.miner = new Miner();   /* create a substructure miner */
    try {                       /* and try to initialize it */
      try { split = Float.parseFloat(this.thresh.getText()); }
      catch (Exception e) { split= 0.5F; }
      this.thresh.setText("" +split); /* get the threshold value */
      this.miner.setGrouping(split, this.invert.isSelected());

      fname = this.datname.getText();
      if (fname.length() == 0)  /* get and check input file name */
        throw new IOException("no input data file specified");
      fmt = (String)this.infmt.getSelectedItem();
      this.miner.setInput(fname, fmt);

      fname = this.subname.getText();
      if (fname.length() == 0)  /* get and check output file name */
        throw new IOException("no substructure output file specified");
      fmt = (String)this.subfmt.getSelectedItem();
      this.miner.setOutput(fname, fmt, this.idsname.getText());

      fmt = (String)this.seedfmt.getSelectedItem();
      this.miner.setSeed    (this.seed.getText(),   fmt);
      this.miner.setExcluded(this.extype.getText(),
                             this.exseed.getText(), fmt);

      min = ((Integer)this.minsize.getValue()).intValue();
      max = ((Integer)this.maxsize.getValue()).intValue();
      this.miner.setSizes(min, max);  /* set the fragment sizes */

      try { supp = Math.abs(Float.parseFloat(this.minsupp.getText())); }
      catch (Exception e) { supp = 10.0F; }
      this.minsupp.setText("" +supp); /* get minimal support in focus */
      try { comp = Math.abs(Float.parseFloat(this.maxsupp.getText())); }
      catch (Exception e) { comp = 2.0F; }
      this.maxsupp.setText("" +comp); /* get max. supp. in complement */
      if (this.abssupp.isSelected()) {
        supp = -supp; comp = -comp; } /* turn into absolute support */
      if (supp >= 0) supp *= 0.01F;   /* change support values from */
      if (comp >= 0) comp *= 0.01F;   /* percentages to fractions */
      this.miner.setLimits(supp, comp);

      mode = Miner.EDGEEXT;     /* set default search mode */
      if (this.closed.isSelected())  mode |= Miner.CLOSED;
      if (this.kekule.isSelected())  mode |= Miner.AROMATIZE;
      if (this.chains.isSelected())  mode |= Miner.CHAINEXT;
      if (this.canonic.isSelected()) mode |= Miner.PR_CANONIC;
      if (this.equiv.isSelected())   mode |= Miner.PR_EQUIV;
      if (this.orbits.isSelected())  mode |= Miner.ORBITS;
      if (this.unembed.isSelected()) mode |= Miner.UNEMBED;
      if (this.normal.isSelected())  mode |= Miner.NORMFORM;
      if (this.verbose.isSelected()) mode |= Miner.VERBOSE;
      switch (this.perfect.getSelectedIndex()) {
        case 0: mode |= Miner.PR_PERFECT; break;
        case 1: mode |= Miner.PR_PARTIAL; break;
      }                         /* set pruning and other flags */
      if (!this.rings.isSelected())  /* if not to dist. ring bonds, */
        rgmin = rgmax = 0;      /* clear the range of ring sizes */
      else {                    /* if to distinguish ring bonds */
        switch (this.ringext.getSelectedIndex()) {
          case 1: mode |= Miner.RINGEXT;                     break;
          case 2: mode |= Miner.CLOSERINGS|Miner.PR_UNCLOSE; break;
          case 3: mode |= Miner.RINGEXT   |Miner.MERGERINGS
                       |  Miner.CLOSERINGS|Miner.PR_UNCLOSE; break;
        }                       /* set ring extension flags */
        rgmin = ((Integer)this.minring.getValue()).intValue();
        rgmax = ((Integer)this.maxring.getValue()).intValue();
      }                         /* get ring sizes and set them */
      this.miner.setRingSizes(rgmin, rgmax);
      this.miner.setMode(mode); /* set the search mode */
      switch (this.supptype.getSelectedIndex()) {
        case  1: type = Fragment.MIS_OLAP;  break;
        case  2: type = Fragment.MIS_HARM;  break;
        case  3: type = Fragment.MIN_IMAGE; break;
        default: type = Fragment.GRAPHS;    break;
      }                         /* get the support type */
      if (this.greedy.isSelected())
        type |= Fragment.GREEDY;/* add the greedy algorithm flag */
      this.miner.setType(type); /* and set the support type */
      fmt = MoSS.EXT_CODES[this.exttype.getSelectedIndex()];
      this.miner.setCnF(CanonicalForm.createCnF(fmt));
                                /* set the extension type */
      matom = mrgat = AtomTypeMgr.ELEMMASK; /* set default masks */
      mbond = mrgbd = BondTypeMgr.BONDMASK; /* for atoms and bonds */
      switch (this.bdarom.getSelectedIndex()) {
        case 1: mbond &=  BondTypeMgr.DOWNGRADE;
                mrgbd &=  BondTypeMgr.DOWNGRADE; break;
        case 2: mbond &=  BondTypeMgr.UPGRADE;
                mrgbd &=  BondTypeMgr.UPGRADE;   break;
      }                         /* treatment of aromatic bonds */
      switch (this.bdtype.getSelectedIndex()) {
        case 1: mrgbd &=  BondTypeMgr.SAMETYPE; break;
        case 2: mbond &=  BondTypeMgr.SAMETYPE;
                mrgbd &=  BondTypeMgr.SAMETYPE; break;
      }                         /* where to ignore the bond type */
      switch (this.attype.getSelectedIndex()) {
        case 1: mrgat &= ~AtomTypeMgr.ELEMMASK; break;
        case 2: matom &= ~AtomTypeMgr.ELEMMASK;
                mrgat &= ~AtomTypeMgr.ELEMMASK; break;
      }                         /* where to ignore the atom type */
      if (this.charge.isSelected()) matom |= AtomTypeMgr.CHARGEMASK;
      if (this.atarom.isSelected()) matom |= AtomTypeMgr.AROMATIC;
      this.miner.setMasks(matom, mbond, mrgat, mrgbd);

      this.miner.setEmbed(      /* maximal number of embeddings */
        ((Integer)this.level.getValue()).intValue(),
        ((Integer)this.maxepg.getValue()).intValue()); }
    catch (Exception e) {       /* check successful initialization */
      System.out.println("\nerror: " +e.getMessage());
      JOptionPane.showMessageDialog(this,
        "Initialization failed:\n" +e.getMessage(),
        "Error", JOptionPane.ERROR_MESSAGE);
      this.running = false;     /* if the initialization failed, */
      this.miner   = null;      /* show an error message, */
      return;                   /* set the status to not running, */
    }                           /* and "delete" the miner */
    this.exec.setText("Abort"); /* change the button text */
    this.running = true;        /* set the status to running */
    this.aborted = false;       /* and not aborted */
    this.thread  = new Thread(this.miner);
    this.thread.start();        /* start computations as a thread */
    this.timer = new Timer(200, new ActionListener () {
      private int cnt = 0;      /* counter for status text update */
      public void actionPerformed (ActionEvent e) {
        if (!MoSS.this.thread.isAlive()) {
          MoSS.this.timer.stop();
          MoSS.this.result();   /* stop the status update timer */
          return;               /* and show the computation result, */
        }                       /* then abort the function */
        if (--this.cnt <= 0) {  /* on every 5th check (once a second) */
          this.cnt = 5; MoSS.this.update(); }
      } } );                    /* update the status text */
    this.timer.start();         /* start the status update timer */
  }  /* execute() */

  /*------------------------------------------------------------------*/
  /** Update the status line during the substructure search.
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void update ()
  {                             /* --- update the status text */
    if (!this.running) return;  /* check for a running search */
    int n = this.miner.getCurrent();
    if (n < 0) this.stat.setText("reading ... " +(-n) +" graph(s)");
    else this.stat.setText("searching ... " +n +" substructure(s)");
  }  /* update() */

  /*------------------------------------------------------------------*/
  /** Report the results of a substructure search in a dialog box.
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  private void result ()
  {                             /* --- show result of computation */
    int       n;                /* number of found substructures */
    float     t;                /* execution time */
    Exception e;                /* error status of the search */

    this.running = false;       /* thread is no longer running */
    this.exec.setText("Run");   /* reset the button text */
    if (this.aborted) {         /* if execution has been aborted */
      this.miner = null;        /* "delete" the miner */
      System.err.println("\nsearch aborted");
      this.stat.setText("substructure search aborted.");
      JOptionPane.showMessageDialog(this,
        "Substructure search aborted.",
        "Information", JOptionPane.INFORMATION_MESSAGE);
      return;                   /* show an abortion dialog */
    }                           /* and abort the function */
    e = this.miner.getError();  /* get the error status of the search */
    if (e != null) {            /* if an error occurred */
      this.miner = null;        /* "delete" the miner */
      this.stat.setText("substructure search failed.");
      JOptionPane.showMessageDialog(this,
        "Substructure search failed:\n" +e.getMessage() +"\n"
       +"(See terminal for more information.)",
        "Error", JOptionPane.ERROR_MESSAGE);
      return;                   /* report the error */
    }                           /* and abort the function */
    this.miner.stats();         /* show search statistics */
    n = this.miner.getCurrent(); if (n < 0) n = 0;
    t = (System.currentTimeMillis() -this.start) / 1000.0F;
    this.stat.setText(n +" substructure(s), "
                        +"total search time: " +t +"s");
    JOptionPane.showMessageDialog(this,
      "Found " +n +" substructure(s).\n"
     +"Total search time: " +t +"s.\n"
     +"(See terminal for more information.)",
      "Information", JOptionPane.INFORMATION_MESSAGE);
    this.miner = null;          /* show a success message */
  }  /* result() */             /* and "delete" the miner */

  /*------------------------------------------------------------------*/
  /** Main function to invoke the user interface
   *  as a stand-alone program.
   *  @param  args the command line arguments
   *  @since  2006.07.13 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static void main (String args[])
  {                             /* --- main function */
    MoSS moss = new MoSS();     /* create a MoSS dialog */
    if (args.length > 0)        /* load configuration if necessary */
      moss.loadConfig(new File(args[0]));
    moss.setVisible(true);      /* show the dialog */
  }  /* main() */

}  /* class MoSS */
