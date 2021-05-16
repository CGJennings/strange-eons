package ca.cgjennings.algo;

/**
 * A generic implementation of the diff algorithm. This algorithm describes the
 * changes needed to convert one sequence of objects into another. It is most
 * commonly used to determine the changes between two text files by comparing
 * their lines.
 *
 * <p>
 * The algorithm is used in combination with a {@link DiffListener}. The
 * listener's methods will be called in the order of the original sequence and
 * will describe a combined version of both the original and changed sequences,
 * with insertions and deletions at the appropriate points in order to describe
 * the actions that would covert the original sequence to the new sequence in
 * the minimal number of steps.
 *
 * @param <E> the type of the elements that may be inserted, deleted, or
 * changed; for example, in a text file each element might be a
 * {@code String} representing a single line
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.0
 */
public class Diff<E> {

    private DiffListener<E> li;

    /**
     * Create a new {@code Diff} instance that uses a default listener that
     * is useful for diagnostic purposes. For each element in the combined
     * collection, this listener prints to the output stream one of the
     * following followed by the string value of the element in question:
     * <dl>
     * <dt>"&gt;&nbsp;"<dd>this element must be inserted
     * <dt>"X&nbsp;"<dd>this element must be deleted
     * <dt>"&nbsp;&nbsp;"<dd>this element is unchanged
     * </dl>
     *
     * This listener prints the string value of each element from the combined
     * collection
     */
    public Diff() {
        li = new DiffListener<E>() {
            @Override
            public void inserted(Object original, Object changed, int originalIndex, E element) {
                System.out.println("> " + element);
            }

            @Override
            public void removed(Object original, Object changed, int originalIndex, E element) {
                System.out.println("X " + element);
            }

            @Override
            public void unchanged(Object original, Object changed, int originalIndex, E element) {
                System.out.println("  " + element);
            }
        };
    }

    /**
     * A diff engine that posts differences to the specified listener.
     *
     * @param listener a listener to be notified of the sequence of changes
     * required to modify the original version of an entity to the changed
     * version
     */
    public Diff(DiffListener<E> listener) {
        li = listener;
    }

    /**
     * Determine the difference between the original and changed element
     * sequences, posting them to the listener supplied at construction.
     *
     * @param original the original sequence of elements
     * @param changed the changed sequence of elements
     */
    public void findChanges(E[] original, E[] changed) {
        // number of lines of each file
        final int M = original.length;
        final int N = changed.length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        final int[][] opt = new int[M + 1][N + 1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M - 1; i >= 0; i--) {
            for (int j = N - 1; j >= 0; j--) {
                if (equal(original[i], changed[j], i, j)) {
                    opt[i][j] = opt[i + 1][j + 1] + 1;
                } else {
                    opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
                }
            }
        }

        // recover LCS
        int i = 0, j = 0;
        while (i < M && j < N) {
            if (equal(original[i], changed[j], i, j)) {
                li.unchanged(original, changed, i, original[i]);
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1]) {
                li.removed(original, changed, i, original[i]);
                i++;
            } else {
                li.inserted(original, changed, i, changed[j]);
                j++;
            }
        }

        // handle the leftovers
        while (i < M || j < N) {
            if (i == M) {
                li.inserted(original, changed, i, changed[j]);
                j++;
            } else if (j == N) {
                li.removed(original, changed, i, original[i]);
                i++;
            }
        }
    }

    /**
     * The method used to compare two entries for equality. The default
     * implementation is equivalent to:
     * <pre>
     * if( a == null ) {
     *     return b == null;
     * } else {
     *     return a.equals( b );
     * }
     * </pre>
     *
     * @param a an item from the original collection being compared
     * @param b an item from the changed collection being compared
     * @param originalIndex the index of {@code a} in the original sequence
     * @param changedIndex the index of {@code b} in the changed sequence
     * @return {@code true} if the objects should be considered equal for
     * the purposes of the diff operation
     */
    public boolean equal(E a, E b, int originalIndex, int changedIndex) {
        return a == null ? b == null : a.equals(b);
    }

//	public static void main( String[] args ) {
//		Integer[] a = new Integer[] { 0, 1, 2, 2, 4, 5, 6, 8, 10 };
//		Integer[] b = new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
//		Diff<Integer> d = new Diff<Integer>();
//		d.findChanges( a, b );
//	}
}
