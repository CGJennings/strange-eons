package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.MarkupTarget;
import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;

/**
 * Base class for creating markup commands.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class HMarkupCommand extends DelegatedCommand {

    private String prefix, suffix;

    public HMarkupCommand(String nameKey) {
        super(nameKey);
    }

    public HMarkupCommand(String nameKey, String iconResource) {
        super(nameKey, iconResource);
    }

    public HMarkupCommand(String nameKey, String iconResource, String prefix, String suffix) {
        super(nameKey, iconResource);
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public boolean isDefaultActionApplicable() {
        return StrangeEons.getApplication().getMarkupTarget() != null;
    }

    @Override
    public void performDefaultAction(ActionEvent e) {
        MarkupTarget mt = StrangeEons.getApplication().getMarkupTarget();
        if (mt != null) {
            insertMarkup(e, mt);
        }
    }

    protected void insertMarkup(ActionEvent e, MarkupTarget mt) {
        if (suffix == null) {
            if (prefix == null) {
                throw new AssertionError();
            }
            StrangeEons.getApplication().insertMarkup(prefix);
        } else {
            StrangeEons.getApplication().insertMarkupTags(prefix, suffix);
        }
    }

    protected void setDialogLocation(Window w, ActionEvent e, MarkupTarget mt) {
        Component c;
        if (e.getSource() instanceof Component) {
            c = (Component) e.getSource();
        } else {
            c = (Component) mt.getTarget();
        }
        w.setLocationRelativeTo(c);
    }
}
