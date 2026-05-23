package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

/**
 * This filter performs 90 degree rotations and horizontal or vertical flips on
 * an image.
 *
 * <p>
 * <b>In-place filtering:</b> This class <b>does not</b> support in-place
 * filtering (the source and destination images must be different).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class TurnAndFlipFilter extends AbstractImageFilter {

    /**
     * Neither turned nor flipped: this value simply copies the source image.
     */
    public static final int TURN_0 = 0;
    /**
     * Rotate 90 degrees anticlockwise.
     */
    public static final int TURN_90_LEFT = 1;
    /**
     * Rotate 180 degrees.
     */
    public static final int TURN_180 = 2;
    /**
     * Rotate 90 degrees clockwise.
     */
    public static final int TURN_90_RIGHT = 3;
    /**
     * Flip horizontally without turning.
     */
    public static final int TURN_0_FLIP_HORZ = 4;
    /**
     * Rotate 90 degrees anticlockwise and flip horizontally.
     */
    public static final int TURN_90_LEFT_FLIP_HORZ = 5;
    /**
     * Rotate 180 degrees and flip horizontally. .
     */
    public static final int TURN_180_FLIP_HORZ = 6;
    /**
     * Rotate 90 degrees clockwise and flip horizontally.
     */
    public static final int TURN_90_RIGHT_FLIP_HORZ = 7;

    /**
     * Flip vertically without turning (same as {@code TURN_180_FLIP}).
     */
    public static final int TURN_0_FLIP_VERT = 6;
    /**
     * Rotate 90 degrees anticlockwise and flip vertically (same as
     * {@code TURN_90_RIGHT_FLIP_HORZ}).
     */
    public static final int TURN_90_LEFT_FLIP_VERT = 7;
    /**
     * Rotate 180 degrees and flip vertically (same as {@code TURN_0_FLIP}).
     */
    public static final int TURN_180_FLIP_VERT = 4;
    /**
     * Rotate 90 degrees clockwise and flip vertically (same as
     * {@code TURN_90_LEFT_FLIP_HORZ}).
     */
    public static final int TURN_90_RIGHT_FLIP_VERT = 5;

    /**
     * Flip both axes without turning (same as {@code TURN_180}).
     */
    public static final int TURN_0_FLIP_BOTH = 2;
    /**
     * Rotate 90 degrees anticlockwise and flip both axes (same as
     * {@code TURN_90_RIGHT}).
     */
    public static final int TURN_90_LEFT_FLIP_BOTH = 3;
    /**
     * Rotate 180 degrees and flip both axes (same as {@code TURN_0}).
     */
    public static final int TURN_180_FLIP_BOTH = 0;
    /**
     * Rotate 90 degrees clockwise and flip both axes (same as
     * {@code TURN_90_LEFT}).
     */
    public static final int TURN_90_RIGHT_FLIP_BOTH = 1;

    private int orient = TURN_0_FLIP_HORZ;

    /**
     * Creates a new filter that is initially set to the
     * {@link #TURN_0_FLIP_HORZ} orientation.
     */
    public TurnAndFlipFilter() {
    }

    /**
     * Creates a new filter that is initially set to the specified orientation.
     *
     * @param orientation the initial orientation
     */
    public TurnAndFlipFilter(int orientation) {
        setOrientation(orientation);
    }

    /**
     * Returns the current orientation setting that describes how output images
     * should be turned and flipped relative to input images.
     *
     * @return the {@code TURN_*} orientation value
     */
    public int getOrientation() {
        return orient;
    }

    /**
     * Sets the orientation of output images relative to source images.
     *
     * @param orientation the {@code TURN_*} value that encodes how the image
     * should be rotated and flipped
     */
    public void setOrientation(int orientation) {
        if (orientation < TURN_0 || orientation > TURN_90_RIGHT_FLIP_HORZ) {
            throw new IllegalArgumentException("orientation: " + orientation);
        }
        this.orient = orientation;
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage source, ColorModel destinationColorModel) {
        final int sw = source.getWidth();
        final int sh = source.getHeight();
        final int or = getOrientation();
        int dw, dh;
        if ((or & 1) == 0) {
            dw = sw;
            dh = sh;
        } else {
            dw = sh;
            dh = sw;
        }
        if (destinationColorModel == null) {
            destinationColorModel = source.getColorModel();
        }
        return new BufferedImage(
                destinationColorModel,
                destinationColorModel.createCompatibleWritableRaster(dw, dh),
                destinationColorModel.isAlphaPremultiplied(), null
        );
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage source) {
        final int sw = source.getWidth();
        final int sh = source.getHeight();
        final int or = getOrientation();
        int dw, dh;
        if ((or & 1) == 0) {
            dw = sw;
            dh = sh;
        } else {
            dw = sh;
            dh = sw;
        }
        return new Rectangle(0, 0, dw, dh);
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        final int w = src.getWidth();
        final int h = src.getHeight();
        if (dest == null) {
            dest = createCompatibleDestImage(src, null);
        }

        if ((w * h) >= Tuning.FLIP) {
            filterParallel(src, dest);
        } else {
            filterSerial(src, dest);
        }
        return dest;
    }

    private void filterSerial(BufferedImage src, BufferedImage dest) {
        final int y1 = src.getHeight() - 1;
        final int or = getOrientation();

        switch (or) {
            case 0:
                t0(src, dest, 0, y1);
                break;
            case 1:
                t1(src, dest, 0, y1);
                break;
            case 2:
                t2(src, dest, 0, y1);
                break;
            case 3:
                t3(src, dest, 0, y1);
                break;
            case 4:
                t4(src, dest, 0, y1);
                break;
            case 5:
                t5(src, dest, 0, y1);
                break;
            case 6:
                t6(src, dest, 0, y1);
                break;
            case 7:
                t7(src, dest, 0, y1);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void filterParallel(BufferedImage src, BufferedImage dest) {
        final int h = src.getHeight();
        final int or = getOrientation();

        SplitJoin sj = SplitJoin.getInstance();
        final int n = Math.min(h, sj.getIdealSplitCount());
        final int rowCount = h / n;
        final int remainder = h - n * rowCount;
        final Runnable[] units = new Runnable[n];
        int y = 0;
        for (int i = 0; i < n; ++i) {
            int rows = rowCount;
            if (i < remainder) {
                ++rows;
            }
            units[i] = new FlipUnit(src, dest, y, rows, or);
            y += rows;
        }
        sj.runUnchecked(units);
    }

    private static class FlipUnit implements Runnable {

        private final int orientation;
        private final BufferedImage source;
        private BufferedImage dest;
        private final int y0;
        private int rows;

        public FlipUnit(BufferedImage source, BufferedImage dest, int y0, int rows, int orientation) {
            this.source = source;
            this.dest = dest;
            this.y0 = y0;
            this.rows = rows;
            this.orientation = orientation;
        }

        @Override
        public void run() {
            switch (orientation) {
                case 0:
                    t0(source, dest, y0, y0 + rows - 1);
                    break;
                case 1:
                    t1(source, dest, y0, y0 + rows - 1);
                    break;
                case 2:
                    t2(source, dest, y0, y0 + rows - 1);
                    break;
                case 3:
                    t3(source, dest, y0, y0 + rows - 1);
                    break;
                case 4:
                    t4(source, dest, y0, y0 + rows - 1);
                    break;
                case 5:
                    t5(source, dest, y0, y0 + rows - 1);
                    break;
                case 6:
                    t6(source, dest, y0, y0 + rows - 1);
                    break;
                case 7:
                    t7(source, dest, y0, y0 + rows - 1);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }

    static void t0(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int[] p = new int[width];
        for (int y = y0; y <= y1; ++y) {
            getARGBSynch(src, 0, y, width, 1, p);
            setARGBSynch(dst, 0, y, width, 1, p);
        }
    }

    static void t1(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int[] p = new int[width];
        for (int y = y0; y <= y1; ++y) {
            getARGBSynch(src, 0, y, width, 1, p);
            reverse(p);
            setARGBSynch(dst, y, 0, 1, width, p);
        }
    }

    static void t2(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int height = src.getHeight();
        int[] p = new int[width];
        for (int y = y0, dy = height - y0 - 1; y <= y1; ++y, --dy) {
            getARGBSynch(src, 0, y, width, 1, p);
            reverse(p);
            setARGBSynch(dst, 0, dy, width, 1, p);
        }
    }

    static void t3(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int height = src.getHeight();
        int[] p = new int[width];
        for (int y = y0, dx = height - y0 - 1; y <= y1; ++y, --dx) {
            getARGBSynch(src, 0, y, width, 1, p);
            setARGBSynch(dst, dx, 0, 1, width, p);
        }
    }

    static void t4(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int[] p = new int[width];
        for (int y = y0; y <= y1; ++y) {
            getARGBSynch(src, 0, y, width, 1, p);
            reverse(p);
            setARGBSynch(dst, 0, y, width, 1, p);
        }
    }

    static void t5(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int height = src.getHeight();
        int[] p = new int[width];
        for (int y = y0, dx = height - y0 - 1; y <= y1; ++y, --dx) {
            getARGBSynch(src, 0, y, width, 1, p);
            reverse(p);
            setARGBSynch(dst, dx, 0, 1, width, p);
        }
    }

    static void t6(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int height = src.getHeight();
        final int[] p = new int[width];
        for (int y = y0, dy = height - y0 - 1; y <= y1; ++y, --dy) {
            getARGBSynch(src, 0, y, width, 1, p);
            setARGBSynch(dst, 0, dy, width, 1, p);
        }
    }

    static void t7(BufferedImage src, BufferedImage dst, int y0, int y1) {
        final int width = src.getWidth();
        final int[] p = new int[width];
        for (int y = y0; y <= y1; ++y) {
            getARGBSynch(src, 0, y, width, 1, p);
            setARGBSynch(dst, y, 0, 1, width, p);
        }
    }

    static void reverse(int[] p) {
        int max = p.length / 2;
        for (int i = 0, o = p.length - 1; i < max; ++i, --o) {
            int temp = p[i];
            p[i] = p[o];
            p[o] = temp;
        }
    }
}
