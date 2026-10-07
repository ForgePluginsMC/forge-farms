package com.forge.farms.api.event;

import com.forge.farms.config.TrackType;
import com.forge.farms.farm.Farm;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/** Fired when a track upgrade is purchased. Cancel to block it. */
public final class FarmUpgradeEvent extends FarmEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final TrackType track;
    private final int oldLevel;
    private final int newLevel;
    private boolean cancelled;

    public FarmUpgradeEvent(Farm farm, Player player, TrackType track, int oldLevel, int newLevel) {
        super(farm);
        this.player = player;
        this.track = track;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public TrackType getTrack() {
        return track;
    }

    public int getOldLevel() {
        return oldLevel;
    }

    public int getNewLevel() {
        return newLevel;
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
