package resources.libraries.legacy_docs;

import ca.cgjennings.io.EscapedLineReader;
import ca.cgjennings.ui.textedit.CSSStyler;
import ca.cgjennings.ui.textedit.tokenizers.JavaScriptTokenizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

/**
 * Extract documentation from library sources.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class MakeDocs {

    private MakeDocs() {
    }

    public static void main(String[] args) {

//		args = new String[] {
//			"C:\\Documents and Settings\\Chris\\My Documents\\Projects\\ArkhamCharacterBuilder\\build\\classes\\resources\\libraries",
//			System.getenv( "TEMP" )
//		};
        if (args.length != 2) {
            System.out.println("Convert .js library source files into simple documentation");
            System.out.println("Usage: MakeDocs sourcedir destdir");
            System.exit(0);
        }

        try {
            convertDirectory(new File(args[0]), new File(args[1]));
            try (Writer w = createWriter(new File(args[1], "makedocs.css"))) {
                writeCSS(w, true);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void convertDirectory(File source, File dest) throws IOException {
        if (!dest.exists()) {
            if (!dest.mkdirs()) {
                throw new IOException("unable to create destination directory " + dest);
            }
        }

        if (!source.isDirectory()) {
            throw new IOException("source is not a directory " + source);
        }

        try (Writer index = createWriter(new File(dest, "index.html"))) {
            writeHTMLHeader(index, "Script Library Documentation", true);

            File[] files = source.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    // not completely implemented (index.html file)
                    // convertDirectory( source, new File( dest, source.getName() ) );
                } else if (f.getName().endsWith(".js") || f.getName().endsWith(".doc")) {
                    String title = convertFile(f, dest);
                    index.write("<a href='" + title + ".html'>" + title + "</a><br>\n");
                }
            }

            writeHTMLFooter(index);
        }
    }

    protected static EscapedLineReader createReader(File f) throws IOException {
        return new EscapedLineReader(new FileInputStream(f), "utf-8");
    }

    protected static Writer createWriter(File d) throws IOException {
        return new OutputStreamWriter(new FileOutputStream(d), "utf-8");
    }

    public static String convertFile(File source, File destdir) {
        System.out.println("Converting: " + source);
        EscapedLineReader in = null;
        Writer out = null;

        String title = source.getName();
        int ext = title.lastIndexOf('.');
        if (ext >= 0) {
            title = title.substring(0, ext);
        }

        File dest = new File(destdir, title + ".html");

        try {
            in = createReader(source);
            out = createWriter(dest);
            createHelpFile(title, in, out, false, true);
        } catch (IOException e) {
            System.err.format("Error while converting %s to %s:\n", source, dest);
            System.err.println(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.err.println("Warning: error closing " + dest);
                    System.err.println(e);
                }
            }
        }

        return title;
    }

    public static String createHelpFile(String title, String source) {
        return createHelpFile(title, source, true);
    }

    public static String createHelpFile(String title, String source, boolean writeTOC) {
        EscapedLineReader in = new EscapedLineReader(new StringReader(source));
        StringWriter out = new StringWriter();
        try {
            createHelpFile(title, in, out, true, writeTOC);
        } catch (IOException e) {
            throw new AssertionError("unexpected I/O exception: " + e);
        }
        return out.toString();
    }

    public static Writer beginCustomHelpPage(String title) {
        StringWriter out = new StringWriter();
        try {
            writeHTMLHeader(out, title, true);
        } catch (IOException e) {
            throw new AssertionError("unexpected I/O exception: " + e);
        }
        return out;
    }

    public static String endCustomHelpPage(Writer w) {
        try {
            writeHTMLFooter(w);
        } catch (IOException e) {
            throw new AssertionError("unexpected I/O exception: " + e);
        }
        return w.toString();
    }

    private static CSSStyler styler = new CSSStyler(new JavaScriptTokenizer());

    protected static void createHelpFile(String title, EscapedLineReader in, Writer out, boolean isInternal, boolean writeTOC) throws IOException {
        writeHTMLHeader(out, title, isInternal);

        Writer trueOut = out;
        Writer toc = new StringWriter();
        out = new StringWriter();

        Writer preStack = null;
        boolean waitingForTitle = true;
        boolean inDoc = false;
        boolean inParamList = false;
        boolean inPre = false;
        String line = null;
        String ciline = null; // case-insensitive version of line
        while ((line = in.readNonemptyLine()) != null) {
            if (inDoc) {
                while (line.endsWith("...") && !inPre) {
                    line = line.substring(0, line.length() - 3);
                    String nextLine = in.readNonemptyLine();
                    if (nextLine == null) {
                        break;
                    }
                    nextLine = nextLine.trim();
                    if (nextLine.startsWith("*/")) {
                        break;
                    }
                    if (nextLine.startsWith("*")) {
                        nextLine = nextLine.substring(1);
                    }
                    line += nextLine;
                }

                ciline = line.trim().toLowerCase(Locale.CANADA);
                if (ciline.startsWith("*")) {
                    ciline = ciline.substring(1).trim();
                }

                if (inParamList && !line.contains(" : ")) {
                    inParamList = false;
                    out.write("</table>\n");
                }

                if (!inPre) {
                    line = line.trim();
                }

                // this hack finds <pre> blocks so we can disable
                // <p> generation on blank lines
                if (ciline.equals("<pre>")) {
                    inPre = true;
                    preStack = out;
                    out = new StringWriter();
                    continue;
                } else if (inPre && ciline.equals("</pre>")) {
                    inPre = false;
                    StringWriter buff = (StringWriter) out;
                    out = preStack;
                    String code = buff.toString();
                    if (code.endsWith("\n")) {
                        code = code.substring(0, code.length() - 1);
                    }
                    out.write("<pre>" + styler.style(code) + "</pre>");
                    continue;
                }

                if (line.startsWith("*/")) {
                    out.write("\n");
                    inDoc = false;
                } else {
                    if (line.startsWith("*")) {
                        line = line.substring(1);
                    }
                    if (waitingForTitle && line.length() > 0) {
                        line = line.trim();
                        String tag = "h2>";
                        int dot = line.indexOf('.');
                        // class names get <h2>
                        // not a class name:
                        //   - anything with a dot
                        //   - if the first letter is lowercase and there is a '(' (global fn)
                        if (dot >= 0 || (line.length() > 0 && Character.isLowerCase(line.charAt(0)) && (line.indexOf('(') >= 0))) {
                            tag = "h3>";
                        }

                        // do [static] enforcement
                        boolean hasProto = line.indexOf(".prototype.") >= 0;
                        boolean hasStatic = line.indexOf("[static]") >= 0;
                        if (hasProto && hasStatic) {
                            if (!isInternal) {
                                System.out.printf("Signature has prototype and [static]\n%s\n\n", line);
                            }
                        }
//						else if( !hasProto && !hasStatic ) {
//							int space = line.indexOf( ' ' );
//							int paren = line.indexOf( '(' );
//							if( space < 0 ) space = Integer.MAX_VALUE;
//							if( paren < 0 ) paren = Integer.MAX_VALUE;
//							if( dot > 0 && dot < paren && dot < space && dot < line.length()-1 && !Character.isWhitespace( line.charAt(dot+1) ) ) {
//								line = line.substring( 0, dot ) + ".prototype" + line.substring( dot );
//								if( !isInternal ) System.out.printf( "Inserting prototype in non-[static] signature\n%s\n", line );
//							}
//						}

                        // generate TOC link name
                        String name = line.replaceAll("\\s+", "")
                                .replaceAll("(?i)\\[static\\]", "")
                                .replaceAll("(?i)\\[ctor\\]", "")
                                .replaceAll("(?i)\\[class\\]", "")
                                .replaceAll("(?i)\\[interface\\]", "")
                                .replaceAll("(?i)\\[readonly\\]", "")
                                .replaceAll("(?i)\\[writeonly\\]", "")
                                .replace("\"", "").replace("'", "");
                        name = name.replaceAll("[^a-zA-Z0-9]", "");

                        String taggedLine = line.replaceAll("(?i)\\[static\\]", "<span class='STATIC'>static</span>")
                                .replaceAll("(?i)\\[ctor\\]", "<span class='CLASS'>constructor</span>")
                                .replaceAll("(?i)\\[class\\]", "<span class='CLASS'>class</span>")
                                .replaceAll("(?i)\\[interface\\]", "<span class='CLASS'>class</span>")
                                .replaceAll("(?i)\\[readonly\\]", "<span class='RESTRICTED'>read-only</span>")
                                .replaceAll("(?i)\\[writeonly\\]", "<span class='RESTRICTED'>write-only</span>")
                                .replace("[", "<span class='OPTIONAL'>[")
                                .replace("]", "]</span>")
                                .replace("(", "(<span class='PARAMLIST'>")
                                .replace(")", "</span>)");

                        out.write("<" + tag + "<a name='" + name + "'>" + taggedLine + "</a></" + tag + "\n");
                        toc.write("<a href='#" + name + "'>" + taggedLine + "</a><br>\n");

                        waitingForTitle = false;
                    } else if (line.length() == 0 && !inPre) {
                        out.write("<p>");
                    } else if (line.contains(" : ")) {
                        if (!inParamList) {
                            inParamList = true;
                            out.write("<table border=0 cellpadding=2 cellspacing=0 class='arglist'>");
                        }
                        int i = line.indexOf(" : ");
                        out.write("<tr><td class='arg' valign='top'><tt>" + line.substring(0, i) + " </tt></td><td class='argdesc' valign='top'>" + line.substring(i + 3) + "</td></tr>\n");
                    } else {
                        out.write(line);
                    }
                    out.write('\n');
                    if (line.endsWith("*/")) {
                        if (inPre) {
                            out.write("</pre>\n");
                        }
                        if (inParamList) {
                            out.write("</table>\n");
                        }
                        out.write("\n");
                        inDoc = false;
                    }
                }
            } else {
                if (line.startsWith("/**")) {
                    inDoc = true;
                    waitingForTitle = true;
                    out.write("<p>\n");
                }
            }
        }

        if (writeTOC) {
            trueOut.write("<div class='toc'>\n");
            trueOut.write(toc.toString());
            trueOut.write("</div>\n");
        }
        trueOut.write(out.toString());
        out = trueOut;

        out.write("<p style='margin-top: 20pt;'>");
        if (!isInternal) {
            out.write("<a href='index.html'>Index</a>&nbsp;&nbsp;&nbsp;&nbsp;");
        }

        if (writeTOC) {
            out.write("<a href='#top'>Contents</a>");
        }

        writeHTMLFooter(out);
    }

    protected static void writeHTMLHeader(Writer w, String title, boolean isInternal) throws IOException {
        w.write("<!DOCTYPE html><html>\n<head>\n");
        if (!isInternal) {
            w.write("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>\n");
        }
        w.write("<title>Script Library Documentation " + title + "</title>\n");
        if (isInternal) {
            w.write("<style>\n");
        }
        writeCSS(w, isInternal);
        if (isInternal) {
            w.write("</style>\n");
        }
        w.write("</head>\n<body>\n");
        if (isInternal) {
            w.write("<p><a href='./source'>Source code</a></p>\n");
        }
        w.write("<a name='top'><h1>" + title + "</h1></a>\n");
    }

    protected static void writeHTMLFooter(Writer w) throws IOException {
        w.write("</body>\n</html>");
    }

    protected static void writeCSS(Writer w, boolean isInternal) throws IOException {
        if (isInternal) {
            InputStream in = MakeDocs.class.getResourceAsStream("makedocs.css");
            if (in == null) {
                throw new IOException("Can't read style sheet");
            }
            InputStreamReader r = new InputStreamReader(in, "utf-8");
            try {
                int read = 0;
                char[] buff = new char[512];
                while ((read = r.read(buff)) > -1) {
                    w.write(buff, 0, read);
                }
            } finally {
                if (r != null) {
                    r.close();
                }
            }
        } else {
            w.write("<link rel=StyleSheet type='text/css' href='./makedocs.css'>\n");
        }
    }
}
