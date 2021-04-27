/*

  tints.js - version 4
  Support for card tinting in custom components.


The SE JavaScript Library Copyright © 2008-2013
Christopher G. Jennings and contributors. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

This software is provided by the author "as is" and any express or implied
warranties, including, but not limited to, the implied warranties of
merchantability and fitness for a particular purpose are disclaimed. In no
event shall the author be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to, procurement
of substitute goods or services; loss of use, data, or profits; or business
interruption) however caused and on any theory of liability, whether in
contract, strict liability, or tort (including negligence or otherwise) arising
in any way out of the use of this software, even if advised of the possibility
of such damage.

*/

/**
 * Support for creating tintable game components.
 * Tintable components provide an easy way for users to introduce new
 * variants of basic designs. Tintable components provide one or more
 * <i>tint panels</i>, controls that allow the user to select a colour.
 * The colour(s) selected are then used to recolour selected parts of
 * a component design. For example, it might be applied to a decorative
 * border running around the outside. This library provides the image
 * processing tools needed to apply tinting options to graphics while
 * painting a game component; the <a href='scriptdoc:uicontrols'>uicontrols</a>
 * and <a href='scriptdoc:uibindings'>uibindings</a> libraries provide the
 * controls and logic needed to support tinting on the user interface side.
 *
 * Tinting modified the original image by adusting its <i>hue</i>,
 * <i>saturation</i>, and <i>brightness</i>.  Hue refers to the colour
 * of the image pixels. The change in hue is expressed as an anglular distance
 * around a colour wheel. (So, shifting the hue by 180 degrees would change
 * the colour to its complementary colour, on the opposite side of the colour
 * wheel.) Saturation describes the purity of the image pixels. The lower the saturation,
 * the more grayish and washed out the colour appears. A saturation of 0
 * results in a shade of grey; a saturation of 1 results in the purest possible
 * expression of the hue. The saturation adjustment for tinting
 * is expressed as a scaling factor, a multiplier that is applied to the original
 * value. Brightness describes how light or dark the image appears. A brightness
 * of 0 results in a completely black image. A brightness of 1 results in the
 * brightest possible colour within the limitations of the hue and saturation.
 * Like saturation, the brightness adjustment for tinting is expressed as a
 * scaling factor.
 */


/**
 * TintingFilter [interface]
 * This interface is implemented by all image filters that can be used with a
 * <tt>TintCache</tt> (see below). For details about this interface, refer
 * to its <a href='javadoc:ca/cgjennings/graphics/filters/TintingFilter'>API
 * documentation</a>.
 */
const TintingFilter = ca.cgjennings.graphics.filters.TintingFilter;

/**
 * TintFilter() : AbstractTintingFilter [ctor]
 * This filter shifts the hue angle and scales the saturation and brightness
 * of every pixel in an image. The image's alpha (opacity)
 * channel is not affected, so translucent areas remain translucent.
 *
 * The hue angle is measured using a scale in which 1 is equal to 360 degrees
 * (a full circle). Values will normally range from -0.5 to +0.5 (-180 to
 * +180 degrees), but values outside of this range are acceptable.
 * A value of 0 leaves the original hue unchanged.
 *
 * The saturation and brightness values are factors multiplied against the
 * saturation and brightness of the source pixel. Factors less than 0 are treated
 * as 0. Factors may be more than 1 (100%). If the scaled value for a given
 * pixel is more than 1, it is clamped at 1 in the result.
 *
 * If using an <tt>HSBPanel</tt>, it will always provide a hue between -0.5 and +0.5,
 * and saturation and brightness values between 0 and 1. Depending on the
 * saturation and brightness of the source image, you may wish to scale those
 * values up or down before passing them to the <tt>TintCache</tt>.
 * Otherwise, using the panel it will only be possible to <i>decrease</i>
 * the brightness and saturation (or keep it the same).
 * For example, if you wanted the maximum brightness scale to be 1.2 (120%),
 * multiply the value returned from the <tt>HSBPanel</tt> by 1.2. When choosing
 * the default brightness, use 1/1.2 if you want the default settings to
 * work out to the original image (1/1.2 * 1.2 = 1).
 *
 * For convenience, the class <tt>TintFilter.ScaledTintFilter</tt> can perform
 * scaling for you:
 * <pre>
 * // scale saturation to 120%, brightness to 200%
 * var f = new TintFilter.ScaledTintFilter( h, s, b, 1.2, 2.0 );
 * </pre>
 * A <tt>ScaledTintFilter</tt> gets and sets factors between 0 and 1 as usual,
 * but internally applies the requested scaling factors.
 */

const TintFilter = ca.cgjennings.graphics.filters.TintFilter;

