package ca.cgjennings.apps.arkham.component.design;

import ca.cgjennings.algo.Diff;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A consequence set formatter formats an ordered list of <i>possible</i>
 * verbal design consequences. In this context, a design consequence refers to
 * the possibly unintended and often hard-to-predict side effects of a design
 * decision that result from trying to balance competing concerns in an
 * ill-structured problem space. The consequence set formatter helps the user to
 * visualize how these consequences change over time as design proceeds by
 * comparing the consequences of the previous design iteration to the current
 * design.
 *
 * <p>
 * During a design support analysis, the design analyzer goes through each of
 * the possible consequences, <i>in order</i>, and indicates whether it applies
 * to the current design. The consequence set formatter will compare how the
 * consequences have changed between the current design and the previous design,
 * and use different formatting to provide visual feedback of which consequences
 * are the same, which no longer apply, and which are new.
 *
 * <p>
 * Consequences may be divided into categories, with a separate formatter for
 * each category. Because all formatting is written to a
 * {@code StringBuilder} provided by the caller, the results from each
 * category can easily be collected into a single formatted document.
 *
 * <p>
 * If for some reason you cannot process each possible consequence in the same
 * order for each analysis, this class will not produce reliable results.
 * However, you could build your own formatter using the {@link Diff} algorithm.
 *
 * <p>
 * Typically, the output produced by this class is a formatted HTML list, but it
 * can be customized to produce other formats, including plain text.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 0.9
 */
public class ConsequenceSet implements Iterable<String> {

    private LinkedHashSet<String> set = new LinkedHashSet<>();

    /**
     * Create a consequence set with an empty category name.
     */
    public ConsequenceSet() {
        this(null);
        setListMarkup("<ol>", "</ol>");
        setDeletedEntryMarkup(null, null);
        setKeptEntryMarkup("<li>", "</li>");
        setNewEntryMarkup("<li><font color='1000ff'><b>", "</b></font></li>");
    }

    /**
     * Create a consequence set with a specific category name.
     *
     * @param category the category title
     */
    public ConsequenceSet(String category) {
        setCategory(category);
        setListMarkup("<ol>", "</ol>");
        setDeletedEntryMarkup(null, null);
        setKeptEntryMarkup("<li>", "</li>");
        setNewEntryMarkup("<li><font color='1000ff'><b>", "</b></font></li>");
    }

    /**
     * Create a consequence set with a specific category name and installs a
     * default style for the given colour.
     *
     * @param category the category title
     * @param color the colour to use for the default style
     */
    public ConsequenceSet(String category, Color color) {
        setCategory(category);
        installDefaultStyle(color);
    }

    /**
     * Adds the next consequence to the set. If a string equal to this
     * consequence is already in the set, the set will not be modified.
     *
     * @param consequence the consequence to add
     * @throws NullPointerException if the consequence text is {@code null}
     */
    public void add(String consequence) {
        if (consequence == null) {
            throw new NullPointerException("consequence");
        }
        set.add(consequence);
    }

    /**
     * Clears the current consequence list. This can be called to reuse an
     * existing consequence set.
     */
    public void reset() {
        set.clear();
    }

    /**
     * Returns the number of consequences that have been added to the set.
     *
     * @return the number of consequences in the set
     */
    public int size() {
        return set.size();
    }

    /**
     * Returns an iterator over the consequences in the set.
     *
     * @return an iterator that can be used to examine the items in the set
     */
    @Override
    public Iterator<String> iterator() {
        return set.iterator();
    }

    /**
     * Returns the category name for this formatter. This describes the general
     * category of consequences included in the set, if more than one category
     * is being used.
     *
     * @return the category name for the formatter, or {@code null}
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category name for this formatter. The category name, if any, is
     * typically printed at the top of the consequence list. It may be
     * {@code null} to indicate that categories are not being used.
     *
     * @param category the name of the category that the consequence set is
     * associated with
     */
    public void setCategory(String category) {
        this.category = category;
    }

    private String category;

