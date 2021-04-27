package ca.cgjennings.layout;

/**
 * Classes that can evaluate script tags implement this.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface Evaluator {

    public Object evaluateScript(String[] parameters);

    public Object evaluateExpression(String expr);
}
