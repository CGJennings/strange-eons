package ca.cgjennings.apps.arkham;

import ca.cgjennings.text.SETemplateProcessor;
import gamedata.ResourceParser;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * For various historical reasons, a number of different text encodings are
 * used across Strange Eons for various purposes. This class presents a single
 * source of truth for these encodings.
 * This class contains both names and {@link Charset} instances for all of
 * the major text encodings used by Strange Eons.
 * Except for UTF-8, the members are named for their purpose rather than
 * for the actual encoding standard.
 * 
 * <p>UTF-8 is generally preferred for new features.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
public final class TextEncoding {
    /** Cannot be instantiated. */
    private TextEncoding() {}
    
    /** Name of the ISO-8859-1 encoding (internal use). */
    private static final String ISO_8859_1 = "ISO-8859-1";
    /** {@link Charset} for {@link #ISO_8859_1} (internal use). */
    private static final Charset ISO_8859_1_CS = StandardCharsets.ISO_8859_1;
    /** Name of the ISO-8859-15 encoding (internal use). */
    private static final String ISO_8859_15 = "ISO-8859-15";
    /** {@link Charset} for {@link #ISO_8859_15} (internal use). */
    private static final Charset ISO_8859_15_CS = Charset.forName(ISO_8859_15);


    /** Name of the UTF-8 8-bit Unicode encoding. */
    public static final String UTF8 = "UTF-8";
    /** {@link Charset} for {@link #UTF8}. */
    public static final Charset UTF8_CS = StandardCharsets.UTF_8;

    /** Name of the encoding used for card layout files ({@code .cardlayout)}. */
    public static final String CARD_LAYOUT = ISO_8859_1; // todo
    /** *  {@link Charset} for {@link #CARD_LAYOUT}. */
    public static final Charset CARD_LAYOUT_CS = ISO_8859_1_CS;

    /** Name of the encoding used for plug-in catalogues. */
    public static final String CATALOG = UTF8;
    /** {@link Charset} for {@link #CATALOG}. */
    public static final Charset CATALOG_CS = UTF8_CS;

    /** Name of the encoding used for the debug protocol. */
    public static final String DEBUGGER = UTF8;
    /** {@link Charset} for {@link #DEBUGGER}. */
    public static final Charset DEBUGGER_CS = UTF8_CS;

    /** Name of the encoding used for HTML and CSS resources ({@code .html}, {@code .css}). */
    public static final String HTML_CSS = UTF8;
    /** {@link Charset} for {@link #HTML_CSS}. */
    public static final Charset HTML_CSS_CS = UTF8_CS;

    /** Name of the encoding used for files processed with a {@link ResourceParser}. */
    public static final String PARSED_RESOURCE = ISO_8859_15;
    /** {@link Charset} for {@link #PARSED_RESOURCE}. */
    public static final Charset PARSED_RESOURCE_CS = ISO_8859_15_CS;

    /** Name of the encoding used for plain text files. */
    public static final String PLAIN_TEXT = UTF8;
    /** {@link Charset} for {@link #PLAIN_TEXT}. */
    public static final Charset PLAIN_TEXT_CS = UTF8_CS;

    /** Name of the encoding used for plug-in root files. */
    public static final String PLUGIN_ROOT = UTF8;
    /** {@link Charset} for {@link #PLUGIN_ROOT}. */
    public static final Charset PLUGIN_ROOT_CS = UTF8_CS;

    /** Name of the encoding used for source code ({@code .ajs}, {@code .js}, {@code .ts}, etc.). */
    public static final String SOURCE_CODE = UTF8;
    /** *  {@link Charset} for {@link #SOURCE_CODE}. */
    public static final Charset SCRIPT_CODE_CS = UTF8_CS;

    /** Name of the encoding used to store {@link resources.Settings}. */
    public static final String SETTINGS = ISO_8859_1;
    /** {@link Charset} for {@link #SETTINGS}. */
    public static final Charset SETTINGS_CS = ISO_8859_1_CS;
    
    /** Name of the encoding used to store string tables ({@code .properties}). */
    public static final String STRINGS = ISO_8859_1;
    /** {@link Charset} for {@link #STRINGS}. */
    public static final Charset STRINGS_CS = ISO_8859_1_CS;

    /** Name of the encoding used for {@link SETemplateProcessor} templates. */
    public static final String TEMPLATE = UTF8;
    /** {@link Charset} for {@link #TEMPLATE}. */
    public static final Charset TEMPLATE_CS = UTF8_CS;

    /** Name of the encoding used to store plain spelling dictionary word lists. */
    public static final String WORD_LIST = UTF8;
    /** {@link Charset} for {@link #WORD_LIST}. */
    public static final Charset WORD_LIST_CS = UTF8_CS;
}
