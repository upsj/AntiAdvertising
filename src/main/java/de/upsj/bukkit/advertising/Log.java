package de.upsj.bukkit.advertising;

import java.util.logging.Logger;

/**
 * Utility class for logging.
 * @author upsj
 * @version 1.0
 */
public final class Log {
    /** Logger. */
    private static Logger log = Logger.getLogger("Minecraft");
    /** Debug mode. */
    private static boolean debug = true;

    /** Avoid instances. */
    private Log() { }

    /**
     * Initializes the logging.
     * @param logger The logger.
     * @param dbg Debug mode?
     */
    public static void init(Logger logger, boolean dbg) {
        log = logger;
        debug = dbg;
    }

    /**
     * Sets the debug mode status.
     * @param dbg Debug mode?
     */
    public static void setDebugMode(boolean dbg) {
        debug = dbg;
    }

    /**
     * Logs the given message.
     * @param message The message.
     */
    public static void log(String message) {
        log.info(message);
    }

    /**
     * Logs a warning message.
     * @param message The message.
     */
    public static void warn(String message) {
        log.warning(message);
    }

    /**
     * Prints a debug message, if in debug mode.
     * @param message The message.
     */
    public static void debug(String message) {
        if (debug) {
            log.info("[DEBUG] " + message);
        }
    }
}
