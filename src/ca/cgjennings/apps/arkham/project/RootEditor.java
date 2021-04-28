package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.MarkupTargetFactory;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.prefs.LanguageCodeDescriptor;
import ca.cgjennings.apps.arkham.plugins.PluginRoot;
import static ca.cgjennings.apps.arkham.plugins.PluginRoot.*;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogIDListPanel;
import ca.cgjennings.apps.arkham.plugins.catalog.Listing;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.IconBorder;
import ca.cgjennings.ui.dnd.ScrapBook;
import ca.cgjennings.ui.textedit.tokenizers.HTMLTokenizer;
import ca.cgjennings.ui.textedit.tokenizers.PropertyTokenizer;
import gamedata.Game;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * An basic interactive editor for <tt>eons-plugin</tt> files. It recursively
 * scans for candidate script files and displays them in a pickable list. It
 * generates a valid root file from the selected files, retaining comments at
 * the top of the file (if any). An alternate view allows the user to edit the
 * code directly.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RootEditor extends javax.swing.JDialog implements AgnosticDialog {

    private File file;
    private boolean simpleView = true;
    private PluginRoot pluginRoot;

    private static final String[] priority_keywords = new String[]{
        "LOW", "NORMAL", "EXPANSION", "GAME", "HIGH"
    };
    private static final int[] priority_map = new int[]{
        PRIORITY_LOW, PRIORITY_DEFAULT, PRIORITY_EXPANSION,
        PRIORITY_GAME, PRIORITY_HIGH
    };

    private String originalText;

    private final Icon greyScript = ImageUtilities.createDesaturatedIcon(MetadataSource.ICON_SCRIPT);
    private final Icon blankIcon = new BlankIcon(greyScript.getIconWidth(), greyScript.getIconHeight());

    /**
     * Shows the root editor for a plug-in root file. If an editor for this root
     * is already open, then that window will gain focus. Otherwise, a new root
     * window will be created.
     *
     * @param root the project member to edit
     * @param cascade if opening multiple editors at once, provide a sequence of
     * cascade values starting from 0
     * @return a root editor for the member, reusing the existing window for the
     * member if one exists
     * @throws IOException if an I/O exception occurs while reading the root
     * file
     */
    public static RootEditor showRootEditor(Member root, int cascade) throws IOException {
        RootEditor ed = openEditors.get(root.getFile());
        if (ed == null) {
            ed = new RootEditor(root);
            Project p = root.getProject();
            if (p != null && p.getView() != null) {
                p.getView().moveToLocusOfAttention(ed, Math.max(cascade, 0));
            }
            openEditors.put(root.getFile(), ed);
            ed.setVisible(true);
        } else {
            ed.toFront();
            ed.requestFocusInWindow();
        }
        return ed;
    }
    private static HashMap<File, RootEditor> openEditors = new HashMap<>();

    @Override
    public void dispose() {
        openEditors.remove(file);
        super.dispose();
    }

    /**
     * Creates new form RootEditor
     */
    private RootEditor(Member root) throws IOException {
        super(StrangeEons.getWindow(), false);
        file = root.getFile();
        initComponents();
        defaultFieldBG = catNameField.getBackground();
        sourceFieldBG = catDescField.getBackground();
        libIconCombo.setResourceBase(root.getFile().getParentFile());
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);
        IconBorder.applyLabelBorder(catHomeField, "http://", ResourceKit.getIcon("ui/controls/url-field.png"), null, null, null);
        AbstractGameComponentEditor.localizeComboBoxLabels(catHiddenCombo, null);

        setLocationRelativeTo(root.getProject().getView());

        DefaultListModel pluginModel = new DefaultListModel();
        DefaultComboBoxModel installModel = new DefaultComboBoxModel();
        installModel.addElement(new Entry());

        buildList("", file.getParentFile(), pluginModel, installModel);

        if (pluginModel.size() == 0) {
            pluginModel.addElement(new Entry());
            scriptList.setEnabled(false);
        }

        scriptList.setModel(pluginModel);
        Renderer renderer = new Renderer();
        scriptList.setCellRenderer(renderer);
        installScript.setModel(installModel);
        installScript.setRenderer(renderer);
        if (installModel.getSize() == 1) {
            installScript.setEnabled(false);
        }

        pluginRoot = new PluginRoot(file);
        originalText = pluginRoot.toString();
        editor.setTokenizer(new PropertyTokenizer(true));
        MarkupTargetFactory.enableTargeting(editor, false);
        editor.setText(originalText);
        editor.select(0, 0);

        final DocumentEventAdapter updateCatListener = new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                if (fillingInView) {
                    return;
                }
                EventQueue.invokeLater(RootEditor.this::updateCatalogInfo);
            }
        };
        ActionListener updateCatAction = (ActionEvent e) -> {
            updateCatListener.changedUpdate(null);
        };

        PropertyChangeListener idListChanged = (PropertyChangeEvent evt) -> {
            if (fillingInView) {
                return;
            }
            updateCatalogInfo();
        };

        catLibNameField.getDocument().addDocumentListener(updateCatListener);
        catLibDescField.getDocument().addDocumentListener(updateCatListener);

        catNameField.getDocument().addDocumentListener(updateCatListener);
        catDescField.getDocument().addDocumentListener(updateCatListener);
        catCreditsField.getDocument().addDocumentListener(updateCatListener);
        catHomeField.getDocument().addDocumentListener(updateCatListener);

        gameField.getDocument().addDocumentListener(updateCatListener);
        coreField.getDocument().addDocumentListener(updateCatListener);
        tagsField.getDocument().addDocumentListener(updateCatListener);

        libIconCombo.addActionListener(updateCatAction);
        catHiddenCombo.addActionListener(updateCatAction);
        buildCombo.addActionListener(updateCatAction);

        catRequiresTable.addPropertyChangeListener(CatalogIDListPanel.LIST_MODIFIED_PROPERTY, idListChanged);
        catReplacesTable.addPropertyChangeListener(CatalogIDListPanel.LIST_MODIFIED_PROPERTY, idListChanged);

        customizeTagsField();

        fillInSimpleView();

        switchViews(true);
        switchViews(true);

        // autogenerate a catalogue ID
        if (pluginRoot.getCatalogID() == null && !Settings.getUser().getYesNo(KEY_NO_AUTOMATIC_ID)) {
            genIDBtnActionPerformed(null);
        }

        // show code editor if there are no scripts to pick:
        // presumably the user is working in a new plug-in and
        // wants to specify a plug-in file that doesn't exist yet
