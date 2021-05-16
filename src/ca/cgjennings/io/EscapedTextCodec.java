package ca.cgjennings.io;

/**
 * This class provides static methods for encoding/decoding Unicode, newline,
 * return, and tab escapes using the same format as property files. The class
 * neither inserts nor removes the possible trailing backslash that indicates
 * that a line should be concatenated with the following line.
 *
 * Valid escapeUnicode sequences consist of a backslash (\) followed by any
 * of:<br>
 * <pre>
 * uXXXX   insert Unicode character U+XXXX (where XXXX is a 16-bit hexadecimal number)
 * n       newline
 * r       return
 * f       form feed
 * t       tab
 * s       space
 *         (a slash followed by a space) space
 * \       backslash
 * :       :
 * =       =
 * #       #
 * !       !
 * </pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class EscapedTextCodec {

    private EscapedTextCodec() {
    }

    /**
     * Use Unicode escapes for characters outside of ISO8859-1.
     */
    public static final int UNICODE = 1;
    /**
     * Escape spaces (just space characters, not other whitespace).
     */
    public static final int SPACE = 1 << 1;
    /**
     * When spaces are escaped, this uses "\s" for space instead of "\ "; this
     * is not valid for .properties files, but for other types of files it
     * produces clearer output.
     */
    public static final int USE_S_ESCAPE = 1 << 4;
    /**
     * Escape the key/value assignment characters '=' and ':'.
     */
    public static final int ASSIGNMENT = 1 << 2;
    /**
     * Escape an initial '#' or '!'.
     */
    public static final int INITIAL_COMMENT = 1 << 3;
    /**
     * Escape the <b>non-space</b> whitespace characters of newline, return,
     * tab, and form feed.
     *
     * @see #SPACE
     */
    public static final int WHITESPACE = 1 << 5;
    /**
     * Escape the back slash character.
     */
    public static final int BACKSLASH = 1 << 6;

    /**
     * Tests whether a line of text ends with a line wrapping back slash. Some
     * file formats indicate that a long line is wrapped onto the next line by
     * ending the line with a back slash. This method will check for this
     * character. It correctly handles cases where the line actually ends in an
     * escaped back slash.
     *
     * @param s the line to test
     * @return {@code true} if the line ends in a line wrapping
     * escapeUnicode
     */
    @SuppressWarnings("empty-statement")
    public static boolean isWrappedLine(CharSequence s) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        final int slength = s.length();

        // count the number of backslashes at the end of the line;
        // if even (including 0), then the line is not wrapped
        int i;
        for (i = slength - 1; i >= 0 && s.charAt(i) == '\\'; --i);
        return ((slength - 1 - i) & 1) == 1;
    }

    /**
     * Escape a string by inserting Unicode escapeUnicode sequences for
     * characters outside of the printable ISO 8859-1 range. This is equivalent
     * to {@code escapeUnicode(&nbsp;s, UNICODE&nbsp;)}. If no
     * escapeUnicode codes are inserted, the original input is returned. (If it
     * is a {@code String}, the same string is returned; otherwise it is
     * converted to a string by calling its {@code toString()} method.)
     *
     * @param s the string to escapeUnicode
     * @return a string equivalent to {@code s} but will appropriate
     * escapes
     * @throws NullPointerException is {@code s} is {@code null}
     * @see #escapeUnicode(java.lang.CharSequence)
     */
    public static String escapeUnicode(CharSequence s) {
        return escape(s, UNICODE);
    }

    /**
     * Escape a string by inserting backslash escapeUnicode sequences similar to
     * the handling of escapes in {@code .properties} files. If no
     * escapeUnicode codes are inserted, the original input is returned. (If it
     * is a {@code String}, the same string is returned; otherwise it is
     * converted to a string by calling its {@code toString()} method.)
     *
     * <p>
     * The set of characters to be escaped is controlled by
     * {@code escapeFlags}, which should be a binary or of some combination
     * of      {@link #ASSIGNMENT}, {@link #BACKSLASH}, {@link #INITIAL_COMMENT},
	 * {@link #SPACE}, {@link #USE_S_ESCAPE}, {@link #UNICODE}, and
     * {@link #WHITESPACE}. For example, the combination
     * {@code UNICODE|WHITESPACE|BACKSLASH} would produced escaped output
     * similar to a {@code .properties} file. If {@code escapeFlags}
     * is zero, the original input is returned.
     *
     * @param s the string to escapeUnicode
     * @param escapeFlags optional flags for the escapeUnicode process
     * @return a string equivalent to {@code s} but will appropriate
     * escapes
     * @throws NullPointerException is {@code s} is {@code null}
     */
    @SuppressWarnings("empty-statement")
    public static String escape(CharSequence s, int escapeFlags) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        if (escapeFlags == 0) {
            return s.toString();
        }

        // do a fast O(n) check to see if anything will be escaped; actually
        // doing the escapeUnicode loop may take O(cn) time, so this will increase
        // performance if most strings don't need escaping
        final int slength = s.length();
        final int unescapedLength = findFirstEscapable(s, escapeFlags);
        if (unescapedLength == slength) {
            return s.toString();
        }

        StringBuilder b = new StringBuilder(slength + Math.min(slength, 256));
        b.append(s, 0, unescapedLength);

        for (int i = unescapedLength; i < slength; ++i) {
            char c = s.charAt(i);
            if (c > 0x3d && c < 0x7f) {
                if (c == '\\' && i < slength - 1 && (escapeFlags & BACKSLASH) != 0) {
                    b.append('\\').append('\\');
                    continue;
                }
                b.append(c);
                continue;
            }
            switch (c) {
                case ' ':
                    if ((escapeFlags & SPACE) != 0) {
                        b.append('\\');
                        if ((escapeFlags & USE_S_ESCAPE) != 0) {
                            b.append('s');
                            break;
                        }
                    }
                    b.append(' ');
                    break;
                case ':':
                case '=':
                    if ((escapeFlags & ASSIGNMENT) != 0) {
                        b.append('\\');
                    }
                    b.append(c);
                    break;
                case '#':
                case '!':
                    if ((escapeFlags & INITIAL_COMMENT) != 0) {
                        int back = i - 1;
                        for (; back >= 0 && Character.isSpaceChar(s.charAt(back)); --back);
                        if (back < 0) {
                            b.append('\\');
                        }
                    }
                    b.append(c);
                    break;
                case '\r':
                    if ((escapeFlags & WHITESPACE) == 0) {
                        b.append(c);
                    } else {
                        b.append('\\').append('r');
                    }
                    break;
                case '\n':
                    if ((escapeFlags & WHITESPACE) == 0) {
                        b.append(c);
                    } else {
                        b.append('\\').append('n');
                    }
                    break;
                case '\t':
                    if ((escapeFlags & WHITESPACE) == 0) {
                        b.append(c);
                    } else {
                        b.append('\\').append('t');
                    }
                    break;
                case '\f':
                    if ((escapeFlags & WHITESPACE) == 0) {
                        b.append(c);
                    } else {
                        b.append('\\').append('f');
                    }
                    break;
                default:
                    if ((c < 0x20 || c > 0x7e) && (escapeFlags & UNICODE) != 0) {
                        b.append('\\').append('u')
                                .append(encodeCharDigit(c >> 12))
                                .append(encodeCharDigit(c >> 8))
                                .append(encodeCharDigit(c >> 4))
                                .append(encodeCharDigit(c));
                    } else {
                        b.append(c);
                    }
            }
        }

        return b.toString();
    }

    private static int findFirstEscapable(CharSequence s, int escapeFlags) {
        final int slength = s.length();
        int i = 0;
        outer:
        for (; i < slength; ++i) {
            char c = s.charAt(i);
            if (c > 0x3d && c < 0x7f) {
                if (c == '\\' && (escapeFlags & BACKSLASH) != 0) {
                    break;
                }
                continue;
            }
            switch (c) {
                case ' ':
                    if ((escapeFlags & SPACE) != 0) {
                        break outer;
                    }
                    break;
                case ':':
                case '=':
                    if ((escapeFlags & ASSIGNMENT) != 0) {
                        break outer;
                    }
                    break;
                case '#':
                case '!':
                    if ((escapeFlags & INITIAL_COMMENT) != 0) {
                        break outer;
                    }
                    break;
                case '\r':
                case '\n':
                case '\f':
                case '\t':
                    if ((escapeFlags & WHITESPACE) != 0) {
                        break outer;
                    }
                    break;
                default:
                    if ((c < 0x20 || c > 0x7e) && (escapeFlags & UNICODE) != 0) {
                        break outer;
                    }
            }
        }
        return i;
    }

    private static char encodeCharDigit(int nybble) {
        nybble &= 0xf;
        if (nybble >= 10) {
            return (char) ('a' + nybble - 10);
        } else {
            return (char) ('0' + nybble);
        }
    }

    /**
     * Returns a string with Unicode escapes converted to normal characters. If
     * the input does not contain any Unicode escapeUnicode sequences, it is
     * returned unchanged.
     *
     * @param s the string to convert
     * @return the unescaped string
     * @throws NullPointerException if {@code s} is {@code null}
     */
    public static String unescapeUnicode(CharSequence s) {
        return unescapeImpl(s, true);
    }

    /**
     * Returns a string equivalent to {@code s}, but with all escapeUnicode
     * sequences converted back to regular (unescaped) characters. If the input
     * does not contain any escapeUnicode sequences, it is returned unchanged.
     *
     * @param s the string to convert
     * @return the unescaped string
     * @throws NullPointerException if {@code s} is {@code null}
     */
    public static String unescape(CharSequence s) {
        return unescapeImpl(s, false);
    }

    private static String unescapeImpl(CharSequence s, boolean unicodeOnly) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        int start = 0;
        final int slength = s.length();
        for (; start < slength; ++start) {
            if (s.charAt(start) == '\\') {
                break;
            }
        }
        if (start == slength) {
            return s.toString();
        }

        StringBuilder b = new StringBuilder(slength);
        b.append(s, 0, start);

        if (unicodeOnly) {
            for (int i = start; i < slength; ++i) {
                char c = s.charAt(i);
                if (c == '\\' && i < slength - 5 && s.charAt(i + 1) == 'u') {
                    char v = decodeCharDigits(s, i + 2);
                    b.append(v);
                    i += 5;
                } else {
                    b.append(c);
                }
            }
        } else {
            for (int i = start; i < slength; ++i) {
                char c = s.charAt(i);
                if (c == '\\') {
                    if (i < slength - 5 && s.charAt(i + 1) == 'u') {
                        char v = decodeCharDigits(s, i + 2);
                        b.append(v);
                        i += 5;
                    } else if (i < slength - 1) {
                        switch (s.charAt(i + 1)) {
                            case 's':
                                b.append(' ');
                                break;
                            case 'r':
                                b.append('\r');
                                break;
                            case 'n':
                                b.append('\n');
                                break;
                            case 't':
                                b.append('\t');
                                break;
                            case 'f':
                                b.append('\f');
                                break;
                            default:
                                // not a known escapeUnicode; leave it unchanged,
                                // assuming it is used by some higher-level code
                                b.append('\\');
                                b.append(s.charAt(i + 1));
                        }
                        ++i;
                    } else {
                        b.append(c);
                    }
                } else {
                    b.append(c);
                }
            }
        }

        return b.toString();
    }

    private static char decodeCharDigits(CharSequence s, int offset) {
        char v = 0;
        final int limit = offset + 4;
        for (int d = offset; d < limit; ++d) {
            v <<= 4;
            char ch = s.charAt(d);
            switch (ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    v += (ch - '0');
                    break;
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                    v += (ch - 'a' + 10);
                    break;
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                    v += (ch - 'A' + 10);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "invalid Unicode escape \\u"
                            + s.charAt(offset) + s.charAt(offset + 1)
                            + s.charAt(offset + 2) + s.charAt(offset + 3)
                    );
            }
        }
        return v;
    }

    public static void main(String[] args) {
        String s;
        s = "   # Hello my name\nis \u00d6\u00d6on# \\dude";
        System.out.println(s);
        s = escape(s, UNICODE | ASSIGNMENT | INITIAL_COMMENT | WHITESPACE | BACKSLASH);
        System.out.println(s);
        String u = unescapeUnicode(s);
        System.out.println(u);
        s = unescape(s);
        System.out.println(s);

        String[] wraps = new String[]{"", "abc", "abc\\", "abc\\\\", "abc\\\\\\"};
        for (String w : wraps) {
            System.out.println(w + ": " + isWrappedLine(w));
        }
    }
}
