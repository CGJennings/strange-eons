/*

  imageutils.js - version 15
  Support functions for working with bitmapped images.


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
 * Support tools for creating images and image resources.
 * This library defines an object, <tt>ImageUtils</tt>, which contains utility
 * methods for working with images. It is mostly used by resource creation
 * scripts. A resource creation script replaces a resource file with
 * a script capable of generating that resource. The script must define
 * a function, <tt>createResource()</tt> that returns the new resource.
 *
 * One way to use an image resource creation scripts is to break
 * a large image into smaller ones in such a way that they can be stored
 * more efficiently. This reduces the download footprint of the program.
 * A good example is an image that is mostly opaque with a small transparent
 * area. Transparent images are not stored as efficiently as non-transparent
 * ones, so it may be more efficient to split the image into two or more pieces
 * and store each in a separate file. The pieces are then combined into the
 * final image at runtime by the resource creation script.
 */

const ImageUtils = {};

/**
 * Constants
 * ImageUtils.STITCH_HORIZONTAL : indicates that images should be stitched from left to right
 * ImageUtils.STITCH_VERTICAL : indicates that images should be stitched from top to bottom
 */
ImageUtils.STITCH_HORIZONTAL = 1;
ImageUtils.STITCH_VERTICAL = 2;


/**
 * ImageUtils.create( width, height, [hasTransparency] ) [static]
 * Create an image that is <tt>width</tt> pixels by <tt>height</tt> pixels.
 * If <tt>hasTransparency</tt> is <tt>true</tt>, the image will have an alpha
 * channel (allowing transparency and translucency), otherwise the image is
 * opaque.
 *
 * width : the image width, in pixels
 * height : the image height, in pixels
 * hasTransparency : if <tt>true</tt>, create an alpha channel for the image ...
 *     (default is <tt>false</tt>)
 *
 * returns a <tt>BufferedImage</tt> with the requested parameters
 */
ImageUtils.create = function create( width, height, hasTransparency ) {
    var type = hasTransparency ? java.awt.image.BufferedImage.TYPE_INT_ARGB
                               : java.awt.image.BufferedImage.TYPE_INT_RGB;
    return new java.awt.image.BufferedImage( width, height, type );
};



/**
 * ImageUtils.createForResolution( dpi, width, height, [hasTransparency] ) [static]
 * Create an image that is <tt>width</tt> points by <tt>height</tt> points.
 * The conversion of points into pixels is determined by <tt>dpi</tt>, the
 * number of dots (pixels) per inch.
 * If <tt>hasTransparency</tt> is <tt>true</tt>, the image will have an alpha
 * channel (allowing transparency and translucency), otherwise the image is
 * opaque.
 *
 * dpi : the image resolution, in dots (pixels) per inch
 * width : the image width, in points
 * height : the image height, in points
 * hasTransparency : if <tt>true</tt>, create an alpha channel for the image ...
 *     (default is <tt>false</tt>)
 *
 * returns a <tt>BufferedImage</tt> with the requested parameters
 */
ImageUtils.createForResolution = function createForResolution( dpi, width, height, hasTransparency ) {
    return ImageUtils.create( width/72*dpi, height/72*dpi, hasTransparency );
};



/**
 * ImageUtils.get( relativeURL, [cacheResult], [quietly] ) [static]
 * Obtain an image from the Strange Eons resources. If the image cannot be
 * obtained, <tt>null</tt> is returned. Typically this indicates that the
 * URL is incorrect.
 *
 * relativeURL : a URL string relative to the <tt>resources</tt> folder
 * cacheResult : if <tt>true</tt>, the image may be cached to speed future ...
 *     requests (default is <tt>true</tt>)
 * quietly : if <tt>true</tt>, no error message is displayed if loading fails ...
 *     (default is <tt>false</tt>, i.e., display a message)
 *
 * returns the image resource as a <tt>BufferedImage</tt>, or <tt>null</tt>
 */
ImageUtils.get = function get( relativeURL, cacheResult, quietly ) {
    var im = null;
    if( cacheResult || cacheResult === undefined ) {
		if( quietly ) {
			im = resources.ResourceKit.getImageQuietly( relativeURL );
		} else {
			im = resources.ResourceKit.getImage( relativeURL );
		}
    } else {
        var url = resources.ResourceKit.composeResourceURL( relativeURL );
        if( url ) im = javax.imageio.ImageIO.read( url );
        if( im )  im = resources.ResourceKit.prepareNewImage( im );
		if( im == null && !quietly ) {
			arkham.dialog.ErrorDialog.displayErrorOnce(
					resource, string( "rk-err-image-resource", resource ), null
			);
		}
    }
    return im;
};



