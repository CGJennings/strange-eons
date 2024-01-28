package ca.cgjennings.ui.textedit;

/*
 * This is an adaptation of the {@code RSyntaxTextArea} error strip to customize
 * how the markers are painted. The adapted code is used under a BSD license, as
 * described in the about dialog template.
 */

import ca.cgjennings.ui.theme.Palette;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.TaskTagParser.TaskNotice;
import org.fife.ui.rtextarea.RTextArea;

/**
 * Displays markers for points of interest in a code editor.
 *
 * @author Robert Futrell (original author)
 * @author Chris Jennings (adaptation and customization)
 */
final class ErrorStrip extends JPanel {

    /**
     * The text area.
     */
    private RSyntaxTextArea textArea;

    /**
     * Listens for events in this component.
     */
    private transient Listener listener;

    /**
     * Whether "marked occurrences" in the text area should be shown in this
     * error strip.
     */
    private boolean showMarkedOccurrences;

    /**
     * Whether markers for "mark all" highlights should be shown in this error
     * strip.
     */
    private boolean showMarkAll;

    /**
     * Only notices of this severity (or worse) will be displayed in this error
     * strip.
     */
    private ParserNotice.Level levelThreshold;

    /**
     * Whether the caret marker's location should be rendered.
     */
    private boolean followCaret;

    /**
     * The color to use for the caret marker.
     */
    private Color caretMarkerColor;

    /**
     * Where we paint the caret marker.
     */
    private int caretLineY;

    /**
     * The last location of the caret marker.
     */
    private int lastLineY;

    /**
     * Generates the tool tips for markers in this error strip.
     */
    private transient ErrorStripMarkerToolTipProvider markerToolTipProvider;

    /**
     * The preferred width of this component.
     */
    private static final int PREFERRED_WIDTH = 14;

    private static final ResourceBundle MSG = ResourceBundle.getBundle(
            "org.fife.ui.rsyntaxtextarea.ErrorStrip");

    /**
     * Constructor.
     *
     * @param textArea The text area we are examining.
     */
    public ErrorStrip(RSyntaxTextArea textArea) {
        this.textArea = textArea;
        listener = new Listener();
        ToolTipManager.sharedInstance().registerComponent(this);
        setLayout(null); // Manually layout Markers as they can overlap
        addMouseListener(listener);
        setShowMarkedOccurrences(true);
        setShowMarkAll(true);
        setLevelThreshold(ParserNotice.Level.WARNING);
        setFollowCaret(true);
        setCaretMarkerColor(getDefaultCaretMarkerColor());
        setMarkerToolTipProvider(null); // Install default
    }

    /**
     * Overridden so we only start listening for parser notices when this
     * component (and presumably the text area) are visible.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        textArea.addCaretListener(listener);
        textArea.addPropertyChangeListener(
                RSyntaxTextArea.PARSER_NOTICES_PROPERTY, listener);
        textArea.addPropertyChangeListener(
                RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY, listener);
        textArea.addPropertyChangeListener(
                RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY, listener);
        textArea.addPropertyChangeListener(
                RSyntaxTextArea.MARK_ALL_OCCURRENCES_CHANGED_PROPERTY, listener);
        refreshMarkers();
    }

    /**
     * Manually manages layout since this component uses no layout manager.
     */
    @Override
    public void doLayout() {
        for (int i = 0; i < getComponentCount(); i++) {
            Marker m = (Marker) getComponent(i);
            m.updateLocation();
        }
        listener.caretUpdate(null); // Force recalculation of caret line pos
    }

    /**
     * returns the color to use when painting the caret marker.
     *
     * @return The caret marker color.
     * @see #setCaretMarkerColor(Color)
     */
    public Color getCaretMarkerColor() {
        return caretMarkerColor;
    }

    /**
     * Returns the default color for the caret marker. This is a UI resource so
     * that it is updated if the LookAndFeel is updated, but not if the user
     * overrides it.
     *
     * @return The default color.
     */
    private ColorUIResource getDefaultCaretMarkerColor() {
        return new ColorUIResource(Palette.get.foreground.translucent.text);
    }

    /**
     * Returns whether the caret's position should be drawn.
     *
     * @return Whether the caret's position should be drawn.
     * @see #setFollowCaret(boolean)
     */
    public boolean getFollowCaret() {
        return followCaret;
    }

