package ca.cgjennings.apps.arkham;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An export container represents a file or file system that can contain a
 * collection of exported files. For example, a container might store exported
 * files in a ZIP archive or a folder. One application of export containers is
 * the export command used with game components. This command displays a dialog
 * that allows the user to choose how the component will be exported by
 * selecting any one of the <i>registered</i> containers.
 *
 * <p>
 * Export containers are registered with the application for use by anyone
 * wanting to export a collection of files. To get the available containers,
 * call
 * {@link ca.cgjennings.apps.arkham.StrangeEons#getRegisteredExportContainers}.
 * Plug-ins can create new kinds of containers and register them by calling
 * {@link ca.cgjennings.apps.arkham.StrangeEons#registerExportContainer}.
 *
 * To use an export container, follow these steps:
 * <ol>
 * <li>Call {@link #selectLocation} to allow the user to select a destination
 * for the container or perform other configuration steps. If this returns
 * <code>false</code>, cancel the export.
 * <li>Call {@link #createContainer} to start the export process by creating the
 * container.
 * <li>Call {@link #addEntry} once for each file to be written. For each call,
 * write the file to the provided output stream (closing the stream when done).
 * <li>Call {@link #closeContainer} once all files have been written.
 * </ol>
 *
 * <p>
 * Once a container has been created, you must either close it or destroy it
 * before a new container can be created. The following Java code pattern is
 * illustrates the process:
 * <pre>
 * ExportContainer ec = theExportContainerToUse;
 * ec.selectLocation( baseName );
 * ec.createContainer();
 * try {
 *     // write files to the container
 *     while( moreFilesToWrite ) {
 *         OutputStream out = ec.addEntry( "file name " + n );
 *         try {
 *             // write file data to output stream
 *         } finally {
 *             out.close();
 *         }
 *     }
 *     ec.closeContainer(false);
 * } catch( Throwable t ) {
 *     ec.destroyContainer();
 *     // handle or throw exception
 * }
 * </pre>
 *
 * <p>
 * Note that there is typically only one instance of a given export container
 * that is shared by all users. It is therefore vital that users of a contain
 * surround its use with appropriate <code>try</code> ... <code>catch</code>
 * blocks. Otherwise, an error may leave the container instance in an invalid
 * state and it will not be usable again until the application is restarted.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public interface ExportContainer {

    /**
     * Returns an identifier unique to this container type. It is never shown to
     * the user and must not be localized; it is used to identify the user's
     * preferred container in user settings.
     *
     * @return a unique identifier for the container
     */
    String getIdentifier();

    /**
     * Returns a description of the container format, ideally in the user
     * interface locale.
     *
     * @return a description of the container, such as "ZIP Archive"
     */
    @Override
    String toString();

    /**
     * Displays a dialog that allows the user to select a destination for the
     * container. The dialog is not necessarily a file dialog; for example, it
     * might prompt the user to enter account credentials for an online service
     * or enter the address of an FTP server. It is not required to show any
     * dialog at all, and may simply return <code>true</code> if there is
     * nothing for the user to decide.
     *
     * @param baseName a suggested base name that can be used to compose a file
     * or folder name for the container
     * @param locationHint an optional component that may be used as a hint to
     * locate any dialogs that must be created
     * @return <code>true</code> if the export should proceed, or
     * <code>false</code> if the user cancels the operation
     */
    boolean selectLocation(String baseName, Component locationHint);

    /**
     * This method can be used to set an explicit location for the container
     * instead of asking the user to select a location. It can only work with
     * containers that write to the local file system, such as
     * {@link FolderExportContainer} and {@link ZIPExportContainer}. The method
     * is provided for testing and for use with explicitly created containers.
     *
     * @param location the parent folder where the container should be created
     * @throws UnsupportedOperationException if the container type does not
     * support creating containers in the file system
     */
    void setLocation(File location);

    /**
     * Begins an export operation by creating an empty container.
     *
     * @throws IOException if an I/O error occurs
     */
    void createContainer() throws IOException;

    /**
     * Creates a new file in the container with the given name and returns an
     * output stream for the caller to write the file to. After writing the
     * file, you are responsible for calling <code>close()</code> on the output
     * stream.
     *
     * @param name the name of the file to create in the container
     * @return an output stream to which the "file" should be written
     * @throws IOException if an I/O error occurs
     */
    OutputStream addEntry(String name) throws IOException;

    /**
     * Close the current container, allowing a new container to be created.
     *
     * @param display if <code>true</code>, the user has requested that the
     * container be "displayed"
     * @throws IOException if an I/O exception occurs
     */
    void closeContainer(boolean display) throws IOException;

    /**
     * Destroys the current container instead of closing it. This can be called
     * instead of {@link #closeContainer} if an I/O error occurs while writing
     * the container. Ideally, it should delete the partially-created container.
     */
    void destroyContainer();

    /**
     * Returns <code>true</code> if the container has user-configurable options.
     *
     * @return <code>true</code> if the container can be {@link #configure}d
     */
    boolean isConfigurable();

    /**
     * Show a dialog allow the user to configure the container options.
     *
     * @param locationHint an optional hint as to where to locate the dialog
     * @throws UnsupportedOperationException if the container isn't configurable
     */
    void configure(Component locationHint);

    /**
     * Returns a hint as to whether a file format is supported. This method
     * returns <code>true</code> if the file type is definitely supported. It
     * returns <code>false</code> if the file type is not supported, or if the
     * container cannot determine whether the file type is supported.
     *
     * <p>
     * A common use of this method is to determine whether a container is
     * compatible with some kind of task being presented to the user. In these
     * cases, implementations that always return <code>false</code> will not be
     * included in the list of container choices presented to the user.
     *
     * <p>
     * Some export containers may not support files in some formats: while a ZIP
     * archive can hold any kind of file, an image hosting service might only
     * accept, say, PNG and JPG images. This method allows an export container
     * to return a hint to exporters as to whether a file format is compatible
     * with the container. To use it, the caller passes in a string representing
     * the typical file name extension for files of the format in question.
     *
     * <p>
     * Note that the {@link #addEntry} method is <i>required</i> to accept any
     * content that it is given, but an export container may choose to do
     * anything it wishes with unsupported formats. For example, it may skip the
     * file by returning a dummy output stream. Or it may accept the file but
     * silently convert it into an acceptable format. (The documentation for the
     * container should specify how unsupported files are handled.)
     *
     * @param extension the typical file format extension for the file, such as
     * "png" for PNG images
     * @return <code>true</code> if the file type is definitely supported, and
     * <code>false</code> otherwise
     */
    boolean isFileFormatSupported(String extension);
}
