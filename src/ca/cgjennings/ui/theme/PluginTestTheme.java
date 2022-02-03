package ca.cgjennings.ui.theme;

import ca.cgjennings.graphics.filters.AbstractImageFilter;
import ca.cgjennings.graphics.filters.GammaCorrectionFilter;
import ca.cgjennings.graphics.filters.GreyscaleFilter;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKGROUND_PAINTER;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_ERROR;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_OUTPUT;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_BACKGROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_FOREGROUND;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;

/**
 * The plug-in test theme is a special built-in theme. It is selected
 * automatically when running in plug-in test mode, unless the plug-in being
 * tested is itself a theme. Visually, it is a brown-coloured variant of the
 * {@link HydraTheme}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class PluginTestTheme extends Theme {

    public PluginTestTheme() {
    }

    @Override
    public String getThemeName() {
        return "Plug-in Test";
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        // semi-lazy filter creation: only create these when the theme
        // is actually loaded
        greyFilt = new GreyscaleFilter();
        sepiaFilt = new GammaCorrectionFilter(2d, 1.32d, 0.8d);

        Color base = new Color(0x87_6e2f);
        defaults.put("nimbusBase", base);
        defaults.put("nimbusSelectionBackground", base.darker());
        defaults.put("control", new Color(0xd9_c89e));
        defaults.put("nimbusFocus", new Color(0xbf_6204));

        // these keys are defined by Strange Eons rather than the look and feel
        defaults.put(Theme.EDITOR_TAB_BACKGROUND, new ColorUIResource(0x56_4723));
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
        defaults.put(CONSOLE_BACKROUND, new Color(0x4b_3d28));
        defaults.put(CONSOLE_OUTPUT, new Color(0xe1_f393));
        defaults.put(CONSOLE_ERROR, new Color(0x9d_f939));
        defaults.put(CONSOLE_SELECTION_BACKGROUND, new Color(0xe1_f393));
        defaults.put(CONSOLE_SELECTION_FOREGROUND, new Color(0x4b_3d28));
        defaults.put(CONSOLE_BACKGROUND_PAINTER, new HydraConsolePainter());
    }

    @Override
    public BufferedImage applyThemeToImage(BufferedImage bi) {
        // large icons (probably banners) pass through unchanged
        if (bi.getWidth() > 128 || bi.getHeight() > 128) {
            if (bi.getWidth() != bi.getHeight()) {
                return bi;
            }
        }
        // creates a copy and desaturates at the same time
        bi = greyFilt.filter(bi, null);
        // filters the copy without a temp image
        sepiaFilt.filter(bi, bi);
        return bi;
    }

    private AbstractImageFilter greyFilt;
    private AbstractImageFilter sepiaFilt;
}
