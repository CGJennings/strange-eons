package ca.cgjennings.ui.dnd;

import java.awt.Point;
import javax.swing.JComponent;

/**
 * A drag handler is responsible for creating drag tokens from a drag source. It
 * acts as the bridge between a UI component that can act as a drag source and
 * the drag token framework.
 *
 * @param T the type of object to be dragged
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see DropHandler
 * @see DragManager
 */
public interface DragHandler<T> {

    /**
     * Creates a drag token for a drag that was initiated in the specified drag
     * source at the specified point. Returns {@code null} if the specified
     * point is not draggable.
     *
     * @param manager the manager that is managing the drag
     * @param dragSource the component being dragged from
     * @param dragPoint the point at which the drag began
     * @return a drag token for the draggable object at the specified point
     */
    DragToken<T> createDragToken(DragManager<T> manager, JComponent dragSource, Point dragPoint);

    /**
     * Called when a drag gesture finishes, whether the drag token was
     * successfully dropped or not. This method is called after
     *
     * @param manager the manager that is managing the drag
     * @param dragSource the component being dragged from
     * @param token the token that was dragged
     * @param dropTarget the target that the token was dropped on, or
     * {@code null} if the drag was cancelled
     */
    void dragFinished(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget);
}
