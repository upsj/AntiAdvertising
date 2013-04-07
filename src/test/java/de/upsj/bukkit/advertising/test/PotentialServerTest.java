package de.upsj.bukkit.advertising.test;

import de.upsj.bukkit.advertising.servers.PotentialServer;
import org.junit.Test;
import static org.junit.Assert.*;

public class PotentialServerTest {
    @Test(timeout=5000)
    public void testNoServer() {
        PotentialServer server = new PotentialServer("google.com");
        assertFalse("New server claims to be final", server.isFinal());
        assertFalse("New server claims to be known server", server.isServer());
        // Requires network permission
        PotentialServer server2 = server.call().call();
        assertTrue("Pinged server not final", server2.isFinal());
        // Should work as long as google doesn't start a minecraft server.
        assertFalse("google.com considered minecraft server", server2.isServer());
    }

    @Test(timeout=5000)
    public void testServer() {
        PotentialServer server = new PotentialServer("server.minecraft.name");
        assertFalse("New server claims to be final", server.isFinal());
        assertFalse("New server claims to be known server", server.isServer());
        // Requires network permission
        PotentialServer server2 = server.call().call();
        assertTrue("Pinged server not final", server2.isFinal());
        // Should work as long as the server is still running.
        assertTrue("server.minecraft.name not considered minecraft server", server2.isServer());
        System.out.println(server2);
    }
}
