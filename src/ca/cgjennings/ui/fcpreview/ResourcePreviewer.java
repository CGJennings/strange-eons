package ca.cgjennings.ui.fcpreview;

import java.awt.image.BufferedImage;
import resources.ResourceKit;

/**
 * Previews images stored as application resources; pass a string containing the
 * resource path.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ResourcePreviewer extends ImagePreviewer {

    public ResourcePreviewer() {
        super();
    }

    @Override
    protected BufferedImage createPreviewImage(Object f) {
        if (f == null) {
            return null;
        }
        return ResourceKit.getImageQuietly((String) f);
    }

    @Override
    public boolean isResourceTypeSupported(Object o) {
        return o instanceof String;
    }
}
