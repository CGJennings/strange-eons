package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.deck.item.Curve;
import ca.cgjennings.apps.arkham.deck.item.CustomTile;
import ca.cgjennings.apps.arkham.deck.item.Line;
import ca.cgjennings.apps.arkham.deck.item.PageItem;
import ca.cgjennings.apps.arkham.deck.item.TextBox;
import ca.cgjennings.apps.arkham.deck.item.TuckBox;
import java.util.LinkedHashSet;
import java.util.Set;
import resources.Language;

/**
 * A registry of prototype {@link PageItem}s for all items that appear in the
 * <b>Tools</b> tab of a deck editor. This can be used to add new tool types.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Tools {

    private Tools() {
    }

    /**
     * Register a new tool to be listed in the <b>Tools</b> tab of the deck
     * editor.
     *
     * @param prototype a prototype item that will be cloned as needed when
     * adding the tool to a deck page
     * @throws NullPointerException if the prototype is {@code null}
     */
    public static synchronized void register(PageItem prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype");
        }
        tools.add(prototype);
    }

    /**
     * Removes a tool from the list of registered tools. If the prototype is not
     * registered, does nothing.
     *
     * @param prototype the registered tool to remove
     */
    public static synchronized void unregister(PageItem prototype) {
        tools.remove(prototype);
    }

    /**
     * Returns the prototypes of all currently registered tools, in order of
     * registration.
     *
     * @return an array of currently registered tools
     */
    public static synchronized PageItem[] getRegisteredTools() {
        return tools.toArray(new PageItem[tools.size()]);
    }

    private static final Set<PageItem> tools = new LinkedHashSet<>();

    static {
        TextBox tb = new TextBox();
        tb.setSize(144d, 72d);
        tb.setText(Language.string("de-text-box-content"));
        tools.add(tb);
        tools.add(new CustomTile("", 75d));
        tools.add(new Line());
        tools.add(new Curve());
        tools.add(new TuckBox());
    }
}
