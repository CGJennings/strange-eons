package ca.cgjennings.ui.textedit;

/**
 * Implemented by classes that may request a code support navigator.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface NavigationHost {
    /**
     * Returns the currently hosted navigator, or null.
     * @return the navigator that would be refreshed if a refresh was requested
     */
    Navigator getNavigator();
    
    /**
     * Requests that the host update to the latest navigation points.
     */
    void refreshNavigator();
}
