package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * A field that allows the user to choose a key stroke by pressing the desired
 * key combination. A typical application is allowing the user to define
 * accelerator keys for commands.
 *
 * <p>
 * By default, the field limits acceptable keys to those that make sense as
 * accelerators: function keys, or other keys with at least one modifier (Ctrl,
 * Alt, AltGr, or Meta). This filtering can be disabled. Subclasses may also
 * implement their own policy for accepting or rejecting key strokes by
 * overriding {@link #filterKeyStroke}.
 *
 * <p>
 * Rejected key strokes are displayed to the user for as long as the key stroke
 * is held down using the colours defined for selected text. (By default, this
 * will display the rejected stroke in red.) When the rejected key stroke is
 * released, the text will return to the last valid key stroke.
 *
 * <p>
 * The {@code null} value is valid if there is no key stroke, as when
 * editing the key for a command with no accelerator assigned. This is displayed
 * as an empty field. By default, the Delete and Back Space keys will all set
 * the current key stroke to {@code null} if they are pressed unmodified.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JKeyStrokeField extends JTextField {

    private boolean filterKeys;
    private KeyStroke eventKey;
    private KeyStroke lastValid;

    /**
     * The name of a vetoable property that is fired when the the key stroke is
     * about to change. This allows listeners to listen for selection changes or
     * filter out unacceptable keys (by vetoing the change). For example, a tool
     * that configures command keys might veto keys that are already in use.
     */
    public static final String PROPERTY_KEY_STROKE_CHANGE = "KeyStrokeChange";

    /**
     * Creates a new key stroke field that filters out "normal" keys that
     * wouldn't be used for a command accelerator (see
     * {@link #filterKeyStroke(javax.swing.KeyStroke)}).
     */
    public JKeyStrokeField() {
        this(true);
    }

    /**
     * Creates a new key stroke field. If filterPlainKeyStrokes is true, then
     * keys that are not sensible as command accelerator keys will be rejected.
     *
     * @param filterPlainKeyStrokes
     */
    public JKeyStrokeField(boolean filterPlainKeyStrokes) {
        super(20);
        filterKeys = filterPlainKeyStrokes;

        setSelectionColor(getBackground());
        setSelectedTextColor(Color.RED);
        setBorder(new StrokedBorder());

        addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean keyAccepted = false;
                eventKey = KeyStroke.getKeyStrokeForEvent(e);

                KeyStroke filteredKey;
                if (isDefaultClearKeyEnabled() && isClearKey(eventKey)) {
                    filteredKey = eventKey;
                } else {
                    filteredKey = filterKeyStroke(eventKey);
                }

                if (filteredKey != null) {
                    try {
                        if (isDefaultClearKeyEnabled() && isClearKey(filteredKey)) {
                            filteredKey = null;
                            keyAccepted = true;
                        }
                        fireVetoableChange(PROPERTY_KEY_STROKE_CHANGE, lastValid, filteredKey);
                        keyAccepted = true;
                    } catch (PropertyVetoException ex) {
                        // listener wants to filter out key: keyAccepted is already false
                    }
                }

                if (keyAccepted) {
                    lastValid = filteredKey;
                    setText(toDisplayString(filteredKey));
                } else {
                    setText(toDisplayString(eventKey));
                    selectAll();
                }

                e.consume();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                setText(toDisplayString(lastValid));
                e.consume();
            }
        });

        // intercept mouse drag events to prevent selection changes
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                fixSelection();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                fixSelection();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                fixSelection();
            }

            private void fixSelection() {
                if (lastValid == eventKey) {
                    final int len = getDocument().getLength();
                    select(len, len);
                } else {
                    selectAll();
                }
            }
        });
    }

    /**
     * Sets the current key stroke to ks. Note that this may be used to set the
     * keystroke to a value that would otherwise be invalid. The value
     * {@code null} may be used to indicate that no key stroke is set, and
     * will be displayed to the user as an empty string.
     *
     * @param ks the key stroke to make current
     */
    public void setKeyStroke(KeyStroke ks) {
        lastValid = eventKey = ks;
        setText(toDisplayString(ks));
    }

    /**
     * Gets the current key stroke. This is the most recent of: the key stoke
     * set by {@link #setKeyStroke}, or the last key stroke typed by the user
     * and accepted by {@link #filterKeyStroke}. This value can be
     * {@code null}, meaning that no key stroke has been set.
     */
    public KeyStroke getKeyStroke() {
        return lastValid;
    }

    /**
     * Returns {@code true} if the key stroke will be accepted by the
     * filtering policy.
     *
     * @return {@code true} if filtering is disabled or the key stroke is
     * accepted
     */
    public boolean isKeyStrokeValid(KeyStroke ks) {
        if (!filterKeys) {
            return true;
        }
        return filterKeyStroke(ks) != null;
    }

    /**
     * If {@code true}, then the Delete and Back Space keys (without
     * modifiers) will all be interpreted as a request to set the key stroke to
     * {@code null}, clearing the field.
     *
     * @return {@code true} if clear keys are enabled
     */
    public boolean isDefaultClearKeyEnabled() {
        return defaultClearKeys;
    }

    /**
     * Enables or disables the clear key mechanism. This interprets Delete or
     * Back Space as an attempt to clear the field. When pressed (unmodified),
     * the key stroke will be set to {@code null}.
     *
     * @param enable if the default key clearing mechanism should be applied
     */
    public void setDefaultClearKeyEnabled(boolean enable) {
        this.defaultClearKeys = enable;
    }

    private boolean defaultClearKeys = true;

    /**
     * Returns {@code true} if the key stroke should be interpreted as a
     * request to clear the field. Subclasses may override this to change the
     * keys used by the default key clearing mechanism.
     *
     * @param ks the key stroke that may be a clear key
     * @return {@code true} if the key should clear the field
     */
    protected boolean isClearKey(KeyStroke ks) {
        if ((ks.getModifiers() & ALL_MODIFIERS_MASK) != 0) {
            return false;
        }

        final int vk = ks.getKeyCode();
        return vk == KeyEvent.VK_DELETE || vk == KeyEvent.VK_BACK_SPACE;
    }

    /**
     * Accepts, rejects, or transforms key strokes. This method is called when a
     * key stroke has been completed. It may either return the key stroke (to
     * accept it), substitute a different key stroke, or return
     * {@code null} to reject the key stroke. Subclasses may override this
     * to customize the set of keys that are accepted. The default behaviour is
     * to reject key strokes if they consist only of modifiers or if they do not
     * include at least one modifier other than Shift (except for the function
     * keys, which are always accepted).
     *
     * @param ks the key stroke to filter
     * @return {@code null} to reject the key stroke, otherwise the
     * (possibly replaced) key stroke
     */
    protected KeyStroke filterKeyStroke(KeyStroke ks) {
        if (!filterKeys) {
            return ks;
        }

        final int vk = ks.getKeyCode();

        // function keys always OK
        if ((vk >= KeyEvent.VK_F1 && vk <= KeyEvent.VK_F12) || (vk >= KeyEvent.VK_F13 && vk <= KeyEvent.VK_F24)) {
            return ks;
        }

        // check for one of Ctrl, Alt, AltGraph, or Meta (Command)
        if ((ks.getModifiers() & ACCELERATOR_MASK) == 0) {
            return null;
        }

        // if the last key down is a modifier, there is no non-modifier key
        if (vk == KeyEvent.VK_CONTROL || vk == KeyEvent.VK_ALT || vk == KeyEvent.VK_ALT_GRAPH || vk == KeyEvent.VK_META || vk == KeyEvent.VK_SHIFT) {
            return null;
        }

        return ks;
    }

    /**
     * Modifier mask for any modifier key except Shift.
     */
    private static final int ACCELERATOR_MASK = InputEvent.CTRL_DOWN_MASK
            | InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
            | InputEvent.META_DOWN_MASK;

    /**
     * Modifier mask for any modifier keys, including Shift.
     */
    private static final int ALL_MODIFIERS_MASK = ACCELERATOR_MASK | InputEvent.SHIFT_DOWN_MASK;

    /**
     * Returns a key stroke for a string that uses the same syntax as the text
     * display in a KeyStrokeField.
     *
     * @param s the string to parse
     * @return the key stroke that matches the string, or {@code null} if
     * the string could not be parsed
     */
    public static KeyStroke fromDisplayString(String s) {
        if (s == null || s.length() == 0) {
            return null;
        }

        StringBuilder b = new StringBuilder();

        String[] tokens = s.split("\\+");
        for (int i = 0; i < tokens.length; ++i) {
            if (i > 0) {
                b.append(' ');
            }
            String lastModifier = null;
            String modifier = "";
            if (tokens[i].equals(keyCodeMap.get(KeyEvent.VK_META))) {
                modifier = "meta";
                lastModifier = "META";
            } else if (tokens[i].equals(keyCodeMap.get(KeyEvent.VK_CONTROL))) {
                modifier = "ctrl";
                lastModifier = "CONTROL";
            } else if (tokens[i].equals(keyCodeMap.get(KeyEvent.VK_ALT))) {
                modifier = "alt";
                lastModifier = "ALT";
            } else if (tokens[i].equals(keyCodeMap.get(KeyEvent.VK_ALT_GRAPH))) {
                modifier = "altGraph";
                lastModifier = "ALT_GRAPH";
            } else if (tokens[i].equals(keyCodeMap.get(KeyEvent.VK_SHIFT))) {
                modifier = "shift";
                lastModifier = "SHIFT";
            }
            if (i == tokens.length - 1) {
                if (lastModifier != null) {
                    b.append(lastModifier);
                } else {
                    String localized = keyNameMap.get(tokens[i]);
                    b.append(localized == null ? tokens[i] : localized);
                }
            } else {
                b.append(modifier);
            }
        }

        return KeyStroke.getKeyStroke(b.toString());
    }

    /**
     * Converts a key stroke into a string with the same format used to display
     * key strokes by a KeyStrokeField. This consists of a series of zero or
     * more modifier key names with trailing plus symbols, followed by a key
     * name for the non-modifier key (if any) as given by
     * {@code KeyEvent.getKeyText()}. For example: Ctrl+Shift+ (only the
     * control and shift modifiers are held), or Ctrl+Alt+X (two modifiers and
     * the non-modifier X are held). A null key stroke is converted to the empty
     * string.
     *
     * @param ks the key to convert
     * @return a string representation for the key stroke
     */
    public static String toDisplayString(KeyStroke ks) {
        if (ks == null) {
            return "";
        }

        StringBuilder b = new StringBuilder();
        int mod = ks.getModifiers();
        if ((mod & InputEvent.META_DOWN_MASK) != 0) {
            b.append(keyCodeMap.get(KeyEvent.VK_META)).append('+');
        }
        if ((mod & InputEvent.CTRL_DOWN_MASK) != 0) {
            b.append(keyCodeMap.get(KeyEvent.VK_CONTROL)).append('+');
        }
        if ((mod & InputEvent.ALT_DOWN_MASK) != 0) {
            b.append(keyCodeMap.get(KeyEvent.VK_ALT)).append('+');
        }
        if ((mod & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            b.append(keyCodeMap.get(KeyEvent.VK_ALT_GRAPH)).append('+');
        }
        if ((mod & InputEvent.SHIFT_DOWN_MASK) != 0) {
            b.append(keyCodeMap.get(KeyEvent.VK_SHIFT)).append('+');
        }

        int code = ks.getKeyCode();
        if (code != KeyEvent.VK_META && code != KeyEvent.VK_CONTROL && code != KeyEvent.VK_ALT && code != KeyEvent.VK_ALT_GRAPH && code != KeyEvent.VK_SHIFT) {
            b.append(keyCodeMap.get(ks.getKeyCode()));
        }

        return b.toString();
    }

    private static final HashMap<String, String> keyNameMap;
    private static final HashMap<Integer, String> keyCodeMap;

    static {
        keyNameMap = new HashMap<>(200);
        keyCodeMap = new HashMap<>(200);
        int psf = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
        for (Field f : KeyEvent.class.getFields()) {
            if ((f.getModifiers() & psf) != psf) {
                continue;
            }
            if (f.getName().startsWith("VK_")) {
                try {
                    int code = f.getInt(null);
                    String name = KeyEvent.getKeyText(code);
                    while (keyNameMap.containsKey(name)) {
                        name += "*";
                    }
                    keyNameMap.put(name, f.getName().substring(3));
                    keyCodeMap.put(code, name);
                } catch (IllegalAccessException ex) {
                    // if this happens we have larger issues
                }
            }
        }
    }
}
