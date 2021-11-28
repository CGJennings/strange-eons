package ca.cgjennings.apps.arkham.plugins.engine;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.layout.PageShape;
import ca.cgjennings.layout.TextStyle;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.script.Bindings;
import resources.Language;
import resources.RawSettings;
import resources.Settings;
import resources.Settings.Colour;

/**
 * A script bindings implementation that allows access to Settings using
 * variable names that start with $, UI strings using names that start with
 * {@literal @}, and game strings using names that start with #. It also defines
 * the "magic" variables <b>Editor</b> and <b>Component</b>.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class SettingBindings implements Bindings {

    private Bindings parent;
    private volatile Settings settings;
    private volatile Language uiLang = Language.getInterface();
    private volatile Language gameLang = Language.getGame();
    private Map<String, MagicVariable> magicVariables = new HashMap<>(8);

    private static final int BINDING_VARIABLE = 0;
    private static final int BINDING_SETTING = 1;
    private static final int BINDING_STRING = 2;
    private static final int BINDING_GSTRING = 3;

    public SettingBindings(Bindings parent) {
        this(parent, null);
    }

    public SettingBindings(Bindings parent, Settings settings) {
        if (parent == null) {
            throw new NullPointerException("null parent");
        }
        this.parent = parent;
        if (settings == null) {
            settings = Settings.getShared();
        }
        this.settings = settings;

        put(ScriptMonkey.VAR_APPLICATION, StrangeEons.getApplication());
        defineMagicVariable(ScriptMonkey.VAR_EDITOR, MAGIC_EDITOR);
        defineMagicVariable(ScriptMonkey.VAR_COMPONENT, MAGIC_COMPONENT);
    }

    private int type(String name) {
        if (name != null && name.length() > 1) {
            final char c = name.charAt(0);
            if (c == '$') {
                return BINDING_SETTING;
            }
            if (c == '@') {
                return BINDING_STRING;
            }
            if (c == '#') {
                return BINDING_GSTRING;
            }
        }
        return BINDING_VARIABLE;
    }

    private static String varToKey(String name) {
        return name.substring(1).replace('_', '-');
    }

//	private String keyToVar( String name ) {
//		return "$" + name.replace( '-', '_' );
//	}
    @Override
    @SuppressWarnings("fallthrough")
    public Object put(String name, Object value) {
        switch (type(name)) {
            case BINDING_SETTING:
                name = varToKey(name);
                // starts with $$
                if (name.length() > 1 && name.charAt(0) == '$') {
                    throwException("Live settings ($$ variables) cannot be modified: " + name);
                }
                if (value instanceof NativeJavaObject) {
                    value = ((NativeJavaObject) value).unwrap();
                }

                String strVal = value == null ? null : value.toString();
                if (getSettings() == Settings.getShared()) {
                    RawSettings.setGlobalSetting(name, strVal);
                } else {
                    getSettings().set(name, strVal);
                }
                // violation of Map spec:
                return strVal;
            case BINDING_STRING:
            case BINDING_GSTRING:
                throwException("String keys (@ and # variables) cannot be modified: " + name);
            default:
                return parent.put(name, value);
        }
    }

    /**
     * Throws an exception in the running script.
     *
     * @param label error message
     */
    private static void throwException(String label) {
        UnsupportedOperationException t = new UnsupportedOperationException(label);
        t.fillInStackTrace();
        if (Context.getCurrentContext() != null) {
            Context.throwAsScriptRuntimeEx(t);
        } else {
            throw t;
        }
    }

    @Override
    public Object get(Object key) {
        String name = key.toString();

        switch (type(name)) {
            case BINDING_SETTING:
                name = varToKey(name);
                // starts with $$
                if (name.length() > 1 && name.charAt(0) == '$') {
                    return createLiveSetting(name.substring(1));
                } else {
                    return settings.get(name);
                }
            case BINDING_STRING:
                name = varToKey(name);
                return uiLang.get(name);
            case BINDING_GSTRING:
                name = varToKey(name);
                return gameLang.get(name);
            default:
                Object v = parent.get(key);
                if (v == null && !parent.containsKey(key)) {
                    MagicVariable mv = magicVariables.get(key);
                    if (mv != null) {
                        v = mv.unwrap();
                    }
                }
                return v;
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (String key : toMerge.keySet()) {
            put(key, toMerge.get(key));
        }
    }

    @Override
    public boolean containsKey(Object key) {
        String name = key.toString();
        switch (type(name)) {
            case BINDING_SETTING:
                if (WARN_UNDECLARED_SETTING) {
                    name = varToKey(name);
                    // take second $ off live setting
                    if (name.length() > 1 && name.charAt(0) == '$') {
                        name = name.substring(1);
                    }
                    return settings.get(name) != null;
                }
                // returning true avoids spurious warnings about undeclared
                // variables in DIY scripts
                return true;
            case BINDING_STRING:
                return uiLang.isKeyDefined(varToKey(name));
            case BINDING_GSTRING:
                return gameLang.isKeyDefined(varToKey(name));
            default:
                boolean v = parent.containsKey(key);
                if (v == false) {
                    v = magicVariables.containsKey(key);
                }
                return v;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        return parent.containsValue(value);
    }

    @Override
    public Object remove(Object key) {
        String name = key.toString();
        switch (type(name)) {
            case BINDING_SETTING:
                name = varToKey(name);
                String old = settings.get(name);
                settings.reset(name);
                return old;
            case BINDING_STRING:
            case BINDING_GSTRING:
                return get(key);
            default:
                return parent.remove(key);
        }
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public boolean isEmpty() {
        // there are always some settings keys
        return parent.isEmpty();
    }

    @Override
    public void clear() {
        parent.clear();
    }

    @Override
    public Set<String> keySet() {
        return parent.keySet();
    }

    @Override
    public Collection<Object> values() {
        return parent.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return parent.entrySet();
    }

    /**
     * Returns the settings used to look up $ variables.
     *
     * @return the $ settings instance
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * Sets the settings used to look up $ and $$ variables.
     *
     * @param settings the $ settings instance
     */
    public void setSettings(Settings settings) {
        if (settings != this.settings) {
            this.settings = settings;
            cachedLS = null;
        }
    }

    /**
     * Returns the {@link Language} used to look up @ variables.
     *
     * @return the UI language instance
     */
    public Language getUILanguage() {
        return uiLang;
    }

    /**
     * Returns the {@link Language} used to look up @ variables.
     *
     * @param ui the UI language instance
     */
    public void setUILanguage(Language ui) {
        if (ui == null) {
            ui = Language.getInterface();
        }
        uiLang = ui;
    }

    /**
     * Returns the {@link Language} used to look up # variables.
     *
     * @return the game language instance
     */
    public Language getGameLanguage() {
        return gameLang;
    }

    /**
     * Returns the {@link Language} used to look up # variables.
     *
     * @param game the game language instance
     */
    public void setGameLanguage(Language game) {
        if (game == null) {
            game = Language.getGame();
        }
        gameLang = game;
    }

    /**
     * A magic variable is a function for producing the value of a variable. For
     * example, the {@code Editor} magic variable is a function that
     * returns the active editor (or {@code null}). Magic variables are
     * stored in a secondary map that is consulted after the primary map fails.
     * (If the name of a magic variable is assigned a normal value, that value
     * will thus supercede the magic variable.)
     */
    private interface MagicVariable {

        public Object unwrap();
    }

    /**
     * Defines a magic variable.
     *
     * @param name the magic variable name
     * @param variable the magic variable; when the variable name is looked up,
     * its value will be returned as {@code variable.unwrap()}.
     */
    private void defineMagicVariable(String name, MagicVariable variable) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (variable == null) {
            throw new NullPointerException("variable");
        }
        magicVariables.put(name, variable);
    }

    /**
     * A live setting is returned when the script uses $$-notation&mdash;two
     * dollar signs instead of one&mdash;when reading a setting value in a
     * script. Live settings include getters for standard data types. A live
     * setting can also be stored and reused; it will always reflect the current
     * value of the encapsulated setting.
     */
    public final class LiveSetting {

        private final String key;

        private LiveSetting(String key) {
            this.key = key;
        }

        /**
         * Returns the {@link Settings} instance that the live setting was
         * created from.
         *
         * @return the settings instance that the setting value is looked up in
         */
        public Settings getSettings() {
            return settings;
        }

        /**
         * Returns the key name of the setting.
         *
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns the current value of the setting.
         *
         * @return the current setting value
         * @see Settings#get
         */
        public String getValue() {
            return settings.get(key);
        }

        /**
         * Returns the current value of the setting as a string. This is
         * equivalent to {@link #getValue()}.
         *
         * @return the current setting value
         * @see Settings#get
         */
        public String getString() {
            return settings.get(key);
        }

        /**
         * Returns the current value of the setting as a
         * {@link resources.Settings.Colour Colour}. This is equivalent to
         * {@link #getColor()}.
         *
         * @return the current setting value as a colour
         * @see Settings#getColour
         */
        public Colour getColour() {
            return Settings.colour(getValue());
        }

        /**
         * Returns the current value of the setting as a
         * {@link resources.Settings.Colour Colour}. This is equivalent to
         * {@link #getColour()}.
         *
         * @return the current setting value as a colour
         * @see Settings#getColour
         */
        public Colour getColor() {
            return Settings.colour(getValue());
        }

        /**
         * Returns the current value of the setting as a
         * {@link ca.cgjennings.layout.PageShape.CupShape CupShape}.
         *
         * @return the current setting value as a cup shape
         * @see Settings#getCupShape
         */
        public PageShape.CupShape getCupShape() {
            return Settings.cupShape(getValue());
        }

        /**
         * Returns the current value of the setting as an integer.
         *
         * @return the current setting value as an integer
         * @see Settings#getInt
         */
        public int getInteger() {
            return Settings.integer(getValue());
        }

        /**
         * Returns the current value of the setting as a double value.
         *
         * @return the current setting value as a double
         * @see Settings#getDouble
         */
        public double getNumber() {
            return Settings.number(getValue());
        }

        /**
         * Returns the current value of the setting as an integer region.
         *
         * @return the current setting value as a region
         * @see Settings#getRegion
         */
        public Rectangle getRegion() {
            return Settings.region(getValue());
        }

        /**
         * Returns the current value of the setting as a
         * {@link resources.Settings.Region2D Region2D}, a region with floating
         * point precision.
         *
         * @return the current setting value as a region
         * @see Settings#getRegion2D
         */
        public Rectangle2D.Double getRegion2D() {
            return Settings.region2D(getValue());
        }

        /**
         * Returns the current value of the setting as an tint value stored as
         * an array of three floating point values in hue, saturation,
         * brightness order.
         *
         * @return the current setting value as a tint value
         * @see Settings#getTint
         */
        public float[] getTint() {
            return Settings.tint(getValue());
        }

        /**
         * Returns the current value of the setting as a boolean value. This is
         * equivalent to {@link #getYesNo()}.
         *
         * @return the current setting value as a boolean
         * @see Settings#getBoolean
         */
        public boolean getBoolean() {
            return Settings.yesNo(getValue());
        }

        /**
         * Returns the current value of the setting as a boolean value. This is
         * equivalent to {@link #getBoolean()}.
         *
         * @return the current setting value as a boolean
         * @see Settings#getYesNo
         */
        public boolean getYesNo() {
            return Settings.yesNo(getValue());
        }

//		public PageShape getShape() {
//			return Settings.cupShape( getValue() );
//		}
        /**
         * Returns the current value of the setting as a text style.
         *
         * @return the current setting value as a text style
         * @see Settings#getTextStyle
         */
        public TextStyle getTextStyle() {
            return Settings.textStyle(getValue(), null);
        }

        /**
         * Returns the current value of the setting as a text alignment value.
         *
         * @return the current setting value as a text alignment
         * @see Settings#getTextAlignment
         */
        public int getTextAlignment() {
            return Settings.textAlignment(getValue());
        }

        /**
         * Returns a string representation of this live setting.
         *
         * @return a string describing the setting and its current value
         */
        @Override
        public String toString() {
            return String.format("Live setting for %s => %s (instance %x)",
                    key, getValue(), System.identityHashCode(this)
            );
        }
    }

    private LiveSetting createLiveSetting(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        synchronized (this) {
            if (cachedLS == null || !cachedLS.key.equals(key)) {
                cachedLS = new LiveSetting(key);
            }
            return cachedLS;
        }
    }
    private LiveSetting cachedLS;

    private static final MagicVariable MAGIC_COMPONENT = StrangeEons::getActiveGameComponent;

    private static final MagicVariable MAGIC_EDITOR = StrangeEons::getActiveEditor;

    private static volatile boolean WARN_UNDECLARED_SETTING = false;

    /**
     * Sets whether the bindings will report $-notation variables that have a
     * {@code null} value are undefined.
     *
     * @param enable if {@code true}, {@code null} setting variables
     * will not be "contained" by the map, resulting in additional warnings when
     * script warnings are enabled
     * @see #containsKey(java.lang.Object)
     */
    public static void setUndeclaredSettingWarningsEnabled(boolean enable) {
        WARN_UNDECLARED_SETTING = enable;
    }
}
