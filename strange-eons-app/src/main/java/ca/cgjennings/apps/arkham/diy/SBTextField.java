package ca.cgjennings.apps.arkham.diy;

import javax.swing.JTextField;

/**
 * A {@link JTextField} that implements the {@link SettingBackedControl}
 * interface.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class SBTextField extends JTextField implements SettingBackedControl {

    public SBTextField() {
    }

    public SBTextField(int columns) {
        super(columns);
    }

    @Override
    public void fromSetting(String v) {
        setText(v);
        select(0, 0);
    }

    @Override
    public String toSetting() {
        return getText();
    }
}
