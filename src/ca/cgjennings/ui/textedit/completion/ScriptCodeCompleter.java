package ca.cgjennings.ui.textedit.completion;

import ca.cgjennings.spelling.SpellingChecker;
import ca.cgjennings.ui.textedit.JSourceCodeEditor;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@linkplain CodeCompleter code completer} for JavaScript source files.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public class ScriptCodeCompleter implements CodeCompleter {

    /**
     * Creates a new script code completer.
     */
    public ScriptCodeCompleter() {
    }

    @Override
    public Set<CodeAlternative> getCodeAlternatives(JSourceCodeEditor editor) {
        TreeSet<CodeAlternative> set = new TreeSet<>();

        String script = editor.getText();
        int offset = editor.getCaretPosition();

        ScriptCompletionContext context = new ScriptCompletionContext(script, offset);

        /////////////
        // Completions inside string literals
        if (context.insideUseLibrary()) {
            CompletionUtilities.addLibraryNameAlternatives(editor, set, context);
        } else if (context.insideStringLiteral()) {
            String prefix = context.getPrefix();
            if (!prefix.isEmpty()) {
                char ch = prefix.charAt(0);
                if (ch == '\'' || ch == '"') {
                    prefix = prefix.substring(1);
                }
            }
            if (!prefix.isEmpty()) {
                CompletionUtilities.addWords(editor, set, context.getPrefix(), SpellingChecker.getSharedInstance());
            }
        }

        if (context.insideStringLiteral()) {
            return set;
        }

        /////////////
        // Special case completions that are independent of scope
        if (context.insideSettingVariable()) {
            CompletionUtilities.addSettingAlternatives(editor, set, context);
        } else if (context.insideInterfaceString()) {
            CompletionUtilities.addStringAlternatives(editor, set, context, true);
        } else if (context.insideGameString()) {
            CompletionUtilities.addStringAlternatives(editor, set, context, false);
        } else if (context.insideImportClass()) {
            CompletionUtilities.addPackages(editor, set, context, true);
        } else if (context.insideImportPackage()) {
            CompletionUtilities.addPackages(editor, set, context, false);
        } /////////////
        // All special cases are exhausted; we'll have to do a more complete
        // parsing of the source file and recursively load and parse linked libraries.
        else {
            ScopeParser parser = new ScopeParser();
            ScopeID root = parser.parse(script, offset, context);

            // build a set of all of possibly matching subtrees; walk the
            // list of identifiers one at a time and eliminate any subtrees
            // that don't match; once all identifiers have been processed,
            // match whatever is left against the prefix, creating completions
            // for each match
            HashSet<ScopeID> candidates = new HashSet<>(root.children());

            // walk through all of the *complete* identifiers (not the prefix)
            int count = context.size() - 1;
            for (int i = 0; i < count; ++i) {
                HashSet<ScopeID> matches = new HashSet<>();
                for (ScopeID id : candidates) {
                    String name = context.getIdentifier(i);
                    int argCount = context.getParameterCount(i);
                    if (id.getName().equals(name) && id.getParameterCount() == argCount) {
                        matches.addAll(id.children());
                    }
                }
                candidates = matches;
            }

            String prefix = context.getPrefix();
            for (ScopeID id : candidates) {
                if (CompletionUtilities.match(prefix, id.getName())) {
                    set.add(id.toCompletion(editor, prefix.length()));
                }
            }
        }

        return set;
    }
}

