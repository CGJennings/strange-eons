package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.sheet.FinishStyle;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import resources.Settings;

/**
 * A simple component for displaying a Sheet instance.
 */
@SuppressWarnings("serial")
public class SheetViewer extends AbstractViewer {

    private Sheet sheet;
    private double upsampleFactor;

    public SheetViewer() {
        super();
    }

    public void setSheet(Sheet c) {
        sheet = c;
        if (sheet != null) {
            // pre-renders before trying to show window; prevents
            // long delay with blank window at startup
            getCurrentImage();
        }
        repaint();
    }

    public Sheet getSheet() {
        return sheet;
    }

    /**
     * Returns the current image to be drawn in the viewer.
     *
     * @return the image the viewer should display
     */
    @Override
    protected BufferedImage getCurrentImage() {
        try {
            final ViewQuality vq;
            if (ViewQuality.isManagedAutomatically()) {
                if (stats == null) {
                    stats = new QualityManager();
                }
                vq = stats.chooseNext();
            } else {
                vq = ViewQuality.get();
            }

            final Sheet<?> sheet = getSheet();
            final RenderTarget rt = vq.getRenderTarget();
            upsampleFactor = vq.getSheetViewerUpsample(sheet);

            final long start = System.nanoTime();
            final boolean hadChanges = sheet.hasChanged();

            // if preview style UBM > 0 and sheet UBM > 0, do not change:
            // this is a little hack so that the user can play with
            // the UBM programmatically for testing and design
            final FinishStyle fs = FinishStyle.getPreviewStyle();
            if (!(sheet.getUserBleedMargin() > 0d && fs.getSuggestedBleedMargin() > 0d)) {
                FinishStyle.applyPreviewStyleToSheet(sheet);
            }

            BufferedImage image = sheet.paint(
                    rt, upsampleFactor * sheet.getTemplateResolution()
            );
            final long time = System.nanoTime() - start;
            if (ViewQuality.isManagedAutomatically()) {
                stats.updateStats((int) (time / 1000000L), hadChanges);
            }
            return image;
        } catch (Exception e) {
            // this prevents the UI from becoming noninteractive when
            // the sheet throws an uncaught exception during drawing
            if (!hadCurrentImageException) {
                hadCurrentImageException = true;
                StrangeEons.log.log(Level.WARNING, null, e);
            }
            return null;
        }
    }
    private boolean hadCurrentImageException = false;

    private QualityManager stats;

    private static class QualityManager {

        static ViewQuality autoDefault = ViewQuality.HIGH;
        ViewQuality current = autoDefault;
        boolean measureNext = true;
        private final int[] sums = new int[4];
        private final int[] renders = new int[4];
        private final int[] rollovers = new int[4];

        public ViewQuality chooseNext() {
            ViewQuality next = current;
            int i = current.ordinal();

            if (rollovers[i] > 0 && renders[i] > 2) {
                int mean = sums[i] / renders[i];
                if (mean > 500) {
                    next = bump(next, false);
                    if (mean > 2000) {
                        // very slow draws can degrade the default for future
                        // components, but can never degrade to LOW
                        // (bump() will never return LOW)
                        if (autoDefault.ordinal() > next.ordinal()) {
                            StrangeEons.log.log(Level.INFO, "reducing default component preview quality to {0}", next);
                            autoDefault = next;
                        }
                        // only degrade to low quality if things are really,
                        // really bad (redraws > 2s)
                        if (current == ViewQuality.MEDIUM || current == ViewQuality.LOW) {
                            next = ViewQuality.LOW;
                        }
                    }
                } else if (mean < 160) {
                    next = bump(next, true);
                }
            }

            if (next != current) {
                StrangeEons.log.log(Level.INFO, "autoswitching component preview quality to {0}", next);
                measureNext = true;
                current = next;
            }
            return current;
        }

        private ViewQuality bump(ViewQuality current, boolean up) {
            ViewQuality vq;
            if (up) {
                switch (current) {
                    case LOW:
                        vq = ViewQuality.MEDIUM;
                        break;
                    case MEDIUM:
                        vq = ViewQuality.HIGH;
                        break;
                    case HIGH:
                    case ULTRAHIGH:
                        vq = ViewQuality.ULTRAHIGH;
                        break;
                    default:
                        throw new AssertionError();
                }
            } else {
                switch (current) {
                    case ULTRAHIGH:
                        vq = ViewQuality.HIGH;
                        break;
                    case HIGH:
                    case MEDIUM:
                    case LOW:
                        vq = ViewQuality.MEDIUM;
                        break;
                    default:
                        throw new AssertionError();
                }
            }
            return vq;
        }

        public void updateStats(int drawTime, boolean hadChanges) {
            if (measureNext || hadChanges) {
                int i = current.ordinal();
                sums[i] += drawTime;
                if (sums[i] < 0 || renders[i] == 4) {
                    sums[i] = drawTime;
                    renders[i] = 1;
                    rollovers[i]++;
                } else {
                    renders[i]++;
                }

                measureNext = false;
            }
        }

    }

