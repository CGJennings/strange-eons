/*
 * CorruptedInputException
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */
package ca.cgjennings.algo.compression;

import java.io.IOException;

/**
 * Thrown when the compressed input data is corrupt. However, it is possible
 * that some or all of the data already read from the input stream was corrupt
 * too.
 */
class CorruptedInputException extends IOException {

    private static final long serialVersionUID = 3L;

    /**
     * Creates a new CorruptedInputException with the default error detail
     * message.
     */
    public CorruptedInputException() {
        super("Compressed data is corrupt");
    }

    /**
     * Creates a new CorruptedInputException with the specified error detail
     * message.
     *
     * @param s error detail message
     */
    public CorruptedInputException(String s) {
        super(s);
    }
}
