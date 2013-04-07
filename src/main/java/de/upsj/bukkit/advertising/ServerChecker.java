package de.upsj.bukkit.advertising;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.upsj.bukkit.advertising.servers.PotentialServer;
import de.upsj.bukkit.advertising.servers.ResolvedServer;
import de.upsj.bukkit.advertising.servers.ServerRequestListener;
import de.upsj.bukkit.annotations.ConfigSection;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Server checker managing known servers and processing.
 * It contains a thread pool checking potential servers.
 *
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = AntiAdvertisingPlugin.CONF_NETWORK,
               description = "Server request configuration.",
               values = {
                   @ConfigVar(name = ServerChecker.CONF_TIMEOUT, type = ConfigVarType.INTEGER,
                              description = "The maximal time to wait for a server to respond to a ping (in ms).")
               }
)
public class ServerChecker implements Runnable, Configurable {
    /** Configuration value for the connection timeout. */
    public static final String CONF_TIMEOUT = "timeout";
    /** Thread pool. */
    private ExecutorService pool;
    /** Servers that are currently being processed. */
    private Queue<ServerFuturePair> processing;
    /** Known servers mapped on themselves, simplifying access. */
    private Map<PotentialServer, PotentialServer> knownServers;

    /**
     * Initializes the server checker.
     */
    public ServerChecker() {
        pool = Executors.newCachedThreadPool();
        knownServers = new HashMap<PotentialServer, PotentialServer>();
        processing = new ConcurrentLinkedQueue<ServerFuturePair>();
    }

    /**
     * Adds all potential matches from the message to the queue,
     * replacing by further processed server instances.
     * @param msg The message
     */
    public synchronized void registerMessage(ChatMessage msg) {
        for (int i = 0; i < msg.getMatchCount(); i++) {
            PotentialServer inServer = msg.getMatch(i);
            PotentialServer outServer = add(inServer, msg);
            msg.updateStatus(inServer, outServer);
        }
    }

    /**
     * Adds a server to be checked, linked to a update listener.
     * @param server The server.
     * @param listener The update listener.
     * @return A server instance that's already being
     * or been processed equal to the given server
     * or {@code server} if no such instance exists.
     * @throws IllegalStateException If the checker has been shut down.
     */
    public synchronized PotentialServer add(PotentialServer server, ServerRequestListener listener) throws IllegalStateException {
        if (pool.isShutdown()) {
            throw new IllegalStateException("shutdown");
        }

        Log.debug("Enqueued server " + server);

        if (server.isFinal()) {
            return server;
        }

        // Already known by address?
        if (knownServers.containsKey(server)) {
            PotentialServer other = knownServers.get(server);
            Log.debug("Replaced by server " + other);
            return other;
        }

        // Already known by name?
        for (PotentialServer other : knownServers.keySet()) {
            // DEBUG assert other.isFinal();
            if (server.equalsByName(other)) {
                Log.debug("Replaced by server " + other);
                return other;
            }
        }

        // Currently processing?
        for (ServerFuturePair other : processing) {
            // DEBUG assert !other.getServer().isFinal();
            if (server.equals(other.server)
                    || server.equalsByName(other.server)) {
                if (listener != null) {
                    other.linkedMessages.add(listener);
                }
                Log.debug("Replaced by currently processing server " + other);
                return other.server;
            }
        }

        ServerFuturePair pair;
        Future<PotentialServer> future = pool.submit(server);
        if (listener != null) {
            pair = new ServerFuturePair(server, future, listener);
        } else {
            pair = new ServerFuturePair(server, future);
        }
        processing.add(pair);
        return server;
    }

    /**
     * Updates server list from finished threads.
     * The process runs as follows:
     * Finished servers are removed.
     * Their linked chat messages are updated.
     * If they are final, they will be stored.
     * If not, they are re-enqueued for further processing.
     * (Always ensuring that the chat messages are linked
     * either to processing or to final servers)
     */
    public synchronized void run() {
        Iterator<ServerFuturePair> it = processing.iterator();
        ServerFuturePair pair;
        PotentialServer server;
        while (it.hasNext()) {
            pair = it.next();
            if (!pair.future.isDone()) {
                continue;
            }

            it.remove();
            server = futureGet(pair);
            Log.debug("Finished server " + pair.server + ", result: " + (server == null ? "null" : server));
            notifyReplace(pair, pair.server, server);

            // Store final, process non-final further
            if (server != null) {
                /* Although to avoid duplicate pings it would be best to check final servers first,
                 * the improbable case of a server that's finished resolving and pinging in the same tick
                 * and the resolved server is first in the list isn't taken into considerations. */
                if (server.isFinal()) {
                    addKnownServer(pair, server);
                } else {
                    addProcessingServer(pair, server);
                }
            }
        }
    }

