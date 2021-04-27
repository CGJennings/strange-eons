package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.Length;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.SizablePageItem;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.StyleUtilities;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;
import static resources.Language.string;
import resources.Settings;

/**
 * The palette window that displays the location and size of the selected deck
 * object(s).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class PropertyPalette extends javax.swing.JDialog {

    private Deck deck;
    private DeckEditor ed;
    private NumberFormat formatter;
    private JTextField[] selFields;
    private Rectangle2D.Double lastSelectionRectangle;

    /**
     * Creates an object palette.
     */
    private PropertyPalette() {
        super(StrangeEons.getWindow(), false);
        JUtilities.makeUtilityWindow(this);
        initComponents();

        DeckEditor.localizeComboBoxLabels(unitCombo, null);
        unitCombo.setSelectedIndex(Length.getDefaultUnit());
        formatter = NumberFormat.getNumberInstance();
        formatter.setMinimumFractionDigits(3);
        formatter.setMaximumFractionDigits(3);
        formatter.setRoundingMode(RoundingMode.HALF_EVEN);

        // set the disabled text color to the normal text color for these
        // fields; this keeps them nice and readable when the selected object(s)
        // can't be resized
        widthField.setDisabledTextColor(xField.getForeground());
        heightField.setDisabledTextColor(xField.getForeground());

        // start listening for deck editors to connect to
        StrangeEons.getWindow().addEditorListener(editorListener);

        // there may already be a deck open, in which case connect to it
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed != null) {
            editorListener.editorSelected(ed);
        }
    }

    /**
     * Returns the global <code>PropertyPalette</code> window.
     *
     * @return the shared object palette instance
     */
    public static PropertyPalette getShared() {
        if (shared == null) {
            shared = new PropertyPalette();
        }
        return shared;
    }
    private static PropertyPalette shared;

    private void connect(DeckEditor editor) {
        if (ed != null) {
            disconnect();
        }

        ed = editor;
        deck = ed.getDeck();
        deck.addSelectionListener(selectionListener);
        updateSelectionLocation();
    }

    private void disconnect() {
        if (deck == null) {
            if (xField.isEnabled()) {
                updateSelectionLocationImpl();
            }
            return;
        }

        deck.removeSelectionListener(selectionListener);
        updateSelectionLocation();
        ed = null;
        deck = null;
    }

    //
    // HANDLE SELECTION CHANGES IN DECK
    //
    private Timer selectionTimer = new Timer(250, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (updateSelectionLocationPending) {
                updateSelectionLocationImpl();
            }
        }
    });

    private Deck.SelectionChangeListener selectionListener = new Deck.SelectionChangeListener() {
        @Override
        public void deckSelectionChanged(Deck source) {
            if (source != deck) {
                StrangeEons.log.warning("got event from wrong deck: " + deck);
                source.removeSelectionListener(this);
                return;
            }
            updateSelectionLocation();
        }
    };

    private void updateSelectionLocation() {
        updateSelectionLocationPending = true;
    }
    private boolean updateSelectionLocationPending = false;

    private void updateSelectionLocationImpl() {
        updateSelectionLocationPending = false;

        Rectangle2D.Double r = deck == null ? null : deck.getSelectionRectangle();
        if (r == null) {
            JUtilities.enableTree(propertyPanel, false);
            for (int i = 0; i < selFields.length; ++i) {
                selFields[i].setText("");
            }
        } else {
            JUtilities.enableTree(propertyPanel, true);
            xField.setText(formatter.format(ptsToUnit(r.x)));
            yField.setText(formatter.format(ptsToUnit(r.y)));
            widthField.setText(formatter.format(ptsToUnit(r.width)));
            heightField.setText(formatter.format(ptsToUnit(r.height)));

            boolean enableSize = deck.getSelectionSize() == 1 && deck.getSelection()[0] instanceof SizablePageItem;
            widthField.setEnabled(enableSize);
            heightField.setEnabled(enableSize);
        }
        lastSelectionRectangle = r;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            if (!hasBeenVisible) {
                hasBeenVisible = true;
                if (!Settings.getUser().applyWindowSettings("deck-palette", this)) {
                    Rectangle b = StrangeEons.getWindow().getBounds();
                    setLocation((b.x + b.width) - (getWidth() + 16), (b.y + b.height) - (getHeight() + 16));
                }
            }

            StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
            if (ed instanceof DeckEditor) {
                connect((DeckEditor) ed);
            } else {
                disconnect();
            }

            selectionTimer.start();
        } else {
            Settings.getUser().storeWindowSettings("deck-palette", this);
            selectionTimer.stop();
        }
        super.setVisible(visible);
    }
    private boolean hasBeenVisible;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        propertyPanel = new javax.swing.JPanel();
        xLabel = new javax.swing.JLabel();
        xField = new javax.swing.JTextField();
        yLabel = new javax.swing.JLabel();
        yField = new javax.swing.JTextField();
        widthLabel = new javax.swing.JLabel();
        heightLabel = new javax.swing.JLabel();
        widthField = new javax.swing.JTextField();
        heightField = new javax.swing.JTextField();
        unitCombo = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("de-opal-title")); // NOI18N
        setResizable(false);

        propertyPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3));

        xLabel.setText("x");

        xField.setColumns(9);
        xField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        xField.setText("0");
        xField.setMargin(new java.awt.Insets(1, 1, 1, 1));
        xField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationFieldActionPerformed(evt);
            }
        });
        xField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                locationFieldFocusLost(evt);
            }
        });

        yLabel.setText("y");

        yField.setColumns(9);
        yField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        yField.setText("0");
        yField.setMargin(new java.awt.Insets(1, 1, 1, 1));
        yField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationFieldActionPerformed(evt);
            }
        });
        yField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                locationFieldFocusLost(evt);
            }
        });

        StyleUtilities.mini( widthLabel );
        widthLabel.setText(string("de-card-width")); // NOI18N

        heightLabel.setText(string("de-card-height")); // NOI18N

        widthField.setColumns(9);
        widthField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        widthField.setText("0");
        widthField.setMargin(new java.awt.Insets(1, 1, 1, 1));
        widthField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationFieldActionPerformed(evt);
            }
        });
        widthField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                locationFieldFocusLost(evt);
            }
        });

        heightField.setColumns(9);
        heightField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        heightField.setText("0");
        heightField.setMargin(new java.awt.Insets(1, 1, 1, 1));
        heightField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationFieldActionPerformed(evt);
            }
        });
        heightField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                locationFieldFocusLost(evt);
            }
        });

        unitCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "iid-cb-unit0", "iid-cb-unit1", "iid-cb-unit2" }));
        unitCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitComboActionPerformed(evt);
            }
        });

        jLabel2.setText(string("de-card-unit")); // NOI18N

        javax.swing.GroupLayout propertyPanelLayout = new javax.swing.GroupLayout(propertyPanel);
        propertyPanel.setLayout(propertyPanelLayout);
        propertyPanelLayout.setHorizontalGroup(
            propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(propertyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(propertyPanelLayout.createSequentialGroup()
                        .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(xLabel)
                            .addComponent(yLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(yField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(xField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(widthLabel)
                            .addComponent(heightLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, propertyPanelLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        propertyPanelLayout.setVerticalGroup(
            propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(propertyPanelLayout.createSequentialGroup()
                .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(xLabel)
                    .addComponent(xField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(widthLabel)
                    .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(yField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(yLabel)
                    .addComponent(heightLabel)
                    .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(propertyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addContainerGap())
        );

        StyleUtilities.sizeTree( propertyPanel, StyleUtilities.SMALL );
        selFields = new JTextField[] { xField, yField, widthField, heightField };

        getContentPane().add(propertyPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void locationFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationFieldActionPerformed
        if (lastSelectionRectangle == null || deck == null) {
            return;
        }

        JTextField src = (JTextField) evt.getSource();
        Rectangle2D.Double r = lastSelectionRectangle;
        try {
            if (src == xField || src == yField) {
                double x = unitToPts(formatter.parse(xField.getText()).doubleValue());
                double y = unitToPts(formatter.parse(yField.getText()).doubleValue());
                deck.nudgeSelection(x - r.x, y - r.y);
            } else {
                PageItem[] sel = deck.getSelection();
                if (sel.length != 1 || !(sel[0] instanceof SizablePageItem)) {
                    updateSelectionLocation();
                    return;
                }
                double w = unitToPts(formatter.parse(widthField.getText()).doubleValue());
                double h = unitToPts(formatter.parse(heightField.getText()).doubleValue());
                if ((w < 0.5d) && (h < 0.5d)) {
                    w = 0.5d;
                    h = 0.5d;
                }

                ((SizablePageItem) sel[0]).setSize(w, h);
            }
        } catch (ParseException e) {
            UIManager.getLookAndFeel().provideErrorFeedback(src);
        }
        updateSelectionLocation();
    }//GEN-LAST:event_locationFieldActionPerformed

    private void locationFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_locationFieldFocusLost
        locationFieldActionPerformed(new ActionEvent(evt.getSource(), 0, null));
    }//GEN-LAST:event_locationFieldFocusLost

    private void unitComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitComboActionPerformed
        if (deck == null) {
            return;
        }
        updateSelectionLocation();
    }//GEN-LAST:event_unitComboActionPerformed

    private double ptsToUnit(double v) {
        int u = unitCombo.getSelectedIndex();
        return Length.convert(v, Length.PT, u);
    }

    private double unitToPts(double v) {
        int u = unitCombo.getSelectedIndex();
        return Length.convert(v, u, Length.PT);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField heightField;
    private javax.swing.JLabel heightLabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel propertyPanel;
    private javax.swing.JComboBox unitCombo;
    private javax.swing.JTextField widthField;
    private javax.swing.JLabel widthLabel;
    private javax.swing.JTextField xField;
    private javax.swing.JLabel xLabel;
    private javax.swing.JTextField yField;
    private javax.swing.JLabel yLabel;
    // End of variables declaration//GEN-END:variables

    private StrangeEonsEditor.EditorListener editorListener = new StrangeEonsEditor.EditorListener() {
        @Override
        public void editorSelected(StrangeEonsEditor editor) {
            if (editor instanceof DeckEditor) {
                connect((DeckEditor) editor);
            }
        }

        @Override
        public void editorDeselected(StrangeEonsEditor editor) {
            disconnect();
        }

        @Override
        public void editorClosing(StrangeEonsEditor editor) {
        }

        @Override
        public void editorDetached(StrangeEonsEditor editor) {
        }

        @Override
        public void editorAttached(StrangeEonsEditor editor) {
        }
    };
}
