package ca.cgjennings.ui.table;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Chris
 */
@SuppressWarnings("serial")
public class IconRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        DefaultTableCellRenderer c = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        c.setHorizontalAlignment(JLabel.CENTER);
        c.setIcon((Icon) value);
        return c;
    }
}
