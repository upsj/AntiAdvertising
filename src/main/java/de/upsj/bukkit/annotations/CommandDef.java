package de.upsj.bukkit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Command definition.
 * @author upsj
 * @version 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface CommandDef {
    /** The command name. */
    String name();
    /** The command aliases. */
    String[] aliases() default { };
    /** The command description. */
    String description();
    /**
     * The command permission.
     * There must be a string constant equal to this
     * annotated with {@link Permission}.
     */
    String permission();
}
