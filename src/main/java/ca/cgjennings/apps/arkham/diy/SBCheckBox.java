package ca.cgjennings.apps.arkham.diy;

import javax.swing.JCheckBox;
import resources.Settings;

/**
 * A {@link JCheckBox} that implements the {@link SettingBackedControl}
 * interface.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class SBCheckBox extends JCheckBox implements SettingBackedControl {

    private boolean invert;

    /**
     * Create a new setting-backed check box.
     *
     * @param text the label text for the control
     * @param invert if true, the box is checked when the setting is disabled,
     * and vice-versa.
     */
    public SBCheckBox(String text, boolean invert) {
        super(text);
        this.invert = invert;
        // for multiline labels
        setVerticalAlignment(TOP);
        setVerticalTextPosition(TOP);
    }

    /**
     * Cover for {@code SBCheckBox((text, false )}.
     */
    public SBCheckBox(String text) {
        this(text, false);
    }

    @Override
    public void fromSetting(String v) {
        boolean b = Settings.yesNo(v);
        if (invert) {
            b = !b;
        }
        setSelected(b);
    }

    @Override
    public String toSetting() {
        boolean b = isSelected();
        if (invert) {
            b = !b;
        }
        return b ? "yes" : "no";
    }
}
