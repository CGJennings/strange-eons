package ca.cgjennings.apps.arkham;

import ca.cgjennings.ui.StyleUtilities;
import ca.cgjennings.ui.anim.AnimationUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * A floating tool window, typically used to display special developer tool
 * windows. The content area of the window can be used in one of two ways:
 * either by adding a single child component using {@link #setBody} (this
 * component may have children of its own) or by obtaining the panel that
 * normally contains this child with {@link #getBodyPanel()}. If the second
 * method is used, then {@code setBody} and {@code getBody} must not be called.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ToolWindow extends javax.swing.JDialog {

    private static final float NO_FOCUS_OPACITY = 0.667f;
    private static final float FOCUS_OPACITY = 1f;
    private static final float ANIM_TIME = 0.33f;

    /**
     * Creates a new tool window.
     */
    public ToolWindow() {
        this(StrangeEons.getWindow(), null, ModalityType.MODELESS);
    }

    public ToolWindow(Window parent, boolean modal) {
        this(parent, null, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
    }

    public ToolWindow(Window parent, String title, ModalityType modality) {
        super(parent, modality);
        setUndecorated(true);
        getRootPane().setDoubleBuffered(true);
        super.setResizable(true);

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                AnimationUtilities.animateOpacityTransition(ToolWindow.this, NO_FOCUS_OPACITY, FOCUS_OPACITY, ANIM_TIME, false);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                AnimationUtilities.animateOpacityTransition(ToolWindow.this, FOCUS_OPACITY, NO_FOCUS_OPACITY, ANIM_TIME, false);
            }
        });

        initComponents();

        setTitle(title);
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        titleLabel.setText((title == null || title.isEmpty()) ? " " : title);
    }

    public void setIcon(Icon icon) {
        titleLabel.setIcon(icon);
    }

    public Icon getIcon() {
        return titleLabel.getIcon();
    }

    @Override
    public void setResizable(boolean resizable) {
        if (isResizable() != resizable) {
            super.setResizable(true);
            repaint(getWidth() - RESIZE_CORNER_SIZE, getHeight() - RESIZE_CORNER_SIZE, RESIZE_CORNER_SIZE, RESIZE_CORNER_SIZE);
        }
    }

    public JPanel getBodyPanel() {
        return bodyContainer;
    }

    public void setBody(JComponent c) {
        bodyContainer.removeAll();
        bodyContainer.add(c, BorderLayout.CENTER);
        validate();
    }

    public JComponent getBody() {
        if (bodyContainer.getComponentCount() == 0) {
            return null;
        }
        return (JComponent) bodyContainer.getComponent(0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        decorationPanel = new javax.swing.JPanel();
        bodyContainer = new javax.swing.JPanel();
        titlePanel = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();
        closeBtn = new ca.cgjennings.apps.arkham.ToolCloseButton();

        setTitle(" ");

        decorationPanel.setBorder( cornerBorder );
        decorationPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                decorationPanelMouseExited(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                decorationPanelMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                decorationPanelMouseReleased(evt);
            }
        });
        decorationPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                decorationPanelMouseDragged(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                decorationPanelMouseMoved(evt);
            }
        });
        decorationPanel.setLayout(new java.awt.BorderLayout());

        bodyContainer.setBackground(java.awt.Color.white);
        bodyContainer.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.darkGray));
        bodyContainer.setLayout(new java.awt.BorderLayout());
        decorationPanel.add(bodyContainer, java.awt.BorderLayout.CENTER);

        titlePanel.setBackground(java.awt.Color.black);
        titlePanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(0, 0, 0)));
        titlePanel.setLayout(new java.awt.GridBagLayout());

        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | java.awt.Font.BOLD));
        titleLabel.setForeground(java.awt.Color.white);
        titleLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                titleLabelMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                titleLabelMouseReleased(evt);
            }
        });
        titleLabel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                titleLabelMouseDragged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 4, 1, 4);
        titlePanel.add(titleLabel, gridBagConstraints);

        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 2, 4);
        titlePanel.add(closeBtn, gridBagConstraints);

        decorationPanel.add(titlePanel, java.awt.BorderLayout.NORTH);

        getContentPane().add(decorationPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void closeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeBtnActionPerformed
            windowClosing();
	}//GEN-LAST:event_closeBtnActionPerformed

	private void titleLabelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_titleLabelMouseDragged
            if (SwingUtilities.isLeftMouseButton(evt) && titleDrag) {
                int newX = evt.getXOnScreen();                
                int newY = evt.getYOnScreen();
                setLocation(newX - dragStartXInWindow, newY - dragStartYInWindow);
            }
	}//GEN-LAST:event_titleLabelMouseDragged

	private void titleLabelMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_titleLabelMousePressed
            if (SwingUtilities.isLeftMouseButton(evt)) {
                titleDrag = true;
                dragStartXInWindow = evt.getX();
                dragStartYInWindow = evt.getY();
                titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
	}//GEN-LAST:event_titleLabelMousePressed

	private void titleLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_titleLabelMouseReleased
            if (titleDrag) {
                titleDrag = false;
                titleLabel.setCursor(Cursor.getDefaultCursor());
            }
	}//GEN-LAST:event_titleLabelMouseReleased

	private void decorationPanelMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_decorationPanelMousePressed
            if (SwingUtilities.isLeftMouseButton(evt)) {
                resizeCorner = getResizeCorner(evt.getX(), evt.getY());
                dragStartXInWindow = evt.getXOnScreen();
                dragStartYInWindow = evt.getYOnScreen();
            }
	}//GEN-LAST:event_decorationPanelMousePressed

	private void decorationPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_decorationPanelMouseReleased
            if (resizeCorner >= 0) {
                resizeCorner = -1;
                setCursor(Cursor.getDefaultCursor());
            }
	}//GEN-LAST:event_decorationPanelMouseReleased

	private void decorationPanelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_decorationPanelMouseDragged
            if (SwingUtilities.isLeftMouseButton(evt) && resizeCorner >= 0) {
                // get current window bounds
                int x1 = getX();
                int y1 = getY();
                int oldWidth = getWidth();
                int oldHeight = getHeight();
                int x2 = x1 + oldWidth;
                int y2 = y1 + oldHeight;

                // determine how cursor has been dragged
                int newX = evt.getXOnScreen();
                int newY = evt.getYOnScreen();
                int dx = newX - dragStartXInWindow;
                int dy = newY - dragStartYInWindow;

                if ((resizeCorner & RC_N) != 0) {
                    dx = 0;
                }
                if ((resizeCorner & RC_E) != 0) {
                    dy = 0;
                }

                // adjust boundary points by drag type
                if ((resizeCorner & 1) == 0) {
                    if ((x2 - x1) + dx < MINIMUM_SIZE) {
                        dx = MINIMUM_SIZE - (x2 - x1);
                    }
                    x2 += dx;
                    dragStartXInWindow += (x2 - x1) - oldWidth;
                } else {
                    if ((x2 - x1) - dx < MINIMUM_SIZE) {
                        dx = (x2 - x1) - MINIMUM_SIZE;
                    }
                    x1 += dx;
                    dragStartXInWindow += oldWidth - (x2 - x1);
                }
                if ((resizeCorner & 2) == 0) {
                    if ((y2 - y1) + dy < MINIMUM_SIZE) {
                        dy = MINIMUM_SIZE - (y2 - y1);
                    }
                    y2 += dy;
                    dragStartYInWindow += (y2 - y1) - oldHeight;
                } else {
                    if ((y2 - y1) - dy < MINIMUM_SIZE) {
                        dy = (y2 - y1) - MINIMUM_SIZE;
                    }
                    y1 += dy;
                    dragStartYInWindow += oldHeight - (y2 - y1);
                }

                setBounds(x1, y1, x2 - x1, y2 - y1);
            }
	}//GEN-LAST:event_decorationPanelMouseDragged

    private static final int MINIMUM_SIZE = 64;

	private void decorationPanelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_decorationPanelMouseMoved
            int corner = getResizeCorner(evt.getX(), evt.getY());
            Cursor c = getResizeCursor(corner);
            if (!c.equals(getCursor())) {
                setCursor(c);
            }
	}//GEN-LAST:event_decorationPanelMouseMoved

	private void decorationPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_decorationPanelMouseExited
            // make sure resize cursor not shown, since the cursor on this
            // panel will be the default for subpanels
            if (resizeCorner < 0 && !Cursor.getDefaultCursor().equals(getCursor())) {
                setCursor(Cursor.getDefaultCursor());
            }
	}//GEN-LAST:event_decorationPanelMouseExited

    private int getResizeCorner(int x, int y) {
        int xbits = -1;
        if (x <= RESIZE_CORNER_SIZE) {
            xbits = RC_SW;
        } else if (x >= getWidth() - RESIZE_CORNER_SIZE) {
            xbits = RC_SE;
        }

        int ybits = -1;
        if (y <= RESIZE_CORNER_SIZE) {
            ybits = RC_NE;
        } else if (y >= getHeight() - RESIZE_CORNER_SIZE) {
            ybits = RC_SE;
        }

        if (xbits < 0 && ybits < 0) {
            return -1;
        }

        if (xbits < 0) {
            xbits = RC_N | ybits;
        } else if (ybits < 0) {
            xbits |= RC_E;
        } else {
            xbits |= ybits;
        }
        return xbits;
    }

    private static final int RC_SE = 0;
    private static final int RC_SW = 1;

    private static final int RC_NE = 2;
    private static final int RC_NW = 3;

    private static final int RC_E = 4;
    private static final int RC_W = 5;

    private static final int RC_N = 8;
    private static final int RC_S = 10;

    private Cursor getResizeCursor(int corner) {
        int c = Cursor.DEFAULT_CURSOR;
        switch (corner) {
            case RC_SE:
                c = Cursor.SE_RESIZE_CURSOR;
                break;
            case RC_SW:
                c = Cursor.SW_RESIZE_CURSOR;
                break;
            case RC_NE:
                c = Cursor.NE_RESIZE_CURSOR;
                break;
            case RC_NW:
                c = Cursor.NW_RESIZE_CURSOR;
                break;
            case RC_N:
                c = Cursor.N_RESIZE_CURSOR;
                break;
            case RC_S:
                c = Cursor.S_RESIZE_CURSOR;
                break;
            case RC_E:
                c = Cursor.E_RESIZE_CURSOR;
                break;
            case RC_W:
                c = Cursor.W_RESIZE_CURSOR;
                break;
        }
        return Cursor.getPredefinedCursor(c);
    }

    private static final int RESIZE_CORNER_SIZE = 12;

    private int dragStartXInWindow, dragStartYInWindow;
    private boolean titleDrag;
    private int resizeCorner = -1;

    protected void windowClosing() {
        AnimationUtilities.animateOpacityTransition(this, FOCUS_OPACITY, 0f, ANIM_TIME / 2f, true);
    }

    /**
     * Closes the window. Subclasses may change the default close behaviour by
     * overriding {@link #windowClosing()}.
     */
    public final void close() {
        windowClosing();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && visible != isVisible()) {
            StyleUtilities.setWindowOpacity(this, NO_FOCUS_OPACITY);
        }
        super.setVisible(visible);
    }