    /**
     * Called from the abstract editor timer to update the display when the card
     * is changed. Causes the view to be redrawn if a sheet is visible and is
     * out of date.
     */
    void rerenderImage() {
        if (!isShowing() || getWidth() < 5 || getHeight() < 5) {
            return;
        }
        // Possible fix for OS X flashing: request image now so it is
        // already available during repaint; keep last non-null image
        getCurrentImage();
        repaint();
    }

    private BufferedImage lastImage;

    @Override
    protected void paintComponent(Graphics g1) {
        try {
            Graphics2D g = (Graphics2D) g1;
            Shape oldClip = g.getClip();

            // adjust for border insets
            double compWidth, compHeight;
            {
                borderInsets = getInsets(borderInsets);
                final int cw = getWidth() - (borderInsets.left + borderInsets.right);
                final int ch = getHeight() - (borderInsets.top + borderInsets.bottom);
                g.translate(borderInsets.left, borderInsets.top);
                g.clipRect(0, 0, cw, ch);
                compWidth = cw - 4d;
                compHeight = ch - 4d;
            }

            g.setPaint(getBackgroundPaint());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (getSheet() == null) {
                return;
            }

            final ViewQuality vq = ViewQuality.get();
            vq.applyPreviewWindowHints(g);

            // updates upsampleFactor as a side effect
            BufferedImage currentImage = getCurrentImage();
            if (currentImage == null) {
                if (lastImage == null) {
                    return;
                }
                currentImage = lastImage;
            } else {
                lastImage = currentImage;
            }

            // scaleAdjust is used to keep the size the user perceives as "100%"
            // the same even if the upsample factor changes
            double scaleAdjust = 1d;
            if (upsampleFactor != sheet.getSuggestedUpsampleFactor()) {
                scaleAdjust = upsampleFactor / sheet.getSuggestedUpsampleFactor();
            }

            double windowFitScale;
            {
                final double hScale = compWidth / currentImage.getWidth() * scaleAdjust;
                final double vScale = compHeight / currentImage.getHeight() * scaleAdjust;
                windowFitScale = Math.min(hScale, vScale);
            }
            if (windowFitScale > 1d && isEnabled()) {
                windowFitScale = 1d;
            }

            // visualScale is the apparent scale from the user's point of view
            //   this is adjusted for the upsample factor to create a scale value
            //   that is constant with respect to printed size
            // scale is the actual scaling factor applied to the image
            if (autoFitToWindow) {
                windowFitScale *= userScaleMultiplier;
            } else {
                windowFitScale = userScaleMultiplier;
            }

            final Settings settings = Settings.getShared();

            double visualScale = windowFitScale;
            windowFitScale /= scaleAdjust;
//			visualScale *= sheet.getSuggestedUpsampleFactor();

            double newWidth = currentImage.getWidth() * windowFitScale;
            double newHeight = currentImage.getHeight() * windowFitScale;
            double x = 2d + (compWidth - newWidth) / 2;
            double y = 2d + (compHeight - newHeight) / 2;

            int ix = (int) Math.round(x + tx);
            int iy = (int) Math.round(y + ty);
            if (windowFitScale <= 0.4999d && settings.getYesNo("use-downsample-preservation")) {
                BufferedImage downsamp = ImageUtilities.resample(currentImage, (float) windowFitScale);
                if (visualScale < 0.5d && settings.getYesNo("use-downsample-sharpening")) {
                    g.drawImage(downsamp, getSharpenKernel(), ix, iy);
                } else {
                    g.drawImage(downsamp, ix, iy, null);
                }
            } else {
                int w = Math.round((float) newWidth);
                int h = Math.round((float) newHeight);
                BufferedImage img = currentImage;
                if (visualScale < 0.5d && settings.getYesNo("use-downsample-sharpening")) {
                    img = getSharpenKernel().filter(img, null);
                }
                g.drawImage(img, ix, iy, w, h, null);
            }

            paintZoomLabel(g, visualScale);

            if (visualScale < scaleAdjust) {
                paintLoupe(g, currentImage, ix, iy, newWidth, newHeight);
            }

            g.setClip(oldClip);

        } catch (OutOfMemoryError e) {
            if (!haveShownOutOfMemoryError) {
                haveShownOutOfMemoryError = true;
                SwingUtilities.invokeLater(ErrorDialog::outOfMemory);
            }
        }
    }
    protected static boolean haveShownOutOfMemoryError = false;

    /**
     * Returns an image op that applies a sharpening filter to an image.
     *
     * @return a light sharpening filter
     */
    private static BufferedImageOp getSharpenKernel() {
        if (sharpenOp == null) {
            sharpenOp = new ConvolveOp(
                    new Kernel(3, 3, new float[]{
                0.00f, -0.25f, 0.00f,
                -0.25f, 2.00f, -0.25f,
                0.00f, -0.25f, 0.00f
            }), ConvolveOp.EDGE_NO_OP, null
            );
        }
        return sharpenOp;
    }
    private static BufferedImageOp sharpenOp;
}
