package ca.cgjennings.ui.theme;

import static ca.cgjennings.apps.arkham.project.MetadataSource.ICON_FILE;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

/**
 * Implements the {@code ThemedIcon} interface for any icon.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
final class ForeignIcon extends AbstractThemedIcon {
    private Icon icon;

    /**
     * Creates a themed icon for the specified icon.
     * @param icon the icon to wrap
     */
    public ForeignIcon(Icon icon) {
        this.icon = icon;
    }

    /**
     * Creates a themed icon based on the platform desktop icon
     * for the specified icon.
     * @param file the non-null file to create an icon for
     */
    public ForeignIcon(File file) {
        icon = FileSystemView.getFileSystemView().getSystemIcon(Objects.requireNonNull(file, "file"));
        if (icon == null) {
            icon = ICON_FILE;
        }
    }

    @Override
    protected void paintIcon(Component c, Graphics2D g, int x, int y) {
        final int sysWidth = icon.getIconWidth();
        final int sysHeight = icon.getIconHeight();
        if (width == sysWidth && height == sysHeight) {
            icon.paintIcon(c, g, x, y);
        } else if (width > sysWidth && height > sysHeight) {
            if (width - sysWidth <= 6 && height - sysHeight <= 6) {
                icon.paintIcon(c, g, x + (width - sysWidth) / 2, y + (height - sysHeight) / 2);
                return;
            }
        }
        AffineTransform oldAt = g.getTransform();
        g.translate(x, y);
        g.scale((double) width / (double) sysWidth, (double) height / (double) sysHeight);
        icon.paintIcon(c, g, 0, 0);
        g.setTransform(oldAt);
    }

    @Override
    public ThemedIcon derive(int newWidth, int newHeight) {
        if (newWidth < 1 || newHeight < 1) {
            throw new IllegalArgumentException("icon size " + newWidth + 'x' + newHeight);
        }
        ForeignIcon derived = new ForeignIcon(icon);
        derived.width = newWidth;
        derived.height = newHeight;
        derived.disabled = disabled;
        return derived;
    }
}
