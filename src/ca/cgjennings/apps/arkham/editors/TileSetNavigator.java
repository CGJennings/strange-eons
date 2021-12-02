package ca.cgjennings.apps.arkham.editors;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Navigator} implementation for tile set files. Creates one point for
 * each tile description using the tile name text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TileSetNavigator implements Navigator {

    @Override
    public void install(CodeEditor editor) {
    }

    @Override
    public void uninstall(CodeEditor editor) {
    }

    @Override
    public List<NavigationPoint> getNavigationPoints(String text) {
        List<NavigationPoint> list = new LinkedList<>();
        Matcher m = null;
        try {
            boolean newTileState = true;
            m = pat.matcher(text);
            while (m.find()) {
                String line = m.group(1).trim();
                if (line.startsWith("#") || line.startsWith("!")) {
                } else if (line.isEmpty()) {
                    newTileState = true;
                } else if (newTileState) {
                    list.add(new NavigationPoint(line, null, m.start(1), 0, NavigationPoint.ICON_TRIANGLE));
                    newTileState = false;
                }
            }
            java.util.Collections.sort(list);
        } catch (Throwable t) {
            AbstractNavigator.dumpRegExpThrowable(t, text, m);
        }
        return list;
    }
    private final Pattern pat = Pattern.compile("^(.*)", Pattern.MULTILINE);
}
