package de.upsj.bukkit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configuration section.
 * @author upsj
 * @version 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ConfigSection {
    /** The section name. */
    String name();
    /** The section description. */
    String description();
    /** The section values. */
    ConfigVar[] values() default { };
    /**
     * The parent object.
     * It must be annotated with {@link ConfigSection} as well.
     */
    Class parent() default Object.class;
}
