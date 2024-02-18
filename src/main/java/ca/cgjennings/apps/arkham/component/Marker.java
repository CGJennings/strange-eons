package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.MarkerEditor;
import ca.cgjennings.apps.arkham.sheet.MarkerSheet;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.graphics.filters.AlphaInversionFilter;
import ca.cgjennings.io.SEObjectInputStream;
import ca.cgjennings.io.SEObjectOutputStream;
import gamedata.Silhouette;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * The {@link GameComponent} used to create generic markers and tokens.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class Marker extends AbstractGameComponent {

    static final long serialVersionUID = 543985398354983546L;

    private String stencilKey;
    private transient BufferedImage stencil;
    /**
     * Path to the portraitb image.
     */
    private String portraitImageb = "";
    private double portraitScaleb, portraitPanXb, portraitPanYb;
    /**
     * Portrait image to use when drawing the component or writing the file.
     */
    private transient BufferedImage portraitb;

    private String frontText, backText;

    private double bleedMargin;

    // front portrait variables moved from AbstractPortraitComponent
    private String portraitImage;
    private double portraitScale, portraitPanX, portraitPanY;
    private transient BufferedImage portrait;

    public Marker() {
        clearAll();
        setNameImpl(Language.string("ae-unsaved"));
    }

    @Override
    public void clearAll() {
        super.clearAll();

        stencil = null;
        stencilKey = "@sil-oval";
        Silhouette[] sils = Silhouette.getSilhouettes();
        for (int i = 0; i < sils.length; ++i) {
            if (sils[i].getKey().equals(stencilKey)) {
                stencil = sils[i].getStencil();
                break;
            }
        }
        if (stencil == null) {
            stencilKey = sils[0].getKey();
            stencil = sils[0].getStencil();
            System.err.println("Warning: missing standard silhouette @sil-oval");
        }

        installDefaultPortrait(true);
        installDefaultPortrait(false);
        frontText = backText = "";
    }

    protected void installDefaultPortrait(boolean front) {
        Silhouette s = Silhouette.get(stencilKey);
        String resource;
        if (s == null) {
            resource = Silhouette.DEFAULT_PORTRAIT;
        } else {
            resource = s.getDefaultPortrait();
        }
        if (front) {
            portrait = ResourceKit.getImage(resource);
            portraitScale = 1d;
            portraitPanX = 0d;
            portraitPanY = 0d;
        } else {
            portraitb = ResourceKit.getImage(resource);
            portraitScaleb = 1d;
            portraitPanXb = 0d;
            portraitPanYb = 0d;
        }
    }

    @Override
    public Sheet<Marker>[] createDefaultSheets() {
        @SuppressWarnings("unchecked")
        Sheet<Marker>[] sh = new Sheet[]{new MarkerSheet(this, false), new MarkerSheet(this, true)};
        setSheets(sh);
        return sh;
    }

    @Override
    public AbstractGameComponentEditor<Marker> createDefaultEditor() {
        return new MarkerEditor(this);
    }

    public BufferedImage getStencil() {
        return stencil;
    }

    public String getStencilKey() {
        return stencilKey;
    }

    /**
     * Returns the bleed margin for this marker, which is determined by the
     * current silhouette.
     *
     * @return the current bleed margin
     */
    public double getBleedMargin() {
        return bleedMargin;
    }

    public void setSilhouette(Silhouette sil) {
        if (sil == null) {
            throw new NullPointerException("sil");
        }
        if (!stencilKey.equals(sil.getKey())) {
            stencilKey = sil.getKey();
            stencil = sil.getStencil();
            cachedClipStencil = null;
            cachedBackClipStencil = null;
            bleedMargin = sil.getBleedMargin();
            // readjust images if using default portraits
            if (portraitImage == null || portraitImage.length() == 0) {
                installDefaultPortrait(true);
            }
            if (portraitImageb == null || portraitImageb.length() == 0) {
                installDefaultPortrait(false);
            }
            markChanged(0);
            markChanged(1);
        }
    }

    private void setPortrait(String portraitImage) throws IOException {
        if (!portraitImage.equals(this.portraitImage)) {
            if (portraitImage.isEmpty()) {
                installDefaultPortrait(true);
            } else {
                portrait = AbstractPortrait.getImageFromIdentifier(portraitImage, new Dimension(stencil.getWidth(), stencil.getHeight()));

                if (privateSettings.getYesNo("premultiply-image-alpha")) {
                    portrait = ImageUtilities.ensurePremultipliedFormat(portrait);
                }

                // come up with default orientation
                portraitPanX = portraitPanY = 0d;
                // ideal portrait size
                portraitScale = ImageUtilities.idealCoveringScaleForImage(stencil.getWidth(), stencil.getHeight(), portrait.getWidth(), portrait.getHeight());
            }
            this.portraitImage = portraitImage;
            markChanged();
        }
    }

    private void setBackPortrait(String portraitImage) throws IOException {
        if (!portraitImageb.equals(portraitImage)) {
            if (portraitImage.isEmpty()) {
                installDefaultPortrait(false);
            } else {
                portraitb = AbstractPortrait.getImageFromIdentifier(portraitImage, new Dimension(stencil.getWidth(), stencil.getHeight()));

                if (privateSettings.getYesNo("premultiply-image-alpha")) {
                    portraitb = ImageUtilities.ensurePremultipliedFormat(portraitb);
                }

                // come up with default orientation
                portraitPanXb = portraitPanYb = 0d;
                // ideal portraitb size
                portraitScaleb = ImageUtilities.idealCoveringScaleForImage(stencil.getWidth(), stencil.getHeight(), portraitb.getWidth(), portraitb.getHeight());
            }
            this.portraitImageb = portraitImage;
            markChanged();
        }
    }

    private String getBackPortrait() {
        return portraitImageb;
    }

    private BufferedImage getBackPortraitImage() {
        return portraitb;
    }

    private double getBackPortraitScale() {
        return portraitScaleb;
    }

    private void setBackPortraitScale(double scale) {
        if (scale < 0.00000001d) {
            scale = 0.00000001d;
        }
        if (portraitScaleb != scale) {
            portraitScaleb = scale;
            markChanged(1);
        }
    }

    private double getBackPortraitPanX() {
        return portraitPanXb;
    }

    private void setBackPortraitPanX(double pan) {
        if (portraitPanXb != pan) {
            portraitPanXb = pan;
            markChanged(1);
        }
    }

    private double getBackPortraitPanY() {
        return portraitPanYb;
    }

    private void setBackPortraitPanY(double pan) {
        if (portraitPanYb != pan) {
            portraitPanYb = pan;
            markChanged(1);
        }
    }

    private double getPortraitScale() {
        return portraitScale;
    }

    private void setPortraitScale(double scale) {
        if (scale < 0.00000001d) {
            scale = 0.00000001d;
        }
        if (portraitScale != scale) {
            portraitScale = scale;
            markChanged();
        }
    }

    private double getPortraitPanX() {
        return portraitPanX;
    }

    private void setPortraitPanX(double pan) {
        if (portraitPanX != pan) {
            portraitPanX = pan;
            markChanged();
        }
    }

    private double getPortraitPanY() {
        return portraitPanY;
    }

    private void setPortraitPanY(double pan) {
        if (portraitPanY != pan) {
            portraitPanY = pan;
            markChanged();
        }
    }

    private String getPortrait() {
        return portraitImage;
    }

    private BufferedImage getPortraitImage() {
        return portrait;
    }

    public String getFrontText() {
        return frontText;
    }

    public void setFrontText(String frontText) {
        if (!this.frontText.equals(frontText)) {
            this.frontText = frontText;
            markChanged(0);
        }
    }

    public String getBackText() {
        return backText;
    }

    public void setBackText(String backText) {
        if (!this.backText.equals(backText)) {
            this.backText = backText;
            markChanged(1);
        }
    }

    public int getPortraitCount() {
        return 2;
    }

    public Portrait getPortrait(int index) {
        if (index < 0 || index > 1) {
            throw new IndexOutOfBoundsException("invalid index " + index);
        }

        if (index == 0) {
            if (cachedPortrait == null) {
                cachedPortrait = new AbstractPortrait() {
                    @Override
                    public void setSource(String resource) {
                        try {
                            setPortrait(resource);
                        } catch (IOException e) {
                            // this should no longer be thrown (will get error image instead)
                            throw new RuntimeException("Assertion failed: setPortrait should not throw IOException", e);
                        }
                    }

                    @Override
                    public String getSource() {
                        return getPortrait();
                    }

                    @Override
                    public BufferedImage getImage() {
                        return getPortraitImage();
                    }

                    @Override
                    public void setImage(String reportedSource, BufferedImage image) {
                        portrait = ResourceKit.prepareNewImage(image);
                        // come up with default orientation
                        portraitPanX = portraitPanY = 0d;
                        // ideal portrait size
                        portraitScale = ImageUtilities.idealCoveringScaleForImage(stencil.getWidth(), stencil.getHeight(), portrait.getWidth(), portrait.getHeight());
                        portraitImage = reportedSource;
                        markChanged();
                    }

                    @Override
                    public double getScale() {
                        return getPortraitScale();
                    }

                    @Override
                    public void setScale(double scale) {
                        setPortraitScale(scale);
                    }

                    @Override
                    public double getPanX() {
                        return getPortraitPanX();
                    }

                    @Override
                    public void setPanX(double x) {
                        setPortraitPanX(x);
                    }

                    @Override
                    public double getPanY() {
                        return getPortraitPanY();
                    }

                    @Override
                    public void setPanY(double y) {
                        setPortraitPanY(y);
                    }

                    @Override
                    public void installDefault() {
                        installDefaultPortrait(true);
                    }

                    @Override
                    public Dimension getClipDimensions() {
                        return new Dimension(
                                stencil.getWidth(),
                                stencil.getHeight()
                        );
                    }

                    @Override
                    public BufferedImage getClipStencil() {
                        updateClipStencils();
                        return cachedClipStencil;
                    }
                };
            }
            return cachedPortrait;
        }

        if (cachedBackPortrait == null) {
            cachedBackPortrait = new AbstractPortrait() {
                @Override
                public void setSource(String resource) {
                    try {
                        setBackPortrait(resource);
                    } catch (IOException e) {
                        // this should no longer be thrown (will get error image instead)
                        throw new RuntimeException("Assertion failed: setPortrait should not throw IOException", e);
                    }
                }

                @Override
                public String getSource() {
                    return getBackPortrait();
                }

                @Override
                public BufferedImage getImage() {
                    return getBackPortraitImage();
                }

                @Override
                public void setImage(String reportedSource, BufferedImage image) {
                    portraitb = ResourceKit.prepareNewImage(image);
                    // come up with default orientation
                    portraitPanXb = portraitPanYb = 0d;
                    // ideal portraitb size
                    portraitScaleb = ImageUtilities.idealCoveringScaleForImage(stencil.getWidth(), stencil.getHeight(), portraitb.getWidth(), portraitb.getHeight());
                    portraitImageb = reportedSource;
                    markChanged();
                }

                @Override
                public double getScale() {
                    return getBackPortraitScale();
                }

                @Override
                public void setScale(double scale) {
                    setBackPortraitScale(scale);
                }

                @Override
                public double getPanX() {
                    return getBackPortraitPanX();
                }

                @Override
                public void setPanX(double x) {
                    setBackPortraitPanX(x);
                }

                @Override
                public double getPanY() {
                    return getBackPortraitPanY();
                }

                @Override
                public void setPanY(double y) {
                    setBackPortraitPanY(y);
                }

                @Override
                public void installDefault() {
                    installDefaultPortrait(false);
                }

                @Override
                public Dimension getClipDimensions() {
                    return new Dimension(
                            stencil.getWidth(), stencil.getHeight()
                    );
                }

                @Override
                public BufferedImage getClipStencil() {
                    updateClipStencils();
                    return cachedBackClipStencil;
                }
            };
        }
        return cachedBackPortrait;
    }
    private transient Portrait cachedBackPortrait;
    private transient Portrait cachedPortrait;

    private transient BufferedImage cachedClipStencil, cachedBackClipStencil;

    private void updateClipStencils() {
        if (cachedClipStencil == null) {
            cachedClipStencil = getStencil();
            AlphaInversionFilter aif = new AlphaInversionFilter();
            cachedClipStencil = aif.filter(cachedClipStencil, null);
            cachedBackClipStencil = ImageUtilities.flip(cachedClipStencil, true, false);
        }
    }

    private static final int CURRENT_VERSION = 2;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_VERSION);

        out.writeObject(getName());
        out.writeObject(getFrontText());
        out.writeObject(getBackText());
        out.writeObject(comments);
        out.writeObject(stencilKey);

        out.writeDouble(bleedMargin);

        out.writeObject(portraitImage);
        out.writeDouble(portraitScale);
        out.writeDouble(portraitPanX);
        out.writeDouble(portraitPanY);

        out.writeObject(portraitImageb);
        out.writeDouble(portraitScaleb);
        out.writeDouble(portraitPanXb);
        out.writeDouble(portraitPanYb);

        out.writeObject(privateSettings);

        SEObjectOutputStream sout = (SEObjectOutputStream) out;
        sout.writeImage(portrait);
        sout.writeImage(portraitb);
        sout.writeImage(stencil);

        markSaved();
    }

    @Override
    public double computeIdealScaleForImage(BufferedImage image, String imageKey) {
        double idealWidth = stencil.getWidth();
        double idealHeight = stencil.getHeight();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        return ImageUtilities.idealCoveringScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();

        setNameImpl((String) in.readObject());
        frontText = (String) in.readObject();
        backText = (String) in.readObject();
        comments = (String) in.readObject();
        stencilKey = (String) in.readObject();

        if (version >= 2) {
            bleedMargin = in.readDouble();
        } else {
            bleedMargin = 0d;
        }

        portraitImage = (String) in.readObject();
        portraitScale = in.readDouble();
        portraitPanX = in.readDouble();
        portraitPanY = in.readDouble();

        portraitImageb = (String) in.readObject();
        portraitScaleb = in.readDouble();
        portraitPanXb = in.readDouble();
        portraitPanYb = in.readDouble();

        privateSettings = (Settings) in.readObject();

        SEObjectInputStream sin = (SEObjectInputStream) in;
        portrait = sin.readImage();
        portraitb = sin.readImage();
        stencil = sin.readImage();
    }
}
