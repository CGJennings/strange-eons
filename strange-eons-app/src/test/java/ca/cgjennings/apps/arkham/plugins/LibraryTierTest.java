package ca.cgjennings.apps.arkham.plugins;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 3 — verifies that the bundle scanner dispatches by an effective
 * classloader policy: libraries and themes default to the shared
 * {@link BundleInstaller#getLibraryClassLoader() library classloader},
 * plug-ins and extensions default to a per-bundle isolated
 * {@link URLClassLoader} parented to the library classloader. The
 * {@code classloader = isolated|shared} manifest key overrides the
 * type-driven default.
 */
class LibraryTierTest {

    private final List<Path> tempFiles = new java.util.ArrayList<>();

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
        for (Path p : tempFiles) {
            Files.deleteIfExists(p);
        }
    }

    @Test
    void libraryDefaultRegistersIntoLibraryClassLoader() throws Exception {
        Path bundle = writeBundle("library-default", ".selibrary", null);
        BundleInstaller.scanBundle(null, bundle.toFile(), PluginBundle.TYPE_LIBRARY);

        assertFalse(BundleInstaller.isIsolatedBundle(bundle.toFile()),
                "library default policy should be SHARED, not ISOLATED");

        URLClassLoader libCL = (URLClassLoader) BundleInstaller.getLibraryClassLoader();
        assertTrue(urlsContain(libCL, bundle),
                "library bundle URL should be registered in the library classloader");
    }

    @Test
    void libraryWithIsolatedOverrideGetsItsOwnClassLoader() throws Exception {
        Path bundle = writeBundle("library-isolated", ".selibrary", "isolated");
        BundleInstaller.scanBundle(null, bundle.toFile(), PluginBundle.TYPE_LIBRARY);

        assertTrue(BundleInstaller.isIsolatedBundle(bundle.toFile()),
                "classloader = isolated should force ISOLATED policy regardless of type");

        ClassLoader cl = BundleInstaller.classLoaderFor(bundle.toFile());
        assertSame(BundleInstaller.getLibraryClassLoader(), cl.getParent(),
                "isolated CLs should parent to the shared library classloader");
    }

    @Test
    void pluginDefaultIsIsolated() throws Exception {
        Path bundle = writeBundle("plugin-default", ".seplugin", null);
        BundleInstaller.scanBundle(new HashSet<>(), bundle.toFile(), PluginBundle.TYPE_PLUGIN);

        assertTrue(BundleInstaller.isIsolatedBundle(bundle.toFile()),
                "plugin default policy should be ISOLATED");

        URLClassLoader libCL = (URLClassLoader) BundleInstaller.getLibraryClassLoader();
        assertFalse(urlsContain(libCL, bundle),
                "isolated plugin bundles should NOT be registered in the library classloader");
    }

    @Test
    void pluginWithSharedOverrideRoutesToLibraryClassLoader() throws Exception {
        Path bundle = writeBundle("plugin-shared", ".seplugin", "shared");
        BundleInstaller.scanBundle(new HashSet<>(), bundle.toFile(), PluginBundle.TYPE_PLUGIN);

        assertFalse(BundleInstaller.isIsolatedBundle(bundle.toFile()),
                "classloader = shared should force SHARED policy on a plugin bundle");

        URLClassLoader libCL = (URLClassLoader) BundleInstaller.getLibraryClassLoader();
        assertTrue(urlsContain(libCL, bundle),
                "plugin with classloader = shared should land in the library classloader");
    }

    @Test
    void extensionDefaultIsIsolated() throws Exception {
        Path bundle = writeBundle("ext-default", ".seext", null);
        BundleInstaller.scanBundle(new HashSet<>(), bundle.toFile(), PluginBundle.TYPE_EXTENSION);

        assertTrue(BundleInstaller.isIsolatedBundle(bundle.toFile()),
                "extension default policy should be ISOLATED");
    }

    @Test
    void themeDefaultRoutesToLibraryClassLoader() throws Exception {
        Path bundle = writeBundle("theme-default", ".setheme", null);
        BundleInstaller.scanBundle(new HashSet<>(), bundle.toFile(), PluginBundle.TYPE_THEME);

        assertFalse(BundleInstaller.isIsolatedBundle(bundle.toFile()),
                "theme default policy should be SHARED");

        URLClassLoader libCL = (URLClassLoader) BundleInstaller.getLibraryClassLoader();
        assertTrue(urlsContain(libCL, bundle),
                "theme bundle URL should be registered in the library classloader");
    }

    private static boolean urlsContain(URLClassLoader cl, Path bundle) throws Exception {
        URL target = bundle.toUri().toURL();
        for (URL u : cl.getURLs()) {
            if (u.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private Path writeBundle(String namePrefix, String extension, String classloaderValue) throws IOException {
        Path file = Files.createTempFile(namePrefix, extension);
        tempFiles.add(file);
        StringBuilder manifest = new StringBuilder();
        manifest.append("#CATALOGUEID{")
                .append(UUID.randomUUID())
                .append(":2026-1-1-0-0-0-0}\n");
        manifest.append("name = ").append(namePrefix).append('\n');
        if (classloaderValue != null) {
            manifest.append("classloader = ").append(classloaderValue).append('\n');
        }
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            jar.putNextEntry(new JarEntry("eons-plugin"));
            jar.write(manifest.toString().getBytes());
            jar.closeEntry();
        }
        return file;
    }
}
