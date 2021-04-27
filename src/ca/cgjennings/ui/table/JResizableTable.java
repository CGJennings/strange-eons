package ca.cgjennings.ui.table;

import java.util.EventObject;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JResizableTable extends JTable {

    public JResizableTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
    }

    public JResizableTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
    }

    public JResizableTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }

    public JResizableTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
    }

    public JResizableTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
    }

    public JResizableTable(TableModel dm) {
        super(dm);
    }

    public JResizableTable() {
    }

    public void setColumnResizable(boolean resizable) {
        if (resizable) {
            if (columnResizer == null) {
                columnResizer = new TableColumnResizer(this);
            }
        } else if (columnResizer != null) {
            removeMouseListener(columnResizer);
            removeMouseMotionListener(columnResizer);
            columnResizer = null;
        }

    }

    public boolean getColumnResizable() {
        return columnResizer != null;
    }

    public void setRowResizable(boolean resizable) {
        if (resizable) {
            if (rowResizer == null) {
                rowResizer = new TableRowResizer(this);
            }
        } else if (rowResizer != null) {
            removeMouseListener(rowResizer);
            removeMouseMotionListener(rowResizer);
            rowResizer = null;
        }
    }

    public boolean getRowResizable() {
        return rowResizer != null;
    }

    public boolean isResizing() {
        return getCursor() == TableColumnResizer.resizeCursor || getCursor() == TableRowResizer.resizeCursor;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (isResizing()) {
            return false;
        }
        return super.isCellEditable(row, column);
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (isResizing()) {
            return false;
        }
        return super.editCellAt(row, column, e);
    }

    @Override
    public void changeSelection(int row, int column, boolean toggle, boolean extend) {
        if (isResizing()) {
            return;
        }
        super.changeSelection(row, column, toggle, extend);
    }

    protected MouseInputAdapter rowResizer;
    protected MouseInputAdapter columnResizer;
}
