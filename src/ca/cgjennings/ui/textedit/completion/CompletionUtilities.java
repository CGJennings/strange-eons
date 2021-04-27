package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.AbstractStrangeEonsEditor;
import ca.cgjennings.apps.arkham.StrangeEons;
import ca.cgjennings.apps.arkham.editors.CodeEditor;
import ca.cgjennings.apps.arkham.editors.NavigationPoint;
import ca.cgjennings.apps.arkham.plugins.LibraryRegistry;
import ca.cgjennings.apps.arkham.project.Member;
import ca.cgjennings.apps.arkham.project.Project;
import ca.cgjennings.apps.arkham.project.ProjectUtilities;
import ca.cgjennings.apps.arkham.project.Task;
import ca.cgjennings.apps.arkham.project.TaskGroup;
import ca.cgjennings.spelling.SpellingChecker;
import ca.cgjennings.spelling.dict.TernaryTreeList;
import ca.cgjennings.spelling.dict.WordList;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import gamedata.Game;
import java.awt.Component;
import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.Collator;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Icon;
import resources.Language;
import resources.Settings;

/**
 * Utility methods useful during code completion. These methods have been
 * offloaded into this utility class to keep the main code completion classes
 * easier to read and maintain.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
final class CompletionUtilities {

    private CompletionUtilities() {
    }

    /**
     * If the specified editor is in a {@link CodeEditor} tab, and there is a
     * project open, and the tab is editing a file in that project, returns the
     * project {@link Member} for the file. Otherwise, returns
     * <code>null</code>.
     *
     * @param editor the editor component
     * @return the project member being edited, or <code>null</code>
     */
    public static Member getEditedProjectMemberOrNull(JSourceCodeEditor editor) {
        Project p = StrangeEons.getOpenProject();
        if (p == null) {
            return null;
        }
        Component c = editor;
        while (c != null) {
            c = c.getParent();
            if (c instanceof AbstractStrangeEonsEditor) {
                File f = ((AbstractStrangeEonsEditor) c).getFile();
                return p.findMember(f);
            }
        }
        return null;
    }

    /**
     * Returns whether a potential completion starts with a given prefix. This
     * is essentially a case insensitive version of
     * {@link String#startsWith(java.lang.String)}.
     *
     * @param prefix the prefix to test for
     * @param identifier the identifier to match against
     * @return <code>true</code> if the start of the identifier matches the
     * prefix, without considering letter case
     */
    public static boolean match(String prefix, String identifier) {
        int len = prefix.length();
        if (identifier.length() < len) {
            return false;
        }

        for (int i = 0; i < len; ++i) {
            char pc = prefix.charAt(i);
            char ic = identifier.charAt(i);
            if (pc != ic) {
                if (Character.toLowerCase(pc) != Character.toLowerCase(ic)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Adds potential script library names that match the prefix context to a
     * set of code alternatives.
     *
     * @param ed the editor
     * @param set the set to add to
     * @param context the context containing the prefix to match
     */
    public static void addLibraryNameAlternatives(JSourceCodeEditor ed, Set<CodeAlternative> set, ScriptCompletionContext context) {
        String prefix = context.getPrefix();
        char quote = '\'';
        if (prefix.startsWith("\"")) {
            quote = '"';
        }
        HashSet<String> libs = new HashSet<>();
        // add standard libraries
        Collections.addAll(libs, LibraryRegistry.getLibraries());

        // look for possible libraries in the open project; if we can figure out
        // that we are in an editor for a project file, then limit the search to
        // that file's task folder
        Member m = getEditedProjectMemberOrNull(ed);
        Task t = m == null ? null : m.getTask();

        if (t == null) {
            addLibraryNameAlternativesImpl(StrangeEons.getOpenProject(), libs);
        } else {
            for (Member kid : t.getChildren()) {
                addLibraryNameAlternativesImpl("/", kid, libs, m);
            }
        }
        for (String library : libs) {
            library = quote + library + quote;
            if (match(prefix, library)) {
                DefaultCodeAlternative alt = new DefaultCodeAlternative(
                        ed, library, library, "Library", NavigationPoint.ICON_HEXAGON,
                        0, false, prefix.length()
                );
                set.add(alt);
            }
        }
    }

    private static void addLibraryNameAlternativesImpl(TaskGroup tg, HashSet<String> libs) {
        if (tg != null) {
            for (Member m : tg.getChildren()) {
                if (m instanceof TaskGroup) {
                    addLibraryNameAlternativesImpl((TaskGroup) m, libs);
                } else if (m instanceof Task) {
                    for (Member kid : m.getChildren()) {
                        addLibraryNameAlternativesImpl("/", kid, libs, null);
                    }
                }
            }
        }
    }

    private static void addLibraryNameAlternativesImpl(String root, Member m, HashSet<String> libs, Member childToIgnore) {
        if (m.hasChildren()) {
            for (Member kid : m) {
                addLibraryNameAlternativesImpl(root + m.getName() + '/', kid, libs, childToIgnore);
            }
        } else if (ProjectUtilities.matchExtension(m, "js")) {
            if (!m.equals(childToIgnore)) {
                String lib = root + m.getName();
                if (lib.startsWith("/resources/")) {
                    lib = "res://" + lib.substring("/resources/".length());
                } else {
                    lib = "res:/" + lib;
                }
                libs.add(lib);
            }
        }
    }

    /**
     * Adds potential $-notation keys that match the prefix context to a set of
     * code alternatives.
     *
     * @param ed the editor
     * @param set the set to add to
     * @param context the context containing the prefix to match
     */
    public static void addSettingAlternatives(JSourceCodeEditor ed, Set<CodeAlternative> set, ScriptCompletionContext context) {
        for (Game g : Game.getGames(true)) {
            addSettingAlternativesImpl(g.getMasterSettings(), false, g.getCode(), ed, set, context);
        }
        addSettingAlternativesImpl(Settings.getUser(), true, null, ed, set, context);
    }

    private static void addSettingAlternativesImpl(Settings s, boolean deep, String suffix, JSourceCodeEditor ed, Set<CodeAlternative> set, ScriptCompletionContext context) {
        final String prefix = context.getPrefix();
        Set<String> keys = deep ? s.getVisibleKeySet() : s.getKeySet();
        String category = "Setting";
        if (suffix != null) {
            category += " (" + suffix + ')';
        }
        for (String key : keys) {
            key = '$' + key;
            if (match(prefix, key)) {
                set.add(new DefaultCodeAlternative(
                        ed, key, key, category, NavigationPoint.ICON_TRIANGLE,
                        0, false, prefix.length()
                ));
            }
        }
    }

    /**
     * Adds alternatives for {@literal @}-notation or #-notation variables that
     * match the current prefix string to a set of code alternatives.
     *
     * @param ed the editor
     * @param set the set to add to
     * @param context the context containing the prefix to match
     * @param ui if <code>true</code>, adds interface string alternatives;
     * otherwise adds game string alternatives
     */
    public static void addStringAlternatives(JSourceCodeEditor ed, Set<CodeAlternative> set, ScriptCompletionContext context, boolean ui) {
        final String prefix = context.getPrefix();
        final int prefixLen = prefix.length();
        final char PREFIX_CHAR = ui ? '@' : '#';

        Language language = ui ? Language.getInterface() : Language.getGame();
        for (String key : language.keySet()) {
            String prefixedKey = PREFIX_CHAR + key;
            if (match(prefix, prefixedKey)) {
                set.add(new DefaultCodeAlternative(
                        ed, prefixedKey, prefixedKey, String.format("%-20.20s", language.get(key)),
                        NavigationPoint.ICON_TRIANGLE, 0, false, prefixLen
                ));
            }
        }
    }

    /**
     * Adds matching package names, and optionally class names, from the
     * {@link APIDatabase} that match the prefix context to a set of code
     * alternatives. You must have already determined the parent node that
     * contains the packages or classes to be added.
     *
     * @param ed the editor
     * @param set the set to add to
     * @param parent the API node containing nodes to match
     * @param context the context containing the prefix to match
     * @param includeClasses whether to include classes or just packages
     */
    public static void addPackages(JSourceCodeEditor ed, Set<CodeAlternative> set, APINode parent, ScriptCompletionContext context, boolean includeClasses) {
        String prefix = context.getPrefix();
        for (APINode childNode : parent.children()) {
            if (includeClasses || childNode instanceof PackageNode) {
                if (match(prefix, childNode.getName())) {
                    addClsPkg(ed, set, childNode, context);
                }
            }
        }
    }

    /**
     * Adds package names, and optionally class names, that match the prefix
     * context to a set of code alternatives. The parent package node is
     * determined from the context.
     *
     * @param ed the editor
     * @param set the set to add to
     * @param context the context containing the prefix to match
     * @param includeClasses whether to include classes or just packages
     */
    public static void addPackages(JSourceCodeEditor ed, Set<CodeAlternative> set, ScriptCompletionContext context, boolean includeClasses) {
        APINode parent = APIDatabase.getPackageRoot();
        int nodes = context.size() - 1;
        for (int i = 0; i < nodes && parent != null; ++i) {
            String id = context.getIdentifier(i);
            if (i == 0 && "Packages".equals(id)) {
                continue;
            }
            parent = parent.find(id);
        }
        if (includeClasses || !(parent instanceof ClassNode)) {
            addPackages(ed, set, parent, context, includeClasses);
        }
        if (nodes == 0 && match(context.getPrefix(), "Packages")) {
            addClsPkg(ed, set, PACKAGES, context);
        }
    }

    private static void addClsPkg(JSourceCodeEditor ed, Set<CodeAlternative> set, APINode node, ScriptCompletionContext context) {
        boolean isClass = node instanceof ClassNode;

        // decide if the completion should chain to another completion
        boolean chained;
        if (isClass) {
            // if we are adding a class node, importClass is done
            // but new operator should chain to constructor
            chained = context.insideNewOperator();
        } else {
            // we basically always chain packages:
            // if importClass or new, packages are not a valid endpoint
            // if importPackage, the majority of the time you'll want
            // to go deeper into the tree; the one caveat is that on
            // importPackage we should not chain if there are no subpackages
            // since we don't want the extra '.' at the end
            chained = true;
            if (node != PACKAGES && context.insideImportPackage()) {
                boolean foundPkg = false;
                for (APINode n : node.children()) {
                    if (n instanceof PackageNode) {
                        foundPkg = true;
                        break;
                    }
                }
                chained = foundPkg;
            }
        }

        String insert = chained ? node.getName() + '.' : node.getName();
        DefaultCodeAlternative alt = new DefaultCodeAlternative(
                ed, insert, node.getName(),
                isClass ? "Class" : "Package",
                isClass ? NavigationPoint.ICON_CLUSTER : NavigationPoint.ICON_PACKAGE,
                0, chained, context.getPrefix().length()
        );
        set.add(alt);
    }

    /**
     * Script code counterpart of the {@link Character} class method.
     *
     * @param ch character to test
     * @return whether it is a valid character for the start of an identifier
     */
    public static boolean isScriptIdentifierStart(char ch) {
        return ch == '@' || ch == '#' || Character.isJavaIdentifierStart(ch);
    }

    /**
     * Script code counterpart of the {@link Character} class method.
     *
     * @param ch character to test
     * @return whether it is a valid character for part of an identifier
     */
    public static boolean isScriptIdentifierPart(char ch) {
        return ch == '@' || ch == '#' || Character.isJavaIdentifierPart(ch);
    }

    /**
     * An API node representing the special "Packages" identifier.
     */
    private static final PackageNode PACKAGES = new PackageNode("Packages");

    /**
     * Adds words from a spelling dictionary that match the suffix to a set of
     * code alternatives.
     *
     * @param ed the editor
     * @param set the set code alternatives to add matches to
     * @param prefix the word to complete (such as the prefix of a completion
     * context)
     * @param checker the spelling checker to use for words; <code>null</code>
     * for default
     */
    public static void addWords(JSourceCodeEditor ed, Set<CodeAlternative> set, String prefix, SpellingChecker checker) {
        int len = prefix.length();
        Set<String> results = generateWordCompletions(prefix, checker, true);
        for (String s : results) {
            set.add(new DefaultCodeAlternative(ed, s, NavigationPoint.ICON_CIRCLE_SMALL, len));
        }
    }

    /**
     * Generate a list of word completions for a prefix of a word. If the prefix
     * string is empty, an empty collection is returned since otherwise it would
     * contain the spelling checker's entire dictionary. If the prefix is short,
     * the number of results returned may be limited. Thus, a longer prefix may
     * in some cases return more results than a shorter prefix.
     *
     * @param prefix the word to complete (such as the prefix of a completion
     * context)
     * @param checker the spelling checker to use for words; <code>null</code>
     * for default
     * @param includeUserDictionaries if <code>true</code>, then the method may
     * include user dictionaries (such as learned words); this may greatly
     * increase the time required by the function to return because user
     * dictionaries are not optimized for search
     * @return a collection of dictionary words that match the prefix
     */
    public static Set<String> generateWordCompletions(String prefix, SpellingChecker checker, boolean includeUserDictionaries) {
        int len = prefix.length();
        if (len == 0) {
            // too many words to add suggestions for empty prefix
            return Collections.emptySet();
        }
        if (checker == null) {
            checker = SpellingChecker.getSharedInstance();
        }
        Locale loc = checker.getLocale();

        TreeSet<String> results = new TreeSet<>(Collator.getInstance(loc));
        processWordList(prefix, results, checker.getMainList(), loc);
        if (includeUserDictionaries) {
            processWordList(prefix, results, checker.getUserList(), loc);
            processWordList(prefix, results, checker.getIgnoreList(), loc);
        }
        return results;
    }

    private static void processWordList(String prefix, TreeSet<String> results, WordList wl, Locale loc) {
        int inSize = results.size();
        if (wl instanceof TernaryTreeList) {
            TernaryTreeList ttl = (TernaryTreeList) wl;
            processTernaryList(prefix, results, ttl);
            int outSize = results.size() - inSize;
            if (outSize == 0 && Character.isUpperCase(prefix.charAt(0))) {
                processTernaryList(prefix.toLowerCase(loc), results, ttl);
            }
        } else {
            List<String> words = wl.getWords();
            for (String w : words) {
                if (w.startsWith(prefix)) {
                    results.add(w);
                }
            }
            int outSize = results.size() - inSize;
            if (outSize == 0 && Character.isUpperCase(prefix.charAt(0))) {
                String lcPrefix = prefix.toLowerCase(loc);
                for (String w : words) {
                    if (w.startsWith(lcPrefix)) {
                        results.add(w);
                    }
                }
            }
        }
    }

    private static void processTernaryList(String prefix, TreeSet<String> results, TernaryTreeList ttl) {
        int len = prefix.length();
        if (len < 3) {
            int inSize = results.size();
            switch (len) {
                case 1:
                    results.addAll(ttl.findPartialMatches(prefix + ".....", '.'));
                case 2:
                    results.addAll(ttl.findPartialMatches(prefix + "....", '.'));
                case 3:
                    results.addAll(ttl.findPartialMatches(prefix + "...", '.'));
                    results.addAll(ttl.findPartialMatches(prefix + "..", '.'));
                    results.addAll(ttl.findPartialMatches(prefix + '.', '.'));
                    break;
                default:
                    throw new AssertionError();
            }
            results.addAll(ttl.findPartialMatches(prefix + ".....".substring(prefix.length()), '.'));

            int outSize = results.size() - inSize;
            if (outSize < 12) {
                results.addAll(ttl.findPartialMatches(prefix + "........", '.'));
                results.addAll(ttl.findPartialMatches(prefix + ".......", '.'));
                results.addAll(ttl.findPartialMatches(prefix + "......", '.'));

                outSize = results.size() - inSize;
                if (outSize < 6) {
                    results.addAll(ttl.findWordsWithPrefix(prefix));
                }
            }
        } else {
            results.addAll(ttl.findWordsWithPrefix(prefix));
        }
    }

    /**
     * Creates a new code completion for a field in a class.
     *
     * @param ed the editor to create a completion for
     * @param node the node representing the class the field belongs to
     * @param field the field of the specified class
     * @param prefixLength the length of the matched prefix
     * @return a code alternative for the field
     */
    public static CodeAlternative completionForField(JSourceCodeEditor ed, ClassNode node, Field field, int prefixLength) {
        String name = field.getName();
        Icon icon = Modifier.isStatic(field.getModifiers()) ? NavigationPoint.ICON_CIRCLE_BAR : NavigationPoint.ICON_CIRCLE;
        return new DefaultCodeAlternative(
                ed, name, name, "field", icon, 0, false, prefixLength
        );
    }

    /**
     * Creates a new code alternative for a method or constructor.
     *
     * @param ed the editor to create a completion for
     * @param node the node representing the class in question
     * @param methodOrCtor the method or constructor of the specified class
     * @param prefixLength the length of the matched prefix
     * @return a code alternative for the method
     */
    public static CodeAlternative completionForMethod(JSourceCodeEditor ed, ClassNode node, AccessibleObject methodOrCtor, int prefixLength) {
        String name;
        String category;
        Icon icon;
        Class[] params;
        if (methodOrCtor instanceof Constructor) {
            Constructor c = (Constructor) methodOrCtor;
            name = node.getJavaClass().getSimpleName();
            category = "constructor";
            icon = NavigationPoint.ICON_CLUSTER;
            params = c.getParameterTypes();
        } else {
            Method m = (Method) methodOrCtor;
            name = m.getName();
            int len = name.length();
            category = node.getJavaClass().getSimpleName() + " method";
            icon = NavigationPoint.ICON_DIAMOND;
            params = m.getParameterTypes();
            // check for static method
            if (Modifier.isStatic(m.getModifiers())) {
                icon = NavigationPoint.ICON_DIAMOND_BAR;
            } // check for getter/setter
            else if (len >= 3) {
                if (name.startsWith("is") && Character.isUpperCase(name.charAt(2)) && params.length == 0) {
                    icon = NavigationPoint.ICON_DIAMOND_LEFT;
                } else if (len >= 4 && Character.isUpperCase(name.charAt(3))) {
                    if (name.startsWith("get") && params.length == 0) {
                        icon = NavigationPoint.ICON_DIAMOND_LEFT;
                    } else if (name.startsWith("set") && params.length == 1) {
                        icon = NavigationPoint.ICON_DIAMOND_RIGHT;
                    }
                }
            }
        }

        StringBuilder insertBuff = new StringBuilder(40);
        StringBuilder descBuff = new StringBuilder(60);
        insertBuff.append(name).append('(');
        descBuff.append("<html>").append(insertBuff, 0, insertBuff.length());

        // append parameter data
        if (params.length > 0) {
            insertBuff.append(' ');
            descBuff.append(' ');
            String[] paramNames = node.getParameterNames(methodOrCtor);
            for (int i = 0; i < params.length; ++i) {
                if (i > 0) {
                    insertBuff.append(", ");
                    descBuff.append(", ");
                }
                insertBuff.append(paramNames[i]);
                descBuff.append("<font color=gray>").append(params[i].getSimpleName()).append("</font> ").append(paramNames[i]);
            }
            insertBuff.append(' ');
            descBuff.append(' ');
        }
        insertBuff.append(')');
        descBuff.append(')');

        return new DefaultCodeAlternative(
                ed, insertBuff.toString(), descBuff.toString(),
                category, icon, 0, false, prefixLength
        );
    }

}
