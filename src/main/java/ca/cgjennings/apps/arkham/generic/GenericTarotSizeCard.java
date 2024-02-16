package ca.cgjennings.apps.arkham.generic;

import ca.cgjennings.apps.arkham.Length;

public class GenericTarotSizeCard extends AbstractGenericCard {
    public GenericTarotSizeCard() {
        super("tarot", new Length(2.75d, Length.IN), new Length(4.75d, Length.IN));
    }
}
