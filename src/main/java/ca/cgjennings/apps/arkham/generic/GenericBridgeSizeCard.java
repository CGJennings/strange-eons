package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.25" by 3.5" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericBridgeSizeCard extends GenericCardBase {
    public GenericBridgeSizeCard() {
        super("bridge", new Length(2.25d, Length.IN), new Length(3.5d, Length.IN));
    }
}