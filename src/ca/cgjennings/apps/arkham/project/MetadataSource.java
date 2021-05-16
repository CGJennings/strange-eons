package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.Length;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.ComponentMetadata;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.plugins.PluginBundle;
import ca.cgjennings.apps.arkham.sheet.PrintDimensions;
import ca.cgjennings.apps.arkham.sheet.RenderTarget;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.spelling.dict.TernaryTreeList;
import ca.cgjennings.spelling.dict.TernaryTreeList.TTLInfo;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * An object that can determine metadata for one or more file types. Note that
 * if {@link #appliesTo} returns {@code false} for a given member, then the
 * result of passing that member to any other method is undefined.
 *
 * <p>
 * A typical metadata source is shared between many different members, and may
 * even represent more than one type of file. For this reason, methods that
 * query metadata require you to pass in the specific member for which the data
 * is desired.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class MetadataSource {

    private static final DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private static final DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    public MetadataSource() {
    }

    /**
     * Returns {@code true} if this source is intended to provide metadata
     * for a given project member. The base class returns {@code true} for
     * anything, as it provides the fallback implementation for unspecialized
     * file types.
     *
     * @param m the member to check for applicability
     * @return {@code true} if this source can provide metadata about the
     * specified member, otherwise {@code false}
     */
    public boolean appliesTo(Member m) {
        return true;
    }

    /**
     * After locating the correct source for a given member, a specific instance
     * is requested by calling this method. The base class simply returns
     * {@code this}, which shares the source with all members that it
     * applies to. Subclasses may return an instance that is unique to a
     * particular member. For example, they might supply a unique icon that is a
     * thumbnail version of the member.
     *
     * @param m the member to request a more specific source for
     * @return a source that provides the most specific available data for
     * {@code m}, possibly {@code this}
     * @throws IllegalArgumentException if this source does not apply to
     * {@code m}
     */
    public MetadataSource getSpecificInstanceFor(Member m) {
        if (!appliesTo(m)) {
            throw new IllegalArgumentException("this source does not apply to the given member");
        }
        return this;
    }

    /**
     * Returns a short description of the type of file represented by the
     * specified member.
     *
     * @param m the member to fetch a description of
     * @return a short description of {@code m}, typically 2-3 words
     */
    public String getDescription(Member m) {
        String type = null;
        String extension = m.getExtension();
        if (m instanceof Project) {
            if (((Project) m).getPackageFile() != null) {
                type = string("prj-prop-project-pkg");
            } else {
                type = string("prj-prop-project");
            }
        } else if (m instanceof Task) {
            type = string("prj-prop-task");
        } else if (m.isFolder()) {
            type = string("prj-prop-folder");
        } else if (extension.isEmpty() && m.getFile().getName().equals("eons-plugin")) {
            type = string("prj-prop-root");
        } else {
            if (TYPE_DESCS.isEmpty()) {
                initTypes();
            }
            type = TYPE_DESCS.get(extension);
            if (type == null) {
                type = fsv.getSystemTypeDescription(m.getFile());
            }
            if (type == null) {
                type = extension;
            }
        }
        return type;
    }

    private static void initTypes() {
        addTypes(
                "cardlayout", string("cle-file-desc"),
                "class", string("prj-prop-class"),
                "csv", string("prj-prop-csv"),
                "eon", string("prj-prop-eon"),
                "java", string("prj-prop-java"),
                "jp2", string("prj-prop-image", "JPEG2000"),
                "jpg", string("prj-prop-image", "JPEG"),
                "jpeg", string("prj-prop-image", "JPEG"),
                "png", string("prj-prop-image", "PNG"),
                "svg", string("prj-prop-image", "SVG"),
                "svgz", string("prj-prop-image", "SVG"),
                "js", string("prj-prop-script"),
                "ajs", string("prj-prop-ascript"),
                "properties", string("prj-prop-props"),
                "collection", string("prj-prop-collection"),
                "seext", string("prj-prop-bundle"),
                "selibrary", string("prj-prop-lib"),
                "seplugin", string("prj-prop-bundle"),
                "setheme", string("prj-prop-theme"),
                "pbz", string("prj-prop-packed-bundle"),
                "pgz", string("prj-prop-packed-bundle"),
                "plzm", string("prj-prop-packed-bundle"),
                "txt", string("prj-prop-txt"),
                "utf8", string("prj-prop-utf8"),
                "classmap", string("prj-prop-class-map"),
                "silhouettes", string("prj-prop-sil"),
                "tiles", string("prj-prop-tiles"),
                "css", string("prj-prop-css"),
                "cpl", string("prj-prop-dict-cpl"),
                "3tree", string("prj-prop-dict-tst"),
                "idx", string("prj-prop-idx")
        );
    }

    private static void addTypes(String... val) {
        if ((val.length & 1) != 0) {
            throw new AssertionError();
        }

        for (int i = 0; i < val.length; i += 2) {
            TYPE_DESCS.put(val[i], val[i + 1]);
        }
    }
    private static final HashMap<String, String> TYPE_DESCS = new HashMap<>();

    /**
     * Return an icon that is appropriate for a particular member. Override this
     * to provide a suitable icon for the file type.
     *
     * <p>
     * The base implementation provides built-in icons for certain common file
     * types, and otherwise attempts to fetch the system icon for the file type.
     *
     * @param m the member to locate an icon for
     * @return an icon appropriate for {@code m}
     */
    public Icon getIcon(Member m) {
        if (!m.getFile().exists()) {
            return ICON_FILE;
        }
        if (m.isFolder()) {
            if (m instanceof Task) {
                if (m instanceof Project) {
                    return ICON_PROJECT;
                }
                Settings s = ((Task) m).getSettings();
                return getDefaultTaskIcon(s.get(Task.KEY_TYPE), s.get(Task.KEY_ICON));
            }
            return ICON_FOLDER;
        }
        String extension = m.getExtension();
        if (extension.length() == 0 && m.getFile().getName().equals("eons-plugin")) {
            return ICON_PLUGIN_ROOT;
        }
        if (DEFAULT_ICONS.isEmpty()) {
            initDefaultIcons();
        }
        Icon i = DEFAULT_ICONS.get(extension);
        if (i != null) {
            return i;
        }
        return getSystemIcon(m);
    }

    /**
     * Publishes metadata to a {@link PropertyConsumer}. This method may return
     * before all available data has been published and continue to add new data
     * as it becomes available. Once all of the available data has been
     * published, if the consumer is still valid, its
     * {@link PropertyConsumer#doneAddingProperties()} method will be called.
     *
     * <p>
     * The base class implementation calls {@link #fillInMetadataImpl} and then
     * calls the consumer's {@link PropertyConsumer#doneAddingProperties()}
     * method. This will fill in the member's file type description and basic
     * file-based metadata.
     *
     * <p>
     * <b>Note:</b> If you want to create a metadata source that adds some
     * properties from another thread (because they take time to provide), use
     * {@link ThreadedMetadataSource} as your base class.
     *
     * @param pc the entity that should receive property data
     */
    public void fillInMetadata(Member m, PropertyConsumer pc) {
        fillInMetadataImpl(m, pc);
        pc.doneAddingProperties();
    }

    /**
     * Called to publish metadata to a consumer. The base class will publish
     * basic metadata including the file type description. Classes that override
     * the default implementation should call the super implementation first to
     * fill in this basic information.
     *
     * @param m the member to publish metadata about
     * @param pc the consumer to publish metadata to
     */
    protected void fillInMetadataImpl(Member m, PropertyConsumer pc) {
        File f = m.getFile();
        if (f.exists()) {
            add(pc, m, "prj-prop-name", f.getName());
            add(pc, m, "prj-prop-type", getDescription(m));
            try {
                Path p = m.getFile().toPath();
                BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                pc.addProperty(m, string("prj-prop-size"), m.isFolder() ? "" : ProjectUtilities.formatByteSize(attr.size()));
                final Date modtime = new Date(attr.lastModifiedTime().toMillis());
                pc.addProperty(m, string("prj-prop-modtime"), dateFormatter.format(modtime) + "  " + timeFormatter.format(modtime));
                if (attr.isSymbolicLink()) {
                    Path target = Files.readSymbolicLink(p);
                    pc.addProperty(m, string("prj-prop-symlink"), target.toString());
                }
            } catch (Throwable t) {
            }

            // if this looks like a localized file, append language information
            Language.LocalizedFileName lfn = new Language.LocalizedFileName(f);
            if (lfn.localeDescription != null) {
                Locale locale = lfn.getLocale();
                String localeDescription;
                if (!locale.getCountry().isEmpty()) {
                    localeDescription = locale.getDisplayLanguage() + '\u2014' + locale.getDisplayCountry();
                } else {
                    localeDescription = locale.getDisplayLanguage();
                }
                add(pc, m, "locale", localeDescription);
                pc.addProperty(m, "", Language.getIconForLocale(locale));
            }

            // Projects do not have a separate metadata source;
            //   it is a bit of a hack to append this here
            if (m.getClass().equals(Project.class)) {
                Project p = (Project) m;
                String loc;
                if (p.getPackageFile() != null) {
                    loc = p.getPackageFile().getPath();
                } else {
                    loc = p.getFile().getParent();
                }
                add(pc, m, "prj-prop-location", loc);
            }
        }
    }

    /**
     * Adds a new, localized property to a consumer. A convenience method to
     * help subclasses write {@link #fillInMetadataImpl} implementations.
     *
     * @param pc the consumer to write to
     * @param m the member that the metadata applies to
     * @param key the interface string key for the metadata label
     * @param value the metadata value
     */
    protected static void add(PropertyConsumer pc, Member m, String key, String value) {
        pc.addProperty(m, string(key), value);
    }

    /**
     * Adds a new, localized boolean property to a consumer. A convenience
     * method to help subclasses write {@link #fillInMetadataImpl}
     * implementations.
     *
     * @param pc the consumer to write to
     * @param m the member that the metadata applies to
     * @param key the interface string key for the metadata label
     * @param value the metadata value
     */
    protected static void add(PropertyConsumer pc, Member m, String key, boolean value) {
        pc.addProperty(m, string(key), value ? string("yes") : string("no"));
    }

    /**
     * Adds a new, localized thumbnail image to a consumer. The thumbnail is
     * created on the fly. A convenience method to help subclasses write
     * {@link #fillInMetadataImpl} implementations.
     *
     * @param pc the consumer to write to
     * @param m the member that the metadata applies to
     * @param key the interface string key for the metadata label
     * @param image the metadata value
     */
    protected static void add(PropertyConsumer pc, Member m, String key, BufferedImage image) {
        pc.addProperty(m, string(key), thumbnail(image));
    }

    /**
     * Adds a new, localized thumbnail image to a consumer. The thumbnail has
     * already been generated using {@link #thumbnail}. A convenience method to
     * help subclasses write {@link #fillInMetadataImpl} implementations.
     *
     * @param pc the consumer to write to
     * @param m the member that the metadata applies to
     * @param key the interface string key for the metadata label
     * @param image the metadata value
     */
    protected static void add(PropertyConsumer pc, Member m, String key, Icon image) {
        pc.addProperty(m, string(key), image);
    }

    /**
     * Creates a thumbnail version of an image.
     *
     * @param image the source image to convert to a thumbnail
     * @return if the source image is small, the source image; otherwise a
     * smaller thumbnail version of the source image
     */
    protected static ImageIcon thumbnail(BufferedImage image) {
        final int MAXW = 128, MAXH = 64;
        if (image.getWidth() > MAXW || image.getHeight() > MAXH) {
            float scale = ImageUtilities.idealCoveringScaleForImage(MAXW, MAXH, image.getWidth(), image.getHeight());
            image = ImageUtilities.resample(image, scale);
        }
        return new ImageIcon(image);
    }

    /**
     * Returns the metadata that would be generated by {@link #fillInMetadata}
     * as a {@link Map}. Unlike {@link #fillInMetadata}, this method will block:
     * the returned map will include all available metadata. The map's iterator
     * is guaranteed to return keys in the same order as they would have been
     * added in by {@code fillInMetadata}. Be aware that the keys in this
     * map are the names of the metadata entries, and that these are typically
     * localized. Therefore, the key names will vary depending on the interface
     * locale.
     *
     * @return a map of the metadata associated with the member
     * @see PropertyConsumer
     * @see Language
     */
    public final Map<String, Object> getMetadata(Member m) {
        GetMetadataPC gmpc = new GetMetadataPC();
        fillInMetadata(m, gmpc);
        // FIXME
        // we don't know what thread (if any) is generating the metadata,
        // so we need to sleep in a loop until it is done
        while (!gmpc.done) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                StrangeEons.log.warning("ignoring thread interruption");
            }
        }
        return gmpc.rawMap;
    }

    private static class GetMetadataPC implements PropertyConsumer {

        volatile boolean done = false;
        Map<String, Object> rawMap = new LinkedHashMap<>();
        Map<String, Object> map = Collections.synchronizedMap(rawMap);

        @Override
        public void addProperty(Member m, String name, Object value) {
            if (done) {
                throw new ConcurrentModificationException("already called doneAddingProperties");
            }
            map.put(name, value);
        }

        @Override
        public void doneAddingProperties() {
            done = true;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void doneAddingBlock() {
        }
    }

    /**
     * Returns a character set encoding for the specified member if it
     * represents a character-based file. If it is not, then this method returns
     * {@code null}.
     *
     * @param m a member to obtain a character set for
     * @return the character set to use when reading the member as a character
     * stream, or {@code null} if the member is not a kind of text file
     * known to this source
     */
    public Charset getDefaultCharset(Member m) {
        if (m.isFolder()) {
            return null;
        }
        Charset encoding = null;
        for (int i = 0; i < KNOWN_TEXT_FILE_TYPES.length; ++i) {
            if (m.getExtension().equals(KNOWN_TEXT_FILE_TYPES[i])) {
                encoding = KNOWN_TEXT_FILE_ENCODINGS[i];
                break;
            }
        }
        // eons-plugin has no extension, but not everything with no extension
        // is eons-plugin...
        if (m.getName().equals("eons-plugin")) {
            encoding = KNOWN_TEXT_FILE_ENCODINGS[2]; // set to UTF-8
        }
        return encoding;
    }
    private static final String[] KNOWN_TEXT_FILE_TYPES = new String[]{
        "txt", "text", "utf8", "htm", "html", "properties", "js", "java",
        "classmap", "tiles", "silhouettes", "css", "cardlayout", "ajs",
        "settings", "collection"
    };
    private static final Charset[] KNOWN_TEXT_FILE_ENCODINGS = new Charset[]{
        Charset.forName("iso-8859-15"),
        Charset.forName("utf-8"),
        Charset.forName("utf-8"),
        Charset.forName("utf-8"),
        Charset.forName("utf-8"),
        Charset.forName("iso-8859-1"),
        Charset.forName("utf-8"),
        Charset.forName("utf-8"),
        Charset.forName("iso-8859-15"),
        Charset.forName("iso-8859-15"),
        Charset.forName("iso-8859-15"),
        Charset.forName("utf-8"),
        Charset.forName("iso-8859-1"),
        Charset.forName("utf-8"),
        Charset.forName("iso-8859-1"),
        Charset.forName("iso-8859-15"),};

    /**
     * This interface is implemented by objects that want to access textual
     * metadata.
     */
    public interface PropertyConsumer {

        /**
         * Called when a new property is ready to be consumed.
         *
         * @param name the name of the property
         * @param value the value of the property
         */
        public void addProperty(Member m, String name, Object value);

        /*
		 * May be called after a group or block of properties
		 * have been added. This is a hint to the consumer that
		 * it may wish to update its own internal data structures
		 * or refresh displayed properties. It is not guaranteed to
		 * be called.
         */
        public void doneAddingBlock();

        /**
         * Called after all properties have been added.
         */
        public void doneAddingProperties();

        /**
         * A metadata source may call this while processing metadata to
         * determine if the consumer is still interested in this data. This
         * allows the source to terminate expensive operations that are no
         * longer of interest. This may be called from any thread.
         *
         * @return {@code true} if the consumer is still interested in this
         * data
         */
        public boolean isValid();
    }

    private static String[] defaultIconExtensions;
    private static Icon[] defaultIcons;

    private static final HashMap<String, Icon> DEFAULT_ICONS = new HashMap<>();

    private static void addIcons(Object... icons) {
        for (int i = 0; i < icons.length; i += 2) {
            DEFAULT_ICONS.put((String) icons[i], (Icon) icons[i + 1]);
        }
    }

    private static void initDefaultIcons() {
        if (DEFAULT_ICONS.size() > 0) {
            return;
        }

        addIcons(
                "cardlayout", ICON_CARD_LAYOUT,
                "class", ICON_CLASS,
                "csv", ICON_TABLE,
                "htm", ICON_HTML,
                "html", ICON_HTML,
                "java", ICON_JAVA,
                "jp2", ICON_IMAGE,
                "jpg", ICON_IMAGE,
                "png", ICON_IMAGE,
                "svg", ICON_VECTOR_IMAGE,
                "svgz", ICON_VECTOR_IMAGE,
                "js", ICON_SCRIPT,
                "ajs", ICON_AUTOMATION_SCRIPT,
                "ttf", ICON_FONT,
                "otf", ICON_FONT,
                "pfb", ICON_FONT,
                "pfa", ICON_FONT,
                "seplugin", ICON_EON_PLUGIN,
                "seext", ICON_EON_EXTENSION,
                "setheme", ICON_EON_THEME,
                "selibrary", ICON_EON_LIBRARY,
                "pbz", ICON_PACKED_BUNDLE,
                "pgz", ICON_PACKED_BUNDLE,
                "plzm", ICON_PACKED_BUNDLE,
                "txt", ICON_SETTINGS,
                "properties", ICON_PROPERTIES,
                "utf8", ICON_SETTINGS,
                "eon", ICON_EON_DEFAULT,
                "doc", ICON_DOCUMENT,
                "rtf", ICON_DOCUMENT,
                "wpd", ICON_DOCUMENT,
                "doc", ICON_DOCUMENT,
                "odt", ICON_DOCUMENT,
                "classmap", ICON_CLASS_MAP,
                "silhouettes", ICON_SILHOUETTES,
                "tiles", ICON_TILE_SET,
                "css", ICON_STYLE_SHEET,
                "cpl", ICON_DICT_CPL,
                "3tree", ICON_DICT_TST,
                "idx", ICON_TEXT_INDEX
        );
    }

    private Icon getSystemIcon(Member m) {
        Icon i = fsv.getSystemIcon(m.getFile());

        if (i == null) {
            i = ICON_FILE;
        } else if (i.getIconWidth() > ICON_SIZE || i.getIconHeight() > ICON_SIZE) {
            i = ImageUtilities.createIconForSize(ImageUtilities.iconToImage(i), ICON_SIZE);
        }

        DEFAULT_ICONS.put(m.getExtension(), i);

        return i;
    }
    private static final FileSystemView fsv = FileSystemView.getFileSystemView();

    /**
     * Preferred width and height of project member icons, in pixels. Note that
     * this value may change in the future.
     */
    static final int ICON_SIZE = 18;

    public static final Icon ICON_PROJECT = getIcon("project/project.png");
    public static final Icon ICON_FOLDER = getIcon("project/folder.png");
    public static final Icon ICON_FILE = getIcon("project/file.png");
    public static final Icon ICON_IMAGE = getIcon("project/image.png");
    public static final Icon ICON_VECTOR_IMAGE = getIcon("project/vector-image.png");
    public static final Icon ICON_JAVA = getIcon("project/java.png");
    public static final Icon ICON_SCRIPT = getIcon("project/script.png");
    public static final Icon ICON_AUTOMATION_SCRIPT = getIcon("project/auto.png");
    public static final Icon ICON_SETTINGS = getIcon("project/settings.png");
    public static final Icon ICON_PROPERTIES = getIcon("project/properties.png");
    public static final Icon ICON_COLLECTION = getIcon("project/collection.png");
    public static final Icon ICON_TABLE = getIcon("project/table.png");
    public static final Icon ICON_HTML = getIcon("project/html.png");
    public static final Icon ICON_STYLE_SHEET = getIcon("project/css.png");
    public static final Icon ICON_PLUGIN_ROOT = getIcon("project/root.png");
    public static final Icon ICON_CLASS = getIcon("project/class.png");
    public static final Icon ICON_BLANK = new BlankIcon(ICON_SIZE);
    public static final Icon ICON_COPIES_LIST = getIcon("project/copies.png");
    public static final Icon ICON_DOCUMENT = getIcon("project/doc.png");
    public static final Icon ICON_FONT = getIcon("project/font.png");
    public static final Icon ICON_EON_DEFAULT = getIcon("project/eon-default.png");
    public static final Icon ICON_EON_PLUGIN = getIcon("project/plugin.png");
    public static final Icon ICON_EON_EXTENSION = getIcon("project/extension.png");
    public static final Icon ICON_EON_THEME = getIcon("project/theme.png");
    public static final Icon ICON_EON_LIBRARY = getIcon("project/library.png");
    public static final Icon ICON_PACKED_BUNDLE = getIcon("project/packed-bundle.png");
    public static final Icon ICON_CLASS_MAP = getIcon("project/classmap.png");
    public static final Icon ICON_SILHOUETTES = getIcon("project/sil.png");
    public static final Icon ICON_TILE_SET = getIcon("project/tiles.png");
    public static final Icon ICON_CARD_LAYOUT = getIcon("project/card-layout.png");

    public static final Icon ICON_DICT_CPL = getIcon("project/dict-cpl.png");
    public static final Icon ICON_DICT_TST = getIcon("project/dict-3tree.png");
    public static final Icon ICON_TEXT_INDEX = getIcon("project/idx.png");

    public static final Icon ICON_TASK = getIcon("project/task.png");
    public static final Icon ICON_TASK_PLUGIN = getIcon("project/plugin-task.png");
    public static final Icon ICON_TASK_FACTORY = getIcon("project/factory-task.png");
    public static final Icon ICON_TASK_EXPBOARD = getIcon("project/ahboard-task.png");
    public static final Icon ICON_TASK_CASEBOOK = getIcon("project/case-task.png");
    public static final Icon ICON_TASK_DECK = getIcon("project/deck-task.png");
    public static final Icon ICON_TASK_DOCUMENTATION = getIcon("project/doc-task.png");
    public static final Icon ICON_TASK_GROUP = getIcon("project/task-group.png");

    // mimics behaviour of ResourceKit.getIcon but defers loading the icon until needed
    // this greatly reduces the time needed to load the class
    private static Icon getIcon(String resource) {
        if (resource.charAt(0) != '/') {
            resource = "icons/" + resource;
        }
        return new ThemedIcon(resource, true);
    }

    /**
     * Returns the standard icon for a task. Note that this is not usually
     * called directly. To determine the icon for a task, call its
     * {@link Task#getIcon} method (or look it up via its metadata source).
     *
     * @param type the task type identifier, stored in the {@link Task#KEY_TYPE}
     * setting key
     * @param iconName the resource name of the preferred icon, stored in the
     * {@link Task#KEY_ICON} setting key
     * @return the standard icon for a task matching the given parameters
     */
    static Icon getDefaultTaskIcon(String type, String iconName) {
        Icon icon = null;

        if (iconName == null) {
            if (null == type) {
                icon = ICON_TASK;
            } else switch (type) {
                case NewTaskType.TASK_GROUP_TYPE:
                    icon = ICON_TASK_GROUP;
                    break;
                case NewTaskType.CASEBOOK_TYPE:
                    icon = ICON_TASK_CASEBOOK;
                    break;
                case NewTaskType.DECK_TYPE:
                    icon = ICON_TASK_DECK;
                    break;
                case NewTaskType.DOCUMENTATION_TYPE:
                    icon = ICON_TASK_DOCUMENTATION;
                    break;
                case NewTaskType.EXPANSION_BOARD_TYPE:
                    icon = ICON_TASK_EXPBOARD;
                    break;
                case NewTaskType.FACTORY_TYPE:
                    icon = ICON_TASK_FACTORY;
                    break;
                case NewTaskType.PLUGIN_TYPE:
                    icon = ICON_TASK_PLUGIN;
                    break;
                default:
                    break;
            }
        } else {
            if (iconName.startsWith("res://")) {
                iconName = iconName.substring("res://".length());
            }
            icon = TASK_ICON_CACHE.get(iconName);

            if (icon == null) {
                // allow theming of custom icons
                BufferedImage bi = new ThemedIcon(iconName).getImage();
                if (bi != null) {
                    if (bi.getWidth() > ICON_SIZE || bi.getHeight() > ICON_SIZE) {
                        float scale = ImageUtilities.idealCoveringScaleForImage(ICON_SIZE, ICON_SIZE, bi.getWidth(), bi.getHeight());
                        bi = ImageUtilities.resample(bi, scale);
                    } else if (bi.getWidth() < ICON_SIZE && bi.getHeight() < ICON_SIZE) {
                        ImageUtilities.center(bi, ICON_SIZE, ICON_SIZE);
                    }
                    icon = new ImageIcon(bi);
                    TASK_ICON_CACHE.put(iconName, icon);
                }
            }
        }

        if (icon == null) {
            icon = MetadataSource.ICON_TASK;
        }
        return icon;
    }
    private static final HashMap<String, Icon> TASK_ICON_CACHE = new HashMap<>();

    /**
     * Property file metadata.
     */
    static class PropertiesMetadata extends MetadataSource {

        @Override
        public boolean appliesTo(Member m) {
            if (m.isFolder()) {
                return false;
            }
            String ext = m.getExtension();
            return ext.equals("properties");
        }

        @Override
        public String getDescription(Member m) {
            if (m.getFile().getName().indexOf('_') < 0) {
                return string("prj-prop-props-def");
            } else {
                return string("prj-prop-props");
            }
        }

        @Override
        public Icon getIcon(Member m) {
            return ICON_PROPERTIES;
        }
    }

    /**
     * Plug-in bundle metadata. Includes compression type listing.
     */
    static class BundleMetadata extends MetadataSource {

        @Override
        public boolean appliesTo(Member m) {
            if (m.isFolder()) {
                return false;
            }
            String ext = m.getExtension();
            return ext.equals("seplugin") || ext.equals("seext") || ext.equals("selibrary") || ext.equals("setheme");
        }

        @Override
        public String getDescription(Member m) {
            return super.getDescription(m);
        }

        @Override
        protected void fillInMetadataImpl(Member m, PropertyConsumer pc) {
            super.fillInMetadataImpl(m, pc);
            try {
                PluginBundle pb = new PluginBundle(m.getFile());
                add(pc, m, "prj-prop-websafe", pb.getFormat() == PluginBundle.FORMAT_WRAPPED);
            } catch (IOException e) {
            }
        }
    }

    /**
     * Deck copies list metadata.
     */
    static class CopiesMetadata extends MetadataSource {

        @Override
        public boolean appliesTo(Member m) {
            return DeckTask.isCopiesList(m);
        }

        @Override
        public String getDescription(Member m) {
            return string("prj-prop-copies");
        }

        @Override
        public Icon getIcon(Member m) {
            return ICON_COPIES_LIST;
        }
    }

    /**
     * Image file metadata. Creates a thumbnail by reading the image in a
     * separate thread.
     */
    static class ImageMetadata extends ThreadedMetadataSource {

        public ImageMetadata() {
            super(false);
        }

        @Override
        public boolean appliesTo(Member m) {
            return ProjectUtilities.matchExtension(m, View.imageTypes);
        }

//		@Override
//		public Icon getIcon( Member m ) {
//			if( ProjectUtilities.matchExtension( m, "psd" ) ) {
//				return super.getIcon( m );
//			} else if( ProjectUtilities.matchExtension( m, View.vectorImageTypes ) ) {
//				return ICON_VECTOR_IMAGE;
//			}
//			return ICON_IMAGE;
//		}
        @Override
        protected void fillInThreadedMetadataImpl(Member m, PropertyConsumer pc) {
            try {
                Object[] data = cache.get(m);
                if (data == null) {
                    if (!pause(pc)) {
                        return;
                    }
                    BufferedImage image = View.getSupportedImage(m.getFile());
                    if (image == null || !pc.isValid()) {
                        return;
                    }

                    if (ProjectUtilities.matchExtension(m, View.vectorImageTypes)) {
                        data = new Object[]{thumbnail(image)};
                    } else {
                        data = new Object[]{
                            String.format("%,d \u00d7 %,d", image.getWidth(), image.getHeight()),
                            thumbnail(image)
                        };
                    }

                    cache.put(m, data);
                }
                if (!pc.isValid()) {
                    return;
                }

                if (ProjectUtilities.matchExtension(m, View.vectorImageTypes)) {
                    add(pc, m, "prj-prop-thumb", (Icon) data[0]);
                } else {
                    add(pc, m, "prj-prop-dim", (String) data[0]);
                    add(pc, m, "prj-prop-thumb", (Icon) data[1]);
                }
            } catch (IOException e) {
            }
        }

        private MetadataCache<Object[]> cache = new MetadataCache<>();
    }

    /**
     * Standard SE text file metadata. Returns a default charset value depending
     * on the file extension.
     */
    static class TextMetadata extends MetadataSource {

        @Override
        public boolean appliesTo(Member m) {
            if (m.isFolder()) {
                return false;
            }
            return ProjectUtilities.matchExtension(m, extensions);
        }
        private static final String[] extensions = new String[]{
            "txt", "text", "settings", "collection"
        };

        @Override
        public String getDescription(Member m) {
            String k = "prj-prop-txt";
            if (isDocType(m)) {
                k = "prj-prop-text";
            } else if (ProjectUtilities.matchExtension(m, "collection")) {
                k = "prj-prop-collection";
            }
            return string(k);
        }

        @Override
        public Icon getIcon(Member m) {
            Icon i = ICON_SETTINGS;
            if (isDocType(m)) {
                i = ICON_DOCUMENT;
            } else if (ProjectUtilities.matchExtension(m, "collection")) {
                i = ICON_COLLECTION;
            }
            return i;
        }

        static boolean isDocType(Member m) {
            if (m == null) {
                return false;
            }

            if (ProjectUtilities.matchExtension(m, "text")) {
                return true;
            }
            if (ProjectUtilities.matchExtension(m, "settings", "collection")) {
                return false;
            }

            if (ProjectUtilities.matchExtension(m, "txt")) {
                Task t = m.getTask();
                if (t != null && NewTaskType.PLUGIN_TYPE.equals(t.getSettings().get(Task.KEY_TYPE))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * HTML metadata; can extract the default charset from a
     * <meta> tag in the file (otherwise defaults to UTF-8).
     */
    static class HTMLMetadata extends MetadataSource {

        @Override
        public boolean appliesTo(Member m) {
            return ProjectUtilities.matchExtension(m, HTML_EXTENSIONS);
        }

        @Override
        public String getDescription(Member m) {
            return DESCRIPTION;
        }
        private static final String DESCRIPTION = string("prj-prop-doc", "HTML");

        @Override
        public Icon getIcon(Member m) {
            return ICON_HTML;
        }

        @Override
        protected void fillInMetadataImpl(Member m, PropertyConsumer pc) {
            super.fillInMetadataImpl(m, pc);

            Charset cs = getDefaultCharset(m);
            BufferedReader r = null;
            try {
                r = new BufferedReader(new InputStreamReader(new FileInputStream(m.getFile()), cs));
                String line;
                while ((line = r.readLine()) != null) {
                    // look for <title>
                    Matcher titleMatcher = PAT_OPEN_TITLE.matcher(line);
                    if (titleMatcher.find()) {
                        String title;
                        Matcher titleCloseMatcher = PAT_CLOSE_TITLE.matcher(line);
                        if (titleCloseMatcher.find(titleMatcher.end())) {
                            title = line.substring(titleMatcher.end(), titleCloseMatcher.start());
                        } else {
                            // only include first line of title to simplify processing
                            title = line.substring(titleMatcher.end(), line.length()) + "...";
                        }
                        add(pc, m, "prj-prop-title", title);
                        return;
                    }
                    // check for <body>, so we stop parsing early
                    if (PAT_OPEN_BODY.matcher(line).find()) {
                        break;
                    }
                }
            } catch (IOException e) {
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                    }
                }
            }
            // add a blank title entry; may remind user to add a title
            add(pc, m, "prj-prop-title", "");
        }

        @Override
        public Charset getDefaultCharset(Member m) {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new InputStreamReader(new FileInputStream(m.getFile()), DEFAULT_CHARSET));
                String line;
                while ((line = r.readLine()) != null) {
                    // look for a <meta ... charset="..."> directive
                    Matcher encodingMatcher = PAT_ENCODING.matcher(line);
                    if (encodingMatcher.find()) {
                        // extract the encoding description from the match
                        // and see if we know this encoding
                        String encoding = encodingMatcher.group(1);
                        if (Charset.isSupported(encoding)) {
                            return Charset.forName(encoding);
                        }
                    }
                    // if we find a </head> or <body>, we will stop looking for an encoding
                    if (PAT_CLOSE_HEAD.matcher(line).find() || PAT_OPEN_BODY.matcher(line).find()) {
                        break;
                    }
                }
            } catch (IOException e) {
                // will end up returning default encoding
                e.printStackTrace();
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                    }
                }
            }
            return DEFAULT_CHARSET;
        }
        private static final String[] HTML_EXTENSIONS = new String[]{"html", "htm"};
        private static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");
        private static final Pattern PAT_ENCODING = Pattern.compile(
                "<meta[^>]+\\s+content\\s*=\\s*['\"][^'\"]*charset=([-\\w])+[^'\"]*['\"][^>]*>",
                Pattern.CASE_INSENSITIVE
        );
        private static final Pattern PAT_CLOSE_HEAD = Pattern.compile(
                "</head>", Pattern.CASE_INSENSITIVE
        );
        private static final Pattern PAT_OPEN_BODY = Pattern.compile(
                "<body>", Pattern.CASE_INSENSITIVE
        );
        private static final Pattern PAT_OPEN_TITLE = Pattern.compile(
                "<title>", Pattern.CASE_INSENSITIVE
        );
        private static final Pattern PAT_CLOSE_TITLE = Pattern.compile(
                "</title>", Pattern.CASE_INSENSITIVE
        );
    }

    static class DictionaryMetadata extends MetadataSource {

        public DictionaryMetadata() {
        }

        @Override
        public boolean appliesTo(Member m) {
            return (!m.hasChildren()) && ProjectUtilities.matchExtension(m, DICTIONARY_EXTENSIONS);
        }

        @Override
        protected void fillInMetadataImpl(Member m, PropertyConsumer pc) {
            super.fillInMetadataImpl(m, pc);

            if (m.getExtension().equals("3tree")) {
                try {
                    TTLInfo ttl = TernaryTreeList.getInfo(m.getFile());
                    pc.addProperty(m, string("prj-prop-dict-tst-freq"), string(ttl.hasFrequencyRanks ? "yes" : "no"));
                    pc.addProperty(m, string("prj-prop-dict-tst-nodes"), String.format("%,d", ttl.nodeCount));
                } catch (IOException e) {
                }
            }
        }

        public static final String[] DICTIONARY_EXTENSIONS = new String[]{
            "cpl", "3tree"
        };
    }

    /**
     * Game component metadata; if the game component file is a new version that
     * provides a metadata header, this data is returned and a thumbnail of the
     * component is generated. A few component types do not support thumbnail
     * images, in which case no thumbnail or a default thumbnail is substituted.
     */
    static class GameComponentMetadata extends ThreadedMetadataSource {

        @Override
        public boolean appliesTo(Member m) {
            return ProjectUtilities.matchExtension(m, "eon");
        }

        @Override
        public Icon getIcon(Member m) {
            return ICON_EON_DEFAULT;
        }

        @Override
        @SuppressWarnings("fallthrough")
        protected void fillInThreadedMetadataImpl(Member m, PropertyConsumer pc) {
            Object[] data = cache.get(m);
            if (data == null) {
                ComponentMetadata cm = new ComponentMetadata(m.getFile());

                if (cm.getMetadataVersion() < 1) {
                    cache.put(m, new Object[0]);
                    return;
                }

                if (!pc.isValid()) {
                    return;
                }

                data = new Object[6];

                // [0] component name
                data[0] = cm.getName();

                // [1] component class
                String klass = cm.getComponentClassName();
                if (klass.startsWith("ca.cgjennings.apps.")) {
                    klass = klass.substring("ca.cgjennings.apps.".length());
                }
                data[1] = klass;

                // if this object can be placed in a deck (so it is a normal
                // card-type component), try to create a thumbnail
                if (cm.isDeckLayoutSupported()) {
                    Sheet[] sheets = null;
                    GameComponent gc = ResourceKit.getGameComponentFromFile(m.getFile(), false);
                    if (!pc.isValid()) {
                        return;
                    }

                    if (gc != null && (sheets = gc.createDefaultSheets()).length > 0) {
                        if (!pc.isValid()) {
                            return;
                        }
                        if (gc instanceof DIY) {
                            data[2] = ((DIY) gc).getHandlerScript();
                        }

                        data[3] = String.format("%,d", sheets.length);

                        sheets[0].setPrototypeRenderingModeEnabled(false);
                        BufferedImage image = sheets[0].paint(RenderTarget.FAST_PREVIEW, 72d);
                        PrintDimensions dim = sheets[0].getPrintDimensions();
                        final int unit = Length.getDefaultUnit();
                        final double w = dim.getWidthInUnit(unit);
                        final double h = dim.getHeightInUnit(unit);
                        data[4] = String.format("%,.2f \u00d7 %,.2f %s", w, h, string("iid-cb-unit" + unit));

                        if (!pc.isValid()) {
                            return;
                        }
                        data[5] = thumbnail(image);

                    }
                } // If it can't appear in a deck but we recognize the class name, show dummy thumbnails
                else if ("ca.cgjennings.apps.arkham.casebook.Casebook".equals(cm.getComponentClassName())) {
                    data[5] = ResourceKit.getIcon("application/preview/casebook.jpg");
                } else if (ca.cgjennings.apps.arkham.deck.Deck.class.getName().equals(cm.getComponentClassName())) {
                    data[5] = ResourceKit.getIcon("application/preview/deck.png");
                }

                cache.put(m, data);
            }

            if (data.length == 0) {
                return;
            }
            add(pc, m, "prj-prop-cname", (String) data[0]);
            add(pc, m, "prj-prop-cclass", (String) data[1]);
            if (data[2] != null) {
                add(pc, m, "prj-prop-diy-script", (String) data[2]);
            }
            if (data[3] != null) {
                add(pc, m, "prj-prop-cfaces", (String) data[3]);
            }
            if (data[4] != null) {
                add(pc, m, "prj-prop-dim", (String) data[4]);
            }
            if (data[5] != null) {
                add(pc, m, "prj-prop-thumb", (Icon) data[5]);
            }
        }

        private MetadataCache<Object[]> cache = new MetadataCache<>();
    }

    /**
     * An abstract helper class for creating metadata sources that fill in some
     * of their data from a background thread. This prevents blocking the user
     * interface to complete potentially complex or time-consuming operations,
     * such as generating a preview thumbnail. Be careful to observe Swing
     * threading restrictions.
     */
    public static abstract class ThreadedMetadataSource extends MetadataSource {

        private boolean autopause;

        public ThreadedMetadataSource() {
            this(true);
        }

        /**
         * If {@code autopause} is {@code true}, then
         * {@code pause()} will be called before calling
         * {@code fillInThreadedMetadataImpl}. If the property consumer
         * becomes invalid during the pause, then the threaded metadata will not
         * be requested.
         *
         * @param autopause
         */
        public ThreadedMetadataSource(boolean autopause) {
            this.autopause = autopause;
        }

        @Override
        public void fillInMetadata(Member m, PropertyConsumer pc) {
            fillInMetadataImpl(m, pc);
        }

        @Override
        protected void fillInMetadataImpl(final Member m, final PropertyConsumer pc) {
            super.fillInMetadataImpl(m, pc);

            executor.execute(() -> {
                pc.doneAddingBlock();
                if (autopause && !pause(pc)) {
                    return;
                }
                fillInThreadedMetadataImpl(m, pc);
                EventQueue.invokeLater(pc::doneAddingProperties);
            });
        }
        private static Executor executor = Executors.newSingleThreadExecutor();

        /**
         * Briefly pauses the current thread, then returns {@code false} if
         * the property consumer is no longer valid or the thread was
         * interrupted. This is called at the start of the background thread if
         * the autopause option is enabled. It prevents the generation of
         * expensive metadata when the user is quickly moving through the
         * project members, such as when navigating the tree with the keyboard.
         *
         * @return {@code true} if the thread should proceed with
         * generating metadata
         */
        protected boolean pause(PropertyConsumer pc) {
            if (Thread.interrupted()) {
                return false;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return false;
            }
            return pc.isValid();
        }

        /**
         * This method is called from a background thread to allow the source to
         * fill in additional metadata that may be expensive to generate.
         *
         * @param m the member to fill in data for
         * @param pc the consumer that will accept the data being produced
         */
        protected abstract void fillInThreadedMetadataImpl(Member m, PropertyConsumer pc);
    }
}
