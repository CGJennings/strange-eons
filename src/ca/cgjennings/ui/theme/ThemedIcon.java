package ca.cgjennings.ui.theme;

import java.io.File;
import javax.swing.Icon;

/**
 * Interface implemented by all Strange Eons icons.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface ThemedIcon extends Icon {

    /**
     * Returns a new icon that renders the same image as this icon, but at a
     * different size.
     *
     * @param newWidth the new width ≥ 1
     * @param newHeight the new height ≥ 1
     * @return an icon with the revised dimensions
     */
    ThemedIcon derive(int newWidth, int newHeight);

    /**
     * Returns a new icon that renders the same image as this icon, but at a
     * different size.
     *
     * @param newSize the new width and height ≥ 1
     * @return an icon with the revised dimensions
     */
    default ThemedIcon derive(int newSize) {
        return derive(newSize, newSize);
    }

    /**
     * Returns a new icon that renders the same image as this icon, but as if
     * for a permanently disabled component.
     *
     * @return a disabled verison of the icon
     */
    ThemedIcon disabled();

    /**
     * Creates a themed icon from any arbitrary icon. If passed a themed icon,
     * returns it unchanged. If passed null, returns null. If passed some other
     * type of icon, it is converted to an equivalent themed icon.
     *
     * @param icon the icon to create a themed version for
     * @return a themed icon that renders the same graphic as the specified icon
     */
    public static ThemedIcon create(Icon icon) {
        if (icon == null) {
            return null;
        }
        return (icon instanceof ThemedIcon) ? (ThemedIcon) icon : new ForeignIcon(icon);
    }

    /**
     * Creates a themed icon based on the platform desktop icon for the
     * specified icon.
     *
     * @param file the non-null file to create an icon for
     */
    public static ThemedIcon create(File platformFile) {
        return new ForeignIcon(platformFile);
    }
    
    default ThemedIcon tiny() {
        return derive(TINY, TINY);
    }
    
    default ThemedIcon small() {
        return derive(SMALL, SMALL);
    }

    default ThemedIcon mediumSmall() {
        return derive(MEDIUM_SMALL, MEDIUM_SMALL);
    }
    
    default ThemedIcon medium() {
        return derive(MEDIUM, MEDIUM);
    }
    
    default ThemedIcon mediumLarge() {
        return derive(MEDIUM_LARGE, MEDIUM_LARGE);
    }
    
    default ThemedIcon large() {
        return derive(LARGE, LARGE);
    }
    
    default ThemedIcon veryLarge() {
        return derive(VERY_LARGE, VERY_LARGE);
    }    
    
    default ThemedIcon gigantic() {
        return derive(GIGANTIC, GIGANTIC);
    }
    
    public static final int TINY = 12;
    public static final int SMALL = 18;
    public static final int MEDIUM_SMALL = 24;
    public static final int MEDIUM = 32;
    public static final int MEDIUM_LARGE = 48;
    public static final int LARGE = 64;
    public static final int VERY_LARGE = 96;
    public static final int GIGANTIC = 256;
}