    @Override
    public Dimension getPreferredSize() {
        int height = textArea.getPreferredScrollableViewportSize().height;
        return new Dimension(PREFERRED_WIDTH, height);
    }

    /**
     * Returns the minimum severity a parser notice must be for it to be
     * displayed in this error strip. This will be one of the constants defined
     * in the <code>ParserNotice</code> class.
     *
     * @return The minimum severity.
     * @see
     * #setLevelThreshold(org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level)
     */
    public ParserNotice.Level getLevelThreshold() {
        return levelThreshold;
    }

    /**
     * Returns whether "mark all" highlights are shown in this error strip.
     *
     * @return Whether markers are shown for "mark all" highlights.
     * @see #setShowMarkAll(boolean)
     */
    public boolean getShowMarkAll() {
        return showMarkAll;
    }

    /**
     * Returns whether marked occurrences are shown in this error strip.
     *
     * @return Whether marked occurrences are shown.
     * @see #setShowMarkedOccurrences(boolean)
     */
    public boolean getShowMarkedOccurrences() {
        return showMarkedOccurrences;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        String text = null;
        int line = yToLine(e.getY());
        if (line > -1) {
            text = MSG.getString("Line");
            text = MessageFormat.format(text, line + 1);
        }
        return text;
    }

    /**
     * Returns the y-offset in this component corresponding to a line in the
     * text component.
     *
     * @param line The line.
     * @return The y-offset.
     * @see #yToLine(int)
     */
    private int lineToY(int line) {
        int h = textArea.getVisibleRect().height;
        float lineCount = textArea.getLineCount();
        int lineHeight = textArea.getLineHeight();
        int linesPerVisibleRect = h / lineHeight;

        return (int) (((line - 1) / (Math.max(lineCount, linesPerVisibleRect) - 1)) * (h - 2));
    }

