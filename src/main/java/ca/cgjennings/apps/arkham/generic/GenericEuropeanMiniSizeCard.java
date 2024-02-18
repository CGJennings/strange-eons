package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

/**
 * A 1.73" by 2.64" generic card.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
public final class GenericEuropeanMiniSizeCard extends GenericCardBase {
    public GenericEuropeanMiniSizeCard() {
        super("eu-mini", new Length(1.73d, Length.IN), new Length(2.64d, Length.IN));
    }
}