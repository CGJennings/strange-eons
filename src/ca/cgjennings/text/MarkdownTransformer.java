package ca.cgjennings.text;

import java.net.URL;
import java.util.List;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.Renderer;
import org.commonmark.renderer.html.DefaultUrlSanitizer;
import org.commonmark.renderer.html.HtmlRenderer;

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
}
