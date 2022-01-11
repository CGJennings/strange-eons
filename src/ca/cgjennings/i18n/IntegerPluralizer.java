package ca.cgjennings.i18n;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import resources.Language;

/**
 * A pluralizer for integer quantities. A pluralizer is used to select the
 * correct localized plural form for a given quantity. For example, in English
 * one writes "1 orange" but "2 oranges"; "1 ox" but "2 oxen". Depending on the
 * locale, a word may have one plural form or many. The pluralizer understands
 * the pluralization rules for the various locales that it supports, and it can
 * supply the correct form automatically so long as you set up your localization
 * files in the way that it expects.
 *
 * <p>
 * Although you can request a pluralizer for any locale by calling
 * {@link #create(java.util.Locale)}, you do not normally create a pluralizer
 * yourself. Instead, you obtain a pluralizer from the appropriate
 * {@link Language}. For example, to pluralize text in the user interface
 * language, you would call {@code Language.getInterface().getPluralizer()}
 * to get the pluralizer for the user interface language.
 *
 * <p>
 * The most common way to set up your {@code .properties} file when you
 * want to add a pluralizable string is to add a key for the singular form, then
 * add a separate key with "-pl" on the end for the plural. For example:
 * <pre>
 * elephant-count = I see %d elephant.
 * elephant-count-pl = I see %d elephants.
 * </pre>
 *
 * <p>
 * For locales with more than one plural form, additional keys would be defined
 * with the names {@code elephant-count-pl2},
 * {@code elephant-count-pl3}, and so on. Locales that don't have plural
 * forms would simply define the singular form key.
 *
 * <p>
 * Using a {@code Language} instance that has this {@code .properties}
 * file loaded, you could obtain a properly formatted and localized plural by
 * calling code similar to the following:
 * <pre>language.getPluralizer().pluralize( numElephants, "elephant-count", numElephants );</pre>
 * (The second {@code numElephants} argument is used to replace the
 * {@code %d} in the plural string.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class IntegerPluralizer {

    /**
     * Creates a new pluralizer instance.
     */
    protected IntegerPluralizer() {
    }
    private int builtin = -1;
    private boolean isDummy;

    /**
     * Create a pluralizer for the default locale. If no more specific
     * pluralizer is available, a default pluralizer is returned that follows
     * the same rules as English.
     *
     * @return a pluralizer for the default locale
     */
    public static IntegerPluralizer create() {
        return create(Locale.getDefault());
    }

    /**
     * Create a pluralizer for the requested locale.
     *
     * <p>
     * The base class includes built-in support for a number of languages. If
     * none of these is suitable, then it will attempt to instantiate a class in
     * this package with the name {@code IntegerPluralizer_<i>xx</i>},
     * where <i>xx</i> is the language code for the locale. If this class cannot
     * be found and instantiated, then a default pluralizer is returned that
     * follows the same rules as English.
     *
     * @param loc the locale, or {@code null} to use the default locale
     * @return a pluralizer for the locale
     */
    public static IntegerPluralizer create(Locale loc) {
        if (loc == null) {
            loc = Locale.getDefault();
        }

        // see if the pluralizer is one of the languages supported by the base class
        IntegerPluralizer pluralizer;
        final String language = loc.getLanguage();
        final String regional = loc.getLanguage() + '_' + loc.getCountry();

        for (int b = 0; b < BUILTINS.length; ++b) {
            final String[] members = BUILTINS[b];
            for (int m = 0; m < members.length; ++m) {
                if (members[m].equals(language) || members[m].equals(regional)) {
                    pluralizer = new IntegerPluralizer();
                    pluralizer.builtin = b;
                    return pluralizer;
                }
            }
        }

        // nope: try to instantiate IntegerPluralizer_xx
        try {
            @SuppressWarnings("unchecked")
            Class<? extends IntegerPluralizer> klass = (Class<? extends IntegerPluralizer>) Class.forName("ca.cgjennings.i18n.IntegerPluralizer_" + language);
            IntegerPluralizer p = (IntegerPluralizer) klass.getConstructor().newInstance();
            // check that the programmer overrode all the methods that they need to
            p.getPluralForm(0);
            p.getPluralFormCount();
            p.getPluralFormDescription();
        } catch (ClassNotFoundException e) {
        } catch (ReflectiveOperationException | RuntimeException e) {
            StrangeEons.log.log(Level.SEVERE, "failed to instantiate for locale: " + language, e);
        }

        // nope: return a default English pluralizer with the fallback flag set
        IntegerPluralizer dummy = new IntegerPluralizer();
        dummy.builtin = 0;
        dummy.isDummy = true;
        return dummy;
    }

    // NOTE: if you need to have different codes for a language based on the country,
    // you can add ll_cc entries to this array. However, you should make the entry
    // with the highest index a default that uses just the language so that if the
    // locale only contains a language, something relevant will still be picked.
    private static final String[][] BUILTINS = new String[][]{
        {"en", "bg", "de", "es", "it", "da", "nl", "fo", "fy", "no", "sv",
            "et", "fi", "hu", "eu", "el", "he", "pt", "ca"},
        {"cs", "sk"},
        {"fr"},
        {"pl"},
        {"ru", "bs", "hr", "sr", "uk"},
        {"zh", "ja", "ko", "vi", "fa", "tr", "th", "lo"},};
    private static final int DEFAULT = 0, CS = 1, FR = 2, PL = 3, RU = 4, ZH = 5;

    /**
     * Returns the plural form index to use for {@code number}. For
     * example, the English pluralizer returns plural form 0 (singular) if
     * {@code number == 1}, and otherwise returns 1 (first plural form).
     *
     * <p>
     * <b>Subclasses that implement support for specific locales must override
     * this method.</b>
     *
     * @param number the number to choose a plural form for
     * @return the plural form index for the chosen number in this pluralizer's
     * locale
     * @see #createKey
     */
    public int getPluralForm(int number) {
        int f;
        switch (builtin) {
            case DEFAULT:
                f = (number == 1) ? 0 : 1;
                break;
            case CS:
                f = (number == 1) ? 0 : (number >= 2 && number <= 4 ? 1 : 2);
                break;
            case FR:
                f = (number == 0 || number == 1) ? 0 : 1;
                break;
            case PL:
                if (number == 1) {
                    f = 0;
                } else {
                    f = 2;
                    final int lastDigit = number % 10;
                    if (lastDigit >= 2 && lastDigit <= 4) {
                        final int secondLastDigit = number / 10 % 10;
                        if (secondLastDigit != 1) {
                            f = 1;
                        }
                    }
                }
                break;
            case RU:
                f = 2;
                 {
                    final int lastDigit = number % 10;
                    if (lastDigit >= 1 && lastDigit <= 4) {
                        final int secondLastDigit = number / 10 % 10;
                        if (lastDigit == 1) {
                            if (secondLastDigit != 1) {
                                f = 0;
                            }
                        } else {
                            if (secondLastDigit != 1) {
                                f = 1;
                            }
                        }
                    }
                }
                break;
            case ZH:
                f = 0;
                break;
            default:
                throw new AssertionError("override getPluralForm(int); builtin=" + builtin);
        }
        return f;
    }

    /**
     * Returns the plural form index to use for a {@code number}. This is
     * the long-sized analogue to {@link #getPluralForm(int)}.
     *
     * @param number the number to choose a plural form for
     * @return the plural form index for the chosen number in this pluralizer's
     * locale
     * @see #createKey
     */
    public int getPluralForm(long number) {
        return getPluralForm((int) (Math.abs(number) % 10_000));
    }

    /**
     * Returns the number of plural forms used by this pluralizer. For example,
     * an English pluralizer would return 2, a Japanese pluralizer would return
     * 1 (since Japanese doesn't use different word forms based on quantity),
     * and a Polish pluralizer would return 3.
     *
     * @return the number of plural forms used by this pluralizer; this is one
     * more than the maximum value that may be returned by
     * {@link #getPluralForm(int)}.
     */
    public int getPluralFormCount() {
        int f;
        switch (builtin) {
            case DEFAULT:
            case FR:
                f = 2;
                break;
            case CS:
            case PL:
            case RU:
                f = 3;
                break;
            case ZH:
                f = 1;
                break;
            default:
                throw new AssertionError("override getPluralFormCount()");
        }
        return f;
    }

    /**
     * Returns a brief description, in English, of the rule used by the
     * pluralizer to select a plural form. Although the description is in
     * English, it follows a specific format so that it can be readily parsed to
     * format it for display. Each plural form is described on a separate line,
     * in increasing order, and each line is divided by a colon. Before the
     * colon comes a description of when the form applies, and after the colon
     * comes an example key name for a key with the base name "key". For
     * example:
     * <pre>
     * N is 1: key
     * N ends in 2-4, except if it ends in 12-14: key-pl
     * Everything else: key-pl2
     * </pre>
     *
     * @return a brief description of the pluralization rule
     */
    public String getPluralFormDescription() {
        if (builtin < 0) {
            throw new AssertionError("override getPluralFormDescription()");
        }
        return ResourceBundle.getBundle(getClass().getPackage().getName().replace('.', '/') + "/pluralizer").getString(String.valueOf(builtin));
    }

    /**
     * Returns a key name based on a plural form index and base key name parts.
     * The value of {@code pluralForm} is 0 for the singular form, 1 for
     * the first plural form, 2 for the second plural form, and so on. (The
     * exact meaning of the plural form index is locale-dependent.) The returned
     * key name is composed by following this pattern:
     * <pre>
     * basePrefix + singularInfix     + baseSuffix      (plural form 0)
     * basePrefix + pluralInfix       + baseSuffix      (plural form 1)
     * basePrefix + pluralInfix + "2" + baseSuffix      (plural form 2)
     * basePrefix + pluralInfix + "3" + baseSuffix      (plural form 3)
     * ...
     * </pre>
     *
     *
     * @param basePrefix the prefix used for all keys (may be empty)
     * @param baseSuffix the suffix used for all keys (may be empty)
     * @param singularInfix the infix used if the plural index is for the
     * singular form
     * @param pluralInfix the infix used for the first plural form, and as the
     * basis of subsequent plural forms
     * @return a key composed for the specific plural form
     */
    public String createKey(int pluralForm, String basePrefix, String baseSuffix, String singularInfix, String pluralInfix) {
        String key;
        if (pluralForm == 0) {
            key = basePrefix + singularInfix + baseSuffix;
        } else if (pluralForm == 1) {
            key = basePrefix + pluralInfix + baseSuffix;
        } else {
            key = basePrefix + pluralInfix + String.valueOf(pluralForm) + baseSuffix;
        }
        return key;
    }

    /**
     * Returns an appropriate plural form string based on an integer value. This
     * is a convenience method that is equivalent to
     * {@code pluralize( getLanguage(), number, resourceKeyBase )}.
     *
     * @param number the number to for which a plural form should be selected
     * @param resourceKeyBase the base key to use to generate a key set
     * @return the appropriate plural form, or an error message
     * @see #pluralize(resources.Language, int, java.lang.String)
     */
    public String pluralize(int number, String resourceKeyBase) {
        return pluralize(getLanguage(), number, resourceKeyBase);
    }

    /**
     * Returns an appropriate plural form string based on an integer value. A
     * set of keys is generated as if by calling
     * {@code createKeys( resourceKeyBase, "", "", "-pl" )}. This means
     * that the following keys will be used:<br>
     * {@code <i>resourceKeyBase</i>} singular form<br>
     * {@code <i>resourceKeyBase</i>-pl} first plural form<br>
     * {@code <i>resourceKeyBase</i>-pl2} second plural form<br>
     * ...
     *
     * <p>
     * Of these keys, the appropriate key will be selected based on the value of
     * {@code number}, and then the value of this key in
     * {@code language} returned.
     *
     * @param language the language to use to look up the plural string
     * @param number the number to for which a plural form should be selected
     * @param resourceKeyBase the base key to use to generate a key set
     * @return the appropriate plural form, or an error message
     * @throws NullPointerException if the language or base key is
     * {@code null}
     */
    public String pluralize(Language language, int number, String resourceKeyBase) {
        if (language == null) {
            throw new NullPointerException("language");
        }
        if (resourceKeyBase == null) {
            throw new NullPointerException("resourceKeyBase");
        }

        int form = getPluralForm(number);

        String key;
        do {
            key = createKey(form, resourceKeyBase, "", "", "-pl");
            if (language.isKeyDefined(key)) {
                return language.get(key);
            }
        } while (--form > 0);
        return language.get(key);
    }

    /**
     * Returns a pluralized string that has been formatted using the supplied
     * objects. This is a convenience for calling {@code String.format} on
     * the result of a call to {@link #pluralize(int, java.lang.String)}.
     *
     * @param number the number to for which a plural form should be selected
     * @param resourceKeyBase the base key to use to generate a key set
     * @param formatObjects the objects to use when formatting the resulting
     * string
     * @return a localized, pluralized, and formatted string
     * @throws NullPointerException if the base key is {@code null}
     */
    public String pluralize(int number, String resourceKeyBase, Object... formatObjects) {
        return String.format(getLanguage().getLocale(), pluralize(getLanguage(), number, resourceKeyBase), formatObjects);
    }

    /**
     * Returns a pluralized string that has been formatted using the supplied
     * objects. This is a convenience for calling {@code String.format} on
     * the result of a call to
     * {@link #pluralize(resources.Language, int, java.lang.String)}.
     *
     * @param language the language to use to look up the plural string
     * @param number the number to for which a plural form should be selected
     * @param resourceKeyBase the base key to use to generate a key set
     * @param formatObjects the objects to use when formatting the resulting
     * string
     * @return a localized, pluralized, and formatted string
     * @throws NullPointerException if the language or base key is
     * {@code null}
     */
    public String pluralize(Language language, int number, String resourceKeyBase, Object... formatObjects) {
        return String.format(language.getLocale(), pluralize(language, number, resourceKeyBase), formatObjects);
    }

    /**
     * Returns the default language instance that will be used by this
     * pluralizer to look up plural form strings.
     *
     * @return the default language that this pluralizer will use to look up
     * pluralized strings
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Sets the language instance to use when looking up plural form strings.
     * (The default for new pluralizer instances is
     * {@link Language#getInterface()}.)
     *
     * @param language the new default language for this instance
     * @throws NullPointerException if the language is {@code null}
     */
    public void setLanguage(Language language) {
        if (language == null) {
            throw new NullPointerException("language");
        }
        this.language = language;
    }

    private Language language = Language.getInterface();

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * If this returns {@code true}, then there is no built-in pluralizer
     * for this pluralizer's locale and a default pluralizer is being used that
     * follows the same rules as English. When starting a translation, you can
     * test if pluralizer support needs to be added by running script code
     * similar to the following (where <i>ll</i> is the relevant language code):
     * <pre>println( ca.cgjennings.i18n.IntegerPluralizer.create( new java.util.Locale("<i>ll</i>") ).isFallbackPluralizer() );</pre>
     * If this prints {@code true}, then you should request that
     * pluralization support be added for your language. Alternatively, if you
     * are familiar with Java, you can add support yourself by writing a class
     * with the name {@code IntegerPluralizer_<i>ll</i>}. The class must be
     * in the same package as this class, and it must subclass
     * {@code IntegerPluralizer} and override the {@link #getPluralForm(int)},
     * {@link #getPluralFormCount()}, and {@link #getPluralFormDescription()}
     * methods to implement the pluralization rule for the locale.
     *
     * @return {@code true} if pluralizer support for your locale needs to
     * be added
     */
    public final boolean isFallbackPluralizer() {
        return isDummy;
    }
}
