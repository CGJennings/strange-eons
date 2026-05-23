package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.ui.JExpansionList;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.ListTransferHandler;
import ca.cgjennings.ui.ToggleSelectionModel;
import ca.cgjennings.ui.theme.Theme;
import gamedata.Expansion;
import gamedata.ExpansionSymbolTemplate;
import gamedata.Game;
import java.awt.Component;
import java.awt.Container;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import static resources.Language.string;
import resources.Settings;

/**
 * A dialog that provides an alternative method for selecting expansions for a
 * game component.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ExpansionSelectionDialog extends javax.swing.JDialog {

    private final AbstractGameComponentEditor<?> ed;
    private final Settings settings;
    private final Game game;
    private final JExpansionList expList;
    private Expansion[] selection;
    private StrangeEonsEditor.EditorListener editorListener;

    /**
     * Creates a new expansion selection dialog.
     */
    public ExpansionSelectionDialog(AbstractGameComponentEditor<?> ed) {
        super(editorFrame(ed), true);
        this.ed = ed;

        if (ed != null) {
            setModalityType(ModalityType.MODELESS);
            editorListener = new StrangeEonsEditor.EditorListener() {
                @Override
                public void editorSelected(StrangeEonsEditor editor) {
                }

                @Override
                public void editorDeselected(StrangeEonsEditor editor) {
                    dispose();
                }

                @Override
                public void editorClosing(StrangeEonsEditor editor) {
                    dispose();
                }

                @Override
                public void editorDetached(StrangeEonsEditor editor) {
                }

                @Override
                public void editorAttached(StrangeEonsEditor editor) {
                }
            };
            ed.addEditorListener(editorListener);
        }

        settings = ed.getGameComponent().getSettings();
        this.game = Game.get(settings.get(Game.GAME_SETTING_KEY, Game.ALL_GAMES_CODE));

        initComponents();
        getRootPane().setDefaultButton(okBtn);
        expList = (JExpansionList) expSelectionList;

        initListUI();
        initExpansionModel();
        initVariantModel();
        initUpdateDelayTimer();

        pack();
        JUtilities.snapToPointer(this);
    }

    private static JFrame editorFrame(AbstractGameComponentEditor<?> ed) {
        if (ed == null) {
            return StrangeEons.getWindow();
        }

        Container c = ed;
        while (!(c instanceof JFrame)) {
            c = c.getParent();
            if (c == null) {
                throw new AssertionError("not in a JFrame");
            }
        }
        return (JFrame) c;
    }

    private void initListUI() {
        expList.setCellRenderer(new JIconList.IconRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // fake cell selection if we are dragging and this was selected
                // before the drag started
                if (selection != null) {
                    for (Expansion e : selection) {
                        if (e == value) {
                            isSelected = true;
                        }
                    }
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        TransferHandler dragHandler = new ListTransferHandler() {

            @Override
            protected void exportDone(JComponent c, Transferable data, int action) {
                super.exportDone(c, data, action);

                if (selection != null) {
                    expList.setSelectedExpansions(selection);
                    selection = null;
                }
                if (action == TransferHandler.MOVE) {
                    redrawComponentPreview();
                }
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                if (c instanceof JList) {
                    selection = expList.getSelectedExpansions();

                    @SuppressWarnings("unchecked")
                    JList<Expansion> source = (JList<Expansion>) c;
                    int leadIndex = source.getSelectionModel().getLeadSelectionIndex();
                    if (leadIndex < 0) {
                        return null;
                    }
                    source.clearSelection();
                    source.addSelectionInterval(leadIndex, leadIndex);
                    return super.createTransferable(c);
                }
                return null;
            }

            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }
        };
        expList.setTransferHandler(dragHandler);
    }

    private void initExpansionModel() {
        expList.setGame(game, false);
        Expansion[] exps = Sheet.parseExpansionList(settings.getExpansionCode());
        expList.setSelectedExpansions(exps);
        expList.setVisibleRowCount(Math.max(4, Math.min(9, expList.getModel().getSize() + 1)));
    }

    private void initVariantModel() {
        DefaultComboBoxModel<JIconList.IconItem> m = new DefaultComboBoxModel<>();
        ExpansionSymbolTemplate est = game.getSymbolTemplate();
        for (int i = 0; i < est.getLogicalVariantCount(); ++i) {
            m.addElement(new JIconList.IconItem(est.getLogicalVariantName(i), est.getLogicalVariantIcon(i)));
        }
        variantCombo.setModel(m);

        int sel = 0;
        String var = settings.get(Expansion.VARIANT_SETTING_KEY, "0");
        try {
            sel = Integer.parseInt(var);
            if (sel < 0 || sel >= est.getLogicalVariantCount()) {
                sel = 0;
            }
        } catch (NumberFormatException e) {
            sel = Settings.yesNo(var) ? 1 : 0;
        }
        variantCombo.setSelectedIndex(sel);
    }

    private void initUpdateDelayTimer() {
        if (ed == null) {
            return;
        }
        updateDelayTimer = new Timer(1_000, (ActionEvent e) -> {
            JUtilities.showWaitCursor(ExpansionSelectionDialog.this);
            try {
                applyToComponent();
            } finally {
                JUtilities.hideWaitCursor(ExpansionSelectionDialog.this);
            }
        });
        updateDelayTimer.setRepeats(false);
    }
    private Timer updateDelayTimer;

    private void redrawComponentPreview() {
        if (updateDelayTimer != null && selection == null) {
            updateDelayTimer.restart();
            ed.setUnsavedChanges(true);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        okBtn = new javax.swing.JButton();
        variantCombo = new ca.cgjennings.ui.JIconComboBox<>();
        expScroll = new javax.swing.JScrollPane();
        expSelectionList =  new JExpansionList() ;
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("expsel-title")); // NOI18N

        okBtn.setText(string("close")); // NOI18N
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBtnActionPerformed(evt);
            }
        });

        variantCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                variantComboActionPerformed(evt);
            }
        });

        expScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        expSelectionList.setDragEnabled(true);
        expSelectionList.setDropMode(javax.swing.DropMode.INSERT);
        expSelectionList.setSelectionModel( new ToggleSelectionModel() );
        expSelectionList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                expSelectionListValueChanged(evt);
            }
        });
        expScroll.setViewportView(expSelectionList);

        jLabel1.setLabelFor(variantCombo);
        jLabel1.setText(string("expsel-l-variant")); // NOI18N

        jPanel1.setBackground(UIManager.getColor(Theme.MESSAGE_BACKGROUND));
        jPanel1.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        jLabel2.setBackground(UIManager.getColor(Theme.MESSAGE_BACKGROUND));
        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-1f));
        jLabel2.setForeground(UIManager.getColor(Theme.MESSAGE_FOREGROUND));
        jLabel2.setText(string("expsel-l-info")); // NOI18N
        jLabel2.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(expScroll)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(variantCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okBtn)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(expScroll)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(variantCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(okBtn)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void applyToComponent() {
        StringBuilder expCodes = new StringBuilder(32);
        DefaultListModel<Expansion> model = (DefaultListModel<Expansion>) expList.getModel();
        for (int i = 0; i < model.getSize(); ++i) {
            if (expList.isSelectedIndex(i)) {
                if (expCodes.length() > 0) {
                    expCodes.append(',');
                }
                expCodes.append(((Expansion) model.get(i)).getCode());
            }
        }
        if (expCodes.length() == 0) {
            expCodes.append(Expansion.getBaseGameExpansion().getCode());
        }
        settings.set(Expansion.EXPANSION_SETTING_KEY, expCodes.toString());

        int sel = Math.max(0, variantCombo.getSelectedIndex());
        settings.set(Expansion.VARIANT_SETTING_KEY, String.valueOf(sel));

        if (ed != null) {
            ed.redrawPreview();
        }
    }

    private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBtnActionPerformed
        applyToComponent();
        dispose();
    }//GEN-LAST:event_okBtnActionPerformed

    private void expSelectionListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_expSelectionListValueChanged
        redrawComponentPreview();
    }//GEN-LAST:event_expSelectionListValueChanged

    private void variantComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_variantComboActionPerformed
        redrawComponentPreview();
    }//GEN-LAST:event_variantComboActionPerformed

    @Override
    public void dispose() {
        if (ed != null) {
            ed.removeEditorListener(editorListener);
        }
        super.dispose();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane expScroll;
    private javax.swing.JList<Expansion> expSelectionList;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton okBtn;
    private ca.cgjennings.ui.JIconComboBox<JIconList.IconItem> variantCombo;
    // End of variables declaration//GEN-END:variables
}
