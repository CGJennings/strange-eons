package ca.cgjennings.apps.arkham.plugins.debugging;

import ca.cgjennings.algo.SplitJoin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Searches for available debug servers.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public final class DiscoveryService {
    /** Provides information about discovered debug servers. */
    public static final class ServerInfo {
        private ServerInfo(InetAddress address, int port, String pid, String hash, String buildNumber, String version, String testBundle) {
            this.address = address;
            this.port = port;
            this.pid = pid;
            this.hash = hash;
            this.buildNumber = buildNumber;
            this.version = version;
            this.testBundle = testBundle;
        }

        /** Address needed to connect to the server. */
        public final InetAddress address;
        /** Port needed to connect to the server. */
        public final int port;
        /**
         * Process ID of the app instance debugged by the server,
         * if available.
         */
        public final String pid;
        /**
         * Hash of the app instance address debugged by the server;
         * the hash and/or process id can uniquely identify an app instance.
         */
        public final String hash;
        /** Build number for the app instance debugged by the server. */
        public final String buildNumber;
        /** Version string for the app instance debugged by the server. */
        public final String version;
        /**
         * If the app instance debugged by the server is running in plug-in
         * test mode, this is the bundle or bundle list passed on the
         * command line.
         */
        public final String testBundle;

        @Override
        public String toString() {
            return "" + pid + ':' + hash + " build " + buildNumber + " (" + version + ')' +
                    (testBundle == null || testBundle.isEmpty() ? "" : " testing " + testBundle)
            ;
        }
    };


    /**
     * Create a new instance that will scan for servers running on the local
     * device.
     */
    public DiscoveryService() {
        local = hostsToTest = getLocalHostAddresses(false);
    }

    /**
     * Create a new instance that will scan for servers on the specified
     * hosts. Note that testing remote hosts is typically much slower
     * than testing local hosts.
     *
     * @param hosts Non-null array of hosts to test.
     */
    public DiscoveryService(InetAddress... hosts) {
        if(hosts == null) {
            throw new IllegalArgumentException("null hosts");
        }
        hostsToTest = new LinkedHashSet<>();
        for(InetAddress host : hosts) {
            if(host == null) {
                throw new IllegalArgumentException("null host");
            }
            hostsToTest.add(host);
        }
        local = getLocalHostAddresses(true);
        local.retainAll(hostsToTest);

        // check if they are equivalent, and if so use the same ref for both
        // to short circuit some checks later
        if(local != hostsToTest && local.size() == hostsToTest.size() && local.containsAll(hostsToTest)) {
            hostsToTest = local;
        }
    }

    private Set<InetAddress> local;
    private Set<InetAddress> hostsToTest;

    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

    /**
     * Returns the hosts to be searched.
     * @return non-null array of hosts, possibly empty
     */
    public InetAddress[] getHosts() {
        return hostsToTest.toArray(new InetAddress[hostsToTest.size()]);
    }

    /**
     * Lists local network addresses, including the loopback address.
     *
     * @param allAdapterAddresses If true, all addresses associated with each
     *  network interface are included (for example, an adapter may have both
     *  an IPv4 and IPv6 address; this would include both).
     * @return a set of local host addresses
     */
    private static Set<InetAddress> getLocalHostAddresses(boolean allAdapterAddresses) {
        Set<InetAddress> localAddr = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> netFaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(netFaces)) {
                if(!iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> netAddr = iface.getInetAddresses();
                if(allAdapterAddresses) {
                    localAddr.addAll(Collections.list(netAddr));
                } else if(netAddr.hasMoreElements()) {
                    localAddr.add(netAddr.nextElement());
                }
            }
        } catch(SocketException ex) {
            // could not get list of interfaces, no network stack?
        }
        return localAddr;
    }

    /**
     * If possible, quickly rule out the port as a possible debug server.
     * If the host address is local, and the socket is available, it can't
     * have a debug server on it.
     *
     * @param host the host to check
     * @param port the port to check
     * @return true if the port can be ruled out, false if it needs further probing
     */
    private boolean canRuleOutQuickly(InetAddress host, int port) {
        // if host is local...
        if(local == hostsToTest || local.contains(host)) {
            // ...see if the port is available
            try {
                ServerSocket ss = new ServerSocket(port, 0, host);
                ss.close();
                return true;
            } catch(IOException ex) {
                // will return false
            }
        }
        return false;
    }

    /**
     * Searches hosts for debug servers, returning information about each
     * one found.
     *
     * @return non-null list of objects that describe discovered servers
     */
    public List<ServerInfo> search() {
        List<ServerInfo> hits = new LinkedList<>();
        List<ServerInfo> synchHits = Collections.synchronizedList(hits);

        final int PORTS_PER_HOST = MAX_PORT - MIN_PORT + 1;
        Runnable[] jobs = new Runnable[PORTS_PER_HOST * hostsToTest.size()];

        int i=0;
        for(InetAddress host : hostsToTest) {
            final InetAddress theHost = host;
            for(int port = MIN_PORT; port <= MAX_PORT; ++port) {
                final int thePort = port;
                jobs[i++] = () -> {
                    ServerInfo hit = testPort(theHost, thePort);
                    if(hit != null) synchHits.add(hit);
                };
            }
        }

        SplitJoin.getInstance().runUnchecked(jobs);
        return hits;
    }

    /**
     * Tests if the port is a debug server on the specified host.
     *
     * @param host the host to test
     * @param port the port on the host to test
     * @return information about the debug server, or null if it is not a server
     */
    private ServerInfo testPort(InetAddress host, int port) {
        Socket s = null;
        try {
            if(canRuleOutQuickly(host, port)) {
                return null;
            }

            s = new Socket(host, port);
            s.setSoTimeout(100);
            sendProbe(s);
            ServerInfo info = readServerInfo(s);
            s.close();
            return info;
        } catch(IOException badPort) {
            return null;
        } finally {
            if(s != null) try {
                s.close();
            } catch(IOException ce) {
                return null;
            }
        }
    }

    /** Sends a SERVERINFO command to the potential server. */
    private static void sendProbe(Socket s) throws IOException {
        OutputStream out = s.getOutputStream();
        out.write(PROBE_BYTES);
        out.flush();
    }

    /**
     * Reads any reply from a potential server.
     * @return returns null if the server does not reply or does not send
     *   a proper debug server reply, or the server information if one is detected
     */
    private static ServerInfo readServerInfo(Socket s) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        String magic = in.readLine();
        if("SEDP3 OK".equals(magic)) {
            return new ServerInfo(s.getInetAddress(), s.getPort(), in.readLine(), in.readLine(), in.readLine(), in.readLine(), in.readLine());
        }
        throw new IOException(); // found something but not a debug server
    }

    private static final byte[] PROBE_BYTES = "SEDP3\nSERVERINFO\n".getBytes(StandardCharsets.UTF_8);

    public static void main(String args[]) throws Exception {
        DiscoveryService ds = new DiscoveryService();
        List<ServerInfo> results = ds.search();
        for(ServerInfo inf : results) {
            System.out.println(inf);
        }
    }
}
