package ca.cgjennings.ui.dnd;

import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 * A support class that allows interface components to easily accept files being
 * dropped on them from other components or from the platform.
 *
 * @author Adapted from public domain code by Robert Harder and Nathan Blomquist
 * @since 1.61
 */
public class FileDrop {

    private JComponent borderOwner;
    private Border oldBorder;
    private DropTargetListener dndListener;
    private FileFilter filter;
    private DropListener listener;

    private static final Logger log = Logger.getLogger(FileDrop.class.getPackage().getName());

    /**
     * Creates a new {@code FileDrop} on the component. If the component is a
     * {@code JComponent}, a {@link DropBorder} will appear when dragging files
     * over it.
     *
     * @param component the component that will accept files
     * @param borderOwner if non-{@code null}, a component whose border will
     * change to indicate that files are being dragged over the component
     * @param attachToDescendants if {@code true}, then all of the descendants
     * of the component will also listen for file drops
     */
    public FileDrop(Component component, JComponent borderOwner, boolean attachToDescendants) {
        this(component, borderOwner, attachToDescendants, null);
    }

    /**
     * Creates a new {@code FileDrop} on the component. If the component is a
     * {@code JComponent}, a {@link DropBorder} will appear when dragging files
     * over it.
     *
     * @param component the component that will accept files
     * @param listener the optional simple listener that will be notified when
     * files are dropped
     */
    public FileDrop(Component component, Listener listener) {
        this(component, (component instanceof JComponent) ? (JComponent) component : null, listener);
    }

    /**
     * Creates a new {@code FileDrop} on the component. If {@code borderOwner}
     * is not {@code null}, a {@link DropBorder} will appear when dragging files
     * over the component.
     *
     * @param component the component that will accept files
     * @param borderOwner if non-{@code null}, a component whose border will
     * change to indicate that files are being dragged over the component
     * @param listener the optional simple listener that will be notified when
     * files are dropped
     */
    public FileDrop(Component component, JComponent borderOwner, Listener listener) {
        this(component, borderOwner, false, listener);
    }

    /**
     * Creates a new {@code FileDrop} on the component. If the component is a
     * {@code JComponent}, a {@link DropBorder} will appear when dragging files
     * over it.
     *
     * @param component the component that will accept files
     * @param borderOwner if non-{@code null}, a component whose border will
     * change to indicate that files are being dragged over the component
     * @param attachToDescendants if {@code true}, then all of the descendants
     * of the component will also listen for file drops
     * @param listener the optional simple listener that will be notified when
     * files are dropped
     */
    public FileDrop(Component component, JComponent borderOwner, boolean attachToDescendants, final Listener listener) {
        Objects.requireNonNull(component, "component");

        this.borderOwner = borderOwner;
        setListener(listener);
        initDropListener();
        makeDropTarget(component, attachToDescendants);
    }

    /**
     * Creates a new {@code FileDrop} on the specified components. The drop will
     * initially have no listener attached.
     *
     * @param borderOwner if non-{@code null}, a component whose border will
     * change to indicate that files are being dragged over the component
     * @param components the components that will accept files
     * @see #setListener
     */
    public FileDrop(JComponent borderOwner, Component[] components) {
        Objects.requireNonNull(components, "component");
        if (components.length == 0) {
            throw new IllegalArgumentException("empty component list");
        }

        this.borderOwner = borderOwner;
        initDropListener();
        for( Component c : components) {
            makeDropTarget(c, false);
        }
    }

    /**
     * Creates and returns a new {@code FileDrop} on the specified components.
     * The returned {@code FileDrop} will have no listener attached, but is can
     * be set using builder-style method chaining.
     * 
     * <p>
     * The method was added to deal with two issues:
     * First, when this form was provided as a constrctor, it broke scripts as
     * the script engine could not distinguish which overload to choose.
     * Second, it avoids some compiler warnings as a common patern is to create
     * a {@code FileDrop} in a constructor without storing a reference to it.
     * 
     * @param borderOwner if non-{@code null}, a component whose border will
     * change to indicate that files are being dragged over the component
     * @param components the components that will accept files
     * @see #setListener
     */
    public static FileDrop of(JComponent borderOwner, Component... components) {
        return new FileDrop(borderOwner, components);
    }

    /**
     * Sets the drop listener to a simple listener that receives a non-null,
     * non-empty array of files. Only one listener may be attached to the
     * {@link FileDrop}.
     *
     * @param simpleListener the listener, or null to stop receiving events
     */
    public final void setListener(final Listener simpleListener) {
        if (simpleListener == null) {
            listener = null;
        } else {
            this.listener = new DropListener() {
                @Override
                public void filesDropped(DropEvent dropEvent) {
                    simpleListener.filesDropped(dropEvent.files.toArray(File[]::new));
                }
            };
        }
    }

    /**
     * Sets the drop listener to receive detailed information about dropped
     * files. Only one listener may be attached to the {@link FileDrop}.
     *
     * @param complexListener the listener, or null to stop receiving events
     */
    public void setListener(final DropListener complexListener) {
        listener = complexListener;
    }

    private void initDropListener() {
        dndListener = new DropTargetListener() {
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
                        if (!fileList.isEmpty()) {
                            if (listener != null) {
                                Component target = null;
                                if (evt.getSource() instanceof Component) {
                                    target = (Component) evt.getSource();
                                }
                                listener.filesDropped(new DropEvent(fileList, target, evt.getLocation()));
                            }
                        }
                        evt.getDropTargetContext().dropComplete(true);
                    } else {
                        evt.rejectDrop();
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
    }

    private void makeDropTarget(final java.awt.Component c, boolean recursive) {
        final java.awt.dnd.DropTarget dt = new java.awt.dnd.DropTarget();
        try {
            dt.addDropTargetListener(dndListener);
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
                new java.awt.dnd.DropTarget(c, dndListener);
                log.finest("drop target added to component");
            }
        });
        if (c.getParent() != null) {
            new java.awt.dnd.DropTarget(c, dndListener);
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
     * @return {@code true} if files are being dropped
     */
    private boolean canAcceptDrag(final java.awt.dnd.DropTargetDragEvent evt) {
        java.awt.datatransfer.DataFlavor[] flavors = evt.getCurrentDataFlavors();
        if ((evt.getSourceActions() & DnDConstants.ACTION_COPY) == 0) {
            return false;
        }

        for (int i = 0; i < flavors.length; ++i) {
            final DataFlavor curFlavor = flavors[i];
            if (curFlavor.equals(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the file filter for this drop handler. When set, dropped files will
     * be filtered before calling the drop listener.
     *
     * @param fileFilter the filter to apply, or {@code null} to accept all
     * files
     */
    public void setFileFilter(FileFilter fileFilter) {
        filter = fileFilter;
    }

    /**
     * Returns the file filter for this drop handler.
     *
     * @return the file filter for this drop handler, or {@code null}
     */
    public FileFilter getFileFilter() {
        return filter;
    }

    /**
     * Removes all drag-and-drop support from a component and optionally from
     * its descendants.
     *
     * @param component the top-level component to modify
     * @param recursive if {@code true}, apply recursively to all descendants
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
         * {@code FileDrop} target.
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

        private final List<File> files;
        private final Component target;
        private final Point location;

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
    @FunctionalInterface
    public interface Listener {

        /**
         * Called when one or more files are successfully dropped onto a
         * {@code FileDrop} target.
         *
         * @param files a non-{@code null} array of one or more files, all of
         * which are acceptable to the listener
         */
        void filesDropped(File[] files);
    }
}
