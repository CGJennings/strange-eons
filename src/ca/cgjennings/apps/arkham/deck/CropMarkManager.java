package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.sheet.MarkerStyle;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes card collections to determine where crop marks should be drawn.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 1.65
 */
final class CropMarkManager {

    public CropMarkManager() {
        this(1f, Color.BLACK);
    }

    public CropMarkManager(float penWidth, Color penColor) {
        markSet = new ArrayList<>(256);
        setMarkWidth(penWidth);
        setMarkColor(penColor);
    }

    public void setMarkWidth(float penWidth) {
        if (markWidth == penWidth) {
            return;
        }
        markWidth = penWidth;
        halfWidth = markWidth / 2d;
        cropStroke = new BasicStroke(penWidth);
        float dashLen = Math.max(2f, penWidth * 2f);
        foldStroke = new BasicStroke(penWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, new float[]{dashLen, dashLen}, 0f);
    }

    public void setMarkColor(Color penColor) {
        cropColor = penColor;
        foldColor = penColor;
    }

    public void setMarkSize(double distance, double length) {
        if (distance < 0) {
            throw new IllegalArgumentException("negative distance: " + distance);
        }
        if (length <= 0) {
            throw new IllegalArgumentException("nonpositive length: " + length);
        }
        CROP_LENGTH = length;
        CROP_POS_OFFSET = distance;
        CROP_NEG_OFFSET = distance + length;
    }

    public void setEnabled(boolean showMarks) {
        enabled = showMarks;
    }

    public boolean getEnabled() {
        return enabled;
    }
    private double markWidth;
    private double halfWidth;

    /**
     * Process the cards on the page to determine the set of crop marks that
     * should be applied.
     */
    public void update(Page page) {
        markSet.clear();

        // add crop marks to cards if another card does not overlap us;
        // if the card is related (the second sheet of a given component) to a
        // card it is edge-aligned with, create a fold mark (promote the usual
        // mark into a fold mark).
        for (int i = 0; i < page.getCardCount(); ++i) {
            PageItem c = page.getCard(i);

            // add implicit crop marks (possibly promoting to fold marks)
            if (c.isBleedMarginMarked()) {
                double bm = c.getBleedMargin();
                boolean hasMargin = bm != 0d;
                double x1 = c.getX(), y1 = c.getY(), x2 = x1 + c.getWidth(), y2 = y1 + c.getHeight();

                // vertical marks
                maybeAddCropMark(page, c, x1 + bm, y1 - CROP_NEG_OFFSET, 0d, CROP_LENGTH, Mark.TYPE_CUT, hasMargin);
                maybeAddCropMark(page, c, x1 + bm, y2 + CROP_POS_OFFSET, 0d, CROP_LENGTH, Mark.TYPE_CUT, hasMargin);
                maybeAddCropMark(page, c, x2 - bm, y1 - CROP_NEG_OFFSET, 0d, CROP_LENGTH, Mark.TYPE_CUT, hasMargin);
                maybeAddCropMark(page, c, x2 - bm, y2 + CROP_POS_OFFSET, 0d, CROP_LENGTH, Mark.TYPE_CUT, hasMargin);

                // horizontal marks
                maybeAddCropMark(page, c, x1 - CROP_NEG_OFFSET, y1 + bm, CROP_LENGTH, 0d, Mark.TYPE_CUT, hasMargin);
                maybeAddCropMark(page, c, x2 + CROP_POS_OFFSET, y1 + bm, CROP_LENGTH, 0d, Mark.TYPE_CUT, hasMargin);
                maybeAddCropMark(page, c, x1 - CROP_NEG_OFFSET, y2 - bm, CROP_LENGTH, 0d, Mark.TYPE_CUT, hasMargin);
                maybeAddCropMark(page, c, x2 + CROP_POS_OFFSET, y2 - bm, CROP_LENGTH, 0d, Mark.TYPE_CUT, hasMargin);
            }
        }

        // add explicit fold marks requested by the card (e.g. Foldable Tome)
        // the marks are provided by the component in the form of an array of
        // unit vectors (in [x0, y0, x1, y1, ...] order).
        for (int i = 0; i < page.getCardCount(); ++i) {
            PageItem c = page.getCard(i);

            final double[] marks = c.getFoldMarks();
            if (marks != null) {
                Rectangle2D.Double r = new Rectangle2D.Double();
                for (int m = 0; m < marks.length; m += 4) {
                    int type = Mark.TYPE_FOLD;
                    if (Double.isNaN(marks[m])) {
                        type = Mark.TYPE_CUT;
                        ++m;
                    }

                    // point relative to width/height of card
                    final double px = marks[m];
                    final double py = marks[m + 1];

                    // unit vector from (px,py)
                    final double dx = marks[m + 2];
                    final double dy = marks[m + 3];

                    // (x0,y0) is the position on the card edge indicated by
                    // the first point (e.g., 0.5, 0 = middle of top edge)
                    final double x0 = c.getX() + c.getWidth() * px;
                    final double y0 = c.getY() + c.getHeight() * py;

                    // using the unit vector to define a parametric line,
                    // we find the line segment occupied by the fold mark:
                    final double x1 = x0 + (dx * CROP_POS_OFFSET);
                    final double y1 = y0 + (dy * CROP_POS_OFFSET);
                    final double x2 = x0 + (dx * (CROP_POS_OFFSET + CROP_LENGTH));
                    final double y2 = y0 + (dy * (CROP_POS_OFFSET + CROP_LENGTH));

                    r.setFrameFromDiagonal(x1, y1, x2, y2);
                    maybeAddCropMark(page, c, r.x, r.y, r.width, r.height, type, false);
                }
            }
        }
    }

