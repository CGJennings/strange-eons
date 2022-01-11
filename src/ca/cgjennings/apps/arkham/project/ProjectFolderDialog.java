package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.FolderTree;
import ca.cgjennings.ui.JUtilities;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreePath;
import static resources.Language.string;
import resources.Settings;

/**
 * A dialog for selecting project file folders, or folders OTHER THAN project
 * file folders.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class ProjectFolderDialog extends javax.swing.JDialog implements AgnosticDialog {

    private Mode mode;
    private File selection;

    /**
     * The type of folder selection supported by the dialog.
     */
    public enum Mode {
        /**
         * In this mode, the user can only select a project folder or a
         * {@code .seproject} crate file. The user cannot navigate into an
         * existing project.
         */
        SELECT_PROJECT,
        /**
         * In this mode, the user can select a parent folder for a new project.
         * The user cannot navigate into an existing project or select a
         * project.
         */
        SELECT_PROJECT_CONTAINER,
        /**
         * In this mode, the user can select any folder.
         */
        SELECT_FOLDER
    }

    /**
     * Creates a new project folder dialog that allows folder selection using
     * the specified mode.
     *
     * @param parent an optional component to use for the dialog's parent; if
     * {@code null} the main application window is used
     * @param mode the type of folder selection to allow
     */
    public ProjectFolderDialog(Component parent, Mode mode) {
        super(StrangeEons.getWindow(), true);
        this.mode = mode;
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);
        setLocationRelativeTo(parent == null ? StrangeEons.getWindow() : parent);

        folderTree.addSelectionListener((PropertyChangeEvent evt) -> {
            updateBtnEnabledStates();
        });
        updateBtnEnabledStates();

        switch (mode) {
            case SELECT_PROJECT:
                newFolderBtn.setVisible(false);
                setTitle(string("prj-dc-open"));
                setAcceptButtonText(string("open"));
                break;
            case SELECT_PROJECT_CONTAINER:
                setTitle(string("prj-dc-title"));
                break;
            case SELECT_FOLDER:
                setTitle(string("rk-dialog-folder"));
                break;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Uses the specified setting key to recall and store the initial selected
     * folder for the dialog. The value of the setting key is used to set the
     * initial folder, and if the user selects a folder then the parent of that
     * folder will be stored in the key.
     *
     * @param key the name of the user setting key to use
     * @see Settings#getUser
     */
    public void useSettingKey(String key) {
        settingKey = key;
        if (settingKey != null) {
            String loc = Settings.getUser().get(settingKey);
            setSelectedFolder(loc == null ? null : new File(loc));
        }
    }
    private String settingKey;

    /**
     * Sets the currently selected folder. If the file is {@code null}, the
     * folder will be set to a platform-dependent default location. If the
     * specified file does not exist or is not a folder, the selection is
     * unchanged.
     *
     * @param f the folder to select
     * @see #getSelectedFolder()
     */
    public void setSelectedFolder(File f) {
        folderTree.setSelectedFolder(f);
    }

    /**
     * Returns the currently selected folder.
     *
     * @return the selected folder
     * @see #setSelectedFolder(java.io.File)
     * @see #showDialog()
     */
    public File getSelectedFolder() {
        return folderTree.getSelectedFolder();
    }

    /**
     * Sets the label text of the button that accepts the selected file and
     * closes the dialog (the "OK" button). If no text is set explicitly, a
     * generic label will be used.
     *
     * @param okText text that describes the effect of accepting the file
     * @see #getAcceptButtonText()
     */
    public void setAcceptButtonText(String okText) {
        PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn).setText(okText);
    }

    /**
     * Returns the label text of the button that accepts the selected file and
     * closes the dialog (the "OK" button).
     *
     * @return text that describes the effect of accepting the file
     * @see #setAcceptButtonText(java.lang.String)
     */
    public String getAcceptButtonText() {
        return PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn).getText();
    }

    private void updateBtnEnabledStates() {
        final JButton ok = PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn);

        boolean enableOKBtn = false;
        boolean enableNewFolderBtn = false;

        File sel = folderTree.getSelectedFolder();

        final boolean isFileSystem = sel != null && folderTree.getFileSystemView().isFileSystem(sel);
        final boolean isTraversable = isFileSystem && folderTree.getFileSystemView().isTraversable(sel);
        final boolean isProject = ((ProjectTree) folderTree).isProjectSelected();

        switch (mode) {
            case SELECT_FOLDER:
                enableOKBtn = isTraversable;
                enableNewFolderBtn = isTraversable;
                break;
            case SELECT_PROJECT:
                enableOKBtn = isProject;
                enableNewFolderBtn = isTraversable && !isProject;
                break;
            case SELECT_PROJECT_CONTAINER:
                enableOKBtn = isTraversable && !isProject;
                enableNewFolderBtn = enableOKBtn;
                break;
            default:
                throw new AssertionError();
        }
        ok.setEnabled(enableOKBtn);
        newFolderBtn.setEnabled(enableNewFolderBtn);
    }

    /**
     * Sets the initial name to use when the user first creates a new folder. If
     * set to {@code null}, then the default name is used.
     *
     * @param suggestion the suggested name, or {@code null}
     * @see #getSuggestedFolderName()
     */
    public void setSuggestedFolderName(String suggestion) {
        suggestedName = suggestion;
    }

    /**
     * Returns the initial name to use when the user first creates a new folder,
     * or {@code null} if no suggested name has been set.
     *
     * @return the suggested name, or {@code null}
     * @see #setSuggestedFolderName(java.lang.String)
     */
    public String getSuggestedFolderName() {
        return suggestedName;
    }

    private String suggestedName;
    private boolean suggestedNameUsed;

    /**
     * Displays the selection dialog and allows the user to select a file. If
     * the user selects a file, the file is returned. If the user cancels the
     * dialog, {@code null} is returned.
     *
     * @return the selected file, or {@code null}
     */
    public File showDialog() {
        suggestedNameUsed = (suggestedName == null);
        selection = null;
        setVisible(true);
        return selection;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        folderTree =  new ProjectTree() ;
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        newFolderBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        scrollPane.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(192, 192, 192)));

        folderTree.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        scrollPane.setViewportView(folderTree);

        cancelBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "select" )); // NOI18N

        newFolderBtn.setText(string( "prj-b-make-folder" )); // NOI18N
        newFolderBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newFolderBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(newFolderBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 174, Short.MAX_VALUE)
                .addComponent(okBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelBtn)
                .addContainerGap())
            .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 415, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn)
                    .addComponent(newFolderBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void newFolderBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newFolderBtnActionPerformed
            createFolder = false;
            final JDialog newFolderDialog = new JDialog(this, string("prj-b-make-folder"), true);
            JUtilities.makeUtilityWindow(newFolderDialog);

            String name;
            if (!suggestedNameUsed) {
                name = suggestedName;
                suggestedNameUsed = true;
            } else {
                name = string("pa-new-folder-name");
            }

            JTextField nameField = new JTextField(name, 20);
            nameField.addActionListener((ActionEvent e) -> {
                createFolder = true;
                newFolderDialog.dispose();
            });
            nameField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        newFolderDialog.dispose();
                    }
                }
            });
            nameField.selectAll();
            nameField.setBorder(BorderFactory.createEmptyBorder());
            newFolderDialog.add(nameField);
            newFolderDialog.pack();
            newFolderDialog.setLocationRelativeTo(this);
            newFolderDialog.setVisible(true);

            if (!createFolder) {
                return;
            }

            TreePath parentPath = folderTree.getSelectionPath();
            File parent = folderTree.getFolderForTreePath(parentPath);
            if (parentPath == null || parent == null) {
                return;
            }

            folderTree.collapsePath(parentPath);

            FileSystemView fsv = folderTree.getFileSystemView();
            File newFolder = null;
            try {
                newFolder = fsv.createNewFolder(parent);
                File dest = new File(newFolder.getParentFile(), nameField.getText());
                if (newFolder.renameTo(dest)) {
                    newFolder = dest;
                } else {
                    newFolder.delete();
                    newFolder = null;
                }
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "failed to create folder", e);
            }
            if (newFolder == null) {
                UIManager.getLookAndFeel().provideErrorFeedback(this);
                return;
            }

            folderTree.reloadChildren(parentPath);
            folderTree.expandPath(parentPath);
            TreePath newFolderPath = folderTree.findChild(parentPath, nameField.getText());
            if (newFolderPath != null) {
                folderTree.setSelectionPath(newFolderPath);
            }
	}//GEN-LAST:event_newFolderBtnActionPerformed
    private boolean createFolder;

    public static boolean isFolderDialogEnabled() {
        return Settings.getShared().getYesNo("use-project-folder-selector");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private ca.cgjennings.ui.FolderTree folderTree;
    private javax.swing.JButton newFolderBtn;
    private javax.swing.JButton okBtn;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        selection = folderTree.getSelectedFolder();
        if (settingKey != null) {
            File toStore = selection;
            if (mode != Mode.SELECT_FOLDER) {
                toStore = selection.getParentFile();
                if (toStore == null) {
                    toStore = selection;
                }
            }
            Settings.getUser().set(settingKey, toStore.getAbsolutePath());
        }
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    private class ProjectTree extends FolderTree {

        public ProjectTree(File root) {
            super(root);
        }

        public ProjectTree() {
        }

        @Override
        protected Icon getIconForNode(Node n) {
            if (isProjectFolder(n)) {
                return MetadataSource.ICON_PROJECT;
            } else {
                return super.getIconForNode(n);
            }
        }

        @Override
        protected File[] filterFolders(File[] source) {
            if (source == null) {
                return null;
            }
            FileSystemView fsv = getFileSystemView();
            LinkedList<File> kids = new LinkedList<>();
            for (File s : source) {
                if (s.isHidden() || s.getName().startsWith(".") || !s.exists()) {
                    continue;
                }

                boolean add;
                switch (mode) {
                    case SELECT_FOLDER:
                        add = fsv.isTraversable(s);
                        break;
                    case SELECT_PROJECT:
                    case SELECT_PROJECT_CONTAINER:
                        add = fsv.isTraversable(s) || Project.isProjectPackage(s);
                        break;
                    default:
                        throw new AssertionError();
                }
                if (add) {
                    kids.add(s);
                }
            }
            if (kids.size() == source.length) {
                return source;
            }
            return kids.toArray(new File[0]);
        }

        @Override
        protected void aboutToAddChildNodes(Node[] children) {
            super.aboutToAddChildNodes(children);
            for (int i = 0; i < children.length; ++i) {
                Object uo = null;
                File f = children[i].getFile();
                try {
                    if (mode != Mode.SELECT_FOLDER && (Project.isProjectFolder(f) || Project.isProjectPackage(f))) {
                        uo = Boolean.TRUE;
                        children[i].checkedForChildren = true;
                        children[i].hasChildren = false;
                    }
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, "exception while checking if folder is a project", e);
                }
                children[i].userObject = uo;
            }
        }

        protected boolean isProjectFolder(Node n) {
            return n.userObject == Boolean.TRUE;
        }

        public boolean isProjectSelected() {
            TreePath path = getSelectionPath();
            if (path == null) {
                return false;
            }
            return isProjectFolder((Node) path.getLastPathComponent());
        }
    }
}