    /**
     * Overridden to (possibly) draw the caret's position.
     *
     * @param g The graphics context.
     */
    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (caretLineY > -1) {
            final int y0 = Math.max(0, caretLineY - 1);
            g.setColor(getCaretMarkerColor());
            g.fillRect(PREFERRED_WIDTH/2 - 3, y0, 5, 4);
            g.drawLine(0, y0+2, getWidth(), y0+2);
        }
    }

    /**
     * Refreshes the markers displayed in this error strip.
     */
    private void refreshMarkers() {

        removeAll(); // listener is removed in Marker.removeNotify()
        Map<Integer, Marker> markerMap = new HashMap<>();

        List<ParserNotice> notices = textArea.getParserNotices();
        for (ParserNotice notice : notices) {
            if (notice.getLevel().isEqualToOrWorseThan(levelThreshold)
                    || (notice instanceof TaskNotice)) {
                Integer key = notice.getLine();
                Marker m = markerMap.get(key);
                if (m == null) {
                    m = new Marker(notice);
                    m.addMouseListener(listener);
                    markerMap.put(key, m);
                    add(m);
                } else {
                    m.addNotice(notice);
                }
            }
        }

        if (getShowMarkedOccurrences() && textArea.getMarkOccurrences()) {
            List<DocumentRange> occurrences = textArea.getMarkedOccurrences();
            addMarkersForRanges(occurrences, markerMap, textArea.getMarkOccurrencesColor());
        }

        if (getShowMarkAll() /*&& textArea.getMarkAll()*/) {
            Color markAllColor = textArea.getMarkAllHighlightColor();
            List<DocumentRange> ranges = textArea.getMarkAllHighlightRanges();
            addMarkersForRanges(ranges, markerMap, markAllColor);
        }

        revalidate();
        repaint();

    }

    /**
     * Adds markers for a list of ranges in the document.
     *
     * @param ranges The list of ranges in the document.
     * @param markerMap A mapping from line number to <code>Marker</code>.
     * @param color The color to use for the markers.
     */
    private void addMarkersForRanges(List<DocumentRange> ranges,
            Map<Integer, Marker> markerMap, Color color) {
        for (DocumentRange range : ranges) {
            int line = 0;
            try {
                line = textArea.getLineOfOffset(range.getStartOffset());
            } catch (BadLocationException ble) { // Never happens
                continue;
            }
            ParserNotice notice = new MarkedOccurrenceNotice(range, color);
            Integer key = line;
            Marker m = markerMap.get(key);
            if (m == null) {
                m = new Marker(notice);
                m.addMouseListener(listener);
                markerMap.put(key, m);
                add(m);
            } else {
                if (!m.containsMarkedOccurence()) {
                    m.addNotice(notice);
                }
            }
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        textArea.removeCaretListener(listener);
        textArea.removePropertyChangeListener(
                RSyntaxTextArea.PARSER_NOTICES_PROPERTY, listener);
        textArea.removePropertyChangeListener(
                RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY, listener);
        textArea.removePropertyChangeListener(
                RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY, listener);
        textArea.removePropertyChangeListener(
                RSyntaxTextArea.MARK_ALL_OCCURRENCES_CHANGED_PROPERTY, listener);
    }

    /**
     * Sets the color to use when painting the caret marker.
     *
     * @param color The new caret marker color.
     * @see #getCaretMarkerColor()
     */
    public void setCaretMarkerColor(Color color) {
        if (color != null) {
            caretMarkerColor = color;
            listener.caretUpdate(null); // Force repaint
        }
    }

    /**
     * Toggles whether the caret's current location should be drawn.
     *
     * @param follow Whether the caret's current location should be followed.
     * @see #getFollowCaret()
     */
    public void setFollowCaret(boolean follow) {
        if (followCaret != follow) {
            if (followCaret) {
                repaint(0, caretLineY, getWidth(), 2); // Erase
            }
            caretLineY = -1;
            lastLineY = -1;
            followCaret = follow;
            listener.caretUpdate(null); // Possibly repaint
        }
    }

    /**
     * Sets the minimum severity a parser notice must be for it to be displayed
     * in this error strip. This should be one of the constants defined in the
     * <code>ParserNotice</code> class. The default value is
     * {@link org.fife.ui.rsyntaxtextarea.parser.ParserNotice.Level#WARNING}.
     *
     * @param level The new severity threshold.
     * @see #getLevelThreshold()
     * @see ParserNotice
     */
    public void setLevelThreshold(ParserNotice.Level level) {
        levelThreshold = level;
        if (isDisplayable()) {
            refreshMarkers();
        }
    }

    /**
     * Sets the provider of tool tips for markers in this error strip.
     * Applications can use this method to control the content and format of the
     * tool tip descriptions of line markers.
     *
     * @param provider The provider. If this is <code>null</code>, a default
     * implementation will be used.
     */
    public void setMarkerToolTipProvider(ErrorStripMarkerToolTipProvider provider) {
        markerToolTipProvider = provider != null ? provider
                : new DefaultErrorStripMarkerToolTipProvider();
    }

    /**
     * Sets whether "mark all" highlights are shown in this error strip.
     *
     * @param show Whether to show markers for "mark all" highlights.
     * @see #getShowMarkAll()
     */
    public void setShowMarkAll(boolean show) {
        if (show != showMarkAll) {
            showMarkAll = show;
            if (isDisplayable()) { // Skip this when we're first created
                refreshMarkers();
            }
        }
    }

    /**
     * Sets whether marked occurrences are shown in this error strip.
     *
     * @param show Whether to show marked occurrences.
     * @see #getShowMarkedOccurrences()
     */
    public void setShowMarkedOccurrences(boolean show) {
        if (show != showMarkedOccurrences) {
            showMarkedOccurrences = show;
            if (isDisplayable()) { // Skip this when we're first created
                refreshMarkers();
            }
        }
    }

    @Override
    public void updateUI() {

        super.updateUI();

        if (caretMarkerColor instanceof ColorUIResource) {
            setCaretMarkerColor(getDefaultCaretMarkerColor());
        }
    }

    /**
     * Returns the line in the text area corresponding to a y-offset in this
     * component.
     *
     * @param y The y-offset.
     * @return The line.
     * @see #lineToY(int)
     */
    private int yToLine(int y) {
        int line = -1;
        int h = textArea.getVisibleRect().height;

        int lineHeight = textArea.getLineHeight();
        int linesPerVisibleRect = h / lineHeight;
        int lineCount = textArea.getLineCount();

        if (y < h) {
            float at = y / (float) h;
            line = Math.round((Math.min(lineCount, linesPerVisibleRect) - 1) * at);
        }
        return line;
    }

    /**
     * The default implementation of the provider of tool tips for markers in an
     * error strip.
     *
     * @author predi
     */
    private static class DefaultErrorStripMarkerToolTipProvider
            implements ErrorStripMarkerToolTipProvider {

        @Override
        public String getToolTipText(List<ParserNotice> notices) {

            String text;

            if (notices.size() == 1) {
                text = notices.get(0).getMessage();
            } else { // > 1
                StringBuilder sb = new StringBuilder("<html>");
                sb.append(MSG.getString("MultipleMarkers"));
                sb.append("<br>");
                for (ParserNotice pn : notices) {
                    sb.append("&nbsp;&nbsp;&nbsp;- ");
                    sb.append(pn.getMessage());
                    sb.append("<br>");
                }
                text = sb.toString();
            }

            return text;

        }

    }

    /**
     * Returns tool tip text for the markers in an {@link ErrorStrip} that
     * denote one or more parser notices.
     *
     * @author predi
     */
    public interface ErrorStripMarkerToolTipProvider {

        /**
         * Returns the tool tip text for a marker in an <code>ErrorStrip</code>
         * that denotes a given list of parser notices.
         *
         * @param notices The list of parser notices.
         * @return The tool tip text. This may be HTML. Returning
         * <code>null</code> will result in no tool tip being displayed.
         */
        String getToolTipText(List<ParserNotice> notices);

    }

    /**
     * Listens for events in the error strip and its markers.
     */
    private class Listener extends MouseAdapter
            implements PropertyChangeListener, CaretListener {

        private Rectangle visibleRect = new Rectangle();

        @Override
        public void caretUpdate(CaretEvent e) {
            if (getFollowCaret()) {
                textArea.computeVisibleRect(visibleRect);
                int h = visibleRect.height;

                int lineHeight = textArea.getLineHeight();
                int linesPerVisibleRect = h / lineHeight;
                int lineCount = textArea.getLineCount();

                int line = textArea.getCaretLineNumber();
                float percent = line / (float) (Math.max(linesPerVisibleRect, lineCount) - 1);

                caretLineY = (int) (visibleRect.height * percent);
                if (caretLineY != lastLineY) {
                    repaint(0, Math.max(0, lastLineY-1), getWidth(), 4); // Erase old position
                    repaint(0, Math.max(0, caretLineY-1), getWidth(), 4);
                    lastLineY = caretLineY;
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {

            Component source = (Component) e.getSource();
            if (source instanceof Marker) {
                ((Marker) source).mouseClicked(e);
                return;
            }

            int line = yToLine(e.getY());
            if (line > -1) {
                try {
                    int offs = textArea.getLineStartOffset(line);
                    textArea.setCaretPosition(offs);
                } catch (BadLocationException ble) { // Never happens
                    UIManager.getLookAndFeel().provideErrorFeedback(textArea);
                }
            }

        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {

            String propName = e.getPropertyName();

            // If they change whether marked occurrences are visible in editor
            if (RSyntaxTextArea.MARK_OCCURRENCES_PROPERTY.equals(propName)) {
                if (getShowMarkedOccurrences()) {
                    refreshMarkers();
                }
            } // If parser notices changed.
            // TODO: Don't update "mark all/occurrences" markers.
            else if (RSyntaxTextArea.PARSER_NOTICES_PROPERTY.equals(propName)) {
                refreshMarkers();
            } // If marked occurrences changed.
            // TODO: Only update "mark occurrences" markers, not all of them.
            else if (RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY.
                    equals(propName)) {
                if (getShowMarkedOccurrences()) {
                    refreshMarkers();
                }
            } // If "mark all" occurrences changed.
            // TODO: Only update "mark all" markers, not all of them.
            else if (RTextArea.MARK_ALL_OCCURRENCES_CHANGED_PROPERTY.
                    equals(propName)) {
                if (getShowMarkAll()) {
                    refreshMarkers();
                }
            }

        }

    }

    /**
     * A notice that wraps a "marked occurrence" instance.
     */
    private class MarkedOccurrenceNotice implements ParserNotice {

        private DocumentRange range;
        private Color color;

        MarkedOccurrenceNotice(DocumentRange range, Color color) {
            this.range = range;
            this.color = color;
        }

        @Override
        public int compareTo(ParserNotice other) {
            return 0; // Value doesn't matter
        }

        @Override
        public boolean containsPosition(int pos) {
            return pos >= range.getStartOffset() && pos < range.getEndOffset();
        }

        @Override
        public boolean equals(Object o) {
            // FindBugs - Define equals() when defining compareTo()
            if (!(o instanceof ParserNotice)) {
                return false;
            }
            return compareTo((ParserNotice) o) == 0;
        }

        @Override
        public Color getColor() {
            return color;
        }

        @Override
        public boolean getKnowsOffsetAndLength() {
            return true;
        }

        @Override
        public int getLength() {
            return range.getEndOffset() - range.getStartOffset();
        }

        @Override
        public Level getLevel() {
            return Level.INFO; // Won't matter
        }

        @Override
        public int getLine() {
            try {
                return textArea.getLineOfOffset(range.getStartOffset()) + 1;
            } catch (BadLocationException ble) {
                return 0;
            }
        }

        @Override
        public String getMessage() {
            String text = null;
            try {
                String word = textArea.getText(range.getStartOffset(),
                        getLength());
                text = MSG.getString("OccurrenceOf");
                text = MessageFormat.format(text, word);
            } catch (BadLocationException ble) {
                UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            }
            return text;
        }

        @Override
        public int getOffset() {
            return range.getStartOffset();
        }

        @Override
        public Parser getParser() {
            return null;
        }

        @Override
        public boolean getShowInEditor() {
            return false; // Value doesn't matter
        }

        @Override
        public String getToolTipText() {
            return null;
        }

        @Override
        public int hashCode() { // FindBugs, since we override equals()
            return 0; // Value doesn't matter for us.
        }

    }

    /**
     * A "marker" in this error strip, representing one or more notices.
     */
    private class Marker extends JComponent {

        private List<ParserNotice> notices;

        Marker(ParserNotice notice) {
            notices = new ArrayList<>(1); // Usually just 1
            addNotice(notice);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setSize(getPreferredSize());
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        public void addNotice(ParserNotice notice) {
            notices.add(notice);
        }

        public boolean containsMarkedOccurence() {
            boolean result = false;
            for (ParserNotice notice : notices) {
                if (notice instanceof MarkedOccurrenceNotice) {
                    result = true;
                    break;
                }
            }
            return result;
        }

        public Color getColor() {
            // Return the color for the highest-level parser.
            Color c = null;
            int lowestLevel = Integer.MAX_VALUE; // ERROR is 0
            for (ParserNotice notice : notices) {
                if (notice.getLevel().getNumericValue() < lowestLevel) {
                    lowestLevel = notice.getLevel().getNumericValue();
                    c = notice.getColor();
                }
            }
            return c;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(PREFERRED_WIDTH, 4);
        }

        @Override
        public String getToolTipText() {
            return markerToolTipProvider.getToolTipText(Collections.
                    unmodifiableList(notices));
        }

        protected void mouseClicked(MouseEvent e) {
            ParserNotice pn = notices.get(0);
            int offs = pn.getOffset();
            int len = pn.getLength();
            if (offs > -1 && len > -1) { // These values are optional
                DocumentRange range = new DocumentRange(offs, offs + len);
                RSyntaxUtilities.selectAndPossiblyCenter(textArea, range, true);
            } else {
                int line = pn.getLine();
                try {
                    offs = textArea.getLineStartOffset(line);
                    textArea.getFoldManager().ensureOffsetNotInClosedFold(offs);
                    textArea.setCaretPosition(offs);
                } catch (BadLocationException ble) { // Never happens
                    UIManager.getLookAndFeel().provideErrorFeedback(textArea);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color color = getColor();
            if (color == null) {
                color = Palette.get.foreground.opaque.text;
            }

            g.setColor(color);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            ToolTipManager.sharedInstance().unregisterComponent(this);
            removeMouseListener(listener);
        }

        public void updateLocation() {
            int line = notices.get(0).getLine();
            int y = lineToY(line + 1);
            setLocation(0, Math.max(0, y-1));
        }

    }
}
