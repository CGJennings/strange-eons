
import ca.cgjennings.apps.CommandLineParser;
import ca.cgjennings.apps.arkham.plugins.BundleInstaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * A commandline utility that helps identify unused string table keys.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public final class expiredkeys extends CommandLineParser {

    private expiredkeys(String[] args) throws Throwable {
        base = BundleInstaller.getApplicationLibrary();
        base = new File("C:\\Users\\Chris\\Documents\\Projects\\ArkhamCharacterBuilder\\src");
        if (!base.isDirectory()) {
            base = new File(System.getProperty("user.dir"));
        } else {
            strings = new File(base, "resources/text/interface/eons-text.properties");
        }

        ext = extlist.split("\\s*,\\s*");
        for (int i = 0; i < ext.length; ++i) {
            ext[i] = '.' + ext[i];
        }

        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(strings)) {
            p.load(in);
            Set<String> keys = p.stringPropertyNames();
            process(base, keys);

            System.out.printf("\n\n%d possible unused keys\n", keys.size());
            String[] remaining = keys.toArray(new String[keys.size()]);
            Arrays.sort(remaining);
            for (String k : remaining) {
                System.out.println(k);
            }
        }
    }

    private void process(File dir, Set<String> keys) throws Throwable {
        System.out.print(dir.getPath() + '\r');
        for (File kid : dir.listFiles()) {
            if (kid.isHidden() || kid.getName().startsWith(".")) {
                continue;
            }
            if (kid.isDirectory()) {
                process(kid, keys);
            } else {
                String name = kid.getName();
                for (String e : ext) {
                    if (name.endsWith(e)) {
                        processFile(kid, keys);
                        break;
                    }
                }
            }
        }
    }

    private void processFile(File f, Set<String> keys) throws Throwable {
        try (FileInputStream in = new FileInputStream(f)) {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            String line;
            while ((line = r.readLine()) != null) {
                Iterator<String> ki = keys.iterator();
                while (ki.hasNext()) {
                    String key = ki.next();
                    if (line.contains(key) || line.contains(key.replace('-', '_'))) {
                        ki.remove();
                    }
                }
            }
        }
    }

    private final String[] ext;

    public File base;
    public File strings;
    public String extlist = "java,js,txt,settings,silhouettes,classmap,ajs,tiles,html";

    public static void main(String[] args) {
        try {
            new expiredkeys(args);
        } catch (Throwable t) {
            System.err.println("Fatal error: uncaught exception");
            t.printStackTrace();
        }
    }
}
