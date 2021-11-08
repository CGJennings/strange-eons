package gamedata;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.conversion.ManualConversionTrigger;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import resources.Language;
import static resources.Language.string;

/**
 * Represents the possible game component conversion options available in the
 * {@link ca.cgjennings.apps.arkham.ConvertMenu} and stores the data needed to
 * initiate the conversions. Primarily, this class is used by extensions to
 * register new conversion options {@linkplain #add from a conversion map file}.
 * This class can also be used to examine conversion map file entries
 * programmatically. For example, the following script code prints a list of
 * direct conversion options for the {@code my.example.CustomComponent}
 * component type:
 * <pre>
 * var classNane = 'my.example.CustomComponent';
 * var conversionMap = gamedata.ConversionMap.globalInstance;
 * for( let entry in Iterator( conversionMap.getDirectConversions(classNane) ) ) {
 *     println( entry.name + ' -&gt; ' + entry.targetClassName );
 * }
 * </pre>
 *
 * @author Henrik Rostedt
 */
public class ConversionMap {

    private static final List<String> conversionMapFiles = new ArrayList<>();
    private static final Map<String, Group> cachedGroups = new HashMap<>();

    private static ConversionMap globalInstance = null;

    private final Map<String, Set<Conversion>> conversionsByClassName = new HashMap<>();
    private final Map<String, Set<Group>> groupsByClassName = new HashMap<>();
    private final Map<Group, Set<Conversion>> conversionsByGroup = new HashMap<>();

    private final Map<String, Set<Conversion>> cachedDirectConversions = new HashMap<>();
    private final Map<String, Map<Group, Set<Conversion>>> cachedGroupConversions = new HashMap<>();

    private ConversionMap() throws IOException {
        this(conversionMapFiles.toArray(new String[conversionMapFiles.size()]));
    }

    /**
     * Creates a new conversion map containing the entries parsed from the
     * specified conversion map resources. Groups entries are cached statically,
     * however, the group members are tracked per instance. An extension wanting
     * to inspect the manual conversion options should generally use
     * {@link #getGlobalInstance()} instead.
     *
     * @param resources the conversion map resources to parse
     * @throws IOException if any of the resources can not be read
     */
    public ConversionMap(String... resources) throws IOException {
        for (String resource : resources) {
            try (Parser parser = new Parser(resource, false)) {
                Entry entry;
                while ((entry = parser.next()) != null) {
                    // Class names and groups are extracted from the conversions
                    if (!(entry instanceof Conversion)) {
                        continue;
                    }
                    Conversion conversion = (Conversion) entry;
                    String sourceClassName = conversion.getSourceClassName();
                    if (sourceClassName != null) {
                        // This is a direct conversion
                        if (!conversionsByClassName.containsKey(sourceClassName)) {
                            conversionsByClassName.put(sourceClassName, new HashSet<>());
                        }
                        conversionsByClassName.get(sourceClassName).add(conversion);
                        continue;
                    }
                    // This is a group conversion
                    String targetClassName = conversion.getTargetClassName();
                    Group group = conversion.getGroup();
                    if (!groupsByClassName.containsKey(targetClassName)) {
                        groupsByClassName.put(targetClassName, new HashSet<>());
                    }
                    groupsByClassName.get(targetClassName).add(group);
                    if (!conversionsByGroup.containsKey(group)) {
                        conversionsByGroup.put(group, new HashSet<>());
                    }
                    conversionsByGroup.get(group).add(conversion);
                }
            }
        }
    }

    /**
     * Returns the direct conversions from the specified component type. Only
     * conversions for installed extensions are included. The returned set is
     * immutable and only calculated once per component type.
     *
     * @param sourceClassName the class name of the source component type
     * @return the direct conversions from the component type
     */
    public Set<Conversion> getDirectConversions(String sourceClassName) {
        if (cachedDirectConversions.containsKey(sourceClassName)) {
            return cachedDirectConversions.get(sourceClassName);
        }
        Set<Conversion> conversions = conversionsByClassName.get(sourceClassName);
        if (conversions == null) {
            return Collections.emptySet();
        }
        Set<Conversion> conversionsToInclude = new HashSet<>();
        for (Conversion conversion : conversions) {
            if (!conversion.hasRequiredExtension()) {
                continue;
            }
            conversionsToInclude.add(conversion);
        }
        conversionsToInclude = Collections.unmodifiableSet(conversionsToInclude);
        cachedDirectConversions.put(sourceClassName, conversionsToInclude);
        return conversionsToInclude;
    }

