package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.layout.PageShape;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.ui.dnd.ScrapBook;
import ca.cgjennings.ui.textedit.CodeEditorBase;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A dialog that allows for the editing of regions on image resources.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class RegionPicker extends javax.swing.JDialog {

    /**
     * Creates new form RegionPicker
     */
    public RegionPicker(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setCloseMode(true);
        editor.setEventSource(this);

        Settings s = Settings.getUser();
        String v = s.get(KEY_RESTRICT, "yes");
        restrictCheck.setSelected(Settings.yesNo(v));

        showOtherCheck.setSelected(s.getYesNo(KEY_SHOW_ALL));

        int z = s.getInt(KEY_ZOOM, 5);
        editor.setZoom(z > 0 ? z : 5);

        editor.addRegionChangeListener((Object source, Rectangle region) -> {
            if (ignoreRegionChange) {
                return;
            }
            ignoreRegionChange = true;

            x.setValue(region.x);
            y.setValue(region.y);
            w.setValue(region.width);
            h.setValue(region.height);

            if (autocopyCheck.isSelected()) {
                updateRegionInEditor(region);
            }

            ignoreRegionChange = false;
        });
        editor.addMouseWheelListener((MouseWheelEvent e) -> {
            int dz = e.getWheelRotation();
            int zoom = editor.getZoom() - dz;
            if (zoom < 1) {
                zoom = 1;
            }
            if (zoom > 32) {
                zoom = 32;
            }
            editor.setZoom(zoom);
            updateMousePos();
            Settings.getUser().set(KEY_ZOOM, String.valueOf(zoom));
            e.consume();
        });
        editor.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateMousePos();
            }
        });

        JPopupMenu copyPasteMenu = new JPopupMenu();
        JMenuItem item = new JMenuItem(string("copy"));
        item.addActionListener((ActionEvent e) -> {
            editor.copy();
        });
        copyPasteMenu.add(item);

        item = new JMenuItem(string("paste"));
        item.addActionListener((ActionEvent e) -> {
            editor.paste();
        });
        copyPasteMenu.add(item);

        editor.setComponentPopupMenu(copyPasteMenu);
        controlPanel.setComponentPopupMenu(copyPasteMenu);

        setSize(getPreferredSize().width + 64, getHeight());

        Settings.getUser().applyWindowSettings("draw-region", this);

        // do first autocopy if possible, otherwise try to paste clipboard region
        if (StrangeEons.getWindow().getActiveEditor() instanceof CodeEditor) {
            Rectangle r = getRegionFromEditor();
            if (r != null) {
                setRegion(r);
            }
        } else {
            editor.paste();
        }
        // this must happen after initializing the region from the code or
        // else it will copy the initial 0,0,1,1 region to the code and back
        v = s.get(KEY_AUTOCOPY, "yes");
        autocopyCheck.setSelected(Settings.yesNo(v));

        editor.scrollRectToSelection();
        autocopyCheckActionPerformed(null);
    }

    private static Rectangle tryToConvert(String s) {
        if (s != null) {
            try {
                return Settings.region(s);
            } catch (Exception e) {
            }
        }
        return null;
    }

    private boolean ignoreRegionChange;

    private void setHightlightsFromEditor() {
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed == null || !(ed instanceof CodeEditor) || (autocopyCheck.isSelected() && !showOtherCheck.isSelected())) {
            editor.setHighlightRegions(null);
            return;
        }

        if (PAT_DQUOTE == null) {
            PAT_DQUOTE = Pattern.compile("\\\"(\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+)\\\"");
            PAT_SQUOTE = Pattern.compile("\\\'(\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+)\\\'");
            PAT_SETTING = Pattern.compile("-region\\s*(?:\\=|\\:)\\s*(\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+)");
        }

        HashSet<Rectangle> set = new HashSet<>();
        CodeEditorBase ced = ((CodeEditor) ed).getEditor();

        String text = ced.getText();
        addHighlightsFromPattern(set, PAT_DQUOTE.matcher(text));
        addHighlightsFromPattern(set, PAT_SQUOTE.matcher(text));
        addHighlightsFromPattern(set, PAT_SETTING.matcher(text));

        if (!set.isEmpty()) {
            editor.setHighlightRegions(set.toArray(Rectangle[]::new));
        } else {
            editor.setHighlightRegions(null);
        }
    }

    private void addHighlightsFromPattern(Set<Rectangle> set, Matcher m) {
        while (m.find()) {
            Rectangle r = Settings.region(m.group(1));
            if (r != null) {
                set.add(r);
            }
        }
    }

    private static Pattern PAT_DQUOTE, PAT_SQUOTE, PAT_SETTING;

    private Rectangle getRegionFromEditor() {
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed != null && ed instanceof CodeEditor) {
            CodeEditorBase ced = ((CodeEditor) ed).getEditor();
            String sel = ced.getSelectedText();
            boolean bump = false;
            if ((sel.startsWith("\"") && sel.endsWith("\""))
                    || (sel.startsWith("'") && sel.endsWith("'"))) {
                bump = true;
                sel = sel.substring(1, sel.length() - 1);
            }
            Rectangle r = tryToConvert(sel);
            if (bump && r != null) {
                int start = Math.min(ced.getSelectionStart(), ced.getSelectionEnd());
                int end = Math.max(ced.getSelectionStart(), ced.getSelectionEnd());
                ced.select(start + 1, end - 1);
            }
            return r;
        }
        return null;
    }

    private void updateRegionInEditor(Rectangle r) {
        StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
        if (ed != null && ed instanceof CodeEditor && getRegionFromEditor() != null) {
            CodeEditor ced = (CodeEditor) ed;
            int start = Math.min(ced.getEditor().getSelectionStart(), ced.getEditor().getSelectionStart());
            ced.getEditor().beginCompoundEdit();
            try {
                String text = regionToString(r);
                ced.getEditor().setSelectedText(text);
                ced.getEditor().select(start, start + text.length());
            } finally {
                ced.getEditor().endCompoundEdit();
            }
        }
    }

    private void updateMousePos() {
        Point p = editor.getMousePosition();
        if (p != null) {
            editor.viewToModel(p, p);
            mousePosLabel.setText(String.format("%,d | %,d  %d\u00d7", p.x, p.y, editor.getZoom()));
        } else {
            mousePosLabel.setText(String.format("0 | 0  %d\u00d7", editor.getZoom()));
        }
    }

    private String regionToString(Rectangle r) {
        return String.format("%d,%d,%d,%d", r.x, r.y, r.width, r.height);
    }

    public Rectangle[] getHighlightRegions() {
        return editor.getHighlightRegions();
    }

    /**
     * Sets an array of additional regions that are relevant but not the region
     * being edited. These are shown in another colour for reference.
     *
     * @param highlightRegions
     */
    public void setHighlightRegions(Rectangle[] highlightRegions) {
        editor.setHighlightRegions(highlightRegions);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        editor = new RegionChooser();
        scrollBox = new JScrollPane( editor );
        controlPanel = new javax.swing.JPanel();
        h = new javax.swing.JSpinner();
        x = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        y = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        w = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        okBtn = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        restrictCheck = new javax.swing.JCheckBox();
        autocopyCheck = new javax.swing.JCheckBox();
        mousePosLabel = new javax.swing.JLabel();
        copyBtn = new javax.swing.JButton();
        showOtherCheck = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(string( "red-title" )); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        scrollBox.setBorder(null);
        scrollBox.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollBox.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollBox.setAutoscrolls(true);
        getContentPane().add(scrollBox, java.awt.BorderLayout.CENTER);

        controlPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.gray));

        h.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        h.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        x.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        x.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        jLabel1.setText("(");

        jLabel2.setText(",");

        y.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        y.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        jLabel3.setText(",");

        w.setModel(new javax.swing.SpinnerNumberModel(0, -10000, 10000, 1));
        w.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                regionFieldChanged(evt);
            }
        });

        jLabel4.setText(",");

        jLabel5.setText(")");

        jLabel7.setText(string( "red-region" )); // NOI18N

        okBtn.setText(string( "select" )); // NOI18N
        okBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okBtnActionPerformed(evt);
            }
        });

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("x");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("y");

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText(string( "de-card-width" )); // NOI18N

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText(string( "de-card-height" )); // NOI18N

        restrictCheck.setSelected(true);
        restrictCheck.setText(string( "red-clip" )); // NOI18N
        restrictCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restrictCheckActionPerformed(evt);
            }
        });

        autocopyCheck.setText(string( "red-autocopy" )); // NOI18N
        autocopyCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autocopyCheckActionPerformed(evt);
            }
        });

        mousePosLabel.setFont(mousePosLabel.getFont().deriveFont(mousePosLabel.getFont().getSize()-1f));
        mousePosLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mousePosLabel.setText("1000|1000 32x");
        mousePosLabel.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 1, 0, java.awt.Color.gray), javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1)));

        copyBtn.setIcon( ResourceKit.getIcon("copy").medium());
        copyBtn.setToolTipText(string( "copy" )); // NOI18N
        copyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyBtnActionPerformed(evt);
            }
        });

        showOtherCheck.setText(string("red-show-all")); // NOI18N
        showOtherCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showOtherCheckActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout controlPanelLayout = new javax.swing.GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(controlPanelLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(showOtherCheck)
                        .addContainerGap())
                    .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(controlPanelLayout.createSequentialGroup()
                            .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(autocopyCheck)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, controlPanelLayout.createSequentialGroup()
                                    .addComponent(restrictCheck)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 321, Short.MAX_VALUE)
                                    .addComponent(okBtn))
                                .addComponent(jLabel7)
                                .addGroup(controlPanelLayout.createSequentialGroup()
                                    .addGap(10, 10, 10)
                                    .addComponent(jLabel1)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(x, javax.swing.GroupLayout.Alignment.LEADING))
                                    .addGap(2, 2, 2)
                                    .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(controlPanelLayout.createSequentialGroup()
                                            .addComponent(jLabel2)
                                            .addGap(2, 2, 2)
                                            .addComponent(y, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGap(2, 2, 2)
                                    .addComponent(jLabel3)
                                    .addGap(2, 2, 2)
                                    .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(w))
                                    .addGap(2, 2, 2)
                                    .addComponent(jLabel4)
                                    .addGap(2, 2, 2)
                                    .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(h))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jLabel5)
                                    .addGap(18, 18, 18)
                                    .addComponent(copyBtn)))
                            .addContainerGap())
                        .addComponent(mousePosLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );
        controlPanelLayout.setVerticalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(controlPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(w, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(h, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(x, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jLabel4)
                            .addComponent(y, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(jLabel3)
                            .addComponent(copyBtn))
                        .addGap(1, 1, 1)
                        .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10)))
                    .addComponent(mousePosLabel))
                .addGap(18, 18, 18)
                .addComponent(autocopyCheck)
                .addGap(2, 2, 2)
                .addComponent(showOtherCheck)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okBtn)
                    .addComponent(restrictCheck))
                .addContainerGap())
        );

        getContentPane().add(controlPanel, java.awt.BorderLayout.SOUTH);

        setSize(new java.awt.Dimension(621, 570));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

	private void regionFieldChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_regionFieldChanged
            Rectangle region = new Rectangle(
                    (Integer) x.getValue(),
                    (Integer) y.getValue(),
                    (Integer) w.getValue(),
                    (Integer) h.getValue()
            );
            editor.setRegion(region);
	}//GEN-LAST:event_regionFieldChanged

	private void restrictCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restrictCheckActionPerformed
            editor.setRestrictedRegion(restrictCheck.isSelected());
            Settings.getUser().set(KEY_RESTRICT, restrictCheck.isSelected() ? "yes" : "no");
	}//GEN-LAST:event_restrictCheckActionPerformed

	private void okBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okBtnActionPerformed
            if (isCloseMode()) {
                editor.copy();
            }
            dispose();
	}//GEN-LAST:event_okBtnActionPerformed

	private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
            if (autocopyCheck.isSelected()) {
                Rectangle r = getRegionFromEditor();
                if (r != null) {
                    setRegion(r);
                }
            }

            StrangeEonsEditor ed = StrangeEons.getWindow().getActiveEditor();
            if (ed != null && ed instanceof CodeEditor) {
                setHightlightsFromEditor();
            }
	}//GEN-LAST:event_formWindowActivated

	private void autocopyCheckActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_autocopyCheckActionPerformed
            // the actual effects of this are done on focus events
            if (autocopyCheck.isEnabled()) {
                Settings.getUser().set(KEY_AUTOCOPY, autocopyCheck.isSelected() ? "yes" : "no");
                showOtherCheck.setEnabled(autocopyCheck.isSelected());
            }
	}//GEN-LAST:event_autocopyCheckActionPerformed

	private void showOtherCheckActionPerformed( java.awt.event.ActionEvent evt ) {//GEN-FIRST:event_showOtherCheckActionPerformed
            Settings.getUser().set(KEY_SHOW_ALL, showOtherCheck.isSelected() ? "yes" : "no");
            setHightlightsFromEditor();
	}//GEN-LAST:event_showOtherCheckActionPerformed

    private void copyBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyBtnActionPerformed
        Rectangle r = getRegion();
        ScrapBook.setText(regionToString(r));
    }//GEN-LAST:event_copyBtnActionPerformed

    private static final String KEY_AUTOCOPY = "draw-region-autocopy";
    private static final String KEY_SHOW_ALL = "draw-region-show-all";
    private static final String KEY_RESTRICT = "draw-region-restricted";
    private static final String KEY_ZOOM = "draw-region-zoom";

    public static final String IMAGE_PROPERTY_NAME = "image";

    public void setImage(BufferedImage image) {
        BufferedImage old = getImage();
        if (image == old) {
            return;
        }

        editor.setImage(image);
        firePropertyChange(IMAGE_PROPERTY_NAME, old, image);
    }

    public BufferedImage getImage() {
        return editor.getImage();
    }

    public void addRegionChangeListener(RegionChangeListener li) {
        editor.addRegionChangeListener(li);
    }

    public void removeRegionChangeListener(RegionChangeListener li) {
        editor.removeRegionChangeListener(li);
    }

    public Rectangle getRegion() {
        return editor.getRegion();
    }

    public void setRegion(Rectangle r) {
        editor.setRegion(r);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autocopyCheck;
    private javax.swing.JPanel controlPanel;
    private javax.swing.JButton copyBtn;
    private javax.swing.JSpinner h;
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
    private javax.swing.JLabel mousePosLabel;
    private javax.swing.JButton okBtn;
    private javax.swing.JCheckBox restrictCheck;
    private javax.swing.JScrollPane scrollBox;
    private javax.swing.JCheckBox showOtherCheck;
    private javax.swing.JSpinner w;
    private javax.swing.JSpinner x;
    private javax.swing.JSpinner y;
    // End of variables declaration//GEN-END:variables
	private RegionChooser editor;

    public static class RegionChooser extends JComponent {

        private Object eventSource;
        private BufferedImage image;
        private Rectangle region;
        private int zoom;
        private boolean resizable = true;
        private boolean clickToDefineEnabled = true;

        public RegionChooser() {
            eventSource = this;
            region = new Rectangle(0, 0, 0, 0);
            zoom = 1;
            setImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
            setRegion(region);
            setZoom(zoom);
            setAutoscrolls(true);
            setFocusable(true);
            setOpaque(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (JUtilities.backButton(e)) {
                        setZoom(getZoom() - 1);
                    } else if (JUtilities.forwardButton(e)) {
                        setZoom(getZoom() + 1);
                    }

                    if (e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    Point p = e.getPoint();
                    viewToModel(p, p);
                    dragStartRect = getRegion();
                    dragType = getLocationType(p);
                    dragCorner = getDragType(p);

                    dragX = p.x;
                    dragY = p.y;

                    isDragging = true;

                    if (dragType == LOC_DRAG) {
                        Rectangle r = getRegion();
                        dragX = r.x;
                        if ((dragCorner & 1) == 0) {
                            dragX += r.width;
                        }
                        dragY = r.y;
                        if ((dragCorner & 2) == 0) {
                            dragY += r.height;
                        }
                    }

                    if (dragType == LOC_OUTSIDE && !isClickToDefineEnabled()) {
                        isDragging = false;
                    }

                    requestFocusInWindow();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isDragging = false;
                    if (e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(getCursor());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!isDragging || !isEnabled()) {
                        return;
                    }

                    if (dragType == LOC_OUTSIDE) {
                        setRegion(new Rectangle(dragX, dragY, 0, 0));
                        dragType = LOC_DRAG;
                        dragCorner = DRAG_LR;
                    }

                    Point p = e.getPoint();
                    viewToModel(p, p);

                    Rectangle r = getRegion();
                    if (dragType == LOC_INSIDE) {
                        int dx = p.x - dragX;
                        int dy = p.y - dragY;
                        dragX = p.x;
                        dragY = p.y;
                        r.x += dx;
                        r.y += dy;
                        if (isRestrictedRegion()) {
                            BufferedImage bi = getImage();
                            if (r.x < 0) {
                                r.x = 0;
                            }
                            if (r.y < 0) {
                                r.y = 0;
                            }
                            if (r.x + r.width > bi.getWidth()) {
                                r.x = bi.getWidth() - r.width;
                            }
                            if (r.y + r.height > bi.getHeight()) {
                                r.y = bi.getHeight() - r.height;
                            }
                        }
                        setRegion(r);
                    } else if (dragType == LOC_DRAG) {
                        if ((dragCorner & 1) == 0) {
                            r.x = p.x;
                            r.width = dragX - p.x;
                        } else {
                            r.x = dragX;
                            r.width = p.x - dragX;
                        }
                        if ((dragCorner & 2) == 0) {
                            r.y = p.y;
                            r.height = dragY - p.y;
                        } else {
                            r.y = dragY;
                            r.height = p.y - dragY;
                        }

                        boolean flipped = false;
                        if (r.width < 0) {
                            dragCorner ^= 1;
                            flipped = true;
                        }
                        if (r.height < 0) {
                            dragCorner ^= 2;
                            flipped = true;
                        }
                        if (flipped) {
                            mouseDragged(e);
                        } else {
                            setRegion(r);
                        }
                    }

                    addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (!isEnabled()) {
                                return;
                            }
                            int jump = 1;
                            if (e.isShiftDown()) {
                                jump = 16;
                            } else if (e.isAltDown() || e.isControlDown() || e.isMetaDown()) {
                                jump = 128;
                            }

                            int dx = 0, dy = 0;
                            switch (e.getKeyCode()) {
                                case KeyEvent.VK_DOWN:
                                case KeyEvent.VK_NUMPAD2:
                                    dy = 1;
                                    break;
                                case KeyEvent.VK_UP:
                                case KeyEvent.VK_NUMPAD8:
                                    dy = -1;
                                    break;
                                case KeyEvent.VK_LEFT:
                                case KeyEvent.VK_NUMPAD4:
                                    dx = -1;
                                    break;
                                case KeyEvent.VK_RIGHT:
                                case KeyEvent.VK_NUMPAD6:
                                    dx = 1;
                                    break;
                            }

                            if (dx != 0 || dy != 0) {
                                region.x += dx * jump;
                                region.y += dy * jump;
                                addDirtyRect();
                                fireRegionChanged();
                            }
                        }
                    });

                    // this handles the synthetic mouse drags created by autoscrolling
                    scrollRectTemp.setBounds(p.x * zoom, p.y * zoom, zoom, zoom);
                    scrollRectToVisible(scrollRectTemp);
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ESCAPE:
                            if (isDragging) {
                                isDragging = false;
                                setRegion(dragStartRect);
                            }
                            break;
                        case KeyEvent.VK_DELETE:
                        case KeyEvent.VK_BACK_SPACE:
                            setRegion(new Rectangle(0, 0, 0, 0));
                            break;
                        case KeyEvent.VK_C:
                            if (e.isControlDown() || e.isMetaDown()) {
                                copy();
                            }
                            break;
                        case KeyEvent.VK_V:
                            if (e.isControlDown() || e.isMetaDown()) {
                                paste();
                            }
                            break;
                    }
                }
            });
        }
        private Rectangle scrollRectTemp = new Rectangle();

        private int dragX, dragY, dragType, dragCorner;
        // the rectangle to restore if the user presses Esc while dragging
        private Rectangle dragStartRect;
        private boolean isDragging;

        public static final int LOC_OUTSIDE = 0;
        public static final int LOC_INSIDE = 1;
        public static final int LOC_DRAG = 2;

        private static final int DRAG_UL = 0;
        private static final int DRAG_UR = 1;
        private static final int DRAG_LL = 2;
        private static final int DRAG_LR = 3;

        public int getLocationType(Point p) {
            if (!isEnabled()) {
                return LOC_OUTSIDE;
            }
            Rectangle r = getRegion();
            if (getDragType(p) >= 0) {
                return LOC_DRAG;
            }
            if (r.contains(p)) {
                return LOC_INSIDE;
            }
            return LOC_OUTSIDE;
        }

        private int getDragType(Point p) {
            if (!resizable) {
                return -1;
            }
            Rectangle r = getRegion();
            if (p.distanceSq((r.x + r.width), (r.y + r.height)) < DRAG_DIST_SQ) {
                return DRAG_LR;
            }
            if (p.distanceSq((r.x + r.width), (r.y)) < DRAG_DIST_SQ) {
                return DRAG_UR;
            }
            if (p.distanceSq((r.x), (r.y + r.height)) < DRAG_DIST_SQ) {
                return DRAG_LL;
            }
            if (p.distanceSq((r.x), (r.y)) < DRAG_DIST_SQ) {
                return DRAG_UL;
            }
            return -1;
        }

        private static final int DRAG_DIST_SQ = 16;

        public void scrollRectToSelection() {
            Rectangle r = getRegion();
            scrollRectTemp.setBounds(r.x * zoom, r.y * zoom, r.width * zoom, r.height * zoom);
            scrollRectToVisible(scrollRectTemp);
        }

        @Override
        public Cursor getCursor() {
            Point p = getMousePosition();
            if (p != null) {
                viewToModel(p, p);
                int type = getLocationType(p);
                switch (type) {
                    case LOC_DRAG:
                        switch (getDragType(p)) {
                            case DRAG_UL:
                                return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                            case DRAG_LL:
                                return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
                            case DRAG_UR:
                                return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                            case DRAG_LR:
                                return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
                        }
                        throw new AssertionError();
                    case LOC_INSIDE:
                        return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
                    case LOC_OUTSIDE:
                        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
                }
            }
            return Cursor.getDefaultCursor();
        }

        public BufferedImage getImage() {
            return image;
        }

        public void setImage(BufferedImage image) {
            this.image = image;
            updateSize();
            repaint();
        }

        public Rectangle getRegion() {
            return new Rectangle(region);
        }

        public void setRegion(Rectangle region) {
            if (region == null) {
                throw new NullPointerException();
            }
            if (!this.region.equals(region)) {
                addDirtyRect();
                this.region.setFrame(region);
                if (isRestrictedRegion()) {
                    BufferedImage bi = getImage();
                    this.region = region.intersection(new Rectangle(0, 0, bi.getWidth(), bi.getHeight()));
                    if (this.region.isEmpty()) {
                        int x = Math.max(0, Math.min(region.x, bi.getWidth()));
                        int y = Math.max(0, Math.min(region.y, bi.getHeight()));
                        this.region.setFrame(x, y, 0, 0);
                    }
                }
                addDirtyRect();
                fireRegionChanged();
            }
        }

        private void addDirtyRect() {
            repaint();
//			if( shape == null ) {
//				repaint( (region.x-zoom)*zoom, (region.y-zoom)*zoom, ((region.width+1)+zoom*2)*zoom, ((region.height+1)+zoom*2)*zoom );
//			} else {
//				repaint();
//			}
        }

        public int getZoom() {
            return zoom;
        }

        public void setZoom(int zoom) {
            if (zoom < 1) {
                zoom = 1;
            }
            if (zoom > 32) {
                zoom = 32;
            }
            if (this.zoom != zoom) {
                final int old = this.zoom;
                this.zoom = zoom;
                updateSize();
                repaint();
                Rectangle r = getRegion();
                scrollRectToVisible(modelToView(r, r));
                firePropertyChange(ZOOM_CHANGED_PROPERTY, old, zoom);
            }
        }

        private void updateSize() {
            BufferedImage image = getImage();
            Dimension size = new Dimension(image.getWidth() * getZoom(), image.getHeight() * getZoom());
            setPreferredSize(size);
            Component parent = getParent();
            if (parent instanceof JViewport) {
                ((JViewport) parent).setViewSize(size);
            }
        }

        @Override
        protected void paintComponent(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            int zoom = getZoom();
            BufferedImage image = getImage();
            int iw = image.getWidth(), ih = image.getHeight();

            g.setPaint(getBackground());
            if (image == null || image.getTransparency() != BufferedImage.OPAQUE) {
                // have to paint entire background in case image has transparency
                g.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g.fillRect(iw * zoom, 0, getWidth(), getHeight());
                g.fillRect(0, ih * zoom, iw * zoom, getHeight());
            }

            g.drawImage(image, 0, 0, iw * zoom, ih * zoom, null);

            if (highlightRegions != null) {
                g.setPaint(COLOR_HIGHLIGHTS);
                for (int i = 0; i < highlightRegions.length; ++i) {
                    Rectangle hr = highlightRegions[i];
                    g.drawRect(hr.x * zoom, hr.y * zoom, hr.width * zoom, hr.height * zoom);
                }
            }

            g.setPaint(COLOR_PRIMARY);
            Rectangle r = getRegion();

            if (regionImage == null) {
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                g.fillRect(r.x * zoom + 1, r.y * zoom + 1, r.width * zoom, r.height * zoom);
                g.setComposite(old);
            } else {
                Shape clip = g.getClip();
                g.clipRect(0, 0, image.getWidth() * zoom, image.getHeight() * zoom);
                g.drawImage(regionImage, r.x * zoom, r.y * zoom, r.width * zoom, r.height * zoom, null);
                g.setClip(clip);
            }

            if (overlay != null) {
                g.drawImage(overlay, 0, 0, overlay.getWidth() * zoom, overlay.getHeight() * zoom, null);
            }

            if (zoom > 1) {
                Composite old = null;
                if (zoom <= 4) {
                    old = g.getComposite();
                    g.setComposite(AlphaComposite.SrcOver.derive((zoom - 1) / 4f));
                }
                g.setPaint(getBackground());
                for (int x = 0; x <= iw; ++x) {
                    g.drawLine(x * zoom, 0, x * zoom, ih * zoom);
                }
                for (int y = 0; y <= ih; ++y) {
                    g.drawLine(0, y * zoom, iw * zoom, y * zoom);
                }
                if (zoom <= 4) {
                    g.setComposite(old);
                }
            }
            g.setStroke(new BasicStroke(Math.max(1, zoom * 2 / 5)));
            g.setPaint(COLOR_PRIMARY);
            g.drawRect(r.x * zoom, r.y * zoom, r.width * zoom, r.height * zoom);
            if (shape != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(COLOR_SHAPE);
                //Shape clip = g.getClip();
                Stroke s = g.getStroke();
                AffineTransform at = g.getTransform();
                g.setStroke(SHAPE_STROKE);
                g.scale(zoom, zoom);
                //g.clip( r );
                shape.debugDraw(g, r);
                g.setTransform(at);
                g.setStroke(s);
                //g.setClip( clip );
            }
        }

        private PageShape shape;
        private Stroke SHAPE_STROKE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, new float[]{3f, 2f}, 0f);

        private Color COLOR_PRIMARY = Color.MAGENTA;
        private Color COLOR_SHAPE = Color.CYAN;
        private Color COLOR_HIGHLIGHTS = Color.BLUE;

        public void setRegionColor(Color c) {
            if (c == null) {
                throw new NullPointerException();
            }
            COLOR_PRIMARY = c;
            repaint();
        }

        public Color getRegionColor() {
            return COLOR_PRIMARY;
        }

        public void setHighlightColor(Color c) {
            if (c == null) {
                throw new NullPointerException();
            }
            COLOR_HIGHLIGHTS = c;
            repaint();
        }

        public Color getHighlightColor() {
            return COLOR_HIGHLIGHTS;
        }

        public void setPageShapeColor(Color c) {
            if (c == null) {
                throw new NullPointerException();
            }
            COLOR_SHAPE = c;
            repaint();
        }

        public Color getPageShapeColor() {
            return COLOR_SHAPE;
        }

        public Rectangle[] highlightRegions;

        public BufferedImage overlay;
        public BufferedImage regionImage;

        public Rectangle[] getHighlightRegions() {
            return highlightRegions;
        }

        /**
         * Sets an array of additional regions that are relevant but not the
         * region being edited. These are shown in another colour for reference.
         *
         * @param highlightRegions
         */
        public void setHighlightRegions(Rectangle[] highlightRegions) {
            if (this.highlightRegions != highlightRegions) {
                this.highlightRegions = highlightRegions;
                repaint();
            }
        }

        public void setOverlayImage(BufferedImage overlay) {
            this.overlay = overlay;
            repaint();
        }

        public BufferedImage getOverlayImage() {
            return overlay;
        }

        public void setRegionImage(BufferedImage region) {
            regionImage = region;
            repaint();
        }

        public BufferedImage getRegionImage() {
            return regionImage;
        }

        public Point viewToModel(Point p, Point dest) {
            if (dest == null) {
                dest = new Point();
            }
            dest.x = p.x / zoom;
            dest.y = p.y / zoom;
            return dest;
        }

        public Point modelToView(Point p, Point dest) {
            if (dest == null) {
                dest = new Point();
            }
            dest.x = p.x * zoom;
            dest.y = p.y * zoom;
            return dest;
        }

        public Rectangle modelToView(Rectangle r, Rectangle dest) {
            if (dest == null) {
                dest = new Rectangle();
            }
            dest.x = r.x * zoom;
            dest.y = r.y * zoom;
            dest.width = r.width * zoom;
            dest.height = r.height * zoom;
            return dest;
        }

        private void setEventSource(Object source) {
            eventSource = source;
        }

        public void addRegionChangeListener(RegionChangeListener li) {
            listenerList.add(RegionChangeListener.class, li);
        }

        public void removeRegionChangeListener(RegionChangeListener li) {
            listenerList.remove(RegionChangeListener.class, li);
        }

        protected void fireRegionChanged() {
            // Guaranteed to return a non-null array
            Object[] listeners = listenerList.getListenerList();
            Rectangle region = null;
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == RegionChangeListener.class) {
                    if (region == null) {
                        region = getRegion();
                    }
                    ((RegionChangeListener) listeners[i + 1]).regionChanged(eventSource, region);
                }
            }
        }

        /**
         * Copy the current region to the clipboard as a string.
         */
        public void copy() {
            Rectangle r = getRegion();
            StringSelection sel = new StringSelection(
                    String.format("%d,%d,%d,%d", r.x, r.y, r.width, r.height)
            );
            getToolkit().getSystemClipboard().setContents(sel, sel);
        }

        /**
         * Parse a region from the current clipboard text and, if valid, set
         * this editor to that region.
         */
        public void paste() {
            Clipboard c = getToolkit().getSystemClipboard();
            if (c.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                try {
                    String s = (String) c.getData(DataFlavor.stringFlavor);
                    if (s == null) {
                        return;
                    }
                    Rectangle r = null;
                    try {
                        r = Settings.region(s);
                    } catch (Exception e) {
                    }
                    if (r == null) {
                        return;
                    }
                    setRegion(r);
                    // this handles the synthetic mouse drags created by autoscrolling
                    scrollRectToVisible(getRegion());
                } catch (UnsupportedFlavorException e) {
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, "unable to read from clipbpard", e);
                    getToolkit().beep();
                }
            }
        }

        private boolean restrictedRegion = true;

        public boolean isRestrictedRegion() {
            return restrictedRegion;
        }

        public void setRestrictedRegion(boolean restrictedRegion) {
            this.restrictedRegion = restrictedRegion;
        }

        public boolean isResizable() {
            return resizable;
        }

        public void setResizable(boolean resizable) {
            this.resizable = resizable;
        }

        public boolean isClickToDefineEnabled() {
            return clickToDefineEnabled;
        }

        public void setClickToDefineEnabled(boolean clickToDefineEnabled) {
            this.clickToDefineEnabled = clickToDefineEnabled;
        }

        /**
         * @return the shape
         */
        public PageShape getPageShape() {
            return shape;
        }

        /**
         * @param shape the shape to set
         */
        public void setPageShape(PageShape shape) {
            this.shape = shape;
            repaint();
        }

        public static final String ZOOM_CHANGED_PROPERTY = "zoom";
    }

    public interface RegionChangeListener extends EventListener {

        public void regionChanged(Object source, Rectangle region);
    }

    private boolean isCloseMode = false;

    public boolean isCloseMode() {
        return isCloseMode;
    }

    public void setCloseMode(boolean isCloseMode) {
        if (this.isCloseMode != isCloseMode) {
            this.isCloseMode = isCloseMode;
            okBtn.setText(isCloseMode ? string("close") : string("select"));
        }
    }

    @Override
    public void dispose() {
        Settings.getUser().storeWindowSettings("draw-region", this);
        super.dispose();
    }
}
