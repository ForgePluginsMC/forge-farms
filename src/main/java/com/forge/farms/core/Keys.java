package com.forge.farms.core;

import com.forge.farms.ForgeFarms;
import org.bukkit.NamespacedKey;

/**
 * Central registry of {@link NamespacedKey}s used in persistent data.
 * Initialized once in {@code onEnable}; the fields are null before that.
 */
public final class Keys {
    private Keys() {
    }

    /** Marks an ItemStack as a farm item; value is the farm type id. */
    public static NamespacedKey FARM_ITEM_TYPE;

    /** Marks a placed farm block entity; value is the farm UUID string. */
    public static NamespacedKey FARM_ID;

    /** Marks a TextDisplay as a ForgeFarms hologram; value is the farm UUID string. */
    public static NamespacedKey HOLOGRAM_FARM;

    public static void init(ForgeFarms plugin) {
        FARM_ITEM_TYPE = new NamespacedKey(plugin, "farm_item_type");
        FARM_ID = new NamespacedKey(plugin, "farm_id");
        HOLOGRAM_FARM = new NamespacedKey(plugin, "hologram_farm");
    }
}
