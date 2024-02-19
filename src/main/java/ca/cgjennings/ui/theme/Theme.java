package ca.cgjennings.ui.theme;

import ca.cgjennings.graphics.MultiResolutionImageResource;
import ca.cgjennings.ui.JHeading;
import ca.cgjennings.ui.JLinkLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import resources.Language;
import resources.Settings.Colour;

/**
 * A {@code Theme} encapsulates a UI design theme. Themes are applied while
 * installing a look and feel and given an opportunity to modify its design
 * parameters (typically by changing values in either the default or look and
 * feel {@link javax.swing.UIDefaults}).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.00RC1
 */
public abstract class Theme {

    public Theme() {
    }

    /**
     * Returns the name of this theme, as it should be presented to the user.
     * Theme names are typically short, one or two words. A localized theme name
     * should use the UI locale ({@link Language#getInterfaceLocale()}).
     *
     * <p>
     * The base class generates a name from the class name by removing "Theme"
     * from the end (if present) and inserting a space whenever an upper case
     * letter follows a lower case letter. So, for example, "TchoTchoTheme"
     * would become "Tcho Tcho".
     *
     * @return the human-friendly name of this theme
     */
    public String getThemeName() {
        String n = getClass().getSimpleName();
        if (n.endsWith("Theme")) {
            n = n.substring(0, n.length() - "Theme".length());
        }
        StringBuilder b = new StringBuilder(n.length() + 6);
        boolean wasLower = false;
        for (int i = 0; i < n.length(); ++i) {
            final char c = n.charAt(i);
            final boolean isLower = Character.isLowerCase(c);
            if (wasLower && !isLower) {
                b.append(' ');
            }
            b.append(c);
            wasLower = isLower;
        }
        return b.toString();
    }

    /**
     * An optional sentence (without final punctuation) that briefly describes
     * the theme. The base class returns {@code null} to indicate that no
     * description is provided.
     *
     * @return a theme description
     */
    public String getThemeDescription() {
        return null;
    }

    /**
     * Returns a string that names a group to which the theme belongs. Themes
     * that have the same group will be placed together in the list of
     * selectable themes. The base class returns the theme name. Themes that
     * complement each other, such as light and dark variants of the same theme,
     * should return the same group name. It is recommended to use the name of
     * the light theme as the group name for both.
     *
     * @return a non-null string naming the theme's group
     */
    public String getThemeGroup() {
        return getThemeName();
    }

    /**
     * Returns whether the theme is, on the whole, light-on-dark, similar to OS
     * "dark modes". The base class returns {@code false}.
     *
     * @return true if the theme is a "dark mode" style theme
     * @since 3.2
     */
    public boolean isDark() {
        return false;
    }

    /**
     * Returns an icon for the theme. The base class attempts to find an image
     * in the same package as the theme's class, with the same name, but with a
     * file extension of either {@code ".png"} or {@code ".jp2"} (in that
     * order). Multiple versions of the image can be provided using
     * {@code "@Nx"} file name suffixes, as in {@code MyTheme.png},
     * {@code MyTheme@2x.png}.
     *
     * @return the theme's representative image
     */
    public ThemedIcon getThemeIcon() {
        MultiResolutionImage mim = null;
        try {
            String ext = ".png";
            for (int i = 0; i < 2 && mim == null; ++i) {
                URL u = getClass().getResource(getClass().getSimpleName() + ext);
                if (u != null) {
                    mim = new MultiResolutionImageResource("/" + getClass().getName().replace('.', '/') + ext);
                }
                ext = ".jp2";
            }
        } catch (Exception e) {
        }

        // use the default fallback
        if (mim == null) {
            mim = new MultiResolutionImageResource("/ca/cgjennings/ui/theme/default.png");
        }

        return new ThemedImageIcon(mim, ThemedImageIcon.MEDIUM_LARGE, ThemedImageIcon.MEDIUM_LARGE);
    }

    /**
     * Returns the name of the theme, as given by {@link #getThemeName()}.
     *
     * @return the name of this theme
     */
    @Override
    public final String toString() {
        return getThemeName();
    }

    /**
     * Returns the first font family in a list of comma-separated font families
     * that is available on this system. If none of the families is available, a
     * standard sans-serif family is returned.
     *
     * @param families a list of comma-separated font family names
     * @return the first available entry in the list, or the standard sans-serif
     * family name
     */
    public static String findAvailableFontFamily(String families) {
        return findAvailableFontFamily(families.split("\\s*,\\s*"));
    }

