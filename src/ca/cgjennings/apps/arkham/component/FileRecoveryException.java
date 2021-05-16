package ca.cgjennings.apps.arkham.component;

import java.io.File;
import java.io.IOException;
import resources.ResourceKit;

/**
 * This is a special exception that may be thrown while a component is being
 * read from a stream. It indicates that the file is corrupt or damaged in some
 * way that it cannot be completely recovered, but that as much of the file as
 * could be salvaged has been written to a new component that has been saved as
 * a temporary file. This exception is caught by
 * {@link ResourceKit#getGameComponentFromStream} and it will automatically
 * substitute the component from the supplied temporary file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class FileRecoveryException extends IOException {

    /**
     * Creates a new exception that allows recovery from the supplied temporary
     * file.
     *
     * @param tempFileForRecovery the file to attempt recovery from
     * @param recoveryTitle the title of a message that may be displayed to the
     * user after recovery; if this is {@code null}, a default title will
     * be used
     * @param successMessage the body of a message that may be displayed to the
     * user after recovery; if this is {@code null}, no message will be
     * displayed
     * @param failMessage the body of a message that may be displayed to the
     * user if recovery fails; if this is {@code null}, no message will be
     * displayed
     */
    public FileRecoveryException(File tempFileForRecovery, String recoveryTitle, String successMessage, String failMessage) {
        tempFile = tempFileForRecovery;
        title = recoveryTitle;
        body = successMessage;
        fail = failMessage;
    }

    /**
     * Returns the file to use to replace the component that was being read when
     * the exception was thrown.
     *
     * @return a temporary file containing a serialized game component
     */
    public File getTempFile() {
        return tempFile;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public String getSuccessMessage() {
        return body;
    }

    public String getFailureMessage() {
        return fail;
    }

    private final File tempFile;
    private final String title;
    private String body, fail;
}

/*
 * NOTES:
 * This is thrown from deep within Investigator
 * serialization as a workaround due to a certain bug in 1.70 update 4
 * that caused corrupt files to be written. When the problem is detected
 * on file open, the subsystem will create a temporary file, dump as much
 * of the character as available into it, and then throw this exception
 * with the name of the temp file. The file is set to delete on exit, so
 * if this exception is not handled as expected the temporary file should
 * be deleted in any case.
 *
 * The file opening subsystem will recognize this exception and try to
 * open the dumped file instead, displaying a warning that the file
 * was only partially recovered.
 *
 * This is a hack, but due to the nature of the bug an exception will always
 * be thrown from within the serialization so this is the best we can do.
 */
