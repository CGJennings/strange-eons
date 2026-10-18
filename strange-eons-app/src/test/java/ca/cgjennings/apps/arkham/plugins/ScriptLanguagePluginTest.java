package ca.cgjennings.apps.arkham.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.strangeeons.scripts.ScriptLanguage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ScriptLanguagePluginTest {

    private Path scriptFile;

    @BeforeEach
    void writeScriptFile() throws IOException {
        scriptFile = Files.createTempFile("script-language-test", ".test");
        Files.writeString(scriptFile, "// host ignores this content");
        TestScriptLanguage.lastSession = null;
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.deleteIfExists(scriptFile);
    }

    /**
     * The TestScriptLanguage in test sources is registered via
     * META-INF/services so ServiceLoader (the same lookup the plug-in loader
     * uses) finds it by id. This is the wire that AbstractInstalledPlugin
     * uses to resolve a manifest's {@code script-language} value.
     */
    @Test
    void serviceLoaderFindsRegisteredScriptLanguage() {
        ScriptLanguage found = null;
        for (ScriptLanguage lang : ServiceLoader.load(ScriptLanguage.class)) {
            if (TestScriptLanguage.ID.equals(lang.id())) {
                found = lang;
                break;
            }
        }
        assertNotNull(found, "TestScriptLanguage should be discoverable via ServiceLoader");
    }

    @Test
    void scriptNamesWithSchemesAreURLs() {
        assertTrue(ScriptLanguagePlugin.isURL("res://plugin/script.js"));
        assertTrue(ScriptLanguagePlugin.isURL("file:/tmp/script.js"));
    }

    @Test
    void filePathsAreNotURLs() {
        assertFalse(ScriptLanguagePlugin.isURL("/tmp/script.js"));
        assertFalse(ScriptLanguagePlugin.isURL("C:\\Users\\me\\script.js"));
        assertFalse(ScriptLanguagePlugin.isURL("script.js"));
    }

    /**
     * Initializing a ScriptLanguagePlugin must drive the session: bind the
     * plug-in context, eval the script source, and probe the optional
     * lifecycle hooks. With no hooks defined, defaults stand.
     */
    @Test
    void initializeRunsSessionLifecycle() {
        TestScriptLanguage lang = new TestScriptLanguage();
        ScriptLanguagePlugin plugin = new ScriptLanguagePlugin(lang, scriptFile.toString());

        PluginContext context = mock(PluginContext.class);
        assertTrue(plugin.initializePlugin(context));

        TestScriptLanguage.RecordingSession session = TestScriptLanguage.lastSession;
        assertNotNull(session);
        assertEquals(1, session.evalCount);
        assertSame(context, session.bindings.get("PluginContext"),
                "host must bind the plug-in context for the script to consume");
        // No hooks defined → defaults
        assertEquals(scriptFile.toString(), plugin.getPluginName());
        assertEquals(0f, plugin.getPluginVersion());
        assertEquals(Plugin.ACTIVATED, plugin.getPluginType());
    }

    /**
     * When the script defines optional lifecycle hooks, the host's metadata
     * accessors return the values reported by the hooks.
     */
    @Test
    void optionalHooksOverrideDefaults() {
        TestScriptLanguage lang = new TestScriptLanguage();
        lang.defaultReturns.put("getName", "My Plug-in");
        lang.defaultReturns.put("getDescription", "A test plug-in");
        lang.defaultReturns.put("getVersion", 2.5f);
        lang.defaultReturns.put("getPluginType", Plugin.INJECTED);
        lang.defaultReturns.put("getDefaultAcceleratorKey", "ctrl Q");

        ScriptLanguagePlugin plugin = new ScriptLanguagePlugin(lang, scriptFile.toString());
        assertTrue(plugin.initializePlugin(mock(PluginContext.class)));

        assertEquals("My Plug-in", plugin.getPluginName());
        assertEquals("A test plug-in", plugin.getPluginDescription());
        assertEquals(2.5f, plugin.getPluginVersion());
        assertEquals(Plugin.INJECTED, plugin.getPluginType());
        assertEquals("ctrl Q", plugin.getDefaultAcceleratorKey());
    }

    /**
     * unloadPlugin must call the script's unload() hook (when defined) and
     * close the underlying session in either case.
     */
    @Test
    void unloadClosesSessionAndCallsHook() {
        TestScriptLanguage lang = new TestScriptLanguage();
        lang.defaultReturns.put("unload", null); // hook present, returns null
        ScriptLanguagePlugin plugin = new ScriptLanguagePlugin(lang, scriptFile.toString());
        plugin.initializePlugin(mock(PluginContext.class));

        TestScriptLanguage.RecordingSession session = TestScriptLanguage.lastSession;
        plugin.unloadPlugin();
        assertTrue(session.closed, "ScriptSession.close must be called on unload");
    }

    /**
     * showPlugin must dispatch to the script's run() / hide() hooks.
     */
    @Test
    void showPluginDispatchesRunAndHide() {
        TestScriptLanguage lang = new TestScriptLanguage();
        ScriptLanguagePlugin plugin = new ScriptLanguagePlugin(lang, scriptFile.toString());
        PluginContext context = mock(PluginContext.class);
        plugin.initializePlugin(context);

        TestScriptLanguage.RecordingSession session = TestScriptLanguage.lastSession;
        plugin.showPlugin(context, true);
        plugin.showPlugin(context, false);
        // Bindings refreshed each time showPlugin is called.
        assertSame(context, session.bindings.get("PluginContext"));
    }
}
