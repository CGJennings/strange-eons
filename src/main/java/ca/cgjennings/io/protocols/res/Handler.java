package ca.cgjennings.io.protocols.res;

import ca.cgjennings.io.protocols.MappedURLHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import resources.ResourceKit;

/**
 * A URL protocol handler for the {@code res:} protocol, which accesses
 * application resources.
 *
 * <p>
 * URLs consist of these segments (where [...] indicates an optional
 * segment):<br> {@code res: [//] [/] [path/]* file}<br>
 * Where:<br>
 * <dl>
 * <dt>[//]<dd> is optional, but makes it easier to distinguish URLs from files
 * in user-supplied strings.
 * <dt>[/]<dd>indicates that the path is not relative to /resources/, but to the
 * default package (/)
 * <dt>[path/]*<dd> is zero or more path entries (subfolders)
 * <dt>[file]<dd> is the name of the resource file
 * </dl>
 *
 * <p>
 * <b>Note:</b> the unusual class name is required for this class to be used by
 * the default protocol handler factory.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Handler extends MappedURLHandler {

    @Override
    protected URL mapURL(URL sourceURL) throws IOException {
        String path = MappedURLHandler.getComposedPath(sourceURL);
        if (sourceURL.getRef() != null) {
            path += '#' + sourceURL.getRef();
        }
        if (path != null) {
            return ResourceKit.composeResourceURL(path);
        }
        throw new FileNotFoundException(sourceURL.toExternalForm());
    }
}
