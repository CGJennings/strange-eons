package ca.cgjennings.apps.arkham.plugins.typescript;

/**
 * Short summary of the language node at a given file offset.
 */
public class Overview extends DocCommentable {

    public boolean isEmpty() {
        return (display == null || display.isEmpty())
                && (documentation == null || documentation.isEmpty())
                && (tags == null || tags.isEmpty());
    }
    
    @Override
    public String toString() {
        return "Overview{" + "kind=" + kind + ", kindModifiers=" + kindModifiers + ", display=" + display + ", documentation=" + documentation + ", tags=" + tags + '}';
    }
}
