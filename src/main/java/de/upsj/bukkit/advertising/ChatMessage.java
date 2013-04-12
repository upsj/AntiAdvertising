package de.upsj.bukkit.advertising;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.upsj.bukkit.advertising.servers.PotentialServer;
import de.upsj.bukkit.advertising.servers.ResolvedServer;
import de.upsj.bukkit.advertising.servers.ServerRequestListener;

/**
 * A chat message possibly containing server advertisement.
 *
 * @author upsj
 * @version 1.0
 */
public final class ChatMessage implements Cloneable, ServerRequestListener {
    /** The processing state of the chat message. */
    public enum State {
        /** Clean message. */
        CLEAN(true, false),
        /** State unknown. */
        UNKNOWN(false, false),
        /** Advertisement for sure, but not completely processed. */
        ADVERTISEMENT_PENDING(false, true),
        /** Advertisement for sure, no pending requests. */
        ADVERTISEMENT(true, true);

        /** True iff the chat matches have been processed completely. */
        private final boolean finished;
        /** True iff the message contains at least one advertisement. */
        private final boolean ad;

        /**
         * Initializes the state by its properties.
         * @param finished Is the processing finished?
         * @param ad Does the message contain advertisement?
         */
        private State(boolean finished, boolean ad) {
            this.finished = finished;
            this.ad = ad;
        }

        /** @return True if and only if all potential matches have been processed completely. */
        public boolean hasFinished() { return finished; }
        /** @return True if and only if the message contains at least one advertisement. */
        public boolean hasAdvertisement() { return ad; }
    }

    /** Regex pattern to match domain name + port. */
    private static final String DOMAIN_PATTERN = "([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,}(:[0-9]{1,5})?";
    /** Regex pattern to match IP + port. */
    private static final String IP_PATTERN = "[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}(:[0-9]{1,5})?";
    /** Compiled domain regex. */
    public static final Pattern DOMAIN = Pattern.compile(DOMAIN_PATTERN);
    /** Compiled IP regex. */
    public static final Pattern IP = Pattern.compile(IP_PATTERN);

    /** The message. */
    private String message;
    /** The sender name. */
    private final String senderName;
    /** The server matches. */
    private final PotentialServer[] serverMatches;
    /** The start indices of the matches. */
    private final int[] matchStart;
    /** The end indices of the matches. */
    private final int[] matchEnd;
    /** The current processing state of the message. */
    private State state;

    /**
     * Initializes a suspicious chat message.
     * @param msg The message.
     * @param sender The sender.
     * @param matches The possible matches.
     * @param matchStart The start indices of the matches.
     * @param matchEnd The end indices of the matches.
     */
    private ChatMessage(String msg, String sender, PotentialServer[] matches, int[] matchStart, int[] matchEnd) {
        this.message = msg;
        this.senderName = sender;
        this.serverMatches = matches;
        this.matchStart = matchStart;
        this.matchEnd = matchEnd;
        this.state = matches.length == 0 ? State.CLEAN : State.UNKNOWN;
    }

    /**
     * @return The processing state.
     */
    public State getState() {
        return state;
    }

    /**
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The player name of the message sender.
     */
    public String getSender() {
        return senderName;
    }

    /**
     * @return The count of possible matches.
     */
    public int getMatchCount() {
        return serverMatches.length;
    }

    /**
     * @param i The index.
     * @return The server match at the given index.
     */
    public PotentialServer getMatch(int i) {
        return serverMatches[i];
    }

    /**
     * Replaces the match given by its index by '*' characters.
     * @param i The index.
     */
    public void censorMatch(int i) {
        int start = matchStart[i];
        int end = matchEnd[i];
        StringBuilder resultBuilder = new StringBuilder(message.length());
        resultBuilder.append(message, 0, start);
        for (int n = 0; n < end - start; n++) {
            resultBuilder.append('*');
        }
        resultBuilder.append(message, end, message.length());
        message = resultBuilder.toString();
    }

    /** Replaces all known advertisement in the message. */
    public void censorAll() {
        for (int i = 0; i < serverMatches.length; i++) {
            if (serverMatches[i].isFinal() && serverMatches[i].isServer()) {
                censorMatch(i);
            }
        }
    }

    /**
     * {@inheritDoc}
     * In this case, it replaces a server match in the chat message by a further processed match.
     * This method can safely be called from the chat thread, too.
     * @param old The old match.
     * @param srv The new match.
     */
    public synchronized void updateStatus(PotentialServer old, PotentialServer srv) {
        // DEBUG assert srv == null || (srv == null && old == null) || old.equals(srv)  : "Invalid replace";
        int adCount = 0;
        int unknownCount = 0;
        for (int i = 0; i < serverMatches.length; i++) {
            if (serverMatches[i] == old) {
                serverMatches[i] = srv;
            }
            if (serverMatches[i] != null) {
                if (serverMatches[i].isServer()) {
                    adCount++;
                } else if (!serverMatches[i].isFinal()) {
                    unknownCount++;
                }
            }
        }
        state = (adCount > 0 ? (unknownCount > 0 ? State.ADVERTISEMENT_PENDING : State.ADVERTISEMENT)
                             : (unknownCount > 0 ? State.UNKNOWN : State.CLEAN));
    }

