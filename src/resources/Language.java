package resources;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.prefs.Preferences;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.i18n.IntegerPluralizer;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.theme.ThemeInstaller;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * A language object manages language-related resources for a particular locale.
 * This includes a table of strings (that can be expanded using resource
 * bundles), a pluralizer, and a string a collator. Other resources can be
 * located using localized strings in the string table, or searching for
 * matching resource files using {@link #getResourceChain}. Static methods
 * {@link #string} and {@link #gstring} provide quick access to strings defined
 * in the default user interface and game languages, respectively.
 *
 * <p>
 * More than one language instance can be defined for a locale, and they can
 * either be kept isolated from each other or linked together in an inheritance
 * chain. When localizing a plug-in, you can either add your strings to the
 * default interface and/or game languages (in which case you must avoid
 * conflicting key names) or create your own. The best option is to create your
 * own and set the default instance as their parents, although this takes a
 * little more work.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Language implements Iterable<String> {

    private Locale loc;

    private HashMap<String, String> strings = new HashMap<>(2500);
    private List<String> bundles = new LinkedList<>();
    private HashMap<String, String> definedStrings = new HashMap<>(50);

    private IntegerPluralizer plur;
    private Formatter formatter;
    private StringBuilder formatBuffer;
    private Collator collator;

    private static Language uiLang;
    private static Language gameLang;

    private Language parent;

    /**
     * Create an empty language resource for a locale. If {@code loc} is
     * {@code null}, the current UI locale is used.
     *
     * @param loc the locale that this language instance is based on
     * @see #setLocale(java.util.Locale)
     */
    public Language(Locale loc) {
        setLocale(loc);
    }

    /**
     * Create an empty language resource for a locale described by
     * a locale description, such as {@code "en_CA"}.
     */
    public Language(String loc) {
        this(parseLocaleDescription(loc));
    }

    /**
     * Sets this language's parent. If a string cannot be found in this
     * language's string table, and a non-{@code null} parent is defined,
     * then the chain of parents will be searched to locate a definition.
     *
     * @param parent the new parent to set; if {@code null} the existing
     * parent (if any) is removed
     * @throws IllegalArgumentException if setting the parent would create a
     * cycle (a language is directly or indirectly its own ancestor)
     */
    public synchronized void setParent(final Language parent) {
        Language p = parent;
        while (p != null) {
            if (p == this) {
                throw new IllegalArgumentException("cycle in parent-child graph");
            }
            p = p.getParent();
        }
        this.parent = parent;
    }

    /**
     * Returns this language's parent, or {@code null} if it has no parent.
     * The language's parent is consulted if this language does not define a
     * string.
     *
     * @return the parent instance, or {@code null}
     */
    public synchronized Language getParent() {
        return parent;
    }

    /**
     * Changes the locale used by this language instance. If the supplied locale
     * is {@code null}, the default locale is used. The supplied locale
     * must include a language and may optionally include a region (country).
     *
     * <p>
     * Variants are not supported. If the supplied locale includes a variant,
     * the variant part of the locale will be dropped silently. (In this case,
     * {@link #getLocale()} will return a value that is not equal to the locale
     * passed to the method.)
     *
     * @param loc the new locale to use
     */
    public void setLocale(Locale loc) {
        if (loc == null) {
            loc = Locale.getDefault();
            if (loc.getLanguage().isEmpty()) {
                StrangeEons.log.warning("default locale has no language: assuming English");
                loc = new Locale("en", loc.getCountry(), "");
            }
        }
        if (loc.equals(this.loc)) {
            return;
        }

        if (loc.getLanguage().isEmpty()) {
            throw new IllegalArgumentException("locale has empty language");
        }
        if (!loc.getVariant().isEmpty()) {
            loc = new Locale(loc.getLanguage(), loc.getCountry(), "");
        }

        synchronized (this) {
            this.loc = loc;

            plur = IntegerPluralizer.create(loc);
            plur.setLanguage(this);
            formatter = new Formatter(loc);
            formatBuffer = (StringBuilder) formatter.out();
            collator = Collator.getInstance(loc);

            strings.clear();
            List<String> bundlesToReload = bundles;
            bundles = new LinkedList<>();
            for (String s : bundlesToReload) {
                addStrings(s);
            }
        }
    }

    /**
     * Returns the locale used by this language instance.
     *
     * @return the language locale
     */
    public synchronized Locale getLocale() {
        return loc;
    }

    /**
     * Adds strings from a new set of resource files to this language. The
     * resource files to be loaded will be determined as if by calling
     * {@link #getResourceChain(java.lang.String, java.lang.String)}. If the
     * base resource name includes an extension (a suffix starting with a '.'
     * character), that extension will be used to locate the resource files. If
     * the base resource name does not include an extension, the extension
     * {@code .properties} will be used.
     *
     * <p>
     * To prevent the accidental replacement of strings from the main
     * application, this method will not alter the value of string keys that are
     * already defined. If you wish to replace the value of an existing key, you
     * can do so using the {@link #set(java.lang.String, java.lang.String)}
     * method.
     *
     * @param baseResource the resource path to the base file of a set of string
     * table files
     */
    public synchronized void addStrings(String baseResource) {
        if (baseResource == null) {
            throw new NullPointerException("baseResource");
        }

        String extension = ".properties";
        int dot = baseResource.lastIndexOf('.');
        if (dot >= 0) {
            extension = baseResource.substring(dot);
            baseResource = baseResource.substring(0, dot);
        }

        boolean allowOverwrite = baseResource.startsWith("project:");

        // ensure not already added
        if (!allowOverwrite) {
            for (String file : bundles) {
                if (file.equals(baseResource)) {
                    return;
                }
            }
        }

        String[] files = getResourceChain(baseResource, extension);
        if (files.length == 0) {
            throw new IllegalArgumentException("no such resource: " + baseResource + extension);
        }

        Properties p = new Properties();
        for (String file : files) {
            InputStream in = null;
            try {
                in = ResourceKit.composeResourceURL(file).openStream();
                p.load(in);
            } catch (IOException e) {
                StrangeEons.log.log(Level.WARNING, "failed to load string bundle: " + file, e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    StrangeEons.log.log(Level.WARNING, file, e);
                }
            }
        }
        for (String key : p.stringPropertyNames()) {
            if (allowOverwrite || !strings.containsKey(key)) {
                strings.put(key, p.getProperty(key));
            }
        }
        bundles.add(baseResource);
    }

    /**
     * Returns a string defined in this language instance.
     *
     * @param key the key for the string to retrieve
     * @return the localized string, or [MISSING: key]
     */
    public String get(String key) {
        String v = getImpl(key);
        if (v == null) {
            v = "[MISSING: " + key + "]";
        }
        return v;
    }

    /**
     * A synonym for {@link #get(java.lang.String)} that can be called from
     * scripts without specifying an overload.
     *
     * @param key the key for the string to retrieve
     * @return the localized string, or [MISSING: key]
     */
    public String str(String key) {
        return get(key);
    }

    private synchronized String getImpl(String key) {
        String v = definedStrings.get(key);
        if (v == null) {
            v = strings.get(key);
            if (v == null) {
                if (getParent() != null) {
                    v = getParent().getImpl(key);
                }
            }
        }
        return v;
    }

    /**
     * Returns a formatted string defined in this language. Formatting flags in
     * the localized string will be filled in using the supplied arguments. The
     * formatting flags accepted by the method are exactly the same as those
     * supported by {@code String.format} (similar to {@code sprintf}
     * in C).
     *
     * @param key the key for the localized formatting template
     * @param args the arguments to use to fill in the formatting template
     * @return the localized, formatted string or [MISSING: key]
     */
    public synchronized String get(String key, Object... args) {
        String v = getImpl(key);
        if (v == null) {
            return "[MISSING: " + key + "]";
        }
        if (args.length == 0) {
            return v;
        }

        formatBuffer.delete(0, formatBuffer.length());
        return formatter.format(getLocale(), v, args).toString();
    }

    /**
     * Returns {@code true} if the specified key if defined.
     *
     * @param key the name of the key to test
     * @return {@code true} if the key has a value
     */
    public synchronized boolean isKeyDefined(String key) {
        if (strings.containsKey(key)) {
            return true;
        } else if (definedStrings.containsKey(key)) {
            return true;
        } else if (getParent() != null) {
            return getParent().isKeyDefined(key);
        }
        return false;
    }

    /**
     * Explicitly sets a key to a fixed value, overriding the result that would
     * be obtained from the loaded string tables. If value is {@code null},
     * any value previously set with this method is removed.
     *
     * @param key the key to affect
     * @param value the new string value
     * @throws NullPointerException if the key is {@code null}
     */
    public synchronized void set(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            definedStrings.remove(key);
        } else {
            definedStrings.put(key, value);
        }
    }

    /**
     * Returns a copy of all of the keys defined in this language (not including
     * any parents).
     *
     * @return a set of this language's keys
     */
    public synchronized Set<String> keySet() {
        HashSet<String> keys = new HashSet<>(definedStrings.size() + strings.size());
        for (String k : definedStrings.keySet()) {
            keys.add(k);
        }
        for (String k : strings.keySet()) {
            keys.add(k);
        }
        return keys;
    }

    /**
     * Returns an iterator that iterates over the keys in this language (not
     * including any parents).
     *
     * @return an iterator over this language's keys
     */
    @Override
    public Iterator<String> iterator() {
        return keySet().iterator();
    }

    /**
     * Returns a {@code Collator} that can be used to sort strings in this
     * language.
     *
     * @return a {@code Collator} instance
     */
    public Collator getCollator() {
        return collator;
    }

    /**
     * Returns a pluralizer for strings in the this language.
     *
     * @return a formatter for descriptions of integer quantities
     */
    public IntegerPluralizer getPluralizer() {
        return plur;
    }

    /**
     * Returns the shared Language instance that represents the UI language.
     *
     * @return the UI language instance
     */
    public synchronized static Language getInterface() {
        return uiLang;
    }

    /**
     * Returns the shared Language instance that represents the game language.
     *
     * @return the game language instance
     */
    public synchronized static Language getGame() {
        return gameLang;
    }

    /**
     * Sets the locale used for the shared interface language.
     *
     * @param ui the locale to use
     */
    public synchronized static void setInterfaceLocale(Locale ui) {
        if (ui == null) {
            ui = Locale.getDefault();
        }
        Locale.setDefault(ui);

        if (uiLang == null) {
            uiLang = new Language(ui);
            uiLang.addStrings("text/interface/eons-text");
        } else {
            uiLang.setLocale(ui);
        }
    }

    /**
     * Returns the user interface locale.
     *
     * @return the interface locale
     */
    public synchronized static Locale getInterfaceLocale() {
        if (uiLang == null) {
            StrangeEons.log.warning("getInterfaceLocale with no locale set");
        }
        return uiLang == null ? null : uiLang.loc;
    }

    /**
     * Sets the locale used for the shared game language.
     *
     * @param game the locale to use
     */
    public synchronized static void setGameLocale(Locale game) {
        if (game == null) {
            game = Locale.getDefault();
        }

        if (gameLang == null) {
            gameLang = new Language(game);
        } else {
            gameLang.setLocale(game);
        }
    }

    /**
     * Returns the game locale.
     *
     * @return the game locale
     */
    public synchronized static Locale getGameLocale() {
        return gameLang == null ? null : gameLang.loc;
    }

    /**
     * Returns a string looked up with the default interface language. Cover for
     * {@code Language.getInterface().get( key )}.
     *
     * @param key the key for the string to retrieve
     * @return the localized string, or [MISSING: key]
     */
    public static String string(String key) {
        GUIBuilderCheck();
        return getInterface().get(key);
    }

    /**
     * Formats and returns a format string looked up with the default interface
     * language. Formatting flags in the localized string will be filled in
     * using the supplied args.
     *
     * @param key the key for the localized formatting template
     * @param args the arguments to use to fill in the formatting template
     * @return the localized, formatted string or [MISSING: key]
     */
    public static String string(String key, Object... args) {
        GUIBuilderCheck();
        return getInterface().get(key, args);
    }

    /**
     * {@link #string} must call this to ensure some language is installed so
     * that calls from components in the GUI builder will work; otherwise
     * editors with components like PortraitPanel can't be loaded.
     */
    private static void GUIBuilderCheck() {
        if (uiLang == null) {
            setInterfaceLocale(null);
        }
    }

    /**
     * Returns a string looked up with the default game language. Cover for
     * {@code Language.getInterface().get( key )}.
     *
     * @param key the key for the string to retrieve
     * @return the localized string, or [MISSING: key]
     */
    public static String gstring(String key) {
        return getGame().get(key);
    }

    /**
     * Formats and returns a format string looked up with the default game
     * language. Formatting flags in the localized string will be filled in
     * using the supplied {@code args}.
     *
     * @param key the key for the localized formatting template
     * @param args the arguments to use to fill in the formatting template
     * @return the localized, formatted string or [MISSING: key]
     */
    public static String gstring(String key, Object... args) {
        return getGame().get(key, args);
    }

    /**
     * Parses a locale description in ll_CC format and returns the
     * {@code Locale}.
     *
     * @param code a locale described with a two-letter language and optional
     * two-letter country
     * @return the parsed locale
     */
    public static Locale parseLocaleDescription(String code) {
        if (code == null) {
            return null;
        }

        String language, country;

        int i = code.indexOf('_');
        if (i == -1 ) {
            i = code.indexOf('-');
        }
        if (i == -1) {
            language = code;
            country = null;
        } else {
            language = code.substring(0, i).toLowerCase();
            country = code.substring(i + 1, code.length()).toUpperCase();
            if (country.length() > 2) {
                country = country.substring(0, 2);
            }
        }

        if (language.length() > 2) {
            language = language.substring(0, 2);
        }

        if (country == null) {
            return new Locale(language);
        }
        return new Locale(language, country);
    }

    /**
     * Returns {@code true} if a locale description represents a valid
     * locale. In order to be valid, a locale description must have one of the
     * following forms, where x indicates a lower case letter and X indicates an
     * upper case letter:<br>
     * xx<br>
     * xx_XX<br>
     * Note that variants (further descriptions of the locale that may follow
     * the country code after an additional underscore) are not supported by
     * Strange Eons at this time.
     *
     * @return {@code true} if the locale description is syntactically
     * valid
     */
    public static boolean isLocaleDescriptionValid(String description) {
        if (description == null) {
            return false;
        }

        final int len = description.length();
        if (len == 2 || len == 5) {
            final char l1 = description.charAt(0);
            final char l2 = description.charAt(1);
            if ((l1 >= 'a' && l1 <= 'z') && (l2 >= 'a' && l2 <= 'z')) {
                if (len == 2) {
                    return true;
                } else if (description.charAt(2) == '_') {
                    final char C1 = description.charAt(3);
                    final char C2 = description.charAt(4);
                    return (C1 >= 'A' && C1 <= 'Z') && (C2 >= 'A' && C2 <= 'Z');
                }
            }
        }

        return false;
    }

    /**
     * Returns a sorted array of the available interface locales.
     *
     * @return a non-empty array of supported locales
     */
    public synchronized static Locale[] getInterfaceLocales() {
        return createSortedLocaleList(prefUILocs);
    }

    /**
     * Returns a sorted array of the available game locales.
     *
     * @return a non-empty array of supported locales
     */
    public synchronized static Locale[] getGameLocales() {
        return createSortedLocaleList(prefGameLocs);
    }

    /**
     * Returns a sorted array of all available locales. This includes a large
     * set of known standard locales along with any added interface and game
     * locales not included in this known set.
     *
     * @return a non-empty array of supported locales
     */
    public synchronized static Locale[] getLocales() {
        return createSortedLocaleList(null);
    }

    /**
     * Returns an array of resources that match the requested file name from
     * least to most specialized for the locale's language. This is done by
     * composing the following resource file names, in order:
     * <ol>
     * <li>{@code baseName + suffix}
     * <li>{@code baseName + "_" + locale.getLanguage() + suffix}
     * <li>{@code baseName + "_" + locale.getLanguage() + "_" + locale.getCountry() + suffix}
     * </ol>
     * At each of these steps, if there is a resource available that matches the
     * name, it will be added to the returned array. If not, then the search
     * ends. (If the locale does not specify a country, step 3 will not be
     * performed. If it does not specify a language, then neither step 2 nor
     * step 3 will be performed.) Thus, this method returns an array of between
     * 0 and 3 resource files. For example, the following line would return the
     * main application interface text resources that match the interface
     * locale:<br>
     * {@code Language.getInterface().getResourceChain( "text/interface/eons-text", ".properties" )}
     *
     * @param baseName the path and base file name of the resource
     * @param suffix the suffix to append to the end of the name; typically this
     * is a file extension
     * @return between 0 and 3 resource files that exist and match the locale
     */
    public String[] getResourceChain(String baseName, String suffix) {
        String[] matchList = new String[3];
        int m = 0;

        String name = baseName + suffix;
        if (ResourceKit.composeResourceURL(name) != null) {
            matchList[m++] = name;
            if (!loc.getLanguage().isEmpty()) {
                name = baseName + '_' + loc.getLanguage() + suffix;
                if (ResourceKit.composeResourceURL(name) != null) {
                    matchList[m++] = name;
                    if (!loc.getCountry().isEmpty()) {
                        name = baseName + '_' + loc.getLanguage() + '_' + loc.getCountry() + suffix;
                        if (ResourceKit.composeResourceURL(name) != null) {
                            matchList[m++] = name;
                        }
                    }
                }
            }
        }
        return m == 3 ? matchList : Arrays.copyOf(matchList, m);
    }

    /**
     * Returns a flag icon for the country part of locale {@code loc}. If
     * the locale has no country part, or if no flag is available for the
     * country, a blank placeholder icon will be returned. The placeholder icon
     * is the same size as the flag icon would have been, if one was available.
     * In no event will {@code null} be returned.
     *
     * @param loc the locale to return a flag for
     * @return a small flag icon for the country of the requested locale, or a
     * blank icon
     * @throws NullPointerException if {@code loc} is {@code null}
     */
    public static synchronized Icon getIconForCountry(Locale loc) {
        if (loc == null) {
            throw new NullPointerException("loc");
        }

        final String country = loc.getCountry();
        if (!country.isEmpty()) {
            return new FlagIcon(country);
        }
        return getBlankIcon();
    }

    /**
     * Returns an icon for the language part of locale {@code loc}. If the
     * locale has no language part, or if no icon is available for the language,
     * a blank placeholder icon will be returned. The placeholder icon is the
     * same size as the language icon would have been, if one was available. In
     * no event will {@code null} be returned.
     *
     * @param loc the locale to return a flag for
     * @return a small icon for the language of the requested locale, or a blank
     * icon
     * @throws NullPointerException if {@code loc} is {@code null}
     */
    public static synchronized Icon getIconForLanguage(Locale loc) {
        if (loc == null) {
            throw new NullPointerException("loc");
        }

        Icon icon = null;

        final String langauge = "_" + loc.getLanguage();
        if (langauge.length() > 1) {
            SoftReference<Icon> iconRef = flagMap.get(langauge);
            if (iconRef != null) {
                icon = iconRef.get();
            }
            if (icon == null) {
                String text = loc.getLanguage().toUpperCase(Locale.CANADA);
                Font sans = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
                BufferedImage langImage = makeIconText(text, sans);
                if (langImage.getWidth() > FLAG_SIZE || langImage.getHeight() > FLAG_SIZE) {
                    float ideal = ImageUtilities.idealCoveringScaleForImage(FLAG_SIZE, FLAG_SIZE, langImage.getWidth(), langImage.getHeight());
                    langImage = ImageUtilities.resample(langImage, ideal);
                }
                langImage = ImageUtilities.center(langImage, FLAG_SIZE, FLAG_SIZE);
                icon = new ImageIcon(langImage);
                flagMap.put(langauge, new SoftReference<>(icon));
            }
        }

        if (icon == null) {
            icon = getBlankIcon();
            if (langauge.length() > 1) {
                // missing flag for this country: avoid repeated searches
                flagMap.put(langauge, new SoftReference<>(icon));
            }
        }

        return icon;
    }

    /**
     * Returns an icon that represents the language and country of a locale. If
     * a representation for the language or country is not available, that part
     * of the resulting icon will be blank. In no event will {@code null}
     * be returned.
     *
     * @param loc the locale to return a flag for
     * @return a small icon for the requested locale, or a blank icon
     * @throws NullPointerException if {@code loc} is {@code null}
     */
    public static Icon getIconForLocale(Locale loc) {
        if (loc == null) {
            throw new NullPointerException("loc");
        }
        if (loc.getCountry().isEmpty()) {
            return getIconForLanguage(loc);
        }
        if (loc.getLanguage().isEmpty()) {
            return getIconForCountry(loc);
        }

        Icon icon = null;

        final String locale = "@" + loc.getLanguage() + "@" + loc.getCountry();
        if (locale.length() > 2) {
            SoftReference<Icon> iconRef = flagMap.get(locale);
            if (iconRef != null) {
                icon = iconRef.get();
            }
            if (icon == null) {
                String text = loc.getLanguage().toUpperCase(Locale.CANADA);
                BufferedImage langImage = makeIconText(text, ResourceKit.getTinyFont());
                BufferedImage iconImage = new BufferedImage(FLAG_SIZE, FLAG_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = iconImage.createGraphics();
                try {
                    Icon ci = getIconForCountry(loc);
                    ci.paintIcon(null, g, 0, -FlagIcon.VERTICAL_OFFSET);
                    g.setComposite(AlphaComposite.DstOut);
                    g.drawImage(langImage, FLAG_SIZE - langImage.getWidth() - 1, FLAG_SIZE - langImage.getHeight() - 1, null);
                    g.setComposite(AlphaComposite.SrcOver);
                    g.drawImage(langImage, FLAG_SIZE - langImage.getWidth(), FLAG_SIZE - langImage.getHeight(), null);
                } finally {
                    g.dispose();
                }
                icon = new ImageIcon(iconImage);
                flagMap.put(locale, new SoftReference<>(icon));
            }
        }

        if (icon == null) {
            icon = getBlankIcon();
            if (locale.length() > 2) {
                // missing flag for this country: avoid repeated searches
                flagMap.put(locale, new SoftReference<>(icon));
            }
        }

        return icon;
    }

    private static BufferedImage makeIconText(String text, Font f) {
        final boolean isDark = ThemeInstaller.getInstalledTheme().isDark();
        BufferedImage bi = new BufferedImage(FLAG_SIZE * 2, FLAG_SIZE * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D bounds = fm.getStringBounds(text, g);
            int w = (int) (bounds.getWidth() + 0.5d);
            int h = (int) (bounds.getHeight() + 0.5d);
            g.setColor(isDark ? Color.LIGHT_GRAY : new Color(0x1a2933));
            g.drawString(text, (FLAG_SIZE * 2 - w) / 2, (FLAG_SIZE * 2 - h) / 2);
        } finally {
            g.dispose();
        }
        return ImageUtilities.trim(bi);
    }

    /**
     * Only call from synchronized static methods, or synch on class.
     */
    private static Icon getBlankIcon() {
        if (blankFlag == null) {
            blankFlag = new BlankIcon(FLAG_SIZE, FLAG_SIZE);
        }
        return blankFlag;
    }

    private static HashMap<String, SoftReference<Icon>> flagMap = new LinkedHashMap<>();
    private static Icon blankFlag;
    private static final int FLAG_SIZE = 16;

    /**
     * Marks the locale {@code loc} as a "preferred locale" for interface
     * language purposes. This method is not normally called directly. Instead,
     * plug-in authors will place a {@code ui-languages} client property in
     * their root file with a comma-separated list of all of the interface
     * locales supported by the plug-in. The bundle installer will automatically
     * mark these as preferred when it loads the bundle. Preferred locales are
     * given priority when the user selects languages in the {@link Preferences}
     * dialog.
     *
     * @param loc the locale to prioritize
     * @throws NullPointerException if {@code loc} is {@code null}
     * @throws IllegalArgumentException if there is no language defined in the
     * locale or there is a variant defined
     */
    public synchronized static void addInterfaceLocale(Locale loc) {
        verify(loc);
        if (prefUILocs.add(loc)) {
            if (!loc.getCountry().isEmpty()) {
                prefUILocs.add(new Locale(loc.getLanguage()));
            }
        }
    }

    /**
     * Marks the locale {@code loc} as a "preferred locale" for game
     * language purposes. This method is not normally called directly. Instead,
     * plug-in authors will place a {@code game-languages} client property
     * in their root file with a comma-separated list of all of the game locales
     * supported by the plug-in. The bundle installer will automatically mark
     * these as preferred when it loads the bundle. Preferred locales are given
     * priority when the user selects languages in the {@link Preferences}
     * dialog.
     * <p>
     * <b>Note:</b> Adding a locale that includes a country automatically
     * implies that the base locale with only a language and no country is also
     * preferred.
     *
     * @param loc the locale to prioritize
     * @throws NullPointerException if {@code loc} is {@code null}
     * @throws IllegalArgumentException if there is no language defined in the
     * locale or there is a variant defined
     */
    public synchronized static void addGameLocale(Locale loc) {
        verify(loc);
        if (prefGameLocs.add(loc)) {
            if (!loc.getCountry().isEmpty()) {
                prefGameLocs.add(new Locale(loc.getLanguage()));
            }
        }
    }

    private static final Set<Locale> prefUILocs = Collections.synchronizedSet(new HashSet<Locale>());
    private static final Set<Locale> prefGameLocs = Collections.synchronizedSet(new HashSet<Locale>());

    /**
     * Checks that the locale is not {@code null}, has a language, and does
     * not have a variant. If any of these conditions fails, an appropriate
     * exception is thrown.
     *
     * @param loc
     */
    private static void verify(Locale loc) {
        if (loc == null) {
            throw new NullPointerException("loc");
        }
        if (loc.getLanguage().isEmpty()) {
            throw new IllegalArgumentException("locale does not define a language: " + loc);
        }
        if (!loc.getVariant().isEmpty()) {
            throw new IllegalArgumentException("locales with variants not supported: " + loc);
        }
    }

    /**
     * Creates an array of supported locales. The array is sorted by language
     * name in the interface locale, and then by country within the same
     * language. If the set of locales to include is {@code null}, then all
     * available locales will be included.
     *
     * @param prefer the set of preferred locales to use (either
     * {@code prefUILocs} or {@code prefGameLocs})
     * @param includeAll if {@code true}, include all locales and not just
     * preferred ones
     * @return an array of locales, sorted as described above
     */
    private static Locale[] createSortedLocaleList(Set<Locale> prefer) {
        if (prefer == null) {
            prefer = new HashSet<>();
            add(prefer, Locale.getAvailableLocales());
            add(prefer, Collator.getAvailableLocales());
            add(prefer, SimpleDateFormat.getAvailableLocales());
            add(prefer, getInterfaceLocales());
            add(prefer, getGameLocales());
        }

        Locale[] sorted = prefer.toArray(new Locale[prefer.size()]);

        final Collator col = Language.getInterface().getCollator();
        Arrays.sort(sorted, (Locale o1, Locale o2) -> {
            // if the language is different, sort by that first
            if (!o1.getLanguage().equals(o2.getLanguage())) {
                return col.compare(o1.getDisplayLanguage(), o2.getDisplayLanguage());
            }
            
            // they are the same language; sort by country within language
            // since the display country for no country is blank, the locale
            // for the language alone will be listed first, as required
            return col.compare(o1.getDisplayCountry(), o2.getDisplayCountry());
        });
        return sorted;
    }

    private static void add(Set<Locale> set, Locale[] locales) {
        for (Locale loc : locales) {
            if (!loc.getLanguage().isEmpty() && loc.getVariant().isEmpty()) {
                add(set, loc);
            } else {
                StrangeEons.log.fine("ignoring locale " + loc);
            }
        }
    }

    private static void add(Set<Locale> set, Locale loc) {
        if (!loc.getCountry().isEmpty()) {
            set.add(new Locale(loc.getLanguage()));
        }
        set.add(loc);
    }

    static {
        // register the UI locales for known included UI languages
        String[] locs = new String[]{
            "cs_CZ",
            "de_DE",
            "en_CA", "en_AU", "en_GB", "en_US",
            "es_ES",
            "fr_CA", "fr_FR",
            "it_IT", "pl_PL",
            "ru_RU"
        };
        for (String loc : locs) {
            addInterfaceLocale(parseLocaleDescription(loc));
        }
        // register english as a known locale for games to ensure there is at least one language
        addGameLocale(Locale.ENGLISH);
    }

    /**
     * Returns the string representation of this language, a string describing
     * the language's locale.
     *
     * @return the locale string for the language's locale
     */
    @Override
    public String toString() {
        String locDesc = (loc == null) ? Locale.getDefault().toString() : loc.toString();
        String prefix;
        if (this == uiLang) {
            prefix = "Language (Interface, ";
        } else if (this == gameLang) {
            prefix = "Language (Game, ";
        } else {
            prefix = "Language (";
        }
        return prefix + locDesc + ')';
    }

    /**
     * Splits a file name into tokens to extract locale information. Some kinds
     * of file names, such as string tables, may include a locale as part of the
     * file name. This class splits such file names into three parts: a base
     * name, an extension, and a locale description. A typical example:
     * <pre>
     * occupations_ll_CC.properties
     * \_________/ \___/ \________/
     *  base name  locale extension
     * </pre>
     *
     * <p>
     * The file name is broken into parts as follows:
     * <ol>
     * <li> The extension consists of all text after the first period (.), or
     * {@code null} if there is no period in the name.
     * <li> The base name consists of all text from the start of the string up
     * to the first underscore (_) if any. If there is no underscore, then it
     * continues up until the first period, if any, and otherwise to the end of
     * the string.
     * <li> The locale description consists of the text after the first
     * underscore and up to the first period (.), if any. If there is no
     * underscore, then the locale description is {@code null}. if there is
     * no period, then the description continues to the end of the string. If
     * there is no underscore in the file name, then the locale description is
     * {@code null}. Moreover, the locale is checked to determine if it is
     * valid, as described below. If the locale is not valid, then the locale
     * description will be {@code null} and the part that would have been
     * the locale description, including the initial underscore, will be
     * included in the base name as if there were no underscore in the name.
     * </ol>
     *
     */
    public static final class LocalizedFileName {

        /**
         * Creates a localized file name from the name of the given file.
         *
         * @param file the file name to tokenize
         * @throws NullPointerException if the file is {@code null}
         */
        public LocalizedFileName(File file) {
            this(file.getName());
        }

        /**
         * Creates a localized file name from a file name string.
         *
         * @param fileName the file name to tokenize
         * @throws NullPointerException if the file is {@code null}
         */
        public LocalizedFileName(String fileName) {
            int dot = fileName.indexOf('.');
            if (dot < 0) {
                extension = null;
                dot = fileName.length();
            } else {
                extension = fileName.substring(dot + 1);
            }

            int score = fileName.indexOf('_');
            if (score < 0) {
                baseName = fileName.substring(0, dot);
                localeDescription = null;
            } else {
                String _localeDescription = fileName.substring(score + 1, dot);
                if (isLocaleDescriptionValid(_localeDescription)) {
                    baseName = fileName.substring(0, score);
                    localeDescription = _localeDescription;
                } else {
                    baseName = fileName.substring(0, dot);
                    localeDescription = null;
                }
            }
        }

        private LocalizedFileName(String baseName, String localeDescription, String extension) {
            this.baseName = baseName;
            this.localeDescription = localeDescription;
            this.extension = extension;
        }

        /**
         * The base name that was parsed out of the file name. This may be an
         * empty string, but it will not be {@code null}.
         */
        public final String baseName;
        /**
         * The locale description that was parsed out of the file name, or
         * {@code null} if there was no locale description. A
         * {@code null} locale description indicates that the file is
         * either not part of a family of localized files, or that it represents
         * the default translation. If it is the default translation and other
         * translations are available, other files in the same folder should
         * have the same base name and extension.
         */
        public final String localeDescription;
        /**
         * The file extension of the file (such as {@code .properties}), or
         * {@code null} if there is no file extension.
         */
        public final String extension;

        /**
         * Returns a locale matching the locale description. Returns
         * {@code null} if the locale description is {@code null}.
         *
         * @return the locale for the locale description, or {@code null}
         * @see Language#parseLocaleDescription(java.lang.String)
         */
        public Locale getLocale() {
            return localeDescription == null ? null : parseLocaleDescription(localeDescription);
        }

        /**
         * Returns the file name for the default translation file. For example,
         * if the original file was {@code fruit_fr.properties}, this would
         * return {@code fruit.properties}.
         *
         * @return the file name representing the default translation in a group
         * of localized files
         */
        public String getDefaultFileName() {
            if (extension != null) {
                return baseName + '.' + extension;
            } else {
                return baseName;
            }
        }

        /**
         * Returns the file name for a localized file with the same base name
         * and extension but the specified locale. If the locale is
         * {@code null}, then this returns the default file name.
         *
         * @param loc the locale to create a file name for
         * @return the file name for the specified locale
         * @see #getDefaultFileName()
         */
        public String getFileNameFor(Locale loc) {
            if (loc == null) {
                return getDefaultFileName();
            }

            StringBuilder b = new StringBuilder(baseName.length() + 20);
            b.append(baseName).append('_').append(loc.toString());
            if (extension != null) {
                b.append('.').append(extension);
            }
            return b.toString();
        }

        /**
         * Returns the localized file name for the parent locale, or
         * {@code null} if this file represents a default translation. For
         * example, the parent of {@code file_en_CA.ext} is
         * {@code file_en.ext}; The parent of {@code file_en.ext} is
         * {@code file.ext}, and the parent of that file is
         * {@code null}.
         *
         * @return the next fallback translation for the file, or
         * {@code null}
         */
        public LocalizedFileName getParent() {
            if (localeDescription == null) {
                return null;
            }
            int score = localeDescription.lastIndexOf('_');
            if (score <= 0) {
                return new LocalizedFileName(baseName, null, extension);
            } else {
                return new LocalizedFileName(baseName, localeDescription.substring(0, score), extension);
            }
        }

        /**
         * Returns a string that describes the parts of the localized file name.
         * This is only intended for use in debugging purposes.
         *
         * @return a description of the file name tokens
         */
        @Override
        public String toString() {
            return "LocalizedFileName{" + "baseName=" + baseName + ", localeDescription=" + localeDescription + ", extension=" + extension + '}';
        }
    }
}
