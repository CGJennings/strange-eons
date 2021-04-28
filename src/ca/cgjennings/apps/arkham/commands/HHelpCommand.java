package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.ui.JHelpButton;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.FocusManager;
import javax.swing.JComponent;
import static resources.Language.string;

/**
 * The command that handles contextual help requests.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class HHelpCommand extends DelegatedCommand {

    public HHelpCommand() {
        super("app-help-item", "application/help-hi.png");
    }

    @Override
    public boolean isDefaultActionApplicable() {
        return true;
    }

    @Override
    public void performDefaultAction(ActionEvent e) {
        String context = getCommandString(e);

        if (context == null) {
            Component c = FocusManager.getCurrentManager().getFocusOwner();
            if (c != null) {
                context = searchForContext(c);
            } else {
                Window w = FocusManager.getCurrentManager().getActiveWindow();
                if (w != null) {
                    context = contextSearchImpl(w, null, false);
                }
            }
        }

        if (context == null) {
            context = DEFAULT_CONTEXT;
        }

        showPage(getName(), context);
    }

    private static String readContext(JComponent c) {
        if (c instanceof JHelpButton) {
            return ((JHelpButton) c).getHelpPage();
        }
        Object v = c.getClientProperty(Commands.HELP_CONTEXT_PROPERTY);
        return v == null ? null : v.toString();
    }

    private static String searchForContext(Component c) {
        String page;
        page = contextSearchImpl(c, null, false);
        while (page == null && c != null) {
            Component kidToIgnore = c;
            c = c.getParent();
            page = contextSearchImpl(c, kidToIgnore, true);
        }
        return page;
    }

    private static String contextSearchImpl(Component c, Component kidToIgnore, boolean ignoreRoot) {
        if (!ignoreRoot) {
            if (c instanceof JComponent) {
                String context = readContext((JComponent) c);
                if (context != null) {
                    return context;
                }
            }
        }
        if (c instanceof Container) {
            Container p = (Container) c;
            for (int i = 0; i < p.getComponentCount(); ++i) {
                Component k = p.getComponent(i);
                if (k == kidToIgnore) {
                    continue;
                }
                String context = contextSearchImpl(k, null, false);
                if (context != null) {
                    return context;
                }
            }
        }
        return null;
    }

    static void showPage(String tabTitle, String pageName) {
        String page = StrangeEons.getUrlForDocPage(pageName);
        try {
            if (DesktopIntegration.BROWSE_SUPPORTED) {
                DesktopIntegration.browse(new URI(page), StrangeEons.getWindow());
                return;
            }
        } catch (URISyntaxException | IOException e) {
            ErrorDialog.displayError(string("rk-err-help"), e);
            return;
        }
        ErrorDialog.displayError(string("rk-err-help"), null);
    }

    /**
     * If the help command can't find a more specific context, it will open this
     * instead.
     */
    private static final String DEFAULT_CONTEXT = "um-index";
}
