package ca.cgjennings.ui.textedit.tokenizers;

/**
 * Tokenizer for editing conversion map files. Just a wrapper around
 * {@link PropertyTokenizer} with different default values. Will treat colons as
 * part of a key instead of a deliminator.
 *
 * @author Henrik Rostedt
 */
public class ConversionMapTokenizer extends PropertyTokenizer {

    public ConversionMapTokenizer() {
        super(true);
    }
}
