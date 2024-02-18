package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.5" by 2.5" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericSquare2_50InSizeCard extends GenericCardBase {
    public GenericSquare2_50InSizeCard() {
        super("sq250", new Length(2.5d, Length.IN), new Length(2.5d, Length.IN));
    }
}