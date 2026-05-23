package gamedata;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.NewEditorDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.StrangeEonsEditor;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.plugins.debugging.Client;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import ca.cgjennings.ui.IconProvider;
import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import gamedata.ClassMap.Entry;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.UIManager;
import resources.CoreComponents.MissingCoreComponentException;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Represents the entries listed in the
 * {@linkplain ca.cgjennings.apps.arkham.NewEditorDialog New Editor dialog} and
 * maps those entries to their respective component classes (or scripts).
 * Primarily, this class is used by extensions to register new kinds of game
 * components {@linkplain #add from a class map file}. This class can also be
 * used to examine class map file entries programmatically.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ClassMap implements Iterable<Entry> {

    /**
     * Creates a new class map that contains entries parsed from the specified
     * class map resources.
     *
     * @param resources the class map resources to parse; if no resources are
     * specified, the default class map files are used
     * @throws ResourceParserException if any of the class map files cannot be
     * parsed
     */
    public ClassMap(String... resources) throws IOException {
        if (resources == null || resources.length == 0) {
            resources = getClassMapFiles();
        }
        for (String file : resources) {
            try (Parser parser = new Parser(file, false)) {
                Entry entry;
                while ((entry = parser.next()) != null) {
                    if (entry.getType() != EntryType.CATEGORY) {
                        TreeSet<Entry> set = catMap.get(entry.getCategory());
                        if (set == null) {
                            set = new TreeSet<>();
                            catMap.put(entry.getCategory(), set);
                        }
                        set.add(entry);
                    }
                }
            }
        }

        immutableCategories = Collections.unmodifiableSet(catMap.keySet());
    }

    private TreeMap<Entry, TreeSet<Entry>> catMap = new TreeMap<>();
    private Set<Entry> immutableCategories;

    /**
     * Returns an iterator that iterates over the categories contained in this
     * class map.
     *
     * @return an iterator over the available categories
     */
    @Override
    public Iterator<Entry> iterator() {
        return immutableCategories.iterator();
    }

    /**
     * Returns an immutable set of the categories contained in this class map.
     *
     * @return an unmodifiable category set
     */
    public Set<Entry> getCategories() {
        return immutableCategories;
    }

    /**
     * Returns an immutable set of the entries associated with a particular
     * category.
     *
     * @param category the category to obtain a set of entries for
     * @return the entries in the requested category, or an empty set
     */
    public Set<Entry> getCategoryEntries(Entry category) {
        TreeSet<Entry> set = catMap.get(category);
        if (set == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(set);
        }
    }

    /**
     * Returns all non-category entries as a set.
     *
     * @return a set of all class map entries, excluding category entries
     */
    public Set<Entry> getEntries() {
        TreeSet<Entry> set = new TreeSet<>();
        for (Entry cat : immutableCategories) {
            for (Entry entry : getCategoryEntries(cat)) {
                set.add(entry);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Adds the specified class map resource to the list of such files that is
     * used to generate the contents of the New Editor dialog. The sample class
     * map (<tt>/resources/projects/new-classmap.txt</tt>) describes the format
     * of these files.
     *
     * @param resource a relative URL within <tt>resources/</tt> that points to
     * the file to add
     * @see #getClassMapFiles()
     */
    public static void add(String resource) {
        Lock.test();
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        classMapFiles.add(resource);
    }

    /**
     * Returns an array of the resource files that are used to build the list of
     * categories and editors in the New Component dialog.
     *
     * @return an array of the resource URLs of all currently registered class
     * maps
     */
    public static String[] getClassMapFiles() {
        return classMapFiles.toArray(String[]::new);
    }

    private static final LinkedHashSet<String> classMapFiles = new LinkedHashSet<>();

    /**
     * An enumeration of the types of entries that may appear in a class map
     * file.
     */
    public static enum EntryType {
        /**
         * An entry for a category that groups entries of other types.
         * Categories cannot be nested.
         */
        CATEGORY,
        /**
         * An entry for an editor that is created by instantiating a compiled
         * class.
         *
         * @see GameComponent
         * @see AbstractGameComponentEditor
         */
        EDITOR_COMPILED,
        /**
         * An entry for an editor that is created from a DIY component script.
         *
         * @see DIY
         */
        EDITOR_DIY,
        /**
         * An entry for an editor that is created by running an arbitrary script
         * file. It is up to the script file to add the new editor to the
         * application. (If the script completes without adding an editor, it is
         * assumed that the script failed with an error and that the error has
         * already been reported to the user.)
         *
         * @see StrangeEonsAppWindow#addEditor
         */
        EDITOR_SCRIPTED,
    }

    /**
     * An entry in a class map file.
     */
    public static final class Entry implements Comparable<Entry>, IconProvider {

        private String key;
        private String entry;
        private String className;
        private Icon icon;
        private Icon banner;
        private Game game;
        private Entry category;

        private Entry(Parser p, String key) {
            this.key = key;
            if (!key.isEmpty() && key.charAt(0) == '@') {
                Language ll = p == null ? Language.getInterface() : p.getLanguage();
                entry = ll.get(key.substring(1).replace('_', '-')).replace("&", "");
            } else {
                entry = key;
            }
        }

        /**
         * Returns the localized entry name. This is the key from the original
         * entry, or the localized string obtained from the key if the key
         * starts with '@'.
         *
         * @return the localized entry name
         * @see #getKey()
         */
        public String getName() {
            return entry;
        }

        /**
         * Returns the original entry name. This is the unlocalized key that
         * defined the entry.
         *
         * @return the original entry key
         * @see #getName()
         */
        public String getKey() {
            return key;
        }

        /**
         * Returns this entry's icon. This method never returns {@code null}.
         *
         * @return the icon for the entry
         */
        @Override
        public Icon getIcon() {
            if (icon == null) {
                switch (getType()) {
                    case CATEGORY:
                        return DEFAULT_CATEGORY_ICON;
                    case EDITOR_SCRIPTED:
                        return DEFAULT_SCRIPTED_ICON;
                    default:
                        return DEFAULT_ITEM_ICON;
                }
            }
            return icon;
        }

        /**
         * Returns the custom category banner to display for this item, or
         * {@code null} if the category should use the default banner.
         *
         * @return the custom category banner, or {@code null}
         */
        public Icon getBanner() {
            return banner;
        }

        /**
         * Returns the class name or script resource description that this entry
         * maps to. If the entry represents a category, the mapping will be
         * {@code null}.
         *
         * @return the class mapped to by this entry, or {@code null}
         */
        public String getMapping() {
            return className;
        }

        /**
         * Returns the category that this entry belongs to.
         *
         * @return this entry's category, or {@code null} if this entry
         * <i>is</i> a category
         */
        public Entry getCategory() {
            return category;
        }

        /**
         * Returns the type of entry that this represents.
         *
         * @return the type of the entry
         */
        public EntryType getType() {
            if (className == null) {
                return EntryType.CATEGORY;
            }
            if (className.startsWith("diy:")) {
                return EntryType.EDITOR_DIY;
            }
            if (className.startsWith("script:")) {
                return EntryType.EDITOR_SCRIPTED;
            }
            return EntryType.EDITOR_COMPILED;
        }

        /**
         * Returns the game that this entry is associated with. Class map files
         * designed for older versions of the application do not define the game
         * for an entry. Entries created from such class maps will return
         * {@code null}.
         *
         * @return the game associated with this entry, or {@code null}
         */
        public Game getGame() {
            return game;
        }

        /**
         * Compares this entry to another entry. Returns an integer less than,
         * equal to or greater than zero depending on whether this entry should
         * fall before, in the same position as, or after the other entry in a
         * sorted list.
         *
         * @param rhs the target entry to compare this with
         * @return returns a negative, zero, or positive value as this entry is
         * less than, equal to, or greater than the target entry
         */
        @Override
        public int compareTo(Entry rhs) {
            int cmp = 0;
            if (this.game != null && rhs.game != null) {
                cmp = this.game.compareTo(rhs.game);
            }
            if (cmp == 0) {
                cmp = Language.getInterface().getCollator().compare(this.entry, rhs.entry);
                if (cmp == 0 && className != null && rhs.className != null) {
                    cmp = this.className.compareTo(rhs.className);
                }
            }
            return cmp;
        }

        /**
         * Returns {@code true} if the specified object is an entry with the
         * same mapping.
         *
         * @param rhs the object to compare this with
         * @return {@code true} if the target object has the same class map
         * value
         */
        @Override
        public boolean equals(Object rhs) {
            if (rhs == null || !(rhs instanceof Entry)) {
                return false;
            }
            Entry c = (Entry) rhs;
            if (this.className == null) {
                if (c.className != null) {
                    return false;
                }
                return compareTo(c) == 0;
            }
            if (c.className == null) {
                return false;
            }
            return this.className.equals(c.className) && compareTo(c) == 0;
        }

        @Override
        public int hashCode() {
            return entry.hashCode() ^ (className == null ? 7 : className.hashCode());
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder(40);
            b.append("ClassMap.Entry{");
            if (category != null) {
                b.append(category.entry).append('/');
            }
            b.append(entry).append('}');
            return b.toString();
        }

        private void fillInIcons(String iconURL) {
            if (iconURL != null) {
                iconURL = iconURL.trim();
                int semi = iconURL.indexOf(';');
                if (semi >= 0) {
                    try {
                        String bannerURL = iconURL.substring(semi + 1).trim();
                        iconURL = iconURL.substring(0, semi).trim();                    
                        ThemedIcon bannerIcon = ResourceKit.createBleedBanner(bannerURL);
                        if (bannerIcon.getIconWidth() != BANNER_WIDTH || bannerIcon.getIconHeight() != BANNER_HEIGHT) {
                            bannerIcon = bannerIcon.derive(BANNER_WIDTH, BANNER_HEIGHT);
                            StrangeEons.log.log(Level.WARNING, "resizing banner to standard " + BANNER_WIDTH + 'x' + BANNER_HEIGHT +  " size: " + bannerURL);
                        }
                        banner = bannerIcon;
                    } catch (Exception e) {
                        StrangeEons.log.log(Level.WARNING, "error reading category banner: " + key, e);
                    }
                }
                if (!iconURL.isEmpty()) {
                    icon = new ThemedImageIcon(iconURL, true);
                } else {
                    iconURL = null;
                }
            }

            if (iconURL == null) {
                iconURL = "editors/" + (key.startsWith("@") ? key.substring(1) : key) + ".png";
            }
            if (!iconURL.isEmpty() && resources.ResourceKit.class.getResource(iconURL) != null) {
                icon = new ThemedImageIcon(iconURL, true);
            }
        }

        /**
         * Creates an editor for this entry, adding it to the application
         * window.
         *
         * @return the new editor, or {@code null} if creation failed
         */
        public StrangeEonsEditor createEditor() {
            return createEditor(false);
        }

        /**
         * Creates an editor for this entry, adding it to the application
         * window.
         *
         * <p>
         * This version of the method allows you to create scripted components
         * in debug mode. In debug mode, a script breakpoint will be triggered
         * at the start of the component creation. Note that this will
         * effectively freeze the application until execution is resumed from an
         * attached {@linkplain Client script debugging client}. Creating a
         * component in debug mode has no effect is script debugging is not
         * enabled, or if the component is created from compiled code rather
         * than a script.
         *
         * @param debug if {@code true}, component types that use scripts will
         * be created in debug mode
         * @return the new editor, or {@code null} if creation failed
         * @throws UnsupportedOperationException if called on a category entry
         */
        public StrangeEonsEditor createEditor(boolean debug) {
            try {
                switch (getType()) {
                    case CATEGORY:
                        throw new UnsupportedOperationException("cannot create an editor for a category");
                    case EDITOR_COMPILED:
                        return createClassBasedItem();
                    case EDITOR_SCRIPTED:
                        return createScriptBasedItem(debug);
                    case EDITOR_DIY:
                        return createDIYBasedItem(debug);
                }
                throw new AssertionError();
            } catch (MissingCoreComponentException e) {
                ErrorDialog.displayError(string("core-info"), null);
                return null;
            } catch (InstantiationException ie) {
                UIManager.getLookAndFeel().provideErrorFeedback(null);
                StrangeEons.log.log(Level.SEVERE, "failed to instantiate editor: " + className, ie);
                ErrorDialog.displayError(className, ie);
                return null;
            }
        }

        private StrangeEonsEditor createScriptBasedItem(boolean debug) throws InstantiationException {
            final StrangeEonsAppWindow app = StrangeEons.getWindow();
            final StrangeEonsEditor oldEd = app.getActiveEditor();
            boolean ok = ScriptMonkey.runResourceScript(className, debug);
            if (ok) {
                StrangeEonsEditor newEd = app.getActiveEditor();
                if (oldEd != newEd && newEd.getGameComponent() != null) {
                    if (game != null) {
                        GameComponent gc = newEd.getGameComponent();
                        if (gc != null) {
                            gc.getSettings().set(Game.GAME_SETTING_KEY, game.getCode());
                        }
                        if (newEd.getFrameIcon() == AbstractGameComponentEditor.DEFAULT_EDITOR_ICON) {
                            newEd.setFrameIcon(getIcon());
                        }
                    }
                }
                return newEd;
            }
            throw new InstantiationException("error running resource script " + className);
        }

        private StrangeEonsEditor createDIYBasedItem(boolean debug) throws InstantiationException {
            GameComponent gc = null;
            try {
                gc = new DIY(className.substring("diy:".length()), game == null ? null : game.getCode(), debug);
            } catch (Exception e) {
                InstantiationError ie = new InstantiationError("error running DIY script " + className);
                ie.initCause(e);
                throw ie;
            }
            AbstractGameComponentEditor<?> editor = gc.createDefaultEditor();
            if (editor.getFrameIcon() == AbstractGameComponentEditor.DEFAULT_EDITOR_ICON) {
                editor.setFrameIcon(getIcon());
            }
            StrangeEons.getWindow().addEditor(editor);
            return editor;
        }

        private StrangeEonsEditor createClassBasedItem() throws InstantiationException {
            GameComponent gc = null;
            try {
                gc = (GameComponent) Class.forName(className).getConstructor().newInstance();
                if (game != null) {
                    gc.getSettings().set(Game.GAME_SETTING_KEY, game.getCode());
                }
            } catch (ExceptionInInitializerError ite) {
                if (ite.getCause() instanceof MissingCoreComponentException) {
                    throw (MissingCoreComponentException) ite.getCause();
                }
                InstantiationException ie = new InstantiationException("error instantiating component class " + className);
                ie.initCause(ite);
                throw ie;
            } catch (Exception e) {
                InstantiationException ie = new InstantiationException("error creating component " + className);
                ie.initCause(e);
                throw ie;
            }
            AbstractGameComponentEditor<?> editor = gc.createDefaultEditor();
            if (editor.getFrameIcon() == AbstractGameComponentEditor.DEFAULT_EDITOR_ICON) {
                editor.setFrameIcon(getIcon());
            }
            StrangeEons.getWindow().addEditor(editor);
            return editor;
        }
    }

    /**
     * The special "Everything" pseudocategory. The {@link NewEditorDialog}
     * generates the contents of this category automatically. It is not a member
     * of the set returned by {@link #getCategories()}.
     */
    public static final Entry ENTRY_EVERYTHING_CATEGORY = new Entry(null, "@cat-everything");

    /**
     * A parser for class map files.
     */
    public static final class Parser extends ResourceParser<Entry> {

        private Entry category;

        /**
         * Creates a parser for the specified resource file.
         *
         * @param resource the location of the desired tile set resource
         * @param gentle if {@code true}, parses in gentle mode
         */
        public Parser(String resource, boolean gentle) throws IOException {
            super(resource, gentle);
        }

        /**
         * Creates a parser for the specified input stream.
         *
         * @param in the input stream to read from
         * @param gentle if {@code true}, parses in gentle mode
         * @throws IOException if an I/O error occurs
         */
        public Parser(InputStream in, boolean gentle) throws IOException {
            super(in, gentle);
        }

        /**
         * Returns the next entry in the class map, or {@code null} if the last
         * entry has been reached.
         *
         * @return the next class map entry, or {@code null}
         */
        @Override
        public Entry next() throws IOException {
            String[] entry = readProperty();
            if (entry == null) {
                return null;
            }

            Entry e;

            // category entry
            if (entry[1].isEmpty()) {
                String[] fields = parseFields(entry[0]);
                e = new Entry(this, fields[0]);
                e.fillInIcons(fields[1]);
            } // item entry
            else {
                e = new Entry(this, entry[0]);
                String[] fields = parseFields(entry[1]);
                e.category = category;
                e.className = fields[0];
                e.fillInIcons(fields[1]);
                if (fields[2] != null) {
                    e.game = Game.get(fields[2]);
                }

                if (e.getType() == EntryType.EDITOR_COMPILED) {
                    if (e.className.indexOf('.') < 0) {
                        e.className = "ca.cgjennings.apps.arkham.component." + e.className;
                    }
                } else {
                    try {
                        ScriptDebugging.preprocessScript(e.className);
                    } catch (Exception ex) {
                        StrangeEons.log.log(Level.WARNING, null, ex);
                    }
                }
            }

            // track active category for subsequent entries
            if (e.getType() == EntryType.CATEGORY) {
                Entry existing = categories.get(e.getKey());
                if (existing == null) {
                    categories.put(e.getKey(), e);
                } else {
                    e = existing;
                }
                category = e;
            } else if (category == null) {
                error(string("rk-err-parse-classmap"));
                return next();
            }

            return e;
        }

        /**
         * Splits a string into up to three segments by breaking on pipes (|).
         * If a segment is missing, its value will be {@code null}.
         */
        private String[] parseFields(String source) {
            final String[] fields = {null, null, null};
            int spos = 0;
            for (int i = 0; i < fields.length; ++i) {
                int epos = source.indexOf('|', spos);
                if (epos < 0) {
                    fields[i] = source.trim();
                    break;
                }
                fields[i] = source.substring(spos, epos).trim();
                source = source.substring(epos + 1);
            }

            if (fields[fields.length - 1] != null && fields[fields.length - 1].indexOf('|') >= 0) {
                warning("extra fields in class map entry");
            }

            return fields;
        }
    }

    private static final HashMap<String, Entry> categories = new HashMap<>();
    private static final int BANNER_WIDTH = 117;
    private static final int BANNER_HEIGHT = 362;
    private static final ThemedIcon DEFAULT_CATEGORY_ICON = ResourceKit.getIcon("res://editors/category.png");
    private static final ThemedIcon DEFAULT_ITEM_ICON = ResourceKit.getIcon("res://editors/missing.png");
    private static final ThemedIcon DEFAULT_SCRIPTED_ICON = ResourceKit.getIcon("res://editors/script.png");
}
