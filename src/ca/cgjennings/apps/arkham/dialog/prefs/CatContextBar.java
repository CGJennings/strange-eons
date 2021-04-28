package ca.cgjennings.apps.arkham.dialog.prefs;

import ca.cgjennings.apps.arkham.ContextBar;
import ca.cgjennings.apps.arkham.ContextBar.Button;
import ca.cgjennings.apps.arkham.ContextBar.Context;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.ListTransferHandler;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * Category panel for the context-sensitive tool bar.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class CatContextBar extends javax.swing.JPanel implements PreferenceCategory {

    private final Color BAR_BACKGROUND;
    private final Color BAR_FOREGROUND;
    private final Color BUTTON_BACKGROUND;
    private final Color BUTTON_ROLLOVER_BACKGROUND;
    private final Color BUTTON_ROLLOVER_FOREGROUND;
    private final Color BUTTON_ARMED_FOREGROUND;

    {
        UIDefaults uid = UIManager.getDefaults();
        BAR_BACKGROUND = uid.getColor(Theme.CONTEXT_BAR_BACKGROUND);
        BAR_FOREGROUND = uid.getColor(Theme.CONTEXT_BAR_FOREGROUND);
        BUTTON_BACKGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_BACKGROUND);
        BUTTON_ROLLOVER_BACKGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_ROLLOVER_BACKGROUND);
        BUTTON_ROLLOVER_FOREGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_ROLLOVER_OUTLINE_FOREGROUND);
        BUTTON_ARMED_FOREGROUND = uid.getColor(Theme.CONTEXT_BAR_BUTTON_ARMED_OUTLINE_FOREGROUND);
    }

    // used to prevent dragging from src list to rubbish
    private boolean isDraggingFromSrcList;

    /**
     * Creates new form CatLanguage
     */
    public CatContextBar() {
        initComponents();

        dstScroll.setRowHeaderView(fauxCollapseBtn);

        srcList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Button b = (Button) value;
                String label = b.getName();
                if (label == null) {
                    label = "<null>";
                    StrangeEons.log.warning("null button label " + b);
                }
                if (label.endsWith("...")) {
                    label = label.substring(0, label.length() - 3);
                } else if (!label.isEmpty() && label.charAt(label.length() - 1) == '\u2026') {
                    label = label.substring(0, label.length() - 1);
                }
                super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                setIcon(fixIcon(b.getIcon()));
                return this;
            }
        });

        final ListTransferHandler srcHandler = new ListTransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return false;
            }

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
            }

            @Override
            public void exportAsDrag(JComponent comp, InputEvent e, int action) {
                isDraggingFromSrcList = true;
                super.exportAsDrag(comp, e, action);
                srcList.setAutoscrolls(false);
            }

            @Override
            protected void exportDone(JComponent c, Transferable data, int action) {
                isDraggingFromSrcList = false;
                if (action == TransferHandler.NONE) {
                    return;
                }

                ArrayList alist = getTransferData(data);
                if (alist.size() > 0 && alist.get(0) != fakeSeparatorButton) {
                    action = TransferHandler.MOVE;
                } else {
                    action = TransferHandler.COPY;
                }
                super.exportDone(c, data, action);
            }
        };

        final ListTransferHandler dstHandler = new ListTransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            @Override
            public boolean canImport(TransferSupport support) {
                if (super.canImport(support)) {
                    if (support.isDrop()) {
                        support.setDropAction(TransferHandler.MOVE);
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected void exportDone(JComponent c, Transferable data, int action) {
                if (action != TransferHandler.NONE) {
                    action = TransferHandler.MOVE;
                }
                super.exportDone(c, data, action);
            }

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
            }
        };

        srcList.setTransferHandler(srcHandler);
        dstList.setTransferHandler(dstHandler);

        TransferHandler rubbishHandler = new ListTransferHandler.RubbishTransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                // prevent dragging from source list to rubbish bin
                if (isDraggingFromSrcList || !support.isDrop()) {
                    return false;
                }

                if ((support.getSourceDropActions() & TransferHandler.MOVE) == TransferHandler.MOVE) {
                    support.setDropAction(TransferHandler.MOVE);
                    return true;
                }
                return false;
            }

            @Override
            public boolean importData(TransferSupport transfer) {
                final ArrayList list = getTransferData(transfer.getTransferable());
                EventQueue.invokeLater(() -> {
                    for (Object o : list) {
                        deleteButton((Button) o);
                    }
                });
                return true;
            }
        };
        bin.setTransferHandler(rubbishHandler);
        setTransferHandler(rubbishHandler);

        removeActions(srcList);
        removeActions(dstList);
        removeActions(bin);
    }

    private static void removeActions(JComponent c) {
        int[] keys = new int[]{
            KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_X
        };

        InputMap im = c.getInputMap();
        ActionMap am = c.getActionMap();
        for (int i = 0; i < keys.length; ++i) {
            int mask = KeyEvent.CTRL_DOWN_MASK;
            for (int m = 0; m < 2; ++m) {
                KeyStroke key = KeyStroke.getKeyStroke(keys[i], mask);
                Object actionKey = im.get(key);
                if (actionKey != null) {
                    im.remove(key);
                    am.remove(actionKey);
                }
                mask = KeyEvent.META_DOWN_MASK;
            }
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

        fauxCollapseBtn = new javax.swing.JLabel();
        javax.swing.JLabel barSect = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        srcScroll = new javax.swing.JScrollPane();
        srcList = new javax.swing.JList();
        dstScroll = new javax.swing.JScrollPane();
        dstList = new JList() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width += 64; // space to easily add more icons
                d.height = 22; // 3 + 3 + 16
                return d;
            }
        };
        bin = new javax.swing.JLabel();
        removeAllBtn = new javax.swing.JButton();

        fauxCollapseBtn.setBackground( BAR_BACKGROUND );
        fauxCollapseBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/icons/toolbar/hide.png"))); // NOI18N
        fauxCollapseBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 1, 2, 4));
        fauxCollapseBtn.setOpaque(true);

        setBackground(java.awt.Color.white);

        barSect.setFont(barSect.getFont().deriveFont(barSect.getFont().getStyle() | java.awt.Font.BOLD, barSect.getFont().getSize()+3));
        barSect.setForeground(new java.awt.Color(135, 103, 5));
        barSect.setText(string("sd-l-toolbar")); // NOI18N

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()-1f));
        jLabel1.setText(string("sd-b-info")); // NOI18N
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        srcList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        srcList.setAutoscrolls(false);
        srcList.setDragEnabled(true);
        srcList.setVisibleRowCount(-1);
        srcScroll.setViewportView(srcList);

        dstScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        dstScroll.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        dstList.setBackground( BAR_BACKGROUND );
        dstList.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        dstList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        dstList.setCellRenderer( barRenderer );
        dstList.setDragEnabled(true);
        dstList.setDropMode(javax.swing.DropMode.INSERT);
        dstList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        dstList.setVisibleRowCount(1);
        dstScroll.setViewportView(dstList);

        bin.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        bin.setIcon( ResourceKit.getIcon( "ui/button/delete.png" ) );
        bin.setText(string("remove")); // NOI18N

        removeAllBtn.setFont(removeAllBtn.getFont().deriveFont(removeAllBtn.getFont().getSize()-1f));
        removeAllBtn.setText(string("sd-b-context-clear")); // NOI18N
        removeAllBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 405, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(barSect))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(30, 30, 30)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(bin)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(removeAllBtn))
                                .addComponent(dstScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(srcScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 395, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(23, 23, 23))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {dstScroll, srcScroll});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(barSect)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(srcScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 213, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(dstScroll, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(removeAllBtn, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(bin, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

	private void removeAllBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAllBtnActionPerformed
            DefaultListModel model = (DefaultListModel) dstList.getModel();
            for (int i = 0; i < model.getSize(); ++i) {
                Object o = model.get(i);
                deleteButton((Button) o);
            }
            model.clear();
	}//GEN-LAST:event_removeAllBtnActionPerformed

    /**
     * "Deletes" a button, restoring it to srcList if not already present. Does
     * not remove the button from, e.g., dstList.
     *
     * @param b
     */
    private void deleteButton(Button b) {
        DefaultListModel m = (DefaultListModel) srcList.getModel();
        int i;
        for (i = 0; i < m.getSize(); ++i) {
            if (m.get(i) == b) {
                break;
            }
        }
        if (i == m.getSize()) {
            m.addElement(b);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bin;
    private javax.swing.JList dstList;
    private javax.swing.JScrollPane dstScroll;
    private javax.swing.JLabel fauxCollapseBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton removeAllBtn;
    private javax.swing.JList srcList;
    private javax.swing.JScrollPane srcScroll;
    // End of variables declaration//GEN-END:variables

    @Override
    public String getTitle() {
        return string("sd-l-toolbar");
    }

    @Override
    public Icon getIcon() {
        if (icon == null) {
            icon = ResourceKit.getIcon("application/prefs-contextbar.png");
        }
        return icon;
    }
    private Icon icon;

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void loadSettings() {
        // reset the models and reload buttons from current config

        DefaultListModel srcModel = new DefaultListModel();
        DefaultListModel dstModel = new DefaultListModel();

        // always present
        srcModel.addElement(fakeSeparatorButton);

        Button[] buttons = ContextBar.getRegisteredButtons();
        Button[] usedButtons = ContextBar.getShared().getButtons();

        //
        for (Button u : usedButtons) {
            if (u == null) {
                dstModel.addElement(fakeSeparatorButton);
            } else {
                dstModel.addElement(u);
            }
        }

        // add all registered buttons to srcList, except those already on bar
        for (Button b : buttons) {
            int i;
            for (i = 0; i < usedButtons.length; ++i) {
                final Button u = usedButtons[i];
                if (u == null) {
                    continue; // skip separator
                }
                if (u == b) {
                    break;
                }
            }
            if (i == usedButtons.length) {
                StrangeEons.log.log(Level.INFO, "added {0} ({1}) to list", new Object[]{b.getID(), b.getName()});
                srcModel.addElement(b);
            } else {
                StrangeEons.log.log(Level.INFO, "added {0} ({1}) to bar", new Object[]{b.getID(), b.getName()});
            }
        }

        srcList.setModel(srcModel);
        dstList.setModel(dstModel);
    }

    @Override
    public void storeSettings() {
        Settings s = Settings.getUser();

        DefaultListModel m = (DefaultListModel) dstList.getModel();
        Button[] buttons = new Button[m.getSize()];
        for (int i = 0; i < buttons.length; ++i) {
            Object o = m.get(i);
            if (o == fakeSeparatorButton) {
                buttons[i] = null;
            } else {
                buttons[i] = (Button) o;
            }
        }

        s.set(ContextBar.BUTTONS_SETTING, ContextBar.toButtonDescription(buttons));
        ContextBar.getShared().setButtons(buttons);

        standinIcons = null;
    }

    @Override
    public boolean isRestartRequired() {
        return false;
    }

    private Button fakeSeparatorButton = new Button() {
        private final Icon icon = new ImageIcon(getClass().getResource("/resources/icons/toolbar/separator.png"));

        @Override
        public String getName() {
            return string("sd-b-separator");
        }

        @Override
        public String getID() {
            return null;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public boolean isEnabledInCurrentContext(Context context) {
            return true;
        }

        @Override
        public boolean isVisibleInCurrentContext(Context context) {
            return true;
        }

        @Override
        public void onAttach(Context context) {
        }

        @Override
        public void onDetach(Context context) {
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    };

    private DefaultListCellRenderer barRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            setText("");
            setIcon(fixIcon(((IconProvider) value).getIcon()));
            setOpaque(true);
            setBackground(isSelected ? BUTTON_ROLLOVER_BACKGROUND : BUTTON_BACKGROUND);
            setBorder(isSelected ? selBorder : btnBorder);
            return this;
        }
    };
    private final Border btnBorder = BorderFactory.createLineBorder(BAR_BACKGROUND, 2);
    private final Border selBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BAR_BACKGROUND, 1),
            BorderFactory.createLineBorder(BUTTON_ROLLOVER_FOREGROUND, 1)
    );

    private Icon fixIcon(Icon source) {
        if (source.getIconWidth() == ICON_SIZE && source.getIconHeight() == ICON_SIZE) {
            return source;
        }

        Icon fixed = null;
        if (standinIcons == null) {
            standinIcons = new HashMap<>();
        } else {
            fixed = standinIcons.get(source);
        }

        if (fixed == null) {
            fixed = ImageUtilities.ensureIconHasSize(source, ICON_SIZE);
            standinIcons.put(source, fixed);
        }

        return fixed;
    }
    private HashMap<Icon, Icon> standinIcons;
    private static final int ICON_SIZE = ContextBar.getPreferredContextBarIconSize();
}
