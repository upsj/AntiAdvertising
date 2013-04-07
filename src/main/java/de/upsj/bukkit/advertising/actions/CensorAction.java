package de.upsj.bukkit.advertising.actions;

import de.upsj.bukkit.advertising.Action;
import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.annotations.ConfigSection;

/**
 * Censors the chat message (replaces all advertisement by '*' characters).
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "censor", // keep up-do-date with the Actions enum!
        description = "Censors server advertisement (replaces it by '*' characters)."
)
public class CensorAction extends Action {
    /** Initializes the censor action. */
    CensorAction() { }

    @Override
    public void doAction(ChatMessage message) {
        message.censorAll();
    }
}
