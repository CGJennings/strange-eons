package ca.cgjennings.algo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.zip.InflaterInputStream;

/**
 * A searchable reverse index of the words contained in a set of documents. Each
 * document is represented by a string (such as a URL) that identifies it. Given
 * a query, the index returns the set of IDs whose documents match the query. A
 * {@code TextIndex} is not constructed directly; the index data is generated as
 * a separate step and stored in a file, and the {@code TextIndex} instance is
 * created from that file.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TextIndex implements Iterable<String> {

    private final String[] stopWords;
    private final String[] words;
    private String[] ids;
    private final BitSet map;

    TextIndex(String[] stopWords, String[] words, String[] ids, BitSet map) {
        this.stopWords = stopWords;
        this.words = words;
        this.ids = ids;
        this.map = map;
    }

    /**
     * Creates a text index from the specified file.
     *
     * @param file the previously written text index
     * @return an index generated from the stored index data
     * @throws IOException if an I/O error occurs while reading the index
     */
    public static TextIndex read(File file) throws IOException {
        return read(file.toURI().toURL());
    }

    /**
     * Creates a text index from the file at the specified URL.
     *
     * @param url the location of the previously written text index
     * @return an index generated from the stored index data
     * @throws IOException if an I/O error occurs while reading the index
     */
    public static TextIndex read(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return read(in);
        }
    }

    /**
     * Creates a text index from a stream.
     *
     * @param in a stream containing the previously written text index
     * @return an index generated from the stored index data
     * @throws IOException if an I/O error occurs while reading the index
     */
    public static TextIndex read(InputStream in) throws IOException {
        ObjectInputStream oin = new ObjectInputStream(new InflaterInputStream(in));
        try {
            String h = (String) oin.readObject();
            if (!HEADER.equals(h)) {
                throw new IOException("invalid file format");
            }
            String[] stopWords = (String[]) oin.readObject();
            String[] words = (String[]) oin.readObject();
            String[] ids = (String[]) oin.readObject();
            BitSet maps = (BitSet) oin.readObject();
            return new TextIndex(stopWords, words, ids, maps);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError();
        }
    }

    static final String HEADER = "TIDX1";

    /**
     * An iterator over the index words.
     *
     * @return word iterator
     */
    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(words).iterator();
    }

    /**
     * A {@code Query} represents a query expression to be matched against a
     * text index.
     */
    public static abstract class Query {

        /**
         * Returns the set of document IDs that satisfy the query in the
         * specified text index. This is equivalent to calling
         * {@code execute( ti ).getResultSet( ti )}
         *
         * @param ti the text index to query
         * @return the set of documents that match the query
         */
        public Set<String> evaluate(TextIndex ti) {
            return execute(ti).getResultSet(ti);
        }

        /**
         * Returns the {@code Result} of performing this query against a
         * specific text index.
         *
         * @param ti the text index to query
         * @return a result that captures the set of matching documents
         * @see #evaluate(ca.cgjennings.algo.TextIndex)
         */
        public abstract Result execute(TextIndex ti);
    }

    /**
     * A {@code Result} captures the result of a query.
     */
    public interface Result {

        /**
         * Returns the set of document IDs represented by this result.
         *
         * @param ti the text index to query
         * @return the set of documents that match the query
         */
        Set<String> getResultSet(TextIndex ti);
    }

    private static final Result ANYTHING = (ti) -> new HashSet<>(Arrays.asList(ti.ids));

    private static final Result NOTHING = (ti) -> new HashSet<>();

    private static class ResultSet implements Result {

        private final Set<String> s;

        public ResultSet(Set<String> s) {
            this.s = s;
        }

        @Override
        public Set<String> getResultSet(TextIndex ti) {
            return s;
        }
    };

    // used internally during execute implementations to optimize processing
    private Result simplify(Set<String> s) {
        if (s.isEmpty()) {
            return NOTHING;
        }
        if (s.size() == ids.length) {
            return ANYTHING;
        }
        return new ResultSet(s);
    }

    /**
     * A query that matches only documents that match both of two child queries.
     */
    public static class And extends Query {

        private Query q1, q2;

        /**
         * Creates a new query that is the logical and of the specified queries.
         *
         * @param q1 the first query operand
         * @param q2 the second query operand
         */
        public And(Query q1, Query q2) {
            if (q1 == null) {
                throw new NullPointerException("q1");
            }
            if (q2 == null) {
                throw new NullPointerException("q2");
            }
            this.q1 = q1;
            this.q2 = q2;
        }

        @Override
        public Result execute(TextIndex ti) {
            Result r1 = q1.execute(ti);
            Result r2 = q2.execute(ti);
            if (r1 == ANYTHING) {
                return r2;
            }
            if (r2 == ANYTHING) {
                return r1;
            }
            if (r1 == NOTHING || r2 == NOTHING) {
                return NOTHING;
            }

            Set<String> s = new HashSet<>(r1.getResultSet(ti));
            s.retainAll(r2.getResultSet(ti));
            return ti.simplify(s);
        }

        @Override
        public String toString() {
            return "(AND " + q1 + ' ' + q2 + ')';
        }

    }

    /**
     * A query that matches any documents that match either of two child
     * queries.
     */
    public static class Or extends Query {

        private Query q1, q2;

        /**
         * Creates a new query that is the logical or of the specified queries.
         *
         * @param q1 the first query operand
         * @param q2 the second query operand
         */
        public Or(Query q1, Query q2) {
            if (q1 == null) {
                throw new NullPointerException("q1");
            }
            if (q2 == null) {
                throw new NullPointerException("q2");
            }
            this.q1 = q1;
            this.q2 = q2;
        }

        @Override
        public Result execute(TextIndex ti) {
            Result r1 = q1.execute(ti);
            Result r2 = q2.execute(ti);
            if (r1 == NOTHING) {
                return r2;
            }
            if (r2 == NOTHING) {
                return r1;
            }
            if (r1 == ANYTHING || r2 == ANYTHING) {
                return ANYTHING;
            }

            Set<String> s = new HashSet<>(r1.getResultSet(ti));
            s.addAll(r2.getResultSet(ti));
            return ti.simplify(s);
        }

        @Override
        public String toString() {
            return "(OR " + q1 + ' ' + q2 + ')';
        }
    }

    /**
     * A query that matches every document except the documents matched by its
     * child query.
     */
    public static class Not extends Query {

        private Query q;

        /**
         * Creates a new query that is the logical negation of the specified
         * query.
         *
         * @param q the query to invert
         */
        public Not(Query q) {
            if (q == null) {
                throw new NullPointerException("q");
            }
            this.q = q;
        }

        @Override
        public Result execute(TextIndex ti) {
            if (q instanceof Atom) {
                return ((Atom) q).executeInverse(ti);
            }
            Result r = q.execute(ti);
            if (r == ANYTHING) {
                return NOTHING;
            }
            if (r == NOTHING) {
                return ANYTHING;
            }
            Set<String> s = ANYTHING.getResultSet(ti);
            s.removeAll(r.getResultSet(ti));
            return ti.simplify(s);
        }

        @Override
        public String toString() {
            return "(NOT " + q.toString() + ")";
        }
    }

    /**
     * An atom is a query that matches all documents that contain a specified
     * one-word search term.
     */
    public static class Atom extends Query {

        private String term;

        /**
         * Creates an atom that matches {@code term}.
         *
         * @param term the word to match against the document set
         */
        public Atom(String term) {
            if (term == null) {
                throw new NullPointerException("term");
            }
            this.term = term;
        }

        @Override
        public Result execute(TextIndex ti) {
            if (Arrays.binarySearch(ti.stopWords, term) >= 0) {
                return ANYTHING;
            }
            int index = Arrays.binarySearch(ti.words, term);
            if (index < 0) {
                return NOTHING;
            }

            BitSet map = ti.map;
            HashSet<String> result = new HashSet<>();
            int docCount = ti.ids.length;
            int offset = index * docCount;
            for (int i = 0; i < ti.ids.length; ++i) {
                if (map.get(offset + i)) {
                    result.add(ti.ids[i]);
                }
            }
//			BitSet map = ti.maps[ index ];
//			for( int i = map.nextSetBit( 0 ); i >= 0; i = map.nextSetBit( i+1 ) ) {
//				result.add( ti.ids[i] );
//			}
            return ti.simplify(result);
        }

        /**
         * Returns the inverse of the set of documents containing the search
         * term. This is equivalent to {@code new Not( this ).execute( ti )}.
         *
         * @param ti the text index to match against
         * @return a result representing the documents that do not contain the
         * search term
         */
        public Result executeInverse(TextIndex ti) {
            if (Arrays.binarySearch(ti.stopWords, term) >= 0) {
                return NOTHING;
            }
            int index = Arrays.binarySearch(ti.words, term);
            if (index < 0) {
                return ANYTHING;
            }

            BitSet map = ti.map;
            HashSet<String> result = new HashSet<>();
            int docCount = ti.ids.length;
            int offset = index * docCount;
            for (int i = 0; i < ti.ids.length; ++i) {
                if (!map.get(offset + i)) {
                    result.add(ti.ids[i]);
                }
            }
//			HashSet<String> result = new HashSet<String>();
//			BitSet map = ti.maps[ index ];
//			for( int i = map.nextClearBit( 0 ); i < ti.ids.length; i = map.nextClearBit( i+1 ) ) {
//				result.add( ti.ids[i] );
//			}
            return ti.simplify(result);
        }

        @Override
        public String toString() {
            return '<' + term + '>';
        }
    }

    /**
     * A parser that converts a plain text query string into a query. The query
     * parser recognizes the following syntax, except that a query may also
     * empty without provoking a syntax error:
     * <pre>
     * query = expression [["|"] query]
     * expression = term | "!" term
     * term = word | "(" query ")"
     * </pre> where a word is any word to be searched for (and not containing a
     * space or the reserved punctuation marks "|", "!", "(", or ")". The "|"
     * symbol performs and {@code Or} query, sequential factors are combined
     * into {@code And} queries, "!" applies a {@code Not} query to its
     * argument, and parentheses may be used to group the query into
     * subexpressions. Some examples:
     *
     * <table border=0>
     * <caption>Examples</caption>
     * <tr><th>Query        <th>Find Documents Containing</tr>
     * <tr><td>apple        <td>"apple"</tr>
     * <tr><td>apple ball   <td>both "apple" and "ball"</tr>
     * <tr><td>apple|ball   <td>either "apple" or "ball"</tr>
     * <tr><td>!apple       <td>not "apple"
     * <tr><td>!(a|b)       <td>neither "a" nor "b"</tr>
     * <tr><td>a | b c      <td>either "a" or both "b" and "c"</tr>
     * </table>
     */
    public static class QueryParser {

        /**
         * Creates a new query parser.
         */
        public QueryParser() {
        }

        /**
         * Parses a search expression into an executable query.
         *
         * @param qs the query to parse
         * @return a parse tree of query nodes that represents the expression
         */
        public Query parse(String qs) {
            LinkedList<String> tokens = tokenize(qs);
            if (tokens.isEmpty()) {
                return new Atom("");
            }
            try {
                Query q = parseQuery(tokens);
                if (tokens.peek() != null) {
                    throw new IllegalArgumentException("unclosed expression");
                }
                return q;
            } catch (IllegalArgumentException e) {
                // the error happened when there were tokens.size() tokens left to parse
                int badIndex = tokens.size();
                tokens = tokenize(qs);
                badIndex = tokens.size() - badIndex;

                String last = null;
                StringBuilder b = new StringBuilder(e.getLocalizedMessage()).append(": ");
                for (int i = 0; i < tokens.size(); ++i) {
                    if (word(last) || bar(last) || rparen(last)) {
                        b.append(' ');
                    }
                    if (i == badIndex) {
                        b.append("^ ");
                    }
                    b.append(tokens.get(i));
                    last = tokens.get(i);
                }
                if (badIndex == tokens.size()) {
                    b.append(" ^");
                }
                throw new IllegalArgumentException(b.toString());
            }
        }

        /**
         * Converts the query string into a list of tokens.
         *
         * @param qs the query string to tokenize
         * @return a possibly empty list of tokens
         */
        protected LinkedList<String> tokenize(String qs) {
            LinkedList<String> t = new LinkedList<>();
            int start = 0;
            int state = 0;
            for (int i = 0; i < qs.length(); ++i) {
                char c = qs.charAt(i);
                switch (state) {
                    case 0:	// waiting for identifier
                        if (!Character.isSpaceChar(c)) {
                            switch (c) {
                                case '!':
                                case '|':
                                case '(':
                                case ')':
                                    t.add(String.valueOf(c));
                                    break;
                                default:
                                    start = i;
                                    state = 1;
                            }
                        }
                        break;
                    case 1:   // inside identifier
                        if (!Character.isSpaceChar(c)) {
                            switch (c) {
                                case '!':
                                case '|':
                                case '(':
                                case ')':
                                    t.add(qs.substring(start, i));
                                    t.add(String.valueOf(c));
                                    state = 0;
                                    break;
                            }
                        } else {
                            t.add(qs.substring(start, i));
                            state = 0;
                        }
                        break;
                }
            }
            if (state == 1) {
                t.add(qs.substring(start));
            }
            return t;
        }

        private Query parseQuery(LinkedList<String> tokens) {
            Query q = parseExpr(tokens);
            if (!tokens.isEmpty()) {
                if (rparen(tokens.peek())) {
                    // if this returns to parseTerm, ok, we'll pop the )
                    // if this returns to parse we'll see the unpopped ) and throw
                    return q;
                }
                if (bar(tokens.peek())) {
                    tokens.pop();
                    q = new Or(parseQuery(tokens), q);
                } else {
                    q = new And(parseQuery(tokens), q);
                }
            }
            return q;
        }

        private Query parseExpr(LinkedList<String> tokens) {
            if (bang(tokens.peek())) {
                tokens.pop();
                return new Not(parseTerm(tokens));
            }
            return parseTerm(tokens);
        }

        private Query parseTerm(LinkedList<String> tokens) {
            if (tokens.isEmpty()) {
                throw new IllegalArgumentException("expected search term or expression");
            }

            String t = tokens.pop();
            if (lparen(t)) {
                Query q = parseQuery(tokens);
                if (!rparen(tokens.peek())) {
                    throw new IllegalArgumentException("missing )");
                }

                if (tokens.isEmpty()) {
                    throw new IllegalArgumentException("expected search term or expression");
                }
                tokens.pop();
                return q;
            }

            if (!word(t)) {
                throw new IllegalArgumentException("expected search term, not " + t);
            }
            return new Atom(t);
        }

        private static boolean bar(String t) {
            return "|".equals(t);
        }

        private static boolean bang(String t) {
            return "!".equals(t);
        }

        private static boolean lparen(String t) {
            return "(".equals(t);
        }

        private static boolean rparen(String t) {
            return ")".equals(t);
        }

        private static boolean word(String t) {
            return t != null && (t.length() != 1 || !(bar(t) || bang(t) || lparen(t) || rparen(t)));
        }
    }
}
