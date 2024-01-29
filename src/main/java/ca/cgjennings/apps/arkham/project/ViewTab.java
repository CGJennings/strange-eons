package ca.cgjennings.apps.arkham.project;

import java.awt.Component;

/**
 * An interface implemented by classes that can add a tab to the project
 * information area displayed at the bottom of {@link ProjectView}s. New view
 * tab types are registered using {@link ProjectView#registerViewTab}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface ViewTab {

    /**
     * Returns the label that will be used for tabs of this type.
     *
     * @return the tab's label text
     */
    String getLabel();

    /**
     * Returns a unique identifier for this view tab.
     *
     * @return a unique ID string
     */
    String getViewTabName();

    /**
     * Returns a component that will display the content of this tab. If this
     * method returns {@code null}, then nothing will be added to the view for
     * this project.
     *
     * @param v the view that this tab will appear in
     * @param p the project that will be displayed in the view
     * @return a component that will display the content of the tab for this
     * project
     */
    Component createViewForProject(ProjectView v, Project p);
}
