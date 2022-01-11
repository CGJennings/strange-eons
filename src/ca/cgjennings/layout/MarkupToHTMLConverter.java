package ca.cgjennings.layout;

import ca.cgjennings.layout.MarkupRenderer.StyledParagraph;
import java.awt.Color;
import java.awt.Image;
import java.awt.font.TextAttribute;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import resources.StrangeImage;

/**
 * Converts markup written for a {@code MarkupRenderer} into simple HTML. It
 * works by taking advantage of the customizability of {@code MarkupRenderer}'s
 * tag handling to insert "invisible" tags back into the markup. The tags are
 * made invisible by surrounding them with special, non-printing characters
 * instead of angle brackets. After the markup is processed, the styled text is
 * converted back into plain text and the invisible tags are converted back into
 * regular tags.
 * <p>
 * This approach allow us to ensure that the markup is processed in exactly the
 * same way as standard markup before being handed to the HTML converter.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class MarkupToHTMLConverter extends MarkupRenderer {

    /**
     *
     */
    public MarkupToHTMLConverter() {
        setHeadlineAlignment(LAYOUT_LEFT);
    }
    private static final int SWITCH_TO_UNICODE_ESCAPES_ABOVE = 127;

    public String markupToHTML(String markup) {
        return markupToHTML(markup, null);
    }

    public String markupToHTML(String markup, String title) {
        return markupToHTML(markup, title, null);
    }

    public String markupToHTML(String markup, String title, String style) {
        setMarkupText(markup);
        createStyledText();
        StringBuilder b = new StringBuilder(markup.length());
        b.append("<html>\n<head>\n");
        if (title != null) {
            b.append("<title>").append(title).append("</title>\n");
        }
        if (style != null) {
            b.append("<style>\n").append(style).append("\n</style>");
        }
        b.append("</head>\n<body>\n");
        b.append("<p>");

        StyledParagraph[] paragraphs = getStyledText();
        int currentAlignment = LAYOUT_LEFT;

        for (int i = 0; i < paragraphs.length; ++i) {
            StyledParagraph p = paragraphs[i];

            // check if the alignment has changed from the last line
            if (currentAlignment != p.getAlignment()) {
                currentAlignment = p.getAlignment();
                b.append("</p>\n").append(createParagraphTagForAlignment(currentAlignment));
            }

            AttributedCharacterIterator it = p.getIterator();
            char c = it.first();
            while (c != AttributedCharacterIterator.DONE) {
                switch (c) {
                    case L:
                        b.append('<');
                        break;
                    case R:
                        b.append('>');
                        break;
                    case Q:
                        b.append('"');
                        break;
                    case A:
                        b.append('\'');
                        break;
                    case P:
                        b.append('.');
                        break;
                    case H:
                        b.append('-');
                        break;
                    case '<':
                        b.append("&lt;");
                        break;
                    case '>':
                        b.append("&gt;");
                        break;
                    default:
                        if (c > SWITCH_TO_UNICODE_ESCAPES_ABOVE) {
                            b.append("&#x").append(Integer.toHexString(c)).append(';');
                        } else {
                            b.append(c);
                        }
                }
                c = it.next();
            }

            if (i < paragraphs.length - 1) {
                AttributedCharacterIterator next = paragraphs[i + 1].getIterator();
                if (next.getEndIndex() - next.getBeginIndex() == 1 && next.first() == ' ') {
                    b.append("</p>\n").append(createParagraphTagForAlignment(currentAlignment));
                    ++i; // we have just handled the next entry, so skip it

                } else {
                    b.append("\n<br>\n");
                }

            }
        }
        b.append("</p>\n</body>\n</html>");
        return b.toString();
    }

    @Override
    protected String handleUnknownTag(String tagnameLowercase, String tagnameOriginalCase) {
        // Unicode escapes
        if (tagnameLowercase.length() <= "u+0000".length() && tagnameLowercase.startsWith("u+")) {
            try {
                char unicodeChar = (char) Integer.parseInt(tagnameLowercase.substring("u+".length()), 16);
                if (unicodeChar == '<') {
                    return "&lt;";
                } else if (unicodeChar == '>') {
                    return "&gt;";
                } else {
                    return "&#x" + Integer.toHexString(unicodeChar) + ";";
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // if left null by the following code, then whatever was sent is assumed
        //   to be HTML and will be encoded before passing it back up for insertion
        String tag = null;
        String[] params = getTagParameters();

        String tagName;
        {
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 0; i < tagnameLowercase.length() && !Character.isWhitespace(tagnameLowercase.charAt(i)); ++i) {
                nameBuilder.append(tagnameLowercase.charAt(i));
            }
            tagName = nameBuilder.toString();
        }

        // Image tags
        //if( tagnameLowercase.length() >= 6 && tagnameLowercase.startsWith( "image" ) && Character.isWhitespace( tagnameLowercase.charAt(5) ) ) {
        switch (tagName) {
            // fammily tag for typeface
            case "image":
                if (params.length == 0) {
                    return null;
                }
                String url;
                try {
                    if (params[0] != null && getBaseFile() != null && params[0].indexOf(':') < 0) {
                        params[0] = GraphicStyleFactory.translateRelativePath(params[0], getBaseFile());
                    }

                    URL imURL = StrangeImage.identifierToURL(params[0]);
                    if (imURL == null) {
                        url = params[0];
                    } else {
                        url = imURL.toURI().toASCIIString();
                    }
                } catch (URISyntaxException e) {
                    url = params[0];
                }
                StringBuilder b = new StringBuilder();
                b.append("<img src=\"").append(url).append("\"");
                if (params.length >= 2) {
                    double length = parseMeasurement(params[1]);
                    if (length == length && length > 0) {
                        b.append(" width=\"").append((int) ((length * dpiForInlineImages) + 0.5d)).append("\"");
                    }

                    if (params.length >= 3) {
                        length = parseMeasurement(params[2]);
                    } else {
                        try {
                            Image im = StrangeImage.getAsBufferedImage(params[0]);
                            double width = im.getWidth(null);
                            double height = im.getHeight(null);
                            length = height / width * length;
                        } catch (Exception e) {
                            length = -1f;
                        }
                    }
                    if (length > 0) {
                        b.append(" height=\"").append((int) ((length * dpiForInlineImages) + 0.5d)).append("\"");
                    }
                }
                tag = encodeHTML(b.append('>').toString());
                break;
            // colour / bgcolour text colour tags
            case "family":
                if (params.length == 0) {
                    return null;
                }
                tag = "<span style='font-family: " + params[0] + "'>";
                break;
            // closing tag for all of the above
            case "colour":
            case "color":
            case "bgcolour":
            case "bgcolor":
                String bg = tagName.startsWith("bg") ? "background-" : "";
                Color c = ForegroundColorStyleFactory.parseColor(params);
                if (c == null) {
                    return null;
                }
                int rgb = c.getRGB() & 0x00ff_ffff;
                tag = String.format("<span style='%scolor: #%06x'>", bg, rgb);
                break;
            case "/family":
            case "/size":
            case "/colour":
            case "/color":
            case "/bgcolour":
            case "/bgcolor":
                tag = "</span>";
                break;
            default:
                break;
        }

        // handle unknown <#tags>
        if (tagnameLowercase.startsWith("#") || tagnameLowercase.startsWith("^")) {
            return encodeHTML("<span class='badref'>") + "[??]" + encodeHTML("</span>") + encodeHTML("<span class='draft'>") + "{" + tagnameLowercase + "}" + encodeHTML("</span>");
        }

        // default: tag is still null, so assume it's a bit of HTML and encode it
        if (tag == null) {
            tag = encodeHTML("<" + tagnameOriginalCase + ">");
        }

        return tag;
    }

    public void setDPIForInlineImages(double dpi) {
        dpiForInlineImages = dpi;
    }

    public double getDPIForInlineImages() {
        return dpiForInlineImages;
    }

    // the resolution used to convert image sizes into a number of pixels when
    // converting inline images from markup to HTML
    public static double dpiForInlineImages = 96d;

    // replacement chars for html tags:
    private static final char L = '\u0001'; // <
    private static final char R = '\u0002'; // >
    private static final char Q = '\u0003'; // "
    private static final char A = '\u0004'; // '
    private static final char P = '\u0005'; // .
    private static final char H = '\u0006'; // -
    private static final char FIRST_HTML_CODE = L;
    private static final char LAST_HTML_CODE = H;

    private String createParagraphTagForAlignment(int layout) {
        switch (layout) {
            case LAYOUT_CENTER:
                return "<p style='text-align: center' align='center'>";
            case LAYOUT_RIGHT:
                return "<p style='text-align: right' align='right'>";
            default:
                return "<p>";
        }
    }

    @Override
    protected void createDefaultStyleMap() {
    }

    @Override
    protected void createDefaultParametricStyleMap() {
    }

    /**
     * Add default entries to the set of replacements.
     */
    @Override
    protected void createDefaultReplacementMap() {
        String[] replacements = new String[]{
            "infinity", "&#x221e;",
            "lq", "&#x201c;",
            "rq", "&#x201d;",
            "\"", "&#x201c;",
            "/\"", "&#x201d;",
            "lsq", "&#x2018;",
            "rsq", "&#x2019;",
            "'", "&#x2018;",
            "/'", "&#x2019;",
            "endash", "&#x2013;",
            "emdash", "&#x2014;",
            "--", "&#x2013;",
            "---", "&#x2014;",
            "...", "&#x2026;",
            "lg", "&#x00ab;",
            "rg", "&#x00bb;",
            "lsg", "&#x2039;",
            "rsg", "&#x203a;",
            "nbsp", "&nbsp;",
            " ", "&nbsp;",
            "thsp", "&#x2009;",
            "emsp", "&#x2003;",
            "ensp", "&#x2002;",
            "hsp", "&#x200a;",};
        for (int i = 0; i < replacements.length; i += 2) {
            setReplacementForTag(replacements[i], replacements[i + 1]);
        }

        // create tags for standard colour tags
        final Object[] colours = {
            "black", TextStyle.COLOR_BLACK,
            "blue", TextStyle.COLOR_BLUE,
            "brown", TextStyle.COLOR_BROWN,
            "dark grey", TextStyle.COLOR_DKGREY,
            "dark gray", TextStyle.COLOR_DKGREY,
            "green", TextStyle.COLOR_GREEN,
            "grey", TextStyle.COLOR_GREY,
            "gray", TextStyle.COLOR_GREY,
            "light grey", TextStyle.COLOR_LTGREY,
            "light gray", TextStyle.COLOR_LTGREY,
            "orange", TextStyle.COLOR_ORANGE,
            "purple", TextStyle.COLOR_PURPLE,
            "red", TextStyle.COLOR_RED,
            "white", TextStyle.COLOR_WHITE,
            "yellow", TextStyle.COLOR_YELLOW
        };
        for (int i = 0; i < colours.length; i += 2) {
            String hexColour = Integer.toHexString(((Color) ((TextStyle) colours[i + 1]).get(TextAttribute.FOREGROUND)).getRGB() & 0xff_ffff);
            setReplacementForTag((String) colours[i], "<color " + hexColour + ">");
            setReplacementForTag("/" + colours[i], "</color>");
        }

        // create tags for standard SE font names
        String[] fontTags = {"body", "tt", "card", "largecard"};
        for (String tag : fontTags) {
            setReplacementForTag(tag, "<span class='" + tag + "'>");
            setReplacementForTag("/" + tag, "</span>");
        }
    }

    /**
     * When we find tags that we don't understand how to deal with, we assume
     * that they are HTML. We want to pass these on to the underlying user
     * agent. If we leave it as-is, the unknown tags will be eaten by the
     * renderer. We convert the characters that will confuse the renderer into
     * other, special characters. Then the string can be fed to markup renderer
     * and will come out the other side unchanged. When we do the conversion
     * from the text generated by the markup renderer to HTML, we will decode
     * the special characters to get back the original text.
     */
    private String encodeHTML(String tag) {
        char[] coded = tag.toCharArray();
        for (int i = 0; i < coded.length; ++i) {
            char r = coded[i];
            switch (r) {
                case '<':
                    r = L;
                    break;
                case '>':
                    r = R;
                    break;
                case '"':
                    r = Q;
                    break;
                case '\'':
                    r = A;
                    break;
                case '.':
                    r = P;
                    break;
                case '-':
                    r = H;
                    break;
            }
            coded[i] = r;
        }
        return new String(coded);
    }

    /**
     * The superclass doesn't handle our special HTML encoding characters, so we
     * must override this.
     */
    @Override
    protected char selectPunctuation(char curr, StringBuilder writtenText) {
        if (currentPunctuationReplacement < 0) {
            return curr;
        }

        // HERE IS THE PROBLEM
        //
        // when the previous char is encoded HTML, the superclass gets confused
        // a completely correct solution would search backwards from the current position,
        // automatically skipping past encoded tags to find out what the previous character
        // was on the other side
        //
        // we cheat and treat an encoded character as if it were a space, which is almost always fine
        //
        char prev = writtenText.length() == 0 ? ' ' : writtenText.charAt(writtenText.length() - 1);

        if (prev >= FIRST_HTML_CODE && prev <= LAST_HTML_CODE) {
            prev = ' ';
        }

        // Straight quotes to open and close quotes
        if (curr == '"' || curr == '\'') {
            boolean useCloseQuote = true;

            if (Character.isWhitespace(prev)) {
                useCloseQuote = false;
            } else {
                int type = Character.getType(prev);
                if (type == Character.DASH_PUNCTUATION) {
                    useCloseQuote = openQuotes > 0;
                }
            }

            if (useCloseQuote) {
                if (--openQuotes < 0) {
                    openQuotes = 0;
                }
                curr = (curr == '"') ? '\u201d' : '\u2019';
            } else {
                ++openQuotes;
                curr = (curr == '"') ? '\u201c' : '\u2018';
            }
        } // Hyphens to en-dash and em-dash
        else if (curr == '-') {
            if (prev == '-' || prev == '\u2013') {
                deletePreviousPunctuation(writtenText, 1);
                curr = (prev == '-') ? '\u2013' : '\u2014';
            }
        } // 3 periods to ellipsis
        else if (curr == '.' && prev == '.') {
            if (writtenText.length() >= 2 && writtenText.charAt(writtenText.length() - 2) == '.') {
                deletePreviousPunctuation(writtenText, 2);
                curr = '\u2026';
            }
        }
        return curr;
    }
    // copied from superclass where it is (and should be) private --- used only by selectPunctuation

    private void deletePreviousPunctuation(StringBuilder text, int count) {
        assert (count > 0);
        text.delete(text.length() - count, text.length());
    }

    /**
     * Breaks a source string into an array of individual lines. Unlike the
     * rendering version, we don't want to split out headline tags as a special
     * case because the HTML agent should be in charge of that.
     */
    @Override
    protected String[] breakIntoParagraphs(String source) {
        return source.split("\n");
    }

    @Override
    protected TextStyle adjustStyleForContext(TextStyle style) {
        return style;
    }
}
