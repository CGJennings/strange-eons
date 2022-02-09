package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.MultiResolutionImageResource;
import ca.cgjennings.ui.theme.Theme;
import ca.cgjennings.ui.theme.ThemeInstaller;
import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.IOException;
import javax.swing.Icon;
import resources.CoreComponents.MissingCoreComponentException;
import resources.Language;

/**
 * Provides the information needed to allow the user to select a theme, and for
 * the {@link ThemeInstaller} to install a selected theme. These objects are
 * usually created by the {@link BundleInstaller} when the application starts.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class InstalledTheme extends InstalledBundleObject {

    private static final int ICON_SIZE_LARGE = 48;
    private static final int ICON_SIZE_SMALL = 16;
    private String id;

    private String name, desc;
    private MultiResolutionImage image;
    private ThemedIcon icon, largeIcon;

    /**
     * Creates a new {@link InstalledTheme} instance that describes the theme
     * with the given className.
     *
     * @param bundle the bundle that the theme class is stored in, or
     * {@code null} for built in themes
     * @param className the class used to instantiate the theme
     * @throws PluginException if the theme is unavailable or there is an
     * exception while extracting the theme information
     * @throws NullPointerException if className is {@code null}
     */
    public InstalledTheme(PluginBundle bundle, String className) throws IOException, PluginException {
        super(bundle);
        if (className == null) {
            throw new NullPointerException("className");
        }

        this.id = className;
        collectPluginInfo();
    }

    /**
     * Returns the fully qualified name of the class that must be instantiated
     * to install this theme (that is, the {@link Theme} subclass).
     *
     * @return the name of the Theme subclass
     */
    public String getThemeClass() {
        return id;
    }

    /**
     * Returns the theme's human-friendly name.
     *
     * @return a short name for the theme
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return desc;
    }
    
    /**
     * Returns the theme's representative image.
     * @return the representative image for the theme
     */
    public MultiResolutionImage getImage() {
        return image;
    }

    /**
     * Returns a small icon for the theme based upon the representative image.
     *
     * @return a small icon
     */
    @Override
    public ThemedIcon getIcon() {
        if (icon == null) {
            icon = new ThemedImageIcon(image, ICON_SIZE_SMALL, ICON_SIZE_SMALL);
        }
        return icon;
    }

    /**
     * Returns a large icon for the theme based upon the representative image.
     *
     * @return a large icon
     */
    public Icon getLargeIcon() {
        if (largeIcon == null) {
            largeIcon = new ThemedImageIcon(image, ICON_SIZE_LARGE, ICON_SIZE_LARGE);
        }
        return largeIcon;
    }

    private void collectPluginInfo() throws PluginException {
        try {
            Theme theme = (Theme) Class.forName(id).getConstructor().newInstance();
            name = theme.getThemeName();
            if (name == null) {
                name = id;
            }
            image = theme.getThemeImage();
            if (image == null) {
                BufferedImage bim = theme.getThemeRepresentativeImage();
                if (bim != null) {
                    image = new BaseMultiResolutionImage(bim);
                }
            }
            if (image == null) {
                image = new MultiResolutionImageResource("/ca/cgjennings/ui/theme/default.png");
            }
            // make sure that the L&F class needed by the theme exists
            // (this will eliminate themes based on Nimbus when Java 6u10 is not installed)
            try {
                if (theme.getLookAndFeelClassName() != null) {
                    Class.forName(theme.getLookAndFeelClassName(), false, ClassLoader.getSystemClassLoader());
                }
            } catch (Throwable t) {
                throw new MissingCoreComponentException("theme's underlying Look and feel unavailable: " + theme.getLookAndFeelClassName());
            }

            desc = theme.getThemeDescription();
            if (desc == null) {
                PluginRoot root = getPluginRoot();
                if (root != null) {
                    desc = root.getLocalizedClientProperty("description");
                }
                if (desc == null) {
                    desc = Language.string("sd-l-theme-desc");
                }
            }
        } catch (Throwable t) {
            markFailed();
            throw new PluginException("unable to create theme instance", t);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final InstalledTheme other = (InstalledTheme) obj;
        return !((this.name == null) ? (other.name != null) : !this.name.equals(other.name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    boolean isLoaded() {
        return true;
    }
}
