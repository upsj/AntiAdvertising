package de.upsj.bukkit.advertising.servers;

import de.upsj.bukkit.advertising.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

/**
 * A potential match (server address) in a {@link de.upsj.bukkit.advertising.ChatMessage}.
 *
 * @author upsj
 * @version 1.0
 */
public class PotentialServer implements Callable<PotentialServer> {
    /** Default port. */
    protected static final int DEFAULT_PORT = 25565;

    /** The matched server domain name. */
    protected final String address;
    /** The server port. */
    protected final int port;
    /** Is the server whitelisted? */
    protected final boolean whitelisted;

    /**
     * Initializes a whitelisted server with a port.
     * @param srvAddr The matched server address.
     * @param srvPort The matched server port.
     * @param whitelist True if and only if the server is whitelisted.
     */
    public PotentialServer(String srvAddr, int srvPort, boolean whitelist) {
        this.address = srvAddr;
        this.port = srvPort;
        this.whitelisted = whitelist;
    }

    /**
     * Initializes a whitelisted server.
     * @param srvAddr The matched server address.
     * @param whitelist True if and only if the server is whitelisted.
     */
    public PotentialServer(String srvAddr, boolean whitelist) {
        this(srvAddr, DEFAULT_PORT, whitelist);
    }

    /**
     * Initializes the server match with a port.
     * @param srvAddr The matched server address.
     * @param srvPort The matched server port.
     */
    public PotentialServer(String srvAddr, int srvPort) {
        this(srvAddr, srvPort, false);
    }

    /**
     * Initializes the server match.
     * @param srvAddr The matched server address.
     */
    public PotentialServer(String srvAddr) {
        this(srvAddr, DEFAULT_PORT, false);
    }

    /**
     * Copy constructor.
     * @param srv The server to copy.
     */
    protected PotentialServer(PotentialServer srv) {
        this(srv.address, srv.port, srv.whitelisted);
    }

    /** @return The matched server domain name. */
    public String getMatchedAddress() {
        return address;
    }

    /** @return The server port. */
    public int getPort() {
        return port;
    }

    /** @return True if and only if this is the server's final form. */
    public boolean isFinal() {
        return false;
    }

    /** @return True if and only if this is a server for sure. */
    public boolean isServer() {
        return false;
    }

    @Override
    public String toString() {
        return "PotentialServer (" + address + ":" + port + ")";
    }

    public String toDisplayString() {
        return address + ":" + port;
    }

    /**
     * Retrieves more information about the server.
     * @return DNS looked up version of this or null if not possible.
     */
    public PotentialServer call() {
        PotentialServer result;
        try {
            InetAddress ip = InetAddress.getByName(address);
            result = new ResolvedServer(this, ip);
            Log.debug("Resolved " + this + ": " + result);
        } catch (UnknownHostException e) {
            Log.debug("Couldn't resolve " + this);
            result = null;
        }
        return result;
    }

    /**
     * Checks for domain name and port equality to another server.
     * @param other The other server.
     * @return True if and only if the server's name (case irrelevant) and the port
     * equal the other server's name and port.
     */
    public boolean equalsByName(PotentialServer other) {
        return address.equalsIgnoreCase(other.address);
    }
}
