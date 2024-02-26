package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.typescript.TSLanguageServices;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import resources.ResourceKit;

/**
 * Helper methods that support the implementation of the script library.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3
 */
public final class LibImpl {

    private LibImpl() {
    }

    /**
     * Regular expression that matches format specifiers.
     */
    private static final Pattern formatSpecPattern = Pattern.compile(
            "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])"
    );
    /**
     * Group containing the index "n$".
     */
    private static final int GR_INDEX = 1;
    /**
     * Group containing the flags.
     */
    private static final int GR_FLAGS = 2;
    /**
     * Group containing the width.
     */
    private static final int GR_WIDTH = 3;
    /**
     * Group containing the precision.
     */
    private static final int GR_PREC = 4;
    /**
     * Group containing the time marker [tT].
     */
    private static final int GR_TIME = 5;
    /**
     * Group containing the conversion code such as d or f.
     */
    private static final int GR_CONV = 6;

    /**
     * Formats a string using C-style % format codes. The result is nearly
     * identical to {@code String.format} but it is more lenient about which
     * format specifiers will accept which types.
     *
     * <p>
     * In particular, numeric format codes accept any type of number. If a
     * floating point type is passed to an integer format specifier, or
     * vice-versa, it will be converted to the appropriate type rather than
     * throwing an error.
     *
     * @param loc The locale to apply during formatting. If null then no
     * localization is applied.
     *
     * @param format A format string as described in <a href="#syntax">Format
     * string syntax</a>
     *
     * @param args Arguments referenced by the format specifiers in the format
     * string. If there are more arguments than format specifiers, the extra
     * arguments are ignored. The maximum number of arguments is limited by the
     * maximum dimension of a Java array as defined by
     * <cite>The Java&trade; Virtual Machine Specification</cite>.
     * @return the formatted string
     */
    public static String sprintf(Locale loc, String format, Object... args) {
        /*
        We keep the input args separate and convert them every time they are used.
        This may be expensive for some rarely used typed (e.g. BigDecimal) but
        since any arg can be referenced multiple times ("%d %1$f %<d") converting
        in place won't work.
        
        The strategy used is to build the output string one segment at a time.
        Regular text is appended to the output. When a format specifier is found,
        identify which argument it refers to, get and convert that argument,
        rewrite the specifier so it contains no argument index, and then format
        and append just the specifier and its converted argument. This way,
        we should retain forward compatibility with Formatter since it does the
        actual formatting, and it will also have the same exception behaviour,
        less the exceptions that would have been caused by our conversions.
         */
        StringBuilder b = new StringBuilder(format.length() + 16);
        try (Formatter fmt = new Formatter(b, loc)) {
            Matcher m = formatSpecPattern.matcher(format);
            // For args with no $ or <
            int independentIndex = 0;
            int lastIndex = -1;
            int lastMatchEndedAt = 0;
            while (m.find()) {
                // is there plain text to insert before the next specifier?
                if (m.start() > lastMatchEndedAt) {
                    // we format this with no arguments because it could have an
                    // invalid specifier which should throw an exception; trying
                    // to format it detects this
                    fmt.format(format.substring(lastMatchEndedAt, m.start()));
                }

                boolean rewriteFormatSpec = false;
                int inputIndex = -1;

                // determine which input argument to use
                //
                // an undocumented behaviour of Formatter is that format specs
                // with both an absolute and relative index (%3$<s) use relative
                if (m.group(GR_FLAGS) != null && m.group(GR_FLAGS).indexOf('<') >= 0) {
                    // relative index %<d:
                    // if there is no lastIndex, we do not rewrite the spec, so it
                    // still has the < in it, and thus we get the right exception
                    if (lastIndex >= 0) {
                        inputIndex = lastIndex;
                        rewriteFormatSpec = true;
                    } else {
                        inputIndex = 0;
                    }
                } else if (m.group(GR_INDEX) != null) {
                    // absolute index %n$d
                    final String indexText = m.group(GR_INDEX);
                    inputIndex = Integer.parseInt(indexText.substring(0, indexText.length() - 1)) - 1;
                    rewriteFormatSpec = true; // rewrite to remove n$ part
                }
                // no index was specified, use the next available
                if (inputIndex == -1) {
                    inputIndex = independentIndex++;
                }
                lastIndex = inputIndex; // ready for < in next format spec

                Object argument;
                if (inputIndex < args.length) {
                    argument = args[inputIndex];
                } else {
                    // if the argument doesn't exist, we carry on but at format
                    // time we will pass zero arguments to get the right exception
                    argument = null;
                    inputIndex = -1;
                }

                // coerce numeric types to their conversion type
                char conversion = m.group(GR_CONV).charAt(0);
                if (m.group(GR_TIME) != null) {
                    argument = coerceToDate(argument);
                } else {
                    // allow %i like C sprintf
                    if (conversion == 'i') {
                        conversion = 'd';
                        rewriteFormatSpec = true;
                    }
                    switch (conversion) {
                        // integer conversions
                        case 'd':
                        case 'o':
                        case 'x':
                        case 'X':
                            argument = coerceToInteger(argument);
                            break;
                        // floating point conversions
                        case 'e':
                        case 'E':
                        case 'f':
                        case 'g':
                        case 'G':
                        case 'a':
                        case 'A':
                            argument = coerceToFloatingPoint(argument);
                            break;
                        // character conversions
                        case 'c':
                        case 'C':
                            argument = coerceToCharacter(argument);
                            break;
                    }
                }

                String formatSpec;
                if (rewriteFormatSpec) {
                    formatSpec = "%"
                            + groupOrEmpty(m, GR_FLAGS).replace("<", "")
                            + groupOrEmpty(m, GR_WIDTH)
                            + groupOrEmpty(m, GR_PREC)
                            + groupOrEmpty(m, GR_TIME)
                            + conversion;
                } else {
                    formatSpec = m.group();
                }

                if (inputIndex >= 0) {
                    fmt.format(formatSpec, argument);
                } else {
                    fmt.format(formatSpec); // throws
                }
                lastMatchEndedAt = m.end();
            }
            // append any final plain text
            if (lastMatchEndedAt < format.length()) {
                fmt.format(format.substring(lastMatchEndedAt, format.length()));
            }
        }

        return b.toString();
    }

