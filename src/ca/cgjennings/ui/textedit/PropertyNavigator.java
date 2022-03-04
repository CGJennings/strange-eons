package ca.cgjennings.ui.textedit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Navigator} implementation for setting and property files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class PropertyNavigator extends RegexNavigatorBase {
    private static final Pattern LINE_PATTERN = Pattern.compile("^(.+)", Pattern.MULTILINE);
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

//    @Override
//    public List<NavigationPoint> getNavigationPoints(String text) {
//        List<NavigationPoint> list = new LinkedList<>();
//        Matcher m = null;
//        try {
//            boolean ignore = false;
//            m = pat.matcher(text);
//            while (m.find()) {
//                String line = m.group(1).trim();
//                if (line.startsWith("#") || line.startsWith("!")) {
//                    continue;
//                }
//                boolean oldIgnore = ignore;
//                ignore = line.endsWith("\\");
//                if (oldIgnore || line.isEmpty()) {
//                    continue;
//                }
//                int split = line.indexOf('=');
//                int colon = ignoreColon ? -1 : line.indexOf(':');
//                if (split < 0 || (colon >= 0 && colon < split)) {
//                    split = colon;
//                }
//                if (split < 0) {
//                    split = line.length();
//                }
//                list.add(new NavigationPoint(line.substring(0, split).trim(), null, m.start(1), 0, NavigationPoint.ICON_SETTING));
//            }
//            java.util.Collections.sort(list);
//        } catch (Throwable t) {
//            AbstractNavigator.dumpRegExpThrowable(t, text, m);
//        }
//        return list;
//    }
}
