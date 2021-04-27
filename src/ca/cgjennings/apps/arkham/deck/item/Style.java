package ca.cgjennings.apps.arkham.deck.item;

/**
 * This is a marker interface extended by all interfaces for getting or setting
 * a type of style information. Each <code>Style</code> subinterface defines a
 * collection of getters and setters that together are used to control the
 * parameters of that style. For example, the {@link LineStyle} defines getters
 * and setters for the line width, colour, dash pattern, and so on.
 *
 * <p>
 * No two styles can share a common method name. For example, the
 * {@link OutlineStyle} also defines a style dealing with lines (outlines of
 * objects), but it uses distinct method names so that an item (or group of
 * items) can implement both styles.
 *
 * <p>
 * The style information for one or more {@link PageItem}s can be read and
 * written between different classes that have one or more <code>Style</code>
 * interface(s) in common using a {@link StyleCapture}. This allows you to, for
 * example, capture the style of one object and then apply it to other objects.
 *
 * <p>
 * To add a new kind of editable page item style that can be edited by the user,
 * you must define both a {@link Style} subinterface the defines the getters and
 * setters needed to modify the style and a {@link StylePanel} class that
 * provides the interface controls to edit the style. The framework will take
 * care of the rest: if a page item is selected that implements your
 * {@link Style}, then the user will have the option to edit the item's style
 * and a suitable interface control will be created using
 * {@link StylePanelFactory}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface Style {
}
