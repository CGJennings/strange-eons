package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.spelling.dict.BucketList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import resources.CoreComponents;

/**
 * A shared database of cached Java class and package information.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class APIDatabase {

    private APIDatabase() {
    }

    /**
     * Returns a package root for the Strange Eons API (including the Java API).
     * This is a tree of {@link APINode}s representing the classes available for
     * plug-ins to use.
     *
     * <p>
     * The returned package root should be treated as read-only, as it may be
     * shared by many callers. Note that the root may be empty or incomplete if
     * the {@linkplain CoreComponents#API_DOCUMENTATION API core plug-in} is not
     * installed.
     *
     * @return a package root of available classes
     */
    public synchronized static PackageRoot getPackageRoot() {
        PackageRoot r = null;
        if (pkgRef != null) {
            r = pkgRef.get();
        }
        if (r == null) {
            r = loadRoot();
            pkgRef = new SoftReference<>(r);
        }
        return r;
    }

    private static PackageRoot loadRoot() {
        PackageRoot root = new PackageRoot();
        loadJDK(root);
        loadSE(root);
        return root;
    }

    private static void loadJDK(PackageRoot root) {
        BucketList list = new BucketList();
        InputStream in = APIDatabase.class.getResourceAsStream("jseclasses.cpl");
        try {
            list.read(in);
            String[] classes = list.getWordsAsArray();
            addToRoot(root, classes);
        } catch (Exception e) {
            StrangeEons.log.log(Level.WARNING, null, e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    private static void loadSE(PackageRoot root) {
        try {
            String[] classes = listJavaDocClasses(STRANGE_EONS_API_BASE);
            addToRoot(root, classes);
        } catch (IOException e) {
            StrangeEons.log.log(Level.WARNING, null, e);
        }
    }

    private static void addToRoot(PackageRoot root, String[] classes) {
        for (String c : classes) {
            root.addName(c);
        }
    }

    private static SoftReference<PackageRoot> pkgRef;

    /**
     * Given the URL of the base directory of a JavaDoc collection, extracts a
     * list of all of the documented classes with their full package names.
     *
     * @param baseURL a string containing the URL of a directory where a JavaDoc
     * collection is stored
     * @return an array of the names of the documented classes
     */
    public static String[] listJavaDocClasses(String baseURL) throws IOException {
        if (baseURL == null) {
            throw new NullPointerException("baseURL");
        }
        if (!baseURL.endsWith("/")) {
            baseURL += '/';
        }
        URL url = new URL(baseURL + "allclasses-noframe.html");
        InputStream in = url.openStream();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            StringWriter w = new StringWriter(32_768);
            ProjectUtilities.copyReader(r, w);
            String html = w.toString();
            int tableStart = html.indexOf("<TABLE");
            int tableEnd = html.lastIndexOf("</TABLE>");
            if (tableStart < 0 || tableEnd < 0) {
                tableStart = html.indexOf("<ul>");
                tableEnd = html.lastIndexOf("</ul>");
            }
            if (tableStart < 0 || tableEnd < 0) {
                throw new AssertionError("incompatible JavaDoc format change");
            }
            html = html.substring(tableStart, tableEnd);
            Matcher m = Pattern.compile("HREF=\"([^\"]+)\\.html\"", Pattern.CASE_INSENSITIVE).matcher(html);
            ArrayList<String> list = new ArrayList<>(1_000);
            while (m.find()) {
                list.add(m.group(1));
            }
            return list.toArray(new String[list.size()]);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * A URL describing the base location where the Strange Eons API
     * documentation is located, if the API documentation core plug-in is
     * installed.
     *
     * @see #listJavaDocClasses
     */
    public static final String STRANGE_EONS_API_BASE = "res:/api/";

    // create a list of java classes and write it to a file
//	public static void main( String[] args ) {
//		try {
//			String base;
//			base = "file:/D:/Java Docs/Java 7/docs/api/";
//			String[] list = listJavaDocClasses( base );
//			Writer w = new OutputStreamWriter( new FileOutputStream( "d:\\j7classes.cpl" ), "utf-8" );
//			for( int i = 0; i < list.length; ++i ) {
//				if( i > 0 ) {
//					w.write( '\n' );
//				}
//				w.write( list[i] );
//			}
//			w.close();
//		} catch( IOException e ) {
//			e.printStackTrace();
//		}
//	}		
}
