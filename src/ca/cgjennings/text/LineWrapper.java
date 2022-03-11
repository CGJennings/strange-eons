package ca.cgjennings.text;

/**
 * A {@code LineWrapper} breaks long strings into multiple lines. It is intended
 * for use with fixed-width code-like text rather than natural language text,
 * although it can be used with any string.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class LineWrapper {

    private String breakText;
    private int firstWrap;
    private int afterWrap;
    private int tolerance;

    /**
     * Creates a line wrapper with a default configuration. This is equivalent
     * to {@code LineWrapper( null, 78, 78, 4 )}.
     */
    public LineWrapper() {
        this(null, 78, 78, 4);
    }

    /**
     * Creates a new line wrapper with the specified wrapper behaviour.
     *
     * @param breakText the text to insert when breaking a line; if {@code null}
     * then a newline character (\n) is used
     * @param firstWrap the optimal length for the first line
     * @param afterWrap the optimal length for lines after the first
     * @param tolerance the amount by which the remaining text must exceed the
     * optimal length before a break is inserted (this allows you to avoid
     * breaking a line when only {@code tolerance} or fewer characters would
     * appear on the following line)
     */
    public LineWrapper(String breakText, int firstWrap, int afterWrap, int tolerance) {
        this.breakText = breakText == null ? "\n" : breakText;
        this.firstWrap = firstWrap;
        this.afterWrap = afterWrap;
        this.tolerance = tolerance;
    }

    /**
     * Returns the break text.
     *
     * @return the text inserted between wrapped lines
     */
    public String getBreakText() {
        return breakText;
    }

    /**
     * Sets the break text.
     *
     * @param breakText the non-null text inserted between wrapped lines
     */
    public void setBreakText(String breakText) {
        if (breakText == null) {
            throw new NullPointerException("breakText");
        }
        this.breakText = breakText;
    }

    /**
     * Returns the line wrap length for the first line.
     *
     * @return the ideal line length
     */
    public int getFirstLineWrapLength() {
        return firstWrap;
    }

    /**
     * Sets the line wrap length for the first line.
     *
     * @param firstWrap the positive ideal line length
     */
    public void setFirstLineWrapLength(int firstWrap) {
        if (firstWrap < 1) {
            throw new IllegalArgumentException("firstWrap must be >0");
        }
        this.firstWrap = firstWrap;
    }

    /**
     * Returns the line wrap length for lines after the first.
     *
     * @return the ideal line length
     */
    public int getFollowingLineWrapLength() {
        return afterWrap;
    }

    public void setFollowingLineWrapLength(int afterWrap) {
        if (afterWrap < 1) {
            throw new IllegalArgumentException("afterWrap must be >0");
        }
        this.afterWrap = afterWrap;
    }

    public int getWrapLengthTolerance() {
        return tolerance;
    }

    public void setWrapLengthTolerance(int tolerance) {
        if (firstWrap < 0) {
            throw new IllegalArgumentException("tolerance must be >=0");
        }
        this.tolerance = tolerance;
    }

    /**
     * Breaks a string into multiple lines, if necessary, using the current
     * line-wrapping parameters. If the string needs no breaking, the original
     * string is returned. Otherwise, a copy of the string is returned that has
     * the current break text inserted at the selected break points.
     *
     * @param s the string to wrap into lines
     * @return the wrapped string
     */
    public String wrap(String s) {
        if (s.length() < firstWrap) {
            return s;
        }

        int wrap = firstWrap;
        StringBuilder b = new StringBuilder();
        int line = 0, maxLines = 1024;

        while (s.length() > (wrap + tolerance) && line++ < maxLines) {
            // find acceptable wrap position:
            //   - must come right after a space
            //   - next char must not be a space
            int pos = wrap;
            while (pos >= 0 && !Character.isSpaceChar(s.charAt(pos))) {
                --pos;
            }
            // couldn't find a space, just break at the wrap point
            if (pos < 0) {
                pos = wrap;
            }
            // skip any following spaces
            while (pos < s.length() - 1 && Character.isSpaceChar(s.charAt(pos + 1))) {
                ++pos;
            }
            // the break char should be included in the string for this line;
            // but substring excludes the final pos
            ++pos;
            b.append(s.substring(0, pos));
            s = s.substring(pos);
            if (!s.isEmpty()) {
                b.append(breakText);
            }

            wrap = afterWrap;
        }
        if (!s.isEmpty()) {
            b.append(s);
        }
        return b.toString();
    }
}
