package ca.cgjennings.graphics.strokes;

import ca.cgjennings.graphics.shapes.ShapeUtilities;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

/**
 * A stroke that draws text along the stroked path, rotating the individual
 * glyphs to follow the shape of the curve.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TextStroke implements Stroke {

    /**
     * The {@code Style} determines how the text is placed fitted to the
     * path when the length of the text and the length of a path are not the
     * same.
     */
    public enum Style {
        /**
         * Repeat the text as needed to complete the stroke.
         */
        REPEAT,
        /**
         * Adjust the natural text spacing to fill out the stroke.
         */
        FIT,
        /**
         * Align the text to the start of the stroke.
         */
        ONCE,
    }

    private String text;
    private Font font;
    private boolean leftToRight = true;
    private boolean flipped = false;
    private Style style;
    private double flatness;
    private int limit;
    private AffineTransform t = new AffineTransform();
    private FontRenderContext frc;
    private GlyphVector glyphVector;
    private Point2D.Double lastEndPoint;

    /**
     * Creates a text stroke for the given text and font that will fit the text
     * to fill the entire path and use default {@code flatness} and
     * {@code limit} parameters.
     *
     * @param text the text to be drawn along stroked paths
     * @param font the font to drawn the text in
     */
    public TextStroke(String text, Font font) {
        this(text, font, Style.FIT, ShapeUtilities.DEFAULT_FLATNESS, ShapeUtilities.DEFAULT_LIMIT);
    }

    /**
     * Creates a text stroke for the given text, font, and fitting style and use
     * default {@code flatness} and {@code limit} parameters.
     *
     * @param text the text to be drawn along stroked paths
     * @param font the font to drawn the text in
     * @param style the style used to fit text to the path
     */
    public TextStroke(String text, Font font, TextStroke.Style style) {
        this(text, font, style, ShapeUtilities.DEFAULT_FLATNESS, ShapeUtilities.DEFAULT_LIMIT);
    }

    /**
     * Creates a text stroke for the given text and font that will fit the text
     * to fill the entire path and use default {@code flatness} and
     * {@code limit} parameters.
     *
     * @param text the text to be drawn along stroked paths
     * @param font the font to drawn the text in
     * @param style the style used to fit text to the path
     * @param flatness the maximum allowable distance between the control points
     * and the flattened curve
     * @param limit log<sub>2</sub> of the maximum number of line segments that
     * will be generated for any curved segment of the path
     * @throws IllegalArgumentException if {@code flatness} or
     * {@code limit} is less than 0
     */
    public TextStroke(String text, Font font, TextStroke.Style style, double flatness, int limit) {
        if (flatness < 0d) {
            throw new IllegalArgumentException("flatness: " + flatness);
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit: " + limit);
        }

        this.text = text;
        this.font = font;
        this.style = style;
        this.flatness = flatness;
        this.limit = limit;

        lastEndPoint = new Point2D.Double(0d, 0d);
        frc = new FontRenderContext(null, true, true);
    }

    /**
     * Sets whether the text will protrude out of the shape (the default) or
     * into it. When drawing text along the path, the rotation of each glyph is
     * determined by finding the right angle vector to a tangent to the curve
     * along the portion of the curve covered by the glyph. There are two such
     * right vectors: one points from the curve towards the <i>outside</i> of
     * the shape, and one points from the curve towards the <i>inside</i> of the
     * shape. When this is set to {@code true}, the inside vector will be
     * selected and the order in which the glyphs are drawn will be reversed.
     *
     * @param flip if {@code true}, flips the text inside-out
     */
    public final void setInsideOut(boolean flip) {
        flipped = flip;
    }

    /**
     * Returns {@code true} if the text is drawn inside-out, that is, if
     * the direction that the glyphs protrude from the path is flipped.
     *
     * @return {@code true} if the glyph direction should be flipped
     */
    public final boolean isInsideOut() {
        return flipped;
    }

    /**
     * Sets the text that will be drawn along stroked paths.
     *
     * @param text the text drawn by this stroke
     */
    public final void setText(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }
        if (!this.text.equals(text)) {
            this.text = text;
            glyphVector = null;
        }
    }

    /**
     * Returns the text that will be drawn along stroked paths.
     *
     * @return the text drawn by this stroke
     */
    public final String getText() {
        return text;
    }

    /**
     * Sets the bidi orientation of the text. Set to {@code true} for
     * left-to-right languages (e.g., English) and {@code false} for
     * right-to-left languages (e.g., Arabic). The default is {@code true}.
     *
     * @param leftToRight {@code true} if the text should be treated as
     * left-to-right
     */
    public final void setLeftToRight(boolean leftToRight) {
        if (this.leftToRight != leftToRight) {
            this.leftToRight = leftToRight;
            glyphVector = null;
        }
    }

    /**
     * Returns {@code true} if the text is treated as a left-to-right
     * language.
     *
     * @return {@code true} if the bidi order is treated as left-to-right
     */
    public final boolean isLeftToRight() {
        return leftToRight;
    }

    /**
     * Sets the font used that the stroke draws text with.
     *
     * @param font the font used by letters in this stroke
     */
    public final void setFont(Font font) {
        if (font == null) {
            throw new NullPointerException("font");
        }
        if (this.font != font) {
            this.font = font;
            glyphVector = null;
        }
    }

    /**
     * Returns the font used to draw text.
     *
     * @return the font used to draw letters along stroked paths
     */
    public final Font getFont() {
        return font;
    }

    private void createGlyphVector() {
        int flags;
        if (leftToRight) {
            flags = Font.LAYOUT_LEFT_TO_RIGHT | Font.LAYOUT_NO_START_CONTEXT | Font.LAYOUT_NO_LIMIT_CONTEXT;
        } else {
            flags = Font.LAYOUT_RIGHT_TO_LEFT | Font.LAYOUT_NO_START_CONTEXT | Font.LAYOUT_NO_LIMIT_CONTEXT;
        }
        final char[] string = text.toCharArray();
        glyphVector = font.layoutGlyphVector(frc, string, 0, string.length, flags);
    }

    @SuppressWarnings("fallthrough")
    @Override
    public Shape createStrokedShape(Shape shape) {
        Path2D result = new Path2D.Double();
        if (glyphVector == null) {
            createGlyphVector();
        }
        final int numGlyphs = glyphVector.getNumGlyphs();
        if (numGlyphs == 0) {
            return result;
        }

        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), flatness, limit);
        double points[] = new double[6];
        double moveX = 0, moveY = 0;
        double lastX = 0, lastY = 0;
        double thisX = 0, thisY = 0;
        double next = 0;

        int currentChar = flipped ? numGlyphs - 1 : 0;

