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

/**
 * The Yuggoth theme is a built-in dark mode theme.
 * 
 * @author Christopher G. Jennings <https://cgjennings.ca/contact>
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

        SolidPainter solidBlack = new SolidPainter(Color.BLACK);
        SolidPainter solidWhite = new SolidPainter(Color.WHITE);
        SolidPainter solidGrey = new SolidPainter(new Color(0x7c7d7f));

        defaults.put("MenuBar[Enabled].backgroundPainter", solidBlack);
        defaults.put("MenuBar:Menu[Enabled].textForeground", Color.WHITE);
        defaults.put("MenuBar:Menu[Selected].backgroundPainter", solidGrey);
        defaults.put("MenuBar:Menu[Selected].textForeground", Color.BLACK);
        defaults.put("MenuBar.background", Color.BLACK);
        defaults.put("MenuBar.foreground", Color.WHITE);

        defaults.put("Menu.background", Color.BLACK);
        defaults.put("Menu.foreground", Color.WHITE);
        defaults.put("Menu[Enabled].textForeground", Color.WHITE);

        defaults.put("MenuItem.textForeground", Color.WHITE);
        defaults.put("MenuItem[Enabled].textForeground", Color.WHITE);
        defaults.put("RadioButtonMenuItem[Enabled].textForeground", Color.WHITE);
        defaults.put("CheckBoxMenuItem[Enabled].textForeground", Color.WHITE);

        defaults.put("menu", Color.BLACK);
        defaults.put("menuText", Color.WHITE);


        /*
        defaults.put("", );
        defaults.put("", );
        defaults.put("", );
        defaults.put("", );
        defaults.put("", );
        defaults.put("", );
        defaults.put("", );
        defaults.put("", );
        defaults.put("", );
        */



        /*


Key	Value	Preview
MenuBar.background	#d6d9df (214,217,223)
MenuBar.contentMargins	Insets (2,6,2,6)	Insets (2,6,2,6)
MenuBar.disabled	#d6d9df (214,217,223)
MenuBar.disabledText	#000000 (0,0,0)
MenuBar.font	Font SansSerif 12	Font SansSerif 12
MenuBar.foreground	#000000 (0,0,0)
MenuBar.windowBindings	[Ljava.lang.Object;@5421e554
MenuBar:Menu.contentMargins	Insets (1,4,2,4)	Insets (1,4,2,4)
MenuBar:Menu:MenuItemAccelerator.contentMargins	Insets (0,0,0,0)	Insets (0,0,0,0)
MenuBar:Menu[Disabled].textForeground	#8e8f91 (142,143,145)
MenuBar:Menu[Enabled].textForeground	#232324 (35,35,36)
MenuBar:Menu[Selected].backgroundPainter	Painter	Painter
MenuBar:Menu[Selected].textForeground	#ffffff (255,255,255)
MenuBar[Enabled].backgroundPainter	Painter	Painter
MenuBar[Enabled].borderPainter
        */

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

    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
        defaults.put("TitledBorder.titleColor", new Color(0xeeffff));
    }

    @Override
    public boolean isDarkOnLight() {
        return true;
    }
}