    private void maybeAddCropMark(Page page, PageItem card, double x1, double y1, double w, double h, int type, boolean hasBleedMargin) {
        Rectangle2D.Double cropRect;
        if (w == 0) {
            cropRect = new Rectangle2D.Double(x1 - halfWidth, y1, markWidth, h);
        } else if (h == 0) {
            cropRect = new Rectangle2D.Double(x1, y1 - halfWidth, w, markWidth);
        } else {
            cropRect = new Rectangle2D.Double(x1 - halfWidth, y1 - halfWidth, w + markWidth, h + markWidth);
        }

        // eliminate crop mark if it would overlap with a card (just the card,
        // it is OK if it would overlap another crop mark)
        for (int i = 0; i < page.getCardCount(); ++i) {
            PageItem target = page.getCard(i);
            if (card == target) {
                continue;
            }
            if (cropRect.intersects(target.getRectangle())) {
                return;
            }
        }

        // if this crop mark is already in the set of marks, there are four possible actions:
        //   (0) if this card has a bleed margin, the original mark is not touched
        //   (1) if the current and existing marks are of different types, ensure
        //       the existing mark is a fold mark
        //   (2) if the current mark is for a different sheet of the same component and
        //       the two sheets are the same size and they are edge-aligned,
        //       change the existing mark to a fold mark (for investigators, two copies
        //       of sheet 3 (character marker) also work). if the sheets are not the same
        //       then one must have an even (front) index, and one must have an odd (back)
        //       index.
        //   (3) otherwise, leave the existing mark as is
        // in any case where the mark already exists, we do not add the current mark
        boolean addMark = true;
        for (int i = 0; i < markSet.size(); ++i) {
            Mark targetMark = markSet.get(i);
            if (!targetMark.matches(cropRect)) {
                continue;
            }

            addMark = false;
            PageItem targetCard = targetMark.card;

            // (0)
            if (hasBleedMargin) {
                break;
            }

            // (1)
            if (type != targetMark.type) {
                targetMark.type = Mark.TYPE_FOLD;
                break;
            }

            // (2)
            // eliminate any cases that do not match the criteria for (2), which
            //   will effectively perform (3) instead
            if (!(card instanceof CardFace) || !(targetCard instanceof CardFace)) {
                break;
            }

            CardFace face = (CardFace) card;
            CardFace targetFace = (CardFace) targetCard;
            int i1 = face.getSheetIndex();
            int i2 = targetFace.getSheetIndex();

            if (!face.getPath().equals(targetFace.getPath())) {
                break;
            }
//			boolean mustBeMirroredForFoldMark = true;
            if (i1 == i2) {
                MarkerStyle style = face.getSheet().getMarkerStyle();
                if (style == null || style == MarkerStyle.ONE_SIDED) {
                    break;
                }
//				if( style == MarkerStyle.COPIED ) mustBeMirroredForFoldMark = false;
            }
            if (i1 != i2) {
                if (i2 < i1) {
                    int t = i1;
                    i1 = i2;
                    i2 = t;
                }
                if (((i1 & 1) == 1) || ((i2 & 1) == 0)) {
                    break;
                }
            }
            if (abs(card.getWidth() - targetCard.getWidth()) >= EPSILON) {
                break;
            }
            if (abs(card.getHeight() - targetCard.getHeight()) >= EPSILON) {
                break;
            }

            // eliminate cases where cards are not aligned for folding:
            //    if cards are aligned along a left or right edge, they must
            //      have the same orientation
            //    if cards are aligned along a top or bottom edge, the
            //      second must be rotated 180 degrees relative to the first
            // cards are aligned in a row
            if (abs(card.getY() - targetCard.getY()) < EPSILON) {
                if (card.isVertical()) {
                    if (!card.isTurned0DegreesFrom(targetCard)) {
                        break;
                    }
                } else {
                    if (!card.isTurned180DegreesFrom(targetCard)) {
                        break;
                    }
                }
            } // cards are aligned in a column
            else if (abs(card.getX() - targetCard.getX()) < EPSILON) {
                if (card.isVertical()) {
                    if (!card.isTurned180DegreesFrom(targetCard)) {
                        break;
                    }
                } else {
                    if (!card.isTurned0DegreesFrom(targetCard)) {
                        break;
                    }
                }
            }

            // two different sheets from the same card with the same width and
            // height share a crop mark: presumably the user intends this to be a fold!
            targetMark.type = Mark.TYPE_FOLD;
            break;
        }

        // if the mark was not eliminated as a duplicate, add it to the list
        if (addMark) {
            markSet.add(new Mark(card, cropRect, x1, y1, x1 + w, y1 + h, type));
        }
    }

