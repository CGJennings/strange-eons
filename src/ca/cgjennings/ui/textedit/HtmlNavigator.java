package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;

/**
 * A {@link Navigator} implementation for HTML files.
 */
public class HtmlNavigator extends RegexNavigatorBase {
    private static final Pattern HTML_PATTERN = Pattern.compile("(?:<h1>(.*?)</h1>)" + "|(?:<h2>(.*?)</h2>)" + "|(?:<h3>(.*?)</h3>)" + "|(?:<h4>(.*?)</h4>)" + "|(?:<h5>(.*?)</h5>)" + "|(?:<h6>(.*?)</h6>)" + "|(?:<title>(.*?)</title>)" + "|(<div>.*?</div>)" + "|(<table>.*?</table>)" + "|(<!--.*?-->)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private int scope;

    public HtmlNavigator() {
        super(HTML_PATTERN);
    }

    @Override
    protected NavigationPoint createNavigationPoint(Matcher m, String sourceText, boolean initialize) {
        if (initialize) {
            scope = 0;
        }
        if (m.start(10) >= 0) {
            return null;
        }
        int h;
        for (h = 1; h < 7; ++h) {
            if (m.start(h) >= 0) {
                Icon hnIcon;
                switch (h) {
                    case 1:
                        hnIcon = NavigationPoint.ICON_H1;
                        break;
                    case 2:
                        hnIcon = NavigationPoint.ICON_H2;
                        break;
                    case 3:
                        hnIcon = NavigationPoint.ICON_H3;
                        break;
                    case 4:
                        hnIcon = NavigationPoint.ICON_H4;
                        break;
                    case 5:
                        hnIcon = NavigationPoint.ICON_H5;
                        break;
                    case 6:
                        hnIcon = NavigationPoint.ICON_H6;
                        break;
                    default:
                        hnIcon = NavigationPoint.ICON_NONE;
                        break;
                }
                scope = h - 1;
                return new NavigationPoint(
                        AbstractGameComponent.filterComponentText(m.group(h)),
                        null, m.start(h), scope, hnIcon
                );
            }
        }
        if (h < 7) {
            return null;
        }
        if (m.start(7) >= 0) {
            return new NavigationPoint(AbstractGameComponent.filterComponentText(m.group(7)), null, m.start(7), 0, NavigationPoint.ICON_TITLE);
        }
        if (m.start(8) >= 0) {
            return new NavigationPoint("<DIV>", null, m.start(8), scope, NavigationPoint.ICON_DIV);
        }
        if (m.start(9) >= 0) {
            return new NavigationPoint("<TABLE>", null, m.start(9), scope, NavigationPoint.ICON_TABLE);
        }

        return null;
    }
//    @Override
//    public List<NavigationPoint> getNavigationPoints(String text) {
//        LinkedList<NavigationPoint> list = new LinkedList<>();
//        Matcher m = null;
//        try {
//            m = pat.matcher(text);
//            int scope = 0;
//            while (m.find()) {
//                if (m.start(10) >= 0) {
//                    continue;
//                }
//                int h;
//                for (h = 1; h < 7; ++h) {
//                    if (m.start(h) >= 0) {
//                        Icon hnIcon;
//                        switch(h){
//                            case 1: hnIcon = NavigationPoint.ICON_H1; break;
//                            case 2: hnIcon = NavigationPoint.ICON_H2; break;
//                            case 3: hnIcon = NavigationPoint.ICON_H3; break;
//                            case 4: hnIcon = NavigationPoint.ICON_H4; break;
//                            case 5: hnIcon = NavigationPoint.ICON_H5; break;
//                            case 6: hnIcon = NavigationPoint.ICON_H6; break;
//                            default: hnIcon = NavigationPoint.ICON_NONE; break;
//                        }
//                        scope = h - 1;
//                        list.add(new NavigationPoint(
//                                AbstractGameComponent.filterComponentText(m.group(h)),
//                                null, m.start(h), scope, hnIcon
//                        ));
//                        break;
//                    }
//                }
//                if (h < 7) {
//                    continue;
//                }
//                if (m.start(7) >= 0) {
//                    list.add(new NavigationPoint(AbstractGameComponent.filterComponentText(m.group(7)), null, m.start(7), 0, NavigationPoint.ICON_TITLE));
//                    continue;
//                }
//                if (m.start(8) >= 0) {
//                    list.add(new NavigationPoint("<DIV>", null, m.start(8), scope, NavigationPoint.ICON_DIV));
//                    continue;
//                }
//                if (m.start(9) >= 0) {
//                    list.add(new NavigationPoint("<TABLE>", null, m.start(9), scope, NavigationPoint.ICON_TABLE));
//                }
//            }
//        } catch (Throwable t) {
//            AbstractNavigator.dumpRegExpThrowable(t, text, m);
//        }
//        return list;
//    }
}
