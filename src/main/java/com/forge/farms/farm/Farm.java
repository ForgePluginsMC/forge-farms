package com.forge.farms.farm;

import com.forge.farms.config.TrackType;
import com.forge.farms.output.OutputMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One placed farm: identity, location, upgrade levels, fuel, virtual
 * storage, and output wiring. Pure data — the managers drive behavior.
 * Instance-synchronized: ticks run on region threads, catch-up on async,
 * GUIs on the main thread.
 */
public final class Farm {
    private final UUID id;
    private final UUID owner;
    private final String typeId;
    private final UUID worldId;
    private final int x;
    private final int y;
    private final int z;

    private int radiusLevel;
    private int speedLevel;
    private int storageLevel;
    private int efficiencyLevel;
    private long fuelTicks;
    private ItemStack[] storage;
    private int storageSlots;
    private final List<OutputMode> outputPriority;
    private boolean autoSell;
    private boolean hologramEnabled;
    private long totalHarvested;
    private final long createdAt;
    private long lastTickAt;

    public Farm(UUID id, UUID owner, String typeId, UUID worldId,
            int x, int y, int z,
            int radiusLevel, int speedLevel, int storageLevel, int efficiencyLevel,
            long fuelTicks, List<OutputMode> outputPriority,
            boolean autoSell, boolean hologramEnabled,
            long totalHarvested, long createdAt, long lastTickAt) {
        this.id = id;
        this.owner = owner;
        this.typeId = typeId;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radiusLevel = radiusLevel;
        this.speedLevel = speedLevel;
        this.storageLevel = storageLevel;
        this.efficiencyLevel = efficiencyLevel;
        this.fuelTicks = Math.max(0L, fuelTicks);
        this.outputPriority = new ArrayList<>(outputPriority);
        this.autoSell = autoSell;
        this.hologramEnabled = hologramEnabled;
        this.totalHarvested = totalHarvested;
        this.createdAt = createdAt;
        this.lastTickAt = lastTickAt;
        this.storage = new ItemStack[0];
        this.storageSlots = 0;
    }

    /** New farm at a location, defaults for everything else. */
    public static Farm create(UUID owner, String typeId, Location core, boolean hologramDefault) {
        long now = System.currentTimeMillis();
        return new Farm(UUID.randomUUID(), owner, typeId, core.getWorld().getUID(),
                core.getBlockX(), core.getBlockY(), core.getBlockZ(),
                0, 0, 0, 0, 0L,
                new ArrayList<>(List.of(OutputMode.STORAGE, OutputMode.HOPPER, OutputMode.SELL)),
                true, hologramDefault, 0L, now, now);
    }

    public UUID id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public String typeId() {
        return typeId;
    }

