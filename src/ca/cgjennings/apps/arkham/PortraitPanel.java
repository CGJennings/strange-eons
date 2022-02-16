package ca.cgjennings.apps.arkham;

import static ca.cgjennings.apps.arkham.MarkupTargetFactory.enableTargeting;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.io.FileChangeListener;
import ca.cgjennings.io.FileChangeMonitor;
import ca.cgjennings.io.FileChangeMonitor.ChangeType;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.ui.JFileField;
import ca.cgjennings.ui.JRepeaterButton;
import ca.cgjennings.ui.JUtilities;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import static resources.Language.string;
import resources.ResourceKit;
import resources.StrangeImage;

/**
 * A standard panel for adjusting portraits.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class PortraitPanel extends javax.swing.JPanel implements java.awt.event.ActionListener, java.awt.event.FocusListener, FileChangeListener, MouseListener {

    private static final long serialVersionUID = -7_566_862_039_613_697_337L;
    private final NumberFormat formatter;
    private Color TAB_BACKGROUND;

    /**
     * Creates new form PortraitPanel
     */
    public PortraitPanel() {
        initComponents();

        TAB_BACKGROUND = UIManager.getColor("controlDkShadow");
        if (TAB_BACKGROUND == null) {
            TAB_BACKGROUND = Color.LIGHT_GRAY;
        } else {
            TAB_BACKGROUND = new Color(TAB_BACKGROUND.getRGB()); // nimbus 
        }
        coarseTab.setBackground(TAB_BACKGROUND);
        fineTab.setBackground(TAB_BACKGROUND);

        JUtilities.setIconPair(upBtn, "ui/button/up.png", "ui/button/up-hi.png", true);
        JUtilities.setIconPair(downBtn, "ui/button/down.png", "ui/button/down-hi.png", true);
        JUtilities.setIconPair(leftBtn, "ui/button/left.png", "ui/button/left-hi.png", true);
        JUtilities.setIconPair(rightBtn, "ui/button/right.png", "ui/button/right-hi.png", true);
        JUtilities.setIconPair(rotateLeftBtn, "ui/button/rotate-left.png", "ui/button/rotate-left-hi.png", true);
        JUtilities.setIconPair(rotateRightBtn, "ui/button/rotate-right.png", "ui/button/rotate-right-hi.png", true);
        JUtilities.setIconPair(scaleUpBtn, "ui/button/scale-up.png", "ui/button/scale-up-hi.png", true);
        JUtilities.setIconPair(scaleDownBtn, "ui/button/scale-down.png", "ui/button/scale-down-hi.png", true);

        final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        upBtn.setCursor(HAND);
        downBtn.setCursor(HAND);
        leftBtn.setCursor(HAND);
        rightBtn.setCursor(HAND);
        rotateLeftBtn.setCursor(HAND);
        rotateRightBtn.setCursor(HAND);
        scaleUpBtn.setCursor(HAND);
        scaleDownBtn.setCursor(HAND);

        // make untrackable by markup menu
        enableTargeting(xField, false);
        enableTargeting(yField, false);
        enableTargeting(rotationField, false);
        enableTargeting(scaleField, false);

        formatter = NumberFormat.getNumberInstance(panelLocale);

        JRepeaterButton[] repeaters = new JRepeaterButton[]{
            upBtn, downBtn, leftBtn, rightBtn,
            rotateLeftBtn, rotateRightBtn, scaleUpBtn, scaleDownBtn
        };
        for (JRepeaterButton rb : repeaters) {
            rb.setFireEventOnRelease(true);
        }

        // synch with the portrait whenever the tab gets selected;
        // this will cover the majority of cases when updatePanel
        // needs to be called automatically without making an
        // excessive number of calls
        addHierarchyListener((HierarchyEvent e) -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                if (isShowing()) {
                    updatePanel();
                }
            }
        });
        
        portrait = createNullPortrait();
    }
    
    private static Portrait createNullPortrait() {
        return new Portrait() {
            @Override
            public void setSource(String resource) {
            }

            @Override
            public String getSource() {
                return "";
            }

            @Override
            public BufferedImage getImage() {
                return bi;
            }
            private BufferedImage bi = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);

            @Override
            public void setImage(String reportedSource, BufferedImage image) {
            }

            @Override
            public double getScale() {
                return 1d;
            }

            @Override
            public void setScale(double scale) {
            }

            @Override
            public double getPanX() {
                return 0d;
            }

            @Override
            public void setPanX(double x) {
            }

            @Override
            public double getPanY() {
                return 0d;
            }

            @Override
            public void setPanY(double y) {
            }

            @Override
            public void setPan(Point2D pan) {
            }

            @Override
            public Point2D getPan(Point2D dest) {
                return new Point2D.Double(0d, 0d);
            }

            @Override
            public double getRotation() {
                return 0d;
            }

            @Override
            public void setRotation(double angleInDegrees) {
            }

            @Override
            public void installDefault() {
            }

            @Override
            public Dimension getClipDimensions() {
                return new Dimension(1,1);
            }

            @Override
            public BufferedImage getClipStencil() {
                return null;
            }

            @Override
            public EnumSet<Portrait.Feature> getFeatures() {
                return EnumSet.noneOf(Portrait.Feature.class);
            }
        };
    }

    /**
     * Change the portrait source as if the user had typed a new file name into
     * the field and pressed Enter.
     *
     * @param newSource the new portrait source file
     */
    public void setSource(String newSource) {
        if (newSource == null) {
            throw new NullPointerException("newSource");
        }

        PortraitPanel p = this;
        while (p.parentPanel != null) {
            p = p.parentPanel;
        }

        p.portraitField.setText(newSource);
        p.portraitFieldActionPerformed(null);
    }

    /**
     * Returns the current value of the portrait source field.
     *
     * @return the source text
     */
    public String getSource() {
        PortraitPanel p = this;
        while (p.parentPanel != null) {
            p = p.parentPanel;
        }

        return p.portraitField.getText();
    }

    /**
     * Returns {@code true} if the invoking an editor application is supported.
     *
     * @return {@code true} if editing is supported
     */
    public static boolean isEditingSupported() {
        return DesktopIntegration.EDIT_SUPPORTED;
    }

    /**
     * Invokes the editing application associated with the given file in the
     * operating system and instructs it to load the file.
     *
     * @param file the file to edit
     */
    public void editFile(File file) {
        if (!isEditingSupported()) {
            return;
        }

        try {
            DesktopIntegration.edit(file, this);
        } catch (Exception e) {
            UIManager.getLookAndFeel().provideErrorFeedback(this);
            ErrorDialog.displayError(string("app-err-open", file.getName()), e);
        }
    }

    public static Locale getPanelLocale() {
        return panelLocale;
    }

    public static void setPanelLocale(Locale aPanelLocale) {
        panelLocale = aPanelLocale;
    }
    private static Locale panelLocale = Locale.getDefault();

    /**
     * Updates the panel with current numeric settings from the portrait.
     */
    public void updatePanel() {
        if (parentPanel == null) {
            portraitField.setText(portrait.getSource());
        }
        portraitControl.synchronize();
        updateNumericFields();
    }

    /**
     * Sets the title for this panel. By default, portrait panels have a titled
     * border with a default title ("Portrait Adjustment", or a localized
     * variant thereof). Calling this method changes the title; if the border
     * has been replaced with a different kind of border, a new titled border is
     * installed with the title. Otherwise, the existing border's title is
     * updated.
     *
     * @param title sets the panel title, creating a title border if necessary
     */
    public void setPanelTitle(String title) {
        if (getBorder() instanceof TitledBorder) {
            ((TitledBorder) getBorder()).setTitle(title);
        } else {
            setBorder(new TitledBorder(title));
        }
    }

    /**
     * Returns the title of the panel's border title. If the border is not
     * longer a titled border, returns the default title.
     *
     * @return the panel title
     * @see #setPanelTitle(java.lang.String)
     */
    public String getPanelTitle() {
        if (getBorder() instanceof TitledBorder) {
            return ((TitledBorder) getBorder()).getTitle();
        }
        return string("ae-panel-portrait");
    }

    /**
     * Sets the portrait that this panel will adjust.
     */
    public void setPortrait(Portrait p) {
        if (p == null) {
            p = createNullPortrait();
        }
        portrait = p;

        final EnumSet<Portrait.Feature> features = portrait.getFeatures();
        
        final boolean rotates = features.contains(Portrait.Feature.ROTATE);
        rotateLeftBtn.setVisible(rotates);
        rotateRightBtn.setVisible(rotates);
        rotationLabel.setVisible(rotates);
        rotationField.setVisible(rotates);
        degreeLabel.setVisible(rotates);
        rotateGapLabel.setVisible(rotates);

        portraitControl.setPortrait(p);
        
        updateNumericFields();
        if (childPanel != null) {
            childPanel.updatePanel();
        }

        validate();
    }

    public Portrait getPortrait() {
        return portrait;
    }

    private void updateNumericFields() {
        if (rotationField.isVisible()) {
            double theta = portrait.getRotation();
            rotationField.setText(formatter.format(theta));
        }
        scaleField.setText(formatter.format(portrait.getScale() * 100d));
        xField.setText(formatter.format(portrait.getPanX()));
        yField.setText(formatter.format(portrait.getPanY()));
    }

    private Portrait portrait;

    /**
     * Link this panel to another panel. The image to use will be determined by
     * the linked parent panel, so image selection is disabled.
     *
     * @param parent the panel to link to
     */
    public void setParentPanel(PortraitPanel parent) {
        parent.childPanel = this;
        parentPanel = parent;
        imageLabel.setVisible(false);
        portraitField.setVisible(false);
    }
    private PortraitPanel parentPanel = null, childPanel = null;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        imageLabel = new javax.swing.JLabel();
        portraitField =  new ImagePastableTextField() ;
        cardPanel = new javax.swing.JPanel();
        coarseAdjustPanel = new javax.swing.JPanel();
        portraitControl = new ca.cgjennings.apps.arkham.PortraitControl();
        fineAdjustPanel = new javax.swing.JPanel();
        repeatMessageLabel = new javax.swing.JLabel();
        translationPanel = new javax.swing.JPanel();
        nudgePanel = new javax.swing.JPanel();
        leftBtn = new ca.cgjennings.ui.JRepeaterButton();
        rightBtn = new ca.cgjennings.ui.JRepeaterButton();
        downBtn = new ca.cgjennings.ui.JRepeaterButton();
        upBtn = new ca.cgjennings.ui.JRepeaterButton();
        xyPosPanel = new javax.swing.JPanel();
        xField = new javax.swing.JTextField();
        xLabel = new javax.swing.JLabel();
        yField = new javax.swing.JTextField();
        yLabel = new javax.swing.JLabel();
        resetBtn = new javax.swing.JButton();
        nudegLabel = new javax.swing.JLabel();
        scaleRotatePanel = new javax.swing.JPanel();
        rotateLeftBtn = new ca.cgjennings.ui.JRepeaterButton();
        rotateRightBtn = new ca.cgjennings.ui.JRepeaterButton();
        rotationField = new javax.swing.JTextField();
        rotationLabel = new javax.swing.JLabel();
        degreeLabel = new javax.swing.JLabel();
        rotateGapLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        scaleLabel = new javax.swing.JLabel();
        scaleDownBtn = new ca.cgjennings.ui.JRepeaterButton();
        scaleField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        scaleUpBtn = new ca.cgjennings.ui.JRepeaterButton();
        jLabel2 = new javax.swing.JLabel();
        coarseTab = new javax.swing.JLabel();
        fineTab = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder(string("ae-panel-portrait"))); // NOI18N
        setName("Form"); // NOI18N
        setLayout(new java.awt.GridBagLayout());

        imageLabel.setFont(imageLabel.getFont().deriveFont(imageLabel.getFont().getStyle() | java.awt.Font.BOLD, imageLabel.getFont().getSize()-1));
        imageLabel.setText(string("ae-panel-image")); // NOI18N
        imageLabel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        imageLabel.setName("imageLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 100;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 6);
        add(imageLabel, gridBagConstraints);

        portraitField.setColumns(40);
        portraitField.setFont(portraitField.getFont().deriveFont(portraitField.getFont().getSize()-1f));
        portraitField.setDragEnabled(true);
        portraitField.setName("portraitField"); // NOI18N
        portraitField.addActionListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 6);
        add(portraitField, gridBagConstraints);

        cardPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 1, 1, java.awt.Color.gray), javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0)));
        cardPanel.setName("cardPanel"); // NOI18N
        cardPanel.setLayout(new java.awt.CardLayout());

        coarseAdjustPanel.setName("coarseAdjustPanel"); // NOI18N
        coarseAdjustPanel.setLayout(new java.awt.GridBagLayout());

        portraitControl.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.gray));
        portraitControl.setName("portraitControl"); // NOI18N

        javax.swing.GroupLayout portraitControlLayout = new javax.swing.GroupLayout(portraitControl);
        portraitControl.setLayout(portraitControlLayout);
        portraitControlLayout.setHorizontalGroup(
            portraitControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 346, Short.MAX_VALUE)
        );
        portraitControlLayout.setVerticalGroup(
            portraitControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 136, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
        coarseAdjustPanel.add(portraitControl, gridBagConstraints);

        cardPanel.add(coarseAdjustPanel, "c");

        fineAdjustPanel.setName("fineAdjustPanel"); // NOI18N

        repeatMessageLabel.setFont(repeatMessageLabel.getFont().deriveFont(repeatMessageLabel.getFont().getStyle() | java.awt.Font.BOLD, repeatMessageLabel.getFont().getSize()-1));
        repeatMessageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        repeatMessageLabel.setText(" ");
        repeatMessageLabel.setName("repeatMessageLabel"); // NOI18N

        translationPanel.setName("translationPanel"); // NOI18N
        translationPanel.setLayout(new java.awt.BorderLayout());

        nudgePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 4));
        nudgePanel.setName("nudgePanel"); // NOI18N
        nudgePanel.setLayout(new java.awt.GridBagLayout());

        leftBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        leftBtn.setToolTipText(string("ae-b-left")); // NOI18N
        leftBtn.setContentAreaFilled(false);
        leftBtn.setMargin(new java.awt.Insets(2, 2, 2, 2));
        leftBtn.setName("leftBtn"); // NOI18N
        leftBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        leftBtn.addActionListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        nudgePanel.add(leftBtn, gridBagConstraints);

        rightBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        rightBtn.setToolTipText(string("ae-b-right")); // NOI18N
        rightBtn.setContentAreaFilled(false);
        rightBtn.setMargin(new java.awt.Insets(2, 2, 2, 2));
        rightBtn.setName("rightBtn"); // NOI18N
        rightBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        rightBtn.addActionListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        nudgePanel.add(rightBtn, gridBagConstraints);

        downBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        downBtn.setToolTipText(string("ae-b-down")); // NOI18N
        downBtn.setContentAreaFilled(false);
        downBtn.setMargin(new java.awt.Insets(2, 2, 2, 2));
        downBtn.setName("downBtn"); // NOI18N
        downBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        downBtn.addActionListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        nudgePanel.add(downBtn, gridBagConstraints);

        upBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        upBtn.setToolTipText(string("ae-b-up")); // NOI18N
        upBtn.setContentAreaFilled(false);
        upBtn.setMargin(new java.awt.Insets(2, 2, 2, 2));
        upBtn.setName("upBtn"); // NOI18N
        upBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        upBtn.addActionListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        nudgePanel.add(upBtn, gridBagConstraints);

        translationPanel.add(nudgePanel, java.awt.BorderLayout.CENTER);

        xyPosPanel.setName("xyPosPanel"); // NOI18N
        xyPosPanel.setLayout(new java.awt.GridBagLayout());

        xField.setColumns(6);
        xField.setFont(xField.getFont().deriveFont(xField.getFont().getSize()-2f));
        xField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        xField.setText("0");
        xField.setMargin(new java.awt.Insets(1, 1, 1, 1));
        xField.setName("xField"); // NOI18N
        xField.addActionListener(this);
        xField.addFocusListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 4);
        xyPosPanel.add(xField, gridBagConstraints);

        xLabel.setText("x");
        xLabel.setName("xLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        xyPosPanel.add(xLabel, gridBagConstraints);

        yField.setColumns(6);
        yField.setFont(yField.getFont().deriveFont(yField.getFont().getSize()-2f));
        yField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        yField.setText("0");
        yField.setMargin(new java.awt.Insets(1, 1, 1, 1));
        yField.setName("yField"); // NOI18N
        yField.addActionListener(this);
        yField.addFocusListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 4);
        xyPosPanel.add(yField, gridBagConstraints);

        yLabel.setText("y");
        yLabel.setName("yLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 0, 4);
        xyPosPanel.add(yLabel, gridBagConstraints);

        resetBtn.setFont(resetBtn.getFont().deriveFont(resetBtn.getFont().getSize()-2f));
        resetBtn.setIcon( ResourceKit.getIcon( "ui/button/portrait-xy-reset.png" ) );
        resetBtn.setText(string("ae-b-reset")); // NOI18N
        resetBtn.setName("resetBtn"); // NOI18N
        resetBtn.addActionListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE_LEADING;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 4);
        xyPosPanel.add(resetBtn, gridBagConstraints);

        translationPanel.add(xyPosPanel, java.awt.BorderLayout.EAST);

        nudegLabel.setFont(nudegLabel.getFont().deriveFont(nudegLabel.getFont().getStyle() | java.awt.Font.BOLD, nudegLabel.getFont().getSize()-1));
        nudegLabel.setText(string("ae-l-nudge")); // NOI18N
        nudegLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 4, 0), javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray)));
        nudegLabel.setName("nudegLabel"); // NOI18N
        translationPanel.add(nudegLabel, java.awt.BorderLayout.NORTH);

        scaleRotatePanel.setName("scaleRotatePanel"); // NOI18N

        rotateLeftBtn.setBorder(null);
        rotateLeftBtn.setToolTipText(string("de-tt-rotate-left")); // NOI18N
        rotateLeftBtn.setContentAreaFilled(false);
        rotateLeftBtn.setName("rotateLeftBtn"); // NOI18N
        rotateLeftBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        rotateLeftBtn.addActionListener(this);

        rotateRightBtn.setBorder(null);
        rotateRightBtn.setToolTipText(string("de-tt-rotate-right")); // NOI18N
        rotateRightBtn.setContentAreaFilled(false);
        rotateRightBtn.setName("rotateRightBtn"); // NOI18N
        rotateRightBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        rotateRightBtn.addActionListener(this);

        rotationField.setColumns(5);
        rotationField.setFont(rotationField.getFont().deriveFont(rotationField.getFont().getSize()-1f));
        rotationField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        rotationField.setName("rotationField"); // NOI18N
        rotationField.addActionListener(this);
        rotationField.addFocusListener(this);

        rotationLabel.setFont(rotationLabel.getFont().deriveFont(rotationLabel.getFont().getStyle() | java.awt.Font.BOLD, rotationLabel.getFont().getSize()-1));
        rotationLabel.setText(string("ae-l-rotate")); // NOI18N
        rotationLabel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        rotationLabel.setName("rotationLabel"); // NOI18N

        degreeLabel.setFont(degreeLabel.getFont().deriveFont(degreeLabel.getFont().getSize()-1f));
        degreeLabel.setText("°");
        degreeLabel.setName("degreeLabel"); // NOI18N

        rotateGapLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 6, 0));
        rotateGapLabel.setName("rotateGapLabel"); // NOI18N

        javax.swing.GroupLayout scaleRotatePanelLayout = new javax.swing.GroupLayout(scaleRotatePanel);
        scaleRotatePanel.setLayout(scaleRotatePanelLayout);
        scaleRotatePanelLayout.setHorizontalGroup(
            scaleRotatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scaleRotatePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rotateLeftBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rotationField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(degreeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rotateRightBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(scaleRotatePanelLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(rotateGapLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, scaleRotatePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rotationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        scaleRotatePanelLayout.setVerticalGroup(
            scaleRotatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scaleRotatePanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(rotationLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(scaleRotatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(rotationField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(degreeLabel)
                    .addComponent(rotateRightBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rotateLeftBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rotateGapLabel)
                .addGap(0, 0, 0))
        );

        jPanel1.setName("jPanel1"); // NOI18N

        scaleLabel.setFont(scaleLabel.getFont().deriveFont(scaleLabel.getFont().getStyle() | java.awt.Font.BOLD, scaleLabel.getFont().getSize()-1));
        scaleLabel.setText(string("ae-l-scale")); // NOI18N
        scaleLabel.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray));
        scaleLabel.setName("scaleLabel"); // NOI18N

        scaleDownBtn.setBorder(null);
        scaleDownBtn.setToolTipText(string("ae-tt-scale-down")); // NOI18N
        scaleDownBtn.setContentAreaFilled(false);
        scaleDownBtn.setName("scaleDownBtn"); // NOI18N
        scaleDownBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        scaleDownBtn.addActionListener(this);

        scaleField.setColumns(5);
        scaleField.setFont(scaleField.getFont().deriveFont(scaleField.getFont().getSize()-1f));
        scaleField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        scaleField.setName("scaleField"); // NOI18N
        scaleField.addFocusListener(this);
        scaleField.addActionListener(this);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getSize()-1f));
        jLabel1.setText("%");
        jLabel1.setName("jLabel1"); // NOI18N

        scaleUpBtn.setBorder(null);
        scaleUpBtn.setToolTipText(string("ae-tt-scale-up")); // NOI18N
        scaleUpBtn.setContentAreaFilled(false);
        scaleUpBtn.setName("scaleUpBtn"); // NOI18N
        scaleUpBtn.setPreferredSize(new java.awt.Dimension(24, 24));
        scaleUpBtn.addActionListener(this);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(scaleDownBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scaleField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scaleUpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(scaleLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(scaleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(scaleDownBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(scaleField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(scaleUpBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout fineAdjustPanelLayout = new javax.swing.GroupLayout(fineAdjustPanel);
        fineAdjustPanel.setLayout(fineAdjustPanelLayout);
        fineAdjustPanelLayout.setHorizontalGroup(
            fineAdjustPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fineAdjustPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fineAdjustPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(repeatMessageLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, fineAdjustPanelLayout.createSequentialGroup()
                        .addComponent(translationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32)
                        .addGroup(fineAdjustPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(scaleRotatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        fineAdjustPanelLayout.setVerticalGroup(
            fineAdjustPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fineAdjustPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(repeatMessageLabel)
                .addGap(1, 1, 1)
                .addGroup(fineAdjustPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(translationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(fineAdjustPanelLayout.createSequentialGroup()
                        .addComponent(scaleRotatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cardPanel.add(fineAdjustPanel, "f");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 6);
        add(cardPanel, gridBagConstraints);

        jLabel2.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 1, java.awt.Color.gray));
        jLabel2.setName("jLabel2"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.ipadx = 18;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        add(jLabel2, gridBagConstraints);

        coarseTab.setBackground(java.awt.Color.lightGray);
        coarseTab.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        coarseTab.setIcon(ResourceKit.getIcon("ui/button/portrait-coarse.png"));
        coarseTab.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 0, 0, java.awt.Color.gray), javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 1)));
        coarseTab.setMinimumSize(new java.awt.Dimension(16, 22));
        coarseTab.setName("coarseTab"); // NOI18N
        coarseTab.setPreferredSize(new java.awt.Dimension(16, 22));
        coarseTab.addMouseListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.ipady = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 0);
        add(coarseTab, gridBagConstraints);

        fineTab.setBackground(java.awt.Color.lightGray);
        fineTab.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        fineTab.setIcon( ResourceKit.getIcon( "ui/button/portrait-fine.png" ) );
        fineTab.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 0, 1, java.awt.Color.gray));
        fineTab.setMinimumSize(new java.awt.Dimension(16, 22));
        fineTab.setName("fineTab"); // NOI18N
        fineTab.setOpaque(true);
        fineTab.setPreferredSize(new java.awt.Dimension(16, 22));
        fineTab.addMouseListener(this);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = 4;
        gridBagConstraints.ipady = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        add(fineTab, gridBagConstraints);
    }

    // Code for dispatching events from components to event handlers.

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (evt.getSource() == portraitField) {
            PortraitPanel.this.portraitFieldActionPerformed(evt);
        }
        else if (evt.getSource() == leftBtn) {
            PortraitPanel.this.leftBtnActionPerformed(evt);
        }
        else if (evt.getSource() == rightBtn) {
            PortraitPanel.this.rightBtnActionPerformed(evt);
        }
        else if (evt.getSource() == downBtn) {
            PortraitPanel.this.downBtnActionPerformed(evt);
        }
        else if (evt.getSource() == upBtn) {
            PortraitPanel.this.upBtnActionPerformed(evt);
        }
        else if (evt.getSource() == xField) {
            PortraitPanel.this.xFieldActionPerformed(evt);
        }
        else if (evt.getSource() == yField) {
            PortraitPanel.this.yFieldActionPerformed(evt);
        }
        else if (evt.getSource() == resetBtn) {
            PortraitPanel.this.resetBtnActionPerformed(evt);
        }
        else if (evt.getSource() == rotateLeftBtn) {
            PortraitPanel.this.rotateLeftBtnActionPerformed(evt);
        }
        else if (evt.getSource() == rotateRightBtn) {
            PortraitPanel.this.rotateRightBtnActionPerformed(evt);
        }
        else if (evt.getSource() == rotationField) {
            PortraitPanel.this.rotationFieldActionPerformed(evt);
        }
        else if (evt.getSource() == scaleDownBtn) {
            PortraitPanel.this.scaleDownBtnActionPerformed(evt);
        }
        else if (evt.getSource() == scaleUpBtn) {
            PortraitPanel.this.scaleUpBtnActionPerformed(evt);
        }
        else if (evt.getSource() == scaleField) {
            PortraitPanel.this.scaleFieldActionPerformed(evt);
        }
    }

    public void focusGained(java.awt.event.FocusEvent evt) {
    }

    public void focusLost(java.awt.event.FocusEvent evt) {
        if (evt.getSource() == xField) {
            PortraitPanel.this.xFieldFocusLost(evt);
        }
        else if (evt.getSource() == yField) {
            PortraitPanel.this.yFieldFocusLost(evt);
        }
        else if (evt.getSource() == rotationField) {
            PortraitPanel.this.rotationFieldFocusLost(evt);
        }
        else if (evt.getSource() == scaleField) {
            PortraitPanel.this.scaleFieldFocusLost(evt);
        }
    }

    public void mouseClicked(java.awt.event.MouseEvent evt) {
    }

    public void mouseEntered(java.awt.event.MouseEvent evt) {
    }

    public void mouseExited(java.awt.event.MouseEvent evt) {
    }

    public void mousePressed(java.awt.event.MouseEvent evt) {
        if (evt.getSource() == coarseTab) {
            PortraitPanel.this.tabPressed(evt);
        }
        else if (evt.getSource() == fineTab) {
            PortraitPanel.this.tabPressed(evt);
        }
    }

    public void mouseReleased(java.awt.event.MouseEvent evt) {
    }// </editor-fold>//GEN-END:initComponents
    private void yFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_yFieldFocusLost
        yFieldActionPerformed(null);
    }//GEN-LAST:event_yFieldFocusLost

    private void xFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_xFieldFocusLost
        xFieldActionPerformed(null);
    }//GEN-LAST:event_xFieldFocusLost

    private double parseField(JTextField field, double previousValue) {
        try {
            return formatter.parse(field.getText()).doubleValue();
        } catch (ParseException p) {
            Toolkit.getDefaultToolkit().beep();
            field.selectAll();
            field.requestFocusInWindow();
            return previousValue;
        }
    }

    private boolean checkRepeatCommand(ActionEvent e) {
        if (e == null) {
            return false;
        }

        if (e.getActionCommand().equals(JRepeaterButton.PRESSED_COMMAND)) {
            showRepeatMessage(true);
        } else if (e.getActionCommand().equals(JRepeaterButton.RELEASE_COMMAND)) {
            showRepeatMessage(false);
            return false;
        }

        return true;
    }

    private void yFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yFieldActionPerformed
        portrait.setPanY(parseField(yField, portrait.getPanY()));
    }//GEN-LAST:event_yFieldActionPerformed

    private void xFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xFieldActionPerformed
        portrait.setPanX(parseField(xField, portrait.getPanX()));
    }//GEN-LAST:event_xFieldActionPerformed

    private void rotateRightBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rotateRightBtnActionPerformed
        if (checkRepeatCommand(evt)) {
            rotateDelta(1d, evt);
        }
    }//GEN-LAST:event_rotateRightBtnActionPerformed

    private void rotateDelta(double d, ActionEvent evt) {
        if (evt != null) {
            if ((evt.getModifiers() & (ActionEvent.SHIFT_MASK | ActionEvent.ALT_MASK | ActionEvent.CTRL_MASK | ActionEvent.META_MASK)) != 0) {
                d *= 45d;
            }
        }

        double theta = portrait.getRotation() - d;
        theta = Math.IEEEremainder(theta, 360f);
        if (theta < 0) {
            theta += 360;
        }
        portrait.setRotation(theta);
        updateNumericFields();
    }

    private void rotateLeftBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rotateLeftBtnActionPerformed
        if (checkRepeatCommand(evt)) {
            rotateDelta(-1d, evt);
        }
    }//GEN-LAST:event_rotateLeftBtnActionPerformed

    private void scaleUpBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleUpBtnActionPerformed
        if (checkRepeatCommand(evt)) {
            double scale = Math.min(portrait.getScale() + 0.1d, 10d);
            portrait.setScale(scale);
            updateNumericFields();
        }
    }//GEN-LAST:event_scaleUpBtnActionPerformed

    private void scaleDownBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleDownBtnActionPerformed
        if (checkRepeatCommand(evt)) {
            double scale = Math.max(portrait.getScale() - 0.1d, 0.01);
            portrait.setScale(scale);
            updateNumericFields();
        }
    }//GEN-LAST:event_scaleDownBtnActionPerformed

    private void rightBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rightBtnActionPerformed
        translate(evt, 1, 0);
    }//GEN-LAST:event_rightBtnActionPerformed

    private void leftBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_leftBtnActionPerformed
        translate(evt, -1, 0);
    }//GEN-LAST:event_leftBtnActionPerformed

    private void downBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downBtnActionPerformed
        translate(evt, 0, 1);
    }//GEN-LAST:event_downBtnActionPerformed

    private void upBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upBtnActionPerformed
        translate(evt, 0, -1);
    }//GEN-LAST:event_upBtnActionPerformed

    private void translate(ActionEvent event, double dx, double dy) {
        if (checkRepeatCommand(event)) {
            double unitSize = 1d;
            int repeats = ((JRepeaterButton) event.getSource()).getRepeatCount();

            if (repeats > 50 || ((event.getModifiers() & ActionEvent.CTRL_MASK) != 0)) {
                unitSize = 100d;
            } else if (repeats > 10 || ((event.getModifiers() & ActionEvent.SHIFT_MASK) != 0)) {
                unitSize = 10;
            }

            double tx = unitSize * dx;
            double ty = unitSize * dy;
            portrait.setPan(
                    new Point2D.Double(portrait.getPanX() + tx, portrait.getPanY() + ty)
            );
            updateNumericFields();
        }
    }

    private void resetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetBtnActionPerformed
        portrait.setPan(new Point2D.Double(0d, 0d));
        updateNumericFields();
    }//GEN-LAST:event_resetBtnActionPerformed

    private void showRepeatMessage(boolean show) {
        String message = show ? string("ae-l-hold-to-repeat") : " ";
        if (!repeatMessageLabel.getText().equals(message)) {
            repeatMessageLabel.setText(message);
        }
    }

    private void scaleFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scaleFieldActionPerformed
        double scale = parseField(scaleField, Math.nextUp(0d), 1000d, portrait.getScale() * 100d);
        portrait.setScale(scale / 100d);
    }//GEN-LAST:event_scaleFieldActionPerformed

    private void rotationFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rotationFieldActionPerformed
        if (!rotationField.isVisible()) {
            return;
        }
        double theta = parseField(rotationField, -1e10, 1e10, portrait.getRotation());
        if (theta == theta) {
            theta = Math.IEEEremainder(theta, 360f);
            if (theta < 0) {
                theta += 360;
            }
            portrait.setRotation(theta);
            updateNumericFields();
        }
    }//GEN-LAST:event_rotationFieldActionPerformed

    private double parseField(JTextField f, double min, double max, double previousValue) {
        double n = parseField(f, previousValue);
        if (n < min) {
            n = min;
            f.setText(formatter.format(n));
        }
        if (n > max) {
            n = max;
            f.setText(formatter.format(n));
        }
        return n;
    }

    private void portraitFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portraitFieldActionPerformed
        if (parentPanel != null) {
            return;
        }

        String s = portraitField.getText();
        try {
            // TODO: this is a workaround that clears the portrait so
            //       that it will always reload even if the file is
            //       the same (in case the file changes)
            BufferedImage bi = portrait.getImage();
            int oldW = bi.getWidth();
            int oldH = bi.getHeight();
            double oldScale = portrait.getScale();
            double oldX = portrait.getPanX();
            double oldY = portrait.getPanY();
            double oldRot = portrait.getRotation();
            // pop in a dummy image
            portrait.setSource("res://icons/1x1.png");
            // put in the real image
            portrait.setSource(s);
            // if the new image has the same dimensions,
            // restore the existing configuration
            bi = portrait.getImage();
            if (oldW == bi.getWidth() && oldH == bi.getHeight()) {
                portrait.setRotation(oldRot);
                portrait.setScale(oldScale);
                portrait.setPanX(oldX);
                portrait.setPanY(oldY);
            }

            updateNumericFields();
            portraitControl.synchronize();
            monitor(s);
            if (childPanel != null) {
                childPanel.updatePanel();
            }
        } catch (Exception e) {
            ErrorDialog.displayError(string("ae-err-portrait"), e);
        }
    }//GEN-LAST:event_portraitFieldActionPerformed

	private void scaleFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_scaleFieldFocusLost
            scaleFieldActionPerformed(null);
	}//GEN-LAST:event_scaleFieldFocusLost

	private void rotationFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_rotationFieldFocusLost
            rotationFieldActionPerformed(null);
	}//GEN-LAST:event_rotationFieldFocusLost

	private void tabPressed( java.awt.event.MouseEvent evt ) {//GEN-FIRST:event_tabPressed
            JComponent tab = (JComponent) evt.getSource();
            swapActiveTab();
            if (parentPanel != null) {
                parentPanel.swapActiveTab();
            }
            if (childPanel != null) {
                childPanel.swapActiveTab();
            }
	}//GEN-LAST:event_tabPressed

    private void swapActiveTab() {
        boolean selectCoarse = coarseTab.isOpaque();

        // swap tab component borders and opaque states
        Border temp = coarseTab.getBorder();
        coarseTab.setBorder(fineTab.getBorder());
        fineTab.setBorder(temp);
        coarseTab.setOpaque(!selectCoarse);
        fineTab.setOpaque(selectCoarse);

        // anytime the coarse tab is reselected, tell the control to re-read
        // the portrait properties since they may have been edited
        if (selectCoarse) {
            portraitControl.synchronize();
        } else {
            updateNumericFields();
        }

        // move card to front depending on which tab was pressed
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, selectCoarse ? "c" : "f");
    }

    private void monitor(String file) {
        FileChangeMonitor.getSharedInstance().removeFileChangeListener(this);
        if (file != null && file.length() > 0 && StrangeImage.isFileIdentifier(file)) {
            File f = new File(file);
            if (f.isAbsolute()) {
                FileChangeMonitor.getSharedInstance().addFileChangeListener(this, f);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel cardPanel;
    private javax.swing.JPanel coarseAdjustPanel;
    private javax.swing.JLabel coarseTab;
    private javax.swing.JLabel degreeLabel;
    private ca.cgjennings.ui.JRepeaterButton downBtn;
    private javax.swing.JPanel fineAdjustPanel;
    private javax.swing.JLabel fineTab;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private ca.cgjennings.ui.JRepeaterButton leftBtn;
    private javax.swing.JLabel nudegLabel;
    private javax.swing.JPanel nudgePanel;
    private ca.cgjennings.apps.arkham.PortraitControl portraitControl;
    private javax.swing.JTextField portraitField;
    private javax.swing.JLabel repeatMessageLabel;
    private javax.swing.JButton resetBtn;
    private ca.cgjennings.ui.JRepeaterButton rightBtn;
    private javax.swing.JLabel rotateGapLabel;
    private ca.cgjennings.ui.JRepeaterButton rotateLeftBtn;
    private ca.cgjennings.ui.JRepeaterButton rotateRightBtn;
    private javax.swing.JTextField rotationField;
    private javax.swing.JLabel rotationLabel;
    private ca.cgjennings.ui.JRepeaterButton scaleDownBtn;
    private javax.swing.JTextField scaleField;
    private javax.swing.JLabel scaleLabel;
    private javax.swing.JPanel scaleRotatePanel;
    private ca.cgjennings.ui.JRepeaterButton scaleUpBtn;
    private javax.swing.JPanel translationPanel;
    private ca.cgjennings.ui.JRepeaterButton upBtn;
    private javax.swing.JTextField xField;
    private javax.swing.JLabel xLabel;
    private javax.swing.JPanel xyPosPanel;
    private javax.swing.JTextField yField;
    private javax.swing.JLabel yLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void fileChanged(File f, ChangeType type) {
        if (type == ChangeType.CREATED || type == ChangeType.MODIFIED) {
            if (f.equals(new File(portraitField.getText()))) {
                portraitFieldActionPerformed(null);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            FileChangeMonitor.getSharedInstance().removeFileChangeListener(this);
        } finally {
            super.finalize();
        }
    }

    @SuppressWarnings("serial")
    static class ImagePastableTextField extends JFileField {

        private PortraitPanel getPanel() {
            Container c = getParent();
            while (c != null && !(c instanceof PortraitPanel)) {
                c = c.getParent();
            }
            return (PortraitPanel) c;
        }

        public ImagePastableTextField() {

            setFileType(FileType.PORTRAIT);

            clipMenu = new JPopupMenu();
            final JMenuItem cut = new JMenuItem(string("cut"));
            cut.addActionListener((ActionEvent e) -> {
                cut();
            });
            clipMenu.add(cut);
            final JMenuItem copy = new JMenuItem(string("copy"));
            copy.addActionListener((ActionEvent e) -> {
                copy();
            });
            clipMenu.add(copy);
            final JMenuItem paste = new JMenuItem(string("paste"));
            paste.addActionListener((ActionEvent e) -> {
                paste();
            });
            clipMenu.add(paste);

            clipMenu.addSeparator();

            final JMenuItem editItem = new JMenuItem(string("edit-card"));
            editItem.addActionListener((ActionEvent e) -> {
                PortraitPanel pp = getPanel();
                if (pp != null) {
                    pp.editFile(new File(ImagePastableTextField.this.getText()));
                }
            });
            clipMenu.add(editItem);

            clipMenu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    boolean hasSelection = getSelectionStart() != getSelectionEnd();
                    copy.setEnabled(hasSelection);
                    cut.setEnabled(hasSelection);

                    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                    paste.setEnabled(t != null && (t.isDataFlavorSupported(DataFlavor.imageFlavor)
                            || t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                            || t.isDataFlavorSupported(DataFlavor.stringFlavor)));
                    File f = new File(getText());
                    editItem.setEnabled(f.exists());
                    editItem.setVisible(getPanel() != null);
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });

            setComponentPopupMenu(clipMenu);
        }

        private final JPopupMenu clipMenu;

        @Override
        public void paste() {
            // check for an image on the clipboard
            try {
                Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (t != null) {
                    if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Image image = (Image) t.getTransferData(DataFlavor.imageFlavor);
                        JUtilities.showWaitCursor(this);
                        try {
                            File tempFile = File.createTempFile("se-pasted-image-", ".png");
                            BufferedImage bi = ImageUtilities.imageToBufferedImage(image);
                            ImageIO.write(bi, "png", tempFile);
                            tempFile.deleteOnExit();
                            setText(tempFile.getAbsolutePath());
                            fireActionPerformed();
                            return;
                        } finally {
                            JUtilities.hideWaitCursor(this);
                        }
                    }
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            setText(((File) files.get(0)).getAbsolutePath());
                            fireActionPerformed();
                            return;
                        }
                    }
                }
            } catch (IllegalStateException | UnsupportedFlavorException e) {
                // clipboard unavailable
            } catch (IOException e) {
                UIManager.getLookAndFeel().provideErrorFeedback(this);
                StrangeEons.log.log(Level.WARNING, "unable to read portrait file", e);
            }

            // found no other datatype: fallback on regular text paste
            super.paste();
        }
    }
}
