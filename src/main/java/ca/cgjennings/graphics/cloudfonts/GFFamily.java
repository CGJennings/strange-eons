package ca.cgjennings.graphics.cloudfonts;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.cgjennings.apps.arkham.StrangeEons;
import resources.ResourceKit;

/**
 * Family implementation for {@link GFCloudCollection}.
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
final class GFFamily implements CloudFontFamily {
    
    GFFamily(GFCloudCollection collection, String name, String path, String fileList, String catList, String axesList, String subsetList, String versionHash, WeakIntern intern) {
        coll = collection;
        this.name = name;
        this.path = intern.of(path);
        this.sortKey = GFCloudCollection.toSortKey(name);
        this.fileList = fileList;
        categoryBits = catList == null ? 0 : CategorySet.toBits(catList.split(","));
        this.axesList = axesList;
        if (subsetList == null) {
            subsets = GFCloudCollection.EMPTY_STRING_ARRAY;
        } else {
            subsets = subsetList.split(",");
            for (int i = 0; i < subsets.length; ++i) {
                subsets[i] = intern.of(subsets[i].trim());
            }
        }
        this.hash = versionHash;
    }

    /** Creates a dummy family for searching by sort key. */
    GFFamily(String sortKey) {
        this.sortKey = sortKey;
        coll = null;
        name = null;
        path = null;
        categoryBits = 0;
        subsets = null;
        hash = null;
    }

    final GFCloudCollection coll;
    private final String name;
    final String sortKey;
    final String path;
    private final int categoryBits;
    private final String[] subsets;
    private final String hash;
    private String fileList;
    private GFFont[] fonts;
    private String axesList;
    private Axis[] axes;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CategorySet getCategories() {
        return new CategorySet(categoryBits);
    }

    @Override
    public String[] getSubsets() {
        return subsets.clone();
    }

    @Override
    public Axis[] getAxes() {
        if (axes == null) {
            if (axesList == null) {
                axes = GFCloudCollection.EMPTY_AXIS_ARRAY;
            } else {
                String[] axisStrings = axesList.split("\\|");
                axes = new Axis[axisStrings.length];
                for (int i = 0; i < axisStrings.length; ++i) {
                    axes[i] = new Axis(axisStrings[i]);
                }
            }
            axesList = null;
        }
        return axes.length == 0 ? axes : axes.clone();
    }

    @Override
    public CloudFont[] getCloudFonts() {
        if (fonts == null) {
            String[] files = fileList.split("\\|");
            fonts = new GFFont[files.length];
            int ok = 0;
            for (int i = 0; i < files.length; ++i) {
                try {
                    fonts[ok] = new GFFont(this, files[i]);
                    ++ok;
                } catch (Exception ex) {
                    log.log(Level.WARNING, "could not parse " + path, ex);
                }
            }
            if (ok < fonts.length) {
                fonts = Arrays.copyOf(fonts, ok);
            }
            Arrays.sort(fonts);
            fileList = null;
        }
        return fonts.clone();
    }

    @Override
    public Font[] getFonts() throws IOException {
        CloudFont[] cf = getCloudFonts();
        File[] files = coll.downloadFonts((GFFont[]) cf);
        Font[] awtFonts = new Font[cf.length];
        for (int i = 0; i < cf.length; ++i) {
            awtFonts[i] = ((GFFont) cf[i]).localFileToFont(files[i]);
        }
        return awtFonts;
    }

    @Override
    public ResourceKit.FontRegistrationResult[] register() throws IOException {
        Font[] awtFonts = getFonts();
        ResourceKit.FontRegistrationResult[] results = new ResourceKit.FontRegistrationResult[awtFonts.length];
        for (int i = 0; i < awtFonts.length; ++i) {
            if (fonts[i].alreadyRegistered) {
                results[i] = new ResourceKit.FontRegistrationResult(awtFonts[i], true);
            } else {
                results[i] = new ResourceKit.FontRegistrationResult(awtFonts[i], GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(awtFonts[i]));
                fonts[i].alreadyRegistered = results[i].isRegistrationSuccessful();
            }
        }
        return results;
    }

    public String getVersionHash() {
        return hash;
    }

    @Override
    public String getLicenseType() {
        if (path.startsWith("apache")) {
            return "Apache";
        } else if (path.startsWith("ofl")) {
            return "Open Font License";
        } else if (path.startsWith("ufl")) {
            return "Ubuntu Font License";
        } else {
            return "Unknown";
        }
    }

    @Override
    public String toString() {
        return name;
    }

    private static final Logger log = StrangeEons.log;
}
