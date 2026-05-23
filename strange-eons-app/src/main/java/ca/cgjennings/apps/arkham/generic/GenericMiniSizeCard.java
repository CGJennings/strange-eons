package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 1.75" by 2.5" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericMiniSizeCard extends GenericCardBase {
    public GenericMiniSizeCard() {
        super("mini", new Length(1.75d, Length.IN), new Length(2.5d, Length.IN));
    }
}