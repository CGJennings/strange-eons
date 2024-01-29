package ca.cgjennings.apps.arkham.dialog.prefs;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.InstalledTheme;
import ca.cgjennings.ui.anim.AnimationUtilities;
import ca.cgjennings.ui.theme.ThemeInstaller;
import ca.cgjennings.ui.theme.ThemedSingleImageIcon;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * Category panel for themes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class CatTheme extends javax.swing.JPanel implements PreferenceCategory {

    public CatTheme() {
        initComponents();
        FillInPreferenceCategory.style(this);

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        markupFamily.setModel(model);
        markupSize.getEditor().setOpaque(false);
        recentFileField.getEditor().setOpaque(false);
    }

    private static final String SYSTEM_LAF_VALUE = "Tcho-Tcho";

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        apiGroup = new javax.swing.ButtonGroup();
        themeLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel themeSect = new javax.swing.JLabel();
        lightThemeBox = new javax.swing.JLabel();
        javax.swing.JLabel validationRulesSect = new javax.swing.JLabel();
        markupFamily = new javax.swing.JComboBox<>();
        markupBold = new javax.swing.JCheckBox();
        markupItalic = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        markupSize = new javax.swing.JSpinner();
        javax.swing.JLabel hideRecentFileLabel = new javax.swing.JLabel();
        openZipCheck = new javax.swing.JCheckBox();
        javax.swing.JLabel miscSect = new javax.swing.JLabel();
        singleInstanceCheck = new javax.swing.JCheckBox();
        javax.swing.JLabel recentFileLabel = new javax.swing.JLabel();
        recentFileField = new javax.swing.JSpinner();
        useDarkCheck = new javax.swing.JCheckBox();
        javax.swing.JLabel darkThemeLabel = new javax.swing.JLabel();
        darkThemeBox = new javax.swing.JLabel();
        darkThemeCombo = new javax.swing.JComboBox<>();
        lightThemeCombo = new javax.swing.JComboBox<>();
        javax.swing.JLabel darkThemeLabel1 = new javax.swing.JLabel();
        darkScreenshot = new javax.swing.JLabel();
        lightScreenshot = new javax.swing.JLabel();

        setBackground(java.awt.Color.white);

        themeLabel1.setForeground(new java.awt.Color(0, 4, 0));
        themeLabel1.setText(string("sd-l-theme-desc")); // NOI18N

        themeSect.setFont(themeSect.getFont().deriveFont(themeSect.getFont().getStyle() | java.awt.Font.BOLD, themeSect.getFont().getSize()+3));
        themeSect.setForeground(new java.awt.Color(135, 103, 5));
        themeSect.setText(string("sd-l-theme")); // NOI18N

        lightThemeBox.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lightThemeBox.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lightThemeBox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 226, 120), 2, true));

        validationRulesSect.setFont(validationRulesSect.getFont().deriveFont(validationRulesSect.getFont().getStyle() | java.awt.Font.BOLD, validationRulesSect.getFont().getSize()+1));
        validationRulesSect.setForeground(new java.awt.Color(135, 103, 5));
        validationRulesSect.setText(string("sd-l-markup-font")); // NOI18N

        markupFamily.setEditable(true);
        markupFamily.setMaximumRowCount(12);

        markupBold.setFont(markupBold.getFont().deriveFont(markupBold.getFont().getStyle() | java.awt.Font.BOLD));
        markupBold.setText(string( "ffd-l-bold" )); // NOI18N

        markupItalic.setFont(markupItalic.getFont().deriveFont((markupItalic.getFont().getStyle() | java.awt.Font.ITALIC)));
        markupItalic.setText(string( "ffd-l-italic" )); // NOI18N

        jLabel1.setText(string( "ffd-l-size" )); // NOI18N

        markupSize.setModel(new javax.swing.SpinnerNumberModel(12, 6, 36, 1));

        hideRecentFileLabel.setFont(hideRecentFileLabel.getFont().deriveFont(hideRecentFileLabel.getFont().getSize()-2f));
        hideRecentFileLabel.setText(string("sd-l-zero-recent-files")); // NOI18N

        openZipCheck.setForeground(new java.awt.Color(0, 4, 0));
        openZipCheck.setText(string("sd-b-open-zip")); // NOI18N
        openZipCheck.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        openZipCheck.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        miscSect.setFont(miscSect.getFont().deriveFont(miscSect.getFont().getStyle() | java.awt.Font.BOLD, miscSect.getFont().getSize()+3));
        miscSect.setForeground(new java.awt.Color(135, 103, 5));
        miscSect.setText(string("sd-l-misc")); // NOI18N

        singleInstanceCheck.setForeground(new java.awt.Color(0, 4, 0));
        singleInstanceCheck.setText(string("sd-b-misc-single-instance")); // NOI18N
        singleInstanceCheck.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        singleInstanceCheck.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        recentFileLabel.setForeground(new java.awt.Color(0, 4, 0));
        recentFileLabel.setText(string("sd-l-recent-files")); // NOI18N

        recentFileField.setModel(new javax.swing.SpinnerNumberModel(0, 0, 25, 1));

        useDarkCheck.setText(string("sd-l-theme-use-dark-alt")); // NOI18N
        useDarkCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useDarkCheckActionPerformed(evt);
            }
        });

        darkThemeLabel.setFont(darkThemeLabel.getFont().deriveFont(darkThemeLabel.getFont().getStyle() | java.awt.Font.BOLD, darkThemeLabel.getFont().getSize()+1));
        darkThemeLabel.setForeground(new java.awt.Color(135, 103, 5));
        darkThemeLabel.setText(string("sd-l-theme-dark")); // NOI18N

        darkThemeBox.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        darkThemeBox.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        darkThemeBox.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 226, 120), 2, true));

        darkThemeCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                darkThemeComboFocusLost(evt);
            }
        });
        darkThemeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                darkThemeComboActionPerformed(evt);
            }
        });

        lightThemeCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                lightThemeComboFocusLost(evt);
            }
        });
        lightThemeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lightThemeComboActionPerformed(evt);
            }
        });

        darkThemeLabel1.setFont(darkThemeLabel1.getFont().deriveFont(darkThemeLabel1.getFont().getStyle() | java.awt.Font.BOLD, darkThemeLabel1.getFont().getSize()+1));
        darkThemeLabel1.setForeground(new java.awt.Color(135, 103, 5));
        darkThemeLabel1.setText(string("sd-l-theme-light")); // NOI18N

        darkScreenshot.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);

        lightScreenshot.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(themeLabel1))
                    .addComponent(themeSect)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(validationRulesSect)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(markupSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(markupBold)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(markupItalic))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(markupFamily, javax.swing.GroupLayout.PREFERRED_SIZE, 265, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(miscSect)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(singleInstanceCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(openZipCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(recentFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addComponent(recentFileField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(hideRecentFileLabel))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(darkThemeLabel)
                            .addComponent(useDarkCheck)
                            .addComponent(darkThemeLabel1)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lightThemeBox, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(lightScreenshot, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lightThemeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(darkThemeBox, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(darkScreenshot, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(darkThemeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(themeSect)
                .addGap(8, 8, 8)
                .addComponent(themeLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(darkThemeLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(lightThemeBox, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lightScreenshot, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lightThemeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(darkThemeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useDarkCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(darkThemeBox, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(darkScreenshot, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(darkThemeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(validationRulesSect)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(markupFamily, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(markupSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(markupBold)
                    .addComponent(markupItalic))
                .addGap(18, 18, 18)
                .addComponent(miscSect)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(openZipCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(singleInstanceCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(recentFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(hideRecentFileLabel)
                    .addComponent(recentFileField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

	private void lightThemeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lightThemeComboActionPerformed
            updateThemeIcons(true);
}//GEN-LAST:event_lightThemeComboActionPerformed

	private void lightThemeComboFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_lightThemeComboFocusLost
            updateThemeIcons(true);
}//GEN-LAST:event_lightThemeComboFocusLost

    private void useDarkCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useDarkCheckActionPerformed
        boolean checked = useDarkCheck.isSelected();
        darkThemeBox.setEnabled(checked);
        darkThemeCombo.setEnabled(checked);
        darkScreenshot.setEnabled(checked);
    }//GEN-LAST:event_useDarkCheckActionPerformed

    private void darkThemeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_darkThemeComboActionPerformed
        updateThemeIcons(false);
    }//GEN-LAST:event_darkThemeComboActionPerformed

    private void darkThemeComboFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_darkThemeComboFocusLost
        updateThemeIcons(false);
    }//GEN-LAST:event_darkThemeComboFocusLost

    private void updateThemeIcons(boolean light) {
        InstalledTheme it;
        JComboBox<InstalledTheme> themeCombo;
        JLabel themeBox, screenshotBox;

        if (light) {
            themeCombo = lightThemeCombo;
            themeBox = lightThemeBox;
            screenshotBox = lightScreenshot;
        } else {
            themeCombo = darkThemeCombo;
            themeBox = darkThemeBox;
            screenshotBox = darkScreenshot;
        }

        it = (InstalledTheme) themeCombo.getSelectedItem();
        if (it != null) {
            AnimationUtilities.animateIconTransition(themeBox, it.getLargeIcon());
            new Thread(() -> {
                BufferedImage im = it.getScreenshot();
                EventQueue.invokeLater(() -> {
                    if (it == themeCombo.getSelectedItem()) {
                        if (im == null) {
                            screenshotBox.setIcon(null);
                            screenshotBox.setToolTipText(null);
                        } else {
                            screenshotBox.setIcon(new ThemedSingleImageIcon(im, 67, 50));
                            screenshotBox.setToolTipText("<html><img width=200 height=150 src='" + it.getScreenshotUrl() + "'>");
                        }
                    }
                });
            }).start();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup apiGroup;
    private javax.swing.JLabel darkScreenshot;
    private javax.swing.JLabel darkThemeBox;
    private javax.swing.JComboBox<InstalledTheme> darkThemeCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lightScreenshot;
    private javax.swing.JLabel lightThemeBox;
    private javax.swing.JComboBox<InstalledTheme> lightThemeCombo;
    private javax.swing.JCheckBox markupBold;
    private javax.swing.JComboBox<String> markupFamily;
    private javax.swing.JCheckBox markupItalic;
    private javax.swing.JSpinner markupSize;
    private javax.swing.JCheckBox openZipCheck;
    private javax.swing.JSpinner recentFileField;
    private javax.swing.JCheckBox singleInstanceCheck;
    private javax.swing.JLabel themeLabel1;
    private javax.swing.JCheckBox useDarkCheck;
    // End of variables declaration//GEN-END:variables

    @Override
    public Icon getIcon() {
        return catIcon;
    }
    private final Icon catIcon = ResourceKit.getIcon("prefs/look-and-feel.png");

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public String getTitle() {
        return string("sd-l-laf");
    }

    private static final String[] restartKeys = {
        "theme", "dark-theme",
        "auto-select-dark-theme",
        "edit-font-family", "edit-font-pointsize",
        "edit-font-bold", "edit-font-italic"
    };
    private final String[] restartValues = new String[restartKeys.length];

    @Override
    public boolean isRestartRequired() {
        boolean restart = false;
        for (int i = 0; i < restartKeys.length; ++i) {
            String newVal = Settings.getShared().get(restartKeys[i]);
            if (restartValues[i] == null) {
                if (newVal != null) {
                    restart = true;
                    StrangeEons.log.log(Level.INFO, "restart key triggered: {0}", restartKeys[i]);
                }
            } else if (!restartValues[i].equals(newVal)) {
                restart = true;
                StrangeEons.log.log(Level.INFO, "restart key triggered: {0}", restartKeys[i]);
            }
        }

        if (lightThemeCombo.isEnabled() && (initialValue != lightThemeCombo.getSelectedIndex())) {
            restart = true;
            StrangeEons.log.info("restart triggered by theme change");
        }
        return restart;
    }

    private int indexOfTheme(String themeClass, InstalledTheme[] themes) {
        int i;
        for (i = 0; i < themes.length; ++i) {
            if (themes[i].getThemeClass().equals(themeClass)) {
                break;
            }
        }
        if (i == themes.length) {
            // could not find user preference value in list, probably the theme
            // was uninstalled --> use the actual theme selected by ThemeInstaller
            themeClass = ThemeInstaller.getInstalledTheme().getClass().getName();
            for (i = 0; i < themes.length; ++i) {
                if (themes[i].getThemeClass().equals(themeClass)) {
                    break;
                }
            }
        }
        return i < themes.length ? i : -1;
    }

    @Override
    public void loadSettings() {
        final Settings user = Settings.getUser();

        // Theme
        String lightTheme = user.get("theme", "ca.cgjennings.ui.theme.HydraTheme");
        InstalledTheme[] themes = BundleInstaller.getInstalledThemes();
        DefaultComboBoxModel<InstalledTheme> themeModel = new DefaultComboBoxModel<>(themes);
        lightThemeCombo.setModel(themeModel);
        lightThemeCombo.setEnabled(themes.length > 1);
        lightThemeCombo.setSelectedIndex(indexOfTheme(lightTheme, themes));
        initialValue = lightThemeCombo.getSelectedIndex();

        loadCheck("auto-select-dark-theme", useDarkCheck);

        String darkTheme = user.get("dark-theme", "ca.cgjennings.ui.theme.YuggothTheme");
        DefaultComboBoxModel<InstalledTheme> darkThemeModel = new DefaultComboBoxModel<>(themes);
        darkThemeCombo.setModel(darkThemeModel);
        darkThemeCombo.setSelectedIndex(indexOfTheme(darkTheme, themes));
        darkThemeCombo.setEnabled(themes.length > 1 && useDarkCheck.isSelected());

        for (int i = 0; i < restartKeys.length; ++i) {
            restartValues[i] = user.get(restartKeys[i]);
        }

        markupFamily.setSelectedItem(user.get("edit-font-family", "default"));
        loadCheck("edit-font-bold", markupBold);
        loadCheck("edit-font-italic", markupItalic);

        loadCheck("open-zip-after-export", openZipCheck);

        // only the true user settings are checked for this value
        if (user.get("limit-to-single-instance") == null) {
            user.set("limit-to-single-instance", "yes");
        }
        loadCheck("limit-to-single-instance", singleInstanceCheck);

        recentFileField.setValue(
                Math.max(0, Math.min(25, user.getInt("recent-file-menu-length")))
        );
    }

    private void loadCheck(String key, JCheckBox box) {
        box.setSelected(Settings.getUser().getYesNo(key));
    }

    private void saveCheck(String key, JCheckBox box) {
        Settings.getUser().set(key, box.isSelected() ? "yes" : "no");
    }

    private int initialValue;

    @Override
    public void storeSettings() {
        final Settings user = Settings.getUser();

        InstalledTheme it = (InstalledTheme) lightThemeCombo.getSelectedItem();
        if (it != null) {
            user.set("theme", it.getThemeClass());
        }
        it = (InstalledTheme) darkThemeCombo.getSelectedItem();
        if (it != null) {
            user.set("dark-theme", it.getThemeClass());
        }
        saveCheck("auto-select-dark-theme", useDarkCheck);

        Object family = markupFamily.getSelectedItem();
        if (family != null) {
            user.set("edit-font-family", family.toString());
        }
        saveCheck("edit-font-bold", markupBold);
        saveCheck("edit-font-italic", markupItalic);

        saveCheck("open-zip-after-export", openZipCheck);
        saveCheck("limit-to-single-instance", singleInstanceCheck);
        user.set("recent-file-menu-length", String.valueOf(((Number) recentFileField.getValue()).intValue()));
    }
}
