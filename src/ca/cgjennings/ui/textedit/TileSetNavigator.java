package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;

/**
 * A {@link Navigator} implementation for tile set files. Creates one point for
 * each tile description using the tile name text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TileSetNavigator extends RegexNavigatorBase {
    private static final Pattern LINE_PATTERN = Pattern.compile("^(.*)", Pattern.MULTILINE);
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
            newTileState  = false;
        }
        return np;
    }    
    
//    @Override
//    public List<NavigationPoint> getNavigationPoints(String text) {
//        List<NavigationPoint> list = new LinkedList<>();
//        Matcher m = null;
//        try {
//            boolean newTileState = true;
//            m = pat.matcher(text);
//            while (m.find()) {
//                String line = m.group(1).trim();
//                if (line.startsWith("#") || line.startsWith("!")) {
//                } else if (line.isEmpty()) {
//                    newTileState = true;
//                } else if (newTileState) {
//                    list.add(new NavigationPoint(line, null, m.start(1), 0, NavigationPoint.ICON_SETTING));
//                    newTileState = false;
//                }
//            }
//            java.util.Collections.sort(list);
//        } catch (Throwable t) {
//            AbstractNavigator.dumpRegExpThrowable(t, text, m);
//        }
//        return list;
//    }
}
