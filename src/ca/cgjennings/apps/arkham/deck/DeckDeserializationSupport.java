package ca.cgjennings.apps.arkham.deck;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.component.GameComponent;
import ca.cgjennings.apps.arkham.deck.item.CardFace;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.platform.PlatformSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import static resources.Language.string;
import resources.ResourceKit;

/**
 * A helper class used when deserializing decks to track linked files that
 * cannot be found (and the new files that replace them). As long as you use
 * {@link ResourceKit#getGameComponentFromFile ResourceKit.getGameComponentFromFile}
 * to read game components, deck deserialization will be handled for you and you
 * needn't worry about this class.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class DeckDeserializationSupport {

    private DeckDeserializationSupport() {
    }

    /**
     * Returns the shared instance of the helper. Because it is shared, only one
     * deck can be (de)serialized at a time.
     *
     * @return the shared helper instance
     */
    public static DeckDeserializationSupport getShared() {
        synchronized (DeckDeserializationSupport.class) {
            if (shared == null) {
                shared = new DeckDeserializationSupport();
            }
        }
        return shared;
    }

    /**
     * Returns <code>true</code> if there is currently a deserialization session
     * active.
     *
     * @return <code>true</code> if the shared instance is in use
     */
    public synchronized boolean isActive() {
        return active;
    }

    /**
     * Sets the folder that will be used as a last-ditch fallback to find a
     * missing file linked to by the deck. This is typically set to the parent
     * folder of the deck file being read on the local system, if known.
     *
     * @param folder the folder to search
     * @throws IllegalArgumentException if the folder exists but is not a folder
     */
    public void setDefaultFallbackFolder(File folder) {
        if (folder != null && folder.exists() && !folder.isDirectory()) {
            throw new IllegalArgumentException("not a directory: " + folder);
        }
        fallbackFolder = folder;
    }

    /**
     * Returns the folder that will be used as a last-ditch fallback to find a
     * missing file linked to by the deck. May be <code>null</code>.
     *
     * @return the fallback folder, or <code>null</code>
     */
    public File getDefaultFallbackFolder() {
        return fallbackFolder;
    }

    /**
     * Begins a new deserialization session. Called from within
     * {@link Deck#readObject(java.io.ObjectInputStream)}.
     *
     * @param relativePathBase the relative path base for interpreting missing
     * file locations; typically the parent of the file where the deck was saved
     */
    synchronized void beginDeserialization(String relativePathBase) {
        if (active) {
            throw new IllegalStateException("deck deserialization session already active");
        }
        active = true;
        componentFromOriginalPath = new LinkedHashMap<>();
        replacementPaths = new LinkedHashMap<>();
        this.relativePathBase = relativePathBase;
        replacedFile = false;
    }

    /**
     * After loading a deck component with an identifier, the original
     * identifier can be passed to this method to see if it has changed to a new
     * location.
     *
     * @param path the original path
     * @return the path that replaced it, or <code>null</code> if not replaced
     */
    String getReplacementPath(String path) {
        return replacementPaths.get(path);
    }

    /**
     * Called from within {@link CardFace}s during deserialization to get a game
     * component from the face's identifier.
     *
     * @param path the identifier that locates the original file
     * @param name the component name to display to the user as a hint
     * @return the game component for the identifier, or <code>null</code>
     * @throws IOException if an I/O error occurs while reading the file
     */
    public GameComponent findGameComponent(String path, String name) throws IOException {
        GameComponent g = componentFromOriginalPath.get(path);

        if (g == null) {
            // hack the source path for better cross-platform compatibility
            if (!PlatformSupport.PLATFORM_IS_WINDOWS) {
                path = path.replace('\\', File.separatorChar);
            }
            File f = new File(path);

            // if a relative path, use the base path to reconstruct a full path
            if (!f.isAbsolute() && relativePathBase != null) {
                f = new File(relativePathBase, path);
            }

            if (!f.exists() && fallbackFolder != null) {
                // the deck's original path is known;
                // try making the card relative to the original deck path,
                // then looking it up from the new deck location
                if (relativePathBase != null) {
                    File relative = ProjectUtilities.makeFileRelativeTo(new File(relativePathBase), f);
                    relative = new File(fallbackFolder, relative.getPath());
                    if (relative.exists()) {
                        f = relative;
                        replacedFile = true;
                        replacementPaths.put(path, f.getAbsolutePath());
                    }
                }

                // that didn't work, try looking in the same folder the deck is in
                if (!f.exists()) {
                    File fallback = new File(fallbackFolder, f.getName());
                    if (fallback.exists()) {
                        f = fallback;
                        replacedFile = true;
                        replacementPaths.put(path, f.getAbsolutePath());
                    }
                }
            }

            while (!f.exists()) {
                DeckDeserializationDialog d = new DeckDeserializationDialog(StrangeEons.getWindow(), true, name, path);
                d.setVisible(true);
                f = d.getReplacementFile();

                if (f == null) {
                    throw new FileNotFoundException(path);
                }

                replacementPaths.put(path, f.getAbsolutePath());
                replacedFile = true;
            }
            g = ResourceKit.getGameComponentFromFile(f);
            if (g == null) {
                throw new IOException(string("de-err-fetch-card", name));
            }
//			if( g.getSheets() == null ) {
            g.createDefaultSheets();
//			}
            componentFromOriginalPath.put(path, g);
        } else {
            String replacedPath = replacementPaths.get(path);
            if (replacedPath != null) {
                replacedFile = true;
            }
        }

        return g;
    }

    /**
     * Called from within deck deserialization to end the session, making the
     * list of replaced components available via {@code getComponentList()}.
     *
     * @return {@code true} if one or more of the paths saved in the deck were
     * replaced with correct paths
     */
    synchronized boolean endDeserialization() {
//		// get all of the paths actually used, removing duplicates
//		Set<String> usedPaths = new HashSet<>();
//		for( String s : componentFromOriginalPath.keySet() ) {
//			String replacement = replacementPaths.get( s );
//			if( replacement != null ) {
//				s = replacement;
//			}
//			usedPaths.add( s );
//		}

        // clear the objects required for serialization so they can be GC'd
        componentFromOriginalPath = null;
        replacementPaths = null;

        active = false;

        return replacedFile;
    }

    private static DeckDeserializationSupport shared;

    private boolean active;
    private HashMap<String, GameComponent> componentFromOriginalPath;
    private HashMap<String, String> replacementPaths;
    private String relativePathBase;
    private boolean replacedFile;
    private File fallbackFolder;
}
