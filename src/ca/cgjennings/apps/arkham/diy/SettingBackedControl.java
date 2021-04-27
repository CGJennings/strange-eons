package ca.cgjennings.apps.arkham.diy;

/**
 * Implemented by user interface controls that can convert their state to and
 * from settings key values automatically.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface SettingBackedControl {

    /**
     * Initialize this control from a setting value.
     *
     * @param v the setting value
     */
    void fromSetting(String v);

    /**
     * Return this control's state as a setting value.
     *
     * @return the setting value
     */
    String toSetting();
}
