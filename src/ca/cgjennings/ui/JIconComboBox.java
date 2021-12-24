package ca.cgjennings.ui;

import java.util.Collection;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;

/**
 * A combo box that that will draw icons for any added items that are
 * {@link IconProvider}s.
 *
 * @param <E> the type of listed objects
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
@SuppressWarnings("serial")
public class JIconComboBox<E> extends JComboBox<E> {

    public JIconComboBox() {
        init();
    }
    
    public JIconComboBox(E[] items) {
        super(items);
        init();
    }

    public JIconComboBox(Collection<E> items) {
        init();
        DefaultComboBoxModel<E> model = new DefaultComboBoxModel<>();
        for (E el : items) {
            model.addElement(el);
        }
        setModel(model);
    }

    public JIconComboBox(ComboBoxModel<E> aModel) {
        super(aModel);
        init();
    }

    private void init() {
        setRenderer((ListCellRenderer<? super E>) new JIconList.IconRenderer());
    }
}
