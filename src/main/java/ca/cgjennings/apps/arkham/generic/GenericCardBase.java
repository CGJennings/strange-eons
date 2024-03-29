package ca.cgjennings.apps.arkham.generic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Objects;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.Length;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.DefaultPortrait;
import ca.cgjennings.apps.arkham.component.PortraitProvider;
import ca.cgjennings.apps.arkham.component.conversion.ConversionSession;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.cloudfonts.CloudFonts;
import ca.cgjennings.util.SerialClone;
import resources.Language;
import resources.Settings;
import resources.Settings.Region;

/**
 * The base class for generic cards. Each subclass implements
 * a specific size. 
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public class GenericCardBase extends AbstractGameComponent implements PortraitProvider {
    static final long serialVersionUID = -45234524755650509L;

    /**
     * Resolution of the template in pixels per inch.
     * This establishes the "template pixel unit" (TPX).
     * When painting, the graphics context is scaled so that 1 unit = 1/PPI inches.
     * (Sheets can be rendered at any resolution regardless of the value.)
     */
    private static final int PPI = 150;
    /** Convert inch measurements to TPX. */
    protected static final double IN_TO_TPX = PPI;
    /** Convert TPX measurements to inches. */
    protected static final double TPX_TO_IN = 1d / PPI;
    /** Convert mm measurements to TPX. */
    protected static final double MM_TO_TPX = 0.0393701d * PPI;
    /** Convert points to TPX. */
    protected static final double PT_TO_TPX = IN_TO_TPX / 72d;
    /** Convert TPX to points. */
    protected static final double TPX_TO_PT = 72d / IN_TO_TPX;

    /** Bleed margin size in points. */
    protected static final int BLEED_MARGIN_PT = 9;
    /** Bleed margin size in template "pixels". */
    private static final int BLEED_MARGIN_TPX = (int) Math.ceil((double) BLEED_MARGIN_PT * PT_TO_TPX);

    /** Width, determined as a number of pixels at the resolution set by PPI. */
    private int width;
    /** Height, determined as a number of pixels at the resolution set by PPI. */
    private int height;
    /** The card type ID set by the subclass. */
    private String id;

    /** Main body text (title is stored in the standard "name" property). */
    private String text;
    /** Font family for headings. */
    private String titleFamily;
    /** Font family for body text. */
    private String textFamily;
    /** Base font size for body text. */
    private float baseFontSize;
    /** If true, no portrait is drawn and only text is displayed. */
    private boolean textOnly;
    /** If true, the interior of the card is filled with a translucent overlay. */
    private boolean fillInterior;
    /** If true, the portrait is drawn under the face of the card. */
    private boolean portraitUnderFace;
    /** Portrait for the actual card portrait. */
    private DefaultPortrait portrait;
    /** Portrait for the front face design. */
    private DefaultPortrait frontFacePortrait;
    /** Portrait for the back face design. */
    private DefaultPortrait backFacePortrait; 

    /**
     * Creates a new generic card with the specified dimensions. Concrete
     * subclasses pass an ID and a size. The card creates its own setting
     * keys without replying on any default settings, using a prefix of
     * {@code generic-} followed by the ID. Though not required, portrait
     * template keys can be set in the default settings to override the
     * default portrait images.
     * 
     * @param id a unique ID that can be used to help identify the card type
     * @param width the width of the card
     * @param height the height of the card
     */
    protected GenericCardBase(String id, Length width, Length height) {
        super();
        this.id = Objects.requireNonNull(id);

        // convert dimensions in TPX
        double w = width.get(Length.IN) * IN_TO_TPX;
        double h = height.get(Length.IN) * IN_TO_TPX;
        // add space for the bleed margin
        w += BLEED_MARGIN_TPX * 2;
        h += BLEED_MARGIN_TPX * 2;
        // store the final, rounded dimensions
        this.width = (int) Math.ceil(w);
        this.height = (int) Math.ceil(h);

        if (this.width < PPI || this.height < PPI) {
            throw new IllegalArgumentException("card dimensions must be at least 1 inch / 2.54 cm");
        }

        // Explicitly set game:
        // the component creation system sets this based on the class map,
        // but given the nature of this class, some might want to create instances
        // directly for various purposes
        getSettings().set("game", "*");

        final int[] frontFace = new int[] {0};
        final int[] backFace = new int[] {1};

        initLayout();
        portrait = new DefaultPortrait(this, key(null));
        portrait.setBackgroundFilled(false);
        portrait.setFacesToUpdate(frontFace);
        portrait.installDefault();
        frontFacePortrait = new DefaultPortrait(this, key("-front-face"));
        frontFacePortrait.setBackgroundFilled(false);
        frontFacePortrait.setFacesToUpdate(frontFace);
        frontFacePortrait.installDefault();        
        backFacePortrait = new DefaultPortrait(this, key("-back-face"));
        backFacePortrait.setFacesToUpdate(backFace);
        backFacePortrait.installDefault();
        applyDefaults();
    }

    /**
     * Returns the card's corner radius, in points.
     * 
     * <p>
     * Subclasses can override this to change the corner radius.
     * The base class returns 15 points.
     */
    public double getCornerRadius() {
        return 15d;
    }

    /** Returns the full name of the card type, as shown in the New Component dialog. */
    public String getLongName() {
        return Language.string("gencard-sz-" + id);
    }

    /** Returns the short name of the card type. */
    public String getShortName() {
        return Language.string("gencard-sh-" + id);
    }

    @Override
    public String toString() {
        return "GenericCardBase{type=" + getLongName() + ", title=" + getName() + '}';
    }

    static { 
        Language.getGame().addStrings("text/game/generic-card");
    }
  
    /**
     * Called to set up the default properties for a new card.
     * Subclasses can override this to set up their own defaults.
     */
    protected void applyDefaults() {
        setName(Language.getGame().get("generic-card-title"));
        text = Language.getGame().get("generic-card-text");

        titleFamily = "";
        textFamily = "";
        baseFontSize = getDefaultFontSize();

        textOnly = false;
        fillInterior = true;
        portraitUnderFace = false;
    }

    /**
     * Returns the default font size to use for the card's text.
     * The default font size is scaled according to the card's
     * dimensions.
     * 
     * @return the default font size in points
     */
    protected float getDefaultFontSize() {
        // linearly interpolate between 6 points
        // at height of 1 inch, and 15 points at
        // a height of 5.5 inches
        float height = (float) this.height / (float) PPI;
        float ratio = Math.max(0, Math.min(1, (height - 1f)/4.4f));
        float size = 6f + ratio * 9f;
        // round to nearest quarter pt
        return Math.round(size * 4f) / 4f;
    }

    /**
     * Clears the card content without affecting the card's
     * design settings.
     */
    public void clearContent() {
        setTitle("");
        setText("");
        portrait.installDefault();
    }

    @Override
    public void clearAll() {
        text = "";
        titleFamily = "";
        textFamily = "";
        baseFontSize = getDefaultFontSize();
        textOnly = false;
        fillInterior = true;
        portraitUnderFace = false;
        portrait.installDefault();
        frontFacePortrait.installDefault();
        backFacePortrait.installDefault();
        super.clearAll();
    }

    @Override
    public void convertFrom(ConversionSession session) {
        super.convertFrom(session);
        GenericCardBase target = (GenericCardBase) session.getTarget();
        target.text = text;
        target.titleFamily = titleFamily;
        target.textFamily = textFamily;
        // use the component's default font size, which is scaled to the card size
        // target.baseFontSize = baseFontSize;
        target.textOnly = textOnly;
        target.fillInterior = fillInterior;
        target.portraitUnderFace = portraitUnderFace;
        // copy portrait images but reset the layout to match the new size
        session.copyAllPortraits(false);
    }

    @Override
    public GenericCardBase clone() {
        return SerialClone.clone(this);
    }    

    String key(String suffix) {
        return "generic-" + id + (suffix == null ? "" : suffix);
    }

    /** Returns the ID string that succinctly describes the card type. */
    public String getId() {
        return id;
    }

    /**
     * Returns the card width, excluding the bleed margin.
     * Due to rounding and other factors, this may not be exactly
     * the same as the width used to create the card.
     */
    public Length getWidthLength() {
        int tpx = width - 2 * BLEED_MARGIN_TPX;
        return new Length(tpx * TPX_TO_IN, Length.IN);
    }

    /**
     * Returns the card height, excluding the bleed margin.
     * Due to rounding and other factors, this may not be exactly
     * the same as the height used to create the card.
     */
    public Length getHeightLength() {
        int tpx = height - 2 * BLEED_MARGIN_TPX;
        return new Length(tpx * TPX_TO_IN, Length.IN);
    }

    /**
     * Sets the title of the card. The title and name are the
     * same for generic cards, so this is equivalent to calling
     * {@link #setName(java.lang.String)}, except that for consistency
     * with {@link #setText(String)} it won't throw an exception
     * if the title is null.
     * 
     * @param title the title text to display
     */
    public void setTitle(String title) {
        setName(title == null ? "" : title);
    }

    /**
     * Returns the title of the card. This is equivalent to calling
     * {@link #getName()}.
     * 
     * @return the title text to display
     */
    public String getTitle() {
        return getName();
    }
    
    /**
     * Sets the main body text of the card.
     * @param markup the markup content to display
     */
    public void setText(String markup) {
        markup = markup == null ? "" : markup;
        if (text.equals(markup)) {
            return;
        }        
        text = markup;
        markChanged(0);
    }

    /**
     * Returns the main body text of the card.
     * @return the markup content to display
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the font family used to draw title text. The family
     * must either be registered or the name of a locally installed
     * font. If the family name begins with the string
     * {@code "cloud:"}, then the rest of the string is treated as
     * the name of a cloud font family, and it will be downloaded
     * and registered if it is not already available.
     * 
     * @param family the font name, or an empty string to use the default,
     * or {@code "cloud:"} followed by the name of a cloud font
     * @see CloudFonts
     * @see #getTitleFamily()
     */
    public void setTitleFamily(String family) {
        if (family == null) {
            family = "";
        }
        if (family.equals(titleFamily)) {
            return;
        }
        titleFamily = family;
        markChanged(0);
    }

    /**
     * Returns the font family used to draw title text.
     * @return the font name
     * @see #setTitleFamily(java.lang.String)
     */
    public String getTitleFamily() {
        return titleFamily;
    }

    /**
     * Sets the font family used to draw main body text. The family
     * must either be registered or the name of a locally installed
     * font. If the family name begins with the string
     * {@code "cloud:"}, then the rest of the string is treated as
     * the name of a cloud font family, and it will be downloaded
     * and registered if it is not already available.
     * 
     * @param family the font name, or an empty string to use the default,
     * or {@code "cloud:"} followed by the name of a cloud font
     * @see CloudFonts
     * @see #getTextFamily()
     */
    public void setTextFamily(String family) {
        if (family == null) {
            family = "";
        }
        if (family.equals(textFamily)) {
            return;
        }
        textFamily = family;
        markChanged(0);
    }

    /**
     * Returns the font family used to draw main body text.
     * @return the font name
     * @see #setTextFamily(java.lang.String)
     */
    public String getTextFamily() {
        return textFamily;
    }

    /**
     * Sets the base font size used to draw text. The size is
     * clamped to a range of 6 to 72 points. The size used
     * for headings is derived from the base size automatically,
     * and the initial font size is chosen based on the
     * card dimensions.
     * 
     * @param size the font size in points
     * @see #getBaseFontSize()
     */
    public void setBaseFontSize(float size) {
        size = Math.max(6f, Math.min(72f, size));
        if (size != baseFontSize) {
            baseFontSize = size;
            markChanged(0);
        }
    }

    /**
     * Returns the base font size used to draw text.
     * @return the font size in points
     * @see #setBaseFontSize(float)
     */
    public float getBaseFontSize() {
        return baseFontSize;
    }

    /**
     * Sets whether the card should display only its text content.
     * 
     * @param textOnly if true, only text is displayed; otherwise,
     * a portrait is drawn in tht top area of the card, and text
     * is drawn below.
     * @see #isTextOnly()
     */
    public void setTextOnly(boolean textOnly) {
        if (this.textOnly != textOnly) {
            this.textOnly = textOnly;
            markChanged(0);
        }
    }

    /**
     * Returns whether the card should display only its text content.
     * @return true if only text is displayed
     * @see #setTextOnly(boolean)
     */
    public boolean isTextOnly() {
        return textOnly;
    }

    /**
     * Sets whether the interior (safe area) of the card should
     * be filled with a translucent overlay. This helps define
     * the card border and makes text easier to read on dark
     * or busy face designs.
     * 
     * @param fill if true, the interior is filled
     * @see #isInteriorFilled()
     */
    public void setInteriorFilled(boolean fill) {
        if (fill != fillInterior) {
            fillInterior = fill;
            markChanged(0);
        }
    }

    /**
     * Returns whether the interior (safe area) of the card
     * is filled with a translucent overlay.
     * 
     * @return true if the interior is filled
     * @see #setInteriorFilled(boolean)
     */
    public boolean isInteriorFilled() {
        return fillInterior;
    }

    /**
     * Sets whether the portrait is drawn under the face of the card.
     * This can be used with a card face design with strategic holes
     * to reveal the portrait or draw complex ornamentation around it.
     * 
     * @param under if true, the portrait is drawn under the face
     * @see #isPortraitUnderFace()
     */
    public void setPortraitUnderFace(boolean under) {
        if (under == portraitUnderFace) {
            portraitUnderFace = under;
            markChanged(0);
        }
    }

    /**
     * Returns whether the portrait is drawn under the face of the card.
     * @return true if the portrait is drawn under the face
     * @see #setPortraitUnderFace(boolean)
     */
    public boolean isPortraitUnderFace() {
        return portraitUnderFace;
    }
    
    /**
     * Automatically devises a layout based on the card size,
     * store the results as private settings.
     */
    protected void initLayout() {
        final Settings s = getSettings();

        // set the bleed margin size using the key expected by the Sheet class
        s.setDouble(key("-front-bleed-margin"), BLEED_MARGIN_PT);
        s.setDouble(key("-back-bleed-margin"), BLEED_MARGIN_PT);        

        // the full design area, including the bleed margins;
        // this is the area that the card face design "portraits" will cover
        //   the "-bleed-region" setting is for reference only
        final Region bleedRegion = new Region(0, 0, width, height);
        s.setRegion(key("-bleed"), bleedRegion);
        fillInPortraitSettings(key("-front-face"), bleedRegion, "templates/generic-card-face.jp2", false);
        fillInPortraitSettings(key("-back-face"), bleedRegion, "templates/generic-card-face.jp2", false);

        // the card face area, excluding the bleed margins
        //   the "-card-face-region" setting is for reference only
        final Region cardEdges = new Region(bleedRegion);
        cardEdges.grow(-BLEED_MARGIN_TPX, -BLEED_MARGIN_TPX);
        s.setRegion(key("-card-face"), cardEdges);

        // the safe area, inset from the card edges by the bleed margin
        // all important content is placed within this area
        //   the "-safe-region" setting is for reference only
        final Region safeRegion = new Region(cardEdges);
        safeRegion.grow(-BLEED_MARGIN_TPX, -BLEED_MARGIN_TPX);
        s.setRegion(key("-safe"), safeRegion);

        // text will be inset by a small margin from the safe area edges
        // and other content (e.g., the portrait)
        //   the "-text-margin" setting is for reference only
        final int textMargin = BLEED_MARGIN_TPX;
        s.setInt(key("-text-margin"), textMargin);

        // the portrait covers the safe area horizontally;
        // to determine the height, we start by calculating what its
        // height would be if it had a 4:3 aspect ratio, but then limit
        // the maximum height to half of the safe area,
        // less the gap the for the text margin
        final Region portraitRegion = new Region(safeRegion);
        portraitRegion.height = Math.min(
            safeRegion.width * 3 / 4,
            safeRegion.height/2 - textMargin
        );
        fillInPortraitSettings(key(null), portraitRegion, "portraits/generic-card-portrait.jp2", true);

        // the text region used for "text-only" cards is just the safe area
        // inset by the text margin
        final Region fullTextRegion = new Region(safeRegion);
        fullTextRegion.grow(-textMargin, -textMargin);
        s.setRegion(key("-full-text"), fullTextRegion);

        // the text region used with portraits is the same, but it starts at the
        // bottom of the portrait region (plus the text margin)
        final Region textRegion = new Region(fullTextRegion);
        textRegion.y = portraitRegion.getY2() + textMargin;
        textRegion.setY2(fullTextRegion.getY2());
        s.setRegion(key("-text"), textRegion);

        // the expansion symbol is placed along the bottom of the safe area
        final Region expansionSymbol = new Region(
            // center horizontally
            width / 2 - textMargin / 2 + 1,
            fullTextRegion.y + fullTextRegion.height,
            textMargin-2,
            textMargin-2
        );
        s.setRegion(key("-front-expsym"), expansionSymbol);
    }

    private void fillInPortraitSettings(String baseName, Region region, String defaultImage, boolean rotation) {
        final Settings s = getSettings();
        final String templateKey = baseName + "-template";
        s.set(templateKey, s.get(templateKey, defaultImage));
        s.setRegion(baseName + "-portrait-clip", region);
        s.set(baseName + "-portrait-template", defaultImage);
        s.set(baseName + "-portrait-scale", null); // calculate scale from region
        s.set(baseName + "-portrait-panx", "0");
        s.set(baseName + "-portrait-pany", "0");
        if (rotation) {
            s.set(baseName + "-portrait-rotation", "0");
        }
    }

    @Override
    public AbstractGameComponentEditor<? extends GenericCardBase> createDefaultEditor() {
        return new GenericCardEditor(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Sheet<AbstractGameComponent>[] createDefaultSheets() {
        var sheets = new Sheet[] {
            new ca.cgjennings.apps.arkham.generic.GenericCardFrontSheet(this),
            new ca.cgjennings.apps.arkham.generic.GenericCardBackSheet(this)
        };
        setSheets(sheets);
        return sheets;
    }

    @Override
    public int getPortraitCount() {
        return 3;
    }

    @Override
    public DefaultPortrait getPortrait(int index) {
        switch (index) {
            case 0: return portrait;
            case 1: return frontFacePortrait;
            case 2: return backFacePortrait;
            default: throw new IndexOutOfBoundsException("portrait index: " + index);
        }
    }

    // These helper methods are called by the sheet implementations because this card
    // type does not follow the usual method of using default settings to define its properties.

    int getTemplateResolution() {
        return PPI;
    }
    BufferedImage getTemplateImage() {
        synchronized (sharedTemplates) {
            final String key = "w" + width + 'h' + height;
            SoftReference<BufferedImage> ref = sharedTemplates.get(key);
            BufferedImage t = ref == null ? null : ref.get();
            if (t == null) {
                t = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = t.createGraphics();
                try {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, width, height);
                } finally {
                    g.dispose();
                }
                sharedTemplates.put(key, new SoftReference<>(t));
            }
            return t;
        }
    }
    private static HashMap<String, SoftReference<BufferedImage>> sharedTemplates = new HashMap<>();

    // Serialization

    private static final int VERSION = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(VERSION);
        write(out);
        out.writeObject(id);
        out.writeInt(width);
        out.writeInt(height);

        out.writeObject(text);
        
        out.writeObject(titleFamily);
        out.writeObject(textFamily);
        out.writeFloat(baseFontSize);
        
        out.writeBoolean(textOnly);
        out.writeBoolean(fillInterior);
        out.writeBoolean(portraitUnderFace);

        out.writeObject(portrait);
        out.writeObject(frontFacePortrait);
        out.writeObject(backFacePortrait);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        /* final int version = */ in.readInt();
        read(in);
        id = (String) in.readObject();
        width = in.readInt();
        height = in.readInt();

        text = (String) in.readObject();
        
        titleFamily = (String) in.readObject();
        textFamily = (String) in.readObject();
        baseFontSize = in.readFloat();
        
        textOnly = in.readBoolean();
        fillInterior = in.readBoolean();
        portraitUnderFace = in.readBoolean();

        portrait = (DefaultPortrait) in.readObject();
        frontFacePortrait = (DefaultPortrait) in.readObject();
        backFacePortrait = (DefaultPortrait) in.readObject();
    }    
}
