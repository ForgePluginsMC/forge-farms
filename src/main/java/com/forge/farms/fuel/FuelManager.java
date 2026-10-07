package com.forge.farms.fuel;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Farm fuel: feed burnable items (coal etc.) into the farm to keep the
 * growth ticks running. Right-click the core while holding fuel.
 */
public final class FuelManager {
    private final ForgeFarms plugin;

    public FuelManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    public boolean isFuelItem(FarmType type, @Nullable ItemStack item) {
        return item != null && type.fuelItems().contains(item.getType());
    }

    /**
     * Add the held stack as fuel. Consumes the items, returns ticks added.
     * Efficiency upgrades stretch each item further.
     */
    public long addFuel(Player player, Farm farm, FarmType type, ItemStack hand) {
        if (!isFuelItem(type, hand)) {
            return 0;
        }
        int amount = hand.getAmount();
        double efficiency = type.efficiencyAt(farm.efficiencyLevel());
        long ticks = (long) (amount * type.fuelTicksPerItem() * efficiency);
        hand.setAmount(0);
        farm.addFuel(ticks);
        plugin.farms().saveFarm(farm);
        plugin.holograms().refresh(farm);
        player.sendMessage(Text.mm("<green>Added <white>" + formatDuration(ticks)
                + "</white> of fuel to your " + type.displayName() + "<green>.</green>"));
        return ticks;
    }

    public String formatDuration(long ticks) {
        long seconds = Math.max(0L, ticks / 20L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    /** Fuel remaining as a 0..1 fraction of a "full tank" (64 fuel items). */
    public double fuelFraction(Farm farm, FarmType type) {
        long full = Math.max(1L, (long) (64 * type.fuelTicksPerItem() * type.efficiencyAt(farm.efficiencyLevel())));
        return Math.min(1.0, farm.fuelTicks() / (double) full);
    }
}
