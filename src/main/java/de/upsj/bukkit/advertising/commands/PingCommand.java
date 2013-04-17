package de.upsj.bukkit.advertising.commands;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.advertising.Log;
import de.upsj.bukkit.advertising.Permissions;
import de.upsj.bukkit.advertising.ServerChecker;
import de.upsj.bukkit.advertising.servers.PotentialServer;
import de.upsj.bukkit.advertising.servers.ServerRequestListener;
import de.upsj.bukkit.annotations.CommandDef;

/**
 * Pings a server manually.
 * @author upsj
 * @version 1.0
 */
@CommandDef(name = PingCommand.NAME, description = "Pings a server manually.", permission = Permissions.PING)
public class PingCommand implements CommandExecutor, ServerRequestListener {
    /** The command name. */
    public static final String NAME = "serverping";
    private final ServerChecker checker;
    private final Server server;
    private final List<ServerPlayerPair> requests;

    /**
     * Initializes the ping command.
     * @param serverChecker The server checker.
     * @param server The server.
     */
    public PingCommand(ServerChecker serverChecker, Server server) {
        this.checker = serverChecker;
        this.requests = new LinkedList<ServerPlayerPair>();
        this.server = server;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!command.getName().equalsIgnoreCase(NAME)) {
            return false;
        }
        if (!commandSender.hasPermission(Permissions.PING)) {
            return false;
        }
        if (strings.length != 1) {
            commandSender.sendMessage(ChatColor.GOLD + "Usage: /" + NAME + " server");
            commandSender.sendMessage(ChatColor.GOLD + " or /" + NAME + " server:port");
            return true;
        }

        PotentialServer match = ChatMessage.parseSingleServer(strings[0], false);
        if (match == null) {
            commandSender.sendMessage(ChatColor.GOLD + "Couldn't parse '" + strings[0] + "'");
        } else {
            commandSender.sendMessage(ChatColor.GOLD + "Checking '" + strings[0] + "'");
            requests.add(new ServerPlayerPair(commandSender, match));
            PotentialServer newServer = checker.add(match, this);
            if (newServer.isFinal()) {
                updateStatus(match, newServer);
            }
            Log.debug("Serverping command: Enqueued " + match);
        }
        return true;
    }

    @Override
    public void updateStatus(PotentialServer oldServer, PotentialServer newServer) {
        Log.debug("Serverping command: Replacing " + oldServer + " by " + newServer);
        Iterator<ServerPlayerPair> it = requests.iterator();
        ServerPlayerPair pair;
        CommandSender sender;
        while (it.hasNext()) {
            pair = it.next();
            if (pair.server == oldServer) {
                Log.debug("Serverping command: Found request for oldServer by " + pair.sender);
                pair.server = newServer;
                sender = pair.getSender(server);
                if (sender == null) {
                    it.remove();
                    continue;
                }

                if (pair.server == null) {
                    it.remove();
                    sender.sendMessage(ChatColor.GOLD + "Couldn't resolve the server.");
                } else if (pair.server.isFinal()) {
                    it.remove();
                    sender.sendMessage(ChatColor.GOLD + "Finished request: " + pair.server.toDisplayString());
                }
            }
        }
    }

    /** A pair of a server and its requester. */
    private static class ServerPlayerPair {
        protected PotentialServer server;
        protected boolean isPlayer;
        protected String sender;

        public ServerPlayerPair(CommandSender commandSender, PotentialServer match) {
            this.server = match;
            this.sender = commandSender.getName();
            this.isPlayer = commandSender instanceof Player;
        }

        public CommandSender getSender(Server server) {
            if (isPlayer) {
                return server.getPlayerExact(sender);
            }
            return server.getConsoleSender();
        }
    }
}
