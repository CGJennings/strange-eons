package ca.cgjennings.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * A <code>KeyListener</code> that, when added to a component, filters out
 * certain keys to prevent them from being processed by that component.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class KeyFilter implements KeyListener {

    private int[] filtered;

    /**
     * Create a filter that allows all key codes. This constructor is meant
     * primarily for use by subclasses that override the {@link #filter(int)}
     * method.
     */
    public KeyFilter() {
    }

    /**
     * Create a filter that filters out key codes from an array. Key codes are
     * supplied as <code>VK_</code> constants (see
     * <code>java.awt.event.KeyEvent</code>).
     *
     * @param keyCodes
     */
    public KeyFilter(int... keyCodes) {
        if (keyCodes == null) {
            throw new NullPointerException("keyCodes array");
        }
        if (keyCodes.length > 0) {
            filtered = keyCodes.clone();
            java.util.Arrays.sort(filtered);
        }
    }

    /**
     * Returns <code>true</code> if <code>keyCode</code> is filtered out by this
     * filter. Subclasses may override this to implement custom filtering
     * mechanisms.
     *
     * @param keyCode the code to check
     * @return <code>true</code> if the key code should be filtered out
     */
    public boolean filter(int keyCode) {
        if (filtered != null) {
            return java.util.Arrays.binarySearch(filtered, keyCode) >= 0;
        }
        return false;
    }

    /**
     * Called whenever a key code has been filtered. This method allows
     * subclasses to perform some action whenever a key is filtered out. The
     * default implementation emits an error beep.
     *
     * @param keyCode the code being filtered
     */
    protected void onFiltration(int keyCode) {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (filter(e.getKeyCode())) {
            onFiltration(e.getKeyCode());
            e.consume();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
