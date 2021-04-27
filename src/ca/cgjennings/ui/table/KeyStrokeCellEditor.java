package ca.cgjennings.ui.table;

import ca.cgjennings.ui.JKeyStrokeField;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class KeyStrokeCellEditor extends DefaultCellEditor {

    public KeyStrokeCellEditor() {
        super((JTextField) null);
        editorComponent = field;
        field.setEditable(false);

        final Color background = new Color(0xfffe99);

        field.setBorder(BorderFactory.createEmptyBorder());
        field.setForeground(Color.BLACK);
        field.setBackground(background);
        field.setSelectedTextColor(field.getForeground());
        field.setSelectionColor(field.getBackground());
        setClickCountToStart(1);
    }

    private JKeyStrokeField field = new JKeyStrokeField() {
        @Override
        protected KeyStroke filterKeyStroke(KeyStroke ks) {
            KeyStroke filtered = super.filterKeyStroke(ks);
            if (filtered == null) {
                int vk = ks.getKeyCode();
                if (vk == KeyEvent.VK_ESCAPE) {
                    cancelCellEditing();
                } else if (vk == KeyEvent.VK_ENTER) {
                    stopCellEditing();
                }
            }
            return filtered;
        }
    };

    @Override
    public Object getCellEditorValue() {
        return super.getCellEditorValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        field.setKeyStroke(JKeyStrokeField.fromDisplayString(value.toString()));
        field.setFont(table.getFont());
        return field;
    }
}
