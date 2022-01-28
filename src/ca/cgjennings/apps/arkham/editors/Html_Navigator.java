package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Navigator} implementation for HTML files.
 */
public class Html_Navigator implements Navigator {

    @Override
    public void install(CodeEditor editor) {
    }

    @Override
    public void uninstall(CodeEditor editor) {
    }

    @Override
    public List<NavigationPoint> getNavigationPoints(String text) {
        LinkedList<NavigationPoint> list = new LinkedList<>();
        Matcher m = null;
        try {
            //text = comments.matcher( text ).replaceAll( "" );
            m = pat.matcher(text);
            int scope = 0;
            while (m.find()) {
                if (m.start(10) >= 0) {
                    continue;
                }
                int h;
                for (h = 1; h < 7; ++h) {
                    if (m.start(h) >= 0) {
                        scope = h - 1;
                        list.add(new NavigationPoint(
                                AbstractGameComponent.filterComponentText(m.group(h)),
                                null, m.start(h), scope,
                                NavigationPoint.ICON_DIAMOND
                        ));
                        break;
                    }
                }
                if (h < 7) {
                    continue;
                }
                if (m.start(7) >= 0) {
                    list.add(new NavigationPoint(AbstractGameComponent.filterComponentText(m.group(7)), null, m.start(7), 0, NavigationPoint.ICON_TRIANGLE));
                    continue;
                }
                if (m.start(8) >= 0) {
                    list.add(new NavigationPoint("<DIV>", null, m.start(8), scope, NavigationPoint.ICON_SQUARE));
                    continue;
                }
                if (m.start(9) >= 0) {
                    list.add(new NavigationPoint("<TABLE>", null, m.start(9), scope, NavigationPoint.ICON_CROSS));
                }
            }
        } catch (Throwable t) {
            AbstractNavigator.dumpRegExpThrowable(t, text, m);
        }
        return list;
    }
    private static final Pattern pat = Pattern.compile("(?:<h1>(.*?)</h1>)" + "|(?:<h2>(.*?)</h2>)" + "|(?:<h3>(.*?)</h3>)" + "|(?:<h4>(.*?)</h4>)" + "|(?:<h5>(.*?)</h5>)" + "|(?:<h6>(.*?)</h6>)" + "|(?:<title>(.*?)</title>)" + "|(<div>.*?</div>)" + "|(<table>.*?</table>)" + "|(<!--.*?-->)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
}
