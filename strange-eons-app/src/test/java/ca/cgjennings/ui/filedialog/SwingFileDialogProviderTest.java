package ca.cgjennings.ui.filedialog;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingFileDialogProviderTest {

    @TempDir
    Path tmp;

    /** Records which dialog was requested and answers without showing it. */
    private static class ScriptedChooser extends JFileChooser {

        private final int option;
        private String shown;

        ScriptedChooser(File dir, int option) {
            super(dir);
            this.option = option;
        }

        @Override
        public int showOpenDialog(Component parent) {
            shown = "open";
            return option;
        }

        @Override
        public int showSaveDialog(Component parent) {
            shown = "save";
            return option;
        }
    }

    @Test
    void showOpensAndReturnsSelection() {
        File file = tmp.resolve("a.eon").toFile();
        ScriptedChooser fc = new ScriptedChooser(tmp.toFile(), JFileChooser.APPROVE_OPTION);

        FileDialogResult r = new SwingFileDialogProvider(fc).show(null,
                new FileDialogRequest(FileDialogRequest.Mode.OPEN).selectedFile(file));

        assertEquals("open", fc.shown);
        assertEquals(file, r.getFile());
    }

    @Test
    void showSavesAndReturnsNullWhenCancelled() {
        ScriptedChooser fc = new ScriptedChooser(tmp.toFile(), JFileChooser.CANCEL_OPTION);

        FileDialogResult r = new SwingFileDialogProvider(fc).show(null,
                new FileDialogRequest(FileDialogRequest.Mode.SAVE).selectedFile(new File("x.eon")));

        assertEquals("save", fc.shown);
        assertNull(r);
    }

    @Test
    void exposesChooser() {
        JFileChooser fc = new JFileChooser();
        assertSame(fc, new SwingFileDialogProvider(fc).getChooser());
    }

    @Test
    void configuresSaveDialog() {
        JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Components", "eon");
        File dir = tmp.toFile();
        File suggested = new File(dir, "card.eon");

        new SwingFileDialogProvider(fc).configure(new FileDialogRequest(FileDialogRequest.Mode.SAVE)
                .title("Save").approveText("Save").filter(filter)
                .directory(dir).selectedFile(suggested));

        assertEquals(JFileChooser.SAVE_DIALOG, fc.getDialogType());
        assertEquals("Save", fc.getDialogTitle());
        assertEquals("Save", fc.getApproveButtonText());
        assertSame(filter, fc.getFileFilter());
        assertEquals(JFileChooser.FILES_ONLY, fc.getFileSelectionMode());
        assertFalse(fc.isMultiSelectionEnabled());
        assertEquals(dir.getAbsoluteFile(), fc.getCurrentDirectory().getAbsoluteFile());
        assertEquals(suggested, fc.getSelectedFile());
    }

    @Test
    void configuresMultiOpenFolderDialog() {
        JFileChooser fc = new JFileChooser();
        new SwingFileDialogProvider(fc).configure(new FileDialogRequest(FileDialogRequest.Mode.OPEN)
                .multiSelection(true).directoriesOnly(true));

        assertEquals(JFileChooser.OPEN_DIALOG, fc.getDialogType());
        assertEquals(JFileChooser.DIRECTORIES_ONLY, fc.getFileSelectionMode());
        assertTrue(fc.isMultiSelectionEnabled());
    }

    @Test
    void leavesChooserStateWhenRequestOmitsIt() {
        JFileChooser fc = new JFileChooser(tmp.toFile());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Scripts", "js");
        fc.setFileFilter(filter);

        new SwingFileDialogProvider(fc).configure(new FileDialogRequest(FileDialogRequest.Mode.OPEN));

        assertSame(filter, fc.getFileFilter());
        assertEquals(tmp.toFile().getAbsoluteFile(), fc.getCurrentDirectory().getAbsoluteFile());
    }

    @Test
    void resultUsesCurrentDirectoryForFiles() throws Exception {
        File file = tmp.resolve("a.eon").toFile();
        assertTrue(file.createNewFile());
        JFileChooser fc = new JFileChooser(tmp.toFile());
        fc.setSelectedFile(file);

        FileDialogResult r = new SwingFileDialogProvider(fc).result();

        assertEquals(file, r.getFile());
        assertEquals(tmp.toFile().getAbsoluteFile(), r.getDirectory().getAbsoluteFile());
    }

    @Test
    void resultUsesSelectedFolderInDirectoryMode() {
        File sub = tmp.resolve("sub").toFile();
        assertTrue(sub.mkdir());
        JFileChooser fc = new JFileChooser(tmp.toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setSelectedFile(sub);

        assertEquals(sub, new SwingFileDialogProvider(fc).result().getDirectory());
    }

    @Test
    void resultUsesCurrentDirectoryForNewFolderName() {
        File missing = tmp.resolve("missing").toFile();
        JFileChooser fc = new JFileChooser(tmp.toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setSelectedFile(missing);

        File dir = new SwingFileDialogProvider(fc).result().getDirectory();
        assertEquals(tmp.toFile().getAbsoluteFile(), dir.getAbsoluteFile());
    }

    @Test
    void resultReturnsAllSelectedFiles() {
        File a = tmp.resolve("a.eon").toFile();
        File b = tmp.resolve("b.eon").toFile();
        JFileChooser fc = new JFileChooser(tmp.toFile());
        fc.setMultiSelectionEnabled(true);
        fc.setSelectedFiles(new File[]{a, b});

        assertArrayEquals(new File[]{a, b}, new SwingFileDialogProvider(fc).result().getFiles());
    }

    @Test
    void resultIsNullWithoutSelection() {
        JFileChooser fc = new JFileChooser(tmp.toFile());
        assertNull(new SwingFileDialogProvider(fc).result());
        fc.setMultiSelectionEnabled(true);
        assertNull(new SwingFileDialogProvider(fc).result());
    }

    @Test
    void doesNotConfirmReplace() {
        assertFalse(new SwingFileDialogProvider(new JFileChooser()).confirmsReplace());
    }

    @Test
    void rejectsMissingChooser() {
        assertThrows(NullPointerException.class, () -> new SwingFileDialogProvider(null));
    }

    @Test
    void resultRejectsEmptySelection() {
        assertThrows(IllegalArgumentException.class, () -> new FileDialogResult(new File[0], null));
    }

    @Test
    void resultCopiesFiles() {
        File[] files = {new File("a")};
        FileDialogResult r = new FileDialogResult(files, null);
        files[0] = new File("b");
        assertEquals(new File("a"), r.getFile());
        r.getFiles()[0] = new File("c");
        assertEquals(new File("a"), r.getFiles()[0]);
    }
}
