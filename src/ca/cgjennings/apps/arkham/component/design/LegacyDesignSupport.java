package ca.cgjennings.apps.arkham.component.design;

import ca.cgjennings.apps.arkham.component.GameComponent;

/**
 * Adapts components with legacy design support implementations to use the new
 * API.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class LegacyDesignSupport extends AbstractVerbalDesignSupport<LegacyDesignSupport.Provider> {

    /**
     * Creates a new legacy design support adapter for a game component with
     * legacy design support methods.
     *
     * @param gc the legacy game component to adapt
     */
    public LegacyDesignSupport(Provider gc) {
        super(gc);
    }

    /**
     * Updates the design support analysis by invoking the legacy design support
     * methods on the adapted component.
     *
     * @param gc the legacy game component
     * @param b a buffer for the analysis content
     * @return <code>true</code> if the component is valid
     */
    @Override
    protected boolean analyze(Provider gc, StringBuilder b) {
        gc.validate();
        b.append(gc.getValidityDescription());
        return gc.isValid();
    }

    /**
     * Implemented by game components that provide design support the pre-SE3.0
     * way.
     *
     * @author Chris Jennings <https://cgjennings.ca/contact>
     * @since 3.0
     */
    public interface Provider extends GameComponent {

        /**
         * Validates the component, updating design support information.
         */
        public void validate();

        /**
         * Returns <code>true</code> if the design is valid.
         *
         * @return <code>false</code> if the component is considered unbalanced
         */
        public boolean isValid();

        /**
         * Returns a verbal analysis of the design.
         *
         * @return text designed to support the user's design process
         */
        public String getValidityDescription();
    }
}
