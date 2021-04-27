package ca.cgjennings.imageio;

import static ca.cgjennings.imageio.ImageLayer.BlendingMode.*;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Minimal reader for PSD format image files. The individual layers are made
 * available as {@link ImageLayer} objects.
 *
 * @see <a href="https://www.adobe.com/devnet-apps/photoshop/fileformatashtml/#50577409_pgfId-1055726">PSD specification</a>
 */
public final class PSDImageReader {
    /** Color modes employed by PSD images. */
    public static enum ColorMode {
        BITMAP, GREYSCALE, INDEXED, RGB, CMYK, MULTICHANNEL, DUOTONE, LAB, UNKNOWN;

        @Override
        public String toString() {
            String s;
            switch(this) {
                case BITMAP: s = "Bitmap"; break;
                case GREYSCALE: s = "Greyscale"; break;
                case INDEXED: s = "Indexed"; break;
                case RGB: s = "RGB"; break;
                case CMYK: s = "CMYK"; break;
                case MULTICHANNEL: s = "Multichannel"; break;
                case DUOTONE: s = "Duotone"; break;
                case LAB: s = "Lab"; break;
                default: s = "unknown";
            }
            return s;
        }
    }

    /** File format version. */
    private int psdVersion;
    private ColorMode colorMode;
    /** Number of channels, including alpha channels, from 1-56. */
    private int numChannels;
    /** Image width in pixels. */
    private int width;
    /** Image height in pixels. */
    private int height;
    /** Channel depth in bits. */
    private int depth;
    /** Number of image layers. */
    private int numLayers;
    private ImageLayer[] layers;

    // temporaries only used while processing the stream
    private BufferedInputStream input;
    private RawLayer[] rawLayer;
    private boolean hasLayers = true;
    private int miscLen;

    private static final int CH_RED = 0;
    private static final int CH_GREEN = 1;
    private static final int CH_BLUE = 2;
    private static final int CH_ALPHA = -1;

    private static final String MAGIC_PSD_IMAGE = "8BPS";
    private static final String MAGIC_IMAGE_RES_BLOCK = "8BIM";

    private static class RawLayer {
        int x, y, w, h;
        int numChannels;
        int[] channelId;
        int alpha;
        int flags;
        ImageLayer.BlendingMode blendMode;
    }

    /**
     * Reads a PSD image from a file.
     *
     * @param input the non-null file to read
     * @throws IOException if an error occurs while reading the image
     */
    public PSDImageReader(File input) throws IOException {
        try(InputStream ins = new FileInputStream(input)) {
            readImage(ins);
        }
    }

    /**
     * Returns the color mode used by the image file. This has no impact on the
     * format of decoded images returned by this class, which are always
     * 8-bit RGB.
     *
     * @return the color mode used by the file
     */
    public ColorMode getColorMode() {
        return colorMode;
    }

    /**
     * Returns the number of layers read from the image file.
     * @return the number of layers in the image
     */
    public int getLayerCount() {
        return layers.length;
    }

    /**
     * Returns the layer at the requested index. The layer's index also
     * indicates its Z-ordering in the layer stack. Higher indices are painted
     * overtop of lower indices.
     *
     * @param index the 0-based index of the layer
     * @return the layer at the requested index
     * @throws IndexOutOfBoundsException if <code>index</code> &lt; 0 or
     * <code>index</code> &gt;= {@link #getLayerCount}
     */
    public ImageLayer getLayer(int index) {
        if (index < 0 || index >= layers.length) {
            throw new IndexOutOfBoundsException("layer: " + index);
        }
        return layers[index];
    }

