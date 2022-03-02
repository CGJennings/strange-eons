package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.util.Objects;

/**
 * Encapsulates the contents of a source file that may vary over time.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class SourceUnit {
   private final String identifier;
   private long version = 0L;
   private String versionKey;
   private String text;
   Object snapshot;
   
   /**
    * Creates a new, empty source unit.
    * 
    * @param identifier the identifier, such as file path, that identifies
    * this source unit
    */
   public SourceUnit(String identifier) {
       this.identifier = Objects.requireNonNull(identifier, "identifier");
   }
   
   /**
    * Creates a new source unit and updates it with the specified initial text.
    * 
    * @param identifier the identifier, such as file path, that identifies
    * this source unit
    * @param initialText the initial file source text
    */
   public SourceUnit(String identifier, String initialText) {
       this(identifier);
       update(initialText);
   }   
   
   /**
    * Returns the unique identifier associated with this source unit.
    * @return an identifier, such as a file path
    */
   public String getIdentifier() {
       return identifier;
   }

   /**
    * Called when the text is requested and no update has been performed,
    * so no text has ever been set.
    * Subclasses can override this to implement source units that are
    * loaded on demand by calling {@link #update}.
    */
   protected void performInitialUpdate() {
   }
   
   /**
    * Returns the current text of the document.
    * 
    * @return the document text
    */
   public String getText() {
       synchronized (this) {
        if (text == null) {
            performInitialUpdate();
            if (text == null) {
                StrangeEons.log.warning("document requested but never updated: " + identifier);
                text = "";
            }
        }
        return text;
       }
   }
   
   /**
    * Updates the text of the document.
    * @param currentText the new file content
    */
   public void update(String currentText) {
       synchronized (this) {
           if (text == null || !text.equals(currentText)) {
                text = Objects.requireNonNull(currentText, "currentText");
                snapshot = null;
                long timestamp = System.nanoTime();
                if (timestamp <= version) {
                    timestamp = version + 1L;
                }
                version = timestamp;
                versionKey = null;
           }
       }
   }
   
   /**
    * Returns a string that uniquely identifies the current version.
    * After each change to the text this will return a different string.
    */
   public String getVersion() {
       synchronized (this) {
           if (versionKey == null) {
               versionKey = Long.toHexString(version);
           }
           return versionKey;
       }
   }
}
