package ca.cgjennings.ui;

import java.awt.Dimension;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

/**
 * A combo box that optionally allows the pop-up menu to be wider than the combo
 * box itself.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class WideItemComboBox extends JComboBox {

    private boolean widePopupEnabled = true;
    private int maxPopupWidth = getToolkit().getScreenSize().width;
    private boolean isLayingOutComboBox = false;

    public WideItemComboBox(ComboBoxModel aModel) {
        super(aModel);
    }

    public WideItemComboBox(Object[] items) {
        super(items);
    }

    public WideItemComboBox(Vector items) {
        super(items);
    }

    public WideItemComboBox() {
    }

    /**
     * Returns the maximum width that the popup menu can grow to. The actual
     * width will vary between the width of the combo box and this value.
     *
     * @return the maximum popup menu width
     */
    public int getMaxPopupWidth() {
        return maxPopupWidth;
    }

    /**
     * Sets the maximum width that the popup menu can grow to.
     *
     * @param maxPopupWidth the maximum popup menu width
     */
    public void setMaxPopupWidth(int maxPopupWidth) {
        if (maxPopupWidth < 1) {
            throw new IllegalArgumentException("maxPopupWidth < 1: " + maxPopupWidth);
        }
        this.maxPopupWidth = maxPopupWidth;
    }

    /**
     * Returns {@code true} if the popup menu is allowed to be wider than
     * the combo box when displaying wide items.
     *
     * @return {@code true} if the popup width expands for large items
     */
    public boolean isWidePopupEnabled() {
        return widePopupEnabled;
    }

    /**
     * Sets whether the popup menu is allowed to be wider than the combo box
     * when displaying wide items.
     *
     * @param widePopupEnabled if {@code true} the popup width expands for
     * large items
     */
    public void setWidePopupEnabled(boolean widePopupEnabled) {
        this.widePopupEnabled = widePopupEnabled;
    }

    @Override
    public void doLayout() {
        try {
            isLayingOutComboBox = true;
            super.doLayout();
        } finally {
            isLayingOutComboBox = false;
        }
    }

    @Override
    public Dimension getSize() {
        Dimension dim = super.getSize();
        if (!isLayingOutComboBox && isWidePopupEnabled()) {
            dim.width = Math.min(maxPopupWidth, Math.max(dim.width, getPreferredSize().width));
        }
        return dim;
    }
}
