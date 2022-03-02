package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.TextEncoding;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

/**
 * A source unit whose content is linked to a file. This should be
 * removed if the file is deleted, and replaced by 
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class FileSourceUnit extends SourceUnit {
    private long lastModified;
    private File file;

    public FileSourceUnit(String identifier, File loadFrom) {
        super(identifier);
        file = Objects.requireNonNull(loadFrom, "loadFrom");
        lastModified = file.lastModified();
    }
    
    @Override
    protected void performInitialUpdate() {
        update();
    }    
    
    private void update() {
        if (file.lastModified() > lastModified) {
            try {
                String text = ProjectUtilities.getFileText(file, TextEncoding.SOURCE_CODE);
                update(text);
            } catch (IOException ioex) {
                StrangeEons.log.log(Level.WARNING, "unable to read " + file, ioex);
            }
        }
    }
    
    public String getVersion() {
        update();
        return super.getVersion();
    }
}