    /**
     * Notifies all linked listeners of an update.
     * @param pair The pair.
     * @param old The old server.
     * @param server The new (replacement) server.
     */
    private void notifyReplace(ServerFuturePair pair, PotentialServer old, PotentialServer server) {
        for (ServerRequestListener listener : pair.linkedMessages) {
            listener.updateStatus(old, server);
        }
    }

    /**
     * Returns the value of the pair's future.
     * @param pair The pair.
     * @return The future's value or null if not available.
     */
    private PotentialServer futureGet(ServerFuturePair pair) {
        try {
            return pair.future.get();
        } catch (InterruptedException e) {
            Log.warn("Processing for " + pair.server + " has been interrupted");
        } catch (ExecutionException e) {
            e.printStackTrace();
            Log.warn("Processing for " + pair.server + " has failed");
        }
        return null;
    }

    /**
     * Adds a known server to the server list.
     * Replaces equal known servers only if they are considered a server
     * and the new server isn't.
     * In this case, the listeners are notified.
     * @param pair The pair from which the server originates.
     * @param server The known server.
     */
    private void addKnownServer(ServerFuturePair pair, PotentialServer server) {
        PotentialServer other;
        other = knownServers.remove(server);
        if (other != null) {
            if (other.isServer() != server.isServer()) {
                /* Different responses from different requests:
                 * Server beats non-server */
                if (server.isServer()) {
                    knownServers.put(server, server);
                } else {
                    notifyReplace(pair, server, other);
                    knownServers.put(other, other);
                }
                Log.warn("Server response conflict between "
                        + server + " and " + other);
            } else {
                knownServers.put(server, server);
            }
        } else {
            knownServers.put(server, server);
        }
    }

    /**
     * Adds a server that should be processed further.
     * @param pair The server pair of the server.
     *             It will be reused to re-enqueue the server.
     * @param server The server.
     */
    private void addProcessingServer(ServerFuturePair pair, PotentialServer server) {
        PotentialServer other = knownServers.get(server);
        if (other != null) {
            notifyReplace(pair, server, other);
        } else {
            // reuse old pair
            pair.server = server;
            pair.future = pool.submit(server);
            processing.add(pair);
            /* no duplicate check as duplicates are quite improbable
             * and should cause no delay as soon as one of them is completely processed. */
        }
    }

    /**
     * Waits for completion of the running tasks.
     * @throws InterruptedException .
     */
    public synchronized void awaitCompletion() throws InterruptedException {
        for (ServerFuturePair pair : processing) {
            try {
                pair.future.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Shuts down the thread pool, clears remaining unfinished servers.
     * Should be called before finalization.
     */
    public synchronized void shutdown() {
        pool.shutdown();
        processing.clear();
        knownServers.clear();
    }

    @Override
    public void reloadConfig(ConfigurationSection config) {
        int timeout = config.getInt(CONF_TIMEOUT, 1000);
        if (timeout < 50) { // 50 ms ping is unrealistic
            timeout = 1000;
        } else if (timeout > 10000) { // max. 10 s ping
            timeout = 10000;
        }
        config.set(CONF_TIMEOUT, timeout);
        ResolvedServer.setTimeout(timeout);
    }

    /** Clears the server list. */
    public synchronized void clear() {
        knownServers.clear();
    }

    /** Pair of a PotentialServer, its Future<> and linked listeners. */
    private static class ServerFuturePair {
        /** The server that is being processed. */
        protected PotentialServer server;
        /** The future processed server. */
        protected Future<PotentialServer> future;
        /** The chat messages containing the server. */
        protected final List<ServerRequestListener> linkedMessages;

        /**
         * Initializes by server and future.
         * @param server The server.
         * @param future The future.
         */
        protected ServerFuturePair(PotentialServer server, Future<PotentialServer> future) {
            this.server = server;
            this.future = future;
            this.linkedMessages = new ArrayList<ServerRequestListener>(2);

        }

        /**
         * Initialize by server, future and a linked listener.
         * @param server The server.
         * @param future The future.
         * @param origin The listener.
         */
        protected ServerFuturePair(PotentialServer server, Future<PotentialServer> future,
                                   ServerRequestListener origin) {
            this(server, future);
            this.linkedMessages.add(origin);
        }
    }
}
