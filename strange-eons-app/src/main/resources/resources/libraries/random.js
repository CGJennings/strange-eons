/*
 random.js - version 5
 Pseudorandom number generation and support tools.
 */

const random = new Random();

function Random(seed) {
    if (!seed)
        this.generator = new java.util.Random();
    else
        this.generator = new java.util.Random(seed);
}

Random.prototype.number = function number() {
    return this.generator.nextDouble();
};

Random.prototype.pick = function pick(m, n) {
    if (m > n) {
        var t = m;
        m = n;
        n = t;
    }
    return this.generator.nextInt(n - m + 1) + m;
};

Random.prototype.pickOtherThan = function pickOtherThan(excluded, m, n) {
    if (excluded == m && m == n) {
        throw new Error("no choice is possible");
    }

    if (m > n) {
        var t = m;
        m = n;
        n = t;
    }

    if (excluded < m || excluded > n) {
        return this.generator.nextInt(n - m + 1) + m;
    }

    var choice = this.generator.nextInt(n - m) + m;
    if (choice >= excluded)
        ++choice;

    return choice;
};

Random.prototype.d6 = function d6() {
    return this.generator.nextInt(6) + 1;
};

Random.prototype.rollDice = function rollDice(n, showDice) {
    if (n === undefined)
        n = 1;
    if (n < 0)
        throw new Error("cannot roll " + n + " dice");
    if (showDice === undefined)
        showDice = true;

    var rolled = new Array(n);
    for (var i = 0; i < n; ++i) {
        rolled[i] = this.d6();
        if (showDice) {
            if (i > 0)
                print(" ");
            Console.printImage(resources.ResourceKit.getIcon("d" + rolled[i] + ".png"));
        }
    }
    return rolled;
};

Array.prototype.pick = function pick() {
    if (this.length == 0) {
        throw new Error("no choice is possible");
    }
    return this[ random.pick(0, this.length - 1) ];
};

Array.prototype.pickOtherThan = function pickOtherThan(excluded) {
    var choice = false;
    for (var i = 0; i < this.length; ++i) {
        if (this[i] != choice) {
            choice = true;
            break;
        }
    }

    if (!choice) {
        throw new Error("no choice is possible");
    }

    var selected;
    do {
        selected = this.pick();
    } while (selected == excluded);

    return selected;
};

Array.prototype.shuffle = function shuffle() {
    for (var i = 0; i < this.length; ++i) {
        var j = random.generator.nextInt(this.length);
        var temp = this[i];
        this[i] = this[j];
        this[j] = temp;
    }
};