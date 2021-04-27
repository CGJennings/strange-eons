package ca.cgjennings.ui.dnd;

import java.awt.Point;
import javax.swing.JComponent;

/**
 * An abstract base class that combines a {@link DragHandler} and a
 * {@link DropHandler} in a single object. This can be a useful basis for
 * building interactions with reasonably simple drag logic, such as dragging and
 * dropping between two containers. For convenience, the method
 * {@link #acceptDrop acceptDrop} returns <code>true</code>; the methods {@link #dragFinished dragFinished}, {@link #dragEnter dragEnter},
 * {@link #dragExit dragExit}, and {@link #dragMove dragMove} have empty
 * implementations.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractDragAndDropHandler<T> implements DragHandler<T>, DropHandler<T> {

    public AbstractDragAndDropHandler() {
    }

    @Override
    public boolean acceptDrop(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget, Point location) {
        return true;
    }

    @Override
    public void dragFinished(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget) {
    }

    @Override
    public void dragEnter(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget, Point location) {
    }

    @Override
    public void dragExit(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget) {
    }

    @Override
    public void dragMove(DragManager<T> manager, JComponent dragSource, DragToken<T> token, JComponent dropTarget, Point location) {
    }
}
