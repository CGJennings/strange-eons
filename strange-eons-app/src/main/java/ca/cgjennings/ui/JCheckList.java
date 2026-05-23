package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * A {@code JList} of {@code JCheckBox} items.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JCheckList extends JList<Object> {

    public JCheckList() {
        super();
        init();
    }

    public JCheckList(Object[] listData) {
        super(listData);
        init();
    }

    public JCheckList(ListModel<Object> dataModel) {
        super(dataModel);
        init();
    }

    private void init() {
        setCellRenderer(new Renderer());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                if (index != -1) {
                    JCheckBox item = (JCheckBox) getModel().getElementAt(index);
                    checkBoxMousePressed(JCheckList.this, index, item, e);
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int index = getSelectedIndex();
                if (index != -1) {
                    JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
                    checkBoxKeyPressed(JCheckList.this, index, checkbox, e);
                }
            }
        });
    }

    protected void checkBoxMousePressed(JCheckList list, int index, JCheckBox item, MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1) {
            toggleCheckBox(index);
        }
    }

    protected void checkBoxKeyPressed(JCheckList list, int index, JCheckBox item, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_SPACE) {
            toggleCheckBox(index);
        }
    }

    protected void toggleCheckBox(int index) {
        JCheckBox box = (JCheckBox) getModel().getElementAt(index);
        box.setSelected(!box.isSelected());
        repaint();
    }

    public void setCheckBox(int index, boolean checked) {
        ((JCheckBox) getModel().getElementAt(index)).setSelected(checked);
        repaint();
    }

    public boolean getCheckBox(int index) {
        return ((JCheckBox) getModel().getElementAt(index)).isSelected();
    }

    protected static class Renderer implements ListCellRenderer<Object> {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            System.err.println("called renderer");
            JCheckBox checkbox = (JCheckBox) value;
            checkbox.setBorderPaintedFlat(true);
            checkbox.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            checkbox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            checkbox.setBorderPainted(true);
            checkbox.setEnabled(list.isEnabled());
            checkbox.setFont(list.getFont());
            checkbox.setFocusPainted(false);
            return checkbox;
        }
        protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
    }
}
