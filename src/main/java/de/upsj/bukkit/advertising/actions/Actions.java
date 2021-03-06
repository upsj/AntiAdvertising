package de.upsj.bukkit.advertising.actions;

import de.upsj.bukkit.advertising.Action;
import org.bukkit.Server;

/**
 * Contains all possible action types.
 * @author upsj
 * @version 1.0
 */
public enum Actions {
    /** @see KickAction */
    kick
    { public Action get(Server srv) { return new KickAction(srv); } },

    /** @see BroadcastAction */
    broadcast
    { public Action get(Server srv) { return new BroadcastAction(srv); } },

    /** @see HideAction */
    hide
    { public Action get(Server srv) { return new HideAction(); } },

    /** @see NotifyModsAction */
    nofityMods
    { public Action get(Server srv) { return new NotifyModsAction(srv); } },

    /** @see CommandAction */
    command
    { public Action get(Server srv) { return new CommandAction(srv); } },

    /** @see LogAction */
    log
    { public Action get(Server srv) { return new LogAction(); } },

    /** @see CensorAction */
    censor // keep it last
    { public Action get(Server srv) { return new CensorAction(); } };


    /**
     * Returns an instance of the action type.
     * @param server The server instance.
     * @return An instance of the action type.
     */
    public abstract Action get(Server server);
}
