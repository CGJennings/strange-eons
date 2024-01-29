package ca.cgjennings.apps.arkham.plugins.typescript;

/**
 * Stores the result of compiling a source file.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class CompiledSource {
    public CompiledSource() {
    }

    /** The compilation root used to compile the file, if any. */
    public CompilationRoot compilationRoot;
    /** File name of the original TypeScript file. */
    public String sourceFile;
    /** File name of the transpiled JavaScript file. */
    public String jsFile;
    /** Text of the transpiled JavaScript file. */
    public String js;
    /** File name of the source map. */
    public String mapFile;
    /** Text of the source map. */
    public String map;
    
    @Override
    public String toString() {
        return "// CompiledSource for \"" + sourceFile + "\"\n"
                + "//   has js output: " + (js != null) + '\n'
                + "//   has source map: " + (map != null) + '\n'
                + (js != null ? js : "");
    }
}
