package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.theme.Theme;
import ca.cgjennings.ui.theme.ThemeInstaller;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import javax.imageio.ImageIO;
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
    private String className;
    private String name, desc, group;
    private ThemedIcon icon, largeIcon;
    private boolean dark;

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

        this.className = className;
        collectPluginInfo();
    }

    /**
     * Returns the fully qualified name of the class that must be instantiated
     * to install this theme (that is, the {@link Theme} subclass).
     *
     * @return the name of the Theme subclass
     */
    public String getThemeClass() {
        return className;
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
     * Returns a small icon for the theme.
     *
     * @return a small icon
     */
    @Override
    public ThemedIcon getIcon() {
        return icon;
    }

    /**
     * Returns a large icon for the theme.
     *
     * @return a large icon
     */
    public ThemedIcon getLargeIcon() {
        return largeIcon;
    }
    
    /**
     * Returns a URL for the location of a screenshot image if one is available,
     * otherwise returns null.
     * 
     * @return the preview screenshot URL for the theme, or null
     */    
    public URL getScreenshotUrl() {
        try {
            Class<?> cl = Class.forName(className);            
            URL url = cl.getResource(cl.getSimpleName() + "_screenshot.png");
            return url;
        } catch (ClassNotFoundException ex) { 
            StrangeEons.log.log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * Returns a screenshot of the theme if one is available,
     * otherwise returns null.
     * 
     * @return the preview screenshot for the theme, or null
     */
    public BufferedImage getScreenshot() {
        try {
            URL url = getScreenshotUrl();
            if (url != null) {
                return ImageIO.read(url);
            }
        } catch (IOException io) {
            StrangeEons.log.log(Level.WARNING, "failed to read screenshot", io);
        }
        return null;
    }

    private void collectPluginInfo() throws PluginException {
        try {
            Theme theme = (Theme) Class.forName(className).getConstructor().newInstance();
            name = theme.getThemeName();
            if (name == null) {
                name = className;
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
            
            largeIcon = theme.getThemeIcon().mediumLarge();
            icon = largeIcon.small();            

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
            
            group = theme.getThemeGroup();
            if (group == null) {
                group = "";
            }
            
            dark = theme.isDark();
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

    @Override
    public int compareTo(InstalledBundleObject o) {
        if (!(o instanceof InstalledTheme)) {
            return super.compareTo(o);
        }
        // sort by group id, then light/dark, then name, then fall back on super
        final InstalledTheme rhs = (InstalledTheme) o;
        int cmp = group.compareTo(rhs.group);
        if (cmp != 0) return cmp;
        
        if (rhs.dark) {
            if (!dark) return -1;
        } else if (dark) {
            return 1;
        }
        
        cmp = Language.getInterface().getCollator().compare(getName(), rhs.getName());
        if (cmp == 0) {
            cmp = super.compareTo(rhs);
        }
        return cmp;
    }
}
