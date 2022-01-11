package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.commands.Commands;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import resources.AcceleratorTable;
import resources.ResourceKit;

/**
 * A label that displays a help icon and can open a help page when clicked. For
 * a help page in the SE3 docs, use {@link #setWikiPage} to set the location.
 * Otherwise, use {@link #setHelpPage} and provide the full URL.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JHelpButton extends JLabel {

    private String helpPage;
    private static final Icon icon = ResourceKit.getIcon("application/help.png");
    private static final Icon moicon = ResourceKit.getIcon("application/help-hi.png");
    private boolean fontSet = false;

    public JHelpButton() {
        helpPage = "index.html";
        setIcon(icon);
        super.setText("");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final Color fg = UIManager.getColor(Theme.LINK_LABEL_FOREGROUND);
        setForeground(fg == null ? Color.BLUE : fg);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    openHelpPage();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setIcon(moicon);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setIcon(icon);
            }
        });
        KeyStroke helpKey = AcceleratorTable.getApplicationTable().get("app-help-item");
        if (helpKey != null) {
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(helpKey, "HELP");
            getActionMap().put("HELP", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Commands.HELP.actionPerformed(e);
                }
            });
        }
    }

    @Override
    public void setText(String text) {
        if (!fontSet && getFont() != null) {
            Font f = getFont();
            Map<TextAttribute, Object> m = new HashMap<>();
            m.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            f = f.deriveFont(m);
            setFont(f);
            fontSet = true;
        }
        super.setText(text);
    }

    public String getHelpPage() {
        return helpPage;
    }

    /**
     * Sets the page that this button links to. The value can either be a
     * complete http[s] URL or the base name of a Strange Eons doc page.
     *
     * @param helpPage a non-null, non-empty help page
     */
    public void setHelpPage(String helpPage) {
        this.helpPage = helpPage;
    }

    @Deprecated
    /**
     * @deprecated
     */
    public void setWikiPage(String pageTitle) {
        setHelpPage(pageTitle);
    }

    /**
     * Open the help page that has been set for this component. Subclasses may
     * override this to implement new help mediums.
     */
    public void openHelpPage() {
        Commands.HELP.actionPerformed(new ActionEvent(this, 0, helpPage));
    }

    @Override
    public Dimension getPreferredSize() {
        // improves selectability on touch devices
        Dimension d = super.getPreferredSize();
        if (d.width < 24) {
            d.width = 24;
        }
        return d;
    }
}
