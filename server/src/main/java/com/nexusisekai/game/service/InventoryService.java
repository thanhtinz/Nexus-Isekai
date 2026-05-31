package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;

/** InventoryService — Thêm/xoá/dùng item, trang bị THẬT */
public class InventoryService {

    public static boolean addItem(long charId, int itemId, int qty) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,
                "INSERT INTO inventory (char_id, item_id, quantity) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE quantity=quantity+?", charId, itemId, qty, qty);
            return true;
        }
    }

    public static boolean removeItem(long charId, int itemId, int qty) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> item = SqlSafe.queryOne(c,
                "SELECT quantity FROM inventory WHERE char_id=? AND item_id=?", charId, itemId);
            if (item == null || ((Number) item.get("quantity")).intValue() < qty) return false;

            int remaining = ((Number) item.get("quantity")).intValue() - qty;
            if (remaining <= 0)
                SqlSafe.update(c, "DELETE FROM inventory WHERE char_id=? AND item_id=?", charId, itemId);
            else
                SqlSafe.update(c, "UPDATE inventory SET quantity=? WHERE char_id=? AND item_id=?", remaining, charId, itemId);
            return true;
        }
    }

    public static boolean equip(long charId, long itemUid, int slot) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Bỏ trang bị cũ ở slot này về túi
            SqlSafe.update(c,
                "UPDATE character_equipment SET equipped=0 WHERE char_id=? AND equip_slot=? AND equipped=1",
                charId, slot);
            // Trang bị mới
            SqlSafe.update(c,
                "UPDATE character_equipment SET equipped=1, equip_slot=? WHERE id=? AND char_id=?",
                slot, itemUid, charId);
            return true;
        }
    }
}
