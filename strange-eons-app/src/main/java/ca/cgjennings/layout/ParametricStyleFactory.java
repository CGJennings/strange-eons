package ca.cgjennings.layout;

/**
 * This interface represents objects that can create text styles that depend on
 * the value of one or more parameters.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface ParametricStyleFactory {

    public abstract TextStyle createStyle(MarkupRenderer renderer, String[] parameters);
}
