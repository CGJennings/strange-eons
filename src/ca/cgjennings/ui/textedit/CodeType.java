package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.editors.AbbreviationTableManager;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.editors.HTMLNavigator;
import ca.cgjennings.apps.arkham.editors.JavaScriptNavigator;
import ca.cgjennings.apps.arkham.editors.Navigator;
import ca.cgjennings.apps.arkham.editors.PropertyNavigator;
import ca.cgjennings.apps.arkham.editors.ResourceFileNavigator;
import ca.cgjennings.apps.arkham.editors.TileSetNavigator;
import ca.cgjennings.apps.arkham.plugins.typescript.TypeScript;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import ca.cgjennings.ui.textedit.SpellingHighlighter;
import ca.cgjennings.ui.textedit.TokenType;
import ca.cgjennings.ui.textedit.Tokenizer;
import ca.cgjennings.ui.textedit.tokenizers.CSSTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.HTMLTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.JavaScriptTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.JavaTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.PlainTextTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.PropertyTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.ResourceFileTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.TypeScriptTokenizer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.logging.Level;
import javax.swing.Icon;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import resources.Language;
import resources.Settings;

/**
 * The file types that can be edited by a {@code CodeEditor}.
 *
 *
 */
public enum CodeType {
    PLAIN(
            "txt",
            "pa-new-text",
            null,
            null,
            null,
            null,
            MetadataSource.ICON_DOCUMENT
    ),
    PLAIN_UTF8(
            "utf8",
            "prj-prop-utf8",
            TextEncoding.UTF8,
            null,
            null,
            null,
            MetadataSource.ICON_FILE
    ),   
    JAVASCRIPT(
            "js",
            "prj-prop-script",
            TextEncoding.SOURCE_CODE,
            JavaScriptTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT,
            JavaScriptNavigator.class,
            MetadataSource.ICON_SCRIPT
    ),
    AUTOMATION_SCRIPT(
            "ajs",
            "prj-prop-script",
            TextEncoding.SOURCE_CODE,
            JavaScriptTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT,
            JavaScriptNavigator.class,
            MetadataSource.ICON_AUTOMATION_SCRIPT
    ),     
    TYPESCRIPT(
            "ts",
            "prj-prop-typescript",
            TextEncoding.SOURCE_CODE,
            TypeScriptTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT,
            null,
            MetadataSource.ICON_TYPESCRIPT
    ),
    JAVA(
            "java",
            "prj-prop-java",
            TextEncoding.SOURCE_CODE,
            JavaTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_JAVA,
            null,
            MetadataSource.ICON_JAVA
    ),
    PROPERTIES(
            "properties",
            "prj-prop-props",
            TextEncoding.STRINGS,
            PropertyTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
            PropertyNavigator.class,
            MetadataSource.ICON_PROPERTIES
    ),
    SETTINGS(
            "settings",
            "prj-prop-txt",
            TextEncoding.SETTINGS,
            PropertyTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
            PropertyNavigator.class,
            MetadataSource.ICON_SETTINGS
    ),
    CLASS_MAP(
            "classmap",
            "prj-prop-class-map",
            TextEncoding.SETTINGS,
            ResourceFileTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
            ResourceFileNavigator.class,
            MetadataSource.ICON_CLASS_MAP
    ),
    CONVERSION_MAP(
            "conversionmap",
            "prj-prop-conversion-map",
            TextEncoding.SETTINGS,
            ResourceFileTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
            ResourceFileNavigator.class,
            MetadataSource.ICON_CONVERSION_MAP
    ),
    SILHOUETTES(
            "silhouettes",
            "prj-prop-sil",
            TextEncoding.SETTINGS,
            ResourceFileTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
            ResourceFileNavigator.class,
            MetadataSource.ICON_SILHOUETTES
    ),
    TILES(
            "tiles",
            "prj-prop-tiles",
            TextEncoding.SETTINGS,
            ResourceFileTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
            TileSetNavigator.class,
            MetadataSource.ICON_TILE_SET
    ),
    HTML(
            "html",
            "pa-new-html",
            TextEncoding.HTML_CSS,
            HTMLTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_HTML,
            HTMLNavigator.class,
            MetadataSource.ICON_HTML
    ),
    CSS(
            "css",
            "prj-prop-css",
            TextEncoding.HTML_CSS,
            CSSTokenizer.class,
            SyntaxConstants.SYNTAX_STYLE_CSS,
            null,
            MetadataSource.ICON_STYLE_SHEET
    );
    
    private final String fileExtension;
    private final String description;
    private final String defaultEncoding;
    private final String languageId;
    private final Class<? extends Tokenizer> tokenizer;
    private final Class<? extends Navigator> navigator;
    private final Icon icon;
    private final boolean escapeOnSave;

    /**
     * Declare a new code type.
     *
     * @param extension file extension
     * @param descKey string key for localized string that describes format
     * @param defaultEncoding default text encoding, null for UTF-8
     * @param tokenizer tokenizer to syntax highlight code, null for none
     * @param navigator navigator implementation to list important document
     * nodes, null for none
     * @param icon icon that represents the file type
     */
    private CodeType(String extension, String descKey, String defaultEncoding, Class<? extends Tokenizer> tokenizer, String languageId, Class<? extends Navigator> navigator, Icon icon) {
        if (extension == null) {
            throw new NullPointerException("extension");
        }
        if (defaultEncoding == null) {
            defaultEncoding = TextEncoding.UTF8;
        }
        this.fileExtension = extension;
        this.defaultEncoding = defaultEncoding;
        this.tokenizer = tokenizer;
        this.languageId = languageId == null ? SyntaxConstants.SYNTAX_STYLE_NONE : languageId;
        this.icon = icon;
        this.escapeOnSave = !defaultEncoding.equals(TextEncoding.UTF8);
        this.description = Language.string(descKey);
        this.navigator = navigator;
    }
    private static final CodeType[] readOnlyValues = CodeType.values();