    private static String groupOrEmpty(Matcher m, int group) {
        return m.group(group) == null ? "" : m.group(group);
    }

    /**
     * Coerce the object to an integer type if it is a floating point type (big
     * decimal, double, float). If the input matches none of these types, the
     * object is returned unchanged (and any errors that would have happened
     * still do).
     *
     * @param o the object to coerce
     * @return the original object, or an integer representation of a floating
     * point input
     */
    private static Object coerceToInteger(Object o) {
        if (o instanceof BigDecimal) {
            o = ((BigDecimal) o).toBigInteger();
        } else if ((o instanceof Double) || (o instanceof Float)) {
            o = ((Number) o).longValue();
        } else if (o instanceof Character) {
            o = Integer.valueOf(((Character) o));
        }
        return o;
    }

    /**
     * Coerce the object to a character type. The input is first coerced to an
     * integer, and then if the result is an integer type that is wider than a
     * int, it is coerced to an int (i.e., a code point). If the input matches
     * none of these types, the object is returned unchanged (and any errors
     * that would have happened still do).
     *
     * @param o the object to coerce
     * @return the original object, or an integer representation of a floating
     * point input
     */
    private static Object coerceToCharacter(Object o) {
        o = coerceToInteger(o);
        if (o instanceof BigDecimal || o instanceof Long) {
            o = ((Number) o).intValue();
        }
        return o;
    }

    /**
     * Coerce the object to an floating point type if it is an integer type (big
     * integer, long, int, char, short, byte). If the input matches none of
     * these types, the object is returned unchanged (and any errors that would
     * have happened still do).
     *
     * @param o the object to coerce
     * @return the original object, or an integer representation of a floating
     * point input
     */
    private static Object coerceToFloatingPoint(Object o) {
        if (o instanceof BigInteger) {
            o = new BigDecimal((BigInteger) o);
        } else if (o instanceof Number) {
            if (!(o instanceof Float)) {
                o = ((Number) o).doubleValue();
            }
        } else if (o instanceof Character) {
            o = Float.valueOf(((Character) o));
        }
        return o;
    }

