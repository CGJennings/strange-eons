package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;

/**
 * An icon that draws text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class LabelIcon implements Icon {

    private DefaultListCellRenderer jl = new DefaultListCellRenderer();
    private boolean explicitSize = false;
    private boolean explicitBG = false;

    public LabelIcon() {
        jl.setOpaque(false);
        jl.setBorder(BorderFactory.createEmptyBorder());
        jl.setName("LabelIcon");
    }

    public LabelIcon(String text) {
        this();
        setText(text);
    }

    public void setText(String text) {
        jl.setText(text);
        if (!explicitSize) {
            pack();
        }
    }

    public String getText(String text) {
        return jl.getText();
    }

    public void setForeground(Color fg) {
        jl.setForeground(fg);
    }

    public Color getForeground() {
        return jl.getForeground();
    }

    public void setBackground(Color bg) {
        explicitBG = true;
        jl.setBackground(bg);
    }

    public Color getBackground() {
        return jl.getBackground();
    }

    public void setOpaque(boolean opaque) {
        jl.setOpaque(opaque);
    }

    public boolean isOpaque() {
        return jl.isOpaque();
    }

    public void setEnabled(boolean enabled) {
        jl.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return jl.isEnabled();
    }

    public void setIcon(Icon i) {
        jl.setIcon(i);
        if (!explicitSize) {
            pack();
        }
    }

    public Icon getIcon() {
        return jl.getIcon();
    }

    public void setIconWidth(int width) {
        explicitSize = false;
        jl.setSize(width, jl.getHeight());
    }

    public void setIconHeight(int height) {
        explicitSize = false;
        jl.setSize(jl.getWidth(), height);
    }

    @Override
    public int getIconHeight() {
        return jl.getHeight();
    }

    @Override
    public int getIconWidth() {
        return jl.getWidth();
    }

    public void pack() {
        jl.setSize(jl.getPreferredSize());
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (c != null) {
            if (!explicitBG) {
                jl.setBackground(c.getBackground());
            }
            jl.setEnabled(c.isEnabled());
        }
        Graphics gc = g.create(x, y, jl.getWidth(), jl.getHeight());
        try {
            jl.paint(gc);
        } finally {
            gc.dispose();
        }
    }
}
