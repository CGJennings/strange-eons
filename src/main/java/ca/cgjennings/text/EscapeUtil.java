package ca.cgjennings.text;

/**
 * String escape utilities.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class EscapeUtil {
    private EscapeUtil() {}
    
    /**
     * Escapes characters that have special meaning inside HTML.
     * Not suitable for use to escape text for use as an attribute of an
     * HTML element.
     * 
     * @param source the string to escape
     * @return the escaped string, or the original string
     */
    public static String escapeHtml(String source) {
        final int len = source.length();
        int p =0;
        for (; p<len; ++p) {
            char ch = source.charAt(p);
            if (ch == '&' || ch == '<') {
                break;
            }
        }
        if (p == len) {
            return source;
        }
        
        StringBuilder b = new StringBuilder(len * 4/3);
        b.append(source, 0, p);
        for (int i = p; i < len; ++i) {
            final char ch = source.charAt(i);
            switch (ch) {
                case '&':
                    b.append("&amp;");
                    break;
                case '<':
                    b.append("&lt;");
                    break;
                default:
                    b.append(ch);
            }
        }
        return b.toString();
    }
}
