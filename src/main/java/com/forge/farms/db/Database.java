package com.forge.farms.db;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.ConfigManager;
import com.forge.farms.farm.Farm;
import com.forge.farms.output.OutputMode;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Persistence abstraction. SQLite is the default and works out of the box
 * (driver bundled); MySQL is available for networks. {@code /farmadmin
 * migrate} copies everything between backends.
 */
public abstract class Database {
    protected final ForgeFarms plugin;
    protected final Object lock = new Object();
    private Connection connection;

    protected Database(ForgeFarms plugin) {
        this.plugin = plugin;
    }

    public static Database open(ForgeFarms plugin, ConfigManager config) {
        if ("mysql".equalsIgnoreCase(config.dbType())) {
            return new MySQLDatabase(plugin, config);
        }
        return new SQLiteDatabase(plugin, config);
    }

    /** Create tables if missing. */
    public abstract void migrate();

    /**
     * Add a column to an existing table if it is missing. Neither SQLite nor
     * MySQL 8 support {@code ADD COLUMN IF NOT EXISTS} portably, so a failed
     * ALTER is treated as "already there".
     */
    protected static void ensureColumn(java.sql.Statement s, String table, String column, String type) {
        try {
            s.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (java.sql.SQLException ignored) {
            // Column already exists.
        }
    }

    public abstract List<Farm> loadFarms();

    public abstract void saveFarm(Farm farm);

    public abstract void deleteFarm(UUID farmId);

    /** farmId -> (playerId -> role name). */
    public abstract Map<UUID, Map<UUID, String>> loadMembers();

    public abstract void saveMembers(UUID farmId, Map<UUID, String> roles);

    /** Delete every row (used by restore). */
    public abstract void clearAll();

    public abstract void backup(File dest) throws IOException;

    public abstract void restore(File src) throws IOException;

    public abstract String backendName();

    public void close() {
        synchronized (lock) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    plugin.getLogger().warning("Error closing database: " + e.getMessage());
                }
                connection = null;
            }
        }
    }

    protected Connection connection() throws SQLException {
        synchronized (lock) {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                close();
                connection = newConnection();
            }
            return connection;
        }
    }

    protected abstract Connection newConnection() throws SQLException;

    protected static final String DDL_FARMS = "CREATE TABLE IF NOT EXISTS forgefarms_farms ("
            + "id CHAR(36) PRIMARY KEY, owner CHAR(36) NOT NULL, type VARCHAR(64) NOT NULL, "
            + "world CHAR(36) NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, "
            + "radius_level INT NOT NULL DEFAULT 0, speed_level INT NOT NULL DEFAULT 0, "
            + "storage_level INT NOT NULL DEFAULT 0, efficiency_level INT NOT NULL DEFAULT 0, "
            + "tilling_level INT NOT NULL DEFAULT 0, "
            + "fuel_ticks BIGINT NOT NULL DEFAULT 0, storage MEDIUMTEXT, "
            + "output_priority VARCHAR(64) NOT NULL DEFAULT 'STORAGE,HOPPER,SELL', "
            + "auto_sell INT NOT NULL DEFAULT 1, hologram INT NOT NULL DEFAULT 1, "
            + "total_harvested BIGINT NOT NULL DEFAULT 0, "
            + "created_at BIGINT NOT NULL, last_tick BIGINT NOT NULL, "
            + "settings MEDIUMTEXT)";

    protected static final String DDL_MEMBERS = "CREATE TABLE IF NOT EXISTS forgefarms_members ("
            + "farm_id CHAR(36) NOT NULL, player CHAR(36) NOT NULL, role VARCHAR(16) NOT NULL, "
            + "PRIMARY KEY (farm_id, player))";

    /**
     * Column order shared by the upserts and the bind/read helpers below.
     * This is the single source of truth — adding a column here without
     * updating DDL_FARMS (and vice versa) is a boot-time bug, so keep them
     * in lockstep.
     */
    protected static final String[] FARM_COLS = {
        "id", "owner", "type", "world", "x", "y", "z",
        "radius_level", "speed_level", "storage_level", "efficiency_level", "tilling_level",
        "fuel_ticks", "storage", "output_priority", "auto_sell", "hologram",
        "total_harvested", "created_at", "last_tick"
        // Note: a "settings" column may exist from the short-lived per-farm
        // tuning experiment (v1.0.0, Oct 6 2026). It is intentionally not
        // referenced here; tuning is now per farm TYPE via the admin GUI.
    };

    protected static String farmColumnList() {
        return String.join(",", FARM_COLS);
    }

    protected static String farmPlaceholders() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < FARM_COLS.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        return sb.toString();
    }

    /** Bind all farm fields in FARM_COLS order. */
    protected static void bindFarm(PreparedStatement ps, Farm farm) throws SQLException {
        ps.setString(1, farm.id().toString());
        ps.setString(2, farm.owner().toString());
        ps.setString(3, farm.typeId());
        ps.setString(4, farm.worldId().toString());
        ps.setInt(5, farm.x());
        ps.setInt(6, farm.y());
        ps.setInt(7, farm.z());
        ps.setInt(8, farm.radiusLevel());
        ps.setInt(9, farm.speedLevel());
        ps.setInt(10, farm.storageLevel());
        ps.setInt(11, farm.efficiencyLevel());
        ps.setInt(12, farm.tillingLevel());
        ps.setLong(13, farm.fuelTicks());
        ps.setString(14, encodeStorage(farm.storageSnapshot()));
        ps.setString(15, joinPriority(farm.outputPriority()));
        ps.setInt(16, farm.autoSell() ? 1 : 0);
        ps.setInt(17, farm.hologramEnabled() ? 1 : 0);
        ps.setLong(18, farm.totalHarvested());
        ps.setLong(19, farm.createdAt());
        ps.setLong(20, farm.lastTickAt());
    }

    /** Rebuild a farm from a row selected in FARM_COLS order (or by name). */
    protected static Farm readFarm(ResultSet rs) throws SQLException {
        List<OutputMode> priority = new ArrayList<>();
        String raw = rs.getString("output_priority");
        if (raw != null) {
            for (String part : raw.split(",")) {
                try {
                    priority.add(OutputMode.valueOf(part.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // Unknown mode from a newer version — skip.
                }
            }
        }
        if (priority.isEmpty()) {
            priority.add(OutputMode.STORAGE);
        }
        // Raw storage is loaded at the legacy 54-slot width; FarmManager
        // resizes to the type's real slot count right after load.
        int rawSlots = 54;
        Farm farm = new Farm(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("owner")),
                rs.getString("type"),
                UUID.fromString(rs.getString("world")),
                rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                rs.getInt("radius_level"), rs.getInt("speed_level"),
                rs.getInt("storage_level"), rs.getInt("efficiency_level"),
                rs.getInt("tilling_level"),
                rs.getLong("fuel_ticks"), priority,
                rs.getInt("auto_sell") != 0, rs.getInt("hologram") != 0,
                rs.getLong("total_harvested"), rs.getLong("created_at"), rs.getLong("last_tick"));
        farm.setRawStorage(decodeStorage(rs.getString("storage"), rawSlots));
        return farm;
    }

    private static String joinPriority(List<OutputMode> priority) {
        StringBuilder sb = new StringBuilder();
        for (OutputMode m : priority) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(m.name());
        }
        return sb.toString();
    }

    /** Virtual storage -> Base64. Null/empty slots round-trip as null. */
    public static String encodeStorage(ItemStack[] items) {
        ItemStack[] norm = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemStack it = items[i];
            norm[i] = (it == null || it.isEmpty()) ? new ItemStack(Material.AIR) : it;
        }
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(norm));
    }

    public static ItemStack[] decodeStorage(String base64, int slots) {
        ItemStack[] out = new ItemStack[Math.max(1, slots)];
        if (base64 == null || base64.isEmpty()) {
            return out;
        }
        try {
            ItemStack[] in = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(base64));
            for (int i = 0; i < Math.min(in.length, out.length); i++) {
                ItemStack it = in[i];
                out[i] = (it == null || it.isEmpty()) ? null : it;
            }
        } catch (IllegalArgumentException e) {
            // Corrupt payload — start empty rather than crash the load.
        }
        return out;
    }
}
