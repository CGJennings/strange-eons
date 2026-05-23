package ca.cgjennings.layout;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.Arrays;

/**
 * A break iterator that caches the breakpoints of another iterator. This is
 * useful when performing text layout, as break point analysis is used heavily
 * during this process.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public final class FastBreakIterator extends BreakIterator {

    private final BreakIterator bi;
    private int[] buffer;
    int last;
    int current;

    public FastBreakIterator(BreakIterator iteratorToCache) {
        bi = iteratorToCache;
    }

    @Override
    public void setText(CharacterIterator newText) {
        int length = newText.getEndIndex() - newText.getBeginIndex() + 2;
        if (buffer == null || buffer.length < length) {
            buffer = new int[length];
        }

        // get all breaks in the text in one go
        bi.setText(newText);
        buffer[0] = bi.first();
        int i, pos;
        for (i = 1; (pos = bi.next()) != BreakIterator.DONE; ++i) {
            buffer[i] = pos;
        }
        buffer[i] = Integer.MAX_VALUE;
//        Arrays.fill(buffer, i, buffer.length, Integer.MAX_VALUE);
        last = i - 1;
    }

    @Override
    public CharacterIterator getText() {
        return bi.getText();
    }

    @Override
    public int current() {
        return buffer[current];
    }

    @Override
    public int first() {
        current = 0;
        return buffer[0];
    }

    @Override
    public int last() {
        current = last;
        return buffer[last];
    }

    @Override
    public int following(int offset) {
        if (current < last && buffer[current] <= offset) {
            if (buffer[current + 1] > offset) {
                return buffer[++current];
            }
        }

        int match = Arrays.binarySearch(buffer, 0, last + 1, offset);

        if (match < 0) {
            match = -match - 1;
            if (match > last) {
                current = last;
                return BreakIterator.DONE;
            }
            current = match;
            return buffer[match];
        } else {
            if (match > last) {
                current = last;
                return BreakIterator.DONE;
            }
            current = match;
            return buffer[match];
        }
    }

    @Override
    public int preceding(int offset) {
        if (current > 0 && buffer[current] >= offset) {
            if (buffer[current - 1] < offset) {
                return buffer[--current];
            }
        }

        int match = Arrays.binarySearch(buffer, 0, last + 1, offset);

        if (match < 0) {
            match = (-match - 1) - 1;
            if (match < 0) {
                current = 0;
                return BreakIterator.DONE;
            }
            current = match;
            return buffer[match];
        } else {
            if (--match < 0) {
                current = 0;
                return BreakIterator.DONE;
            }
            current = match;
            return buffer[match];
        }
    }

    @Override
    public int next(int n) {
        if (n > 0) {
            if (current + n > last) {
                current = last;
                return BreakIterator.DONE;
            }
            current += n;
            return buffer[current];
        } else if (n < 0) {
            if (current - n < 0) {
                current = 0;
                return BreakIterator.DONE;
            }
            current -= n;
            return buffer[current];
        }
        return buffer[current];
    }

    @Override
    public int next() {
        if (current == last) {
            return BreakIterator.DONE;
        }
        return buffer[++current];
    }

    @Override
    public int previous() {
        if (current == 0) {
            return BreakIterator.DONE;
        }
        return buffer[--current];
    }
}
