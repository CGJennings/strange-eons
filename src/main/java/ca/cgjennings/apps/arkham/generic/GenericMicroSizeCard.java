package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 1.25" by 1.75" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericMicroSizeCard extends GenericCardBase {
    public GenericMicroSizeCard() {
        super("micro", new Length(1.25d, Length.IN), new Length(1.75d, Length.IN));
    }
}
