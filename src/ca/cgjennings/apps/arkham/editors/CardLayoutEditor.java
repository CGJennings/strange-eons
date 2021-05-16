package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.AbstractSupportEditor;
import ca.cgjennings.apps.arkham.MarkupTargetFactory;
import ca.cgjennings.apps.arkham.RegionPicker;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.commands.AbstractCommand;
import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.MetadataSource;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.apps.arkham.project.Rename;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.imageio.ImageLayer;
import ca.cgjennings.imageio.PSDImageReader;
import ca.cgjennings.io.EscapedTextCodec;
import ca.cgjennings.layout.PageShape.CupShape;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.util.SortedProperties;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;
import resources.StrangeImage;

/**
 * A support editor for the construction of cards as an ordered list of layers.
 * The layer arrangement can be exported to code suitable for use with a DIY
 * component. The editor creates lightweight save files that can be included
 * with a plug-in without significantly affecting size.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class CardLayoutEditor extends AbstractSupportEditor implements RegionPicker.RegionChangeListener, Rename.RenameListener {

    private RegionPicker.RegionChooser regionEd;
    private int activeIndex = -1;
    private LayerListProperties listProps = new LayerListProperties();

    /**
     * Creates a new card layout editor for the specified layout file. As they
     * are part of a project, a card layout is always tied to a file (i.e., it
     * never has a  {@code null} file value).
     *
     * @param f the file to edit
     */
    public CardLayoutEditor(File f) throws IOException {
        initComponents();
        AbstractGameComponentEditor.localizeComboBoxLabels(shapeCombo, null);
        MarkupTargetFactory.enableTargeting(nameField, false);
        MarkupTargetFactory.enableTargeting(keyField, false);
        MarkupTargetFactory.enableTargeting(this, false);
        JUtilities.setIconPair(downBtn, "ui/button/down.png", "ui/button/down-hi.png", false);
        JUtilities.setIconPair(upBtn, "ui/button/up.png", "ui/button/up-hi.png", false);

        setFile(f);
        updateSelectableImageList();
        int updatePeriod = Settings.getShared().getInt("file-monitoring-period");
        if (updatePeriod < 1) {
            updatePeriod = 5_000;
        }
        Timer listUpdater = new Timer(updatePeriod, (ActionEvent e) -> {
            updateSelectableImageList();
        });
        listUpdater.start();

        regionEd = new RegionPicker.RegionChooser();
        edScroll.setViewportView(regionEd);

        // create list with one layer; must happen after creating internal image list
        clearImpl();
        layerList.setSelectedIndex(0);

        regionEd.addMouseWheelListener((MouseWheelEvent e) -> {
            int dz = e.getWheelRotation();
            int zoom = regionEd.getZoom() - dz;
            if (zoom < 1) {
                zoom = 1;
            }
            if (zoom > 32) {
                zoom = 32;
            }
            regionEd.setZoom(zoom);
            //updateMousePos();
            e.consume();
        });
        regionEd.setRestrictedRegion(false);
        regionEd.addRegionChangeListener(this);
        regionEd.setClickToDefineEnabled(false);

        MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                didADrag = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    didADrag = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (didADrag || (e.getButton() != MouseEvent.BUTTON1)) {
                    didADrag = false;
                    return;
                }
                Point p = e.getPoint();
                regionEd.viewToModel(p, p);
                // if not inside the current region, only select a new region
                //     if the mouse button is released without any dragging
//				if( regionEd.getLocationType( p ) != RegionChooser.LOC_OUTSIDE ) return;
                for (int i = 0; i < layers.size(); ++i) {
                    Layer layer = get(i);
                    if (layer.isShowing() && layer.region.contains(p)) {
                        // if the layer has an image, test against the image shape
                        if (layer.getImage() != null) {
                            int x = p.x - layer.getX();
                            int y = p.y - layer.getY();

                            BufferedImage bi = layer.getImage();

                            // if image is scaled, project to original coordinate system
                            if (layer.getWidth() != bi.getWidth()) {
                                float scale = bi.getWidth() / (float) layer.getWidth();
                                x = Math.round(x * scale);
                            }
                            if (layer.getHeight() != bi.getHeight()) {
                                float scale = bi.getHeight() / (float) layer.getHeight();
                                y = Math.round(y * scale);
                            }

                            if (x < 0 || y < 0 || x >= bi.getWidth() || y >= bi.getHeight()) {
                                continue;
                            }
                            int alpha = bi.getRGB(x, y) >>> 24;
                            // ignore if alpha of this point in the image is very low
                            if (alpha < 16) {
                                continue;
                            }
                        }
                        layerList.setSelectedIndex(i);
                        break;
                    }
                }
            }
            private boolean didADrag;
        };
        regionEd.addMouseListener(mouseListener);
        regionEd.addMouseMotionListener(mouseListener);

//		DocumentListener changeListener = new DocumentListener() {
//			@Override
//			public void insertUpdate( DocumentEvent e ) {
//				setUnsavedChanges( true );
//			}
//			@Override
//			public void removeUpdate( DocumentEvent e ) {
//				setUnsavedChanges( true );
//			}
//			@Override
//			public void changedUpdate( DocumentEvent e ) {
//				setUnsavedChanges( true );
//			}
//		};
//		nameField.getDocument().addDocumentListener( changeListener );
//		keyField.getDocument().addDocumentListener( changeListener );
        setFrameIcon(MetadataSource.ICON_CARD_LAYOUT);

        InputMap imap = layerList.getInputMap();
        ActionMap amap = layerList.getActionMap();

        Action deleteAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (layers.size() > 1) {
                    deleteBtnActionPerformed(null);
                }
            }
        };
        amap.put("delete", deleteAction);
        imap.put(KeyStroke.getKeyStroke("DELETE"), "delete");

        imap = regionEd.getInputMap();
        amap = regionEd.getActionMap();
        amap.put("delete", deleteAction);
        imap.put(KeyStroke.getKeyStroke("DELETE"), "delete");

        Rename.addRenameListener(this);

        load(f);

        pack();
