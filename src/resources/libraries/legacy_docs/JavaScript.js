/*

  javascript.doc - version 4
  This file documents functions in the common library that extend core
  JavaScript objects. This separates the documentation along logical
  boundaries and keeps the size of common.js down.


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
 * Language Extensions
 * This document describes extensions to the standard JavaScript language.
 * These are defined in the <tt>common</tt> library, but grouped together
 * here for convenience. There is no need to import this file.
 */

/**
 * Array.from( arrayLikeObject ) [static]
 * Create a JavaScript <tt>Array</tt> object from any "array-like" object,
 * including a Java array. An array-like object <tt>a</tt> includes a length property
 * and a sequence of zero or more sequential properties <tt>a[0]</tt>,
 * <tt>a[1]</tt>, <tt>a[2]</tt>, ... <tt>a[ a.length-1 ]</tt>.
 *
 * returns an <tt>Array</tt> containing a copy of the elements of <tt>arrayLikeObject</tt>
 */

/**
 * Function.prototype.subclass( superConstructor )
 *
 * Makes objects created with the calling constructor a "subclass" of
 * the <tt>superConstructor</tt> by chaining
 * <tt>constructor.prototype</tt> to <tt>superConstructor.prototype</tt>.
 *
 * <b>Example:</b>
 * <pre>
 * function Super() {
 *     this.x = "super";
 * }
 *
 * Super.prototype.getX = function getX() {
 *     return this.x;
 * }
 *
 * function Sub() {
 *     Super.call( this );
 *     this.x = "sub";
 * }
 *
 * Sub.subclass( Super );
 *
 * var subobj = new Sub();
 * // this will use the getX function defined for Super
 * // (i.e. Super.prototype.getX)
 * println( subobj.getX() );
 * </pre>
 *
 * constructor : the name of the subclass constructor function
 * superConstructor : the name of the superclass constructor function
 */

/**
 * Function.abstractMethod() [static]
 * When called, prints a standard warning message if script warnings are enabled.
 * This function is not meant to be called directly. Rather, this function is
 * assigned to a property in an object's prototype to make that function
 * abstract. When a subclassing object fails to override the abstract method,
 * calls to the abstract method will thus display a warning message for the
 * developer.
 */

/**
 * Number.prototype.toInt()
 * Converts this number to a Java <tt>Integer</tt> object.
 * Any fractional part is discarded. If the number lies outside the
 * range of possible <tt>Integer</tt> values, an exception is thrown.
 *
 * returns this number as a <tt>java.lang.Integer</tt>
 */

/**
 * Number.prototype.toLong()
 * Converts this number to a Java <tt>Long</tt> object.
 * Any fractional part is discarded. If the number lies outside the
 * range of possible <tt>Long</tt> values, an exception is thrown.
 *
 * returns this number as a <tt>java.lang.Long</tt>
 */

/**
 * Number.prototype.toFloat()
 * Converts this number to a Java <tt>Float</tt> object.
 * If the number lies outside the range of possible <tt>Float</tt> values,
 * an exception is thrown.
 *
 * returns this number as a <tt>java.lang.Float</tt>
 */

/**
 * String.prototype.trim()
 * Removes leading and trailing whitespace from this string.
 *
 * returns this string with whitespace removed from both ends
 */

/**
 * String.prototype.trimLeft()
 * Removes leading whitespace from this string.
 *
 * returns this string with whitespace removed from the start
 */

/**
 * String.prototype.trimRight()
 * Removes trailing whitespace from this string.
 *
 * returns this string with whitespace removed from the end
 */

/**
 * String.prototype.replaceAll( pattern, replacement )
 * Replaces every occurrence of <tt>pattern</tt> with <tt>replacement</tt>,
 * where both values are plain strings rather than regular expressions.
 * The replacements are made as if by processing the string from left
 * to right and replacing each time a match is possible. For example,
 * the following code results in <tt>bb</tt> rather than <tt>bbb</tt>:
 * <pre>
 * println( "aaaa".replaceAll( "aa", "b" ) );
 * </pre>
 *
 * pattern : the substring to replace all occurrences of
 * replacement : the substring to replace each occurrence with
 *
 * returns the replaced string
 */

/**
 * String.prototype.startsWith( pattern )
 * Returns <tt>true</tt> if <tt>pattern</tt> is a prefix of this string.
 * <pre>
 * "meta".startsWith( "me" ); // true
 * "Random".startsWith( "and" ); // false
 * </pre>
 *
 * pattern : the string to match against the start of this string
 *
 * returns <tt>true</tt> if and only if this string starts with <tt>pattern</tt>
 */

/**
 * String.prototype.endsWith( pattern )
 * Returns <tt>true</tt> if <tt>pattern</tt> is a suffix of this string.
 * <pre>
 * "bartend".endsWith( "tend" ); // true
 * "moose".endsWith( "oos" ); // false
 * </pre>
 *
 * pattern : the string to match against the end of this string
 *
 * returns <tt>true</tt> if and only if this string ends with <tt>pattern</tt>
 */

/**
 * Object.prototype.dontEnum( methodNames... )
 * Prevents the named methods from being enumerated. This is most useful when
 * extending <tt>Object</tt>'s prototype with new functions, as it maintains
 * the ability to use plain objects as a map.
 * <pre>
 * var obj = {};
 * Object.prototype.blight = function blight() {};
 * println( "Before:" );
 * for( let i in obj ) println(i);
 * Object.prototype.dontEnum( "blight" );
 * println( "After:" );
 * for( let i in obj ) println(i);
 * </pre>
 */

/**
 * RegExp.quote( string ) [static]
 * Escapes the characters in a string that have special meaning in a regular
 * expression. If a regular expression is created from the string, it will
 * match <tt>string</tt> literally. For example, <tt>*</tt> would be replaced
 * with <tt>\\*</tt> because <tt>*</tt> has special meaning in a regular
 * expression.
 *
 * string : the string to escape
 *
 * returns a string that encodes <tt>string</tt> as regular expression
 */

/**
 * RegExp.quoteReplacement( string ) [static]
 * Escapes the characters in a string that have special meaning when used as
 * a replacement for a regular expression.
 *
 * string : the string to escape
 * returns a string that encodes <tt>string</tt> as regular expression replacement
 */