package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.32" by 3.58" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericSkatSizeCard extends GenericCardBase {
    public GenericSkatSizeCard() {
        super("skat", new Length(2.32d, Length.IN), new Length(3.58d, Length.IN));
    }
}