    /**
     * Sets all of the markup for the list to a set of suitable defaults for
     * writing HTML documents. The list text will use the specified colour, or
     * black if color is {@code null}.
     *
     * @param color the colour to use for the list entries
     */
    public void installDefaultStyle(Color color) {
        int rgb = 0;
        if (color != null) {
            rgb = color.getRGB() & 0xff_ffff;
        }
        String hex = String.format("%06x", rgb);
        setDeletedEntryMarkup("<li value='@n;'><font color='#777777'><s>", "</s></font></li>");
        setKeptEntryMarkup("<li value='@n;'><font color='#" + hex + "'>", "</font></li>");
        setNewEntryMarkup("<li value='@n;'><font color='#" + hex + "'><b>", "</b></font></li>");

        setListMarkup("<ol>", "</ol>");
        setEntrySeparatorMarkup(null);
    }

    /**
     * Sets the markup to write before and after the text of a deleted entry.
     * Typically this will include a {@code &lt;li&gt;} tag pair and some
     * style instructions.
     *
     * @param prologue the markup to write before the entry, or
     * {@code null} to suppress the entry
     * @param epilogue the markup to write after the entry
     */
    public void setDeletedEntryMarkup(String prologue, String epilogue) {
        itemDecorators[DELETED][0] = prologue;
        itemDecorators[DELETED][1] = epilogue;
    }

    /**
     * Sets the markup to write before and after the text of an entry that does
     * not change from the previous consequence set to this one. Typically this
     * will include a {@code &lt;li&gt;} tag pair and some style
     * instructions.
     *
     * @param prologue the markup to write before the entry, or
     * {@code null} to suppress the entry
     * @param epilogue the markup to write after the entry
     */
    public void setKeptEntryMarkup(String prologue, String epilogue) {
        itemDecorators[KEPT][0] = prologue;
        itemDecorators[KEPT][1] = epilogue;
    }

    /**
     * Sets the markup to write before and after the text of a new entry that
     * was not present in the previous consequence set. Typically this will
     * include a {@code &lt;li&gt;} tag pair and some style instructions.
     *
     * @param prologue the markup to write before the entry, or
     * {@code null} to suppress the entry
     * @param epilogue the markup to write after the entry
     */
    public void setNewEntryMarkup(String prologue, String epilogue) {
        itemDecorators[INSERTED][0] = prologue;
        itemDecorators[INSERTED][1] = epilogue;
    }

    /**
     * Sets the markup written to start and the list.
     *
     * @param prologue the markup to start a list, e.g., {@code &lt;ol&gt;}
     * @param epilogue the markup to end a list, e.g., {@code &lt;/ol&gt;}
     */
    public void setListMarkup(String prologue, String epilogue) {
        listHead = prologue;
        listTail = epilogue;
    }

    public void setEntrySeparatorMarkup(String separator) {
        listSeparator = separator;
    }

    private int undeletedItemCounter;
    private String listHead;
    private String listTail;
    private String listSeparator;
    private String[][] itemDecorators = new String[3][2];

    /**
     * Creates a formatted list of consequences that compares the current design
     * state to the previous design state and appends them to the string
     * builder.
     *
     * @param b the buffer to append to
     * @param predecessor the consequence set for the previous design state, or
     * {@code null}
     * @param preamble an optional preamble to write after the category and
     * before the start of the list (if any), or {@code null}
     */
    public void formatConsequences(StringBuilder b, ConsequenceSet predecessor, String preamble) {
        if (predecessor == null) {
            predecessor = this;
        }

        writeCategory(b);
        if (preamble != null) {
            b.append(preamble);
        }

        // so writeEntry does not print a separator the first time out
        firstEntry = true;

        undeletedItemCounter = 1;

        // we build our list here first, then only make a <ol> list if it will not be empty
        StringBuilder list = new StringBuilder();

        // This is a (very) special case of finding the longest common substring;
        // it works because any given symbol (entry) may only occur once in a
        // given string (predecessor or this). As a result, there are no
        // alternative strings to consider: simply taking the list of elements
        // (in order) that occur in both sets yields the LCS.
        ArrayList<String> longestCommonSubstring = new ArrayList<>(20);
        for (String s : set) {
            if (predecessor.set.contains(s)) {
                longestCommonSubstring.add(s);
            }
        }

        Iterator<String> itPred = predecessor.set.iterator();
        Iterator<String> itThis = set.iterator();
        String currPred = null, currThis = null, currLCS = null;
        for (int i = 0; i < longestCommonSubstring.size(); ++i) {
            currLCS = longestCommonSubstring.get(i);

            // INVARIANT: We are synchronizing itPred and itThis with the LCS,
            //            which is the longest common substring of both the
            //            predecessor and this.
            //
            //            Since i < longestCommonSubstring.size(), there
            //            exists some successor element in both itPred and
            //            itThis that is equal to the currLCS entry.
            //
            //            Successors reached before the match is found are
            //            not in the LCS, therefore they are deletions
            //            (if in itPred) or insertions (if in itThis).
            // find deletions: loop as long as there are predecessor elements that are not in the LCS
            while (itPred.hasNext() && !(currPred = itPred.next()).equals(currLCS)) {
                writeEntry(list, currPred, DELETED, undeletedItemCounter);
            }

            // At this point, itPred.hasNext() may be false (if no more elements in LCS)
            // find insertions: loop as long as there are elements in this that are not in the LCS
            while (itThis.hasNext() && !(currThis = itThis.next()).equals(currLCS)) {
                writeEntry(list, currThis, INSERTED, undeletedItemCounter++);
            }

            // At this point, itThis.hasNext() may be false (if no more elements in LCS)
            // itPred and itThis are now caught up to the next item they have in common
            writeEntry(list, currLCS, KEPT, undeletedItemCounter++);
        }

        // itPred and itThis have no more items in common, but they may still have items
        while (itPred.hasNext()) {
            writeEntry(list, itPred.next(), DELETED, undeletedItemCounter);
        }
        while (itThis.hasNext()) {
            writeEntry(list, itThis.next(), INSERTED, undeletedItemCounter++);
        }

        if (list.length() > 0) {
            writeListHead(b, listHead);
            b.append(list);
            writeListTail(b, listTail);
        }
    }
    private boolean firstEntry;

