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
import java.util.Set;
import java.util.logging.Level;
import resources.Language;

public class ConversionMap {

    private static final List<String> conversionMapFiles = new ArrayList<>();
    private static final Map<String, GroupEntry> cachedGroups = new HashMap<>();

    private static ConversionMap globalInstance = null;

    private final Map<String, Set<ConversionEntry>> conversionsByClassName = new HashMap<>();
    private final Map<String, Set<GroupEntry>> groupsByClassName = new HashMap<>();
    private final Map<GroupEntry, Set<ConversionEntry>> conversionsByGroup = new HashMap<>();

    private final Map<String, Set<ConversionEntry>> cachedDirectConversions = new HashMap<>();
    private final Map<String, Map<GroupEntry, Set<ConversionEntry>>> cachedGroupConversions = new HashMap<>();

    public ConversionMap() throws IOException {
        this(conversionMapFiles.toArray(new String[conversionMapFiles.size()]));
    }

    public ConversionMap(String... resources) throws IOException {
        for (String resource : resources) {
            try (Parser parser = new Parser(resource, false)) {
                Entry entry;
                while ((entry = parser.next()) != null) {
                    if (!(entry instanceof ConversionEntry)) {
                        continue;
                    }
                    ConversionEntry e = (ConversionEntry) entry;
                    String sourceClassName = e.getSourceClassName();
                    if (sourceClassName != null) {
                        if (!conversionsByClassName.containsKey(sourceClassName)) {
                            conversionsByClassName.put(sourceClassName, new HashSet<>());
                        }
                        conversionsByClassName.get(sourceClassName).add(e);
                        continue;
                    }
                    String targetClassName = e.getTargetClassName();
                    GroupEntry group = e.getGroup();
                    if (!groupsByClassName.containsKey(targetClassName)) {
                        groupsByClassName.put(targetClassName, new HashSet<>());
                    }
                    groupsByClassName.get(targetClassName).add(group);
                    if (!conversionsByGroup.containsKey(group)) {
                        conversionsByGroup.put(group, new HashSet<>());
                    }
                    conversionsByGroup.get(group).add(e);
                }
            }
        }
    }

    public Set<ConversionEntry> getDirectConversions(String sourceClassName) {
        if (cachedDirectConversions.containsKey(sourceClassName)) {
            return cachedDirectConversions.get(sourceClassName);
        }
        Set<ConversionEntry> entries = conversionsByClassName.get(sourceClassName);
        if (entries == null) {
            return Collections.emptySet();
        }
        Set<ConversionEntry> entriesToInclude = new HashSet<>();
        for (ConversionEntry entry : entries) {
            if (!entry.hasRequiredExtension()) {
                continue;
            }
            entriesToInclude.add(entry);
        }
        entriesToInclude = Collections.unmodifiableSet(entriesToInclude);
        cachedDirectConversions.put(sourceClassName, entriesToInclude);
        return entriesToInclude;
    }

    public Map<GroupEntry, Set<ConversionEntry>> getGroupConversions(String sourceClassName) {
        if (cachedGroupConversions.containsKey(sourceClassName)) {
            return cachedGroupConversions.get(sourceClassName);
        }
        Set<GroupEntry> groups = groupsByClassName.get(sourceClassName);
        if (groups == null) {
            return Collections.emptyMap();
        }
        Map<GroupEntry, Set<ConversionEntry>> conversions = new HashMap<>();
        for (GroupEntry group : groups) {
            Set<ConversionEntry> entries = conversionsByGroup.get(group);
            Set<ConversionEntry> entriesToInclude = new HashSet<>();
            for (ConversionEntry entry : entries) {
                if (entry.getTargetClassName() == sourceClassName || !entry.hasRequiredExtension()) {
                    continue;
                }
                entriesToInclude.add(entry);
            }
            if (entriesToInclude.size() == 0) {
                continue;
            }
            conversions.put(group, Collections.unmodifiableSet(entriesToInclude));
        }
        conversions = Collections.unmodifiableMap(conversions);
        cachedGroupConversions.put(sourceClassName, conversions);
        return conversions;
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

    private static GroupEntry cacheGroup(GroupEntry group) {
        GroupEntry existing = cachedGroups.get(group.getId());
        if (existing == null) {
            cachedGroups.put(group.getId(), group);
            return group;
        }
        if (existing.getName() == existing.getId()) {
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
    }

    public static final class ClassEntry extends Entry {

        public ClassEntry(String className) {
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

    public static final class GroupEntry extends NamedEntry {

        public GroupEntry(String id, String name) {
            super(id, name);
        }
    }

    public static final class ConversionEntry extends NamedEntry {

        private final String sourceClassName;
        private final String targetClassName;
        private final String requiredExtensionName;
        private final String requiredExtensionId;
        private final GroupEntry group;

        public ConversionEntry(String name, ClassEntry sourceClassName, String targetClassName, String requiredExtensionName, String requiredExtensionId, GroupEntry group) {
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

        public GroupEntry getGroup() {
            return group;
        }

        public boolean hasRequiredExtension() {
            try {
                return requiredExtensionId == null || BundleInstaller.isPluginBundleInstalled(requiredExtensionId);
            } catch (IllegalArgumentException e) {
                StrangeEons.log.log(Level.WARNING, "ignoring conversion " + getId() + " because the extension id is invalid: " + requiredExtensionId);
                return false;
            }
        }

        public ManualConversionTrigger createManualConversionTrigger() {
            return new ManualConversionTrigger(targetClassName, requiredExtensionName, requiredExtensionId, group != null ? group.getId() : null);
        }
    }

    public static final class Parser extends ResourceParser<Entry> {

        private ClassEntry currentClass = null;
        private GroupEntry currentGroup = null;

        public Parser(String resource, boolean gentle) throws IOException {
            super(resource, gentle);
        }

        public Parser(InputStream in, boolean gentle) throws IOException {
            super(in, gentle);
        }

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
                currentClass = new ClassEntry(entry[0].trim());
                currentGroup = null;
                return currentClass;
            }
            if (currentGroup == null && currentClass == null) {
                error("parse error");
                return next();
            }
            return parseConversion(entry);
        }

        private GroupEntry parseGroup(String[] entry) {
            if (!entry[1].isEmpty()) {
                error("parse error");
            }
            String[] parts = entry[0].split("\\|");
            String id = parts[0].substring(1).trim();
            if (parts.length < 2) {
                return new GroupEntry(id, id);
            }
            if (parts.length > 2) {
                error("parse error");
            }
            return new GroupEntry(id, parts[1].trim());
        }

        private ConversionEntry parseConversion(String[] entry) {
            String name = entry[0].trim();
            String[] parts = entry[1].split("\\|");
            String targetClassName = parts[0].trim();
            if (parts.length < 2) {
                return new ConversionEntry(name, currentClass, targetClassName, null, null, currentGroup);
            }
            String[] extension = parts[1].split(":");
            String requiredExtensionName = null;
            String requiredExtensionId = null;
            if (extension.length == 2) {
                requiredExtensionName = extension[0].trim();
                requiredExtensionId = extension[1].trim();
            } else {
                error("parse error");
            }
            if (parts.length > 2) {
                error("parse error");
            }
            return new ConversionEntry(name, currentClass, targetClassName, requiredExtensionName, requiredExtensionId, currentGroup);
        }
    }
}
