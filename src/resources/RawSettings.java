package resources;

import ca.cgjennings.apps.arkham.CommandLineArguments;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.io.Base64;
import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.platform.PlatformFileSystem;
import ca.cgjennings.ui.JUtilities;
import ca.cgjennings.util.SortedProperties;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.Timer;

/**
 * Provides low-level access to the global and user settings tables. This class
 * is used primarily to initialize settings during application startup. For
 * general purpose setting access, use the abstractions provided by
 * {@link Settings}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class RawSettings {

    /**
     * This class cannot be instantiated.
     */
    private RawSettings() {
    }

    private static final Properties settings = new Properties();
    private static final Properties userSettings = new SortedProperties();
    /**
     * The file that stores user settings on this system.
     */
    private static final File PREFERENCE_FILE = StrangeEons.getUserStorageFile("preferences");

    /**
     * Returns the inherited value of the setting key, or <code>null</code> if
     * it is not defined. This method will search both user settings and the
     * global settings table, returning the first hit.
     *
     * @param key the setting key to return the value of
     * @return the value of the key, or <code>null</code>
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     */
    public static String getSetting(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        String value = userSettings.getProperty(key);
        if (value == null) {
            value = settings.getProperty(key);
        }
        return value;
    }

    /**
     * Returns the value of a user setting. If the key is not defined in the
     * user settings table, <code>null</code> will be returned. The global
     * settings table is never consulted.
     *
     * @param key the user setting key to return the value of
     * @return the value of the key, or <code>null</code>
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     */
    public static String getUserSetting(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return userSettings.getProperty(key);
    }

    /**
     * Sets the value of a key in the user settings table.
     *
     * @param key the key to set
     * @param value the new value to set for the key
     * @throws NullPointerException if <code>key</code> or <code>value</code> is
     * <code>null</code>
     */
    public static void setUserSetting(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        synchronized (userSettings) {
            Object prev = userSettings.setProperty(key, value);
            if (!value.equals(prev)) {
                writePending = true;
            }
        }
    }

    /**
     * Removes a key from user settings. If the key has a global value, it will
     * revert to that value.
     *
     * @param key the user setting key to remove
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     */
    public static void removeUserSetting(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        synchronized (userSettings) {
            if (userSettings.remove(key) != null) {
                // will be null if there was no previous value
                writePending = true;
            }
        }
    }

    /**
     * This method is called during application startup to load the user
     * settings from the preferences file. It is declared public in order to
     * cross a package boundary, but it should not be called by user code.
     */
    public static void readUserSettings() {
        if (!userSettings.isEmpty()) {
            throw new IllegalStateException("already loaded");
        }

        CommandLineArguments args = StrangeEons.getApplication().getCommandLineArguments();
        // set the migration hint to a default value from the command line
        performMigrationHint = args.migrateprefs;
        // if the command line --resetprefs flag is set, we are done
        if (args.resetprefs) {
            return;
        }

        // otherwise, try to read the preferences file
        BufferedInputStream in = null;
        try {
            if (PREFERENCE_FILE.exists()) {
                in = new BufferedInputStream(new FileInputStream(PREFERENCE_FILE));
                userSettings.load(in);
            } else {
                performMigrationHint = true;
            }
        } catch (IOException e) {
            StrangeEons.log.log(Level.SEVERE, "Unable to read user preferences from " + PREFERENCE_FILE, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }
    private static boolean performMigrationHint;

    /**
     * Calling this method schedules the user settings to be written to the
     * preference file in the near future. Call this method after changing a
     * user preference so that the change is saved in the event that the
     * application does not terminate normally. Because this method only
     * schedules an update, it returns very quickly. This means that it can be
     * called very often in response to user action even if the preferences are
     * written to an unusually slow device.
     */
    public synchronized static void writeUserSettings() {
        writeRequested = true;
    }
    private static boolean writeRequested;
    private static boolean writePending;

    /**
     * Writes the user settings to the preference file immediately. This method
     * is called during application shutdown to ensure that a final copy of any
     * changes gets written. To store changes to user preferences at other
     * times, call {@link #writeUserSettings()}.
     */
    public synchronized static void writeUserSettingsImmediately() {
        // do nothing if the settings haven't changed
        if (!writePending) {
            return;
        }

        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(PREFERENCE_FILE));
            userSettings.store(out, " This file contains user settings for Strange Eons.\n Any key in settings.txt can be customized here.\n");
            StrangeEons.log.info("wrote user settings");
        } catch (IOException e) {
            StrangeEons.log.log(Level.SEVERE, "failure while writing user settings", e);
        } finally {
            writeRequested = false;
            writePending = false;
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static final int PREF_WRITE_INITIAL_DELAY = 90 * 1_000;
    private static final int PREF_WRITE_DELAY = 30 * 1_000;

    static {
        Timer prefWriter = new Timer(PREF_WRITE_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (RawSettings.class) {
                    if (writeRequested) {
                        writeUserSettingsImmediately();
                    }
                }
            }
        });
        prefWriter.setInitialDelay(PREF_WRITE_INITIAL_DELAY);
        prefWriter.start();
    }

    /**
     * Merge settings from a resource file into the global settings table. These
     * settings will be visible from every context, including all game
     * components (although their value may also be overridden).
     *
     * @param resource the resource file to read settings from
     * @throws NullPointerException if <code>resource</code> is
     * <code>null</code>
     */
    public static void loadGlobalSettings(String resource) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }

        InputStream in = null;
        try {
            URL url = ResourceKit.composeResourceURL(resource);
            if (url == null) {
                ErrorDialog.displayError(Language.string("rk-err-read-resource", resource), null);
                return;
            }
            in = url.openStream();
            Properties p = new Properties();
            p.load(in);
            settings.putAll(p);
        } catch (IOException e) {
            ErrorDialog.displayError(Language.string("rk-err-read-resource", resource), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Returns the value of the setting key in the global setting table, or
     * <code>null</code> if it is not defined.
     *
     * @param key the setting key to return the value of
     * @return the value of the key, or <code>null</code>
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     */
    static String getGlobalSetting(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return settings.getProperty(key);
    }

    /**
     * Sets the value of a global setting. This setting will not persist after
     * the application terminates. Moreover, if a user setting is defined with
     * the same key name, it will take precedence over this one.
     *
     * @param key the key to set
     * @param value the new value to set for the key
     * @throws NullPointerException if <code>key</code> or <code>value</code> is
     * <code>null</code>
     */
    public static void setGlobalSetting(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        if (Settings.getShared().hasPropertyChangeListeners()) {
            String oldVal = getGlobalSetting(key);
            settings.put(key, value);
            String newVal = getSetting(key);
            if (oldVal == null ? newVal != null : !oldVal.equals(newVal)) {
                Settings.getShared().firePropertyChangeEvent(key, oldVal, newVal);
            }
        } else {
            settings.put(key, value);
        }
    }

    /**
     * Return a set of all of the keys that are defined for {@link #getSetting}
     * at the time of the call. Includes keys that are only defined in user
     * settings.
     *
     * @return an immutable set of the keys for settings
     */
    static Set<String> getSettingKeySet() {
        Set<String> keys = new HashSet<>(userSettings.stringPropertyNames());
        keys.addAll(settings.stringPropertyNames());
        return Collections.unmodifiableSet(keys);
    }

    /**
     * Return a set of all of the user keys that are defined at the time of the
     * call.
     *
     * @return an immutable set of the keys for settings
     */
    static Set<String> getUserSettingKeySet() {
        return Collections.unmodifiableSet(userSettings.stringPropertyNames());
    }

    private static void migrateFile(String oldName, String newName) {
        File newFile = StrangeEons.getUserStorageFile(newName);
        if (newFile.exists()) {
            return;
        }
        File oldFile = new File(System.getProperty("user.home"), oldName);
        if (!oldFile.exists()) {
            return;
        }
        PlatformFileSystem.setHidden(oldFile, false);
        try {
            ProjectUtilities.copyFile(oldFile, newFile);
            StrangeEons.log.log(Level.INFO, "migrated file {0} -> {1}", new Object[]{oldName, newName});
        } catch (IOException e) {
            newFile.delete();
            StrangeEons.log.log(Level.WARNING, "unable to migrate {0} -> {1}", new Object[]{oldName, newName});
            StrangeEons.log.log(Level.WARNING, "cause of failure:", e);
        }
    }

    /**
     * This is called during startup sometime after {@link #readUserSettings()}
     * is called. If there was no user setting file to read settings from, or if
     * the <code>--migrateprefs</code> option was set on the command line, this
     * method will attempt to migrate compatible settings from a previous major
     * version of the application.
     *
     * @return <code>true</code> if settings were migrated
     */
    public static boolean migratePreferences() {
        // Ensure that this is the EDT since we may show a dialog
        JUtilities.threadAssert();

        // No need; already have SE3 prefs file
        if (!performMigrationHint) {
            return false;
        }

        // Check for SE2 settings
        File oldSettings = new File(System.getProperty("user.home"), ".strange-eons-settings");
        if (!oldSettings.exists()) {
            return false;
        }

        // Read SE2 settings
        Properties se2 = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(oldSettings);
            se2.load(in);
        } catch (IOException e) {
            StrangeEons.log.log(Level.SEVERE, "unable to read old preferences", e);
            // better not try to migrate in case of corruption
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        // nothing to migrate
        if (se2.isEmpty()) {
            return false;
        }

        // Build the list of migratable keys
        HashMap<String, String> migrateTable = new HashMap<>();
        EscapedLineReader r = null;
        try {
            URL u = ResourceKit.composeResourceURL("migration-keys.text");
            if (u != null) {
                r = new EscapedLineReader(u);
                String line;
                while ((line = r.readNonemptyLine()) != null) {
                    if (line.charAt(line.length() - 1) == '*') {
                        line = line.substring(0, line.length() - 1);
                        for (String s : se2.stringPropertyNames()) {
                            if (s.startsWith(line)) {
                                migrateTable.put(s, s);
                            }
                        }
                    } else if (line.indexOf("->") >= 0) {
                        String[] tokens = line.split("\\s*\\-\\>\\s*");
                        migrateTable.put(tokens[0], tokens[1]);
                    } else {
                        migrateTable.put(line, line);
                    }
                }
            }
        } catch (Exception e) {
            StrangeEons.log.log(Level.SEVERE, "unable to read migration key list", e);
            return false;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                }
            }
        }
        // nothing to migrate
        if (migrateTable.isEmpty()) {
            return false;
        }

        boolean didMigrateSomething = false;
        StrangeEons.log.info("migrating compatible 2.x preferences");
        for (String se2Key : migrateTable.keySet()) {
            if (se2.getProperty(se2Key) != null) {
                final String se3Key = migrateTable.get(se2Key);
                setUserSetting(migrateTable.get(se2Key), se2.getProperty(se2Key));
                StrangeEons.log.log(Level.CONFIG, "migrated preference key {0}", se3Key);
                didMigrateSomething = true;
            }
        }

        migrateFile(".strange-eons-recovered-script", "quickscript");
        migrateFile(".strange-eons-user-dict", "learned-spelling-words");

        return didMigrateSomething;
    }

    /**
     * Obfuscates a string. This converts a string into a form that is difficult
     * for a human to associate with the original string without the help of a
     * computer. This should be used as an additional layer of protection if you
     * intend to store private information in the user's settings. The primary
     * protection for such information is the file system permissions: a user's
     * settings file should be owned by that user and should not be readable by
     * other users. Obfuscating the private text is an additional precaution to
     * protect the text from being read by a casual observer should the value
     * happen to be displayed (for example, if the user's settings file is open
     * in a text editor on the screen).
     *
     * <p>
     * <b>This is not intended to be a secure encryption method.</b>
     * The original string can be readily recovered by passing the obfuscated
     * string to {@link #unobfuscate}.
     *
     * @param clearText the text to obfuscate
     * @return the obfuscated version of the text
     * @throws NullPointerException if the clear text string is
     * <code>null</code>
     * @see #unobfuscate
     */
    public static String obfuscate(String clearText) {
        if (clearText == null) {
            throw new NullPointerException("clearText");
        }
        try {
            return Base64.encodeObject(limit(clearText), Base64.GZIP);
        } catch (IOException e) {
            throw new AssertionError("Internal Error: encoding failure");
        }
    }

    /**
     * Recovers the original text of an obfuscated string.
     *
     * @param obfuscatedText the obfuscated string
     * @return the original clear text
     * @throws NullPointerException if the obfuscated string is
     * <code>null</code>
     * @see #obfuscate
     */
    public static String unobfuscate(String obfuscatedText) {
        if (obfuscatedText == null) {
            throw new NullPointerException("obfuscatedText");
        }
        try {
            return limit((String) Base64.decodeToObject(obfuscatedText));
        } catch (Exception e) {
            throw new AssertionError("Internal Error: decoding failure");
        }
    }

    private static String limit(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            b.append((char) (s.charAt(i) ^ (char) (0xf37f + Integer.rotateLeft(0x1432, i))));
        }
        return b.toString();
    }
}
