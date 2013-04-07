package de.upsj.bukkit.advertising.servers;

/**
 * Listener for updates for server request status.
 * @author upsj
 * @version 1.0
 */
public interface ServerRequestListener {
    /**
     * Called when the status of a server is updated.
     * Called only from the main thread.
     * @param oldServer Old server.
     * @param newServer New server.
     */
    void updateStatus(PotentialServer oldServer, PotentialServer newServer);
}
