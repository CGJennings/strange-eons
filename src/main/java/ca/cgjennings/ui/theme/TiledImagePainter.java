package ca.cgjennings.ui.theme;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Painter;
import resources.ResourceKit;

/**
 * A painter that tiles an image over the entire painted surface. For best
 * results, the caller should ensure that the tile image has seamless edges.
 *
 * @param <T> the type of object being painted (may be null)
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TiledImagePainter<T> implements Painter<T> {

    private BufferedImage tile;

    /**
     * Creates a painter that loads its tile image from a resource.
     *
     * @param resource the resource of the tile image
     */
    public TiledImagePainter(String resource) {
        this(ResourceKit.getImage(resource));
    }

    /**
     * Creates a painter that tiles the supplied image.
     *
     * @param tileImage the image to tile
     */
    public TiledImagePainter(BufferedImage tileImage) {
        if (tileImage == null) {
            throw new NullPointerException("tileImage");
        }
        tile = tileImage;
    }

    @Override
    public void paint(Graphics2D g, Object object, int width, int height) {
        BufferedImage im = tile;
        int iw = im.getWidth();
        int ih = im.getHeight();
        for (int y = 0; y <= height; y += iw) {
            for (int x = 0; x <= width; x += ih) {
                g.drawImage(im, x, y, null);
            }
        }
    }
}
