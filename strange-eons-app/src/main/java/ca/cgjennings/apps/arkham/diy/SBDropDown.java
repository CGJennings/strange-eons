package ca.cgjennings.apps.arkham.diy;

import java.awt.Dimension;
import javax.swing.JComboBox;

/**
 * A {@link JComboBox} that implements the {@link SettingBackedControl}
 * interface.
 *
 * @param <E> the type of element listed by the box
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class SBDropDown<E> extends JComboBox<E> implements SettingBackedControl {

    private final String[] values;

    public SBDropDown(E[] items, String[] values) {
        super(items);
        this.values = values;
    }

    @Override
    public void fromSetting(String v) {
        for (int i = 0; i < values.length; ++i) {
            if (values[i] == null && v != null) {
                continue;
            }
            if (values[i] != null && v == null) {
                continue;
            }
            if (values[i] == v || values[i].equals(v)) {
                setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width += 22;
        return d;
    }

    @Override
    public String toSetting() {
        if (getSelectedIndex() < 0) {
            return null;
        }
        return values[getSelectedIndex()];
    }
}
