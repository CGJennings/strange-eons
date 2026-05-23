package ca.cgjennings.text;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Generates random text using a Markov model. The generator must be supplied
 * with a sample text model on which to base the produced text. The text must
 * consist of at least one word, and may consist of any number of words. A word
 * is any uninterrupted sequence of non-whitespace characters. Whitespace
 * characters have special meaning to the generator; any sequence of whitespace
 * is treated as if it were a single plain space.
 * <p>
 * The generator can produce either letter sequences or word sequences. When
 * generating a letter sequence, a specific number of letters is requested by
 * the caller; for word sequences, a specific number of words is requested. When
 * producing word sequences, the system can either choose whole words (in which
 * case every word generated will actually occur in the model text), or it can
 * create a sequence of pseudo-words, which are generated one letter at a time
 * and may include "words" that do not appear in the model text.
 * <p>
 * Markov modelling produces more realistic text than simply selecting items
 * (letters or words) at random from the model text. Markov models have an
 * <i>order</i>, and the next item that is chosen depends on the previous
 * <i>order</i> items. When <i>order</i> is 0, no previous items are taken into
 * account: the next item depends only on the frequency of items in the model.
 * If A appears twice as often as B, then A is twice as likely to be chosen. If
 * <i>order</i> is 1, then 1 previous item is considered. If the previous item
 * is C, and A follows C three times as often as B follows C, then A is three
 * times more likely to be chosen after a C is chosen.
 * <p>
 * High <i>order</i>s become increasingly likely to simply reproduce long
 * passages from the source text, because the number of possible choices drops
 * rapidly as the order increases. That is, there will often be only one item
 * that follows the previous <i>order</i>
 * items, so that item will be the only choice that can be made.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class MarkovText {

    public MarkovText() {
        // this is useless for generating text but it is
        // often handy to have a no-arg constructor
        this(".");
    }

    public MarkovText(CharSequence text) {
        this(text, new Random());
    }

    public MarkovText(CharSequence text, Random rand) {
        setText(text);
        rnd = rand;
    }

    /**
     * Set the text used as a model for generating new text. The text will be
     * broken into words, where a word is any non-whitespace sequence. An
     * exception is thrown if the text does not contain at least one word.
     *
     * @param text the model text to base generated text upon
     * @throws IllegalArgumentException if the text does not contain any words
     */
    public void setText(CharSequence text) {
        // Convert all whitespace sequences to a single space, and
        // ensure that the string starts and ends with a space; a space
        // will therefore come before and after every word.
        this.text = (" " + text.toString() + " ").replaceAll("\\s+", " ").toCharArray();

        // if there are no words, the text will have collapsed to a single space;
        // otherwise there must be at least 3 characters
        if (this.text.length == 1) {
            throw new IllegalArgumentException("text contains no words");
        }

        makeSuffixArrays();
    }

    /**
     * Returns the currently set Markov order. The Markov order determines how
     * many previous words or letters are used for context when choosing the
     * next word or letter.
     *
     * @return the current Markov order
     */
    public int getOrder() {
        return order;
    }

    /**
     * Set the Markov order to use for text generation. The Markov order
     * determines how many previous words or letters are used for context when
     * choosing the next word or letter.
     *
     * @param order the Markov order to use when generating text
     * @throws IllegalArgumentException if order &lt; 0
     */
    public void setOrder(int order) {
        if (order < 0) {
            throw new IllegalArgumentException("Markov order cannot be negative: " + order);
        }
        this.order = order;
    }

    /**
     * Generate {@code charCount} letters of text.
     *
     * @param charCount the number of letters to generate.
     * @return the generated text
     * @throws IllegalArgumentException is {@code n} &lt; 0
     */
    public String generateCharacters(int charCount) {
        if (charCount < 1) {
            if (charCount == 0) {
                return "";
            }
            throw new IllegalArgumentException("Character count must be non-negative: " + charCount);
        }
        StringBuilder b = new StringBuilder(" ");

        int maxOrder = 0;
        for (int i = 0; i < charCount; ++i) {
            chooseLetter(b, maxOrder < order ? maxOrder++ : order);
            if (orderReversion) {
                maxOrder = 1;
                orderReversion = false;
            }
        }
        return b.substring(1, b.length() - 1);
    }

    /**
     * Generate {@code wordCount} words of text, one letter at a time.
     *
     * @param wordCount the number of words to generate.
     * @return the generated text
     * @throws IllegalArgumentException is {@code words} &lt; 0
     */
    public String generatePseudowords(int wordCount) {
        if (wordCount < 1) {
            if (wordCount == 0) {
                return "";
            }
            throw new IllegalArgumentException("Character count must be non-negative: " + wordCount);
        }
        StringBuilder b = new StringBuilder(" ");

        // maxOrder limits the maximum order that is possible based upon the
        // number of characters generated; character n can be no more than
        // order n+1 since that is all the context that is available;
        // we start the string with a space so that order 1 and up will
        // start on a word boundary.
        int maxOrder = 1, word = 0;
        while (word < wordCount) {
            char c = chooseLetter(b, maxOrder < order ? maxOrder++ : order);
            if (orderReversion) {
                maxOrder = 1;
                orderReversion = false;
            }
            if (c == ' ') {
                ++word;
            }
        }

        return b.substring(1, b.length() - 1);
    }

    /**
     * Generate {@code wordCount} words of text, one word at a time.
     *
     * @param wordCount the number of words to generate.
     * @return the generated text
     * @throws IllegalArgumentException is {@code words} &lt; 0
     */
    public String generateWords(int wordCount) {
        if (wordCount < 1) {
            if (wordCount == 0) {
                return "";
            }
            throw new IllegalArgumentException("Character count must be non-negative: " + wordCount);
        }
        StringBuilder b = new StringBuilder(" ");

        int maxOrder = 0;
        for (int i = 0; i < wordCount; ++i) {
            chooseWord(b, maxOrder < order ? maxOrder++ : order);
            if (orderReversion) {
                maxOrder = 1;
                orderReversion = false;
            }
        }
        return b.substring(1, b.length() - 1);
    }

    /**
     * Append a random word to {@code b}, using the last {@code order} words in
     * {@code b} as context.
     */
    private void chooseWord(StringBuilder b, int order) {
        if (order == 0) {
            int s = wordSuffixes[rnd.nextInt(wordSuffixes.length)];
            appendWordAt(s, b);
            return;
        }

        // scan backwards from the end of the generated text until we find
        // order words; this is the context we will consider when picking the
        // next word
        int pos = b.length() - 1;
        for (int words = -1; pos > 0; --pos) {
            if (b.charAt(pos) == ' ') {
                ++words;
                if (words == order) {
                    break;
                }
            }
        }
        ++pos; // skip the initial space at b[pos]

        // find the first and last suffix array entry that matches the context,
        // then randomly choose one of those entries and pick the word that
        // follows the context (in the selected entry)
        int charsToMatch = b.length() - pos;
        int first = findFirstPrefix(wordSuffixes, b, charsToMatch);

        if (first >= 0) {
            int last = first + 1;
            while (last < wordSuffixes.length && prefixCompare(wordSuffixes, b, charsToMatch, last) == 0) {
                ++last;
            }
            int choice = wordSuffixes[first + rnd.nextInt(last - first)] + charsToMatch;
            if (choice < text.length) {
                appendWordAt(choice, b);
                return;
            }
        }

        // if no matching suffix was found, the context must include the end of
        // the model text; we will reset the generator as if it was just starting
        // out so that text generation can continue
        chooseWord(b, 0);
        orderReversion = true;
    }

    /**
     * Append the word starting at {@code text[ startIndex ]} to {@code b},
     * including the subsequent space.
     */
    private void appendWordAt(int startIndex, StringBuilder b) {
        do {
            b.append(text[startIndex]);
        } while (text[startIndex++] != ' ');
    }

    /**
     * Append a random letter to {@code b}, using the last {@code order} letters
     * in {@code b} as context. The selected letter is also returned so that it
     * can be used by the caller to select a stopping point (for example, after
     * space has been selected a certain number of times).
     */
    private char chooseLetter(StringBuilder b, int order) {
        char c;
        orderReversion = true;

        // this loop prevents generating a space at the start
        // or two spaces in a row even on order 0
        do {
            c = text[letterSuffixes[rnd.nextInt(letterSuffixes.length)]];
            if (order > 0) {
                // find the first and last suffixArray that start with the
                // last order letters in b
                int first = findFirstPrefix(letterSuffixes, b, order);
                if (first >= 0) {
                    int last = first + 1;
                    while (last < letterSuffixes.length && prefixCompare(letterSuffixes, b, order, last) == 0) {
                        ++last;
                    }

                    // choose one of these suffixes at random, and select the letter
                    // that follows the last order letters in b in the suffixArray
                    int choice = letterSuffixes[first + rnd.nextInt(last - first)] + order;
                    if (choice < text.length) {
                        c = text[choice];
                        orderReversion = false;
                    }
                }
            }
        } while (c == ' ' && (b.length() == 0 || b.charAt(b.length() - 1) == ' '));
        b.append(c);
        return c;
    }
    /**
     * This flag indicates that chooseLetter or chooseWord previously picked the
     * last item in the model text. As a result, the generator now has no choice
     * it could make for the next item. Instead, an order 0 item was selected
     * and appended. This flag indicates to the caller that it should reset its
     * own state accordingly.
     */
    private boolean orderReversion = false;

    /**
     * Find the first entry in {@code suffixArray} that starts with the last
     * {@code charsToMatch} characters in {@code b}. If no entry matches,
     * returns -1.
     */
    private int findFirstPrefix(Integer[] suffixArray, StringBuilder b, int charsToMatch) {
        // a binary search that finds the first matching entry
        int l = -1, h = suffixArray.length;
        while (l + 1 != h) {
            int m = (l + h) / 2;
            if (prefixCompare(suffixArray, b, charsToMatch, m) > 0) {
                l = m;
            } else {
                h = m;
            }
        }

        if (h >= suffixArray.length || prefixCompare(suffixArray, b, charsToMatch, h) != 0) {
            h = -1;
        }
        return h;
    }

    /**
     * Compare the last {@code charsToMatch} characters in {@code b} to the
     * first {@code charsToMatch} characters at {@code suffixArray[suff]}.
     */
    private int prefixCompare(Integer[] suffixArray, StringBuilder b, int charsToMatch, int suff) {
        int s = suffixArray[suff];

        for (int i = 0; i < charsToMatch; ++i, ++s) {
            if (s == text.length) {
                return 1;
            }
            char lhs = b.charAt(b.length() - charsToMatch + i);
            char rhs = text[s];
            if (lhs != rhs) {
                return lhs - rhs;
            }
        }
        return 0;
    }

    private void makeSuffixArrays() {
        letterSuffixes = new Integer[text.length];
        for (int i = 0; i < letterSuffixes.length; ++i) {
            letterSuffixes[i] = i;
        }
        Arrays.sort(letterSuffixes, suffixSorter);

        int words = 0;
        for (int i = 1; i < text.length; ++i) {
            if (text[i] == ' ') {
                ++words;
            }
        }
        wordSuffixes = new Integer[words];
        for (int word = 0, i = 0; i < text.length - 1; ++i) {
            if (text[i] == ' ') {
                wordSuffixes[word++] = i + 1;
            }
        }
        Arrays.sort(wordSuffixes, suffixSorter);
    }

    private int suffixCompare(int lhs, int rhs) {
        if (lhs == rhs) {
            return 0;
        }
        int len = text.length;
        while (lhs < len && rhs < len) {
            if (text[lhs++] != text[rhs++]) {
                return text[--lhs] - text[--rhs];
            }
        }
        return lhs == len ? -1 : 1;
    }

    /**
     * The model text to use, with each word separated by exactly one space.
     */
    private char[] text;
    /**
     * A sorted suffix array; elements are indices into text[].
     */
    private Integer[] letterSuffixes;
    /**
     * A sorted suffix array with indices only at word boundaries.
     */
    private Integer[] wordSuffixes;
    /**
     * Random number generator to use when generating text.
     */
    private Random rnd;
    /**
     * Markov order at which to generate text.
     */
    private int order = 2;
    /**
     * A comparator for sorting this instance's suffixArray arrays.
     */
    private Comparator<Integer> suffixSorter = this::suffixCompare;
}
