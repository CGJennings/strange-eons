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

public class ConversionMap {

    private static final List<String> conversionMapFiles = new ArrayList<>();
    private static final Map<String, Group> cachedGroups = new HashMap<>();

    private static ConversionMap globalInstance = null;

    private final Map<String, Set<Conversion>> conversionsByClassName = new HashMap<>();
    private final Map<String, Set<Group>> groupsByClassName = new HashMap<>();
    private final Map<Group, Set<Conversion>> conversionsByGroup = new HashMap<>();

    private final Map<String, Set<Conversion>> cachedDirectConversions = new HashMap<>();
    private final Map<String, Map<Group, Set<Conversion>>> cachedGroupConversions = new HashMap<>();

    public ConversionMap() throws IOException {
        this(conversionMapFiles.toArray(new String[conversionMapFiles.size()]));
    }

    public ConversionMap(String... resources) throws IOException {
        for (String resource : resources) {
            try (Parser parser = new Parser(resource, false)) {
                Entry entry;
                while ((entry = parser.next()) != null) {
                    if (!(entry instanceof Conversion)) {
                        continue;
                    }
                    Conversion conversion = (Conversion) entry;
                    String sourceClassName = conversion.getSourceClassName();
                    if (sourceClassName != null) {
                        if (!conversionsByClassName.containsKey(sourceClassName)) {
                            conversionsByClassName.put(sourceClassName, new HashSet<>());
                        }
                        conversionsByClassName.get(sourceClassName).add(conversion);
                        continue;
                    }
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

    public static void add(String resource) {
        Lock.test();
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        conversionMapFiles.add(resource);
    }

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

    public static abstract class Entry {

        private final String id;

        public Entry(String id) {
            this.id = id;
        }

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

    public static final class Class extends Entry {

        public Class(String className) {
            super(className);
        }

        public String getClassName() {
            return getId();
        }
    }

    public static abstract class NamedEntry extends Entry {

        private String name;

        public NamedEntry(String id, String name) {
            super(id);
            if (name.startsWith("@")) {
                this.name = Language.getInterface().get(name.substring(1));
            } else {
                this.name = name;
            }
        }

        public String getName() {
            return name;
        }

        protected void setName(String name) {
            this.name = name;
        }
    }

    public static final class Group extends NamedEntry {

        public Group(String id, String name) {
            super(id, name);
        }
    }

    public static final class Conversion extends NamedEntry {

        private final String sourceClassName;
        private final String targetClassName;
        private final String requiredExtensionName;
        private final String requiredExtensionId;
        private final Group group;

        public Conversion(String name, Class sourceClassName, String targetClassName, String requiredExtensionName, String requiredExtensionId, Group group) {
            super(name, name);
            this.sourceClassName = sourceClassName != null ? sourceClassName.getClassName() : null;
            this.targetClassName = targetClassName;
            this.requiredExtensionName = requiredExtensionName;
            this.requiredExtensionId = requiredExtensionId;
            this.group = group;
        }

        public String getSourceClassName() {
            return sourceClassName;
        }

        public String getTargetClassName() {
            return targetClassName;
        }

        public String getRequiredExtensionName() {
            return requiredExtensionName;
        }

        public String getRequiredExtensionId() {
            return requiredExtensionId;
        }

        public Group getGroup() {
            return group;
        }

        public boolean hasRequiredExtension() {
            try {
                return requiredExtensionId == null || BundleInstaller.isPluginBundleInstalled(requiredExtensionId);
            } catch (IllegalArgumentException e) {
                StrangeEons.log.log(Level.WARNING, "ignoring conversion {0} because the extension id is invalid: {1}", new Object[]{getId(), requiredExtensionId});
                return false;
            }
        }

        public ManualConversionTrigger createManualConversionTrigger() {
            return new ManualConversionTrigger(targetClassName, requiredExtensionName, requiredExtensionId, group != null ? group.getId() : null);
        }
    }

    public static final class Parser extends ResourceParser<Entry> {

        private Class currentClass = null;
        private Group currentGroup = null;

        public Parser(String resource, boolean gentle) throws IOException {
            super(resource, gentle);
        }

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
                currentClass = null;
                currentGroup = cacheGroup(parseGroup(entry));
                return currentGroup;
            }
            if (entry[1].isEmpty()) {
                currentClass = new Class(entry[0].trim());
                currentGroup = null;
                return currentClass;
            }
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
            String[] extension = parts[1].split(":");
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
