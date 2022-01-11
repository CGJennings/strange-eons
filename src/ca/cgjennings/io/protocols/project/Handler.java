package ca.cgjennings.io.protocols.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.StrangeEonsAppWindow;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.io.protocols.MappedURLHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * A URL protocol handler for the {@code project:} protocol, which accesses the
 * contents of the open project.
 *
 * <p>
 * Note: the unusual class name is required for this class to be used by the
 * default protocol handler factory.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class Handler extends ca.cgjennings.io.protocols.MappedURLHandler {

    @Override
    protected void parseURL(URL u, String spec, int start, int limit) {
        super.parseURL(u, spec, start, limit);
        if (u.getHost() != null && !u.getHost().isEmpty()) {
            setURL(u, u.getProtocol(), "", -1, null, null, u.getHost() + u.getPath(), u.getQuery(), u.getRef());
        }
    }

    @Override
    protected URLConnection mapConnection(URL sourceURL, URL mappedURL) throws IOException {
        String path = MappedURLHandler.getComposedPath(sourceURL);
        if (path != null) {
            int slashes = 0;
            for (; slashes < path.length(); ++slashes) {
                if (path.charAt(slashes) != '/') {
                    break;
                }
            }
            path = path.substring(slashes);
            StrangeEonsAppWindow af = StrangeEons.getApplication() == null ? null : StrangeEons.getWindow();
            if (af != null) {
                Project p = af.getOpenProject();
                if (p != null) {
                    URL projURL = p.getFile().toURI().toURL();
                    projURL = new URL(projURL, path);
                    return projURL.openConnection();
                }
            }
        }
        throw new FileNotFoundException(path);
    }
}
