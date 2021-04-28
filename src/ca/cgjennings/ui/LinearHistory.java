package ca.cgjennings.ui;

import java.util.LinkedList;
import java.util.List;

/**
 * A <code>LinearHistory</code> captures a sequence of user actions and allows
 * them to be replayed, as with the forward/back operations in a browsing
 * interface.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class LinearHistory<P> {

    private int index = 0;
    private boolean forwardBias = true;
    private final List<P> list = new LinkedList<>();

    /**
     * Creates a new linear history that tracks state objects (URL, location,
     * document, etc.) of type <code>P</code>.
     */
    public LinearHistory() {
    }

    /**
     * Clears the history.
     */
    public void clear() {
        index = 0;
        list.clear();
    }

    /**
     * Go directly to a new position (without using forward or back) and display
     * the position. Any positions after the current position in the history are
     * deleted, and the new <code>position</code> becomes the end of the
     * history.
     *
     * @param position
     */
    public void go(P position) {
//		To add a history limit
//		while( list.size() >= HISTORY_SIZE && index > 0 ) {
//			list.remove(0);
//			--index;
//		}
        if (!forwardBias) {
            ++index;
        }
        while (index < list.size()) {
            list.remove(list.size() - 1);
        }
        list.add(position);
        index = list.size();
        forwardBias = true;
        display(position);
    }

    /**
     * Go back one step in the history and display the resulting position.
     */
    public void back() {
        if (canGoBack()) {
            if (forwardBias) {
                --index;
                forwardBias = false;
            }
            if (list.size() == index) {
                --index;
            }
            display(list.get(--index));
        }
    }

    /**
     * Go forward one step in the history and display the resulting position.
     */
    public void forward() {
        if (canGoForward()) {
            if (!forwardBias) {
                ++index;
                forwardBias = true;
            }
            display(list.get(index++));
        }
    }

    /**
     * Returns <code>true</code> if it is possible to go back in the history.
     *
     * @return <code>true</code> if {@link #back()} will have any effect
     */
    public boolean canGoBack() {
        return index > (forwardBias ? 1 : 0);
    }

    /**
     * Returns <code>true</code> if it is possible to go forward in the history.
     *
     * @return <code>true</code> if {@link #forward()} will have any effect
     */
    public boolean canGoForward() {
        return index < list.size() + (forwardBias ? 0 : -1);
    }

    /**
     * Called when a new position is visited (via {@link #go}, {@link #back}, or
     * {@link #forward}). The base class does nothing; subclasses may override
     * this to customize behaviour.
     *
     * @param position the new position to display
     */
    public void display(P position) {
    }

    /**
     * Returns the number of positions stored in the history.
     *
     * @return the size of the history list
     */
    public int getSize() {
        return list.size();
    }
}