//		listSplitter.setDividerLocation(-1);

        EventQueue.invokeLater(ErrorDialog::nyi);

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        listSplitter = new javax.swing.JSplitPane();
        editPanel = new javax.swing.JPanel();
        javax.swing.JLabel layerPropTitle = new javax.swing.JLabel();
        layerStaticPropPanel = new javax.swing.JPanel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        wField = new javax.swing.JSpinner();
        showLayerCheck = new javax.swing.JCheckBox();
        regionLabel = new javax.swing.JLabel();
        yField = new javax.swing.JSpinner();
        nameField = new javax.swing.JTextField();
        hField = new javax.swing.JSpinner();
        exportCheck = new javax.swing.JCheckBox();
        resizeCheck = new javax.swing.JCheckBox();
        xField = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        widthLabel = new javax.swing.JLabel();
        heightLabel = new javax.swing.JLabel();
        prefixField = new javax.swing.JTextField();
        javax.swing.JLabel prefixLabel = new javax.swing.JLabel();
        layerDynPropPanel = new javax.swing.JPanel();
        imagePanel = new javax.swing.JPanel();
        imageResCombo = new javax.swing.JComboBox();
        resLabel = new javax.swing.JLabel();
        textPropPanel = new javax.swing.JPanel();
        cupX1Field = new javax.swing.JSpinner();
        dx1Label = new javax.swing.JLabel();
        cupYField = new javax.swing.JSpinner();
        shapeCombo = new javax.swing.JComboBox();
        shapeLabel = new javax.swing.JLabel();
        dx2Label = new javax.swing.JLabel();
        ySplitLabel = new javax.swing.JLabel();
        cupX2Field = new javax.swing.JSpinner();
        addPanel = new javax.swing.JPanel();
        javax.swing.JLabel addLayerTitle = new javax.swing.JLabel();
        importPSDBtn = new javax.swing.JButton();
        addPortrait = new javax.swing.JButton();
        addTextBtn = new javax.swing.JButton();
        addImageBtn = new javax.swing.JButton();
        addExpSymBtn = new javax.swing.JButton();
        layerListPanel = new javax.swing.JPanel();
        downBtn = new javax.swing.JButton();
        upBtn = new javax.swing.JButton();
        deleteBtn = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        layerList = new javax.swing.JList();
        javax.swing.JLabel layerListTitle = new javax.swing.JLabel();
        cardPropPanel = new javax.swing.JPanel();
        javax.swing.JLabel cardPropTitle = new javax.swing.JLabel();
        keyLabel = new javax.swing.JLabel();
        keyField = new javax.swing.JTextField();
        edScroll = new javax.swing.JScrollPane();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        listSplitter.setDividerSize(8);
        listSplitter.setOneTouchExpandable(true);

        layerPropTitle.setFont(layerPropTitle.getFont().deriveFont(layerPropTitle.getFont().getStyle() | java.awt.Font.BOLD, layerPropTitle.getFont().getSize()+1));
        layerPropTitle.setText(string( "cle-props" )); // NOI18N
        layerPropTitle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        jLabel4.setLabelFor(nameField);
        jLabel4.setText(string( "cle-name" )); // NOI18N

        wField.setFont(wField.getFont().deriveFont(wField.getFont().getSize()-1f));
        wField.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        wField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        showLayerCheck.setText(string( "cle-hidden" )); // NOI18N
        showLayerCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showLayerCheckActionPerformed(evt);
            }
        });

        regionLabel.setText(string( "cle-region" )); // NOI18N

        yField.setFont(yField.getFont().deriveFont(yField.getFont().getSize()-1f));
        yField.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        yField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        nameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameFieldActionPerformed(evt);
            }
        });
        nameField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                nameFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                nameFieldFocusLost(evt);
            }
        });

        hField.setFont(hField.getFont().deriveFont(hField.getFont().getSize()-1f));
        hField.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        hField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        exportCheck.setText(string( "cle-include" )); // NOI18N
        exportCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportCheckActionPerformed(evt);
            }
        });

        resizeCheck.setText(string( "cle-allow-resize" )); // NOI18N
        resizeCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resizeCheckActionPerformed(evt);
            }
        });

        xField.setFont(xField.getFont().deriveFont(xField.getFont().getSize()-1f));
        xField.setModel(new javax.swing.SpinnerNumberModel(0, -9999, 9999, 1));
        xField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getSize()-1f));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("x");

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getSize()-1f));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("y");

        widthLabel.setFont(widthLabel.getFont().deriveFont(widthLabel.getFont().getSize()-1f));
        widthLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        widthLabel.setText(string( "de-card-width" )); // NOI18N

        heightLabel.setFont(heightLabel.getFont().deriveFont(heightLabel.getFont().getSize()-1f));
        heightLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        heightLabel.setText(string( "de-card-height" )); // NOI18N

        prefixField.setColumns(8);
        prefixField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prefixFieldActionPerformed(evt);
            }
        });
        prefixField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prefixFieldFocusLost(evt);
            }
        });

        prefixLabel.setLabelFor(prefixField);
        prefixLabel.setText(string("cle-prefix")); // NOI18N

        javax.swing.GroupLayout layerStaticPropPanelLayout = new javax.swing.GroupLayout(layerStaticPropPanel);
        layerStaticPropPanel.setLayout(layerStaticPropPanelLayout);
        layerStaticPropPanelLayout.setHorizontalGroup(
            layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layerStaticPropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(regionLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layerStaticPropPanelLayout.createSequentialGroup()
                        .addComponent(resizeCheck)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layerStaticPropPanelLayout.createSequentialGroup()
                        .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layerStaticPropPanelLayout.createSequentialGroup()
                                .addComponent(showLayerCheck)
                                .addGap(18, 18, 18)
                                .addComponent(exportCheck))
                            .addGroup(layerStaticPropPanelLayout.createSequentialGroup()
                                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(xField, javax.swing.GroupLayout.Alignment.LEADING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(yField))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(widthLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(wField))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(heightLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(hField)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layerStaticPropPanelLayout.createSequentialGroup()
                                .addComponent(nameField)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(prefixLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(prefixField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())))
        );
        layerStaticPropPanelLayout.setVerticalGroup(
            layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layerStaticPropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(showLayerCheck)
                    .addComponent(exportCheck))
                .addGap(8, 8, 8)
                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prefixField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prefixLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(regionLabel)
                    .addComponent(xField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(yField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(wField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(1, 1, 1)
                .addGroup(layerStaticPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel8)
                    .addComponent(widthLabel)
                    .addComponent(heightLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resizeCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layerDynPropPanel.setLayout(new java.awt.CardLayout());

        imageResCombo.setEditable(true);
        imageResCombo.setMaximumRowCount(12);
        imageResCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageResComboActionPerformed(evt);
            }
        });
        imageResCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                imageResComboFocusLost(evt);
            }
        });

        resLabel.setLabelFor(imageResCombo);
        resLabel.setText(string( "cle-image" )); // NOI18N

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(resLabel)
                .addGap(18, 18, 18)
                .addComponent(imageResCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 352, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        imagePanelLayout.setVerticalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(imagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resLabel)
                    .addComponent(imageResCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        layerDynPropPanel.add(imagePanel, "image");

        cupX1Field.setModel(new javax.swing.SpinnerNumberModel(0.0d, -999.0d, 999.0d, 1.0d));
        cupX1Field.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                shapeFieldChanged(evt);
            }
        });

        dx1Label.setFont(dx1Label.getFont().deriveFont(dx1Label.getFont().getSize()-1f));
        dx1Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        dx1Label.setText("dx1");

        cupYField.setModel(new javax.swing.SpinnerNumberModel(0.0d, -9999.0d, 9999.0d, 1.0d));
        cupYField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                shapeFieldChanged(evt);
            }
        });

        shapeCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "cle-shape0", "cle-shape1", "cle-shape2" }));
        shapeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shapeComboActionPerformed(evt);
            }
        });

        shapeLabel.setText(string( "cle-shape" )); // NOI18N

        dx2Label.setFont(dx2Label.getFont().deriveFont(dx2Label.getFont().getSize()-1f));
        dx2Label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        dx2Label.setText("dx2");

        ySplitLabel.setFont(ySplitLabel.getFont().deriveFont(ySplitLabel.getFont().getSize()-1f));
        ySplitLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ySplitLabel.setText(string( "cle-shape-y-split" )); // NOI18N

        cupX2Field.setModel(new javax.swing.SpinnerNumberModel(0.0d, -999.0d, 999.0d, 1.0d));
        cupX2Field.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                shapeFieldChanged(evt);
            }
        });

        javax.swing.GroupLayout textPropPanelLayout = new javax.swing.GroupLayout(textPropPanel);
        textPropPanel.setLayout(textPropPanelLayout);
        textPropPanelLayout.setHorizontalGroup(
            textPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(textPropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(shapeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(shapeCombo, 0, 176, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(textPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(dx1Label, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cupX1Field, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(textPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(dx2Label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cupX2Field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(textPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ySplitLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cupYField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        textPropPanelLayout.setVerticalGroup(
            textPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(textPropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(textPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(shapeLabel)
                    .addComponent(shapeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cupX1Field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cupX2Field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cupYField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(1, 1, 1)
                .addGroup(textPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dx1Label)
                    .addComponent(dx2Label)
                    .addComponent(ySplitLabel))
                .addContainerGap())
        );

        layerDynPropPanel.add(textPropPanel, "text");

        addLayerTitle.setFont(addLayerTitle.getFont().deriveFont(addLayerTitle.getFont().getStyle() | java.awt.Font.BOLD, addLayerTitle.getFont().getSize()+1));
        addLayerTitle.setText(string( "cle-add-layers" )); // NOI18N
        addLayerTitle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        importPSDBtn.setFont(importPSDBtn.getFont().deriveFont(importPSDBtn.getFont().getSize()-1f));
        importPSDBtn.setText(string( "cle-import" )); // NOI18N
        importPSDBtn.setToolTipText(string( "cle-import-tt" )); // NOI18N
        importPSDBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importPSDBtnActionPerformed(evt);
            }
        });

        addPortrait.setFont(addPortrait.getFont().deriveFont(addPortrait.getFont().getSize()-1f));
        addPortrait.setIcon( ICON_PORTRAIT );
        addPortrait.setText(string( "cle-add-portrait" )); // NOI18N
        addPortrait.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        addPortrait.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPortraitActionPerformed(evt);
            }
        });

        addTextBtn.setFont(addTextBtn.getFont().deriveFont(addTextBtn.getFont().getSize()-1f));
        addTextBtn.setIcon( ICON_TEXT );
        addTextBtn.setText(string( "cle-add-text" )); // NOI18N
        addTextBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        addTextBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addTextBtnActionPerformed(evt);
            }
        });

        addImageBtn.setFont(addImageBtn.getFont().deriveFont(addImageBtn.getFont().getSize()-1f));
        addImageBtn.setIcon( ICON_IMAGE );
        addImageBtn.setText(string( "cle-add-image" )); // NOI18N
        addImageBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        addImageBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addImageBtnActionPerformed(evt);
            }
        });

        addExpSymBtn.setFont(addExpSymBtn.getFont().deriveFont(addExpSymBtn.getFont().getSize()-1f));
        addExpSymBtn.setIcon( ICON_EXPSYM );
        addExpSymBtn.setText(string( "cle-add-expsym" )); // NOI18N
        addExpSymBtn.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        addExpSymBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addExpSymBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout addPanelLayout = new javax.swing.GroupLayout(addPanel);
        addPanel.setLayout(addPanelLayout);
        addPanelLayout.setHorizontalGroup(
            addPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(addPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addLayerTitle)
                    .addComponent(addImageBtn)
                    .addComponent(addTextBtn)
                    .addComponent(addExpSymBtn)
                    .addComponent(addPortrait)
                    .addComponent(importPSDBtn))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        addPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addExpSymBtn, addImageBtn, addLayerTitle, addPortrait, addTextBtn});

        addPanelLayout.setVerticalGroup(
            addPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addLayerTitle)
                .addGap(7, 7, 7)
                .addComponent(addImageBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addPortrait)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addTextBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addExpSymBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(importPSDBtn)
                .addContainerGap())
        );

        downBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downBtnActionPerformed(evt);
            }
        });

        upBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upBtnActionPerformed(evt);
            }
        });

        deleteBtn.setText(string( "delete" )); // NOI18N
        deleteBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteBtnActionPerformed(evt);
            }
        });

        layerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        layerList.setCellRenderer( new LayerCellRenderer() );
        layerList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                layerListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(layerList);

        layerListTitle.setFont(layerListTitle.getFont().deriveFont(layerListTitle.getFont().getStyle() | java.awt.Font.BOLD, layerListTitle.getFont().getSize()+1));
        layerListTitle.setText(string( "cle-layer-list" )); // NOI18N
        layerListTitle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));

        javax.swing.GroupLayout layerListPanelLayout = new javax.swing.GroupLayout(layerListPanel);
        layerListPanel.setLayout(layerListPanelLayout);
        layerListPanelLayout.setHorizontalGroup(
            layerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layerListPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(layerListTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layerListPanelLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1)
                            .addGroup(layerListPanelLayout.createSequentialGroup()
                                .addComponent(upBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(downBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(deleteBtn)))))
                .addContainerGap())
        );
        layerListPanelLayout.setVerticalGroup(
            layerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layerListPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(layerListTitle)
                .addGap(7, 7, 7)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layerListPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(upBtn, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(downBtn, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(deleteBtn, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        layerListPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {deleteBtn, downBtn, upBtn});

        cardPropTitle.setFont(cardPropTitle.getFont().deriveFont(cardPropTitle.getFont().getStyle() | java.awt.Font.BOLD, cardPropTitle.getFont().getSize()+1));
        cardPropTitle.setText(string("cle-key-title")); // NOI18N
        cardPropTitle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(128, 128, 128)));

        keyLabel.setLabelFor(keyField);
        keyLabel.setText(string( "cle-key" )); // NOI18N

        keyField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyFieldActionPerformed(evt);
            }
        });
        keyField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                keyFieldFocusLost(evt);
            }
        });

        javax.swing.GroupLayout cardPropPanelLayout = new javax.swing.GroupLayout(cardPropPanel);
        cardPropPanel.setLayout(cardPropPanelLayout);
        cardPropPanelLayout.setHorizontalGroup(
            cardPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cardPropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(cardPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(cardPropPanelLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(keyLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(keyField))
                    .addComponent(cardPropTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        cardPropPanelLayout.setVerticalGroup(
            cardPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cardPropPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cardPropTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(cardPropPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keyLabel)
                    .addComponent(keyField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout editPanelLayout = new javax.swing.GroupLayout(editPanel);
        editPanel.setLayout(editPanelLayout);
        editPanelLayout.setHorizontalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editPanelLayout.createSequentialGroup()
                .addGroup(editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(editPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(layerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(addPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(editPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(layerStaticPropPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(layerDynPropPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(layerPropTitle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(editPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(cardPropPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        editPanelLayout.setVerticalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cardPropPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(layerListPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(layerPropTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(layerStaticPropPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(layerDynPropPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        listSplitter.setLeftComponent(editPanel);

        edScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        listSplitter.setRightComponent(edScroll);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(listSplitter, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void setFile(File f) {
        super.setFile(f);
        updateSelectableImageList();
    }

    /**
     * Rename listener implementation. Checks for renaming of images files that
     * are in use by a layer and updates the layer resource field automatically.
     */
    @Override
    public void fileRenamed(Project p, Member m, File oldFile, File newFile) {
        InternalImage[] oldIntImages = internalImages;
        updateSelectableImageList();
        for (int i = 0; i < oldIntImages.length; ++i) {
            if (!oldFile.equals(oldIntImages[i].file)) {
                continue;
            }
            // are any layers using this file?
            for (int j = 0; j < layers.size(); ++j) {
                Layer la = get(j);
                if (!oldIntImages[i].path.equals(la.imageResource)) {
                    continue;
                }
                // build a new matching path for the layer
                File pathComponent = newFile;
                String path = "";
                while (pathComponent != null) {
                    path = "/" + pathComponent.getName() + path;
                    pathComponent = pathComponent.getParentFile();
                    if (pathComponent != null) {
                        if (pathComponent.getName().equals("resources")) {
                            path = path.substring(1);
                            break;
                        } else if (Task.isTaskFolder(pathComponent)) {
                            break;
                        }
                    }
                }
                if (activeIndex == j) {
                    imageResCombo.setSelectedItem(path);
                } else {
                    la.setImageResource(path);
                }
            }
        }
    }

    static class InternalImage implements Comparable<InternalImage> {

        public String path;
        public File file;

        InternalImage(String path, File file) {
            this.path = path;
            this.file = file;
        }

        @Override
        public String toString() {
            return path;
        }

        @Override
        public int compareTo(InternalImage rhs) {
            return Language.getInterface().getCollator().compare(path, rhs.path);
        }
    }

    private InternalImage[] createImageList(boolean foldersOnly) {
        File f = getFile();
        if (f == null) {
            return new InternalImage[0];
        }

        File task = f.getParentFile();
        while (task != null && !Task.isTaskFolder(task)) {
            task = task.getParentFile();
        }

        // the list is not located in a task; use the list's parent folder
        if (task == null) {
            task = f.getParentFile();
            if (task == null) {
                return new InternalImage[0];
            }
        }

        File resourceFolder = new File(task, "resources");
        if (!resourceFolder.exists()) {
            resourceFolder.mkdirs();
        }

        ArrayList<InternalImage> images = new ArrayList<>();
        createImageListImpl(images, resourceFolder, "", foldersOnly);

        InternalImage[] list = images.toArray(new InternalImage[images.size()]);
        java.util.Arrays.sort(list);
        return list;
    }

    private void createImageListImpl(List<InternalImage> list, File parent, String path, boolean foldersOnly) {
        File[] children = parent.listFiles();
        if (children == null) {
            return;
        }

        // if listing folders, add the parent to the list now: ensures that
        // the resources folder iteself is included
        if (foldersOnly) {
            list.add(new InternalImage(path, parent));
        }

        for (File f : children) {
            if (f.isDirectory()) {
                createImageListImpl(list, f, path + (path.isEmpty() ? "" : "/") + f.getName(), foldersOnly);
            } else if (!foldersOnly && ProjectUtilities.matchExtension(f, IMAGE_EXTENSIONS)) {
                list.add(new InternalImage(path + (path.isEmpty() ? "" : "/") + f.getName(), f));
            }
        }
    }
    private static final String[] IMAGE_EXTENSIONS = new String[]{
        "jpg", "png", "jp2", "jpeg"
    };

    private InternalImage[] internalImages;

    private void updateSelectableImageList() {
        // Otherwise, the list will be replaced out from under the
        // user while they are working, closing the popup.
        if (imageResCombo.isPopupVisible()) {
            return;
        }

        InternalImage[] images = createImageList(false);

        boolean replace = false;
        if (internalImages == null) {
            replace = true;
        } else {
            if (images.length != internalImages.length) {
                replace = true;
            } else {
                for (int i = 0; i < images.length; ++i) {
                    if (!images[i].equals(internalImages[i])) {
                        replace = true;
                        break;
                    }
                }
            }
        }
        if (replace) {
            internalImages = images;

            Object sel = imageResCombo.getSelectedItem();
            imageResCombo.setModel(new DefaultComboBoxModel(images));
            imageResCombo.setSelectedItem(sel);
        }
    }

	private void layerListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_layerListValueChanged
            if (evt.getValueIsAdjusting()) {
                return;
            }

            int selection = layerList.getSelectedIndex();

            if (selection < 0) {
                if (layerList.getModel().getSize() > 0) {
                    layerList.setSelectedIndex(0);
                }
                return;
            }

            EventQueue.invokeLater(() -> {
                activeIndex = layerList.getSelectedIndex();
                listChanged();
            });
	}//GEN-LAST:event_layerListValueChanged

	private void upBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upBtnActionPerformed
            int sel = activeIndex;
            if (sel < 1) {
                return;
            }
            Layer layer = get(sel);
            layers.remove(sel);
            layers.add(sel - 1, layer);
            layerList.setSelectedIndex(sel - 1);
            listChanged();
            setUnsavedChanges(true);
	}//GEN-LAST:event_upBtnActionPerformed

	private void downBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downBtnActionPerformed
            int sel = activeIndex;
            if (sel < 0 || sel >= layers.size() - 1) {
                return;
            }
            Layer layer = get(sel);
            layers.add(sel + 2, layer);
            layers.remove(sel);
            layerList.setSelectedIndex(sel + 1);
            listChanged();
            setUnsavedChanges(true);
	}//GEN-LAST:event_downBtnActionPerformed

	private void deleteBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteBtnActionPerformed
            int sel = activeIndex;
            if (sel < 0) {
                return;
            }
            layers.remove(sel);
            if (sel < layers.size()) {
                layerList.setSelectedIndex(sel);
            }
            listChanged();
            setUnsavedChanges(true);
	}//GEN-LAST:event_deleteBtnActionPerformed

	private void nameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nameFieldActionPerformed
            int sel = activeIndex;
            if (sel < 0) {
                return;
            }
            get(sel).setName(nameField.getText());
            listChanged();
	}//GEN-LAST:event_nameFieldActionPerformed

	private void addTextBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addTextBtnActionPerformed
            Layer layer = new Layer();
            layer.imageResource = "";
            layer.setType(LayerType.TEXT);
            layer.setResizable(true);
            layer.image = null;
            layer.setRegion(8, 8, 90, 30);

            addLayerImpl(layer, -1);
	}//GEN-LAST:event_addTextBtnActionPerformed

	private void importPSDBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importPSDBtnActionPerformed
            CLEImportLayersDialog d = new CLEImportLayersDialog(StrangeEons.getWindow(), createImageList(true));
            d.setLocationRelativeTo(importPSDBtn);
            File f = d.showDialog();
            if (f == null) {
                return;
            }

            try {
                PSDImageReader r = new PSDImageReader(f);
                String baseLayerName = f.getName().trim();
                if (ProjectUtilities.matchExtension(f, "psd")) {
                    baseLayerName = baseLayerName.substring(0, baseLayerName.length() - 4);
                }
                String baseKeyName = baseLayerName.replace(' ', '-').toLowerCase(Locale.CANADA);

                if (d.isReplaceSelected()) {
                    layers.clear();
                }
                InternalImage dest = d.getDestination();

                for (int i = 0; i < r.getLayerCount(); ++i) {
                    ImageLayer il = r.getLayer(i);
                    BufferedImage image = il.getAlpha() >= 1f ? il.getImage() : il.createStyledImage();

                    String name = baseLayerName + " " + (i + 1) + ".png";
                    String path = dest.path + (dest.path.isEmpty() ? "" : "/") + name;
                    ImageIO.write(image, "png", new File(dest.file, name));

                    Layer layer = new Layer();
                    layer.setName(baseLayerName + " " + (i + 1));
                    layer.imageResource = path;
                    layer.image = image;
                    layer.setRegion(il.getRectangle());
                    layer.setShowing(il.isVisible());
                    layers.add(0, layer);
                }
                layerList.setSelectedIndex(0);
                updateSelectableImageList();
                listChanged();
            } catch (IOException e) {
                ErrorDialog.displayError(string("cle-psd-err-format"), e);
            } finally {
                // should never get PSD with 0 layers, but just in case
                // there is an exception we need to ensure that there is never
                // a 0-layer list
                if (layers.size() == 0) {
                    layers.addElement(new Layer());
                }
            }
	}//GEN-LAST:event_importPSDBtnActionPerformed

	private void showLayerCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showLayerCheckActionPerformed
            int sel = activeIndex;
            if (sel < 0) {
                return;
            }
            get(sel).setShowing(showLayerCheck.isSelected());
            updateEditorForLayer(sel);
            layerList.repaint(layerList.getCellBounds(sel, sel));
	}//GEN-LAST:event_showLayerCheckActionPerformed

	private void exportCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportCheckActionPerformed
            int sel = activeIndex;
            if (sel < 0) {
                return;
            }
            get(sel).setExported(exportCheck.isSelected());
	}//GEN-LAST:event_exportCheckActionPerformed

	private void resizeCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resizeCheckActionPerformed
            int sel = activeIndex;
            if (sel < 0) {
                return;
            }
            get(sel).setResizable(resizeCheck.isSelected());
            updateEditorForLayer(sel);
	}//GEN-LAST:event_resizeCheckActionPerformed

	private void addPortraitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPortraitActionPerformed
            Layer layer = new Layer();
            layer.imageResource = "";
            layer.setType(LayerType.PORTRAIT);
            layer.setResizable(true);
            layer.setName(string("cle-new-portrait"));

            CLEImageChooser d = new CLEImageChooser(StrangeEons.getWindow(), true, createImageList(false), createImageList(true));
            d.setLocationRelativeTo(addPortrait);
            String resource = d.showDialog();

            if (resource == null) {
                return;
            }

            // discover any newly copied image
            updateSelectableImageList();

            layer.setImageResource(resource);

            // set initial pos
            layer.setX(0);
            layer.setY(0);

            int templateLayer = getTemplateLayer();
            if (templateLayer >= 0) {
                Layer temp = get(templateLayer);
                layer.setX((temp.getWidth() - layer.getWidth()) / 2);
                layer.setY((temp.getHeight() - layer.getHeight()) / 2);
            }

            addLayerImpl(layer, layers.size());
	}//GEN-LAST:event_addPortraitActionPerformed

	private void regionFieldChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_regionFieldChanged
            if (regionUpdateInProgress || layerUpdateInProgress) {
                return;
            }
            regionUpdateInProgress = true;
            try {
                int sel = activeIndex;
                if (sel < 0) {
                    return;
                }
                Layer layer = get(sel);

                Rectangle r = layer.getRegion();
                r.x = ((Number) xField.getValue()).intValue();
                r.y = ((Number) yField.getValue()).intValue();
                r.width = ((Number) wField.getValue()).intValue();
                r.height = ((Number) hField.getValue()).intValue();

                layer.setRegion(r);
                regionEd.setRegion(r);
            } finally {
                regionUpdateInProgress = false;
            }
	}//GEN-LAST:event_regionFieldChanged
    private boolean regionUpdateInProgress;

	private void imageResComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_imageResComboActionPerformed
            if (layerUpdateInProgress) {
                return;
            }

            int sel = activeIndex;
            if (sel < 0) {
                return;
            }
            Layer layer = get(sel);

            if (imageResCombo.getSelectedItem() == null) {
                return;
            }
            String val = imageResCombo.getSelectedItem().toString();
            if (!val.equals(layer.imageResource)) {
                layer.setImageResource(val);
                updateEditorForLayer(sel);
            }
	}//GEN-LAST:event_imageResComboActionPerformed

	private void addImageBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addImageBtnActionPerformed

            CLEImageChooser d = new CLEImageChooser(StrangeEons.getWindow(), false, createImageList(false), createImageList(true));
            d.setLocationRelativeTo(addImageBtn);
            String resource = d.showDialog();

            if (resource == null) {
                return;
            }

            // discover any newly copied image
            updateSelectableImageList();

            Layer layer = new Layer();
            layer.imageResource = "";
            layer.setType(LayerType.IMAGE);
            layer.setImageResource(resource);
            boolean template = d.isTemplateModeSelected();

            // set initial pos
            int x = 0, y = 0;
            if (!template) {
                int templateLayer = getTemplateLayer();
                if (templateLayer >= 0) {
                    Layer temp = get(templateLayer);
                    x = (temp.getWidth() - layer.getWidth()) / 2;
                    y = (temp.getHeight() - layer.getHeight()) / 2;
                    x = Math.max(0, x);
                    y = Math.max(0, y);
                }
            }
            layer.setX(x);
            layer.setY(y);

            addLayerImpl(layer, template ? layers.size() + 1 : -1);
	}//GEN-LAST:event_addImageBtnActionPerformed

	private void addExpSymBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addExpSymBtnActionPerformed
            Layer layer = new Layer();
            layer.setType(LayerType.EXPSYM);
            layer.setImageResource("expansiontokens/XX.png");
            layer.setResizable(true);
            layer.setName(string("cle-new-expsym"));

            // set initial pos
            layer.setRegion(0, 0, 32, 32);

            int templateLayer = getTemplateLayer();
            if (templateLayer >= 0) {
                Layer temp = get(templateLayer);
                layer.setX((temp.getWidth() - layer.getWidth()) / 2);
                layer.setY((temp.getHeight() - layer.getHeight()) / 2);
            }

            addLayerImpl(layer, 0);
	}//GEN-LAST:event_addExpSymBtnActionPerformed

	private void shapeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_shapeComboActionPerformed
            int shape = shapeCombo.getSelectedIndex();
            if (shape < 0) {
                return;
            }

            int sel = activeIndex;
            if (sel < 0) {
                return;
            }

            Layer la = get(sel);

            boolean enable = shape != 0;
            cupX1Field.setEnabled(enable);
            cupX2Field.setEnabled(enable);
            cupYField.setEnabled(enable);

            if (shape == 0) {
                cupX1Field.setValue(0d);
                cupX2Field.setValue(0d);
                cupYField.setValue(0d);
            } else {
                if (la.getCupX1() == 0d && la.getCupX2() == 0d) {
                    cupX1Field.setValue(16d);
                    cupX2Field.setValue(16d);
                } else {
                    cupX1Field.setValue(la.getCupX1());
                    cupX2Field.setValue(la.getCupX2());
                }
                cupYField.setValue(la.getCupY());
                la.setCupAtTop(shape != 2);
            }

            listChanged();
	}//GEN-LAST:event_shapeComboActionPerformed

	private void shapeFieldChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_shapeFieldChanged
            if (shapeChangeInProgress || layerUpdateInProgress) {
                return;
            }
            shapeChangeInProgress = true;
            try {
                int sel = activeIndex;
                if (sel < 0) {
                    return;
                }

                Layer la = get(sel);

                la.setCupX1((Double) cupX1Field.getValue());
                la.setCupX2((Double) cupX2Field.getValue());
                la.setCupY((Double) cupYField.getValue());

                listChanged();
            } finally {
                shapeChangeInProgress = false;
            }
	}//GEN-LAST:event_shapeFieldChanged
    private boolean shapeChangeInProgress;

	private void keyFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyFieldActionPerformed
            listProps.setBaseKey(keyField.getText());
	}//GEN-LAST:event_keyFieldActionPerformed

	private void nameFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_nameFieldFocusLost
            nameFieldActionPerformed(null);
	}//GEN-LAST:event_nameFieldFocusLost

	private void keyFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_keyFieldFocusLost
            keyFieldActionPerformed(null);
	}//GEN-LAST:event_keyFieldFocusLost

    private void imageResComboFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_imageResComboFocusLost
        imageResComboActionPerformed(null);
    }//GEN-LAST:event_imageResComboFocusLost

    private void nameFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_nameFieldFocusGained
        if (nameField.getText().startsWith(string("cle-new-layer"))) {
            nameField.selectAll();
        }
    }//GEN-LAST:event_nameFieldFocusGained

    private void prefixFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prefixFieldActionPerformed
        int sel = activeIndex;
        if (sel < 0) {
            return;
        }
        get(sel).setKeyPrefix(prefixField.getText());
        listChanged();
    }//GEN-LAST:event_prefixFieldActionPerformed

    private void prefixFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prefixFieldFocusLost
        prefixFieldActionPerformed(null);
    }//GEN-LAST:event_prefixFieldFocusLost

    private void addLayerImpl(Layer layer, int posHint) {
        int pos = posHint;
        if (posHint < 0) {
            pos = activeIndex;
            if (pos < 0) {
                pos = 0;
            }
        } else {
            if (pos > layers.size()) {
                int t = getTemplateLayer();
                if (t >= 0) {
                    pos = t + 1;
                } else {
                    pos = layers.size();
                }
            }
        }

        layers.add(pos, layer);
        layerList.setSelectedIndex(pos);
        setUnsavedChanges(true);
        //listChanged();
    }

    @Override
    public void export() {
        CLEExportDialog exd = new CLEExportDialog(StrangeEons.getWindow(), this);
        exd.setLocationRelativeTo(this);
        exd.setVisible(true);
    }

    @Override
    protected void exportImpl(int type, File f) throws IOException {
        FileOutputStream out = null;
        Writer w = null;
        String s = null;
        try {
            out = new FileOutputStream(f);
            switch (type) {
                case 0:
                    w = new OutputStreamWriter(out, ProjectUtilities.ENC_SETTINGS);
                    s = createExportText(ExportType.SETTINGS, true, true);
                    break;
                case 1:
                    w = new OutputStreamWriter(out, ProjectUtilities.ENC_SCRIPT);
                    s = createExportText(ExportType.PAINTING_CODE, true, true);
                    break;
                default:
                    throw new AssertionError("unknown export type");
            }
            w.write(s);
        } finally {
            if (w != null) {
                w.close();
            } else if (out != null) {
                out.close();
            }
        }
    }

    public String createExportText(ExportType type, boolean isFrontFace, boolean escapeText) {
        StringBuilder b = new StringBuilder();

        switch (type) {
            case SETTINGS:
                b.append("# NOTE: THIS FEATURE IS UNDER DEVELOPMENT.\n# PLEASE REPORT ANY ISSUES.\n\n");
                exportSettingsImpl(b, isFrontFace);
                break;
            case PAINTING_CODE:
                b.append("/* NOTE: THIS FEATURE IS UNDER DEVELOPMENT.\n   PLEASE REPORT ANY ISSUES. */\n\n");
                exportPaintImpl(b, isFrontFace);
        }

        return escapeText ? EscapedTextCodec.escapeUnicode(b.toString()) : b.toString();
    }

    public static enum ExportType {
        SETTINGS, PAINTING_CODE
    };

    public void exportPaintImpl(StringBuilder b, boolean isFrontFace) {
        boolean hasImage = false;
        int template = getTemplateLayer();
        b.append("function paint").append(isFrontFace ? "Front" : "Back").append("( g, diy, sheet ) {\n");
        for (int i = layers.size() - 1; i >= 0; --i) {
            Layer la = get(i);
            if (!la.isExported()) {
                continue;
            }

            if (la.getType() == LayerType.EXPSYM) {
                continue;
            }

            if (i == template) {
                b.append("\tsheet.paintTemplateImage( g );\n\n");
                continue;
            }

            String key = la.getBaseKey().trim();
            if (!key.endsWith("-")) {
                key += "-";
            }

            switch (la.getType()) {
                case IMAGE:
                    String name = key + getImageKey(i, template, isFrontFace);
                    b.append("\t").append("drawImage( g, diy, \"").append(key).append("\" );\n\n");
                    hasImage = true;
                    break;
                case PORTRAIT:
                    b.append("\tsheet.paintPortrait( g );\n\n");
                    break;
                case TEXT:
                    String box = var(la).substring(1) + "Box";
                    b.append('\t').append(box).append(".markupText = ").append(var(la)).append('\n');
                    if (la.hasShape()) {
                        b.append('\t').append(box).append(".pageShape = $").append(var(la)).append(".shape;\n");
                    }
                    b.append('\t').append(box).append(".draw( g, $").append(var(la)).append("_region.region );\n\n");
                    break;
            }
        }
        if (b.charAt(b.length() - 1) == '\n' && b.charAt(b.length() - 2) == '\n') {
            b.deleteCharAt(b.length() - 1);
        }
        b.append("}\n");

        if (hasImage) {
            b.append("\nfunction drawImage( g, diy, imageKey ) {\n");
            b.append("\tvar r = diy.settings.getRegion( imageKey );\n");
            b.append("\tg.drawImage(\n");
            b.append("\t\tImageUtils.fetchImageResource( diy.settings.get( imageKey ), true ),\n");
            b.append("\t\tr.x, r.y, r.width, r.height, null");
            b.append("\t);\n");
            b.append("}\n");
        }
    }

    private void exportSettingsImpl(StringBuilder b, boolean isFrontFace) {
        int template = getTemplateLayer();

        for (int i = layers.size() - 1; i >= 0; --i) {
            Layer la = get(i);
            if (!la.isExported()) {
                continue;
            }

            String key = la.getBaseKey().trim();
            if (!key.endsWith("-")) {
                key += '-';
            }

            String prefix = la.getKeyPrefix();
            if (!prefix.endsWith("-")) {
                prefix += '-';
            }
            if (prefix.length() > 1) {
                key += prefix;
            }

            b.append("# ").append(la.getName()).append('\n');
            switch (la.getType()) {
                case TEXT:
                    b.append(key).append("region = ").append(la.toRegionString()).append('\n');
                    if (la.hasShape()) {
                        b.append(key).append("shape = ").append(la.toShapeString()).append('\n');
                    }
                    break;
                case IMAGE:
                    b.append(key).append(getImageKey(i, template, isFrontFace))
                            .append(" = ").append(la.imageResource.trim()).append('\n');
                    if (i != template) {
                        b.append(key).append(getImageKey(i, template, isFrontFace)).append("-region = ").append(la.toRegionString()).append('\n');
                    }
                    break;
                case PORTRAIT:
                    b.append(key).append("portrait-template = ").append(la.imageResource.trim()).append('\n');
                    b.append(key).append("portrait-clip-region = ").append(la.toRegionString()).append('\n');
                    break;
                case EXPSYM:
                    b.append(key).append(isFrontFace ? "front" : "back").append("-expsym-region = ").append(la.toRegionString()).append('\n');
            }
        }
    }

    private String getImageKey(int layer, int templateLayer, boolean isFrontFace) {
        return (layer < templateLayer ? "overlay"
                : (layer == templateLayer ? ((isFrontFace ? "front" : "back") + "-sheet-template") : "underlay"));
    }

    private String var(Layer la) {
        StringBuilder b = new StringBuilder("$");
        String name = la.getName().trim();
        if (name.startsWith("$")) {
            name = name.substring(1);
        }
        boolean hasSpace = false;
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            if (Character.isSpaceChar(c)) {
                hasSpace = true;
                continue;
            }
            if (!Character.isJavaIdentifierPart(c)) {
                continue;
            }
            if (i == 0 || hasSpace) {
                b.append(Character.toUpperCase(c));
            } else {
                b.append(c);
            }
            hasSpace = false;
        }
        return b.toString();
    }

    @Override
    protected void saveImpl(File f) throws IOException {
        Properties p = new SortedProperties();
        Settings s = Settings.forProperties(null, p);

        listProps.toSettings(s);

        for (int i = 0; i < layers.size(); ++i) {
            get(i).toSettings(s, i);
        }

        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, " Card Layout");
        }
    }

    protected void load(File f) throws IOException {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
            Settings s = Settings.forProperties(null, p);
            listProps.fromSettings(s);
            layers.clear();
            for (int i = 0; p.getProperty(String.valueOf(i) + "-type") != null; ++i) {
                Layer l = new Layer();
                l.fromSettings(s, i);
                layers.addElement(l);
            }
            if (layers.size() == 0) {
                layers.addElement(new Layer());
            }
            listChanged();
            EventQueue.invokeLater(() -> {
                setUnsavedChanges(false);
            });
        }
    }

    @Override
    protected void clearImpl() {
        layers = new DefaultListModel();
        Layer layer = new Layer();
        layers.addElement(layer);
        layerList.setModel(layers);
        listChanged();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addExpSymBtn;
    private javax.swing.JButton addImageBtn;
    private javax.swing.JPanel addPanel;
    private javax.swing.JButton addPortrait;
    private javax.swing.JButton addTextBtn;
    private javax.swing.JPanel cardPropPanel;
    private javax.swing.JSpinner cupX1Field;
    private javax.swing.JSpinner cupX2Field;
    private javax.swing.JSpinner cupYField;
    private javax.swing.JButton deleteBtn;
    private javax.swing.JButton downBtn;
    private javax.swing.JLabel dx1Label;
    private javax.swing.JLabel dx2Label;
    private javax.swing.JScrollPane edScroll;
    private javax.swing.JPanel editPanel;
    private javax.swing.JCheckBox exportCheck;
    private javax.swing.JSpinner hField;
    private javax.swing.JLabel heightLabel;
    private javax.swing.JPanel imagePanel;
    private javax.swing.JComboBox imageResCombo;
    private javax.swing.JButton importPSDBtn;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField keyField;
    private javax.swing.JLabel keyLabel;
    private javax.swing.JPanel layerDynPropPanel;
    private javax.swing.JList layerList;
    private javax.swing.JPanel layerListPanel;
    private javax.swing.JPanel layerStaticPropPanel;
    private javax.swing.JSplitPane listSplitter;
    private javax.swing.JTextField nameField;
    private javax.swing.JTextField prefixField;
    private javax.swing.JLabel regionLabel;
    private javax.swing.JLabel resLabel;
    private javax.swing.JCheckBox resizeCheck;
    private javax.swing.JComboBox shapeCombo;
    private javax.swing.JLabel shapeLabel;
    private javax.swing.JCheckBox showLayerCheck;
    private javax.swing.JPanel textPropPanel;
    private javax.swing.JButton upBtn;
    private javax.swing.JSpinner wField;
    private javax.swing.JLabel widthLabel;
    private javax.swing.JSpinner xField;
    private javax.swing.JSpinner yField;
    private javax.swing.JLabel ySplitLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public String getFileNameExtension() {
        return "cardlayout";
    }

    @Override
    public String getFileTypeDescription() {
        return string("cle-file-desc");
    }

    private boolean isUpdatingUI;

    private void listChanged() {
        if (isUpdatingUI) {
            return;
        }

        isUpdatingUI = true;
        try {
            deleteBtn.setEnabled(layers.size() > 1);
            int sel = activeIndex;
            upBtn.setEnabled(sel > 0);
            downBtn.setEnabled(sel < layers.size() - 1);
            updateEditorForLayer(sel);
            layerList.repaint();
        } finally {
            isUpdatingUI = false;
        }
    }

    @Override
    public void regionChanged(Object source, Rectangle r) {
        boolean old = regionUpdateInProgress;
        regionUpdateInProgress = true;
        try {
            int sel = activeIndex;
            if (sel < 0) {
                return;
            }
            Layer layer = get(sel);

            if (layer == null) {
                StrangeEons.log.log(Level.WARNING, "no active layer");
            } else {
                xField.setValue(r.x);
                yField.setValue(r.y);
                wField.setValue(r.width);
                hField.setValue(r.height);

                layer.setRegion(r);
            }
        } finally {
            regionUpdateInProgress = old;
        }
    }

    public class Layer {

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Layer other = (Layer) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            if ((this.prefix == null) ? (other.prefix != null) : !this.prefix.equals(other.prefix)) {
                return false;
            }
            if (this.region != other.region && (this.region == null || !this.region.equals(other.region))) {
                return false;
            }
            if ((this.imageResource == null) ? (other.imageResource != null) : !this.imageResource.equals(other.imageResource)) {
                return false;
            }
            if (this.show != other.show) {
                return false;
            }
            if (this.export != other.export) {
                return false;
            }
            if (this.resizable != other.resizable) {
                return false;
            }
            if (this.type != other.type && (this.type == null || !this.type.equals(other.type))) {
                return false;
            }
            if (this.cupX1 != other.cupX1) {
                return false;
            }
            if (this.cupX2 != other.cupX2) {
                return false;
            }
            if (this.cupY != other.cupY) {
                return false;
            }
            if (this.cupAtTop != other.cupAtTop) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 37 * hash + (this.prefix != null ? this.prefix.hashCode() : 0);
            hash = 37 * hash + (this.region != null ? this.region.hashCode() : 0);
            hash = 37 * hash + (this.imageResource != null ? this.imageResource.hashCode() : 0);
            hash = 37 * hash + (this.show ? 1 : 0);
            hash = 37 * hash + (this.export ? 1 : 0);
            hash = 37 * hash + (this.resizable ? 1 : 0);
            hash = 37 * hash + (this.type != null ? this.type.hashCode() : 0);
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.cupX1) ^ (Double.doubleToLongBits(this.cupX1) >>> 32));
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.cupX2) ^ (Double.doubleToLongBits(this.cupX2) >>> 32));
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.cupY) ^ (Double.doubleToLongBits(this.cupY) >>> 32));
            hash = 37 * hash + (this.cupAtTop ? 1 : 0);
            return hash;
        }

        private String name = listProps.newLayerName();
        private String prefix = "";
        private Rectangle region = new Rectangle();
        private String imageResource;
        private boolean show = true;
        private boolean export = true;
        private boolean resizable = false;
        private LayerType type = LayerType.IMAGE;
        private BufferedImage image;
        private double cupX1, cupX2, cupY;
        private boolean cupAtTop;

        private void toSettings(Settings s, int index) {
            String key = String.valueOf(index) + "-";
            s.set(key + "name", getName());
            s.set(key + "prefix", prefix);
            s.set(key + "image", imageResource);
            s.set(key + "region", toRegionString());
            s.set(key + "cup-at-top", isCupAtTop() ? "yes" : "no");
            s.set(key + "cup-x1", String.valueOf(getCupX1()));
            s.set(key + "cup-x2", String.valueOf(getCupX2()));
            s.set(key + "cup-y", String.valueOf(getCupY()));
            s.set(key + "visible", isShowing() ? "yes" : "no");
            s.set(key + "export", isExported() ? "yes" : "no");
            s.set(key + "resizable", isResizable() ? "yes" : "no");
            s.set(key + "type", getType().name());
        }

        private void fromSettings(Settings s, int index) {
            String indexPrefix = String.valueOf(index) + "-";
            setName(s.get(indexPrefix + "name", ""));
            prefix = s.get(indexPrefix + "prefix", "");
            setImageResource(s.get(indexPrefix + "image", ""));
            region = s.getRegion(String.valueOf(index));
            setCupAtTop(s.getYesNo(indexPrefix + "cup-at-top"));
            setCupX1(Settings.number(s.get(indexPrefix + "cup-x1", "0")));
            setCupX2(Settings.number(s.get(indexPrefix + "cup-x2", "0")));
            setCupY(Settings.number(s.get(indexPrefix + "cup-y", "0")));
            setShowing(s.getYesNo(indexPrefix + "visible"));
            setExported(s.getYesNo(indexPrefix + "export"));
            setResizable(s.getYesNo(indexPrefix + "resizable"));
            setType(LayerType.valueOf(s.get(indexPrefix + "type")));
        }

        private Layer() {
            setImageResource("templates/large-blank.png");
        }

        @Override
        public String toString() {
            return getName();
        }

        public String toRegionString() {
            return "" + region.x + "," + region.y + "," + region.width + "," + region.height;
        }

        public String toShapeString() {
            double ySplit = Math.min(getCupY() + region.y, region.y + region.height);
            if (isCupAtTop()) {
                return "" + getCupX1() + "," + getCupX2() + "," + ySplit + ",0,0";
            } else {
                return "0,0," + ySplit + "," + getCupX1() + "," + getCupX2();
            }
        }

        public boolean hasShape() {
            return getCupX1() != 0d || getCupX2() != 0d;
        }

        public void setImageResource(String resource) {
            if (resource == null) {
                throw new NullPointerException("resource");
            }
            resource = resource.trim();

            if (resource.equals(imageResource)) {
                return;
            }

            imageResource = resource;
            image = null;
            getImage();
            if (image != null && isResizable()) {
                region.width = image.getWidth();
                region.height = image.getHeight();
            }
            setUnsavedChanges(true);
        }

        public String getImageResource() {
            return imageResource;
        }

        public BufferedImage getImage() {
            if (imageResource == null || imageResource.isEmpty()) {
                return null;
            }
            if (image == null) {
                for (InternalImage ii : internalImages) {
                    if (ii.path.equals(imageResource)) {
                        try {
                            image = ImageIO.read(ii.file);
                        } catch (IOException e) {
                        }
                        break;
                    }
                }
                if (image == null) {
                    image = StrangeImage.getAsBufferedImage("res://" + imageResource);
                }
            }
            return image;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            name = name.trim();
            if (this.name.equals(name)) {
                return;
            }
            this.name = name;
            setUnsavedChanges(true);
        }

        public String getKeyPrefix() {
            return prefix;
        }

        public void setKeyPrefix(String prefix) {
            if (prefix == null) {
                prefix = "";
            } else {
                prefix = prefix.trim();
            }
            if (!this.prefix.equals(prefix)) {
                this.prefix = prefix;
                setUnsavedChanges(true);
            }
        }

        public String getBaseKey() {
            return listProps.getBaseKey();
        }

        public void setX(int x) {
            if (region.x == x) {
                return;
            }
            region.x = x;
            setUnsavedChanges(true);
        }

        public int getX() {
            return region.x;
        }

        public void setY(int y) {
            if (region.y == y) {
                return;
            }
            region.y = y;
            setUnsavedChanges(true);
        }

        public int getY() {
            return region.y;
        }

        public void setWidth(int width) {
            if (region.width == width) {
                return;
            }
            region.width = width;
            setUnsavedChanges(true);
        }

        public int getWidth() {
            return region.width;
        }

        public void setHeight(int height) {
            if (region.height == height) {
                return;
            }
            region.height = height;
            setUnsavedChanges(true);
        }

        public int getHeight() {
            return region.height;
        }

        public void setRegion(int x, int y, int w, int h) {
            setX(x);
            setY(y);
            setWidth(w);
            setHeight(h);
        }

        public void setRegion(Rectangle region) {
            setRegion(region.x, region.y, region.width, region.height);
        }

        public Rectangle getRegion() {
            return (Rectangle) region.clone();
        }

        public boolean isShowing() {
            return show;
        }

        public void setShowing(boolean show) {
            if (this.show == show) {
                return;
            }
            this.show = show;
            setUnsavedChanges(true);
        }

        public boolean isExported() {
            return export;
        }

        public void setExported(boolean export) {
            if (this.export == export) {
                return;
            }
            this.export = export;
            setUnsavedChanges(true);
        }

        public boolean isResizable() {
            return resizable;
        }

        public void setResizable(boolean resizable) {
            if (this.resizable == resizable) {
                return;
            }
            this.resizable = resizable;
            setUnsavedChanges(true);
        }

        public LayerType getType() {
            return type;
        }

        public void setType(LayerType type) {
            if (this.type == type) {
                return;
            }
            this.type = type;
            setUnsavedChanges(true);
        }

        public double getCupX1() {
            return cupX1;
        }

        public void setCupX1(double cupX1) {
            if (this.cupX1 == cupX1) {
                return;
            }
            this.cupX1 = cupX1;
            setUnsavedChanges(true);
        }

        public double getCupX2() {
            return cupX2;
        }

        public void setCupX2(double cupX2) {
            if (this.cupX2 == cupX2) {
                return;
            }
            this.cupX2 = cupX2;
            setUnsavedChanges(true);
        }

        public double getCupY() {
            return cupY;
        }

        public void setCupY(double cupY) {
            if (this.cupY == cupY) {
                return;
            }
            this.cupY = cupY;
            setUnsavedChanges(true);
        }

        public boolean isCupAtTop() {
            return cupAtTop;
        }

        public void setCupAtTop(boolean cupAtTop) {
            if (this.cupAtTop == cupAtTop) {
                return;
            }
            this.cupAtTop = cupAtTop;
            setUnsavedChanges(true);
        }
    }

    public static enum LayerType {
        IMAGE(true),
        TEXT(false),
        PORTRAIT(true),
        EXPSYM(true);

        private LayerType(boolean hasImageRes) {
            this.hasImageRes = hasImageRes;
        }

        private final boolean hasImageRes;

        public boolean hasImageResource() {
            return hasImageRes;
        }
    }

    public final class LayerListProperties {

        private String key;
        private int newLayerCount = 0;

        private LayerListProperties() {
            setBaseKey(null);
        }

        public void setBaseKey(String key) {
            if (key != null) {
                key = key.trim();
            }
            if (key == null || key.isEmpty()) {
                key = "typeid";
            }
            if (!key.equals(this.key)) {
                this.key = key;
                setUnsavedChanges(true);
            }
        }

        public String getBaseKey() {
            return key;
        }

        private String newLayerName() {
            return string("cle-new-layer") + ' ' + (++newLayerCount);
        }

        private void toSettings(Settings s) {
            s.set("basekey", key);
        }

        private void fromSettings(Settings s) {
            setBaseKey(s.get("basekey"));
        }
    }

    private DefaultListModel layers = new DefaultListModel();

    /**
     * Returns the index of the layer that will represent the card template.
     * This is the index of the lowest visible layer with type
     * {@code IMAGE} located at (0,0). If there is no suitable layer,
     * returns -1.
     *
     * @return the index of the template layer, or -1
     */
    public int getTemplateLayer() {
        for (int i = layers.size() - 1; i >= 0; --i) {
            Layer la = get(i);
            if (!la.isShowing()) {
                continue;
            }
            if (la.getType() != LayerType.IMAGE) {
                continue;
            }
            if (la.region.x != 0 || la.region.x != 0) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private void updateEditorForLayer(int index) {
        if (layerUpdateInProgress) {
            return;
        }
        layerUpdateInProgress = true;

        try {
            // locate the template image; this is the image layer with the lowest index
            int templateIndex = getTemplateLayer();

            // create an image that has everything that is underneath the layer at the
            // selected index
            BufferedImage underlay;
            if (templateIndex < 0) {
                // no image in the list; create a dummy image
                underlay = dummyImage;
            } else {
                BufferedImage bi = get(templateIndex).getImage();
                if (bi == null) {
                    bi = dummyImage;
                }
                underlay = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D g = null;
            try {
                g = underlay.createGraphics();
                g.setPaint(Color.ORANGE);
                for (int i = layers.size() - 1; i >= 0 && i > index; --i) {
                    paintLayer(g, get(i));
                }
            } finally {
                if (g != null) {
                    g.dispose();
                }
            }

            // create an image that has everything that is overtop of the layer
            // at the selected index
            BufferedImage overlay = new BufferedImage(underlay.getWidth(), underlay.getHeight(), BufferedImage.TYPE_INT_ARGB);
            g = null;
            try {
                g = overlay.createGraphics();
                g.setPaint(Color.ORANGE);
                for (int i = index - 1; i >= 0; --i) {
                    paintLayer(g, get(i));
                }
            } finally {
                if (g != null) {
                    g.dispose();
                }
            }

            regionEd.setImage(underlay);
            regionEd.setOverlayImage(overlay);

            Layer sel;
            if (index >= 0 && index < layers.size()) {
                sel = get(index);
                switch (sel.getType()) {
                    case IMAGE:
                    case PORTRAIT:
                    case EXPSYM:
                        regionEd.setRegionImage(sel.isShowing() ? sel.getImage() : null);
                        break;
                    case TEXT:
                        regionEd.setRegionImage(null);
                        break;
                    default:
                        throw new AssertionError("unknown layer type");
                }
                regionEd.setRegion(sel.region);
            } else {
                regionEd.setRegion(new Rectangle(0, 0, 1, 1));
                sel = new Layer();
            }

            nameField.setText(sel.getName());
            keyField.setText(sel.getBaseKey());
            imageResCombo.setSelectedItem(sel.getImageResource());
            resLabel.setEnabled(sel.getType().hasImageResource());
            imageResCombo.setEnabled(sel.getType().hasImageResource());
            xField.setValue(sel.region.x);
            yField.setValue(sel.region.y);
            wField.setValue(sel.region.width);
            hField.setValue(sel.region.height);
            if (!sel.hasShape()) {
                shapeCombo.setSelectedIndex(0);
            } else {
                if (sel.isCupAtTop()) {
                    shapeCombo.setSelectedIndex(1);
                } else {
                    shapeCombo.setSelectedIndex(2);
                }
            }

            showLayerCheck.setSelected(sel.isShowing());
            exportCheck.setSelected(sel.isExported());
            regionEd.setResizable(sel.isResizable());

            boolean isTemplate = templateIndex == index;
            regionLabel.setEnabled(!isTemplate);
            xField.setEnabled(!isTemplate);
            yField.setEnabled(!isTemplate);
            wField.setEnabled(sel.isResizable() && !isTemplate);
            hField.setEnabled(sel.isResizable() && !isTemplate);
            widthLabel.setEnabled(sel.isResizable() && !isTemplate);
            heightLabel.setEnabled(sel.isResizable() && !isTemplate);
            resizeCheck.setEnabled(!isTemplate);
            resizeCheck.setSelected(sel.isResizable() && !isTemplate);
            regionEd.setEnabled(!isTemplate);

            // SHAPES
            String propertyPanelToShow;

            if (sel.getType() == LayerType.TEXT) {
                propertyPanelToShow = "text";
                shapeLabel.setEnabled(true);
                shapeCombo.setEnabled(true);
                boolean hasShape = sel.hasShape();
                if (hasShape) {
                    double ySplit = Math.min(sel.getCupY() + sel.region.y, sel.region.y + sel.region.height);
                    if (sel.isCupAtTop()) {
                        regionEd.setPageShape(new CupShape(sel.getCupX1(), sel.getCupX2(), ySplit, 0d, 0d));
                    } else {
                        regionEd.setPageShape(new CupShape(0d, 0d, ySplit, sel.getCupX1(), sel.getCupX2()));
                    }
                } else {
                    regionEd.setPageShape(null);
                }
                cupX1Field.setEnabled(hasShape);
                cupX2Field.setEnabled(hasShape);
                cupYField.setEnabled(hasShape);
                dx1Label.setEnabled(hasShape);
                dx2Label.setEnabled(hasShape);
                ySplitLabel.setEnabled(hasShape);
            } else {
                propertyPanelToShow = "image";
                shapeLabel.setEnabled(false);
                shapeCombo.setEnabled(false);
                cupX1Field.setEnabled(false);
                cupX2Field.setEnabled(false);
                cupYField.setEnabled(false);
                dx1Label.setEnabled(false);
                dx2Label.setEnabled(false);
                ySplitLabel.setEnabled(false);
                regionEd.setPageShape(null);
            }
            ((CardLayout) layerDynPropPanel.getLayout()).show(layerDynPropPanel, propertyPanelToShow);
        } finally {
            layerUpdateInProgress = false;
        }
    }
    private boolean layerUpdateInProgress;

    private void paintLayer(Graphics2D g, Layer layer) {
        if (!layer.isShowing()) {
            return;
        }
        if (layer.getImage() != null) {
            Rectangle r = layer.region;
            g.drawImage(layer.getImage(), r.x, r.y, r.width, r.height, null);
        } else {
            g.draw(layer.region);
        }
    }

    private Layer get(int index) {
        if (index < 0 || index >= layers.size()) {
            return null;
        }
        return (Layer) layers.get(index);
    }

    private BufferedImage dummyImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

    private final Icon ICON_TEXT = ResourceKit.getIcon("ui/dev/layer-text.png");
    private final Icon ICON_IMAGE = ResourceKit.getIcon("ui/dev/layer-image.png");
    private final Icon ICON_TEMPLATE = ResourceKit.getIcon("ui/dev/layer-template.png");
    private final Icon ICON_PORTRAIT = ResourceKit.getIcon("ui/dev/layer-portrait.png");
    private final Icon ICON_EXPSYM = ResourceKit.getIcon("ui/dev/layer-expsym.png");

    private class LayerCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            final boolean isTemplate = index == getTemplateLayer();

            if (templateFont == null) {
                defaultFont = getFont();
                templateFont = defaultFont.deriveFont(Font.BOLD);
            }
            Layer layer = (Layer) value;
            setEnabled(layer.isShowing());
            switch (layer.getType()) {
                case TEXT:
                    setIcon(ICON_TEXT);
                    break;
                case IMAGE:
                    if (isTemplate) {
                        setIcon(ICON_TEMPLATE);
                    } else {
                        setIcon(ICON_IMAGE);
                    }
                    break;
                case PORTRAIT:
                    setIcon(ICON_PORTRAIT);
                    break;
                case EXPSYM:
                    setIcon(ICON_EXPSYM);
                    break;
            }
            setFont(isTemplate ? templateFont : defaultFont);
            return this;
        }

        private Font templateFont, defaultFont;
    }

    @Override
    public boolean canPerformCommand(AbstractCommand command) {
        return command == Commands.CLEAR
                || command == Commands.SAVE
                || command == Commands.SAVE_AS
                || command == Commands.EXPORT;
    }
}
