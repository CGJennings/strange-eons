package resources;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.io.EscapedLineWriter;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.textedit.CodeEditorBase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Level;
import javax.swing.KeyStroke;

/**
 * An accelerator table maps command names to keyboard gestures. Like a
 * {@link Language} instance, it can load gestures from a resource file and it
 * uses file name patterns to combine a baseline set of definitions with
 * context-sensitive overrides. In the case of an accelerator table, the
 * following patterns are used to create platform-specific variants:
 * <pre>
 * basename.properties
 * basename-win.properties
 * basename-mac.properties
 * basename-other.properties
 * </pre> Other variant names may be added in future to support new target
 * platforms.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0.3666
 */
public final class AcceleratorTable {

    private HashMap<String, KeyStroke> loaded = new HashMap<>();
    private HashMap<String, KeyStroke> user;
    private File userFile;
    private boolean update = false;

    /**
     * Creates a new, empty accelerator table.
     */
    public AcceleratorTable() {
    }

    /**
     * Creates a new accelerator table that reads its content from the specified
     * resource file. The table will also check for a file with the same name in
     * the {@code keys} subfolder of the user storage folder, and load any
     * definitions found there. Tables created using this constructor can null
     * null null null     {@linkplain #set(java.lang.String, javax.swing.KeyStroke) modify the
	 * key mappings} and {@linkplain #update save changes back again}.
     *
     * @param baseFile the base file name of the table to load
     * @throws IOException if an I/O error occurs
     */
    public AcceleratorTable(String baseFile) throws IOException {
        addAcceleratorsFrom(baseFile);

        String userName = baseFile;
        int slash = baseFile.lastIndexOf('/');
        if (slash >= 0) {
            userName = userName.substring(slash + 1);
        }
        user = new HashMap<>();
        userFile = StrangeEons.getUserStorageFile("keys" + File.separatorChar + userName);
        if (userFile.exists()) {
            addAcceleratorsImpl(new FileInputStream(userFile), true);
        }
    }

    /**
     * Adds key mappings from the specified resource file. The loaded mappings
     * will not override any user mappings loaded when the table was created.
     *
     * @param baseFile the base file name of the table to load
     * @throws IOException if an I/O error occurs
     */
    public void addAcceleratorsFrom(String baseFile) throws IOException {
        String prefix, suffix;
        int dot = baseFile.lastIndexOf('.');
        if (dot < 0) {
            prefix = baseFile;
            suffix = "";
        } else {
            prefix = baseFile.substring(0, dot);
            suffix = baseFile.substring(dot);
        }
        addAcceleratorsImpl(prefix, suffix);
        if (PlatformSupport.PLATFORM_IS_WINDOWS) {
            addAcceleratorsImpl(prefix + "-win", suffix);
        } else if (PlatformSupport.PLATFORM_IS_MAC) {
            addAcceleratorsImpl(prefix + "-mac", suffix);
        } else {
            addAcceleratorsImpl(prefix + "-other", suffix);
        }
    }

    private void addAcceleratorsImpl(String prefix, String suffix) throws IOException {
        try {
            String[] resources = Language.getInterface().getResourceChain(prefix, suffix);
            for (String resource : resources) {
                URL url = ResourceKit.composeResourceURL(resource);
                if (url != null) {
                    addAcceleratorsImpl(url.openStream(), false);
                }
            }
        } catch (NullPointerException e) {
            // this allows components that use AccelTable to work in GUI builder
            StrangeEons.log.log(Level.WARNING, null, e);
        }
    }

    private void addAcceleratorsImpl(InputStream in, boolean isUserSource) throws IOException {
        try (in) {
            EscapedLineReader r = new EscapedLineReader(in);
            String[] tokens;
            while ((tokens = r.readProperty()) != null) {
                KeyStroke v = acceleratorFromString(tokens[1]);
                if (isUserSource) {
                    user.put(tokens[0], v);
                } else {
                    loaded.put(tokens[0], v);
                }
            }
        }
    }

    /**
     * Returns the key stroke associated with an arbitrary key.
     *
     * @param key the key name
     * @return the key stroke associated with the key, or {@code null}
     */
    public KeyStroke get(String key) {
        KeyStroke k;
        if (user != null) {
            k = user.get(key);
            if (k == null && !user.containsKey(key)) {
                k = loaded.get(key);
            }
        } else {
            k = loaded.get(key);
        }
        return k;
    }

    /**
     * Sets the keystroke associated with an arbitrary key in the user table. If
     * the accelerator is set to the same value as that loaded from the resource
     * files, then the value will revert to the default instead of modifying the
     * user table.
     *
     * @param key the key name
     * @param accelerator the key stroke to associated with the key (may be
     * {@code null})
     */
    public void set(String key, KeyStroke accelerator) {
        if (userFile == null) {
            throw new IllegalStateException("no user table is associated with this instance");
        }
        if (key == null) {
            throw new NullPointerException("key");
        }

        KeyStroke def = loaded.get(key);
        if (def == null) {
            if (accelerator != null) {
                user.put(key, accelerator);
                markForUpdate();
            }
        } else if (!def.equals(accelerator)) {
            user.put(key, accelerator);
            markForUpdate();
        }
    }

