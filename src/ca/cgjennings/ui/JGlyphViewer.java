package ca.cgjennings.ui;

import ca.cgjennings.graphics.paints.CheckeredPaint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.swing.JLabel;

/**
 * A simple viewer that shows a single glyph from a font.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JGlyphViewer extends JLabel {

    public JGlyphViewer() {
        super();
        setBackground(Color.WHITE);
        setForeground(Color.BLACK);
        setText("A");
    }

    private final Paint transparencyPaint = new CheckeredPaint();

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        insets = getInsets(insets);
        int t = insets.top;
        int l = insets.left;
        int w = getWidth() - insets.left - insets.right;
        int h = getHeight() - insets.top - insets.bottom;
        if (isOpaque()) {
            g.setPaint(transparencyPaint);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        paintBorder(g1);

        String text = getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        Font f = getFont();
        GlyphVector gv = f.createGlyphVector(frc, text);
        Shape outline = gv.getGlyphOutline(0);
        Rectangle2D bounds = outline.getBounds2D();

        AffineTransform oldAT = g.getTransform();
        g.translate(t, l);

        double scale = Math.min(w / bounds.getWidth(), h / bounds.getHeight());
        g.scale(scale, scale);
        double scw = (double) w * 1 / scale;
        double sch = (double) h * 1 / scale;
        g.translate(-bounds.getX() + (scw - bounds.getWidth()) / 2, -bounds.getY() + (sch - bounds.getHeight()) / 2);

        g.setStroke(new BasicStroke(2f/(float)scale));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.draw(outline);

        g.setPaint(getBackground());
        g.fill(outline);
        g.setPaint(getForeground());
        g.setTransform(oldAT);
    }
    private Insets insets;
    private static final FontRenderContext frc = new FontRenderContext(AffineTransform.getScaleInstance(150, 150), true, true);
}
