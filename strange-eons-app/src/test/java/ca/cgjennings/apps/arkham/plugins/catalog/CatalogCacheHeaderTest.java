package ca.cgjennings.apps.arkham.plugins.catalog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogCacheHeaderTest {

    private static final URL PRIMARY = url("https://strangeeons.org/updates/");
    private static final URL LEGACY = url("https://strangeeons.cgjennings.ca/plugins3/");

    private Path tempCache;

    @BeforeEach
    void setUp() throws IOException {
        tempCache = Files.createTempFile("se-cat-cache-test-", ".txt");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempCache);
    }

    @Test
    void headerWritten_matchesSameURL() throws IOException {
        writeHeader(PRIMARY);
        assertTrue(Catalog.cacheBelongsTo(tempCache.toFile(), PRIMARY));
    }

    @Test
    void headerWritten_doesNotMatchDifferentURL() throws IOException {
        writeHeader(LEGACY);
        assertFalse(Catalog.cacheBelongsTo(tempCache.toFile(), PRIMARY),
                "cache stamped with legacy URL must not be considered valid for the new primary");
    }

    @Test
    void unstampedLegacyCache_isRejected() throws IOException {
        // Mimics a pre-fix cache file: starts straight into catalog content.
        Files.writeString(tempCache, "url = ./DeveloperTools.seplugin.plzm\nname = Developer Tools\n");
        assertFalse(Catalog.cacheBelongsTo(tempCache.toFile(), PRIMARY));
    }

    @Test
    void emptyCache_isRejected() throws IOException {
        assertFalse(Catalog.cacheBelongsTo(tempCache.toFile(), PRIMARY));
    }

    private void writeHeader(URL source) throws IOException {
        try (FileOutputStream out = new FileOutputStream(tempCache.toFile())) {
            Catalog.writeCacheHeader(out, source);
        }
    }

    private static URL url(String s) {
        try {
            return new URL(s);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
