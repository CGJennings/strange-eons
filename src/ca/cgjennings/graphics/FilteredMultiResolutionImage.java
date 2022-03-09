package ca.cgjennings.graphics;

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
        private Image cacheFiltered;
        
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
                Image unfilteredBase = null;
                
                // we want to avoid generating all of the variants (else clause)
                // so we look for various specialized ways to avoid it based
                // on common implementations of MultiResolutionImage
                if (sourceImage instanceof FilteredMultiResolutionImage) {
                    unfilteredBase = ((FilteredMultiResolutionImage) sourceImage).getBaseImage();
                } else if (sourceImage instanceof MultiResolutionImageResource) {
                    unfilteredBase = ((MultiResolutionImageResource) sourceImage).getBaseImage();
                } else if (sourceImage instanceof Image) {
                    final Image si = (Image) sourceImage;
                    unfilteredBase = sourceImage.getResolutionVariant(si.getWidth(null), si.getHeight(null));
                } else {
                    unfilteredBase = base = sourceImage.getResolutionVariants().get(0);
                }
                
                base = applyFilter(unfilteredBase);
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
        public abstract Image applyFilter(Image source);

        @Override
        public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
            Image var = sourceImage.getResolutionVariant(destImageWidth, destImageHeight);
            if (var != cacheSource || cacheFiltered == null) {
                cacheSource = var;
                cacheFiltered = applyFilter(var);
            }
            return cacheFiltered;
        }

        @Override
        public List<Image> getResolutionVariants() {
            List<Image> sourceList = sourceImage.getResolutionVariants();
            List<Image> destList = new ArrayList<>(sourceList.size());
            sourceList.forEach(im -> destList.add(applyFilter(im)));
            return destList;
        }
}