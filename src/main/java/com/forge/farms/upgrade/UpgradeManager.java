package com.forge.farms.upgrade;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.event.FarmUpgradeEvent;
import com.forge.farms.config.FarmType;
import com.forge.farms.config.TrackType;
import com.forge.farms.config.UpgradeLevel;
import com.forge.farms.config.UpgradeTrack;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        if (level.cost().isFree()) {
            apply(player, farm, type, track, level);
            return true;
        }
        // Event first so other plugins can veto before money moves.
        FarmUpgradeEvent event = new FarmUpgradeEvent(farm, player, track, current, current + 1);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        List<String> unmet = level.cost().unmet(player, plugin.output().economy());
        if (!unmet.isEmpty()) {
            player.sendMessage(Text.mm("<red>You need " + String.join(", ", unmet)
                    + " for this upgrade.</red>"));
            return false;
        }
        if (!level.cost().charge(player, plugin.output().economy())) {
            player.sendMessage(Text.mm("<red>Payment failed — upgrade cancelled.</red>"));
            return false;
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
}
