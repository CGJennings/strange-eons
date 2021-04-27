package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.script.ScriptException;
import static resources.Language.string;

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
     * <code>scriptId</code>, which is a script identifier in the format
     * specified for plug-in root files, namely a resource path for script file,
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
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "utf-8"));

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
     * <code>initialize()</code> function, it will be called, and if it returns
     * <code>false</code>, then this method will return <code>false</code>.
     * Otherwise, the plug-in's name, description, type, and version will be
     * obtained and cached by calling the functions <code>getName()</code>,
     * <code>getDescription()</code>, <code>getPluginType()</code>, and
     * <code>getVersion()</code>, respectively.
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
     * <b>Scripted Plug-in Notes:</b> Calls the script's <code>unload()</code>
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
     * <b>Scripted Plug-in Notes:</b> If called with <code>show == true</code>,
     * calls the script's <code>run()</code> function, if any. If called with
     * <code>show == false</code>, calls the script's <code>hide()</code>
     * function, if any.
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
     * <code>isShowing()</code> function, if any. Otherwise, returns
     * <code>false</code>.
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
     * @return the return value returned by the function, or <code>null</code>
     */
    public Object call(String name, Object[] args) {
        return monkey.call(name, args);
    }

    /**
     * <p>
     * <b>Scripted Plug-in Notes:</b> The default implementation will look for
     * an image file with the same name (and in the same folder) as the plug-in
     * script, but with a <tt>.png</tt> or <tt>.jp2</tt> extension instead of a
     * <tt>.js</tt> extension. If no image file is found with one of these
     * names, or if the image file cannot be read, <code>null</code> is
     * returned.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public BufferedImage getRepresentativeImage() {
        // Stage 1: look to see if the script contains this method
        if (scriptEvalsOK) {
            Object retval = monkey.ambivalentCall("getRepresentativeImage");
            if (retval != null && retval instanceof BufferedImage) {
                ScriptMonkey.getSharedConsole().flush();
                return (BufferedImage) retval;
            }
        }
        // Stage 2: look for a file with the same name as the script but a.png
        //          file extension
        if (scriptFile.length() < 3 || !scriptFile.substring(scriptFile.length() - 3).equalsIgnoreCase(".js")) {
            return null;
        }
        String imageFile = scriptFile.substring(0, scriptFile.length() - 2) + "png";

        BufferedImage im = null;
        for (int ext = 0; ext < 2; ++ext) {
            URL imageURL = null;
            try {
                if (ScriptMonkey.isLibraryNameAURL(imageFile)) {
                    imageURL = new URL(imageFile);
                } else {
                    imageURL = new File(imageFile).toURI().toURL();
                }
            } catch (MalformedURLException malformedURLException) {
            }

            if (imageURL != null) {
                try {
                    im = ImageIO.read(imageURL);
                } catch (IOException ex) {
                }
            }

            if (im == null && ext == 0) {
                imageFile = scriptFile.substring(0, scriptFile.length() - 2) + "jp2";
            } else {
                break;
            }
        }

        return im;
    }

    /**
     * <p>
     * <b>Scripted Plug-in Notes:</b> The default implementation returns the
     * result of calling the script's <code>isUsable()</code> function, if any,
     * and otherwise returns <code>true</code>.
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
     * result of calling the script's <code>getDefaultAcceleratorKey()</code>
     * function, if any, and otherwise returns <code>null</code>.
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
