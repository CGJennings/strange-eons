package ca.cgjennings.apps.arkham.plugins;

import ca.cgjennings.layout.*;
import java.util.Arrays;

/**
 * An evaluator factory that creates evaluators for &lt;script&gt; tags in
 * {@link MarkupRenderer}s.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class StrangeEonsEvaluatorFactory implements EvaluatorFactory {

    @Override
    public Evaluator createEvaluator(MarkupRenderer renderer) {
        return new SEEvaluator(renderer);
    }

    private static class SEEvaluator implements Evaluator {

        private ScriptMonkey monkey;
        private MarkupRenderer renderer;

        public SEEvaluator(MarkupRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public Object evaluateScript(String[] params) {
            checkForMonkey();
            if (monkey != null) {
                monkey.eval("useLibrary('" + params[0].replace("'", "\\'") + "');");
                params = Arrays.copyOfRange(params, 1, params.length);
                return monkey.ambivalentCall("main", (Object[]) params);
            }
            return null;
        }

        @Override
        public Object evaluateExpression(String expr) {
            checkForMonkey();
            if (monkey != null) {
                return monkey.eval(expr);
            }
            return null;
        }

        protected void checkForMonkey() {
            if (monkey == null) {
                monkey = new ScriptMonkey("script tag");
                monkey.bind("Renderer", renderer);
            }
        }
    }
}
