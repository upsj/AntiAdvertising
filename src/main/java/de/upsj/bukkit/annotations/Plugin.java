package de.upsj.bukkit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bukkit Plugin (only one permitted).
 * @author upsj
 * @version 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Plugin {
    /** The plugin name. */
    String name();
    /** The plugin description. */
    String description();
    /** The plugin version. */
    String version();
    /** The plugin author. */
    String author();
}
