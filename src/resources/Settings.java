package resources;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.GameComponent;
import static ca.cgjennings.apps.arkham.dialog.ErrorDialog.displayError;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.PluginContext;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.filters.TintFilter;
import ca.cgjennings.graphics.filters.TintingFilter;
import ca.cgjennings.io.NewerVersionException;
import ca.cgjennings.layout.MarkupRenderer;
import ca.cgjennings.layout.PageShape;
import ca.cgjennings.layout.TextStyle;
import gamedata.Expansion;
import gamedata.Game;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.color.ColorSpace;
import java.awt.font.TextAttribute;
import java.awt.font.TransformAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import static resources.Language.string;

/**
 * A collection of keys (names) that map to {@link String} values. Settings are
 * generally used to store configuration information: the configurable feature
 * is given a key name, and that key name can then be used to look up the
 * feature's current value.
 *
 * <p>
 * The main application uses settings to control program behaviour and store
 * user preferences. (For example, the {@link #getUser user setting} with the
 * key name <tt>find-incremental</tt> will have a value of either
 * <tt>yes</tt> or <tt>no</tt> depending on whether the incremental search
 * option is currently selected in the find and replace window for code
 * editors.) Game components may use settings to control how cards are laid out.
 * In some cases, settings are also used to localize text for the game language.
 *
 * <p>
 * Internally, all setting values are stored as strings. However, a number of
 * methods are provided that accept other data types and convert back and forth
 * between strings automatically. Some of these expect the key name to include a
 * specific suffix, and will append this to the key name if it is not present.
 * The use of suffixes helps to distinguish the intended type of the setting
 * when it is listed in a <tt>.settings</tt> file.
 *
 * <p>
 * Settings instances can have another settings instance as a parent. If the
 * value of a key is requested, and it is not defined in a given instance, then
 * that instance will ask its parent to return its value for the key. This
 * process continues recursively until a value is found or a settings instance
 * is reached that has no parent. Another way to understand this is that a
 * settings instance defines a <i>scope</i>
 * (in the sense of programming language theory) within the scope defined by its
 * parent. The most common place where plug-in developers encounter such scopes
 * is with the private settings instances found in {@link GameComponent}s. These
 * are always children of the {@link Game#getSettings game settings} for the
 * game that the component belongs to, which in turn is a child of the
 * {@link #getShared shared global settings instance}. Thus, plug-in developers
 * can store the default values for settings used by the game component in the
 * game's settings instance. This avoids unnecessary duplication and makes it
 * easier to change those defaults in future versions of the plug-in. It also
 * allows the end user to "hack" the layout of a component by modifying the
 * component's private settings to override values normally defined in the
 * game's settings, such as image templates and text regions.
 *
 * <p>
 * Most settings instances have a chain of parents that ultimately leads back to
 * the global {@link #getShared shared settings instance}, which combines
 * application default settings, temporary setting changes, and user preferences
 * in a single space. Note that the shared settings instance is read-only. You
 * can, however, change {@link #getUser user preference settings}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 0.95
 */
public class Settings implements Serializable, Iterable<String> {

    private static final long serialVersionUID = 35_461_387_643_185L;
    private HashMap<String, String> p;
    private Settings parent;

    /**
     * Creates a new, empty {@code Settings} scope whose parent is the shared
     * global scope.
     */
    public Settings() {
    }

    /**
     * Create a new settings scope within the scope of {@code parent}. Passing
     * {@code null} or {@code Settings.getShared()} for the parent will use the
     * shared global scope.
     *
     * @param parent the parent scope that will be used to look up keys not
     * defined in this scope
     * @since 2.00.7
     */
    public Settings(Settings parent) {
        if (parent == getShared()) {
            parent = null;
        }
        if (parent != null) {
            if (parent.parent != null) {
                HashSet<Settings> parents = new HashSet<>();
                Settings s = parent;
                while (s.parent != null) {
                    if (parents.contains(s.parent)) {
                        throw new AssertionError("settings form an infinite loop of parents");
                    }
                    parents.add(s.parent);
                    s = s.parent;
                }
            }
            this.parent = parent;
        }
    }

    /**
     * Returns the {@code Settings} instance that represents the shared global
     * scope.
     *
     * <p>
     * Currently, attempting to set or reset a setting key in the shared scope
     * will cause an {@code UnsupportedOperationException} to be thrown,
     * although this may change in the future.
     *
     * @return the shared global settings scope
     */
    public static Settings getShared() {
        return SHARED;
    }

    /**
     * Returns the {@code Settings} instance that represents user settings. User
     * settings are used to store data that must persist between program runs,
     * such as the recent file list and preference choices.
     *
     * <p>
     * Because user settings take precedence over the standard Strange Eons
     * settings, it can also be used to permanently modify a setting defined by
     * a default value in the shared setting scope. However, this is not usually
     * recommended. The preferred way to achieve this is to write an extension
     * that temporarily changes the setting for the duration of the program run.
     * This way, the extension can be uninstalled to eliminate the effect at a
     * later time. (See {@link RawSettings#setGlobalSetting}.)
     *
     * <p>
     * The user settings scope should not generally be used to directly store
     * preference settings for a third-party plug-in. Instead, use
     * {@link PluginContext#getSettings()} to get a
     * {@link #createNamespace namespace} within the user settings that is
     * unique to the plug-in. This ensures that your setting names will not
     * conflict with the standard application settings or other plug-ins.
     *
     * @return the shared user settings scope
     * @since 2.00.8
     */
    public static Settings getUser() {
        return USER;
    }

    /**
     * Returns a view of an existing {@code java.util.Properties} object as a
     * {@code Settings} object. The {@code Settings} object is fully backed by
     * the {@code Properties} object: changes to one will affect the other.
     *
     * @param toWrap the {@code Properties} to be treated as {@code Settings}
     * @param name a name that will be used to help distinguish the
     * {@code Settings} instance when its {@link #toString()} method is called;
     * may be {@code null}
     * @return a view of the properties as settings
     * @since 2.1
     */
    public static Settings forProperties(String name, Properties toWrap) {
        if (toWrap == null) {
            throw new NullPointerException("toWrap");
        }
        return new PropertySettings(name, toWrap);
    }

    /**
     * Returns a view of these settings within a namespace. All keys accessed
     * through the returned {@code Settings} will add the namespace prefix to
     * their keys. Namespace names always end in a colon (:), a character not
     * normally used in key names. If the supplied name does not end in a colon,
     * a colon will be added to the name automatically. For example, if the
     * namespace "alpha" is created, and the returned settings instance is asked
     * to get or set the value of the "arbitrary-example" key, it will look up
     * the key named "alpha:arbitrary-example" in this settings instance.
     *
     * <p>
     * Plug-ins may obtain a private namespace through their
     * {@link PluginContext} for storing preferences in user settings without
     * fear of conflicting with other plug-ins (as long as conventions for key
     * names are followed).
     *
     * @param namespace the space to restrict key names to
     * @return a view of this {@code Settings} instance that is confined to the
     * specified namespace
     * @since 2.1 alpha 1
     * @throws NullPointerException if the specified namespace is {@code null}
     */
    public final Settings createNamespace(String namespace) {
        if (namespace == null) {
            throw new NullPointerException("namespace");
        }
        return new SettingsNamespace(namespace, this);
    }

    /**
     * Returns the parent scope of this {@code Settings}, or {@code null} if
     * this is the shared scope.
     *
     * @return the parent scope that will be used to look up keys not defined in
     * this scope
     * @since 2.00.7
     */
    public Settings getParent() {
        return parent == null ? getShared() : parent;
    }

    /**
     * Returns an immutable set of all of the keys that have values defined at
     * this scope. Keys that are visible from this scope because they exist in a
     * parent scope, but which are not actually defined in this scope, are not
     * included. To get all of the keys that are visible from this scope, use
     * {@link #getVisibleKeySet()} instead.
     *
     * <p>
     * Note that the returned set represents a copy of the keys defined at this
     * scope when the method was called. It does not change when this underlying
     * {@link Settings} instance is modified.
     *
     * @return an immutable set of the names of all keys that have been set at
     * this scope
     * @since 2.00.7
     */
    public Set<String> getKeySet() {
        Set<String> set;
        if (p == null) {
            set = Collections.emptySet();
        } else {
            set = Collections.unmodifiableSet(new HashSet<>(p.keySet()));
        }
        return set;
    }

    /**
     * Returns an iterator over the keys in this settings instance.
     *
     * @return an iterator over the setting keys defined in this scope
     * @see #getKeySet()
     */
    @Override
    public Iterator<String> iterator() {
        return getKeySet().iterator();
    }

    /**
     * Returns an immutable set of all of the keys that are visible from this
     * scope, including all parent scopes.
     *
     * <p>
     * Note that the returned set represents a <i>copy</i> of the keys visible
     * from this scope when the method was called.
     *
     * @return a set of the names of the keys that are visible from this scope
     * @since 2.00.7
     */
    public Set<String> getVisibleKeySet() {
        HashSet<String> keys = new HashSet<>(1_000);

        Settings s = this;
        do {
            keys.addAll(s.getKeySet());
            s = s.getParent();
        } while (s != null);
        return Collections.unmodifiableSet(keys);
    }

    /**
     * Returns the number of keys that are defined at this scope. Additional
     * keys may be visible from this scope if they are defined in a parent
     * scope. If you need to know the total number of unique keys that are
     * visible from this scope, call {@code getVisibleKeySet().size()}.
     *
     * @return the number of keys defined at this scope
     * @since 2.00.7
     */
    public int size() {
        return p == null ? 0 : p.size();
    }

    /**
     * Returns a setting value, or {@code null} if it is not defined.
     *
     * @param key the key to fetch the value of
     * @return the value of key, or {@code null}
     * @throws NullPointerException if the key is {@code null}
     */
    public String get(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = null;
        // look for private setting
        if (p != null) {
            value = p.get(key);
            // do a special check for the case that the key
            // has been explicitly set to null
            if (value == null && p.containsKey(key)) {
                return null;
            }
        }
        // fall back on the default setting
        if (value == null) {
            // save a method call in the most common case
            if (parent == null) {
                value = RawSettings.getSetting(key);
            } else {
                value = parent.get(key);
            }
        }
        return value;
    }

