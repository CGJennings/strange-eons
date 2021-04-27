package ca.cgjennings.math;

import static java.lang.Math.*;

/**
 * Basic combinatoric mathematics support.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Combinatorics {

//	public static void main( String[] args ) {
//		long start = System.nanoTime();
//		for( int s = 1; s <= 20; ++s ) {
//			for( int d = 1; d <= 20; ++d ) {
//				if( s <= d ) {
//					//System.err.format( "%03.1f%%  ", 100d * upperCumulativeDistribution( s, d, 1/6d ) );
//					upperCumulativeDistribution( s, d, 1 / 6d );
//				}
//			}
//			//System.err.println();
//		}
//		long stop = System.nanoTime();
//		System.out.println( "" + ( stop - start ) + " ns" );
//	}
    /**
     * The largest <code>n</code> such that <code>n</code>! &lt;=
     * <code>Long.MAX_VALUE</code>.
     */
    private static final int MAXIMUM_LONG_FACTORIAL = 20;

    /**
     * Returns <i>n</i>! as a <code>long</code> value. In order to fit within a
     * <code>long</code>, the value of <code>n</code> must not be greater than
     * <code>MAXIMUM_LONG_FACTORIAL</code>.
     *
     * @param n the value to compute the factorial of
     * @return the factorial of <code>n</code>, that is, <code>n</code>!
     * @throws ArrayIndexOutOfBoundsException if <code>n</code> &lt; 0 or
     * <code>n</code> &gt; MAXIMUM_LONG_FACTORIAL
     */
    public static final long factorial(int n) {
        return factorialTable[n];
    }
    /**
     * A table of the first <code>MAXIMUM_LONG_FACTORIAL</code> + 1 factorials
     * (including 0!).
     */
    private static final long[] factorialTable = new long[MAXIMUM_LONG_FACTORIAL + 1];

    // Initialize the factorial table
    static {
        factorialTable[0] = 1L;
        factorialTable[1] = 1L;
        for (int i = 2; i <= MAXIMUM_LONG_FACTORIAL; ++i) {
            factorialTable[i] = factorialTable[i - 1] * i;
        }
    }

    /**
     * Return P(<i>n</i>, <i>r</i>), the number of lists of size <code>r</code>
     * that can be created from a set of <code>n</code> objects. Since this
     * method counts the number of unique <i>lists</i>, the order in which the
     * elements are selected is important. For example, the selection (1,2,3)
     * and the selection (3,1,2) are both counted by this method. In order to
     * fit within a <code>long</code>, the value of <code>n</code> must not be
     * greater than <code>MAXIMUM_LONG_FACTORIAL</code>.
     * <p>
     * <b>Example:</b> The set {1,2,3} contains 3 elements. If we construct
     * every possible list of length 2 that is composed of elements from that
     * set, we would find that there are 6 possibilities:<br>
     * (1,2), (1,3), (2,1), (2,3), (3,1) and (3,2)
     * <p>
     * Hence, <code>permutations( 3, 2 )</code> would return 6.
     *
     * @param n the size of the set from which the elements of lists are to be
     * taken
     * @param r the size of the lists to created
     * @return the number of unique lists that can be created
     * @throws ArrayIndexOutOfBoundsException if <code>n</code> &lt; 0,
     * <code>n</code> &gt; MAXIMUM_LONG_FACTORIAL, <code>r</code> &lt; 0, or
     * <code>r</code> &gt; <code>n</code>
     */
    public static final long permutations(int n, int r) {
        // no cleverness required; since the factorials are all in
        // a table, we can just divide
        return factorialTable[n] / factorialTable[n - r];
    }

    /**
     * Return C(<i>n</i>, <i>r</i>), the number of sets of size <code>r</code>
     * that can be created from a set of <code>n</code> objects. Since this
     * method counts the number of unique <i>sets</i>, the order in which the
     * elements are selected is not important. For example, the selection
     * (1,2,3) and the selection (3,1,2) would only be counted once by this
     * method. In order to fit within a <code>long</code>, the value of
     * <code>n</code> must not be greater than
     * <code>MAXIMUM_LONG_FACTORIAL</code>.
     * <p>
     * <b>Example:</b> The set {1,2,3} contains 3 elements. If we construct
     * every possible set of length 2 that is composed of elements from that
     * set, we would find that there are 3 possibilities:<br>
     * {1,2}, {1,3}, {2,3}
     * <p>
     * Hence, <code>combinations( 3, 2 )</code> would return 3.
     *
     * @param n the size of the set from which the elements of lists are to be
     * taken
     * @param r the size of the lists to created
     * @return the number of unique lists that can be created
     * @throws ArrayIndexOutOfBoundsException if <code>n</code> &lt; 0,
     * <code>n</code> &gt; MAXIMUM_LONG_FACTORIAL, <code>r</code> &lt; 0, or
     * <code>r</code> &gt; <code>n</code>
     */
    public static final long combinations(int n, int r) {
        return permutations(n, r) / factorialTable[r];
    }

    /**
     * Returns the probability that out of <code>trials</code> attempts, a test
     * that has a <code>pSuccess</code> probability of succeeding on each
     * attempt will succeed <i>exactly</i> <code>successes</code> times. For
     * example, the probability of rolling 10 dice and having exactly 2 of those
     * dice land on 6 is <code>probabilityMass( 2, 10, 1/6d )</code> (about
     * 0.29).
     *
     * @param successes the exact number of successes required
     * @param trials the number of trials to attempt
     * @param pSuccess the probability that a given trial will succeed
     * @return the probability that exactly <code>successes</code> trials will
     * succeed
     * @throws IllegalArgumentException if <code>pSuccess</code> is not in the
     * range (0,1)
     * @throws ArrayIndexOutOfBoundsException if <code>n</code> &lt; 0,
     * <code>n</code> &gt; MAXIMUM_LONG_FACTORIAL, <code>r</code> &lt; 0, or
     * <code>r</code> &gt; <code>n</code>
     */
    public static final double probabilityMass(int successes, int trials, double pSuccess) {
        if (pSuccess < 0d || pSuccess > 1d) {
            throw new IllegalArgumentException("pSuccess is not a probability");
        }

        double s = successes;
        double t = trials;

        return combinations(trials, successes) * pow(pSuccess, s) * pow(1 - pSuccess, t - s);
    }

    /**
     * Returns the probability that out of <code>trials</code> attempts, a test
     * that has a <code>pSuccess</code> probability of succeeding on each
     * attempt will succeed <i>at most</i> <code>successes</code> times. For
     * example, the probability of rolling 10 dice and having at most 2 of those
     * dice land on 6 is <code>cumulativeDistribution( 2, 10, 1/6d )</code>
     * (about 0.78).
     *
     * @param successes the maximum number of successes allowed
     * @param trials the number of trials to attempt
     * @param pSuccess the probability that a given trial will succeed
     * @return the probability that at most <code>successes</code> trials will
     * succeed
     * @throws IllegalArgumentException if <code>pSuccess</code> is not in the
     * range (0,1)
     * @throws ArrayIndexOutOfBoundsException if <code>n</code> &lt; 0,
     * <code>n</code> &gt; MAXIMUM_LONG_FACTORIAL, <code>r</code> &lt; 0, or
     * <code>r</code> &gt; <code>n</code>
     */
    public static final double cumulativeDistribution(int successes, int trials, double pSuccess) {
        double sum = 0d;
        for (int i = 0; i <= successes; ++i) {
            sum += probabilityMass(i, trials, pSuccess);
        }
        return sum;
    }

    /**
     * Returns the probability that out of <code>trials</code> attempts, a test
     * that has a <code>pSuccess</code> probability of succeeding on each
     * attempt will succeed <i>at least</i> <code>successes</code> times. For
     * example, the probability of rolling 10 dice and having at least 2 of
     * those dice land on 6 is
     * <code>upperCumulativeDistribution( 2, 10, 1/6d )</code> (about 0.52).
     * <p>
     * Mathematically, this value can be computed as:<br>
     * <code>upperCumulativeDistribution(s,t,p) = probabilityMass(s,t,p) + 1-cumulativeDistribution(s,t,p)</code>
     * <p>
     * Note that this method may not compute the value in exactly this way, and
     * so may return a slightly different result if comprared to the above.
     *
     * @param successes the minimum number of successes required
     * @param trials the number of trials to attempt
     * @param pSuccess the probability that a given trial will succeed
     * @return the probability that at least <code>successes</code> trials will
     * succeed
     * @throws IllegalArgumentException if <code>pSuccess</code> is not in the
     * range (0,1)
     * @throws ArrayIndexOutOfBoundsException if <code>n</code> &lt; 0,
     * <code>n</code> &gt; MAXIMUM_LONG_FACTORIAL, <code>r</code> &lt; 0, or
     * <code>r</code> &gt; <code>n</code>
     */
    public static final double upperCumulativeDistribution(int successes, int trials, double pSuccess) {
        double sum = 0d;
        for (int i = successes; i <= trials; ++i) {
            sum += probabilityMass(i, trials, pSuccess);
        }
        return sum;
    }

    private Combinatorics() {
    }
}
