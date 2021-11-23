package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import ca.cgjennings.apps.arkham.plugins.catalog.Listing;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.io.EscapedLineWriter;
import ca.cgjennings.io.EscapedTextCodec;
import ca.cgjennings.io.InvalidFileFormatException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import resources.Language;
import resources.Settings;

/**
 * Represents the information stored in a plug-in bundle's root file. This
 * includes the bundle's {@link CatalogID}, the plug-in objects contained in the
 * bundle, the bundle's startup priority, and other properties. If the bundle is
 * not stored in a plain format, creating a {@code PluginRoot} for the
 * bundle will convert it in place.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class PluginRoot implements Comparable<PluginRoot> {

    private PluginBundle bundle;
    private List<String> pluginIDs = new LinkedList<>();
    private List<String> errors = new LinkedList<>();
    private CatalogID id;
    private int pri = PRIORITY_DEFAULT;
    private String install;
    private String comment;

    private Settings client;

    /**
     * Read root file information from a plug-in bundle. Typically, this
     * constructor is not used directly. Instead, the root is obtained using
     * {@link PluginBundle#getPluginRoot()}, which caches the result.
     *
     * @param pb the bundle file that contains the root
     * @throws IOException if there is an I/O error while parsing the file
     * @throws NullPointerException if file is null
     */
    public PluginRoot(PluginBundle pb) throws IOException {
        if (pb == null) {
            throw new NullPointerException("bundle");
        }
        this.bundle = pb;

        if (bundle.getFormat() != PluginBundle.FORMAT_PLAIN) {
            // should not actually be possible since the bundle object was instantiated
            if (bundle.getFormat() == PluginBundle.FORMAT_INVALID) {
                throw new InvalidFileFormatException();
            }
            bundle.copy(bundle.getFile());
        }
        try (ZipFile jar = bundle.getZipFile()) {
            ZipEntry entry = jar.getEntry(ROOT_FILE);
            if (entry != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), TextEncoding.PLUGIN_ROOT_CS));
                parseStream(r);
            }
        }
    }

    /**
     * Read root file information directly from a root file, such as one stored
     * in a project folder.
     *
     * @param file the UTF-8 encoded text file that contains the root
     * @throws IOException if there is an I/O error while parsing the file
     * @throws NullPointerException if file is null
     */
    public PluginRoot(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        this.bundle = null;

        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), TextEncoding.PLUGIN_ROOT_CS))) {
            parseStream(r);
        }
    }

    /**
     * Creates a root file by parsing a string that uses the root file format.
     *
     * @param rootSettings the root file code
     * @throws NullPointerException if rootSettings is null
     */
    public PluginRoot(String rootSettings) {
        if (rootSettings == null) {
            throw new NullPointerException("rootSettings");
        }
        try {
            parseStream(new BufferedReader(new StringReader(rootSettings)));
        } catch (IOException e) {
            throw new AssertionError();
        }
    }

    private void parseStream(BufferedReader r) throws IOException {
        pluginIDs.clear();
        errors.clear();
        id = null;
        pri = PRIORITY_DEFAULT;
        install = null;
        comment = null;
        cachedString = null;

        int lineNum = 0;
        boolean keepingComments = true;
        StringBuilder commentLines = new StringBuilder();

        String line;
        while ((line = r.readLine()) != null) {
            ++lineNum;
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.charAt(0) == '#' || line.charAt(0) == '!') {
                if (id == null) {
                    id = CatalogID.extractCatalogID(line);
                    // (id will still be null if the comment doesn't contain a valid ID)
                    if (id != null) {
                        continue; // don't keep comment line with ID; will use id key instead
                    }
                }
                if (keepingComments) {
                    if (commentLines.length() > 0) {
                        commentLines.append('\n');
                    }
                    commentLines.append(line);
                }
                continue;
            }
            keepingComments = false;

            if (line.endsWith("\\")) {
                String nextLine;
                do {
                    nextLine = r.readLine();
                    if (nextLine == null) {
                        break;
                    }
                    nextLine = nextLine.trim();
                    if (nextLine.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
                        continue;
                    }
                    line = line.substring(0, line.length() - 1) + nextLine;
                } while (nextLine.endsWith("\\"));
            }

            String error = parseLine(line);
            if (error != null) {
                errors.add(String.valueOf(lineNum) + ": " + error);
            }
        }
        if (commentLines.length() == 0) {
            comment = null;
        } else {
            comment = commentLines.toString();
        }
    }

    private String parseLine(String line) {
        line = EscapedTextCodec.unescape(line);
        int eq = line.indexOf('=');
        if (eq >= 0) {
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();

            switch (key) {
                case KEY_ID:
                    id = CatalogID.extractCatalogID(value);
                    if (id == null) {
                        return "invalid id: " + value;
                    }   break;
                case KEY_PRIORITY:
                    if (!value.isEmpty()) {
                        if (Character.isJavaIdentifierStart(value.charAt(0))) {
                            value = value.toUpperCase(Locale.ENGLISH);
                            for (int i = 0; i < priority_keywords.length; ++i) {
                                if (priority_keywords[i].equals(value)) {
                                    pri = priority_map[i];
                                    break;
                                }
                            }
                        } else {
                            try {
                                pri = Integer.parseInt(value);
                            } catch (NumberFormatException e) {
                                return "invalid priority: " + value;
                            }
                        }
                    }   break;
                case KEY_INSTALL_SCRIPT:
                    install = normalizePluginIdentifier(value);
                    break;
                default:
                    putClientProperty(key, value);
                    break;
            }
        } else {
            try {
                line = normalizePluginIdentifier(line);
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
            for (String s : pluginIDs) {
                if (s.equals(line)) {
                    return "duplicate plug-in entry: " + line;
                }
            }
            pluginIDs.add(line);
        }
        return null;
    }

    /**
     * Converts a plug-in identifier into the normalized form expected by the
     * plug-in system. Scripts must start with "script:" followed by a "res:"
     * URL; classes must not start with "class:".
     *
     * <p>
     * <b>Note:</b> Identifiers returned by {@link #getPluginIdentifiers()} will
     * already be in normal form.
     *
     * <p>
     * The normalization process consists of the following:
     * <ol>
     * <li>Entries that end with ".js" and do not start with "class:" are
     * treated as scripts and will start with "script:" if they did not
     * previously.
     * <li>Entries that start with "res://" are treated as scripts and will
     * start with "script:" if they did not previously.
     * <li>All scripts will identify the script location by res:// URL.
     * <li>Class entries that start with "class:" will have this prefix removed.
     * <li>Class entries that end with ".class" or ".java" will have this suffix
     * removed.
     * </ol>
     *
     * @param pid the plug-in identifier (line from a root file that identifies
     * a plug-in)
     * @return the identifier in normal form
     * @throws IllegalArgumentException if the identifier is not valid
     */
    public static String normalizePluginIdentifier(String pid) {
        boolean isScript = pid.startsWith(SCRIPT_PREFIX);

        // add script: to .js entries if they don't already have it
        if (!isScript && pid.endsWith(".js") && !pid.startsWith(CLASS_PREFIX)) {
            pid = SCRIPT_PREFIX + pid;
            isScript = true;
        }

        // add script: to res:// entries if they don't already have it
        if (!isScript && pid.startsWith("res:")) {
            pid = SCRIPT_PREFIX + pid;
            isScript = true;
        }

        // convert script names to res:// URLs if they are not already
        if (isScript && !pid.startsWith("script:res:")) {
            pid = pid.substring(SCRIPT_PREFIX.length());
            if (pid.startsWith("resources/")) {
                pid = "script:res://" + pid.substring("resources/".length());
            } else {
                pid = "script:res:///" + pid;
            }
        }

        // classes: remove class: from start and check for valid chars
        if (!isScript) {
            if (pid.startsWith(CLASS_PREFIX)) {
                pid = pid.substring(CLASS_PREFIX.length());
            }
            if (pid.endsWith(".class")) {
                pid = pid.substring(0, pid.length() - ".class".length());
            } else if (pid.endsWith(".java")) {
                pid = pid.substring(0, pid.length() - ".java".length());
            }

            pid = pid.replace('/', '.');
            boolean start = true;
            for (int i = 0; i < pid.length(); ++i) {
                char c = pid.charAt(i);
                if (c == '.') {
                    start = true;
                    continue;
                }
                if (start) {
                    if (!Character.isJavaIdentifierStart(c)) {
                        throw new IllegalArgumentException("invalid character in class name: " + c);
                    }
                    start = false;
                } else {
                    if (!Character.isJavaIdentifierPart(c)) {
                        throw new IllegalArgumentException("invalid character in class name: " + c);
                    }
                }
            }
        }
        return pid;
    }

    /**
     * Converts a plug-in identifier into a short, human-friendly form. The
     * decorated format is used by this class to create externalized identifiers
     * (as with {@link #toString}). The decorated form is equivalent to the
     * normalized form except that scripts will not start with "script:". (This
     * is acceptable since in normal form they will start with "res://", which
     * is not valid in a class name.)
     *
     * @param pid the identifier
     * @return the decorated identifier
     * @throws IllegalArgumentException if the identifier is invalid
     * @see #normalizePluginIdentifier(java.lang.String)
     */
    public static String decoratePluginIdentifier(String pid) {
        return decorateImpl(normalizePluginIdentifier(pid));
    }

    /**
     * Internal implementation assumes pid already in normal form.
     */
    private static String decorateImpl(String pid) {
        if (pid.startsWith(SCRIPT_PREFIX)) {
            pid = pid.substring(SCRIPT_PREFIX.length());
        }
        return pid;
    }

    /**
     * Returns the plug-in bundle that this root belongs to, or
     * {@code null} if this root object was not created from a bundle.
     *
     * @return the bundle associated with this root
     */
    public PluginBundle getBundle() {
        return bundle;
    }

    /**
     * Returns the comment lines located at the top of the root file, if any.
     * The returned text will include the comment marker characters ('#' or
     * '!').
     *
     * @return the content of the comment block at the top of the file, or
     * {@code null}
     * @see #setComments(java.lang.String)
     */
    public String getComments() {
        return comment;
    }

    /**
     * Sets the text of a comment that will be placed at the top of the root
     * file. Any existing comment block will be replaced. Lines in the comment
     * string should be separated by a newline character. Any lines that do not
     * already start with a comment marker ('#' or '!') will cause an
     * {@code IllegalArgumentException} to be thrown.
     *
     * @param comments the comment text to set, or {@code null} to clear
     * the comment block
     * @see #getComments()
     */
    public void setComments(String comments) {
        if (comments == null) {
            if (comment != null) {
                comment = null;
                cachedString = null;
            }
        } else {
            // check that each line is really a comment
            for (String line : comments.split("\n", -1)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || (trimmed.charAt(0) != '#' && trimmed.charAt(0) != '!')) {
                    throw new IllegalArgumentException("line start missing comment token: " + comments);
                }
            }
            if (!comments.equals(comment)) {
                comment = comments;
                cachedString = null;
            }
        }
    }

    /**
     * Returns the bundle's load priority as an integer.
     *
     * @return the load priority value
     */
    public int getPriority() {
        return pri;
    }

    /**
     * Sets the bundle's load priority from an integer value. Bundles are loaded
     * in order of their priority values, from lowest to highest. That is,
     * setting this to a lower value gives the bundle a higher load priority. If
     * the priority value corresponds to one of the special named priorities
     * ("GAME", "EXPANSION, "HIGH", "NORMAL", "LOW"), then writing the root file
     * will write the value as the keyword rather than an integer string. If the
     * value corresponds to the default load priority, then the key will be
     * omitted altogether.
     *
     * @param pri the new priority value
     * @see #KEY_PRIORITY
     * @see #PRIORITY_GAME
     * @see #PRIORITY_EXPANSION
     * @see #PRIORITY_HIGH
     * @see #PRIORITY_NORMAL
     * @see #PRIORITY_LOW
     * @see #PRIORITY_DEFAULT
     */
    public void setPriority(int pri) {
        if (this.pri != pri) {
            this.pri = pri;
            cachedString = null;
        }
    }

    /**
     * Returns the catalog ID for this root file, or {@code null} if none
     * is defined.
     *
     * @return the ID for the bundle that this root file is (or will be) for
     */
    public CatalogID getCatalogID() {
        return id;
    }

    /**
     * Sets the catalog ID for this root file.
     *
     * @param id the new catalog ID for the root file's bundle
     * @see #getCatalogID()
     */
    public void setCatalogID(CatalogID id) {
        if (this.id != id) {
            this.id = id;
            cachedString = null;
        }
    }

    /**
     * Returns the identifier of the {@linkplain InstallationActions installer
     * class or script} for this root file.
     *
     * @return the installation script resource, or {@code null}
     * @see #setInstallerIdentifier(java.lang.String)
     */
    public String getInstallerIdentifier() {
        return install;
    }

    /**
     * Sets the identifier of the {@linkplain InstallationActions installer
     * class or script} to run during installation or uninstallation of the
     * associated plug-in bundle. Setting this to {@code null} will clear
     * the current installer, if any.
     *
     * @param install the installation script identifier
     * @see #getInstallerIdentifier()
     * @see #KEY_INSTALL_SCRIPT
     */
    public void setInstallerIdentifier(String install) {
        if (install == null && this.install == null) {
            return;
        }

        if (install != null) {
            install = normalizePluginIdentifier(install);
        }
        if (this.install == null || !this.install.equals(install)) {
            this.install = install;
            cachedString = null;
        }
    }

    /**
     * Returns an array of the compiled class and script identifiers that
     * represent plug-ins (or themes) stored in this bundle.
     *
     * @return an array of the stored plug-in identifiers
     */
    public String[] getPluginIdentifiers() {
        return pluginIDs.toArray(new String[pluginIDs.size()]);
    }

    /**
     * Replaces the existing plug-in identifiers with the identifiers in the
     * specified array.
     *
     * @param ids an array of plug-in identifiers
     * @throws NullPointerException if the array or any of its elements is
     * {@code null}
     */
    public void setPluginIdentifiers(String[] ids) {
        if (ids == null) {
            throw new NullPointerException("IDs");
        }

        boolean different = ids.length != pluginIDs.size();
        LinkedList<String> newIdList = new LinkedList<>();
        for (int i = 0; i < ids.length; ++i) {
            String id = ids[i];
            if (id == null) {
                throw new NullPointerException("IDs[ " + i + " ]");
            }
            id = normalizePluginIdentifier(id);
            newIdList.add(id);
            if (!different && !(id.equals(pluginIDs.get(i)))) {
                different = true;
            }
        }

        if (different) {
            pluginIDs = newIdList;
            cachedString = null;
        }
    }

    /**
     * Adds a new plug-in identifier to the list of identifiers. Does nothing if
     * the plug-in is already listed.
     *
     * @param id the identifier to add
     * @throws NullPointerException if the identifier is {@code null}
     */
    public void addPluginIdentifier(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        id = normalizePluginIdentifier(id);
        // given that these lists normally contain 1 or at most 2
        // entries, this is expected to be more time/space efficient than LinkedHashSet
        for (String s : pluginIDs) {
            if (s.equals(id)) {
                return;
            }
        }
        pluginIDs.add(id);
        cachedString = null;
    }

    /**
     * Removes a new plug-in identifier from the list of identifiers. Does
     * nothing if the plug-in is not listed.
     *
     * @param id the identifier to remove
     * @throws NullPointerException if the identifier is {@code null}
     */
    public void removePluginIdentifier(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        id = normalizePluginIdentifier(id);
        if (pluginIDs.remove(id)) {
            cachedString = null;
        }
    }

    /**
     * Sets the value of a client property, or removes the property if
     * {@code value} is {@code null}.
     *
     * @param key the name of the client key
     * @param value the value of the client key
     */
    public void putClientProperty(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (client == null) {
            client = new Settings();
        }

        if (value == null) {
            client.reset(key);
        } else {
            client.set(key, value);
        }
    }

    /**
     * Returns the value of a client property, or {@code null} if the
     * property is not defined.
     *
     * @param key the name of the client property
     * @return the value of the named property, or {@code null}
     */
    public String getClientProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (client == null) {
            return null;
        }

        return client.get(key);
    }

    /**
     * Returns the value of a client property, or {@code null} if the
     * property is not defined. The property will be localized for the
     * {@linkplain Language#getInterface() user interface language}, if
     * possible.
     *
     * @param key the name of the client property
     * @return the value of the named property, or {@code null}
     */
    public String getLocalizedClientProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        if (client == null) {
            return null;
        }

        return client.getLocalized(key, Language.getInterfaceLocale());
    }

    /**
     * Returns an immutable set of all client property keys that are defined at
     * the time the method is called.
     *
     * @return keys available as client properties
     */
    public Set<String> getClientPropertyKeys() {
        if (client == null) {
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableSet(client.getKeySet());
    }

    /**
     * If this root file contains any special property defining keys, returns a
     * set of these keys, otherwise returns {@code null}. In most cases,
     * this avoids having to create a copy of the client keys for the
     * {@code BundleInstaller}. Special definition keys start with a hyphen
     * followed by a capital letter. They are used like -D arguments on the
     * {@code java} command line. Keys that start with -D will define
     * system properties. Keys that start with -S will define temporary
     * settings.
     *
     * @return a set of keys matching the above description
     */
    List<String> getSpecialDefinitionKeys() {
        List<String> keys = null;
        if (client != null) {
            for (String k : client) {
                if (k.length() > 2 && k.charAt(0) == '-') {
                    switch (k.charAt(1)) {
                        case 'D':
                        case 'S':
                            if (keys == null) {
                                keys = new LinkedList<>();
                            }
                            keys.add(k);
                            break;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns an array of messages describing syntax errors in the file. The
     * returned array will contain one element for each error.
     *
     * @return a (possibly empty) array of error messages
     */
    public String[] getErrors() {
        return errors.toArray(new String[errors.size()]);
    }

    /**
     * Writes this plug-in root as a new archive file entry (with the name
     * <tt>eons-plugin</tt>) to an archive output stream.
     *
     * @param out the archive stream
     * @throws IOException if an I/O or Zip error occurs while writing the entry
     */
    public void writeToBundleArchive(ZipOutputStream out) throws IOException {
        out.putNextEntry(new ZipEntry(ROOT_FILE));
        out.write(toString().getBytes(TextEncoding.PLUGIN_ROOT_CS));
        out.closeEntry();
    }

    /**
     * Writes this plug-in root to a UTF-8 encoded text file.
     *
     * @param f the file to write to
     * @throws IOException if an I/O exception occurs
     */
    public void writeToFile(File f) throws IOException {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(toString().getBytes(TextEncoding.PLUGIN_ROOT_CS));
        }
    }

    /**
     * Updates the plug-in bundle that this plug-in was created from so that its
     * root file matches the current state of this root. This may be a lengthy
     * operation, as the bundle must first be copied and then the original file
     * overwritten.
     *
     * @throws IOException if an I/O or Zip error occurs while updating the
     * archive
     * @throws IllegalStateException if this root file was not created from a
     * bundle
     */
    public void updateBundle() throws IOException {
        if (bundle == null) {
            throw new IllegalStateException("PluginRoot did not come from a PluginBundle");
        }

        File bundleFile = bundle.getFile();
        File temp = File.createTempFile("se-update-bundle-", ".bundle");
        temp.deleteOnExit();
        bundle.copy(temp);
        JarFile source = null;
        ZipOutputStream dest = null;
        try {
            source = new JarFile(temp);
            // copy the files, substituting the new root file
            dest = ProjectUtilities.createPluginBundleArchive(bundleFile, true);

            writeToBundleArchive(dest);

            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zeIn = entries.nextElement();
                if (zeIn.getName().equals(ROOT_FILE)) {
                    continue;
                }

                ZipEntry zeOut = new ZipEntry(zeIn.getName());
                zeOut.setTime(zeIn.getTime());
                dest.putNextEntry(zeOut);
                ProjectUtilities.copyStream(source.getInputStream(zeIn), dest);
                dest.closeEntry();
            }
        } catch (IOException e) {
            // at this point the original bundle is stored in the temp file;
            // try to copy it back over to its starting location
            ProjectUtilities.copyFile(temp, bundleFile);
            throw e;
        } finally {
            if (dest != null) {
                dest.close();
            }
            if (source != null) {
                source.close();
            }
        }
        // try to delete the temp file if there were no errors
        // (if there were errors, file will exist until the app ends to allow
        // a chance for last-ditch manual recovery)
        temp.delete();
    }

    /**
     * Compares two roots based on their priorities. This implements a natural
     * sort order for plug-in roots that sorts them into their preferred order
     * for installation.
     *
     * @param root the plug-in root to compare this one with
     * @return a comparison value that compares the priorities of both
     * {@code PluginRoot}s
     */
    @Override
    public int compareTo(PluginRoot root) {
        return pri - root.pri;
    }

    /**
     * Returns a string that is equivalent to the code in the original root
     * file, though not necessarily identical. It may leave out some comments,
     * blank lines, or invalid lines, may store keys in a different order, and
     * may use more modern syntax where applicable.
     */
    @Override
    public String toString() {
        if (cachedString == null) {
            try {
                cachedString = toStringImpl();
            } catch (IOException e) {
                throw new AssertionError();
            }
        }
        return cachedString;
    }
    private String cachedString;

    private String toStringImpl() throws IOException {
        // Table of all client properties that we haven't written yet.
        // Anytime we want to write one, we remove it from this set.
        // Then we can write leftover keys we don't know about in blocks
        // at the end.
        Set<String> unwrittenKeys;
        if (client != null) {
            unwrittenKeys = new TreeSet<>(client.getKeySet());
        } else {
            unwrittenKeys = Collections.emptySet();
        }

        StringWriter sw = new StringWriter(1_024);
        EscapedLineWriter lw = new EscapedLineWriter(sw);
        lw.setUnicodeEscaped(false);

        // COMMENT BLOCK
        if (comment != null) {
            lw.write(comment);
            lw.write("\n\n");
        }

        // ID BLOCK
        if (id != null) {
            lw.writeProperty(KEY_ID, id.toString());
            lw.write('\n');
        }

        // CORE PROPERTY BLOCK
        int clientSize = unwrittenKeys.size();
        if (pri != PRIORITY_DEFAULT) {
            String priString = null;
            for (int i = 0; i < priority_map.length; ++i) {
                if (priority_map[i] == pri) {
                    priString = priority_keywords[i];
                    break;
                }
            }
            if (priString == null) {
                priString = String.valueOf(pri);
            }

            lw.writeProperty(KEY_PRIORITY, priString);
        }
        appendLocalizedClientKey(unwrittenKeys, CLIENT_KEY_NAME, lw);
        appendLocalizedClientKey(unwrittenKeys, CLIENT_KEY_DESCRIPTION, lw);
        appendClientKey(unwrittenKeys, CLIENT_KEY_IMAGE, lw);
        appendClientKey(unwrittenKeys, CLIENT_KEY_UI_LANGUAGES, lw);
        appendClientKey(unwrittenKeys, CLIENT_KEY_GAME_LANGUAGES, lw);
        if (install != null) {
            lw.writeProperty(KEY_INSTALL_SCRIPT, decorateImpl(install));
        }
        if (pri != PRIORITY_DEFAULT || install != null || clientSize != unwrittenKeys.size()) {
            lw.append('\n');
        }

        // LEFTOVER NON-CATALOG BLOCK
        LinkedList<String> leftovers = new LinkedList<>();
        for (String key : unwrittenKeys) {
            if (!key.startsWith("catalog-")) {
                leftovers.add(key);
            }
        }
        appendLeftovers(unwrittenKeys, leftovers, lw, true);

        // STANDARD CATALOG BLOCK
        clientSize = unwrittenKeys.size();
        if (client != null) {
            if (!client.get("catalog-" + Listing.HIDDEN, "no").equals("no")) {
                appendClientKey(unwrittenKeys, "catalog-" + Listing.HIDDEN, lw);
            }
        }
        appendClientKey(unwrittenKeys, "catalog-" + Listing.CORE, lw);
        appendLocalizedClientKey(unwrittenKeys, CLIENT_KEY_CATALOG_NAME, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.CREDIT, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.HOME_PAGE, lw);
        appendLocalizedClientKey(unwrittenKeys, CLIENT_KEY_CATALOG_DESCRIPTION, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.GAME, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.TAGS, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.VERSION, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.MAXIMUM_VERSION, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.MINIMUM_VERSION, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.REPLACES, lw);
        appendClientKey(unwrittenKeys, "catalog-" + Listing.REQUIRES, lw);

        if (clientSize != unwrittenKeys.size()) {
            lw.write('\n');
        }

        // LEFTOVER CATALOG BLOCK
        for (String key : unwrittenKeys) {
            leftovers.add(key);
        }
        appendLeftovers(unwrittenKeys, leftovers, lw, true);

        // PLUG-IN BLOCK
        for (int i = 0; i < pluginIDs.size(); ++i) {
            lw.writeLine(decorateImpl(pluginIDs.get(i)));
        }

        lw.flush();

        // trim any leftover newlines from the end of the text
        StringBuffer sb = sw.getBuffer();
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        // trim extraneous blank lines
        int nl = 0;
        for (int i = 0; i < sb.length(); ++i) {
            if (sb.charAt(i) == '\n') {
                if (++nl >= 3) {
                    sb.deleteCharAt(i);
                }
            } else {
                nl = 0;
            }
        }

        return sb.toString();
    }

    /**
     * Writes the specified key out, if present, and removes the key name from
     * unwrittenKeys.
     */
    private void appendClientKey(Set<String> unwrittenKeys, String keyName, EscapedLineWriter lw) {
        if (unwrittenKeys.contains(keyName)) {
            unwrittenKeys.remove(keyName);
            String v = client.get(keyName);
            if (v.isEmpty()) {
                return;
            }
            try {
                lw.writeProperty(keyName, v);
            } catch (IOException e) {
                StrangeEons.log.log(Level.SEVERE, null, e);
            }
        }
    }

    /**
     * Writes the specified localized key set out, if present, and removes the
     * key names from unwrittenKeys.
     */
    private void appendLocalizedClientKey(Set<String> unwrittenKeys, String keyName, EscapedLineWriter lw) {
        String locNamePrefix = keyName + '_';
        LinkedList<String> toWrite = null;
        for (String key : unwrittenKeys) {
            if (key.equals(keyName) || key.startsWith(locNamePrefix)) {
                if (toWrite == null) {
                    toWrite = new LinkedList<>();
                }
                toWrite.add(key);
            }
        }
        if (toWrite != null) {
            int size = toWrite.size();
            appendLeftovers(unwrittenKeys, toWrite, lw, false);
            if (size > 1) {
                try {
                    lw.write('\n');
                } catch (IOException e) {
                    StrangeEons.log.log(Level.SEVERE, null, e);
                }
            }
        }
    }

    private void appendLeftovers(Set<String> unwrittenKeys, List<String> keys, EscapedLineWriter lw, boolean appendNewline) {
        if (keys != null && keys.size() > 0) {
            for (String key : keys) {
                appendClientKey(unwrittenKeys, key, lw);
            }
            keys.clear();
            if (appendNewline) {
                try {
                    lw.write('\n');
                } catch (IOException e) {
                    StrangeEons.log.log(Level.SEVERE, null, e);
                }
            }
        }
    }

    /**
     * Returns {@code true} if and only if the specified object is a root
     * file with the same {@linkplain #toString() string representation}.
     *
     * @param obj the object to compare this object to
     * @return {@code true} if the object represents a root file with the
     * same properties
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PluginRoot other = (PluginRoot) obj;
        return (this == other) || (toString().equals(other.toString()));
    }

    /**
     * Returns a hash code for this root file.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * The name of the file that contains the plug-in root in a bundle file
     * ("eons-plugin").
     */
    public static final String ROOT_FILE = "eons-plugin";

    /**
     * The prefix that may explicitly prefix a scripted plug-in entry. Present
     * in {@linkplain #normalizePluginIdentifier(java.lang.String) normal form}.
     */
    public static final String SCRIPT_PREFIX = "script:";
    /**
     * The prefix that may explicitly prefix a compiled plug-in entry. Not
     * present in {@linkplain #normalizePluginIdentifier(java.lang.String)
     * normal form}.
     */
    public static final String CLASS_PREFIX = "class:";

    /**
     * The property key in the root file that describes the catalog ID.
     */
    public static final String KEY_ID = "id";
    // Note: for backwards compatibility, the ID may also be located in the file's comments.

    /**
     * The property key that defines the bundle's load priority. When a group of
     * plug-ins is discovered, the priority values will be used to determine the
     * order that the plug-ins are installed in. This is mainly useful for
     * extensions, where an extension that installs additional material for a
     * game might rely on the game plug-in already being installed. The priority
     * is also used to group plug-ins in the
     * <b>Toolbox</b> menu.
     *
     * @see #setPriority(int)
     */
    public static final String KEY_PRIORITY = "priority";

    /**
     * Property key that identifies a script to run after installing and before
     * uninstalling the plug-in bundle.
     *
     * @see InstallationActions
     */
    public static final String KEY_INSTALL_SCRIPT = "installer";

    /**
     * A predefined priority value used when the value of the priority key is
     * the string "GAME". Equivalent to the value 100.
     */
    public static final int PRIORITY_GAME = 100;
    /**
     * A predefined priority value used when the value of the priority key is
     * the string "EXPANSION". Equivalent to the value 500.
     */
    public static final int PRIORITY_EXPANSION = 500;
    /**
     * A predefined priority value used when the value of the priority key is
     * the string "HIGH". Equivalent to the value 1000.
     */
    public static final int PRIORITY_HIGH = 1_000;
    /**
     * A predefined priority value used when the value of the priority key is
     * the string "NORMAL". Equivalent to the value 5000.
     */
    public static final int PRIORITY_NORMAL = 5_000;
    /**
     * A predefined priority value used when the value of the priority key is
     * the string "LOW". Equivalent to the value 10000.
     */
    public static final int PRIORITY_LOW = 10_000;

    /**
     * The default priority used when no value is specified in the root file.
     */
    public static final int PRIORITY_DEFAULT = PRIORITY_NORMAL;

    private static final String[] priority_keywords = new String[]{
        "HIGH", "GAME", "EXPANSION", "NORMAL", "LOW"
    };
    private static final int[] priority_map = new int[]{
        PRIORITY_HIGH, PRIORITY_GAME, PRIORITY_EXPANSION,
        PRIORITY_NORMAL, PRIORITY_LOW
    };

    /**
     * The name of the plug-in bundle. The name returned by the plug-in(s) takes
     * precedence, so this is useful mainly for library bundles.
     */
    public static final String CLIENT_KEY_NAME = "name";
    /**
     * The description of the plug-in bundle. The description returned by the
     * plug-in(s) takes precedence, so this is useful mainly for library
     * bundles.
     */
    public static final String CLIENT_KEY_DESCRIPTION = "description";
    /**
     * Resource URL string of a representative image for the bundle. Used for
     * library bundles, since they contain no plug-ins.
     */
    public static final String CLIENT_KEY_IMAGE = "image";
    /**
     * A comma-separated list of interface locales supported by the plug-in.
     */
    public static final String CLIENT_KEY_UI_LANGUAGES = "ui-languages";
    /**
     * A comma-separated list of game locales supported by the plug-in.
     */
    public static final String CLIENT_KEY_GAME_LANGUAGES = "game-languages";
    /**
     * The plug-in name that will be shown in the catalogue if a
     * catalogue listing is extracted from this root file.
     */
    public static final String CLIENT_KEY_CATALOG_NAME = "catalog-name";
    /**
     * The plug-in description that will be shown in the catalogue if a
     * catalogue listing is extracted from this root file.
     */
    public static final String CLIENT_KEY_CATALOG_DESCRIPTION = "catalog-description";
}
