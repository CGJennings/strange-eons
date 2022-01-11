package ca.cgjennings.ui;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import resources.ResourceKit;

/**
 * An eye dropper component that the user can drag to select any colour on the
 * screen. To discover colours that the user selects, listen for changes to the
 * {@link #DROPPER_COLOR_CHANGED} property.
 *
 * <p>
 * To work, {@link Robot} must be available. If not available, the control will
 * be set to disabled and invisible and will do nothing when dragged. To
 * determine if the eye dropper will work beforehand, call {@link #isSupported}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
@SuppressWarnings("serial")
public class EyeDropper extends JLabel {

    /**
     * The name of a property that changes while the user drags the eye dropper.
     * To listen for colour changes, add a property change listener for this
     * value. The new value of the property is the {@link Color} under the eye
     * dropper.
     */
    public static final String DROPPER_COLOR_CHANGED = "DropperColor";

    /**
     * Creates a new eye dropper. If {@link #isSupported()} returns {@code true}
     * and the component is added to a window, the user will be able to drag the
     * eye dropper icon from the panel around the display in order to select
     * colours.
     */
    public EyeDropper() {
        initialize();
        setIcon(regularIcon);
        setCursor(regularCursor);
        setText(null);
        setAutoscrolls(true);
        if (robot == null) {
            setEnabled(false);
            setVisible(false);
        } else {
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    Color c = robot.getPixelColor(e.getXOnScreen(), e.getYOnScreen());
                    if (isBackgroundMatched()) {
                        setOpaque(true);
                        setBackground(c);
                    } else {
                        setOpaque(opaqueCache);
                        setBackground(backgroundCache);
                    }
                    firePropertyChange(DROPPER_COLOR_CHANGED, null, c);

                    Component gp = findGlassPane();
                    if (gp != null) {
                        gp.setCursor(dragCursor);
                    }
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    ++globalDragCount;
                    opaqueCache = isOpaque();
                    backgroundCache = getBackground();

                    Component gp = findGlassPane();
                    if (gp != null) {
                        cursorCache = gp.getCursor();
                        gp.setVisible(true);
                        gp.setCursor(dragCursor);
                        gp.addComponentListener(noClearListener);
                    }
                    setCursor(dragCursor);
                    setIcon(dragIcon);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    --globalDragCount;
                    setOpaque(opaqueCache);
                    setBackground(backgroundCache);

                    Component gp = findGlassPane();
                    if (gp != null) {
                        gp.removeComponentListener(noClearListener);
                        gp.setCursor(cursorCache);
                        gp.setVisible(false);
                    }
                    setCursor(regularCursor);
                    setIcon(regularIcon);
                }
            });
        }
    }

    // attached to the glass pane while the dropper is dragged so that
    // the glass pane cannot be hidden, e.g., during a wait cursor cycle
    private ComponentListener noClearListener = new ComponentAdapter() {
        @Override
        public void componentHidden(ComponentEvent e) {
            final Component gp = e.getComponent();
            EventQueue.invokeLater(() -> {
                gp.setVisible(true);
                gp.setCursor(dragCursor);
            });
        }
    };

    /**
     * Returns {@code true} if <i>any</i> {@code EyeDropper} is currently being
     * used to take colour samples.
     *
     * @return {@code true} if an eye dropper instance is being dragged
     * @since 3.0
     */
    public static boolean isCurrentlySampling() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("can only be called from EDT");
        }
        return globalDragCount > 0;
    }

    private Component findGlassPane() {
        JRootPane rp = SwingUtilities.getRootPane(this);
        if (rp == null) {
            return null;
        }
        return rp.getGlassPane();
    }

    /**
     * Sets whether the background colour of this control should be updated to
     * reflect the colour under the eye dropper as it is dragged.
     *
     * @param match if {@code true}, the background colour of the control is
     * updated automatically
     */
    public void setBackgroundMatched(boolean match) {
        updateBackground = match;
    }

    /**
     * Returns {@code true} if the background colour of this control is updated
     * to reflect the colour under the eye dropper as it is dragged.
     *
     * @return {@code true} if the background is set to the colour under the
     * dropper
     */
    public boolean isBackgroundMatched() {
        return updateBackground;
    }

    /**
     * Returns {@code true} if the eye dropper is usable on this platform. If
     * this returns {@code false}, the control can still be added to a layout
     * but it will be disabled and invisible.
     *
     * @return {@code true} if the dropper is supported on this platform
     */
    public static boolean isSupported() {
        initialize();
        return robot != null;
    }

    private static void initialize() {
        if (isInitialized) {
            return;
        }
        try {
            robot = new java.awt.Robot();
        } catch (AWTException e) {
            // robot not supported
        }

        // fallback cursor if we can't set our custom one
        dragCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        regularCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

        BufferedImage ed = ResourceKit.getThemedImage("icons/ui/controls/eyedropper.png");
        regularIcon = new ImageIcon(ed);
        dragIcon = new BlankIcon(regularIcon.getIconWidth(), regularIcon.getIconHeight());
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getBestCursorSize(ed.getWidth(), ed.getHeight());
        // we can make a cursor at least as big as our image,
        // so we will use a custom cursor
        if (d.width >= ed.getWidth() && d.height >= ed.getHeight()) {
            Point hotspot = new Point(0, ed.getHeight() - 1);
            // we need to extend the image
            if (d.width != ed.getWidth() || d.height != ed.getHeight()) {
                BufferedImage resized = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = resized.createGraphics();
                try {
                    g.drawImage(ed, 0, 0, null);
                } finally {
                    g.dispose();
                }
                ed = resized;
            }
            dragCursor = tk.createCustomCursor(ed, hotspot, "Eye Dropper");
        }

        isInitialized = true;
    }
    private static boolean isInitialized = false;
    private static Robot robot;
    private static Icon regularIcon, dragIcon;
    private static Cursor dragCursor, regularCursor;

    private boolean opaqueCache;
    private Color backgroundCache;
    private Cursor cursorCache;
    private boolean updateBackground;

    private static int globalDragCount = 0;

//	public static void main( String[] args ) {
//		java.awt.EventQueue.invokeLater( new Runnable() {
//			public void run() {
//				EyeDropper ed = new EyeDropper();
//				ed.addPropertyChangeListener( DROPPER_COLOR_CHANGED, new PropertyChangeListener() {
//					public void propertyChange( PropertyChangeEvent evt ) {
//						System.err.println( evt.getNewValue() );
//					}
//				});
//				javax.swing.JFrame f = new javax.swing.JFrame( "Eye Dropper Test" );
//				f.add( ed );
//				f.pack();
//				f.setDefaultCloseOperation( javax.swing.JFrame.EXIT_ON_CLOSE );
//				f.setVisible( true );
//			}
//		});
//	}
}
