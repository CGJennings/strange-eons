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
}
