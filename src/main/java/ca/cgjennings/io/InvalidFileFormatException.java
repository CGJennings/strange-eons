package ca.cgjennings.io;

import java.io.IOException;

/**
 * An I/O exception that is thrown by classes that process files in a specific
 * format when that format specification is violated. The message may contain
 * more information about the specific cause. The thrower may optionally mark
 * the location of the problem using a {@code long} value. If present, this will
 * become part of the message by appending a space and the text "[offset:
 * <i>n</i>]". (If the message is {@code null}, then this text will be used as
 * the message.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class InvalidFileFormatException extends IOException {

    public InvalidFileFormatException(Throwable cause, long offset) {
        super(composeMessage(null, offset), cause);
    }

    public InvalidFileFormatException(String message, Throwable cause, long offset) {
        super(composeMessage(message, offset), cause);
    }

    public InvalidFileFormatException(String message, long offset) {
        super(composeMessage(message, offset));
    }

    public InvalidFileFormatException(long offset) {
        super(composeMessage(null, offset));
    }

    public InvalidFileFormatException(Throwable cause) {
        super(cause);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFileFormatException(String message) {
        super(message);
    }

    public InvalidFileFormatException() {
    }

    private static String composeMessage(String message, long value) {
        StringBuilder b = new StringBuilder();
        if (message != null) {
            b.append(message).append(' ');
        }
        b.append("[offset: ").append(value).append(']');
        return b.toString();
    }
}
