package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.component.conversion.ConversionSession;
import ca.cgjennings.apps.arkham.component.conversion.UpgradeConversionTrigger;
import ca.cgjennings.apps.arkham.deck.Deck;
import ca.cgjennings.apps.arkham.diy.DIY;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import gamedata.Game;
import java.io.Serializable;
import resources.CoreComponents;
import resources.Settings;

/**
 * This interface defines common methods for all supported types of
 * "components": paper gaming elements that may consist of one or more faces
 * (sides). Concrete custom component implementations will typically be
 * subclasses of {@link AbstractGameComponent}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface GameComponent extends Serializable, Cloneable {
    /**
     * Returns the standard class map name that describes this component.
     * For a compiled component this is the fully qualified name of the class.
     * For a standard DIY component, it is {@code diy:} followed by the resource
     * path of the script file.
     * If the component was created from a {@code script:} class map entry,
     * that will not be returned. The name of the true underlying type that was
     * ultimately created by the script is returned instead.
     *
     * @return the class map type of this instance
     */
    default String getClassName() {
        return getClass().getName();
    }

    /**
     * Returns the name of this component. This is not the name of the component
     * type, but the name of the specific component. For example, a component
     * that represents a game item would return the name of the item.
     *
     * @return the component's name; possibly a shortened version of the full
     * name
     * @see #getFullName()
     */
    public String getName();

    /**
     * Returns the "full name" of this component. Typically this is the same as
     * {@link #getName}, but {@link #getName} may optionally return a shorter
     * name. For example, a component that represents a human character might
     * return the character's first name from {@link #getName} and their full
     * name from this method.
     *
     * @return the full name of the component
     * @see #getName()
     */
    public String getFullName();

    /**
     * Returns the design rationale comments associated with this component. If
     * there are no comments, it should return an empty string.
     *
     * @return design comments supplied by the user of the component
     */
    public String getComment();

    /**
     * Returns a {@link Settings} instance that will return this component's
     * private settings. A component's private settings are saved along with the
     * component when it is written to a file. This can be used to override the
     * default settings for component (stored in the master settings for the
     * game) as a way to "hack" existing component designs. It can also be used
     * by the component itself to store arbitrary information.
     * {@link DIY DIY components} generally use the component's private settings
     * to store the current user-configurable state of the component.
     *
     * <p>
     * Note that setting the key with the name {@link Game#GAME_SETTING_KEY}
     * ("game") will change the parent scope of the private settings to the
     * {@link Game#getMasterSettings() master settings instance} for that game
     * whose code matches the new value. (The initial value of this key is
     * normally set on the component's half using the game code specified in the
     * component's class map entry.)
     *
     * @return the private settings that can be used to override settings for
     * this component
     */
    public Settings getSettings();

    /**
     * Sets this component's content to an empty state. Typically, this is
     * called from an editor when the user wishes to erase their work and start
     * over.
     */
    public void clearAll();

    /**
     * Returns the sheets attached to this component to draw its faces, or
     * {@code null} if no sheets are attached. The returned array is owned
     * by the game component and must not be modified.
     *
     * @return the sheets that will be updated by calls to
     * {@link #markChanged(int)}.
     * @see #getSheets
     */
    public Sheet[] getSheets();

    /**
     * Sets the sheets that are attached to this component to draw its faces.
     * Once set, the array is owned by the component and must not be modified.
     *
     * @param sheets the sheets to associate with the component, or
     * {@code null} to clear the associated sheets
     * @see #getSheets
     * @see #createDefaultSheets
     */
    public void setSheets(Sheet[] sheets);

    /**
     * Creates a set of default sheets that are compatible with this component
     * and associates them with the component as if by calling
     * {@link #setSheets}. As with {@link #getSheets}, the returned sheets are
     * owned by the component and must not be modified.
     *
     * @return the newly created sheets
     */
    public Sheet[] createDefaultSheets();

    /**
     * Returns human-readable names for the sheets used by this component. A
     * typical result would be something like
     * {@code ["Front Face", "Back Face"]}, localized for the user
     * interface language.
     *
     * <p>
     * Implementations should assume that the titles are for the same kinds and
     * number of sheets that are returned by {@link #createDefaultSheets()}. (In
     * other words, if a user of this class decides to use their own sheet
     * implementations, you are not responsible for ensuring the sheet titles
     * are accurate.)
     *
     * <p>
     * <b>Note:</b> The returned sheet name may be shared with any number of
     * callers. The values must be considered read-only unless otherwise stated
     * by the subclass documentation.
     *
     * @return an array of sheet titles matching the assigned sheets, or
     * {@code null} if there are no sheets attached
     * @see #createDefaultSheets
     */
    public String[] getSheetTitles();

    /**
     * Creates an editor capable of modifying this component.
     *
     * @return a new editor that can be added to the application window to edit
     * this component
     * @see StrangeEonsAppWindow#addEditor
     */
    public AbstractGameComponentEditor<? extends GameComponent> createDefaultEditor();

    /**
     * Called to signal that changes have been made that require the
     * {@code i}the sheet to be redrawn. This is typically not called
     * directly. Instead, calling a method like "{@code setName}" should
     * check if the name being set is actually different, and if so then call
     * this method for each sheet that may have changed as a result. Plug-ins
     * that customize an existing component may also call this method as needed
     * to reflect new features that they have added.
     *
     * <p>
     * Implementations of this method will typically call the
     * {@link Sheet#markChanged()} method of the relevant sheet (unless the
     * sheet set is {@code null}), set a flag for use by
     * {@link #hasChanged()}, and then call {@link #markUnsavedChanges()}.
     *
     * @param i the index of the sheet that needs to be redrawn
     */
    public void markChanged(int i);

    /**
     * Returns {@code true} if this component has been modified since the
     * last call to {@code hasChanged()}.
     *
     * @return {@code true} if the component has changed since this was
     * last called
     */
    public boolean hasChanged();

    /**
     * Returns the value of this component's unsaved changes flag.
     *
     * @return {@code true} if this component has unsaved changes
     */
    public boolean hasUnsavedChanges();

    /**
     * This method sets the component's unsaved changes flag. It is typically
     * called from {@link #markChanged}.
     */
    public void markUnsavedChanges();

    /**
     * This method is called by the component's editor when the the component is
     * saved to clear the component's unsaved changes flag.
     */
    public void markSaved();

    /**
     * Returns {@code true} if components of this type can be placed in a
     * deck. Typically, only components that don't have faces return
     * {@code false}. (Decks themselves, for example, cannot be placed
     * inside other decks.)
     *
     * @return {@code true} if and only if this component can be added to a
     * deck
     * @see #createDefaultSheets()
     * @see Deck#isDeckLayoutSupported(java.io.File)
     */
    public boolean isDeckLayoutSupported();

    /**
     * Returns a deep copy of this game component. The default clone
     * implementation provided by {@code super.clone()} will return a
     * <i>shallow</i> copy of the object. This will correctly clone all of this
     * instance's fields that have primitive types. It is then up to you to
     * clone any object fields where the field is not of an immutable type.
     * Images used to store portraits, although not technically immutable, are
     * treated as immutable by Strange Eons. So long as you also follow this
     * convention, you can save memory by sharing the shallow copy of the image.
     *
     * <p>
     * <b>Debugging tip:</b> One operation that makes use of the
     * {@code clone()} method is the <b>Spin Off</b> command. If you apply
     * this command, make changes to the copied component, redraw the original
     * component, and notice that changes in the copy have carried over to the
     * original, then you are using a shallow copy rather than a deep copy.
     * (That is, you are sharing a reference to the same mutable object rather
     * than making a copy of the mutable object during the cloning.)
     *
     * @return a deep copy of this component
     */
    public GameComponent clone();

    /**
     * Checks if all required libraries and extensions are installed. If a
     * required library is not install This method is called when the component
     * is read from a file, and possibly at other times
     *
     * <p>
     * This can safely be implemented as an empty method. However, implementing
     * it correctly improves the user experience since they can be notified of
     * which plug-ins they need to install to correctly use the component. In
     * the case of a required but not installed library, the library can
     * actually be downloaded and installed on demand and the component can then
     * be successfully opened.
     *
     * @see CoreComponents
     * @throws CoreComponents.MissingCoreComponentException if a required
     * plug-in is missing or out of date and the user cannot or will not install
     * it on demand
     */
    public void coreCheck();

    /**
     * Returns the {@link ConversionSession} created when the component type no
     * longer handles the component. This method is called after a component is
     * read from file. Should return {@code null} when no conversion is needed.
     *
     * @see ConversionSession
     * @return a conversion context if the upgrade requires a conversion
     */
    default UpgradeConversionTrigger createUpgradeConversionTrigger() {
        return null;
    }

    /**
     * Called on a component that is being converted into another component
     * type. Based on the conversion strategy, the old component may modify the
     * new component directly, or modify the conversion context, or do nothing.
     * This method is called before calling
     * {@link #convertTo(GameComponent, ConversionContext)} on the target, and
     * before any automatic conversion steps.
     *
     * @param target the new component that will replace this one
     * @param context the conversion context
     */
    default void convertFrom(ConversionSession session) {
    }

    /**
     * Called on the replacement component when converting a component to
     * another component type. Based on the conversion strategy, the new
     * component may modify itself directly, or modify the conversion context,
     * or do nothing. This method is called after calling
     * {@link #convertFrom(GameComponent, ConversionContext)} on the source, but
     * before any automatic conversion steps.
     *
     * @param source the old component that will be replaced
     * @param context the conversion context
     */
    default void convertTo(ConversionSession session) {
    }
}
