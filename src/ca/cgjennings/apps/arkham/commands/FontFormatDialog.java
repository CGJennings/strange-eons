package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.dialog.InsertCharsDialog;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JUtilities;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.font.TextAttribute;
import static java.awt.font.TextAttribute.*;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SpinnerNumberModel;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Dialog for inserting font markup.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
class FontFormatDialog extends javax.swing.JDialog implements AgnosticDialog {
    private class FontTokenRenderer extends DefaultListCellRenderer {
        private Font listFont, regListFont;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Font f = ((FontToken) value).getFont();
            String name = f.getFamily(Locale.getDefault());

            super.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);

            if (listFont == null) {
                listFont = list.getFont();
                regListFont = listFont.deriveFont(Font.BOLD);
            }

            Font itemFont;
            if (ResourceKit.isFamilyRegistered(f.getFamily())) {
                itemFont = regListFont;
            } else {
                itemFont = listFont;
            }

            setFont(itemFont);

            setToolTipText(name);

            // if we can print the family name with the font and the resulting
            // string doesn't render crazy huge (this was an issue), do so,
            // otherwise use a standard UI font
            boolean canDisplay = f.canDisplayUpTo(name) == -1;
            if (canDisplay) {
                setFont(f);
            } else {
                setFont(familyList.getFont());
            }

            dividerFlag = index == registeredFontCount;

