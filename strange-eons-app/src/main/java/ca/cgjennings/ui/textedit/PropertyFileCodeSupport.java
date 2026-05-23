package ca.cgjennings.ui.textedit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    static final Pattern LINE_PATTERN = Pattern.compile("^(.+)", Pattern.MULTILINE);

    /**
     * A {@link Navigator} implementation for setting and property files.
     *
     * @author Chris Jennings <https://cgjennings.ca/contact>
     * @since 3.0
     */
    public static class PropertyNavigator extends RegexNavigatorBase {

        private final boolean ignoreColon;
        private boolean ignore;

        public PropertyNavigator() {
            super(LINE_PATTERN);
            ignoreColon = false;
        }

        public PropertyNavigator(boolean ignoreColons) {
            super(LINE_PATTERN);
            ignoreColon = ignoreColons;
        }

        @Override
        protected NavigationPoint createNavigationPoint(Matcher m, String sourceText, boolean initialize) {
            if (initialize) {
                ignore = false;
            }
            String line = m.group(1).trim();
            if (line.startsWith("#") || line.startsWith("!")) {
                return null;
            }
            boolean oldIgnore = ignore;
            ignore = line.endsWith("\\");
            if (oldIgnore || line.isEmpty()) {
                return null;
            }
            int split = line.indexOf('=');
            int colon = ignoreColon ? -1 : line.indexOf(':');
            if (split < 0 || (colon >= 0 && colon < split)) {
                split = colon;
            }
            if (split < 0) {
                split = line.length();
            }
            return new NavigationPoint(line.substring(0, split).trim(), null, m.start(1), 0, NavigationPoint.ICON_SETTING);
        }
    }
}
