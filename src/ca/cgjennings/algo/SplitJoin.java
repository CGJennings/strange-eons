package ca.cgjennings.algo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Simplifies running an algorithm in parallel on multiple CPUs (or CPU cores).
 * Like the fork-join model, it is used to break a problem into independent
 * subproblems, the results of which are combined to produce a final result.
 * Unlike the fork-join model, the split-join model only splits the problem up
 * one time, whereas fork-join repeats the split recursively. The split-join
 * model thus avoids some of the overhead of subproblem creation, but is more
 * sensitive to having its performance degraded by a rate-limiting subproblem.
 * The split-join model is typically more efficient than fork-join when the
 * following conditions hold:
 * <ol>
 * <li> All of the computational units have similar performance characteristics
 * (multi-core CPUs, for example).
 * <li> The problem is CPU-bound so that performance is predictable.
 * <li> The problem can be broken down in roughly equal subproblems, each with
 * the same computational complexity. (Or, more generally, each subproblem can
 * be expected to complete in roughly the same amount of time.)
 * </ol>
 *
 * <p>
 * A typical pattern for using {@code SplitJoin} is as follows:
 * <ol>
 * <li>Obtain one of the available shared instances using
 * {@link #getInstance()}.
 * <li>Call that instance's {@link #getIdealSplitCount()} to determine the
 * number of subproblems to create.
 * <li>Create an array or collection or Runnables or Callables, each of which
 * will complete one subproblem.
 * <li>Use one of the {@code run} or {@code evaluate} methods to complete the
 * subproblems (in parallel if possible). This will return when all of the
 * subproblems have been completed.
 * <li>If necessary, combine the results of the subproblems.
 * </ol>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class SplitJoin {

    private static final int cpus = Runtime.getRuntime().availableProcessors();

    /**
     * A constructor to be used by concrete subclasses. To get an instance of a
     * suitable concrete subclass, use {@link #getInstance()} or
     * {@link #createInstance()}.
     */
    protected SplitJoin() {
    }

    /**
     * Returns a {@code SplitJoin} instance suited to the platform.
     *
     * @return a suitable instance; it may be new or shared
     */
    public static SplitJoin getInstance() {
        return instance.get();
    }
    private static final ThreadLocal<SplitJoin> instance = new ThreadLocal<SplitJoin>() {
        @Override
        protected SplitJoin initialValue() {
            return createInstance();
        }
    };

    /**
     * Returns a new {@code SplitJoin} instance suited to the platform. This
     * instance is guaranteed not to be shared.
     *
     * @return a new instance suitable for parallelizing CPU bound tasks
     */
    public static SplitJoin createInstance() {
        if (cpus == 1 || !allowThreads) {
            return new SerialSplitJoin();
        }
        return new ThreadPoolSplitJoin();
    }

    /**
     * Returns a new {@code SplitJoin} instance that will use the specified
     * number of threads. This instance is guaranteed not to be shared, and is
     * guaranteed to employ up to the specified number of threads (depending on
     * the number of subproblems).
     *
     * @param nThreads the number of threads to be used
     * @return a new instance that uses the specified number of threads
     * @throws IllegalArgumentException if the number of threads is not positive
     */
    public static SplitJoin createInstance(int nThreads) {
        if (nThreads < 1) {
            throw new IllegalArgumentException("nThreads " + nThreads);
        }
        if (nThreads == 1) {
            return new SerialSplitJoin();
        } else {
            return new ThreadPoolSplitJoin(nThreads);
        }
    }

    /**
     * Submits an array of subproblems for completion. The subproblems will not
     * return a result; if a return value is required for the join stage, submit
     * <b>Callable</b>s instead.
     *
     * @param subproblems the subproblems to complete
     * @throws ExecutionException if at least one subproblem throws an
     * exception, then this method will wrap and throw one of those exceptions
     * (which one cannot be guaranteed)
     */
    public abstract void run(Runnable[] subproblems) throws ExecutionException;

    /**
     * Submits a collection of subproblems for completion. The subproblems will
     * not return a result; if a return value is required for the join stage,
     * submit
     * <b>Callables</b> instead.
     *
     * @param subproblems the subproblems to complete
     * @throws ExecutionException if at least one subproblem throws an
     * exception, then this method will wrap and throw one of those exceptions
     * (which one cannot be guaranteed)
     */
    public abstract void run(Collection<? extends Runnable> subproblems) throws ExecutionException;

    /**
     * Submits subproblems for completion. Each subproblem is a callable that
     * returns a value of type V. The return values of each subproblem will be
     * returned in an array (in the same order as the subproblems).
     *
     * @param <V> the type of the value returned by each subproblem
     * @param subproblems the problems to complete
     * @return the individual return values from each subproblem
     * @throws ExecutionException if at least one subproblem throws an
     * exception, then this method will wrap and throw one of those exceptions
     * (which one cannot be guaranteed)
     */
    public abstract <V> List<V> evaluate(Collection<? extends Callable<V>> subproblems) throws ExecutionException;

    /**
     * Submits subproblems as if by {@code run( subproblems )}. However, if one
     * of the subproblems throws an exception, it will be thrown as an unchecked
     * {@code RuntimeException}.
     *
     * @param subproblems the subproblems to complete
     * @throws RuntimeException if one of the subproblems throws an exception
     * @see #run(java.lang.Runnable[])
     */
    public void runUnchecked(Runnable[] subproblems) {
        try {
            run(subproblems);
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Submits subproblems as if by {@code run( subproblems )}. However, if one
     * of the subproblems throws an exception, it will be thrown as an unchecked
     * {@code RuntimeException}.
     *
     * @param subproblems the subproblems to complete
     * @throws RuntimeException if one of the subproblems throws an exception
     * @see	#run(java.util.Collection)
     */
    public void runUnchecked(Collection<? extends Runnable> subproblems) {
        try {
            run(subproblems);
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Submits subproblems as if by {@code evaluate( subproblems )}. However, if
     * one of the subproblems throws an exception, it will be thrown as an
     * unchecked {@code RuntimeException}.
     *
     * @param <V> the type of the value returned by each subproblem
     * @param subproblems the subproblems to complete
     * @return the individual return values from each subproblem
     * @throws RuntimeException if one of the subproblems throws an exception
     * @see #evaluate(java.util.Collection)
     */
    public <V> List<V> evaluateUnchecked(Collection<? extends Callable<V>> subproblems) {
        try {
            return evaluate(subproblems);
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Runs a {@code task}. The task may be run in another thread, in which case
     * this method <i>will not wait</i> for the task to complete. The purpose of
     * this method is to reuse a thread that would normally have been used for
     * split-join tasks instead of creating a new {@code Thread} yourself. If
     * there is an idle split-join thread available, then this method generally
     * has lower overhead than creating a new thread.
     *
     * @param task the task to run in another thread
     */
    public abstract void execute(Runnable task);

    /**
     * Provides a hint to this instance that it will no longer be used. Calling
     * this is not required, but may free up resources sooner than not calling
     * it would. The effect of continuing to use this {@code SplitJoin} after
     * disposing of it is undefined.
     */
    public void dispose() {
    }

    /**
     * Returns the ideal number of evenly divided subproblems to break problems
     * into. (Since the {@code SplitJoin} is unaware of the nature of the
     * problem being solved, it cannot guarantee the accuracy of this value.
     * However, if each subproblem performs the same amount of work, the
     * returned value should be close to the ideal value.)
     *
     * @return the number of subproblems that a problem should be broken into to
     * achieve optimal wall clock execution times
     */
    public int getIdealSplitCount() {
        return cpus;
    }

    /**
     * Sets a hint as to whether the creation of higher than normal priority
     * threads is allowed. If {@code true}, then {@code SplitJoin} instances
     * <i>may try</i> to creates threads with above average priority. Note that
     * even if enabled, this is not guaranteed to have an effect.
     *
     * @param enable {@code true} to allow higher than normal priority threads
     */
    public static void setHighPriorityThreadHint(boolean enable) {
        synchronized (SplitJoin.class) {
            if (allowHighPriThreads != enable) {
                allowHighPriThreads = enable;
                instance.remove();
            }
        }
    }

    /**
     * Returns the value of a hint that indicates whether SplitJoins are allowed
     * to create higher than normal priority threads.
     *
     * @return the current value of the hint (default is {@code true})
     */
    public static boolean getHighPriorityThreadHint() {
        synchronized (SplitJoin.class) {
            return allowHighPriThreads;
        }
    }

    /**
     * Sets a hint as to whether to allow parallel execution of subproblems.
     * This may be useful during debugging or to work around any hardware- or
     * platform-specific issues that appear (such as CPU overheating). When set
     * to {@code false}, future calls to {@link #getInstance()} or
     * {@link #createInstance()}
     * <i>that create a new instance rather than share an existing one</i>
     * will return a {@code SplitJoin} implementation that executes subproblems
     * serially in the calling thread. To guarantee that all {@code SplitJoin}
     * instances run serially, this must value must be set before the first call
     * to {@link #getInstance()}.
     *
     * @param enable allow parallel problem solving if {@code true}, disable if
     * {@code false} (default is {@code true})
     */
    public static void setParallelExecutionEnabled(boolean enable) {
        allowThreads = enable;
    }

    private static volatile boolean allowThreads = true;
    private static boolean allowHighPriThreads = true;
}
