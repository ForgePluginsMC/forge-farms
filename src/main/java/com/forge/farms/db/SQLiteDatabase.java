package com.forge.farms.db;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.ConfigManager;
import com.forge.farms.farm.Farm;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Default backend. Single file, zero setup, driver bundled in the jar. */
public final class SQLiteDatabase extends Database {
    private final File file;

    public SQLiteDatabase(ForgeFarms plugin, ConfigManager config) {
        super(plugin);
        this.file = new File(plugin.getDataFolder(), "data.db");
    }

    @Override
    public String backendName() {
        return "sqlite";
    }

    @Override
    protected Connection newConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver missing from jar", e);
        }
        if (!plugin.getDataFolder().isDirectory()) {
            plugin.getDataFolder().mkdirs();
        }
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=NORMAL");
        }
        return c;
    }

    @Override
    public void migrate() {
        synchronized (lock) {
            try {
                try (Statement s = connection().createStatement()) {
                    s.execute(DDL_FARMS);
                    s.execute(DDL_MEMBERS);
                }
                // v2: per-farm tuning knobs
                try (Statement s = connection().createStatement()) {
                    s.execute("ALTER TABLE forgefarms_farms ADD COLUMN settings MEDIUMTEXT");
                } catch (SQLException e) {
                    if (e.getMessage() == null
                            || !e.getMessage().toLowerCase(java.util.Locale.ROOT).contains("duplicate")) {
                        throw e;
                    }
                    // Column already exists — nothing to do.
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to migrate SQLite schema", e);
            }
        }
    }

    @Override
    public List<Farm> loadFarms() {
        List<Farm> out = new ArrayList<>();
        synchronized (lock) {
            try (Statement s = connection().createStatement();
                    ResultSet rs = s.executeQuery("SELECT * FROM forgefarms_farms")) {
                while (rs.next()) {
                    out.add(Database.readFarm(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load farms", e);
            }
        }
        return out;
    }

    @Override
    public void saveFarm(Farm farm) {
        synchronized (lock) {
            StringBuilder sql = new StringBuilder("INSERT INTO forgefarms_farms (")
                    .append(farmColumnList()).append(") VALUES (").append(farmPlaceholders()).append(")")
                    .append(" ON CONFLICT(id) DO UPDATE SET ");
            for (int i = 1; i < FARM_COLS.length; i++) {
                if (i > 1) {
                    sql.append(',');
                }
                sql.append(FARM_COLS[i]).append("=excluded.").append(FARM_COLS[i]);
            }
            try (PreparedStatement ps = connection().prepareStatement(sql.toString())) {
                bindFarm(ps, farm);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save farm " + farm.id(), e);
            }
        }
    }

    @Override
    public void deleteFarm(UUID farmId) {
        synchronized (lock) {
            try (PreparedStatement ps = connection().prepareStatement("DELETE FROM forgefarms_farms WHERE id=?");
                    PreparedStatement ps2 = connection()
                            .prepareStatement("DELETE FROM forgefarms_members WHERE farm_id=?")) {
                ps.setString(1, farmId.toString());
                ps.executeUpdate();
                ps2.setString(1, farmId.toString());
                ps2.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete farm " + farmId, e);
            }
        }
    }

    @Override
    public Map<UUID, Map<UUID, String>> loadMembers() {
        Map<UUID, Map<UUID, String>> out = new HashMap<>();
        synchronized (lock) {
            try (Statement s = connection().createStatement();
                    ResultSet rs = s.executeQuery("SELECT farm_id, player, role FROM forgefarms_members")) {
                while (rs.next()) {
                    UUID farmId = UUID.fromString(rs.getString("farm_id"));
                    UUID player = UUID.fromString(rs.getString("player"));
                    out.computeIfAbsent(farmId, k -> new HashMap<>()).put(player, rs.getString("role"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load farm members", e);
            }
        }
        return out;
    }

    @Override
    public void saveMembers(UUID farmId, Map<UUID, String> roles) {
        synchronized (lock) {
            try (PreparedStatement del = connection()
                    .prepareStatement("DELETE FROM forgefarms_members WHERE farm_id=?")) {
                del.setString(1, farmId.toString());
                del.executeUpdate();
                if (!roles.isEmpty()) {
                    try (PreparedStatement ps = connection().prepareStatement(
                            "INSERT INTO forgefarms_members (farm_id, player, role) VALUES (?,?,?)")) {
                        for (Map.Entry<UUID, String> e : roles.entrySet()) {
                            ps.setString(1, farmId.toString());
                            ps.setString(2, e.getKey().toString());
                            ps.setString(3, e.getValue());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save members for farm " + farmId, e);
            }
        }
    }

    @Override
    public void clearAll() {
        synchronized (lock) {
            try (Statement s = connection().createStatement()) {
                s.execute("DELETE FROM forgefarms_members");
                s.execute("DELETE FROM forgefarms_farms");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to clear SQLite data", e);
            }
        }
    }

    @Override
    public void backup(File dest) throws IOException {
        synchronized (lock) {
            try (Statement s = connection().createStatement()) {
                s.execute("PRAGMA wal_checkpoint(TRUNCATE)");
            } catch (SQLException e) {
                throw new IOException("Checkpoint failed: " + e.getMessage(), e);
            }
            if (dest.getParentFile() != null) {
                dest.getParentFile().mkdirs();
            }
            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void restore(File src) throws IOException {
        synchronized (lock) {
            close();
            Files.copy(src.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