    /**
     * Paint the current set of crop marks in an appropriately transformed
     * graphics context.
     */
    public void paint(Graphics2D g) {
        if (!enabled) {
            return;
        }

        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        try {
            for (int i = 0; i < markSet.size(); ++i) {
                Mark mark = markSet.get(i);

                if (mark.type == Mark.TYPE_CUT) {
                    g.setStroke(cropStroke);
                    g.setColor(cropColor);
                } else {
                    g.setStroke(foldStroke);
                    g.setColor(foldColor);
                }
                g.draw(mark.line);
            }
        } finally {
            g.setPaint(oldPaint);
            g.setStroke(oldStroke);
        }
    }

    /**
     * Represents a single crop mark in the manager's computed mark set.
     */
    private static class Mark {

        public Mark(PageItem card, Rectangle2D rect, double x1, double y1, double x2, double y2, int type) {
            this.card = card;
            this.rect = rect;
            this.type = type;
            line = new Line2D.Double(x1, y1, x2, y2);
            this.x = x1;
            this.y = y1;
        }
        PageItem card;
        Rectangle2D rect;
        Line2D line;
        int type;
        double x, y;
        /**
         * A crop mark indicating a cut line.
         */
        public static final int TYPE_CUT = 0;
        /**
         * A crop mark indicating a fold line.
         */
        public static final int TYPE_FOLD = 1;

        public boolean matches(Rectangle2D rhs) {
            Rectangle2D lhs = rect;

            return (abs(lhs.getX() - rhs.getX()) < EPSILON)
                    && (abs(lhs.getY() - rhs.getY()) < EPSILON)
                    && (abs(lhs.getWidth() - rhs.getWidth()) < EPSILON)
                    && (abs(lhs.getHeight() - rhs.getHeight()) < EPSILON);
        }
    }

    private List<Mark> markSet;
    protected Stroke cropStroke;
    protected Stroke foldStroke;
    protected Color cropColor;
    protected Color foldColor;
    protected boolean enabled;
    private static double CROP_NEG_OFFSET = 18d, CROP_POS_OFFSET = 4d, CROP_LENGTH = 14d;
    private static double EPSILON = 0.001d;
}
