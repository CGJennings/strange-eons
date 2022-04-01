package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.text.EscapeUtil;
import ca.cgjennings.text.MarkdownTransformer;
import ca.cgjennings.ui.textedit.TypeScriptCodeSupport;
import java.util.List;

/**
 * Extended by classes that can be annotated with doc comments.
 * Not all comment parts may be present.
 */
public class DocCommentable {
    public DocCommentable() {
    }
    
    public String kind;
    public String kindModifiers;
    public String name;
    public String display;
    public String documentation;
    public String source;
    public List<DocTag> tags;
    
    public String toMarkup(MarkdownTransformer markdown, String style) {
        StringBuilder b = new StringBuilder(256);
        b.append(style);
        
        String signature = getSignature();
        if (!signature.isEmpty()) {
            b.append("<p class='head'><code class='head'>").append(EscapeUtil.escapeHtml(signature)).append("</code></p>");
        }
        
        if (source != null && !source.isEmpty()) {
            b.append(markdown.render("SOURCE: " + source));
        }
        
        if (documentation != null && !documentation.isEmpty() && !"parameter".equals(signature)) {
            b.append(markdown.render(documentation));
        }
        
        if (tags != null && !tags.isEmpty()) {
            final int len = tags.size();
            for (int i=0; i<len; ++i) {
                DocTag tag = tags.get(i);
                String text = tag.text == null ? "" : tag.text;
                if (tag.name == null || tag.name.isEmpty()) {
                    text = tag.tag + ' ' + text;
                } else {
                    text = "<code class='param'>" + tag.name + "</code><br>" + text;
                }
                b.append(markdown.render(text));
            }
        }
        
        return b.toString();
    }
    
    public String getSignature() {
        if (display != null) {
            return display;
        }
        
        StringBuilder b = new StringBuilder(64);
        b.append(describeModifiers(kindModifiers));
        if (b.length() > 0) {
            b.append(' ');
        }
        if (kind != null) {
            b.append(kind);
        }
        if (b.length() > 0) {
            b.append(' ');
        }
        if (name != null) {
            b.append(name);
        }
        return b.toString();
    }
    
    static String describeModifiers(String kindModifiers) {
        if (kindModifiers == null || kindModifiers.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder(32);
        for (int m=0; m<ORDERED_MODIFIERS.length; ++m) {
            if (kindModifiers.contains(ORDERED_MODIFIERS[m])) {
                b.append(ORDERED_MODIFIERS[m]).append(' ');
            }
        }
        return b.toString();
    }
    
    private static final String[] ORDERED_MODIFIERS = new String[] {
      "deprecated",
      "public",
      "private",
      "protected",
      "static",
      "optional",      
      "abstract"
    };
}
