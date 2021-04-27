package ca.cgjennings.ui.table;

import java.util.Vector;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * A table that does not create a visible header when embedded in a scroll pane.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JHeadlessTable extends JResizableTable {

    public JHeadlessTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
    }

    public JHeadlessTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
    }

    public JHeadlessTable(int numRows, int numColumns) {
        super(numRows, numColumns);
    }

    public JHeadlessTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
    }

    public JHeadlessTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
    }

    public JHeadlessTable(TableModel dm) {
        super(dm);
    }

    public JHeadlessTable() {
    }

    @Override
    protected void configureEnclosingScrollPane() {
//		Container p = getParent();
//		if( p instanceof JViewport ) {
//			Container gp = p.getParent();
//			if( gp instanceof JScrollPane ) {
//				JScrollPane scrollPane = (JScrollPane) gp;
//				// Make certain we are the viewPort's view and not, for
//				// example, the rowHeaderView of the scrollPane -
//				// an implementor of fixed columns might do this.
//				JViewport viewport = scrollPane.getViewport();
//				if( viewport == null || viewport.getView() != this ) {
//					return;
//				}
//				scrollPane.setColumnHeaderView( getTableHeader() );
//				// configure the scrollpane for any LAF dependent settings
//				configureEnclosingScrollPaneUI();
//			}
//		}
    }
}
