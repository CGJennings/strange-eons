package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.75" by 4.75" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericTarotSizeCard extends GenericCardBase {    
    public GenericTarotSizeCard() {
        super("tarot", new Length(2.75d, Length.IN), new Length(4.75d, Length.IN));
    }
}
