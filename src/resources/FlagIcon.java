package resources;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.graphics.filters.SubstitutionFilter;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.Icon;

/**
 * Icon implementation that displays a small flag image for a specified country.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
final class FlagIcon implements Icon {

    private int sx = -1, sy;

    private static final int COLUMNS = 22;
    private static char[] index;
    private static BufferedImage matrix;

    static {
        InputStream in = null;
        try {
            matrix = ImageIO.read(ResourceKit.composeResourceURL("icons/flags/flags.png"));
            matrix = new SubstitutionFilter().filter(
                    matrix,
                    new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_ARGB)
            );

            int ch;
            StringBuilder b = new StringBuilder();
            in = ResourceKit.getInputStream("icons/flags/index");
            while ((ch = in.read()) >= 0) {
                b.append((char) ch);
            }
            in.close();
            index = new char[b.length()];
            b.getChars(0, index.length, index, 0);
        } catch (Exception e) {
            // this will cause all code lookups to fail, so all icons are blank
            index = new char[0];
            StrangeEons.log.log(Level.WARNING, "failed to create flag matrix", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
     * Creates a flag icon for a country with the specified country code. If no
     * icon is available, the icon will display a blank image.
     *
     * @param countryCode the code for the country to display
     */
    public FlagIcon(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            throw new IllegalArgumentException();
        }
        char c0 = Character.toLowerCase(countryCode.charAt(0));
        char c1 = Character.toLowerCase(countryCode.charAt(1));
        int i = 0;
        for (; i < index.length; i += 2) {
            if (index[i] == c0 && index[i + 1] == c1) {
                break;
            }
        }
        if (i < index.length) {
            i >>= 1;
            final int col = i % COLUMNS;
            final int row = i / COLUMNS;
            sx = col * W;
            sy = row * H;
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (sx >= 0) {
            g.drawImage(matrix, x, y + VERTICAL_OFFSET, x + W, y + VERTICAL_OFFSET + H, sx, sy, sx + W, sy + H, null);
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

//	public static void main(String[] args) throws IOException {
//		FlagIcon i = new FlagIcon( "ca" );
//		BufferedImage bi = new BufferedImage( 16,16,BufferedImage.TYPE_INT_ARGB );
//		Graphics2D g  = bi.createGraphics();
//		i.paintIcon(null, g, 0, 0);
//		g.dispose();
//		ImageIO.write(bi, "png", new File("d:/test.png"));
//	}
    private static final int SIZE = 16; // icon width
    private static final int W = 16; // true icon height
    private static final int H = 11; // true icon height
    static final int VERTICAL_OFFSET = (SIZE - H + 1) / 2; // offset since flags 11px high
}
