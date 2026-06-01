package com.nexusisekai.game.server;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Quản lý danh sách server, bảo trì, lịch mở/đóng server.
 */
public class ServerManager {

    private static final Logger log = LoggerFactory.getLogger(ServerManager.class);
    private static ServerManager INSTANCE;

    private volatile int currentServerId = 1;
    private volatile boolean maintenanceActive = false;
    private volatile String maintenanceMessage = "";
    private volatile LocalDateTime maintenanceEnd;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static synchronized ServerManager getInstance() {
        if (INSTANCE == null) INSTANCE = new ServerManager();
        return INSTANCE;
    }

    public void start(int serverId) {
        this.currentServerId = serverId;
        // Kiểm tra maintenance mỗi phút
        scheduler.scheduleAtFixedRate(this::checkMaintenance, 0, 60, TimeUnit.SECONDS);
        // Auto-snapshot nhân vật active mỗi 24h (rollback sau hack/bug), trễ 30 phút sau khởi động
        scheduler.scheduleAtFixedRate(
            com.nexusisekai.game.snapshot.SnapshotService::autoSnapshotActive, 30, 1440, TimeUnit.MINUTES);
        log.info("[SERVER] ServerManager started for server id={}", serverId);
    }

    public void stop() { scheduler.shutdownNow(); }

    // ─────────────────────────────────────────
    // Maintenance check
    // ─────────────────────────────────────────

    private void checkMaintenance() {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM maintenance_schedule " +
                 "WHERE (server_id=0 OR server_id=?) AND status<2 " +
                 "AND start_time<=NOW() AND end_time>NOW() LIMIT 1")) {
            ps.setInt(1, currentServerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (!maintenanceActive) {
                    maintenanceActive = true;
                    maintenanceMessage = rs.getString("message");
                    maintenanceEnd = rs.getTimestamp("end_time").toLocalDateTime();
                    log.warn("[SERVER] Maintenance active: {}", maintenanceMessage);
                    // Update status
                    try (PreparedStatement upd = c.prepareStatement(
                            "UPDATE maintenance_schedule SET status=1 WHERE id=?")) {
                        upd.setInt(1, rs.getInt("id"));
                        upd.executeUpdate();
                    }
                }
            } else {
                if (maintenanceActive) {
                    maintenanceActive = false;
                    maintenanceMessage = "";
                    log.info("[SERVER] Maintenance ended.");
                    // Mark done
                    try (PreparedStatement upd = c.prepareStatement(
                            "UPDATE maintenance_schedule SET status=2 " +
                            "WHERE (server_id=0 OR server_id=?) AND status=1 AND end_time<=NOW()")) {
                        upd.setInt(1, currentServerId);
                        upd.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            log.error("checkMaintenance error: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // Admin API methods
    // ─────────────────────────────────────────

    public List<Map<String,Object>> listServers() throws SQLException {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM game_servers ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnName(i), rs.getObject(i));
                result.add(row);
            }
        }
        return result;
    }

    public void createServer(String name, int type, String host, int port, int adminPort,
                              String version, String description) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO game_servers (name,server_type,host,port,admin_port,version,description,status) " +
                 "VALUES (?,?,?,?,?,?,?,0)")) {
            ps.setString(1, name); ps.setInt(2, type);
            ps.setString(3, host); ps.setInt(4, port); ps.setInt(5, adminPort);
            ps.setString(6, version); ps.setString(7, description);
            ps.executeUpdate();
        }
    }

    public void setServerStatus(int serverId, int status) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE game_servers SET status=? WHERE id=?")) {
            ps.setInt(1, status); ps.setInt(2, serverId);
            ps.executeUpdate();
        }
    }

    public void scheduleOpenTime(int serverId, LocalDateTime openTime) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE game_servers SET open_time=? WHERE id=?")) {
            ps.setTimestamp(1, Timestamp.valueOf(openTime)); ps.setInt(2, serverId);
            ps.executeUpdate();
        }
    }

    public void scheduleMaintenance(int serverId, String title, String message,
                                     LocalDateTime start, LocalDateTime end,
                                     String patchNotes, String createdBy) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO maintenance_schedule (server_id,title,message,start_time,end_time,patch_notes,created_by) " +
                 "VALUES (?,?,?,?,?,?,?)")) {
            ps.setInt(1, serverId); ps.setString(2, title); ps.setString(3, message);
            ps.setTimestamp(4, Timestamp.valueOf(start)); ps.setTimestamp(5, Timestamp.valueOf(end));
            ps.setString(6, patchNotes); ps.setString(7, createdBy);
            ps.executeUpdate();
        }
    }

    public List<Map<String,Object>> getMaintenanceList() throws SQLException {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM maintenance_schedule ORDER BY start_time DESC LIMIT 50");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnName(i), rs.getObject(i));
                result.add(row);
            }
        }
        return result;
    }

    public boolean isMaintenanceActive() { return maintenanceActive; }
    public String getMaintenanceMessage() { return maintenanceMessage; }
    public int getCurrentServerId() { return currentServerId; }
}
