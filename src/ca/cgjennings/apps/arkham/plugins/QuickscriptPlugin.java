package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.textedit.CodeEditorBase;
import ca.cgjennings.ui.theme.ThemedIcon;
import ca.cgjennings.ui.theme.ThemedImageIcon;
import java.awt.Window;
import java.awt.image.BufferedImage;
import static resources.Language.string;

/**
 * A plug-in that allows editing and running small scripts from within Strange
 * Eons without the need to create a project or script file. This can be used to
 * test small snippets of code, hack game components, or to experiment with the
 * language or APIs.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public final class QuickscriptPlugin implements Plugin {

    private QuickscriptDialog dialog;

    /**
     * Creates a new instance of the plug-in. This is called by the application
     * when the plug-in is being started.
     */
    public QuickscriptPlugin() {
    }

    /**
     * Returns the window that is used to display the script editor when the
     * plug-in is activated.
     *
     * @return the window displayed by the plug-in
     */
    public Window getWindow() {
        initDialog();
        return dialog;
    }

    /**
     * Returns the editor component used to edit script files. This can be used
     * to add custom commands or modify the edited script.
     *
     * @return the source code editor
     * @since 3.0
     */
    public CodeEditorBase getEditor() {
        initDialog();
        return dialog.getEditor();
    }

    /**
     * Run the current script.
     *
     * @param debugIfAvailable if the debugger is enabled, debug the script by
     * activating a breakpoint when the script starts running
     * @since 2.00.9
     */
    public void run(boolean debugIfAvailable) {
        initDialog();
        dialog.run(debugIfAvailable);
    }

    @Override
    public boolean initializePlugin(PluginContext context) {
        return true;
    }

    @Override
    public void unloadPlugin() {
        if (dialog != null) {
            dialog.dispose();
        }
    }

    @Override
    public String getPluginName() {
        return string("qs-name");
    }

    @Override
    public String getPluginDescription() {
        return string("qs-desc");
    }

    @Override
    public float getPluginVersion() {
        return 3.0f;
    }

    private void initDialog() {
        if (dialog == null) {
            dialog = new QuickscriptDialog();
        }
    }

    @Override
    public void showPlugin(PluginContext context, boolean show) {
        if (show) {
            initDialog();
            dialog.setVisible(true);
        } else if (dialog != null) {
            dialog.setVisible(false);
        }
    }

    @Override
    public boolean isPluginShowing() {
        return dialog != null && dialog.isShowing();
    }

    @Override
    public int getPluginType() {
        return ACTIVATED;
    }

    @Override
    public BufferedImage getRepresentativeImage() {
        return ImageUtilities.iconToImage(getPluginIcon());
    }
    
    @Override
    public ThemedIcon getPluginIcon() {
       if (pluginIcon == null) {
           return new ThemedImageIcon("/ca/cgjennings/apps/arkham/plugins/quickscript.png");
       }
       return pluginIcon;
    }
    private ThemedIcon pluginIcon;

    @Override
    public boolean isPluginUsable() {
        return true;
    }

    @Override
    public String getDefaultAcceleratorKey() {
        return "ctrl Q";
    }
}
