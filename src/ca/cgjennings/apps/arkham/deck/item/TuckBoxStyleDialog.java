package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.ColourDialog;
import ca.cgjennings.apps.arkham.Length;
import ca.cgjennings.apps.arkham.deck.item.TuckBox.BoxSizer;
import ca.cgjennings.apps.arkham.deck.item.TuckBox.BoxType;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.RightAlignedListRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTextField;
import static resources.Language.string;

/**
 * Style/editor dialog for tuck boxes.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
final class TuckBoxStyleDialog extends javax.swing.JDialog implements AgnosticDialog {

    private TuckBox item;
    private NumberFormat formatter;
    private double[] dimensions = new double[]{1d, 1d, 1d};
    private double sleeveThickness = 0d;
    private TuckBoxPreview tuckBoxPreview;

    /**
     * Creates new form LineStyle
     */
    public TuckBoxStyleDialog(java.awt.Frame parent, TuckBox box) {
        super(parent, true);
        initComponents();
        preferHingeCut = box.hasHingeCut();
        boxTypeCombo.setModel(new DefaultComboBoxModel(TuckBox.BoxType.values()));
        sleeveCombo.setRenderer(new RightAlignedListRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setEnabled(sleeveCombo.isEnabled());
                return this;
            }
        });
        AbstractGameComponentEditor.localizeComboBoxLabels(sleeveCombo, "style-cb-box-sleeves");
        tuckBoxPreview = new TuckBoxPreview();
        previewPanel.add(tuckBoxPreview);

        formatter = NumberFormat.getNumberInstance();

        sizers = TuckBox.getBoxSizers();
        cardTypeCombo.setModel(new DefaultComboBoxModel(sizers));

        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        this.item = box;
        unitSelector.setUnit(Length.getDefaultUnit());
        oldUnit = unitSelector.getUnit();

        copyStyleFromItem();

        setLocationRelativeTo(getParent());
    }

    private void formatField(JTextField f, double v) {
        f.setText(formatter.format(v));
    }

    private void formatFields(double[] values) {
        Length len = new Length();
        int unit = oldUnit;
        JTextField[] fields = new JTextField[]{widthField, heightField, depthField};
        for (int i = 0; i < fields.length; ++i) {
            len.set(values[i], Length.PT);
            formatField(fields[i], len.get(unit));
            dimensions[i] = len.get(unit);
        }
    }

    private double[] readDimensions(int unit) {
        Length len = new Length();
        JTextField[] fields = new JTextField[]{widthField, heightField, depthField};
        for (int i = 0; i < fields.length; ++i) {
            double val = 0d;
            try {
                val = formatter.parse(fields[i].getText()).doubleValue();
            } catch (ParseException e) {
            }
            if (val <= 0d) {
                val = dimensions[i];
            }
            len.set(val, unit);
            dimensions[i] = len.get(Length.PT);
        }
        return dimensions;
    }

    private void copyStyleFromItem() {
        boxTypeCombo.setSelectedItem(item.getBoxType());
        formatFields(item.getDimensions());
        intColourLabel.setBackground(unpackColour(item.getInteriorFill()));
        extColourLabel.setBackground(unpackColour(item.getExteriorFill()));
        lineColourLabel.setBackground(unpackColour(item.getLineColor()));
        foldColourLabel.setBackground(unpackColour(item.getFoldColour()));
        thumbNotchCheck.setSelected(item.isThumbNotched());
        roundedSideFlapsCheck.setSelected(item.hasRoundedSideFlaps());
        hingeCutCheck.setSelected(item.hasHingeCut());
        foldLineCheck.setSelected(item.hasFoldLines());
        thicknessSpinner.setValue(item.getLineThickness());

        updateRestrictedControls();
        updatePreview();
    }

    private void updateRestrictedControls() {
        boolean enable = (BoxType) boxTypeCombo.getSelectedItem() != BoxType.OPEN_FACE_BOX;
        hingeCutCheck.setEnabled(enable);
        if (enable) {
            hingeCutCheck.setSelected(preferHingeCut);
        } else {
            hingeCutCheck.setSelected(false);
        }
    }
    private boolean preferHingeCut;

    private static Color unpackColour(Color c) {
        if (c == null) {
            return new Color(0, true);
        }
        return c;
    }

    private static Color packColour(Color c) {
        if (c == null || (c.getAlpha() == 0)) {
            return null;
        }
        return c;
    }

    private boolean copyStyleToItem() {
        item.setBoxType((BoxType) boxTypeCombo.getSelectedItem());

        readDimensions(oldUnit);

        dimensions[0] = Math.max(dimensions[0], 36);
        dimensions[1] = Math.max(dimensions[1], 36);
        dimensions[2] = Math.max(dimensions[2], 18);

        item.setDimensions(dimensions[0], dimensions[1], dimensions[2]);
        formatFields(item.getDimensions());

        item.setInteriorFill(packColour(intColourLabel.getBackground()));
        item.setExteriorFill(packColour(extColourLabel.getBackground()));
        item.setLineColour(packColour(lineColourLabel.getBackground()));
        item.setFoldColour(packColour(foldColourLabel.getBackground()));
        item.setThumbNotched(thumbNotchCheck.isSelected());
        item.setRoundedSideFlaps(roundedSideFlapsCheck.isSelected());
        if (hingeCutCheck.isEnabled()) {
            item.setHingeCut(hingeCutCheck.isSelected());
        }
        item.setFoldLines(foldLineCheck.isSelected());
        item.setLineThickness(((Number) thicknessSpinner.getValue()).floatValue());

        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancelBtn = new javax.swing.JButton();
        okBtn = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        intColourLabel = new ColourDialog.ColourButton();
        intColourButton = new javax.swing.JButton();
        intLabel = new javax.swing.JLabel();
        extLabel = new javax.swing.JLabel();
        extColourLabel = new ColourDialog.ColourButton();
        extColourButton = new javax.swing.JButton();
        lineLabel = new javax.swing.JLabel();
        lineColourLabel = new ColourDialog.ColourButton();
        lineColourButton = new javax.swing.JButton();
        foldLabel = new javax.swing.JLabel();
        foldColourLabel = new ColourDialog.ColourButton();
        foldColourButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        widthField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        heightField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        depthField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        unitSelector = new ca.cgjennings.apps.arkham.UnitSelector();
        jLabel11 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel7 = new javax.swing.JLabel();
        cardTypeCombo = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        cardCount = new javax.swing.JSpinner();
        sleeveLabel = new javax.swing.JLabel();
        sleeveCombo = new javax.swing.JComboBox();
        previewPanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        thumbNotchCheck = new javax.swing.JCheckBox();
        roundedSideFlapsCheck = new javax.swing.JCheckBox();
        hingeCutCheck = new javax.swing.JCheckBox();
        spacer = new javax.swing.JLabel();
        foldLineCheck = new javax.swing.JCheckBox();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        thicknessSpinner = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        boxTypeCombo = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string("style-t-box-style")); // NOI18N

        cancelBtn.setText(string("cancel")); // NOI18N

        okBtn.setText(string("style-b-edit-box")); // NOI18N

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-l-box-fill"))); // NOI18N

        intColourLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        intColourLabel.setContentAreaFilled(false);
        intColourLabel.setPreferredSize(new java.awt.Dimension(24, 24));
        intColourLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intColourButtonActionPerformed(evt);
            }
        });

        intColourButton.setText(string("style-li-colour")); // NOI18N
        intColourButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                intColourButtonActionPerformed(evt);
            }
        });

        intLabel.setText(string("style-l-interior")); // NOI18N

        extLabel.setText(string("style-l-exterior")); // NOI18N

        extColourLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        extColourLabel.setContentAreaFilled(false);
        extColourLabel.setPreferredSize(new java.awt.Dimension(24, 24));
        extColourLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extColourButtonActionPerformed(evt);
            }
        });

        extColourButton.setText(string("style-li-colour")); // NOI18N
        extColourButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extColourButtonActionPerformed(evt);
            }
        });

        lineLabel.setText(string("style-l-box-line")); // NOI18N

        lineColourLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        lineColourLabel.setContentAreaFilled(false);
        lineColourLabel.setPreferredSize(new java.awt.Dimension(24, 24));
        lineColourLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lineColourButtonActionPerformed(evt);
            }
        });

        lineColourButton.setText(string("style-li-colour")); // NOI18N
        lineColourButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lineColourButtonActionPerformed(evt);
            }
        });

        foldLabel.setText(string("style-l-box-fold")); // NOI18N

        foldColourLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        foldColourLabel.setContentAreaFilled(false);
        foldColourLabel.setPreferredSize(new java.awt.Dimension(24, 24));
        foldColourLabel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                foldColourButtonActionPerformed(evt);
            }
        });

        foldColourButton.setText(string("style-li-colour")); // NOI18N
        foldColourButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                foldColourButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lineLabel)
                    .addComponent(foldLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lineColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(foldColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lineColourButton)
                    .addComponent(foldColourButton))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(intLabel)
                    .addComponent(extLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(intColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(extColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(intColourButton)
                    .addComponent(extColourButton))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(intColourButton)
                    .addComponent(intColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(intLabel)
                    .addComponent(lineLabel)
                    .addComponent(lineColourButton)
                    .addComponent(lineColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(extColourButton)
                    .addComponent(extColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(extLabel)
                    .addComponent(foldLabel)
                    .addComponent(foldColourButton)
                    .addComponent(foldColourLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-t-dimensions"))); // NOI18N

        jLabel1.setText(string("style-l-dimensions")); // NOI18N

        widthField.setColumns(5);
        widthField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        widthField.setText("2");
        widthField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dimensionFieldFocusLost(evt);
            }
        });

        jLabel2.setText("×");

        heightField.setColumns(5);
        heightField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        heightField.setText("2");
        heightField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dimensionFieldFocusLost(evt);
            }
        });

        jLabel3.setText("×");

        depthField.setColumns(5);
        depthField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        depthField.setText("2");
        depthField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                dimensionFieldFocusLost(evt);
            }
        });

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getSize()-2f));
        jLabel4.setText(string("style-l-width")); // NOI18N

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getSize()-2f));
        jLabel5.setText(string("style-l-height")); // NOI18N

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getSize()-2f));
        jLabel6.setText(string("style-l-depth")); // NOI18N

        unitSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitSelectorActionPerformed(evt);
            }
        });

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getStyle() | java.awt.Font.BOLD, jLabel11.getFont().getSize()-1));
        jLabel11.setText(string("style-l-box-size-help")); // NOI18N

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getSize()-1f));
        jLabel7.setText(string("style-l-box-type")); // NOI18N

        cardTypeCombo.setFont(cardTypeCombo.getFont().deriveFont(cardTypeCombo.getFont().getSize()-1f));
        cardTypeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cardTypeComboActionPerformed(evt);
            }
        });

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getSize()-1f));
        jLabel8.setText(string("style-l-box-cards")); // NOI18N

        cardCount.setFont(cardCount.getFont().deriveFont(cardCount.getFont().getSize()-1f));
        cardCount.setModel(new javax.swing.SpinnerNumberModel(50, 1, 999, 1));
        cardCount.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cardCountStateChanged(evt);
            }
        });

        sleeveLabel.setFont(sleeveLabel.getFont().deriveFont(sleeveLabel.getFont().getSize()-1f));
        sleeveLabel.setText(string("style-l-box-sleeves")); // NOI18N

        sleeveCombo.setFont(sleeveCombo.getFont().deriveFont(sleeveCombo.getFont().getSize()-1f));
        sleeveCombo.setMaximumRowCount(16);
        sleeveCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "style-cb-box-sleeves0", "25 µm", "30 µm", "40 µm", "50 µm", "62.5  µm", "90 µm", "100  µm", "120  µm", "125  µm" }));
        sleeveCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sleeveComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8)
                    .addComponent(sleeveLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cardTypeCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cardCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sleeveCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(depthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(unitSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator1)
                .addGap(10, 10, 10))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(widthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(heightField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(depthField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(unitSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel11)
                .addGap(1, 1, 1)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cardTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(cardCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sleeveLabel)
                    .addComponent(sleeveCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        previewPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-t-preview"))); // NOI18N
        previewPanel.setOpaque(false);
        previewPanel.setLayout(new java.awt.BorderLayout());

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-t-style"))); // NOI18N

        thumbNotchCheck.setText(string("style-b-notch")); // NOI18N
        thumbNotchCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkActionPerformed(evt);
            }
        });

        roundedSideFlapsCheck.setText(string("style-b-rounded-sides")); // NOI18N
        roundedSideFlapsCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkActionPerformed(evt);
            }
        });

        hingeCutCheck.setText(string("style-b-hinge-cut")); // NOI18N
        hingeCutCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkActionPerformed(evt);
            }
        });

        foldLineCheck.setText(string("style-b-fold-lines")); // NOI18N
        foldLineCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkActionPerformed(evt);
            }
        });

        jLabel9.setText(string("style-l-box-thickness")); // NOI18N

        thicknessSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.1f), Float.valueOf(6.0f), Float.valueOf(0.1f)));
        thicknessSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                thicknessSpinnerStateChanged(evt);
            }
        });

        jLabel10.setText(string("style-li-points")); // NOI18N

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(spacer)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(thumbNotchCheck)
                            .addComponent(roundedSideFlapsCheck))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(foldLineCheck)
                            .addComponent(hingeCutCheck)))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(thicknessSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel10)))
                .addContainerGap(20, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(thicknessSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spacer)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(thumbNotchCheck)
                            .addComponent(hingeCutCheck))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(roundedSideFlapsCheck)
                            .addComponent(foldLineCheck))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(string("style-t-box-type"))); // NOI18N

        boxTypeCombo.setFont(boxTypeCombo.getFont().deriveFont(boxTypeCombo.getFont().getStyle() | java.awt.Font.BOLD));
        boxTypeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxTypeComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(boxTypeCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(2, 2, 2))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(boxTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelBtn))
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(previewPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelBtn, okBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(previewPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cancelBtn)
                            .addComponent(okBtn)))
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jPanel4, jPanel5});

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void editColour(JButton label) {
        ColourDialog d = ColourDialog.getSharedDialog();
        d.setSelectedColor(label.getBackground());
        d.setLocationRelativeTo(label);
        d.setVisible(true);
        if (d.getSelectedColor() != null) {
            label.setBackground(d.getSelectedColor());
            updatePreview();
        }
    }

	private void intColourButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_intColourButtonActionPerformed
            editColour(intColourLabel);
}//GEN-LAST:event_intColourButtonActionPerformed

