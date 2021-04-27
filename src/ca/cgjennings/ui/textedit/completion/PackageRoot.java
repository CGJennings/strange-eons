package ca.cgjennings.ui.textedit.completion;

/**
 * The mutable root of a tree of package and class information.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 * @see APIDatabase#getPackageRoot()
 */
public final class PackageRoot extends APINode {

    /**
     * Creates a new package root. Package roots have an empty name.
     */
    public PackageRoot() {
        super("");
    }

    /**
     * Adds an entry in the tree for a class or package with the specified fully
     * qualified name. If the entry already exists, no change is made. Note that
     * this assumes that the specified name follows the standard Java naming
     * convention: packages must start with lower case letters, and classes must
     * start with upper case letters (or other valid identifier characters).
     *
     * @param fullyQualifiedPackageOrClass the name of the class or package to
     * add, such as <code>"java.lang.String"</code>
     */
    public APINode addName(String fullyQualifiedPackageOrClass) {
        if (fullyQualifiedPackageOrClass == null) {
            throw new NullPointerException("fullyQualifiedPackageOrClass");
        }
        if (fullyQualifiedPackageOrClass.isEmpty()) {
            return this;
        }

        String[] names = fullyQualifiedPackageOrClass.split("\\.|\\/");
        APINode node = this;
        for (String name : names) {
            node = node.add(name);
        }
        return node;
    }

    /**
     * Returns the node for the specified name, if it exists in the database.
     *
     * @return the node for the fully qualified name, if available
     */
    public APINode getName(String fullyQualifiedPackageOrClass) {
        if (fullyQualifiedPackageOrClass == null) {
            throw new NullPointerException("fullyQualifiedPackageOrClass");
        }
        if (fullyQualifiedPackageOrClass.isEmpty()) {
            return this;
        }

        String[] names = fullyQualifiedPackageOrClass.split("\\.|\\/");
        APINode node = this;
        for (String name : names) {
            node = node.find(name);
            if (node == null) {
                break;
            }
        }
        return node;
    }

    /**
     * Returns the API node for the requested package, or <code>null</code> if
     * it is not in the database.
     *
     * @param pkg the package to look up a name for
     * @return the node for the package, or <code>null</code>
     */
    public PackageNode get(Package pkg) {
        return (PackageNode) getName(pkg.getName());
    }

    /**
     * Returns the API node for the requested class, or <code>null</code> if it
     * is not in the database.
     *
     * @param klass the class to look up a name for
     * @return the node for the class, or <code>null</code>
     */
    public ClassNode get(Class klass) {
        if (klass == null) {
            throw new NullPointerException("klass");
        }
        ClassNode node = null;
        try {
            node = (ClassNode) getName(klass.getName());
            if (node == null) {
                // class was not in API database; if the class exists, we
                // can add it dynamically
                String name = klass.getCanonicalName();
                if (name != null) {
                    node = (ClassNode) addName(name);
                }
            }
        } catch (Exception e) {
            // class does not exist
        }
        return node;
    }
}
