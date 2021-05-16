package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.deck.CustomPaperDialog;
import ca.cgjennings.apps.arkham.deck.PDFPrintSupport;
import ca.cgjennings.apps.arkham.deck.PaperProperties;
import ca.cgjennings.apps.arkham.deck.PaperSets;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.math.Interpolation;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;
import javax.swing.SwingUtilities;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Dialog that allows user to set options for printing standard game components
 * (but not decks or case books). This dialog is used by the base printing
 * implementation in {@link AbstractGameComponentEditor}. A number of
 * package-visible members allow fetching the requested settings; after the
 * modal dialog returns, the value of {@code ok} is {@code true} if
 * the user wants to proceed with printing.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 0.90
 */
@SuppressWarnings("serial")
class PrintSetupDialog extends javax.swing.JDialog implements AgnosticDialog {

    /**
     * Creates new form PrintSetupDialog
     */
    public PrintSetupDialog(Component parent) {
        super(parent == null ? StrangeEons.getWindow() : SwingUtilities.getWindowAncestor(parent), ModalityType.APPLICATION_MODAL);
        initComponents();
        getRootPane().setDefaultButton(printBtn);
        PlatformSupport.makeAgnosticDialog(this, printBtn, cancelBtn);

        initPaperList(null);

        if (!PDFPrintSupport.isAvailable()) {
            pdfBtn.setVisible(false);
            pdfBtn.setEnabled(false);
        }

        pack();
        setLocationRelativeTo(parent);
    }

    private void initPaperList(PaperProperties sel) {
        HashMap<Object, Object> attr = new HashMap<>();
        attr.put(PaperSets.KEY_CONCRETENESS, PaperSets.VALUE_CONCRETENESS_PHYSICAL);
        Set<PaperProperties> papers = PaperSets.getMatchingPapers(attr);
        if (sel == null) {
            sel = PaperSets.getDefaultPaper(papers);
        } else {
            sel = PaperSets.findBestPaper(sel, papers);
        }
        paperSizeCombo.setModel(PaperSets.setToComboBoxModel(papers));
        paperSizeCombo.setSelectedItem(sel);
    }

    public boolean componentHasMarker() {
        return isFrontBackOnly;
    }

    /**
     * Returns a bit set representing the faces to be printed.
     *
     * @return a bit set whose set bits are the indices of the faces to print
     */
    private BitSet composeFaceSet() {
        BitSet faces = new BitSet();
        if (everythingBtn.isSelected()) {
            faces.set(0, sheetCount);
        } else if (frontOnlyBtn.isSelected()) {
            faces.set(0);
        } else {
            String list = listField.getText().replaceAll("\\s+", "").replaceAll("[\\-]+", "-");
            String[] tokens = list.split("[\\,\\;\\/]");
            for (int i = 0; i < tokens.length; ++i) {
                try {
                    final String t = tokens[i];
                    if (t.isEmpty()) {
                        continue;
                    }
                    int dash = t.indexOf('-');

                    if (dash >= 0 && t.lastIndexOf('-') != dash) {
                        StrangeEons.log.warning("Skipping token with multiple dashes: " + tokens[i]);
                        continue;
                    }

                    int start, end;
                    if (dash == 0) {
                        start = 0;
                    } else if (dash > 0) {
                        start = Integer.parseInt(t.substring(0, dash));
                    } else {
                        start = Integer.parseInt(t);
                    }

                    if (dash < 0) {
                        end = start;
                    } else if (dash == t.length() - 1) {
                        end = sheetCount;
                    } else {
                        end = Integer.parseInt(t.substring(dash + 1));
                    }

                    // clamp start and end and adjust from 1-based indices to 0-based
                    start = Interpolation.clamp(start, 1, sheetCount) - 1;
                    end = Interpolation.clamp(end, 1, sheetCount) - 1;
                    if (start > end) {
                        int temp = start;
                        start = end;
                        end = temp;
                    }

                    faces.set(start, end + 1);
                } catch (NumberFormatException e) {
                    StrangeEons.log.warning("Error parsing token in face list: " + tokens[i]);
                }
            }
        }
        // empty list equates to printing everything
        if (faces.cardinality() == 0) {
            faces.set(0, sheetCount);
        }
        return faces;
    }

    private void updateComponentBasedControls() {
        if (sheetCount == 1 && !everythingBtn.isSelected()) {
            everythingBtn.setSelected(true);
        }
        frontOnlyBtn.setEnabled(sheetCount > 1);
        printListedBtn.setEnabled(sheetCount > 1);

        listField.setEnabled(printListedBtn.isSelected());

        final int printCount = composeFaceSet().cardinality();
        doubleSidedCheck.setEnabled(printCount > 1);
    }

    private boolean ok;
    private boolean isFrontBackOnly;
    private PaperProperties printerPaper;
    private File pdfFile;

    public boolean showDialog() {
        initPaperList(null);
        ok = false;
        pdfFile = null;
        setVisible(true);
        return ok;
    }

    public boolean isDoubleSided() {
        return doubleSidedCheck.isSelected();
    }

    public PaperProperties getPrinterPaper() {
        return printerPaper;
    }

