package ca.cgjennings.ui.filedialog;

import ca.cgjennings.ui.filedialog.NfdFileDialogProvider.Backend;
import ca.cgjennings.ui.filedialog.NfdFileDialogProvider.Filter;
import ca.cgjennings.ui.filedialog.NfdFileDialogProvider.Outcome;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfdFileDialogProviderTest {

    @TempDir
    Path tmp;

    /** Records calls and answers with a scripted outcome. */
    private static class FakeBackend implements Backend {

        String initError;
        int initCalls;
        Outcome outcome = Outcome.cancel();
        RuntimeException failure;
        String call;
        long parent;
        List<Filter> filters;
        String defaultPath;
        String defaultName;
        boolean multiple;
        final List<String> threads = new ArrayList<>();

        @Override
        public String init() {
            ++initCalls;
            threads.add(Thread.currentThread().getName());
            return initError;
        }

        private Outcome answer(String call, long parent, String defaultPath) {
            this.call = call;
            this.parent = parent;
            this.defaultPath = defaultPath;
            threads.add(Thread.currentThread().getName());
            if (failure != null) {
                throw failure;
            }
            return outcome;
        }

        @Override
        public Outcome open(long parent, List<Filter> filters, String defaultPath, boolean multiple) {
            this.filters = filters;
            this.multiple = multiple;
            return answer("open", parent, defaultPath);
        }

        @Override
        public Outcome save(long parent, List<Filter> filters, String defaultPath, String defaultName) {
            this.filters = filters;
            this.defaultName = defaultName;
            return answer("save", parent, defaultPath);
        }

        @Override
        public Outcome pickFolder(long parent, String defaultPath) {
            return answer("folder", parent, defaultPath);
        }
    }

    /** Fallback that records whether it was used. */
    private static class FallbackProvider implements FileDialogProvider {

        FileDialogRequest request;
        final FileDialogResult answer = new FileDialogResult(new File[]{new File("fallback")}, null);

        @Override
        public FileDialogResult show(Component parent, FileDialogRequest request) {
            this.request = request;
            return answer;
        }

        @Override
        public boolean confirmsReplace() {
            return false;
        }
    }

    private final FakeBackend backend = new FakeBackend();
    private final FallbackProvider fallback = new FallbackProvider();
    private final ExecutorService thread = Executors.newSingleThreadExecutor(r -> new Thread(r, "dialog-test"));
    private Window handleWindow;

    @AfterEach
    void stopThread() {
        thread.shutdownNow();
    }

    private NfdFileDialogProvider provider() {
        return new NfdFileDialogProvider(backend, w -> {
            handleWindow = w;
            return 42L;
        }, thread, fallback);
    }

    private static FileDialogRequest open() {
        return new FileDialogRequest(FileDialogRequest.Mode.OPEN);
    }

    private static FileDialogRequest save() {
        return new FileDialogRequest(FileDialogRequest.Mode.SAVE);
    }

    @Test
    void opensOnDialogThread() {
        File file = tmp.resolve("a.eon").toFile();
        backend.outcome = Outcome.ok(List.of(file.getPath()));

        FileDialogResult r = provider().show(new JPanel(), open()
                .filter(new FileNameExtensionFilter("Components", "eon", "seplugin"))
                .directory(tmp.toFile()));

        assertEquals("open", backend.call);
        assertEquals(42L, backend.parent);
        assertNull(handleWindow);
        assertEquals(List.of(new Filter("Components", "eon,seplugin")), backend.filters);
        assertEquals(tmp.toFile().getAbsolutePath(), backend.defaultPath);
        assertFalse(backend.multiple);
        assertEquals(List.of("dialog-test", "dialog-test"), backend.threads);
        assertEquals(file, r.getFile());
        assertEquals(tmp.toFile(), r.getDirectory());
        assertNull(fallback.request);
    }

    @Test
    void initializesOnce() {
        NfdFileDialogProvider p = provider();
        p.show(null, open());
        p.show(null, open());
        assertEquals(1, backend.initCalls);
    }

    @Test
    void withFallbackSharesInitialization() {
        backend.initError = "no portal";
        NfdFileDialogProvider p = provider();
        FallbackProvider other = new FallbackProvider();
        NfdFileDialogProvider q = p.withFallback(other);

        assertSame(fallback.answer, p.show(null, open()));
        assertSame(other.answer, q.show(null, open()));
        assertEquals(1, backend.initCalls);
        assertThrows(NullPointerException.class, () -> p.withFallback(null));
    }

    @Test
    void opensMultipleFiles() {
        backend.outcome = Outcome.ok(List.of("/x/a.eon", "/x/b.eon"));

        FileDialogResult r = provider().show(null, open().multiSelection(true));

        assertTrue(backend.multiple);
        assertArrayEquals(new File[]{new File("/x/a.eon"), new File("/x/b.eon")}, r.getFiles());
    }

    @Test
    void singleSelectionKeepsFirstPath() {
        backend.outcome = Outcome.ok(List.of("/x/a.eon", "/x/b.eon"));
        assertArrayEquals(new File[]{new File("/x/a.eon")}, provider().show(null, open()).getFiles());
    }

    @Test
    void savesWithSuggestedName() {
        File suggested = tmp.resolve("sub/card.eon").toFile();
        backend.outcome = Outcome.ok(List.of(suggested.getPath()));

        FileDialogResult r = provider().show(null, save().selectedFile(suggested).multiSelection(true));

        assertEquals("save", backend.call);
        assertEquals("card.eon", backend.defaultName);
        assertEquals(suggested.getParentFile().getAbsolutePath(), backend.defaultPath);
        assertArrayEquals(new File[]{suggested}, r.getFiles());
    }

    @Test
    void savesWithoutSuggestion() {
        provider().show(null, save());
        assertNull(backend.defaultName);
        assertNull(backend.defaultPath);
        assertEquals(List.of(), backend.filters);
    }

    @Test
    void bareSuggestedNameKeepsRequestedFolder() {
        provider().show(null, save().directory(tmp.toFile()).selectedFile(new File("card.eon")));
        assertEquals(tmp.toFile().getAbsolutePath(), backend.defaultPath);
    }

    @Test
    void picksFolders() {
        File folder = tmp.resolve("project").toFile();
        backend.outcome = Outcome.ok(List.of(folder.getPath()));

        FileDialogResult r = provider().show(null, open().directoriesOnly(true)
                .filter(new FileNameExtensionFilter("x", "x")));

        assertEquals("folder", backend.call);
        assertEquals(folder, r.getFile());
        assertEquals(folder, r.getDirectory());
    }

    @Test
    void cancelReturnsNull() {
        assertNull(provider().show(null, open()));
        assertNull(fallback.request);
    }

    @Test
    void emptySuccessReturnsNull() {
        backend.outcome = Outcome.ok(List.of());
        assertNull(provider().show(null, open()));
    }

    @Test
    void dialogErrorUsesFallback() {
        backend.outcome = Outcome.error("boom");
        FileDialogRequest req = open();

        assertSame(fallback.answer, provider().show(null, req));
        assertSame(req, fallback.request);
    }

    @Test
    void backendExceptionUsesFallback() {
        backend.failure = new IllegalStateException("boom");
        assertSame(fallback.answer, provider().show(null, open()));
    }

    @Test
    void initFailureUsesFallbackWithoutRetrying() {
        backend.initError = "no portal";
        NfdFileDialogProvider p = provider();

        assertSame(fallback.answer, p.show(null, open()));
        assertSame(fallback.answer, p.show(null, open()));
        assertFalse(p.isAvailable());
        assertEquals(1, backend.initCalls);
        assertNull(backend.call);
    }

    @Test
    void dispatchesEventsWhileWaitingOnEventThread() throws Exception {
        File file = tmp.resolve("a.eon").toFile();
        AtomicReference<Boolean> ranDuringDialog = new AtomicReference<>(false);
        FakeBackend waiting = new FakeBackend() {
            @Override
            public Outcome open(long parent, List<Filter> filters, String defaultPath, boolean multiple) {
                // completes only if the event thread keeps dispatching
                try {
                    EventQueue.invokeAndWait(() -> ranDuringDialog.set(true));
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
                return Outcome.ok(List.of(file.getPath()));
            }
        };
        NfdFileDialogProvider p = new NfdFileDialogProvider(waiting, w -> 0L, thread, fallback);
        AtomicReference<FileDialogResult> result = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> result.set(p.show(null, open())));

        assertTrue(ranDuringDialog.get());
        assertEquals(file, result.get().getFile());
    }

    @Test
    void interruptedWaitIsAnError() {
        NfdFileDialogProvider p = provider();
        Thread.currentThread().interrupt();
        try {
            Outcome o = p.call(Outcome::cancel);
            assertEquals(Outcome.Status.ERROR, o.status());
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void rejectedTaskIsAnError() {
        thread.shutdown();
        assertEquals(Outcome.Status.ERROR, provider().call(Outcome::cancel).status());
    }

    @Test
    void filtersRequireExtensionFilter() {
        FileFilter custom = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return true;
            }

            @Override
            public String getDescription() {
                return "All";
            }
        };
        assertEquals(List.of(), NfdFileDialogProvider.filters(null));
        assertEquals(List.of(), NfdFileDialogProvider.filters(custom));
    }

    @Test
    void confirmsReplace() {
        assertTrue(provider().confirmsReplace());
    }

    @Test
    void rejectsMissingArguments() {
        assertThrows(NullPointerException.class, () -> new NfdFileDialogProvider(null));
        assertThrows(NullPointerException.class, () -> new NfdFileDialogProvider(null, w -> 0L, thread, fallback));
        assertThrows(NullPointerException.class, () -> new NfdFileDialogProvider(backend, null, thread, fallback));
        assertThrows(NullPointerException.class, () -> new NfdFileDialogProvider(backend, w -> 0L, null, fallback));
    }

    @Test
    void outcomeCopiesPaths() {
        List<String> paths = new ArrayList<>(List.of("a"));
        Outcome o = Outcome.ok(paths);
        paths.add("b");
        assertEquals(List.of("a"), o.paths());
    }
}
