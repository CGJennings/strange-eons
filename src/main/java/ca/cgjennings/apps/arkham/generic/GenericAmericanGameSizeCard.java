package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.2" by 3.43" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericAmericanGameSizeCard extends GenericCardBase {
    public GenericAmericanGameSizeCard() {
        super("am", new Length(2.2d, Length.IN), new Length(3.43d, Length.IN));
    }
}