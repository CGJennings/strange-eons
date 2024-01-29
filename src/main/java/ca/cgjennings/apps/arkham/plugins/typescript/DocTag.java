package ca.cgjennings.apps.arkham.plugins.typescript;

/**
 * Content of a JSDoc tag.
 */
public class DocTag {
    public String tag;
    public String name;
    public String text;

    public DocTag(String tag, String name, String text) {
        this.tag = tag;
        this.name = name;
        this.text = text;
    }
    
    @Override
    public String toString() {
        return '@' + tag + ' ' + name + '-' + text;
    }
}