    /**
     * Given an array of font family names, returns the first font family that
     * is available on this system. If none of the font families in the array
     * are available, this method returns {@code Font.SANS_SERIF}.
     *
     * @param families an array of candidate family names
     * @return the available candidate with the lowest index in
     * {@code families}, or the standard sans-serif family name
     */
    public static String findAvailableFontFamily(String[] families) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFonts = ge.getAvailableFontFamilyNames();

        for (String font : families) {
            for (String candidate : availableFonts) {
                candidate = candidate.trim();
                if (font.equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        return Font.SANS_SERIF;
    }

    /**
     * Returns the look and feel class name. Subclasses may override this to use
     * a custom look and feel.
     *
     * @return the name of a {@code LookAndFeel} subclass
     */
    public String getLookAndFeelClassName() {
        return StrangeNimbus.class.getName();
    }

    /**
     * Returns an instance of the look and feel for the theme. If
     * {@link #getLookAndFeelClassName()} returns null, then the theme installer
     * will call this instead. The base class throws an
     * {@link UnsupportedOperationException}. If the class name is null and this
     * returns null, the app will refuse to start.
     *
     * @return a non-null look-and-feel instance for the theme
     */
    public LookAndFeel createLookAndFeelInstance() {
        throw new UnsupportedOperationException("override this if getLookAndFeelClassName returns null");
    }

    /**
     * This method is called prior to instantiating the look and feel and allows
     * you to modify the UI manager's default properties.
     *
     * @param defaults the {@code UIManager} defaults
     */
    public abstract void modifyManagerDefaults(UIDefaults defaults);

    /**
     * This method is called after the look and feel has been instantiated but
     * before it has been installed and allows you to modify the look and feel's
     * default properties.
     *
     * @param defaults the Look and Feel UI defaults
     */
    public abstract void modifyLookAndFeelDefaults(UIDefaults defaults);

    /**
     * This method is called just before the look and feel is installed and just
     * after {@link #modifyLookAndFeelDefaults}. It allows you to make any final
     * changes to the look and feel before it is installed.
     *
     * <p>
     * The base class implementation does nothing.
     *
     * @param laf the look and feel to be installed
     */
    public void modifyLookAndFeel(LookAndFeel laf) {
    }

    /**
     * Called just after the look and feel has been installed. This is the final
     * step of theme installation and provides the theme with a chance to clean
     * up any resources that are no longer required.
     *
     * <p>
     * The base class implementation does nothing.
     */
    public void themeInstalled() {
    }

    /**
     * Gives the theme the opportunity to modify a image to reflect the theme.
     * The base class returns {@code source} unmodified.
     *
     * <p>
     * <strong>Important:</strong> Themes that wish to override this to modify
     * images must be sure to return a <em>copy</em> of the original image and
     * leave the {@code source} unmodified to avoid corrupting the image cache.
     *
     * @param source the image to apply themeing to
     * @return a themed copy of the image, or the original image if it is not
     * modified by the theme
     * @see #applyThemeToImage(java.lang.String)
     */
    public BufferedImage applyThemeToImage(BufferedImage source) {
        return source;
    }

    /**
     * Gives the theme the opportunity to modify a colour to reflect the theme.
     * The base class returns the input without changes, other than ensuring
     * it is a {@code Colour}.
     *
     * @param source the colour to theme
     * @return a modified colour, or the original colour
     */
    public Colour applyThemeToColor(Color source) {
        return Colour.from(source);
    }
    
    /**
     * Gives the theme the opportunity to modify a colour to refelct the theme.
     * This is a convenience that passes the ARGB value through
     * {@link #applyThemeToColor(java.awt.Color)}.
     * 
     * @param argb the colour to theme, as an int in ARGB format
     * @return a modified colour, or the original colour
     */
    public final Colour applyThemeToColor(int argb) {
        return applyThemeToColor(new Colour(argb, true));
    }

    /**
     * Returns a URL for a file that describes this theme's preferred syntax
     * highlighting theme, or null to use a default.
     *
     * @return URL of a document describing the syntax theme, or null
     */
    public URL getSyntaxThemeUrl() {
        return null;
    }

    /**
     * A UI key that controls a boolean property that affects whether document
     * tabs should switch their orientation. Set if tabs look wrong for a given
     * L&F.
     */
    public static final String ALTERNATE_DOCUMENT_TAB_ORIENTATION = "eons-alt-doc-tab-dir";
    /**
     * A UI key that contains the {@code Border} used for headings and
     * subheadings. If {@code null}, a default algorithm is used to create a
     * suitably themed border. The font for headings is based on the font used
     * for titled borders, at a larger size. (If that font is not available, the
     * default font for labels is used.)
     *
     * @see JHeading
     */
    public static final String HEADING_BORDER = "eons-heading-border";
    /**
     * A UI key that contains the {@code Color} used for the background of the
     * editor tabs.
     */
    public static final String EDITOR_TAB_BACKGROUND = "eons-editor-tab-background";
    /**
     * A UI key that contains the {@code Color} used for the background of
     * sidepanel title bars, like at the top of project views.
     */
    public static final String SIDEPANEL_TITLE_BACKGROUND = "eons-title-background";
    /**
     * A UI key that contains the {@code Color} used for the foreground of
     * sidepanel title bars, like at the top of project views.
     */
    public static final String SIDEPANEL_TITLE_FOREGROUND = "eons-title-foreground";
    /**
     * A UI key that contains the {@code Color} used for the border drawn around
     * fields that accept a file drop when files are dragged over them.
     */
    public static final String FILE_DROP_BORDER = "FileDrop.borderColor";
    /**
     * UI key for context bar background colour.
     */
    public static final String CONTEXT_BAR_BACKGROUND = "eons-cb-bg";
    /**
     * UI key for context bar foreground colour.
     */
    public static final String CONTEXT_BAR_FOREGROUND = "eons-cb-fg";
    /**
     * UI key for context bar button background colour.
     */
    public static final String CONTEXT_BAR_BUTTON_BACKGROUND = "eons-cb-btn-bg";
    /**
     * UI key for context bar button background colour when under pointer.
     */
    public static final String CONTEXT_BAR_BUTTON_ROLLOVER_BACKGROUND = "eons-cb-btn-bghi";
    /**
     * UI key for context bar button outline colour when under pointer.
     */
    public static final String CONTEXT_BAR_BUTTON_ROLLOVER_OUTLINE_FOREGROUND = "eons-cb-brdr-fg";
    /**
     * UI key for context bar button outline colour when held down.
     */
    public static final String CONTEXT_BAR_BUTTON_ARMED_OUTLINE_FOREGROUND = "eons-cb-brdr-fghi";
    /**
     * A UI key for an adjustment applied to the margin of cycle buttons to move
     * the cycle icon close to the button edge.
     */
    public static final String CYCLE_BUTTON_ICON_MARGIN_ADJUSTMENT = "eons-cycle-margin";
    /**
     * UI key for console background colour; used if painter is {@code null}.
     */
    public static final String CONSOLE_BACKROUND = "eons-conbg";
    /**
     * UI key for console background colour.
     */
    public static final String CONSOLE_BACKGROUND_PAINTER = "eons-conp";
    /**
     * UI key for console output text colour.
     */
    public static final String CONSOLE_OUTPUT = "eons-conout";
    /**
     * UI key for console error text colour.
     */
    public static final String CONSOLE_ERROR = "eons-conerr";
    /**
     * UI key for console warning text colour.
     */
    public static final String CONSOLE_WARNING = "eons-conwarn";
    /**
     * UI key for console information text colour.
     */
    public static final String CONSOLE_INFO = "eons-coninfo";
    /**
     * UI key for console text selection background colour.
     */
    public static final String CONSOLE_SELECTION_BACKGROUND = "eons-conselbg";
    /**
     * UI key for console text selection foreground colour.
     */
    public static final String CONSOLE_SELECTION_FOREGROUND = "eons-conselfg";
    /**
     * UI key for console font.
     */
    public static final String CONSOLE_FONT = "eons-confont";
    /**
     * UI key for text notes panels (e.g., project notes) background colour.
     */
    public static final String NOTES_BACKGROUND = "eons-notes-bg";
    /**
     * UI key for text notes panels (e.g., project notes) foreground colour.
     */
    public static final String NOTES_FOREGROUND = "eons-notes-fg";
    /**
     * UI key for the foreground colour of {@link JLinkLabel}s.
     */
    public static final String LINK_LABEL_FOREGROUND = "eons-link-label-fg";
    /**
     * Exterior border colour of mesage pop-ups.
     */
    public static final String MESSAGE_BORDER_EXTERIOR = "eons-mbdark";
    /**
     * Interior border colour of mesage pop-ups.
     */
    public static final String MESSAGE_BORDER_EDGE = "eons-mblight";
    /**
     * Primary border colour of mesage pop-ups.
     */
    public static final String MESSAGE_BORDER_MAIN = "eons-mbcenter";
    /**
     * Background colour of mesage pop-ups.
     */
    public static final String MESSAGE_BACKGROUND = "eons-mbbg";
    /**
     * Foreground (text) colour of mesage pop-ups.
     */
    public static final String MESSAGE_FOREGROUND = "eons-mbfg";
    /**
     * Message pop-up border for informational messages.
     */
    public static final String MESSAGE_BORDER_INFORMATION = "eons-mbinfo";
    /**
     * Message pop-up border for dialog messages.
     */
    public static final String MESSAGE_BORDER_DIALOG = "eons-mbdialog";
    /**
     * UI key for project search field background colour.
     */
    public static final String PROJECT_FIND_BACKGROUND = "eons-projfind-bg";
    /**
     * UI key for project search field foreground colour.
     */
    public static final String PROJECT_FIND_FOREGROUND = "eons-projfind-fg";
    /**
     * UI key for plug-in installation notes and catalog info background colour.
     */
    public static final String PLUGIN_README_BACKGROUND = "eons-readme-bg";
    /**
     * UI key for plug-in installation notes and catalog info foreground colour.
     */
    public static final String PLUGIN_README_FOREGROUND = "eons-readme-fg";
    /**
     * UI key for the project area properties/notes tabs background color.
     */
    public static final String PROJECT_NOTES_TAB_BACKGROUND = "eons-projnotestab-bg";
    /**
     * UI key for the background colour used for project view headers (Project
     * Files, Add New Task..., Properties/Notes, etc.)
     */
    public static final String PROJECT_HEADER_BACKGROUND = "eons-projhead-bg";
    /**
     * UI key for the background colour used for project view headers (Project
     * Files, Add New Task..., Properties/Notes, etc.)
     */
    public static final String PROJECT_HEADER_FOREGROUND = "eons-projhead-fg";
    /**
     * UI key for project search field background colour.
     */
    public static final String PREFS_BACKGROUND = "eons-pref-bg";
    /**
     * UI key for project search field foreground colour.
     */
    public static final String PREFS_FOREGROUND = "eons-pref-fg";
    /**
     * UI key for project search field foreground colour.
     */
    public static final String PREFS_HEADING = "eons-pref-head";
    /**
     * UI key for the "head banner" foreground colour. This is a rectangular
     * banner with higher contrast than a standard label-on-panel. The most
     * prominent example is the {@link MultiCloseDialog}.
     */
    public static final String HEAD_BANNER_FOREGROUND = "eons-headbanner-fg";
    /**
     * UI key for the "head banner" backround colour. This is a rectangular
     * banner with higher contrast than a standard label-on-panel.
     */
    public static final String HEAD_BANNER_BACKGROUND = "eons-headbanner-bg";

    /**
     * An image filter used to create a disabled icon from a regular icon when
     * no disabled icon is explicitly set. If set, the value must be a subclass
     * of {@link java.awt.image.BufferedImageOp}. (This includes any filter in
     * the {@code ca.cgjennings.graphics.filters} package.)
     */
    public static final String DISABLED_ICON_FILTER = "eons-difilt";

    /**
     * A helper that returns a disabled version of any icon. Used by
     * look-and-feel implementations to provide default disabled icons.
     *
     * @param component the component that the icon is for; may be null
     * @param icon the icon to convert
     * @return a version of the icon that will render in a disabled state
     */
    public static Icon getDisabledIcon(JComponent component, Icon icon) {
        // ThemedIcons know how to render themselves as disabled and
        // will check the component's enabled state
        if (icon instanceof ThemedIcon) {
            icon = component == null ? ((ThemedIcon) icon).disabled() : icon;
        } else if (icon != null) {
            // wrap the icon with one that will render it in a disabled state
            icon = new DisabledIconWrapper(icon);
        }
        return icon;
    }

    private static class DisabledIconWrapper implements Icon {

        private Icon wrapped;

        public DisabledIconWrapper(Icon toWrap) {
            wrapped = toWrap;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g;
            Composite oldComp = g2.getComposite();
            g2.setComposite(AbstractThemedIcon.DISABLED_COMPOSITE);
            wrapped.paintIcon(c, g, x, y);
            g2.setComposite(oldComp);
        }

        @Override
        public int getIconWidth() {
            return wrapped.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return wrapped.getIconHeight();
        }
    }
}
