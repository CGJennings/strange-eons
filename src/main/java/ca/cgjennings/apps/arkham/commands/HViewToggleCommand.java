package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * A helper for creating commands that toggle a global view option by calling a
 * static method in a specific class.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class HViewToggleCommand extends AbstractToggleCommand {

    private Method toggleMethod;

    public HViewToggleCommand(String nameKey, Class<?> optionClass, String optionName) {
        super(nameKey);
        init(optionClass, optionName);
    }

    public HViewToggleCommand(String nameKey, String iconResource, Class<?> optionClass, String optionName) {
        super(nameKey, iconResource);
        init(optionClass, optionName);
    }

    private void init(Class<?> optionClass, String optionName) {
        try {
            Method getter = optionClass.getMethod("is" + optionName);
            putValue(SELECTED_KEY, getter.invoke(null));
            toggleMethod = optionClass.getMethod("set" + optionName, boolean.class);
        } catch (Throwable ex) {
            StrangeEons.log.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            toggleMethod.invoke(null, (isSelected()));
        } catch (Exception ex) {
            StrangeEons.log.log(Level.SEVERE, null, ex);
        }
    }
}
