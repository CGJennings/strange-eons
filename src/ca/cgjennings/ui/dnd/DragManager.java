package ca.cgjennings.ui.dnd;

import ca.cgjennings.platform.PlatformSupport;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * The drag manager is the core of the draggable token framework. This is a
 * small set of classes that can be used to implement drag-and-drop
 * functionality independently of AWT/Swing drag support. It can therefore
 * display drag operations in a way that is consistent across platforms, and
 * also avoids platform-specific drag bugs. However, unlike AWT/Swing, this
 * framework does not interact with the native platform's drag support. It is
 * only intended to be used to drag objects within a single virtual machine.
 *
 * <p>
 * To use the framework, you must implement a {@link DragHandler} and
 * {@link DropHandler} to provide the logic necessary to select an object when a
 * drag starts and do something with it when it is dropped. (Both can be
 * implemented in a single object using {@link AbstractDragAndDropHandler}.)
 * Then create a manager for the handlers and add the source components that be
 * dragged from and the target components that can be dragged to. In the
 * simplest case, you simply want the user to be able to drag objects from one
 * component and drop them on another. In this case, the drag handler detects
 * the object being dragged and packages it as a {@link DragToken}, and the drop
 * handler fetches the object from the drag token and adds it to the target
 * container.
 *
 * @param T the type of object to be dragged via this manager
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class DragManager<T> {
    // the user-supplied handlers; set at construction

    private DragHandler<T> dragHandler;
    private DropHandler<T> dropHandler;

    // the sets of components managed by this instance that can be dragged from or to
    private HashSet<JComponent> sources = new HashSet<>(4);
    private HashSet<JComponent> targets = new HashSet<>(4);

    // shared listener used to handle drags on all source components
    private GestureHandler gestureHandler = new GestureHandler();

    // user configurable properties
    private Cursor dropCursor = DragSource.DefaultMoveDrop;
    private Cursor noDropCursor = Cursor.getDefaultCursor();
    private boolean tokenVisible = !PlatformSupport.PLATFORM_IS_OSX;

    {
        if (PlatformSupport.PLATFORM_IS_OSX) {
            dropCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            noDropCursor = dropCursor;
        }
    }

    // info about the current ("active") drag
    private JComponent aSource; // active drag source
    private Point aStart; // start point of drag, relative to the source; null if drag cancelled or inactive
    private boolean aWasAutoscrolls; // tracks whether source was autoscrolling
    private Cursor aWasCursor; // original cursor at drag start
    private JComponent aTarget; // active drop target, when drag is over one
    private DragToken aToken; // token being dragged
    private DragWindow aWindow; // window displaying the token

    public DragManager(AbstractDragAndDropHandler<T> handler) {
        this(handler, handler);
    }

    public DragManager(DragHandler<T> dragHandler, DropHandler<T> dropHandler) {
        if (dragHandler == null) {
            throw new NullPointerException("dragHandler");
        }
        if (dropHandler == null) {
            throw new NullPointerException("dropHandler");
        }
        this.dragHandler = dragHandler;
        this.dropHandler = dropHandler;
    }

    /**
     * Adds a new source component in which the user can initiate drags.
     *
     * @param source the new drag source to add
     */
    public void addDragSource(JComponent source) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        if (sources.add(source)) {
            source.addMouseListener(gestureHandler);
            source.addMouseMotionListener(gestureHandler);
        }
    }

    /**
     * Removes a component from the set of drag sources.
     *
     * @param source the source to remove
     */
    public void removeDragSource(JComponent source) {
        if (sources.remove(source)) {
            source.removeMouseListener(gestureHandler);
            source.removeMouseMotionListener(gestureHandler);
        }
    }

    /**
     * Adds a new target that the user can drag dragged objects upon.
     *
     * @param target the target component to add
     */
    public void addDropTarget(JComponent target) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        targets.add(target);
    }

    /**
     * Removes a component from the set of drop targets.
     *
     * @param target the target to remove
     */
    public void removeDropTarget(JComponent target) {
        if (sources.remove(target)) {
            target.removeMouseListener(gestureHandler);
        }
    }

    /**
     * Returns the cursor displayed for the token when a drag is allowed.
     *
     * @return the drop cursor
     */
    public Cursor getDropCursor() {
        return dropCursor;
    }

    /**
     * Sets the cursor displayed for the token when a drag is allowed.
     *
     * @param dropCursor the new drop cursor to use
     */
    public void setDropCursor(Cursor dropCursor) {
        if (dropCursor == null) {
            dropCursor = Cursor.getDefaultCursor();
        }
        if (this.dropCursor != dropCursor) {
            this.dropCursor = dropCursor;
            if (aWindow != null) {
                aWindow.setDroppable(aWindow.isDroppable(), dropCursor, noDropCursor, true);
            }
        }
    }

    /**
     * Returns the cursor displayed for the token when a drag is not allowed.
     *
     * @return the drop cursor
     */
    public Cursor getNoDropCursor() {
        return noDropCursor;
    }

    /**
     * Sets the cursor displayed for the token when a drag is not allowed.
     *
     * @param noDropCursor the new cursor to use; <code>null</code> for default
     * cursor
     */
    public void setNoDropCursor(Cursor noDropCursor) {
        if (noDropCursor == null) {
            noDropCursor = Cursor.getDefaultCursor();
        }
        if (this.noDropCursor != noDropCursor) {
            this.noDropCursor = noDropCursor;
            if (aWindow != null) {
                aWindow.setDroppable(aWindow.isDroppable(), dropCursor, noDropCursor, true);
            }
        }
    }

    /**
     * Returns <code>true</code> if drag token images are displayed.
     *
     * @return <code>true</code> if the images associated with tokens are
     * visible
     */
    public boolean isTokenVisible() {
        return tokenVisible;
    }

    /**
     * Sets whether drag token images are displayed. Some platforms may not
     * support changing the token's visibility while a drag is active. On these
     * platforms, the method will only affect subsequent tokens and not the
     * currently displayed token.
     *
     * @param visible <code>true</code> if the images associated with tokens are
     * visible
     */
    public void setTokenVisible(boolean visible) {
        // On OS X, monkeying with the visibility can affect whether we get a RELEASED event
        if (PlatformSupport.PLATFORM_IS_OSX) {
            return;
        }

        if (visible != tokenVisible) {
            tokenVisible = visible;
            if (aWindow != null) {
                aWindow.setVisible(visible);
            }
        }
    }

    /**
     * Cancels the active drag operation, if any.
     */
    public void cancelDrag() {
        if (aSource != null) {
            aSource.setCursor(aWasCursor);
            aSource.setAutoscrolls(aWasAutoscrolls);
            aWasCursor = null;
            endDrag(null);
        }
    }

    /**
     * Ends a drag; if the specified target is <code>null</code>, the drag is
     * being cancelled, otherwise the drag completed with a drop on that target.
     *
     * @param target the drop target, or <code>null</code>
     */
    private void endDrag(JComponent target) {
        DragToken oToken = aToken;
        JComponent oSource = aSource;

        if (aWindow != null) {
            aWindow.dispose();
            aWindow = null;
        }
        aToken = null;
        aStart = null;
        aTarget = null;
        aSource = null;

        // if a drag has actually been started, inform the drag handler
        if (oToken != null) {
            dragHandler.dragFinished(this, oSource, oToken, target);
        }
    }

    /**
     * Finds a target for the current drag operation.
     *
     * @param location a location on the display
     * @return a drop target at the location willing to accept the active drag,
     * or <code>null</code>
     */
    private JComponent findTarget(Point location) {
        for (Window w : Window.getWindows()) {
            if (!w.isShowing() || w instanceof DragWindow) {
                continue;
            }
            Point windowRelative = fromScreen(location, w);
            Component c = SwingUtilities.getDeepestComponentAt(w, windowRelative.x, windowRelative.y);
            if (c == null) {
                continue;
            }
            if (targets.contains(c)) {
                JComponent jc = (JComponent) c;
                Point componentRelative = fromScreen(location, jc);
                if (dropHandler.acceptDrop(this, aSource, aToken, jc, componentRelative)) {
                    return jc;
                }
            }
        }
        return null;
    }

    private static Point fromScreen(Point location, Component targetSpace) {
        Point p = new Point(location);
        SwingUtilities.convertPointFromScreen(p, targetSpace);
        return p;
    }

    /**
     * Detects and handles drag gestures on registered source components.
     */
    private class GestureHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }

            Object src = e.getSource();
            if (!sources.contains(src)) {
                return;
            }

            e.consume();

            // for sanity, should be impossible:
            if (aSource != null) {
                cancelDrag();
            }

            // at this point, this is only a *potential* drag
            aSource = (JComponent) src;
            aWasAutoscrolls = aSource.getAutoscrolls();
            aWasCursor = aSource.getCursor();
            boolean isTarget = targets.contains(aSource);
            aSource.setCursor(isTarget ? dropCursor : noDropCursor);
            aSource.setAutoscrolls(aWasAutoscrolls && isTarget);
            aStart = e.getPoint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // aStart is cleared if the drag started but was cancelled; this can happen
            // if cancelDrag() is called while a drag is ongoing, for example, if the
            // the drag handler returns a null token to indicate an invalid drag
            //
            // the aSource check is just for sanity, it should not be possible
            if (aStart == null || aSource == null) {
                return;
            }

            e.consume();

            // if aToken is null, the drag has not started yet; check if the mouse
            // has moved far enough to start the drag
            if (aToken == null) {
                int dx = e.getX() - aStart.x;
                int dy = e.getY() - aStart.y;
                if (dx * dx + dy * dy > DRAG_THRESHOLD_SQ) {
                    // it was dragged far enough, ask if the drag spot has an associated token
                    aToken = dragHandler.createDragToken(DragManager.this, aSource, aStart);
                    // if not, the drag is effectively cancelled
                    if (aToken == null) {
                        cancelDrag();
                        return;
                    } // otherwise create the window that will display the token
                    else {
                        aWindow = new DragWindow(aSource, aToken);
                        aWindow.setVisible(tokenVisible);
                    }
                }
            }

            if (aWindow != null) {
                Point sp = e.getLocationOnScreen();
                aWindow.updateLocation(sp);

                JComponent newTarget = findTarget(sp);
                aWindow.setDroppable(newTarget != null, dropCursor, noDropCursor, false);
                aSource.setCursor(newTarget == null ? noDropCursor : dropCursor);

                Point targetRelative = null;
                if (newTarget != null) {
                    targetRelative = fromScreen(sp, newTarget);
                }
                if (aTarget != newTarget) {
                    if (aTarget != null) {
                        dropHandler.dragExit(DragManager.this, aSource, aToken, aTarget);
                    }
                    aTarget = newTarget;
                    if (aTarget != null) {
                        dropHandler.dragEnter(DragManager.this, aSource, aToken, aTarget, targetRelative);
                    }
                }
                if (aTarget != null) {
                    dropHandler.dragMove(DragManager.this, aSource, aToken, aTarget, targetRelative);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
//			if( e.getButton() != MouseEvent.BUTTON1 ) return;

            if (aSource != null) {
                aSource.setCursor(aWasCursor);
                aSource.setAutoscrolls(aWasAutoscrolls);
                aWasCursor = null;
            } else {
                return;
            }
            if (aStart == null) {
                return;
            }

            e.consume();

            try {
                if (aTarget != null) {
                    if (!dropHandler.handleDrop(
                            DragManager.this, aSource, aToken, aTarget,
                            SwingUtilities.convertPoint(aSource, e.getPoint(), aTarget)
                    )) {
                        // if handleDrop returned false, clear aTarget so that
                        // the drag is considered cancelled rather than completed
                        aTarget = null;
                    }
                }
            } finally {
                endDrag(aTarget);
            }
        }
    }

    // square of the platform-specific distance that the cursor must be
    // dragged before a drag gesture will be recognized
    private static final int DRAG_THRESHOLD_SQ;

    static {
        int dist = DragSource.getDragThreshold();
        dist *= dist;
        DRAG_THRESHOLD_SQ = Math.max(1, dist);
    }

    @Override
    public String toString() {
        return "DragManager{dragHandler=" + dragHandler + ", dropHandler=" + dropHandler + '}';
    }

