package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.editors.AbbreviationTableManager;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.textedit.AbbreviationTable;
import ca.cgjennings.ui.textedit.CodeType;
import gamedata.Game;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * An editor for {@link AbbreviationTable}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class AbbreviationEditor extends javax.swing.JDialog implements AgnosticDialog {

    private final boolean markupMode;

    /**
     * Creates a new abbreviation editor.
     */
    public AbbreviationEditor(java.awt.Frame parent, boolean markupMode) {
        super(parent, true);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        if (markupMode) {
            codeTypeCombo.setVisible(false);
            codeTypeLabel.setText(string("abrv-l-game"));
            syntaxTip.setTipText(string("abrv-l-text-tip2"));
        } else {
            gameCombo.setVisible(false);
        }
        this.markupMode = markupMode;

        loadTables();

        abrvTable.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            int row = abrvTable.getSelectedRow();

            if (expField.hasFocus()) {
                expFieldFocusLost(null);
            }

            boolean enable;
            if (row < 0) {
                enable = false;
                expField.setText("");
            } else {
                enable = true;
                expField.setText(abrvTable.getModel().getValueAt(row, 1).toString());
            }
            expLabel.setEnabled(enable);
            expField.setEnabled(enable);
            if (enable) {
                expField.select(0, 0);
                expField.requestFocusInWindow();
            }
            delBtn.setEnabled(enable);
            previousSel = row;
        });

        previewCode.setFont(expField.getFont());
        previewCode.setWhitespaceVisible(true);

        expField.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                if (expField.isEnabled()) {
                    AbbreviationTable at = new AbbreviationTable();
                    at.put("x", expField.getText());
                    previewCode.setAbbreviationTable(at);
                    previewCode.setEditable(true);
                    previewCode.setText("x");                    
                    previewCode.type(KeyStroke.getKeyStroke("TAB"));
                    previewCode.setEditable(false);
                    previewCode.clearUndoHistory();
                } else {
                    previewCode.setText("");
                }
            }
        });

        pack();
        setLocationRelativeTo(parent);
        installPreviewCodeVisibleSelectionWorkaround();
    }

    private void installPreviewCodeVisibleSelectionWorkaround() {
        // workaround: editor preview will not show selection until it is
        // focused at least once        
        workaroundListener = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                previewCode.removeFocusListener(workaroundListener);
                workaroundListener = null;
                if (abrvTable.getRowCount() > 0) {
                    abrvTable.addRowSelectionInterval(0, 0);
                } else {
                    expField.requestFocusInWindow();
                }
            }
        };
        previewCode.addFocusListener(workaroundListener);
        previewCode.requestFocusInWindow();
    }
    private FocusListener workaroundListener;

    private int previousSel = -1;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        codeTypeLabel = new javax.swing.JLabel();
        codeTypeCombo = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        abrvTable = new javax.swing.JTable();
        addBtn = new javax.swing.JButton();
        delBtn = new javax.swing.JButton();
        expLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        expField = new javax.swing.JTextArea();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        syntaxTip = new ca.cgjennings.ui.JTip();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        previewCode = new ca.cgjennings.ui.textedit.CodeEditorBase();
        jLabel3 = new javax.swing.JLabel();
        gameCombo = new ca.cgjennings.ui.JGameCombo();
        javax.swing.JLabel spacerLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("abrv-title")); // NOI18N

        codeTypeLabel.setLabelFor(codeTypeCombo);
        codeTypeLabel.setText(string("abrv-l-lang")); // NOI18N

        codeTypeCombo.setMaximumRowCount(16);
        codeTypeCombo.setRenderer( listRenderer );
        codeTypeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableComboActionPerformed(evt);
            }
        });

        abrvTable.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        abrvTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        abrvTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(abrvTable);

        addBtn.setIcon( ResourceKit.getIcon( "ui/button/plus.png" ) );
        addBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBtnActionPerformed(evt);
            }
        });

        delBtn.setIcon( ResourceKit.getIcon( "ui/button/minus.png" ) );
        delBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delBtnActionPerformed(evt);
            }
        });

        expLabel.setText(string("abrv-l-text")); // NOI18N

        expField.setColumns(20);
        expField.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        expField.setRows(5);
        expField.setTabSize(4);
        expField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                expFieldFocusLost(evt);
            }
        });
        jScrollPane2.setViewportView(expField);

        cancelBtn.setText(string("cancel")); // NOI18N

        okBtn.setText(string("save")); // NOI18N

        syntaxTip.setTipText(string("abrv-l-text-tip")); // NOI18N

        jLabel2.setText(string("abrv-l-preview")); // NOI18N

        jPanel1.setLayout(new java.awt.BorderLayout());

        previewCode.setCodeFoldingEnabled(false);
        previewCode.setContentFeedbackVisible(false);
        previewCode.setNumberLineVisible(false);
        jPanel1.add(previewCode, java.awt.BorderLayout.CENTER);

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getSize()-1f));
        jLabel3.setText(string("abrv-l-note")); // NOI18N

        gameCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tableComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(codeTypeLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(codeTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(spacerLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(addBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(delBtn))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(expLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(syntaxTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 10, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 186, Short.MAX_VALUE)
                                .addComponent(okBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cancelBtn)))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(codeTypeLabel)
                    .addComponent(codeTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spacerLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addBtn)
                    .addComponent(delBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(expLabel, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(syntaxTip, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okBtn, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cancelBtn, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tableComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tableComboActionPerformed
        Object sel;
        sel = (markupMode ? gameCombo : codeTypeCombo).getSelectedItem();

        if (sel != null) {
            abrvTable.setModel(tables.get(sel));
            TableColumn col = abrvTable.getColumnModel().getColumn(0);

            TableCellRenderer rend = abrvTable.getTableHeader().getDefaultRenderer();
            Component c = rend.getTableCellRendererComponent(abrvTable, col.getHeaderValue(), false, false, 0, 0);
            int width = c.getPreferredSize().width;
            col.setWidth(width);
            col.setMaxWidth(width);
            col.setMinWidth(width);
            col.setPreferredWidth(width);
            col.setResizable(false);

            if (!markupMode) {
                previewCode.setCodeType((CodeType) sel, false);
            }
        }
    }//GEN-LAST:event_tableComboActionPerformed

    private void delBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delBtnActionPerformed
        int row = abrvTable.getSelectedRow();
        ATModel m = (ATModel) abrvTable.getModel();
        m.remove(row);
    }//GEN-LAST:event_delBtnActionPerformed

    private void addBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBtnActionPerformed
        String name = JOptionPane.showInputDialog(addBtn, "New Abbreviation", "Add Abbreviation", JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.length() == 0) {
            return;
        }
        ATModel m = (ATModel) abrvTable.getModel();

        // if already in table, select it instead
        int rowToSelect = -1;
        for (int r = 0; r < m.getRowCount() && rowToSelect == -1; ++r) {
            if (m.getValueAt(r, 0).equals(name)) {
                rowToSelect = r;
            }
        }

        // not already in table, add it
        if (rowToSelect == -1) {
            rowToSelect = m.getRowCount();
            m.add(name);
        }

        abrvTable.setRowSelectionInterval(rowToSelect, rowToSelect);
        abrvTable.scrollRectToVisible(abrvTable.getCellRect(rowToSelect, 0, true));
        return;
    }//GEN-LAST:event_addBtnActionPerformed

    private void expFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_expFieldFocusLost
        String newExp = expField.getText();
        int row = previousSel; // abrvTable.getSelectedRow();
        if (row == -1) {
            return;
        }

        ATModel m = (ATModel) abrvTable.getModel();
        m.set(row, newExp);
    }//GEN-LAST:event_expFieldFocusLost

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable abrvTable;
    private javax.swing.JButton addBtn;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JComboBox<CodeType> codeTypeCombo;
    private javax.swing.JLabel codeTypeLabel;
    private javax.swing.JButton delBtn;
    private javax.swing.JTextArea expField;
    private javax.swing.JLabel expLabel;
    private ca.cgjennings.ui.JGameCombo gameCombo;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton okBtn;
    private ca.cgjennings.ui.textedit.CodeEditorBase previewCode;
    private ca.cgjennings.ui.JTip syntaxTip;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        JUtilities.showWaitCursor(this);
        try {
            for (Object o : tables.keySet()) {
                ATModel m = tables.get(o);
                if (m.isModified()) {
                    if (markupMode) {
                        saveModifiedTable((Game) o, m);
                    } else {
                        saveModifiedTable((CodeType) o, m);
                    }
                }
            }
            dispose();
        } catch (Exception ex) {
            ErrorDialog.displayError(string("abrv-err"), ex);
        } finally {
            JUtilities.hideWaitCursor(this);
        }
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    private void saveModifiedTable(Game g, ATModel m) throws IOException {
        AbbreviationTable at = new AbbreviationTable();
        for (int i = 0; i < m.getRowCount(); ++i) {
            at.put(
                    m.getValueAt(i, 0),
                    m.getValueAt(i, 1)
            );
        }
        AbbreviationTableManager.saveUserTable(g.getCode(), at);
    }

    private void saveModifiedTable(CodeType t, ATModel m) throws IOException {
        AbbreviationTable at = new AbbreviationTable();
        for (int i = 0; i < m.getRowCount(); ++i) {
            at.put(
                    m.getValueAt(i, 0),
                    m.getValueAt(i, 1).replace("\n${INDENT}", "${LINE}")
            );
        }
        AbbreviationTableManager.saveUserTable(t, at);
    }

    private static class ATModel extends AbstractTableModel {

        private boolean modified = false;
        private final ArrayList<String> abrvs = new ArrayList<>(30);
        private final ArrayList<String> exps = new ArrayList<>(30);

        public ATModel(AbbreviationTable at) {
            if (at != null) {
                String[] keys = at.keySet().toArray(new String[at.size()]);
                java.util.Arrays.sort(keys, Language.getInterface().getCollator());
                for (String k : keys) {
                    abrvs.add(k);
                    exps.add(at.get(k).replace("${LINE}", "\n${INDENT}"));
                }
            }
        }

        @Override
        public int getRowCount() {
            return abrvs.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return string(column == 0 ? "abrv-l-abrv" : "abrv-l-text");
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return abrvs.get(rowIndex);
            } else {
                return exps.get(rowIndex);
            }
        }

        public void add(String name) {
            int row = abrvs.size();
            abrvs.add(name);
            exps.add("");
            fireTableRowsInserted(row, row);
            modified = true;
        }

        public void remove(int index) {
            abrvs.remove(index);
            exps.remove(index);
            fireTableRowsDeleted(index, index);
            modified = true;
        }

        public void set(int index, String exp) {
            if (exp == null) {
                throw new NullPointerException("exp");
            }
            if (!exps.get(index).equals(exp)) {
                exps.set(index, exp);
                fireTableCellUpdated(index, 1);
                modified = true;
            }
        }

        public boolean isModified() {
            return modified;
        }
    }

    private HashMap<Object, ATModel> tables = new HashMap<>();

    private void loadTables() {
        if (markupMode) {
            loadGameTables();
        } else {
            loadCodeTables();
        }
    }

    private void loadGameTables() {
        Game sel = null;
        StrangeEonsAppWindow window = StrangeEons.getWindow();
        if (window != null) {
            StrangeEonsEditor ed = window.getActiveEditor();
            if (ed != null) {
                GameComponent gc = ed.getGameComponent();
                if (gc != null) {
                    sel = Game.get(gc.getSettings().get(Game.GAME_SETTING_KEY));
                }
            }
        }

        ComboBoxModel<Game> model = gameCombo.getModel();
        for (int i = 0; i < model.getSize(); ++i) {
            Game g = model.getElementAt(i);
            tables.put(g, new ATModel(AbbreviationTableManager.getTable(g.getCode())));
        }
        if (sel != null) {
            gameCombo.setSelectedItem(sel);
        } else {
            gameCombo.setSelectedIndex(0);
        }
    }

    private void loadCodeTables() {
        CodeType sel = CodeType.JAVASCRIPT;
        CodeType active = null;

        StrangeEonsAppWindow window = StrangeEons.getWindow();
        if (window != null) {
            StrangeEonsEditor ed = window.getActiveEditor();
            if (ed instanceof CodeEditor) {
                active = ((CodeEditor) ed).getCodeType();
            }
        }

        DefaultComboBoxModel<CodeType> model = new DefaultComboBoxModel<>();
        for (CodeType t : CodeType.values()) {
            if (AbbreviationTableManager.isCodeTypeMapped(t)) {
                continue;
            }
            model.addElement(t);
            if (t == active) {
                sel = active;
            }
            tables.put(t, new ATModel(AbbreviationTableManager.getTable(t)));
        }
        codeTypeCombo.setModel(model);
        codeTypeCombo.setSelectedItem(sel);
    }

    private final DefaultListCellRenderer listRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            CodeType t = (CodeType) value;
            super.getListCellRendererComponent(list, t.getDescription(), index, isSelected, cellHasFocus);
            setIcon(t.getIcon());
            return this;
        }
    };
}
