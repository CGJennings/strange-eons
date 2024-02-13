package ca.cgjennings.graphics.cloudfonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;

/**
 * Font implementation for {@link GFCloudCollection}.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
final class GFFont implements CloudFont, Comparable<GFFont> {
    
    protected GFFont(GFFamily family, String file) {
        this.family = family;
        this.file = file;
        String parsedStyle = "";
        int oSquare = file.indexOf('[');
        int styleEnd = oSquare < 0 ? file.lastIndexOf('.') : oSquare;
        if (styleEnd < 0) {
            throw new AssertionError("no extension");
        }
        int lastHyphen = file.lastIndexOf('-', styleEnd);
        parsedStyle = "";
        if (lastHyphen >= 0) {
            parsedStyle = file.substring(lastHyphen + 1, styleEnd);
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
    private final String file;
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
                    if (file.contains('[' + a.tag + ']')) {
                        list.add(a);
                    }
                }
                axes = list.toArray(Axis[]::new);
            }
            return axes.clone();
        }
    }

    String getCloudPath() {
        return family.path + '/' + file;
    }

    @Override
    public Font getFont() throws IOException {
        synchronized (family.coll) {
            if (font == null) {
                fontFile = family.coll.downloadFont(this);
                font = localFileToFont(fontFile);
            }
            return font;
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
        d = file.compareTo(other.file);
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
