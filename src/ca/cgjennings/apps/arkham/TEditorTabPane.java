package ca.cgjennings.apps.arkham;

import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.JCloseableTabbedPane;
import ca.cgjennings.ui.StyleUtilities;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.util.Collections;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.event.ChangeEvent;
import resources.ResourceKit;

/**
 * The tabbed pane that manages document switching when using the tabbed
 * document interface.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class TEditorTabPane extends JCloseableTabbedPane {

    private int minimumHeight;

    public TEditorTabPane() {
        //setUI( new BasicTabbedPaneUI() );
        addChangeListener((ChangeEvent e) -> {
            int i = getSelectedIndex();
            TAttachedEditor frame = editorFrameAt(i);
            if (frame != null) {
                try {
                    frame.setSelected(true);
                } catch (PropertyVetoException ex) {
                }
                frame.moveToFront();
            }
        });
        addTabClosingListener((JCloseableTabbedPane source, int tab, boolean isDirty) -> {
            TAttachedEditor frame = editorFrameAt(tab);
            if (frame != null) {
                if (frame.isClosed()) {
                    closeTab(tab);
                } else {
                    frame.doDefaultCloseAction();
                }
            }
        });
        setAutocloseEnabled(false);

        // temporarily add a fake tab to determine
        // the minimum height we should maintain
        TEditorTabLink sizingPanel = new TEditorTabLink(null);
        Icon sizingIcon = new ImageIcon(ResourceKit.getImage("editors/blank-editor-icon.png"));
        addTab("TEST", sizingIcon, sizingPanel);
        validate();
        minimumHeight = getPreferredSize().height;
        removeTabAt(0);

        // when the main app window is activated, we may be coming from
        // a detached window; check the selected tab and if it is detached,
        // try to find the document that is actually showing and activate its tab
        // this ensures that the correct editor will be targeted by commands
        AppFrame.getApp().addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent e) {
                int i = getSelectedIndex();
                if (i < 0) {
                    return;
                }
                TAttachedEditor ef = editorFrameAt(i);
                if (ef != null && !ef.isAttached()) {
                    // try to find an attached tab
                    JDesktopPane desktop = AppFrame.getApp().getDesktop();
                    JInternalFrame[] frames = desktop.getAllFrames();
                    TAttachedEditor targetFrame = null;
                    for (int j = 0; j < frames.length; ++j) {
                        TAttachedEditor candidate = (TAttachedEditor) frames[j];
                        if (candidate.isAttached()) {
                            targetFrame = candidate;
                            break;
                        }
                    }
                    if (targetFrame == null) {
                        setSelectedIndex(-1);
                    } else {
                        for (int j = 0; j < getTabCount(); ++j) {
                            if (editorFrameAt(j) == targetFrame) {
                                setSelectedIndex(j);
                            }
                        }
                    }
                }
            }
        });
    }
    private Font detachedFont;

    public Font getDetachedTabFont() {
        if (detachedFont == null) {
            Font f = getFont();
            f = f.deriveFont(Collections.singletonMap(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED));
            detachedFont = f;
        }
        return detachedFont;
    }

    private TAttachedEditor editorFrameAt(int index) {
        if (index < 0) {
            return null;
        }
        Component c = getComponentAt(index);
        if (c instanceof TEditorTabLink) {
            TEditorTabLink elp = (TEditorTabLink) c;
            TAttachedEditor frame = elp.getLinkedEditor();
            return frame;
        }
        return null;
    }

    @Override
    protected Component getDragImageRepresentativeComponent(int dragIndex) {
        TAttachedEditor f = editorFrameAt(dragIndex);
        if (f == null) {
            return null;
        }
        return f.getContentPane();
    }

    @Override
    protected JWindow createDragImage(int dragIndex) {
        JWindow w = super.createDragImage(dragIndex);
        if (w != null) {
            StyleUtilities.setWindowOpacity(w, 0.8F);
        }
        return w;
    }

    @Override
    protected Cursor getDragOutCursor() {
        if (dragOutCursor == null) {
            BufferedImage ci = ResourceKit.getImage("icons/ui/controls/detach-cursor.png");
            dragOutCursor = ImageUtilities.createCustomCursor(getToolkit(), ci, ci.getWidth() / 2, 0, "TabDetach");
        }
        return dragOutCursor; // Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR );
    }
    private Cursor dragOutCursor;

    @Override
    protected boolean isDragOutSupported() {
        return true;
    }

    @Override
    protected void handleDragOut(int dragIndex, Point locationOnScreen) {
        TAttachedEditor f = editorFrameAt(dragIndex);
        if (f != null) {
            if (f.isAttached()) {
                f.setAttached(false);
                TDetachedEditor ed = f.getDetachedEditor();
                Insets insets = ed.getInsets();
                ed.setLocation(locationOnScreen.x - insets.left, locationOnScreen.y - insets.top);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (d.height < minimumHeight) {
            d.height = minimumHeight;
        }
        return d;
    }

    @Override
    public JPopupMenu getComponentPopupMenu() {
        StrangeEonsEditor ed = AppFrame.getApp().getActiveEditor();
        if (ed != null) {
            return ed.getTabStripPopupMenu();
        }
        return null;
    }

}
