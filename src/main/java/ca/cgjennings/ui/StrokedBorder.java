package ca.cgjennings.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;

/**
 * A border that displays a dashed line around a component.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class StrokedBorder extends AbstractBorder {

    private float[] dashArray;
    private float dashPhase;
    private float penSize;
    private Color strokeColor;
    private int cap;
    private int join;

    private BasicStroke stroke;

    public StrokedBorder() {
        this(Color.GRAY, 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, new float[]{2f, 2f}, 0f);
    }

    public StrokedBorder(Color strokeColor, float penSize, int cap, int join, float[] dashArray, float dashPhase) {
        this.strokeColor = strokeColor;
        this.penSize = penSize;
        this.cap = cap;
        this.join = join;
        this.dashArray = dashArray;
        this.dashPhase = dashPhase;
    }

    public float[] getDashArray() {
        return dashArray.clone();
    }

    /**
     * Sets the dash array for the border; may be null for a solid line.
     *
     * @param dashArray
     */
    public void setDashArray(float[] dashArray) {
        if (dashArray == null) {
            this.dashArray = null;
        } else {
            this.dashArray = dashArray.clone();
        }
        stroke = null;
    }

    public float getDashPhase() {
        return dashPhase;
    }

    public void setDashPhase(float dashPhase) {
        this.dashPhase = dashPhase;
        stroke = null;
    }

    public float getPenSize() {
        return penSize;
    }

    public void setPenSize(float penSize) {
        this.penSize = penSize;
        stroke = null;
    }

    public int getCap() {
        return cap;
    }

    public void setCap(int cap) {
        this.cap = cap;
        stroke = null;
    }

    public int getJoin() {
        return join;
    }

    public void setJoin(int join) {
        this.join = join;
        stroke = null;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        if (strokeColor == null) {
            throw new NullPointerException();
        }
        this.strokeColor = strokeColor;
        stroke = null;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, null);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int inset = (int) Math.ceil(penSize);
//		if( rounded ) inset *= 2 ;
        if (insets == null) {
            insets = new Insets(inset, inset, inset, inset);
        } else {
            insets.set(inset, inset, inset, inset);
        }
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g1, int x, int y, int width, int height) {
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (stroke == null) {
            stroke = new BasicStroke(penSize, cap, join, penSize, dashArray, dashPhase);
        }
        g.setColor(strokeColor);
        g.setStroke(stroke);
        final int ps = (int) penSize;
        final int ps2 = (int) (penSize / 2);
        g.drawRect(x + ps2, y + ps2, width - ps, height - ps);
    }
}