    /**
     * Parses a chat message for possible advertisement.
     * @param playerName The message sender.
     * @param message The message.
     * @return The ChatMessage instance.
     */
    public static ChatMessage parse(String playerName, String message) {
        List<PotentialServer> matches = new ArrayList<PotentialServer>();
        List<Integer> matchStarts = new ArrayList<Integer>();
        List<Integer> matchEnds = new ArrayList<Integer>();
        PotentialServer server;

        // Domain matches
        Matcher domainMatcher = DOMAIN.matcher(message);
        while (domainMatcher.find()) {
            server = parseDomain(domainMatcher.group(), false);
            if (server != null) {
                matchStarts.add(domainMatcher.start());
                matchEnds.add(domainMatcher.end());
                matches.add(server);
            }
        }

        // IP matches
        Matcher ipMatcher = IP.matcher(message);
        while (ipMatcher.find()) {
            server = parseIP(ipMatcher.group(), false);
            if (server != null) {
                matchStarts.add(ipMatcher.start());
                matchEnds.add(ipMatcher.end());
                matches.add(server);
            }
        }

        // copy lists to arrays
        PotentialServer[] matchArray = matches.toArray(new PotentialServer[matches.size()]);
        int[] startArray = new int[matches.size()];
        int[] endArray = new int[matches.size()];

        for (int i = 0; i < matchArray.length; i++) {
            startArray[i] = matchStarts.get(i);
            endArray[i] = matchEnds.get(i);
        }

        Log.debug("Parsed chat message: '" + message + "' by " + playerName);
        return new ChatMessage(message, playerName, matchArray, startArray, endArray);
    }

    /**
     * @param domainString The domain match (with optional port part).
     * @param whitelisted If the server should be whitelisted.
     * @return The parsed server or null if the parsing failed.
     */
    public static PotentialServer parseDomain(String domainString, boolean whitelisted) {
        String[] parts = domainString.split(":");

        // No port number
        if (parts.length == 1) {
            return new PotentialServer(parts[0], whitelisted);

        // Port number
        } else {
            try {
                int port = Integer.parseInt(parts[1]);
                if (port <= 65535) {
                    return new PotentialServer(parts[0], port, whitelisted);
                }
            } catch (NumberFormatException e) { /* nothing to do here */ }
        }

        return null;
    }

    /**
     * @param ipString The IP match (with optional port part).
     * @param whitelisted If the server should be whitelisted.
     * @return The parsed server or null if the parsing failed.
     */
    public static PotentialServer parseIP(String ipString, boolean whitelisted) {
        String[] parts = ipString.split(":");
        String[] parts2 = parts[0].split("\\.");

        try {
            // Parse IP
            short ip1 = Short.parseShort(parts2[0]);
            short ip2 = Short.parseShort(parts2[1]);
            short ip3 = Short.parseShort(parts2[2]);
            short ip4 = Short.parseShort(parts2[3]);
            if (ip1 > 255 || ip2 > 255 || ip3 > 255 || ip4 > 255) {
                return null;
            }
            InetAddress address = InetAddress.getByAddress(
                    new byte[] {(byte) ip1, (byte) ip2, (byte) ip3, (byte) ip4});

            // No port number
            if (parts.length == 1) {
                return new ResolvedServer(address, whitelisted);

            // Port number
            } else {
                int port = Integer.parseInt(parts[1]);
                if (port <= 65535) {
                    return new ResolvedServer(address, port, whitelisted);
                }
            }
        } catch (NumberFormatException e) { // nothing to do here
        } catch (UnknownHostException e) { /* shouldn't happen */ }

        return null;
    }

    /**
     * @param name The server match (with optional port part).
     * @param whitelisted If the server should be whitelisted.
     * @return The parsed server or null if the parsing failed.
     */
    public static PotentialServer parseSingleServer(String name, boolean whitelisted) {
        Matcher m = DOMAIN.matcher(name);
        PotentialServer match = null;
        if (m.find()) {
            match = parseDomain(m.group(), whitelisted);
        }
        if (match == null) {
            m = DOMAIN.matcher(name);
            if (m.find()) {
                match = ChatMessage.parseIP(m.group(), whitelisted);
            }
        }
        return match;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChatMessage)) {
            return false;
        }

        ChatMessage that = (ChatMessage) o;

        return message.equals(that.message)
            && senderName.equals(that.senderName);
    }

    @Override
    public int hashCode() {
        int result = message.hashCode();
        result = 31 * result + senderName.hashCode();
        return result;
    }

    @Override
    public ChatMessage clone() {
        ChatMessage result = new ChatMessage(message, senderName, serverMatches, matchStart, matchEnd);
        // update state
        result.updateStatus(null, null);
        return result;
    }
}
