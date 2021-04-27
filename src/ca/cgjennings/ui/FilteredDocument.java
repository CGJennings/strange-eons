package ca.cgjennings.ui;

import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * An extension of <code>PlainDocument</code> that transparently filters out
 * certain characters. A set of characters is provided as a string, and the
 * filter operates as either a white list (allowing only characters in the
 * filter string) or a black list (allowing all characters except those in the
 * filter string).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class FilteredDocument extends PlainDocument {

    private String blackList;
    private Pattern filterPat;
    private boolean isWhiteList;

    /**
     * Creates a filtered document that allows all characters.
     */
    public FilteredDocument() {
        this("");
    }

    /**
     * Creates a filtered document that filters out the characters in the filter
     * list.
     *
     * @param blackList the characters to exclude
     * @throws NullPointerException if <code>filterList</code> is
     * <code>null</code>
     */
    public FilteredDocument(String blackList) {
        this(blackList, false);
    }

    /**
     * Creates a filtered document with the specified filter characters and list
     * type.
     *
     * @param filterList the characters to include/exclude
     * @param isWhiteList whether the filter includes or excludes the filter
     * list
     * @throws NullPointerException if <code>filterList</code> is
     * <code>null</code>
     */
    public FilteredDocument(String filterList, boolean isWhiteList) {
        super();
        this.isWhiteList = isWhiteList;
        setFilteredCharacters(filterList);
    }

    /**
     * Sets whether the list is treated as a white list (<code>true</code>) or
     * black list (<code>false</code>).
     *
     * @param isWhiteList <code>true</code> if the filter characters are
     * <i>allowed</i>
     */
    public void setWhiteList(boolean isWhiteList) {
        this.isWhiteList = isWhiteList;
        setFilteredCharacters(blackList);
    }

    /**
     * Returns <code>true</code> if the filter characters form a white list
     * (list of the allowed characters), or <code>false</code> if they form a
     * black list (list of excluded characters).
     *
     * @return <code>true</code> if the list is a white list
     */
    public boolean isWhiteList() {
        return isWhiteList;
    }

    /**
     * Sets the list of characters to include/exclude.
     *
     * @param filterList each character in this string will be excluded or
     * included (depending on whether the set is a whitelist)
     * @throws NullPointerException if <code>filterList</code> is
     * <code>null</code>
     */
    public void setFilteredCharacters(String filterList) {
        if (filterList == null) {
            throw new NullPointerException("filterList");
        }
        this.blackList = filterList;
        StringBuilder pat = new StringBuilder(filterList.length() * 3 + 3);
        pat.append('[');
        if (isWhiteList) {
            pat.append('^');
        }
        for (int i = 0; i < filterList.length(); ++i) {
            if (i > 0) {
                pat.append('|');
            }
            pat.append(Pattern.quote(filterList.substring(i, i + 1)));
        }
        pat.append(']');
        filterPat = Pattern.compile(pat.toString());
    }

    /**
     * Returns the characters being filtered as a string.
     *
     * @return the characters to include/exclude
     */
    public String getFilteredCharacters() {
        return blackList;
    }

    /**
     * Performs filtering of characters when new text is inserted into the
     * document being calling the super implementation to actually insert the
     * (filtered) text.
     *
     * @param offs offset into the document
     * @param str the string to insert
     * @param a attributes for the inserted text (typically <code>null</code>)
     * @throws BadLocationException if the offset is not within the document
     * bounds (0..document.length())
     */
    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        String filtered = filterPat.matcher(str).replaceAll("");
        if (filtered.length() != str.length()) {
            onFiltration(str, filtered);
        }
        if (filtered.length() > 0) {
            super.insertString(offs, filtered, a);
        }
    }

    /**
     * Override to react to a string having characters filtered out. The default
     * implementation emits an error beep.
     *
     * @param oldString the original string that was to be inserted into the
     * document
     * @param newString the (possibly empty) filtered string
     */
    protected void onFiltration(String oldString, String newString) {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Returns a new document that filters out characters that are illegal in
     * Windows, *NIX file names.
     *
     * @return a file name filtering document
     */
    public static final PlainDocument createFileNameDocument() {
        return new FilteredDocument("?[]/\\=+<>:;\",*|^~");
    }

    /**
     * Returns a new document that filters out characters that are illegal in
     * Windows, *NIX file paths. This is identical to the filter created with
     * {@link #createFileNameDocument()}, but it does not filter path characters
     * (/, \, and :).
     *
     * @return a file path filtering document
     */
    public static final PlainDocument createFilePathDocument() {
        return new FilteredDocument("?[]=+<>;\",*|^~");
    }

    /**
     * Returns a new document that filters out non-digits.
     *
     * @return a document that accepts only digits
     */
    public static final PlainDocument createDigitDocument() {
        return new FilteredDocument("0123456789", true);
    }

    /**
     * Returns a new document that filters out characters that are escaped in
     * property files.
     *
     * @return a property key filtering document
     */
    public static final PlainDocument createPropertyKeyDocument() {
        return new FilteredDocument("\\=: \t\r\n#!");
    }

    /**
     * Returns a new document that converts all text to lower case.
     *
     * @param locale the locale to use for conversion
     * @return a document that allows only lower case, converting if necessary
     */
    public static final PlainDocument createLowerCaseDocument(final Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                super.insertString(offs, str.toLowerCase(locale), a);
            }
        };
    }

    /**
     * Returns a new document that converts all text to upper case.
     *
     * @param locale the locale to use for conversion
     * @return a document that allows only upper case, converting if necessary
     */
    public static final PlainDocument createUpperCaseDocument(final Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        return new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                super.insertString(offs, str.toUpperCase(locale), a);
            }
        };
    }

    /**
     * Returns a new document that accepts hex digits. Alphabetic digits are
     * converted to upper case.
     *
     * @return a document that accepts only hex digits 0-9, a-f, and A-F
     */
    public static final PlainDocument createHexDocument() {
        return new FilteredDocument("0123456789ABCDEFabcdef", true) {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                super.insertString(offs, str.toUpperCase(Locale.ENGLISH), a);
            }
        };
    }

    /**
     * Returns a document that only accepts characters that can appear in a Java
     * class name. Note that the document will not prevent the initial character
     * from being a digit.
     *
     * @return a document that accepts only Java identifier characters
     */
    public static final PlainDocument createJavaNameDocument() {
        return new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                StringBuilder buff = new StringBuilder(str.length());
                for (int i = 0; i < str.length(); ++i) {
                    char c = str.charAt(i);
                    if (Character.isJavaIdentifierPart(c)) {
                        buff.append(c);
                    }
                }
                String filtered = buff.length() == str.length() ? str : buff.toString();

                if (filtered.length() != str.length()) {
                    java.awt.Toolkit.getDefaultToolkit().beep();
                }
                if (filtered.length() > 0) {
                    super.insertString(offs, filtered, a);
                }
            }
        };
    }

    /**
     * Returns the result of filtering a string with the document's filter. As a
     * side effect, the document content will be replaced by the filtered text.
     *
     * @param text the text to filter
     * @return the text that remains after applying the filter
     */
    public String filterString(String text) {
        try {
            remove(0, getLength());
            insertString(0, text, null);
            return getText(0, getLength());
        } catch (BadLocationException ex) {
            throw new AssertionError();
        }
    }
}
