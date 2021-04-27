package ca.cgjennings.math;

/**
 * An interface implemented by univariate polynomials, i.e., polynomials in one
 * variable.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface UnivariatePolynomial {

    /**
     * Returns the number of coefficients for this polynomial. This is not the
     * same as the degree, since the high coefficients may be zero; it simply
     * indicates the highest currently possible coefficient in this
     * implementation.
     */
    public int getNumCoefficients();

    /**
     * Returns the n<sup>th</sup> coefficient for this polynomial.
     *
     * @throws IndexOutOfBoundsException if n &lt; 0 or n &gt;
     * {@link #getNumCoefficients()}-1
     */
    double getCoefficient(int n);
}