/**
 * ImageUtils.getIcon( relativeURL, [unthemed] ) [static]
 * Create an <tt>Icon</tt> from an image resource, suitable for use with Swing
 * components. If the icon cannot be obtained, <tt>null</tt> is returned.
 * Typically, this indicates that the URL is incorrect. (Note that is
 * usually safe to set a component's Icon to <tt>null</tt>, as it is
 * interpreted as setting no icon.)
 *
 * relativeURL : a URL string relative to the <tt>resources</tt> folder
 * unthemed : if <tt>true</tt>, then the current theme will not be allowed ...
 *     to substitute a different image (default is <tt>false</tt>)
 *
 * returns the image resource as a <tt>javax.swing.ImageIcon</tt>, or <tt>null</tt>
 */
ImageUtils.getIcon = function getIcon( relativeURL, unthemed ) {
	if( unthemed ) {
		var im = resources.ResourceKit.getImageQuietly( relativeURL );
		return im == null ? null : new swing.ImageIcon( im );
	} else {
		var icon = new ca.cgjennings.ui.theme.ThemedIcon( relativeURL );
		if( icon.image == null ) return null;
		return icon;
	}
};


/**
 * ImageUtils.createIcon( image, [size] )
 * Creates an icon from an existing image. The icon will be resized to fit
 * within <tt>size</tt> by <tt>size</tt> pixels.
 *
 * image : the image to use in the icon
 * size : the target size for the icon (default is 16)
 *
 * returns an icon that displays the image and is the requested size
 */
ImageUtils.createIcon = function createIcon( image, size ) {
	if( size === undefined ) size = 16;
	return ca.cgjennings.graphics.ImageUtilities.createIconForSize( image, size );
};


/**
 * ImageUtils.copy( image )
 * Returns a new copy of <tt>image</tt>. If you are going to draw on an image
 * that you obtained from resources, it is important to work with a copy
 * or you will corrupt the shared version stored in the image cache.
 *
 * returns a copy of the source image
 */
ImageUtils.copy = function copy( image ) {
	return ca.cgjennings.graphics.ImageUtilities.copy( image );
};


/**
 * ImageUtils.stitch( image1, image2, stitchEdge ) [static]
 * Returns a new image that combines two source images by "stitching" them
 * together along an edge. If <tt>stitchEdge</tt> is <tt>ImageUtils.STITCH_HORIZONTAL</tt>,
 * then the right edge of <tt>image1</tt> will be stitched to the left edge of
 * <tt>image2</tt>. If <tt>stitchEdge</tt> is <tt>ImageUtils.STITCH_VERTICAL</tt>,
 * then the bottom edge of <tt>image1</tt> will be stitched to the top edge of
 * <tt>image2</tt> If either source image has transparency, then the returned
 * image will also; otherwise it is opaque.
 *
 * image1 : the first <tt>BufferedImage</tt> to be stitched
 * image2 : the second <tt>BufferedImage</tt> to be stitched
 * stitchEdge : a constant indicating which edges to join
 *
 * returns a <tt>BufferedImage</tt> that joins the source images at their edges
 */
ImageUtils.stitch = function stitch( image1, image2, stitchEdge ) {
    if( !image1 || !image2 )
        throw new Error( "null source image" );
    if( stitchEdge != ImageUtils.STITCH_HORIZONTAL && stitchEdge != ImageUtils.STITCH_VERTICAL )
        throw new Error( "invalid stitchEdge: " + stitchEdge );

    var OPAQUE = java.awt.Transparency.OPAQUE;
    var transparent = true;
    if( image1.getTransparency() == OPAQUE && image2.getTransparency() == OPAQUE )
        transparent = false;

    var im, g, w, h;
    try {
      if( stitchEdge == ImageUtils.STITCH_HORIZONTAL ) {
          w = image1.getWidth() + image2.getWidth();
          h = Math.max( image1.getHeight(), image2.getHeight() );
          im = ImageUtils.create( w, h, transparent );
      } else {
          w = Math.max( image1.getWidth(), image2.getWidth() );
          h = image1.getHeight() + image2.getHeight();
          im = ImageUtils.create( w, h, transparent );
      }

      g = im.createGraphics();
      g.drawImage( image1, 0, 0, null );

      if( stitchEdge == ImageUtils.STITCH_HORIZONTAL )
          g.drawImage( image2, image1.getWidth(), 0, null );
      else
          g.drawImage( image2, 0, image1.getHeight(), 0, null );
    } finally {
        if( g ) g.dispose();
    }
    return im;
};



