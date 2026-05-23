package ca.cgjennings.ui.table;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * A Nimbus-friendly replacement for the default boolean renderer.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class BooleanRenderer extends JCheckBox implements TableCellRenderer {

    private DefaultTableCellRenderer tcr = new DefaultTableCellRenderer();
    private boolean disableIfNotEditable;

    public BooleanRenderer() {
        this(false);
    }

    public BooleanRenderer(boolean disableIfNotEditable) {
        super();
        this.disableIfNotEditable = disableIfNotEditable;
        setHorizontalAlignment(JLabel.CENTER);
        setBorderPainted(true);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        tcr.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        setSelected((value != null && ((Boolean) value)));
        setForeground(convertNimbusColor(tcr.getForeground()));
        setBackground(convertNimbusColor(tcr.getBackground()));
        setBorder(tcr.getBorder());
        if (disableIfNotEditable) {
            setEnabled(table.getModel().isCellEditable(table.convertRowIndexToModel(row), table.convertColumnIndexToModel(column)));
        }
        return this;
    }

    private Color convertNimbusColor(Color c) {
        for (int i = 0; i < cache.length; i += 2) {
            if (c == cache[i]) {
                return cache[i + 1];
            }
        }
        Color out = new Color(c.getRed(), c.getGreen(), c.getBlue());
        if (!full) {
            for (int i = 0; i < cache.length; i += 2) {
                if (cache[i] == null) {
                    cache[i] = c;
                    cache[i + 1] = out;
                    return out;
                }
            }
            full = true;
        }
        return out;
    }
    private Color[] cache = new Color[2 * 2]; // 2*number of cache entries
    private boolean full = false;
}
