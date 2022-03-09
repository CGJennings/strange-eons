package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.event.ActionEvent;
import java.util.Locale;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACTION_COMMAND_KEY;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import resources.AcceleratorTable;
import resources.Language;
import resources.ResourceKit;

/**
 * The abstract base class for main application commands in Strange Eons.
 *
 * <p>
 * Because this class is descended from Swing {@code Action}s, it includes a
 * property map that can be used to set its name, icon, and so forth. However,
 * this class also defines several convenience methods for setting these
 * properties (all of which are {@code final}). Some of these perform additional
 * processing on the input before setting it as the value for the relevant
 * property (see, for example, {@link #setName(java.lang.String)}. In these
 * cases you can bypass the additional processing by setting the relevant
 * property value directly.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see AbstractAction#putValue(java.lang.String, java.lang.Object)
 */
@SuppressWarnings("serial")
public abstract class AbstractCommand extends AbstractAction {

    /**
     * Creates a command with no predefined name or icon.
     */
    public AbstractCommand() {
        // ensure a default value for the action command
        putValue(ACTION_COMMAND_KEY, getClass().getSimpleName());
    }

    /**
     * Defines a command using a name determined by the specified string key.
     *
     * @param nameKey the localized text key for the command name
     * @see #setNameKey
     */
    public AbstractCommand(String nameKey) {
        setNameKey(nameKey);
    }

    /**
     * Defines a command using a name determined by the specified string key and
     * an icon loaded from the given icon resource.
     *
     * @param nameKey the localized text key for the command name
     * @param iconResource the resource file to load an icon from
     * @see #setNameKey
     * @see #setIcon
     */
    public AbstractCommand(String nameKey, String iconResource) {
        setNameKey(nameKey);
        setIcon(iconResource);
    }

    /**
     * Updates the command's state. This method is called to update the
     * command's internal state. It is typically called just before a menu
     * containing the a menu item whose action is set to the command is
     * displayed. (The default implementation does nothing.)
     */
    public void update() {
    }

    /**
     * Sets the name of the action. If the name contains an ampersand (&amp;),
     * the following character will be used to set the mnemonic letter for the
     * action.
     *
     * @param name the new action name
     * @see PlatformSupport#getKeyStroke(java.lang.String)
     */
    public final void setName(String name) {
        // replace "..." with true ellipsis
        name = name.replace("...", "\u2026");

        // find mnemonic if present
        int amp = name.indexOf('&');
        if (amp >= 0) {
            StringBuilder b = new StringBuilder(name.length() - 1);
            b.append(name, 0, amp);
            boolean wasAmp = false;
            int displayedIndex = 0;
            char mnemonic = '\0';
            int i = amp;
            for (; i < name.length(); ++i) {
                char c = name.charAt(i);
                if (wasAmp) {
                    if (c == '&') {
                        b.append('&');
                    } else {
                        b.append(c);
                        if (mnemonic == '\0') {
                            mnemonic = c;
                            displayedIndex = b.length() - 1;
                        }
                    }
                    wasAmp = false;
                } else {
                    wasAmp = (c == '&');
                    if (!wasAmp) {
                        b.append(c);
                    }
                }
            }
            name = b.toString();
            if (mnemonic != '\0') {
                int vk = mnemonic;
                if (vk >= 'a' && vk <= 'z') {
                    vk -= ('a' - 'A');
                }
                putValue(MNEMONIC_KEY, vk);
                putValue(DISPLAYED_MNEMONIC_INDEX_KEY, displayedIndex);
            }
        }

        putValue(NAME, name);
    }

