package de.upsj.bukkit.advertising;

import de.upsj.bukkit.annotations.ConfigSection;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Chat listener storing messages to be passed through.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = AntiAdvertisingPlugin.CONF_CHAT,
               description = "Configuration of how the chat is checked for advertisement.",
               parent = AntiAdvertisingPlugin.class,
               values = {
                   @ConfigVar(name = ChatListener.CONF_COMMANDS,
                              description = "List of commands whose parameters should be checked "
                                          + "in addition to the normal chat. (like /msg)",
                              type = ConfigVarType.STRING_LIST)
               }
)
public class ChatListener implements Listener, Configurable {
    public static final String CONF_COMMANDS = "commands";

    /** The server checker. */
    private final ServerChecker checker;
    /** The action handler. */
    private final ActionHandler handler;
    /** List of messages to be passed through. */
    private final List<ChatMessage> approved;
    /** List of commands to be checked as well. */
    private List<String> commands;
    /** Should the chat be checked? */
    private boolean enabled;

    /**
     * Initializes the chat listener.
     * @param checker The server checker.
     * @param handler The action handler.
     */
    public ChatListener(ServerChecker checker, ActionHandler handler) {
        this.checker = checker;
        this.handler = handler;
        this.handler.setListener(this);
        this.approved = new LinkedList<ChatMessage>();
        this.commands = new ArrayList<String>();
        this.enabled = true;
    }

    /**
     * Sets the enabled status of the chat listener.
     * @param enabled The enabled status.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Approves the message, leading to it not being canceled when resent.
     * @param msg The message.
     */
    public synchronized void approve(ChatMessage msg) {
        approved.add(msg);
    }

    /**
     * Called when a player chats.
     * @param event The chat event.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    /* Normal priority to prevent anti spam plugins from assuming that
     * the resent message and the cancelled message are both sent.
     * Regex has better performance than I thought ;-) */
    public void onChat(AsyncPlayerChatEvent event) {
        checkMessage(event, event.getPlayer().getName(), event.getMessage());
    }

    /**
     * Called when a player issues a command.
     * @param event The command event.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    /* Normal priority to prevent anti spam plugins from assuming that
     * the resent message and the cancelled message are both sent.
     * Regex has better performance than I thought ;-) */
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        for (String command : commands) {
            String lowCommand = command.toLowerCase();
            if (message.startsWith(lowCommand + " ") || message.equals(lowCommand)) {
                checkMessage(event, event.getPlayer().getName(), event.getMessage());
                return;
            }
        }
    }

    /**
     * Checks the given message, cancels and registers it for further investigation.
     * (if approved before, it will be passed through)
     * @param event The cancelable event.
     * @param player The sending player.
     * @param message The message.
     */
    private synchronized void checkMessage(Cancellable event, String player, String message) {
        if (!enabled) {
            return;
        }

        ChatMessage msg = ChatMessage.parse(player, message);

        // Check for approved messages
        Iterator<ChatMessage> it = approved.iterator();
        while (it.hasNext()) {
            if (it.next().equals(msg)) {
                it.remove();
                return;
            }
        }

        // If not clean: Cancel first, check further
        if (msg.getState() != ChatMessage.State.CLEAN) {
            checker.registerMessage(msg);
            event.setCancelled(true);
            handler.putPending(msg);
        }
    }

    @Override
    public void reloadConfig(ConfigurationSection section) {
        commands = section.getStringList(CONF_COMMANDS);
        section.set(CONF_COMMANDS, commands);
    }
}
