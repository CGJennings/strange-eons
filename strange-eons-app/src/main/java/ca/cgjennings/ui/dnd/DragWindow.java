package ca.cgjennings.ui.dnd;

import ca.cgjennings.platform.PlatformSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;

/**
 * The UI widget used to display a {@link DragToken}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class DragWindow extends JWindow {

    private final JLabel label;
    private final int xOff;
    private int yOff;

    private static GraphicsConfiguration gc(JComponent source) {
        return source == null ? GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
                : source.getGraphicsConfiguration();
    }

    public DragWindow(JComponent source, DragToken<?> token) {
        super(gc(source));
        BufferedImage bi = token.getImage();
        if (bi.getTransparency() != Transparency.OPAQUE && gc(source).isTranslucencyCapable()) {
            setBackground(new Color(255, 255, 255, 0));
        } else {
            setBackground(Color.WHITE);
        }
        label = new JLabel(new ImageIcon(bi));
        setLayout(new BorderLayout());
        add(label, BorderLayout.CENTER);

        setFocusableWindowState(false);
        if (getGraphicsConfiguration().isTranslucencyCapable()) {
            setOpacity(0.8f);
        }
        if (!PlatformSupport.PLATFORM_IS_MAC) {
            // prevents RELEASED event on OS X if always on top
            setAlwaysOnTop(true);
        }
        xOff = -token.getHandleOffsetX();
        yOff = -token.getHandleOffsetY();
        pack();
    }

    /**
     * Updates the cursor to reflect whether the token is currently droppable.
     * The force parameter can be used to force the change, as when one of the
     * drop cursors is being updated.
     */
    public void setDroppable(boolean canDrop, Cursor drop, Cursor noDrop, boolean force) {
        if (!force && (droppable == canDrop)) {
            return;
        }

        droppable = canDrop;
        Cursor c = canDrop ? drop : noDrop;
        setCursor(c);
        getRootPane().setCursor(c);
        label.setCursor(c);
    }
    private boolean droppable = true;

    public boolean isDroppable() {
        return droppable;
    }

    public void updateLocation(Point cursorLocation) {
        setLocation(cursorLocation.x + xOff, cursorLocation.y + yOff);
    }
}
