package de.upsj.bukkit.advertising.actions;

import de.upsj.bukkit.advertising.Action;
import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.annotations.ConfigSection;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Kicks the message sender.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "kick", // keep up-do-date with the Actions enum!
        description = "Kicks the player from the server.",
        values = {
                @ConfigVar(name = KickAction.CONF_MESSAGE, type = ConfigVarType.STRING,
                          description = "The kick message. Use &0 - &f for colors.")
        }
)
public class KickAction extends Action {
    /** Config calue for the kick message. */
    public static final String CONF_MESSAGE = "message";
    private final Server server;
    private String kickMessage;

    /**
     * Initializes the kick action.
     * @param server The server.
     */
    KickAction(Server server) {
        this.server = server;
    }

    @Override
    public void doAction(ChatMessage message) {
        Player sender = server.getPlayerExact(message.getSender());
        if (sender != null) {
            sender.kickPlayer(kickMessage);
        }
    }

    @Override
    public void reloadConfig(ConfigurationSection config) {
        super.reloadConfig(config);
        String message = config.getString(CONF_MESSAGE, "You were kicked because of server advertising!");
        config.set(CONF_MESSAGE, message);
        this.kickMessage = ChatColor.translateAlternateColorCodes('&', message);
    }
}
