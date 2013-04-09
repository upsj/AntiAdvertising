package de.upsj.bukkit.advertising;

import de.upsj.bukkit.annotations.ConfigSection;
import de.upsj.bukkit.annotations.ConfigVar;
import de.upsj.bukkit.annotations.ConfigVarType;
import org.bukkit.configuration.ConfigurationSection;

/**
 * An action that can be performed after a player advertised another server.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "<action>",
               description = "Configuration values used by all action types.",
               values = {
                   @ConfigVar(name = Action.CONF_ATTEMPTS, type = ConfigVarType.INTEGER,
                              description = "The count of advertisement attempts a player can make "
                                          + "before this action is performed (0 means the action is disabled)"),
                   @ConfigVar(name = Action.CONF_ONCE, type = ConfigVarType.BOOLEAN,
                              description = "Set this to true to reset the attempt counter for this action "
                                          + "every time this action is performed, false to always perform the action "
                                          + "after its first execution.")
               }
)
public abstract class Action implements Configurable {
    /** Config value of the attempt count. */
    public static final String CONF_ATTEMPTS = "attempts";
    /** Config value true iff the action should only be taken once. */
    public static final String CONF_ONCE = "once";

    /** Allowed attempt count + 1. */
    private int attemptCount;
    /** True iff the action should only be taken all <attemptCount + 1> attempts. */
    private boolean once;

    /**
     * Executes an action caused by the given chat message.
     * @param message The chat message.
     */
    public abstract void doAction(ChatMessage message);

    /**
     * Returns whether the message may be shown.
     * @return True if the message may be shown.
     */
    public boolean mayShow() {
        return true;
    }

    /**
     * @param attempt The current attempt count.
     * @return True iff the action should be taken.
     */
    public boolean shouldUse(int attempt) {
        if (attemptCount == 0) {
            return false;
        }
        if (once) {
            return attempt % attemptCount == 0;
        } else {
            return attempt >= attemptCount;
        }
    }

    @Override
    public void reloadConfig(ConfigurationSection config) {
        attemptCount = config.getInt(CONF_ATTEMPTS, 0);
        if (attemptCount < 0) {
            attemptCount = 0;
        }
        once = config.getBoolean(CONF_ONCE, false);
        config.set(CONF_ATTEMPTS, attemptCount);
        config.set(CONF_ONCE, once);
    }
}
