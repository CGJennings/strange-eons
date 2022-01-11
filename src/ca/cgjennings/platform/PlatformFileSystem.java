package ca.cgjennings.platform;

import java.io.File;
import java.io.IOException;

/**
 * Basic platform-specific file system operations.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class PlatformFileSystem {

    private PlatformFileSystem() {
    }

    /**
     * Sets the hidden attribute of a file. Has no effect if the file does not
     * exist or the platform is not Windows-based. (On UNIX family platforms,
     * including OS X, a file is considered "hidden" if the file name starts
     * with a period character.)
     *
     * @param file the file to modify
     * @param hidden whether the hidden bit should be set or unset
     * @return {@code true} if no change was needed or the required change was
     * successful
     * @throws NullPointerException if the file is {@code null}
     */
    public static boolean setHidden(File file, boolean hidden) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (PlatformSupport.PLATFORM_IS_WINDOWS && file.exists() && file.isHidden() != hidden) {
            return attrib(file, hidden ? "+h" : "-h");
        }
        return false;
    }

    /**
     * Attempts to modify the permissions on a file so that the current user can
     * read the file, and list contents if the file is a directory.
     *
     * @param file the file to make readable
     * @return {@code true} if change was successful
     * @throws NullPointerException if the file is {@code null}
     */
    public static boolean makeReadableByUser(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (PlatformSupport.PLATFORM_IS_WINDOWS) {
            attrib(file, "-h");
            return cacls(file, "r");
        } else {
            return chmod(file, "u+r");
        }
    }

    /**
     * Attempts to modify the permissions on a file so that the current user can
     * read and write to the file, and list contents if the file is a directory.
     *
     * @param file the file to make writable
     * @return {@code true} if change was successful
     * @throws NullPointerException if the file is {@code null}
     */
    public static boolean makeWritableByUser(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (PlatformSupport.PLATFORM_IS_WINDOWS) {
            attrib(file, "-h");
            attrib(file, "-r");
            return cacls(file, "w");
        } else {
            return chmod(file, "u+rw");
        }
    }

    private static boolean exec(String... args) {
        if (args == null) {
            throw new NullPointerException("args");
        }
        try {
            return waitFor(Runtime.getRuntime().exec(args)) == 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static int waitFor(Process p) {
        for (;;) {
            try {
                return p.waitFor();
            } catch (InterruptedException e) {
            }
        }
    }

    private static boolean chmod(File file, String flags) {
        if (file.isDirectory()) {
            flags += 'x';
        }
        return exec("chmod", "u+" + flags, file.getAbsolutePath());
    }

    private static boolean attrib(File file, String flags) {
        return exec("attrib", flags, file.getAbsolutePath());
    }

    private static boolean cacls(File file, String flags) {
        return exec("cacls", file.getAbsolutePath(), "/e", "/g",
                System.getProperty("user.name") + ':' + flags
        );
    }
}
