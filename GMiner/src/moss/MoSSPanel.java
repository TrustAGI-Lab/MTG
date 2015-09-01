/*----------------------------------------------------------------------
  File    : MoSSPanel.java
  Contents: tab panel for the MoSS graphical user interfaces
  Author  : Christian Borgelt
  History : 2007.07.07 file created
            2011.08.01 ipadx and ipady set for all grid bag constraints
----------------------------------------------------------------------*/
package moss;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.InputVerifier;

/*--------------------------------------------------------------------*/
/** Class for a tab panel for the MoSS graphical user interface.
 *  @author Christian Borgelt
 *  @since  2007.07.07 */
/*--------------------------------------------------------------------*/
public class MoSSPanel extends JPanel {

  private static final long serialVersionUID = 0x00020003;

  /*------------------------------------------------------------------*/
  /*  constants                                                       */
  /*------------------------------------------------------------------*/
  /** the font for input text fields */
  protected static final Font BOLD  = new Font("Dialog", Font.BOLD, 12);
  /** the font for help  text fields */
  protected static final Font SMALL = new Font("Dialog", Font.PLAIN,10);
  /** the grid bag constraints for labels */
  protected static final GridBagConstraints LEFT;
  /** the grid bag constraints for middle input fields */
  protected static final GridBagConstraints MIDDLE;
  /** the grid bag constraints for right  input fields */
  protected static final GridBagConstraints RIGHT;
  /** the grid bag constraints for fillers */
  protected static final GridBagConstraints FILL;

  /*------------------------------------------------------------------*/
  /*  class initialization code                                       */
  /*------------------------------------------------------------------*/

  static {                      /* --- initialize the class */
    LEFT            = new GridBagConstraints();
    LEFT.fill       = GridBagConstraints.BOTH;
    LEFT.weightx    = 0.0;
    LEFT.ipadx      = LEFT.ipady   = 10;
    MIDDLE          = new GridBagConstraints();
    MIDDLE.fill     = GridBagConstraints.BOTH;
    MIDDLE.weightx  = 1.0;
    MIDDLE.ipadx    = MIDDLE.ipady = 1;
    RIGHT           = new GridBagConstraints();
    RIGHT.fill      = GridBagConstraints.BOTH;
    RIGHT.weightx   = 1.0;
    RIGHT.gridwidth = GridBagConstraints.REMAINDER;
    RIGHT.ipadx     = RIGHT.ipady  = 1;
    FILL            = new GridBagConstraints();
    FILL.fill       = GridBagConstraints.BOTH;
    FILL.weightx    = FILL.weighty = 1.0;
    FILL.gridwidth  = GridBagConstraints.REMAINDER;
    FILL.ipadx      = FILL.ipady  = 1;
  }                             /* create the grid bag constraints */

