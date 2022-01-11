package ca.cgjennings.apps.arkham;

import java.util.Locale;
import resources.RawSettings;

/**
 * A length measurement that can be represented in centimetres, inches, or
 * points.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class Length implements Comparable<Length> {

    private double points;

    /**
     * Creates a new length representing a distance of zero.
     */
    public Length() {
        points = 0d;
    }

    /**
     * Creates a new length equivalent to the distance specified by the
     * {@code measurement} and {@code unit}.
     *
     * @param measurement the magnitude of the distance
     * @param unit the unit in which the measurement is expressed
     */
    public Length(double measurement, int unit) {
        set(measurement, unit);
    }

    /**
     * Sets this length to the distance specified by the {@code measurement} and
     * {@code unit}.
     *
     * @param measurement the magnitude of the distance
     * @param unit the unit in which the measurement is expressed
     */
    public void set(double measurement, int unit) {
        points = convert(measurement, unit, PT);
    }

    /**
     * Returns the value of this length measurement in the requested unit.
     *
     * @param unit the desired unit, such as {@code CM}, {@code IN}, or
     * {@code PT}
     * @return this object's length measurement expressed in the specified unit
     */
    public double get(int unit) {
        return convert(points, PT, unit);
    }

    /**
     * Sets the value of this length measurement in points.
     *
     * @param points the length to set, in points
     */
    public void setPoints(double points) {
        this.points = points;
    }

    /**
     * Returns this length in points.
     *
     * @return the distance represented by this length, in points
     */
    public double getPoints() {
        return points;
    }

    /**
     * Centimetres
     */
    public static final int CM = 0;
    /**
     * Inches
     */
    public static final int IN = 1;
    /**
     * Points
     */
    public static final int PT = 2;

    private static final int UNITS = 3;

    /**
     * Compares this length with the specified length. Returns a negative
     * integer, zero, or a positive integer as this length is less than, equal
     * to, or greater than the specified length.
     *
     * @param rhs the length to be compared with this length
     * @return an integer whose sign is consistent with the sign of
     * {@code this.get(PT) - rhs.get(PT)}
     */
    @Override
    public int compareTo(Length rhs) {
        return (int) Math.signum(points - rhs.points);
    }

    /**
     * Returns {@code true} if and only if the compared object is a length of
     * equal size.
     *
     * @param obj the object to compare this to
     * @return {@code true} if {@code obj} is a {@code Length} that represents
     * the same distance
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Length other = (Length) obj;
        return points == other.points;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(points);
        return (int) bits ^ (int) (bits >> 32);
    }

    @Override
    public String toString() {
        return new StringBuilder(10).append(points).append(" pt").toString();
    }

    /**
     * Returns the a measurement in terms of a different unit.
     *
     * @param measurement the measurement to convert
     * @param sourceUnit the unit used for the source measurement
     * @param destUnit the unit used for the destination measurement
     * @return the measurement in the source unit converted to the destination
     * unit
     */
    public static double convert(double measurement, int sourceUnit, int destUnit) {
        if (sourceUnit < 0 || sourceUnit >= UNITS) {
            throw new IllegalArgumentException("invalid source unit");
        }
        if (destUnit < 0 || destUnit >= UNITS) {
            throw new IllegalArgumentException("invalid dest unit");
        }
        // convert to points
        switch (sourceUnit) {
            case IN:
                measurement *= 72d;
                break;
            case CM:
                measurement *= 72d / 2.54d;
                break;
            case PT:
                break;
            default:
                throw new AssertionError();
        }
        // convert to destination unit
        switch (destUnit) {
            case IN:
                measurement /= 72d;
                break;
            case CM:
                measurement *= 2.54d / 72d;
                break;
            case PT:
                break;
            default:
                throw new AssertionError();
        }
        return measurement;
    }

    /**
     * Returns the default unit to use when displaying lengths to the user. If
     * no default unit is set, then the default unit will be {@link #CM} unless
     * the default locale has a country value of "US", in which case the default
     * unit will be {@link #IN}.
     *
     * @return the default unit for presenting length measurements to the user
     */
    public static int getDefaultUnit() {
        int unit = CM;
        String u = RawSettings.getUserSetting("measurement-unit");
        if (u == null) {
            Locale loc = Locale.getDefault();
            String country = loc.getCountry();
            if (country.equals("US")) {
                unit = IN;
            }
        } else {
            try {
                unit = Integer.parseInt(u);
                if (unit < 0 || unit >= UNITS) {
                    unit = CM;
                }
            } catch (NumberFormatException e) {
                // will use default of CM
            }
        }
        return unit;
    }

    /**
     * Sets the default unit to use when displaying lengths to the user.
     *
     * @param unit the unit to set
     * @throws IllegalArgumentException if the unit is not one of the legal
     * values
     */
    public static void setDefaultUnit(int unit) {
        if (unit < 0 || unit >= UNITS) {
            throw new IllegalArgumentException("invalid unit: " + unit);
        }
        RawSettings.setUserSetting("measurement-unit", String.valueOf(unit));
        RawSettings.writeUserSettings();
    }
}
