package de.upsj.bukkit.advertising.actions;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;

import de.upsj.bukkit.advertising.Action;
import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.advertising.Permissions;
import de.upsj.bukkit.advertising.servers.PingedServer;
import de.upsj.bukkit.advertising.servers.PotentialServer;
import de.upsj.bukkit.annotations.ConfigSection;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;

/**
 * Notifies the moderators of the advertisement.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "notifyMods", // keep up-do-date with the Actions enum!
        description = "Notifies moderators with notification permission of the advertisement.",
        values = {
                @ConfigVar(name = NotifyModsAction.CONF_MESSAGE, type = ConfigVarType.STRING,
                           description = "The notification message to be broadcasted. "
                                       + "Use %NAME% to insert the player name, &0 - &f for colors."),
                @ConfigVar(name = NotifyModsAction.CONF_FORMAT, type = ConfigVarType.STRING,
                           description = "The output format of the server information. "
                                       + "Use %MOTD% for the server's message of the day, "
                                       + "%PLAYERS% for the current and %MAX% for the maximal player count.")
         }
)
public class NotifyModsAction extends Action {
    /** Config value for the notification message. */
    public static final String CONF_MESSAGE = "message";
    /** Config value for the server format. */
    public static final String CONF_FORMAT = "format";
    private static final String NAME = "%NAME%";
    private static final String MOTD = "%MOTD%";
    private static final String PLAYERS = "%PLAYERS%";
    private static final String MAXPLAYERS = "%MAX%";
    private final Server server;
    private String notificationMessage;
    private String serverFormat;

    /**
     * Initializes the notification action.
     * @param server The server.
     */
    NotifyModsAction(Server server) {
        this.server = server;
    }

    @Override
    public void doAction(ChatMessage message) {
        server.broadcast(notificationMessage.replace(NAME, message.getSender()), Permissions.NOTIFY);
        for (int i = 0; i < message.getMatchCount(); i++) {
            PotentialServer match = message.getMatch(i);
            if (match.isServer()) {
                PingedServer pinged = (PingedServer) match;
                server.broadcast(serverFormat.replace(MOTD, pinged.getMOTD())
                                             .replace(PLAYERS, String.valueOf(pinged.getPlayers()))
                                             .replace(MAXPLAYERS, String.valueOf(pinged.getMaxPlayers())),
                        Permissions.NOTIFY);
            }
        }
    }

    @Override
    public void reloadConfig(ConfigurationSection config) {
        super.reloadConfig(config);
        String message = config.getString(CONF_MESSAGE, NAME + " adverted to a server:");
        String format = config.getString(CONF_FORMAT, MOTD + " (" + PLAYERS + "/" + MAXPLAYERS + ")");
        config.set(CONF_MESSAGE, message);
        config.set(CONF_FORMAT, format);
        this.notificationMessage = ChatColor.translateAlternateColorCodes('&', message);
        this.serverFormat = ChatColor.translateAlternateColorCodes('&', format);
    }
}
