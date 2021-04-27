package ca.cgjennings.math;

/**
 * An interface implemented by classes that can be used as numerical functions
 * with arity 1.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface Fn {

    /**
     * Computes the value of the function.
     *
     * @param x the x value for which to evaluate the function
     * @return f(x)
     */
    double f(double x);

    /**
     * A function that returns the input value unchanged.
     */
    public static final Fn IDENTITY = (double x) -> x;
}
