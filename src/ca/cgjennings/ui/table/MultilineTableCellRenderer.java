package ca.cgjennings.ui.table;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

/**
 * A <code>JTable</code> cell renderer that supports mutiple lines.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class MultilineTableCellRenderer extends JTextArea implements TableCellRenderer {

//	private boolean autoRowHeight = false;
//
//	public boolean isAutomaticRowHeight() {
//		return autoRowHeight;
//	}
//
//	public void setAutomaticRowHeight( boolean autoRowHeight ) {
//		this.autoRowHeight = autoRowHeight;
//	}
    public MultilineTableCellRenderer() {
        setOpaque(false);
        setWrapStyleWord(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            if (table.isCellEditable(row, column)) {
                setForeground(UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            setBorder(unfocusedBorder);
        }
        setFont(table.getFont());
        setText(value == null ? "" : value.toString());
//		if( autoRowHeight ) {
//			int height = getPreferredSize().height;
//			if( table.getRowHeight( row ) != height ) {
//				table.setRowHeight( row, height );
//			}
//		}
        return this;
    }

    private final EmptyBorder unfocusedBorder = new EmptyBorder(1, 2, 1, 2);
}
