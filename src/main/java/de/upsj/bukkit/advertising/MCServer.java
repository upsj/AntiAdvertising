package de.upsj.bukkit.advertising;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class MCServer {
	public int port;
	public String response;
	public String mentionedBy;
	public String address;
	public boolean found;
	public static final int StandardPort = 25565;
	
	InetAddress addrObj;
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof MCServer)
		{
			MCServer m = (MCServer) o;
			byte[] part1 = m.addrObj.getAddress();
			byte[] part2 = addrObj.getAddress();
			return (part1[0] == part2[0]) && (part1[1] == part2[1]) && (part1[2] == part2[2]) && (part1[3] == part2[3]);
		}
		return false;
	}
	
	public static MCServer fromAddress(String domainString)
	{
		MCServer s = new MCServer();
		String[] parts = domainString.split(":");
		if (parts.length == 1)
		{
			s.port = StandardPort;
		}
		else
		{
			s.port = Integer.parseInt(parts[1]);
		}
		try {
			s.addrObj = InetAddress.getByName(parts[0]);
		} catch (UnknownHostException e) {
			if (AntiAdvertisingPlugin.debug)
				ChatHelper.log("Unresolved hostname: " + parts[0]);
			try {
				s.addrObj = InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 });
			} catch (UnknownHostException e1) { }
		}
		s.address = domainString;
		s.found = false;
		
		return s;
	}

	public void ping() throws IOException {
		Socket sock = new Socket();
		sock.connect(new InetSocketAddress(addrObj, port), 5000);
		sock.setSoTimeout(5000);
		sock.getOutputStream().write(254);
		InputStream is = sock.getInputStream();
		int packet = is.read();
		if (packet != 255)
		{
			throw new IOException("Invalid server response");
		}
		is.read();
		is.read();
		byte[] responseBytes = new byte[256];
		sock.getInputStream().read(responseBytes);
		response = new String(responseBytes, Charset.forName("UTF-16"));
		found = true;
		sock.close();
	}

	public String getInfo() {
		if (response == null)
			return null;
		String[] parts = response.split("§");
		if (parts.length != 3)
			return response;
		return parts[0] + " (" + parts[1] + "/" + parts[2] + ")";
	}
}
