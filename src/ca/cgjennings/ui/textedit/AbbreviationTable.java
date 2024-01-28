package ca.cgjennings.ui.textedit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * An abbreviation table stores a set of abbreviations. Abbreviations can be
 * matched against a context string, which typically consists of the last
 * several characters that were typed.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class AbbreviationTable {

    private Properties table;
    private AbbreviationTable parent;

    /**
     * Creates a new, empty abbreviation table.
     */
    public AbbreviationTable() {
        table = new Properties();
    }

    /**
     * Creates a new abbreviation table whose entries match the specified source
     * table.
     *
     * @param toCopy the table to copy entries from
     */
    public AbbreviationTable(AbbreviationTable toCopy) {
        this();
        if (toCopy == null) {
            throw new NullPointerException("toCopy");
        }
        synchronized (table) {
            for (String k : keySet()) {
                table.put(k, get(k));
            }
        }
    }

    /**
     * Adds a new abbreviation to the table.
     *
     * @param abbrev the abbreviation
     * @param expansion the text that the abbreviation expands to (is short for)
     * @throws NullPointerException if either parameter is <code>null</code>
     */
    public void put(String abbrev, String expansion) {
        if (abbrev == null) {
            throw new NullPointerException("abbrev");
        }
        if (expansion == null) {
            throw new NullPointerException("expansion");
        }
        table.setProperty(abbrev, expansion);
        maxLen = Math.max(maxLen, abbrev.length());
    }

    /**
     * Returns the expansion of the abbreviation. If an abbreviation is not
     * defined by this table, then <code>null</code> is returned unless this
     * table has a parent. If it has a parent, then the parent's expansion for
     * the abbreviation is returned as if by calling
     * <code>getParent().get( abbrev )</code>.
     *
     * @param abbrev the abbreviation to expand
     * @return the expansion, or <code>null</code> if none is defined
     * @see #put
     * @see #setParent
     */
    public String get(String abbrev) {
        String exp = table.getProperty(abbrev);
        if (exp == null && parent != null) {
            exp = parent.get(abbrev);
        }
        return exp;
    }

    /**
     * Sets the parent table for this table. If there is no expansion defined
     * for an abbreviation in this table, and the parent table is
     * non-<code>null</code>, then the expansion from the parent table (if any)
     * will be returned instead. The only methods whose return value is affected
     * by the parent are {@link #get} and {@link #getMaximumAbbreviationLength}.
     * (For example, {@link #keySet} returns only the keys immediately defined
     * in this table. It does not include the keys defined in the parent table.)
     *
     * @param parent the new parent table to set for this table
     * @see #getParent
     */
    public final void setParent(AbbreviationTable parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent table of this table, or <code>null</code> if this
     * table has no parent defined.
     *
     * @return the parent table that is consulted to expand abbreviations when
     * this table does not define an expansion for an abbreviation
     * @see #setParent
     */
    public final AbbreviationTable getParent() {
        return parent;
    }

    /**
     * Returns a set of all of the currently defined abbreviations.
     *
     * @return a set of the defined abbreviations
     */
    public Set<String> keySet() {
        return table.stringPropertyNames();
    }

    /**
     * Removes an abbreviation from the table.
     *
     * @param abbrev the abbreviation to remove
     * @throws NullPointerException if the abbreviation is <code>null</code>
     */
    public void remove(String abbrev) {
        if (abbrev == null) {
            throw new NullPointerException("abbrev");
        }
        table.remove(abbrev);
        if (abbrev.length() == maxLen) {
            updateMaxLength();
        }
    }

    /**
     * Removes all abbreviations from the table.
     */
    public void clear() {
        table.clear();
        maxLen = 0;
    }

    /**
     * Returns the number of abbreviations in the table.
     *
     * @return the number of abbreviations
     */
    public int size() {
        return table.size();
    }

    /**
     * Returns the length of the longest abbreviation in the table, or 0 if the
     * table is empty. If the table has a parent, then the larger of this
     * table's maximum abbreviation and the parent table's is returned.
     *
     * @return the longest abbreviation
     */
    public int getMaximumAbbreviationLength() {
        if (parent != null) {
            return Math.max(maxLen, parent.getMaximumAbbreviationLength());
        }
        return maxLen;
    }

    /**
     * Returns the expansion for the longest abbreviation that is a suffix of
     * the context string. Ideally, the context string has length
     * {@link #getMaximumAbbreviationLength()}, but any length string can be
     * provided.
     *
     * @param context the context for expanding the abbreviation
     * @return the expansion of the longest abbreviation <code>a</code> for
     * which <code>context.endsWith( a )</code> is true, or <code>null</code>
     *
     * @throws NullPointerException if the context string is <code>null</code>
     */
    public Expansion expandAbbreviation(String context) {
        final int contextLen = context.length();
        final int len = Math.min(contextLen, getMaximumAbbreviationLength());
        if (len == 0) {
            return null;
        }

        Expansion expansion = null;
        for (int i = len; i >= 1 && expansion == null; --i) {
            String key = context.substring(contextLen - i);
            String exp = get(key);
            if (exp != null) {
                expansion = new Expansion(key, exp);
            }
        }
        return expansion;
    }

    /**
     * Expands an abbreviation inline in the specified editor. The context for
     * the expansion will be taken from the text immediately prior to the
     * current caret position. If the context matches an abbreviation, then the
     * abbreviation will be replaced by the expansion
     * and the method will return <code>true</code>. Otherwise, the method
     * returns <code>false</code>. If the editor has a non-empty selection, this
     * method will always return false.
     *
     * @param ed the editor to expand an abbreviation in
     * @return <code>true</code> if an abbreviation was expanded
     */
    public boolean expandAbbreviation(CodeEditorBase ed) {
        if (ed.getSelectionStart() != ed.getSelectionEnd()) {
            return false;
        }

        final int end = ed.getSelectionEnd();
        final int line = ed.getLineOfOffset(end);
        final int start = ed.getLineStartOffset(line);
        final String context = ed.getText(start, end - start);
        final Expansion expansion = expandAbbreviation(context);
        if (expansion != null) {
            ed.beginCompoundEdit();
            try {
                // delete abbreviation
                String abbrev = expansion.getAbbreviation();
                ed.select(end - abbrev.length(), end);
                ed.replaceSelection(null);

                // insert expansion
                String expansionText = expansion.getExpansion();

                // determine the indentation at the start of the current line, to use
                // for the ${INDENT} token
                String indent = ed.getLineText();
                int numSpaces = 0;
                while (numSpaces < indent.length() && Character.isWhitespace(indent.charAt(numSpaces))) {
                    ++numSpaces;
                }
                indent = indent.substring(0, numSpaces).replace("\n", "");
                expansionText = expansionText.replace("${INDENT}", indent).replace("${LINE}", "\n" + indent);

                // replace deprecated requests to expand commands with empty text
                expansionText = expansionText.replaceAll("\\&\\{((\\p{javaJavaIdentifierPart}|\\-)+)\\}", "");

                // split the expansion into three parts: prefix, selection, suffix
                String prefix, selection = "", suffix = "";
                int token = expansionText.indexOf("$|");
                if (token >= 0) {
                    prefix = expansionText.substring(0, token);
                    int nekot = expansionText.lastIndexOf("|$");
                    if (nekot >= 0) {
                        selection = expansionText.substring(token + 2, nekot);
                        suffix = expansionText.substring(nekot + 2);
                    } else {
                        selection = expansionText.substring(token + 2);
                    }
                } else {
                    prefix = expansionText;
                }

                ed.replaceSelection(prefix);
                int selStart = ed.getSelectionStart();
                ed.replaceSelection(selection);
                int selEnd = ed.getSelectionStart();
                ed.replaceSelection(suffix);
                ed.select(selStart, selEnd);
            } finally {
                ed.endCompoundEdit();
            }
            return true;
        }
        return false;
    }

    private static void insertExpansion(CodeEditorBase ed, String expansionText) {

    }

    /**
     * Expands an abbreviation inline in a Swing text component. The context for
     * the expansion will be taken from the text immediately prior to the
     * current caret position. If the context matches an abbreviation, then the
     * abbreviation will be replaced by the expansion and the method will return
     * <code>true</code>. Otherwise, the method returns <code>false</code>. If
     * the editor has a non-empty selection, this method will have no effect.
     *
     * @param ed the editor to expand an abbreviation in
     * @param suffixToIgnore if this is not equal to '\0' and it is the last
     * character before the caret, it is ignored (it will not form part of the
     * expansion context)
     * @return <code>true</code> if an abbreviation was expanded
     */
    public boolean expandAbbreviation(JTextComponent ed, char suffixToIgnore) {
        if (ed.getSelectionStart() != ed.getSelectionEnd()) {
            return false;
        }
        final Document doc = ed.getDocument();
        int end = ed.getSelectionEnd();
        int start = end - (getMaximumAbbreviationLength() + (suffixToIgnore == '\0' ? 0 : 1));
        if (start < 0) {
            start = 0;
        }
        int len = end - start;
        len = Math.max(0, Math.min(len, doc.getLength() - start));
        try {
            int contextAdjustment = 0;
            String context = ed.getText(start, len);
            if (!context.isEmpty() && suffixToIgnore != '\0' && context.charAt(context.length() - 1) == suffixToIgnore) {
                context = context.substring(0, context.length() - 1);
                contextAdjustment = -1;
            }
            final Expansion expansion = expandAbbreviation(context);
            if (expansion != null) {
                // select abbreviation to prepare to delete it
                String abbrev = expansion.getAbbreviation();
                start = end - abbrev.length() + contextAdjustment;
                ed.select(start, end);

                // insert expansion
                final String exp = expansion.getExpansion();

                int selStart = exp.indexOf("$|");
                int selEnd = exp.lastIndexOf("|$");
                if (selStart >= 0 && selEnd >= 0 && selStart < selEnd) {
                    // get the expansion but leave out the $| |$ markers
                    String repText = exp.substring(0, selStart) + exp.substring(selStart + 2, selEnd) + exp.substring(selEnd + 2);
                    ed.replaceSelection(repText);
                    ed.select(start + selStart, start + selEnd - 2); // -2 to account for skipped $|
                } else {
                    ed.replaceSelection(exp);
                }
                return true;
            }
        } catch (BadLocationException ble) {
            throw new AssertionError(ble);
        }

        return false;
    }

    /**
     * Loads an abbreviation table from an input stream.
     *
     * @param in the stream to read from
     * @throws IOException if an I/O occurs
     */
    public void load(InputStream in) throws IOException {
        table.load(in);
        updateMaxLength();
    }

    /**
     * Loads an abbreviation table from a reader.
     *
     * @param in the reader to read from
     * @throws IOException if an I/O occurs
     */
    public void load(Reader in) throws IOException {
        table.load(in);
        updateMaxLength();
    }

    /**
     * Loads an abbreviation table from a URL.
     *
     * @param url the URL to read from
     * @throws IOException if an I/O occurs
     */
    public void load(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            load(in);
        }
    }

    /**
     * Loads an abbreviation table from a file.
     *
     * @param f the file to read from
     * @throws IOException if an I/O occurs
     */
    public void load(File f) throws IOException {
        try {
            load(f.toURI().toURL());
        } catch (MalformedURLException e) {
        }
    }

    /**
     * Writes an abbreviation table to an output stream.
     *
     * @param out the stream to write to
     * @throws IOException if an I/O occurs
     */
    public void store(OutputStream out) throws IOException {
        table.store(out, "Abbreviation Table");
    }

    /**
     * Writes an abbreviation table to a stream writer.
     *
     * @param out the writer to write to
     * @throws IOException if an I/O error occurs
     */
    public void store(Writer out) throws IOException {
        table.store(out, "Abbreviation Table");
    }

    /**
     * Writes an abbreviation table to a file.
     *
     * @param out the file to write to
     * @throws IOException if an I/O error occurs
     */
    public void store(File out) throws IOException {
        try (FileOutputStream outs = new FileOutputStream(out)) {
            store(outs);
        }
    }

    private void updateMaxLength() {
        int max = 0;
        for (String v : table.stringPropertyNames()) {
            max = Math.max(max, v.length());
        }
        maxLen = max;
    }

    private int maxLen;

    /**
     * Encapsulates an expansion result from
     * {@link AbbreviationTable#expandAbbreviation(java.lang.String)}.
     */
    public static class Expansion {

        private String abbrev;
        private String exp;

        private Expansion(String abbrev, String exp) {
            this.abbrev = abbrev;
            this.exp = exp;
        }

        /**
         * Returns the abbreviation that triggered the result.
         *
         * @return the matching abbreviation
         */
        public String getAbbreviation() {
            return abbrev;
        }

        /**
         * Returns the expansion associated with the abbreviation.
         *
         * @return the expansion for the abbreviation
         */
        public String getExpansion() {
            return exp;
        }

        /**
         * Returns a string describing the expansion; useful for debugging.
         *
         * @return a string describing this expansion result
         */
        @Override
        public String toString() {
            return "Expansion{" + abbrev + '=' + exp + '}';
        }
    }
}
