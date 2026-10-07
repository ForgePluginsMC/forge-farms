package com.forge.farms.db;

import com.forge.farms.ForgeFarms;
import com.forge.farms.config.ConfigManager;
import com.forge.farms.farm.Farm;
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
                // v2: per-farm tuning knobs
                try (Statement s = connection().createStatement()) {
                    s.execute("ALTER TABLE forgefarms_farms ADD COLUMN settings MEDIUMTEXT");
                } catch (SQLException e) {
                    if (e.getMessage() == null
                            || !e.getMessage().toLowerCase(Locale.ROOT).contains("duplicate")) {
                        throw e;
                    }
                    // Column already exists — nothing to do.
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
                    .append(" ON DUPLICATE KEY UPDATE ");
            for (int i = 1; i < FARM_COLS.length; i++) {
                if (i > 1) {
                    sql.append(',');
                }
                sql.append(FARM_COLS[i]).append("=VALUES(").append(FARM_COLS[i]).append(')');
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
