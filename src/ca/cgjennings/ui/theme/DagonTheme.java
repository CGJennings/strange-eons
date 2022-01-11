package ca.cgjennings.ui.theme;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.BlurFilter;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKGROUND_PAINTER;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_ERROR;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_OUTPUT;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_BACKGROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_FOREGROUND;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import resources.ResourceKit;

/**
 * The Dagon theme is a built-in theme that is a subtle grey-green hue, with
 * orange highlights and an octopus motif.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public class DagonTheme extends Theme {

    public DagonTheme() {
    }

    @Override
    public String getThemeName() {
        return "Dagon";
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        defaults.put(EDITOR_TAB_BACKGROUND, new Color(0xD4_D7D0));
        defaults.put("nimbusBase", new Color(0x56_6924));
        defaults.put("nimbusSelection", new Color(0xBF_6204));
        defaults.put("nimbusSelectionBackground", new Color(0xBF_6204));
        defaults.put("control", new Color(0xBE_C1B4));
        defaults.put("nimbusFocus", new Color(0xe5_9900));
        defaults.put("info", new Color(0xF1_DFBD));
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
        Color CON_BACKGROUND = new Color(0x3d_4b28);
        Color CON_TEXT = new Color(0xf3_e193);
        Color CON_ERROR_TEXT = new Color(0xf9_9d39);
        Color CON_SELECTION = new Color(0xf3_e193);
        Color CON_SELECTION_TEXT = new Color(0x3d_4b28);

        defaults.put(CONSOLE_BACKROUND, CON_BACKGROUND);
        defaults.put(CONSOLE_OUTPUT, CON_TEXT);
        defaults.put(CONSOLE_ERROR, CON_ERROR_TEXT);
        defaults.put(CONSOLE_SELECTION_BACKGROUND, CON_SELECTION);
        defaults.put(CONSOLE_SELECTION_FOREGROUND, CON_SELECTION_TEXT);
        defaults.put(CONSOLE_BACKGROUND_PAINTER, new CachingPainter<>(new BackdropPainter()));
        defaults.put("DesktopPane[Enabled].backgroundPainter", new CachingPainter<>(new BackdropPainter()));
    }

    private static class BackdropPainter implements Painter<JComponent> {

        final Color CON_BACKGROUND = new Color(0x3d_4b28);
        final Color CON_BACKGROUND_OUTER = CON_BACKGROUND.darker();
        final Color CON_BACKGROUND_INNER = CON_BACKGROUND.brighter();
        final Color CON_BACKGROUND_INNER2 = CON_BACKGROUND_INNER.brighter();
        BufferedImage stencil;

        @Override
        public void paint(Graphics2D g, JComponent o, int w, int h) {
            if (stencil == null) {
                stencil = ResourceKit.getImage("icons/octopus.png");
                if (o instanceof JDesktopPane) {
                    int lpad = 0, tpad;
                    tpad = stencil.getWidth() - stencil.getHeight();
                    if (tpad < 0) {
                        lpad = -tpad;
                        tpad = 0;
                    }

                    int rpad = lpad / 2;
                    lpad -= rpad;

                    int bpad = tpad / 2;
                    tpad -= bpad;

                    stencil = ImageUtilities.pad(stencil, 12 + tpad, 12 + lpad, 12 + bpad, 12 + rpad);
                    stencil = new BlurFilter(6, 6, 2).filter(stencil, null);
                }
            }

            Composite c = g.getComposite();
            Paint p = g.getPaint();
            int cx = w / 2, cy = h / 2;

            if (o instanceof JDesktopPane) {
                g.setPaint(CON_BACKGROUND_OUTER);
                g.fillRect(0, 0, w, h);
                int size = Math.max(w, h) * 80 / 100;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.075f));
                g.drawImage(stencil, cx - size / 2, cy - size / 2, size, size, null);
            } else {
                RadialGradientPaint rgp = new RadialGradientPaint(
                        cx, cy, Math.max(w, h),
                        new float[]{0f, 0.05f, 0.2f, 0.8f, 1f},
                        new Color[]{CON_BACKGROUND_INNER2, CON_BACKGROUND_INNER, CON_BACKGROUND, CON_BACKGROUND_OUTER, CON_BACKGROUND_OUTER}
                );
                g.setPaint(rgp);
                g.fillRect(0, 0, w, h);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                g.drawImage(stencil, cx - stencil.getWidth() / 2, cy - stencil.getHeight() / 2, null);
            }
            g.setPaint(p);
            g.setComposite(c);
        }
    }
}
