package ca.cgjennings.apps.arkham.dialog.prefs;

import static resources.Language.string;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import ca.cgjennings.graphics.cloudfonts.CloudFontFamily;
import ca.cgjennings.graphics.cloudfonts.CloudFonts;
import resources.ResourceKit;

/**
 * Preference category for managing reserved cloud fonts.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class CatReservedCloudFonts extends javax.swing.JPanel implements PreferenceCategory {

    /**
     * Creates new form CatReservedCloudFonts
     */
    public CatReservedCloudFonts() { 
        initComponents();
        FillInPreferenceCategory.style(this);
        reserveButton.setEnabled(false);
        reserveButton.addActionListener(this::reserveButtonPressed);
        cloudFontPanel.addFamilySelectionListener(this::selectionChanged);
        cloudFontPanel.addFilterChangedListener(ev -> {
            numMatchesLabel.setText(String.valueOf(cloudFontPanel.getFilteredFamilyCount()) + '/' + cloudFontPanel.getFamilyCount());
        });
        // TODO: for future use
        refreshButton.setVisible(false);
    }

    private void reserveButtonPressed(ActionEvent e) {
        var sel = cloudFontPanel.getSelectedFamilies();
        if (sel == null || sel.isEmpty()) {
            return;
        }
        if (allEntriesAreReserved(sel)) {
            for (CloudFontFamily family : sel) {
                CloudFonts.removeReservedFamily(family);
            }
        } else {
            for (CloudFontFamily family : sel) {
                CloudFonts.addReservedFamily(family);
            }
        }
    }

    private void selectionChanged(ActionEvent ev) {
        var sel = cloudFontPanel.getSelectedFamilies();
        if (sel == null || sel.isEmpty()) {
            reserveButton.setText(string("clf-reserve"));
            reserveButton.setEnabled(false);
        } else {
            if (allEntriesAreReserved(sel)) {
                reserveButton.setText(string("clf-unreserve"));
            } else {
                reserveButton.setText(string("clf-reserve"));
            }
            reserveButton.setEnabled(true);
        }
    }

    private boolean allEntriesAreReserved(List<CloudFontFamily> list) {
        for (CloudFontFamily family : list) {
            if (!CloudFonts.isReservedFamily(family)) {
                return false;
            }
        }
        return true;
    }    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        cloudFontPanel = new ca.cgjennings.graphics.cloudfonts.CloudFontExplorerPanel();
        reserveInfo = new javax.swing.JLabel();
        reserveButton = new javax.swing.JButton();
        refreshButton = new javax.swing.JButton();
        numMatchesLabel = new javax.swing.JLabel();

        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | java.awt.Font.BOLD, titleLabel.getFont().getSize()+3));
        titleLabel.setForeground(new java.awt.Color(135, 103, 5));
        titleLabel.setText(string("clf-title")); // NOI18N
        titleLabel.setName("titleLabel"); // NOI18N

        cloudFontPanel.setName("cloudFontPanel"); // NOI18N

        reserveInfo.setText(string("clf-pref-reserve")); // NOI18N
        reserveInfo.setName("reserveInfo"); // NOI18N

        reserveButton.setText(string("clf-reserve")); // NOI18N
        reserveButton.setName("reserveButton"); // NOI18N

        refreshButton.setIcon(ResourceKit.getIcon("cloud-font-refresh"));
        refreshButton.setText(string("clf-refresh")); // NOI18N
        refreshButton.setName("refreshButton"); // NOI18N

        numMatchesLabel.setText(" ");
        numMatchesLabel.setToolTipText("");
        numMatchesLabel.setName("numMatchesLabel"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(reserveInfo)
                        .addGap(18, 18, 18)
                        .addComponent(refreshButton)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(reserveButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(numMatchesLabel))
                            .addComponent(titleLabel)
                            .addComponent(cloudFontPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reserveInfo)
                    .addComponent(refreshButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cloudFontPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 290, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reserveButton)
                    .addComponent(numMatchesLabel))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private ca.cgjennings.graphics.cloudfonts.CloudFontExplorerPanel cloudFontPanel;
    private javax.swing.JLabel numMatchesLabel;
    private javax.swing.JButton refreshButton;
    private javax.swing.JButton reserveButton;
    private javax.swing.JLabel reserveInfo;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public String getTitle() {
        return titleLabel.getText();
    }

    @Override
    public Icon getIcon() {
        if (catIcon == null) {
            catIcon = ResourceKit.getIcon("cloud-font-prefs");
        }
        return catIcon;
    }
    private Icon catIcon;

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void loadSettings() {
    }

    @Override
    public void storeSettings() {
    }

    @Override
    public boolean isRestartRequired() {
        return false;
    }
}