/**
 * ImageUtils.resize( image, width, height, [fast] ) [static]
 * Create a copy of a <tt>BufferedImage</tt> that is resampled at a new size,
 * either smaller or larger in either dimension.
 *
 * image : the image to create a resized copy of
 * width : the width of the new image, in pixels
 * height : the height of the new image, in pixels
 * fast : an optional hint; if <tt>true</tt>, lower quality but faster ...
 *     resampling is performed (default is <tt>false</tt>)
 *
 * returns a resized copy of <tt>image</tt>
 */
ImageUtils.resize = function resize( image, width, height, fast ) {
	if( image === undefined ) throw new Error( "image undefined" );
	if( width === undefined ) throw new Error( "width undefined" );
	if( height === undefined ) throw new Error( "height undefined" );
    if( fast ) {
        return ca.cgjennings.graphics.ImageUtilities.resample(
            image, width, height, false,
            java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, null
        );
	} else {
        return ca.cgjennings.graphics.ImageUtilities.resample( image, width, height );
	}
};


/**
 * ImageUtils.fit( image, width, height, [fast] ) [static]
 * Ensures that <tt>image</tt> is as large as it can be and still fit within
 * <tt>width</tt> and <tt>height</tt> pixels, without changing the aspect
 * ratio. If the image already just fits, it is returned.
 * Otherwise a new, resized image is returned.
 *
 * image : the image to fit within the space
 * width : the maximum width of the resized image
 * height : the maximum height of the resized image
 * fast : if <tt>true</tt> a faster but lower quality algorithm is used ...
 *     (default is <tt>false</tt>)
 */
ImageUtils.fit = function fit( image, width, height, fast ) {
	if( image === undefined ) throw new Error( "image undefined" );
	if( width === undefined ) throw new Error( "width undefined" );
	if( height === undefined ) throw new Error( "height undefined" );
	if( image.width == width ) {
		if( image.height <= height ) return image;
	}
	if( image.height == height ) {
		if( image.width <= width ) return image;
	}
	var scale = Math.min( width / image.width, height / image.height );
	return ImageUtils.resize(
		image,
		Math.round( image.width * scale ),
		Math.round( image.height * scale ),
		fast
	);
};



/**
 * ImageUtils.crop( image, x, y, width, height ) [static]
 * Create an image by cropping a source image. The resulting image
 * will consist of the subimage of the source image that starts at pixel
 * (<tt>x</tt>,<tt>y</tt>) in the source image and is <tt>width</tt> by
 * <tt>height</tt> pixels in size. Either <tt>width</tt> or <tt>height</tt>
 * (or both) may be less than 1, in which case the width will be set to
 * the number of pixels from the upper-left corner of the crop to the edge.
 *
 * image : the source image, which is not changed
 * x : the x-coordinate of the upper-left corner of the region to retain in the destination
 * y : the y-coordinate of the upper-left corner of the region to retain in the destination
 * width : the width of the region to retain in the destination
 * height : the height of the region to retain in the destination
 */
ImageUtils.crop = function crop( image, x, y, width, height ) {
	if( width < 1 ) width = image.width - x;
	if( height < 1 ) height = image.height - y;
	var dest = ca.cgjennings.graphics.ImageUtilities.createCompatibleIntRGBFormat( image, width, height );
	var g = dest.createGraphics();
	try {
		g.drawImage( image, -x, -y, null );
	} finally {
		g.dispose();
	}
	return dest;
};

/**
 * ImageUtils.pad( image, top, [left, bottom, right] ) [static]
 * Returns a padded copy of the image that adds magins of the given sizes to
 * the outside of the image. The margins may be negative, in which case rows
 * or columns will be removed from the relevant edge. If only the <tt>top</tt>
 * margin size is given, then the same margin will be used on all four sides.
 *
 * image : a <tt>BufferedImage</tt> to pad
 * top : the number of pixels to add to the top edge
 * left :  the number of pixels to add to the left edge
 * bottom : the number of pixels to add to the bottom edge
 * right : the number of pixels to add to the right edge
 *
 * returns a padded copy of <tt>image</tt>
 */
ImageUtils.pad = function pad( image, top, left, bottom, right ) {
	if( image === undefined ) throw new Error( 'image undefined' );
	if( top === undefined ) throw new Error( 'margin undefined' );
	if( left === undefined ) {
		left = bottom = right = top;
	}
	return ca.cgjennings.graphics.ImageUtilities.pad( image, top, left, bottom, right );
};


