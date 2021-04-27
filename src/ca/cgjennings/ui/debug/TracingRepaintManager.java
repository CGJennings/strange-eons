package ca.cgjennings.ui.debug;

import javax.swing.JComponent;
import javax.swing.RepaintManager;

/**
 * Can be used to track down the cause of excessive repaints. Call the static
 * installDefault method for simple use. Override isComponentOfInterest and call
 * install for more fine-grained use.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TracingRepaintManager extends RepaintManager {

    private int maxTrace;

    public TracingRepaintManager() {
        this(8);
    }

    public TracingRepaintManager(int maxStackTraceLength) {
        super();
        if (maxStackTraceLength < 1) {
            throw new IllegalArgumentException("maxStackTraceLength < 1: " + maxStackTraceLength);
        }
        maxTrace = maxStackTraceLength;
    }

    @Override
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        if (isComponentOfInterest(c)) {
            Exception tracer = new Exception();
            tracer.fillInStackTrace();

            StringBuilder b = new StringBuilder("**** Repaint stack ****\n");
            StackTraceElement[] stack = tracer.getStackTrace();
            for (int i = 0; i < stack.length && i < maxTrace; ++i) {
                b.append("\t");
                b.append(stack[i].getClassName() + ".");
                b.append(stack[i].getMethodName() + " [");
                b.append(stack[i].getLineNumber() + "]");
                b.append("\n");
            }
            System.err.println(b.toString());
        }

        super.addDirtyRegion(c, x, y, w, h);
    }

    public void install() {
        RepaintManager.setCurrentManager(this);
    }

    public static void installDefault() {
        RepaintManager.setCurrentManager(new TracingRepaintManager());
    }

    public boolean isComponentOfInterest(JComponent c) {
        return true;
    }
}
