package ca.cgjennings.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import resources.ResourceKit;

/**
 * An abstract base class for creating new themes that are variants of the
 * built-in themes. Methods are provided that allow you to create a theme with
 * an alternative colour scheme with a minimum of effort.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractBaseTheme extends Theme {

    private boolean hasAlpha = false;

    public AbstractBaseTheme() {
    }

    private HashMap<String, Object> uid;

    private void set(String key, int argbColourValue) {
        set(key, new Color(argbColourValue, hasAlpha));
    }

    /**
     * Sets whether translucency is used when setting colour values. By default,
     * the alpha bits of the colour values passed to various protected "set"
     * methods are ignored. This method can be used to change this; specifying
     * {@code true} will caused the highest 8 bits of colour values to be
     * used for the alpha value.
     *
     * @param obeyAlphaBits if true, colour values are interpreted as ARGB
     * values rather than RGB values
     */
    protected void useAlpha(boolean obeyAlphaBits) {
        hasAlpha = true;
    }

    /**
     * This is called to allow subclasses to customize the theme by calling the
     * base theme's various protected "set" methods.
     */
    protected abstract void initializeTheme();

    /**
     * Sets the base background colour. This primarily affects the background
     * colour of panels and menus.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setBackgroundBase(int argb) {
        set("*control", argb);
    }

    /**
     * Sets the base colour used for controls. This primarily affects the colour
     * of buttons and combo boxes.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setForegroundBase(int argb) {
        set("*nimbusBase", argb);
    }

    /**
     * Sets the base colour used for the ring drawn around the control that has
     * input focus.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setFocusRingBase(int argb) {
        set("*nimbusFocus", argb);
    }

    /**
     * Sets the base colour used as a contrasting colour with the foreground
     * base. Used primarily by progress bars.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setContrastBase(int argb) {
        set("*nimbusOrange", argb);
    }

    /**
     * Sets the foreground colour for text.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setTextForeground(int argb) {
        set("*text", argb);
    }

    /**
     * Sets the background colour for text editing controls, trees, and other
     * controls that typically have a light background colour.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setTextBackground(int argb) {
        set("*nimbusLightBackground", argb);
    }

    /**
     * Sets the foreground colour for text selections.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setTextSelectionForeground(int argb) {
        set("*nimbusSelectedText", argb);
    }

    /**
     * Sets the background colour for text selections.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setTextSelectionBackground(int argb) {
        set("*nimbusSelectionBackground", argb);
    }

    /**
     * Sets the background colour for (enabled) tool tips.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setToolTipBackground(int argb) {
        set("*info", argb);
    }

    /**
     * Sets the background colour of the script output console. The background
     * colour is only used if no painter is set.
     *
     * @param argb RGB value of the base colour to use
     * @see #setConsoleBackgroundPainter
     */
    protected void setConsoleBackground(int argb) {
        set(CONSOLE_BACKROUND, argb);
    }

    /**
     * Sets the background painter of the script output console.
     *
     * @param painter the script console background painter
     */
    protected void setConsoleBackgroundPainter(Painter painter) {
        set(CONSOLE_BACKGROUND_PAINTER, painter);
    }

    /**
     * Sets the foreground colour of the script output console's output stream.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setConsoleOutputForeground(int argb) {
        set(CONSOLE_OUTPUT, argb);
    }

    /**
     * Sets the foreground colour of the script output console's error stream.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setConsoleErrorForeground(int argb) {
        set(CONSOLE_ERROR, argb);
    }

    /**
     * Sets the background colour of selected text in the script console window.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setConsoleSelectionBackground(int argb) {
        set(CONSOLE_SELECTION_BACKGROUND, argb);
    }

    /**
     * Sets the foreground colour of selected text in the script console window.
     *
     * @param argb RGB value of the base colour to use
     */
    protected void setConsoleSelectionForeground(int argb) {
        set(CONSOLE_SELECTION_FOREGROUND, argb);
    }

    /**
     * Sets the base font used for the script console window.
     *
     * @param font
     */
    protected void setConsoleFont(Font font) {
        set(CONSOLE_FONT, font);
    }

    /**
     * Sets the painter used to draw the backdrop of the document space when no
     * documents are open.
     *
     * @param backdrop the empty document space backdrop
     */
    protected void setDocumentBackdropPainter(Painter backdrop) {
        set("DesktopPane[Enabled].backgroundPainter", backdrop);
    }

    /**
     * Sets the base font used for interface text.
     *
     * @param font the base font for all controls
     */
    protected void setFontBase(Font font) {
        if (font == null) {
            font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        set("*defaultFont", font);
    }

    /**
     * Sets the font used for panel titles.
     *
     * @param font the font to use for the titled border on panels
     */
    protected void setPanelTitleFont(Font font) {
        set("TitledBorder.font", font);
    }

    /**
     * Returns the font that is the best match for the given specification. The
     * font is specified using a comma-separated list of font family names. The
     * first font family that is available will be used; if no font in the list
     * is available, then a default sans serif font family is used.
     *
     * @param families a comma-separated list of font family names
     * @param style the font style bitmap, as per
     * {@link Font#Font(java.lang.String, int, int)}
     * @param size the point size of the font
     * @return a font with the specified size and style whose family is the
     * first family in the list that is installed on this system
     * @see ResourceKit#findAvailableFontFamily
     */
    protected static Font matchFont(String families, int style, int size) {
        String family = ResourceKit.findAvailableFontFamily(families, Font.SANS_SERIF);
        return new Font(family, style, size);
    }

    /**
     * Sets the value of an arbitrary look and feel default key. This can be
     * used to customize aspects of the theme that cannot be modified via
     * existing helper methods.
     *
     * @param key the key name of the default to modify
     * @param value the value of the default to modify
     * @throws IllegalArgumentException if the key is {@code null} or empty
     */
    protected void set(String key, Object value) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException();
        }
        uid.put(key, value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * {@code AbstractBaseTheme} makes this method final, so the underlying
     * look and feel cannot be changed.
     */
    @Override
    public final String getLookAndFeelClassName() {
        return super.getLookAndFeelClassName();
    }

    @Override
    public final void modifyManagerDefaults(UIDefaults defaults) {
        uid = new HashMap<>();
        initializeTheme();
        install(defaults);
        if (!uid.containsKey(CONSOLE_BACKGROUND_PAINTER)) {
            defaults.put(CONSOLE_BACKGROUND_PAINTER, new DefaultConsolePainter());
        }
    }

    @Override
    public final void modifyLookAndFeelDefaults(UIDefaults defaults) {
        install(defaults);
        uid = null; // allow setup hash to be GC'd
    }

    private Color derive(Color base, float h, float s, float b) {
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        hsb[0] += h;
        hsb[1] *= s;
        hsb[2] *= b;
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }

    private void derive(UIDefaults defaults, String key, String baseKey, float h, float s, float b) {
        Color c = defaults.getColor(baseKey);
        if (c != null) {
            defaults.put(key, derive(c, h, s, b));
        }
    }

    private void install(UIDefaults defaults) {
        // give priority to keys marked with '*' prefix
        for (String key : uid.keySet()) {
            if (key.charAt(0) == '*') {
                defaults.put(key.substring(1), uid.get(key));
            }
        }

        // derive defaults for SE-specific colour keys,
        // before installing any overrides
        derive(defaults, EDITOR_TAB_BACKGROUND, "nimbusBase", 0f, 1f, 0.8f);

        // apply default font key to .font keys because
        // these are already "derived" by Nimbus by this time
        Object defaultFont = uid.get("defaultFont");
        if (defaultFont != null) {
            for (Object key : defaults.keySet()) {
                if (!(key instanceof String)) {
                    continue;
                }
                if (((String) key).endsWith(".font")) {
                    Object val = defaults.get(key);
                    if (val instanceof Font) {
                        defaults.put(key, defaultFont);
                    }
                }
            }

            defaults.put("TitledBorder.font", ((Font) defaultFont).deriveFont(Font.BOLD));
        }

        // now copy all lower priority keys
        for (String key : uid.keySet()) {
            if (key.charAt(0) != '*') {
                defaults.put(key, uid.get(key));
            }
        }
    }

//	/**
//	 * Sets the menu to use custom solid colours instead of the default colours.
//	 * Use -1 to keep the default for any parameter.
//	 *
//	 * @param background background colour for menu items
//	 * @param text text colour
//	 * @param selectedText text colour of selected item
//	 * @param disabledText text colour of disabled items
//	 */
//	protected void MENU_PALETTE( int background, int text, int selectedText, int disabledText ) {
//		if( background != -1 ) {
//			SET( new BorderedSolidPainter( C( background ) ), "PopupMenu[Enabled].backgroundPainter" );
//			SET( new SolidPainter( C( background ) ), "MenuBar[Enabled].backgroundPainter", "Menu[Enabled].backgroundPainter" );
//		}
//		SET( C( text ), cMenuBarMenuEnabled_textForeground, cMenuEnabled_textForeground, cMenuItem_foreground, cMenuItemEnabled_textForeground, cMenu_foreground, cMenuEnabled_textForeground );
//		SET( C( selectedText ), cMenuEnabledSelected_textForeground, cMenuItemEnabled_textForeground, cCheckBoxMenuItemEnabled_textForeground, cRadioButtonMenuItemEnabled_textForeground );
//		SET( C( disabledText ), cMenuBarMenuDisabled_textForeground, cMenuDisabled_textForeground, cMenuItemDisabled_textForeground, cCheckBoxMenuItemDisabled_textForeground, cRadioButtonMenuItemDisabled_textForeground );
//		MENU_ACCELERATOR_PALETTE( text, selectedText, disabledText );
//	}
//
//	/**
//	 * Sets the menu accelerator text to custom colours. Use -1 to keep the
//	 * default for any parameter.
//	 *
//	 * @param plain the regular accelerator colour
//	 * @param selectedText the selected accelerator colour
//	 * @param disabledText the disabled accelerator colour
//	 */
//	protected void MENU_ACCELERATOR_PALETTE( int text, int selectedText, int disabledText ) {
//		SET( C( text ), "MenuItem:MenuItemAccelerator[Enabled].textForeground" );
//		SET( C( selectedText ), cMenuItemMenuItemAcceleratorMouseOver_textForeground );
//		SET( C( disabledText ), cMenuItemMenuItemAcceleratorDisabled_textForeground );
//	}
//	// used to paint menu backgrounds
//	private static class BorderedSolidPainter extends SolidPainter {
//
//		public BorderedSolidPainter( Paint paint ) {
//			super( paint );
//		}
//
//		@Override
//		public void paint( Graphics2D g, Object object, int width, int height ) {
//			super.paint( g, object, width, height );
//			g.setColor( UIManager.getLookAndFeel().getDefaults().getColor( "nimbusBorder" ) );
//			g.drawRect( 0, 0, width - 1, height - 1 );
//		}
//	}
}
