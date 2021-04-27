package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.StrangeEonsEditor.EditorListener;
import ca.cgjennings.graphics.ImageUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.border.Border;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.basic.BasicInternalFrameUI;

//
// IMPORTANT: When the StrangeEonsEditor specifies a method that is
// also defined by this class, it is critical that
// the JavaDoc for that method be copied from that interface or else
// the JavaDoc will be blank since it will attempt to get it from the
// (missing) Java API.
//
/**
 * A base class for Strange Eons editors that implements support for the tabbed
 * document style and detachable windows.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class TAttachedEditor extends JInternalFrame {

    private TEditorTabLink elp;
    static TEditorTabPane editorTab;

    public TAttachedEditor() {
        setUI(new BlankInternalFrameUI(this));
        setBorder(null);
        setResizable(false);
    }

    void setEditorLinkPanel(TEditorTabLink elp) {
        this.elp = elp;
        updateTab();
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                int tab = getEditorTabIndex();
                if (tab == -1) {
                    return;
                }
                if (editorTab.getSelectedIndex() != tab) {
                    editorTab.setSelectedIndex(tab);
                }
                // update command enable/disable states (e.g., so Ctrl+F works in new code editor)
                AppFrame.getApp().updateCommonCommandStates(false);
            }
        });
    }

    @Override
    public void doDefaultCloseAction() {
        super.doDefaultCloseAction();
        final int index = getEditorTabIndex();
        if (index >= 0) {
            editorTab.closeTab(index);
        }
    }

    @Override
    public final Border getBorder() {
        return none;
    }
    private static Border none = BorderFactory.createEmptyBorder();

    /**
     * Set the title used to describe this document in the tab strip or document
     * window (depending on program settings). The <code>title</code> value may
     * be <code>null</code>, which is equivalent to an empty title.
     *
     * @param title the text of the title to display
     */
    @Override
    public void setTitle(String title) {
        if (title == null) {
            title = "";
        }

        if (!title.equals(super.getTitle())) {
            super.setTitle(title);
            updateTab();
        }
        if (getDetachedEditor() != null) {
            getDetachedEditor().setTitle(title);
        }
    }

    /**
     * Returns the title used to describe this editor. The returned value is
     * never <code>null</code>; if a <code>null</code> title is set with
     * {@link #setTitle} then this method returns an empty string.
     *
     * @return the current title, which is guaranteed not to be
     * <code>null</code>
     */
    @Override
    public String getTitle() {
        String title = super.getTitle();
        if (title == null) {
            title = "";
        }
        return title;
    }

    /**
     * Sets the preferred icon to use for this editor window. This icon may be
     * used to represent the window in various parts of the application
     * interface. Possible example uses include the following: the document tab,
     * the editor's item in the Window menu, and the frame icon of the editor's
     * detached window. Note, however, that there is no guarantee that the icon
     * will be used, or how. Furthermore, the icon may be used in modified form.
     *
     * @param icon the preferred icon for the editor window
     * @see #getFrameIcon()
     */
    @Override
    public void setFrameIcon(Icon icon) {
        if (icon == currentIcon) {
            return;
        }
        currentIcon = icon;
        if (icon != null && icon.getIconHeight() < ICON_HEIGHT) {
            icon = ImageUtilities.createIconForSize(ImageUtilities.iconToImage(icon), ICON_HEIGHT);
        }
        attachedIcon = icon;
        detachedIcon = ImageUtilities.createGhostedIcon(icon);
        super.setFrameIcon(attachedIcon);
        if (getDetachedEditor() != null) {
            getDetachedEditor().setFrameIcon(attachedIcon);
        }
        updateTab();
    }
    private static final int ICON_HEIGHT = 24;
    // the true icon passed to the setter
    private Icon currentIcon;
    // the sized, normal version
    private Icon attachedIcon;
    // the sized, ghosted version
    private Icon detachedIcon;

    /**
     * Returns the editor window icon.
     *
     * @return the icon for the editor, or <code>null</code> if no icon is set
     */
    @Override
    public Icon getFrameIcon() {
        return currentIcon;
    }

    /**
     * Sets the tool tip text to display for the editor's tab.
     *
     * @param toolTipText the text to display, or <code>null</code> to clear the
     * tool tip
     */
    @Override
    public void setToolTipText(String toolTipText) {
        super.setToolTipText(toolTipText);
        updateTab();
    }

    /**
     * Returns the tool tip text displayed for the editor's tab.
     */
    @Override
    public String getToolTipText() {
        return super.getToolTipText();
    }

    final int getEditorTabIndex() {
        if (elp != null) {
            return editorTab.indexOfComponent(elp);
        }
        return -1;
    }

    public boolean hasUnsavedChanges() {
        return false;
    }

    final void updateTab() {
        int tab = getEditorTabIndex();
        if (tab >= 0) {
            final boolean dirty = hasUnsavedChanges();
            TDetachedEditor ded = getDetachedEditor();
            editorTab.setTitleAt(tab, getTitle());
            editorTab.setIconAt(tab, ded == null ? attachedIcon : detachedIcon);
            editorTab.setDirty(tab, dirty);
            editorTab.setToolTipTextAt(tab, getToolTipText());
            editorTab.setTabFont(tab, ded == null ? editorTab.getFont() : editorTab.getDetachedTabFont());
            if (ded != null) {
                ded.setTitle(getTitle());
                ded.setFrameIcon(getFrameIcon());
                ded.getRootPane().putClientProperty("windowModified", dirty);
            }
        }
    }

    private void fireEditorDetached() {
        final Object[] list = listenerList.getListenerList();
        final StrangeEonsEditor cast = (StrangeEonsEditor) this;
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorDetached(cast);
            }
        }
    }

    private void fireEditorAttached() {
        final Object[] list = listenerList.getListenerList();
        final StrangeEonsEditor cast = (StrangeEonsEditor) this;
        for (int i = 0; i < list.length; i += 2) {
            if (list[i] == EditorListener.class) {
                ((EditorListener) list[i + 1]).editorAttached(cast);
            }
        }
    }

    /**
     * Sets whether this editor is attached to the document tab strip. If
     * <code>true</code>, the editor is attached; this is the default state for
     * new editors. If <code>false</code>, the editor is detached from the tab
     * strip. When detached, the editor appears in its own floating window
     * separate from the main application window. The editor reattaches when the
     * window is closed.
     *
     * @param attach if <code>true</code>, attaches the window to the tab strip
     */
    public void setAttached(boolean attach) {
        if (attach) {
            if (detachedEditor != null) {
                AppFrame app = AppFrame.getApp();
                detachedEditor.dispose();
                app.stopTracking(detachedEditor);
                detachedEditor = null;
                // do reattachment
                AppFrame.getApp().getDesktop().add(this);
                int tab = getEditorTabIndex();
                if (tab >= 0) {
                    editorTab.setSelectedIndex(tab);
                }
                try {
                    // force it into maximizing
                    setMaximum(false);
                    setMaximum(true);
                } catch (PropertyVetoException pve) {
                    throw new AssertionError(pve);
                }
                AppFrame.getApp().getDesktop().setSelectedFrame(this);
                app.toFront();
                app.requestFocus();
                updateTab();
                fireEditorAttached();
            }
        } else {
            if (detachedEditor != null) {
                detachedEditor.toFront();
                detachedEditor.requestFocus();
            } else {
                // do detachment
                setVisible(false);
                AppFrame.getApp().getDesktop().remove(this);
                detachedEditor = new TDetachedEditor((AbstractStrangeEonsEditor) this);
                detachedEditor.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowActivated(WindowEvent e) {
                        // ensure that the tab for this editor is selected
                        int tab = getEditorTabIndex();
                        if (tab >= 0) {
                            editorTab.setSelectedIndex(tab);
                        }
                    }
                });
                setVisible(true);
                detachedEditor.setVisible(true);
                AppFrame.getApp().startTracking(detachedEditor);
                updateTab();
                fireEditorDetached();
            }
        }
    }

    /**
     * Returns <code>true</code> if this editor is currently attached to the
     * document tab strip.
     *
     * @return <code>true</code> if the editor is attached
     * @see #setAttached(boolean)
     */
    public boolean isAttached() {
        return detachedEditor == null;
    }

    final TDetachedEditor getDetachedEditor() {
        return detachedEditor;
    }
    private TDetachedEditor detachedEditor = null;

    /**
     * A UI for an internal frame without the frame parts; used to create a
     * frame that covers the entire desktop with no window decorations when
     * using the tabbed document style.
     */
    private static class BlankInternalFrameUI extends BasicInternalFrameUI {

        public BlankInternalFrameUI(JInternalFrame b) {
            super(b);
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            setNorthPane(null);
            setSouthPane(null);
            setEastPane(null);
            setWestPane(null);
        }
    }
}
