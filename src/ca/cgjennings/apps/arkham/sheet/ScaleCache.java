package ca.cgjennings.apps.arkham.sheet;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

/**
 * A cache that stores a pre-scaled version of a source image. This can be used
 * to draw static images on a sheet. Example use:
 * <pre>
 * // When setting up the sheet:
 * sc = new ScaleCache( imageToCache );
 * // When drawing the sheet, instead of drawing the original image:
 * sc.draw( g, this, target, x, y );
 * </pre>
 *
 * <p>
 * Drawing an image via a scale cache can produce nicer results than drawing the
 * image directly via {@code g.drawImage} in a scaled graphics context,
 * particularly when the image must be scaled to less than half its true size.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public final class ScaleCache {

    private BufferedImage source;
    private SoftReference<BufferedImage> cache;
    private int sw, sh, cw = -1, ch;
    private int dw, dh;
    // the ratio of the template image size (dw) to the true source image size (sw)
    // this is used to adjust the sheet's scaling factor by an appropriate amount
    private float templateToSourceRatio;

    /**
     * Creates a scale cache. The source image is assumed to have the same
     * resolution as the template image.
     *
     * @param source the image to scale for drawing
     */
    public ScaleCache(BufferedImage source) {
        this.source = source;
        sw = source.getWidth();
        sh = source.getHeight();
        dw = sw;
        dh = sh;
        templateToSourceRatio = 1f;
    }

    /**
     * Creates a scale cache. The source image may have a different resolution
     * than the template image. The specified width and height define the size
     * that image should be drawn at on the template image. For example, if you
     * have an image {@code im} that is 300 ppi, but the template image is
     * 150 ppi, then you would use:<br>
     * {@code new ScaleCache( im, im.getWidth()/2, im.getHeight()/2 )}.
     *
     * @param source the image to scale for drawing
     * @param widthInTemplatePixels the width to draw the image at, measured as
     * if it were being drawn on the original template image
     * @param heightInTemplatePixels the width to draw the image at, measured as
     * if it were being drawn on the original template image
     * @since 3.0
     */
    public ScaleCache(BufferedImage source, int widthInTemplatePixels, int heightInTemplatePixels) {
        this(source);
        dw = widthInTemplatePixels;
        dh = heightInTemplatePixels;
        templateToSourceRatio = dw / (float) sw;
    }

    /**
     * Returns a version of the image that is scaled to the requested size. If
     * the dimensions of the original source image are used, the source image is
     * returned. If the dimensions match a previously cached value, then the
     * cached image is returned. Otherwise, a new, scaled image will be created
     * and returned on the fly.
     *
     * @param target the target of the rendering operation
     * @param width the required image width
     * @param height the required image height
     * @return a version of the source image scaled to the requested size
     */
    public BufferedImage getScaledImage(RenderTarget target, int width, int height) {
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }
        if (width == sw && height == sh) {
            return source;
        }
        if (width == cw && height == ch && cache != null) {
            BufferedImage bi = cache.get();
            if (bi != null) {
                return bi;
            }
        }
        BufferedImage bi = target.resample(source, width, height);
        cache = new SoftReference<>(bi);
        cw = width;
        ch = height;
        return bi;
    }

    /**
     * Returns a version of the image that is scaled up or down from the source
     * image by the requested scaling factor. If the size of the scaled image
     * matches a previously cached result, then the cached result will be
     * returned. If the scale is 1, then the source image will be returned.
     *
     * @param target the target of the rendering operation
     * @param factor a scaling factor to apply to the image
     * @return a version of the source image whose dimensions are scaled by the
     * requested amount
     */
    public BufferedImage getScaledImage(RenderTarget target, float factor) {
        return getScaledImage(target, Math.round(sw * factor), Math.round(sh * factor));
    }

    /**
     * Draws the image onto a sheet at the specified location.
     *
     * @param gScaled the sheet's scaled graphics context
     * @param sheet the sheet being drawn
     * @param target the render target value for the sheet
     * @param x the x-offset into the template to draw the image at
     * @param y the y-offset into the template to draw the image at
     */
    public void draw(Graphics2D gScaled, Sheet sheet, RenderTarget target, int x, int y) {
        BufferedImage bi = getScaledImage(target, templateToSourceRatio * (float) sheet.getScalingFactor());

        // the scaled image should be very close to the right size, so we can
        // use a faster interpolation method when drawing it
        Object oldHint = gScaled.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object newHint = target.ordinal() > RenderTarget.PREVIEW.ordinal()
                ? RenderingHints.VALUE_INTERPOLATION_BICUBIC
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        gScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, newHint);

        gScaled.drawImage(bi, x, y, dw, dh, null);

        gScaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
    }
}
