package ca.cgjennings.ui;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.BlockView;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.HTMLFactory;

/**
 * A drop-in replacement for {@link JEditorPane} with somewhat improved painting
 * quality and performance when rendering the text/html content type.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
@SuppressWarnings("serial")
public class EditorPane extends JEditorPane {

    /**
     * Creates a new <code>EditorPane</code>. The document model is set to
     * <code>null</code>.
     */
    public EditorPane() {
        install();
    }

    /**
     * Creates an <code>EditorPane</code> based on a specified URL for input.
     *
     * @param initialPage the URL
     * @exception IOException if the URL is <code>null</code> or cannot be
     * accessed
     */
    public EditorPane(URL initialPage) throws IOException {
        super(initialPage);
        install();
    }

    /**
     * Creates an <code>EditorPane</code> based on a string containing a URL
     * specification.
     *
     * @param url the URL
     * @exception IOException if the URL is <code>null</code> or cannot be
     * accessed
     */
    public EditorPane(String url) throws IOException {
        super(url);
        install();
    }

    /**
     * Creates an <code>EditorPane</code> that has been initialized to the given
     * text by calling the <code>setContentType</code> and <code>setText</code>
     * methods.
     *
     * @param type mime type of the given text
     * @param text the text to initialize with; may be <code>null</code>
     * @exception NullPointerException if the <code>type</code> parameter is
     * <code>null</code>
     */
    public EditorPane(String type, String text) {
        super(type, text);
        install();
    }

    private void install() {
        setEditorKitForContentType("text/html", new HTMLKit());
    }

    @Override
    public void addNotify() {
        super.addNotify();

        // Java 6: use getParent()
        Container c = SwingUtilities.getUnwrappedParent(this);
        while (c != null && (c instanceof JViewport)) {
            c = SwingUtilities.getUnwrappedParent(c);
        }
        if (c instanceof JScrollPane) {
            ((JScrollPane) c).setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g);
    }

    private static class HTMLKit extends HTMLEditorKit {

        @Override
        public HTMLFactory getViewFactory() {
            return factory;
        }

        HTMLFactory factory = new HTMLFactory() {
            @Override
            public View create(Element elem) {
                AttributeSet attrs = elem.getAttributes();
                Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
                Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
                if (o instanceof HTML.Tag) {
                    HTML.Tag kind = (HTML.Tag) o;
                    if (kind == HTML.Tag.HTML) {
                        return new UBlockView(elem);
                    } else if ((kind == HTML.Tag.P)
                            || (kind == HTML.Tag.H1)
                            || (kind == HTML.Tag.H2)
                            || (kind == HTML.Tag.H3)
                            || (kind == HTML.Tag.H4)
                            || (kind == HTML.Tag.H5)
                            || (kind == HTML.Tag.H6)
                            || (kind == HTML.Tag.DT)) {
                        return new UParagraphView(elem);
                    } else if (kind == HTML.Tag.IMPLIED) {
                        String ws = (String) elem.getAttributes().getAttribute(CSS.Attribute.WHITE_SPACE);
                        if ((ws != null) && ws.equals("pre")) {
                            return super.create(elem);
                        }
                        return new UParagraphView(elem);
                    }
                }
                return super.create(elem);
            }
        };
    }

    private static class UParagraphView extends javax.swing.text.html.ParagraphView {

        private static int MAX_FRAGMENT_SIZE = 100;

        public UParagraphView(Element elem) {
            super(elem);
            strategy = new UParagraphView.HTMLFlowStrategy();
        }

        public static class HTMLFlowStrategy extends FlowStrategy {

            @Override
            protected View createView(FlowView fv, int startOffset, int spanLeft, int rowIndex) {
                View res = super.createView(fv, startOffset, spanLeft, rowIndex);
                if (res.getEndOffset() - res.getStartOffset() > MAX_FRAGMENT_SIZE) {
                    res = res.createFragment(startOffset, startOffset + MAX_FRAGMENT_SIZE);
                }
                return res;
            }
        }

        @Override
        public int getResizeWeight(int axis) {
            return 0;
        }
    }

    private static class UBlockView extends BlockView {

        public UBlockView(Element elem) {
            super(elem, View.Y_AXIS);
        }

        @Override
        protected void layout(int width, int height) {
            if (width < Integer.MAX_VALUE) {
                super.layout(width, height);
            }
        }
    }
}
