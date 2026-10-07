package com.forge.farms.api.event;

import com.forge.farms.farm.Farm;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Fired when a farm is removed. Cancel to keep it. */
public final class FarmRemoveEvent extends FarmEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    public FarmRemoveEvent(Farm farm) {
        super(farm);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
