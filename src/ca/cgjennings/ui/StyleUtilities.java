package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.IllegalComponentStateException;
import java.awt.Window;
import java.lang.reflect.Method;
import javax.swing.JButton;
import javax.swing.JComponent;

/**
 * Utility methods to set style client properties.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class StyleUtilities {

    public static String MINI = "mini";
    public static String SMALL = "small";
    public static String REGULAR = "regular";
    public static String LARGE = "large";

    private static final String SIZE = "JComponent.sizeVariant";

//	private static String[] SEARCH_FIELD_KEYS = new String[] {
//		"Quaqua.TextField.style",
//		"JTextField.variant"
//	};
//
//	private static String[] SEARCH_FIELD_VALUES = new String[] {
//		"search",
//		"search"
//	};
//
//	public static void searchField( JTextField searchField, JLabel labelToHide ) {
//		if( PlatformSupport.isUsingAquaDerivedLookAndFeel() ) {
//			for( int i=0; i<SEARCH_FIELD_KEYS.length; ++i ) {
//				searchField.putClientProperty( SEARCH_FIELD_KEYS[i], SEARCH_FIELD_VALUES[i] );
//			}
//			if( labelToHide != null ) {
//				labelToHide.setVisible( false );
//			}
//		} else {
//			labelToHide.setIcon( new ImageIcon( findIconURL ) );
//			labelToHide.setText( " " );
//			labelToHide.setIconTextGap( 0 );
//		}
//	}
    public static void colorWell(JButton button) {
        button.putClientProperty("Quaqua.Button.style", "colorWell");
    }

    public static void size(JComponent comp, Object sizeValue) {
        // WORKAROUND: weird jumping font size bug in Nimbus
//        comp.putClientProperty(SIZE, sizeValue);

        float adjust = 0f;
        if (sizeValue.equals(SMALL) || sizeValue.equals(MINI)) {
            adjust = -1f;
        } else if (sizeValue.equals(LARGE)) {
            adjust = +2f;
        }
        if (adjust != 0f) {
            Font old = comp.getFont();
            comp.setFont(old.deriveFont(Math.max(8f, old.getSize2D() + adjust)));
        }
    }

    public static void sizeTree(JComponent parent, Object sizeValue) {
        sizeTree(parent, sizeValue, false);
    }

    public static void sizeTree(JComponent parent, Object sizeValue, boolean applyToParent) {
        for (int i = 0; i < parent.getComponentCount(); ++i) {
            Component child = parent.getComponent(i);
            if (child instanceof JComponent) {
                sizeTree((JComponent) child, sizeValue, true);
            }
        }
        if (applyToParent) {
            size(parent, sizeValue);
        }
    }

    public static void mini(JComponent comp) {
        size(comp, MINI);
    }

    public static void small(JComponent comp) {
        size(comp, SMALL);
    }

    public static void regular(JComponent comp) {
        size(comp, REGULAR);
    }

    public static void large(JComponent comp) {
        size(comp, LARGE);
    }

    /**
     * Returns the opacity of the window. This method can use either the JDK7 or
     * JDK6u10 APIs for window opacity. If neither is available or opacity
     * changes have been disabled, the method returns 1 (i.e., it assumes that
     * the window is fully opaque).
     *
     * @param window the window to obtain the opacity of
     * @throws NullPointerException if {@code window} is {@code null}
     */
    public static float getWindowOpacity(Window window) {
        if (window == null) {
            throw new NullPointerException("window");
        }
        if (enableOpacity) {
            if (getOpacityMethod == -1) {
                getOpacityMethod = 0;
                try {
                    getWindowOpacity = Class.forName("com.sun.awt.AWTUtilities")
                            .getMethod("getWindowOpacity", Window.class);
                    getOpacityMethod = 2;
                } catch (Exception e) {
                    try {
                        getWindowOpacity = java.awt.Window.class.getMethod("getOpacity");
                        getOpacityMethod = 1;
                    } catch (Exception ie) {
                    }
                }
            }

            // if an API is known, try calling it to set the opacity
            try {
                if (getOpacityMethod == 1) {
                    return (Float) getWindowOpacity.invoke(window);
                } else if (getOpacityMethod == 2) {
                    return (Float) getWindowOpacity.invoke(null, window);
                }
            } catch (Exception e) {
                // weird! this shouldn't happen, but just in case...
                getOpacityMethod = 0;
            }
        }
        return 1f;
    }
    private static Method getWindowOpacity = null;
    private static int getOpacityMethod = -1; // -1 = unknown; 0 = none;  1 = JDK7; 2 = JDK6

    /**
     * Sets the translucency of a window if possible. This method can use either
     * the JDK7 or JDK6u10 APIs for setting window opacity. If neither is
     * available, or if window translucency is unsupported or has been disabled,
     * then this method has no effect.
     *
     * @param window the window to change the opacity of
     * @param alpha the new opacity value in the range [0..1]
     * @throws NullPointerException if window is null
     * @throws IllegalArgumentException if alpha is not in range
     * @throws IllegalComponentStateException if alpha &lt; 1 is set on a full
     * screen window
     * @see #setOpacityChangeEnabled(boolean)
     */
    public static void setWindowOpacity(Window window, float alpha) {
        if(alpha < 1f) {
            if(!enableOpacity) {
                return;
            }
            if(!window.getGraphicsConfiguration().getDevice().isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
                return;
            }
            if (window instanceof Frame && !((Frame) window).isUndecorated()) {
                return;
            }
            if (window instanceof Dialog && !((Dialog) window).isUndecorated()) {
                return;
            }
            if( window == window.getGraphicsConfiguration().getDevice().getFullScreenWindow()) {
                return;
            }
        }
        window.setOpacity(alpha);
    }

    public static void setOpacityChangeEnabled(boolean enable) {
        enableOpacity = enable;
    }

    public static boolean isOpacityChangeEnabled() {
        return enableOpacity;
    }

    private static boolean enableOpacity = true;

    private StyleUtilities() {
    }
}
