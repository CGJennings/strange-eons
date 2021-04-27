/*
  cards.js - version 8
  Virtual card decks.
*/

useLibrary( 'random' );

function CardDeck() {
    this.cards = [];
}

CardDeck.prototype.toString = function toString() {
    var s = "";
    for( var i=0; i<this.cards.length; ++i ) {
        if( i > 0 ) s += ", ";
        s += this.cards[i];
    }
    return s;
};

CardDeck.createFromArray = function createFromArray( arrayLikeObject ) {
    var deck = new CardDeck();
    deck.cards = Array.from( arrayLikeObject );
    return deck;
};

CardDeck.prototype.shuffle = function shuffle() {
    this.cards.shuffle();
};

CardDeck.prototype.draw = function draw() {
    return this.cards.length === 0 ? null : this.cards.shift();
};

CardDeck.prototype.drawFirst = function drawFirst( matchFunction ) {
    if( matchFunction === undefined ) Error.error( "missing matchFunction" );
    for( var i=0; i<this.cards.length; ++i ) {
        if( matchFunction( this.cards[i] ) ) {
            return this.pull( i );
        }
    }
    return null;
};

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

CardDeck.prototype.size = function size() {
    return this.cards.length;
};