package ca.cgjennings.ui;

import ca.cgjennings.graphics.shapes.ShapeUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.Painter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * A {@link TreeLabelExposer} causes a {@link JTree} to pop up a small component
 * to display the full label text when the mouse hovers over nodes whose label
 * doesn't fit in the tree component. To use it, simply create a new instance
 * attached to the tree you wish to modify.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TreeLabelExposer {

    private JTree tree;
    private Component glassPane;
    private TreePath path;
    private int row;
    private Rectangle rowBounds;
    private Rectangle treeBounds;
    private MouseInputAdapter mouseListener = new MouseInputAdapter() {
        @Override
        public void mouseExited(MouseEvent e) {
            resetGlassPane();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            exposePath(tree.getPathForLocation(e.getX(), e.getY()));
        }
    };
    private TreeSelectionListener selectionListener = new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            exposePath(path); // redraw row when selection state changes
        }
    };
    private JComponent helper = new JComponent() {
        @Override
        public void paint(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            boolean selected = tree.isRowSelected(row);
            TreePath sel = tree.getLeadSelectionPath();
            boolean focused = sel == null ? false : tree.getRowForPath(sel) == row && tree.isFocusOwner();

            helper.setFont(tree.getFont());
            JComponent renderer = (JComponent) tree.getCellRenderer().getTreeCellRendererComponent(
                    tree, path.getLastPathComponent(),
                    selected, tree.isExpanded(row),
                    tree.getModel().isLeaf(path.getLastPathComponent()),
                    row, focused
            );
            Rectangle paintBounds = SwingUtilities.convertRectangle(tree, rowBounds, this);
            Rectangle treePaintBounds = SwingUtilities.convertRectangle(tree, treeBounds, this);
            renderer.setBounds(paintBounds);
            renderer.doLayout();

            Rectangle superClip = new Rectangle();
            Rectangle.union(paintBounds, treePaintBounds, superClip);
            superClip.grow(2, 2);
            g.setClip(ShapeUtilities.subtract(superClip, treePaintBounds));

            boolean bgDrawn = false;
            String painterKey = null;
            if (selected) {
                if (focused) {
                    painterKey = "Focused+Selected";
                } else {
                    painterKey = "Enabled+Selected";
                }
            } else {
                if (focused) {
                    painterKey = "Enabled+Focused";
                }
            }
            if (painterKey != null) {
                Object painter = UIManager.get("Tree:TreeCell[" + painterKey + "].backgroundPainter");
                if (painter instanceof Painter) {
                    Graphics2D clone = (Graphics2D) g.create();
                    try {
                        clone.translate(paintBounds.x, paintBounds.y);
                        ((Painter) painter).paint(clone, renderer, renderer.getWidth(), renderer.getHeight());
                    } finally {
                        clone.dispose();
                    }
                    bgDrawn = true;
                }
            }

            if (!bgDrawn) {
                Color bg;
                if (selected) {
                    bg = UIManager.getColor("Tree.selectionBackground");
                    if (bg == null) {
                        bg = renderer.getBackground();
                    }
                } else {
                    bg = tree.getBackground();
                }
                g.setColor(bg);
                g.fill(paintBounds);
            }

            boolean wasOpaque = renderer.isOpaque();
            renderer.setOpaque(false);
            SwingUtilities.paintComponent(g, renderer, this, paintBounds);
            renderer.setOpaque(wasOpaque);

            Color lineColor = selected ? UIManager.getColor("controlLHighlight") : UIManager.getColor("nimbusBorder");
            if (lineColor == null) {
                lineColor = selected ? tree.getBackground() : tree.getForeground();
            }

            g.setColor(lineColor);
            g.drawRect(paintBounds.x, paintBounds.y - 1, paintBounds.width, paintBounds.height + 1);
        }
    };

    /**
     * Creates a new tree label exposer and installs it on the specified tree.
     *
     * @param tree the tree to create exposed labels for
     */
    public TreeLabelExposer(JTree tree) {
        this.tree = tree;
        tree.addMouseListener(mouseListener);
        tree.addMouseMotionListener(mouseListener);
        tree.addTreeSelectionListener(selectionListener);
    }

    private void resetGlassPane() {
        if (glassPane != null) {
            helper.setVisible(false);
            tree.getRootPane().setGlassPane(glassPane);
            glassPane = null;
            path = null;
            rowBounds = null;
            treeBounds = null;
        }
    }

    private void exposePath(TreePath path) {
        this.path = path;
        if (path != null && tree.isEnabled()) {
            row = tree.getRowForPath(path);
            treeBounds = tree.getVisibleRect();
            rowBounds = tree.getPathBounds(path);
            if (rowBounds != null && !treeBounds.contains(rowBounds)) {
                if (glassPane == null) {
                    glassPane = tree.getRootPane().getGlassPane();
                    helper.setOpaque(false);
                    tree.getRootPane().setGlassPane(helper);
                    helper.setVisible(true);
                } else {
                    tree.getRootPane().repaint();
                }
            } else {
                resetGlassPane();
            }
        } else {
            resetGlassPane();
        }
    }
}
