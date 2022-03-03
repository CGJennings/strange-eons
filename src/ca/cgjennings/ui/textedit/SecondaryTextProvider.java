package ca.cgjennings.ui.textedit;

/**
 * Implemented by items that can provide text and "secondary text".
 * Typically for use by a cell renderer.
 */
public interface SecondaryTextProvider {
    String getText();
    String getSecondaryText();
}
