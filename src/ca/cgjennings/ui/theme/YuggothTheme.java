package ca.cgjennings.ui.theme;

import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKGROUND_PAINTER;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_ERROR;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_OUTPUT;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_BACKGROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_FOREGROUND;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.Objects;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;

/**
 * The Yuggoth theme is a built-in dark mode theme.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class YuggothTheme extends Theme {
    public YuggothTheme() {
    }

    @Override
    public String getThemeName() {
        return "Yuggoth";
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        defaults.put(CONSOLE_BACKROUND, new Color(0x111111));
        defaults.put(CONSOLE_OUTPUT, new Color(0xf3_e193));
        defaults.put(CONSOLE_ERROR, new Color(0xf9_9d39));
        defaults.put(CONSOLE_SELECTION_BACKGROUND, new Color(0xf3_e193));
        defaults.put(CONSOLE_SELECTION_FOREGROUND, new Color(0x3d_4b28));
        defaults.put(CONSOLE_BACKGROUND_PAINTER, new DefaultConsolePainter());

        defaults.put(EDITOR_TAB_BACKGROUND, new Color(0x000000));

        defaults.put( "control", new Color(0x171717) );
        defaults.put( "info", new Color(128,128,128) );
        defaults.put( "nimbusBase", new Color( 9, 15, 25) );

        defaults.put( "nimbusAlertYellow", new Color( 248, 187, 0) );
        defaults.put( "nimbusDisabledText", new Color( 150, 150, 150) );
        defaults.put( "nimbusFocus", new Color(115,164,209) );
        defaults.put( "nimbusGreen", new Color(176,179,50) );
        defaults.put( "nimbusInfoBlue", new Color( 66, 139, 221) );
        defaults.put( "nimbusLightBackground", new Color( 18, 30, 49) );
        defaults.put( "nimbusOrange", new Color(191,98,4) );
        defaults.put( "nimbusRed", new Color(169,46,34) );
        defaults.put( "nimbusSelectedText", new Color( 255, 255, 255) );
        defaults.put( "nimbusSelectionBackground", new Color( 104, 93, 156) );
        defaults.put( "text", new Color( 230, 230, 230) );

        ColorUIResource MENU = new ColorUIResource(0x111111);
        ColorUIResource MENU_TEXT = new ColorUIResource(0xffffff);
        Color MENU_SELECTED = new ColorUIResource(0x685d9c);
        Color MENU_SELECTED_TEXT = MENU_TEXT;
        Color MENU_DISABLED_TEXT = new ColorUIResource(0x555555);
        Color MENU_SHORTCUT = new Color(0x959595);
        Color MENU_SHORTCUT_DISABLED = MENU_DISABLED_TEXT;

        Painter<?> P_MENU = new SolidPainter(MENU);
        Painter<?> P_MENU_SELECTED = new SolidPainter(MENU_SELECTED);

        defaults.put("menu", MENU);
        defaults.put("menuText", MENU_TEXT);

        defaults.put("Menu.disabledText", MENU_DISABLED_TEXT);
        defaults.put("MenuBar.disabledText", MENU_DISABLED_TEXT);
        defaults.put("MenuItem.disabledText", MENU_DISABLED_TEXT);
        defaults.put("MenuBar.foreground", MENU_TEXT);
        defaults.put("MenuItem.textForeground", MENU_TEXT);
        defaults.put("MenuBar:Menu[Enabled].textForeground", MENU_TEXT);
        defaults.put("Menu[Enabled].textForeground", MENU_TEXT);
        defaults.put("MenuItem[Enabled].textForeground", MENU_TEXT);
        defaults.put("RadioButtonMenuItem.textForeground", MENU_TEXT);
        defaults.put("RadioButtonMenuItem[Enabled].textForeground", MENU_TEXT);
        defaults.put("CheckBoxMenuItem.foreground", MENU_TEXT);
        defaults.put("CheckBoxMenuItem.textForeground", MENU_TEXT);
        defaults.put("CheckBoxMenuItem[Enabled].textForeground", MENU_TEXT);
        defaults.put("MenuBar:Menu[Selected].textForeground", MENU_SELECTED_TEXT);
        defaults.put("MenuBar:Menu[Disabled].textForeground", MENU_DISABLED_TEXT);
        defaults.put("Menu[Disabled].foreground", MENU_DISABLED_TEXT);
        defaults.put("Menu[Disabled].textForeground", MENU_DISABLED_TEXT);
        defaults.put("MenuItem[Disabled].textForeground", MENU_DISABLED_TEXT);
        defaults.put("RadioButtonMenuItem.disabledText", MENU_DISABLED_TEXT);
        defaults.put("RadioButtonMenuItem[Disabled].textForeground", MENU_DISABLED_TEXT);
        defaults.put("CheckBoxMenuItem.disabledText", MENU_DISABLED_TEXT);
        defaults.put("CheckBoxMenuItem[Disabled].foreground", MENU_DISABLED_TEXT);
        defaults.put("CheckBoxMenuItem[Disabled].textForeground", MENU_DISABLED_TEXT);
        defaults.put("MenuItem:MenuItemAccelerator.textForeground", MENU_SHORTCUT);
        defaults.put("MenuItem:MenuItemAccelerator[Disabled].textForeground", MENU_SHORTCUT_DISABLED);
        defaults.put("MenuBar[Enabled].backgroundPainter", P_MENU);
        defaults.put("MenuBar:Menu[Selected].backgroundPainter", P_MENU_SELECTED);
        defaults.put("MenuBar.background", MENU);
        defaults.put("PopupMenu.background", MENU);
        defaults.put("PopupMenu[Disabled].backgroundPainter", P_MENU);
        defaults.put("PopupMenu[Enabled].backgroundPainter", P_MENU );
        defaults.put("PopupMenuSeparator[Enabled].backgroundPainter", P_MENU);
        defaults.put("PopupMenuSeparator.contentMargins", new InsetsUIResource(2, 0, 3, 0));
        defaults.put("Menu.backgroundPainter", MENU);
        defaults.put("Menu[Enabled].backgroundPainter", P_MENU);
        defaults.put("Menu[Disabled].backgroundPainter", P_MENU);
        defaults.put("Menu[Enabled+Selected].backgroundPainter", P_MENU_SELECTED);
        defaults.put("MenuItem.background", MENU);
        defaults.put("MenuItem[Enabled].backgroundPainter", P_MENU);
        defaults.put("MenuItem[Disabled].backgroundPainter", P_MENU);
        defaults.put("MenuItem[MouseOver].backgroundPainter", P_MENU_SELECTED);
        defaults.put("RadioButtonMenuItem[Enabled].backgroundPainter", P_MENU);
        defaults.put("RadioButtonMenuItem[Disabled].backgroundPainter", P_MENU);
        defaults.put("RadioButtonMenuItem[MouseOver].backgroundPainter", P_MENU_SELECTED);
        defaults.put("RadioButtonMenuItem[MouseOver+Selected].backgroundPainter", P_MENU_SELECTED);
        defaults.put("CheckBoxMenuItem[Enabled].backgroundPainter", P_MENU);
        defaults.put("CheckBoxMenuItem[Disabled].backgroundPainter", P_MENU);
        defaults.put("CheckBoxMenuItem[MouseOver].backgroundPainter", P_MENU_SELECTED);
        defaults.put("CheckBoxMenuItem[MouseOver+Selected].backgroundPainter", P_MENU_SELECTED);

        Object painter = defaults.get("CheckBoxMenuItem[MouseOver+Selected].checkIconPainter");
        if(painter != null) {
            defaults.put("CheckBoxMenuItem[Enabled+Selected].checkIconPainter", painter);
        }
        painter = defaults.get("RadioButtonMenuItem[MouseOver+Selected].checkIconPainter");
        if(painter != null) {
            defaults.put("RadioButtonMenuItem[Enabled+Selected].checkIconPainter", painter);
        }
        painter = defaults.get("Menu[Enabled+Selected].arrowIconPainter");
        if(painter != null) {
            defaults.put("Menu[Enabled].arrowIconPainter", painter);
        }

        defaults.put("SplitPane.background", new Color(0x1b1d22));
        defaults.put("TitledBorder.titleColor", new Color(0xeeffff));
    }
    

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
        modifyManagerDefaults(defaults);
    }



    private static class SolidPainter implements Painter<Component> {
        private final Paint paint;
        public SolidPainter(final Paint paint) {
            this.paint = Objects.requireNonNull(paint);
        }

        @Override
        public void paint(Graphics2D g, Component c, int width, int height) {
            g.setPaint(paint);
            g.fillRect(0, 0, width, height);
        }
    }

    private static class OutlinedPainter implements Painter<Component> {
        private final Paint paint, outline;

        public OutlinedPainter(final Paint paint, final Paint outline) {
            this.paint = Objects.requireNonNull(paint);
            this.outline = Objects.requireNonNull(outline);
        }

        @Override
        public void paint(Graphics2D g, Component c, int width, int height) {
            g.setPaint(paint);
            g.fillRect(0, 0, width, height);
            g.setPaint(outline);
            g.fillRect(0, 0, width, height);
        }
    }

    @Override
    public boolean isDark() {
        return true;
    }
}
