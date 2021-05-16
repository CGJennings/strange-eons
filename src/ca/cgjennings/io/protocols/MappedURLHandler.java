package ca.cgjennings.io.protocols;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.LinkedList;

/**
 * A base class for creating protocol handlers that substitutes the source URL
 * with a stand-in. Implementations should override {@link #mapURL} and/or
 * {@link #mapConnection}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class MappedURLHandler extends URLStreamHandler {

    @Override
    protected final URLConnection openConnection(URL sourceURL) throws IOException {
        URL mapped = mapURL(sourceURL);
        return mapConnection(sourceURL, mapped);
    }

    /**
     * Maps a URL in the handled protocol to one of the underlying URLs. The
     * base class returns the original URL.
     *
     * @param sourceURL a URL in the protocol handled by this handler
     * @return a URL using another protocol that maps to the same resource as
     * this handler, or {@code null}
     * @throws IOException if an I/O error occurs while composing the new URL
     */
    protected URL mapURL(URL sourceURL) throws IOException {
        return sourceURL;
    }

    /**
     * Returns a {@code URLConnection} for the mapped URL. The base class
     * returns {@code mappedURL.openConnection()}.
     *
     * @param sourceURL the original URL
     * @param mappedURL the mapped URL returned from {@link #mapURL}
     * @return a connection for the URL
     * @throws IOException if an I/O error occurs or the mapped URL is
     * {@code null}
     */
    protected URLConnection mapConnection(URL sourceURL, URL mappedURL) throws IOException {
        if (mappedURL == null) {
            throw new FileNotFoundException(sourceURL.toExternalForm());
        }
        return mappedURL.openConnection();
    }

    /**
     * Installs all custom protocol handlers. There is normally no need to call
     * this as Strange Eons will install the handler during initialization.
     */
    public static void install() {
        String property = System.getProperty("java.protocol.handler.pkgs", null);
        if (property == null || !property.contains("ca.cgjennings.io.protocols")) {
            if (property == null) {
                property = "ca.cgjennings.io.protocols";
            } else {
                property = property + '|' + "ca.cgjennings.io.protocols";
            }
            System.setProperty("java.protocol.handler.pkgs", property);
        }
    }

    /**
     * Composes the host and path of a URL together into a single stream; this
     * is useful when processing file system-like URLs that do not use a host.
     *
     * @param url the URL to compose
     * @return the host and path, concatenated and separated by a slash, or
     * {@code null} if {@code null} was passed in
     */
    public static String getComposedPath(URL url) {
        if (url == null) {
            return null;
        }
        String host = url.getHost();
        if (host == null || host.isEmpty()) {
            return url.getPath();
        } else {
            return host + url.getPath();
        }
    }

    /**
     * Removes . and .. segments from a URL path. This can be used on the
     * concatenation of an absolute path with a path with one or more relative
     * segments to produce an equivalent absolute path.
     *
     * @param path the concatenated path
     * @return an absolute path
     */
    public static String removeRelativePathComponents(String path) {
        if (path.indexOf("./") < 0) {
            return path;
        }
        LinkedList<String> out = new LinkedList<>();
        for (String segment : path.substring(2).split("/", -1)) {
            switch (segment) {
                case ".":
                    continue;
                case "..":
                    if (out.size() > 0) {
                        out.removeLast();
                    }   break;
                default:
                    out.add(segment);
                    break;
            }
        }
        StringBuilder b = new StringBuilder(path.length());
        for (String segment : out) {
            if (b.length() > 0) {
                b.append('/');
            } else {
                b.append("//");
            }
            b.append(segment);
        }
        return b.toString();
    }
}
