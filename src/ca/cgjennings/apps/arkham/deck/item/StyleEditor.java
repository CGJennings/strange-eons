package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.Timer;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * An editor dialog for the styles of one or more page items in a deck.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class StyleEditor extends javax.swing.JDialog implements AgnosticDialog {

    /**
     * Creates new style editor.
     */
    public StyleEditor(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);
    }

    public void initializeForSelection(PageItem[] sel) {
        if (sel == null) {
            throw new NullPointerException("sel");
        }
        selection = sel.clone();
        composite = new StyleCapture(selection);
        cancelCaptures = new StyleCapture[selection.length];
        for (int i = 0; i < selection.length; ++i) {
            cancelCaptures[i] = new StyleCapture(selection[i]);
        }

        LinkedList<PanelKit> ok = new LinkedList<>();
        for (Class<? extends Style> style : composite.getCapturedStyles()) {
            try {
                StylePanel<? extends Style> panel = StylePanelFactory.createStylePanel(style);
                ok.add(new PanelKit(panel, style));
            } catch (Throwable t) {
                StrangeEons.log.log(Level.SEVERE, "failed to created style panel for " + style, t);
            }
        }

        kits = ok.toArray(new PanelKit[ok.size()]);
        Arrays.sort(kits);

        boolean noConflicts = true;
        styleTab.removeAll();
        for (PanelKit kit : kits) {
            kit.panel.setCallback(callback);
            kit.panel.populatePanelFromCapture(composite);
            Icon tabIcon = null;
            if (kit.conflicted) {
                tabIcon = conflictLabel.getIcon();
                noConflicts = false;
            }
            styleTab.addTab(kit.panel.getTitle(), tabIcon, kit.panel.getPanelComponent());
        }
        if (noConflicts) {
            conflictWarning.setVisible(false);
        }
        pack();
        int w = getWidth();
        int h = getHeight();
        if (w < 400 || h < 250) {
            w = Math.max(400, w);
            h = Math.max(250, h);
            setSize(w, h);
        }
    }

    private PageItem[] selection;
    private StyleCapture composite;
    private PanelKit[] kits;
    private StyleCapture[] cancelCaptures;

    private final Timer updateDelay = new Timer(500, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (updateTimeDrawn < updateTimeLatest) {
                apply(true);
                updateTimeDrawn = System.currentTimeMillis();
            }
        }
    });

    {
        updateDelay.setRepeats(false);
    }
    private long updateTimeDrawn;
    private long updateTimeLatest;

    private final class PanelKit implements Comparable<PanelKit> {

        public PanelKit(StylePanel<? extends Style> panel, Class<? extends Style> style) {
            this.panel = panel;
            this.style = style;
            this.conflicted = composite.isStyleInConflict(style);
        }
        private StylePanel<? extends Style> panel;
        private final Class<? extends Style> style;
        private boolean conflicted;

        @Override
        public int compareTo(PanelKit o) {
            // sort by group, and if the groups are the same sort by title
            int order = panel.getPanelGroup() - o.panel.getPanelGroup();
            if (order == 0) {
                order = Language.getInterface().getCollator().compare(panel.getTitle(), o.panel.getTitle());
            }
            return order;
        }
    }

    private final StylePanel.StyleEditorCallback callback = this::handleStyleChange;

    private void handleStyleChange() {
        updateTimeLatest = System.currentTimeMillis();
//		System.err.println(updateTimeLatest);
        updateDelay.restart();
    }

    /**
     * Returns {@code true} if the specified selection of items contains at
     * least one item with editable style information.
     *
     * @param sel the selection to test
     * @return {@code true} if at least one item can be edited by this
     * dialog
     */
    public static boolean selectionHasStyledItems(PageItem[] sel) {
        if (sel != null) {
            for (PageItem pi : sel) {
                if (pi instanceof Style) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        styleTab = new javax.swing.JTabbedPane();
        okPanel = new javax.swing.JPanel();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        conflictWarning = new javax.swing.JPanel();
        conflictLabel = new javax.swing.JLabel();
        javax.swing.JLabel jLabel0 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("style-l-title")); // NOI18N

        styleTab.setFont(styleTab.getFont().deriveFont(styleTab.getFont().getSize()-1f));
        getContentPane().add(styleTab, java.awt.BorderLayout.CENTER);

        cancelBtn.setText(string("cancel")); // NOI18N

        okBtn.setText(string("apply")); // NOI18N

        conflictWarning.setBackground(new java.awt.Color(255, 255, 204));
        conflictWarning.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.lightGray), javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1)));

        conflictLabel.setIcon( ResourceKit.getIcon( "ui/deck/style-conflict.png" ) );
        conflictLabel.setPreferredSize(new java.awt.Dimension(16, 16));

        jLabel0.setFont(jLabel0.getFont().deriveFont(jLabel0.getFont().getSize()-1f));
        jLabel0.setText(string("style-ed-l-conflict")); // NOI18N

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-1f));
        jLabel2.setText(string("style-ed-l-conflict-overwrite")); // NOI18N

        javax.swing.GroupLayout conflictWarningLayout = new javax.swing.GroupLayout(conflictWarning);
        conflictWarning.setLayout(conflictWarningLayout);
        conflictWarningLayout.setHorizontalGroup(
            conflictWarningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(conflictWarningLayout.createSequentialGroup()
                .addComponent(conflictLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(conflictWarningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(conflictWarningLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel0, javax.swing.GroupLayout.DEFAULT_SIZE, 355, Short.MAX_VALUE))
                .addContainerGap())
        );
        conflictWarningLayout.setVerticalGroup(
            conflictWarningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(conflictWarningLayout.createSequentialGroup()
                .addGroup(conflictWarningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(conflictLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel0))
                .addGap(0, 0, 0)
                .addComponent(jLabel2))
        );

        javax.swing.GroupLayout okPanelLayout = new javax.swing.GroupLayout(okPanel);
        okPanel.setLayout(okPanelLayout);
        okPanelLayout.setHorizontalGroup(
            okPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(okPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(okPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(okPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addComponent(conflictWarning, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        okPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        okPanelLayout.setVerticalGroup(
            okPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, okPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(conflictWarning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(okPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );

        getContentPane().add(okPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JLabel conflictLabel;
    private javax.swing.JPanel conflictWarning;
    private javax.swing.JButton okBtn;
    private javax.swing.JPanel okPanel;
    private javax.swing.JTabbedPane styleTab;
    // End of variables declaration//GEN-END:variables

    private StyleCapture composeNewStyle() {
        StyleCapture cap = new StyleCapture();
        for (int i = 0; i < kits.length; ++i) {
            kits[i].panel.populateCaptureFromPanel(cap);
        }

        return cap;
    }

    private void apply(boolean protectSaveFlag) {
        Deck d = null;
        boolean unsaved = false;
        if (protectSaveFlag) {
            d = selection[0].getPage().getDeck();
            unsaved = d.hasUnsavedChanges();
        }

        StyleCapture capture = composeNewStyle();
        for (PageItem pi : selection) {
            capture.apply(pi);
        }

        if (protectSaveFlag) {
            if (!unsaved) {
                d.markSaved();
            }
        }
    }

    @Override
    public void handleOKAction(ActionEvent e) {
        updateDelay.stop();
        apply(false);
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        updateDelay.stop();
        for (int i = 0; i < selection.length; ++i) {
            cancelCaptures[i].apply(selection[i]);
        }

        dispose();
    }
}
