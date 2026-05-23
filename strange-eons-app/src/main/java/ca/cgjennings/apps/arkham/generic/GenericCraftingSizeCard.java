package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 2.66" by 4.7" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericCraftingSizeCard extends GenericCardBase {
    public GenericCraftingSizeCard() {
        super("crafting", new Length(2.66d, Length.IN), new Length(4.7d, Length.IN));
    }
}