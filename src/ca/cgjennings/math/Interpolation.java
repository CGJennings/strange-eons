package ca.cgjennings.math;

/**
 * Interpolate or clamp within a range of values.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class Interpolation {

    private Interpolation() {
    }

    /**
     * Return a linear interpolation between two double values.
     *
     * @param position the desired relative point between the low and high
     * @param low the low end of the range
     * @param high the high end of the range
     * @return the value that is position*100% of the way between low and high
     */
    public static double lerp(double position, double low, double high) {
        return low + (high - low) * position;
    }

    /**
     * Returns a {@linkplain Fn function} that performs linear interpolation.
     *
     * @param low the low end of the range
     * @param high the high end of the range
     * @return the function f(x) = lerp( x, low, high )
     */
    public static Fn createLerpFunction(final double low, final double high) {
        return (double x) -> lerp(x, low, high);
    }

    /**
     * Return a linear interpolation between two float values.
     *
     * @param position the desired relative point between the low and high
     * @param low the low end of the range
     * @param high the high end of the range
     * @return the value that is position*100% of the way between low and high
     */
    public static float lerp(float position, float low, float high) {
        return low + (high - low) * position;
    }

    /**
     * Return a linear interpolation between two int values.
     *
     * @param position the desired relative point between the low and high
     * @param low the low end of the range
     * @param high the high end of the range
     * @return the nearest integer value that is position*100% of the way
     * between low and high
     */
    public static int lerp(float position, int low, int high) {
        return low + (int) (((high - low) * position) + 0.5f);
    }

    /**
     * Maps a position in one range of double values to another range. This is
     * the same as using {@link #lerp} to interpolate between low2 and high2,
     * except that the ratio between the two is determined from the ratio of
     * position between low1 and high1. (So if position is half way between low1
     * and high1, this is the same as calling
     * {@code lerp( 0.5, low2, high2 )}.
     *
     * @param position the position in (low1, high1) to map to (low2, high2)
     * @param low1 the low end of the original range
     * @param high1 the high end of the original range
     * @param low2 the low end of the new range
     * @param high2 the high end of the new range
     * @return the relative value of position between low1 and high1 mapped to
     * the range between low2 and high2
     * @throws IllegalArgumentException if low1 and high1 are equal, in which
     * case the range is 0 and position does not represent any one ratio
     */
    public static double map(double position, double low1, double high1, double low2, double high2) {
        if (low1 == high1) {
            throw new IllegalArgumentException("range is 0");
        }
        double p = (position - low1) / (high1 - low1);
        return lerp(p, low2, high2);
    }

    /**
     * Returns a {@linkplain Fn function} that maps the input x value from the
     * range (low1, high1) to (low2, high2).
     *
     * @param low1 the low end of the original range
     * @param high1 the high end of the original range
     * @param low2 the low end of the new range
     * @param high2 the high end of the new range
     * @return the function f(x) = map( x, low1, high1, low2, high2 )
     */
    public static Fn createMapFunction(final double low1, final double high1, final double low2, final double high2) {
        if (low1 == high1) {
            throw new IllegalArgumentException("range is 0");
        }
        return (double x) -> {
            double p = (x - low1) / (high1 - low1);
            return lerp(p, low2, high2);
        };
    }

    /**
     * Maps a position in one range of float values to another range. This is
     * the same as using {@link #lerp} to interpolate between low2 and high2,
     * except that the ratio between the two is determined from the ratio of
     * position between low1 and high1. (So if position is half way between low1
     * and high1, this is the same as calling
     * {@code lerp( 0.5, low2, high2 )}.
     *
     * @param position the position in (low1, high1) to map to (low2, high2)
     * @param low1 the low end of the original range
     * @param high1 the high end of the original range
     * @param low2 the low end of the new range
     * @param high2 the high end of the new range
     * @return the relative value of position between low1 and high1 mapped to
     * the range between low2 and high2
     * @throws IllegalArgumentException if low1 and high1 are equal, in which
     * case the range is 0 and position does not represent any one ratio
     */
    public static float map(float position, float low1, float high1, float low2, float high2) {
        if (low1 == high1) {
            throw new IllegalArgumentException("range is 0");
        }
        float p = (position - low1) / (high1 - low1);
        return lerp(p, low2, high2);
    }

    /**
     * Maps a position in one range of integer values to another range. This is
     * the same as using {@link #lerp} to interpolate between low2 and high2,
     * except that the ratio between the two is determined from the ratio of
     * position between low1 and high1. (So if position is half way between low1
     * and high1, this is the same as calling
     * {@code lerp( 0.5, low2, high2 )}.
     *
     * @param position the position in (low1, high1) to map to (low2, high2)
     * @param low1 the low end of the original range
     * @param high1 the high end of the original range
     * @param low2 the low end of the new range
     * @param high2 the high end of the new range
     * @return the relative value of position between low1 and high1 mapped to
     * the range between low2 and high2
     * @throws IllegalArgumentException if low1 and high1 are equal, in which
     * case the range is 0 and position does not represent any one ratio
     */
    public static int map(int position, int low1, int high1, int low2, int high2) {
        if (low1 == high1) {
            throw new IllegalArgumentException("range is 0");
        }
        float p = (position - low1) / (float) (high1 - low1);
        return lerp(p, low2, high2);
    }

    /**
     * Clamp a double value to fall within a range from low to high. Values less
     * than low will be increased to low, values greater than high will be
     * reduced to high, and other values will be returned unchanged.
     *
     * @param value the value to clamp
     * @param low the lowest acceptable value
     * @param high the highest acceptable value
     * @return the clamped value
     */
    public static double clamp(double value, double low, double high) {
        return value < low ? low : value > high ? high : value;
    }

    /**
     * Clamp a float value to fall within a range from low to high. Values less
     * than low will be increased to low, values greater than high will be
     * reduced to high, and other values will be returned unchanged.
     *
     * @param value the value to clamp
     * @param low the lowest acceptable value
     * @param high the highest acceptable value
     * @return the clamped value
     */
    public static float clamp(float value, float low, float high) {
        return value < low ? low : value > high ? high : value;
    }

    /**
     * Clamp an integer value to fall within a range from low to high. Values
     * less than low will be increased to low, values greater than high will be
     * reduced to high, and other values will be returned unchanged.
     *
     * @param value the value to clamp
     * @param low the lowest acceptable value
     * @param high the highest acceptable value
     * @return the clamped value
     */
    public static int clamp(int value, int low, int high) {
        return value < low ? low : value > high ? high : value;
    }

    /**
     * This is a base class for interpolated functions. An interpolated function
     * is given a set of points. It then creates a function for a curve that
     * passes through or near those points, interpolating "smoothly" between
     * them.
     * <p>
     * One way to think of this is that you have a mystery curve that you don't
     * have a function for, but that you have some (possibly noisy) points which
     * are on or near the curve. An InterpolatedFunction will create a function
     * for your mystery curve, allowing you to evaluate the function for any
     * point instead of just your sample points. Depending on the kind of
     * interpolation, the data may be assumed to be noisy (the interpolator
     * finds a curve of best fit, effectively adjusting the points to fit an
     * idealized curve) or it may assume that the data are perfect (the
     * interpolator passes through each points, adjusting the curve as needed).
     * It may also fall between these extremes, coming close to each point but
     * not necessarily passing through it.
     */
    public static abstract class InterpolatedFunction implements Fn {

        /**
         * Creates an interpolated function for the points (x[0],y[0]),
         * (x[1],y[1]), ..., (x[x.length-1],y[y.length-1). The nature of the
         * interpolation is determined by the concrete implementation.
         *
         * <p>
         * The base class calls {@link #checkPoints} in order to validate the
         * input, but does nothing else.
         *
         * @param x an array of x-values for the points
         * @param y an array of y-values for the points
         * @throws NullPointerException if either array is {@code null}
         * @throws IllegalArgumentException if the data point criteria are not
         * met
         */
        public InterpolatedFunction(double[] x, double[] y) {
            checkPoints(x, y);
        }

        /**
         * Checks that the set of sample points satisfy any criteria for using
         * the interpolator. The base class checks the following:
         * <ul>
         * <li>x and y must be non-{@code null} and have the same length
         * <li>there must be at least one point (note that the concrete subclass
         * may require more than one point; check the specific class for
         * details)
         * <li>the values of x must be strictly increasing (for all j in 1 to
         * x.length-1, x[j-1] &lt; x[j]); this simply means that the x values
         * must be sorted from lowest to highest and must not contain any
         * repeats
         * </ul>
         * Subclasses should call the superclass implementation and then perform
         * any additional checks specific to the interpolator.
         *
         * @param x an array of x-values for the points
         * @param y an array of y-values for the points
         * @throws NullPointerException if either array is {@code null}
         * @throws IllegalArgumentException if the criteria above are not met
         */
        protected void checkPoints(double[] x, double[] y) {
            if (x == null) {
                throw new NullPointerException("x");
            }
            if (y == null) {
                throw new NullPointerException("y");
            }
            if (x.length != y.length) {
                throw new IllegalArgumentException("x.length must equal y.length");
            }
            if (x.length < 1) {
                throw new IllegalArgumentException("x.length must be >= 1");
            }
            for (int i = 1; i < x.length; ++i) {
                if (x[i - 1] >= x[i]) {
                    throw new IllegalArgumentException("x[" + (i) + "] < x[" + (i - 1) + "]");
                }
            }
        }

        /**
         * Returns the y-value of the interpolation function given an x-value.
         *
         * @param x the value to evaluate the curve function at
         * @return f(x), where f is a function for the interpolating curve
         */
        @Override
        public abstract double f(double x);
    }

    /**
     * A LinearRegression function finds the line of best fit for a set of
     * (usually noisy) data points.
     */
    public static class LinearRegression extends InterpolatedFunction implements UnivariatePolynomial {

        public LinearRegression(double[] x, double[] y) {
            super(x, y);
            findCoefficients(x, y);
        }

        @Override
        public double f(double x) {
            return a * x + b;
        }

        private void findCoefficients(double[] x, double[] y) {
            int i, n = x.length;

            double xi, yi, Sx = 0d, Sxy = 0d, Sy = 0d, Sxsq = 0d, N = n;

            for (i = 0; i < n; ++i) {
                xi = x[i];
                Sx += xi;
                Sxsq += (xi * xi);
                yi = y[i];
                Sxy += (xi * yi);
                Sy += y[i];
            }

            a = (n * Sxy - Sx * Sy) / (n * Sxsq - Sx * Sx);
            b = Sy / n - a * Sx / n;
        }

        @Override
        public int getNumCoefficients() {
            return 2;
        }

        /**
         * Returns the n<sup>th</sup> coefficient for this polynomial. The
         * formula for this interpolator's curve is:
         * <pre>f(x) = getCoefficient(1) * x + getCoefficient(0)</pre>
         */
        @Override
        public double getCoefficient(int n) {
            if (n == 0) {
                return b;
            }
            if (n == 1) {
                return a;
            }
            throw new IndexOutOfBoundsException("n: " + n);
        }

        private double a, b;
    }

    /**
     * A QuadraticRegression function finds the quadratic curve of best fit for
     * a set of (usually noisy) data points.
     */
    public static class QuadraticRegression extends InterpolatedFunction implements UnivariatePolynomial {

        public QuadraticRegression(double[] x, double[] y) {
            super(x, y);
            findCoefficients(x, y);
        }

        @Override
        public double f(double x) {
            return a * (x * x) + b * x + c;
        }

        private void findCoefficients(double[] x, double[] y) {
            int i, n = x.length;

            double N = n;
            double yi, xi, xisq, xicu, Sx = 0d, Sxsq = 0d, Sxcu = 0d, Sxfourth = 0d, Sy = 0d, Sxy = 0d, Sxsqy = 0d;

            for (i = 0; i < n; ++i) {
                yi = y[i];
                xi = x[i];
                xisq = xi * xi;
                xicu = xisq * xi;

                Sx += xi;
                Sxsq += xisq;
                Sxcu += xicu;
                Sxfourth += xicu * xi;
                Sy += yi;
                Sxy += xi * yi;
                Sxsqy += xisq * yi;
            }
            double Qsq = Sxsq * Sxsq, Psq = Sx * Sx, Rsq = Sxcu * Sxcu;
            double d = N * Sxsq * Sxfourth + 2 * Sx * Sxsq * Sxcu - (Qsq * Sxsq) - (Psq) * Sxfourth - N * (Rsq);

            a = (N * Sxsq * Sxsqy + Sx * Sxcu * Sy + Sx * Sxsq * Sxy - (Qsq) * Sy - (Psq) * Sxsqy - N * Sxcu * Sxy) / d;
            b = (N * Sxfourth * Sxy + Sx * Sxsq * Sxsqy + Sxsq * Sxcu * Sy - (Qsq) * Sxy - Sx * Sxfourth * Sy - N * Sxcu * Sxsqy) / d;
            c = (Sxsq * Sxfourth * Sy + Sxsq * Sxcu * Sxy + Sx * Sxcu * Sxsqy - (Qsq) * Sxsqy - Sx * Sxfourth * Sxy - (Rsq) * Sy) / d;
        }

        @Override
        public int getNumCoefficients() {
            return 3;
        }

        /**
         * Returns the n<sup>th</sup> coefficient for this polynomial. The
         * formula for this interpolator's curve is:
         * <pre>f(x) = getCoefficient(2) * x^2 * getCoefficient(1) * x + getCoefficient(0)</pre>
         */
        @Override
        public double getCoefficient(int n) {
            if (n == 0) {
                return c;
            }
            if (n == 1) {
                return b;
            }
            if (n == 2) {
                return a;
            }
            throw new IndexOutOfBoundsException("n: " + n);
        }

        private double a, b, c;
    }

    /**
     * A CubicSpline interpolator passes through each point in its data set,
     * connecting the points with cubic spline curves. The array of x values
     * used to define the curve must be ordered and strictly increasing. That
     * is, for {@code i} in {@code 0..x.length-2},
     * {@code x[i] &lt; x[i+1]}.
     */
    public static class CubicSpline extends InterpolatedFunction {

        /**
         * Creates a cubic spline that passes through the points (x[0],y[0]),
         * (x[1],y[1]), ..., (x[x.length-1],y[y.length-1). Default derivatives
         * will be computed for the end points of the spline.
         *
         * @param x an array of x-values for the points
         * @param y an array of y-values for the points
         * @throws NullPointerException if either array is {@code null}
         * @throws IllegalArgumentException if the data point criteria are not
         * met
         */
        public CubicSpline(double[] x, double[] y) {
            this(x, y, Double.NaN, Double.NaN);
        }

        /**
         * Creates a cubic spline that passes through the points (x[0],y[0]),
         * (x[1],y[1]), ..., (x[x.length-1],y[y.length-1). This constructor
         * specifies the derivatives at the lower ({@code yp0}) and upper
         * ({@code ypN}) boundaries of the function.
         *
         * @param x an array of x-values for the points
         * @param y an array of y-values for the points
         * @param yp0 derivative at the lower boundary, or
         * {@code Double.NaN} to let the interpolator choose
         * @param ypN derivative at the upper boundary, or
         * {@code Double.NaN} to let the interpolator choose
         * @throws NullPointerException if either array is {@code null}
         * @throws IllegalArgumentException if the data point criteria are not
         * met
         */
        public CubicSpline(double[] x, double[] y, double yp0, double ypN) {
            super(x, y);

            // the object maintains its own immutable copies of the tables
            // we also check that the table is ordered (strictly increasing) on x
            // for understandability, we order the tables from 1 as done in NRiC
            int n = x.length;
            this.yp1 = yp0;
            this.ypn = ypN;
            this.x = new double[n + 1];
            this.y = new double[n + 1];

            java.lang.System.arraycopy(x, 0, this.x, 1, n);
            java.lang.System.arraycopy(y, 0, this.y, 1, n);
            findDerivatives();
        }

        /**
         * Compute derivative table from x[], y[], yp1, ypn and store it to
         * interpolate values as required.
         */
        private void findDerivatives() {
            int i, k, n = x.length - 1;
            double p, qn, sig, un, u[];

            y2 = new double[n + 1];
            u = new double[n + 1];

            // NRiC uses yp1 or ypn >= 10^30 to indicate the use of "natural" boundary
            // derivatives --- we use the special value NaN instead; this is defined
            // to have the property that it is not equal to any value, including itself.
            if (yp1 != yp1) {
                y2[1] = u[1] = 0d;
            } else {
                y2[1] = -0.5;
                u[1] = (3d / (x[2] - x[1])) * ((y[2] - y[1]) / (x[2] - x[1]) - yp1);
            }

            // decomposition
            for (i = 2; i <= n - 1; i++) {
                sig = (x[i] - x[i - 1]) / (x[i + 1] - x[i - 1]);
                p = sig * y2[i - 1] + 2d;
                y2[i] = (sig - 1d) / p;
                u[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]) - (y[i] - y[i - 1]) / (x[i] - x[i - 1]);
                u[i] = (6d * u[i] / (x[i + 1] - x[i - 1]) - sig * u[i - 1]) / p;
            }

            // again, we use NaN to indicate a "natural" boundary derivative
            if (ypn != ypn) {
                qn = un = 0d;
            } else {
                qn = 0.5;
                un = (3d / (x[n] - x[n - 1])) * (ypn - (y[n] - y[n - 1]) / (x[n] - x[n - 1]));
            }

            // backsubstitution
            y2[n] = (un - qn * u[n - 1]) / (qn * y2[n - 1] + 1d);
            for (k = n - 1; k >= 1; --k) {
                y2[k] = y2[k] * y2[k + 1] + u[k];
            }
        }

        @Override
        public double f(double xValue) {
            int klo = 1, khi = x.length - 1, k;
            double h, b, a;

            while (khi - klo > 1) {
                k = (khi + klo) / 2;
                if (x[k] > xValue) {
                    khi = k;
                } else {
                    klo = k;
                }
            }

            h = x[khi] - x[klo];
            // NRiC checks that h != 0 here, but we already checked that
            // the tables are ordered so this is "impossible"
            a = (x[khi] - xValue) / h;
            b = (xValue - x[klo]) / h;
            return a * y[klo] + b * y[khi] + ((a * a * a - a) * y2[klo] + (b * b * b - b) * y2[khi]) * (h * h) / 6d;
        }

        private double yp1, ypn;
        private double[] x;
        private double[] y;
        private double[] y2;
    }
}
