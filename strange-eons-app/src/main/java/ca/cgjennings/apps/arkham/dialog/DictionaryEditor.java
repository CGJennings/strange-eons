package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.spelling.Suggestion;
import ca.cgjennings.spelling.dict.BucketList;
import ca.cgjennings.spelling.dict.FrequencyAnalyzer;
import ca.cgjennings.spelling.dict.RankedList;
import ca.cgjennings.spelling.dict.TernaryTreeList;
import ca.cgjennings.spelling.dict.Tools;
import ca.cgjennings.spelling.dict.WordList;
import ca.cgjennings.spelling.policy.BasicPolicy;
import ca.cgjennings.spelling.policy.PolicyChain;
import ca.cgjennings.spelling.policy.WordPolicy;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.JFileField;
import ca.cgjennings.ui.JFileField.FileType;
import ca.cgjennings.ui.dnd.FileDrop;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Basic testing and editing of spelling dictionaries.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class DictionaryEditor extends javax.swing.JDialog implements AgnosticDialog {

    protected WordPolicy policy = new BasicPolicy();
    protected WordList wl;
    protected File f;

    /**
     * While the dialog is displayed, a named object with this name ("WordList")
     * will be available in the named object database. You can use this named
     * object to modify the word list from a script (for example, using the
     * Quickscript window). For example, the following snippet explicitly sets
     * the word frequency rank of the word "monkey" in a ranked list (.3tree):
     * <pre>
     * Eons.namedObjects['WordList'].setFrequencyRank( 'monkey', 252 );
     * </pre>
     */
    public static final String WORD_LIST_NAMED_OBJECT = "WordList";

    /**
     * Creates new form DictionaryEditor
     */
    public DictionaryEditor(java.awt.Frame parent, File f) throws IOException {
        super(parent, false);
        initComponents();
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        this.f = f;
        wl = Tools.read(f);

        if (!(wl instanceof RankedList)) {
            tabs.setEnabledAt(tabs.indexOfComponent(freqPanel), false);
        }

        StrangeEons.getApplication().getNamedObjects().putObject(WORD_LIST_NAMED_OBJECT, wl);

        setTitle(f.getName());

        testWordField.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateTestResults();
            }
        });

        new FileDrop(statList, null, false, (File[] files) -> {
            if (files == null) {
                return;
            }
            for (File f1 : files) {
                if (f1.isDirectory()) {
                    for (File ch : f1.listFiles()) {
                        if (ProjectUtilities.matchExtension(ch, "txt", "text", "utf8")) {
                            addStatFile(ch);
                        }
                    }
                } else {
                    addStatFile(f1);
                }
            }
        });

        initListField(operandField);
        initListField(misspellField);
        initEquivField(equivField);
    }

    private void initListField(JFileField ff) {
        ff.setFileType(FileType.GENERIC);
        ff.setGenericFileTypeDescription(string("dicted-listfile"));
        ff.setGenericFileTypeExtensions("cpl", "3tree");
    }

    private void initEquivField(JFileField ff) {
        ff.setFileType(FileType.GENERIC);
        ff.setGenericFileTypeDescription(string("dicted-equivfile"));
        ff.setGenericFileTypeExtensions("txt");
    }

    private void updateTestResults() {
        String word = testWordField.getText();
        Matcher m = ((Pattern) policy.getHint(WordPolicy.Hint.WORD_REGEX)).matcher(word);
        if (m.find()) {
            word = m.group(1);
        } else {
            word = "";
        }
        if (word.isEmpty()) {
            testOutputField.setText(word);
        } else {
            long time = System.nanoTime();
            Suggestion[] suggestions = wl.getSuggestions(policy, word, -1, -1);
            time = (System.nanoTime() - time) / 1_000_000;
            benchLabel.setText(string("dicted-l-benchmark", time));

            StringBuilder b = new StringBuilder(256);
            int n = 0;
            if (wl.contains(word)) {
                b.append(word);
                ++n;
            }
            for (Suggestion s : suggestions) {
                if (n++ > 0) {
                    b.append('\n');
                }
                b.append(s);
            }

            testOutputField.setText(b.toString());
            testOutputField.select(0, 0);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        tabs = new javax.swing.JTabbedPane();
        testPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        testWordField = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        testOutputField = new javax.swing.JTextArea();
        benchLabel = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        policyChainCombo = new javax.swing.JComboBox<>();
        jTip1 = new ca.cgjennings.ui.JTip();
        modifyPanel = new javax.swing.JPanel();
        operandField = new ca.cgjennings.ui.JFileField();
        mergeBtn = new javax.swing.JButton();
        subtractBtn = new javax.swing.JButton();
        intersectBtn = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        optimizeBtn = new javax.swing.JButton();
        jWarningLabel1 = new ca.cgjennings.ui.JWarningLabel();
        freqPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        statList = new javax.swing.JList<>();
        processStatsBtn = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        addStatFile = new javax.swing.JButton();
        remStatFiles = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        equivField = new ca.cgjennings.ui.JFileField();
        misspellField = new ca.cgjennings.ui.JFileField();
        jLabel3 = new javax.swing.JLabel();
        corpusFormatCombo = new javax.swing.JComboBox<>();
        indexPanel = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        indexList = new javax.swing.JList<>();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        progressField = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        helpBtn = new ca.cgjennings.ui.JHelpButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        tabs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabChanged(evt);
            }
        });

        jLabel2.setText(string( "dicted-l-test-info1" )); // NOI18N

        testWordField.setColumns(25);

        testOutputField.setEditable(false);
        testOutputField.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        jScrollPane3.setViewportView(testOutputField);

        benchLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        benchLabel.setText("     ");

        jLabel10.setText(string("dicted-l-policy")); // NOI18N

        policyChainCombo.setEditable(true);
        policyChainCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "BasicPolicy", "BasicPolicy, Policy_de", "BasicPolicy, Policy_en", "BasicPolicy, Policy_es", "BasicPolicy, Policy_fr", "BasicPolicy, Policy_it" }));
        policyChainCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                policyChainComboActionPerformed(evt);
            }
        });

        jTip1.setTipText(string("dicted-l-test-info2")); // NOI18N

        javax.swing.GroupLayout testPanelLayout = new javax.swing.GroupLayout(testPanel);
        testPanel.setLayout(testPanelLayout);
        testPanelLayout.setHorizontalGroup(
            testPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(testPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(testPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3)
                    .addGroup(testPanelLayout.createSequentialGroup()
                        .addGroup(testPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(testPanelLayout.createSequentialGroup()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(policyChainCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 333, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTip1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(testWordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)
                            .addComponent(benchLabel))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        testPanelLayout.setVerticalGroup(
            testPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(testPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(testWordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(benchLabel)
                .addGap(18, 18, 18)
                .addGroup(testPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel10)
                    .addComponent(policyChainCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTip1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        tabs.addTab(string( "dicted-l-test" ), testPanel); // NOI18N

        operandField.setFileType(null);

        mergeBtn.setFont(mergeBtn.getFont().deriveFont(mergeBtn.getFont().getSize()-1f));
        mergeBtn.setIcon( ResourceKit.getIcon( "toolbar/cag-or.png" ) );
        mergeBtn.setText(string( "dicted-b-op-merge" )); // NOI18N
        mergeBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        mergeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mergeBtnActionPerformed(evt);
            }
        });

        subtractBtn.setFont(subtractBtn.getFont().deriveFont(subtractBtn.getFont().getSize()-1f));
        subtractBtn.setIcon( ResourceKit.getIcon( "toolbar/cag-exclude.png" ) );
        subtractBtn.setText(string( "dicted-b-op-subtract" )); // NOI18N
        subtractBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        subtractBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subtractBtnActionPerformed(evt);
            }
        });

        intersectBtn.setFont(intersectBtn.getFont().deriveFont(intersectBtn.getFont().getSize()-1f));
        intersectBtn.setIcon( ResourceKit.getIcon( "toolbar/cag-and.png" ) );
        intersectBtn.setText(string( "dicted-b-op-intersect" )); // NOI18N
        intersectBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        intersectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intersectBtnActionPerformed(evt);
            }
        });

        jLabel5.setText(string( "dicted-l-op" )); // NOI18N

        jLabel8.setText(string( "dicted-l-modify" )); // NOI18N

        optimizeBtn.setText(string( "dicted-b-optimize" )); // NOI18N
        optimizeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optimizeBtnActionPerformed(evt);
            }
        });

        jWarningLabel1.setText(string("dicted-l-modify-info")); // NOI18N

        javax.swing.GroupLayout modifyPanelLayout = new javax.swing.GroupLayout(modifyPanel);
        modifyPanel.setLayout(modifyPanelLayout);
        modifyPanelLayout.setHorizontalGroup(
            modifyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modifyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(modifyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addGroup(modifyPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(modifyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(operandField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                            .addGroup(modifyPanelLayout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(modifyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(modifyPanelLayout.createSequentialGroup()
                                        .addComponent(mergeBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(subtractBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(intersectBtn))
                                    .addComponent(jWarningLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addComponent(optimizeBtn))
                .addContainerGap())
        );

        modifyPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {intersectBtn, mergeBtn, subtractBtn});

        modifyPanelLayout.setVerticalGroup(
            modifyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modifyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addGap(4, 4, 4)
                .addComponent(operandField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(modifyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel5)
                    .addComponent(mergeBtn)
                    .addComponent(subtractBtn)
                    .addComponent(intersectBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jWarningLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 191, Short.MAX_VALUE)
                .addComponent(optimizeBtn)
                .addContainerGap())
        );

        tabs.addTab(string( "dicted-t-modify" ), modifyPanel); // NOI18N

        statList.setModel( new DefaultListModel<File>() );
        jScrollPane1.setViewportView(statList);

        processStatsBtn.setText(string( "dicted-b-process-stats" )); // NOI18N
        processStatsBtn.setEnabled(false);
        processStatsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processStatsBtnActionPerformed(evt);
            }
        });

        jLabel1.setText(string( "dicted-l-stats-info" )); // NOI18N

        addStatFile.setIcon( ResourceKit.getIcon( "ui/button/plus.png" ) );
        addStatFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStatFileActionPerformed(evt);
            }
        });

        remStatFiles.setIcon( ResourceKit.getIcon( "ui/button/minus.png" ) );
        remStatFiles.setEnabled(false);
        remStatFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remStatFilesActionPerformed(evt);
            }
        });

        jLabel6.setText(string( "dicted-l-misspelling" )); // NOI18N

        jLabel7.setText(string( "dicted-l-equivalencies" )); // NOI18N

        jLabel3.setText(string("dicted-freq-fmt")); // NOI18N

        corpusFormatCombo.setModel( new javax.swing.DefaultComboBoxModel<String>( new String[] { string("dicted-freq-fmt-0"), string("dicted-freq-fmt-1"), string("dicted-freq-fmt-2") } ) );

        javax.swing.GroupLayout freqPanelLayout = new javax.swing.GroupLayout(freqPanel);
        freqPanel.setLayout(freqPanelLayout);
        freqPanelLayout.setHorizontalGroup(
            freqPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(freqPanelLayout.createSequentialGroup()
                .addGroup(freqPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(freqPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(freqPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel6)
                            .addComponent(jLabel7)
                            .addGroup(freqPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(freqPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(freqPanelLayout.createSequentialGroup()
                                        .addComponent(addStatFile)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(remStatFiles)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel3)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(corpusFormatCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)))))
                    .addGroup(freqPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(equivField, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE))
                    .addGroup(freqPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(misspellField, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, freqPanelLayout.createSequentialGroup()
                        .addContainerGap(347, Short.MAX_VALUE)
                        .addComponent(processStatsBtn)))
                .addContainerGap())
        );
        freqPanelLayout.setVerticalGroup(
            freqPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(freqPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(equivField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(misspellField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 143, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(freqPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(remStatFiles)
                    .addComponent(addStatFile)
                    .addGroup(freqPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(corpusFormatCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(processStatsBtn)
                .addContainerGap())
        );

        tabs.addTab(string( "dicted-l-stats-tab" ), freqPanel); // NOI18N

        indexPanel.setLayout(new java.awt.GridBagLayout());

        jScrollPane4.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        indexList.setModel( emptyModel );
        indexList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane4.setViewportView(indexList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        indexPanel.add(jScrollPane4, gridBagConstraints);

        tabs.addTab(string( "dicted-l-index" ), indexPanel); // NOI18N

        cancelBtn.setText(string( "close" )); // NOI18N

        okBtn.setText(string( "save" )); // NOI18N

        progressField.setEditable(false);
        progressField.setColumns(20);
        progressField.setFont(progressField.getFont().deriveFont(progressField.getFont().getSize()-1f));
        progressField.setLineWrap(true);
        progressField.setRows(6);
        progressField.setTabSize(4);
        progressField.setWrapStyleWord(true);
        jScrollPane2.setViewportView(progressField);

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel4.setText(string( "dicted-l-output" )); // NOI18N
        jLabel4.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        helpBtn.setHelpPage("tm-spelling");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabs)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(tabs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn)
                    .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {helpBtn, okBtn});

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void addStatFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStatFileActionPerformed
            File f = ResourceKit.showGenericOpenDialog(this, null, "UTF-8", "txt", "text", "utf8");
            if (f != null) {
                addStatFile(f);
            }
	}//GEN-LAST:event_addStatFileActionPerformed

	private void remStatFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remStatFilesActionPerformed
            DefaultListModel<File> m = (DefaultListModel<File>) statList.getModel();
            if (statList.getSelectedIndex() < 0 && m.getSize() > 0) {
                statList.getSelectionModel().addSelectionInterval(0, m.getSize() - 1);
            }
            int[] sel = statList.getSelectedIndices();
            for (int i = sel.length - 1; i >= 0; --i) {
                m.remove(sel[i]);
            }

            if (m.getSize() == 0) {
                processStatsBtn.setEnabled(true);
                remStatFiles.setEnabled(true);
            }
	}//GEN-LAST:event_remStatFilesActionPerformed

    private Locale inferLocaleFromPolicyChain() {
        Locale loc = Locale.getDefault();

        Object selPolicy = policyChainCombo.getSelectedItem();
        if (selPolicy != null) {
            String[] policies = selPolicy.toString().split(",");
            for (int i = policies.length - 1; i >= 0; --i) {
                String policyClass = policies[i].trim();
                int lastDot = policyClass.lastIndexOf('.');
                if (lastDot >= 0) {
                    policyClass = policyClass.substring(lastDot + 1);
                }
                if (policyClass.startsWith("Policy_")) {
                    policyClass = policyClass.substring("Policy_".length());
                    if (!policyClass.isEmpty()) {
                        loc = Language.parseLocaleDescription(policyClass);
                        StrangeEons.log.log(Level.INFO, "inferred locale from policy chain: {0}", loc);
                        break;
                    }
                }
            }
        }

        return loc;
    }

    private void addStatFile(File f) {
        if (f == null) {
            throw new NullPointerException("file");
        }
        DefaultListModel<File> m = (DefaultListModel<File>) statList.getModel();
        m.addElement(f);
        processStatsBtn.setEnabled(true);
        remStatFiles.setEnabled(true);
    }

	private void processStatsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processStatsBtnActionPerformed
            final ListModel<File> m = statList.getModel();
            if (m.getSize() == 0) {
                return;
            }

            Thread t = new Thread() {
                @Override
                public void run() {
                    RankedList rl = (RankedList) wl;
                    FrequencyAnalyzer fa = new FrequencyAnalyzer(rl);
                    fa.setPolicy(policy);

                    int selProc = corpusFormatCombo.getSelectedIndex();
                    FrequencyAnalyzer.CorpusProcessor proc;
                    switch (selProc) {
                        case 1:
                            proc = new FrequencyAnalyzer.ColumnDataFileProcessor(0, 1);
                            break;
                        case 2:
                            proc = new FrequencyAnalyzer.ColumnDataFileProcessor(0, 2);
                            break;
                        default:
                            proc = new FrequencyAnalyzer.ProseProcessor();
                            break;
                    }
                    fa.setCorpusProcessor(proc);
                    fa.setLocale(inferLocaleFromPolicyChain());

                    try {

                        File equivFile = equivField.getFile();
                        if (equivFile != null) {
                            fa.processEquivalencies(equivFile);
                        }

                        File mispFile = misspellField.getFile();
                        if (mispFile != null) {
                            WordList wl = Tools.read(mispFile);
                            fa.setMisspellingList(Tools.listToSet(wl));
                        }

                        for (int i = 0; i < m.getSize(); ++i) {
                            File f = (File) m.getElementAt(i);
                            postMessage(f.toString() + "...", false);
                            fa.process(f);
                        }

                        fa.applyRanks();

                        postMessage(string("dicted-op-freqcount", fa.getStatTotalWordCount(), fa.getStatUniqueWordCount()));
                        postMessage(string("dicted-op-groupcount", fa.getStatPotentialGroups()));

                    } catch (IOException e) {
                        postMessage(e.toString(), false);
                        rl.clearFrequencyRanks();
                    } finally {
                        endOperation();
                    }
                }
            };
            beginOperation();
            t.start();
	}//GEN-LAST:event_processStatsBtnActionPerformed

    private void beginOperation() {
        okBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        getGlassPane().setVisible(true);
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressField.setText("");
    }

    private void endOperation() {
        postMessage(string("dicted-finished"), true);
    }

    private void applyOp(final int opcode) {
        final File src = operandField.getFile();
        if (src == null) {
            operandField.getToolkit().beep();
            return;
        }

        Thread t = new Thread() {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                try {
                    postMessage(string("dicted-op-size", wl.getWordCount()));
                    postMessage(src.toString() + "...");
                    WordList operand = Tools.read(src);
                    postMessage(string("dicted-op-opsize", operand.getWordCount()));

                    Set<String> lhsSet = Tools.listToSet(wl);
                    Set<String> rhsSet = Tools.listToSet(operand);
                    Set<String> result;

                    switch (opcode) {
                        case 0:
                            result = Tools.union(lhsSet, rhsSet);
                            break;
                        case 1:
                            result = Tools.subtract(lhsSet, rhsSet);
                            break;
                        case 2:
                            result = Tools.intersect(lhsSet, rhsSet);
                            break;
                        default:
                            throw new AssertionError("opcode:" + opcode);
                    }

                    postMessage(string("dicted-op-size", result.size()));

                    WordList out;
                    if (wl instanceof BucketList) {
                        out = new BucketList();
                    } else if (wl instanceof TernaryTreeList) {
                        out = new TernaryTreeList();
                    } else {
                        throw new AssertionError("unknown list type");
                    }

                    out.addAll(Tools.setToArray(result));
                    wl = out;
                } catch (IOException e) {
                    postMessage(e.toString());
                } finally {
                    endOperation();
                }
            }
        };

        beginOperation();
        t.start();
    }

	private void mergeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mergeBtnActionPerformed
            applyOp(0);
	}//GEN-LAST:event_mergeBtnActionPerformed

	private void subtractBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subtractBtnActionPerformed
            applyOp(1);
	}//GEN-LAST:event_subtractBtnActionPerformed

	private void intersectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intersectBtnActionPerformed
            applyOp(2);
	}//GEN-LAST:event_intersectBtnActionPerformed

	private void tabChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabChanged
            if (tabs.getSelectedComponent() == indexPanel) {
                if (indexList.getModel() == emptyModel) {
                    beginOperation();
                    List<String> wordList = wl.getWords();
                    String[] words = wordList.toArray(String[]::new);
                    Arrays.sort(words);
                    DefaultListModel<String> index = new DefaultListModel<>();

                    if (wl.hasFrequencyRanks()) {
                        for (String w : words) {
                            index.addElement(w + " [F" + wl.getFrequencyRank(w) + ']');
                        }
                    } else {
                        for (String w : words) {
                            index.addElement(w);
                        }
                    }
                    indexList.setModel(index);
                    postMessage(string("dicted-op-size", index.getSize()), true);
                }
            } else {
                if (indexList.getModel() != emptyModel) {
                    indexList.setModel(emptyModel);
                }
            }
	}//GEN-LAST:event_tabChanged

	private void optimizeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optimizeBtnActionPerformed
            beginOperation();
            try {
                postMessage(string("dicted-op-size", wl.getWordCount()));
                postMessage(string("dicted-op-optimizing"));
                Set<String> set = Tools.listToSet(wl);
                Tools.removeDuplicates(set);
                WordList out;
                if (wl instanceof BucketList) {
                    out = new BucketList();
                } else if (wl instanceof TernaryTreeList) {
                    out = new TernaryTreeList();
                } else {
                    throw new AssertionError("unknown list type");
                }

                out.addAll(Tools.setToArray(set));
                wl = out;
                postMessage(string("dicted-op-size", wl.getWordCount()));
            } finally {
                endOperation();
            }
	}//GEN-LAST:event_optimizeBtnActionPerformed

    private void policyChainComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_policyChainComboActionPerformed
        Object selObj = policyChainCombo.getSelectedItem();
        if (selObj == null || !isVisible()) {
            return;
        }

        String classList = selObj.toString().trim();

        try {
            String[] classes = classList.split(",");
            WordPolicy[] policies = new WordPolicy[classes.length];
            for (int i = 0; i < classes.length; ++i) {
                classes[i] = classes[i].trim();
                if (classes[i].isEmpty()) {
                    classes[i] = "BasicPolicy";
                }
                if (classes[i].indexOf('.') < 0) {
                    classes[i] = "ca.cgjennings.spelling.policy." + classes[i];
                }
                policies[i] = (WordPolicy) Class.forName(classes[i]).getConstructor().newInstance();
            }
            policy = PolicyChain.createPolicyChain(policies);
            updateTestResults();
        } catch (Throwable t) {
            ErrorDialog.displayError(string("dicted-l-policy-err"), t);
        }
    }//GEN-LAST:event_policyChainComboActionPerformed

    private final DefaultListModel<String> emptyModel = new DefaultListModel<>();

    private void postMessage(String message) {
        postMessage(message, false);
    }

    private void postMessage(final String message, final boolean done) {
        EventQueue.invokeLater(() -> {
            if (done) {
                okBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                getGlassPane().setVisible(false);
                getGlassPane().setCursor(Cursor.getDefaultCursor());
            }

            Document doc = progressField.getDocument();
            try {
                doc.insertString(doc.getLength(), message + "\n", null);
            } catch (BadLocationException ex) {
                throw new AssertionError();
            }

            progressField.select(doc.getLength(), doc.getLength());
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addStatFile;
    private javax.swing.JLabel benchLabel;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JComboBox<String> corpusFormatCombo;
    private ca.cgjennings.ui.JFileField equivField;
    private javax.swing.JPanel freqPanel;
    private ca.cgjennings.ui.JHelpButton helpBtn;
    private javax.swing.JList<String> indexList;
    private javax.swing.JPanel indexPanel;
    private javax.swing.JButton intersectBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private ca.cgjennings.ui.JTip jTip1;
    private ca.cgjennings.ui.JWarningLabel jWarningLabel1;
    private javax.swing.JButton mergeBtn;
    private ca.cgjennings.ui.JFileField misspellField;
    private javax.swing.JPanel modifyPanel;
    private javax.swing.JButton okBtn;
    private ca.cgjennings.ui.JFileField operandField;
    private javax.swing.JButton optimizeBtn;
    private javax.swing.JComboBox<String> policyChainCombo;
    private javax.swing.JButton processStatsBtn;
    private javax.swing.JTextArea progressField;
    private javax.swing.JButton remStatFiles;
    private javax.swing.JList<File> statList;
    private javax.swing.JButton subtractBtn;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JTextArea testOutputField;
    private javax.swing.JPanel testPanel;
    private javax.swing.JTextField testWordField;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent evt) {
        beginOperation();
        try {
            if (wl instanceof TernaryTreeList) {
                // may throw AssertionError if sanity check fails
                ((TernaryTreeList) wl).deduplicate();
            }
            wl.write(f, 9);
            dispose();
        } catch (Exception e) {
            postMessage(e.toString());
            getToolkit().beep();
        } finally {
            endOperation();
        }
    }

    @Override
    public void handleCancelAction(ActionEvent evt) {
        dispose();
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) {
            StrangeEons.getApplication().getNamedObjects().removeObject(WORD_LIST_NAMED_OBJECT);
        }
        super.setVisible(visible);
    }
}
