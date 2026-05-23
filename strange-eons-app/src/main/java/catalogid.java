
import ca.cgjennings.apps.arkham.plugins.catalog.CatalogID;

/**
 * A convenience class that launches the {@link CatalogID} application for
 * generating catalog IDs from the command line. When run, it simply passes it
 * arguments on to {@link CatalogID#main(java.lang.String[])}.
 * <p>
 * To use this from a command line, use a command like the following:<br>
 * <pre>java -cp strange-eons.jar catalogid [arguments...]</pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class catalogid {

    /**
     * Launches the catalog ID command line tool by passing the command line
     * arguments to {@link CatalogID#main(java.lang.String[])}.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        CatalogID.main(args);
    }
}
