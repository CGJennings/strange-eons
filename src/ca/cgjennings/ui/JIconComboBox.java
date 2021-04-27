package ca.cgjennings.ui;

import java.util.Collection;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/**
 * A combo box that that will draw icons for any added items that are
 * {@link IconProvider}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
@SuppressWarnings("serial")
public class JIconComboBox extends JComboBox {

    public JIconComboBox() {
        init();
    }

    public JIconComboBox(Collection<?> items) {
        init();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (Object o : items) {
            model.addElement(o);
        }
        setModel(model);
    }

    public JIconComboBox(Object[] items) {
        super(items);
        init();
    }

    public JIconComboBox(ComboBoxModel aModel) {
        super(aModel);
        init();
    }

    private void init() {
        setRenderer(new JIconList.IconRenderer());
    }
}
