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
                "SELECT quantity FROM inventory WHERE char_id=? AND item_id=?", charId, itemId);
            if (inv == null || ((Number)inv.get("quantity")).intValue() < qty) return false;

            SqlSafe.update(c, "UPDATE inventory SET quantity=quantity-? WHERE char_id=? AND item_id=?", qty, charId, itemId);
            SqlSafe.update(c, "DELETE FROM inventory WHERE char_id=? AND item_id=? AND quantity<=0", charId, itemId);
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
                "INSERT INTO inventory (char_id, item_id, quantity) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE quantity=quantity+?", charId, itemId, qty, qty);
            return true;
        }
    }
}
