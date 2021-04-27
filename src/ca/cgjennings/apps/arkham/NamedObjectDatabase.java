package ca.cgjennings.apps.arkham;

/**
 * This interface defines the Java-side interface to the named object database
 * that can be obtained from
 * {@link ca.cgjennings.apps.arkham.StrangeEons#getNamedObjects()}. The purpose
 * of the database is to allow plug-in scripts to share objects and communicate
 * with each other. This interface provides a way to access these objects from
 * compiled Java code as well.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see ca.cgjennings.apps.arkham.StrangeEons#getNamedObjects()
 */
public interface NamedObjectDatabase {

    /**
     * Returns the object associated with the requested name, or
     * <code>null</code> if there is no object associated with the name.
     *
     * @param name the object name to return
     * @return the value of the object in the database, or <code>null</code>
     * @throws NullPointerException if name is <code>null</code>
     */
    Object getObject(String name);

    /**
     * Associates an object with the specified name. If an object with this name
     * already exists, a warning will be logged to help diagnose naming
     * conflicts.
     *
     * @param name the name of the object to return
     * @param object the object to associate with the name in the database
     * @throws NullPointerException if the name or object is <code>null</code>
     * @throws IllegalArgumentException if the name is already associated with
     * an object
     */
    void putObject(String name, Object object);

    /**
     * Removes the object with the requested name, if any.
     *
     * @param name the name of the object to remove
     * @throws NullPointerException if name is <code>null</code>
     */
    void removeObject(String name);

}
