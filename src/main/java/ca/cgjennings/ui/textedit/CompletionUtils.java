package ca.cgjennings.ui.textedit;

import ca.cgjennings.spelling.SpellingChecker;
import ca.cgjennings.spelling.dict.TernaryTreeList;
import ca.cgjennings.spelling.dict.WordList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionProvider;

/**
 * Utilities to support creating completion providers.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 */
final class CompletionUtils {
    private CompletionUtils() {}

    /** Minimum number of completions before we stop trying to find more. */
    private static final int MIN_COMPLETIONS = 6;
    
    public static List<Completion> getWordCompletions(CompletionProvider provider, String prefix, SpellingChecker checker, boolean includeUserDictionaries) {
        int len = prefix.length();
        if (len == 0) {
            // too many words to add suggestions for empty prefix
            return Collections.emptyList();
        }
        if (checker == null) {
            checker = SpellingChecker.getSharedInstance();
        }
        
        Locale loc = checker.getLocale();
        Set<String> results = new LinkedHashSet<>();
        
        if (Character.isUpperCase(prefix.charAt(0))) {
            addWords(prefix.toLowerCase(loc), checker, includeUserDictionaries, results, true);
        }
        addWords(prefix, checker, includeUserDictionaries, results, false);
        
        List<Completion> completions = new ArrayList<>(results.size());
        
        // if not many completions, add spelling correction suggestions to TOP of list
        if (results.size() < MIN_COMPLETIONS) {
            String[] suggestions = checker.getSuggestions(prefix, MIN_COMPLETIONS);
            checker.adjustSuggestionCapitalization(prefix, suggestions);
            for (String word : suggestions) {
                completions.add(new BasicCompletion(provider, word));
                results.remove(word); // prevent being listed twice
            }
        }
        
        // add words which complete the prefix as suggestions
        results.forEach(word -> completions.add(new BasicCompletion(provider, word)));
        return completions;
    }
    
    private static void addWords(String prefix, SpellingChecker checker, boolean includeUserDictionaries, Set<String> results, boolean capitalize) {
        Locale loc = checker.getLocale();
        addFromWordList(prefix, results, checker.getMainList(), loc, capitalize);
        if (includeUserDictionaries) {
            addFromWordList(prefix, results, checker.getUserList(), loc, capitalize);
            addFromWordList(prefix, results, checker.getIgnoreList(), loc, capitalize);
        }
    }

    private static void addFromWordList(String prefix, Set<String> results, WordList wl, Locale loc, boolean capitalize) {
        if (wl instanceof TernaryTreeList) {
            TernaryTreeList ttl = (TernaryTreeList) wl;
            addFromTreeList(prefix, results, ttl, capitalize);
        } else {
            List<String> words = wl.getWords();
            for (String w : words) {
                if (w.startsWith(prefix)) {
                    results.add(capitalize ? capitalize(w) : w);
                }
            }
        }
    }
    
    private static void addFromTreeList(String prefix, Set<String> results, TernaryTreeList ttl, boolean capitalize) {
        int len = prefix.length();
        if (len <= 3) {
            for (int dots=1; dots <= 5 && results.size() < MIN_COMPLETIONS; ++dots) {
                String pat = ".....".substring(0, dots);
                addFrom(ttl.findPartialMatches(prefix + pat, '.'), results, capitalize);
            }
            
            if (results.size() < MIN_COMPLETIONS) {
                addFrom(ttl.findWordsWithPrefix(prefix), results, capitalize);
            }
        } else {
            addFrom(ttl.findWordsWithPrefix(prefix), results, capitalize);
        }
    }
    
    private static void addFrom(Collection<String> words, Set<String> results, boolean capitalize) {
        if (capitalize) {
            for (String w : words) {
                results.add(capitalize(w));
            }
        } else {
            results.addAll(words);
        }
    }
    
    private static String capitalize(String w) {
        if (w.isEmpty()) return w;
        return Character.toUpperCase(w.charAt(0)) + w.substring(1);
    }
}