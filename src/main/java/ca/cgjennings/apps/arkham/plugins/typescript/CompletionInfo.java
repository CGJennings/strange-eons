package ca.cgjennings.apps.arkham.plugins.typescript;

import java.util.List;

/**
 * A collection of possible completions.
 */
public class CompletionInfo {
    public CompletionInfo() {
    }
    
    public boolean isMemberCompletion;
    public boolean isNewIdentifierLocation;
    public boolean isIncomplete;
    public List<Entry> entries;

    @Override
    public String toString() {
        return "CompletionInfo{" + "isMemberCompletion=" + isMemberCompletion + ", isNewIdentifierLocation=" + isNewIdentifierLocation + ", isIncomplete=" + isIncomplete + ", entries=" + entries + '}';
    }


    
    /** A code completion. */
    public static class Entry implements Comparable<Entry>{
        public Entry(Object jsObject, int sortKey) {
            this.js = jsObject;
            this.sortKey = sortKey;
        }
        
        public String name;
        public String kind;
        public String kindModifiers;
        public boolean isSnippet;
        public String insertText;
        public TextSpan replacementSpan;
        public boolean isRecommended;
        public boolean hasAction;
        public String sourceDisplay;
        
        /** Private JS object. */
        public final Object js;
        private final int sortKey;

        public String getTextToInsert() {
            return insertText == null ? name : insertText;
        }

        @Override
        public String toString() {
            return "Entry{" + "name=" + name + ", kind=" + kind + ", kindModifiers=" + kindModifiers + ", isSnippet=" + isSnippet + ", insertText=" + insertText + ", replacementSpan=" + replacementSpan + ", isRecommended=" + isRecommended + ", hasAction=" + hasAction + ", sourceDisplay=" + sourceDisplay + '}';
        }
        


        @Override
        public int compareTo(Entry o) {
            int d = sortKey - o.sortKey;
            if (d == 0) {
                d = name.compareTo(o.name);
            }
            return d;
        }
    }
    
    public static class EntryDetails extends DocCommentable {
        public EntryDetails() {
        }
        
        public List<CodeAction> actions;

        @Override
        public String toString() {
            return "EntryDetails{" + "display=" + display + ", documentation=" + documentation + ", source=" + source + ", actions=" + actions + '}';
        }
    }
}