    /**
     * Returns the group conversions from the specified component type. Only
     * conversions for installed extensions are included. The returned map is
     * immutable and only calculated once per component type.
     *
     * @param sourceClassName the class name of the source component type
     * @return the group conversions from the component type
     */
    public Map<Group, Set<Conversion>> getGroupConversions(String sourceClassName) {
        if (cachedGroupConversions.containsKey(sourceClassName)) {
            return cachedGroupConversions.get(sourceClassName);
        }
        Set<Group> groups = groupsByClassName.get(sourceClassName);
        if (groups == null) {
            return Collections.emptyMap();
        }
        Map<Group, Set<Conversion>> groupsToInclude = new HashMap<>();
        for (Group group : groups) {
            Set<Conversion> conversions = conversionsByGroup.get(group);
            Set<Conversion> conversionsToInclude = new HashSet<>();
            for (Conversion conversion : conversions) {
                if (conversion.getTargetClassName().equals(sourceClassName) || !conversion.hasRequiredExtension()) {
                    continue;
                }
                conversionsToInclude.add(conversion);
            }
            if (conversionsToInclude.isEmpty()) {
                continue;
            }
            groupsToInclude.put(group, Collections.unmodifiableSet(conversionsToInclude));
        }
        groupsToInclude = Collections.unmodifiableMap(groupsToInclude);
        cachedGroupConversions.put(sourceClassName, groupsToInclude);
        return groupsToInclude;
    }

    /**
     * Adds the specified conversion map resource to the list of such files that
     * is used to generate the contents of the convert menu options. The sample
     * conversion map (<tt>/resources/projects/new-conversionmap.txt</tt>)
     * describes the format of these files.
     *
     * @param resource a relative URL within <tt>resources/</tt> that points to
     * the file to add
     */
    public static void add(String resource) {
        Lock.test();
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        conversionMapFiles.add(resource);
    }

    /**
     * Returns the global instance of {@code ConversionMap} that can be used to
     * look up conversion options. The instance is created the first time this
     * is called. Must not be called before all extensions have been loaded.
     *
     * @return the global {@code ConversionMap} instance
     * @throws IOException if the global instance could not be created
     */
    public static ConversionMap getGlobalInstance() throws IOException {
        if (globalInstance == null) {
            globalInstance = new ConversionMap();
        }
        return globalInstance;
    }

    private static Group cacheGroup(Group group) {
        Group existing = cachedGroups.get(group.getId());
        if (existing == null) {
            cachedGroups.put(group.getId(), group);
            return group;
        }
        if (existing.getName().equals(existing.getId())) {
            existing.setName(group.getName());
        }
        return existing;
    }

    /**
     * Represent any entry of a conversion map file.
     */
    public static abstract class Entry {

        private final String id;

        /**
         * Creates a new entry.
         *
         * @param id the identifier for the entry
         */
        public Entry(String id) {
            this.id = id;
        }

        /**
         * Returns the identifier for the entry. Is the group identifier for
         * {@link Group} and the entry key for {@link Conversion}. Only
         * guaranteed to be unique within a conversion group.
         *
         * @return the entry identifier
         */
        public String getId() {
            return id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            return Objects.equals(this.id, ((Entry) obj).id);
        }
    }

    /**
     * Represents a direct conversion group in a conversion map file.
     */
    public static final class Class extends Entry {

        /**
         * Creates a new class name entry
         *
         * @param className the class name
         */
        public Class(String className) {
            super(className);
        }

        /**
         * Returns the source class name for the direct conversions.
         *
         * @return the class name
         */
        public String getClassName() {
            return getId();
        }
    }

    /**
     * Represents a conversion map entry with a localized name.
     */
    public static abstract class NamedEntry extends Entry {

        private String name;

        /**
         * Creates a new named entry.
         *
         * @param id the entry identifier
         * @param name the name to be localized
         */
        public NamedEntry(String id, String name) {
            super(id);
            if (name.startsWith("@")) {
                this.name = Language.getInterface().get(name.substring(1));
            } else {
                this.name = name;
            }
        }

        /**
         * Returns the localized name of the entry.
         *
         * @return the localized name
         */
        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Represents a conversion group in a conversion map file.
     */
    public static final class Group extends NamedEntry {

        /**
         * Creates a new group entry.
         *
         * @param id the group id
         * @param name the group name
         */
        public Group(String id, String name) {
            super(id, name);
        }
    }

    /**
     * Represents a conversion option in a conversion map file
     */
    public static final class Conversion extends NamedEntry {

        private final String sourceClassName;
        private final String targetClassName;
        private final String requiredExtensionName;
        private final String requiredExtensionId;
        private final Group group;

        /**
         * Creates a new conversion option.
         *
         * @param name the name of the conversion option
         * @param sourceClassName the source class name entry, or {@code null}
         * if this is a group conversion option
         * @param targetClassName the target class name
         * @param requiredExtensionName the name of the required extension, or
         * {@code null} if the target belongs to the same extension
         * @param requiredExtensionId the identifier of the required extension,
         * or {@code null} if the target belongs to the same extension
         * @param group the group entry, or {@code null} if this is a direct
         * conversion option
         */
        public Conversion(String name, Class sourceClassName, String targetClassName, String requiredExtensionName, String requiredExtensionId, Group group) {
            super(name, name);
            this.sourceClassName = sourceClassName != null ? sourceClassName.getClassName() : null;
            this.targetClassName = targetClassName;
            this.requiredExtensionName = requiredExtensionName;
            this.requiredExtensionId = requiredExtensionId;
            this.group = group;
        }