            return this;
        }

        @Override
        protected void paintComponent(Graphics g1) {
            super.paintComponent(g1);
            if (dividerFlag) {
                Graphics2D g = (Graphics2D) g1;
                Stroke old = g.getStroke();
                g.setStroke(dividerStroke);
                g.setPaint(dividerColor);
                g.drawLine(0, 0, getWidth(), 0);
                g.setStroke(old);
            }
        }

        private final BasicStroke dividerStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10f, new float[]{6f, 6f}, 0f);
        private final Color dividerColor = Color.GRAY;
        private boolean dividerFlag;
    };

    /**
     * Creates new form FontFormatDialog
     */
    public FontFormatDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        AbstractGameComponentEditor.localizeComboBoxLabels(widthCombo, null);
        AbstractGameComponentEditor.localizeComboBoxLabels(sizeTypeCombo, null);
        
        familyList.setCellRenderer(new FontTokenRenderer());

        doneInit = true;
        pack();
        updateSample();

        Thread familyLoader = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                String[] names = ge.getAvailableFontFamilyNames();
                Arrays.sort(names, fontSorter);
                final DefaultListModel<FontToken> model = new DefaultListModel<>();
                for (int i = 0; i < names.length; ++i) {
                    model.addElement(new FontToken(names[i]));
                    if (registeredFontCount == -1 && !ResourceKit.isFamilyRegistered(names[i])) {
                        registeredFontCount = i;
                    }
                }
                EventQueue.invokeLater(() -> {
                    familyList.setModel(model);
                    ((CardLayout) loadingPanel.getLayout()).show(loadingPanel, "list");
                    dncFamily.setEnabled(true);
                });
            }
            private final Comparator<String> fontSorter = new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (ResourceKit.isFamilyRegistered(o1)) {
                        if (ResourceKit.isFamilyRegistered(o2)) {
                            return collator.compare(o1, o2);
                        }
                        return -1;
                    } else if (ResourceKit.isFamilyRegistered(o2)) {
                        return 1;
                    }
                    return collator.compare(o1, o2);
                }
                Collator collator = Language.getInterface().getCollator();
            };
        };
        familyLoader.setPriority(Thread.MIN_PRIORITY);
        familyLoader.start();
    }

    private int registeredFontCount = -1;
    private static final int FONT_LIST_ROW_HEIGHT = 24;

    private static class FontToken {

        private String name;
        private Font font;

        public FontToken(String name) {
            this.name = name;
        }

        public Font getFont() {
            if (font == null) {
                font = new Font(name, Font.PLAIN, 18);
                name = null;
            }
            return font;
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

        familyPanel = new javax.swing.JPanel();
        dncFamily = new javax.swing.JCheckBox();
        glyphsBtn = new javax.swing.JButton();
        loadingPanel = new javax.swing.JPanel();
        loadingLabel = new javax.swing.JLabel();
        familyScroll = new javax.swing.JScrollPane();
        familyList = new javax.swing.JList<>();
        sizePanel = new javax.swing.JPanel();
        dncSize = new javax.swing.JCheckBox();
        sizeField = new javax.swing.JSpinner();
        sizeTypeCombo = new javax.swing.JComboBox<>();
        stylePanel = new javax.swing.JPanel();
        bCheck = new javax.swing.JCheckBox();
        iCheck = new javax.swing.JCheckBox();
        uCheck = new javax.swing.JCheckBox();
        sCheck = new javax.swing.JCheckBox();
        supCheck = new javax.swing.JCheckBox();
        subCheck = new javax.swing.JCheckBox();
        widthPanel = new javax.swing.JPanel();
        widthCombo = new javax.swing.JComboBox<>();
        dncWidth = new javax.swing.JCheckBox();
        trackingPanel = new javax.swing.JPanel();
        dncTracking = new javax.swing.JCheckBox();
        trackingField = new javax.swing.JSpinner();
        okCancelPanel = new javax.swing.JPanel();
        okBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        overlayPanel = new ca.cgjennings.apps.arkham.dialog.OverlayPanel();
        sampleLabel = new FontSampleLabel() ;

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "ffd-l-title" )); // NOI18N
        setResizable(false);

        familyPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "ffd-l-family" ))); // NOI18N

        dncFamily.setSelected(true);
        dncFamily.setText(string( "ffd-l-no-change" )); // NOI18N
        dncFamily.setEnabled(false);
        dncFamily.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dncActionPerformed(evt);
            }
        });

        glyphsBtn.setIcon( ResourceKit.getIcon( "toolbar/inspect-font.png" ) );
        glyphsBtn.setEnabled(false);
        glyphsBtn.setMargin(new java.awt.Insets(1, 2, 1, 2));
        glyphsBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                glyphsBtnActionPerformed(evt);
            }
        });

        loadingPanel.setLayout(new java.awt.CardLayout());

        loadingLabel.setBackground(java.awt.Color.white);
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(loadingLabel.getFont().getSize()-1f));
        loadingLabel.setForeground(new java.awt.Color(0, 0, 3));
        loadingLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        loadingLabel.setIcon( resources.ResourceKit.createWaitIcon( loadingLabel ) );
        loadingLabel.setText(string( "ffd-l-loading" )); // NOI18N
        loadingLabel.setBorder(familyScroll.getBorder());
        loadingLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        loadingLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        loadingLabel.setIconTextGap(16);
        loadingLabel.setOpaque(true);
        loadingLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        loadingPanel.add(loadingLabel, "wait");

        familyList.setFont(familyList.getFont().deriveFont((float)20));
        familyList.setFixedCellHeight( FONT_LIST_ROW_HEIGHT );
        familyList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                familyListValueChanged(evt);
            }
        });
        familyScroll.setViewportView(familyList);

        loadingPanel.add(familyScroll, "list");

        javax.swing.GroupLayout familyPanelLayout = new javax.swing.GroupLayout(familyPanel);
        familyPanel.setLayout(familyPanelLayout);
        familyPanelLayout.setHorizontalGroup(
            familyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(familyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(familyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(loadingPanel, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, familyPanelLayout.createSequentialGroup()
                        .addComponent(dncFamily)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(glyphsBtn)))
                .addContainerGap())
        );
        familyPanelLayout.setVerticalGroup(
            familyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(familyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(familyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(dncFamily)
                    .addComponent(glyphsBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        sizePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "ffd-l-size" ))); // NOI18N

        dncSize.setSelected(true);
        dncSize.setText(string( "ffd-l-no-change" )); // NOI18N
        dncSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dncActionPerformed(evt);
            }
        });

        sizeField.setModel(new javax.swing.SpinnerNumberModel(12.0d, 1.0d, 288.0d, 0.5d));
        sizeField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sizeFieldStateChanged(evt);
            }
        });

        sizeTypeCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ffd-cb-size-0", "ffd-cb-size-1" }));
        sizeTypeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sizeTypeComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sizePanelLayout = new javax.swing.GroupLayout(sizePanel);
        sizePanel.setLayout(sizePanelLayout);
        sizePanelLayout.setHorizontalGroup(
            sizePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sizePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sizePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sizePanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(sizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sizeTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(dncSize))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        sizePanelLayout.setVerticalGroup(
            sizePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sizePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(dncSize)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sizePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sizeTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        stylePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "ffd-l-style" ))); // NOI18N

        bCheck.setFont(bCheck.getFont().deriveFont(bCheck.getFont().getStyle() | java.awt.Font.BOLD));
        bCheck.setText(string( "ffd-l-bold" )); // NOI18N
        bCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                styleCheckActionPerformed(evt);
            }
        });

        iCheck.setFont(iCheck.getFont().deriveFont((iCheck.getFont().getStyle() | java.awt.Font.ITALIC)));
        iCheck.setText(string( "ffd-l-italic" )); // NOI18N
        iCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                styleCheckActionPerformed(evt);
            }
        });

        uCheck.setText(string( "ffd-l-underline" )); // NOI18N
        uCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                styleCheckActionPerformed(evt);
            }
        });

        sCheck.setText(string( "ffd-l-strikethrough" )); // NOI18N
        sCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                styleCheckActionPerformed(evt);
            }
        });

        supCheck.setText(string( "ffd-l-super" )); // NOI18N
        supCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                supCheckActionPerformed(evt);
            }
        });

        subCheck.setText(string( "ffd-l-sub" )); // NOI18N
        subCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subCheckActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout stylePanelLayout = new javax.swing.GroupLayout(stylePanel);
        stylePanel.setLayout(stylePanelLayout);
        stylePanelLayout.setHorizontalGroup(
            stylePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(stylePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(stylePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(bCheck)
                    .addComponent(iCheck)
                    .addComponent(uCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(stylePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(supCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        stylePanelLayout.setVerticalGroup(
            stylePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(stylePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(stylePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(bCheck)
                    .addComponent(sCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(stylePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(supCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(iCheck))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(stylePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(uCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subCheck, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        widthPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "ffd-l-width" ))); // NOI18N

        widthCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ffd-cb-width-0", "ffd-cb-width-1", "ffd-cb-width-2", "ffd-cb-width-3", "ffd-cb-width-4" }));
        widthCombo.setSelectedIndex(2);
        widthCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                widthComboActionPerformed(evt);
            }
        });

        dncWidth.setSelected(true);
        dncWidth.setText(string( "ffd-l-no-change" )); // NOI18N
        dncWidth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dncActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout widthPanelLayout = new javax.swing.GroupLayout(widthPanel);
        widthPanel.setLayout(widthPanelLayout);
        widthPanelLayout.setHorizontalGroup(
            widthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(widthPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(widthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(widthPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(widthCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(dncWidth))
                .addContainerGap())
        );
        widthPanelLayout.setVerticalGroup(
            widthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(widthPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(dncWidth)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(widthCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        trackingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string( "ffd-l-tracking" ))); // NOI18N

        dncTracking.setSelected(true);
        dncTracking.setText(string( "ffd-l-no-change" )); // NOI18N
        dncTracking.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dncActionPerformed(evt);
            }
        });

        trackingField.setModel(new javax.swing.SpinnerNumberModel(0.0d, -0.1d, 1.2d, 0.025d));
        trackingField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                trackingFieldStateChanged(evt);
            }
        });

        javax.swing.GroupLayout trackingPanelLayout = new javax.swing.GroupLayout(trackingPanel);
        trackingPanel.setLayout(trackingPanelLayout);
        trackingPanelLayout.setHorizontalGroup(
            trackingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(trackingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(trackingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(trackingPanelLayout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(trackingField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(dncTracking))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        trackingPanelLayout.setVerticalGroup(
            trackingPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(trackingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(dncTracking)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(trackingField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        okBtn.setText(string( "ffd-b-ok" )); // NOI18N

        cancelBtn.setText(string( "cancel" )); // NOI18N

        javax.swing.GroupLayout okCancelPanelLayout = new javax.swing.GroupLayout(okCancelPanel);
        okCancelPanel.setLayout(okCancelPanelLayout);
        okCancelPanelLayout.setHorizontalGroup(
            okCancelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(okCancelPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(okBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelBtn)
                .addContainerGap())
        );
        okCancelPanelLayout.setVerticalGroup(
            okCancelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(okCancelPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(okCancelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelBtn)
                    .addComponent(okBtn))
                .addContainerGap())
        );

        sampleLabel.setText(string( "ffd-l-sample-text" )); // NOI18N
        sampleLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));

        javax.swing.GroupLayout overlayPanelLayout = new javax.swing.GroupLayout(overlayPanel);
        overlayPanel.setLayout(overlayPanelLayout);
        overlayPanelLayout.setHorizontalGroup(
            overlayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overlayPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(sampleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 338, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10))
        );
        overlayPanelLayout.setVerticalGroup(
            overlayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overlayPanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(sampleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(familyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(overlayPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(sizePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(widthPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stylePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(trackingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(okCancelPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sizePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(widthPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(stylePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(trackingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(familyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(overlayPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(okCancelPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private String[] markup;

    public String[] showDialog() {
        markup = null;
        setVisible(true);
        return markup;
    }

    private String[] createMarkup() {
        StringBuilder lhs = new StringBuilder();
        StringBuilder rhs = new StringBuilder();
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(1);

        if (!dncFamily.isSelected()) {
            add(lhs, rhs,
                    "<family \"" + ((FontToken) (familyList.getSelectedValue())).getFont().getFamily() + "\">",
                    "</family>"
            );
        }

        if (!dncSize.isSelected()) {
            add(lhs, rhs,
                    "<size " + nf.format(sizeField.getValue()) + (sizeTypeCombo.getSelectedIndex() == 1 ? "%" : "") + ">",
                    "</size>"
            );
        }

        if (bCheck.isSelected()) {
            add(lhs, rhs, "<b>", "</b>");
        }
        if (iCheck.isSelected()) {
            add(lhs, rhs, "<i>", "</i>");
        }
        if (uCheck.isSelected()) {
            add(lhs, rhs, "<u>", "</u>");
        }
        if (sCheck.isSelected()) {
            add(lhs, rhs, "<del>", "</del>");
        }
        if (supCheck.isSelected()) {
            add(lhs, rhs, "<sup>", "</sup>");
        }
        if (subCheck.isSelected()) {
            add(lhs, rhs, "<sub>", "</sub>");
        }

        if (!dncWidth.isSelected()) {
            final String[] names = new String[]{
                "condensed", "semicondensed", "regular", "semiextended", "extended"
            };
            add(lhs, rhs,
                    "<width " + names[widthCombo.getSelectedIndex()] + ">",
                    "</width>"
            );
        }

        nf.setMaximumFractionDigits(2);
        if (!dncTracking.isSelected()) {
            add(lhs, rhs,
                    "<tracking " + nf.format(trackingField.getValue()) + ">",
                    "</tracking>"
            );
        }
        return new String[]{lhs.toString(), rhs.toString()};
    }

    private static final float BASE_SIZE = 12f;

    private void updateSample() {
        Font f;
        if (!dncFamily.isSelected()) {
            f = ((FontToken) familyList.getSelectedValue()).getFont();
        } else {
            f = new Font(Font.SANS_SERIF, Font.PLAIN, (int) BASE_SIZE);
        }

        HashMap<TextAttribute, Object> attr = new HashMap<>();

        float size;
        if (!dncSize.isSelected()) {
            size = ((Number) sizeField.getValue()).floatValue();
            if (sizeTypeCombo.getSelectedIndex() == 1) {
                size = BASE_SIZE * size / 100;
            }
//			f = f.deriveFont( size );
        } else {
            size = BASE_SIZE;
        }
        // scale the size up a fixed amount so that it is easier to inspect
        attr.put(SIZE, size * 2f);

        attr.put(KERNING, KERNING_ON);
        attr.put(LIGATURES, LIGATURES_ON);

        add(attr, bCheck, WEIGHT, WEIGHT_BOLD);
        add(attr, iCheck, POSTURE, POSTURE_OBLIQUE);
        add(attr, uCheck, UNDERLINE, UNDERLINE_ON);
        add(attr, sCheck, STRIKETHROUGH, STRIKETHROUGH_ON);
        add(attr, supCheck, SUPERSCRIPT, SUPERSCRIPT_SUPER);
        add(attr, subCheck, SUPERSCRIPT, SUPERSCRIPT_SUB);

        if (!dncWidth.isSelected()) {
            final float[] widths = new float[]{
                WIDTH_CONDENSED,
                WIDTH_SEMI_CONDENSED,
                WIDTH_REGULAR,
                WIDTH_SEMI_EXTENDED,
                WIDTH_EXTENDED,};
            attr.put(TextAttribute.WIDTH, widths[widthCombo.getSelectedIndex()]);
        }
        if (!dncTracking.isSelected()) {
            attr.put(TRACKING, trackingField.getValue());
        }
        sampleLabel.setFont(f.deriveFont(attr));
    }

    private void add(HashMap<TextAttribute, Object> map, JCheckBox box, TextAttribute key, Object value) {
        if (box.isSelected()) {
            map.put(key, value);
        }
    }

    private Font apply(Font f, TextAttribute ta, Object value) {
        return f.deriveFont(Collections.singletonMap(ta, value));
    }

    private void add(StringBuilder lhs, StringBuilder rhs, String open, String close) {
        lhs.append(open);
        rhs.insert(0, close);
    }

	private void supCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_supCheckActionPerformed
            subCheck.setSelected(false);
            updateSample();
	}//GEN-LAST:event_supCheckActionPerformed

	private void subCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subCheckActionPerformed
            supCheck.setSelected(false);
            updateSample();
	}//GEN-LAST:event_subCheckActionPerformed

	private void widthComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_widthComboActionPerformed
            if (doneInit) {
                dncWidth.setSelected(false);
                updateSample();
            }
	}//GEN-LAST:event_widthComboActionPerformed

	private void familyListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_familyListValueChanged
            if (evt.getValueIsAdjusting()) {
                return;
            }
            int i = familyList.getSelectedIndex();
            if (i >= 0) {
                dncFamily.setSelected(false);
                updateSample();
            }
            glyphsBtn.setEnabled(i >= 0);
	}//GEN-LAST:event_familyListValueChanged

	private void sizeTypeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sizeTypeComboActionPerformed
            if (sizeTypeCombo.getSelectedIndex() >= 0 && doneInit) {
                dncSize.setSelected(false);
                int type = sizeTypeCombo.getSelectedIndex();
                if (type == currentSizeType) {
                    return;
                }
                currentSizeType = type;
                float value = ((Number) sizeField.getValue()).floatValue();
                if (type == 0) {
                    // the *2/2 gives us the nearest half-point increment
                    value = Math.round(2f * BASE_SIZE * value / 100f) / 2f;
                    value = Math.max(1f, value);
                    value = Math.min(144f, value);
                    sizeField.setModel(new SpinnerNumberModel(value, 1f, 144f, 0.5f));
                } else {
                    value = Math.round(value / BASE_SIZE * 100f);
                    value = Math.max(1f, value);
                    value = Math.min(1000f, value);
                    sizeField.setModel(new SpinnerNumberModel((int) value, 1, 999, 1));
                }
                updateSample();
            }
	}//GEN-LAST:event_sizeTypeComboActionPerformed
    private int currentSizeType = 0;

	private void trackingFieldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_trackingFieldStateChanged
            if (doneInit) {
                dncTracking.setSelected(false);
                updateSample();
            }
	}//GEN-LAST:event_trackingFieldStateChanged

	private void sizeFieldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sizeFieldStateChanged
            if (doneInit) {
                dncSize.setSelected(false);
                updateSample();
            }
	}//GEN-LAST:event_sizeFieldStateChanged

	private void styleCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_styleCheckActionPerformed
            updateSample();
	}//GEN-LAST:event_styleCheckActionPerformed

	private void dncActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dncActionPerformed
            updateSample();
	}//GEN-LAST:event_dncActionPerformed

	private void glyphsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_glyphsBtnActionPerformed
            if (familyList.getSelectedIndex() < 0) {
                return;
            }
            Font f = ((FontToken) familyList.getSelectedValue()).getFont();
            JUtilities.showWaitCursor(this);
            try {
                JDialog icd = InsertCharsDialog.createFontViewer(f);
                icd.setLocationRelativeTo(this);
                icd.setModal(true);
                icd.setVisible(true);
            } finally {
                JUtilities.hideWaitCursor(this);
            }
	}//GEN-LAST:event_glyphsBtnActionPerformed

    private boolean doneInit = false;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox bCheck;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JCheckBox dncFamily;
    private javax.swing.JCheckBox dncSize;
    private javax.swing.JCheckBox dncTracking;
    private javax.swing.JCheckBox dncWidth;
    private javax.swing.JList<FontToken> familyList;
    private javax.swing.JPanel familyPanel;
    private javax.swing.JScrollPane familyScroll;
    private javax.swing.JButton glyphsBtn;
    private javax.swing.JCheckBox iCheck;
    private javax.swing.JLabel loadingLabel;
    private javax.swing.JPanel loadingPanel;
    private javax.swing.JButton okBtn;
    private javax.swing.JPanel okCancelPanel;
    private ca.cgjennings.apps.arkham.dialog.OverlayPanel overlayPanel;
    private javax.swing.JCheckBox sCheck;
    private javax.swing.JLabel sampleLabel;
    private javax.swing.JSpinner sizeField;
    private javax.swing.JPanel sizePanel;
    private javax.swing.JComboBox<String> sizeTypeCombo;
    private javax.swing.JPanel stylePanel;
    private javax.swing.JCheckBox subCheck;
    private javax.swing.JCheckBox supCheck;
    private javax.swing.JSpinner trackingField;
    private javax.swing.JPanel trackingPanel;
    private javax.swing.JCheckBox uCheck;
    private javax.swing.JComboBox<String> widthCombo;
    private javax.swing.JPanel widthPanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        markup = createMarkup();
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    public static class FontSampleLabel extends JLabel {

        public FontSampleLabel() {
            setText(string("ffd-l-sample-text"));
        }

        @Override
        public void setFont(Font f) {
            String s = string("ffd-l-sample-text");
            boolean ok = true;
            final int sampleLength = s.length();
            for (int i = 0; i < s.length() && ok; ++i) {
                ok = f.canDisplay(s.charAt(i));
            }
            if (!ok) {
                StringBuilder b = new StringBuilder(sampleLength);
                for (char i = 33; i < 0xfffe && b.length() < sampleLength; ++i) {
                    if (f.canDisplay(i)) {
                        b.append(i);
                    }
                }
                s = b.toString();
            }
            setText(s);
            super.setFont(f);
        }
    }
}
