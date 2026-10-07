package com.forge.farms.db;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.ConfigManager;
import com.forge.farms.farm.Farm;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
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
            + "fuel_ticks BIGINT NOT NULL DEFAULT 0, storage MEDIUMTEXT, "
            + "output_priority VARCHAR(64) NOT NULL DEFAULT 'STORAGE,HOPPER,SELL', "
            + "auto_sell INT NOT NULL DEFAULT 1, hologram INT NOT NULL DEFAULT 1, "
            + "total_harvested BIGINT NOT NULL DEFAULT 0, "
            + "created_at BIGINT NOT NULL, last_tick BIGINT NOT NULL)";

    protected static final String DDL_MEMBERS = "CREATE TABLE IF NOT EXISTS forgefarms_members ("
            + "farm_id CHAR(36) NOT NULL, player CHAR(36) NOT NULL, role VARCHAR(16) NOT NULL, "
            + "PRIMARY KEY (farm_id, player))";

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