/**
 * ImageUtils.tint( image, h, s, b ) [static]
 * Returns a tinted copy of an image. The value of <tt>h</tt> is a relative
 * number of degrees. The hue of each pixel in the source image will be
 * shifted by <tt>h</tt> degrees around the HSB colour wheel; a value of
 * 0 leaves the hue unchanged. The value of <tt>s</tt> is a saturation factor;
 * each pixel's saturation will be multiplied by this value. A value of 0 will
 * convert the image to greyscale. A value of 1 will leave the saturation
 * unchanged. The value of <tt>b</tt> is a brightness factor. Each pixel's
 * brightness will be multiplied by this value. A value of 0 will set each
 * pixel's brightness to 0, resulting in a black image. A value of 1 will leave
 * the brightness unchanged.
 *
 * image : the image to tint
 * h : the hue shift to apply
 * s : the saturation factor to apply
 * b : the brightness factor to apply
 *
 * returns a tinted copy of <tt>image</tt>
 */
ImageUtils.tint = function tint( image, h, s, b ) {
    if( !image ) throw new Error( "missing image" );
	if( arguments.length === 2 ) {
		let hsb = Array.from(h);
		h = hsb[0];
		s = hsb[1];
		b = hsb[2];
	} else if( arguments.length !== 4 ) {
		throw new Error( "missing (h, s, b)" );
	}

    let hsbFilter = new ca.cgjennings.graphics.filters.TintFilter( h, s, b );
	image = ca.cgjennings.graphics.ImageUtilities.ensureIntRGBFormat( image );
	return hsbFilter.filter( image, null );
};



/**
 * ImageUtils.mirror( image, [horiz], [vert] ) [static]
 * Returns a new <tt>BufferedImage</tt> that is a mirror image of <tt>image</tt>.
 *
 * image : a <tt>BufferedImage</tt> to mirror
 * horiz : if <tt>true</tt>, flip horizontally (default is <tt>true</tt>)
 * vert : if <tt>true</tt>, flip vertically (default is <tt>false</tt>)
 *
 * returns a mirrored copy of <tt>image</tt>
 */
ImageUtils.mirror = function mirror( image, horiz, vert ) {
    if( !image ) throw new Error( "missing image" );
	if( horiz === undefined ) horiz = true;
	if( vert  === undefined ) vert = false;
	return ca.cgjennings.graphics.ImageUtilities.flip( image, horiz, vert );
};



/**
 * ImageUtils.invert( image ) [static]
 * Returns a copy of the image with all of the pixels inverted.
 *
 * image : the image to invert
 *
 * returns the inverse image
 */
ImageUtils.invert = function invert( image ) {
	return ca.cgjennings.graphics.ImageUtilities.invert( image );
};



/**
 * ImageUtils.desaturate( image ) [static]
 * Returns a copy of the image converted to greyscale.
 *
 * image : the image to desaturate
 *
 * returns the desaturated image
 */
ImageUtils.desaturate = function desaturate( image ) {
	return ca.cgjennings.graphics.ImageUtilities.desaturate( image );
};


 /**
  * ImageUtils.read( file ) [static]
  * Read and return an image from a file.
  *
  * file : the name of the file to read
  *
  * returns the image, or throws an error if reading fails
  */
ImageUtils.read = function read( file ) {
    if( !file ) throw new Error( "missing file" );
    if( !(file instanceof java.io.File) ) {
        file = new java.io.File( file.toString() );
    }
	var image = null;
	image = javax.imageio.ImageIO.read( file );
    if( image == null ) throw new Error( "unable to read file" );
	return resources.ResourceKit.prepareNewImage( image );
};


/**
 * ImageUtils.write( image, file, [format], [quality], [progressive], [dpi] ) [static]
 * Write an image to a file. The value of <tt>format</tt> is a string that
 * describes the desired image format. The values "png", "jpg", or "jp2" are
 * all acceptable and use one of the three standard Strange Eons image formats
 * (PNG, JPEG, and JPEG 2000). In addition, the value "gif" will create a
 * file in GIF89a format, while "bmp" will create a Windows BMP file.
 * If <tt>format</tt> is not defined, it defaults to "png".
 *
 * image : the image to write
 * file : the file to write to; either a file name string or a <tt>File</tt> object
 * format : a string describing the format to write the image in ...
 *     (default is <tt>"png"</tt>)
 * quality : a value between 0 and 1 (inclusive) to control compression quality ...
 *     (higher is better quality, -1 for default)
 * progressive : if <tt>true</tt> requests an image that can be displayed ...
 *     progressively as it downloads (default is <tt>false</tt>)
 * ppi : if supported by the image encoder, the image's metadata will ...
 *     indicate that this is the resolution of the image (in pixels per inch)
 */
