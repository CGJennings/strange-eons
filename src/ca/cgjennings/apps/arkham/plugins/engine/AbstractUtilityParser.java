package ca.cgjennings.apps.arkham.plugins.engine;

import ca.cgjennings.apps.arkham.StrangeEons;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ErrorCollector;
import java.util.logging.Level;

/**
 * This abstract base class is used to build tools that aid script code
 * developers. It parses script files into an abstract syntax tree and then  {@linkplain #processTree(org.mozilla.javascript.ast.AstRoot)
 * provides the tree root} to the concrete subclass.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.0
 */
public abstract class AbstractUtilityParser {

    /**
     * Creates a new utility parser.
     */
    public AbstractUtilityParser() {
    }

    /**
     * Parses the specified script.
     *
     * @param script the script to process
     */
    public void parse(String script) {
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        Context cx = ContextFactory.getGlobal().enterContext();
        try {
            compilerEnv.initFromContext(cx);
            compilerEnv.setLanguageVersion(Context.VERSION_ES6);
            compilerEnv.setOptimizationLevel(-1);
            compilerEnv.setGenerateDebugInfo(false);
            compilerEnv.setGenerateObserverCount(false);
            compilerEnv.setRecoverFromErrors(true);
            compilerEnv.setGeneratingSource(true);
            compilerEnv.setStrictMode(true);
            compilerEnv.setIdeMode(true);

            ErrorReporter err;
            synchronized (this) {
                if (reporter == null) {
                    reporter = new NoOpErrorReporter();
                }
                err = reporter;
            }

            compilerEnv.setErrorReporter(err);
            Parser p = new Parser(compilerEnv, err);

            AstRoot root = null;
            try {
                root = p.parse(script, null, 1);
            } catch (Throwable t) {
                StrangeEons.log.log(Level.SEVERE, "unexpected parser exception", t);
            }

            processTree(root);
        } finally {
            Context.exit();
        }
    }

    /**
     * Sets an error reporter that will be used to collect information about any
     * syntax errors and warnings that are found during parsing. If no error
     * reporter is set explicitly, then a new {@link NoOpErrorReporter} will be
     * set automatically the first time that a script is
     * {@linkplain #parse(java.lang.String) parsed}.
     *
     * @param reporter the reporter to use; instances of {@link ErrorCollector}
     * will have the IDE versions of the error methods called
     */
    protected synchronized void setErrorReporter(ErrorReporter reporter) {
        this.reporter = reporter;
    }

    private ErrorReporter reporter;

    /**
     * This is called after each script is parsed to allow the utility access to
     * the resulting abstract syntax tree. In the event of a serious parser
     * error that prevents the tree from being generated, the root node will be
     * {@code null}.
     *
     * @param rootNode the root of the parse tree
     */
    protected abstract void processTree(AstRoot rootNode);
}
