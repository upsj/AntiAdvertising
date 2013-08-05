package de.upsj.bukkit.advertising.test;

import de.upsj.bukkit.advertising.AntiAdvertisingPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.util.logging.Logger;

public class ConfigGenerationTest {
    @Test
    public void testConfigGeneration() {
        final YamlConfiguration config = new YamlConfiguration();
        AntiAdvertisingPlugin plugin = new AntiAdvertisingPlugin() {
            @Override
            protected Logger logger() {
                return Logger.getAnonymousLogger();
            }

            @Override
            public FileConfiguration getConfig() {
                return config;
            }

            @Override
            public void reloadConfig() { }

            @Override
            public void saveConfig() {
                System.out.println(config.saveToString());
            }
        };
        try {
            plugin.onEnable();
        } catch (NullPointerException npe) {}
    }

}