private void unitSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitSelectorActionPerformed
    readDimensions(oldUnit);
    oldUnit = unitSelector.getUnit();
    formatFields(dimensions);
}//GEN-LAST:event_unitSelectorActionPerformed

private void extColourButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extColourButtonActionPerformed
    editColour(extColourLabel);
}//GEN-LAST:event_extColourButtonActionPerformed

private void lineColourButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lineColourButtonActionPerformed
    editColour(lineColourLabel);
}//GEN-LAST:event_lineColourButtonActionPerformed

private void cardCountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cardCountStateChanged
    applyHelperSize();
}//GEN-LAST:event_cardCountStateChanged

private void cardTypeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cardTypeComboActionPerformed
    int type = cardTypeCombo.getSelectedIndex();
    if (type < 0) {
        return;
    }
    BoxSizer sizer = sizers[type];

    if (sizers[oldSizerType].allowSleeves() != sizer.allowSleeves()) {
        if (sizer.allowSleeves()) {
            sleeveLabel.setEnabled(true);
            sleeveCombo.setEnabled(true);
            if (oldSleeveType >= 0) {
                sleeveCombo.setSelectedIndex(oldSleeveType);
            }
        } else {
            oldSleeveType = sleeveCombo.getSelectedIndex();
            sleeveLabel.setEnabled(false);
            sleeveCombo.setEnabled(false);
            sleeveCombo.setSelectedIndex(0);
            sleeveThickness = 0d;
        }
    }
    oldSizerType = type;

    applyHelperSize();
}//GEN-LAST:event_cardTypeComboActionPerformed
    private int oldSizerType = 0;
    private int oldSleeveType = 0;

