package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.EditorPane;
import ca.cgjennings.ui.FilteredDocument;
import ca.cgjennings.ui.theme.Palette;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import static resources.Language.string;
import resources.Settings;

/**
 * The dialog displayed to the user when adding a new task to a project.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class NewTaskDialog extends javax.swing.JDialog implements AgnosticDialog {

    private final TaskGroup parent;

    /**
     * Creates new form NewTaskDialog
     */
    public NewTaskDialog(TaskGroup parent) {
        super(StrangeEons.getWindow(), true);
        this.parent = parent;
        initComponents();
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        getRootPane().setDefaultButton(okBtn);
        name.selectAll();
        name.requestFocusInWindow();

        typeList.setCellRenderer(new Renderer());
        DefaultListModel<NewTaskType> model = new DefaultListModel<>();
        NewTaskType[] types = NewTaskType.getNewTaskTypes();
        for (NewTaskType ntt : types) {
            model.addElement(ntt);
        }
        typeList.setModel(model);

        // ensure nothing is selcted
        // this forces the user to select something before OK will enable
        // users tended to enter a name and hit OK without choosing a type
        typeList.setSelectedIndex(-1);

        Settings.getUser().applyWindowSettings(WINDOW_KEY, this);

        name.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateOK();
            }
        });
        updateOK();
    }

    private static final String WINDOW_KEY = "new-task-dialog";

    @Override
    public void dispose() {
        Settings.getUser().storeWindowSettings(WINDOW_KEY, this);
        super.dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        name = new javax.swing.JTextField();
        okBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        startLabel1 = new javax.swing.JLabel();
        startLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        typeList = new javax.swing.JList<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        descPane = new EditorPane();
        errorLabel = new javax.swing.JLabel();
        helpBtn = new ca.cgjennings.ui.JHelpButton();
        jHeading1 = new ca.cgjennings.ui.JHeading();
        banner = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "task-l-title" )); // NOI18N

        jLabel2.setText(string( "task-l-name" )); // NOI18N

        jLabel3.setText(string( "task-l-type" )); // NOI18N

        name.setDocument( FilteredDocument.createFileNameDocument() );
        name.setText("New Task Name");

        okBtn.setText(string( "task-b-ok" )); // NOI18N

        cancelBtn.setText(string( "cancel" )); // NOI18N

        startLabel1.setFont(startLabel1.getFont().deriveFont(startLabel1.getFont().getSize()-1f));
        startLabel1.setText(string( "task-l-intro" )); // NOI18N

        startLabel.setFont(startLabel.getFont().deriveFont(startLabel.getFont().getSize()-1f));
        startLabel.setText(string( "task-l-intro2" )); // NOI18N

        typeList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        typeList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                typeListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(typeList);

        descPane.setEditable(false);
        descPane.setContentType("text/html"); // NOI18N
        descPane.setFont(descPane.getFont().deriveFont(descPane.getFont().getSize()+2f));
        descPane.setText("<html>\r");
        jScrollPane2.setViewportView(descPane);

        errorLabel.setForeground(Palette.get.foreground.opaque.red);
        errorLabel.setText(" ");

        helpBtn.setHelpPage("proj-intro#adding-tasks");

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("resources/text/interface/eons-text"); // NOI18N
        jHeading1.setText(bundle.getString("task-l-title")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jHeading1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(name, javax.swing.GroupLayout.DEFAULT_SIZE, 513, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 369, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE))
                    .addComponent(errorLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 554, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(startLabel1)
                            .addComponent(startLabel))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jHeading1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(startLabel1)
                .addGap(1, 1, 1)
                .addComponent(startLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(errorLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cancelBtn)
                        .addComponent(okBtn))
                    .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        banner.setBackground(java.awt.Color.darkGray);
        banner.setIcon(resources.ResourceKit.createBleedBanner("new-task.jpg"));
        banner.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        banner.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, java.awt.Color.gray));
        banner.setOpaque(true);
        getContentPane().add(banner, java.awt.BorderLayout.WEST);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void typeListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_typeListValueChanged
            NewTaskType ntt = (NewTaskType) typeList.getSelectedValue();
            if (ntt != null) {
                descPane.setText(ntt.getDescription());
                descPane.select(0, 0);
            }
            updateOK();
	}//GEN-LAST:event_typeListValueChanged

    private void updateOK() {
        JButton ok = PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn);

        if (name.getText().length() == 0) {
            ok.setEnabled(false);
            errorLabel.setText(string("prj-err-no-task-name"));
            return;
        } else if (new File(parent.getFile(), name.getText()).exists()) {
            ok.setEnabled(false);
            errorLabel.setText(string("prj-err-folder-exists"));
            return;
        }

        if (typeList.getSelectedIndex() < 0) {
            ok.setEnabled(false);
            errorLabel.setText(string("prj-err-no-task-selected"));
            return;
        }

        errorLabel.setText(" ");
        ok.setEnabled(true);
    }

    @Override
    public void handleCancelAction(ActionEvent ev) {
        dispose();
    }

    @Override
    public void handleOKAction(ActionEvent ev) {
        NewTaskType ntt = (NewTaskType) typeList.getSelectedValue();
        if (ntt == null) {
            return;
        }

        try {
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getGlassPane().setVisible(true);
            Task t = parent.addNewTask(ntt, name.getText());
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-task"), e);
        }
        dispose();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel banner;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JEditorPane descPane;
    private javax.swing.JLabel errorLabel;
    private ca.cgjennings.ui.JHelpButton helpBtn;
    private ca.cgjennings.ui.JHeading jHeading1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField name;
    private javax.swing.JButton okBtn;
    private javax.swing.JLabel startLabel;
    private javax.swing.JLabel startLabel1;
    private javax.swing.JList<NewTaskType> typeList;
    // End of variables declaration//GEN-END:variables

    private static class Renderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            NewTaskType ntt = (NewTaskType) value;
            setIcon(MetadataSource.getDefaultTaskIcon(ntt.getType(), ntt.getIconResource()));
            return this;
        }
    }
}
