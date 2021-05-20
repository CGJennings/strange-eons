package ca.cgjennings.apps.arkham.plugins.typescript;

import ca.cgjennings.apps.arkham.StrangeEons;
import java.awt.EventQueue;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * The primary method of accessing TypeScript language services.
 * This class allows you to hand off service requests to be performed in
 * the background. A callback that you provide will be called with
 * the result once it is available.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class TypeScript {
    private TypeScript() {
    }

    /**
     * Gets a string describing which version of TypeScript is being used.
     *
     * @param callback will be called with a non-null version string,
     *    such as "4.2.0-dev"
     */
    public static void getVersion(Consumer<String> callback) {
        post(RequestType.VERSION, callback);
    }

    /**
     * Transpiles the specified source code from TypeScript to JavaScript,
     * using default options.
     *
     * @param source the non-null code to compile
     * @param callback the callback to invoke with the result when ready
     */
    public static void transpile(String source, Consumer<String> callback) {
        post(RequestType.TRANSPILE, callback, Objects.requireNonNull(source, "source"));
    }

    /**
     * Restarts the service provider. This can be called during development
     * to pick up changes to the service implementation without restarting
     * the app.
     */
    public static void restart() {
        post(RequestType.RELOAD, null);
    }

    /** The types of request that can be posted. */
    private static enum RequestType {
        VERSION,
        TRANSPILE,
        RELOAD
    }

    /** Queues up a request to be performed later in the background. */
    private static void post(RequestType type, Consumer<?> callback, Object... args) {
        queue.add(new Request(Objects.requireNonNull(type), callback, args));
        StrangeEons.log.log(Level.INFO, "posted {0} request", type);
    }

    /**
     * Code that performs posted requests in a separate thread in the order that
     * they are received, then sends the result to the specified callback.
     */
    private static void requestHandler() {
        try {
            ts = new TypeScriptServiceProvider();
            TypeScriptServices tss = ts.getServices();
            for(;;) {
                Request next = queue.poll(365L, TimeUnit.DAYS);
                if(next == null) continue;
                StrangeEons.log.log(Level.INFO, "received {0} request", next.type);
                switch(next.type) {
                    case VERSION:
                        reply(next, tss.getVersion());
                        break;
                    case TRANSPILE:
                        reply(next, tss.transpile((String)next.args[0]));
                        break;
                    case RELOAD:
                        ts = new TypeScriptServiceProvider();
                        tss = ts.getServices();
                        break;
                    default:
                        throw new AssertionError();
                }
            }
        } catch(InterruptedException ie) {
            StrangeEons.log.warning("thread forced to exit");
        } catch(Exception ex) {
            StrangeEons.log.log(Level.SEVERE, "service error", ex);
        }
    }
    
    /** Posts a string result back to the caller. */
    @SuppressWarnings("unchecked")
    private static void reply(Request r, String s) {
        EventQueue.invokeLater(()->((Consumer<String>)r.callback).accept(s));
    }

    private static TypeScriptServiceProvider ts;
    private static final BlockingQueue<Request> queue = new LinkedBlockingQueue<>();
    private static final Thread handler;
    static {
        handler = new Thread(TypeScript::requestHandler, "TypeScript service provider");
        handler.setDaemon(true);
        handler.start();
    }

    private static class Request {
        Request(RequestType type, Consumer<?> callback, Object[] args) {
            this.type = type;
            this.callback = callback;
            this.args = args;
        }
        public RequestType type;
        public Object[] args;
        public Consumer<?> callback;
    }
}
