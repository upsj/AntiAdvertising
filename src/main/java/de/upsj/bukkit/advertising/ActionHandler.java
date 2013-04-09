package de.upsj.bukkit.advertising;

import de.upsj.bukkit.advertising.actions.Actions;
import de.upsj.bukkit.advertising.util.CountMap;
import de.upsj.bukkit.annotations.ConfigSection;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Manages currently pending messages and checks if they are clean periodically.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = AntiAdvertisingPlugin.CONF_ACTIONS,
               description = "Configuration of the actions performed when a player advertises a server.",
               parent = AntiAdvertisingPlugin.class
)
public class ActionHandler implements Runnable, Configurable {
    /** Attempt counter. */
    private final CountMap<String> attempts;
    /** Messages not completely processed. */
    private final Set<ChatMessage> pending;
    /** The actions. */
    private final List<Action> actions;
    /** The server. */
    private final Server server;
    /** Listener to approve messages. */
    private ChatListener listener;

    /**
     * Initializes the action handler.
     * @param server The server.
     */
    public ActionHandler(Server server) {
        this.pending = new HashSet<ChatMessage>();
        this.attempts = new CountMap<String>();
        this.actions = new ArrayList<Action>(Actions.values().length);
        this.server = server;
    }

    /** Checks pending messages for completion, passing them through. */
    public synchronized void run() {
        Iterator<ChatMessage> it = pending.iterator();
        ChatMessage msg;
        ChatMessage.State state;
        while (it.hasNext()) {
            msg = it.next();
            state = msg.getState();
            if (state.hasFinished()) {
                it.remove();
                if (!state.hasAdvertisement()) {
                    approveMessage(msg);
                } else {
                    doActions(msg);
                }
            }
        }
    }

    /**
     * Approves a message - it won't be checked again.
     * @param msg The message.
     */
    private void approveMessage(ChatMessage msg) {
        Log.debug("Approving message '" + msg.getMessage() + "' by " + msg.getSender());
        Player player = server.getPlayerExact(msg.getSender());
        if (player != null) {
            listener.approve(msg);
            player.chat(msg.getMessage());
        }
    }

    /**
     * Perform actions because of the given message.
     * @param msg The message.
     */
    private void doActions(ChatMessage msg) {
        boolean mayShow = true;
        if (mayIgnore(msg)) {
            Log.debug("Ignoring '" + msg.getMessage() + "' by " + msg.getSender());
        } else {
            Log.debug("Taking actions because of '" + msg.getMessage() + "' by " + msg.getSender());
            String name = msg.getSender().toLowerCase();
            attempts.increment(name);
            for (Action action : actions) {
                if (action.shouldUse(attempts.get(name))) {
                    Log.debug("Taking action " + action.getClass().getSimpleName() + " - mayShow() = " + action.mayShow());
                    mayShow &= action.mayShow();
                    action.doAction(msg);
                }
            }
        }
        if (mayShow) {
            approveMessage(msg);
        }
    }

    /**
     * Checks if a given message may be ignored.
     * (if the sender has the IGNORE permission)
     * @param msg The message.
     * @return true if the message may be ignored.
     */
    private boolean mayIgnore(ChatMessage msg) {
        Player pl = server.getPlayerExact(msg.getSender());
        if (pl == null) {
            return false;
        }
        return pl.hasPermission(Permissions.IGNORE);
    }

    /**
     * Puts a pending message.
     * @param msg The message.
     */
    public synchronized void putPending(ChatMessage msg) {
        pending.add(msg);
    }

    @Override
    public void reloadConfig(ConfigurationSection config) {
        actions.clear();
        Action action;
        for (Actions a : Actions.values()) {
            action = a.get(server);
            action.reloadConfig(getSection(config, a.name()));
            actions.add(action);
        }
    }

    /**
     * Gets the subsection with the given name in the parent section.
     * @param parent The parent section.
     * @param name The subsection name.
     * @return The subsection.
     */
    private ConfigurationSection getSection(ConfigurationSection parent, String name) {
        if (parent.isConfigurationSection(name)) {
            return parent.getConfigurationSection(name);
        }
        return parent.createSection(name);
    }

    /**
     * Links the handler to the listener.
     * @param listener The linked chat listener.
     */
    public void setListener(ChatListener listener) {
        this.listener = listener;
    }
}
