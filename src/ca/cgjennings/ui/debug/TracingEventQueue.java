package ca.cgjennings.ui.debug;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.event.InvocationEvent;

/**
 * Monitor events being dispatched to application.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class TracingEventQueue extends EventQueue {

    private int maxTrace;

    public TracingEventQueue() {
        this(8);
    }

    public TracingEventQueue(int maxStackTraceLength) {
        super();
        if (maxStackTraceLength < 1) {
            throw new IllegalArgumentException("maxStackTraceLength < 1: " + maxStackTraceLength);
        }
        maxTrace = maxStackTraceLength;
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        final boolean watch = isEventOfInterest(event);
        if (watch) {
            Exception tracer = new Exception();
            tracer.fillInStackTrace();

            StringBuilder b = new StringBuilder("**** Event stack ****\n");
            b.append(event).append('\n');
            b.append('[').append(event.getSource()).append("]\n");
            StackTraceElement[] stack = tracer.getStackTrace();
            for (int i = 0; i < stack.length && i < maxTrace; ++i) {
                b.append('\t');
                b.append(stack[i].getClassName() + ".");
                b.append(stack[i].getMethodName() + " [");
                b.append(stack[i].getLineNumber() + "]");
                b.append('\n');
            }
            System.err.println(b.toString());
        }

        long start = System.nanoTime();
        super.dispatchEvent(event);
        long stop = System.nanoTime();
        stop = (stop - start + 500_000L) / 1_000_000L;

        if (watch && stop > 500) {
            System.err.println("\tTime to process (ms): " + stop + "\n");
        }
    }

    public boolean isEventOfInterest(AWTEvent event) {
        // ignore Swing Timer events
        if (event instanceof InvocationEvent) {
            if (event.paramString().contains("javax.swing.Timer$DoPostEvent")) {
                return false;
            }
        }
        return true;
    }

    public void install() {
        java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().push(this);
    }

    public static void installDefault() {
        new TracingEventQueue().install();
    }
}
