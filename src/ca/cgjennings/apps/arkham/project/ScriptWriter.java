package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.io.EscapedTextCodec;
import ca.cgjennings.text.SETemplateProcessor;
import ca.cgjennings.util.SortedProperties;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Properties;

/**
 * A tool to assist in the automated generation of scripts and other text files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @Deprecated Use {@link SETemplateProcessor}.
 */
@Deprecated
class ScriptWriter {

    private StringBuilder libs;
    private StringBuilder body;
    private HashMap<String, String> vars = new HashMap<>();

    private boolean localized;
    private SortedProperties ui, game;

    public ScriptWriter() {
        clear();
        vars = new HashMap<>();
    }

    /**
     * Creates a new script writer that shares variables and localization data
     * with a parent writer. Used to create a separate script or data file that
     * is called by the primary script. You must already have called
     * {@link #configureLocalization} on the primary writer for this to work
     * correctly.
     *
     * @param primary
     */
    public ScriptWriter(ScriptWriter primary) {
        clear();
        vars = primary.vars;
        localized = primary.localized;
        ui = primary.ui;
        game = primary.game;
    }

    public void configureLocalization(boolean localize) {
        localized = localize;
        if (localize) {
            ui = new SortedProperties();
            game = new SortedProperties();
        }
    }

    public String localizeAtString(String key, String text) {
        if (localized) {
            key = replaceVariables("${PREFIX}" + key);
            ui.put(key, text);
            return "@" + key;
        } else {
            return text;
        }
    }

    public String localize(String key, String text) {
        return localize(key, text, true);
    }

    public String localize(String key, String text, boolean isUI) {
        text = replaceVariables(text);
        if (!localized) {
            return "\"" + text + "\"";
        }

        key = replaceVariables("${PREFIX}" + key);
        if (isUI) {
            ui.put(key, text);
        } else {
            game.put(key, text);
        }

        return "string( \"" + key + "\" )";
    }

    public void writeResourceBundles(File baseFile, boolean addTranslations) throws IOException {
        if (!localized) {
            return;
        }
        writeBundleImpl(baseFile, "-game.properties", game, addTranslations);
        writeBundleImpl(baseFile, "-ui.properties", ui, addTranslations);
    }

    private void writeBundleImpl(File base, String suffix, final Properties data, boolean addTranslations) throws IOException {
        final File f = new File(base, replaceVariables("${SCRIPTNAME}") + suffix);
        try (FileOutputStream out = new FileOutputStream(f)) {
            data.store(out, "");
        }
        if (addTranslations) {
            EventQueue.invokeLater(() -> {
                AddLocaleDialog atd = new AddLocaleDialog(f);
                atd.setLocationRelativeTo(StrangeEons.getWindow());
                atd.setVisible(true);
            });
        }
    }

    /**
     * Adds one or more named variables to the variable database. When text is
     * added to the script, any text that matches the name of a variable
     * surrounded with <tt>${</tt> and <tt>}</tt>
     * will be replaced by the most recently value set here.
     *
     * @param keysAndValues a sequence of variable name and replacement value
     * pairs
     */
    public void setVariables(String... keysAndValues) {
        for (int i = 0; i < keysAndValues.length; i += 2) {
            vars.put(keysAndValues[i], keysAndValues[i + 1]);
        }
    }

    /**
     * Performs variable replacement on a source string, returning a string with
     * all <tt>${variables}</tt> replaced by their values.
     *
     * @param source the source text to perform repalcements on
     * @return the script with variable sequences replaced by their values
     */
    protected String replaceVariables(String source) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        for (String key : vars.keySet()) {
            String value = vars.get(key);
            if (value != null) {
                source = source.replace("${" + key + "}", value);
            }
        }
        return source;
    }

    /**
     * Adds a statement to the top of the script to include this library.
     *
     * @param name
     */
    public void useLibrary(String name) {
        libs.append("useLibrary( \"").append(replaceVariables(name)).append("\" );\n");
    }

    /**
     * Adds code to the script body.
     *
     * @param source
     */
    public void append(String source) {
        body.append(replaceVariables(source));
    }

    /**
     * Adds code from a source fragment resource. This is a file in
     * <tt>resources/projects</tt> that contains a fragment of code to be
     * appended.
     *
     * @param name the resource file that contains the fragment
     */
    public void appendFragment(String name) throws IOException {
        String frag = ProjectUtilities.getFragment(name);
        if (frag == null) {
            throw new FileNotFoundException(name);
        }
        append(frag);
    }

    /**
     * Adds a fragment from an arbitrary file.
     */
    public void appendFragment(File file) throws IOException {
        append(ProjectUtilities.getFileAsString(file, ProjectUtilities.ENC_UTF8));
    }

    /**
     * Returns the current script.
     *
     * @return
     */
    public String getScript() {
        if (libs.length() > 0) {
            return libs.toString() + "\n" + body.toString();
        }
        return body.toString();
    }

    /**
     * Writes the current script to a file.
     *
     * @param file
     * @throws IOException
     */
    public void writeScript(File file) throws IOException {
        ProjectUtilities.copyReader(new StringReader(getScript()), file, ProjectUtilities.ENC_SCRIPT);
    }

    public void writeScript(File file, String encoding) throws IOException {
        String text = getScript();
        if (!encoding.equals(ProjectUtilities.ENC_UTF8)) {
            text = EscapedTextCodec.escapeUnicode(text);
        }
        ProjectUtilities.copyReader(new StringReader(text), file, encoding);
    }

    /**
     * Clears the current script, but retains variable definitions.
     */
    public void clear() {
        libs = new StringBuilder(256);
        body = new StringBuilder(1_024);
    }
}
