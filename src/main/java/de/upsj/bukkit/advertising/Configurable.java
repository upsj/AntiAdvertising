package de.upsj.bukkit.advertising;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Interface for classes that use configuration values.
 * @author upsj
 * @version 1.0
 */
public interface Configurable {
    /**
     * Reloads the configuration values from the given configuration section.
     * If any values are missing, they will be set to default values.
     * @param section The configuration section.
     */
    void reloadConfig(ConfigurationSection section);
}
