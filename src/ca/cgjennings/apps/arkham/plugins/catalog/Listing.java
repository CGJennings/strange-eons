package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.plugins.PluginRoot;
import ca.cgjennings.io.EscapedTextCodec;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import resources.Language;
import resources.Settings;

/**
 * A catalog listing that provides information about a plug-in bundle that the
 * user might wish to download. A listing can include technical information to
 * enable downloading, verifying, and installing the plug-in, versioning
 * information (a {@link CatalogID}), category information, and a description
 * for the user.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public final class Listing implements Comparable<Listing> {

    private Properties p;
    private URL absUrl;

    /**
     * Creates a new listing. The download location of the listed plug-in will
     * be relative to the base URL. The keys stored in the properties instance
     * will be used to fill in the fields of the listing.
     *
     * @param baseURL the base URL to use when composing a URL
     * @param p the properties of the listing
     * @throws NullPointerException if the properties are {@code null}
     * @throws IllegalArgumentException if required fields are missing from the
     * provided properties
     */
    Listing(URL baseURL, Properties p) {
        this.p = (Properties) p.clone();
        String missing = checkRequiredFields();
        if (missing != null) {
            throw new IllegalArgumentException("Missing required field: " + missing + " [" + p + "]");
        }
        if (baseURL != null) {
            try {
                String url = get(URL);
                absUrl = new URL(baseURL, get(URL));
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid URL: " + get(URL) + " [" + p + "]", e);
            }
        }
    }

    /**
     * Creates a new listing based on a local file that might be added to a
     * catalog. The file must be a plug-in bundle. If it already has a catalog
     * ID, that ID will be used. Otherwise a new ID will be generated and added
     * to the file. Note that no checksum will be set for this listing since
     * checksums are based on the actual download file, which is typically a
     * published bundle generated from the plug-in bundle.
     *
     * @param f the (unpublished) plug-in bundle to extract listing details from
     */
    public Listing(File f) throws IOException {
        p = new Properties();

        // Copy relevant keys stored in the root file into the listing
        PluginBundle pb = new PluginBundle(f);
        PluginRoot root = pb.getPluginRoot();
        if (root.getCatalogID() == null) {
            root.setCatalogID(new CatalogID());
            root.updateBundle();
        }

        Set<String> keys = root.getClientPropertyKeys();
        for (String key : unlocalizedKeysToImport) {
            importRootFileKey(root, keys, "catalog-" + key, key, false);
        }
        for (String key : localizedKeysToImport) {
            if (!p.containsKey(key)) {
                importRootFileKey(root, keys, "catalog-" + key, key, true);
            }
        }
        for (String key : fallbackKeysToImport) {
            if (!p.containsKey(key)) {
                importRootFileKey(root, keys, key, key, true);
            }
        }

        // Set the initial URL
        String name = f.getName();
        absUrl = f.toURI().toURL();
        p.setProperty(URL, "./" + name);

        // If there still isn't a name, use the file name
        if (!p.containsKey(NAME)) {
            int ext = name.indexOf('.');
            // if 0, we'd end up with an empty name
            if (ext > 0) {
                name = name.substring(0, ext);
            }
            p.setProperty(NAME, name);
        }

        // Copy the CatID
        setCatalogID(root.getCatalogID());
    }

    private void importRootFileKey(PluginRoot root, Set<String> rootKeySet, String rootKey, String listingKey, boolean localized) {
        if (rootKeySet.contains(rootKey)) {
            set(listingKey, root.getClientProperty(rootKey));
        }
        if (localized) {
            final String localizedRootKeyPrefix = rootKey + '_';
            for (String localizedRootKey : rootKeySet) {
                if (localizedRootKey.startsWith(localizedRootKeyPrefix)) {
                    String localizedListingKey = listingKey + localizedRootKey.substring(localizedRootKey.indexOf('_'));
                    set(localizedListingKey, root.getClientProperty(localizedRootKey));
                }
            }
        }
    }

    /**
     * Returns the (possibly localized) value of a key in the catalog
     * properties. The value will be looked up by appending UI locale
     * information in the following order:
     * <pre>
     * key_lang_country
     * key_lang
     * key
     * </pre> The first key that is defined will be returned, or else
     * {@code null} will be returned if the none of these keys are defined.
     * For example, on a French system in Canada, when looking up the key
     * <tt>homepage</tt>, the following keys are searched (in order):
     * <tt>homepage_fr_CA</tt>,
     * <tt>homepage_fr</tt>, <tt>homepage</tt>.
     *
     * @param key the name of the catalog listing property to look up
     * @return the value of the property in the first matching locale, or
     * {@code null}
     */
    public String get(String key) {
        Locale loc = Locale.getDefault();
        if (!loc.getLanguage().isEmpty()) {
            String val;
            if (!loc.getCountry().isEmpty()) {
                val = p.getProperty(key + "_" + loc.getLanguage() + "_" + loc.getCountry());
                if (val != null) {
                    return val;
                }
            }
            val = p.getProperty(key + "_" + loc.getLanguage());
            if (val != null) {
                return val;
            }
        }
        return p.getProperty(key);
    }

    String getUnlocalized(String key) {
        return p.getProperty(key);
    }

    /**
     * Sets the value of the given key. If the value is set to
     * {@code null}, the key will be removed.
     *
     * @param key the name of the key to modify
     * @param value the new value of the key, or {@code null} to delete it
     * @throws NullPointerException if the key is {@code null}
     */
    public void set(String key, String value) {
        p.setProperty(key, value);
    }

    /**
     * Returns the catalog ID for this listing, or {@code null} if none is
     * set.
     *
     * @return the catalog ID, or {@code null}
     */
    public CatalogID getCatalogID() {
        String id = p.getProperty(ID);
        if (id == null) {
            return null;
        }
        return CatalogID.extractCatalogID(id);
    }

    /**
     * Sets the catalog ID for this listing, or clears it if {@code null}.
     *
     * @param id the new ID
     */
    public void setCatalogID(CatalogID id) {
        p.setProperty(ID, id.toString());
    }

    /**
     * Returns the download file checksum string for this listing, or
     * {@code null} if none is present. The checksum string can be used to
     * check that the file downloaded correctly.
     *
     * @return the checksum string or {@code null}
     */
    public String getChecksum() {
        return get(DIGEST);
    }

