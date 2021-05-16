package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.Length;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.imageio.SimpleImageWriter;
import ca.cgjennings.imageio.WritableImageFormat;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.ui.DocumentEventAdapter;
import ca.cgjennings.ui.FileNameExtensionFilter;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.dnd.ScrapBook;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import static resources.Language.string;
import resources.RawSettings;
import resources.Settings;

/**
 * A simple image viewer dialog used by the project system and the
 * <a href='scriptdoc:imageutils'>imageutils</a> script library.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ImageViewer extends javax.swing.JDialog {

    private NumberFormat intFormat;
    private NumberFormat floatFormat;

    private double dpi = 150;
    private static final String DPI_KEY = "image-viewer-dpi";

    /**
     * Creates a new image viewer for the specified image.
     *
     * @param parent the dialog's parent window
     * @param image the image to show in the viewer
     * @param modal whether the dialog should block until closed
     */
    public ImageViewer(java.awt.Frame parent, BufferedImage image, boolean modal) {
        this(parent, image, null, modal);
    }

    /**
     * Creates a new image viewer for the specified image.
     *
     * @param parent the dialog's parent window
     * @param image the image to show in the viewer
     * @param modal whether the dialog should block until closed
     */
    public ImageViewer(java.awt.Window parent, BufferedImage image, boolean modal) {
        this(parent, image, null, modal);
    }

    /**
     * Creates a new image viewer for the specified image.
     *
     * @param parent the dialog's parent window
     * @param image the image to show in the viewer
     * @param original the source file of the image, or {@code null}
     * @param modal whether the dialog should block until closed
     */
    public ImageViewer(java.awt.Window parent, BufferedImage image, File original, boolean modal) {
        super(parent == null ? StrangeEons.getWindow() : parent, string("iv-title"), modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        initComponents();
        AbstractGameComponentEditor.localizeComboBoxLabels(unitCombo, null);
        intFormat = NumberFormat.getIntegerInstance();
        floatFormat = NumberFormat.getNumberInstance();
        floatFormat.setMinimumFractionDigits(0);
        floatFormat.setMaximumFractionDigits(2);
        getRootPane().setDefaultButton(closeBtn);
        this.original = original;
        editBtn.setVisible(original != null);

        Settings s = Settings.getShared();
        if (s.get(DPI_KEY) != null) {
            dpi = s.getDouble(DPI_KEY);
            if (dpi != dpi) {
                dpi = 150;
            }
        }

        setImage(image);

        unitCombo.setSelectedIndex(Length.getDefaultUnit());
        dpiField.getDocument().addDocumentListener(new DocumentEventAdapter() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                EventQueue.invokeLater(ImageViewer.this::updatePhysicalSize);
            }
        });
        dpiField.setText(floatFormat.format(dpi));

        pack();
    }

    public void setImage(BufferedImage image) {
        String t;
        if (image == null) {
            t = "0 \u00d7 0";
        } else {
            t = intFormat.format(image.getWidth()) + " \u00d7 " + intFormat.format(image.getHeight()) + " ";
            if (image.getTransparency() != BufferedImage.OPAQUE) {
                t += "A";
            }
            t += "RGB";
        }
        dimensionLabel.setText(t);
        imageViewer.setImage(image);
        updatePhysicalSize();
    }

    private void updatePhysicalSize() {
        BufferedImage image = imageViewer.getImage();

        String t;
        if (image == null) {
            t = "0 \u00d7 0";
        } else {
            double w = image.getWidth();
            double h = image.getHeight();

            try {
                dpi = floatFormat.parse(dpiField.getText()).floatValue();
                Settings.getUser().set(DPI_KEY, String.valueOf(dpi));
            } catch (ParseException ex) {
                dpiField.setText(floatFormat.format(dpi));
            }

            if (dpi < 1) {
                dpi = 1;
                dpiField.setText(floatFormat.format(dpi));
            }

            // convert image size to inches
            w /= dpi;
            h /= dpi;

            int unit = unitCombo.getSelectedIndex();
            if (unit == Length.PT) {
                w = Length.convert(w, Length.IN, Length.PT);
                h = Length.convert(h, Length.IN, Length.PT);
            } else if (unit == Length.CM) {
                w = Length.convert(w, Length.IN, Length.CM);
                h = Length.convert(h, Length.IN, Length.CM);
            }

            t = floatFormat.format(w) + " \u00d7 " + floatFormat.format(h);
        }
        physDimensionLabel.setText(t);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        closeBtn = new javax.swing.JButton();
        dimensionLabel = new javax.swing.JLabel();
        saveBtn = new javax.swing.JButton();
        imageViewer = new ca.cgjennings.apps.arkham.ImageViewer();
        editBtn = new javax.swing.JButton();
        physDimensionLabel = new javax.swing.JLabel();
        unitCombo = new javax.swing.JComboBox();
        atLabel = new javax.swing.JLabel();
        dpiField = new javax.swing.JTextField();
        dpiLabel = new javax.swing.JLabel();
        copyBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("resources/text/interface/eons-text"); // NOI18N
        setTitle(bundle.getString("iv-title")); // NOI18N

        closeBtn.setText(bundle.getString("ab-close")); // NOI18N
        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeBtnActionPerformed(evt);
            }
        });

        dimensionLabel.setFont(dimensionLabel.getFont().deriveFont(dimensionLabel.getFont().getSize()-2f));
        dimensionLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        dimensionLabel.setText("          ");

        saveBtn.setText(bundle.getString("iv-b-save")); // NOI18N
        saveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBtnActionPerformed(evt);
            }
        });

        imageViewer.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 1, 0, new java.awt.Color(128, 128, 128)));

        javax.swing.GroupLayout imageViewerLayout = new javax.swing.GroupLayout(imageViewer);
        imageViewer.setLayout(imageViewerLayout);
        imageViewerLayout.setHorizontalGroup(
            imageViewerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 452, Short.MAX_VALUE)
        );
        imageViewerLayout.setVerticalGroup(
            imageViewerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 328, Short.MAX_VALUE)
        );

        editBtn.setText(string( "edit-misc" )); // NOI18N
        editBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editBtnActionPerformed(evt);
            }
        });

        physDimensionLabel.setFont(physDimensionLabel.getFont().deriveFont(physDimensionLabel.getFont().getSize()-2f));
        physDimensionLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        physDimensionLabel.setText("          ");

        unitCombo.setFont(unitCombo.getFont().deriveFont(unitCombo.getFont().getSize()-2f));
        unitCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "iid-cb-unit0", "iid-cb-unit1", "iid-cb-unit2" }));
        unitCombo.setBorder(null);
        unitCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitComboActionPerformed(evt);
            }
        });

        atLabel.setFont(atLabel.getFont().deriveFont(atLabel.getFont().getSize()-2f));
        atLabel.setText("@");

        dpiField.setColumns(4);
        dpiField.setFont(dpiField.getFont().deriveFont(dpiField.getFont().getSize()-2f));
        dpiField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        dpiField.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.gray));

        dpiLabel.setDisplayedMnemonic('d');
        dpiLabel.setFont(dpiLabel.getFont().deriveFont(dpiLabel.getFont().getSize()-2f));
        dpiLabel.setLabelFor(dpiField);
        dpiLabel.setText(string( "dpi" )); // NOI18N

        copyBtn.setText(string("copy")); // NOI18N
        copyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(imageViewer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(copyBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 169, Short.MAX_VALUE)
                        .addComponent(editBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(closeBtn))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(dimensionLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(physDimensionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(atLabel)
                        .addGap(1, 1, 1)
                        .addComponent(dpiField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dpiLabel)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {closeBtn, editBtn});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dimensionLabel)
                    .addComponent(physDimensionLabel)
                    .addComponent(unitCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(atLabel)
                    .addComponent(dpiField, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dpiLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imageViewer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(closeBtn)
                    .addComponent(editBtn)
                    .addComponent(copyBtn)
                    .addComponent(saveBtn))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void saveBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBtnActionPerformed
    JFileChooser fc = getFileChooser();
    if (fc.showSaveDialog(saveBtn) == JFileChooser.APPROVE_OPTION) {
        String fileName = fc.getSelectedFile().getName();
        String type = "png";
        int i = fileName.lastIndexOf('.');
        if (i >= 0) {
            type = fileName.substring(i + 1);
        }

        WritableImageFormat found = null;
        for (WritableImageFormat wif : SimpleImageWriter.getImageFormats()) {
            if (wif.getExtension().equals(type)) {
                found = wif;
                break;
            }
        }

        SimpleImageWriter iw = null;
        try {
            iw = found.createImageWriter();
            iw.setEncodingHint("file-source", true);
            iw.setCompressionQuality(0.75f);
            iw.setPixelsPerInch((float) dpi);
            iw.write(imageViewer.getImage(), fc.getSelectedFile());
        } catch (IOException e) {
            ErrorDialog.displayError(string("rk-err-export"), e);
        } finally {
            if (iw != null) {
                iw.dispose();
            }
        }
    }
}//GEN-LAST:event_saveBtnActionPerformed

private void closeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeBtnActionPerformed
    dispose();
}//GEN-LAST:event_closeBtnActionPerformed

