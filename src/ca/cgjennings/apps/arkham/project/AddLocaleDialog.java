package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.prefs.LanguageCodeDescriptor;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.FilteredListModel;
import ca.cgjennings.ui.JIconList;
import ca.cgjennings.ui.JUtilities;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import resources.Language;
import static resources.Language.string;

/**
 * Dialog to create new locales for a set of property files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class AddLocaleDialog extends javax.swing.JDialog implements AgnosticDialog {

    private File parent;
    private String baseName;
    private String targetExtension;

    /**
     * Creates new form AddLocaleDialog
     */
    public AddLocaleDialog(File anyPropertyFile) {
        super(StrangeEons.getWindow(), true);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        targetExtension = ".properties";
        String name = decodeBaseName(anyPropertyFile, targetExtension);
        Locale loc = decodeLocale(anyPropertyFile, targetExtension);
        if (name == null || loc == null) {
            targetExtension = ".html";
            name = decodeBaseName(anyPropertyFile, targetExtension);
            loc = decodeLocale(anyPropertyFile, targetExtension);
        }
        if (name == null || loc == null) {
            throw new AssertionError("not a .properties or .html file");
        }

        parent = anyPropertyFile.getParentFile();
        baseName = name;

        FilteredListModel model = new FilteredListModel();
        for (Locale lang : Language.getLocales()) {
            boolean disable = getFileName(lang).exists();
            model.add(new LanguageCodeDescriptor(lang, disable));
        }
        locList.setModel(model);

        baseFileField.setText(name + targetExtension);
        baseFileField.setFont(new Font("Monospaced", Font.PLAIN, baseFileField.getFont().getSize()));

        localeField.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                localeFieldFocusLost(null);
            }
        });

        setTitle(getTitle() + " (" + name + ")");

        updateOKBtn();
    }

    private static Locale decodeLocale(File propertiesFile) {
        return decodeLocale(propertiesFile, ".properties");
    }

    private static Locale decodeLocale(File propertiesFile, String targetExtension) {
        String locale = propertiesFile.getName();
        if (!locale.endsWith(targetExtension)) {
            return null;
        }

        locale = locale.substring(0, locale.length() - targetExtension.length());
        String[] tokens = locale.split("\\_");

        int len = tokens.length;
        if (len == 1) {
            return new Locale("");
        }
        if (len == 2) {
            return new Locale(tokens[len - 1]);
        }
        if (len == 3) {
            return new Locale(tokens[len - 2], tokens[len - 1]);
        }
        if (len >= 4) {
            return new Locale(tokens[len - 3], tokens[len - 2], tokens[len - 1]);
        }

        return null;
    }

    private static String decodeBaseName(File propertiesFile) {
        return decodeBaseName(propertiesFile, ".properties");
    }

    private static String decodeBaseName(File propertiesFile, String targetExtension) {
        String locale = propertiesFile.getName();
        if (!locale.endsWith(targetExtension)) {
            return null;
        }

        locale = locale.substring(0, locale.length() - targetExtension.length());
        String[] tokens = locale.split("\\_");

        int keep = 0;
        int len = tokens.length;
        if (len <= 1) {
            return locale;
        }
        if (len <= 4) {
            keep = 1;
        } else {
            keep = len - 3;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < keep; ++i) {
            b.append(tokens[i]).append(i < keep - 1 ? "_" : "");
        }
        return b.toString();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        actionGroup = new javax.swing.ButtonGroup();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        copyBtn = new javax.swing.JRadioButton();
        nothingBtn = new javax.swing.JRadioButton();
        baseFileField = new javax.swing.JLabel();
        localeMethodTab = new javax.swing.JTabbedPane();
        localePanel = new javax.swing.JPanel();
        locScroll = new javax.swing.JScrollPane();
        locList = new JIconList();
        jLabel6 = new javax.swing.JLabel();
        localeFilter = new ca.cgjennings.ui.JFilterField();
        customPanel = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        langCombo = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        countryCombo = new javax.swing.JComboBox();
        localeField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        syntaxWarnLabel = new ca.cgjennings.ui.JWarningLabel();
        tip = new ca.cgjennings.ui.JTip();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "pa-at-title" )); // NOI18N

        cancelBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "pa-at-ok" )); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "pa-at-content" ))); // NOI18N

        jLabel5.setText(string( "pa-at-action" )); // NOI18N

        actionGroup.add(copyBtn);
        copyBtn.setText(string( "pa-at-copy-base" )); // NOI18N

        actionGroup.add(nothingBtn);
        nothingBtn.setSelected(true);
        nothingBtn.setText(string( "pa-at-create-empty" )); // NOI18N

        baseFileField.setText("    ");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(copyBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(baseFileField))
                            .addComponent(nothingBtn))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nothingBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(copyBtn)
                    .addComponent(baseFileField))
                .addContainerGap())
        );

        localeMethodTab.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        localeMethodTab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                localeMethodTabStateChanged(evt);
            }
        });

        locScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        locList.setVisibleRowCount(10);
        locList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                locListValueChanged(evt);
            }
        });
        locScroll.setViewportView(locList);

        jLabel6.setText(string( "pa-at-multiples" )); // NOI18N

        localeFilter.setLabel(string("pa-at-filter")); // NOI18N

        javax.swing.GroupLayout localePanelLayout = new javax.swing.GroupLayout(localePanel);
        localePanel.setLayout(localePanelLayout);
        localePanelLayout.setHorizontalGroup(
            localePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(localePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(localePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(locScroll)
                    .addGroup(localePanelLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addComponent(localeFilter, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)))
                .addContainerGap())
        );
        localePanelLayout.setVerticalGroup(
            localePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(localePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(localePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(localeFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(locScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 249, Short.MAX_VALUE)
                .addContainerGap())
        );

        localeFilter.getDocument().addDocumentListener( new DocumentEventAdapter() {
            @Override
            public void changedUpdate( DocumentEvent e ) {
                List<LanguageCodeDescriptor> selValues = locList.getSelectedValuesList();
                final Pattern p = Pattern.compile( "(?i)(?u)" + Pattern.quote( localeFilter.getText() ) );
                ((FilteredListModel) locList.getModel()).setFilter( new FilteredListModel.ListFilter() {
                    @Override
                    public boolean include( FilteredListModel model, Object item ) {
                        if( item == null ) return false;
                        Locale loc = ((LanguageCodeDescriptor) item).getLocale();
                        return p.matcher( loc.getLanguage() ).find()
                        || p.matcher( loc.getCountry() ).find()
                        || p.matcher( loc.getDisplayLanguage() ).find()
                        || p.matcher( loc.getDisplayCountry() ).find()
                        || p.matcher( loc.getDisplayLanguage( loc ) ).find()
                        || p.matcher( loc.getDisplayCountry( loc ) ).find()
                        ;
                    }
                });
                if( selValues.size() > 0 ) {
                    ListModel m = locList.getModel();
                    for( LanguageCodeDescriptor sel : selValues ) {
                        for( int i=0; i<m.getSize(); ++i ) {
                            if( sel.equals( m.getElementAt( i ) ) ) {
                                locList.addSelectionInterval( i, i );
                            }
                        }
                    }
                }
            }
        });

        localeMethodTab.addTab(string("pa-at-standard-langs"), localePanel); // NOI18N

        jLabel7.setText(string( "pa-at-custom" )); // NOI18N

        jLabel8.setLabelFor(langCombo);
        jLabel8.setText(string( "pa-at-language" )); // NOI18N

        langCombo.setRenderer( langRenderer );
        langCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countryComboActionPerformed(evt);
            }
        });

        jLabel9.setLabelFor(countryCombo);
        jLabel9.setText(string( "pa-at-country" )); // NOI18N

        countryCombo.setRenderer( countryRenderer );
        countryCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countryComboActionPerformed(evt);
            }
        });

        localeField.setColumns(5);
        localeField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                localeFieldFocusLost(evt);
            }
        });

        jLabel1.setText(string("locale")); // NOI18N

        jLabel2.setText(string("pa-at-custom-alt")); // NOI18N

        javax.swing.GroupLayout customPanelLayout = new javax.swing.GroupLayout(customPanel);
        customPanel.setLayout(customPanelLayout);
        customPanelLayout.setHorizontalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9)
                    .addComponent(jLabel2)
                    .addGroup(customPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(countryCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 384, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(langCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 384, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(customPanelLayout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(localeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(syntaxWarnLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        customPanelLayout.setVerticalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addGap(18, 18, 18)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(langCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(countryCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(localeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(syntaxWarnLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(90, Short.MAX_VALUE))
        );

        customPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {localeField, syntaxWarnLabel});

        localeMethodTab.addTab(string("pa-at-tab-custom"), customPanel); // NOI18N

        tip.setTipText(string("pa-at-root-tip")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(tip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn)))
                .addContainerGap())
            .addComponent(localeMethodTab)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(localeMethodTab, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(12, 12, 12)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(okBtn)
                        .addComponent(cancelBtn))
                    .addComponent(tip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private boolean loadedCustomCombos;

	private void localeMethodTabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_localeMethodTabStateChanged
            if (!loadedCustomCombos) {
                try {
                    loadedCustomCombos = true;
                    JUtilities.showWaitCursor(this);

                    final Collator cmp = Language.getInterface().getCollator();
                    String[] labels = Locale.getISOLanguages();
                    Arrays.sort(labels, (String o1, String o2) -> {
                        Locale loc1 = new Locale(o1);
                        Locale loc2 = new Locale(o2);
                        return cmp.compare(loc1.getDisplayLanguage(), loc2.getDisplayLanguage());
                    });
                    DefaultComboBoxModel langModel = new DefaultComboBoxModel(labels);

                    labels = Locale.getISOCountries();
                    Arrays.sort(labels, (String o1, String o2) -> {
                        Locale loc1 = new Locale("", o1);
                        Locale loc2 = new Locale("", o2);
                        return cmp.compare(loc1.getDisplayCountry(), loc2.getDisplayCountry());
                    });
                    DefaultComboBoxModel countryModel = new DefaultComboBoxModel();
                    countryModel.addElement("");
                    for (String country : labels) {
                        countryModel.addElement(country);
                    }

                    langCombo.setModel(langModel);
                    countryCombo.setModel(countryModel);
                    langCombo.setSelectedItem(Locale.getDefault().getLanguage());
                    countryCombo.setSelectedItem(Locale.getDefault().getCountry());
                    updateLocaleField(); // init the field from combo boxes
                } finally {
                    JUtilities.hideWaitCursor(this);
                }
            }
            updateOKBtn();
	}//GEN-LAST:event_localeMethodTabStateChanged

	private void locListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_locListValueChanged
            updateLocaleField();
            updateOKBtn();
	}//GEN-LAST:event_locListValueChanged

	private void countryComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countryComboActionPerformed
            updateLocaleField();
            updateOKBtn();
	}//GEN-LAST:event_countryComboActionPerformed

    private void localeFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_localeFieldFocusLost
        if (isUpdatingAfterFocusLost) {
            return;
        }
        isUpdatingAfterFocusLost = true;
        try {
            String text = localeField.getText();
            // this method parses gently, so we can use it to normalize the locale
            Locale loc = Language.parseLocaleDescription(text);
            final String localeText = loc.toString();

            if (evt != null && !localeText.equals(text)) {
                EventQueue.invokeLater(() -> {
                    isUpdatingAfterFocusLost = true;
                    localeField.setText(localeText);
                    isUpdatingAfterFocusLost = false;
                });

            }
            matchElement(langCombo, loc.getLanguage());
            matchElement(countryCombo, loc.getCountry());
        } finally {
            isUpdatingAfterFocusLost = false;
        }
        updateOKBtn();
    }//GEN-LAST:event_localeFieldFocusLost

    private static void matchElement(JComboBox box, String s) {
        DefaultComboBoxModel m = (DefaultComboBoxModel) box.getModel();
        for (int i = 0; i < m.getSize(); ++i) {
            if (m.getElementAt(i).toString().equals(s)) {
                box.setSelectedIndex(i);
                return;
            }
        }
    }

    private void updateLocaleField() {
        if (isUpdatingAfterFocusLost) {
            return;
        }

        Object ll = langCombo.getSelectedItem();
        Object CC = countryCombo.getSelectedItem();
        if (ll == null || CC == null) {
            // nothing in model yet
            localeField.setText("");
        } else {
            Locale loc = new Locale(ll.toString(), CC.toString());
            localeField.setText(loc.toString());
        }
        syntaxWarnLabel.setVisible(false);
    }
    private boolean isUpdatingAfterFocusLost;

    private static DefaultListCellRenderer langRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Locale loc = new Locale(value.toString());
            value = loc.getDisplayLanguage();
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setIcon(Language.getIconForLanguage(loc));
            return this;
        }
    };

    private static DefaultListCellRenderer countryRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Locale loc = new Locale("", value.toString());
            value = loc.getDisplayCountry();
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setIcon(Language.getIconForCountry(loc));
            return this;
        }
    };

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup actionGroup;
    private javax.swing.JLabel baseFileField;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JRadioButton copyBtn;
    private javax.swing.JComboBox countryCombo;
    private javax.swing.JPanel customPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JComboBox langCombo;
    private javax.swing.JList<LanguageCodeDescriptor> locList;
    private javax.swing.JScrollPane locScroll;
    private javax.swing.JTextField localeField;
    private ca.cgjennings.ui.JFilterField localeFilter;
    private javax.swing.JTabbedPane localeMethodTab;
    private javax.swing.JPanel localePanel;
    private javax.swing.JRadioButton nothingBtn;
    private javax.swing.JButton okBtn;
    private ca.cgjennings.ui.JWarningLabel syntaxWarnLabel;
    private ca.cgjennings.ui.JTip tip;
    // End of variables declaration//GEN-END:variables

    private void updateOKBtn() {
        boolean isValid = true;
        Locale[] sel = getSelectedLocales();
        if (sel.length == 0) {
            isValid = false;
        } else {
            for (Locale loc : sel) {
                if (getFileName(loc).exists()) {
                    isValid = false;
                    break;
                }
            }
        }
        PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn).setEnabled(isValid);
        syntaxWarnLabel.setVisible(!isValid);
    }

    private Locale[] getSelectedLocales() {
        Locale[] result;
        if (localeMethodTab.getSelectedIndex() == 0) {
            List<LanguageCodeDescriptor> sel = locList.getSelectedValuesList();
            result = new Locale[sel.size()];
            for(int i=0; i<result.length; ++i) {
                result[i] = sel.get(i).getLocale();
            }
        } else {
            String loc = localeField.getText();
            if (Language.isLocaleDescriptionValid(loc)) {
                result = new Locale[]{Language.parseLocaleDescription(loc)};
            } else {
                return new Locale[0];
            }
        }
        return result;
    }

    @Override
    public void handleOKAction(ActionEvent evt) {
        try {
            for (Locale loc : getSelectedLocales()) {
                create(loc);
            }

            ProjectView v = ProjectView.getCurrentView();
            if (v != null) {
                Member m = v.getProject() != null ? v.getProject().findMember(parent) : null;
                if (m != null) {
                    m.synchronize();
                }
            }

        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-create-new"), e);
        }
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    private File getFileName(Locale loc) {
        String code = loc.toString();
        String name = baseName + (!code.isEmpty() ? "_" : "") + code + targetExtension;
        return new File(parent, name);
    }

    private void create(Locale loc) throws IOException {
        File target = getFileName(loc);

        if (target.exists()) {
            return;
        }

        String text = nothingBtn.isSelected() ? "" : ProjectUtilities.getFileAsString(new File(parent, baseName + targetExtension), ProjectUtilities.ENC_SETTINGS);
        ProjectUtilities.copyReader(new StringReader(text), target, ProjectUtilities.ENC_SETTINGS);

        // ensure base file exists
        if (!loc.getCountry().isEmpty()) {
            create(new Locale(loc.getLanguage()));
        }
    }
}
