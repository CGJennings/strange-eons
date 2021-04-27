/*

  random.js - version 4
  Pseudorandom number generation and support tools.


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
 * Random selection and number generation
 */

/**
 * random
 * A shared global instance of <tt>Random</tt>.
 */
const random = new Random();



/**
 * Random( seed ) [ctor]
 * A constructor to create a new <tt>Random</tt> object.
 * If a <tt>seed</tt> is given, the random number generator will use it as an
 * initial seed value. This makes the number sequence it generates reproducable
 * for testing purposes.
 *
 * seed : optional seed value
 */
function Random( seed ) {
    if( !seed )
        this.generator = new java.util.Random();
    else
        this.generator = new java.util.Random( seed );
}



/**
 * Random.prototype.number()
 * Returns a random number between 0 (inclusive) and 1 (exclusive).
 * Effectively the same as <tt>Math.random()</tt>, but uses the random number
 * generator of this <tt>Random</tt> object.
 */
Random.prototype.number = function number() {
    return this.generator.nextDouble();
};



/**
 * Random.prototype.pick( m, n )
 * Returns a random integer between <tt>m</tt> (inclusive) and <tt>n</tt>
 * (inclusive).
 *
 * m : one end of the range
 * n : the other end of the range
 */
Random.prototype.pick = function pick( m, n ) {
    if( m > n ) {
        var t = m;
        m = n;
        n = t;
    }
    return this.generator.nextInt( n-m+1 ) + m;
};



/**
 * Random.prototype.pickOtherThan( m, n, excluded )
 * Returns a random integer between <tt>m</tt> (inclusive) and <tt>n</tt>
 * (inclusive), but excluding the value of <tt>excluded</tt>.
 *
 * m : one end of the range
 * n : the other end of the range
 * excluded : a value that will never be returned
 */
Random.prototype.pickOtherThan = function pickOtherThan( excluded, m, n ) {
    if( excluded == m && m == n ) {
        throw new Error( "no choice is possible" );
    }

    if( m > n ) {
        var t = m;
        m = n;
        n = t;
    }

    if( excluded < m || excluded > n ) {
        return this.generator.nextInt( n-m+1 ) + m;
    }

    var choice = this.generator.nextInt( n-m ) + m;
    if( choice >= excluded )
        ++choice;

    return choice;
};



/**
 * Random.prototype.d6()
 * Returns the result of rolling a simulated 6-sided die.
 * A convenience for <tt>Random.pick( 1, 6 )</tt>.
 */
Random.prototype.d6 = function d6() {
    return this.generator.nextInt( 6 ) + 1;
};

/**
 * Random.prototype.rollDice( [n], [showDice] )
 * Simulates rolling <tt>n</tt> dice. If <tt>showDice</tt> is <tt>true</tt>,
 * an image of each die result is printed to the console.
 * The values rolled are returned in an array.
 *
 * n : the optional number of dice to roll (default is 1, must be non-negative)
 * showDice : if <tt>true</tt>, the results are printed to the console graphically (default is <tt>true</tt>)
 *
 * returns an array containing the <tt>n</tt> numbers that were rolled
 */
Random.prototype.rollDice = function rollDice( n, showDice ) {
    if( n === undefined ) n = 1;
    if( n < 0 ) throw new Error( "cannot roll " + n + " dice" );
    if( showDice === undefined ) showDice = true;

    var rolled = new Array(n);
    for( var i=0; i<n; ++i ) {
        rolled[i] = this.d6();
        if( showDice ) {
            if( i > 0 ) print( " " );
            Console.printImage( resources.ResourceKit.getIcon( "d" + rolled[i] + ".png" ) );
        }
    }
    return rolled;
};


/**
 * Array.prototype.pick()
 * Randomly returns one element of the array.
 * The random selection is made using <tt>random</tt>.
 * An extension to all <tt>Array</tt> objects.
 */
Array.prototype.pick = function pick() {
    if( this.length == 0 ) {
        throw new Error( "no choice is possible" );
    }
    return this[ random.pick( 0, this.length-1 ) ];
};



/**
 * Array.prototype.pickOtherThan( excluded )
 * Randomly returns one element of the array other than any element equal
 * to <tt>excluded</tt>.
 * An extension to all <tt>Array</tt> objects.
 *
 * excluded : a value that will never be returned
 */
Array.prototype.pickOtherThan = function pickOtherThan( excluded ) {
    var choice = false;
    for( var i=0; i < this.length; ++i ) {
        if( this[i] != choice ) {
            choice = true;
            break;
        }
    }

    if( !choice ) {
        throw new Error( "no choice is possible" );
    }

    var selected;
    do {
        selected = this.pick();
    } while( selected == excluded );

    return selected;
};

/**
 * Array.prototype.shuffle()
 * Shuffles the contents of an array into random order.
 * An extension to all <tt>Array</tt> objects.
 */
Array.prototype.shuffle = function shuffle() {
    for( var i=0; i<this.length; ++i ) {
        var j = random.generator.nextInt( this.length );
        var temp = this[i];
        this[i] = this[j];
        this[j] = temp;
    }
};
