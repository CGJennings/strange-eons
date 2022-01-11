package ca.cgjennings.io;

import java.io.IOException;
import resources.Language;

/**
 * An exception that this thrown when you try to read an object that was written
 * with a newer file format than this version of Strange Eons knows how to
 * handle.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class NewerVersionException extends IOException {

    /**
     * Creates a new exception with a default message.
     */
    public NewerVersionException() {
        super(Language.string("rk-err-version-mismatch"));
    }

    /**
     * Creates a new exception with the specified error message.
     *
     * @param message the error message to use
     */
    public NewerVersionException(String message) {
        super(message);
    }

    /**
     * Throws a newer version exception if {@code currentVersion} is less than
     * {@code formatVersion}.
     *
     * @param currentVersion the format version that the caller knows how to
     * read
     * @param formatVersion the format version that is reported by this object
     * @throws NewerVersionException if the caller doesn't know how to read
     * {@code formatVersion}
     */
    public static void check(int currentVersion, int formatVersion) throws NewerVersionException {
        if (currentVersion < formatVersion) {
            throw new NewerVersionException();
        }
    }
}
