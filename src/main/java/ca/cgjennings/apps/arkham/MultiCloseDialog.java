package ca.cgjennings.apps.arkham;

import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.theme.Theme;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.UIManager;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * This dialog is typically used when there are files with unsaved changes during
 * application exit. Instead of saving the files one at a time, it displays a
 * list of all such files in a single dialog so that they can be dealt with as a
 * unit.
 *
 * <p>
 * This is a single use object: create it, then call {@link #showDialog()}. If
 * it returns {@code false}, the operation was cancelled. Otherwise, the user wishes
 * to proceed with the operation. If the operation is cancelled, a new dialog must be
 * created on the next operation attempt. Note that it is always safe to create this
 * dialog; if there are no unsaved files, {@link #showDialog()} will return
 * {@code true} without displaying anything.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class MultiCloseDialog extends javax.swing.JDialog implements AgnosticDialog {

    /**
     * Stores info needed to represent a tab in the close list.
     */
    private static final class TabInfo implements Comparable<TabInfo>, IconProvider {

        public TabInfo(int index, StrangeEonsEditor editor) {
            this.index = index;
            this.editor = editor;
            icon = editor.getFrameIcon();
            label = editor.getTitle();
            file = editor.getFile();
        }

        int index;
        Icon icon;
        String label;
        File file;
        StrangeEonsEditor editor;

        @Override
        public int compareTo(TabInfo o) {
            return index - o.index;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        public void discard() {
            if (editor instanceof AbstractSupportEditor) {
                AbstractSupportEditor ase = (AbstractSupportEditor) editor;
                ase.setUnsavedChanges(false);
            } else if (editor instanceof AbstractGameComponentEditor) {
                AbstractGameComponentEditor<?> agc = (AbstractGameComponentEditor<?>) editor;
                agc.setUnsavedChanges(false);
            }
            // If an unknown type of StrangeEonsEditor subclass, we don't know
            // how to clear the unsaved change flag, so this might pop up an
            // extra dialog later.
            editor.close();
        }

        public boolean save() {
            if (file == null) {
                editor.select();
                editor.saveAs();
                file = editor.getFile();
            } else {
                editor.save();
            }
            return file != null;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private boolean cancel;
    private final DefaultListModel<TabInfo> model;
    private List<StrangeEonsEditor> ignoreList;

    /**
     * Creates new form MultiCloseDialog
     */
    public MultiCloseDialog(java.awt.Frame parent) {
        super(parent, true);
        initComponents();
        getRootPane().setDefaultButton(saveAllBtn);
        PlatformSupport.makeAgnosticDialog(this, saveAllBtn, cancelBtn);

        model = new DefaultListModel<>();
        fileList.setModel(model);
    }
    
    private void createTabModel() {
        model.removeAllElements();
        StrangeEonsEditor[] eds = StrangeEons.getWindow().getEditors();
        for (int i = 0; i < eds.length; ++i) {
            if (ignoreList != null && ignoreList.contains(eds[i])) {
                continue;
            }
            if (eds[i].hasUnsavedChanges()) {
                model.addElement(new TabInfo(i, eds[i]));
            }
        }        
    }
    
    public MultiCloseDialog setIgnoreList(List<StrangeEonsEditor> toIgnore) {
        ignoreList = toIgnore;
        return this;
    }
    
    public List<StrangeEonsEditor> getIgnoreList() {
        return ignoreList;
    }

    /**
     * Displays the dialog and waits for the user to handle files
     * with unsaved changes by either closing them or saving and closing them.
     * @return true if all of the files were eventually closed
     */
    public boolean showDialog() {
        createTabModel();
        if (!model.isEmpty()) {
            cancel = true;
            pack();
            setLocationRelativeTo(getParent());
            setVisible(true);
        }
        return !cancel;
    }

    @Override
    public void handleOKAction(ActionEvent e) {
        processSelected(true, false);
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        cancel = true;
        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancelBtn = new javax.swing.JButton();
        saveAllBtn = new javax.swing.JButton();
        discardAllBtn = new javax.swing.JButton();
        fileScroll = new javax.swing.JScrollPane();
        fileList = new JIconList<>();
        jSeparator1 = new javax.swing.JSeparator();
        saveSelBtn = new javax.swing.JButton();
        discardSelBtn = new javax.swing.JButton();
        headBannerPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        warningIcon = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("app-l-multiclose-title")); // NOI18N
        setName("Form"); // NOI18N

        cancelBtn.setText(string("cancel")); // NOI18N
        cancelBtn.setName("cancelBtn"); // NOI18N

        saveAllBtn.setText(string("app-b-save-all")); // NOI18N
        saveAllBtn.setName("saveAllBtn"); // NOI18N

        discardAllBtn.setText(string("app-b-discard-all")); // NOI18N
        discardAllBtn.setName("discardAllBtn"); // NOI18N
        discardAllBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discardAllBtnActionPerformed(evt);
            }
        });

        fileScroll.setName("fileScroll"); // NOI18N

        fileList.setName("fileList"); // NOI18N
        fileList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                fileListValueChanged(evt);
            }
        });
        fileScroll.setViewportView(fileList);

        jSeparator1.setName("jSeparator1"); // NOI18N

        saveSelBtn.setFont(saveSelBtn.getFont().deriveFont(saveSelBtn.getFont().getSize()-1f));
        saveSelBtn.setText(string("app-b-save-sel")); // NOI18N
        saveSelBtn.setName("saveSelBtn"); // NOI18N
        saveSelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSelBtnActionPerformed(evt);
            }
        });

        discardSelBtn.setFont(discardSelBtn.getFont().deriveFont(discardSelBtn.getFont().getSize()-1f));
        discardSelBtn.setText(string("app-b-discard-sel")); // NOI18N
        discardSelBtn.setName("discardSelBtn"); // NOI18N
        discardSelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discardSelBtnActionPerformed(evt);
            }
        });

        headBannerPanel.setBackground(UIManager.getColor(Theme.HEAD_BANNER_BACKGROUND));
        headBannerPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        headBannerPanel.setName("headBannerPanel"); // NOI18N

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-1f));
        jLabel2.setForeground(UIManager.getColor(Theme.HEAD_BANNER_FOREGROUND));
        jLabel2.setText(string("app-l-multiclose-2")); // NOI18N
        jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 6, 0));
        jLabel2.setName("jLabel2"); // NOI18N

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel1.setForeground(UIManager.getColor(Theme.HEAD_BANNER_FOREGROUND));
        jLabel1.setText(string("app-l-multiclose-1")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        warningIcon.setIcon(ResourceKit.getIcon("application/warning.png").medium());
        warningIcon.setName("warningIcon"); // NOI18N

        javax.swing.GroupLayout headBannerPanelLayout = new javax.swing.GroupLayout(headBannerPanel);
        headBannerPanel.setLayout(headBannerPanelLayout);
        headBannerPanelLayout.setHorizontalGroup(
            headBannerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headBannerPanelLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(warningIcon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(headBannerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        headBannerPanelLayout.setVerticalGroup(
            headBannerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headBannerPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(headBannerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(warningIcon)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, headBannerPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(1, 1, 1)
                        .addComponent(jLabel2)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fileScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(discardAllBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(saveAllBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(discardSelBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(saveSelBtn)))
                .addContainerGap())
            .addComponent(headBannerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, discardAllBtn, saveAllBtn});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {discardSelBtn, saveSelBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(headBannerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(fileScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveSelBtn)
                    .addComponent(discardSelBtn))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(saveAllBtn)
                    .addComponent(discardAllBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void saveSelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSelBtnActionPerformed
        processSelected(false, false);
    }//GEN-LAST:event_saveSelBtnActionPerformed

    private void discardSelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discardSelBtnActionPerformed
        processSelected(false, true);
    }//GEN-LAST:event_discardSelBtnActionPerformed

    private void fileListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_fileListValueChanged
        // if a file entry has been clicked, select it to help user differentiate
        // files with the same name
        TabInfo sel = (TabInfo) fileList.getSelectedValue();
        if (sel != null) {
            sel.editor.select();
        }
    }//GEN-LAST:event_fileListValueChanged

    private void discardAllBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_discardAllBtnActionPerformed
        processSelected(true, true);
    }//GEN-LAST:event_discardAllBtnActionPerformed

    private void processSelected(boolean applyToAll, boolean discard) {
        List<TabInfo> selected;
        if (applyToAll) {
            selected = new LinkedList<>();
            final int size = model.getSize();
            for (int i=0; i<size; ++i) {
                selected.add(model.getElementAt(i));
            }
        } else {
            selected = fileList.getSelectedValuesList();
        }
        
        for (TabInfo tab : selected) {
            if (!discard && !tab.save()) {
                break;                
            }
            tab.discard();
        }
        
        createTabModel();
        
        if (model.isEmpty()) {
            cancel = false;
            dispose();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JButton discardAllBtn;
    private javax.swing.JButton discardSelBtn;
    private javax.swing.JList<TabInfo> fileList;
    private javax.swing.JScrollPane fileScroll;
    private javax.swing.JPanel headBannerPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JButton saveAllBtn;
    private javax.swing.JButton saveSelBtn;
    private javax.swing.JLabel warningIcon;
    // End of variables declaration//GEN-END:variables
}
