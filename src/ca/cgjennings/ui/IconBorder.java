package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.text.JTextComponent;

/**
 * Paints an icon on the left or right side of a component, depending on reading
 * direction. Intended to be used to add decorative icons to text fields.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class IconBorder extends AbstractBorder {

    private Icon icon, disabledIcon;
    private boolean rightSide;

    public IconBorder() {
    }

    public IconBorder(Icon icon) {
        setIcon(icon);
    }

    public IconBorder(URL icon) {
        setIcon(new ImageIcon(icon));
    }

    public IconBorder(Icon icon, boolean showOnRightSide) {
        setIcon(icon);
        this.rightSide = showOnRightSide;
    }

    public IconBorder(URL icon, boolean showOnRightSide) {
        setIcon(new ImageIcon(icon));
        this.rightSide = showOnRightSide;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(null, icon);
    }

    public boolean isIconOnRightSide() {
        return rightSide;
    }

    public void setIconOnRightSide(boolean rightSide) {
        this.rightSide = rightSide;
    }

    /**
     * Assuming that a component has this border installed on it, returns
     * {@code true} if a point in the component's coordinate space would be over
     * the icon.
     *
     * @param c the component to test
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return {@code true} if (x,y) is over the icon border
     */
    public boolean isPointOverIcon(JComponent c, int x, int y) {
        Insets i = c.getInsets();
        if (y >= i.top && y <= c.getHeight() - i.bottom) {
            if (rightSide) {
                return x >= c.getWidth() - i.right + MARGIN_INNER;
            } else {
                return x <= i.left - MARGIN_INNER;
            }
        }
        return false;
    }

    /**
     * Installs this border on a component by setting a new compound border on
     * the component with the existing border on the outside and this border on
     * the inside. Returns the new compound border.
     * <p>
     * If the component has no border, this border is simply set on the
     * component without creating a compound border.
     * <p>
     * If the component is an editable combo box, the icon will be installed on
     * the editor component if it is a {@code JComponent}.
     *
     * @param c the component to install the border on, typically a text field
     * @return the installed compound border
     */
    public Border install(JComponent c) {
        if (c instanceof JComboBox) {
            JComboBox cb = (JComboBox) c;
            if (cb.isEditable() && cb.getEditor().getEditorComponent() instanceof JComponent) {
                c = (JComponent) cb.getEditor().getEditorComponent();
            }
            noFillHint = true;
        }

        Border current = c.getBorder();
        if (current == null) {
            c.setBorder(this);
            return this;
        } else {
            CompoundBorder cb = new CompoundBorder(current, this);
            c.setBorder(cb);
            return cb;
        }
    }

    private boolean noFillHint;

    /**
     * Installs this border on a component and makes the icon border clickable.
     * This behaves exactly as for calling {@link #install}, except that the
     * border also responds to mouse events.
     *
     * @param c the component to install in
     * @param hoverIcon an optional alternate icon displayed when the pointer is
     * over the icon
     * @param li an optional listener that will be called when the icon is left
     * clicked
     * @param popupMenu an optional popup menu that will be displayed when the
     * icon is right clicked
     * @return the installed compound border
     */
    public Border installClickable(final JComponent c, final Icon hoverIcon, final ActionListener li, final JPopupMenu popupMenu) {
        Border b = install(c);

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearHover();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isPointOverIcon(c, e.getX(), e.getY())) {
                    if (tempCursor == null && c.isEnabled()) {
                        tempCursor = c.getCursor();
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        if (hoverIcon != null) {
                            tempIcon = icon;
                            icon = hoverIcon;
                            c.repaint();
                        }
                    }
                } else {
                    clearHover();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (tempCursor != null) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        if (li != null) {
                            li.actionPerformed(new ActionEvent(c, ActionEvent.ACTION_PERFORMED, "BORDER_CLICK", e.getModifiersEx()));
                        }
                    } else if (e.isPopupTrigger()) {
                        showPopup(e);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (tempCursor != null) {
                    if (e.isPopupTrigger()) {
                        showPopup(e);
                    }
                }
            }

            private void showPopup(MouseEvent e) {
                if (popupMenu == null) {
                    return;
                }
                Insets i = c.getInsets();
                int x;
                if (isIconOnRightSide()) {
                    x = i.left - popupMenu.getWidth();
                } else {
                    x = c.getWidth() - i.right;
                }
                int y = c.getHeight() - i.bottom;
                popupMenu.show(c, x, y);
            }

            private void clearHover() {
                if (tempCursor != null) {
                    c.setCursor(tempCursor);
                    tempCursor = null;
                    if (tempIcon != null) {
                        icon = tempIcon;
                        tempIcon = null;
                        c.repaint();
                    }
                }
            }
            private Cursor tempCursor;
            private Icon tempIcon;
        };

        c.addMouseMotionListener(mouse);
        c.addMouseListener(mouse);

        return b;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = 0;
        insets.bottom = 0;
        if (icon == null) {
            insets.left = 0;
            insets.right = 0;
        } else {
            final int width = MARGIN_TOTAL + icon.getIconWidth();
            if (rightSide) {
                insets.left = 0;
                insets.right = width;
            } else {
                insets.left = width;
                insets.right = 0;
            }
        }
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return !noFillHint;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        final boolean enabled = c.isEnabled();
        Icon i = enabled ? icon : (disabledIcon == null ? icon : disabledIcon);

        if (!noFillHint) {
            fillBorder(c, g, x, y, width, height, enabled);
        }
        
        if (i == null) {
            return;
        }

        if (rightSide) {
            int w = MARGIN_TOTAL + icon.getIconWidth();
            i.paintIcon(c, g, x + width - w + MARGIN_INNER, y + (height - icon.getIconHeight()) / 2);
        } else {
            i.paintIcon(c, g, x + MARGIN_OUTER, y + (height - icon.getIconHeight()) / 2);
        }
    }

    private void fillBorder(Component c, Graphics g, int x, int y, int width, int height, boolean enabled) {
        Color bkg = null;
        if (!enabled && (c instanceof JTextComponent)) {
            // a hack that gets the right background for the text area under Nimbus
            // this is needed since there is no getDisabledBackground()
            bkg = UIManager.getColor("TextField.disabled");
        }
        if (bkg == null) {
            bkg = c.getBackground();
        }
        g.setColor(bkg);
        if (rightSide) {
            int w = MARGIN_TOTAL + icon.getIconWidth();
            g.fillRect(x + width - w, y, w, height);
        } else {
            g.fillRect(x, y, MARGIN_TOTAL + icon.getIconWidth(), height);
        }
    }

    // Margins of space around the icon:
    //   inner is gap between icon and text
    //   outer is gap between icon and outer border
    private int MARGIN_OUTER = 2;
    private int MARGIN_INNER = 4;
    private int MARGIN_TOTAL = MARGIN_OUTER + MARGIN_INNER;

    public void setOuterIconMargin(int gap) {
        MARGIN_OUTER = gap;
        MARGIN_TOTAL = MARGIN_OUTER + MARGIN_INNER;
    }

    public int getOuterIconMargin() {
        return MARGIN_OUTER;
    }

    public void setInnerIconMargin(int gap) {
        MARGIN_INNER = gap;
        MARGIN_TOTAL = MARGIN_OUTER + MARGIN_INNER;
    }

    public int getInnerIconMargin() {
        return MARGIN_INNER;
    }

    private static LabelIcon makeLabelIcon(String text, Icon icon) {
        LabelIcon i = new LabelIcon(text);
        UIDefaults d = UIManager.getLookAndFeel().getDefaults();
        Color c = d.getColor("TextArea[Disabled].textForeground");
        if (c == null) {
            c = SystemColor.textInactiveText;
        } else {
            c = new Color(c.getRGB(), false);
        }
        i.setForeground(c);
        if (icon != null) {
            i.setIcon(icon);
        }
        return i;
    }

    public static IconBorder applyLabelBorder(JComponent target, String prefix, Icon icon, Icon hoverIcon, ActionListener li, JPopupMenu menu) {
        IconBorder ib = new IconBorder();
        Icon i;
        if (prefix != null) {
            i = makeLabelIcon(prefix, icon);
        } else {
            i = icon;
        }
        ib.setIcon(i);
        ib.setInnerIconMargin(1);
        if (li != null || menu != null) {
            ib.installClickable(target, hoverIcon, li, menu);
        } else {
            ib.install(target);
        }
        return ib;
    }

}
