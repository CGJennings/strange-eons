package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

/**
 * An enumeration of generic use cases for a requested rendering. When a caller
 * wants to request that a sheet (or an item in a deck) be drawn, it must also
 * provide this hint to describe how it intends to use the requested result. The
 * hint will be used to choose the most appropriate combination of rendering
 * algorithms.
 *
 * <p>
 * Note that if you are implementing a {@link Sheet}, you do not generally have
 * to be concerned with this value as the framework will respond to the hint for
 * you.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public enum RenderTarget {
    /**
     * Indicates that the purpose of the rendering is to create a preview image,
     * and furthermore that completing the rendering quickly is more important
     * than image quality. This can be useful in specific situations, such as
     * creating a thumbnail image. In more general contexts, it indicates that
     * the user's system is not capable of adequate performance at the regular
     * {@link #PREVIEW} target level. As a general guideline, you should aim for
     * adequate performance on systems that are at least two years old when this
     * target is active.
     */
    FAST_PREVIEW {
        @Override
        public void applyTo(Graphics2D g) {
            g.addRenderingHints(StandardHints.FAST_PREVIEW);
        }

        @Override
        public int getTransformInterpolationType() {
            return AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
        }

        @Override
        public BufferedImage resample(BufferedImage source, int width, int height) {
            if (width < 1) {
                width = 1;
            }
            if (height < 1) {
                height = 1;
            }
            final boolean shrinkSmallImage = ((source.getWidth() * source.getHeight()) <= 4_096) && (width < source.getWidth()) && (height < source.getHeight());

            final Object hint1 = shrinkSmallImage ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
            final Object hint2 = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
            return ImageUtilities.resample(source, width, height, true, hint1, hint2);
        }
    },
    /**
     * Indicates that the purpose of the rendering is to create a preview image.
     * This target strikes a balance between speed and quality. As a general
     * guideline, you should aim for adequate performance on systems that are
     * less than two years old when this target is active.
     */
    PREVIEW {
        @Override
        public void applyTo(Graphics2D g) {
            g.addRenderingHints(StandardHints.PREVIEW);
        }

        @Override
        public int getTransformInterpolationType() {
            return AffineTransformOp.TYPE_BILINEAR;
        }

        @Override
        public BufferedImage resample(BufferedImage source, int width, int height) {
            if (width < 1) {
                width = 1;
            }
            if (height < 1) {
                height = 1;
            }
            final boolean shrinkSmallImage = ((source.getWidth() * source.getHeight()) <= 4_096) && (width < source.getWidth()) && (height < source.getHeight());
            final Object hint1 = shrinkSmallImage ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR;
            final Object hint2 = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
            return ImageUtilities.resample(source, width, height, true, hint1, hint2);
        }
    },
    /**
     * Indicates that the purpose of the rendering is to create a standalone
     * image (typically at high resolution) suitable for use in other
     * applications. This target favours quality without regard to speed.
     */
    EXPORT {
        @Override
        public void applyTo(Graphics2D g) {
            g.addRenderingHints(StandardHints.EXPORT);
        }
    },
    /**
     * Indicates that the purpose of the rendering is to render a printed image.
     * This target favours quality without regard to speed.
     */
    PRINT;

    /**
     * Applies suitable rendering hints to the graphics context for this target.
     * This will apply a default set of hints that is suitable for most
     * applications. Some renderers may choose to further customize the graphics
     * context.
     *
     * @param g the graphics context to modify
     * @throws NullPointerException if the graphics context is {@code null}
     * @see Sheet#applyContextHints(java.awt.Graphics2D)
     */
    public void applyTo(Graphics2D g) {
        g.addRenderingHints(StandardHints.PRINT);
    }

    /**
     * Returns an appropriate {@code AffineTransformOp} interpolation type
     * for this target.
     *
     * @return an appropriate affine transform interpolation type
     */
    public int getTransformInterpolationType() {
        return AffineTransformOp.TYPE_BICUBIC;
    }

    /**
     * Returns a scaled version of the source image. The image will be scaled
     * using
     * {@link ImageUtilities#resample(java.awt.image.BufferedImage, int, int, boolean, java.lang.Object, java.lang.Object)}
     * using appropriate hint values for the target type. If either the width or
     * height is less than 1, a value of 1 will be used for that dimension since
     * images cannot have a 0 width or height.
     *
     * @param source the image to scale
     * @param width the desired image width
     * @param height the desired image height
     * @return a scaled version of the image
     * @throws NullPointerException if the source image is {@code null}
     * @see ScaleCache
     */
    public BufferedImage resample(BufferedImage source, int width, int height) {
        if (width < 1) {
            width = 1;
        }
        if (height < 1) {
            height = 1;
        }

        final Object hint1 = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
        final Object hint2 = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
        return ImageUtilities.resample(source, width, height, true, hint1, hint2);
    }

    /**
     * Returns a version of the image that is scaled up or down from the source
     * image by the requested scaling factor. If the size of the scaled image
     * matches a previously cached result, then the cached result will be
     * returned. If the scale is 1, then the source image will be returned.
     *
     * @param factor a scaling factor to apply to the image
     * @return a version of the source image whose dimensions are scaled by the
     * requested amount
     * @throws NullPointerException if the source image is {@code null}
     * @see ScaleCache
     */
    public final BufferedImage resample(BufferedImage source, float factor) {
        return resample(source, Math.round(source.getWidth() * factor), Math.round(source.getHeight() * factor));
    }
}
