/*
 
 libutils.js - version 12
 Low-level script library support.
 (Deprecated; included for compatibility.)
 
 */

const LibUtils = {
    get registeredLibraries() {
        return LibUtils.getRegisteredLibraries();
    },
    get: function get(library) {
        return arkham.plugins.ScriptMonkey.getLibrary(library);
    },
    register: function register(library) {
        arkham.plugins.LibraryRegistry.register(library);
    },
    isRegistered: function isRegistered(library) {
        return arkham.plugins.LibraryRegistry.isRegistered(library);
    },
    exists: function exists(library) {
        try {
            LibUtils.get(library);
            return true;
        } catch (ex) {
        }
        return false;
    },
    getRegisteredLibraries: function getRegisteredLibraries() {
        return Array.from(arkham.plugins.LibraryRegistry.getLibraries());
    }
};