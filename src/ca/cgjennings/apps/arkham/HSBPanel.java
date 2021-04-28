package ca.cgjennings.apps.arkham;

import static ca.cgjennings.apps.arkham.MarkupTargetFactory.enableTargeting;
import ca.cgjennings.graphics.paints.CheckeredPaint;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.script.mozilla.javascript.NativeArray;
import ca.cgjennings.ui.EyeDropper;
import ca.cgjennings.ui.FilteredDocument;
import ca.cgjennings.ui.OpacityLabel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuListener;
import static resources.Language.string;
import resources.Settings;

/**
 * A custom UI control for choosing HSB colours.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class HSBPanel extends javax.swing.JPanel implements javax.swing.event.ChangeListener, java.awt.event.ActionListener, java.awt.event.FocusListener, PopupMenuListener {
    // needed for form serialization

    private static final long serialVersionUID = -3_518_230_129_696_936_527L;
    private final NumberFormat formatter;

    /**
     * Creates new form HSBPanel
     */
    public HSBPanel() {
        initComponents();
        eyeDropper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        takeOverSlider(hueLabel, HSlider);
        takeOverSlider(saturationLabel, SSlider);
        takeOverSlider(brightnessLabel, BSlider);

        // make untrackable by markup menu
        enableTargeting(HField, false);
        enableTargeting(SField, false);
        enableTargeting(BField, false);
        enableTargeting(webColourField, false);

        formatter = NumberFormat.getNumberInstance();
        formatter.setMaximumFractionDigits(1);
        formatter.setMinimumFractionDigits(1);

        eyeDropper.addPropertyChangeListener(EyeDropper.DROPPER_COLOR_CHANGED, (PropertyChangeEvent evt) -> {
            setHSB((Color) evt.getNewValue());
        });

        //setForeground(sourceColor);
        updateColor(0f, 1f, 1f);
        installAccelerators();
    }

    /**
     * Sets whether a combo box of preset values is shown in the panel.
     *
     * @param visible if true, the preset controls are shown
     */
    public void setPresetsVisible(boolean visible) {
        presetCombo.setVisible(visible);
        presetLabel.setVisible(visible);

        hsbPanel.revalidate();
    }

    /**
     * Returns whether the preset list is visible.
     *
     * @return true if the preset controls are visible
     */
    public boolean isPresetsVisible() {
        return presetLabel.isVisible();
    }

    /**
     * Sets a list of preset values that the user can choose from.Setting an
 empty array has the same effect as passing null, namely, clearing the
 list of presets. Otherwise, the preset values are determined using pairs
 of elements are follows: The string value of the first element in each
 pair is used as the name to display for the preset value. If the second
 value in the pair is a float array, then its first three values determine
 the h, s, and b component of the preset value, respectively. Otherwise,
 the string value of the object is parsed into such an array using
 {@link Settings#tint(java.lang.String)}.
     *
     * <p>
     * Examples:
     * <pre>
     * // clear the preset list
     * panel.setPresets();
     * // create entries "Happy" and "Sad" with values [0.181,0.6,1] and [-0.25,0.33,0.83]
     * panel.setPresets( "Happy", "65,0.6,1", "Sad", "-155,0.33,0.83" );
     * // create entry "Angry" with value [0,1,0.7]
     * panel.setPresets( "Angry", new float[] {0f,1f,0.7f} );
     * // same as above, but in a script instead of Java code:
     * panel.setPresets( "Angry", [0,1,0.7] );
     * </pre>
     *
     * <p>
     * This method will automatically make the preset controls visible when
     * setting a non-empty list, and hide them when setting an empty list.
     *
     * @param presets the list of presets to display, or null
     * @throws NullPointerException if an element is null
     * @throws Settings.ParseError if a tint string value cannot be parsed
     */
    public void setPresets(Object... presets) {
        presetModel = new DefaultComboBoxModel();
        if (presets == null || presets.length == 0) {
            presetList = null;
            presetNames = null;
            presetValues = null;
            setPresetsVisible(false);
        } else {
            if ((presets.length & 1) == 1) {
                throw new IllegalArgumentException("presets must have even length");
            }
            try {
                presetList = presets;
                final int nPresets = presets.length / 2;
                presetNames = new String[nPresets];
                presetValues = new float[nPresets][];
                for (int i = 0; i < nPresets; ++i) {
                    presetNames[i] = presets[i * 2].toString();
                    Object val = presets[i * 2 + 1];

                    // convert a scripted array
                    if (val.getClass() == NativeArray.class) {
                        Object[] jsVal = ((NativeArray) val).toArray();
                        val = new float[3];
                        for (int f = 0; f < 3; ++f) {
                            ((float[]) val)[f] = ((Double) jsVal[f]).floatValue();
                        }
                    }

                    if (val.getClass() == float[].class) {
                        presetValues[i] = Arrays.copyOf((float[]) val, 3);
                    } else {
                        presetValues[i] = Settings.tint(val.toString());
                    }
                    int color = Color.HSBtoRGB(presetValues[i][0], presetValues[i][1], presetValues[i][2]) & 0xffffff;
                    String label = "<html><font color=#" + Integer.toHexString(color) + "><b>\u2022</b></font> " + presetNames[i];
                    presetModel.addElement(label);
                }
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("value must be an tint string or a Number[]");
            } catch (RuntimeException ex) {
                presetNames = null;
                presetValues = null;
                presetList = null;
                throw ex;
            }

            setPresetsVisible(true);
        }
        presetCombo.setModel(presetModel);
    }

    /*
			for( int i = 0; i < tints.size(); ++i ) {
			if( Arrays.equals( tint, tints.get( i ) ) ) {
				HSBPanel.presetCombo.setSelectedIndex( i );
			}
		}
     */
    /**
     * Returns the number of presets, or 0 if none are set.
     *
     * @return the number of preset values
     */
    public int getPresetCount() {
        return presetNames == null ? 0 : presetNames.length;
    }

    /**
     * Returns a the name of the specified preset value.
     *
     * @param n the preset index
     * @return the name of the preset with the specifed index
     */
    public String getPresetName(int n) {
        if (presetNames == null) {
            throw new IndexOutOfBoundsException(String.valueOf(n));
        }
        return presetNames[n];
    }

    /**
     * Returns a copy of the HSB components of the specified preset value.
     *
     * @param n the preset index
     * @return a copy of the preset's HSB values
     */
    public float[] getPresetValue(int n) {
        if (presetNames == null) {
            throw new IndexOutOfBoundsException(String.valueOf(n));
        }
        return presetValues[n].clone();
    }

    /**
     * Returns the list of preset values, or null if no presets have been set.
     * The list will be an exact copy of the passed-in list, or null if no list
     * was set, or the list was empty or null.
     *
     * @return an array of preset values in the same format as
     * {@link #setPresets}
     */
    public Object[] getPresets() {
        return presetList == null ? null : presetList.clone();
    }

    /**
     * Selects the specified preset value.
     *
     * @param n the preset index
     */
    public void selectPreset(int n) {
        presetCombo.setSelectedIndex(n);
    }

    private Object[] presetList;
    private String[] presetNames;
    private float[][] presetValues;
    private DefaultComboBoxModel presetModel;

    /**
     * Sets the selected HSB value to match the specified color.
     *
     * @param color the color to match
     */
    public void setHSB(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        updateColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Sets the selected HSB value.
     *
     * @param h the hue value to set
     * @param s the saturation value to set
     * @param b the brightness value to set
     */
    public void setHSB(float h, float s, float b) {
        updateColor(h, s, b);
    }

    /**
     * Sets the selected HSB value using the first three elements of the
     * specified array.
     *
     * @param hsb an array of at least 3 float values
     */
    public void setHSB(float[] hsb) {
        updateColor(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Returns the selected HSB value.
     *
     * @return an array containing the hue, saturation, and brightness values
     * @see #setHSB(float[])
     */
    public float[] getHSB() {
        return Arrays.copyOf(hsb, hsb.length);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tintPopup = new javax.swing.JPopupMenu();
        copyTintItem = new javax.swing.JMenuItem();
        pasteTintItem = new javax.swing.JMenuItem();
        hsbPanel = new javax.swing.JPanel();
        SSlider = new javax.swing.JSlider();
        BField = new javax.swing.JTextField();
        HField = new javax.swing.JTextField();
        presetCombo = new javax.swing.JComboBox();
        hueLabel = new ca.cgjennings.ui.HueLabel();
        jLabel5 = new javax.swing.JLabel();
        SField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        BSlider = new javax.swing.JSlider();
        brightnessLabel = new ca.cgjennings.ui.BrightnessLabel();
        HSlider = new javax.swing.JSlider();
        jLabel7 = new javax.swing.JLabel();
        saturationLabel = new ca.cgjennings.ui.SaturationLabel();
        presetLabel = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        swatchPanel = new javax.swing.JPanel();
        swatch = new javax.swing.JLabel();
        webColourField = new javax.swing.JTextField();
        eyeDropper = new ca.cgjennings.ui.EyeDropper();
        jLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();

        tintPopup.addPopupMenuListener(this);

        copyTintItem.setText(string("hsb-b-copy")); // NOI18N
        copyTintItem.addActionListener(this);
        tintPopup.add(copyTintItem);

        pasteTintItem.setText(string("hsb-b-paste")); // NOI18N
        pasteTintItem.addActionListener(this);
        tintPopup.add(pasteTintItem);

        setBorder(javax.swing.BorderFactory.createTitledBorder(string("hsb-l-title"))); // NOI18N
        setComponentPopupMenu(tintPopup);

        SSlider.setMajorTickSpacing(500);
        SSlider.setMaximum(1000);
        SSlider.setInheritsPopupMenu(true);
        SSlider.addChangeListener(this);

        BField.setColumns(6);
        BField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        BField.setText("100.0%");
        BField.addActionListener(this);
        BField.addFocusListener(this);

        HField.setColumns(6);
        HField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        HField.setText("+180.0°");
        HField.addActionListener(this);
        HField.addFocusListener(this);

        presetCombo.setInheritsPopupMenu(true);
        presetCombo.addActionListener(this);

        hueLabel.setText("hueLabel1");

        jLabel5.setText("0%");

        SField.setColumns(6);
        SField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        SField.setText("100.0%");
        SField.addActionListener(this);
        SField.addFocusListener(this);

        jLabel9.setText("100%");

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel4.setText(string("hsb-l-brightness")); // NOI18N

        jLabel10.setText("+180°");

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel3.setText(string("hsb-l-saturation")); // NOI18N

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel2.setText(string("hsb-l-hue")); // NOI18N

        BSlider.setMajorTickSpacing(500);
        BSlider.setMaximum(1000);
        BSlider.setInheritsPopupMenu(true);
        BSlider.addChangeListener(this);

        brightnessLabel.setText("brightnessLabel1");

        HSlider.setMajorTickSpacing(1800);
        HSlider.setMaximum(1800);
        HSlider.setMinimum(-1800);
        HSlider.setInheritsPopupMenu(true);
        HSlider.addChangeListener(this);

        jLabel7.setText("-180°");

        saturationLabel.setText("saturationLabel1");

        presetLabel.setFont(presetLabel.getFont().deriveFont(presetLabel.getFont().getStyle() | java.awt.Font.BOLD));
        presetLabel.setText(string("hsb-l-preset")); // NOI18N

        jLabel6.setText("0%");

        jLabel8.setText("100%");

        javax.swing.GroupLayout hsbPanelLayout = new javax.swing.GroupLayout(hsbPanel);
        hsbPanel.setLayout(hsbPanelLayout);
        hsbPanelLayout.setHorizontalGroup(
            hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hsbPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel2)
                    .addComponent(HField)
                    .addComponent(jLabel3)
                    .addComponent(SField)
                    .addComponent(jLabel4)
                    .addComponent(BField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(presetLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(presetCombo, 0, 194, Short.MAX_VALUE)
                    .addComponent(BSlider, 0, 0, Short.MAX_VALUE)
                    .addComponent(brightnessLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                    .addComponent(SSlider, 0, 0, Short.MAX_VALUE)
                    .addComponent(saturationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                    .addComponent(HSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE)
                    .addComponent(hueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 194, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel10)
                    .addComponent(jLabel9))
                .addContainerGap())
        );

        hsbPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {BField, HField, SField});

        hsbPanelLayout.setVerticalGroup(
            hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hsbPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(hueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(HField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(HSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(saturationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(SField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(SSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel4)
                    .addComponent(brightnessLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(BField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(BSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(hsbPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(presetLabel)
                    .addComponent(presetCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        swatch.setBackground(new java.awt.Color(51, 102, 0));
        swatch.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        swatch.setOpaque(true);

        webColourField.setColumns(6);
        webColourField.setDocument( FilteredDocument.createHexDocument() );
        webColourField.setText("FFFFFF");
        Font mono = new Font( Font.MONOSPACED, Font.PLAIN, 10 );
        mono = mono.deriveFont( webColourField.getFont().getSize2D()-1 );
        webColourField.setFont( mono );
        webColourField.addActionListener(this);
        webColourField.addFocusListener(this);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()-1f));
        jLabel1.setText("#");

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getSize()-1f));
        jLabel11.setText(string( "hsb-l-web-colour" )); // NOI18N

        javax.swing.GroupLayout swatchPanelLayout = new javax.swing.GroupLayout(swatchPanel);
        swatchPanel.setLayout(swatchPanelLayout);
        swatchPanelLayout.setHorizontalGroup(
            swatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(swatchPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(swatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(swatch, javax.swing.GroupLayout.DEFAULT_SIZE, 65, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, swatchPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(1, 1, 1)
                        .addGroup(swatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addGroup(swatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                .addComponent(eyeDropper, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(webColourField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );
        swatchPanelLayout.setVerticalGroup(
            swatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(swatchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(swatch, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel11)
                .addGap(1, 1, 1)
                .addGroup(swatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(webColourField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(eyeDropper, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(48, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(hsbPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(swatchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(hsbPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(swatchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (evt.getSource() == BField) {
            HSBPanel.this.HSBFieldsActionPerformed(evt);
        }
        else if (evt.getSource() == HField) {
            HSBPanel.this.HSBFieldsActionPerformed(evt);
        }
        else if (evt.getSource() == SField) {
            HSBPanel.this.HSBFieldsActionPerformed(evt);
        }
        else if (evt.getSource() == webColourField) {
            HSBPanel.this.webColourFieldActionPerformed(evt);
        }
        else if (evt.getSource() == copyTintItem) {
            HSBPanel.this.copyTintItemActionPerformed(evt);
        }
        else if (evt.getSource() == pasteTintItem) {
            HSBPanel.this.pasteTintItemActionPerformed(evt);
        }
        else if (evt.getSource() == presetCombo) {
            HSBPanel.this.presetComboActionPerformed(evt);
        }
    }

    public void focusGained(java.awt.event.FocusEvent evt) {
        if (evt.getSource() == webColourField) {
            HSBPanel.this.webColourFieldFocusGained(evt);
        }
    }

    public void focusLost(java.awt.event.FocusEvent evt) {
        if (evt.getSource() == BField) {
            HSBPanel.this.HSBFieldsFocusLost(evt);
        }
        else if (evt.getSource() == HField) {
            HSBPanel.this.HSBFieldsFocusLost(evt);
        }
        else if (evt.getSource() == SField) {
            HSBPanel.this.HSBFieldsFocusLost(evt);
        }
        else if (evt.getSource() == webColourField) {
            HSBPanel.this.webColourFieldFocusLost(evt);
        }
    }

    public void stateChanged(javax.swing.event.ChangeEvent evt) {
        if (evt.getSource() == SSlider) {
            HSBPanel.this.HSBSliderStateChanged(evt);
        }
        else if (evt.getSource() == BSlider) {
            HSBPanel.this.HSBSliderStateChanged(evt);
        }
        else if (evt.getSource() == HSlider) {
            HSBPanel.this.HSBSliderStateChanged(evt);
        }
    }

    public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
    }

    public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
    }

    public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
        if (evt.getSource() == tintPopup) {
            HSBPanel.this.tintPopupPopupMenuWillBecomeVisible(evt);
        }
    }// </editor-fold>//GEN-END:initComponents
    private void HSBFieldsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_HSBFieldsFocusLost
        HSBFieldsActionPerformed(null);
    }//GEN-LAST:event_HSBFieldsFocusLost

    private void HSBFieldsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HSBFieldsActionPerformed
        if (internalUpdate == 0) {
            ++internalUpdate;
            try {
                updateColor(
                        parseFloatKindly(HField.getText()) / 360f,
                        parseFloatKindly(SField.getText()) / 100f,
                        parseFloatKindly(BField.getText()) / 100f);
            } catch (NumberFormatException e) {
                Toolkit.getDefaultToolkit().beep();
            }
            --internalUpdate;
        }
    }//GEN-LAST:event_HSBFieldsActionPerformed

    private float parseFloatKindly(String text) throws NumberFormatException {
        StringBuilder b = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ++i) {
            final char c = text.charAt(i);
            if (Character.isDigit(c) || c == '-' || c == ',' || c == '.' || c == 'e') {
                b.append(c);
            }
        }
        if (b.length() != text.length()) {
            text = b.toString();
        }

        try {
            return formatter.parse(text).floatValue();
        } catch (ParseException e) {
            throw new NumberFormatException();
        }
    }

    private void HSBSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_HSBSliderStateChanged
        if (internalUpdate == 0) {
            ++internalUpdate;

            float h = HSlider.getValue() / 3600f;
            float s = SSlider.getValue() / 1000f;
            float b = BSlider.getValue() / 1000f;

            updateColor(h, s, b);

            --internalUpdate;
        }
    }//GEN-LAST:event_HSBSliderStateChanged

    private void installAccelerators() {
        KeyStroke copy = PlatformSupport.getKeyStroke("menu C");
        KeyStroke paste = PlatformSupport.getKeyStroke("menu V");

        InputMap imap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap amap = getActionMap();

        addAccelerator(imap, amap, copy, "COPY TINT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyTint();
            }
        });
        addAccelerator(imap, amap, paste, "PASTE TINT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteTint();
            }
        });
    }

    private void addAccelerator(InputMap imap, ActionMap amap, KeyStroke key, String command, Action a) {
        imap.put(key, command);
        amap.put(command, a);
    }

    private void tintPopupPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_tintPopupPopupMenuWillBecomeVisible
        pasteTintItem.setEnabled(clipB == clipB);
    }//GEN-LAST:event_tintPopupPopupMenuWillBecomeVisible

    private void copyTintItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyTintItemActionPerformed
        copyTint();
    }//GEN-LAST:event_copyTintItemActionPerformed

    private void pasteTintItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteTintItemActionPerformed
        pasteTint();
    }//GEN-LAST:event_pasteTintItemActionPerformed

	private void webColourFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_webColourFieldActionPerformed
            String hex = webColourField.getText();
            // fix length to either 3 or 6
            if (hex.length() > 6) {
                hex = hex.substring(hex.length() - 6);
            } else if (hex.length() < 3) {
                while (hex.length() < 3) {
                    hex = "0" + hex;
                }
            } else if (hex.length() > 3 && hex.length() < 6) {
                while (hex.length() < 6) {
                    hex = "0" + hex;
                }
            }

            // convert 3 form to 6 form
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1)
                        + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            }

            int rgb = 0;
            try {
                rgb = Integer.valueOf(hex, 16);
            } catch (NumberFormatException e) {
            }

            webColourField.setText(hex);

            Color c = new Color(rgb);
            doNotUpdateWebColour = true;
            setHSB(c);
            doNotUpdateWebColour = false;
	}//GEN-LAST:event_webColourFieldActionPerformed
    private boolean doNotUpdateWebColour;

	private void webColourFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_webColourFieldFocusLost
            webColourFieldActionPerformed(null);
	}//GEN-LAST:event_webColourFieldFocusLost

	private void webColourFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_webColourFieldFocusGained
            webColourField.selectAll();
	}//GEN-LAST:event_webColourFieldFocusGained

    private void presetComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_presetComboActionPerformed
        int index = presetCombo.getSelectedIndex();
        if (index >= 0 && presetList != null && internalUpdate == 0) {
            setHSB(presetValues[index]);
        }
    }//GEN-LAST:event_presetComboActionPerformed

    public void copyTint() {
        clipH = ch;
        clipS = cs;
        clipB = cb;
        StringSelection clip = new StringSelection(String.format("%.3f,%.3f,%.3f", ch * 360f, cs, cb));
        getToolkit().getSystemClipboard().setContents(clip, clip);
    }

    public void pasteTint() {
        if (clipB == clipB) {
            updateColor(clipH, clipS, clipB);
        }
    }
    /**
     * A shared "clipboard" for copying and pasting tint values across editors.
     * If <code>clipB</code> is set to <code>NaN</code>, then nothing has been
     * copied yet during this session. This scheme does not work across virtual
     * machines.
     */
    private static float clipH, clipS, clipB = Float.NaN;
    private int internalUpdate = 0;
    private float ch, cs, cb;

    private void updateColor(float h, float s, float b) {
        ++internalUpdate;

        Color.RGBtoHSB(sourceColor.getRed(), sourceColor.getGreen(), sourceColor.getBlue(), hsb);
        hsb[0] += h;

        // TODO: clean up and simplify
        while (hsb[0] > 0.5f) {
            hsb[0] -= 1f;
        }
        while (hsb[0] < -0.5f) {
            hsb[0] += 1f;
        }

        hsb[1] = s;
        if (hsb[1] < 0f) {
            hsb[1] = 0f;
        }
        if (hsb[1] > 1f) {
            hsb[1] = 1f;
        }
        hsb[2] = b;
        if (hsb[2] < 0f) {
            hsb[2] = 0f;
        }
        if (hsb[2] > 1f) {
            hsb[2] = 1f;
        }

        h = hsb[0];
        s = hsb[1];
        b = hsb[2];

        Color rgb = new Color(Color.HSBtoRGB(h, s, b));

        if (isEnabled()) {
            swatch.setBackground(rgb);
        } else {
            swatch.setBackground(new Color(Color.HSBtoRGB(h, 0f, b)));
        }

        saturationLabel.setHue(h);
        brightnessLabel.setHue(h);

        if (tintable != null) {
            tintable.setTint(h, s, b);
        }

        ch = h;
        cs = s;
        cb = b;

        HSlider.setValue(Math.round(h * 3_600));
        SSlider.setValue(Math.round(s * 1_000));
        BSlider.setValue(Math.round(b * 1_000));
        HField.setText(formatter.format(h * 360) + "\u00b0");
        SField.setText(formatter.format(s * 100) + "%");
        BField.setText(formatter.format(b * 100) + "%");

        if (!doNotUpdateWebColour) {
            int rgb32 = rgb.getRGB() & 0xff_ffff;
            webColourField.setText(String.format("%06x", rgb32));
        }

        if (presetList != null) {
            for (int i = 0, len = presetValues.length; i < len; ++i) {
                float[] preset = presetValues[i];
                if (Arrays.equals(hsb, preset)) {
                    presetCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        --internalUpdate;
    }

    public static void takeOverSlider(final JLabel label, final JSlider slider) {
        if (!UIManager.getLookAndFeel().getName().equals("Nimbus")) {
            return;
        }

        if (thumbPainter == null) {
            thumbPainter = (Graphics2D g, Object c, int w, int h) -> {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(Color.GRAY);
                Composite oldComp = g.getComposite();
                g.setComposite(AlphaComposite.SrcOver.derive(0.5f));
                g.fillOval(1, 1, w - 3, h - 3);
                g.drawOval(1, 1, w - 3, h - 3);
                
                g.setColor(Color.DARK_GRAY);
                g.drawArc(1, 1, w - 3, h - 3, 45, -180);
                g.setColor(Color.WHITE);
                g.drawArc(1, 1, w - 3, h - 3, 45, 180);
                
                g.setComposite(oldComp);
                g.drawOval(4, 4, w - 10, h - 10);
            };
        }

        boolean isOpacity = label instanceof OpacityLabel;

        //label.getParent().remove( label );
        label.setVisible(false);
        final BufferedImage texture = new BufferedImage(256, 1, isOpacity ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        label.setSize(256, 1);
        Graphics2D g = texture.createGraphics();
        try {
            if (isOpacity) {
                g.setPaint(new LinearGradientPaint(
                        0, 0, 255, 0, new float[]{0f, 1f}, new Color[]{new Color(255, 0, 0, 0), new Color(255, 0, 0, 255)}
                ));
                g.fillRect(0, 0, 256, 1);
            } else {
                label.paint(g);
            }
        } finally {
            g.dispose();
        }

        final Paint underTexture;
        if (!(label instanceof OpacityLabel)) {
            underTexture = null;
        } else {
            underTexture = new CheckeredPaint(4);
        }

        Painter backPainter = (Graphics2D g1, Object c, int w, int h) -> {
            g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean enable = true;
            if (c instanceof JComponent) {
                enable = ((JComponent) c).isEnabled();
            }
            if (enable) {
                if (underTexture != null) {
                    g1.setPaint(underTexture);
                    g1.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
                }
                g1.setPaint(new TexturePaint(texture, new Rectangle(0, 0, w, h)));
                g1.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);
            }
            g1.setColor(enable ? Color.DARK_GRAY : Color.LIGHT_GRAY);
            g1.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
        };

        UIDefaults sliderDefaults = new UIDefaults();
        sliderDefaults.put("Slider.thumbWidth", 16);
        sliderDefaults.put("Slider.thumbHeight", 16);
        sliderDefaults.put("Slider:SliderThumb.backgroundPainter", thumbPainter);
        sliderDefaults.put("Slider:SliderTrack.backgroundPainter", backPainter);
        slider.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        slider.putClientProperty("Nimbus.Overrides", sliderDefaults);
    }

    private static Painter thumbPainter;

    private Tintable tintable;
    private final float[] hsb = new float[3];
    private final Color sourceColor = Color.RED;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField BField;
    private javax.swing.JSlider BSlider;
    private javax.swing.JTextField HField;
    private javax.swing.JSlider HSlider;
    private javax.swing.JTextField SField;
    private javax.swing.JSlider SSlider;
    private ca.cgjennings.ui.BrightnessLabel brightnessLabel;
    private javax.swing.JMenuItem copyTintItem;
    private ca.cgjennings.ui.EyeDropper eyeDropper;
    private javax.swing.JPanel hsbPanel;
    private ca.cgjennings.ui.HueLabel hueLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuItem pasteTintItem;
    javax.swing.JComboBox presetCombo;
    private javax.swing.JLabel presetLabel;
    private ca.cgjennings.ui.SaturationLabel saturationLabel;
    private javax.swing.JLabel swatch;
    private javax.swing.JPanel swatchPanel;
    private javax.swing.JPopupMenu tintPopup;
    private javax.swing.JTextField webColourField;
    // End of variables declaration//GEN-END:variables

    public Tintable getTintable() {
        return tintable;
    }

    public void setTintable(Tintable tintable, boolean matchTintableToPanel) {
        this.tintable = tintable;
        if (matchTintableToPanel) {
            updateColor(ch, cs, cb);
        }
    }

    @Override
    public void setEnabled(boolean enable) {
        if (enable == isEnabled()) {
            return;
        }

        super.setEnabled(enable);
        recursiveEnable(this, enable);

        HSBSliderStateChanged(null);
    }

    private void recursiveEnable(Container parent, boolean enable) {
        for (Component child : parent.getComponents()) {
            child.setEnabled(enable);
            if (child instanceof Container) {
                recursiveEnable((Container) child, enable);
            }
        }
    }

    /**
     * Sets the title displayed for the panel.
     *
     * @param panelTitle title text
     */
    public void setTitle(String panelTitle) {
        ((TitledBorder) getBorder()).setTitle(panelTitle);
    }

    /**
     * Returns the title displayed for the panel.
     *
     * @return the title text
     */
    public String getTitle() {
        return ((TitledBorder) getBorder()).getTitle();
    }

    @Override
    public String toString() {
        return "" + Float.toString(hsb[0] * 360f)
                + "," + Float.toString(hsb[1])
                + "," + Float.toString(hsb[2]);
    }
}
