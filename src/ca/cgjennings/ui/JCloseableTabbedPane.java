package ca.cgjennings.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JComponent;

/**
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JCloseableTabbedPane extends JReorderableTabbedPane {

    public JCloseableTabbedPane() {
        this(TOP);
    }

    public JCloseableTabbedPane(int tabPlacement) {
        this(tabPlacement, WRAP_TAB_LAYOUT);
    }

    public JCloseableTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    int index = getTabIndex(e.getX(), e.getY());
                    if (index < 0) {
                        return;
                    }
                    if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
                        closeAllBut(-1);
                    } else if ((e.getModifiersEx() & (MouseEvent.ALT_DOWN_MASK | MouseEvent.META_DOWN_MASK)) != 0) {
                        closeAllBut(index);
                    } else {
                        fireTabClosing(index, isDirty(index));
                    }
                } else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    editTitle(getTabIndex(e.getX(), e.getY()));
                }
            }
        });
    }

    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        if (isTabClosable(title, icon, component, tip, index)) {
            super.setTabComponentAt(index, new TabCloseComponent(this));
        }
    }

    public void editTitle(int i) {
    }

    @Override
    public void setFont(Font f) {
        super.setFont(f);
        for (int i = 0; i < getTabCount(); ++i) {
            Component c = getTabComponentAt(i);
            if (c != null && c instanceof TabCloseComponent) {
                c.setFont(f);
            }
        }
    }

    public void setTabForeground(int i, Color fg) {
        Component c = getTabComponentAt(i);
        if (c != null && c instanceof TabCloseComponent) {
            c.setForeground(fg);
        }
    }

    public void setTabFont(int i, Font f) {
        Component c = getTabComponentAt(i);
        if (c != null && c instanceof TabCloseComponent) {
            c.setFont(f);
        }
    }

    /**
     * Override this method to control which tabs will have a close button
     * added. It will be called when the tab is first added to the pane.
     *
     * @param title the title of the tested tab
     * @param icon the icon of the tested tab
     * @param component the cxontents of the tested tab
     * @param tip the tool tip text of the tested tab
     * @param index the index of the tested tab
     * @return {@code true} if the tab should be closable
     */
    protected boolean isTabClosable(String title, Icon icon, Component component, String tip, int index) {
        return true;
    }

    public void fireTabClosing(int tab, boolean isDirty) {
        if (!isTabClosable(tab)) {
            return;
        }
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TabClosingListener.class) {
                ((TabClosingListener) listeners[i + 1]).tabClosing(this, tab, isDirty);
            }
        }
        if (isAutocloseEnabled()) {
            closeTab(tab);
        }
    }

    @Override
    public void setIconAt(int index, Icon icon) {
        super.setIconAt(index, icon);
    }

    @Override
    public void setTitleAt(int index, String title) {
        super.setTitleAt(index, title);
    }

    public void addTabClosingListener(TabClosingListener li) {
        listenerList.add(TabClosingListener.class, li);
    }

    public void removeTabClosingListener(TabClosingListener li) {
        listenerList.remove(TabClosingListener.class, li);
    }

    public boolean isAutocloseEnabled() {
        return autocloseEnabled;
    }

    public void setAutocloseEnabled(boolean autocloseEnabled) {
        this.autocloseEnabled = autocloseEnabled;
    }

    private boolean autocloseEnabled = true;

    public boolean isDirty(int index) {
        Component c = getTabComponentAt(index);
        if (c == null || !(c instanceof TabCloseComponent)) {
            return false;
        }
        return ((TabCloseComponent) c).isDirty();
    }

    public void setDirty(int index, boolean dirty) {
        ((TabCloseComponent) getTabComponentAt(index)).setDirty(dirty);
    }

    /**
     * Fire a tab closing event for each tab in reverse order, except the tab at
     * {@code index}. If autoclosing is enabled, each tab will be closed
     * after its event fires.
     *
     * @param index
     */
    void closeAllBut(int index) {
        for (int i = getTabCount() - 1; i >= 0; --i) {
            if (i == index) {
                continue;
            }
            if (isTabClosable(i)) {
                fireTabClosing(i, isDirty(i));
            }
        }
    }

    private boolean isTabClosable(int index) {
        return isTabClosable(getTitleAt(index), getIconAt(index), getComponentAt(index), getToolTipTextAt(index), index);
    }

    /**
     * Close a tab without firing an event, as long as {@link #isTabClosable}
     * returns {@code true}. This allows you to close a tab voluntarily in
     * response to a tab closing event.
     *
     * @param index the tab to close
     */
    public void closeTab(int index) {
        if (isTabClosable(index)) {
            removeTabAt(index);
        }
    }

    @Override
    public void revalidate() {
        super.revalidate();
        for (int i = 0; i < getTabCount(); ++i) {
            JComponent tc = (JComponent) getTabComponentAt(i);
            if (tc != null) {
                tc.revalidate();
            }
        }
    }

//	public static void main( String[] args ) {
//		EventQueue.invokeLater( new Runnable() {
//			@Override
//			public void run() {
//				JFrame f = new JFrame();
//				JCloseableTabbedPane tab = new JCloseableTabbedPane();
//				tab.add( "Hello", new JLabel("FIRST") );
//				tab.add( "there", new JLabel("SECOND"));
//				tab.add( "Jim", new JLabel("THIRD"));
//				tab.setDirty( 2, true );
//				f.add(tab);
//				f.pack();
//				f.setLocationRelativeTo( null );
//				f.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
//				f.setVisible( true );
//			}
//		});
//	}
}
