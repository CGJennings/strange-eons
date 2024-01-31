package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.sheet.FinishStyle;
import ca.cgjennings.ui.JIconComboBox;
import ca.cgjennings.ui.JUtilities;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.text.DefaultFormatter;
import static resources.Language.string;

/**
 * The style panel for face edge finishing options.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.3
 */
public class BleedMarginStylePanel extends AbstractStylePanel<BleedMarginStyle> implements BleedMarginStyle {

    /**
     * Creates new form BleedMarginStylePanel
     */
    public BleedMarginStylePanel() {
        initComponents();
        {
            // update bleed margin immediately on click
            JFormattedTextField field = ((JSpinner.DefaultEditor) bleedSpinner.getEditor()).getTextField();
            ((DefaultFormatter) field.getFormatter()).setCommitsOnValidEdit(true);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        finishCombo = new JIconComboBox<>(FinishStyle.values());
        bleedLabel = new javax.swing.JLabel();
        bleedSpinner = new javax.swing.JSpinner();

        finishCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                finishComboActionPerformed(evt);
            }
        });

        bleedLabel.setText(string("style-ef-bleed-margin")); // NOI18N

        bleedSpinner.setModel(new javax.swing.SpinnerNumberModel(9.0d, 0.25d, 36.0d, 0.25d));
        bleedSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                bleedSpinnerStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(bleedLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(bleedSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(finishCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(finishCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bleedLabel)
                    .addComponent(bleedSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void finishComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_finishComboActionPerformed
        boolean bleed = finishCombo.getSelectedItem() == FinishStyle.MARGIN;
        JUtilities.enable(bleed, bleedLabel, bleedSpinner);
        styleChanged();
    }//GEN-LAST:event_finishComboActionPerformed

    private void bleedSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_bleedSpinnerStateChanged
        styleChanged();
    }//GEN-LAST:event_bleedSpinnerStateChanged

    @Override
    public String getTitle() {
        return string("style-ef-title");
    }

    @Override
    public FinishStyle getFinishStyle() {
        return (FinishStyle) finishCombo.getSelectedItem();
    }

    @Override
    public void setFinishStyle(FinishStyle style) {
        finishCombo.setSelectedItem(style);
        styleChanged();
    }

    @Override
    public double getBleedMarginWidth() {
        return (double) bleedSpinner.getValue();
    }

    @Override
    public void setBleedMarginWidth(double widthInPoints) {
        bleedSpinner.setValue(widthInPoints);
        styleChanged();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bleedLabel;
    private javax.swing.JSpinner bleedSpinner;
    private javax.swing.JComboBox<FinishStyle> finishCombo;
    // End of variables declaration//GEN-END:variables
}
