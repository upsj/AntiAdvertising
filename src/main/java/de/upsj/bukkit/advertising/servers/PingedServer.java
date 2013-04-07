package de.upsj.bukkit.advertising.servers;

/**
 * Stores a server and its ping response.
 *
 * @author upsj
 * @version 1.0
 */
public class PingedServer extends ResolvedServer {
    /** Server message of the day. */
    protected final String motd;
    /** Full response string of the server ping. */
    protected final String fullResponse; // saved for debugging purposes
    /** Current player count. */
    protected final int players;
    /** Maximal player count. */
    protected final int maxPlayers;

    /**
     * Initializes a server by its ping response.
     * @param srv The server.
     * @param response The ping response.
     */
    public PingedServer(ResolvedServer srv, String response) {
        super(srv);

        this.fullResponse = response;
        // new protocol
        if (response.startsWith("§1")) {
            String[] parts = response.split("\0");

            this.motd = parts.length > 3 ? parts[3] : "INVALID SERVER RESPONSE";
            this.players = getArrayValue(parts, 4);
            this.maxPlayers = getArrayValue(parts, 5);
        // old protocol (pre-1.4)
        } else {
            String[] parts = response.split("§");

            this.motd = parts[0];
            this.players = getArrayValue(parts, 1);
            this.maxPlayers = getArrayValue(parts, 2);
        }
    }

    /**
     * @param parts array
     * @param i index
     * @return The integer value in the array at i or -1 if not found.
     */
    private int getArrayValue(String[] parts, int i) {
        if (i >= 0 && i < parts.length) {
            try {
                return Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * @return The message of the day.
     */
    public String getMOTD() {
        return motd;
    }

    /**
     * @return The online player count.
     */
    public int getPlayers() {
        return players;
    }

    /**
     * @return The maximal player count.
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    @Override
    public boolean isServer() {
        return !whitelisted;
    }

    @Override
    public String toString() {
        return "PingedServer (" + address + ":" + port
                + " - " + ipAddress.getHostAddress() + " - " + motd
                + " " + players + "/" + maxPlayers + ")";
    }

    @Override
    public String toDisplayString() {
        return motd + " (" + players + "/" + maxPlayers + ")";
    }

    /**
     * {@inheritDoc}
     * @return this, as this is the final state of the server.
     */
    public PingedServer call() {
        return this;
    }
}
