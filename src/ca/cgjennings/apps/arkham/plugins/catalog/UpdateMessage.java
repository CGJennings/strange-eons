package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.dialog.Messenger;
import ca.cgjennings.platform.PlatformSupport;
import ca.cgjennings.ui.JLinkLabel;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * Creates and displays a {@linkplain Messenger message} that describes the
 * result of an update check.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
class UpdateMessage {

    /**
     * Used by subclasses to create custom update messages. This constructor
     * does not create or display anything; see
     * {@link #postMessage(javax.swing.JComponent[])}.
     */
    protected UpdateMessage() {
    }

    /**
     * Creates and displays an update message using the supplied links.
     * Typically, you would set a small icon on each link, and add an action
     * listener (or use the
     * {@linkplain JLinkLabel#setAutoFollowLinks(boolean) auto follow} feature)
     * to handle link clicks.
     *
     * @param updateLinks an array of update links for available updates
     */
    public UpdateMessage(JComponent[] updateLinks) {
        postMessage(updateLinks);
    }

    /**
     * Creates and displays an update message for the built-in update mechanism.
     *
     * @param applicationUpdate if {@code true}, creates a link for an
     * application update
     * @param pluginUpdate if {@code true} creates a link for plug-in updates
     * @param newPlugins if {@code true}, creates a link for newly available
     * plug-ins
     */
    public UpdateMessage(boolean applicationUpdate, boolean pluginUpdate, boolean newPlugins) {
        JLinkLabel appLink = createUpdateLink(
                "application/app.png",
                string("core-l-app-update"), string("core-l-app-update-false"),
                getDownloadURI(), applicationUpdate
        );
        JLinkLabel updateLink = createUpdateLink(
                "catalog/update-available.png",
                string("core-l-plugin-update"), string("core-l-plugin-update-false"),
                null, pluginUpdate
        );
        JLinkLabel newLink = createUpdateLink(
                "catalog/not-installed-new.png",
                string("core-l-new-plugins"), string("core-l-new-plugins-false"),
                null, newPlugins
        );

        if (AutomaticUpdater.getUpdateAction() != AutomaticUpdater.ACTION_TELL_USER) {
            updateLink.setVisible(false);
        }

        postMessage(new JComponent[]{appLink, updateLink, newLink});
    }

    private MouseListener linkListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            JLinkLabel link = (JLinkLabel) e.getSource();
            try {
                Window w = SwingUtilities.getWindowAncestor(link);
                if (w != null) {
                    w.setVisible(false);
                }

                if (!link.isEnabled()) {
                    return;
                }

                if (link.getURI() != null) {
                    link.followLink();
                } else {
                    new CatalogDialog(StrangeEons.getWindow()).setVisible(true);
                }
            } catch (IOException ex) {
                ErrorDialog.displayError(string("browser-err-pageload") + '\n' + link.getURI(), ex);
            }
        }
    };

    /**
     * Convenience method that creates a default update link control.
     *
     * @param iconResource an icon resource for the control
     * @param enableText the link text to display when enabled
     * @param disableText the link text to display when disabled
     * @param updatePage the URI to display when clicked; if {@code null}, opens
     * the plug-in catalog
     * @param enable whether the control should be enabled
     * @return a new link control with the specified properties
     */
    public JLinkLabel createUpdateLink(String iconResource, String enableText, String disableText, URI updatePage, boolean enable) {
        JLinkLabel updateLink = new JLinkLabel(enableText);
        if (iconResource != null) {
            updateLink.setIcon(ResourceKit.getIcon(iconResource));
        }
        updateLink.setAutoFollowLinks(false);
        updateLink.setURI(updatePage);
        if (!enable) {
            updateLink.setEnabled(false);
            updateLink.setText(disableText);
        }
        updateLink.addMouseListener(linkListener);
        return updateLink;
    }

    /**
     * Creates and displays a message using the specified update links. This can
     * be used by subclasses to create custom update messages.
     *
     * @param links the links to display
     */
    protected void postMessage(JComponent[] links) {
        if (links == null) {
            return;
        }
        for (JComponent link : links) {
            if (link != null && link.isEnabled()) {
                Messenger.displayMessage(
                        null, ResourceKit.getIcon("application/update.png"),
                        "", links
                );
                break;
            }
        }
    }

    /**
     * Returns a URI which can be loaded in a browser to display the download
     * page for the application.
     *
     * @return a download URI
     */
    public static URI getDownloadURI() {
        URI appURI = null;
        try {
            String os = PlatformSupport.PLATFORM_IS_WINDOWS ? "win"
                    : (PlatformSupport.PLATFORM_IS_MAC ? "osx" : "nix");
            String version = String.valueOf(StrangeEons.getBuildNumber());
            appURI = new URI(
                    "http://cgjennings.ca/eons/download/update.html?platform=" + os + "&version=" + version
            );
        } catch (URISyntaxException ex) {
            StrangeEons.log.log(Level.SEVERE, null, ex);
        }
        return appURI;
    }
}
