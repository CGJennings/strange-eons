package ca.cgjennings.layout;

import java.awt.font.GraphicAttribute;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import resources.ResourceKit;
import resources.StrangeImage;

/**
 * A style factory that creates text styles that replace selections with images.
 * This factory is not normally created directly, but rather is used indirectly
 * from a {@link MarkupRenderer}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 1.5
 */
public final class GraphicStyleFactory implements ParametricStyleFactory {

    private double ppi;

    public GraphicStyleFactory(double ppi) {
        this.ppi = ppi;
    }

    public void setResolution(double ppi) {
        this.ppi = ppi;
    }

    public double getResolution() {
        return ppi;
    }

    /**
     * Create a style based on a list of parameters to be parsed, typically from
     * an &lt;image&gt; tag. The format of the parameters is as follows:<br>
     * {@code     url [width [height]] [alignment [offset]]}
     */
    @Override
    public TextStyle createStyle(MarkupRenderer renderer, String[] parameters) {
        if (parameters.length >= 1) {
            int alignment = GraphicAttribute.ROMAN_BASELINE;

            int number = 0;
            double[] numericParameters = new double[]{Double.NaN, Double.NaN, Double.NaN};

            boolean lastWasAlignment = false;
            String name = parameters[0];
            for (int i = 1; i < parameters.length; ++i) {
                int a = parseAlignment(parameters[i]);
                if (a == PARSE_FAIL) {
                    double m = MarkupRenderer.parseMeasurement(parameters[i]);

                    if (m != m) {
                        continue; // parse fail
                    }
                    if (m <= 0d && number < 2) {
                        continue; // invalid for width, height
                    }
                    if (number >= numericParameters.length) {
                        continue;
                    }

                    if (lastWasAlignment) {
                        numericParameters[numericParameters.length - 1] = m;
                        number = numericParameters.length;
                    } else {
                        numericParameters[number++] = m;
                    }
                    lastWasAlignment = false;
                } else {
                    alignment = a;
                    lastWasAlignment = true;
                }
            }

            if (renderer.getBaseFile() != null && name.indexOf(':') == -1) {
                name = translateRelativePath(name, renderer.getBaseFile());
            }

            StrangeImage si = StrangeImage.get(name);

            double width = numericParameters[0];
            double height = numericParameters[1];
            double originY = numericParameters[2];

            // create default width assuming 150 ppi
            if (width != width) {
                width = si.getWidth2D() / 150d;
            }
            // create default height by maintaining aspect ratio
            if (height != height) {
                height = width / si.getAspectRatio();
            }
            // default origin is (0, height)
            if (originY != originY) {
                originY = height;
                if (alignment == GraphicAttribute.CENTER_BASELINE) {
                    originY = height / 2d;
                } else if (alignment == GraphicAttribute.HANGING_BASELINE) {
                    originY = 0d;
                }
            }

            if (alignment == GraphicAttribute.TOP_ALIGNMENT) {
                originY = 0d;
            } else if (alignment == GraphicAttribute.BOTTOM_ALIGNMENT) {
                originY = height;
            }

            // convert the measurements from inches to pixels
            width *= ppi;
            height *= ppi;
            originY *= ppi;

            return createStyle(si, alignment, width, height, 0f, (float) originY);
        }
        return createStyle(StrangeImage.getMissingImage());
    }

    /**
     * Returns an absolute path for a file identifier that is relative to a
     * known base file.
     *
     * @param path the path to make absolute
     * @param base the base folder that the path is relative to
     * @return an absolute identifier, or the original identifier if it is
     * already absolute
     */
    public static String translateRelativePath(String path, File base) {
        if (base == null || path.indexOf(':') < 0) {
            return path;
        }
        File relative = new File(path);
        if (relative.isAbsolute()) {
            return path;
        }
        relative = new File(base, path);
        return relative.getAbsolutePath();
    }

    /**
     * Parse an argument which may be an alignment string. If it is, returns the
     * alignment requested. Returns PARSE_FAIL if the argument is not an
     * alignment string.
     */
    private static int parseAlignment(String p) {
        int parseVal = PARSE_FAIL;
        p = p.toLowerCase();
        switch (p) {
            case "top":
                parseVal = GraphicAttribute.TOP_ALIGNMENT;
                break;
            case "bottom":
                parseVal = GraphicAttribute.BOTTOM_ALIGNMENT;
                break;
            case "baseline":
                parseVal = GraphicAttribute.ROMAN_BASELINE;
                break;
            case "hanging":
                parseVal = GraphicAttribute.HANGING_BASELINE;
                break;
            case "center":
            case "centre":
            case "middle":
                parseVal = GraphicAttribute.CENTER_BASELINE;
                break;
        }
        return parseVal;
    }
    private static final int PARSE_FAIL = 1_000;

    public TextStyle createStyle(StrangeImage i) {
        return createStyle(i, GraphicAttribute.BOTTOM_ALIGNMENT);
    }

    public TextStyle createStyle(StrangeImage i, int alignment) {
        return createStyle(i, alignment, i.getWidth2D() / 72d, i.getHeight2D() / 72d, 0f, 0f);
    }

    public TextStyle createStyle(StrangeImage i, int alignment, double width, double height, float originX, float originY) {
        StrangeImageAttribute attr = new StrangeImageAttribute(i, alignment, originX, originY, (float) width, (float) height);
        return new TextStyle(TextAttribute.CHAR_REPLACEMENT, attr);
    }

    /**
     * @deprecated Use {@link StrangeImage#get(java.lang.String)} to load user
     * images so that you can work with both bitmap and vector images
     * transparently.
     *
     * @param identifier the portrait identifier to load
     * @return the image as a bitmap
     */
    public static BufferedImage fetchImage(String identifier) {
        return StrangeImage.getAsBufferedImage(identifier);
    }

    /**
     * @deprecated Use {@link StrangeImage#exists}.
     *
     * @param identifier the portrait identifier to test
     * @return true if the image exists
     */
    public static boolean imageExists(String identifier) {
        return StrangeImage.exists(identifier);
    }

    /**
     * @deprecated Use {@link StrangeImage#identifierToURL(java.lang.String)}.
     *
     * @param identifier an identifier containing a local file path or URL
     * @return a URL that can be used to read the identified content
     */
    public static URL translatePathToImageURL(String identifier) {
        return StrangeImage.identifierToURL(identifier);
    }

    /**
     * Returns {@code true} if the image identifier appears to refer to a URL
     * rather than a local file.
     *
     * @param identifier the identifier to check
     * @return {@code true} if the identifier does not refer to a file on the
     * local file system
     *
     * @deprecated Use {@link StrangeImage} to load user images and
     * {@code !StrangeImage.isFileIdentifier( identifier )} as a replacement for
     * this call.
     */
    public static boolean isURLString(String identifier) {
        return !StrangeImage.isFileIdentifier(identifier);
    }

    /**
     * Returns the shared image that represents a missing image.
     *
     * @return the shared missing image
     *
     * @deprecated Use {@link StrangeImage} to load user images and
     * {@link ResourceKit#getMissingImage()} or
     * {@link StrangeImage#getMissingImage()} to get the missing image as a
     * {@link BufferedImage} or {@link StrangeImage}, respectively.
     */
    public static BufferedImage getMissingImage() {
        return ResourceKit.getMissingImage();
    }
}
