package ca.cgjennings.ui.theme;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.BlurFilter;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKGROUND_PAINTER;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_BACKROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_ERROR;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_OUTPUT;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_BACKGROUND;
import static ca.cgjennings.ui.theme.Theme.CONSOLE_SELECTION_FOREGROUND;
import static ca.cgjennings.ui.theme.Theme.EDITOR_TAB_BACKGROUND;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.InsetsUIResource;
import resources.ResourceKit;

/**
 * The Dagon theme is a built-in theme that is a dark mode verison of
 * {@link HydraTheme}. It features subtle grey-blue colours with greeen
 * and orange highlights and an octopus motif.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public class DagonTheme extends Theme {

    public DagonTheme() {
    }

    @Override
    public String getThemeName() {
        return "Dagon";
    }

    @Override
    public void modifyManagerDefaults(UIDefaults defaults) {
        defaults.put(CONSOLE_BACKROUND, new Color(9, 15, 25));
        defaults.put(CONSOLE_OUTPUT, new Color(0xf3e193));
        defaults.put(CONSOLE_ERROR, new Color(0xf99d39));
        defaults.put(CONSOLE_SELECTION_BACKGROUND, new Color(0xf3e193));
        defaults.put(CONSOLE_SELECTION_FOREGROUND, new Color(0x3d4b28));
        defaults.put(CONSOLE_BACKGROUND_PAINTER, new BackdropPainter());
        
        defaults.put(EDITOR_TAB_BACKGROUND, new Color(0x1b2327));
        
        defaults.put(CONTEXT_BAR_BACKGROUND, Color.BLACK);
        defaults.put(CONTEXT_BAR_FOREGROUND, new Color(0x424548));
        defaults.put(CONTEXT_BAR_BUTTON_BACKGROUND, Color.BLACK);

        // Primary colours
        // not what you might think
        defaults.put("control", new ColorUIResource(0x455a64));
        // controls with shading gradients: buttons, scrollbars, etc.
        defaults.put("nimbusBase", new ColorUIResource(0x060809));
        defaults.put("nimbusAlertYellow", new ColorUIResource(0xf8bb00));
        defaults.put("nimbusDisabledText", new ColorUIResource(0x969696));
        defaults.put("nimbusFocus", new ColorUIResource(0xffc107));
        defaults.put("nimbusGreen", new ColorUIResource(0xb0b332));
        defaults.put("nimbusInfoBlue", new ColorUIResource(0x428bdd));    
        defaults.put("nimbusOrange", new ColorUIResource(0xb46204));
        defaults.put("nimbusRed", new ColorUIResource(0xa92e22));
        
        // Text controls
        Color TEXT = new ColorUIResource(0xf7f7f7);
        Color SELECTION_BACKGROUND = new ColorUIResource(0x685d9c);
        defaults.put("text", TEXT);
        defaults.put("nimbusSelectedText", TEXT);
        defaults.put("nimbusSelection", SELECTION_BACKGROUND);
        defaults.put("nimbusSelectionBackground", SELECTION_BACKGROUND);
        defaults.put("TextField.selectionForeground", TEXT);
        defaults.put("TextField.selectionBackground", SELECTION_BACKGROUND);
        defaults.put("TextField.caretForeground", TEXT);
        
        // Panels
        final Color BACKGROUND = new ColorUIResource(0x455a64);
        defaults.put("background", BACKGROUND);
        defaults.put("TextArea.background", BACKGROUND);
        defaults.put("FileChooser.background", BACKGROUND);
        defaults.put("RootPane.background", BACKGROUND);        
        
        // Lists, trees, etc.
        final Color BACKGROUND_LIST = new ColorUIResource(0x37474f);
        defaults.put("nimbusLightBackground", BACKGROUND_LIST);
        
        // Tool tips
        final Color TIP_BACKGROUND = new ColorUIResource(0x000000);
        final Color TIP_FOREGROUND = new ColorUIResource(0xffffff);
        defaults.put("info", TIP_BACKGROUND);
        defaults.put("infoText", TIP_FOREGROUND);
        defaults.put("ToolTip.foreground", TIP_FOREGROUND);

        // Menus
        Color MENU = new ColorUIResource(0x111111);
        Color MENU_TEXT = new ColorUIResource(0xffffff);
        Color MENU_SELECTED = new ColorUIResource(0x685d9c);
        Color MENU_SELECTED_TEXT = MENU_TEXT;
        Color MENU_DISABLED_TEXT = new ColorUIResource(0x555555);
        Color MENU_SHORTCUT = new ColorUIResource(0x959595);
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
        defaults.put("PopupMenu[Enabled].backgroundPainter", P_MENU);
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
        if (painter != null) {
            defaults.put("CheckBoxMenuItem[Enabled+Selected].checkIconPainter", painter);
        }
        painter = defaults.get("RadioButtonMenuItem[MouseOver+Selected].checkIconPainter");
        if (painter != null) {
            defaults.put("RadioButtonMenuItem[Enabled+Selected].checkIconPainter", painter);
        }
        painter = defaults.get("Menu[Enabled+Selected].arrowIconPainter");
        if (painter != null) {
            defaults.put("Menu[Enabled].arrowIconPainter", painter);
        }
    }
    
    @Override
    public void modifyLookAndFeelDefaults(UIDefaults defaults) {
        Color CON_BACKGROUND = new Color(0x3d4b28);
        Color CON_TEXT = new Color(0xf3e193);
        Color CON_ERROR_TEXT = new Color(0xf99d39);
        Color CON_SELECTION = new Color(0xf3e193);
        Color CON_SELECTION_TEXT = new Color(0x3d4b28);

        defaults.put(CONSOLE_BACKROUND, CON_BACKGROUND);
        defaults.put(CONSOLE_OUTPUT, CON_TEXT);
        defaults.put(CONSOLE_ERROR, CON_ERROR_TEXT);
        defaults.put(CONSOLE_SELECTION_BACKGROUND, CON_SELECTION);
        defaults.put(CONSOLE_SELECTION_FOREGROUND, CON_SELECTION_TEXT);
        defaults.put(CONSOLE_BACKGROUND_PAINTER, new CachingPainter<>(new BackdropPainter()));
        
        modifyManagerDefaults(defaults);
    }

    private static class BackdropPainter implements Painter<JComponent> {

        final Color CON_BACKGROUND = new Color(0x3d4b28);
        final Color CON_BACKGROUND_OUTER = CON_BACKGROUND.darker();
        final Color CON_BACKGROUND_INNER = CON_BACKGROUND.brighter();
        final Color CON_BACKGROUND_INNER2 = CON_BACKGROUND_INNER.brighter();
        BufferedImage stencil;

        @Override
        public void paint(Graphics2D g, JComponent o, int w, int h) {
            if (stencil == null) {
                stencil = ResourceKit.getImage("icons/octopus.png");
                if (o instanceof JDesktopPane) {
                    int lpad = 0, tpad;
                    tpad = stencil.getWidth() - stencil.getHeight();
                    if (tpad < 0) {
                        lpad = -tpad;
                        tpad = 0;
                    }

                    int rpad = lpad / 2;
                    lpad -= rpad;

                    int bpad = tpad / 2;
                    tpad -= bpad;

                    stencil = ImageUtilities.pad(stencil, 12 + tpad, 12 + lpad, 12 + bpad, 12 + rpad);
                    stencil = new BlurFilter(6, 6, 2).filter(stencil, null);
                }
            }

            Composite c = g.getComposite();
            Paint p = g.getPaint();
            int cx = w / 2, cy = h / 2;

            if (o instanceof JDesktopPane) {
                g.setPaint(CON_BACKGROUND_OUTER);
                g.fillRect(0, 0, w, h);
                int size = Math.max(w, h) * 80 / 100;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.075f));
                g.drawImage(stencil, cx - size / 2, cy - size / 2, size, size, null);
            } else {
                RadialGradientPaint rgp = new RadialGradientPaint(
                        cx, cy, Math.max(w, h),
                        new float[]{0f, 0.05f, 0.2f, 0.8f, 1f},
                        new Color[]{CON_BACKGROUND_INNER2, CON_BACKGROUND_INNER, CON_BACKGROUND, CON_BACKGROUND_OUTER, CON_BACKGROUND_OUTER}
                );
                g.setPaint(rgp);
                g.fillRect(0, 0, w, h);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                g.drawImage(stencil, cx - stencil.getWidth() / 2, cy - stencil.getHeight() / 2, null);
            }
            g.setPaint(p);
            g.setComposite(c);
        }
    }
    
    @Override
    public String getThemeGroup() {
        return "\udbff\udfff_98";
    }

    @Override
    public boolean isDark() {
        return true;
    }    
}
