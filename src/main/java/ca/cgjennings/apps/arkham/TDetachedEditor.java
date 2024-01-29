package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.plugins.catalog.CatalogDialog;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * A top-level container for an editor that has been detached from the main
 * application window.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
class TDetachedEditor extends JFrame implements TrackedWindow {

    private TAttachedEditor editor;

    /**
     * Creates new form TDetachedEditor
     */
    public TDetachedEditor(AbstractStrangeEonsEditor editor) {
        super();
        //super( StrangeEons.getWindow(), false );
        initComponents();
        this.editor = editor;
        dummyDesktop.add(editor);
        Insets frameInsets = getInsets();
        setSize(
                editor.getWidth() + frameInsets.left + frameInsets.right,
                editor.getHeight() + frameInsets.top + frameInsets.bottom
        );
        editor.setMaximizable(true);
        try {
            editor.setMaximum(true);
        } catch (PropertyVetoException pve) {
            StrangeEons.log.log(Level.WARNING, null, pve);
        }
        setLocationRelativeTo(StrangeEons.getWindow());

        // set default icons, most likely to be replaced by specific frame icons
        setFrameIcon(editor.getFrameIcon());
        setTitle(editor.getTitle());

        updateInputMap();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Insets frameInsets = getInsets();
                TDetachedEditor.this.editor.setSize(getWidth() - (frameInsets.left + frameInsets.right), getHeight() - (frameInsets.top + frameInsets.bottom));
            }
        });

        installCatalogSearchHandler(this);
    }

    /**
     * Installs a window focus listener that checks to see if an eonscat: link
     * has been placed on the clipboard. When this is detected, the catalog
     * manager is opened and this is set as the filter text (without the
     * eonscat: prefix).
     */
    static void installCatalogSearchHandler(Window w) {
        w.addWindowFocusListener(CAT_LISTENER);
    }
    private static final WindowFocusListener CAT_LISTENER = new WindowFocusListener() {
        @Override
        public void windowGainedFocus(WindowEvent e) {
            if (CatalogDialog.getCatalogSearchClip() != null) {
                new CatalogDialog(AppFrame.getApp()).setVisible(true);
            }
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
        }
    };

    private void updateInputMap() {
        StrangeEonsAppWindow af = StrangeEons.getWindow();
        JMenuBar menuBar = af.getJMenuBar();
        for (int m = 0; m < menuBar.getMenuCount(); ++m) {
            JMenu menu = menuBar.getMenu(m);
            if (menu != null) {
                updateInputMap(menu);
            }
        }
    }

    private void updateInputMap(JMenu menu) {
        for (int i = 0; i < menu.getMenuComponentCount(); ++i) {
            Component c = menu.getMenuComponent(i);
            if (!(c instanceof JMenuItem)) {
                continue;
            }
            final JMenuItem item = (JMenuItem) c;
            if (item.getAccelerator() != null) {
                final String actionName = item.getText();
                getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(item.getAccelerator(), actionName);
                getRootPane().getActionMap().put(actionName, new AbstractAction(actionName) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        item.doClick(0);
                    }
                });
                if (item instanceof JMenu) {
                    updateInputMap((JMenu) item);
                }
            }
        }
    }

    /**
     * Returns the editor has been embedded within this frame.
     *
     * @return the formerly attached editor that is contained by this detached
     * window
     */
    public TAttachedEditor getEmbeddedEditor() {
        return editor;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dummyDesktop = new javax.swing.JDesktopPane();

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().add(dummyDesktop, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

	private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
            if (!editor.isAttached()) {
                editor.setAttached(true);
            }
            dispose();
	}//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDesktopPane dummyDesktop;
    // End of variables declaration//GEN-END:variables

    public void setFrameIcon(Icon icon) {
        if (icon == null) {
            currentIcon = null;
            setIconImages(AppFrame.getApplicationFrameIcons());
        } else if (icon != currentIcon) {
            currentIcon = icon;
            setIconImage(ca.cgjennings.graphics.ImageUtilities.iconToImage(icon));
        }
    }

    private Icon currentIcon;

    @Override
    public Icon getIcon() {
        return currentIcon;
    }
}