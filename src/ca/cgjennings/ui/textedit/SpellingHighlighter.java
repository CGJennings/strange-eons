package ca.cgjennings.ui.textedit;

import ca.cgjennings.spelling.SpellingChecker;

/**
 * A highlighter for source code editors that highlights spelling errors using a
 * {@link SpellingChecker}.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class SpellingHighlighter /*extends ErrorHighlighter*/ {

//    private SpellingChecker sc;
//    private EnumSet<TokenType> types;
//    private Pattern regex;
//
//    /**
//     * If false, spelling error highlights in code editors are not drawn.
//     */
    public static boolean ENABLE_SPELLING_HIGHLIGHT = true;
//
//    /**
//     * Creates a spelling highlighter that uses the shared spelling checker.
//     */
//    public SpellingHighlighter() {
//        this(null, null);
//    }
//
//    /**
//     * Creates a spelling highlighter that uses the shared spelling checker.
//     * Only text marked with the token types included in the specified enum set
//     * are checked.
//     *
//     * @param tokenTypesToCheck the set of token types to check
//     */
//    public SpellingHighlighter(EnumSet<TokenType> tokenTypesToCheck) {
//        this(null, tokenTypesToCheck);
//    }
//
//    /**
//     * Creates a spelling highlighter that uses the specified spelling checker.
//     * Only text marked with the token types included in the specified enum set
//     * are checked.
//     *
//     * @param checker the spelling checker to use
//     * @param tokenTypesToCheck the set of token types to check
//     */
//    public SpellingHighlighter(SpellingChecker checker, EnumSet<TokenType> tokenTypesToCheck) {
//        if (checker == null) {
//            checker = SpellingChecker.getSharedInstance();
//        }
//        if (tokenTypesToCheck == null) {
//            tokenTypesToCheck = EnumSet.of(TokenType.PLAIN);
//        }
//        sc = checker;
//        types = tokenTypesToCheck;
//        if (sc.getPolicy() != null) {
//            regex = (Pattern) sc.getPolicy().getHint(WordPolicy.Hint.WORD_REGEX);
//        }
//        if (regex == null) {
//            regex = WordTokenizer.WORD_PATTERN;
//        }
//    }

//    @Override
//    public void paint(Graphics2D g, HighlightedLine hl) {
//        if (!ENABLE_SPELLING_HIGHLIGHT) {
//            return;
//        }
//
//        Token token = hl.tokenList();
//        if (token == null) {
//            return;
//        }
//
//        int offset = 0;
//        for (;;) {
//            if (token.type == TokenType.END_MARKER) {
//                break;
//            }
//
//            if (types.contains(token.type)) {
//                segment = hl.text(offset, token.length, segment);
//
//                Matcher matcher = regex.matcher(segment);
//                for (int i = 0; matcher.find(i) && matcher.start() < segment.length(); i = matcher.end()) {
//                    if (!sc.isCorrect(matcher.group(1)) && !ignoreWord(segment, matcher.start(1), matcher.end(1))) {
//                        paintErrorLine(g, hl, offset + matcher.start(), offset + matcher.end());
//                    }
//                }
//            }
//
//            offset += token.length;
//            token = token.next;
//        }
//
//        restore(g);
//    }
//    private Segment segment;
//
//    /**
//     * Called before marking a word as incorrectly spelled. This allows the
//     * highlighter to be more lax about spelling errors than a typical
//     * spelling-checked component would be. The default implementation ignores
//     * words that use CamelCase or that are connected to other words by any of
//     * {@code .:/\}.
//     *
//     * @param text a segment containing the contents of the current token
//     * @param offset the offset to the start of the match
//     * @param length the length of the match
//     */
//    protected boolean ignoreWord(Segment text, int offset, int length) {
//        int first = text.offset;
//        char[] array = text.array;
//
//        // check if previous character is connector punctuation
//        if (offset > 0) {
//            char c = array[first + offset - 1];
//            if (c == '.' || c == ':' || c == '/' || c == '\\') {
//                return true;
//            }
//        }
//
//        // check if following character is connector punctuation, and
//        // the character after that is an identifier part
//        int next = first + offset + length;
//        if (next < first + text.count) {
//            char c = array[next];
//            if (Character.isJavaIdentifierStart(c)) {
//                c = array[--next];
//                if (c == '.' || c == ':' || c == '/' || c == '\\') {
//                    return true;
//                }
//            }
//        }
//
//        // check for CamelCase
//        if (length > 2) {
//            int uc = 0, lc = 0;
//            for (int i = offset; i < length; ++i) {
//                char c = array[first + i];
//                if (Character.isUpperCase(c)) {
//                    ++uc;
//                } else if (Character.isLowerCase(c)) {
//                    ++lc;
//                    if (i == offset) {
//                        ++uc;
//                    }
//                }
//                if (lc >= 1 && uc >= 2) {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public String getToolTipText(MouseEvent evt) {
//        return null;
//    }
//
//    @Override
//    public JPopupMenu getPopupMenu(MouseEvent e, HighlightedLine hl) {
//        // Step 1: determine if the user right clicked on a highlighted word
//        int selectedOffset = hl.offset(e.getX());
//        Token token = hl.tokenList();
//        if (selectedOffset < 0 || token == null) {
//            return null;
//        }
//
//        int offset = 0;
//        for (;;) {
//            if (token.type == TokenType.END_MARKER) {
//                break;
//            }
//
//            // is this the token that contains the right click point?
//            if (selectedOffset >= offset && selectedOffset < offset + token.length) {
//                if (types.contains(token.type)) {
//                    segment = hl.text(offset, token.length, segment);
//
//                    Matcher matcher = regex.matcher(segment);
//                    for (int i = 0; matcher.find(i) && matcher.start() < segment.length(); i = matcher.end()) {
//                        int start = offset + matcher.start(1);
//                        int end = offset + matcher.end(1);
//                        if (selectedOffset >= start && selectedOffset <= end) {
//                            if (!sc.isCorrect(matcher.group(1)) && !ignoreWord(segment, matcher.start(1), matcher.end(1))) {
//                                return createMenu(hl, matcher.group(1), start, end);
//                            }
//                        }
//                    }
//                }
//                // either the word was spelled correctly or it wasn't a checked
//                // token type, in either case we can stop looking
//                break;
//            }
//
//            offset += token.length;
//            token = token.next;
//        }
//        return null;
//    }
//
//    private JPopupMenu createMenu(final HighlightedLine hl, final String word, final int start, final int end) {
//        JPopupMenu popup = new JPopupMenu();
//        JMenuItem item;
//
//        String[] suggestions = sc.getSuggestions(word, 10);
//        if (suggestions.length > 0) {
//            for (String suggestion : suggestions) {
//                item = new JMenuItem(suggestion);
//                item.addActionListener((ActionEvent e) -> {
//                    JSourceCodeEditor editor = hl.editor();
//                    editor.getDocument().beginCompoundEdit();
//                    try {
//                        int startOffset = hl.startOffset();
//                        int docStart = start + startOffset;
//                        int docEnd = end + startOffset;
//                        editor.select(docStart, docEnd);
//                        String replacement = ((JMenuItem) e.getSource()).getText();
//                        editor.setSelectedText(replacement);
//                        editor.select(docStart, docStart + replacement.length());
//                    } finally {
//                        editor.getDocument().endCompoundEdit();
//                    }
//                });
//                item.setFont(item.getFont().deriveFont(Font.BOLD));
//                popup.add(item);
//            }
//        } else {
//            item = new JMenuItem(string("no-suggestions", "No Suggestions"));
//            item.setEnabled(false);
//            item.setFont(item.getFont().deriveFont(Font.BOLD));
//            popup.add(item);
//        }
//        popup.addSeparator();
//
//        item = popup.add(new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                sc.ignoreWord(word);
//            }
//        });
//
//        item.setText(string("ignore", "Ignore All Occurrences"));
//
//        item = popup.add(new AbstractAction() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                sc.learnWord(word);
//            }
//        });
//
//        item.setText(string("learn", "Add to Dictionary"));
//
//        MenuBuilder mb = PopupMenuFactory.getMisspellMenuBuilder();
//        if (mb != null) {
//            mb.buildMenu(popup);
//        }
//
//        return popup;
//    }
//
//    @Override
//    public Set<MarginNote> getMarginNotes(int lineIndex) {
//        return null;
//    }
}
