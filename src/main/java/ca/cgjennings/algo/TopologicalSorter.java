package ca.cgjennings.algo;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A topological sorter produces an ordering that respects the requirement
 * relationships of a collection of objects. For example, suppose that you are
 * taking courses in school, and some courses are prerequisites for other
 * courses. (To take course X, you must first have taken all of the courses that
 * are its prerequisites.) A topological sorter would produce an ordering such
 * that, if you took the courses in that order, all of the prerequisites would
 * be satisfied.
 *
 * <p>
 * To create such an order, it is necessary that the dependency graph be
 * acyclic. That is, you cannot have a situation where A depends on B and B also
 * depends on A, directly or indirectly. For example, if A depends on B and C
 * and C depends on D and D depends on A, then there is a cycle A → C →
 * D → A. Attempting to sort a collection containing a cycle
 * will throw an exception.
 *
 * @param <T> the type of object that will be sorted
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TopologicalSorter<T extends DependencyRelation<T>> {

    private boolean lexSort = false;
    private boolean assumesAllPresent = false;
    private Comparator<T> comparator;

    /**
     * Returns {@code true} if this sorter will assume that all required
     * elements are present in all collections that are to be sorted.
     *
     * @return {@code true} if all required objects are to be included in the
     * input to {@link #topSort}
     * @see #setAllPresent(boolean)
     */
    public boolean isAllPresent() {
        return assumesAllPresent;
    }

    /**
     * Sets whether this sorter will assume that all possible elements are
     * included in collections that it sorts. Setting this to {@code true} makes
     * a guarantee that when sorting a collection, for all elements E in the
     * collection, if E depends on F then F is also in the collection. In other
     * words, everything that is required is already included in the collection
     * passed to {@link #topSort}. Setting this to {@code true} may result in
     * more efficient sorting, but if it is set to {@code true} when the
     * condition does not hold then the sorted output will be incomplete.
     *
     * <p>
     * When this is set to {@code false}, the sorter will detect the presence of
     * required objects that were not part of the original collection and
     * include them in the sorted output. Thus, the size of the sorted list may
     * be larger than that of the unsorted list. A useful side effect of this
     * feature is that if a single element is sorted, the result will be a list
     * of all of the elements that are required (directly or indirectly) by that
     * element.
     *
     * @param assumesAllPresent {@code true} if all required objects are to be
     * included in the input to {@link #topSort}
     */
    public void setAllPresent(boolean assumesAllPresent) {
        this.assumesAllPresent = assumesAllPresent;
    }

    /**
     * If {@code true}, then the sort order will also be ordered according to a
     * lexicographic sort.
     *
     * @return {@code true}
     * @see #setLexicographicallySorted(boolean)
     */
    public boolean isLexicographicallySorted() {
        return lexSort;
    }

    /**
     * Sets whether collections will also be lexicographically sorted. When set
     * to {@code true}, then at any point in the sorted list, the element at
     * that position is the one that is lexicographically least of all of the
     * elements that could be placed there without violating the topological
     * ordering. For example, a topological sort of a collection of courses
     * could be further sorted lexicographically by their course number so that
     * (to the maximum extent possible) the courses with the lowest number are
     * listed first.
     *
     * @param lexSort whether output should also be lexicographically sorted
     * @see #setComparator
     */
    public void setLexicographicallySorted(boolean lexSort) {
        this.lexSort = lexSort;
    }

    /**
     * Returns the comparator that will be used for lexicographic sorting. If
     * {@code null}, the natural order is used.
     *
     * @return the sorting comparator
     */
    public Comparator<T> getComparator() {
        return comparator;
    }

    /**
     * Sets the comparator that will be used if lexicographic sorting is
     * enabled. If {@code null}, which is the default, the natural order is
     * used.
     *
     * @param comparator the sorting comparator, or {@code null} for natural
     * order
     */
    public void setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    /**
     * Performs a topological sort on the elements of a collection using the
     * current settings, returning the sorted elements in a list.
     *
     * @param collection the collection of objects to sort
     * @return a list of the objects in sorted order
     */
    public List<T> topSort(Collection<T> collection) {
        // this is a working list of everything we haven't sorted yet
        List<T> unsorted = new LinkedList<>(collection);
        // when an element on the unsorted this can have all its requirements
        // satisfied by elements on the sorted list, then we can move it to
        // the sorted list; this will become our sort order
        List<T> sorted = new LinkedList<>();
        // this is used to speed up our ability to check what's on the sorted
        // list; so we don't do a linear search for every requirement
        Set<T> finished = new HashSet<>(collection.size());
        //

        // because the algorithm always takes the available elements from
        // left to right in each pass, it is sufficient to sort the list
        // ahead of time---however, if we discover new elements that we
        // didn't know about, we will have to repeat the sort
        if (lexSort) {
            Collections.sort(unsorted, comparator);
        }

        // keeps track of every element that we know about: if an element
        // depends on another element that wasn't part of the original
        // collection, we will also have to sort it
        Set<T> known;
        if (assumesAllPresent) {
            known = null;
        } else {
            known = new HashSet<>(unsorted);
        }

        // Our goal is to move all elements from unsorted to sorted
        while (!unsorted.isEmpty()) {
            // Find an object in unsorted whose requirements are now
            // met by the objects in sorted, and move it to sorted.
            boolean hasCycle = true;
            for (int i = 0; i < unsorted.size(); ++i) { // doesn't use iterator because the list might grow
                T el = unsorted.get(i);
                boolean satisfied;
                @SuppressWarnings("unchecked")
                Set<T> prereqs = el.getDependants();

                if (prereqs == null || prereqs.isEmpty()) {
                    // This has no requirements, so we can immediately move it
                    // (if there are no elements of this kind then there must be a cycle)
                    satisfied = true;
                } else {
                    // We start by assuming that the requirements are met, and
                    // we try to prove that they are not.
                    satisfied = true;

                    // If we do not assume all requirements are met, then we also
                    // need to check if this element has unknown requirements,
                    // so we always iterate over the whole set. Otherwise, we
                    // can quit as soon as something isn't satisifed.
                    if (assumesAllPresent) {
                        for (T req : prereqs) {
                            if (!finished.contains(req)) {
                                satisfied = false;
                                break;
                            }
                        }
                    } else {
                        for (T req : prereqs) {
                            if (satisfied && !finished.contains(req)) {
                                satisfied = false;
                            }
                            if (!known.contains(req)) {
                                // Never heard of this one... make sure we sort it, too
                                known.add(req);
                                unsorted.add(req);
                                hasCycle = false;
                            }
                        }
                    }
                }

                // Are all of the prerequisites satisfied? Then move it to sorted.
                if (satisfied) {
                    finished.add(el);
                    sorted.add(el);
                    unsorted.remove(el);
                    hasCycle = false;
                    break;
                }
            }

            // At the end of each pass, we must have either moved something
            // to sorted, or discovered new unknown elements. If we did neither,
            // then there are cycle(s) in the graph.
            if (hasCycle) {
                throw new GraphCycleException("cannot sort a cyclical relationship", unsorted);
            }
        }

        // Everything has been moved from unsorted to sorted. So we know there are
        // no cycles and that every object that is directly or indirectly required
        // is now on the sorted list. If sorting is requested, and we discovered some
        // objects that were not in the original collection, then the discovered
        // objects were not included in the initial lex sort. Therefore, we
        // repeat the topological sort using the now-complete list.
        if (lexSort && !assumesAllPresent && known.size() > collection.size()) {
            sorted = topSort(sorted);
        }

        return sorted;
    }
    
    /**
     * Returns a set of the direct and indirect objects required by an object.
     *
     * @param object the object to return all dependants for
     * @return a set of all objects that this object depends on, the objects
     * that those objects depend on, and so on
     * @throws GraphCycleException if there is a cycle in the dependency graph
     * @param <T> the type of the objects that depend on each other
     */
    public static <T extends DependencyRelation<T>> Set<T> getAllDependants(T object) {
        TopologicalSorter<T> s = new TopologicalSorter<>();
        HashSet<T> dependants = new HashSet<>(s.topSort(Collections.singleton(object)));
        dependants.remove(object);
        return dependants;
    }
}
