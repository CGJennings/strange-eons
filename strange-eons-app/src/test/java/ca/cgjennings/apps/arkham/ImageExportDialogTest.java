package ca.cgjennings.apps.arkham;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageExportDialogTest {

    @Test
    void printResolutionIsAtLeastStandard() {
        assertEquals(300, ImageExportDialog.printResolution(0d));
        assertEquals(300, ImageExportDialog.printResolution(150d));
        assertEquals(300, ImageExportDialog.printResolution(300d));
    }

    @Test
    void printResolutionUsesHigherTemplateResolution() {
        assertEquals(600, ImageExportDialog.printResolution(599.6d));
    }

    @Test
    void suggestsComponentValueWhenUserHasNotSetKey() {
        assertTrue(ImageExportDialog.suggests(Set.of(), "imexport-dpi", 600d));
    }

    @Test
    void userSettingTakesPrecedence() {
        assertFalse(ImageExportDialog.suggests(Set.of("imexport-dpi"), "imexport-dpi", 600d));
    }

    @Test
    void noSuggestionWithoutComponentValue() {
        assertFalse(ImageExportDialog.suggests(Set.of(), "imexport-bleed-margin", 0d));
    }
}
