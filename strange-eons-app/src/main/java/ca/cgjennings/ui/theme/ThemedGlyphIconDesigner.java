package ca.cgjennings.ui.theme;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.DocumentEventAdapter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * A simple interactive tool to aid in designing {@link ThemedGlyphIcon}s. You
 * can create these directly via that class, or by calling
 * {@link resources.ResourceKit#getIcon(java.lang.String)} with a {@code gly:}
 * pseudo-URL.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ThemedGlyphIconDesigner {

    public static void main(String[] argv) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel() {
                    @Override
                    public Icon getDisabledIcon(JComponent c, Icon i) {
                        return i;
                    }
                });
            } catch (UnsupportedLookAndFeelException ulaf) {
            }
            JFrame f = new JFrame("ThemedGlyphIcon designer");
            final JLabel label = new JLabel(new ThemedGlyphIcon("gly:F0198"));
            JTextField tf = new JTextField("gly:F0198");
            tf.setDragEnabled(true);
            final DocumentEventAdapter tfHandler = new DocumentEventAdapter() {
                private int last, lastHandled;

                @Override
                public void changedUpdate(DocumentEvent e) {
                    ++last;
                    SwingUtilities.invokeLater(() -> {
                        if (lastHandled != last) {
                            lastHandled = last;
                            try {
                                String desc = tf.getText().trim();
                                if (!desc.isEmpty()) {
                                    Icon icon;
                                    if ("zoomed".equals(label.getName())) {
                                        icon = new ZoomedGlyphIcon(desc);
                                    } else {
                                        icon = new ThemedGlyphIcon(desc);
                                    }
                                    label.setIcon(icon);
                                }
                            } catch (Exception ex) {
                                StrangeEons.log.log(Level.SEVERE, "uncaught exception during parse", ex);
                            }
                        }
                    });
                }
            };
            tf.getDocument().addDocumentListener(tfHandler);

            Color dark = new Color(0x323232);
            Color light = new Color(0xe0e0e0);

            final JComponent cp = (JComponent) f.getContentPane();
            label.setBackground(light);
            label.setOpaque(true);
            cp.add(label);
            cp.add(tf, BorderLayout.NORTH);
            cp.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    Icon icon = label.getIcon();
                    if (icon == null) {
                        return;
                    }
                    final int clicks = e.getWheelRotation();
                    if (clicks < 0) {
                        if (!"zoomed".equals(label.getName())) {
                            label.setName("zoomed");
                            tfHandler.changedUpdate(null);
                        }
                    } else if (clicks > 0) {
                        if ("zoomed".equals(label.getName())) {
                            label.setName("");
                            tfHandler.changedUpdate(null);
                        }
                    }
                }
            });
            cp.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // left click to toggle light/dark
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        label.setBackground(label.getBackground() == light ? dark : light);
                    } // right click to toggle enabled/disabled
                    else if (e.getButton() == MouseEvent.BUTTON3) {
                        label.setEnabled(!label.isEnabled());
                    } // middle button to save as a PNG
                    else if (e.getButton() == MouseEvent.BUTTON2) {
                        ThemedIcon icon = (ThemedGlyphIcon) label.getIcon();
                        final int W = icon.getIconWidth();
                        final int H = icon.getIconHeight();
                        BufferedImage im = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = im.createGraphics();
                        try {
                            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            icon.paintIcon(null, g, 0, 0);
                        } finally {
                            g.dispose();
                        }
                        try {
                            File output = File.createTempFile("glyph-icon-", ".png");
                            ImageIO.write(im, "png", output);
                            System.out.println(output.getAbsolutePath());
                        } catch (IOException ex) {
                            ex.printStackTrace(System.out);
                        }
                    }
                }
            });
            JLabel key = new JLabel(
                    "<html><pre>gly:CP[!%*][,FG[,BG]][@[Z][,+|-ADJ[,DX[,DY]]]][;...]<br><hr>"
                    + "CP        = code point or single char<br>"
                    + "! % *     = mirror L-to-R / T-to-B / rotate 45 deg<br>"
                    + "FG/BG     = hex color or rR oO yY gG bB iI vV pP wW tT cC kK 0 1<br>"
                    + "Z         = size t s S m M l L g<br>"
                    + "ADJ,DX,DY = size adjust, x shift, y shift<br>"
                    + ";...      = additional layers<hr>Left click = light/dark, right click = enable/disable, scroll = zoom"
            );
            key.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
            cp.add(key, BorderLayout.SOUTH);
            // Create a JList
            DefaultListModel<String> listModel = new DefaultListModel<>();
            JList<String> iconList = new JList<>(listModel);
            iconList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
            iconList.setVisibleRowCount(-1);
            iconList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    label.setIcon(new ThemedGlyphIcon(value + ",0"));
                    return label;
                }
            });
            iconList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        int index = iconList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            String selectedItem = listModel.getElementAt(index);
                            tf.replaceSelection(selectedItem);
                            int pos = tf.getCaretPosition();
                            tf.select(pos - selectedItem.length(), pos);
                        }
                    }
                }
            });
            // Add available icons to the list
            Arrays.sort(DEPRECATED);
            Font glyphFont = ThemedGlyphIcon.getDefaultFont();
            for (int i = 0xf0000; i < 0xf5000; ++i) {
                if (glyphFont.canDisplay(i) && Arrays.binarySearch(DEPRECATED, i) < 0) {
                    listModel.addElement(Integer.toHexString(i));
                }
            }
            JScrollPane scrollPane = new JScrollPane(iconList);
            scrollPane.setPreferredSize(new Dimension(225, 0));
            cp.add(scrollPane, BorderLayout.EAST);

            f.setSize(550, 500);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setLocationByPlatform(true);
            f.setVisible(true);
        });
    }
    
    private static class ZoomedGlyphIcon implements Icon {
        private ThemedGlyphIcon tgi;
        
        public ZoomedGlyphIcon(String desc) {
            tgi = new ThemedGlyphIcon(desc).derive(256);
        }
        
        @Override
        public void paintIcon(Component c, Graphics g1, int x, int y) {
            Graphics2D g = (Graphics2D) g1;
            g.setColor(new Color(0x77000000, true));
            for (int i=1; i<24; ++i) {
                int xy = (int) (256f / 24f * (float) i + 0.5f);
                g.drawLine(xy + x, y, xy + x, 255 + y);
                g.drawLine(0 + x, xy + y, 255 + x, xy + y);
            }
            g.setColor(Color.BLACK);
            g.drawRect(x, y, 255, 255);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 5f}, 0f));
            g.drawRect(x, y, 255, 255);
            tgi.paintIcon(c, g, x, y);
        }
        @Override
        public int getIconWidth() {
            return 256;
        }
        @Override
        public int getIconHeight() {
            return 256;
        }
    }

    private static final int[] DEPRECATED = new int[]{
        0xf0032, 0xf0034, 0xf06b2, 0xf06bf, 0xf109a, 0xf100f, 0xf0035, 0xf0036, 0xf0038, 0xf0037, 0xf0039,
        0xf08c7, 0xf0b5b, 0xf0804, 0xf0e0f, 0xf0a25, 0xf00a8, 0xf0813, 0xf00a9, 0xf00ab, 0xf06c6, 0xf00d4,
        0xf12e7, 0xf111a, 0xf0175, 0xf0958, 0xf0d6b, 0xf0959, 0xf08da, 0xf0d6e, 0xf01c7, 0xf1237, 0xf01d2,
        0xf0a41, 0xf0868, 0xf06b3, 0xf0aae, 0xf01e3, 0xf01e4, 0xf1024, 0xf0b30, 0xf06b4, 0xf0c7a, 0xf086a,
        0xf0204, 0xf020c, 0xf07dd, 0xf020e, 0xf0b31, 0xf08db, 0xf0967, 0xf0239, 0xf024e, 0xf003a, 0xf08e0,
        0xf0e43, 0xf08e8, 0xf02a2, 0xf02a4, 0xf0ba0, 0xf02ab, 0xf02ac, 0xf0ba1, 0xf0d7b, 0xf02ad, 0xf0c87,
        0xf07cc, 0xf07cd, 0xf02ae, 0xf02af, 0xf02b0, 0xf02b1, 0xf02b2, 0xf02b3, 0xf02c0, 0xf11f6, 0xf1362,
        0xf02b6, 0xf02b7, 0xf096c, 0xf02b8, 0xf02c9, 0xf06dc, 0xf09f6, 0xf05f5, 0xf1048, 0xf02b9, 0xf02bc,
        0xf02bd, 0xf0eb9, 0xf09f7, 0xf0c88, 0xf02bf, 0xf0877, 0xf07d0, 0xf0829, 0xf0744, 0xf02fe, 0xf12e8,
        0xf0dd5, 0xf0303, 0xf087d, 0xf0304, 0xf0745, 0xf0314, 0xf10fe, 0xf0671, 0xf0672, 0xf031b, 0xf031c,
        0xf121a, 0xf07d3, 0xf0c92, 0xf031d, 0xf0b37, 0xf031e, 0xf1219, 0xf08b1, 0xf0354, 0xf0f5b, 0xf031f,
        0xf0320, 0xf07d4, 0xf0d2d, 0xf0acf, 0xf1617, 0xf06e5, 0xf06e6, 0xf0673, 0xf0ad0, 0xf0446, 0xf033b,
        0xf033d, 0xf08ed, 0xf0a61, 0xf0346, 0xf160a, 0xf0baa, 0xf0ad1, 0xf0986, 0xf0357, 0xf0629, 0xf0372,
        0xf138e, 0xf0805, 0xf0fd5, 0xf00a4, 0xf0988, 0xf01e9, 0xf138f, 0xf0300, 0xf03c6, 0xf03ca, 0xf0747,
        0xf0d22, 0xf1390, 0xf1391, 0xf02bb, 0xf0610, 0xf0a1e, 0xf05b3, 0xf0a21, 0xf1392, 0xf05b9, 0xf05ba,
        0xf074b, 0xf0a22, 0xf074c, 0xf074d, 0xf074e, 0xf074f, 0xf0e6f, 0xf05bb, 0xf0e70, 0xf08f1, 0xf0373,
        0xf0880, 0xf0746, 0xf0396, 0xf07e1, 0xf05ab, 0xf072d, 0xf1105, 0xf0399, 0xf06f7, 0xf1106, 0xf12e9,
        0xf03c5, 0xf0881, 0xf0bae, 0xf03cd, 0xf03ce, 0xf0b43, 0xf03db, 0xf0882, 0xf0df1, 0xf0407, 0xf06ba,
        0xf0a09, 0xf0421, 0xf0999, 0xf0605, 0xf0d29, 0xf0708, 0xf044d, 0xf111b, 0xf0bc0, 0xf088e, 0xf07ec,
        0xf1316, 0xf131d, 0xf0adf, 0xf04af, 0xf04b0, 0xf04b1, 0xf04b6, 0xf0414, 0xf04c0, 0xf04c7, 0xf060b,
        0xf04cc, 0xf0359, 0xf04d3, 0xf0721, 0xf0ae6, 0xf13ff, 0xf0500, 0xf1062, 0xf0532, 0xf0543, 0xf0544,
        0xf0bda, 0xf0548, 0xf0549, 0xf0ed0, 0xf06af, 0xf09b1, 0xf0577, 0xf057c, 0xf0844, 0xf0e6d, 0xf0bde,
        0xf072b, 0xf1248, 0xf0611, 0xf05a3, 0xf05ac, 0xf05b4, 0xf0845, 0xf07ff, 0xf0b4f, 0xf05c3, 0xf0848,
        0xf0847, 0xf0d40, 0xf0448, 0xf0aea, 0xf0aeb, 0xf0d41
    };
}
