package ca.cgjennings.ui.dnd;

import java.awt.Point;
import javax.swing.JComponent;

/**
 * A drop handler is responsible for responding to drops of a {@link DragToken}
 * onto a drop target. It acts as the bridge between a UI component that can act
 * as a drop target and the drag token framework.
 *
 * @param T the type of object to be dropped
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see DragHandler
 * @see DragManager
 */
public interface DropHandler<T> {

    /**
     * Called to determine if a drop target is willing to accept a particular
     * drop.
     *
     * @param manager the manager that is managing the drag
     * @param dragSource the component being dragged from
     * @param token the token that was dragged
     * @param dropTarget the target that the token would be dropped on
     * @param location the location of the potential drop
     * @return <code>true</code> if the handler would allow the drop,
     * <code>false</code> otherwise
     */
    boolean acceptDrop(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget, Point location);

    /**
     * Called when a drag gesture finishes with a drop. The handler should take
     * whatever action is necessary to complete the action. Typically this means
     * adding the content of the token to the container represented by the drop
     * target.
     *
     * @param manager the manager that is managing the drag
     * @param dragSource the component being dragged from
     * @param token the token that was dragged
     * @param dropTarget the target that the token is being dropped on
     * @param location the location of the drop
     * @return returns <code>true</code> if the drop was successful, or
     * <code>false</code> if it could not be completed
     */
    boolean handleDrop(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget, Point location);

    /**
     * Called when the cursor enters a viable drop target. This can be used to
     * update the visual representation of the target.
     *
     * @param manager the manager that is managing the drag
     * @param dragSource the component being dragged from
     * @param token the token that was dragged
     * @param dropTarget the target that the token is being dropped on
     * @param location the location of the cursor
     */
    void dragEnter(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget, Point location);

    /**
     * Called when the cursor leaves a drop target (or moves over part of the
     * target that will not accept the drop). This can be used to update the
     * visual representation of the target.
     *
     * @param manager the manager that is managing the drag
     * @param dragSource the component being dragged from
     * @param token the token that was dragged
     * @param dropTarget the target that the token is being dropped on
     */
    void dragExit(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget);

    /**
     * Called when the cursor is moved over a viable drop target. This can be
     * used to update the visual representation of the target.
     *
     * @param manager the manager that is managing the drag
     * @param dragSource the component being dragged from
     * @param token the token that was dragged
     * @param dropTarget the target that the token is being dropped on
     * @param location the location of the cursor
     */
    void dragMove(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget, Point location);
}
