package ca.cgjennings.apps.arkham.diy;

import ca.cgjennings.apps.arkham.PortraitPanel;
import ca.cgjennings.apps.arkham.component.AbstractGameComponent;
import ca.cgjennings.apps.arkham.component.AbstractPortrait;
import ca.cgjennings.apps.arkham.component.DefaultPortrait;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.Portrait;
import ca.cgjennings.apps.arkham.component.Portrait.Feature;
import ca.cgjennings.apps.arkham.component.PortraitProvider;
import ca.cgjennings.apps.arkham.component.conversion.ConversionSession;
import ca.cgjennings.apps.arkham.component.conversion.UpgradeConversionTrigger;
import ca.cgjennings.apps.arkham.plugins.PluginContextFactory;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.sheet.MarkerStyle;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.apps.arkham.sheet.Sheet.DeckSnappingHint;
import ca.cgjennings.apps.arkham.sheet.UndecoratedCardBack;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.io.NewerVersionException;
import ca.cgjennings.io.SEObjectInputStream;
import ca.cgjennings.io.SEObjectOutputStream;
import gamedata.Game;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.swing.text.JTextComponent;
import resources.Language;
import static resources.Language.string;
import resources.ResourceKit;
import resources.Settings;

/**
 * A scriptable {@link GameComponent}. A DIY component delegates key
 * functionality to an object that implements the {@link Handler} interface.
 * This object is typically derived from script code, and so terms like "script"
 * and "script function" will be used throughout this document to refer to the
 * parts of the component that are implemented by the {@code Handler} proxy, but
 * it should be understood that compiled code can also subclass DIY and provide
 * its own {@code Handler} instance.
 *
 * <a name='locked'><b>Restricted Properties:</b></a>
 * Several properties of DIY components are <i>restricted</i>. Attempts to
 * change a restricted property except from your script's {@code create} or
 * {@code onRead} functions will cause an {@code IllegalStateException} to be
 * thrown. Restricted properties control the component's basic structure and
 * features, such as the number of faces it consists of. These properties cannot
 * be changed dynamically: they are normally set within the component's
 * {@code create} script function and never changed again. To allow cards to
 * evolve over time, they can also be changed within the script's {@code onRead}
 * function. So for example, a component that starts with no portrait could
 * later add one by checking for the previous version in the {@code onRead}
 * function (see {@link #setVersion}), and if found, defining a portrait key
 * (see {@link #setPortraitKey}).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class DIY extends AbstractGameComponent implements Handler {

    /**
     * An enumeration of the different kinds of component layouts that are
     * supported by DIY components.
     *
     * @see #setFaceStyle
     */
    public enum FaceStyle {
        /**
         * The component is a card or other object with only a font face.
         */
        ONE_FACE(1),
        /**
         * The component is a card or other object with two faces, each of which
         * has its own painter.
         */
        TWO_FACES(2),
        /**
         * The component is a card or other object with two faces, but both
         * faces are identical.
         */
        SHARED_FACE(2),
        /**
         * The component has two faces, but the back is just a static image. The
         * image specified by the back image template key will be used to create
         * a a back face painter automatically.
         */
        PLAIN_BACK(2),
        /**
         * The component consists of a total of four faces, but only three are
         * shown. This is a special case to handle the commonly found
         * combination of a character card with an accompanying marker. The
         * first two faces of the component represent the character card, while
         * the third face represents one side of the marker (the other side is a
         * mirror image). If the default portrait handling system is used, then
         * the component will have an additional portrait panel to adjust the
         * location and scale of the portrait on the marker (the image will be
         * identical to the main portrait image).
         */
        CARD_AND_MARKER(3),
        /**
         * The component consists of a total of four faces. Typically, the
         * component is a pair of closely related cards. The front painting
         * functions are called for the first and third face, and the back
         * painting functions are called for the second and fourth face. You can
         * tell which face is requested by calling the
         * {@link DIYSheet#getSheetIndex()} method of the passed-in sheet.
         */
        FOUR_FACES(4),
        /**
         * The component consists of a total of six faces. Typically, the
         * component is a triple of closely related cards, or a pair of cards
         * and a double-sided marker. The front painting functions are called
         * for the odd-numbered faces (first, third, fifth), and the back
         * painting functions are called for the even-numbered faces (second,
         * fourth, sixth). You can tell which face is requested by calling the
         * {@link DIYSheet#getSheetIndex()} method of the passed-in sheet.
         */
        SIX_FACES(6),
        /**
         * The component consists of a total of eight faces. Typically, the
         * component is a quadruple of closely related cards, or a triple of
         * cards and a double-sided marker. The front painting functions are
         * called for the odd-numbered faces (first, third, fifth, seventh), and
         * the back painting functions are called for the even-numbered faces
         * (second, fourth, sixth, eighth). You can tell which face is requested
         * by calling the {@link DIYSheet#getSheetIndex()} method of the
         * passed-in sheet. Eight faces is about the limit of what a user can
         * reasonably work with from a single editor; if you have more related
         * faces than this, try to find a way to split the design into two or
         * more editors. You can always add a button to create or import the
         * common content for the second editor automatically from the first.
         */
        EIGHT_FACES(8);

        private final int faces;

        private FaceStyle(int faceCount) {
            faces = faceCount;
        }

        /**
         * Returns the number of faces (sheets) that are used by this face
         * style. For example, {@code SHARED_FACE.getFaceCount()} would return
         * 2, while {@code SIX_FACES.getFaceCount()} would return 6.
         *
         * @return the number of sheets associated with this face style
         */
        public int getFaceCount() {
            return faces;
        }
    }

    /**
     * The valid modes for high resolution image substitution. Images that are
     * painted using {@link DIYSheet} by resource key can automatically
     * substitute a higher-resolution image when available.
     *
     * @see #setHighResolutionSubstitutionMode
     */
    public enum HighResolutionMode {
        /**
         * Never substitute high resolution images. This setting is used mainly
         * for testing.
         */
        DISABLE,
        /**
         * Allow substitution when the component is being drawn at a resolution
         * higher than the template resolution.
         */
        ENABLE,
        /**
         * Force substitution if possible. This setting is used mainly for
         * testing.
         */
        FORCE
    }

    private String handlerScript;
    private String extensionName;
    private int cardVersion = 1;
    private FaceStyle faceStyle = FaceStyle.PLAIN_BACK;
    private String frontTemplateKey = "diy-front-sheet";
    private String backTemplateKey = "diy-front-sheet";
    private String thirdTemplateKey = "diy-front-sheet";
    private String fourthTemplateKey = "diy-front-sheet";
    private String fifthTemplateKey = "diy-front-sheet";
    private String sixthTemplateKey = "diy-front-sheet";
    private String seventhTemplateKey = "diy-front-sheet";
    private String eighthTemplateKey = "diy-front-sheet";
    private String[] customSheetTitles;
    private String portraitKey;
    private String portraitImage;
    private double portraitScale, portraitPanX, portraitPanY;
    private double markerScale, markerPanX, markerPanY;
    private MarkerStyle markerStyle = MarkerStyle.COPIED;
    private transient BufferedImage portrait;

    private int flags = 0;
    private double bleedMargin = 0d;
    boolean hasExplicitBleedMargin = false;
    private double cornerRadius = 0d;
    boolean hasExplicitCornerRadius = false;
    private double[][] customFoldMarks = null;
    private HighResolutionMode highResMode = HighResolutionMode.ENABLE;
    private Sheet.DeckSnappingHint deckSnappingHint = Sheet.DeckSnappingHint.CARD;

    private transient boolean locked = false;
    private transient JTextComponent nameField = null;
    private transient boolean scriptDebug;
    private transient ScriptMonkey monkey;
    private transient Handler handler;
    private transient UpgradeConversionTrigger upgradeConversionTrigger = null;

    static final int OPT_NO_PORTRAIT_FILL = 1;
    static final int OPT_NO_QUALITY_INIT = 1 << 1;
    static final int OPT_TRANSPARENT = 1 << 2;
    static final int OPT_VARIABLE_SIZE = 1 << 3;
