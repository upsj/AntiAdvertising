package de.upsj.bukkit.advertising.actions;

import de.upsj.bukkit.advertising.Action;
import de.upsj.bukkit.advertising.ActionHandler;
import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.annotations.ConfigSection;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Broadcasts a message.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "broadcast", // keep up-do-date with the Actions enum!
               description = "Broadcasts a message to the server.",
               values = {
                   @ConfigVar(name = BroadcastAction.CONF_MESSAGE, type = ConfigVarType.STRING,
                              description = "The message to be broadcasted. "
                                          + "Use %NAME% to insert the player name, &0 - &f for colors.")
               },
               parent = ActionHandler.class
)
public class BroadcastAction extends Action {
    /** Config value for the message to be broadcasted. */
    public static final String CONF_MESSAGE = "message";
    private static final String NAME = "%NAME%";
    private final Server server;
    private String broadcastMessage;

    /**
     * Initializes the broadcast action.
     * @param server The server.
     */
    BroadcastAction(Server server) {
        this.server = server;
    }

    @Override
    public void doAction(ChatMessage message) {
        server.broadcastMessage(broadcastMessage.replace(NAME, message.getSender()));
    }

    @Override
    public void reloadConfig(ConfigurationSection config) {
        super.reloadConfig(config);
        String message = config.getString(CONF_MESSAGE, NAME + " was kicked because of server advertisement");
        config.set(CONF_MESSAGE, message);
        this.broadcastMessage = ChatColor.translateAlternateColorCodes('&', message);
    }
}
