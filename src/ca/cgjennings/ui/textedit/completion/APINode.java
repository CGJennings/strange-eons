package ca.cgjennings.ui.textedit.completion;

/**
 * An abstract completion node base class that represents part of a static API,
 * such as a class or package.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see APIDatabase#getPackageRoot()
 */
public abstract class APINode extends DefaultCNode<APINode> {

    /**
     * The node's unique parent node. This is set automatically when the node is
     * created within a {@link PackageRoot}. It should not be modified.
     */
    protected APINode parent;

    /**
     * Creates a new node with the specified name.
     *
     * @param name the node name (may not be <code>null</code>)
     */
    public APINode(String name) {
        super(name);
    }

    /**
     * Returns the canonical parent of this node. The canonical parent of the
     * node is the node that would result from removing this node's name from
     * its fully qualified name. For example, the node that represents the class
     * <code>java.lang.String</code> has the parent node that represents
     * <code>java.lang</code>.
     *
     * @return the unique parent of this node
     */
    public final APINode getParent() {
        return parent;
    }

    /**
     * Adds a child to this node with the specified name. If an entry with the
     * specified name exists, no change is made. Note that this assumes that the
     * specified name follows the standard Java naming convention: packages must
     * start with lower case letters, and classes must start with upper case
     * letters (or other valid identifier characters).
     *
     * @param child the name of the child class or package to add
     * @return the child node for the specified name
     */
    public APINode add(String child) {
        if (child == null) {
            throw new NullPointerException("child");
        }
        if (child.isEmpty()) {
            throw new IllegalArgumentException("empty child");
        }

        // check if the child already exists; if so, we're done
        APINode node = find(child);
        if (node == null) {
            if (!Character.isJavaIdentifierStart(child.charAt(0))) {
                throw new IllegalArgumentException("invalid name: " + child);
            }
            final int len = child.length();
            for (int i = 1; i < len; ++i) {
                if (!Character.isJavaIdentifierPart(child.charAt(i))) {
                    throw new IllegalArgumentException("invalid name: " + child);
                }
            }
            // the child doesn't exist, and the name is valid: create the node
            boolean isPkg = false;
            if (!(this instanceof ClassNode)) {
                isPkg = Character.isLowerCase(child.charAt(0));
            }
            if (isPkg) {
                node = new PackageNode(child);
            } else {
                node = new ClassNode(child);
            }
            node.parent = this;
            add(node);
        }
        return node;
    }

    /**
     * Returns the fully qualified name of this node. For example, the fully
     * qualified name of a node representing this class would be
     * <code>ca.cgjennings.ui.textedit.completion.APINode</code>. Within a given
     * {@link PackageRoot}, no two nodes may have the same fully qualified name.
     *
     * @return the fully qualified name of this node
     */
    public String getFullyQualifiedName() {
        StringBuilder b = new StringBuilder(40);
        fillInName(b, false);
        return b.toString();
    }

    private void fillInName(StringBuilder b, boolean internal) {
        APINode parent = getParent();
        if (parent != null) {
            parent.fillInName(b, internal);
            if (b.length() > 0) {
                if (internal && parent instanceof ClassNode) {
                    b.append('$');
                } else {
                    b.append('.');
                }
            }
        }
        b.append(getName());
    }

    String getFullyQualifiedNameInternal() {
        StringBuilder b = new StringBuilder(40);
        fillInName(b, true);
        return b.toString();
    }

    /**
     * Returns a string representation of this node.
     *
     * @return a string representing the node
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + getFullyQualifiedName() + '}';
    }

    void print() {
        System.out.println(getFullyQualifiedName());
        for (APINode k : children()) {
            k.print();
        }
    }
}
