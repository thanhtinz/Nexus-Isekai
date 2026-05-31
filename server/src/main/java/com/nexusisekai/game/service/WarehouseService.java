package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;

/** WarehouseService — Kho đồ (deposit/withdraw giữa túi và kho) */
public class WarehouseService {

    public static boolean deposit(long charId, int itemId, int qty) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> inv = SqlSafe.queryOne(c,
                "SELECT qty FROM character_inventory WHERE char_id=? AND item_id=? AND slot=-1", charId, itemId);
            if (inv == null || ((Number)inv.get("qty")).intValue() < qty) return false;

            // Kiểm tra sức chứa: nếu là item MỚI trong kho và kho đầy → từ chối
            Map<String,Object> existing = SqlSafe.queryOne(c,
                "SELECT quantity FROM character_warehouse WHERE char_id=? AND item_id=?", charId, itemId);
            if (existing == null && usedSlots(charId) >= maxSlots(charId)) return false;

            SqlSafe.update(c, "UPDATE character_inventory SET qty=qty-? WHERE char_id=? AND item_id=? AND slot=-1", qty, charId, itemId);
            SqlSafe.update(c, "DELETE FROM character_inventory WHERE char_id=? AND item_id=? AND qty<=0", charId, itemId);
            SqlSafe.update(c,
                "INSERT INTO character_warehouse (char_id, item_id, quantity) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE quantity=quantity+?", charId, itemId, qty, qty);
            return true;
        }
    }

    public static boolean withdraw(long charId, int itemId, int qty) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> wh = SqlSafe.queryOne(c,
                "SELECT quantity FROM character_warehouse WHERE char_id=? AND item_id=?", charId, itemId);
            if (wh == null || ((Number)wh.get("quantity")).intValue() < qty) return false;

            SqlSafe.update(c, "UPDATE character_warehouse SET quantity=quantity-? WHERE char_id=? AND item_id=?", qty, charId, itemId);
            SqlSafe.update(c, "DELETE FROM character_warehouse WHERE char_id=? AND item_id=? AND quantity<=0", charId, itemId);
            SqlSafe.update(c,
                "INSERT INTO character_inventory (char_id, item_id, qty, slot) VALUES (?,?,?,-1) " +
                "ON DUPLICATE KEY UPDATE quantity=quantity+?", charId, itemId, qty, qty);
            return true;
        }
    }

    /** Danh sách vật phẩm trong kho. */
    public static java.util.List<Map<String,Object>> list(long charId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            return SqlSafe.query(c,
                "SELECT cw.item_id, cw.quantity, COALESCE(i.name,'') AS name, COALESCE(i.sell_price,0) AS sell_price, COALESCE(i.icon_id,0) AS icon_id " +
                "FROM character_warehouse cw LEFT JOIN items i ON i.id=cw.item_id WHERE cw.char_id=? ORDER BY cw.item_id", charId);
        }
    }

    /** Số ô đang dùng (loại item khác nhau). */
    public static int usedSlots(long charId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> r = SqlSafe.queryOne(c, "SELECT COUNT(*) AS n FROM character_warehouse WHERE char_id=? AND quantity>0", charId);
            return r == null ? 0 : ((Number)r.get("n")).intValue();
        }
    }

    /** Sức chứa tối đa (tạo mặc định nếu chưa có). */
    public static int maxSlots(long charId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> r = SqlSafe.queryOne(c, "SELECT max_slots FROM warehouse_info WHERE char_id=?", charId);
            if (r == null) {
                SqlSafe.update(c, "INSERT IGNORE INTO warehouse_info (char_id) VALUES (?)", charId);
                return 50;
            }
            return ((Number)r.get("max_slots")).intValue();
        }
    }

    /** Bán trực tiếp 1 loại sản phẩm TỪ KHO → nhận vàng (sell_price × qty). */
    public static long sellFromWarehouse(long charId, int itemId, int qty) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> wh = SqlSafe.queryOne(c,
                "SELECT quantity FROM character_warehouse WHERE char_id=? AND item_id=?", charId, itemId);
            if (wh == null || ((Number)wh.get("quantity")).intValue() < qty || qty <= 0) return -1;
            Map<String,Object> it = SqlSafe.queryOne(c, "SELECT sell_price FROM items WHERE id=?", itemId);
            int price = it == null ? 0 : ((Number)it.get("sell_price")).intValue();
            long earn = (long) price * qty;
            SqlSafe.update(c, "UPDATE character_warehouse SET quantity=quantity-? WHERE char_id=? AND item_id=?", qty, charId, itemId);
            SqlSafe.update(c, "DELETE FROM character_warehouse WHERE char_id=? AND item_id=? AND quantity<=0", charId, itemId);
            SqlSafe.update(c, "UPDATE characters SET gold=gold+? WHERE id=?", earn, charId);
            return earn;
        }
    }
}
