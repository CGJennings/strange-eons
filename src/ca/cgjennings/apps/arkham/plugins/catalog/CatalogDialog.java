package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog.VersioningState;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.AutocompletionDocument;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.EditorPane;
import ca.cgjennings.ui.IconBorder;
import ca.cgjennings.ui.JLabelledField;
import ca.cgjennings.ui.dnd.ScrapBook;
import ca.cgjennings.ui.table.BooleanRenderer;
import ca.cgjennings.ui.table.IconRenderer;
import gamedata.Game;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.RowFilter.Entry;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.text.html.HTMLDocument;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A dialog that can download, display, and install plug-ins from catalogues
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class CatalogDialog extends javax.swing.JDialog implements AgnosticDialog {
    /** Property that changes when the dialog switches to a new catalog. */
    public static final String CATALOG_PROPERTY = "catalog";

    /**
     * The string "eonscat:", the prefix that marks a Strange Eons <i>cat
     * link</i>. A cat link consists of this prefix followed the text of a
     * catalog search filter. When Strange Eons gains focus and a cat link is on
     * the clipboard, a catalog dialog showing the results of the specified
     * search filter will be displayed. For example, putting the following text
     * on the clipboard (or system selection on *NIX) would filter out
     * everything but the
     * <b>Developer Tools</b> plug-in (since it has the specified UUID):<br>
     * {@code eonscat:39f10fa9-6574-4be1-9dd6-3e658c9a6fd3}
     */
    public static final String CATLINK_PREFIX = "eonscat:";

    /** Empty catalog used whenever a valid catalog has not or could not be loaded. */
    private final Catalog placeholderCatalog = new Catalog();
    private Catalog catalog = placeholderCatalog;
    private boolean doneInit = false;

    /**
     * Creates a new dialog for downloading plug-ins.
     *
     * @param parent the parent window, typically the application window
     */
    public CatalogDialog(java.awt.Frame parent) {
        this(parent, null, true);
    }

    /**
     * Creates a new dialog for downloading plug-ins.
     *
     * @param parent the parent window, typically the application window
     * @param defaultCatalogToOpen catalog to open, or {@code null} for the
     * default catalog
     */
    public CatalogDialog(java.awt.Frame parent, URL defaultCatalogToOpen) {
        this(parent, defaultCatalogToOpen, true);
    }

    /**
     * Creates a new dialog for downloading plug-ins.
     *
     * @param parent the parent window, typically the application window
     * @param defaultCatalogToOpen catalog to open, or {@code null} for the
     * default catalog
     * @param allowCache a hint passed on to the catalogue that suggests whether
     * it should allow cached versions of the catalogue
     */
    public CatalogDialog(java.awt.Frame parent, URL defaultCatalogToOpen, boolean allowCache) {
        super(parent, true);

        if (allowCache && getCatalogSearchClip() != null) {
            allowCache = false;
        }

        allowCacheHint = allowCache;
        initComponents();
        findPanel.setBackground(filterField.getBackground());
        initFilterMenu();

        // TODO: installs when hitting enter in URL combo
        //getRootPane().setDefaultButton( okBtn );
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        table.setModel(new Model());

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // the presence of popup text overrides a search clip
                if (popupText != null) {
                    showPopupText();
                } else if (!hasExplicitFilter) {
                    String search = getCatalogSearchClip();
                    if (search != null) {
                        String s = ScrapBook.getSystemSelectionText();
                        if (s != null && s.trim().startsWith(CATLINK_PREFIX)) {
                            ScrapBook.setSystemSelectionText("");
                        } else {
                            ScrapBook.setText("");
                        }
                        filterField.setText(search);
                        // pretend the filter was set explicitly so we request focus
                        hasExplicitFilter = true;
                    }
                }
                if (hasExplicitFilter) {
                    filterField.selectAll();
                    filterField.requestFocusInWindow();
                }
                hasExplicitFilter = false;
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });

        new IconBorder(ResourceKit.getIcon("ui/controls/url-field.png")).install(urlCombo);

        showListing(-1);
        table.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (catalog == null) {
                showListing(-1);
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0) {
                row = table.convertRowIndexToModel(row);
            }
            showListing(row);
        });

        table.setDefaultRenderer(Icon.class, new IconRenderer());
        table.setDefaultRenderer(Boolean.class, new BooleanRenderer(true));

        rowSorter = new TableRowSorter(table.getModel());
        rowSorter.setComparator(COL_ICON, new Comparator<Icon>() {
            public int iconToInteger(Icon i) {
                int v = -1;
                if (i == ICON_INSTALLED_IS_NEWER) {
                    v = 10;
                } else if (i == CORE_ICON_INSTALLED_IS_NEWER) {
                    v = 15;
                } else if (i == ICON_NEW_PLUGIN) {
                    v = 20;
                } else if (i == CORE_ICON_NEW_PLUGIN) {
                    v = 25;
                } else if (i == ICON_APP_UPDATE) {
                    v = 30;
                } else if (i == CORE_ICON_APP_UPDATE) {
                    v = 35;
                } else if (i == ICON_UPDATE_AVAILABLE) {
                    v = 40;
                } else if (i == CORE_ICON_UPDATE_AVAILABLE) {
                    v = 45;
                } else if (i == ICON_NOT_INSTALLED) {
                    v = 50;
                } else if (i == CORE_ICON_NOT_INSTALLED) {
                    v = 55;
                } else if (i == ICON_UP_TO_DATE) {
                    v = 60;
                } else if (i == CORE_ICON_UP_TO_DATE) {
                    v = 65;
                }
                return v;
            }

            @Override
            public int compare(Icon o1, Icon o2) {
                if (o1 == ICON_UP_TO_DATE) {
                    if (o1 == o2) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (o2 == ICON_UP_TO_DATE) {
                    return 1;
                }

                if (o1 == ICON_NOT_INSTALLED) {
                    if (o1 == o2) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (o2 == ICON_NOT_INSTALLED) {
                    return 1;
                }

                if (o1 == ICON_UPDATE_AVAILABLE) {
                    if (o1 == o2) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (o2 == ICON_UPDATE_AVAILABLE) {
                    return 1;
                }

                if (o1 == ICON_NEW_PLUGIN) {
                    if (o1 == o2) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (o2 == ICON_NEW_PLUGIN) {
                    return 1;
                }

                if (o1 == ICON_APP_UPDATE) {
                    if (o1 == o2) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (o2 == ICON_APP_UPDATE) {
                    return 1;
                }

                return 0;
            }
        });
        table.setRowSorter(rowSorter);
        rowSorter.setSortKeys(Collections.singletonList(new SortKey(COL_ICON, SortOrder.ASCENDING)));

        ((JLabelledField) filterField).setLabel(string("cat-filter"));
        filterField.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateRowFilter();
            }
        });

        // load catalog URLs from user settings
        AutocompletionDocument.install(urlCombo, false);
        DefaultComboBoxModel urlModel = new DefaultComboBoxModel();
        Settings s = Settings.getUser();
        for (int i = 1; s.get("catalog-url-" + i) != null; ++i) {
            urlModel.addElement(s.get("catalog-url-" + i));
        }
        urlCombo.setModel(urlModel);
        urlCombo.setSelectedIndex(0);

        Insets ri = restartWarnLabel.getInsets();
        WARN_ACTIVE_BORDER = restartWarnLabel.getBorder();
        WARN_INACTIVE_BORDER = BorderFactory.createEmptyBorder(ri.top, ri.left, ri.bottom, ri.right);
        Dimension rd = restartWarnLabel.getPreferredSize();
        restartWarnLabel.setPreferredSize(rd);
        restartWarnLabel.setMinimumSize(rd);
        restartWarnLabel.setMaximumSize(rd);
        updateDownloadButtonText();

        showAdvCheck.setSelected(s.getYesNo("catalog-show-expert-info"));

        descPane.addHyperlinkListener((HyperlinkEvent e) -> {
            URL url = e.getURL();
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
                String ref = e.getDescription();
                if (ref.startsWith("#")) {
                    try {
                        UUID uuid = UUID.fromString(ref.substring(1));
                        int targetIndex = catalog.findListingByUUID(uuid);
                        if (targetIndex >= 0) {
                            int row = table.convertRowIndexToView(targetIndex);
                            if (row >= 0) {
                                table.clearSelection();
                                table.addRowSelectionInterval(row, row);
                                return;
                            }
                        }
                        UIManager.getLookAndFeel().provideErrorFeedback(descPane);
                        return;
                    } catch (IllegalArgumentException ex) {
                        StrangeEons.log.log(Level.WARNING, null, ex);
                    }
                    descPane.scrollToReference(ref.substring(1));
                    return;
                } else if (url != null) {
                    try {
                        DesktopIntegration.browse(url.toURI());
                        return;
                    } catch (Exception ex) {
                        StrangeEons.log.log(Level.WARNING, "Exception opening browser", ex);
                    }
                }
                UIManager.getLookAndFeel().provideErrorFeedback(descPane);
            }
        });

        // download default catalog
        doneInit = true;
        if (defaultCatalogToOpen != null) {
            urlCombo.setSelectedItem(defaultCatalogToOpen.toExternalForm());
        } else {
            urlComboActionPerformed(null);
        }

        if (!s.applyWindowSettings("plugin-catalog", this)) {
            setLocationRelativeTo(StrangeEons.getWindow());
        }

        AutomaticUpdater.markUpdate();
    }

    /**
     * Sets the current listing filter. This is a comma-separated list of search
     * tokens that can be edited by the user. Each token is matched against
     * either a default set of keys (including name, description, core, tags,
     * and id) or else against a specific tag. To match a specific tag, start
     * the token with the tag name followed by '='.
     *
     * Tokens that start with '!' will hide listings that match the rest of the
     * token. If all tokens start with '!', then all listings that are not
     * hidden will be shown. Otherwise, only listings that match the non-'!'
     * tokens and do not match the '!' tokens will be shown.
     *
     * @param filter the filter text
     */
    public void setListingFilter(String filter) {
        if (filter == null) {
            filter = "";
        }
        filterField.setText(filter);
        hasExplicitFilter = true;
    }
    private boolean hasExplicitFilter;

    /**
     * Returns the current filter being applied to catalogue listings. If no
     * filter is applied, an empty string is returned.
     *
     * @return the current listing filter
     * @see #setListingFilter
     */
    public String getListingFilter() {
        return filterField.getText();
    }

    /**
     * Selects all currently visible plug-ins for installation if possible.
     * Does not affect the selection state of any plug-ins that are filtered
     * out of the view.
     */
    public void selectFilteredListingsForInstallation() {
        if (catalog == placeholderCatalog) {
            addPropertyChangeListener(CATALOG_PROPERTY, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    selectFilteredListingsForInstallation();
                    CatalogDialog.this.removePropertyChangeListener(this);
                }
            });
        } else {
            selectAllItemActionPerformed(null);
        }
    }

    /**
     * Sets a text message that will be displayed the next time the this dialog
     * gains focus, or clears the current message if {@code null}. This can
     * be used to provide an explanatory message or additional help when the
     * dialog is being displayed under program control (for example, if the
     * program is specifying the filter text).
     */
    public void setPopupText(String popupText) {
        this.popupText = popupText;
    }
    private String popupText;

    /**
     * Called when the window gains focus if there is pop-up text. Shows the
     * text in a a dialog and resets the text to {@code null}.
     */
    private void showPopupText() {
        if (popupText != null && !popupText.isEmpty()) {
            JOptionPane.showMessageDialog(this, popupText, getTitle(), JOptionPane.INFORMATION_MESSAGE, ResourceKit.getIcon("application/lantern.png"));
        }
        popupText = null;
    }

    /**
     * Returns the current {@code eonscat:} link text that is on the
     * clipboard, or {@code null} if the clipboard is empty or not an
     * {@code eonscat:} link. On platforms with a system selection
     * (primarily those based on the X Window System), the system selection is
     * checked first. If there is no system selection or it does not contain a
     * link, then then system clipboard is checked.
     *
     * @return a catalogue link, without the {@link #CATLINK_PREFIX}, if one is
     * present in the system selection or clipboard; otherwise {@code null}
     */
    public static String getCatalogSearchClip() {
        String text = ScrapBook.getSystemSelectionText();
        if (text != null && (text = text.trim()).startsWith(CATLINK_PREFIX)) {
            return text.substring(CATLINK_PREFIX.length());
        }

        text = ScrapBook.getText();
        if (text != null && (text = text.trim()).startsWith(CATLINK_PREFIX)) {
            return text.substring(CATLINK_PREFIX.length());
        }

        return null;
    }

    private boolean allowCacheHint;
    private TableRowSorter rowSorter;

    private synchronized void downloadCatalog(final URL location, final boolean allowCache) {
        final Catalog oldCatalog = catalog == placeholderCatalog ? null : catalog;
        Thread checkThread = downloadThread;
        if (checkThread != null) {
            checkThread.interrupt();
            EventQueue.invokeLater(() -> {
                downloadCatalog(location, allowCache);
            });
            return;
        }
        dlProgress.setIndeterminate(true);
        showPanel("download");
        downloadThread = new Thread(() -> {
            try {
                final Catalog c = new Catalog(location, allowCache, dlProgress);
                EventQueue.invokeLater(() -> {
                    catalog = c;
                    catalogLoaded();
                    firePropertyChange(CATALOG_PROPERTY, oldCatalog, catalog);
                });
            } catch (final Throwable e) {
                EventQueue.invokeLater(() -> {
                    catalog = placeholderCatalog;
                    // make sure any selection in the table is cleared:
                    // although the table isn't visible, there might be
                    // a selection so you could install plug-ins you
                    // can't see
                    clearItemActionPerformed(null);
                    
                    if (e instanceof Catalog.CatalogIsLockedException) {
                        showPanel("locked");
                    } else {
                        String error = e.getClass().getSimpleName();
                        if (e.getLocalizedMessage() != null) {
                            error = "<html>" + error + "<br>" + ResourceKit.makeStringHTMLSafe(e.getLocalizedMessage());
                        }
                        downloadErrorLabel.setText(error);
                        showPanel("error");
                        StrangeEons.log.log(Level.INFO, "catalog download error", e);
                    }
                    if (oldCatalog != null) {
                        firePropertyChange(CATALOG_PROPERTY, oldCatalog, null);
                    }
                });
            } finally {
                downloadThread = null;
            }
        }, "Catalog Download Thread");
        downloadThread.start();
    }
    private volatile Thread downloadThread = null;

    private void catalogLoaded() {
        table.clearSelection();
        table.setModel(new Model());

        // init column widths
        TableColumn col = table.getColumnModel().getColumn(COL_INSTALL);
        int width = table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(
                table, col.getHeaderValue(), false, false, 0, COL_INSTALL
        ).getPreferredSize().width + 2;
        col.setPreferredWidth(width);
        col.setMaxWidth(width);
        col.setResizable(false);

        col = table.getColumnModel().getColumn(COL_ICON);
        col.setPreferredWidth(ICON_UP_TO_DATE.getIconWidth() + 2);
        col.setResizable(false);

        width += ICON_UP_TO_DATE.getIconWidth() + 2;
        col = table.getColumnModel().getColumn(COL_NAME);
        col.setPreferredWidth(table.getWidth() - width);

        // Check updated and core plug-ins
        boolean autoUpdate = Settings.getUser().getBoolean("catalog-autoselect-updated");
        boolean autoCore = Settings.getUser().getBoolean("catalog-autoselect-core");

        //  - create matchers for acceptable core types
        String[] coreTokens = Settings.getUser().get("catalog-autoselect-core-types", "*").split("\\s*,\\s*");
        Pattern[] cores = new Pattern[coreTokens.length];
        for (int i = 0; i < cores.length; ++i) {
            try {
                cores[i] = Pattern.compile(Pattern.quote(coreTokens[i]).replace("*", "\\E.*\\Q"), Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                StrangeEons.log.log(Level.SEVERE, coreTokens[i], e);
            }
        }

        //  - check each plug-in for an update or core type match
        for (int i = 0; i < catalog.size(); ++i) {
            Catalog.VersioningState s = catalog.getVersioningState(i);
            if (s == Catalog.VersioningState.OUT_OF_DATE || s == Catalog.VersioningState.OUT_OF_DATE_LEGACY) {
                if (autoUpdate) {
                    catalog.setInstallFlag(i, true);
                }
            } else if (s == Catalog.VersioningState.NOT_INSTALLED && autoCore && catalog.get(i).get(Listing.CORE) != null) {
                String core = catalog.get(i).get(Listing.CORE).trim();
                for (int c = 0; c < cores.length; ++c) {
                    if (cores[c] == null) {
                        continue;
                    }
                    if (cores[c].matcher(core).matches()) {
                        catalog.setInstallFlag(i, true);
                        break;
                    }
                }
            }
        }

        // Set default sort order to checked, name
        List<SortKey> sortOrder = new LinkedList<>();
        sortOrder.add(new SortKey(COL_INSTALL, SortOrder.DESCENDING));
        sortOrder.add(new SortKey(COL_ICON, SortOrder.ASCENDING));
        sortOrder.add(new SortKey(COL_NAME, SortOrder.ASCENDING));
        table.getRowSorter().setSortKeys(sortOrder);
        updateRowFilter();

        // Ensure something is selected
        int sel = table.getSelectedRow();
        if (sel < 0 && table.getRowCount() > 0) {
            sel = 0;
        }
        String targetURL = urlCombo.getSelectedItem().toString().trim();
        try {
            ((HTMLDocument) descPane.getDocument()).setBase(new URL(targetURL + "catalog.txt"));
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        }
        showListing(sel);

        updateDownloadButtonText();
        showPanel("catalog");
        dlProgress.setIndeterminate(true);

        // check if this is a URL that isn't in the history
        // now that we know it works, we will add it to the list
        // load catalog URLs from user settings
        if (!targetURL.isEmpty()) {
            boolean found = false;
            Settings s = Settings.getUser();
            int settingIndex;
            for (settingIndex = 1; s.get("catalog-url-" + settingIndex) != null; ++settingIndex) {
                String history = s.get("catalog-url-" + settingIndex);
                if (history.equalsIgnoreCase(targetURL)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                DefaultComboBoxModel urlModel = (DefaultComboBoxModel) urlCombo.getModel();
                // prevent reloading catalog by selecting object
                //doneInit = false;
                s.set("catalog-url-" + settingIndex, targetURL);
                urlModel.addElement(targetURL);
                //doneInit = true;
            }
        }
    }

    private void updateRowFilter() {
        String pat = ((JLabelledField) filterField).isLabelShowing() ? "" : filterField.getText().trim();
        if (pat.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            String[] tokens = pat.split("[,\\s]\\s*");
            final String keys[] = new String[tokens.length];
            final Pattern[] pats = new Pattern[tokens.length];
            final boolean[] invert = new boolean[tokens.length];
            int notTokenCount = 0;
            for (int i = 0; i < tokens.length; ++i) {
                if (tokens[i].startsWith("!")) {
                    invert[i] = true;
                    tokens[i] = tokens[i].substring(1);
                    ++notTokenCount;
                }
                int keySplit = tokens[i].indexOf('=');
                if (keySplit >= 0) {
                    keys[i] = tokens[i].substring(0, keySplit).trim().toLowerCase(Locale.CANADA);
                    tokens[i] = tokens[i].substring(keySplit + 1).trim();
                }
                pats[i] = Pattern.compile(Pattern.quote(tokens[i]).replace("*", "\\E[^\\s]*\\Q").replace("?", "\\E.\\Q"), Pattern.CASE_INSENSITIVE);
            }

            if (StrangeEons.log.isLoggable(Level.FINE)) {
                for (int i = 0; i < tokens.length; ++i) {
                    StrangeEons.log.fine(
                            (invert[i] ? " !   " : "     ")
                            + (keys[i] == null ? "<*>" : keys[i]) + " = "
                            + pats[i]
                    );
                }
            }

            final boolean allTokensAreInverted = (notTokenCount == invert.length);
            rowSorter.setRowFilter(new RowFilter<Object, Integer>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Integer> entry) {
                    // if any !not condition matches, return false (a not filter takes precedence)
                    if (matches(entry, true)) {
                        return false;
                    } else if (allTokensAreInverted) {
                        // if all the patterns start with !,
                        // we need to handle it as a special case by subtracting
                        // matches from all possible plug-ins instead of the
                        // plug-ins that match the non-! patterns.
                        // Otherwise, we are subtracting from an empty set
                        // and the filter will always exclude everything.
                        return true;
                    }
                    // otherwise, return whether any non-!not condition matches
                    return matches(entry, false);
                }

                private boolean matches(Entry<? extends Object, ? extends Integer> entry, boolean checkNots) {
                    if (catalog == null) {
                        return false;
                    }
                    int row = entry.getIdentifier();
                    Listing li = catalog.get(row);
                    for (int i = 0; i < pats.length; ++i) {
                        if (checkNots != invert[i]) {
                            continue;
                        }

                        if (keys[i] != null) {
                            String field;
                            if ("state".equals(keys[i])) {
                                // the special "state" field checks versioning state
                                // rather than an entry in the listing
                                field = catalog.getVersioningState(row).name();
                            } else {
                                field = li.get(keys[i]);
                            }
                            if (filterMatch(pats[i], field, keys[i].equals("game"))) {
                                return true;
                            }
                        } else {
                            if (filterMatch(pats[i], li.getName(), false)) {
                                return true;
                            }
                            if (filterMatch(pats[i], li.getDescription(), false)) {
                                return true;
                            }
                            if (filterMatch(pats[i], li.get(Listing.CREDIT), false)) {
                                return true;
                            }
                            if (filterMatch(pats[i], li.get(Listing.TAGS), false)) {
                                return true;
                            }
                            if (filterMatch(pats[i], li.get(Listing.CORE), false)) {
                                return true;
                            }
                            if (filterMatch(pats[i], li.get(Listing.ID), false)) {
                                return true;
                            }
                            if (filterMatch(pats[i], li.get(Listing.COMMENT), false)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

            });
        }
    }

    private boolean filterMatch(Pattern p, String field, boolean mustMatchEntireField) {
        if (field == null) {
            return false;
        }
        return mustMatchEntireField ? p.matcher(field).matches() : p.matcher(field).find();
    }

    private void updateDownloadButtonText() {
        int selected = catalog == null ? 0 : catalog.determineInstallationRequirements().cardinality();
        JButton dlBtn = PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn);

        if (selected == 0) {
            dlBtn.setEnabled(false);
            dlBtn.setText(string("cat-ok"));
        } else {
            dlBtn.setEnabled(true);
            dlBtn.setText(string("cat-ok") + " (" + selected + ")");
        }

        if (catalog != null && catalog.isRestartRequiredAfterInstall()) {
            restartWarnLabel.setText(string("cat-restart-required"));
            restartWarnLabel.setIcon(WARN_ACTIVE_ICON);
            restartWarnLabel.setBorder(WARN_ACTIVE_BORDER);
            restartWarnLabel.setOpaque(true);
        } else {
            restartWarnLabel.setText("<html>&nbsp;<br>&nbsp;");
            restartWarnLabel.setIcon(WARN_INACTIVE_ICON);
            restartWarnLabel.setBorder(WARN_INACTIVE_BORDER);
            restartWarnLabel.setOpaque(false);
        }
    }

    private final Icon WARN_ACTIVE_ICON = ResourceKit.getIcon("ui/warning.png");
    private final Icon WARN_INACTIVE_ICON = new BlankIcon(WARN_ACTIVE_ICON.getIconWidth(), WARN_ACTIVE_ICON.getIconHeight());
    private Border WARN_INACTIVE_BORDER;
    private Border WARN_ACTIVE_BORDER;

    private void showPanel(String name) {
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, name);
    }

    private void showListing(int n) {
        URL url = null;
        Listing li = n < 0 ? null : catalog.get(n);

        if (li == null) {
            installStateLabel.setIcon(ICON_NOT_INSTALLED);
            installStateLabel.setText("");
            sizeLabel.setText("");
            nameLabel.setText("");
            verLabel.setText("");
            creditsLabel.setText("");
            descPane.setText("<html>");
            coreLabel.setText(" ");
        } else {
            Catalog.VersioningState state = catalog.getVersioningState(n);
            String stateText = "";
            switch (state) {
                case NOT_INSTALLED:
                    if (li.isNew()) {
                        stateText = "cat-is-new-plugin";
                    } else {
                        stateText = "cat-is-not-installed";
                    }
                    break;
                case UP_TO_DATE:
                    stateText = "cat-is-up-to-date";
                    break;
                case OUT_OF_DATE:
                case OUT_OF_DATE_LEGACY:
                    stateText = "cat-is-out-of-date";
                    break;
                case INSTALLED_IS_NEWER:
                    stateText = "cat-is-installed-is-newer";
                    break;
                case REQUIRES_APP_UPDATE:
                    stateText = "cat-is-app-update";
                    break;
            }
            installStateLabel.setText(string(stateText));
            installStateLabel.setIcon(getIconForState(n));
            nameLabel.setText(li.getName());

            long size = -1L;
            {
                String installSize = li.get(Listing.INSTALL_SIZE);
                if (installSize != null) {
                    try {
                        size = Long.parseLong(installSize);
                    } catch (NumberFormatException e) {
                        StrangeEons.log.log(Level.WARNING, "bad install size in listing {0}", li.getName());
                    }
                }
                if (size <= 0L) {
                    // use download size if install size not available
                    size = li.getSize();
                }
            }
            sizeLabel.setText(size <= 0 ? string("cat-unknown") : ProjectUtilities.formatByteSize(size));

            String version = li.getVersion().isEmpty() ? li.getDisplayDate() : li.getVersion() + " (" + li.getDisplayDate() + ")";
            verLabel.setText(version);

            String descText = "<html>" + li.getDescription();

            if (showAdvCheck.isSelected()) {
                StringWriter listingText = new StringWriter();
                try {
                    li.write(listingText);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                listingText.flush();
                descText += "<p style='background-color: #f7f7ff; font-family: Consolas, Andale Mono, Monospaced'>"
                        + listingText.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
                        + "</p>";
            }
            descPane.setText(descText);
            descPane.select(0, 0);
            creditsLabel.setText(li.get(Listing.CREDIT) == null ? " " : li.get(Listing.CREDIT));
            coreLabel.setText(li.get(Listing.CORE) == null ? " " : string("cat-l-core"));
            url = li.getHomePage();
        }
        pageLabel.setText(url == null ? "" : url.toString());
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

        tablePopup = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem clearItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem selectAllItem = new javax.swing.JMenuItem();
        selectAllCoresItem = new javax.swing.JMenuItem();
        descriptionPopup = new javax.swing.JPopupMenu();
        descCopyItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        showAdvCheck = new javax.swing.JCheckBoxMenuItem();
        filterPopup = new javax.swing.JPopupMenu();
        filterHeadItem = new javax.swing.JMenuItem();
        filterGameItem = new javax.swing.JMenuItem();
        filterExpItem = new javax.swing.JMenuItem();
        filterGCItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        filterRefItem = new javax.swing.JMenuItem();
        filterToolItem = new javax.swing.JMenuItem();
        filterDeckItem = new javax.swing.JMenuItem();
        filterProjectItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        cardPanel = new javax.swing.JPanel();
        downloadPanel = new javax.swing.JPanel();
        dlLabel = new javax.swing.JLabel();
        dlProgress = new javax.swing.JProgressBar();
        catalogPanel = new javax.swing.JPanel();
        splitPane = new javax.swing.JSplitPane();
        tableEncloser = new javax.swing.JPanel();
        tableScroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        findPanel = new javax.swing.JPanel();
        findIcon = new javax.swing.JLabel();
        filterField =  new JLabelledField() ;
        infoScroll = new javax.swing.JScrollPane();
        infoPanel = new javax.swing.JPanel();
        nameLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        verLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        pageLabel = new ca.cgjennings.ui.JLinkLabel();
        descPane = new EditorPane();
        installStateLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        sizeLabel = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        creditsLabel = new javax.swing.JLabel();
        coreLabel = new javax.swing.JLabel();
        errorPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        downloadErrorLabel = new javax.swing.JLabel();
        retryBtn1 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        lockedPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        retryBtn = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        helpBtn = new ca.cgjennings.ui.JHelpButton();
        urlCombo = new javax.swing.JComboBox();
        restartWarnLabel = new ca.cgjennings.ui.JWarningLabel();

        clearItem.setText(string( "cat-clear" )); // NOI18N
        clearItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearItemActionPerformed(evt);
            }
        });
        tablePopup.add(clearItem);
        tablePopup.add(jSeparator2);

        selectAllItem.setText(string( "select-all" )); // NOI18N
        selectAllItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllItemActionPerformed(evt);
            }
        });
        tablePopup.add(selectAllItem);

        selectAllCoresItem.setText(string( "cat-select-cores" )); // NOI18N
        selectAllCoresItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllCoresItemActionPerformed(evt);
            }
        });
        tablePopup.add(selectAllCoresItem);

        descCopyItem.setText(string( "copy" )); // NOI18N
        descCopyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                descCopyItemActionPerformed(evt);
            }
        });
        descriptionPopup.add(descCopyItem);
        descriptionPopup.add(jSeparator1);

        showAdvCheck.setSelected(true);
        showAdvCheck.setText(string( "cat-show-adv" )); // NOI18N
        showAdvCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAdvCheckActionPerformed(evt);
            }
        });
        descriptionPopup.add(showAdvCheck);

        filterHeadItem.setFont(filterHeadItem.getFont().deriveFont(filterHeadItem.getFont().getStyle() | java.awt.Font.BOLD));
        filterHeadItem.setText(string("cat-filter-title")); // NOI18N
        filterHeadItem.setEnabled(false);
        filterPopup.add(filterHeadItem);

        filterGameItem.setText(string("cat-filter-game")); // NOI18N
        filterPopup.add(filterGameItem);

        filterExpItem.setText(string("cat-filter-exp")); // NOI18N
        filterPopup.add(filterExpItem);

        filterGCItem.setText(string("cat-filter-gc")); // NOI18N
        filterPopup.add(filterGCItem);
        filterPopup.add(jSeparator4);

        filterRefItem.setText(string("cat-filter-ref")); // NOI18N
        filterPopup.add(filterRefItem);

        filterToolItem.setText(string("cat-filter-tool")); // NOI18N
        filterPopup.add(filterToolItem);

        filterDeckItem.setText(string("cat-filter-deck")); // NOI18N
        filterPopup.add(filterDeckItem);

        filterProjectItem.setText(string("cat-filter-proj")); // NOI18N
        filterPopup.add(filterProjectItem);
        filterPopup.add(jSeparator5);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "cat-title" )); // NOI18N

        cardPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 1, 0, new java.awt.Color(146, 151, 161)), javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        cardPanel.setLayout(new java.awt.CardLayout());

        downloadPanel.setLayout(new java.awt.GridBagLayout());

        dlLabel.setText(string( "cat-busy" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(16, 32, 8, 32);
        downloadPanel.add(dlLabel, gridBagConstraints);

        dlProgress.setIndeterminate(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 32, 12, 32);
        downloadPanel.add(dlProgress, gridBagConstraints);

        cardPanel.add(downloadPanel, "download");

        splitPane.setDividerSize(8);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);

        tableEncloser.setLayout(new java.awt.BorderLayout());

        tableScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        table.setFont(table.getFont().deriveFont(table.getFont().getSize()-1f));
        table.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setComponentPopupMenu(tablePopup);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableScroll.setViewportView(table);

        tableEncloser.add(tableScroll, java.awt.BorderLayout.CENTER);

        findPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0, 0, 0)));
        findPanel.setLayout(new java.awt.GridBagLayout());

        findIcon.setIcon( ResourceKit.getIcon( "ui/find-sm.png" ) );
        findIcon.setMinimumSize(new java.awt.Dimension(11, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        findPanel.add(findIcon, gridBagConstraints);

        filterField.setFont(filterField.getFont().deriveFont(filterField.getFont().getSize()-1f));
        filterField.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
        filterField.setComponentPopupMenu(filterPopup);
        filterField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        findPanel.add(filterField, gridBagConstraints);

        tableEncloser.add(findPanel, java.awt.BorderLayout.SOUTH);

        splitPane.setLeftComponent(tableEncloser);

        infoScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        infoPanel.setBackground(java.awt.Color.white);
        infoPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        nameLabel.setBackground(java.awt.Color.lightGray);
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | java.awt.Font.BOLD, nameLabel.getFont().getSize()+2));
        nameLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.darkGray), javax.swing.BorderFactory.createEmptyBorder(2, 8, 4, 8)));
        nameLabel.setOpaque(true);

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD, jLabel3.getFont().getSize()-1));
        jLabel3.setText(string( "cat-ver" )); // NOI18N

        verLabel.setFont(verLabel.getFont().deriveFont(verLabel.getFont().getSize()-1f));
        verLabel.setText("*");

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD, jLabel1.getFont().getSize()-1));
        jLabel1.setText(string( "cat-home" )); // NOI18N

        pageLabel.setText("*");
        pageLabel.setFont(pageLabel.getFont().deriveFont(pageLabel.getFont().getSize()-1f));

        descPane.setEditable(false);
        descPane.setBackground( Color.WHITE );
        descPane.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.darkGray), javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        descPane.setContentType("text/html"); // NOI18N
        descPane.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        descPane.setForeground( Color.BLACK );
        descPane.setComponentPopupMenu(descriptionPopup);

        installStateLabel.setText("state");

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD, jLabel4.getFont().getSize()-1));
        jLabel4.setText(string( "cat-size" )); // NOI18N

        sizeLabel.setFont(sizeLabel.getFont().deriveFont(sizeLabel.getFont().getSize()-1f));
        sizeLabel.setText("*");

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getStyle() | java.awt.Font.BOLD, jLabel6.getFont().getSize()-1));
        jLabel6.setText(string( "cat-credit" )); // NOI18N

        creditsLabel.setFont(creditsLabel.getFont().deriveFont(creditsLabel.getFont().getSize()-1f));
        creditsLabel.setText("*");

        coreLabel.setFont(coreLabel.getFont().deriveFont(coreLabel.getFont().getStyle() | java.awt.Font.BOLD, coreLabel.getFont().getSize()-1));
        coreLabel.setText(string( "cat-l-core" )); // NOI18N

        javax.swing.GroupLayout infoPanelLayout = new javax.swing.GroupLayout(infoPanel);
        infoPanel.setLayout(infoPanelLayout);
        infoPanelLayout.setHorizontalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(nameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
            .addComponent(descPane, javax.swing.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(infoPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(infoPanelLayout.createSequentialGroup()
                                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(pageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                                    .addComponent(sizeLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                                    .addComponent(creditsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                                    .addComponent(verLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, infoPanelLayout.createSequentialGroup()
                                .addComponent(installStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
                                .addGap(89, 89, 89))))
                    .addGroup(infoPanelLayout.createSequentialGroup()
                        .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(infoPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel6))
                            .addGroup(infoPanelLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(coreLabel)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        infoPanelLayout.setVerticalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addComponent(nameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(installStateLabel)
                .addGap(1, 1, 1)
                .addComponent(coreLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(sizeLabel)
                    .addComponent(jLabel4))
                .addGap(1, 1, 1)
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(verLabel))
                .addGap(1, 1, 1)
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(creditsLabel))
                .addGap(1, 1, 1)
                .addGroup(infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(pageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(descPane, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE))
        );

        infoScroll.setViewportView(infoPanel);

        splitPane.setRightComponent(infoScroll);

        javax.swing.GroupLayout catalogPanelLayout = new javax.swing.GroupLayout(catalogPanel);
        catalogPanel.setLayout(catalogPanelLayout);
        catalogPanelLayout.setHorizontalGroup(
            catalogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 811, Short.MAX_VALUE)
        );
        catalogPanelLayout.setVerticalGroup(
            catalogPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(splitPane)
        );

        cardPanel.add(catalogPanel, "catalog");

        errorPanel.setLayout(new java.awt.GridBagLayout());

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getStyle() | java.awt.Font.BOLD, jLabel7.getFont().getSize()+2));
        jLabel7.setText(string( "cat-err" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 4, 16);
        errorPanel.add(jLabel7, gridBagConstraints);

        downloadErrorLabel.setFont(downloadErrorLabel.getFont().deriveFont(downloadErrorLabel.getFont().getSize()-1f));
        downloadErrorLabel.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 16);
        errorPanel.add(downloadErrorLabel, gridBagConstraints);

        retryBtn1.setFont(retryBtn1.getFont().deriveFont(retryBtn1.getFont().getSize()+1f));
        retryBtn1.setText(string("cat-b-update-in-progress")); // NOI18N
        retryBtn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                retryBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 16);
        errorPanel.add(retryBtn1, gridBagConstraints);

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/application/error.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 16, 12);
        errorPanel.add(jLabel5, gridBagConstraints);

        cardPanel.add(errorPanel, "error");

        lockedPanel.setLayout(new java.awt.GridBagLayout());

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD, jLabel2.getFont().getSize()+2));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText(string( "cat-l-update-in-progress" )); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 8, 16);
        lockedPanel.add(jLabel2, gridBagConstraints);

        retryBtn.setFont(retryBtn.getFont().deriveFont(retryBtn.getFont().getSize()+1f));
        retryBtn.setText(string("cat-b-update-in-progress")); // NOI18N
        retryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                retryBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 16);
        lockedPanel.add(retryBtn, gridBagConstraints);

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/application/information.png"))); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 16, 12);
        lockedPanel.add(jLabel8, gridBagConstraints);

        cardPanel.add(lockedPanel, "locked");

        cancelBtn.setText(string( "close" )); // NOI18N

        okBtn.setText(string( "cat-ok" )); // NOI18N

        helpBtn.setHelpPage("plugins-catalogue");

        urlCombo.setEditable(true);
        urlCombo.setToolTipText(string("cat-url")); // NOI18N
        urlCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                urlComboActionPerformed(evt);
            }
        });

        restartWarnLabel.setText(string("cat-restart-required")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(restartWarnLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(okBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelBtn)
                .addContainerGap())
            .addComponent(urlCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(urlCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1, 1, 1)
                .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(cancelBtn)
                        .addComponent(okBtn))
                    .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(restartWarnLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void initFilterMenu() {
        final HashMap<JMenuItem, String> filterMap = new HashMap<>();
        filterMap.put(filterGameItem, "tags=game");
        filterMap.put(filterExpItem, "tags=expansion");
        filterMap.put(filterGCItem, "tags=game,tags=expansion,tags=component");
        filterMap.put(filterRefItem, "tags=reference");
        filterMap.put(filterToolItem, "tags=tool");
        filterMap.put(filterDeckItem, "tags=deck");
        filterMap.put(filterProjectItem, "tags=project");

        // add items for installed games
        int widestIconWidth = 0;
        Icon widestIcon = null;
        for (Game g : Game.getGames(false)) {
            JMenuItem gameFilter = new JMenuItem(g.getUIName(), g.getIcon());
            if (g.getIcon().getIconWidth() > widestIconWidth) {
                widestIcon = g.getIcon();
                widestIconWidth = widestIcon.getIconWidth();
            }
            filterPopup.add(gameFilter);
            filterMap.put(gameFilter, "game=" + g.getCode());
        }

        // fix for OS X native menus to line up text of items with no icons
        //     with text of items with icons
        Icon blank = null;
        if (widestIcon != null) {
            blank = new BlankIcon(widestIconWidth, widestIcon.getIconHeight());
        }

        ActionListener applyFilterAction = (ActionEvent e) -> {
            String filter = filterMap.get(e.getSource());
            filterField.setText(filter);
        };

        for (int i = 1; i < filterPopup.getComponentCount(); ++i) {
            if (filterPopup.getComponent(i) instanceof JMenuItem) {
                JMenuItem item = (JMenuItem) filterPopup.getComponent(i);
                if (!filterMap.containsKey(item)) {
                    StrangeEons.log.warning("missing filter text for menu item " + item);
                    continue;
                }
                item.addActionListener(applyFilterAction);
                if (item.getIcon() == null) {
                    item.setIcon(blank);
                }
            }
        }

        // keep heading from being displayed in disabled text colour
        filterHeadItem.setFont(
                filterHeadItem.getFont().deriveFont(
                        Collections.singletonMap(TextAttribute.FOREGROUND, filterHeadItem.getForeground())
                )
        );
    }

	private void urlComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_urlComboActionPerformed
            if (!doneInit) {
                return;
            }
            String location = urlCombo.getSelectedItem().toString();
            if (location.indexOf(':') < 0) {
                location = "https://" + location;
                urlCombo.setSelectedItem(location);
                return;
            }
            if (location.endsWith("/catalog.txt")) {
                location = location.substring(0, location.length() - "catalog.txt".length());
                urlCombo.setSelectedItem(location);
                return;
            }
            if (!location.endsWith("/")) {
                location += "/";
                urlCombo.setSelectedItem(location);
                return;
            }
            URL url = null;
            try {
                url = new URL(location);
            } catch (MalformedURLException ex) {
            }
            if (url == null) {
                UIManager.getLookAndFeel().provideErrorFeedback(urlCombo);
                urlCombo.getEditor().selectAll();
                urlCombo.requestFocusInWindow();
                return;
            }

            // see declaration of lastLoadedCatalog, below, for an explanation
            boolean allowCache = lastLoadedCatalog == null || !lastLoadedCatalog.equals(url);
            if (!allowCacheHint) {
                allowCache = false;
                allowCacheHint = true;
            }
            lastLoadedCatalog = url;

            downloadCatalog(url, allowCache);
	}//GEN-LAST:event_urlComboActionPerformed

    // URL of last loaded catalog: this is used to control caching:
    // if the same URL is loaded two or more times in a row, caches will
    // not be allowed on the second and following loads.
    private URL lastLoadedCatalog;

	private void clearItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearItemActionPerformed
            if (catalog == null) {
                return;
            }
            Model model = (Model) table.getModel();
            for (int i = 0; i < catalog.size(); ++i) {
                if (model.isCellEditable(i, COL_INSTALL)) {
                    catalog.setInstallFlag(i, false);
                }
            }
            table.repaint();
            updateDownloadButtonText();
	}//GEN-LAST:event_clearItemActionPerformed

	private void selectAllItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllItemActionPerformed
            if (catalog == null) {
                return;
            }

            // select all possible visible (non-filtered) plug-ins
            final Model model = (Model) table.getModel();
            for (int i = 0; i < catalog.size(); ++i) {
                // can't change state, so skip
                if (!model.isCellEditable(i, COL_INSTALL)) continue;

                if (table.convertRowIndexToView(i) >= 0) {
                    catalog.setInstallFlag(i, true);
                }
            }
            table.repaint();
            updateDownloadButtonText();
	}//GEN-LAST:event_selectAllItemActionPerformed

	private void descCopyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_descCopyItemActionPerformed
            if (descPane.getSelectionStart() == descPane.getSelectionEnd()) {
                StringSelection ss = new StringSelection(descPane.getText());
                getToolkit().getSystemClipboard().setContents(ss, ss);
            } else {
                descPane.copy();
            }
	}//GEN-LAST:event_descCopyItemActionPerformed

	private void showAdvCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showAdvCheckActionPerformed
            if (!doneInit) {
                return;
            }
            Settings.getUser().setYesNo("catalog-show-expert-info", showAdvCheck.isSelected());

            // update listing if possible
            int sel = table.getSelectedRow();
            if (sel < 0 && table.getRowCount() > 0) {
                sel = 0;
            }
            sel = table.convertRowIndexToModel(sel);
            showListing(sel);
	}//GEN-LAST:event_showAdvCheckActionPerformed

	private void selectAllCoresItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllCoresItemActionPerformed
            if (catalog == null) {
                return;
            }
            Model model = (Model) table.getModel();
            for (int i = 0; i < catalog.size(); ++i) {
                if (model.isCellEditable(i, COL_INSTALL) && catalog.get(i).get(Listing.CORE) != null) {
                    catalog.setInstallFlag(i, true);
                }
            }
            table.repaint();
            updateDownloadButtonText();
	}//GEN-LAST:event_selectAllCoresItemActionPerformed

	private void filterFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterFieldActionPerformed
            updateRowFilter();
}//GEN-LAST:event_filterFieldActionPerformed

    private void retryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_retryBtnActionPerformed
        urlComboActionPerformed(null);
    }//GEN-LAST:event_retryBtnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JPanel cardPanel;
    private javax.swing.JPanel catalogPanel;
    private javax.swing.JLabel coreLabel;
    private javax.swing.JLabel creditsLabel;
    private javax.swing.JMenuItem descCopyItem;
    private javax.swing.JEditorPane descPane;
    private javax.swing.JPopupMenu descriptionPopup;
    private javax.swing.JLabel dlLabel;
    private javax.swing.JProgressBar dlProgress;
    private javax.swing.JLabel downloadErrorLabel;
    private javax.swing.JPanel downloadPanel;
    private javax.swing.JPanel errorPanel;
    private javax.swing.JMenuItem filterDeckItem;
    private javax.swing.JMenuItem filterExpItem;
    private javax.swing.JTextField filterField;
    private javax.swing.JMenuItem filterGCItem;
    private javax.swing.JMenuItem filterGameItem;
    private javax.swing.JMenuItem filterHeadItem;
    private javax.swing.JPopupMenu filterPopup;
    private javax.swing.JMenuItem filterProjectItem;
    private javax.swing.JMenuItem filterRefItem;
    private javax.swing.JMenuItem filterToolItem;
    private javax.swing.JLabel findIcon;
    private javax.swing.JPanel findPanel;
    private ca.cgjennings.ui.JHelpButton helpBtn;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JScrollPane infoScroll;
    private javax.swing.JLabel installStateLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPanel lockedPanel;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JButton okBtn;
    private ca.cgjennings.ui.JLinkLabel pageLabel;
    private ca.cgjennings.ui.JWarningLabel restartWarnLabel;
    private javax.swing.JButton retryBtn;
    private javax.swing.JButton retryBtn1;
    private javax.swing.JMenuItem selectAllCoresItem;
    private javax.swing.JCheckBoxMenuItem showAdvCheck;
    private javax.swing.JLabel sizeLabel;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JTable table;
    private javax.swing.JPanel tableEncloser;
    private javax.swing.JPopupMenu tablePopup;
    private javax.swing.JScrollPane tableScroll;
    private javax.swing.JComboBox urlCombo;
    private javax.swing.JLabel verLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void dispose() {
        Settings.getUser().storeWindowSettings("plugin-catalog", this);
        super.dispose();
    }
    // End of variables declaration

    @Override
    public void handleOKAction(ActionEvent e) {
        PlatformSupport.getAgnosticOK(true, okBtn, cancelBtn).setEnabled(false);

        boolean requiresRestart = catalog.installFlaggedPlugins();

        if (requiresRestart) {
            StrangeEons.getWindow().suggestRestart(null);
        }

        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    private class Model extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return catalog == null ? 0 : catalog.size();
        }

        @Override
        public int getColumnCount() {
            return COL_COUNT;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (catalog != null && columnIndex == COL_INSTALL) {
                Catalog.VersioningState state = catalog.getVersioningState(rowIndex);
                return (state != VersioningState.REQUIRES_APP_UPDATE) && (state != VersioningState.UP_TO_DATE) && (state != VersioningState.INSTALLED_IS_NEWER);
            }
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case COL_INSTALL:
                    return Boolean.class;
                case COL_ICON:
                    return Icon.class;
                default:
                    return String.class;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_INSTALL:
                    return string("cat-check-col");
                case COL_ICON:
                    return "";
                case COL_NAME:
                    return string("cat-name");
                default:
                    throw new AssertionError("unknown column: " + column);
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            catalog.setInstallFlag(rowIndex, (Boolean) aValue);
            updateDownloadButtonText();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Listing li = catalog.get(rowIndex);
            switch (columnIndex) {
                case COL_INSTALL:
                    Catalog.VersioningState state = catalog.getVersioningState(rowIndex);
                    if (state == Catalog.VersioningState.REQUIRES_APP_UPDATE) {
                        return false;
                    }
                    if (state == Catalog.VersioningState.UP_TO_DATE) {
                        return true;
                    }
                    return catalog.getInstallFlag(rowIndex);
                case COL_ICON:
                    return getIconForState(rowIndex);
                case COL_NAME:
                    return li.getName();
                default:
                    return "??? " + columnIndex;
            }
        }
    }

    private static final int COL_INSTALL = 0;
    private static final int COL_NAME = 1;
    private static final int COL_ICON = 2;
    private static final int COL_COUNT = 3;

    private Icon getIconForState(int rowIndex) {
        if (catalog == null) {
            return ICON_NOT_INSTALLED;
        }
        Catalog.VersioningState state = catalog.getVersioningState(rowIndex);
        boolean core = catalog.get(rowIndex).get(Listing.CORE) != null;

        switch (state) {
            case NOT_INSTALLED:
                // return a "new" icon not just for IDs that have never been seen before,
                // but also if this version was recently released
                boolean isNew = catalog.get(rowIndex).isNew();
                if (!isNew) {
                    CatalogID id = catalog.get(rowIndex).getCatalogID();
                    if (id != null) {
                        final GregorianCalendar itemDate = id.getDate();
                        final long daysOld = (now.getTimeInMillis() - itemDate.getTimeInMillis())
                                / (1_000L * 60L * 60L * 24L);
                        if (daysOld <= daysToConsiderNew) {
                            isNew = true;
                        }
                    }
                }
                if (isNew) {
                    return core ? CORE_ICON_NEW_PLUGIN : ICON_NEW_PLUGIN;
                } else {
                    return core ? CORE_ICON_NOT_INSTALLED : ICON_NOT_INSTALLED;
                }
            case UP_TO_DATE:
                return core ? CORE_ICON_UP_TO_DATE : ICON_UP_TO_DATE;
            case OUT_OF_DATE:
            case OUT_OF_DATE_LEGACY:
                return core ? CORE_ICON_UPDATE_AVAILABLE : ICON_UPDATE_AVAILABLE;
            case INSTALLED_IS_NEWER:
                return core ? CORE_ICON_INSTALLED_IS_NEWER : ICON_INSTALLED_IS_NEWER;
            case REQUIRES_APP_UPDATE:
                return core ? CORE_ICON_APP_UPDATE : ICON_APP_UPDATE;
            default:
                throw new AssertionError("unknown state: " + state);
        }
    }
    private GregorianCalendar now = new GregorianCalendar();
    private int daysToConsiderNew = 90;

    static final Icon ICON_NEW_PLUGIN = ResourceKit.getIcon("catalog/not-installed-new.png");
    static final Icon ICON_NOT_INSTALLED = ResourceKit.getIcon("catalog/not-installed.png");
    static final Icon ICON_UP_TO_DATE = ResourceKit.getIcon("catalog/up-to-date.png");
    static final Icon ICON_INSTALLED_IS_NEWER = ResourceKit.getIcon("catalog/installed-is-newer.png");
    static final Icon ICON_UPDATE_AVAILABLE = ResourceKit.getIcon("catalog/update-available.png");
    static final Icon ICON_APP_UPDATE = ResourceKit.getIcon("catalog/app-update.png");

    static final Icon CORE_ICON_NEW_PLUGIN = ResourceKit.getIcon("catalog/core-not-installed-new.png");
    static final Icon CORE_ICON_NOT_INSTALLED = ResourceKit.getIcon("catalog/core-not-installed.png");
    static final Icon CORE_ICON_UP_TO_DATE = ResourceKit.getIcon("catalog/core-up-to-date.png");
    static final Icon CORE_ICON_INSTALLED_IS_NEWER = ResourceKit.getIcon("catalog/core-installed-is-newer.png");
    static final Icon CORE_ICON_UPDATE_AVAILABLE = ResourceKit.getIcon("catalog/core-update-available.png");
    static final Icon CORE_ICON_APP_UPDATE = ICON_APP_UPDATE;
}
