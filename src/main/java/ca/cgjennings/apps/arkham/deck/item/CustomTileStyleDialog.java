package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.ColourDialog;
import ca.cgjennings.apps.arkham.ColourDialog.ColourButton;
import ca.cgjennings.apps.arkham.HSBPanel;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import static resources.Language.string;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class CustomTileStyleDialog extends javax.swing.JDialog implements AgnosticDialog {

    protected CustomTile item;
    NumberFormat formatter;

    private float opacity;

    /**
     * Creates new form LineStyle
     */
    public CustomTileStyleDialog(java.awt.Frame parent, CustomTile tile) {
        super(parent, true);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        DashPattern.createSelector(dashCombo);
        AbstractGameComponentEditor.localizeComboBoxLabels(endcapCombo, null);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        this.item = tile;
        formatter = NumberFormat.getNumberInstance();
        copyStyleFromItem();
        cropMarkBoxActionPerformed(null); // enable/disable bleed controls

        HSBPanel.takeOverSlider(opacityLabel, opacitySlider);

        pack();
    }

    protected void copyStyleFromItem() {
        opacity = item.getOpacity();
        opacity = Math.min(1, Math.max(0, opacity));
        opacitySlider.setValue(Math.round(opacity * 1000f));

        cropMarkBox.setSelected(item.isBleedMarginMarked());
        bleedSpinner.setValue(item.getBleedMargin());

        thicknessField.setValue(item.getOutlineWidth());
        switch (item.getOutlineJoin()) {
            case ROUND:
                endcapCombo.setSelectedIndex(0);
                break;
            case MITER:
                endcapCombo.setSelectedIndex(1);
                break;
            case BEVEL:
                endcapCombo.setSelectedIndex(2);
                break;
        }
        colourLabel.setBackground(item.getOutlineColor());
        dashCombo.setSelectedItem(item.getOutlineDashPattern());
    }

    protected boolean copyStyleToItem(CustomTile item) {
        item.setOpacity(opacity);
        item.setBleedMarginMarked(cropMarkBox.isSelected());
        item.setBleedMargin(((Number) bleedSpinner.getValue()).doubleValue());

        float thickness = ((Number) thicknessField.getValue()).floatValue();
        item.setOutlineWidth(thickness);
        switch (endcapCombo.getSelectedIndex()) {
            case 0:
                item.setOutlineJoin(LineJoin.ROUND);
                break;
            case 1:
                item.setOutlineJoin(LineJoin.MITER);
                break;
            case 2:
                item.setOutlineJoin(LineJoin.BEVEL);
                break;
        }
        item.setOutlineColor(colourLabel.getBackground());
        item.setOutlineDashPattern((DashPattern) dashCombo.getSelectedItem());
        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        opacityPanel = new javax.swing.JPanel();
        opacityLabel = new ca.cgjennings.ui.OpacityLabel();
        opacitySlider = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        opacityField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        opacitySwatch = new ColourButton();
        jPanel1 = new javax.swing.JPanel();
        cropMarkBox = new javax.swing.JCheckBox();
        bleedLabel = new javax.swing.JLabel();
        bleedSpinner = new javax.swing.JSpinner();
        pointsLabel = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        endcapCombo = new javax.swing.JComboBox<>();
        dashCombo = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        thicknessField = new javax.swing.JSpinner();
        jPanel4 = new javax.swing.JPanel();
        colourButton = new javax.swing.JButton();
        colourLabel = new ColourDialog.ColourButton();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("style-l-custom-tile")); // NOI18N

        cancelBtn.setText(string("cancel")); // NOI18N

        okBtn.setText(string("style-li-change")); // NOI18N

        opacityPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("cd-l-opacity"))); // NOI18N

        opacityLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        opacityLabel.setText("opacityLabel1");

        opacitySlider.setMajorTickSpacing(500);
        opacitySlider.setMaximum(1000);
        opacitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                opacitySliderStateChanged(evt);
            }
        });

        jLabel1.setText("0%");

        opacityField.setColumns(6);
        opacityField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        opacityField.setText("100.0%");
        opacityField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                opacityFieldActionPerformed(evt);
            }
        });
        opacityField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                opacityFieldFocusLost(evt);
            }
        });

        jLabel2.setText("100%");

        opacitySwatch.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        opacitySwatch.setContentAreaFilled(false);
        opacitySwatch.setEnabled(false);

        javax.swing.GroupLayout opacityPanelLayout = new javax.swing.GroupLayout(opacityPanel);
        opacityPanel.setLayout(opacityPanelLayout);
        opacityPanelLayout.setHorizontalGroup(
            opacityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(opacityPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(opacitySwatch, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(opacityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(opacityLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE)
                    .addComponent(opacitySlider, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 157, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(opacityField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        opacityPanelLayout.setVerticalGroup(
            opacityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(opacityPanelLayout.createSequentialGroup()
                .addGroup(opacityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(opacityPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(opacityPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(opacitySwatch, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1)
                            .addComponent(opacitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)
                            .addComponent(opacityField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(opacityLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "style-t-crop-marks" ))); // NOI18N

        cropMarkBox.setText(string( "style-optional-crop-marks" )); // NOI18N
        cropMarkBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cropMarkBoxActionPerformed(evt);
            }
        });

        bleedLabel.setText(string( "style-bleed-margin" )); // NOI18N

        bleedSpinner.setModel(new javax.swing.SpinnerNumberModel(0.0d, 0.0d, 144.0d, 0.5d));

        pointsLabel.setText(string( "de-l-points" )); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(bleedLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bleedSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pointsLabel))
                    .addComponent(cropMarkBox))
                .addContainerGap(110, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cropMarkBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bleedLabel)
                    .addComponent(bleedSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pointsLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-l-border"))); // NOI18N

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-li-shape"))); // NOI18N

        jLabel3.setText(string("style-li-thickness")); // NOI18N

        jLabel4.setText(string("style-li-points")); // NOI18N

        jLabel5.setText(string("style-li-corners")); // NOI18N

        endcapCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "style-li-round", "style-li-square", "style-li-bevel" }));

        dashCombo.setMaximumRowCount(12);

        jLabel6.setText(string("style-li-dash")); // NOI18N

        thicknessField.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(144.0f), Float.valueOf(0.5f)));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(thicknessField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4))
                    .addComponent(endcapCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(dashCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(thicknessField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(endcapCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dashCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-li-appearance"))); // NOI18N

        colourButton.setText(string("style-li-colour")); // NOI18N
        colourButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colourButtonActionPerformed(evt);
            }
        });

        colourLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        colourLabel.setContentAreaFilled(false);
        colourLabel.setPreferredSize(new java.awt.Dimension(24, 24));
        colourLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colourLabelcolourButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(colourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(colourButton)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(colourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(colourButton))
                .addContainerGap(70, Short.MAX_VALUE))
        );

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getSize()-2f));
        jLabel7.setText(string("style-l-disable-border")); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel7))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel7)
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel2, jPanel4});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancelBtn)
                        .addGap(10, 10, 10))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(opacityPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(opacityPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

private void opacitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_opacitySliderStateChanged
    opacity = opacitySlider.getValue() / 1000f;
    opacityField.setText(formatter.format(opacity * 100f) + "%");
    opacitySwatch.setBackground(
            new Color(255, 0, 0, Math.round(opacity * 255f))
    );
}//GEN-LAST:event_opacitySliderStateChanged

private void opacityFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_opacityFieldActionPerformed
    if (!opacityUpdate) {
        opacityUpdate = true;
        float val = opacity;
        try {
            val = formatter.parse(opacityField.getText()).floatValue();
        } catch (ParseException e) {
        }
        int op = Math.round(val * 10f);
        if (op < 0) {
            op = 0;
        }
        if (op > 1_000) {
            op = 1_000;
        }
        opacitySlider.setValue(op);
        opacityUpdate = false;
    }
}//GEN-LAST:event_opacityFieldActionPerformed

    private boolean opacityUpdate;

