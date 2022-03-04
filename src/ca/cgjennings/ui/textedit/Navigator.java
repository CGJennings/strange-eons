package ca.cgjennings.ui.textedit;

import java.util.List;

/**
 * Navigators build a list of {@link NavigationPoint}s that allow quick
 * navigation between semantically significant sections of edited source code.
 * The generated list can be displayed to the user in a special pane, allowing
 * the user to quickly jump between these significant locations.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see CodeEditor#setNavigator
 * @see NavigationPoint
 */
public interface Navigator {
    /**
     * Parses the specified source text and returns a list of navigation points.
     *
     * @param sourceText the source text to determine navigation points for
     * @return a (possibly empty) list of the extracted points
     */
    List<NavigationPoint> getNavigationPoints(String sourceText);
}
