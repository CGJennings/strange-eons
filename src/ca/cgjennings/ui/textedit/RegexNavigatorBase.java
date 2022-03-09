package ca.cgjennings.ui.textedit;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A base class for creating a navigator that finds navigation
 * points using a regular expression.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class RegexNavigatorBase implements Navigator {
    protected Pattern pattern;
    
    public RegexNavigatorBase(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Returns a list of navigation points. If the {@link #pattern} is currently
     * null, an empty list is returned. Otherwise, {@link #match} is called
     * to create the list of points.
     * 
     * @param sourceText the source text to determine navigation points for
     * @return a (possibly empty) list of the extracted points
     */
    @Override
    public List<NavigationPoint> getNavigationPoints(String sourceText) {
        if (pattern == null) {
            return Collections.emptyList();
        }
        List<NavigationPoint> results = new ArrayList<>(32);
        match(sourceText, results);
        return results;
    }
    
    /**
     * Searches the source text for matches of the regular expression, collecting
     * the resulting
     * {@linkplain #createNavigationPoint(java.util.regex.Matcher, java.lang.String) navigation points}.
     * 
     * @param sourceText the source text to determine navigation points for
     * @param a list to use to collect extracted points
     */
    protected void match(String sourceText, List<NavigationPoint> results) {
        Matcher m = null;
        try {
            boolean initialize = true;
            m = pattern.matcher(sourceText);
            while (m.find()) {
                NavigationPoint np = createNavigationPoint(m, sourceText, initialize);
                if (np != null) {
                    results.add(np);
                }
                initialize = false;
            }
        } catch (Throwable t) {
            dumpRegexThrowable(t, sourceText, m);
        }
    }

    /**
     * Called with each match result to create a navigation point based on the
     * match. May return null to indicate that no point should be added for
     * the match. The base class returns a simple point based on the text and
     * location of the match.
     * 
     * @param m the match information
     * @param sourceText the source text
     * @param initialize if true, this is the first match of a new pass
     * @return a navigation point for the match, or null
     */
    protected NavigationPoint createNavigationPoint(Matcher m, String sourceText, boolean initialize) {
        return new NavigationPoint(m.group(), m.start());
    }

    /**
     * Logs a description of an exception that occurs during the matching process.
     *
     * @param t the exception that was thrown
     * @param text the source text being matched against
     * @param m the matcher that threw {@code t}
     */
    protected void dumpRegexThrowable(Throwable t, String text, Matcher m) {
        StrangeEons.log.log(Level.SEVERE, "navigation regexp exception", t);
        System.err.println("--------");
        if (m != null) {
            int pos = 0;
            try {
                pos = m.start();
            } catch (IllegalStateException e) {
            }
            System.err.print(text.substring(0, pos));
            System.err.print(">>|<<");
            System.err.println(text.substring(pos));
        }
        System.err.println("--------");
    }
}
