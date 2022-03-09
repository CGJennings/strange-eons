package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.logging.Level;
import resources.Language;

/**
 * A source unit whose content is linked to a file. The source unit
 * path does not need to match the actual file. 
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class FileSourceUnit extends SourceUnit {
    private long lastModified = -1L;
    private final File file;

    public FileSourceUnit(String identifier, File loadFrom) {
        super(identifier);
        file = Objects.requireNonNull(loadFrom, "loadFrom");
        lastModified = file.lastModified();
    }
    
    /**
     * Returns the file that this source unit is updated from.
     * @return the non-null file that backs this source unit
     */
    public final File getFile() {
        return file;
    }
    
    @Override
    protected void updateFromSource(String possiblyStaleVersion) {
        final long modTime = file.lastModified();
        if (modTime != lastModified) {
            String updatedText = null;
            try {
                updatedText = ProjectUtilities.getFileText(file, TextEncoding.SOURCE_CODE);
            } catch (FileNotFoundException fnf) {
                // an expected case, file was deleted or does not exist yet;
                // will update the text with "null" to indicate
            } catch (IOException ioex) {
                // problem reading file (bad media, no permissionsm, etc.)
                // update with "null", but also print an error message
                PrintWriter err = ScriptMonkey.getSharedConsole().getErrorWriter();
                err.println(Language.string("rk-err-reading-script", file));
                ScriptMonkey.scriptError(ioex);
                err.flush();
                StrangeEons.log.log(Level.WARNING, "unable to read " + file, ioex);
            }
            lastModified = modTime;
            update(updatedText);
        }
    }
}
