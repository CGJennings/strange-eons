package ca.cgjennings.algo;

/**
 * A generic exception that can be thrown to indicate that the user has
 * cancelled an ongoing operation. In general, this should not be thought of as
 * an exceptional circumstance. However, it is sometimes the case that handling
 * this case as an exception results in significantly simpler and easier-to-read
 * code. Indicating this state using an exception can also be useful when the
 * code must pass through a third-party API (which provides no other means to
 * flag the state or perform appropriate cleanup).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class CancelledOperationException extends RuntimeException {

    /**
     * Constructs a new exception with <code>null</code> as its detail message.
     */
    public CancelledOperationException() {
    }

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message
     */
    public CancelledOperationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause an exception that caused this exception
     */
    public CancelledOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with a <code>null</code> message and the
     * specified cause.
     *
     * @param cause an exception that caused this exception
     */
    public CancelledOperationException(Throwable cause) {
        super(cause);
    }
}
