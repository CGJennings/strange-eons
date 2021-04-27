package ca.cgjennings.ui;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.paints.CheckeredPaint;
import ca.cgjennings.layout.GraphicStyleFactory;
import ca.cgjennings.ui.dnd.FileDrop;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import resources.ResourceKit;

/**
 * A box to drop image files on in order to fetch the image or image file. The
 * selected image can be changed by calling {@link #setFile} or
 * {@link #setImage}. Changes to the selected image can be detected by
 * registering a property change listener and listening for changes to the
 * property <code>SELECTED_IMAGE_CHANGED_PROPERTY</code>.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JIconDrop extends JLabel {

    private BufferedImage image, original;
    private File file;
    private int idealSize;

    public static final String SELECTED_IMAGE_CHANGED_PROPERTY = "IMAGE_CHANGED";

    public JIconDrop() {
        setIdealDropBoxSize(64);
        setBorder(BorderFactory.createEtchedBorder());
        setIconTextGap(0);
        setHorizontalAlignment(CENTER);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        new FileDrop(this, (File[] files) -> {
            if (files.length == 0) {
                return;
            }
            setFile(files[0]);
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    File f = ResourceKit.showImageFileDialog(JIconDrop.this);
                    if (f != null) {
                        setFile(f);
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        Paint p = g.getPaint();
        getInsets(paintInsets);
        g.setPaint(paint);
        g.fillRect(paintInsets.left, paintInsets.top, getWidth() - paintInsets.left - paintInsets.right, getHeight() - paintInsets.top - paintInsets.bottom);
        super.paintComponent(g);
        g.setPaint(p);
    }
    private Paint paint = new CheckeredPaint();
    private Insets paintInsets = new Insets(0, 0, 0, 0);

    /**
     * Sets the paint used to draw the backdrop behind the image. If
     * <code>null</code>, a default paint will be used.
     *
     * @param p
     */
    public void setBackgroundPaint(Paint p) {
        if (p == null) {
            p = new CheckeredPaint();
        }
        paint = p;
        repaint();
    }

    public Paint getBackgroundPaint() {
        return paint;
    }

    /**
     * This call is rerouted to {@link #setBackgroundPaint}.
     *
     * @param c the background color to set
     */
    @Override
    public void setBackground(Color c) {
        setBackgroundPaint(paint);
    }

    /**
     * Returns the background paint, if it is a color. Otherwise, returns the
     * background color of the parent. It is preferable to call
     * {@link #getBackgroundPaint()} instead.
     *
     * @return the background paint, if a color, or the parent's background
     */
    public Color getBackColor() {
        if (paint instanceof Color) {
            return (Color) paint;
        } else {
            return super.getBackground();
        }
    }

    /**
     * Returns the file that was the source of the current image, or
     * <code>null</code> if no image is set or if the image did not come from a
     * file.
     *
     * @return the file that the current image came from, or <code>null</code>
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the current image by loading it from a file.
     *
     * @param file the file containing the selected image
     */
    public void setFile(File file) {
        if (file == null) {
            setImage(null);
            this.file = null;
        } else {
            setImage(GraphicStyleFactory.fetchImage(file.getAbsolutePath()));
            // set this after calling setImage because setImage nulls it out
            this.file = file;
        }
    }

    /**
     * Returns the currently selected image, or <code>null</code> if no image is
     * selected.
     *
     * @return the selected image
     */
    public BufferedImage getImage() {
        return original;
    }

    /**
     * Sets the selected image, or clears the selection if the image is
     * <code>null</code>. Setting the image directly will also set the current
     * file to <code>null</code>. If <code>image</code> is not in an integer RGB
     * format, the component will create a copy of the image in such a format,
     * using the converted image in place of the original.
     *
     * @param image the image to select
     */
    public void setImage(BufferedImage image) {
        if (original != image) {
            final BufferedImage oldImage = original;
            image = ImageUtilities.ensureIntRGBFormat(image);
            original = image;
            this.image = image;
            setIcon(createIconForImage());
            this.file = null;

            firePropertyChange(SELECTED_IMAGE_CHANGED_PROPERTY, oldImage, image);
        }
    }

    protected Icon createIconForImage() {
        if (image == null) {
            return null;
        }
        image = getImageAtSize(idealSize);
        return new ImageIcon(image);
    }

    public int getIdealDropBoxSize() {
        return idealSize;
    }

    public void setIdealDropBoxSize(int idealSize) {
        this.idealSize = idealSize;
        int boxSize = Math.max(72, idealSize + 8);
        Dimension size = new Dimension(boxSize, boxSize);
        setMinimumSize(size);
        setMaximumSize(size);
        setPreferredSize(size);
    }

    public BufferedImage getImageAtSize(int size) {
        return getImageAtSize(size, size);
    }

    public BufferedImage getImageAtSize(int width, int height) {
        BufferedImage source = getImage();
        if (source == null) {
            return null;
        }
        if (source.getWidth() > width || source.getHeight() > height) {
            float scale = ImageUtilities.idealCoveringScaleForImage(width, height, source.getWidth(), source.getHeight());
            source = ImageUtilities.resample(source, scale);
        }
        if (source.getWidth() != width || source.getHeight() != height) {
            source = ImageUtilities.center(source, width, height);
        }
        return source;
    }
}
