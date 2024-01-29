/**
 * Plug-in Script
 *
 * NOTE: You can delete any function marked Optional that you are not using.
 */

/**
 * Returns a short name for the plug-in; this appears as the
 * plug-in's name in the plug-in manager. If the plug-i is ACTIVATED,
 * then this will be the name of its menu item in the Toolbox menu.
 *
 * Optional function.
 */
function getName() {
    return 'Plug-in Script';
}


/**
 * Returns a brief description of the plug-in; this is used
 * in the plug-in manager. If the plug-i is ACTIVATED, then this is also
 * the tooltip for the plug-in's menu item in the Toolbox menu.
 *
 * Optional function.
 */
function getDescription() {
    return 'A template for writing plug-in scripts.';
}


/**
 * Returns a version number for the plug-in.
 *
 * Optional function.
 */
function getVersion() {
    return 1.0;
}


/**
 * Returns the type of plug-in, ACTIVATED, INJECTED, or EXTENSION.
 * ACTIVATED plug-ins are given a menu item in the Toolbox menu.
 * These plug-ins are activated whenever the user selects this menu item.
 * They can be loaded and unloaded at any time after the application starts.
 * INJECTED plug-ins, like activated plug-ins, can be loaded and unloaded
 * at any time but they are not automatically associated with a menu item.
 * Instead of being activated any number of times, they are activated once
 * when the plug-in is loaded. EXTENSION plug-ins are loaded during startup
 * and are active the entire time the application is running. Extension
 * plug-ins can do certain things (such as registering new kinds of game
 * components) that other plug-in types cannot.
 *
 * This function rarely needs to be included in the script. If left out,
 * the default is ACTIVATED. For EXTENSION plug-ins, the extension library
 * will define this function for you. Thus, you only need to explicitly
 * define this function when the plug-in type is INJECTED.
 *
 * Optional function unless the plug-in type is INJECTED (see details above).
 */
function getPluginType() {
    return arkham.plugins.Plugin.ACTIVATED;
}


/**
 * Called when the plug-in is first loaded to allow it to do one-time setup.
 * This function is called before any other function, including getName() and
 * getDescription(). This allows you to load string resources if you want to
 * return localized strings from those functions.
 *
 * For an EXTENSION plug-in, since run() will never be called, this is also
 * the place where your plug-in should do whatever work it needs to do
 * (registering games and expansions, adding new game component types to
 * the class map, and so on).
 *
 * If during initialization the plug-in determines that it cannot work under
 * the current circumstances (for example, a required library or game is not
 * installed), then this function can return false. In this case, the plug-in
 * startup process will end and the plug-in will not be loaded.
 *
 * Optional function.
 */
function initialize() {
}


/**
 * Called when the plug-in is being unloaded to allow it to clean up and
 * release any resources it is holding. If not defined in the script,
 * no special cleanup is performed.
 *
 * ACTIVATED and INJECTED plug-ins may be unloaded at any time. EXTENSION
 * plug-ins are only unloaded during application shutdown.
 *
 * Optional function. Advanced feature.
 */
function unload() {
}


/**
 * This function is used in ACTIVATED or INJECTED plug-ins. It is
 * called when the user chooses the plug-in's menu item in the Toolbox
 * menu (unless isShowing() returns true). The following variables are
 * available:
 *
 * Eons : the application (an instance of the StrangeEons class)
 * Editor : the active editor tab (or null if no editor is active)
 * PluginContext : an instance of the PluginContext class
 *
 * This function is called once during startup for INJECTED plug-ins.
 * It is never called for EXTENSION plug-ins.
 *
 * Required function unless the plug-in is an EXTENSION, in which case
 * it is optional.
 */
function run() {
    println('Plug-in ' + getName() + ' activated');
}


/**
 * This function is used in ACTIVATED plug-ins. It is called instead of run()
 * when the user selects the plug-in's menu item and isShowing() returns true.
 * This is used in conjunction with isShowing() by plug-ins that have an
 * on/off or shown/hidden aspect to their behaviour.
 *
 * This function is never called for INJECTED or EXTENSION plug-ins.
 *
 * Optional function. Advanced feature.
 */
function hide() {
}


/**
 * Returns true if the plug-in is currently shown and/or working.
 * When this returns true, the plug-in's Toolbox menu item will have
 * a checkmark next to it and if selected the hide() function will
 * be called instead of run(). If this function is not defined in the
 * plug-in script, then the plug-in will behave as if it always returns false.
 * (That is, the plug-in's menu item will never have a checkmark and
 * only run() will be called to activate it.)
 *
 * This function is never called for INJECTED or EXTENSION plug-ins.
 *
 * Optional function. Advanced feature.
 */
function isShowing() {
    return false;
}

/**
 * Returns true if the plug-in can currently be activated. When this
 * returns false, the plug-in's Toolbox menu item will be disabled
 * so that the user cannot activate the plug-in. If this function is not
 * defined in the plug-in script, then the plug-in will behave as if it
 * always returns true. In this case, the plug-in's menu item will never
 * be disabled.
 *
 * This function is never called for INJECTED or EXTENSION plug-ins.
 *
 * Optional function. Advanced feature.
 */
function isUsable() {
    return true;
}