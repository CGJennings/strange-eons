package ca.cgjennings.apps.arkham.plugins.typescript;

/**
 * Interface that provides direct access to language services from Java.
 *
 * Interface that defines the services
 * The primary interface to TypeScript language services.
 * This class allows you to hand off service requests to be performed in
 * the background. A callback that you provide will be called with
 * the result once it is available.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface TypeScriptServices {
    // Note: the implementation of this interface is defined in javaBridge.js

    /**
     * Returns a string describing which version of TypeScript is being used.
     * 
     * @return a non-null version string, such as "4.2.0-dev"
     */
    String getVersion();

    /**
     * Transpiles the specified source code from TypeScript to JavaScript,
     * using default options.
     *
     * @param source the non-null code to compile
     * @return the transpiler output
     */
    String transpile(String source);
}