    /**
     * Returns a setting value, or a default value if it is not defined.
     *
     * @param key the key to fetch the value of
     * @param defaultValue the default value to return if
     * {@code get(key) == null}
     * @return the value of the key, or its default
     * @throws NullPointerException if the key is {@code null}
     */
    public String get(String key, String defaultValue) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String s = get(key);
        return s == null ? defaultValue : s;
    }

    /**
     * Returns a setting value which can be localized. Key names are generated
     * in the order [key_ll_CC, key_ll, key] (where key is the supplied key
     * name, and ll and CC and the language and country codes of the specified
     * locale) and the first key found is returned. If no key is found,
     * {@code null} is returned.
     *
     * <p>
     * <b>Note:</b> While this method is useful in some situations, in most
     * cases it is preferable to use {@link Language#getResourceChain} to load
     * an appropriate sequence of {@code .settings} files and merge them using
     * {@link #addSettingsFrom(java.lang.String)}.
     *
     * @param key the key to fetch the value of
     * @param loc the locale to try to find a more specialized value for
     * @return the value of the key, localized if possible, or {@code null}
     * @throws NullPointerException if the key is {@code null}
     */
    public String getLocalized(String key, Locale loc) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (!loc.getLanguage().isEmpty()) {
            if (!loc.getCountry().isEmpty()) {
                String val = get(key + '_' + loc.getLanguage() + '_' + loc.getCountry());
                if (val != null) {
                    return val;
                }
            }
            String val = get(key + '_' + loc.getLanguage());
            if (val != null) {
                return val;
            }
        }
        return get(key);
    }

    /**
     * Returns a setting value, or {@code null} if it is not defined. This
     * method searches only the local scope, without delegating to parent
     * setting scopes.
     *
     * @param key the key to fetch the value of
     * @return the value of key, or {@code null}
     * @throws NullPointerException if the key is {@code null}
     */
    public String getOverride(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value;
        // look for local setting if available
        if (p != null) {
            value = p.get(key);
        } else {
            if (this == USER) {
                // returns only the value explicitly defined in user settings
                value = RawSettings.getUserSetting(key);
            } else {
                // returns the user setting, if defined, or else the global setting
                value = RawSettings.getSetting(key);
            }
        }
        return value;
    }

    /**
     * Sets the value of a key at this scope.
     *
     * @param key the key to whose value at this scope should be changed
     * @param value the value to set for this scope
     * @throws NullPointerException if the supplied key is {@code null}
     */
    public void set(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (p == null) {
            p = new HashMap<>();
        }

        // magic parent changing code
        if (key.equals(Game.GAME_SETTING_KEY)) {
            if (getClass() == Settings.class) {
                parent = null;
                if (value != null) {
                    Game g = Game.get(value);
                    if (g != null) {
                        Settings potentialParent = g.getSettings();
                        if (potentialParent != this) {
                            parent = potentialParent;
                        }
                    }
                    if (parent == null) {
                        StrangeEons.log.log(Level.WARNING, "no settings for game {0}", value);
                    }
                }
            }
        }

        if (hasPropertyChangeListeners()) {
            String old = p.get(key);
            p.put(key, value);
            firePropertyChangeEvent(key, old, p.get(key));
        } else {
            p.put(key, value);
        }
    }

    /**
     * Removes the specified key from this scope. When a key is removed, a
     * subsequent {@link #get get} of the key will search the parent scope, just
     * as if the key had never been set. That is, the key's value is reset to
     * the default value stored in the parent.
     *
     * @param key the key to be removed
     * @throws NullPointerException if the key is {@code null}
     */
    public void reset(String key) {
        if (key == null) {
            throw new NullPointerException("null key");
        }
        if (p == null) {
            return;
        }

        if (key.equals(Game.GAME_SETTING_KEY)) {
            if (getClass() == Settings.class) {
                parent = null;
            }
        }

        if (hasPropertyChangeListeners()) {
            String old = p.get(key);
            p.remove(key);
            firePropertyChangeEvent(key, old, p.get(key));
        } else {
            p.remove(key);
        }
        if (p.isEmpty()) {
            p = null;
        }
    }

    /**
     * Sets multiple key, value pairs with a single method call.
     *
     * @param keyValuePairs an array where elements at even indices are key
     * names and elements at odd indices define the value to be associated with
     * the key at the previous index
     * @throws NullPointerException if the array of setting pairs is
     * {@code null}, or if any individual key name is {@code null}
     * @throws IllegalArgumentException if the array of setting pairs has an odd
     * length
     */
    public void setSettings(String... keyValuePairs) {
        if (keyValuePairs == null) {
            throw new NullPointerException("keyValuePairs");
        }
        if ((keyValuePairs.length & 1) != 0) {
            throw new IllegalArgumentException("arguments must come in key, value pairs");
        }
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            set(keyValuePairs[i], keyValuePairs[i + 1]);
        }
    }

    /**
     * Adds settings stored in a {@code .settings} resource file to this
     * settings object. The resource file defines keys and values using lines
     * with the format
     * <tt>key = value</tt>. Long entries may be broken over multiple physical
     * lines by ending each physical line except the last with a backslash.
     * Spaces, newlines, equals signs, colons, hashes, and backslashes may be
     * inserted using the escape sequence <tt>\&nbsp;</tt>, <tt>\n</tt>,
     * <tt>\=</tt>,
     * <tt>\:</tt>, <tt>\#</tt>, and <tt>\\</tt>, respectively. Unicode
     * characters may be inserted using standard Java-style
     * <tt>\<!-- -->uXXXX</tt> escape sequences, and must be used for characters
     * outside of the ISO8859-15 character set. Note that if you edit the file
     * in the built-in code editor, the editor will convert such characters to
     * escape sequences automatically.
     *
     * @param resource a file relative to the resources folder
     * @throws FileNotFoundException if the specified resource does not exist
     * @throws IOException if an error occurs while loading the settings
     */
    public void addSettingsFrom(String resource) throws IOException, FileNotFoundException {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        if (resource.isEmpty()) {
            StrangeEons.log.warning("asked to add empty file resource");
            return;
        }

        Properties loader = new Properties();
        InputStream in = null;
        try {
            URL url = resources.ResourceKit.composeResourceURL(resource);
            if (url == null) {
                throw new FileNotFoundException(resource);
            }
            in = url.openStream();
            loader.load(in);
            for (String key : loader.stringPropertyNames()) {
                set(key, loader.getProperty(key));
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Adds entries stored in a map collection to this settings instance.
     *
     * @param settingMap a map of keys and associated values
     * @throws NullPointerException if the map is {@code null}
     */
    public void addSettingsFrom(Map<String, String> settingMap) {
        if (settingMap == null) {
            throw new NullPointerException("settingMap");
        }
        if (settingMap.isEmpty()) {
            return;
        }

        for (String key : settingMap.keySet()) {
            set(key, settingMap.get(key));
        }
    }

    /**
     * Adds entries from another {@code Settings} instance to this instance. If
     * a key is defined in both instances, the current value in this instance
     * will be replaced. Keys from the source's parent, if any, are not copied.
     *
     * @param sourceSettings the settings instance to copy from
     */
    public void addSettingsFrom(Settings sourceSettings) {
        if (sourceSettings == null) {
            throw new NullPointerException("sourceSettings");
        }
        for (String key : sourceSettings.getKeySet()) {
            set(key, sourceSettings.get(key));
        }
    }

    //
    //
    //
    // Data type getter/setters and string parsers
    //
    //
    //
    /**
     * Returns the value of a text key. This is essentially the same as
     * {@link #get(java.lang.String)}, with the following exceptions:
     * <ol>
     * <li> the key name will have {@code -text} appended if necessary
     * <li> if the key is not defined, instead of returning {@code null} the
     * method will return {@code [MISSING: <i>key name</i>]}
     * </ol>
     * This is useful when the contents of a key will be displayed directly on a
     * game component, as a missing or reset key will be immediately obvious to
     * the viewer.
     *
     * @param key the name of a text key, with or without the {@code -text}
     * suffix
     * @return the value of the key, with the {@code -text} suffix, or a special
     * undefined string if the key does not exist
     */
    public final String getText(String key) {
        String value = get(suffix(key, "-text"));
        if (value == null) {
            value = "[MISSING: " + key + "]";
        }
        return value;
    }

    /**
     * Sets the value of a text key. This is equivalent to calling
     * {@link #set(java.lang.String, java.lang.String)}, except that the key
     * will have the suffix {@code -text} appended if necessary.
     *
     * @param key the text key to change the value of
     * @param value the new value for the key
     */
    public final void setText(String key, String value) {
        set(suffix(key, "-text"), value);
    }

    /**
     * Returns a boolean value based on the value of {@code key}. Any of "yes",
     * "true", or "1" are mapped to {@code true}, and all other values are
     * mapped to {@code false} (including {@code null}).
     *
     * @param key the name of the setting key
     * @return {@code true} if the value of the setting is "yes", "true", or "1"
     * @see #setYesNo
     */
    public final boolean getYesNo(String key) {
        return yesNo(get(key));
    }

    /**
     * Returns a boolean value based on the value of {@code key}. Any of "yes",
     * "true", or "1" are mapped to {@code true}, and all other values are
     * mapped to {@code false}.
     *
     * @param key the name of the setting key
     * @param defaultValue the default value to return if the key is not defined
     * @return {@code true} if the value of the setting is "yes", "true", or "1"
     * @see #setYesNo
     */
    public final boolean getYesNo(String key, boolean defaultValue) {
        String v = get(key);
        return v == null ? defaultValue : yesNo(v);
    }

    /**
     * Sets the value of a key with a {@code boolean} value suitable for reading
     * with {@link #getYesNo}.
     *
     * @param key the name of the key to set
     * @param value the value to convert into a setting
     * @see #getYesNo
     */
    public final void setYesNo(String key, boolean value) {
        set(key, value ? "yes" : "no");
    }

    /**
     * Returns a boolean value based on the value of {@code key}. This is a
     * cover for {@link #getYesNo(java.lang.String)}.
     *
     * @param key the name of the setting key
     * @return {@code true} if the value of the setting is "yes", "true", or "1"
     */
    public final boolean getBoolean(String key) {
        return getYesNo(key);
    }

    /**
     * Returns a boolean value based on the value of {@code key}. This is a
     * cover for {@link #getYesNo(java.lang.String, boolean)}.
     *
     * @param key the name of the setting key
     * @param defaultValue the default value to return if the key is not defined
     * @return {@code true} if the value of the setting is "yes", "true", or "1"
     */
    public final boolean getBoolean(String key, boolean defaultValue) {
        return getYesNo(key, defaultValue);
    }

    /**
     * Sets the value of a key with a {@code boolean} value. This is a cover for
     * {@link #setYesNo}.
     *
     * @param key the name of the key to set
     * @param value the value to convert into a setting
     * @see #getYesNo
     */
    public final void setBoolean(String key, boolean value) {
        set(key, value ? "yes" : "no");
    }

    /**
     * Returns a {@code boolean} value for an arbitrary string using the same
     * rules as those used by {@link #getYesNo(java.lang.String)}. Namely: any
     * of "yes", "true", or "1" are mapped to {@code true}, and all other values
     * are mapped to {@code false} (including {@code null}).
     *
     * @param value the value to parse
     * @return {@code true} if the value is "yes", "true", or "1" (not case
     * sensitive)
     */
    public static boolean yesNo(String value) {
        boolean answer = false;
        if (value != null) {
            value = value.trim();
            if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true") || value.equals("1")) {
                answer = true;
            }
        }
        return answer;
    }

    /**
     * Returns an integer value based on the value of {@code key}. The
     * conversion of the value is performed as if by
     * {@link #integer(java.lang.String)}.
     *
     * @param key the name of the setting key
     * @return the integer value of the setting
     * @throws ParseError if the value is not valid or is not defined
     *
     */
    public final int getInt(String key) {
        return integerImpl(key, get(key));
    }

    /**
     * Returns the value of the specified key as an integer value. If the value
     * is invalid or not defined, the default value is returned.
     *
     * @param key the key to look up
     * @param defaultValue the default value for the key
     * @return the integer value of the key, or the default
     */
    public final int getInt(String key, int defaultValue) {
        try {
            String value = get(key);
            if (value != null) {
                return integerImpl(key, value);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    /**
     * Sets the value of a key to an integer value suitable for reading with
     * {@link #getInt}.
     *
     * @param key the name of the key to set
     * @param value the value to convert into a setting
     * @see #getInt
     */
    public final void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    /**
     * Parses a string into an integer value. Values that fit the following
     * pattern can be parsed:
     * <pre>            [+|-][0x|0X|#|0][digits]</pre> A basic integer value consists of an
     * optional sign (+ or -) followed by decimal digits. If the digits follow
     * 0x, 0X, or #, then the value is treated as a hexadecimal (base 16)
     * number. In this case, the digits may include the letters a-f (or A-F). If
     * the digits follow 0, then the value is treated as an octal (base 8)
     * number. In this case the allowed digits are 0-7 inclusive.
     *
     * <p>
     * If the value includes a decimal point ('.'), then the value will be
     * parsed up to the decimal point and any following digits ignored.
     *
     * <p>
     * If the value cannot be parsed, or if it falls outside of the range
     * {@link Integer#MIN_VALUE}...{@link Integer#MAX_VALUE}, a
     * {@link ParseError} is thrown.
     *
     * @param value the string to convert to an integer
     * @return the converted value, or {@code Integer.MIN_VALUE}
     * @throws ParseError if the value is not valid
     */
    public static int integer(String value) {
        return integerImpl(null, value);
    }

    private static int integerImpl(String key, String value) {
        try {
            value = value.trim();
            return Integer.decode(value);
        } catch (Throwable t) {
            int decimal = value.indexOf('.');
            if (decimal >= 0) {
                if (decimal == 0) {
                    return 0;
                }
                try {
                    return Integer.decode(value.substring(0, decimal));
                } catch (Throwable t2) {
                    /* will throw below */ }
            }
            throw new ParseError("rk-err-int", key, value, t);
        }
    }

    /**
     * Returns the value of the specified key as a {@code float} value. The
     * conversion of the value is performed as if by
     * {@link #number(java.lang.String)}, except for being single precision
     * floating point rather than double precision floating point.
     *
     * @param key the name of the setting key
     * @return the float value of the setting
     * @throws ParseError if the value is not valid as defined above
     */
    public final float getFloat(String key) {
        return (float) doubleImpl(key, get(key));
    }

    /**
     * Returns the value of the specified key as an float value. If the value is
     * invalid or not defined, the default value is returned.
     *
     * @param key the key to look up
     * @param defaultValue the default value for the key
     * @return the float value of the key, or the default
     */
    public final float getFloat(String key, float defaultValue) {
        try {
            String value = get(key);
            if (value != null) {
                return (float) doubleImpl(key, value);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    /**
     * Sets the value of a key to a float value suitable for reading with
     * {@link #getFloat}.
     *
     * @param key the name of the key to set
     * @param value the value to convert into a setting
     * @see #getFloat
     */
    public final void setFloat(String key, float value) {
        set(key, String.valueOf(value));
    }

    /**
     * Returns the value of the specified key as a double value. The conversion
     * of the value is performed as if by {@link #number(java.lang.String)}.
     *
     * @param key the name of the setting key
     * @return the float value of the setting
     * @throws ParseError if the value is not valid
     * @see #setDouble
     */
    public final double getDouble(String key) {
        return doubleImpl(key, get(key));
    }

    /**
     * Returns the value of the specified key as a double value. If the value is
     * invalid or not defined, the default value is returned.
     *
     * @param key the key to look up
     * @param defaultValue the default value for the key
     * @return the double value of the key, or the default
     * @see #setDouble
     */
    public final double getDouble(String key, double defaultValue) {
        try {
            String value = get(key);
            if (value != null) {
                return doubleImpl(key, value);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    /**
     * Sets the value of a key to a double value suitable for reading with
     * {@link #getDouble}.
     *
     * @param key the name of the key to set
     * @param value the value to convert into a setting
     * @see #getDouble
     */
    public final void setDouble(String key, double value) {
        set(key, String.valueOf(value));
    }

    /**
     * Parses a string into a {@code double} value. The value is parsed as if by
     * {@link Double#parseDouble}, except that surrounding whitespace is
     * ignored.
     *
     * @param value the string to convert to a floating-point number
     * @return the parsed value
     * @throws ParseError if the value is not valid as defined above
     */
    public static double number(String value) {
        return doubleImpl(null, value);
    }

    private static double doubleImpl(String key, String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Throwable t) {
            throw new ParseError("rk-err-float", key, value, t);
        }
    }

    /**
     * Returns the value of the specified key as a {@link Colour};
     * <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     * The colour must be expressed in one of the formats described under
     * {@link #colour(java.lang.String)}.
     *
     * @param key the the name of a key, with or without the {@code -colour}
     * suffix
     * @return a colour created by interpreting the value of the key
     * @throws ParseError if the value is {@code null} or invalid
     * @see #colour(java.lang.String)
     */
    public final Colour getColour(String key) {
        key = suffix(key, "-colour");
        return argbToColour(colourImpl(key, get(key)));
    }

    /**
     * Returns the value of the specified key as a {@link Colour};
     * <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     * If the value is invalid or not defined, the default value is returned.
     *
     * @param key the the name of a key, with or without the {@code -colour}
     * suffix
     * @param defaultValue the default value for the key
     * @return a colour created by interpreting the value of the key
     * @see #colour(java.lang.String)
     */
    public final Colour getColour(String key, Color defaultValue) {
        try {
            final String v = get(suffix(key, "-colour"));
            if (v != null) {
                return argbToColour(colourImpl(key, v));
            }
        } catch (ParseError e) {
        }
        if (defaultValue instanceof Colour) {
            return (Colour) defaultValue;
        }
        return defaultValue == null ? null : new Colour(defaultValue.getRGB(), true);
    }

    /**
     * Returns the value of the specified key as a {@link Colour};
     * <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     * If the value is invalid or not defined, a {@code Colour} is created for
     * the ARGB value represented by {@code defaultValue} and returned.
     *
     * @param key the the name of a key, with or without the {@code -colour}
     * suffix
     * @param defaultValue the default value for the key
     * @return a colour created by interpreting the value of the key
     * @see #colour(java.lang.String)
     */
    public final Colour getColour(String key, int defaultValue) {
        try {
            final String v = get(suffix(key, "-colour"));
            if (v != null) {
                return colour(v);
            }
        } catch (ParseError e) {
        }
        return argbToColour(defaultValue);
    }

    /**
     * Sets the value of the specified key to a suitable value for the specified
     * colour; <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     *
     * @param key the name of the key to set, with or without {@code -colour}
     * @param colour the colour value to write as a setting
     */
    public final void setColour(String key, Color colour) {
        if (colour == null) {
            throw new NullPointerException("colour");
        }
        final int argb = colour.getRGB();

        String v;
        if ((argb & 0xff000000) == 0xff000000) {
            v = String.format("%06x", (argb & 0xffffff));
        } else {
            v = String.format("%08x", argb);
        }

        set(key, v);
    }

    /**
     * Returns the value of the specified key as a {@link Colour};
     * <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     * This is a cover for {@link #getColour(java.lang.String)}.
     *
     * @param key the the name of a key, with or without the {@code -colour}
     * suffix
     * @return a colour created by interpreting the value of the key
     * @throws ParseError if the value is {@code null} or invalid
     */
    public final Colour getColor(String key) {
        return getColour(key);
    }

    /**
     * Returns the value of the specified key as a {@link Colour};
     * <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     * This is a cover for {@link #getColour(java.lang.String, java.awt.Color)}.
     *
     * @param key the the name of a key, with or without the {@code -colour}
     * suffix
     * @param defaultValue the default value for the key
     * @return a colour created by interpreting the value of the key
     */
    public final Colour getColor(String key, Color defaultValue) {
        return getColour(key);
    }

    /**
     * Returns the value of the specified key as a {@link Colour};
     * <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     * This is a cover for {@link #getColour(java.lang.String, int)}.
     *
     * @param key the the name of a key, with or without the {@code -colour}
     * suffix
     * @param defaultValue the default value for the key
     * @return a colour created by interpreting the value of the key
     */
    public final Colour getColor(String key, int defaultValue) {
        return getColour(key, defaultValue);
    }

    /**
     * Sets the value of the specified key to a suitable value for the specified
     * colour; <i>the key name will have {@code -colour} appended if necessary.
     * </i>
     * This is a cover for {@link #setColour(java.lang.String, java.awt.Color)}.
     *
     * @param key the name of the key to set, with or without {@code -colour}
     * @param colour the colour value to write as a setting
     */
    public final void setColor(String key, Color colour) {
        setColour(key, colour);
    }

    /**
     * Returns a {@link Colour} parsed from a string value. The colour must be
     * expressed using one of the following formats:
     *
     * <p>
     * <b>RGB colour value, hexadecimal:</b> {@code [#][AA]RRGGBB}<br>
     * A colour in the sRGB colour space defined using either 6 or 8 hexadecimal
     * (base 16) digits. The colour value may optionally start with a '#'
     * character, like colour values in HTML.
     *
     * <p>
     * Each pair of hexadecimal digits defines the level of red (RR), green
     * (GG), or blue (BB) used in the resulting colour: 00 means no amount of
     * that primary should be used, and ff (equivalent to decimal 255) means the
     * maximum amount of that primary should be used. For example, the purest,
     * brightest possible red colour would be specified as #ff0000; that is, the
     * maximum amount of red and no amount of any other primary.
     *
     * <p>
     * If the colour value consists of exactly 6 hexadecimal digits, then it
     * will be completely <i>opaque</i>. If there are 8 hexadecimal digits, then
     * the first two digits specify an opacity (or alpha) value for the colour,
     * also as a value between 00 and ff. A value of 00 is completely
     * transparent (effectively invisible), while values between 00 and ff
     * specify increasing degrees of translucency. For example, a colour with an
     * alpha value of 80 (128 in decimal) would show 50% of the underlying
     * colour when painted onto a surface.
     *
     * <p>
     * <b>RGB colour value, decimal:</b> {@code [rgb[a]](R,G,B[,A])}<br>
     * A colour in the sRGB colour space defined using either 3 or 4 fractional
     * values. This is essentially the same as the above format, but it uses
     * decimal colour components instead of hexadecimal. The colour value must
     * be placed inside parentheses, and may optionally be prefixed with either
     * "rgb" or "rgba" (like colour values in HTML). For legibility, you can add
     * one or more spaces between the components.
     *
     * <p>
     * Each fraction defines the level of red (R), green (G), or blue (B) as a
     * value between 0 and 1. For example, the colour #ff0000 above would be
     * expressed as (1, 0, 0); the colour 50% grey would be expressed as (0.5,
     * 0.5, 0.5). If a fourth value is supplied, it defines the alpha value of
     * the colour. For example, the value (1, 1, 1, 0.5) yields a 50%
     * translucent white.
     *
     * <p>
     * <b>HSB colour value, decimal:</b> {@code hsb[a](H,S,B[,A])}<br>
     * A colour in the HSB (HSV) colour space defined using either 3 or four
     * decimal numbers, representing the Hue, Saturation, Brightness, and an
     * optional Alpha value. The hue value is an angle, measured in degrees,
     * which defines a point on the edge of a colour wheel where 0&deg; is pure
     * red, 120&deg; is pure green, and 240&deg; is pure blue. The saturation
     * value is a number between 0 and 1, where 0 is desaturated (grey) and 1 is
     * fully saturated. The brightness value is also a number between 0 and 1,
     * where 0 represents black and 1 represents maximum brightness. If a fourth
     * value is supplied, it defines the alpha value of the colour as described
     * above. For example, the value hsb(180, 1, 1, 0.8) yields pure cyan with
     * an alpha of 80%.
     *
     * @param value a string of that describes a colour using one of the above
     * formats
     * @return the colour represented by the supplied value
     * @throws ParseError
     * @since 2.00.7
     * @see #getColor
     */
    public static Colour colour(String value) {
        return argbToColour(colourImpl(null, value));
    }

    /**
     * Cover method for {@link #colour(java.lang.String)}.
     *
     * @param value a string of that describes a colour
     * @return the colour represented by the supplied value
     * @since 2.00.7
     */
    public static Colour color(String value) {
        return colour(value);
    }

    /**
     * Returns a (possibly cached) {@code Colour} for an argb int.
     */
    private static Colour argbToColour(int argb) {
        if (argb == 0xff000000) {
            return BLACK;
        } else if (argb == 0xffffffff) {
            return WHITE;
        }
        return new Colour(argb, true);
    }
    private static final Colour BLACK = new Colour(0);
    private static final Colour WHITE = new Colour(0xffffff);

    /**
     * Converts a value to an argb int.
     */
    private static int colourImpl(String key, String value) {
        try {
            value = value.trim();
            char c0 = value.charAt(0);
            if (c0 == 'r' || c0 == 'R' || c0 == 'h' || c0 == 'H' || c0 == '(') {
                int lparen = value.indexOf('(');
                int rparen = value.lastIndexOf(')');
                if (lparen < 0) {
                    throw new IllegalArgumentException("missing (");
                }
                if (rparen < 0) {
                    throw new IllegalArgumentException("missing )");
                }
                String[] comps = LIST_SPLITTER.split(value.substring(lparen + 1, rparen).trim());
                if (comps.length < 3) {
                    throw new IllegalArgumentException("missing components");
                }
                if (comps.length > 4) {
                    throw new IllegalArgumentException("too many components");
                }

                if (c0 == 'h' || c0 == 'H') {
                    int rgb = Color.HSBtoRGB(Float.parseFloat(comps[0]) / 360f, Float.parseFloat(comps[1]), Float.parseFloat(comps[2]));
                    if (comps.length == 4) {
                        rgb |= toByte(comps[3]) << 24;
                    }
                    return rgb;
                }

                final int r = toByte(comps[0]);
                final int g = toByte(comps[1]);
                final int b = toByte(comps[2]);
                final int a = comps.length == 3 ? 255 : toByte(comps[3]);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }

            if (c0 == '#') {
                value = value.substring(1);
            }

            if (value.length() == 6) {
                return 0xff000000 | Integer.parseInt(value, 16);
            } else if (value.length() == 8) {
                return (int) (Long.parseLong(value, 16) & 0xffffffffL);
            }
            throw new IllegalArgumentException("wrong number of digits; must be 6 or 8");
        } catch (Throwable t) {
            throw new ParseError("rk-err-colour", key, value, t);
        }
    }

    /**
     * Converts a float string from 0-1 to a byte from 0-255, clamped.
     */
    private static int toByte(String colourComponent) {
        final int v = (int) (Float.parseFloat(colourComponent) * 255f + 0.5f);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /**
     * Returns the value of the specified key as a tint configuration;
     * <i>the key name will have {@code -tint} appended if necessary. </i>
     * The tint must be expressed using the format described under
     * {@link #tint(java.lang.String)}. The returned float array is suitable for
     * use with a {@link TintingFilter}.
     *
     * @param key the the name of a key, with or without the {@code -tint}
     * suffix
     * @return a tint configuration created by interpreting the value of the key
     * @throws ParseError if the value is {@code null} or invalid
     * @see #tint
     * @see #setTint
     */
    public final float[] getTint(String key) {
        key = suffix(key, "-tint");
        return tintImpl(key, get(key));
    }

    /**
     * Returns the value of the specified key as a tint configuration;
     * <i>the key name will have {@code -tint} appended if necessary. </i>
     * If the value is invalid or not defined, the default value is returned.
     * Note that it is up to the caller to ensure that if a default is supplied
     * that it is a valid tint configuration (namely, that is has length three).
     *
     * @param key the key to look up, with or without the {@code -tint} suffix
     * @param defaultValue the default value for the key
     * @return the tint configuration extracted from the key, or the default
     * @see #tint
     * @see #setTint
     */
    public final float[] getTint(String key, float[] defaultValue) {
        key = suffix(key, "-tint");
        try {
            final String v = get(key);
            if (v != null) {
                return tintImpl(key, v);
            }
        } catch (ParseError e) {
        }
        return defaultValue;
    }

    /**
     * Sets the specified key to a suitable value for the given tint
     * configuration;
     * <i>the key name will have {@code -tint} appended if necessary. </i>
     * The tint configuration will be taken from the first three elements of the
     * specified array.
     *
     * @param key the key to modify, with or without the {@code -tint} suffix
     * @param tintConfiguration an array of at least three float values
     * containing the tint configuration as the first three elements
     * @throws IllegalArgumentException if the array has less than three
     * elements
     */
    public final void setTint(String key, float[] tintConfiguration) {
        if (tintConfiguration.length < 3) {
            throw new IllegalArgumentException("not enough elements");
        }
        setTint(key, tintConfiguration[0], tintConfiguration[1], tintConfiguration[2]);
    }

    /**
     * Sets the specified key to a suitable value for the given tint
     * configuration;
     * <i>the key name will have {@code -tint} appended if necessary. </i>
     *
     * @param key the key to modify, with or without the {@code -tint} suffix
     * @param hFactor the hue adjustment factor for the tint configuration
     * @param sFactor the saturation adjustment factor for the tint
     * configuration
     * @param bFactor the brightness adjustment factor for the tint
     * configuration
     */
    public final void setTint(String key, float hFactor, float sFactor, float bFactor) {
        set(suffix(key, "-tint"), String.valueOf(hFactor) + ',' + String.valueOf(sFactor) + ',' + String.valueOf(bFactor));
    }

    /**
     * Returns a tint configuration parsed from a string value. A tint
     * configuration is used to express colours in relative terms. A tint
     * setting consists of the following components, analogous to how HSB
     * colours are defined by {@link #colour}:
     * <pre>            [tint(]H,S,B[)]</pre> The value of H is a positive or negative
     * change in the hue angle, expressed in degrees, such as -90.0. The values
     * for S and B are scaling factors that are applied to the saturation and
     * brightness values, respectively. The "tint" keyword and parentheses are
     * optional.
     *
     * <p>
     * The tint configuration returned from this method consists of an array of
     * three floating point values containing the hue, saturation, and
     * brightness components as above, except that the angle is converted to a
     * value between 0 and 1 by dividing it by 360. This is the same format that
     * is expected by {@link TintingFilter}s.
     *
     * @param value the string to convert into a tint configuration using the
     * format described above
     * @return a tint configuration for the value
     * @throws ParseError if the value is {@code null} or invalid
     * @see #getTint
     * @see TintFilter
     * @see <a href='scriptdoc:tints'>tints scripting library</a>
     * @since 2.00.7
     */
    public static float[] tint(String value) {
        return tintImpl(null, value);
    }

    private static float[] tintImpl(String key, String value) {
        try {
            value = value.trim();
            final char c0 = value.charAt(0);
            if (c0 == 't' || c0 == 'T' || c0 == '(') {
                // extract value from "tint(...)"
                final int lparen = value.indexOf('(');
                final int rparen = value.lastIndexOf(')');
                if (lparen < 0) {
                    throw new IllegalArgumentException("missing (");
                }
                if (rparen < 0) {
                    throw new IllegalArgumentException("missing )");
                }
                value = value.substring(lparen + 1, rparen).trim();
            }
            String[] comps = LIST_SPLITTER.split(value);
            if (comps.length != 3) {
                throw new IllegalArgumentException("wrong number of components: " + comps.length);
            }
            return new float[]{Float.parseFloat(comps[0]) / 360f, Float.parseFloat(comps[1]), Float.parseFloat(comps[2])};
        } catch (Exception e) {
            throw new ParseError("rk-err-tint", key, value, e);
        }
    }

    /**
     * Returns the {@link Region} represented by the value of the specified key;
     * <i>the key name will have {@code -region} appended if necessary.
     * </i>
     * The returned region is limited to integer precision. The region value
     * must use one of the following forms:
     *
     * <p>
     * {@code <i>x</i>, <i>y</i>, <i>w</i>, <i>h</i>}<br>
     * A list of four numbers that specify the <i>x</i>-position,
     * <i>y</i>-position, width, and height of the region.
     *
     * <p>
     * {@code d(<i>parent</i>, <i>dx</i>, <i>dy</i>, <i>dw</i>,
     * <i>dh</i>)}<br>
     * This form allows you to specify one region in terms of another region.
     * For example, if a region key has the value {@code d(other,5,-10,0,0)},
     * then the value of the region would be determined by looking up the value
     * of the region with the key name "other-region", and then shifting it to
     * the right by 5 units and up by 10 units.
     *
     * @param key the the name of a key, with or without the {@code -region}
     * suffix
     * @return the region described by the value of the key, or {@code null} if
     * the key is not defined
     * @throws ParseError if the value does not have the correct syntax, or if
     * any region referenced by the value is missing
     * @see #setRegion
     * @see #region
     * @see Region
     */
    public final Region getRegion(String key) {
        key = suffix(key, "-region");
        String value = get(key);
        if (value == null) {
            return null;
        }

        // check for a delta region
        value = value.trim();
        if (value.length() >= 3 && value.charAt(0) == 'd' && value.charAt(1) == '(' && value.charAt(value.length() - 1) == ')') {
            int comma = value.indexOf(','); // first comma splits parent region from delta values
            if (comma < 0) {
                throw new ParseError("rk-err-region", key, value);
            }

            Region delta = regionImpl(key, value.substring(comma + 1, value.length() - 1), null);

            Region source;
            String parentName = value.substring(2, comma).trim();
            try {
                source = getRegion(parentName);
            } catch (ParseError de) {
                throw new ParseError("rk-err-region", key, value, de);
            }
            if (source == null) {
                throw new ParseError("rk-err-region", key, value, null);
            }
            source.x += delta.x;
            source.y += delta.y;
            source.width += delta.width;
            source.height += delta.height;
            return source;
        }

        return regionImpl(key, value, null);
    }

    /**
     * Returns the {@link Region} represented by the value of the specified key;
     * <i>the key name will have {@code -region} appended if necessary.
     * </i>
     * If the value is not defined or is invalid, the default value is returned.
     *
     * @param key the the name of a key, with or without the {@code -region}
     * suffix
     * @param defaultValue the region to region if the key value is missing or
     * invalid
     * @return the region described by the value of the key
     */
    public final Region getRegion(String key, Region defaultValue) {
        key = suffix(key, "-region");
        try {
            final String v = get(key);
            if (v != null) {
                return regionImpl(key, v.trim(), null);
            }
        } catch (ParseError e) {
        }
        return defaultValue;
    }

    /**
     * Sets the specified key to a suitable value for the given region;
     * <i>the key name will have {@code -region} appended if necessary.
     * </i>
     *
     * @param key the key to modify, with or without the {@code -region} suffix
     * @param region the region to write as a setting value
     */
    public final void setRegion(String key, Region region) {
        set(suffix(key, "-region"),
                String.valueOf(region.x) + ','
                + String.valueOf(region.y) + ','
                + String.valueOf(region.width) + ','
                + String.valueOf(region.height)
        );
    }

    /**
     * Returns a {@link Region} defined by a comma-separated list of four
     * integers in the specified value. The value must describe a region using
     * the first of the formats described
     * {@linkplain #getRegion(java.lang.String) above}.
     *
     * @param value a string containing a description of the region
     * @return the region described by the value
     * @throws ParseError if the value is not a valid region description
     */
    public static Region region(String value) {
        if (value == null) {
            throw new ParseError("rk-err-region", null, null, null);
        }
        return regionImpl(null, value.trim(), null);
    }

    private static Region regionImpl(String key, String value, Region r) {
        try {
            final String[] comps = LIST_SPLITTER.split(value);
            if (comps.length != 4) {
                throw new IllegalArgumentException("regions are defined by exactly four values");
            }
            return fillRegion(r, Integer.parseInt(comps[0]), Integer.parseInt(comps[1]), Integer.parseInt(comps[2]), Integer.parseInt(comps[3]));
        } catch (NumberFormatException nfe) {
            // try parsing as Region2D
            try {
                Region2D r2d = region2DImpl(null, value, null);
                StrangeEons.log.log(Level.WARNING, "converted Region2D to int precision: {0}", (key == null ? value : key));
                return fillRegion(r, (int) (r2d.x + 0.5d), (int) (r2d.y + 0.5d), (int) (r2d.width + 0.5d), (int) (r2d.height + 0.5d));
            } catch (Exception e) {
                throw new ParseError("rk-err-region", key, value, nfe);
            }
        } catch (Exception e) {
            throw new ParseError("rk-err-region", key, value, e);
        }
    }

    private static Region2D region2DImpl(String key, String value, Region2D r) {
        try {
            final String[] comps = LIST_SPLITTER.split(value);
            if (comps.length != 4) {
                throw new IllegalArgumentException("regions are defined by exactly four values");
            }
            return fillRegion2D(r, Double.parseDouble(comps[0]), Double.parseDouble(comps[1]), Double.parseDouble(comps[2]), Double.parseDouble(comps[3]));
        } catch (Exception e) {
            throw new ParseError("rk-err-region", key, value, e);
        }
    }

    private static Region fillRegion(Region r, int x, int y, int w, int h) {
        if (r == null) {
            r = new Region(x, y, w, h);
        } else {
            r.x = x;
            r.y = y;
            r.width = w;
            r.height = h;
        }
        return r;
    }

    private static Region2D fillRegion2D(Region2D r, double x, double y, double w, double h) {
        if (r == null) {
            r = new Region2D(x, y, w, h);
        } else {
            r.x = x;
            r.y = y;
            r.width = w;
            r.height = h;
        }
        return r;
    }

    /**
     * Returns the {@link Region2D} represented by the value of the specified
     * key;
     * <i>the key name will have {@code -region} appended if necessary.
     * </i>
     * The region value must use one of the forms listed in
     * {@link #getRegion(java.lang.String)}.
     *
     * @param key the the name of a key, with or without the {@code -region}
     * suffix
     * @return the region described by the value of the key, or {@code null} if
     * the key is undefined
     * @throws ParseError if the value does not have the correct syntax, or if
     * any region referenced by the value is missing
     * @see #setRegion2D
     * @see #region2D
     * @see Region2D
     */
    public final Region2D getRegion2D(String key) {
        key = suffix(key, "-region");
        String value = get(key);
        if (value == null) {
            return null;
        }

        // check for a delta region
        value = value.trim();
        if (value.length() >= 3 && value.charAt(0) == 'd' && value.charAt(1) == '(' && value.charAt(value.length() - 1) == ')') {
            int comma = value.indexOf(','); // first comma splits parent region from delta values
            if (comma < 0) {
                throw new ParseError("rk-err-region", key, value);
            }

            Region2D delta = region2DImpl(key, value.substring(comma + 1, value.length() - 1), null);

            Region2D source;
            String parentName = value.substring(2, comma).trim();
            try {
                source = getRegion2D(parentName);
            } catch (ParseError de) {
                throw new ParseError("rk-err-region", key, value, de);
            }
            source.x += delta.x;
            source.y += delta.y;
            source.width += delta.width;
            source.height += delta.height;
            return source;
        }

        return region2DImpl(key, value, null);
    }

    /**
     * Returns the {@link Region2D} represented by the value of the specified
     * key;
     * <i>the key name will have {@code -region} appended if necessary.
     * </i>
     * If the value is not defined or is invalid, the default value is returned.
     *
     * @param key the the name of a key, with or without the {@code -region}
     * suffix
     * @param defaultValue the region to region if the key value is missing or
     * invalid
     * @return the region described by the value of the key
     */
    public final Region2D getRegion2D(String key, Region2D defaultValue) {
        key = suffix(key, "-region");
        try {
            final String v = get(key);
            if (v != null) {
                return region2DImpl(key, v.trim(), null);
            }
        } catch (ParseError e) {
        }
        return defaultValue;
    }

    /**
     * Sets the specified key to a suitable value for the given region;
     * <i>the key name will have {@code -region} appended if necessary.
     * </i>
     *
     * @param key the key to modify, with or without the {@code -region} suffix
     * @param region the region to write as a setting value
     */
    public final void setRegion2D(String key, Region2D region) {
        set(suffix(key, "-region"),
                String.valueOf(region.x) + ','
                + String.valueOf(region.y) + ','
                + String.valueOf(region.width) + ','
                + String.valueOf(region.height)
        );
    }

    /**
     * Returns a {@link Region2D} defined by a comma-separated list of four
     * numbers in the specified value.
     *
     * @param value a string containing a description of the region
     * @return the region described by the value
     * @throws ParseError if the value is not a valid region description
     */
    public static Region2D region2D(String value) {
        if (value == null) {
            throw new ParseError("rk-err-region", null, null, null);
        }
        return region2DImpl(null, value.trim(), null);
    }

    /**
     * Returns a {@link ca.cgjennings.layout.PageShape.CupShape CupShape} for
     * changing the flow of markup text from the value of the specified key;
     * <i>the key name will have {@code -region} appended if necessary.
     * </i>
     * The value of the key is a comma-separated list of 5 numbers, specifying
     * the left and right insets at the top of the shape, the y-position at
     * which to switch to the bottom insets, and finally the left and right
     * insets at the bottom of the shape.
     *
     * @param key the key whose value will be used to create a cup shape, with
     * or without the {@code -shape} suffix
     * @return the cup shape described by the key's value
     * @see #cupShape(java.lang.String)
     * @since 2.1
     */
    public final PageShape getCupShape(String key) {
        key = suffix(key, "-shape");
        return cupShapeImpl(key, get(key));
    }

    /**
     * Returns a {@link ca.cgjennings.layout.PageShape.CupShape CupShape} for
     * changing the flow of markup text from the specified value. The supplied
     * string must be a comma-separated list of 5 numbers, specifying the left
     * and right insets at the top of the shape, the y-position at which to
     * switch to the bottom insets, and finally the left and right insets at the
     * bottom of the shape.
     *
     * @param value the value to parse into a cup shape description
     * @return the cup shape described by the value
     * @see #cupShape(java.lang.String)
     * @since 2.1
     */
    public static PageShape.CupShape cupShape(String value) {
        return cupShapeImpl(null, value);
    }

    private static PageShape.CupShape cupShapeImpl(String key, String value) {
        try {
            value = value.trim();
            String[] comps = LIST_SPLITTER.split(value);
            if (comps.length != 5) {
                throw new IllegalArgumentException("need 5 values");
            }
            return new PageShape.CupShape(
                    Double.parseDouble(comps[0]),
                    Double.parseDouble(comps[1]),
                    Double.parseDouble(comps[2]),
                    Double.parseDouble(comps[3]),
                    Double.parseDouble(comps[4])
            );
        } catch (Exception e) {
            throw new ParseError("rk-err-shape", key, value, e);
        }
    }

    /**
     * Returns a font point size from the specified key;
     * <i>the key name will have {@code -pointsize} appended if necessary.
     * </i>
     *
     * @param key the name of the setting key, with or without the
     * {@code -pointsize} suffix
     * @return the float value of the key
     * @throws ParseError if the value is undefined or invalid
     */
    public final float getPointSize(String key) {
        key = suffix(key, "-pointsize");
        try {
            return getFloat(key);
        } catch (Exception e) {
            throw new ParseError("rk-err-pointsize", key, get(key), null);
        }
    }

    /**
     * Returns a font point size from the specified key;
     * <i>the key name will have {@code -pointsize} appended if necessary.
     * </i>
     * If the value is not defined or invalid, the default value is returned.
     *
     * @param key the name of the setting key, with or without the
     * {@code -pointsize} suffix
     * @param defaultValue the value to use if the key is not valid
     * @return the float value of the key
     * @since 3.0
     */
    public final float getPointSize(String key, float defaultValue) {
        try {
            key = suffix(key, "-pointsize");
            String v = get(key);
            if (v != null) {
                return getFloat(key);
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }

    /**
     * Returns a font based on the specified font but with the point size given
     * in the the specified key;
     * <i>the key name will have {@code -pointsize} appended if necessary.
     * </i>
     * If the value is not defined, invalid, or is the same as the source font's
     * point size, the source font will be returned. Otherwise, a new font is
     * returned that is identical to the source font except that its point size
     * matches the size defined by the value of the key.
     *
     * @param key the name of the setting key, with or without the
     * {@code -pointsize} suffix
     * @param sourceFont the font to use to derive a font at the specified point
     * size
     * @return a version of the source font with the point size given by the
     * value of the key
     * @since 3.0
     */
    public final Font getPointSize(String key, Font sourceFont) {
        try {
            key = suffix(key, "-pointsize");
            String v = get(key);
            if (v != null) {
                float ps = Float.parseFloat(v.trim());
                if (sourceFont.getSize2D() != ps) {
                    return sourceFont.deriveFont(ps);
                }
            }
        } catch (Exception e) {
        }
        return sourceFont;
    }

    /**
     * Returns an alignment value suitable for use with a markup renderer from
     * the value of the specified key;
     * <i>the key name will have {@code -alignment} appended if necessary.
     * </i>
     * The value will be converted into an alignment value as described under
     * {@link #textAlignment}.
     *
     * @param key the the name of a key, with or without the {@code -alignment}
     * suffix
     * @return a binary or of alignment flags
     * @throws ParseError if the value is invalid
     * @see #getTextAlignment(java.lang.String,
     * ca.cgjennings.layout.MarkupRenderer)
     * @see MarkupRenderer#setAlignment(int)
     */
    public final int getTextAlignment(String key) {
        String value;
        key = suffix(key, "-alignment");
        value = get(key);
        int align;
        if (value == null) {
            align = MarkupRenderer.LAYOUT_LEFT | MarkupRenderer.LAYOUT_TOP;
        } else {
            align = textAlignmentImpl(key, value);
        }
        return align;
    }

    /**
     * Sets the text alignment of a markup renderer to match the value of an
     * alignment key;
     * <i>the key name will have {@code -alignment} appended if necessary.
     * </i>
     * The value will be converted into an alignment value as described under
     * {@link #textAlignment}.
     *
     * @param key the the name of a key, with or without the {@code -alignment}
     * suffix
     * @param renderer the markup renderer which will be modified
     * @throws ParseError if the value is invalid
     * @see #textAlignment
     * @see MarkupRenderer#setAlignment(int)
     */
    public final void getTextAlignment(String key, MarkupRenderer renderer) {
        renderer.setAlignment(getTextAlignment(key));
    }

    /**
     * Returns an alignment value suitable for use with a
     * {@link MarkupRenderer}. The alignment value is a comma-separated list of
     * the following case-insensitive tokens:
     * <dl>
     * <dt>{@code left} <dd>sets the horizontal alignment to left-aligned
     *
     * <dt>{@code right} <dd>sets the horizontal alignment to right-aligned
     * <dt>{@code center} <dd>sets the horizontal alignment to center-aligned
     * <dt>{@code centre} <dd>synonym for {@code center}
     * <dt>{@code top} <dd>sets the vertical alignment to top-aligned
     *
     * <dt>{@code bottom} <dd>sets the vertical alignment to bottom-aligned
     * <dt>{@code middle} <dd>sets the vertical alignment to center-aligned
     * <dt>{@code justified} <dd>sets text justification to justified
     * <dt>{@code unjustified} <dd>sets text justification to unjustified
     *
     * <dt>{@code ragged} <dd>synonym for {@code unjustified}
     * </dl>* default value for category
     *
     * <p>
     * The tokens are divided into three categories: horizontal alignment,
     * vertical alignment, and justification. If more than one token in the same
     * category appears in the list, the rightmost value takes precedence. For
     * example, the value {@code right, top, left} would result in left- and
     * top-aligned text. If any category is left unspecified, the default value
     * for the category is used.
     *
     * @param value the text alignment value to parse
     * @return the bitwise or of the last-specified alignment value in each
     * category
     */
    public static int textAlignment(String value) {
        return textAlignmentImpl(null, value);
    }

    private static int textAlignmentImpl(String key, String value) {
        try {
            int valign = MarkupRenderer.LAYOUT_TOP;
            int halign = MarkupRenderer.LAYOUT_LEFT;
            int justify = 0;
            if (value != null) {
                String[] tokens = LIST_SPLITTER.split(value.trim());
                for (String token : tokens) {
                    if (token.isEmpty()) {
                        continue;
                    }
                    if (token.equalsIgnoreCase("left")) {
                        halign = MarkupRenderer.LAYOUT_LEFT;
                    } else if (token.equalsIgnoreCase("right")) {
                        halign = MarkupRenderer.LAYOUT_RIGHT;
                    } else if (token.equalsIgnoreCase("center") || token.equalsIgnoreCase("centre")) {
                        halign = MarkupRenderer.LAYOUT_CENTER;
                    } else if (token.equalsIgnoreCase("top")) {
                        valign = MarkupRenderer.LAYOUT_TOP;
                    } else if (token.equalsIgnoreCase("bottom")) {
                        valign = MarkupRenderer.LAYOUT_BOTTOM;
                    } else if (token.equalsIgnoreCase("middle")) {
                        valign = MarkupRenderer.LAYOUT_MIDDLE;
                    } else if (token.equalsIgnoreCase("justified")) {
                        justify = MarkupRenderer.LAYOUT_JUSTIFY;
                    } else if (token.equalsIgnoreCase("unjustified") || token.equalsIgnoreCase("ragged")) {
                        justify = 0;
                    } else {
                        throw new IllegalArgumentException("unknown token: " + token);
                    }
                }
            }
            return valign | halign | justify;
        } catch (Exception e) {
            throw new ParseError("rk-err-alignment", key, value);
        }
    }

    /**
     * Returns a {@link TextStyle} suitable for use with markup from the value
     * of the specified key;
     * <i>the key name will have {@code -style} appended if necessary. </i>
     * The value will converted into a text style as described under
     * {@link #textStyle}.
     *
     * @param key the the name of a key, with or without the {@code -style}
     * suffix
     * @param base a baseline style to modify, or {@code null} to create a new
     * style
     * @return the text style described by the value of the key
     * @throws ParseError if the value is missing or is invalid
     */
    public final TextStyle getTextStyle(String key, TextStyle base) {
        String value = null;
        try {
            key = suffix(key, "-style");
            value = get(key);
            return textStyleImpl(key, value, base);
        } catch (ParseError pe) {
            throw pe;
        } catch (Exception e) {
            throw new ParseError("rk-err-style", key, value, e);
        }
    }

    /**
     * Returns a {@link TextStyle} suitable for use with markup from the
     * specified value. If a base text style is provided, then that style will
     * be modified with the style information embedded in the value. Otherwise,
     * a new, empty style will be created and filled in from the value. The
     * value describes zero or more style settings using the following format:
     *
     * <p>
     * Style settings are separated from each other by semicolons (';'). Each
     * style setting consists of two parts, a key name and a value, separated by
     * a colon (':'). The key is the name of a {@link TextAttribute} key. It not
     * case sensitive, and you may substitute spaces for underscores. For
     * example, the key name "underline" matches
     * {@link TextAttribute#UNDERLINE}, and the key name "numeric shaping"
     * matches {@link TextAttribute#NUMERIC_SHAPING}. By default, a value must
     * be the name of one of the predefined values found in
     * {@code TextAttribute}; these names are also case insensitive and allow
     * the use of spaces for underscores. For example, the following style
     * setting would enable underlined text:<br> {@code underline: underline on}
     *
     * <p>
     * Some attribute keys require values that are not predefined. For example,
     * the {@code FAMILY} key requires a string that names the desired font. The
     * parser lets you describe strings, colours, integers and floating point
     * numbers literally. Other values can be set using script expressions.
     *
     * <p>
     * <b>Examples and Additional Information</b><br>
     * String values must be explicitly marked off with quotes (' or "), or the
     * text will be interpreted as a text attribute constant. When setting a
     * text attribute constant, you can leave off the prefix for the key type.
     * In this example, the parser infers that {@code oblique} means
     * {@code POSTURE_OBLIQUE} from the fact that the key is
     * {@code POSTURE}:<br> {@code family: 'Helvetica'; posture: oblique}
     *
     * <p>
     * Integers and floating point values are written as an optional sign
     * followed by a sequence of digits. Sequences that include a decimal point
     * are interpreted as floating point values. Colour values are specified
     * using the same formats as described under
     * {@link #colour(java.lang.String)}; colours described with a sequence of
     * hex digits must start with a '#' to distinguish them from integers.
     *
     * <p>
     * Placing braces around a value causes it to be interpreted as a script
     * expression. The expression is evaluated, and the result becomes the value
     * assigned to the attribute. This can be used to determine a value
     * dynamically at runtime or to create appropriate objects for keys that
     * take values not supported by the parser, such as
     * {@link TransformAttribute}:<br>
     * {@code family: {ResourceKit.bodyFamily}; size: 10; foreground: #ff0000;}<br>
     *
     * @param value the style specification to parse; see above for format
     * @param base a baseline style to modify, or {@code null} to create a new
     * style
     * @return the modified baseline style, or a new style if the baseline style
     * was {@code null}
     * @see MarkupRenderer
     * @see TextStyle
     * @see <a href='scriptdoc:markup'>markup scripting library</a>
     */
    public static TextStyle textStyle(String value, TextStyle base) {
        return textStyleImpl(null, value, base);
    }

    private static TextStyle textStyleImpl(String key, String value, TextStyle base) {
        try {
            if (base == null) {
                base = new TextStyle();
            }
            int s = 0;
            while (s < value.length()) {
                s = nextStyle(base, value, s);
            }
            return base;
        } catch (Throwable t) {
            throw new ParseError("rk-err-style", key, value, t);
        }
    }

    private static int nextStyle(TextStyle base, String styleDesc, int s) {
        // Format is KEY1: VALUE1; KEY2: VALUE2; ...
        int attr = styleDesc.indexOf(':', s);
        if (attr >= 0) {
            String keyName = cleanStyleKey(styleDesc.substring(s, attr).trim());
            Object key = styleLookup(null, keyName);
            if (!(key instanceof TextAttribute)) {
                throw new IllegalArgumentException("token is not a TextAttribute: \"" + keyName + '"');
            }

            int token = findValueEnd(styleDesc, attr + 1);
            String valueText = styleDesc.substring(attr + 1, token).trim();

            // remove literal markers if present; if literal markers are present,
            // then the value is that object and no additional parsing will be done
            Object value = null;
            boolean isLiteral = false;
            if (!valueText.isEmpty()) {
                char start = valueText.charAt(0);
                char end = valueText.length() > 1 ? valueText.charAt(valueText.length() - 1) : '\0';
                switch (start) {
                    case '{':
                        isLiteral = true;
                        valueText = valueText.substring(1, valueText.length() - (end == '}' ? 1 : 0));
                        ScriptMonkey sm = new ScriptMonkey("style setting literal");
                        sm.eval(
                                "importPackage(gamedata);"
                                + "importPackage(resources);"
                                + "importClass(java.awt.font.TextAttribute);"
                                + "importClass(java.awt.font.TransformAttribute);"
                                + "importClass(java.awt.geom.AffineTransform);"
                        );
                        value = sm.eval(valueText);
                        break;
                    case '\'':
                    case '"':
                        isLiteral = true;
                        value = valueText.substring(1, valueText.length() - (end == start ? 1 : 0));
                        break;
                }
            }

            // if it wasn't a string literal or code literal, try to parse
            // other object types out, including TextAttribute constants
            if (!isLiteral) {
                if (valueText.isEmpty()) {
                    value = null;
                } else if (COLOUR_MATCHER.matcher(valueText).find()) {
                    value = colour(valueText);
                } else if (INT_MATCHER.matcher(valueText).matches()) {
                    value = Integer.valueOf(valueText);
                } else if (FLOAT_MATCHER.matcher(valueText).matches()) {
                    value = Float.valueOf(valueText);
                } else {
                    valueText = cleanStyleKey(valueText);
                    value = styleLookup(keyName, valueText);
                    if (value instanceof TextAttribute) {
                        throw new IllegalArgumentException("expected value but found TextAttribute: \"" + valueText + '"');
                    }
                }
            }

            base.add((TextAttribute) key, value);

            return token + 1;
        }
        return styleDesc.length();
    }

    @SuppressWarnings("empty-statement")
    private static int findValueEnd(String styleDesc, int start) {
        int i = start;

        // skip past initial whitespace, if any
        for (; i < styleDesc.length() && Character.isSpaceChar(styleDesc.charAt(i)); ++i);

        // nothing but spaces
        if (i >= styleDesc.length()) {
            return styleDesc.length();
        }

        // if the first char is {, ', or " then this is the start of a string;
        // otherwise we simply find the terminating ; and we are done
        char stackOpen = styleDesc.charAt(i);
        char stackClose;
        switch (stackOpen) {
            case '{':
                stackClose = '}';
                break;
            case '\'':
            case '"':
                stackClose = stackOpen;
                break;
            default:
                int semi = styleDesc.indexOf(';', i);
                return semi < 0 ? styleDesc.length() : semi;
        }

        ++i;
        int stackCount = 1;
        for (; i < styleDesc.length(); ++i) {
            char c = styleDesc.charAt(i);
            if (c == stackOpen && stackOpen != stackClose) {
                ++stackCount;
            } else if (c == stackClose) {
                if (--stackCount < 0) {
                    throw new IllegalArgumentException(stackClose + " without " + stackOpen + " at " + (i + 1));
                }
            } else if (c == ';' && stackCount == 0) {
                return i;
            }
        }

        if (stackCount > 0) {
            throw new IllegalArgumentException("unclosed " + stackOpen + " literal at " + (i + 1));
        }
        return styleDesc.length();
    }

    private static String cleanStyleKey(String key) {
        return key.toUpperCase(Locale.CANADA).replace(' ', '_');
    }

    private static final Pattern INT_MATCHER = Pattern.compile("[+-]?[0-9]+");
    private static final Pattern FLOAT_MATCHER = Pattern.compile("[+-]?[0-9]*\\.[0-9]+");
    private static final Pattern COLOUR_MATCHER = Pattern.compile("\\#|((rgba?)\\()|(hsba?\\()", Pattern.CASE_INSENSITIVE);

    private static Object styleLookup(String base, String name) {
        if (STYLE_MAP.containsKey(name)) {
            return STYLE_MAP.get(name);
        }
        if (base != null) {
            base = base + '_' + name;
            if (STYLE_MAP.containsKey(base)) {
                return STYLE_MAP.get(base);
            }
        }
        throw new IllegalArgumentException("unknown token: " + base);
    }

    private static final HashMap<String, Object> STYLE_MAP = new HashMap<>(100);

    static {
        try {
            for (Field f : TextAttribute.class.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                STYLE_MAP.put(f.getName(), f.get(null));
            }
        } catch (Throwable t) {
            StrangeEons.log.log(Level.SEVERE, "unable to init text style map", t);
        }
        STYLE_MAP.put("JUSTIFICATION_OFF", 0f);
        STYLE_MAP.put("KERNING_OFF", 0);
        STYLE_MAP.put("LIGATURES_OFF", 0);
        STYLE_MAP.put("POSTURE_ITALIC", TextAttribute.POSTURE_OBLIQUE);
        STYLE_MAP.put("STRIKETHROUGH_OFF", false);
        STYLE_MAP.put("SWAP_COLORS_OFF", false);
        STYLE_MAP.put("SWAP_COLOURS", TextAttribute.SWAP_COLORS);
        STYLE_MAP.put("SWAP_COLOURS_ON", true);
        STYLE_MAP.put("SWAP_COLOURS_OFF", false);
        STYLE_MAP.put("TRACKING_DEFAULT", 0f);
        STYLE_MAP.put("UNDERLINE_OFF", -1);
        STYLE_MAP.put("WIDTH_SEMICONDENSED", TextAttribute.WIDTH_SEMI_CONDENSED);
        STYLE_MAP.put("WIDTH_SEMIEXTENDED", TextAttribute.WIDTH_SEMI_EXTENDED);
//		STYLE_MAP.put( "",  );
    }

    /**
     * Returns a text-fitting method constant from the value of the specified
     * key;
     * <i>the key name will have {@code -text-fitting} appended if necessary.
     * </i>
     * The value is mapped to a text-fitting constant as follows:
     * <pre>
     * "none"    MarkupRenderer.FIT_NONE
     * "spacing" MarkupRenderer.FIT_TIGHTEN_LINE_SPACING
     * "scaling" MarkupRenderer.FIT_SCALE_TEXT
     * "both"    MarkupRenderer.FIT_BOTH
     * </pre>
     *
     * @param key the key, with or without the {@code -text-fitting} suffix
     * @return a text fitting constant suitable for use with a
     * {@link MarkupRenderer}.
     */
    public int getTextFittingMethod(String key) {
        key = suffix(key, "-text-fitting");
        String v = get(key);
        if (v == null) {
            v = get("default-text-fitting");
            return MarkupRenderer.FIT_BOTH;
        }
        v = v.trim();

        int result = MarkupRenderer.FIT_NONE;
        if (v.equalsIgnoreCase("none")) {
            result = MarkupRenderer.FIT_NONE;
        } else if (v.equalsIgnoreCase("spacing")) {
            result = MarkupRenderer.FIT_TIGHTEN_LINE_SPACING;
        } else if (v.equalsIgnoreCase("scaling")) {
            result = MarkupRenderer.FIT_SCALE_TEXT;
        } else if (v.equalsIgnoreCase("both")) {
            result = MarkupRenderer.FIT_BOTH;
        } else {
            throw new ParseError("rk-err-textfit", key, v);
        }
        return result;
    }

    /**
     * Returns the code(s) for the selected expansion(s), assuming that these
     * settings are a game component's private settings. If no expansion is set,
     * it will return the code for the generic "base game" expansion.
     *
     * @return the expansion code set on this settings instance, or the base
     * game code
     * @see
     * Expansion#getComponentExpansionSymbols(ca.cgjennings.apps.arkham.component.GameComponent)
     * @see Sheet#parseExpansionList(java.lang.String)
     * @see Settings#getExpansionVariant(ca.cgjennings.apps.arkham.sheet.Sheet)
     */
    public String getExpansionCode() {
        String code = get(Expansion.EXPANSION_SETTING_KEY);

        // we used to check for if the expanison existed here, but now
        // this is handled by the expansion parser
        if (code != null) {
            code = code.trim();
        }
        if (code == null || code.length() == 0) {
            code = "NX";
        }
        return code;
    }

    /**
     * Returns the expansion logical variant index for the selected expansion,
     * assuming that these settings are a game component's private settings. If
     * no variant is set, returns 0. If the variant is a boolean value (which
     * was a valid value in versions of Strange Eons prior to 3), it will be
     * silently converted to "0" or "1".
     *
     * <p>
     * If the settings do not define a value for the variant key
     * ({@link Expansion#VARIANT_SETTING_KEY}) and a non-{@code null}
     * {@link Sheet} instance is provided, then a default value will be
     * determined by looking up the value of the key
     * {@code sheetWithDefaultValue.getExpansionSymbolKey() + "-invert"}. This
     * is for backwards compatibility with components from previous versions. To
     * set a default variant for a component, it is recommended that you set the
     * value of the {@code Expansion.VARIANT_SETTING_KEY} key with the desired
     * default variant when the component is first created.
     *
     * @param sheetWithDefaultValue optional sheet to use to determine a default
     * value
     * @return the logical variant index for the component, or "0" if none is
     * set
     * @see #getExpansionCode()
     */
    public int getExpansionVariant(Sheet<? extends GameComponent> sheetWithDefaultValue) {
        // determine the symbol variant
        String variant = get(Expansion.VARIANT_SETTING_KEY);

        if (variant == null && sheetWithDefaultValue != null) {
            final String expSymKey = sheetWithDefaultValue.getExpansionSymbolKey() + "-invert";
            variant = get(expSymKey);
        }

        if (variant == null || variant.isEmpty()) {
            variant = "0";
        } else {
            char c = variant.charAt(0);
            if (!(c >= '0' && c <= '9')) {
                variant = yesNo(variant) ? "1" : "0";
            }
        }

        int intVariant = 0;
        try {
            intVariant = Integer.parseInt(variant);
            if (intVariant < 0) {
                intVariant = 0;
            }
        } catch (NumberFormatException e) {
            intVariant = 0;
        }

        return intVariant;
    }

    /**
     * Returns the {@link URL} of a resource from the value of the specified
     * key. The value of the key will be looked up using
     * {@link #get(java.lang.String)}, and if the value is non-{@code null} then
     * it will be converted into a URL as if by
     * {@link ResourceKit#composeResourceURL} using the value of the key. If the
     * key is not defined, a {@link ParseError} will be thrown.
     *
     * @param key a key whose value is a resource URL
     * @return the location of the resource, if it exists, or {@code null}
     * @see ResourceKit#composeResourceURL(java.lang.String)
     * @since 2.1
     */
    public final URL getResourceURL(String key) {
        String res = get(key);
        if (key == null) {
            throw new ParseError("rk-err-compose-url", key, null);
        }

        URL url = ResourceKit.composeResourceURL(res);
        if (StrangeEons.log.isLoggable(Level.FINE)) {
            StrangeEons.log.log(Level.FINE, "resource setting \"{0}\" = \"{1}\"", new Object[]{key, url});
        }
        return url;
    }

    /**
     * Return an image for the resource identified by the value of the specified
     * key. The value of the key is treated as a resource identifier and loaded
     * as if by {@link ResourceKit#getImage(java.lang.String)}. If the value of
     * the key is {@code null}, a placeholder "missing image" graphic is
     * returned.
     *
     * @param key a key whose value points to an image file
     * @return the image specified by the value of the key
     * @since 2.1
     */
    public final BufferedImage getImageResource(String key) {
        String v = get(key);
        return v == null ? ResourceKit.getMissingImage() : ResourceKit.getImage(v);
    }

    /**
     * Returns an image resource that is one in a numbered sequence. The value
     * of the key should include the string "00". This string will be replaced
     * by the value of the specified number. If the number is less than 10, it
     * will be preceded by a 0. The value, after the number has been replaced,
     * will be passed to {@link ResourceKit#getImage(java.lang.String)} to load
     * an appropriate image. If the value of the key is {@code null}, a
     * placeholder "missing image" graphic is returned.
     *
     * <p>
     * For example, suppose the following files are included in a plug-in:<br>
     * <pre>
     * resources/xyzzy/sample-00-image.png
     * resources/xyzzy/sample-01-image.png
     * resources/xyzzy/sample-02-image.png
     * </pre> If the value of the key {@code sample-images} is
     * {@code xyzzy/sample-00-image.png}, then calling
     * {@code getNumberedImageResource( "sample-images", 2 )} would return the
     * {@code sample-02-image.png} resource as an image.
     *
     * @param key the key that contains the base name of the image resource
     * @param number the index of the desired image in the sequence
     * @return the image for the specified number in the sequence, as determined
     * by the above algorithm
     * @throws IllegalArgumentException if the number is negative
     */
    public final BufferedImage getNumberedImageResource(String key, int number) {
        if (number < 0) {
            throw new IllegalArgumentException("number < 0");
        }

        String v = get(key);
        BufferedImage bi;
        if (v == null) {
            bi = ResourceKit.getMissingImage();
        } else if (number == 0) {
            bi = ResourceKit.getImage(v);
        } else {
            String repl = number < 10 ? '0' + String.valueOf(number) : String.valueOf(number);
            bi = ResourceKit.getImage(v.replace("00", repl));
        }
        return bi;
    }

    /**
     * Return an icon for the resource identified by the value of the specified
     * key. The value of the key is treated as a resource identifier and loaded
     * as if by {@link ResourceKit#getIcon(java.lang.String)}. If the value of
     * the key is {@code null}, a placeholder "missing image" icon is returned.
     *
     * @param key a key whose value points to an image file
     * @return an icon of the image
     * @since 2.00.7
     */
    public final Icon getIconResource(String key) {
        final String v = get(key);
        return v == null ? new ImageIcon(ResourceKit.getMissingImage())
                : ResourceKit.getIcon(v);
    }

    /**
     * Registers a font family using the value of a setting key. The value of
     * the key must be a comma-separated list of font resources. If the
     * registration fails, an error will be displayed and a fallback family will
     * be returned.
     *
     * @param key the name of the family key to register
     * @return the family name to use for the font
     * @see ResourceKit#registerFontFamily(java.lang.String)
     * @throws NullPointerException if the key is {@code null}
     * @since 3.0
     */
    public final String registerFontFamily(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        try {
            String value = get(key);
            if (value == null) {
                throw new ParseError(key);
            } else {
                ResourceKit.FontRegistrationResult[] results = ResourceKit.registerFontFamily(value);
                if (results.length > 0) {
                    return results[0].getFamily();
                }
            }
        } catch (Exception e) {
            displayError(string("rk-err-font", key), e);
        }
        return Font.SERIF;
    }

    /**
     * Saves the position of a window for later recall by writing it as a group
     * of settings. The {@code prefix} is a unique prefix for that window. To
     * restore the position later, call {@link #applyWindowSettings} using the
     * same prefix. Usually used with {@linkplain #getUser() user settings}.
     *
     * @param prefix a unique name for the window
     * @param window the window whose shape should be written into settings
     */
    public final void storeWindowSettings(String prefix, Window window) {
        // if doing a plug-in test, save window settings to separate test keys
        if (BundleInstaller.hasTestBundles()) {
            prefix += "plugin-test-mode";
        }

        StrangeEons.log.log(Level.INFO, "storing window settings {0}", prefix);
        boolean writeBounds = true;

        GraphicsConfiguration config = window.getGraphicsConfiguration();
        String id = config == null ? "" : config.getDevice().getIDstring();
        set(prefix + "-device", id);

        if (window instanceof Frame) {
            int state = ((Frame) window).getExtendedState();
            if (state == 0 || (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
                set(prefix + "-maximized", (state != 0) ? "yes" : "no");
            } else {
                writeBounds = false;
            }
        }
        if (writeBounds) {
            Rectangle r = window.getBounds();
            set(prefix + "-bounds", String.format("%d,%d,%d,%d", r.x, r.y, r.width, r.height));
        }
    }

    /**
     * Restores the position and size of a window from the last time its
     * settings were stored in this settings object.
     *
     * @param prefix the unique prefix chosen for this window when it was stored
     * @param window the window to be restored
     * @return {@code true} if there were settings to restore; otherwise you
     * should use a default placement algorithm to place the window
     */
    public final boolean applyWindowSettings(String prefix, Window window) {
        // if doing a plug-in test, save window settings to separate test keys
        if (BundleInstaller.hasTestBundles()) {
            prefix += "plugin-test-mode";
        }

        StrangeEons.log.log(Level.FINE, "applying window settings {0}", prefix);

        String frameValue = get(prefix + "-bounds");
        if (frameValue != null) {
            Rectangle stored = Settings.region(frameValue);
            if (stored != null) {
                boolean maximize = false;
                if (window instanceof Frame) {
                    maximize = getYesNo(prefix + "-maximized");
                }
                if (maximize) {
                    // When maximized, we need to ignore the stored bounds
                    // since they will reflect the maximized size, not the size
                    // that the window should be when restored (which cannot
                    // be reliably obtained from the AWT).
                    //
                    // 1. Check which display it was maximized on
//					String id = config == null ? "" : config.getDevice().getIDstring();
//					set( prefix + "-device", id );

                    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice deviceToUse = env.getDefaultScreenDevice();
                    String id = get(prefix + "-device", "");
                    if (!id.isEmpty()) {
                        for (GraphicsDevice gd : env.getScreenDevices()) {
                            if (gd.getIDstring().equals(id)) {
                                deviceToUse = gd;
                                break;
                            }
                        }
                    }
                    if (deviceToUse.equals(env.getDefaultScreenDevice())) {
                        window.setLocationByPlatform(true);
                    } else {
                        Rectangle devBounds = deviceToUse.getDefaultConfiguration().getBounds();
                        GraphicsConfiguration config = deviceToUse.getDefaultConfiguration();
                        Insets insets = window.getToolkit().getScreenInsets(config);
                        devBounds.x += insets.left + 16;
                        devBounds.y += insets.top + 16;
                        devBounds.width -= (insets.left + insets.right + 32);
                        devBounds.height -= (insets.top + insets.bottom + 32);
                        devBounds.width = devBounds.width * 2 / 3;
                        devBounds.height = devBounds.height * 2 / 3;
                        window.setBounds(devBounds);
                    }
                    ((Frame) window).setExtendedState(Frame.MAXIMIZED_BOTH);
                } else {
                    // ensure window is not entirely offscreen current display configuration
                    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    Rectangle bounds = new Rectangle(0, 0, 0, 0);
                    for (GraphicsDevice dev : env.getScreenDevices()) {
                        bounds.add(dev.getDefaultConfiguration().getBounds());
                    }
                    Rectangle clipped = stored.intersection(bounds);
                    if (stored.width == 0) {
                        stored = bounds;
                    }

                    if (getYesNo("clip-window-extents")) {
                        window.setBounds(clipped);
                    } else {
                        window.setBounds(clipped.x, clipped.y, stored.width, stored.height);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a string that describes this settings instance.
     *
     * @return a description of the settings scope
     */
    @Override
    public String toString() {
        return "Settings (Private, instance " + Integer.toHexString(System.identityHashCode(this)) + ")";
    }

    /**
     * Adds a property change listener for this scope. The listener will be
     * notified when any key's value changes at this scope. The name of the
     * property specified by the event is the name of the key that is changing.
     * Changes made in parent scopes will not fire an event, but you may also
     * add listeners to each of the parent scopes.
     *
     * @param listener the listener to call when a key's value changes
     * @since 2.1
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (listeners == null) {
            listeners = new LinkedList<>();
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener that was previously added to this {@code Settings}.
     *
     * @param listener the listener to remove
     * @since 2.1
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            listeners = null;
        }
    }

    /**
     * Fire a property change event for the key {@code property} to signify that
     * its value is changing from {@code oldValue} to {@code newValue}. If the
     * old and new value are equal, no event will be fired.
     *
     * @param property the name of the property (key) that is changing
     * @param oldValue the previous value of the key
     * @param newValue the new value of the key
     */
    final void firePropertyChangeEvent(String property, String oldValue, String newValue) {
        if (listeners == null) {
            return;
        }
        if (oldValue == null ? newValue == null : oldValue.equals(newValue)) {
            return;
        }

        PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
        for (PropertyChangeListener li : listeners) {
            li.propertyChange(event);
        }
    }

    /**
     * Returns {@code true} if this {@code Settings} has any property change
     * listeners attached.
     *
     * @return {@code true} if and only if there are added listeners
     */
    final boolean hasPropertyChangeListeners() {
        return listeners != null && !listeners.isEmpty();
    }

    private transient List<PropertyChangeListener> listeners = null;

    /**
     * A {@link PropertyChangeListener} that will print diagnostic information
     * to the script console when a setting changes.
     *
     * @since 2.1
     */
    public static final PropertyChangeListener DEBUG_LISTENER = (PropertyChangeEvent evt) -> {
        PrintWriter pw = ScriptMonkey.getSharedConsole().getErrorWriter();
        pw.format("[%s]: ", evt.getPropertyName());
        if (evt.getOldValue() == null) {
            if (evt.getNewValue() == null) {
                pw.println("set <null> to <null>");
            }
            pw.format("set <null> to \"%s\"", evt.getNewValue());
        } else if (evt.getNewValue() == null) {
            pw.format("set \"%s\" to <null>", evt.getOldValue());
        } else {
            pw.format("set: \"%s\" to \"%s\"", evt.getOldValue(), evt.getNewValue());
        }
        pw.format(" (In: %s)\n", evt.getSource());
    };

    private static final int CURRENT_VERSION = 4;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_VERSION);
        out.writeObject(p);
        if (parent != null && parent.isSerializedParent()) {
            out.writeObject(parent);
        } else {
            out.writeObject(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        NewerVersionException.check(CURRENT_VERSION, version);

        if (version < 2) {
            Properties temp = (Properties) in.readObject();
            if (temp != null) {
                for (Object s : temp.keySet()) {
                    try {
                        String key = (String) s;
                        set(key, temp.getProperty(key));
                    } catch (ClassCastException e) {
                    }
                }
            }
        } else {
            p = (HashMap<String, String>) in.readObject();
        }
        if (version >= 3) {
            parent = (Settings) in.readObject();
            // version 3 accidentally wrote the game settings parent,
            // so we have to read this in, but null it out so the current
            // game settings are applied below
            if (version == 3) {
                parent = null;
            }
        } else {
            parent = null;
        }
        if (parent == null && getClass() == Settings.class) {
            String v = get(Game.GAME_SETTING_KEY);
            if (v != null) {
                set(Game.GAME_SETTING_KEY, v);
            }
        }
    }

    /**
     * A specialized {@code Rectangle} that is more easily converted to or from
     * a setting value.
     */
    public static class Region extends Rectangle {

        private static final long serialVersionUID = 5_735_765_786_377_364_767L;

        /**
         * Creates a new region from a string using the same format as a region
         * setting value.
         *
         * @param settingValue the setting value to create a region from
         */
        public Region(String settingValue) {
            Settings.regionImpl(null, settingValue, this);
        }

        /**
         * Creates a new region whose upper-left corner is (0,0) and whose width
         * and height are specified by the {@code Dimension} argument.
         *
         * @param d a {@code Dimension}, specifying width and height
         */
        public Region(Dimension d) {
            super(d);
        }

        /**
         * Creates a new region whose upper-left corner is the specified
         * {@code Point}, and whose width and height are both zero.
         *
         * @param p a {@code Point} that is the top left corner of the region
         */
        public Region(Point p) {
            super(p);
        }

        /**
         * Creates a new region whose upper-left corner is specified by the
         * {@link Point} argument, and whose width and height are specified by
         * the {@link Dimension} argument.
         *
         * @param p a {@code Point} that is the upper-left corner of the region
         * @param d a {@code Dimension}, representing the width and height of
         * the region
         */
        public Region(Point p, Dimension d) {
            super(p, d);
        }

        /**
         * Creates a new region whose upper-left corner is at (0,0) in the
         * coordinate space, and whose width and height are specified by the
         * arguments of the same name.
         *
         * @param width the width of the region
         * @param height the height of the region
         */
        public Region(int width, int height) {
            super(width, height);
        }

        /**
         * Creates a new region whose upper-left corner is specified as
         * {@code (x,y)} and whose width and height are specified by the
         * arguments of the same name.
         *
         * @param x the specified X coordinate
         * @param y the specified Y coordinate
         * @param width the width of the region
         * @param height the height of the region
         */
        public Region(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        /**
         * Creates a new region from the specified rectangle.
         *
         * @param r the rectangle to copy
         */
        public Region(Rectangle r) {
            super(r);
        }

        /**
         * Creates a new region whose upper-left corner is at (0,&nbsp;0) in the
         * coordinate space, and whose width and height are both zero.
         */
        public Region() {
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder(20);
            b.append(x).append(',').append(y).append(',')
                    .append(width).append(',').append(height);
            return b.toString();
        }
    }

    /**
     * A specialized {@code Rectangle2D} that is more easily converted to or
     * from a setting value.
     */
    public static class Region2D extends Rectangle2D.Double {

        private static final long serialVersionUID = 87_868_654_635_425_654L;

        /**
         * Creates a new region from a string using the same format as a region
         * setting value.
         *
         * @param settingValue the setting value to create a region from
         */
        public Region2D(String settingValue) {
            Settings.region2DImpl(null, settingValue, this);
        }

        /**
         * Creates a new region from the specified rectangle.
         *
         * @param r the rectangle to convert
         */
        public Region2D(Rectangle2D.Double r) {
            super(r.x, r.y, r.width, r.height);
        }

        /**
         * Creates a new region with the specified coordinates and dimensions.
         *
         * @param x the x-coordinate
         * @param y the y-coordinate
         * @param w the region width
         * @param h the region height
         */
        public Region2D(double x, double y, double w, double h) {
            super(x, y, w, h);
        }

        /**
         * Creates a new region at (0,0) with zero width and height.
         */
        public Region2D() {
        }

        /**
         * Returns an integer precision region by rounding this region's
         * coordinates and dimensions.
         *
         * @return an integer precision region
         */
        public Region toRegion() {
            return new Region((int) (x + 0.5d), (int) (y + 0.5d), (int) (width + 0.5d), (int) (height + 0.5d));
        }

        /**
         * Returns a string value suitable for storing this region in a setting.
         *
         * @return a regions string with form {@code x,y,width,height}
         */
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder(20);
            b.append(x).append(',').append(y).append(',')
                    .append(width).append(',').append(height);
            return b.toString();
        }
    }

    /**
     * A {@link Color} subclass that is specialized to convert more easily to
     * and from setting values. This class is identical to {@code Color} except
     * that it provides an additional constructor that takes a setting value,
     * and its {@code toString()} method returns a setting value.
     *
     * @since 2.1
     */
    public static class Colour extends Color {

        private static final long serialVersionUID = 4_578_357_746_764_736L;

        /**
         * Creates a colour from a setting value.
         *
         * @param settingValue a hex digit string encoding the colour value in
         * RRGGBB or AARRGGBB format
         * @see #toString
         * @see Settings#colour(java.lang.String)
         */
        public Colour(String settingValue) {
            super(colourImpl(null, settingValue), true);
        }

        /**
         * Creates a colour in the specified {@code ColorSpace} with the colour
         * components specified in the {@code float} array and the specified
         * alpha. The number of components is determined by the type of the
         * {@code ColorSpace}. For example, RGB requires 3 components, but CMYK
         * requires 4 components.
         *
         * @param cspace the {@code ColorSpace} to be used to interpret the
         * components
         * @param components an arbitrary number of color components that is
         * compatible with the {@code ColorSpace}
         * @param alpha alpha value
         * @throws IllegalArgumentException if any of the values in the
         * {@code components} array or {@code alpha} is outside of the range 0.0
         * to 1.0
         * @see #getComponents
         * @see #getColorComponents
         */
        public Colour(ColorSpace cspace, float[] components, float alpha) {
            super(cspace, components, alpha);
        }

        /**
         * Creates an sRGB colour with the specified red, green, blue, and alpha
         * values in the range (0.0 - 1.0). The actual colour used in rendering
         * depends on finding the best match given the colour space available
         * for a particular output device.
         *
         * @throws IllegalArgumentException if {@code r}, {@code g} {@code b} or
         * {@code a} are outside of the range 0.0 to 1.0, inclusive
         * @param r the red component
         * @param g the green component
         * @param b the blue component
         * @param a the alpha component
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getAlpha
         * @see #getRGB
         */
        public Colour(float r, float g, float b, float a) {
            super(r, g, b, a);
        }

        /**
         * Creates an opaque sRGB colour with the specified red, green, and blue
         * values in the range (0.0 - 1.0). Alpha is defaulted to 1.0. The
         * actual colour used in rendering depends on finding the best match
         * given the color space available for a particular output device.
         *
         * @throws IllegalArgumentException if {@code r}, {@code g} or {@code b}
         * are outside of the range 0.0 to 1.0, inclusive
         * @param r the red component
         * @param g the green component
         * @param b the blue component
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getRGB
         */
        public Colour(float r, float g, float b) {
            super(r, g, b);
        }

        /**
         * Creates an sRGB colour with the specified combined ARGB value
         * consisting of the alpha component in bits 24-31, the red component in
         * bits 16-23, the green component in bits 8-15, and the blue component
         * in bits 0-7. If the {@code hasAlpha} argument is {@code false}, alpha
         * is defaulted to 255.
         *
         * @param argb the combined ARGB components
         * @param hasAlpha {@code true} if the alpha bits are valid;
         * {@code false} otherwise
         * @see java.awt.image.ColorModel#getRGBdefault
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getAlpha
         * @see #getRGB
         */
        public Colour(int argb, boolean hasAlpha) {
            super(argb, hasAlpha);
        }

        /**
         * Creates an opaque sRGB colour with the specified combined RGB value
         * consisting of the red component in bits 16-23, the green component in
         * bits 8-15, and the blue component in bits 0-7. The actual colour used
         * in rendering depends on finding the best match given the colour space
         * available for a particular output device. Alpha is defaulted to 255.
         *
         * @param rgb the combined RGB components
         * @see java.awt.image.ColorModel#getRGBdefault
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getRGB
         */
        public Colour(int rgb) {
            super(rgb);
        }

        /**
         * Creates an sRGB colour with the specified red, green, blue, and alpha
         * values in the range (0 - 255).
         *
         * @throws IllegalArgumentException if {@code r}, {@code g}, {@code b}
         * or {@code a} are outside of the range 0 to 255, inclusive
         * @param r the red component
         * @param g the green component
         * @param b the blue component
         * @param a the alpha component
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getAlpha
         * @see #getRGB
         */
        public Colour(int r, int g, int b, int a) {
            super(r, g, b, a);
        }

        /**
         * Creates an sRGB colour with the specified red, green, blue, and alpha
         * values in the range (0 - 255).
         *
         * @throws IllegalArgumentException if {@code r}, {@code g}, {@code b}
         * or {@code a} are outside of the range 0 to 255, inclusive
         * @param r the red component
         * @param g the green component
         * @param b the blue component
         * @see #getRed
         * @see #getGreen
         * @see #getBlue
         * @see #getAlpha
         * @see #getRGB
         */
        public Colour(int r, int g, int b) {
            super(r, g, b);
        }

        /**
         * Creates a colour from a JavaScript {@code Number} object. This is
         * equivalent to {@code Colour(int rgb)} for script code.
         *
         * @param argb the combined RGB components
         */
        public Colour(double argb) {
            super((int) ((long) argb));
        }

        /**
         * Creates a colour from a JavaScript {@code Number} object. This is
         * equivalent to {@code Colour(int argb, boolean translucent)} for
         * script code.
         *
         * @param argb the combined RGB components
         * @param hasAlpha {@code true} if the alpha bits are valid;
         * {@code false} otherwise
         */
        public Colour(double argb, boolean hasAlpha) {
            super((int) ((long) argb), hasAlpha);
        }

        /**
         * Given a standard {@link Color} object, converts it to a {@code Colour}.
         * 
         * @param c the color to convert
         * @return the original object if it is already a {@code Colour}, otherwise
         * an equivalent {@code Colour}; returns null if passed null
         */        
        public static Colour from(Color c) {
            return (c instanceof Colour) ? (Colour) c : (c == null ? null : new Colour(c.getRGB(), true));
        }

        /**
         * Mixes this colour with an equal amount of another colour.
         * 
         * @param with the colour to mix this colour with
         * @return the mixed colour
         * @see #mix(java.awt.Color, float)
         */
        public Colour mix(Color with) {
            return mix(with, 0.5f);
        }
        
        /**
         * Mixes this colour with another colour.
         * The colours are mixed in a linear RGB (not sRGB) space for 
         * results that more closely match human perception.
         * Only the colours are mixed; the new colour will retain the alpha
         * value of this colour.
         * 
         * @param with the colour to mix this colour with
         * @param thisAmount the proportion of this colour to include, from 0f-1f
         * @return the mixed colour
         */
        public Colour mix(Color with, float thisAmount) {
            if (thisAmount >= 1f) {
                return this;
            }
            if (thisAmount <= 0f) {
                return from(with);
            }
            
            int rgb1 = getRGB(), rgb2 = with.getRGB();

            if (rgb1 == rgb2) {
                return this;
            }
            
            final float thatAmount = 1f - thisAmount;
            
            float r1 = ((rgb1 & 0xff0000) >> 16) / 255f;
            float r2 = ((rgb2 & 0xff0000) >> 16) / 255f;
            r1 = (float) Math.sqrt((r1*r1*thisAmount) + (r2*r2*thatAmount));
            
            float g1 = ((rgb1 & 0x00ff00) >> 8) / 255f;
            float g2 = ((rgb2 & 0x00ff00) >> 8) / 255f;
            g1 = (float) Math.sqrt((g1*g1*thisAmount) + (g2*g2*thatAmount));
            
            float b1 = (rgb1 & 0x0000ff) / 255f;
            float b2 = (rgb2 & 0x0000ff) / 255f;
            b1 = (float) Math.sqrt((b1*b1*thisAmount) + (b2*b2*thatAmount));
            
            return new Colour(r1, g1, b1, getAlpha()/255f);
        }
        
        /**
         * Returns a version of this colour with a different alpha value.
         * 
         * @param newAlpha the new alpha value, from 0-255
         * @return the adjusted colour
         */
        public Colour derive(int newAlpha) {
            newAlpha = Math.max(0, Math.min(255, newAlpha));
            return new Colour((getRGB() & 0xffffff) | (newAlpha << 24), true);
        }
        
        /**
         * Returns a derived colour that shifts the hue and scales the
         * brightness and saturation of this colour.
         * The result will have the same alpha value as this colour.
         * 
         * @param hueShift the angle to shift the hue by, in rotations
         * @param satFactor the factor to apply to the saturation
         * @param briFactor the factor to apply to the brightness
         * @return the derived colour
         */
        public Colour derive(float hueShift, float satFactor, float briFactor) {
            float[] hsb = RGBtoHSB(getRed(), getGreen(), getBlue(), null);
            hsb[0] += hueShift;
            hsb[1] = Math.max(0f, Math.min(1f, hsb[1] * satFactor));
            hsb[2] = Math.max(0f, Math.min(1f, hsb[2] * briFactor));
            return new Colour(HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xffffff | (getAlpha() << 24), true);
        }
        
        /**
         * Returns a string suitable for storing this colour as a setting value.
         *
         * @return a string of either 6 or 8 hexadecimal digits (depending on
         * whether the colour is translucent, with alpha &lt; 255), in RRGGBB or
         * AARRGGBB order
         */
        @Override
        public String toString() {
            int argb = getRGB();
            String v;
            if ((argb & 0xff000000) == 0xff000000) {
                v = String.format("%06x", (argb & 0xffffff));
            } else {
                v = String.format("%08x", argb);
            }
            return v;
        }
    }

    //
    // IMPLEMENTATION
    //
    /**
     * Returns the key with a type suffix applied. If the key already ends in
     * the type suffix, the original key is returned.
     *
     * @param key the key to apply a suffix to, e.g., "body-text"
     * @param suffix the suffix to apply, e.g., "-region"
     * @return the key with the suffix applied, if not already applied
     */
    private static String suffix(String key, String suffix) {
        if (!key.endsWith(suffix)) {
            key += suffix;
        }
        return key;
    }

    /**
     * Regex that splits comma-separated strings into their component parts.
     */
    private static final Pattern LIST_SPLITTER = Pattern.compile("\\s*,\\s*");

    private static final Settings SHARED = new Settings() {
        @Override
        public void set(String key, String value) {
            throw new UnsupportedOperationException("cannot set settings on shared instance");
        }

        @Override
        public void reset(String key) {
            throw new UnsupportedOperationException("cannot reset settings on shared instance");
        }

        @Override
        public Set<String> getKeySet() {
            return RawSettings.getSettingKeySet();
        }

        @Override
        public int size() {
            return getKeySet().size();
        }

        @Override
        public String toString() {
            return "Settings (Shared)";
        }

        @Override
        public Settings getParent() {
            return null;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new UnsupportedOperationException("cannot serialize shared settings");
        }

        @Override
        public void addSettingsFrom(Map<String, String> settingMap) {
            for (Entry<String, String> en : settingMap.entrySet()) {
                RawSettings.setGlobalSetting(en.getKey(), en.getValue());
            }
        }

        @Override
        public void addSettingsFrom(Settings sourceSettings) {
            for (String key : sourceSettings.getKeySet()) {
                RawSettings.setGlobalSetting(key, sourceSettings.get(key));
            }
        }

        @Override
        public void addSettingsFrom(String resource) {
            RawSettings.loadGlobalSettings(resource);
        }
    };

    private static final Settings USER = new Settings() {
        @Override
        public void set(String key, String value) {
            RawSettings.setUserSetting(key, value);
        }

        @Override
        public void reset(String key) {
            RawSettings.removeUserSetting(key);
        }

        @Override
        public Set<String> getKeySet() {
            return RawSettings.getUserSettingKeySet();
        }

        @Override
        public int size() {
            return getKeySet().size();
        }

        @Override
        public String toString() {
            return "Settings (User)";
        }

        @Override
        public Settings getParent() {
            return getShared();
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new UnsupportedOperationException();
        }
    };

    @SuppressWarnings("serial")
    private static class SettingsNamespace extends Settings {

        private String space;

        public SettingsNamespace(String namespace, Settings parent) {
            super(parent);
            space = namespace;
            if (!space.endsWith(":")) {
                space += ":";
            }
        }

        @Override
        public String get(String key) {
            return getParent().get(space + key);
        }

        @Override
        public Set<String> getKeySet() {
            HashSet<String> plucked = new HashSet<>();
            for (String s : getParent().getKeySet()) {
                if (s.startsWith(space)) {
                    plucked.add(s.substring(space.length()));
                }
            }
            return plucked;
        }

        @Override
        public void reset(String key) {
            getParent().reset(space + key);
        }

        @Override
        public void set(String key, String value) {
            getParent().set(space + key, value);
        }

        @Override
        public int size() {
            return getKeySet().size();
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new UnsupportedOperationException("cannot serialize namespace; serialize parent instead");
        }

        @Override
        public String toString() {
            return getParent().toString() + " with namespace \"" + space + "\"";
        }
    }

    private static class PropertySettings extends Settings {

        private static final long serialVersionUID = -1_111_158_290_458_777_391L;
        private Properties p;
        private String name;

        public PropertySettings(String name, Properties p) {
            this.p = p;
            this.name = name;
        }

        @Override
        public String get(String key) {
            String v = p.getProperty(key);
            if (v == null) {
                Settings parent = getParent();
                if (parent != null) {
                    v = parent.get(key);
                }
            }
            return v;
        }

        @Override
        public String getOverride(String key) {
            return p.getProperty(key);
        }

        @Override
        public void set(String key, String value) {
            p.setProperty(key, value);
        }

        @Override
        public void reset(String key) {
            p.remove(key);
        }

        @Override
        public Set<String> getKeySet() {
            return p.stringPropertyNames();
        }

        @Override
        public int size() {
            return getKeySet().size();
        }

        @Override
        public Settings getParent() {
            return null;
        }

        private static final int VERSION = 1;

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeInt(VERSION);
            out.writeObject(name);
            out.writeObject(p);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            int version = in.readInt();
            name = (String) in.readObject();
            p = (Properties) in.readObject();
        }

        @Override
        public String toString() {
            if (name == null) {
                return "Settings (anonymous wrapper)";
            } else {
                return "Settings (" + name + ')';
            }
        }
    }

    /**
     * Thrown by methods that attempt to convert setting values to objects when
     * the conversion fails due to an error in the format of the value.
     */
    public static class ParseError extends RuntimeException {

        private static final long serialVersionUID = -1_034_856_754_864_576_514L;

        ParseError(String missingKey) {
            super(string("rk-err-missing-key", missingKey));
        }

        ParseError(String errorMsgKey, String keyName, String badValue) {
            this(errorMsgKey, keyName, badValue, null);
        }

        ParseError(String errorMsgKey, String keyName, String badValue, Throwable cause) {
            super(Language.string(errorMsgKey, keyName == null ? "n/a" : keyName, badValue), cause);

            try {
                if (keyName != null) {
                    StrangeEons.log.log(Level.WARNING, "Setting parser exception", this);
                    ScriptMonkey.scriptError(this);
                }
            } catch (Throwable t) {
                StrangeEons.log.log(Level.SEVERE, null, t);
            }
        }
    }

    protected boolean isSerializedParent() {
        return true;
    }
}
