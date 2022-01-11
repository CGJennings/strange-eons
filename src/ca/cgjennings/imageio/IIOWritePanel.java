package ca.cgjennings.imageio;

import ca.cgjennings.imageio.plugins.jpeg2000.J2KImageWriteParam;
import ca.cgjennings.ui.JUtilities;
import java.text.NumberFormat;
import java.util.Hashtable;
import javax.imageio.ImageWriteParam;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import static resources.Language.string;
import resources.Settings;

/**
 * A panel that can be used to configure an {@link ImageWriteParam}'s
 * compression method, compression quality, and progressive scan options.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class IIOWritePanel extends javax.swing.JPanel {

    /**
     * A property fired when the image write parameters change.
     */
    public static final String PARAMETERS_PROPERTY = "image_write_param";

    /**
     * Creates a new panel for configuring image writing options. Options that
     * allow choosing a compression mode will be hidden.
     */
    public IIOWritePanel() {
        this(false);
    }

    /**
     * Creates a new panel for configuring image writing options.
     *
     * @param fullComplexityMode if {@code true}, compression mode options will
     * be shown
     */
    public IIOWritePanel(boolean fullComplexityMode) {
        initComponents();
        // lock size of percent label for 100%
        percentLabel.setPreferredSize(percentLabel.getPreferredSize());

        JUtilities.installDisabledHTMLFix(qualityLabel);
        setImageWriteParam(null);
        if (!fullComplexityMode) {
            compressCheck.setVisible(false);
            typeCombo.setVisible(false);
        }

        formatter.setMaximumFractionDigits(0);
        final int inc = 25;
        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        for (int i = 0; i <= 100; i += inc) {
            JLabel label = new JLabel(formatter.format(i / 100d));
            label.setFont(qualitySlider.getFont());
            labels.put(i, label);
        }
        qualitySlider.setLabelTable(labels);
        updatePercent();
    }

    /**
     * Sets the write param instance to be edited by this panel.
     *
     * @param iwp the write param to edit
     */
    public void setImageWriteParam(ImageWriteParam iwp) {
        p = new Parameters();
        if (iwp == null) {
            p.canCompress = false;
            p.canDisableCompress = false;
            p.canProgressive = false;
            p.compressed = false;
            p.compressionQuality = 1f;
            p.compressionType = null;
            p.compressionTypes = null;
        } else {
            p.initFrom(iwp);
        }
        initAllControls();
        firePropertyChange(PARAMETERS_PROPERTY, null, null);
    }

    /**
     * Returns the edited write param.
     *
     * @return the write param instance edited by the panel
     */
    public ImageWriteParam getImageWriteParam() {
        if (p == null) {
            return null;
        } else {
            return p.iwp;
        }
    }

    /**
     * If compression is supported and the quality can be modified, sets the
     * compression quality.
     *
     * @param quality the quality value to set, from 0 to 1 inclusive
     */
    public void setCompressionQuality(float quality) {
        if (p.canCompress && p.compressed) {
            qualitySlider.setValue(Math.round(quality * 100f));
        }
    }

    /**
     * Returns the current compression quality as a value from 0 to 1.
     *
     * @return the compression quality
     */
    public float getCompressionQuality() {
        return qualitySlider.getValue() / 100f;
    }

    /**
     * If the progressive scan option can be modified, sets the progressive scan
     * option.
     *
     * @param enable the progressive scan option
     */
    public void setProgressiveScanEnabled(boolean enable) {
        if (p.canProgressive) {
            progressiveCheck.setSelected(enable);
            progressiveCheckActionPerformed(null);
        }
    }

    /**
     * Returns {@code true} if the progressive scan option is selected.
     *
     * @return whether progressive scan is enabled
     */
    public boolean isProgressiveScanEnabled() {
        return progressiveCheck.isSelected();
    }

    /**
     * If the compression option can be modified, sets the compression option.
     *
     * @param enable the compresssion option
     */
    public void setCompressionEnabled(boolean enable) {
        if (compressCheck.isEnabled() && compressCheck.isVisible()) {
            compressCheck.setSelected(enable);
            compressCheckActionPerformed(null);
        }
    }

    /**
     * Returns {@code true} if the compression option is selcted.
     *
     * @return whether compression is enabled
     */
    public boolean isCompressionScanEnabled() {
        return compressCheck.isSelected();
    }

    /**
     * If multiple compression methods are supported, sets the compression
     * method. If the name is invalid, selects a default method.
     *
     * @param type the name of the method
     */
    public void setCompressionMethod(String type) {
        if (typeCombo.getModel().getSize() > 1) {
            typeCombo.setSelectedItem(type);
            if (typeCombo.getSelectedIndex() < 0) {
                typeCombo.setSelectedIndex(0);
            }
        }
    }

    /**
     * Returns the name of the selected compression method.
     *
     * @return the selected compression method
     */
    public String getCompressionMethod() {
        return (String) typeCombo.getSelectedItem();
    }

    /**
     * Returns a (possible empty) array of the names of the available
     * compression methods.
     *
     * @return the supported compression methods
     */
    public String[] getCompressionMethods() {
        return p.compressionTypes == null ? new String[0] : p.compressionTypes.clone();
    }

    /**
     * Uses the specified parameters (and underlying write param) for the panel.
     *
     * @param p
     */
    void setParameters(Parameters p) {
        if (p == null) {
            throw new NullPointerException("p");
        }
        if (this.p != p) {
            this.p = p;
            initAllControls();
        }
    }

    /**
     * Use this panel to configure the features of an image writer. The writers
     * {@code IOWriteParam} will replace the one currently being edited, if any.
     *
     * @param iw the writer to configure
     */
    public void configureWriter(SimpleImageWriter iw) {
        setParameters(iw.getParameters());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        typeCombo = new javax.swing.JComboBox<>();
        qualityLabel = new javax.swing.JLabel();
        qualitySlider = new javax.swing.JSlider();
        qualityDesc = new javax.swing.JLabel();
        progressiveCheck = new javax.swing.JCheckBox();
        compressCheck = new javax.swing.JCheckBox();
        percentLabel = new javax.swing.JLabel();

        typeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typeComboActionPerformed(evt);
            }
        });

        qualityLabel.setText(string( "exf-l-image-quality" )); // NOI18N

        qualitySlider.setFont(qualitySlider.getFont().deriveFont(qualitySlider.getFont().getSize()-3f));
        qualitySlider.setMajorTickSpacing(25);
        qualitySlider.setMinorTickSpacing(5);
        qualitySlider.setPaintLabels(true);
        qualitySlider.setPaintTicks(true);
        qualitySlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                qualitySliderStateChanged(evt);
            }
        });

        qualityDesc.setFont(qualityDesc.getFont().deriveFont(qualityDesc.getFont().getStyle() | java.awt.Font.BOLD, qualityDesc.getFont().getSize()-1));
        qualityDesc.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        qualityDesc.setText("description");

        progressiveCheck.setText(string( "exf-b-progressive" )); // NOI18N
        progressiveCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                progressiveCheckActionPerformed(evt);
            }
        });

        compressCheck.setText(string( "exf-l-compress" )); // NOI18N
        compressCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compressCheckActionPerformed(evt);
            }
        });

        percentLabel.setText("100%");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(compressCheck)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(typeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(progressiveCheck, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(qualityLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(qualityDesc, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                            .addComponent(qualitySlider, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(percentLabel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(compressCheck)
                    .addComponent(typeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(qualityLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(qualitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(percentLabel))
                .addGap(1, 1, 1)
                .addComponent(qualityDesc)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressiveCheck)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void updatePercent() {
        percentLabel.setText(formatter.format(qualitySlider.getValue() / 100d));
        percentLabel.setEnabled(qualitySlider.isEnabled());
    }

	private void typeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typeComboActionPerformed
            if (typeCombo.getSelectedItem() == null) {
                return;
            }
            p.setCompressionType(typeCombo.getSelectedItem().toString());
            initQualities();
            firePropertyChange(PARAMETERS_PROPERTY, null, null);
	}//GEN-LAST:event_typeComboActionPerformed

	private void qualitySliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_qualitySliderStateChanged
            p.setCompressionQuality(qualitySlider.getValue() / 100f);
            updateQualityDesc();
            updatePercent();
            if (!qualitySlider.getValueIsAdjusting()) {
                firePropertyChange(PARAMETERS_PROPERTY, null, null);
            }
	}//GEN-LAST:event_qualitySliderStateChanged

	private void compressCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compressCheckActionPerformed
            p.setCompression(compressCheck.isSelected());
            initAllControls();
            firePropertyChange(PARAMETERS_PROPERTY, null, null);
	}//GEN-LAST:event_compressCheckActionPerformed

	private void progressiveCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_progressiveCheckActionPerformed
            p.setProgressive(progressiveCheck.isSelected());
            firePropertyChange(PARAMETERS_PROPERTY, null, null);
	}//GEN-LAST:event_progressiveCheckActionPerformed

    private Parameters p;
    private NumberFormat formatter = NumberFormat.getPercentInstance();
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox compressCheck;
    private javax.swing.JLabel percentLabel;
    private javax.swing.JCheckBox progressiveCheck;
    private javax.swing.JLabel qualityDesc;
    private javax.swing.JLabel qualityLabel;
    private javax.swing.JSlider qualitySlider;
    private javax.swing.JComboBox<String> typeCombo;
    // End of variables declaration//GEN-END:variables

    private void initAllControls() {
        if (p.canCompress && p.compressed && p.compressionTypes != null && p.compressionTypes.length > 0) {
            typeCombo.setModel(new DefaultComboBoxModel<>(p.compressionTypes));
            typeCombo.setSelectedItem(p.compressionType);
            typeCombo.setEnabled(true);
        } else {
            typeCombo.setModel(new DefaultComboBoxModel<>());
            typeCombo.setEnabled(false);
        }

        initCheapControls();
        initQualities();
    }

    private void initCheapControls() {
        if (p.canCompress && p.canDisableCompress) {
            compressCheck.setEnabled(true);
            compressCheck.setSelected(p.compressed);
        } else {
            compressCheck.setEnabled(false);
            compressCheck.setSelected(p.canCompress);
        }

        progressiveCheck.setSelected(p.isProgressive);
        progressiveCheck.setEnabled(p.canProgressive);
    }

    private void initQualities() {
        boolean enable;
        if (p.canCompress && p.compressed) {
            enable = true;
            qualitySlider.setValue(Math.round(p.compressionQuality * 100f));
            updateQualityDesc();
        } else {
            enable = false;
            qualityDesc.setText(" ");
            qualitySlider.setValue(100);
        }
        qualityLabel.setEnabled(enable);
        qualitySlider.setEnabled(enable);
        percentLabel.setEnabled(enable);
        updatePercent();
    }

    private void updateQualityDesc() {
        if (!qualitySlider.isEnabled() || p.standardQualityNames == null) {
            qualityDesc.setText(" ");
        } else {
            for (int i = 1; i < p.standardQualities.length; ++i) {
                if (p.compressionQuality < p.standardQualities[i]) {
                    qualityDesc.setText(p.standardQualityNames[i - 1]);
                    return;
                }
            }
            qualityDesc.setText(p.standardQualityNames[p.standardQualityNames.length - 1]);
        }
    }

    /**
     * This package private class implements a thin abstraction layer over an
     * {@code ImageWriteParam} instance. It is used by classes in this package
     * to manage compression options when writing images.
     *
     * @author Chris Jennings <https://cgjennings.ca/contact>
     * @since 3.0
     */
    static class Parameters implements Cloneable {

        private ImageWriteParam iwp;
        // if iwp is a J2K instance, this will be equal to iwp; the JPEG2000
        // options ignore some settings and need to be controlled directly
        // via lower-level methods
        private J2KImageWriteParam j2k;

        // only set when reading from IIOParam
        boolean canCompress;
        boolean canDisableCompress;

        boolean compressed;

        String[] compressionTypes;
        String compressionType;
        boolean lossless;

        float compressionQuality;
        float[] standardQualities;
        String[] standardQualityNames;

        boolean canProgressive;
        boolean isProgressive;

        public Parameters() {
        }

        public void initFrom(ImageWriteParam iwp) {
            this.iwp = iwp;
            j2k = iwp instanceof J2KImageWriteParam ? (J2KImageWriteParam) iwp : null;

            canProgressive = iwp.canWriteProgressive();
            if (canProgressive) {
                int mode = iwp.getProgressiveMode();
                if (mode == ImageWriteParam.MODE_COPY_FROM_METADATA) {
                    isProgressive = false;
                    iwp.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
                } else {
                    isProgressive = mode != ImageWriteParam.MODE_DISABLED;
                }
            } else {
                isProgressive = false;
                if (j2k != null) {
                    isProgressive = true;
                }
            }

            try {
                iwp.setCompressionMode(ImageWriteParam.MODE_DISABLED);
                canDisableCompress = true;
            } catch (UnsupportedOperationException e) {
                canDisableCompress = false;
            }

            compressed = false;
            canCompress = iwp.canWriteCompressed();
            if (canCompress) {
                compressed = true;
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                compressionTypes = iwp.getCompressionTypes();
                compressionType = iwp.getCompressionType();
                if (compressionType == null && compressionTypes != null && compressionTypes.length > 0) {
                    compressionType = compressionTypes[0];
                }
                setCompressionType(compressionType);
                if (j2k != null) {
                    double bpp = j2k.getEncodingRate();
                    if (bpp == Double.MAX_VALUE) {
                        compressionQuality = 1f;
                    } else {
                        compressionQuality = (float) ((bpp - MIN_BPP) / (MAX_BPP - MIN_BPP));
                        if (compressionQuality < 0f) {
                            compressionQuality = 0f;
                        } else if (compressionQuality > 1f) {
                            compressionQuality = 1f;
                        }
                    }
                } else {
                    compressionQuality = iwp.getCompressionQuality();
                }
                setCompressionQuality(compressionQuality);
            }
        }

        public void setCompression(boolean enable) {
            if (enable && canCompress) {
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwp.setCompressionType(compressionType);
                compressed = true;
                setCompressionQuality(compressionQuality);
            } else if (!enable && canDisableCompress) {
                iwp.setCompressionMode(ImageWriteParam.MODE_DISABLED);
                compressed = false;
            }
        }

        /**
         * Changes the compression type and updates the compression quality
         * names.
         *
         * @param type
         */
        public void setCompressionType(String compressionType) {
            if (!compressed) {
                if (!canCompress) {
                    return;
                }
                compressed = true;
                iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            }
            if (compressionType == null) {
                compressionType = compressionTypes[0];
            }
            iwp.setCompressionType(compressionType);
            if (iwp instanceof J2KImageWriteParam) {
                standardQualityNames = new String[]{
                    string("exf-l-lowqual"), string("exf-l-medqual"), string("exf-l-highqual"), string("exf-l-maxqual")
                };
                standardQualities = new float[]{
                    0f, 0.22f, 0.74f, 1f
                };
                lossless = compressionQuality == 1f;
            } else {
                standardQualityNames = iwp.getCompressionQualityDescriptions();
                standardQualities = iwp.getCompressionQualityValues();
                lossless = iwp.isCompressionLossless();
            }
        }

        public void setCompressionQuality(float q) {
            if (canCompress && compressed) {
                compressionQuality = q;

                if (j2k != null) {
                    double rate;
                    if (q > 0.999f) {
                        rate = Double.MAX_VALUE;
                    } else {
                        rate = Math.min(1f, q + 0.0019f);
                        rate = MIN_BPP + (q * (MAX_BPP - MIN_BPP));
                    }
                    j2k.setEncodingRate(rate);
                    lossless = compressionQuality == 1f;
                } else {
                    iwp.setCompressionQuality(q);
                }
            }
        }

        private static final double MIN_BPP = 0.2d, MAX_BPP = 4d;

        public void setProgressive(boolean progressive) {
            if (canProgressive) {
                if (progressive) {
                    iwp.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
                    isProgressive = true;
                } else {
                    iwp.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
                    isProgressive = false;
                }
            }
        }

        public ImageWriteParam getImageWriteParam() {
            return iwp;
        }

        @Override
        public Parameters clone() {
            try {
                return (Parameters) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

//	public static void main( String[] args ) {
//		ca.cgjennings.imageio.JPEG2000.registerServiceProviders();
//
//		for(String n:ImageIO.getWriterMIMETypes())System.err.println( n );
//
//		for( String s:ImageIO.getReaderFileSuffixes())System.err.println( s );
//
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				ImageWriteParam iwp = ImageIO.getImageWritersBySuffix( "bmp" ).next().getDefaultWriteParam();
//				JFrame d = new JFrame();
//				d.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//				IIOWritePanel p = new IIOWritePanel();
//				p.setImageWriteParam( iwp );
//				d.add( p );
//				d.pack();
//				d.setLocationRelativeTo( null );
//				d.setVisible( true );
//			}
//		});
//	}
    public void loadDefaults(String format) {
        Settings s = Settings.getUser();

        boolean compress = s.getYesNo(key(format, "compress"));
        if (compressCheck.isEnabled()) {
            compressCheck.setSelected(compress);
        }

        String type = s.get(key(format, "type"));
        if (type != null) {
            DefaultComboBoxModel model = (DefaultComboBoxModel) typeCombo.getModel();
            for (int i = 0; i < model.getSize(); ++i) {
                String target = model.getElementAt(i).toString();
                if (target.equals(type)) {
                    typeCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        int quality = s.getInt(key(format, "quality"));
        if (quality < 0 || quality > 100) {
            quality = 100;
        }
        if (qualitySlider.isEnabled()) {
            qualitySlider.setValue(quality);
        }

        boolean progress = s.getYesNo(key(format, "progressive"));
        if (progressiveCheck.isEnabled()) {
            progressiveCheck.setSelected(progress);
        }
    }

    public void saveDefaults(String format) {
        Settings s = Settings.getUser();

        s.set(key(format, "compress"), compressCheck.isSelected() ? "yes" : "no");

        Object type = typeCombo.getSelectedItem();
        if (type == null) {
            s.reset(key(format, "type"));
        } else {
            s.set(key(format, "type"), type.toString());
        }

        s.set(key(format, "quality"), String.valueOf(qualitySlider.getValue()));

        s.set(key(format, "progressive"), progressiveCheck.isSelected() ? "yes" : "no");
    }

    private String key(String format, String type) {
        return "image-compression-" + format + "-" + type;
    }
}
