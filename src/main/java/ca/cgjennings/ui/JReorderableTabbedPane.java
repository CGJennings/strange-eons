package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.dnd.DragSource;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JWindow;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.TabbedPaneUI;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JReorderableTabbedPane extends JTabbedPane {

    public JReorderableTabbedPane() {
        this(TOP);
    }

    public JReorderableTabbedPane(int tabPlacement) {
        this(tabPlacement, WRAP_TAB_LAYOUT);
    }

    public JReorderableTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);

        mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    protected MouseInputListener mouseHandler;

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            Point p = e.getPoint();
            // && fixed NPE in JDK7b108
            if (contains(p) && getTabIndex(p.x, p.y) >= 0) {
                return;
            }
        }
        super.processMouseEvent(e);
    }

    private void dragTab(int dragIndex, int tabIndex) {
        if (!canReorderTo(dragIndex, tabIndex)) {
            return;
        }

        String title = getTitleAt(dragIndex);
        Icon icon = getIconAt(dragIndex);
        Component component = getComponentAt(dragIndex);
        String toolTipText = getToolTipTextAt(dragIndex);

        Color background = getBackgroundAt(dragIndex);
        Color foreground = getForegroundAt(dragIndex);
        Icon disabledIcon = getDisabledIconAt(dragIndex);
        int mnemonic = getMnemonicAt(dragIndex);
        int displayedMnemonicIndex = getDisplayedMnemonicIndexAt(dragIndex);
        boolean enabled = isEnabledAt(dragIndex);

        Component oldTabComp = getTabComponentAt(dragIndex);
        // inserting then removing would make it easier for subclassers who want to add
        // a last tab that inserts a "new page" tab when clicked (prevents accidental activation)
        remove(dragIndex);
        insertTab(title, icon, component, toolTipText, tabIndex);
        if (oldTabComp != null) {
            setTabComponentAt(tabIndex, oldTabComp);
        }

        setBackgroundAt(tabIndex, background);
        setForegroundAt(tabIndex, foreground);
        setDisabledIconAt(tabIndex, disabledIcon);
        setMnemonicAt(tabIndex, mnemonic);
        setDisplayedMnemonicIndexAt(tabIndex, displayedMnemonicIndex);
        setEnabledAt(tabIndex, enabled);

        for (TabbedPaneReorderListener l : listeners) {
            l.tabbedPanesReordered(this, dragIndex, tabIndex);
        }
    }

    protected Cursor getDefaultCursor() {
        return Cursor.getDefaultCursor();
    }

    protected Cursor getDragNoDropCursor() {
        return DragSource.DefaultMoveNoDrop;
    }

    protected Cursor getDragCursor() {
        return DragSource.DefaultMoveDrop.equals(Cursor.getDefaultCursor())
                ? Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
                : DragSource.DefaultMoveDrop;
    }

    protected final int getTabIndex(int x, int y) {
        return getUI().tabForCoordinate(this, x, y);
    }

    protected Cursor getDragOutCursor() {
        return Cursor.getDefaultCursor();
    }

    private void maybeSetDragOutCursor() {
        Cursor cursor = getDragOutCursor();

        if (getCursor() != cursor) {
            setCursor(cursor);
            if (dragImage != null) {
                dragImage.setCursor(cursor);
            }
        }
    }

    private void maybeSetNoDropCursor() {
        Cursor cursor = getDragNoDropCursor();

        if (getCursor() != cursor) {
            setCursor(cursor);
            if (dragImage != null) {
                dragImage.setCursor(cursor);
            }
        }
    }
    private HashSet<TabbedPaneReorderListener> listeners = new HashSet<>();

    public void addTabbedPaneReorderListener(TabbedPaneReorderListener listener) {
        listeners.add(listener);
    }

    public void removeTabbedPaneReorderListener(TabbedPaneReorderListener listener) {
        listeners.remove(listener);
    }

    /**
     * Override this method to control which tabs can be reordered. It will be
     * called when the tab is "grabbed" with the mouse pointer.
     *
     * @param index the index of the tab being tested
     * @return {@code true} if moving the tab to a new position is allowed
     */
    protected boolean isTabReorderable(int index) {
        return true;
    }

    protected boolean canReorderTo(int oldIndex, int newIndex) {
        return true;
    }

    private void maybeSetDropCursor() {
        Cursor cursor = getDragCursor();

        if (getCursor() != cursor) {
            setCursor(cursor);
            if (dragImage != null) {
                dragImage.setCursor(cursor);
            }
        }
    }

    private JTabbedPane getPane() {
        return this;
    }

    @Override
    public void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        super.paintComponent(g);
        paintInsertionMark(g);
    }

    public void paintInsertionMark(Graphics2D g) {
        if (paintTriangle) {
            g.setClip(null);
            Paint p = g.getPaint();
//			Object hint = g.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
//			g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            g.setPaint(Color.WHITE);
            g.fillPolygon(xPoint, yPoint, 3);
            g.setPaint(Color.BLACK);
            g.drawPolygon(xPoint, yPoint, 3);
//			g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, hint );
            g.setPaint(p);
        }
    }
    private int[] xPoint = new int[3];
    private int[] yPoint = new int[3];
    private boolean paintTriangle = false;

    protected Component getDragImageRepresentativeComponent(int dragIndex) {
        return getComponentAt(dragIndex);
    }

    protected JWindow createDragImage(int dragIndex) {
        Component c = getDragImageRepresentativeComponent(dragIndex);
        if (c == null) {
            return null;
        }

        Dimension size = c.getSize();
        if (size.width == 0 || size.height == 0) {
            return null;
        }

        BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            if (!c.isOpaque()) {
                g.setPaint(getBackground());
                g.fillRect(0, 0, size.width, size.height);
            }
            c.printAll(g);
        } finally {
            g.dispose();
        }
        if (size.width > MAX_IMAGE_SIZE || size.height > MAX_IMAGE_SIZE) {
            if (size.width > size.height) {
                size.height = (int) (MAX_IMAGE_SIZE / (float) size.width * size.height);
                size.width = MAX_IMAGE_SIZE;
            } else {
                size.width = (int) (MAX_IMAGE_SIZE / (float) size.height * size.width);
                size.height = MAX_IMAGE_SIZE;
            }
        }

        while (img.getWidth() > size.width * 2 && img.getHeight() > size.height * 2) {
            BufferedImage temp = new BufferedImage(img.getWidth() / 2, img.getHeight() / 2, BufferedImage.TYPE_INT_RGB);
            g = temp.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(img, 0, 0, temp.getWidth(), temp.getHeight(), null);
            } finally {
                g.dispose();
            }
            img = temp;
        }

        BufferedImage icon = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        g = icon.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, size.width, size.height, null);
            g.setPaint(Color.DARK_GRAY);
            g.drawRect(0, 0, size.width - 1, size.height - 1);
        } finally {
            g.dispose();
        }

        Window owner = null;
        while (c != null) {
            if (c instanceof Window) {
                owner = (Window) c;
                break;
            }
            c = c.getParent();
        }
        JWindow w = new JWindow(owner);
        w.setFocusable(false);
        JLabel content = new JLabel(new ImageIcon(icon));
        content.setBorder(BorderFactory.createEtchedBorder());
        w.getContentPane().add(content);
        w.setCursor(getCursor());
        w.pack();
        return w;
    }
    private static final int MAX_IMAGE_SIZE = 384;

    protected boolean isDragOutSupported() {
        return false;
    }

    protected void handleDragOut(int tabIndex, Point locationOnScreen) {
    }
    private boolean doingDragOut = false;
    private Window dragImage = null;

    class MouseHandler extends MouseInputAdapter {

        @Override
        public void mouseDragged(MouseEvent e) {
            if (dragIndex != -1) {
                int tabToMark = getTabIndex(e.getX(), e.getY());

                if (dragImage == null) {
                    dragImage = createDragImage(dragIndex);
                }
                if (dragImage != null) {
                    dragImage.setLocation(e.getXOnScreen(), e.getYOnScreen());
                    dragImage.setVisible(true);
                }

                doingDragOut = false;
                if (tabToMark != -1) {
                    setSelectedIndex(tabToMark);
                    maybeSetDropCursor();
                } else {
                    if (isDragOutSupported()) {
                        Rectangle tabBounds = getBoundsAt(dragIndex);
                        if (tabBounds != null && (e.getY() < 0 || e.getY() >= tabBounds.getHeight())) {
                            maybeSetDragOutCursor();
                            doingDragOut = true;
                        } else {
                            maybeSetNoDropCursor();
                        }
                    } else {
                        maybeSetNoDropCursor();
                    }
                }

                if (lastMarked != tabToMark) {
                    clearMark(lastMarked);
                    drawMark(tabToMark);
                }

                lastMarked = tabToMark;

                if (dragIndex != -1) {
                    if (canReorderTo(dragIndex, tabToMark)) {
                        Graphics2D g = (Graphics2D) getGraphics();
                        if (g != null) {
                            paintInsertionMark(g);
                        }
                    } else {
                        maybeSetNoDropCursor();
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON1) {
                int tabIndex = getTabIndex(e.getX(), e.getY());
                if (tabIndex != -1 && isTabReorderable(tabIndex)) {
                    dragIndex = tabIndex;
                    maybeSetDropCursor();
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON1) {
                if (dragImage != null) {
                    dragImage.dispose();
                    dragImage = null;
                }
                if (doingDragOut && isDragOutSupported()) {
                    handleDragOut(dragIndex, e.getLocationOnScreen());
                }
                if (dragIndex != -1) {
                    setSelectedIndex(dragIndex);
                    int tabIndex = getTabIndex(e.getX(), e.getY());
                    if (reorderable && tabIndex != -1 && tabIndex != dragIndex) {
                        dragTab(dragIndex, tabIndex);
                        setSelectedIndex(tabIndex);
                    }
                    clearMark(lastMarked);
                    dragIndex = -1;
                    lastMarked = -1;
                }
            }
            doingDragOut = false;
            getPane().setCursor(getDefaultCursor());
        }
        private int dragIndex = -1;
        private int lastMarked = -1;
        private boolean reorderable = true;

        private static final int NUDGE_SIZE = 6;

        private void clearMark(int index) {
            if (index == -1) {
                return;
            }

            TabbedPaneUI ui = getUI();
            Rectangle rect = ui.getTabBounds(getPane(), index);

            rect.x -= NUDGE_SIZE;
            rect.width += NUDGE_SIZE * 2;
            paintTriangle = false;
            repaint(rect);
        }

        private void drawMark(int index) {
            if (index == -1) {
                paintTriangle = false;
                return;
            }

            TabbedPaneUI ui = getUI();
            Rectangle rect = ui.getTabBounds(getPane(), index);

            int nudge = (index == 0 || index == getPane().getTabCount() - 1) ? 0 : NUDGE_SIZE;

            if (dragIndex > index) {
                buildTriangle(rect.x + 6 - nudge, rect.y + 14);
            } else if (dragIndex < index) {
                buildTriangle(rect.x + rect.width - 7 + nudge, rect.y + 14);
            } else {
                buildTriangle(rect.x + rect.width / 2 - 3, rect.y + 14);
            }

            paintTriangle = true;
            rect.x -= (TRIANGLE_SIZE / 2);
            rect.width += (TRIANGLE_SIZE);
            repaint(rect);
        }

        private void buildTriangle(int cx, int by) {
            xPoint[0] = cx - (TRIANGLE_SIZE / 2);
            yPoint[0] = by - (TRIANGLE_SIZE);
            xPoint[1] = cx + (TRIANGLE_SIZE / 2);
            yPoint[1] = by - (TRIANGLE_SIZE);
            xPoint[2] = cx;
            yPoint[2] = by - 1;
        }
        private static final int TRIANGLE_SIZE = 10;

        /**
         * @return the reorderable
         */
        public boolean isReorderable() {
            return reorderable;
        }

        /**
         * @param reorderable the reorderable to set
         */
        public void setReorderable(boolean reorderable) {
            this.reorderable = reorderable;
        }
    }
}
