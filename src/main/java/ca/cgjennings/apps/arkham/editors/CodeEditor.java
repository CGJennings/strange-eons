package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.algo.StaggeredDelay;
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
import ca.cgjennings.apps.arkham.plugins.typescript.TSLanguageServices;
import ca.cgjennings.apps.arkham.project.Member;
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
import ca.cgjennings.ui.textedit.CodeEditorBase;
import ca.cgjennings.ui.textedit.CodeType;
import static ca.cgjennings.ui.textedit.CodeType.TYPESCRIPT;
import ca.cgjennings.ui.textedit.Formatter;
import ca.cgjennings.ui.textedit.HtmlStyler;
import ca.cgjennings.ui.textedit.NavigationHost;
import ca.cgjennings.ui.textedit.NavigationPoint;
import ca.cgjennings.ui.textedit.Navigator;
import ca.cgjennings.ui.theme.Palette;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
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
import javax.swing.JSeparator;
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
public class CodeEditor extends AbstractSupportEditor implements NavigationHost {

    /**
     * Creates a code editor with no file, encoding, or file type attached. Note
     * that code editors are designed to edit files within a project, and
     * therefore expect {@link #getFile()} to return a non-{@code null} value.
     */
    private CodeEditor() {
        initComponents();
        editor.putClientProperty(ContextBar.BAR_LEADING_SIDE_PROPERTY, Boolean.TRUE);

        sideBarPanel.setVisible(false);
        findPanel.setVisible(false);

        MarkupTargetFactory.enableTargeting(findField, false);
        MarkupTargetFactory.enableTargeting(replaceField, false);

        editor.addDocumentListener(new DocumentEventAdapter() {
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
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && value instanceof NavigationPoint) {
                    NavigationPoint np = (NavigationPoint) value;
                    setIcon(np.getIcon());
                    setToolTipText(np.getLongDescription());
                    setBorder(
                            BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(0, np.getScope() * 12, 0, 0))
                    );
                }
                return this;
            }
        });

        FileDrop.Listener dropListener = (f) -> {
            if (fileDropListener == null) {
                doDefaultFileDrop(f);
            } else {
                fileDropListener.filesDropped(f);
            }
        };
        new FileDrop(navPanel, null, true, dropListener);
        new FileDrop(findPanel, null, true, dropListener);
        FileDrop.of(null, sideBarSplitter).setListener(dropListener);

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

        createTimer((int) NAVIGATOR_SCAN_DELAY);
        editor.setPopupMenuBuilder(this::createPopupMenu);
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
        initializeForCodeType();
        setFile(file);
        this.encoding = encoding;
        readFile();
        setUnsavedChanges(false);
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
        this.encoding = encoding;
        type = codeType;
        initializeForCodeType();

        editor.setInitialText(text);
        setUnsavedChanges(false);
        setReadOnly(true);
    }

    private void initializeForCodeType() {
        editor.setCodeType(type);

        setFrameIcon(type.getIcon());
        setCharacterEscapingEnabled(type.getAutomaticCharacterEscaping());

        Navigator nav = null;
        if (editor.getCodeSupport() != null) {
            nav = editor.getCodeSupport().createNavigator(this);
        }
        setNavigator(nav);

        encoding = type.getEncodingName();
    }

    public void setReadOnly(boolean readOnly) {
        if (readOnly == editor.isEditable()) {
            final boolean editable = !readOnly;
            editor.setEditable(editable);
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
     * If one exists, returns the {@code Charset} indicated by the sequence.
     * Otherwise, returns {@code null}.
     *
     * @param f the file to check
     * @return the encoding represented by the byte order mark, or {@code null}
     */
    public static Charset checkFileForBOM(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            int b0 = in.read();
            switch (b0) {
                case 0xEE:
                    if (in.read() == 0xBB && in.read() == 0xBF) {
                        return StandardCharsets.UTF_8;
                    }
                    break;
                case 0xFE:
                    if (in.read() == 0xFF) {
                        return StandardCharsets.UTF_16BE;
                    }
                    break;
                case 0xFF:
                    if (in.read() == 0xFE) {
                        if (in.read() == 0x00) {
                            if (in.read() == 0x00) {
                                return Charset.forName("UTF-32LE");
                            }
                        } else {
                            return StandardCharsets.UTF_16LE;
                        }
                    }
                    break;
                case 0x00:
                    if (in.read() == 0x00 && in.read() == 0xFE && in.read() == 0xFF) {
                        return Charset.forName("UTF-32BE");
                    }
                    break;
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
        CodeEditorBase ed = getEditor();
        int line = ed.getLineOfOffset(ed.getCaretPosition());

        readFile();

        int offset = ed.getLineStartOffset(line);
        if (offset < 0) {
            offset = ed.getLength();
        }
        ed.select(offset, offset);
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
        editor.setInitialText(text);
        refreshNavigator(text);
        setUnsavedChanges(false);
    }

    private String encoding = "utf-8";
    private CodeType type = CodeType.PLAIN;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
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
        caseSensCheck = new javax.swing.JCheckBox();
        regExpCheck = new javax.swing.JCheckBox();
        regExpErrorLabel = new javax.swing.JLabel();
        wholeWordCheck = new javax.swing.JCheckBox();
        sideBarSplitter = new javax.swing.JSplitPane();
        sideBarPanel = new javax.swing.JPanel();
        navPanel = new javax.swing.JPanel();
        navScroll = new javax.swing.JScrollPane();
        navList = new javax.swing.JList<>();
        navTitle = new javax.swing.JPanel();
        javax.swing.JLabel navLabel = new javax.swing.JLabel();
        ca.cgjennings.apps.arkham.ToolCloseButton sourceNavCloseButton = new ca.cgjennings.apps.arkham.ToolCloseButton();
        editor = new ca.cgjennings.ui.textedit.CodeEditorBase();

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
                findCloseBtnClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.ipady = 1;
        titlePanel.add(closeBtn, gridBagConstraints);

        findPanel.add(titlePanel, java.awt.BorderLayout.NORTH);

        icon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        icon.setIcon(ResourceKit.getIcon("find-lr"));
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

        caseSensCheck.setFont(caseSensCheck.getFont().deriveFont(caseSensCheck.getFont().getSize()-1f));
        caseSensCheck.setSelected(Settings.getUser().getBoolean("find-case-sensitive"));
        caseSensCheck.setText(string( "find-case-sense" )); // NOI18N
        caseSensCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findOptionsChanged(evt);
            }
        });

        regExpCheck.setFont(regExpCheck.getFont().deriveFont(regExpCheck.getFont().getSize()-1f));
        regExpCheck.setSelected(Settings.getUser().getBoolean("find-regular-expression"));
        regExpCheck.setText(string( "find-reg-exp" )); // NOI18N
        regExpCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findOptionsChanged(evt);
            }
        });

        regExpErrorLabel.setFont(regExpErrorLabel.getFont().deriveFont(regExpErrorLabel.getFont().getSize()-1f));
        regExpErrorLabel.setForeground(Palette.get.foreground.opaque.red);
        regExpErrorLabel.setText(" ");

        wholeWordCheck.setFont(wholeWordCheck.getFont().deriveFont(wholeWordCheck.getFont().getSize()-1f));
        wholeWordCheck.setSelected(Settings.getUser().getBoolean("find-whole-word"));
        wholeWordCheck.setText(string( "find-whole-word" )); // NOI18N
        wholeWordCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findOptionsChanged(evt);
            }
        });

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
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(regExpErrorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 617, Short.MAX_VALUE)
                            .addGroup(searchControlsPanelLayout.createSequentialGroup()
                                .addGroup(searchControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(findField, javax.swing.GroupLayout.DEFAULT_SIZE, 279, Short.MAX_VALUE)
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
                                    .addComponent(replaceField, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE))
                                .addGap(8, 8, 8))))
                    .addGroup(searchControlsPanelLayout.createSequentialGroup()
                        .addComponent(caseSensCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(wholeWordCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(regExpCheck)
                        .addGap(0, 0, Short.MAX_VALUE)))
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
                            .addComponent(caseSensCheck)
                            .addComponent(regExpCheck)
                            .addComponent(wholeWordCheck))))
                .addContainerGap())
        );

        KeyListener findListener = new KeyAdapter() {
            @Override
            public void keyTyped( final KeyEvent e ) {
                EventQueue.invokeLater( new Runnable() {
                    @Override
                    public void run() {
                        if( e.getKeyChar() == 27 ) { // Escape
                            findCloseBtnClicked(null);
                            return;
                        }
                        if( e.getSource() == findField && e.getKeyChar() != '\n' ) {
                            updateFindParameters();
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
        sideBarSplitter.setRightComponent(editor);

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
        final CodeEditorBase ed = getEditor();

        File file = getFile();
        boolean quote = getFileNameExtension().equals("js")
                || getFileNameExtension().equals("html") || getFileNameExtension().equals("css");
        boolean doubleQuote = getFileNameExtension().equals("java");

        ed.beginCompoundEdit();
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
            ed.endCompoundEdit();
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
            updateFindParameters();
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
            if (getCodeType().normalize().isRunnable()) {
                if (command == Commands.DEBUG_FILE) {
                    return ScriptDebugging.isInstalled();
                }
                return true;
            }
            return false;
        } else if (command == Commands.FORMAT_CODE) {
            return editor.getCodeSupport().createFormatter() != null;
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
        editor.setFile(f);
        boolean editable = false;
        if (f != null && type.getDeterminativeFile(f) == null && (!f.exists() || f.canWrite())) {
            editable = true;
        }
        setReadOnly(!editable);
    }

	private void findCloseBtnClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findCloseBtnClicked
            editor.endSearch();
            editor.requestFocusInWindow();

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
}//GEN-LAST:event_findCloseBtnClicked

    private boolean updateFindParameters() {
        if (!checkPattern()) {
            return false;
        }
        CodeEditorBase.Result result = getEditor().beginSearch(
                findField.getText(),
                caseSensCheck.isSelected(),
                wholeWordCheck.isSelected(),
                regExpCheck.isSelected(),
                true
        );
        describeResult(result);
        return true;
    }

    private void describeResult(CodeEditorBase.Result result) {
        String desc = " ";
        if (result != null && !findField.getText().isEmpty()) {
            if (!result.found) {
                desc = string("find-no-matches");
                getToolkit().beep();
            } else if (result.wrapped) {
                desc = string(result.wasForward ? "find-eof" : "find-tof");
            }
        }
        regExpErrorLabel.setText(desc);
    }
    
    private void findNext(boolean forward, boolean replace) {
        if (updateFindParameters()) {
            CodeEditorBase.Result result;
            if (replace) {
                result = getEditor().replaceNext(forward, replaceField.getText());
            } else {
                result = getEditor().findNext(forward);
            }
            describeResult(result);
        }
    }

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
                if (sideBarSplitter.isEnabled() && getNavigator() != null) {
                    int newSize = sideBarSplitter.getDividerLocation();
                    navSplitSize = newSize;
                    StrangeEonsEditor[] eds = StrangeEons.getWindow().getEditors();
                    for (int i = 0; i < eds.length; ++i) {
                        if (!(eds[i] instanceof CodeEditor)) {
                            continue;
                        }
                        CodeEditor ced = (CodeEditor) eds[i];
                        if (ced.sideBarSplitter.isEnabled() && ced.getNavigator() != null) {
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

    private void findOptionsChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findOptionsChanged
        final Settings s = Settings.getUser();
        s.setBoolean("find-case-sensitive", caseSensCheck.isSelected());
        s.setBoolean("find-whole-word", wholeWordCheck.isSelected());
        s.setBoolean("find-regular-expression", regExpCheck.isSelected());
        updateFindParameters();
    }//GEN-LAST:event_findOptionsChanged

    private void replaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceActionPerformed
        findNext(true, true);
    }//GEN-LAST:event_replaceActionPerformed

    private void replaceAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllActionPerformed
        if (updateFindParameters()) {
            getEditor().replaceAll(replaceField.getText());
        }
    }//GEN-LAST:event_replaceAllActionPerformed

    private void findNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findNextActionPerformed
        findNext(true, false);
    }//GEN-LAST:event_findNextActionPerformed

    private void findPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findPrevActionPerformed
        findNext(false, false);
    }//GEN-LAST:event_findPrevActionPerformed

    private static final String KEY_LAST_FIND = "last-find";
    private static final String KEY_LAST_REPLACE = "last-replace";

    public static final String KEY_SHOW_NAVIGATOR = "show-source-navigator";
    private static boolean navIsVisible = Settings.getShared().getYesNo(KEY_SHOW_NAVIGATOR);

    public static final String KEY_NAV_SIZE = "source-navigator-size";
    private static int navSplitSize = Settings.getShared().getInt(KEY_NAV_SIZE, 200);

    private static final long NAVIGATOR_SCAN_DELAY = 1000;

    private long lastNavUpdate;
    private int navIsChanging;

    private boolean checkPattern() {
        String patternText = findField.getText();
        Settings.getUser().set(KEY_LAST_FIND, patternText);
        Settings.getUser().set(KEY_LAST_REPLACE, replaceField.getText());

        regExpErrorLabel.setText(" ");
        if (errorHighlight != null) {
            findField.getHighlighter().removeHighlight(errorHighlight);
        }

        if (!regExpCheck.isSelected() || patternText.isEmpty()) {
            return true;
        }

        try {
            int flags = Pattern.MULTILINE | Pattern.UNICODE_CASE | Pattern.UNIX_LINES;
            if (!caseSensCheck.isSelected()) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            Pattern.compile(patternText, flags);
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
                    StrangeEons.log.log(Level.WARNING, "unexpected BLE formatting error", ble);
                }
            }
            findField.requestFocusInWindow();
            return false;
        }
        return true;
    }

    private Object errorHighlight;
    private static HighlightPainter ORANGE_SQUIGGLE = new ErrorSquigglePainter(new Color(0xcc5600));

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox caseSensCheck;
    private ca.cgjennings.apps.arkham.ToolCloseButton closeBtn;
    private ca.cgjennings.ui.textedit.CodeEditorBase editor;
    private javax.swing.JTextField findField;
    private javax.swing.JButton findNextBtn;
    private javax.swing.JPanel findPanel;
    private javax.swing.JButton findPrevBtn;
    private javax.swing.JLabel icon;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JList<NavigationPoint> navList;
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
    private javax.swing.JCheckBox wholeWordCheck;
    // End of variables declaration//GEN-END:variables

    public final CodeEditorBase getEditor() {
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
        processAfterWrite(f, text);
    }

    private void processAfterWrite(File source, String text) {
        if (source == null) {
            return;
        }
        if (type == TYPESCRIPT) {
            startedCodeGeneration();
            TSLanguageServices.getShared().transpile(source.getName(), text, transpiled -> {
                final File js = type.getDependentFile(source);
                try {
                    ProjectUtilities.writeTextFile(js, transpiled, ProjectUtilities.ENC_SCRIPT);
                    StrangeEons.log.fine("wrote transpiled code");
                } catch (IOException ex) {
                    StrangeEons.log.log(Level.SEVERE, "failed to write transpiled file", ex);
                }
                refreshDependentFiles(type, js);
                finishedCodeGeneration();
            });
        }
    }

    /**
     * Call to reload files that depend on this file and were changed when it
     * was saved. This is called immediately if
     * {@link CodeType#processAfterWrite} returns true. Otherwise it can be
     * called manually if processing completes in another thread.
     *
     * @param f the file for which editors should be reloaded
     */
    private void refreshDependentFiles(CodeType type, File f) {
        File generated = type.getDependentFile(f);
        if (generated != null) {
            StrangeEonsEditor[] showingGenerated = StrangeEons.getWindow().getEditorsShowingFile(generated);
            for (StrangeEonsEditor ed : showingGenerated) {
                if (ed instanceof CodeEditor) {
                    try {
                        ((CodeEditor) ed).refresh();
                    } catch (IOException ioe) {
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
        CodeEditor clone = new CodeEditor(getText(), type);
        clone.setFile(getFile());
        clone.encoding = encoding;
        clone.setUnsavedChanges(hasUnsavedChanges());
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
        tc.setForeground(Color.BLACK);
        tc.setBackground(Color.WHITE);        
        tc.setFont(getEditor().getFont().deriveFont(10f));
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
        HtmlStyler styler = new HtmlStyler(getCodeType());
        styler.setText(editor.getText());
        String html = "<!DOCTYPE html><html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'></head><body>"
                + styler.styleAll() + "</body></html>";
        ProjectUtilities.writeTextFile(f, html, TextEncoding.HTML_CSS);
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
     * {@link #isCharacterEscapingEnabled()} returns {@code true}, and if so, {@linkplain EscapedTextCodec#escapeUnicode(java.lang.CharSequence)
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
     * {@link #isCharacterEscapingEnabled()} returns {@code true}, and if so, {@linkplain EscapedTextCodec#unescapeUnicode
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
     * Returns {@code true} if character escaping is enabled on read and write
     * of the document. When enabled, the default behaviour of {@link #escape}
     * and {@link #unescape} is to automatically convert Java-style Unicode
     * escapeUnicode sequences in the file into their character equivalents when
     * displayed in the editor.
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
     * @param characterEscaping if {@code true}, automatic character escaping is
     * enabled
     * @see #isCharacterEscapingEnabled()
     * @see #escape
     * @see #unescape
     */
    public void setCharacterEscapingEnabled(boolean characterEscaping) {
        this.characterEscaping = characterEscaping;
    }

    /**
     * Returns a popup menu of actions that are appropriate for the editor type.
     *
     * @return a context menu populated with menu items for the current
     * selection and code type
     */
    protected JPopupMenu createPopupMenu(CodeEditorBase editor, JPopupMenu menu) {

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

        // insert items before default menu
        int pos = 0;

        if (isCommandApplicable(Commands.RUN_FILE)) {
            menu.insert(Commands.RUN_FILE, pos++);
            if (ScriptDebugging.isInstalled()) {
                menu.insert(Commands.DEBUG_FILE, pos++);
            }
            menu.insert(new JSeparator(), pos++);
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
                menu.insert(browse, pos++);
                menu.insert(new JSeparator(), pos++);
            }
        }
        
        if (isCommandApplicable(Commands.FORMAT_CODE)) {
            menu.insert(Commands.FORMAT_CODE, pos++);
            menu.insert(new JSeparator(), pos++);
        }

        return menu;
    }

    /**
     * Format the code in the code editor, if a suitable formatter is available.
     * Otherwise, do nothing.
     */
    public void format() {
        Formatter f = editor.getCodeSupport().createFormatter();
        if (f != null) {
            int line = editor.getCaretLine();
            String formatted = f.format(editor.getText());
            editor.setText(formatted);

            int offset;
            if (editor.getLineCount() <= line) {
                offset = editor.getLineStartOffset(editor.getLineOfOffset(editor.getLength()));
            } else {
                offset = editor.getLineStartOffset(line);
            }
            editor.setCaretPosition(offset);
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
        navigator = nav;
        if (navigator != null) {
            refreshNavigator();
        }
        updateNavigatorVisibility();
    }
    private static final int NAV_DIV_SIZE = 8;
    
    private void updateNavigatorVisibility() {
        if (navigator == null || !navIsVisible) {
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

    /**
     * Returns the current navigator for this editor, or {@code null} if none is
     * set.
     *
     * @return the current navigator
     */
    @Override
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
    @Override
    public void refreshNavigator() {
        if (!navIsVisible) return;
        refreshNavigator(getEditor().getText());
    }

    private void refreshNavigator(String text) {
        if (navigator == null || text == null) {
            refreshNavigator((List<NavigationPoint>) null);
        } else {
            List<NavigationPoint> navPoints = navigator.getNavigationPoints(text);            
            refreshNavigator(navPoints);
            if (navPoints == Navigator.ASYNC_RETRY) {
                StaggeredDelay.then(this::refreshNavigator);
            }
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

    private class NavModel extends AbstractListModel<NavigationPoint> {

        private List<NavigationPoint> points;

        public void setList(List<NavigationPoint> newList) {
            List<NavigationPoint> oldList = points;
            points = newList;
            if (oldList != null && !oldList.isEmpty()) {
                fireIntervalRemoved(this, 0, oldList.size() - 1);
            }
            if (newList != null && !newList.isEmpty()) {
                fireContentsChanged(this, 0, newList.size() - 1);
            }
        }

        @Override
        public int getSize() {
            return navigator == null ? 0 : (points == null ? 0 : points.size());
        }

        @Override
        public NavigationPoint getElementAt(int index) {
            if (navigator == null || points == null) {
                throw new IllegalArgumentException("" + index + ">=0");
            } else {
                return points.get(index);
            }
        }
    }

    /**
     * Returns {@code true} if code editors will display a source navigator when
     * one has been set. This setting applies to all code editors.
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
                ced.updateNavigatorVisibility();
                if (navIsVisible && ced.getNavigator() != null) {
                    StaggeredDelay.then(ced::refreshNavigator);
                }
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
        return getEditor().getLength();
    }

    private int activeCodeGenerationRequests = 0;
    private int pendingActionAfterCodeGeneration = 0;
    private static final int POST_GEN_NO_ACTION = 0;
    private static final int POST_GEN_RUN = 1;
    private static final int POST_GEN_DEBUG = 2;

    /**
     * Called after a save when code generation starts. Allows generated code to
     * be acted on once generation finishes, even if in another thread. Must be
     * called on EDT.
     */
    private void startedCodeGeneration() {
        if (!EventQueue.isDispatchThread()) {
            throw new AssertionError();
        }
        ++activeCodeGenerationRequests;
    }

    /**
     * Called after a save when code generation ends. Allows generated code to
     * be acted on once generation finishes, even if in another thread. Must be
     * called on EDT.
     */
    private void finishedCodeGeneration() {
        if (!EventQueue.isDispatchThread()) {
            throw new AssertionError();
        }
        if (activeCodeGenerationRequests > 0) {
            --activeCodeGenerationRequests;
            if (activeCodeGenerationRequests == 0) {
                int actionWas = pendingActionAfterCodeGeneration;
                pendingActionAfterCodeGeneration = POST_GEN_NO_ACTION;
                switch (actionWas) {
                    case POST_GEN_RUN:
                        run(false);
                        break;
                    case POST_GEN_DEBUG:
                        run(true);
                        break;
                    case POST_GEN_NO_ACTION:
                        break;
                    default:
                        throw new AssertionError();
                }
            }
        } else {
            throw new AssertionError();
        }
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
            if (activeCodeGenerationRequests > 0) {
                StrangeEons.log.fine("deferring code execution until generation complete");
                pendingActionAfterCodeGeneration = debugIfAvailable ? POST_GEN_DEBUG : POST_GEN_RUN;
                return;
            }
            if (p != null) {
                m = p.findMember(f);
                if (m != null) {
                    t = m.getTask();
                }
            }
            
            File transpiled = type.getDependentFile(f);
            if (transpiled == null) transpiled = f;
            
            try {
                ProjectUtilities.runScript(transpiled, p, t, m, debugIfAvailable);
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

        CodeEditorBase ed = getEditor();
        ed.beginCompoundEdit();
        StrangeEons.setWaitCursor(true);
        try {
            if (ed.getSelectionStart() == ed.getSelectionEnd()) {
                ed.selectAll();
            }
            String[] lines = ed.getSelectedLineText(false);
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
            ed.endCompoundEdit();
            StrangeEons.setWaitCursor(false);
        }
    }
    private static SortDialog sortDialog;
}
