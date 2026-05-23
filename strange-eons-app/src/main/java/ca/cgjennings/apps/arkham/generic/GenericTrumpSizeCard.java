package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.45" by 3.95" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericTrumpSizeCard extends GenericCardBase {
    public GenericTrumpSizeCard() {
        super("trump", new Length(2.45d, Length.IN), new Length(3.95d, Length.IN));
    }
}