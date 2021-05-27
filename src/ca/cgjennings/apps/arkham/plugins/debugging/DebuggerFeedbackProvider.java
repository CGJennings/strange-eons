package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.dialog.prefs.Preferences;
import ca.cgjennings.ui.JTip;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * This class is responsible for adding and removing a tag over the main
 * application window that provides feedback to the user about the state of the
 * default debug server (in particular, that it is running and the port where it
 * can be found).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class DebuggerFeedbackProvider {

    private DefaultScriptDebugger dg;
    private Component glue;
    private JTip label;

    public DebuggerFeedbackProvider(DefaultScriptDebugger dg) {
        if (dg == null) {
            throw new NullPointerException();
        }
        this.dg = dg;
        threadSafeAction(INIT);
    }

    private void init() {
        glue = Box.createHorizontalGlue();
        label = new JTip();
        label.setIcon(ResourceKit.getIcon("application/db16.png"));
        JLabel tip = (JLabel) label.getTipComponent();
        tip.setIcon(ResourceKit.getIcon("application/db32.png"));
        tip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)
                )
        ));
        String addressText = string("debugger-ip", dg.getHost() + ':' + dg.getPort());
        tip.setText(
                "<html><b>" + string("debugger-name") + "</b><br>"
                + addressText
        );
        tip.setBackground(UIManager.getColor(Theme.MESSAGE_BACKGROUND));
        tip.setForeground(UIManager.getColor(Theme.MESSAGE_FOREGROUND));

        final JMenuItem launchItem = new JMenuItem(string("debugger-launch"));
        final JMenuItem uninstallItem = new JMenuItem(string("debugger-uninstall"));
        String prefs = string("app-settings");
        int pipe = prefs.indexOf('|');
        if (pipe >= 0) {
            prefs = prefs.substring(0, pipe).trim();
        }
        final JMenuItem prefsItem = new JMenuItem(prefs);

        final ActionListener al = (ActionEvent e) -> {
            Object src = e.getSource();
            if (src == launchItem) {
                try {
                    ScriptDebugging.getInstaller().startClient();
                } catch (IOException ex) {
                    StrangeEons.log.log(Level.WARNING, "launch failed", ex);
                }
            } else if (src == uninstallItem) {
                EventQueue.invokeLater(ScriptDebugging::uninstall);
            } else if (src == prefsItem) {
                Preferences d = new Preferences();
                d.setSelectedCategory(string("sd-l-plugins-cat"));
                d.scrollToCategorySection(string("sd-l-debugger"), string("sd-b-debug-client"));
                d.setVisible(true);
            } else {
                StrangeEons.log.warning((String) null);
            }
        };
        launchItem.addActionListener(al);
        uninstallItem.addActionListener(al);
        prefsItem.addActionListener(al);
        JPopupMenu popup = new JPopupMenu();
        popup.add(launchItem);
        popup.addSeparator();
        popup.add(uninstallItem);
        popup.add(prefsItem);

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                launchItem.setEnabled(!DefaultScriptDebugger.isClientConnected());
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    al.actionPerformed(new ActionEvent(launchItem, ActionEvent.ACTION_PERFORMED, null));
                }
            }
        });
        label.setComponentPopupMenu(popup);

        StrangeEons.log.log(Level.INFO, "debug server: {0}", addressText);
    }

    private void installImpl() {
        StrangeEonsAppWindow af = StrangeEons.getWindow();
        if (af == null) {
            return;
        }
        JMenuBar mb = af.getJMenuBar();
        mb.add(glue);
        mb.add(label);
        mb.revalidate();
    }

    private void uninstallImpl() {
        StrangeEonsAppWindow af = StrangeEons.getWindow();
        if (af == null) {
            return;
        }
        JMenuBar mb = af.getJMenuBar();
        mb.remove(glue);
        mb.remove(label);
        mb.revalidate();
    }

    private void threadSafeAction(final int action) {
        if (EventQueue.isDispatchThread()) {
            switch (action) {
                case INIT:
                    init();
                    break;
                case INSTALL:
                    installImpl();
                    break;
                case UNINSTALL:
                    uninstallImpl();
                    break;
                default:
                    throw new AssertionError();
            }
        } else {
            EventQueue.invokeLater(() -> {
                threadSafeAction(action);
            });
        }
    }

    private static final int INIT = 0;
    private static final int INSTALL = 1;
    private static final int UNINSTALL = 2;

    public void install() {
        threadSafeAction(INSTALL);
    }

    public void uninstall() {
        threadSafeAction(UNINSTALL);
    }
}
