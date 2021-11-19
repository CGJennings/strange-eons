package ca.cgjennings.ui.textedit.tokenizers;

import gamedata.ResourceParser;

/**
 * Tokenizer for editing files that that are parsed by using a
 * {@link ResourceParser} to read properties.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
public class ResourceFileTokenizer extends PropertyTokenizer {
    public ResourceFileTokenizer() {
        super(true);
    }
}
