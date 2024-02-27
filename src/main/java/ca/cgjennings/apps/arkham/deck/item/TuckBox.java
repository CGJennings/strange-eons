package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import javax.swing.Icon;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * The tuck box item is used to create tuck boxes (or other fold-up boxes). It
 * also supports the registration of {@link TuckBox.BoxSizer}s, which assist the
 * user in creating boxes of the right size for a particular application.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class TuckBox extends AbstractFlippableItem implements EditablePageItem {

    /**
     * An enumeration of the supported box designs.
     */
    public enum BoxType {
        /**
         * A playing card-style box that is not glued closed on one of the sides
         * so that it can be opened and closed.
         */
        TUCK_BOX("style-cb-box-tuck"),
        /**
         * A bottom and four sides, with the sides glued to each other using
         * flaps. This can be used to organize groups of smaller boxes, or two
         * copies can be made (one slightly larger than the other) to make a box
         * with a removable lid.
         */
        OPEN_FACE_BOX("style-cb-box-open");

        private String name;

        private BoxType(String key) {
            name = string(key);
        }

        @Override
        public String toString() {
            return name;
        }
    };

    private BoxType type = BoxType.TUCK_BOX;
    private double boxW = 4.5d / 2.54d * 72d, boxH = 6.5d / 2.54d * 72d, boxD = 2.4d / 2.54d * 72d;
    private Color interiorFill = Color.WHITE, exteriorFill = null, lineColour = Color.BLACK, foldColour = Color.DARK_GRAY;
    private boolean thumbNotch = true, roundedSideFlaps = false, hingeCut = true, showFoldLines = true;
    private float lineThickness = 1f;

    /**
     * A sizing tool for a particular type of component. The sizer's
     * {@code toString} method should return a human-readable description of the
     * type of component(s) that this sizer produces box measurements for.
     */
    public static interface BoxSizer {

        /**
         * Returns the dimensions for a box of the type represented by this
         * sizer that allows sufficient space to hold a number of cards equal to
         * {@code cards}. The dimensions are returned as an array of three
         * {@code double} values which measure the width, height, and depth in
         * mm (respectively).
         *
         * @param cards the number of objects the box should hold
         * @param sleeveThicknessInMicrons the tickness rating of the plastic
         * sleeve in microns, 0 for no sleeves
         * @return an array of millimetre measurements for the box's width,
         * height and depth
         */
        public double[] size(int cards, double sleeveThicknessInMicrons);

        /**
         * Returns {@code true} if this helper uses sleeve thickness as part of
         * its sizing algorithm. Non-card components typically don't have a
         * suitable sleeve available, so sizers for such components would return
         * {@code false}.
         *
         * @return {@code true} if and only if the user can store this component
         * type on plastic sleeves
         */
        public boolean allowSleeves();
    }

    // 30 100 micron sleeves = 3 mm (double since 2 layers of plastic)
    // the rest is padding to allow for irregularities in how the cards lay
    private static final double STANDARD_THICKNESS_ALLOWANCE_PER_MICRON = 2d * (20d / 50d / 100d);

    /**
     * An implementation of {@link BoxSizer} that has a fixed width and height
     * but varies in depth according to the number of components the box will
     * contain. The additional thickness due to sleeves is calculated
     * automatically using a default method.
     */
    public static class SimpleDepthwiseSizer implements BoxSizer {

        private String name;
        private double w, h, dpc, padding;
        private int minCards;
        private boolean sleeves = true;

        /**
         * This convenience constructor creates a sizer that allows plastic
         * sleeves.
         *
         * @param name the name to be returned by the sizer's {@code name()}
         * method
         * @param width the width of boxes created by this sizer, in mm
         * @param height the height of boxes created by this sizer, in mm
         * @param depthPadding a padding value added to the depth of every box,
         * in mm (5 is a typical value)
         * @param depthPerCard the added depth required to hold 1 card, in mm
         * @param minCards a minimum depth for the box, expressed as a card
         * count
         * @throws NullPointerException if {@code name} is {@code null}
         * @throws IllegalArgumentException if any dimension or {@code minCards}
         * is less than or equal to 0, or if {@code depthPadding} is negative
         */
        public SimpleDepthwiseSizer(String name, double width, double height, double depthPadding, double depthPerCard, int minCards) {
            this(name, width, height, depthPadding, depthPerCard, minCards, true);
        }

        /**
         * Create a {@link BoxSizer} that creates boxes of the given width and
         * height. The depth of the box will be determined using the formula
         * {@code depthPadding + depthPerCard * numberOfCards}, but will not be
         * less than {@code depthPadding + depthPerCard * minCards}. All
         * measurements are in millimetres.
         *
         * @param name the name to be returned by the sizer's {@code name()}
         * method
         * @param width the width of boxes created by this sizer, in mm
         * @param height the height of boxes created by this sizer, in mm
         * @param depthPadding a padding value added to the depth of every box,
         * in mm (5 is a typical value)
         * @param depthPerCard the added depth required to hold 1 card, in mm
         * @param minCards a minimum depth for the box, expressed as a card
         * count
         * @throws NullPointerException if {@code name} is {@code null}
         * @throws IllegalArgumentException if any dimension or {@code minCards}
         * is less than or equal to 0, or if {@code depthPadding} is negative
         */
        public SimpleDepthwiseSizer(String name, double width, double height, double depthPadding, double depthPerCard, int minCards, boolean allowSleeves) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (width <= 0d || height <= 0d || depthPerCard <= 0d) {
                throw new IllegalArgumentException("zero or negative dimension");
            }
            if (minCards <= 0) {
                throw new IllegalArgumentException("zero or negative minCards");
            }
            if (depthPadding < 0) {
                throw new IllegalArgumentException("negative depthPadding");
            }
            this.name = name;
            w = width;
            h = height;
            dpc = depthPerCard;
            padding = depthPadding;
            this.minCards = minCards;
            sleeves = allowSleeves;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public double[] size(int cards, double sleeveThicknessInMicrons) {
            if (cards < minCards) {
                cards = minCards;
            }
            double sleevePad = sleeveThicknessInMicrons > 0 ? 7 : 4;
            double sleeveThickness = sleeveThicknessInMicrons * STANDARD_THICKNESS_ALLOWANCE_PER_MICRON * cards;
            return new double[]{
                w + sleevePad, h + sleevePad, padding + dpc * cards + sleeveThickness
            };
        }

        @Override
        public boolean allowSleeves() {
            return sleeves;
        }
    }

    /**
     * An implementation of {@link BoxSizer} that has a fixed width and depth
     * but varies in height according to the number of components the box will
     * contain. The additional thickness due to sleeves is calculated
     * automatically using a default method.
     */
    public static class SimpleHeightwiseSizer implements BoxSizer {

        private String name;
        private double w, d, hpc, padding;
        private int minCards;
        private boolean sleeves;

        /**
         * This is a convenience that creates a sizer that <b>does not</b>
         * allow sleeves. Note that this is the opposite default of
         * {@link SimpleDepthwiseSizer}, because this sizer tends to be used for
         * thick items like tokens.
         *
         * @param name the name to be returned by the sizer's {@code name()}
         * method
         * @param width the width of boxes created by this sizer, in mm
         * @param depth the depth of boxes created by this sizer, in mm
         * @param heightPadding a padding value added to the height of every
         * box, in mm (5 is a typical value)
         * @param heightPerCard the added height required to hold 1 card, in mm
         * @param minCards a minimum depth for the box, expressed as a card
         * count
         * @throws NullPointerException if {@code name} is {@code null}
         * @throws IllegalArgumentException if any dimension or {@code minCards}
         * is less than or equal to 0, or if {@code heightPadding} is negative
         */
        public SimpleHeightwiseSizer(String name, double width, double depth, double heightPadding, double heightPerCard, int minCards) {
            this(name, width, depth, heightPadding, heightPerCard, minCards, false);
        }

        /**
         * Create a {@link BoxSizer} that creates boxes of the given width and
         * depth. The height of the box will be determined using the formula
         * {@code heightPadding + heightPerCard * numberOfCards}, but will not
         * be less than {@code heightPadding + heightPerCard * minCards}. All
         * measurements are in millimetres.
         *
         * @param name the name to be returned by the sizer's {@code name()}
         * method
         * @param width the width of boxes created by this sizer, in mm
         * @param depth the height of boxes created by this sizer, in mm
         * @param heightPadding a padding value added to the height of every
         * box, in mm (5 is a typical value)
         * @param heightPerCard the added height required to hold 1 card, in mm
         * @param minCards a minimum depth for the box, expressed as a card
         * count
         * @param allowSleeves {@code true} if the user can select a sleeve
         * thickness when using this sizer
         * @throws NullPointerException if {@code name} is {@code null}
         * @throws IllegalArgumentException if any dimension or {@code minCards}
         * is less than or equal to 0, or if {@code heightPadding} is negative
         */
        public SimpleHeightwiseSizer(String name, double width, double depth, double heightPadding, double heightPerCard, int minCards, boolean allowSleeves) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (width <= 0d || depth <= 0d || heightPerCard <= 0d) {
                throw new IllegalArgumentException("zero or negative dimension");
            }
            if (minCards <= 0) {
                throw new IllegalArgumentException("zero or negative minCards");
            }
            if (heightPadding < 0) {
                throw new IllegalArgumentException("negative depthPadding");
            }
            this.name = name;
            w = width;
            d = depth;
            hpc = heightPerCard;
            padding = heightPadding;
            this.minCards = minCards;
            sleeves = allowSleeves;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public double[] size(int cards, double sleeveThicknessInMicrons) {
            if (cards < minCards) {
                cards = minCards;
            }
            double sleevePad = sleeveThicknessInMicrons > 0 ? 7 : 4;
            double sleeveThickness = sleeveThicknessInMicrons * STANDARD_THICKNESS_ALLOWANCE_PER_MICRON * cards;
            return new double[]{
                w + sleevePad, padding + hpc * cards + sleeveThickness, d + sleevePad
            };
        }

        @Override
        public boolean allowSleeves() {
            return sleeves;
        }
    }

    private static BoxSizer[] sizers = new BoxSizer[]{
        // cards matching the generic card sizes
        new SimpleDepthwiseSizer(string("gencard-sh-am"), 55.88d, 87.122d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-am-mini"), 40.894d, 62.992d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-bridge"), 57.15d, 88.9d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-business"), 50.8d, 88.9d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-crafting"), 67.564d, 119.38d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-domino"), 44.45d, 88.9d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-eu-mini"), 43.942d, 67.056d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-jumbo"), 88.9d, 139.7d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-micro"), 31.75d, 44.45d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-mini"), 44.45d, 63.5d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-poker"), 62.992d, 87.884d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-skat"), 58.928d, 90.932d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-sq200"), 50.8d, 50.8d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-sq250"), 63.5d, 63.5d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-sq275"), 69.85d, 69.85d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-tarot"), 69.85d, 120.65d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-trading"), 63.5d, 88.9d, 5d, 12d / 30d, 15),
        new SimpleDepthwiseSizer(string("gencard-sh-trump"), 62.23d, 100.33d, 5d, 12d / 30d, 15),        
        // small tokens
        new SimpleHeightwiseSizer(string("style-cb-box-3"), 43d, 43d, 5d, 48d / 25d, 48),};

    /**
     * Registers a new sizer, adding it to the set of standard box sizing
     * helpers. If {@code sizer} has already been added, nothing happens.
     * Otherwise, the new sizer is added to the set of helpers that is returned
     * by {@link #getBoxSizers()}.
     *
     * @param sizer a sizing helper to add to the standard list of helpers
     * @throws NullPointerException if {@code sizer} is {@code null}
     */
    public static void registerBoxSizer(BoxSizer sizer) {
        if (sizer == null) {
            throw new NullPointerException("sizer");
        }
        for (int i = 0; i < sizers.length; ++i) {
            if (sizers[i].equals(sizer)) {
                return;
            }
        }
        sizers = Arrays.copyOf(sizers, sizers.length + 1);
        sizers[sizers.length - 1] = sizer;
    }

    /**
     * Returns a copy of the current set of standard box sizers as an array.
     *
     * @return an array of the current standard box sizing helpers
     */
    public static BoxSizer[] getBoxSizers() {
        return sizers.clone();
    }

    public TuckBox() {
        updateBoxShape();
    }

    @Override
    public String getName() {
        return string("de-tuck-box-name");
    }

    @Override
    public double getHeight() {
        double h;
        switch (type) {
            case TUCK_BOX:
                if (isVertical()) {
                    h = boxH + 2 * boxD + wFlapSize;
                } else {
                    h = 2 * boxW + 2 * boxD + hFlapSize;
                }
                break;
            case OPEN_FACE_BOX:
                if (isVertical()) {
                    h = boxH + 2d * boxD;
                } else {
                    h = boxW + 2d * boxD;
                }
                break;
            default:
                throw new AssertionError();
        }
        return h;
    }

    @Override
    public double getWidth() {
        double w;
        switch (type) {
            case TUCK_BOX:
                if (isVertical()) {
                    w = 2 * boxW + 2 * boxD + hFlapSize;
                } else {
                    w = boxH + 2 * boxD + wFlapSize;
                }
                break;
            case OPEN_FACE_BOX:
                if (isVertical()) {
                    w = boxW + 2d * boxD;
                } else {
                    w = boxH + 2d * boxD;
                }
                break;
            default:
                throw new AssertionError();
        }
        return w;
    }

    @Override
    public Icon getThumbnailIcon() {
        if (sharedIcon == null) {
            sharedIcon = ResourceKit.getIcon("deck/box.png");
        }
        return sharedIcon;
    }
    private static transient Icon sharedIcon = null;

    public void setDimensions(double width, double height, double depth) {
        boxW = width;
        boxH = height;
        boxD = depth;
        updateBoxShape();
    }

    public double[] getDimensions() {
        return new double[]{boxW, boxH, boxD};
    }

    public void setBoxType(BoxType type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        if (this.type != type) {
            this.type = type;
            updateBoxShape();
        }
    }

    public BoxType getBoxType() {
        return type;
    }

    private double calcFlapCutInset(double length) {
        double inset = Math.max(FLAP_CUT_INSET, length / 4d);
        if (inset > length / 3d) {
            inset = length / 3d;
        }
        return inset;
    }

    /**
     * Recompute tuck box geometry based on boxW, boxH, and boxD.
     */
    private void updateBoxShape() {
        cutLines = new Path2D.Double();
        foldLines = new Path2D.Double();
        interiorCuts = new Path2D.Double();
        switch (type) {
            case TUCK_BOX:
                updateTuckBoxShape();
                break;
            case OPEN_FACE_BOX:
                updateOpenFaceShape();
                break;
            default:
                throw new AssertionError("unknown box type");
        }
    }

    private void updateOpenFaceShape() {
        final double w = boxW;
        final double h = boxH;
        final double d = boxD;

        // calc size of thumb notch
        boolean notch = thumbNotch;
        final double arcWidth = notch ? calcArcWidth() : 0d;
        final double arcHeight = Math.min(calcArcHeight(), 0.6d * d);
        // too small for a useful arc
        if (arcHeight < 14) {
            notch = false;
        }

        // limit the width of the flaps in case the box depth > box height
        final double fo = Math.min(d, (h - 18) / 2d); // 18pt = 1/4" gap between flaps

        // Top
        // draw the top edge first; this will always be the same regardless
        // of whether flaps are rounded since we don't draw flaps till after
        cutLines.moveTo(d, 0d);

        if (notch) {
            final double hAw = arcWidth / 2d;
            final double c = d + w / 2d;
            cutLines.lineTo(c - hAw, 0d);
            cutLines.quadTo(c - hAw, arcHeight, c, arcHeight);
            cutLines.quadTo(c + hAw, arcHeight, c + hAw, 0);
        }
        cutLines.lineTo(d + w, 0d);

        if (roundedSideFlaps) {
            // UR corner
            cutLines.quadTo(d + w + fo, 0d, d + w + fo, d);
            cutLines.lineTo(2d * d + w, d);

            // Right
            cutLines.lineTo(2d * d + w, d + h);

            // BR corner
            cutLines.lineTo(d + w + fo, d + h);
            cutLines.quadTo(d + w + fo, 2d * d + h, d + w, 2d * d + h);

            // Bottom
            cutLines.lineTo(d, 2d * d + h);

            // LL corner
            cutLines.quadTo(d - fo, 2d * d + h, d - fo, d + h);
            cutLines.lineTo(0, d + h);

            // Left
            cutLines.lineTo(0, d);

            // UL corner
            cutLines.lineTo(d - fo, d);
            cutLines.quadTo(d - fo, 0d, d, 0d);
        } else /* straight side flaps */ {
            // UR corner
            cutLines.lineTo(d + w + fo, 0d);
            cutLines.lineTo(d + w + fo, d);
            cutLines.lineTo(2d * d + w, d);

            // Right
            cutLines.lineTo(2d * d + w, d + h);

            // LR corner
            cutLines.lineTo(d + w + fo, d + h);
            cutLines.lineTo(d + w + fo, 2d * d + h);
            cutLines.lineTo(d + w, 2d * d + h);

            // Bottom
            cutLines.lineTo(d, 2d * d + h);

            // LL corner
            cutLines.lineTo(d - fo, 2d * d + h);
            cutLines.lineTo(d - fo, d + h);
            cutLines.lineTo(0, d + h);

            // Left
            cutLines.lineTo(0, d);

            // UL corner
            cutLines.lineTo(d - fo, d);
            cutLines.lineTo(d - fo, 0);
            cutLines.lineTo(d, 0);
        }

        openFaceInteriorCut(d, d);
        openFaceInteriorCut(d * 2d + w, d);
        openFaceInteriorCut(d, d + h);
        openFaceInteriorCut(d * 2d + w, d + h);

        foldLines.moveTo(d, d);
        foldLines.lineTo(w + d, d);
        foldLines.lineTo(w + d, d + h);
        foldLines.lineTo(d, d + h);
        foldLines.lineTo(d, d);
        openFaceEdgeFold(d, d);
        openFaceEdgeFold(w + d, d);
        openFaceEdgeFold(d, d * 2d + h);
        openFaceEdgeFold(w + d, d * 2d + h);
    }

    private void openFaceEdgeFold(double x, double y) {
        foldLines.moveTo(x, y);
        foldLines.lineTo(x, y - boxD);
    }

    private void openFaceInteriorCut(double x, double y) {
        interiorCuts.moveTo(x, y);
        interiorCuts.lineTo(x - boxD, y);
    }

    private double calcArcWidth() {
        double arcWidth = boxW / ARC_TO_WIDTH_RATIO;
        if (arcWidth < ARC_MINIMUM) {
            arcWidth = boxW / ARC_TO_WIDTH_MINIMUM_RATIO;
        } else {
            arcWidth = Math.min(arcWidth, ARC_MAXIMUM);
        }
        return arcWidth;
    }

    private double calcArcHeight() {
        double arcHeight = calcArcWidth() / ARC_HEIGHT_RATIO;
        return arcHeight;
    }

    private void updateTuckBoxShape() {
        final double arcWidth = calcArcWidth();
        final double arcHeight = calcArcHeight();

        // the width of the top and bottom flaps
        dFlapSize = Math.min(boxW / 3d, boxD);
        hFlapSize = boxD * SIDE_FLAP_MINIMUM_RATIO;
        if (hFlapSize < SIDE_FLAP_PREFERRED_WIDTH) {
            hFlapSize = boxD * SIDE_FLAP_MAXIMUM_RATIO;
        }
        wFlapSize = arcHeight * 5d / 4d;

        double flapCutInsetW = calcFlapCutInset(wFlapSize);
        double flapCutInsetH = calcFlapCutInset(hFlapSize);
        double flapCutInsetD = calcFlapCutInset(dFlapSize);

        double boxTopY = wFlapSize + boxD;

        // top flap fold
        double curveWidth = flapCutInsetW / 5d;
        line(foldLines, boxD + flapCutInsetW + curveWidth, wFlapSize + 0.5d, boxD + boxW - flapCutInsetW - curveWidth, wFlapSize + 0.5d);
        // partial cut lines for top flap
        //line( interiorCuts, boxD, wFlapSize, boxD + flapCutInsetW, wFlapSize );
        line(interiorCuts, boxD, wFlapSize, boxD + flapCutInsetW - curveWidth, wFlapSize);
        interiorCuts.quadTo(boxD + flapCutInsetW + curveWidth * 2d, wFlapSize, boxD + flapCutInsetW + curveWidth * 2d, wFlapSize + curveWidth * 2d);
        //line( interiorCuts, boxD + boxW, wFlapSize, boxD + boxW - flapCutInsetW, wFlapSize );
        line(interiorCuts, boxD + boxW, wFlapSize, boxD + boxW - flapCutInsetW + curveWidth, wFlapSize);
        interiorCuts.quadTo(boxD + boxW - flapCutInsetW - curveWidth * 2d, wFlapSize, boxD + boxW - flapCutInsetW - curveWidth * 2d, wFlapSize + curveWidth * 2d);

        // back side cuts to allow lid to open
        double cutDepth = arcHeight;
        if (hingeCut) {
            line(foldLines, boxD, boxTopY + cutDepth, boxD + boxW, boxTopY + cutDepth);
            // partial cut lines for back cut
            line(interiorCuts, boxD, boxTopY, boxD, boxTopY + cutDepth);
            line(interiorCuts, boxD + boxW, boxTopY, boxD + boxW, boxTopY + cutDepth);
        }

        // folds along top of box
        line(foldLines, 0, boxTopY, boxD * 2d + boxW, boxTopY);

        // folds along bottom of box
        double boxBottomY = boxTopY + boxH;
        line(foldLines, 0, boxBottomY, boxD * 2d + boxW * 2d, boxBottomY);

        // bottom flap fold
        // line( foldLines, boxD, boxBottomY + boxD, boxD + boxW, boxBottomY + boxD );
        // box side folds
        double x = 0;
        for (int i = 0; i < 4; ++i) {
            double y = boxTopY;
            if (i < 2 && showFoldLines && hingeCut) {
                y += cutDepth;
            }
            if ((i & 1) == 0) {
                x += boxD;
            } else {
                x += boxW;
            }
            line(foldLines, x, y, x, boxBottomY);
        }

        //////////////////////////////
        // top : three sides with flap
        cutLines.moveTo(0d, boxTopY);
        fhflap(cutLines, 0d, boxTopY, boxD, flapCutInsetD, ROUND_LEFT | (roundedSideFlaps ? ROUND_RIGHT : 0), dFlapSize);
        cutLines.lineTo(boxD, wFlapSize);
        fhflap(cutLines, boxD, wFlapSize, boxW, flapCutInsetW, ROUND_LEFT | ROUND_RIGHT, wFlapSize);
        cutLines.lineTo(boxD + boxW, boxTopY);
        fhflap(cutLines, boxD + boxW, boxTopY, boxD, flapCutInsetD, ROUND_RIGHT | (roundedSideFlaps ? ROUND_LEFT : 0), dFlapSize);

        // top : back with thumb cut
        if (thumbNotch) {
            x = boxD * 2d + boxW;
            double arcX1 = x + (boxW - arcWidth) / 2d;
            double arcX2 = arcX1 + arcWidth / 2d;
            double arcX3 = arcX1 + arcWidth;
            double arcY = boxTopY + arcHeight;
            cutLines.lineTo(arcX1, boxTopY);
            cutLines.quadTo(arcX1, arcY, arcX2, arcY);
            cutLines.quadTo(arcX3, arcY, arcX3, boxTopY);
        }
        cutLines.lineTo(boxD * 2d + boxW * 2d, boxTopY);

        // side : right-hand flap
        vflap(cutLines, boxD * 2d + boxW * 2d, boxTopY, boxH, flapCutInsetH, hFlapSize);

        // bottom : flat bottom flap
        double bottomFlapSize = Math.min(boxD * 0.45d, Math.min(boxW * 0.45d, Math.min(wFlapSize, dFlapSize)));
        double bottomFlapCutInset = bottomFlapSize;
        cutLines.lineTo(boxD * 2 + boxW * 2, boxBottomY + boxD);
        cutLines.lineTo(boxD * 2 + boxW, boxBottomY + boxD);
        cutLines.lineTo(boxD * 2 + boxW, boxBottomY + bottomFlapSize);
        line(interiorCuts, boxD * 2 + boxW, boxBottomY + bottomFlapSize, boxD * 2 + boxW, boxBottomY);

        // bottom : 3 back flaps
        rhflap(cutLines, boxD * 2d + boxW, boxBottomY, -boxD, bottomFlapCutInset, ROUND_RIGHT, bottomFlapSize);
        rhflap(cutLines, boxD + boxW, boxBottomY, -boxW, bottomFlapCutInset, ROUND_LEFT | ROUND_RIGHT | ROUND_EXTEND, bottomFlapSize);
        rhflap(cutLines, boxD, boxBottomY, -boxD, bottomFlapCutInset, ROUND_LEFT, bottomFlapSize);

        // side: left-hand edge
        cutLines.lineTo(0, boxTopY);
    }

    private void line(Path2D p, double x1, double y1, double x2, double y2) {
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
    }

    private void fhflap(Path2D p, double x, double y, double dx, double inset, int roundCornerBitMask, double flapSize) {
        double dy = -flapSize;
        if ((roundCornerBitMask & ROUND_LEFT) != 0) {
            p.quadTo(x, y + dy, x + inset, y + dy);
        } else {
            p.lineTo(x, y + dy);
            p.lineTo(x + inset, y + dy);
        }
        p.lineTo(x + (dx - inset), y + dy);
        if ((roundCornerBitMask & ROUND_RIGHT) != 0) {
            p.quadTo(x + dx, y + dy, x + dx, y);
        } else {
            p.lineTo(x + dx, y + dy);
            p.lineTo(x + dx, y);
        }
    }

    private static final int ROUND_LEFT = 1;
    private static final int ROUND_RIGHT = 2;
    private static final int ROUND_EXTEND = 4;

    private void rhflap(Path2D p, double x, double y, double dx, double inset, int roundCornerBitMask, double flapSize) {
        double dy = flapSize;
        if ((roundCornerBitMask & ROUND_LEFT) != 0) {
            p.lineTo(x - inset, y + dy);
            if ((roundCornerBitMask & ROUND_EXTEND) != 0) {
                p.lineTo(x - inset, y + boxD);
            }
        } else {
            p.lineTo(x, y + dy);
        }
        if ((roundCornerBitMask & ROUND_RIGHT) != 0) {
            if ((roundCornerBitMask & ROUND_EXTEND) != 0) {
                p.lineTo(x + (dx + inset), y + boxD);
            }
            p.lineTo(x + (dx + inset), y + dy);
            p.lineTo(x + dx, y);
        } else {
            p.lineTo(x + dx, y + dy);
            p.lineTo(x + dx, y);
        }
    }

    private void vflap(Path2D p, double x, double y, double dy, double inset, double flapSize) {
        double dx = flapSize;
        p.lineTo(x + dx, y + inset);
        p.lineTo(x + dx, y + (dy - inset));
        p.lineTo(x, y + dy);
    }

    private transient double wFlapSize, hFlapSize, dFlapSize;
    private transient Path2D cutLines, foldLines, interiorCuts;

    private static final double BOTTOM_FLAP_STYLE_THRESHOLD = 28d;

    private static final double SIDE_FLAP_MINIMUM_RATIO = 0.4d;
    private static final double SIDE_FLAP_MAXIMUM_RATIO = 0.8d;
    private static final double SIDE_FLAP_PREFERRED_WIDTH = 28d;

    private static final double FLAP_CUT_INSET = 14d;

    private static final double ARC_TO_WIDTH_RATIO = 5d;
    private static final double ARC_TO_WIDTH_MINIMUM_RATIO = 3d;
    private static final double ARC_MINIMUM = 43d;
    private static final double ARC_MAXIMUM = 57d;

    private static final double ARC_HEIGHT_RATIO = 2d;

    @Override
    public void paint(Graphics2D g, RenderTarget target, double renderResolutionHint) {
        Stroke oldStroke = g.getStroke();
        Paint oldPaint = g.getPaint();
        // rotate by number of quadrants
        int rotation = getOrientation() & 3;

        double rotWidth, rotHeight;
        if (isVertical()) {
            rotWidth = getWidth();
            rotHeight = getHeight();
        } else {
            rotWidth = getHeight();
            rotHeight = getWidth();
        }
        //AffineTransform at = AffineTransform.getQuadrantRotateInstance( rotation, rotWidth/2, rotHeight/2 );
        AffineTransform at = AffineTransform.getQuadrantRotateInstance(rotation);

        // item is mirrored
        if ((getOrientation() & 4) != 0) {
            at.preConcatenate(AffineTransform.getScaleInstance(-1, 1));
            double dx = getWidth();

            if (rotation >= 1 && rotation <= 2) {
                dx = -dx;
            }
            at.preConcatenate(AffineTransform.getTranslateInstance(dx, 0d));
        }

        double tx = getX(), ty = getY();
        if (rotation > 0 && rotation < 3) {
            tx += getWidth();
        }
        if (rotation > 1 && rotation < 4) {
            ty += getHeight();
        }

        at.preConcatenate(AffineTransform.getTranslateInstance(tx, ty));
        AffineTransform oldAt = g.getTransform();
        g.transform(at);

        if (getInteriorFill() != null || getExteriorFill() != null) {
            if (getExteriorFill() != null) {
                g.setColor(getExteriorFill());
                Area exterior = new Area(new Rectangle.Double(0d, 0d, rotWidth, rotHeight));
                exterior.subtract(new Area(cutLines));
                g.fill(exterior);
            }
            if (getInteriorFill() != null) {
                g.setColor(getInteriorFill());
                g.fill(cutLines);
            }
        }

        if (showFoldLines && foldColour != null && foldColour.getAlpha() > 0) {
            g.setColor(foldColour);
            g.setStroke(createFoldStroke());
            g.draw(foldLines);
        }

        if (lineColour != null && lineColour.getAlpha() > 0) {
            g.setColor(lineColour);
            g.setStroke(createLineStroke());
            g.draw(cutLines);
            g.draw(interiorCuts);
        }

        g.setPaint(oldPaint);
        g.setStroke(oldStroke);
        g.setTransform(oldAt);
    }

    private Stroke createLineStroke() {
        return new BasicStroke(lineThickness);
    }

    private Stroke createFoldStroke() {
        return new BasicStroke(lineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[]{lineThickness * 4f, lineThickness * 4f}, 0f);
    }

