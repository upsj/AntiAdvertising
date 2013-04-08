package de.upsj.bukkit.advertising.actions;

import de.upsj.bukkit.advertising.Action;
import de.upsj.bukkit.advertising.ActionHandler;
import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.annotations.ConfigSection;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Executes a custom command.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "command", // keep up-do-date with the Actions enum!
        description = "Executes a custom command. (Input like server commands: 'kick name')",
        values = {
                @ConfigVar(name = CommandAction.CONF_COMMAND, type = ConfigVarType.STRING,
                          description = "The command to be executed. "
                                      + "Use %NAME% to insert the player name.")
        },
        parent = ActionHandler.class
)
public class CommandAction extends Action {
    /** Config value for the command to be executed. */
    public static final String CONF_COMMAND = "command";
    private static final String NAME = "%NAME%";
    private final Server server;
    private String command;

    /**
     * Initializes the command action.
     * @param server The server.
     */
    CommandAction(Server server) {
        this.server = server;
    }

    @Override
    public void doAction(ChatMessage message) {
        server.dispatchCommand(server.getConsoleSender(), command.replace(NAME, message.getSender()));
    }

    @Override
    public void reloadConfig(ConfigurationSection config) {
        super.reloadConfig(config);
        this.command = config.getString(CONF_COMMAND, "ban " + NAME);
        config.set(CONF_COMMAND, command);
    }
}
