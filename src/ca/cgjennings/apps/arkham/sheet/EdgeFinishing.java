package ca.cgjennings.apps.arkham.sheet;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Uses heuristics to quickly in-paint a margin onto an existing image that
 * matches the existing image. (This is a utility class that offloads the
 * bleed margin synthesis code to make the {@link Sheet} class easier to
 * maintain.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
final class EdgeFinishing {

    private EdgeFinishing() {
    }
    
    /**
     * Returns a copy of the image that is clipped to the specified corner radius.
     * 
     * @param image the image to have its corners rounded
     * @param arcRadiusPx the radius of the corner cut, in pixels
     * @return an image with cut corners, or the original image
     */
    public static BufferedImage cutCorners(BufferedImage image, RenderTarget target, int arcRadiusPx) {
        if (arcRadiusPx <= 0) {
            return image;
        }
        
        final int w = image.getWidth();
        final int h = image.getHeight();

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            target.applyTo(g);
            g.setPaint(Color.WHITE);
            g.fillRoundRect(0, 0, w, h, arcRadiusPx, arcRadiusPx);
            g.setComposite(AlphaComposite.SrcIn);
            g.drawImage(image, null, null);
        } finally {
            g.dispose();
        }
        return bi;
    }

    /**
     * Synthesize additional pixels along each edge of the supplied image using
     * default heuristics to choose the best method.
     *
     * @param image the image to pad
     * @param marginPx the number of additional pixels to add to each edge.
     * @return the padded image
     */
    public static BufferedImage synthesizeMargin(BufferedImage image, int marginPx) {
        if (marginPx <= 0) {
            return image;
        }
        if (hasInferredSolidBorder(image)) {
            image = extendSolidBorder(image, marginPx);
        } else {
            image = extendByMirroring(image, marginPx);
        }
        return image;
    }

    /**
     * Synthesizes a solid border by mirroring the specified image.
     *
     * @param image the source image
     * @param m the margin to add to each edge in pixels
     * @return the extended image
     */
    private static BufferedImage extendByMirroring(BufferedImage image, final int m) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final int m2 = m * 2;

        BufferedImage bi = new BufferedImage(w + m2, h + m2, image.getType());
        Graphics2D g = bi.createGraphics();
        try {
            g.drawImage(image, m - w, m - h, m, m, w, h, 0, 0, null);
            g.drawImage(image, m, m - h, m + w, m, 0, h, w, 0, null);
            g.drawImage(image, m + w, m - h, m + w + w, m, w, h, 0, 0, null);

            g.drawImage(image, m - w, m, m, m + h, w, 0, 0, h, null);
            g.drawImage(image, m + w, m, m + w + w, m + h, w, 0, 0, h, null);

            g.drawImage(image, m - w, m + h, m, m + h + h, w, h, 0, 0, null);
            g.drawImage(image, m, m + h, m + w, m + h + h, 0, h, w, 0, null);
            g.drawImage(image, m + w, m + h, m + w + w, m + h + h, w, h, 0, 0, null);

            g.drawImage(image, m, m, null);
        } finally {
            g.dispose();
        }
        return bi;
    }

    /**
     * Synthesizes a solid border by sampling a single border pixel from the
     * source image. The source image will be scanned to see if it appears to
     * have corner cuts, and if so they will be filled with the border colour.
     *
     * @param sheetImage the image to extend
     * @param m the margin to add to each edge in pixels
     * @return the extended image
     */
    private static BufferedImage extendSolidBorder(BufferedImage sheetImage, final int m) {
        final int w = sheetImage.getWidth();
        final int h = sheetImage.getHeight();
        final int m2 = m * 2;
        
        BufferedImage bi = ImageUtilities.createCompatibleIntRGBFormat(sheetImage, w + m2, h + m2);
        Graphics2D g = bi.createGraphics();
        try {
            final int rgb = sheetImage.getRGB(w / 2, 0);

            if (sheetImage.getTransparency() == BufferedImage.OPAQUE) {
                g.setColor(new Color(rgb));
                g.fillRect(0, 0, w + m2, h + m2);
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w + m2, h + m2);
                g.setColor(new Color(rgb, true));
                g.fillRect(0, 0, m, h + m2);
                g.fillRect(w + m, 0, m, h + m2);
                g.fillRect(m, 0, w, m);
                g.fillRect(m, h + m, w, m);
            }

            g.drawImage(sheetImage, m, m, null);

            // check for and fill rounded corners
            final int w1 = w - 1;
            final int h1 = h - 1;
            boolean fillCorners = false;
            if (!similar(rgb, sheetImage.getRGB(0, 0)) && !similar(rgb, sheetImage.getRGB(w1, 0)) && !similar(rgb, sheetImage.getRGB(0, h1)) && !similar(rgb, sheetImage.getRGB(w1, h1))) {
                fillCorners = true;
            }
            if (fillCorners) {
                int j;
                int[] xp = new int[3];
                int[] yp = new int[3];
                g.translate(m, m);

                j = findInset(sheetImage, rgb, 0, 0, 1, 0);
                xp[1] = j;
                j = findInset(sheetImage, rgb, 0, 0, 0, 1);
                yp[2] = j;
                g.fillPolygon(xp, yp, 3);

                yp[0] = h;
                j = findInset(sheetImage, rgb, 0, h1, 0, -1);
                xp[1] = 0;
                yp[1] = j;
                j = findInset(sheetImage, rgb, 0, h1, 1, 0);
                xp[2] = j;
                yp[2] = h;
                g.fillPolygon(xp, yp, 3);

                xp[0] = w;
                xp[1] = w;
                j = findInset(sheetImage, rgb, w1, h1, 0, -1);
                yp[1] = j;
                j = findInset(sheetImage, rgb, w1, h1, -1, 0);
                xp[2] = j;
                g.fillPolygon(xp, yp, 3);

                yp[0] = 0;
                j = findInset(sheetImage, rgb, w1, 0, -1, 0);
                xp[1] = j;
                yp[1] = 0;
                j = findInset(sheetImage, rgb, w1, 0, 0, 1);
                xp[2] = w;
                yp[2] = j;
                g.fillPolygon(xp, yp, 3);
            }
        } finally {
            g.dispose();
        }
        return bi;
    }

    /**
     * Returns whether the image appears to have a border of a solid colour,
     * with the possible exception of cut corners, by sampling pixels along the
     * image edges. To allow for scanned images, an error margin is allowed when
     * comparing whether sampled pixels are considered the same colour.
     *
     * @param sheetImage the image to sample
     * @return true if the image appears to have a solid border
     */
    private static boolean hasInferredSolidBorder(BufferedImage sheetImage) {
        final int w = sheetImage.getWidth();
        final int h = sheetImage.getHeight();

        boolean solid = false;
        int rgb = sheetImage.getRGB(w / 2, 0);
        if (similar(rgb, sheetImage.getRGB(w / 2, h - 1))) {
            if (similar(rgb, sheetImage.getRGB(0, h / 3))) {
                if (similar(rgb, sheetImage.getRGB(w - 1, h / 3))) {
                    if (similar(rgb, sheetImage.getRGB(0, h * 2 / 3))) {
                        if (similar(rgb, sheetImage.getRGB(w - 1, h * 2 / 3))) {
                            solid = true;
                        }
                    }
                }
            }
        }

        return solid;
    }

    /**
     * Scans along a horizontal or vertical line until it finds a pixel that is
     * not similar to a specified colour, returning the x- or y- value of the
     * different pixel. (The scan distance is limited to a set fraction of the
     * image size.)
     *
     * @param bi the image to scan
     * @param rgb the colour to expect
     * @param x0 the starting x-coordinate; (x0, y0) must be on an edge
     * @param y0 the starting y-coordinate
     * @param dx the horizontal delta; must be -1, 0, or 1; exactly one of dx
     * and dy must be 0
     * @param dy the vertical delta; must be -1, 0, or 1
     * @return the x-coordinate (if dy == 0) or y-coordinate (if dx == 0) of the
     * first rejected pixel (or the end of the allowed scanning range)
     */
    private static int findInset(BufferedImage bi, int rgb, int x0, int y0, int dx, int dy) {
        final int limit = Math.max(1, (dx == 0 ? bi.getHeight() : bi.getWidth()) / 3);
        for (int i = 0; i < limit; ++i) {
            if (similar(rgb, bi.getRGB(x0, y0))) {
                break;
            }
            x0 += dx;
            y0 += dy;
        }
        return dx == 0 ? y0 : x0;
    }

    private static boolean similar(int rgb1, int rgb2) {
        final int da = (rgb1 >>> 24) - (rgb2 >>> 24);
        final int dr = ((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff);
        final int dg = ((rgb1 >> 8) & 0xff) - ((rgb2 >> 8) & 0xff);
        final int db = (rgb1 & 0xff) - (rgb2 & 0xff);

        return (dr * dr + dg * dg + db * db) <= MAX_DIST;
    }

    // square of maximum distance in RGB space within which two pixels are
    // considered similar in colour
    private static final int MAX_DIST = 36;
}
