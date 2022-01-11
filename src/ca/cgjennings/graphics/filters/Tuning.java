package ca.cgjennings.graphics.filters;

import ca.cgjennings.graphics.ImageUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

/**
 * The {@code Tuning} class contains tuning hints that help the image
 * processing system decide between multiple algorithms when performing certain
 * operations. A default tuning profile is built in, but more accurate tuning
 * values can be determined for a particular platform by calling
 * {@link #update}. This should only be done when no image processing is
 * currently taking place. Tuning profiles can be read to and from a file to
 * avoid having to call {@code update} for each application run.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class Tuning {

    private Tuning() {
    }

    public static float SET_INT_ARGB = Float.MAX_VALUE;
    public static float SET_INT_RGB = 32 * 32;
    public static float SET_INT_OTHER = 128 * 128;

    public static float GET_INT_ARGB = Float.MAX_VALUE;
    public static float GET_INT_RGB = 32 * 32;
    public static float GET_INT_OTHER = 128 * 128;

    public static float PER_ROW = 128 * 128;
    public static float PER_IMAGE = 128 * 128;

    public static float GROW_ROWS = 128 * 128;

    public static float PREMUL = 128 * 128;

    public static float FLIP = 128 * 128;

    /**
     * Copies the tuning parameter values into a set of Properties. Tuning
     * parameters will be added using a key consisting of "IMGOP_ACCEL_"
     * followed by the tuning parameter name. This can be used to save the
     * tuning parameters instead of running update
     *
     * @param p the properties instance to add the tuning parameters to
     */
    public synchronized static void write(Properties p) {
        for (Field f : Tuning.class.getFields()) {
            f.getModifiers();
            if (Modifier.isPublic(f.getModifiers())) {
                try {
                    p.setProperty("IMGOP_ACCEL_" + f.getName(), String.valueOf(f.getFloat(null)));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        p.setProperty("IMGOP_ACCEL_VERSION", "1");
    }

    /**
     * Sets the tuning parameters using previously written properties.
     *
     * @param p the properties instance to read the turning parameters from
     */
    public synchronized static void read(Properties p) {
        for (Field f : Tuning.class.getFields()) {
            f.getModifiers();
            if (Modifier.isPublic(f.getModifiers())) {
                try {
                    String v = p.getProperty("IMGOP_ACCEL_" + f.getName());
                    if (v != null) {
                        try {
                            long lv = Long.valueOf(v);
                            f.setFloat(null, lv);
                        } catch (NumberFormatException e) {
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Updates the tuning parameters with platform-specific data by running a
     * sequence of test operations. This is a cover for
     * {@code update( 5, false )}.
     */
    private static void update() {
        update(5, false);
    }

    /**
     * Updates the tuning parameters with platform-specific data by running a
     * sequence of test operations. The accuracy parameter allows you to trade
     * speed for accuracy. Lower values will allow the tests to complete more
     * quickly and use fewer resources. Higher values will obtain more accurate
     * results.
     *
     * @param accuracy a value from 1-9, with higher values requested
     * progressively more accurate results
     * @param printResults if {@code true} the results will be printed to
     * System.err as they are generated.
     */
    public synchronized static void update(int accuracy, boolean printResults) {
        // init accuracy
        if (accuracy < 1) {
            accuracy = 1;
        } else if (accuracy > 9) {
            accuracy = 9;
        }
        final int reps = 2 + accuracy * 2;
        final int[] sizes;
        switch (accuracy) {
            case 1:
            case 2:
            case 3:
                sizes = new int[]{32, 64, 128, 256};
                break;
            case 4:
            case 5:
            case 6:
                sizes = new int[]{32, 64, 96, 128, 192, 256};
                break;
            case 7:
            case 8:
            case 9:
                sizes = new int[]{16, 32, 64, 96, 128, 192, 256, 384, 512};
                break;
            default:
                throw new AssertionError();
        }

        BufferedImage[] bis = new BufferedImage[sizes.length];
        for (int i = 0; i < sizes.length; ++i) {
            bis[i] = makeImage(sizes[i]);
        }

        for (Field f : Tuning.class.getFields()) {
            try {
                Test t = (Test) Tuning.class.getDeclaredField("_" + f.getName()).get(null);
                float v = runTest(f, t, bis, reps);
                f.setFloat(null, v);
                if (printResults) {
                    System.err.printf("%s = %,d\n", f.getName(), v);
                }
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }

    private static BufferedImage makeImage(int size) {
        BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(Color.ORANGE);
            g.fillOval(1, 1, size - 2, size - 2);
        } finally {
            g.dispose();
        }
        return bi;
    }

    private static float runTest(Field f, Test test, BufferedImage[] bis, int reps) throws Exception {
        int i = 0;
        for (; i < bis.length; ++i) {
            float oldValue = f.getFloat(null);
            try {
                f.setFloat(null, Float.MAX_VALUE);
                long sTime = test.test(reps, bis[i], true);
                f.setFloat(null, 0);
                long pTime = test.test(reps, bis[i], false);
                if (pTime < sTime) {
                    break;
                }
            } finally {
                f.setFloat(null, oldValue);
            }
        }
        if (i == bis.length) {
            return Float.MAX_VALUE;
        } else {
            return test.evaluation(bis[i].getWidth(), bis[i].getHeight());
        }
    }

    private static abstract class Test {

        protected Object initObject;

        public long test(int reps, BufferedImage bi, boolean serial) {
            initObject = bi;
            init(bi, serial);

            long min = Long.MAX_VALUE;
            for (int i = 0; i < reps; ++i) {
                if (serial) {
                    long start = System.nanoTime();
                    run();
                    long time = System.nanoTime() - start;
                    min = Math.min(min, time);
                } else {
                    long start = System.nanoTime();
                    run();
                    long time = System.nanoTime() - start;
                    min = Math.min(min, time);
                }
            }

            dispose();
            return min;
        }

        // called before all testing; sets initObject with its test data
        public void init(BufferedImage bi, boolean serial) {

        }

        public abstract void run();

        /**
         * Returns the value to store for this test's
         *
         * @param width the width of the first image that parallel was faster
         * for, or -1
         * @param height the height of the first image that parallel was faster
         * for, or -1
         * @return the threshold value to store for this test
         */
        public long evaluation(int width, int height) {
            return (width * height);
        }

        public void dispose() {

        }
    }

    private static class GetIntTest extends Test {

        private final int t;

        public GetIntTest(int type) {
            t = type;
        }

        @Override
        public void init(BufferedImage bi, boolean serial) {
            initObject = ImageUtilities.ensureImageHasType(bi, t);
        }

        @Override
        public void run() {
            BufferedImage bi = (BufferedImage) initObject;
            AbstractImageFilter.getARGB((BufferedImage) initObject, null);
        }
    }

    private static class SetIntTest extends Test {

        private final int t;
        private int[] data;

        public SetIntTest(int type) {
            t = type;
        }

        @Override
        public void init(BufferedImage bi, boolean serial) {
            initObject = ImageUtilities.ensureImageHasType(bi, t);
            data = AbstractImageFilter.getARGB((BufferedImage) initObject, null);
        }

        @Override
        public void run() {
            BufferedImage bi = (BufferedImage) initObject;
            AbstractImageFilter.getARGB((BufferedImage) initObject, null);
        }

        @Override
        public void dispose() {
            data = null;
        }

    }

    private static final Test _GET_INT_ARGB = new GetIntTest(BufferedImage.TYPE_INT_ARGB);
    private static final Test _GET_INT_RGB = new GetIntTest(BufferedImage.TYPE_INT_RGB);
    private static final Test _GET_INT_OTHER = new GetIntTest(BufferedImage.TYPE_INT_ARGB_PRE);

    private static final Test _SET_INT_ARGB = new GetIntTest(BufferedImage.TYPE_INT_ARGB);
    private static final Test _SET_INT_RGB = new GetIntTest(BufferedImage.TYPE_INT_RGB);
    private static final Test _SET_INT_OTHER = new GetIntTest(BufferedImage.TYPE_INT_ARGB_PRE);

    public static void main(String[] args) {
        update(4, true);
    }
}
