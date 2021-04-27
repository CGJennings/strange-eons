package ca.cgjennings.ui.textedit.completion;

/**
 * A node representing a Java package in a {@link PackageRoot}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class PackageNode extends APINode {

    /**
     * Creates a new package node.
     *
     * @param name the package name
     */
    PackageNode(String name) {
        super(name);
    }
}
