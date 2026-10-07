package com.forge.farms.core;

import com.forge.farms.ForgeFarms;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Folia-safe scheduling facade.
 *
 * <p>Paper 26.3 ships the {@code threadedregions} scheduler API on both
 * regular Paper and Folia: region tasks run on the owning region's thread,
 * global tasks on the global thread. One code path therefore serves both
 * server types with no runtime detection.
 *
 * <p>Note: {@code RegionScheduler} has no {@code cancelTasks(Plugin)} in
 * 26.3 — region tasks are cancelled individually, so this facade tracks
 * every region task it creates.
 */
public final class Scheduler {
    private final ForgeFarms plugin;
    private final Set<ScheduledTask> regionTasks = ConcurrentHashMap.newKeySet();

    public Scheduler(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** Repeating task bound to the region owning {@code location}. */
    public ScheduledTask regionAtFixedRate(Location location, Consumer<ScheduledTask> task,
            long delayTicks, long periodTicks) {
        ScheduledTask scheduled = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, task,
                delayTicks, periodTicks);
        regionTasks.add(scheduled);
        return scheduled;
    }

    /** One-shot task bound to the region owning {@code location}. */
    public void regionLater(Location location, Consumer<ScheduledTask> task, long delayTicks) {
        ScheduledTask scheduled = Bukkit.getRegionScheduler().runDelayed(plugin, location, task, delayTicks);
        regionTasks.add(scheduled);
    }

    /** Run immediately on the region owning {@code location}. */
    public void region(Location location, Runnable task) {
        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    /** Repeating global task (not bound to any region). */
    public ScheduledTask globalAtFixedRate(Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task, delayTicks, periodTicks);
    }

    /** Run on the async scheduler (never touch world state here). */
    public void async(Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    /** Run async after a tick delay (delay is in ticks, 20 = 1 second). */
    public void asyncLater(Runnable task, long delayTicks) {
        Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), delayTicks * 50L,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /** Cancel every task owned by this plugin. */
    public void cancelAll() {
        for (ScheduledTask task : regionTasks) {
            task.cancel();
        }
        regionTasks.clear();
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }
}
