package ca.cgjennings.apps.arkham.diy;

import ca.cgjennings.apps.arkham.AbstractGameComponentEditor;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.component.design.AbstractVerbalDesignSupport;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;

/**
 * This class is an adapter for creating {@link AbstractVerbalDesignSupport}s
 * for DIY components. Instead of having to create a subclass that calls a super
 * constructor with arguments, you simply provide a function that does the work
 * of
 * {@link AbstractVerbalDesignSupport#analyze(ca.cgjennings.apps.arkham.component.GameComponent, java.lang.StringBuilder)}.
 *
 * <p>
 * To add verbal design support to a component, create an instance of this class
 * supplied your design analysis function and use the editor's
 * {@link AbstractGameComponentEditor#setDesignSupport} method to install it in
 * the editor:
 * <pre>
 * function designAnalyzer( diy, sb ) {
 *     sb.append( 'support text: ' + diy.fullName );
 *     return true;
 * }
 *
 * ...
 * function createInterface( diy, editor ) {
 *     ...
 *     editor.designSupport = new VerbalDesignSupportAdapter( diy, designAnalyzer );
 * }
 * </pre>
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class VerbalDesignSupportAdapter extends AbstractVerbalDesignSupport<GameComponent> {

    private DesignAnalyzer da;

    /**
     * Creates a new adapter that consults with the specified analyzer function.
     *
     * @param gc the component to analyze
     * @param scriptFunction the script function that will handle the analysis
     */
    public VerbalDesignSupportAdapter(GameComponent gc, DesignAnalyzer scriptFunction) {
        super(gc);
        if (scriptFunction == null) {
            throw new NullPointerException("scriptFunction");
        }
        da = scriptFunction;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This subclass will defer the analysis to the {@link DesignAnalyzer}
     * passed to the constructor.
     */
    @Override
    protected boolean analyze(GameComponent gc, StringBuilder b) {
        try {
            return da.analyze(gc, b);
        } catch (Throwable t) {
            ScriptMonkey.scriptError(t);
            return true;
        }
    }

    /**
     * An interface that represents the script function passed to the adapter.
     * This function performs the function of
     * {@link AbstractVerbalDesignSupport#analyze(ca.cgjennings.apps.arkham.component.GameComponent, java.lang.StringBuilder)}.
     */
    public interface DesignAnalyzer {

        /**
         * Analyzes the component and appends a verbal description to the
         * supplied string builder.
         *
         * @param gc the component to analyze
         * @param b a buffer to use to build up the description
         * @return {@code true} if the design is considered valid, {@code false}
         * otherwise
         */
        public boolean analyze(GameComponent gc, StringBuilder b);
    }
}
