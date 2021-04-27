package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.Length;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.JUtilities;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Dialog for creating and editing custom paper sizes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class CustomPaperDialog extends javax.swing.JDialog implements AgnosticDialog {

    private NumberFormat formatter;
    private PaperProperties paperDefaults;
    // sizes for the paper currently being edited
    private double width, height, margin, grid;
    private boolean physicalOnly;

    /**
     * Creates a new custom paper dialog.
     */
    public CustomPaperDialog(Component parent, PaperProperties defaultPaper, boolean physicalPapersOnly) {
        super(parent == null ? StrangeEons.getWindow() : SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        physicalOnly = physicalPapersOnly;

        initComponents();
        getRootPane().setDefaultButton(okBtn);
        formatter = NumberFormat.getNumberInstance();
        AbstractGameComponentEditor.localizeComboBoxLabels(unitCombo, null);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        paperDefaults = defaultPaper;
        loadCustomPapers(defaultPaper);
        unitCombo.setSelectedIndex(Length.getDefaultUnit());

        // disable paper panel if nothing selected
        paperListValueChanged(null);

        setLocationRelativeTo(parent == null ? StrangeEons.getWindow() : parent);
    }

    private void loadCustomPapers(PaperProperties select) {
        // this set will include both portrait and landscape for each size;
        // we'll keep only the portrait versions
        Set<PaperProperties> set = PaperSets.getUserDefinedPapers();
        if (select == null) {
            select = PaperSets.getDefaultPaper(set);
            if (!select.isPortraitOrientation()) {
                select = select.deriveOrientation(PaperProperties.PORTRAIT);
            }
        } else {
            if (!select.isPortraitOrientation()) {
                select = select.deriveOrientation(PaperProperties.PORTRAIT);
            }
            select = PaperSets.findBestPaper(select, set);
        }

        int selectIndex = -1;
        int i = 0;
        DefaultListModel m = new DefaultListModel();
        for (PaperProperties pp : set) {
            if (!pp.isPortraitOrientation()) {
                continue;
            }
            if (pp.equals(select)) {
                selectIndex = i;
            }
            m.addElement(new PaperWrapper(pp));
            ++i;
        }

        paperList.setModel(m);
        paperList.setSelectedIndex(selectIndex);
    }

    private void saveCustomPapers() {
        ListModel m = paperList.getModel();
        LinkedHashSet<PaperProperties> unwrapped = new LinkedHashSet<>(m.getSize());
        for (int i = 0; i < m.getSize(); ++i) {
            unwrapped.add(((PaperWrapper) m.getElementAt(i)).paper);
        }
        PaperSets.setUserDefinedPapers(unwrapped);
    }

    private double parseGently(JTextField field, double previousValue) {
        try {
            double value = unitToPts(formatter.parse(field.getText()).doubleValue());
            if (value > Deck.MAX_PAPER_SIZE) {
                value = Deck.MAX_PAPER_SIZE;
                fillField(field, value);
            }
            if (value > 0 && value < 10) {
                value = 0;
                fillField(field, value);
            }
            if (value >= 0d) {
                return value;
            }
        } catch (ParseException e) {
        }
        Toolkit.getDefaultToolkit().beep();
        field.selectAll();
        field.requestFocusInWindow();
        fillField(field, previousValue);
        return previousValue;
    }

    private double ptsToUnit(double pts) {
        return Length.convert(pts, Length.PT, unitCombo.getSelectedIndex());
    }

    private double unitToPts(double units) {
        return Length.convert(units, unitCombo.getSelectedIndex(), Length.PT);
    }

    private void copyFieldsToMembers() {
        width = parseGently(widthField, width);
        height = parseGently(heightField, height);
        margin = parseGently(marginField, margin);
        grid = parseGently(gridField, grid);
    }

    private void fillField(JTextField field, double value) {
        field.setText(formatter.format(ptsToUnit(value)));
    }

    private void copyMembersToFields() {
        fillField(widthField, width);
        fillField(heightField, height);
        fillField(marginField, margin);
        fillField(gridField, grid);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        paperList = new javax.swing.JList();
        addBtn = new javax.swing.JButton();
        remBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        paperPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        widthField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        heightField = new javax.swing.JTextField();
        unitCombo = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        marginField = new javax.swing.JTextField();
        gridField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        jHelpButton1 = new ca.cgjennings.ui.JHelpButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("de-paper-title")); // NOI18N

        paperList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        paperList.setCellRenderer( wrapperRenderer );
        paperList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                paperListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(paperList);

        addBtn.setIcon( ResourceKit.getIcon( "ui/button/plus.png" ) );
        addBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBtnActionPerformed(evt);
            }
        });

        remBtn.setIcon( ResourceKit.getIcon( "ui/button/minus.png" ) );
        remBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remBtnActionPerformed(evt);
            }
        });

        cancelBtn.setText(string("cancel")); // NOI18N

        okBtn.setText(string("de-paper-ok")); // NOI18N

        paperPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder("")));

        jLabel1.setLabelFor(nameField);
        jLabel1.setText(string("de-paper-name")); // NOI18N

        nameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameFieldActionPerformed(evt);
            }
        });
        nameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                nameFieldFocusLost(evt);
            }
        });

        jLabel2.setLabelFor(widthField);
        jLabel2.setText(string("de-paper-size")); // NOI18N

        widthField.setColumns(6);
        widthField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        widthField.setText("0");
        widthField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                measureActionPerformed(evt);
            }
        });
        widthField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                measureFocusLost(evt);
            }
        });

        jLabel3.setText("×");

        heightField.setColumns(6);
        heightField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        heightField.setText("0");
        heightField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                measureActionPerformed(evt);
            }
        });
        heightField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                measureFocusLost(evt);
            }
        });

        unitCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "iid-cb-unit0", "iid-cb-unit1", "iid-cb-unit2" }));
        unitCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitComboActionPerformed(evt);
            }
        });

        jLabel4.setLabelFor(marginField);
        jLabel4.setText(string("de-paper-margin")); // NOI18N

        jLabel5.setLabelFor(gridField);
        jLabel5.setText(string("de-l-grid")); // NOI18N

        marginField.setColumns(6);
        marginField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        marginField.setText("0");
        marginField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                measureActionPerformed(evt);
            }
        });
        marginField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                measureFocusLost(evt);
            }
        });

        gridField.setColumns(6);
        gridField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        gridField.setText("0");
        gridField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                measureActionPerformed(evt);
            }
        });
        gridField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                measureFocusLost(evt);
            }
        });

        javax.swing.GroupLayout paperPanelLayout = new javax.swing.GroupLayout(paperPanel);
        paperPanel.setLayout(paperPanelLayout);
        paperPanelLayout.setHorizontalGroup(
            paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paperPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(paperPanelLayout.createSequentialGroup()
                        .addGroup(paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gridField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(marginField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(paperPanelLayout.createSequentialGroup()
                                .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(nameField))
                .addContainerGap())
        );
        paperPanelLayout.setVerticalGroup(
            paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paperPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(marginField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paperPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(gridField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getSize()-2f));
        jLabel6.setText(string("de-paper-note1")); // NOI18N
        jLabel6.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jHelpButton1.setHelpPage("deck-pages#paper-size");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(paperPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jHelpButton1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(addBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(remBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addBtn, remBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(remBtn, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(addBtn, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(paperPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jHelpButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cancelBtn)
                        .addComponent(okBtn)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void unitComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitComboActionPerformed
            copyMembersToFields();
            paperList.repaint();
	}//GEN-LAST:event_unitComboActionPerformed

	private void addBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBtnActionPerformed
            PaperProperties defaultSource = paperDefaults;
            if (paperList.getSelectedIndex() >= 0) {
                defaultSource = ((PaperWrapper) paperList.getSelectedValue()).paper;
            }
            PaperProperties p = new PaperProperties(
                    string("de-paper-default-name"),
                    defaultSource.getPageWidth(),
                    defaultSource.getPageHeight(),
                    PaperProperties.PORTRAIT,
                    defaultSource.getMargin(),
                    defaultSource.getGridSeparation());
            PaperWrapper wrapper = new PaperWrapper(p);
            ((DefaultListModel) paperList.getModel()).addElement(wrapper);
            paperList.setSelectedValue(wrapper, true);
            nameField.selectAll();
            nameField.requestFocusInWindow();
	}//GEN-LAST:event_addBtnActionPerformed

	private void paperListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_paperListValueChanged
            if (isEditingIndex != -1) {
                updatePaper();
            }

            isEditingIndex = paperList.getSelectedIndex();

            // it is OK if all papers are deleted or there is no selection,
            // since the user may just want to update the paper list;
            // however, they must not be allowed to pick a virtual paper
            // if the physical paper flag was set
            boolean enableOK = true;

            if (isEditingIndex >= 0) {
                PaperProperties pp = ((PaperWrapper) paperList.getSelectedValue()).paper;
                if (physicalOnly && !pp.isPhysical()) {
                    enableOK = false;
                }
                isFillingInPaperPanel = true;
                try {
                    nameField.setText(pp.getName());
                    width = pp.getPageWidth();
                    height = pp.getPageHeight();
                    if (width > height) {
                        double temp = width;
                        width = height;
                        height = temp;
                    }
                    margin = pp.getMargin();
                    grid = pp.getGridSeparation();
                    copyMembersToFields();
                } finally {
                    isFillingInPaperPanel = false;
                }
                remBtn.setEnabled(true);
                JUtilities.enableTree(paperPanel, true);
            } else {
                remBtn.setEnabled(false);
                JUtilities.enableTree(paperPanel, false);

                nameField.setText("");
                widthField.setText("");
                heightField.setText("");
                marginField.setText("");
                gridField.setText("");
            }

            PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn).setEnabled(enableOK);

	}//GEN-LAST:event_paperListValueChanged

    private boolean isFillingInPaperPanel = false;
    private int isEditingIndex = -1;

    private void updatePaper() {
        if (isFillingInPaperPanel) {
            return;
        }

        if (isEditingIndex >= 0) {
            DefaultListModel model = (DefaultListModel) paperList.getModel();
            PaperProperties paper = new PaperProperties(
                    nameField.getText(), width, height, PaperProperties.PORTRAIT, margin, grid
            );
            model.set(isEditingIndex, new PaperWrapper(paper));
        }
        // } else { last paper was deleted }
    }

	private void measureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_measureActionPerformed
            copyFieldsToMembers();
            updatePaper();
	}//GEN-LAST:event_measureActionPerformed

	private void measureFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_measureFocusLost
            measureActionPerformed(null);
	}//GEN-LAST:event_measureFocusLost

	private void nameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nameFieldActionPerformed
            updatePaper();
	}//GEN-LAST:event_nameFieldActionPerformed

	private void nameFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_nameFieldFocusLost
            updatePaper();
	}//GEN-LAST:event_nameFieldFocusLost

	private void remBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remBtnActionPerformed
            final int index = paperList.getSelectedIndex();

            if (index >= 0) {
                paperListValueChanged(null);
                isEditingIndex = -1;
                DefaultListModel m = (DefaultListModel) paperList.getModel();
                m.remove(index);
                if (index < m.getSize()) {
                    paperList.setSelectedIndex(index);
                } else {
                    paperList.setSelectedIndex(index - 1);
                }
            }
	}//GEN-LAST:event_remBtnActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addBtn;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JTextField gridField;
    private javax.swing.JTextField heightField;
    private ca.cgjennings.ui.JHelpButton jHelpButton1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JTextField marginField;
    private javax.swing.JTextField nameField;
    private javax.swing.JButton okBtn;
    private javax.swing.JList paperList;
    private javax.swing.JPanel paperPanel;
    private javax.swing.JButton remBtn;
    private javax.swing.JComboBox unitCombo;
    private javax.swing.JTextField widthField;
    // End of variables declaration//GEN-END:variables

    /**
     * Wraps a PaperProperties with a toString that ignores the paper's
     * orientation. Used to populate the custom paper list.
     */
    private class PaperWrapper implements IconProvider {

        PaperProperties paper;

        public PaperWrapper(PaperProperties wrapped) {
            paper = wrapped;
        }

        @Override
        public String toString() {
            return paper.getName() + " ("
                    + formatter.format(ptsToUnit(paper.getPageWidth()))
                    + " \u00d7 "
                    + formatter.format(ptsToUnit(paper.getPageHeight()))
                    + " " + unitCombo.getSelectedItem() + ")";
        }

        @Override
        public Icon getIcon() {
            return paper.getIcon();
        }
    }

    private ListCellRenderer wrapperRenderer = new JIconList.IconRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            boolean enable = true;
            if (physicalOnly) {
                PaperProperties pp = ((PaperWrapper) value).paper;
                enable = pp.isPhysical();
            }
            setEnabled(enable);
            return this;
        }
    };

    @Override
    public void handleOKAction(ActionEvent e) {
        okResult = true;
        saveCustomPapers();
        Length.setDefaultUnit(unitCombo.getSelectedIndex());
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        okResult = false;
        Length.setDefaultUnit(unitCombo.getSelectedIndex());
        dispose();
    }

    public PaperProperties showDialog() {
        setVisible(true);
        PaperProperties p = null;
        if (okResult) {
            if (paperList.getSelectedIndex() >= 0) {
                p = ((PaperWrapper) paperList.getSelectedValue()).paper;
            }
        }
        return p;
    }
    private boolean okResult;
}
