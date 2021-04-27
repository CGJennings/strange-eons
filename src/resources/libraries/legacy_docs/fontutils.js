/*

  fontutils.js - version 7
  Support for finding and registering fonts.


The SE JavaScript Library Copyright © 2008-2012
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
 * Utility functions for working with font resources
 *
 * <b>Note:</b> A development version of this library defined
 * utility functions in the global namespace. These functions are
 * still defined by this library, but they have been deprecated.
 * Use the versions in <tt>FontUtils</tt> instead.
 */

const FontUtils = {};

/**
 * FontUtils.findMatchingFamily( families, [defaultFamily] ) [static]
 * Returns a font family name that is available on this system.
 * The method for selecting a font is similar to that used by CSS and HTML:
 * The string <tt>families</tt> is a list of candidate font family names
 * separated by commas, and this function returns the first available entry.
 * For example, <tt>FontUtils.findMatchingFamily( "Bookman Old
 * Style,Times" )</tt> would return <tt>Bookman Old Style</tt> if that family
 * was available. If not then it would return <tt>Times</tt> if that family was available.
 * If none of the typefaces in <tt>families</tt> is available, then <tt>defaultFamily</tt>
 * is returned (which may be <tt>null</tt>).
 *
 * families : a string listing candidate families
 * defaultFamily : an optional default to use if no family matches ...
 *     (may be <tt>null</tt>; if not specified will use the default body family)
 *
 * Returns the first available font family, or the default value.
 */
FontUtils.findMatchingFamily = function findMatchingFamily( families, defaultFamily ) {
    if( defaultFamily === undefined ) {
        defaultFamily = ResourceKit.getBodyFamily();
	}
    return ResourceKit.findAvailableFontFamily( families, defaultFamily );
};

/**
 * FontUtils.availableFontFamilies() [static]
 * Returns a list of the available font families on this system.
 * This will include fonts that have been successfully registered with
 * <tt>FontUtils.registerFontFamily</tt> or
 * <tt>FontUtils.registerFontFamilyFromResources</tt>.
 */
FontUtils.availableFontFamilies = function availableFontFamilies() {
   var families = Array.from(
       java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
       .getAvailableFontFamilyNames( Language.getInterfaceLocale() )
   );
   return families;
};

/**
 * FontUtils.registerFontFamily( key ) [static]
 * Create a typeface family by creating fonts from font files. The files
 * are found by looking up the setting <tt>key</tt> using the same
 * <tt>Settings</tt> provider as is used for <tt>$</tt> variables.
 * The value of this key must be a comma-separated list of font file
 * resources.
 *
 * Entries after the first will use the same resource path as the first
 * entry if they do not include a '/' character. For example, the following
 * list would load a family of two fonts located in the <tt>foo/fonts</tt>
 * resource folder: <tt>foot/fonts/bar_regular.ttf, bar_italic.ttf</tt>.
 *
 * Supported font formats include TrueType (<tt>.ttf</tt>), OpenType (<tt>.otf</tt>),
 * and PostScript Type 1 fonts.
 * The file extension used for the resource file name must be correct
 * for the format of the font resource, or font creation will fail.
 *
 * key : the setting key containing the list of font files
 *
 * returns the font family name of the registered family
 */
FontUtils.registerFontFamily = function registerFontFamily( key ) {
    return useLibrary.__$notation.globalSettingsProvider.registerFontFamily( key );
};

/**
 * FontUtils.registerFontFamilyFromResources( [resourceFile...] ) [static]
 * Create a typeface family directly from a list of resource files instead of
 * reading a list from a key. Instead of passing in a list of separate file
 * arguments, you can also pass in a string containing a comma-separated
 * list of resources as if it were the value of a font family key.
 *
 * resourceFile : one or more arguments denoting location of the font files ...
 *    that make up the family
 *
 * returns the font family name of the registered family
 */
FontUtils.registerFontFamilyFromResources = function registerFontFamilyFromResources() {
    if( arguments.length < 1 ) {
        Error.error( "at least one resource file name required" );
    }
    var b = new java.lang.StringBuilder();
    for( var i=0; i<arguments.length; ++i ) {
        if( i>0 ) b.append( "," );
        b.append( arguments[i] );
    }
	return ResourceKit.registerFontFamily( b.toString() )[0].family;
};


if( useLibrary.compatibilityMode ) {
	useLibrary( "res://libraries/fontutils.ljs" );
}