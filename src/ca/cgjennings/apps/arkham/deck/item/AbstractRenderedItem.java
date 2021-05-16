package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.ViewQuality;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.TurnAndFlipFilter;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * An item whose visual representation is generated from a bitmap. The bitmap
 * may or may not be re-rendered to suit the resolution. To create an rendered
 * item subclass, you must override
 * {@link #renderImage(ca.cgjennings.apps.arkham.sheet.RenderTarget, double)} to
 * provide an appropriate image when required. If a change to the item state
 * requires that the image be re-rendered, you must call
 * {@link #clearCachedImages()} and then {@link #itemChanged()} so that the
 * image will be updated on the next paint. (For example, an item that displays
 * an image from a file would call this if the user selected a new file.)
 *
 * <p>
 * By default, rendered items will transparently use a MIP map technique to
 * cache results for different zoom levels in a page view. The MIP map cache
 * works by rendering the image at a high resolution and then creating scaled
 * versions (that is, scaling down the initial high resolution rendering rather
 * than re-rendering the image for each resolution). The pre-scaled images
 * improve image quality and drawing performance at common view scales. You can
 * disable this cache (see {@link #setMipMapCacheEnabled(boolean)}), but be
 * aware that you will then be asked to re-render the image every time the view
 * scale changes unless you implement your own caching method.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractRenderedItem extends AbstractFlippableItem {

    private static final long serialVersionUID = 6_385_369_211_586_957_987L;
    protected transient BufferedImage render;

    /**
     * Creates a new rendered item.
     */
    public AbstractRenderedItem() {
        setMipMapCacheEnabled(isMipMapCacheEnabledByDefault());
    }

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        if (mipMap != null) {
            // When using the MIP map cache, the returned image may be different
            // than the requested resolution, so we need to query the cache
            // for the true resolution
            BufferedImage bi = mipMap.getOrientedImage(target, renderResolutionHint);
            double trueResolution = mipMap.getTrueResolution();
            double scale = 72d / trueResolution;

            AffineTransform at = AffineTransform.getTranslateInstance(xOff, yOff);
            at.concatenate(AffineTransform.getScaleInstance(scale, scale));
            g.drawImage(bi, at, null);
        } else {
            AffineTransform at = AffineTransform.getTranslateInstance(xOff, yOff);
            at.concatenate(AffineTransform.getScaleInstance(72d / renderResolutionHint, 72d / renderResolutionHint));
            g.drawImage(getOrientedImage(target, renderResolutionHint), at, null);
        }
    }

    @Override
    public void prepareToPaint(RenderTarget target, double renderResolutionHint) {
        if (mipMap != null) {
            mipMap.getOrientedImage(target, renderResolutionHint);
        } else {
            getOrientedImage(target, renderResolutionHint);
        }
    }

    /**
     * Return the width of this item. The result is corrected for the item's
     * current orientation. Subclasses should override {@link #getUprightWidth}
     * to provide the item's width in its upright orientation.
     *
     * @return the width of this item, in points
     */
    @Override
    public double getWidth() {
        if ((getOrientation() & 1) == 0) {
            return getUprightWidth();
        } else {
            return getUprightHeight();
        }
    }

    /**
     * Return the width of this item in its standard, upright orientation.
     *
     * @return the width of the unoriented item
     */
    protected abstract double getUprightWidth();

    /**
     * Return the height of this item. The result is corrected for the item's
     * current orientation. Subclasses should override {@link #getUprightWidth}
     * to provide the item's hright in its upright orientation.
     *
     * @return the height of this item, in points
     */
    @Override
    public double getHeight() {
        if ((orientation & 1) == 0) {
            return getUprightHeight();
        } else {
            return getUprightWidth();
        }
    }

    /**
     * Return the height of this item in its standard, upright orientation.
     *
     * @return the height of the unoriented item
     */
    protected abstract double getUprightHeight();

    @Override
    public Icon getThumbnailIcon() {
        if (cachedThumbnailIcon == null) {

            BufferedImage bi;
            if (mipMap != null) {
                // if using MM, may as well pre-load the cache
                bi = mipMap.getOrientedImage(
                        ViewQuality.get().ordinal() < ViewQuality.HIGH.ordinal() ? RenderTarget.FAST_PREVIEW : RenderTarget.PREVIEW,
                        36d
                );
            } else {
                bi = renderImage(RenderTarget.FAST_PREVIEW, 36d);
            }

            BufferedImage o = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = o.createGraphics();
            final int INSET = 2;
            final int INSET_ICON_SIZE = ICON_SIZE - INSET * 2;

            double scale = ImageUtilities.idealCoveringScaleForImage(
                    INSET_ICON_SIZE, INSET_ICON_SIZE,
                    bi.getWidth(), bi.getHeight()
            );

            bi = ImageUtilities.resample(bi, (float) scale);
            int x = INSET + (INSET_ICON_SIZE - bi.getWidth()) / 2;
            int y = INSET + (INSET_ICON_SIZE - bi.getHeight()) / 2;

            g.drawImage(bi, x, y, null);
            g.dispose();
            cachedThumbnailIcon = new ImageIcon(o);
        }
        return cachedThumbnailIcon;
    }
    private transient ImageIcon cachedThumbnailIcon;

    /**
     * Ensure that the thumbnail image is regenerated the next time
     * {@link #getThumbnailIcon} is called.
     */
    protected void clearCachedIcon() {
        cachedThumbnailIcon = null;
    }

    /**
     * Render an image based on the target and resolution hints. This is called
     * whenever the item's image is required. The image should always be
     * rendered in the upright, unmirrored position. An oriented image will be
     * generated automatically as needed.
     *
     * <p>
     * It is recommended that subclasses cache results if possible, in which
     * case {@link #clearCachedImages()} should be overridden to clear the
     * cached result before calling the {@code super} implementation.
     *
     * @param target the target type for rendering
     * @param resolution the resolution that the item should be rendered at
     * @return an image representing the item's current state
     */
    protected abstract BufferedImage renderImage(RenderTarget target, double resolution);

    /**
     * Returns an image of this item that is correctly oriented. (If the card is
     * rotated or flipped, the image will be flipped and/or rotated
     * accordingly.) This method calls {@link #renderImage} in order to obtain
     * the upright version of the image, and {@link #createOrientedImage} to
     * change the orientation if required.
     *
     * <p>
     * The oriented result is cached, and on future calls the cached image will
     * be returned if the orientation is the same and {@link #renderImage}
     * returns the same image instance as the previous call.
     *
     * @param target the rendering target
     * @param resolution the desired resolution of the image, in pixels per inch
     * @return an oriented image of this item
     */
    protected BufferedImage getOrientedImage(RenderTarget target, double resolution) {
        final int orient = getOrientation();
        BufferedImage source = renderImage(target, resolution);

        // Our cached copies of the oriented source image and original source
        // image are stored in SoftRefs to allow GCing in response to memory demand;
        // these two blocks extract the cached objects if they are still
        // available.
        BufferedImage cachedOrientedImage = null;
        if (cachedOrientedImageRef != null) {
            cachedOrientedImage = cachedOrientedImageRef.get();
        }

        BufferedImage cachedSourceImage = null;
        if (cachedSourceImageRef != null) {
            cachedSourceImage = cachedSourceImageRef.get();
        }

        // Check if the orientation or resolution changed, or if one of our
        // cached objects was GC'd to free up memory.
        if (orient != cachedOrientation || cachedOrientedResolution != resolution || source != cachedSourceImage || cachedOrientedImage == null) {
            cachedSourceImageRef = new SoftReference<>(source);
            cachedOrientedImage = createOrientedImage(source, orient);
            cachedOrientedImageRef = new SoftReference<>(cachedOrientedImage);
            cachedOrientation = orient;
            cachedOrientedResolution = resolution;
        }

        return cachedOrientedImage;
    }
    private transient int cachedOrientation = -1;
    private transient SoftReference<BufferedImage> cachedSourceImageRef = null;
    private transient SoftReference<BufferedImage> cachedOrientedImageRef = null;
    private transient double cachedOrientedResolution = -1;

    /**
     * Clear any cached image data held by this object. The base class clears
     * any cached oriented results and the MIP map cache (if enabled).
     * Subclasses should override this (calling
     * {@code super.clearCachedImages}) if the {@link #renderImage}
     * implementation caches results.
     */
    protected void clearCachedImages() {
        cachedOrientation = -1;
        cachedSourceImageRef = null;
        cachedOrientedImageRef = null;
        if (mipMap != null) {
            mipMap.clear();
        }
        clearCachedIcon();
    }

    /**
     * Given an upright, unmirrored source image, this method returns an
     * oriented version of the image. If the requested orientation is upright,
     * the original image is returned.
     *
     * @param source the rendered image
     * @param orientation the required orientation
     * @return a version of the source image rotated or flipped into the
     * required orientation
     */
    protected BufferedImage createOrientedImage(BufferedImage source, int orientation) {
        if (orientation == ORIENT_UPRIGHT) {
            return source;
        }

        if (orientationFilter == null) {
            orientationFilter = new TurnAndFlipFilter(orientation);
        } else {
            orientationFilter.setOrientation(orientation);
        }

        return orientationFilter.filter(source, null);
    }
    private transient TurnAndFlipFilter orientationFilter;

    private static final int ABSTRACT_RENDERED_ITEM_VERSION = 1;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(ABSTRACT_RENDERED_ITEM_VERSION);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        clearCachedImages();
        setMipMapCacheEnabled(isMipMapCacheEnabledByDefault());
    }

    /**
     * Manages a set of MIP maps used by {@link #paint} to draw rendered content
     * with fewer scaling artifacts.
     */
    private class MipMap {

        public MipMap() {
        }

        @SuppressWarnings("unchecked")
        private final SoftReference<BufferedImage>[] cache = new SoftReference[MIP_MAP_RESOLUTIONS.length];

        public void clear() {
            for (int i = 0; i < cache.length; ++i) {
                cache[i] = null;
            }
        }

        /**
         * Returns an oriented image at or above the requested resolution.
         *
         * @param target the output target
         * @param resolution the requested resolution
         * @return a rendering with the correct orientation and resolution equal
         * or greater than the requested resolution
         */
        public BufferedImage getOrientedImage(RenderTarget target, double resolution) {
            // Decide if we will use the MIP map to satisfy this request:
            // we use regular rendering in the following cases:
            //   - the request is for PRINT or EXPORT
            //   - the requested resolution is higher than our maximum resolution
            //     and the page item says it wants to do the drawing in this case
            boolean noMipMap = (target == RenderTarget.PRINT) || (target == RenderTarget.EXPORT);
            if (!noMipMap && (resolution - MIP_MAP_RESOLUTIONS[0]) > 0.5d) {
                noMipMap = !scaleMipMapUpAtHighZoom(target, resolution);

                // At this point we know the requested resolution exceeds the MIP map maximum
                // and that we are going to scale up an image. If there is already
                // an image with higher res than the MIP map cache available
                // from the oriented image cache, we can scale it up instead.
                if (!noMipMap && cachedOrientedImageRef != null && (cachedOrientedResolution - MIP_MAP_RESOLUTIONS[0]) > 0.5d) {
                    BufferedImage cachedOrientedImage = cachedOrientedImageRef.get();
                    if (cachedOrientedImage != null) {
                        lastRes = cachedOrientedResolution;
                        return cachedOrientedImage;
                    }
                }
            }

            if (noMipMap) {
                lastRes = resolution;
                return AbstractRenderedItem.this.getOrientedImage(target, resolution);
            }

            // Clear cache if rendering target or orientation changed
            // Thought: would it confuse the user too much if the cache was
            // only cleared if the target improves? (No feedback on quality change.)
            if (lastTarget != target || lastOrientation != getOrientation()) {
                clear();
                lastTarget = target;
                lastOrientation = getOrientation();
            }

            // find the index of the resolution just equal or greater than the requested resolution
            int mipdex = MAX_MIPDEX;
            for (; mipdex >= 0; --mipdex) {
                if (MIP_MAP_RESOLUTIONS[mipdex] >= resolution) {
                    break;
                }
            }
            if (mipdex < 0) {
                mipdex = 0;
            }

            // check for a cached image
            lastRes = MIP_MAP_RESOLUTIONS[mipdex];
            BufferedImage bi = get(mipdex);

            // not in cache, create
            if (bi == null) {
                // find the first larger mip-map that is available
                int srcdex = mipdex - 1;
                for (; srcdex >= 0; --srcdex) {
                    bi = get(srcdex);
                    if (bi != null) {
                        break;
                    }
                }

                // if the highest res image is unavailable, we need
                // to render and cache it
                if (srcdex < 0) {
                    srcdex = 0;
                    bi = AbstractRenderedItem.this.getOrientedImage(target, MIP_MAP_RESOLUTIONS[0]);
                    set(0, bi);
                }

                // now fill in the intervening slots quickly using fast resampling
                // instead of rendering, up to and incuding the desired mipdex slot
                // (if mipdex is 0 then the slot is filled in and this loop is skipped)
                Object hint = (target == RenderTarget.FAST_PREVIEW)
                        ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                        : RenderingHints.VALUE_INTERPOLATION_BICUBIC;
                for (int i = srcdex + 1; i <= mipdex; ++i) {
                    float factor = ((float) (MIP_MAP_RESOLUTIONS[i] / MIP_MAP_RESOLUTIONS[i - 1]));
                    try {
                        bi = ImageUtilities.resample(bi, factor, false, hint, hint);
                    } catch (OutOfMemoryError oom) {
                        // if there isn't enough memory for all the subimages,
                        // use the last one we created successfully, which still
                        // has a reference to it in bi
                        lastRes = MIP_MAP_RESOLUTIONS[i - 1];
                        break;
                    }
                    set(i, bi);
                }
            }

            return bi;
        }

        /**
         * Returns the true resolution of the last image that was returned from
         * {@link #getOrientedImage}.
         *
         * @return the actual resolution of the last result
         */
        public double getTrueResolution() {
            return lastRes;
        }

        private double lastRes;

        /**
         * Sets the cached mip-map image at the specified index.
         *
         * @param mipdex the index in {@link #MIP_MAP_RESOLUTIONS} that matches
         * the image's resolution
         * @param image the image to cache
         */
        private void set(int mipdex, BufferedImage image) {
            cache[mipdex] = new SoftReference<>(image);
        }

        /**
         * Get a cached mip-map image, or {@code null}.
         *
         * @param mipdex the mip-map index
         * @return the cached image
         */
        private BufferedImage get(int mipdex) {
            BufferedImage bi = null;
            SoftReference<BufferedImage> cached = cache[mipdex];
            if (cached != null) {
                bi = cached.get();
            }
            return bi;
        }

        private RenderTarget lastTarget;
        private int lastOrientation = -1;
    }
    private static final double[] MIP_MAP_RESOLUTIONS = new double[]{300d, 150d, 75d, 37d};
    private static final int MAX_MIPDEX = MIP_MAP_RESOLUTIONS.length - 1;

    /**
     * Sets whether the built-in MIP map mechanism is enabled. By default, the
     * rendered item will transparently create and use MIP mapped versions of
     * the bitmap to improve painting speed and target. Subclasses may call this
     * method to disable the MIP map mechanism. To do so, you must call
     * {@code setMipMapCacheEnabled( false )} in <b>both</b> the
     * constructor and {@link #readImpl}.
     *
     * @param enable if {@code true}, the MIP map painting mechanism is
     * enabled
     * @see #isMipMapCacheEnabled()
     * @since 3.0
     */
    protected void setMipMapCacheEnabled(boolean enable) {
        if (enable) {
            if (mipMap == null) {
                mipMap = new MipMap();
            }
        } else {
            mipMap = null;
        }
    }

    /**
     * Returns {@code true} if the MIP map mechanism is currently enabled
     * on this item.
     *
     * @return {@code true} if MIP mapping is enabled
     * @see #setMipMapCacheEnabled(boolean)
     * @since 3.0
     */
    protected final boolean isMipMapCacheEnabled() {
        return mipMap != null;
    }

    /**
     * Returns {@code true} if the MIP map cache should be enabled by
     * default for this item type. Subclasses can override this to disable this
     * mechanism. (The base class returns {@code true}.)
     *
     * @return {@code true} if the MIP map cache should automatically be
     * enabled during construction or deserialization
     */
    protected boolean isMipMapCacheEnabledByDefault() {
        return true;
    }

    /**
     * Returns {@code true} if the highest resolution MIP map image should
     * be scaled up at high zoom levels. This method is called when the MIP map
     * cache is enabled but the current zoom level of the view requires an image
     * with higher resolution than the maximum resolution used by the MIP map
     * cache. If it returns {@code true}, then the highest resolution MIP
     * map image will be scaled up to the requested resolution. If it returns
     * {@code false}, then a new rendering will be requested at the exact
     * resolution needed, just as if the MIP map cache was disabled.
     *
     * <p>
     * It is generally faster and consumes less memory to scale up the MIP map
     * image than to render a new image. However, scaling up the MIP map image
     * reduces visual fidelity at high zoom levels. By overriding this method,
     * subclasses can optimize this trade-off for their particular item type.
     *
     * <p>
     * The value returned by the base class is unspecified because it may change
     * in future versions.
     *
     * @param target the rendering target of the request
     * @param resolution the desired image resolution
     * @return whether the rendering engine should scale up the highest
     * resolution MIP map image or render the image at the exact resolution
     * required
     * @see #setMipMapCacheEnabled(boolean)
     */
    protected boolean scaleMipMapUpAtHighZoom(RenderTarget target, double resolution) {
        if (target != RenderTarget.FAST_PREVIEW) {
            boolean niceDraw = (Math.abs(resolution - 450d) < 0.5d) || (Math.abs(resolution - 600d) < 0.5d);
            return !niceDraw;
        }
        return true;
    }

    private transient MipMap mipMap;

    @Override
    public PageItem clone() {
        final boolean hasMipMap = isMipMapCacheEnabled();
        AbstractRenderedItem ari = (AbstractRenderedItem) super.clone();
        if (hasMipMap) {
            ari.setMipMapCacheEnabled(false);
            ari.setMipMapCacheEnabled(true);
        }
        ari.clearCachedImages();
        return ari;
    }
}
