package ca.cgjennings.layout;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.Locale;

/**
 * An abstract class that allows subclasses to easily create classes that filter
 * out undesired break points returned by a <code>java.text.BreakIterator</code>
 * instance.
 * <p>
 * It works by wrapping an existing base <code>BreakIterator</code> that returns
 * a superset of the desired break points. Before any potential break point is
 * returned from the adaptor, the adapter will validate it by calling
 * {@link #isBreakValidInternal(int)}. If that returns <code>true</code>, then
 * the break is returned to the caller. If it returns <code>false</code>, the
 * break will be filtered out. The next valid break in the same direction will
 * then be substituted until an acceptable break is found.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class BreakIteratorAdpater extends BreakIterator {

    public BreakIteratorAdpater() {
        setBreakIterator(BreakIterator.getLineInstance());
    }

    public BreakIteratorAdpater(Locale loc) {
        setBreakIterator(BreakIterator.getLineInstance(loc));
    }

    public BreakIteratorAdpater(BreakIterator wrapee) {
        setBreakIterator(wrapee);
    }

    protected abstract boolean isBreakValid(int pos);

    @Override
    public int current() {
        return bi.current();
    }

    @Override
    public int first() {
        return bi.first();
    }

    @Override
    public int following(int offset) {
        do {
            offset = bi.following(offset);
        } while (!isBreakValidInternal(offset));

        return offset;
    }

    @Override
    public CharacterIterator getText() {
        return ci;
    }

    @Override
    public void setText(CharacterIterator newText) {
        ci = newText;
        bi.setText(ci);
    }

    @Override
    public int last() {
        return bi.last();
        // is this the last pos or the last break?
        // if last break, should go to previous if invalid
        // likewise for first()
    }

    @Override
    public int next() {
        int p;
        do {
            p = bi.next();
        } while (!isBreakValidInternal(p));
        return p;
    }

    @Override
    public int next(int n) {
        int p = current();
        for (int i = 0; i < n; ++n) {
            p = next();
        }
        return p;
    }

    @Override
    public int previous() {
        int p;
        do {
            p = bi.previous();
        } while (!isBreakValidInternal(p));
        return p;
    }

    public void setBreakIterator(BreakIterator wrapee) {
        bi = wrapee;
    }

    public BreakIterator getBreakIterator() {
        return bi;
    }

    private boolean isBreakValidInternal(int pos) {
        if (pos == BreakIterator.DONE) {
            return true;
        }
        return isBreakValid(pos);
    }
    protected BreakIterator bi;
    protected CharacterIterator ci;
}
