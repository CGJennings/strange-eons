package ca.cgjennings.layout;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.Locale;

/**
 * Wraps a BreakIterator.getLineInstance() to fix bugs in the Sun/Taligent
 * implementation.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class LineBreakIterator extends BreakIteratorAdpater {

    /**
     * Create an iterator that wraps the standard line
     * <code>BreakIterator</code> for the default locale.
     */
    public LineBreakIterator() {
        bi = BreakIterator.getLineInstance();
    }

    public LineBreakIterator(Locale loc) {
        bi = BreakIterator.getLineInstance(loc);
    }

    @Override
    protected boolean isBreakValid(int pos) {
        char prev = ci.previous();
        if (prev == CharacterIterator.DONE) {
            return true;
        }
        char curr = ci.next();

        if (prev == '\u2019' && !Character.isWhitespace(curr)) {
            return false;
        }

        return true;
    }
}
