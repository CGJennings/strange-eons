package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.apps.arkham.plugins.catalog.NetworkProxy;
import gamedata.*;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import javax.swing.UIManager;
import resources.CacheMetrics;
import resources.ResourceKit;
import resources.Settings;

/**
 * A registry of {@link TableGenerator} instances. By registering your own
 * generator, you can extend the list of data tables available from the
 * debugger.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Tables {

    private Tables() {
    }

    /**
     * Register a table. If the table is already registered, this method has no
     * effect. If a table with the same name is already registered, then a new
     * unique name will be synthesized for the table based on the conflicting
     * name.
     *
     * @param tg the generator to register
     * @return the name that the generator is registered under; this may be
     * different from the name returned by the generator itself
     */
    public static String register(TableGenerator tg) {
        if (tg == null) {
            throw new NullPointerException("tg");
        }
        String baseName = tg.getTableName();
        if (baseName == null) {
            throw new NullPointerException("table name");
        }
        baseName = baseName.replace('\n', ' ');

        String name = baseName;
        synchronized (toGen) {
            int n = 0;
            TableGenerator exists;
            while ((exists = getGenerator(name)) != null) {
                if (exists == tg) {
                    return name;
                }
                name = baseName + ' ' + (++n);
            }
            toGen.put(name, tg);
        }
        return name;
    }

    /**
     * Removes a generator from the registry. There is no effect if the
     * generator is not registered.
     *
     * @param tg the generator to unregister
     */
    public static void unregister(TableGenerator tg) {
        synchronized (toGen) {
            String key = null;
            for (Entry<String, TableGenerator> e : toGen.entrySet()) {
                if (e.getValue() == tg) {
                    key = e.getKey();
                    break;
                }
            }
            toGen.remove(key);
        }
    }

    /**
     * Returns an array of the names of the registered generators.
     *
     * @return an array of the current generator names
     */
    public static String[] getTableNames() {
        String[] names = toGen.keySet().toArray(new String[toGen.size()]);
        Arrays.sort(names);
        return names;
    }

    /**
     * Returns the generator with the given {@code name}, or {@code null} if
     * there is no such generator.
     *
     * @param name the name of the generator to find
     * @return the generator registered under {@code name}, or {@code null}
     */
    public static TableGenerator getGenerator(String name) {
        return toGen.get(name);
    }

    /**
     * Returns a table generator using the generator with the given
     * {@code name}, or {@code null}.
     *
     * @param name the name of the generator to use
     * @return a table generated with the named generator, or {@code null}
     */
    public static InfoTable generate(String name) {
        TableGenerator tg = getGenerator(name);
        if (tg == null) {
            return null;
        } else {
            return tg.generateTable();
        }
    }

    private static final Map<String, TableGenerator> toGen = Collections.synchronizedMap(new HashMap<>());

    private static void stdcols(InfoTable t) {
        t.setColumns("Property", "Value");
    }

    /**
     * Standard table of root settings.
     */
    public static final TableGenerator SETTINGS = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Settings";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            stdcols(t);
            Settings s = Settings.getShared();
            for (String key : s.getVisibleKeySet()) {
                t.add(key, s.get(key));
            }
            return t;
        }
    };

    /**
     * Standard table of games and expansions.
     */
    public static final TableGenerator GAMES = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Games and Expansions";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            t.setColumns("Game", "Expansion", "Code");
            for (Game g : Game.getGames(true)) {
                t.add(g.getUIName(), "", g.getCode());
                for (Expansion e : Expansion.getExpansionsForGame(g, false)) {
                    t.add("", e.getUIName(), e.getCode());
                }
            }
            return t;
        }
    };

    /**
     * Standard table of platform-specific information.
     */
    public static final TableGenerator PLATFORM = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Host Platform";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            stdcols(t);
            OperatingSystemMXBean b = ManagementFactory.getOperatingSystemMXBean();
            Properties p = System.getProperties();
            try {
                t.add("<html><b>System", "");
                t.add("Operating System", format("%s (version %s)", b.getName(), b.getVersion()));
                t.add("Patch Level", p.getProperty("sun.os.patch.level"));
                t.add("Architecture", format("%s (%d CPU%s)", b.getArch(), b.getAvailableProcessors(), b.getAvailableProcessors() > 1 ? "s" : ""));
                t.add("Endianess", p.getProperty("sun.cpu.endian"));

                t.add("<html><b>Current Context", "");
                t.add("User Name", p.getProperty("user.name"));
                t.add("Home Directory", p.getProperty("user.home"));
                t.add("Current Directory", p.getProperty("user.dir"));
                t.add("Temp Directory", p.getProperty("java.io.tmpdir"));
                t.add("Country", p.getProperty("user.country"));
                t.add("Language", p.getProperty("user.language"));
                t.add("Time Zone", TimeZone.getDefault().getDisplayName(false, TimeZone.LONG));

                t.add("<html><b>File System", "");
                t.add("Separator", File.separator);
                t.add("Path Separator", File.pathSeparator);
                File[] roots = File.listRoots();
                StringBuilder s = new StringBuilder(roots.length * 5 + 8);
                for (int i = 0; i < roots.length; ++i) {
                    if (i > 0) {
                        s.append(", ");
                    }
                    s.append(roots[i]);
                }
                t.add("Root Partitions", s.toString());

                t.add("<html><b>Network Connectivity", "");
                try {
                    String localhost = InetAddress.getLocalHost().getCanonicalHostName();
                    InetAddress ips[] = InetAddress.getAllByName(localhost);
                    t.add("Host Name", localhost);
                    t.add("IP Addresses", valueOf(ips.length));
                    for (int i = 0; i < ips.length; ++i) {
                        t.add(format("Address %d", i + 1), ips[i].getHostAddress());
                    }
                    t.add("Proxy Mode", NetworkProxy.getProxyType().toString().toLowerCase().replace('_', ' '));
                    String proxyServer = NetworkProxy.getServer();
                    if (proxyServer != null && !proxyServer.isEmpty()) {
                        t.add("Proxy Server", format("%s:%d", proxyServer, NetworkProxy.getPort()));
                    } else {
                        t.add("Proxy Server", "");
                    }
                    t.add("Proxy User", NetworkProxy.getUser());
                } catch (java.net.UnknownHostException e) {
                    t.add("Host Name", "no local host found");
                }

                t.add("<html><b>Human Interface", "");
                String theme = Settings.getShared().get("look-and-feel-theme");
                if (theme == null || theme.isEmpty()) {
                    theme = "ca.cgjennings.ui.DagonTheme";
                }
                if (!Settings.getShared().get("default-look-and-feel").equals("auto")) {
                    theme = Settings.getShared().get("default-look-and-feel");
                }
                t.add("Theme", theme);
                t.add("Swing L&F Class", UIManager.getLookAndFeel().getDescription());
                Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
                t.add("Display Size", format("%,d \u00d7 %,d", d.width, d.height));
                try {
                    java.awt.Robot test = new java.awt.Robot();
                    t.add("AWT Robots", "Yes");
                } catch (java.awt.AWTException e) {
                    t.add("AWT Robots", "No");
                }
                t.add("Mouse Buttons", valueOf(java.awt.MouseInfo.getNumberOfButtons()));
            } catch (SecurityException e) {
                return InfoTable.errorTable(null, e);
            }

            return t;
        }
    };

    /**
     * Standard table of memory management details.
     */
    public static final TableGenerator MEMORY = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Memory Management";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            stdcols(t);
            MemoryMXBean m = ManagementFactory.getMemoryMXBean();

            float heap, nonheap;
            heap = m.getHeapMemoryUsage().getUsed() / 1024f;
            nonheap = m.getNonHeapMemoryUsage().getUsed() / 1024f;
            t.add("In Use", format("%,.0f kB (%,.0f kB heap, %,.0f kB non-heap)", heap + nonheap, heap, nonheap));
            heap = m.getHeapMemoryUsage().getInit() / 1024f;
            nonheap = m.getNonHeapMemoryUsage().getInit() / 1024f;
            t.add("Initial Size", format("%,.0f kB (%,.0f kB heap, %,.0f kB non-heap)", heap + nonheap, heap, nonheap));
            heap = m.getHeapMemoryUsage().getCommitted() / 1024f;
            nonheap = m.getNonHeapMemoryUsage().getCommitted() / 1024f;
            t.add("Committed Size", format("%,.0f kB (%,.0f kB heap, %,.0f kB non-heap)", heap + nonheap, heap, nonheap));
            heap = m.getHeapMemoryUsage().getMax() / 1024f;
            nonheap = m.getNonHeapMemoryUsage().getMax() / 1024f;
            t.add("Maximum Size", format("%,.0f kB (%,.0f kB heap, %,.0f kB non-heap)", heap + nonheap, heap, nonheap));

            long pool = 1;
            t.section("Memory Pools");
            List<MemoryPoolMXBean> p = ManagementFactory.getMemoryPoolMXBeans();
            t.add("Number of Pools", valueOf(p.size()));
            for (MemoryPoolMXBean b : p) {
                t.add(format("Pool %d", pool++), format("(%s) %,.0f kB in use (%,.0f kB peak use)", b.getName(),
                        (float) b.getUsage().getUsed(), (float) b.getPeakUsage().getUsed()));

                t.add("Usage Threshold", b.isUsageThresholdSupported()
                        ? (format("%,.0f kb, exceeded %d times", b.getUsageThreshold() / 1024f, b.getUsageThresholdCount()))
                        : ("Unsupported")
                );

                t.add("Collection Threshold", b.isCollectionUsageThresholdSupported()
                        ? (format("%,.0f kb, exceeded %d times", b.getCollectionUsageThreshold() / 1024f, b.getCollectionUsageThresholdCount()))
                        : ("Unsupported")
                );

                String names[] = b.getMemoryManagerNames();
                StringBuilder buff = new StringBuilder(names.length * 25 + 8);
                if (names.length >= 1) {
                    buff.append(names[0]);
                    for (int i = 1; i < names.length; ++i) {
                        buff.append(", ");
                        buff.append(names[i]);
                    }
                }
                t.add("Manager(s)", buff.toString());
            }

            t.section("Garbage Collection");

            List<GarbageCollectorMXBean> l = ManagementFactory.getGarbageCollectorMXBeans();
            MemoryMXBean b = ManagementFactory.getMemoryMXBean();
            long totalCollections = 0;
            long totalTime = 0;
            long number = 1;
            t.add("Pending Finalizations", format("%d (approximate)", b.getObjectPendingFinalizationCount()));
            t.add("Number of Collectors", valueOf(l.size()));
            for (GarbageCollectorMXBean g : l) {
                totalCollections += g.getCollectionCount();
                totalTime += g.getCollectionTime();
                t.add(format("Collector %d", number++), format("(%s) %d collections in %d ms", g.getName(), g.getCollectionCount(), g.getCollectionTime()));
                String[] pools = g.getMemoryPoolNames();
                StringBuilder pb = new StringBuilder(pools.length * 25 + 8);
                for (int i = 0; i < pools.length; ++i) {
                    if (i > 0) {
                        pb.append(", ");
                    }
                    pb.append(pools[i]);
                }
                t.add("Pools Managed", pb.toString());
            }

            float avg = totalTime / (float) totalCollections;
            if (avg != avg) {
                avg = 0f;
            }
            t.add("Summary", format("%d collections in %d ms (mean %,.2f ms/collection)", totalCollections, totalTime, avg));
            return t;
        }
    };

    /**
     * Standard table of thread states.
     */
    public static final TableGenerator THREADS = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Threads";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            stdcols(t);
            ThreadMXBean b = ManagementFactory.getThreadMXBean();
            t.add("Active Threads", format("%d (%d daemon)", b.getThreadCount(), b.getDaemonThreadCount()));
            t.add("Total Threads", format("%d peak, %d started in life of VM", b.getPeakThreadCount(), b.getTotalStartedThreadCount()));

            long[] list = b.findMonitorDeadlockedThreads();
            if (list == null) {
                t.add("Monitor Deadlocks", "none found");
            } else {
                StringBuilder s = new StringBuilder(list.length * 30 + 8);
                s.append(list.length);
                s.append(" (");
                s.append(list[0]);
                for (int i = 1; i < list.length; ++i) {
                    s.append(", ");
                    s.append(list[i]);
                }
                s.append(")");
                t.add("Monitor Deadlocks", s.toString());
            }

            String timing = null;
            if (!b.isThreadCpuTimeSupported()) {
                timing = "(Timing Unsupported)";
            } else if (!b.isThreadCpuTimeEnabled()) {
                timing = "(Timing Disabled)";
            }

            long threads[] = b.getAllThreadIds();

            for (int i = 0; i < threads.length; ++i) {
                ThreadInfo info = b.getThreadInfo(threads[i], 1);
                if (timing == null) {
                    t.add(format("<html><b>Thread %d", i + 1), format("Name \"%s\", ID %d [%s] (%,.4f s CPU, %,.4f s user)",
                            info.getThreadName(), threads[i], info.getThreadState().toString(),
                            b.getThreadCpuTime(threads[i]) * 1e-9f, b.getThreadUserTime(threads[i]) * 1e-9f));
                } else {
                    t.add(format("<html><b>Thread %d", i + 1), format("Name \"%s\", ID %d [%s] %s",
                            info.getThreadName(), threads[i], timing, info.getThreadState().toString(), timing));
                }

                if (b.isThreadContentionMonitoringSupported() && b.isThreadContentionMonitoringEnabled()) {
                    t.add("", format("Blocked %d times (for %,.4f s), Waited %d times (for %,.4f s)",
                            info.getBlockedCount(), info.getBlockedTime() / 1000f,
                            info.getWaitedCount(), info.getWaitedTime() / 1000f)
                    );
                } else {
                    t.add("Contention History", format("Blocked %d times, Waited %d times",
                            info.getBlockedCount(), info.getWaitedCount()));
                }

                String lockedOn = info.getLockName();
                if (lockedOn != null) {
                    long lockedBy = info.getLockOwnerId();
                    t.add("Blocking on Monitor",
                            (lockedBy != -1) ? format("\"%s\" (owned by thread ID %d)", lockedOn, lockedBy)
                                    : format("\"%s\"", lockedOn));
                }

                StackTraceElement[] trace = info.getStackTrace();
                t.add("Executing Method", (trace.length != 0) ? trace[0].toString() : "Unavailable");
            }

            return t;
        }
    };

    /**
     * Standard table of virtual machine information.
     */
    public static final TableGenerator JVM = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Virtual Machine";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            stdcols(t);
            RuntimeMXBean b = ManagementFactory.getRuntimeMXBean();
            Properties p = System.getProperties();

            t.add("Instance Name", format("%s (running for %,.2f s)", b.getName(), b.getUptime() / 1000f));

            CompilationMXBean c = ManagementFactory.getCompilationMXBean();
            if (c.isCompilationTimeMonitoringSupported()) {
                t.add("JIT Compiler", format("%s (%,.4f s spent compiling)", c.getName(), c.getTotalCompilationTime() / 1000f));
            } else {
                t.add("JIT Compiler", c.getName());
            }

            ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
            t.add("Class Loader", format("%d classes loaded (%d currently loaded, %d unloaded)",
                    cl.getTotalLoadedClassCount(), cl.getLoadedClassCount(), cl.getUnloadedClassCount()));

            t.add("<html><b>Runtime");
            t.add("Name", p.getProperty("java.runtime.name"));
            t.add("Specification", format("%s (%s) version %s",
                    p.getProperty("java.specification.name"), p.getProperty("java.specification.vendor"), p.getProperty("java.specification.version")));
            t.add("Vendor", format("%s (%s) version %s",
                    p.getProperty("java.vendor"), p.getProperty("java.vendor.url"), p.getProperty("java.version")));

            t.add("<html><b>Virtual Machine");
            t.add("Name", p.getProperty("java.vm.name"));
            t.add("Specification", format("%s (%s) version %s",
                    b.getSpecName(), b.getSpecVendor(), b.getSpecVersion()));
            t.add("Vendor", format("%s (%s) version %s",
                    b.getVmName(), b.getVmVendor(), b.getVmVersion()));
            t.add("Information", p.getProperty("java.vm.info"));

            t.add("Management Spec.", format("version %s", b.getManagementSpecVersion()));

            t.add("Java Installation", p.getProperty("java.home"));
            t.add("Boot Class Path", b.isBootClassPathSupported() ? b.getBootClassPath() : "not supported");
            t.add("Class Path", b.getClassPath());
            t.add("Library Path", b.getLibraryPath());
            t.add("Extension Path", p.getProperty("java.ext.dirs"));

            StringBuilder args = new StringBuilder(128);
            for (String arg : b.getInputArguments()) {
                args.append(arg);
                args.append(' ');
            }
            t.add("Command Line Options", args.toString());
            return t;
        }
    };

    /**
     * Standard table of font information.
     */
    public static final TableGenerator FONTS = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Typefaces";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            t.setColumns("Family Names");
            String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (int i = 0; i < fonts.length; ++i) {
                t.add(fonts[i]);
            }
            return t;
        }
    };

    public static final TableGenerator CACHES = new TableGenerator() {
        @Override
        public String getTableName() {
            return "Cache Metrics";
        }

        @Override
        public InfoTable generateTable() {
            InfoTable t = new InfoTable();
            t.setColumns("Name", "Type", "Metrics");
            for (CacheMetrics cm : ResourceKit.getRegisteredCacheMetrics()) {
                t.add(cm.toString(), cm.getContentType().getSimpleName(), cm.status());
            }
            return t;
        }
    };

    static {
        register(CACHES);
        register(SETTINGS);
        register(GAMES);
        register(PLATFORM);
        register(MEMORY);
        register(THREADS);
        register(JVM);
        register(FONTS);
    }
}