private void dimensionFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_dimensionFieldFocusLost
    updatePreview();
}//GEN-LAST:event_dimensionFieldFocusLost

private void checkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkActionPerformed
    if (evt.getSource() == hingeCutCheck && hingeCutCheck.isEnabled()) {
        preferHingeCut = hingeCutCheck.isSelected();
    }
    updatePreview();
}//GEN-LAST:event_checkActionPerformed

private void foldColourButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_foldColourButtonActionPerformed
    editColour(foldColourLabel);
}//GEN-LAST:event_foldColourButtonActionPerformed

private void sleeveComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sleeveComboActionPerformed
    if (!isShowing()) {
        return;
    }

    int sel = sleeveCombo.getSelectedIndex();
    if (sel <= 0) {
        sleeveThickness = 0;
    } else {
        String size = sleeveCombo.getSelectedItem().toString();
        int pos = size.indexOf(' ');
        if (pos < 0) {
            throw new AssertionError("Can't find sleeve thickness value: " + size);
        }
        size = size.substring(0, pos).trim();
        try {
            sleeveThickness = Double.parseDouble(size);
            applyHelperSize();
        } catch (NumberFormatException e) {
            throw new AssertionError("Unable to parse sleeve thickness: " + size);
        }
    }
}//GEN-LAST:event_sleeveComboActionPerformed

    private void boxTypeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxTypeComboActionPerformed
        BoxType sel = (BoxType) boxTypeCombo.getSelectedItem();
        if (sel != null) {
            updatePreview();
            updateRestrictedControls();
        }
    }//GEN-LAST:event_boxTypeComboActionPerformed

    private void thicknessSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_thicknessSpinnerStateChanged
        updatePreview();
    }//GEN-LAST:event_thicknessSpinnerStateChanged

    private BoxSizer[] sizers;

    private void applyHelperSize() {
        int type = cardTypeCombo.getSelectedIndex();
        if (type < 0) {
            return;
        }
        BoxSizer sizer = sizers[type];

        int numCards = ((Number) cardCount.getValue()).intValue();
        double[] dimInMM = sizer.size(numCards, sleeveThickness);

        // convert mm to pt
        for (int i = 0; i < dimInMM.length; ++i) {
            dimInMM[i] = dimInMM[i] / 25.4d * 72d;
        }
        formatFields(dimInMM);
        updatePreview();
    }

    private void updatePreview() {
        TuckBox oldBox = item;
        item = tuckBoxPreview.getBox();
        copyStyleToItem();
        tuckBoxPreview.setBox(item);
        item = oldBox;
    }
    private int oldUnit = Length.CM;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox boxTypeCombo;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JSpinner cardCount;
    private javax.swing.JComboBox cardTypeCombo;
    private javax.swing.JTextField depthField;
    private javax.swing.JButton extColourButton;
    private javax.swing.JButton extColourLabel;
    private javax.swing.JLabel extLabel;
    private javax.swing.JButton foldColourButton;
    private javax.swing.JButton foldColourLabel;
    private javax.swing.JLabel foldLabel;
    private javax.swing.JCheckBox foldLineCheck;
    private javax.swing.JTextField heightField;
    private javax.swing.JCheckBox hingeCutCheck;
    private javax.swing.JButton intColourButton;
    private javax.swing.JButton intColourLabel;
    private javax.swing.JLabel intLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JButton lineColourButton;
    private javax.swing.JButton lineColourLabel;
    private javax.swing.JLabel lineLabel;
    private javax.swing.JButton okBtn;
    private javax.swing.JPanel previewPanel;
    private javax.swing.JCheckBox roundedSideFlapsCheck;
    private javax.swing.JComboBox sleeveCombo;
    private javax.swing.JLabel sleeveLabel;
    private javax.swing.JLabel spacer;
    private javax.swing.JSpinner thicknessSpinner;
    private javax.swing.JCheckBox thumbNotchCheck;
    private ca.cgjennings.apps.arkham.UnitSelector unitSelector;
    private javax.swing.JTextField widthField;
    // End of variables declaration//GEN-END:variables

    @Override
    public void handleOKAction(ActionEvent e) {
        Length.setDefaultUnit(oldUnit);
        if (!copyStyleToItem()) {
            return;
        }
        item.getPage().getView().repaint();
        dispose();
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }
}
