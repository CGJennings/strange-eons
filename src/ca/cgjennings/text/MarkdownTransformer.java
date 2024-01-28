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
 *
 * @author chris
 */
public class MarkdownTransformer {

    protected Parser parser;
    protected Renderer renderer;
    private String defaultLanguage = null;

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

    public void setDefaultCodeBlockLanguage(String languageTag) {
        this.defaultLanguage = languageTag;
    }

    public String getDefaultCodeBlockLanguage() {
        return defaultLanguage;
    }

    public String toHtmlDocument(String markdownInput) {
        return toHtmlDocument(markdownInput, null);
    }

    public String toHtmlDocument(String markdownInput, String title) {
        String titleElement = title == null ? ""
                : "    <title>" + EscapeUtil.escapeHtml(title) + "</title>\n";

        return "<!DOCTYPE html>\n<html>\n"
                + "  <head>\n" + title
                + "  </head>\n<body>\n"
                + render(markdownInput).trim()
                + "\n</body>\n</html>";
    }

    public String render(String markdownInput) {
        Node root = parser.parse(markdownInput);
        return renderer.render(root);
    }

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
