package ca.cgjennings.graphics.filters;

import ca.cgjennings.algo.SplitJoin;
import static ca.cgjennings.graphics.filters.AbstractImageFilter.setARGB;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * An image filter that strokes the outline of the image shape. The stroke
 * width, colour, and the side of the image edge which is stroked can all be
 * configured by the filter user.
 *
 * <p>
 * <b>In-place filtering:</b> This filter does not support in-place filtering
 * (the source and destination must be different).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class StrokeFilter extends AbstractImageFilter {

    private int width = 4;
    private Position position = Position.OUTSIDE;
    private int color = 0xff000000;
    private boolean round = true;

    /**
     * Creates a new stroke filter that strokes with a 4 pixel wide black pen
     * along the outside of the image shape.
     */
    public StrokeFilter() {
        setWidth(4);
    }

    /**
     * Creates a new stroke filter that strokes with the specified width and
     * colour along the outside edge of the image shape.
     *
     * @param color the pen colour, as a 32-bit ARGB integer
     * @param width the pen width, in pixels
     * @throws IllegalArgumentException if the width is less than 1
     */
    public StrokeFilter(int color, int width) {
        setARGB(color);
        setWidth(width);
    }

    /**
     * Creates a new stroke filter that strokes with the specified width and
     * colour along the outside edge of the image shape.
     *
     * @param color the pen colour, as a {@code Color} object
     * @param width the pen width, in pixels
     * @throws NullPointerException if the colour is {@code null}
     * @throws IllegalArgumentException if the width is less than 1
     */
    public StrokeFilter(Color color, int width) {
        setColor(color);
        setWidth(width);
    }

    /**
     * Creates a new stroke filter that strokes with the specified width and
     * colour along the specified edge of the image shape.
     *
     * @param color the pen colour, as a {@code Color} object
     * @param width the pen width, in pixels
     * @param position the side of the edge to stroke
     * @throws NullPointerException if the colour or position is {@code null}
     * @throws IllegalArgumentException if the width is less than 1
     */
    public StrokeFilter(Color color, int width, Position position) {
        setColor(color);
        setWidth(width);
        setPosition(position);
    }

    /**
     * Creates a new stroke filter that strokes with the specified width and
     * colour along the specified edge of the image shape.
     *
     * @param color the pen colour, as a 32-bit ARGB integer
     * @param width the pen width, in pixels
     * @param position the side of the edge to stroke
     * @throws NullPointerException if the colour or position is {@code null}
     * @throws IllegalArgumentException if the width is less than 1
     */
    public StrokeFilter(int color, int width, Position position) {
        setColorRGB(color);
        setWidth(width);
        setPosition(position);
    }

    /**
     * Returns the position of the stroke relative to the edges in the image.
     *
     * @return whether the stroke falls outside, inside, or on the image edge
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Sets the position of the stroke relative the edges in the image.
     *
     * @param position whether the stroke falls outside, inside, or on the image
     * edge
     * @throws NullPointerException if the position is {@code null}
     */
    public void setPosition(Position position) {
        if (position == null) {
            throw new NullPointerException("position");
        }
        this.position = position;
    }

    /**
     * Sets the width of the stroke effect, in pixels.
     *
     * @param width the radius of the stroke effect
     */
    public void setWidth(int width) {
        if (width < 1) {
            throw new IllegalArgumentException("width < 1: " + width);
        }
        this.width = width;
    }

    /**
     * Returns the width of the stroke effect, in pixels.
     *
     * @return the radius of the stroke effect
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the stroke colour using an ARGB value.
     *
     * @param argb the stroke colour
     */
    public void setColorRGB(int argb) {
        this.color = argb;
    }

    /**
     * Returns the stroke colour as an ARGB value.
     *
     * @return the ARGB value of the stroke colour
     */
    public int getColorRGB() {
        return color;
    }

    /**
     * Sets the stroke colour using an ARGB value.
     *
     * @param argb the stroke colour
     * @deprecated Replaced by {@link #setColorRGB(int)} for consistency.
     */
    public void setARGB(int argb) {
        this.color = argb;
    }

    /**
     * Returns the stroke colour as an ARGB value.
     *
     * @return the ARGB value of the stroke colour
     * @deprecated Replaced by {@link #getColorRGB()} for consistency.
     */
    public int getARGB() {
        return color;
    }

    /**
     * Sets the stroke colour.
     *
     * @param c the stroke colour
     */
    public void setColor(Color c) {
        if (c == null) {
            throw new NullPointerException("c");
        }
        color = c.getRGB();
    }

    /**
     * Returns the stroke colour as a {@link Color} instance.
     *
     * @return the stroke colour
     */
    public Color getColor() {
        return new Color(color, true);
    }

    /**
     * Sets whether the pen shape is rounded.
     *
     * @param roundedPen {@code true} if the pen is rounded; {@code false} if
     * the pen is squared
     */
    public void setRoundedPen(boolean roundedPen) {
        round = roundedPen;
    }

    /**
     * Returns whether the pen shape is rounded.
     *
     * @return {@code true} if the pen is rounded; {@code false} if the pen is
     * squared
     */
    public boolean isRoundedPen() {
        return round;
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        if ((color & 0xff000000) == 0 || width < 1) {
            return new CloneFilter().filter(src, dest);
        }
        if (dest == null) {
            dest = createCompatibleDestImage(src, null);
        }

        int w = src.getWidth();
        int h = src.getHeight();

        int[] srcPixels = getARGB(src, null);
        int[] dstPixels;

        // TODO: for efficiency, implement SrcOver, DstAtop for int[] images
        // instead of making J2D calls; can assume src and dst are same dimensions
        Graphics2D g;
        switch (getPosition()) {
            case OUTSIDE:
                dstPixels = boxOutlineFilter(srcPixels, null, w, h, width, false, round, color);
                setARGB(dest, dstPixels);
                g = dest.createGraphics();
                try {
                    g.setComposite(AlphaComposite.SrcOver);
                    g.drawImage(src, 0, 0, null);
                } finally {
                    g.dispose();
                }
                break;
            case INSIDE:
                dstPixels = boxOutlineFilter(srcPixels, null, w, h, width, true, round, color);
                setARGB(dest, dstPixels);
                g = dest.createGraphics();
                try {
                    g.setComposite(AlphaComposite.DstAtop);
                    g.drawImage(src, 0, 0, null);
                } finally {
                    g.dispose();
                }
                break;
            case CENTER:
                // create inside part in temp image
                int outsideWidth = width / 2;
                BufferedImage temp = createCompatibleDestImage(src, null);
                dstPixels = boxOutlineFilter(srcPixels, null, w, h, width - outsideWidth, true, round, color);
                setARGB(temp, dstPixels);
                g = temp.createGraphics();
                try {
                    g.setComposite(AlphaComposite.DstAtop);
                    g.drawImage(src, 0, 0, null);
                } finally {
                    g.dispose();
                }

                // apply outside part to temp image
                boxOutlineFilter(srcPixels, dstPixels, w, h, outsideWidth, false, round, color);
                setARGB(dest, dstPixels);
                g = dest.createGraphics();
                try {
                    g.setComposite(AlphaComposite.SrcOver);
                    g.drawImage(temp, 0, 0, null);
                } finally {
                    g.dispose();
                }
                break;
            default:
                throw new AssertionError();
        }

        return dest;
    }

    /**
     * An enumeration of the possible positions for the pen relative to the
     * edges in the source image.
     */
    public static enum Position {
        /**
         * The stroke extends from the edges outwards.
         */
        OUTSIDE,
        /**
         * The stroke extends from the edges inwards.
         */
        INSIDE,
        /**
         * The stroke is centered over the edge.
         */
        CENTER
    }

    static int[] boxOutlineFilter(final int[] srcImage, int[] dstImage, final int width, final int height, int strokeWidth, final boolean invert, final boolean round, final int argb) {
        final int boxSize = strokeWidth * 2 + 1;
        final int[] dest = dstImage == null ? new int[srcImage.length] : dstImage;

        if ((width * height * width) > Tuning.PER_IMAGE) {
            SplitJoin sj = SplitJoin.getInstance();
            final int n = Math.min(height, sj.getIdealSplitCount());
            final int rowsPerTask = height / n;
            final int remainder = height - n * rowsPerTask;
            final int[][] alphas = round ? makeRoundAlphaTable(boxSize) : null;
            Runnable[] units = new Runnable[n];
            int y = 0;
            for (int i = 0; i < n; ++i) {
                int rows = rowsPerTask;
                if (i < remainder) {
                    ++rows;
                }

                final int y0 = y;
                final int rowCount = rows;
                units[i] = () -> {
                    if (round) {
                        filterBlockRound(srcImage, dest, width, height, boxSize, invert, argb, y0, rowCount, alphas);
                    } else {
                        filterBlockSquare(srcImage, dest, width, height, boxSize, invert, argb, y0, rowCount);
                    }
                };

                y += rows;
            }
            sj.runUnchecked(units);
        } else {
            if (round) {
                final int[][] alphas = makeRoundAlphaTable(boxSize);
                filterBlockRound(srcImage, dest, width, height, boxSize, invert, argb, 0, height, alphas);
            } else {
                filterBlockSquare(srcImage, dest, width, height, boxSize, invert, argb, 0, height);
            }
        }
        return dest;
    }

    static void filterBlockSquare(int[] srcImage, int[] dstImage, int width, int height, int boxSize, boolean invert, int argb, int y0, int rows) {
        int box2 = boxSize / 2;
        int boxLimit = boxSize - box2;

        final int A_COMP = (argb >>> 24);
        final int RGB_COMP = argb & 0xffffff;

        rows += y0;
        for (int y = y0; y < rows; ++y) {
            final int destRow = y * width;
            for (int x = 0; x < width; ++x) {

                int by0 = Math.max(y - box2, 0);
                int by1 = Math.min(y + boxLimit, height);
                int bx0 = Math.max(x - box2, 0);
                int bx1 = Math.min(x + boxLimit, width);

                int alpha;
                if (invert) {
                    alpha = 255;
                    for (int i = by0; i < by1; ++i) {
                        final int rowOffset = i * width;
                        final int last = rowOffset + bx1;
                        for (int j = rowOffset + bx0; j < last; ++j) {
                            alpha = Math.min(alpha, (srcImage[j] >>> 24));
                        }
                        if (alpha == 0) {
                            break;
                        }
                    }
                    alpha = 255 - alpha;
                } else {
                    alpha = 0;
                    for (int i = by0; i < by1; ++i) {
                        final int rowOffset = i * width;
                        final int last = rowOffset + bx1;
                        for (int j = rowOffset + bx0; j < last; ++j) {
                            alpha = Math.max(alpha, (srcImage[j] >>> 24));
                        }
                        if (alpha == 255) {
                            break;
                        }
                    }
                }

                if (alpha > 0) {
                    alpha = (alpha * A_COMP) / 255;
                    dstImage[destRow + x] = RGB_COMP | (alpha << 24);
                } else {
                    dstImage[destRow + x] = RGB_COMP;
                }
            }
        }
    }

    static int[][] makeRoundAlphaTable(int boxSize) {
        // create table at 4x size, then halve twice to anti-alias
        int[][] alphas = makeNonAATable(boxSize * 4);
        alphas = makeAATable(alphas);
        return makeAATable(alphas);
    }

    /**
     * Creates an anti-aliased rounded alpha table by scaling down a table to
     * half size.
     *
     * @param alphas2x an alpha table for a pen with twice the size
     * @return an anti-aliased table at half the input table size
     */
    private static int[][] makeAATable(int[][] alphas2x) {
        final int boxSize = alphas2x.length / 2;
        int[][] alphas = new int[boxSize][boxSize];
        for (int y = 0; y < boxSize; ++y) {
            int y2 = y * 2;
            for (int x = 0; x < boxSize; ++x) {
                int x2 = x * 2;
                alphas[y][x] = (alphas2x[y2][x2]
                        + alphas2x[y2][x2 + 1]
                        + alphas2x[y2 + 1][x2]
                        + alphas2x[y2 + 1][x2 + 1]) / 4;
            }
        }
        return alphas;
    }

    /**
     * Make a table of rounded alpha values without anti-aliasing.
     *
     * @param boxSize the width and height of the pen
     * @return a table of alpha values for drawing a pen of the specified size,
     * with no antialiasing
     */
    private static int[][] makeNonAATable(int boxSize) {
        int box2 = boxSize / 2;
        int boxLimit = boxSize - box2;
        int radius = box2 * boxLimit;
        int[][] alphas = new int[boxSize][boxSize];
        for (int y = 0; y < boxSize; ++y) {
            int[] row = alphas[y];
            int dy = y - box2;
            dy *= dy;
            for (int x = 0; x < boxSize; ++x) {
                int dx = x - box2;
                dx *= dx;
                int dist = dx + dy;
                if (dist <= radius) {
                    row[x] = 255;
                }
            }
        }
        return alphas;
    }

    static void filterBlockRound(int[] srcImage, int[] dstImage, int width, int height, int boxSize, boolean invert, int argb, int y0, int rows, int[][] alphas) {
        int box2 = boxSize / 2;
        int boxLimit = boxSize - box2;

        final int A_COMP = (argb >>> 24);
        final int RGB_COMP = argb & 0xffffff;

        rows += y0;
        for (int y = y0; y < rows; ++y) {
            final int destRow = y * width;
            for (int x = 0; x < width; ++x) {

                int by0 = Math.max(y - box2, 0);
                int by1 = Math.min(y + boxLimit, height);
                int bx0 = Math.max(x - box2, 0);
                int bx1 = Math.min(x + boxLimit, width);

                int alpha;
                if (invert) {
                    alpha = 255;
                    for (int i = by0; i < by1; ++i) {
                        final int rowOffset = i * width;
                        final int last = rowOffset + bx1;
                        final int[] aRow = alphas[i - y + box2];
                        for (int j = rowOffset + bx0; j < last; ++j) {
                            final int aRound = aRow[j - rowOffset - x + box2];
                            if (aRound == 0) {
                                continue;
                            }
                            int aSrc = (srcImage[j] >>> 24);
                            aSrc = 255 - ((255 - aSrc) * aRound / 255);
                            alpha = Math.min(alpha, aSrc);
                        }
                        if (alpha == 0) {
                            break;
                        }
                    }
                    alpha = 255 - alpha;
                } else {
                    alpha = 0;
                    for (int i = by0; i < by1; ++i) {
                        final int rowOffset = i * width;
                        final int last = rowOffset + bx1;
                        final int[] aRow = alphas[i - y + box2];
                        for (int j = rowOffset + bx0; j < last; ++j) {
                            final int aRound = aRow[j - rowOffset - x + box2];
                            if (aRound == 0) {
                                continue;
                            }
                            int aSrc = (srcImage[j] >>> 24);
                            if (aRound < 255) {
                                aSrc = aSrc * aRound / 255;
                            }
                            alpha = Math.max(alpha, aSrc);
                        }
                        if (alpha == 255) {
                            break;
                        }
                    }
                }

                if (alpha > 0) {
                    alpha = (alpha * A_COMP) / 255;
                    dstImage[destRow + x] = RGB_COMP | (alpha << 24);
                } else {
                    dstImage[destRow + x] = RGB_COMP;
                }
            }
        }
    }

//	public static void main(String[] args) {
//		try {
//			BufferedImage bi = ImageIO.read( new File("d:\\in2.png") );
//			StrokeFilter sf = new StrokeFilter(Color.MAGENTA, 2, Position.CENTER );
//			bi = sf.filter( bi, null );
//			ImageIO.write( bi, "png", new File("d:\\out.png") );
//		} catch( Throwable t ) {
//			t.printStackTrace();
//		}
//	}
}
