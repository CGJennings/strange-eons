package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Dialog to prompt user before replacing files during a copy operation.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class ReplaceDialog extends javax.swing.JDialog implements AgnosticDialog {

    /**
     * Creates new form ReplaceDialog
     */
    public ReplaceDialog(java.awt.Frame parent, File replaceFile, File existingFile) {
        super(parent, true);
        initComponents();
        infoLabel.setText(string("proj-l-replace-file", existingFile.getName()));
        okBtn.setText(string("proj-rename-file", ProjectUtilities.getAvailableFile(existingFile).getName()));

        Project p = StrangeEons.getWindow().getOpenProject();
        if (p != null) {
            Member m = p.findMember(replaceFile);
            if (m != null) {
                newFileName.setIcon(m.getIcon());
            }
            m = p.findMember(existingFile);
            if (m != null) {
                oldFileName.setIcon(m.getIcon());
            }
        }
        newFileName.setText(replaceFile.getName());
        oldFileName.setText(existingFile.getName());
        if (replaceFile.length() >= 0) {
            newFileSize.setText(ProjectUtilities.formatByteSize(replaceFile.length()));
        } else {
            newFileSize.setText(" ");
        }

        if (existingFile.length() >= 0) {
            oldFileSize.setText(ProjectUtilities.formatByteSize(existingFile.length()));
        } else {
            oldFileSize.setText(" ");
        }

        Date modtime = new Date(replaceFile.lastModified());
        newFileModTime.setText(dateFormatter.format(modtime) + " " + timeFormatter.format(modtime));
        modtime = new Date(existingFile.lastModified());
        oldFileModTime.setText(dateFormatter.format(modtime) + " " + timeFormatter.format(modtime));

        getRootPane().setDefaultButton(cancelBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        pack();
        setLocationRelativeTo(parent);
    }
    // formatters used by the default metadata impl to fill in modification times
    private final DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private final DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        replaceAllBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        replaceBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        srcPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        newFileName = new javax.swing.JLabel();
        newFileSize = new javax.swing.JLabel();
        newFileModTime = new javax.swing.JLabel();
        destPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        oldFileName = new javax.swing.JLabel();
        oldFileSize = new javax.swing.JLabel();
        oldFileModTime = new javax.swing.JLabel();
        javax.swing.JPanel titlePanel = new javax.swing.JPanel();
        infoLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "proj-title-replace" )); // NOI18N

        replaceAllBtn.setText(string( "proj-replace-all-files" )); // NOI18N
        replaceAllBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceAllBtnActionPerformed(evt);
            }
        });

        cancelBtn.setText(string( "cancel" )); // NOI18N

        replaceBtn.setText(string( "proj-replace-file" )); // NOI18N
        replaceBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replaceBtnActionPerformed(evt);
            }
        });

        okBtn.setText(string( "proj-rename-file" )); // NOI18N

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel3.setText(string( "proj-replace-replacement" )); // NOI18N

        newFileName.setText("file name");

        newFileSize.setFont(newFileSize.getFont().deriveFont(newFileSize.getFont().getSize()-1f));
        newFileSize.setText("XXX KiB");

        newFileModTime.setFont(newFileModTime.getFont().deriveFont(newFileModTime.getFont().getSize()-1f));
        newFileModTime.setText("changed today");

        javax.swing.GroupLayout srcPanelLayout = new javax.swing.GroupLayout(srcPanel);
        srcPanel.setLayout(srcPanelLayout);
        srcPanelLayout.setHorizontalGroup(
            srcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(srcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(srcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(srcPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(srcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(newFileName)
                            .addGroup(srcPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(srcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(newFileModTime)
                                    .addComponent(newFileSize))))))
                .addContainerGap(59, Short.MAX_VALUE))
        );
        srcPanelLayout.setVerticalGroup(
            srcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(srcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(newFileName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(newFileSize)
                .addGap(2, 2, 2)
                .addComponent(newFileModTime)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel4.setText(string( "proj-replace-existing" )); // NOI18N

        oldFileName.setText("file name");

        oldFileSize.setFont(oldFileSize.getFont().deriveFont(oldFileSize.getFont().getSize()-1f));
        oldFileSize.setText("XXX KiB");

        oldFileModTime.setFont(oldFileModTime.getFont().deriveFont(oldFileModTime.getFont().getSize()-1f));
        oldFileModTime.setText("changed today");

        javax.swing.GroupLayout destPanelLayout = new javax.swing.GroupLayout(destPanel);
        destPanel.setLayout(destPanelLayout);
        destPanelLayout.setHorizontalGroup(
            destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(destPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(destPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(oldFileName)
                            .addGroup(destPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(oldFileModTime)
                                    .addComponent(oldFileSize)))))
                    .addComponent(jLabel4))
                .addContainerGap(72, Short.MAX_VALUE))
        );
        destPanelLayout.setVerticalGroup(
            destPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(destPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(oldFileName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(oldFileSize)
                .addGap(2, 2, 2)
                .addComponent(oldFileModTime)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        titlePanel.setBackground(ThemeInstaller.isDark() ? Color.BLACK : Color.WHITE);
        titlePanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        infoLabel.setFont(infoLabel.getFont().deriveFont(infoLabel.getFont().getStyle() | java.awt.Font.BOLD));
        infoLabel.setText(string( "proj-l-replace-file" )); // NOI18N

        jLabel2.setText(string( "proj-l-replace-file2" )); // NOI18N

        jLabel1.setIcon(ResourceKit.getIcon("application/app.png").derive(64, 64));

        javax.swing.GroupLayout titlePanelLayout = new javax.swing.GroupLayout(titlePanel);
        titlePanel.setLayout(titlePanelLayout);
        titlePanelLayout.setHorizontalGroup(
            titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, titlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(infoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 492, Short.MAX_VALUE)
                    .addComponent(jLabel2))
                .addContainerGap())
        );
        titlePanelLayout.setVerticalGroup(
            titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(titlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(titlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addGroup(titlePanelLayout.createSequentialGroup()
                        .addComponent(infoLabel)
                        .addGap(2, 2, 2)
                        .addComponent(jLabel2)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(replaceAllBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(replaceBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 147, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(srcPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(destPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addComponent(titlePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(titlePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(destPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(srcPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(replaceAllBtn)
                    .addComponent(cancelBtn)
                    .addComponent(replaceBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

	private void replaceAllBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceAllBtnActionPerformed
            result = REPLACE_ALL;
            dispose();
	}//GEN-LAST:event_replaceAllBtnActionPerformed

	private void replaceBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replaceBtnActionPerformed
            result = REPLACE;
            dispose();
	}//GEN-LAST:event_replaceBtnActionPerformed

    @Override
    public void handleOKAction(ActionEvent e) {
        result = RENAME;
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        result = CANCEL;
        dispose();
    }

    public int showDialog() {
        result = 0;
        setVisible(true);
        return result;
    }
    private int result;

    public static final int RENAME = 1;
    public static final int CANCEL = 0;
    public static final int REPLACE = -1;
    public static final int REPLACE_ALL = -2;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel destPanel;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel newFileModTime;
    private javax.swing.JLabel newFileName;
    private javax.swing.JLabel newFileSize;
    private javax.swing.JButton okBtn;
    private javax.swing.JLabel oldFileModTime;
    private javax.swing.JLabel oldFileName;
    private javax.swing.JLabel oldFileSize;
    private javax.swing.JButton replaceAllBtn;
    private javax.swing.JButton replaceBtn;
    private javax.swing.JPanel srcPanel;
    // End of variables declaration//GEN-END:variables

}