package com.forge.farms.hologram;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.FarmType;
import com.forge.farms.core.Keys;
import com.forge.farms.core.Text;
import com.forge.farms.farm.Farm;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

/**
 * Native holograms on TextDisplay entities — no armor stands, no
 * HolographicDisplays dependency. One billboarded display floats above
 * each farm core and refreshes on a throttle.
 */
public final class HologramManager {
    /** farmId -> display entity uuid. */
    private final Map<UUID, UUID> holograms = new ConcurrentHashMap<>();
    private final ForgeFarms plugin;

    public HologramManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    /** (Re)spawn or update the hologram for a farm. Region-thread safe. */
    public void refresh(Farm farm) {
        if (!farm.hologramEnabled()) {
            remove(farm);
            return;
        }
        FarmType type = plugin.config().getType(farm.typeId());
        if (type == null || type.hologramLines().isEmpty()) {
            remove(farm);
            return;
        }
        Location core = plugin.farms().coreLocation(farm);
        if (core == null || core.getWorld() == null) {
            return;
        }
        Location at = core.clone().add(0, 2.4, 0);
        TextDisplay display = find(farm);
        if (display == null || !display.isValid()) {
            holograms.remove(farm.id());
            display = spawn(farm, at);
        }
        if (display == null) {
            return;
        }
        if (!display.getLocation().getWorld().equals(at.getWorld())
                || display.getLocation().distanceSquared(at) > 4) {
            display.teleport(at);
        }
        display.text(compose(farm, type));
    }

    public void remove(Farm farm) {
        UUID entityId = holograms.remove(farm.id());
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    public void removeAll() {
        for (UUID entityId : holograms.values()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        holograms.clear();
    }

    private @Nullable TextDisplay find(Farm farm) {
        UUID entityId = holograms.get(farm.id());
        if (entityId == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(entityId);
        return entity instanceof TextDisplay td ? td : null;
    }

    private @Nullable TextDisplay spawn(Farm farm, Location at) {
        World world = at.getWorld();
        if (world == null) {
            return null;
        }
        TextDisplay display = world.spawn(at, TextDisplay.class, td -> {
            td.setBillboard(Display.Billboard.CENTER);
            td.setViewRange(48f);
            td.setShadowed(true);
            td.setLineWidth(240);
            td.getPersistentDataContainer().set(Keys.HOLOGRAM_FARM, PersistentDataType.STRING,
                    farm.id().toString());
            td.setPersistent(true);
        });
        holograms.put(farm.id(), display.getUniqueId());
        return display;
    }

    private Component compose(Farm farm, FarmType type) {
        List<String> lines = type.hologramLines();
        Component out = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out = out.append(Component.newline());
            }
            out = out.append(Text.mm(applyPlaceholders(lines.get(i), farm, type)));
        }
        return out;
    }

    private String applyPlaceholders(String line, Farm farm, FarmType type) {
        String ownerName = Bukkit.getOfflinePlayer(farm.owner()).getName();
        long fuelMax = Math.max(1L, type.fuelTicksPerItem() * 64L);
        long fuelPct = Math.min(100L, farm.fuelTicks() * 100L / fuelMax);
        return line
                .replace("{owner}", ownerName == null ? "?" : ownerName)
                .replace("{type}", Text.plain(type.displayName()))
                .replace("{fuel}", formatDuration(farm.fuelTicks()))
                .replace("{fuel_percent}", String.valueOf(fuelPct))
                .replace("{storage_used}", String.valueOf(farm.totalItemCount()))
                .replace("{storage_slots}", String.valueOf(farm.usedSlots()) + "/" + farm.storageSlots())
                .replace("{radius}", String.valueOf(type.radiusAt(farm.radiusLevel())));
    }

    private String formatDuration(long ticks) {
        long seconds = ticks / 20L;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60L;
        if (minutes < 60) {
            return minutes + "m";
        }
        return (minutes / 60L) + "h " + (minutes % 60L) + "m";
    }
}
