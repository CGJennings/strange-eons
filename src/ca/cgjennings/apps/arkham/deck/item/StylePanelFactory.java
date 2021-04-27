package ca.cgjennings.apps.arkham.deck.item;

import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;

/**
 * A factory for creating the panels that are used to edit page item
 * {@link Style}s.
 *
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see Style
 * @see StylePanel
 */
public class StylePanelFactory {

    private StylePanelFactory() {
    }

    @SuppressWarnings("unchecked")
    public synchronized static <S extends Style> StylePanel<S> createStylePanel(Class<S> styleClass) {
        try {
            Class panelClass = null;
            if (registry != null) {
                panelClass = registry.get(styleClass);
            }
            if (panelClass == null) {
                panelClass = Class.forName(styleClass.getName() + "Panel");
            }
            return (StylePanel) panelClass.getConstructor().newInstance();
        } catch (IllegalAccessException | InvocationTargetException uex) {
            throw new RuntimeException("failed to instantiate panel class: " + styleClass, uex);
        } catch (ClassNotFoundException cnf) {
            throw new AssertionError("no style panel class exists for " + styleClass);
        } catch (InstantiationException | NoSuchMethodException ie) {
            throw new RuntimeException("registered class must be concrete and have a no-arg constructor: " + styleClass, ie);
        } catch (ClassCastException cce) {
            throw new RuntimeException("registered class is not a StylePanel: " + styleClass, cce);
        }
    }

    public synchronized static <S extends Style> void registerClass(Class<S> styleClass, Class<? extends StylePanel<S>> panelClass) {
        // runtime type check:
        if (!Style.class.isAssignableFrom(styleClass)) {
            throw new IllegalArgumentException("styleClass must be the Class object of the desired Style");
        }
        if (!StylePanel.class.isAssignableFrom(panelClass)) {
            throw new IllegalArgumentException("panelClass must be the Class object of the desired StylePanel");
        }

        if (registry == null) {
            registry = new HashMap<>();
        }
        registry.put(styleClass, panelClass);
    }

    private static HashMap<Class, Class> registry;
}
