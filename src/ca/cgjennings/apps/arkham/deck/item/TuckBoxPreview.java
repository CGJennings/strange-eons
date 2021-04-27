package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * Preview component for the tuck box style editor.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class TuckBoxPreview extends JComponent {

    private TuckBox box;

    public TuckBoxPreview() {
        box = new TuckBox();
    }

    public TuckBoxPreview(TuckBox box) {
        this.box = box;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g1);
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = Math.min(getWidth() * 0.95d / box.getWidth(), getHeight() * 0.95d / box.getHeight());

        g.translate(
                -box.getX() + (getWidth() - (scale * box.getWidth())) / 2d,
                -box.getY() + (getHeight() - (scale * box.getHeight())) / 2d
        );
        g.scale(scale, scale);
        box.paint(g, RenderTarget.PREVIEW, 1d);
    }

    /**
     * @return the box
     */
    public TuckBox getBox() {
        return box;
    }

    /**
     * @param box the box to set
     */
    public void setBox(TuckBox box) {
        this.box = box;
        repaint();
    }
}
