package ca.cgjennings.algo;

/**
 * An adapter that can be wrapped around a progress listener to scale the update
 * messages it receives to a certain range.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class ProgressRangeAdapter implements ProgressListener {

    private float base, range;
    private ProgressListener adaptee;

    /**
     * Creates a new range adapter. The input progress range of 0..1 will be
     * mapped to the range {@code rangeLow}..{@code rangeHigh} before
     * passing the progress message on to the adaptee.
     *
     * @param rangeLow the progress value to report when the input progress is 0
     * @param rangeHigh the progress value to report when the input progress is
     * 1
     * @param adaptee the listener that the adapted range will be forwarded to
     * @throws IllegalArgumentException if the range values are out of the
     * allowed range of progress values (0 to 1), or if
     * {@code rangeLow &gt; rangeHigh}
     * @throws NullPointerException if the adaptee is {@code null}
     */
    public ProgressRangeAdapter(float rangeLow, float rangeHigh, ProgressListener adaptee) {
        if (rangeLow > rangeHigh) {
            throw new IllegalArgumentException("rangeLow > rangeHigh");
        }
        if (adaptee == null) {
            throw new NullPointerException("adaptee");
        }
        if (rangeLow < 0f) {
            throw new IllegalArgumentException("rangeLow < 0");
        }
        if (rangeHigh > 1f) {
            throw new IllegalArgumentException("rangeHigh > 1");
        }
        this.base = rangeLow;
        this.range = rangeHigh - rangeLow;
        this.adaptee = adaptee;
    }

    @Override
    public boolean progressUpdate(Object source, float progress) {
        return adaptee.progressUpdate(source, base + range * progress);
    }
}
