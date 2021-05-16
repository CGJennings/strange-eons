package ca.cgjennings.apps.arkham.plugins.catalog;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.plugins.catalog.Catalog.VersioningState;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import javax.swing.Timer;
import resources.RawSettings;
import resources.Settings;

/**
 * Checks for updates to plug-ins and the application at regular intervals and
 * displays feedback to the user when updates are available.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class AutomaticUpdater {

    /**
     * Called when any kind of plug-in check occurs; updates the setting that
     * tracks when the last update took place.
     */
    public synchronized static void markUpdate() {
        Settings.getUser().set(LAST_UPDATE, String.valueOf(new Date().getTime()));
    }

    /**
     * Returns the number of milliseconds that elapse between automatic checks,
     * depending on the update frequency setting.
     *
     * @return the ideal number of milliseconds between update checks
     */
    public static long getMillisecondsBetweenChecks() {
        int freq = Settings.getUser().getInt(FREQUENCY, FREQUENCY_NEVER);
        long period;
        switch (freq) {
            case FREQUENCY_MONTHLY:
                period = Integer.MAX_VALUE; // approximately 24 days
                break;
            case FREQUENCY_WEEKLY:
                period = (1_000L * 60L * 60L * 24L) * 7L;
                break;
            case FREQUENCY_DAILY:
                period = (1_000L * 60L * 60L * 24L) * 1L;
                break;
            // not quite always: this prevents double updates when a new
            // version of SE is installed, or another update check right
            // after restarting due to a previous update
            case FREQUENCY_ALWAYS:
                period = (1_000L * 60L * 30L);
                break;
            default: // 0, or bad value: never update
                period = Long.MAX_VALUE;
        }
        return period;
    }

    /**
     * Returns {@code true} if, according to current settings, an automatic
     * update check is overdue.
     *
     * @return {@code true} if the time since the last check is greater
     * than the update check frequency set by the user
     */
    public static boolean isUpdateOverdue() {
        return getMillisecondsUntilNextCheck() < 0;
    }

    /**
     * Returns the number of milliseconds before the next update check should be
     * performed, or {@code Long.MAX_VALUE} if no update check will be
     * performed. Note that this may be less than 0 if an update is overdue.
     *
     * @return the number of milliseconds before an update is pending, or
     * {@code Long.MAX_VALUE}
     */
    public static long getMillisecondsUntilNextCheck() {
        long period = getMillisecondsBetweenChecks();
        if (period < 0 || period == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        long lastUpdate;
        try {
            lastUpdate = Long.parseLong(Settings.getUser().get(LAST_UPDATE, "0"));
        } catch (Exception e) {
            lastUpdate = 0L;
        }
        long elapsed = new Date().getTime() - lastUpdate;
        StrangeEons.log.log(Level.FINE, "time remaining until next automatic update check is due: {0} hours", (period - elapsed) / (1_000L * 60L * 60L));
        return period - elapsed;
    }

    /**
     * Checks if an update is overdue, and if so checks if updates are available
     * in a separate thread. If they are, displays feedback to the user.
     */
    public static void performAutomaticUpdate() {
        if (!isUpdateOverdue()) {
            StrangeEons.log.info("not due for an update check (or checks disabled)");
            return;
        }
        StrangeEons.log.info("due for an automatic update check");
        new Thread(() -> {
            performUpdate(
                    getUpdateAction(),
                    getUpdateAllCatalogs()
            );
        }).start();
    }

    /**
     * Returns {@code true} if it can determine with certainty that an
     * application update is available. If no update is available, or if any
     * error occurs, it will return {@code false}. This call bypasses the
     * usual update infrastructure; for example, it does not reset the time
     * until the next update check.
     *
     * @return {@code true} if an application update is available
     */
    public static boolean isApplicationUpdateAvailable() {
        try {
            URL url = new URL(RawSettings.getSetting("catalog-url-1"));
            Catalog cat = new Catalog(url, false, null);

            UUID toCheck = APP_STABLE;
            for (int u = 0; u < 2; ++u) {
                int index = cat.findListingByUUID(toCheck);
                if (index >= 0) {
                    if (checkCoreApplicationListing(cat.get(index))) {
                        return true;
                    }
                }
                toCheck = APP_EXPERIMENTAL;
            }
        } catch (Throwable t) {
            try {
                StrangeEons.log.log(Level.SEVERE, "exception doing low-level update check", t);
            } catch (Throwable it) {
            }
        }
        return false;
    }

    /**
     * A bit flag returned by {@link #performUpdate} when there is a plug-in
     * update available.
     */
    public static final int AVAILABLE_PLUGIN_UPDATE = 1;
    /**
     * A bit flag returned by {@link #performUpdate} when there is an
     * application update available.
     */
    public static final int AVAILABLE_APP_UPDATE = 2;
    /**
     * A bit flag returned by {@link #performUpdate} when there are new plug-ins
     * available.
     */
    public static final int AVAILABLE_NEW_PLUGIN = 4;

    /**
     * Performs an update check immediately, even if an update is not overdue.
     * The value of {@code updateAction} must be one of:
     * {@code ACTION_TELL_USER}, {@code ACTION_OPEN_CATALOG},
     * {@code ACTION_INSTALL_IMMEDIATELY}. If {@code allCatalogs} is
     * {@code true}, then all catalogs in the user's catalog URL history
     * will be checked. Otherwise, only the default catalog is checked. Returns
     * a bitwise combination of {@code true} if any updates were found, or
     * {@code false} otherwise.
     *
     * @param updateAction the action to take when updates are found
     * @param allCatalogs update from all catalogs in the catalog URL history,
     * not just the primary catalog
     * @return {@code true} if any updates were found
     * @throws IllegalArgumentException if the {@code updateAction} is not
     * valid
     */
    public static int performUpdate(int updateAction, boolean allCatalogs) {
        if (updateAction < ACTION_TELL_USER || updateAction > ACTION_INSTALL_IMMEDIATELY) {
            throw new IllegalArgumentException("invalid update action: " + updateAction);
        }
        boolean background = !EventQueue.isDispatchThread();

        AutomaticUpdater.markUpdate();
        Settings s = Settings.getUser();
        int maxCatalog = allCatalogs ? Integer.MAX_VALUE : 1;
        boolean newPlugins = false;
        boolean pluginUpdates = false;
        boolean foundSEUpdate = false;
        boolean foundSEExperimentalUpdate = false;
        for (int cat = 1; cat <= maxCatalog; ++cat) {
            String catUrl = s.get("catalog-url-" + cat);
            if (catUrl == null) {
                break;
            }
            StrangeEons.log.log(Level.INFO, "checking for updates at {0}", catUrl);
            try {
                URL url = new URL(catUrl);
                // get an *uncached* copy of the catalog to do the update check
                Catalog c = new Catalog(url, false, null);
                for (int p = 0; p < c.trueSize(); ++p) {
                    Listing li = c.get(p);
                    VersioningState state = c.getVersioningState(p);
                    if (state == VersioningState.OUT_OF_DATE || state == VersioningState.OUT_OF_DATE_LEGACY) {
                        c.setInstallFlag(p, true);
                    }
                    // *** NB: li.isnew must come first because each plug-in MUST be checked
                    newPlugins |= (li.isNew() && (state == VersioningState.NOT_INSTALLED || state == VersioningState.REQUIRES_APP_UPDATE) && !li.isHidden());

                    // if this is the master catalog, check for SE updates
                    if (cat == 1) {
                        if (li.getCatalogID().getUUID().equals(APP_STABLE) && checkCoreApplicationListing(li)) {
                            foundSEUpdate = true;
                        } else if (li.getCatalogID().getUUID().equals(APP_EXPERIMENTAL) && checkCoreApplicationListing(li)) {
                            foundSEExperimentalUpdate = true;
                        }
                    }
                }

                if (c.getInstallFlagCount() == 0) {
                    StrangeEons.log.info("no updates found");
                    continue;
                }

                // we never get here unless there are updates
                pluginUpdates = true;

                // install update
                if (updateAction == 2) {
                    StrangeEons.log.info("starting update");
                    Runnable installPass = new InstallUpdateRunnable(c);
                    if (background) {
                        try {
                            EventQueue.invokeAndWait(installPass);
                        } catch (Exception e) {
                            StrangeEons.log.log(Level.SEVERE, "exception during background install", e);
                        }
                    } else {
                        installPass.run();
                    }
                    StrangeEons.log.info("finished update");
                } else if (updateAction == 1) {
                    StrangeEons.log.info("found updates, opening catalog");
                    Runnable opencat = () -> {
                        new CatalogDialog(StrangeEons.getWindow()).setVisible(true);
                    };
                    if (background) {
                        EventQueue.invokeLater(opencat);
                    } else {
                        opencat.run();
                    }
                }

            } catch (IOException e) {
                StrangeEons.log.log(Level.SEVERE, "exception while processing " + catUrl, e);
                return -1;
            }
        }

        int updateFlags = 0;
        if (newPlugins) {
            updateFlags = AVAILABLE_NEW_PLUGIN;
            StrangeEons.log.info("found new plug-ins");
        }
        if (pluginUpdates) {
            updateFlags |= AVAILABLE_PLUGIN_UPDATE;
            StrangeEons.log.info("found updates for plug-ins");
        }
        if (foundSEUpdate) {
            updateFlags |= AVAILABLE_APP_UPDATE;
            StrangeEons.log.info("found stable app update");
        }
        if (foundSEExperimentalUpdate) {
            updateFlags |= AVAILABLE_APP_UPDATE;
            StrangeEons.log.info("found work-in-progress app update");
        }

        // possibly show results of update
        // check 1: has the user asked to see app updates/new plug-ins?
        //          if not, are we running in info only mode (so we must show this
        //          if there are plug-in updates as it is the only way to tell them)
        if (updateAction == 0 || isShowingAppUpdates() || isShowingNewPlugins()) {
            // check 2: are there any updates to tell them about?
            final boolean fiNewPlugins = newPlugins;
            final boolean fiAppUpdate = (foundSEUpdate || foundSEExperimentalUpdate);
            final boolean fiPlugUpdate = pluginUpdates;
            if (fiNewPlugins || fiAppUpdate || fiPlugUpdate) {
                Runnable updateMessage = new Runnable() {
                    @Override
                    public void run() {
                        new UpdateMessage(fiAppUpdate, fiPlugUpdate, fiNewPlugins);
                    }
                };
                if (background) {
                    EventQueue.invokeLater(updateMessage);
                } else {
                    updateMessage.run();
                }
            }
        }
        return updateFlags;
    }

    /**
     * When updates are found, tell the user about them.
     */
    public static final int ACTION_TELL_USER = 0;
    /**
     * When updates are found, open the plug-in catalog dialog.
     */
    public static final int ACTION_OPEN_CATALOG = 1;
    /**
     * When updates are found, install the plug-ins in the background.
     */
    public static final int ACTION_INSTALL_IMMEDIATELY = 2;

    private static class InstallUpdateRunnable implements Runnable {

        Catalog catalog;

        public InstallUpdateRunnable(Catalog c) {
            this.catalog = c;
        }

        @Override
        public void run() {
            if (catalog.installFlaggedPlugins()) {
                StrangeEons.getWindow().suggestRestart(null);
            }
        }
    }

    private static boolean checkCoreApplicationListing(Listing li) {
        String ver = li.get(Listing.VERSION);
        if (ver == null) {
            StrangeEons.log.warning("core application listing is missing version");
            return false;
        }
        try {
            return Integer.parseInt(ver) > StrangeEons.getBuildNumber();
        } catch (NumberFormatException e) {
            StrangeEons.log.warning("core application listing has bad version string " + ver);
            return false;
        }
    }

    /**
     * Called from catalogs when they load a listing to determine if the plug-in
     * is "new" (has a newer date than the most recent date seen by the user
     * when the application started).
     *
     * @param id the ID to check
     * @return {@code true} if this ID's date is more recent than the most
     * recent ID seen at application start
     */
    static boolean isNew(CatalogID id) {
        if (!loadedNewestID) {
            newest = CatalogID.extractCatalogID(Settings.getUser().get(NEWEST_ID, ""));
            loadedNewestID = true;
        }
        if (newest == null || newest.compareDates(id) < 0) {
            if (pending == null || pending.compareDates(id) < 0) {
                pending = id;
            }
            return true;
        }
        return false;
    }
    private static CatalogID newest;
    private static boolean loadedNewestID;

    /**
     * This is called during shutdown to write required information into user
     * settings. It is public only to cross a package boundary and should not be
     * called by plug-ins.
     */
    public static void writePendingUpdateInformation() {
        if (pending != null) {
            Settings.getUser().set(NEWEST_ID, pending.toString());
        }
    }
    private static CatalogID pending;

    public static boolean getUpdateAllCatalogs() {
        return Settings.getUser().getYesNo(ALL_CATALOGS);
    }

    public static void setUpdateAllCatalogs(boolean updateAll) {
        Settings.getUser().set(ALL_CATALOGS, updateAll ? "yes" : "no");
    }

    public static int getUpdateFrequency() {
        Settings s = Settings.getUser();
        int freq = s.getInt(FREQUENCY);
        if (freq == Integer.MIN_VALUE) {
            freq = 0;
        }
        if (freq < 0 || freq > FREQUENCY_ALWAYS) {
            freq = 0;
        }
        return freq;
    }

    public static final int FREQUENCY_NEVER = 0;
    public static final int FREQUENCY_MONTHLY = 1;
    public static final int FREQUENCY_WEEKLY = 2;
    public static final int FREQUENCY_DAILY = 3;
    public static final int FREQUENCY_ALWAYS = 4;

    public static void setUpdateFrequency(int freq) {
        if (freq < 0 || freq > FREQUENCY_ALWAYS) {
            throw new IllegalArgumentException("" + freq);
        }
        Settings.getUser().set(FREQUENCY, "" + freq);
        // resych the update timer
        startAutomaticUpdateTimer();
    }

    public static int getUpdateAction() {
        Settings s = Settings.getUser();
        int action = s.getInt(UPDATE_ACTION, ACTION_SHOW_MESSAGE);
        if (action < 0 || action > ACTION_INSTALL) {
            action = 0;
        }
        return action;
    }

    public static final int ACTION_SHOW_MESSAGE = 0;
    public static final int ACTION_SHOW_CATALOG = 1;
    public static final int ACTION_INSTALL = 2;

    public static void setUpdateAction(int action) {
        if (action < 0 || action > ACTION_INSTALL) {
            throw new IllegalArgumentException("" + action);
        }
        Settings.getUser().set(UPDATE_ACTION, "" + action);
    }

    public static boolean isShowingAppUpdates() {
        return Settings.getUser().getYesNo(SHOW_APP_UPDATES);
    }

    public static void setShowingAppUpdates(boolean show) {
        Settings.getUser().set(SHOW_APP_UPDATES, show ? "yes" : "no");
    }

    public static boolean isShowingNewPlugins() {
        return Settings.getUser().getYesNo(SHOW_NEW_PLUGINS);
    }

    public static void setShowingNewPlugins(boolean show) {
        Settings.getUser().set(SHOW_NEW_PLUGINS, show ? "yes" : "no");
    }

    private static final String FREQUENCY = "core-autoupdate-frequency";
    private static final String LAST_UPDATE = "core-last-update-check";
    private static final String NEWEST_ID = "core-newest-observed-id";
    private static final String ALL_CATALOGS = "core-update-all-catalogs";
    private static final String UPDATE_ACTION = "core-update-action";
    private static final String SHOW_APP_UPDATES = "core-show-app-updates";
    private static final String SHOW_NEW_PLUGINS = "core-show-new-plugins";

    private AutomaticUpdater() {
    }

    /**
     * Starts a timer that counts down until the next automatic update is due.
     * If the timer is already running, it will be reset to match the current
     * update frequency. The timer is normally started during application
     * startup.
     */
    public static void startAutomaticUpdateTimer() {
        if (automaticUpdateTimer == null) {
            createTimer();
        }

        automaticUpdateTimer.stop();
        long remaining = getMillisecondsUntilNextCheck();

        // if a check is overdue, schedule it a few seconds into the future
        // (this is mainly for the benefit of the initial check when the app starts)
        if (remaining < 5_000) {
            remaining = 5_000;
        }

        // note that if the remaining time is the special infinite value,
        // we won't (re)start the timer
        if (remaining < Long.MAX_VALUE) {
            automaticUpdateTimer.setInitialDelay((int) Math.min(Integer.MAX_VALUE, remaining));
            automaticUpdateTimer.start();
            StrangeEons.log.log(Level.INFO, "scheduled automatic update check");
        }
    }

    private static Timer automaticUpdateTimer;

    private static void createTimer() {
        automaticUpdateTimer = new Timer(5 * 1_000, (ActionEvent e) -> {
            if (automaticUpdateTimer == null) {
                return;
            }
            
            if (isUpdateOverdue()) {
                StrangeEons.log.info("running automatic update check in background");
                AutomaticUpdater.markUpdate();
                AutomaticUpdater.performAutomaticUpdate();
            }
            
            // this limits the checks to once per app run when set to "ALWAYS"
            // as otherwise you'd get a check every half hour
            if (getMillisecondsBetweenChecks() > (1_000L * 60L * 60L)) {
                startAutomaticUpdateTimer();
            }
        });
        automaticUpdateTimer.setRepeats(false);
    }

    private static final UUID APP_STABLE = UUID.fromString("c6d49b71-5321-4f42-b77f-5d3fd119d8f3");
    private static final UUID APP_EXPERIMENTAL = UUID.fromString("a334a2b6-8a46-47b3-8a43-5ab3a1c85ced");
}
