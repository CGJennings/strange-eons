package ca.cgjennings.ui.textedit;

import java.util.Collections;
import java.util.List;
import resources.Language;

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

    /**
     * A navigator can return this constant to indicate that the generation of
     * navigation points is in progress. The caller should request the list
     * again later using the same source text.
     */
    public static final List<NavigationPoint> ASYNC_RETRY = Collections.unmodifiableList(
        Collections.singletonList(new NavigationPoint(Language.string("busy-script"),0))
    );
}
