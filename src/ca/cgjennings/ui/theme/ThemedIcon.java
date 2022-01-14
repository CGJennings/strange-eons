package ca.cgjennings.ui.theme;

import ca.cgjennings.ui.MultiResolutionImageResource;
import ca.cgjennings.algo.SplitJoin;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.FilteredMultiResolutionImage;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.MultiResolutionImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import resources.ResourceKit;

/**
 * An icon that whose image can change according to the installed {@link Theme}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ThemedIcon implements Icon {
    private String resource;
    private volatile FilteredMultiResolutionImage mim;
    private FilteredMultiResolutionImage dim;
    private int width, height;
    

    /**
     * Creates a new themed icon. The icon's image will normally be obtained as
     * if loading an image with the {@link ResourceKit}, but if a theme is
     * installed then the theme will be given a chance to switch the image for a
     * themed version.
     *
     * @param resource the resource identifier for the icon
     * @see Theme#applyThemeToImage(java.lang.String)
     */
    public ThemedIcon(String resource) {
        this(resource, false);
    }

    /**
     * Creates a new themed icon. The icon's image will normally be obtained as
     * if loading an image with the {@link ResourceKit}, but if a theme is
     * installed then the theme will be given a chance to switch the image for a
     * themed version. If {@code deferLoading} is {@code true}, then it will not
     * be loaded until the first time it is used. Otherwise, the image may
     * start loading immediately.
     *
     * @param resource the resource identifier for the icon
     * @param deferLoading if {@code true}, the image is loaded lazily
     * @see Theme#applyThemeToImage(java.lang.String)
     */
    public ThemedIcon(String resource, boolean deferLoading) {
        this.resource = Objects.requireNonNull(resource, "resource");

        if (!deferLoading) {
            if (Runtime.getRuntime().availableProcessors() > 1) {
                SplitJoin.getInstance().execute(this::getMultiResolutionImage);
            } else {
                getMultiResolutionImage();
            }
        }
    }
    
    /**
     * Creates a new themed icon that overrides the size of the image base image
     * resource to return the indicated size.
     * 
     * @param resource the resource identifier for the icon
     * @param width the desired icon width (for 1:1 displays)
     * @param height the desired icon height (for 1:1 displays)
     */
    public ThemedIcon(String resource, int width, int height) {
        this(resource, true);
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("bad dimensions: " + width + 'x' + height);
        }
        getMultiResolutionImage();
        this.width = width;
        this.height = height;
    }
    
    private ThemedIcon(ThemedIcon src, int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("bad dimensions: " + width + 'x' + height);
        }
        src.getMultiResolutionImage();
        mim = src.mim;
        dim = src.dim;
        resource = src.getResource();
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the resource identifier for this icon.
     *
     * @return the image resource
     */
    public String getResource() {
        return resource;
    }
    
    /**
     * Returns an image that can be used to render the icon's image at
     * multiple resolutions.
     * 
     * @return a multi-resolution version of the source image
     */
    public final AbstractMultiResolutionImage getMultiResolutionImage() {
        FilteredMultiResolutionImage mim = this.mim;
        if (mim == null) {
            synchronized(this) {
                mim = this.mim;
                if (mim == null) {
                    // create base image that returns "raw" image resources
                    MultiResolutionImageResource resIm = new MultiResolutionImageResource(resource);

                    // get the intended icon size at 1:1 scale
                    BufferedImage base = resIm.getBaseImage();
                    width = base.getWidth();
                    height = base.getHeight();

                    // wrap the base image to ensure theme is applied
                    this.mim = mim = new FilteredMultiResolutionImage(resIm) {
                        @Override
                        public Image applyEffect(Image source) {
                            Theme th = ThemeInstaller.getInstalledTheme();
                            if (th != null) {
                                source = th.applyThemeToImage(ImageUtilities.ensureIntRGBFormat(source));
                            }
                            return source;                    
                        }
                    };
                }
            }
        }
        return mim;
    }
    
    /**
     * Returns a multi-resolution image that can be used to render a version
     * of the icon's image to reflect the disabled component state.
     * 
     * @return 
     */
    public final AbstractMultiResolutionImage getDisabledMultiResolutionImage() {
        if (dim == null) {
            dim = new FilteredMultiResolutionImage(getMultiResolutionImage()) {
                @Override
                public Image applyEffect(Image source) {
                    return ImageUtilities.createDisabledImage((BufferedImage) source);
                }
            };
        }
        return dim;
    }
    
    /**
     * Returns an icon with the same base image as this icon,
     * but which renders with a different nominal size or state.
     * 
     * @param width the new width (on 1:1 displays)
     * @param height the new height (on 1:1 displays)
     * @param disabledState if true, the icon is always rendered in the
     * disabled state
     * @return an icon for the new size
     */
    public final ThemedIcon derive(int width, int height, boolean disabledState) {
        ThemedIcon derived = new ThemedIcon(this, width, height);
        if (disabledState) {
            getDisabledMultiResolutionImage();
            derived.mim = dim;
            derived.dim = dim;
        }
        return derived;
    }


    /**
     * Returns the (possibly themed) base image that will be used by the icon.
     * The base image is used by the icon when no desktop scaling is applied,
     * and so determines the base size of the icon.
     *
     * @return the image drawn by the icon at 1:1 scale
     */
    public final BufferedImage getImage() {
        if (mim == null) {
            getMultiResolutionImage();
        }
        return (BufferedImage) mim.getBaseImage();
    }

    @Override
    public int getIconWidth() {
        if (mim == null) getMultiResolutionImage();
        return width;
    }

    @Override
    public int getIconHeight() {
        if (mim == null) getMultiResolutionImage();
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (mim == null) getMultiResolutionImage();
        
        if (c != null && !c.isEnabled()) {
            g.drawImage(getDisabledMultiResolutionImage(), x, y, width, height, null);
            return;
        }
        g.drawImage(mim, x, y, width, height, null);
    }
}
