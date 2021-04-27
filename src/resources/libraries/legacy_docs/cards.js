/*

  cards.js - version 8
  Virtual card decks.


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
 * Virtual card decks
 * Adds the <tt>CardDeck</tt> class, a stack-like collection of objects that
 * supports operations typical of a deck of cards, such as shuffling, drawing from,
 * and discarding to.
 */

useLibrary( 'random' );

/**
 * CardDeck() [ctor]
 * Creates a new, empty deck of cards.
 */
function CardDeck() {
    this.cards = [];
}

/**
 * CardDeck.prototype.toString()
 * Returns a string consisting of a comma separated list of the cards in
 * this card deck.
 *
 * returns a string listing the contents of this <tt>CardDeck</tt>
 */
CardDeck.prototype.toString = function toString() {
    var s = "";
    for( var i=0; i<this.cards.length; ++i ) {
        if( i > 0 ) s += ", ";
        s += this.cards[i];
    }
    return s;
};

/**
 * CardDeck.createFromArray( arrayLikeObject ) [static]
 * Creates a new deck, using items from an array to populate it.
 *
 * arrayLikeObject : an array or array-like object to copy from
 *
 * returns a card deck consisting of objects copied from <tt>arrayLikeObject</tt>
 */
CardDeck.createFromArray = function createFromArray( arrayLikeObject ) {
    var deck = new CardDeck();
    deck.cards = Array.from( arrayLikeObject );
    return deck;
};



/**
 * CardDeck.prototype.shuffle()
 * Shuffles the cards in a deck into a random order.
 */
CardDeck.prototype.shuffle = function shuffle() {
    this.cards.shuffle();
};




/**
 * CardDeck.prototype.draw()
 * Draws the top card off the deck. This removes the card from the deck;
 * to return the card, use <tt>CardDeck.discard</tt>. Drawing from an
 * empty deck throws an exception.
 *
 * removes and returns the top card from the deck
 */
CardDeck.prototype.draw = function draw() {
    if( this.cards.length == 0 )
        Error.error( "cannot draw from empty deck" );
    return this.cards.shift();
};


/**
 * CardDeck.prototype.drawFirst( matchFunction )
 * Searches the deck for the first <tt>card</tt> for which <tt>matchFunction(card)</tt>
 * returns <tt>true</tt>. If a matching card is found, removes it from the deck and
 * returns it. If there is no matching card, returns <tt>null</tt>.
 *
 * matchFunction : a function that takes a card and returns <tt>true</tt> if ...
 *     it is the type of card you wish to draw
 *
 * returns the matching card (which is removed from the deck), or <tt>null</tt>
 */
CardDeck.prototype.drawFirst = function drawFirst( matchFunction ) {
    if( matchFunction === undefined ) Error.error( "missing matchFunction" );
    for( var i=0; i<this.cards.length; ++i ) {
        if( matchFunction( this.cards[i] ) ) {
            return this.pull( i );
        }
    }
    return null;
};

/**
 * CardDeck.prototype.discard( card, [copies] )
 * Adds <tt>card</tt> to the bottom of the deck.
 * It is not required that <tt>card</tt> have previously been drawn from
 * the deck. If <tt>copies</tt> is defined, then
 * that many copies are added, all identical.
 * The default value is 1.
 *
 * card : the card to add to the deck
 * copies : the optional number of copies to add to the deck
 *
 * places card(s) on the bottom of the deck
 */
CardDeck.prototype.discard = function discard( card, copies ) {
    if( card === undefined )
        Error.error( "missing card" );
    if( copies === undefined ) copies = 1;
    if( copies < 0 )
        Error.error( "number of copies cannot be negative: " + copies );
    for( var i=0; i<copies; ++i ) {
        this.cards.push( card.toString() );
    }
};



/**
 * CardDeck.prototype.peek( [index] )
 * Returns a card from the deck without removing it. If <tt>index</tt>
 * is not defined, then the card at the top of the deck is returned.
 * Otherwise, if <tt>index</tt> is 0 or positive, then the card at position
 * <tt>index</tt> is returned, where a position of 0 is the top of the deck,
 * 1 is the next card from the top, and so on. If <tt>index</tt> is negative,
 * then the cards are counted from the bottom of the deck: -1 is the last card,
 * -2 is the penultimate card, and so on.
 *
 * index : the position of the card to peek at within the deck
 *
 * returns the card at <tt>index</tt>
 */
CardDeck.prototype.peek = function peek( index ) {
    if( index === undefined ) index = 0;
    if( index >= 0 ) {
        if( index >= this.cards.length )
            Error.error( "bad index: " + index );
    } else {
        var invertedIndex = this.cards.length + index;
        if( invertedIndex < 0 )
            Error.error( "bad index: " + index );
        index = invertedIndex;
    }
    return this.cards[ index ];
};



/**
 * CardDeck.prototype.pull( [index] )
 * Pulls a card from the deck from any position. The card at the requested position
 * is removed from the deck and returned. As with <tt>CardDeck.peek()</tt>,
 * a negative index may be used to count from the bottom of the deck.
 * If called without an <tt>index</tt> argument, this method is equivalent to
 * <tt>CardDeck.draw()</tt>.
 *
 * index : the optional position of the card to remove from the deck
 *
 * returns and removes the card at <tt>index</tt>
 */
CardDeck.prototype.pull = function pull( index ) {
    if( index === undefined ) index = 0;
    if( index >= 0 ) {
        if( index >= this.cards.length )
            Error.error( "bad index: " + index );
    } else {
        var invertedIndex = this.cards.length + index;
        if( invertedIndex < 0 )
            Error.error( "bad index: " + index );
        index = invertedIndex;
    }
    return this.cards.splice( index, 1 )[0];
};



/**
 * CardDeck.prototype.size()
 * Returns the number of cards currently in this deck.
 */
CardDeck.prototype.size = function size() {
    return this.cards.length;
};