package ca.cgjennings.ui.textedit;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;

/**
 * Common editor action identifiers.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.4
 * @see CodeEditorBase#performAction(java.lang.String) 
 */
public final class CodeAction {
    private CodeAction() {}
    
    public static final String completeCode = "AutoComplete";
    
    public static final String moveUp = RSyntaxTextAreaEditorKit.rtaLineUpAction;
    public static final String moveDown = RSyntaxTextAreaEditorKit.rtaLineDownAction;
    
    public static final String toggleFold = RSyntaxTextAreaEditorKit.rstaToggleCurrentFoldAction;
    public static final String expandFold = RSyntaxTextAreaEditorKit.rstaExpandFoldAction;
    public static final String expandAllFolds = RSyntaxTextAreaEditorKit.rstaExpandAllFoldsAction;
    public static final String collapseFold = RSyntaxTextAreaEditorKit.rstaCollapseFoldAction;
    public static final String collapseAllFolds = RSyntaxTextAreaEditorKit.rstaCollapseAllFoldsAction;
    public static final String collapseCommentFolds = RSyntaxTextAreaEditorKit.rstaCollapseAllCommentFoldsAction;
    
    public static final String goToMatchingBracket = RSyntaxTextAreaEditorKit.rstaGoToMatchingBracketAction;
    public static final String goToNextOccurrence = RSyntaxTextAreaEditorKit.rtaNextOccurrenceAction;
    public static final String goToPrevOccurrence = RSyntaxTextAreaEditorKit.rtaPrevOccurrenceAction;
    
    public static final String toggleComment = RSyntaxTextAreaEditorKit.rstaToggleCommentAction;
    
    public static final String joinLines = RSyntaxTextAreaEditorKit.rtaJoinLinesAction;
    
    public static final String invertCase = RSyntaxTextAreaEditorKit.rtaInvertSelectionCaseAction;
    public static final String lowerCase = RSyntaxTextAreaEditorKit.rtaLowerSelectionCaseAction;
    public static final String upperCase = RSyntaxTextAreaEditorKit.rtaUpperSelectionCaseAction;
    
    public static final String timeAndDate = RSyntaxTextAreaEditorKit.rtaTimeDateAction;
}