    public File getPDFDestinationFile() {
        return pdfFile;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnGroup = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        doubleSidedCheck = new javax.swing.JCheckBox();
        paperSizeCombo = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        customPaperBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        everythingBtn = new javax.swing.JRadioButton();
        frontOnlyBtn = new javax.swing.JRadioButton();
        printListedBtn = new javax.swing.JRadioButton();
        listField = new javax.swing.JTextField();
        tip = new ca.cgjennings.ui.JTip();
        printBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        pdfBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("psd-title")); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(string("psd-panel-options"))); // NOI18N

        doubleSidedCheck.setText(string("psd-print-double-sided")); // NOI18N
        doubleSidedCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doubleSidedCheckActionPerformed(evt);
            }
        });

        paperSizeCombo.setMaximumRowCount(12);
        paperSizeCombo.setRenderer( PaperSets.createListCellRenderer() );
        paperSizeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paperSizeComboActionPerformed(evt);
            }
        });

        jLabel1.setLabelFor(paperSizeCombo);
        jLabel1.setText(string("de-l-paper-size")); // NOI18N

        customPaperBtn.setFont(customPaperBtn.getFont().deriveFont(customPaperBtn.getFont().getSize()-1f));
        customPaperBtn.setText(string( "de-b-cust-paper" )); // NOI18N
        customPaperBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customPaperBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(doubleSidedCheck)
                    .addComponent(jLabel1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(customPaperBtn)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(paperSizeCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(paperSizeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customPaperBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(doubleSidedCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(string("psd-panel-material"))); // NOI18N

        btnGroup.add(everythingBtn);
        everythingBtn.setSelected(true);
        everythingBtn.setText(string("psd-everything")); // NOI18N
        everythingBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printSetActionPerformed(evt);
            }
        });

        btnGroup.add(frontOnlyBtn);
        frontOnlyBtn.setText(string("psd-front-only")); // NOI18N
        frontOnlyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printSetActionPerformed(evt);
            }
        });

        btnGroup.add(printListedBtn);
        printListedBtn.setText(string("psd-selected-faces")); // NOI18N
        printListedBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printSetActionPerformed(evt);
            }
        });

        listField.setColumns(12);
        listField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                listFieldFocusLost(evt);
            }
        });

        tip.setTipText(string("psd-selected-faces-note")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(everythingBtn)
                    .addComponent(frontOnlyBtn)
                    .addComponent(printListedBtn)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(listField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(everythingBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(frontOnlyBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(printListedBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(listField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        printBtn.setText(string("psd-ok")); // NOI18N

        cancelBtn.setMnemonic('C');
        cancelBtn.setText("Cancel");

        pdfBtn.setText(string("psd-pdf")); // NOI18N
        pdfBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pdfBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(pdfBtn)
                        .addGap(18, 18, Short.MAX_VALUE)
                        .addComponent(printBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, printBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(printBtn)
                    .addComponent(pdfBtn))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void printSetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printSetActionPerformed
        updateComponentBasedControls();
    }//GEN-LAST:event_printSetActionPerformed

    private void paperSizeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paperSizeComboActionPerformed
        printerPaper = (PaperProperties) paperSizeCombo.getSelectedItem();
    }//GEN-LAST:event_paperSizeComboActionPerformed

	private void customPaperBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customPaperBtnActionPerformed
            PaperProperties sel = (PaperProperties) paperSizeCombo.getSelectedItem();

            CustomPaperDialog cpd = new CustomPaperDialog(customPaperBtn, sel, true);
            PaperProperties selected = cpd.showDialog();
            if (selected != null) {
                sel = selected;
            }
            initPaperList(sel);
	}//GEN-LAST:event_customPaperBtnActionPerformed

    private void listFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_listFieldFocusLost
        updateComponentBasedControls();
    }//GEN-LAST:event_listFieldFocusLost

    private void doubleSidedCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_doubleSidedCheckActionPerformed
        updateComponentBasedControls();
    }//GEN-LAST:event_doubleSidedCheckActionPerformed

    private void pdfBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdfBtnActionPerformed
        String baseName = name;
        if (name == null || name.isEmpty()) {
            baseName = string("prj-doc-title");
        }
        baseName = ResourceKit.makeStringFileSafe(baseName);
        File sel = ResourceKit.showGenericExportFileDialog(this, baseName, string("psd-pdf-desc"), "pdf");
        if (sel != null) {
            pdfFile = sel;
            handleOKAction(evt);
        }
    }//GEN-LAST:event_pdfBtnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup btnGroup;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JButton customPaperBtn;
    private javax.swing.JCheckBox doubleSidedCheck;
    private javax.swing.JRadioButton everythingBtn;
    private javax.swing.JRadioButton frontOnlyBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField listField;
    private javax.swing.JComboBox paperSizeCombo;
    private javax.swing.JButton pdfBtn;
    private javax.swing.JButton printBtn;
    private javax.swing.JRadioButton printListedBtn;
    private ca.cgjennings.ui.JTip tip;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        ok = true;
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    /**
     * Returns a bit set representing the faces to be printed.
     *
     * @return a bit set whose set bits are the indices of the faces to print
     */
    public BitSet getSelectedFaces() {
        return ok ? composeFaceSet() : null;
    }

    /**
     * Configures the dialog controls to gather settings for printing the
     * provided sheets (or a set of similar sheets from the same component
     * type).
     *
     * @param printSheets the sheets to be printed
     * @throws NullPointerException if the array of sheets is {@code null}
     */
    public void setUpForSheets(Sheet[] printSheets) {
        sheetCount = printSheets.length;
        if (sheetCount > 0) {
            name = printSheets[0].getGameComponent().getFullName();
        }

        // set up old variables for now
        isFrontBackOnly = (printSheets.length & 1) == 0;
        updateComponentBasedControls();
    }

    private int sheetCount;
    private String name;
}
