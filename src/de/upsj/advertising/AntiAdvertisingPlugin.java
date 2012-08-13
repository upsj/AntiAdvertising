package de.upsj.advertising;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiAdvertisingPlugin extends JavaPlugin implements Listener, Runnable {
	boolean checkDomains;
	boolean kickOnAdvert;
	boolean broadcastOnAdvert;
	boolean notifyOnAdvert;
	boolean commandOnAdvert;
	int kickAttempts;
	int commandAttempts;
	String broadcastMessage;
	String kickMessage;
	String notifyMessage;
	String command;
	String censorMessage;
	boolean enabled;
	boolean censorIP;
	boolean censorKnownServers;
	
	boolean countAttempts;
	
	Map<String, Integer> attemptsList;
	
	static boolean debug;
	
	Pattern ipPattern;
	Pattern domainPattern;
	Thread checkerThread;
	
	ConcurrentLinkedQueue<MCServer> toCheck;
	ConcurrentLinkedQueue<MCServer> checked;
	List<MCServer> servers;
	List<MCServer> noServers;
	List<MCServer> whitelist;
	
	List<String> chatCommands;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		new ChatHelper(getServer());
		toCheck = new ConcurrentLinkedQueue<MCServer>();
		checked = new ConcurrentLinkedQueue<MCServer>();
		servers = Collections.synchronizedList(new LinkedList<MCServer>());
		noServers = Collections.synchronizedList(new LinkedList<MCServer>());
		whitelist = Collections.synchronizedList(new LinkedList<MCServer>());
		
		attemptsList = Collections.synchronizedMap(new HashMap<String, Integer>());
		
		ipPattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d*)?");
		domainPattern = Pattern.compile("([0-9a-z_\\-]{2,}\\.)+[a-z]{2,}(:\\d*)?");
		
		reload();
		
		if (enabled) {
			checkerThread = new ServerChecker(toCheck, checked);
			checkerThread.start();
		}
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, this, 1, 1);
	}
	
	@Override
	public void onDisable()
	{
		if (enabled) {
			checkerThread.interrupt();
		}
	}

	public void reload()
	{
		reloadConfig();
		FileConfiguration conf = getConfig();
		
		enabled = conf.getBoolean("enabled", true);
		debug = conf.getBoolean("debug", false);
		kickOnAdvert = conf.getBoolean("action.kick.do", true);
		kickAttempts = conf.getInt("action.kick.attempts", 1);
		kickMessage = conf.getString("action.kick.message", "You have been kicked because of server advertising.");
		broadcastOnAdvert = conf.getBoolean("action.broadcast.do", true);
		broadcastMessage = conf.getString("action.broadcast.message", "&e%NAME% has been kicked because of server advertising.");
		notifyOnAdvert = conf.getBoolean("action.notify.do", true);
		notifyMessage = conf.getString("action.notify.message", "&e%NAME% adverted to %ADDRESS% - %INFO%");
		commandOnAdvert = conf.getBoolean("action.command.do", false);
		commandAttempts = conf.getInt("action.command.attempts", 1);
		command = conf.getString("action.command.command", "kick %NAME% Advertising for %SERVER%");
		censorIP = conf.getBoolean("action.censor.IP", true);
		censorKnownServers = conf.getBoolean("action.censor.KnownServers", true);
		censorMessage = conf.getString("action.censor.message", "&cYou're not allowed to do server advertising!");
		chatCommands = conf.getStringList("chat.commands");
		
		if (kickAttempts < 1) {
			ChatHelper.warn("Configuration warning: kick attempt count < 1 - will be set to 1");
			kickAttempts = 1;
		}
		
		if (commandAttempts < 1) {
			ChatHelper.warn("Configuration warning: command attempt count < 1 - will be set to 1");
			commandAttempts = 1;
		}
		
		countAttempts = !(commandAttempts == kickAttempts && kickAttempts == 1);
		
		if (chatCommands == null) {
			chatCommands = new LinkedList<String>();
			chatCommands.add("/tell");
		}
		
		List<String> whitelistAddr = conf.getStringList("whitelist");
		
		if (whitelistAddr == null) {
			whitelistAddr = new LinkedList<String>();
			whitelistAddr.add("localhost");
		} else {
			if (!whitelistAddr.contains("localhost")) {
				whitelistAddr.add("localhost");
			}
		}
		
		synchronized (whitelist) {
			for (String line : whitelistAddr) {
				try {
					whitelist.add(MCServer.fromAddress(line));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
		
		conf.set("enabled", enabled);
		conf.set("action.kick.do", kickOnAdvert);
		conf.set("action.kick.attempts", kickAttempts);
		conf.set("action.kick.message", kickMessage);
		conf.set("action.broadcast.do", broadcastOnAdvert);
		conf.set("action.broadcast.message", broadcastMessage);
		conf.set("action.notify.do", notifyOnAdvert);
		conf.set("action.notify.message", notifyMessage);
		conf.set("action.command.do", commandOnAdvert);
		conf.set("action.command.attempts", commandAttempts);
		conf.set("action.command.command", command);
		conf.set("action.censor.IP", censorIP);
		conf.set("action.censor.KnownServers", censorKnownServers);
		conf.set("action.censor.message", censorMessage);
		
		whitelistAddr = new LinkedList<String>();
		synchronized (whitelist) {
			for (MCServer server : whitelist) {
				whitelistAddr.add(server.address);
			}
		}
		
		conf.set("whitelist", whitelistAddr);
		
		conf.set("chat.commands", chatCommands);
		
		saveConfig();
	}
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		if (!checkChat(e.getPlayer(), e.getMessage().toLowerCase()))
			e.setCancelled(true);
		if (censorIP && enabled)
			e.setMessage(ipPattern.matcher(e.getMessage()).replaceAll("*.*.*.*"));
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		for (String s : chatCommands) {
			if ((e.getMessage().toLowerCase() + " ").startsWith(s.toLowerCase() + " ")) {
				if (!checkChat(e.getPlayer(), e.getMessage().toLowerCase()))
					e.setCancelled(true);
				return;
			}
		}
	}

	private boolean checkChat(Player player, String message) {
		if (enabled) {
			if (player.hasPermission("AntiAdvertising.ignore"))
				return true;
			List<String> matches = new LinkedList<String>();
			
			Matcher m = ipPattern.matcher(message);
			while(m.find()) {
				matches.add(m.group());
			}
			
			m = domainPattern.matcher(message);
			while(m.find()) {
				matches.add(m.group());
			}
			
			for (String addr : matches) {
				if (debug)
					ChatHelper.log("Found possible address: " + addr);
				try {
					MCServer server = MCServer.fromAddress(addr);
					if (server.addrObj.getAddress()[0] == 127) {
						if (debug)
							ChatHelper.log("Server " + addr + " is localhost");
						continue;
					}
					
					boolean whitelisted;
					synchronized (whitelist) {
						whitelisted = whitelist.contains(server);						
					}
					
					if (!whitelisted) {
						synchronized (servers) {
							for (MCServer s : servers) {
								if (s.equals(server)) {
									if (debug)
										ChatHelper.log(addr + " is listed mc server");
									s.mentionedBy = player.getName();
									doActions(s);
									
									if (censorKnownServers) {
										ChatHelper.sendMessage(player, censorMessage);
										return false;
									}
									return true;
								}
							}
						}
						
						synchronized (noServers) {
							for (MCServer s : noServers) {
								if (s.equals(server)) {
									if (debug)
										ChatHelper.log(addr + " is listed as non-mc server");
									continue;
								}
							}
						}
						
						if (debug)
							ChatHelper.log("Pinging server " + addr);
						server.mentionedBy = player.getName();
						toCheck.add(server);
						synchronized(toCheck) {
							toCheck.notify();
						}
					} else {
						if (debug)
							ChatHelper.log("Server " + addr + " is whitelisted");
					}
				} catch (UnknownHostException e1) {
					if (debug)
						ChatHelper.log("Unresolved Hostname: " + addr);
				}
			}
		}
		return true;
	}
	
	private void doActions(MCServer server) {		
		Integer attempts;
		if (countAttempts) {
			synchronized (attemptsList) {
				attempts = attemptsList.get(server.mentionedBy);
				if (attempts == null)
					attempts = 0;
				attempts++;
				attemptsList.put(server.mentionedBy, attempts);
			}
		} else {
			attempts = 1;
		}
		
		if (broadcastOnAdvert) {
			ChatHelper.broadcastMessage(broadcastMessage.replace("%NAME%", server.mentionedBy));
		}
		
		if (notifyOnAdvert) {
			String message = notifyMessage.replace("%NAME%", server.mentionedBy).replace("%ADDRESS%", server.address).replace("%INFO%", server.getInfo());
			for (Player p : getServer().getOnlinePlayers()) {
				if (p.hasPermission("AntiAdvertising.notify"))
					ChatHelper.sendMessage(p, message);
			}
			ChatHelper.log(ChatHelper.removeColorCodes(message));
		}
		
		if (commandOnAdvert && attempts >= commandAttempts) {
			try {
				getServer().dispatchCommand(getServer().getConsoleSender(), command.replace("%NAME%", server.mentionedBy).replace("%SERVER%", server.address));
			} catch (CommandException e) {
				ChatHelper.log.severe(ChatHelper.prefix + "Command exception - You should check your config.yml for \"command\": " + e.getMessage());
			}
		}
		
		if (kickOnAdvert && attempts >= kickAttempts) {
			Player p = getServer().getPlayerExact(server.mentionedBy);
			if (p != null) {
				p.kickPlayer(kickMessage);
			}
		}
	}

	@Override
	public void run() {
		MCServer finished;
		while ((finished = checked.poll()) != null) {
			if (finished.found) {
				synchronized (servers) {
					servers.add(finished);
				}
				doActions(finished);
			} else {
				synchronized (noServers) {
					noServers.add(finished);
				}
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("AntiAdvertReload")) {
			reload();
			ChatHelper.sendMessage(sender, "&eAntiAdvertReload finished");
			return true;
		}
		return false;
	}
}
