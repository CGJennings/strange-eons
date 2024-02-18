package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 1.75" by 3.5" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericDominoSizeCard extends GenericCardBase {
    public GenericDominoSizeCard() {
        super("domino", new Length(1.75d, Length.IN), new Length(3.5d, Length.IN));
    }
}