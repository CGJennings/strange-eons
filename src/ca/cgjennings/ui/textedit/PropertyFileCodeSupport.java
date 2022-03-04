package ca.cgjennings.ui.textedit;

/**
 * Code support for the Java language.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class PropertyFileCodeSupport extends DefaultCodeSupport {

    @Override
    public Navigator createNavigator(NavigationHost host) {
        return new PropertyNavigator();
    }
}
