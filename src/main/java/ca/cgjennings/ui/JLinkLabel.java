package ca.cgjennings.ui;

import ca.cgjennings.platform.DesktopIntegration;
import ca.cgjennings.ui.theme.Theme;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashMap;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * A clickable link label.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
@SuppressWarnings("serial")
public class JLinkLabel extends JLabel {

    public JLinkLabel() {
        super();
        init();
    }

    public JLinkLabel(Icon image) {
        super(image);
        init();
    }

    public JLinkLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
        init();
    }

    public JLinkLabel(String text) {
        super(text);
        init();
    }

    public JLinkLabel(String text, URI link) {
        super(text);
        init();
        setURI(link);
    }

    public JLinkLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
        init();
    }

    public JLinkLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
        init();
    }

    private void init() {
        final Color fg = UIManager.getColor(Theme.LINK_LABEL_FOREGROUND);
        setForeground(fg == null ? Color.BLUE : fg);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        HashMap<Attribute, Object> underline = new HashMap<>();
        underline.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        setFont(getFont().deriveFont(underline));
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    try {
                        if (autoFollowLinks && isEnabled()) {
                            followLink();
                        }
                    } catch (IOException ioe) {
                        getToolkit().beep();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
    }

    private URI linkTarget;
    private boolean autoFollowLinks = true;

    public void setURI(URI link) {
        linkTarget = link;
    }

    public URI getURI() {
        return linkTarget;
    }

    public void followLink() throws IOException {
        URI link = linkTarget;
        if (link == null) {
            try {
                link = new URI(getText());
            } catch (URISyntaxException e) {
            }
        }
        if (canFollowLinks() && link != null) {
            DesktopIntegration.browse(link, this);
        }
    }

    public boolean canFollowLinks() {
        return DesktopIntegration.BROWSE_SUPPORTED;
    }

    public boolean getAutoFollowLinks() {
        return autoFollowLinks;
    }

    public void setAutoFollowLinks(boolean autoFollowLinks) {
        this.autoFollowLinks = autoFollowLinks;
    }
}
