package ca.cgjennings.layout;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Shapes the left and right margins of rendered text. The shape is defined in
 * terms of insets relative to the original margins. A <code>PageShape</code>
 * returns insets based on the range of Y-positions covered by a line of text.
 * Positive inset values always reduce the margins; negative values increase
 * them. A zero inset leaves the margin unchanged.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class PageShape {

    /**
     * Return the narrowest (maximum) left edge inset between y1 and y2. y1 must
     * be &lt;= y2.
     */
    public double getLeftInset(double y1, double y2) {
        return 0;
    }

    /**
     * Return the narrowest (maximum) right edge inset between y1 and y2. y1
     * must be &lt;= y2.
     */
    public double getRightInset(double y1, double y2) {
        return 0;
    }

    double getShapedX1(double x1, double y, double ascent, double descent) {
        return x1 + getLeftInset(y, y + ascent + descent);
    }

    double getShapedX2(double x2, double y, double ascent, double descent) {
        return x2 - getRightInset(y, y + ascent + descent);
    }

    /**
     * This function is called when the markup renderer is painting its text box
     * for debugging purposes. It should draw lines or a shape to indicate how
     * the margins are modified by this shape. The default implementation
     * samples the left and right insets at regular intervals to construct an
     * approximate shape. Subclasses are encouraged to override this to provide
     * more efficient and/or more accurate implementations.
     *
     * @param g the graphics context to draw into
     * @param rect the rectangle within which text is being laid out
     */
    public void debugDraw(Graphics2D g, Rectangle2D rect) {
        Path2D.Double path = new Path2D.Double();
        double x1 = rect.getX(), x2 = rect.getX() + rect.getWidth();
        double y1 = rect.getY(), y2 = rect.getY() + rect.getHeight();
        path.moveTo(getShapedX2(x2, y1, 0, 0), y1);
        path.lineTo(getShapedX1(x1, y1, 0, 0), y1);
        for (double y = y1; y <= y2; y += 1d) {
            path.lineTo(getShapedX1(x1, y, 0, 0), y);
        }
        path.lineTo(getShapedX2(x2, y2, 0, 0), y2);
        path.moveTo(getShapedX2(x2, y1, 0, 0), y1);
        for (double y = y1; y <= y2; y += 1d) {
            path.lineTo(getShapedX2(x2, y, 0, 0), y);
        }
        g.draw(path);
    }

    /**
     * A standard shape that causes the text to conform to its layout rectangle.
     * This is the default shape for a <code>MarkupRenderer</code>. It works
     * just as if using <code>new InsetShape(0,0)</code>, although it may use an
     * optimized implementation.
     */
    public static final PageShape RECTANGLE_SHAPE = new PageShape() {
        @Override
        final double getShapedX1(double x1, double y, double ascent, double descent) {
            return x1;
        }

        @Override
        final double getShapedX2(double x2, double y, double ascent, double descent) {
            return x2;
        }

        @Override
        public void debugDraw(Graphics2D g, Rectangle2D rect) {
        }
    };

    /**
     * A <code>PageShape</code> that returns constant inset values regardless of
     * the Y-position. This is sometimes useful when creating a
     * <code>CompoundShape</code>.
     */
    public static class InsetShape extends PageShape {

        private double x1, x2;

        public InsetShape(double leftInset, double rightInset) {
            x1 = leftInset;
            x2 = rightInset;
        }

        @Override
        public double getLeftInset(double y1, double y2) {
            return x1;
        }

        @Override
        public double getRightInset(double y1, double y2) {
            return x2;
        }

        @Override
        public void debugDraw(Graphics2D g, Rectangle2D rect) {
            Line2D.Double li = new Line2D.Double(
                    rect.getX() + x1,
                    rect.getY(),
                    rect.getX() + x1,
                    rect.getY() + rect.getHeight()
            );
            g.draw(li);
            li.x1 = rect.getX() + rect.getWidth() - x2;
            li.x2 = li.x1;
            g.draw(li);
        }
    }

    /**
     * A <code>PageShape</code> that switches between two
     * <code>PageShape</code>s at a specified Y-position. This may be used to
     * build more complex shapes from simpler ones. Either or both of the shapes
     * may themselves be <code>CompoundShape</code>s.
     * <p>
     * The following example uses a compound shape to create a "plus" or "double
     * cup" shape, that is, a cup shape that handles four corners instead of
     * two:
     * <pre>
     * //     i1    i2
     * //     +------+
     * //     |      |
     * // 0 +-+      +-+ 0  y1
     * //   |          |
     * //   +---+  +---+    y2
     * //       |  |
     * //       +--+
     * //      i3  i4
     * new PageShape.CompoundShape(
     *     new PageShape.CupShape( i1, i2, y1, 0, 0 ),
     *     y2,
     *     new PageShape.InsetShape( i3, i4 )
     * );
     * </pre>
     */
    public static class CompoundShape extends PageShape {

        PageShape top, bottom;
        double y;

        public CompoundShape(PageShape topShape, double y, PageShape bottomShape) {
            this.y = y;
            top = topShape;
            bottom = bottomShape;
        }

        @Override
        public double getLeftInset(double y1, double y2) {
            if (y2 >= y) {
                if (y1 >= y) {
                    return bottom.getLeftInset(y1, y2);
                }
                return Math.max(top.getLeftInset(y1, y2), bottom.getLeftInset(y1, y2));
            } else {
                return top.getLeftInset(y1, y2);
            }
        }

        @Override
        public double getRightInset(double y1, double y2) {
            if (y2 >= y) {
                if (y1 >= y) {
                    return bottom.getRightInset(y1, y2);
                }
                return Math.max(top.getRightInset(y1, y2), bottom.getRightInset(y1, y2));
            } else {
                return top.getRightInset(y1, y2);
            }
        }

        @Override
        public void debugDraw(Graphics2D g, Rectangle2D rect) {
            Shape oldClip = g.getClip();
            Rectangle clip = new Rectangle();
            clip.setFrameFromDiagonal(
                    rect.getX(),
                    rect.getY(),
                    rect.getX() + rect.getWidth(),
                    y
            );
            g.setClip(clip);
            top.debugDraw(g, rect);

            clip.setFrameFromDiagonal(
                    rect.getX(),
                    y,
                    rect.getX() + rect.getWidth(),
                    rect.getY() + rect.getHeight()
            );
            g.setClip(clip);
            bottom.debugDraw(g, rect);

            g.setClip(oldClip);
        }
    }

    /**
     * A <code>PageShape</code> that is optimized for the most common case: a
     * rectangle that becomes wider or narrower after a certain y-point is
     * reached, e.g.:
     *
     * <pre>
     * x1        x2
     *  +--------+
     *  |        |
     *  |        |
     *  +--+  +--+  y
     *     |  |
     *     +--+
     *   xp1  xp2
     * </pre>
     */
    public static class CupShape extends PageShape {

        private double x1, x2, y, xp1, xp2, maxX1, maxX2;

        public CupShape(double leftInset1, double rightInset1, double y, double leftInset2, double rightInset2) {
            this.x1 = leftInset1;
            this.x2 = rightInset1;
            this.y = y;
            this.xp1 = leftInset2;
            this.xp2 = rightInset2;
            maxX1 = Math.max(leftInset1, leftInset2);
            maxX2 = Math.max(rightInset1, rightInset2);
        }

        @Override
        public double getLeftInset(double y1, double y2) {
            if (y2 >= y) {
                if (y1 >= y) {
                    return xp1;
                }
                return maxX1;
            } else {
                return x1;
            }
        }

        @Override
        public double getRightInset(double y1, double y2) {
            if (y2 >= y) {
                if (y1 >= y) {
                    return xp2;
                }
                return maxX2;
            } else {
                return x2;
            }
        }

        @Override
        public void debugDraw(Graphics2D g, Rectangle2D rect) {
            Line2D.Double li = new Line2D.Double(
                    rect.getX() + x1,
                    rect.getY(),
                    rect.getX() + x1,
                    y
            );
            g.draw(li);

            li.x1 = rect.getX() + rect.getWidth() - x2;
            li.x2 = li.x1;
            g.draw(li);

            li.x1 = rect.getX() + xp1;
            li.x2 = li.x1;
            li.y1 = y;
            li.y2 = rect.getY() + rect.getHeight();
            g.draw(li);

            li.x1 = rect.getX() + rect.getWidth() - xp2;
            li.x2 = li.x1;
            g.draw(li);

            // middle-left
            li.x1 = rect.getX() + x1;
            li.x2 = rect.getX() + xp1;
            li.y1 = y;
            li.y2 = y;
            g.draw(li);

            // middle-right
            li.x1 = rect.getX() + rect.getWidth();
            li.x2 = li.x1 - xp2;
            li.x1 -= x2;
            g.draw(li);
        }

        @Override
        public int hashCode() {
            return (int) (x1 + x2 + y + xp1 + xp2);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CupShape other = (CupShape) obj;
            if (x1 != other.x1) {
                return false;
            }
            if (x2 != other.x2) {
                return false;
            }
            if (y != other.y) {
                return false;
            }
            if (xp1 != other.xp1) {
                return false;
            }
            if (xp2 != other.xp2) {
                return false;
            }
            return true;
        }
    }

    /**
     * A shape that merges two source shapes into a single shape. The resulting
     * shape always uses the narrowest margin of the two source shapes at any
     * given Y-position. For example, to ensure that a shape never has a
     * negative inset (making the text wider than the requested rectangle),
     * merge it with <tt>PageShape.RECTANGLE_SHAPE</tt>.
     */
    public static class MergedShape extends PageShape {

        private PageShape s1, s2;

        public MergedShape(PageShape shape1, PageShape shape2) {
            s1 = shape1;
            s2 = shape2;
        }

        @Override
        public double getLeftInset(double y1, double y2) {
            return Math.max(
                    s1.getLeftInset(y1, y2),
                    s2.getLeftInset(y1, y2)
            );
        }

        @Override
        public double getRightInset(double y1, double y2) {
            return Math.max(
                    s1.getRightInset(y1, y2),
                    s2.getRightInset(y1, y2)
            );
        }
    }

    /**
     * A <code>PageShape</code> that takes its form from a drawing shape
     * (<code>java.awt.Shape</code>). The result is similar to scan converting
     * the source shape and adjusting the text margins to the first "on pixel"
     * in each scan row (from the left or right edge depending on the margin).
     * Notes:
     * <ol>
     * <li> Creating a <code>GeometricShape</code> is fairly expensive; they
     * should be reused where possible.
     * <li> A page shape only modifies the left and right margins of a line;
     * unlike a drawing shape, a line of text cannot contain any interior gaps.
     * </ol>
     */
    public static class GeometricShape extends PageShape {

        private Shape s;
        private double[] leftSlice;
        private double[] rightSlice;
        private double dy, height;
        private int numSlices;
        private double sliceRoundingFactor;

        private static final int MAX_SLICES = 11 * 1_200; // based on 11" paper * 1200DPI

        public GeometricShape(Shape s, Rectangle2D r) {
            this(s, r, -1);
        }

        public GeometricShape(Shape s, Rectangle2D r, int numSlices) {
            this.s = s;
            if (numSlices < 1) {
                numSlices = Math.min((int) Math.round(r.getHeight() + 0.5d), 100);
            }
            numSlices = Math.min(numSlices, MAX_SLICES);
            this.numSlices = numSlices;

            leftSlice = new double[numSlices];
            rightSlice = new double[numSlices];

            dy = r.getY();
            height = r.getHeight();
            double sliceSize = r.getHeight() / numSlices;

            Area shape = new Area(s);
            Rectangle2D shapeBounds = s.getBounds2D();
            Rectangle2D.Double sliceRect = new Rectangle2D.Double(
                    shapeBounds.getX(), r.getY(),
                    shapeBounds.getWidth(), sliceSize
            );

            double y1 = dy;
            for (int i = 0; i < numSlices; ++i) {
                Area sect = new Area(sliceRect);
                sect.intersect(shape);
                Rectangle2D sliceBounds = sect.getBounds2D();
                leftSlice[i] = sliceBounds.getX() - r.getX();
                rightSlice[i] = (r.getX() + r.getWidth()) - (sliceBounds.getX() + sliceBounds.getWidth());
                sliceRect.y += sliceSize;
            }

            sliceRoundingFactor = sliceSize / 2;
        }

        private int yToSlice(double y) {
            if (y < dy) {
                return 0;
            }
            y -= dy;
            int sl = (int) Math.floor((y + sliceRoundingFactor) / height * numSlices);
            if (sl >= numSlices) {
                sl = numSlices - 1;
            }
            return sl;
        }

        private double getInset(double y1, double y2, double[] insets) {
            int i1 = yToSlice(y1);
            int i2 = yToSlice(y2);

            if (i1 == i2) {
                return insets[i1];
            }

            double max = Double.NEGATIVE_INFINITY;
            for (int i = i1; i <= i2; ++i) {
                max = Math.max(max, insets[i]);
            }
            return max;
        }

        @Override
        public void debugDraw(Graphics2D g, Rectangle2D rect) {
            g.draw(s);
        }

        @Override
        public double getLeftInset(double y1, double y2) {
            return getInset(y1, y2, leftSlice);
        }

        @Override
        public double getRightInset(double y1, double y2) {
            return getInset(y1, y2, rightSlice);
        }
    }
}
