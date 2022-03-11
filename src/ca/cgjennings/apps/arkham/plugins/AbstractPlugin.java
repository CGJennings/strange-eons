package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import java.net.URL;
import resources.ResourceKit;

/**
 * Simplifies writing a plug-in by providing default implementations of all
 * methods. Subclasses will typically override at least {@link #getPluginName()},
 * {@link #getPluginDescription()}, and
 * {@link #showPlugin(ca.cgjennings.apps.arkham.plugins.PluginContext, boolean)}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.00
 */
public abstract class AbstractPlugin implements Plugin {

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns {@code true}.
     */
    @Override
    public boolean initializePlugin(PluginContext context) {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation does nothing.
     */
    @Override
    public void unloadPlugin() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns the name of the class, with spaces
     * inserted between character pairs whenever a lower case letter is followed
     * by an upper case letter. For example:
     * <pre>
     * MyNiftyThing -&gt; My Nifty Thing
     * </pre>
     */
    @Override
    public String getPluginName() {
        String base = getClass().getSimpleName();
        StringBuilder b = new StringBuilder(base.length() + 3);
        boolean wasCap = true;
        for (int i = 0; i < base.length(); ++i) {
            char c = base.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!wasCap) {
                    b.append(' ');
                }
                wasCap = true;
            } else {
                wasCap = false;
            }
            b.append(c);
        }
        return b.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns {@code null}.
     */
    @Override
    public String getPluginDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns {@code 1}.
     */
    @Override
    public float getPluginVersion() {
        return 1f;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns {@link Plugin#ACTIVATED}.
     */
    @Override
    public int getPluginType() {
        return ACTIVATED;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation does nothing.
     */
    @Override
    public void showPlugin(PluginContext context, boolean show) {

    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns {@code false}.
     */
    @Override
    public boolean isPluginShowing() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns {@code true}.
     */
    @Override
    public boolean isPluginUsable() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>The abstract base class looks for an image in the same package and with
     * the same base file name as the class file.
     * 
     * @return an icon representing the plugin
     */
    @Override
    public ThemedIcon getPluginIcon() {
        if (pluginIcon == null) {
            String base = getPluginIconBaseName();
            
            for (int i=0; i<ICON_SUFFIXES.length; ++i) {
                URL url = ResourceKit.class.getResource(base + ICON_SUFFIXES[i]);
                if (url != null) {
                    pluginIcon = new ThemedImageIcon(base + ICON_SUFFIXES[i], ThemedImageIcon.SMALL);
                }
            }
            if (pluginIcon == null) {
                if (getPluginType() == EXTENSION) {
                    pluginIcon = ResourceKit.getIcon("extension").small();
                } else {
                    pluginIcon = ResourceKit.getIcon("plugin").small();
                }
            }
        }
        return pluginIcon;
    }
    private ThemedIcon pluginIcon = null;
    
    /**
     * Returns a base class path to use to locate the default icon,
     * including a base file name but no extension.
     * 
     * @return package path to the class or script to use to locate an icon
     */
    protected String getPluginIconBaseName() {
        return '/' + getClass().getName().replace('.', '/');
    }
    private static final String[] ICON_SUFFIXES = new String[]{".png", ".jp2"};
    
    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns {@code null}.
     */
    @Override
    public String getDefaultAcceleratorKey() {
        return null;
    }
}
