package ca.cgjennings.ui;

import javax.swing.text.Document;

/**
 * This interface should be implemented by {@code Document}s that wrap
 * around other documents to modify their basic functionality. (Such wrapping
 * can be used to simulate the effects of multiple inheritence.) The interface
 * specifies a {@code getDocument} method that allows nesting-aware code to
 * access the document(s) being wrapped.
 *
 * @author Christopher G. Jennings
 */
public interface NestingDocument extends Document {

    /**
     * Return the document that this document is wrapped around.
     *
     * @return the nested document
     */
    public abstract Document getDocument();
}
