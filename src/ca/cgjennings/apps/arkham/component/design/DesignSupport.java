package ca.cgjennings.apps.arkham.component.design;

import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import java.awt.Component;

/**
 * A design support provides feedback to the user about the design of their game
 * component. The goal of a design support is to help the user produce better
 * designs. Some ways of doing this are:
 * <ol>
 * <li> help the user to explore the space of possible designs more thoroughly
 * by helping them discover alternatives
 * <li> help the user to compare their design to known designs or to other
 * variations of the current design
 * <li> help the user to understand the possible consequences of their design
 * decisions
 * <li> inform the user when the design does not meet the usual design rules for
 * the component type
 * </ol>
 *
 * <p>
 * A game component either includes design support or it does not. If it
 * includes design support, then it will have a single design support instance
 *
 * @param G the type of game component supported by the component
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface DesignSupport<G extends GameComponent> {

    /**
     * Returns the game component being supported. A given instance of a design
     * support can only ever support one component.
     *
     * @return this support's supported component
     */
    public G getGameComponent();

    /**
     * When a support is attached to an editor, the editor will call this to
     * mark the analysis as out of date when the component is edited.
     */
    public void markChanged();

    /**
     * Creates an interface component that will display or visualize the design
     * support for the user. An editor for a game component that includes design
     * support may create a view to add to its interface. It will then pass its
     * view to the design support when it wants the design support to update the
     * support content.
     *
     * @return a component that visualizes a design support analysis
     */
    public Component createSupportView();

    /**
     * Updates a support view created by this design support to reflect the
     * current design analysis. Because it may be costly to create this
     * visualization, the view is not expected to update automatically. Instead,
     * the game component editor will monitor the component for changes and then
     * schedule an update of the the view for some future time.
     *
     * @param view the view to update; the result if a component that was not
     * created by calling this support's {@link #createSupportView()} method is
     * undefined
     */
    public void updateSupportView(Component view);

    /**
     * Returns {@code true} if the component's design is valid. The exact
     * meaning of "valid" depends on the component type, but the general intent
     * is that invalid components break the usual design rules for the component
     * type in such a way that the resulting component may be unfair. Strange
     * Eons does not use this value itself, but there are preference settings
     * that allow users to request that their components should be marked if
     * this returns {@code false}. (It is up to the plug-in author to
     * implement this in the component's {@link Sheet}s.
     *
     * @return {@code true} if the design is valid
     */
    public boolean isDesignValid();
}
