package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import resources.Language;

/**
 * Creates simple PDF documents. PDF support is not built in; to use this class
 * the {@code core-PDFOutput.selibrary} library must be installed. (That library
 * provides a concrete implementation of the {@link PDFWriter} interface defined
 * in this class.) You can test whether PDF support is available by calling
 * {@link #isAvailable()}.
 *
 * <p>
 * For easy PDF creation, use one of the static {@link #printToPDF printToPDF}
 * methods that take a {@link Printable} or {@link DeckEditor}. Alternatively,
 * call {@link #createPDFWriter()} to obtain a high-level interface to the PDF
 * engine.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class PDFPrintSupport {

    private PDFPrintSupport() {
    }

    /**
     * Returns {@code true} if PDF support is available.
     *
     * @return {@code true} if {@link #createPDFWriter} will not throw an
     * {@code UnsupportedOperationException} exception
     */
    public synchronized static boolean isAvailable() {
        if (knownOK) {
            return true;
        }
        if (DEFAULT_IMPLEMENTATION_CLASS.equals(IMPLEMENTATION_CLASS)) {
            if (PDF_LIB_UUID == null) {
                PDF_LIB_UUID = UUID.fromString("c7c96b28-2c70-4632-b364-e060fd3a25b3");
            }
            if (BundleInstaller.getBundleFileForUUID(PDF_LIB_UUID) != null) {
                knownOK = true;
            }
        } else {
            try {
                Class.forName(IMPLEMENTATION_CLASS, false, PDFPrintSupport.class.getClassLoader());
                knownOK = true;
            } catch (Throwable t) {
            }
        }
        return knownOK;
    }
    private static UUID PDF_LIB_UUID;
    private static boolean knownOK;

    private static synchronized void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * Prints the contents of any {@link Printable} object to a PDF file.
     *
     * @param title the title of the PDF file
     * @param pageFormat the page format to use
     * @param printable the content to print
     * @param pdfFile the PDF file to create
     */
    public static void printToPDF(final String title, final PageFormat pageFormat, final Printable printable, final File pdfFile) {
        if (pageFormat == null) {
            throw new NullPointerException("pageFormat");
        }
        if (printable == null) {
            throw new NullPointerException("printable");
        }
        if (pdfFile == null) {
            throw new NullPointerException("pdfFile");
        }

        if (!isAvailable()) {
            throw new UnsupportedOperationException();
        }

        new BusyDialog(Language.string("busy-exporting"), () -> {
            boolean existed = pdfFile.exists();
            try {
                PDFWriter w = createPDFWriter();
                try {
                    final Configuration c = new Configuration(pdfFile);
                    c.setTitle(title);
                    c.setAuthor(System.getProperty("user.name"));
                    c.setPageWidth(pageFormat.getWidth());
                    c.setPageHeight(pageFormat.getHeight());
                    w.setConfiguration(c);
                    BufferedImage page = null;
                    double ppi = 300d;
                    do {
                        try {
                            int pw = (int) (pageFormat.getWidth() * ppi / 72d + 0.5);
                            int ph = (int) (pageFormat.getHeight() * ppi / 72d + 0.5);
                            page = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
                        } catch (OutOfMemoryError oom) {
                            ppi -= 50d;
                            if (ppi < 150d) {
                                throw oom;
                            }
                        }
                    } while (page == null);

                    for (int pageIndex = 0;; ++pageIndex) {
                        Graphics2D g = page.createGraphics();
                        try {
                            applyHints(g);
                            g.setColor(Color.WHITE);
                            g.fillRect(0, 0, page.getWidth(), page.getHeight());
                            g.setColor(Color.BLACK);
                            g.scale(ppi / 72d, ppi / 72d);

                            int result = printable.print(g, pageFormat, pageIndex);
                            if (result == Printable.NO_SUCH_PAGE) {
                                break;
                            }
                        } finally {
                            g.dispose();
                        }

                        w.addPage(page, 0d, 0d, c.getPageWidth(), c.getPageHeight());
                    }
                } catch (IOException e) {
                    try {
                        w.close();
                        w = null;
                    } catch (IOException ie) {
                        throw e;
                    }
                } finally {
                    if (w != null) {
                        w.close();
                    }
                }
            } catch (PrinterAbortException pae) {
                if (!existed) {
                    pdfFile.delete();
                }
            } catch (Throwable t) {
                if (!existed) {
                    pdfFile.delete();
                }
                ErrorDialog.displayError(Language.string("psd-pdf-err"), t);
            }
        });
    }

    /**
     * Prints the contents of any {@link Printable} object to a PDF file.
     *
     * @param title the title of the PDF file
     * @param paper the paper type to use
     * @param printable the content to print
     * @param pdfFile the PDF file to create
     */
    public static void printToPDF(final String title, final PaperProperties paper, final Printable printable, final File pdfFile) {
        if (paper == null) {
            throw new NullPointerException("paper");
        }
        if (printable == null) {
            throw new NullPointerException("printable");
        }
        if (pdfFile == null) {
            throw new NullPointerException("pdfFile");
        }
        if (!isAvailable()) {
            throw new UnsupportedOperationException();
        }
        PageFormat pf = paper.createCompatiblePageFormat(false);
        printToPDF(title, pf, printable, pdfFile);
    }

    /**
     * Prints a the contents of a deck to a PDF file.
     *
     * @param deckEditor the editor displaying the deck to print
     * @param pdfFile the PDF file to write
     * @throws UnsupportedOperationException if PDF support is not available
     * @see #isAvailable()
     */
    public static void printToPDF(final DeckEditor deckEditor, final File pdfFile) {
        if (deckEditor == null) {
            throw new NullPointerException("deckEd");
        }
        if (pdfFile == null) {
            throw new NullPointerException("pdfFile");
        }

        if (!isAvailable()) {
            throw new UnsupportedOperationException();
        }

        new BusyDialog(Language.string("busy-exporting"), () -> {
            boolean existed = pdfFile.exists();
            try {
                PDFWriter w = createPDFWriter();
                final Deck deck = deckEditor.getDeck();
                try {
                    final Configuration c = new Configuration(pdfFile);
                    c.setTitle(deck.getName());
                    c.setAuthor(System.getProperty("user.name"));
                    c.setPageWidth(deck.getPaperProperties().getPageWidth());
                    c.setPageHeight(deck.getPaperProperties().getPageHeight());
                    w.setConfiguration(c);
                    BufferedImage page = null;
                    for (int i = 0; i < deck.getPageCount(); ++i) {
                        double ppi = 300d;
                        boolean ok = false;
                        do {
                            try {
                                page = deckEditor.createPageImage(page, i, ppi);
                                ok = true;
                            } catch (OutOfMemoryError oom) {
                                ppi -= 50d;
                                page = null;
                                if (ppi < 150d) {
                                    throw oom;
                                }
                            }
                        } while (!ok);
                        w.addPage(page, 0d, 0d, c.getPageWidth(), c.getPageHeight());
                    }
                } catch (IOException e) {
                    try {
                        w.close();
                        w = null;
                    } catch (IOException ie) {
                        throw e;
                    }
                } finally {
                    if (w != null) {
                        w.close();
                    }
                }
            } catch (Throwable t) {
                if (!existed) {
                    pdfFile.delete();
                }
                ErrorDialog.displayError(Language.string("psd-pdf-err"), t);
            }
        });
    }

    /**
     * Returns a new instance of a {@link PDFWriter}.
     *
     * @return a new, uninitialized PDF writer instance
     * @throws UnsupportedOperationException if no PDF support is available
     */
    public static synchronized PDFWriter createPDFWriter() {
        try {
            return (PDFWriter) Class.forName(IMPLEMENTATION_CLASS).getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            StrangeEons.log.log(Level.SEVERE, "expected to instantiate PDF writer but failed", e);
        } catch (Throwable t) {
        }
        throw new UnsupportedOperationException();
    }

    /**
     * An interface implemented by classes that can provide very basic PDF
     * writing support. To use a PDF writer, follow these steps:
     * <ol>
     * <li> Get a new writer instance by calling
     * {@link PDFPrintSupport#createPDFWriter()}.
     * <li> Create a new {@link Configuration} object and fill in the members
     * with suitable configuration information.
     * <li> Load the configuration into the writer by calling
     * {@link #setConfiguration(ca.cgjennings.apps.arkham.deck.PDFPrintSupport.Configuration)}.
     * <li> Call
     * {@linkplain #addPage(java.awt.image.BufferedImage, double, double, double, double) addPage}
     * in sequence for each page you wish to add, supplying an image of the page
     * content.
     * <li> Call {@link #close()} after all pages have been added.
     * </ol>
     *
     * <p>
     * <b>This interface may change incompatibly in future versions.</b>
     */
    public interface PDFWriter {

        /**
         * Sets the configuration information to be used by this writer to
         * create a PDF file.
         *
         * @param config configuration details that describe the destination,
         * page size, and basic metadata
         * @throws IOException if an I/O error occurs while initializing the PDF
         * file
         */
        void setConfiguration(Configuration config) throws IOException;

        /**
         * Adds a new page that consists of a single image.
         *
         * @param pageImage an image to be drawn on the page
         * @param xInPoints the x-offset at which to draw the image
         * @param yInPoints the y-offset at which to draw the image
         * @param widthInPoints the width of the image
         * @param heightInPoints the height of the image
         * @throws IOException if an I/O error occurs while adding the page
         */
        void addPage(BufferedImage pageImage, double xInPoints, double yInPoints, double widthInPoints, double heightInPoints) throws IOException;

        /**
         * Closes the PDF file once all pages have been added.
         *
         * @throws IOException if an I/O error occurs while finishing the PDF
         * file
         */
        void close() throws IOException;
    }

    private static float defQuality = 0.8f;

    /**
     * Returns the default output quality level for new {@link Configuration}s.
     *
     * @return the default quality setting
     */
    public static synchronized float getDefaultOutputQuality() {
        return defQuality;
    }

    /**
     * Sets the default output quality level. This is a value between 0 and 1
     * inclusive, where higher values suggest higher quality output, generally
     * at the cost of larger file size. This is the default quality level for
     * new {@link Configuration}s.
     *
     * @param quality the new quality setting
     * @throws IllegalArgumentException if the quality is not in the range 0 to
     * 1.
     * @see Configuration#setQuality
     */
    public static synchronized void setDefaultOutputQuality(float quality) {
        if (quality < 0f || quality > 1f) {
            throw new IllegalArgumentException("quality: " + quality);
        }
        defQuality = quality;
    }

    /**
     * This class is a simple container for the configuration information that
     * is passed to a {@link PDFWriter}.
     *
     * <p>
     * <b>This class may change incompatibly in future versions.</b>
     */
    public static final class Configuration {

        private File destination;
        private double pageWidthInPoints = 8.5d * 72d;
        private double pageHeightInPoints = 11d * 72d;
        private String title;
        private String author;
        private String subject;
        private float quality = getDefaultOutputQuality();

        /**
         * Creates a new configuration instance for writing a PDF file to the
         * specified destination.
         *
         * @param destination the PDF output file
         * @throws NullPointerException if the file is {@code null}
         */
        public Configuration(File destination) {
            setOutputFile(destination);
        }

        /**
         * Creates a new configuration that copies its state from a template.
         *
         * @param toCopy the configuration to copy
         */
        public Configuration(Configuration toCopy) {
            destination = toCopy.destination;
            pageWidthInPoints = toCopy.pageWidthInPoints;
            pageHeightInPoints = toCopy.pageHeightInPoints;
            title = toCopy.title;
            author = toCopy.author;
            subject = toCopy.subject;
        }

        /**
         * Returns the file that the destination will be written to; may not be
         * {@code null}.
         *
         * @return the destination
         */
        public File getOutputFile() {
            return destination;
        }

        /**
         * Sets the file that the destination will be written to; may not be
         * {@code null}.
         *
         * @param destination the PDF output file
         * @throws NullPointerException if the file is {@code null}
         */
        public void setOutputFile(File destination) {
            if (destination == null) {
                throw new NullPointerException("destination");
            }
            this.destination = destination;
        }

        /**
         * Sets the width and height of document pages from a
         * {@link PaperProperties} object representing the target paper size.
         *
         * @param pp the paper properties to copy the page width and height from
         */
        public void setPageDimensions(PaperProperties pp) {
            setPageWidth(pp.getPageWidth());
            setPageHeight(pp.getPageHeight());
        }

        /**
         * Returns the width of document pages, measured in points.
         *
         * @return the page width in points
         */
        public double getPageWidth() {
            return pageWidthInPoints;
        }

        /**
         * Sets the width of document pages, measured in points; must be a
         * positive value.
         *
         * @param pageWidthInPoints the page width
         */
        public void setPageWidth(double pageWidthInPoints) {
            if (pageWidthInPoints <= 0d) {
                throw new IllegalArgumentException("pageWidthInPoints: " + pageWidthInPoints);
            }
            this.pageWidthInPoints = pageWidthInPoints;
        }

        /**
         * Returns the height of document pages, measured in points.
         *
         * @return the page height in points
         */
        public double getPageHeight() {
            return pageHeightInPoints;
        }

        /**
         * The height of document pages, measured in points; must be a positive
         * value.
         *
         * @param pageHeightInPoints the pageHeightInPoints to set
         */
        public void setPageHeight(double pageHeightInPoints) {
            if (pageHeightInPoints <= 0d) {
                throw new IllegalArgumentException("pageHeightInPoints: " + pageHeightInPoints);
            }
            this.pageHeightInPoints = pageHeightInPoints;
        }

        /**
         * Returns the value to use as the title of the document in the PDF
         * metadata; if {@code null} a default, empty value is used.
         *
         * @return the document title
         */
        public String getTitle() {
            return title;
        }

        /**
         * Sets the value to use as the title of the document in the PDF
         * metadata; if {@code null} a default, empty value is used.
         *
         * @param title the document title to set
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Returns the value to use as the document author's name in the PDF
         * metadata; if {@code null} a default, empty value is used.
         *
         * @return the document author
         */
        public String getAuthor() {
            return author;
        }

        /**
         * Sets the value to use as the document author's name in the PDF
         * metadata; if {@code null} a default, empty value is used.
         *
         * @param author the document author to set
         */
        public void setAuthor(String author) {
            this.author = author;
        }

        /**
         * Returns the value to use as the subject of the document in the PDF
         * metadata; if {@code null} a default, empty value is used.
         *
         * @return the document subject
         */
        public String getSubject() {
            return subject;
        }

        /**
         * Sets the value to use as the subject of the document in the PDF
         * metadata; if {@code null} a default, empty value is used.
         *
         * @param subject the document subject to set
         */
        public void setSubject(String subject) {
            this.subject = subject;
        }

        /**
         * Returns the output quality level. This is a value between 0 and 1
         * inclusive, where 1 represents maximum quality and 0 represents
         * minimum file size.
         *
         * @return the quality setting
         */
        public float getQuality() {
            return quality;
        }

        /**
         * Sets the output quality level. This is a value between 0 and 1
         * inclusive, where higher values suggest higher quality output,
         * generally at the cost of larger file size. Note that PDF writers are
         * not required to use this value.
         *
         * @param quality the new quality setting
         * @throws IllegalArgumentException if the quality is not in the range 0
         * to 1.
         */
        public void setQuality(float quality) {
            if (quality < 0f || quality > 1f) {
                throw new IllegalArgumentException("quality: " + quality);
            }
            this.quality = quality;
        }
    }

    private static final String DEFAULT_IMPLEMENTATION_CLASS = "ca.cgjennings.apps.arkham.deck.PDFWriterImpl";
    private static String IMPLEMENTATION_CLASS = DEFAULT_IMPLEMENTATION_CLASS;

    /**
     * Sets the name of the concrete {@link PDFWriter} class to use.
     *
     * @param className the fully qualified name of a class with a no-arg
     * constructor that implements {@link PDFWriter}, or {@code null} to use the
     * default class
     */
    public static synchronized void setImplementationClassName(String className) {
        if (className == null) {
            className = DEFAULT_IMPLEMENTATION_CLASS;
        } else {
            // verify that the class exists before switching
            try {
                Class.forName(className, false, PDFPrintSupport.class.getClassLoader());
            } catch (Throwable t) {
                throw new IllegalArgumentException("could not find class: " + className);
            }
        }
        if (IMPLEMENTATION_CLASS.equals(className)) {
            return;
        }
        IMPLEMENTATION_CLASS = className;
        knownOK = false;
    }

    /**
     * Returns the name of the concrete {@link PDFWriter} class to use.
     *
     * @return the fully qualified name of a class use to create new
     * {@link PDFWriter}s
     */
    public static synchronized String getImplementationClassName() {
        return IMPLEMENTATION_CLASS;
    }
}
