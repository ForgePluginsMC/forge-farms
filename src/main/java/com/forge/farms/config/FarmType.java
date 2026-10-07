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
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
    private final int agePerSample;
    private final boolean harvestSweep;
    private final int maxHarvestsPerTick;
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
    private final @Nullable Particle harvestParticle;
    private final int harvestParticleCount;
    private final @Nullable Sound harvestSound;
    private final float harvestSoundVolume;
    private final float harvestSoundPitch;
    private final @Nullable Particle growthParticle;
    private final int growthParticleCount;
    private final @Nullable Particle workingParticle;
    private final int workingParticleCount;
    private final @Nullable Particle radiusParticle;
    private final int radiusParticleCount;
    private final int radiusParticleInterval;
    private final boolean shopEnabled;
    private final Cost shopCost;

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
        this.agePerSample = b.agePerSample;
        this.harvestSweep = b.harvestSweep;
        this.maxHarvestsPerTick = b.maxHarvestsPerTick;
        this.harvestParticle = b.harvestParticle;
        this.harvestParticleCount = b.harvestParticleCount;
        this.harvestSound = b.harvestSound;
        this.harvestSoundVolume = b.harvestSoundVolume;
        this.harvestSoundPitch = b.harvestSoundPitch;
        this.growthParticle = b.growthParticle;
        this.growthParticleCount = b.growthParticleCount;
        this.workingParticle = b.workingParticle;
        this.workingParticleCount = b.workingParticleCount;
        this.radiusParticle = b.radiusParticle;
        this.radiusParticleCount = b.radiusParticleCount;
        this.radiusParticleInterval = b.radiusParticleInterval;
        this.shopEnabled = b.shopEnabled;
        this.shopCost = b.shopCost;
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

    /** Ages added each time a crop is sampled (default 1). */
    public int agePerSample() {
        return agePerSample;
    }

    /** When true, each tick sweeps the radius for harvest-ready blocks. */
    public boolean harvestSweep() {
        return harvestSweep;
    }

    /** Cap on harvests per tick (sampling + sweep). 0 = unlimited. */
    public int maxHarvestsPerTick() {
        return maxHarvestsPerTick;
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

    public @Nullable Particle harvestParticle() {
        return harvestParticle;
    }

    public int harvestParticleCount() {
        return harvestParticleCount;
    }

    public @Nullable Sound harvestSound() {
        return harvestSound;
    }

    public float harvestSoundVolume() {
        return harvestSoundVolume;
    }

    public float harvestSoundPitch() {
        return harvestSoundPitch;
    }

    public @Nullable Particle growthParticle() {
        return growthParticle;
    }

    public int growthParticleCount() {
        return growthParticleCount;
    }

    /** Ambient particle shown at the core while the farm is fueled and ticking. */
    public @Nullable Particle workingParticle() {
        return workingParticle;
    }

    public int workingParticleCount() {
        return workingParticleCount;
    }

    /** Particle drawn as a ring at the working radius. */
    public @Nullable Particle radiusParticle() {
        return radiusParticle;
    }

    public int radiusParticleCount() {
        return radiusParticleCount;
    }

    /** Farm ticks between radius ring draws. */
    public int radiusParticleInterval() {
        return radiusParticleInterval;
    }

    /** Whether this farm type appears in the /farm shop GUI. */
    public boolean shopEnabled() {
        return shopEnabled;
    }

    /** Purchase price in the shop (Cost.free() = no charge). */
    public Cost shopCost() {
        return shopCost;
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
        b.agePerSample = Math.max(1, c.getInt("farm.age-per-sample", 1));
        b.harvestSweep = c.getBoolean("farm.harvest-sweep", false);
        b.maxHarvestsPerTick = Math.max(0, c.getInt("farm.max-harvests-per-tick", 24));
        b.baseStorageSlots = Math.max(1, c.getInt("storage.base-slots", 27));
        b.harvestParticle = parseParticle(c.getString("effects.harvest-particle"), file);
        b.harvestParticleCount = Math.max(1, c.getInt("effects.harvest-particle-count", 8));
        b.harvestSound = parseSound(c.getString("effects.harvest-sound"), file);
        b.harvestSoundVolume = (float) c.getDouble("effects.harvest-sound-volume", 0.6);
        b.harvestSoundPitch = (float) c.getDouble("effects.harvest-sound-pitch", 1.1);
        b.growthParticle = parseParticle(c.getString("effects.growth-particle"), file);
        b.growthParticleCount = Math.max(1, c.getInt("effects.growth-particle-count", 4));
        b.workingParticle = parseParticle(c.getString("effects.working-particle"), file);
        b.workingParticleCount = Math.max(1, c.getInt("effects.working-particle-count", 6));
        b.radiusParticle = parseParticle(c.getString("effects.radius-particle"), file);
        b.radiusParticleCount = Math.max(8, c.getInt("effects.radius-particle-count", 48));
        b.radiusParticleInterval = Math.max(1, c.getInt("effects.radius-particle-interval", 4));
        ConfigurationSection shop = c.getConfigurationSection("shop");
        if (shop != null) {
            b.shopEnabled = shop.getBoolean("enabled", true);
            b.shopCost = Cost.parse(shop.getValues(false), "price-", file, "shop");
        } else {
            b.shopEnabled = true;
            b.shopCost = Cost.free();
        }
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
                    Cost cost = Cost.parse(entry, "cost-", file, "upgrades." + key + ".level" + n);
                    String desc = entry.get("description") == null ? "" : entry.get("description").toString();
                    levels.add(new UpgradeLevel(n, effect, cost, desc));
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

    private static @Nullable Particle parseParticle(@Nullable String name, File file) {
        if (name == null || name.isBlank() || name.equalsIgnoreCase("none")) {
            return null;
        }
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(file.getName() + ": unknown particle '" + name + "'");
        }
    }

    private static @Nullable Sound parseSound(@Nullable String name, File file) {
        if (name == null || name.isBlank() || name.equalsIgnoreCase("none")) {
            return null;
        }
        // Sound.valueOf is deprecated for removal in 26.3; the registry is
        // the supported path. Enum names map to keys: BLOCK_CROP_BREAK ->
        // minecraft:block.crop_break.
        Sound sound = org.bukkit.Registry.SOUNDS.get(
                NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT).replace('_', '.')));
        if (sound == null) {
            throw new IllegalArgumentException(file.getName() + ": unknown sound '" + name + "'");
        }
        return sound;
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
        int agePerSample = 1;
        boolean harvestSweep = false;
        int maxHarvestsPerTick = 24;
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
        @Nullable Particle harvestParticle = null;
        int harvestParticleCount = 8;
        @Nullable Sound harvestSound = null;
        float harvestSoundVolume = 0.6f;
        float harvestSoundPitch = 1.1f;
        @Nullable Particle growthParticle = null;
        int growthParticleCount = 4;
        @Nullable Particle workingParticle = null;
        int workingParticleCount = 6;
        @Nullable Particle radiusParticle = null;
        int radiusParticleCount = 48;
        int radiusParticleInterval = 4;
        boolean shopEnabled = true;
        Cost shopCost = Cost.free();
    }
}
