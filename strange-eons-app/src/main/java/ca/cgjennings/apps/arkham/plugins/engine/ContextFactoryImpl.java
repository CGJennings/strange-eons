package ca.cgjennings.apps.arkham.plugins.engine;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;

final class ContextFactoryImpl extends ContextFactory {

    public ContextFactoryImpl() {
    }

    @Override
    protected Context makeContext() {
        final boolean standardThread = SEScriptEngineFactory.isStandardThread();
        Context cx = super.makeContext();
        final int opt = standardThread ? SEScriptEngineFactory.getOptimizationLevel() : -1;
        cx.setOptimizationLevel(opt);
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setGeneratingSource(opt < 2);
        cx.setInstructionObserverThreshold(0);
        cx.setGenerateObserverCount(false);
        if (opt == -1) {
            cx.setMaximumInterpreterStackDepth(1_000);
        }
        if (SEScriptEngineFactory.debugInfoEnabled) {
            cx.setGeneratingDebug(true);
        }
        if (SEScriptEngineFactory.warningsEnabled && standardThread) {
            ErrorReporter er = WarningErrorReporter.getShared(cx.getErrorReporter());
            cx.setErrorReporter(er);
        }
        return cx;
    }

    @Override
    protected boolean hasFeature(Context cx, int featureIndex) {
        boolean enable;
        switch (featureIndex) {
            case Context.FEATURE_E4X:
                enable = false;
                break;
            case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
            case Context.FEATURE_INTEGER_WITHOUT_DECIMAL_PLACE:
            case Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME:
                enable = true;
                break;
            case Context.FEATURE_STRICT_EVAL:
            case Context.FEATURE_STRICT_VARS:
            case Context.FEATURE_STRICT_MODE:
                enable = SEScriptEngineFactory.isStandardThread();
                break;
            case Context.FEATURE_WARNING_AS_ERROR:
                enable = SEScriptEngineFactory.getWarningsAreTreatedAsErrors();
                break;
            default:
                enable = super.hasFeature(cx, featureIndex);
        }
        return enable;
    }

}
