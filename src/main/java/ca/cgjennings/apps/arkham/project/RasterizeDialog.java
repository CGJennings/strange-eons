package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.graphics.shapes.VectorImage;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.event.ActionEvent;
import javax.swing.JSpinner;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Dialog displayed by the rasterize action to choose an image size.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class RasterizeDialog extends javax.swing.JDialog implements AgnosticDialog {

    private final double wh;
    private double hw;

    public RasterizeDialog(VectorImage image) {
        super(StrangeEons.getWindow(), true);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);
        double w = image.getWidth();
        double h = image.getHeight();
        if (w <= 0d) {
            w = 1d;
        }
        if (h <= 0d) {
            h = 1d;
        }
        wh = w / h;
        hw = h / w;
        widthSpinner.setValue(300);
        editSize(widthSpinner, heightSpinner);
    }

    private int[] result;

    public int[] showDialog() {
        result = null;
        setVisible(true);
        return result;
    }

    public void editSize(JSpinner edited, JSpinner other) {
        if (updating) {
            return;
        }
        updating = true;
        try {
            if (aspectLock.isSelected()) {
                double editedAspect;
                if (edited == widthSpinner) {
                    editedAspect = hw;
                } else {
                    editedAspect = wh;
                }
                int sel = ((Number) edited.getValue()).intValue();
                int locked = (int) Math.round((sel * editedAspect));
                if (locked > 5_000 || locked < 1) {
                    locked = locked == 0 ? 1 : 5_000;
                    updating = false;
                    other.setValue(locked);
                    editSize(other, edited);
                } else {
                    other.setValue(locked);
                }
            }
        } finally {
            updating = false;
        }
    }

    private boolean updating;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        javax.swing.JLabel widthLabel = new javax.swing.JLabel();
        javax.swing.JLabel heightLabel = new javax.swing.JLabel();
        heightSpinner = new javax.swing.JSpinner();
        widthSpinner = new javax.swing.JSpinner();
        javax.swing.JLabel bracket = new javax.swing.JLabel();
        aspectLock = new javax.swing.JToggleButton();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("rast-name")); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(string("rast-l-size"))); // NOI18N

        widthLabel.setLabelFor(widthSpinner);
        widthLabel.setText(string("width")); // NOI18N

        heightLabel.setLabelFor(heightSpinner);
        heightLabel.setText(string("height")); // NOI18N

        heightSpinner.setModel(new javax.swing.SpinnerNumberModel(300, 1, 5000, 1));
        heightSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                heightSpinnerStateChanged(evt);
            }
        });

        widthSpinner.setModel(new javax.swing.SpinnerNumberModel(300, 1, 5000, 1));
        widthSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                widthSpinnerStateChanged(evt);
            }
        });

        bracket.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(6, 0, 6, 0), javax.swing.BorderFactory.createMatteBorder(1, 0, 1, 1, java.awt.Color.darkGray)), javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0)));

        aspectLock.setIcon( ResourceKit.getIcon( "ui/locked.png" ));
        aspectLock.setSelected(true);
        aspectLock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aspectLockActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(widthLabel)
                        .addGap(18, 18, 18)
                        .addComponent(widthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(heightLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(heightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bracket)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(aspectLock)
                .addGap(0, 122, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(bracket, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(aspectLock))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(widthLabel)
                    .addComponent(widthSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(heightLabel)
                    .addComponent(heightSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6))
        );

        cancelBtn.setText(string("cancel")); // NOI18N

        okBtn.setText(string("rast-name")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void widthSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_widthSpinnerStateChanged
        editSize(widthSpinner, heightSpinner);
    }//GEN-LAST:event_widthSpinnerStateChanged

    private void heightSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightSpinnerStateChanged
        editSize(heightSpinner, widthSpinner);
    }//GEN-LAST:event_heightSpinnerStateChanged

    private void aspectLockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aspectLockActionPerformed
        if (aspectLock.isSelected()) {
            if (wh > hw) {
                editSize(widthSpinner, heightSpinner);
            } else {
                editSize(heightSpinner, widthSpinner);
            }
        }
    }//GEN-LAST:event_aspectLockActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton aspectLock;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JSpinner heightSpinner;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton okBtn;
    private javax.swing.JSpinner widthSpinner;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        result = new int[]{
            ((Number) widthSpinner.getValue()).intValue(),
            ((Number) heightSpinner.getValue()).intValue()
        };
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }
}
