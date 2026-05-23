package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.algo.SplitJoin;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import java.util.concurrent.atomic.AtomicInteger;
import org.mozilla.javascript.Context;

/**
 * A {@code Runnable} that can execute script code in another thread. This can
 * be used to parallelize script code using {@link SplitJoin}. It is also used
 * by the {@code threads} library to support with script code.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ScriptedRunnable implements Runnable {

    /**
     * A runnable that executes the script code represented by the specified
     * {@code ScriptedRunnable.Future} instance. From script code, a
     * {@code ScriptedRunnable} can be instantiated by passing any function to
     * this constructor. A trivial example:
     * <pre>
     * importClass( ca.cgjennings.apps.arkham.plugins.ScriptedRunnable );
     * importClass( ca.cgjennings.apps.arkham.plugins.ScriptedRunnable.Future );
     * new ScriptedRunnable( function f() { println('f()'); } ).run();
     * </pre>
     *
     * @param task a function to be executed when this runnable is
     * {@link #run()}
     */
    public ScriptedRunnable(Future task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        scriptedThread = task;
    }

    /**
     * Implements the {@code Runnable} interface required for threads and
     * {@link SplitJoin} tasks. This will perform the necessary setup to execute
     * script code in a given thread, call the scripted function (with no
     * arguments), then clean up the script execution environment.
     */
    @Override
    public void run() {
        ScriptDebugging.prepareToEnterContext();
        Context.enter();
        try {
            value = scriptedThread.run();
        } catch (Throwable t) {
            isExceptional = true;
            value = t;
        } finally {
            Context.exit();
            hasValue = true;
        }
    }

    /**
     * Returns the return value of the script function. If an exception was
     * thrown by the function, it will be thrown when this is called. If the
     * function has not completed or returned no value, returns {@code null}.
     *
     * @return the return value of the function, or {@code null}
     */
    public Object getReturnValue() {
        boolean done = hasValue;
        if (done) {
            if (isExceptional) {
                Context.throwAsScriptRuntimeEx((Throwable) value);
            }
            return value;
        } else {
            return null;
        }
    }

    /**
     * Returns {@code true} if the runnable has completed.
     *
     * @return {@code true} if the runnable finished running
     */
    public boolean hasReturnValue() {
        return hasValue;
    }

    private Future scriptedThread;
    private volatile boolean isExceptional;
    private volatile boolean hasValue;
    private volatile Object value;

    /**
     * Returns a unique default name for a thread running in a script. The name
     * consists of the text "Scripted Thread", a space, and an integer that
     * increases monotonically with each call.
     *
     * @return a default thread name
     */
    public static String getDefaultThreadName() {
        return "Scripted Thread " + counter.getAndIncrement();
    }
    private static final AtomicInteger counter = new AtomicInteger(1);

    /**
     * The interface that will be implemented using the function passed to the
     * constructor. This is then called to execute the script code by the
     * {@code Runnable} implementation.
     */
    public interface Future {

        public Object run();
    }
}
