package ca.cgjennings.ui.textedit;

/**
 * Code support for game resource files (class maps, tiles, and so on).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class ResourceFileCodeSupport extends PropertyFileCodeSupport {

    @Override
    public Navigator createNavigator(NavigationHost host) {
        return new ResourceFileNavigator();
    }
}
