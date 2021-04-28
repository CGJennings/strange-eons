package ca.cgjennings.platform;

import java.awt.AWTEvent;
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * This utility class provides methods that help integrate an application into
 * the native operating system more cleanly. It is pure Java code.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class PlatformSupport {

    private PlatformSupport() {
    }

    /**
     * A convenience method for <code>makeAgnosticDialog( dialog, true,
     * designedOK, designedCancel )</code>, which assumes that the dialog
     * buttons are in "Windows order" (OK on the left, Cancel on the right).
     *
     * @param dialog the dialog or other control to be reordered
     * @param designedOK the button used as the OK button in the design
     * @param designedCancel the button used as the Cancel button in the design
     * @return the button that will be used as the OK button
     */
    public static JButton makeAgnosticDialog(AgnosticDialog dialog, JButton designedOK, JButton designedCancel) {
        return makeAgnosticDialog(dialog, true, designedOK, designedCancel);
    }

    /**
     * Initialize an <code>AgnosticDialog</code> by swapping the OK and Cancel
     * buttons, if appropriate, and attaching action listeners to the
     * {@link AgnosticDialog#handleOKAction} and
     * {@link AgnosticDialog#handleCancelAction} methods.
     * <p>
     * This method should generally be applied during the window's construction,
     * before it is first made visible.
     * <p>
     * By default, this method will automatically swap the text, icon, mnemonic,
     * displayed mnemonic index, and default button status of the two buttons if
     * they need to be swapped. No other properties are changed. For complete
     * control over the actions taken when a swap occurs, implement
     * {@link QueriedAgnosticDialog} instead. This is a subinterface of
     * <code>AgnosticDialog</code>.
     * <p>
     * <b>Notes:</b>
     * <ol>
     * <li>You must ensure that this method is only called once for a given
     * <code>AgnosticDialog</code> during its lifetime, or the button order will
     * be inconsistent and the action events will be called multiple times.
     * <li>Although referred to in the general sense as the "OK" button, the
     * text of this button should name the specific action it will perform.
     * </ol>
     *
     * @param dialog the dialog or other control to be reordered
     * @param isInOKCancelOrder <code>true</code> is the dialog is designed with
     * <code>designedOK</code> on the left
     * @param designedOK the button that was designed to be the "OK" (accept,
     * commit) button
     * @param designedCancel the button that was designed to be the "Cancel"
     * button
     * @return the button that will be used as the OK button
     * @throws IllegalArgumentException if either button is <code>null</code> or
     * if they refer to the same object
     */
    public static JButton makeAgnosticDialog(final AgnosticDialog dialog, boolean isInOKCancelOrder, JButton designedOK, JButton designedCancel) {
        if (designedOK == null) {
            throw new IllegalArgumentException("null designedOK");
        }
        if (designedCancel == null) {
            throw new IllegalArgumentException("null designedCancel");
        }
        if (designedOK == designedCancel) {
            throw new IllegalArgumentException("designedOK and designedCancel must be different");
        }
        JButton OK = designedOK, Cancel = designedCancel;
        boolean swap = getAgnosticOK(isInOKCancelOrder, designedOK, designedCancel) == designedCancel;

        // swap buttons and content if required
        if (swap) {
            OK = designedCancel;
            Cancel = designedOK;

            // do the default swap operations for non-QueriedAgnosticDialogs
            if (!(dialog instanceof QueriedAgnosticDialog)) {
                String text = OK.getText();
                OK.setText(Cancel.getText());
                Cancel.setText(text);

                Icon icon = OK.getIcon();
                OK.setIcon(Cancel.getIcon());
                Cancel.setIcon(icon);

                int mnemonic = OK.getMnemonic();
                int okIndex = OK.getDisplayedMnemonicIndex();
                int cancelIndex = Cancel.getDisplayedMnemonicIndex();
                OK.setMnemonic(Cancel.getMnemonic());
                Cancel.setMnemonic(mnemonic);
                OK.setDisplayedMnemonicIndex(cancelIndex);
                Cancel.setDisplayedMnemonicIndex(okIndex);

                if (OK.isDefaultButton()) {
                    OK.getRootPane().setDefaultButton(Cancel);
                } else if (Cancel.isDefaultButton()) {
                    Cancel.getRootPane().setDefaultButton(OK);
                }
            }
        }

        // allow QueriedAgnosticDialog to initialize itself instead of us
        // doing the swapping
        if (dialog instanceof QueriedAgnosticDialog) {
            ((QueriedAgnosticDialog) dialog).performAgnosticButtonSwap(swap, OK, Cancel);
        }

        // create action listeners for AgnosticDialog implemenation
        OK.addActionListener(new ActionListener() {

            AgnosticDialog target = dialog;

            @Override
            public void actionPerformed(ActionEvent e) {
                target.handleOKAction(e);
            }
        });

        Cancel.addActionListener(new ActionListener() {

            AgnosticDialog target = dialog;

            @Override
            public void actionPerformed(ActionEvent e) {
                target.handleCancelAction(e);
            }
        });

        // add an Esc action to cancel entire dialog
        if (dialog instanceof JDialog) {
            JDialog jdlg = (JDialog) dialog;
            jdlg.getRootPane().registerKeyboardAction(dialog::handleCancelAction,
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        }

        return OK;
    }

    /**
     * Return the button that will be (or is) the OK button in an agnostic
     * dialog. This can be called before or after {@link #makeAgnosticDialog}.
     *
     * @param isInOKCancelOrder <code>true</code> is the dialog is designed with
     * <code>designedOK</code> on the left
     * @param designedOK the button that was designed as the "OK" (accept,
     * commit) button
     * @param designedCancel the button that was designed as the "Cancel" button
     * @return the button that will represent the OK button in an agnostic
     * dialog
     */
    public static JButton getAgnosticOK(boolean isInOKCancelOrder, JButton designedOK, JButton designedCancel) {
        boolean swap = !isAgnosticOKInFirstPosition();

        if (!isInOKCancelOrder) {
            swap = !swap;
        }
        return swap ? designedCancel : designedOK;
    }

    /**
     * Return the button that will be (or is) the Cancel button in an agnostic
     * dialog. This can be called before or after {@link #makeAgnosticDialog}.
     *
     * @param isInOKCancelOrder <code>true</code> is the dialog is designed with
     * <code>designedOK</code> on the left
     * @param designedOK the button that was designed as the "OK" (accept,
     * commit) button
     * @param designedCancel the button that was designed as the "Cancel" button
     * @return the button that will represent the OK button in an agnostic
     * dialog
     */
    public static JButton getAgnosticCancel(boolean isInOKCancelOrder, JButton designedOK, JButton designedCancel) {
        return getAgnosticOK(isInOKCancelOrder, designedOK, designedCancel) == designedOK
                ? designedCancel : designedOK;
    }

    /**
     * Returns <code>true</code> if the OK button should be left (ahead) of the
     * Cancel button on this platform.
     *
     * @return <code>true</code> if OK comes before Cancel; <code>false</code>
     * if it comes after
     */
    public static boolean isAgnosticOKInFirstPosition() {
        return !PLATFORM_IS_OSX;
    }

    /**
     * Call this method before opening any Swing windows to install the native
     * look and feel. If they are available in the classpath, various
     * OS-specific optimizations and patches may also be installed.
     */
    public static void installNativeLookAndFeel() {
        // set custom system properties
        String[] newProperties = {
            "apple.laf.useScreenMenuBar", "true",};

        Properties p = System.getProperties();
        for (int i = 0; i < newProperties.length; i += 2) {
            p.setProperty(newProperties[i], newProperties[i + 1]);
        }
        System.setProperties(p);

        // set look & feel
        java.awt.Toolkit.getDefaultToolkit().setDynamicLayout(true);
        JFrame.setDefaultLookAndFeelDecorated(true);
        try {
            // install native l&f
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            Logger.getGlobal().log(Level.SEVERE, "can't install native L&F", e);
        }
    }

    // TODO: centralize control variant client properties for different L&Fs
    /**
     * Parse a string to create a <code>KeyStroke</code> appropriate as an
     * accelerator for the native OS. The string must have the following syntax:
     * <pre>
     *    &lt;modifiers&gt;* (&lt;typedID&gt; | &lt;pressedReleasedID&gt;)
     *
     *    modifiers := menu | shift | control | ctrl | meta | alt | altGraph
     *    typedID := typed &lt;typedKey&gt;
     *    typedKey := string of length 1 giving Unicode character.
     *    pressedReleasedID := (pressed | released) key
     *    key := KeyEvent key code name, without the "VK_" prefix.
     * </pre> If typed, pressed or released is not specified, pressed is
     * assumed.
     * <p>
     * The special pseudo-modifier "menu" will be converted into the correct
     * menu accelerator key for the native platform. For example, "menu X" will
     * be treated as "ctrl X" on the Windows platform, but as "meta X" (which is
     * Command key + X) on Max OS X platform. Note that there is no way to
     * determine from the returned <code>KeyStroke</code> instance whether the
     * "menu" modifier was used or not.
     *
     * @param stroke a string formatted as above
     * @return a <code>KeyStroke</code> object representing the specified key
     * event
     */
    @SuppressWarnings("deprecation")
    public static KeyStroke getKeyStroke(String stroke) {
        if (stroke == null) {
            return null;
        }

        Matcher menuPatternMatcher = menuKeyPattern.matcher(stroke);
        stroke = menuPatternMatcher.replaceAll(menuKeyReplacement);
        return KeyStroke.getKeyStroke(stroke);
    }
    private static final Pattern menuKeyPattern = Pattern.compile("menu\\s", Pattern.CASE_INSENSITIVE);
    private static final String menuKeyReplacement = getMenuKeyReplacement();

    @SuppressWarnings("deprecation")
    private static String getMenuKeyReplacement() {
        //
        // Uses deprecated ALT_MASK instead of ALT_DOWN_MASK, etc.,
        // but until there is something like getMenuShortcutKeyMaskEx
        // we have no choice.
        //
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        String actualKey;
        switch (mask) {
            case InputEvent.ALT_MASK:
                actualKey = "alt ";
                break;
            case InputEvent.META_MASK:
                actualKey = "meta ";
                break;
            case InputEvent.SHIFT_MASK:
                actualKey = "shift ";
                break;
            default:
                actualKey = "ctrl ";
                if (mask != InputEvent.CTRL_MASK) {
                    System.err.println("Warning: unknown menu accelerator mask; using CTRL instead");
                }
        }
        return actualKey;
    }

    /** True if the JVM is running on an Apple macOS operating system. */
    public static final boolean PLATFORM_IS_OSX;
    /** True if the JVM is running on a Windows operating system. */
    public static final boolean PLATFORM_IS_WINDOWS;
    /**
     * True if running on a non-Windows, non-maxOS operating system.
     * Typically this means a Unix-like operating system such as Linux.
     */
    public static final boolean PLATFORM_IS_OTHER;

    static {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        PLATFORM_IS_WINDOWS = os.contains("windows");
        PLATFORM_IS_OSX = os.contains("mac") || os.contains("darwin");
        PLATFORM_IS_OTHER = !(PLATFORM_IS_WINDOWS || PLATFORM_IS_OSX);
    }
}
