package ca.cgjennings.ui.filedialog;

import ca.cgjennings.ui.filedialog.NativeFileDialogProvider.NativeDialog;
import ca.cgjennings.ui.filedialog.NativeFileDialogProvider.Platform;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import javax.swing.JPanel;
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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class NativeFileDialogProviderTest {

    @TempDir
    Path tmp;

    @AfterEach
    void clearProperty() {
        System.clearProperty(NativeFileDialogProvider.MAC_DIRECTORIES_PROPERTY);
    }

    /** Records configuration and answers with scripted files. */
    private static class FakeDialog implements NativeDialog {

        String title;
        int mode = -1;
        String directory;
        String file;
        FilenameFilter filter;
        boolean multiple;
        boolean shown;
        String directoriesPropertyWhenShown;
        File[] files = new File[0];
        String resultDirectory;

        @Override
        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public void setMode(int mode) {
            this.mode = mode;
        }

        @Override
        public void setDirectory(String dir) {
            this.directory = dir;
        }

        @Override
        public void setFile(String file) {
            this.file = file;
        }

        @Override
        public void setFilenameFilter(FilenameFilter filter) {
            this.filter = filter;
        }

        @Override
        public void setMultipleMode(boolean enable) {
            this.multiple = enable;
        }

        @Override
        public void show() {
            shown = true;
            directoriesPropertyWhenShown = System.getProperty(NativeFileDialogProvider.MAC_DIRECTORIES_PROPERTY);
        }

        @Override
        public File[] getFiles() {
            return files;
        }

        @Override
        public String getDirectory() {
            return resultDirectory;
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

    private FakeDialog dialog = new FakeDialog();
    private FallbackProvider fallback = new FallbackProvider();
    private Window owner;

    private NativeFileDialogProvider provider(Platform platform) {
        return new NativeFileDialogProvider(platform, fallback, w -> {
            owner = w;
            return dialog;
        });
    }

    private static FileDialogRequest open() {
        return new FileDialogRequest(FileDialogRequest.Mode.OPEN);
    }

    private static FileDialogRequest save() {
        return new FileDialogRequest(FileDialogRequest.Mode.SAVE);
    }

    @Test
    void opensFileAndRemembersItsFolder() {
        File file = tmp.resolve("a.eon").toFile();
        dialog.files = new File[]{file};
        dialog.resultDirectory = tmp.toString();

        FileDialogResult r = provider(Platform.MAC).show(null, open().title("Open").directory(tmp.toFile()));

        assertTrue(dialog.shown);
        assertEquals(FileDialog.LOAD, dialog.mode);
        assertEquals("Open", dialog.title);
        assertEquals(tmp.toFile().getAbsolutePath(), dialog.directory);
        assertNull(dialog.directoriesPropertyWhenShown);
        assertEquals(file, r.getFile());
        assertEquals(tmp.toFile(), r.getDirectory());
    }

    @Test
    void cancelReturnsNull() {
        assertNull(provider(Platform.WINDOWS).show(null, open()));
        dialog.files = null;
        assertNull(provider(Platform.WINDOWS).show(null, open()));
    }

    @Test
    void saveUsesSuggestedFileFolderAndName() {
        File suggested = tmp.resolve("sub/card.eon").toFile();
        provider(Platform.OTHER).show(null, save().directory(new File("elsewhere")).selectedFile(suggested).multiSelection(true));

        assertEquals(FileDialog.SAVE, dialog.mode);
        assertEquals(suggested.getParentFile().getAbsolutePath(), dialog.directory);
        assertEquals("card.eon", dialog.file);
        assertFalse(dialog.multiple);
    }

    @Test
    void bareSuggestedNameKeepsRequestedFolder() {
        provider(Platform.MAC).show(null, save().directory(tmp.toFile()).selectedFile(new File("card.eon")));

        assertEquals(tmp.toFile().getAbsolutePath(), dialog.directory);
        assertEquals("card.eon", dialog.file);
    }

    @Test
    void leavesDefaultsWhenRequestIsEmpty() {
        provider(Platform.OTHER).show(null, open());

        assertNull(dialog.title);
        assertNull(dialog.directory);
        assertNull(dialog.file);
        assertNull(dialog.filter);
    }

    @Test
    void multiSelectionReturnsAllFiles() {
        File a = new File("a.eon");
        File b = new File("b.eon");
        dialog.files = new File[]{a, b};

        FileDialogResult r = provider(Platform.MAC).show(null, open().multiSelection(true));

        assertTrue(dialog.multiple);
        assertArrayEquals(new File[]{a, b}, r.getFiles());
    }

    @Test
    void singleSelectionKeepsFirstFile() {
        File a = new File("a.eon");
        dialog.files = new File[]{a, new File("b.eon")};

        assertArrayEquals(new File[]{a}, provider(Platform.MAC).show(null, open()).getFiles());
    }

    @Test
    void folderFallsBackToParentOfFile() {
        File file = tmp.resolve("a.eon").toFile();
        dialog.files = new File[]{file};

        assertEquals(tmp.toFile(), provider(Platform.MAC).show(null, open()).getDirectory());
    }

    @Test
    void appliesFilterOnAllPlatforms() {
        FileFilter eon = new FileNameExtensionFilter("Components", "eon");
        for (Platform p : Platform.values()) {
            dialog = new FakeDialog();
            provider(p).show(null, open().filter(eon));
            assertTrue(dialog.filter.accept(tmp.toFile(), "card.eon"), p.name());
            assertFalse(dialog.filter.accept(tmp.toFile(), "card.zip"), p.name());
        }
    }

    @Test
    void windowsOpenUsesWildcardPattern() {
        provider(Platform.WINDOWS).show(null, open().filter(new FileNameExtensionFilter("Images", "png", "jpg")));
        assertEquals("*.png;*.jpg", dialog.file);
    }

    @Test
    void windowsSaveDoesNotUseWildcardPattern() {
        provider(Platform.WINDOWS).show(null, save().filter(new FileNameExtensionFilter("Components", "eon")));
        assertNull(dialog.file);
    }

    @Test
    void windowsSuggestedNameWinsOverPattern() {
        provider(Platform.WINDOWS).show(null, open().filter(new FileNameExtensionFilter("Components", "eon")).selectedFile(new File("a.eon")));
        assertEquals("a.eon", dialog.file);
    }

    @Test
    void otherPlatformsDoNotUseWildcardPattern() {
        provider(Platform.OTHER).show(null, open().filter(new FileNameExtensionFilter("Components", "eon")));
        assertNull(dialog.file);
    }

    @Test
    void windowsPatternRequiresExtensionFilter() {
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
        assertNull(NativeFileDialogProvider.windowsPattern(null));
        assertNull(NativeFileDialogProvider.windowsPattern(custom));
    }

    @Test
    void macSelectsFoldersNatively() {
        File folder = tmp.resolve("project").toFile();
        dialog.files = new File[]{folder};
        dialog.resultDirectory = tmp.toString();

        FileDialogResult r = provider(Platform.MAC).show(null, open().directoriesOnly(true).filter(new FileNameExtensionFilter("x", "x")));

        assertEquals("true", dialog.directoriesPropertyWhenShown);
        assertNull(System.getProperty(NativeFileDialogProvider.MAC_DIRECTORIES_PROPERTY));
        assertNull(dialog.filter);
        assertNull(fallback.request);
        assertEquals(folder, r.getFile());
        assertEquals(folder, r.getDirectory());
    }

    @Test
    void macRestoresExistingFolderProperty() {
        System.setProperty(NativeFileDialogProvider.MAC_DIRECTORIES_PROPERTY, "false");
        provider(Platform.MAC).show(null, open().directoriesOnly(true));

        assertEquals("true", dialog.directoriesPropertyWhenShown);
        assertEquals("false", System.getProperty(NativeFileDialogProvider.MAC_DIRECTORIES_PROPERTY));
    }

    @Test
    void otherPlatformsUseFallbackForFolders() {
        for (Platform p : new Platform[]{Platform.WINDOWS, Platform.OTHER}) {
            fallback = new FallbackProvider();
            dialog = new FakeDialog();
            FileDialogRequest req = open().directoriesOnly(true);

            FileDialogResult r = provider(p).show(null, req);

            assertSame(req, fallback.request, p.name());
            assertSame(fallback.answer, r, p.name());
            assertFalse(dialog.shown, p.name());
        }
    }

    @Test
    void ownerIsWindowOfParent() {
        provider(Platform.MAC).show(new JPanel(), open());
        assertNull(owner);
        assertNull(NativeFileDialogProvider.windowFor(null));
    }

    @Test
    void confirmsReplace() {
        assertTrue(provider(Platform.OTHER).confirmsReplace());
    }

    @Test
    void platformFromFlags() {
        assertEquals(Platform.MAC, Platform.of(true, false));
        assertEquals(Platform.WINDOWS, Platform.of(false, true));
        assertEquals(Platform.OTHER, Platform.of(false, false));
        assertTrue(Platform.current() != null);
    }

    @Test
    void ownerIsParentWhenParentIsWindow() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Frame f = new Frame();
        try {
            assertSame(f, NativeFileDialogProvider.windowFor(f));
            JPanel p = new JPanel();
            f.add(p);
            assertSame(f, NativeFileDialogProvider.windowFor(p));
        } finally {
            f.dispose();
        }
    }

    @Test
    void awtDialogWrapsFileDialog() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Frame f = new Frame();
        Dialog owned = new Dialog(f);
        try {
            NativeFileDialogProvider.AwtDialog d = (NativeFileDialogProvider.AwtDialog) NativeFileDialogProvider.createDialog(f);
            assertSame(f, d.fd.getOwner());
            assertSame(owned, ((NativeFileDialogProvider.AwtDialog) NativeFileDialogProvider.createDialog(owned)).fd.getOwner());
            NativeFileDialogProvider.createDialog(null);

            FilenameFilter filter = (dir, name) -> true;
            d.setTitle("T");
            d.setMode(FileDialog.SAVE);
            d.setDirectory(tmp.toString());
            d.setFile("a.eon");
            d.setFilenameFilter(filter);
            d.setMultipleMode(true);

            assertEquals("T", d.fd.getTitle());
            assertEquals(FileDialog.SAVE, d.fd.getMode());
            assertEquals(tmp.toString(), d.fd.getDirectory().replaceAll("[/\\\\]$", ""));
            assertEquals("a.eon", d.fd.getFile());
            assertSame(filter, d.fd.getFilenameFilter());
            assertTrue(d.fd.isMultipleMode());
            assertEquals(d.fd.getDirectory(), d.getDirectory());
            assertEquals(0, d.getFiles().length);
        } finally {
            owned.dispose();
            f.dispose();
        }
    }

    @Test
    void rejectsMissingArguments() {
        assertThrows(NullPointerException.class, () -> new NativeFileDialogProvider(null));
        assertThrows(NullPointerException.class, () -> new NativeFileDialogProvider(null, fallback, w -> dialog));
        assertThrows(NullPointerException.class, () -> new NativeFileDialogProvider(Platform.MAC, fallback, null));
    }
}
