package ca.cgjennings.apps.arkham.diy;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import resources.Settings;

/**
 * An integer-valued spinner that implements the {@link SettingBackedControl}
 * interface.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class SBIntSpinner extends JSpinner implements SettingBackedControl {

    SpinnerNumberModel model;
    private int min, max;

    public SBIntSpinner(int min, int max, int stepSize) {
        super();
        if (max < min) {
            throw new IllegalArgumentException("max < min");
        }
        model = new SpinnerNumberModel(min, min, max, stepSize);
        setModel(model);
        this.min = min;
        this.max = max;
        getEditor().setOpaque(false);
    }

    @Override
    public void fromSetting(String v) {
        int w;
        try {
            w = Settings.integer(v);
            if (w < min) {
                w = min;
            }
            if (w > max) {
                w = max;
            }
        } catch (Settings.ParseError e) {
            w = min;
        }
        model.setValue(w);
    }

    @Override
    public String toSetting() {
        return model.getValue().toString();
    }
}
