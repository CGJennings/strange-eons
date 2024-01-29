package ca.cgjennings.apps.arkham.deck.item;

import ca.cgjennings.graphics.filters.BlurFilter;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 *
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
class DropShadow extends OldDropShadow {

    public DropShadow() {
    }

    public DropShadow(Color color, int size, float opacity) {
        super(color, size, opacity);
    }

    @Override
    public BufferedImage createShadowImage(BufferedImage shadowMask) {
//		if( highQuality ) {
//			BufferedImage shadow = new BufferedImage( shadowMask.getWidth(), shadowMask.getHeight(), BufferedImage.TYPE_INT_ARGB );
//			getLinearBlurOp(shadowSize, 1).filter(shadowMask, shadow);
//			getLinearBlurOp(1, shadowSize).filter(shadow, shadowMask);
//			return shadowMask;
//		} else {
//			fastShadow( shadowMask );
//			return shadowMask;
//		}

        //ColorOverlayFilter paint = new ColorOverlayFilter6
        BlurFilter blurOp = new BlurFilter(getShadowSize(), 1);
        return blurOp.filter(shadowMask, null);
    }
}
