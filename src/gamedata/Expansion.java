package gamedata;

import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.sheet.Sheet;
import ca.cgjennings.graphics.ImageUtilities;
import ca.cgjennings.ui.BlankIcon;
import ca.cgjennings.ui.IconProvider;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import resources.Language;
import resources.ResourceKit;
import resources.Settings;

/**
 * The expansion class represents an expansion for a {@link Game}. Components
 * can include a symbol to indicate that they are from a given expansion. This
 * can be done automatically, if certain keys are set for the component and
 * appropriate graphics are registered along with the expansion. Alternatively,
 * a flag can be set when registering an expansion to indicate that it is drawn
 * using custom code (the component is responsible for drawing).
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0a14
 */
public final class Expansion implements Comparable<Expansion>, IconProvider {

    private Game forGame;
    private String code;
    private String uiName;
    private String gameName;
    private int number = -1;
    private Icon icon;
    private BufferedImage[] symbols;

    private Expansion(Game forGame, String code, String uiName, String gameName, Icon icon, BufferedImage[] symbols) {
        this.forGame = forGame;
        this.code = code;
        this.uiName = uiName;
        this.gameName = gameName;
        this.icon = icon;
        this.symbols = symbols;
    }

    /**
     * Returns the unique code for this expansion.
     *
     * @return the code used to register the expansion
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the game that this expansion belongs to. If the expansion does
     * not belong to any particular game, then the special "all games" game is
     * returned.
     *
     * @return the game that the expansion expands upon
     */
    public Game getGame() {
        return forGame;
    }

    /**
     * Returns the expansion's name in the game locale.
     *
     * @return the localized game name
     */
    public String getGameName() {
        return gameName;
    }

    /**
     * Returns the expansion's name in the interface locale.
     *
     * @return the localized interface name
     */
    public String getUIName() {
        return uiName;
    }

    /**
     * Returns the icon used to represent the expansion in the interface.
     *
     * @return the expansion's icon
     */
    @Override
    public Icon getIcon() {
        return icon;
    }

    /**
     * Returns a string representation of the expansion. This is identical to
     * {@link #getUIName()}.
     *
     * @return the interface name of the expansion
     */
    @Override
    public String toString() {
        return uiName;
    }

    /**
     * Returns a symbol that is associated with this expansion.
     *
     * @deprecated This method is provided to ease migration from SE 2.x. It is
     * equivalent to calling <code>getSymbol( inverse ? 1 : 0 )</code>.
     *
     * @param inverse if <code>true</code>, return the symbol for dark
     * backgrounds
     * @return the 0th (if <code>false</code>) or 1st (if <code>true</code>)
     * symbol for the expansion
     * @see #getSymbol(int)
     */
    public BufferedImage getSymbol(boolean inverse) {
        if (symbols == null) {
            return null;
        }
        if (symbols.length < 2) {
            return symbols[0];
        }
        return inverse ? symbols[1] : symbols[0];
    }

    /**
     * Returns a symbol that is associated with this expansion. This method can
     * be used to retrieve extra symbols beyond the standard two types used by
     * the default expansion symbol painting mechanism. Under that mechanism,
     * index 0 is the standard symbol while index 1 is the inverse symbol.
     * However, custom painting methods may assign any interpretation they wish
     * to these indices. If the requested symbol is not defined for this
     * expansion, the symbol at index 0 is returned instead.
     *
     * @param customSymbolIndex the index into the passed in array of custom
     * symbols
     * @return the requested symbol, or <code>null</code> if there is no symbol
     * at that index
     * @throws IllegalArgumentException if <code>customSymbolIndex</code> is
     * less than 0
     */
    public BufferedImage getSymbol(int customSymbolIndex) {
        if (customSymbolIndex < 0) {
            throw new IllegalArgumentException("customSymbolIndex cannot be negative: " + customSymbolIndex);
        }
        if (symbols == null) {
            return null;
        }
        if (customSymbolIndex >= symbols.length) {
            if (customSymbolIndex == 0) {
                return null;
            }
            return symbols[0];
        }
        return symbols[customSymbolIndex];
    }

    /**
     * Returns an array of all registered expansions. The returned array will
     * not be shared (that is, the caller can freely modify the contents).
     *
     * @return a new array of the currently registered expansions
     */
    public static Expansion[] getExpansions() {
        Expansion[] c = expCache;
        if (c == null) {
            // NOTE: it is possible, though highly unlikely, that two threads
            // could enter this at the same time. However, expCache is volatile
            // so the only negative effect would be regenerating the cache twice.
            synchronized (Expansion.class) {
                // ensure base game is available and registered
                if (BASE_GAME == null) {
                    getBaseGameExpansion();
                }

                c = new Expansion[exps.size()];
                int i = 0;
                for (Expansion e : exps.values()) {
                    c[i++] = e;
                }
                Arrays.sort(c);
                expCache = c;
            }
        }
        return c.clone();
    }

