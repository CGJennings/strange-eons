package ca.cgjennings.ui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import resources.ResourceKit;

/**
 * General Swing utilities.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class JUtilities {

    private JUtilities() {
    }

    /**
     * Enable or disable a group list of components.
     */
    public static void enable(boolean enable, JComponent... components) {
        for (JComponent c : components) {
            if (c != null) {
                c.setEnabled(enable);
            }
        }
    }

    public static void enable(boolean enable, JComponent component1, JComponent component2) {
        component1.setEnabled(enable);
        component2.setEnabled(enable);
    }

    /**
     * Recursively enable or disable a tree of components.
     *
     * @param component the root of the component tree
     * @param enable if {@code true}, enable the components, else disable
     * @throws NullPointerException if {@code c} is {@code null}
     */
    public static void enableTree(JComponent component, boolean enable) {
        if (component == null) {
            throw new NullPointerException("component");
        }

        component.setEnabled(enable);
        for (Component child : component.getComponents()) {
            if (child instanceof JComponent) {
                enableTree((JComponent) child, enable);
            }
        }
    }

    /**
     * Returns the window that a component is contained within, if any.
     *
     * @param component the component to find the window of
     * @return the containing window, or {@code null}
     * @throws NullPointerException if {@code c} is {@code null}
     */
    public static Window findWindow(Component component) {
        if (component == null) {
            throw new NullPointerException("component");
        }
        return SwingUtilities.getWindowAncestor(component);
    }

    /**
     * Adjusts a window's location so that the center is under the pointer. This
     * is a cover for
     * {@code snapToPointer( window, ALIGN_CENTER, ALIGN_MIDDLE )}.
     *
     * @param window the window to adjust
     */
    public static void snapToPointer(Window window) {
        snapToPointer(window, ALIGN_CENTER, ALIGN_MIDDLE);
    }

    /**
     * Adjusts a window's location to align it with the pointing device's
     * pointer on screen. The window will be moved as close as possible to the
     * pointer without allowing it to fall outside of the available desktop area
     * any reserved system areas along the edges of that desktop. If there is no
     * pointing device, or the pointer location cannot be determined, the window
     * will be centered.
     *
     * @param window the window to adjust
     * @param halign a horizontal alignment value
     * ({@link #ALIGN_LEFT}, {@link #ALIGN_CENTER}, or {@link #ALIGN_RIGHT})
     * @param valign a vertical alignment value
     * ({@link #ALIGN_TOP}, {@link #ALIGN_MIDDLE}, or {@link #ALIGN_BOTTOM})
     */
    public static void snapToPointer(Window window, int halign, int valign) {
        try {
            Point p = MouseInfo.getPointerInfo().getLocation();
            snapToPoint(window, p, halign, valign);
        } catch (Exception e) {
            window.setLocationRelativeTo(null);
            snapToDesktop(window);
        }
    }

    /**
     * Adjusts a window's location to align it with a point on the screen. The
     * window will be moved as close as possible to the point without allowing
     * it to fall outside of the available desktop area any reserved system
     * areas along the edges of that desktop.
     *
     * @param window the window to adjust
     * @param point the point to align against, in screen coordinates
     * @param halign a horizontal alignment value
     * ({@link #ALIGN_LEFT}, {@link #ALIGN_CENTER}, or {@link #ALIGN_RIGHT})
     * @param valign a vertical alignment value
     * ({@link #ALIGN_TOP}, {@link #ALIGN_MIDDLE}, or {@link #ALIGN_BOTTOM})
     */
    public static void snapToPoint(Window window, Point point, int halign, int valign) {
        if (window == null) {
            throw new NullPointerException("window");
        }
        if (point == null) {
            throw new NullPointerException("point");
        }

        int x = point.x;
        int y = point.y;

        if (halign == ALIGN_CENTER) {
            x -= window.getWidth() / 2;
        } else if (halign >= ALIGN_RIGHT) {
            x -= window.getWidth();
        }

        if (valign == ALIGN_MIDDLE) {
            y -= window.getHeight() / 2;
        } else if (valign >= ALIGN_BOTTOM) {
            y -= window.getHeight();
        }

        snapToDesktopImpl(window, x, y);
    }

    /**
     * Horizontal alignment constant.
     */
    public static int ALIGN_LEFT = -1;
    /**
     * Horizontal alignment constant.
     */
    public static int ALIGN_CENTER = 0;
    /**
     * Horizontal alignment constant.
     */
    public static int ALIGN_RIGHT = 1;
    /**
     * Vertical alignment constant.
     */
    public static int ALIGN_TOP = -1;
    /**
     * Vertical alignment constant.
     */
    public static int ALIGN_MIDDLE = 0;
    /**
     * Vertical alignment constant.
     */
    public static int ALIGN_BOTTOM = 1;

    /**
     * Adjusts a window's location so that it does not extend beyond the edge of
     * the display or overlap system areas (such as the task bar on Windows). If
     * the window cannot fit completely in this area, then it will be resized to
     * the available area.
     *
     * @param window the window to adjust
     */
    public static void snapToDesktop(Window window) {
        if (window == null) {
            throw new NullPointerException("window");
        }
        snapToDesktopImpl(window, window.getX(), window.getY());
    }

    /**
     * Internal implementation that snaps a window to the desktop bounds as if
     * it was currently at the specified (x,y) position.
     *
     * @param window window (not explicitly {@code null} checked)
     * @param x the x coord to pretend the window is at (may or may not be
     * {@code w.getX()})
     * @param y the y coord to pretend the window is at (may or may not be
     * {@code w.getY()})
     */
    private static void snapToDesktopImpl(Window window, int x, int y) {
        // get the rectangle of the window's display
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        if (gc == null) {
            return;
        }
        Rectangle bounds = gc.getBounds();

        // get the insets of the system areas
        Insets insets = window.getToolkit().getScreenInsets(gc);

        // adjust the raw screen rectangle to exclude the system areas
        bounds.x += insets.left;
        bounds.width -= insets.left;

        bounds.y += insets.top;
        bounds.height -= insets.top;

        bounds.width -= insets.right;

        bounds.height -= insets.bottom;

        // the window's rectangle
        Rectangle r = new Rectangle(x, y, window.getWidth(), window.getHeight());

        // snap to top/left edges
        if (r.x < bounds.x) {
            r.x = bounds.x;
        }
        if (r.y < bounds.y) {
            r.y = bounds.y;
        }

        // snap to bottom/right edges
        if (r.x + r.width > bounds.x + bounds.width) {
            r.x = (bounds.x + bounds.width) - r.width;
        }
        if (r.y + r.height > bounds.y + bounds.height) {
            r.y = (bounds.y + bounds.height) - r.height;
        }

        // if that moved us past the top/left edge, resize
        if (r.x < bounds.x) {
            r.x = bounds.x;
            r.width = bounds.width;
        }
        if (r.y < bounds.y) {
            r.y = bounds.y;
            r.height = bounds.height;
        }

        window.setBounds(r);
    }

    /**
     * Adds listeners for all addXXXListener methods on a component that print
     * information to a selected output stream.
     *
     * @param out the stream to print to ({@code System.err} if {@code null})
     * @param component the component to add listeners to
     * @param includeAllMoveEvents if {@code true}, low-level cursor movements
     * are included
     * @param recursive if {@code true}, listeners are added recursively to
     * children
     */
    public static void addDebugListeners(final PrintStream out, final Object component, final boolean includeAllMoveEvents, boolean recursive) {
        final PrintStream outstream = out == null ? System.err : out;

        // :: Find all add*Listener methods for supplied component.
        List<Class<?>> listenerInterfaces = new ArrayList<>();
        List<Method> addListenerMethods = new ArrayList<>();
        Method[] methods = component.getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("add") && name.endsWith("Listener")) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 1) {
                    Class<?> interfaze = parameters[0];
                    if (interfaze.isInterface()) {
                        listenerInterfaces.add(interfaze);
                        addListenerMethods.add(method);
                    }
                }
            }
        }

        // :: Make handler for "super listener"
        InvocationHandler handler = new InvocationHandler() {

            long _lastEvent;
            String _lastMethodName;
            boolean _threadFired;
            InvocationHandler _handler = this;

            @Override
            public synchronized Object invoke(@SuppressWarnings("unused") Object proxy, Method method, Object[] args)
                    throws Throwable {
                _lastEvent = System.currentTimeMillis();
                String methodName = method.getName();
                if (includeAllMoveEvents || !(methodName.endsWith("Moved") && methodName.equals(_lastMethodName))) {
                    String name = null;
                    if (args.length > 0 && args[0] instanceof AWTEvent) {
                        Object source = ((AWTEvent) args[0]).getSource();

                        if (source instanceof Component) {
                            name = ((Component) source).getName();
                        }
                    }
                    outstream.println("event:[" + method.getName() + "] - on - [" + method.getDeclaringClass().getName()
                            + (name != null ? " \"" + name + "\"" : "") + "] - with - " + Arrays.asList(args) + ".");
                    if (!_threadFired) {
                        _threadFired = true;
                        new Thread("debug:Event Stream Breaker") {

                            @Override
                            public void run() {
                                while (true) {
                                    long millisLeft;
                                    synchronized (_handler) {
                                        millisLeft = 400 - (System.currentTimeMillis() - _lastEvent);
                                        if (millisLeft < 0) {
                                            outstream.println("===== event stream break.");
                                            _threadFired = false;
                                            _lastMethodName = null;
                                            break;
                                        }
                                    }
                                    try {
                                        Thread.sleep(millisLeft + 2);
                                    } catch (InterruptedException e) {
                                        break;
                                    }
                                }
                            }
                        }.start();
                    }
                }
                _lastMethodName = methodName;
                return null;
            }
        };

        // :: Make "super listener", "implementing" all the Listener interfaces
        Object superListener = Proxy.newProxyInstance(JUtilities.class.getClassLoader(),
                listenerInterfaces.toArray(Class<?>[]::new), handler);

        // :: Attach "super listener" using all add*Listener methods on supplied component
        for (Method method : addListenerMethods) {
            try {
                method.invoke(component, superListener);
                outstream.println(" + add*Listener: [" + method + "].");
            } catch (Throwable e) {
                outstream.println(" * Exception while adding listener: " + method);
                e.printStackTrace(outstream);
            }
        }

        // :: Apply recursively
        if (recursive && (component instanceof Container)) {
            Container parent = (Container) component;
            for (int kid = 0; kid < parent.getComponentCount(); ++kid) {
                addDebugListeners(
                        outstream, parent.getComponent(kid),
                        includeAllMoveEvents, recursive
                );
            }
        }
    }

    /**
     * Handles wait cursor change from non-EDT.
     *
     * @param show
     * @param c
     */
    private static void doCursorChangeLater(final boolean show, final Component c) {
        EventQueue.invokeLater(() -> {
            if (show) {
                showWaitCursor(c);
            } else {
                hideWaitCursor(c);
            }
        });
    }

    public static void showWaitCursor(Component c) {
        if (!EventQueue.isDispatchThread()) {
            doCursorChangeLater(true, c);
            return;
        }
        JRootPane root = SwingUtilities.getRootPane(c);
        if (root == null) {
            throw new IllegalStateException("component not in a JWindow: " + c);
        }

        Integer level = waitMap.get(root);
        if (level == null) {
            waitMap.put(root, 1);
            Component glass = root.getGlassPane();
            glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            glass.setVisible(true);
        } else {
            waitMap.put(root, level + 1);
        }
    }

    public static void hideWaitCursor(Component c) {
        if (!EventQueue.isDispatchThread()) {
            doCursorChangeLater(false, c);
            return;
        }
        JRootPane root = SwingUtilities.getRootPane(c);
        if (root == null) {
            throw new IllegalStateException("component not in a JWindow: " + c);
        }

        Integer level = waitMap.get(root);
        if (level <= 1) {
            waitMap.remove(root);
            Component glass = root.getGlassPane();
            glass.setVisible(false);
            glass.setCursor(Cursor.getDefaultCursor());
        } else {
            waitMap.put(root, level - 1);
        }
    }

    // shows and hides should always be paired, so this should not leak
    private static HashMap<JRootPane, Integer> waitMap = new HashMap<>();

    /**
     * Returns {@code true} if the mouse button with the indicated number is
     * down. If the button number is greater than the number of buttons
     * recognized by this JVM, {@code false} is returned. Buttons from 1 to 3
     * can always be recognized; higher-numbered buttons may be recognized. The
     * highest possible button number that might be recognized is
     * {@link java.awt.MouseInfo#getNumberOfButtons()}. Button numbers higher
     * than this can be safely requested but will return {@code false}.
     *
     * @param button the number of the button (1 for BUTTON1, etc.) to test for
     * @param e the input event to test
     * @return {@code true} if the button can be detected and is down
     * @throws IllegalArgumentException if the button number is less than 1
     */
    public static boolean isButtonDown(int button, InputEvent e) {
        --button;
        if (button < 0) {
            throw new IllegalArgumentException("button < 1: " + button);
        }
        if (button >= BUTTON_MASKS.length) {
            return false;
        }
        return (e.getModifiersEx() & BUTTON_MASKS[button]) == BUTTON_MASKS[button];
    }

    /**
     * Returns {@code true} if the left mouse button was held down during the
     * given {@code InputEvent}.
     *
     * @param e the event to test
     * @return {@code true} is mouse button was down
     */
    public static boolean leftButton(InputEvent e) {
        return isButtonDown(1, e);
    }

    /**
     * Returns {@code true} if the middle mouse button was held down during the
     * given {@code InputEvent}.
     *
     * @param e the event to test
     * @return {@code true} is mouse button was down
     */
    public static boolean midButton(InputEvent e) {
        return isButtonDown(2, e);
    }

    /**
     * Returns {@code true} if the right mouse button was held down during the
     * given {@code InputEvent}.
     *
     * @param e the event to test
     * @return {@code true} is mouse button was down
     */
    public static boolean rightButton(InputEvent e) {
        return isButtonDown(3, e);
    }

    /**
     * Returns {@code true} if the "back" mouse button was held down during the
     * given {@code InputEvent} and this button can be detected by this JVM.
     *
     * @param e the event to test
     * @return {@code true} is mouse button was down
     * @see #isButtonDown(int, java.awt.event.InputEvent)
     */
    public static boolean backButton(InputEvent e) {
        return isButtonDown(4, e);
    }

    /**
     * Returns {@code true} if the "forward" mouse button was held down during
     * the given {@code InputEvent} and this button can be detected by this JVM.
     *
     * @param e the event to test
     * @return {@code true} is mouse button was down
     * @see #isButtonDown(int, java.awt.event.InputEvent)
     */
    public static boolean forwardButton(InputEvent e) {
        return isButtonDown(5, e);
    }

    private static final int[] BUTTON_MASKS;

    static {
        // JAVA 7
        final int BTNS = MouseInfo.getNumberOfButtons();
        BUTTON_MASKS = new int[Math.max(3, BTNS)];
        BUTTON_MASKS[0] = MouseEvent.BUTTON1_DOWN_MASK;
        BUTTON_MASKS[1] = MouseEvent.BUTTON2_DOWN_MASK;
        BUTTON_MASKS[2] = MouseEvent.BUTTON3_DOWN_MASK;
        for (int i = 3; i < BTNS; ++i) {
            BUTTON_MASKS[i] = MouseEvent.getMaskForButton(i + 1);
        }

        // JAVA 6 and older:
//		Method m = null;
//		try {
//			m = MouseEvent.class.getMethod( "getMaskForButton", int.class );
//		} catch( NoSuchMethodException e ) {
//			// Java 6 or earlier
//		}
//		BUTTON_MASKS = new int[
//				m == null ? 3 : Math.max( 3, MouseInfo.getNumberOfButtons() )
//		];
//		BUTTON_MASKS[0] = MouseEvent.BUTTON1_DOWN_MASK;
//		BUTTON_MASKS[1] = MouseEvent.BUTTON2_DOWN_MASK;
//		BUTTON_MASKS[2] = MouseEvent.BUTTON3_DOWN_MASK;
//		if( m != null ) {
//			for( int i=3; i<BUTTON_MASKS.length; ++i ) {
//				try {
//					BUTTON_MASKS[i] = (Integer) m.invoke( null, Integer.valueOf(i+1) );
//				} catch( Exception ex ) {
//					BUTTON_MASKS[i] = 0;
//				}
//			}
//		}
    }

    /**
     * If supported by the JRE and the underlying platform, make a window a
     * "utility" window. Utility windows are useful for features such as
     * palettes and floating tool bars. When supported, they typically result in
     * the window having a smaller title bar.
     *
     * @param window the window to make into a utility window
     * @return {@code true} if the attempt succeeds, {@code false} otherwise
     */
    public static boolean makeUtilityWindow(Window window) {
        if (window == null) {
            throw new NullPointerException("window");
        }
        if (assumeUtilitySupported) {
            Class T;
            Method m;
            Object UTILITY;
            try {
                T = Class.forName("java.awt.Window$Type");
                m = Window.class.getMethod("setType", T);
                UTILITY = T.getField("UTILITY").get(null);
            } catch (Throwable t) {
                assumeUtilitySupported = false;
                return false;
            }
            try {
                m.invoke(window, UTILITY);
                return true;
            } catch (Throwable t) {
            }
        }
        return false;
    }
    private static boolean assumeUtilitySupported = true;

    /**
     * Throws an assertion if the calling thread is not the event dispatch
     * thread.
     *
     * @throws AssertionError if called from a thread other than the EDT
     */
    public static void threadAssert() {
        if (!EventQueue.isDispatchThread()) {
            throw new AssertionError("Swing code called outside of event dispatch thread");
        }
    }

    /**
     * Returns a border that is the composition of any number of other borders.
     * Unlike {@code BorderFactory.createCompoundBorder}, this method can
     * compose an arbitrary number of borders. In no case will {@code null} be
     * returned; if no borders are passed in, an empty border will be returned.
     * If one border is passed in, it is returned unchanged. If two borders are
     * passed in, a simple compound border will be returned. Otherwise, a nested
     * compound border that composes the borders is returned.
     *
     * @param outerToInner the borders to compose
     * @return a border that composes all of the requested borders into a single
     * border
     */
    public static Border createCompoundBorder(Border... outerToInner) {
        Border r;
        if (outerToInner.length == 0) {
            r = BorderFactory.createEmptyBorder();
        } else if (outerToInner.length == 1) {
            r = outerToInner[0];
        } else {
            int i = outerToInner.length - 3;
            r = new CompoundBorder(outerToInner[i + 1], outerToInner[i + 2]);
            for (; i >= 0; --i) {
                r = new CompoundBorder(outerToInner[i], r);
            }
        }
        return r;
    }

    public static void installDisabledHTMLFix(JComponent c) {
        c.addPropertyChangeListener(disabledHTMLFix);
    }

    public static void uninstallDisabledHTMLFix(JComponent c) {
        c.removePropertyChangeListener(disabledHTMLFix);
    }

    private static String ENABLED_PROPERTY = "enabled";
    private static String FIX_PROPERTY = "html-fix-color";
    private static final PropertyChangeListener disabledHTMLFix = (evt) -> {
        if (ENABLED_PROPERTY.equals(evt.getPropertyName())) {
            Object src = evt.getSource();
            if (!(src instanceof JComponent)) {
                return;
            }
            JComponent c = (JComponent) src;
            Color fg;
            if (c.isEnabled()) {
                fg = (Color) c.getClientProperty(FIX_PROPERTY);
                if (fg == null) {
                    fg = UIManager.getDefaults().getColor("Label.foreground");
                }
            } else {
                fg = (Color) c.getClientProperty(FIX_PROPERTY);
                if (fg == null) {
                    c.putClientProperty(FIX_PROPERTY, c.getForeground());
                }
                fg = UIManager.getDefaults().getColor("Label.disabledText");
                if (fg == null) {
                    fg = Color.GRAY;
                }
            }
            c.setForeground(fg);
        }
    };

    /**
     * Determines the height (width) needed to display an HTML label with a
     * specified width (height).
     *
     * @param html the HTML string being displayed in a label
     * @param prefSize the preferred width or height of the component
     * @param width if {@code true}, the preferred size dimension is width;
     * otherwise height
     * @return the new preferred size of the component, with the specified
     * dimension set to the preferred size and other dimension calculated
     */
    public static java.awt.Dimension getPreferredSizeForHTMLLabel(String html, boolean width, int prefSize) {
        resizer.setText(html);
        View view = (View) resizer.getClientProperty(BasicHTML.propertyKey);
        view.setSize(width ? prefSize : 0, width ? 0 : prefSize);
        float w = view.getPreferredSpan(View.X_AXIS);
        float h = view.getPreferredSpan(View.Y_AXIS);
        return new java.awt.Dimension(
                (int) Math.ceil(w), (int) Math.ceil(h)
        );
    }
    private static final JLabel resizer = new JLabel();

    /**
     * Applies a pair of icons to a button. The standard icon is displayed
     * normally; the highlight icon is displayed when the button is pressed. If
     * rollover is enabled, the highlight icon is set for both rollover
     * highlights and selection.
     *
     * @param button the button to modify
     * @param standard the normal icon for the button
     * @param highlight the highlight icon for the button
     * @param rollover if {@code true}, rollover effects are applied
     */
    public static void setIconPair(AbstractButton button, Icon standard, Icon highlight, boolean rollover) {
        button.setIcon(standard);
        button.setPressedIcon(highlight);
        button.setSelectedIcon(highlight);
        if (rollover) {
            button.setRolloverEnabled(true);
            button.setRolloverIcon(highlight);
            button.setRolloverSelectedIcon(highlight);
        }
    }

    /**
     * Applies a pair of icons to a button. This is equivalent to
     * {@linkplain #setIconPair(javax.swing.AbstractButton, javax.swing.Icon, javax.swing.Icon, boolean) the icon version of this method},
     * except that it takes the names of icon resources instead of the icons
     * themselves.
     *
     * @param button the button to modify
     * @param standard the normal icon resource
     * @param highlight the highlight icon resource
     * @param rollover if {@code true}, rollover effects are applied
     */
    public static void setIconPair(AbstractButton button, String standard, String highlight, boolean rollover) {
        setIconPair(button, ResourceKit.getIcon(standard), ResourceKit.getIcon(highlight), rollover);
    }

    /**
     * Checks whether an accelerator key is already in use in a menu bar.
     *
     * @param bar the menu bar to search
     * @param accel the accelerator key stroke to check for
     * @return {@code true} if the accelerator is used by an item in the menu
     * bar
     */
    public static boolean isAcceleratorInUse(JMenuBar bar, KeyStroke accel) {
        for (int i = 0; i < bar.getMenuCount(); ++i) {
            if (isAcceleratorInUse(bar.getMenu(i), accel)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAcceleratorInUse(JMenu menu, KeyStroke accel) {
        if (menu == null) {
            return false;
        }
        for (int i = 0; i < menu.getMenuComponentCount(); ++i) {
            Component c = menu.getMenuComponent(i);
            if ((c instanceof JMenuItem) && !(((JMenuItem) c).getClientProperty("ignoreAccelerator") == Boolean.TRUE)) {
                if (accel.equals(((JMenuItem) c).getAccelerator())) {
                    return true;
                }
            }
            if (c instanceof JMenu) {
                if (isAcceleratorInUse((JMenu) c, accel)) {
                    return true;
                }
            }
        }
        return false;
    }
}
