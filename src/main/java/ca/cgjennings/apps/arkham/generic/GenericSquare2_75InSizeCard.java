package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.75" by 2.75" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericSquare2_75InSizeCard extends GenericCardBase {
    public GenericSquare2_75InSizeCard() {
        super("square275", new Length(2.75d, Length.IN), new Length(2.75d, Length.IN));
    }
}