    /**
     * Converts a string to a single key stroke. The string may use one of two
     * formats: the verbose format use by the {@link KeyStroke} class, or a
     * compact format that requires less typing. The compact format uses the
     * form {@code [modifiers*+]key}. Here,{@code modifiers} represents a
     * sequence of one or more modifier keys. Each modifier is represented by a
     * single letter:
     *
     * <table border=0>
     * <caption>Modifier letter codes</caption>
     * <tr valign=top><th>{@code P} <td>Platform-specific menu accelerator key
     * (Control on most platforms; Command on OS X)
     * <tr valign=top><th>{@code M} <td>Meta (Command)
     * <tr valign=top><th>{@code C} <td>Control
     * <tr valign=top><th>{@code A} <td>Alt
     * <tr valign=top><th>{@code S} <td>Shift
     * <tr valign=top><th>{@code G} <td>AltGr (not recommended for shortcut
     * keys)
     * </table>
     *
     * <p>
     * <b>Examples:</b><br> {@code HOME}<br> {@code ctrl X}<br>
     * {@code ctrl alt DELETE}<br> {@code C+X}<br> {@code CA+DELETE}<br>
     * {@code C + A + Delete}
     *
     * @param description the string description of the key stroke, in one of
     * the two supported formats
     * @return a {@code KeyStroke} for the string description, or {@code null}
     */
    public static KeyStroke acceleratorFromString(String description) {
        if (description == null || description.isEmpty()) {
            return null;
        }
        return CodeEditorBase.parseKeyStroke(description);
    }

    /**
     * Returns a string that can be
     * {@linkplain #acceleratorFromString(java.lang.String) converted back} into
     * the specified key stroke. Returns an empty string if the key stroke is
     * {@code null}. The string will use the compact form if possible.
     *
     * @param accelerator the accelerator to convert
     * @return a string representation of the key stroke
     */
    public static String stringFromAccelerator(KeyStroke accelerator) {
        if (accelerator == null) {
            return "";
        }
        String text = accelerator.toString();
        int pressed = text.indexOf("pressed");
        if (pressed >= 0) {
            StringBuilder b = new StringBuilder(text.length());
            String[] tokens = text.split("\\s");
            for (String token : tokens) {
                token = token.toLowerCase(Locale.CANADA);
                switch (token) {
                    case "meta":
                        b.append('M');
                        break;
                    case "control":
                    case "ctrl":
                        b.append('C');
                        break;
                    case "alt":
                        b.append('A');
                        break;
                    case "altGraph":
                        b.append('G');
                        break;
                    case "shift":
                        b.append('S');
                        break;
                    case "pressed":
                        // default, so skip it
                        break;
                    default:
                        if (b.length() > 0) {
                            b.append('+');
                        }
                        b.append(token);
                }
            }
            text = b.toString();
        }
        return text;
    }

    private void markForUpdate() {
        if (!update) {
            update = true;
            StrangeEons se = StrangeEons.getApplication();
            if (se == null) {
                StrangeEons.log.warning((String) null);
                return;
            }
            se.addExitTask(new Runnable() {
                @Override
                public void run() {
                    if (userFile.getParentFile().exists()) {
                        userFile.getParentFile().mkdirs();
                    }
                    try (OutputStream out = new FileOutputStream(userFile)) {
                        EscapedLineWriter w = new EscapedLineWriter(out);
                        for (Entry<String, KeyStroke> entry : user.entrySet()) {
                            w.writeProperty(entry.getKey(), stringFromAccelerator(entry.getValue()));
                        }
                    } catch (IOException e) {
                        StrangeEons.log.log(Level.WARNING, null, e);
                    }
                }

                @Override
                public String toString() {
                    return "saving user accelerator keys (" + userFile.getName() + ')';
                }
            });
        }
    }

    /**
     * Returns the standard accelerator table for the application.
     *
     * @return application keyboard shortcuts
     */
    public static synchronized AcceleratorTable getApplicationTable() {
        AcceleratorTable t;
        if (appRef == null || (t = appRef.get()) == null) {
            try {
                t = new AcceleratorTable("text/interface/eons-keys.properties");
            } catch (IOException e) {
                StrangeEons.log.log(Level.SEVERE, "failed to load app accelerators", e);
                return new AcceleratorTable();
            }
            appRef = new SoftReference<>(t);
        }
        return t;
    }

    private static SoftReference<AcceleratorTable> appRef;
}
