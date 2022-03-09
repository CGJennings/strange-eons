package ca.cgjennings.ui.theme;

import ca.cgjennings.apps.arkham.StrangeEons;
import resources.Language;
import resources.ResourceKit;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * An icon that represents a locale, language, or region.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class LocaleIcon implements Icon {
    // icon that renders the language code
    private ThemedIcon lang;
    // position of language in icon space
    private int lx, ly;
    // position of flag in sprite sheet
    private int fx = -1, fy = -1, fsize;
    
    /** Maps a country code to the index of its flag on the flag sprite sheet. */
    private static final byte[] MAP;
    /** The flag sprite sheet. */
    private static final BufferedImage FLAG_SHEET;
    /** The width and height of each flag. */
    private static final int FLAG_PX_SIZE;
    /** The icon size. */
    private static final int SIZE = ThemedIcon.SMALL;
    
    static {
        FLAG_PX_SIZE = SIZE * (ResourceKit.estimateDesktopScalingFactor() > 1d ? 2 : 1);
        
        byte[] map;
        try (InputStream in = ResourceKit.class.getResourceAsStream("icons/flags/map.bin")) {
            map = in.readAllBytes();
        } catch (IOException | NullPointerException ex) {
            StrangeEons.log.log(Level.SEVERE, "missing flag data");
            map = new byte[26*26];
        }
        MAP = map;

        BufferedImage flagImage;
        try {
            String sheetImage = "icons/flags/flags" + (FLAG_PX_SIZE > SIZE ? "@2x" : "") + ".png";
            flagImage = ImageIO.read(ResourceKit.composeResourceURL(sheetImage));
        } catch (IOException | NullPointerException ex) {
            StrangeEons.log.log(Level.SEVERE, "missing flag image");
            flagImage = null;
        }
        FLAG_SHEET = flagImage;
    }
    
    public LocaleIcon(Language lang) {
        this(lang.getLocale());
    }
    
    public LocaleIcon(Locale locale) {
        this(locale.getLanguage(), locale.getCountry());
    }
    
    public LocaleIcon(String language, String country) {
        if (language != null && language.length() >= 2) {
            lang = new ThemedGlyphIcon(
                    Character.toUpperCase(language.charAt(0)) +
                    ",,i@-4,-4;" +
                    Character.toUpperCase(language.charAt(1)) +
                    "@-4,4"
            );
        }
        
        if (country != null && country.length() == 2) {
            // determine offset of country code in the flag map
            int row = Character.toUpperCase(country.charAt(0)) - 'A';
            int col = Character.toUpperCase(country.charAt(1)) - 'A';
            if (row >= 0 && row < 26 && col >= 0 && col < 26) {
                // map the country code to a flag index, or -1 if the
                // country code does not have a flag on the sheet
                int index = ((int) MAP[row * 26 + col] & 0xff) - 1;
                if (index >= 0) {
                    // convert the flag index to a location on the sheet
                    fx = (index & 0xf) * FLAG_PX_SIZE;
                    fy = (index >> 4) * FLAG_PX_SIZE;
                    fsize = SIZE;
                    if (lang != null) {
                        fsize = fsize * 2/3;
                        lx = ly = 6;
                        lang = lang.derive(fsize);
                    }
                }
            }
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g1, int x, int y) {
        Graphics2D g = (Graphics2D) g1;
        
        if (fx >= 0) {
            Object oldTerp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(FLAG_SHEET, x, y, x+fsize, y+fsize, fx, fy, fx + FLAG_PX_SIZE, fy + FLAG_PX_SIZE, null);
            if (oldTerp != null && oldTerp != RenderingHints.VALUE_INTERPOLATION_BILINEAR) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldTerp);
            }
        }
        
        if (lang != null) {
            lang.paintIcon(c, g, x + lx, y + ly);
        }
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
    
    
    public static void main(String[] args) {
        EventQueue.invokeLater(()->{
            JFrame f = new JFrame();
            f.setSize(400,400);
            f.setLocationByPlatform(true);
            f.add(new JLabel(new LocaleIcon("", "")));
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);
        });
    }
}
