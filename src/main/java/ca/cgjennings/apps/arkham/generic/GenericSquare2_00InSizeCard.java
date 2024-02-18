package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2" by 2" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericSquare2_00InSizeCard extends GenericCardBase {
    public GenericSquare2_00InSizeCard() {
        super("sq200", new Length(2d, Length.IN), new Length(2d, Length.IN));
    }
}