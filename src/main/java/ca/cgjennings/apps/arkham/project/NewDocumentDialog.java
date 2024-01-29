package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.theme.Palette;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import static resources.Language.string;
import resources.Settings;

/**
 * Allows the user to select one of a set of document formats and create a
 * document of that type.
 */
@SuppressWarnings("serial")
public final class NewDocumentDialog extends javax.swing.JDialog implements AgnosticDialog {

    private Member folder;

    /**
     * Creates new form NewDocumentDialog
     */
    public NewDocumentDialog(TaskAction[] docTypes, Member parent) {
        super(StrangeEons.getWindow(), true);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        folder = parent;

        Member[] singletonMembers = new Member[]{parent};
        DefaultListModel<TaskAction> model = new DefaultListModel<>();
        for (TaskAction dta : docTypes) {
            if (dta.appliesToSelection(singletonMembers)) {
                model.addElement(dta);
            }
        }
        typeList.setModel(model);

        Settings s = Settings.getShared();
        String name = s.get(KEY_DOC_NAME);
        if (name == null) {
            name = string("pa-new-doc-name");
        }
        nameField.setText(name);

        String type = s.get(KEY_DOC_TYPE);
        if (type != null) {
            for (int i = 0; i < model.size(); ++i) {
                if (type.equals(model.get(i).toString())) {
                    typeList.setSelectedIndex(i);
                    break;
                }
            }
            if (typeList.getSelectedIndex() < 0) {
                typeList.setSelectedIndex(0);
            }
        }

        nameField.selectAll();
        nameField.requestFocusInWindow();

        pack();
        typeListValueChanged(null);
    }

    private static final String KEY_DOC_NAME = "project-doc-name";
    private static final String KEY_DOC_TYPE = "project-doc-type";

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        typeList =  new JIconList() ;
        jLabel1 = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        errorLabel = new javax.swing.JLabel();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        extLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "prj-doc-title" )); // NOI18N

        typeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        typeList.setVisibleRowCount(6);
        typeList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                typeListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(typeList);

        jLabel1.setText(string( "prj-doc-name" )); // NOI18N

        nameField.setColumns(20);
        nameField.setText(string( "pa-new-doc-name" )); // NOI18N

        jLabel2.setText(string( "prj-doc-type" )); // NOI18N

        errorLabel.setForeground(Palette.get.foreground.opaque.red);
        errorLabel.setText(" ");

        cancelBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "prj-doc-ok" )); // NOI18N

        extLabel.setText(".MMMMMMMMMM");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(errorLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(1, 1, 1)
                                        .addComponent(extLabel))
                                    .addComponent(jLabel2))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(okBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cancelBtn))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jScrollPane1)))
                        .addContainerGap())))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(extLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(errorLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void typeListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_typeListValueChanged
            final JButton ok = PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn);
            New.NewAction dta = (New.NewAction) typeList.getSelectedValue();

            if (dta == null) {
                ok.setEnabled(false);
                extLabel.setText(" ");
            } else {
                ok.setEnabled(true);
                extLabel.setText("." + dta.getFileExtension());
            }
	}//GEN-LAST:event_typeListValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JLabel errorLabel;
    private javax.swing.JLabel extLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField nameField;
    private javax.swing.JButton okBtn;
    private javax.swing.JList<TaskAction> typeList;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent evt) {
        ok = true;
        New.NewAction dta = (New.NewAction) typeList.getSelectedValue();
        if (dta == null) {
            return;
        }
        String name = nameField.getText();
        String ext = extLabel.getText();
        if (name.endsWith(ext)) {
            name = name.substring(0, name.length() - ext.length());
        }

        Settings s = Settings.getUser();
        s.set(KEY_DOC_NAME, name);
        s.set(KEY_DOC_TYPE, dta.toString());

        File f = new File(folder.getFile(), name + ext);
        f = ProjectUtilities.getAvailableFile(f);
        try {
            dta.createFile(f);
            folder.synchronize();
            dispose();
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-create-new"), e);
        }
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    public boolean showDialog() {
        ok = false;
        setVisible(true);
        return ok;
    }
    private boolean ok;

}