package ca.cgjennings.graphics.cloudfonts;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.InsertCharsDialog;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.FilteredListModel;
import ca.cgjennings.ui.theme.Palette;

import java.awt.CardLayout;

import static resources.Language.string;

import java.util.Arrays;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;

/**
 * A panel that allows the user to browse and select cloud fonts.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class CloudFontExplorerPanel extends javax.swing.JPanel {
    private static final float PREVIEW_SIZE = 24f;

    /**
     * Creates new form CloudFontExplorerPanel
     */
    public CloudFontExplorerPanel() {
        initComponents();
        error(null); // init font colour and show empty panel
        setPreviewText(previewLabel.getFont());
        showCard("init");
        initFiltering();
        matchesLabel.setText(null);
    }
    
    private boolean fontListInitializeStarted = false;
    private boolean fontListInitializeFinished = false;
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // only initialize the fonts (shortly after) the panel is for sure
        // actually being shown
        if (!fontListInitializeStarted) {
            initializeFontList();
        }
    }
    
    private void initializeFontList() {
        if (fontListInitializeStarted) return;
        fontListInitializeStarted = true;
        var t = new Timer(500, e -> CloudFontExplorerPanel.this.createFontList());
        t.setRepeats(false);
        t.start();
    }

    /**
     * Sets whether the user is allowed to select multiple font families at once.
     * @param enabled true to allow multiple selection
     * @see #isMultipleSelectionEnabled()
     * @see #getSelectedFamilies()
     */
    public void setMultipleSelectionEnabled(boolean enabled) {
        familyList.setSelectionMode(
            enabled ? javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : javax.swing.ListSelectionModel.SINGLE_SELECTION
        );
    }

    /**
     * Returns whether the user is allowed to select multiple font families at once.
     * @return true if multiple selection is enabled
     * @see #setMultipleSelectionEnabled(boolean)
     */
    public boolean isMultipleSelectionEnabled() {
        return familyList.getSelectionMode() == javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    }

    /**
     * Returns the currently selected font families.
     * @return a list of selected families, or an empty list if no families are selected
     */
    public List<CloudFontFamily> getSelectedFamilies() {
        return familyList.getSelectedValuesList();
    }

    /**
     * Adds a listener that is notified when the user's selected font families change.
     * @param l the listener to add
     * @see #removeFamilySelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void addFamilySelectionListener(javax.swing.event.ListSelectionListener l) {
        familyList.addListSelectionListener(l);
    }

    /**
     * Removes a font family selection listener.
     * @param l the listener to remove
     * @see #addFamilySelectionListener(javax.swing.event.ListSelectionListener)
     */
    public void removeFamilySelectionListener(javax.swing.event.ListSelectionListener l) {
        familyList.removeListSelectionListener(l);
    }

    /**
     * Updates the font list to reflect the current status of any listed fonts.
     * This should be called if fonts are registered or loaded from outside of the panel.
     */
    public void updateFontStatus() {
        familyList.repaint();
    }

    private void initFiltering() {
        fontFilter.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateListFilter();
            }
        });
        ActionListener al = e -> updateListFilter();
        filterCheckDisplay.addActionListener(al);
        filterCheckHandwriting.addActionListener(al);
        filterCheckMono.addActionListener(al);
        filterCheckSans.addActionListener(al);
        filterCheckSerif.addActionListener(al);
        filterCheckSymbols.addActionListener(al);
        filterCheckReserved.addActionListener(al);
    }
    
    private void updateListFilter() {
        if (!fontListInitializeFinished) return;

        boolean mustBeReserved = filterCheckReserved.isSelected();
        List<String> subsets = subsetList.getSelectedValuesList();
        List<CloudFontFamily> stash = FilteredListModel.stashSelection(familyList);
        List<String> subsetStash = FilteredListModel.stashSelection(subsetList);

        CategorySet cats = new CategorySet(
                filterCheckDisplay.isSelected(),
                filterCheckHandwriting.isSelected(),
                filterCheckMono.isSelected(),
                filterCheckSans.isSelected(),
                filterCheckSerif.isSelected(),
                filterCheckSymbols.isSelected(),
                false
        );

        String query = fontFilter.getText().trim().replaceAll("\\s+", " ");
        Pattern p = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
        filterModel.setFilter((model, item) -> {
            CloudFontFamily cff = (CloudFontFamily) item;
            if (mustBeReserved && !CloudFonts.isReservedFamily(cff)) {
                return false;
            }            
            if (cats.hasEmptyIntersectionWith(cff.getCategories())) {
                return false;
            }
            if (!subsets.isEmpty()) {
                for (String subset : subsets) {
                    if (!cff.hasSubset(subset)) {
                        return false;
                    }
                }
            }
            return p.matcher(cff.getName()).find();
        });
        FilteredListModel.restoreSelection(subsetList, subsetStash);
        FilteredListModel.restoreSelection(familyList, stash);
        matchesLabel.setText(filterModel.getSize() + "/" + filterModel.getItemCount());
    }

    private static String escape(String s) {
        return "<html>" + s.replace("<", "&lt;").replace("\n", "<br>");
    }
    
    private void error(Exception ex) {
        if (ex == null) {
            infoLabel.getFont().deriveFont(
                    Collections.singletonMap(TextAttribute.FOREGROUND, Palette.get.foreground.opaque.red)
            );
            errorText.setText(null);
            showCard("error");
            return;
        }
        StrangeEons.log.log(Level.WARNING, "error accessing cloud font", ex);
        showCard("error");
        errorText.setText(escape(string("clf-error", ex.getMessage())));
    }
    
    private void setPreviewText(Font font) {
        if (font == null) {
            previewLabel.setText(null);  
            return;
        }
        previewLabel.setFont(font);

        String text = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz0123456789.,!;:@#$%^&*()-+[]{}";
        StringBuilder latinCore = new StringBuilder(text.length()*2 + 6);
        StringBuilder nonLatinCore = new StringBuilder(512);
        
        if (font.canDisplay('A')) {
            for (int i=0; i<text.length(); ++i) {
                char ch = text.charAt(i);
                if (font.canDisplay(ch)) {
                    latinCore.append(ch);
                }
            }
        }

        // find letters up to the end of the 0xffee; separating Latin and other
        // so that we can put Latin at the end when it doesn't dominate the font
        for (int cp = 0x391; cp < 0xe000; ++cp) {
            if (Character.isLetter(cp) && font.canDisplay(cp)) {
                var block = Character.UnicodeBlock.of(cp);
                if (
                    block == Character.UnicodeBlock.BASIC_LATIN // impossible, but kept for clarity
                    || block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT
                    || block == Character.UnicodeBlock.LATIN_EXTENDED_A
                    || block == Character.UnicodeBlock.LATIN_EXTENDED_B
                    || block == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL
                ) {
                    latinCore.appendCodePoint(cp);
                } else {
                    nonLatinCore.appendCodePoint(cp);
                }
            }
        }

        // add basic PUA charcaters
        for (int cp = 0xe000; cp < 0xf8ff; ++cp) {
            if (font.canDisplay(cp)) {
                nonLatinCore.appendCodePoint(cp);
            }
        }

        String previewText;
        if (nonLatinCore.length() > (latinCore.length() * 3/2) && latinCore.length() < 200) {
            nonLatinCore.append(latinCore);
            previewText = nonLatinCore.toString();
        } else {
            latinCore.append(nonLatinCore);
            previewText = latinCore.toString();
        }
        previewLabel.setText(previewText);
        previewLabel.setCaretPosition(0);
    }
    
    private void createFontList() {
        showWaitCursor();
        Thread loader = new Thread(()->{
            try {
                StrangeEons.log.info("started background listing cloud fonts in background");
                if (collection == null) {
                    collection = CloudFonts.getDefaultCollection();
                }
                var cff = collection.getFamilies();
                
                FilteredListModel<CloudFontFamily> newModel = new FilteredListModel<>(cff);
                EventQueue.invokeLater(()->{
                    fontListInitializeFinished = true;
                    filterModel = newModel;
                    familyList.setModel(filterModel);
                    fontFilter.setEnabled(true);
                    familyList.setEnabled(true);
                    updateListFilter();
                });                
            } catch (IOException ex) {
                error(ex);
            } finally {
                EventQueue.invokeLater(this::hideWaitCursor);
            }
        });
        loader.setDaemon(true);
        loader.start();
    }

    private Font getSelectedFont() {
        CloudFontFamily cff = familyList.getSelectedValue();
        if (cff == null) return null;
        CloudFont[] cFonts = cff.getCloudFonts();
        if (cFonts.length > 0) {
            // return immediately if available, otherwise download
            CloudFont cf = cFonts[0];
            if (cf.isDownloaded()) {
                try {
                    return cf.getFont().deriveFont(PREVIEW_SIZE);
                } catch (IOException ex) {
                    error(ex);
                    return null;
                }
            }
            
            showWaitCursor();
            try {
                return cf.getFont().deriveFont(PREVIEW_SIZE);
            } catch (IOException ex) {
                StrangeEons.log.log(Level.WARNING, "could not load font", ex);
                error(ex);
            } finally {
                hideWaitCursor();
            }
        }        
        return null;
    }    
    
    private CloudFontCollection collection;
    private FilteredListModel<CloudFontFamily> filterModel = new FilteredListModel<>();
    
    private int waitNest = 0;
    
    public void showWaitCursor() {
        if (waitNest > 0) return;
        ++waitNest;
        var root = getRootPane();
        if (root != null) {
            var glass = root.getGlassPane();
            glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            glass.setVisible(true);
        }
    }
    
    public void hideWaitCursor() {
        if (waitNest != 1) return;
        --waitNest;
        var root = getRootPane();
        if (root != null) {
            var glass = root.getGlassPane();
            glass.setVisible(false);
            glass.setCursor(null);
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        categoryPanel = new javax.swing.JPanel();
        filterCheckSans = new javax.swing.JCheckBox();
        filterCheckDisplay = new javax.swing.JCheckBox();
        filterCheckSymbols = new javax.swing.JCheckBox();
        filterCheckHandwriting = new javax.swing.JCheckBox();
        filterCheckSerif = new javax.swing.JCheckBox();
        filterCheckMono = new javax.swing.JCheckBox();
        filterAll = new javax.swing.JButton();
        filterNone = new javax.swing.JButton();
        matchesLabel = new javax.swing.JLabel();
        fontFilter = new ca.cgjennings.ui.JFilterField();
        familyScroll = new javax.swing.JScrollPane();
        familyList = new ca.cgjennings.ui.JIconList<>();
        openFontViewerBtn = new javax.swing.JButton();
        previewScroll = new javax.swing.JScrollPane();
        previewLabel = new javax.swing.JTextArea();
        infoPanel = new javax.swing.JPanel();
        errorPanel = new javax.swing.JPanel();
        errorText = new javax.swing.JLabel();
        infoPage = new javax.swing.JPanel();
        infoLabel = new javax.swing.JLabel();
        licenseLabel = new javax.swing.JLabel();
        stylesLabel = new javax.swing.JLabel();
        stylesCount = new javax.swing.JLabel();
        javax.swing.JScrollPane subsetScroll = new javax.swing.JScrollPane();
        subsetList = new javax.swing.JList<>();
        filterCheckReserved = new javax.swing.JCheckBox();

        categoryPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("clf-cat-title"))); // NOI18N
        categoryPanel.setName("categoryPanel"); // NOI18N

        filterCheckSans.setFont(filterCheckSans.getFont().deriveFont(filterCheckSans.getFont().getSize()-2f));
        filterCheckSans.setSelected(true);
        filterCheckSans.setText(string("clf-cat-sans")); // NOI18N
        filterCheckSans.setName("filterCheckSans"); // NOI18N

        filterCheckDisplay.setFont(filterCheckDisplay.getFont().deriveFont(filterCheckDisplay.getFont().getSize()-2f));
        filterCheckDisplay.setSelected(true);
        filterCheckDisplay.setText(string("clf-cat-display")); // NOI18N
        filterCheckDisplay.setName("filterCheckDisplay"); // NOI18N

        filterCheckSymbols.setFont(filterCheckSymbols.getFont().deriveFont(filterCheckSymbols.getFont().getSize()-2f));
        filterCheckSymbols.setSelected(true);
        filterCheckSymbols.setText(string("clf-cat-symbols")); // NOI18N
        filterCheckSymbols.setName("filterCheckSymbols"); // NOI18N

        filterCheckHandwriting.setFont(filterCheckHandwriting.getFont().deriveFont(filterCheckHandwriting.getFont().getSize()-2f));
        filterCheckHandwriting.setSelected(true);
        filterCheckHandwriting.setText(string("clf-cat-handwriting")); // NOI18N
        filterCheckHandwriting.setName("filterCheckHandwriting"); // NOI18N

        filterCheckSerif.setFont(filterCheckSerif.getFont().deriveFont(filterCheckSerif.getFont().getSize()-2f));
        filterCheckSerif.setSelected(true);
        filterCheckSerif.setText(string("clf-cat-serif")); // NOI18N
        filterCheckSerif.setName("filterCheckSerif"); // NOI18N

        filterCheckMono.setFont(filterCheckMono.getFont().deriveFont(filterCheckMono.getFont().getSize()-2f));
        filterCheckMono.setSelected(true);
        filterCheckMono.setText(string("clf-cat-monospace")); // NOI18N
        filterCheckMono.setName("filterCheckMono"); // NOI18N

        filterAll.setFont(filterAll.getFont().deriveFont(filterAll.getFont().getSize()-2f));
        filterAll.setText(string("clf-cat-all")); // NOI18N
        filterAll.setName("filterAll"); // NOI18N
        filterAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterAllActionPerformed(evt);
            }
        });

        filterNone.setFont(filterNone.getFont().deriveFont(filterNone.getFont().getSize()-2f));
        filterNone.setText(string("clf-cat-none")); // NOI18N
        filterNone.setName("filterNone"); // NOI18N
        filterNone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterNoneActionPerformed(evt);
            }
        });

        matchesLabel.setFont(matchesLabel.getFont().deriveFont(matchesLabel.getFont().getSize()-2f));
        matchesLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        matchesLabel.setText("9999/9999");
        matchesLabel.setName("matchesLabel"); // NOI18N

        javax.swing.GroupLayout categoryPanelLayout = new javax.swing.GroupLayout(categoryPanel);
        categoryPanel.setLayout(categoryPanelLayout);
        categoryPanelLayout.setHorizontalGroup(
            categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(categoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(categoryPanelLayout.createSequentialGroup()
                        .addGroup(categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(filterCheckMono)
                            .addComponent(filterCheckSans)
                            .addComponent(filterCheckSerif))
                        .addGap(18, 18, 18)
                        .addGroup(categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(filterCheckDisplay)
                            .addComponent(filterCheckHandwriting)
                            .addComponent(filterCheckSymbols))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(categoryPanelLayout.createSequentialGroup()
                        .addComponent(filterAll)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterNone)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(matchesLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        categoryPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {filterAll, filterNone});

        categoryPanelLayout.setVerticalGroup(
            categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, categoryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterCheckSerif)
                    .addComponent(filterCheckDisplay))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterCheckSans)
                    .addComponent(filterCheckHandwriting))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterCheckMono)
                    .addComponent(filterCheckSymbols))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(categoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterAll)
                    .addComponent(filterNone)
                    .addComponent(matchesLabel))
                .addContainerGap())
        );

        matchesLabel.getAccessibleContext().setAccessibleName("");

        fontFilter.setName("fontFilter"); // NOI18N

        familyScroll.setName("familyScroll"); // NOI18N

        familyList.setModel(filterModel);
        familyList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        familyList.setName("familyList"); // NOI18N
        familyList.setVisibleRowCount(0);
        familyList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                familyListValueChanged(evt);
            }
        });
        familyScroll.setViewportView(familyList);

        openFontViewerBtn.setFont(openFontViewerBtn.getFont().deriveFont(openFontViewerBtn.getFont().getSize()-2f));
        openFontViewerBtn.setText(string("clf-view")); // NOI18N
        openFontViewerBtn.setName("openFontViewerBtn"); // NOI18N
        openFontViewerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFontViewerBtnActionPerformed(evt);
            }
        });

        previewScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        previewScroll.setName("previewScroll"); // NOI18N

        previewLabel.setFont(previewLabel.getFont().deriveFont(PREVIEW_SIZE));
        previewLabel.setLineWrap(true);
        previewLabel.setTabSize(4);
        previewLabel.setName("previewLabel"); // NOI18N
        previewScroll.setViewportView(previewLabel);

        infoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("clf-info"))); // NOI18N
        infoPanel.setName("infoPanel"); // NOI18N
        infoPanel.setLayout(new java.awt.CardLayout());

        errorPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        errorPanel.setName("errorPanel"); // NOI18N

        errorText.setForeground(Palette.get.foreground.opaque.red);
        errorText.setName("errorText"); // NOI18N

        javax.swing.GroupLayout errorPanelLayout = new javax.swing.GroupLayout(errorPanel);
        errorPanel.setLayout(errorPanelLayout);
        errorPanelLayout.setHorizontalGroup(
            errorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(errorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(errorText, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addContainerGap())
        );
        errorPanelLayout.setVerticalGroup(
            errorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(errorPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(errorText, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE)
                .addContainerGap())
        );

        infoPanel.add(errorPanel, "error");

        infoPage.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        infoPage.setName("infoPage"); // NOI18N

        infoLabel.setText("name");
        infoLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        infoLabel.setName("infoLabel"); // NOI18N

        licenseLabel.setText("license");
        licenseLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        licenseLabel.setName("licenseLabel"); // NOI18N

        stylesLabel.setText(string("clf-styles-info")); // NOI18N
        stylesLabel.setName("stylesLabel"); // NOI18N

        stylesCount.setText("0");
        stylesCount.setName("stylesCount"); // NOI18N

        subsetScroll.setName("subsetScroll"); // NOI18N

        subsetList.setFont(subsetList.getFont().deriveFont(subsetList.getFont().getSize()-2f));
        subsetList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        subsetList.setName("subsetList"); // NOI18N
        subsetList.setVisibleRowCount(-1);
        subsetList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                subsetListValueChanged(evt);
            }
        });
        subsetScroll.setViewportView(subsetList);

        javax.swing.GroupLayout infoPageLayout = new javax.swing.GroupLayout(infoPage);
        infoPage.setLayout(infoPageLayout);
        infoPageLayout.setHorizontalGroup(
            infoPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPageLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(infoPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(infoPageLayout.createSequentialGroup()
                        .addComponent(stylesLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stylesCount, javax.swing.GroupLayout.DEFAULT_SIZE, 86, Short.MAX_VALUE))
                    .addComponent(licenseLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                    .addComponent(infoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(subsetScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                .addContainerGap())
        );
        infoPageLayout.setVerticalGroup(
            infoPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPageLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(infoPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(infoPageLayout.createSequentialGroup()
                        .addComponent(infoLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(licenseLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(infoPageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(stylesLabel)
                            .addComponent(stylesCount))
                        .addContainerGap(34, Short.MAX_VALUE))
                    .addComponent(subsetScroll)))
        );

        infoPanel.add(infoPage, "info");

        filterCheckReserved.setFont(filterCheckReserved.getFont().deriveFont(filterCheckReserved.getFont().getSize()-2f));
        filterCheckReserved.setText(string("clf-cat-reserved")); // NOI18N
        filterCheckReserved.setName("filterCheckReserved"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(familyScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .addComponent(fontFilter, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE)
                    .addComponent(categoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(filterCheckReserved)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(previewScroll)
                            .addComponent(infoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(openFontViewerBtn)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(categoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(infoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filterCheckReserved)
                    .addComponent(openFontViewerBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fontFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(familyScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 34, Short.MAX_VALUE))
                    .addComponent(previewScroll))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {fontFilter, openFontViewerBtn});

    }// </editor-fold>//GEN-END:initComponents

    private void familyListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_familyListValueChanged
        CloudFontFamily cff = familyList.getSelectedValue();
        if (cff != null) {
            Font aFont = getSelectedFont();
            if (aFont != null) {
                setPreviewText(aFont);
            } else {
                previewLabel.setText("");
            }
            
            // calc num styles as
            //     number of fonts * number of standard variable axes            
            int styles = cff.getCloudFonts().length;
            int standardAxes = 1;
            for (Axis a : cff.getAxes()) {
                String tag = a.tag;
                if (tag.equals("wght") || tag.equals("wdth")
                        || tag.equals("ital") || tag.equals("slnt")
                        || tag.equals("opsz") // doubt opsz actually supported
                ) {
                    ++standardAxes;
                }
            }
            styles *= standardAxes;
             
            infoLabel.setText(cff.getName());
            licenseLabel.setText(cff.getLicenseType());
            stylesCount.setText(String.valueOf(styles));
            DefaultListModel<String> subsets = new DefaultListModel<>();
            subsets.addAll(Arrays.asList(cff.getSubsets()));            
            subsetList.setModel(subsets);
            showCard("info");
        }
    }//GEN-LAST:event_familyListValueChanged

    private void openFontViewerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFontViewerBtnActionPerformed
        Font f = getSelectedFont();
        if (f != null) {
            java.awt.Window w = SwingUtilities.getWindowAncestor(this);
            InsertCharsDialog d;
            if (w instanceof java.awt.Dialog) {
                d = InsertCharsDialog.createFontViewer((java.awt.Dialog) w, f);
            } else {
                d = InsertCharsDialog.createFontViewer(f);
            }
            d.setLocationRelativeTo(openFontViewerBtn);
            d.setVisible(true);
        }
    }//GEN-LAST:event_openFontViewerBtnActionPerformed

    private void filterAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterAllActionPerformed
        setAllCategoryChecks(true);
    }//GEN-LAST:event_filterAllActionPerformed

    private void filterNoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterNoneActionPerformed
        setAllCategoryChecks(false);
        subsetList.setSelectedValue(null, false);
    }//GEN-LAST:event_filterNoneActionPerformed

    private void subsetListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_subsetListValueChanged
        updateListFilter();
    }//GEN-LAST:event_subsetListValueChanged

    private void setAllCategoryChecks(boolean set) {
        filterCheckDisplay.setSelected(set);
        filterCheckHandwriting.setSelected(set);
        filterCheckSymbols.setSelected(set);
        filterCheckSans.setSelected(set);
        filterCheckSerif.setSelected(set);
        filterCheckMono.setSelected(set);
        updateListFilter();
    }
    
    private void showCard(String cardName) {
        ((CardLayout) infoPanel.getLayout()).show(infoPanel, cardName);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel categoryPanel;
    private javax.swing.JPanel errorPanel;
    private javax.swing.JLabel errorText;
    private javax.swing.JList<CloudFontFamily> familyList;
    private javax.swing.JScrollPane familyScroll;
    private javax.swing.JButton filterAll;
    private javax.swing.JCheckBox filterCheckDisplay;
    private javax.swing.JCheckBox filterCheckHandwriting;
    private javax.swing.JCheckBox filterCheckMono;
    private javax.swing.JCheckBox filterCheckReserved;
    private javax.swing.JCheckBox filterCheckSans;
    private javax.swing.JCheckBox filterCheckSerif;
    private javax.swing.JCheckBox filterCheckSymbols;
    private javax.swing.JButton filterNone;
    private ca.cgjennings.ui.JFilterField fontFilter;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JPanel infoPage;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JLabel licenseLabel;
    private javax.swing.JLabel matchesLabel;
    private javax.swing.JButton openFontViewerBtn;
    private javax.swing.JTextArea previewLabel;
    private javax.swing.JScrollPane previewScroll;
    private javax.swing.JLabel stylesCount;
    private javax.swing.JLabel stylesLabel;
    private javax.swing.JList<String> subsetList;
    // End of variables declaration//GEN-END:variables
}