ImageUtils.write = function write( image, file, format, quality, progressive, ppi ) {
    if( !image ) throw new Error( "missing image" );
    if( !file ) throw new Error( "missing file" );
    if( format === undefined ) format = ImageUtils.FORMAT_PNG;
	if( quality === undefined ) quality = 0.75;
	if( progressive === undefined ) progressive = false;
    if( !(file instanceof java.io.File) ) {
        file = new java.io.File( file.toString() );
    }

	var iw;
	try {
		iw = new ca.cgjennings.imageio.SimpleImageWriter( format );
		if( ppi === undefined ) {
			iw.metadataEnabled = false;
		} else {
			iw.pixelsPerInch = ppi;
		}
		iw.compressionQuality = quality;
		iw.progressiveScan = progressive;
		iw.write( image, file );
	} finally {
		iw.dispose();
	}
};

/**
 * File Format Constants
 * Constants that can be passed as <tt>format</tt> values to
 * <tt>ImageUtils.write</tt>.
 *
 * ImageUtils.FORMAT_PNG : the PNG (png) image format
 * ImageUtils.FORMAT_JPEG : the JPEG (jpg) image format
 * ImageUtils.FORMAT_JPEG2000 : the JPEG2000 (jp2) image format
 * ImageUtils.FORMAT_BMP : the BMP (bmp) image format
 * ImageUtils.FORMAT_GIF : the GIF89a (gif) image format
 */
ImageUtils.FORMAT_PNG = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_PNG;
ImageUtils.FORMAT_JPEG = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_JPEG;
ImageUtils.FORMAT_JPEG2000 = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_JPEG2000;
ImageUtils.FORMAT_BMP = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_BMP;
ImageUtils.FORMAT_GIF = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_GIF;

/**
 * ImageUtils.save( image, [defaultFile], [parent] ) [static]
 *
 * Prompts the user to choose a file, then saves <tt>image</tt> to the selected
 * file. If no <tt>defaultFile</tt> is specified, then a default location
 * is selected based on the last saved file, or if no image file has been saved
 * previously, a platform-specific default location is used. The value of
 * <tt>defaultFile</tt> may either be a <tt>java.io.File</tt> object, or else
 * it can be another object whose string representation is a valid path on this
 * platform.
 *
 * image : the image to be written
 * defaultFile : an optional default location to save to
 * parent : an optional parent for the file chooser; defaults to the main ...
 *     application window
 *
 * returns the <tt>java.io.File</tt> that was saved, or <tt>null</tt> if the ...
 *     user cancelled the save operation
 */
ImageUtils.save = function save( image, defaultFile, parent ) {
	if( !image ) throw new Error( "null image" );
	if( defaultFile === undefined ) {
		defaultFile = Settings.shared.get( "default-image-folder" );
	}
	if( defaultFile != null && !(defaultFile instanceof java.io.File) ) {
		defaultFile = new java.io.File( defaultFile.toString() );
	}
	if( parent === undefined ) parent = Eons.window;
	var fc = arkham.dialog.ImageViewer.getFileChooser();
	fc.selectedFile = defaultFile;

	if( fc.showSaveDialog( parent ) != swing.JFileChooser.APPROVE_OPTION ) {
		return null;
	}

	var type = "png";
	var file = fc.selectedFile;
	var ext = arkham.project.ProjectUtilities.getFileExtension( file );
	if( !ext.isEmpty() ) {
		type = ext;
	}

	ImageUtils.write( image, file, type );

	// update default save location
	parent = file.parentFile;
	if( parent != null ) {
		Settings.user.set( "default-image-folder", parent.absolutePath );
	}

	return file;
};






/**
 * ImageUtils.view( image, [title], [modal], [parent] ) [static]
 * Display an image in an image viewer window.
 *
 * image : a <tt>BufferedImage</tt> to display
 * title : an optional title for the view window
 * modal : if <tt>true</tt>, the viewer should be modal ...
 *         (blocks the application until closed)
 * parent : an optional parent for the file chooser; defaults to the main ...
 *     application window
 */
ImageUtils.view = function view( image, title, modal, parent ) {
    if( !image || !(image instanceof java.awt.image.BufferedImage) ) {
        throw new Error( "must provide a BufferedImage argument" );
	}
    if( modal === undefined ) modal = false;
	if( parent === undefined ) parent = Eons.window;
    var d = new arkham.dialog.ImageViewer( parent, image, modal );
    if( title != null ) d.setTitle( title );
    d.setLocationByPlatform( true );
    d.setVisible( true );
};

if( useLibrary.compatibilityMode ) {
	useLibrary( "res://libraries/imageutils.ljs" );
}