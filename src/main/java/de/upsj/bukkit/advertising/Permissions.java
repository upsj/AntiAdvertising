package de.upsj.bukkit.advertising;

import de.upsj.bukkit.annotations.Permission;

/**
 * Permissions constants.
 * @author upsj
 * @version 1.0
 */
// TODO wildcard permissions
public final class Permissions {
    /** Avoid instances. */
    private Permissions() { }
    private static final String PREFIX = "antiad.";

    @Permission("Permission to receive notifications about server advertisement.")
    public static final String NOTIFY = PREFIX + "notify";

    @Permission("Permission to send chat messages without being checked.")
    public static final String IGNORE = PREFIX + "ignore";

    @Permission("Permissions to reload the configuration of the plugin.")
    public static final String RELOAD = PREFIX + "reload";

    @Permission("Permissions to ping potential servers manually.")
    public static final String PING   = PREFIX + "ping";
}
