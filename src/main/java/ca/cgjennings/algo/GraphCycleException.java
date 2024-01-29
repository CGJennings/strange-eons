package ca.cgjennings.algo;

import java.util.Collection;

/**
 * An exception thrown when an algorithm that operates on a graph fails because
 * the graph contains a cycle.
 */
@SuppressWarnings("serial")
public class GraphCycleException extends IllegalArgumentException {

    private final Collection<?> culprits;

    /**
     * Creates a new exception for a detected cycle in a graph.
     *
     * @param message the detail message to associate with the cycle
     * @param culprits the objects involved in the cycle
     */
    public GraphCycleException(String message, Collection<?> culprits) {
        super(message);
        this.culprits = culprits;
    }

    /**
     * Returns a collection of the objects involved in the cycle, as passed to
     * the constructor.
     *
     * @return the culprits that were passed to the constructor
     */
    public Collection<?> getCycleMembers() {
        return culprits;
    }
}
