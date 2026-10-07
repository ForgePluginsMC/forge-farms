package com.forge.farms.api.event;

import com.forge.farms.farm.Farm;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when harvests overflow every output mode. The overflow items would
 * otherwise be dropped at the farm core.
 */
public final class FarmStorageFullEvent extends FarmEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final List<ItemStack> overflow;

    public FarmStorageFullEvent(Farm farm, List<ItemStack> overflow) {
        super(farm);
        this.overflow = List.copyOf(overflow);
    }

    public List<ItemStack> getOverflow() {
        return overflow;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