//  static final int OPT_BOARD_OVERLAY = 1<<4; // unused
    static final int OPT_MIN_PORTRAIT_SCALE = 1 << 5;
    static final int OPT_NO_PORTRAIT_CLIP = 1 << 6;
    static final int OPT_OWN_PORTRAIT_HANDLING = 1 << 7;
    static final int OPT_NO_MARKER_FILL = 1 << 8;
    static final int OPT_MIN_MARKER_SCALE = 1 << 9;
    static final int OPT_NO_MARKER_CLIP = 1 << 10;
    static final int AVAILABLE_FLAGS = (1 << 11) - 1;

    /**
     * Creates a new DIY component that will call into the given handler. If the
     * game code represents a registered game, the new component's settings will
     * inherit from the settings for that game.
     *
     * @param handler the handler instance that will be called by this component
     * to perform customizable functions
     * @param gameCode the code for a game to associate with this component
     * @throws NullPointerException if the handler is {@code null}
     */
    protected DIY(Handler handler, String gameCode) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        try {
            handlerScript = null;
            this.handler = handler;
            init(gameCode);
        } catch (IOException e) {
            throw new AssertionError(
                    "unexpected IOException while instantiating DIY subclass "
                    + getClass().getName() + ": " + e
            );
        }
    }

    /**
     * Creates a new DIY component that will call into the given handler. If the
     * game code represents a registered game, the new component's settings will
     * inherit from the settings for that game. If the {@code debug} flag is set
     * to {@code true}, then a breakpoint will be set at the start of handler
     * script.
     *
     * @param handlerScript the location of the script resource that defines the
     * handler functions that will be called by this component to perform
     * customizable functions
     * @param gameCode the code for a game to associate with this component, or
     * {@code null}
     * @param debug if {@code true}, then a breakpoint is set just before
     * executing the handler script; initialization of the handler can be
     * stepped through from the script debugger
     * @throws IOException if an exception occurs while loading the script or
     * starting the handler
     * @throws NullPointerException if the handler script is {@code null}
     */
    public DIY(String handlerScript, String gameCode, boolean debug) throws IOException {
        if (handlerScript == null) {
            throw new NullPointerException("handlerScript");
        }

        scriptDebug = debug;
        this.handlerScript = handlerScript;
        init(gameCode);
    }

    /**
     * This helper method can be called to create a new component for testing
     * purposes using just a {@link Handler} implementation. (This method is
     * called by the {@code testDIYScript()} function in the {@code diy} script
     * library.) Note that if you save a test instance, the saved file will not
     * open correctly since the component will not know what script file to load
     * to recreate the handler.
     *
     * @param h the handler instance to test
     * @param gameCode a game to associate with the component, or {@code null}
     * @return a new DIY instance that will call into the provided handler
     */
    public static DIY createTestInstance(Handler h, String gameCode) {
        DIY diy = new DIY(h, gameCode);
        return diy;
    }

    /**
     * Creates a {@link Handler} instance from this component's handler script.
     * The handler script is a resource identifier or fully qualified class name
     * that was set when the component was constructed. If the component doesn't
     * have a handler script, does nothing and returns.
     *
     * @throws IOException if an exception occurs while trying to load or
     * execute the script, or instantiate the class named by the script resource
     * @see #DIY(java.lang.String, java.lang.String, boolean)
     */
    private void createHandler() throws IOException {
        if (handlerScript == null) {
            return;
        }

        if (!handlerScript.endsWith(".js")) {
            try {
                // try to create the handler from a class
                handler = (Handler) Class.forName(handlerScript).getConstructor().newInstance();
                return;
            } catch (ClassNotFoundException ex) {
                // doesn't exist, will try to load this as a script
            } catch (Exception ex) {
                throw new IOException("failed to instantiate handler class " + handlerScript, ex);
            }
        }

        String script = handlerScript;
        if (!script.contains(":/")) {
            script = "res://" + script;
        }
        try {
            monkey = new ScriptMonkey(script);
            monkey.setSettingProvider(getSettings());
            monkey.bind(PluginContextFactory.createDummyContext());
            monkey.setBreakpoint(scriptDebug);
            Object retVal = monkey.eval(ScriptMonkey.getLibrary(script));
            if (!(retVal instanceof ScriptException)) {
                try {
                    handler = monkey.implement(Handler.class);
                } catch (IllegalArgumentException e) {
                    ScriptMonkey.getSharedConsole().getErrorWriter().println(
                            "DIY script does not implement Handler functions: " + script
                    );
                }
            }
        } catch (FileNotFoundException e) {
            if (extensionName != null) {
                ca.cgjennings.apps.arkham.dialog.ErrorDialog.displayError(string("plug-err-diy-missing-1", extensionName), e);
            }
            throw new FileNotFoundException(string("plug-err-diy-missing-2", script));
        } catch (IOException e) {
            e.printStackTrace(ScriptMonkey.getSharedConsole().getErrorWriter());
        }

        if (handler == null) {
            throw new IOException("could not create DIY handler, cause unknown: " + script);
        }
    }

    /**
     * Sets the game code in private settings, calls {@link #createHandler()} to
     * create the handler instance from the identifier, and calls
     * {@link #create} to invoke the handler's create method.
     *
     * @param gameCode the code fro the game to associate with the component, or
     * {@code null}
     * @throws IOException if an exception occurs while trying to read the
     * handler script
     * @see #DIY(java.lang.String, java.lang.String, boolean)
     */
    private void init(String gameCode) throws IOException {
        // do not automatically associate this component with any particular game
        if (gameCode != null) {
            privateSettings.set(Game.GAME_SETTING_KEY, gameCode);
        }
        locked = false;
        try {
            createHandler();
            create(this);
        } finally {
            locked = true;
        }
    }

    @Override
    public String getClassName() {
        if (handlerScript == null) {
            return super.getClassName();
        }
        return "diy:" + getHandlerScript();
    }

    /**
     * Returns the resource name used to create a {@link Handler} for this
     * component, or {@code null} if the component was created directly from a
     * {@link Handler} instance. Typically the resource used to create a handler
     * is a script file, but it can also be the name of a class on the class
     * path that implements the {@code Handler} interface.
     *
     * @return the resource used to create this component, or {@code null}
     */
    public String getHandlerScript() {
        return handlerScript;
    }

    /**
     * Returns an extension name that can be reported to the user as the plug-in
     * required for this component to load correctly.
     *
     * @return the extension plug-in that adds this component type to the class
     * map
     */
    public String getExtensionName() {
        return extensionName;
    }

    /**
     * Sets the name of an extension to report to the user if the script
     * required by this component cannot be found.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param extensionName extension required to instantiate this component
     */
    public void setExtensionName(String extensionName) {
        checkPropertyLock();
        this.extensionName = extensionName;
    }

    /**
     * Returns the version number of this component. The version number is
     * stored in the component's save file and can be used to transparently
     * upgrade components saved from older versions of the component's handler
     * script.
     *
     * @return the version number of the component
     * @see #setVersion
     */
    public int getVersion() {
        return cardVersion;
    }

    /**
     * Sets the version number of this component. The version number should be
     * when the component is created. When a new, incompatible version of the
     * script is released, the next higher version number can be set. Then, in
     * the component's {@link Handler#onRead} function, check for older versions
     * and upgrade the component as necessary (for example, assign default
     * values to any new settings used by the component).
     *
     * <p>
     * <b>Note:</b> The version numbers are integers; although you can assign a
     * value like 1.1 from a script file, all decimal digits will be dropped.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param version the version number you have assigned to this component
     * @see #getVersion
     */
    public void setVersion(int version) {
        checkPropertyLock();
        this.cardVersion = version;
    }

    /**
     * @deprecated Replaced by {@link #getVersion()}.
     */
    public final int getCardVersion() {
        return getVersion();
    }

    /**
     * @deprecated Replaced by {@link #setVersion(int)}.
     */
    @Deprecated
    public final void setCardVersion(int version) {
        setVersion(version);
    }

    /**
     * Returns the {@link FaceStyle} used by this card.
     *
     * @return the value that determines the number and type of card faces
     * presented by this component
     */
    public FaceStyle getFaceStyle() {
        return faceStyle;
    }

    /**
     * Sets the {@link FaceStyle} used by this card. The face style controls the
     * number and style of the faces presented by this component.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param faceStyle the value that determines the number and type of card
     * faces used by this component
     * @throws NullPointerException if the style is {@code null}
     */
    public void setFaceStyle(FaceStyle faceStyle) {
        if (faceStyle == null) {
            throw new NullPointerException("faceStyle");
        }
        checkPropertyLock();
        this.faceStyle = faceStyle;
    }

    /**
     * Returns the template key for the front face of this component. This is
     * equivalent to {@code getTemplateKey( 0 )}.
     *
     * @return the front face template key
     * @see #setTemplateKey
     */
    public String getFrontTemplateKey() {
        return frontTemplateKey;
    }

    /**
     * Sets the template key for the front face of this component. This is
     * equivalent to {@code setTemplate( 0, frontTemplateKey )}.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param frontTemplateKey the template key to use for the front face
     * @throws NullPointerException if the key is {@code null}
     * @see #setTemplateKey
     */
    public void setFrontTemplateKey(String frontTemplateKey) {
        checkPropertyLock();
        if (frontTemplateKey == null) {
            throw new NullPointerException("frontTemplateKey");
        }
        this.frontTemplateKey = frontTemplateKey;
    }

    /**
     * Returns the template key for the back face of this component. This is
     * equivalent to {@code getTemplateKey( 1 )}.
     *
     * @return the back face template key
     * @see #setTemplateKey
     */
    public String getBackTemplateKey() {
        return backTemplateKey;
    }

    /**
     * Sets the template key for the back face of this component. This is
     * equivalent to {@code setTemplate( 1, backTemplateKey )}.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param backTemplateKey the base template key name to use for the back
     * face
     * @throws NullPointerException if the key is {@code null}
     * @see #setTemplateKey
     */
    public void setBackTemplateKey(String backTemplateKey) {
        checkPropertyLock();
        if (backTemplateKey == null) {
            throw new NullPointerException("backTemplateKey");
        }
        this.backTemplateKey = backTemplateKey;
    }

    /**
     * Returns the base template key name for the face with the given index.
     *
     * @param index the index of the face to obtain a key for (0 for the first
     * front face, 1 for the first back face, 2 for the second front face, and
     * so on)
     * @return the base template key for specified face
     * @throws IndexOutOfBoundsException if the face index is invalid for the
     * face style
     */
    public String getTemplateKey(int index) {
        String k;
        checkFaceIndex(index);
        switch (index) {
            case 0:
                k = frontTemplateKey;
                break;
            case 1:
                k = backTemplateKey;
                break;
            case 2:
                k = thirdTemplateKey;
                break;
            case 3:
                k = fourthTemplateKey;
                break;
            case 4:
                k = fifthTemplateKey;
                break;
            case 5:
                k = sixthTemplateKey;
                break;
            case 6:
                k = seventhTemplateKey;
                break;
            case 7:
                k = eighthTemplateKey;
                break;
            default:
                throw new IndexOutOfBoundsException("index: " + index);
        }
        return k;
    }

    /**
     * Sets the template key for the face with the given index. The template key
     * is the base name for a collection of setting keys that define the
     * properties of the template for that face, including its size and
     * background image. The following setting keys are derived from the base
     * key name:
     *
     * <dl>
     * <dt><i>templateKey</i>-template
     * <dd>the resource file that contains the image
     * <dt><i>templateKey</i>-ppi
     * <dd>if defined, the resolution of the template image in pixels per inch
     * (2.54cm) (default is <tt>150</tt>); the suffix -dpi can also be used
     * <dt><i>templateKey</i>-expsym-region
     * <dd>if defined, the region where the expansion symbol is drawn (default
     * is no expansion symbol)
     * <dt><i>templateKey</i>-expsym-invert
     * <dd>if defined, the default index of the variant style to use for the
     * expansion symbol (default is 0)
     * <dt><i>templateKey</i>-upsample
     * <dd>the upsample factor for previews (default is <tt>1.0</tt>); this is a
     * multiplier for the card resolution to be used when previewing the
     * component; it is sometimes useful for small templates that use small text
     * (6 points or so)
     * </dl>
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param index the index of the face to set the template for (0 for the
     * first front face, 1 for the first back face, 2 for the second front face,
     * and so on)
     * @param templateKey the base template key name for the face
     * @throws NullPointerException if the key is {@code null}
     * @throws IndexOutOfBoundsException if the index is not valid for the face
     * style
     * @see #getTemplateKey
     * @see #setFaceStyle
     */
    public void setTemplateKey(int index, String templateKey) {
        checkPropertyLock();
        checkFaceIndex(index);
        // a bit more awkward than necessary since the original design assumed
        // there would only be a front and back key and the serialization format
        // needs to remain compatible
        if (templateKey == null) {
            throw new NullPointerException("templateKey");
        }
        switch (index) {
            case 0:
                frontTemplateKey = templateKey;
                break;
            case 1:
                backTemplateKey = templateKey;
                break;
            case 2:
                thirdTemplateKey = templateKey;
                break;
            case 3:
                fourthTemplateKey = templateKey;
                break;
            case 4:
                fifthTemplateKey = templateKey;
                break;
            case 5:
                sixthTemplateKey = templateKey;
                break;
            case 6:
                seventhTemplateKey = templateKey;
                break;
            case 7:
                eighthTemplateKey = templateKey;
                break;
            default:
                // should never happen unless checkFaceIndex is buggy
                throw new AssertionError();
        }
    }

    /**
     * Sets a custom title for the sheet with the specified index. Sheet titles
     * are used to describe the purpose of each sheet in the user interface.
     * Typical sheet titles include "Front Face", "Back Face", and "Marker"
     * (localized for the interface language). If you do not set any custom
     * titles, then the titles for this component's sheets will be generated
     * automatically based on the {@code FaceStyle}.
     *
     * <p>
     * If the title that you set begins with an '@' character, then the sheet's
     * true title will be
     * {@linkplain Language#string(java.lang.String) looked up} using the active
     * user interface language
     * {@linkplain #getSheetTitles() whenever the titles are requested}. This
     * allows you to set a custom sheet title once (during {@code onCreate}) and
     * yet get a correctly localized title when the resulting component is saved
     * and later reopened using a different language setting.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param index the index of the sheet whose title should be modified
     * @param title the new title to set
     * @see #getSheetTitles()
     * @throws NullPointerException if the title is {@code null}
     * @throws IllegalArgumentException if the index is not valid for the
     * {@code FaceStyle}
     */
    public void setSheetTitle(int index, String title) {
        checkFaceIndex(index);
        if (title == null) {
            throw new NullPointerException("title");
        }

        if (customSheetTitles == null) {
            customSheetTitles = new String[MAX_FACES];
        }

        customSheetTitles[index] = title;
    }

    /**
     * Returns a copy of the human-readable names of the sheets used by this
     * component. A typical result would be something like
     * {@code ["Front Face", "Back Face"]}, localized for the user interface
     * language. Note that, since this returns a copy of the current titles,
     * changing the values of the returned array will have no effect on the
     * titles used by this component. To change a sheet title, you must instead
     * call {@link #setSheetTitle(int, java.lang.String)}.
     *
     * <p>
     * If no custom sheet titles have ever been set on this component, then this
     * will return an array of default titles based on the face style.
     *
     * @return an array of sheet titles matching the assigned sheets, or
     * {@code null} if there are no sheets attached
     * @see #createDefaultSheets
     */
    @Override
    public String[] getSheetTitles() {
        if (getSheets() == null) {
            return null;
        }
        // unlike the super class, we always return a copy of the titles
        // in case the DIY user modifies the returned array
        if (customSheetTitles == null) {
            return super.getSheetTitles().clone();
        }

        String[] defaultTitles = null;
        String[] titles = Arrays.copyOf(customSheetTitles, faceStyle.getFaceCount());
        for (int i = 0; i < titles.length; ++i) {
            if (titles[i] == null) {
                if (defaultTitles == null) {
                    defaultTitles = super.getSheetTitles();
                }
                titles[i] = defaultTitles[i];
            } else if (!titles[i].isEmpty() && titles[i].charAt(0) == '@') {
                titles[i] = Language.string(titles[i].substring(1));
            }
        }
        return titles;
    }

    /**
     * Returns the bleed margin for this component, in points.
     *
     * @return the component's bleed margin, in points
     */
    public double getBleedMargin() {
        return bleedMargin;
    }

    /**
     * Sets the size of the bleed margin for this component, in points. This can
     * be set if the component's template images include a bleed margin as part
     * of their design so that Strange Eons will take this into account when
     * rendering sheets with bleed margins enabled. This will affect all of the
     * component's faces (sheets). (The default is 0, meaning there is no bleed
     * margin included in the design.)
     *
     * <p>
     * If no bleed margin is set using this method, each sheet will use a bleed
     * margin set by the setting key
     * <i>templateKey</i>{@code -bleed-margin}, or 0 if the key is not set.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param marginInPoints the new bleed margin; this is used to adjust the
     * location of the component's crop marks in the deck editor
     * @throws IllegalArgumentException if the margin is negative
     * @see #getBleedMargin
     */
    public void setBleedMargin(double marginInPoints) {
        checkPropertyLock();
        if (marginInPoints < 0d) {
            throw new IllegalArgumentException("bleed margin cannot be negative: " + marginInPoints);
        }
        hasExplicitBleedMargin = true;
        bleedMargin = marginInPoints;
    }

    /**
     * Returns the corner radius for this component, in points.
     *
     * @return the component's corner radius, in points
     */
    public double getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Sets the radius for rounding the corners of this component, in points.
     * This will affect all of the component's faces (sheets). (The default is
     * 0, leaving the corners sharp.)
     *
     * <p>
     * If no corner radius is set using this method, each sheet will use a
     * separate radius set by the setting key
     * <i>templateKey</i>{@code -corner-radius}, or 0 if the key is not set.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param radiusInPoints the new corner radius; this is used to round the
     * corners of the component when trimming the edges.
     * @throws IllegalArgumentException if the radius is negative
     * @see #getCornerRadius
     */
    public void setCornerRadius(double radiusInPoints) {
        checkPropertyLock();
        if (radiusInPoints < 0d) {
            throw new IllegalArgumentException("corner radius cannot be negative: " + radiusInPoints);
        }
        hasExplicitCornerRadius = true;
        cornerRadius = radiusInPoints;
    }

    /**
     * Returns the high resolution image substitution mode for this component.
     *
     * @return the component's high resolution image substitution mode setting
     * @see #setHighResolutionSubstitutionMode
     * @see HighResolutionMode
     */
    public HighResolutionMode getHighResolutionSubstitutionMode() {
        return highResMode;
    }

    /**
     * Sets the high resolution image substitution mode used by the component.
     * When substitution is active, certain image drawing methods defined on
     * {@link DIYSheet} will automatically replace a standard resolution image
     * with a resolution version. This setting is normally only changed from its
     * default value for testing purposes.
     *
     * @param mode the high resolution image substitution mode for this
     * component
     * @throws NullPointerException if the mode is {@code null}
     * @see #getHighResolutionSubstitutionMode
     * @see HighResolutionMode
     */
    public void setHighResolutionSubstitutionMode(HighResolutionMode mode) {
        if (mode == null) {
            throw new NullPointerException("mode");
        }
        highResMode = mode;
        if (sheets != null) {
            for (int i = 0; i < sheets.length; ++i) {
                if (sheets[i] != null) {
                    sheets[i].markChanged();
                }
            }
        }
    }

    /**
     * Returns a copy of the custom fold mark array for the specified component
     * face, or {@code null} if the face has no custom fold marks.
     *
     * @param faceIndex the index of the face of interest
     * @return the custom fold marks set on the face, or {@code null}
     * @throws IndexOutOfBoundsException if the face index is invalid for the
     * face style
     */
    public final double[] getCustomFoldMarks(int faceIndex) {
        checkFaceIndex(faceIndex);
        if (customFoldMarks == null) {
            return null;
        }
        return customFoldMarks[faceIndex].clone();
    }

    /**
     * Sets an array of custom fold marks to be used by this component. For more
     * information on custom fold marks, see {@link Sheet#getFoldMarks()}.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param faceIndex the index of the face that the fold marks will appear on
     * @param foldMarkTuples the fold mark unit vectors to set
     * @throws IllegalArgumentException if the length of the fold mark array is
     * not a multiple of 4 or the vectors it defines do not have unit length
     * @throws IndexOutOfBoundsException if the face index is invalid for the
     * face style
     */
    public final void setCustomFoldMarks(int faceIndex, double[] foldMarkTuples) {
        checkPropertyLock();
        checkFaceIndex(faceIndex);

        if (foldMarkTuples != null) {
            if ((foldMarkTuples.length & 3) != 0) {
                throw new IllegalArgumentException(
                        "foldMarkTuples must have a multiple of 4 entries (m1x1, m1y1, m1x2, m1y2...): " + foldMarkTuples.length
                );
            }
            final double EPSILON = 0.5d / 72d;
            for (int i = 0; i < foldMarkTuples.length; i += 4) {
                double dx = foldMarkTuples[i] - foldMarkTuples[i + 2];
                double dy = foldMarkTuples[i + 1] - foldMarkTuples[i + 3];
                double distSq = dx * dx + dy * dy;
                if (Math.abs(distSq - 1d) > EPSILON) {
                    throw new IllegalArgumentException(String.format(
                            "foldMarkTuple starting at %d  does not have a unit (length=1) vector: (%f,%f)-(%f,%f)",
                            i, foldMarkTuples[i], foldMarkTuples[i + 1], foldMarkTuples[i + 2], foldMarkTuples[i + 3]
                    ));
                }
            }
        }

        if (customFoldMarks == null && foldMarkTuples != null) {
            customFoldMarks = new double[MAX_FACES][];
        }
        customFoldMarks[faceIndex] = foldMarkTuples;
    }

    /**
     * A faster version of {@link #getCustomFoldMarks(int)}, without error
     * checking, for use by {@link DIYSheet}. This returns the actual array, not
     * a copy, so it should be treated as read-only.
     *
     * @param faceIndex the requested face index
     * @return the internal fold mark array for the requested face
     */
    final double[] getCustomFoldMarksInternal(int faceIndex) {
        if (customFoldMarks == null) {
            return null;
        }
        return customFoldMarks[faceIndex];
    }

    /**
     * Checks a sheet index against the component's face style and throws
     * {@code IndexOutOfBoundsException} if it is invalid.
     *
     * @param index the index to check
     */
    private void checkFaceIndex(int index) {
        final boolean ok;
        if (index < 0) {
            ok = false;
        } else {
            ok = index < faceStyle.getFaceCount();
        }
        if (!ok) {
            throw new IndexOutOfBoundsException("invalid face index: " + index);
        }
    }

    // N.B. THERE ARE SERIALIZED ARRAYS THAT DEPEND ON THIS VALUE;
    // IF IT CHANGES, THE ARRAYS WILL NEED TO BE ADAPTED ON DESERIALIZATION
    private static final int MAX_FACES = 8;

    /**
     * Returns the DIY's feature bit flags.
     *
     * @return a bit mask of enabled features
     */
    final int getFlags() {
        return flags;
    }

    private void setFeature(int feature, boolean enable) {
        checkPropertyLock();
        if (enable) {
            flags |= feature;
        } else {
            flags &= ~feature;
        }
    }

    private boolean getFeature(int feature) {
        return (flags & feature) != 0;
    }

    /**
     * Returns {@code true} if the component's sheet uses translucent pixels.
     *
     * @return {@code true} if the component's sheets include an alpha channel
     * @see #setTransparentFaces
     */
    public final boolean getTransparentFaces() {
        return getFeature(OPT_TRANSPARENT);
    }

    /**
     * This flag must be set if the card faces require support for translucent
     * pixels; for example, if the faces have a nonrectangular shape.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param transparent {@code true} if the component's sheets require an
     * alpha channel
     * @see #getTransparentFaces
     */
    public final void setTransparentFaces(boolean transparent) {
        setFeature(OPT_TRANSPARENT, transparent);
    }

    /**
     * Returns {@code true} if the card faces can change in size.
     *
     * @return {@code true} if variable-sized faces are enabled
     * @see #setVariableSizedFaces
     */
    public final boolean getVariableSizedFaces() {
        return getFeature(OPT_VARIABLE_SIZE);
    }

    /**
     * Sets whether faces can change in size. If {@code true}, then transparent
     * faces will also automatically be enabled. When this option is enabled,
     * sheets have their edges trimmed of transparent pixels before being
     * returned. The easiest way to implement variably-sized faces is to use a
     * template image that matches the largest possible size that is desired; if
     * no maximum size is known ahead of time, you must replace the template
     * image with an appropriately sized template at the start of each rendering
     * pass.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param variable if {@code true}, faces may vary in size
     * @see #getVariableSizedFaces
     */
    public final void setVariableSizedFaces(boolean variable) {
        if (variable) {
            setFeature(OPT_VARIABLE_SIZE | OPT_TRANSPARENT, true);
        } else {
            setFeature(OPT_VARIABLE_SIZE, false);
        }
    }

    /**
     * Returns the deck snapping hint for the component. The deck snapping hint
     * defines the default snapping behaviour for the component when it is
     * placed in a deck editor.
     *
     * @return the default snapping behaviour for the component
     * @see #setDeckSnappingHint
     */
    public DeckSnappingHint getDeckSnappingHint() {
        return deckSnappingHint;
    }

    /**
     * Sets the default deck snapping behaviour when the component is placed in
     * a deck editor.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param deckSnappingHint a hint describing the default snapping behaviour
     * @throws NullPointerException if the hint is {@code null}
     * @see #getDeckSnappingHint
     */
    public void setDeckSnappingHint(DeckSnappingHint deckSnappingHint) {
        checkPropertyLock();
        if (deckSnappingHint == null) {
            throw new NullPointerException("deckSnappingHint");
        }
        this.deckSnappingHint = deckSnappingHint;
    }

    /**
     * Returns the base key name that controls the default portrait handling
     * system. Note that this key will be ignored if custom portrait handling is
     * enabled.
     *
     * @return the portrait key base name
     * @see #setPortraitKey
     * @see #getPortrait
     */
    public String getPortraitKey() {
        if (isCustomPortraitHandling()) {
            return null;
        }
        return portraitKey;
    }

    /**
     * Sets the base portrait key for this component. The base portrait key is
     * used to compose a group of setting keys that control the built-in
     * portrait manager. The key will be {@code null} if custom portrait
     * handling is enabled, and trying to set the key to non-{@code null} value
     * while custom portrait handling is enabled will result in an
     * {@code IllegalStateException} being thrown. When custom portrait handling
     * is disabled, setting the key to {@code null} will disable portraits for
     * the component ({@link #getPortraitCount()} will return 0). If the key is
     * non-{@code null}, then the following keys will be referred to when
     * setting up the portrait system:
     * <dl>
     * <dt><i>x</i>-portrait-template
     * <dd>the resource file that contains the default portrait image
     * <dt><i>x</i>-portrait-clip-region
     * <dd>this is the region where the portrait will be drawn on the card
     * <dt><i>x</i>-portrait-scale
     * <dd>if defined, sets the scale for the default portrait; otherwise the
     * scale is determined automatically to fit the portrait in the clip region
     * (a scale of 1 = 100%)
     * <dt><i>x</i>-portrait-panx
     * <dd>if defined, the horizontal pan of the default portrait (default is 0)
     * <dt><i>x</i>-portrait-pany
     * <dd>if defined, the vertical pan of the default portrait (default is 0)
     * </dl>
     *
     * <p>
     * If the face style is {@link FaceStyle#CARD_AND_MARKER}, then an
     * additional group of setting keys is used to determine the clip region,
     * and the scale and position of the default portrait on the marker (the
     * default portrait image will be the same).
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param portraitKey the base portrait key, or {@code null} to disable
     * portraits
     * @throws IllegalStateException if the key is not {@code null} and custom
     * portrait handling is enabled
     * @see #setCustomPortraitHandling
     * @see #getPortrait
     * @see #setFaceStyle
     */
    public void setPortraitKey(String portraitKey) {
        checkPropertyLock();
        if (isCustomPortraitHandling() && portraitKey != null) {
            throw new IllegalStateException("custom portrait handling is enabled");
        }
        this.portraitKey = portraitKey;
    }

    /**
     * Returns {@code true} if portrait areas will be filled with solid white
     * before painting the portrait.
     *
     * @return {@code true} if background filling is enabled
     */
    public final boolean isPortraitBackgroundFilled() {
        return !getFeature(OPT_NO_PORTRAIT_FILL);
    }

    /**
     * If set, the portrait clip region will be filled in with solid white
     * before painting the portrait. This is usually turned off when the user is
     * expected to use portraits that have transparency because the portrait is
     * painted over a background illustration.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param fill if {@code true}, the clip region will be filled before
     * painting when {@link DIYSheet#paintPortrait(java.awt.Graphics2D)} is
     * called
     */
    public final void setPortraitBackgroundFilled(boolean fill) {
        setFeature(OPT_NO_PORTRAIT_FILL, !fill);
    }

    /**
     * Returns {@code true} if the minimum portrait scale is used for newly
     * installed portrait images.
     *
     * @return {@code true} if the minimum portrait scale method is used for new
     * portrait images
     * @see #setPortraitScaleUsesMinimum
     */
    public final boolean getPortraitScaleUsesMinimum() {
        return getFeature(OPT_MIN_PORTRAIT_SCALE);
    }

    /**
     * Sets how the portrait's initial scale is determined. If {@code false},
     * the default, the initial scale is the smallest scale that completely
     * covers the portrait's clip region. If {@code true}, then the initial
     * scale will be the smallest scale that causes either the top and bottom
     * edge of the image or the left and right edge of the image to touch the
     * corresponding edges of the clipping region.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param useMinimum {@code true} to set the minimum portrait scale method
     * for new portrait images
     * @see #getPortraitScaleUsesMinimum
     */
    public final void setPortraitScaleUsesMinimum(boolean useMinimum) {
        setFeature(OPT_MIN_PORTRAIT_SCALE, useMinimum);
    }

    /**
     * Returns {@code true} if the portrait is clipped to the clip region when
     * drawn with {@link DIYSheet#paintPortrait(java.awt.Graphics2D)}.
     *
     * @return {@code true} if clipping is enabled
     * @see #setPortraitClipping
     */
    public final boolean getPortraitClipping() {
        return !getFeature(OPT_NO_PORTRAIT_CLIP);
    }

    /**
     * Sets whether the portrait is clipped. If {@code true} (the default), then
     * no part of the portrait image that lies outside of the clip region will
     * be drawn when {@link DIYSheet#paintPortrait(java.awt.Graphics2D)} is used
     * to draw the portrait. If {@code false}, then the portrait can "escape"
     * from the clip region and draw over any surrounding content. When set to
     * {@code false}, the minimum scaling option is often also set.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param clipping {@code true} if the portrait should be clipped
     * @see #getPortraitClipping
     * @see #setPortraitScaleUsesMinimum
     */
    public final void setPortraitClipping(boolean clipping) {
        setFeature(OPT_NO_PORTRAIT_CLIP, !clipping);
    }

    /**
     * Sets an explicit clip stencil for this component's portrait. A portrait's
     * clip stencil is used by the portrait adjustment panel to show which parts
     * of the portrait will be occluded by other parts of the component. An
     * explicit clip stencil overrides the default mechanism for creating a clip
     * stencil (described below). The provided image should be the same size as
     * the clip rectangle. The image's alpha channel should match the
     * translucency of any graphics drawn over the portrait region.
     * Alternatively, the stencil can be set to {@code null} to indicate that
     * the portrait is not occluded.
     *
     * <p>
     * If no clip stencil is set, a default mechanism will be used to create
     * one. This mechanism assumes that the portrait is either drawn underneath
     * the template image for the sheet with index 0, or else not occluded. The
     * stencil will be based on the subimage of the template image that is
     * covered by the clipping region. If all of the pixels in this subimage are
     * fully opaque, then the portrait is assumed to be drawn over the template
     * and not occluded by other graphics. Otherwise, it is assumed that the
     * template is drawn over the portrait and the subimage's alpha channel will
     * be used to create the stencil.
     *
     * <p>
     * In the interest of efficiency, the portrait panel caches the clip stencil
     * rather than requesting it repeatedly. If the clip region or stencil image
     * change during editing, you must call the portrait panel's
     * {@link PortraitPanel#updatePanel()} method to cause it to adjust for the
     * stencil and clip values.
     *
     * <p>
     * <b>Note:</b> Explicit clip stencils are not saved with the component, but
     * must be restored when the component is read from a file. A simple way to
     * handle this is to set the clip stencil in {@code createInterface}, as
     * this is called whether the component is new or being read from a file.
     *
     * <p>
     * If the component uses custom portrait handling, calling this method will
     * throw an {@code IllegalStateException}. However, if you use
     * {@link DefaultPortrait}s in your custom handling, you can change the clip
     * stencil using {@link DefaultPortrait#setClipStencil}.
     *
     * @param stencil an image whose alpha channel will be used by the portrait
     * adjustment panel to clip the portrait
     * @throws IllegalStateException if custom portrait handling is used
     * @see AbstractPortrait#createStencil
     * @see #isCustomPortraitHandling
     * @see Portrait#getClipDimensions
     * @see Portrait#getClipStencil
     */
    public void setPortraitClipStencil(BufferedImage stencil) {
        if (isCustomPortraitHandling()) {
            throw new IllegalStateException("using custom portrait handling");
        }
        explicitClip = stencil;
        hasExplicitClip = true;
    }

    /**
     * Sets an explicit clip stencil region for this component's portrait. The
     * clip stencil region is the area that the editor's portrait panel
     * considers to be the ideal area covered by the portrait. Normally, this is
     * the same as the portrait clip region. If a design uses transparent
     * portraits drawn over a background, however, one might wish to set the
     * clip stencil smaller than the true clipping region so that the default
     * portrait scale still lets some of the background show through. In this
     * case, you can set the this value to the true clipping region so that the
     * portrait panel's portrait area has the correct shape.
     *
     * <p>
     * <b>Note:</b> Explicit clip stencil regions are not saved with the
     * component, but must be restored when the component is read from a file. A
     * simple way to handle this is to set the region in
     * {@code createInterface}, as this is called whether the component is new
     * or being read from a file.
     *
     * <p>
     * If the component uses custom portrait handling, calling this method will
     * throw an {@code IllegalStateException}.
     *
     * @param region the region of the template covered by the "true" clipping
     * region portrait adjustment panel to clip the portrait
     * @throws IllegalStateException if custom portrait handling is used
     * @see #isCustomPortraitHandling
     * @see Portrait#getClipDimensions
     * @see #setPortraitClipStencil
     */
    public void setPortraitClipStencilRegion(Rectangle region) {
        if (isCustomPortraitHandling()) {
            throw new IllegalStateException("using custom portrait handling");
        }
        if (region == null) {
            throw new NullPointerException("region");
        }
        explicitClipRegion = region;
        hasExplicitClipRegion = true;
    }

    /**
     * Returns {@code true} if marker portrait areas will be filled with solid
     * white before painting the marker portrait.
     *
     * @return {@code true} if background filling is enabled
     * @see #setMarkerBackgroundFilled
     * @throws IllegalStateException if the face style is not
     * {@code CARD_AND_MARKER}
     */
    public final boolean isMarkerBackgroundFilled() {
        if (faceStyle != FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("component has no marker");
        }
        return !getFeature(OPT_NO_MARKER_FILL);
    }

    /**
     * If set, the marker portrait clip region will be filled in with solid
     * white before painting the portrait on the marker.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param fill if {@code true}, the clip region will be filled before
     * painting when {@link DIYSheet#paintMarkerPortrait(java.awt.Graphics2D)}
     * is called
     * @see #isMarkerBackgroundFilled
     * @throws IllegalStateException if the face style is not
     * {@code CARD_AND_MARKER}
     */
    public final void setMarkerBackgroundFilled(boolean fill) {
        if (faceStyle != FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("component has no marker");
        }
        setFeature(OPT_NO_MARKER_FILL, !fill);
    }

    /**
     * Returns {@code true} if the minimum portrait scale is used on the marker
     * for newly installed portrait images.
     *
     * @return {@code true} if the minimum portrait scale method is used for new
     * portrait images
     * @see #setMarkerScaleUsesMinimum
     * @throws IllegalStateException if the face style is not
     * {@code CARD_AND_MARKER}
     */
    public final boolean getMarkerScaleUsesMinimum() {
        if (faceStyle != FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("component has no marker");
        }
        return getFeature(OPT_MIN_MARKER_SCALE);
    }

    /**
     * Sets how the portrait's initial scale is determined for the marker
     * portrait.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param useMinimum {@code true} to set the minimum portrait scale method
     * for new portrait images
     * @see #getMarkerScaleUsesMinimum
     * @throws IllegalStateException if the face style is not
     * {@code CARD_AND_MARKER}
     */
    public final void setMarkerScaleUsesMinimum(boolean useMinimum) {
        if (faceStyle != FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("component has no marker");
        }
        setFeature(OPT_MIN_MARKER_SCALE, useMinimum);
    }

    /**
     * Returns {@code true} if the marker portrait is clipped to the clip region
     * when drawn with
     * {@link DIYSheet#paintMarkerPortrait(java.awt.Graphics2D)}.
     *
     * @return {@code true} if clipping is enabled
     * @see #setMarkerClipping
     * @throws IllegalStateException if the face style is not
     * {@code CARD_AND_MARKER}
     */
    public final boolean getMarkerClipping() {
        if (faceStyle != FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("component has no marker");
        }
        return !getFeature(OPT_NO_MARKER_CLIP);
    }

    /**
     * Sets whether the marker portrait is clipped. If {@code true} (the
     * default), then no part of the portrait image that lies outside of the
     * clip region will be drawn when
     * {@link DIYSheet#paintMarkerPortrait(java.awt.Graphics2D)} is used to draw
     * the portrait. If {@code false}, then the portrait can "escape" from the
     * clip region and draw over any surrounding content.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param clipping {@code true} if the marker portrait should be clipped
     * @see #getMarkerClipping
     * @see #setMarkerScaleUsesMinimum
     * @throws IllegalStateException if the face style is not
     * {@code CARD_AND_MARKER}
     */
    public final void setMarkerClipping(boolean clipping) {
        if (faceStyle != FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("component has no marker");
        }
        setFeature(OPT_NO_MARKER_CLIP, !clipping);
    }

    /**
     * Sets an explicit clip stencil for the marker of a
     * {@link FaceStyle#CARD_AND_MARKER} component. This is essentially the same
     * as setting a portrait clip stencil except that it applies to the portrait
     * on the marker rather than the main portrait. Refer to that method for a
     * detailed explanation of how the clip stencil works. The only major
     * difference is that when the default mechanism for creating a stencil is
     * used, this will use the template image for the sheet with index 2 rather
     * than the sheet with index 0.
     *
     * @param stencil an image whose alpha channel will be used by the portrait
     * adjustment panel to clip the marker portrait
     * @see #setPortraitClipStencil
     * @throws IllegalStateException if teh face style is not
     * {@code CARD_AND_MAKRER} or custom portrait handling is used
     */
    public void setMarkerClipStencil(BufferedImage stencil) {
        if (isCustomPortraitHandling()) {
            throw new IllegalStateException("using custom portrait handling");
        }
        explicitClip1 = stencil;
        hasExplicitClip1 = true;
    }

    /**
     * Sets an explicit clip stencil region for this component's marker
     * portrait. See {@link #setPortraitClipStencilRegion} for information about
     * clip stencil regions.
     *
     * @param region the region of the template covered by the "true" clipping
     * region portrait adjustment panel to clip the portrait
     * @throws IllegalStateException if custom portrait handling is used
     * @see #isCustomPortraitHandling
     * @see Portrait#getClipDimensions
     * @see #setPortraitClipStencilRegion
     * @see #setMarkerClipStencil
     */
    public void setMarkerClipStencilRegion(Rectangle region) {
        if (isCustomPortraitHandling()) {
            throw new IllegalStateException("using custom portrait handling");
        }
        if (region == null) {
            throw new NullPointerException("region");
        }
        explicitClipRegion1 = region;
        hasExplicitClipRegion1 = true;
    }

    /**
     * Returns the marker style for the marker sheet of a
     * {@link FaceStyle#CARD_AND_MARKER} component. This style determines how a
     * back face for the marker is generated.
     *
     * @return the component's marker style
     */
    public MarkerStyle getMarkerStyle() {
        return getFaceStyle() == FaceStyle.CARD_AND_MARKER ? markerStyle : null;
    }

    /**
     * Sets the marker style for the marker sheet of a
     * {@link FaceStyle#CARD_AND_MARKER} component. This style determines how a
     * back face for the marker is generated.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param markerStyle
     */
    public void setMarkerStyle(MarkerStyle markerStyle) {
        checkPropertyLock();
        if (markerStyle == null) {
            if (getFaceStyle() == FaceStyle.CARD_AND_MARKER) {
                throw new IllegalStateException("face style specifies a marker");
            }
        } else if (getFaceStyle() != FaceStyle.CARD_AND_MARKER) {
            throw new IllegalStateException("face style must specify a marker (e.g. CARD_AND_MARKER)");
        }
        this.markerStyle = markerStyle;
    }

    /**
     * Returns {@code true} if this component is providing its own
     * {@link Portrait}s through script code.
     *
     * <p>
     * <b>Note:</b> The somewhat awkward name is to allow scripts to access this
     * value using the automatically generated getter
     * {@code customPortraitHandling}.
     *
     * @return {@code true} if the DIY script implements the
     * {@link PortraitProvider} interface
     * @see #setCustomPortraitHandling
     * @see PortraitProvider
     */
    public final boolean isCustomPortraitHandling() {
        return getFeature(OPT_OWN_PORTRAIT_HANDLING);
    }

    /**
     * Sets whether this component's script will provide its own portrait
     * handling code or whether the built-in portrait management system will be
     * used. The default is {@code false} (use the built-in system), which is
     * suitable in most cases. If set to {@code true}, the component script must
     * define functions to implement the {@link PortraitProvider} interface, and
     * the component will delegate to these functions when its own
     * {@link #getPortraitCount()} and {@link #getPortrait(int)} methods are
     * called.
     *
     * <p>
     * <b>This is a <a href='#locked'>restricted property</a>.</b>
     *
     * @param scripted whether portrait handling will be provided by the
     * component's built-in portrait management ({@code false}) or by script
     * code ({@code true})
     * @see #isCustomPortraitHandling
     * @see PortraitProvider
     * @see DefaultPortrait
     */
    public final void setCustomPortraitHandling(boolean scripted) {
        setFeature(OPT_OWN_PORTRAIT_HANDLING, scripted);
    }

    /**
     * Returns the number of portraits provided by this component. If custom
     * portrait handling is enabled, then this returns the number of portraits
     * reported by the script's {@code getPortraitCount} function. Otherwise, it
     * returns 0 if no portrait key is set, 1 if a portrait key is set, or 2 if
     * a portrait key is set <i>and</i> the face style is
     * {@code CARD_AND_MARKER}.
     *
     * @return the number of portraits available from {@link #getPortrait}
     * @see #setPortraitKey
     * @see #setCustomPortraitHandling
     * @see #setFaceStyle
     */
    @Override
    public int getPortraitCount() {
        int count;
        if (isCustomPortraitHandling()) {
            count = handler.getPortraitCount();
        } else if (portraitKey == null) {
            count = 0;
        } else if (faceStyle == FaceStyle.CARD_AND_MARKER) {
            count = 2;
        } else {
            count = 1;
        }
        return count;
    }

    /**
     * Returns the specified {@link Portrait}. If custom portrait handling is
     * enabled, then this calls the script's {@code getPortrait} function to
     * retrieve the portrait to return. Otherwise it will return a built-in
     * portrait implementation: if no portrait key is set, then no portraits are
     * available. Otherwise, the built-in portrait can be fetched using an index
     * of 0. If the face style is {@link FaceStyle#CARD_AND_MARKER}, then the
     * marker portrait can be fetched using an index of 1. Note that the marker
     * portrait uses the same image as the main portrait, but has its own scale
     * and pan values.
     *
     * @param index the index of the desired portrait, from 0 to
     * {@link #getPortraitCount()}-1.
     * @return the requested portrait instance
     * @throws IndexOutOfBoundsException if the portrait index is invalid
     * @see #setPortraitKey
     * @see #setCustomPortraitHandling
     * @see PortraitProvider
     * @see FaceStyle
     */
    @Override
    public Portrait getPortrait(int index) {
        final int portraitCount = getPortraitCount();
        if (index < 0 || index >= portraitCount) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        if (isCustomPortraitHandling()) {
            return handler.getPortrait(index);
        }

        if (index == 0) {
            return getPortrait0();
        } else {
            return getPortrait1();
        }
    }

    /**
     * Returns a possibly cached portrait 0 (the main portrait). This is called
     * by {@link #getPortrait}.
     *
     * @return portrait 0
     */
    private Portrait getPortrait0() {
        if (cachedPortrait == null) {
            cachedPortrait = new AbstractPortrait() {
                @Override
                public String getSource() {
                    return portraitImage;
                }

                @Override
                public void setSource(String resource) {
                    if (resource == null) {
                        resource = "";
                    }
                    if (!resource.equals(portraitImage)) {
                        if (resource.isEmpty()) {
                            installDefault();
                        } else {
                            portrait = AbstractPortrait.getImageFromIdentifier(resource, getClipDimensions());
                            // come up with default orientation
                            portraitPanX = portraitPanY = 0d;
                            // ideal portrait size
                            if (getPortraitScaleUsesMinimum()) {
                                portraitScale = computeMinimumScaleForImage(portrait, null);
                            } else {
                                portraitScale = computeIdealScaleForImage(portrait, null);
                            }
                        }

                        if (faceStyle == FaceStyle.CARD_AND_MARKER) {
                            markerPanX = markerPanY = 0d;
                            if (getMarkerScaleUsesMinimum()) {
                                markerScale = computeMinimumScaleForImage(portrait, "marker");
                            } else {
                                markerScale = computeIdealScaleForImage(portrait, "marker");
                            }
                        }

                        portraitImage = resource;
                        markChanged();
                    }
                }

                @Override
                public void installDefault() {
                    if (getPortraitKey() != null) { // will be null if custom portrait is used
                        // look for default portrait keys, and if not found create defaults
                        Settings s = getSettings();
                        portrait = s.getImageResource(portraitKey + "-portrait-template");

                        if (s.get(portraitKey + "-portrait-panx") == null) {
                            portraitPanX = 0d;
                        } else {
                            portraitPanX = s.getDouble(portraitKey + "-portrait-panx");
                        }
                        if (s.get(portraitKey + "-portrait-pany") == null) {
                            portraitPanY = 0d;
                        } else {
                            portraitPanY = s.getDouble(portraitKey + "-portrait-pany");
                        }
                        if (s.get(portraitKey + "-portrait-scale") == null) {
                            BufferedImage image = getDefaultPortrait(portraitKey);
                            if (getPortraitScaleUsesMinimum()) {
                                portraitScale = computeMinimumScaleForImage(image, null);
                            } else {
                                portraitScale = computeIdealScaleForImage(image, null);
                            }
                        } else {
                            portraitScale = s.getDouble(portraitKey + "-portrait-scale");
                        }

                        if (faceStyle == FaceStyle.CARD_AND_MARKER) {
                            if (s.get(portraitKey + "-marker-panx") == null) {
                                markerPanX = 0d;
                            } else {
                                markerPanX = s.getDouble(portraitKey + "-marker-panx");
                            }
                            if (s.get(portraitKey + "-marker-pany") == null) {
                                markerPanY = 0d;
                            } else {
                                markerPanY = s.getDouble(portraitKey + "-marker-pany");
                            }
                            if (s.get(portraitKey + "-marker-scale") == null) {
                                BufferedImage image = getDefaultPortrait(portraitKey);
                                if (getMarkerScaleUsesMinimum()) {
                                    markerScale = computeMinimumScaleForImage(image, "marker");
                                } else {
                                    markerScale = computeIdealScaleForImage(image, "marker");
                                }
                            } else {
                                markerScale = s.getDouble(portraitKey + "-marker-scale");
                            }
                        }
                    } else {
                        throw new IllegalStateException("using built-in portrait when custom portrait is enabled");
                    }
                }

                @Override
                public double getPanX() {
                    return portraitPanX;
                }

                @Override
                public void setPanX(double x) {
                    if (portraitPanX != x) {
                        portraitPanX = x;
                        markChanged();
                    }
                }

                @Override
                public double getPanY() {
                    return portraitPanY;
                }

                @Override
                public void setPanY(double y) {
                    if (portraitPanY != y) {
                        portraitPanY = y;
                        markChanged();
                    }
                }

                @Override
                public double getScale() {
                    return portraitScale;
                }

                @Override
                public void setScale(double scale) {
                    if (scale < 0.00000001d) {
                        scale = 0.00000001d;
                    }
                    if (portraitScale != scale) {
                        portraitScale = scale;
                        markChanged();
                    }
                }

                @Override
                public BufferedImage getImage() {
                    return portrait;
                }

                @Override
                public void setImage(String reportedSource, BufferedImage image) {
                    if (reportedSource == null) {
                        reportedSource = "";
                    }
                    image = ResourceKit.prepareNewImage(image);
                    portraitPanX = portraitPanY = 0d;
                    if ((flags & OPT_MIN_PORTRAIT_SCALE) != 0) {
                        portraitScale = computeMinimumScaleForImage(image, getPortraitKey());
                    } else {
                        portraitScale = computeIdealScaleForImage(image, getPortraitKey());
                    }
                    portraitImage = reportedSource;
                    markChanged();
                }

                private Rectangle getClipRect() {
                    final Rectangle clipRect;
                    if (hasExplicitClipRegion) {
                        clipRect = explicitClipRegion;
                    } else {
                        clipRect = privateSettings.getRegion(getPortraitKey() + "-portrait-clip");
                    }
                    return clipRect;
                }

                @Override
                public Dimension getClipDimensions() {
                    Rectangle clipRect = getClipRect();
                    return new Dimension(clipRect.width, clipRect.height);
                }

                @Override
                public BufferedImage getClipStencil() {
                    if (getPortraitKey() == null) {
                        return null;
                    }
                    if (hasExplicitClip) {
                        clip = lastTemplate = null;
                        return explicitClip;
                    }

//				if( getPortraitClipping() == false ) return null;
                    Sheet[] sheets = getSheets();
                    if (sheets == null || sheets.length == 0) {
                        return null;
                    }

                    BufferedImage template = sheets[0].getTemplateImage();
                    if (clip == null || template != lastTemplate) {
                        lastTemplate = template;
                        Rectangle clipRect = getClipRect();
                        clip = createStencil(template, clipRect);
                    }
                    return clip;
                }
                private BufferedImage clip;
                private BufferedImage lastTemplate;
            };
        }
        return cachedPortrait;
    }
    private transient Portrait cachedPortrait;
    private transient BufferedImage explicitClip;
    private transient boolean hasExplicitClip;
    private transient Rectangle explicitClipRegion;
    private transient boolean hasExplicitClipRegion;

    /**
     * Returns a possibly cached portrait 1 (the marker portrait). This is
     * called by {@link #getPortrait}.
     *
     * @return portrait 1
     */
    private Portrait getPortrait1() {
        if (cachedPortrait1 == null) {
            cachedPortrait1 = new AbstractPortrait() {
                @Override
                public String getSource() {
                    return getPortrait(0).getSource();
                }

                @Override
                public void setSource(String resource) {
                    getPortrait(0).setSource(resource);
                }

                @Override
                public void installDefault() {
                }

                @Override
                public double getPanX() {
                    return markerPanX;
                }

                @Override
                public void setPanX(double x) {
                    if (markerPanX != x) {
                        markerPanX = x;
                        markChanged(2);
                    }
                }

                @Override
                public double getPanY() {
                    return markerPanY;
                }

                @Override
                public void setPanY(double y) {
                    if (markerPanY != y) {
                        markerPanY = y;
                        markChanged(2);
                    }
                }

                @Override
                public double getScale() {
                    return markerScale;
                }

                @Override
                public void setScale(double scale) {
                    if (scale < 0.00000001d) {
                        scale = 0.00000001d;
                    }
                    if (markerScale != scale) {
                        markerScale = scale;
                        markChanged(2);
                    }
                }

                @Override
                public BufferedImage getImage() {
                    return portrait;
                }

                @Override
                public void setImage(String reportedSource, BufferedImage image) {
                    getPortrait0().setImage(reportedSource, image);
                }

                private Rectangle getClipRect() {
                    final Rectangle clipRect;
                    if (hasExplicitClipRegion1) {
                        clipRect = explicitClipRegion1;
                    } else {
                        clipRect = privateSettings.getRegion(getPortraitKey() + "-marker-clip");
                    }
                    return clipRect;
                }

                @Override
                public Dimension getClipDimensions() {
                    Rectangle clipRect = getClipRect();
                    return new Dimension(clipRect.width, clipRect.height);
                }

                @Override
                public BufferedImage getClipStencil() {
                    if (getPortraitKey() == null) {
                        return null;
                    }
                    if (hasExplicitClip1) {
                        clip = lastTemplate = null;
                        return explicitClip1;
                    }

//				if( getMarkerClipping() == false ) return null;
                    Sheet[] sheets = getSheets();
                    if (sheets == null || sheets.length < 3) {
                        return null;
                    }

                    BufferedImage template = sheets[2].getTemplateImage();
                    if (clip == null || template != lastTemplate) {
                        lastTemplate = template;
                        Rectangle clipRect = getClipRect();
                        clip = createStencil(template, clipRect);
                    }
                    return clip;
                }
                private BufferedImage clip;
                private BufferedImage lastTemplate;

                @Override
                public EnumSet<Feature> getFeatures() {
                    return MARKER_FEATURES;
                }
            };
        }
        return cachedPortrait1;
    }
    private transient Portrait cachedPortrait1;
    private transient BufferedImage explicitClip1;
    private transient boolean hasExplicitClip1;
    private transient Rectangle explicitClipRegion1;
    private transient boolean hasExplicitClipRegion1;
    private static final EnumSet<Portrait.Feature> MARKER_FEATURES = EnumSet.range(Portrait.Feature.SCALE, Portrait.Feature.PAN);

    /**
     * Returns the portrait source identifier. This is equivalent to
     * {@code getPortrait(0).getSource()}.
     *
     * @return the portrait source
     * @see #getPortrait
     * @see Portrait
     */
    public final String getPortraitSource() {
        return getPortrait(0).getSource();
    }

    /**
     * Sets the portrait source. This is equivalent to
     * {@code getPortrait(0).setSource(source)}.
     *
     * @param source the portrait source
     * @see #getPortrait
     * @see Portrait
     */
    public final void setPortraitSource(String source) {
        getPortrait(0).setSource(source);
    }

    /**
     * Returns the current portrait image as a bitmap. This is equivalent to
     * {@code getPortrait(0).getImage()}.
     *
     * @return the portrait image
     * @see #getPortrait
     * @see Portrait
     */
    public final BufferedImage getPortraitImage() {
        return getPortrait(0).getImage();
    }

    /**
     * Returns the horizontal position of the portrait. This is equivalent to
     * {@code getPortrait(0).getPanX()}.
     *
     * @return the x pan position
     * @see #getPortrait
     * @see Portrait
     */
    public final double getPortraitPanX() {
        return getPortrait(0).getPanX();
    }

    /**
     * Sets the horizontal position of the portrait. This is equivalent to
     * {@code getPortrait(0).setPanX(x)}.
     *
     * @param x the x pan position
     * @see #getPortrait
     * @see Portrait
     */
    public final void setPortraitPanX(double x) {
        getPortrait(0).setPanX(x);
    }

    /**
     * Returns the vertical position of the portrait. This is equivalent to
     * {@code getPortrait(0).getPanY()}.
     *
     * @return the y pan position
     * @see #getPortrait
     * @see Portrait
     */
    public final double getPortraitPanY() {
        return getPortrait(0).getPanY();
    }

    /**
     * Sets the vertical position of the portrait. This is equivalent to
     * {@code getPortrait(0).setPanY(y)}.
     *
     * @param y the y pan position
     * @see #getPortrait
     * @see Portrait
     */
    public final void setPortraitPanY(double y) {
        getPortrait(0).setPanY(y);
    }

    /**
     * Returns the scale factor of the portrait. This is equivalent to
     * {@code getPortrait(0).getScale()}.
     *
     * @return the scale factor
     * @see #getPortrait
     * @see Portrait
     */
    public final double getPortraitScale() {
        return getPortrait(0).getScale();
    }

    /**
     * Sets the scale factor of the portrait. This is equivalent to
     * {@code getPortrait(0).setScale(factor)}.
     *
     * @param factor the scale factor
     * @see #getPortrait
     * @see Portrait
     */
    public final void setPortraitScale(double factor) {
        getPortrait(0).setScale(factor);
    }

    /**
     * Clears the component by setting the name and comments to empty strings,
     * resetting the expansion symbol key, marking all sheets as changed, and
     * calling the handler scripts {@code onClear} function to clear the
     * component elements defined by the script.
     */
    @Override
    public void clearAll() {
        super.clearAll();
        handler.onClear(this);
    }

    /**
     * Creates a set of sheets to draw the faces for this component and sets
     * them as the current sheet set. The number and type of sheet is determined
     * by the component's {@link FaceStyle}.
     *
     * {@inheritDoc}
     *
     * @see #setFaceStyle
     */
    @Override
    public Sheet[] createDefaultSheets() {
        switch (faceStyle) {
            case ONE_FACE:
                sheets = new DIYSheet[]{
                    new DIYSheet(this, frontTemplateKey + "-template", 0)
                };
                createFrontPainter(this, (DIYSheet) sheets[0]);
                break;
            case PLAIN_BACK:
                sheets = new Sheet[]{
                    new DIYSheet(this, frontTemplateKey + "-template", 0),
                    new UndecoratedCardBack(this, backTemplateKey + "-template", bleedMargin, cornerRadius)
                };
                createFrontPainter(this, (DIYSheet) sheets[0]);
                break;
            case SHARED_FACE:
                DIYSheet front = new DIYSheet(this, frontTemplateKey + "-template", 0);
                sheets = new Sheet[]{
                    front, front
                };
                createFrontPainter(this, front);
                break;
            case TWO_FACES:
                sheets = new Sheet[]{
                    new DIYSheet(this, frontTemplateKey + "-template", 0),
                    new DIYSheet(this, backTemplateKey + "-template", 1)
                };
                createFrontPainter(this, (DIYSheet) sheets[0]);
                createBackPainter(this, (DIYSheet) sheets[1]);
                break;
            case CARD_AND_MARKER:
                sheets = new Sheet[]{
                    new DIYSheet(this, frontTemplateKey + "-template", 0),
                    new DIYSheet(this, backTemplateKey + "-template", 1),
                    new DIYSheet(this, thirdTemplateKey + "-template", 2),};
                createFrontPainter(this, (DIYSheet) sheets[0]);
                createBackPainter(this, (DIYSheet) sheets[1]);
                createFrontPainter(this, (DIYSheet) sheets[2]);
                break;
            case FOUR_FACES:
                sheets = new Sheet[]{
                    new DIYSheet(this, frontTemplateKey + "-template", 0),
                    new DIYSheet(this, backTemplateKey + "-template", 1),
                    new DIYSheet(this, thirdTemplateKey + "-template", 2),
                    new DIYSheet(this, fourthTemplateKey + "-template", 3)
                };
                createFrontPainter(this, (DIYSheet) sheets[0]);
                createBackPainter(this, (DIYSheet) sheets[1]);
                createFrontPainter(this, (DIYSheet) sheets[2]);
                createBackPainter(this, (DIYSheet) sheets[3]);
                break;
            case SIX_FACES:
                sheets = new Sheet[]{
                    new DIYSheet(this, frontTemplateKey + "-template", 0),
                    new DIYSheet(this, backTemplateKey + "-template", 1),
                    new DIYSheet(this, thirdTemplateKey + "-template", 2),
                    new DIYSheet(this, fourthTemplateKey + "-template", 3),
                    new DIYSheet(this, fifthTemplateKey + "-template", 4),
                    new DIYSheet(this, sixthTemplateKey + "-template", 5),};
                createFrontPainter(this, (DIYSheet) sheets[0]);
                createBackPainter(this, (DIYSheet) sheets[1]);
                createFrontPainter(this, (DIYSheet) sheets[2]);
                createBackPainter(this, (DIYSheet) sheets[3]);
                createFrontPainter(this, (DIYSheet) sheets[4]);
                createBackPainter(this, (DIYSheet) sheets[5]);
                break;
            case EIGHT_FACES:
                sheets = new Sheet[]{
                    new DIYSheet(this, frontTemplateKey + "-template", 0),
                    new DIYSheet(this, backTemplateKey + "-template", 1),
                    new DIYSheet(this, thirdTemplateKey + "-template", 2),
                    new DIYSheet(this, fourthTemplateKey + "-template", 3),
                    new DIYSheet(this, fifthTemplateKey + "-template", 4),
                    new DIYSheet(this, sixthTemplateKey + "-template", 5),
                    new DIYSheet(this, seventhTemplateKey + "-template", 6),
                    new DIYSheet(this, eighthTemplateKey + "-template", 7),};
                createFrontPainter(this, (DIYSheet) sheets[0]);
                createBackPainter(this, (DIYSheet) sheets[1]);
                createFrontPainter(this, (DIYSheet) sheets[2]);
                createBackPainter(this, (DIYSheet) sheets[3]);
                createFrontPainter(this, (DIYSheet) sheets[4]);
                createBackPainter(this, (DIYSheet) sheets[5]);
                createFrontPainter(this, (DIYSheet) sheets[6]);
                createBackPainter(this, (DIYSheet) sheets[7]);
                break;
            default:
                throw new AssertionError("Unknown face style: " + faceStyle);
        }
        return sheets;
    }

    /**
     * Creates a {@link DIYEditor} with this component set as the game component
     * for editing.
     *
     * @return a new editor for the component
     */
    @Override
    public DIYEditor createDefaultEditor() {
        return new DIYEditor(this);
    }

    /**
     * Determine the ideal default scale for a portrait image so that the image
     * completely covers the clip region.
     *
     * @param image the image to compute the ideal scale for
     * @param clipKeyType use "portrait" or {@code null} for the portrait clip
     * region, "marker" for the marker clip region
     * @return the ideal scale for the image based on the portrait clip region
     */
    @Override
    public double computeIdealScaleForImage(BufferedImage image, String clipKeyType) {
        if (portraitKey == null) {
            return 1d;
        }
        if (clipKeyType == null) {
            clipKeyType = "portrait";
        }
        final String key = portraitKey + "-" + clipKeyType + "-clip";
        Rectangle clip = getSettings().getRegion(key);
        if (clip == null) {
            throw new NullPointerException("clip key not defined: " + key);
        }
        double idealWidth = clip.getWidth();
        double idealHeight = clip.getHeight();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        return ImageUtilities.idealCoveringScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
    }

    /**
     * Determine the ideal default scale for a portrait image so that the image
     * just touches the clip edges in one dimension.
     *
     * <p>
     * This is overridden to use the portrait key to determine the clip region.
     *
     * @param image the image to compute the minimum scale for
     * @param clipKeyType use "portrait" or {@code null} for the portrait clip
     * region, "marker" for the marker clip region
     * @return the minimum scale for the image based on the portrait clip region
     * @see #setPortraitScaleUsesMinimum
     */
    @Override
    public double computeMinimumScaleForImage(BufferedImage image, String clipKeyType) {
        if (portraitKey == null) {
            return 1d;
        }
        if (clipKeyType == null) {
            clipKeyType = "portrait";
        }
        final String key = portraitKey + "-" + clipKeyType + "-clip";
        Rectangle clip = getSettings().getRegion(portraitKey + "-" + clipKeyType + "-clip");
        if (clip == null) {
            throw new NullPointerException("clip key not defined: " + key);
        }
        double idealWidth = clip.getWidth();
        double idealHeight = clip.getHeight();
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        return ImageUtilities.idealBoundingScaleForImage(idealWidth, idealHeight, imageWidth, imageHeight);
    }

    /**
     * Returns the name field used to hold the name of the component. May be
     * {@code null}.
     *
     * @return the text field that contains the component's name, as set by
     * {@link #setNameField}
     */
    public JTextComponent getNameField() {
        return nameField;
    }

    /**
     * Sets the field that contains the component's name. If set, edits in this
     * field will automatically update the component's name, and the component's
     * name will be copied to the field when copying the component state to the
     * editor. Leave this set to {@code null} if you would like to handle
     * editing the name yourself (for example, if you want separate given and
     * family name fields).
     *
     * @param nameField the text field used to edit the component's name, or
     * {@code null}
     */
    public void setNameField(JTextComponent nameField) {
        this.nameField = nameField;
    }

    /**
     * Calls the script's {@code onClear} function. This should not be called
     * directly; instead, call {@link #clearAll}.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * {@inheritDoc}
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param diy this component
     */
    @Override
    public void onClear(DIY diy) {
        try {
            handler.onClear(diy);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code create} function. This should not be called
     * directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param diy this component
     */
    @Override
    public void create(DIY diy) {
        try {
            handler.create(diy);
            if (getPortraitKey() != null) {
                getPortrait(0).installDefault();
            }
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code createInterface} function. This should not be
     * called directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param diy this component
     * @param editor an editor created to edit the component
     */
    @Override
    public void createInterface(DIY diy, DIYEditor editor) {
        try {
            handler.createInterface(diy, editor);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code createFrontPainter} function. This should not
     * be called directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param diy this component
     * @param sheet the sheet used to paint the face
     */
    @Override
    public void createFrontPainter(DIY diy, DIYSheet sheet) {
        try {
            handler.createFrontPainter(diy, sheet);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code createBackPainter} function. This should not be
     * called directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param diy this component
     * @param sheet the sheet used to paint the face
     */
    @Override
    public void createBackPainter(DIY diy, DIYSheet sheet) {
        try {
            handler.createBackPainter(diy, sheet);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code paintFront} function. This should not be called
     * directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param g a graphics context to draw the face with
     * @param diy this component
     * @param sheet the sheet used to paint the face
     */
    @Override
    public void paintFront(Graphics2D g, DIY diy, DIYSheet sheet) {
        try {
            handler.paintFront(g, diy, sheet);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code paintBack} function. This should not be called
     * directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param g a graphics context to draw the face with
     * @param diy this component
     * @param sheet the sheet used to paint the face
     */
    @Override
    public void paintBack(Graphics2D g, DIY diy, DIYSheet sheet) {
        try {
            handler.paintBack(g, diy, sheet);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code onRead} function. This should not be called
     * directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param diy this component
     * @param objectInputStream the object input stream being used to read the
     * component
     */
    @Override
    public void onRead(DIY diy, ObjectInputStream objectInputStream) {
        try {
            handler.onRead(diy, objectInputStream);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Calls the script's {@code onWrite} function. This should not be called
     * directly.
     *
     * Exceptions thrown by the script function will be caught and reported in
     * the script console.
     *
     * <p>
     * Subclasses may override this function to implement a DIY component as a
     * subclass of DIY. In order to do this, <b>all</b> of the methods that make
     * up the {@link Handler} interface <b>must</b> be overridden, except those
     * in {@link PortraitProvider}.
     *
     * @param diy this component
     * @param objectOutputStream the object output stream being used to write
     * the component
     */
    @Override
    public void onWrite(DIY diy, ObjectOutputStream objectOutputStream) {
        try {
            handler.onWrite(diy, objectOutputStream);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
        }
    }

    /**
     * Throws an IllegalStateException if the restricted properties are locked.
     */
    private void checkPropertyLock() {
        if (locked) {
            throw new IllegalStateException("restricted DIY properties can only be modified in create() and onRead(): " + this);
        }
    }

    /**
     * Returns a string describing the component, including the handler script
     * name and face style.
     *
     * @return a string describing this DIY component
     */
    @Override
    public String toString() {
        return "DIY{script=" + getHandlerScript() + ", faceStyle=" + getFaceStyle() + '}';
    }

    private static final int CURRENT_VERSION = 9;
    private static final long serialVersionUID = 6_385_243_435_343_478_156L;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(CURRENT_VERSION);

        out.writeObject(getName());
        out.writeObject(comments);

        out.writeObject(portraitImage);
        out.writeDouble(portraitScale);
        out.writeDouble(portraitPanX);
        out.writeDouble(portraitPanY);

        out.writeDouble(markerScale);
        out.writeDouble(markerPanX);
        out.writeDouble(markerPanY);

        out.writeObject(handlerScript);
        out.writeObject(extensionName);
        out.writeInt(cardVersion);
        out.writeObject(faceStyle);
        out.writeObject(frontTemplateKey);
        out.writeObject(backTemplateKey);
        out.writeObject(thirdTemplateKey);
        out.writeObject(fourthTemplateKey);
        out.writeObject(fifthTemplateKey);
        out.writeObject(sixthTemplateKey);
        out.writeObject(seventhTemplateKey);
        out.writeObject(eighthTemplateKey);

        out.writeObject(customSheetTitles);

        out.writeObject(portraitKey);
        out.writeInt(flags);
        out.writeObject(deckSnappingHint);
        out.writeDouble(cornerRadius);
        out.writeBoolean(hasExplicitCornerRadius);
        out.writeDouble(bleedMargin);
        out.writeBoolean(hasExplicitBleedMargin);
        out.writeObject(highResMode);
        out.writeObject(customFoldMarks);

        out.writeObject(markerStyle);

        out.writeObject(privateSettings);

        onWrite(this, out);

        // write whether we are writing an image, then write the image
        if (portraitKey != null) {
            ((SEObjectOutputStream) out).writeImage(portrait);
        }

        markSaved();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        NewerVersionException.check(CURRENT_VERSION, version);

        setNameImpl((String) in.readObject());
        comments = (String) in.readObject();

        portraitImage = (String) in.readObject();
        portraitScale = in.readDouble();
        portraitPanX = in.readDouble();
        portraitPanY = in.readDouble();

        if (version >= 6) {
            markerScale = in.readDouble();
            markerPanX = in.readDouble();
            markerPanY = in.readDouble();
        }

        handlerScript = (String) in.readObject();
        extensionName = (String) in.readObject();
        cardVersion = in.readInt();
        faceStyle = (FaceStyle) in.readObject();

        frontTemplateKey = (String) in.readObject();
        backTemplateKey = (String) in.readObject();

        if (version >= 5) {
            thirdTemplateKey = (String) in.readObject();
            fourthTemplateKey = (String) in.readObject();
        } else {
            thirdTemplateKey = "diy-front-sheet";
            fourthTemplateKey = "diy-front-sheet";
        }
        if (version >= 8) {
            fifthTemplateKey = (String) in.readObject();
            sixthTemplateKey = (String) in.readObject();
            seventhTemplateKey = (String) in.readObject();
            eighthTemplateKey = (String) in.readObject();
        } else {
            fifthTemplateKey = "diy-front-sheet";
            sixthTemplateKey = "diy-front-sheet";
            seventhTemplateKey = "diy-front-sheet";
            eighthTemplateKey = "diy-front-sheet";
        }

        if (version >= 8) {
            customSheetTitles = (String[]) in.readObject();
        } else {
            customSheetTitles = null;
        }

        portraitKey = (String) in.readObject();
        flags = in.readInt();

        if (version >= 2) {
            deckSnappingHint = (Sheet.DeckSnappingHint) in.readObject();
        } else {
            deckSnappingHint = Sheet.DeckSnappingHint.CARD;
        }

        if (version >= 9) {
            cornerRadius = in.readDouble();
            hasExplicitCornerRadius = in.readBoolean();
        } else {
            cornerRadius = 0d;
            hasExplicitCornerRadius = false;
        }

        if (version >= 3) {
            bleedMargin = in.readDouble();
            if (version >= 9) {
                hasExplicitBleedMargin = in.readBoolean();
            } else {
                hasExplicitBleedMargin = bleedMargin != 0d;
            }
            highResMode = (HighResolutionMode) in.readObject();
            customFoldMarks = (double[][]) in.readObject();
        } else {
            bleedMargin = 0d;
            hasExplicitBleedMargin = false;
            highResMode = HighResolutionMode.ENABLE;
            customFoldMarks = null;
        }

        if (version >= 7) {
            markerStyle = (MarkerStyle) in.readObject();
        } else {
            markerStyle = MarkerStyle.MIRRORED;
        }

        privateSettings = (Settings) in.readObject();

        locked = false;
        try {
            createHandler();
            onRead(this, in);
        } finally {
            locked = true;
        }

        if (portraitKey != null) {
            if (version >= 4) {
                portrait = ((SEObjectInputStream) in).readImage();
            } else {
                portrait = ImageIO.read(in);
            }
        }
    }

    /**
     * This method can be called by DIY components that have been updated to use
     * the style of property names introduced along with support for $-notation.
     * It automatically converts old "x-" style property names to the new naming
     * style. (For example, <tt>x-my-setting</tt> becomes <tt>MySetting</tt>.)
     *
     * @deprecated This should only be called in an {@code onRead} function when
     * upgrading very old DIY components.
     */
    @Deprecated
    public final void upgradeOldPropertyNames() {
        List<String> deleteKeys = new LinkedList<>();
        List<String> replaceKeys = new LinkedList<>();
        List<String> replaceValues = new LinkedList<>();

        for (String key : privateSettings.getKeySet()) {
            if (key.startsWith("x-")) {
                String value = privateSettings.get(key);
                deleteKeys.add(key);
                StringBuilder b = new StringBuilder(key.length() - 2);
                for (int i = 1; i < key.length(); ++i) {
                    char ch = key.charAt(i);
                    if (ch != '-') {
                        b.append(ch);
                    } else {
                        if (++i < key.length()) {
                            b.append(Character.toUpperCase(key.charAt(i)));
                        }
                    }
                }
                replaceKeys.add(b.toString());
                replaceValues.add(value);
            }
        }
        for (int i = 0; i < replaceKeys.size(); ++i) {
            privateSettings.set(replaceKeys.get(i), replaceValues.get(i));
        }
        for (String key : deleteKeys) {
            privateSettings.reset(key);
        }
    }

    @Override
    public UpgradeConversionTrigger createUpgradeConversionTrigger() {
        // let the trigger be garbage collected after conversion is finished
        UpgradeConversionTrigger trigger = upgradeConversionTrigger;
        upgradeConversionTrigger = null;
        return trigger;
    }

    @Override
    public void convertFrom(ConversionSession session) {
        if (monkey != null) {
            monkey.ambivalentCall("onConvertFrom", this, session);
        }
    }

    @Override
    public void convertTo(ConversionSession session) {
        if (monkey != null) {
            monkey.ambivalentCall("onConvertTo", this, session);
        }
    }

    /**
     * This method can be called by the DIY component if it wants to initiate
     * conversion to another component type during {@code onRead}. The new type
     * is assumed to belong to the same extension. If this is not the case, use
     * {@link #convertToComponentType(String, String)} instead.
     *
     * @param className the class or script identifier to convert to
     */
    public void convertToComponentType(String className) {
        convertToComponentType(className, null, null);
    }

    /**
     * This method can be called by the DIY component if it wants to initiate
     * conversion to another component type during {@code onRead}.
     *
     * @param className the class or script identifier to convert to
     * @param extensionName the name of the extension containing the new type
     * @param extensionId the UUID of the extension containing the new type
     */
    public void convertToComponentType(String className, String extensionName, String extensionId) {
        checkPropertyLock();
        upgradeConversionTrigger = new UpgradeConversionTrigger(className, extensionName, extensionId);
    }
}
