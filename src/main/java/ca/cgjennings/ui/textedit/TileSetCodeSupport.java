package ca.cgjennings.ui.textedit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code support for tile set resource files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class TileSetCodeSupport extends DefaultCodeSupport {

    @Override
    public Navigator createNavigator(NavigationHost host) {
        return new TileSetNavigator();
    }

    private static final Pattern LINE_PATTERN = Pattern.compile("^(.*)", Pattern.MULTILINE);

    public static class TileSetNavigator extends RegexNavigatorBase {

        private boolean newTileState;

        public TileSetNavigator() {
            super(LINE_PATTERN);
        }

        @Override
        protected NavigationPoint createNavigationPoint(Matcher m, String sourceText, boolean initialize) {
            if (initialize) {
                newTileState = true;
            }
            NavigationPoint np = null;
            String line = m.group(1).trim();
            if (line.startsWith("#") || line.startsWith("!")) {
                // np = null;
            } else if (line.isEmpty()) {
                // np = null;
                newTileState = true;
            } else if (newTileState) {
                np = new NavigationPoint(line, null, m.start(1), 0, NavigationPoint.ICON_SETTING);
                newTileState = false;
            }
            return np;
        }
    }
}
