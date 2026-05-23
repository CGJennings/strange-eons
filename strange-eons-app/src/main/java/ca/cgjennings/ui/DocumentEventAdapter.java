package ca.cgjennings.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A {@code DocumentListener} that routes all events through
 * {@code changedUpdate}.
 *
 * @author Chris
 */
public abstract class DocumentEventAdapter implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        changedUpdate(e);
    }

    @Override
    public abstract void changedUpdate(DocumentEvent e);
}
