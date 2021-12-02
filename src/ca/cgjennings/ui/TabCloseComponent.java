package ca.cgjennings.ui;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import resources.ResourceKit;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class TabCloseComponent extends JPanel {

    private final JCloseableTabbedPane pane;

    public TabCloseComponent(final JCloseableTabbedPane pane) {
        super(new FlowLayout(FlowLayout.LEADING, 0, 0));
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;
        setOpaque(false);

        label = new JLabel() {
            @Override
            public String getText() {
                int i = pane.indexOfTabComponent(TabCloseComponent.this);
                if (i != -1) {
                    return pane.getTitleAt(i);
                }
                return null;
            }

            @Override
            public Icon getIcon() {
                int i = pane.indexOfTabComponent(TabCloseComponent.this);
                if (i != -1) {
                    return pane.getIconAt(i);
                }
                return null;
            }
        };

        // handle selection when clicking on label; forward mouse event's
        // to tab pane's handler so that tabs can be dragged by their label
        label.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                e = prepareForParent(e);
                if (e != null) {
                    pane.mouseHandler.mousePressed(e);
                }
                int i = pane.indexOfTabComponent(TabCloseComponent.this);
                if (i != -1) {
                    pane.setSelectedIndex(i);
                    if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                        pane.editTitle(i);
                    }
                    return;
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                e = prepareForParent(e);
                if (e != null) {
                    pane.mouseHandler.mouseClicked(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                e = prepareForParent(e);
                if (e != null) {
                    pane.mouseHandler.mouseReleased(e);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                e = prepareForParent(e);
                if (e != null) {
                    pane.mouseHandler.mouseEntered(e);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                e = prepareForParent(e);
                if (e != null) {
                    pane.mouseHandler.mouseExited(e);
                }
            }
        });

        label.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                e = prepareForParent(e);
                if (e != null) {
                    pane.mouseHandler.mouseDragged(e);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                e = prepareForParent(e);
                if (e != null) {
                    pane.mouseHandler.mouseMoved(e);
                }
            }
        });
        ////////////////////////

        label.setFont(pane.getFont());
        label.setBorder(labelBorder);
        add(label);

        closeButton = new TabButton();
        add(closeButton);

        setBorder(buttonBorder);
    }

    protected MouseEvent prepareForParent(MouseEvent e) {
        int xs = e.getXOnScreen();
        int ys = e.getYOnScreen();

        Point paneLoc = pane.getLocationOnScreen();
        if (paneLoc == null) {
            return null;
        }

        int cx = xs - paneLoc.x;
        int cy = ys - paneLoc.y;

        // create a copy of this event that translates the X and Y
        // relative to the parent tab
        MouseEvent relativeToParent = new MouseEvent(
                e.getComponent(), e.getID(), e.getWhen(),
                e.getModifiersEx(), cx, cy,
                xs, ys, e.getClickCount(),
                e.isPopupTrigger(), e.getButton()
        );
        return relativeToParent;
    }

    private static final Border labelBorder = BorderFactory.createEmptyBorder(0, 0, 0, 5);
    private static final Border buttonBorder = BorderFactory.createEmptyBorder(2, 0, 0, 0);

    private TabButton closeButton;
    private JLabel label;

    public boolean isDirty() {
        return closeButton.isDirty();
    }

    public void setDirty(boolean dirty) {
        closeButton.setDirty(dirty);
    }

    @Override
    public void setFont(Font f) {
        if (label != null && f != null) {
            label.setFont(f);
        }
    }

    @Override
    public JPopupMenu getComponentPopupMenu() {
        return pane.getComponentPopupMenu();
    }

    @Override
    public String getToolTipText() {
        int i = pane.indexOfTabComponent(TabCloseComponent.this);
        if (i == -1) {
            return null;
        }
        return pane.getToolTipTextAt(i);
    }

    private static final int ICON_COUNT = 6;
    private static final Icon[] buttonIcons = new Icon[ICON_COUNT];

    static {
        final boolean dm = UIManager.getBoolean("useDarkTheme");
        for (int i = 0; i < ICON_COUNT; ++i) {
            BufferedImage im = ResourceKit.getImage("icons/ui/controls/close" + i + ".png");
            if (dm) {
                im = ImageUtilities.invert(im);
            }
            buttonIcons[i] = new ImageIcon(im);
        }

    }

    class TabButton extends JButton implements ActionListener {

        public TabButton() {
            setPreferredSize(new Dimension(buttonIcons[0].getIconWidth() + 1, buttonIcons[0].getIconWidth() + 1));
            // Make it look the same under all LaFs
            setUI(new BasicButtonUI());
            // Make transparent
            setContentAreaFilled(false);
            setFocusable(false);
            setBorderPainted(false);
            // Rollover effect
            addMouseListener(BUTTON_MOUSE_LISTENER);
            setRolloverEnabled(true);
            setFocusable(false);

            //Close the proper tab by clicking the button (remove this line
            //and the associated listener code to have a base class for
            //close buttons)
            addActionListener(this);
        }

        @Override
        public void updateUI() {
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int icon = getModel().isArmed() ? 2 : isRolledOver ? 1 : 0;
            if (dirty) {
                icon += 3;
            }
            buttonIcons[icon].paintIcon(this, g, 1, 1);
        }

        boolean isRolledOver = false;
        private boolean dirty = false;

        public boolean isDirty() {
            return dirty;
        }

        public void setDirty(boolean dirty) {
            if (this.dirty != dirty) {
                this.dirty = dirty;
                repaint();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = pane.indexOfTabComponent(TabCloseComponent.this);
            if (i == -1) {
                return;
            }
            if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                pane.closeAllBut(-1);
            } else if ((e.getModifiers() & (ActionEvent.ALT_MASK | ActionEvent.META_MASK)) != 0) {
                pane.closeAllBut(i);
            } else {
                pane.fireTabClosing(i, isDirty());
            }
        }
    } // End of TabButton

    private static final MouseListener BUTTON_MOUSE_LISTENER = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof TabButton) {
                TabButton button = (TabButton) component;
                button.isRolledOver = true;
                button.repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof TabButton) {
                TabButton button = (TabButton) component;
                button.isRolledOver = false;
                button.repaint();
            }
        }
    };
}
