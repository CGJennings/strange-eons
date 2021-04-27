package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.plugins.AbstractUtilityParser;
import ca.cgjennings.script.mozilla.javascript.ast.AstNode;
import ca.cgjennings.script.mozilla.javascript.ast.AstRoot;
import ca.cgjennings.script.mozilla.javascript.ast.ElementGet;
import ca.cgjennings.script.mozilla.javascript.ast.ErrorNode;
import ca.cgjennings.script.mozilla.javascript.ast.FunctionCall;
import ca.cgjennings.script.mozilla.javascript.ast.Name;
import ca.cgjennings.script.mozilla.javascript.ast.NewExpression;
import ca.cgjennings.script.mozilla.javascript.ast.NodeVisitor;
import ca.cgjennings.script.mozilla.javascript.ast.PropertyGet;
import ca.cgjennings.script.mozilla.javascript.ast.StringLiteral;
import java.util.LinkedList;
import java.util.List;

/**
 * Describes the context in which a scripting code completion occurs. The
 * context consists of these elements:
 *
 * <p>
 * First the context captures the sequence of dot expressions and function calls
 * that prefix the caret. For each subexpression, users can look up the
 * identifier with {@link #getIdentifier(int)} and the number of arguments with
 * {@link #getParameterCount(int)} (which is -1 if the subexpression is just an
 * identifier). To get the remainder of the subexpression that follows the
 * caret, call {@link #getSuffix()}. For example, the following expression would
 * consist of the listed values:
 * <pre>
 * alpha.beta.gamma( mu, nu ).del|ta    (where | is the caret)
 *
 *                   i    0     1      2     3
 *     getIdentifier(i) alpha  beta  gamma  del
 * getParameterCount(i)  -1     -1     2    -1
 *
 * getSuffix() = ta
 * </pre>
 *
 * <p>
 * Second, the context includes boolean flags about the syntactic structure that
 * the caret is found in. The names of these methods start with
 * <code>inside</code>. For example {@link #insideNewOperator()} returns
 * <code>true</code> when the user is competing code after the <code>new</code>
 * operator. (In which case, the completer should return the names of
 * constructors.)
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ScriptCompletionContext {

    private LinkedList<String> ids = new LinkedList<>();
    private LinkedList<Integer> args = new LinkedList<>();
    private int offset;
    private String suffix = "";
    private boolean isNonRootLookup = false;
    private static final String STANDIN_ID = "_$__$$___$$$_";

    /**
     * Creates a new prefix analysis for the specified text, with the insertion
     * caret at the specified offset.
     *
     * @param scriptText the script
     * @param caretOffset the caret position
     */
    @SuppressWarnings("empty-statement")
    public ScriptCompletionContext(String scriptText, int caretOffset) {
        offset = caretOffset;

        // HACK:
        // Check if we are at a dot with no id following. This tends to
        // cause errors that are not easily detected. If this appears to
        // be the case, we modify the script to insert a dummy identifier
        // which we later detect and treat as an empty string.
        // scan backwards; if first non-space is a dot, we may need hack
        int i = caretOffset - 1;
        for (; i >= 0 && Character.isSpaceChar(scriptText.charAt(i)); --i);
        if (i >= 0 && !CompletionUtilities.isScriptIdentifierPart(scriptText.charAt(i))) {
            final int scriptLen = scriptText.length();
            // maybe there is still an identifier... scan to the right
            i = caretOffset;
            for (; i < scriptLen && Character.isSpaceChar(scriptText.charAt(i)); ++i);
            // if there is nothing to the right, or there is something that is not part of an identifier
            if (i == scriptLen || !CompletionUtilities.isScriptIdentifierStart(scriptText.charAt(i))) {
                // no identifer here, insert stand-in identifier
                scriptText = scriptText.substring(0, caretOffset) + STANDIN_ID + scriptText.substring(caretOffset);
            }
        }

        //
        // The basic strategy for creating the context is as follows:
        //
        // 1. parse the script into an AST (parser.parse)
        // 2. visit the tree nodes that contain the caret position, recursing
        //    until we find the exact node containing the caret (parser.deepest)
        // 3. to determine the expression of interest, work back up the
        //    tree until we find a parent of a node type that isn't one of
        //    the kinds that make up expressions we support (findExpressionRoot( node ))
        // 4. once we find that point, look at the parent to set context flags
        //    (e.g. is the expression the direct child of a new operator)
        // 5. walk back down the tree to build the list of identifier names and
        //    function call argument counts (visitChild( node, root ))
        //    that isn't part of the expression we are capturing
        //
        ParserBridge parser = new ParserBridge();
        parser.parse(scriptText);

        AstNode node = transformDeepestNode(scriptText, parser.deepest);
        if (node == null) {
            emptyExpression();
            return;
        }

        if (isExpressionNodeType(node) || isSpecialLiteralNodeType(node)) {
            AstNode root = findExpressionRoot(node);

            if (isSpecialLiteralNodeType(node)) {
                // "special literals" are some special cases where instead
                // of the context being a tree, it is just the original
                // root node; for example, if you do a completion inside
                // of a string. In this case, root and node are identical.
                handleSpecialLiteral(node);
            } else {
                visitExpressionNode(node, root);
            }
        }

        if (ids.isEmpty()) {
            emptyExpression();
        } else {
            if (ids.size() == 1 && args.get(0) < 0 && !ids.get(0).isEmpty()) {
                char ch = ids.get(0).charAt(0);
                switch (ch) {
                    case '$':
                        set(CX_SETTING);
                        break;
                    case '@':
                        set(CX_UI_STRING);
                        break;
                    case '#':
                        set(CX_GAME_STRING);
                        break;
                }
            }
        }
    }

    /**
     * Returns the number of identifiers in the expression. For example, if the
     * editor looks like the following, returns 3:
     * <pre>Eons.window.di|</pre>
     *
     * @return the id count
     */
    public int size() {
        return ids.size();
    }

    /**
     * Returns the identifier at the specified index. If this is
     * <code>size()-1</code>, then the identifier includes the text up to the
     * caret position. The identifier text following the caret can be retrieved
     * by calling {@link #getSuffix()}.
     *
     * @param index the index of the identifier segment
     * @return the identifier at the specified segment, or identifier start for
     * the last segment
     */
    public String getIdentifier(int index) {
        return ids.get(index);
    }

    /**
     * Returns the number of parameters used by the function call that occurs
     * within the specified segment. If the segment does not represent a
     * function call, returns -1. For example, the following would have three
     * segments with the parameter counts [2,-1,0]:
     * <pre>alpha(x,y).beta.gamma()</pre>
     *
     * @param index the index of the identifier segment
     * @return the number of parameters passed to the function call at that
     * index, or -1
     */
    public int getParameterCount(int index) {
        return args.get(index);
    }

    /**
     * Returns the prefix immediately before the caret. This is simply the last
     * identifier segment. For example, the following expression has prefix
     * "gam":
     * <pre>alpha.beta.gam|ma</pre>
     *
     * @return the part of the last identifier segment up to the caret; i.e. the
     * last identifier segment
     * @see #getSuffix()
     */
    public String getPrefix() {
        return ids.getLast();
    }

    /**
     * Returns the identifier text that follows the caret, or an empty string if
     * there is none. The following expression has suffix "ma":
     * <pre>alpha.beta.gam|ma</pre>
     *
     * @return the part of the last identifier segment that comes after the
     * caret and is not included in the last segment string
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Returns <code>true</code> if the sequence of names begins by looking up
     * something in scope. If this returns <code>false</code>, then at some
     * point the expression became an arbitrary object that cannot be traced
     * from the scope with the available information.
     *
     * @return whether the expression includes reflective property lookups
     */
    public boolean isFromScopeRoot() {
        return !isNonRootLookup;
    }

    /**
     * Returns <code>true</code> if the caret is inside the parentheses of a
     * call to <code>useLibrary</code>.
     */
    public boolean insideUseLibrary() {
        return test(CX_USE_LIB);
    }

    /**
     * Returns <code>true</code> if the caret is inside the parentheses of a
     * call to <code>importClass</code>.
     */
    public boolean insideImportClass() {
        return test(CX_IMP_CLASS);
    }

    /**
     * Returns <code>true</code> if the caret is inside the parentheses of a
     * call to <code>importPackage</code>.
     */
    public boolean insideImportPackage() {
        return test(CX_IMP_PKG);
    }

    /**
     * Returns <code>true</code> if the expression follows the <code>new</code>
     * keyword.
     */
    public boolean insideNewOperator() {
        return test(CX_NEW);
    }

    /**
     * Returns <code>true</code> if the caret is in a string literal. This is
     * provided mainly for completing <code>useLibrary</code> calls; in other
     * cases the literal may not be detected and the context will be an empty
     * expression. When this is <code>true</code>, there is a single identifier
     * and it will start with the quote character that begins the string literal
     * in the source. The suffix will be blank. Since the literal is usually
     * unclosed when completing code, the intended string end cannot be known
     * for certain.
     */
    public boolean insideStringLiteral() {
        return test(CX_STRING_LIT);
    }

    /**
     * Returns <code>true</code> if the caret is in an $-notation identifier.
     *
     * @return <code>true</code> if completing an interface string
     */
    public boolean insideSettingVariable() {
        return test(CX_SETTING);
    }

    /**
     * Returns <code>true</code> if the caret is in an {@literal @}-notation
     * identifier.
     *
     * @return <code>true</code> if completing an interface string
     */
    public boolean insideInterfaceString() {
        return test(CX_UI_STRING);
    }

    /**
     * Returns <code>true</code> if the caret is in a #-notation identifier.
     *
     * @return <code>true</code> if completing a game string
     */
    public boolean insideGameString() {
        return test(CX_GAME_STRING);
    }

    private void set(int flag) {
        flags |= flag;
    }

    private boolean test(int flag) {
        return (flags & flag) == flag;
    }

    private static final int CX_NEW = 1 << 0;
    private static final int CX_USE_LIB = 1 << 1;
    private static final int CX_IMP_PKG = 1 << 2;
    private static final int CX_IMP_CLASS = 1 << 3;
    private static final int CX_STRING_LIT = 1 << 4;
    private static final int CX_SETTING = 1 << 5;
    private static final int CX_UI_STRING = 1 << 6;
    private static final int CX_GAME_STRING = 1 << 7;
    private int flags;

    /**
     * Returns a string representation of the context for debugging.
     *
     * @return a debug string
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(16 * ids.size());
        b.append('{');
        for (int i = 0; i < ids.size(); ++i) {
            if (i > 0) {
                b.append('.');
            }
            b.append(ids.get(i));
            int a = args.get(i);
            if (a >= 0) {
                b.append('(').append(a).append(')');
            }
        }
        b.append("}, suffix '").append(suffix).append("', flags: ")
                .append(isFromScopeRoot() ? 'R' : 'r')
                .append(insideNewOperator() ? 'N' : 'n')
                .append(insideUseLibrary() ? 'L' : 'l')
                .append(insideImportPackage() ? 'P' : 'p')
                .append(insideImportClass() ? 'C' : 'c')
                .append(' ')
                .append(insideStringLiteral() ? 'S' : 's');
        return b.toString();
    }

    /**
     * Set up the data structures to represent an empty expression.
     */
    private void emptyExpression() {
        ids.clear();
        ids.add("");
        args.clear();
        args.add(-1);
        suffix = "";
    }

    /**
     * Traces up the expression tree to find the start node for the property
     * lookup.
     *
     * @param node the node to start from
     * @return the expression root
     */
    private AstNode findExpressionRoot(AstNode node) {
        // Check for node types that don't involve walking up the tree,
        // e.g., inside a string literal.
        if (isSpecialLiteralNodeType(node)) {
            // call just to set context flags;
            // we will return the same node we were given
            checkParentNode(node.getParent());
        } else {
            for (;;) {
                AstNode parent = node.getParent();
                if (checkParentNode(parent)) {
                    node = parent;
                } else {
                    break;
                }
            }
        }
        return node;
    }

    /**
     * Checks the node that is the parent of part of an expression, setting
     * appropriate context flags and returning whether the current node (the
     * child) is the last node in the expression. That is, it checks the type of
     * the parent node and returns false if it should not be considered part of
     * the context.
     *
     * @param parent the parent node to check
     * @return <code>true</code> if the node is part of the expression (i.e. the
     * caller should keep going up the tree); this does not apply if the child
     * node is a special literal
     */
    private boolean checkParentNode(AstNode parent) {
        if (isExpressionNodeType(parent)) {
            // if the parent is a function call, we need to check if the
            // node is one of the arguments, in which case the function call
            // is not part of the name: i.e., we are checking if we are seeing
            // fncall( alph| ) instead of fncall( alpha ).bet|
            if (parent instanceof FunctionCall) {
                FunctionCall fc = (FunctionCall) parent;
                if (isCaretInsideBrackets(fc, fc.getLp(), fc.getRp())) {
                    // if the caret is in the brackets of the function call,
                    // then we need to check the function name to see if
                    // we need to set any context flags
                    String funcName = getFunctionName(fc);
                    if (funcName != null) {
                        switch (funcName) {
                            case "useLibrary":
                                set(CX_USE_LIB);
                                break;
                            case "importClass":
                                set(CX_IMP_CLASS);
                                break;
                            case "importPackage":
                                set(CX_IMP_PKG);
                                break;
                        }
                    }
                    return false;
                }
            } // same as above, but for elget[ aplh| ]
            else if (parent instanceof ElementGet) {
                ElementGet eg = (ElementGet) parent;
                if (isCaretInsideBrackets(parent, eg.getLb(), eg.getRb())) {
                    return false;
                }
            }
            return true;
        } else {
            // set flag if the parent of the expression is the new operator
            if (parent instanceof NewExpression) {
                set(CX_NEW);
            }
        }
        return false;
    }

    /**
     * Returns the name the function called by a function call node, or
     * <code>null</code> if the function call does not use a simple name.
     *
     * @param fc the function call node
     * @return the name of the called function, if it uses a simple identifier,
     * or <code>null</code>
     */
    private String getFunctionName(FunctionCall fc) {
        return (fc.getTarget() instanceof Name) ? ((Name) fc.getTarget()).getIdentifier() : null;
    }

    /**
     * Used to check if the offset is in the bracket part of nodes like
     * FunctionCall or ElementGet.
     *
     * @param node the node with brackets being checked
     * @param lparen the left bracket offset
     * @param rparen the right bracket offset
     * @return if the caret is inside the brackets
     */
    private boolean isCaretInsideBrackets(AstNode node, int lparen, int rparen) {
        int start = node.getAbsolutePosition();
        if ((lparen >= 0) && (offset >= (start + lparen)) && ((rparen < 0) || (offset <= (start + rparen)))) {
            return true;
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the node is one of the types that make up a
     * supported dot expression.
     *
     * @param node the node to check
     * @return true if the node can be part of a dot expression
     */
    private boolean isExpressionNodeType(AstNode node) {
        if (node instanceof ElementGet) {
            return true;
        }
        if (node instanceof PropertyGet) {
            return true;
        }
        if (node instanceof Name) {
            return true;
        }
        if (node instanceof FunctionCall) {
            // because NewExpression is a subclass of FunctionCall
            return node.getClass() == FunctionCall.class;
        }
        if (node instanceof ErrorNode) {
            return true;
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the node is one of the special literals we
     * are interested in ONLY when they are the node that contains the caret.
     *
     * @param node the node to check
     * @return <code>true</code> if the node is a special literal
     */
    private boolean isSpecialLiteralNodeType(AstNode node) {
        return node instanceof StringLiteral;
    }

    /**
     * This is called when it is determined that the expression is a "special
     * literal" type. The main context flags have already been set. This method
     * has to extract an expression segment for the node and set the flag
     * describing the type of special node it represents.
     *
     * @param node the special node to analyze
     */
    private void handleSpecialLiteral(AstNode node) {
        if (node instanceof StringLiteral) {
            StringLiteral litNode = (StringLiteral) node;
            String value = litNode.getValue(true);
            int split = offset - litNode.getAbsolutePosition();
            ids.add(value.substring(0, split));
            // since the literal is sometimes unclosed, and we can't tell that
            // from here, we always leave the suffix blank
            suffix = "";
            args.add(-1);
            set(CX_STRING_LIT);
        } else {
            throw new AssertionError();
        }
    }

    /**
     * Recursively builds the prefix list by visiting the subtree.
     *
     * @param last the node where the caret occurs
     * @param node the subtree to visit
     * @return true if the caller should continue visiting nodes; false if the
     * node containing the caret has been reached
     */
    private boolean visitExpressionNode(AstNode last, AstNode root) {
        boolean keepGoing = false;
        if (root instanceof FunctionCall) {
            keepGoing = visitFunctionCallTree(last, (FunctionCall) root);
        } else if (root instanceof PropertyGet) {
            keepGoing = visitPropertyGetTree(last, (PropertyGet) root);
        } else if (root instanceof Name) {
            keepGoing = visitNameLeaf(last, (Name) root);
        } else if (root instanceof ElementGet) {
            keepGoing = visitElementGet(last, (ElementGet) root);
        } else if (root instanceof ErrorNode) {
            ids.add("");
            args.add(-1);
        }
        return keepGoing;
    }

    /**
     * Recursively build the script prefix list.
     *
     * @param last the node where the caret occurs
     * @param node the root of an expression subtree
     * @return true if the recursive list building should continue; false if the
     * node containing the caret has been reached
     */
    private boolean visitPropertyGetTree(AstNode last, PropertyGet node) {
        AstNode lhs = node.getLeft();
        AstNode rhs = node.getRight();

        if (!visitExpressionNode(last, lhs)) {
            return false;
        }
        return visitExpressionNode(last, rhs);
    }

    /**
     * Recursively build the script prefix list.
     *
     * @param last the node where the caret occurs
     * @param node the root of an expression subtree
     * @return true if the recursive list building should continue; false if the
     * node containing the caret has been reached
     */
    private boolean visitFunctionCallTree(AstNode last, FunctionCall node) {
        // recursively build the name of the function being called, then
        // patch the segment (which will have an arg count of -1) to the correct count
        boolean keepGoing = visitExpressionNode(last, node.getTarget());
        args.set(args.size() - 1, node.getArguments().size());
        return keepGoing;
    }

    /**
     * Recursively build the script prefix list.
     *
     * @param last the node where the caret occurs
     * @param node the root of an expression subtree
     * @return true if the recursive list building should continue; false if the
     * node containing the caret has been reached
     */
    private boolean visitElementGet(AstNode last, ElementGet node) {
        isNonRootLookup = true;
        return visitExpressionNode(last, node.getTarget());
    }

    /**
     * Recursively build the script prefix list.
     *
     * @param last the node where the caret occurs
     * @param node the root of an expression subtree
     * @return true if the recursive list building should continue; false if the
     * node containing the caret has been reached
     */
    private boolean visitNameLeaf(AstNode last, Name node) {
        if (last == node) {
            String id = node.getIdentifier();
            int length = offset - node.getAbsolutePosition();
            int prefixLength = Math.max(0, Math.min(length, id.length()));
            suffix = id.substring(prefixLength);

            // HACK
            // See constructor
            if (STANDIN_ID.equals(suffix)) {
                suffix = "";
            }

            id = id.substring(0, prefixLength);
            ids.add(id);
            args.add(-1);
        } else {
            ids.add(node.getIdentifier());
            args.add(-1);
        }
        return node != last;
    }

    /**
     * Returns true if the caret is within this node's subtree.
     *
     * @param node the node to test
     * @return true if caret is within the node or its descendants
     */
    private boolean caretIsInNode(AstNode node) {
        final int start = node.getAbsolutePosition();
        return offset >= start && offset <= start + node.getLength();
    }

    /**
     * This is called after visiting nodes in order to find the deepest one.
     * This checks for some special cases, and may create and return a
     * replacement node to use instead. Alternatively, it may return null to
     * prevent any further processing. Usually it returns the original node
     * unchanged. It is used because the node visitor skips error nodes, but
     * since we are completing code and not executing it, we mat be able to
     * recover from cases that a parser can't.
     */
    private AstNode transformDeepestNode(String script, AstNode node) {

        // Here we are checking for the special case of a useLibrary call with
        // an unclosed string literal inside; in this case the first argument
        // is an error node, but the position may be invalid so we have to extract
        // the string literal ourselves.
        // Sometimes the deepest node will be the error node, sometimes
        // the enclosing function call; for simplicity we check for the
        // first type and transfrom it into the second.
        if (node instanceof ErrorNode && (node.getParent() instanceof FunctionCall)) {
            FunctionCall fc = (FunctionCall) node.getParent();
            List<AstNode> funcArgs = fc.getArguments();
            if (!funcArgs.isEmpty() && funcArgs.get(0) == node) {
                node = fc;
            }
        }
        if (node instanceof FunctionCall && "useLibrary".equals(getFunctionName((FunctionCall) node))) {
            FunctionCall fc = (FunctionCall) node;
            List<AstNode> funcArgs = fc.getArguments();
            if (!funcArgs.isEmpty() && funcArgs.get(0) instanceof ErrorNode) {
                int pos = fc.getAbsolutePosition() + fc.getLp() + 1;
                int len = script.length();
                while (pos < len && Character.isSpaceChar(script.charAt(pos))) {
                    ++pos;
                }
                if (pos < len && (script.charAt(pos) == '\'' || script.charAt(pos) == '"')) {
                    // we found the start of a string; we know the caret is in
                    // the string because the string wasn't closed (hence the error)
                    // so, we'll assume everything up to the caret or the first
                    // newline is part of the literal
                    int end = script.indexOf('\n', pos + 1);
                    if (end < 0) {
                        end = offset;
                    } else {
                        // check for \r\n; end-1 must exist though it might be pos, which is a quote
                        if (script.charAt(end - 1) == '\r') {
                            --end;
                        }
                        end = Math.min(offset, end);
                    }
                    StringLiteral literalNode = new StringLiteral(pos, end);
                    literalNode.setQuoteCharacter(script.charAt(pos));
                    literalNode.setValue(script.substring(pos + 1, end));
                    fc.setArguments(null);
                    fc.addArgument(literalNode);
                    return literalNode;
                }

            }
        }
        return node;
    }

    /*
	 * Both this and the main code completer use parsers and parse the
	 * source file. However, this parser only examines the current source file
	 * to get info about the area surrounding the caret, so it is very fast.
	 * In fact, in many cases this is the only parse needed. For example,
	 * if it comes back the user is inside of an importClass( ... ), then we
	 * can immediately build a list of packages and classes without considering
	 * the rest of the file.
     */
    private class ParserBridge extends AbstractUtilityParser implements NodeVisitor {

        @Override
        protected void processTree(AstRoot rootNode) {
            if (rootNode != null) {
//				System.out.println( rootNode.debugPrint() );
                rootNode.visit(this);
            }
        }

        @Override
        public boolean visit(AstNode node) {
            if (caretIsInNode(node)) {
                deepest = node;
                return true;
            }
            return false;
        }
        AstNode deepest;
    }

//	public static void main(String[] args) {
//		String[] tests = new String[] {
//			"i(|);",
//			"alp|ha",
//			"alpha.beta.gamma.del|",
//			"alpha.beta.gamma.del|ta",
//			"alpha.be|ta.gamma.delta",
//			"alpha.beta|.gamma.delta",
//			"alpha.beta.|gamma.delta",
//			"alp|ha.beta['b'].gamma",
//			"alpha.beta['special'].gamma.de|lta",
//			"alpha.bet|a['special'].gamma.delta",
//			"alpha.beta[ omicron.om|ega ].gamma.delta",
//			"omega[beta.gamma.de|lta]",
//			"alpha.beta(a).ga|mma.delta",
//			"alpha.beta.gamma.delta(a,b,c,d)",
//			"alpha(x).beta",
//			"alpha.beta|(omicron, phi.omeg).gamma",
//			"alpha.beta(|omicron, phi.omeg).gamma",
//			"alpha.beta(omicron, phi.omeg|).gamma",
//			"alpha.beta(omicron, phi.omega( pi, mu.rh| )).gamma",
//		};
//		for( String code : tests ) {
//			int offset = code.indexOf('|');
//			if( offset < 0 ) {
//				offset = code.length();
//				code += '|';
//			}
//			System.out.println( code + ':');
//			code = code.substring( 0, offset ) + code.substring( offset+1 );
//			System.out.println( new ScriptCompletionContext( code, offset ).toString() );
//			System.out.println();
//		}
//	}
}