    /**
     * Coerce the object to a date type. If the object is a number, it is
     * converted to a long value. Otherwise, the object is returned unchanged
     * (and any errors that would have happened still do).
     *
     * @param o the object to coerce
     * @return the original object, or a long version of its value
     */
    private static Object coerceToDate(Object o) {
        if (o instanceof Number) {
            o = ((Number) o).longValue();
        }
        return o;
    }

    /**
     * Implements CommonJS require() support.
     *
     * @param requireSourcePath the path to the script that is calling require,
     *     to resolve against a relative module path
     * @param modulePath the relative module path to load
     * @param module the module object to pass to the module
     * @param exports the module.exports object to pass to the module
     * @returns an object containing the module exports
     */
    public static Object require(String requireSourcePath, String modulePath, Scriptable module, Scriptable exports, Scriptable cache) throws IOException {
        modulePath = joinModulePath(requireSourcePath, modulePath);
        if (cache.has(modulePath, cache)) {
            return cache.get(modulePath, cache);
        }

        final ModuleText source = findModuleText(modulePath);
        if (source == null) {
            throw new FileNotFoundException("module not found: " + modulePath);
        }

        // execute the module in a separate scope
        Object exported;
        ScriptMonkey m = new ScriptMonkey(source.path);
        if (source.path.endsWith(".json")) {
            m.bind("jsonData", source.text);
            exported = m.eval("JSON.parse(jsonData)");
        } else {
            // found a .ts module but no precompiled .ts.js file
            if (source.path.endsWith(".ts")) {
                StrangeEons.log.log(Level.INFO, "transpiling \"{0}\" on demand", source.path);
                source.text = TSLanguageServices.getShared().transpile(source.path, source.text);
            }
            m.bind("module", module);
            m.bind("exports", exports);
            m.bind("modCache", cache);
            m.eval("require.cache=modCache;delete modCache;");    
            m.eval(source.text);
            exported = module.get("exports", module);
        }

        // add to module cache under both the original and modified paths
        cache.put(modulePath, cache, exported);
        if (!source.path.equals(modulePath)) {
            cache.put(source.path, cache, exported);
        }

        return exported;
    }

    /**
     * Given a path and a child path, return the child path resolved against the
     * parent path. The child path may be absolute, in which case it is
     * returned unchanged. If the child path is relative, it is resolved against
     * the parent path.
     * 
     * @param path the parent path
     * @param child the child path
     * @return the combined path
     */
    public static String joinModulePath(String path, String child) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (child == null || child.isEmpty()) {
            return path;
        }

        // normalize path separators
        path = path.replace('\\', '/');
        child = child.replace('\\', '/');
        if (child.endsWith("/")) {
            child = child.substring(0, child.length() - 1);
        }

        // if the child is absolute, return it
        if (child.startsWith("/")) {
            return child;
        }
        Matcher m = PAT_PROTOCOL.matcher(child);
        if (m.find()) {
            return child;
        }

        // if the path starts with a protocol, snip it off and save it for later, including the //
        String proto = "";
        m = PAT_PROTOCOL.matcher(path);
        if (m.find()) {
            proto = m.group(1);
            path = path.substring(proto.length());
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // resolve the child path against the parent path
        String[] parentParts = path.split("/");
        String[] childParts = child.split("/");
        String[] resolved = new String[parentParts.length + childParts.length];

        int next = 0;
        String[] source = parentParts;
        for (int s=0; s<2; ++s) {
            for (int i=0; i<source.length; ++i) {
                if (source[i].equals("..")) {
                    if (next > 0) {
                        --next;
                    }
                } else if (!source[i].equals(".")) {
                    resolved[next++] = source[i];
                }
            }
            source = childParts;
        }

        StringBuilder b = new StringBuilder(path.length() + child.length());
        b.append(proto);
        for (int i=0; i<next; ++i) {
            b.append(resolved[i]).append('/');
        }
        if (b.length() > 0) {
            b.setLength(b.length() - 1);
        }
        return b.toString();
    }
    private static final Pattern PAT_PROTOCOL = Pattern.compile("^(\\w+:/+)");
    
