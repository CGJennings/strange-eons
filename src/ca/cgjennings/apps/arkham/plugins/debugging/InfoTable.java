package ca.cgjennings.apps.arkham.plugins.debugging;

import java.util.ArrayList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Used to package and unpackage tabular information to aid with debugging. If
 * you wish to add a new kind of table data that can be viewed with debuggers
 * that support this feature, you must implement a {@link TableGenerator} and
 * register it with {@link Tables}.
 *
 * @see TableGenerator
 * @see Tables
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class InfoTable {

    private String[] columns;
    private ArrayList<String> data = new ArrayList<>();

    /**
     * Creates a new, empty table.
     */
    public InfoTable() {
    }

    /**
     * Creates a new table by deserializing a previously serialized table.
     *
     * @param serializedTable a serialized table, presented as one line per
     * array entry
     */
    public InfoTable(String[] serializedTable) {
        int colNums = Integer.parseInt(serializedTable[0]);
        columns = new String[colNums];
        int i = 1;
        for (; i <= colNums; ++i) {
            columns[i - 1] = unescape(serializedTable[i]);
        }
        for (; i < serializedTable.length; ++i) {
            data.add(unescape(serializedTable[i]));
        }
    }

    /**
     * Sets the column names used by this table. The columns can only be set
     * once.
     *
     * @param cols an array of column names
     * @throws NullPointerException if {@code cols} is {@code null}
     * @throws IllegalArgumentException if the columns
     */
    public void setColumns(String... cols) {
        if (cols == null) {
            throw new NullPointerException("cols");
        }
        if (cols.length == 0) {
            throw new IllegalArgumentException("empty cols");
        }
        if (columns != null) {
            throw new IllegalArgumentException("already set");
        }
        columns = cols.clone();
    }

    /**
     * Returns an array of the column names for this table.
     *
     * @return the columns previously set on this table
     */
    public String[] getColumns() {
        return columns.clone();
    }

    /**
     * Inserts a new section into the table data. A section is a row with only
     * one column.
     *
     * @param name the name of the section
     * @throws IllegalStateException if the columns have not been set
     */
    public void section(String name) {
        add("<html><b>" + name);
    }

    /**
     * Adds a new row of data to the table. One string is passed per column; if
     * the number of strings passed in is less than the number of columns,
     * additional empty columns will be appended to fill out the row. If too
     * many strings are passed in, the superfluous strings are ignored.
     *
     * @param row the strings that make up the cells in the this row
     * @throws IllegalStateException if the columns have not been set
     */
    public void add(String... row) {
        if (columns == null) {
            throw new IllegalStateException("columns not set");
        }
        if (row == null) {
            throw new NullPointerException("row");
        }
        for (int i = 0; i < columns.length; ++i) {
            if (i >= row.length || row[i] == null) {
                data.add("");
            } else {
                data.add(row[i]);
            }
        }
    }

    /**
     * Returns the table cell for the requested row and column.
     *
     * @param row the index of the cell's row
     * @param col the index of the cell's column
     * @return the cell data at the requested row and column
     * @throws IndexOutOfBoundsException if the row or column is outside of the
     * table
     */
    public String get(int row, int col) {
        int index = row * columns.length + col;
        if (index < 0) {
            throw new IndexOutOfBoundsException("bad location: " + row + ", " + col);
        }
        if (index >= data.size()) {
            return "";
        }
        return data.get(index);
    }

    /**
     * Serializes this table's content by writing a string into a
     * {@code StringBuilder}. This prepares the table for transport to a
     * debugging client running in a separate process.
     *
     * @param b the string builder to append the table to
     */
    public void serialize(StringBuilder b) {
        b.append(columns.length);
        for (int i = 0; i < columns.length; ++i) {
            b.append('\n').append(escape(columns[i]));
        }
        for (int i = 0; i < data.size(); ++i) {
            b.append('\n').append(escape(data.get(i)));
        }
    }

    /**
     * Install this table's data in a {@code JTable}, replacing the
     * existing table model.
     *
     * @param t the table component to use to display the data
     */
    public void install(JTable t) {
        TableModel m = createTableModel();
        t.setModel(m);
        TableColumnModel cols = t.getColumnModel();
        t.validate();

        int sumWidths = 0;
        for (int c = 0; c < cols.getColumnCount() - 1; ++c) {
            int minWidth = 0;
            for (int r = 0; r < m.getRowCount(); ++r) {
                minWidth = Math.max(
                        minWidth,
                        t.getCellRenderer(r, c)
                                .getTableCellRendererComponent(t, m.getValueAt(r, c), false, false, r, c)
                                .getPreferredSize().width
                );
            }
            minWidth += 16;
            cols.getColumn(c).setPreferredWidth(minWidth);
            sumWidths += minWidth;
        }
        if (cols.getColumnCount() > 0) {
            int containerWidth = t.getWidth();
            if (t.getParent() instanceof JScrollPane) {
                containerWidth = t.getParent().getWidth();
            }
            int width = Math.max(32, containerWidth - sumWidths);
            cols.getColumn(cols.getColumnCount() - 1).setPreferredWidth(width);
        }
    }

    private TableModel createTableModel() {
        final int cols = columns.length;
        final int rows = (data.size() + (cols - 1)) / cols;
        return new AbstractTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public String getColumnName(int column) {
                return columns[column];
            }

            @Override
            public int getRowCount() {
                return rows;
            }

            @Override
            public int getColumnCount() {
                return cols;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return get(rowIndex, columnIndex);
            }
        };
    }

    /**
     * Escapes the content of a cell string into the format used for
     * serialization. You do not typically call this directly; the table
     * implementation handles escaping and unescaping transparently.
     *
     * @param s the string to escape
     * @return an escaped string that is ready to serialize
     */
    public static String escape(String s) {
        if (s == null) {
            return "^";
        }
        boolean required = false;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n' || c == '\\' || c == '^' || c == '|') {
                required = true;
                break;
            }
        }
        if (!required) {
            return s;
        }

        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n' || c == '\\' || c == '^' || c == '|') {
                b.append('\\');
                if (c == '\r') {
                    b.append('r');
                } else if (c == '\n') {
                    b.append('n');
                } else {
                    b.append(c);
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Unescapes a previously escaped string from the format used for
     * serialization. You do not typically call this directly; the table
     * implementation handles escaping and unescaping transparently.
     *
     * @param s the string to unescape
     * @return an unescaped string
     */
    public static String unescape(String s) {
        if (s != null) {
            if (s.equals("^")) {
                return null;
            }
            if (s.indexOf('\\') >= 0) {
                StringBuilder b = new StringBuilder(s.length() + 16);
                for (int i = 0; i < s.length(); ++i) {
                    char c = s.charAt(i);
                    if (c == '\\' && i < (s.length() - 1)) {
                        char e = s.charAt(++i);
                        if (e == 'r') {
                            e = '\r';
                        } else if (e == 'n') {
                            e = '\n';
                        }
                        b.append(e);
                    } else {
                        b.append(c);
                    }
                }
                s = b.toString();
            }
        }
        return s;
    }

    /**
     * Creates a dummy table that is filled in with an error message. This can
     * be used to generate a stand-in table when table generation fails.
     *
     * @param message an optional message
     * @param t an optional exception
     * @return a table that describes the error
     */
    public static InfoTable errorTable(String message, Throwable t) {
        InfoTable it = new InfoTable();
        it.setColumns("Error");
        if (message == null) {
            message = "There was an error while generating the table";
        }
        it.add(message);
        if (t != null) {
            it.add(t.toString());
        }
        return it;
    }
}
