package ca.cgjennings.ui;

import ca.cgjennings.apps.arkham.MarkupTargetFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JTextField;
import resources.ResourceKit;

/**
 * A field for entering URLs, decorated to match the {@link JFileField}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class JURLField extends JTextField {

    public JURLField() {
        super(FilteredDocument.createFilePathDocument(), null, 0);
        new IconBorder(ResourceKit.getIcon("ui/controls/url-field.png")).install(this);
        MarkupTargetFactory.enableTargeting(this, false);
    }

    public URI getURI() {
        String t = getText().trim();
        if (!t.isEmpty()) {
            try {
                URI u = new URI(t);
                return u;
            } catch (URISyntaxException e) {
            }
        }
        return null;
    }

    public URL getURL() {
        URI u = getURI();
        if (u != null) {
            try {
                return u.toURL();
            } catch (MalformedURLException e) {
            }
        }
        return null;
    }

    public void setURI(URI u) {
        if (u == null) {
            setText(null);
            return;
        }
        setText(u.toString());
    }

    public void setURL(URL u) {
        if (u == null) {
            setText(null);
            return;
        }
        String t;
        try {
            t = u.toURI().toString();
        } catch (URISyntaxException ex) {
            t = u.toString();
        }
        setText(t);
    }
}
