package ca.cgjennings.ui.theme;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;
import resources.ResourceKit;

/**
 * Icon that paints a pair of icons overtop of each other.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ThemedCompoundIcon implements ThemedIcon {
    private ThemedIcon bottom, top;

    /**
     * Create a compound icon from a pair of icons.
     * 
     * @param bottom the icon to paint first
     * @param top the icon to paint over the bottom icon
     */
    public ThemedCompoundIcon(ThemedIcon bottom, ThemedIcon top) {
        this.bottom = Objects.requireNonNull(bottom, "bottom");
        this.top = Objects.requireNonNull(top, "top");
    }
    
    /**
     * Create a compound icon from a pair of icon to be fetched with
     * {@link ResourceKit#getIcon}.
     * 
     * @param bottom the icon to paint first
     * @param top the icon to paint over the bottom icon
     */
    public ThemedCompoundIcon(String bottom, String top) {
        this.bottom = ResourceKit.getIcon(bottom);
        this.top = ResourceKit.getIcon(top);
    }
    
    @Override
    public ThemedIcon derive(int width, int height) {
       if (width == bottom.getIconWidth() && height == bottom.getIconHeight()) {
           return this;
       }
       return new ThemedCompoundIcon(bottom.derive(width, height), top.derive(width, height));
    }

    @Override
    public ThemedIcon disabled() {
        return new ThemedCompoundIcon(bottom.disabled(), top.disabled());
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // avoid reading size until necessary, to avoid loading unused resources
        if (top.getIconWidth() != bottom.getIconWidth() || top.getIconHeight() != bottom.getIconHeight()) {
            top = top.derive(bottom.getIconWidth(), bottom.getIconHeight());
        }
        
        bottom.paintIcon(c, g, x, y);
        top.paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return bottom.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return bottom.getIconHeight();
    }    
}
