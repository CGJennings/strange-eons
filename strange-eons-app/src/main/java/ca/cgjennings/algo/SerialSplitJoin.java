package ca.cgjennings.algo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * A serial implementation of {@link SplitJoin}. This will typically be used on
 * systems with only one CPU.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class SerialSplitJoin extends SplitJoin {

    public SerialSplitJoin() {
    }

    @Override
    public void execute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            Thread th = Thread.currentThread();
            Thread.UncaughtExceptionHandler h = th.getUncaughtExceptionHandler();
            if (h == null) {
                h = Thread.getDefaultUncaughtExceptionHandler();
            }
            if (h != null) {
                h.uncaughtException(th, t);
            }
        }
    }

    @Override
    public int getIdealSplitCount() {
        return 1;
    }

    @Override
    public void run(Runnable[] subproblems) throws ExecutionException {
        if (subproblems == null) {
            throw new NullPointerException("subproblems");
        }

        try {
            for (int i = 0; i < subproblems.length; ++i) {
                subproblems[i].run();
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void run(Collection<? extends Runnable> subproblems) throws ExecutionException {
        if (subproblems == null) {
            throw new NullPointerException("subproblems");
        }

        try {
            for (Runnable r : subproblems) {
                r.run();
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public <V> List<V> evaluate(Collection<? extends Callable<V>> subproblems) throws ExecutionException {
        if (subproblems == null) {
            throw new NullPointerException("subproblems");
        }

        final int size = subproblems.size();
        ArrayList<V> results = new ArrayList<>(size);
        try {
            for (Callable<V> c : subproblems) {
                results.add(c.call());
            }
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
        return results;
    }
}
