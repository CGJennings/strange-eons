package ca.cgjennings.algo;

import ca.cgjennings.algo.TextIndexer.DefaultTextMapper;
import ca.cgjennings.algo.TextIndexer.TextMapper;
import ca.cgjennings.apps.arkham.BusyDialog;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.dialog.ErrorDialog;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Creates a file that can be used to create a {@link TextIndex} by indexing the
 * contents of a number of source texts. The indexer uses a {@link TextMapper}
 * to locate source texts from a set of identifiers. The resulting index
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class TextIndexer implements MonitoredAlgorithm {

    private boolean enableCache = false;
    private TextMapper tmap;
    private Set<String> stopWords = new HashSet<>();
    private BreakIterator it = null;

    /**
     * Creates a new text indexer.
     */
    public TextIndexer() {
    }

    /**
     * Returns the text mapper used to map source identifiers to texts.
     *
     * @return the current mapper
     */
    public TextMapper getTextMapper() {
        if (tmap == null) {
            setTextMapper(new DefaultTextMapper());
        }
        if (tmap instanceof CachingTextMapper) {
            return ((CachingTextMapper) tmap).source;
        }
        return tmap;
    }

    /**
     * Sets the text mapper used to map source identifiers to texts.
     *
     * @param mapper the mapper to use to locate source texts
     */
    public void setTextMapper(TextMapper mapper) {
        if (mapper == null) {
            throw new NullPointerException("mapper");
        }
        if (enableCache) {
            tmap = new CachingTextMapper(mapper);
        } else {
            tmap = mapper;
        }
    }

    /**
     * Returns the break iterator used to split the document into words. Each
     * word will become a searchable word in the index entry unless it is on the
     * stop word list.
     *
     * @return the break iterator used to find words in the source texts
     */
    public BreakIterator getBreakIterator() {
        if (it == null) {
            it = BreakIterator.getWordInstance();
        }
        return it;
    }

    /**
     * Sets the break iterator used to split the document into words.
     *
     * @param it the break iterator that tokenizes the source texts
     */
    public void setBreakIterator(BreakIterator it) {
        if (it == null) {
            throw new NullPointerException("it");
        }
        this.it = it;
    }

    @Override
    public ProgressListener setProgressListener(ProgressListener li) {
        if (li != null && tracker == null) {
            tracker = new ProgressHelper();
            tracker.setSource(this);
        }
        return tracker == null ? null : tracker.setProgressListener(li);
    }
    private ProgressHelper tracker;

    /**
     * Generates a {@link TextIndex} in memory. This has a similar effect to
     * writing the index to a file and then immediately creating a
     * {@link TextIndex} instance from the file, but without actually creating
     * the file.
     *
     * @param sourceIDs the IDs of the documents to include in the index
     * @return a searchable index
     */
    public TextIndex makeIndex(Collection<String> sourceIDs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(256 * sourceIDs.size());
        try {
            write(out, sourceIDs);
            out.flush();
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            return TextIndex.read(in);
        } catch (IOException e) {
            throw new RuntimeException("error while simulating file I/O", e);
        }
    }

    /**
     * Creates an index for a collection of sources, writing that index to a
     * file.
     *
     * @param f the file to write the index to
     * @param sourceIDs the IDs to index
     * @throws IOException if an I/O error occurs
     */
    public void write(File f, Collection<String> sourceIDs) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(f)) {
            BufferedOutputStream out = new BufferedOutputStream(fout, 64 * 1_024);
            write(out, sourceIDs);
            out.flush();
        }
    }

    /**
     * Creates an index for a collection of sources, writing that index to a
     * stream.
     *
     * @param stream the output stream to write the index to
     * @param sourceIDs the IDs to index
     * @throws IOException if an I/O error occurs
     */
    public void write(OutputStream stream, Collection<String> sourceIDs) throws IOException {
        final int numSources = sourceIDs.size();

        if (tracker != null) {
            tracker.setStepCount(numSources * 2);
        }

        // set of lower case stop words
        HashSet<String> stops = new HashSet<>(stopWords.size());
        for (String stop : stopWords) {
            stops.add(stop.toLowerCase(Locale.ENGLISH));
        }

        // get list of unique words
        String[] words = createWordList(stops, sourceIDs);

        // create bit sets for each word; each bitset contains a bit for each unique word, set if the source contains the word
        BitSet bitmap = new BitSet(words.length * numSources);
        HashMap<String, Integer> wordIndex = new HashMap<>(words.length);
        for (int i = 0; i < words.length; ++i) {
            wordIndex.put(words[i], i);
        }

//		BitSet[] maps = new BitSet[ words.length ];
//		HashMap<String,BitSet> wordMap = new HashMap<String,BitSet>( words.length );
//		for( int i=0; i<maps.length; ++i ) {
//			maps[i] = new BitSet();
//			wordMap.put( words[i], maps[i] );
//		}
        // fill in the bitmaps with the words found in each source
        fillMap(bitmap, wordIndex, sourceIDs);
//		wordMap = null;

        // map the source IDs to index IDs
        int i = 0;
        String[] ids = new String[sourceIDs.size()];
        for (String s : sourceIDs) {
            ids[i] = tmap.getIndexID(s);
            if (ids[i++] == null) {
                throw new NullPointerException("mapped source ID " + (i - 1));
            }
        }

        // create a sorted array of the stop words
        String[] stopWords = stops.toArray(new String[stops.size()]);
        Arrays.sort(stopWords);
//		TextIndex ti = new TextIndex( stopWords, words, ids, maps );

        ObjectOutputStream out = null;
        DeflaterOutputStream deflater = new DeflaterOutputStream(stream);
//		try {
        out = new ObjectOutputStream(deflater);
        out.writeObject(TextIndex.HEADER);
        out.writeObject(stopWords);
        out.writeObject(words);
        out.writeObject(ids);
        out.writeObject(bitmap);
        out.flush();
        deflater.finish();
        deflater.flush();
//		} finally {
//			if( out != null ) {
//				out.close();
//			} else if( fout != null ) {
//				fout.close();
//			}
//		}
    }

    private String[] createWordList(Set<String> stopList, Collection<String> sourceIDs) throws IOException {
        HashSet<String> words = new HashSet<>(5_000);
        for (String id : sourceIDs) {
            String text = tmap.getText(id);
            for (String word : new WordIterator(text, getBreakIterator())) {
                words.add(word);
            }
            if (tracker != null) {
                tracker.addCompletedSteps(1);
            }
        }
        for (String stop : stopList) {
            words.remove(stop);
        }
        String[] list = words.toArray(new String[words.size()]);
        Arrays.sort(list);
        return list;
    }

    private void fillMap(BitSet bitmap, Map<String, Integer> wordIndex, Collection<String> sourceIDs) throws IOException {
        final int docCount = sourceIDs.size();
        int i = 0;
        for (String id : sourceIDs) {
            String text = tmap.getText(id);
            for (String word : new WordIterator(text, getBreakIterator())) {
                Integer wordNumber = wordIndex.get(word);
                if (wordNumber != null) {
                    bitmap.set(wordNumber * docCount + i);
                }
            }
            if (tracker != null) {
                tracker.addCompletedSteps(1);
            }
            ++i;
        }
    }

    /**
     * A text mapper maps an identifier to a source text to be indexed. It also
     * allows you to substitute the source ID used to locate the text during
     * indexing with another ID used to locate the text when using the index
     * later. For example, you could use a local copy of a Web site to create an
     * index, and substitute the URL of the online version of the Web site
     * within the index.
     */
    public interface TextMapper {

        /**
         * Given a source ID, return the text associated with that ID.
         *
         * @param sourceID an identifier that the mapper uses to locate the text
         * @return the text mapped to by the ID
         * @throws IOException if an I/O error occurs while fetching the
         * document
         */
        public String getText(String sourceID) throws IOException;

        /**
         * Maps a source identifier to an index identifier. If the source ID
         * should be identified differently in the index, this returns the
         * version to include in the index.
         *
         * @param sourceID the ID used to locate the text during indexing
         * @return the ID used to locate the text when using the index
         */
        public String getIndexID(String sourceID);
    }

    /**
     * A default text mapper implementation that assumes that the source IDs
     * represent URLs. The returned indexed IDs are identical to the source IDs.
     */
    public static class DefaultTextMapper implements TextMapper {

        @Override
        public String getIndexID(String sourceID) {
            if (sourceID == null) {
                throw new NullPointerException("sourceID");
            }
            return sourceID;
        }

        /**
         * Given a source ID, return the text associated with that ID. The
         * default mapper does this by calling {@link #toURL(java.lang.String)}
         * on the source ID, reading and then
         * {@linkplain #preprocess(java.lang.String, java.net.URL, java.lang.String) preprocessing}
         * the result.
         *
         * @param sourceID an identifier that the mapper uses to locate the text
         * @return the text mapped to by the ID
         * @throws IOException if an I/O error occurs while fetching the
         * document
         */
        @Override
        public String getText(String sourceID) throws IOException {
            URL url = toURL(sourceID);
            String text = read(sourceID, url, null);
            return preprocess(sourceID, url, text);
        }

        /**
         * Return a URL for the source ID. The default implementation simply
         * returns a new URL using the source ID as if by
         * <code>new URL(sourceID)</code>.
         *
         * @param sourceID returns a URL for the source ID
         * @return a URL to use to read the source text
         * @throws IOException if an error occurs while creating the URL
         */
        protected URL toURL(String sourceID) throws IOException {
            return new URL(sourceID);
        }

        /**
         * Reads the source document from the URL and returns it as a string of
         * indexable words.
         *
         * @param sourceID the identifier of the document
         * @param url the URL to read the document from
         * @param encodingHint the name of an encoding, or <code>null</code> to
         * use a default encoding
         * @return the document text
         * @throws IOException if an error occurs while reading the document
         */
        protected String read(String sourceID, URL url, String encodingHint) throws IOException {
            if (encodingHint == null) {
                encodingHint = "UTF-8";
            }

            if (isHTML(sourceID)) {
                return readHTML(url);
            }

            try (InputStream in = url.openStream()) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, encodingHint), 16_384);
                return read(r);
            }
        }

        /**
         * Preprocesses the text after it is read but before it is returned to
         * the caller of {@link #getText}. The default implementation returns
         * the text unchanged.
         *
         * @param sourceID the identifier of the document
         * @param url the URL that the document was read from
         * @param text the original text
         * @return the modified text
         */
        protected String preprocess(String sourceID, URL url, String text) {
            return text;
        }

        private String readHTML(URL url) throws IOException {
            JEditorPane jed = new JEditorPane();
            jed.setEditorKit(new HTMLEditorKit() {
                @Override
                public Document createDefaultDocument() {
                    Document d = super.createDefaultDocument();
                    ((HTMLDocument) d).setAsynchronousLoadPriority(-1);
                    return d;
                }
            });
            jed.setPage(url);
            HTMLDocument doc = (HTMLDocument) jed.getDocument();
            try {
                return doc.getText(0, doc.getLength());
            } catch (BadLocationException ex) {
                throw new AssertionError();
            }
        }

        private String read(BufferedReader r) throws IOException {
            StringBuilder b = new StringBuilder();
            String li;
            while ((li = r.readLine()) != null) {
                if (b.length() > 0) {
                    b.append('\n');
                }
                b.append(li);
            }
            return li.toString();
        }

        private boolean isHTML(String sourceID) {
            if (sourceID == null || sourceID.length() < 4) {
                return false;
            }
            final String htm = sourceID.substring(sourceID.length() - 4).toLowerCase(Locale.ENGLISH);
            if (htm.equals(".htm") || htm.equals(".php")) {
                return true;
            }
            if (sourceID.length() > 4) {
                final String html = sourceID.substring(sourceID.length() - 5).toLowerCase(Locale.ENGLISH);
                return html.equals(".html");
            }
            return false;
        }
    }

    /**
     * Wraps the user's text mapper to cache read files.
     */
    private static class CachingTextMapper implements TextMapper {

        private TextMapper source;
        private HashMap<String, SoftReference<String>> cache = new HashMap<>();

        public CachingTextMapper(TextMapper source) {
            if (source == null) {
                throw new NullPointerException("source");
            }
            this.source = source;
        }

        @Override
        public String getIndexID(String sourceID) {
            String id = source.getIndexID(sourceID);
            if (id == null) {
                id = sourceID;
            }
            return id;
        }

        @Override
        public String getText(String sourceID) throws IOException {
            String s = null;
            SoftReference<String> c = cache.get(sourceID);
            if (c != null) {
                s = c.get();
            }
            if (s == null) {
                s = source.getText(sourceID);
                cache.put(sourceID, new SoftReference<>(s));
            }
            return s;
        }
    }

    /**
     * Jiggers a break iterator into a plain iterator, and scrubs the token
     * stream as it goes.
     */
    private static class WordIterator implements Iterator<String>, Iterable<String> {

        private String s;
        private String nextToken;
        private BreakIterator bi;
        private int start, end;

        public WordIterator(String s, BreakIterator bi) {
            this.s = s;
            this.bi = bi;
            bi.setText(s);
            start = bi.first();
            end = bi.next();
            nextToken = fetch();
        }

        @Override
        public Iterator<String> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return nextToken != null;
        }

        @Override
        public String next() {
            if (nextToken == null) {
                throw new NoSuchElementException();
            }
            String t = nextToken;
            nextToken = fetch();
            return t;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private String fetch() {
            String accepted = null;
            while (accepted == null && end != BreakIterator.DONE) {
                if (isWord(start, end)) {
                    accepted = s.substring(start, end);
                }
                start = end;
                end = bi.next();
            }
            if (accepted != null) {
                accepted = accepted.toLowerCase(Locale.ENGLISH);
            }
            return accepted;
        }

        private boolean isWord(int start, int end) {
            for (int i = start; i < end; ++i) {
                char c = s.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A convenience method that creates an index using the default
     * configuration.
     *
     * @param indexFile the file to write the index to
     * @param sourceURLs an array of source URLs
     * @param indexIDs an array of identifers to use in the index for the source
     * URL at the same index, or <code>null</code> to use the sourceURLs
     * @throws IOException if an error occurs while writing the file
     */
    public static void createIndex(final File indexFile, final String[] sourceURLs, final String[] indexIDs) throws IOException {
        final HashMap<String, String> map = new HashMap<>(sourceURLs.length);
        if (indexIDs != null) {
            for (int i = 0; i < sourceURLs.length; ++i) {
                map.put(sourceURLs[i], indexIDs[i]);
            }
        } else {
            for (int i = 0; i < sourceURLs.length; ++i) {
                map.put(sourceURLs[i], sourceURLs[i]);
            }
        }
        final TextMapper tm = new DefaultTextMapper() {
            @Override
            public String getIndexID(String sourceID) {
                return map.get(sourceID);
            }

            @Override
            public String getText(String sourceID) throws IOException {
                int sl = sourceID.lastIndexOf('/');
                String id = sourceID;
                if (sl >= 0) {
                    id = sourceID.substring(sl + 1);
                }
                BusyDialog.titleText(id);
                return super.getText(sourceID);
            }

        };
        final TextIndexer ti = new TextIndexer();
        ti.setTextMapper(tm);
        ti.setProgressListener(new ProgressListener() {
            private boolean called;

            @Override
            public boolean progressUpdate(Object source, float progress) {
                if (!called) {
                    BusyDialog.maximumProgress(1_000);
                    called = true;
                }
                BusyDialog.currentProgress((int) (progress * 1000f));
                return false;
            }
        });
        new BusyDialog(StrangeEons.getWindow(), null, () -> {
            try {
                ti.write(indexFile, Arrays.asList(sourceURLs));
            } catch (Throwable e) {
                e.printStackTrace();
                ErrorDialog.displayError(e.getClass().getSimpleName(), e);
            }
        });
    }
}
