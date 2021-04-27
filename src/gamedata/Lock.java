package gamedata;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Maintains a lock on the game data. When game data is locked, trying to add or
 * modify game data will fail by throwing an <code>IllegalStateException</code>.
 * Game data is normally locked after extensions are loaded to ensure that the
 * application remains in a consistent state. Developers may find it useful to
 * unlock the game data while debugging so that fewer application restarts are
 * required.
 *
 * <p>
 * Some kinds of game data may not require locking to remain consistent, in
 * which case they can be modified freely. Future versions may lessen the
 * locking constraints on a kind of game data, but will not increase them.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Lock {

    private Lock() {
    }
    private static boolean locked;
    private static boolean everLocked;
    private static LinkedList<Runnable> lockTasks = new LinkedList<>();

    /**
     * Returns <code>true</code> if the game data has been locked.
     *
     * @return <code>true</code> if lock tests will fail
     */
    public static synchronized boolean isLocked() {
        return locked;
    }

    /**
     * Locks or unlocks the game data.
     *
     * @param lock if <code>true</code>, lock game data so that future tests
     * fail
     */
    public static synchronized void setLocked(boolean lock) {
        // instead of only doing the lock tasks  if the DB was never locked,
        // we run those tasks that have been ADDED since the last lock;
        // this allows debugging to work as expected
        for (Runnable r : lockTasks) {
            try {
                r.run();
            } catch (Throwable t) {
                StrangeEons.log.log(Level.SEVERE, "locking task threw an exception", t);
            }
        }
        lockTasks.clear();

        locked = lock;
        if (lock) {
            everLocked = true;
        }
    }

    /**
     * Returns <code>true</code> if the game data has ever been locked during
     * this run of the application.
     *
     * @return <code>true</code> if {@link #isLocked()} has ever been true
     */
    public static synchronized boolean hasBeenLocked() {
        return everLocked;
    }

    /**
     * Test the lock, and if locked throw an IllegalStateException using a
     * default message.
     */
    public static void test() {
        test(null, null);
    }

    /**
     * Test the lock, and if locked throw an IllegalStateException using a
     * custom message.
     *
     * @param message the base message, may be <code>null</code> for a default
     * message
     */
    public static void test(String message) {
        test(message, null);
    }

    /**
     * Test the lock, and if locked throw an IllegalStateException by composing
     * a message from the message and suffix. (This allows you to add a
     * parameter to the message without having to create a concatenated string
     * in the normal case where the test passes.)
     *
     * @param message the base message, may be <code>null</code> for a default
     * message
     * @param suffix an optional suffix to be appended as if by message + ": " +
     * suffix
     */
    public static synchronized void test(String message, String suffix) {
        if (locked) {
            fail(message, suffix);
        }
    }

    /**
     * Throws an exception as if a test had been failed.
     *
     * @param message the base message, may be <code>null</code> for a default
     * message
     * @param suffix an optional suffix to be appended as if by message + ": " +
     * suffix
     */
    public static void fail(String message, String suffix) {
        if (message == null) {
            message = "The game database cannot be modified when running in standard (locked) mode.";
        }
        if (suffix == null) {
            throw new IllegalStateException(message);
        }
        throw new IllegalStateException(message + ": " + suffix);
    }

    /**
     * Adds a new runnable task that will be executed just after the database is
     * locked. Game plug-ins may use this to normalize their own internal game
     * databases after all extension plug-ins have had a chance to run.
     *
     * @param task the task to be executed after the database is locked
     */
    public synchronized static void addLockingTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("null locking task");
        }
        if (!lockTasks.contains(task)) {
            lockTasks.add(task);
        }
    }
}
