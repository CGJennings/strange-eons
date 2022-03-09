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
 * The Hydra theme is a built-in theme featuring blue highlights and an abstract
 * wave motif.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public class HydraTheme extends Theme {

    public HydraTheme() {
    }

    @Override
    public String getThemeName() {
        return "Hydra";
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        defaults.put(CONSOLE_BACKROUND, new Color(0x34536b));
        defaults.put(CONSOLE_OUTPUT, new Color(0xf3e193));
        defaults.put(CONSOLE_ERROR, new Color(0xf99d39));
        defaults.put(CONSOLE_SELECTION_BACKGROUND, new Color(0xf3e193));
        defaults.put(CONSOLE_SELECTION_FOREGROUND, new Color(0x3d4b28));
        defaults.put(CONSOLE_BACKGROUND_PAINTER, new HydraConsolePainter());
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
        defaults.put("TitledBorder.titleColor", new Color(0x3d4b28));
    }

    @Override
    public String getThemeGroup() {
        return "\udbff\udfff_98";
    }

    /**
     * Paints the default wave background pattern used by {@link HydraTheme}.
     */
    static class HydraConsolePainter extends CachingPainter<JComponent> {

        public HydraConsolePainter() {
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
}
