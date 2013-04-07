package de.upsj.bukkit.advertising.servers;

/**
 * Indicator class for resolved "non-servers".
 *
 * @author upsj
 * @version 1.0
 */
public class ResolvedNoServer extends ResolvedServer {

    /**
     * Initializes a non-server.
     * @param srv The underlying resolved server.
     */
    public ResolvedNoServer(ResolvedServer srv) {
        super(srv);
    }

    @Override
    public boolean isFinal() {
        return true;
    }

    /**
     * {@inheritDoc}
     * @return this, as this is the final state of the server.
     */
    public PotentialServer call() {
        return this;
    }

    @Override
    public String toString() {
        return "ResolvedNoServer (" + address + ":" + port
                + " - " + ipAddress.getHostAddress() + ")";
    }

    @Override
    public String toDisplayString() {
        return super.toDisplayString() + " - no minecraft server";
    }
}
