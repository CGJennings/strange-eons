package ca.cgjennings.text;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.Renderer;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlNodeRendererFactory;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

/**
 *
 * @author chris
 */
public class MarkdownTransformer {

    protected Parser parser;
    protected Renderer renderer;
    
    public MarkdownTransformer() {
        parser = Parser.builder()
                .extensions(List.of(
                        ImageAttributesExtension.create(),
                        StrikethroughExtension.create(),
                        TablesExtension.create()
                ))
                .build();
        renderer = HtmlRenderer.builder()
                .percentEncodeUrls(true)
                .urlSanitizer(new DefaultUrlSanitizer(List.of("http", "https", "mailto", "res", "project", "res")))
                .nodeRendererFactory(new HtmlNodeRendererFactory() {
                    @Override
                    public NodeRenderer create(HtmlNodeRendererContext context) {
                        return new CodeBlockRenderer(context);
                    }
                })
                .build();
    }
    
    public String toHtmlDocument(String markdownInput) {
        return toHtmlDocument(markdownInput, null);
    }
    
    private static String escape(String html) {
        return html.replace("&", "&amp;").replace("<", "&lt");
    }
    
    public String toHtmlDocument(String markdownInput, String title) {
        String titleElement = title == null ? ""
                : "    <title>" + escape(title) + "</title>\n";
        
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
    
    private static class CodeBlockRenderer implements NodeRenderer {

        private final HtmlWriter html;

        public CodeBlockRenderer(HtmlNodeRendererContext context) {
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Collections.<Class<? extends Node>>singleton(IndentedCodeBlock.class);
        }

        @Override
        public void render(Node node) {
            IndentedCodeBlock codeBlock = (IndentedCodeBlock) node;
            html.line();
            html.tag("pre");
            html.text(codeBlock.getLiteral());
            html.tag("/pre");
            html.line();
        }
    }
}