    /**
     * Returns an array of the expansions that have been registered for a
     * specific game. If <code>includeGenerics</code> is <code>true</code>, then
     * expansions registered for all games will also be included.
     *
     * @param game the game to find expansions for
     * @param includeGenerics whether to include generic "all games" expansions
     * @return an array of matching expansions
     * @throws NullPointerException if the game is <code>null</code>
     * @see Game#getAllGamesInstance()
     */
    public static Expansion[] getExpansionsForGame(Game game, boolean includeGenerics) {
        if (game == null) {
            throw new NullPointerException("game");
        }

        LinkedList<Expansion> matches = new LinkedList<>();
        Expansion[] allExps = getExpansions();
        for (int i = 0; i < allExps.length; ++i) {
            if (allExps[i].forGame.equals(game)) {
                matches.add(allExps[i]);
            } else if (includeGenerics && allExps[i].forGame.getCode().equals(Game.ALL_GAMES_CODE)) {
                matches.add(allExps[i]);
            }
        }
        Expansion[] e = matches.toArray(new Expansion[matches.size()]);
        return e;
    }

    /**
     * Sorts groups of expansions by game and then by expansion name.
     *
     * @param o the expansion to compare this expansion to
     * @return a negative integer, zero, or a positive integer as this expansion
     * is less than, equal to, or greater than the parameter in terms of sort
     * order
     */
    @Override
    public int compareTo(Expansion o) {
        // base game always comes first
        if (this == getBaseGameExpansion()) {
            if (o == BASE_GAME) {
                return 0;
            }
            return -1;
        }
        if (o == BASE_GAME) {
            return 1;
        }

        // generic custom expansion always comes last
        if (this.code.equals("XX")) {
            if (o.code.equals("XX")) {
                return 0;
            }
            return 1;
        }
        if (o.code.equals("XX")) {
            return -1;
        }

        // sort together by game
        if (!forGame.getCode().equals(o.forGame.getCode())) {
            // make sure "all games" expansions other than base go at end
            if (forGame.getCode().equals(Game.ALL_GAMES_CODE)) {
                return 1;
            }
            if (o.forGame.getCode().equals(Game.ALL_GAMES_CODE)) {
                return -1;
            }
            return forGame.compareTo(o.forGame);
        }
        // within the same game, sort by expansion number or name
        if (number >= 0 && o.number >= 0) {
            return number - o.number;
        }
        return Language.getInterface().getCollator().compare(uiName, o.uiName);
    }

    private static Expansion BASE_GAME;

    /**
     * Returns the base game "expansion". This is an expansions code that is
     * used to represent no expansion. It is registered for all games, has a
     * blank icon, and <code>null</code> expansion symbol images.
     *
     * @return the shared base game expansion that represents the base version
     * of any game with no expansions
     */
    public static Expansion getBaseGameExpansion() {
        if (BASE_GAME == null) {
            BASE_GAME = new Expansion(
                    Game.getAllGamesInstance(), "NX",
                    Language.string("exp-NX"),
                    Language.gstring("exp-NX"),
                    new BlankIcon(ICON_WIDTH, ICON_HEIGHT),
                    null
            );
            exps.put("NX", BASE_GAME);
        }
        return BASE_GAME;
    }

    /**
     * Return the expansion with the requested identifier, or <code>null</code>
     * if there is no such expansion.
     *
     * @param id the identifier to look up
     * @return the expansion registered with that identifier, or
     * <code>null</code>
     */
    public static synchronized Expansion get(String id) {
        if (id == null) {
            return getBaseGameExpansion();
        }

        // ensure base game is available and registered
        if (BASE_GAME == null) {
            getBaseGameExpansion();
        }

        return exps.get(id);
    }

