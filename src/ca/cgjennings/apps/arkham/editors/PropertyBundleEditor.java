package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.apps.arkham.AbstractSupportEditor;
import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.i18n.IntegerPluralizer;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.spelling.MultilanguageSupport;
import ca.cgjennings.spelling.SpellingChecker;
import ca.cgjennings.spelling.ui.JSpellingTextArea;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.theme.Palette;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * An editor for string resource bundles (<tt>.properties</tt> files).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class PropertyBundleEditor extends AbstractSupportEditor {

    private Locale locale;
    private Properties base;
    private Properties language;
    private Properties country;

    /**
     * If true, the locale has both language and country, else just language.
     */
    private boolean hasCountry;

    private Key curKey;
    private boolean changingKeys;

    private DefaultListModel<Key> keyModel = new DefaultListModel<>();
    private HashMap<String, Key> keyMap = new HashMap<>();

    public synchronized static SpellingChecker loadDictionaryWithFeedback(Locale loc) {
        if (loc == null) {
            loc = Locale.getDefault();
        }

        // before loading a new checker, check to see if the default language
        // is compatible
        SpellingChecker shared = SpellingChecker.getSharedInstance();
        Locale sharedLocale = shared.getLocale();
        if (sharedLocale.equals(loc) || sharedLocale.equals(MultilanguageSupport.getSupportedLocale(loc))) {
            return shared;
        }

        SpellingChecker sc = MultilanguageSupport.getCheckerIfLoaded(loc);
        if (sc == null) {
            DictLoader loader = new DictLoader(loc);
            String title = string("multilang-loading", loc.getDisplayLanguage());
            new BusyDialog(title, loader);
            sc = loader.sc;
        }
        if (sc == null) {
            sc = MultilanguageSupport.getDummyChecker();
        }
        return sc;
    }

    private static class DictLoader implements Runnable {

        private SpellingChecker sc;
        private final Locale loc;

        public DictLoader(Locale loc) {
            this.loc = loc;
        }

        @Override
        public void run() {
            try {
                sc = MultilanguageSupport.getChecker(loc);
            } catch (IOException e) {
                StrangeEons.log.log(Level.SEVERE, "unable to load spelling dictionary", e);
            }
        }
    }

    public PropertyBundleEditor(File propFile) throws IOException {
        if (propFile == null) {
            throw new NullPointerException("propFile");
        }
        initComponents();
        JUtilities.setIconPair(copyDefaultBtn, "ui/button/down.png", "ui/button/down-hi.png", true);
        JUtilities.setIconPair(copyLanguageBtn, "ui/button/down.png", "ui/button/down-hi.png", true);

        Language.LocalizedFileName lfn = new Language.LocalizedFileName(propFile);
        locale = lfn.getLocale();
        if (locale == null) {
            throw new IllegalArgumentException("not a valid property bundle file name: " + propFile.getName());
            //loc = new Locale( "", "", "" );
        }

        setFile(propFile);
        setTitle(lfn.baseName + " [" + locale.getLanguage() + (locale.getCountry().isEmpty() ? "" : ('_' + locale.getCountry())) + ']');
        setFrameIcon(Language.getIconForLocale(locale));

        hasCountry = !locale.getCountry().isEmpty();

        base = readPropertyFile(propFile, 0);
        language = readPropertyFile(propFile, 1);

        langLabel.setText(locale.getLanguage());

        if (hasCountry) {
            country = readPropertyFile(propFile, 2);
            regionLabel.setText(locale.getLanguage() + '_' + locale.getCountry());
            languageField.setEditable(false);
            countryField.getDocument().addDocumentListener(editListener);
            MultilanguageSupport.changeChecker(countryField, loadDictionaryWithFeedback(locale));
        } else {
            country = new Properties();
            regionLabel.setText("");
            countryField.setEnabled(false);
            countryPlainLabel.setEnabled(false);
            translatedInParentLabel.setVisible(false);
            translatedInParentTextLabel.setVisible(false);
            languageField.getDocument().addDocumentListener(editListener);
            copyLanguageBtn.setVisible(false);
            MultilanguageSupport.changeChecker(languageField, loadDictionaryWithFeedback(locale));
        }

        IntegerPluralizer ip = IntegerPluralizer.create(locale);
        StringBuilder pluralHelp = new StringBuilder("<html>");
        if (ip.isFallbackPluralizer()) {
            pluralHelp.append(string("dt-l-plural-no-pluralizer"));
        } else {
            pluralHelp.append("<b>").append(string("dt-l-plural-title", locale.getDisplayLanguage())).append("</b><br><table border=0>");
            for (String row : ip.getPluralFormDescription().split("\n")) {
                int colon = row.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String rule = row.substring(0, colon).trim();
                String key = row.substring(colon + 1).trim().replace("key", "<i>key</i>");
                pluralHelp.append("<tr><td>").append(rule).append("</td><td>&nbsp;&nbsp;").append(key).append("</td></tr>");
            }
            pluralHelp.append("</table>");
        }
        pluralTip.setTipText(pluralHelp.toString());

        buildCommentTable(propFile);

        // re-read the base file to get the keys in key order
        EscapedLineReader elr = new EscapedLineReader(createFileName(propFile, 0));
        try {
            String[] entry;
            while ((entry = elr.readProperty()) != null) {
                String key = entry[0];
                Key k = new Key(key);
                keyModel.addElement(k);
                keyMap.put(key, k);
            }
        } finally {
            try {
                elr.close();
            } catch (IOException ie) {
                StrangeEons.log.log(Level.WARNING, null, ie);
            }
        }

        insertMissingKeys(language);
        insertMissingKeys(country);

        keyList.setModel(keyModel);

        // select something if possible, but make sure that
        // editing is disabled if there is no selection
        if (keyModel.size() > 0) {
            keyList.setSelectedIndex(0);
        } else {
            keyListValueChanged(null);
        }
    }

    private void buildCommentTable(File propFile) throws IOException {
        int lastCommentStart = 0;
        int state = 0; // 0 = NOT IN COMMENT, 1 = IN COMMENT, 2 = LAST LINE WAS KEY WITH CONTINUATION
        StringBuilder b = new StringBuilder(16_384);
        try (InputStream in = new FileInputStream(createFileName(propFile, 0))) {
            BufferedReader r = new BufferedReader(new InputStreamReader(in), 32 * 1_024);
            String line;
            while ((line = r.readLine()) != null) {
                if (b.length() > 0) {
                    b.append('\n');
                }
                int lineOffset = b.length();
                b.append(line);
                line = line.trim();
                boolean isComment = !line.isEmpty() && (line.charAt(0) == '#' || line.charAt(0) == '!');

                switch (state) {
                    case 0:
                        if (isComment) {
                            state = 1;
                            lastCommentStart = lineOffset;
                        } else if (!line.isEmpty()) {
                            String key = line.substring(0, keySplit(line)).trim();
                            if (key.charAt(key.length() - 1) == '\\') {
                                state = 2;
                            }
                            keyToCommentLine.put(key, lastCommentStart);
                        }
                        break;
                    case 1:
                        if (!isComment && !line.isEmpty()) {
                            String key = line.substring(0, keySplit(line)).trim();
                            if (key.charAt(key.length() - 1) == '\\') {
                                state = 2;
                            }
                            keyToCommentLine.put(key, lastCommentStart);
                            state = 0;
                        }
                        break;
                    case 2:
                        if (!line.endsWith("\\")) {
                            state = 0;
                        }
                        break;
                    default:
                        throw new AssertionError();
                }
            }
        }

        commentBlockField.setText(b.toString());
        commentBlockField.select(0, 0);
    }
    private HashMap<String, Integer> keyToCommentLine = new HashMap<>(2_000);

    private static int keySplit(String s) {
        boolean escape = false;
        int len = s.length();
        for (int i = 0; i < len; ++i) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == ':' || c == '=') {
                return i;
            }
            if (c == '\\') {
                escape = true;
            }
        }
        return len;
    }

    private void insertMissingKeys(Properties source) {
        for (String key : source.stringPropertyNames()) {
            if (!keyMap.containsKey(key)) {
                insertKey(key);
            }
        }
    }

    private int insertKey(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("empty key");
        }
        if (keyMap.containsKey(key)) {
            throw new IllegalArgumentException("key exists");
        }
        Key k = new Key(key);

        // find the first key that is a prefix of this key name, and insert this
        // key under it; if no such key, add to bottom
        int pos = keyModel.getSize();
        String prefix = key;
        out:
        for (;;) {
            prefix = key.substring(0, prefix.length() - 1);
            if (prefix.isEmpty()) {
                break;
            }
            if (keyMap.containsKey(prefix)) {
                for (int i = 0; i < pos; ++i) { // pos happens to be the list size
                    if (prefix.equals(((Key) keyModel.get(i)).key)) {
                        pos = i + 1;
                        break out;
                    }
                }
            }
        }
        keyMap.put(key, k);
        keyModel.add(pos, k);
        return pos;
    }

    // level = 0: base file / 1: lang / 2: lang + country
    private File createFileName(File propFile, int level) {
        Language.LocalizedFileName lfn = new Language.LocalizedFileName(propFile);
        String baseName = lfn.baseName;
        if (level > 0) {
            if (locale.getLanguage().isEmpty()) {
                throw new AssertionError();
            }
            baseName += '_' + locale.getLanguage();
        }
        if (level > 1) {
            if (locale.getCountry().isEmpty()) {
                throw new AssertionError();
            }
            baseName += '_' + locale.getCountry();
        }
        baseName += '.' + lfn.extension;
        return new File(propFile.getParent(), baseName);
    }

    private Properties readPropertyFile(File propFile, int level) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(createFileName(propFile, level))) {
            p.load(in);
        }
        return p;
    }

    @Override
    protected void clearImpl() {
        for (String keyName : keyMap.keySet()) {
            Key k = keyMap.get(keyName);
            if (hasCountry) {
                k.setCtry(null);
            } else {
                k.setLang(null);
            }
        }
    }

    @Override
    protected void saveImpl(File f) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f)) {
            Properties p = getProperties();
            p.store(out, f.getName());
        }
    }

    /**
     * Returns the current state of the document as a set of properties.
     *
     * @return the document state, including all keys defined in the edited
     * locale but not any parent keys that are not redefined
     */
    private Properties getProperties() {
        Properties p = new Properties();
        for (String keyName : keyMap.keySet()) {
            Key key = keyMap.get(keyName);
            String value = null;
            if (hasCountry) {
                if (key.isDefinedInCountry()) {
                    value = key.getCtry();
                }
            } else {
                if (key.isDefinedInLanguage()) {
                    value = key.getLang();
                }
            }
            if (value != null) {
                p.setProperty(keyName, value);
            }
        }
//		System.err.println( p.toString() );
        return p;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        northPanel = new javax.swing.JPanel();
        overlayPanel = new ca.cgjennings.apps.arkham.dialog.OverlayPanel();
        jLabel2 = new javax.swing.JLabel();
        translatedInParentLabel = new javax.swing.JLabel();
        notInDefaultLabel = new javax.swing.JLabel();
        translatedLabel = new javax.swing.JLabel();
        untranslatedLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        translatedInParentTextLabel = new javax.swing.JLabel();
        filterField = new ca.cgjennings.ui.JFilterField();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        keyList = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        addKeyBtn = new javax.swing.JButton();
        deleteKeyBtn = new javax.swing.JButton();
        btnSpacer = new javax.swing.JLabel();
        diffBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        defaultField = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        languageField =  new JSpellingTextArea() ;
        countryScroll = new javax.swing.JScrollPane();
        countryField =  new JSpellingTextArea() ;
        jLabel12 = new javax.swing.JLabel();
        countryPlainLabel = new javax.swing.JLabel();
        langLabel = new javax.swing.JLabel();
        regionLabel = new javax.swing.JLabel();
        commentBlockScroll = new javax.swing.JScrollPane();
        commentBlockField = new javax.swing.JTextArea();
        commentBlockLabel = new javax.swing.JLabel();
        copyDefaultBtn = new javax.swing.JButton();
        copyLanguageBtn = new javax.swing.JButton();
        nextUntransKeyBtn = new javax.swing.JButton();
        nextKeyBtn = new javax.swing.JButton();
        pluralTip = new ca.cgjennings.ui.JTip();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        northPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.darkGray));

        ca.cgjennings.ui.ArcBorder arcBorder1 = new ca.cgjennings.ui.ArcBorder();
        overlayPanel.setBorder(arcBorder1);
        overlayPanel.setLayout(new java.awt.GridBagLayout());

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-1f));
        jLabel2.setText(string( "dt-l-leg-new-key" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 22);
        overlayPanel.add(jLabel2, gridBagConstraints);

        translatedInParentLabel.setBackground(Palette.get.foreground.opaque.blue);
        translatedInParentLabel.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.darkGray));
        translatedInParentLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 12;
        gridBagConstraints.ipady = 12;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 4);
        overlayPanel.add(translatedInParentLabel, gridBagConstraints);

        notInDefaultLabel.setBackground(Palette.get.foreground.opaque.orange);
        notInDefaultLabel.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.darkGray));
        notInDefaultLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 12;
        gridBagConstraints.ipady = 12;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 4);
        overlayPanel.add(notInDefaultLabel, gridBagConstraints);

        translatedLabel.setBackground(Palette.get.foreground.opaque.text);
        translatedLabel.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.darkGray));
        translatedLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 12;
        gridBagConstraints.ipady = 12;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 4);
        overlayPanel.add(translatedLabel, gridBagConstraints);

        untranslatedLabel.setBackground(Palette.get.foreground.opaque.grey);
        untranslatedLabel.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.darkGray));
        untranslatedLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 12;
        gridBagConstraints.ipady = 12;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 4);
        overlayPanel.add(untranslatedLabel, gridBagConstraints);

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getSize()-1f));
        jLabel7.setText(string( "dt-l-leg-translated" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 16);
        overlayPanel.add(jLabel7, gridBagConstraints);

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel8.setText(string( "dt-l-legend" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(12, 16, 16, 18);
        overlayPanel.add(jLabel8, gridBagConstraints);

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getSize()-1f));
        jLabel5.setText(string( "dt-l-leg-untranslated" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 16);
        overlayPanel.add(jLabel5, gridBagConstraints);

        translatedInParentTextLabel.setFont(translatedInParentTextLabel.getFont().deriveFont(translatedInParentTextLabel.getFont().getSize()-1f));
        translatedInParentTextLabel.setText(string( "dt-l-leg-translated-parent" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 16, 16);
        overlayPanel.add(translatedInParentTextLabel, gridBagConstraints);

        filterField.setColumns(20);
        filterField.setLabel(string("search")); // NOI18N
        filterField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout northPanelLayout = new javax.swing.GroupLayout(northPanel);
        northPanel.setLayout(northPanelLayout);
        northPanelLayout.setHorizontalGroup(
            northPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(northPanelLayout.createSequentialGroup()
                .addComponent(overlayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(179, 179, 179)
                .addComponent(filterField, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                .addContainerGap())
        );
        northPanelLayout.setVerticalGroup(
            northPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(northPanelLayout.createSequentialGroup()
                .addGroup(northPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(filterField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(overlayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(northPanel, gridBagConstraints);

        jSplitPane1.setDividerLocation(200);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jLabel14.setBackground(UIManager.getColor(Theme.PROJECT_HEADER_BACKGROUND));
        jLabel14.setFont(jLabel14.getFont().deriveFont(jLabel14.getFont().getStyle() | java.awt.Font.BOLD, jLabel14.getFont().getSize()-1));
        jLabel14.setForeground(UIManager.getColor(Theme.PROJECT_HEADER_FOREGROUND)
        );
        jLabel14.setText(string( "dt-l-keys" )); // NOI18N
        jLabel14.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
        jLabel14.setOpaque(true);
        jPanel1.add(jLabel14, java.awt.BorderLayout.PAGE_START);

        jScrollPane5.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        keyList.setFont(keyList.getFont().deriveFont(keyList.getFont().getSize()-1f));
        keyList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        keyList.setCellRenderer( keyRenderer );
        keyList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                keyListValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(keyList);

        jPanel1.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        addKeyBtn.setIcon( ResourceKit.getIcon( "ui/button/plus.png" ) );
        addKeyBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        addKeyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addKeyBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 2, 8);
        jPanel3.add(addKeyBtn, gridBagConstraints);

        deleteKeyBtn.setIcon( ResourceKit.getIcon( "ui/button/minus.png" ) );
        deleteKeyBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        deleteKeyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteKeyBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 8);
        jPanel3.add(deleteKeyBtn, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.ipady = 4;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(btnSpacer, gridBagConstraints);

        diffBtn.setText(string("dt-l-diff")); // NOI18N
        diffBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                diffBtnActionPerformed(evt);
            }
        });
        jPanel3.add(diffBtn, new java.awt.GridBagConstraints());

        jPanel1.add(jPanel3, java.awt.BorderLayout.PAGE_END);

        jSplitPane1.setLeftComponent(jPanel1);

        jLabel11.setText(string( "dt-l-text-default" )); // NOI18N

        defaultField.setColumns(20);
        defaultField.setEditable(false);
        defaultField.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        defaultField.setLineWrap(true);
        defaultField.setTabSize(4);
        defaultField.setWrapStyleWord(true);
        jScrollPane1.setViewportView(defaultField);

        languageField.setColumns(20);
        languageField.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        languageField.setLineWrap(true);
        languageField.setTabSize(4);
        languageField.setWrapStyleWord(true);
        jScrollPane2.setViewportView(languageField);

        countryField.setColumns(20);
        countryField.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        countryField.setLineWrap(true);
        countryField.setTabSize(4);
        countryField.setWrapStyleWord(true);
        countryScroll.setViewportView(countryField);

        jLabel12.setText(string( "dt-l-text-language" )); // NOI18N

        countryPlainLabel.setText(string( "dt-l-text-region" )); // NOI18N

        langLabel.setText("xx");

        regionLabel.setText("xx_CC");

        commentBlockField.setColumns(20);
        commentBlockField.setEditable(false);
        commentBlockField.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        commentBlockField.setForeground(java.awt.Color.gray);
        commentBlockField.setTabSize(4);
        commentBlockField.setWrapStyleWord(true);
        commentBlockScroll.setViewportView(commentBlockField);

        commentBlockLabel.setText(string( "dt-l-notes" )); // NOI18N

        copyDefaultBtn.setMnemonic('d');
        copyDefaultBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 8, 0));
        copyDefaultBtn.setContentAreaFilled(false);
        copyDefaultBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        copyDefaultBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        copyDefaultBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyDefaultBtnActionPerformed(evt);
            }
        });

        copyLanguageBtn.setMnemonic('l');
        copyLanguageBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 8, 0));
        copyLanguageBtn.setBorderPainted(false);
        copyLanguageBtn.setContentAreaFilled(false);
        copyLanguageBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        copyLanguageBtn.setMargin(new java.awt.Insets(1, 1, 1, 1));
        copyLanguageBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyLanguageBtnActionPerformed(evt);
            }
        });

        nextUntransKeyBtn.setText(string("dt-l-next-untrans-key")); // NOI18N
        nextUntransKeyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextUntransKeyBtnActionPerformed(evt);
            }
        });

        nextKeyBtn.setText(string("dt-l-next-key")); // NOI18N
        nextKeyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextKeyBtnActionPerformed(evt);
            }
        });

        pluralTip.setText(string("dt-l-plural-tip")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(pluralTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 325, Short.MAX_VALUE)
                        .addComponent(nextKeyBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nextUntransKeyBtn))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(copyDefaultBtn)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jLabel11)
                                .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(countryPlainLabel)
                                .addComponent(langLabel)
                                .addComponent(regionLabel)
                                .addComponent(copyLanguageBtn, javax.swing.GroupLayout.Alignment.TRAILING)))
                        .addGap(6, 6, 6)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(commentBlockScroll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
                            .addComponent(countryScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
                            .addComponent(commentBlockLabel))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 58, Short.MAX_VALUE)
                        .addComponent(copyDefaultBtn))
                    .addComponent(jScrollPane1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addGap(1, 1, 1)
                        .addComponent(langLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 67, Short.MAX_VALUE)
                        .addComponent(copyLanguageBtn)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(countryPlainLabel)
                        .addGap(1, 1, 1)
                        .addComponent(regionLabel))
                    .addComponent(countryScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(commentBlockLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(commentBlockScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nextUntransKeyBtn)
                    .addComponent(nextKeyBtn)
                    .addComponent(pluralTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jSplitPane1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private DocumentEventAdapter editListener = new DocumentEventAdapter() {
        @Override
        public void changedUpdate(DocumentEvent e) {
            if (changingKeys) {
                return;
            }

            if (hasCountry) {
                curKey.setCtry(countryField.getText());
            } else {
                curKey.setLang(languageField.getText());
            }
            // in case status changes as a result
            final int i = keyList.getSelectedIndex();
            if (i >= 0) {
                keyList.repaint(keyList.getCellBounds(i, i));
            }
            setUnsavedChanges(true);
        }
    };

	private void keyListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_keyListValueChanged
            changingKeys = true;
            curKey = (Key) keyList.getSelectedValue();

            if (curKey == null) {
                defaultField.setText("");
                languageField.setText("");
                countryField.setText("");
                defaultField.setEnabled(false);
                languageField.setEnabled(false);
                countryField.setEnabled(false);
                deleteKeyBtn.setEnabled(false);
            } else {
                defaultField.setText(curKey.base);
                languageField.setText(curKey.lang);
                countryField.setText(curKey.ctry);
                defaultField.setEnabled(true);
                languageField.setEnabled(true);
                countryField.setEnabled(hasCountry);
                deleteKeyBtn.setEnabled(!curKey.isDefinedInBase() || (hasCountry ? curKey.ctry != null : curKey.lang != null));
                if (hasCountry) {
                    countryField.requestFocusInWindow();
                } else {
                    languageField.requestFocusInWindow();
                }

                Integer commentOffset = keyToCommentLine.get(curKey.key);
                if (commentOffset == null) {
                    StrangeEons.log.log(Level.WARNING, "no comment line entry for {0}", curKey.key);
                } else {
                    try {
                        Rectangle2D pos = commentBlockField.modelToView2D(commentOffset);
                        if (pos != null) {
                            commentBlockScroll.getViewport().setViewPosition(new Point(0, (int) pos.getY()));
                        }
                    } catch (Exception e) {
                        StrangeEons.log.log(Level.WARNING, null, e);
                    }
                }
            }

            changingKeys = false;
	}//GEN-LAST:event_keyListValueChanged

	private void copyDefaultBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_copyDefaultBtnActionPerformed
            JTextArea dest = countryField.isEnabled() ? countryField : languageField;
            dest.setText(defaultField.getText());
	}//GEN-LAST:event_copyDefaultBtnActionPerformed

	private void copyLanguageBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_copyLanguageBtnActionPerformed
            String text = languageField.getText();
            if (text.isEmpty()) {
                text = defaultField.getText();
            }
            countryField.setText(text);
	}//GEN-LAST:event_copyLanguageBtnActionPerformed

	private void deleteKeyBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_deleteKeyBtnActionPerformed
            Key k = (Key) keyList.getSelectedValue();

            if (k == null) {
                UIManager.getLookAndFeel().provideErrorFeedback(keyList);
                return;
            }

            final int choice = JOptionPane.showConfirmDialog(this, string("dt-l-verify-key-delete", k.key), getTitle(), JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            int type = k.getType();
            if (type == NOT_IN_ROOT) {
                // has translation in language, can't delete key
                if (hasCountry && k.isDefinedInLanguage()) {
                    k.setCtry(null);
                } else {
                    int oldIndex = keyList.getSelectedIndex();
                    keyModel.removeElement(k);
                    keyMap.remove(k.key);
                    if (oldIndex >= keyModel.getSize()) {
                        oldIndex = keyModel.getSize() - 1;
                    }
                    keyList.setSelectedIndex(oldIndex);
                }
            } else {
                if (hasCountry) {
                    k.setCtry(null);
                } else {
                    k.setLang(null);
                }
            }
            setUnsavedChanges(true);
	}//GEN-LAST:event_deleteKeyBtnActionPerformed

	private void addKeyBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_addKeyBtnActionPerformed
            String name = JOptionPane.showInputDialog(this, string("dt-l-add-key"), getTitle(), JOptionPane.PLAIN_MESSAGE);
            if (name == null) {
                return;
            }
            if (keyMap.containsKey(name)) {
                keyList.setSelectedValue(keyMap.get(name), true);
            } else {
                int pos = insertKey(name);
                keyList.setSelectedIndex(pos);
                keyList.ensureIndexIsVisible(pos);
                setUnsavedChanges(true);
            }
	}//GEN-LAST:event_addKeyBtnActionPerformed

	private void nextUntransKeyBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_nextUntransKeyBtnActionPerformed
            nextKey(1);
	}//GEN-LAST:event_nextUntransKeyBtnActionPerformed

	private void nextKeyBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_nextKeyBtnActionPerformed
            nextKey(0);
	}//GEN-LAST:event_nextKeyBtnActionPerformed

	private void diffBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_diffBtnActionPerformed
            if (evt != null && (evt.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
                showDiffFrom(null);
                return;
            }

            File f = ResourceKit.showGenericOpenDialog(this, getFile(), getFileTypeDescription(), getFileNameExtension());
            if (f == null) {
                return;
            }

            FileInputStream in = null;
            Properties p = new Properties();
            try {
                in = new FileInputStream(f);
                p.load(in);
            } catch (IOException e) {
                getToolkit().beep();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }

            showDiffFrom(p);
	}//GEN-LAST:event_diffBtnActionPerformed

    private void filterFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterFieldActionPerformed
        String search = filterField.getText();
        if (search.isEmpty()) {
            lastSearch = null;
            return;
        }

        Pattern searchText = Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE);

        int keys = keyModel.getSize();

        // if nothing selected, start at key 0; else start with next key
        int startIndex = keyList.getSelectedIndex() + 1;
        if (startIndex == keys) {
            startIndex = 0;
        }

        boolean found = false;
        int i;
        for (i = startIndex; i < keys && !found; ++i) {
            found = matchKey(i, searchText);
        }
        // not found yet, wrap aorund to top of list
        if (!found && startIndex > 0) {
            for (i = 0; i <= startIndex && !found; ++i) {
                found = matchKey(i, searchText);
            }
        }
        if (found) {
            // match, change the selection
            keyList.setSelectedIndex(--i); // (one too many ++i's)
            // setting the key will change focus to the edit field
            filterField.requestFocus();
        } else {
            // no match, do an error beep
            UIManager.getLookAndFeel().provideErrorFeedback(filterField);
        }

    }//GEN-LAST:event_filterFieldActionPerformed
    private String lastSearch;

    private boolean matchKey(int index, Pattern searchText) {
        Key key = (Key) keyModel.get(index);
        if (searchText.matcher(key.key).find()) {
            return true;
        }
        if (key.base != null && searchText.matcher(key.base).find()) {
            return true;
        }
        if (key.lang != null && searchText.matcher(key.lang).find()) {
            return true;
        }
        return key.ctry != null && searchText.matcher(key.ctry).find();
    }

    @Override
    public String getFileNameExtension() {
        return "properties";
    }

    @Override
    public String getFileTypeDescription() {
        return string("pa-new-properties");
    }

    private void showDiffFrom(Properties p) {
        for (String key : keyMap.keySet()) {
            Key k = keyMap.get(key);
            k.diff = false;
            if (p != null) {
                String delta = p.getProperty(key);
                if (delta != null && !delta.equals(k.base)) {
                    k.diff = true;
                }
            }
        }
        keyList.repaint();
    }

    private void nextKey(int cond) {
        if (keyModel.getSize() > 1) {
            int curKey = keyList.getSelectedIndex();
            int key = curKey;
            do {
                ++key;
                if (key >= keyModel.getSize()) {
                    key = 0;
                }
                boolean done = false;
                if (cond == 0) {
                    done = true;
                }
                if (cond == 1) {
                    Key k = (Key) keyModel.get(key);
                    if ((hasCountry && !k.isDefinedInCountry()) || !k.isDefinedInLanguage()) {
                        done = true;
                    }
                }
                // ...
                if (done) {
                    keyList.setSelectedIndex(key);
                    return;
                }
            } while (key != curKey);
        }
        UIManager.getLookAndFeel().provideErrorFeedback(keyList);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addKeyBtn;
    private javax.swing.JLabel btnSpacer;
    private javax.swing.JTextArea commentBlockField;
    private javax.swing.JLabel commentBlockLabel;
    private javax.swing.JScrollPane commentBlockScroll;
    private javax.swing.JButton copyDefaultBtn;
    private javax.swing.JButton copyLanguageBtn;
    private javax.swing.JTextArea countryField;
    private javax.swing.JLabel countryPlainLabel;
    private javax.swing.JScrollPane countryScroll;
    private javax.swing.JTextArea defaultField;
    private javax.swing.JButton deleteKeyBtn;
    private javax.swing.JButton diffBtn;
    private ca.cgjennings.ui.JFilterField filterField;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JList<Key> keyList;
    private javax.swing.JLabel langLabel;
    private javax.swing.JTextArea languageField;
    private javax.swing.JButton nextKeyBtn;
    private javax.swing.JButton nextUntransKeyBtn;
    private javax.swing.JPanel northPanel;
    private javax.swing.JLabel notInDefaultLabel;
    private ca.cgjennings.apps.arkham.dialog.OverlayPanel overlayPanel;
    private ca.cgjennings.ui.JTip pluralTip;
    private javax.swing.JLabel regionLabel;
    private javax.swing.JLabel translatedInParentLabel;
    private javax.swing.JLabel translatedInParentTextLabel;
    private javax.swing.JLabel translatedLabel;
    private javax.swing.JLabel untranslatedLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public boolean canPerformCommand(AbstractCommand command) {
        return command == Commands.CLEAR
                || command == Commands.FIND
                || command == Commands.SAVE
                || command == Commands.SAVE_AS;
    }

    @Override
    public void performCommand(AbstractCommand command) {
        if (command == Commands.FIND) {
            filterField.selectAll();
            filterField.requestFocusInWindow();
        } else {
            super.performCommand(command);
        }
    }

    private class Key {

        String key;
        String base;
        String lang;
        String ctry;
        private int type;
        boolean diff;

        public Key(String key) {
            if (key == null) {
                throw new NullPointerException("key");
            }
            this.key = key;
            setBase(PropertyBundleEditor.this.base.getProperty(key));
            setLang(language.getProperty(key));
            setCtry(country.getProperty(key));
        }

        public String getBase() {
            if (base == null) {
                return "";
            }
            return base;
        }

        public void setBase(String base) {
            this.base = base;
            updateType();
        }

        public String getCtry() {
            if (ctry == null) {
                return getLang();
            }
            return ctry;
        }

        public void setCtry(String ctry) {
            this.ctry = ctry;
            updateType();
        }

        public String getLang() {
            if (lang == null) {
                return getBase();
            }
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
            updateType();
        }

        public boolean isDefinedInBase() {
            return base != null;
        }

        public boolean isDefinedInLanguage() {
            return lang != null;
        }

        public boolean isDefinedInCountry() {
            return ctry != null;
        }

        private void updateType() {
            if (base == null) {
                type = NOT_IN_ROOT;
                return;
            }

            String base = this.base;
            String lang = this.lang;
            String ctry = this.ctry;

            // if strings are the same, they are not really considered
            // to be translated; we set our local copy to null to simplify
            // later processing
            if (lang != null && lang.equals(base)) {
                lang = null;
            }
            if (hasCountry && ctry != null) {
                if (lang != null) {
                    if (ctry.equals(lang)) {
                        ctry = null;
                    }
                } else if (base != null) {
                    if (ctry.equals(base)) {
                        ctry = null;
                    }
                }
            }
            if (lang != null) {
                if (lang.equals(base)) {
                    lang = null;
                }
            }

            // OK, now we are ready to assign a type
            if (hasCountry) {
                if (ctry == null) {
                    type = UNTRANSLATED;
                    if (lang != null) {
                        type = TRANSLATED_IN_PARENT;
                    }
                } else {
                    type = TRANSLATED;
                }
            } else {
                type = lang == null ? UNTRANSLATED : TRANSLATED;
            }
        }

        @Override
        public String toString() {
            return key;
        }

        /**
         * @return the type
         */
        public int getType() {
            return type;
        }
    }

    private static final int NOT_IN_ROOT = -1;
    private static final int UNTRANSLATED = 0;
    private static final int TRANSLATED_IN_PARENT = 1;
    private static final int TRANSLATED = 2;

    private DefaultListCellRenderer keyRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Color c;
            Key k = (Key) value;
            switch (k.getType()) {
                case NOT_IN_ROOT:
                    c = notInDefaultLabel.getBackground();
                    break;
                case UNTRANSLATED:
                    c = untranslatedLabel.getBackground();
                    break;
                case TRANSLATED_IN_PARENT:
                    c = translatedInParentLabel.getBackground();
                    break;
                default:
                    c = isSelected ? list.getSelectionForeground() : list.getForeground();
            }
            // Having the diff tag overrides all other conditions
            if (k.diff) {
                c = Palette.get.foreground.opaque.red;
            }
            setForeground(c);
            return this;
        }
    };
}