    /**
     * Writes markup before the start of the list to describe the category.
     * Typically does nothing if the category is {@code null} or an empty
     * stirng.
     *
     * @param b the destination for the markup text
     */
    protected void writeCategory(StringBuilder b) {
        String categoryName = getCategory();
        if (categoryName != null && !categoryName.isEmpty()) {
            b.append("<h3>");
            b.append(categoryName);
            b.append("</h3>");
        }
    }

    /**
     * Writes markup to start the consequence list.
     *
     * @param b the destination for the markup text
     * @param listHead the current list heading markup
     */
    protected void writeListHead(StringBuilder b, String listHead) {
        b.append(listHead);
    }

    /**
     * Writes a decorated consequence list entry into the string builder. The
     * base class will use the currently set markup for the entry type to
     * compose the entry, replacing the text {@code @n;} with the undeleted
     * item number.
     *
     * @param b the destination for the markup text
     * @param entry the string that describes the consequence
     * @param comparison the entry style to use; one of {@link #DELETED},
     *     {@link #KEPT} or {@link #INSERTED}
     * @param undeletedItemNumber this is the suggested number to use if writing
     * a numbered list; it is incremented for each entry unless the entry is
     * being deleted from the list
     */
    protected void writeEntry(StringBuilder b, String entry, int comparison, int undeletedItemNumber) {
        if (firstEntry) {
            firstEntry = false;
        } else if (listSeparator != null) {
            writeListSeparator(b, listSeparator);
        }

        String lhs = itemDecorators[comparison][0];
        if (lhs != null) {
            lhs = lhs.replace("@n;", String.valueOf(undeletedItemNumber));
            b.append(lhs).append(entry);
            final String rhs = itemDecorators[comparison][1];
            if (rhs != null) {
                b.append(rhs);
            }
        }
    }

    /**
     * Writes markup to separate one list entry from another.
     *
     * @param b the destination for the markup text
     * @param listSeparator the current list separator markup
     */
    protected void writeListSeparator(StringBuilder b, String listSeparator) {
        b.append(listSeparator);
    }

    /**
     * Writes markup to end the formatting of a list.
     *
     * @param b the destination for the markup text
     * @param listTail the current list tail markup
     */
    protected void writeListTail(StringBuilder b, String listTail) {
        b.append(listTail);
    }

    /**
     * Comparison code for a consequence that was present in the previous set
     * but isn't now.
     */
    protected static final int DELETED = 0;
    /**
     * Comparison code for a consequence that is present in both sets.
     */
    protected static final int KEPT = 1;
    /**
     * Comparison code for a consequence that is present in the current set but
     * not the previous one.
     */
    protected static final int INSERTED = 2;
}
