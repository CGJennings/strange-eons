package ca.cgjennings.apps.arkham.dialog.prefs;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.plugins.catalog.ConfigureUpdatesDialog;
import ca.cgjennings.apps.arkham.plugins.catalog.NetworkProxy;
import ca.cgjennings.apps.arkham.plugins.debugging.ScriptDebugging;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import javax.swing.JComponent;
import static resources.Language.string;
import resources.Settings;

/**
 * Plug-in support preferences category.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
class CatPlugins extends FillInPreferenceCategory {

    public CatPlugins() {
        super(string("sd-l-plugins-cat"), "icons/application/prefs-plugins.png");

        Font fieldFont = new Font(Font.MONOSPACED, Font.PLAIN, 11);

        heading(string("sd-l-catalog"));
        label(string("sd-l-cat-options"));
        indent();
        addCheckBox("catalog-autoselect-updated", string("sd-b-select-updates"), false);
        addCheckBox("catalog-autoselect-core", string("sd-b-select-cores"), false);
        join();
        tip(string("sd-tip-select-cores"));
        addButton(string("sd-b-updates"), (ActionEvent e) -> {
            ConfigureUpdatesDialog d = new ConfigureUpdatesDialog();
            d.setLocationRelativeTo((JComponent) e.getSource());
            d.setVisible(true);
        });
        join();
        addButton(string("sd-b-proxy-config"), (ActionEvent e) -> {
            NetworkProxy.showProxySettingsDialog(((JComponent) e.getSource()));
        });

        heading(string("sd-l-js-engine"));

        subheading(string("sd-l-compiler-opts"));
        label(string("sd-l-js-optimiztion"));
        indent();
        note(string("sd-l-js-opt-note"));
        SBSliderKit slider = (SBSliderKit) add("script-optimization-level",
                new SBSliderKit(0, 3, 1, 1, string("sd-l-js-interpret-only"), string("sd-l-js-maximum-opt"), false) {
            @Override
            public void fromSetting(String v) {
                int level = -1;
                try {
                    level = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                }
                // first normalize to a Rhino optimizer setting
                if (level < -1) {
                    level = -1;
                }
                if (level > 9) {
                    level = 9;
                }

                // now map the setting to the slider: anything above 1
                // (optimization enabled) is mapped to 2 (max optimization),
                // which is the final slider position
                if (level > 2) {
                    level = 2;
                }
                level += 1; // map -1,0,1,2 to 0,1,2,3
                slider.setValue(level);
            }

            @Override
            public String toSetting() {
                int val = slider.getValue() - 1;
                if (val == 2) {
                    val = 9;
                }
                return "" + val;
            }
        });
        slider.getSlider().setPaintLabels(true);
        indent();
        addUnmanagedControl(slider);

        unindent();
        addCheckBox("script-warnings", string("sd-l-js-warnings"), false);
        indent();
        addCheckBox("script-ignore-missing-semicolons", string("sd-l-js-semicolons"), false);
        unindent();
        addCheckBox("script-full-exception-trace", string("sd-l-js-stack-trace"), false);
        addCheckBox("script-compatibility-mode", string("sd-l-js-lib-compat"), false);

        subheading(string("sd-l-debugger"));
        addCheckBox("enable-script-debugger", string("sd-l-js-debugger"), false);
        indent();
        note(string("sd-l-js-debugger-note"));
        addCheckBox("enable-remote-debugging", string("sd-l-debug-remote"), false);
        addRange("script-debug-port", string("sd-l-debug-port"), 1_024, 65_535, 1);

        // !!! See DEBUGGER_KEYS when adding new server keys
        subheading(string("sd-l-debug-client"));
        addCheckBox("script-debug-client-autostart", string("sd-b-debug-client-autostart"), false);
        indent();
        addButton(string("sd-b-debug-client"), (ActionEvent e) -> {
            try {
                ScriptDebugging.getInstaller().startClient();
            } catch (Throwable ioe) {
                ErrorDialog.displayError(string("killps"), ioe);
            }
        });
    }

    private final String[] DEBUGGER_KEYS = new String[]{
        "enable-script-debugger", "enable-remote-debugging", "script-debug-port"
    };
    private final String[] DEBUGGER_VALUES = new String[DEBUGGER_KEYS.length];

    @Override
    public void loadSettings() {
        super.loadSettings();
        Settings s = Settings.getUser();
        for (int i = 0; i < DEBUGGER_KEYS.length; ++i) {
            DEBUGGER_VALUES[i] = s.get(DEBUGGER_KEYS[i]);
        }
    }

    @Override
    public void storeSettings() {
        super.storeSettings();

        boolean restartDebugServer = false;
        Settings s = Settings.getUser();
        for (int i = 0; i < DEBUGGER_KEYS.length && !restartDebugServer; ++i) {
            String newValue = s.get(DEBUGGER_KEYS[i]);
            if (newValue == null ? DEBUGGER_VALUES[i] != null : !newValue.equals(DEBUGGER_VALUES[i])) {
                restartDebugServer = true;
            }
        }
        if (restartDebugServer) {
            EventQueue.invokeLater(new ServerRestarter(false));
            if (s.getYesNo("enable-script-debugger")) {
                EventQueue.invokeLater(new ServerRestarter(true));
            }
        }
    }

    static class ServerRestarter implements Runnable {

        private final boolean install;

        public ServerRestarter(boolean install) {
            this.install = install;
        }

        @Override
        public void run() {
            StrangeEons.setWaitCursor(true);
            try {
                if (install) {
                    ScriptDebugging.install();
                } else {
                    ScriptDebugging.uninstall();
                }
            } catch (Throwable e) {
                StrangeEons.log.log(Level.SEVERE, "exception while restarting debug server", e);
            } finally {
                StrangeEons.setWaitCursor(false);
            }
        }
    }
}
