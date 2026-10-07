package com.forge.farms.hook;

import com.forge.farms.ForgeFarms;
import com.forge.farms.farm.Farm;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion ({@code %forgefarms_...%}).
 * Only loaded when PlaceholderAPI is present (softdepend).
 *
 * <ul>
 *   <li>{@code %forgefarms_farms_owned%} — farms the player owns</li>
 *   <li>{@code %forgefarms_farms_limit%} — per-player farm cap</li>
 *   <li>{@code %forgefarms_farm_fuel_<id>%} — fuel time remaining</li>
 *   <li>{@code %forgefarms_farm_storage_<id>%} — used/total slots</li>
 * </ul>
 */
public final class PlaceholderHook extends PlaceholderExpansion {
    private final ForgeFarms plugin;

    public PlaceholderHook(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "forgefarms";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Forge";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        if (params.equals("farms_owned")) {
            return String.valueOf(plugin.farms().countByOwner(player.getUniqueId()));
        }
        if (params.equals("farms_limit")) {
            return String.valueOf(plugin.config().maxFarmsPerPlayer());
        }
        if (params.startsWith("farm_fuel_")) {
            Farm farm = byId(params.substring("farm_fuel_".length()));
            return farm == null ? null : plugin.fuel().formatDuration(farm.fuelTicks());
        }
        if (params.startsWith("farm_storage_")) {
            Farm farm = byId(params.substring("farm_storage_".length()));
            return farm == null ? null : farm.usedSlots() + "/" + farm.storageSlots();
        }
        return null;
    }

    private @Nullable Farm byId(String raw) {
        try {
            return plugin.farms().getFarm(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
