package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.apps.arkham.plugins.AbstractUtilityParser;
import ca.cgjennings.script.mozilla.javascript.ast.AstNode;
import ca.cgjennings.script.mozilla.javascript.ast.AstRoot;
import ca.cgjennings.script.mozilla.javascript.ast.FunctionNode;
import ca.cgjennings.script.mozilla.javascript.ast.Name;
import ca.cgjennings.script.mozilla.javascript.ast.NodeVisitor;
import ca.cgjennings.script.mozilla.javascript.ast.Scope;
import ca.cgjennings.script.mozilla.javascript.ast.Symbol;
import ca.cgjennings.script.mozilla.javascript.ast.VariableDeclaration;
import ca.cgjennings.script.mozilla.javascript.ast.VariableInitializer;
import java.util.HashSet;
import java.util.Map;

/**
 * Builds a tree of objects in the scope of a offset position within a script
 * file. Used to determine the names and possible types of identifiers visible
 * from the offset for source completion.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
final class ScopeParser extends AbstractUtilityParser {

    int offset;
    private ScopeID root;
    private final HashSet<String> scopeTable = new HashSet<>();
    private ScriptCompletionContext scc;

    public ScopeParser() {
    }

    /**
     * Parses the source file, building a tree of scope information for
     * identifiers in scope.
     *
     * @param script the script to parse
     * @param caretOffset the caret offset where completion is requested
     * @param scc a completion context for the script and offset
     * @return a tree of identifiers
     */
    public ScopeID parse(String script, int caretOffset, ScriptCompletionContext scc) {
        if (caretOffset < 0 || caretOffset > script.length()) {
            throw new IllegalArgumentException("caretOffset");
        }
        try {
            this.scc = scc;
            root = new ScopeID(scc);
            scopeTable.clear();
            offset = caretOffset;
            parse(script);
            cleanRoot();
        } finally {
            this.scc = null;
        }

        return root;
    }

    @Override
    public void parse(String script) {
        if (scc == null) {
            // must call via parse( String, int, ScriptCompletionContext )
            throw new IllegalStateException();
        }
        try {
            super.parse(script);
        } finally {
            // in case no offset param supplied next time
            offset = 0;
        }

//		for( ScopeValue n : scopeTable ) {
//			System.out.println(n);
//		}
    }

    @Override
    protected void processTree(AstRoot rootNode) {
        new Visitor(root, rootNode);
    }

    private boolean visitNode(ScopeID parent, AstNode node) {
        if (node instanceof VariableDeclaration) {
            visitVar(parent, (VariableDeclaration) node);
        } else if (node instanceof FunctionNode) {
            return visitFunction(parent, (FunctionNode) node);
        } else if (node instanceof Scope) {
            visitScope((Scope) node);
        }
//			System.out.println( node.getClass().getName() );
//
//
//			Scope scope = (Scope) node;
//			if( scope.getSymbolTable() != null ) {
//				for( Entry<String,Symbol> e : scope.getSymbolTable().entrySet() ) {
//	//				ScopeValue v = new ScopeValue( e.getKey() );
//					Symbol sym = e.getValue();
//					System.out.println( sym.getNode() );
//				}
//			}
//
//			return true;
//		}

        return true;
//		return false;
    }

    /**
     * Process a function node by adding it to the scope tree. If the cursor is
     * inside the node, we also need to add the function arguments.
     *
     * @param parent the parent node
     * @param fn the function to process
     * @return <code>true</code> if the scope analysis should recurse into the
     * function
     */
    private boolean visitFunction(ScopeID parent, FunctionNode fn) {
        AstRoot r;
        if (caretIsInNode(fn)) {
            fn.flattenSymbolTable(false);
            int paramCount = fn.getParamCount();
            String[] names = fn.getParamAndVarNames();
            for (int i = 0; i < paramCount; ++i) {
                parent.addArgument(names[i]);
                scopeTable.add(names[i]);
            }
            return true;
        }
        return false;
    }

    /**
     * Process a variable declaration (var, const, let).
     *
     * @param parent the node that the new variables are defined in
     * @param var the root node of the declaration
     */
    private void visitVar(ScopeID parent, VariableDeclaration var) {
        for (VariableInitializer vi : var.getVariables()) {
            AstNode target = vi.getTarget();
            // ignore variable if destructuring form
            if (!(target instanceof Name)) {
                continue;
            }

            String name = ((Name) target).getIdentifier();
            decodeAssignment(parent, name, vi.getInitializer());
        }
    }

    /**
     * Decodes an expression used to define a variable and tries to create an
     * appropriate entry in the scope tree.
     *
     * @param parent the scope tree to add a new entry to
     * @param root the root of the AST tree for the assignment
     */
    private void decodeAssignment(ScopeID parent, String name, AstNode root) {
        parent.add(name);
    }

    /**
     * Handles a scope node in the parse tree. This ensures that all variables
     * in scope get added to the scopeTable. This is a fallback to catch any
     * names we might otherwise miss. Unfortunately, the getNode method of the
     * scope table entries returns false. Otherwise we could just zip through
     * the scopes and create ScopeIDs for exactly the nodes in scope.
     *
     * @param scope the AST scope to process
     */
    private void visitScope(Scope scope) {
        if (!caretIsInNode(scope)) {
            return;
        }

        Map<String, Symbol> table = scope.getSymbolTable();
        if (table != null) {
            for (String name : table.keySet()) {
                scopeTable.add(name);
            }
        }
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
     * Cleans up the root after the analysis is complete, pruning branches for
     * names that don't appear in the scope table.
     */
    private void cleanRoot() {
        // ensure that only names in the scope table appear in the root
        HashSet<ScopeID> branchesToPrune = new HashSet<>();
        for (ScopeID id : root.children()) {
            if (!scopeTable.contains(id.getName())) {
                branchesToPrune.add(id);
            }
        }
        for (ScopeID id : branchesToPrune) {
            root.remove(id);
        }

        // ensure everything in scope has some kind of entry
        for (String name : scopeTable) {
            if (!root.containsName(name)) {
                root.add(name);
            }
        }
    }

    private class Visitor implements NodeVisitor {

        private final ScopeID root;

        public Visitor(ScopeID root, AstNode startNode) {
            this.root = root;
            startNode.visit(this);
        }

        @Override
        public boolean visit(AstNode node) {
            return visitNode(root, node);
        }
    }
}
