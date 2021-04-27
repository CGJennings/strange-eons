package ca.cgjennings.ui.dnd;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 * A support class that allows interface components to easily accept files being
 * dropped on them from the other components or from the platform.
 *
 * @author Adapted from public domain code by Robert Harder and Nathan Blomquist
 * @since 1.61
 */
public class FileDrop {

    private JComponent borderOwner;
    private Border oldBorder;
    private DropTargetListener dropListener;
    private FileFilter filter;

    private HashSet<DropListener> listeners = null;

    private static final Logger log = Logger.getLogger(FileDrop.class.getPackage().getName());

    /**
     * Creates a new <code>FileDrop</code> on the component. If the component is
     * a <code>JComponent</code>, a {@link DropBorder} will appear when dragging
     * files over it.
     *
     * @param component the component that will accept files
     * @param borderOwner if non-<code>null</code>, a component whose border
     * will change to indicate that files are being dragged over the component
     * @param attachToDescendants if <code>true</code>, then all of the
     * descendants of the component will also listen for file drops
     */
    public FileDrop(Component component, JComponent borderOwner, boolean attachToDescendants) {
        this(component, borderOwner, attachToDescendants, null);
    }

    /**
     * Creates a new <code>FileDrop</code> on the component. If the component is
     * a <code>JComponent</code>, a {@link DropBorder} will appear when dragging
     * files over it.
     *
     * @param component the component that will accept files
     * @param listener the listener that will be notified when files are dropped
     */
    public FileDrop(Component component, Listener listener) {
        this(component, (component instanceof JComponent) ? (JComponent) component : null, listener);
    }

    /**
     * Creates a new <code>FileDrop</code> on the component. If
     * <code>borderOwner</code> is not <code>null</code>, a {@link DropBorder}
     * will appear when dragging files over the component.
     *
     * @param component the component that will accept files
     * @param borderOwner if non-<code>null</code>, a component whose border
     * will change to indicate that files are being dragged over the component
     * @param listener the simple listener that will be notified when files are
     * dropped
     */
    public FileDrop(Component component, JComponent borderOwner, Listener listener) {
        this(component, borderOwner, false, listener);
    }

    /**
     * Creates a new <code>FileDrop</code> on the component. If the component is
     * a <code>JComponent</code>, a {@link DropBorder} will appear when dragging
     * files over it.
     *
     * @param component the component that will accept files
     * @param borderOwner if non-<code>null</code>, a component whose border
     * will change to indicate that files are being dragged over the component
     * @param attachToDescendants if <code>true</code>, then all of the
     * descendants of the component will also listen for file drops
     * @param listener an optional simple listener that will be notified when
     * files are dropped
     */
    public FileDrop(final Component component, JComponent borderOwner, boolean attachToDescendants, final Listener listener) {
        if (component == null) {
            throw new NullPointerException("component");
        }
//		if( listener == null ) throw new NullPointerException( "listener" );

        this.borderOwner = borderOwner;

        dropListener = new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent evt) {
                if (canAcceptDrag(evt)) {
                    if (FileDrop.this.borderOwner != null) {
                        JComponent bo = FileDrop.this.borderOwner;
                        oldBorder = bo.getBorder();
                        bo.setBorder(new DropBorder(bo));
                    }
                    evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
                    log.finest("dragEnter accepted");
                } else {
                    evt.rejectDrag();
                    log.finest("dragEnter rejected");
                }
            }

            @Override
            public void dragOver(java.awt.dnd.DropTargetDragEvent evt) {
            }

            @Override
            public void dragExit(java.awt.dnd.DropTargetEvent evt) {
                if (FileDrop.this.borderOwner != null) {
                    FileDrop.this.borderOwner.setBorder(oldBorder);
                }
            }

