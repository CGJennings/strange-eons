/*
 * FinishableWrapperOutputStream
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */
package ca.cgjennings.algo.compression;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps an output stream to a finishable output stream for use with raw
 * encoders. This is not needed for XZ compression and thus most people will
 * never need this.
 */
class FinishableWrapperOutputStream extends FinishableOutputStream {

    /**
     * The {@link java.io.OutputStream OutputStream} that has been wrapped into
     * a FinishableWrapperOutputStream.
     */
    protected OutputStream out;

    /**
     * Creates a new output stream which support finishing. The
     * {@code finish()} method will do nothing.
     */
    public FinishableWrapperOutputStream(OutputStream out) {
        this.out = out;
    }

    /**
     * Calls {@link java.io.OutputStream#write(int) out.write(b)}.
     */
    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    /**
     * Calls {@link java.io.OutputStream#write(byte[]) out.write(buf)}.
     */
    @Override
    public void write(byte[] buf) throws IOException {
        out.write(buf);
    }

    /**
     * Calls {@link java.io.OutputStream#write(byte[],int,int)
     * out.write(buf, off, len)}.
     */
    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        out.write(buf, off, len);
    }

    /**
     * Calls {@link java.io.OutputStream#flush() out.flush()}.
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Calls {@link java.io.OutputStream#close() out.close()}.
     */
    @Override
    public void close() throws IOException {
        out.close();
    }
}
