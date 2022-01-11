package ca.cgjennings.apps.arkham.plugins;

/**
 * This interface is implemented by plug-ins that operate by executing scripts.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface ScriptedPlugin {

    /**
     * Return the name of the script file the plug-in executes; this might not
     * be an actual path depending on the script's origin.
     *
     * @return the script's file identifier
     */
    public abstract String getScriptFile();

    /**
     * Returns the {@code ScriptMonkey} for the script being run by the plug-in.
     * This encapsulates the script's execution context. If the plug-in creates
     * contexts dynamically as it runs scripts, the plug-in may return
     * {@code null} if it is not currently executing a script.
     *
     * @return the monkey for this plug-in's script
     */
    public abstract ScriptMonkey getScriptMonkey();
}
