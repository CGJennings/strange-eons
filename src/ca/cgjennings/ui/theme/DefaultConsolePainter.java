package ca.cgjennings.ui.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JComponent;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Paints the default wave background pattern used by {@link HydraTheme} and
 * {@link AbstractBaseTheme}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
class DefaultConsolePainter extends CachingPainter<JComponent> {

    public DefaultConsolePainter() {
        super((Painter<JComponent>) (Graphics2D g, JComponent o, int w, int h) -> {
            UIDefaults uid = UIManager.getDefaults();
            Color background = uid.getColor(Theme.CONSOLE_BACKROUND);
            if (background == null) {
                background = uid.getColor("nimbusBase");
                if (background == null) {
                    background = new Color(0x34536b);
                }
            }
            Color wave = background.brighter();
            
            Paint p = g.getPaint();
            g.setPaint(background);
            g.fillRect(0, 0, w, h);
            g.setPaint(new LinearGradientPaint(0, h, w, 0, new float[]{0f, 1f}, new Color[]{background, wave}));
            Path2D.Float path = new Path2D.Float();
            int k = Math.min(w, h) / 8;
            path.moveTo(w, h);
            path.lineTo(k, h);
            path.curveTo(w / 4, h / 4, w / 2, h / 2, w, k);
            path.closePath();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fill(path);
            g.setPaint(p);
        });
    }
}
