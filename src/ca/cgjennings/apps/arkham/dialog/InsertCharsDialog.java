package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.platform.AgnosticDialog;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * Dialog for inserting Unicode symbols.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public final class InsertCharsDialog extends javax.swing.JDialog implements AgnosticDialog {

    private static final String KEY_FULL_UNICODE = "insert-chars-full-unicode";

    /**
     * Creates new form InsertCharsDialog
     */
    public InsertCharsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        rowMap = buildDynamicRowMap();
        setLimitedToBMP(false);
        initComponents();

        final Font headerFont = new Font(Font.MONOSPACED, Font.PLAIN, charTable.getFont().getSize() + 2);
        charTable.getTableHeader().setFont(headerFont);

        AbstractGameComponentEditor.localizeComboBoxLabels(showUnicodeCombo, null);
        unicodeNameLabel.setBackground(Color.LIGHT_GRAY);
        unicodeNameLabel.setOpaque(true);

        getRootPane().setDefaultButton(okBtn);
        PlatformSupport.makeAgnosticDialog(this, okBtn, cancelBtn);

        if (!Settings.getUser().applyWindowSettings("insert-chars", this)) {
            setLocationRelativeTo(parent);
        }

        gridBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, charTable.getGridColor());
        charTable.setDefaultRenderer(Object.class, new PageRenderer());
        charTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        charTable.setShowHorizontalLines(false);
        charTable.setShowVerticalLines(false);
        ListSelectionListener selectionListener = (ListSelectionEvent e) -> {
            int col = charTable.getSelectedColumn();
            int row = charTable.getSelectedRow();
            if (col >= 1 && row >= 0) {
                int cp = getCodePointForLocation(row, col);
                if (!Character.isValidCodePoint(cp)) {
                    return;
                }

                String character = codePointToString(cp);
                Font font = getFontForCodepoint(cp, true);
                if (font != null) {
                    glyphViewer.setText(character);
                    glyphViewer.setFont(font);
                } else {
                    glyphViewer.setText(" ");
                    glyphViewer.setFont(getFallbackFonts()[0]);
                }

                String name1 = getUnicodeName(cp);
                if (name1 != null) {
                    unicodeNameLabel.setText(name1);
                } else {
                    unicodeNameLabel.setText(" ");
                }
            }
        };
        charTable.getSelectionModel().addListSelectionListener(selectionListener);
        charTable.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);
        charTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int col = charTable.getSelectedColumn();
                int row = charTable.getSelectedRow();
                int maxcol = charTable.getColumnCount() - 1;
                int maxrow = charTable.getRowCount() - 1;
                if (col < 0) {
                    col = 1;
                }
                if (row < 0) {
                    row = 0;
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_KP_RIGHT:
                        if (col == maxcol) {
                            ++row;
                            col = 1;
                            if (row > maxrow) {
                                row = 0;
                            }
                            charTable.changeSelection(row, col, false, false);
                            e.consume();
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_KP_LEFT:
                        if (col < 2) {
                            col = maxcol;
                            --row;
                            if (row < 0) {
                                row = maxrow;
                            }
                            charTable.changeSelection(row, col, false, false);
                            e.consume();
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_KP_DOWN:
                        if (row == maxrow) {
                            charTable.changeSelection(0, col, false, false);
                            e.consume();
                        }
                        break;
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_KP_UP:
                        if (row == 0) {
                            charTable.changeSelection(maxrow, col, false, false);
                            e.consume();
                        }
                        break;
                    case KeyEvent.VK_HOME:
                        charTable.changeSelection(0, 1, false, false);
                        e.consume();
                        break;
                    case KeyEvent.VK_END:
                        charTable.changeSelection(maxrow, maxcol, false, false);
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER: {
                        Font f = getTableFont();
                        int cp = getCodePointForLocation(row, col);
                        int delta=+1, switchAt=Character.MAX_CODE_POINT+1, switchTo=0;
                        if(e.isShiftDown()) {
                            switchTo = switchAt-1; switchAt = -1; delta = -1;
                        }
                        int i;
                        for (i = cp + delta; i != cp; i += delta) {
                            if (i == switchAt) {
                                i = switchTo;
                            }
                            if (Character.isDefined(i) && f.canDisplay(i)) {
                                break;
                            }
                        }
                        if (i == cp) {
                            UIManager.getLookAndFeel().provideErrorFeedback(charTable);
                        } else {
                            selectNearestValidRow(i);
                        }
                        e.consume();
                        return;
                    }
                    case KeyEvent.VK_SPACE:
                        int cp = getCodePointForLocation(row, col);
                        insertField.setText(insertField.getText() + codePointToString(cp));
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                    case KeyEvent.VK_DELETE:
                        String t = insertField.getText();
                        if (!t.isEmpty()) {
                            insertField.setText(t.substring(0, t.length() - 1));
                        }
                        break;
                }
            }
        });

        setTableFont(new Font(ResourceKit.getBodyFamily(), Font.PLAIN, 28));
        showUnicodeCombo.setSelectedIndex(Settings.getShared().getYesNo(KEY_FULL_UNICODE) ? 1 : 0);
        charTable.requestFocus();
    }

    public int getCodePointForLocation(int row, int col) {
        return rowMap[row] + Math.max(1, col) - 1; // -1 for row header
    }

    public int getRowForCodePoint(int cp) {
        return Arrays.binarySearch(rowMap, cp & 0xfffffff0);
    }

    public static String codePointToString(int cp) {
        cpBuff[0] = cp;
        return new String(cpBuff, 0, 1);
    }
    private static final int[] cpBuff = new int[1];

    public String getUnicodeName(int cp) {
        String name = Character.getName(cp);
        if (name == null) {
            name = "UNASSIGNED";
        }
        return name;
    }

    private static Font[] fallbackFonts = new Font[]{
        new Font(Font.SANS_SERIF, Font.PLAIN, 24),
        new Font(Font.SERIF, Font.PLAIN, 24)
    };
    private static final int LAST_FALLBACK_FOR_CONTROL_CHARS = fallbackFonts.length;

    public synchronized static void addFallbackFont(Font lrf) {
        if (fallbackFonts.length >= 16) {
            return; // safety limit
        }
        lrf = lrf.deriveFont(24f);
        Font[] extendedFonts = Arrays.copyOf(fallbackFonts, fallbackFonts.length + 1);
        extendedFonts[extendedFonts.length - 1] = lrf;
        fallbackFonts = extendedFonts;
    }

    private synchronized static Font[] getFallbackFonts() {
        return fallbackFonts;
    }

    protected Font getFontForCodepoint(int cp, boolean forGlyphViewer) {
        Font f = charTable.getFont();
        if (f.canDisplay(cp)) {
            return f;
        }

        Font[] fallbacks = getFallbackFonts();
        int lastFallback = fallbacks.length;
        if (Character.getType(cp) == Character.CONTROL) {
            lastFallback = LAST_FALLBACK_FOR_CONTROL_CHARS;
        }

        for (int i = 0; i < lastFallback; ++i) {
            if (fallbacks[i].canDisplay(cp)) {
                return fallbacks[i];
            }
        }
        return null;
    }

    public static JDialog createFontViewer(Font f) {
        InsertCharsDialog d = new InsertCharsDialog(StrangeEons.getWindow(), false);
        d.isFontViewer = true;
        d.instructionLabel.setVisible(false);
        PlatformSupport.getAgnosticOK(true, d.okBtn, d.cancelBtn).setText(string("copy"));
        PlatformSupport.getAgnosticCancel(true, d.okBtn, d.cancelBtn).setText(string("close"));
        d.insertLabel.setText(string("copy"));
        d.setTableFont(f.deriveFont(28f));
        d.pack();
        d.setTitle(string("icd-l-view-only", f.getFontName(Locale.getDefault())));
        return d;
    }
    private boolean isFontViewer;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        charTable = new javax.swing.JTable();
        instructionLabel = new javax.swing.JLabel();
        okBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        insertLabel = new javax.swing.JLabel();
        unicodeNameLabel = new javax.swing.JLabel();
        findBtn = new javax.swing.JButton();
        glyphViewer = new ca.cgjennings.ui.JGlyphViewer();
        jScrollPane2 = new javax.swing.JScrollPane();
        insertField = new javax.swing.JTextArea() {
            public void paintComponent( Graphics g ) {
                ((Graphics2D) g).setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
                super.paintComponent( g );
            }
        };
        jLabel1 = new javax.swing.JLabel();
        showUnicodeCombo = new javax.swing.JComboBox<>();
        insertSizeField = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        findField = new ca.cgjennings.ui.JFilterField();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Insert Characters");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jScrollPane1.setBackground(java.awt.Color.gray);
        jScrollPane1.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.gray), javax.swing.BorderFactory.createMatteBorder(2, 2, 0, 2, java.awt.Color.gray)));

        charTable.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        charTable.setModel(new UnicodeTableModel());
        charTable.setCellSelectionEnabled(true);
        charTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        charTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                charTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(charTable);

        instructionLabel.setText(string("icd-instructions")); // NOI18N

        okBtn.setMnemonic('i');
        okBtn.setText(string("icd-ok")); // NOI18N

        cancelBtn.setText(string("cancel")); // NOI18N

        insertLabel.setText(string("icd-l-insert")); // NOI18N

        unicodeNameLabel.setBorder( javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 2, java.awt.Color.gray), javax.swing.BorderFactory.createCompoundBorder(new ca.cgjennings.ui.ArcBorder( 3, Color.GRAY, 2 ), javax.swing.BorderFactory.createEmptyBorder(0, 16, 0, 16))) );
        unicodeNameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        unicodeNameLabel.setOpaque(true);

        findBtn.setFont(findBtn.getFont().deriveFont(findBtn.getFont().getSize()-1f));
        findBtn.setMnemonic('f');
        findBtn.setText(string("icd-b-find")); // NOI18N
        findBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findBtnActionPerformed(evt);
            }
        });

        glyphViewer.setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        glyphViewer.setOpaque(true);

        insertField.setColumns(20);
        insertField.setLineWrap(true);
        insertField.setTabSize(4);
        insertField.setWrapStyleWord(true);
        jScrollPane2.setViewportView(insertField);

        jLabel1.setText(string( "icd-l-show" )); // NOI18N

        showUnicodeCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "icd-cb-show0", "icd-cb-show1" }));
        showUnicodeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showUnicodeComboActionPerformed(evt);
            }
        });

        insertSizeField.setFont(insertSizeField.getFont().deriveFont(insertSizeField.getFont().getSize()-2f));
        insertSizeField.setModel(new javax.swing.SpinnerNumberModel(24, 6, 144, 1));
        insertSizeField.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                insertSizeFieldStateChanged(evt);
            }
        });

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getSize()-2f));
        jLabel2.setText(string("ffd-l-size")); // NOI18N

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getSize()-2f));
        jLabel3.setText(string("ffd-cb-size-0")); // NOI18N

        findField.setLabel(string("icd-l-find")); // NOI18N
        findField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findFieldActionPerformed(evt);
            }
        });

        jLabel4.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.gray));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 670, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(instructionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 368, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(showUnicodeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(glyphViewer, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                                .addGap(10, 10, 10))
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE))
                        .addGap(0, 0, 0)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(unicodeNameLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(okBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cancelBtn))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(findField, javax.swing.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(findBtn))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(insertLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 344, Short.MAX_VALUE)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(insertSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(1, 1, 1)
                                .addComponent(jLabel3))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 449, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(instructionLabel)
                    .addComponent(showUnicodeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(unicodeNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(findBtn)
                            .addComponent(findField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                            .addComponent(insertLabel)
                            .addComponent(jLabel2)
                            .addComponent(insertSizeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addGap(1, 1, 1)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cancelBtn)
                            .addComponent(okBtn)))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(glyphViewer, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        Settings.getUser().storeWindowSettings("insert-chars", this);
        dispose();
    }//GEN-LAST:event_formWindowClosing

    private boolean limitToBMP = true;
    private int maxChar;
    private int rowCount;

    public void setLimitedToBMP(boolean limit) {
        limitToBMP = limit;
        maxChar = limit ? 0xffff : Character.MAX_CODE_POINT;
        rowCount = getRowForCodePoint(maxChar) + 1;
        if (charTable != null) {
            ((UnicodeTableModel) charTable.getModel()).fireTableChanged(new TableModelEvent(charTable.getModel()));
        }
    }

    public boolean isLimitedToBMP() {
        return limitToBMP;
    }

    private void findBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findBtnActionPerformed
        String fragment = findField.getText().toUpperCase(Locale.US).trim();

        // check if it looks like a hex code point
        if (fragment.matches("((U\\+)|(0X))[0-9A-F]+$")) {
            try {
                fragment = fragment.replaceAll("[^0-9A-F]", "");
                int cp = Integer.parseUnsignedInt(fragment, 16);
                if (Character.isValidCodePoint(cp)) {
                    if (cp > maxChar) {
                        showUnicodeCombo.setSelectedIndex(showUnicodeCombo.getItemCount() - 1);
                    }
                    selectNearestValidRow(cp);
                }
            } catch (Exception e) {
            }
            UIManager.getLookAndFeel().provideErrorFeedback(findField);
            return;
        }

        if (!fragment.isEmpty()) {
            int col = Math.max(charTable.getSelectedColumn(), 1);
            int row = Math.max(charTable.getSelectedRow(), 0);

            // start search from the character after the selected character
            int testChar = getCodePointForLocation(row, col) + 1;
            if (testChar > maxChar) {
                testChar = 0;
            }

            // search one full revolution or until a match is found
            for (int i = 0; i <= maxChar; ++i) {
                String name = getUnicodeName(testChar);
                if (name != null) {
                    if (name.contains(fragment)) {
                        int targetRow = getRowForCodePoint(testChar);
                        if(targetRow >= 0) {
                            selectNearestValidRow(testChar);
                            return;
                        }
                    }
                }
                ++testChar;
                if (testChar > maxChar) {
                    testChar = 0;
                }
            }
            // no match was ever found
        }
        UIManager.getLookAndFeel().provideErrorFeedback(findField);
    }//GEN-LAST:event_findBtnActionPerformed
    private String selectedText;
    
    /**
     * If the code point is in the table, it is selected. Otherwise the nearest
     * valid row is "selected" by selecting column 0 so that the view scrolls
     * to the row.
     * @param codePoint the code point to select or scroll near
     */
    private void selectNearestValidRow(int codePoint) {
        int col;
        int row = getRowForCodePoint(codePoint);
        if (row >= 0) {
            col = (codePoint & 0xf) + 1;
            charTable.changeSelection(row, col, false, false);
            return;
        } else {
            // select nearest available row
            row = Math.max(0, Math.min(rowMap.length - 1, (-(row) - 1)));
            col = 0;
        }
        charTable.clearSelection();
        charTable.changeSelection(row, col, false, false);
        charTable.scrollRectToVisible(charTable.getCellRect(row, col, true));
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            selectedText = null;
            insertField.setText("");
        }
        super.setVisible(true);
    }

    public String getSelectedText() {
        return selectedText;
    }

    public void setTableFont(Font font) {
        // since these attributes are normally enabled in the markup system,
        // we enable them in the sample text field as well
        font = ResourceKit.enableKerningAndLigatures(font);

        charTable.setFont(font);
        charTable.setRowHeight(charTable.getFontMetrics(font).getHeight() + 4);
        glyphViewer.setFont(font.deriveFont(72f));
        // match the insert field's font to the selected font, then pretend
        // that the spinner was pressed to set the size
        insertField.setFont(font);
        insertSizeFieldStateChanged(null);

        FontMetrics fm = getFontMetrics(font);
        int maxWidth = 0;
        for (int i = 0; i <= 0xffff; ++i) { // don't test all chars
            int width = fm.charWidth((char) i);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        maxWidth += 4;

        TableColumnModel model = charTable.getColumnModel();
        for (int i = 1; i <= 16; ++i) {
            model.getColumn(i).setPreferredWidth(maxWidth);
        }

        int width = getFontMetrics(charTable.getTableHeader().getFont()).stringWidth("00000000");
        model.getColumn(0).setPreferredWidth(width);
        model.getColumn(0).setMinWidth(width);
        model.getColumn(0).setResizable(false);
    }

    public Font getTableFont() {
        return charTable.getFont();
    }

    private void charTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_charTableMouseClicked
        int row = charTable.getSelectedRow();
        int col = charTable.getSelectedColumn();
        if (row < 0 || col < 0) {
            return;
        }
        if (col < 1) {
            col = 1;
            charTable.changeSelection(row, col, false, false);
        }

        int cp = getCodePointForLocation(row, col);
        int charType = Character.getType(cp);
        if (evt.isShiftDown()
                || (charType != Character.UNASSIGNED
                && charType != Character.CONTROL
                && charTable.getFont().canDisplay(cp))) {
            String text = insertField.getText() + codePointToString(cp);
            insertField.setText(text);
        }
    }//GEN-LAST:event_charTableMouseClicked

	private void showUnicodeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showUnicodeComboActionPerformed
            setLimitedToBMP(showUnicodeCombo.getSelectedIndex() == 0);
	}//GEN-LAST:event_showUnicodeComboActionPerformed

	private void insertSizeFieldStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_insertSizeFieldStateChanged
            float size = ((Number) insertSizeField.getValue()).floatValue();
            insertField.setFont(insertField.getFont().deriveFont(size));
	}//GEN-LAST:event_insertSizeFieldStateChanged

	private void findFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findFieldActionPerformed
            findBtnActionPerformed(evt);
	}//GEN-LAST:event_findFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JTable charTable;
    private javax.swing.JButton findBtn;
    private ca.cgjennings.ui.JFilterField findField;
    private ca.cgjennings.ui.JGlyphViewer glyphViewer;
    private javax.swing.JTextArea insertField;
    private javax.swing.JLabel insertLabel;
    private javax.swing.JSpinner insertSizeField;
    private javax.swing.JLabel instructionLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JButton okBtn;
    private javax.swing.JComboBox<String> showUnicodeCombo;
    private javax.swing.JLabel unicodeNameLabel;
    // End of variables declaration//GEN-END:variables

    private class UnicodeTableModel extends AbstractTableModel {

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                int rowBase = getCodePointForLocation(rowIndex, 1);
                return Integer.toHexString(rowBase >> 4) + "x ";
            } else {
                return codePointToString(getCodePointForLocation(rowIndex, columnIndex));
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public int getRowCount() {
            return rowCount;
        }

        @Override
        public int getColumnCount() {
            return 17;
        }

        @Override
        public String getColumnName(int column) {
            if (column > 0) {
                char digit = (char) ('0' - 1 + column);
                if (column >= 11) {
                    digit = (char) ('a' - 11 + column);
                }
                return String.valueOf(digit);
            }
            return "";
        }

    }

    private Border gridBorder;

    class PageRenderer extends DefaultTableCellRenderer {

        public PageRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (column == 0) {
                JTableHeader header = table.getTableHeader();
                setBackground(header.getBackground());
                setForeground(header.getForeground());
                setFont(header.getFont());
                setHorizontalAlignment(JLabel.RIGHT);
                setText(value.toString()); // actually the row offset
                setEnabled(true);
                setToolTipText(null);
            } else {
                final int cp = getCodePointForLocation(row, column);

                final boolean charIsInFont = table.getFont().canDisplay(cp);

                final int charType = Character.getType(cp);

                final boolean isMark = (charType == Character.COMBINING_SPACING_MARK)
                        || (charType == Character.ENCLOSING_MARK)
                        || (charType == Character.NON_SPACING_MARK);

                // draw combining characters over a zero-width space
                String charValue;
                if (charIsInFont && isMark) {
                    charValue = "\u200b" + value;
                } else {
                    charValue = value.toString();
                }

                isSelected |= hasFocus;

                Color foreground = table.getForeground();
                Color background = table.getBackground();

                Font font = table.getFont();
                boolean enable = true;

                if (charType == Character.UNASSIGNED) {
                    foreground = FG_SPECIAL;
                    background = BG_UNASSIGNED;
                    enable = false;
                    charValue = "";
                } else if (charType == Character.CONTROL) {
                    foreground = FG_SPECIAL;
                    background = BG_CONTROL_CODE;
                    enable = false;
                    charValue = "";
                } else if (!charIsInFont) {
                    foreground = FG_SPECIAL;
                    background = BG_INVALID;
                    enable = false;

                    font = getFontForCodepoint(cp, false);
                    if (font == null) {
                        charValue = "";
                        font = getFallbackFonts()[0];
                    }
                }

                setFont(font);
                setBackground(isSelected ? table.getSelectionBackground() : background);
                setForeground(isSelected && enable ? table.getSelectionForeground() : foreground);
                setHorizontalAlignment(JLabel.CENTER);
                setText(charValue);
                setToolTipText(composeToolTip(value.toString(), cp));
            }
            setBorder(gridBorder);
            return this;
        }

        private String composeToolTip(String character, int codePoint) {
            if (codePoint == '<') {
                character = "&lt;";
            } else if (codePoint == '&') {
                character = "&amp;";
            }

            String name = getUnicodeName(codePoint);
            name = name == null ? "" : "<br><b>" + name;
            String format = codePoint <= 0xffff ? "<html><tt>u+%04x  </tt>%s%s" : "<html><tt>u+%06x   </tt> %s%s";

            return String.format(format, codePoint, character, name);
        }

        private final Color FG_SPECIAL = Color.WHITE;
        private final Color BG_CONTROL_CODE = new Color(0xa5d6a7);
        private final Color BG_UNASSIGNED = new Color(0x90a4ae);
        private final Color BG_INVALID = new Color(0xef9a9a);
    }

    @Override
    public void handleOKAction(ActionEvent e) {
        if (isFontViewer) {
            StringSelection s = new StringSelection(insertField.getText());
            Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            c.setContents(s, s);
            c = Toolkit.getDefaultToolkit().getSystemSelection();
            if (c != null) {
                c.setContents(s, s);
            }
        } else {
            selectedText = insertField.getText();
            dispose();
        }
    }

    @Override
    public void handleCancelAction(ActionEvent e) {
        dispose();
    }

    @Override
    public void dispose() {
        Settings.getUser().set(KEY_FULL_UNICODE, showUnicodeCombo.getSelectedIndex() == 0 ? "no" : "yes");
        super.dispose();
    }

    private int[] rowMap;

    /**
     * Since the set of assigned characters (and thus the rows we should
     * display) can change from version to version, we use a searchable table of
     * the rows with at least one assigned character and use it to map between
     * table positions and code points.
     */
    private static int[] buildDynamicRowMap() {
        int[] table = null;
        for (int pass = 0; pass < 2; ++pass) {
            int rows = 0;
            for (int cp = 0; cp < 0x10ffff; cp += 16) {
                // skip planes 3-13 for speed, not expected to be assigned soon
                if (cp == 0x30000) {
                    cp = 0xf0000;
                }

                boolean hasAssignedChar = false;
                for (int i = 0; i < 16; ++i) {
                    if (Character.getType(cp + i) != Character.UNASSIGNED) {
                        hasAssignedChar = true;
                        break;
                    }
                }
                if (hasAssignedChar) {
                    if (table != null) {
                        table[rows] = cp;
                    }
                    ++rows;
                }
            }
            if (table == null) {
                table = new int[rows];
            }
        }
        return table;
    }
}
