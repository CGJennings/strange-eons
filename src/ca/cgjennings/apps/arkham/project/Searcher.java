package ca.cgjennings.apps.arkham.project;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs a multi-threaded search of a file tree and informs a listener of the
 * results.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1a7
 */
public class Searcher {

    /**
     * Begins a new search in the background, passing matches to the specified
     * receiver.
     *
     * @param parent the root of the tree at which the search should begin
     * @param pattern the regular expression to search for
     * @param receiver the receiver to be notified of search results as they
     * become available
     * @throws NullPointerException if any parameter is {@code null}
     */
    public Searcher(final Member parent, Pattern pattern, ResultReceiver receiver) {
        this(parent, pattern, receiver, 0);
    }

    Searcher(final Member parent, Pattern pattern, ResultReceiver receiver, final int startDelay) {
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        if (pattern == null) {
            throw new NullPointerException("pattern");
        }
        if (receiver == null) {
            throw new NullPointerException("receiver");
        }
        cancel = false;
        executor = new ThreadPoolExecutor(
                Math.min(Runtime.getRuntime().availableProcessors() - 1, 1),
                Integer.MAX_VALUE,
                100, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );
        this.receiver = receiver;
        this.pattern = pattern;
        new Thread() {
            @Override
            public void run() {
                if (startDelay != 0) {
                    try {
                        Thread.sleep(startDelay);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                // count all children that we will be scanning
                final int totalFiles = countChildren(parent);
                if (!cancel) {
                    Searcher.this.receiver.setFileCount(Searcher.this, totalFiles);
                }

                // start scanning
                if (totalFiles > 0) {
                    scanFolder(parent);
                    try {
                        executor.awaitTermination(365_250L, TimeUnit.DAYS);
                    } catch (InterruptedException ex) {
                    }
                    if (!cancel) {
                        Searcher.this.receiver.searchCompleted(Searcher.this);
                    }
                    Searcher.this.receiver.searchCompleted(Searcher.this);
                }
            }
        }.start();
    }

    private int countChildren(Member m) {
        if (isExcluded(m)) {
            return 0;
        }

        int sum = 1;
        if (m.hasChildren()) {
            for (Member k : m.getChildren()) {
                if (k.hasChildren()) {
                    sum += countChildren(k);
                } else {
                    ++sum;
                }
            }
        }
        return sum;
    }

    /**
     * Returns the number of files that have been scanned so far.
     *
     * @return the number of scanned files
     */
    public int getFilesSearched() {
        return finished.get();
    }

    /**
     * Recursively scans folders, calling {@link #scanFile} for all non-folders
     * found.
     *
     * @param member the parent folder to scan
     */
    private void scanFolder(final Member member) {
        if (cancel) {
            return;
        }

        if (isExcluded(member)) {
            return;
        }

        // check the file name for a match
        File f = member.getFile();
        Matcher matcher = pattern.matcher(f.getName());
        if (matcher.find()) {
            if (!cancel) {
                receiver.addResult(this, member, 0, f.getName(), matcher.start(), matcher.end());
            }
        }

        // continue scanning recursively
        if (member.isFolder()) {
            for (Member child : member.getChildren()) {
                scanFolder(child);
            }
            finished.incrementAndGet();
        } else {
            executor.execute(() -> {
                if (cancel || Thread.interrupted()) {
                    return;
                }
                scanFile(member);
            });
        }
    }

    /**
     * Called to scan a file for matches to the specified pattern.
     *
     * @param member the non-{@code null}, non-folder member to scan (if
     * possible)
     */
    private void scanFile(Member member) {
        FileInputStream in = null;
        BufferedReader r = null;
        try {
            File f = member.getFile();
            Charset cs = getEncodingFor(member);
            if (cs != null) {
                final boolean isBinaryType = member.getMetadataSource().getDefaultCharset(member) == null;
                in = new FileInputStream(f);
                r = new BufferedReader(new InputStreamReader(in, cs), 16 * 1_024);
                scanReader(member, r, isBinaryType);
            }
        } catch (IOException e) {
            StrangeEons.log.log(Level.WARNING, "exception while trying to search file " + member.getFile(), e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (!cancel) {
                receiver.setFinishedFileCount(this, finished.incrementAndGet());
            }
        }
    }

    /**
     * Called to scan the contents of a file.
     *
     * @param member the member being searched
     * @param reader a reader than can be used to read text content to be
     * searched
     * @param isBinaryType {@code true} if the file is binary rather than a text
     * file
     * @throws IOException if an I/O error occurs while reading the file
     */
    protected void scanReader(Member member, BufferedReader reader, boolean isBinaryType) throws IOException {
        int linenum = 1;
        String line;
        while ((line = reader.readLine()) != null && !cancel) {
            if (Thread.interrupted()) {
                return;
            }
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                if (!cancel) {
                    receiver.addResult(this, member, isBinaryType ? -1 : linenum, line, matcher.start(), matcher.end());
                }

                // we won't print specific results, so any match will do
                if (isBinaryType) {
                    return;
                }

                // report any other matches in the line
                while (matcher.find()) {
                    if (!cancel) {
                        receiver.addResult(this, member, linenum, line, matcher.start(), matcher.end());
                    }
                }
            }
            ++linenum;
        }
    }

    /**
     * Returns {@code true} if the given member should be skipped by the
     * searcher. (The base class returns {@code false}, meaning that all files
     * are searched.)
     *
     * @param member the file to test
     * @return {@code true} if the file should be excluded
     */
    protected boolean isExcluded(Member member) {
        return false;
    }

    /**
     * Returns the character encoding to use when scanning the specified member.
     * If the file is a known text file type, the default encoding for that type
     * will be returned. If the file is one of a small set of known binary file
     * types, returns an encoding suitable for that file type. Otherwise,
     * returns {@code null} to indicate that the file cannot be scanned.
     *
     * @param member the member to determine an encoding for
     * @return an encoding to use for scanning, or {@code null} if none applies
     * @see MetadataSource#getDefaultCharset
     */
    protected Charset getEncodingFor(Member member) {
        Charset cs = member.getMetadataSource().getDefaultCharset(member);
        if (cs != null) {
            return cs;
        }

        if (ProjectUtilities.matchExtension(member, FORCED_SEARCH_EXTENSIONS)) {
            return TextEncoding.UTF8_CS;
        }

        return null;
    }

    /**
     * Cancels the search before it completes.
     */
    public void cancel() {
        cancel = true;
        executor.shutdownNow();
        receiver.searchCompleted(this);
    }

    private Pattern pattern;
    private ResultReceiver receiver;
    private ThreadPoolExecutor executor;
    private volatile boolean cancel;
    private AtomicInteger finished = new AtomicInteger();

    private static final String[] FORCED_SEARCH_EXTENSIONS = new String[]{
        "class", "eon"
    };

    /**
     * An interface that is sent search results and progress updates as the
     * search proceeds.
     */
    public interface ResultReceiver {

        /**
         * Called when a new match is found.
         *
         * @param source the instance that is performing the search
         * @param member the member that matched the pattern
         * @param line the line number within the member that matched; this will
         * be 0 if the match is in the file name, or -1 for binary files
         * @param context a snippet of the text surrounding the match
         * @param start the start of the match, relative to the start of the
         * context string
         * @param end the end of the match, relative to the start of the context
         * string
         */
        void addResult(Searcher source, Member member, int line, String context, int start, int end);

        /**
         * Called to inform the receiver of the total number of files being
         * scanned.
         *
         * @param source the instance that is performing the search
         * @param count the number of files scanned so far
         */
        void setFileCount(Searcher source, int count);

        /**
         * Called periodically to update the receiver with the number of files
         * that have been scanned so far.
         *
         * @param source the instance that is performing the search
         * @param fileCount the total number of searched files
         */
        void setFinishedFileCount(Searcher source, int fileCount);

        /**
         * Called when the search is completed, regardless of whether it
         * completes normally or is cancelled.
         *
         * @param source the instance that was performing the search
         */
        void searchCompleted(Searcher source);
    }
}
