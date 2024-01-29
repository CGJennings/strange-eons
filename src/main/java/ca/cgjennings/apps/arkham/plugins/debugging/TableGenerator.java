package ca.cgjennings.apps.arkham.plugins.debugging;

/**
 * This interface is implemented by classes that wish to provide a new kind of
 * tabular data to users of the debugging system. Generators are made known to
 * the debugging system by registering them with the {@link Tables} class.
 *
 * <p>
 * <b>Note:</b> Tables are generated from a thread controlled by the debugger;
 * it must be safe to generate the table from this thread. Since the event
 * dispatch thread is often suspended during debugging, it is generally not a
 * good idea to create tables for data that can only be evaluated in the EDT, as
 * this may lead to a deadlock.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface TableGenerator {

    /**
     * Returns the user-friendly name of this table.
     *
     * @return the name of the table created by this generator
     */
    String getTableName();

    /**
     * Generates a current version of the table.
     *
     * @return a representation of the generated table data
     */
    InfoTable generateTable();
}
