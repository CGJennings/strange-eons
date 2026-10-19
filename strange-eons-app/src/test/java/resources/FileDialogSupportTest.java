package resources;

import ca.cgjennings.ui.FileNameExtensionFilter;
import ca.cgjennings.ui.filedialog.FileDialogProvider;
import ca.cgjennings.ui.filedialog.FileDialogRequest;
import ca.cgjennings.ui.filedialog.FileDialogResult;
import ca.cgjennings.ui.filedialog.NativeFileDialogProvider;
import ca.cgjennings.ui.filedialog.NativeFileDialogProvider.Platform;
import ca.cgjennings.ui.filedialog.NfdFileDialogProvider;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDialogSupportTest {

    @TempDir
    Path tmp;

    /** Answers each dialog with the next scripted file, or cancels. */
    private static class ScriptedProvider implements FileDialogProvider {

        final Deque<File> answers = new ArrayDeque<>();
        final List<File> shownWith = new ArrayList<>();
        File startedIn;
        boolean confirms;

        @Override
        public FileDialogResult show(Component parent, FileDialogRequest request) {
            if (startedIn == null) {
                startedIn = request.getDirectory();
            }
            shownWith.add(request.getSelectedFile());
            File f = answers.poll();
            return f == null ? null : new FileDialogResult(new File[]{f}, f.getParentFile());
        }

        @Override
        public boolean confirmsReplace() {
            return confirms;
        }
    }

    private final Map<String, String> folders = new HashMap<>();
    private final Deque<Integer> confirmAnswers = new ArrayDeque<>();
    private final List<File> confirmed = new ArrayList<>();
    private final ScriptedProvider provider = new ScriptedProvider();
    private final FileDialogSupport support = new FileDialogSupport(
            new FileDialogSupport.FolderStore() {
        @Override
        public String get(String key) {
            return folders.get(key);
        }

        @Override
        public void set(String key, String path) {
            folders.put(key, path);
        }
    },
            f -> {
                confirmed.add(f);
                return confirmAnswers.poll();
            });

    private static final FileFilter EON = new FileNameExtensionFilter("Components", "eon");

    private static FileDialogRequest open() {
        return new FileDialogRequest(FileDialogRequest.Mode.OPEN);
    }

    private static FileDialogRequest save() {
        return new FileDialogRequest(FileDialogRequest.Mode.SAVE).filter(EON);
    }

    private File existing(String name) throws IOException {
        File f = tmp.resolve(name).toFile();
        assertTrue(f.createNewFile());
        return f;
    }

    @Test
    void openStartsInAndRemembersFolder() {
        File start = tmp.resolve("start").toFile();
        folders.put("k", start.getPath());
        File chosen = tmp.resolve("other/a.eon").toFile();
        provider.answers.add(chosen);

        FileDialogResult r = support.open(null, provider, "k", true, open());

        assertEquals(start, provider.startedIn);
        assertEquals(chosen, r.getFile());
        assertEquals(chosen.getParentFile().getAbsolutePath(), folders.get("k"));
    }

    @Test
    void openCanSkipRemembering() {
        provider.answers.add(tmp.resolve("a.eon").toFile());
        support.open(null, provider, "k", false, open());
        assertFalse(folders.containsKey("k"));
    }

    @Test
    void openKeepsRequestedFolder() {
        folders.put("k", "/stored");
        File requested = tmp.toFile();
        support.open(null, provider, "k", true, open().directory(requested));
        assertEquals(requested, provider.startedIn);
    }

    @Test
    void openWithoutStoredFolder() {
        assertNull(support.open(null, provider, "k", true, open()));
        assertNull(provider.startedIn);
        assertFalse(folders.containsKey("k"));
    }

    @Test
    void saveCancelled() {
        assertNull(support.save(null, provider, "k", null, save()));
        assertTrue(confirmed.isEmpty());
    }

    @Test
    void saveNewFileAddsExtension() {
        File chosen = tmp.resolve("card").toFile();
        provider.answers.add(chosen);

        File f = support.save(null, provider, "k", null, save());

        assertEquals(tmp.resolve("card.eon").toFile(), f);
        assertEquals(tmp.toFile().getAbsolutePath(), folders.get("k"));
        assertTrue(confirmed.isEmpty());
    }

    @Test
    void saveConfirmsReplaceWhenDialogDoesNot() throws IOException {
        File f = existing("card.eon");
        provider.answers.add(f);
        confirmAnswers.add(JOptionPane.YES_OPTION);

        assertEquals(f, support.save(null, provider, "k", null, save()));
        assertEquals(List.of(f), confirmed);
    }

    @Test
    void saveTrustsDialogThatConfirms() throws IOException {
        File f = existing("card.eon");
        provider.confirms = true;
        provider.answers.add(f);

        assertEquals(f, support.save(null, provider, "k", null, save()));
        assertTrue(confirmed.isEmpty());
    }

    @Test
    void saveConfirmsWhenAddedExtensionHitsExistingFile() throws IOException {
        File f = existing("card.eon");
        provider.confirms = true;
        provider.answers.add(tmp.resolve("card").toFile());
        confirmAnswers.add(JOptionPane.YES_OPTION);

        assertEquals(f, support.save(null, provider, "k", null, save()));
        assertEquals(List.of(f), confirmed);
    }

    @Test
    void saveDoesNotConfirmUnconfirmedFile() throws IOException {
        File f = existing("card.eon");
        provider.answers.add(f);

        assertEquals(f, support.save(null, provider, "k", f, save()));
        assertTrue(confirmed.isEmpty());
    }

    @Test
    void saveCancelledAtConfirmation() throws IOException {
        provider.answers.add(existing("card.eon"));
        confirmAnswers.add(JOptionPane.CANCEL_OPTION);

        assertNull(support.save(null, provider, "k", null, save()));
        assertFalse(folders.containsKey("k"));
    }

    @Test
    void saveAsksAgainAfterNo() throws IOException {
        File first = existing("card.eon");
        File second = tmp.resolve("other.eon").toFile();
        provider.answers.add(first);
        provider.answers.add(second);
        confirmAnswers.add(JOptionPane.NO_OPTION);

        assertEquals(second, support.save(null, provider, "k", null, save().selectedFile(new File("card.eon"))));
        assertEquals(List.of(new File("card.eon"), first), provider.shownWith);
    }

    @Test
    void withExtensionKeepsMatchingNames() {
        File f = new File("/x/Card.V2.EON");
        assertSame(f, FileDialogSupport.withExtension(f, EON));
    }

    @Test
    void withExtensionUsesFirstExtension() {
        FileFilter images = new FileNameExtensionFilter("Images", "png", "jpg");
        assertEquals(new File("/x/a.png"), FileDialogSupport.withExtension(new File("/x/a"), images));
        assertEquals(new File("/x/a.jpg"), FileDialogSupport.withExtension(new File("/x/a.jpg"), images));
    }

    @Test
    void withExtensionNeedsExtensionFilter() {
        File f = new File("/x/a");
        assertSame(f, FileDialogSupport.withExtension(f, null));
        assertSame(f, FileDialogSupport.withExtension(f, new FileNameExtensionFilter("Any", "")));
    }

    @Test
    void rememberIgnoresMissingFolder() {
        support.remember("k", null);
        assertFalse(folders.containsKey("k"));
    }

    @Test
    void selectsBuiltInWhenNativeDisabled() {
        assertSame(provider, FileDialogSupport.select(provider, false, Platform.WINDOWS, () -> {
            throw new AssertionError();
        }));
    }

    @Test
    void selectsAwtDialogOnMac() {
        assertInstanceOf(NativeFileDialogProvider.class, FileDialogSupport.select(provider, true, Platform.MAC, () -> {
            throw new AssertionError();
        }));
    }

    @Test
    void selectsNfdElsewhere() {
        NfdFileDialogProvider nfd = new NfdFileDialogProvider(provider);
        for (Platform p : new Platform[]{Platform.WINDOWS, Platform.OTHER}) {
            FileDialogProvider selected = FileDialogSupport.select(provider, true, p, () -> nfd);
            assertInstanceOf(NfdFileDialogProvider.class, selected);
            assertTrue(selected != nfd);
        }
    }

    @Test
    void rejectsMissingCollaborators() {
        assertThrows(NullPointerException.class, () -> new FileDialogSupport(null, f -> 0));
        assertThrows(NullPointerException.class, () -> new FileDialogSupport(new FileDialogSupport.FolderStore() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void set(String key, String path) {
            }
        }, null));
    }

    private File projectFolder(String name) throws IOException {
        File folder = tmp.resolve(name).toFile();
        assertTrue(folder.mkdir());
        java.nio.file.Files.writeString(folder.toPath().resolve("seproject"), "type = PROJECT\n");
        return folder;
    }

    @Test
    void projectFilterAcceptsSettingsFilePackagesAndFolders() throws IOException {
        FileFilter f = FileDialogSupport.PROJECT_FILE_FILTER;
        File project = projectFolder("p");
        assertTrue(f.accept(project));
        assertTrue(f.accept(new File(project, "seproject")));
        assertTrue(f.accept(tmp.resolve("game.seproject").toFile()));
        assertFalse(f.accept(existing("card.eon")));
        assertFalse(f.accept(tmp.resolve("seproject.txt").toFile()));
        assertEquals("seproject", f.getDescription());
    }

    @Test
    void projectForSettingsFileIsItsFolder() throws IOException {
        File project = projectFolder("p");
        assertEquals(project, FileDialogSupport.projectFor(new File(project, "seproject")));
    }

    @Test
    void projectForFolder() throws IOException {
        File project = projectFolder("p");
        assertEquals(project, FileDialogSupport.projectFor(project));
    }

    @Test
    void projectForPackage() throws IOException {
        File pkg = existing("game.seproject");
        assertEquals(pkg, FileDialogSupport.projectFor(pkg));
    }

    @Test
    void projectForOtherFilesIsNull() throws IOException {
        File plain = tmp.resolve("plain").toFile();
        assertTrue(plain.mkdir());
        assertNull(FileDialogSupport.projectFor(plain));
        assertNull(FileDialogSupport.projectFor(existing("card.eon")));
        assertNull(FileDialogSupport.projectFor(new File("seproject")));
    }

    @Test
    void projectForTaskFolderIsNull() throws IOException {
        File task = tmp.resolve("task").toFile();
        assertTrue(task.mkdir());
        java.nio.file.Files.writeString(task.toPath().resolve("seproject"), "type = FACTORY\n");
        assertNull(FileDialogSupport.projectFor(new File(task, "seproject")));
    }

    @Test
    void extensionsOfBothFilterTypes() {
        assertArrayEquals(new String[]{"png", "jpg"},
                ca.cgjennings.ui.filedialog.FileFilters.extensions(new FileNameExtensionFilter("Images", "png", "", "JPG")));
        assertArrayEquals(new String[]{"txt"},
                ca.cgjennings.ui.filedialog.FileFilters.extensions(new javax.swing.filechooser.FileNameExtensionFilter("Text", "txt")));
        assertNull(ca.cgjennings.ui.filedialog.FileFilters.extensions(null));
        assertArrayEquals(new String[]{"jpg"}, new FileNameExtensionFilter("Images", "JPG").getExtensions());
    }
}
