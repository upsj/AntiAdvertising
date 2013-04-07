package de.upsj.bukkit.advertising.commands;

import de.upsj.bukkit.advertising.AntiAdvertisingPlugin;
import de.upsj.bukkit.advertising.Permissions;
import de.upsj.bukkit.annotations.CommandDef;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;

/**
 * Reloads the plugin configuration.
 * @author upsj
 * @version 1.0
 */
@CommandDef(name = ReloadCommand.NAME, description = "Reloads the plugin configuration.", permission = Permissions.RELOAD)
public class ReloadCommand implements CommandExecutor {
    /** The command name. */
    public static final String NAME = "antiadreload";
    private final AntiAdvertisingPlugin plugin;

    /**
     * Initializes the reload command.
     * @param plugin The plugin.
     */
    public ReloadCommand(AntiAdvertisingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] strings) {
        if (!commandSender.hasPermission(Permissions.RELOAD)) {
            return false;
        }
        if (!command.getName().equalsIgnoreCase(NAME)) {
            return false;
        }
        plugin.reload();
        commandSender.sendMessage(ChatColor.GOLD + "[AntiAd] Reload finished");
        return true;
    }
}
