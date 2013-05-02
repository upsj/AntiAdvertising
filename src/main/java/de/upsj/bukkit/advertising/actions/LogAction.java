package de.upsj.bukkit.advertising.actions;

import de.upsj.bukkit.advertising.Action;
import de.upsj.bukkit.advertising.ActionHandler;
import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.advertising.servers.PotentialServer;
import de.upsj.bukkit.annotations.ConfigSection;
import org.bukkit.Server;

import java.io.IOException;
import java.util.Date;
import java.util.logging.*;

/**
 * Logs the advertisement.
 * @author upsj
 * @version 1.0
 */
@ConfigSection(name = "log", // keep up-do-date with the Actions enum!
        description = "Logs the advertisement.",
        parent = ActionHandler.class
)
public class LogAction extends Action {
    private final Logger log;

    /** Initializes the log action. */
    public LogAction(Server srv) {
        // Initialize logger, remove old file handler
        log = Logger.getLogger("AntiAdvertising");
        Handler[] handlers = log.getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i] instanceof LogHandler) {
                log.removeHandler(handlers[i]);
            }
        }

        try {
            Handler newHandler = new LogHandler("advertisement.log", true);
            newHandler.setFormatter(new LogFormatter());
            log.addHandler(newHandler);
        } catch (IOException e) {
            e.printStackTrace();
            srv.getLogger().warning("[AntiAdvertisement] Creating log file failed");
        }
    }

    @Override
    public void doAction(ChatMessage message) {
        StringBuilder builder = new StringBuilder(message.getSender());
        builder.append(" advertised server(s) ");
        for (int i = 0; i < message.getMatchCount(); i++) {
            PotentialServer server = message.getMatch(i);
            if (server != null && server.isServer()) {
                builder.append("(");
                builder.append(server.toDisplayString());
                builder.append(") ");
            }
        }
        builder.append(" - message: '");
        builder.append(message.getMessage());
        builder.append("'");
        log.info(builder.toString());
    }

    /** Indicator class for log handler used by LogAction logger. */
    private static class LogHandler extends FileHandler { public LogHandler(String pattern, boolean append) throws IOException, SecurityException { super(pattern, append); } }

    /** Log formatter. */
    private static class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return new Date(record.getMillis()).toString() + ": " + record.getMessage() + "\n";
        }
    }
}
