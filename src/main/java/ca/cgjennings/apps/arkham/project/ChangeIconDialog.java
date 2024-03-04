package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import static resources.Language.string;

/**
 * Dialog used by the {@link ChangeIcon} task action.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class ChangeIconDialog extends javax.swing.JDialog implements AgnosticDialog {

    /**
     * Creates new form ChangeIconDialog
     */
    public ChangeIconDialog() {
        super(StrangeEons.getWindow(), true);
        JUtilities.makeUtilityWindow(this);

        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String i : Task.getCustomIcons()) {
            model.addElement(i);
        }
        iconList.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        iconList = new javax.swing.JList<>();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        resetBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "pa-icon-title" )); // NOI18N

        jScrollPane1.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(192, 192, 192)));

        iconList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        iconList.setCellRenderer( new TaskIconRenderer() );
        iconList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        iconList.setVisibleRowCount(-1);
        jScrollPane1.setViewportView(iconList);

        cancelBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "select" )); // NOI18N

        resetBtn.setText(string( "pa-icon-reset" )); // NOI18N
        resetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(resetBtn)
                .addGap(18, 29, Short.MAX_VALUE)
                .addComponent(okBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelBtn)
                .addContainerGap())
            .addComponent(jScrollPane1)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn)
                    .addComponent(resetBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void resetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetBtnActionPerformed
            selection = null;
            ok = true;
            dispose();
	}//GEN-LAST:event_resetBtnActionPerformed

    @Override
    public void handleOKAction(ActionEvent e) {
        Object value = iconList.getSelectedValue();
        selection = value == null ? null : value.toString();
        ok = true;
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    static class TaskIconRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setText(null);
            setPreferredSize(ICON_CELL_SIZE);
            setHorizontalAlignment(JLabel.CENTER);
            setIcon(MetadataSource.getDefaultTaskIcon(null, value.toString()));
            return this;
        }
    }
    private static final Dimension ICON_CELL_SIZE = new Dimension(32, 32);

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JList<String> iconList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okBtn;
    private javax.swing.JButton resetBtn;
    // End of variables declaration//GEN-END:variables

    public boolean showDialog(String currentValue) {
        ok = false;
        selection = currentValue;
        iconList.setSelectedValue(currentValue, true);
        setVisible(true);
        return ok;
    }

    public String getSelectedResource() {
        return selection;
    }

    private String selection;
    private boolean ok;
}
