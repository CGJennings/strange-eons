package ca.cgjennings.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;

/**
 * A control that consists of a list of icon buttons for selecting from a group
 * of options. Its applications and uses are similar to a combo box, but suited
 * for a small list of fixed options that can be represented visually.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JIconSelector<S> extends JComponent {

    /**
     * Creates a new, empty button combo.
     */
    public JIconSelector() {
        super();

        MARGIN = UIManager.getLookAndFeel().getName().equals("Nimbus")
                ? new Insets(0, -6, 0, -6)
                : null;

        BoxLayout layout = new BoxLayout(this, BoxLayout.LINE_AXIS);
        setLayout(layout);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        // when the selector contains no items, this will give it a
        // default size so it can be manipulated in a GUI editor
        addDummySpacer();
    }

    private void addDummySpacer() {
        if (getComponentCount() == 0) {
            add(Box.createRigidArea(new Dimension(16, 16)));
        }
    }

    private void removeDummySpacer() {
        if (getComponentCount() == 1 && (getComponent(0) instanceof Box.Filler)) {
            remove(0);
        }
    }

    /**
     * Adds an item to the list, obtaining an icon and tool tip text from the
     * item itself. The specified item must implement the {@link IconProvider}
     * interface, or a {@code ClassCastException} will be thrown. The tool tip
     * text will be set to the item's {@code toString()} value.
     *
     * @param item the item to add
     */
    public void addItem(S item) {
        addItem(item, ((IconProvider) item).getIcon(), item.toString());
    }

    /**
     * Adds an item to the list, obtaining an icon and tool tip text from the
     * item itself. The specified item must implement the {@link IconProvider}
     * interface, or a {@code ClassCastException} will be thrown.
     *
     * @param item the item to add
     * @param toolTipText the tool tip text (may be {@code null})
     */
    public void addItem(S item, String toolTipText) {
        addItem(item, ((IconProvider) item).getIcon(), toolTipText);
    }

    /**
     * Adds an item to the list, using the specified icon and tool tip text to
     * represent it. If the item is already in the list, this has no effect.
     *
     * @param item the item to add to the list
     * @param icon the non-{@code null} icon for the item
     * @param toolTipText the tool tip text (may be {@code null})
     */
    public void addItem(S item, Icon icon, String toolTipText) {
        if (objects.contains(item)) {
            return;
        }

        removeDummySpacer();

        int index = objects.size();
        objects.add(item);

        JToggleButton button = new JToggleButton(icon);
        button.setMargin(MARGIN);
        button.setToolTipText(toolTipText);
        button.addActionListener(proxyListener);
        group.add(button);

        if (index > 0) {
            add(Box.createHorizontalStrut(6));
        }
        add(button);
    }

    /**
     * Removes the item from the list. Has no effect if the item is not in the
     * list.
     *
     * @param item the item to remove
     */
    public void removeItem(S item) {
        int index = objects.indexOf(item);
        if (index >= 0) {
            objects.remove(index);

            JToggleButton button = getButton(index);
            button.removeActionListener(proxyListener);
            group.remove(button);

            remove(index * 2);
            if (index > 0) {
                remove(index * 2 - 1);
            }

            addDummySpacer();
        }
    }

    /**
     * Returns the selected item in the list, or {@code null} if there is no
     * selection.
     *
     * @return the selected item
     */
    public S getSelectedItem() {
        int len = objects.size();
        for (int i = 0; i < len; ++i) {
            if (getButton(i).isSelected()) {
                return objects.get(i);
            }
        }
        return null;
    }

    /**
     * Sets the selected item. If {@code null}, the selection is cleared.
     *
     * @param item the item to select
     */
    public void setSelectedItem(S item) {
        if (item == null) {
            group.clearSelection();
        } else {
            int i = objects.indexOf(item);
            if (i >= 0) {
                getButton(i).setSelected(true);
            }
        }
    }

    protected JToggleButton getButton(int index) {
        return (JToggleButton) getComponent(index * 2);
    }

    private final LinkedList<S> objects = new LinkedList<>();
    private final ButtonGroup group = new ButtonGroup();

    /**
     * Adds an {@code ActionListener} to the list.
     *
     * @param l the {@code ActionListener} to be added
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes an {@code ActionListener} from the list.
     *
     * @param l the listener to be removed
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    /**
     * Notifies all listeners that have registered interest for notification on
     * this event type. The event instance is lazily created using the
     * {@code event} parameter.
     *
     * @param event the {@code ActionEvent} object
     * @see EventListenerList
     */
    protected void fireActionPerformed(ActionEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                // Lazily create the event:
                if (e == null) {
                    e = new ActionEvent(JIconSelector.this, ActionEvent.ACTION_PERFORMED, event.getActionCommand(), event.getWhen(), event.getModifiers());
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    private final ActionListener proxyListener = this::fireActionPerformed;

//	public static void main(String[] args) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				try {
//					UIManager.setLookAndFeel( new NimbusLookAndFeel() );
//				} catch( Exception e ){}
//				JFrame f = new JFrame("test");
//				f.setSize( 200, 200 );
//				f.setLocationRelativeTo( null );
//				f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//
//				final JIconSelector jbc = new LineJoinSelector();
////				jbc.addItem( "+", new PaintIcon( Color.BLACK ), "Plus" );
////				jbc.addItem( "-", new PaintIcon( Color.WHITE ), "Minus" );
////				jbc.addActionListener( new ActionListener() {
////					@Override
////					public void actionPerformed( ActionEvent e ) {
////						System.err.println( jbc.getSelectedItem() );
////					}
////				});
//
//				f.add( jbc );
//				f.setVisible( true );
//			}
//		});
//	}
    private final Insets MARGIN;
}
