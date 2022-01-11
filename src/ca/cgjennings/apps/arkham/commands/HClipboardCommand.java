package ca.cgjennings.apps.arkham.commands;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

/**
 * A package private helper class for commands that operate on the clipboard.
 * This delegated command has a default command that checks the focused
 * component for an action associated with its own accelerator, and if found
 * performs it.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class HClipboardCommand extends DelegatedCommand {

    HClipboardCommand(String nameKey) {
        super(nameKey);
    }

    HClipboardCommand(String nameKey, String iconResource) {
        super(nameKey, iconResource);
    }

    @Override
    public void performDefaultAction(ActionEvent e) {
        final ActionListener clipAction = getClipAction();
        if (clipAction != null) {
            clipAction.actionPerformed(new ActionEvent(currentClippableComponent, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers()));
            // so other windows like Quickscript get focus back if selected in app window menu
            currentClippableComponent.requestFocusInWindow();
        }
    }

    @Override
    public boolean isDefaultActionApplicable() {
        return getClipAction() != null;
    }

    /**
     * If the current "clippable component" has this command's accelerator set
     * as a key stroke, return that action. As a special case, when the
     * accelerator uses Command, it will also try looking up the Ctrl key.
     *
     * @return the action mapped to by this command's accelerator, or
     * {@code null}
     */
    private ActionListener getClipAction() {
        ActionListener clipAction = null;
        if (currentClippableComponent != null) {
            KeyStroke accel = getAccelerator();
            clipAction = currentClippableComponent.getActionForKeyStroke(accel);

            // For OS X: if Command+X is not defined, also try Ctrl+X
            if (clipAction == null) {
                int modifiers = accel.getModifiers();
                if ((modifiers & InputEvent.META_DOWN_MASK) == InputEvent.META_DOWN_MASK) {
                    accel = KeyStroke.getKeyStroke(accel.getKeyCode(), InputEvent.CTRL_DOWN_MASK);
                    clipAction = currentClippableComponent.getActionForKeyStroke(accel);
                }
            }
        }
        return clipAction;
    }

    private static JComponent currentClippableComponent;

    static {
        FocusManager.getCurrentManager().addPropertyChangeListener("focusOwner", (evt) -> {
                Object newObj = evt.getNewValue();
                // menu items don't affect focus so that user can open menu
                // with pointer without disabling the clipboard items
                if (newObj != null && !(newObj instanceof JMenuItem) && !(newObj instanceof JRootPane)) {
                    if (newObj instanceof JComponent) {
                        currentClippableComponent = (JComponent) newObj;
                    } else {
                        currentClippableComponent = null;
                    }
                }
        });
    }
}
