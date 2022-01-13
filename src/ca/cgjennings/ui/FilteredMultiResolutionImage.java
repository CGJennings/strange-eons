package ca.cgjennings.ui;

import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.MultiResolutionImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A multi-resolution image that modifies an existing multi-resolution source
 * image by applying effects such as image filters.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class FilteredMultiResolutionImage extends AbstractMultiResolutionImage {
        private MultiResolutionImage sourceImage;
        private Image base;
        private Image cacheSource;
        private Image cacheDisabled;
        
        public FilteredMultiResolutionImage(MultiResolutionImage source) {
            this.sourceImage = Objects.requireNonNull(source, "source");
        }
        
        /**
         * Returns the source image from which filtered images are derived.
         * @return the source image passed to the constructor
         */
        public final MultiResolutionImage getSourceImage() {
            return sourceImage;
        }
        
        @Override
        public Image getBaseImage() {
            if (base == null) {
                if (sourceImage instanceof MultiResolutionImageResource) {
                    base = ((MultiResolutionImageResource) sourceImage).getBaseImage();
                } else {
                    base = sourceImage.getResolutionVariants().get(0);
                }                
                base = applyEffect(sourceImage.getResolutionVariants().get(0));
            }
            return base;
        }
        
        /**
         * Applies this image's effect to a source image. Subclasses must
         * implement the specific effect that they wish to achieve.
         * 
         * @param source the source image
         * @return the new image that applies the desired effects to the source
         */
        public abstract Image applyEffect(Image source);

        @Override
        public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
            Image var = sourceImage.getResolutionVariant(destImageWidth, destImageHeight);
            if (var != cacheSource || cacheDisabled == null) {
                cacheSource = var;
                cacheDisabled = applyEffect(var);
            }
            return cacheDisabled;
        }

        @Override
        public List<Image> getResolutionVariants() {
            List<Image> sourceList = sourceImage.getResolutionVariants();
            List<Image> destList = new ArrayList<>(sourceList.size());
            sourceList.forEach(im -> destList.add(applyEffect(im)));
            return destList;
        }
}