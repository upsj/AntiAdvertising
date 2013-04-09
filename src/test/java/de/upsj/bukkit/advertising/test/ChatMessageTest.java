package de.upsj.bukkit.advertising.test;

import de.upsj.bukkit.advertising.ChatMessage;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChatMessageTest {
    @Test
    public void testParseMultiDomain() {
        final int port = 25560;
        ChatMessage msg = ChatMessage.parse("Player",
                "Hey! Come and visit my server at " + TestConfig.TEST_SERVER + ":" + port
                + " " + TestConfig.TEST_NO_SERVER);
        assertFalse("Obvious advertising not found", msg.getState() == ChatMessage.State.CLEAN);
        assertEquals("Match count", 2, msg.getMatchCount());
        assertEquals("Address 1", TestConfig.TEST_SERVER, msg.getMatch(0).getMatchedAddress());
        assertEquals("Port 1", port, msg.getMatch(0).getPort());
        assertEquals("Address 2", TestConfig.TEST_NO_SERVER, msg.getMatch(1).getMatchedAddress());
        assertEquals("Port 2", TestConfig.DEFAULT_PORT, msg.getMatch(1).getPort());
    }

    @Test
    public void testParseMultiIP() {
        final int port = 8080;
        final String addr = "127.0.0.1";
        ChatMessage msg = ChatMessage.parse("Player",
                // invalid IP to check bounds
                "Hey! 256.127.0.12 Come and visit my servers at " + addr + ":" + port + " and " + addr);
        assertFalse("Obvious advertising not found", msg.getState() == ChatMessage.State.CLEAN);
        assertEquals("Match count", 2, msg.getMatchCount());
        assertEquals("Address equality", msg.getMatch(0).getMatchedAddress(), msg.getMatch(1).getMatchedAddress());
        assertTrue("Address", msg.getMatch(0).getMatchedAddress().contains("127.0.0.1"));
        assertEquals("Port 1", port, msg.getMatch(0).getPort());
        assertEquals("Port 2", TestConfig.DEFAULT_PORT, msg.getMatch(1).getPort());
    }

    @Test
    public void testParseClean() {
        ChatMessage msg = ChatMessage.parse("Player",
                "just some normal text that shouldn't be suspicious in any way.");
        assertEquals("Clean message suspicious", ChatMessage.State.CLEAN, msg.getState());
    }

    @Test
    public void testCensor() {
        String s1 = "This page google.com is just so 127.0.0.1:4922!";
        String s2 = "This page ********** is just so 127.0.0.1:4922!";
        String s3 = "This page ********** is just so **************!";
        ChatMessage msg = ChatMessage.parse("Player", s1);
        msg.censorMatch(0);
        assertEquals(s2, msg.getMessage());
        msg.censorMatch(1);
        assertEquals(s3, msg.getMessage());
    }

    @Test
    public void testPerformance() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            ChatMessage.parse("Player", "just some normal text that shouldn't be suspicious in any way.");
        }
        System.out.println ("Normal message: " + ((System.currentTimeMillis() - start) / 10000f) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            ChatMessage.parse("Player", "This page google.com is just so 127.0.0.1:4922!");
        }
        System.out.println("Double advertising: " + ((System.currentTimeMillis() - start) / 10000f) + " ms");
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            ChatMessage.parse("Player", "Hey! Come and visit my server at " + TestConfig.TEST_SERVER + ":" + TestConfig.DEFAULT_PORT);
        }
        System.out.println("Single advertising: " + ((System.currentTimeMillis() - start) / 10000f) + " ms");
    }
}
