package ca.cgjennings.layout;

/**
 * Setting a default {@code EvaluatorFactory} for markup renderers allows
 * them to handle script tags.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public interface EvaluatorFactory {

    public Evaluator createEvaluator(MarkupRenderer renderer);
}
