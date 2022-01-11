package gamedata;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.Marker;
import ca.cgjennings.apps.arkham.deck.item.StyleApplicator;
import ca.cgjennings.apps.arkham.deck.item.StyleCapture;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.IconProvider;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import javax.swing.Icon;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * The game class represents a distinct, standalone game. Instances of this
 * class cannot be created directly but are instead <i>registered</i>. Each game
 * is assigned a unique code that can be used to name it in a
 * language-independent way. Game codes are typically 2-6 capital letters, but
 * this is not a requirement. For example, the code for the game
 * <i>Arkham Horror</i> is AH.
 *
 * <p>
 * <b>Note:</b> Expansions for a game, that is, products that add new material
 * to an existing game but which cannot be used without it, are managed through
 * the {@link Expansion} class.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class Game implements Comparable<Game>, IconProvider {

    private String code;
    private String uiName;
    private String gameName;
    private Icon icon;
    private ExpansionSymbolTemplate template;

    private Game() {
    }

    private Game(String code, String uiName, String gameName, BufferedImage iconImage, ExpansionSymbolTemplate template) {
        if (code == null) {
            throw new NullPointerException("code");
        }
        if (uiName == null) {
            throw new NullPointerException("uiName");
        }
        if (gameName == null) {
            throw new NullPointerException("gameName");
        }

        if (!code.equals(ALL_GAMES_CODE)) {
            String filtered = ResourceKit.makeStringFileSafe(code);
            if (filtered.length() != code.length()) {
                throw new IllegalArgumentException("game codes can only use characters that can appear in file names: " + code);
            }
        }

        this.code = code;
        this.uiName = uiName;
        this.gameName = gameName;
        if (iconImage == null) {
            iconImage = ResourceKit.getImage("editors/game-XX.png");
        }
        icon = ImageUtilities.createIconForSize(iconImage, ICON_SIZE);
        if (template == null) {
            // workaround for Arkham Horror using default template: we have changed
            // the default template to only supply darkl and light variants
            if ("AH".equals(code)) {
                template = new DefaultExpansionSymbolTemplate(3);
            } else {
                template = DEFAULT_TEMPLATE;
            }
        }
        this.template = template;
    }

    /**
     * Return the unique identifier code of this game.
     *
     * @return this game's code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the name of this game in the user interface language.
     *
     * @return the game name
     */
    public String getUIName() {
        return uiName;
    }

    /**
     * Returns the name of this game in the game language.
     *
     * @return the game name
     */
    public String getGameName() {
        return gameName;
    }

    /**
     * Returns the template that describes the symbol graphics used to draw
     * expansion symbols for this component.
     *
     * @return the template instance
     */
    public ExpansionSymbolTemplate getSymbolTemplate() {
        return template;
    }

    /**
     * Returns a small icon that can be used in the user interface to represent
     * the game visually.
     *
     * @return a representative icon for the game, or a generic icon if a
     * specific icon is not available
     */
    @Override
    public Icon getIcon() {
        return icon;
    }

    /**
     * Returns the settings instance for this game. Each registered game has
     * exactly one such instance. This is the preferred place to store the
     * default values for all of the settings used by a particular game. The
     * settings instance for a game should in turn be the parent of all of the
     * private settings instances for individual components that belong to that
     * game. (The game's settings will in turn have the shared settings as its
     * parent.)
     *
     * @return the settings instance for this game
     */
    public synchronized Settings getSettings() {
        if (settings == null) {
            settings = new GameSettings(this);
        }
        return settings;
    }
    private Settings settings;

    /**
     * @deprecated Replaced by {@link #getSettings()}.
     */
    @Deprecated
    public final Settings getMasterSettings() {
        return getSettings();
    }

    /**
     * Returns the default style applicator used to modify the style of new
     * objects that are added to a deck for this game.
     *
     * @return the non-{@code null} style applicator for this game
     * @see #setDefaultDeckStyleApplicator
     */
    public StyleApplicator getDefaultDeckStyleApplicator() {
        if (defaultStyle == null) {
            defaultStyle = new StyleCapture();
        }
        return defaultStyle;
    }

    /**
     * Sets the applicator that will be used to apply default style settings to
     * objects that are added to a deck for this game. For example, the game
     * <i>Arkham Horror</i> defines an applicator that changes the default style
     * for new lines and curves to match the movement lines used on expansion
     * boards for that game. If no applicator is explicitly set, a default
     * applicator that leaves the style unchanged will be provided.
     *
     * @param applicator the applicator to use for the specified game
     */
    public void setDefaultDeckStyleApplicator(StyleApplicator applicator) {
        if (applicator == null) {
            throw new NullPointerException("applicator");
        }
        defaultStyle = applicator;
    }

    private StyleApplicator defaultStyle = NULL_APPLICATOR;

    private static final StyleApplicator NULL_APPLICATOR = new StyleApplicator() {
        @Override
        public void apply(Object o) {
        }

        @Override
        public String toString() {
            return "Default Style Applicator";
        }
    };

    /**
     * Returns the user interface name of this game.
     *
     * @return the name of the game, suitable for use in lists or other UI
     * controls
     */
    @Override
    public String toString() {
        return uiName;
    }

    /**
     * Compares this game to another game. The all games instance will be
     * considered less than any other game; other games are sorted by their user
     * interface name.
     *
     * @param o the game to compare this one to
     * @return a negative integer, zero, or a positive integer as this game is
     * less than, equal to, or greater than the parameter in terms of sort order
     */
    @Override
    public int compareTo(Game o) {
        if (o == null) {
            return 1;
        }
        if (code.equals(ALL_GAMES_CODE)) {
            if (o.code.equals(ALL_GAMES_CODE)) {
                return 0;
            }
            return -1;
        }
        if (o.code.equals(ALL_GAMES_CODE)) {
            return 1;
        }
        return Language.getInterface().getCollator().compare(uiName, o.uiName);
    }

    /**
     * Returns a special Game instance that represents "all games". This can be
     * used in a user interface when filtering by game. This game has the code
     * "*".
     *
     * @return the unique "All Games" instance
     */
    public static Game getAllGamesInstance() {
        init();
        return get(ALL_GAMES_CODE);
    }

    /**
     * Returns the game that was registered using the code, or {@code null} if
     * no game has been registered with the code.
     *
     * @param code the unique code that identifies the game
     * @return the game for the requested code, or {@code null}
     */
    public static Game get(String code) {
        init();
        return games.get(code);
    }

    /**
     * Returns an array of all of the registered games. If includeAllGame is
     * {@code true}, then the special all games instance will be included at
     * index 0 in the array.
     *
     * @param includeAllGame whether to include the special All Games instance
     * @return a sorted array of games
     */
    public static Game[] getGames(boolean includeAllGame) {
        init();

        if (!Lock.hasBeenLocked() && StrangeEons.log.isLoggable(Level.WARNING)) {
            StackTraceElement[] els = new Exception().getStackTrace();
            StrangeEons.log.logp(Level.WARNING, els[1].getClassName(), els[1].getMethodName(), "requested game list before database locked");
        }

        if (gameCache == null) {
            gameCache = new Game[games.size()];
            int i = 0;
            for (String key : games.keySet()) {
                gameCache[i++] = games.get(key);
            }
            Arrays.sort(gameCache);
        }
        if (includeAllGame) {
            return gameCache.clone();
        } else {
            if (gameCache.length == 1) {
                return new Game[0];
            }
            return Arrays.copyOfRange(gameCache, 1, gameCache.length);
        }
    }

    /**
     * Register a new game with an associated icon. This is a convenience method
     * that uses a default icon to represent the game. It is only intended for
     * use as an interim step early in the development of a game plug-in, or for
     * experimenting with the API.
     *
     * @param code a short code string for the game, usually 2-6 capital letters
     * @param gameName the localized name of the game for presentation to the
     * user
     * @return the registered game
     * @throws IllegalArgumentException if a game with this code is already
     * registered, or if the code contains characters that are not legal for
     * file names
     * @throws NullPointerException if the code or name is {@code null}
     * @see #register(java.lang.String, java.lang.String,
     * java.awt.image.BufferedImage)
     * @since 2.1a9
     */
    public static Game register(String code, String gameName) {
        return register(code, gameName, null);
    }

    /**
     * Register a new game with an associated icon. This method looks up the
     * names for the game using a string key and {@link Language}. (The
     * interface name is determined using {@code Language.string( code )} and
     * the game name is determined using {@code Language.gstring( code )}.)
     *
     * @param code a short code string for the game, usually 2-6 capital letters
     * @param key the string table key to use when looking up the game name(s)
     * @param iconImage an image to use to represent the game; if {@code null} a
     * default image is used (see
     * {@link #register(java.lang.String, java.lang.String)}).
     * @return the registered game
     * @throws IllegalArgumentException if a game with this code is already
     * registered, or if the code contains characters that are not legal for
     * file names
     * @throws NullPointerException if the code or key is {@code null}
     * @see #register(java.lang.String, java.lang.String, java.lang.String,
     * java.awt.image.BufferedImage, gamedata.ExpansionSymbolTemplate)
     */
    public static Game register(String code, String key, BufferedImage iconImage) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        Lock.test();
        init();
        if (games.containsKey(code) && !Lock.hasBeenLocked()) {
            throw new IllegalArgumentException("game code already registered: " + code);
        }
        Game g = new Game(code, Language.string(key), Language.gstring(key), iconImage, null);
        games.put(code, g);
        gameCache = null;
        return g;
    }

    /**
     * Register a new game with an associated icon. Games are registered using a
     * short identifier code, usually 2-6 letters and all caps. For example, the
     * game Arkham Horror is registered as <tt>AH</tt>.
     *
     * <p>
     * When a component is created, a setting named {@code game} will be added
     * to it, set to the game code indicated by the class map file. The parent
     * settings for that component will then be the settings instance for the
     * associated game. Components not associated with a specific game in their
     * class map will be associated with the special "all games" game. (Generic
     * components like {@link Marker}s use this game.)
     *
     * <p>
     * Expansions are also associated with particular games, so that only the
     * expansions that match the component currently being edited are listed as
     * choices for that component.
     *
     * <p>
     * <b>Game Editions:</b> For games that have more than one edition, treat
     * different editions as different games if they use substantially different
     * rules or graphic design.
     *
     * @param code a short code string for the game, usually 2-6 capital letters
     * @param uiName the name of the game, in the user interface locale
     * @param gameName the name of the game, in the game locale
     * @param iconImage an image to use to represent the game; if {@code null} a
     * default image is used (see
     * {@link #register(java.lang.String, java.lang.String)}).
     * @param template an expansion symbol template that describes the expansion
     * symbols for this game, or {@code null} to use a default template
     * @return the registered game
     * @throws IllegalArgumentException if a game with this code is already
     * registered, or if the code contains characters that are not legal for
     * file names
     * @throws NullPointerException if the code or either name is {@code null}
     * @see #getSettings()
     * @see #getAllGamesInstance()
     * @see ClassMap
     */
    public static Game register(String code, String uiName, String gameName, BufferedImage iconImage, ExpansionSymbolTemplate template) {
        Lock.test();
        init();
        if (games.containsKey(code) && !Lock.hasBeenLocked()) {
            throw new IllegalArgumentException("game code already registered: " + code);
        }
        Game g = new Game(code, uiName, gameName, iconImage, template);
        games.put(code, g);
        gameCache = null;
        return g;
    }

    private static void init() {
        if (!initialized) {
            games = new HashMap<>();
            ResourceKit.getImage("editors/game-XX.png");
            Game allGames = new Game(ALL_GAMES_CODE,
                    Language.string("game-all"),
                    Language.gstring("game-all"),
                    null, null
            );
            games.put(ALL_GAMES_CODE, allGames);
            gameCache = null;
            initialized = true;
        }
    }

    private static boolean initialized;
    private static HashMap<String, Game> games;
    private static Game[] gameCache;
    private static final int ICON_SIZE = 18;

    /**
     * The code assigned to the special "all games" instance.
     */
    public static final String ALL_GAMES_CODE = "*";

    /**
     * This key is set on a component's private settings with the code of the
     * component's game. Components from versions of Strange Eons prior to 3
     * might not have this value set; you can set in when the card is being read
     * in (for example, in a DIY's onRead function).
     */
    public static final String GAME_SETTING_KEY = "game";

    private static ExpansionSymbolTemplate DEFAULT_TEMPLATE = new DefaultExpansionSymbolTemplate();

    /**
     * The class of all settings instances returned from {@link #getSettings()}.
     */
    private static final class GameSettings extends Settings {

        private static final long serialVersionUID = 2_348_987_646_598_866_534L;

        private GameSettings(Game g) {
            super.set(GAME_SETTING_KEY, g.code);
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * The settings instance for a game will not allow you to change the
         * value of the key with the name {@link #GAME_SETTING_KEY}.
         *
         * @throws IllegalArgumentException if the key is the
         * {@link #GAME_SETTING_KEY} and the value is not equal to the code for
         * the game for which this is a settings instance
         */
        @Override
        public void set(String key, String value) {
            if (key.equals(GAME_SETTING_KEY)) {
                String v = get(GAME_SETTING_KEY);
                if ((v == null && value != null) || !v.equals(value)) {
                    throw new IllegalArgumentException("cannot change the game setting of a game's settings instance");
                }
                return; // no need to actually do the set since it would have no effect
            }
            super.set(key, value);
        }

        @Override
        public String toString() {
            Game g = Game.get(get(GAME_SETTING_KEY));
            String code, name;
            if (g == null) {
                code = "??";
                name = "<null>";
            } else {
                code = g.code;
                name = g.uiName;
            }
            return "Settings (for " + code + '/' + name + ")";
        }

        @Override
        protected boolean isSerializedParent() {
            return false;
        }
    };
}