    /**
     * Sets the name of the command by looking up nameKey in the global
     * interface {@link Language}. The localized text for the key will be used
     * to set the name as if by calling {@link #setName}. Before using the value
     * of the key, platform-specific variants will be checked for by appending
     * "-win", "-mac", or "-other" to the key name, depending on the platform.
     * If the platform-specific key exists, it is used instead.
     *
     * <p>
     * The name key will also be used to set the ID to use when the command is
     * added to a {@link ContextBar}. The ID is generated automatically by:
     * removing "app-" from the start of the key if present, replacing hyphens
     * with underscores, and converting the key to uppercase. To change the
     * default ID, use {@code putValue( BUTTON_ID_KEY, customID )}.
     *
     * @param nameKey the name of the string key to use to set the name
     */
    public final void setNameKey(String nameKey) {
        if (nameKey == null) {
            throw new NullPointerException("nameKey");
        }

        String id = nameKey;
        if (id.startsWith("app-")) {
            id = id.substring("app-".length());
        }
        // find accelerator
        KeyStroke accelKey = AcceleratorTable.getApplicationTable().get(nameKey);
        if (accelKey == null) {
            // look for key without "app-"
            accelKey = AcceleratorTable.getApplicationTable().get(id);
        }
        if (accelKey != null) {
            setAccelerator(accelKey);
        }

        // set a default context bar ID
        id = id.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.CANADA);
        putValue(BUTTON_ID_KEY, id);
        putValue(ACTION_COMMAND_KEY, id);

        // get the localized, platform specific name
        Language ui = Language.getInterface();
        if (PlatformSupport.PLATFORM_IS_WINDOWS) {
            if (ui.isKeyDefined(nameKey + "-win")) {
                nameKey += "-win";
            }
        } else if (PlatformSupport.PLATFORM_IS_MAC) {
            if (ui.isKeyDefined(nameKey + "-mac")) {
                nameKey += "-mac";
            }
        } else if (ui.isKeyDefined(nameKey + "-other")) {
            nameKey += "-other";
        }

        setName(ui.get(nameKey).replace("...", "\u2026"));
    }

    /**
     * Returns the name of the command, or {@code null}.
     *
     * @return the command name
     */
    public final String getName() {
        return (String) getValue(NAME);
    }

    /**
     * Sets the command's icon.
     *
     * @param icon the icon to use for the command
     */
    public final void setIcon(Icon icon) {
        putValue(SMALL_ICON, icon);
    }

    /**
     * Sets the command's icon from an icon resource identifier. The icon is
     * loaded as if by {@link ResourceKit#getIcon}.
     *
     * @param iconResource the name of the icon resource to load
     */
    public final void setIcon(String iconResource) {
        if (iconResource == null) {
            setIcon((Icon) null);
            return;
        }


        setIcon(ResourceKit.getIcon(iconResource));
    }

    /**
     * Returns the command's icon.
     *
     * @return the icon associated with the command, if any
     */
    public final Icon getIcon() {
        return (Icon) getValue(SMALL_ICON);
    }

    /**
     * Sets the accelerator key for this command. If {@code null}, no
     * accelerator key is assigned.
     *
     * @param ks the key stroke to use as the accelerator for this action
     */
    public final void setAccelerator(KeyStroke ks) {
        putValue(ACCELERATOR_KEY, ks);
    }

    /**
     * Returns the accelerator key for this command, or {@code null} if the
     * command has no accelerator key.
     *
     * @return the accelerator key for the command, or {@code null}
     */
    public final KeyStroke getAccelerator() {
        return (KeyStroke) getValue(ACCELERATOR_KEY);
    }

    /**
     * The key used for storing the ID string to use for this command when it is
     * added to a context bar using {@code ContextBar.CommandButton}. If this is
     * {@code null}, then a non-{@code null} ID must be provided when creating
     * the button.
     */
    public static final String BUTTON_ID_KEY = "se#buttonID";

    /**
     * Returns a string including the command's ID and whether or not it is
     * enabled, suitable for debugging purposes.
     *
     * @return a debugging string for the command
     */
    @Override
    public String toString() {
        return getName() + "{enabled: " + isEnabled() + "}";
    }

    /**
     * If the action event contains a special command string, returns that
     * string. Otherwise, returns {@code null}.
     *
     * @param e the action event that fired the command
     * @return {@code null} if the event's action command is {@code null},
     * empty, or the default value for this command; otherwise, the value of the
     * action command
     */
    protected String getCommandString(ActionEvent e) {
        String command = e == null ? null : e.getActionCommand();
        if (command != null) {
            if (command.isEmpty() || command.equals(getValue(ACTION_COMMAND_KEY))) {
                command = null;
            }
        }
        return command;
    }
}
