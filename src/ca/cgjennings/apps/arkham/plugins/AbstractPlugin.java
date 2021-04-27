package ca.cgjennings.apps.arkham.plugins;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

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
     * The abstract implementation returns <code>true</code>.
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
     * The abstract implementation returns <code>null</code>.
     */
    @Override
    public String getPluginDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns <code>1</code>.
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
     * The abstract implementation returns <code>false</code>.
     */
    @Override
    public boolean isPluginShowing() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns <code>true</code>.
     */
    @Override
    public boolean isPluginUsable() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation looks in the folder where the class is
     * located for an image with the same name as the class but with a
     * <tt>.png</tt> or <tt>.jp2</tt> extension. If found, it attempts to read
     * an image from the file. If successful, the image is returned; otherwise,
     * <code>null</code> is returned.
     */
    @Override
    public BufferedImage getRepresentativeImage() {
        BufferedImage image = null;
        for (int i = 0; i < REPRESENTATIVE_IMAGE_SUFFIXES.length && image == null; ++i) {
            URL url = getClass().getResource(getClass().getSimpleName() + REPRESENTATIVE_IMAGE_SUFFIXES[i]);
            if (url != null) {
                try {
                    image = ImageIO.read(url);
                } catch (IOException ex) {
                }
            }
        }
        return image;
    }
    private static final String[] REPRESENTATIVE_IMAGE_SUFFIXES = new String[]{".png", ".jp2"};

    /**
     * {@inheritDoc}
     * <p>
     * The abstract implementation returns <code>null</code>.
     */
    @Override
    public String getDefaultAcceleratorKey() {
        return null;
    }
}
