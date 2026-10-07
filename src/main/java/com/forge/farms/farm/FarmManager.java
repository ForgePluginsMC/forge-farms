package com.forge.farms.farm;

import com.forge.farms.ForgeFarms;
import com.forge.farms.api.event.FarmCreateEvent;
import com.forge.farms.api.event.FarmRemoveEvent;
import com.forge.farms.config.ConfigManager;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Text;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Registry of all farms: in-memory index, persistence, per-farm region
 * tick tasks, and offline catch-up. Tick tasks are bound to the farm
 * core's region, so Paper and Folia share one code path.
 */
public final class FarmManager {
    private final ForgeFarms plugin;
    private final Map<UUID, Farm> farms = new ConcurrentHashMap<>();
    private final Map<CoreKey, UUID> coreIndex = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public FarmManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** Load from the database, resolve types, index cores, start ticks. */
    public void loadAll() {
        ConfigManager config = plugin.config();
        int skipped = 0;
        for (Farm farm : plugin.database().loadFarms()) {
            FarmType type = config.getType(farm.typeId());
            if (type == null) {
                plugin.getLogger().warning("Skipping farm " + farm.id() + ": unknown type '" + farm.typeId() + "'.");
                skipped++;
                continue;
            }
            farm.setStorageSlots(type.slotsAt(farm.storageLevel()));
            farms.put(farm.id(), farm);
            coreIndex.put(CoreKey.of(farm), farm.id());
            startTick(farm);
        }
        // Members are loaded by TrustManager separately (same DB).
        plugin.trust().loadAll();
        if (skipped > 0) {
            plugin.getLogger().warning("Skipped " + skipped + " farms with unknown types.");
        }
    }

    /** Re-read everything from the database (used after /farmadmin restore). */
    public void reloadFromDatabase() {
        for (ScheduledTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        farms.clear();
        coreIndex.clear();
        plugin.trust().clear();
        loadAll();
    }

    public void saveAll() {
        for (Farm farm : farms.values()) {
            plugin.database().saveFarm(farm);
        }
        plugin.trust().saveAll();
    }

    /**
     * Re-apply a farm type's derived stats to its live farms (used after the
     * admin editor changes a type). Storage slots follow the new config.
     */
    public void refreshTypeStats(String typeId) {
        FarmType type = plugin.config().getType(typeId);
        if (type == null) {
            return;
        }
        for (Farm farm : farms.values()) {
            if (farm.typeId().equalsIgnoreCase(typeId)) {
                farm.setStorageSlots(type.slotsAt(farm.storageLevel()));
            }
        }
    }

    public void saveFarm(Farm farm) {
        plugin.database().saveFarm(farm);
    }

    /**
     * Create a farm at a core block. Fires FarmCreateEvent (cancellable).
     * Returns null when the event is cancelled.
     */
    public @Nullable Farm createFarm(Player owner, FarmType type, Location core) {
        Farm farm = Farm.create(owner.getUniqueId(), type.id(), core, type.hologramDefault());
        farm.setStorageSlots(type.slotsAt(0));
        FarmCreateEvent event = new FarmCreateEvent(farm, owner);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }
        farms.put(farm.id(), farm);
        coreIndex.put(CoreKey.of(farm), farm.id());
        plugin.database().saveFarm(farm);
        startTick(farm);
        plugin.holograms().refresh(farm);
        return farm;
    }

    /**
     * Remove a farm entirely: cancel tick, drop hologram, delete rows.
     * Returns false when the farm did not exist or removal was cancelled.
     */
    public boolean removeFarm(UUID farmId) {
        Farm farm = farms.get(farmId);
        if (farm == null) {
            return false;
        }
        FarmRemoveEvent event = new FarmRemoveEvent(farm);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        ScheduledTask task = tasks.remove(farmId);
        if (task != null) {
            task.cancel();
        }
        plugin.holograms().remove(farm);
        farms.remove(farmId);
        coreIndex.remove(CoreKey.of(farm));
        plugin.database().deleteFarm(farmId);
        plugin.trust().removeFarm(farmId);
        return true;
    }

    public @Nullable Farm getFarm(UUID id) {
        return farms.get(id);
    }

