package de.upsj.bukkit.advertising;

import de.upsj.bukkit.advertising.commands.PingCommand;
import de.upsj.bukkit.advertising.commands.ReloadCommand;
import de.upsj.bukkit.advertising.servers.PotentialServer;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;
import de.upsj.bukkit.annotations.Plugin;
import de.upsj.bukkit.annotations.ConfigSection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Iterator;
import java.util.List;

/**
 * AntiAdvertising plugin implementation.
 * @author upsj
 * @version 2.0
 */
@Plugin(name = "AntiAdvertising",
        description = "Kicking server advertisers",
        version = "2.1", author = "upsj")
@ConfigSection(name = "",
               description = "AntiAdvertising configuration",
               values = {
                   @ConfigVar(name = AntiAdvertisingPlugin.CONF_ENABLED, type = ConfigVarType.BOOLEAN,
                              description = "Set this to false to disable checking the chat for server advertisement. "
                                          + "Servers can still be pinged manually using the /serverping command."),
                   @ConfigVar(name = AntiAdvertisingPlugin.CONF_DEBUG, type = ConfigVarType.BOOLEAN,
                              description = "Set this to true to enable the debug mode. "
                                          + "This will produce a more detailed server log output."),
                   @ConfigVar(name = AntiAdvertisingPlugin.CONF_WHITELIST, type = ConfigVarType.STRING_LIST,
                             description = "The list of servers allowed to be mentioned in the chat."
                                         + "Format: server-address or server-address:port")
               }
)
public class AntiAdvertisingPlugin extends JavaPlugin {
    /** Config value for the enabled status. */
    public static final String CONF_ENABLED = "enabled";
    /** Config value for the debug mode. */
    public static final String CONF_DEBUG   = "debug";
    /** Config section for the actions. */
    public static final String CONF_ACTIONS = "actions";
    /** Config section for network. */
    public static final String CONF_NETWORK = "network";
    /** Config section for ChatListener. */
    public static final String CONF_CHAT    = "chat";
    /** Config section for whitelist. */
    public static final String CONF_WHITELIST = "whitelist";

    /** The server checker. */
    private ServerChecker serverChecker;
    /** The action handler. */
    private ActionHandler handler;
    /** The chat listener. */
    private ChatListener listener;

    @Override
    public void onEnable() {
        Log.init(getLogger(), isDebugMode());
        serverChecker = new ServerChecker();
        handler = new ActionHandler(getServer());
        listener = new ChatListener(serverChecker, handler);
        // Save possibly missing default values
        reload();

        // Register listeners, tasks
        getServer().getPluginManager().registerEvents(listener, this);
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, serverChecker, 1, 1);
        scheduler.scheduleSyncRepeatingTask(this, handler, 1, 1);

        // Register commands
        getCommand(ReloadCommand.NAME).setExecutor(new ReloadCommand(this));
        getCommand(PingCommand.NAME).setExecutor(new PingCommand(serverChecker, getServer()));
    }

    /** Reloads config values, adds possibly missing default values. */
    public void reload() {
        Log.log("(Re-)loading config...");
        reloadConfig();
        Log.setDebugMode(isDebugMode());
        listener.setEnabled(isCheckEnabled());
        serverChecker.reloadConfig(getSection(CONF_NETWORK));
        handler.reloadConfig(getSection(CONF_ACTIONS));
        listener.reloadConfig(getSection(CONF_CHAT));
        serverChecker.clear();
        loadWhiteList();
        saveConfig();
        Log.log("(Re-)loading config finished");
    }

    /**
     * Returns the configuration section at the given path.
     * If it doesn't exists, it is created.
     * @param path The path.
     * @return The configuration section at the given path.
     */
    private ConfigurationSection getSection(String path) {
        FileConfiguration conf = getConfig();
        if (conf.isConfigurationSection(path)) {
            return conf.getConfigurationSection(path);
        }
        return conf.createSection(path);
    }

    /** @return true iff the plugin is checking the chat. */
    private boolean isCheckEnabled() {
        FileConfiguration conf = getConfig();
        boolean enabled = conf.getBoolean(CONF_ENABLED, true);
        conf.set(CONF_ENABLED, enabled);
        return enabled;
    }

    /** @return true iff the plugin is in debug mode. */
    private boolean isDebugMode() {
        return getConfig().getBoolean(CONF_DEBUG, false);
    }

    /** Loads the white list. */
    private void loadWhiteList() {
        // FIXME possible issue: server advertised short before the whitelist is reloaded.
        List<String> whitelist = getConfig().getStringList(CONF_WHITELIST);
        Iterator<String> it = whitelist.iterator();
        String serverName;
        PotentialServer server;
        while (it.hasNext()) {
            serverName = it.next();
            server = ChatMessage.parseSingleServer(serverName, true);
            if (server == null) {
                it.remove();
                Log.warn("Couldn't parse whitelisted server " + serverName);
            } else {
                serverChecker.add(server, null);
            }
        }
        getConfig().set(CONF_WHITELIST, whitelist);
    }

    @Override
    public void onDisable() {
        serverChecker.shutdown();
        serverChecker = null;
        getServer().getScheduler().cancelTasks(this);
    }
}
