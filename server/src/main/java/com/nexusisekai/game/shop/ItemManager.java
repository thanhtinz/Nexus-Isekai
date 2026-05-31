package com.nexusisekai.game.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.entity.InventoryItem;
import com.nexusisekai.game.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemManager {

    private static final Logger log = LoggerFactory.getLogger(ItemManager.class);

    // Singleton — dùng bởi QuestManager, GiftCodeManager, MissionPassManager,
    // FarmingManager, MentorManager, InventoryHandler, ExtendedHandlers...
    private static ItemManager INSTANCE;
    public static synchronized ItemManager getInstance() {
        if (INSTANCE == null) INSTANCE = new ItemManager();
        return INSTANCE;
    }
    private final ObjectMapper mapper = new ObjectMapper();

    // item_id → ItemTemplate
    private final Map<Integer, ItemTemplate> itemTemplates = new ConcurrentHashMap<>();

    public void loadAll() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM items WHERE is_active=1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemTemplate t = ItemTemplate.fromRs(rs);
                itemTemplates.put(t.getId(), t);
            }
        }
        log.info("[ITEM] Loaded {} item templates.", itemTemplates.size());
    }

    public int getItemCount() { return itemTemplates.size(); }

    public ItemTemplate getTemplate(int itemId) { return itemTemplates.get(itemId); }

    /**
     * Load inventory của player từ DB
     */
    public void loadInventory(Player player) throws Exception {
        player.getInventory().clear();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT ci.*, i.name, i.type, i.icon_id, i.stats_json " +
                     "FROM character_inventory ci JOIN items i ON ci.item_id=i.id " +
                     "WHERE ci.char_id=?")) {
            ps.setLong(1, player.getCharId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setId(rs.getLong("id"));
                item.setItemId(rs.getInt("item_id"));
                item.setName(rs.getString("name"));
                item.setType(rs.getInt("type"));
                item.setQuantity(rs.getInt("quantity"));
                item.setSlot(rs.getInt("slot"));
                item.setEnchant(rs.getByte("enchant"));
                item.setIconId(rs.getInt("icon_id"));

                // Parse stats
                String statsJson = rs.getString("stats_json");
                if (statsJson != null && !statsJson.isEmpty()) {
                    JsonNode stats = mapper.readTree(statsJson);
                    if (stats.has("atk")) item.setAtkBonus(stats.get("atk").asInt());
                    if (stats.has("def")) item.setDefBonus(stats.get("def").asInt());
                }
                player.getInventory().add(item);
            }
        }
    }

    /**
     * Cộng item vào inventory player (dùng khi player đã load vào game)
     */
    public void giveItem(Player player, int itemId, int qty) {
        giveItem(player.getCharId(), itemId, qty);
    }

    /**
     * Cộng item vào inventory theo charId (dùng khi tạo nhân vật mới)
     */
    public void giveItem(long charId, int itemId, int qty) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, qty FROM character_inventory WHERE char_id=? AND item_id=? AND slot=-1 LIMIT 1");
            ps.setLong(1, charId);
            ps.setInt(2, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int newQty = rs.getInt("quantity") + qty;
                PreparedStatement upd = conn.prepareStatement(
                        "UPDATE character_inventory SET qty=? WHERE id=?");
                upd.setInt(1, newQty);
                upd.setLong(2, rs.getLong("id"));
                upd.executeUpdate();
            } else {
                PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO character_inventory (char_id,item_id,qty,slot) VALUES (?,?,?,-1)");
                ins.setLong(1, charId);
                ins.setInt(2, itemId);
                ins.setInt(3, qty);
                ins.executeUpdate();
            }
        } catch (Exception e) {
            log.error("giveItem error: {}", e.getMessage(), e);
        }
    }

    public Map<Integer, ItemTemplate> getAllTemplates() { return itemTemplates; }
}