//	public static void main(String[] args) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				try {
//					JFrame f = new JFrame();
//					f.setSize(100, 100);
//					f.setLayout( new FlowLayout() );
//					JLabel src = new JLabel("1");
//					final JLabel dst = new JLabel("2");
//					f.add(src);
//					f.add( new JLabel("<html> <br> ") );
//					f.add(dst);
//					f.setDefaultCloseOperation( f.EXIT_ON_CLOSE );
//					f.setLocationRelativeTo( null );
//					f.setVisible( true );
//					final BufferedImage bi = new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
//					AbstractDragAndDropHandler h = new AbstractDragAndDropHandler() {
//						@Override
//						public DragToken createDragToken( DragManager manager, JComponent dragSource, Point dragPoint ) {							
//							return new DragToken( ""+((int)(Math.random()*10d)), bi );
//						}
//						@Override
//						public void dragFinished( DragManager manager, JComponent dragSource, DragToken token, JComponent dropTarget ) {
//							manager.setTokenVisible( true );
//						}
//						@Override
//						public boolean handleDrop( DragManager manager, JComponent dragSource, DragToken token, JComponent dropTarget, Point location ) {
//							dst.setText( token.getObject().toString() );
//							return true;
//						}
//						@Override
//						public void dragEnter( DragManager manager, JComponent dragSource, DragToken token, JComponent dropTarget, Point location ) {
//							manager.setTokenVisible( false );
//							System.err.println("Enter: " + location );
//						}
//						@Override
//						public void dragExit( DragManager manager, JComponent dragSource, DragToken token, JComponent dropTarget ) {
////							manager.setTokenVisible( false );
//							System.err.println("Exit");
//						}
//
//						@Override
//						public void dragMove( DragManager manager, JComponent dragSource, DragToken token, JComponent dropTarget, Point location ) {
//							System.err.println("Move: " + location );
//						}						
//					};
//					DragManager m = new DragManager( h );
//					m.addDragSource(src);
//					m.addDropTarget(dst);
//				} catch( Throwable t ) {
//					t.printStackTrace();
//				}
//			}
//		});
//	}
}
