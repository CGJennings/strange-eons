package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.apps.arkham.AbstractSupportEditor;
import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.apps.arkham.MarkupTargetFactory;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import ca.cgjennings.apps.arkham.plugins.typescript.TypeScript;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.i18n.PatternExceptionLocalizer;
import ca.cgjennings.io.EscapedTextCodec;
import ca.cgjennings.math.Interpolation;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.anim.Animation;
import ca.cgjennings.ui.dnd.FileDrop;
import ca.cgjennings.ui.text.ErrorSquigglePainter;
import ca.cgjennings.ui.textedit.CSSStyler;
import ca.cgjennings.ui.textedit.EditorCommands;
import ca.cgjennings.ui.textedit.InputHandler;
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
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.print.PrintException;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.JTextComponent;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * Support editors are used to edit content other than game components.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class CodeEditor extends AbstractSupportEditor {

    /**
     * Creates a code editor with no file, encoding, or file type attached. Note
     * that code editors are designed to edit files within a project, and
     * therefore expect {@link #getFile()} to return a non-{@code null}
     * value.
     */
    private CodeEditor() {
        initComponents();
        editor.putClientProperty(ContextBar.BAR_LEADING_SIDE_PROPERTY, Boolean.TRUE);

        sideBarPanel.setVisible(false);
        findPanel.setVisible(false);
        // set Find checkbox options
        findPanelShown(null);

        MarkupTargetFactory.enableTargeting(findField, false);
        MarkupTargetFactory.enableTargeting(replaceField, false);

        editor.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                setUnsavedChanges(true);
                navOutOfDate = true;
            }
        });
        editor.addCaretListener(new CaretListener() {
            private int lastLine;

            @Override
            public void caretUpdate(CaretEvent e) {
                if (navOutOfDate && navigator != null) {
                    EventQueue.invokeLater(() -> {
                        int line = editor.getCaretLine();
                        if (lastLine != line) {
                            lastLine = line;
                            refreshNavigator();
                        }
                    });
                }
            }
        });

        navList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && value instanceof NavigationPoint) {
                    NavigationPoint np = (NavigationPoint) value;
                    setIcon(np.getIcon());
                    setToolTipText(np.getLongDescription());
                    setBorder(
                            BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(0, np.scope * 12, 0, 0))
                    );
                }
                return this;
            }
        });

        getEditor().setFileDropEnabled(true);
        getEditor().addPropertyChangeListener(JSourceCodeEditor.FILE_DROP_PROPERTY, (PropertyChangeEvent evt) -> {
            List list = (List) evt.getNewValue();
            File[] files = new File[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                files[i] = (File) list.get(i);
            }
            if (fileDropListener == null) {
                doDefaultFileDrop(files);
            } else {
                fileDropListener.filesDropped(files);
            }
        });

        editor.putClientProperty(ContextBar.BAR_INSIDE_PROPERTY, true);
        editor.addCaretListener(new CaretListener() {
            private boolean wasOutside = true;
            private boolean wasCollapsed = ContextBar.getShared().isCollapsed();

            @Override
            public void caretUpdate(CaretEvent e) {
                boolean isOutside = (editor.getLineOfOffset(e.getDot()) - editor.getFirstDisplayedLine()) >= 2;
                if (isOutside != wasOutside) {
                    ContextBar cb = ContextBar.getShared();
                    if (isOutside) {
                        cb.setCollapsed(wasCollapsed);
                    } else {
                        wasCollapsed = cb.isCollapsed();
                        cb.setCollapsed(true);
                    }
                }
                wasOutside = isOutside;
            }
        });

        installStrangeEonsEditorCommands(editor);
        createTimer((int) NAVIGATOR_SCAN_DELAY);

        EventQueue.invokeLater(editor::requestFocusInWindow);
    }

    /**
     * Creates a new code editor for the requested file and file type. The
     * default encoding for the file type will be used to read and write the
     * file.
     *
     * @param file
     * @param codeType
     * @throws IOException
     */
    public CodeEditor(File file, CodeType codeType) throws IOException {
        this(file, codeType.getEncodingName(), codeType);
    }

    /**
     * Creates a new code editor for the requested file, encoding, and file
     * type.
     *
     * @param file
     * @param encoding
     * @param codeType
     * @throws IOException
     */
    public CodeEditor(File file, Charset encoding, CodeType codeType) throws IOException {
        this(file, encoding.name(), codeType);
    }

    /**
     * Creates a new code editor for the requested file, encoding, and file
     * type.
     *
     * @param file
     * @param encoding
     * @param codeType
     * @throws IOException
     */
    public CodeEditor(File file, String encoding, CodeType codeType) throws IOException {
        this();
        type = codeType;
        codeType.initializeEditor(this);
        setFile(file);
        this.encoding = encoding;

        readFile();

        editor.select(0, 0);
        editor.getDocument().clearUndoHistory();
        setUnsavedChanges(false);

        editor.setComponentPopupMenu(createPopupMenu());
    }

    /**
     * Creates a new code editor that displays text but does not allow it to be
     * edited unless the user creates a copy using Save As. The default encoding
     * for the content type will be used to read and write the file.
     *
     * @param text the text to display in the editor
     * @param codeType the content type of the text
     */
    public CodeEditor(String text, CodeType codeType) {
        this(text, codeType.getEncodingName(), codeType);
    }

    /**
     * Creates a new code editor that displays text but does not allow it to be
     * edited unless the user creates a copy using Save As.
     *
     * @param text the text to display in the editor
     * @param encoding the name of the character set to use when reading or
     * writing the file
     * @param codeType the content type of the text
     */
    public CodeEditor(String text, String encoding, CodeType codeType) {
        this();
        codeType.initializeEditor(this);
        this.encoding = encoding;
        type = codeType;

        editor.setText(text);
        editor.select(0, 0);
        editor.getDocument().clearUndoHistory();
        setUnsavedChanges(false);

        editor.setComponentPopupMenu(createPopupMenu());
        setReadOnly(true);
    }

    public void setReadOnly(boolean readOnly) {
        if (readOnly == editor.isEditable()) {
            editor.setEditable(!readOnly);
            Icon i = type.getIcon();
            if (readOnly) {
                i = ImageUtilities.createDisabledIcon(i);
            }
            setFrameIcon(i);
        }
    }

    public boolean isReadOnly() {
        return editor.isEditable();
    }

    /**
     * Detects a UTF-8/UTF-16/UTF-32 BOM sequence at the start of a text file.
     * If one exists, returns the {@code Charset} indicated by the
     * sequence. Otherwise, returns {@code null}.
     *
     * @param f the file to check
     * @return the encoding represented by the byte order mark, or
     * {@code null}
     */
    public static Charset checkFileForBOM(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            int b0 = in.read();
            switch (b0) {
                case 0xEE:
                    if (in.read() == 0xBB && in.read() == 0xBF) {
                        return StandardCharsets.UTF_8;
                    }   break;
                case 0xFE:
                    if (in.read() == 0xFF) {
                        return StandardCharsets.UTF_16BE;
                    }   break;
                case 0xFF:
                    if (in.read() == 0xFE) {
                        if (in.read() == 0x00) {
                            if (in.read() == 0x00) {
                                return Charset.forName("UTF-32LE");
                            }
                        } else {
                            return StandardCharsets.UTF_16LE;
                        }
                    }   break;
                case 0x00:
                    if (in.read() == 0x00 && in.read() == 0xFE && in.read() == 0xFF) {
                        return Charset.forName("UTF-32BE");
                    }   break;
                default:
                    break;
            }
        }
        return null;
    }

    /**
     * Reload the file, replacing the edited text.
     */
    public void refresh() throws IOException {
        if (getFile() == null) {
            return;
        }
        JSourceCodeEditor ed = getEditor();
        int line = ed.getLineOfOffset(ed.getCaretPosition());

        readFile();

        int offset = ed.getLineStartOffset(line);
        if (offset < 0) {
            offset = ed.getDocumentLength();
        }
        ed.select(offset, offset);
        ed.scrollToCaret();
    }

    private void readFile() throws IOException {
        String readEncoding = encoding;
        Charset bom = checkFileForBOM(getFile());
        if (bom != null) {
            readEncoding = bom.name();
        }

        String text = ProjectUtilities.getFileText(getFile(), readEncoding);

        // if the file starts with a BOM, strip it off
        if (!text.isEmpty() && text.charAt(0) == '\ufeff') {
            text = text.substring(1);
        }

        text = unescape(text);
        editor.setText(text);
        refreshNavigator(text);
        setUnsavedChanges(false);
    }

    /**
     * The file types that can be edited by a {@code CodeEditor}.
     */
    public static enum CodeType {
        PLAIN("txt", "pa-new-text", null, null, null, MetadataSource.ICON_DOCUMENT, false),
        JAVASCRIPT("js", "prj-prop-script", TextEncoding.SOURCE_CODE, JavaScriptTokenizer.class, JavaScriptNavigator.class, MetadataSource.ICON_SCRIPT, false),
        TYPESCRIPT("ts", "prj-prop-typescript", TextEncoding.SOURCE_CODE, TypeScriptTokenizer.class, null, MetadataSource.ICON_TYPESCRIPT, false),
        JAVA("java", "prj-prop-java", TextEncoding.SOURCE_CODE, JavaTokenizer.class, null, MetadataSource.ICON_JAVA, true),
        PROPERTIES("properties", "prj-prop-props", TextEncoding.STRINGS, PropertyTokenizer.class, PropertyNavigator.class, MetadataSource.ICON_PROPERTIES, true),
        SETTINGS("settings", "prj-prop-txt", TextEncoding.SETTINGS, PropertyTokenizer.class, PropertyNavigator.class, MetadataSource.ICON_SETTINGS, true),
        CLASS_MAP("classmap", "prj-prop-class-map", TextEncoding.SETTINGS, ResourceFileTokenizer.class, ResourceFileNavigator.class, MetadataSource.ICON_CLASS_MAP, true),
        CONVERSION_MAP("conversionmap", "prj-prop-conversion-map", TextEncoding.SETTINGS, ResourceFileTokenizer.class, ResourceFileNavigator.class, MetadataSource.ICON_CONVERSION_MAP, true),
        SILHOUETTES("silhouettes", "prj-prop-sil", TextEncoding.SETTINGS, ResourceFileTokenizer.class, ResourceFileNavigator.class, MetadataSource.ICON_SILHOUETTES, true),
        TILES("tiles", "prj-prop-tiles", TextEncoding.SETTINGS, ResourceFileTokenizer.class, TileSetNavigator.class, MetadataSource.ICON_TILE_SET, true),
        HTML("html", "pa-new-html", TextEncoding.HTML_CSS, HTMLTokenizer.class, HTMLNavigator.class, MetadataSource.ICON_HTML, false),
        CSS("css", "prj-prop-css", TextEncoding.HTML_CSS, CSSTokenizer.class, null, MetadataSource.ICON_STYLE_SHEET, false),
        PLAIN_UTF8("utf8", "prj-prop-utf8", TextEncoding.UTF8, null, null, MetadataSource.ICON_FILE, false),
        AUTOMATION_SCRIPT("ajs", "prj-prop-script", TextEncoding.SOURCE_CODE, JavaScriptTokenizer.class, JavaScriptNavigator.class, MetadataSource.ICON_AUTOMATION_SCRIPT, true),
        ;

        private String enc;
        private Class<? extends Tokenizer> tokenizer;
        private Class<? extends Navigator> navigator;
        private Icon icon;
        private boolean escapeOnSave;
        private String ext, description;

        private CodeType(
                String extension, String descKey, String defaultEncoding,
                Class<? extends Tokenizer> tokenizer, Class<? extends Navigator> navigator,
                Icon icon, boolean escapeOnSave
        ) {
            if (extension == null) {
                throw new NullPointerException("extension");
            }
            if (defaultEncoding == null) {
                defaultEncoding = ProjectUtilities.ENC_UTF8;
            }
            this.ext = extension;
            this.enc = defaultEncoding;
            this.tokenizer = tokenizer;
            this.icon = icon;
            this.escapeOnSave = escapeOnSave;
            this.description = string(descKey);
            this.navigator = navigator;
        }

        public String getExtension() {
            return ext;
        }

        public String getDescription() {
            return description;
        }

        public String getEncodingName() {
            return enc;
        }

        public Charset getEncodingCharset() {
            return Charset.forName(enc);
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

        public Icon getIcon() {
            return icon;
        }

        public boolean getAutomaticCharacterEscaping() {
            return escapeOnSave;
        }

        /**
         * If this file type should be processed automatically after writing
         * it, perform that processing.
         */
        public boolean processAfterWrite(File source, String text) {
            if (source == null) return false;

            if (this == TYPESCRIPT) {
                ca.cgjennings.apps.arkham.plugins.typescript.TypeScript.transpile(text, transpiled -> {
                    final File js = this.getDependentFile(source);
                    try {
                        ProjectUtilities.writeTextFile(js, transpiled, ProjectUtilities.ENC_SCRIPT);
                    } catch(IOException ex) {
                        StrangeEons.log.log(Level.SEVERE, "failed to write transpiled file", ex);
                    }
                    refreshDependentFiles(this, js);
                });
                return false;
            }

            return true;
        }

        /**
         * If this type generates another editable file type, returns the file
         * name that the specified file would generate. For example, for
         * {@code source.ts} this might return {@code source.js}.
         *
         * @param source the file containing source code of this type
         * @return the file that compiled code should be written to, or null
         * if this file type does not generate code
         */
        public File getDependentFile(File source) {
            if (source == null || this != TYPESCRIPT) return null;
            return ProjectUtilities.changeExtension(source, "js");
        }

        /**
         * Given a file of this type, if that file's contents are controlled
         * by another file that currently exists, returns that file.  For example, for
         * {@code source.js} this might return {@code source.ts}.
         *
         * @param source the file that might be controlled by another file
         * @return the file that controls the content of this file, or null
         */
        public File getDeterminativeFile(File source) {
            if (source == null || this != JAVASCRIPT) return null;

            File tsFile = ProjectUtilities.changeExtension(source, "ts");
            if (tsFile.exists()) return tsFile;
            return null;
        }

        private void initializeEditor(CodeEditor ce) {
            JSourceCodeEditor ed = ce.getEditor();
            Tokenizer t = createTokenizer();
            ed.setTokenizer(t);
            ed.setAbbreviationTable(AbbreviationTableManager.getTable(this));
            ce.setFrameIcon(icon);
            ce.encoding = enc;
            ce.setCharacterEscapingEnabled(escapeOnSave);
            ce.setNavigator(createNavigator(ce));

            if (t != null) {
                EnumSet<TokenType> toSpellCheck = t.getNaturalLanguageTokenTypes();
                if (toSpellCheck != null && !toSpellCheck.isEmpty()) {
                    ed.addHighlighter(new SpellingHighlighter(toSpellCheck));
                }
            }

            if (this == TYPESCRIPT) {
                TypeScript.warmUp();
            }
        }

        /**
         * Normalizes the code type by converting variant types to their common
         * base type. If the type is a more specialized version of an existing
         * type, then this will return a simple common type. This is useful if
         * you are interested in the basic file type and do not rely on
         * information like the file extension, icon, or encoding. In
         * particular, it is guaranteed that the tokenizer for the returned type
         * will match the tokenizer of the original type.
         *
         * <p>
         * This method performs the following conversions:
         * <ul>
         * <li> All plain text types are converted to {@code PLAIN}.
         * <li> All script types are converted to {@code JAVASCRIPT}.
         * </ul>
         *
         * <p>
         * Note that this list could change if new code types are added in
         * future versions):
         *
         * @return the most basic code type with the same tokenizer as this type
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

    private String encoding = "utf-8";
    private CodeType type = CodeType.PLAIN;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        findPanel = new javax.swing.JPanel();
        titlePanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        closeBtn = new ca.cgjennings.apps.arkham.ToolCloseButton();
        searchControlsPanel = new javax.swing.JPanel();
        icon = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        findField = new javax.swing.JTextField();
        replaceField = new javax.swing.JTextField();
        findPrevBtn = new javax.swing.JButton();
        findNextBtn = new javax.swing.JButton();
        replaceAllBtn = new javax.swing.JButton();
        replaceBtn = new javax.swing.JButton();
        incrementalCheck = new javax.swing.JCheckBox();
        caseSensCheck = new javax.swing.JCheckBox();
        regExpCheck = new javax.swing.JCheckBox();
        regExpErrorLabel = new javax.swing.JLabel();
        sideBarSplitter = new javax.swing.JSplitPane();
        editor = new ca.cgjennings.ui.textedit.JSourceCodeEditor();
        sideBarPanel = new javax.swing.JPanel();
        navPanel = new javax.swing.JPanel();
        navScroll = new javax.swing.JScrollPane();
        navList = new javax.swing.JList();
        navTitle = new javax.swing.JPanel();
        javax.swing.JLabel navLabel = new javax.swing.JLabel();
        ca.cgjennings.apps.arkham.ToolCloseButton sourceNavCloseButton = new ca.cgjennings.apps.arkham.ToolCloseButton();

        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        findPanel.setLayout(new java.awt.BorderLayout());

        titlePanel.setLayout(new java.awt.GridBagLayout());

        jLabel1.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel1.setForeground(UIManager.getColor(Theme.PROJECT_HEADER_FOREGROUND));
        jLabel1.setText(string( "find-replace" )); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, new java.awt.Color(99, 99, 99)), javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        jLabel1.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        titlePanel.add(jLabel1, gridBagConstraints);

        closeBtn.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeBtncloseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.ipady = 1;
        titlePanel.add(closeBtn, gridBagConstraints);

        findPanel.add(titlePanel, java.awt.BorderLayout.NORTH);

        searchControlsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                findPanelShown(evt);
            }
        });

        icon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        icon.setIcon( ResourceKit.getIcon( "ui/find-lr.png" ) );
        icon.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 0));
        icon.setIconTextGap(0);

        jLabel2.setText(string( "find" )); // NOI18N

        jLabel3.setText(string( "replace" )); // NOI18N

        findField.setColumns(12);
        findField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findNextActionPerformed(evt);
            }
        });

        replaceField.setColumns(12);
        replaceField.setText(Settings.getUser().get(KEY_LAST_REPLACE));
        replaceField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceActionPerformed(evt);
            }
        });

        findPrevBtn.setText(string( "find-previous" )); // NOI18N
        findPrevBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findPrevActionPerformed(evt);
            }
        });

        findNextBtn.setText(string( "find-next" )); // NOI18N
        findNextBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findNextActionPerformed(evt);
            }
        });

        replaceAllBtn.setText(string( "replace-all" )); // NOI18N
        replaceAllBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceAllActionPerformed(evt);
            }
        });

        replaceBtn.setText(string( "replace" )); // NOI18N
        replaceBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceActionPerformed(evt);
            }
        });

        incrementalCheck.setFont(incrementalCheck.getFont().deriveFont(incrementalCheck.getFont().getSize()-1f));
        incrementalCheck.setSelected(true);
        incrementalCheck.setText(string( "find-incremental" )); // NOI18N
        incrementalCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                storeFindCheckStates(evt);
            }
        });

        caseSensCheck.setFont(caseSensCheck.getFont().deriveFont(caseSensCheck.getFont().getSize()-1f));
        caseSensCheck.setText(string( "find-case-sense" )); // NOI18N
        caseSensCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                storeFindCheckStates(evt);
            }
        });

        regExpCheck.setFont(regExpCheck.getFont().deriveFont(regExpCheck.getFont().getSize()-1f));
        regExpCheck.setText(string( "find-reg-exp" )); // NOI18N
        regExpCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                storeFindCheckStates(evt);
            }
        });

        regExpErrorLabel.setFont(regExpErrorLabel.getFont().deriveFont(regExpErrorLabel.getFont().getSize()-1f));
        regExpErrorLabel.setForeground(java.awt.Color.red);
        regExpErrorLabel.setText(" ");

        javax.swing.GroupLayout searchControlsPanelLayout = new javax.swing.GroupLayout(searchControlsPanel);
        searchControlsPanel.setLayout(searchControlsPanelLayout);
        searchControlsPanelLayout.setHorizontalGroup(
            searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchControlsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(icon)
                .addGap(18, 18, 18)
                .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchControlsPanelLayout.createSequentialGroup()
                        .addComponent(incrementalCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(caseSensCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(regExpCheck))
                    .addGroup(searchControlsPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(regExpErrorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 617, Short.MAX_VALUE)
                            .addGroup(searchControlsPanelLayout.createSequentialGroup()
                                .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(findField, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                                    .addGroup(searchControlsPanelLayout.createSequentialGroup()
                                        .addComponent(findNextBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(findPrevBtn)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(searchControlsPanelLayout.createSequentialGroup()
                                        .addComponent(replaceBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(replaceAllBtn))
                                    .addComponent(replaceField, javax.swing.GroupLayout.DEFAULT_SIZE, 282, Short.MAX_VALUE))
                                .addGap(8, 8, 8)))))
                .addContainerGap())
        );

        searchControlsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {findNextBtn, findPrevBtn});

        searchControlsPanelLayout.setVerticalGroup(
            searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchControlsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(icon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(searchControlsPanelLayout.createSequentialGroup()
                        .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(findField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(replaceField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(findNextBtn)
                            .addComponent(findPrevBtn)
                            .addComponent(replaceBtn)
                            .addComponent(replaceAllBtn))
                        .addGap(5, 5, 5)
                        .addComponent(regExpErrorLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(incrementalCheck)
                            .addComponent(caseSensCheck)
                            .addComponent(regExpCheck))))
                .addContainerGap())
        );

        KeyListener findListener = new KeyAdapter() {
            @Override
            public void keyTyped( final KeyEvent e ) {
                EventQueue.invokeLater( new Runnable() {
                    @Override
                    public void run() {
                        ca.cgjennings.ui.textedit.JSourceCodeEditor editor = getEditor();
                        if( e.getKeyChar() == 27 ) { // Escape
                            editor.requestFocusInWindow();
                            findPanel.setVisible( false );
                            return;
                        }
                        if( e.getSource() == findField ) {
                            // do syntax checking if in regexp mode
                            if( regExpCheck.isSelected() ) createPattern();
                            // do incremental searches
                            if( !incrementalCheck.isSelected() ) return;
                            if( e.getKeyChar() != '\n' ) {
                                if( findField.getText().length() > 0 ) {
                                    findNextActionPerformed( null );
                                } else {
                                    int sel = Math.min( editor.getSelectionStart(), editor.getSelectionEnd() );
                                    editor.select( sel, sel );
                                }
                            }
                        }
                    }
                });
            }
        };
        findField.addKeyListener( findListener );
        replaceField.addKeyListener( findListener );

        findPanel.add(searchControlsPanel, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(findPanel, gridBagConstraints);

        sideBarSplitter.setDividerLocation(200);
        sideBarSplitter.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                sideBarSplitterPropertyChange(evt);
            }
        });
        sideBarSplitter.setRightComponent(editor);

        sideBarPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sideBarPanel.setLayout(new java.awt.BorderLayout());

        navPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        navPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        navPanel.setLayout(new java.awt.BorderLayout());

        navScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        navScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setMinimumSize(new java.awt.Dimension(0, 0));

        navList.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        navList.setFont(navList.getFont().deriveFont(navList.getFont().getSize()-1f));
        navList.setModel( navModel );
        navList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        navList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                navListMouseEntered(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                navListMousePressed(evt);
            }
        });
        navList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                navListValueChanged(evt);
            }
        });
        navScroll.setViewportView(navList);

        navPanel.add(navScroll, java.awt.BorderLayout.CENTER);

        navTitle.setOpaque(false);
        navTitle.setLayout(new java.awt.GridBagLayout());

        navLabel.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        navLabel.setFont(navLabel.getFont().deriveFont(navLabel.getFont().getStyle() | java.awt.Font.BOLD, navLabel.getFont().getSize()-2));
        navLabel.setForeground(UIManager.getColor(Theme.PROJECT_HEADER_FOREGROUND));
        navLabel.setText(string( "code-navigator" )); // NOI18N
        navLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
        navLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        navTitle.add(navLabel, gridBagConstraints);

        sourceNavCloseButton.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        sourceNavCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sourceNavCloseButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        navTitle.add(sourceNavCloseButton, gridBagConstraints);

        navPanel.add(navTitle, java.awt.BorderLayout.NORTH);

        sideBarPanel.add(navPanel, java.awt.BorderLayout.CENTER);

        sideBarSplitter.setLeftComponent(sideBarPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(sideBarSplitter, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void setFileDropListener(FileDrop.Listener fdl) {
        fileDropListener = fdl;
    }

    public FileDrop.Listener getFileDropListener() {
        return fileDropListener;
    }

    private FileDrop.Listener fileDropListener;

    private void doDefaultFileDrop(File[] files) {
        final String sep = System.getProperty("file.separator");
        final String resources = "resources" + sep;
        final JSourceCodeEditor ed = getEditor();

        File file = getFile();
        boolean quote = getFileNameExtension().equals("js")
                || getFileNameExtension().equals("html") || getFileNameExtension().equals("css");
        boolean doubleQuote = getFileNameExtension().equals("java");

        ed.getDocument().beginCompoundEdit();
        try {
            for (File f : files) {
                String insert = null;
                String name = f.getPath();

                for (int off = 0; off < name.length();) {
                    int resPos = name.indexOf(resources, off);
                    if (resPos >= 0) {
                        // matched pos != "../resources/.." so look again
                        if (resPos > 0 && name.charAt(resPos - 1) != sep.charAt(0)) {
                            off = resPos + resources.length();
                            continue;
                        }
                        insert = name.substring(resPos + resources.length());
                    }
                    break;
                }

                if (insert == null) {
                    if (file != null) {
                        File relative = ProjectUtilities.makeFileRelativeTo(file, f);
                        insert = relative.getPath();
                    } else {
                        insert = f.getPath();
                    }
                }

                if (insert != null) { // currently always true
                    if (quote) {
                        insert = "'" + insert + '\'';
                    } else if (doubleQuote) {
                        insert = "\"" + insert + '\"';
                    }

                    // document position will be set by editor's transfer handler
                    ed.setSelectedText(insert.replace(sep, "/"));
                }
            }
        } finally {
            ed.getDocument().endCompoundEdit();
        }
    }

    public void find() {
        String sel = getEditor().getSelectedText();
        if (sel.isEmpty()) {
           if (findField.getText().isEmpty()) {
               findField.setText(Settings.getUser().get(KEY_LAST_FIND));
           }
        } else {
            findField.setText(sel);
        }
        if (!findPanel.isVisible()) {
            final Dimension preferredSize = findPanel.getPreferredSize();
            final int finalHeight = preferredSize.height;
            preferredSize.height = 1;
            findPanel.setPreferredSize(preferredSize);
            findPanel.setVisible(true);
            new Animation(0.2f, finalHeight) {
                @Override
                public void composeFrame(float position) {
                    preferredSize.height = Interpolation.lerp(position, 1, finalHeight);
                    findPanel.setPreferredSize(preferredSize);
                    findPanel.setSize(findPanel.getWidth(), preferredSize.height);
                    getRootPane().validate();
                }
            }.play();
        } else {
            findNextBtn.doClick();
        }
        findField.selectAll();
        findField.requestFocusInWindow();
    }

    @Override
    public boolean canPerformCommand(AbstractCommand command) {
        if (command == Commands.FIND || command == Commands.RUN_FILE || command == Commands.DEBUG_FILE) {
            return true;
        }
        return super.canPerformCommand(command);
    }

    @Override
    public boolean isCommandApplicable(AbstractCommand command) {
        if (!getEditor().isEditable()) {
            if ((command == Commands.CLEAR) || (command == Commands.SAVE) || (command == Commands.FORMAT_CODE)) {
                return false;
            }
        }
        if (command == Commands.RUN_FILE || command == Commands.DEBUG_FILE) {
            if (getCodeType().normalize() == CodeType.JAVASCRIPT) {
                if (command == Commands.DEBUG_FILE) {
                    return ScriptDebugging.isInstalled();
                }
                return true;
            }
            return false;
        } else if (command == Commands.FORMAT_CODE) {
            return CodeFormatterFactory.getFormatter(getCodeType()) != null;
        }

        return super.isCommandApplicable(command);
    }

    @Override
    public void performCommand(AbstractCommand command) {
        if (command == Commands.FIND) {
            find();
        } else if (command == Commands.RUN_FILE || command == Commands.DEBUG_FILE) {
            if (isCommandApplicable(command)) {
                run(command == Commands.DEBUG_FILE);
            }
        } else if (command == Commands.FORMAT_CODE) {
            format();
        } else {
            super.performCommand(command);
        }
    }

    @Override
    public void setFile(File f) {
        super.setFile(f);
        boolean editable = false;
        if (f != null && type.getDeterminativeFile(f) == null && (!f.exists() || f.canWrite())) {
            editable = true;
        }
        setReadOnly(!editable);
    }

	private void closeBtncloseClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeBtncloseClicked
            final Dimension preferredSize = findPanel.getPreferredSize();
            final int startHeight = preferredSize.height;
            findPanel.setPreferredSize(preferredSize);
            new Animation(0.2f, startHeight) {
                @Override
                public void composeFrame(float position) {
                    if (position == 1) {
                        findPanel.setVisible(false);
                        preferredSize.height = startHeight;
                    } else {
                        preferredSize.height = Interpolation.lerp(position, startHeight, 1);
                    }
                    findPanel.setPreferredSize(preferredSize);
                    findPanel.setSize(findPanel.getWidth(), preferredSize.height);
                    getRootPane().validate();
                }
            }.play();
}//GEN-LAST:event_closeBtncloseClicked

	private void findNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findNextActionPerformed
            StrangeEons.setWaitCursor(true);
            try {

                Pattern pattern = createPattern();
                if (pattern == null) {
                    return;
                }

                JSourceCodeEditor ed = getEditor();

                // search from current pos to end of file, or, if we at the previous
                // match, search from current pos + 1
                int s = ed.getSelectionStart();
                int e = ed.getDocumentLength();

                int oldStart = ed.getSelectionStart();
                int oldEnd = ed.getSelectionEnd();

                String selection = ed.getSelectedText();
                if (selection.length() > 0 && pattern.matcher(selection).matches()) {
                    if (regExpCheck.isSelected()) {
                        s += selection.length();
                    } else {
                        ++s;
                    }
                }
                boolean didRollover = false;
                if (s >= e) {
                    s = 0;
                    didRollover = true;
                }

                ed.setSelectionStart(s);
                ed.setSelectionEnd(e);
                String searchFrame = ed.getSelectedText();

                regExpErrorLabel.setText(" ");

                currentSearch = pattern.matcher(searchFrame);
                if (currentSearch.find()) {
                    ed.setSelectionStart(s + currentSearch.start());
                    ed.setSelectionEnd(s + currentSearch.end());
                } else {
                    ed.select(e, e);
                    if (didRollover) {
                        regExpErrorLabel.setText(string("find-no-matches"));
                        getToolkit().beep();
                    } else {
                        findNextActionPerformed(evt);

                        String labelText = regExpErrorLabel.getText();
                        if (labelText.equals(string("find-no-matches"))) {
                            ed.select(oldStart, oldEnd);
                        } else if (labelText.equals(" ")) {
                            regExpErrorLabel.setText(string("find-eof"));
                        }
                    }
                }
            } finally {
                StrangeEons.setWaitCursor(false);
            }
	}//GEN-LAST:event_findNextActionPerformed

	private void findPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findPrevActionPerformed
            StrangeEons.setWaitCursor(true);
            try {
                Pattern pattern = createPattern();
                if (pattern == null) {
                    return;
                }

                JSourceCodeEditor ed = getEditor();

                // search from current pos to end of file, or, if we are at the previous
                // match, search up to the position just before the current one
                int s = ed.getSelectionEnd();

                int oldStart = ed.getSelectionStart();
                int oldEnd = ed.getSelectionEnd();

                String selection = ed.getSelectedText();
                if (!selection.isEmpty() && pattern.matcher(selection).matches()) {
                    if (regExpCheck.isSelected()) {
                        s -= selection.length();
                    } else {
                        --s;
                    }
                }

                boolean didRollover = false;
                if (s <= 0) {
                    s = ed.getDocumentLength();
                    didRollover = true;
                }

                String searchFrame = ed.getText(0, s);

                int start = s, end = -1;
                currentSearch = pattern.matcher(searchFrame);
                for (int i = s; i >= 0; --i) {
                    if (currentSearch.find(i)) {
                        if (end == -1) {
                            start = currentSearch.start();
                            end = currentSearch.end();
                        } else if (end == currentSearch.end()) {
                            // if using regexps, this might not be the longest
                            // possible match anchored at this end position:
                            // after the initial match, keep the loop going and
                            // track the lowest start pos that has the same end pos
                            start = currentSearch.start();
                        }
                    }
                }

                regExpErrorLabel.setText(" ");

                if (end >= 0) {
                    ed.select(start, end);
                } else {
                    if (didRollover) {
                        regExpErrorLabel.setText(string("find-no-matches"));
                        getToolkit().beep();
                    } else {
                        end = ed.getDocumentLength();
                        ed.select(0, 0);
                        findPrevActionPerformed(evt);

                        String labelText = regExpErrorLabel.getText();
                        if (labelText.equals(string("find-no-matches"))) {
                            ed.select(oldStart, oldEnd);
                        } else if (labelText.equals(" ")) {
                            regExpErrorLabel.setText(string("find-tof"));
                        }
                    }
                }
            } finally {
                StrangeEons.setWaitCursor(false);
            }
	}//GEN-LAST:event_findPrevActionPerformed

	private void replaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceActionPerformed
            Pattern pattern = createPattern();
            if (pattern == null) {
                return;
            }
            String replacement = replaceField.getText();
            if (!regExpCheck.isSelected()) {
                replacement = Matcher.quoteReplacement(replacement);
            }
            String selection = getEditor().getSelectedText();
            if (selection != null) {
                Matcher m = pattern.matcher(selection);
                if (m.matches()) {
                    getEditor().setSelectedText(m.replaceFirst(replacement));
                }
            }
            findNextActionPerformed(null);
	}//GEN-LAST:event_replaceActionPerformed

	private void replaceAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllActionPerformed
            Pattern pattern = createPattern();
            if (pattern == null) {
                return;
            }
            String replacement = replaceField.getText();
            if (!regExpCheck.isSelected()) {
                replacement = Matcher.quoteReplacement(replacement);
            }
            Matcher m = pattern.matcher(getEditor().getText());
            getEditor().setText(m.replaceAll(replacement));
	}//GEN-LAST:event_replaceAllActionPerformed

	private void storeFindCheckStates(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_storeFindCheckStates
            Settings s = Settings.getUser();
            if (evt.getSource() == regExpCheck) {
                if (regExpCheck.isSelected()) {
                    incrementalCheck.setEnabled(false);
                    incrementalCheck.setSelected(false);
                } else {
                    incrementalCheck.setSelected(s.getBoolean("find-incremental"));
                    incrementalCheck.setEnabled(true);
                }
                createPattern();
            }

            if (incrementalCheck.isEnabled()) {
                s.set("find-incremental", incrementalCheck.isSelected() ? "yes" : "no");
            }
            s.set("find-case-sensitive", caseSensCheck.isSelected() ? "yes" : "no");
            s.set("find-regular-expression", regExpCheck.isSelected() ? "yes" : "no");
	}//GEN-LAST:event_storeFindCheckStates

	private void findPanelShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_findPanelShown
            Settings s = Settings.getUser();
            regExpCheck.setSelected(s.getBoolean("find-regular-expression"));
            if (regExpCheck.isSelected()) {
                incrementalCheck.setSelected(false);
                incrementalCheck.setEnabled(false);
            } else {
                incrementalCheck.setSelected(s.getBoolean("find-incremental"));
                incrementalCheck.setEnabled(true);
            }
            caseSensCheck.setSelected(s.getBoolean("find-case-sensitive"));
	}//GEN-LAST:event_findPanelShown

	private void navListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_navListValueChanged
            if (navIsChanging > 0) {
                return;
            }
            NavigationPoint np = (NavigationPoint) navList.getSelectedValue();
            if (np == null) {
                return;
            }
            np.visit(this);
            getEditor().requestFocusInWindow();
            navList.setSelectedIndex(-1);
	}//GEN-LAST:event_navListValueChanged

    @Override
    protected void onHeartbeat() {
        super.onHeartbeat();
        if (++navBeatCounter == 3) {
            navBeatCounter = 0;
            navListMouseEntered(null);
        }
    }
    private int navBeatCounter;

	private void navListMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_navListMouseEntered
            if (navIsChanging > 0 || navigator == null || !navOutOfDate) {
                return;
            }

            long time = System.currentTimeMillis();
            if ((time - lastNavUpdate) > NAVIGATOR_SCAN_DELAY) {
                refreshNavigator();
            }
	}//GEN-LAST:event_navListMouseEntered

	private void sideBarSplitterPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_sideBarSplitterPropertyChange
            if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(evt.getPropertyName())) {
                if (sideBarSplitter.isEnabled()) {
                    int newSize = sideBarSplitter.getDividerLocation();
                    navSplitSize = newSize;
                    StrangeEonsEditor[] eds = StrangeEons.getWindow().getEditors();
                    for (int i = 0; i < eds.length; ++i) {
                        if (!(eds[i] instanceof CodeEditor)) {
                            continue;
                        }
                        CodeEditor ced = (CodeEditor) eds[i];
                        if (ced.sideBarSplitter.isEnabled()) {
                            ced.sideBarSplitter.setDividerLocation(newSize);
                        }
                    }
                }
            }
	}//GEN-LAST:event_sideBarSplitterPropertyChange

	private void sourceNavCloseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceNavCloseButtonActionPerformed
            setNavigatorVisible(false);
	}//GEN-LAST:event_sourceNavCloseButtonActionPerformed

	private void formFocusGained( java.awt.event.FocusEvent evt ) {//GEN-FIRST:event_formFocusGained
            editor.requestFocusInWindow();
	}//GEN-LAST:event_formFocusGained

    private void navListMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_navListMousePressed
        // if user clicks on the item that is already selected, it should
        // act like it is being selected again (i.e., show the nav point)
        int index = navList.locationToIndex(evt.getPoint());
        if (index != -1 && index == navList.getSelectedIndex()) {
            navListValueChanged(null);
        }
    }//GEN-LAST:event_navListMousePressed

    private static final String KEY_LAST_FIND = "last-find";
    private static final String KEY_LAST_REPLACE = "last-replace";

    public static final String KEY_SHOW_NAVIGATOR = "show-source-navigator";
    private static boolean navIsVisible = Settings.getShared().getYesNo(KEY_SHOW_NAVIGATOR);

    public static final String KEY_NAV_SIZE = "source-navigator-size";
    private static int navSplitSize = Settings.getShared().getInt(KEY_NAV_SIZE, 200);

    private static final long NAVIGATOR_SCAN_DELAY = 1000;

    private long lastNavUpdate;
    private int navIsChanging;

    private Pattern createPattern() {
        String patternText = findField.getText();
        Settings.getUser().set(KEY_LAST_FIND, patternText);
        Settings.getUser().set(KEY_LAST_REPLACE, replaceField.getText());
        if (errorHighlight != null) {
            findField.getHighlighter().removeHighlight(errorHighlight);
        }
        if (patternText.isEmpty()) {
            regExpErrorLabel.setText(" ");
            return null;
        }
        if (!regExpCheck.isSelected()) {
            patternText = Pattern.quote(patternText);
        }

        Pattern pattern;
        try {
            int flags = Pattern.MULTILINE | Pattern.UNICODE_CASE | Pattern.UNIX_LINES;
            if (!caseSensCheck.isSelected()) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            pattern = Pattern.compile(patternText, flags);
            regExpErrorLabel.setText(" ");
        } catch (PatternSyntaxException e) {
            String message = PatternExceptionLocalizer.localize(patternText, e);
            regExpErrorLabel.setText(message);
            int pos = e.getIndex();
            if (pos >= 0) {
                try {
                    if (pos == patternText.length()) {
                        --pos;
                    }
                    errorHighlight = findField.getHighlighter().addHighlight(pos, pos + 1, ORANGE_SQUIGGLE);
                } catch (BadLocationException ble) {
                    ble.printStackTrace();
                }
            }
            findField.requestFocusInWindow();
            return null;
        }
        return pattern;
    }

    private Matcher currentSearch;
    private Object errorHighlight;
    private static HighlightPainter ORANGE_SQUIGGLE = new ErrorSquigglePainter(new Color(0xcc_5600));

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox caseSensCheck;
    private ca.cgjennings.apps.arkham.ToolCloseButton closeBtn;
    private ca.cgjennings.ui.textedit.JSourceCodeEditor editor;
    private javax.swing.JTextField findField;
    private javax.swing.JButton findNextBtn;
    private javax.swing.JPanel findPanel;
    private javax.swing.JButton findPrevBtn;
    private javax.swing.JLabel icon;
    private javax.swing.JCheckBox incrementalCheck;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList navList;
    private javax.swing.JPanel navPanel;
    private javax.swing.JScrollPane navScroll;
    private javax.swing.JPanel navTitle;
    private javax.swing.JCheckBox regExpCheck;
    private javax.swing.JLabel regExpErrorLabel;
    private javax.swing.JButton replaceAllBtn;
    private javax.swing.JButton replaceBtn;
    private javax.swing.JTextField replaceField;
    private javax.swing.JPanel searchControlsPanel;
    private javax.swing.JPanel sideBarPanel;
    private javax.swing.JSplitPane sideBarSplitter;
    private javax.swing.JPanel titlePanel;
    // End of variables declaration//GEN-END:variables

    public final JSourceCodeEditor getEditor() {
        return editor;
    }

    public String getEncoding() {
        return encoding;
    }

    @Override
    protected void saveImpl(File f) throws IOException {
        String text = editor.getText();
        ProjectUtilities.copyReader(new StringReader(escape(text)), f, encoding);
        refreshNavigator(text);
        if (type.processAfterWrite(f, text)) {
            refreshDependentFiles(type, f);
        }
    }

    /**
     * Call to reload files that depend on this file and were changed when it
     * was saved. This is called immediately if {@link CodeType#processAfterWrite}
     * returns true. Otherwise it can be called manually if processing completes
     * in another thread.
     *
     * @param f the file for which editors should be reloaded
     */
    private static void refreshDependentFiles(CodeType type, File f) {
        File generated = type.getDependentFile(f);
        if (generated != null) {
            StrangeEonsEditor[] showingGenerated = StrangeEons.getWindow().getEditorsShowingFile(generated);
            for (StrangeEonsEditor ed : showingGenerated) {
                if (ed instanceof CodeEditor) {
                    try {
                        ((CodeEditor) ed).refresh();
                    } catch(IOException ioe) {
                        StrangeEons.log.log(Level.SEVERE, "failed to reload", ioe);
                    }
                }
            }
        }
    }

    public CodeType getCodeType() {
        return type;
    }

    @Override
    public String getFileNameExtension() {
        return type.getExtension();
    }

    @Override
    public String getFileTypeDescription() {
        return type.getDescription();
    }

    @Override
    protected StrangeEonsEditor spinOffImpl() {
        CodeEditor clone = new CodeEditor();
        clone.setFile(getFile());
        clone.encoding = encoding;
        clone.type = type;
        type.initializeEditor(clone);
        clone.editor.setText(editor.getText());
        clone.editor.select(0, 0);
        clone.editor.getDocument().clearUndoHistory();
        return clone;
    }

    @Override
    public void clear() {
        editor.setText("");
    }

    @Override
    protected void printImpl(final PrinterJob job) throws PrintException, PrinterException {
        String source = getText();
        JTextComponent tc = new JTextArea();
        tc.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        tc.setText(source);
        Printable p = tc.getPrintable(new MessageFormat(getFile().getPath()), new MessageFormat("{0}"));
        job.setPrintable(p);
        job.setJobName(getFile().getName());
        if (ResourceKit.showPrintDialog(job)) {
            new BusyDialog(StrangeEons.getWindow(), string("busy-printing"), () -> {
                try {
                    job.print();
                } catch (PrinterAbortException e) {
                } catch (Exception e) {
                    ErrorDialog.displayError(string("ae-err-print"), e);
                }
            }, (ActionEvent e) -> {
                job.cancel();
            });
        }
    }

    @Override
    protected void exportImpl(int type, File f) throws IOException {
        CSSStyler styler = new CSSStyler(editor.getTokenizer());
        String html = styler.style(editor.getText());
        html = "<html>\n<head>\n<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n<title>"
                + f.getName() + "</title>\n"
                + styler.getCSS(true)
                + "<body>\n<pre>\n" + html + "</pre>\n</body>\n</html>";

        ProjectUtilities.copyReader(new StringReader(html), f, ProjectUtilities.ENC_UTF8);
    }

    @Override
    public String[] getExportExtensions() {
        return new String[]{"html"};
    }

    @Override
    public String[] getExportDescriptions() {
        return new String[]{string("pa-new-html")};
    }

    /**
     * Called before each line is written to a file to allow the editor to
     * convert the text content. The default implementation checks if
     * {@link #isCharacterEscapingEnabled()} returns {@code true}, and if
     * so, {@linkplain EscapedTextCodec#escapeUnicode(java.lang.CharSequence)
     * it applies Unicode escapes to the line}.
     *
     * @param line the line to process
     * @return the processed line
     */
    protected String escape(String line) {
        if (isCharacterEscapingEnabled()) {
            line = EscapedTextCodec.escapeUnicode(line);
        }
        return line;
    }

    /**
     * Called as each line is read to allow the editor to convert the text
     * content. The default implementation checks if
     * {@link #isCharacterEscapingEnabled()} returns {@code true}, and if
     * so, {@linkplain EscapedTextCodec#unescapeUnicode
     * it removes Unicode escapes from the line}.
     *
     * @param line the line to process
     * @return the processed line
     */
    protected String unescape(String line) {
        if (isCharacterEscapingEnabled()) {
            line = EscapedTextCodec.unescapeUnicode(line);
        }
        return line;
    }

    /**
     * Returns {@code true} if character escaping is enabled on read and
     * write of the document. When enabled, the default behaviour of
     * {@link #escape} and
     * {@link #unescape} is to automatically convert
     * Java-style Unicode escapeUnicode sequences in the file into their
     * character equivalents when displayed in the editor.
     *
     * @return {@code true} if automatic character escaping is enabled
     * @see EscapedTextCodec
     */
    public boolean isCharacterEscapingEnabled() {
        return characterEscaping;
    }
    private boolean characterEscaping = true;

    /**
     * Sets whether character escaping is enabled on read and write of the
     * document.
     *
     * @param characterEscaping if {@code true}, automatic character
     * escaping is enabled
     * @see #isCharacterEscapingEnabled()
     * @see #escape
     * @see #unescape
     */
    public void setCharacterEscapingEnabled(boolean characterEscaping) {
        this.characterEscaping = characterEscaping;
    }

    private static void installStrangeEonsEditorCommands(JSourceCodeEditor ed) {
        final InputHandler ih = ed.getInputHandler();

        final ActionListener EVAL = (ActionEvent e) -> {
            JSourceCodeEditor ed1 = EditorCommands.findEditor(e);
            if (!ed1.isEditable()) {
                ed1.getToolkit().beep();
                return;
            }
            ed1.getDocument().beginCompoundEdit();
            try {
                if (ed1.getSelectionStart() == ed1.getSelectionEnd()) {
                    ih.executeAction(EditorCommands.HOME, ed1, "");
                    ih.executeAction(EditorCommands.SELECT_END, ed1, "");
                }
                String expression = ed1.getSelectedText();
                Object result = null;
                try {
                    result = ProjectUtilities.runScript("Selected Text", expression);
                    if (result instanceof Double) {
                        String v = result.toString();
                        if (v.endsWith(".0")) {
                            v = v.substring(0, v.length() - 2);
                        }
                        result = v;
                    }
                } catch (Exception ex) {
                    result = ex;
                }
                int start = Math.min(ed1.getSelectionStart(), ed1.getSelectionEnd());
                String replacement = String.valueOf(result);
                ed1.setSelectedText(replacement);
                ed1.select(start, start + replacement.length());
            } finally {
                ed1.getDocument().endCompoundEdit();
            }
        };

        ih.addKeyBinding("P+EQUALS", EVAL);
        ih.addKeyBinding("C+EQUALS", EVAL);

        ih.addKeyBinding("S+DELETE", EditorCommands.CUT);
        ih.addKeyBinding("C+INSERT", EditorCommands.COPY);
        ih.addKeyBinding("S+INSERT", EditorCommands.PASTE);
    }

    /**
     * Returns a popup menu of actions that are appropriate for the editor type.
     *
     * @return a context menu populated with menu items for the current
     * selection and code type
     */
    protected JPopupMenu createPopupMenu() {
        final JPopupMenu menu = new JPopupMenu();

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                for (int i = 0; i < menu.getComponentCount(); ++i) {
                    final Component c = menu.getComponent(i);
                    if (c instanceof JMenuItem) {
                        final Action a = ((JMenuItem) c).getAction();
                        if (a instanceof AbstractCommand) {
                            ((AbstractCommand) a).update();
                        }
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        if(isCommandApplicable(Commands.FORMAT_CODE)) {
            menu.add(Commands.FORMAT_CODE);
            menu.addSeparator();
        }

        if (type == CodeType.JAVASCRIPT) {
            menu.add(Commands.RUN_FILE);
            if (ScriptDebugging.isInstalled()) {
                menu.add(Commands.DEBUG_FILE);
            }
            menu.addSeparator();
        }

        if (type == CodeType.HTML) {
            JMenuItem browse = new JMenuItem(string("pa-browse-name"));
            browse.addActionListener((ActionEvent e) -> {
                if (getFile() == null) {
                    saveAs();
                    if (getFile() == null) {
                        return;
                    }
                }
                try {
                    DesktopIntegration.browse(getFile().toURI(), CodeEditor.this);
                } catch (IOException ex) {
                    ErrorDialog.displayError(string("prj-err-open", getFile().getName()), ex);
                }
            });
            if (getFile() != null && DesktopIntegration.BROWSE_SUPPORTED) {
                menu.add(browse);
                menu.addSeparator();
            }
        }

        menu.add(Commands.CUT);
        menu.add(Commands.COPY);
        menu.add(Commands.PASTE);

        return menu;
    }

    /**
     * Format the code in the code editor, if a suitable formatter is available.
     * Otherwise, do nothing.
     */
    public void format() {
        CodeFormatterFactory.Formatter f = CodeFormatterFactory.getFormatter(type);
        if(f != null) {
            int line = editor.getCaretLine();
            String formatted = f.format(editor.getText());
            editor.setText(formatted);
            int offset = editor.getLineCount() < line ? editor.getDocumentLength() : editor.getLineStartOffset(line);
            editor.setCaretPosition(line);
            editor.scrollToCaret();
        }
    }

    /**
     * Sets the navigator that will be used to populate the navigation panel for
     * this editor. Setting the navigator to {@code null} will disable the
     * navigator panel.
     *
     * @param nav the new navigator to set
     */
    public void setNavigator(Navigator nav) {
        if (navigator == nav) {
            return;
        }
        if (navigator != null) {
            navigator.uninstall(this);
        }

        navigator = nav;

        if (navigator != null) {
            navigator.install(this);
            refreshNavigator();
        }

        // if the nav isn't visible, the code below will think it is being
        // uninstalled, which will ensure that it remains hidden but up-to-date
        // in case it becomes visible later
        if (!navIsVisible) {
            nav = null;
        }

        if (nav == null) {
            sideBarPanel.setVisible(false);
            navPanel.setVisible(false);
            sideBarSplitter.setEnabled(false); // must come before resize
            sideBarSplitter.setDividerSize(0);
            sideBarSplitter.setDividerLocation(getWidth());
        } else {
            sideBarPanel.setVisible(true);
            navPanel.setVisible(true);
            sideBarSplitter.setEnabled(true); // must come before resize
            sideBarSplitter.setDividerSize(NAV_DIV_SIZE);
            sideBarSplitter.setDividerLocation(navSplitSize);
        }
    }
    private static final int NAV_DIV_SIZE = 8;

    /**
     * Returns the current navigator for this editor, or {@code null} if
     * none is set.
     *
     * @return the current navigator
     */
    public final Navigator getNavigator() {
        return navigator;
    }

    /**
     * Returns the navigation point selected in the navigation panel, or
     * {@code null} if none is selected.
     *
     * @return the selected navigation point, or {@code null}
     */
    public NavigationPoint getSelectedNavigationPoint() {
        if (navigator == null || !navIsVisible) {
            return null;
        }
        return (NavigationPoint) navList.getSelectedValue();
    }

    /**
     * Updates the navigation panel to reflect the current state of the text.
     */
    public void refreshNavigator() {
        refreshNavigator(getEditor().getText());
    }

    private void refreshNavigator(String text) {
        if (navigator == null || text == null) {
            refreshNavigator((List<NavigationPoint>) null);
        } else {
            refreshNavigator(navigator.getNavigationPoints(text));
        }
    }

    private void refreshNavigator(List<NavigationPoint> points) {
        ++navIsChanging;
        try {
            NavigationPoint np = (NavigationPoint) navList.getSelectedValue();
            navModel.setList(points);
            if (np != null) {
                np = np.getClosestPoint(points);
                navList.setSelectedValue(np, true);
            }
        } finally {
            --navIsChanging;
            navOutOfDate = false;
        }
        lastNavUpdate = System.currentTimeMillis();
    }

    private Navigator navigator;
    private boolean navOutOfDate;
    private NavModel navModel = new NavModel();

    private class NavModel extends AbstractListModel {

        private List<NavigationPoint> points;

        public void setList(List<NavigationPoint> newList) {
            List<NavigationPoint> oldList = points;
            points = newList;
            if (oldList != null && oldList.size() > 0) {
                fireIntervalRemoved(this, 0, oldList.size() - 1);
            }
            if (newList != null && newList.size() > 0) {
                fireContentsChanged(this, 0, newList.size() - 1);
            }
        }

        @Override
        public int getSize() {
            return navigator == null ? 0 : (points == null ? 0 : points.size());
        }

        @Override
        public Object getElementAt(int index) {
            if (navigator == null || points == null) {
                throw new IllegalArgumentException("" + index + ">=0");
            } else {
                return points.get(index);
            }
        }
    }

    /**
     * Returns {@code true} if code editors will display a source navigator
     * when one has been set. This setting applies to all code editors.
     *
     * @return {@code true} if navigators are visible
     */
    public static boolean isNavigatorVisible() {
        return navIsVisible;
    }

    /**
     * Sets whether source navigators are hidden. This affects all code editors.
     * To remove the navigator of a specific editor, set is navigator to
     * {@code null}.
     *
     * @param navIsVisible if {@code true}, the navigator panel will be
     * displayed by code editors; otherwise it will be hidden
     */
    public static void setNavigatorVisible(boolean navIsVisible) {
        if (CodeEditor.navIsVisible != navIsVisible) {
            CodeEditor.navIsVisible = navIsVisible;
            StrangeEonsEditor[] eds = StrangeEons.getWindow().getEditors();
            for (int i = 0; i < eds.length; ++i) {
                if (!(eds[i] instanceof CodeEditor)) {
                    continue;
                }
                CodeEditor ced = (CodeEditor) eds[i];
                ced.setNavigator(ced.getNavigator());
            }
            Settings.getUser().set(KEY_SHOW_NAVIGATOR, navIsVisible ? "yes" : "no");
        }
    }

    /**
     * Returns the document text.
     *
     * @return the edited text
     */
    public String getText() {
        return getEditor().getText();
    }

    /**
     * Sets the document text.
     *
     * @param text to text to be edited
     */
    public void setText(String text) {
        getEditor().setText(text);
    }

    /**
     * Returns the selected text. Returns an empty string if the selection is
     * empty.
     *
     * @return the text with the current selection
     */
    public String getSelectedText() {
        return getEditor().getSelectedText();
    }

    /**
     * Replaces the selection with the specified text. If the selection is
     * empty, then the specified text is inserted.
     *
     * @param text the text to replace the selection with
     */
    public void setSelectedText(String text) {
        getEditor().setSelectedText(text);
    }

    /**
     * Selects from the start offset to the end offset.
     *
     * @param startOffset the start offset
     * @param endOffset the end offset
     */
    public void select(int startOffset, int endOffset) {
        getEditor().select(startOffset, endOffset);
    }

    /**
     * Selects the entire document.
     */
    public void selectAll() {
        getEditor().selectAll();
    }

    /**
     * Returns the length of the document, in characters.
     *
     * @return the document length
     */
    public int getDocumentLength() {
        return getEditor().getDocumentLength();
    }

    /**
     * Run the current script.
     *
     * @param debugIfAvailable if the debugger is enabled, debug the script by
     * activating a breakpoint when the script starts running
     */
    public void run(boolean debugIfAvailable) {
        Project p = StrangeEons.getWindow().getOpenProject();
        Task t = null;
        Member m = null;

        File f = getFile();
        if (f != null) {
            if (hasUnsavedChanges()) {
                save();
            }
            if (p != null) {
                m = p.findMember(f);
                if (m != null) {
                    t = m.getTask();
                }
            }
            try {
                ProjectUtilities.runScript(f, p, t, m, debugIfAvailable);
            } catch (IOException e) {
                ErrorDialog.displayError(title, e);
            }
        } else {
            ProjectUtilities.runScript("Quickscript", getEditor().getText(), p, t, m, debugIfAvailable);
        }
    }

    /**
     * Displays a dialog that allows the user to sort the selected lines.
     */
    public void sortSelectedLines() {
        if (sortDialog == null) {
            sortDialog = new SortDialog(StrangeEons.getWindow());
            sortDialog.setLocationRelativeTo(this);
        }

        if (!sortDialog.showDialog()) {
            return;
        }

        JSourceCodeEditor ed = getEditor();

        StrangeEons.setWaitCursor(true);
        try {
            if (ed.getSelectionStart() == ed.getSelectionEnd()) {
                ed.selectAll();
            }
            String[] lines = EditorCommands.getSelectedLineText(ed, false);
            lines = sortDialog.getSorter().sort(lines);
            boolean delDupes = sortDialog.getDeleteDuplicates();

            StringBuilder b = new StringBuilder();
            for (int i = 0; i < lines.length; ++i) {
                if (delDupes && i > 0 && lines[i].equals(lines[i - 1])) {
                    continue;
                }
                if (i > 0) {
                    b.append('\n');
                }
                b.append(lines[i]);
            }

            // add \n since selection extends to next line
            if (ed.getSelectionEnd() == ed.getLineStartOffset(ed.getSelectionEndLine())) {
                b.append('\n');
            }

            ed.setSelectedText(b.toString());
        } finally {
            StrangeEons.setWaitCursor(false);
        }
    }
    private static SortDialog sortDialog;
}
