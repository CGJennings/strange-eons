package ca.cgjennings.layout;

import java.awt.Graphics2D;
import java.awt.font.TextLayout;

/**
 * A TextLayout together with the positional information needed to draw it in
 * the position it was placed by a markup renderer. The renderer does this by
 * appending a sequence of segments that represent what it would have drawn if
 * it had been drawing. Note that a line may consist of multiple segments.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class LayoutSegment {

    public LayoutSegment(TextLayout layout, float xOffset, float yOffset, float yBottom, boolean startOfLine, int sourceParagraph) {
        this.layout = layout;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.yBottom = yBottom;
        this.startOfLine = startOfLine;
        this.sourceParagraph = sourceParagraph;
    }

    public float getAscent() {
        return layout.getAscent();
    }

    public float getDescent() {
        return layout.getDescent();
    }

    public float getXOffset() {
        return xOffset;
    }

    public float getYOffset() {
        return yOffset;
    }

    public float getYBottom() {
        return yBottom;
    }

    public boolean isStartOfLine() {
        return startOfLine;
    }

    public int getSourceParagraphIndex() {
        return sourceParagraph;
    }

    public void draw(Graphics2D g) {
        layout.draw(g, xOffset, yOffset);
    }

    public void draw(Graphics2D g, float dx, float dy) {
        layout.draw(g, xOffset + dx, yOffset + dy);
    }

    public void translate(double dx, double dy) {
        xOffset += dx;
        yOffset += dy;
    }

    private TextLayout layout;
    private float xOffset, yOffset, yBottom;
    private boolean startOfLine;
    private int sourceParagraph;
}
