package ca.cgjennings.graphics.cloudfonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Font implementation for {@link GFCloudFontCollection}.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
final class GFFont implements CloudFont, Comparable<GFFont> {
    
    /**
     * Creates a new font instance for the specified family and file name.
     * 
     * @param family the family that the font belongs to
     * @param fileName the file name of the font on the server
     */
    protected GFFont(GFFamily family, String fileName) {
        this.family = family;
        this.fileName = fileName;
        String parsedStyle = "";
        int oSquare = fileName.indexOf('[');
        int styleEnd = oSquare < 0 ? fileName.lastIndexOf('.') : oSquare;
        if (styleEnd < 0) {
            throw new AssertionError("no extension");
        }
        int lastHyphen = fileName.lastIndexOf('-', styleEnd);
        parsedStyle = "";
        if (lastHyphen >= 0) {
            parsedStyle = fileName.substring(lastHyphen + 1, styleEnd);
            if (!parsedStyle.matches("(Black|Bold|Extra|Italic|Light|Medium|Regular|Semi|Thin)*")) {
                parsedStyle = "";
            }
        }
        style = parsedStyle;
        axes = null;
        sortKey = 2;
        if (parsedStyle.isEmpty()) {
            sortKey = 0;
        } else if (parsedStyle.equals("Regular")) {
            sortKey = 1;
        }
    }
    final GFFamily family;
    private final String fileName;
    private final String style;
    private Axis[] axes;
    private int sortKey;
    // file at time font was created, since it will be locked
    private File fontFile;
    private Font font;
    boolean alreadyRegistered;

    @Override
    public CloudFontFamily getFamily() {
        return family;
    }

    @Override
    public String getName() {
        String familyName = family.getName();
        if (style.isEmpty()) {
            return familyName;
        }
        return familyName + ' ' + style;
    }

    @Override
    public String getStyle() {
        return style;
    }

    @Override
    public Axis[] getAxes() {
        synchronized (this) {
            if (axes == null) {
                Axis[] familyAxes = family.getAxes();
                LinkedList<Axis> list = new LinkedList<>();
                for (Axis a : familyAxes) {
                    if (fileName.contains('[' + a.tag + ']')) {
                        list.add(a);
                    }
                }
                axes = list.toArray(Axis[]::new);
            }
            return axes.clone();
        }
    }

    String getCloudPath() {
        return family.path + '/' + fileName;
    }

    @Override
    public Font getFont() throws IOException {
        synchronized (family.coll) {
            if (font == null) {
                fontFile = getFontFile();
                font = localFileToFont(fontFile);
            }
            return font;
        }
    }

    @Override
    public File getFontFile() throws IOException {
        synchronized (family.coll) {
            if (fontFile == null) {
                fontFile = family.coll.downloadFont(this);
            }
            return fontFile;
        }
    }

    Font localFileToFont(File fontFile) throws IOException {
        synchronized (family.coll) {
            if (font != null) {
                return font;
            }
            try {
                log.log(Level.INFO, "loading font {0}", fontFile);
                font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                return font;
            } catch (FontFormatException ffe) {
                throw new IOException("invalid font file " + fontFile, ffe);
            }
        }
    }

    @Override
    public boolean isDownloaded() {
        try {
            File dlFile = family.coll.fontPathToLocalCacheFile(getCloudPath(), family.getVersionHash());
            return dlFile.exists();
        } catch (Exception ex) {
            return false;
        }
    }
    
    @Override
    public boolean isRegistered() {
        return alreadyRegistered;
    }

    @Override
    public int compareTo(GFFont other) {
        if (this == other) {
            return 0;
        }
        int d = family.getName().compareTo(other.family.getName());
        if (d != 0) {
            return d;
        }
        d = sortKey - other.sortKey;
        if (d != 0) {
            return d;
        }
        d = style.compareTo(other.style);
        if (d != 0) {
            return d;
        }
        d = fileName.compareTo(other.fileName);
        if (d != 0) {
            return d;
        }
        d = sortKey - other.sortKey;
        if (d != 0) {
            return d;
        }
        d = style.compareTo(other.style);
        if (d != 0) {
            return d;
        }
        d = fileName.compareTo(other.fileName);
        if (d != 0) {
            return d;
        }
        return family.path.compareTo(other.family.path);
    }

    @Override
    public String toString() {
        String s = getName();
        for (int i = 0; i < axes.length; ++i) {
            s += " [" + axes[i] + ']';
        }
        return s;
    }

    private static final java.util.logging.Logger log = ca.cgjennings.apps.arkham.StrangeEons.log;
}
