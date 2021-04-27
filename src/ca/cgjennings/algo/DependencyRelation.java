package ca.cgjennings.algo;

import java.util.Set;

/**
 * Represents the relation of dependency by allowing implementing classes to
 * specify the set of objects that they directly depend upon. (These objects may
 * in turn depend upon other objects, recursively, so that an object may have
 * both direct and indirect dependencies.)
 *
 * @param <T> the type of objects that express this type of dependency
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see TopologicalSorter
 */
public interface DependencyRelation<T extends DependencyRelation> {

    /**
     * Returns a set of objects that this object depends upon. If this object
     * does not depend on any objects, it is allowed to return either
     * <code>null</code> or an empty set.
     *
     * @return the set of objects directly required by this object
     */
    public Set<T> getDependants();
}
