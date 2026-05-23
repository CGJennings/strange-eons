package ca.cgjennings.apps.arkham.dialog.prefs;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.theme.Theme;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.logging.Level;
import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.UIManager;
import resources.Language;
import static resources.Language.string;

/**
 * The dialog that displays registered preference categories.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class Preferences extends javax.swing.JDialog implements AgnosticDialog {

    /**
     * Creates new form Preferences
     */
    public Preferences() {
        super(StrangeEons.getWindow(), ModalityType.APPLICATION_MODAL);
        StrangeEons.setWaitCursor(true);
        try {
            initComponents();

            // to force naughty pref category names to a conformant width
            categoryWidth = catScroll.getViewport().getWidth();

            getRootPane().setDefaultButton(okBtn);
            PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

            catList.setForeground(UIManager.getColor(Theme.PREFS_FOREGROUND));
            catList.setBackground(UIManager.getColor(Theme.PREFS_BACKGROUND));
            catList.setCellRenderer(new Renderer());
            updateCategories();
            catList.setSelectedIndex(0);

            cardScroll.getVerticalScrollBar().setUnitIncrement(12);

            setLocationRelativeTo(StrangeEons.getWindow());
        } finally {
            StrangeEons.setWaitCursor(false);
        }
    }

    private final int categoryWidth;

    /**
     * Sets the selected category in this preferences dialog.
     *
     * @param pc the category to select
     */
    public void setSelectedCategory(PreferenceCategory pc) {
        catList.setSelectedValue(pc, true);
    }

    /**
     * Selects the a preference category by title. If no category matches the
     * provided title, the selection will not change.
     *
     * @param name the title of the category to select
     */
    public void setSelectedCategory(String name) {
        DefaultListModel<PreferenceCategory> m = (DefaultListModel<PreferenceCategory>) catList.getModel();
        int size = m.getSize();
        int sel = -1;
        for (int i = 0; i < size; ++i) {
            PreferenceCategory cat = (PreferenceCategory) m.get(i);
            if (name.equals(cat.getTitle())) {
                sel = i;
                break;
            }
        }
        if (sel != -1) {
            catList.setSelectedIndex(sel);
        }
    }

    /**
     * Returns the currently selected category in this preferences dialog.
     *
     * @return the selected preference category, or {@code null} if no category
     * is selected
     */
    public PreferenceCategory getSelectedCategory() {
        return (PreferenceCategory) catList.getSelectedValue();
    }

    /**
     * Scroll the currently displayed category to display a particular section.
     * The desired section is identified by specifying the label text of labels
     * at its top and bottom. If the top label is {@code null}, then the top of
     * the category is used. If the bottom label is {@code null}, then the
     * bottom of the category is used. If either label cannot be found in the
     * category, it is treated as {@code null} (although a warning will be
     * logged to the console). If the section is too long to fit within the
     * category view without scrolling, the dialog will scroll to the top of the
     * top section.
     *
     * @param sectionTop label text identifying the top of the section, or
     * {@code null}
     * @param sectionBottom label text identifying the bottom of the section, or
     * {@code null}
     */
    public void scrollToCategorySection(String sectionTop, String sectionBottom) {
        // get the container for the showing category
        Component parent = null;
        for (int i = 0; i < prefCards.getComponentCount(); ++i) {
            if (prefCards.getComponent(i).isVisible()) {
                parent = prefCards.getComponent(i);
                break;
            }
        }
        if (!(parent instanceof Container)) {
            return;
        }
        Container panel = (Container) parent;

        final Rectangle top = findSection(panel, sectionTop, true);
        final Rectangle bottom = findSection(panel, sectionBottom, false);
        EventQueue.invokeLater(() -> {
            Rectangle t = top;
            Rectangle b = bottom;
            if (b.y < t.y) {
                Rectangle temp = b;
                t = b;
                b = temp;
            }
            prefCards.scrollRectToVisible(b);
            prefCards.scrollRectToVisible(t);
        });
    }

    private Rectangle findSection(Container parent, String text, boolean isTop) {
        if (text != null) {
            // hack: remove '&' from label string
            text = text.replace("&", "");
            for (int i = 0; i < parent.getComponentCount(); ++i) {
                Component c = parent.getComponent(i);
                boolean match = false;
                if ((c instanceof JLabel) && text.equals(((JLabel) c).getText())) {
                    match = true;
                } else if ((c instanceof AbstractButton) && text.equals(((AbstractButton) c).getText())) {
                    match = true;
                }
                if (match) {
                    if (isTop) {
                        return new Rectangle(0, c.getY(), 1, 1);
                    } else {
                        return new Rectangle(0, c.getY() + c.getHeight() - 2, 1, 1);
                    }
                }
            }
            StrangeEons.log.warning("label not found: " + text);
        }
        if (isTop) {
            return new Rectangle(0, 0, 1, 1);
        } else {
            return new Rectangle(0, parent.getHeight() - 2, 1, 1);
        }
    }

    private final Comparator<PreferenceCategory> catSorter = (PreferenceCategory lhs, PreferenceCategory rhs) -> {
        if (rhs == null) {
            return -1;
        }
        return Language.getInterface().getCollator().compare(lhs.getTitle(), rhs.getTitle());
    };

    private void updateCategories() {
        if (!isSorted) {
            Collections.sort(custom, catSorter);
        }

        prefCards.removeAll();

        DefaultListModel<PreferenceCategory> m = new DefaultListModel<>();
        LinkedList<PreferenceCategory> list = main;
        int count = 0;
        for (int type = 0; type < 2; ++type) {
            for (int i = 0; i < list.size(); ++i) {
                try {
                    StrangeEons.log.log(Level.INFO, "loading preference category: {0}", list.get(i).getTitle());
                    long startTime = System.currentTimeMillis();
                    PreferenceCategory pc = list.get(i);
                    pc.loadSettings();
                    m.addElement(pc);
                    JPanel panel = pc.getPanel();
                    panel.validate();
                    panel.setSize(panel.getPreferredSize());
                    prefCards.add(panel, String.valueOf(count++));
                    long time = System.currentTimeMillis() - startTime;
                    if (time > 500) {
                        StrangeEons.log.warning("slow preference category " + pc.getTitle() + " (" + time + " ms)");
                    } else {
                        StrangeEons.log.log(Level.INFO, "loaded in {0} ms", time);
                    }
                } catch (Throwable t) {
                    StrangeEons.log.log(Level.SEVERE, "exception while loading preference category", t);
                }
            }
            list = custom;
        }

        catList.setModel(m);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        catScroll = new javax.swing.JScrollPane();
        catList = new javax.swing.JList<>();
        cardScroll = new javax.swing.JScrollPane();
        prefCards = new javax.swing.JPanel();
        okBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        overlayPanel = new ca.cgjennings.apps.arkham.dialog.OverlayPanel();
        restartFootnoteLabel = new javax.swing.JLabel();
        helpBtn = new ca.cgjennings.ui.JHelpButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "sd-title" )); // NOI18N

        catScroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        catList.setFont(catList.getFont().deriveFont(catList.getFont().getStyle() | java.awt.Font.BOLD, catList.getFont().getSize()-1));
        catList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        catList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                catListValueChanged(evt);
            }
        });
        catScroll.setViewportView(catList);

        prefCards.setBackground(UIManager.getColor(Theme.PREFS_BACKGROUND));
        prefCards.setLayout(new java.awt.CardLayout());
        cardScroll.setViewportView(prefCards);

        okBtn.setText(string("sd-b-ok")); // NOI18N

        cancelBtn.setText(string("cancel")); // NOI18N

        overlayPanel.setLayout(new java.awt.GridBagLayout());

        restartFootnoteLabel.setFont(restartFootnoteLabel.getFont().deriveFont(restartFootnoteLabel.getFont().getStyle() | java.awt.Font.BOLD, restartFootnoteLabel.getFont().getSize()-1));
        restartFootnoteLabel.setText(string("sd-l-requires-restart")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 12);
        overlayPanel.add(restartFootnoteLabel, gridBagConstraints);

        helpBtn.setHelpPage("ui-preferences");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(catScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cardScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 635, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(overlayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(helpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 273, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cardScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                    .addComponent(catScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(overlayPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cancelBtn)
                            .addComponent(okBtn))
                        .addContainerGap())
                    .addComponent(helpBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void catListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_catListValueChanged
            final int sel = catList.getSelectedIndex();
            if (sel < 0) {
                return;
            }
            CardLayout layout = (CardLayout) prefCards.getLayout();
            layout.show(prefCards, String.valueOf(sel));
            PreferenceCategory pc = (PreferenceCategory) catList.getSelectedValue();
            prefCards.setPreferredSize(pc.getPanel().getPreferredSize());
	}//GEN-LAST:event_catListValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JScrollPane cardScroll;
    private javax.swing.JList<PreferenceCategory> catList;
    private javax.swing.JScrollPane catScroll;
    private ca.cgjennings.ui.JHelpButton helpBtn;
    private javax.swing.JButton okBtn;
    private ca.cgjennings.apps.arkham.dialog.OverlayPanel overlayPanel;
    private javax.swing.JPanel prefCards;
    private javax.swing.JLabel restartFootnoteLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        boolean restart = false;
        LinkedList<PreferenceCategory> list = custom;
        for (int i = 1; i >= 0; --i) {
            for (PreferenceCategory pc : list) {
                try {
                    pc.storeSettings();
                    restart |= pc.isRestartRequired();
                } catch (Throwable t) {
                    StrangeEons.log.log(Level.WARNING, "exception while storing preference category " + pc, t);
                }
            }
            list = main;
        }
        dispose();
        if (restart) {
            StrangeEons.getWindow().suggestRestart(null);
        }
        firePreferenceUpdate();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }
    // End of variables declaration

    private class Renderer extends DefaultListCellRenderer {

        public Renderer() {
            super();
            setHorizontalAlignment(CENTER);
            setHorizontalTextPosition(CENTER);
            setVerticalTextPosition(BOTTOM);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            PreferenceCategory cat = (PreferenceCategory) value;
            super.getListCellRendererComponent(list, cat.getTitle(), index, isSelected, cellHasFocus);
            setIcon(cat.getIcon());
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = categoryWidth;
            return d;
        }
    }

    private static final LinkedList<PreferenceCategory> main = new LinkedList<>();
    private static final LinkedList<PreferenceCategory> custom = new LinkedList<>();

    public static void registerCategory(PreferenceCategory cat) {
        registerCategory(cat, true);
    }

    public static void unregisterCategory(PreferenceCategory cat) {
        // first we try to find the category by reference; if that fails
        // we find the first category with the same title---this is a
        // workaround for plug-in scripts since most plug-in authors are
        // novice programmers and unlikely to realize that if a plug-in
        // must be restarted, the category instance that they create the
        // second time will not be the same instance unless special steps
        // are taken to preserve it
        for (int compareType = 0; compareType < 2; ++compareType) {
            LinkedList<PreferenceCategory> list = main;
            for (int t = 1; t >= 0; --t) {
                for (int i = list.size() - 1; i >= 0; --i) {
                    final PreferenceCategory potMatch = list.get(i);
                    if ( // match by reference
                            (compareType == 0 && potMatch == cat)
                            || // match by name
                            (compareType == 1 && potMatch.getTitle().equals(cat.getTitle()))) {
                        list.remove(i);
                        return;
                    }
                }
                list = custom;
            }
        }
        StrangeEons.log.log(Level.WARNING, "tried to unregister unknown category: {0}", cat);
    }

    private static void registerCategory(PreferenceCategory cat, boolean customList) {
        LinkedList<PreferenceCategory> list = customList ? custom : main;
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).equals(cat)) {
                return;
            }
        }
        list.add(cat);
        if (customList) {
            isSorted = false;
        }
    }

    private static boolean isSorted = true;

    static {
        registerCategory(new CatLanguage(), false);
        registerCategory(new CatTheme(), false);
        registerCategory(new CatContextBar(), false);
        registerCategory(new CatReservedCloudFonts(), false);
        registerCategory(new CatDesignSupport(), false);
        registerCategory(new CatDrawPerformance(), false);
        registerCategory(new CatPlugins(), false);
    }

    /**
     * Adds a listener that will be updated when the user changes the
     * application preferences.
     *
     * @param li the listener to remove
     */
    public static void addPreferenceUpdateListener(PreferenceUpdateListener li) {
        if (li == null) {
            throw new NullPointerException("listener");
        }
        puls.remove(li);
        puls.add(li);
    }

    /**
     * Stops a listener from being updated when the user changes the application
     * preferences.
     *
     * @param li the listener to remove
     */
    public static void removePreferenceUpdateListener(PreferenceUpdateListener li) {
        puls.remove(li);
    }

    /**
     * Informs all registered listeners that the user has changed their
     * preference settings.
     */
    public static void firePreferenceUpdate() {
        for (PreferenceUpdateListener target : puls) {
            target.preferencesUpdated();
        }
    }

    private static final LinkedList<PreferenceUpdateListener> puls = new LinkedList<>();
}
