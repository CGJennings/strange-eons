package ca.cgjennings.apps.arkham.plugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for {@link BundleInstaller#resolveBundleRequirements}, focused on
 * the parsing + graceful-degradation paths that don't require a populated
 * installed-bundle registry.
 */
class BundleRequirementsTest {

    private Path bundleFile;

    @AfterEach
    void cleanup() throws IOException {
        if (bundleFile != null) {
            Files.deleteIfExists(bundleFile);
        }
    }

    @Test
    void noManifestEntryReturnsEmpty() throws IOException {
        bundleFile = writeBundle(null);
        assertTrue(BundleInstaller.resolveBundleRequirements(bundleFile.toFile()).isEmpty());
    }

    @Test
    void emptyRequiresReturnsEmpty() throws IOException {
        bundleFile = writeBundle("catalog-requires =\n");
        assertTrue(BundleInstaller.resolveBundleRequirements(bundleFile.toFile()).isEmpty());
    }

    @Test
    void uninstalledRequirementIsSkipped() throws IOException {
        // Real-shaped CatalogID for a UUID that isn't loaded by the test.
        bundleFile = writeBundle(
                "catalog-requires = CATALOGUEID{12345678-1234-1234-1234-1234567890ab:2026-1-1-0-0-0-0}\n");
        // No installed bundle uses this UUID, so the lookup returns null and
        // the entry is logged + skipped.
        List<java.io.File> resolved = BundleInstaller.resolveBundleRequirements(bundleFile.toFile());
        assertTrue(resolved.isEmpty());
    }

    @Test
    void malformedTokenDoesNotAbort() throws IOException {
        // A non-UUID, non-CatalogID token should be ignored; resolution
        // continues for the remaining (uninstalled) entry without throwing.
        bundleFile = writeBundle(
                "catalog-requires = not-a-uuid, CATALOGUEID{12345678-1234-1234-1234-1234567890ab:2026-1-1-0-0-0-0}\n");
        assertEquals(0, BundleInstaller.resolveBundleRequirements(bundleFile.toFile()).size());
    }

    @Test
    void multipleEntriesAreParsed() throws IOException {
        // None of the UUIDs are installed in the test, so the result is
        // empty, but the test exercises the comma-separated parse path
        // without throwing on any token.
        bundleFile = writeBundle(
                "catalog-requires = "
                        + "CATALOGUEID{12345678-1234-1234-1234-1234567890ab:2026-1-1-0-0-0-0}, "
                        + "CATALOGUEID{87654321-4321-4321-4321-1234567890ab:2026-1-1-0-0-0-0}\n");
        assertTrue(BundleInstaller.resolveBundleRequirements(bundleFile.toFile()).isEmpty());
    }

    /**
     * Writes a tiny JAR containing only an eons-plugin manifest entry. If
     * {@code requiresLine} is {@code null}, no manifest entry is written
     * (empty bundle).
     */
    private static Path writeBundle(String requiresLine) throws IOException {
        Path file = Files.createTempFile("test-bundle", ".seext");
        ByteArrayOutputStream manifest = new ByteArrayOutputStream();
        manifest.write("#CATALOGUEID{99999999-9999-9999-9999-999999999999:2026-1-1-0-0-0-0}\n".getBytes());
        manifest.write("name = test\n".getBytes());
        if (requiresLine != null) {
            manifest.write(requiresLine.getBytes());
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            jar.putNextEntry(new JarEntry("eons-plugin"));
            jar.write(manifest.toByteArray());
            jar.closeEntry();
        }
        return file;
    }
}