//	public static void main( String[] args ) {
//
//		Scanner sc = new Scanner( System.in );
//		TuckBox b = new TuckBox();
//
//		for(;;) {
//			double w = sc.nextDouble();
//			if( w < 0 ) break;
//			double h = sc.nextDouble();
//			if( h < 0 ) break;
//			double d = sc.nextDouble();
//			if( d < 0 ) break;
//			b.setDimensions( w, h, d );
//			b.setInteriorFill( null );
//
//
//			BufferedImage i = new BufferedImage( 2*(int)Math.ceil(b.getWidth()), 2*(int)Math.ceil(b.getHeight()), BufferedImage.TYPE_INT_ARGB );
//			Graphics2D g = i.createGraphics();
//			g.scale(2,2);
//			g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
//
//			b.paint( g, 0, 72 );
//			g.dispose();
//			try {
//			ImageIO.write( i, "PNG", new File("c:\\test.png") );
//			} catch( Exception e ) { e.printStackTrace(); }
//
//		}
//	}
    @Override
    public void beginEditing() {
        TuckBoxStyleDialog d = new TuckBoxStyleDialog(StrangeEons.getWindow(), this);
        d.setVisible(true);
    }

    public void setLineThickness(float points) {
        if (points < 0.1f) {
            points = 0.1f;
        }
        lineThickness = points;
    }

    public float getLineThickness() {
        return lineThickness;
    }

    public boolean isThumbNotched() {
        return thumbNotch;
    }

    public void setThumbNotched(boolean thumbNotch) {
        this.thumbNotch = thumbNotch;
        updateBoxShape();
    }

    public Color getInteriorFill() {
        return interiorFill;
    }

    public void setInteriorFill(Color interiorFill) {
        this.interiorFill = interiorFill;
    }

    public Color getExteriorFill() {
        return exteriorFill;
    }

    public void setExteriorFill(Color exteriorFill) {
        this.exteriorFill = exteriorFill;
    }

    public Color getLineColor() {
        return lineColour;
    }

    public void setLineColour(Color lineColour) {
        this.lineColour = lineColour;
    }

    public boolean hasRoundedSideFlaps() {
        return roundedSideFlaps;
    }

    public void setRoundedSideFlaps(boolean roundedSideFlaps) {
        this.roundedSideFlaps = roundedSideFlaps;
        updateBoxShape();
    }

    public boolean hasHingeCut() {
        return hingeCut;
    }

    public void setHingeCut(boolean hingeCut) {
        this.hingeCut = hingeCut;
        updateBoxShape();
    }

    public Color getFoldColour() {
        return foldColour;
    }

    public void setFoldColour(Color foldColour) {
        this.foldColour = foldColour;
        updateBoxShape();
    }

    public boolean hasFoldLines() {
        return showFoldLines;
    }

    public void setFoldLines(boolean foldLines) {
        showFoldLines = foldLines;
        updateBoxShape();
    }

    private static final int TUCK_BOX_ITEM_VERSION = 2;

    @Override
    protected void writeImpl(ObjectOutputStream out) throws IOException {
        super.writeImpl(out);

        out.writeInt(TUCK_BOX_ITEM_VERSION);

        out.writeObject(type);
        out.writeDouble(boxW);
        out.writeDouble(boxH);
        out.writeDouble(boxD);
        out.writeObject(interiorFill);
        out.writeObject(exteriorFill);
        out.writeObject(lineColour);
        out.writeObject(foldColour);
        out.writeFloat(lineThickness);
        out.writeBoolean(thumbNotch);
        out.writeBoolean(roundedSideFlaps);
        out.writeBoolean(hingeCut);
        out.writeBoolean(showFoldLines);
    }

    @Override
    protected void readImpl(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readImpl(in);

        int version = in.readInt();

        if (version >= 2) {
            type = (BoxType) in.readObject();
        } else {
            type = BoxType.TUCK_BOX;
        }
        boxW = in.readDouble();
        boxH = in.readDouble();
        boxD = in.readDouble();
        interiorFill = (Color) in.readObject();
        exteriorFill = (Color) in.readObject();
        lineColour = (Color) in.readObject();
        foldColour = (Color) in.readObject();
        if (version >= 2) {
            lineThickness = in.readFloat();
        } else {
            lineThickness = 1f;
        }
        thumbNotch = in.readBoolean();
        roundedSideFlaps = in.readBoolean();
        hingeCut = in.readBoolean();
        showFoldLines = in.readBoolean();

        updateBoxShape();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeImpl(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readImpl(in);
    }

    private static final long serialVersionUID = 924237498132749834L;
}
