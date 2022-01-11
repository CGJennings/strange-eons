package resources;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Construct paths to resource files from relative paths or path elements.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.2
 */
public class ResPath {

    private String proto;
    private Path path;

    /**
     * Creates a resource path from the specified string.
     *
     * @param path a non-null string describing a resource path
     */
    public ResPath(String path) {
        parse(Objects.requireNonNull(path));
    }

    private ResPath(String proto, Path path) {
        this.path = Objects.requireNonNull(path).normalize();
        if (this.path.startsWith("..")) {
            parse("");
        }
        this.proto = Objects.requireNonNull(proto);
    }

    private void parse(String path) {
        proto = "";
        Matcher m = PAT_PROTOCOL.matcher(path);
        if (m.find()) {
            proto = m.group(1) + "//";
        }
        this.path = java.nio.file.FileSystems.getDefault().getPath(path.substring(proto.length())).normalize();
    }

    /**
     * Returns the path that results from appending zero or more path elements
     * to this path.
     *
     * @param parts the path elements to append to this path
     * @return the combined path
     */
    public ResPath join(String... parts) {
        return resolve(String.join("/", parts));
    }

    /**
     * Returns the path that results from resolving a relative path against this
     * path.
     *
     * @param other the relative path
     * @return the resolved path
     */
    public ResPath resolve(String other) {
        return resolve(new ResPath(other));
    }

    /**
     * Returns the that results from resolving a relative path against this
     * path.
     *
     * @param other the relative path
     * @return the resolved path
     */
    public ResPath resolve(ResPath other) {
        if (!other.proto.isEmpty()) {
            return other;
        }
        if (other.path.startsWith("/")) {
            return new ResPath(proto, other.path);
        }
        return new ResPath(proto, path.resolve(other.path));
    }

    /**
     * Returns the parent of this path. Taking the parent of a root-level path
     * has no effect.
     *
     * @return a representing the parent of this path
     */
    public ResPath getParent() {
        return resolve("..");
    }

    /**
     * Returns the file or directory name, that is, the most deeply nested path
     * component.
     *
     * @return the file or directory name referenced by this path
     */
    public String getName() {
        return path.getFileName().toString();
    }

    /**
     * Regex to extract the protocol part of a path, if any.
     */
    private static final Pattern PAT_PROTOCOL = Pattern.compile("^([^:]*:)\\/*");

    /**
     * True if the path character must be transformed before returning a path
     * string.
     */
    private static final boolean TRANSFORM = File.separatorChar != '/';

    /**
     * Returns a string representation of the resource path.
     */
    @Override
    public String toString() {
        String pathPart = path.toString();
        if (TRANSFORM) {
            pathPart = pathPart.replace(File.separatorChar, '/');
        }
        return proto + pathPart;
    }

    /**
     * Returns whether this path is equal to another path.
     *
     * @param other the object to compare this object to
     * @return true if the paths are equal; false if they are not or the
     * parameter is not a path
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ResPath) {
            ResPath rhs = (ResPath) other;
            return proto.equals(rhs.proto) && path.equals(rhs.path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return proto.hashCode() ^ path.hashCode();
    }
}
