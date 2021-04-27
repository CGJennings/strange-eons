/*
 
 libutils.js - version 12
 Low-level script library support.
 
 */

/**
 * Utility functions for working with script libraries.
 */

const LibUtils = {
    get registeredLibraries() {
        return LibUtils.getRegisteredLibraries();
    }
};

/**
 * LibUtils.get( library ) [static]
 * Returns the source code of a library as a string.
 *
 * library : the desired library's identifier
 */
LibUtils.get = function get(library) {
    return arkham.plugins.ScriptMonkey.getLibrary(library);
};

/**
 * LibUtils.register( library ) [static]
 * Register a library. A list of registered libraries can be fetched by
 * calling <tt>LibUtils.getRegisteredLibraries()</tt>. If you write a library
 * of scripting functions, plug-ins that rely on that library can check if it
 * is available through the registry service, and registered librarys will
 * be included in the built-in library help system.
 *
 * library : the library to register
 */
LibUtils.register = function register(library) {
    arkham.plugins.LibraryRegistry.register(library);
};

/**
 * LibUtils.isRegistered( library ) [static]
 * Returns <tt>true</tt> if <tt>library</tt> is a registered library.
 * If the library has not been previously registered, but the library exists,
 * then it will be registered immediately and this function
 * will return <tt>true</tt>. Otherwise, this method returns <tt>false</tt>.
 *
 * library : the library to check for registration
 */
LibUtils.isRegistered = function isRegistered(library) {
    return arkham.plugins.LibraryRegistry.isRegistered(library);
};

/**
 * LibUtils.exists( library ) [static]
 * Returns <tt>true</tt> if <tt>library</tt> exists, that is, if it can be
 * loaded by calling <tt>useLibrary(&nbsp;library&nbsp;)</tt>.
 *
 * library : the library to register
 */
LibUtils.exists = function exists(library) {
    try {
        LibUtils.get(library);
        return true;
    } catch (ex) {
    }
    return false;
};


/**
 * LibUtils.getRegisteredLibraries() [static]
 * Returns an array of the names of all registered libraries.
 */
LibUtils.getRegisteredLibraries = function getRegisteredLibraries() {
    return Array.from(arkham.plugins.LibraryRegistry.getLibraries());
};
