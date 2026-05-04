package ca.cgjennings.apps.arkham.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.strangeeons.scripts.ScriptLanguage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a bundle declaring {@code classloader = isolated} in its
 * eons-plugin manifest is held in its own classloader (parented to the
 * shared library classloader) and that
 * {@link AbstractInstalledPlugin#lookupScriptLanguage(String)} discovers
 * services registered through that classloader's
 * {@code META-INF/services}.
 */
class IsolatedBundleTest {

    private Path bundleFile;

    /**
     * BundleInstaller's static state isn't reset across surefire
     * test runs (init() runs during real app startup, not here, and the
     * isolated-bundle registry accumulates across @Test methods). Seed
     * the deprecated-bundles array and clear the isolated registries via
     * reflection so each test gets a clean slate.
     */
    @BeforeEach
    void seedBundleInstallerState() throws Exception {
        Field deprecated = BundleInstaller.class.getDeclaredField("deprecatedBundles");
        deprecated.setAccessible(true);
        if (deprecated.get(null) == null) {
            deprecated.set(null, new String[0]);
        }

        clearStaticCollection("isolatedBundleFiles");
        clearStaticCollection("isolatedClassLoaders");
        clearStaticCollection("isolatedBundleLanguages");
        clearStaticCollection("discoveredBundles");
        clearStaticCollection("catalogIDMap");
        clearStaticCollection("uuidToFileMap");
    }

    private static void clearStaticCollection(String fieldName) throws Exception {
        Field f = BundleInstaller.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        Object value = f.get(null);
        if (value instanceof java.util.Collection<?> c) {
            c.clear();
        } else if (value instanceof java.util.Map<?, ?> m) {
            m.clear();
        }
    }

    @AfterEach
    void cleanup() throws IOException {
        if (bundleFile != null) {
            Files.deleteIfExists(bundleFile);
        }
    }

    @Test
    void isolatedBundleIsRecordedAndGetsItsOwnClassLoader() throws Exception {
        bundleFile = writeIsolatedBundle();
        scanAsLibrary(bundleFile);

        assertTrue(BundleInstaller.isIsolatedBundle(bundleFile.toFile()),
                "isIsolatedBundle should return true for a bundle that opted in");

        ClassLoader cl = BundleInstaller.classLoaderFor(bundleFile.toFile());
        assertNotNull(cl);
        assertNotSame(ClassLoader.getSystemClassLoader(), cl);
        assertSame(BundleInstaller.getLibraryClassLoader(), cl.getParent(),
                "isolated CLs should parent to the shared library classloader");
        // Same instance on subsequent lookups.
        assertEquals(cl, BundleInstaller.classLoaderFor(bundleFile.toFile()));
        assertTrue(BundleInstaller.getIsolatedClassLoaders().contains(cl));
    }

    @Test
    void classLoaderForUnisolatedBundleThrows() {
        java.io.File phantom = new java.io.File("/tmp/never-registered.selibrary");
        assertFalse(BundleInstaller.isIsolatedBundle(phantom));
        assertThrows(IllegalArgumentException.class,
                () -> BundleInstaller.classLoaderFor(phantom));
    }

    @Test
    void lookupScriptLanguageFindsServiceInIsolatedClassLoader() throws Exception {
        bundleFile = writeIsolatedBundle();
        scanAsLibrary(bundleFile);

        ClassLoader cl = BundleInstaller.classLoaderFor(bundleFile.toFile());
        // Sanity: the JAR-internal services file is reachable from the
        // isolated classloader.
        java.net.URL svcUrl = cl.getResource(
                "META-INF/services/org.strangeeons.scripts.ScriptLanguage");
        assertNotNull(svcUrl, "service registration must be visible to URLClassLoader");

        ScriptLanguage found = AbstractInstalledPlugin.lookupScriptLanguage(
                IsolatedScriptLanguage.ID);
        assertNotNull(found,
                "lookupScriptLanguage must walk isolated classloaders to find "
                        + "services not registered on the system classloader");
        assertEquals("Isolated Test Language", found.displayName());
    }

    /**
     * Builds a tiny .selibrary jar containing:
     *   - eons-plugin (with classloader = isolated)
     *   - META-INF/services/org.strangeeons.scripts.ScriptLanguage
     *   - the compiled bytes of IsolatedScriptLanguage.class so the JAR
     *     can stand alone on its URLClassLoader.
     */
    private static Path writeIsolatedBundle() throws IOException {
        Path file = Files.createTempFile("isolated-bundle", ".selibrary");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            jar.putNextEntry(new JarEntry("eons-plugin"));
            jar.write(("#CATALOGUEID{11111111-1111-1111-1111-111111111111:2026-1-1-0-0-0-0}\n"
                    + "name = isolated-test\n"
                    + "classloader = isolated\n").getBytes());
            jar.closeEntry();

            jar.putNextEntry(new JarEntry(
                    "META-INF/services/org.strangeeons.scripts.ScriptLanguage"));
            jar.write((IsolatedScriptLanguage.class.getName() + "\n").getBytes());
            jar.closeEntry();

            String classResource = IsolatedScriptLanguage.class.getName().replace('.', '/') + ".class";
            jar.putNextEntry(new JarEntry(classResource));
            try (InputStream in = IsolatedBundleTest.class.getClassLoader()
                    .getResourceAsStream(classResource)) {
                assertNotNull(in, "test class file must be on test classpath");
                in.transferTo(jar);
            }
            jar.closeEntry();
        }
        return file;
    }

    /**
     * Drives a single bundle through {@link BundleInstaller#scanBundle},
     * which triggers the manifest-key branch we're testing without firing
     * the full {@code loadLibraryBundles} path (which would scan the
     * user's real plug-ins folder and pull in unrelated initialization).
     */
    private static void scanAsLibrary(Path bundle) {
        BundleInstaller.scanBundle(null, bundle.toFile(), PluginBundle.TYPE_LIBRARY);
    }
}
