package com.forge.farms.api.event;

import com.forge.farms.farm.Farm;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a growth tick harvests items, before they enter the output
 * pipeline. Mutate the drops list to change what the farm produces.
 */
public final class FarmHarvestEvent extends FarmEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final List<ItemStack> drops;

    public FarmHarvestEvent(Farm farm, List<ItemStack> drops) {
        super(farm);
        this.drops = new ArrayList<>(drops);
    }

    public List<ItemStack> getDrops() {
        return drops;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
