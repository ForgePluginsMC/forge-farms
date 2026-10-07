package com.forge.farms.api.event;

import com.forge.farms.farm.Farm;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Fired when a farm is placed. Cancel to prevent creation. */
public final class FarmCreateEvent extends FarmEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player owner;
    private boolean cancelled;

    public FarmCreateEvent(Farm farm, Player owner) {
        super(farm);
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
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
