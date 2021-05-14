package ca.cgjennings.algo;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * The primary multi-threaded implementation of {@link SplitJoin}. This is
 * typically used on systems with multiple CPUs.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class ThreadPoolSplitJoin extends SplitJoin {
    public ThreadPoolSplitJoin() {
        this(-1);
    }  

    public ThreadPoolSplitJoin(int splitCount) {
        final int cpus = splitCount < 1 ? getIdealSplitCount() : splitCount;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
        threadPool = new ThreadPoolExecutor(cpus, cpus, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, workQueue, THREAD_FACTORY);
        threadPool.allowCoreThreadTimeOut(true);
        threadPool.prestartAllCoreThreads();
    }

    @Override
    public void execute(Runnable task) {
        threadPool.execute(task);
    }

    @Override
    public void run(Runnable[] subproblems) throws ExecutionException {
        if (subproblems == null) {
            throw new NullPointerException("subproblems");
        }

        Future[] futures = new Future[subproblems.length];
        for (int i = 0; i < subproblems.length; ++i) {
            futures[i] = threadPool.submit(subproblems[i]);
        }
        for (int i = 0; i < subproblems.length; ++i) {
            try {
                futures[i].get();
            } catch (InterruptedException ie) {
                --i;
            }
        }
    }

    @Override
    public void run(Collection<? extends Runnable> subproblems) throws ExecutionException {
        if (subproblems == null) {
            throw new NullPointerException("subproblems");
        }

        final int size = subproblems.size();
        Future[] futures = new Future[size];
        int i = 0;
        for (Runnable r : subproblems) {
            futures[i++] = threadPool.submit(r);
        }
        for (i = 0; i < size; ++i) {
            try {
                futures[i].get();
            } catch (InterruptedException ie) {
                --i;
            }
        }
    }

    @Override
    public <V> List<V> evaluate(Collection<? extends Callable<V>> subproblems) throws ExecutionException {
        if (subproblems == null) {
            throw new NullPointerException("subproblems");
        }

        final int size = subproblems.size();
        ArrayList<V> results = new ArrayList<>(size);
        ArrayList<Future<V>> futures = new ArrayList<>(size);
        for (Callable<V> c : subproblems) {
            futures.add(threadPool.submit(c));
        }
        for (int i = 0; i < size; ++i) {
            try {
                results.add(futures.get(i).get());
            } catch (InterruptedException e) {
                --i;
            }
        }
        return results;
    }

    @Override
    public void dispose() {
        if(this != SplitJoin.getInstance()) {
            threadPool.shutdown();
        }
    }

    private final ThreadPoolExecutor threadPool;

    private static final long KEEP_ALIVE_TIME = 1_000L * 60L * 15L;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "SplitJoin worker thread #" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            if (getHighPriorityThreadHint()) {
                t.setPriority(Thread.NORM_PRIORITY + (Thread.MAX_PRIORITY - Thread.NORM_PRIORITY) / 2);
            }
            t.setUncaughtExceptionHandler(UNCAUGHT_HANDLER);
            return t;
        }
    };

    private static AtomicInteger threadCounter = new AtomicInteger(0);

    private static final Thread.UncaughtExceptionHandler UNCAUGHT_HANDLER = (Thread t, Throwable e) -> {
        StrangeEons.log.log(Level.SEVERE, "Uncaught exception in worker thread", e);
    };
}
