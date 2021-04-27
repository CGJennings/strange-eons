package ca.cgjennings.apps.arkham.deck.item;

/**
 * A style implemented by items that can have a custom, user-defined bleed
 * margin.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface UserBleedMarginStyle extends Style {

    /**
     * Returns <code>true</code> if the bleed margin should be drawn (as crop
     * marks).
     *
     * @return <code>true</code> if crop marks are enabled
     */
    boolean isBleedMarginMarked();

    /**
     * Sets whether the user bleed margin is drawn (as crop marks).
     *
     * @param mark <code>true</code> if crop marks are enabled
     */
    void setBleedMarginMarked(boolean mark);

    /**
     * Returns the size of the user-defined bleed margin, in points.
     *
     * @return the bleed margin size
     */
    double getBleedMargin();

    /**
     * Sets the size of the user-defined bleed margin, in points. If the
     * specified value is more half of the greater of the width or height of the
     * item, then the value will be silently reduced to this maximum.
     *
     * @param margin the bleed margin size
     */
    void setBleedMargin(double margin);
}
