package ca.cgjennings.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import resources.Language;

/**
 * A tree that displays the local file system folders, and supports selection of
 * one of these folders.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class FolderTree extends JTree {

    /**
     * Named property that can be listened for to detect selection changes.
     */
    public static final String FOLDER_SELECTED = "folderSelection";
    private File selectedFile;

    /**
     * Creates a new folder tree starting at the local file system root.
     */
    public FolderTree() {
        this(null);
    }

    /**
     * Creates a new folder tree starting from the specified root folder.
     *
     * @param root the file system root
     */
    public FolderTree(final File root) {
        init();
        createModel(root);
        new TreeLabelExposer(this);
    }

    /**
     * Returns the file system view used to obtain platform-specific folder
     * information.
     *
     * @return the file system view used by the tree
     */
    public FileSystemView getFileSystemView() {
        return fsv;
    }

    private void init() {
        setRootVisible(false);
        setCellRenderer(renderer);
        DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
        selModel.setSelectionMode(DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);
        setSelectionModel(selModel);
        addTreeSelectionListener((TreeSelectionEvent e) -> {
            File oldSelection = selectedFile;
            TreePath path = e.getPath();
            if (path != null) {
                Node n = (Node) path.getLastPathComponent();
                selectedFile = n.file;
            } else {
                selectedFile = null;
            }
            if (selectedFile == null) {
                if (oldSelection != null) {
                    fireSelectionChange(oldSelection, null);
                }
            } else {
                if (!selectedFile.equals(oldSelection)) {
                    fireSelectionChange(oldSelection, selectedFile);
                }
            }
        });
    }

    /**
     * Adds a listener for changes to the selected folder. This is a convenience
     * that adds a property change listener for the selection property.
     *
     * @param li adds a selected folder listener
     */
    public void addSelectionListener(PropertyChangeListener li) {
        addPropertyChangeListener(FOLDER_SELECTED, li);
    }

    /**
     * Removes a listener for changes to the selected folder. This is a
     * convenience that removes a property change listener for the selection
     * property.
     *
     * @param li adds a selected folder listener
     */
    public void removeSelectionListener(PropertyChangeListener li) {
        removePropertyChangeListener(FOLDER_SELECTED, li);
    }

    private void fireSelectionChange(File oldFile, File newFile) {
        firePropertyChange(FOLDER_SELECTED, oldFile, newFile);
    }

    /**
     * Returns the currently selected folder.
     *
     * @return a file representing the selected folder
     */
    public File getSelectedFolder() {
        TreePath path = getSelectionPath();
        if (path == null) {
            return fsv.getDefaultDirectory();
        }
        return ((Node) path.getLastPathComponent()).file;
    }

    /**
     * Sets the currently selected folder. If the parameter is {@code null}, a
     * default folder is used.
     *
     * @param f a file representing the folder to select
     * @see #getSelectedFolder()
     * @return true if a valid folder was selected
     */
    public boolean setSelectedFolder(File f) {
        if (f == null) {
            f = fsv.getDefaultDirectory();
        }

        TreePath path = getTreePathForFolder(f);
        if (path == null) {
            return false;
        }
        setSelectionPath(path);
        expandPath(path);
        scrollPathToVisible(path);
        return true;
    }

    /**
     * Returns a tree path for the specified file, or {@code null}. If there are
     * multiple tree paths that are equivalent to the specified file, there is
     * no guarantee as to which is selected.
     *
     * @param f the file to locate in the tree
     * @return the path to the file, or {@code null}
     */
    public TreePath getTreePathForFolder(File f) {
        if (!fsv.isFileSystem(f) || !fsv.isTraversable(f)) {
            return null;
        }
        // account for possible symbolic link loops
        LinkedList<Node> toCheck = new LinkedList<>();
        toCheck.add((Node) getModel().getRoot());
        Node match = searchForTreePath(toCheck, f);
        if (match == null) {
            return null;
        }
        LinkedList<Node> nodes = new LinkedList<>();
        while (match != null) {
            nodes.addFirst(match);
            match = match.parent;
        }
        return new TreePath(nodes.toArray());
    }

    private Node searchForTreePath(LinkedList<Node> toCheck, File f) {
        while (!toCheck.isEmpty()) {
            Node candidate = toCheck.removeFirst();
            File candidateFile = candidate.getFile();
            if (candidateFile != null && candidateFile.getAbsoluteFile().equals(f)) {
                return candidate;
            }
            if (candidateFile == null || isAncestor(candidateFile, f) || (candidate.parent.file == null) || (fsv.isTraversable(candidateFile) && !fsv.isFileSystem(candidateFile))) {
                Node[] kids = candidate.getChildren();
                if (kids != null) {
                    for (int i = kids.length - 1; i >= 0; --i) {
                        toCheck.addFirst(kids[i]);
                    }
                }
            }
        }
        return null;
    }

    private boolean isAncestor(File parent, File child) {
        child = child.getAbsoluteFile();

        while (child != null) {
            parent = parent.getAbsoluteFile();
            if (child.equals(parent)) {
                return true;
            }
            child = child.getParentFile();
        }
        return false;
    }

