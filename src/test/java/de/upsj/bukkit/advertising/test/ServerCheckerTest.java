package de.upsj.bukkit.advertising.test;

import de.upsj.bukkit.advertising.ChatMessage;
import de.upsj.bukkit.advertising.ServerChecker;
import de.upsj.bukkit.advertising.servers.PotentialServer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServerCheckerTest {
    private ChatMessage msg1;
    private ChatMessage msg2;
    private ChatMessage msg3;
    private ChatMessage original1;
    private ChatMessage original2;
    private ChatMessage original3;
    private PotentialServer whitelisted;
    private ServerChecker checker;

    @Before
    public void setup() {
        msg1 = ChatMessage.parse("Player", "Hallo! Geht auf meinen Server unter " + TestConfig.TEST_NO_SERVER);
        msg2 = ChatMessage.parse("Player", "Hallo! Geht auf meinen Server unter " + TestConfig.TEST_SERVER);
        msg3 = ChatMessage.parse("Player", "Hallo! Geht auf meinen Server unter " + TestConfig.TEST_SERVER
                + " " + TestConfig.TEST_SERVER + " " + TestConfig.TEST_NO_SERVER);
        original1 = msg1.clone();
        original2 = msg2.clone();
        original3 = msg3.clone();
        whitelisted = new PotentialServer(TestConfig.TEST_SERVER, true);
        checker = new ServerChecker();
        checker.reloadConfig(new YamlConfiguration());
    }

    private void waitForCompletion() throws InterruptedException {
        checker.awaitCompletion();
        checker.run();
        checker.awaitCompletion();
        checker.run();
    }

    @After
    public void shutdown() throws InterruptedException {
        checker.awaitCompletion();
        checker.shutdown();
    }

    @Test(timeout=10000)
    public void testStandardBehaviour() throws InterruptedException {
        checker.registerMessage(msg1);
        waitForCompletion();
        assertEquals(ChatMessage.State.CLEAN, msg1.getState());

        checker.registerMessage(msg2);
        waitForCompletion();
        assertEquals(ChatMessage.State.ADVERTISEMENT, msg2.getState());
    }

    @Test(timeout=10000)
    public void testConcurrentRequests() throws InterruptedException {
        checker.registerMessage(msg1);
        checker.registerMessage(msg2);
        waitForCompletion();
        assertEquals(ChatMessage.State.CLEAN, msg1.getState());
        assertEquals(ChatMessage.State.ADVERTISEMENT, msg2.getState());
    }

    @Test(timeout=10000)
    public void testDuplicate() throws InterruptedException {
        checker.registerMessage(msg1);
        checker.registerMessage(msg2);
        waitForCompletion();
        checker.registerMessage(msg3);
        assertEquals("Already known server", ChatMessage.State.ADVERTISEMENT, msg3.getState());
        checker.registerMessage(msg1);
        assertEquals("Already known non-server", ChatMessage.State.CLEAN, msg1.getState());
    }

    @Test(timeout=10000)
    public void testWhitelist() throws InterruptedException {
        checker.add(whitelisted, null);
        waitForCompletion();
        checker.registerMessage(msg2);
        checker.registerMessage(msg3);
        waitForCompletion();
        assertEquals("Whitelisted server", ChatMessage.State.CLEAN, msg2.getState());
        assertEquals("Whitelisted server", ChatMessage.State.CLEAN, msg3.getState());
    }

    @Test(timeout=10000)
    public void testAlreadyKnown() throws InterruptedException {
        checker.registerMessage(msg1);
        checker.registerMessage(msg2);
        checker.registerMessage(msg3);
        waitForCompletion();
        checker.registerMessage(original1);
        checker.registerMessage(original2);
        checker.registerMessage(original3);
        assertEquals("Different states 1", msg1.getState(), original1.getState());
        assertEquals("Different states 2", msg2.getState(), original2.getState());
        assertEquals("Different states 3", msg3.getState(), original3.getState());
    }
}
