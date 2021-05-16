package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.ui.JIconList;
import gamedata.Game;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import resources.RawSettings;
import resources.Settings;

/**
 * Organizes the available paper sizes into various sets broken down by
 * characteristics and allows registration of new paper sizes. Strange Eons
 * maintains several sets of paper sizes for different purposes. Using this
 * class you can request all of the available paper sizes that match a
 * particular description. The description is defined using a map. The map
 * contains keys for properties that you are interested in matching, and each
 * key maps to a value describing the attribute that a matching paper will have.
 * In addition to this general matching capability, some static methods are
 * provided to look up commonly used sets. The papers that match a given request
 * will be described using instances of the {@link PaperProperties} class.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class PaperSets {

    private PaperSets() {
    }

    /**
     * Add this key to the request map to specify whether papers should be
     * physical or virtual.
     */
    public static final Object KEY_CONCRETENESS = 0;
    /**
     * Value for papers that represent a true physical paper size, such as A4 or
     * Letter.
     */
    public static final Object VALUE_CONCRETENESS_PHYSICAL = 1;
    /**
     * Value for papers that represent a virtual paper size, like the size of a
     * board for a board game. These sizes are used to guide design rather than
     * representing the actual size of the paper that will be printed on.
     * Typically, decks that use virtual paper sizes are printed by splitting
     * the virtual paper over multiple physical pages.
     */
    public static final Object VALUE_CONCRETENESS_VIRTUAL = 2;
    /**
     * Add this key to the request map to specify whether papers from the
     * built-in, user-defined, or temporary groups should be included.
     */
    public static final Object KEY_ORIGIN = 3;
    /**
     * Value for the default paper sizes that come built into Strange Eons.
     */
    public static final Object VALUE_ORIGIN_BUILT_IN = 4;
    /**
     * Value for paper sizes that have been registered as temporary papers by
     * plug-ins.
     *
     * @see #addTemporaryPaper
     */
    public static final Object VALUE_ORIGIN_TEMPORARY = 5;
    /**
     * Value for paper sizes that the user has defined as custom paper sizes.
     */
    public static final Object VALUE_ORIGIN_USER_DEFINED = 6;
    /**
     * Add this key to the request map to specify whether papers should have
     * portrait or landscape orientation.
     */
    public static final Object KEY_ORIENTATION = 7;
    /**
     * Value to request paper sizes with landscape orientation (wider than they
     * are tall).
     */
    public static final Object VALUE_ORIENTATION_LANDSCAPE = 8;
    /**
     * Value to request paper sizes with portrait orientation (taller than they
     * are wide).
     */
    public static final Object VALUE_ORIENTATION_PORTRAIT = 9;
    /**
     * Add this key to the request map to specify the game to which the paper
     * belongs. The value is an instance of {@link Game}. If set, it will filter
     * out papers that were registered for any other game, but not papers that
     * were registered for all games. That is, the papers in the returned set
     * will either not be tied to a particular game, or will be tied to the
     * specified game. If the value is {@link #VALUE_DONT_CARE} (the default),
     * then all papers will match regardless of game. If the value is
     * {@link Game#getAllGamesInstance()}, then the papers in the returned set
     * will not be associated with a particular game.
     */
    public static final Object KEY_GAME = 10;

    /**
     * Add this key with a value of type {@code Number} to specify a
     * maximum size (in points). All matching papers will have both a width and
     * a height that is less than or equal to this value.
     */
    public static final Object KEY_MAXIMUM_SIZE = 11;

    /**
     * Add this key with a value of type {@code Number} to specify a
     * minimum size (in points). All matching papers will have both a width and
     * a height that is greater than or equal to this value.
     */
    public static final Object KEY_MINIMUM_SIZE = 12;

    /**
     * Add this key with a value of type {@link PaperProperties} to specify that
     * all returned papers must have a smaller area than the specified target.
     */
    public static final Object KEY_SMALLER_THAN = 13;

    /**
     * Any key can be mapped to this value to indicate that it should not be
     * considered. This is also the default if a key is not present in the map.
     */
    public static final Object VALUE_DONT_CARE = null;

    /**
     * Returns the paper type in the specified set that most closely matches the
     * platform-dependant default paper size. If the set is {@code null},
     * the set of built-in, physical papers will be used.
     *
     * @return of the candidates, the type that most closely matches the default
     * paper size
     */
    public static PaperProperties getDefaultPaper(Set<PaperProperties> candidates) {
        if (candidates == null) {
            HashMap<Object, Object> attr = new HashMap<>();
            attr.put(KEY_ORIGIN, VALUE_ORIGIN_BUILT_IN);
            attr.put(KEY_CONCRETENESS, VALUE_CONCRETENESS_PHYSICAL);
            candidates = getMatchingPapers(attr);
        }
        PrinterJob pj = PrinterJob.getPrinterJob();
        PageFormat pf = pj.defaultPage();
        PaperProperties pp = new PaperProperties("", pf.getWidth(), pf.getHeight(), PaperProperties.LANDSCAPE, -1d, -1d, true, Game.getAllGamesInstance());
        return findBestPaper(pp, candidates);
    }

    /**
     * Returns the paper type most similar to the specified target of those
     * candidates in the provided set. This method will only return
     * {@code null} if the set of candidates is empty.
     *
     * @return of the candidates, the type that most closely matches the default
     * paper size
     * @throws NullPointerException if the target or candidate set is
     * {@code null}, or if the candidate set contains {@code null}
     */
    public static PaperProperties findBestPaper(PaperProperties target, Set<PaperProperties> candidates) {
        if (target == null) {
            throw new NullPointerException("target");
        }
        if (candidates == null) {
            throw new NullPointerException("candidates");
        }
        if (candidates.contains(null)) {
            throw new NullPointerException("candidates contains null");
        }

        // first try to find something with the exact dimensions and other attributes as close as possible
        int bestLevel = 0;
        PaperProperties bestMatch = null;
        for (PaperProperties c : candidates) {
            int level = matchLevel(target, c);
            if (level > bestLevel) {
                bestMatch = c;
                bestLevel = level;
            }
        }

        // as a fallback, find the paper with the closest lower-right corner
        if (bestMatch == null) {
            double maxError = Double.MAX_VALUE;
            for (PaperProperties c : candidates) {
                double error = errorSq(target, c);
                if (error < maxError) {
                    maxError = error;
                    bestMatch = c;
                }
            }
        }

        return bestMatch;
    }

    /**
     * Returns an integer describing how closely the two paper properties match.
     * It returns 0 if they don't match at all, it returns a positive number
     * with higher values indicating a better match.
     *
     * @param lhs the first paper to compare
     * @param rhs the other paper to compare
     * @return 0 if the papers don't match, higher numbers the more similar they
     * are
     */
    private static int matchLevel(PaperProperties lhs, PaperProperties rhs) {
        if (lhs == null) {
            throw new NullPointerException("lhs");
        }
        if (rhs == null) {
            throw new NullPointerException("rhs");
        }

        // not same size (within epsilson), no match
        if (Math.abs(lhs.getPageWidth() - rhs.getPageWidth()) > 1d) {
            return 0;
        }
        if (Math.abs(lhs.getPageHeight() - rhs.getPageHeight()) > 1d) {
            return 0;
        }

        // at this point they are a size match, now compare names
        if (!lhs.getInternalName().equals(rhs.getInternalName())) {
            return 1;
        }

        // if either grid or margin matches, that's +1, +2 for both
        int score = 2;
        if (Math.abs(lhs.getGridSeparation() - rhs.getGridSeparation()) < 0.01d) {
            ++score;
        }
        if (Math.abs(lhs.getMargin() - rhs.getMargin()) < 0.01d) {
            ++score;
        }

        if (score == 4 && lhs.isPhysical() == rhs.isPhysical()) {
            score = 5;
        }

        return score;
    }

    /**
     * Returns the paper in the candidate set that is the closest match for the
     * target size and orientation. If the width is greater then the height,
     * then only a landscape paper amongst the candidates will match. Otherwise,
     * only a portrait candidate will match. If there is no paper with the
     * correct orientation, {@code null} is returned. Otherwise, the paper
     * closest in size with the correct orientation is returned.
     *
     * @param targetWidth the paper width to match, in points
     * @param targetHeight the paper height to match, in points
     * @param candidates the set of papers to match against
     * @return the closest size match with the correct orientation, or
     * {@code null}
     * @throws NullPointerException if the candidate set is {@code null} or
     * if the candidate set contains {@code null}
     */
    public static PaperProperties findBestPaper(double targetWidth, double targetHeight, Set<PaperProperties> candidates) {
        if (candidates == null) {
            throw new NullPointerException("candidates");
        }
        PaperProperties match = null;
        double matchError = Double.MAX_VALUE;
        boolean orient = targetWidth > targetHeight ? PaperProperties.LANDSCAPE : PaperProperties.PORTRAIT;
        PaperProperties targetDummy = new PaperProperties("", targetWidth, targetHeight, orient);
        for (PaperProperties pp : candidates) {
            if (pp.getOrientation() != orient) {
                continue;
            }
            double err = errorSq(targetDummy, pp);
            if (err < matchError) {
                match = pp;
                matchError = err;
            }
        }
        return match;
    }

    /**
     * Returns the square of the difference between the sizes of two papers.
     * This is essentially the square of the distance between where their lower
     * right corners would be if they were placed one on top of the other and
     * the upper-left corners aligned. The higher this value, the more different
     * the papers are in size.
     *
     * @param lhs a paper to compare
     * @param rhs the paper to compare with
     * @return the square of the size difference of	{@code lhs} and
     * {@code rhs}
     */
    private static double errorSq(PaperProperties lhs, PaperProperties rhs) {
        double dx = lhs.getPageWidth() - rhs.getPageWidth();
        double dy = lhs.getPageHeight() - rhs.getPageHeight();
        return (dx * dx) + (dy * dy);
    }

    /**
     * Returns a set of all built-in papers.
     *
     * @return a set of the built-in paper types
     */
    public static Set<PaperProperties> getBuiltInPapers() {
        synchronized (PaperSets.class) {
            if (builtIn == null) {
                builtIn = new LinkedHashSet<>();
                for (int i = 1;; ++i) {
                    PaperProperties[] portLand = readPaperAtIndex(false, i);
                    if (portLand == null) {
                        break;
                    }
                    builtIn.add(portLand[0]);
                    builtIn.add(portLand[1]);
                }
            }
        }
        return new LinkedHashSet<>(builtIn);
    }
    private static Set<PaperProperties> builtIn;

    /**
     * Returns a set of all currently registered temporary papers.
     *
     * @return a set of all temporary papers that have been registered with
     * {@link #addTemporaryPaper}.
     */
    public static Set<PaperProperties> getTemporaryPapers() {
        synchronized (temporary) {
            return new LinkedHashSet<>(temporary);
        }
    }
    private static final LinkedHashSet<PaperProperties> temporary = new LinkedHashSet<>();

    /**
     * Temporarily adds a paper size that will last until the application exits.
     * This is used by plug-ins to add special paper sizes that are only
     * relevant as long as the plug-in is installed. A common case is the
     * plug-in for a game adding virtual paper sizes to represent expansion
     * boards for the game.
     *
     * <p>
     * There is no need to add separate portrait and landscape versions of the
     * paper size; both will be generated automatically. Moreover, it does not
     * matter if the width and height parameters are supplied in a portrait
     * configuration or a landscape configuration.
     *
     * <p>
     * The value of {@code marginInPoints} determines the size of the
     * margin, which is used as a visual cue for the designer (it does not
     * prevent the placement of objects, although most printers cannot print all
     * the way to their edges). The value of {@code marginInPoints} may be
     * 0 to indicate no margin.
     *
     * <p>
     * The value of {@code gridInPoints} determines the spacing of a set of
     * equidistant grid lines. Components that are not snapped to other
     * components will normally snap to the nearest half-grid line. Either or
     * both of these values may be less than 0, in which case default values
     * will be used. If {@code name} starts with an at sign (@), then the
     * rest of the string is used as an interface language key to look up a
     * localized name.
     *
     * @param name the name to use for this paper type, localized if possible
     * @param widthInPoints the width of the paper, in points (1/72 inch)
     * @param heightInPoints the height of the paper, in points
     * @param marginInPoints the visual margin provided around the page in the
     * deck editor, in points
     * @param gridInPoints the spacing of the grid in the deck editor, in points
     * @param isPhysicalPaperSize if {@code true}, then this represents an
     * actual paper size; otherwise, it represents a size that is convenient to
     * design for, like an expansion board
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is empty or
     * contains only whitespace, if either dimension is not in the range 0 &lt;
     * <i>d</i> <u>&lt;</u> {@link PaperProperties#MAX_PAPER_SIZE}, or if
     * {@code gridInPoints} is 0
     */
    public static void addTemporaryPaper(String name, double widthInPoints, double heightInPoints, double marginInPoints, double gridInPoints, boolean isPhysicalPaperSize, Game game) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        name = name.trim();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }
        if (gridInPoints == 0) {
            throw new IllegalArgumentException("gridInPoints cannot be 0");
        }
        if (widthInPoints > heightInPoints) {
            double swapTemp = widthInPoints;
            widthInPoints = heightInPoints;
            heightInPoints = swapTemp;
        }
        PaperProperties portrait = new PaperProperties(name, widthInPoints, heightInPoints, PaperProperties.PORTRAIT, marginInPoints, gridInPoints, isPhysicalPaperSize, game);
        temporary.add(portrait);
        temporary.add(portrait.deriveOrientation(PaperProperties.LANDSCAPE));
    }

    /**
     * Returns a set of all user-defined paper types at the time that the method
     * is called.
     *
     * @return a set of user-defined papers
     */
    public static Set<PaperProperties> getUserDefinedPapers() {
        LinkedHashSet<PaperProperties> user = new LinkedHashSet<>();
        Settings s = Settings.getUser();
        synchronized (s) {
            for (int i = 1;; ++i) {
                PaperProperties[] pl = readPaperAtIndex(true, i);
                if (pl == null) {
                    break;
                }
                user.add(pl[0]);
                user.add(pl[1]);
            }
        }
        return user;
    }

    /**
     * Replaces the current set of user-defined paper types with the specified
     * set. It is strongly recommended that the specified set be of a type that
     * uses an ordered iterator, such as {@code LinkedHashSet}, as this
     * will determine the order that the paper types will be written in.
     * (Ordered sets are always returned by the methods in this class.)
     *
     * <p>
     * <b>Important:</b> This set should include only the portrait <b>or</b>
     * landscape version of each paper size. Portrait and landscape pairs will
     * be generated automatically when the list is read in using
     * {@link #getUserDefinedPapers()}.
     *
     * @param papers the set of paper properties that will replace the current
     * user-defined paper set
     * @throws NullPointerException if the set is {@code null} or if it
     * contains {@code null}
     */
    public static void setUserDefinedPapers(Set<PaperProperties> papers) {
        if (papers == null) {
            throw new NullPointerException("papers");
        }
        if (papers.contains(null)) {
            throw new NullPointerException("null entry in papers");
        }

        StrangeEons.log.info("Updating user-defined paper types:");
        int index = 1;
        for (PaperProperties pp : papers) {
            StrangeEons.log.info(pp.toDebugString());
            writeCustomPaperToIndex(index++, pp);
        }
        writeCustomPaperToIndex(index, null);
        RawSettings.writeUserSettings();
    }

    /**
     * Returns an ordered set of all of the paper types that match the specified
     * criteria. The criteria are specified as a map using the keys and values
     * defined in this class.
     *
     * @param criteria a map of the criteria to match
     * @return a set of the papers that match the specified criteria
     */
    public static Set<PaperProperties> getMatchingPapers(Map<?, ?> criteria) {
        if (criteria == null) {
            throw new NullPointerException("properties");
        }

        LinkedHashSet<PaperProperties> set = new LinkedHashSet<>();

        Object origin = criteria.get(KEY_ORIGIN);
        if (origin == null || origin == VALUE_ORIGIN_BUILT_IN) {
            set.addAll(getBuiltInPapers());
        }
        if (origin == null || origin == VALUE_ORIGIN_TEMPORARY) {
            set.addAll(getTemporaryPapers());
        }
        if (origin == null || origin == VALUE_ORIGIN_USER_DEFINED) {
            set.addAll(getUserDefinedPapers());
        }

        List<PaperProperties> rejected = new LinkedList<>();
        for (PaperProperties pp : set) {
            if (!matchOtherThanOrigin(criteria, pp)) {
                rejected.add(pp);
            }
        }

        set.removeAll(rejected);
        return set;
    }

    /**
     * Matches the given paper properties against the keys other than
     * {@code KEY_ORIGIN}.
     *
     * @param map the properties to match
     * @param pp the paper to consider
     * @return {@code true} if it matches the criteria in the map
     */
    private static boolean matchOtherThanOrigin(Map<?, ?> map, PaperProperties pp) {
        Object c = map.get(KEY_CONCRETENESS);
        if (c != null) {
            if (c == VALUE_CONCRETENESS_PHYSICAL) {
                if (!pp.isPhysical()) {
                    return false;
                }
            } else if (c == VALUE_CONCRETENESS_VIRTUAL) {
                if (pp.isPhysical()) {
                    return false;
                }
            } else {
                throw new IllegalArgumentException("not a valid value for KEY_CONCRETENESS");
            }
        }

        Object o = map.get(KEY_ORIENTATION);
        if (o != null) {
            if (o == VALUE_ORIENTATION_LANDSCAPE) {
                if (pp.isPortraitOrientation()) {
                    return false;
                }
            } else if (o == VALUE_ORIENTATION_PORTRAIT) {
                if (!pp.isPortraitOrientation()) {
                    return false;
                }
            } else {
                throw new IllegalArgumentException("not a valid value for KEY_ORIENTATION");
            }
        }

        Object g = map.get(KEY_GAME);
        if (g != null) {
            if (!(g instanceof Game)) {
                throw new IllegalArgumentException("not an instance of Game: " + g);
            }
            String code = ((Game) g).getCode();
            if (!code.equals(pp.getGameCode()) && !pp.getGameCode().equals(Game.ALL_GAMES_CODE)) {
                return false;
            }
        }

        Object m = map.get(KEY_MAXIMUM_SIZE);
        if (m != null) {
            if (!(m instanceof Number)) {
                throw new IllegalArgumentException("not a Number: " + m);
            }
            double v = ((Number) m).doubleValue();
            if (pp.getPageWidth() > v || pp.getPageHeight() > v) {
                return false;
            }
        }

        m = map.get(KEY_MINIMUM_SIZE);
        if (m != null) {
            if (!(m instanceof Number)) {
                throw new IllegalArgumentException("not a Number: " + m);
            }
            double v = ((Number) m).doubleValue();
            if (pp.getPageWidth() < v || pp.getPageHeight() < v) {
                return false;
            }
        }

        m = map.get(KEY_SMALLER_THAN);
        if (m != null) {
            if (!(m instanceof PaperProperties)) {
                throw new IllegalArgumentException("not a PaperProperties: " + m);
            }
            PaperProperties target = (PaperProperties) m;
            double mArea = target.getPageWidth() * target.getPageHeight();
            double ppArea = pp.getPageWidth() * pp.getPageHeight();
            if (mArea >= ppArea) {
                return false;
            }
        }

        return true;
    }

    /**
     * Reads the paper entry with the specified index from settings and returns
     * an array of two {@link PaperProperties} instances in portrait, landscape
     * order.
     *
     * @param userDefined if {@code true}, read from the user defined paper
     * list; otherwise, read from the built-in paper list
     * @param index the index of the entry (from one up)
     * @return an array of the portrait and landscape versions of the entry, or
     * {@code null} if there the index if past the end of available entries
     */
    private static PaperProperties[] readPaperAtIndex(boolean userDefined, int index) {
        String prefix = userDefined ? "user-" : "";
        if (index < 0) {
            throw new IndexOutOfBoundsException("index must be >= 1: " + index);
        }

        String name = RawSettings.getSetting(prefix + "paper-name-" + index);
        if (name == null) {
            return null;
        }

        double width = DEF_WIDTH;
        try {
            width = Double.valueOf(RawSettings.getSetting(prefix + "paper-width-" + index));
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "bad paper width at entry " + prefix + index, e);
        }

        double height = 11 * 72d;
        try {
            height = Double.valueOf(RawSettings.getSetting(prefix + "paper-height-" + index));
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "bad paper height at entry " + prefix + index, e);
        }

        if (width <= 0) {
            width = DEF_WIDTH;
        }
        if (height <= 0) {
            height = DEF_HEIGHT;
        }

        // note that we expect that the margin/grid will not usually be defined
        Settings rk = Settings.getShared();

        double margin = DEF_MARGIN;
        String v = rk.get(prefix + "paper-margin-size-" + index);
        if (v != null) {
            margin = Settings.number(v);
        }

        double grid = DEF_GRID;
        v = rk.get(prefix + "paper-grid-size-" + index);
        if (v != null) {
            margin = Settings.number(v);
        }

        boolean physical = !rk.getBoolean(prefix + "paper-is-pseudo-" + index);

        return new PaperProperties[]{
            new PaperProperties(name, width, height, PaperProperties.PORTRAIT, margin, grid, physical, null),
            new PaperProperties(name, width, height, PaperProperties.LANDSCAPE, margin, grid, physical, null)
        };
    }

    /**
     * Writes paper properties at the specified index.
     *
     * @param i the index, from one up
     * @param pp the paper type to write; if {@code null}, deletes the type
     * at the index
     */
    private static void writeCustomPaperToIndex(int i, PaperProperties pp) {
        if (pp == null) {
            RawSettings.removeUserSetting("user-paper-name-" + i);
            RawSettings.removeUserSetting("user-paper-width-" + i);
            RawSettings.removeUserSetting("user-paper-height-" + i);
            RawSettings.removeUserSetting("user-paper-grid-size-" + i);
            RawSettings.removeUserSetting("user-paper-margin-size-" + i);
            RawSettings.removeUserSetting("user-paper-is-pseudo-" + i);
        } else {
            // for neatness, always write the paper in portrait orientation
            // (the orientation doesn't matter in practice)
            pp = pp.deriveOrientation(PaperProperties.PORTRAIT);
            RawSettings.setUserSetting("user-paper-name-" + i, pp.getName());
            RawSettings.setUserSetting("user-paper-width-" + i, Double.toString(pp.getPageWidth()));
            RawSettings.setUserSetting("user-paper-height-" + i, Double.toString(pp.getPageHeight()));
            RawSettings.setUserSetting("user-paper-grid-size-" + i, Double.toString(pp.getGridSeparation()));
            RawSettings.setUserSetting("user-paper-margin-size-" + i, Double.toString(pp.getMargin()));
            RawSettings.setUserSetting("user-paper-is-pseudo-" + i, pp.isPhysical() ? "no" : "yes");
        }
    }

    // HACK: Copies of private values in PaperProperties
    private static final double DEF_WIDTH = 8.5d * 72d;
    private static final double DEF_HEIGHT = 11d * 72d;
    private static final double DEF_GRID = 72d * 0.393701d;
    private static final double DEF_MARGIN = DEF_GRID * 2d;

    /**
     * Returns a model suitable for use in combo boxes that allows selection
     * from the specified set of paper types.
     *
     * @param papers the set to create a model for
     * @return a combo box model of the requested papers
     * @throws NullPointerException if the paper set is {@code null}
     */
    public static DefaultComboBoxModel setToComboBoxModel(Set<PaperProperties> papers) {
        if (papers == null) {
            throw new NullPointerException("papers");
        }
        DefaultComboBoxModel m = new DefaultComboBoxModel();
        for (PaperProperties pp : papers) {
            m.addElement(pp);
        }
        return m;
    }

    /**
     * Returns a model suitable for use in {@code JList}s that allows
     * selection from the specified set of paper types.
     *
     * @param papers the set to create a model for
     * @return a list model of the requested papers
     * @throws NullPointerException if the paper set is {@code null}
     */
    public static DefaultListModel setToListModel(Set<PaperProperties> papers) {
        if (papers == null) {
            throw new NullPointerException("papers");
        }

        DefaultListModel m = new DefaultListModel();
        for (PaperProperties pp : papers) {
            m.addElement(pp);
        }
        return m;
    }

    /**
     * Creates a set of paper types that match the contents of the specified
     * list or combo box model ({@code ComboBoxModel} extends
     * {@code ListModel}).
     *
     * @param model the model to create a set for
     * @return a set with the same papers as the model
     * @throws NullPointerException if the model is {@code null}
     */
    public static Set<PaperProperties> modelToSet(ListModel model) {
        if (model == null) {
            throw new NullPointerException("model");
        }

        LinkedHashSet<PaperProperties> papers = new LinkedHashSet<>();
        for (int i = 0; i < model.getSize(); ++i) {
            papers.add((PaperProperties) model.getElementAt(i));
        }
        return papers;
    }

    /**
     * A utility method that returns a new list cell renderer suitable for
     * displaying a list of paper types. Using this method ensures consistency
     * for paper lists and combo boxes.
     *
     * @return a list cell renderer for sets of paper properties instances
     */
    public static ListCellRenderer createListCellRenderer() {
        return new JIconList.IconRenderer();
    }
}
