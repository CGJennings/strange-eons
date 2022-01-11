package ca.cgjennings.apps.arkham.component;

import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.apps.arkham.sheet.UndecoratedCardBack;
import ca.cgjennings.graphics.ImageUtilities;
import gamedata.Expansion;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import static resources.Language.string;
import resources.Settings;
import resources.StrangeImage;

/**
 * Provides default implementations for the {@code GameComponent} interface.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public abstract class AbstractGameComponent implements Serializable, Cloneable, GameComponent {

    static final long serialVersionUID = -6_569_298_078_755_650_503L;

    private String name;
    protected String comments;
    protected Settings privateSettings = new Settings();

    protected transient boolean hasUndrawnChanges;
    private transient boolean hasUnsavedChanges;
    protected transient Sheet[] sheets;

    public AbstractGameComponent() {
        coreCheck();
        name = "";
        comments = "";
        hasUnsavedChanges = false;
        hasUndrawnChanges = false;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the component. If the new name is different from the
     * existing name, and there are sheets installed, then any sheets which are
     * not subclasses of {@link UndecoratedCardBack} will be marked as changed.
     * In addition, the component is marked as having unsaved changes. To change
     * this behaviour, you can override this method; see {@link #setNameImpl}.
     *
     * @param name the new name of the component
     * @throws NullPointerException if the name is {@code null}
     */
    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!this.name.equals(name)) {
            this.name = name;
            Sheet[] sheets = getSheets();
            if (sheets != null) {
                for (int i = 0; i < sheets.length; ++i) {
                    final Sheet s = sheets[i];
                    if (s != null && !(s instanceof UndecoratedCardBack)) {
                        markChanged(i);
                    }
                }
            }
            markUnsavedChanges();
        }
    }

    /**
     * Sets the component name without marking any sheets as changed or marking
     * the component as having unsaved changes. Subclasses may call this from
     * within an overridden {@link #setName} method to change the default
     * behaviour for marking sheets.
     *
     * @param name the new name of the component
     * @throws NullPointerException if the name is {@code null}
     */
    protected final void setNameImpl(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    @Override
    public AbstractGameComponent clone() {
        try {
            return (AbstractGameComponent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This base class implementation returns {@link #getName()}.
     */
    @Override
    public String getFullName() {
        return getName();
    }

    @Override
    public String getComment() {
        return comments;
    }

    /**
     * Sets the design rationale comment associated with this component. If the
     * comment is different from the existing comment, then
     * {@link #markUnsavedChanges()}.
     *
     * @param comment the new design comment
     * @throws NullPointerException if the comment is {@code null}
     */
    public void setComment(String comment) {
        if (comment == null) {
            throw new NullPointerException("comment");
        }
        if (!this.comments.equals(comment)) {
            this.comments = comment;
            markUnsavedChanges();
        }
    }

    /**
     * Set all character data to a neutral, blank state. Marks all character
     * sheets as changed, as well as marking the character unsaved. * /**
     * {@inheritDoc}
     *
     * <p>
     * The base class implementation w
     */
    @Override
    public void clearAll() {
        name = "";
        comments = "";
        if (sheets != null) {
            for (int i = 0; i < sheets.length; ++i) {
                markChanged(i);
            }
        }
        getSettings().reset(Expansion.EXPANSION_SETTING_KEY);
    }

    @Override
    public void setSheets(Sheet[] sheets) {
        this.sheets = sheets;
        hasUndrawnChanges = true;
    }

    @Override
    public Sheet[] getSheets() {
        return sheets;
    }

    @Override
    public abstract Sheet[] createDefaultSheets();

    /**
     * {@inheritDoc}
     *
     * <p>
     * The base class implementation will provide suitable localized titles for
     * most cases. It is designed to handle any number of alternating front and
     * back faces, with one exception. If there are exactly three sheets, then
     * it assumes that the third sheet is a token related to the first two.
     */
    @Override
    public String[] getSheetTitles() {
        if (sheets == null) {
            return null;
        }
        return getStandardSheetTitles(sheets.length);
    }

    static synchronized String[] getStandardSheetTitles(int numSheets) {
        if (ST_FRONT == null) {
            ST_FRONT = new String[]{string("front")};
            ST_FRONTBACK = new String[]{string("front"), string("back")};
            ST_FRONTBACKTOKEN = new String[]{string("front"), string("back"), string("marker")};
            ST_FRONTBACKFRONTBACK = new String[]{string("front"), string("back"), string("front"), string("back")};
        }
        switch (numSheets) {
            case 1:
                return ST_FRONT;
            case 2:
                return ST_FRONTBACK;
            case 3:
                return ST_FRONTBACKTOKEN;
            case 4:
                return ST_FRONTBACKFRONTBACK;
            default:
                String[] t = new String[numSheets];
                for (int i = 0; i < t.length;) {
                    t[i++] = string("front");
                    if (i < t.length) {
                        t[i++] = string("back");
                    }
                }
                return t;
        }
    }
    private static String[] ST_FRONT = null;
    private static String[] ST_FRONTBACK = null;
    private static String[] ST_FRONTBACKTOKEN = null;
    private static String[] ST_FRONTBACKFRONTBACK = null;

    @Override
    public void markChanged(int i) {
        if (sheets != null && sheets[i] != null) {
            sheets[i].markChanged();
        }
        markUnsavedChanges();
        hasUndrawnChanges = true;
    }

    /**
     * A convenience method that can be used to mark a default sheet or group of
     * sheets as having changed. The base class will call
     * {@link #markChanged(int)} for every sheet.
     */
    protected void markChanged() {
        if (sheets != null) {
            for (int i = 0; i < sheets.length; ++i) {
                markChanged(i);
            }
        }
    }

    @Override
    public boolean hasChanged() {
        boolean result = hasUndrawnChanges;
        hasUndrawnChanges = false;
        return result;
    }

    @Override
    public void markUnsavedChanges() {
        hasUnsavedChanges = true;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    @Override
    public void markSaved() {
        hasUnsavedChanges = false;
    }

    @Override
    public Settings getSettings() {
        return privateSettings;
    }

    @Override
    public boolean isDeckLayoutSupported() {
        return true;
    }

    @Override
    public void coreCheck() {
        // noop
    }

    /**
     * Given a string from a game component that may contain markup or other
     * special coding, return a copy of the string containing plain text with
     * all coding removed and newlines converted to spaces. Useful for printing
     * a display name.
     *
     * @param source the string to filter
     * @throws NullPointerException if the source string is {@code null}
     */
    @SuppressWarnings("fallthrough")
    public static String filterComponentText(String source) {
        StringBuilder b = new StringBuilder(source.length());
        final int TEXT = 0, TAG = 1, TAGQ = 2, TAGQBS = 3, TEXTBS = 4, TAGSTART = 5, TAGB = 6, TAGBR = 7, TAGSPACE = 8;
        int state = TEXT;

        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            switch (state) {
                case TEXTBS:
                    if (Character.isWhitespace(c)) {
                        continue;
                    }
                    state = TEXT;
                // !! fallthrough
                case TEXT:
                    if (c == '<') {
                        state = TAGSTART;
                    } else {
                        if (c == '\\') {
                            state = TEXTBS;
                        } else if (Character.isWhitespace(c)) {
                            b.append(' ');
                        } else {
                            b.append(c);
                        }
                    }
                    break;
                case TAGSTART:
                    if (c == 'b' || c == 'B') {
                        state = TAGB;
                    } else if (c == ' ') {
                        state = TAGSPACE;
                    } else if (c != '>') {
                        state = TAG;
                        break;
                    } else {
                        state = TEXT;
                    }
                    break;
                case TAGB:
                    if (c == 'r' || c == 'R') {
                        state = TAGBR;
                        break;
                    }
                // !! fallthrough
                case TAGSPACE:
                case TAGBR:
                case TAG:
                    if (c == '>') {
                        if (state == TAGBR || state == TAGSPACE) // convert <br> to space
                        {
                            b.append(' ');
                        }
                        state = TEXT;
                    } else if (c == '"') {
                        state = TAGQ;
                    } else {
                        state = TAG;
                    }
                    break;

                case TAGQ:
                    if (c == '\\') {
                        state = TAGQBS;
                    } else if (c == '"') {
                        state = TAG;
                    }
                    break;
                case TAGQBS:
                    state = TAGQ;
                    break;
                default:
                    throw new AssertionError("Unknown state in finite state machine");
            }
        }
        return b.toString();
    }

    /**
     * Given a portrait image key name (without the "-portrait-template"),
     * return the appropriate portrait image.
     */
    public BufferedImage getDefaultPortrait(String portraitKey) {
        return privateSettings.getImageResource(portraitKey + "-portrait-template");
    }

    /**
     * Duct tape for components not derived from this class.
     */
    static BufferedImage getDefaultPortrait(GameComponent gc, String portraitKey) {
        return gc.getSettings().getImageResource(portraitKey + "-portrait-template");
    }

    /**
     * Returns the largest scaling factor that, when multiplied by the given
     * image size, ensures that the image will match the ideal size in at least
     * one dimension. The image will either match the ideal size in the other
     * dimension, or else be larger than the other ideal dimension. If the image
     * is opaque, the result is the scaling factor to obtain the smallest image
     * with the same aspect ratio that would completely cover the ideal image
     * area.
     *
     * @param idealWidth the width of the area the image must cover
     * @param idealHeight the height of the area the image must cover
     * @param imageWidth the current width of the image to be fitted
     * @param imageHeight the current height of the image to be fitted
     * @return the scale that would ensure that the image would just cover the
     * specified area
     *
     * @deprecated Use
     * {@link ImageUtilities#idealCoveringScaleForImage(double, double, double, double)}.
     */
    public static double idealScaleForImage(double idealWidth, double idealHeight, double imageWidth, double imageHeight) {
        return ImageUtilities.idealCoveringScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
    }

    public double computeMinimumScaleForImage(BufferedImage image, String imageKey) {
        int idealWidth = privateSettings.getInt("ideal-" + imageKey + "-portrait-width");
        int idealHeight = privateSettings.getInt("ideal-" + imageKey + "-portrait-height");
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        return ImageUtilities.idealBoundingScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
    }

    public double computeIdealScaleForImage(BufferedImage image, String imageKey) {
        int idealWidth = privateSettings.getInt("ideal-" + imageKey + "-portrait-width");
        int idealHeight = privateSettings.getInt("ideal-" + imageKey + "-portrait-height");
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        return ImageUtilities.idealCoveringScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
    }

    /**
     * Duct tape for old components not derived from this class.
     */
    static double computeIdealScaleForImage(GameComponent gc, BufferedImage image, String imageKey) {
        Settings s = gc.getSettings();
        int idealWidth = s.getInt("ideal-" + imageKey + "-portrait-width");
        int idealHeight = s.getInt("ideal-" + imageKey + "-portrait-height");
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        return ImageUtilities.idealCoveringScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
    }

    /**
     * Returns a bitmap image for a user-supplied path. The path may name a
     * local file or be any of the special URL paths supported by
     * {@code StrangeImage}. If the path points to a vector image (and vector
     * support is installed), then the image will be converted to a bitmap
     * automatically, at a size and resolution based on
     *
     * @param path the path to locate an image for
     * @return a bitmap image for the path; if the path does not point to a
     * valid image, a stand-in "missing image" image will be returned
     *
     * @deprecated Use {@link StrangeImage#get} to load user-supplied images.
     */
    public static BufferedImage imagePathToImage(String path) {
        return StrangeImage.get(path).asBufferedImage();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    }

    /**
     * Provides default code to write this object's values from a subclass. For
     * historical reasons, nothing is written by this class's default
     * serialization mechanism. Subclasses therefore have to read and set the
     * name, comment, and settings instances themselves. This method can be
     * called from a subclass {@code writeObject} method to do this on behalf of
     * the subclass.
     *
     * @param out the stream to write to
     * @throws IOException if an I/O exception occurs
     */
    protected final void write(ObjectOutputStream out) throws IOException {
        out.writeObject(name);
        out.writeObject(comments);
        out.writeObject(privateSettings);
    }

    /**
     * Provides default code to read this object's values from a subclass. For
     * historical reasons, nothing is written by this class's default
     * serialization mechanism. Subclasses therefore have to read and write the
     * name, comment, and settings instances themselves. This method can be
     * called from a subclass {@code readObject} method to restore serialized
     * data written with {@link #write}.
     *
     * @param in the stream to read from
     * @throws IOException if an I/O exception occurs
     */
    protected final void read(ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = (String) in.readObject();
        comments = (String) in.readObject();
        privateSettings = (Settings) in.readObject();
    }
}
