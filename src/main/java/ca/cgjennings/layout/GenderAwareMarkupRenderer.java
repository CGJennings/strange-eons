package ca.cgjennings.layout;

import resources.Language;

/**
 * This class overrides the {@code handleUnknownTag} method of
 * {@code MarkupRenderer} to allow arbitrary tags that vary with the gender of
 * some object.
 * <p>
 * More generally, it allows tags of the form &lt;left/right&gt; which will
 * generate either the left text or the right text depending on the "gender" the
 * renderer has been set to.
 * <p>
 * To prevent confusion with closing tags, if no text should be produced for a
 * given gender, the tag should indicate this with a hyphen, e.g.,
 * &lt;-/something&gt; would produce either "" or "something".
 * <p>
 * Some examples of how the tags can be used:
 * <ul>
 * <li> {@code <Jack/Diane> was a great scholar of old.}
 * <li> {@code The priest<-/ess> is angry.}
 * <li> {@code You bite <his/her/its/their> hand<-/-/-/s>.}
 * <li> {@code The <orange/yellow/red/green> key fits this lock.}
 * </ul>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class GenderAwareMarkupRenderer extends MarkupRenderer {

    public GenderAwareMarkupRenderer() {
        super();
    }

    public GenderAwareMarkupRenderer(double dpi) {
        super(dpi);
    }

    /**
     * Sets the "gender" using a simple male/female rule. If female is
     * {@code true}, the second segment of a gender tag is used. If
     * {@code false}, the first segment is used.
     *
     * @param female if the second segment should be used
     */
    public void setGender(boolean female) {
        setGender(female ? 1 : 0);
    }

    /**
     * Set the segment of gender tags to be used. A gender tag may have any
     * number of segments/separated/by/slashes. This index indicates which
     * segment to use. A common mapping is 0 = male, 1 = female, and 2 (if
     * applicable) is neuter ("it").
     */
    public void setGender(int segmentToUse) {
        if (gender != segmentToUse) {
            gender = segmentToUse;
            invalidateLayoutCache();
        }
    }

    /**
     * Return the current gender setting as a {@code boolean}; {@code true}
     * indicates that the right side of gender-sensitive tags will be selected.
     */
    public int getGender() {
        return gender;
    }

    /**
     * Checks for tags of the form &lt;m/f&gt; or &lt;capital m/f&gt;and returns
     * replacement text based on the current gender setting.
     */
    @Override
    protected String handleUnknownTag(String tagnameLowercase, String tagnameOriginalCase) {
        String replacement = super.handleUnknownTag(tagnameLowercase, tagnameOriginalCase);

        if (replacement != null) {
            return replacement;
        }

        String tagname = tagnameOriginalCase;
        int i = tagname.indexOf('/');
        if (i < 1) {
            return null;
        }

        boolean capitalized = false;
        if (tagname.startsWith("capital ")) {
            tagname = tagname.substring("capital ".length());
            capitalized = true;
            i -= "capital ".length();
        }

        int segment = gender;

        boolean capitalize = false;
        int segmentStart = 0;
        int segmentEnd = i;

        if (tagname.startsWith("capital ")) {
            segmentStart += "capital ".length();
            capitalized = true;
        }

        while (segment > 0) {
            segmentStart = segmentEnd + 1;
            segmentEnd = tagname.indexOf('/', segmentStart);
            --segment;
        }

        if (segmentStart < 0) {
            replacement = "";
        } else {
            if (segmentEnd < 0) {
                segmentEnd = tagname.length();
            }
            replacement = tagnameOriginalCase.substring(segmentStart, segmentEnd);
        }

        if (capitalized) {
            replacement = capitalize(replacement);
        }

        if (replacement.equals("-")) {
            replacement = "";
        }

        return replacement;
    }

    /**
     * Returns a string identical to {@code s} except that the first character,
     * if any and if it has a capital version, is capitalized.
     */
    public static String capitalize(String s) {
        if (s.length() > 1) {
            return s.substring(0, 1).toUpperCase(Language.getGameLocale()) + s.substring(1);
        } else {
            return s.toUpperCase();
        }
    }

    public static final int GENDER_LEFT = 0;
    public static final int GENDER_MIDDLE = 1;
    public static final int GENDER_RIGHT = 2;

    private int gender = 0;
}
