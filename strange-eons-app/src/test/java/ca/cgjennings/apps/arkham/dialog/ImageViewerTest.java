package ca.cgjennings.apps.arkham.dialog;

import ca.cgjennings.imageio.SimpleImageWriter;
import ca.cgjennings.imageio.WritableImageFormat;
import java.io.File;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ImageViewerTest {

    @Test
    void extensionsFollowFormatOrder() {
        WritableImageFormat[] formats = SimpleImageWriter.getImageFormats();
        String[] exts = ImageViewer.writableExtensions();
        assertEquals(formats.length, exts.length);
        assertEquals("png", exts[0]);
        for (int i = 0; i < formats.length; ++i) {
            assertEquals(formats[i].getExtension(), exts[i]);
        }
    }

    @Test
    void formatMatchesExtensionIgnoringCase() {
        assertEquals("jpg", ImageViewer.formatFor(new File("/x/card.v2.JPG")).getExtension());
        assertEquals("png", ImageViewer.formatFor(new File("card.png")).getExtension());
    }

    @Test
    void unknownExtensionUsesFirstFormat() {
        assertSame(SimpleImageWriter.getImageFormats()[0], ImageViewer.formatFor(new File("card.tiff")));
        assertSame(SimpleImageWriter.getImageFormats()[0], ImageViewer.formatFor(new File("card")));
    }
}
