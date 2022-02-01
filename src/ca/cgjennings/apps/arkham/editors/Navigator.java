package ca.cgjennings.apps.arkham.editors;

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
     * Called when this navigator is about to become the navigator for a code
     * editor. This allows the navigator to perform editor-specific
     * initialization. For example, it might install a custom
     * {@link Highlighter}.
     *
     * @param editor the editor that this navigator is being added to
     */
    void install(CodeEditor editor);

    /**
     * Called when this navigator is about to be removed from a code editor.
     * This allows the navigator to reverse editor-specific initialization. For
     * example, it might remove a previously installed {@link Highlighter}.
     *
     * @param editor the editor that this navigator is being removed from
     * @see #install(ca.cgjennings.apps.arkham.editors.CodeEditor)
     */
    void uninstall(CodeEditor editor);

    /**
     * Parses the specified source text and returns a list of navigation points.
     *
     * @param sourceText the source text to determine navigation points for
     * @return a (possibly empty) list of the extracted points
     */
    List<NavigationPoint> getNavigationPoints(String sourceText);
}
