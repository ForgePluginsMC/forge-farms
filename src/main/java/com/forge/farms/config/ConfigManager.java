package com.forge.farms.config;

import com.forge.farms.ForgeFarms;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * Loads {@code config.yml} plus every {@code farm-types/*.yml} into
 * {@link FarmType} definitions. Ships sane defaults on first run;
 * {@code /farmadmin reload} re-reads everything.
 */
public final class ConfigManager {
    private static final List<String> DEFAULT_TYPES = List.of("wheat.yml", "oak.yml", "sugarcane.yml", "melon.yml");

    private final ForgeFarms plugin;
    private final Map<String, FarmType> types = new LinkedHashMap<>();
    private final Map<String, File> typeFiles = new LinkedHashMap<>();

    private String dbType = "sqlite";
    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "forgefarms";
    private String mysqlUser = "forgefarms";
    private String mysqlPassword = "change-me";
    private boolean mysqlUseSsl = false;

    private int maxFarmsPerPlayer = 3;
    private int maxMembersPerFarm = 10;
    private long hologramRefreshTicks = 100L;
    private final Map<Material, Double> sellPrices = new EnumMap<>(Material.class);

    public ConfigManager(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        dbType = c.getString("database.type", "sqlite").toLowerCase(Locale.ROOT);
        mysqlHost = c.getString("database.mysql.host", "localhost");
        mysqlPort = c.getInt("database.mysql.port", 3306);
        mysqlDatabase = c.getString("database.mysql.database", "forgefarms");
        mysqlUser = c.getString("database.mysql.user", "forgefarms");
        mysqlPassword = c.getString("database.mysql.password", "change-me");
        mysqlUseSsl = c.getBoolean("database.mysql.use-ssl", false);

        maxFarmsPerPlayer = Math.max(1, c.getInt("limits.max-farms-per-player", 3));
        maxMembersPerFarm = Math.max(1, c.getInt("limits.max-members-per-farm", 10));
        hologramRefreshTicks = Math.max(20L, c.getLong("hologram.refresh-ticks", 100L));

        sellPrices.clear();
        ConfigurationSection prices = c.getConfigurationSection("prices");
        if (prices != null) {
            for (String key : prices.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m == null) {
                    plugin.getLogger().warning("config.yml: unknown material in prices: " + key);
                    continue;
                }
                sellPrices.put(m, prices.getDouble(key));
            }
        }

        File dir = new File(plugin.getDataFolder(), "farm-types");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create farm-types directory.");
        }
        for (String def : DEFAULT_TYPES) {
            File f = new File(dir, def);
            if (!f.exists()) {
                plugin.saveResource("farm-types/" + def, false);
            }
        }
        types.clear();
        typeFiles.clear();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                try {
                    FarmType type = FarmType.load(f);
                    types.put(type.id(), type);
                    typeFiles.put(type.id(), f);
                    plugin.getLogger().info("Loaded farm type '" + type.id() + "'.");
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().log(Level.WARNING, "Skipping farm type " + f.getName() + ": " + e.getMessage());
                }
            }
        }
        if (types.isEmpty()) {
            plugin.getLogger().severe("No farm types loaded — /farm get will have nothing to give.");
        }
    }

    public void reload() {
        load();
    }

    /**
     * Re-read a single farm type from its yml file (used by the admin
     * tuning GUI after it rewrites values). Returns false when the file
     * is missing or fails to parse; the old definition stays live.
     */
    public boolean reloadType(String id) {
        if (id == null) {
            return false;
        }
        String key = id.toLowerCase(Locale.ROOT);
        File f = typeFiles.get(key);
        if (f == null || !f.isFile()) {
            return false;
        }
        try {
            FarmType type = FarmType.load(f);
            if (!type.id().equals(key)) {
                types.remove(key);
                typeFiles.remove(key);
            }
            types.put(type.id(), type);
            typeFiles.put(type.id(), f);
            plugin.getLogger().info("Reloaded farm type '" + type.id() + "'.");
            return true;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to reload farm type " + f.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /** The yml file a farm type was loaded from, or null. */
    public @Nullable File typeFile(String id) {
        if (id == null) {
            return null;
        }
        return typeFiles.get(id.toLowerCase(Locale.ROOT));
    }

    public @Nullable FarmType getType(String id) {
        if (id == null) {
            return null;
        }
        return types.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<FarmType> getTypes() {
        return List.copyOf(types.values());
    }

    public List<String> typeIds() {
        return new ArrayList<>(types.keySet());
    }

    public String dbType() {
        return dbType;
    }

    public String mysqlHost() {
        return mysqlHost;
    }

    public int mysqlPort() {
        return mysqlPort;
    }

    public String mysqlDatabase() {
        return mysqlDatabase;
    }

    public String mysqlUser() {
        return mysqlUser;
    }

    public String mysqlPassword() {
        return mysqlPassword;
    }

    public boolean mysqlUseSsl() {
        return mysqlUseSsl;
    }

    public int maxFarmsPerPlayer() {
        return maxFarmsPerPlayer;
    }

    public int maxMembersPerFarm() {
        return maxMembersPerFarm;
    }

    public long hologramRefreshTicks() {
        return hologramRefreshTicks;
    }

    /** Sell price per item, or null when the material has no price. */
    public @Nullable Double sellPrice(Material material) {
        return sellPrices.get(material);
    }

    /** All sell prices as Bukkit names, for the admin GUI. */
    public java.util.Map<String, Double> sellPrices() {
        java.util.Map<String, Double> out = new java.util.TreeMap<>();
        for (java.util.Map.Entry<Material, Double> e : sellPrices.entrySet()) {
            out.put(e.getKey().name(), e.getValue());
        }
        return out;
    }
}