  /*------------------------------------------------------------------*/
  /** Create a MoSS tab panel.
   *  @since  2007.02.11 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected MoSSPanel ()
  {                             /* --- create a tab panel */
    super(new GridBagLayout()); /* create the and configure the panel */
    this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
  }  /* TabPanel() */

  /*------------------------------------------------------------------*/
  /** Add a component.
   *  @param  comp the component to add
   *  @param  gbc  the grid bag constraints to use
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void add (Component comp, GridBagConstraints gbc)
  {                             /* --- add a component */
    ((GridBagLayout)this.getLayout()).setConstraints(comp, gbc);
    this.add(comp);             /* set constraints and add component */
  }  /* add() */

  /*------------------------------------------------------------------*/
  /** Add a help text.
   *  @param  text the help text
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void addHelp (String text)
  {                             /* --- add a help text to a panel */
    JTextArea help = new JTextArea(text);
    this.add(help, RIGHT);      /* create and add a text area */
    help.setBackground(this.getBackground());
    help.setFont(SMALL);        /* configure the text area */
    help.setEditable(false);
    help.setFocusable(false);
  }  /* addHelp() */

  /*------------------------------------------------------------------*/
  /** Add a filler.
   *  @param  height the height of the filler
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected void addFiller (int height)
  {                             /* --- add a filler to a tab */
    JTextArea fill = new JTextArea((String)null);
    this.add(fill, (height <= 0) ? FILL : RIGHT);
    fill.setPreferredSize(new Dimension(0, height));
    fill.setBackground(this.getBackground());
    fill.setEditable(false);    /* configure the text area */
  }  /* addFiller() */

  /*------------------------------------------------------------------*/
  /** Add a label to a tab.
   *  @param  text the text of the label
   *  @return the created <code>JLabel</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JLabel addLabel (String text)
  { return this.addLabel(text, LEFT); }

  /*------------------------------------------------------------------*/
  /** Add a label to a tab.
   *  @param  text the text of the label
   *  @param  gbc  the grid bag constraints to use
   *  @return the created <code>JLabel</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JLabel addLabel (String text, GridBagConstraints gbc)
  {                             /* --- add a label to a tab */
    JLabel label = new JLabel(text);
    this.add(label, gbc);       /* create and add a label and */
    return label;               /* return the created label */
  }  /* addLabel() */

  /*------------------------------------------------------------------*/
  /** Add a button to a tab.
   *  @param  text the text of the button
   *  @return the created <code>JButton</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JButton addButton (String text)
  { return this.addButton(text, RIGHT); }

  /*------------------------------------------------------------------*/
  /** Add a button to a tab.
   *  @param  text the text of the button
   *  @param  gbc  the grid bag constraints to use
   *  @return the created <code>JButton</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JButton addButton (String text, GridBagConstraints gbc)
  {                             /* --- add a button to a tab */
    JButton button = new JButton(text);
    this.add(button, gbc);      /* create and add a button and */
    return button;              /* return the created button */
  }  /* addButton() */

  /*------------------------------------------------------------------*/
  /** Add a text input field to a tab.
   *  @param  text the initial text of the text input field
   *  @return the created <code>JTextField</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JTextField addTextInput (String text)
  { return this.addTextInput(text, RIGHT); }

  /*------------------------------------------------------------------*/
  /** Add a text input field to a tab.
   *  @param  text the initial text of the text input field
   *  @param  gbc  the grid bag constraints to use
   *  @return the created <code>JTextField</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JTextField addTextInput (String text, GridBagConstraints gbc)
  {                             /* --- add a text input to a tab */
    JTextField tfld = new JTextField(text);
    this.add(tfld, gbc);        /* create and add a text field and */
    tfld.setFont(BOLD);         /* and set bold font */
    return tfld;                /* return the created text field */
  }  /* addTextInput() */

  /*------------------------------------------------------------------*/
  /** Create a number input field.
   *  @param  text the initial text of the input field
   *  @return the created <code>JFormattedTextField</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  public static JTextField createNumberInput (String text)
  {                             /* --- create a number input field */
    JTextField tfld = new JTextField();
    tfld.setFont(BOLD);         /* create a text field */
    tfld.setText(text);         /* and configure it */
    tfld.setInputVerifier(new InputVerifier() {
      public boolean verify (JComponent comp) {
        String text = ((JTextField)comp).getText();
        if (text.length() <= 0) return true;
        try { Double.parseDouble(text); }
        catch (NumberFormatException e) { return false; }
        return true;            /* add a simple verifier */
      } } );                    /* for real-valued numbers */
    return tfld;                /* return the created text field */
  }  /* createNumberInput() */

  /*------------------------------------------------------------------*/
  /** Add a text input field to a tab.
   *  @param  text the initial text of the text input field
   *  @return the created <code>JFormattedTextField</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JTextField addNumberInput (String text)
  { return this.addNumberInput(text, RIGHT); }

  /*------------------------------------------------------------------*/
  /** Add a text input field to a tab.
   *  @param  text the initial text of the text input field
   *  @param  gbc  the grid bag constraints to use
   *  @return the created <code>JFormattedTextField</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JTextField addNumberInput (String text, GridBagConstraints gbc)
  {                             /* --- add a number input to a tab */
    JTextField tfld = MoSSPanel.createNumberInput(text);
    this.add(tfld, gbc);        /* create and add a text field */
    tfld.setFont(BOLD);         /* and set bold font */
    return tfld;                /* return the created text field */
  }  /* addNumberInput() */

  /*------------------------------------------------------------------*/
  /** Add a file input field to a tab.
   *  @param  text the initial text of the file input field
   *  @return the created <code>JTextField</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JTextField addFileInput (String text)
  { return this.addFileInput(text, RIGHT); }

  /*------------------------------------------------------------------*/
  /** Add a text input field to a tab.
   *  @param  text the initial text of the file input field
   *  @param  gbc  the grid bag constraints to use
   *  @return the created <code>JTextField</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JTextField addFileInput (String text, GridBagConstraints gbc)
  {                             /* --- add a file input to a tab */
    JTextField tfld = new JTextField(text);
    this.add(tfld, gbc);        /* create and add a text field */
    return tfld;                /* return the created text field */
  }  /* addFileInput() */

  /*------------------------------------------------------------------*/
  /** Add a check box to a tab.
   *  @param  state the initial state of the check box
   *  @return the created <code>JCheckBox</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JCheckBox addCheckBox (boolean state)
  { return this.addCheckBox(state, RIGHT); }

  /*------------------------------------------------------------------*/
  /** Add a check box to a tab.
   *  @param  state the initial state of the check box
   *  @param  gbc  the grid bag constraints to use
   *  @return the created <code>JCheckBox</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JCheckBox addCheckBox (boolean state, GridBagConstraints gbc)
  {                             /* --- add a check box to a tab */
    JCheckBox cbox = new JCheckBox("", state);
    this.add(cbox, gbc);        /* create and add a check box and */
    return cbox;                /* return the created check box */
  }  /* addCheckBox() */

  /*------------------------------------------------------------------*/
  /** Add a combo box to a tab.
   *  @param  items the list of items
   *  @return the created <code>JComboBox</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JComboBox addComboBox (String[] items)
  { return this.addComboBox(items, RIGHT); }

  /*------------------------------------------------------------------*/
  /** Add a combo box to a tab.
   *  @param  items the list of items
   *  @param  gbc   the grid bag constraints to use
   *  @return the created <code>JComboBox</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JComboBox addComboBox (String[] items, GridBagConstraints gbc)
  {                             /* --- add a combo box to a tab */
    JComboBox cbox = new JComboBox(items);
    this.add(cbox, gbc);        /* create and add a combo box */
    cbox.setFont(BOLD);         /* and set bold font */
    return cbox;                /* return the created combo box */
  }  /* addComboBox() */

  /*------------------------------------------------------------------*/
  /** Add a spinner to a tab.
   *  @param  val  the initial value
   *  @param  min  the minimal value
   *  @param  max  the maximal value
   *  @param  step the step size
   *  @return the created <code>JSpinner</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JSpinner addSpinner (int val, int min, int max, int step)
  { return this.addSpinner(val, min, max, step, RIGHT); }

  /*------------------------------------------------------------------*/
  /** Add a combo box to a tab.
   *  @param  val  the initial value
   *  @param  min  the minimal value
   *  @param  max  the maximal value
   *  @param  step the step size
   *  @param  gbc  the grid bag constraints to use
   *  @return the created <code>JSpinner</code>
   *  @since  2007.07.07 (Christian Borgelt) */
  /*------------------------------------------------------------------*/

  protected JSpinner addSpinner (int val, int min, int max, int step,
                              GridBagConstraints gbc)
  {                             /* --- add a combo box to a tab */
    JSpinner spin = new JSpinner(
      new SpinnerNumberModel(val, min, max, step));
    this.add(spin, gbc);        /* create and add a combo box */
    ((DefaultEditor)spin.getEditor()).getTextField().setFont(BOLD);
    return spin;                /* return the created combo box */
  }  /* addSpinner() */

}  /* class MoSSPanel() */