    public UUID worldId() {
        return worldId;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public synchronized int radiusLevel() {
        return radiusLevel;
    }

    public synchronized int speedLevel() {
        return speedLevel;
    }

    public synchronized int storageLevel() {
        return storageLevel;
    }

    public synchronized int efficiencyLevel() {
        return efficiencyLevel;
    }

    public synchronized int getLevel(TrackType track) {
        return switch (track) {
            case RADIUS -> radiusLevel;
            case SPEED -> speedLevel;
            case STORAGE -> storageLevel;
            case EFFICIENCY -> efficiencyLevel;
        };
    }

    public synchronized void setLevel(TrackType track, int level) {
        switch (track) {
            case RADIUS -> radiusLevel = level;
            case SPEED -> speedLevel = level;
            case STORAGE -> storageLevel = level;
            case EFFICIENCY -> efficiencyLevel = level;
        }
    }

    public synchronized long fuelTicks() {
        return fuelTicks;
    }

    public synchronized void addFuel(long ticks) {
        fuelTicks = Math.min(Long.MAX_VALUE - 1, fuelTicks + Math.max(0L, ticks));
    }

    /**
     * Consume fuel ticks. Returns true when fuel covered the cost
     * (or the cost was zero); false when the tank hit empty.
     */
    public synchronized boolean consumeFuel(long ticks) {
        if (ticks <= 0) {
            return true;
        }
        if (fuelTicks <= 0) {
            return false;
        }
        fuelTicks = Math.max(0L, fuelTicks - ticks);
        return true;
    }

    /** Slots the virtual storage currently offers. Set by FarmManager from type + level. */
    public synchronized int storageSlots() {
        return storageSlots;
    }

    public synchronized void setStorageSlots(int slots) {
        this.storageSlots = Math.max(1, slots);
        resizeStorage(this.storageSlots);
    }

    /** Raw backing array; callers must not retain it. */
    public synchronized ItemStack[] storageSnapshot() {
        return storage.clone();
    }

    /** Used by the database load path before slots are known. */
    public synchronized void setRawStorage(ItemStack[] raw) {
        this.storage = raw.clone();
    }

    public synchronized void resizeStorage(int slots) {
        if (storage.length == slots) {
            return;
        }
        ItemStack[] next = new ItemStack[slots];
        System.arraycopy(storage, 0, next, 0, Math.min(storage.length, slots));
        storage = next;
    }

    public synchronized @Nullable ItemStack getSlot(int slot) {
        if (slot < 0 || slot >= storage.length) {
            return null;
        }
        ItemStack it = storage[slot];
        return it == null ? null : it.clone();
    }

    public synchronized void setSlot(int slot, @Nullable ItemStack item) {
        if (slot < 0 || slot >= storage.length) {
            return;
        }
        storage[slot] = (item == null || item.isEmpty()) ? null : item.clone();
    }

    /**
     * Add items to virtual storage. Returns whatever did not fit.
     * Mirrors inventory stacking rules (merge then fill empties).
     */
    public synchronized List<ItemStack> addItems(Collection<ItemStack> items) {
        List<ItemStack> leftover = new ArrayList<>();
        for (ItemStack in : items) {
            if (in == null || in.isEmpty()) {
                continue;
            }
            ItemStack stack = in.clone();
            int max = stack.getMaxStackSize();
            // Merge into partial stacks first.
            for (int i = 0; i < storage.length && stack.getAmount() > 0; i++) {
                ItemStack slot = storage[i];
                if (slot != null && !slot.isEmpty() && slot.isSimilar(stack) && slot.getAmount() < max) {
                    int room = max - slot.getAmount();
                    int move = Math.min(room, stack.getAmount());
                    slot.setAmount(slot.getAmount() + move);
                    stack.setAmount(stack.getAmount() - move);
                }
            }
            // Then empty slots.
            for (int i = 0; i < storage.length && stack.getAmount() > 0; i++) {
                if (storage[i] == null || storage[i].isEmpty()) {
                    int move = Math.min(max, stack.getAmount());
                    ItemStack placed = stack.clone();
                    placed.setAmount(move);
                    storage[i] = placed;
                    stack.setAmount(stack.getAmount() - move);
                }
            }
            if (stack.getAmount() > 0) {
                leftover.add(stack);
            }
        }
        return leftover;
    }

    public synchronized int usedSlots() {
        int n = 0;
        for (ItemStack it : storage) {
            if (it != null && !it.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public synchronized boolean isStorageFull() {
        for (ItemStack it : storage) {
            if (it == null || it.isEmpty()) {
                return false;
            }
        }
        return storage.length > 0;
    }

    public synchronized long totalItemCount() {
        long n = 0;
        for (ItemStack it : storage) {
            if (it != null && !it.isEmpty()) {
                n += it.getAmount();
            }
        }
        return n;
    }

    public synchronized List<OutputMode> outputPriority() {
        return new ArrayList<>(outputPriority);
    }

    public synchronized void setOutputPriority(List<OutputMode> priority) {
        outputPriority.clear();
        outputPriority.addAll(priority);
    }

    public synchronized boolean autoSell() {
        return autoSell;
    }

    public synchronized void setAutoSell(boolean autoSell) {
        this.autoSell = autoSell;
    }

    public synchronized boolean hologramEnabled() {
        return hologramEnabled;
    }

    public synchronized void setHologramEnabled(boolean enabled) {
        this.hologramEnabled = enabled;
    }

    public synchronized long totalHarvested() {
        return totalHarvested;
    }

    public synchronized void addHarvested(long n) {
        totalHarvested += n;
    }

    public long createdAt() {
        return createdAt;
    }

    public synchronized long lastTickAt() {
        return lastTickAt;
    }

    public synchronized void setLastTickAt(long at) {
        this.lastTickAt = at;
    }
}
