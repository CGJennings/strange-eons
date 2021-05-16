package ca.cgjennings.apps.arkham.diy;

import ca.cgjennings.ui.IconProvider;
import javax.swing.Icon;

/**
 * A list item is an item that can be placed in a list or combo box control in a
 * DIY component's user interface. A list item will display one label for the
 * user, but use a different string for the setting that the item is bound to in
 * the DIY component. One use for this is to easily create components that
 * support multiple languages: the localized text can be used for the label, and
 * a unique internal value used for the setting value. In order to be displayed
 * correctly, the list or combo box must use a {@link ListItemRenderer} to
 * render the list items. (Lists and combo boxes created using the
 * {@code uicontrols} library will use this renderer.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ListItem implements IconProvider {

    private Object value;
    private String settingValue;
    private Icon icon;

    /**
     * Creates a new list item with the given label and setting representation,
     * but no icon.
     *
     * @param settingValue the setting value to use for this item when copying
     * the state of the DIY component to the UI control, or from the UI control
     * to the DIY component when the user selects the item
     * @param labelObject an object used to create the label text for the item
     * @throws NullPointerException if the label object or setting value are
     * {@code null}
     */
    public ListItem(String settingValue, Object labelObject) {
        this(settingValue, labelObject, null);
    }

    /**
     * Creates a new list item with the given label, setting representation, and
     * icon.
     *
     * @param settingValue the setting value to use for this item when copying
     * the state of the DIY component to the UI control, or from the UI control
     * to the DIY component when the user selects the item
     * @param labelObject an object used to create the label text for the item
     * @param icon the item's icon (may be {@code null})
     * @throws NullPointerException if the label object or setting value are
     * {@code null}
     */
    public ListItem(String settingValue, Object labelObject, Icon icon) {
        if (labelObject == null) {
            throw new NullPointerException("value");
        }
        if (settingValue == null) {
            throw new NullPointerException("settingValue");
        }
        this.value = labelObject;
        this.settingValue = settingValue;
        this.icon = icon;
    }

    /**
     * Sets the object used to produce the item's label for the user. The label
     * text will consist of the object's string representation. If the item is
     * already being displayed in a list, the list will not reflect this change
     * until it is next repainted.
     *
     * @param value the object used to generate the item's label
     * @see #getLabel()
     */
    public void setLabelObject(Object value) {
        this.value = value;
    }

    /**
     * Returns the object used to produce the item's label for the user.
     *
     * @return the current label object
     * @see #setLabelObject(java.lang.Object)
     */
    public Object getLabelObject() {
        return value;
    }

    /**
     * Returns the text to display to the user for this list item. The base
     * class returns the string value of the label object.
     *
     * @return the label this item displays to the user
     */
    public String getLabel() {
        return String.valueOf(value);
    }

    /**
     * Returns this item's icon, or {@code null} if it doesn't have an
     * icon.
     *
     * @return this item's icon, or {@code null}
     */
    @Override
    public Icon getIcon() {
        return icon;
    }

    /**
     * Sets the item's icon. Note that if the item is displayed in a list, the
     * list will not reflect the change until it is next repainted.
     *
     * @param icon the item's new icon
     */
    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    /**
     * Returns a string representation of the item. The string representation of
     * a list item is its setting value.
     *
     * @return the item's setting value
     */
    @Override
    public String toString() {
        return settingValue;
    }

    /**
     * Returns {@code true} if another object's string representation is
     * equal to this item's setting value.
     *
     * @param obj the object to check for equality with this item
     * @return {@code true} if the object is not {@code null} and
     * {@code obj.toString()} is equal to this item's setting value
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        String value;
        if (getClass() != obj.getClass()) {
            value = obj.toString();
        } else {
            value = ((ListItem) obj).settingValue;
        }
        return settingValue.equals(value);
    }

    /**
     * Returns a hash code for this list item. The hash code will be equal to
     * the hash code of the setting value.
     *
     * @return a hash code for this item
     */
    @Override
    public int hashCode() {
        return settingValue.hashCode();
    }
}
