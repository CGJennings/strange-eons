package ca.cgjennings.apps.arkham.dialog.prefs;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.diy.SBCheckBox;
import ca.cgjennings.apps.arkham.diy.SBDropDown;
import ca.cgjennings.apps.arkham.diy.SBIntSpinner;
import ca.cgjennings.apps.arkham.diy.SBTextField;
import ca.cgjennings.apps.arkham.diy.SettingBackedControl;
import ca.cgjennings.apps.arkham.plugins.PluginContext;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.JHelpButton;
import ca.cgjennings.ui.JLinkLabel;
import ca.cgjennings.ui.JTip;
import ca.cgjennings.ui.theme.Theme;
import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import resources.ResourceKit;
import resources.Settings;
import se.datadosen.component.RiverLayout;

/**
 * A preference category allows the user to manage a set of user preferences
 * linked to user settings. Commonly used controls can be added easily using the
 * provided methods, or you can add custom panels and override the appropriate
 * methods to manage loading and changing settings keys.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class FillInPreferenceCategory implements PreferenceCategory {

    private String title;
    private ThemedIcon icon;

    private Settings settings;
    private JPanel panel;

    private int indent = 0;
    private int indentHgapOffset;

    private ButtonGroup currentGroup = new ButtonGroup();

    private HashMap<String, SettingBackedControl> auto;
    private HashMap<String, String> resetKeys;

    private static final int ICON_SIZE = 48;

    /**
     * Create a new preferences category that will use plain settings keys (that
     * is, the exact keys that are specified).
     *
     * @param title the localized name of the category
     * @param iconImage an optional image for the category; should be themed
     * @see ResourceKit#getThemedImage(java.lang.String)
     */
    public FillInPreferenceCategory(String title, BufferedImage iconImage) {
        this(null, title, iconImage);
    }

    /**
     * Create a new preferences category that will use plain settings keys (that
     * is, the exact keys that are specified).
     *
     * @param title the localized name of the category
     * @param iconResource a resource file that represents the icon
     */
    public FillInPreferenceCategory(String title, String iconResource) {
        this(null, title, iconResource);
    }
    
    /**
     * Create a new preferences category that will use plain settings keys (that
     * is, the exact keys that are specified).
     *
     * @param title the localized name of the category
     * @param icon the category icon
     */
    public FillInPreferenceCategory(String title, Icon icon) {
        this(null, title, icon);
    }

    /**
     * Create a new preferences panel that will use decorated settings keys that
     * are unique to a given plug-in.
     *
     * @param context a plug-in context for the plug-in for which decorated
     * settings keys should be used
     * @param title the localized name of the category
     * @param iconImage an optional image for the category; should be themed
     * @see ResourceKit#getThemedImage(java.lang.String)
     */
    public FillInPreferenceCategory(PluginContext context, String title, BufferedImage iconImage) {
        this(null, title, imageToIcon(iconImage));
    }
    
    private static ThemedIcon imageToIcon(BufferedImage im) {
        if (im == null) return null;
        if (im.getWidth() <= ICON_SIZE && im.getHeight() <= ICON_SIZE) {
            im = ImageUtilities.center(im, ICON_SIZE, ICON_SIZE);
        }
        return new ThemedImageIcon(im).derive(ICON_SIZE, ICON_SIZE);
    }

    /**
     * Create a new preferences panel that will use decorated settings keys that
     * are unique to a given plug-in.
     *
     * @param context a plug-in context for the plug-in for which decorated
     * settings keys should be used
     * @param title the localized name of the category
     * @param iconResource a resource file that represents the icon
     */
    public FillInPreferenceCategory(PluginContext context, String title, String iconResource) {
        this(context, title, (iconResource == null || iconResource.isEmpty()) ? null : new ThemedImageIcon(iconResource));
    }
    
    /**
     * Create a new preferences panel that will use decorated settings keys that
     * are unique to a given plug-in.
     *
     * @param context a plug-in context for the plug-in for which decorated
     * settings keys should be used
     * @param title the localized name of the category
     * @param icon the category icon
     */
    public FillInPreferenceCategory(PluginContext context, String title, Icon icon) {
        this.title = Objects.requireNonNull(title, "title");
        settings = context == null ? Settings.getUser() : context.getSettings();
        this.icon = icon == null ? new BlankIcon(ICON_SIZE) : ThemedIcon.create(icon).derive(ICON_SIZE);
        
        panel = new JPanel();
        RiverLayout layout = new RiverLayout();
        indentHgapOffset = layout.getHgap();
        panel.setLayout(layout);        
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    /**
     * Apply theme colours to preference controls.
     *
     * @param c the component to style
     */
    static void style(JComponent c) {
        c.setOpaque(true);
        c.setBackground(UIManager.getColor(Theme.PREFS_BACKGROUND));
        if (c instanceof JLinkLabel || c instanceof JHelpButton) {
            return;
        }
        c.setForeground(UIManager.getColor(Theme.PREFS_FOREGROUND));
        if (c instanceof JLabel) {
            JLabel label = (JLabel) c;
            if (label.getFont() != null && label.getFont().getSize2D() > defaultLabelFontSize) {
                label.setForeground(UIManager.getColor(Theme.PREFS_HEADING));
            }
        } else if (c instanceof JScrollPane) {
            c.setOpaque(true);
        }

        for (int i = 0; i < c.getComponentCount(); ++i) {
            Component kid = c.getComponent(i);
            if (kid instanceof JPanel || kid instanceof JLabel || kid instanceof JCheckBox || kid instanceof JRadioButton || kid instanceof JScrollPane) {
                style((JComponent) kid);
            }
        }
    }
    private static final float defaultLabelFontSize;

    static {
        Font f = new JLabel().getFont();
        if (f == null) {
            defaultLabelFontSize = 12f;
        } else {
            defaultLabelFontSize = f.getSize2D();
        }
    }

    /**
     * Add a control to the list of automatically managed controls.
     *
     * @param key
     * @param control
     */
    private void addAuto(String key, SettingBackedControl control) {
        if (auto == null) {
            auto = new HashMap<>();
        }
        auto.put(key, control);
    }

    /**
     * Register a settings key. If the value of the key changes, the user will
     * be informed that they must restart Strange Eons for changes to take
     * effect.
     *
     * @param key the name of the settings key to track
     */
    public void addResetKey(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (resetKeys == null) {
            resetKeys = new HashMap<>();
        }
        resetKeys.put(key, null);
    }

    /**
     * A convenience method for adding multiple keys at once.
     *
     * @param keys
     */
    public void addResetKeys(String... keys) {
        for (String key : keys) {
            addResetKey(key);
        }
    }

    /**
     * If the user applies the settings changes, this will be called after the
     * settings are updated to check if a restart is required. The base
     * implementation checks to see if any of the keys registered with
     * {@link #addResetKey} have changed value, and returns {@code true} if they
     * have.
     *
     * @return {@code true} if the application must be restarted for changes to
     * take effect
     */
    @Override
    public boolean isRestartRequired() {
        if (resetKeys == null) {
            return false;
        }

        boolean willReset = false;
        for (String key : resetKeys.keySet()) {
            final String oldVal = resetKeys.get(key);
            final String newVal = settings.get(key);
            boolean triggered = false;
            if (oldVal == null && newVal != null) {
                triggered = true;
            }
            if (newVal == null && oldVal != null) {
                triggered = true;
            }
            if (oldVal != null && !oldVal.equals(newVal)) {
                triggered = true;
            }
            if (triggered) {
                willReset = true;
                if (!StrangeEons.log.isLoggable(Level.INFO)) {
                    break;
                }
                StrangeEons.log.log(Level.INFO, "restart key triggered: {0}", key);
            }
        }
        return willReset;
    }

    /**
     * This method is called when the preferences dialog is about to be
     * displayed in order to initialize the controls with the current settings.
     * The base class will initialize any of the standard controls that have
     * been added with a non-{@code null} key. It also records the initial value
     * of any reset keys that have been added so that they can be compared to
     * their new value later on to determine if a reset is required. Subclasses
     * may override this to handle loading of custom controls.
     */
    @Override
    public void loadSettings() {
        if (auto != null) {
            for (String key : auto.keySet()) {
                SettingBackedControl sbc = auto.get(key);
                sbc.fromSetting(settings.get(key));
            }
        }
        if (resetKeys != null) {
            for (String key : resetKeys.keySet()) {
                resetKeys.put(key, settings.get(key));
            }
        }
    }

    /**
     * This method is called when the user accepts the changes in the
     * preferences dialog. It updates the settings values with the new values
     * set in the category panel. The base class implementation handles
     * processing of any built-in controls that were added.
     */
    @Override
    public void storeSettings() {
        if (auto != null) {
            for (String key : auto.keySet()) {
                SettingBackedControl sbc = auto.get(key);
                String val = sbc.toSetting();
                if (val == null) {
                    settings.reset(key);
                } else {
                    settings.set(key, val);
                }
            }
        }
    }

    /**
     * Returns the settings instance used by this category to
     * {@linkplain #loadSettings() load} and {@linkplain #storeSettings() store}
     * settings.
     */
    public final Settings getSettings() {
        return settings;
    }

    /**
     * Adds a heading label to the panel.
     */
    public JLabel heading(String text) {
        JLabel label = makeHeading(text, 3f);
        String constr = panel.getComponentCount() > 0 ? "p" : "br";
        panel.add(label, constr);
        indent = 1;
        join = false;
        return label;
    }

    /**
     * Adds a subheading label to the panel.
     */
    public JLabel subheading(String text) {
        JLabel label = makeHeading(text, 1f);
        addIndent(1);
        panel.add(label, "");
        indent = 2;
        join = false;
        return label;
    }

    /**
     * Adds a plain label to the panel.
     */
    public JLabel label(String text) {
        JLabel label = new JLabel(text);
        addIndent(indent);
        panel.add(label, "");
        return label;
    }

    /**
     * Adds a small note label to the panel.
     */
    public JLabel note(String text) {
        JLabel label = new JLabel(text);
        Font f = label.getFont();
        label.setFont(f.deriveFont(f.getSize2D() - 1f));
        addIndent(indent);
        panel.add(label, "");
        return label;
    }

    /**
     * Adds a pop-up tip with the specified text to the panel.
     *
     * @param tipText the help text to display
     * @return the component that was added
     */
    public JTip tip(String tipText) {
        JTip tip = new JTip();
        tip.setTipText(tipText);
        addIndent(indent);
        panel.add(tip, "");
        return tip;
    }

    /**
     * Adds a checkbox at the current indent level that is mapped to key. If
     * invert is {@code true}, then the box will be checked when the setting is
     * false instead of true.
     *
     * @param key the setting key name
     * @param text the text for the checkbox
     * @param invert whether to invert the relationship with the setting value
     * @return the checkbox that was added
     */
    public SBCheckBox addCheckBox(String key, String text, boolean invert) {
        SBCheckBox b = new SBCheckBox(text, invert);
        addAuto(key, b);
        addIndent(indent);
        panel.add(b, "");
        return b;
    }

    /**
     * Adds a dropdown menu button at the current indent level that is mapped to
     * key.
     *
     * @param key the setting key name
     * @param labels the list of labels to show in the drop down
     * @param values the values to assign to the key for each label
     * @return the dropdown menu that was added
     */
    public SBDropDown<String> addDropDown(String key, String[] labels, String[] values) {
        @SuppressWarnings("unchecked")
        SBDropDown<String> b = new SBDropDown(labels, values);
        addAuto(key, b);
        addIndent(indent);
        panel.add(b, "");
        return b;
    }

    /**
     * Adds a ranged integer control at the current indent level that is mapped
     * to key.
     *
     * @param key the setting key name
     * @param min the minimum value
     * @param max the maximum value
     * @param stepSize the adjustment step size
     * @return the control that was added
     */
    public SBIntSpinner addRange(String key, String label, int min, int max, int stepSize) {
        SBIntSpinner b = new SBIntSpinner(min, max, stepSize);
        b.getEditor().setOpaque(false);
        addAuto(key, b);
        addIndent(indent);

        if (label != null) {
            JLabel jlab = new JLabel(label);
            jlab.setLabelFor(b);
            panel.add(jlab, "");
        }

        panel.add(b, "");
        return b;
    }

    /**
     * Adds a text field with an optional label.
     *
     * @param key the settings key to use
     * @param label the label text to use, or {@code null}
     * @param cols the number of columns for the field
     * @return the text field
     */
    public JTextField addField(String key, String label, int cols) {
        SBTextField tf = new SBTextField(cols);
        addAuto(key, tf);
        addIndent(indent);

        if (label != null) {
            JLabel jlab = new JLabel(label);
            jlab.setLabelFor(tf);
            panel.add(jlab, "");
        }

        panel.add(tf, "");

        return tf;
    }

    /**
     * Adds a button to the panel that will call the supplied action listener
     * when pressed.
     *
     * @param label the label text for the button
     * @param pressHandler a listener to call when the button is pressed
     * @return returns the button that was added
     */
    public JButton addButton(String label, ActionListener pressHandler) {
        JButton b = new JButton(label);
        if (pressHandler != null) {
            b.addActionListener(pressHandler);
        }
        addIndent(indent);
        panel.add(b, "");
        return b;
    }

    /**
     * Starts a new button group. Any radio buttons that are added are
     * automatically placed in the last-started group. Only one button in a
     * given group can be selected at any one time.
     */
    public void startNewButtonGroup() {
        currentGroup = new ButtonGroup();
    }

    /**
     * Adds a new radio button that will be a member of the current button
     * group.
     *
     * @param label the label text for the button
     * @param pressHandler an optional listener to call when the button is
     * pressed; generally every button in the group would share a single
     * listener
     * @return the new radio button
     * @see #startNewButtonGroup()
     */
    public JRadioButton addRadioButton(String label, ActionListener pressHandler) {
        if (currentGroup == null) {
            startNewButtonGroup();
        }
        JRadioButton b = new JRadioButton(label);
        if (pressHandler != null) {
            b.addActionListener(pressHandler);
        }
        currentGroup.add(b);
        addIndent(indent);
        panel.add(b, "");
        return b;
    }

    /**
     * Adds any setting backed control, mapped to key.
     *
     * @param key the setting key name
     * @param control the control to add
     * @return the control
     */
    public SettingBackedControl add(String key, SettingBackedControl control) {
        addAuto(key, control);
        addIndent(indent);
        panel.add((Component) control, "");
        return control;
    }

    /**
     * Adds a custom control that will be managed by the caller. It is up to the
     * subclass to map preference settings to and from the control.
     *
     * @param uc the unmanaged control to add
     * @return the unmanaged control
     */
    public JComponent addUnmanagedControl(JComponent uc) {
        addIndent(indent);
        panel.add(uc, "");
        return uc;
    }

    /**
     * Adds a help icon that links to a help page. If the help page looks like a
     * {@code http[s]} URL, the button will link to that page. Otherwise it is
     * assumed to the base name of documentation page.
     *
     * @param helpPage the page to open when the icon is clicked
     * @param label an optional label for the help button
     * @see StrangeEons#getUrlForDocPage(java.lang.String)
     */
    public void addHelp(String helpPage, String label) {
        if (helpPage == null) {
            throw new NullPointerException("helpPage");
        }
        JHelpButton btn = new JHelpButton();
        btn.setHelpPage(helpPage);
        if (label != null && !label.isEmpty()) {
            btn.setText(label);
        }
        addUnmanagedControl(btn);
    }

    @Deprecated
    /**
     * @deprecated
     */
    public void addHelp(String helpPage, String label, boolean isDocPage) {
        addHelp(helpPage, label);
    }

    /**
     * Adds an icon that displays pop-up help text when hovered over.
     *
     * @param popupText the text to display
     */
    public void addTip(String popupText) {
        JTip tip = new JTip(popupText);
        addUnmanagedControl(tip);
    }

    private void addIndent(int level) {
        if (!join) {
            int size = level * 16 - indentHgapOffset;
            JLabel lab = new JLabel();
            lab.setBorder(BorderFactory.createEmptyBorder(1, size, 0, 0));
            panel.add(lab, "br");
        } else {
            join = false;
        }
    }

    /**
     * Causes the next control to be added to be placed on the same line as the
     * previous control, instead of on a new line.
     */
    public void join() {
        join = true;
    }

    private boolean join = false;

    private JLabel makeHeading(String text, float sizeAdj) {
        JLabel label = new JLabel(text);
        Font f = label.getFont();
        label.setFont(f.deriveFont(Font.BOLD, f.getSize2D() + sizeAdj));
        return label;
    }

    /**
     * Indent subsequent controls one additional level. Indentation is reset
     * automatically when a heading or subheading is added.
     */
    public void indent() {
        ++indent;
    }

    /**
     * Reduce indentation of subsequent controls one level.
     */
    public void unindent() {
        indent = Math.max(0, --indent);
    }

    @Override
    public JPanel getPanel() {
        style(panel);
        return panel;
    }

    @Override
    public String toString() {
        return title;
    }
}
