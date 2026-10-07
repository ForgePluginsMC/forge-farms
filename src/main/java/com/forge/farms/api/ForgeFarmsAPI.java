package com.forge.farms.api;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Keys;
import com.forge.farms.farm.Farm;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Public static API for other plugins. Available after ForgeFarms enables;
 * {@link #isAvailable()} guards early access.
 */
public final class ForgeFarmsAPI {
    private static ForgeFarms plugin;

    private ForgeFarmsAPI() {
    }

    public static void init(ForgeFarms plugin) {
        ForgeFarmsAPI.plugin = plugin;
    }

    public static void shutdown() {
        plugin = null;
    }

    public static boolean isAvailable() {
        return plugin != null;
    }

    private static ForgeFarms require() {
        if (plugin == null) {
            throw new IllegalStateException("ForgeFarms is not enabled");
        }
        return plugin;
    }

    public static @Nullable Farm getFarm(UUID farmId) {
        return require().farms().getFarm(farmId);
    }

    public static @Nullable Farm getFarmAt(Location location) {
        return require().farms().getByCore(location);
    }

    public static @Nullable Farm getFarmContaining(Location location) {
        return require().farms().farmContaining(location);
    }

    public static Collection<Farm> getFarms() {
        return require().farms().all();
    }

    public static List<Farm> getFarmsOf(UUID owner) {
        return require().farms().getByOwner(owner);
    }

    public static @Nullable FarmType getFarmType(String id) {
        return require().config().getType(id);
    }

    public static Collection<FarmType> getFarmTypes() {
        return require().config().getTypes();
    }

    /** True when the item is a ForgeFarms farm item (any type). */
    public static boolean isFarmItem(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(Keys.FARM_ITEM_TYPE, PersistentDataType.STRING);
    }

    /** The farm type id stored on a farm item, or null. */
    public static @Nullable String farmItemType(@Nullable ItemStack item) {
        if (!isFarmItem(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(Keys.FARM_ITEM_TYPE, PersistentDataType.STRING);
    }

    /**
     * Build a farm item for a type. Amount is clamped to 1..64.
     * Returns null for unknown types.
     */
    public static @Nullable ItemStack createFarmItem(String typeId, int amount) {
        ForgeFarms pl = require();
        FarmType type = pl.config().getType(typeId);
        if (type == null) {
            return null;
        }
        ItemStack item = new ItemStack(type.itemMaterial(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.FARM_ITEM_TYPE, PersistentDataType.STRING, type.id());
        meta.displayName(com.forge.farms.core.Text.mm(type.itemName()));
        meta.lore(type.itemLore().stream()
                .map(line -> com.forge.farms.core.Text.mm(line
                        .replace("{radius}", String.valueOf(type.baseRadius()))
                        .replace("{interval}", String.format("%.1f", type.baseTickInterval() / 20.0))
                        .replace("{slots}", String.valueOf(type.baseStorageSlots()))))
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    /** NamespacedKey marking farm items ({@code forgefarms:farm_item_type}). */
    public static NamespacedKey farmItemKey() {
        return Keys.FARM_ITEM_TYPE;
    }

    /** NamespacedKey tagging hologram entities ({@code forgefarms:hologram_farm}). */
    public static NamespacedKey hologramKey() {
        return Keys.HOLOGRAM_FARM;
    }

    /** Remove a farm by id. Returns false when it did not exist or removal was vetoed. */
    public static boolean removeFarm(UUID farmId) {
        return require().farms().removeFarm(farmId);
    }

    /** Give a farm item to a player. Returns false for unknown types. */
    public static boolean giveFarmItem(Player player, String typeId, int amount) {
        ItemStack item = createFarmItem(typeId, amount);
        if (item == null) {
            return false;
        }
        player.getInventory().addItem(item);
        return true;
    }
}