//	public TreePath _getTreePathForFolder( File f ) {
//		if( !f.isDirectory() ) return null;
//		f = fsv.createFileObject( f.getPath() );
//
//		// create a list of the files in the path of f, starting from the root
//		File target = f;
//		LinkedList<File> stack = new LinkedList<>();
//		while( target != null ) {
//			stack.push( target );
//			target = target.getParentFile();
//		}
//
//		// try to find the root of f in the tree
//		target = stack.pop();
//		Node parent = findInRoots( target );
//		if( parent == null ) return null;
//
//		// now we can build a tree path in two stages:
//		// (1) add all of the pseudofileystem nodes above the matching node
//		// (2) add the real file nodes from the matching node down
//
//		int fakeNodeCount = 0;
//		Node n = parent.parent;
//		while( n != null ) {
//			++fakeNodeCount;
//			n = n.parent;
//		}
//
//		Node[] path = new Node[ fakeNodeCount + stack.size() + 1 ];
//		n = parent.parent;
//		for( int i=fakeNodeCount-1; i>=0; --i ) {
//			path[i] = n;
//			n = n.parent;
//		}
//
//		int index = fakeNodeCount;
//
//		// we already found the root of the file; just add it
//		path[index++] = parent;
//
//		while( !stack.isEmpty() ) {
//			// find the next directory in the target path
//			target = stack.pop();
//			int c=0;
//			for( ; c<parent.getChildCount(); ++c ) {
//				if( target.equals( ((Node)parent.getChildAt( c ) ).file ) ){
//					break;
//				}
//			}
//			if( c == parent.getChildCount() ) {
//				return null;
//			}
//			parent = parent.getChildAt( c );
//			path[index++] = parent;
//		}
//		if( index != path.length ) throw new AssertionError();
//		return new TreePath( path );
//	}
//	// find a top level (parentless) file among the roots of the file
//	// system view --- since the root of the file system may consist of
//	// pseudofilesystems, we build a queue of places to search and
//	// recursively add all pseudofilsystems as we search. Once we
//	// hit a real file, we return the node if it matches the target,
//	// but don't search that node any deeper
//	private Node findInRoots( File f ) {
//		if( !f.exists() ) return null;
//		LinkedList<Node> rootQueue = new LinkedList<>();
//
//		Node root = (Node) getModel().getRoot();
//		Node match;
//		do {
//			match = searchRoot( root, f, rootQueue );
//			if( match != null ) return match;
//			root = rootQueue.isEmpty() ? null : rootQueue.removeFirst();
//		} while( root != null );
//		return null;
//	}
//	// check root's children to see if any matches f; in the meantime,
//	// if f is not a real folder (e.g., it is "Desktop"), add its children
//	// to the queue of roots to search
//	private Node searchRoot( Node root, File f, LinkedList<Node> rootQueue ) {
//		// is this the tree root placeholder node or a fake filesystem?
//		// if so, don't expect a match: add the kids instead and eventually
//		// we should hit the "real" files somewhere
//		boolean addToQueue = root.file == null || !fsv.isFileSystem( root.file ) || !fsv.isFileSystemRoot( root.file );
//		for( int i=0; i<root.getChildCount(); ++i ) {
//			Node child = root.getChildAt( i );
//			if( f.equals( child.file ) ) return child;
//			if( addToQueue && isAncestor( child.getFile(), f ) ) rootQueue.add( child );
//		}
//		return null;
//	}
    /**
     * Given a tree path to a node in the tree, returns the file represented by
     * the node.
     *
     * @param path the path to convert to a file
     * @return the file represented by the path, or {@code null}
     */
    public File getFolderForTreePath(TreePath path) {
        if (path == null) {
            return null;
        }
        return ((Node) path.getLastPathComponent()).getFile();
    }

    /**
     * Updates the child nodes of the specified tree path. This may be called if
     * the file structure changes.
     *
     * @param path the tree path to update
     */
    public void reloadChildren(TreePath path) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        Node n = (Node) path.getLastPathComponent();
        n.checkedForChildren = false;
        n.children = null;
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.nodeStructureChanged(n);
    }

    /**
     * Finds the child with the specified name that is a child node of the given
     * tree path.
     *
     * @param path the path to search
     * @param name the name to search for
     * @return the node in the path that matches the specified name, or
     * {@code null} if it is not found
     */
    public TreePath findChild(TreePath path, String name) {
        Node n = (Node) path.getLastPathComponent();
        File target = fsv.getChild(n.file, name);
        for (int i = 0; i < n.getChildCount(); ++i) {
            if (n.getChildAt(i).file.equals(target)) {
                DefaultTreeModel model = (DefaultTreeModel) getModel();
                TreePath tp = new TreePath(model.getPathToRoot(n.getChildAt(i)));
                return tp;
            }
        }
        return null;
    }

    /**
     * Returns an icon for the file. The default implementation returns the
     * standard system icon.
     *
     * @param n the node to locate an icon for
     * @return an icon for the node
     */
    protected Icon getIconForNode(Node n) {
        return fsv.getSystemIcon(n.file);
    }

    /**
     * Given an array of files, returns an array containing only those that you
     * wish to appear in the tree. The base class filters out hidden and
     * non-traversable files.
     *
     * @param source the parent
     * @return the children that should be displayed
     */
    protected File[] filterFolders(File[] source) {
        if (source == null) {
            return null;
        }
        LinkedList<File> kids = new LinkedList<>();
        for (File s : source) {
            if (fsv.isTraversable(s) && !s.isHidden() && !s.getName().startsWith(".") && s.exists()) {
                kids.add(s);
            }
        }
        if (kids.size() == source.length) {
            return source;
        }
        return kids.toArray(File[]::new);
    }

    /**
     * This is called just before new child nodes appear in the tree, and allows
     * subclasses a chance to set a user object on nodes. It is called after
     * {@link #filterFolders(java.io.File[])}.
     *
     * @param children the child nodes about to be added
     */
    protected void aboutToAddChildNodes(Node[] children) {
    }

    private void createModel(File root) {
        File[] roots;
        if (root == null) {
            roots = fsv.getRoots();
        } else {
            roots = new File[]{root};
        }
        Node tree = new Node(roots);
        DefaultTreeModel model = new DefaultTreeModel(tree, true);
        setModel(model);

        for (int i = 0; i < tree.getChildCount(); ++i) {
            expandPath(new TreePath(model.getPathToRoot(tree.getChildAt(i))));
        }
    }

    private FileFilter filter;
    protected static final FileSystemView fsv = FileSystemView.getFileSystemView();

    private DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (!(value instanceof Node)) {
                return this;
            }
            Node node = (Node) value;
            setEnabled(filter == null || filter.accept(node.file));
            Icon icon = getIconForNode(node);
            setIcon(icon == null ? getDefaultClosedIcon() : icon);
            return this;
        }
    };

    /**
     * Class of nodes in the folder tree.
     */
    protected class Node implements TreeNode {

        protected File file;
        public boolean checkedForChildren;
        public boolean hasChildren;
        private Node[] children;
        protected Node parent;

        /**
         * Subclasses of FolderTree can use this as they see fit.
         */
        public Object userObject;

        public Node(File[] roots) {
            roots = filterFolders(roots);
            Arrays.sort(roots, sorter);
            file = null;
            hasChildren = true;
            checkedForChildren = true;
            children = new Node[roots.length];
            parent = null;
            for (int i = 0; i < roots.length; ++i) {
                children[i] = new Node(this, roots[i]);
            }
            aboutToAddChildNodes(children);
        }

        public Node(File f) {
            parent = null;
            file = f;
            checkedForChildren = false;
            hasChildren = true;
        }

        private Node(Node parent, File f) {
            this(f);
            this.parent = parent;
        }

        @Override
        public Node getChildAt(int childIndex) {
            Node[] c = getChildren();
            if (childIndex < 0 || c == null || childIndex >= c.length) {
                throw new IndexOutOfBoundsException("no such child: " + childIndex);
            }
            return c[childIndex];
        }

        @Override
        public int getChildCount() {
            Node[] c = getChildren();
            return c == null ? 0 : c.length;
        }

        @Override
        public TreeNode getParent() {
            return parent;
        }

        @Override
        public int getIndex(TreeNode node) {
            Node[] c = getChildren();
            if (c == null) {
                return -1;
            }
            for (int i = 0; i < c.length; ++i) {
                if (c[i].equals(node)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public boolean getAllowsChildren() {
            return hasChildren;
        }

        @Override
        public boolean isLeaf() {
            return !hasChildren;
        }

        private Node[] getChildren() {
            if (hasChildren && !checkedForChildren) {
                checkedForChildren = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    File[] kids = filterFolders(fsv.getFiles(file, true));
                    Arrays.sort(kids, sorter);
                    if (kids != null && kids.length > 0) {
                        children = new Node[kids.length];
                        for (int i = 0; i < kids.length; ++i) {
                            children[i] = new Node(this, kids[i]);
                        }
                        aboutToAddChildNodes(children);
                        hasChildren = true;
                    } else {
                        hasChildren = false;
                    }
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
            return children;
        }

        @Override
        public Enumeration<Node> children() {
            return new Enumeration<Node>() {
                Node[] children = getChildren().clone();
                int i = 0;

                @Override
                public boolean hasMoreElements() {
                    return i < children.length;
                }

                @Override
                public Node nextElement() {
                    return children[i++];
                }
            };
        }

        @Override
        public String toString() {
            return file == null ? "<root>" : fsv.getSystemDisplayName(file);
        }

        public final File getFile() {
            return file;
        }
    }

//	public static void main( String[] args ) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				JFrame d = new JFrame();
//				FolderTree t = new FolderTree();
//				d.add( new JScrollPane(t));
//				d.pack();
//				d.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//				d.setLocationRelativeTo( null );
//				t.addSelectionListener( new PropertyChangeListener() {
//					@Override
//					public void propertyChange( PropertyChangeEvent evt ) {
//						System.err.println( evt.getNewValue() );
//					}
//				});
//				d.setVisible( true );
//			}
//		});
//	}
    private Comparator<File> sorter = new Comparator<File>() {
        private int classify(File f) {
            int cl = 0;
            if (!fsv.isFileSystem(f)) {
                cl = 1;
            }
            if (fsv.isComputerNode(f) || fsv.isDrive(f)) {
                cl = 2;
            }
            if (fsv.isHiddenFile(f)) {
                cl += 10;
            }
            return cl;
        }

        @Override
        public int compare(File f1, File f2) {
            int c1 = classify(f1);
            int c2 = classify(f2);
            if (c1 == c2) {
                return Language.getInterface().getCollator().compare(f1.getName(), f2.getName());
            } else {
                return c1 - c2;
            }
        }
    };
};
