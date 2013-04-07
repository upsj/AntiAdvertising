package de.upsj.bukkit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Plugin permission.
 * @author upsj
 * @version 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Permission {
    /** Permission description. */
    String value();
    /** Default status of this permission. */
    PermissionDefault defaultPerm() default PermissionDefault.OP;
}
