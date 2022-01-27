package ca.cgjennings.apps.arkham.editors;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.textedit.CodeType;
import ca.cgjennings.ui.textedit.completion.AbbreviationTable;
import gamedata.Game;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import resources.Language;
import resources.ResourceKit;

/**
 * The abbreviation table manager facilitates access to the user's preferred
 * abbreviations for the various code types supported by {@link CodeEditor}s and
 * for markup targets contained in game component editors. The table returned
 * for a given code type or game code is fixed for the life of the application
 * and shared with all callers requesting that table.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class AbbreviationTableManager {

    private AbbreviationTableManager() {
    }

    /**
     * Returns the shared abbreviation table for the given code type. If the
     * user has created a custom table, this will be returned. Otherwise, a
     * default table is returned if one exists (they are defined in
     * {@code resources/abbrv}). If no default table exists, an empty table is
     * returned.
     *
     * <p>
     * <b>Note:</b> Tables returned from this method are shared and should
     * generally be considered immutable (read-only). If you wish to change the
     * contents of the table, use {@link #saveUserTable}.
     *
     * @param type the code type whose table should be fetched
     * @return the shared abbreviation table for the code type
     */
    public synchronized static LanguageAwareAbbreviationTable getTable(CodeType type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        type = type.normalize();

        LanguageAwareAbbreviationTable at = tables.get(type);

        if (at == null) {
            at = loadAbbreviationTable(fileNameForCodeType(type));
            if (at == null) {
                // make sure the table is non-null so that if the user
                // edits it, the changes will appear in all open editors
                at = new LanguageAwareAbbreviationTable();
            }
            tables.put(type, at);
        }

        return at;
    }

    /**
     * Returns the shared abbreviation table for the game with the specified
     * code. If the user has created a custom table, this will be returned.
     * Otherwise, a default table is returned if one exists (they are defined in
     * {@code resources/abbrv}). If no default table exists, an empty table is
     * returned.
     *
     * <p>
     * <b>Note:</b> Tables returned from this method are shared and should
     * generally be considered immutable (read-only). If you wish to change the
     * contents of the table, use {@link #saveUserTable}.
     *
     * @param gameCode the code for the game whose table should be fetched
     * @return the shared abbreviation table for the code type
     */
    public synchronized static LanguageAwareAbbreviationTable getTable(String gameCode) {
        if (gameCode == null) {
            gameCode = Game.ALL_GAMES_CODE;
        }

        LanguageAwareAbbreviationTable at = tables.get(gameCode);

        if (at == null) {
            at = loadAbbreviationTable(fileNameForGame(gameCode));
            if (at == null) {
                // make sure the table is non-null so that if the user
                // edits it, the changes will appear in all open editors
                at = new LanguageAwareAbbreviationTable();
            }
            // make sure that the parent table is ALL_GAMES
            if (at.getParent() == null && !gameCode.equals(Game.ALL_GAMES_CODE)) {
                at.setParent(getTable(Game.ALL_GAMES_CODE));
            }
            tables.put(gameCode, at);
        }

        return at;
    }

    private static String fileNameForCodeType(CodeType type) {
        return type == null ? null : type.normalize().getExtension();
    }

    private static String fileNameForGame(String code) {
        if (null == code) {
            return null;
        } else {
            switch (code) {
                case Game.ALL_GAMES_CODE:
                    return "all-games";
                default:
                    return "game-" + code;
            }
        }
    }

    /**
     * Loads and returns an abbreviation table suitable for the given code type,
     * or returns {@code null}.
     *
     * @param type the code type to load a table for
     * @return the table for the code type, or {@code null}
     */
    private static LanguageAwareAbbreviationTable loadAbbreviationTable(String fileName) {
        LanguageAwareAbbreviationTable at = null;

        if (fileName != null) {
            // try loading user table
            File f = StrangeEons.getUserStorageFile("abbrev/" + fileName + ".settings");
            if (f.exists()) {
                try {
                    LanguageAwareAbbreviationTable loaded = new LanguageAwareAbbreviationTable();
                    loaded.load(f);
                    at = loaded;
                } catch (Exception e) {
                    StrangeEons.log.log(Level.WARNING, "exception loading user abbrev table " + fileName + ".settings", e);
                }
            }

            // try loading default table for language
            if (at == null) {

                String[] resources = Language.getGame().getResourceChain("abbrev/" + fileName, ".settings");
                if (resources.length > 0) {
                    URL url = ResourceKit.composeResourceURL(resources[resources.length - 1]);
                    if (url != null) {
                        try {
                            LanguageAwareAbbreviationTable loaded = new LanguageAwareAbbreviationTable();
                            loaded.load(url);
                            at = loaded;
                        } catch (Exception e) {
                            StrangeEons.log.log(Level.WARNING, "exception loading default abbrev table " + fileName + ".settings", e);
                        }
                    }
                }
            }
        }

        return at;
    }

    private static HashMap<Object, LanguageAwareAbbreviationTable> tables = new HashMap<>();

    /**
     * Saves the specified table as the user table for the given code type.
     * After this returns, the table returned by
     * {@link #getTable(ca.cgjennings.apps.arkham.editors.CodeEditor.CodeType)}
     * will be identical to the saved table.
     *
     * @param type the code type whose table should be modified
     * @param at the new table content for the code type
     * @throws IOException if an I/O error occurs during the save operation
     * @throws NullPointerException if either parameter is {@code null}
     */
    public static void saveUserTable(CodeType type, AbbreviationTable at) throws IOException {
        save(getTable(type), fileNameForCodeType(type), at);
    }

    /**
     * Saves the specified table as the user table for the given game code.
     * After this returns, the table returned by
     * {@link #getTable(java.lang.String)} will be identical to the saved table.
     *
     * @param gameCode the code of the game whose table should be modified
     * @param at the new table content for the code type
     * @throws IOException if an I/O error occurs during the save operation
     * @throws NullPointerException if either parameter is {@code null}
     */
    public static void saveUserTable(String gameCode, AbbreviationTable at) throws IOException {
        save(getTable(gameCode), fileNameForGame(gameCode), at);
    }

    private static synchronized void save(AbbreviationTable currentTable, String fileName, AbbreviationTable newTable) throws IOException {
        if (currentTable == null) {
            throw new NullPointerException("currentTable");
        }
        if (fileName == null) {
            throw new NullPointerException("type");
        }
        if (newTable == null) {
            throw new NullPointerException("at");
        }

        // copy the table contents into the cached table
        if (currentTable != newTable) {
            currentTable.clear();
            for (String k : newTable.keySet()) {
                currentTable.put(k, newTable.get(k));
            }
        }

        // figure out the new location and save
        File folder = StrangeEons.getUserStorageFile("abbrev");
        folder.mkdirs();
        File dest = new File(folder, fileName + ".settings");
        currentTable.store(dest);
    }

    /**
     * Returns {@code true} if the specified code type is mapped to another type
     * for the purposes of loading an abbreviation table. For example, the code
     * type for automation scripts is mapped to the code type for regular script
     * files, so they share a common set of definitions.
     *
     * @param type the code type to check for mapping
     * @return {@code true} if the code type shares another type's abbreviation
     * table
     */
    public static boolean isCodeTypeMapped(CodeType type) {
        if (type == null) {
            throw new NullPointerException("type");
        }
        return type != type.normalize();
    }

    /**
     * An abbreviation table that also expands @string and #string (when placed
     * in braces) to user interface and game language strings, respectively. All
     * abbreviation tables returned by the manager are instances of this class.
     */
    public static class LanguageAwareAbbreviationTable extends AbbreviationTable {

        private Language ui = Language.getInterface();
        private Language game = Language.getGame();

        public LanguageAwareAbbreviationTable() {
        }

        public LanguageAwareAbbreviationTable(AbbreviationTable toCopy) {
            super(toCopy);
        }

        @Override
        public String get(String abbrev) {
            String exp = super.get(abbrev);
            if (exp != null && exp.indexOf('{') >= 0) {
                Pattern p = Pattern.compile("\\{(\\@|\\#)([^\\}])+\\}");
                Matcher m = p.matcher(exp);
                StringBuilder b = new StringBuilder(exp.length() + 32);
                int unused = 0;
                while (m.find()) {
                    b.append(exp, unused, m.start());
                    unused = m.end() + 1;
                    Language lang = m.group(1).equals("@") ? ui : game;
                    b.append(lang.get(m.group(2)));
                }
                if (unused < exp.length()) {
                    b.append(exp, unused, exp.length());
                }
                exp = b.toString();
            }
            return exp;
        }

        /**
         * Sets the language used to expand game string (#) tags. The default
         * language is {@link Language#getGame()}.
         *
         * @param lang the non-{@code null} language to use
         * @see #getGameLanguage()
         */
        public void setGameLanguage(Language lang) {
            if (lang == null) {
                throw new NullPointerException("lang");
            }
            game = lang;
        }

        /**
         * Returns the language used to expand game string (#) tags. The default
         * language is {@link Language#getGame()}.
         *
         * @return the game language used by the table
         * @see #setGameLanguage
         */
        public Language getGameLanguage() {
            return game;
        }

        /**
         * Sets the language used to expand interface string (@) tags. The
         * default language is {@link Language#getInterface()}.
         *
         * @param lang the non-{@code null} language to use
         * @see #getInterfaceLanguage
         */
        public void setInterfaceLanguage(Language lang) {
            if (lang == null) {
                throw new NullPointerException("lang");
            }
            ui = lang;
        }

        /**
         * Returns the language used to expand interface string (@) tags. The
         * default language is {@link Language#getInterface()}.
         *
         * @return the game language used by the table
         * @see #setInterfaceLanguage
         */
        public Language getInterfaceLanguage() {
            return ui;
        }
    }

//	public static void main(String[] args) {
//		Language.setInterfaceLocale( Locale.getDefault() );
//		Language.setGameLocale( Locale.getDefault() );
//		LanguageAwareAbbreviationTable at = new LanguageAwareAbbreviationTable();
//		at.put( "test", "z{@save}x{@cancel}p" );
//		System.err.println(at.get("test"));
//	}
}