//    /**
//    * @param args the command line arguments
//    */
//    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//			@Override
//            public void run() {
//                ToolWindow twin = new ToolWindow();
//				twin.setSize( 200, 200 );
////				twin.addTitleButton( createTitleButton( "X", null, null, null) );
//				twin.setVisible(true);
//				twin.setLocationRelativeTo(null);
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bodyContainer;
    private ca.cgjennings.apps.arkham.ToolCloseButton closeBtn;
    private javax.swing.JPanel decorationPanel;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JPanel titlePanel;
    // End of variables declaration//GEN-END:variables

    private static final int BORDER_THICKNESS = 3;
    private final Border cornerBorder = new LineBorder(Color.BLACK, BORDER_THICKNESS) {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            super.paintBorder(c, g, x, y, width, height);

            if (isResizable()) {
                g.setColor(Color.DARK_GRAY);

                final int DEPTH = BORDER_THICKNESS - 1;
                final int SIZE = RESIZE_CORNER_SIZE + 1;

                final int x2 = x + width - 1;
                final int y2 = y + height - 1;

                g.drawLine(x2 - SIZE, y2, x2 - SIZE, y2 - DEPTH);
//				g.drawLine( x2-SIZE, y2-DEPTH, x2-DEPTH, y2-DEPTH );
//				g.drawLine( x2-DEPTH, y2-DEPTH, x2-DEPTH, y2-SIZE );
                g.drawLine(x2 - DEPTH, y2 - SIZE, x2, y2 - SIZE);

                g.drawRect(x, y, width - 1, height - 1);
            }
        }
    };

    /**
     * Creates a button that can be added to the title bar.
     *
     * @return a title bar button
     */
    public static JButton createTitleButton(String text, Icon icon, Icon rolloverIcon, Icon selectedIcon) {
        JButton tb = new JButton(text, icon);
        tb.setContentAreaFilled(false);
        tb.setFocusPainted(false);
        tb.setBorderPainted(true);
        tb.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY),
                        BorderFactory.createMatteBorder(0, 4, 0, 4, Color.BLACK)
                )
        );
        tb.setMargin(tb.getText() == null || tb.getText().isEmpty() ? new Insets(3, 4, 2, 4) : new Insets(1, 4, 1, 4));
        tb.setBackground(Color.BLACK);
        tb.setForeground(Color.WHITE);
        tb.setOpaque(true);
        Font f = tb.getFont();
        tb.setFont(f.deriveFont(f.getSize2D() - 1f));
        tb.setSelectedIcon(selectedIcon);
        if (rolloverIcon != null) {
            tb.setRolloverEnabled(true);
            tb.setRolloverIcon(rolloverIcon);
        }
        return tb;
    }

    public void addTitleButton(JButton tb) {
        GridBagLayout gb = (GridBagLayout) titlePanel.getLayout();
        GridBagConstraints gbc = new GridBagConstraints(
                GridBagConstraints.RELATIVE, 0,
                1, 1, 0d, 1d, GridBagConstraints.CENTER,
                GridBagConstraints.VERTICAL,
                new Insets(0, 0, 0, 0),
                0, 0
        );
        GridBagConstraints tbc = gb.getConstraints(titleLabel);
        GridBagConstraints cbc = gb.getConstraints(closeBtn);
        titlePanel.remove(titleLabel);
        titlePanel.remove(closeBtn);
        titlePanel.add(tb, gbc);
        titlePanel.add(titleLabel, tbc);
        titlePanel.add(closeBtn, cbc);
    }

    public void removeTitleButton(JButton tb) {
        titlePanel.remove(tb);
    }
}