    public @Nullable Farm getByCore(Location loc) {
        World w = loc.getWorld();
        if (w == null) {
            return null;
        }
        UUID id = coreIndex.get(new CoreKey(w.getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        return id == null ? null : farms.get(id);
    }

    public @Nullable Farm getByCore(Block block) {
        return getByCore(block.getLocation());
    }

    public List<Farm> getByOwner(UUID owner) {
        List<Farm> out = new ArrayList<>();
        for (Farm farm : farms.values()) {
            if (farm.owner().equals(owner)) {
                out.add(farm);
            }
        }
        return out;
    }

    public int countByOwner(UUID owner) {
        int n = 0;
        for (Farm farm : farms.values()) {
            if (farm.owner().equals(owner)) {
                n++;
            }
        }
        return n;
    }

    public int count() {
        return farms.size();
    }

    /** Every loaded farm. */
    public List<Farm> all() {
        return new ArrayList<>(farms.values());
    }

    /** Farm whose radius contains the location, or null. */
    public @Nullable Farm farmContaining(Location loc) {
        World w = loc.getWorld();
        if (w == null) {
            return null;
        }
        for (Farm farm : farms.values()) {
            if (!farm.worldId().equals(w.getUID())) {
                continue;
            }
            FarmType type = plugin.config().getType(farm.typeId());
            if (type == null) {
                continue;
            }
            int r = type.radiusAt(farm.radiusLevel());
            int dx = loc.getBlockX() - farm.x();
            int dz = loc.getBlockZ() - farm.z();
            if (dx * dx + dz * dz <= r * r && Math.abs(loc.getBlockY() - farm.y()) <= 12) {
                return farm;
            }
        }
        return null;
    }

    public @Nullable Location coreLocation(Farm farm) {
        World world = Bukkit.getWorld(farm.worldId());
        if (world == null) {
            return null;
        }
        return new Location(world, farm.x() + 0.5, farm.y(), farm.z() + 0.5);
    }

    /** Ensure every loaded farm has a live region tick task. Runs each second globally. */
    public void tickLoaded() {
        for (Farm farm : farms.values()) {
            ScheduledTask task = tasks.get(farm.id());
            if (task == null || task.isCancelled()) {
                startTick(farm);
            }
        }
    }

    private void startTick(Farm farm) {
        Location core = coreLocation(farm);
        if (core == null) {
            return;
        }
        FarmType type = plugin.config().getType(farm.typeId());
        if (type == null) {
            return;
        }
        long interval = type.intervalAt(farm.speedLevel());
        try {
            ScheduledTask task = plugin.scheduler().regionAtFixedRate(core,
                    t -> plugin.growth().tickFarm(farm), interval, interval);
            ScheduledTask old = tasks.put(farm.id(), task);
            if (old != null) {
                old.cancel();
            }
        } catch (IllegalArgumentException e) {
            // Region not ready yet; tickLoaded() retries next second.
            plugin.getLogger().log(Level.FINE, "Deferring tick for farm " + farm.id(), e);
        }
    }

    /** Restart a farm's tick (used after speed upgrades). */
    public void restartTick(Farm farm) {
        ScheduledTask old = tasks.remove(farm.id());
        if (old != null) {
            old.cancel();
        }
        startTick(farm);
    }

    /**
     * Simulated growth while the owner was offline: converts elapsed time
     * into harvests at a conservative rate instead of pausing the farm.
     * Pure data math — safe to call from any thread.
     */
    public void applyOfflineCatchup(Player player) {
        ConfigManager config = plugin.config();
        List<Farm> owned = getByOwner(player.getUniqueId());
        if (owned.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Farm farm : owned) {
            FarmType type = config.getType(farm.typeId());
            if (type == null) {
                continue;
            }
            long elapsedMs;
            synchronized (farm) {
                elapsedMs = now - farm.lastTickAt();
            }
            long maxMs = (long) type.maxOfflineHours() * 3_600_000L;
            if (maxMs <= 0 || elapsedMs < 60_000L) {
                continue;
            }
            long simMs = Math.min(elapsedMs, maxMs);
            // Conservative: one growth tick's worth of attempts per interval,
            // at 25% efficiency, only if the farm had fuel banked.
            long interval = type.intervalAt(farm.speedLevel());
            long simTicks = simMs / (interval * 50L);
            if (simTicks <= 0) {
                continue;
            }
            long fuelCost = simTicks * interval;
            double efficiency = type.efficiencyAt(farm.efficiencyLevel());
            long discounted = (long) (fuelCost / efficiency);
            boolean hadFuel;
            synchronized (farm) {
                hadFuel = farm.fuelTicks() >= discounted;
                if (hadFuel) {
                    farm.consumeFuel(discounted);
                }
            }
            if (!hadFuel) {
                continue;
            }
            // Estimated yield: attempts * harvest-chance * avg drops, quartered.
            long estItems = simTicks * type.growthAttempts() / 4;
            if (estItems <= 0) {
                continue;
            }
            List<org.bukkit.inventory.ItemStack> yield = plugin.growth().estimateOfflineYield(type, estItems);
            List<org.bukkit.inventory.ItemStack> leftover;
            synchronized (farm) {
                leftover = farm.addItems(yield);
                farm.addHarvested(yield.size() - leftover.size());
            }
            int kept = yield.size() - leftover.size();
            if (kept > 0) {
                final int keptFinal = kept;
                final String typeName = type.displayName();
                Location msgLoc = coreLocation(farm);
                plugin.scheduler().region(msgLoc == null ? player.getLocation() : msgLoc,
                        () -> player.sendMessage(Text.mm(
                                "<green>While you were away, your " + typeName
                                        + " <green>produced <white>" + keptFinal
                                        + "</white> items (offline catch-up).</green>")));
            }
        }
    }

    /** Block coords + world identity for the core index. */
    public record CoreKey(UUID world, int x, int y, int z) {
        static CoreKey of(Farm farm) {
            return new CoreKey(farm.worldId(), farm.x(), farm.y(), farm.z());
        }
    }
}
