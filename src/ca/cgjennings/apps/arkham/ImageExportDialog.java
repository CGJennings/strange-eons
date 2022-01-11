package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.sheet.FinishStyle;
import ca.cgjennings.imageio.SimpleImageWriter;
import ca.cgjennings.imageio.WritableImageFormat;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JIconComboBox;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.RightAlignedListRenderer;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.logging.Level;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A dialog used to configure image export options.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class ImageExportDialog extends javax.swing.JDialog implements AgnosticDialog {

    private WritableImageFormat[] wifs;
    private String[] formats;
    private SimpleImageWriter[] writers;
    private SimpleImageWriter selected;
    private boolean largeFormatHint;
    private boolean allowJoinHint;
    private boolean multipleFacesHint;

    private SimpleImageWriter writer(int index) {
        if (writers[index] == null) {
            writers[index] = new SimpleImageWriter(formats[index]);
        }
        return writers[index];
    }

    public ImageExportDialog() {
        this(150, false, true, true);
    }

    public ImageExportDialog(int defaultDPI, boolean largeFormatHint, boolean allowJoinHint, boolean multipleFacesHint) {
        super(StrangeEons.getWindow(), true);

        this.largeFormatHint = largeFormatHint;
        this.allowJoinHint = allowJoinHint;
        this.multipleFacesHint = multipleFacesHint;
        initComponents();
        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);
        AbstractGameComponentEditor.localizeComboBoxLabels(unitCombo, null);

        // init selectable, extendable image formats
        wifs = SimpleImageWriter.getImageFormats();
        formats = new String[wifs.length];
        DefaultComboBoxModel<String> fm = new DefaultComboBoxModel<>();
        for (int i = 0; i < wifs.length; ++i) {
            formats[i] = wifs[i].getExtension();
            fm.addElement(wifs[i].getName());
        }
        // matching writers for each format, created by writer(i) on demand
        writers = new SimpleImageWriter[formats.length];
        formatCombo.setModel(fm);

        // init container list
        DefaultComboBoxModel<ExportContainer> ecm = new DefaultComboBoxModel<ExportContainer>(StrangeEons.getRegisteredExportContainers());
        destinationCombo.setModel(ecm);
        if (ecm.getSize() < 2) {
            destinationLabel.setEnabled(false);
            destinationCombo.setEnabled(false);
        }
        // enable or disable config button for initial setting
        destinationComboActionPerformed(null);

        // size the dialog before making the custom panel (in)visible
        pack();
        setLocationRelativeTo(StrangeEons.getWindow());

        // init unit and load default resolutions
        int defUnit = Length.getDefaultUnit();
        if (defUnit == Length.CM) {
            unitCombo.setSelectedIndex(1);
        } else {
            unitCombo.setSelectedIndex(0);
        }

        // init panel selection
        customPanel.setVisible(false);
        loadBasicSettings();
        updateFormatWarning();
        createTipText();
        pack();
    }

    private void createTipText() {
        StringBuilder b = new StringBuilder("<html>");
        int sel = formatCombo.getSelectedIndex();
        if (sel >= 0) {
            String fname = ResourceKit.makeStringHTMLSafe(wifs[sel].getFullName());
            String desc = ResourceKit.makeStringHTMLSafe(wifs[sel].getDescription());
            if (fname != null) {
                b.append("<b>").append(fname).append("</b><br>");
                if (desc != null) {
                    b.append(desc);
                }
            }
        }
        if (b.length() > "<html>".length()) {
            formatTip.setTipText(b.toString());
            formatTip.setVisible(true);
        } else {
            formatTip.setVisible(false);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        SimpleImageWriter writerToKeep = ok ? getImageWriter() : null;
        for (int i = 0; i < writers.length; ++i) {
            if (writers[i] != null && writerToKeep != writers[i]) {
                writers[i].dispose();
                writers[i] = null;
            }
        }
    }

    public boolean showDialog() {
        ok = false;
        setVisible(true);
        return ok;
    }
    private boolean ok;

    private void saveBasicSettings() {
        Settings s = Settings.getUser();
        s.set(KEY_CONTAINER, ((ExportContainer) destinationCombo.getSelectedItem()).getIdentifier());

        for (int i = 0; i < taskBtns.length; ++i) {
            if (taskBtns[i].isSelected()) {
                s.set(KEY_TASK, tasks[i]);
            }
        }
    }

    private void loadBasicSettings() {
        Settings s = Settings.getUser();
        String containerID = s.get(KEY_CONTAINER);
        DefaultComboBoxModel m = (DefaultComboBoxModel) destinationCombo.getModel();
        for (int i = 0; i < m.getSize(); ++i) {
            ExportContainer ec = (ExportContainer) m.getElementAt(i);
            if (ec.getIdentifier().equals(containerID)) {
                destinationCombo.setSelectedIndex(i);
            }
        }

        taskBtns = new JRadioButton[]{
            customBtn, postOnlineBtn, printBtn, compatibleBtn
        };
        String task = s.get(KEY_TASK);
        int i = 0;
        for (; i < tasks.length; ++i) {
            if (tasks[i].equals(task)) {
                break;
            }
        }
        if (i == tasks.length) {
            --i;
        }
        taskBtns[i].setSelected(true);
        exportTypeActionPerformed(null);
    }
    private final String[] tasks = {"custom", "post", "print", "general"};
    private JRadioButton[] taskBtns;

    private static final String KEY_TASK = "export-task";
    private static final String KEY_CONTAINER = "export-container";

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        autoGroup = new javax.swing.ButtonGroup();
        customPanel = new javax.swing.JPanel();
        iowpPanel = new ca.cgjennings.imageio.IIOWritePanel();
        javax.swing.JLabel formatLabel = new javax.swing.JLabel();
        formatTip = new ca.cgjennings.ui.JTip();
        javax.swing.JLabel resolutionLabel = new javax.swing.JLabel();
        dpiCombo = new javax.swing.JComboBox();
        Component dpiEdC = dpiCombo.getEditor().getEditorComponent();
        if( dpiEdC instanceof JTextField ) {
            JTextField dpiEd = (JTextField) dpiEdC;
            dpiEd.setHorizontalAlignment( SwingConstants.TRAILING );
            dpiEd.setColumns( 4 );
        }
        joinImagesBox = new javax.swing.JCheckBox();
        formatCombo = new javax.swing.JComboBox<>();
        ca.cgjennings.ui.JHelpButton resolutionHelp = new ca.cgjennings.ui.JHelpButton();
        unitCombo = new javax.swing.JComboBox<>();
        suppressBackBtn = new javax.swing.JCheckBox();
        formatWarning = new ca.cgjennings.ui.JWarningLabel();
        javax.swing.JLabel finishLabel = new javax.swing.JLabel();
        edgeFinishCombo = new JIconComboBox(FinishStyle.values());
        bleedWidthLabel = new javax.swing.JLabel();
        bleedWidthField = new javax.swing.JSpinner();
        bleedWidthUnit = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        postOnlineBtn = new javax.swing.JRadioButton();
        printBtn = new javax.swing.JRadioButton();
        customBtn = new javax.swing.JRadioButton();
        compatibleBtn = new javax.swing.JRadioButton();
        javax.swing.JLabel purposeLabel = new javax.swing.JLabel();
        destinationLabel = new javax.swing.JLabel();
        destinationCombo = new javax.swing.JComboBox<>();
        configDestinationBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        ca.cgjennings.ui.JHelpButton dlgHelp = new ca.cgjennings.ui.JHelpButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("exf-title")); // NOI18N
        setResizable(false);

        customPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("exf-l-custom-settings"))); // NOI18N

        formatLabel.setLabelFor(formatCombo);
        formatLabel.setText(string("exf-l-format")); // NOI18N

        resolutionLabel.setLabelFor(dpiCombo);
        resolutionLabel.setText(string("exf-l-resolution")); // NOI18N

        dpiCombo.setEditable(true);
        dpiCombo.setRenderer( new RightAlignedListRenderer() );
        dpiCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dpiComboFocusLost(evt);
            }
        });
        dpiCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dpiComboActionPerformed(evt);
            }
        });

        joinImagesBox.setText(string("exf-b-combine-sheets")); // NOI18N

        formatCombo.setMaximumRowCount(12);
        formatCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formatComboActionPerformed(evt);
            }
        });

        resolutionHelp.setHelpPage("gc-export#resolution");

        unitCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "dpi", "dpcm" }));
        unitCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitComboActionPerformed(evt);
            }
        });

        suppressBackBtn.setText(string("exf-b-suppress-backs")); // NOI18N
        suppressBackBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                suppressBackBtnActionPerformed(evt);
            }
        });

        formatWarning.setText(string("exf-warn-format")); // NOI18N

        finishLabel.setLabelFor(edgeFinishCombo);
        finishLabel.setText(string("exf-l-edge-finish")); // NOI18N

        edgeFinishCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edgeFinishComboActionPerformed(evt);
            }
        });

        bleedWidthLabel.setLabelFor(bleedWidthField);
        bleedWidthLabel.setText(string("exf-l-bleed-margin")); // NOI18N

        bleedWidthField.setModel(new javax.swing.SpinnerNumberModel(9.0d, 0.25d, 36.0d, 0.25d));

        bleedWidthUnit.setText(string("iid-cb-unit2")); // NOI18N

        javax.swing.GroupLayout customPanelLayout = new javax.swing.GroupLayout(customPanel);
        customPanel.setLayout(customPanelLayout);
        customPanelLayout.setHorizontalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customPanelLayout.createSequentialGroup()
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(customPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(resolutionLabel)
                            .addComponent(finishLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(customPanelLayout.createSequentialGroup()
                                .addComponent(dpiCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(resolutionHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(edgeFinishCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(customPanelLayout.createSequentialGroup()
                                .addComponent(bleedWidthLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(bleedWidthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(bleedWidthUnit))))
                    .addGroup(customPanelLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(iowpPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(customPanelLayout.createSequentialGroup()
                                .addComponent(formatCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(formatTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(formatWarning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(customPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(suppressBackBtn))
                    .addGroup(customPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(formatLabel))
                    .addGroup(customPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(joinImagesBox)))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        customPanelLayout.setVerticalGroup(
            customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(formatLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(formatTip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(formatCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(formatWarning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iowpPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(resolutionLabel)
                    .addComponent(dpiCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resolutionHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(finishLabel)
                    .addComponent(edgeFinishCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(customPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bleedWidthLabel)
                    .addComponent(bleedWidthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(bleedWidthUnit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(joinImagesBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(suppressBackBtn)
                .addContainerGap())
        );

        mainPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("exf-l-basic"))); // NOI18N

        autoGroup.add(postOnlineBtn);
        postOnlineBtn.setText(string("exf-b-post-online")); // NOI18N
        postOnlineBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTypeActionPerformed(evt);
            }
        });

        autoGroup.add(printBtn);
        printBtn.setText(string("exf-b-print")); // NOI18N
        printBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTypeActionPerformed(evt);
            }
        });

        autoGroup.add(customBtn);
        customBtn.setText(string("exf-b-set-custom")); // NOI18N
        customBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTypeActionPerformed(evt);
            }
        });

        autoGroup.add(compatibleBtn);
        compatibleBtn.setText(string("exf-b-compatible")); // NOI18N
        compatibleBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportTypeActionPerformed(evt);
            }
        });

        purposeLabel.setText(string("exf-l-desc")); // NOI18N

        destinationLabel.setLabelFor(destinationCombo);
        destinationLabel.setText(string("exf-destination")); // NOI18N

        destinationCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                destinationComboActionPerformed(evt);
            }
        });

        configDestinationBtn.setFont(configDestinationBtn.getFont().deriveFont(configDestinationBtn.getFont().getSize()-1f));
        configDestinationBtn.setText(string("exf-destination-config")); // NOI18N
        configDestinationBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configDestinationBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(printBtn)
                            .addComponent(postOnlineBtn)
                            .addComponent(compatibleBtn)
                            .addComponent(customBtn)))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(purposeLabel))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(destinationLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(destinationCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(configDestinationBtn)))
                .addContainerGap(140, Short.MAX_VALUE))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(destinationLabel)
                    .addComponent(destinationCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(configDestinationBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(purposeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(postOnlineBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(printBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(compatibleBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customBtn)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cancelBtn.setText(string("cancel")); // NOI18N

        okBtn.setText(string("exf-ok")); // NOI18N

        dlgHelp.setHelpPage("gc-export");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(customPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(dlgHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 303, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(customPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(dlgHelp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okBtn)
                    .addComponent(cancelBtn))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

	private void formatComboActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_formatComboActionPerformed
            int sel = formatCombo.getSelectedIndex();
            if (sel >= 0) {
                selected = writer(sel);
                iowpPanel.configureWriter(selected);
                createTipText();
                updateFormatWarning();
            }
	}//GEN-LAST:event_formatComboActionPerformed

    void setFormat(String ext) {
        for (int i = 0; i < formats.length; ++i) {
            if (formats[i].equals(ext)) {
                formatCombo.setSelectedIndex(i);
                return;
            }
        }
        StrangeEons.log.log(Level.WARNING, "invalid format setting: {0}", ext);
        formatCombo.setSelectedIndex(0);
    }

    String getFormat() {
        return formats[Math.max(0, formatCombo.getSelectedIndex())];
    }

    String getFormatDescription() {
        return wifs[Math.max(0, formatCombo.getSelectedIndex())].getName();
    }

    ExportContainer getExportContainer() {
        return (ExportContainer) destinationCombo.getSelectedItem();
    }

    SimpleImageWriter getImageWriter() {
        return writer(Math.max(0, formatCombo.getSelectedIndex()));
    }

    void setResolution(int ppi) {
        ppi = clampResolution(ppi, 0);
        if (unitCombo.getSelectedIndex() == 1) {
            ppi = ppi2ppcm(ppi);
        }
        dpiCombo.setSelectedItem(resolutionFormat.format(ppi));
    }

    int getResolution() {
        int v = parseResolution();
        if (unitCombo.getSelectedIndex() == 1) {
            v = ppcm2ppi(v);
        }
        v = clampResolution(v, 0);
        return v;
    }

    boolean isImageJoinEnabled() {
        return joinImagesBox.isEnabled() && joinImagesBox.isSelected();
    }

    boolean isFaceSuppressionEnabled() {
        return suppressBackBtn.isSelected();
    }

    double getUserBleedMargin() {
        FinishStyle fs = (FinishStyle) edgeFinishCombo.getSelectedItem();
        double ubm = fs.getSuggestedBleedMargin();
        if (fs == FinishStyle.MARGIN) {
            ubm = (Double) bleedWidthField.getValue();
        }
        return ubm;
    }

	private void exportTypeActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_exportTypeActionPerformed
            JRadioButton[] btns = new JRadioButton[]{
                postOnlineBtn, printBtn, compatibleBtn, customBtn
            };
            int sel = -1;
            for (int i = 0; i < btns.length; ++i) {
                if (btns[i].isSelected()) {
                    sel = i;
                    break;
                }
            }

            // if we were showing the custom settings, save the user's changes
            if (wasShowingCustomSettings) {
                saveCustomSettings();
            }
            wasShowingCustomSettings = sel == 3;

            // these are the same for all standard tasks except Web re-enables suppression
            // and custom will load the value from settings; if they cannot be set then
            // they will be cleared and disabled further below
            joinImagesBox.setSelected(false);
            suppressBackBtn.setSelected(false);
            switch (sel) {
                case 0:
                    setFormat("jpg");
                    iowpPanel.setCompressionQuality(largeFormatHint ? 0.40f : 0.50f);
                    iowpPanel.setProgressiveScanEnabled(true);
                    setResolution(largeFormatHint ? 125 : 150);
                    suppressBackBtn.setSelected(true);
                    edgeFinishCombo.setSelectedItem(FinishStyle.SQUARE);
                    JUtilities.enableTree(customPanel, false);
                    break;
                case 1:
                    setFormat("png");
                    iowpPanel.setProgressiveScanEnabled(false);
                    setResolution(300);
                    edgeFinishCombo.setSelectedItem(FinishStyle.MARGIN);
                    bleedWidthField.setValue(9d);
                    JUtilities.enableTree(customPanel, false);
                    break;
                case 2:
                    setFormat("jpg");
                    iowpPanel.setCompressionQuality(0.75f);
                    iowpPanel.setProgressiveScanEnabled(true);
                    setResolution(200);
                    edgeFinishCombo.setSelectedItem(FinishStyle.SQUARE);
                    JUtilities.enableTree(customPanel, false);
                    break;
                case 3:
                    JUtilities.enableTree(customPanel, true);
                    if (!customPanel.isVisible()) {
                        customPanel.setVisible(true);
                        pack();
                    }
                    loadCustomSettings();
                    break;
            }

            if (!allowJoinHint) {
                joinImagesBox.setEnabled(false);
                joinImagesBox.setSelected(false);
            }
            if (!multipleFacesHint) {
                suppressBackBtn.setEnabled(false);
                suppressBackBtn.setSelected(false);
            }
            // enforces rule that if suppress back is selected, join is not
            suppressBackBtnActionPerformed(null);
	}//GEN-LAST:event_exportTypeActionPerformed

    private boolean wasShowingCustomSettings = false;

    private void loadCustomSettings() {
        Settings s = Settings.getUser();
        setFormat(s.get(KEY_FORMAT));
        iowpPanel.setCompressionQuality(s.getFloat(KEY_QUALITY));
        iowpPanel.setProgressiveScanEnabled(s.getYesNo(KEY_SCAN));
        setResolution(s.getInt(KEY_DPI));
        joinImagesBox.setSelected(s.getYesNo(KEY_COMBINE));
        suppressBackBtn.setSelected(s.getYesNo(KEY_SUPPRESS));
        edgeFinishCombo.setSelectedItem(FinishStyle.fromSetting(s.get(KEY_FINISH_STYLE)));
        try {
            bleedWidthField.setValue(s.getDouble(KEY_BLEED_MARGIN, 9d));
        } catch (IllegalArgumentException iae) {
            bleedWidthField.setValue(9d);
        }
    }

    private void saveCustomSettings() {
        Settings s = Settings.getUser();
        s.set(KEY_FORMAT, getFormat());
        s.setFloat(KEY_QUALITY, iowpPanel.getCompressionQuality());
        s.setYesNo(KEY_SCAN, iowpPanel.isProgressiveScanEnabled());
        s.setInt(KEY_DPI, getResolution());
        if (allowJoinHint) {
            s.setYesNo(KEY_COMBINE, joinImagesBox.isSelected());
        }
        if (multipleFacesHint) {
            s.setYesNo(KEY_SUPPRESS, suppressBackBtn.isSelected());
        }
        s.set(KEY_FINISH_STYLE, ((FinishStyle) edgeFinishCombo.getSelectedItem()).toSetting());
        s.setDouble(KEY_BLEED_MARGIN, (Double) bleedWidthField.getValue());
    }

    private static final String KEY_FORMAT = "imexport-format";
    private static final String KEY_QUALITY = "imexport-quality";
    private static final String KEY_SCAN = "imexport-progressive";
    private static final String KEY_DPI = "imexport-dpi";
    private static final String KEY_COMBINE = "imexport-combine";
    private static final String KEY_SUPPRESS = "imexport-suppress-backs";
    private static final String KEY_FINISH_STYLE = "imexport-finish";
    private static final String KEY_BLEED_MARGIN = "imexport-bleed-margin";

	private void unitComboActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_unitComboActionPerformed
            int unit = unitCombo.getSelectedIndex();
            if (unit == currentUnit) {
                return;
            }
            currentUnit = unit;

            // convert the selected value to the new unit
            int v = parseResolution();
            if (unit == 0) {
                v = ppcm2ppi(v);
            } else {
                v = ppi2ppcm(v);
            }
            v = clampResolution(v, unit);

            // install the default model values
            @SuppressWarnings("unchecked")
            DefaultComboBoxModel<String> m = (DefaultComboBoxModel<String>) dpiCombo.getModel();
            m.removeAllElements();
            for (int i = 0; i < ppiOptions.length; ++i) {
                int o = ppiOptions[i];
                if (unit == 1) {
                    o = ppi2ppcm(o);
                }
                m.addElement(resolutionFormat.format(o));
            }

            dpiCombo.setSelectedItem(resolutionFormat.format(v));

            // update user's system-wide default unit
            Length.setDefaultUnit(unit == 0 ? Length.IN : Length.CM);
	}//GEN-LAST:event_unitComboActionPerformed

    // set to currently selected export resolution unit; used to prevent converting
    // between ppi/ppcm when the unit hasn't actually changed
    private int currentUnit = -2;

	private void dpiComboActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_dpiComboActionPerformed
            if (dpiComboIsUpdating) {
                return;
            }
            dpiComboIsUpdating = true;
            try {
                int v = parseResolution();
                v = clampResolution(v, unitCombo.getSelectedIndex());
                dpiCombo.setSelectedItem(resolutionFormat.format(v));
            } finally {
                dpiComboIsUpdating = false;
            }
	}//GEN-LAST:event_dpiComboActionPerformed

	private void dpiComboFocusLost( java.awt.event.FocusEvent evt ) {//GEN-FIRST:event_dpiComboFocusLost
            dpiComboActionPerformed(null);
	}//GEN-LAST:event_dpiComboFocusLost

	private void configDestinationBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_configDestinationBtnActionPerformed
            ExportContainer ec = getExportContainer();
            if (ec.isConfigurable()) {
                ec.configure(configDestinationBtn);
            }
	}//GEN-LAST:event_configDestinationBtnActionPerformed

	private void destinationComboActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_destinationComboActionPerformed
            final boolean hasOptions = destinationCombo.isEnabled() && getExportContainer().isConfigurable();
            configDestinationBtn.setEnabled(hasOptions);
            configDestinationBtn.setVisible(hasOptions);
            updateFormatWarning();
	}//GEN-LAST:event_destinationComboActionPerformed

    private void suppressBackBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_suppressBackBtnActionPerformed
        if (suppressBackBtn.isSelected()) {
            joinImagesBox.setSelected(false);
            joinImagesBox.setEnabled(false);
        } else {
            joinImagesBox.setEnabled(allowJoinHint && customBtn.isSelected());
        }
    }//GEN-LAST:event_suppressBackBtnActionPerformed

    private void edgeFinishComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_edgeFinishComboActionPerformed
        final boolean enable = edgeFinishCombo.getSelectedItem() == FinishStyle.MARGIN;
        bleedWidthLabel.setEnabled(enable);
        bleedWidthField.setEnabled(enable);
        bleedWidthUnit.setEnabled(enable);
        updateFormatWarning();
    }//GEN-LAST:event_edgeFinishComboActionPerformed
    private boolean dpiComboIsUpdating;

    private int[] ppiOptions = new int[]{
        96, 125, 150, 200, 300, 450, 600
    };

    private static int ppi2ppcm(int ppi) {
        return (int) (ppi / 2.54d + 0.5d);
    }

    private static int ppcm2ppi(int ppcm) {
        // we handle some values as special cases so that
        // when using standard values, going from in->cm->in
        // will end up back at the same value
        int res;
        switch (ppcm) {
            case 38:
                res = 96;
                break;
            case 49:
                res = 125;
                break;
            case 79:
                res = 200;
                break;
            case 236:
                res = 600;
                break;
            case 472:
                res = 1_200;
                break;
            default:
                res = (int) (ppcm * 2.54d + 0.5d);
        }
        return res;
    }

    private int clampResolution(int res, int unit) {
        if (unit == 0) {
            if (res == -1) {
                res = 150;
            } else if (res < 48) {
                res = 48;
            } else if (res > 1_200) {
                res = 1_200;
            }
        } else {
            if (res == -1) {
                res = 59;
            } else if (res < 19) {
                res = 19;
            } else if (res > 472) {
                res = 472;
            }
        }
        return res;
    }

    private int parseResolution() {
        if (dpiCombo.getSelectedItem() == null) {
            return -1;
        }
        String dpi = dpiCombo.getSelectedItem().toString();
        ParsePosition pp = new ParsePosition(0);
        for (int i = 0; i < dpi.length(); ++i) {
            pp.setIndex(i);
            Number n = resolutionFormat.parse(dpi, pp);
            if (pp.getIndex() != i) {
                return n.intValue();
            }
        }
        // only garbage in the field
        return -1;
    }

    NumberFormat resolutionFormat = NumberFormat.getNumberInstance();

    {
        resolutionFormat.setMaximumFractionDigits(0);
        resolutionFormat.setParseIntegerOnly(true);
    }

    private void updateFormatWarning() {
        String warning = null;
        String fmt = getFormat();
        ExportContainer ec = getExportContainer();
        FinishStyle fs = (FinishStyle) edgeFinishCombo.getSelectedItem();
        boolean fmtHasAlpha = getImageWriter().isTransparencySupported();

        if (!ec.isFileFormatSupported(fmt.toLowerCase(Locale.ROOT))) {
            warning = string("exf-warn-format");
        } else if (fs == FinishStyle.ROUND && !fmtHasAlpha) {
            warning = string("exf-warn-transparency");
        }

        if (warning != null) {
            formatWarning.setText(warning);
        }
        formatWarning.setVisible(warning != null);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup autoGroup;
    private javax.swing.JSpinner bleedWidthField;
    private javax.swing.JLabel bleedWidthLabel;
    private javax.swing.JLabel bleedWidthUnit;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JRadioButton compatibleBtn;
    private javax.swing.JButton configDestinationBtn;
    private javax.swing.JRadioButton customBtn;
    private javax.swing.JPanel customPanel;
    private javax.swing.JComboBox<ExportContainer> destinationCombo;
    private javax.swing.JLabel destinationLabel;
    private javax.swing.JComboBox dpiCombo;
    private javax.swing.JComboBox<FinishStyle> edgeFinishCombo;
    private javax.swing.JComboBox<String> formatCombo;
    private ca.cgjennings.ui.JTip formatTip;
    private ca.cgjennings.ui.JWarningLabel formatWarning;
    private ca.cgjennings.imageio.IIOWritePanel iowpPanel;
    private javax.swing.JCheckBox joinImagesBox;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton okBtn;
    private javax.swing.JRadioButton postOnlineBtn;
    private javax.swing.JRadioButton printBtn;
    private javax.swing.JCheckBox suppressBackBtn;
    private javax.swing.JComboBox<String> unitCombo;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        ok = true;
        handleCancelAction(e);
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        saveBasicSettings();
        if (customBtn.isSelected()) {
            saveCustomSettings();
        }
        dispose();
    }
}
