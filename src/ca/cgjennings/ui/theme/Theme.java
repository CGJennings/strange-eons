package ca.cgjennings.ui.theme;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.prefs.Preferences;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.JHeading;
import ca.cgjennings.ui.JLinkLabel;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import resources.Language;
import resources.ResourceKit;

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
     * Returns whether the theme is, on the whole, dark-on-light, similar
     * to OS "dark modes". The base class returns {@code false}.
     *
     * @return true if the theme is a "dark mode" style theme
     * @since 3.2
     */
    public boolean isDarkOnLight() {
        return false;
    }

    /**
     * Returns a representative image for the theme. Typical images are 48 by 48
     * pixels and are framed by the theme frame image provided in the plug-in
     * authoring kit. This image may be resized as needed by Strange Eons for
     * various purposes, such as displaying the image as an icon for the theme
     * in the {@link Preferences} dialog, or in a list of installed plug-in
     * bundles.
     *
     * <p>
     * The base implementation attempts to locate an image in the same package
     * as the theme's class, with the same name, but with a file extension of
     * either ".png" or ".jp2" (in that order).
     *
     * @return the theme's representative image
     */
    public BufferedImage getThemeRepresentativeImage() {
        BufferedImage bi = null;
        try {
            String ext = ".png";
            for (int i = 0; i < 2 && bi == null; ++i) {
                URL u = getClass().getResource(getClass().getSimpleName() + ext);
                if (u != null) {
                    bi = ImageIO.read(u);
                    if (bi != null) {
                        bi = ImageUtilities.ensureIntRGBFormat(bi);
                    }
                }
                ext = ".jp2";
            }
        } catch (Exception e) {
        }
        return bi;
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
     * Gives the theme the opportunity to modify a {@link ThemedIcon} or an
     * image read using {@link ResourceKit#getThemedImage}. The base class
     * performs the following steps:
     * <ol>
     * <li> Read the image as if by calling {@link ResourceKit#getImage}.
     * <li> Call {@link #applyThemeToImage(java.awt.image.BufferedImage)} with
     * the image just read.
     * <li> Return the result.
     * </ol>
     *
     * <p>
     * Themes can customize the look of an image by swapping the image for
     * another resource, modifying the result, or both. To swap images, simply
     * compare the requested resource name to see if it is on your swap list,
     * and if so, call the super class implementation with the replacement
     * resource instead. To change the look of every image algorithmically,
     * override {@link #applyThemeToImage(java.awt.image.BufferedImage)}.
     *
     * <p>
     * Here is an example of substituting the resource identifier:
     * <pre>
     * public BufferedImage applyThemeToImage( String resource ) {
     *     // if we have an alternate version of the image stored in our
     *     // own resource folder, we will use it instead
     *     String themedResource = "mytheme/myicons/" + resource;
     *     if( ResourceKit.composeResourceURL( themedResource ) != null ) {
     *         resource = themedResource;
     *     }
     *     return super.applyThemeToImage( resource );
     * }
     * </pre>
     *
     * <p>
     * Here is an example that changes images algorithmically:
     * <pre>
     * public BufferedImage applyThemeToImage( BufferedImage source ) {
     *     BufferedImage copy = ca.cgjennings.graphics.ImageUtilities.copy( source );
     *     Graphics2D g = copy.createGraphics();
     *     try {
     *         int w = copy.getWidth(), h = copy.getHeight();
     *         g.setPaint( Color.RED );
     *         g.drawLine( 0, 0, w, h );
     *         g.drawLine( w, 0, 0, h );
     *     } finally {
     *         g.dispose();
     *     }
     *     return copy;
     * }
     * </pre>
     *
     * @param resource the image resource that is requested
     * @return the image to use to satisfy the request
     */
    public BufferedImage applyThemeToImage(String resource) {
        BufferedImage bi = ResourceKit.getImageQuietly(resource);
        if (bi != null) {
            return applyThemeToImage(bi);
        }
        StrangeEons.log.log(Level.WARNING, "failed to load image resource {0}", resource);
        return null;
    }

    /**
     * Gives the theme the opportunity to modify a image to reflect the
     * installed theme. This version of the method is called in cases where
     * there is no resource identifier available for the image. The base class
     * returns {@code source} unmodified. If you wish to modify images for
     * your theme, it is important that you make your changes to a
     * <i>copy</i> of the original image to avoid corrupting the image cache.
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
     * A UI key that controls a boolean property that affects whether document
     * tabs should switch their orientation. Set if tabs look wrong for a
     * given L&F. 
     */
    public static final String ALTERNATE_DOCUMENT_TAB_ORIENTATION = "se-alt-doc-tab-dir";
    /**
     * A UI key that contains the {@code Border} used for headings and
     * subheadings. If {@code null}, a default algorithm is used to create
     * a suitably themed border. The font for headings is based on the font used
     * for titled borders, at a larger size. (If that font is not available, the
     * default font for labels is used.)
     *
     * @see JHeading
     */
    public static final String HEADING_BORDER = "se-heading-border";
    /**
     * A UI key that contains the {@code Color} used for the background of
     * the editor tabs.
     */
    public static final String EDITOR_TAB_BACKGROUND = "se-editor-tab-background";
    /**
     * A UI key that contains the {@code Color} used for the background of
     * sidepanel title bars, like at the top of project views.
     */
    public static final String SIDEPANEL_TITLE_BACKGROUND = "se-title-background";
    /**
     * A UI key that contains the {@code Color} used for the foreground of
     * sidepanel title bars, like at the top of project views.
     */
    public static final String SIDEPANEL_TITLE_FOREGROUND = "se-title-foreground";
    /**
     * A UI key that contains the {@code Color} used for the border drawn
     * around fields that accept a file drop when files are dragged over them.
     */
    public static final String FILE_DROP_BORDER = "FileDrop.borderColor";
    /**
     * UI key for context bar background colour.
     */
    public static final String CONTEXT_BAR_BACKGROUND = "se-cb-bg";
    /**
     * UI key for context bar foreground colour.
     */
    public static final String CONTEXT_BAR_FOREGROUND = "se-cb-fg";
    /**
     * UI key for context bar button background colour.
     */
    public static final String CONTEXT_BAR_BUTTON_BACKGROUND = "se-cb-btn-bg";
    /**
     * UI key for context bar button background colour when under pointer.
     */
    public static final String CONTEXT_BAR_BUTTON_ROLLOVER_BACKGROUND = "se-cb-btn-bghi";
    /**
     * UI key for context bar button outline colour when under pointer.
     */
    public static final String CONTEXT_BAR_BUTTON_ROLLOVER_OUTLINE_FOREGROUND = "se-cb-brdr-fg";
    /**
     * UI key for context bar button outline colour when held down.
     */
    public static final String CONTEXT_BAR_BUTTON_ARMED_OUTLINE_FOREGROUND = "se-cb-brdr-fghi";
    /**
     * A UI key for an adjustment applied to the margin of cycle buttons to move
     * the cycle icon close to the button edge.
     */
    public static final String CYCLE_BUTTON_ICON_MARGIN_ADJUSTMENT = "se-cycle-margin";
    /**
     * UI key for console background colour; used if painter is
     * {@code null}.
     */
    public static final String CONSOLE_BACKROUND = "se-conbg";
    /**
     * UI key for console background colour.
     */
    public static final String CONSOLE_BACKGROUND_PAINTER = "se-conp";
    /**
     * UI key for console output text colour.
     */
    public static final String CONSOLE_OUTPUT = "se-conout";
    /**
     * UI key for console error text colour.
     */
    public static final String CONSOLE_ERROR = "se-conerr";
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
    public static final String NOTES_BACKGROUND = "se-notes-bg";
    /**
     * UI key for text notes panels (e.g., project notes) foreground colour.
     */
    public static final String NOTES_FOREGROUND = "se-notes-fg";
    /**
     * UI key for the foreground colour of {@link JLinkLabel}s.
     */
    public static final String LINK_LABEL_FOREGROUND = "se-link-label-fg";
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
    public static final String PROJECT_FIND_BACKGROUND = "se-projfind-bg";
    /**
     * UI key for project search field foreground colour.
     */
    public static final String PROJECT_FIND_FOREGROUND = "se-projfind-fg";
    /**
     * UI key for the project area properties/notes tabs background color.
     */
    public static final String PROJECT_NOTES_TAB_BACKGROUND = "se-projnotestab-bg";

    /**
     * UI key for the background colour used for project view headers
     * (Project Files, Add New Task..., Properties/Notes, etc.)
     */
    public static final String PROJECT_HEADER_BACKGROUND = "se-projhead-bg";

    /**
     * UI key for the background colour used for project view headers
     * (Project Files, Add New Task..., Properties/Notes, etc.)
     */
    public static final String PROJECT_HEADER_FOREGROUND = "se-projhead-fg";

    /**
     * UI key for project search field background colour.
     */
    public static final String PREFS_BACKGROUND = "se-pref-bg";
    /**
     * UI key for project search field foreground colour.
     */
    public static final String PREFS_FOREGROUND = "se-pref-fg";
    /**
     * UI key for project search field foreground colour.
     */
    public static final String PREFS_HEADING = "se-pref-head";
    /**
     * UI key for the "head banner" foreground colour. This is a rectangular
     * banner with higher contrast than a standard label-on-panel.
     * The most prominent example is the {@link MultiCloseDialog}.
     */
    public static final String HEAD_BANNER_FOREGROUND = "se-headbanner-fg";
    /**
     * UI key for the "head banner" backround colour. This is a rectangular
     * banner with higher contrast than a standard label-on-panel.
     */
    public static final String HEAD_BANNER_BACKGROUND = "se-headbanner-bg";

    /**
     * An image filter used to create a disabled icon from a regular icon when
     * no disabled icon is explicitly set. If set, the value must be a subclass
     * of {@link java.awt.image.BufferedImageOp}. (This includes any filter in
     * the {@code ca.cgjennings.graphics.filters} package.)
     */
    public static final String DISABLED_ICON_FILTER = "eons-difilt";

    /**
     * A UI key for a boolean value that, if true and supported by the selected
     * LaF class, will override the LaF icons for JOptionPane-style messages
     * with a common set of icons.
     */
    public static final String OVERRIDE_LAF_MESSAGE_ICONS = "override-icons";
}
