package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import resources.Language;
import static resources.Language.string;

/**
 * Prompts the user to select a locale. To use, call {@link #showDialog()}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class LocaleSelectionDialog extends javax.swing.JDialog implements AgnosticDialog {

    public LocaleSelectionDialog(JDialog parent) {
        super(parent, true);
        constructorImpl();
    }

    public LocaleSelectionDialog(JFrame parent) {
        super(parent, true);
        constructorImpl();
    }

    private void constructorImpl() {
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, closeBtn);

        Set<String> langs = new TreeSet<>();
        Set<String> countries = new TreeSet<>();
        for (Locale loc : Locale.getAvailableLocales()) {
            if (!loc.getCountry().isEmpty()) {
                countries.add(loc.getCountry());
            }
            if (!loc.getLanguage().isEmpty()) {
                langs.add(loc.getLanguage());
            }
        }

        DefaultComboBoxModel<Locale> lmodel = new DefaultComboBoxModel<>();
        for (String lang : langs) {
            lmodel.addElement(new Locale(lang));
        }

        DefaultComboBoxModel<Object> cmodel = new DefaultComboBoxModel<>();
        cmodel.addElement(" ");
        for (String count : countries) {
            cmodel.addElement(new Locale("", count));
        }
        langCombo.setModel(lmodel);
        countryCombo.setModel(cmodel);
        langCombo.setRenderer(locRenderer);
        countryCombo.setRenderer(locRenderer);
    }

    private void loadCountries(Locale language) {
        if (language == null) {
            return;
        }
        Object oldCountry = countryCombo.getSelectedItem();
        Set<String> countries = new TreeSet<>();
        for (Locale loc : Locale.getAvailableLocales()) {
            if (loc.getLanguage().equals(language.getLanguage())) {
                if (!loc.getCountry().isEmpty()) {
                    countries.add(loc.getCountry());
                }
            }
        }
        DefaultComboBoxModel<Object> countModel = new DefaultComboBoxModel<>();
        countModel.addElement(" ");
        for (String c : countries) {
            countModel.addElement(new Locale("", c));
        }

        countryCombo.setModel(countModel);
        if (oldCountry != null) {
            for (int i = 0; i < countModel.getSize(); ++i) {
                if (oldCountry.equals(countModel.getElementAt(i))) {
                    countryCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (countModel.getSize() == 2) {
            countryCombo.setSelectedIndex(1);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        langCombo = new javax.swing.JComboBox<>();
        countryCombo = new javax.swing.JComboBox<>();
        locField = new javax.swing.JTextField();
        closeBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setLabelFor(langCombo);
        jLabel1.setText(string( "dt-l-language" )); // NOI18N

        jLabel2.setLabelFor(countryCombo);
        jLabel2.setText(string( "dt-l-country" )); // NOI18N

        langCombo.setMaximumRowCount(12);
        langCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                langComboActionPerformed(evt);
            }
        });

        countryCombo.setMaximumRowCount(12);
        countryCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                countryComboActionPerformed(evt);
            }
        });

        locField.setColumns(8);

        closeBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "dt-b-add-locale" )); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(locField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(countryCombo, 0, 248, Short.MAX_VALUE)
                            .addComponent(langCombo, 0, 248, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(closeBtn)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {closeBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(langCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(countryCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(locField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(closeBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void langComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_langComboActionPerformed
            if (langCombo.getSelectedIndex() < 0) {
                return;
            }
            updateLocale();
            loadCountries((Locale) langCombo.getSelectedItem());
	}//GEN-LAST:event_langComboActionPerformed

	private void countryComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_countryComboActionPerformed
            if (countryCombo.getSelectedIndex() < 0) {
                return;
            }
            updateLocale();
	}//GEN-LAST:event_countryComboActionPerformed

    private void updateLocale() {
        String loc = ((Locale) langCombo.getSelectedItem()).getLanguage();
        if (countryCombo.getSelectedIndex() > 0) {
            loc += "_" + ((Locale) countryCombo.getSelectedItem()).getCountry();
        }
        if (loc.endsWith("_")) {
            loc = loc.substring(0, loc.length() - 1);
        }
        locField.setText(loc);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeBtn;
    private javax.swing.JComboBox<Object> countryCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JComboBox<Locale> langCombo;
    private javax.swing.JTextField locField;
    private javax.swing.JButton okBtn;
    // End of variables declaration//GEN-END:variables

    private Locale sel = null;

    /**
     * Displays the dialog and waits for the user to choose a locale.
     *
     * @return the selected locale, or {@code null} is the user cancels
     */
    public Locale showDialog() {
        sel = null;
        setVisible(true);
        return sel;
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    @Override
    public void handleOKAction(ActionEvent e) {
        String locale = locField.getText();

        String lang = "";
        String count = "";

        int div = locale.indexOf('_');
        if (div >= 0) {
            lang = locale.substring(0, div);
            int div2 = locale.indexOf('_', div + 1);
            if (div2 < 0) {
                div2 = locale.length();
            }
            count = locale.substring(div + 1, div2);
        } else {
            lang = locale.trim();
        }
        sel = new Locale(lang, count);
        dispose();
    }

    public Locale getSelectedLocale() {
        return sel;
    }

    private DefaultListCellRenderer locRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof String) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
            Locale loc = (Locale) value;
            String s;
            Icon i;
            if (loc.getCountry().isEmpty()) {
                s = loc.getLanguage() + " (" + loc.getDisplayLanguage() + ")";
                i = Language.getIconForLanguage(loc);
            } else {
                s = loc.getCountry() + " (" + loc.getDisplayCountry() + ")";
                i = Language.getIconForCountry(loc);
            }
            super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
            return this;
        }
    };
}
