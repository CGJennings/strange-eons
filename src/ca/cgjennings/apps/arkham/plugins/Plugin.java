package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;
import java.awt.image.BufferedImage;

/**
 * An interface that allows third-party code (plug-ins) to be integrated into
 * Strange Eons at run time. To be valid, a plug-in class must:
 * <ol>
 * <li> be a valid Java class in the classpath
 * <li> have an accessible no-argument constructor
 * <li> implement the {@code Plugin} interface
 * </ol>
 * <p>
 * Alternatively, the plug-in can be implemented by script code, with a
 * {@link DefaultScriptedPlugin} acting as the plug-in class and forwarding
 * plug-in method calls to the script.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public interface Plugin {

    /**
     * This method will be called once for each registered plug-in before any
     * other methods are called. It allows the plug-in to perform any required
     * initialization prior to the plug-in being shown. If initialization
     * succeeds, it should return {@code true}. Otherwise, the plug-in will
     * not be made available to the user. If this method returns
     * {@code false}, a warning is logged but no other action is taken.
     * This allows the plug-in the opportunity to display its own error message.
     * If this method throws an exception, a dialog is displayed that will
     * include the message text of the exception (if any).
     * <p>
     * Sometimes a plug-in is instantiated only in order to determine its name,
     * description, version, and type. In such cases, the plug-in's
     * {@code initializePlugin} method is still called, so that a localized
     * plug-in can provide a name and description in the appropriate language.
     * Plug-ins that do not wish to perform a complete initialization in such
     * cases can detect when the plug-in is being created for information
     * purposes only by calling the {@link PluginContext#isInformationProbe()}
     * method of the provided {@code context}.
     * <p>
     * <b>Note:</b> This method will never be called more than once for a given
     * <i>instance</i> of a plug-in class. However, multiple instances of the
     * same plug-in class are often created. Therefore, you should not assume
     * that this method will only be called once per session (unless it is an
     * {@code EXTENSION}).
     *
     * @param context a {@link PluginContext} instance that can be accessed
     * during initialization
     * @return {@code true} if the plug-in was initialized;
     * {@code false} if initialization failed
     */
    public boolean initializePlugin(PluginContext context);

    /**
     * This method is called when the plug-in is about to be unloaded to give
     * the plug-in the chance to release any resources it may be holding. It
     * will be called prior to ending the application, or whenever the plug-in
     * will be stopped and restarted.
     * <p>
     * <b>Notes:</b>
     * <ol>
     * <li>It is not guaranteed that this method will be called if the program
     * terminates abnormally.
     * <li>This method will never be called more than once for a given
     * <i>instance</i> of a plug-in. (More than one instance of an
     * {@code ACTIVATED} or {@code INJECTED} plug-in may be created
     * during a session.)
     * <li>For {@code ACTIVATED} plug-ins that are currently showing,
     * {@link #showPlugin} will be called with {@code false} (in order to
     * hide the plug-in) before this method is called.
     * </ol>
     */
    public void unloadPlugin();

    /**
     * Returns the name that should be shown to the user for this plug-in,
     * ideally in the UI locale. This should be a short name that describes the
     * plug-in's purpose in three words or less. If this is an
     * {@code ACTIVATED} plug-in, the returned value should ideally be a
     * verb phrase that describes the effect of activating the plug-in, such as
     * "Select All Lines". It should not include extraneous information, such as
     * the author's name; this kind of information can be included in the
     * description string or, ideally, in the plug-in's catalogue description.
     *
     * @return a string that identifies this plug-in for end users
     */
    public String getPluginName();

    /**
     * Returns a description that can be shown to the user for this plug-in. The
     * returned string will be used to describe the purpose of the plug-in to
     * the user. This should be a single short sentence without any terminating
     * punctuation, such as "Selects all lines on the current deck page".
     *
     * @return a string that describes the purpose of this plug-in for end users
     */
    public String getPluginDescription();

    /**
     * Returns a number representing the version or release number of the
     * plug-in. This is primarily for the user's information; the application
     * only compares versions using the date of the {@link CatalogID}.
     *
     * @return a number that corresponds to the plug-in's internal version
     * number
     */
    public float getPluginVersion();

    /**
     * Returns the type identifier of the plug-in. This must be one of
     * {@code ACTIVATED}, {@code INJECTED}, or {@code EXTENSION}.
     *
     * @return a plug-in type that describes how the plug-in should be
     * integrated with Strange Eons
     */
    public int getPluginType();

    /**
     * Show (activate) or hide (deactivate) the plug-in. This method is most
     * often called when the user activates the plug-in's menu item.
     * <p>
     * Typically, the value of {@code show} will be the opposite of the
     * value currently returned by {@link #isPluginShowing}. If the plug-in uses
     * a modeless dialog box, then {@code isPluginShowing} should thus
     * return {@code true} when the dialog is showing.
     * <p>
     * If a modal dialog is shown, or if an operation that blocks the calling
     * thread is performed, then it will not be possible for the user to
     * activate the command again until this method returns. In that case,
     * {@code isPluginShowing} can be implemented to simply return
     * {@code false}.
     * <p>
     * <b>Notes:</b> This method is never called for {@code EXTENSION}
     * plug-ins. It is only called once, after initialization, for
     * {@code INJECTED} plug-ins.
     *
     * @param context a valid {@link PluginContext}
     * @param show if {@code true} show/start the plug-in, otherwise,
     * hide/cancel it
     * @see #isPluginShowing()
     */
    public void showPlugin(PluginContext context, boolean show);

    /**
     * Returns {@code true} if this plug-in's interface is currently
     * showing, or, if it has no interface, if it is currently running.
     * <p>
     * If the plug-in blocks the event thread when shown (for example, if it
     * displays a modal dialog), then this method can simply return
     * {@code false}.
     *
     * <b>Notes:</b> This method is only called for ACTIVATED plug-ins.
     *
     * @return {@code true} to indicate that the plug-in is "active"
     */
    public boolean isPluginShowing();

    /**
     * Returns {@code true} if it is currently valid to activate this
     * plug-in by calling {@link #showPlugin}. For example, a plug-in that only
     * works on components from a certain game might return {@code false}
     * if the currently edited component is not from that game.
     *
     * <p>
     * <b>Note:</b> Plug-ins must still check for any conditions that are
     * required to hold in {@link #showPlugin}. The plug-in may be activated
     * without checking this method, or the state may change between the time
     * that this method is called and the time the plug-in is activated.
     *
     * <p>
     * <b>Scripted Plug-in Notes:</b> The default implementation returns
     * {@code true}.
     *
     * @return {@code true} if the plug-in can be successfully and
     * meaningfully activated
     */
    public boolean isPluginUsable();

    /**
     * Returns an image that may be used to represent the plug-in on a menu or
     * toolbar. Strange Eons may resize the image to fit available space, and
     * may modify it to suit the context of use. Return <tt>null</tt> to
     * indicate that the plug-in does not have a representative image.
     *
     * @return an image that can be used as a menu item icon
     */
    public BufferedImage getRepresentativeImage();

    /**
     * Return a string that describes the key stroke that is the preferred
     * default accelerator key for this plug-in. In most cases, you should
     * return {@code null} for no default accelerator. The user can always
     * assign an accelerator key of their choice to an {@code ACTIVATED}
     * plug-in through the plug-in manager dialog.
     * <p>
     * The format of the string is similar to that used by
     * {@code javax.swing.KeyStroke}, but the special modifier
     * <tt>menu</tt> may be used to describe the standard menu accelerator
     * modifier on this platform (Ctrl, Command, etc.). An invalid descriptor is
     * silently ignored. There is no guarantee that the requested accelerator
     * will actually be assigned. (For example, it might already be in use by
     * another command.)
     *
     * <p>
     * <b>Note:</b> An accelerator key is only meaningful for
     * {@code ACTIVATED} plug-ins.
     *
     * @return a description of the preferred default accelerator
     */
    public String getDefaultAcceleratorKey();

    /**
     * A plug-in type value. An activated plug-in adds a new command to the
     * application. The command will be made accessible to the user by adding a
     * menu item to the
     * <b>Toolbox</b> menu with the following characteristics:
     * <ul>
     * <li>The menu item's label text will be the value of
     * {@link #getPluginName()}.
     * <li>The menu item's tool tip text will be the value of
     * {@link #getPluginDescription()}.
     * <li>If the plug-in has a representative image (and the user enables this
     * feature), then that image will be used to create an icon for the menu
     * item.
     * <li>If {@link #isPluginUsable()} returns {@code true}, the item will
     * be enabled; otherwise disabled.
     * <li>If the plug-in is showing, then the item will have a check mark,
     * otherwise it will not.
     * <li>If the menu item is selected by the user, then the plug-in will
     * either be shown or hidden depending on whether it is currently shown.
     * That is, code similar to the following will be executed:<br>
     * {@code showPlugin( pluginContext, !isPluginShowing() )}
     * </ul>
     */
    public static final int ACTIVATED = 0;

    /**
     * A plug-in type value. An {@code INJECTED} plug-in fills a role
     * between the {@code ACTIVATED} and {@code EXTENSION} types. Like
     * an {@code ACTIVATED} plug-in, it is loaded and unloaded on demand.
     * However, like an {@code EXTENSION} plug-in, there is no explicit
     * predetermined means to activate the effect (i.e., there is no
     * <b>Toolbox</b>
     * menu item). Either the plug-in adds its own explicit activation
     * method(s), or the plug-in is activated passively.
     *
     * <p>
     * This type of plug-in is typically used in the following cases:
     * <ul>
     * <li>Modifications are being made that are reversible (and do not require
     * the game database to be unlocked).
     * <li>New commands or other features are being added, but they will be
     * accessed by means other than the <b>Toolbox</b> menu. (A common example
     * being new commands for the project system.)
     * </ul>
     *
     * <p>
     * When loaded, the {@link #showPlugin} method will be called once with
     * {@code show == true}, at which time it should install its
     * modifications. These should continue to take effect until
     * {@link #unloadPlugin()} is called, at which time the modifications should
     * be removed.
     */
    public static final int INJECTED = 1;

    /**
     * A plug-in type value. Extension plug-ins are started exactly once, during
     * application startup and before the {@link gamedata.Lock game database} is
     * locked. Because of this, an extension is allowed to extend the database
     * by, for example, adding new games or component types.
     *
     * <p>
     * Extensions are never "shown" (via {@link #showPlugin}); they should do
     * all of their work when {@link #initializePlugin} is called. An
     * extension's {@link #unloadPlugin} method will be called at shutdown
     * (unless the program terminates abnormally). This allows the extension to
     * release any system resources that it may be holding or perform other
     * cleanup; unlike an {@code INJECTED} plug-in, it is not expected to
     * reverse the changes that it makes.
     *
     * <p>
     * To load successfully, an extension plug-in must both identify itself by
     * the {@code EXTENSION} type and be packaged in a bundle with the
     * <tt>.seext</tt> file name extension.
     */
    public static final int EXTENSION = 2;
}
