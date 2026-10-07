package com.forge.farms.api.event;

import com.forge.farms.farm.Farm;
import org.bukkit.event.HandlerList;

/** Fired once when a farm's fuel tank hits empty (not every tick). */
public final class FarmFuelEmptyEvent extends FarmEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public FarmFuelEmptyEvent(Farm farm) {
        super(farm);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