private void editBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editBtnActionPerformed
    try {
        DesktopIntegration.edit(original, this);
        dispose();
    } catch (IOException e) {
        ErrorDialog.displayError(string("app-err-open", original.getName()), e);
    }
}//GEN-LAST:event_editBtnActionPerformed

private void unitComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitComboActionPerformed
    int sel = unitCombo.getSelectedIndex();
    if (sel < 0) {
        return;
    }
    updatePhysicalSize();
    Length.setDefaultUnit(sel);
}//GEN-LAST:event_unitComboActionPerformed

	private void copyBtnActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_copyBtnActionPerformed
            ScrapBook.setImage(imageViewer.getImage());
	}//GEN-LAST:event_copyBtnActionPerformed

    private File original;

    /**
     * Returns a file chooser suited to saving a copy of an image file.
     *
     * @return a file chooser that allows the selection of a JPG or PNG save
     * file
     */
    public static JFileChooser getFileChooser() {
        JUtilities.threadAssert();
        JFileChooser fc = chooser.get();
        if (fc == null) {
            fc = createFileChooser();
        }
        return fc;
    }

    private static JFileChooser createFileChooser() {
        String folder = RawSettings.getUserSetting("default-image-folder");
        JFileChooser fc = new JFileChooser(folder);
        for (WritableImageFormat wif : SimpleImageWriter.getImageFormats()) {
            fc.addChoosableFileFilter(
                    new FileNameExtensionFilter(wif.getName(), wif.getExtension())
            );
        }
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser = new SoftReference<>(fc);
        return fc;
    }
    private static SoftReference<JFileChooser> chooser = new SoftReference<>(null);

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel atLabel;
    private javax.swing.JButton closeBtn;
    private javax.swing.JButton copyBtn;
    private javax.swing.JLabel dimensionLabel;
    private javax.swing.JTextField dpiField;
    private javax.swing.JLabel dpiLabel;
    private javax.swing.JButton editBtn;
    private ca.cgjennings.apps.arkham.ImageViewer imageViewer;
    private javax.swing.JLabel physDimensionLabel;
    private javax.swing.JButton saveBtn;
    private javax.swing.JComboBox unitCombo;
    // End of variables declaration//GEN-END:variables
}