    /**
     * Returns a composite image of all of the layers.
     *
     * @return an image representing a composite view of the image layers
     */
    public BufferedImage createComposite() {
        BufferedImage bi = new BufferedImage(layers[0].getWidth(), layers[0].getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = null;
        try {
            g = bi.createGraphics();
            for (int i = 0; i < getLayerCount(); ++i) {
                ImageLayer la = getLayer(i);
                if (!la.isVisible()) {
                    continue;
                }
                la.paint(g);
            }
        } finally {
            if (g != null) {
                g.dispose();
            }
        }
        return bi;
    }

    private static ColorMode decodeColorMode(int cm) {
        ColorMode m;
        switch(cm) {
            case 0: m = ColorMode.BITMAP; break;
            case 1: m = ColorMode.GREYSCALE; break;
            case 2: m = ColorMode.INDEXED; break;
            case 3: m = ColorMode.RGB; break;
            case 4: m = ColorMode.CMYK; break;
            case 7: m = ColorMode.MULTICHANNEL; break;
            case 8: m = ColorMode.DUOTONE; break;
            case 9: m = ColorMode.LAB; break;
            default: m = ColorMode.UNKNOWN;
        }
        return m;
    }

    private static void throwInvalidFormat() throws IOException {
        throw new PSDFormatException("Not a valid PSD file");
    }

    public static final class PSDFormatException extends IOException {
        private static final long serialVersionUID = 1L;
        public PSDFormatException(String message) {
            super(message);
        }
    }

    private void readImage(InputStream input) throws IOException {
        if (input instanceof BufferedInputStream) {
            this.input = (BufferedInputStream) input;
        } else {
            this.input = new BufferedInputStream(input);
        }

        readHeader();
        readLayerInfo();
        if (numLayers == 0) {
            createFakeLayer();
        } else {
            readLayers();
            int tx = layers[0].getX();
            int ty = layers[0].getY();
            for (int i = 0; i < numLayers; ++i) {
                layers[i].setX(layers[i].getX() - tx);
                layers[i].setY(layers[i].getY() - ty);
            }
        }
    }

    private void readHeader() throws IOException {
        String magic = readString(4);
        if (!magic.equals(MAGIC_PSD_IMAGE)) {
            throwInvalidFormat();
        }

        psdVersion = readShort();
        if (psdVersion != 1) {
            throw new PSDFormatException("Unsupported version: " + psdVersion);
        }

        skipBytes(6); // reserved, "must be 0"

        numChannels = readShort(); // 1 to 56

        height = readInt();
        width = readInt();
        depth = readShort();
        colorMode = decodeColorMode(readShort());
        int colorModeDataLen = readInt();
        skipBytes(colorModeDataLen);
        int imageResourcesLen = readInt();
        skipBytes(imageResourcesLen);

        if(colorMode != ColorMode.RGB) {
            throw new PSDFormatException("Unsupported color mode: " + colorMode);
        }

        if (depth != 8) {
            throw new PSDFormatException("Unsupported bit depth: " + depth);
        }
    }

    private void readLayerInfo() throws IOException {
        miscLen = readInt();
        if (miscLen == 0) {
            return; // no layers, only base image
        }
        int layerInfoLen = readInt();
        if (layerInfoLen > 0) {
            numLayers = Math.abs(readShort());
            if (numLayers > 0) {
                rawLayer = new RawLayer[numLayers];
            }
            for (int i = 0; i < numLayers; i++) {
                RawLayer info = new RawLayer();
                rawLayer[i] = info;
                info.y = readInt();
                info.x = readInt();
                info.h = readInt() - info.y;
                info.w = readInt() - info.x;
                info.numChannels = readShort();
                info.channelId = new int[info.numChannels];
                for (int j = 0; j < info.numChannels; j++) {
                    int id = readShort();
                    readInt(); // size
                    info.channelId[j] = id;
                }
                String s = readString(4);
                if (!s.equals(MAGIC_IMAGE_RES_BLOCK)) {
                    throwInvalidFormat();
                }
                info.blendMode = convertBlendMode(readString(4));
                info.alpha = readByte();
                readByte();  // clipping
                info.flags = readByte();  // flags
                readByte(); // filler
                int extraSize = readInt();
                skipBytes(extraSize);
            }
        } else {
            // no layer info, skip anything else
            skipBytes(miscLen - 4);
        }
    }

    /** Read each layer and extract image data. */
    private void readLayers() throws IOException {
        // read and convert each layer to BufferedImage
        layers = new ImageLayer[numLayers];
        for (int i = 0; i < numLayers; i++) {
            RawLayer info = rawLayer[i];
            byte[] r = null, g = null, b = null, a = null;
            for (int j = 0; j < info.numChannels; j++) {
                int id = info.channelId[j];
                switch (id) {
                    case CH_RED:
                        r = readPlane(info.w, info.h);
                        break;
                    case CH_GREEN:
                        g = readPlane(info.w, info.h);
                        break;
                    case CH_BLUE:
                        b = readPlane(info.w, info.h);
                        break;
                    case CH_ALPHA:
                        a = readPlane(info.w, info.h);
                        break;
                    default:
                        readPlane(info.w, info.h);
                }
            }

            BufferedImage im = createImageFromPlaneData(info.w, info.h, r, g, b, a);
            layers[i] = new ImageLayer(im, info.x, info.y, info.alpha / 255f, info.blendMode);
            layers[i].setVisible((info.flags & 2) == 0);
        }

        if (miscLen > 0) {
            int n = readInt(); // global layer mask info len
            skipBytes(n);
        }
    }

    /** Read an image plane (channel), returning 8-bit data. */
    private byte[] readPlane(int w, int h) throws IOException {
        short[] lineLengths = null;
        boolean isRLECompressed = false;

        if (hasLayers) {
            isRLECompressed = readShort() == 1;
            if (isRLECompressed) {
                lineLengths = readListOfRLELineLengths(h);
            }
        }

        final int size = w * h;
        final byte[] b = new byte[size];
        if (isRLECompressed) {
            byte[] s = new byte[w * 2];
            int pos = 0;
            for (int i = 0; i < h; i++) {
                if (i >= lineLengths.length) {
                    throwInvalidFormat();
                }
                int len = lineLengths[i];
                readBytes(s, len);
                decodeRLE(s, 0, len, b, pos);
                pos += w;
            }
        } else {
            readBytes(b, size);
        }

        return b;
    }

    private void decodeRLE(byte[] src, int sindex, int slen, byte[] dst, int dindex) throws IOException {
        try {
            int max = sindex + slen;
            while (sindex < max) {
                byte b = src[sindex++];
                int n = b;
                if (n < 0) {
                    n = 1 - n;
                    b = src[sindex++];
                    for (int i = 0; i < n; i++) {
                        dst[dindex++] = b;
                    }
                } else {
                    // run of n + 1 bytes
                    ++n;
                    System.arraycopy(src, sindex, dst, dindex, n);
                    dindex += n;
                    sindex += n;
                }
            }
        } catch (Exception e) {
            throwInvalidFormat();
        }
    }

    private ImageLayer.BlendingMode convertBlendMode(String key) {
        if (key == null) {
            return UNKNOWN;
        }
        for (int i = 0; i < BLEND_MODE_KEYS.length; ++i) {
            if (key.equals(BLEND_MODE_KEYS[i])) {
                return BLEND_MODE_VALUES[i];
            }
        }
        return UNKNOWN;
    }

    private static final String[] BLEND_MODE_KEYS = new String[]{
        "norm", "dark", "lite", "hue", "sat", "colr",
        "lum", "mul", "scrn", "diss", "over", "hLit",
        "sLit", "diff"
    };

    private static final ImageLayer.BlendingMode[] BLEND_MODE_VALUES = new ImageLayer.BlendingMode[]{
        NORMAL, DARKEN, LIGHTEN, HUE, SATURATION, COLOR,
        LUMINOSITY, MULTIPLY, SCREEN, DISSOLVE, OVERLAY, HARD_LIGHT,
        SOFT_LIGHT, DIFFERENCE
    };

    private void createFakeLayer() throws IOException {
        hasLayers = false;
        numLayers = 1;
        rawLayer = new RawLayer[1];
        RawLayer layer = new RawLayer();
        rawLayer[0] = layer;
        layer.h = height;
        layer.w = width;
        int numLayerChannels = Math.min(numChannels, 4);

        final boolean isRLECompressed = readShort() == 1;
        if (isRLECompressed) {
            readListOfRLELineLengths(height * numLayerChannels);
        }
        layer.numChannels = numLayerChannels;
        layer.channelId = Arrays.copyOf(new int[] {0,1,2,-1}, numLayerChannels);
    }

    private short[] readListOfRLELineLengths(int nLines) throws IOException {
        short[] lineLengths = new short[nLines];
        for (int i = 0; i < nLines; i++) {
            lineLengths[i] = readShort();
        }
        return lineLengths;
    }

    /** Combine individual color channel planes into a BufferedImage. */
    private BufferedImage createImageFromPlaneData(int w, int h, byte[] r, byte[] g, byte[] b, byte[] a) {
        if (w == 0 || h == 0) {
            BufferedImage im = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            im.setRGB(0, 0, 0x00ffffff);
            return im;
        }

        if (r == null || g == null || b == null) {
            byte[] zeroBytes = new byte[w * h];
            if (r == null) r = zeroBytes;
            if (g == null) g = zeroBytes;
            if (b == null) b = zeroBytes;
        }
        if (a == null) {
            a = new byte[w * h];
            Arrays.fill(a, (byte) 255);
        }

        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int i=0;
        int[] rowArgb = new int[w];
        for (int y=0; y<h; ++y) {
            for (int x=0; x<w; ++x, ++i) {
                rowArgb[x] = ((((int) a[i]) & 0xff) << 24)
                           | ((((int) r[i]) & 0xff) << 16)
                           | ((((int) g[i]) & 0xff) << 8)
                           |  (((int) b[i]) & 0xff);
            }
            im.getRaster().setDataElements(0, y, w, 1, rowArgb);
        }

        return im;
    }


    /** Read a single byte, throwing if not available. */
    private int readByte() throws IOException {
        int b = input.read();
        if(b < 0) throwInvalidFormat();
        return b;
    }

    /** Read n bytes into an array, throwing if not available. */
    private void readBytes(byte[] bytes, int n) throws IOException {
        if (bytes == null) {
            return;
        }
        int r = 0;
        do {
            int read = input.read(bytes, r, n - r);
            if (read < 0) throwInvalidFormat();
            r += read;
        } while (r < n);
    }

    /** Read big-endian 32-bit int, throwing if not available. */
    private int readInt() throws IOException {
        return (((((readByte() << 8) | readByte()) << 8) | readByte()) << 8)
                | readByte();
    }

    /** Read big-endian 16-bit int, throwing if not available. */
    private short readShort() throws IOException {
        return (short) ((readByte() << 8) | readByte());
    }

    /** Read ASCII string of specified length, throwing if not available. */
    private String readString(int len) throws IOException {
        StringBuilder b = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            b.append((char) readByte());
        }
        return b.toString();
    }

    /** Skip n bytes of input, throwing if not available. */
    private void skipBytes(int n) throws IOException {
        for (int i = 0; i < n; i++) {
            readByte();
        }
    }
}
