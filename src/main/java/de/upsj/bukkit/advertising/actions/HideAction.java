package de.upsj.bukkit.advertising.actions;

import de.upsj.bukkit.advertising.Action;
import de.upsj.bukkit.advertising.ActionHandler;
import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.annotations.ConfigSection;

/**
 * Hides the chat message.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "hide", // keep up-do-date with the Actions enum!
        description = "Hides server advertisement (drops the messages).",
        parent = ActionHandler.class
)
public class HideAction extends Action {
    /** Initializes the hide action. */
    HideAction() { }

    @Override
    public void doAction(ChatMessage message) { }

    @Override
    public boolean mayShow() {
        return false;
    }
}
