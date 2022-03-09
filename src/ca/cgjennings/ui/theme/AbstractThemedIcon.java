package ca.cgjennings.ui.theme;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Abstract base class for themed icons.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractThemedIcon implements ThemedIcon {
    protected int width = SMALL;
    protected int height = SMALL;
    protected boolean disabled;
    
    @Override
    public ThemedIcon disabled() {
        if (disabled) return this;
        
        // temporarily change the icon size so we derive a new instance
        ++width;
        AbstractThemedIcon icon = (AbstractThemedIcon) derive(width-1, height);
        --width;
        icon.disabled = true;
        return icon;
    }
    
    @Override
    public final void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Object oldTerp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // keep bicubic interpolation if that's been set, otherwise compromise with bilinear
        if (oldTerp != RenderingHints.VALUE_INTERPOLATION_BICUBIC) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        }
        if (disabled || (c != null && !c.isEnabled())) {
            Composite oldComp = g2.getComposite();
            g2.setComposite(DISABLED_COMPOSITE);
            paintIcon(c, g2, x, y);
            g2.setComposite(oldComp);
        } else {
            paintIcon(c, g2, x, y);
        }
        if (oldAA != null) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
        if (oldTerp != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldTerp);
        }
    }
    
    protected abstract void paintIcon(Component c, Graphics2D g, int x, int y);

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
    
    static final Composite DISABLED_COMPOSITE = AlphaComposite.SrcOver.derive(0.4f);
    
    @Override
    public String toString() {
        return "ThemedIcon: size=" + getIconWidth() + 'x' + getIconHeight();
    }
}
