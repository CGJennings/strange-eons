package ca.cgjennings.apps.arkham.plugins.typescript;

import java.util.Objects;

/**
 * Encapsulates the contents of a source file that may vary over time.
 * The actual content may come from anywhere, such as memory,
 * the file system, a network connection, etc. Source units are identified
 * by a path. Relative paths will be considered to be relative to the
 * {@link CompilationRoot} that contains them.
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class SourceUnit {
   private final String path;
   private int version = 0;
   private String versionKey;
   private String text;
   Object snapshot;
   
   /**
    * Creates a new, empty source unit.
    * 
    * @param path the identifier, such as file path, that identifies
    * the (real or virtual) location of this source unit
    */
   public SourceUnit(String path) {
       this.path = Objects.requireNonNull(path, "path");
   }
   
   /**
    * Creates a new source unit and updates it with the specified initial text.
    * 
    * @param path the identifier, such as file path, that identifies
    * the (real or virtual) location of this source unit
    * @param initialText the initial source text
    */
   public SourceUnit(String identifier, String initialText) {
       this(identifier);
       update(initialText);
   }   
   
   /**
    * Returns the unique identifier associated with this source unit.
    * @return an identifier, such as a file path
    */
   public final String getPath() {
       return path;
   }
   
   /**
    * Returns the current text of the document.
    * This is the text of the most recent
    * {@linkplain #update(java.lang.String) update}.
    * 
    * @return the document text; may return null if the text is unavailable
    */
    public final String getText() {
        synchronized (this) {
            updateFromSource(text);
            return text;
        }
    }
   
   /**
    * Updates the text of the document.
    * 
    * @param currentText the new file content; may be null if no text is
    * available (for example, if the source was deleted)
    */
   public final void update(String currentText) {
       synchronized (this) {
           if (!Objects.equals(text, currentText)) {
                text = currentText;
                snapshot = null;
                ++version;
                versionKey = null;
           }
       }
   }
   

   /**
    * This method is called whenever the source's version or text is requested,
    * <em>before</em> a result is returned. If the source unit subclass knows that
    * a more up-to-date result is available than the one currently stored
    * in the source unit, it can immediately update the source unit to the
    * latest version.
    * 
    * <p>
    * This can be used to implement features such as lazily-loaded static files
    * or source units tied to files in the file system that update when the
    * file changes. The base class does meaning, which means that the text of
    * the source unit can only change if explicitly updated.
    * 
    * <p>Subclasses which override this method should ensure that it returns
    * as quickly as possible if the source has not changed.
    * 
    * @param possiblyStaleVersion the current, possibly out of date, source
    * unit text; may be null
    */
   protected void updateFromSource(String possiblyStaleVersion) {
   }   
   
   /**
    * Returns a string that uniquely identifies the current version.
    * After each change to the text this will return a different string.
    */
   public final String getVersion() {
       synchronized (this) {
           updateFromSource(text);
           if (versionKey == null) {
               versionKey = Integer.toHexString(version);
           }
           return versionKey;
       }
   }
   
   @Override
   public String toString() {
       return getClass().getSimpleName() + '{' + path + " / v" + getVersion() + '}';
   }
}
