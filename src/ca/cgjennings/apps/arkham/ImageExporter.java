package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.sheet.EdgeStyle;
import ca.cgjennings.apps.arkham.sheet.PrintDimensions;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.apps.arkham.sheet.UndecoratedCardBack;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.imageio.SimpleImageWriter;
import org.mozilla.javascript.WrappedException;
import ca.cgjennings.text.SETemplateProcessor;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * An image exporter assists in exporting a collection of images to an
 * {@link ExportContainer} such as a ZIP archive. It is most commonly used by
 * the <b>Export</b> command to export images of a game component.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ImageExporter {

    /**
     * Creates a new export manager. An export manager is usually obtained by
     * calling {@link #getSharedInstance()}. The only time you would need to
     * create an export manager via the constructor is if you need to use more
     * than one simultaneously.
     */
    public ImageExporter() {
    }

    /**
     * Returns a shared export manager. Note that if you do not retain a
     * reference to the returned instance, it may be garbage collected at any
     * time. (Assign the returned value to a variable and leave it unmodified
     * for the duration of the export session.)
     *
     * @return returns a shared export manager
     */
    public static ImageExporter getSharedInstance() {
        ImageExporter em = null;
        if (shared != null) {
            em = shared.get();
        }
        if (em == null) {
            em = new ImageExporter();
            shared = new SoftReference<>(em);
        }
        return em;
    }

    /** Stores summary information about each exported image, for creating the readme file. */
    private static final class ItemData {
        public String name;
        public String link;
        public PrintDimensions dimensions;
        public boolean joined;
        public int pixelWidth;
    }

    private static SoftReference<ImageExporter> shared;
    private List<ItemData> itemData;
    private double targetDPI;
    private String format;
    private String baseSheetName;
    private String comments;
    private boolean joinImages;
    private boolean hasMarker;
    private Sheet<?> lastFace; // track last face written for face suppression
    private boolean suppressSimpleFaces;
    private EdgeStyle edgeStyle;
    private boolean joinPerformed;
    private BufferedImage joinImageLHS;
    private ExportContainer exporter;
    private SimpleImageWriter imageWriter;
    private int state = NOINIT;
    private static final int NOINIT = 0, INIT = 1, WROTEIMAGE = 2, ERROR = 1_000;

    private void handleWrappedException(Throwable t) throws IOException {
        if (t instanceof WrappedException) {
            WrappedException we = (WrappedException) t;
            t = we.getWrappedException();
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            ScriptMonkey.scriptError(t);
            throw we;
        }
    }

    /**
     * Begin an export operation. The user will be presented with an opportunity
     * to set options and choose a destination {@link ExportContainer}. The
     * {@code basename} will be used to form a suggested file name and as a base
     * name for constructing the names of the exported files. If this method
     * returns {@code false}, the user has cancelled the operation or an error
     * occurred.
     *
     * @param baseSheetName a base name for the exported files
     * @param comments notes that may optionally be included in the export
     * @param defaultDPI suggested default resolution
     * @param largeFormat a hint that the images are large and a lower
     * resolution may be appropriate
     * @param allowJoining a hint that an option to join the images should be
     * enabled
     * @param multipleFaces a hint that the export will consist of a card with
     * multiple faces
     * @param hasMarker a hint that the final sheet is a marker
     * @return {@code false} if the export was cancelled
     * @throws IOException if an I/O error occurs
     */
    public boolean beginExport(String baseSheetName, String comments, double defaultDPI, boolean largeFormat, boolean allowJoining, boolean multipleFaces, boolean hasMarker) throws IOException {
        if (state != NOINIT) {
            throw new IllegalStateException("must end previous export before starting a new one");
        }

        StrangeEons.log.info(String.format(
                "exporting %s@%.0f, large format %b, allow joining %b, multiple faces %b, has marker %b",
                baseSheetName, defaultDPI, largeFormat, allowJoining, multipleFaces, hasMarker
        ));

        this.hasMarker = hasMarker;
        usedSheetSuffixes.clear();
        imageWriter = null;

        ImageExportDialog efd = new ImageExportDialog((int) (defaultDPI + 0.5d), largeFormat, allowJoining, multipleFaces);
        if (!efd.showDialog()) {
            return false;
        }

        exporter = efd.getExportContainer();
        if (!exporter.selectLocation(baseSheetName, StrangeEons.getWindow().getRootPane())) {
            return false;
        }
        try {
            exporter.createContainer();
        } catch (WrappedException we) {
            handleWrappedException(we);
        }

        this.baseSheetName = baseSheetName;
        targetDPI = efd.getResolution();
        imageWriter = efd.getImageWriter();
        format = efd.getFormat();

        joinImages = efd.isImageJoinEnabled();
        joinImageLHS = null;
        joinPerformed = false;

        suppressSimpleFaces = efd.isFaceSuppressionEnabled();
        edgeStyle = efd.getEdgeStyle();

        itemData = new LinkedList<>();
        this.comments = comments;

        state = INIT;

        return true;
    }

    /**
     * Begin an export operation. The user will be presented with an opportunity
     * to set options and choose a destination {@link ExportContainer}. The
     * {@code basename} will be used to form a suggested file name and as a base
     * name for constructing the names of the exported files. If this method
     * returns {@code false}, the user has cancelled the operation or an error
     * occurred.
     *
     * @param baseSheetName a base name for the exported files
     * @param comments notes that may optionally be included in the export
     * @param defaultDPI suggested default resolution
     * @param sizeInPoints the dimensions of the content to be exported, in
     * points
     * @param allowJoining a hint that an option to join the images should be
     * enabled
     * @param multipleFaces a hint that the export will consist of a card with
     * multiple faces
     * @param hasMarker a hint that the final sheet is a marker
     * @return {@code false} if the export was cancelled
     * @throws IOException if an I/O error occurs
     */
    public boolean beginExport(String baseSheetName, String comments, double defaultDPI, PrintDimensions sizeInPoints, boolean allowJoining, boolean multipleFaces, boolean hasMarker) throws IOException {
        return beginExport(baseSheetName, comments, defaultDPI,
                sizeInPoints.getWidth() > 4.5 * 72 || sizeInPoints.getHeight() > 5.5 * 72,
                allowJoining, multipleFaces, hasMarker);
    }

    /**
     * Exports the next sheet.
     *
     * @param sheetSuffix the suffix to append to the base sheet name for this
     * sheet
     * @param face the sheet to export
     * @throws IOException if an I/O error occurs
     */
    public void exportSheet(String sheetSuffix, Sheet face) throws IOException {
        if (state == ERROR) {
            throw new IllegalStateException("cannot proceed with session due to an error");
        }
        if (state == INIT) {
            state = WROTEIMAGE;
        } else if (state != WROTEIMAGE) {
            throw new IllegalStateException("beginExport not called");
        }

        // if we've exported at least one face, we might suppress this face
        // if it is the same or is just a simple card back
        if (suppressSimpleFaces && lastFace != null) {
            if (face instanceof UndecoratedCardBack || face == lastFace) {
                return;
            }
        }
        lastFace = face;

        // we set the state to error: if an exception is thrown this is correct;
        // if no exception is thrown we set it to WROTEIMAGE at the end of the method
        state = ERROR;

        boolean oversampled = false;

        // requested default resolution
        if (targetDPI < 0d) {
            targetDPI = face.getPaintingResolution();
        }

        double renderDPI = targetDPI;
        if (Settings.getShared().getYesNo("use-export-oversampling") && targetDPI < Settings.getShared().getDouble("export-oversampling-threshold")) {
            oversampled = true;
            renderDPI *= 2;
        }

        // within this lock we change, draw, and restore the sheet settings
        BufferedImage i = face.paint(RenderTarget.EXPORT, renderDPI, edgeStyle);

        if (oversampled) {
            // bilinear scaling produces fewer JPEG artifacts than bicubic
            Object interpolation = format.equals("jpg")
                    ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                    : RenderingHints.VALUE_INTERPOLATION_BICUBIC;
            i = ImageUtilities.resample(i, i.getWidth() / 2, i.getHeight() / 2, false, interpolation, null);
        }

        boolean markJoinedInImageData = false;
        if (joinImages && !joinPerformed) {
            if (joinImageLHS == null) {
                joinImageLHS = i;
                // make no entry this time through
                state = WROTEIMAGE;
                return;
            } else {
                // replace the image to be exported with an image of the previous
                // sheet and this sheet side-by-side
                joinPerformed = true;
                BufferedImage joined = new BufferedImage(
                        joinImageLHS.getWidth() + i.getWidth(),
                        joinImageLHS.getHeight() >= i.getHeight() ? joinImageLHS.getHeight() : i.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = joined.createGraphics();
                g.setPaint(Color.WHITE);
                g.fillRect(0, 0, joined.getWidth(), joined.getHeight());
                g.drawImage(joinImageLHS, 0, 0, null);
                g.drawImage(i, joinImageLHS.getWidth(), 0, null);
                g.dispose();
                joinImageLHS = null;
                sheetSuffix = "";
                i = joined;
                markJoinedInImageData = true;
            }
        }

        imageWriter.setPixelsPerInch((float) targetDPI);
        makeEntry(sheetSuffix, i);
        itemData.get(itemData.size()-1).joined = markJoinedInImageData;
        state = WROTEIMAGE;
    }

    /**
     * Exports an image. The resolution is assumed to be
     * {@code getTargetResolution()}.
     *
     * @param sheetSuffix the suffix to append to the base sheet name for this
     * sheet
     * @param bi the image to export
     * @throws IOException if an I/O error occurs
     */
    public void exportImage(String sheetSuffix, BufferedImage bi) throws IOException {
        if (state == ERROR) {
            throw new IllegalStateException("cannot proceed with session due to an error");
        }
        if (state == INIT) {
            state = WROTEIMAGE;
        } else if (state != WROTEIMAGE) {
            throw new IllegalStateException("beginExport not called");
        }

        imageWriter.setPixelsPerInch((float) targetDPI);
        makeEntry(sheetSuffix, bi);
    }

    /**
     * Returns the target resolution that sheets will be rendered at when
     * exporting.
     *
     * @return the target resolution in pixels per inch
     */
    public double getTargetResolution() {
        if (state != INIT && state != WROTEIMAGE) {
            throw new IllegalStateException("target resolution invalid until beginExport completed");
        }

        return targetDPI;
    }

    private void makeEntry(String sheetSuffix, BufferedImage image) throws IOException {
        OutputStream out = null;
        try {
            String fileName = makeFileName(sheetSuffix, format);
            ItemData data = new ItemData();
            data.name = fileName;
            data.link = fileName;
            data.dimensions = new PrintDimensions(image, targetDPI);
            data.pixelWidth = image.getWidth();
            itemData.add(data);
            try {
                out = exporter.addEntry(fileName);
            } catch (WrappedException we) {
                handleWrappedException(we);
            }

            imageWriter.write(image, out);
        } catch (IOException e) {
            state = ERROR;
            exporter.destroyContainer();
            throw e;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private String makeFileName(String sheetSuffix, String formatSuffix) {
        if (sheetSuffix == null) {
            throw new NullPointerException("sheetSuffix");
        }

        Integer lastUsed = usedSheetSuffixes.get(sheetSuffix);
        if (lastUsed == null) {
            usedSheetSuffixes.put(sheetSuffix, 1);
        } else {
            int n = lastUsed + 1;
            usedSheetSuffixes.put(sheetSuffix, n);
            sheetSuffix += "-" + n;
        }

        String fileName = baseSheetName;
        if (!sheetSuffix.isEmpty()) {
            fileName += "-" + sheetSuffix;
        }
        return ResourceKit.makeStringFileSafe(fileName).replaceAll("\\s+", "-") + "." + formatSuffix;
    }
    private final HashMap<String, Integer> usedSheetSuffixes = new HashMap<>();

    private void makeReadme() throws IOException {
        Locale loc = Language.getGameLocale();
        Language gl = new Language(loc);
        gl.addStrings("text/game/image-export-text");
        SETemplateProcessor tp = new SETemplateProcessor(loc);
        tp.setGameLanguage(gl);

        comments = ResourceKit.makeStringHTMLSafe(comments.trim())
                .replace("\n\n", "<p>").replace("\n", "<br>");
        if(!comments.isEmpty()) comments = "<strong>" + gl.get("comments-label") + "</strong> " + comments;

        // if the format is one that can be displayed in all Web browsers,
        //   use <img> tags, otherwise we will create links to the files.
        boolean imagesUseDisplayableFormat = SimpleImageWriter.FORMAT_PNG.equals(format)
                || SimpleImageWriter.FORMAT_JPEG.equals(format)
                || SimpleImageWriter.FORMAT_GIF.equals(format);

        // set up the variables for the template processor
        tp.setAll(
                "name", ResourceKit.makeStringHTMLSafe(baseSheetName),
                "comments", comments,
                "ver", StrangeEons.getVersionString(),
                "lastItem", String.valueOf(itemData.size())
        );
        tp.setCondition("showPrintButton", imagesUseDisplayableFormat);


        String printSizes = "";
        String screenSizes = "";
        String imageLinks = "";
        for (int i=0, len=itemData.size(); i<len; ++i) {
            ItemData item = itemData.get(i);
            if(imagesUseDisplayableFormat) {
                boolean isMarker = hasMarker && i == itemData.size()-1;
                String cssClassName = "file" + (i+1);
                String imgTag = makeImageTag(item.link, cssClassName, item.joined);
                printSizes += makeCssPrintWidth(cssClassName, item.dimensions);
                screenSizes += makeCssScreenWidth(cssClassName, targetDPI, item.pixelWidth);
                imageLinks += isMarker ? makeMarkerWrapping(imgTag) : imgTag;
            } else {
                imageLinks += makeFileLink(item.link);
            }
        }
        tp.set("images", imageLinks);
        tp.set("printSizes", printSizes);
        tp.set("screenSizes", screenSizes);

        String readmeName = makeFileName(gl.get("file-name"), "html");
        String text = tp.processFromResource("text/game/image-export.template");

        BufferedWriter writer = null;
        try {
            try {
                OutputStream out = exporter.addEntry(readmeName);
                writer = new BufferedWriter(new OutputStreamWriter(out, TextEncoding.HTML_CSS_CS));
                writer.write(text);
            } catch (WrappedException we) {
                handleWrappedException(we);
            }
        } catch (IOException e) {
            state = ERROR;
            throw e;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static String makeFileLink(String fileName) {
        return "<a download href=\"" + fileName + "\">" + fileName + "</a>";
    }

    private static String makeImageTag(String fileName, String cssClassName, boolean isJoined) {
        return "<img alt=\"" + fileName + '"'
                + " class=\"" + cssClassName + (isJoined ? " joined" : "") + '"'
                + " onclick=\"show(this)\" src=\"" + fileName + '"'
                + '>';
    }

    private static String makeCssPrintWidth(String cssClassName, PrintDimensions size) {
        return '.' + cssClassName + "{width:" + size.getWidthInUnit(Length.IN) + "in}";
    }

    private static String makeCssScreenWidth(String cssClassName, double targetDPI, int pixelWidth) {
        if(targetDPI <= 150d) return "";
        pixelWidth = (int) Math.round(150d * pixelWidth / targetDPI);
        return '.' + cssClassName + "{width:" + pixelWidth + "px}";
    }

    private static String makeMarkerWrapping(String imgTag) {
        return "<div class=\"marker\">" + imgTag + imgTag + "</div>";
    }

    /**
     * Completes the export operation, writing a readme file into the export
     * container, closing it, and optionally opening the container depending on
     * application settings.
     *
     * @throws IOException if an I/O error occurs while closing the container
     */
    public void endExport() throws IOException {
        int oldState = state;
        state = NOINIT;

        if (oldState == ERROR) {
            cleanup();
            return;
        }

        if (oldState != WROTEIMAGE) {
            throw new IllegalStateException("must have exported at least one image");
        }

        makeReadme();
        exporter.closeContainer(Settings.getShared().getYesNo("open-zip-after-export"));
        cleanup();
    }

    private void cleanup() {
        itemData = null;
        comments = null;
        format = null;
        baseSheetName = null;
        comments = null;
        joinImageLHS = null;
        exporter = null;
        lastFace = null;
        if (imageWriter != null) {
            imageWriter.dispose();
            imageWriter = null;
        }
    }
}
