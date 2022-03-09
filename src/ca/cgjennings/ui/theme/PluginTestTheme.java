package ca.cgjennings.ui.theme;

import ca.cgjennings.graphics.filters.AbstractPixelwiseFilter;
import ca.cgjennings.graphics.filters.GammaCorrectionFilter;
import ca.cgjennings.graphics.filters.GreyscaleFilter;
import static ca.cgjennings.ui.theme.Theme.*;
import resources.Settings.Colour;
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
        
        // apply theme to standard palette
        BufferedImage im = Palette.get.toImage();
        Palette.get = Palette.fromImage(applyThemeToImage(im));

        Color base = new Color(0x876e2f);
        defaults.put("nimbusBase", base);
        defaults.put("nimbusSelectionBackground", base.darker());
        defaults.put("control", new Color(0xd9c89e));
        defaults.put("nimbusFocus", new Color(0xbf6204));

        // these keys are defined by Strange Eons rather than the look and feel
        defaults.put(Theme.EDITOR_TAB_BACKGROUND, new ColorUIResource(0x564723));
    }

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
        defaults.put(CONSOLE_BACKROUND, new Color(0x4b3d28));
        defaults.put(CONSOLE_OUTPUT, new Color(0xe1f393));
        defaults.put(CONSOLE_ERROR, new Color(0x9df939));
        defaults.put(CONSOLE_SELECTION_BACKGROUND, new Color(0xe1f393));
        defaults.put(CONSOLE_SELECTION_FOREGROUND, new Color(0x4b3d28));
        defaults.put(CONSOLE_BACKGROUND_PAINTER, new HydraTheme.HydraConsolePainter());
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
    
    @Override
    public Colour applyThemeToColor(Color c) {
        return new Colour(sepiaFilt.filterPixel(greyFilt.filterPixel(c.getRGB())), true);
    }

    private AbstractPixelwiseFilter greyFilt;
    private AbstractPixelwiseFilter sepiaFilt;
}