    /**
     * Registers an expansion with "regular" and "inverse" versions of a symbol.
     *
     * @deprecated This method is provided to ease migration from SE 2.x. It
     * should not be called by plug-in authors.
     *
     * @param forGame the game to register for, or <code>null</code> for all
     * games
     * @param code the expansion code
     * @param nameKey the string table key to use for the name
     * @param iconResource the location of the icon image to use
     * @param normalSymbol the location of the normal (dark on light) symbol
     * image
     * @param inverseSymbol the location of the inverse (light on dark) symbol
     * image, or <code>null</code> to generate one from the normal symbol
     * @return the newly registered expansion
     * @see #register(gamedata.Game, java.lang.String, java.lang.String,
     * java.lang.String, java.awt.image.BufferedImage,
     * java.awt.image.BufferedImage[])
     */
    public static synchronized Expansion register(Game forGame, String code, String nameKey, String iconResource, String normalSymbol, String inverseSymbol) {
        BufferedImage normal;
        BufferedImage inverse;

        if (forGame == null) {
            forGame = Game.getAllGamesInstance();
        }
        if (normalSymbol == null) {
            if (inverseSymbol != null) {
                throw new NullPointerException("normal is null but inverse is not");
            }
            normal = forGame.getSymbolTemplate().getDefaultSymbol(0);
            inverse = forGame.getSymbolTemplate().getDefaultSymbol(1);
        } else {
            normal = ResourceKit.getImage(normalSymbol);
            if (inverseSymbol == null) {
                inverse = ImageUtilities.invert(normal);
            } else if (normalSymbol.equals(inverseSymbol)) {
                inverse = normal;
            } else {
                inverse = ResourceKit.getImage(normalSymbol);
            }
        }

        return register(forGame, code, Language.string(nameKey), Language.gstring(nameKey),
                ResourceKit.getImage(iconResource),
                new BufferedImage[]{normal, inverse}
        );
    }

    /**
     * Register an expansion for a game.
     * {@linkplain Game#register The game must have already be registered.} Each
     * expansion must have a unique identifier string. An easy way to construct
     * such identifiers is to append the number of the expansion to the game's
     * identifier (e.g., TAL1, TAL2 are the first two expansions for the game
     * with identifier TAL, namely Talisman). Alternatively, you could append a
     * hyphen and short mnemonic for the name of the expansion (TAL-TD, TAL-FM,
     * and so on).
     *
     * <p>
     * Note that there is a
     * {@linkplain #getBaseGameExpansion() common "base game" expansion} that
     * all games share to indicate that no expansion applies. There is no need
     * to register an expansion for this purpose.
     *
     * @param forGame the game to which the expansion belongs, or all games if
     * <code>null</code>
     * @param code a short unique code string for the expansion, usually 2-6
     * capital letters
     * @param uiName the name of the expansion, in the user interface locale
     * @param gameName the name of the expansion, in the game locale
     * @param iconImage an image to use to represent the expansion, may be
     * <code>null</code> in which case a default image is used
     * @param symbols an array of symbol images used to represent the expansion
     * on components
     * @return the registered expansion
     * @throws NullPointerException if the code is <code>null</code>
     * @throws IllegalArgumentException if the code is empty or contains
     * characters not allowed in a file name, or if an expansion with the same
     * code is already registered and the database is locked
     * @see #getBaseGameExpansion()
     */
    public static synchronized Expansion register(Game forGame, String code, String uiName, String gameName, BufferedImage iconImage, BufferedImage[] symbols) {
        if (code == null) {
            throw new NullPointerException("code");
        }
        if (code.length() == 0) {
            throw new IllegalArgumentException("expansion code cannot be empty");
        }

        String filtered = ResourceKit.makeStringFileSafe(code);
        if (filtered.length() != code.length()) {
            throw new IllegalArgumentException("expansion codes can only use characters that can appear in file names: " + code);
        }

        // ensure base game is available and registered
        if (BASE_GAME == null) {
            getBaseGameExpansion();
        }

        if (exps.containsKey(code) && !Lock.hasBeenLocked()) {
            throw new IllegalArgumentException("expansion code already registered: " + code);
        }

        if (iconImage == null) {
            iconImage = ResourceKit.getImage("icons/un-expansion-icon.png");
        }
        if (iconImage.getWidth() > ICON_WIDTH || iconImage.getHeight() > ICON_HEIGHT) {
            float scale = ImageUtilities.idealCoveringScaleForImage(ICON_WIDTH, ICON_HEIGHT, iconImage.getWidth(), iconImage.getHeight());
            iconImage = ImageUtilities.resample(iconImage, scale);
        }
        iconImage = ImageUtilities.center(iconImage, ICON_WIDTH, ICON_HEIGHT);

        if (forGame == null) {
            forGame = Game.getAllGamesInstance();
        }
        Expansion e = new Expansion(forGame, code, uiName, gameName, new ImageIcon(iconImage), symbols);

        int number = -1;
        for (Expansion older : exps.values()) {
            if (older.forGame.equals(forGame) && older.number > number) {
                number = older.number;
            }
        }
        e.number = number + 1;

        exps.put(code, e);
        expCache = null;

        Listeners.fireRegistrationEvent(e);

        return e;
    }

