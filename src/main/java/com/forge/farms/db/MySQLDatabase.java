package com.forge.farms.db;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.ConfigManager;
import com.forge.farms.farm.Farm;
import com.forge.farms.output.OutputMode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/** Network backend for multi-server setups. Driver bundled in the jar. */
public final class MySQLDatabase extends Database {
    private final String url;
    private final String user;
    private final String password;

    public MySQLDatabase(ForgeFarms plugin, ConfigManager config) {
        super(plugin);
        String ssl = config.mysqlUseSsl() ? "true" : "false";
        this.url = "jdbc:mysql://" + config.mysqlHost() + ":" + config.mysqlPort() + "/"
                + config.mysqlDatabase() + "?useSSL=" + ssl
                + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true";
        this.user = config.mysqlUser();
        this.password = config.mysqlPassword();
    }

    @Override
    public String backendName() {
        return "mysql";
    }

    @Override
    protected Connection newConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver missing from jar", e);
        }
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public void migrate() {
        synchronized (lock) {
            try {
                try (Statement s = connection().createStatement()) {
                    s.execute(DDL_FARMS);
                    s.execute(DDL_MEMBERS);
                }
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to migrate MySQL schema", e);
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
                    out.add(readFarm(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load farms", e);
            }
        }
        return out;
    }

    private Farm readFarm(ResultSet rs) throws SQLException {
        List<OutputMode> priority = new ArrayList<>();
        String raw = rs.getString("output_priority");
        if (raw != null) {
            for (String part : raw.split(",")) {
                try {
                    priority.add(OutputMode.valueOf(part.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // skip unknown entries
                }
            }
        }
        if (priority.isEmpty()) {
            priority.add(OutputMode.STORAGE);
        }
        Farm farm = new Farm(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("owner")),
                rs.getString("type"),
                UUID.fromString(rs.getString("world")),
                rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                rs.getInt("radius_level"), rs.getInt("speed_level"),
                rs.getInt("storage_level"), rs.getInt("efficiency_level"),
                rs.getLong("fuel_ticks"),
                priority,
                rs.getInt("auto_sell") != 0,
                rs.getInt("hologram") != 0,
                rs.getLong("total_harvested"),
                rs.getLong("created_at"), rs.getLong("last_tick"));
        farm.setRawStorage(Database.decodeStorage(rs.getString("storage"), 54));
        return farm;
    }

    @Override
    public void saveFarm(Farm farm) {
        synchronized (lock) {
            String sql = "INSERT INTO forgefarms_farms (id, owner, type, world, x, y, z,"
                    + " radius_level, speed_level, storage_level, efficiency_level,"
                    + " fuel_ticks, storage, output_priority, auto_sell, hologram,"
                    + " total_harvested, created_at, last_tick)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                    + " ON DUPLICATE KEY UPDATE owner=VALUES(owner), type=VALUES(type),"
                    + " world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z),"
                    + " radius_level=VALUES(radius_level), speed_level=VALUES(speed_level),"
                    + " storage_level=VALUES(storage_level), efficiency_level=VALUES(efficiency_level),"
                    + " fuel_ticks=VALUES(fuel_ticks), storage=VALUES(storage),"
                    + " output_priority=VALUES(output_priority), auto_sell=VALUES(auto_sell),"
                    + " hologram=VALUES(hologram), total_harvested=VALUES(total_harvested),"
                    + " created_at=VALUES(created_at), last_tick=VALUES(last_tick)";
            try (PreparedStatement ps = connection().prepareStatement(sql)) {
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
                ps.setLong(12, farm.fuelTicks());
                ps.setString(13, Database.encodeStorage(farm.storageSnapshot()));
                ps.setString(14, farm.outputPriority().stream().map(Enum::name).collect(Collectors.joining(",")));
                ps.setInt(15, farm.autoSell() ? 1 : 0);
                ps.setInt(16, farm.hologramEnabled() ? 1 : 0);
                ps.setLong(17, farm.totalHarvested());
                ps.setLong(18, farm.createdAt());
                ps.setLong(19, farm.lastTickAt());
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
                plugin.getLogger().log(Level.SEVERE, "Failed to clear MySQL data", e);
            }
        }
    }

    @Override
    public void backup(File dest) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("-- ForgeFarms backup\n");
        synchronized (lock) {
            try {
                for (String table : List.of("forgefarms_farms", "forgefarms_members")) {
                    try (Statement s = connection().createStatement();
                            ResultSet rs = s.executeQuery("SELECT * FROM " + table)) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int cols = meta.getColumnCount();
                        while (rs.next()) {
                            sb.append("INSERT INTO ").append(table).append(" VALUES (");
                            for (int i = 1; i <= cols; i++) {
                                if (i > 1) {
                                    sb.append(',');
                                }
                                String v = rs.getString(i);
                                if (rs.wasNull()) {
                                    sb.append("NULL");
                                } else {
                                    sb.append('\'').append(v.replace("\\", "\\\\").replace("'", "\\'")).append('\'');
                                }
                            }
                            sb.append(");\n");
                        }
                    }
                }
            } catch (SQLException e) {
                throw new IOException("MySQL backup failed: " + e.getMessage(), e);
            }
        }
        if (dest.getParentFile() != null) {
            dest.getParentFile().mkdirs();
        }
        Files.writeString(dest.toPath(), sb.toString(), StandardCharsets.UTF_8);
    }

    @Override
    public void restore(File src) throws IOException {
        String sql = Files.readString(src.toPath(), StandardCharsets.UTF_8);
        synchronized (lock) {
            try {
                clearAll();
                try (Statement s = connection().createStatement()) {
                    for (String stmt : sql.split(";\\n")) {
                        String t = stmt.trim();
                        if (!t.isEmpty()) {
                            s.execute(t);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new IOException("MySQL restore failed: " + e.getMessage(), e);
            }
        }
    }
}
