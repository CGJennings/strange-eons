package ca.cgjennings.apps.arkham.plugins.typescript;

/**
 * Encapsulates a diagnostic message arising from the compilation process.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Diagnostic {
    public int code;
    public String message;
    public String file;
    public int line = -1;
    public int col = -1;
    public int offset = -1;
    public int length = 0;
    public boolean isWarning;

    public Diagnostic(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Diagnostic(int code, String message, String file, int line, int col, int offset, int length) {
        this.code = code;
        this.message = message;
        this.file = file;
        this.line = line;
        this.col = col;
        this.offset = offset;
        this.length = length;
    }
    
    public boolean hasLocation() {
        return file != null;
    }
   
    @Override
    public String toString() {
        return message;
    }
    
    public String toLongString() {
        String s = message;
        if (hasLocation()) {
            s += "\n\tat " + file + ':' + (line + 1);
        }
        return s;
    }
}