private void opacityFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_opacityFieldFocusLost
    opacityFieldActionPerformed(null);
}//GEN-LAST:event_opacityFieldFocusLost

private void cropMarkBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cropMarkBoxActionPerformed
    boolean enabled = cropMarkBox.isSelected();
    bleedLabel.setEnabled(enabled);
    bleedSpinner.setEnabled(enabled);
    pointsLabel.setEnabled(enabled);
}//GEN-LAST:event_cropMarkBoxActionPerformed

private void colourButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colourButtonActionPerformed
    ColourDialog d = ColourDialog.getSharedDialog();
    d.setSelectedColor(colourLabel.getBackground());
    d.setLocationRelativeTo((Component) evt.getSource());
    d.setVisible(true);
    if (d.getSelectedColor() != null) {
        colourLabel.setBackground(d.getSelectedColor());
    }
}//GEN-LAST:event_colourButtonActionPerformed

private void colourLabelcolourButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colourLabelcolourButtonActionPerformed
    ColourDialog d = ColourDialog.getSharedDialog();
    d.setSelectedColor(colourLabel.getBackground());
    d.setLocationRelativeTo((Component) evt.getSource());
    d.setVisible(true);
    if (d.getSelectedColor() != null) {
        colourLabel.setBackground(d.getSelectedColor());
    }
}//GEN-LAST:event_colourLabelcolourButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bleedLabel;
    private javax.swing.JSpinner bleedSpinner;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JButton colourButton;
    private javax.swing.JButton colourLabel;
    private javax.swing.JCheckBox cropMarkBox;
    private javax.swing.JComboBox dashCombo;
    private javax.swing.JComboBox<String> endcapCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JButton okBtn;
    private javax.swing.JTextField opacityField;
    private ca.cgjennings.ui.OpacityLabel opacityLabel;
    private javax.swing.JPanel opacityPanel;
    private javax.swing.JSlider opacitySlider;
    private javax.swing.JButton opacitySwatch;
    private javax.swing.JLabel pointsLabel;
    private javax.swing.JSpinner thicknessField;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        Deck d = item.getPage().getDeck();
        PageItem[] sel = d.getSelection();
        for (int i = 0; i < sel.length; ++i) {
            if (sel[i] instanceof CustomTile) {
                // returns false if there is an error in a field
                if (!copyStyleToItem((CustomTile) sel[i])) {
                    return;
                }
            }
        }
        item.getPage().getView().repaint();
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

}