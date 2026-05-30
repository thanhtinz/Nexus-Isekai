package com.nexusisekai.game.economy;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Kho vật phẩm admin — admin upload item sẵn, dùng khi cần
 * (giftcode rewards, event rewards, webshop refill, v.v.)
 */
public class WarehouseManager {

    private static final Logger log = LoggerFactory.getLogger(WarehouseManager.class);
    private static WarehouseManager INSTANCE;

    public static synchronized WarehouseManager getInstance() {
        if (INSTANCE == null) INSTANCE = new WarehouseManager();
        return INSTANCE;
    }

    // ─── CRUD ─────────────────────────────────────────────────

    public long addItem(int itemId, String itemName, String itemType,
                        int qty, String description, String iconUrl,
                        String createdBy) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO admin_item_warehouse " +
                 "(item_id, item_name, item_type, qty, qty_total, description, icon_url, created_by) " +
                 "VALUES (?,?,?,?,?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemId);
            ps.setString(2, itemName);
            ps.setString(3, itemType);
            ps.setInt(4, qty);
            ps.setInt(5, qty);
            ps.setString(6, description);
            ps.setString(7, iconUrl);
            ps.setString(8, createdBy);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            long id = keys.next() ? keys.getLong(1) : -1;
            logAction(id, "add", qty, "Initial stock", "manual", null, createdBy);
            return id;
        }
    }

    /** Thêm số lượng vào kho (restock) */
    public void restock(long warehouseId, int qty, String reason, String adminUser) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement(
                "UPDATE admin_item_warehouse SET qty=qty+" + qty +
                ", qty_total=qty_total+" + qty +
                " WHERE id=" + warehouseId).executeUpdate();
            logAction(warehouseId, "add", qty, reason, "manual", null, adminUser);
        }
        log.info("[WAREHOUSE] Restock id={} +{} by {}", warehouseId, qty, adminUser);
    }

    /** Điều chỉnh số lượng trực tiếp */
    public void adjustStock(long warehouseId, int newQty, String reason, String adminUser) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT qty FROM admin_item_warehouse WHERE id=?");
            ps.setLong(1, warehouseId);
            ResultSet rs = ps.executeQuery();
            int oldQty = rs.next() ? rs.getInt(1) : 0;
            int diff = newQty - oldQty;

            c.prepareStatement(
                "UPDATE admin_item_warehouse SET qty=" + newQty + " WHERE id=" + warehouseId)
                .executeUpdate();
            logAction(warehouseId, "adjust", diff, reason, "manual", null, adminUser);
        }
    }

    /**
     * Lấy item từ kho cho mục đích sử dụng (giftcode, event, v.v.)
     * @return false nếu không đủ hàng
     */
    public boolean useItems(long warehouseId, int qty, String refType, String refId, String adminUser)
            throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                PreparedStatement ps = c.prepareStatement(
                    "SELECT qty FROM admin_item_warehouse WHERE id=? AND is_active=1 FOR UPDATE");
                ps.setLong(1, warehouseId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next() || rs.getInt(1) < qty) {
                    c.rollback();
                    return false;
                }
                c.prepareStatement(
                    "UPDATE admin_item_warehouse SET qty=qty-" + qty +
                    ", qty_used=qty_used+" + qty +
                    " WHERE id=" + warehouseId).executeUpdate();
                c.commit();
                logAction(warehouseId, "use", -qty, "Used for " + refType, refType, refId, adminUser);
                return true;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    // ─── Queries ──────────────────────────────────────────────

    public List<Map<String, Object>> listAll() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT w.*, " +
                 "  (SELECT COUNT(*) FROM admin_warehouse_log l WHERE l.warehouse_id=w.id) as log_count " +
                 "FROM admin_item_warehouse w ORDER BY w.created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getLogs(long warehouseId, int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM admin_warehouse_log WHERE warehouse_id=? ORDER BY created_at DESC LIMIT ?")) {
            ps.setLong(1, warehouseId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
        }
        return list;
    }

    public Map<String, Object> getItem(long warehouseId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM admin_item_warehouse WHERE id=?")) {
            ps.setLong(1, warehouseId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Map<String, Object> row = new LinkedHashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++)
                row.put(meta.getColumnName(i), rs.getObject(i));
            return row;
        }
    }

    public void deactivate(long warehouseId, String adminUser) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE admin_item_warehouse SET is_active=0 WHERE id=?")) {
            ps.setLong(1, warehouseId);
            ps.executeUpdate();
            logAction(warehouseId, "adjust", 0, "Deactivated", "manual", null, adminUser);
        }
    }

    // ─── Private ──────────────────────────────────────────────

    private void logAction(long warehouseId, String action, int qtyChange,
                            String reason, String refType, String refId,
                            String adminUser) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO admin_warehouse_log " +
                 "(warehouse_id, action, qty_change, reason, ref_type, ref_id, admin_user) " +
                 "VALUES (?,?,?,?,?,?,?)")) {
            ps.setLong(1, warehouseId);
            ps.setString(2, action);
            ps.setInt(3, qtyChange);
            ps.setString(4, reason);
            ps.setString(5, refType);
            ps.setString(6, refId);
            ps.setString(7, adminUser);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Warehouse log error: {}", e.getMessage());
        }
    }
}