    private static final class ModuleText {
        public ModuleText(String path, String text) {
            this.path = path;
            this.text = text;
        }
        String path;
        String text;
    }
    
    /**
     * Given the path of a {@code require}d file, possibly without an extension,
     * locate and return the actual source text and location.
     * 
     * @param modulePath an absolute path to a module name
     * @return an object describing the true location and full text, or null if not found
     * @throws IOException if the module file exists but cannot be read
     */
    private static ModuleText findModuleText(String modulePath) throws IOException {
        // if modulePath includes an extension, look for the file as specified
        final int dot = modulePath.lastIndexOf('.');
        final int slash = modulePath.lastIndexOf('/');
        if (dot > 0 && dot > slash) {
            String text = readModuleText(modulePath);
            return text == null ? null : new ModuleText(modulePath, text);
        }

        // otherwise, try standard extensions in preferred order
        for (int i=0; i<MODULE_EXTENSIONS.length; ++i) {
            final String pathToTry = modulePath + MODULE_EXTENSIONS[i];
            String text = readModuleText(pathToTry);
            if (text != null) {
                return new ModuleText(pathToTry, text);
            }
        }

        return null;
    }
    /** Extensions to check when a module is required with no explicit extension, in preferred order. */
    private static final String[] MODULE_EXTENSIONS = new String[] {
        ".ts.js", ".js", "/index.ts.js", "/index.js"
    };
    
    private static String readModuleText(String path) throws IOException {
        URL url;

        // check if this is a complete, non-res:// URL
        if (path.contains(":/") && !path.startsWith("res:")) {
            url = new URL(path);
        } else {
            url = ResourceKit.composeResourceURL(path);
            if (url == null) return null;
        }

        try (InputStream in = url.openStream()) {
            StringWriter sw = new StringWriter(512);
            ProjectUtilities.copyReader(new InputStreamReader(in, ProjectUtilities.ENC_UTF8), sw);
            return sw.toString();
        } catch (FileNotFoundException ex) {
            return null; // other I/O errors throw normally
        }
    }

    /**
     * Information about a single frame on the script call stack.
     */
    public static final class ScriptTraceElement {

        private String file;
        private int line;

        public ScriptTraceElement(String file, int line) {
            if (file == null) {
                throw new NullPointerException("file");
            }
            this.file = file;
            this.line = line;
        }

        public String getFile() {
            return file;
        }

        public int getLine() {
            return line;
        }

        @Override
        public String toString() {
            return file + ":" + line;
        }
    }

    /**
     * Return an array of {@link ScriptTraceElement}s that represents the script
     * stack frames for the current thread.
     *
     * @return returns a script stack trace for the current thread
     */
    public static ScriptTraceElement[] getScriptTrace() {
        List<ScriptTraceElement> stack = new LinkedList<>();

        CharArrayWriter writer = new CharArrayWriter();
        try {
            Context.throwAsScriptRuntimeEx(new RuntimeException());
        } catch (Throwable t) {
            t.printStackTrace(new PrintWriter(writer));
        }

        String s = writer.toString();
        int open = -1;
        int close = -1;
        int colon = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ':') {
                colon = i;
            } else if (c == '(') {
                open = i;
            } else if (c == ')') {
                close = i;
            } else if (c == '\n' && open != -1 && close != -1 && colon != -1
                    && open < colon && colon < close) {
                String file = s.substring(open + 1, colon);
                if (!file.endsWith(".java")) {
                    int line = -1;
                    String lineStr = s.substring(colon + 1, close);
                    try {
                        line = Integer.parseInt(lineStr);
                        if (line < 0) {
                            line = 0;
                        }
                    } catch (NumberFormatException e) {
                    }
                    stack.add(new ScriptTraceElement(file, line));
                }
                open = close = colon = -1;
            }
        }

        return stack.toArray(ScriptTraceElement[]::new);
    }
}
