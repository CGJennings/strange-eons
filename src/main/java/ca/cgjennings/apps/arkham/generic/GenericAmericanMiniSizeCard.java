package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 1.61" by 1.24" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericAmericanMiniSizeCard extends GenericCardBase {
    public GenericAmericanMiniSizeCard() {
        super("am-mini", new Length(1.61d, Length.IN), new Length(1.24d, Length.IN));
    }
}