    private static final HashMap<String, Expansion> exps = new HashMap<>();
    private static volatile Expansion[] expCache; // note that only the array reference is volatile
    static final int ICON_WIDTH = 24;
    static final int ICON_HEIGHT = 18;

    /**
     * Generates a unique expansion identifier. This is not meant for use by
     * plug-ins that are registering standard expansions. Rather, it is intended
     * for use by tools that help end users create their own custom expansions.
     *
     * @param game the game that the expansion will be for
     * @return an identifier that is highly likely to be unique across all
     * Strange Eons users, and definitely unique for the current user and
     * session
     */
    public static synchronized String generateEndUserIdentifier(Game game) {
        String baseName = ResourceKit.makeStringFileSafe("eue_" + game.getCode() + "_" + Long.toString(new Date().getTime(), Character.MAX_RADIX));
        String trial = baseName;
        int i = 0;
        while (get(trial) != null) {
            trial = baseName + '_' + i++;
        }
        return trial;
    }

    /**
     * Sets the game associated with an expansion.
     *
     * @deprecated This method is used only to allow backwards compatibility
     * with script code from SE 2.x. It should not be called by plug-in authors.
     *
     * @param expansion code for the expansion
     * @param game code for the game to associate it with
     */
    @Deprecated
    public static void setGameForExpansion(String expansion, String game) {
        Expansion e = get(expansion);
        Game g = Game.get(game);
        if (e == null) {
            throw new IllegalArgumentException("no such expansion code");
        }
        if (g == null) {
            throw new IllegalArgumentException("no such game code");
        }
        if (e.forGame != Game.getAllGamesInstance()) {
            throw new IllegalStateException("this expansion already has a game set");
        }
        e.forGame = g;
    }

    /**
     * This key is updated on a component's private settings when a new
     * expansion is selected.
     */
    public static final String EXPANSION_SETTING_KEY = "active-expansion";
    /**
     * This key is updated on a component's private settings when a new symbol
     * variant is selected.
     */
    public static final String VARIANT_SETTING_KEY = "active-variant";

    /**
     * Listeners that will receive notification of new registrations.
     */
    public static final RegistrationEventSource<Expansion> Listeners = new RegistrationEventSource<>();

    /**
     * Returns a new, modifiable set of the expansions set on a game component
     * by reading the value of the component's {@link #EXPANSION_SETTING_KEY}
     * private setting.
     *
     * @param gc the game component to read expansions from
     * @return the set of expansions a set of the expansions
     * @see Sheet#parseExpansionList(java.lang.String)
     * @see Settings#getExpansionCode()
     * @see Settings#getExpansionVariant(ca.cgjennings.apps.arkham.sheet.Sheet)
     */
    public static Set<Expansion> getComponentExpansionSymbols(GameComponent gc) {
        if (gc == null) {
            throw new NullPointerException("gc");
        }
        String code = gc.getSettings().getExpansionCode();

        LinkedHashSet<Expansion> expansions = new LinkedHashSet<>(exps.size());
        Expansion[] parsedExps = Sheet.parseExpansionList(code);
        if (parsedExps != null) {
            for (Expansion e : parsedExps) {
                if (e != null) {
                    expansions.add(e);
                }
            }
        }

        if (expansions.size() > 1) {
            expansions.remove(getBaseGameExpansion());
        } else if (expansions.size() == 0) {
            expansions.add(getBaseGameExpansion());
        }

        return expansions;
    }

    /**
     * Sets the expansion symbols associated with a game component by modifying
     * the component's {@link #EXPANSION_SETTING_KEY} private setting. If the
     * expansion set is <code>null</code> or empty, then the base game expansion
     * will be used.
     *
     * @param gc the game component to modify
     * @param exps a set of the expansions to associate with the component
     * @see #getBaseGameExpansion()
     */
    public static void setComponentExpansionSymbols(GameComponent gc, Set<Expansion> exps) {
        if (gc == null) {
            throw new NullPointerException("gc");
        }

        String val;
        if (exps == null || exps.isEmpty()) {
            val = "NX";
        } else {
            StringBuilder b = new StringBuilder(10);
            for (Expansion e : exps) {
                if (b.length() > 0) {
                    b.append(',');
                }
                b.append(e.code);
            }
            val = b.toString();
        }
        gc.getSettings().set(EXPANSION_SETTING_KEY, val);
    }
}
