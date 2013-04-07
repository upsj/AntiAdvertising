package de.upsj.bukkit.advertising.util;

import java.util.HashMap;

/**
 * Map used for counting objects.
 * @author upsj
 * @version 1.0
 * @param <E> The element type.
 */
public class CountMap<E> extends HashMap<E, Integer> {
    /**
     * Increments the count of the given object.
     * @param obj The given object.
     */
    public void increment(E obj) {
        put(obj, get(obj) + 1);
    }

    /**
     * {@inheritDoc}
     * @return The count of the given object or 0.
     */
    public Integer get(Object obj) {
        Integer val = super.get(obj);
        return val == null ? 0 : val;
    }
}
