package ca.cgjennings.apps.arkham.editors;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Navigator} implementation for setting and property files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class PropertyNavigator implements Navigator {

    private final boolean ignoreColon;

    public PropertyNavigator() {
        ignoreColon = false;
    }

    public PropertyNavigator(boolean ignoreColons) {
        ignoreColon = ignoreColons;
    }

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
            boolean ignore = false;
            m = pat.matcher(text);
            while (m.find()) {
                String line = m.group(1).trim();
                if (line.startsWith("#") || line.startsWith("!")) {
                    continue;
                }
                boolean oldIgnore = ignore;
                ignore = line.endsWith("\\");
                if (oldIgnore || line.isEmpty()) {
                    continue;
                }
                int split = line.indexOf('=');
                int colon = ignoreColon ? -1 : line.indexOf(':');
                if (split < 0 || (colon >= 0 && colon < split)) {
                    split = colon;
                }
                if (split < 0) {
                    split = line.length();
                }
                list.add(new NavigationPoint(line.substring(0, split).trim(), null, m.start(1), 0, NavigationPoint.ICON_SETTING));
            }
            java.util.Collections.sort(list);
        } catch (Throwable t) {
            AbstractNavigator.dumpRegExpThrowable(t, text, m);
        }
        return list;
    }
    private final Pattern pat = Pattern.compile("^(.+)", Pattern.MULTILINE);
}
