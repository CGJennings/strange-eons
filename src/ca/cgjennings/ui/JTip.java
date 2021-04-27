package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.border.Border;
import resources.ResourceKit;

/**
 * A small icon that displays a tip when the cursor floats over it. The tip text
 * is set via {@link #setTipText}.
 *
 * <p>
 * <b>Note:</b> This class uses a <code>JLabel</code> to display the pop-up tip
 * text. Subclasses may substitute a different component by overriding
 * {@link #getTipComponent()}. If the returned component is not an instance of
 * <code>JLabel</code>, then the methods for getting and setting the tip
 * properties must also be overridden.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JTip extends JLabel {

    private JComponent tip;
    private Popup popup;

    private static final Border TIP_BORDER = JUtilities.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createLineBorder(new Color(0x24_9bcc), 1),
            BorderFactory.createLineBorder(new Color(0x80_c9e8), 1),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
    );

    public JTip() {
        setIcon(ResourceKit.getIcon("application/information-sm.png"));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel stdTip = new JLabel();
        stdTip.setFont(stdTip.getFont().deriveFont(Font.PLAIN, stdTip.getFont().getSize2D() - 1));
        stdTip.setBackground(Color.WHITE);
        stdTip.setForeground(Color.BLACK);
        stdTip.setOpaque(true);
        stdTip.setBorder(TIP_BORDER);
        stdTip.setIcon(ResourceKit.getIcon("application/information.png"));
        stdTip.setIconTextGap(12);
        this.tip = stdTip;

        // Note: mouse pressed/released for touch input
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                showTip();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hideTip();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    showTip();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    hideTip();
                }
            }
        });
    }

    private void showTip() {
        if (popup != null) {
            return;
        }

        JComponent tip = getTipComponent();
        if (tip == null) {
            return;
        }

        // if using the base class tip, setting null tip text hides the pop-up
        if (tip == JTip.this.tip && getTipText() == null) {
            return;
        }

        updateTipComponent();

        Point p = getLocationOnScreen();

        int w = tip.getWidth();
        int h = tip.getHeight();
        int x = p.x + (getWidth() - tip.getWidth()) / 2;
        int y = p.y + getHeight() + VERTCIAL_GAP_TO_POPUP;

        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle r = gc.getBounds();
        Insets i = getToolkit().getScreenInsets(gc);
        r.x += i.left;
        r.y += i.top;
        r.width -= i.left + i.right;
        r.height -= i.top + i.bottom;

        if (x + w > r.x + r.width) {
            x = r.x + r.width - w - 1;
        }
        // if popup goes below bottom of screen, don't just move up, move
        // above the tip label so that the label doesn't get a mouse exit
        // event, which would immediately hide the tip
        if (y + h > r.y + r.height) {
            y = p.y - h - VERTCIAL_GAP_TO_POPUP;
        }
        if (x < r.x) {
            x = r.x + 1;
        }
        if (y < r.y) {
            y = r.y + 1;
        }

        popup = PopupFactory.getSharedInstance().getPopup(JTip.this, tip, x, y);
        popup.show();
    }

    private void hideTip() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }

    /**
     * The vertical space between the tip label and the pop-up.
     */
    private static final int VERTCIAL_GAP_TO_POPUP = 12;

    /**
     * Creates a new <code>JTip</code> using the specified tip text.
     *
     * @param tipText the pop-up tip text to display
     */
    public JTip(String tipText) {
        this();
        setTipText(tipText);
    }

    /**
     * Sets the pop-up tip text displayed by this tip control.
     *
     * @param tipText the text to display when the mouse moves over this control
     */
    public void setTipText(String tipText) {
        final JLabel c = (JLabel) getTipComponent();
        c.setText(tipText);
    }

    /**
     * Returns the pop-up tip text displayed by this tip control.
     *
     * @return the text displayed when the mouse moves over this control
     */
    public String getTipText() {
        return ((JLabel) getTipComponent()).getText();
    }

//	public static void main( String[] args ) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				JFrame f = new JFrame();
//				f.add( new JTip( "Hullo. This is a test tip." ) );
//				f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//				f.pack();
//				f.setLocationRelativeTo( null );
//				f.setVisible( true );
//			}
//		});
//	}
    /**
     * Returns the component used to display the tip's pop-up text. If this
     * returns <code>null</code>, then no pop-up will be shown.
     *
     * @return the tip label
     */
    public JComponent getTipComponent() {
        return tip;
    }

    /**
     * Allows subclasses to modify the tip component just prior to display.
     */
    protected void updateTipComponent() {
        final JLabel tip = (JLabel) getTipComponent();
        tip.setSize(tip.getPreferredSize());
    }

    @Override
    public Dimension getPreferredSize() {
        // improves selectability on touch devices
        Dimension d = super.getPreferredSize();
        if (d.width < 24) {
            d.width = 24;
        }
        return d;
    }
}
