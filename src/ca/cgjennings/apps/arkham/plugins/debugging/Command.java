package ca.cgjennings.apps.arkham.plugins.debugging;

import resources.CacheMetrics;

/**
 * An enumeration of the commands recognized by the network protocol of the
 * {@linkplain DefaultScriptDebugger default debugger implementation}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see DefaultScriptDebugger
 */
public enum Command {
    /**
     * Verify that server is up. Returns a number indicating the number of stack
     * frame updates, which can be polled to determine when a breakpoint
     * changes.
     */
    PROBE(0),
    /**
     * Returns a description of the server to help tell multiple servers on the
     * same device apart.
     */
    SERVERINFO(0),
    /**
     * Stop the running script.
     */
    STOP(0),
    /**
     * Returns 0 or 1 depending on whether a breakpoint has been hit. If the
     * first line is 1 (indicating that a script is interrupted), then the next
     * two lines will be the name of the interrupted thread and the name of the
     * exception (if any) that caused the interrupt.
     */
    INTERRUPTED(0),
    /**
     * Continue running until the next breakpoint.
     */
    CONTINUE(0),
    /**
     * Break at the next opportunity.
     */
    BREAK(0),
    /**
     * Step over the current line.
     */
    STEPOVER(0),
    /**
     * Step into functions on the current line.
     */
    STEPINTO(0),
    /**
     * Step out of the current function.
     */
    STEPOUT(0),
    /**
     * Get a list of all script file names, one per line.
     */
    FILELIST(0),
    /**
     * Given a script file, get the script source code.
     */
    SOURCE(1),
    /**
     * Given a script file, get a list of breakable lines. This is a list of
     * line numbers for which a break is possible, one line number per line.
     * Each line number is followed by another line, either "-" (no break set)
     * or "X" (break set).
     */
    BREAKPOINTS(1),
    /**
     * Toggle a breakpoint in the given file and line number.
     */
    TOGGLEBREAK(2),
    /**
     * Clear all breakpoints in the specified file.
     */
    CLEARBREAKPOINTS(1),
    /**
     * Set break-on-enter off or on using 0 or 1.
     */
    BREAKONENTER(1),
    /**
     * Set break-on-exit off or on using 0 or 1.
     */
    BREAKONEXIT(1),
    /**
     * Set break-on-exception off or on using 0 or 1.
     */
    BREAKONTHROW(1),
    /**
     * Set break-on-debugger-statement off or on using 0 or 1.
     */
    BREAKONDEBUGGER(1),
    /**
     * Returns whether break-on-X is enabled, one per line as 0 or 1.
     */
    BREAKSTATUS(0),
    /**
     * Returns the current call stack as a sequence of pairs of lines with the
     * format: file, line number.
     */
    CALLSTACK(0),
    /**
     * Evaluates an expression in the context of a stack frame. The first
     * argument is an index indicating the stack frame (0 is the top). The
     * second argument is the expression to evaluate.
     */
    EVAL(2),
    /**
     * Obtain a list of the immediate child properties of an object in scope.
     * The first argument is an index indicating the stack frame (0 is the top).
     * The second argument identifies the object to list the properties of; it
     * is a chain of null-character separated symbols, starting with either
     * {@code &lt;scope&gt;} or {@code &lt;this&gt;} to begin from the local
     * scope or the current {@code this} object (respectively).
     *
     */
    SCOPE(2),
    /**
     * Acts as per {@link #SCOPE}, but returns the string representation of the
     * value of the specified object instead of its child properties.
     */
    SCOPEEVAL(2),
    /**
     * Returns a list of the names of available data tables from the server.
     * This is a list of all generators registered with {@link Tables}, one per
     * line.
     */
    INFOTABLELIST(0),
    /**
     * Returns the serialized form of the named {@link InfoTable}. The argument
     * is one of the names returned using {@link #INFOTABLELIST}.
     */
    INFOTABLE(1),
    /**
     * Returns a list of information about registered {@link CacheMetrics}
     * instances. The result consists of one entry per metrics instance. Each
     * entry consists of three lines: the first line is the metric's name, a
     * colon, and its status string, the next is the content type class name,
     * and the last is Y or N as the cache is clearable or not. The command
     * takes a single integer parameter. If the integer value is -1, no
     * additional action is taken. If it is the index of a valid entry in the
     * returned list, then the matching cache will be cleared before returning
     * the list. If the value is -2, all clearable caches are cleared.
     */
    CACHEMETRICS(1),;

    private final int argCount;

    private Command(int argCount) {
        this.argCount = argCount;
    }

    /**
     * Returns the number of arguments required by the command.
     *
     * @return the arity of this {@code Command}
     */
    public int getArgCount() {
        return argCount;
    }

    /**
     * Escapes raw text that will be displayed within HTML content.
     *
     * @param s the string to escape
     * @return the content of {@code s}, with the characters &lt;, &gt;, and
     * &amp; converted to the appropriate HTML entity values
     */
    public static String escapeHTML(String s) {
        int col = 0;
        StringBuilder b = new StringBuilder(s.length() + 10);
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    b.append("&amp;");
                    ++col;
                    break;
                case '<':
                    b.append("&lt;");
                    ++col;
                    break;
                case '>':
                    b.append("&gt;");
                    ++col;
                    break;
                case '\t':
                    int spaces = ((col / 4) + 1) * 4 - col;
                    for (int sp = 0; sp < spaces; ++sp) {
                        b.append(' ');
                    }
                    col += spaces;
//					b.append( "    " );
                    break;
                default:
                    b.append(c);
                    ++col;
            }
        }
        return b.length() == s.length() ? s : b.toString();
    }

    /**
     * Escape server arguments and responses.
     *
     * @param s the plain string
     * @return an escaped string
     */
    public static String escapeProtocolText(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    b.append("\\\\");
                    break;
                case '\0':
                    b.append("\\0");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                default:
                    b.append(c);
            }
        }
        return b.length() == s.length() ? s : b.toString();
    }

    /**
     * Converts a string escaped with {@link #escapeProtocolText} back to the
     * original string.
     *
     * @param s the string to convert
     * @return the unescaped string
     */
    public static String unescapeProtocolText(String s) {
        StringBuilder b = new StringBuilder(s.length());
        boolean esc = false;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (esc) {
                switch (c) {
                    case '0':
                        b.append('\0');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 'n':
                        b.append('\n');
                        break;
                    default:
                        b.append(c);
                }
                esc = false;
            } else {
                if (c == '\\') {
                    esc = true;
                } else {
                    b.append(c);
                }
            }
        }
        return b.length() == s.length() ? s : b.toString();
    }
}