//
//import ca.cgjennings.apps.arkham.AbstractStrangeEonsEditor;
//import ca.cgjennings.apps.arkham.StrangeEons;
//import ca.cgjennings.apps.arkham.editors.NavigationPoint;
//import ca.cgjennings.apps.arkham.plugins.AbstractUtilityParser;
//import ca.cgjennings.apps.arkham.plugins.LibraryRegistry;
//import ca.cgjennings.apps.arkham.plugins.ScriptMonkey;
//import ca.cgjennings.apps.arkham.plugins.SyntaxChecker;
//import ca.cgjennings.apps.arkham.project.Member;
//import ca.cgjennings.apps.arkham.project.Project;
//import ca.cgjennings.apps.arkham.project.ProjectUtilities;
//import ca.cgjennings.apps.arkham.project.Task;
//import ca.cgjennings.apps.arkham.project.TaskGroup;
//import ca.cgjennings.script.mozilla.javascript.Parser;
//import ca.cgjennings.script.mozilla.javascript.Token;
//import ca.cgjennings.script.mozilla.javascript.ast.AstNode;
//import ca.cgjennings.script.mozilla.javascript.ast.AstRoot;
//import ca.cgjennings.script.mozilla.javascript.ast.FunctionCall;
//import ca.cgjennings.script.mozilla.javascript.ast.Name;
//import ca.cgjennings.script.mozilla.javascript.ast.NodeVisitor;
//import ca.cgjennings.script.mozilla.javascript.ast.PropertyGet;
//import ca.cgjennings.script.mozilla.javascript.ast.Scope;
//import ca.cgjennings.script.mozilla.javascript.ast.StringLiteral;
//import ca.cgjennings.script.mozilla.javascript.ast.Symbol;
//import ca.cgjennings.script.mozilla.javascript.ast.VariableDeclaration;
//import ca.cgjennings.script.mozilla.javascript.ast.VariableInitializer;
//import ca.cgjennings.ui.textedit.JSourceCodeEditor;
//import ca.cgjennings.ui.textedit.KeywordMap;
//import ca.cgjennings.ui.textedit.TokenType;
//import ca.cgjennings.ui.textedit.tokenizers.JavaScriptTokenizer;
//import gamedata.Game;
//import java.awt.Component;
//import java.io.File;
//import java.lang.reflect.AccessibleObject;
//import java.lang.reflect.Constructor;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.lang.reflect.Modifier;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeSet;
//import java.util.logging.Level;
//import javax.swing.Icon;
//import resources.Language;
//import resources.Settings;
//
///**
// * A {@linkplain CodeCompleter code completer} for JavaScript source files.
// *
// * @author Chris Jennings <https://cgjennings.ca/contact>
// * @since 3.0
// */
//public class ScriptCodeCompleter implements CodeCompleter {
//	private static final int CX_NEW = 1;
//	private static final int CX_USE_LIB = 2;
//	private static final int CX_IMP_PKG = 4;
//	private static final int CX_IMP_CLASS = 8;
//
//	/**
//	 * Creates a new JavaScript code completer.
//	 */
//	public ScriptCodeCompleter() {
//	}
//
//	@Override
//	public synchronized Set<CodeAlternative> getCodeAlternatives( JSourceCodeEditor editor ) {
//		String script = editor.getText();
//		int offset = editor.getCaretPosition();
//
//		// Step 1: Before perfoming a complete parse, check if the user
//		//         is completing a $, @, or # special variable. If so, we
//		//         can short circuit code completion.
//
//		Set<CodeAlternative> alternatives = checkForSpecialVariableCompletions( editor, script, offset );
//		if( alternatives != null ) {
//			return alternatives;
//		}
//
//		classes = new HashSet<>();
//		state = parse( editor, script, offset );
//
//		// at this point:
//		//   (1) variables defined in all the scopes that the caret
//		//       is nested within have been added to the alternatives (if there
//		//       is no context string and they match the prefix);
//		//   (2) imported classes that match the prefix have been added to
//		//       the packages variable; if there is a context string these
//		//       can be used to generate constructor/method names
//
//		// useLibrary(...), replace alternatives with installed libraries
//		if( (state.stateFlags & CX_USE_LIB) != 0 ) {
//			addLibraryNameAlternatives();
//		}
//
//		// import[Class|Package(...), replace alternatives with packages and classes
//		else if( (state.stateFlags & (CX_IMP_PKG|CX_IMP_CLASS)) != 0 ) {
//			state.alternatives.clear();
//			addImportablePackageAlternatives();
//		}
//
//		// "new" operator: filter down to capitalized function names, constructors
//		else if( (state.stateFlags & CX_NEW) != 0 ) {
//			addNewOperatorAlternatives();
//			if( state.dotexpr.isEmpty() ) addJavaScriptKeywords();
//		}
//
//		else if( state.dotexpr.isEmpty() ) {
//			addJavaScriptKeywords();
//			// add root packages
//			for( APINode n : APIDatabase.getPackageRoot().children() ) {
//				String name = n.getName() + '.';
//				String insert = name;
//				if( n instanceof PackageNode && insert != null ) {
//					add( insert, name, "package", NavigationPoint.ICON_PACKAGE, 0, true );
//				}
//			}
//		}
//
////		// check for some special, objects with known types
////		else if( state.dotexpr.equals( "Component" ) ) {
////			addEditorsOrComponents( true );
////		}
////
////		else if( state.dotexpr.equals( "Editor" ) ) {
////			addEditorsOrComponents( false );
////		}
//
//		// non-empty context:
//		//   if the prefix is a package prefix, load packages and classes
//		//   otherwise, load all matching methods, functions, etc.
//		else {
//			PackageExpressionDecoder decoder = new PackageExpressionDecoder();
//			if( decoder.remainder.length == 0 ) {
//				if( decoder.nodes.size() == 1 && ((decoder.nodes.iterator().next()) instanceof PackageNode) ) {
//					state.stateFlags |= CX_IMP_CLASS;
//					addImportablePackageAlternatives();
//				} else {
//					for( APINode node : decoder.nodes ) {
//						if( !(node instanceof ClassNode) ) {
//							StrangeEons.log.warning(null);
//							continue;
//						}
//						addStaticClassFields( (ClassNode) node );
//					}
//				}
//			} else {
//				// The dot expression is not a package description, so it
//				// may represent an instance of anything.
//
//				// Add info for all imported classes
//				for( ClassNode cn : classes ) {
//					addClassFields( cn );
//				}
//
//				// Add info for standard JS objects
//
//
//				// Add info for parsed JS objects
//
//			}
//		}
//
//		alternatives = state.alternatives;
//		classes = null;
//		return alternatives;
//	}
//
//	// use to store imported classes and packages
//	private HashSet<ClassNode> classes;
//
//	private class State {
//		State( State parent, JSourceCodeEditor editor, String text, int offset ) {
//			this.parent = parent;
//			this.editor = editor;
//			this.document = text;
//			this.offset = offset;
//
//			if( parent == null ) {
//				names = new ScriptID("");
//				alternatives = new TreeSet<>();
//			} else {
//				names = parent.names;
//				alternatives = parent.alternatives;
//				dotexpr = parent.dotexpr;
//				prefix = parent.prefix;
//			}
//		}
//
//		private String dotexpr;
//		private String prefix;
//
//		private State parent;
//		private JSourceCodeEditor editor;
//		private String document;
//		private int offset;
//		private int stateFlags;
//
//		private ScriptID names;
//		private TreeSet<CodeAlternative> alternatives;
//	}
//
//
//	private State parse( JSourceCodeEditor editor, String script, int offset ) {
//		state = new State( state, editor, script, offset );
//		boolean isRoot = state.parent == null;
//
//		if( isRoot ) {
//			scanForContext( false, false, false );
//		}
//
//		try {
//			bridge.parse( script );
//		} finally {
//			if( isRoot ) {
//				try {
//					parse( editor, ScriptMonkey.getLibrary( "common" ), 0 );
//					parse( editor, ScriptMonkey.getLibrary( "res://libraries/bootstrap" ), 0 );
//				} catch( Exception e ) {
//					StrangeEons.log.log( Level.SEVERE, null, e );
//				}
//			}
//		}
//
//		State temp = state;
//		state = state.parent;
//		return temp;
//	}
//
//	private State state;
//
//	/**
//	 * Returns <code>true</code> if the editor caret is somewhere inside the
//	 * subtree represented by this node.
//	 */
//	private boolean caretIsInNode( AstNode node ) {
//		final int offset = state.offset;
//		final int start = node.getAbsolutePosition();
//		return offset >= start && offset < start + node.getLength();
//	}
//
//	/**
//	 * Returns <code>true</code> if the caret is within the parentheses
//	 * of a function call.
//	 */
//	private boolean caretIsInParens( FunctionCall node ) {
//		int lp = node.getLp();
//		int rp = node.getRp();
//		if( lp < 0 ) return false;
//
//		final int offset = state.offset;
//		final int nodeStart = node.getAbsolutePosition();
//		final int start = nodeStart + lp;
//		if( rp >= 0 ) {
//			return offset >= start && offset < nodeStart + rp;
//		} else {
//			return offset >= start && offset < nodeStart + node.getLength();
//		}
//	}
//
//	private void visit( AstNode node ) {
//		// if the context is empty, add symbol names that match the prefix
//		if( node instanceof Scope && caretIsInNode( node ) && state.dotexpr.isEmpty() ) {
//			Map<String,Symbol> symbols = ((Scope) node).getSymbolTable();
//			if( symbols != null ) {
//				for( Symbol sym : symbols.values() ) {
//					String name = sym.getName();
//					String insert = makeInsert( name );
//					if( insert != null ) {
//						String context = "";
//						Icon icon = null;
//						int type = sym.getDeclType();
//						switch( type ) {
//							case Token.FUNCTION:
//								context = "function";
//								icon = NavigationPoint.ICON_DIAMOND;
//								break;
//							case Token.LP:
//								context = "argument";
//								icon = NavigationPoint.ICON_SQUARE_ALTERNATIVE;
//								break;
//							case Token.VAR:
//								context = "var";
//								icon = NavigationPoint.ICON_SQUARE;
//								break;
//							case Token.LET:
//								context = "let";
//								icon = NavigationPoint.ICON_SQUARE;
//								break;
//							case Token.CONST:
//								context = "const";
//								icon = NavigationPoint.ICON_SQUARE_BAR;
//								break;
//						}
//						add( insert, name, context, icon, node.depth(), false );
//					}
//				}
//			}
//		}
//
//		else if( node instanceof VariableDeclaration ) {
//			visitVariableDeclaration( (VariableDeclaration) node );
//		}
//
//		// check for import and library calls
//		else if( node instanceof FunctionCall ) {
//			FunctionCall fc = (FunctionCall) node;
//			AstNode target = fc.getTarget();
//			if( target instanceof Name ) {
//				String funcName = ((Name) target).getIdentifier();
//				if( funcName.equals( "useLibrary" ) || funcName.equals( "uselibrary" ) ) {
//					// set special state if caret is inside
//					if( state.parent == null && caretIsInParens( fc ) )	{
//						state.stateFlags |= CX_USE_LIB;
//					}
//
//					// parse included libraries to add their symbols
//					List<AstNode> args = fc.getArguments();
//					if( !args.isEmpty() ) {
//						AstNode libArg = args.get(0);
//						if( libArg instanceof StringLiteral ) {
//							String libName = ((StringLiteral) libArg).getValue();
//							if( !libName.endsWith( ".ljs" ) ) {
//								try {
//									String library = ScriptMonkey.getLibrary( libName );
//									parse( state.editor, library, 0 );
//								} catch( Exception e ) {}
//							}
//						}
//					}
//				} else if( (funcName.equals( "importPackage" ) || funcName.equals( "importClass" )) ) {
//					// set special state if caret is inside
//					if( state.parent == null && caretIsInParens( fc ) )	{
//						if( funcName.equals( "importPackage" ) ) {
//							state.stateFlags |= CX_IMP_PKG;
//						} else {
//							state.stateFlags |= CX_IMP_CLASS;
//						}
//					}
//
//					List<AstNode> args = fc.getArguments();
//					if( !args.isEmpty() ) {
//						AstNode pkgArg = args.get(0);
//						if( pkgArg instanceof PropertyGet ) {
//							StringBuilder pkgBuffer = new StringBuilder( 40 );
//							unrollDotExpression( pkgBuffer, (PropertyGet) pkgArg );
//							String pkg = pkgBuffer.toString();
//							if( pkg.startsWith( "Packages." ) ) {
//								pkg = pkg.substring( "Packages.".length() );
//							}
//							if( pkg.startsWith( "arkham." ) ) {
//								pkg = "ca.cgjennings.apps." + pkg;
//							}
//							APINode pkgNode = APIDatabase.getPackageRoot().getName( pkg );
//							if( pkgNode instanceof PackageNode ) {
//								for( APINode kid : pkgNode.children() ) {
//									if( kid instanceof ClassNode ) {
//										addClassNode( node, (ClassNode) kid );
//									}
//								}
//							} else if( pkgNode instanceof ClassNode ) {
//								addClassNode( node, (ClassNode) pkgNode );
//							}
//						}
//					}
//				}
//			}
//		}
//	}
//
//	private void visitVariableDeclaration( VariableDeclaration var ) {
//		int type = var.getType();
//		if( type == Token.LET || !var.isStatement() ) return;
//
//		for( VariableInitializer vinit : var.getVariables() ) {
//			AstNode rhs = vinit.getInitializer();
//		}
//	}
//
//	/**
//	 * Adds a class node. If there is no context, the class name will be added
//	 * as an alternative. In any case, it is added to the package set.
//	 *
//	 * @param sourceNode the node from which the class was extracted
//	 * @param classNode the class node as looked up from the database
//	 */
//	private void addClassNode( AstNode sourceNode, ClassNode classNode ) {
//		if( state.dotexpr.isEmpty() ) {
//			String insert = makeInsert( classNode.getName() );
//			if( insert != null ) {
//				String category;
//				String name = classNode.getName();
//				boolean isEnum = classNode.getJavaClass().isEnum();
//				if( isEnum ) {
//					insert += '.';
//					name += '.';
//					category = "enum class";
//				} else {
//					category = "class";
//				}
//
//				add( insert, name, category, NavigationPoint.ICON_CLUSTER, 0, isEnum );
//			}
//		}
//		classes.add( classNode );
//	}
//
//	/**
//	 * Returns the string needed to complete a name, given the current prefix.
//	 * If the specified name does not start with the current prefix, <code>null</code>
//	 * is returned. Otherwise, the suffix of the name that follows the prefix is
//	 * returned.
//	 *
//	 * @param name the name to generate an insertion string for
//	 * @return the string to insert to complete the prefix, or <code>null</code>
//	 */
//	private String makeInsert( String name ) {
//		if( name.length() >= state.prefix.length() && name.startsWith( state.prefix ) ) {
//			return name.substring( state.prefix.length() );
//		}
//		return null;
//	}
//
//	/**
//	 * Adds a new entry to the list of possible completions.
//	 *
//	 * @param insert text to insert
//	 * @param name text to display
//	 * @param category secondary text to display
//	 * @param icon icon to display
//	 * @param depth scope for sorting
//	 * @param chained if completion should fire again
//	 */
//	private void add( String insert, String name, String category, Icon icon, int depth, boolean chained ) {
//		state.alternatives.add( new DefaultCodeAlternative(
//				state.editor, insert, name, category, icon, depth, chained, state.prefix.length()
//		));
//	}
//
//	/**
//	 * Adds a new entry to the list for a method or constructor.
//	 *
//	 * @param node the node representing the class in question
//	 * @param methodOrCtor the method or constructor of the specified class
//	 */
//	private void add( ClassNode node, AccessibleObject methodOrCtor ) {
//		String name;
//		String category;
//		Icon icon;
//		Class[] params;
//		if( methodOrCtor instanceof Constructor ) {
//			Constructor c = (Constructor) methodOrCtor;
//			name = node.getJavaClass().getSimpleName();
//			category = "constructor";
//			icon = NavigationPoint.ICON_CLUSTER;
//			params = c.getParameterTypes();
//		} else {
//			Method m = (Method) methodOrCtor;
//			name = m.getName();
//			int len = name.length();
//			category = node.getJavaClass().getSimpleName() + " method";
//			icon = NavigationPoint.ICON_DIAMOND;
//			// check for static method
//			if( Modifier.isStatic(m.getModifiers()) ) {
//				icon = NavigationPoint.ICON_DIAMOND_BAR;
//			}
//			// check for getter/setter
//			else if( len >= 3 ) {
//				if( name.startsWith( "is" ) && Character.isUpperCase(name.charAt(2)) ) {
//					icon = NavigationPoint.ICON_DIAMOND_LEFT;
//				} else if( len >= 4 && Character.isUpperCase(name.charAt(3)) ) {
//					if( name.startsWith( "get" ) ) {
//						icon = NavigationPoint.ICON_DIAMOND_LEFT;
//					} else if( name.startsWith( "set" ) ) {
//						icon = NavigationPoint.ICON_DIAMOND_RIGHT;
//					}
//				}
//			}
//			params = m.getParameterTypes();
//		}
//
//		StringBuilder insertBuff = new StringBuilder( 40 );
//		StringBuilder descBuff = new StringBuilder( 60 );
//		insertBuff.append( name ).append( '(' );
//		descBuff.append( "<html>" ).append( insertBuff, 0, insertBuff.length() );
//
//		// append parameter data
//		if( params.length > 0 ) {
//			insertBuff.append( ' ' );
//			descBuff.append( ' ' );
//			String[] paramNames = node.getParameterNames( methodOrCtor );
//			for( int i=0; i<params.length; ++i ) {
//				if( i > 0 ) {
//					insertBuff.append( ", " );
//					descBuff.append( ", " );
//				}
//				insertBuff.append( paramNames[i] );
//				descBuff.append( "<font color=gray>" ).append( params[i].getSimpleName() ).append( "</font> " ).append( paramNames[i] );
//			}
//			insertBuff.append( ' ' );
//			descBuff.append( ' ' );
//		}
//		insertBuff.append( ')' );
//		descBuff.append( ')' );
//
//		String insert = makeInsert( insertBuff.toString() );
//		if( insert != null ) {
//			add( insert, descBuff.toString(), category, icon, 0, false );
//		}
//	}
//
//	/**
//	 * Given the root of a tree of nodes for dot expressions ("a.b.c.d"),
//	 * returns the dot expression as a plain string.
//	 *
//	 * @param b a string builder to store the result in
//	 * @param pg the root node
//	 */
//	private void unrollDotExpression( StringBuilder b, PropertyGet pg ) {
//		AstNode lhs = pg.getLeft();
//		AstNode rhs = pg.getRight();
//		if( lhs instanceof PropertyGet ) {
//			unrollDotExpression( b, (PropertyGet) lhs );
//		} else if( lhs instanceof Name ) {
//			appendIdentifier( b, (Name) lhs );
//		}
//		if( rhs instanceof Name ) {
//			appendIdentifier( b, (Name) rhs );
//		}
//	}
//	private void appendIdentifier( StringBuilder b, Name n ) {
//		if( b.length() > 0 ) b.append( '.' );
//		b.append( n.getIdentifier() );
//	}
//
//
//
//
//
//	@SuppressWarnings( "empty-statement" )
//	private void scanForContext( boolean allowQuotes, boolean allowPathChars, boolean allowHyphens ) {
//		// (1) if prev char from caret is JID (Java ID), then the prefix is
//		//     non-empty and continues to the first non-JID char, exclusive;
//		//     this then becomes the next "prev" char
//		// (2) if prev char is '.', then the context is non-empty and continues
//		//     until the first non-JID, non '.' charcater
//		String text = state.document;
//		int prev = state.offset-1;
//		while( prev >= 0 ) {
//			char ch = text.charAt( prev );
//			if( !isJID( ch, allowQuotes, allowPathChars, allowHyphens ) ) break;
//			--prev;
//		}
//		state.prefix = text.substring( prev+1, state.offset );
//
//		if( prev > 0 && text.charAt(prev) == '.' ) {
//			int dot = prev;
//			--prev;
//			while( prev >= 0 ) {
//				char ch = text.charAt( prev );
//				if( !(isJID( ch, allowQuotes, allowPathChars, allowHyphens ) || ch == '.') ) break;
//				--prev;
//			}
//			state.dotexpr = text.substring( prev+1, dot );
//		} else {
//			state.dotexpr = "";
//		}
//
//		// check if there is a "new" operator at the start of this context
//		if( !(allowQuotes||allowPathChars) && prev >= 3 /*&& state.context.indexOf( '(' ) < 0*/ ) {
//			if( Character.isSpaceChar( text.charAt( prev-- ) ) ) {
//				while( prev >= 2 && Character.isSpaceChar( text.charAt( prev-- ) ) );
//				if( prev >= 1 && text.charAt(prev+1) == 'w' && text.charAt(prev) == 'e' && text.charAt(prev-1) == 'n' ) {
//					state.stateFlags |= CX_NEW;
//				}
//			}
//		}
//	}
//	private static boolean isJID( char ch, boolean allowQuotes, boolean allowPathChars, boolean allowHyphens ) {
//		if( allowQuotes && (ch =='\'' || ch == '"') ) return true;
//		if( allowPathChars && (ch == '/' || ch =='\\' || ch == ':' ) ) return true;
//		if( allowHyphens && ch == '-' ) return true;
//		return Character.isJavaIdentifierPart(ch) || ch == '@' || ch == '#';
//	}
//
//
//	/**
//	 * Switches the alternatives to a list of libraries.
//	 * Called when {@link #CX_USE_LIB} context is active; i.e., when the
//	 * caret is inside a useLibrary() directive.
//	 */
//	protected void addLibraryNameAlternatives() {
//		state.alternatives.clear();
//
//		// get context, including surrounding quotes, if any
//		scanForContext( true, true, true );
//		char quote = '\'';
//		if( state.prefix.startsWith("\"") ) {
//			quote = '"';
//		}
//		HashSet<String> libs = new HashSet<>();
//		// add standard libraries
//		Collections.addAll( libs, LibraryRegistry.getLibraries() );
//
//		// look for possible libraries in the open project; if we can figure out
//		// that we are in an editor for a project file, then limit the search
//		// that file's task folder
//		Member m = getEditedProjectMemberOrNull();
//		Task t = m == null ? null : m.getTask();
//
//		if( t == null ) {
//			addLibraryNameAlternativesImpl( StrangeEons.getOpenProject(), libs );
//		} else {
//			for( Member kid : t.getChildren() ) {
//				addLibraryNameAlternativesImpl( "/", kid, libs, m );
//			}
//		}
//		for( String library : libs ) {
//			library = quote + library + quote;
//			String insert = makeInsert( library );
//			if( insert != null ) {
//				add( insert, library, "library", NavigationPoint.ICON_HEXAGON, 0, false );
//			}
//		}
//	}
//	private void addLibraryNameAlternativesImpl( TaskGroup tg, HashSet<String> libs ) {
//		if( tg != null ) {
//			for( Member m : tg.getChildren() ) {
//				if( m instanceof TaskGroup ) {
//					addLibraryNameAlternativesImpl( (TaskGroup) m, libs );
//				} else if( m instanceof Task ) {
//					for( Member kid : m.getChildren() ) {
//						addLibraryNameAlternativesImpl( "/", kid, libs, null );
//					}
//				}
//			}
//		}
//	}
//	private void addLibraryNameAlternativesImpl( String root, Member m, HashSet<String> libs, Member childToIgnore ) {
//		if( m.hasChildren() ) {
//			for( Member kid : m ) {
//				addLibraryNameAlternativesImpl( root + m.getName() + '/', kid, libs, childToIgnore );
//			}
//		} else if( ProjectUtilities.matchExtension( m, "js" ) ) {
//			if( !m.equals( childToIgnore ) ) {
//				String lib = root + m.getName();
//				if( lib.startsWith( "/resources/" ) ) {
//					lib = "res://" + lib.substring( "/resources/".length() );
//				} else {
//					lib = "res:/" + lib;
//				}
//				libs.add( lib );
//			}
//		}
//	}
//
//	private Member getEditedProjectMemberOrNull() {
//		Project p = StrangeEons.getOpenProject();
//		if( p == null ) return null;
//		Component c = state.editor;
//		while( c != null ) {
//			c = c.getParent();
//			if( c instanceof AbstractStrangeEonsEditor ) {
//				File f = ((AbstractStrangeEonsEditor) c).getFile();
//				return p.findMember( f );
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Add member fields and methods from a particular class.
//	 * @param classNode the class node to add from
//	 */
//	private void addClassFields( ClassNode classNode ) {
//		for( Method m : classNode.getMethods() ) {
//			String insert = makeInsert( m.getName() );
//			if( insert != null ) {
//				add( classNode, m );
//			}
//		}
//		for( Field f : classNode.getStaticFields() ) {
//			String insert = makeInsert( f.getName() );
//			if( insert != null ) {
//				add( insert, f.getName(), f.getType().getSimpleName(), NavigationPoint.ICON_SQUARE_ALTERNATIVE_BAR, 0, false );
//			}
//		}
//	}
//
//	/**
//	 * Add static fields and methods from a particular class.
//	 * @param classNode the class node to add from
//	 */
//	private void addStaticClassFields( ClassNode classNode ) {
//		for( Method sm : classNode.getStaticMethods() ) {
//			String insert = makeInsert( sm.getName() );
//			if( insert != null ) {
//				add( classNode, sm );
//			}
//		}
//		for( String e : classNode.getEnumNames() ) {
//			String insert = makeInsert( e );
//			if( insert != null ) {
//				add( insert, e, "enum", NavigationPoint.ICON_SQUARE_BAR, 0, false );
//			}
//		}
//		for( Field f : classNode.getStaticFields() ) {
//			String insert = makeInsert( f.getName() );
//			if( insert != null ) {
//				add( insert, f.getName(), f.getType().getSimpleName(), NavigationPoint.ICON_SQUARE_ALTERNATIVE_BAR, 0, false );
//			}
//		}
//		for( APINode n : classNode.children() ) {
//			if( !(n instanceof ClassNode) ) continue;
//			String insert = makeInsert( n.getName() );
//			if( insert != null ) {
//				add( insert, n.getName(), "class", NavigationPoint.ICON_CLUSTER, 0, false );
//			}
//		}
//	}
//
//
//
//
//	/**
//	 * Switches the alternatives to a list of packages and classes.
//	 * Called when {@link #CX_IMP_CLASS} or {@link #CX_IMP_PKG} is active; i.e.,
//	 * when the caret is in an importClass or importPackage directive.
//	 * Also called when {@link #CX_NEW} flag is set to load packages and
//	 * constructors. In this case <code>newOperatorMode</code> will be
//	 * <code>true</code>.
//	 */
//	private void addImportablePackageAlternatives() {
//		boolean newOperatorMode = (state.stateFlags & CX_NEW) != 0;
//
//		// special case: offer to complete "Packages" only if partially typed
//		if( state.dotexpr.isEmpty() && !state.prefix.isEmpty() && makeInsert( "Packages" ) != null ) {
//			add( makeInsert( "Packages." ), "Packages.", "const", NavigationPoint.ICON_SQUARE_BAR, 0, true );
//			return;
//		}
//
//		// look up the prefix to get a parent node
//		String parentName = normalizePackageContext( state.dotexpr );
//		APINode parent = APIDatabase.getPackageRoot().getName( parentName );
//		if( parent == null ) return;
//
//		if( (state.stateFlags & CX_IMP_CLASS) != 0 ) {
//			for( APINode kid : parent.children() ) {
//				boolean isPackageNode = kid instanceof PackageNode;
//				String name = kid.getName();
//				if( isPackageNode ) name += '.';
//				String insert = makeInsert( name );
//				if( insert != null ) {
//					Icon icon;
//					String category;
//					if( isPackageNode ) {
//						category = "package";
//						icon = NavigationPoint.ICON_PACKAGE;
//					} else {
//						if( newOperatorMode ) {
//							category = "constructor";
//						} else {
//							category = "class";
//						}
//						icon = NavigationPoint.ICON_CLUSTER;
//					}
//
//					if( newOperatorMode && !isPackageNode ) {
//						ClassNode classNode = (ClassNode) kid;
//						Class klass = classNode.getJavaClass();
//						int m = klass.getModifiers();
//						if( !Modifier.isPublic(m) || (klass.isMemberClass() && !Modifier.isStatic(m)) ) continue;
//
//						for( Constructor c : classNode.getConstructors() ) {
//							add( classNode, c  );
//						}
//					} else {
//						add( insert, name, category, icon, 0, isPackageNode );
//					}
//				}
//			}
//		} else {
//			for( APINode kid : parent.children() ) {
//				if( kid instanceof PackageNode ) {
//					boolean hasPackageChild = false;
//					for( APINode gkid : kid.children() ) {
//						if( gkid instanceof PackageNode ) {
//							hasPackageChild = true;
//							break;
//						}
//					}
//					String name = kid.getName();
//					if( hasPackageChild ) name += '.';
//					String insert = makeInsert( name );
//					if( insert != null ) {
//						add( insert, name, "package", NavigationPoint.ICON_PACKAGE, 0, hasPackageChild );
//					}
//				}
//			}
//		}
//	}
//
//	private static String normalizePackageContext( String packageBase ) {
//		if( packageBase.equals( "Packages" ) ) {
//			packageBase = "";
//		} else if( packageBase.startsWith( "Packages." ) ) {
//			packageBase = packageBase.substring( "Packages.".length() );
//		} else if( packageBase.equals( "arkham" ) || packageBase.startsWith( "arkham." ) ) {
//			packageBase = "ca.cgjennings.apps." + packageBase;
//		}
//		return packageBase;
//	}
//
//	private void addNewOperatorAlternatives() {
//		TreeSet<CodeAlternative> newAlts = new TreeSet<>();
//		for( CodeAlternative ca : state.alternatives ) {
//			if( ca.getCategory().equals( "function" ) ) {
//				String name = ca.toString();
//				char start = name.isEmpty() ? '\0' : name.charAt(0);
//				if( Character.isUpperCase( start ) ) {
//					newAlts.add( ca );
//				}
//			}
//		}
//		state.alternatives = newAlts;
//		state.stateFlags |= CX_IMP_CLASS;
//		// load packages and pkg.pkg.Class entries
//		addImportablePackageAlternatives();
//		// load classes imported into global scope
//		if( state.dotexpr.isEmpty() ) {
//			for( ClassNode node : classes ) {
//				Class klass = node.getJavaClass();
//				int m = klass.getModifiers();
//				if( !Modifier.isPublic(m) || (klass.isMemberClass() && !Modifier.isStatic(m)) ) continue;
//				for( Constructor c : node.getConstructors() ) {
//					add( node, c );
//				}
//			}
//		}
//		state.stateFlags &= ~(CX_IMP_CLASS|CX_IMP_PKG);
//	}
//
//
//
//
//	/**
//	 * Checks if a $, @, or # variable is being completed, and if so returns
//	 * a set of alternatives. Otherwise returns <code>null</code>. This is
//	 * called as a first step before performing a full parse of the script.
//	 * If it returns a non-<code>null</code> set, code completion is short
//	 * circuited (a full parse is not performed).
//	 *
//	 * @param editor the editor being completed
//	 * @param script the script being completed
//	 * @param offset the offset into the script at which completion should occur
//	 * @return a set of special variable alternatives, or <code>null</code>
//	 */
//	private Set<CodeAlternative> checkForSpecialVariableCompletions( JSourceCodeEditor editor, String script, int offset ) {
//		Set<CodeAlternative> alternatives = null;
//		// create a dummy state instance for gathering the prefix and context
//		state = new State( null, editor, script, offset );
//		scanForContext( false, false, true );
//		// check for $, @, # variables
//		if( state.dotexpr.isEmpty() && !state.prefix.isEmpty() ) {
//			char start = state.prefix.charAt(0);
//			if( start == '$' ) {
//				addSettingAlternatives();
//				alternatives = state.alternatives;
//			} else if( start == '@' ) {
//				addStringAlternatives( true );
//				alternatives = state.alternatives;
//			} else if( start == '#' ) {
//				addStringAlternatives( false );
//				alternatives = state.alternatives;
//			}
//		}
//		// reset state so we're ready to begin the deep scan
//		state = null;
//		return alternatives;
//	}
//
//	/**
//	 * Adds alternatives for <code>$setting</code> variables that match the current
//	 * prefix string.
//	 */
//	protected void addSettingAlternatives() {
//		for( Game g : Game.getGames( true ) ) {
//			addSettingAlternativesImpl( g.getMasterSettings(), false, g.getCode() );
//		}
//		addSettingAlternativesImpl( Settings.getUser(), true, null );
//	}
//	private void addSettingAlternativesImpl( Settings s, boolean deep, String suffix ) {
//		Set<String> keys = deep ? s.getVisibleKeySet() : s.getKeySet();
//		String context = "setting";
//		if( suffix != null ) context += " (" + suffix + ')';
//		for( String key : keys ) {
//			key = '$' + key;
//			String insert = makeInsert( key );
//			if( insert != null ) {
//				add( insert, key, context, NavigationPoint.ICON_TRIANGLE, 0, false );
//			}
//		}
//	}
//
//	/**
//	 * Adds alternatives for <code>@string</code> or <code>#string</code>
//	 * variables that match the current prefix string.
//	 *
//	 * @param ui if <code>true</code>, adds interface string alternatives;
//	 *   otherwise adds game string alternatives
//	 */
//	private void addStringAlternatives( boolean ui ) {
//		final char PREFIX_CHAR = ui ? '@' : '#';
//		Language language = ui ? Language.getInterface() : Language.getGame();
//		for( String key : language.keySet() ) {
//			String insert = makeInsert( PREFIX_CHAR + key );
//			if( insert != null ) {
//				String val = String.format( "%-20.20s", language.get( key ) );
//				add( insert, PREFIX_CHAR + key, val, NavigationPoint.ICON_TRIANGLE, 0, false );
//			}
//		}
//	}
//
//
//	/**
//	 * Attempts to decode a package dot expression. If the expression matches
//	 * a package or class, that single entity will be stored in the match set.
//	 * Once a class is reached, if there are additional identifiers, that class's
//	 * fields and methods will be searched for matching names. This continues
//	 * recursively, using the possible return types for each matching name. This
//	 * may result in a set of possible matching classes.
//	 */
//	private class PackageExpressionDecoder {
//		public PackageExpressionDecoder() {
//			String normalized = normalizePackageContext( state.dotexpr );
//			String[] segments = normalized.split( "\\s*\\.\\s*", -1 );
//			APINode node = APIDatabase.getPackageRoot();
//			int i=0;
//			for( ; i<segments.length; ++i ) {
//				APINode child = node.find( segments[i] );
//				if( child == null ) {
//					// check first segment against imported classes before giving up
//					if( i == 0 ) {
//						for( ClassNode cn : ScriptCodeCompleter.this.classes ) {
//							if( cn.getName().equals( segments[0] ) ) {
//								child = cn;
//								break;
//							}
//						}
//						if( child == null ) break;
//					} else {
//						break;
//					}
//				}
//				node = child;
//			}
//			if( node != APIDatabase.getPackageRoot() ) {
//				nodes.add( node );
//				if( i < segments.length && node instanceof ClassNode ) {
//					for( ; i<segments.length; ++i ) {
//						Set<APINode> newNodes = findStaticMatches( segments[i], nodes );
//						if( newNodes.isEmpty() ) break;
//						nodes = newNodes;
//					}
//				}
//			}
//			if( i < segments.length ) {
//				remainder = Arrays.copyOfRange( segments, i, segments.length, String[].class );
//			} else {
//				remainder = new String[0];
//			}
//		}
//
//		private Set<APINode> findMatches( String name, Set<APINode> sources ) {
//			HashSet<APINode> out = new HashSet<>();
//			return out;
//		}
//
//		private Set<APINode> findStaticMatches( String name, Set<APINode> sources ) {
//			HashSet<APINode> out = new HashSet<>();
//
//			for( APINode source : sources ) {
//				ClassNode cn = (ClassNode) source;
//				APINode mclass = cn.find( name );
//				if( mclass != null ) out.add( mclass );
//				for( Field f : cn.getStaticFields() ) {
//					if( name.equals( f.getName() ) ) {
//						out.add( APIDatabase.getPackageRoot().get( f.getType() ) );
//					}
//				}
//				for( Method m : cn.getStaticMethods() ) {
//					if( name.equals( m.getName() ) ) {
//						out.add( APIDatabase.getPackageRoot().get( m.getReturnType() ) );
//					}
//				}
//				for( String e : cn.getEnumNames() ) {
//					if( name.equals( e ) ) {
//						out.add( cn );
//					}
//				}
//
//				out.remove( null );
//			}
//
//			return out;
//		}
//
//
//		/** The deepest decoded class(es). */
//		public Set<APINode> nodes = new HashSet<APINode>();
//		/** The unused dot segments following the deepest node, or an empty array. */
//		public String[] remainder;
//	}
//
//
//
//	private void addJavaScriptKeywords() {
//		boolean newOperator = (state.stateFlags & CX_NEW) != 0;
//		KeywordMap map = JavaScriptTokenizer.getKeywords();
//		for( String k : map.getKeySet() ) {
//			String insert = makeInsert( k );
//			if( insert != null ) {
//				TokenType type = map.get( k );
//				if( type != TokenType.INVALID ) {
//					boolean isNewCompatible = false;
//					Icon icon;
//					String category;
//					if( type == TokenType.KEYWORD1 ) {
//						icon = NavigationPoint.ICON_CIRCLE_SMALL;
//						category = "keyword";
//						switch( k ) {
//							case "catch":
//							case "for":
//							case "if":
//							case "switch":
//							case "with":
//							case "while":
//								k += "(  )";
//								insert += "(  )";
//								break;
//						}
//					} else if( Character.isUpperCase( k.charAt(0) ) ) {
//						icon = NavigationPoint.ICON_DIAMOND_BAR;
//						category = "constructor";
//						isNewCompatible = true;
//					} else {
//						switch( k ) {
//							case "arguments":
//								icon = NavigationPoint.ICON_SQUARE;
//								category = "object";
//								break;
//							default:
//								icon = NavigationPoint.ICON_DIAMOND_BAR;
//								category = "function";
//								k += "(  )";
//								insert += "(  )";
//						}
//					}
//
//					if( newOperator ) {
//						if( !isNewCompatible ) {
//							continue;
//						}
//						k += "(  )";
//						insert += "(  )";
//					}
//					add( insert, k, category, icon, 0, false );
//				}
//			}
//		}
//	}
//
//
//	/**
//	 * This class provides the basic parsing infrastructure needed by
//	 * {@link ScriptCodeCompleter}. It extends {@link SyntaxChecker} so that it
//	 * uses the same parser configuration as that class.
//	 */
//	private final class ParserBridge extends AbstractUtilityParser implements NodeVisitor {
//		public ParserBridge() {
//		}
//		@Override
//		protected void processTree( AstRoot rootNode ) {
//			if( rootNode != null ) {
////				System.out.println( rootNode.debugPrint() );
//				rootNode.visit( this );
//			}
//		}
//		@Override
//		public boolean visit( AstNode node ) {
//			ScriptCodeCompleter.this.visit( node );
//			return true;
//		}
//	}
//	private ParserBridge bridge = new ParserBridge();
//}
