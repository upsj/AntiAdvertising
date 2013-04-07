package de.upsj.bukkit.advertising.servers;

import de.upsj.bukkit.advertising.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * A server match with resolved IP address.
 * @author upsj
 * @version 1.0
 */
public class ResolvedServer extends PotentialServer {
    /** Ping timeout. */
    private static int timeout = 1000;

    /** The server's IP address. */
    protected final InetAddress ipAddress;

    /**
     * Initializes a resolved server by a potential match and the IP address.
     * @param srv The potential match.
     * @param ip The IP address.
     */
    public ResolvedServer(PotentialServer srv, InetAddress ip) {
        super(srv);
        this.ipAddress = ip;
    }

    /**
     * Initializes a resolved server by the IP address, the port and the whitelist status.
     * @param ip The IP address.
     * @param port The port.
     * @param whitelisted True if the server is whitelisted.
     */
    public ResolvedServer(InetAddress ip, int port, boolean whitelisted) {
        super(ip.getHostAddress(), port, whitelisted);
        this.ipAddress = ip;
    }

    /**
     * Initializes a resolved server by the IP address and the port.
     * @param ip The IP address.
     * @param port The port.
     */
    public ResolvedServer(InetAddress ip, int port) {
        this(ip, port, false);
    }

    /**
     * Initializes a resolved server by the IP address and the whitelist status.
     * @param ip The IP address.
     * @param whitelisted True if the server is whitelisted.
     */
    public ResolvedServer(InetAddress ip, boolean whitelisted) {
        this(ip, DEFAULT_PORT, whitelisted);
    }

    /**
     * Initializes a resolved server by the IP address.
     * @param ip The IP address.
     */
    public ResolvedServer(InetAddress ip) {
        this(ip, DEFAULT_PORT);
    }

    /**
     * Copy constructor.
     * @param srv The server to copy.
     */
    protected ResolvedServer(ResolvedServer srv) {
        super(srv);
        this.ipAddress = srv.ipAddress;
    }

    /**
     * @return The IP address of the server.
     */
    public InetAddress getAddress() {
        return ipAddress;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public String toString() {
        return "ResolvedServer (" + address + ":" + port
                + " - " + ipAddress.getHostAddress() + ")";
    }

    public String toDisplayString() {
        return super.toDisplayString() + " (" + ipAddress.getHostAddress() + ")";
    }

    @Override
    public int hashCode() {
        return ipAddress.hashCode() + 31 * port;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ResolvedServer)) {
            return false;
        }
        ResolvedServer otherServer = (ResolvedServer) other;
        return this.port == otherServer.port
            && this.ipAddress.equals(otherServer.ipAddress);
    }

    /**
     * {@inheritDoc}
     * @return A PingedServer (if successful) or ResolvedNoServer (if unsuccessful ping).
     */
    public PotentialServer call() {
        PotentialServer result;
        // loopback adapter
        if (ipAddress.isLoopbackAddress()) {
            Log.debug("Ignored " + this + ": Loopback");
            result = new ResolvedNoServer(this);
        } else {
            try {
                result = new PingedServer(this, ping());
                Log.debug("Pinged " + this + ": " + result);
            } catch (IOException e) {
                Log.debug("Pinging " + this + " failed: " + e.getMessage());
                result = new ResolvedNoServer(this);
            }
        }
        return result;
    }

    /**
     * Pings the server.
     * @return Ping response.
     * @throws IOException If an exception occurs while pinging the server.
     */
    private String ping() throws IOException {
        DataInputStream in;
        OutputStream out;
        InputStreamReader strReader;
        char[] chars;
        Socket socket = new Socket();

        try {
            socket.setSoTimeout(timeout);
            socket.connect(new InetSocketAddress(address, port), timeout);

            out = socket.getOutputStream();
            in = new DataInputStream(socket.getInputStream());

            // Ping
            out.write(0xFE);
            // Magic byte
            out.write(0x01);

            // Kick packet: 1 byte 0xFF followed by...
            int packet = in.readUnsignedByte();
            if (packet != 0xFF) {
                throw new IOException("Wrong packet ID (" + packet + ").");
            }

            // ... string: 2 bytes (signed short) for string length...
            int strLen = in.readShort();

            // ... and <strLen> 16bit UCS-2 characters.
            strReader = new InputStreamReader(in, Charset.forName("UTF-16BE"));
            chars = new char[strLen];
            if (strReader.read(chars) != strLen) {
                throw new IOException("End of stream");
            }

            return new String(chars);
        } catch (IOException e) {
            throw e;
        } finally {
            socket.close();
        }
    }

    /**
     * Sets the ping timeout.
     * @param timeout The ping timeout.
     */
    public static void setTimeout(int timeout) {
        ResolvedServer.timeout = timeout;
    }
}
