package ca.cgjennings.apps.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Prevents multiple instances of a program from running. On startup, the
 * class's {@code makeExclusive} method will be called with any file parameters
 * from the command line. This method will attempt to create a server on a
 * certain port.
 *
 * If it fails, it assumes a previous instance has already registered the port.
 * In this case, it sends a message to the existing server to identify itself
 * and to specify which file(s) to were requested on this instance's command
 * line.
 *
 * This method will return {@code true} if the calling instance should keep
 * running after the method returns, or {@code false} if it should stop. A
 * {@code false} return value indicates that the program arguments were
 * successfully sent to an instance that is already running. A value of
 * {@code true} indicates that the current instance is the first, exclusive
 * instance, or that the method is unable to verify whether an existing instance
 * is running.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class InstanceController implements Runnable {

    private InstanceController() {
    }

    public static boolean makeExclusive(String programName, int port, String[] args, InstanceControllerListener listener) {
//	if( thread != null ) throw new IllegalStateException( "Must call stopExclusion before calling makeExclusive again" );

        try {
            server = new ServerSocket(port, 50, InetAddress.getLoopbackAddress());
            server.setSoTimeout(250);
        } catch (BindException b) {
            // another instance is probably running; try sending it commands
            try {
                String response;
                try (Socket client = new Socket(InetAddress.getLoopbackAddress(), port)) {
                    client.setSoTimeout(60 * 1_000);
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    DataInputStream in = new DataInputStream(client.getInputStream());
                    out.writeUTF(MAGIC);
                    out.writeUTF(programName);
                    out.writeInt(args.length);
                    for (String arg : args) {
                        out.writeUTF(arg);
                    }   response = in.readUTF();
                    in.close();
                    out.close();
                }

                if (OK.equals(response)) {
                    return false;
                }

                callListener(listener, true, args);
                return true;
            } catch (IOException e) {
                // we don't know what happened, but something went wrong:
                // better run this instance to be sure
                return true;
            }
        } catch (IOException e) {
            return true;
        }

        // we have been able to bind the port, now we will start a server
        // thread to listen for messages from other instances
        InstanceController.programName = programName;
        InstanceController.listener = listener;
        callListener(listener, true, args);

        thread = new Thread(new InstanceController());
        thread.setDaemon(true);
        thread.start();

        return true;
    }

    protected static void callListener(InstanceControllerListener l, boolean initial, String[] args) {
        try {
            l.parseInstanceArguments(initial, args);
        } catch (Throwable t) {
            System.err.println("InstanceControllerListener: exception while parsing arguments");
            t.printStackTrace();
        }
    }

    public static void stopExclusion() {
        if (thread != null) {
            thread.interrupt();
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }
            }
            thread = null;
        }
    }

    @Override
    public void run() {
        Socket socket;
        while (true) {
            socket = null;
            try {
                socket = server.accept();
            } catch (SocketTimeoutException e) {
            } catch (IOException e) {
            }

            if (thread.isInterrupted()) {
                try {
                    server.close();
                    thread = null;
                } catch (IOException e) {
                }
                return;
            }

            if (socket != null) {
                DataOutputStream out = null;
                DataInputStream in = null;
                try {
                    socket.setSoTimeout(60 * 1_000);
                    out = new DataOutputStream(socket.getOutputStream());
                    in = new DataInputStream(socket.getInputStream());

                    if (in.readUTF().equals(MAGIC) && in.readUTF().equals(programName)) {
                        int arglen = in.readInt();
                        String[] args = new String[arglen];
                        for (int i = 0; i < arglen; ++i) {
                            args[i] = in.readUTF();
                        }

                        callListener(listener, false, args);
                        out.writeUTF(OK);
                    }
                } catch (Exception e) {
                    // ...
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                        if (in != null) {
                            out.close();
                        }
                        socket.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private static Thread thread = null;
    private static ServerSocket server;
    private static InstanceControllerListener listener;
    private static String programName;
    private static String MAGIC = "InstanceController.makeExclusive";
    private static String OK = "OK. Please die now.";

//    public static void main( String[] args ) {
//	InstanceControllerListener ic = new InstanceControllerListener() {
//	    public boolean parseInstanceArguments( boolean isInitialInstance, String[] args ) {
//		for( String arg : args ) {
//		    System.out.println( "argument: " + arg );
//		}
//		return true;
//	    }
//	};
//
//	System.out.println( "Testing:" );
//	System.out.println( makeExclusive( "test", 8081, args, ic ) );
//	System.out.println( makeExclusive( "test", 8081, args, ic ) );
//    }
}
