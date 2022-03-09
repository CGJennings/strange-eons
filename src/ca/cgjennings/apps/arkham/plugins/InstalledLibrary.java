package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import static ca.cgjennings.apps.arkham.plugins.PluginRoot.*;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.Icon;
import resources.Language;
import resources.ResourceKit;

/**
 * Represents an installed library. Generally, much less information is
 * available about libraries than about other kinds of bundle objects because
 * library bundles do not contain specific plug-ins.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see BundleInstaller#getInstalledLibraries()
 */
public final class InstalledLibrary extends InstalledBundleObject {

    private String name;
    private String desc;
    private Icon icon;
    private BufferedImage image;

    InstalledLibrary(PluginBundle bundle) throws IOException {
        super(bundle);

        PluginRoot root = getPluginRoot();
        if (root != null) {
            name = root.getLocalizedClientProperty(CLIENT_KEY_NAME);
            desc = root.getLocalizedClientProperty(CLIENT_KEY_DESCRIPTION);
            String imageSrc = root.getClientProperty(CLIENT_KEY_IMAGE);
            if (imageSrc != null) {
                image = ResourceKit.getImage(imageSrc);
                icon = ImageUtilities.createIconForSize(image, 18);
            }
        }

        // place defaults in any fields we couldn't fill in
        if (name == null) {
            // create name from file name
            name = bundle.getFile().getName();
            // make the name more human-friendly:
            // - remove core- and .selibrary, insert spaces into camel case names
            if (name.startsWith("core-")) {
                name = name.substring("core-".length());
            }
            boolean lastWasLowerCase = false;
            StringBuilder b = new StringBuilder(name.length());
            for (int i = 0; i < name.length(); ++i) {
                char c = name.charAt(i);
                if (c == '.') {
                    break;
                }
                if (lastWasLowerCase && (Character.isUpperCase(c) || Character.isDigit(c))) {
                    b.append(' ');
                }
                b.append(c);
                lastWasLowerCase = Character.isLowerCase(c);
            }
            name = b.toString();
        }

        if (desc == null) {
            desc = Language.string("plug-lib-desc");
        }

        if (icon == null) {
            icon = PluginBundle.getIcon(bundle.getFile(), true);
        }

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public ThemedIcon getIcon() {
        if (libraryIcon == null) {
            try {
                PluginRoot root = getBundle().getPluginRoot();
                if (root != null) {
                    String image = root.getClientProperty(PluginRoot.CLIENT_KEY_IMAGE);
                    if (image != null) {
                        libraryIcon = new ThemedImageIcon(image).small();
                    }
                }
            } catch (IOException ex) {
                StrangeEons.log.log(Level.WARNING, "could not read plug-in root");
            }
            if (libraryIcon == null) {
                libraryIcon = ResourceKit.getIcon("library").small();
            }
        }
        return libraryIcon;
    }
    private ThemedIcon libraryIcon;

    @Override
    public String toString() {
        return name;
    }

    @Override
    boolean isLoaded() {
        return true;
    }
}
