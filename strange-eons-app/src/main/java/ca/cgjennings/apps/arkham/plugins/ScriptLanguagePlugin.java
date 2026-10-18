package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.ui.theme.ThemedIcon;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import org.strangeeons.scripts.ScriptException;
import org.strangeeons.scripts.ScriptLanguage;
import org.strangeeons.scripts.ScriptSession;
import static resources.Language.string;

/**
 * Hosts a scripted plug-in whose source is interpreted by a {@link ScriptLanguage}
 * registered through the {@code org.strangeeons.scripts} SPI. Counterpart to
 * {@link DefaultScriptedPlugin}, which keeps hosting the legacy JavaScript path
 * directly through {@link ScriptMonkey}.
 *
 * <p>Lifecycle metadata ({@code getName}, {@code getDescription},
 * {@code getPluginType}, {@code getVersion}) and optional hooks
 * ({@code initialize}, {@code unload}, {@code run}, {@code hide},
 * {@code isShowing}, {@code isUsable}, {@code getDefaultAcceleratorKey})
 * are resolved by name against the session, mirroring the contract scripted
 * JS plug-ins already follow.
 */
public class ScriptLanguagePlugin implements Plugin {

    private final ScriptLanguage language;
    private final String scriptFile;
    private ScriptSession session;
    private boolean evalsOK;
    private String name;
    private String description;
    private float version;
    private int type = Plugin.ACTIVATED;

    public ScriptLanguagePlugin(ScriptLanguage language, String scriptId) {
        this.language = language;
        if (scriptId.startsWith(PluginRoot.SCRIPT_PREFIX)) {
            this.scriptFile = scriptId.substring(PluginRoot.SCRIPT_PREFIX.length());
        } else {
            this.scriptFile = scriptId;
        }
        this.name = scriptFile;
    }

    private boolean evalSource() {
        try (InputStream in = openSource();
                BufferedReader r = new BufferedReader(new InputStreamReader(in, TextEncoding.SOURCE_CODE))) {
            StringBuilder buf = new StringBuilder(4096);
            char[] block = new char[4096];
            for (int n; (n = r.read(block)) >= 0;) {
                buf.append(block, 0, n);
            }
            session.eval(buf.toString(), scriptFile);
            return true;
        } catch (IOException ioe) {
            ErrorDialog.displayError(string("rk-err-script-file"), ioe);
            return false;
        } catch (ScriptException e) {
            StrangeEons.log.log(Level.WARNING, "script-language plug-in failed to evaluate: " + scriptFile, e);
            return false;
        }
    }

    private InputStream openSource() throws IOException {
        if (isURL(scriptFile)) {
            return new URL(scriptFile).openStream();
        }
        return new FileInputStream(scriptFile);
    }

    /**
     * Returns whether a script name is a URL rather than a file path: it has
     * a scheme before a colon. A single letter before the colon is a Windows
     * drive.
     */
    static boolean isURL(String scriptName) {
        return scriptName.indexOf(':') > 1;
    }

    private Object optionalCall(String fn, Object... args) {
        try {
            return session.callIfDefined(fn, args);
        } catch (ScriptException e) {
            StrangeEons.log.log(Level.WARNING, fn + "() in plug-in script raised an error: " + scriptFile, e);
            return null;
        }
    }

    /**
     * Returns the {@link ScriptLanguage} that hosts this plug-in.
     */
    public ScriptLanguage getLanguage() {
        return language;
    }

    /**
     * Returns the script identifier for this plug-in's source.
     */
    public String getScriptFile() {
        return scriptFile;
    }

    @Override
    public boolean initializePlugin(PluginContext context) {
        session = language.createSession();
        session.bind("PluginContext", context);

        evalsOK = evalSource();
        if (!evalsOK) {
            return false;
        }

        Object retval = optionalCall("initialize");
        if (retval instanceof Boolean && !((Boolean) retval)) {
            return false;
        }

        retval = optionalCall("getVersion");
        if (retval instanceof Number) {
            version = ((Number) retval).floatValue();
        }

        retval = optionalCall("getName");
        if (retval != null) {
            name = retval.toString();
        }

        retval = optionalCall("getDescription");
        if (retval != null) {
            description = retval.toString();
        }

        retval = optionalCall("getPluginType");
        if (retval instanceof Number) {
            int t = ((Number) retval).intValue();
            if (t < Plugin.ACTIVATED || t > Plugin.EXTENSION) {
                StrangeEons.log.warning("getPluginType() returned an unknown value: " + retval);
                return false;
            }
            type = t;
        }
        return true;
    }

    @Override
    public synchronized void unloadPlugin() {
        if (session != null) {
            if (evalsOK) {
                optionalCall("unload");
            }
            try {
                session.close();
            } catch (RuntimeException e) {
                StrangeEons.log.log(Level.WARNING, "closing script session: " + scriptFile, e);
            }
            session = null;
        }
        evalsOK = false;
    }

    @Override
    public String getPluginName() {
        return name;
    }

    @Override
    public String getPluginDescription() {
        return description;
    }

    @Override
    public float getPluginVersion() {
        return version;
    }

    @Override
    public int getPluginType() {
        return type;
    }

    @Override
    public void showPlugin(PluginContext context, boolean show) {
        if (!evalsOK || session == null) {
            return;
        }
        session.bind("PluginContext", context);
        optionalCall(show ? "run" : "hide");
    }

    @Override
    public boolean isPluginShowing() {
        Object retval = optionalCall("isShowing");
        return retval instanceof Boolean && (Boolean) retval;
    }

    @Override
    public boolean isPluginUsable() {
        Object retval = optionalCall("isUsable");
        if (retval instanceof Boolean) {
            return (Boolean) retval;
        }
        return true;
    }

    @Override
    public ThemedIcon getPluginIcon() {
        return null;
    }

    @Override
    public String getDefaultAcceleratorKey() {
        Object retval = optionalCall("getDefaultAcceleratorKey");
        return retval == null ? null : retval.toString();
    }
}
