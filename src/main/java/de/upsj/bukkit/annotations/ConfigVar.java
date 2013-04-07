package de.upsj.bukkit.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Configuration value.
 * @author upsj
 * @version 1.0
 */
@Retention(RetentionPolicy.SOURCE)
public @interface ConfigVar {
    /** The config value name. */
    String name();
    /** The config value description. */
    String description();
    /** The config value type. */
    ConfigVarType type();
}
