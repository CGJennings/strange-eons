package ca.cgjennings.apps.arkham.deck.item;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferInt;
import java.awt.image.Kernel;

/**
 * Creates drop shadows of arbitrary images.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class OldDropShadow {

    private int shadowSize = 8;
    private Color shadowColor = Color.BLACK;
    private float shadowOpacity = 0.5f;
    private boolean highQuality = true;

    public OldDropShadow() {
    }

    public OldDropShadow(Color color, int size, float opacity) {
        shadowSize = size;
        shadowColor = color;
    }

    /**
     * In order to have sufficient room to render the shadow, the source image
     * must have an empty margin equal to the shadow size on all sides. For
     * efficiency, {@link #createShadowImage(java.awt.image.BufferedImage)}
     * assumes that these margins are already present. If your original image
     * can be created with the margin, you can call it directly. Otherwise, use
     * this method to convert an existing image into an image with appropriate
     * margins.
     *
     * @param image
     * @return an image surrounded by transparent black pixels equal to the
     * shadow size on all sides
     */
    public BufferedImage createMarginImage(BufferedImage image) {
        BufferedImage subject = new BufferedImage(image.getWidth() + shadowSize * 2,
                image.getHeight() + shadowSize * 2,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = subject.createGraphics();
        g2.drawImage(image, null, shadowSize, shadowSize);
        g2.dispose();

        return subject;
    }

    public int getShadowSize() {
        return shadowSize;
    }

    public void setShadowSize(int shadowSize) {
        this.shadowSize = shadowSize;
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(Color shadowColor) {
        this.shadowColor = shadowColor;
    }

    public static Point2D calculateShadowOffset(double angle, double distance, int shadowSize) {
        double marginOffset = -shadowSize;
        double theta = Math.toRadians(angle);
        double dx = (int) ((Math.cos(theta) * distance) + 0.5d);
        double dy = (int) ((Math.sin(theta) * distance) + 0.5d);
        return new Point2D.Double(dx + marginOffset, dy + marginOffset);
    }

    public BufferedImage createShadowImage(BufferedImage shadowMask) {
        if (highQuality) {
            BufferedImage shadow = new BufferedImage(shadowMask.getWidth(), shadowMask.getHeight(), BufferedImage.TYPE_INT_ARGB);
            getLinearBlurOp(shadowSize, 1).filter(shadowMask, shadow);
            getLinearBlurOp(1, shadowSize).filter(shadow, shadowMask);
            return shadowMask;
        } else {
            fastShadow(shadowMask);
            return shadowMask;
        }
    }

    private ConvolveOp getLinearBlurOp(int width, int height) {
        float[] data = new float[width * height];
        float value = 1.0f / (width * height);
        for (int i = 0; i < data.length; i++) {
            data[i] = value;
        }
        return new ConvolveOp(new Kernel(width, height, data));
    }

    /**
     * Convert an image in place into a drop shadow of the original image. Uses
     * the fast algorithm.
     *
     * @param image a source image with appropriate margins (see
     * {@link #createMarginImage(java.awt.image.BufferedImage)})
     */
    private void fastShadow(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            throw new IllegalArgumentException("image type must be TYPE_INT_ARGB");
        }
        int dstWidth = image.getWidth();
        int dstHeight = image.getHeight();

        int left = (shadowSize - 1) >> 1;
        int right = shadowSize - left;
        int xStart = left;
        int xStop = dstWidth - right;
        int yStart = left;
        int yStop = dstHeight - right;

        int shadowRgb = shadowColor.getRGB() & 0x00FF_FFFF;

        int[] aHistory = new int[shadowSize];
        int historyIdx = 0;

        int aSum;

        int[] dataBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        int lastPixelOffset = right * dstWidth;
        float sumDivider = shadowOpacity / shadowSize;

        // horizontal pass
        for (int y = 0, bufferOffset = 0; y < dstHeight; y++, bufferOffset = y * dstWidth) {
            aSum = 0;
            historyIdx = 0;
            for (int x = 0; x < shadowSize; x++, bufferOffset++) {
                int a = dataBuffer[bufferOffset] >>> 24;
                aHistory[x] = a;
                aSum += a;
            }

            bufferOffset -= right;

            for (int x = xStart; x < xStop; x++, bufferOffset++) {
                int a = (int) (aSum * sumDivider);
                dataBuffer[bufferOffset] = a << 24 | shadowRgb;

                // substract the oldest pixel from the sum
                aSum -= aHistory[historyIdx];

                // get the lastest pixel
                a = dataBuffer[bufferOffset + right] >>> 24;
                aHistory[historyIdx] = a;
                aSum += a;

                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
            }
        }

        // vertical pass
        for (int x = 0, bufferOffset = 0; x < dstWidth; x++, bufferOffset = x) {
            aSum = 0;
            historyIdx = 0;
            for (int y = 0; y < shadowSize; y++, bufferOffset += dstWidth) {
                int a = dataBuffer[bufferOffset] >>> 24;
                aHistory[y] = a;
                aSum += a;
            }

            bufferOffset -= lastPixelOffset;

            for (int y = yStart; y < yStop; y++, bufferOffset += dstWidth) {
                int a = (int) (aSum * sumDivider);
                dataBuffer[bufferOffset] = a << 24 | shadowRgb;

                // substract the oldest pixel from the sum
                aSum -= aHistory[historyIdx];

                // get the lastest pixel
                a = dataBuffer[bufferOffset + lastPixelOffset] >>> 24;
                aHistory[historyIdx] = a;
                aSum += a;

                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize;
                }
            }
        }
    }

    /**
     * @return the highQuality
     */
    public boolean isHighQuality() {
        return highQuality;
    }

    /**
     * @param highQuality the highQuality to set
     */
    public void setHighQuality(boolean highQuality) {
        this.highQuality = highQuality;
    }
}
