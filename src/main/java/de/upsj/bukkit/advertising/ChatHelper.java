package de.upsj.bukkit.advertising;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

public class ChatHelper {
	static Logger log = Logger.getLogger("Minecraft");
	static Server server;
	public static final String prefix = "[AntiAd] ";

	public ChatHelper(Server server) {
		ChatHelper.server = server;
	}

	public static String replaceColorCodes(String message) {
		return message.replaceAll("(&([a-f0-9]))", ChatColor.COLOR_CHAR + "$2");
	}

	public static String removeColorCodes(String message) {
		return message.replaceAll("(&([a-f0-9]))", "");
	}

	public static void log(String message) {
		log.info(prefix + message);
	}
	
	public static void warn(String message) {
		log.warning(prefix + message);
	}

	public static void broadcastMessage(String message) {
		server.broadcastMessage(replaceColorCodes(message));
	}

	public static void sendMessage(CommandSender sender, String message) {
		sender.sendMessage(replaceColorCodes(message));
	}
}
