package ca.cgjennings.ui.filedialog;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Shows file dialogs through nativefiledialog-extended. On Windows this is the
 * system file dialog; on Linux it is the desktop portal's dialog. If the native
 * library cannot be initialized, or a dialog fails, requests are passed to a
 * fallback provider.
 *
 * <p>
 * Dialogs run on a dedicated thread. When called from the event dispatch
 * thread, events continue to be dispatched until the dialog closes.
 */
public final class NfdFileDialogProvider implements FileDialogProvider {

    /**
     * A file type filter: a description and comma-separated extensions.
     */
    record Filter(String name, String spec) {

    }

    /**
     * The outcome of a native dialog.
     */
    record Outcome(Status status, List<String> paths, String error) {

        enum Status {
            OK, CANCEL, ERROR
        }

        static Outcome ok(List<String> paths) {
            return new Outcome(Status.OK, List.copyOf(paths), null);
        }

        static Outcome cancel() {
            return new Outcome(Status.CANCEL, List.of(), null);
        }

        static Outcome error(String error) {
            return new Outcome(Status.ERROR, List.of(), error);
        }
    }

    /**
     * The native dialog functions. All methods are called on the dialog
     * thread.
     */
    interface Backend {

        /**
         * Initializes the library.
         *
         * @return {@code null} on success, otherwise a description of the
         * failure
         */
        String init();

        Outcome open(long parent, List<Filter> filters, String defaultPath, boolean multiple);

        Outcome save(long parent, List<Filter> filters, String defaultPath, String defaultName);

        Outcome pickFolder(long parent, String defaultPath);
    }

    private final Backend backend;
    private final ToLongFunction<Window> windowHandles;
    private final ExecutorService dialogThread;
    private final FileDialogProvider fallback;
    private Boolean available;

    /**
     * Creates a provider for the current platform.
     *
     * @param fallback shows requests that cannot be shown natively
     */
    public NfdFileDialogProvider(FileDialogProvider fallback) {
        this(new LwjglNfdBackend(), JawtWindowHandles::handleOf, createDialogThread(), fallback);
    }

    NfdFileDialogProvider(Backend backend, ToLongFunction<Window> windowHandles, ExecutorService dialogThread, FileDialogProvider fallback) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.windowHandles = Objects.requireNonNull(windowHandles, "windowHandles");
        this.dialogThread = Objects.requireNonNull(dialogThread, "dialogThread");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    private static ExecutorService createDialogThread() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "native-file-dialog");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public FileDialogResult show(Component parent, FileDialogRequest request) {
        if (!isAvailable()) {
            return fallback.show(parent, request);
        }
        long handle = windowHandles.applyAsLong(NativeFileDialogProvider.windowFor(parent));
        Outcome outcome = call(() -> dispatch(handle, request));
        switch (outcome.status()) {
            case OK:
                return result(outcome, request);
            case CANCEL:
                return null;
            default:
                StrangeEons.log.log(Level.WARNING, "native file dialog failed: {0}", outcome.error());
                return fallback.show(parent, request);
        }
    }

    /**
     * Returns {@code true}: the native save dialogs ask before replacing a
     * file.
     */
    @Override
    public boolean confirmsReplace() {
        return true;
    }

    /**
     * Returns whether the native library initialized. Initialization is
     * attempted once, on first use.
     */
    synchronized boolean isAvailable() {
        if (available == null) {
            Outcome init = call(() -> {
                String error = backend.init();
                return error == null ? Outcome.cancel() : Outcome.error(error);
            });
            available = init.status() != Outcome.Status.ERROR;
            if (!available) {
                StrangeEons.log.log(Level.WARNING, "native file dialogs unavailable: {0}", init.error());
            }
        }
        return available;
    }

    private Outcome dispatch(long handle, FileDialogRequest request) {
        String defaultPath = defaultPath(request);
        if (request.isDirectoriesOnly()) {
            return backend.pickFolder(handle, defaultPath);
        }
        List<Filter> filters = filters(request.getFilter());
        if (request.getMode() == FileDialogRequest.Mode.SAVE) {
            File sel = request.getSelectedFile();
            return backend.save(handle, filters, defaultPath, sel == null ? null : sel.getName());
        }
        return backend.open(handle, filters, defaultPath, request.isMultiSelection());
    }

    /**
     * Runs a task on the dialog thread and waits for it, dispatching events
     * meanwhile if called from the event dispatch thread.
     */
    Outcome call(Callable<Outcome> task) {
        try {
            if (!EventQueue.isDispatchThread()) {
                return dialogThread.submit(task).get();
            }
            SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            Future<Outcome> f = dialogThread.submit(() -> {
                try {
                    return task.call();
                } finally {
                    loop.exit();
                }
            });
            loop.enter();
            return f.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Outcome.error(ex.toString());
        } catch (Exception ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            return Outcome.error(cause.toString());
        }
    }

    static String defaultPath(FileDialogRequest request) {
        File dir = request.getDirectory();
        File sel = request.getSelectedFile();
        if (sel != null && sel.getParentFile() != null) {
            dir = sel.getParentFile();
        }
        return dir == null ? null : dir.getAbsolutePath();
    }

    static List<Filter> filters(FileFilter filter) {
        if (!(filter instanceof FileNameExtensionFilter)) {
            return List.of();
        }
        FileNameExtensionFilter ef = (FileNameExtensionFilter) filter;
        return List.of(new Filter(ef.getDescription(), String.join(",", ef.getExtensions())));
    }

    static FileDialogResult result(Outcome outcome, FileDialogRequest request) {
        List<String> paths = outcome.paths();
        if (paths.isEmpty()) {
            return null;
        }
        File[] files;
        if (request.isMultiSelection() && request.getMode() == FileDialogRequest.Mode.OPEN) {
            files = paths.stream().map(File::new).toArray(File[]::new);
        } else {
            files = new File[]{new File(paths.get(0))};
        }
        File dir = request.isDirectoriesOnly() ? files[0] : files[0].getParentFile();
        return new FileDialogResult(files, dir);
    }
}
