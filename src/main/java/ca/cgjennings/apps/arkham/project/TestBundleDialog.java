package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.DefaultCommandFormatter;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.Subprocess;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.prefs.LanguageCodeDescriptor;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JIconComboBox;
import ca.cgjennings.util.CommandFormatter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import resources.Language;
import static resources.Language.string;
import resources.Settings;

/**
 * Dialog that gets user configuration during a plug-in bundle test.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class TestBundleDialog extends javax.swing.JDialog implements AgnosticDialog {

    private File bundle;
    private final boolean copyBundle;

    /**
     * Creates new form TestBundleDialog
     */
    public TestBundleDialog(File bundle, boolean forceCopy) {
        super(StrangeEons.getWindow(), true);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        // create dummy bundle if required
        if (bundle == null) {
            forceCopy = false;
            try {
                bundle = File.createTempFile("plugin-test-", ".seext");
                ProjectUtilities.copyResourceToFile("projects/standin-bundle.seext", bundle);
            } catch (IOException ex) {
                ErrorDialog.displayError(string("prj-err-copy", bundle.getName()), ex);
                throw new IllegalArgumentException("Unable to create temporary bundle", ex);
            }
        }

        // Game Languages
        int sel = -1;
        Locale toMatch = Language.getGameLocale();
        for (Locale loc : Language.getGameLocales()) {
            gameCombo.addItem(new LanguageCodeDescriptor(loc));
            if (loc.equals(toMatch) || (sel == -1 && loc.getLanguage().equals(toMatch.getLanguage()))) {
                sel = gameCombo.getItemCount() - 1;
            }
        }
        gameCombo.setSelectedIndex(sel);

        // UI Locales
        sel = -1;
        toMatch = Language.getInterfaceLocale();
        for (Locale loc : Language.getInterfaceLocales()) {
            LanguageCodeDescriptor lcd = new LanguageCodeDescriptor(loc);
            uiCombo.addItem(lcd);
            if (loc.equals(toMatch) || (sel == -1 && loc.getLanguage().equals(toMatch.getLanguage()))) {
                sel = uiCombo.getItemCount() - 1;
            }
        }
        uiCombo.setSelectedIndex(sel);

        this.bundle = bundle;
        copyBundle = forceCopy;

        Settings s = Settings.getUser();

        String loglevel = s.get("test-bundle-log", "ALL");
        logLevelCombo.setSelectedItem(loglevel.toUpperCase(Locale.CANADA));
        doNotLoadPluginsCheck.setSelected(s.getYesNo("test-bundle-no-plugins"));
        jvmField.setText(s.get("test-bundle-vm-args", ""));
        argsField.setText(s.get("test-bundle-args", ""));
        jvmField.select(0, 0);
        argsField.select(0, 0);
        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jvmField = new javax.swing.JTextField();
        argsField = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        uiCombo =  new JIconComboBox() ;
        gameCombo =  new JIconComboBox() ;
        jLabel6 = new javax.swing.JLabel();
        logLevelCombo = new javax.swing.JComboBox();
        gameLocField = new javax.swing.JTextField();
        uiLocField = new javax.swing.JTextField();
        doNotLoadPluginsCheck = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "pa-test-bundle-title" )); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "pa-test-bundle-advanced" ))); // NOI18N

        jLabel1.setText(string( "pa-test-bundle-jvm" )); // NOI18N

        jLabel2.setText(string( "pa-test-bundle-args" )); // NOI18N

        jvmField.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N

        argsField.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(argsField, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                            .addComponent(jvmField))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jvmField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(argsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "pa-test-bundle-options" ))); // NOI18N

        jLabel4.setText(string( "pa-test-bundle-game-lang" )); // NOI18N

        jLabel5.setText(string( "pa-test-bundle-ui-locale" )); // NOI18N

        uiCombo.setMaximumRowCount(12);
        uiCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uiComboActionPerformed(evt);
            }
        });

        gameCombo.setMaximumRowCount(12);
        gameCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gameComboActionPerformed(evt);
            }
        });

        jLabel6.setText(string("pa-test-bundle-debug")); // NOI18N

        logLevelCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ALL", "FINE", "CONFIG", "INFO", "WARNING", "SEVERE", "OFF" }));

        gameLocField.setColumns(5);

        uiLocField.setColumns(5);

        doNotLoadPluginsCheck.setText(string("pa-test-bundle-no-plugins")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(gameLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(uiCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(uiLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(jLabel6)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(logLevelCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(doNotLoadPluginsCheck))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {gameCombo, uiCombo});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gameLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(uiCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(uiLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(logLevelCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(doNotLoadPluginsCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel3.setText(string( "pa-test-bundle-tt" )); // NOI18N

        cancelBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "pa-test-bundle-ok" )); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel3)
                            .addGap(61, 61, 61))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addContainerGap()))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn)
                        .addContainerGap())))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void gameComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gameComboActionPerformed
        LanguageCodeDescriptor lcd = (LanguageCodeDescriptor) gameCombo.getSelectedItem();
        if (lcd != null) {
            gameLocField.setText(lcd.getCode());
        }
    }//GEN-LAST:event_gameComboActionPerformed

    private void uiComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_uiComboActionPerformed
        LanguageCodeDescriptor lcd = (LanguageCodeDescriptor) uiCombo.getSelectedItem();
        if (lcd != null) {
            uiLocField.setText(lcd.getCode());
        }
    }//GEN-LAST:event_uiComboActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField argsField;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JCheckBox doNotLoadPluginsCheck;
    private javax.swing.JComboBox<LanguageCodeDescriptor> gameCombo;
    private javax.swing.JTextField gameLocField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField jvmField;
    private javax.swing.JComboBox logLevelCombo;
    private javax.swing.JButton okBtn;
    private javax.swing.JComboBox<LanguageCodeDescriptor> uiCombo;
    private javax.swing.JTextField uiLocField;
    // End of variables declaration//GEN-END:variables

    private String determineLocaleCode(JComboBox picker, JTextField manual) {
        // if there is a valid value in the text field, use that
        // (if the user picked something, the field was updated to the selection)
        String locale = manual.getText().trim();
        if (!Language.isLocaleDescriptionValid(locale)) {
            // if the field is invalid, use whatever was last selected in the combo
            locale = ((LanguageCodeDescriptor) picker.getSelectedItem()).getCode();
        }
        return locale;
    }

    @Override
    public void handleOKAction(ActionEvent evt) {
        Settings s = Settings.getUser();
        s.set("test-bundle-log", logLevelCombo.getSelectedItem().toString());
        s.setYesNo("test-bundle-no-plugins", doNotLoadPluginsCheck.isSelected());
        s.set("test-bundle-vm-args", jvmField.getText());
        s.set("test-bundle-args", argsField.getText());
        final String gLang = determineLocaleCode(gameCombo, gameLocField);
        final String uLang = determineLocaleCode(uiCombo, uiLocField);

        try {
            String name = bundle.getName();
            if (copyBundle) {
                File temp = File.createTempFile("se-test-bundle-", "." + ProjectUtilities.getFileExtension(bundle));
                temp.deleteOnExit();
                ProjectUtilities.copyFile(bundle, temp);
                bundle = temp;
            }

            String sejar = Subprocess.getClasspath();

            String loglevel = logLevelCombo.getSelectedItem().toString();
            try {
                loglevel = Level.parse(loglevel).toString();
            } catch (Exception e) {
                loglevel = "ALL";
                StrangeEons.log.log(Level.WARNING, null, e);
            }

            String noPluginsOption = doNotLoadPluginsCheck.isSelected() ? " --xDisablePluginLoading" : "";

            LinkedList<String> testArgs = new LinkedList<>();

            // start with the custom launch string, if any
            boolean isOverriden = false;
            String userOverride = s.get("test-bundle-launch");
            if (userOverride != null && !userOverride.isEmpty()) {
                isOverriden = true;
                CommandFormatter cf = new DefaultCommandFormatter();
                testArgs.addAll(Arrays.asList(cf.formatCommand(userOverride)));
            }

            // add the VM arguments
            appendArgField(testArgs, "test-bundle-vm-args", jvmField.getText());

            // add the command and dialog arguments
            testArgs.addAll(Arrays.asList(new String[]{
                "strangeeons",
                "--glang", gLang,
                "--ulang", uLang,
                "--loglevel", loglevel,
                "--plugintest", bundle.getAbsolutePath(),}));
            if (doNotLoadPluginsCheck.isSelected()) {
                testArgs.add("--xDisablePluginLoading");
            }

            // add the user's extra arguments
            appendArgField(testArgs, "test-bundle-args", argsField.getText());

            // launch the app
            Subprocess process = isOverriden
                    ? new Subprocess(testArgs)
                    : Subprocess.launch(testArgs);
            process.createStopButton(name);
            process.start();
            dispose();
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-open", bundle.getName()), e);
        }
    }

    /**
     * Adds the contents of an argument field to the arg list, and updates the
     * setting.
     */
    private static void appendArgField(List<String> args, String key, String field) {
        field = field.trim();
        Settings.getUser().set(key, field);
        if (!field.isEmpty()) {
            CommandFormatter cf = new CommandFormatter();
            String[] tokens = cf.formatCommand(field);
            args.addAll(Arrays.asList(tokens));
        }
    }

    @Override
    public void handleCancelAction(ActionEvent evt) {
        dispose();
    }
}