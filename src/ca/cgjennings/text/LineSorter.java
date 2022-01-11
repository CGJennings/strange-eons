package ca.cgjennings.text;

import java.text.Collator;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Sorts a set of lines using various mechanisms. The base class sorts in
 * lexicographic order.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class LineSorter {

    public LineSorter() {
    }

    public LineSorter(boolean descending) {
        if (descending) {
            comparator = (Object o1, Object o2) -> -(((String) o1).compareTo((String) o2));
        }
    }

    /**
     * Subclasses can set this to modify the sort order or to apply special
     * sorting based on line tagging.
     */
    protected Comparator<Object> comparator = null;

    /**
     * Sorts an array of strings. Depending on the details of the algorithm, the
     * returned array may or may not be the same.
     *
     * @param lines the strings to sort
     * @return the original strings, in sorted order
     */
    public String[] sort(String[] lines) {
        TaggedLine[] tagged = prepSort(lines);
        return sortTagged(lines, tagged, comparator);
    }

    protected TaggedLine[] prepSort(String[] lines) {
        return null;
    }

    @SuppressWarnings("unchecked")
    protected String[] sortTagged(String[] lines, TaggedLine[] tagged, Comparator cmp) {
        if (tagged == null) {
            Arrays.sort(lines, (Comparator<String>) cmp);
        } else {
            Arrays.sort(tagged, (Comparator<TaggedLine>) cmp);
            String[] restored = new String[lines.length];
            for (int i = 0; i < tagged.length; ++i) {
                restored[i] = lines[tagged[i].getIndex()];
            }
            lines = restored;
        }
        return lines;
    }

    protected interface TaggedLine {

        public int getIndex();
    }

    /**
     * A line sorter that uses a locale-specific sorting order.
     */
    public static class LocalizedSorter extends LineSorter {

        public LocalizedSorter() {
            this(Locale.getDefault());
        }

        public LocalizedSorter(Locale locale) {
            this(locale, Collator.PRIMARY, false);
        }

        public LocalizedSorter(Locale locale, int strength, boolean descending) {
            Collator coll = Collator.getInstance(locale);
            coll.setStrength(strength);
            coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            comparator = coll;
            if (descending) {
                comparator = new ReverseComparator<>(comparator);
            }
        }
    }

    public static class SemanticSorter extends LocalizedSorter {

        private Comparator<Object> stringComparator;
        private NumberFormat format;

        public SemanticSorter() {
            this(Locale.getDefault());
        }

        public SemanticSorter(Locale locale) {
            this(locale, Collator.PRIMARY, false);
        }

        public SemanticSorter(Locale locale, int strength, final boolean descending) {
            super(locale, strength, descending);
            format = NumberFormat.getNumberInstance(locale);
            stringComparator = comparator;
            comparator = (Object o1, Object o2) -> {
                final LinkedList<Object> t1 = ((SemanticLine) o1).tokens;
                final LinkedList<Object> t2 = ((SemanticLine) o2).tokens;

                int maxToken = Math.min(t1.size(), t2.size());
                int i = 0;
                for (; i < maxToken; ++i) {
                    final Object k1 = t1.get(i);
                    final Object k2 = t2.get(i);

                    int tcmp;
                    if (k1 instanceof String || k2 instanceof String) {
                        tcmp = stringComparator.compare(k1.toString(), k2.toString());
                    } else {
                        double d = ((Number) k1).doubleValue() - ((Number) k2).doubleValue();
                        tcmp = (int) Math.signum(d);
                        if (descending) {
                            tcmp = -tcmp;
                        }
                    }
                    if (tcmp != 0) {
                        return tcmp;
                    }
                }
                // equal for all tokens in common, therefore treat whichever
                // is longer as the "greater" one
                return t1.size() - t2.size();
            };
        }

        @Override
        protected TaggedLine[] prepSort(String[] lines) {
            TaggedLine[] tagged = new SemanticLine[lines.length];
            for (int i = 0; i < lines.length; ++i) {
                tagged[i] = new SemanticLine(i, lines[i], format);
            }
            return tagged;
        }
    }

    private static final class SemanticLine implements TaggedLine {

        private int index;
        private LinkedList<Object> tokens;

        public SemanticLine(int index, String text, NumberFormat format) {
            this.index = index;
            tokens = new LinkedList<>();
            String[] sTokens = text.trim().split("\\s+");
            ParsePosition pp = new ParsePosition(0);
            for (int i = 0; i < sTokens.length; ++i) {
                String t = sTokens[i];
                if (t.isEmpty()) {
                    continue;
                }
                char c = t.charAt(0);
                if (c == '.' || c == ',' || Character.isDigit(c)) {
                    pp.setIndex(0);
                    Number n = format.parse(t, pp);
                    if (pp.getIndex() == 0) {
                        tokens.add(t);
                    } else {
                        tokens.add(n);
                        int j = pp.getIndex();
                        if (j < t.length()) {
                            sTokens[i--] = t.substring(j);
                        }
                    }
                } else {
                    for (int j = 1; j < t.length(); ++j) {
                        if (Character.isDigit(t.charAt(j))) {
                            sTokens[i--] = t.substring(j);
                            t = t.substring(0, j);
                        }
                    }
                    tokens.add(t);
                }
            }
        }

        @Override
        public int getIndex() {
            return index;
        }
    }

    private static class ReverseComparator<T> implements Comparator<T> {

        private final Comparator<T> cmp;

        public ReverseComparator(Comparator<T> c) {
            cmp = c;
        }

        @Override
        public int compare(T o1, T o2) {
            return -cmp.compare(o1, o2);
        }
    }
}
