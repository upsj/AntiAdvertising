package de.upsj.bukkit.annotations;

/**
 * Default status of a {@link Permission}.
 * @author upsj
 * @version 1.0
 */
public enum PermissionDefault {
    /** The permission is granted by default. */
    TRUE("true"),
    /** The permission is not granted by default. */
    FALSE("false"),
    /** The permission is granted to OPs by default. */
    OP("op")
    /*, NO_OP("no op")*/;

    /** YAML value. */
    private final String name;
    /**
     * Initializes a permission default status.
     * @param value The YAML value of the default status.
     */
    private PermissionDefault(String value) {
        this.name = value;
    }

    /** @return The YAML value for the default status. */
    public String getValue() {
        return name;
    }
}
