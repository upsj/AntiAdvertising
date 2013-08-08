package de.upsj.bukkit.advertising.util;

import de.upsj.bukkit.advertising.Log;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class SRVRecord {
    private static DirContext srvContext;
    static {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        try {
            srvContext = new InitialDirContext(env);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private String newDomain;
    private int newPort;

    public SRVRecord(String domain, int port, String type) {
        String[] parts = new String[0];
        try {
            Attributes a = srvContext.getAttributes("_" + type + "._tcp." + domain, new String[]{"SRV"});
            Attribute attr;
            Object obj;
            if (a != null && (attr = a.get("srv")) != null && (obj = attr.get(0)) != null) {
                String msg = obj.toString();
                parts = msg.split(" ");
            }
        } catch (NamingException e) {
            Log.debug("SRV request for " + domain + " failed because of " + e.getClass().getSimpleName());
        }

        try {
            newPort = Integer.parseInt(parts[2]);
            newDomain = parts[3];
        } catch (Exception e) { // NumberFormatException and ArrayOutOfBoundsException - quick 'n' dirty
            newPort = port;
            newDomain = domain;
        }
    }

    public String getDomain() {
        return newDomain;
    }

    public int getPort() {
        return newPort;
    }
}