            @Override
            public void drop(java.awt.dnd.DropTargetDropEvent evt) {
                try {
                    java.awt.datatransfer.Transferable tr = evt.getTransferable();

                    // is it a file list?
                    if (tr.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                        log.finest("accepting javaFileListFlavor");
                        evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);

                        @SuppressWarnings("unchecked")
                        List<File> fileList = (List<File>) tr.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                        // filter the file list
                        for (int i = fileList.size() - 1; i >= 0; --i) {
                            File f = fileList.get(i);
                            if (f == null || (filter != null && !filter.accept(f))) {
                                fileList.remove(f);
                            }
                        }
                        if (fileList.size() > 0) {
                            if (listener != null) {
                                listener.filesDropped(fileList.toArray(new File[fileList.size()]));
                            }
                            if (listeners != null) {
                                DropEvent event = new DropEvent(fileList, component, evt.getLocation());
                            }
                        }
                        evt.getDropTargetContext().dropComplete(true);
                    } // not a file list, check for a reader
                    else {
                        DataFlavor[] flavors = tr.getTransferDataFlavors();
                        boolean handled = false;
                        for (DataFlavor flavor : flavors) {
                            if (flavor.isRepresentationClassReader()) {
                                log.finest("accepting reader");
                                evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                                Reader reader = null;
                                handled = true;
                                try {
                                    reader = flavor.getReaderForText(tr);
                                    File[] files = createFileArrayFromReader(new BufferedReader(reader));
                                    if (files != null && files.length > 0) {
                                        listener.filesDropped(files);
                                    }
                                    evt.getDropTargetContext().dropComplete(true);
                                }catch (IOException e) {
                                    log.log(Level.INFO, "could not covnvert reader to file list", e);
                                    handled = false;
                                } finally {
                                    if (reader != null) {
                                        try {
                                            reader.close();
                                        } catch (IOException e) {
                                            log.log(Level.WARNING, null, e);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                        if (!handled) {
                            evt.rejectDrop();
                        }
                    }
                } catch (java.awt.datatransfer.UnsupportedFlavorException | IOException ufe) {
                    log.log(Level.WARNING, null, ufe);
                    evt.rejectDrop();
                } finally {
                    if (FileDrop.this.borderOwner != null) {
                        FileDrop.this.borderOwner.setBorder(oldBorder);
                    }
                }
            }

            @Override
            public void dropActionChanged(java.awt.dnd.DropTargetDragEvent evt) {
                if (canAcceptDrag(evt)) {
                    evt.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY);
                } else {
                    evt.rejectDrag();
                }
            }
        };

        // create drop target(s) on the component and children
        makeDropTarget(component, attachToDescendants);
    }

    private File[] createFileArrayFromReader(BufferedReader r) throws IOException {
        List<File> list = new java.util.LinkedList<>();
        String line = null;
        while ((line = r.readLine()) != null) {
            try {
                // KDE seems to append '\0' to the end of the reader
                if (NULL_CHAR_STRING.equals(line) || line.isEmpty()) {
                    continue;
                }
                File f = new File(new java.net.URI(line));
                if (filter == null || filter.accept(f)) {
                    list.add(f);
                }
            } catch (java.net.URISyntaxException ex) {
                log.log(Level.WARNING, "exception converting local URI to file", ex);
            }
        }
        return list.toArray(new File[list.size()]);
    }
    private static String NULL_CHAR_STRING = "\0";

    private void makeDropTarget(final java.awt.Component c, boolean recursive) {
        final java.awt.dnd.DropTarget dt = new java.awt.dnd.DropTarget();
        try {
            dt.addDropTargetListener(dropListener);
        } catch (java.util.TooManyListenersException e) {
            log.log(Level.SEVERE, "attempt to attach file drop handler when another drop listener was attached", e);
        }

        // Listen for hierarchy changes and remove the drop target when the parent gets cleared out.
        c.addHierarchyListener((java.awt.event.HierarchyEvent evt) -> {
            java.awt.Component parent = c.getParent();
            if (parent == null) {
                c.setDropTarget(null);
                log.finest("drop target cleared from component");
            } else {
                new java.awt.dnd.DropTarget(c, dropListener);
                log.finest("drop target added to component");
            }
        });
        if (c.getParent() != null) {
            new java.awt.dnd.DropTarget(c, dropListener);
        }

        if (recursive && (c instanceof java.awt.Container)) {
            for (Component kid : ((java.awt.Container) c).getComponents()) {
                makeDropTarget(kid, true);
            }
        }
    }

    /**
     * Check if a drop consists of files (which can be accepted) or a foreign
     * data type (which can't).
     *
     * @param evt the event to check for acceptable incoming data flavors
     * @return <code>true</code> if files are being dropped
     */
    private boolean canAcceptDrag(final java.awt.dnd.DropTargetDragEvent evt) {
        java.awt.datatransfer.DataFlavor[] flavors = evt.getCurrentDataFlavors();

        for (int i = 0; i < flavors.length; ++i) {
            final DataFlavor curFlavor = flavors[i];
            if (curFlavor.equals(java.awt.datatransfer.DataFlavor.javaFileListFlavor)
                    || curFlavor.isRepresentationClassReader()) {
                return true;
            }
        }
        return false;
    }

    public void addListener(DropListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        if (listeners == null) {
            listeners = new HashSet<>();
        }
        listeners.add(listener);
    }

    public void removeListener(DropListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Sets the file filter for this drop handler. When set, dropped files will
     * be filtered before calling the drop listener.
     *
     * @param fileFilter the filter to apply, or <code>null</code> to accept all
     * files
     */
    public void setFileFilter(FileFilter fileFilter) {
        filter = fileFilter;
    }

    /**
     * Returns the file filter for this drop handler.
     *
     * @return the file filter for this drop handler, or <code>null</code>
     */
    public FileFilter getFileFilter() {
        return filter;
    }

    /**
     * Removes all drag-and-drop support from a component and optionally from
     * its descendants.
     *
     * @param component the top-level component to modify
     * @param recursive if <code>true</code>, apply recursively to all
     * descendants
     */
    public static void remove(java.awt.Component component, boolean recursive) {
        component.setDropTarget(null);
        if (recursive && (component instanceof java.awt.Container)) {
            for (Component kid : ((java.awt.Container) component).getComponents()) {
                remove(kid, true);
            }
        }
    }

    /**
     * Listener that may be registered to receive {@link DropEvent}s when files
     * are dropped on a target component.
     *
     * @since 3.0
     */
    public interface DropListener {

        /**
         * Called when one or more files are successfully dropped onto a
         * <code>FileDrop</code> target.
         *
         * @param dropEvent an event object that provides details about the drop
         */
        void filesDropped(DropEvent dropEvent);
    }

    /**
     * Event that is provided to listeners when files are dropped on a target.
     *
     * @since 3.0
     */
    public static final class DropEvent {

        private List<File> files;
        private Component target;
        private Point location;

        private DropEvent(List<File> files, Component target, Point location) {
            this.files = Collections.unmodifiableList(files);
            this.target = target;
            this.location = location;
        }

        /**
         * Returns a list of the files that were dropped.
         *
         * @return the dropped files
         */
        public List<File> getFiles() {
            return files;
        }

        /**
         * Returns the component that the files were dropped on.
         *
         * @return the drop target
         */
        public Component getDropTarget() {
            return target;
        }

        /**
         * Returns the location of the drop within the drop target.
         *
         * @return the drop location, in the target component's coordinate space
         */
        public Point getDropLocation() {
            return (Point) location.clone();
        }
    }

    /**
     * A simpler listener that may be used as an alternative to
     * {@link DropListener}.
     *
     * @since 1.61
     */
    public interface Listener {

        /**
         * Called when one or more files are successfully dropped onto a
         * <code>FileDrop</code> target.
         *
         * @param files a non-<code>null</code> array of one or more files, all
         * of which are acceptable to the listener
         */
        void filesDropped(File[] files);
    }
}
