package ca.cgjennings.ui.textedit;

/**
 * Code support for game resource files such as class maps.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class ResourceFileCodeSupport extends PropertyFileCodeSupport {

    @Override
    public Navigator createNavigator(NavigationHost host) {
        return new PropertyNavigator(true);
    }
}
