package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.File;
import java.nio.charset.Charset;
import java.util.logging.Level;
import javax.swing.Icon;
import resources.Language;

/**
 * The file types that can be edited by a {@code CodeEditor}.
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public enum CodeType {
    PLAIN(
            "txt",
            "pa-new-text",
            MetadataSource.ICON_DOCUMENT, DefaultCodeSupport.class, null
    ),
    PLAIN_UTF8(
            "utf8",
            "prj-prop-utf8",
            MetadataSource.ICON_FILE,
            DefaultCodeSupport.class,
            TextEncoding.UTF8
    ),   
    JAVASCRIPT(
            "js",
            "prj-prop-script",
            MetadataSource.ICON_SCRIPT,
            ScriptCodeSupport.class,
            TextEncoding.SOURCE_CODE
    ),
    AUTOMATION_SCRIPT(
            "ajs",
            "prj-prop-script",
            MetadataSource.ICON_AUTOMATION_SCRIPT,
            ScriptCodeSupport.class,
            TextEncoding.SOURCE_CODE
    ),     
    TYPESCRIPT(
            "ts",
            "prj-prop-typescript",
            MetadataSource.ICON_TYPESCRIPT,
            HtmlCodeSupport.class,
            TextEncoding.SOURCE_CODE
    ),
    JAVA(
            "java",
            "prj-prop-java",
            MetadataSource.ICON_JAVA,
            JavaCodeSupport.class,
            TextEncoding.SOURCE_CODE
    ),
    PROPERTIES(
            "properties",
            "prj-prop-props",
            MetadataSource.ICON_PROPERTIES,
            PropertyFileCodeSupport.class,
            TextEncoding.STRINGS
    ),
    SETTINGS(
            "settings",
            "prj-prop-txt",
            MetadataSource.ICON_SETTINGS,
            PropertyFileCodeSupport.class,
            TextEncoding.SETTINGS
    ),
    CLASS_MAP(
            "classmap",
            "prj-prop-class-map",
            MetadataSource.ICON_CLASS_MAP,
            ResourceFileCodeSupport.class,
            TextEncoding.SETTINGS
    ),
    CONVERSION_MAP(
            "conversionmap",
            "prj-prop-conversion-map",
            MetadataSource.ICON_CONVERSION_MAP,
            ResourceFileCodeSupport.class,
            TextEncoding.SETTINGS
    ),
    SILHOUETTES(
            "silhouettes",
            "prj-prop-sil",
            MetadataSource.ICON_SILHOUETTES,
            ResourceFileCodeSupport.class,
            TextEncoding.SETTINGS
    ),
    TILES(
            "tiles",
            "prj-prop-tiles",
            MetadataSource.ICON_TILE_SET,
            ResourceFileCodeSupport.class,
            TextEncoding.SETTINGS
    ),
    HTML(
            "html",
            "pa-new-html",
            MetadataSource.ICON_HTML,
            HtmlCodeSupport.class,
            TextEncoding.HTML_CSS
    ),
    CSS(
            "css",
            "prj-prop-css",
            MetadataSource.ICON_STYLE_SHEET,
            CssCodeSupport.class,
            TextEncoding.HTML_CSS
    );
    
    private final String fileExtension;
    private final String description;
    private final String defaultEncoding;
    private final Class<? extends CodeSupport> supportClass;
    private final Icon icon;
    private final boolean escapeOnSave;

    /**
     *
     * @param extension the value of extension
     * @param descKey the value of descKey
     * @param icon the value of icon
     * @param supportClass the value of supportClass
     * @param defaultEncoding the value of defaultEncoding
     */
    private CodeType(String extension, String descKey, Icon icon, Class<? extends CodeSupport> supportClass, String defaultEncoding) {
        if (extension == null) {
            throw new NullPointerException("extension");
        }
        if (defaultEncoding == null) {
            defaultEncoding = TextEncoding.UTF8;
        }

        this.fileExtension = extension;
        this.description = Language.string(descKey);
        this.supportClass = supportClass;
        this.icon = icon;
        this.defaultEncoding = defaultEncoding;
        this.escapeOnSave = !defaultEncoding.equals(TextEncoding.UTF8);
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
    
    CodeSupport createCodeSupport() {
        try {
            return supportClass.getDeclaredConstructor().newInstance();
        } catch (RuntimeException | ReflectiveOperationException ex) {
            StrangeEons.log.log(Level.WARNING, "unable to create code support for " + this.name(), ex);
        }
        return new DefaultCodeSupport();
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
