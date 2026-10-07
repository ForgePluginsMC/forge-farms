package com.forge.farms.upgrade;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.event.FarmUpgradeEvent;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TrackType;
import com.forge.farms.config.UpgradeLevel;
import com.forge.farms.config.UpgradeTrack;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Independent upgrade tracks (radius / speed / storage / efficiency).
 * Each track levels on its own — no bundled farm levels.
 */
public final class UpgradeManager {
    private final ForgeFarms plugin;

    public UpgradeManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** The next purchasable level, or null when maxed / no track. */
    public @Nullable UpgradeLevel nextLevel(Farm farm, FarmType type, TrackType track) {
        UpgradeTrack t = type.upgradeTrack(track);
        if (t == null) {
            return null;
        }
        return t.level(farm.getLevel(track) + 1);
    }

    public int maxLevel(FarmType type, TrackType track) {
        UpgradeTrack t = type.upgradeTrack(track);
        return t == null ? 0 : t.maxLevel();
    }

    /**
     * Attempt to buy the next level of a track. Charges money (Vault)
     * and/or items, fires FarmUpgradeEvent first, then applies.
     */
    public boolean purchase(Player player, Farm farm, TrackType track) {
        FarmType type = plugin.config().getType(farm.typeId());
        if (type == null) {
            return false;
        }
        int current = farm.getLevel(track);
        UpgradeTrack t = type.upgradeTrack(track);
        if (t == null) {
            player.sendMessage(Text.mm("<red>This farm has no " + track.key() + " upgrades.</red>"));
            return false;
        }
        UpgradeLevel level = t.level(current + 1);
        if (level == null) {
            player.sendMessage(Text.mm("<yellow>" + trackTitle(track) + " is already maxed.</yellow>"));
            return false;
        }
        if (!level.hasCost()) {
            apply(player, farm, type, track, level);
            return true;
        }
        // Event first so other plugins can veto before money moves.
        FarmUpgradeEvent event = new FarmUpgradeEvent(farm, player, track, current, current + 1);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        // Money leg.
        if (level.costMoney() > 0) {
            if (!plugin.output().economy().isAvailable()) {
                plugin.output().tellNoEconomy(player);
                return false;
            }
            double balance = plugin.output().economy().balance(player);
            if (balance < level.costMoney()) {
                player.sendMessage(Text.mm("<red>You need <white>"
                        + plugin.output().formatMoney(level.costMoney())
                        + "</white> for this upgrade.</red>"));
                return false;
            }
        }
        // Item leg.
        Material costItem = level.costItem();
        if (costItem != null && level.costItemAmount() > 0) {
            if (!takeItems(player, costItem, level.costItemAmount())) {
                player.sendMessage(Text.mm("<red>You need <white>" + level.costItemAmount()
                        + "x " + pretty(costItem) + "</white> for this upgrade.</red>"));
                return false;
            }
        }
        // XP leg.
        if (level.costXpLevels() > 0 && player.getLevel() < level.costXpLevels()) {
            player.sendMessage(Text.mm("<red>You need <white>" + level.costXpLevels()
                    + " XP levels</white> for this upgrade.</red>"));
            return false;
        }
        if (level.costMoney() > 0 && !plugin.output().economy().withdraw(player, level.costMoney())) {
            player.sendMessage(Text.mm("<red>Payment failed — upgrade cancelled.</red>"));
            return false;
        }
        if (level.costXpLevels() > 0) {
            player.setLevel(player.getLevel() - level.costXpLevels());
        }
        apply(player, farm, type, track, level);
        return true;
    }

    private void apply(Player player, Farm farm, FarmType type, TrackType track, UpgradeLevel level) {
        farm.setLevel(track, level.level());
        if (track == TrackType.STORAGE) {
            farm.setStorageSlots(type.slotsAt(level.level()));
        }
        if (track == TrackType.SPEED) {
            plugin.farms().restartTick(farm);
        }
        plugin.farms().saveFarm(farm);
        plugin.holograms().refresh(farm);
        String effectDesc = level.description().isEmpty() ? trackTitle(track) + " " + level.level()
                : level.description();
        player.sendMessage(Text.mm("<green>Upgraded " + trackTitle(track) + " to <white>"
                + effectDesc + "</white>!</green>"));
    }

    private boolean takeItems(Player player, Material material, int amount) {
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                found += item.getAmount();
            }
        }
        if (found < amount) {
            return false;
        }
        player.getInventory().removeItem(new ItemStack(material, amount));
        return true;
    }

    public String trackTitle(TrackType track) {
        return switch (track) {
            case RADIUS -> "Radius";
            case SPEED -> "Speed";
            case STORAGE -> "Storage";
            case EFFICIENCY -> "Fuel efficiency";
        };
    }

    public String describeEffect(FarmType type, TrackType track, int level) {
        UpgradeTrack t = type.upgradeTrack(track);
        UpgradeLevel lvl = t == null ? null : t.level(level);
        if (lvl == null) {
            return switch (track) {
                case RADIUS -> type.baseRadius() + " blocks";
                case SPEED -> (type.baseTickInterval() / 20.0) + "s interval";
                case STORAGE -> type.baseStorageSlots() + " slots";
                case EFFICIENCY -> "x1.0 fuel";
            };
        }
        if (!lvl.description().isEmpty()) {
            return lvl.description();
        }
        return switch (track) {
            case RADIUS -> (int) lvl.effect() + " blocks";
            case SPEED -> (lvl.effect() / 20.0) + "s interval";
            case STORAGE -> (int) lvl.effect() + " slots";
            case EFFICIENCY -> "x" + lvl.effect() + " fuel";
        };
    }

    private String pretty(Material material) {
        String[] parts = material.name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