/**
 * TintOverlayFilter() : AbstractTintingFilter [ctor]
 * This filter sets the entire source image to the same HSB colour
 * value (without affecting transparency).
 */
const TintOverlayFilter = ca.cgjennings.graphics.filters.TintOverlayFilter;
const SetTintFilter = TintOverlayFilter; // backwards compatibility with 2.x

/**
 * ReplaceFilter() [ctor]
 * This filter replaces the hue and saturation, and scales the brightness,
 * of the pixels in the source image.
 * This filter can be used to tint a greyscale (black and white) source
 * image. Depending on the brightness of the source image, you may wish to
 * scale the brightness value passed to the filter up in order to cover a
 * wider range of possible tints. (For example, if the average brightness
 * of the source image is 50%, you might scale the brightness value by 2.)
 */
const ReplaceFilter = ca.cgjennings.graphics.filters.ReplaceHueSaturationFilter;

/**
 * Tintable
 * <tt>Tintable</tt> is a Java interface that is implemented by objects
 * that can act as models for an <tt>HSBPanel</tt>. Panels read their
 * initial settings by calling the <tt>Tintable</tt>'s <tt>getTint</tt>
 * method, and call its <tt>setTint</tt> method when the tint controls
 * are adjusted. By implementing the <tt>Tintable</tt> interface you
 * can process messages from the control panel yourself.
 * Normally, however, you do not need to implement this interface.
 * A standard implementation is provided through the
 * <tt><a href="uibindings.html">uibindings</a></tt>
 * library that can read and write tints to a private setting on a component.
 */

const Tintable = arkham.Tintable;

/**
 * Tintable.setTint( hue, saturation, brightness )
 * Called to set the tint being managed by this <tt>Tintable</tt>.
 */
/**
 * Tintable.getTint()
 * Returns an array of three floats that represent the current hue, saturation,
 * and brightness values of the tint being managed by this <tt>Tintable</tt>.
 */


const TintCache = ca.cgjennings.graphics.filters.TintCache;

/**
 * TintCache( filter ) [ctor]
 * A <tt>TintCache</tt> improves drawing performance when working with
 * tints. It takes advantage of the fact that the base image to be tinted
 * doesn't usually change between draws, and that the user only rarely
 * adjusts the tint compared to other editing operations. Instead of applying
 * a tint filter to draw the tinted graphic each time the card is redrawn,
 * you can create a cache, set the filter and base image to use, and then
 * request the tinted version of the image as needed. The cache object
 * will keep a tinted copy of the image available, and update it as needed
 * when the selected tint changes.
 *
 * To create a new tint cache, you must pass in a new instance of
 * the tinting filter class that you wish to use (typically
 * an instance of <tt>TintFilter</tt>).
 *
 * For example:
 * <pre>
 * // during setup:
 * var frontTinter = new TintCache( new TintFilter() );
 * frontTinter.setImage( frontImage );
 *
 * // ...
 *
 * // during drawing:
 * frontTinter.setFactors( hueShift, saturationScale, brightnessScale );
 * var tintedImage = frontTinter.getTintedImage();
 * </pre>
 */

 /**
  * TintCache.setImage( image )
  * Sets the source image that will have tinting applied to it.
  * image : a <tt>BufferedImage</tt> that will be tinted using the tinting filter
  */

 /**
  * TintCache.setFactors( hue, saturation, brightness )
  * Set the HSB tinting factors to use when tinting the source image.
  * hue : the hue adjustment; the exact effect depends on the filter used
  * saturation : the saturation adjustment; the exact effect depends on the filter used
  * brightness : the brightness adjustment; the exact effect depends on the filter used
  */

 /**
  * TintCache.getTintedImage()
  * Returns a tinted image that is created by applying the tinting filter used to
  * construct this cache to the current source image, using the current HSB factors.
  * If the image and factors have not changed since the last call, this may return
  * a cached result.
  *
  * If the current source image data has been modified by writing
  * to the image (for example, if you used as the destination image for some other
  * filtering operation), then you should force any cached result to be cleared before
  * calling this method. An example:
  * <pre>
  * var tc = new TintCache( new TintFilter() );
  * tc.setFactors( -0.25, 1, 0.8 );
  * tc.setImage( source );
  *
  * // ...
  *
  * var tinted = TintCache.getTintedImage();
  *
  * var g = source.createGraphics();
  * try {
  *     // ...
  *     // modify the pixels in "source"
  *     // ...
  * } finally {
  *     g.dispose();
  * }
  *
  * // force clearing cached results before getting tinted version
  * // of the modified source image:
  * tc.setImage( null );
  * tc.setImage( source );
  * tinted = TintCache.getTintedImage();
  * </pre>
  *
  * returns a tinted version of the source image
  */