//		double beginOffset = 0d;
        double nextAdvance = 0;
        double advanceScale = 1d;

        switch (style) {
            case FIT:
                advanceScale = ShapeUtilities.pathLength(shape, flatness, limit) / textWidth();
                break;
            case ONCE:
            case REPEAT:
                break;
            default:
                throw new AssertionError("unknown fitting style");
        }

        while (currentChar < numGlyphs && currentChar >= 0 && !it.isDone()) {
            final int segmentType = it.currentSegment(points);
            switch (segmentType) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    result.moveTo(moveX, moveY);
                    nextAdvance = glyphVector.getGlyphMetrics(currentChar).getAdvance() * 0.5f;
                    next = nextAdvance;
                    break;

                case PathIterator.SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                // fallthrough

                case PathIterator.SEG_LINETO:
                    thisX = points[0];
                    thisY = points[1];
                    double dx = thisX - lastX;
                    double dy = thisY - lastY;
                    double distance = Math.sqrt(dx * dx + dy * dy);

//				if( beginOffset > 0d ) {
//					beginOffset -= distance;
//					if( beginOffset < 0d )
//						distance += -beginOffset;
//				} else {
                    if (distance >= next) {
                        double r = 1.0f / distance;
                        double angle = Math.atan2(dy, dx);
                        if (flipped) {
                            angle += Math.PI;
                        }

                        while (currentChar < numGlyphs && currentChar >= 0 && distance >= next) {
                            Shape glyph = glyphVector.getGlyphOutline(currentChar);
                            Point2D p = glyphVector.getGlyphPosition(currentChar);
                            double px = p.getX();
                            double py = p.getY();
                            double x = lastX + next * dx * r;
                            double y = lastY + next * dy * r;
                            double advance = nextAdvance;

                            lastEndPoint.setLocation(x + nextAdvance * dx * r, y + nextAdvance * dy * r);

                            if (flipped) {
                                nextAdvance = glyphVector.getGlyphMetrics(currentChar > 0 ? currentChar - 1 : numGlyphs - 1).getAdvance() * 0.5f;
                                currentChar--;
                                if (style == Style.REPEAT && currentChar == -1) {
                                    currentChar = numGlyphs - 1;
                                }
                            } else {
                                nextAdvance = glyphVector.getGlyphMetrics(currentChar < numGlyphs - 1 ? currentChar + 1 : 0).getAdvance() * 0.5f;
                                currentChar++;
                                if (style == Style.REPEAT && currentChar == numGlyphs) {
                                    currentChar = 0;
                                }
                            }
                            t.setToTranslation(x, y);
                            t.rotate(angle);
                            t.translate(-px - advance, -py);
                            result.append(t.createTransformedShape(glyph), false);
                            next += (advance + nextAdvance) * advanceScale;
                        }
                    }
                    next -= distance;
//					first = false;
//				}
                    lastX = thisX;
                    lastY = thisY;
                    break;
            }
            it.next();
        }
        return result;
    }

    /**
     * Returns the point at which the last call to
     * {@link #createStrokedShape(java.awt.Shape)} last drew a glyph.
     *
     * @return the end point of the last glyph
     */
    public Point2D.Double getEndPoint() {
        return lastEndPoint;
    }

    /**
     * Returns the width of the current text, if it was rendered in a straight
     * line.
     *
     * @return the length of path that would naturally fit the current text
     */
    public double textWidth() {
        if (glyphVector == null) {
            createGlyphVector();
        }
        return glyphVector.getLogicalBounds().getWidth();
    }
}
