package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.apps.arkham.PortraitPanel;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.io.SEObjectInputStream;
import ca.cgjennings.io.SEObjectOutputStream;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import resources.ResourceKit;
import resources.Settings;

/**
 * A default implementation of the {@link Portrait} interface that creates a
 * key-based portrait similar to that provided by the {@link DIY} system. This
 * implementation can be used with components that have up to 32 faces.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class DefaultPortrait extends AbstractPortrait implements Serializable {

    private static final long serialVersionUID = 1_974_043_020_090_328L;
    private EnumSet<Feature> features;
    private String key;
    private double x, y, scale, angle;
    private String source = "";
    private BufferedImage image;
    private int sheetBitMap = -1;
    private GameComponent gc;
    private boolean useMinimalScale, noClip, noFill;
    private double syntheticEdgeLimit = 0d;

    private DefaultPortrait parent;
    private List<DefaultPortrait> children;

    /**
     * The maximum index of a component's sheets that a default portrait can be
     * drawn on.
     */
    public static final int MAXIMUM_SHEET_INDEX = 31;

    /**
     * Creates a default portrait instance that does not support rotation. This
     * is a cover for {@code DefaultPortrait( gc, key, false )}.
     *
     * @param gc the game component that the portrait is used with
     * @param key the key used to determine the basic properties of the portrait
     */
    public DefaultPortrait(GameComponent gc, String key) {
        this(gc, key, false);
    }

    /**
     * Creates a default portrait instance for a game component.
     *
     * @param gc the game component that the portrait is used with
     * @param key the key used to determine the basic properties of the portrait
     * @param allowRotation if {@code true}, rotating the portrait is
     * allowed
     */
    public DefaultPortrait(GameComponent gc, String key, boolean allowRotation) {
        this.gc = gc;
        this.key = key;
        if (allowRotation) {
            features = ROTATABLE_PORTRAIT_FEATURES;
        } else {
            features = STANDARD_PORTRAIT_FEATURES;
        }
        resetTransients();
    }

    /**
     * Creates a default portrait instance for a game component.
     *
     * @param gc the game component that the portrait is used with
     * @param key the key used to determine the basic properties of the portrait
     * @param portraitFeatures set of portrait features that the portrait will
     * report supporting
     * @see #setFeatures(java.util.EnumSet)
     */
    public DefaultPortrait(GameComponent gc, String key, EnumSet<Feature> portraitFeatures) {
        this(gc, key, false);
        if (portraitFeatures == null) {
            throw new NullPointerException("portraitFeatures");
        }
        features = portraitFeatures;
    }

    /**
     * Creates a default portrait instance that is a linked to another portrait,
     * called its parent. The portrait will start with the same features as its
     * parent except for the
     * {@link ca.cgjennings.apps.arkham.component.Portrait.Feature#SOURCE SOURCE}
     * feature.
     *
     * <p>
     * <b>Note:</b> Be careful not to confuse this with
     * {@link #DefaultPortrait(java.lang.String, ca.cgjennings.apps.arkham.component.DefaultPortrait)}
     * which takes arguments of the same type, but in the opposite order.
     *
     * @param parent the portrait that this portrait's source will come from
     * @param key the key used to determine the basic properties of the portrait
     */
    public DefaultPortrait(DefaultPortrait parent, String key) {
        this(parent.getGameComponent(), key, false);
        features = parent.getFeatures().clone();
        features.remove(Feature.SOURCE);
        this.parent = parent;
        if (parent.children == null) {
            parent.children = new LinkedList<>();
        }
        parent.children.add(this);
    }

    /**
     * Creates a default portrait that replaces an existing portrait by
     * switching to a new base key. This can be used to convert old component
     * files when a component's base key name changes. It is normally used in a
     * component's {@code onRead} function (for DIY components) or
     * {@code readObject} method (for compiled code).
     *
     * <p>
     * <b>Note:</b> Be careful not to confuse this with
     * {@link #DefaultPortrait(ca.cgjennings.apps.arkham.component.DefaultPortrait, java.lang.String)}
     * which takes arguments of the same type, but in the opposite order.
     *
     * @param newKey the new base key that will replace the old key
     * @param original the original portrait (typically, it has just be read
     * from a save file)
     */
    public DefaultPortrait(String newKey, DefaultPortrait original) {
        if (newKey == null) {
            throw new NullPointerException("newKey");
        }
        if (original == null) {
            throw new NullPointerException("original");
        }

        key = newKey;
        features = original.features;
        x = original.x;
        y = original.y;
        scale = original.scale;
        angle = original.angle;
        source = original.source;
        image = original.image;
        sheetBitMap = original.sheetBitMap;
        gc = original.gc;
        useMinimalScale = original.useMinimalScale;
        noClip = original.noClip;
        noFill = original.noFill;
        parent = original.parent;
        children = original.children;
    }

    /**
     * Returns the portrait that this portrait is linked to, or
     * {@code null}.
     *
     * @return the parent portrait, or {@code null} if this portrait is not
     * linked to another portrait
     * @see
     * DefaultPortrait#DefaultPortrait(ca.cgjennings.apps.arkham.component.DefaultPortrait,
     * java.lang.String)
     * @see #getChildren()
     */
    public final DefaultPortrait getParent() {
        return parent;
    }

    /**
     * Returns any portraits that are linked to this portrait as a list.
     *
     * @return a list of portraits linked to this portrait (possibly empty)
     * @see
     * DefaultPortrait#DefaultPortrait(ca.cgjennings.apps.arkham.component.DefaultPortrait,
     * java.lang.String)
     * @see #getParent()
     */
    public final List<DefaultPortrait> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns the game component that this portrait is associated with.
     *
     * @return the component that was provided when this portrait instance was
     * created
     */
    public final GameComponent getGameComponent() {
        return gc;
    }

    /**
     * Returns the base setting key that this portrait uses.
     *
     * @return the base key name that was provided when this portrait instance
     * was created
     */
    public final String getBaseKey() {
        return key;
    }

    /**
     * Sets the sheets to be updated when the portrait is modified. This is a
     * convenience method that sets the faces from a bit mask instead of an
     * array. Setting bit <i>n</i> of the bitmap indicates that face <i>n</i>
     * of the component should be updated when the portrait changes. (That is,
     * that the portrait appears on that face.)
     *
     * <p>
     * Example {@code bitmap} values:<br>
     * 0 : do not redraw any faces automatically<br>
     * 1 : front face only (this is the value of {@code 1 &lt;&lt; 0})<br>
     * 2 : back face only<br>
     * 3 : front and back faces (this is the value of
     * {@code (1 &lt;&lt; 0)|(1 &lt;&lt; 1)})<br>
     * 4 : update only the third face (this is the value of
     * {@code 1 &lt;&lt; 2})<br>
     * -1 : update all faces
     *
     * @param bitmap a bitmap indicating the indices of the faces to update
     * automatically
     * @deprecated It is preferable, and easier, to use
     * {@link #setFacesToUpdate(int[])}.
     */
    @Deprecated
    public void setFacesToUpdate(int bitmap) {
        sheetBitMap = bitmap;
        cachedClipSheetIndex = -1;
    }

    /**
     * Sets the sheets that need to be redrawn when the portrait settings
     * change. When the portrait is adjusted, the faces that use the portrait
     * must be redrawn to reflect the adjustment. By default, every face of the
     * component will be updated. Unless every face actually displays the
     * portrait in question, this slows down the application needlessly. This
     * method allows you specify which faces need to be redrawn. To use it, pass
     * an array of integers in which each integer is the index of one of the
     * component's faces (0 for the first face, 1 for the second, and so on).
     *
     * @param faces an array, each element of which is the index of a face that
     * shows this portrait
     * @throws NullPointerException if {@code faces} is {@code null}
     * @throws IllegalArgumentException if any value in the {@code faces}
     * array is outside the supported range of 32 faces
     */
    public final void setFacesToUpdate(int[] faces) {
        if (faces == null) {
            throw new NullPointerException("faces");
        }
        sheetBitMap = 0;
        for (int i = 0; i < faces.length; ++i) {
            if (faces[i] > MAXIMUM_SHEET_INDEX || faces[i] < 0) {
                throw new IllegalArgumentException("face: " + faces[i]);
            }
            sheetBitMap |= 1 << faces[i];
        }
        cachedClipSheetIndex = -1;
    }

    /**
     * Returns an array of the indices of the faces on which this portrait will
     * appear. This allows the portrait instance to avoid redrawing faces that
     * the portrait does not appear on.
     *
     * @return a bitmap indicating the indices of the faces to update
     * automatically
     * @see	#setFacesToUpdate(int[])
     */
    public int[] getFacesToUpdate() {
        int bits = sheetBitMap;
        int size = Integer.bitCount(bits);
        int[] faces = new int[size];
        for (int f = 0, i = 0; i < size; ++f) {
            if ((bits & 1) == 1) {
                faces[i++] = f;
            }
            bits >>>= 1;
        }
        return faces;
    }

    /**
     * Sets the feature set supported by the portrait. This method allows
     * subclasses to modify portrait features. It should be called only from a
     * constructor.
     *
     * @param features the set of features to support
     */
    protected final void setFeatures(EnumSet<Feature> features) {
        if (features == null) {
            throw new NullPointerException("features");
        }
        this.features = features;
    }

    /**
     * Returns the set of portrait features supported by the portrait.
     *
     * @return the portrait's capabilities
     */
    @Override
    public final EnumSet<Feature> getFeatures() {
        return features;
    }

    /**
     * Installs the default portrait, if it is not already set. The default
     * portrait is determined by looking up the base key with the suffix
     * {@code -portrait-template} appended. The value of this key must be a
     * path relative to the resources folder that identifies an image file. If
     * the key is not defined, then the value
     * {@code portraits/misc-portrait.jp2} will be used, but this should
     * only be used as a placeholder during development. By default, the pan
     * position will be set to (0,0) and the scale and rotation will be set as
     * if for any other portrait installed by calling {@link #setSource}. The
     * default panning and scale values (for the default image only) can be
     * overridden by keys with the suffix {@code -portrait-panx},
     * {@code -portrait-pany}, and {@code -portrait-scale}. The
     * default rotation (if enabled, and for both the default image and images
     * set by {@link #setSource}), can be set via the key suffix
     * {@code -portrait-rotation}. (See
     * {@link #computeDefaultImageRotation}.)
     */
    @Override
    public void installDefault() {
        Settings s = gc.getSettings();
        if (parent == null) {
            source = null;
            setSource("res://" + s.get(key + "-portrait-template", "portraits/misc-portrait.jp2"));
            source = "";
        }
        // get either this portrait's image, or the parent if linked
        BufferedImage bi = getImage();
        Point2D pan = computeDefaultImagePan(bi);
        setPanX(s.getDouble(key + "-portrait-panx", pan.getX()));
        setPanY(s.getDouble(key + "-portrait-pany", pan.getY()));
        setScale(s.getDouble(key + "-portrait-scale", computeDefaultImageScale(bi)));
        setRotation(computeDefaultImageRotation(bi));
    }

    @Override
    public void setSource(String resource) {
        if (parent != null) {
            parent.setSource(resource);
            return;
        }

        if (resource == null) {
            resource = "";
        }

        if (source == null || !source.equals(resource)) {
            if (resource.isEmpty()) {
                installDefault();
            } else {
                image = getImageFromIdentifier(resource, getClipDimensions());
                x = y = 0d;
                scale = computeDefaultImageScale(image);
                angle = computeDefaultImageRotation(image);
                source = resource;
                firePortraitModified();
                fireChildrenModified();
            }
        }

        if (children != null) {
            for (DefaultPortrait kid : children) {
                Point2D pan = kid.computeDefaultImagePan(image);
                kid.x = pan.getX();
                kid.y = pan.getY();
                kid.scale = kid.computeDefaultImageScale(image);
                kid.angle = kid.computeDefaultImageRotation(image);
            }
        }
    }

    @Override
    public void setImage(String resource, BufferedImage image) {
        if (parent != null) {
            parent.setImage(resource, image);
            return;
        }

        if (resource == null) {
            resource = "";
        }

        image = ResourceKit.prepareNewImage(image);
        this.image = image;
        x = y = 0d;
        scale = computeDefaultImageScale(image);
        angle = computeDefaultImageRotation(image);
        source = resource;
        firePortraitModified();
        fireChildrenModified();
    }

    @Override
    public String getSource() {
        if (source == null) {
            return "";
        }
        return source;
    }

    @Override
    public BufferedImage getImage() {
        return parent == null ? image : parent.image;
    }

    /**
     * Sets the synthetic edge limit ratio for the portrait. When set to a
     * non-zero value, the portrait image will be extended in every direction
     * using mirrored copies of the original image. The amount that each edge
     * is extended depends on the limit ratio. A value of 1 extends the image
     * by one full image copy on each side, while values between 0 and 1 extend
     * the image proportionally. For example, a value of 0.1 would extend the
     * image by one tenth of an image on each side. The default edge limit is 0,
     * meaning that no edges will be synthesized.
     *
     * <p>The effect of a non-zero limit is similar to creating a
     * {@link Sheet#synthesizeBleedMargin synthetic bleed margin} on a sheet.
     *
     * <p>This feature may be particularly useful for designs that:
     * <ol>
     *   <li> include a designed (not synthetic) bleed margin;
     *   <li> have a portrait area that overlaps this bleed margin; and
     *   <li> want to provide a way for users to easily extend a portrait
     *        into the bleed margin without sacrificing composition.
     * </ol>
     *
     * @param limitRatio the new limit ratio to set
     * @throws IllegalArgumentException if the limit is not between 0 and 1 inclusive
     */
    public void setSyntheticEdgeLimit(double limitRatio) {
        if(limitRatio < 0d || limitRatio > 1d) {
            throw new IllegalArgumentException("limit must be between 0 and 1 inclusive");
        }
        if(limitRatio != syntheticEdgeLimit) {
            syntheticEdgeLimit = limitRatio;
            syntheticImageCached = null;
            syntheticImageSource = null;
        }
    }

    /**
     * Returns the current synthetic edge limit ratio.
     *
     * @return the current limit, from 0 to 1
     * @see #setSyntheticEdgeLimit
     */
    public double getSyntheticEdgeLimit() {
        return syntheticEdgeLimit;
    }

    /**
     * Returns an image that can be used to paint the portrait with any
     * synthetic edges applied.
     *
     * @return an image, with edges extended according to the synthetic edge limit, or null
     */
    protected BufferedImage getSyntheticEdgeImage() {
        final BufferedImage bi = getImage();
        if(syntheticEdgeLimit == 0d || bi == null) {
            return bi;
        }
        if(bi == syntheticImageSource) {
            return syntheticImageCached;
        }

        syntheticImageSource = bi;
        final int w = bi.getWidth();
        final int h = bi.getHeight();
        final int wm = Math.min(w, (int)(w * syntheticEdgeLimit));
        final int hm = Math.min(h, (int)(h * syntheticEdgeLimit));

        syntheticImageCached = ImageUtilities.createCompatibleIntRGBFormat(bi, w + wm*2, h + hm*2);
        Graphics2D g = syntheticImageCached.createGraphics();
        try {
            g.drawImage(bi, wm - w, hm - h, wm, hm, w, h, 0, 0, null);
            g.drawImage(bi, wm, hm - h, wm + w, hm, 0, h, w, 0, null);
            g.drawImage(bi, wm + w, hm - h, wm + w + w, hm, w, h, 0, 0, null);

            g.drawImage(bi, wm - w, hm, wm, hm + h, w, 0, 0, h, null);
            g.drawImage(bi, wm + w, hm, wm + w + w, hm + h, w, 0, 0, h, null);

            g.drawImage(bi, wm - w, hm + h, wm, hm + h + h, w, h, 0, 0, null);
            g.drawImage(bi, wm, hm + h, wm + w, hm + h + h, 0, h, w, 0, null);
            g.drawImage(bi, wm + w, hm + h, wm + w + w, hm + h + h, w, h, 0, 0, null);

            g.drawImage(bi, wm, hm, null);
        } finally {
            g.dispose();
        }
        return syntheticImageCached;
    }

    private transient BufferedImage syntheticImageSource;
    private transient BufferedImage syntheticImageCached;


    @Override
    public final Point2D getPan(Point2D dest) {
        if (dest == null) {
            dest = new Point2D.Double(x, y);
        } else {
            dest.setLocation(x, y);
        }
        return dest;
    }

    @Override
    public final void setPan(Point2D pan) {
        setPanX(pan.getX());
        setPanY(pan.getY());
    }

    @Override
    public double getPanX() {
        return x;
    }

    @Override
    public double getPanY() {
        return y;
    }

    @Override
    public void setPanX(double x) {
        if (this.x != x) {
            this.x = x;
            firePortraitModified();
        }
    }

    @Override
    public void setPanY(double y) {
        if (this.y != y) {
            this.y = y;
            firePortraitModified();
        }
    }

    @Override
    public double getScale() {
        return scale;
    }

    @Override
    public void setScale(double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be positive: " + scale);
        }
        if (this.scale != scale) {
            this.scale = scale;
            firePortraitModified();
        }
    }

    @Override
    public void setRotation(double angleInDegrees) {
        if (angle != angleInDegrees) {
            angle = angleInDegrees;
            firePortraitModified();
        }
    }

    @Override
    public double getRotation() {
        return angle;
    }

    /**
     * Sets whether this portrait should use the minimum fit scale when
     * computing a default scale value for an image. When {@code false},
     * the default scale value is selected so that the {@linkplain ImageUtilities#idealCoveringScaleForImage
     * entire portrait clip region is covered}, even if that means part of the
     * portrait won't be visible. When {@code true}, the default scale
     * value is selected so that the {@linkplain ImageUtilities#idealCoveringScaleForImage
     * entire image just fits within the clip region}, even if that means that
     * part of the clip region will not be covered by the portrait. The default
     * setting is {@code false} (cover the entire clip region).
     *
     * <p>
     * <b>Note:</b> This is normally called at most once, just after the
     * portrait is first created.
     *
     * @param useMinimum if {@code true}, the minimum scale that fits the
     * portrait within the clip region will be the default scale value
     */
    public final void setScaleUsesMinimum(boolean useMinimum) {
        this.useMinimalScale = useMinimum;
        firePortraitModified();
    }

    /**
     * Returns whether the default portrait scale will cover the entire clip
     * region or fit the portrait within the clip region. See
     * {@link #setScaleUsesMinimum} for details.
     *
     * @return {@code true} if the minimal scaling method is enabled
     */
    public final boolean getScaleUsesMinimum() {
        return useMinimalScale;
    }

    /**
     * Sets whether the portrait is clipped to the clip region. The default is
     * {@code true}. When {@code false} the clip region is only used
     * to determine a portrait image's default size.
     *
     * <p>
     * <b>Note:</b> This is normally called at most once, just after the
     * portrait is first created.
     *
     * @param clipping if {@code true}, clip the portrait to the clip
     * region when it is drawn with {@link #paint}.
     */
    public final void setClipping(boolean clipping) {
        if (noClip == clipping) {
            noClip = !clipping;
            firePortraitModified();
        }
    }

    /**
     * Returns {@code true} if the portrait will be clipped to the clip
     * region. See {@link #setClipping} for details.
     *
     * @return {@code true} if clipping is enabled.
     */
    public final boolean getClipping() {
        return !noClip;
    }

    /**
     * Returns the size of the bounding rectangle of the area that the portrait
     * is drawn in on the component sheet, in the coordinate system of the
     * sheet's template. This may return {@code null} if this value is
     * unknown or inapplicable, in which case some features of the portrait
     * panel will not be available.
     *
     * @return the dimensions of the portrait's clipping rectangle
     */
    @Override
    public Dimension getClipDimensions() {
        if (gc == null || key == null) {
            return null;
        }
        Rectangle r = gc.getSettings().getRegion(key + "-portrait-clip");
        return new Dimension(r.width, r.height);
    }

    @Override
    public BufferedImage getClipStencil() {
        if (hasExplicitClip) {
            return explicitClip;
        }

        if (gc == null || key == null || noClip) {
            return null;
        }

        if (cachedClipSheetIndex < 0) {
            // flush the cache since we might need to use a different sheet
            cachedClip = null;
            // find the lowest-indexed sheet that the portrait is drawn on
            int bits = sheetBitMap;
            for (cachedClipSheetIndex = 0; cachedClipSheetIndex <= MAXIMUM_SHEET_INDEX; ++cachedClipSheetIndex) {
                if ((bits & 1) != 0) {
                    break;
                }
                bits >>= 1;
            }
        }
        // portrait doesn't appear on any sheet
        if (cachedClipSheetIndex == 0) {
            return null;
        }

        // check if the sheet actually exists
        final Sheet[] sheets = gc.getSheets();
        if (sheets == null || cachedClipSheetIndex >= sheets.length) {
            return null;
        }

        // check if the sheet's template image has changed
        final BufferedImage template = sheets[cachedClipSheetIndex].getTemplateImage();
        if (template != cachedClipTemplate) {
            cachedClipTemplate = template;
            cachedClip = null;
        }

        // nothing can possibly show through since there is no alpha channel
        if (template.getTransparency() == BufferedImage.OPAQUE) {
            return null;
        }

        if (cachedClip == null) {
            cachedClip = createStencil(template, gc.getSettings().getRegion(key + "-portrait-clip"));
        }

        return cachedClip;
    }
    private transient int cachedClipSheetIndex;
    private transient BufferedImage cachedClip;
    private transient BufferedImage explicitClip;
    private transient BufferedImage cachedClipTemplate;
    private transient boolean hasExplicitClip;

    private void resetTransients() {
        cachedClipSheetIndex = -1;
        hasExplicitClip = false;
        cachedClip = null;
        explicitClip = null;
        cachedClipTemplate = null;
        syntheticImageCached = null;
        syntheticImageSource = null;
    }

    /**
     * Sets an explicit clip stencil for this portrait. Setting an explicit clip
     * stencil permanently overrides the default clip stencil mechanism. Once an
     * explicit stencil is set, {@link #getClipStencil()} will return the most
     * recently set stencil. The stencil can be removed by explicitly setting it
     * to {@code null}.
     *
     * <p>
     * <b>Note:</b> The explicit clip stencil is <i>not</i> serialized when the
     * portrait is written to a save file. You will need to ensure that the
     * stencil is set both when the component is first created and when it is
     * read form a save file. In DIY components, a convenient way to do this is
     * to set the stencil in the appropriate painter creation function.
     *
     * @param clipStencil the clip stencil to use in the portrait's
     * {@link PortraitPanel} component
     */
    public void setClipStencil(BufferedImage clipStencil) {
        explicitClip = clipStencil;
        hasExplicitClip = true;
        cachedClip = null; // not needed anymore, so make gc-able
    }

    /**
     * Sets whether the portrait clip region will be filled with solid white
     * before painting the portrait. If set, the portrait clip region will be
     * filled in with solid white before painting the portrait. This is usually
     * turned off when the user is expected to use portraits that have
     * transparency because the portrait is painted over a background
     * illustration.
     *
     * <p>
     * <b>Note:</b> This is normally called at most once, just after the
     * portrait is first created.
     *
     * @param fill if {@code true}, the portrait background will be filled
     * in when it is drawn with {@link #paint}.
     */
    public final void setBackgroundFilled(boolean fill) {
        if (noFill == fill) {
            noFill = !fill;
            firePortraitModified();
        }
    }

    /**
     * Returns {@code true} if portrait areas will be filled with solid
     * white before painting the portrait. See {@link #setBackgroundFilled(boolean)
     * } for details.
     *
     * @return {@code true} if the portrait clip region is filled before
     * drawing the portrait
     */
    public final boolean isBackgroundFilled() {
        return !noFill;
    }

    /**
     * Paints the portrait image on a graphics context provided by a
     * {@link Sheet}.
     *
     * @param g the graphics context for painting
     * @param target the rendering target
     */
    public void paint(Graphics2D g, RenderTarget target) {
        // set bi to this portrait's image, or the parent if it is linked
        BufferedImage bi = getImage();
        if (key == null || bi == null || gc == null) {
            return;
        }

        final Settings s = gc.getSettings();

        Rectangle r = s.getRegion(key + "-portrait-clip");

        Shape oldClip = null;
        final boolean obeyClip = getClipping();
        if (obeyClip) {
            oldClip = g.getClip();
            g.setClip(r);
        }

        if (isBackgroundFilled()) {
            final Paint p = g.getPaint();
            g.setPaint(Color.WHITE);
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setPaint(p);
        }

        if(syntheticEdgeLimit > 0d) {
            // could check to see where portrait corners will end up and
            // only use extended image if the entire portrait region will not
            // be covered
            bi = getSyntheticEdgeImage();
        }

        final double scaledWidth = bi.getWidth() * scale;
        final double scaledHeight = bi.getHeight() * scale;

        final double centerX = scaledWidth / 2d;
        final double centerY = scaledHeight / 2d;

        // note that the pan value is relative to the center of the clip region;
        // i.e. a pan of (0,0) always centers the image
        final double regionX = r.getCenterX();
        final double regionY = r.getCenterY();

        if (angle != 0 && features.contains(Feature.ROTATE)) {
            AffineTransform xform = AffineTransform.getTranslateInstance(regionX - centerX + x, regionY - centerY + y);
            xform.concatenate(AffineTransform.getRotateInstance(angle * DEGREES_TO_RADIANS, centerX, centerY));
            xform.concatenate(AffineTransform.getScaleInstance(scale, scale));
            AffineTransformOp xformop = new AffineTransformOp(xform, target.getTransformInterpolationType());
            g.drawImage(bi, xformop, 0, 0);
        } else {
            final int x0 = (int) (regionX - centerX + x);
            final int y0 = (int) (regionY - centerY + y);
            final int w = (int) (scaledWidth + 0.5d);
            final int h = (int) (scaledHeight + 0.5d);
            g.drawImage(bi, x0, y0, w, h, null);
        }

        if (obeyClip) {
            g.setClip(oldClip);
        }
        
        Sheet.drawPortraitBox(g, r, this);
    }

    /** Converts angle measures and direction to match the portrait panel specs. */
    private static final double DEGREES_TO_RADIANS = -0.0174532925d;

    /**
     * Notifies the sheets indicated by {@link #getFacesToUpdate()} that they
     * must be redrawn because the portrait settings have been modified. There
     * is normally no need to call this directly, as updates are taken care of
     * automatically.
     */
    protected void firePortraitModified() {
        if (gc == null || sheetBitMap == 0 || (parent == null && image == null)) {
            return;
        }

        Sheet[] sheets = gc.getSheets();
        if (sheets == null) {
            return;
        }

        int limit = Math.min(32, sheets.length);
        int bits = sheetBitMap;
        for (int i = 0; i < limit; ++i) {
            if ((bits & (1 << i)) != 0) {
                gc.markChanged(i);
            }
        }
    }

    /**
     * Notifies any children that their attached sheets must be redrawn. This is
     * called when the parent's source image changes. There is normally no need
     * to call this directly, as updates are taken care of automatically.
     */
    protected void fireChildrenModified() {
        if (children != null) {
            for (DefaultPortrait p : children) {
                p.firePortraitModified();
            }
        }
    }

    /**
     * Returns a default pan value for an image. This is the initial pan value
     * used when the image source changes. The base class always returns (0,0).
     *
     * @param image the image to compute a default pan for
     * @return the default pan value
     */
    public Point2D computeDefaultImagePan(BufferedImage image) {
        return DEFAULT_PAN;
    }
    private static final Point2D DEFAULT_PAN = new Point2D.Double();

    /**
     * Returns a default scale value for an image. This is the initial scale
     * value used when the image source changes. The scale is determined based
     * on the {@code portrait-clip-region} for the provided portrait key.
     *
     * @param image the image to compute a default scale for
     * @return the default scale value for {@code image}
     */
    public double computeDefaultImageScale(BufferedImage image) {
        if (gc == null || key == null) {
            return 1d;
        }
        Settings s = gc.getSettings();

        Rectangle clip = s.getRegion(key + "-portrait-clip");
        double idealWidth = clip.getWidth();
        double idealHeight = clip.getHeight();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        double defScale;
        if (useMinimalScale) {
            defScale = ImageUtilities.idealBoundingScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
        } else {
            defScale = ImageUtilities.idealCoveringScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
        }
        return defScale;
    }

    /**
     * Returns a default rotation value for an image. This is the initial
     * rotation used when the image source changes.
     *
     * <p>
     * The base class implementation uses the following procedure: If the
     * portrait does not include the rotation feature, 0 is always returned.
     * Otherwise, if a key equal to the base key with the suffix
     * {@code -default-rotation} is defined and can be parsed as a double
     * value, then that value is returned. If the key does not exist or cannot
     * be parsed, 0 is returned.
     *
     * @param image the image to determine the default rotation for
     * @return the default rotation value for {@code image}
     */
    public double computeDefaultImageRotation(BufferedImage image) {
        if (gc != null && features.contains(Feature.ROTATE)) {
            final double v = gc.getSettings().getDouble(key + "-portrait-rotation");
            if (v == v) {
                return v;
            }
        }
        return 0d;
    }

    // the file format version for serialization
    private static final int VERSION = 3;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(VERSION);
        out.writeObject(gc);
        out.writeObject(features);
        out.writeObject(key);
        out.writeInt(sheetBitMap);
        out.writeBoolean(useMinimalScale);
        out.writeBoolean(noClip);
        out.writeBoolean(noFill);
        out.writeObject(source);
        out.writeDouble(x);
        out.writeDouble(y);
        out.writeDouble(scale);
        out.writeDouble(angle);
        out.writeDouble(syntheticEdgeLimit);
        out.writeObject(parent);
        out.writeObject(children);
        ((SEObjectOutputStream) out).writeImage(image);

        if (parent != null && image != null) {
            StrangeEons.log.severe("child has non-null image");
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        resetTransients();

        final int version = in.readInt();
        gc = (GameComponent) in.readObject();
        features = (EnumSet<Feature>) in.readObject();
        key = (String) in.readObject();
        sheetBitMap = in.readInt();
        useMinimalScale = in.readBoolean();
        noClip = in.readBoolean();
        noFill = in.readBoolean();
        source = (String) in.readObject();
        x = in.readDouble();
        y = in.readDouble();
        scale = in.readDouble();
        angle = in.readDouble();
        if(version >= 3) {
            syntheticEdgeLimit = in.readDouble();
        } else {
            syntheticEdgeLimit = 0d;
        }
        if (version >= 2) {
            parent = (DefaultPortrait) in.readObject();
            children = (List<DefaultPortrait>) in.readObject();
        } else {
            parent = null;
            children = null;
        }
        image = ((SEObjectInputStream) in).readImage();
    }
}
