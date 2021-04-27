package ca.cgjennings.apps.arkham.commands;

import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.catalog.AutomaticUpdater;
import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.platform.PlatformSupport;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import static resources.Language.string;

/**
 * Command to file a bug report.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
final class HFileBugReport extends AbstractCommand {

    public HFileBugReport() {
        super("app-report-bug", "ui/bug-report.png");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Component source
                = (e == null || e.getSource() == null || !(e.getSource() instanceof Component))
                ? StrangeEons.getWindow()
                : (Component) e.getSource();

        new BusyDialog(string("rk-err-check"), () -> {
            final boolean appUpdate = AutomaticUpdater.isApplicationUpdateAvailable();
            EventQueue.invokeLater(() -> {
                if (appUpdate) {
                    Object[] options = new Object[]{
                        string("rk-err-update-ok"), string("rk-err-update-cancel")
                    };
                    int defIndex = 0;
                    if (PlatformSupport.PLATFORM_IS_OSX) {
                        Object o = options[0];
                        options[0] = options[1];
                        options[1] = o;
                        defIndex = 1;
                    }
                    int choice = JOptionPane.showOptionDialog(
                            source,
                            string("rk-err-update-query"), "Strange Eons",
                            0, JOptionPane.QUESTION_MESSAGE, null,
                            options, options[defIndex]
                    );
                    if (choice == JOptionPane.CLOSED_OPTION) {
                        return;
                    }
                    if (choice == defIndex) {
                        try {
                            try {
                                URI uri = new URI("http://cgjennings.ca/eons/download/update.html");
                                DesktopIntegration.browse(uri, StrangeEons.getWindow());
                            } catch (Throwable t) {
                            }
                        } catch (Exception e1) {
                            StrangeEons.log.log(Level.WARNING, null, e1);
                        }
                        return;
                    }
                }
                StrangeEons.getApplication().fileBugReport(null, null);
            });
        });
	}
}
