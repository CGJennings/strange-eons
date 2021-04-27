package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.logging.Level;
import resources.ResourceKit;

/**
 * Prints large virtual pages by tiling them over multiple physical pages.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class PaperSplitter {

    private double[] xOffsets;
    private double[] yOffsets;
    private PaperProperties virtual;
    private PaperProperties physical;

    private boolean printableBounded;
    private float printableFrameWidth;
    private Color printableFrameColor;
    private boolean printableNumbered;
    private int lastPhysicalPage;

    /**
     * Create a layout for splitting a virtual page over one or more physical
     * ones. If the two paper types are the same, returns a special
     * configuration that maps the two pages to each other exactly. Otherwise, a
     * layout is created as follows:
     * <p>
     * The printable area of each page is taken to be its margin. The upper-left
     * corner of the document is mapped to the upper-left margin of the first
     * page. The following pages assume that only the area within the margins
     * was printed, and adjust the page to line up that edge with the margin of
     * the next page.
     *
     * @param virtualPaper
     * @param physicalPaper
     */
    public PaperSplitter(PaperProperties virtualPaper, PaperProperties physicalPaper) {
        physical = physicalPaper;
        virtual = virtualPaper;
        printableBounded = true;
        printableFrameWidth = 0f;
        printableFrameColor = Color.BLACK;
        printableNumbered = true;
        lastPhysicalPage = 0;
        createLayout();
    }

    /**
     * Compute the mapping of virtual to physical pages. This is called from the
     * constructor after this instance is initialized.
     */
    private void createLayout() {
        double margin = physical.getMargin();
        double innerWidth = physical.getPageWidth() - margin * 2d;
        double innerHeight = physical.getPageHeight() - margin * 2d;

        int xPages = (int) Math.ceil(virtual.getPageWidth() / innerWidth);
        int yPages = (int) Math.ceil(virtual.getPageHeight() / innerHeight);
        xOffsets = new double[xPages];
        yOffsets = new double[yPages];

        // spread the unused space over both ends instead of putting it
        // all on the right/bottom edges; basically this means centering
        // the virtual page over the physical pages
        double widthUsed = xPages * innerWidth;
        double heightUsed = yPages * innerHeight;
        double xBias = (widthUsed - virtual.getPageWidth()) / 2d;
        double yBias = (heightUsed - virtual.getPageHeight()) / 2d;

        // start at the inside corner of the margin, offset by the bias
        // needed to center the image
        double xOff = margin + xBias, yOff = margin + yBias;
        for (int i = 0; i < xPages; ++i) {
            xOffsets[i] = xOff;
            xOff -= innerWidth;
        }

        for (int i = 0; i < yPages; ++i) {
            yOffsets[i] = yOff;
            yOff -= innerHeight;
        }
    }

    /**
     * Returns the offset to translate the virtual graphics context by to map it
     * to a physical page.
     *
     * @param physicalPage the physical page being printed
     * @return the amount by which to translate the virtual page before printing
     */
    public Point2D getPrintOffsetForPage(int physicalPage) {
        return new Point2D.Double(
                xOffsets[getPhysicalColumn(physicalPage)],
                yOffsets[getPhysicalRow(physicalPage)]
        );
    }

    /**
     * Given a physical page number, determine the number of the virtual page
     * being printed (assuming that they are all the same size). Essentially,
     * this is <code>floor(physicalPage/getPhysicalPagesPerPage())</code>.
     *
     * @return the virtual page number
     */
    public int getVirtualPageForPage(int physicalPage) {
        return physicalPage / getPhysicalPagesPerPage();
    }

    /**
     * Given a physical page number in the final printed document, returns its
     * tile column.
     *
     * @param physicalPage the physical page number
     * @return the tile column of the page
     */
    public int getPhysicalColumn(int physicalPage) {
        physicalPage %= getPhysicalPagesPerPage();
        return physicalPage % xOffsets.length;
    }

    /**
     * Given a physical page number in the final printed document, returns its
     * tile row.
     *
     * @param physicalPage the physical page number
     * @return the tile row of the page
     */
    public int getPhysicalRow(int physicalPage) {
        physicalPage %= getPhysicalPagesPerPage();
        return physicalPage / xOffsets.length;
    }

    /**
     * Returns the number of columns of physical pages required for each virtual
     * page.
     *
     * @return the number of columns of tiled pages
     */
    public int getPhysicalColumns() {
        return xOffsets.length;
    }

    /**
     * Returns the number of rows of physical pages required for each virtual
     * page.
     *
     * @return the number of rows of tiled pages
     */
    public int getPhysicalRows() {
        return yOffsets.length;
    }

    /**
     * Returns the number of physical pages required for each virtual page.
     *
     * @return the number of printed pages required to tile a single virtual
     * page
     */
    public int getPhysicalPagesPerPage() {
        return xOffsets.length * yOffsets.length;
    }

    /**
     * Returns the total number of physical pages needed to print a document.
     *
     * @return the number of pages required to print
     * <code>virtualPageCount</code> virtual pages
     */
    public int getTotalPhysicalPagesRequired(int virtualPageCount) {
        if (virtualPageCount < 0) {
            throw new IllegalArgumentException("virtualPageCount < 0: " + virtualPageCount);
        }
        return virtualPageCount * getPhysicalPagesPerPage();
    }

    /**
     * Returns a printable capable of printing physical pages by delegating to a
     * printable that prints virtual pages. If the virtual printable takes up
     * more than one physical page, then it will be printed multiple times using
     * different translation and clip settings to cover the matching physical
     * pages.
     */
    public Printable createPrintable(final Printable virtualPrintable) {
        Printable p = (Graphics graphics, PageFormat pageFormat, int pageIndex) -> {
            lastPhysicalPage = pageIndex;
            int virtualIndex = getVirtualPageForPage(pageIndex);
            Point2D delta = getPrintOffsetForPage(pageIndex);
            
            StrangeEons.log.log(Level.INFO, "printing virtual page {0} on physical page {1}", new Object[]{virtualIndex, pageIndex});
            
            Graphics2D g = (Graphics2D) graphics;
            
            double physW = physical.getPageWidth();
            double physH = physical.getPageHeight();
            double m = physical.getMargin();
            
            // draw framing lines
            Stroke oldStroke = g.getStroke();
            Paint oldPaint = g.getPaint();
            if (printableBounded) {
                g.setPaint(Color.GRAY);
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                double x1 = m - 0.5d, y1 = m - 0.5d, x2 = physW - m + 0.5d, y2 = physH - m + 0.5d;
                Line2D.Double line = new Line2D.Double(0, y1, physW, y1);
                g.draw(line);
                line.setLine(0, y2, physW, y2);
                g.draw(line);
                line.setLine(x1, 0, x1, physH);
                g.draw(line);
                line.setLine(x2, 0, x2, physH);
                g.draw(line);
            }
            
            Shape oldClip = g.getClip();
            AffineTransform oldAT = g.getTransform();
            
            g.clip(new Rectangle2D.Double(m / 2d, m / 2d, physical.getPageWidth() - m, physical.getPageHeight() - m));
            g.translate(delta.getX(), delta.getY());
            
            if (printableFrameWidth > 0f) {
                float penW = printableFrameWidth;
                g.setPaint(printableFrameColor);
                g.setStroke(new BasicStroke(penW, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                g.draw(new Rectangle2D.Double(-penW / 2d, -penW / 2d, virtual.getPageWidth() + penW, virtual.getPageHeight() + penW));
            }
            
            g.setPaint(oldPaint);
            g.setStroke(oldStroke);
            g.clip(new Rectangle2D.Double(0d, 0d, virtual.getPageWidth(), virtual.getPageHeight()));
            
            int pageExists = virtualPrintable.print(graphics, virtual.createCompatiblePageFormat(false), virtualIndex);
            
            if (printableNumbered) {
                g.setTransform(oldAT);
                g.setClip(oldClip);
                Font f = g.getFont();
                String pageNum = String.format(
                        "%d [%d|%d]",
                        getVirtualPageForPage(pageIndex) + 1,
                        getPhysicalRow(pageIndex) + 1,
                        getPhysicalColumn(pageIndex) + 1
                );
                Font pageNumFont = ResourceKit.getBodyFont().deriveFont(10f);
                g.setFont(pageNumFont);
                FontMetrics fm = g.getFontMetrics();
                Rectangle2D bounds = fm.getStringBounds(pageNum, g);
                GlyphVector gv = pageNumFont.createGlyphVector(g.getFontRenderContext(), pageNum);
                Shape outline = gv.getOutline((float) (physW - m - bounds.getWidth()), (float) (m - fm.getDescent() - 2));
                
                g.setStroke(new BasicStroke(1f));
                g.setPaint(Color.WHITE);
                g.draw(outline);
                g.setPaint(Color.BLACK);
                g.fill(outline);
                g.setFont(f);
            }
            
            return pageExists;
        };
        return p;
    }

    /**
     * Returns the index of the last physical page that was printed using a
     * Printable instance created with
     * {@link #createPrintable(java.awt.print.Printable)}. This is provided
     * mainly for debugging from within the virtual printable's
     * <code>print</code> method.
     *
     * @return the physical page last printed by a printable created from this
     * splitter
     */
    public int getPhysicalPageBeingPrinted() {
        return lastPhysicalPage;
    }

    public boolean isPrintableBounded() {
        return printableBounded;
    }

    public void setPrintableBounded(boolean printableBounded) {
        this.printableBounded = printableBounded;
    }

    public float getPrintableFrameWidth() {
        return printableFrameWidth;
    }

    public void setPrintableFrameWidth(float printableFrameWidth) {
        this.printableFrameWidth = printableFrameWidth;
    }

    public Color getPrintableFrameColor() {
        return printableFrameColor;
    }

    public void setPrintableFrameColor(Color printableFrameColor) {
        this.printableFrameColor = printableFrameColor;
    }
}