    /**
     * Return the type of this file, based on its extension, or null.
     */
    public static CodeType forFile(File f) {
        if (f == null) {
            return null;
        }
        String ext = ProjectUtilities.getFileExtension(f);
        for (int i = 0; i < readOnlyValues.length; ++i) {
            if (readOnlyValues[i].getExtension().equals(ext)) {
                return readOnlyValues[i];
            }
        }
        return null;
    }

    /** Returns the primary file extension used to identify the code type. */
    public String getExtension() {
        return fileExtension;
    }

    /** Returns a human-friendly description of the code type. */
    public String getDescription() {
        return description;
    }

    /** Returns the name of the standard text encoding for the file type. */
    public String getEncodingName() {
        return defaultEncoding;
    }

    /** Returns the charset for the standard text encoding. */
    public Charset getEncodingCharset() {
        return Charset.forName(defaultEncoding);
    }
    
    String getLanguageIdentifier() {
        return languageId;
    }

    public Tokenizer createTokenizer() {
        try {
            if (tokenizer != null) {
                return tokenizer.getConstructor().newInstance();
            }
        } catch (Exception ex) {
            StrangeEons.log.log(Level.SEVERE, "exception while creating tokenizer", ex);
        }
        return new PlainTextTokenizer();
    }

    /** Returns a new navigator instance for the code type, if supported, or null. */
    public Navigator createNavigator(CodeEditor ed) {
        try {
            if (navigator != null) {
                Navigator nav = navigator.getConstructor().newInstance();
                nav.install(ed);
                return nav;
            }
        } catch (Exception ex) {
            StrangeEons.log.log(Level.SEVERE, "exception while creating navigator", ex);
        }
        return null;
    }

    /** Returns an icon for the code type. */
    public Icon getIcon() {
        return icon;
    }

    /** Returns whether this file type should have characters converted to and from Unicode escapes automatically. */
    public boolean getAutomaticCharacterEscaping() {
        return escapeOnSave;
    }

    /**
     * If this type generates another editable file type, returns the file name
     * that the specified file would generate. For example, for
     * {@code source.ts} this might return {@code source.js}.
     *
     * @param source the file containing source code of this type
     * @return the file that compiled code should be written to, or null if this
     * file type does not generate code
     */
    public File getDependentFile(File source) {
        if (source == null || this != TYPESCRIPT) {
            return null;
        }
        return ProjectUtilities.changeExtension(source, "js");
    }

    /**
     * Given a file of this type, if that file's contents are controlled by
     * another file that currently exists, returns that file. For example, for
     * {@code source.js} this might return {@code source.ts}.
     *
     * @param source the file that might be controlled by another file
     * @return the file that controls the content of this file, or null
     */
    public File getDeterminativeFile(File source) {
        if (source == null || this != JAVASCRIPT) {
            return null;
        }
        File tsFile = ProjectUtilities.changeExtension(source, "ts");
        if (tsFile.exists()) {
            return tsFile;
        }
        return null;
    }

    /**
     * Returns whether this code type represents runnable script code.
     */
    public boolean isRunnable() {
        return this == JAVASCRIPT || this == AUTOMATION_SCRIPT || this == TYPESCRIPT;
    }

    void initializeEditor(CodeEditor ce) {
        JSourceCodeEditor ed = ce.getEditor();
        Tokenizer t = createTokenizer();
        ed.setTokenizer(t);
        ed.setAbbreviationTable(AbbreviationTableManager.getTable(this));
        ce.setFrameIcon(icon);
//        ce.encoding = enc; // FIXME
        ce.setCharacterEscapingEnabled(escapeOnSave);
        ce.setNavigator(createNavigator(ce));
        if (t != null) {
            EnumSet<TokenType> toSpellCheck = t.getNaturalLanguageTokenTypes();
            if (toSpellCheck != null && !toSpellCheck.isEmpty()) {
                ed.addHighlighter(new SpellingHighlighter(toSpellCheck));
                SpellingHighlighter.ENABLE_SPELLING_HIGHLIGHT = Settings.getUser().getBoolean("spelling-code-enabled");
            }
        }
        if (this == TYPESCRIPT) {
            TypeScript.warmUp();
        }
    }

    /**
     * Normalizes the code type by converting variant types to their common base
     * type. If the type is a more specialized version of an existing type, then
     * this will return a simple common type. This is useful if you are
     * interested in the basic file type and do not rely on information like the
     * file extension, icon, or encoding.
     *
     * <p>
     * This method performs the following conversions:
     * <ul>
     * <li> All plain text types are converted to {@code PLAIN}.
     * <li> Automation scripts type is converted to {@code JAVASCRIPT}.
     * </ul>
     *
     * <p>
     * Note that this list could change if new code types are added in future
     * versions).
     *
     * @return the most basic code type for this type
     */
    public CodeType normalize() {
        CodeType type = this;
        switch (type) {
            case PLAIN_UTF8:
                type = CodeType.PLAIN;
                break;
            case AUTOMATION_SCRIPT:
                type = CodeType.JAVASCRIPT;
                break;
            default:
                // keep original type
        }
        return type;
    }

}
