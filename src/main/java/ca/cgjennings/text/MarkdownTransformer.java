package ca.cgjennings.text;

import ca.cgjennings.ui.textedit.CodeType;
import ca.cgjennings.ui.textedit.HtmlStyler;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.Renderer;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

/**
 * Converts Markdown content to HTML.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class MarkdownTransformer {

    protected Parser parser;
    protected Renderer renderer;
    private String defaultLanguage = null;

    /**
     * Creates a new transformer.
     */
    public MarkdownTransformer() {
        parser = Parser.builder()
                .extensions(List.of(
                        ImageAttributesExtension.create(),
                        StrikethroughExtension.create(),
                        TablesExtension.create()
                ))
                .build();
        renderer = HtmlRenderer.builder()
                .nodeRendererFactory((context) -> new CodeBlockRenderer(context))
                .percentEncodeUrls(true)
                .urlSanitizer(new DefaultUrlSanitizer(List.of("http", "https", "mailto", "res", "project", "res")))
                .build();
    }

    /**
     * Set the default language tag to use when rendering code blocks that do not
     * specify a language.
     * @param languageTag the language tag to use, which should be a file extension
     * recognized as a {@link CodeType}; may be {@code null} to use no default
     */
    public void setDefaultCodeBlockLanguage(String languageTag) {
        this.defaultLanguage = languageTag;
    }

    /**
     * Retruns the default langauge tag used to highlight code blocks
     * that do not specify a language.
     * @return the default language tag
     */
    public String getDefaultCodeBlockLanguage() {
        return defaultLanguage;
    }

    /**
     * Render the input markdown as a complete HTML document with no title.
     * @param markdownInput
     * @return
     */
    public String toHtmlDocument(String markdownInput) {
        return toHtmlDocument(markdownInput, null);
    }

    /**
     * Render the input markdown as a complete HTML document.
     * 
     * @param markdownInput the markdown to render
     * @param title the title of the document, or {@code null} to omit the title
     * @return the complete HTML document
     */
    public String toHtmlDocument(String markdownInput, String title) {
        String titleElement = title == null ? ""
                : "    <title>" + EscapeUtil.escapeHtml(title) + "</title>\n";

        return "<!DOCTYPE html>\n<html>\n"
                + "  <head>\n" + titleElement
                + "  </head>\n<body>\n"
                + render(markdownInput).trim()
                + "\n</body>\n</html>";
    }

    /**
     * Render the input markdown as a fragment of HTML.
     * @param markdownInput the markdown to render
     * @return the HTML fragment
     */
    public String render(String markdownInput) {
        Node root = parser.parse(markdownInput);
        return renderer.render(root);
    }

    /**
     * A custom renderer for fenced code blocks that uses a {@link HtmlStyler}
     * to syntax highlight the code.
     */
    private class CodeBlockRenderer implements NodeRenderer {

        private final HtmlWriter html;

        public CodeBlockRenderer(HtmlNodeRendererContext context) {
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Collections.<Class<? extends Node>>singleton(FencedCodeBlock.class);
        }

        @Override
        public void render(Node node) {
            FencedCodeBlock codeBlock = (FencedCodeBlock) node;

            String lang = codeBlock.getInfo();
            if (lang == null || lang.isBlank()) {
                lang = defaultLanguage;
            }

            html.line();
            html.tag("pre");
            HtmlStyler styler = createHtmlStyler(lang);
            if (styler != null) {
                styler.setText(codeBlock.getLiteral());
                html.raw(styler.styleAll());
            } else {
                html.text(codeBlock.getLiteral());
            }
            html.tag("/pre");
            html.line();
        }

        private HtmlStyler createHtmlStyler(String lang) {
            if (lang == null) {
                return null;
            }
            lang = lang.trim();
            if (lang.isEmpty()) {
                return null;
            }

            // check the cache 
            HtmlStyler styler = null;
            SoftReference<HtmlStyler> cachedRef = stylerCache.get(lang);
            if (cachedRef != null) {
                styler = cachedRef.get();
            }

            if (styler != null) {
                return styler;
            }

            // try to create and cache a suitable styler
            CodeType ct = CodeType.forFile(new File("s." + lang));
            if (ct == null) {
                return null;
            }
            styler = new HtmlStyler(ct, true, false, true);
            stylerCache.put(lang, new SoftReference<>(styler));
            return styler;
        }
    }
    private static Map<String, SoftReference<HtmlStyler>> stylerCache = new HashMap<>(16);
}
