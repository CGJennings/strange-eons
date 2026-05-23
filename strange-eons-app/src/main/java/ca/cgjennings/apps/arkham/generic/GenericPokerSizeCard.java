package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.48" by 3.46" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericPokerSizeCard extends GenericCardBase {
    public GenericPokerSizeCard() {
        super("poker", new Length(2.48d, Length.IN), new Length(3.46d, Length.IN));
    }
}