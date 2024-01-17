package ca.cgjennings.apps.arkham.plugins.typescript;

/**
 * Encapsulates a span of text, with a start offset and length.
 * 
 * @author Christopher G. Jennings (cjennings@acm.org)
 */
public class TextSpan {
    /** The start offset from the beginning of the text. */
    public int start;
    /** The length of the span. */
    public int length;
    
    public TextSpan(int start, int length) {
        this.start = start;
        this.length = length;
    }

    @Override
    public String toString() {
        return "TextSpan{" + "start=" + start + ", length=" + length + '}';
    }
}