//	public void updateMessageDigest() throws IOException {
//		InputStream in = null;
//		try {
//			in = absUrl.openStream();
//			p.setProperty( DIGEST, createDigest( in ) );
//			System.err.println( p.getProperty( DIGEST ) );
//		} catch( IOException e ) {
//			if( in != null ) {
//				in.close();
//			}
//		}
//	}
    /**
     * Sets the checksum value for the listing from a computed checksum value,
     * or removes it if {@code null}. Checksums are computed on the actual
     * download file (typically a published bundle); they are not computed on
     * the plug-in bundle unless the plug-in bundle is the actual file that will
     * be downloaded.
     *
     * @param checksum the checksum calculator containing the relevant checksum,
     * or {@code null}
     */
    public void setChecksum(MD5Checksum checksum) {
        set(DIGEST, checksum == null ? null : checksum.getChecksumString());
    }

    /**
     * Returns {@code true} if the checksum in this listing matches the
     * checksum of a file. If this listing has no checksum, returns
     * {@code true} without computing the file's checksum. Note that
     * checksums should be computed against the actual file that is downloaded
     * (typically a published bundle), not against any other forms.
     *
     * @param f the file to compare to this listing's checksum
     * @return {@code true} if the file does not appear to be corrupt
     * @throws IOException if an exception occurs while processing the file
     */
    public boolean checksumMatches(File f) throws IOException {
        String listed = getChecksum();
        if (listed == null) {
            return true;
        }

        MD5Checksum target = MD5Checksum.forFile(f);
        return target.matches(listed);
    }

    /**
     * The catalog ID (required).
     */
    public static final String ID = "id";
    /**
     * The URL, either absolute or relative to the catalog if it starts with
     * "./" (required).
     */
    public static final String URL = "url";
    /**
     * The name of the plug-in (may be localized) (required).
     */
    public static final String NAME = "name";
    /**
     * The human friendly version of the plug-in.
     */
    public static final String VERSION = "version";
    /**
     * The download size (bytes).
     */
    public static final String SIZE = "size";
    /**
     * The installed (unpacked) size (bytes).
     */
    public static final String INSTALL_SIZE = "install-size";
    /**
     * The human-friendly description (may be localized).
     */
    public static final String DESCRIPTION = "description";
    /**
     * The MD5 checksum of the unpacked bundle.
     */
    public static final String DIGEST = "md5";
    /**
     * A URL for more information.
     */
    public static final String HOME_PAGE = "homepage";
    /**
     * An optional date that overrides the catalog ID.
     */
    public static final String DATE = "date";
    /**
     * Name of the plug-in developer.
     */
    public static final String CREDIT = "credit";
    /**
     * Tags that can be searched for.
     */
    public static final String TAGS = "tags";
    /**
     * The minimum build number needed by the plug-in.
     */
    public static final String MINIMUM_VERSION = "minver";
    /**
     * The maximum build number that the plug-in is compatible with.
     */
    public static final String MAXIMUM_VERSION = "maxver";
    /**
     * A list of other catalog IDs required by this plug-in.
     */
    public static final String REQUIRES = "requires";
    /**
     * A catalog ID that should be uninstalled if this is installed.
     */
    public static final String REPLACES = "replaces";
    /**
     * The listing should not be included in the download list shown to the
     * user.
     */
    public static final String HIDDEN = "hidden";
    /**
     * The listing is a core component and should be shown with a core component
     * icon (the value identifies the core and is used for filtering).
     */
    public static final String CORE = "core";
    /**
     * The listing supports the game with the specific listed game code listed.
     */
    public static final String GAME = "game";
    /**
     * A comment specific to the listing.
     */
    public static final String COMMENT = "comment";

    /**
     * Returns {@code null} if required fields are present, otherwise the
     * key of the first missing field.
     *
     * @return the first missing required field, or {@code null}
     */
    private String checkRequiredFields() {
        for (String f : REQUIRED_FIELDS) {
            if (get(f) == null) {
                return f;
            }
        }
        return null;
    }
    private static final String[] REQUIRED_FIELDS = new String[]{ID, URL, NAME};

    @Override
    public int compareTo(Listing o) {
        String lhs = get(NAME);
        if (lhs == null) {
            lhs = "";
        }
        String rhs = o == null ? "" : o.get(NAME);
        if (rhs == null) {
            rhs = "";
        }
        return Language.getInterface().getCollator().compare(lhs, rhs);
    }

    /**
     * Returns {@code true} if and only if the listing has exactly the same
     * keys and values as the specified listing. (This method is not named
     * {@code equals} to avoid confusion with
     * {@link #compareTo(ca.cgjennings.apps.arkham.plugins.catalog.Listing) compareTo},
     * which simply compares the names of the listings for sorting purposes.)
     *
     * @param other the listing to compare this listing to
     * @return {@code true} if the listings have exactly the same keys and
     * values
     */
    public boolean isIdenticalTo(Listing other) {
        if (p.size() != other.p.size()) {
            return false;
        }
        for (String key : p.stringPropertyNames()) {
            if (!Objects.equals(p.getProperty(key), other.p.getProperty(key))) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        String t = get(NAME);
        return t == null ? "" : t;
    }

    public String getVersion() {
        String t = get(VERSION);
        if (t == null) {
            return "";
        }
        try {
            float f = Float.parseFloat(t);
            return String.format("%.1f", f);
        } catch (NumberFormatException e) {
            return t;
        }
    }

    public long getSize() {
        String t = get(SIZE);
        if (t == null) {
            return -1;
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String getDisplayDate() {
        String t = get(DATE);
        if (t != null) {
            String[] tokens = t.split("\\-");
            if (tokens.length >= 3) {
                GregorianCalendar c = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
                c.set(Calendar.YEAR, Integer.valueOf(tokens[0]));
                c.set(Calendar.MONTH, Integer.valueOf(tokens[1]) - 1);
                c.set(Calendar.DAY_OF_MONTH, Integer.valueOf(tokens[2]));
                if (tokens.length >= 6) {
                    c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(tokens[3]));
                    c.set(Calendar.MINUTE, Integer.valueOf(tokens[4]));
                    c.set(Calendar.SECOND, Integer.valueOf(tokens[5]));
                } else {
                    c.set(Calendar.HOUR_OF_DAY, 0);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                }
                return String.format(
                        "%1$ta %1$tb %1$td %1$tY",
                        c
                );
            }
        }
        CatalogID id = getCatalogID();
        if (id == null) {
            return "";
        }
        return String.format(
                "%1$ta %1$tb %1$td %1$tY",
                id.getDate()
        );
    }

    public String getDescription() {
        String t = get(DESCRIPTION);
        return t == null ? "" : t;
    }

    public URL getHomePage() {
        String t = get(HOME_PAGE);
        if (t == null) {
            return null;
        }
        try {
            return new URL(t);
        } catch (MalformedURLException e) {
            System.err.println("Bad home page format: " + t);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Compares the version of the application to the version information
     * included in the catalog listing, if any. Returns 0 if the version
     * information indicates that this plug-in is compatible, -1 if the plug-in
     * is deprecated for this version (the application is too new), or 1 if the
     * plug-in requires a new version of the application.
     *
     * @return 0 if the plug-in is compatible, or else -1 or 1 if the
     * application version is too old or too new
     */
    public int getApplicationCompatibility() {
        String t;

        t = get(MAXIMUM_VERSION);
        if (t != null) {
            try {
                int maxVer = parseVersion(t);
                if (StrangeEons.getBuildNumber() > maxVer) {
                    return -1;
                }
            } catch (IllegalArgumentException e) {
                StrangeEons.log.log(Level.WARNING, "PLUG-INS: Warning: listing contains bad maxver: {0}", getName());
            }
        }

        t = get(MINIMUM_VERSION);
        if (t != null) {
            try {
                int minVer = parseVersion(t);
                if (StrangeEons.getBuildNumber() < minVer) {
                    return 1;
                }
            } catch (IllegalArgumentException e) {
                StrangeEons.log.log(Level.WARNING, "PLUG-INS: Warning: listing contains bad minver: {0}", getName());
            }
        }

        return 0;
    }

    private static int parseVersion(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns {@code true} if the listing is hidden. A listing is hidden
     * if its {@code hidden} property is set to {@code yes}, or if it
     * is set to {@code depends} and the plug-in is either not installed or
     * is up to date.
     *
     * @return {@code true} if the listing should be hidden in the catalog
     */
    public boolean isHidden() {
        String h = get(HIDDEN);
        if (h != null) {
            if (h.equals("depends")) {
                // a depends entry is visible if the plug-in is installed AND
                // this is an update; useful for hidden, required plug-ins
                CatalogID id = getCatalogID();
                if (id == null) {
                    return true;
                }
                CatalogID installed = BundleInstaller.getInstalledCatalogID(id.getUUID());
                if (installed == null) {
                    return true;
                }
                return !installed.isOlderThan(id);
            } else {
                return Settings.yesNo(h);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this plug-in appears to be "new", that is,
     * added recently and not seen by the user before. A listing is considered
     * new if its timestamp is more recent than the timestamp of the newest
     * bundle that was observed when the application was last run.
     *
     * @return {@code true} if this listing is considered "new";
     * {@code false} otherwise
     */
    public boolean isNew() {
        return AutomaticUpdater.isNew(getCatalogID());
    }

    /**
     * Writes this listing in standard order.
     *
     * @param w the writer to write this listing to
     * @throws IOException if an error occurs
     */
    void write(Writer w) throws IOException {
        LinkedList<String> keyList = new LinkedList<>();
        Enumeration<?> names = p.propertyNames();
        while (names.hasMoreElements()) {
            keyList.add(names.nextElement().toString());
        }
        String[] keys = keyList.toArray(new String[keyList.size()]);
        java.util.Arrays.sort(keys);

        // write standard keys
        for (int i = 0; i < KEY_ORDER.length; ++i) {
            writeKey(w, keys, KEY_ORDER[i]);
        }

        // write any custom keys
        for (int i = 0; i < keys.length; ++i) {
            if (keys[i] != null) {
                writeKey(w, keys, keys[i]);
            }
        }
    }

    private void writeKey(Writer w, String[] keys, String key) throws IOException {
        String prefix = key + "_";
        int i;
        for (i = 0; i < keys.length; ++i) {
            if (keys[i] == null) {
                continue;
            }
            if (keys[i].equals(key) || keys[i].startsWith(prefix)) {
                break;
            }
        }

        if (i == keys.length) {
            return;
        }

        do {
            String value = getUnlocalized(keys[i]);
            w.write(EscapedTextCodec.escapeUnicode(keys[i].replace(":", "\\:").replace("=", "\\=")));
            if (!value.isEmpty()) {
                w.write(" = ");
                value = EscapedTextCodec.escapeUnicode(value.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n"));
                value = wrap(value, "\\\n    ", 80 - keys[i].length() + 3, 76, 10);
                w.write(value);
            }
            w.write('\n');
            keys[i] = null;
            ++i;
        } while (i < keys.length && keys[i] != null && (keys[i].equals(key) || keys[i].startsWith(prefix)));
    }

    private static String wrap(String s, String wrapText, int firstWrap, int afterWrap, int tolerance) {
        if (s.length() < firstWrap) {
            return s;
        }

        int wrap = firstWrap;
        StringBuilder b = new StringBuilder();
        int line = 0, maxLines = 1_024;

        while (s.length() > (wrap + tolerance) && line++ < maxLines) {
            // find acceptable wrap position:
            //   - must come right after a space
            //   - next char must not be a space
            int pos = wrap;
            while (pos >= 0 && !Character.isSpaceChar(s.charAt(pos))) {
                --pos;
            }
            // couldn't find a space, just break at the wrap point
            if (pos < 0) {
                pos = wrap;
            }
            // skip any following spaces
            while (pos < s.length() - 1 && Character.isSpaceChar(s.charAt(pos + 1))) {
                ++pos;
            }
            // the break char should be included in the string for this line;
            // but substring excludes the final pos
            ++pos;
            b.append(s.substring(0, pos));
            s = s.substring(pos);
            if (!s.isEmpty()) {
                b.append(wrapText);
            }

            wrap = afterWrap;
        }
        if (!s.isEmpty()) {
            b.append(s);
        }
        return b.toString();
    }

    private static final String[] KEY_ORDER = new String[]{
        Listing.HIDDEN,
        Listing.CORE,
        Listing.URL,
        Listing.NAME,
        Listing.CREDIT,
        Listing.HOME_PAGE,
        Listing.DESCRIPTION,
        Listing.GAME,
        Listing.TAGS,
        Listing.SIZE,
        Listing.INSTALL_SIZE,
        Listing.DATE,
        Listing.VERSION,
        Listing.MAXIMUM_VERSION,
        Listing.MINIMUM_VERSION,
        Listing.REPLACES,
        Listing.REQUIRES,
        Listing.DIGEST,
        Listing.ID,
        Listing.COMMENT
    };

    /**
     * Keys imported from root files.
     */
    private static final String[] localizedKeysToImport = new String[]{
        NAME, DESCRIPTION, CREDIT, HOME_PAGE
    };
    /**
     * Keys imported from root files without localization.
     */
    private static final String[] unlocalizedKeysToImport = new String[]{
        COMMENT, CORE, GAME, DATE, MAXIMUM_VERSION, MINIMUM_VERSION,
        REPLACES, REQUIRES, TAGS, VERSION, HIDDEN, /**
     * ID is imported by calling getCatalogID rather than by key name.
     */
    };
    /**
     * Standard root file keys to copy if the catalog- key is missing from the
     * root.
     */
    private static final String[] fallbackKeysToImport = new String[]{
        NAME, DESCRIPTION
    };

    @Override
    public String toString() {
        StringWriter w = new StringWriter(256);
        try {
            write(w);
        } catch (IOException e) {
            return "unexpected error: " + e;
        }
        return w.toString();
    }
}
