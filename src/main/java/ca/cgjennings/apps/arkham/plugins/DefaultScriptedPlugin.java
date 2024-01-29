package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.ui.theme.ThemedGlyphIcon;
import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import ca.cgjennings.ui.theme.ThemedSingleImageIcon;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.script.ScriptException;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * The standard implementation of {@link ScriptedPlugin} that executes
 * JavaScript-based plug-ins.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class DefaultScriptedPlugin implements Plugin, ScriptedPlugin {

    private String scriptFile;
    private ScriptMonkey monkey;
    private boolean scriptEvalsOK;
    private String name;
    private String description;
    private float version;
    private int type;

    /**
     * Creates a new scripted plug-in for the script identified by
     * {@code scriptId}, which is a script identifier in the format specified
     * for plug-in root files, namely a resource path for script file,
     * optionally starting with the prefix <tt>script:</tt>. (The script file
     * name presented to the script monkey and script debugger will never have
     * this prefix.)
     *
     * @param scriptId the script resource identifier
     */
    public DefaultScriptedPlugin(String scriptId) {
        if (scriptId.startsWith(PluginRoot.SCRIPT_PREFIX)) {
            scriptFile = scriptId.substring(PluginRoot.SCRIPT_PREFIX.length());
        }
    }

    private boolean installScript() {
        InputStream in = null;
        try {
            if (ScriptMonkey.isLibraryNameAURL(scriptFile)) {
                try {
                    in = new URL(scriptFile).openStream();
                } catch (NullPointerException e) {
                    throw new FileNotFoundException(scriptFile);
                }
            } else {
                in = new FileInputStream(scriptFile);
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, TextEncoding.SOURCE_CODE));

            scriptEvalsOK = true;
            Object retval = monkey.eval(r);
            if (retval != null && retval instanceof ScriptException) {
                scriptEvalsOK = false;
            }
        } catch (IOException e) {
            ErrorDialog.displayError(string("rk-err-script-file"), e);
            scriptEvalsOK = false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return scriptEvalsOK;
    }

    /**
     * <b>Scripted Plug-in Notes:</b> The script will be evaluated (so any code
     * with global scope will be run). Then, if the script defines an
     * {@code initialize()} function, it will be called, and if it returns
     * {@code false}, then this method will return {@code false}. Otherwise, the
     * plug-in's name, description, type, and version will be obtained and
     * cached by calling the functions {@code getName()},
     * {@code getDescription()}, {@code getPluginType()}, and
     * {@code getVersion()}, respectively.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean initializePlugin(PluginContext context) {
        ScriptConsole con = ScriptMonkey.getSharedConsole();

        monkey = new ScriptMonkey(scriptFile);
        monkey.bind(context);

        name = scriptFile;
        description = null;
        version = 0;
        type = Plugin.ACTIVATED;
        if (installScript()) {
            Object retval = monkey.ambivalentCall("initialize");
            if (retval != null) {
                try {
                    if (retval.equals(Boolean.FALSE)) {
                        return false;
                    }
                } catch (ClassCastException e) {
                    con.getErrorWriter().println("initialize() returned a non-boolean value: " + retval);
                }
            }

            retval = monkey.ambivalentCall("getVersion");
            if (retval != null) {
                try {
                    version = ((Number) retval).floatValue();
                } catch (ClassCastException e) {
                    con.getErrorWriter().println("getVersion() returned a non-numeric value: " + retval);
                }
            }

            retval = monkey.ambivalentCall("getName");
            if (retval != null) {
                name = retval.toString();
            }

            retval = monkey.ambivalentCall("getDescription");
            if (retval != null) {
                description = retval.toString();
            }

            retval = monkey.ambivalentCall("getPluginType");
            if (retval != null) {
                try {
                    type = ((Number) retval).intValue();
                    if (type < Plugin.ACTIVATED || type > Plugin.EXTENSION) {
                        con.getErrorWriter().println("getPluginType() returned an unknown plug-in type: " + retval);
                        return false;
                    }
                } catch (ClassCastException e) {
                    con.getErrorWriter().println("getPluginType() returned an invalid value: " + retval);
                    return false;
                }
            }
        }
        con.flush();
        return scriptEvalsOK;
    }

    /**
     * <b>Scripted Plug-in Notes:</b> Calls the script's {@code unload()}
     * function, if any.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void unloadPlugin() {
        if (scriptEvalsOK) {
            monkey.ambivalentCall("unload");
            ScriptMonkey.getSharedConsole().flush();
        }
    }

    /**
     * <b>Scripted Plug-in Notes:</b> Returns the previously cached plug-in name
     * (see {@link #initializePlugin}).
     * <p>
     * {@inheritDoc}
     */
    @Override
    public String getPluginName() {
        return name;
    }

    /**
     * <b>Scripted Plug-in Notes:</b> Returns the previously cached plug-in
     * description (see {@link #initializePlugin}).
     * <p>
     * {@inheritDoc}
     */
    @Override
    public String getPluginDescription() {
        return description;
    }

    /**
     * <b>Scripted Plug-in Notes:</b> Returns the previously cached plug-in
     * version (see {@link #initializePlugin}).
     * <p>
     * {@inheritDoc}
     */
    @Override
    public float getPluginVersion() {
        return version;
    }

    /**
     * <b>Scripted Plug-in Notes:</b> If called with {@code show == true}, calls
     * the script's {@code run()} function, if any. If called with
     * {@code show == false}, calls the script's {@code hide()} function, if
     * any.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void showPlugin(PluginContext context, boolean show) {
        if (scriptEvalsOK) {
            monkey.bind(context);
            if (show) {
                monkey.ambivalentCall(show ? "run" : "hide");
//                ((PluginContextImpl) context).synchronize();
                ScriptMonkey.getSharedConsole().flush();
            }
        }
    }

    /**
     * <b>Scripted Plug-in Notes:</b> Returns the value of calling the script's
     * {@code isShowing()} function, if any. Otherwise, returns {@code false}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean isPluginShowing() {
        if (scriptEvalsOK) {
            Object retval = monkey.ambivalentCall("isShowing");
            if (retval != null && retval instanceof Boolean) {
                ScriptMonkey.getSharedConsole().flush();
                return ((Boolean) retval);
            }
        }
        return false;
    }

    /**
     * <b>Scripted Plug-in Notes:</b> Returns the previously cached plug-in type
     * (see {@link #initializePlugin}).
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int getPluginType() {
        return type;
    }

    /**
     * Returns the script resource identifier for the script that is providing
     * plug-in functionality.
     *
     * @return the script file for this plug-in's script
     */
    @Override
    public String getScriptFile() {
        return scriptFile;
    }

    /**
     * Returns the {@link ScriptMonkey} used to manage the execution of the
     * plug-in's script code.
     *
     * @return the monkey bound to the plug-in script
     */
    @Override
    public ScriptMonkey getScriptMonkey() {
        return monkey;
    }

    /**
     * This method is provided as a convenience for other scripts that wish to
     * call a function defined in this plug-in.
     *
     * @param name the name of the function to call
     * @param args an array of arguments to pass to the function
     * @return the return value returned by the function, or {@code null}
     */
    public Object call(String name, Object[] args) {
        return monkey.call(name, args);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * <b>Scripted Plug-in Notes:</b> The default implementation will query
     * the script, and if it gets no result, will look for an image file with
     * the same name (and in the same folder) as the plug-in
     * script. If no image file is found with one of these
     * names, or if the image file cannot be read, a default icon is returned.
     */    
    @Override
    public ThemedIcon getPluginIcon() {
        if (pluginIcon == null) {
            // Look to see if the script contains this method
            if (scriptEvalsOK) {
                Object retval = monkey.ambivalentCall("getPluginIcon");
                if (retval != null && retval instanceof ThemedIcon) {
                    ScriptMonkey.getSharedConsole().flush();
                    return (ThemedIcon) retval;
                }
                retval = monkey.ambivalentCall("getRepresentativeImage");
                if (retval != null && retval instanceof BufferedImage) {
                    ScriptMonkey.getSharedConsole().flush();
                    return new ThemedSingleImageIcon((BufferedImage) retval);
                }
            }
            
            if (ResourceKit.composeResourceURL(scriptFile) != null) {
                int dot = scriptFile.lastIndexOf('.');
                if (dot < scriptFile.lastIndexOf('/')) dot = -1;
                String baseName = dot >= 0 ? scriptFile.substring(0, dot) : scriptFile;
                String ext = ".png";
                for (int i=0; i<2; ++i) {
                    if (ResourceKit.composeResourceURL(baseName + ext) != null) {
                        pluginIcon = new ThemedImageIcon(baseName + ext);
                        break;
                    }
                    ext = ".jp2";
                }
                
            }
            if (pluginIcon == null) {
                if (getPluginType() == EXTENSION) {
                    pluginIcon = ResourceKit.getIcon("extension");
                } else {
                    pluginIcon = ResourceKit.getIcon("plugin");
                }
                if (pluginIcon instanceof ThemedGlyphIcon) {
                    pluginIcon = ((ThemedGlyphIcon) pluginIcon).derive(getPluginName());
                }
            }
            pluginIcon = pluginIcon.small();
        }
        return pluginIcon;
    }
    private ThemedIcon pluginIcon;

    /**
     * <p>
     * <b>Scripted Plug-in Notes:</b> The default implementation returns the
     * result of calling the script's {@code isUsable()} function, if any, and
     * otherwise returns {@code true}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public boolean isPluginUsable() {
        if (scriptEvalsOK) {
            Object retval = monkey.ambivalentCall("isUsable");
            if (retval != null && retval instanceof Boolean) {
                return ((Boolean) retval);
            }
        }
        return true;
    }

    /**
     * <p>
     * <b>Scripted Plug-in Notes:</b> The default implementation returns the
     * result of calling the script's {@code getDefaultAcceleratorKey()}
     * function, if any, and otherwise returns {@code null}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public String getDefaultAcceleratorKey() {
        if (scriptEvalsOK) {
            Object retval = monkey.ambivalentCall("getDefaultAcceleratorKey");
            if (retval != null) {
                return retval.toString();
            }
        }
        return null;
    }
}
