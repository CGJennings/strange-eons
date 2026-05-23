package ca.cgjennings.apps.arkham.plugins.engine;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Optional, gated counters and timers around script engine construction
 * and evaluation. Enabled via the {@code strange-eons.script-perf-log}
 * system property; otherwise every method here is a no-op and adds no
 * meaningful overhead. Intended for ad-hoc performance investigation,
 * not as a permanent feature.
 */
public final class ScriptEngineMetrics {

    public static final boolean ENABLED = Boolean.getBoolean("strange-eons.script-perf-log");

    /**
     * Optional file path to also append metrics events to, set via
     * {@code -Dstrange-eons.script-perf-file=/path/to/file}. Falls back to
     * {@link StrangeEons#log} when unset, which can disappear if the harness
     * is run with stdio redirection that the host swallows.
     */
    private static final String FILE_PATH = System.getProperty("strange-eons.script-perf-file");

    private static final AtomicLong engineCount = new AtomicLong();
    private static final AtomicLong engineNanos = new AtomicLong();
    private static final AtomicLong monkeyCount = new AtomicLong();
    private static final AtomicLong monkeyNanos = new AtomicLong();
    private static final AtomicLong evalCount = new AtomicLong();
    private static final AtomicLong evalNanos = new AtomicLong();

    private ScriptEngineMetrics() {
    }

    private static void emit(String message) {
        StrangeEons.log.log(Level.INFO, message);
        if (FILE_PATH != null) {
            Path path = Paths.get(FILE_PATH);
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    PrintWriter pw = new PrintWriter(w)) {
                pw.println(System.currentTimeMillis() + " " + message);
            } catch (IOException ignored) {
            }
        }
    }

    public static void engineCreated(long nanos) {
        if (!ENABLED) {
            return;
        }
        long n = engineCount.incrementAndGet();
        long total = engineNanos.addAndGet(nanos);
        emit("script-perf: engine #" + n + " created in " + (nanos / 1_000_000L)
                + " ms (cumulative " + (total / 1_000_000L) + " ms)");
    }

    public static void scriptMonkeyCreated(long nanos) {
        if (!ENABLED) {
            return;
        }
        long n = monkeyCount.incrementAndGet();
        long total = monkeyNanos.addAndGet(nanos);
        emit("script-perf: ScriptMonkey #" + n + " created in " + (nanos / 1_000_000L)
                + " ms (cumulative " + (total / 1_000_000L) + " ms)");
        if (n % 25 == 0) {
            summary();
        }
    }

    public static void evalRecorded(String filename, long nanos) {
        if (!ENABLED) {
            return;
        }
        long n = evalCount.incrementAndGet();
        long total = evalNanos.addAndGet(nanos);
        emit("script-perf: eval #" + n + " " + filename + " in " + (nanos / 1_000_000L)
                + " ms (cumulative " + (total / 1_000_000L) + " ms)");
    }

    public static void summary() {
        if (!ENABLED) {
            return;
        }
        long ec = engineCount.get();
        long mc = monkeyCount.get();
        long evc = evalCount.get();
        long avgEngine = ec > 0 ? (engineNanos.get() / 1_000_000L / ec) : 0L;
        long avgMonkey = mc > 0 ? (monkeyNanos.get() / 1_000_000L / mc) : 0L;
        long ch = CompiledScriptCache.hits();
        long cm = CompiledScriptCache.misses();
        long total = ch + cm;
        long pct = total > 0 ? (ch * 100L / total) : 0L;
        emit("script-perf summary: " + ec + " engines (" + (engineNanos.get() / 1_000_000L)
                + " ms, avg " + avgEngine + " ms), " + mc + " ScriptMonkeys ("
                + (monkeyNanos.get() / 1_000_000L) + " ms, avg " + avgMonkey + " ms), "
                + evc + " evals (" + (evalNanos.get() / 1_000_000L) + " ms), cache "
                + ch + "/" + total + " hits (" + pct + "%, " + CompiledScriptCache.size() + " entries)");
    }
}
