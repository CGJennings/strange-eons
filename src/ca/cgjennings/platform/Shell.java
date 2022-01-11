package ca.cgjennings.platform;

import ca.cgjennings.io.StreamPump;
import ca.cgjennings.util.CommandFormatter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A Shell executes commands as if from the command line for the purpose of
 * automating platform-specific tasks.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Shell {

    private int timeout;
    private LinkedList<File> dirStack;
    private final LinkedList<String> command = new LinkedList<>();
    private final ProcessBuilder pb = new ProcessBuilder(command);

    /**
     * Creates a new Shell for executing commands.
     */
    public Shell() {
        pb.redirectErrorStream(true);
    }

    /**
     * Returns the working directory for executing commands.
     *
     * @return the working directory
     */
    public synchronized File directory() {
        return pb.directory();
    }

    /**
     * Changes the working directory that will be used to execute subsequent
     * commands.
     *
     * @param dir the directory; if {@code null}, use the working directory of
     * the parent process
     */
    public synchronized void directory(File dir) {
        pb.directory(dir);
    }

    /**
     * Pushes the current directory onto a stack for later recall.
     */
    public synchronized void pushDirectory() {
        if (dirStack == null) {
            dirStack = new LinkedList<>();
        }
        dirStack.push(pb.directory());
    }

    /**
     * Sets the directory to the most recently pushed directory, removing it
     * from the directory stack.
     *
     * @throws NoSuchElementException if the directory stack is empty
     */
    public synchronized void popDirectory() {
        if (dirStack == null || dirStack.isEmpty()) {
            throw new NoSuchElementException("stack is empty");
        }
        pb.directory(dirStack.pop());
    }

    /**
     * Returns a modifiable map of the environment variables used to execute
     * commands.
     *
     * @return the shell's environment variables
     */
    public Map<String, String> environment() {
        return pb.environment();
    }

    /**
     * Returns the current timeout value.
     *
     * @return returns the maximum time, in milliseconds, that a command can
     * execute, or 0 if timeouts are disabled
     */
    public synchronized int timeout() {
        return timeout;
    }

    /**
     * Sets a timeout for executing commands, in milliseconds. Any command that
     * does not complete within the timeout will be forcibly terminated. Setting
     * the timeout to 0 will disable timeouts.
     *
     * @param timeout the timeout value, in milliseconds, or 0 for no timeout
     */
    public synchronized void timeout(int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0: " + timeout);
        }
        this.timeout = timeout;
    }

    /**
     * Returns a copy of the last command that was started, or an empty array if
     * no commands have been executed yet.
     *
     * @return the last executed command
     */
    public synchronized String[] lastCommand() {
        return command.toArray(new String[0]);
    }

    /**
     * Executes a command. If non-{@code null}, the contents of {@code stdin}
     * will be used as the input stream for the command.
     *
     * @param commandTokens an array of command arguments
     * @param stdin the standard input stream content, or {@code null}
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     */
    public synchronized Result exec(String[] commandTokens, String stdin) throws IOException {
        if (commandTokens == null) {
            throw new NullPointerException("commandTokens");
        }
        if (commandTokens.length == 0) {
            throw new IllegalArgumentException("commandTokens is empty");
        }

        fillInCommand(commandTokens, false);
        return execImpl(stdin);
    }

    /**
     * Executes a command. If non-{@code null}, the contents of {@code stdin}
     * will be used as the input stream for the command.
     *
     * @param commandTokens an list of command arguments
     * @param stdin the standard input stream content, or {@code null}
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     */
    public Result exec(List<String> commandTokens, String stdin) throws IOException {
        if (commandTokens == null) {
            throw new NullPointerException("commandTokens");
        }
        return exec(commandTokens.toArray(new String[0]), stdin);
    }

    /**
     * Executes a command.
     *
     * @param commandTokens an list of command arguments
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     */
    public Result exec(List<String> commandTokens) throws IOException {
        if (commandTokens == null) {
            throw new NullPointerException("commandTokens");
        }
        return exec(commandTokens.toArray(new String[0]), null);
    }

    /**
     * Executes a command as a superuser, if possible. If non-{@code null}, the
     * contents of {@code stdin} will be used as the input stream for the
     * command.
     *
     * @param commandTokens an array of command arguments
     * @param stdin the standard input stream content, or {@code null}
     * @param password the superuser account password
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     */
    public Result sudo(String[] commandTokens, String stdin, String password) throws IOException {
        if (commandTokens == null) {
            throw new NullPointerException("commandTokens");
        }
        if (commandTokens.length == 0) {
            throw new IllegalArgumentException("commandTokens is empty");
        }

        stdin = stdin == null ? password : password + '\n' + stdin;
        if (!PlatformSupport.PLATFORM_IS_WINDOWS) {
            fillInCommand(new String[]{"sudo", "-S"}, false);
        }
        fillInCommand(commandTokens, true);
        return execImpl(stdin);
    }

    /**
     * Executes a command as a superuser, if possible. If non-{@code null}, the
     * contents of {@code stdin} will be used as the input stream for the
     * command.
     *
     * @param commandTokens an array of command arguments
     * @param stdin the standard input stream content, or {@code null}
     * @param password the superuser account password
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     */
    public Result sudo(List<String> commandTokens, String stdin, String password) throws IOException {
        if (commandTokens == null) {
            throw new NullPointerException("commandTokens");
        }

        return sudo(commandTokens.toArray(new String[0]), stdin, password);
    }

    /**
     * Executes a command.
     *
     * @param commandTokens an array of command arguments
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     */
    public Result exec(String... commandTokens) throws IOException {
        return exec(commandTokens, null);
    }

    /**
     * Executes a command by parsing the specified string into a command name
     * and arguments using the built-in {@link CommandFormatter}.
     *
     * @param command a command string
     * @param stdin the standard input stream content, or {@code null}
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     * @see #commandFormatter()
     */
    public Result pexec(String command, String stdin) throws IOException {
        if (command == null) {
            throw new NullPointerException("command");
        }
        return exec(tokenize(command), stdin);
    }

    /**
     * Executes a command by parsing the specified string into a command name
     * and arguments using the built-in {@link CommandFormatter}.
     *
     * @param command a command string
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     * @see #commandFormatter()
     */
    public Result pexec(String command) throws IOException {
        if (command == null) {
            throw new NullPointerException("command");
        }
        return exec(tokenize(command), null);
    }

    /**
     * Executes a command by parsing the specified string into a command name
     * and arguments using the built-in {@link CommandFormatter}. The command is
     * executed as a superuser, if possible.
     *
     * @param command a command string
     * @param stdin the standard input stream content, or {@code null}
     * @param password the superuser account password
     * @return the outcome of the command, or {@code null} if a timeout is set
     * and the command timed out
     * @throws IOException if an I/O error occurs; typically this occurs if the
     * command is not found
     * @see #commandFormatter()
     */
    public Result psudo(String command, String stdin, String password) throws IOException {
        if (command == null) {
            throw new NullPointerException("command");
        }
        if (password == null) {
            password = "";
        }
        return sudo(tokenize(command), stdin, password);
    }

    /**
     * Returns the command formatter that is used to parse commands. The command
     * formatter is used by {@link #pexec} and {@link #psudo} to split whole
     * command strings into command tokens. By modifying the formatter, you can
     * define custom variables to use during parsing.
     *
     * @return the command formatter used by this Shell
     */
    public synchronized CommandFormatter commandFormatter() {
        if (cf == null) {
            cf = new CommandFormatter();
        }
        return cf;
    }
    private CommandFormatter cf;

    /**
     * Captures the result of executing a command.
     */
    public static final class Result {

        private final String[] tokens;
        private final int code;
        private final String output;

        private Result(String[] tokens, int exitCode, String output) {
            this.tokens = tokens;
            this.code = exitCode;
            this.output = output;
        }

        /**
         * Returns a string that describes the command that was executed to
         * produce this result.
         *
         * @return a string describing the command that is formed by
         * concatenating the command tokens
         */
        public String command() {
            StringBuilder b = new StringBuilder(256);
            for (int i = 0; i < tokens.length; ++i) {
                if (i > 0) {
                    b.append(' ');
                }
                b.append(tokens[i]);
            }
            return b.toString();
        }

        /**
         * Returns a copy of the command tokens that were executed to produce
         * this result.
         *
         * @return a copy of the parsed command tokens
         */
        public String[] tokens() {
            return tokens.clone();
        }

        /**
         * Returns the merged output that the command sent to stdout and stderr
         * as a string.
         *
         * @return the command's console output
         */
        public String output() {
            return output;
        }

        /**
         * Returns the exit code of the command. An exit code of 0 indicates
         * success; other values typically indicate errors, but there is no
         * standard mapping of exit codes to specific errors.
         *
         * @return the exit code returned by the application
         */
        public int exitCode() {
            return code;
        }

        @Override
        public String toString() {
            return output();
        }
    }

    /**
     * Tokenizes a command string into an array of command tokens.
     *
     * @param command the command
     * @return the tokens
     */
    private synchronized String[] tokenize(String command) {
        return commandFormatter().formatCommand(command);
    }

    /**
     * Executes the command tokens stored in {@code this.command}.
     *
     * @param stdin the input stream, or {@code null}
     * @return the result of the command
     * @throws IOException if an exception occurs
     */
    private synchronized Result execImpl(String stdin) throws IOException {
        PrintStream procInput = null;
        try {
            final Process proc = pb.start();

            if (stdin != null) {
                procInput = new PrintStream(proc.getOutputStream());
                for (String line : stdin.split("\n")) {
                    procInput.println(line);
                }
                procInput.close();
                procInput = null;
            }

            Timer cancelTimer = null;
            if (timeout > 0) {
                final Thread procThread = Thread.currentThread();
                cancelTimer = new Timer("Shell timer");
                cancelTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        procThread.interrupt();
                    }
                }, timeout);
            }

            try {
                int exitCode = proc.waitFor();
                if (cancelTimer != null) {
                    cancelTimer.cancel();
                }
                StringWriter sw = new StringWriter(256);
                StreamPump.copy(new InputStreamReader(proc.getInputStream()), sw);
                sw.flush();
                return new Result(command.toArray(new String[0]), exitCode, sw.toString());
            } catch (InterruptedException e) {
                proc.destroy();
                return null;
            }
        } finally {
            if (procInput != null) {
                procInput.close();
            }
        }
    }

    /**
     * Build a list of command tokens from an array. If {@code append} is
     * {@code true}, the tokens are appended to the current list. Otherwise, the
     * current list is cleared before adding the tokens.
     *
     * @param tokens the command tokens to append
     * @param append if {@code true}, do not clear the existing token list
     */
    private void fillInCommand(String[] tokens, boolean append) {
        if (!append) {
            command.clear();
        }
        if (PlatformSupport.PLATFORM_IS_WINDOWS) {
            command.add("cmd");
            command.add("/c");
        }
        command.addAll(Arrays.asList(tokens));
    }
}
