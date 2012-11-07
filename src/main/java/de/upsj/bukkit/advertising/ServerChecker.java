package de.upsj.bukkit.advertising;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerChecker extends Thread {
	ConcurrentLinkedQueue<MCServer> toCheck;
	ConcurrentLinkedQueue<MCServer> checked;

	public ServerChecker(ConcurrentLinkedQueue<MCServer> toCheck, ConcurrentLinkedQueue<MCServer> checked)
	{
		this.toCheck = toCheck;
		this.checked = checked;
	}

	@Override
	public void run()
	{
		while (!this.isInterrupted())
		{
			if (toCheck.isEmpty())
			{
				synchronized (toCheck) {
					try {
						toCheck.wait();
					} catch (InterruptedException e) {
						break;
					}
				}
			}
			
			MCServer server;
			while ((server = toCheck.poll()) != null)
			{
				try {
					server.ping();
				} catch (IOException e) {
					if (AntiAdvertisingPlugin.debug)
					{
						ChatHelper.log("Couldn't ping server " + server.address + ": " + e.getMessage());
					}
				}
				checked.add(server);
			}
		}
	}

}
