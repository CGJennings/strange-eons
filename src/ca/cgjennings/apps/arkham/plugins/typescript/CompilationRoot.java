package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Encapsulates a collection of script files that should be compiled together as
 * a unit. Files in a compilation unit can reference each other (for example,
 * by importing each other).
 * 
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class CompilationRoot {
    private final TSLanguageServices ts;
    private final Map<String,SourceUnit> map = Collections.synchronizedMap(new HashMap<>());
    private Object langService;
    
    /**
     * Creates a new, empty compilation root.
     */
    public CompilationRoot() {
        this(null);
    }
    
    /**
     * Creates a new, empty compilation root that uses the specified
     * language services instance.
     * 
     * @param ts the TS services instance, or null to use the default shared instance
     */
    public CompilationRoot(TSLanguageServices ts) {
        this.ts = ts == null ? TSLanguageServices.getShared() : ts;
    }

    /**
     * Adds a source file to the compilation root.
     * If an existing source has the same 
     * 
     * @param source the source file to add
     */
    public void add(SourceUnit source) {
        map.put(source.getPath(), source);
    }
    
    /**
     * A convenience method that adds or updates a source file directly from
     * an identifier and script text. If a source unit exists for the identifier,
     * it will be updated. Otherwise a source unit will be created for the
     * identifier using the specified text.
     * 
     * @param identifier the identifier, such as a path, that uniquely identifies
     * the script
     * @param text the content of the script
     * @return the source unit that was created for the script
     */
    public SourceUnit add(String identifier, String text) {
        SourceUnit unit = map.get(identifier);
        if (unit == null) {
            unit = new SourceUnit(identifier, text);
            map.put(identifier, unit);
        } else {
            unit.update(text);
        }
        return unit;
    }
    
    /**
     * Removes a source unit.
     * @param source the unit to remove
     */
    public void remove(SourceUnit source) {
        map.remove(source.getPath());
    }
    
    /**
     * Returns the source unit for the specified identifier, or null if
     * there is no such source unit.
     * 
     * @param fileName the identifier
     * @return the source unit for the identifier
     */
    public SourceUnit get(String fileName) {
        SourceUnit found = map.get(fileName);
        if (found == null && exists(fileName)) {
            found = map.get(fileName);
        }
        return found;
    }
    
    /**
     * Returns an array of the identifiers for the added sources.
     * 
     * @return all identifiers that are part of this compilation root
     */
    public String[] list() {
        synchronized (map) {
            return map.keySet().toArray(new String[0]);
        }
    }

    /**
     * Compiles a source unit.
     * 
     * @param fileName the name of the file to compile
     * @return the compilation output
     */
    public CompiledSource compile(String fileName) {
        CompiledSource result = ts.compile(langService(), fileName);
        if (result != null) {
            result.compilationRoot = this;
        }
        return result;
    }
    
    /**
     * Returns a list of diagnostic messages for a source unit.
     * 
     * @param fileName the file name to get diagnostics for
     * @param includeSyntactic if true, includes syntax-related diagnostics
     * @param includeSemantic if true, includes semantic diagnostics
     * @return a list of diagnostics, possibly empty
     */    
    public List<Diagnostic> getDiagnostics(String fileName, boolean includeSyntactic, boolean includeSemantic) {
        List<Diagnostic> result = ts.getDiagnostics(langService(), fileName, includeSyntactic, includeSemantic);
        if (result == null) {
            result = Collections.emptyList();
        } else {
            result = Collections.unmodifiableList(result);
        }
        return result;
    }
    
    /**
     * Gathers a list of diagnostic messages in the background, passing them
     * to the specified callback once they are available.
     * 
     * @param fileName the file name to get diagnostics for
     * @param includeSyntactic if true, includes syntax-related diagnostics
     * @param includeSemantic if true, includes semantic diagnostics
     * @param callback the callback to invoke with the result
     */    
    public void getDiagnostics(String fileName, boolean includeSyntactic, boolean includeSemantic, Consumer<List<Diagnostic>> callback) {
        ts.getDiagnostics(langService(), fileName, includeSyntactic, includeSemantic, (result) -> {
            if (result == null) {
                result = Collections.emptyList();
            } else {
                result = Collections.unmodifiableList(result);
            }
            callback.accept(result);
        });
    }    
    
    /**
     * Returns code completions for the specified position.
     * 
     * @param fileName the file name to get completions for
     * @param position the position within the file at which completions should be performed
     * @return possible code completions, or null if no completions are available
     */
    public CompletionInfo getCodeCompletions(String fileName, int position) {
        CompletionInfo ci = ts.getCodeCompletions(langService(), fileName, position);
        if (ci == null || ci.entries == null || ci.entries.isEmpty()) {
            return null;
        }
        Collections.sort(ci.entries);
        return ci;
    }
    
    /**
     * Returns additional details about a particular code completion.
     * 
     * @param fileName the file name that the completion was generated for
     * @param position the position that the completion was generated for
     * @param completion the completion to get details for
     * @return additional details
     */
    public CompletionInfo.EntryDetails getCodeCompletionDetails(String fileName, int position, CompletionInfo.Entry completion) {
        CompletionInfo.EntryDetails details = ts.getCodeCompletionDetails(langService(), fileName, position, completion);
        details.kind = completion.kind;
        details.kindModifiers = completion.kindModifiers;
        return details;
    }
    
    /**
     * Returns a file's current navigation tree.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @return the file's current navigation tree
     */
    public NavigationTree getNavigationTree(String fileName) {
        return ts.getNavigationTree(langService(), fileName);
    }
    
    /**
     * Returns a file's current navigation tree.
     * 
     * @param languageService the language service that manages the file
     * @param fileName the file name to get diagnostics for
     * @param callback the callback to invoke with the result
     */
    public void getNavigationTree(String fileName, Consumer<NavigationTree> callback) {
        ts.getNavigationTree(langService(), fileName, callback);
    }
    
    /**
     * Returns a quick overview at the node at the current position, or null.
     * 
     * @param fileName the file name that the completion was generated for
     * @param position the position that the completion was generated for
     * @return information about the node at the position, or null
     */
    public Overview getOverview(String fileName, int position) {
        return ts.getOverview(langService(), fileName, position);
    }
    
    /**
     * Returns the language service instance for this root, creating it
     * if necessary.
     * @return a language service object that delegates to this root
     */
    private Object langService() {
        if (langService == null) {
            langService = ts.createLanguageService(this);
        }
        return langService;
    }
    
    /**
     * Returns a version tag for the specified file.
     * @param fileName the script identifier
     * @return the version tag
     */ 
    public String getVersion(String fileName) {
        SourceUnit unit = map.get(fileName);
        if (unit == null) {
            return null;
        }
        return unit.getVersion();
    }

    /**
     * Returns a current script snapshot for the specified file.
     * @param fileName the script identifier
     * @return a snapshot object, or null if the root does not contain the
     * specified file
     */    
    public Object getSnapshot(String fileName) {
        SourceUnit unit = map.get(fileName);
        if (unit == null) {
            return null;
        }
        if (unit.snapshot == null) {
            unit.snapshot = ts.createSnapshot(unit.getText());
        }
        return unit.snapshot;
    }
    
    private File rootFile;
    
    /**
     * Set a base folder to create suggested identifiers relative to.
     * This can optionally be used by file-based compilation roots
     * to create identifiers using {@link #getSuggestedIdentifier(java.io.File)}.
     * 
     * @param file the root file for composing identifiers, or null
     */
    public void setRootFile(File file) {
        if (!Objects.equals(rootFile, file)) {
            rootFile = file;
            log.log(Level.INFO, "set compilation root file \"{0}\"", rootFile);
        }
    }
    
    /**
     * Suggest an identifier for a file to be added to this compilation root.
     * The base class will make the file relative to the root file, if any,
     * and normalize the directory separator to {@code '/'}. Subclasses may
     * override this to implement other naming schemes.
     * 
     * @param file the file to suggest an identifier for
     * @return the suggested identifier
     */
    public String getSuggestedIdentifier(File file) {
        if (file == null) {
            return "unknown.ts";
        }
        
        File out = file;
        if (rootFile != null) {
            out = ProjectUtilities.makeFileRelativeTo(rootFile, file);
        }
        String id = out.toString().replace(File.separatorChar, '/');
        if (id.startsWith("./")) {
            id = id.substring(2);
        }
        
        log.log(Level.FINE, "assigning identifer \"{0}\" to \"{1}\"", new Object[]{id, file});
        
        return id;
    }
    
    /**
     * Returns true if the specified identifier exists. If the identifier
     * is not already added to this root but a foot file has been set,
     * the base class will test if the identifier exists relative to the
     * root file. If the file exists, it will be added as as a new source
     * unit if {@link #fileCanBeAddedAutomatically(java.io.File) allowed}.
     * 
     * @param identifer the identifer, typically a path relative to this root
     * @return true if the file exists
     */
    public boolean exists(String identifer) {
        synchronized (map) {
            if (map.containsKey(identifer)) {
                return true;
            }
            SourceUnit found = createSourceUnitForReferencedFile(identifer);
            if (found != null) {
                add(found);
            }
            return found != null;
        }
    }
    
    /**
     * Gives the compilation root the opportunity to create a source unit for
     * a referenced file that is not already in the root. May return a suitable
     * source unit for the file, or null to indicate no source is available.
     * 
     * <p>
     * The base class will check if a root file has been set, in which case it
     * will return an {@link EditableSourceUnit} for the path if such a file
     * exists relative to the root.
     * 
     * @param file the file to create a source unit for
     * @return the source unit, or null
     */
    protected SourceUnit createSourceUnitForReferencedFile(String path) {
        if (rootFile != null) {
            File file = new File(rootFile, path);
            log.log(Level.INFO, "checking if referenced exists \"{0}\"", file);
            if (file.exists()) {
                return new EditableSourceUnit(path, file);
            }
        }
        return null;
    }
    
    private static final java.util.logging.Logger log = StrangeEons.log;
}