//		if( !scriptList.isEnabled() ) {
//			switchViews( true );
//		}
        languageCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof LanguageCodeDescriptor) {
                    LanguageCodeDescriptor lcd = (LanguageCodeDescriptor) value;
                    setIcon(Language.getIconForLocale(lcd.getLocale()));
                } else {
                    setIcon(ResourceKit.getIcon("ui/locale.png"));
                }
                return this;
            }
        });
        loadLanguageCombo();
        simpleTabs.addChangeListener((ChangeEvent e) -> {
            int tab = simpleTabs.getSelectedIndex();
            if (tab == 1) {
                loadLanguageCombo();
            }
        });
        languageCombo.addActionListener((ActionEvent e) -> {
            if (fillingInView) {
                return;
            }
            fillingInView = true;
            readWriteLocalizedProperties(false, selectedLocale);
            Object sel = languageCombo.getSelectedItem();
            if (sel == null || sel instanceof String) {
                selectedLocale = null;
            } else {
                selectedLocale = ((LanguageCodeDescriptor) sel).getLocale();
            }
            readWriteLocalizedProperties(true, selectedLocale);
            fillingInView = false;
            
            Color fieldBG = selectedLocale == null ? defaultFieldBG : nondefaultBG;
            Color sourceBG = selectedLocale == null ? sourceFieldBG : nondefaultBG;
            catNameField.setBackground(fieldBG);
            catDescField.setBackground(sourceBG);
            catLibNameField.setBackground(fieldBG);
            catLibDescField.setBackground(sourceBG);
        });

        pack();
    }

    private void readWriteLocalizedProperties(boolean read, Locale locale) {
        String suffix = locale == null ? "" : '_' + locale.toString();
        if (read) {
            catNameField.setText(pluginRoot.getClientProperty(PluginRoot.CLIENT_KEY_CATALOG_NAME + suffix));
            catDescField.setText(pluginRoot.getClientProperty(PluginRoot.CLIENT_KEY_CATALOG_DESCRIPTION + suffix));
            catLibNameField.setText(pluginRoot.getClientProperty(PluginRoot.CLIENT_KEY_NAME + suffix));
            catLibDescField.setText(pluginRoot.getClientProperty(PluginRoot.CLIENT_KEY_DESCRIPTION + suffix));
        } else {
            writeLocalizedProperty(PluginRoot.CLIENT_KEY_NAME + suffix, catLibNameField.getText());
            writeLocalizedProperty(PluginRoot.CLIENT_KEY_DESCRIPTION + suffix, catLibDescField.getText());
            writeLocalizedProperty(PluginRoot.CLIENT_KEY_CATALOG_NAME + suffix, catNameField.getText());
            writeLocalizedProperty(PluginRoot.CLIENT_KEY_CATALOG_DESCRIPTION + suffix, catDescField.getText());
        }
    }

    private void writeLocalizedProperty(String key, String value) {
        value = value.trim();
        if (value.isEmpty()) {
            pluginRoot.putClientProperty(key, null);
        } else {
            pluginRoot.putClientProperty(key, value);
        }
    }

    private Color defaultFieldBG, sourceFieldBG, nondefaultBG = new Color(0xffefc3);

    private void loadLanguageCombo() {
        DefaultComboBoxModel languageModel = new DefaultComboBoxModel();
        languageModel.addElement(string("dt-l-text-default"));
        Set<String> locales = fieldListToSet(uiLangField);
        boolean wasFillingInView = fillingInView;
        fillingInView = true;
        for (String s : locales) {
            languageModel.addElement(
                    new LanguageCodeDescriptor(Language.parseLocaleDescription(s), false, true)
            );
        }
        languageCombo.setModel(languageModel);
        if (selectedLocale == null) {
            languageCombo.setSelectedIndex(0);
        } else {
            int i = 1;
            for (; i < languageModel.getSize(); ++i) {
                if (((LanguageCodeDescriptor) languageModel.getElementAt(i)).getLocale().equals(selectedLocale)) {
                    languageCombo.setSelectedIndex(i);
                    break;
                }
            }
            if (i == languageModel.getSize()) {
                languageModel.addElement(new LanguageCodeDescriptor(selectedLocale));
                languageCombo.setSelectedIndex(i);
            }
        }
        fillingInView = wasFillingInView;
    }

    private Locale selectedLocale;

    private void switchViews(boolean fillIn) {
        simpleView = !simpleView;
        String card = simpleView ? "s" : "a";
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, card);
        if (simpleView) {
            if (fillIn) {
                pluginRoot = new PluginRoot(editor.getText());
                fillInSimpleView();
            }
            switchBtn.setText(string("prj-re-advanced"));
        } else {
            if (fillIn) {
                fillInAdvancedView();
            }
            switchBtn.setText(string("prj-re-simple"));
            editor.requestFocusInWindow();
        }
    }

    private void buildList(String base, File parent, DefaultListModel pluginModel, DefaultComboBoxModel installModel) {
        File[] files = parent.listFiles();
        Arrays.sort(files);
        for (int i = 0; i < files.length; ++i) {
            if (files[i].isHidden()) {
                continue;
            }
            String name = files[i].getName();
            if (files[i].isDirectory()) {
                String path = base + (base.length() > 0 ? "/" : "") + name;
                buildList(path, files[i], pluginModel, installModel);
            } else if (name.endsWith(".js") || name.endsWith(".class") || name.endsWith(".java")) {
                // rule out anonymous inner classes
                if (!name.matches(".*\\$\\d.*\\.((class)|(java))")) {
                    String path = base + (base.length() > 0 ? "/" : "") + name;

                    // since .class and .java are both included, check if there
                    // is already an entry before adding a duplicate
                    Entry entry = new Entry(files[i], path);
                    boolean found = false;
                    for (int j = 0; j < pluginModel.getSize() && !found; ++j) {
                        if (pluginModel.get(j).toString().equals(entry.toString())) {
                            found = true;
                        }
                    }

                    if (!found) {
                        pluginModel.addElement(entry);
                        // installers can only be scripts
                        if (!name.endsWith(".class") && !name.endsWith(".java")) {
                            installModel.addElement(new Entry(files[i], path, true));
                        }
                    }
                }
            }
        }
    }

    private boolean fillingInView = false;

    private void fillInSimpleView() {
        fillingInView = true;

        // MAIN TAB
        DefaultListModel model = (DefaultListModel) scriptList.getModel();
        for (String plugin : pluginRoot.getPluginIdentifiers()) {
            plugin = PluginRoot.decoratePluginIdentifier(plugin);
            int s;
            for (s = 0; s < model.size(); ++s) {
                if (((Entry) model.getElementAt(s)).getCode().equals(plugin)) {
                    scriptList.addSelectionInterval(s, s);
                    break;
                }
            }
            if (s < model.size()) {
                scriptList.scrollRectToVisible(scriptList.getCellBounds(s, s));
            }
        }

        String install = pluginRoot.getInstallerIdentifier();
        if (install == null) {
            installScript.setSelectedIndex(0);
        } else {
            install = PluginRoot.decoratePluginIdentifier(install);
            DefaultComboBoxModel installModel = (DefaultComboBoxModel) installScript.getModel();
            for (int is = 0; is < installModel.getSize(); ++is) {
                if (((Entry) installModel.getElementAt(is)).getCode().equals(install)) {
                    installScript.setSelectedIndex(is);
                    break;
                }
            }
        }

        int pri = pluginRoot.getPriority();
        int p;
        for (p = 0; p < priority_map.length; ++p) {
            if (pri == priority_map[p]) {
                priorityCombo.setSelectedIndex(p);
                break;
            }
        }
        if (p == priority_map.length) {
            priorityCombo.setSelectedItem(String.valueOf(pri));
        }

        uiLangField.setText(cleanLanguageTokenList(pluginRoot.getClientProperty(PluginRoot.CLIENT_KEY_UI_LANGUAGES), true));
        gameLangField.setText(cleanLanguageTokenList(pluginRoot.getClientProperty(PluginRoot.CLIENT_KEY_GAME_LANGUAGES), true));

        CatalogID extractedID = pluginRoot.getCatalogID();
        boolean enable;
        if (extractedID == null) {
            uuidField.setText("");
            idVersionField.setText("");
            enable = false;
        } else {
            uuidField.setText(extractedID.toUUIDString().toUpperCase(Locale.ENGLISH));
            idVersionField.setText(extractedID.getFormattedDate());
            uuidField.select(0, 0);
            idVersionField.select(0, 0);
            catRequiresTable.setForbiddenID(extractedID);
            catReplacesTable.setForbiddenID(extractedID);
            enable = true;
        }
        idVersionLabel.setEnabled(enable);
        idVersionField.setEnabled(enable);
        copyIDBtn.setEnabled(enable);
        touchBtn.setEnabled(enable);

        // METADATA TAB
        readWriteLocalizedProperties(true, selectedLocale);
        String res = pluginRoot.getClientProperty(PluginRoot.CLIENT_KEY_IMAGE);
        libIconCombo.setSelectedItem(stripPrefix(res, "res://"));

        catDescField.select(0, 0);
        catCreditsField.setText(pluginRoot.getClientProperty("catalog-" + Listing.CREDIT));

        buildCombo.setSelectedItem(pluginRoot.getClientProperty("catalog-" + Listing.MINIMUM_VERSION));
        coreField.setText(pluginRoot.getClientProperty("catalog-" + Listing.CORE));
        tagsField.setText(pluginRoot.getClientProperty("catalog-" + Listing.TAGS));

        gameField.setText(pluginRoot.getClientProperty("catalog-" + Listing.GAME));
        gameFieldFocusLost(null);

        String url = pluginRoot.getClientProperty("catalog-" + Listing.HOME_PAGE);
        catHomeField.setText(stripPrefix(url, "http://"));

        // ADVANCED TAB
        catRequiresTable.initFromString(pluginRoot.getClientProperty("catalog-" + Listing.REQUIRES));
        catReplacesTable.initFromString(pluginRoot.getClientProperty("catalog-" + Listing.REPLACES));

        int hideIndex = 0;
        String hiddenVal = pluginRoot.getClientProperty("catalog-" + Listing.HIDDEN);
        if (hiddenVal != null) {
            if (hiddenVal.equals("yes")) {
                hideIndex = 1;
            } else if (hiddenVal.equals("depends")) {
                hideIndex = 2;
            }
        }
        catHiddenCombo.setSelectedIndex(hideIndex);

        fillingInView = false;
    }

    private String stripPrefix(String s, String prefix) {
        if (s != null) {
            while (s.startsWith(prefix)) {
                s = s.substring(prefix.length());
            }
        } else {
            s = "";
        }
        return s;
    }

    private void updateCatalogInfo() {
        readWriteLocalizedProperties(false, selectedLocale);
//		updateCatalogInfoImpl( PluginRoot.CLIENT_KEY_NAME, catLibNameField.getText() );
//		updateCatalogInfoImpl( PluginRoot.CLIENT_KEY_DESCRIPTION, catLibDescField.getText() );
//		updateCatalogInfoImpl( "catalog-" + Listing.NAME, catNameField.getText() );
//		updateCatalogInfoImpl( "catalog-" + Listing.DESCRIPTION, catDescField.getText() );
        updateCatalogInfoImpl("catalog-" + Listing.CREDIT, catCreditsField.getText());
        libIconComboActionPerformed(null);

        String home = stripPrefix(catHomeField.getText(), "http://");
        if (!home.isEmpty()) {
            home = "http://" + home;
        }
        updateCatalogInfoImpl("catalog-" + Listing.HOME_PAGE, home);

        Object build = buildCombo.getSelectedItem();
        updateCatalogInfoImpl("catalog-" + Listing.MINIMUM_VERSION, build == null ? null : build.toString());
        updateCatalogInfoImpl("catalog-" + Listing.REQUIRES, catRequiresTable.toCatalogString());
        updateCatalogInfoImpl("catalog-" + Listing.REPLACES, catReplacesTable.toCatalogString());
        updateCatalogInfoImpl("catalog-" + Listing.TAGS, tagsField.getText());
        updateCatalogInfoImpl("catalog-" + Listing.GAME, gameField.getText());
        updateCatalogInfoImpl("catalog-" + Listing.CORE, coreField.getText());

        String hidden;
        switch (catHiddenCombo.getSelectedIndex()) {
            case 1:
                hidden = "yes";
                break;
            case 2:
                hidden = "depends";
                break;
            default:
                hidden = null;
                break;
        }
        updateCatalogInfoImpl("catalog-" + Listing.HIDDEN, hidden);
    }

    private void updateCatalogInfoImpl(String prop, String val) {
        if (val != null) {
            val = val.trim();
            if (val.isEmpty()) {
                val = null;
            }
        }
        pluginRoot.putClientProperty(prop, val);
    }

    private void fillInAdvancedView() {
        editor.setText(pluginRoot.toString());
        editor.select(0, 0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cardPanel = new javax.swing.JPanel();
        simple = new javax.swing.JPanel();
        simpleTabs = new javax.swing.JTabbedPane();
        javax.swing.JPanel classTabPanel = new javax.swing.JPanel();
        javax.swing.JPanel pluginClassPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        scriptList = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        javax.swing.JPanel catalogIDPanel = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        uuidField = new javax.swing.JTextField();
        genIDBtn = new javax.swing.JButton();
        idVersionLabel = new javax.swing.JLabel();
        idVersionField = new javax.swing.JTextField();
        touchBtn = new javax.swing.JButton();
        catIDhelp = new ca.cgjennings.ui.JHelpButton();
        copyIDBtn = new javax.swing.JButton();
        autotouchCheck = new javax.swing.JCheckBox();
        catalogDescPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        catNameField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        catCreditsField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        catHomeField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        catDescField = new ca.cgjennings.ui.textedit.JSourceCodeEditor();
        libraryDescPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        catLibNameField = new javax.swing.JTextField();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        libIconCombo = new ca.cgjennings.apps.arkham.project.ImageResourceCombo();
        catLibDescField = new ca.cgjennings.ui.textedit.JSourceCodeEditor();
        javax.swing.JPanel referencedBundlePanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        jTip3 = new ca.cgjennings.ui.JTip();
        catRequiresTable = new ca.cgjennings.apps.arkham.plugins.catalog.CatalogIDListPanel();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        catReplacesTable = new ca.cgjennings.apps.arkham.plugins.catalog.CatalogIDListPanel();
        jTip4 = new ca.cgjennings.ui.JTip();
        javax.swing.JPanel advancedTabPanel = new javax.swing.JPanel();
        javax.swing.JPanel languagePanel = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        gameLangField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        uiLangField = new javax.swing.JTextField();
        javax.swing.JLabel languagesInfo = new javax.swing.JLabel();
        javax.swing.JPanel bundleSettingPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        priorityCombo = new javax.swing.JComboBox();
        installScript = new javax.swing.JComboBox();
        jTip1 = new ca.cgjennings.ui.JTip();
        javax.swing.JPanel advancedFeaturePanel = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        buildCombo = new javax.swing.JComboBox();
        jLabel18 = new javax.swing.JLabel();
        gameField = new javax.swing.JTextField();
        gameCombo = new ca.cgjennings.ui.JGameCombo();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        catHiddenCombo = new javax.swing.JComboBox();
        jTip2 = new ca.cgjennings.ui.JTip();
        jLabel19 = new javax.swing.JLabel();
        coreField = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        tagsField = new javax.swing.JTextField();
        advanced = new javax.swing.JPanel();
        editor = new ca.cgjennings.ui.textedit.JSourceCodeEditor();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        switchBtn = new javax.swing.JButton();
        ca.cgjennings.ui.JHelpButton rootEdHelp = new ca.cgjennings.ui.JHelpButton();
        languageLabel = new javax.swing.JLabel();
        languageCombo = new javax.swing.JComboBox();
        jTip5 = new ca.cgjennings.ui.JTip();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "prj-re-title" )); // NOI18N

        cardPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        cardPanel.setLayout(new java.awt.CardLayout());

        pluginClassPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("prj-re-script-list"))); // NOI18N

        scriptList.setFont(scriptList.getFont().deriveFont(scriptList.getFont().getSize()-1f));
        scriptList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                scriptListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(scriptList);

        jLabel1.setText(string( "prj-re-intro" )); // NOI18N

        javax.swing.GroupLayout pluginClassPanelLayout = new javax.swing.GroupLayout(pluginClassPanel);
        pluginClassPanel.setLayout(pluginClassPanelLayout);
        pluginClassPanelLayout.setHorizontalGroup(
            pluginClassPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pluginClassPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pluginClassPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(pluginClassPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pluginClassPanelLayout.setVerticalGroup(
            pluginClassPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pluginClassPanelLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
        );

        catalogIDPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "prj-re-cat-panel" ))); // NOI18N

        jLabel3.setText(string( "prj-re-uuid" )); // NOI18N

        uuidField.setEditable(false);
        uuidField.setColumns(36);
        uuidField.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        uuidField.setText("A27B23AF-95B5-4A4C-8FEC-C849C72CE512");

        genIDBtn.setText(string( "prj-re-id-generate" )); // NOI18N
        genIDBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genIDBtnActionPerformed(evt);
            }
        });

        idVersionLabel.setText(string( "prj-re-id-version" )); // NOI18N

        idVersionField.setEditable(false);
        idVersionField.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N

        touchBtn.setText(string( "prj-re-id-touch" )); // NOI18N
        touchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                touchBtnActionPerformed(evt);
            }
        });

        catIDhelp.setHelpPage("dm-eons-plugin#catalogue-information");

        copyIDBtn.setIcon( ResourceKit.getIcon( "toolbar/copy.png" ) );
        copyIDBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyIDBtnActionPerformed(evt);
            }
        });

        autotouchCheck.setSelected( Settings.getUser().getBoolean( "make-bundle-autotouch", true ) );
        autotouchCheck.setText(string("prj-re-id-autotouch")); // NOI18N
        autotouchCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autotouchCheckActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout catalogIDPanelLayout = new javax.swing.GroupLayout(catalogIDPanel);
        catalogIDPanel.setLayout(catalogIDPanelLayout);
        catalogIDPanelLayout.setHorizontalGroup(
            catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(catalogIDPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(idVersionLabel)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(autotouchCheck)
                    .addGroup(catalogIDPanelLayout.createSequentialGroup()
                        .addGroup(catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(idVersionField)
                            .addComponent(uuidField))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(genIDBtn)
                            .addComponent(touchBtn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(copyIDBtn)
                            .addComponent(catIDhelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        catalogIDPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {genIDBtn, touchBtn});

        catalogIDPanelLayout.setVerticalGroup(
            catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(catalogIDPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel3)
                    .addComponent(uuidField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(genIDBtn)
                    .addComponent(copyIDBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(catalogIDPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(idVersionLabel)
                    .addComponent(idVersionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(touchBtn)
                    .addComponent(catIDhelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(autotouchCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout classTabPanelLayout = new javax.swing.GroupLayout(classTabPanel);
        classTabPanel.setLayout(classTabPanelLayout);
        classTabPanelLayout.setHorizontalGroup(
            classTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(classTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(classTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pluginClassPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(catalogIDPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        classTabPanelLayout.setVerticalGroup(
            classTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(classTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pluginClassPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(catalogIDPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        simpleTabs.addTab(string("prj-re-tab-plugin"), classTabPanel); // NOI18N

        jLabel2.setLabelFor(catNameField);
        jLabel2.setText(string("prj-re-ci-name")); // NOI18N

        catNameField.setColumns(30);

        jLabel5.setLabelFor(catCreditsField);
        jLabel5.setText(string("prj-re-ci-credits")); // NOI18N

        jLabel7.setLabelFor(catHomeField);
        jLabel7.setText(string("prj-re-ci-url")); // NOI18N

        catHomeField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                catHomeFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                catHomeFieldFocusLost(evt);
            }
        });

        jLabel9.setLabelFor(catDescField);
        jLabel9.setText(string("prj-re-ci-desc")); // NOI18N

        jLabel15.setFont(jLabel15.getFont().deriveFont(jLabel15.getFont().getSize()-1f));
        jLabel15.setText(string("prj-re-ci-cat-info")); // NOI18N

        catDescField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(128, 128, 128)));
        catDescField.setPreferredSize(new java.awt.Dimension(100, 32));
        catDescField.setTokenizer( new HTMLTokenizer(false) );

        javax.swing.GroupLayout catalogDescPanelLayout = new javax.swing.GroupLayout(catalogDescPanel);
        catalogDescPanel.setLayout(catalogDescPanelLayout);
        catalogDescPanelLayout.setHorizontalGroup(
            catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(catalogDescPanelLayout.createSequentialGroup()
                .addGroup(catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(catalogDescPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, catalogDescPanelLayout.createSequentialGroup()
                                .addGroup(catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel5)
                                    .addComponent(jLabel7))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(catCreditsField)
                                    .addComponent(catHomeField)
                                    .addComponent(catNameField)))
                            .addGroup(catalogDescPanelLayout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addGap(0, 515, Short.MAX_VALUE))))
                    .addGroup(catalogDescPanelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(catDescField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(catalogDescPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        catalogDescPanelLayout.setVerticalGroup(
            catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(catalogDescPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(catNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(catCreditsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(catalogDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(catHomeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(catDescField, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                .addContainerGap())
        );

        simpleTabs.addTab(string("prj-re-cat-info"), catalogDescPanel); // NOI18N

        jLabel8.setLabelFor(catNameField);
        jLabel8.setText(string("prj-re-ci-libname")); // NOI18N

        catLibNameField.setColumns(30);

        jLabel13.setLabelFor(catDescField);
        jLabel13.setText(string("prj-re-ci-libdesc")); // NOI18N

        jLabel14.setFont(jLabel14.getFont().deriveFont(jLabel14.getFont().getSize()-1f));
        jLabel14.setIcon( ResourceKit.getIcon( "project/library.png" ) );
        jLabel14.setText(string("prj-re-ci-lib-info")); // NOI18N

        jLabel16.setText(string("prj-re-ci-libicon")); // NOI18N

        libIconCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libIconComboActionPerformed(evt);
            }
        });
        libIconCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                libIconComboFocusLost(evt);
            }
        });

        catLibDescField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(128, 128, 128)));
        catLibDescField.setPreferredSize(new java.awt.Dimension(100, 32));
        catLibDescField.setTokenizer( new HTMLTokenizer(false) );

        javax.swing.GroupLayout libraryDescPanelLayout = new javax.swing.GroupLayout(libraryDescPanel);
        libraryDescPanel.setLayout(libraryDescPanelLayout);
        libraryDescPanelLayout.setHorizontalGroup(
            libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(libraryDescPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(libraryDescPanelLayout.createSequentialGroup()
                        .addGroup(libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8)
                            .addComponent(jLabel16))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(libIconCombo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(catLibNameField)))
                    .addGroup(libraryDescPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(catLibDescField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(libraryDescPanelLayout.createSequentialGroup()
                        .addGroup(libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel14))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        libraryDescPanelLayout.setVerticalGroup(
            libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(libraryDescPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(catLibNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(libraryDescPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel16)
                    .addComponent(libIconCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(catLibDescField, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                .addContainerGap())
        );

        simpleTabs.addTab(string("prj-re-lib-entry"), libraryDescPanel); // NOI18N

        jLabel11.setLabelFor(catRequiresTable);
        jLabel11.setText(string("prj-re-requires")); // NOI18N

        jTip3.setTipText(string("prj-re-cat-requires-tip")); // NOI18N

        jLabel12.setLabelFor(catReplacesTable);
        jLabel12.setText(string("prj-re-replaces")); // NOI18N

        jTip4.setTipText(string("prj-re-cat-replaces-tip")); // NOI18N

        javax.swing.GroupLayout referencedBundlePanelLayout = new javax.swing.GroupLayout(referencedBundlePanel);
        referencedBundlePanel.setLayout(referencedBundlePanelLayout);
        referencedBundlePanelLayout.setHorizontalGroup(
            referencedBundlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(referencedBundlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(referencedBundlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(catReplacesTable, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
                    .addGroup(referencedBundlePanelLayout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTip4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(catRequiresTable, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(referencedBundlePanelLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTip3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        referencedBundlePanelLayout.setVerticalGroup(
            referencedBundlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(referencedBundlePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(referencedBundlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel11)
                    .addComponent(jTip3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(catRequiresTable, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(referencedBundlePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel12)
                    .addComponent(jTip4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(catReplacesTable, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
                .addContainerGap())
        );

        simpleTabs.addTab(string("prj-re-cat-ref-links"), referencedBundlePanel); // NOI18N

        advancedTabPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));

        languagePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("prj-re-cat-lang"))); // NOI18N

        jLabel21.setLabelFor(gameLangField);
        jLabel21.setText(string("prj-re-ci-game-lang")); // NOI18N

        gameLangField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                gameLangFieldFocusLost(evt);
            }
        });

        jLabel22.setLabelFor(uiLangField);
        jLabel22.setText(string("prj-re-ci-ui-lang")); // NOI18N

        uiLangField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                uiLangFieldFocusLost(evt);
            }
        });

        languagesInfo.setFont(languagesInfo.getFont().deriveFont(languagesInfo.getFont().getSize()-1f));
        languagesInfo.setIcon( ResourceKit.getIcon( "ui/locale.png" ) );
        languagesInfo.setText(string("prj-re-ci-lang-info")); // NOI18N

        javax.swing.GroupLayout languagePanelLayout = new javax.swing.GroupLayout(languagePanel);
        languagePanel.setLayout(languagePanelLayout);
        languagePanelLayout.setHorizontalGroup(
            languagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(languagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(languagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(languagePanelLayout.createSequentialGroup()
                        .addGroup(languagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel22)
                            .addComponent(jLabel21))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(languagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gameLangField)
                            .addComponent(uiLangField)))
                    .addGroup(languagePanelLayout.createSequentialGroup()
                        .addComponent(languagesInfo)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        languagePanelLayout.setVerticalGroup(
            languagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(languagePanelLayout.createSequentialGroup()
                .addComponent(languagesInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(languagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(gameLangField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(languagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(uiLangField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        bundleSettingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("prj-re-options"))); // NOI18N

        jLabel6.setLabelFor(priorityCombo);
        jLabel6.setText(string( "prj-re-priority" )); // NOI18N

        jLabel4.setLabelFor(installScript);
        jLabel4.setText(string( "prj-re-pick-install-script" )); // NOI18N

        priorityCombo.setEditable(true);
        priorityCombo.setModel( new DefaultComboBoxModel( priority_keywords ) );
        priorityCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                priorityComboActionPerformed(evt);
            }
        });
        priorityCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                priorityComboFocusLost(evt);
            }
        });

        installScript.setFont(installScript.getFont().deriveFont(installScript.getFont().getSize()-1f));
        installScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installScriptActionPerformed(evt);
            }
        });

        jTip1.setTipText(string( "prj-tip-priority" )); // NOI18N

        javax.swing.GroupLayout bundleSettingPanelLayout = new javax.swing.GroupLayout(bundleSettingPanel);
        bundleSettingPanel.setLayout(bundleSettingPanelLayout);
        bundleSettingPanelLayout.setHorizontalGroup(
            bundleSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bundleSettingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bundleSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(bundleSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bundleSettingPanelLayout.createSequentialGroup()
                        .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTip1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(installScript, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        bundleSettingPanelLayout.setVerticalGroup(
            bundleSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bundleSettingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bundleSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(installScript, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(bundleSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bundleSettingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(priorityCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTip1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        bundleSettingPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jTip1, priorityCombo});

        advancedFeaturePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("prj-re-cat-advanced"))); // NOI18N

        jLabel17.setLabelFor(buildCombo);
        jLabel17.setText(string("prj-re-minver")); // NOI18N

        buildCombo.setEditable(true);
        buildCombo.setModel( new DefaultComboBoxModel( new Object[] { StrangeEons.getBuildNumber() } ) );
        buildCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                buildComboFocusLost(evt);
            }
        });

        jLabel18.setLabelFor(gameField);
        jLabel18.setText(string("prj-re-game")); // NOI18N

        gameField.setColumns(6);
        gameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                gameFieldFocusLost(evt);
            }
        });

        gameCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gameFieldActionPerformed(evt);
            }
        });

        jLabel10.setLabelFor(catHiddenCombo);
        jLabel10.setText(string("prj-re-cat-hidden")); // NOI18N

        catHiddenCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "prj-re-cat-hidden-0", "prj-re-cat-hidden-1", "prj-re-cat-hidden-2" }));

        jTip2.setTipText(string("prj-re-cat-hidden-tip")); // NOI18N

        jLabel19.setLabelFor(coreField);
        jLabel19.setText(string("prj-re-core")); // NOI18N

        jLabel20.setLabelFor(tagsField);
        jLabel20.setText(string("prj-re-tags")); // NOI18N

        javax.swing.GroupLayout advancedFeaturePanelLayout = new javax.swing.GroupLayout(advancedFeaturePanel);
        advancedFeaturePanel.setLayout(advancedFeaturePanelLayout);
        advancedFeaturePanelLayout.setHorizontalGroup(
            advancedFeaturePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advancedFeaturePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advancedFeaturePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(advancedFeaturePanelLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addGap(27, 27, 27)
                        .addComponent(gameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(advancedFeaturePanelLayout.createSequentialGroup()
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buildCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(advancedFeaturePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10)
                    .addComponent(jLabel20))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(advancedFeaturePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(tagsField)
                    .addGroup(advancedFeaturePanelLayout.createSequentialGroup()
                        .addComponent(catHiddenCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTip2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(coreField, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)))
                .addContainerGap())
        );

        advancedFeaturePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buildCombo, catHiddenCombo});

        advancedFeaturePanelLayout.setVerticalGroup(
            advancedFeaturePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advancedFeaturePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advancedFeaturePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel18)
                    .addComponent(gameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gameCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20)
                    .addComponent(tagsField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(advancedFeaturePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel17)
                    .addComponent(buildCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(catHiddenCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTip2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19)
                    .addComponent(coreField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout advancedTabPanelLayout = new javax.swing.GroupLayout(advancedTabPanel);
        advancedTabPanel.setLayout(advancedTabPanelLayout);
        advancedTabPanelLayout.setHorizontalGroup(
            advancedTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advancedTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(advancedTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(languagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(advancedFeaturePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(bundleSettingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        advancedTabPanelLayout.setVerticalGroup(
            advancedTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(advancedTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(languagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bundleSettingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(advancedFeaturePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        simpleTabs.addTab(string("prj-re-tab-catadv"), advancedTabPanel); // NOI18N

        javax.swing.GroupLayout simpleLayout = new javax.swing.GroupLayout(simple);
        simple.setLayout(simpleLayout);
        simpleLayout.setHorizontalGroup(
            simpleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(simpleTabs)
        );
        simpleLayout.setVerticalGroup(
            simpleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(simpleLayout.createSequentialGroup()
                .addComponent(simpleTabs)
                .addGap(16, 16, 16))
        );

        cardPanel.add(simple, "s");

        advanced.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 16, 0), javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(128, 128, 128))));

        javax.swing.GroupLayout advancedLayout = new javax.swing.GroupLayout(advanced);
        advanced.setLayout(advancedLayout);
        advancedLayout.setHorizontalGroup(
            advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editor, javax.swing.GroupLayout.DEFAULT_SIZE, 602, Short.MAX_VALUE)
        );
        advancedLayout.setVerticalGroup(
            advancedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editor, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 493, Short.MAX_VALUE)
        );

        cardPanel.add(advanced, "a");

        cancelBtn.setText(string( "cancel" )); // NOI18N

        okBtn.setText(string( "update" )); // NOI18N

        switchBtn.setFont(switchBtn.getFont().deriveFont(switchBtn.getFont().getSize()-1f));
        switchBtn.setText(string( "prj-re-advanced" )); // NOI18N
        switchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                switchBtnActionPerformed(evt);
            }
        });

        rootEdHelp.setHelpPage("dm-eons-plugin");

        languageLabel.setLabelFor(languageCombo);
        languageLabel.setText(string("prj-re-ci-lang-select")); // NOI18N

        jTip5.setTipText(string("prj-re-l-lang-info")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rootEdHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(switchBtn)
                .addGap(18, 18, 18)
                .addComponent(languageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(languageCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTip5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(okBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelBtn)
                .addContainerGap())
            .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rootEdHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(switchBtn)
                    .addComponent(languageLabel)
                    .addComponent(languageCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTip5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okBtn)
                    .addComponent(cancelBtn))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {okBtn, rootEdHelp});

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jTip5, languageCombo});

    }// </editor-fold>//GEN-END:initComponents

	private void switchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_switchBtnActionPerformed
            switchViews(true);
	}//GEN-LAST:event_switchBtnActionPerformed

	private void scriptListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_scriptListValueChanged
            if (fillingInView) {
                return;
            }

            List<Entry> sel = scriptList.getSelectedValuesList();
            String[] files = new String[sel.size()];
            for (int i = 0; i < files.length; ++i) {
                files[i] = sel.get(i).getCode();
            }
            pluginRoot.setPluginIdentifiers(files);
	}//GEN-LAST:event_scriptListValueChanged

	private void genIDBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genIDBtnActionPerformed
            if (pluginRoot.getCatalogID() != null) {
                String[] options = new String[]{string("prj-re-id-generate"), string("cancel")};
                int GEN_OPT = 0;
                if (PlatformSupport.PLATFORM_IS_MAC) {
                    String t = options[0];
                    options[0] = options[1];
                    options[1] = t;
                    GEN_OPT = 1;
                }
                int choice = JOptionPane.showOptionDialog(
                        this, string("prj-re-id-generate-warn"), "",
                        0, JOptionPane.WARNING_MESSAGE,
                        null, options, 1 - GEN_OPT
                );
                if (choice != GEN_OPT) {
                    return;
                }
            }
            pluginRoot.setCatalogID(new CatalogID());

            fillInSimpleView();
	}//GEN-LAST:event_genIDBtnActionPerformed

	private void touchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_touchBtnActionPerformed
            CatalogID id = pluginRoot.getCatalogID();
            if (id == null) {
                throw new AssertionError("script has no CatalogID; touching disabled");
            }
            pluginRoot.setCatalogID(new CatalogID(id));
            fillInSimpleView();
	}//GEN-LAST:event_touchBtnActionPerformed

	private void installScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_installScriptActionPerformed
            if (fillingInView) {
                return;
            }

            int sel = installScript.getSelectedIndex();
            if (sel < 0) {
                return;
            }
            if (sel == 0) {
                pluginRoot.setInstallerIdentifier(null);
            } else {
                pluginRoot.setInstallerIdentifier(((Entry) installScript.getSelectedItem()).getCode());
            }
	}//GEN-LAST:event_installScriptActionPerformed

	private void priorityComboFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_priorityComboFocusLost
            int pri = PRIORITY_DEFAULT;
            String val = priorityCombo.getSelectedItem().toString().trim().toUpperCase(Locale.ENGLISH);
            if (!val.isEmpty() && Character.isLetter(val.charAt(0))) {
                for (int i = 0; i < priority_keywords.length; ++i) {
                    if (val.equals(priority_keywords[i])) {
                        pri = priority_map[i];
                        break;
                    }
                }
            } else {
                try {
                    pri = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                }
            }
            pluginRoot.setPriority(pri);
	}//GEN-LAST:event_priorityComboFocusLost

	private void priorityComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_priorityComboActionPerformed
            priorityComboFocusLost(null);
	}//GEN-LAST:event_priorityComboActionPerformed

	private void copyIDBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_copyIDBtnActionPerformed
            CatalogID id = pluginRoot.getCatalogID();
            if (id != null) {
                ScrapBook.setText(id.toString());
            }
	}//GEN-LAST:event_copyIDBtnActionPerformed

	private void libIconComboActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_libIconComboActionPerformed
            if (fillingInView || !libIconCombo.isEnabled()) {
                return;
            }
            Object o = libIconCombo.getSelectedItem();
            String value = o == null ? null : o.toString();
            if (o != null) {
                value = stripPrefix(value, "res://");
                if (!value.isEmpty()) {
                    value = "res://" + value;
                }
            }
            updateCatalogInfoImpl(PluginRoot.CLIENT_KEY_IMAGE, value);
	}//GEN-LAST:event_libIconComboActionPerformed

	private void libIconComboFocusLost( java.awt.event.FocusEvent evt ) {//GEN-FIRST:event_libIconComboFocusLost
            libIconComboActionPerformed(null);
	}//GEN-LAST:event_libIconComboFocusLost

	private void gameFieldActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_gameFieldActionPerformed
            Object o = gameCombo.getSelectedItem();
            if (o != null) {
                String code = ((Game) o).getCode();
                if (code.equals(Game.ALL_GAMES_CODE)) {
                    code = "";
                }
                gameField.setText(code);
            }
	}//GEN-LAST:event_gameFieldActionPerformed

	private void gameFieldFocusLost( java.awt.event.FocusEvent evt ) {//GEN-FIRST:event_gameFieldFocusLost
            String game = gameField.getText();
            if (game.isEmpty()) {
                game = Game.ALL_GAMES_CODE;
            }

            DefaultComboBoxModel gm = (DefaultComboBoxModel) gameCombo.getModel();
            for (int i = 0; i < gm.getSize(); ++i) {
                Game g = (Game) gm.getElementAt(i);
                if (g.getCode().equals(game)) {
                    gameCombo.setSelectedIndex(i);
                    break;
                }
            }
	}//GEN-LAST:event_gameFieldFocusLost

	private void buildComboFocusLost( java.awt.event.FocusEvent evt ) {//GEN-FIRST:event_buildComboFocusLost
            updateCatalogInfo();
	}//GEN-LAST:event_buildComboFocusLost

    private void catHomeFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_catHomeFieldFocusLost
        String u = catHomeField.getText();
        String s = stripPrefix(u, "http://");
        if (u.length() != s.length()) {
            catHomeField.setText(s);
        }
    }//GEN-LAST:event_catHomeFieldFocusLost

    private void catHomeFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_catHomeFieldFocusGained
        catHomeField.selectAll();
    }//GEN-LAST:event_catHomeFieldFocusGained

    private void autotouchCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autotouchCheckActionPerformed
        Settings.getUser().setBoolean("make-bundle-autotouch", autotouchCheck.isSelected());
    }//GEN-LAST:event_autotouchCheckActionPerformed

    private void gameLangFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_gameLangFieldFocusLost
        languageFieldChanged(gameLangField, PluginRoot.CLIENT_KEY_GAME_LANGUAGES);
    }//GEN-LAST:event_gameLangFieldFocusLost

    private void uiLangFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_uiLangFieldFocusLost
        languageFieldChanged(uiLangField, PluginRoot.CLIENT_KEY_UI_LANGUAGES);
        loadLanguageCombo();
    }//GEN-LAST:event_uiLangFieldFocusLost

    private void languageFieldChanged(JTextField field, String propertyKey) {
        String s = field.getText();
        field.setText(cleanLanguageTokenList(s, true));
        String v = cleanLanguageTokenList(s, false);
        if (v.isEmpty()) {
            v = null;
        }
        pluginRoot.putClientProperty(propertyKey, v);
    }

    private static String cleanLanguageTokenList(String value, boolean formatForDisplay) {
        StringBuilder clean = new StringBuilder(32);
        if (value != null) {
            String[] tokens = value.split(",");
            for (String t : tokens) {
                t = t.trim();
                if (Language.isLocaleDescriptionValid(t)) {
                    if (clean.length() > 0) {
                        clean.append(',');
                        if (formatForDisplay) {
                            clean.append(' ');
                        }
                    }
                    clean.append(t);
                }
            }
        }
        return clean.toString();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel advanced;
    private javax.swing.JCheckBox autotouchCheck;
    private javax.swing.JComboBox buildCombo;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel cardPanel;
    private javax.swing.JTextField catCreditsField;
    private ca.cgjennings.ui.textedit.JSourceCodeEditor catDescField;
    private javax.swing.JComboBox catHiddenCombo;
    private javax.swing.JTextField catHomeField;
    private ca.cgjennings.ui.JHelpButton catIDhelp;
    private ca.cgjennings.ui.textedit.JSourceCodeEditor catLibDescField;
    private javax.swing.JTextField catLibNameField;
    private javax.swing.JTextField catNameField;
    private ca.cgjennings.apps.arkham.plugins.catalog.CatalogIDListPanel catReplacesTable;
    private ca.cgjennings.apps.arkham.plugins.catalog.CatalogIDListPanel catRequiresTable;
    private javax.swing.JPanel catalogDescPanel;
    private javax.swing.JButton copyIDBtn;
    private javax.swing.JTextField coreField;
    private ca.cgjennings.ui.textedit.JSourceCodeEditor editor;
    private ca.cgjennings.ui.JGameCombo gameCombo;
    private javax.swing.JTextField gameField;
    private javax.swing.JTextField gameLangField;
    private javax.swing.JButton genIDBtn;
    private javax.swing.JTextField idVersionField;
    private javax.swing.JLabel idVersionLabel;
    private javax.swing.JComboBox installScript;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private ca.cgjennings.ui.JTip jTip1;
    private ca.cgjennings.ui.JTip jTip2;
    private ca.cgjennings.ui.JTip jTip3;
    private ca.cgjennings.ui.JTip jTip4;
    private ca.cgjennings.ui.JTip jTip5;
    private javax.swing.JComboBox languageCombo;
    private javax.swing.JLabel languageLabel;
    private ca.cgjennings.apps.arkham.project.ImageResourceCombo libIconCombo;
    private javax.swing.JPanel libraryDescPanel;
    private javax.swing.JButton okBtn;
    private javax.swing.JComboBox priorityCombo;
    private javax.swing.JList<Entry> scriptList;
    private javax.swing.JPanel simple;
    private javax.swing.JTabbedPane simpleTabs;
    private javax.swing.JButton switchBtn;
    private javax.swing.JTextField tagsField;
    private javax.swing.JButton touchBtn;
    private javax.swing.JTextField uiLangField;
    private javax.swing.JTextField uuidField;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent event) {
        if (simpleView) {
            editor.setText(pluginRoot.toString());
        }
        String text = editor.getText();
        pluginRoot = new PluginRoot(text);

        String[] errors = pluginRoot.getErrors();
        if (errors.length > 0) {
            Object[] message = new Object[2];
            message[0] = string("prj-re-warn-errs");
            StringBuilder b = new StringBuilder(256);
            for (int i = 0; i < errors.length; ++i) {
                if (i > 0) {
                    b.append('\n');
                }
                b.append(errors[i]);
            }
            JTextArea errs = new JTextArea(b.toString(), 10, 30);
            errs.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            errs.setEditable(false);
            errs.select(0, 0);
            message[1] = errs;
            if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                    this, message, string("prj-re-title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            )) {
                return;
            }
        }

        try {
            ProjectUtilities.copyReader(new StringReader(text), file, ProjectUtilities.ENC_UTF8);
            dispose();
        } catch (IOException e) {
            ErrorDialog.displayError(string("prj-err-write", file.getName()), e);
        }
    }

    @Override
    public void handleCancelAction(ActionEvent event) {
        dispose();
    }

    private void customizeTagsField() {
        IconBorder b = new IconBorder(ResourceKit.getIcon("ui/controls/drop-field.png"), true);
        final JPopupMenu menu = new JPopupMenu();
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Set<String> tags = fieldListToSet(tagsField);
                for (int i = 0; i < menu.getComponentCount(); ++i) {
                    Component c = menu.getComponent(i);
                    if (c instanceof JCheckBoxMenuItem) {
                        JCheckBoxMenuItem item = (JCheckBoxMenuItem) c;
                        String tag = item.getClientProperty("tag").toString();
                        item.setSelected(tags.contains(tag));
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        ActionListener filter = (ActionEvent e) -> {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            Set<String> tags = fieldListToSet(tagsField);
            String tag = item.getClientProperty("tag").toString();
            if (tags.contains(tag)) {
                tags.remove(tag);
            } else {
                tags.add(tag);
            }
            setToFieldList(tagsField, tags);
        };

        tagItem(menu, "cat-filter-game", "game", filter);
        tagItem(menu, "cat-filter-exp", "expansion", filter);
        tagItem(menu, "cat-filter-gc", "component", filter);
        menu.addSeparator();
        tagItem(menu, "cat-filter-deck", "deck", filter);
        tagItem(menu, "cat-filter-proj", "project", filter);
        menu.addSeparator();
        tagItem(menu, "cat-filter-tool", "tool", filter);
        tagItem(menu, "cat-filter-ref", "reference", filter);
        tagItem(menu, "cat-filter-docs", "docs", filter);
        menu.addSeparator();
        tagItem(menu, "cat-filter-lib", "library", filter);
        menu.addSeparator();
        tagItem(menu, "cat-filter-alpha", "alpha", filter);
        tagItem(menu, "cat-filter-beta", "beta", filter);

        ActionListener li = (ActionEvent e) -> {
            int x1 = tagsField.getWidth() - menu.getPreferredSize().width;
            int y1 = tagsField.getHeight();
            menu.show(tagsField, x1, y1);
        };

        b.installClickable(tagsField, ResourceKit.getIcon("ui/controls/drop-field-hi.png"), li, null);
    }

    private void tagItem(JPopupMenu menu, String labelKey, String tag, ActionListener li) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(string(labelKey));
        item.putClientProperty("tag", tag);
        item.addActionListener(li);
        menu.add(item);
    }

    private static Set<String> fieldListToSet(JTextField field) {
        String[] tokens = field.getText().split("\\s*,\\s*");
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String t : tokens) {
            t = t.trim();
            if (!t.isEmpty()) {
                tags.add(t);
            }
        }
        return tags;
    }

    private static void setToFieldList(JTextField field, Set<String> set) {
        StringBuilder b = new StringBuilder(field.getDocument().getLength() + 16);
        for (String token : set) {
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(token);
        }
        field.setText(b.toString());
        field.select(0, 0);
    }

    private class Renderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Icon i;
            Entry e = (Entry) value;
            setEnabled(e.valid);
            if (e.type == 0) {
                if (e.valid) {
                    i = MetadataSource.ICON_SCRIPT;
                } else {
                    i = greyScript;
                }
            } else if (e.type == 1) {
                i = MetadataSource.ICON_CLASS;
            } else {
                i = blankIcon;
            }

            setIcon(i);
            setToolTipText(e.name);
            return this;
        }
    }

    private static class Entry {

        String name, decname;
        int type;
        boolean valid;

        public Entry() {
            name = decname = string("prj-re-no-script");
            type = 2;
            valid = true;
        }

        public Entry(File f, String name) {
            this(f, name, false);
        }

        public Entry(File f, String name, boolean isInstallType) {
            boolean isClass = name.endsWith(".class") || name.endsWith(".java");
            if (isClass) {
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length() - ".class".length());
                } else {
                    name = name.substring(0, name.length() - ".java".length());
                }
            }
            type = isClass ? 1 : 0;

            if (isClass) {
                // convert to package syntax
                name = name.replace('/', '.').replace('$', '.');
                decname = name;
                valid = true;
            } else {
                int split = name.lastIndexOf('/') + 1;
                if (split >= 0 && split < name.length() - 1) {
                    decname = name.substring(split);
                } else {
                    decname = name;
                }
                valid = false;
                if (f != null) {
                    try {
                        String code = ProjectUtilities.getFileAsString(f, ProjectUtilities.ENC_UTF8);
                        if (isInstallType) {
                            valid = code.contains("install") && code.contains("uninstall");
                        } else {
                            valid = code.contains("getName");
                        }
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.WARNING, "exception while checking script for plug-in functions", e);
                    }
                }
            }
            this.name = PluginRoot.decoratePluginIdentifier(name);
        }

        public String getCode() {
            return name;
        }

        @Override
        public String toString() {
            return decname;
        }
    }

    /**
     * If this setting key is set to <code>true</code>, the editor won't
     * automatically generate a catalogue ID when you open a root file that
     * doesn't have one.
     */
    public static final String KEY_NO_AUTOMATIC_ID = "rooted-no-catid-gen";
}