        /**
         * Returns the class name of the source component type. Returns
         * {@code null} if this is a group conversion option.
         *
         * @return the source class name
         */
        public String getSourceClassName() {
            return sourceClassName;
        }

        /**
         * Returns the class name of the target component type.
         *
         * @return the target class name
         */
        public String getTargetClassName() {
            return targetClassName;
        }

        /**
         * Returns the name of the required extension for the target component
         * type. Returns {@code null} if it belongs to the same extension as the
         * source component type.
         *
         * @return the required extension name
         */
        public String getRequiredExtensionName() {
            return requiredExtensionName;
        }

        /**
         * Returns the identifier of the required extension for the target
         * component type. Returns {@code null} if it belongs to the same
         * extension as the source component type.
         *
         * @return the required extension identifier
         */
        public String getRequiredExtensionId() {
            return requiredExtensionId;
        }

        /**
         * Returns the conversion group this conversion option belongs to.
         * Returns {@code null} if this is a direct conversion option.
         *
         * @return the conversion group
         */
        public Group getGroup() {
            return group;
        }

        /**
         * Checks if the required extension is installed.
         *
         * @return whether the required extension is installed or not
         */
        public boolean hasRequiredExtension() {
            try {
                return requiredExtensionId == null || BundleInstaller.isPluginBundleInstalled(requiredExtensionId);
            } catch (IllegalArgumentException e) {
                StrangeEons.log.log(Level.WARNING, "ignoring conversion {0} because the extension id is invalid: {1}", new Object[]{getId(), requiredExtensionId});
                return false;
            }
        }

        /**
         * Creates a manual conversion trigger based on this conversion option.
         *
         * @return a manual conversion trigger
         */
        public ManualConversionTrigger createManualConversionTrigger() {
            return new ManualConversionTrigger(targetClassName, requiredExtensionName, requiredExtensionId, group != null ? group.getId() : null);
        }
    }

    /**
     * A parser for conversion map files.
     */
    public static final class Parser extends ResourceParser<Entry> {

        private Class currentClass = null;
        private Group currentGroup = null;

        /**
         * Creates a new parser for the specified resource file.
         *
         * @param resource the location of the conversion map resource
         * @param gentle if {@code true}, parses in gentle mode
         * @throws IOException if an I/O error occurs
         */
        public Parser(String resource, boolean gentle) throws IOException {
            super(resource, gentle);
        }

        /**
         * Creates a new parser for the specified input stream.
         *
         * @param in the input stream to read from
         * @param gentle if {@code true}, parses in gentle mode
         * @throws IOException if an I/O error occurs
         */
        public Parser(InputStream in, boolean gentle) throws IOException {
            super(in, gentle);
        }

        @Override
        public Entry next() throws IOException {
            String[] entry = readProperty();
            if (entry == null) {
                return null;
            }
            if (entry[0].startsWith("$")) {
                // group entry
                currentClass = null;
                currentGroup = cacheGroup(parseGroup(entry));
                return currentGroup;
            }
            if (entry[1].isEmpty()) {
                // class name entry
                currentClass = new Class(entry[0].trim());
                currentGroup = null;
                return currentClass;
            }
            // conversion option entry
            if (currentGroup == null && currentClass == null) {
                error(string("rk-err-parse-conversionmap"));
                return next();
            }
            return parseConversion(entry);
        }

        private Group parseGroup(String[] entry) {
            if (!entry[1].isEmpty()) {
                warning("assignment to conversion map group");
            }
            String[] parts = entry[0].split("\\|");
            String id = parts[0].substring(1).trim();
            if (parts.length < 2) {
                return new Group(id, id);
            }
            if (parts.length > 2) {
                warning("extra fields in conversion map group");
            }
            return new Group(id, parts[1].trim());
        }

        private Conversion parseConversion(String[] entry) {
            String name = entry[0].trim();
            String[] parts = entry[1].split("\\|");
            String targetClassName = parts[0].trim();
            if (parts.length < 2) {
                return new Conversion(name, currentClass, targetClassName, null, null, currentGroup);
            }
            String[] extension = parts[1].split(";");
            String requiredExtensionName = null;
            String requiredExtensionId = null;
            if (extension.length == 2) {
                requiredExtensionName = extension[0].trim();
                requiredExtensionId = extension[1].trim();
            } else {
                warning("malformed extension field in conversion map entry");
            }
            if (parts.length > 2) {
                warning("extra fields in conversion map entry");
            }
            return new Conversion(name, currentClass, targetClassName, requiredExtensionName, requiredExtensionId, currentGroup);
        }
    }
}
