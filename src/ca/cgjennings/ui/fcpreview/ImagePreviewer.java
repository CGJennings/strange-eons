package ca.cgjennings.ui.fcpreview;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import resources.ResourceKit;
import resources.StrangeImage;

/**
 * A generic preview panel that can display previews based on arbitrary
 * "resource pointers". These are usually files, but can be any object. This
 * class handles image files; subclasses can override
 * {@link #isResourceTypeSupported} and {@link #createPreviewImage} to support
 * other types of resource pointers or resource types.
 *
 * <p>
 * A special constructor allows use as a self-installing file chooser accessory.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ImagePreviewer extends JPanel {

    private static Border panelBorder;
    private JLabel loadingCard;
    protected final ca.cgjennings.apps.arkham.ImageViewer viewer;
    private CardLayout card;

    public ImagePreviewer() {
        Color background = UIManager.getColor("background");
        if (background == null) {
            JPanel panel = new JPanel();
            background = panel.getBackground();
        }
        if (background == null) {
            background = Color.BLACK;
        }
        background = new Color(background.getRGB());

        viewer = new ca.cgjennings.apps.arkham.ImageViewer();
        viewer.setBackground(background);
        setBackground(background);
        setOpaque(true);

        card = new CardLayout();
        setLayout(card);

        loadingCard = new JLabel();
        loadingCard.setIcon(ResourceKit.createWaitIcon(loadingCard));
        loadingCard.setBackground(background);
        loadingCard.setHorizontalAlignment(JLabel.CENTER);
        loadingCard.setOpaque(true);

        add(viewer, "v");
        add(loadingCard, "w");
        setPreferredSize(new Dimension(300, 300));

        viewer.setEnabled(false);
        viewer.setShowZoomLevel(false);

        setPreviewImage(nullImage);
    }

    public ImagePreviewer(JFileChooser fc) {
        this();

        if (panelBorder == null) {
            JScrollPane borderToCopy = new JScrollPane();
            panelBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, 6, 0, 0),
                    BorderFactory.createCompoundBorder(
                            borderToCopy.getBorder(),
                            BorderFactory.createEmptyBorder(3, 3, 3, 3)
                    )
            );
        }
        setBorder(panelBorder);

        fc.addPropertyChangeListener(this::fileSelectionChange);
        fc.setAccessory(this);

        Dimension size = fc.getSize();
        size.width += 200;
        if (size.height < size.width) {
            size.height = size.width;
        }
        fc.setSize(size);
    }

    private class LoaderThread extends Thread {

        private final Object f;

        public LoaderThread(Object f) {
            this.f = f;
        }

        @Override
        public void run() {
            BufferedImage temp = null;
            try {
                temp = createPreviewImage(f);
            } catch (Throwable t) {
                StrangeEons.log.log(Level.INFO, "previewer could not load image: " + f, t);
            }
            if (temp != null && !Thread.interrupted()) {
                final BufferedImage tn = temp;
                EventQueue.invokeLater(() -> {
                    setPreviewImage(tn);
                });
            }
            EventQueue.invokeLater(() -> {
                if (loadThread == LoaderThread.this) {
                    loadThread = null;
                }
                card.show(ImagePreviewer.this, "v");
            });
        }
    };

    private volatile LoaderThread loadThread = null;

    private void changeImage(Object o) {
        if (o == null) {
            setPreviewImage(null);
            return;
        }

        viewer.setImage(null);
        card.show(this, "v");

        LoaderThread old = loadThread;
        if (old != null) {
            old.interrupt();
        }

        loadThread = new LoaderThread(o);
        loadThread.start();
        try {
            Thread.sleep(loadIconDelay);
        } catch (InterruptedException i) {
        }
        if (loadThread != null) {
            card.show(this, "w");
        }
    }

    /**
     * Returns the current load delay, in milliseconds.
     *
     * @return the delay before a load animation is shown
     * @see #setLoadProgressDelay(int)
     */
    public final int getLoadProgressDelay() {
        return loadIconDelay;
    }

    /**
     * Sets the delay to allow loading the preview image before an animated
     * delay indicated is shown. Note that this should not be set too high, as
     * the event dispatch thread will be blocked for this duration when the
     * selection changes.
     *
     * @param loadIconDelayInMilliseconds the delay before a load animation is
     * shown
     */
    public final void setLoadProgressDelay(int loadIconDelayInMilliseconds) {
        this.loadIconDelay = loadIconDelayInMilliseconds;
    }

    private volatile int loadIconDelay = 20;

    public void setZoomable(boolean zoomable) {
        viewer.setEnabled(zoomable);
        viewer.setShowZoomLevel(zoomable);
    }

    public boolean isZoomable() {
        return viewer.isEnabled();
    }

    /**
     * Creates the preview image from the target file. Subclasses should
     * override this to create a custom previewer for a different file type.
     * Note that this is typically
     * <b>not</b> run from the event dispatch thread. If constructing the image
     * fails for any reason, this method must return {@code null} and
     * should not display an error message.
     *
     * @param f the file to compose a preview of
     * @return the preview image
     */
    protected BufferedImage createPreviewImage(Object f) {
        BufferedImage temp = null;
        try {
            StrangeImage si = StrangeImage.get(f.toString());
            temp = si.asBufferedImage();
        } catch (Throwable t) {
            StrangeEons.log.log(Level.WARNING, "failed to create peview", t);
        }
        return temp;
    }

    /**
     * Returns {@code true} if this file appears to be of a type for which
     * a preview can be created.
     *
     * @param o the file to check
     * @return {@code true} if previewing this file is expected to succeed
     */
    public boolean isResourceTypeSupported(Object o) {
        if (!(o instanceof File)) {
            return false;
        }
        final File f = (File) o;
        if (f.isDirectory()) {
            return false;
        }
        String ext = f.getName();
        int dot = ext.indexOf('.');
        if (dot >= 0) {
            ext = ext.substring(dot + 1);
        } else {
            ext = "";
        }
        ext = ext.toLowerCase(Locale.CANADA);
        for (int i = 0; i < IMAGE_SUFFIXES.length; ++i) {
            if (IMAGE_SUFFIXES[i].equals(ext)) {
                return true;
            }
        }
        return false;
    }
    private static final String[] IMAGE_SUFFIXES = {
        "jp2", "png", "jpg", "jpeg", "svg", "svgz", "bmp", "gif"
    };

    private void fileSelectionChange(PropertyChangeEvent e) {
        boolean update = false;
        String prop = e.getPropertyName();
        File file = null;

        //If the directory changed, don't show an image.
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
            update = true;
            //If a file became selected, find out which one.
        } else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
            file = (File) e.getNewValue();
            update = true;
            if (file != null && !isResourceTypeSupported(file)) {
                file = null;
            }
        }

        //Update the preview accordingly.
        if (update) {
            if (isShowing()) {
                changeImage(file);
            }
        }
    }

    public void showPreview(Object o) {
        if (!isResourceTypeSupported(o)) {
            o = null;
        }
        if (isShowing()) {
            changeImage(o);
        }
    }

    public void setPreviewImage(BufferedImage i) {
        if (i == null) {
            if (nullImage == null) {
                final String icon = "icons/" +
                        (ThemeInstaller.getInstalledTheme().isDark()
                        ? "back-fedora.png"
                        : "fedora.png");
                nullImage = ResourceKit.getImage(icon);
            }
            i = nullImage;
            card.show(this, "v");
        }
        viewer.setImage(i);
    }
    private static BufferedImage nullImage;
}
