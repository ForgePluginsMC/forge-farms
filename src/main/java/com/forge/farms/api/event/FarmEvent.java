package com.forge.farms.api.event;

import com.forge.farms.farm.Farm;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Base class for all ForgeFarms events. */
public abstract class FarmEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    protected final Farm farm;

    protected FarmEvent(Farm farm) {
        this.farm = farm;
    }

    /** The farm this event concerns. */
    public Farm getFarm() {
        return farm;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
