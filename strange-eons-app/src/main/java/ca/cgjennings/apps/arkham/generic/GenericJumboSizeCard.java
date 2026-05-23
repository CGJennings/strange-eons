package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 3.5" by 5.5" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericJumboSizeCard extends GenericCardBase {
    public GenericJumboSizeCard() {
        super("jumbo", new Length(3.5d, Length.IN), new Length(5.5d, Length.IN));
    }
}