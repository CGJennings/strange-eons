package ca.cgjennings.ui;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

/**
 * A {@code TreeBuilder} aids in the dynamic construction of a
 * {@link TreeModel}. It operates under the assumption that the tree is
 * constructed from two kinds of nodes: containers and leaves. For example, if
 * building a tree from a directory structure, directories would be containers
 * and other files would be leaves.
 *
 * @param <C> the type used to identify containers
 * @param <L> the type used to identify leaves
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class TreeBuilder<C, L> {

    private final HashMap<C, Container<C>> map = new HashMap<>();
    private final Container<C> root;

    /**
     * Creates a tree builder that will build on a root node for the specified
     * container.
     *
     * @param root the container that is the root of the tree
     */
    public TreeBuilder(C root) {
        this.root = new Container<>(root);
        map.put(root, this.root);
    }

    /**
     * Adds a new node to the tree as a child of the specified parent container.
     *
     * @param parent the container to place the node in
     * @param leaf the leaf to add; if {@code null} ensures that a node for
     * the parent exists
     */
    public void add(C parent, L leaf) {
        Container<C> node = getContainerNode(parent);
        node.add(new Leaf<>(leaf));
    }

    /**
     * Returns the node for the root of the tree. Once all of the desired nodes
     * have been added, call this method to get a root suitable for use with a
     * {@link DefaultTreeModel}.
     *
     * @return the tree root
     */
    public Container<C> getRootNode() {
        return root;
    }

    /**
     * Returns the node that represents the specified container. If no such node
     * has been previously requested, a node is created and stored to satisfy
     * future requests.
     *
     * @param container the container to create a node for
     * @return the container node
     */
    protected Container<C> getContainerNode(C container) {
        Container<C> node = map.get(container);
        if (node == null) {
            node = new Container<>(container);
            Container<C> parent = getContainerNode(getParentContainer(container));
            parent.add(node);
            map.put(container, node);
        }
        return node;
    }

    /**
     * Returns the container that is parent container for the specified
     * container. This is called when a new container node must be generated, in
     * order to determine which parent node to add the new container to.
     *
     * <p>
     * <b>Note:</b> To function correctly, the implementation must ensure that
     * for any valid input container, recursively calling this method will
     * eventually return the root container. For example, if the container type
     * is {@link File} and this method returns
     * {@code container.getParent()}, then any directory added to the tree
     * would have to be a direct or indirect child of the file used to create
     * the root.
     *
     * @param container the container to determine a parent for
     * @return the container that represents the parent of the specified
     * container
     */
    protected abstract C getParentContainer(C container);

    /**
     * The class used to represent container nodes in the tree.
     */
    @SuppressWarnings("serial")
    public static class Container<T> extends DefaultMutableTreeNode {

        public Container(T userObject) {
            super(userObject);
        }

        /**
         * Sorts the children of this container.
         *
         * @param allDescendants if {@code true}, the child containers are
         * also sorted, recursively
         */
        @SuppressWarnings("unchecked")
        public void sort(boolean allDescendants) {
            if (children == null) {
                return;
            }
            Collections.sort(children, (o1, o2) -> {
                if (o1 == null) {
                    return o2 == null ? 0 : -1;
                }
                if (o2 == null) {
                    return 1;
                }
                if (o1 instanceof Comparable) {
                    return ((Comparable) o1).compareTo(o2);
                }
                if (o1 instanceof Container) {
                    if (!(o2 instanceof Container)) {
                        return -1;
                    }
                } else if (o2 instanceof Container) {
                    if (!(o1 instanceof Container)) {
                        return 1;
                    }
                }
                return o1.toString().compareTo(o2.toString());
            });
            if (allDescendants) {
                for (Object n : children) {
                    if (n instanceof Container) {
                        ((Container) n).sort(true);
                    }
                }
            }
        }
    }

    /**
     * The class used to represent leaf nodes in the tree.
     */
    @SuppressWarnings("serial")
    public static class Leaf<T> extends DefaultMutableTreeNode {

        public Leaf(T userObject) {
            super(userObject, false);
        }
    }
}
