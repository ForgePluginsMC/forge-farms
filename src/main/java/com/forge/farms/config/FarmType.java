package com.forge.farms.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable definition of one farm type, loaded from
 * {@code farm-types/<id>.yml}. All tuning lives here so new farm types
 * are data, not code.
 */
public final class FarmType {
    private final String id;
    private final String displayName;
    private final String description;
    private final Material coreBlock;
    private final Material itemMaterial;
    private final String itemName;
    private final List<String> itemLore;
    private final int baseRadius;
    private final long baseTickInterval;
    private final int growthAttempts;
    private final int baseStorageSlots;
    private final Set<Material> fuelItems;
    private final long fuelTicksPerItem;
    private final List<Harvestable> harvestables;
    private final boolean treeFarming;
    private final Set<Material> allowedSaplings;
    private final int maxOfflineHours;
    private final boolean hologramDefault;
    private final List<String> hologramLines;
    private final Map<TrackType, UpgradeTrack> upgrades;

    private FarmType(Builder b) {
        this.id = b.id;
        this.displayName = b.displayName;
        this.description = b.description;
        this.coreBlock = b.coreBlock;
        this.itemMaterial = b.itemMaterial;
        this.itemName = b.itemName;
        this.itemLore = Collections.unmodifiableList(new ArrayList<>(b.itemLore));
        this.baseRadius = b.baseRadius;
        this.baseTickInterval = b.baseTickInterval;
        this.growthAttempts = b.growthAttempts;
        this.baseStorageSlots = b.baseStorageSlots;
        this.fuelItems = Collections.unmodifiableSet(new HashSet<>(b.fuelItems));
        this.fuelTicksPerItem = b.fuelTicksPerItem;
        this.harvestables = Collections.unmodifiableList(new ArrayList<>(b.harvestables));
        this.treeFarming = b.treeFarming;
        this.allowedSaplings = Collections.unmodifiableSet(new HashSet<>(b.allowedSaplings));
        this.maxOfflineHours = b.maxOfflineHours;
        this.hologramDefault = b.hologramDefault;
        this.hologramLines = Collections.unmodifiableList(new ArrayList<>(b.hologramLines));
        this.upgrades = Collections.unmodifiableMap(new EnumMap<>(b.upgrades));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Material coreBlock() {
        return coreBlock;
    }

    public Material itemMaterial() {
        return itemMaterial;
    }

    public String itemName() {
        return itemName;
    }

    public List<String> itemLore() {
        return itemLore;
    }

    public int baseRadius() {
        return baseRadius;
    }

    public long baseTickInterval() {
        return baseTickInterval;
    }

    public int growthAttempts() {
        return growthAttempts;
    }

    public int baseStorageSlots() {
        return baseStorageSlots;
    }

    public Set<Material> fuelItems() {
        return fuelItems;
    }

    public long fuelTicksPerItem() {
        return fuelTicksPerItem;
    }

    public List<Harvestable> harvestables() {
        return harvestables;
    }

    public boolean treeFarming() {
        return treeFarming;
    }

    public Set<Material> allowedSaplings() {
        return allowedSaplings;
    }

    public int maxOfflineHours() {
        return maxOfflineHours;
    }

    public boolean hologramDefault() {
        return hologramDefault;
    }

    public List<String> hologramLines() {
        return hologramLines;
    }

    public @Nullable UpgradeTrack upgradeTrack(TrackType type) {
        return upgrades.get(type);
    }

    /** Radius in blocks at the given radius-track level (0 = base). */
    public int radiusAt(int radiusLevel) {
        UpgradeTrack track = upgrades.get(TrackType.RADIUS);
        if (track != null) {
            UpgradeLevel lvl = track.level(radiusLevel);
            if (lvl != null) {
                return (int) lvl.effect();
            }
        }
        return baseRadius;
    }

    /** Growth tick interval at the given speed-track level (0 = base). */
    public long intervalAt(int speedLevel) {
        UpgradeTrack track = upgrades.get(TrackType.SPEED);
        if (track != null) {
            UpgradeLevel lvl = track.level(speedLevel);
            if (lvl != null) {
                return Math.max(1L, (long) lvl.effect());
            }
        }
        return baseTickInterval;
    }

    /** Virtual storage slots at the given storage-track level (0 = base). */
    public int slotsAt(int storageLevel) {
        UpgradeTrack track = upgrades.get(TrackType.STORAGE);
        if (track != null) {
            UpgradeLevel lvl = track.level(storageLevel);
            if (lvl != null) {
                return Math.max(1, (int) lvl.effect());
            }
        }
        return baseStorageSlots;
    }

    /** Fuel multiplier at the given efficiency-track level (0 = 1.0). */
    public double efficiencyAt(int efficiencyLevel) {
        UpgradeTrack track = upgrades.get(TrackType.EFFICIENCY);
        if (track != null) {
            UpgradeLevel lvl = track.level(efficiencyLevel);
            if (lvl != null) {
                return Math.max(0.1, lvl.effect());
            }
        }
        return 1.0;
    }

    /** Parse a farm type file. Throws IllegalArgumentException on bad data. */
    public static FarmType load(File file) {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);
        Builder b = new Builder();
        b.id = c.getString("id", stripExt(file.getName())).toLowerCase(Locale.ROOT);
        b.displayName = c.getString("display-name", b.id);
        b.description = c.getString("description", "");
        b.coreBlock = material(c.getString("core-block", "COMPOSTER"), file, "core-block");
        b.itemMaterial = material(c.getString("item.material", "WHEAT_SEEDS"), file, "item.material");
        b.itemName = c.getString("item.name", b.displayName);
        b.itemLore = c.getStringList("item.lore");
        b.baseRadius = Math.max(1, c.getInt("farm.base-radius", 4));
        b.baseTickInterval = Math.max(1L, c.getLong("farm.tick-interval", 100L));
        b.growthAttempts = Math.max(1, c.getInt("farm.growth-attempts", 10));
        b.baseStorageSlots = Math.max(1, c.getInt("storage.base-slots", 27));
        for (String s : c.getStringList("fuel.items")) {
            b.fuelItems.add(material(s, file, "fuel.items"));
        }
        b.fuelTicksPerItem = Math.max(1L, c.getLong("fuel.ticks-per-item", 1200L));
        ConfigurationSection harvest = c.getConfigurationSection("harvest");
        if (harvest != null) {
            for (String key : harvest.getKeys(false)) {
                ConfigurationSection h = harvest.getConfigurationSection(key);
                if (h == null) {
                    continue;
                }
                Material m = material(h.getString("material", key), file, "harvest." + key);
                String beh = h.getString("behavior", "AGEABLE_CROP").toUpperCase(Locale.ROOT);
                HarvestBehavior behavior;
                try {
                    behavior = HarvestBehavior.valueOf(beh);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(file.getName() + ": unknown behavior '" + beh + "'");
                }
                b.harvestables.add(new Harvestable(m, behavior));
            }
        }
        b.treeFarming = c.getBoolean("trees.enabled", false);
        for (String s : c.getStringList("trees.allowed-saplings")) {
            b.allowedSaplings.add(material(s, file, "trees.allowed-saplings"));
        }
        b.maxOfflineHours = Math.max(0, c.getInt("offline.max-hours", 12));
        b.hologramDefault = c.getBoolean("hologram.enabled", true);
        b.hologramLines = c.getStringList("hologram.lines");
        ConfigurationSection up = c.getConfigurationSection("upgrades");
        if (up != null) {
            for (String key : up.getKeys(false)) {
                TrackType type = TrackType.fromKey(key);
                if (type == null) {
                    throw new IllegalArgumentException(file.getName() + ": unknown upgrade track '" + key + "'");
                }
                ConfigurationSection t = up.getConfigurationSection(key);
                if (t == null) {
                    continue;
                }
                List<UpgradeLevel> levels = new ArrayList<>();
                List<Map<?, ?>> raw = t.getMapList("levels");
                int n = 1;
                for (Map<?, ?> entry : raw) {
                    double effect = toDouble(entry.get("effect"));
                    double costMoney = toDouble(entry.get("cost-money"));
                    Material costItem = null;
                    int costAmount = 0;
                    Object ci = entry.get("cost-item");
                    if (ci instanceof Map<?, ?> cm) {
                        Object mat = cm.get("material");
                        if (mat != null) {
                            costItem = material(mat.toString(), file, "upgrades." + key + ".cost-item");
                        }
                        costAmount = toInt(cm.get("amount"));
                    }
                    int costXp = toInt(entry.get("cost-xp-levels"));
                    String desc = entry.get("description") == null ? "" : entry.get("description").toString();
                    levels.add(new UpgradeLevel(n, effect, costMoney, costItem, costAmount, costXp, desc));
                    n++;
                }
                b.upgrades.put(type, new UpgradeTrack(type, levels));
            }
        }
        return new FarmType(b);
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }

    private static Material material(String name, File file, String path) {
        Material m = Material.matchMaterial(name);
        if (m == null) {
            throw new IllegalArgumentException(file.getName() + ": unknown material '" + name + "' at " + path);
        }
        return m;
    }

    private static double toDouble(@Nullable Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return 0;
    }

    private static int toInt(@Nullable Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private static final class Builder {
        String id = "";
        String displayName = "";
        String description = "";
        Material coreBlock = Material.COMPOSTER;
        Material itemMaterial = Material.WHEAT_SEEDS;
        String itemName = "";
        List<String> itemLore = new ArrayList<>();
        int baseRadius = 4;
        long baseTickInterval = 100L;
        int growthAttempts = 10;
        int baseStorageSlots = 27;
        Set<Material> fuelItems = new HashSet<>();
        long fuelTicksPerItem = 1200L;
        List<Harvestable> harvestables = new ArrayList<>();
        boolean treeFarming = false;
        Set<Material> allowedSaplings = new HashSet<>();
        int maxOfflineHours = 12;
        boolean hologramDefault = true;
        List<String> hologramLines = new ArrayList<>();
        Map<TrackType, UpgradeTrack> upgrades = new EnumMap<>(TrackType.class);
    }
}
