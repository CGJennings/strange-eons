package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.plugins.PluginRoot;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A unique identifier for a particular version of a particular plug-in bundle.
 * This ID consists of two parts: a UUID that is unique to a given plug-in
 * bundle, and an encoded date that identifies the version.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class CatalogID {

    private UUID uuid;
    private GregorianCalendar date;

    /**
     * Creates an ID with a new UUID and the current time. This is used when
     * adding a new bundle to a catalog.
     */
    public CatalogID() {
        this(null, null);
    }

    /**
     * Creates an ID with a UUID taken from the ID of an existing bundle and the
     * current time. This is used to update an existing bundle when a new
     * version becomes available.
     *
     * @param parent
     */
    public CatalogID(CatalogID parent) {
        this(parent.uuid, null);
    }

    /**
     * Creates an ID with the given UUID and the current time.
     *
     * @param uuid the UUID part of the new ID
     */
    public CatalogID(UUID uuid) {
        this(uuid, null);
    }

    protected CatalogID(UUID uuid, GregorianCalendar date) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (date == null) {
            date = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        }
        this.uuid = uuid;
        this.date = date;
    }

    @Override
    public String toString() {
        return "CATALOGUEID{" + toUUIDString() + ":" + toDateString() + "}";
    }

    public String toDateString() {
        return "" + date.get(Calendar.YEAR) + "-" + date.get(Calendar.MONTH) + "-" + date.get(Calendar.DAY_OF_MONTH) + "-" + date.get(Calendar.HOUR_OF_DAY) + "-" + date.get(Calendar.MINUTE) + "-" + date.get(Calendar.SECOND) + "-" + date.get(Calendar.MILLISECOND);
    }

    public String toUUIDString() {
        return uuid.toString();
    }

    private static UUID parseUUIDString(String uuid) {
        return UUID.fromString(uuid.toLowerCase(Locale.CANADA));
    }

    private static GregorianCalendar parseDateString(String date) {
        String[] tokens = date.split("-");
        if (tokens.length != 7) {
            throw new IllegalArgumentException("invalid date: " + date);
        }
        GregorianCalendar c = (GregorianCalendar) GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.YEAR, Integer.valueOf(tokens[0]));
        c.set(Calendar.MONTH, Integer.valueOf(tokens[1]));
        c.set(Calendar.DAY_OF_MONTH, Integer.valueOf(tokens[2]));
        c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(tokens[3]));
        c.set(Calendar.MINUTE, Integer.valueOf(tokens[4]));
        c.set(Calendar.SECOND, Integer.valueOf(tokens[5]));
        c.set(Calendar.MILLISECOND, Integer.valueOf(tokens[6]));
        return c;
    }

    /**
     * Returns the ID's date as a string formatted for ease of reading by the
     * user.
     *
     * @return a nicely formated string for the ID's timestamp
     */
    public String getFormattedDate() {
        return String.format(
                "%1$ta %1$tb %1$td %1$tT %1$tL ms %1$tZ %1$tY",
                date
        );
    }

    /**
     * If a string contains an ID string description of the form
     * <tt>CATALOGID{<i>uuid</i>:<i>date</i>}</tt>
     * returns it as a {@code CatalogID}. Otherwise returns null.
     *
     * @param text the string to parse
     * @return the bundle ID contained in the string, or null
     */
    public static CatalogID extractCatalogID(String text) {
        Matcher m = ID_PAT.matcher(text);
        if (m.find()) {
            try {
                return new CatalogID(parseUUIDString(m.group(1)), parseDateString(m.group(2)));
            } catch (Exception e) {
                StrangeEons.log.log(Level.WARNING, "skipping invalid bundle ID: " + text, e);
            }
        }
        return null;
    }

    /**
     * Compares the UUID of this ID with another ID returns {@code true} if and
     * only if they are the same.
     *
     * @param rhs the ID to compare UUIDs with
     * @return {@code true} if both IDs have the same UUID
     */
    public boolean sameUUID(CatalogID rhs) {
        if (rhs == null) {
            return false;
        }
        if (rhs.uuid == null) {
            return uuid == null;
        }
        return uuid.equals(rhs.uuid);
    }

    /**
     * Returns {@code true} if this ID and {@code rhs} are equal, meaning that
     * {@code rhs} is also an ID, and its UUID and date match exactly.
     *
     * @param obj the object to test for equality
     * @return {@code true} is and only if the this is equal to {@code rhs}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CatalogID)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        CatalogID rhs = (CatalogID) obj;
        return sameUUID(rhs) && (compareDates(rhs) == 0);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.uuid);
        hash = 83 * hash + Objects.hashCode(this.date);
        return hash;
    }

    /**
     * Compares this ID to another ID and returns {@code true} if and only if
     * they have the same UUID and the date of {@code rhs} is newer than the
     * date of this ID.
     *
     * @param rhs the ID to compare this ID to
     * @return {@code true} if both IDs have the same UUID and this ID is older
     * than the {@code rhs} ID
     */
    public boolean isOlderThan(CatalogID rhs) {
        if (sameUUID(rhs)) {
            return date.before(rhs.date);
        }
        return false;
    }

    /**
     * Compares the date of this ID with another ID, without considering whether
     * the UUIDs match. Returns a negative value if {@code rhs} is newer, zero
     * if they are the same, or a positive value if this ID is newer.
     *
     * @param rhs the ID to compare this ID to
     * @return a negative, zero, or positive value as this ID is older, the same
     * age, or newer than the specified ID
     */
    public int compareDates(CatalogID rhs) {
        return date.compareTo(rhs.date);
    }

    private static final Pattern ID_PAT = Pattern.compile(
            "CATALOGU?E?ID\\{([-\\da-fA-F]+)\\:(\\d+\\-\\d+\\-\\d+\\-\\d+\\-\\d+\\-\\d+\\-\\d+)\\}",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Returns the unique identifier (UUID) for this ID.
     *
     * @return this ID's UUID part
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Returns the date (timestamp) for this ID.
     *
     * @return this ID's date part
     */
    public GregorianCalendar getDate() {
        return date;
    }

    /**
     * Get a bundle's catalog ID. If the bundle is Web-safe, it is first
     * unwrapped to a temporary file. The bundle's root file is located, and it
     * will be parsed and its embedded ID will be returned. Note that if you
     * might wish to access the rest of the plug-in's root file content, it will
     * be more efficient to use code similar to the following:
     * <pre>
     * PluginBundle bundle = new PluginBundle( bundleFile );
     * PluginRoot root = pb.getPluginRoot();
     * id = root.getCatalogID();
     * // do other things with the root file
     * </pre>
     *
     * @param bundleFile the plug-in bundle to extract an ID from
     * @return the UUID listed in the root file, or {@code null}
     * @throws IOException if an error occurs while parsing the UUID
     */
    public static CatalogID getCatalogID(File bundleFile) throws IOException {
        if (bundleFile == null) {
            throw new NullPointerException("bundleFile");
        }
        PluginBundle pb = new PluginBundle(bundleFile);
        PluginRoot root = pb.getPluginRoot();
        return root.getCatalogID();
    }

    /**
     * Change a bundle's catalog id. This involves copying the bundle to a
     * temporary location, then writing over the original bundle but replacing
     * the <tt>eons-plugin</tt> root file to include the new UUID.
     *
     * @param bundleFile the file that contains a plug-in bundle
     * @param id the new ID
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if bundleFile is null
     */
    public static void setCatalogID(File bundleFile, CatalogID id) throws IOException {
        if (bundleFile == null) {
            throw new NullPointerException("bundleFile");
        }
        PluginBundle pb = new PluginBundle(bundleFile);
        PluginRoot root = pb.getPluginRoot();
        root.setCatalogID(id);
        root.updateBundle();
    }

    /**
     * A command line tool for creating and updating IDs. It accepts zero or
     * more arguments with the following form:
     * <pre>[--touch]  [--xfile] [--tfile] [n...] [existing ID...]</pre> Where:
     * <dl>
     * <dt>{@code --touch}</dt><dd>generate and print the date part of an ID
     * with current timestamp</dd>
     * <dt>{@code --xfile}</dt><dd>will extract and print the ID from plug-in
     * bundle 'file'</dd>
     * <dt>{@code --tfile}</dt><dd>will generate or touch the ID in plug-in
     * bundle 'file'</dd>
     * <dt>{@code n}</dt><dd>a series of one or more integers will generate a
     * group of that many IDs</dd>
     * <dt>{@code existing ID}</dt><dd>using an existing ID will touch the ID
     * (make the date current)</dd>
     * </dl>
     * Generated IDs and timestamps are printed to the output stream, unless
     * using the {@code --t} option (in which case the root file of the bundle
     * itself is modified).
     *
     * @param args the command line arguments for the tool
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("CatalogID: use --h for more options");
            System.out.println(new CatalogID());
        }

        try {
            int group = 0;
            for (int i = 0; i < args.length; ++i) {
                String arg = args[i];
                String lcArg = args[i].toLowerCase(Locale.CANADA);
                if (lcArg.equals("--h") || lcArg.equals("-?") || lcArg.equals("--help")) {
                    System.out.println("CatalogID [--touch]  [--xfile] [--tfile] [n...] [existing ID...]");
                    System.out.println("    --touch generate and print the date part of an ID with current timestamp");
                    System.out.println("    --xfile will extract and print the ID from plug-in bundle 'file'");
                    System.out.println("    --tfile will generate or touch the ID in plug-in bundle 'file'");
                    System.out.println("    a series of one or more integers will generate a group of that many IDs");
                    System.out.println("    using an existing ID will touch the ID (make the date current)");
                    continue;
                }
                if (lcArg.equals("--touch")) {
                    System.out.println(new CatalogID().toDateString());
                    continue;
                }
                if (lcArg.startsWith("--x")) {
                    CatalogID id = CatalogID.getCatalogID(new File(arg.substring(3)));
                    if (id == null) {
                        System.out.println("No ID found in bundle");
                    } else {
                        System.out.println(id);
                    }
                    continue;
                }
                if (lcArg.startsWith("--t")) {
                    File bundle = new File(arg.substring(3));
                    CatalogID id = CatalogID.getCatalogID(bundle);
                    if (id == null) {
                        id = new CatalogID();
                    }
                    CatalogID.setCatalogID(bundle, id);
                    System.out.println("Set ID to " + id);
                    continue;
                }
                try {
                    int num = Integer.parseInt(args[i]);
                    if (num > 0) {
                        if (group++ > 0) {
                            System.out.println();
                        }
                        for (int j = 0; j < num; ++j) {
                            System.out.println(new CatalogID());
                        }
                    }
                    continue;
                } catch (NumberFormatException e) {
                }
                CatalogID id = CatalogID.extractCatalogID(args[i]);
                if (id != null) {
                    System.out.println(new CatalogID(id));
                } else {
                    System.err.println("Unknown argument " + args[i]);
                    System.err.println("Use --h for help");
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Required argument missing; use --h for help");
        } catch (IOException e) {
            System.err.println("Error while manipulating ID in file");
            e.printStackTrace(System.err);
        }
    }
}
