package ca.cgjennings.apps.arkham.component.design;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.ui.EditorPane;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.StringReader;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * An abstract base class for design supports that create verbal reports to
 * describe their design analyses. The reports can include basic HTML
 * formatting.
 *
 * @param <G> the type of game component analyzed by the design support
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractVerbalDesignSupport<G extends GameComponent> implements DesignSupport {

    /**
     * Creates a new verbal design support that will support the specified
     * component.
     *
     * @param gc the game component that the support will analyze
     * @throws NullPointerException if the component is {@code null}
     */
    public AbstractVerbalDesignSupport(G gc) {
        if (gc == null) {
            throw new NullPointerException("gc");
        }
        this.gc = gc;
    }

    /**
     * Returns the game component that this design support will analyze.
     *
     * @return the component supported by this design support; cannot be
     * {@code null}
     */
    @Override
    public final G getGameComponent() {
        return gc;
    }

    /**
     * Returns the design support's verbal report as a string.
     *
     * @return the current report, reanalyzing the component if necessary
     */
    public final String getReport() {
        if (dirty) {
            analyze();
        }
        return report;
    }

    @Override
    public void markChanged() {
        dirty = true;
    }

    @Override
    public boolean isDesignValid() {
        if (dirty) {
            analyze();
        }
        return valid;
    }

    private synchronized void analyze() {
        if (!dirty) {
            return;
        }
        try {
            StringBuilder b = new StringBuilder("<html><body style='font-family: SansSerif; font-size: 11pt'>");
            valid = analyze(gc, b);
            b.append("</body></html>");
            String newReport = b.toString();
            if (!newReport.equals(report)) {
                report = newReport;
                // change the analysis number so that views will know
                // they really do need to be synched
                ++analysis;
            }
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, "design support for " + gc.getFullName() + " threw uncaught exception", e);
        }
        dirty = false;
    }

    /**
     * This method is called when the design needs to reanalyzed. It creates the
     * new report by appending text to the provided string builder and returns a
     * boolean value to indicate whether or not the design is valid.
     *
     * @param gc the game component to analyze (same as
     * {@link #getGameComponent()})
     * @param b a string builder that must be used to build the verbal design
     * report
     * @return {@code true} if the design is considered valid, {@code false}
     * otherwise
     */
    protected abstract boolean analyze(G gc, StringBuilder b);

    @Override
    public void updateSupportView(Component view) {
        if (view == null) {
            throw new NullPointerException("view");
        }
        if (!(view instanceof View)) {
            throw new ClassCastException("not a valid view for this support: " + view);
        }

        if (dirty) {
            analyze();
        }

        View v = (View) view;
        if (v.lastSynchedWithAnalysis != analysis) {
            HTMLEditorKit kit = (HTMLEditorKit) v.getEditorKit();
            HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
            doc.setTokenThreshold(5_000);
            v.setDocument(doc);
            try {
                kit.read(new StringReader(report), doc, 0);
            } catch (Exception e) {
                StrangeEons.log.log(Level.SEVERE, null, e);
            }
            v.lastSynchedWithAnalysis = analysis;
            v.select(0, 0);
        }
    }

    @Override
    public Component createSupportView() {
        return new View();
    }

    @SuppressWarnings("serial")
    private static class View extends EditorPane {

        public View() {
            super("text/html", "<html><body></body></html>");
            setForeground(Color.BLACK);
            setBackground(new Color(250, 255, 196));
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            setEditable(false);
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        }
        private int lastSynchedWithAnalysis = -1;
    }

    private G gc;
    private boolean dirty = true;
    private boolean valid;
    private int analysis = 0;
    private String report;
}
