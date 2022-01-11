package ca.cgjennings.graphics.shapes;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.PathIterator;

/**
 * Utility methods for shapes and the paths that delineate them.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class ShapeUtilities {

    private ShapeUtilities() {
    }

    /**
     * The default flatness for flattening path iterators; equal to 1.
     */
    public static final double DEFAULT_FLATNESS = 1d;
    /**
     * The default limit for flattening path iterators; equal to 10.
     */
    public static final int DEFAULT_LIMIT = 10;

    /**
     * Measures the length of a shape's outline by flattening its path to a
     * sequence of line segments. Jumps caused by moving to another point
     * without creating a line or curve are not counted.
     *
     * @param shape the shape whose outline is to be measured
     * @return the sum of the lengths of the line segments of the flattened
     * curve
     * @throws NullPointerException if shape is {@code null}
     * @throws IllegalArgumentException if {@code flatness} or {@code limit} is
     * less than 0
     */
    public static double pathLength(Shape shape) {
        return pathLength(shape, DEFAULT_FLATNESS, DEFAULT_LIMIT);
    }

    /**
     * Measures the length of a shape's outline by flattening its path to a
     * sequence of line segments. Jumps caused by moving to another point
     * without creating a line or curve are not counted.
     *
     * @param shape the shape whose outline is to be measured
     * @param flatness the maximum allowable distance between the control points
     * and the flattened curve
     * @return the sum of the lengths of the line segments of the flattened
     * curve
     * @throws NullPointerException if shape is {@code null}
     * @throws IllegalArgumentException if {@code flatness} or {@code limit} is
     * less than 0
     */
    public static double pathLength(Shape shape, double flatness) {
        return pathLength(shape, flatness, DEFAULT_LIMIT);
    }

    /**
     * Measures the length of a shape's outline by flattening its path to a
     * sequence of line segments. Jumps caused by moving to another point
     * without creating a line or curve are not counted.
     *
     * @param shape the shape whose outline is to be measured
     * @param flatness the maximum allowable distance between the control points
     * and the flattened curve
     * @param limit log<sub>2</sub> of the maximum number of line segments that
     * will be generated for any curved segment of the path
     * @return the sum of the lengths of the line segments of the flattened
     * curve
     * @throws NullPointerException if shape is {@code null}
     * @throws IllegalArgumentException if {@code flatness} or {@code limit} is
     * less than 0
     */
    @SuppressWarnings("fallthrough")
    public static double pathLength(Shape shape, double flatness, int limit) {
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), flatness, limit);

        double points[] = new double[6];
        double moveX = 0, moveY = 0;
        double lastX = 0, lastY = 0;
        double thisX, thisY;
        double length = 0;

        while (!it.isDone()) {
            final int segmentType = it.currentSegment(points);
            switch (segmentType) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
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
                    length += Math.sqrt(dx * dx + dy * dy);
                    lastX = thisX;
                    lastY = thisY;
                    break;
            }
            it.next();
        }

        return length;
    }

    /**
     * Returns a {@link Shape} that is the union of the two shape parameters:
     * all areas that are part of either shape will be in the returned shape.
     * Neither of original shapes will be modified.
     *
     * @param lhs the primary shape
     * @param rhs the shape to add to the primary shape
     * @return the union of the two shapes
     */
    public static Area add(Shape lhs, Shape rhs) {
        Area out = new Area(lhs);
        Area arhs = rhs instanceof Area ? (Area) rhs : new Area(rhs);
        out.add(arhs);
        return out;
    }

    /**
     * Returns a {@link Shape} that is the difference of the two shape
     * parameters: the result is the {@code lhs} shape with any parts that
     * overlap the {@code rhs} removed. Neither of original shapes will be
     * modified.
     *
     * @param lhs the primary shape
     * @param rhs the shape to subtract from the primary shape
     * @return the difference of the two shapes
     */
    public static Area subtract(Shape lhs, Shape rhs) {
        Area out = new Area(lhs);
        Area arhs = rhs instanceof Area ? (Area) rhs : new Area(rhs);
        out.subtract(arhs);
        return out;
    }

    /**
     * Returns a {@link Shape} that is the intersection of the two shape
     * parameters: only areas that are part both shapes will be in the returned
     * shape. Neither of original shapes will be modified.
     *
     * @param lhs the primary shape
     * @param rhs the shape to intersect with the primary shape
     * @return the intersection of the two shapes
     */
    public static Area intersect(Shape lhs, Shape rhs) {
        Area out = new Area(lhs);
        Area arhs = rhs instanceof Area ? (Area) rhs : new Area(rhs);
        out.intersect(arhs);
        return out;
    }

    /**
     * Returns a {@link Shape} that is the exclusive or of the two shape
     * parameters: areas in one shape or the other will be included in the
     * returned shape, excluding areas that are present in both. This is
     * equivalent to {@code union(lhs,subtract(lhs,rhs))}. Neither of original
     * shapes will be modified.
     *
     * @param lhs the primary shape
     * @param rhs the shape to intersect with the primary shape
     * @return the intersection of the two shapes
     */
    public static Area exclusiveOr(Shape lhs, Shape rhs) {
        Area out = new Area(lhs);
        Area arhs = rhs instanceof Area ? (Area) rhs : new Area(rhs);
        out.exclusiveOr(arhs);
        return out;
    }
}
