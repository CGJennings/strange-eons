package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.spelling.ui.MenuBuilder;
import ca.cgjennings.spelling.ui.PopupMenuFactory;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

///////////////////////////////////////////////////////
//
// NOTE:
// The documented purpose of this class is to install
// mnemonics. However, since it hooks directly into the 
// Toolkit's event dispatch mechanism, it is also used
// to implement some global user experience improvements.
// This avoids having multiple Toolkit hooks,
// which can be expensive since they are called for
// every event.
//
// The undocumented improvements include:
//  - enforce minimum row count of 12 for all combo boxes
//  - provide an UndoManager and default popup menu for all text controls
//  - enable selection drag-and-drop in all text controls
//  - platform-specific workaround hacks
//
///////////////////////////////////////////////////////
/**
 * A singleton class that that intercepts components, extracts a mnemonic key
 * from the component's label or button text, and sets that as the the
 * component's mnemonic key.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class MnemonicInstaller {

    private MnemonicInstaller() {
    }

    public static final int LABELS = 1;
    public static final int MENUS = 2;
    public static final int BUTTONS = 4;
    public static final int TOGGLE_BUTTONS = 8;
    public static final int ALL = 15;

    private static int mask = 0;
    private static boolean installed = false;
    private static boolean hideMnem = false;

    /**
     * Sets whether mnemonics will be displayed. When set to <code>true</code>,
     * mnemonic codes will still be extracted and set on components, but they
     * will not be marked in the UI. For example, if mnemonic letters are
     * normally underlined, this underlining will be disabled.
     *
     * @param hide if <code>true</code>, mnemonics will be hidden
     */
    public static void setMnemonicHidden(boolean hide) {
        hideMnem = hide;
    }

    /**
     * Returns <code>true</code> if mnemonic letters will not be marked within
     * the UI.
     *
     * @return <code>true</code> if mnemonics are hidden
     * @see #setMnemonicHidden
     */
    public static boolean isMnemonicHidden() {
        return hideMnem;
    }

    /**
     * Returns the bitmask of component types that the manager will
     * automatically handle mnemonics for.
     *
     * @return the mask of component types
     */
    public static int getMask() {
        return mask;
    }

    /**
     * Sets the bitmask that determines which control types to manage. Setting
     * the mask to 0 will disable automatic management of mnemonics.
     *
     * @param mask the logical or of the mask values for the control types to
     * manage
     */
    public static void setMask(int mask) {
        MnemonicInstaller.mask = mask;
        if (!installed && mask != 0) {
            Toolkit.getDefaultToolkit().addAWTEventListener((AWTEvent event) -> {
                if (!(event instanceof ContainerEvent)) {
                    return;
                }
                ContainerEvent ce = (ContainerEvent) event;
                if (ce.getID() != ContainerEvent.COMPONENT_ADDED) {
                    return;
                }
                Component comp = ce.getChild();
                if (!(comp instanceof JComponent)) {
                    return;
                }
                if ((comp instanceof JLabel) && (MnemonicInstaller.mask & LABELS) != 0) {
                    createMnemonic((JLabel) comp);
                } else if (comp instanceof AbstractButton) {
                    if (comp instanceof JMenuItem && (MnemonicInstaller.mask & MENUS) != 0) {
                        createMnemonic((AbstractButton) comp);
                    }
                    if (comp instanceof JToggleButton && (MnemonicInstaller.mask & TOGGLE_BUTTONS) != 0) {
                        createMnemonic((AbstractButton) comp);
                    } else if ((MnemonicInstaller.mask & BUTTONS) != 0) {
                        createMnemonic((AbstractButton) comp);
                    }
                }
                
                // fixes for Nimbus L&F
                if (isNimbus()) {
                    // Hack to fix Aqua system menus for popup menus
                    // (otherwise they have no border)
                    if (USE_POPUP_BORDER_HACK && comp instanceof JPopupMenu) {
                        ((JPopupMenu) comp).setBorder(getHackPopupBorder());
                    }
                    
                    if (comp instanceof JComboBox) {
                        JComboBox cb = (JComboBox) comp;
//							if( cb.isEditable() ) {
//								Component c = cb.getEditor().getEditorComponent();
//								if( c instanceof JTextField ) {
//									JTextField tf = (JTextField) c;
//									Border b = tf.getBorder();
//									if( b != null && ((b instanceof EmptyBorder) || b.getClass().getSimpleName().equals( "SynthBorder" ) ) ) {
//										tf.setBorder( nimbusComboBoxBorder );
//									}
//								}
//							}
if (cb.getMaximumRowCount() < 12) {
    cb.setMaximumRowCount(12);
}
                    }
                }
                
                // fixes for text components to enable drag & drop
                if (comp instanceof JTextComponent) {
                    JTextComponent tc = (JTextComponent) comp;
                    if (!tc.getDragEnabled() && tc.getTransferHandler() != null) {
                        tc.setDropMode(DropMode.INSERT);
                        tc.setDragEnabled(true);
                    }

                    // if it isn't a spelling component, it doesn't have an
                    // undo manager and popup menu
                    if (tc.getClientProperty("SpellCheckTokenizer") == null) {
                        installDefaultMenu(tc);
                    }
                }
            }, AWTEvent.CONTAINER_EVENT_MASK);
        }
    }

    private static Border getHackPopupBorder() {
        if (nimbusBorder == null) {
            Color c = UIManager.getColor("nimbusBorder");
            if (c == null) {
                c = Color.LIGHT_GRAY;
            }
            nimbusBorder = new CompoundBorder(new LineBorder(c), new EmptyBorder(4, 0, 4, 0));
        }
        return nimbusBorder;
    }
    private static Border nimbusBorder;
    private static final boolean USE_POPUP_BORDER_HACK;

    static {
        USE_POPUP_BORDER_HACK = PlatformSupport.PLATFORM_IS_OSX && "true".equals(System.getProperty("apple.laf.useScreenMenuBar"));
    }

    private static void installDefaultMenu(final JTextComponent comp) {
        if (comp.getComponentPopupMenu() != null) {
            return;
        }

        final Document doc = comp.getDocument();
        final UndoManager manager = new UndoManager();
        manager.setLimit(512);

        doc.addUndoableEditListener((UndoableEditEvent evt) -> {
            manager.addEdit(evt.getEdit());
        });

        Action undo = new AbstractAction(UNDO) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (manager.canUndo()) {
                        manager.undo();
                    }
                } catch (CannotUndoException e) {
                }
            }

            @Override
            public boolean isEnabled() {
                return comp.isEditable() && manager.canUndo();
            }
        };

        Action redo = new AbstractAction(REDO) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    if (manager.canRedo()) {
                        manager.redo();
                    }
                } catch (CannotRedoException e) {
                }
            }

            @Override
            public boolean isEnabled() {
                return comp.isEditable() && manager.canRedo();
            }
        };

        Action selectAll = new AbstractAction(SELECT_ALL) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                comp.selectAll();
            }
        };

        comp.getActionMap().put(UNDO, undo);
        comp.getActionMap().put(REDO, redo);
        comp.getActionMap().put(SELECT_ALL, selectAll);
        comp.getInputMap().put(KeyStroke.getKeyStroke("control Z"), UNDO);
        comp.getInputMap().put(KeyStroke.getKeyStroke("meta Z"), UNDO);
        comp.getInputMap().put(KeyStroke.getKeyStroke("undo"), UNDO);
        comp.getInputMap().put(KeyStroke.getKeyStroke("control Y"), REDO);
        comp.getInputMap().put(KeyStroke.getKeyStroke("meta Y"), REDO);
        comp.getInputMap().put(KeyStroke.getKeyStroke("control A"), SELECT_ALL);
        comp.getInputMap().put(KeyStroke.getKeyStroke("meta A"), SELECT_ALL);

        try {
            JPopupMenu popup = new JPopupMenu();
            MENU_BUILDER.buildMenu(popup);
            comp.setComponentPopupMenu(popup);
        } catch (Throwable t) {
            StrangeEons.log.log(Level.WARNING, null, t);
        }
    }

    private static final MenuBuilder MENU_BUILDER = (JPopupMenu menu) -> {
        menu.add(Commands.CUT);
        menu.add(Commands.COPY);
        menu.add(Commands.PASTE);
        menu.addSeparator();
        menu.add(Commands.SELECT_ALL);
    };

    static {
        PopupMenuFactory.setDefaultMenuBuilder(MENU_BUILDER);
    }

    private static final String UNDO = "undo";
    private static final String REDO = "redo";
    private static final String SELECT_ALL = "select-all";

    private static boolean isNimbus() {
        if (nimbusCheck == 0) {
            nimbusCheck = UIManager.getLookAndFeel() instanceof NimbusLookAndFeel ? 1 : -1;
        }
        return nimbusCheck == 1;
    }
    private static int nimbusCheck = 0;

    private static void createMnemonic(JLabel label) {
        if (label.getLabelFor() == null) {
            return;
        }

        extractMnemonic(label.getText());

        label.setText(text);
        if (key != '\0') {
            label.setDisplayedMnemonic(key);
        }
        if (index >= 0 && !hideMnem) {
            label.setDisplayedMnemonicIndex(index);
        }
        if (hideMnem) {
            label.setDisplayedMnemonicIndex(-1);
        }
    }

    private static void createMnemonic(AbstractButton button) {
        extractMnemonic(button.getText());

        button.setText(text);
        if (key != '\0') {
            button.setMnemonic(key);
        }
        if (index >= 0 && !hideMnem) {
            button.setDisplayedMnemonicIndex(index);
        }
        if (hideMnem) {
            button.setDisplayedMnemonicIndex(-1);
        }
    }

    private static void extractMnemonic(String label) {
        // flag that no mnemonic was found
        index = -1;
        key = '\0';
        // adjustment to the index after replacing \&s
        int indexOffset = 0;

        if (label != null) {
            int pos = 0;
            boolean hasEscape = false;

            // find the first & that is not escaped
            for (;;) {
                pos = label.indexOf('&', pos);
                if (pos > 0 && label.charAt(pos - 1) == '\\') {
                    hasEscape = true;
                    ++pos;
                    continue;
                }
                break;
            }

            // check if there is an escape after any discovered & point
            if (pos >= 0 && !hasEscape && label.indexOf('\\', pos) >= 0) {
                hasEscape = label.indexOf("\\&", pos) >= 0;
            }

            // if there are escapes, the mnemonic index may change after
            // substitution (if & follows \&), so count the number of
            // \&s before the first &
            if (hasEscape) {
                boolean esc = false;
                for (int i = 0; i < label.length(); ++i) {
                    char c = label.charAt(i);
                    if (c == '\\') {
                        esc = true;
                        continue;
                    }
                    if (c == '&') {
                        if (esc) {
                            --indexOffset;
                        } else {
                            break;
                        }
                    }
                    esc = false;
                }
            }

            // there is an ampersand to convert to a mnemonic
            if (pos >= 0) {
                // remove the & from the label
                label = label.substring(0, pos) + label.substring(pos + 1);
                if (pos < label.length()) {
                    // to work, we need to be able to reduce the character
                    // to a letter in a-zA-Z0-9
                    char mnem = Character.toLowerCase(label.charAt(pos));

                    // if the char comes after 'z', then it may be a letter
                    // with an accent: we will try to decompose it
                    // and obtain the "plain" letter
                    if (mnem > 'z') {
                        String decomp = java.text.Normalizer.normalize(
                                String.valueOf(mnem), java.text.Normalizer.Form.NFD
                        );
                        if (!decomp.isEmpty()) {
                            mnem = decomp.charAt(0);
                        }
                    }

                    // if we were able to get a valid key, set the mnemonic info
                    if ((mnem >= 'a' && mnem <= 'z') || (mnem >= '0' && mnem <= '9')) {
                        index = pos + indexOffset;
                        key = mnem;
                    }
                }
            }

            if (hasEscape) {
                label = label.replace("\\&", "&");
            }
        }

        // if the text contains no &s, this is the original string object
        // otherwise it is a new string with the \&s and &s converted
        text = label;
    }

    // It is safe to share these since all processing must be in the event thread
    private static int index;
    private static char key;
    private static String text;

//	public static void main( String[] args ) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				JFrame f = new JFrame();
//				f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//
//				MnemonicInstaller.setMask( ALL );
//
//				JTextField name = new JTextField( 8 );
//				JLabel label = new JLabel( "&Name \\&tc" );
//				label.setLabelFor( name );
//				f.setLayout( new FlowLayout() );
//
//				f.add( label );
//				f.add( name );
//				f.add( new JButton( "The &Âccent" ) );
//				f.add( new JButton( "post&" ) );
//				f.add( new JButton( "sp& ace" ) );
//				f.add( new JButton( "esc\\&ape" ) );
//				f.add( new JButton( "esc\\&ap&e" ) );
//
//				f.pack();
//				f.setLocationRelativeTo( null );
//				f.setVisible( true );
//			}
//		});
//	}
}
